package com.astroimagej.tasks

import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openpgp.*
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentVerifierBuilderProvider
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.net.URL
import java.security.Security

abstract class PGPVerify : DefaultTask() {
    @get:Input
    abstract val keyId: Property<String>

    @get:InputFile
    abstract val file: RegularFileProperty

    @get:InputFile
    abstract val signature: RegularFileProperty

    @TaskAction
    fun verify() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }

        val sigFile = signature.asFile.get()
        val pgpSignature = loadSignature(sigFile)
            ?: throw GradleException("Could not extract PGPSignature from ${sigFile.path}")

        val keyIdLong = pgpSignature.keyID
        //val keyIdHex = java.lang.Long.toHexString(keyIdLong).uppercase()
        val keyIdHex = keyId.get().uppercase().takeLast(16)

        val publicKey = fetchSinglePublicKeyFromKeyServer(keyIdHex)

        pgpSignature.init(
            JcaPGPContentVerifierBuilderProvider().setProvider("BC"),
            publicKey
        )

        val dataFile = file.asFile.get()
        BufferedInputStream(FileInputStream(dataFile)).use { dataIn ->
            val buffer = ByteArray(1 shl 16) // 64 KB
            var len: Int
            while (dataIn.read(buffer).also { len = it } > 0) {
                pgpSignature.update(buffer, 0, len)
            }
        }

        if (!pgpSignature.verify()) {
            throw GradleException("PGP signature verification FAILED for ${dataFile.name}")
        }
    }

    private fun fetchSinglePublicKeyFromKeyServer(keyIdHex: String): PGPPublicKey {
        val urlStr = "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x${keyIdHex}"

        URL(urlStr).openStream().use { rawIn ->
            PGPUtil.getDecoderStream(rawIn).use { keyIn ->
                val keyRings = PGPPublicKeyRingCollection(keyIn, JcaKeyFingerprintCalculator())
                val keyIdLong = java.lang.Long.parseUnsignedLong(keyIdHex, 16)
                val pubKey = keyRings.getPublicKey(keyIdLong)
                if (pubKey != null) {
                    return pubKey
                }
            }
        }

        throw GradleException("No public key 0x$keyIdHex found on Ubuntu keyserver")
    }

    private fun loadSignature(sigFile: File): PGPSignature? {
        FileInputStream(sigFile).use { sigInRaw ->
            PGPUtil.getDecoderStream(sigInRaw).use { sigStream ->
                val factory = PGPObjectFactory(sigStream, JcaKeyFingerprintCalculator())
                var obj: Any? = factory.nextObject()
                while (obj != null && obj !is PGPSignatureList) {
                    obj = factory.nextObject()
                }
                val sigList = (obj as? PGPSignatureList) ?: return null
                return sigList.firstOrNull()
            }
        }
    }
}
