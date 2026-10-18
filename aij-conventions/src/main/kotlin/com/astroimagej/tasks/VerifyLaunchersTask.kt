package com.astroimagej.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission

abstract class VerifyLaunchersTask : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val launchersDir: DirectoryProperty

    private val targets = listOf(
        LauncherTarget("Linux_aarch64", TargetOs.LINUX, TargetArch.AARCH64),
        LauncherTarget("Linux_x64", TargetOs.LINUX, TargetArch.X64),
        LauncherTarget("Mac_aarch64", TargetOs.MAC, TargetArch.AARCH64),
        LauncherTarget("Mac_x64", TargetOs.MAC, TargetArch.X64),
        LauncherTarget("Windows_aarch64", TargetOs.WINDOWS, TargetArch.AARCH64),
        LauncherTarget("Windows_x64", TargetOs.WINDOWS, TargetArch.X64),
    )

    @TaskAction
    fun verify() {
        val root = launchersDir.get().asFile
        val errors = mutableListOf<String>()

        for (target in targets) {
            val file = root.resolve(target.dirName).resolve(target.fileName)
            val label = "${target.dirName}/${target.fileName}"
            if (!file.isFile) {
                errors += "$label: file not found"
                continue
            }

            val header = readHeader(file)
            val formatErrors = when (target.os) {
                TargetOs.LINUX -> checkElf(header, target.arch)
                TargetOs.MAC -> checkMachO(header, target.arch)
                TargetOs.WINDOWS -> checkPe(header, target.arch) + checkPeSigned(header, file.length())
            }
            errors += formatErrors.map { "$label: $it" }

            if (target.os != TargetOs.WINDOWS) {
                checkExecutable(file)?.let { errors += "$label: $it" }
            }
        }

        val targetDirs = targets.map { target -> target.dirName }
        root.listFiles { it.isDirectory }!!.filterNot { targetDirs.contains(it.name) }.forEach { errors += "Unexpected directory: ${it.name}" }

        if (errors.isNotEmpty()) {
            throw GradleException(
                "Launcher verification failed:\n" + errors.joinToString("\n") { "  - $it" }
            )
        }
        logger.lifecycle("All ${targets.size} launchers verified.")
    }

    private fun readHeader(file: File): ByteBuffer {
        val bytes = file.inputStream().use { it.readNBytes(HEADER_BYTES) }
        return ByteBuffer.wrap(bytes)
    }

    private fun ByteBuffer.safeInt(offset: Int, order: ByteOrder): Int? =
        if (offset < 0 || offset + 4 > limit()) null else duplicate().order(order).getInt(offset)

    private fun ByteBuffer.safeShort(offset: Int, order: ByteOrder): Int? =
        if (offset < 0 || offset + 2 > limit()) null else duplicate().order(order).getShort(offset).toInt() and 0xFFFF

    // ---- ELF (Linux) -------------------------------------------------------------------------

    private fun checkElf(h: ByteBuffer, arch: TargetArch): List<String> {
        if (h.limit() < 20 || h.get(0) != 0x7F.toByte() || h.get(1) != 'E'.code.toByte() ||
            h.get(2) != 'L'.code.toByte() || h.get(3) != 'F'.code.toByte()
        ) return listOf("not an ELF executable")

        val errors = mutableListOf<String>()
        if (h.get(4).toInt() != 2) errors += "not a 64-bit ELF"
        if (h.get(5).toInt() != 1) return errors + "not a little-endian ELF"

        val machine = h.safeShort(18, ByteOrder.LITTLE_ENDIAN)
        val expected = when (arch) {
            TargetArch.X64 -> EM_X86_64
            TargetArch.AARCH64 -> EM_AARCH64
        }
        if (machine != expected) errors += "ELF machine is ${machine?.toString(16)}, expected ${expected.toString(16)} ($arch)"
        return errors
    }

    // ---- Mach-O (Mac) ------------------------------------------------------------------------

    private fun checkMachO(h: ByteBuffer, arch: TargetArch): List<String> {
        val expected = when (arch) {
            TargetArch.X64 -> CPU_TYPE_X86_64
            TargetArch.AARCH64 -> CPU_TYPE_ARM64
        }
        val magicLE = h.safeInt(0, ByteOrder.LITTLE_ENDIAN)
        val magicBE = h.safeInt(0, ByteOrder.BIG_ENDIAN)

        return when {
            magicLE == MH_MAGIC_64 -> {
                val cpu = h.safeInt(4, ByteOrder.LITTLE_ENDIAN)
                if (cpu == expected) emptyList()
                else listOf("Mach-O cpu type is 0x${cpu?.toUInt()?.toString(16)}, expected 0x${expected.toUInt().toString(16)} ($arch)")
            }
            // Universal ("fat") binary: accept if it contains the target arch.
            magicBE == FAT_MAGIC || magicBE == FAT_MAGIC_64 -> {
                val count = h.safeInt(4, ByteOrder.BIG_ENDIAN) ?: 0
                val entrySize = if (magicBE == FAT_MAGIC_64) 32 else 20
                val cpus = (0 until count).mapNotNull { h.safeInt(8 + it * entrySize, ByteOrder.BIG_ENDIAN) }
                if (expected in cpus) emptyList()
                else listOf("universal Mach-O does not contain $arch")
            }
            else -> listOf("not a 64-bit Mach-O executable")
        }
    }

    // ---- PE (Windows) ------------------------------------------------------------------------

    private fun peOffset(h: ByteBuffer): Int? {
        if (h.limit() < 0x40 || h.get(0) != 'M'.code.toByte() || h.get(1) != 'Z'.code.toByte()) return null
        val pe = h.safeInt(0x3C, ByteOrder.LITTLE_ENDIAN) ?: return null
        val sig = h.safeInt(pe, ByteOrder.LITTLE_ENDIAN)
        return if (sig == PE_SIGNATURE) pe else null
    }

    private fun checkPe(h: ByteBuffer, arch: TargetArch): List<String> {
        val pe = peOffset(h) ?: return listOf("not a PE executable")
        val machine = h.safeShort(pe + 4, ByteOrder.LITTLE_ENDIAN)
        val expected = when (arch) {
            TargetArch.X64 -> IMAGE_FILE_MACHINE_AMD64
            TargetArch.AARCH64 -> IMAGE_FILE_MACHINE_ARM64
        }
        return if (machine == expected) emptyList()
        else listOf("PE machine is 0x${machine?.toString(16)}, expected 0x${expected.toString(16)} ($arch)")
    }

    /**
     * Checks that the PE has an embedded Authenticode signature (a non-empty certificate table
     * that lies inside the file). This confirms the file was signed; it does not validate the
     * certificate chain. Run `osslsigncode verify` / `signtool verify` for full validation.
     */
    private fun checkPeSigned(h: ByteBuffer, fileLength: Long): List<String> {
        val pe = peOffset(h) ?: return emptyList() // already reported by checkPe
        val optionalHeader = pe + 24
        val dataDirs = when (h.safeShort(optionalHeader, ByteOrder.LITTLE_ENDIAN)) {
            0x20B -> optionalHeader + 112 // PE32+
            0x10B -> optionalHeader + 96  // PE32
            else -> return listOf("unrecognized PE optional header")
        }
        val securityEntry = dataDirs + IMAGE_DIRECTORY_ENTRY_SECURITY * 8
        val certOffset = h.safeInt(securityEntry, ByteOrder.LITTLE_ENDIAN)?.toUInt()?.toLong()
        val certSize = h.safeInt(securityEntry + 4, ByteOrder.LITTLE_ENDIAN)?.toUInt()?.toLong()

        return when {
            certOffset == null || certSize == null -> listOf("could not read PE certificate table")
            certOffset == 0L || certSize == 0L -> listOf("not signed (no Authenticode certificate table)")
            certOffset + certSize > fileLength -> listOf("signature table extends beyond end of file (corrupt or truncated)")
            else -> emptyList()
        }
    }

    private fun checkExecutable(file: File): String? {
        return try {
            val perms = Files.getPosixFilePermissions(file.toPath())
            if (PosixFilePermission.OWNER_EXECUTE in perms) null
            else "missing executable permission (mode ${PosixFilePermission::class.java.simpleName} ${perms})"
        } catch (e: UnsupportedOperationException) {
            // POSIX permissions aren't visible on this filesystem (e.g. building on Windows).
            logger.warn("Skipping executable-bit check for ${file.name}: filesystem has no POSIX permissions")
            null
        }
    }

    private companion object {
        const val HEADER_BYTES = 4096

        // ELF
        const val EM_X86_64 = 0x3E
        const val EM_AARCH64 = 0xB7

        // Mach-O
        const val MH_MAGIC_64 = 0xFEEDFACF.toInt()
        const val FAT_MAGIC = 0xCAFEBABE.toInt()
        const val FAT_MAGIC_64 = 0xCAFEBABF.toInt()
        const val CPU_TYPE_X86_64 = 0x01000007
        const val CPU_TYPE_ARM64 = 0x0100000C

        // PE
        const val PE_SIGNATURE = 0x00004550 // "PE\0\0"
        const val IMAGE_FILE_MACHINE_AMD64 = 0x8664
        const val IMAGE_FILE_MACHINE_ARM64 = 0xAA64
        const val IMAGE_DIRECTORY_ENTRY_SECURITY = 4
    }
}

enum class TargetOs { LINUX, MAC, WINDOWS }
enum class TargetArch { X64, AARCH64 }

data class LauncherTarget(val dirName: String, val os: TargetOs, val arch: TargetArch) {
    val fileName: String get() = if (os == TargetOs.WINDOWS) "JavaLauncher.exe" else "JavaLauncher"
}