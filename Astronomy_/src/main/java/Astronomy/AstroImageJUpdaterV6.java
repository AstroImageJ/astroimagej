package Astronomy;

import static java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;

import java.awt.FileDialog;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.DigestInputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.Vector;
import java.util.concurrent.Executors;

import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;

import Astronomy.updater.MetaVersion;
import Astronomy.updater.SemanticVersion;
import Astronomy.updater.SpecificVersion;
import dev.sigstore.KeylessVerificationException;
import dev.sigstore.KeylessVerifier;
import dev.sigstore.VerificationOptions;
import dev.sigstore.bundle.Bundle;
import dev.sigstore.strings.StringMatcher;
import dev.sigstore.trustroot.SigstoreConfigurationException;
import ij.IJ;
import ij.ImageJ;
import ij.Prefs;
import ij.astro.gui.ToolTipRenderer;
import ij.astro.io.prefs.Property;
import ij.astro.util.ProgressTrackingInputStream;
import ij.astro.util.UIHelper;
import ij.gui.GenericDialog;
import ij.gui.MultiLineLabel;
import ij.plugin.PlugIn;
import util.BrowserOpener;

public class AstroImageJUpdaterV6 implements PlugIn {
    public static final String DO_UPDATE_NOTIFICATION = ".aij.update";
    private final URI metaUrl = new URI("https://astroimagej.com/meta/versions.json");
    private static final HttpClient HTTP_CLIENT;
    public static final String CERTIFICATE_IDENTITY = "https://github.com/AstroImageJ/astroimagej/.github/workflows/publish.yml@refs/heads/master";
    private MetaVersion meta;
    private static final Property<Boolean> ENABLE_PRERELEASES = new Property<>(false, AstroImageJUpdaterV6.class);
    private static final Property<Boolean> ENABLE_DAILY_BUILDS = new Property<>(false, AstroImageJUpdaterV6.class);

    static {
        HTTP_CLIENT = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
    }

    public AstroImageJUpdaterV6() throws URISyntaxException {
        UIHelper.setLookAndFeel();
    }

    MetaVersion fetchVersions() {
        if (meta == null) {
            meta = MetaVersion.readJson(metaUrl);
        }

        return meta;
    }

    public void update() {
        SwingUtilities.invokeLater(() -> {
            AstroImageJUpdaterV6 u;
            try {
                u = new AstroImageJUpdaterV6();
            } catch (URISyntaxException e) {
                IJ.error("Failed to connect AstroImageJ meta");
                return;
            }
            u.dialog();
        });
    }

    public void updateCheck() {
        if (Prefs.getBoolean(DO_UPDATE_NOTIFICATION, true)) {
            if (hasUpdateAvailable()) {
                dialog();
            }
        }
    }

    private boolean hasUpdateAvailable() {
        var meta = fetchVersions();
        var current = new SemanticVersion(IJ.getAstroVersion());
        for (MetaVersion.VersionEntry version : meta.versions()) {
            if (version.releaseType() == MetaVersion.ReleaseType.RELEASE) {
                if (version.version().compareTo(current) > 0) {
                    return true;
                }
            }
        }

        return false;
    }

    public void downloadSpecificVersion(MetaVersion.VersionEntry entry) throws Exception {
        var version = SpecificVersion.readJson(new URI(entry.url()));

        if (version.message() != null && !version.message().isBlank()) {
            if (!IJ.showMessageWithCancel("Updater", version.message())) {
                return;
            }
        }

        var appFolder = getBaseDirectory(ImageJ.class).toAbsolutePath().normalize();
        Path baseDir;
        if (IJ.isWindows()) {
            baseDir = appFolder.getParent();
        } else if (IJ.isMacOSX()) {
            baseDir = appFolder.getParent().getParent();
        } else if (IJ.isLinux()) {
            baseDir = appFolder.getParent().getParent();
        } else {
            throw new IllegalStateException("Unknown OS - could not find installation directory");
        }

        if (!Files.exists(baseDir)) {
            IJ.error("Unable to find installation directory.");
            return;
        }

        var gd = new GenericDialog("Updater");
        gd.addMessage("""
               AstroImageJ update will OVERWRITE or REMOVE the files in its install directory.
               Please make sure AIJ has a dedicated install directory, not shared with other software.
                \s
               Install directory: %s
               """.formatted(baseDir.toAbsolutePath()));
        gd.showDialog();

        if (gd.wasCanceled()) {
            return;
        }

        SpecificVersion.FileEntry fileEntry = null;
        for (SpecificVersion.FileEntry file : version.files()) {
            if (file.matchesSystem()) {
                fileEntry = file;
            }
        }

        if (fileEntry == null) {
            IJ.error("Unable to find file entry for current system.");
            return;
        }

        var pid = ProcessHandle.current().pid();

        // Manually manage temp folder and deletion as Windows doesn't automatically clean them
        var tmpFolder = Path.of(System.getProperty("java.io.tmpdir")).resolve("aij-updater");

        if (!IJ.isMacOSX() && Files.exists(tmpFolder)) {
            Files.walkFileTree(tmpFolder,
                    new SimpleFileVisitor<>() {
                        @Override
                        public FileVisitResult postVisitDirectory(
                                Path dir, IOException exc) throws IOException {
                            Files.deleteIfExists(dir);
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFile(
                                Path file, BasicFileAttributes attrs)
                                throws IOException {
                            Files.deleteIfExists(file);
                            return FileVisitResult.CONTINUE;
                        }
                    });
        }

        if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
            Files.createDirectories(tmpFolder, PosixFilePermissions.asFileAttribute(EnumSet
                    .of(OWNER_READ, OWNER_WRITE, OWNER_EXECUTE)));
        } else {
            Files.createDirectories(tmpFolder);
        }

        var tmp = tmpFolder.resolve("updateScript" + (IJ.isWindows() ? ".bat" : ".sh"));

        // Copy script to location for execution
        if (!IJ.isMacOSX()) {
            Files.copy(Objects.requireNonNull(AstroImageJUpdaterV6.class.getClassLoader()
                    .getResourceAsStream(getScriptPath())), tmp, StandardCopyOption.REPLACE_EXISTING);
        }

        var ext = ".msi";
        if (IJ.isLinux()) {
            ext = ".tgz";
        } else if (IJ.isMacOSX()) {
            ext = ".dmg";
        }
        var inst = tmpFolder.resolve("installer" + ext);

        var installerBytes = downloadAndComputeHash(fileEntry, 5);
        if (installerBytes == null) {
            IJ.error("Unable to download installer.");
            return;
        }

        if (IJ.isAijDev()) {
            IJ.showMessage("Updater", "Running in development mode, skipping installation");
            return;
        }

        if (IJ.isMacOSX()) {
            var p = Path.of("/tmp/aij-updater/installer.dmg");
            var f = new File[1];
            SwingUtilities.invokeAndWait(() -> {
                var fd = new FileDialog(IJ.getInstance(), "Save update as...", FileDialog.SAVE);
                fd.setFile(p.toAbsolutePath().toString());
                fd.setVisible(true);
                if (fd.getFiles().length == 1) {
                    f[0] = fd.getFiles()[0];
                }
            });

            if (f[0] != null) {
                Files.write(f[0].toPath(), installerBytes);
            } else {
                return;
            }
        } else {
            Files.write(inst, installerBytes);
        }

        if (IJ.isWindows()) {
            ProcessBuilder pb = new ProcessBuilder(
                    "cmd", "/c",
                    "start", "\"\"", "/b",
                    tmp.toAbsolutePath().toString(),
                    Long.toString(pid),
                    inst.toAbsolutePath().toString(),
                    baseDir.toAbsolutePath().toString()
            );
            try {
                Process p = pb.start();
            } catch (Exception e) {
                IJ.error("Updater", "Failed to run elevator: " + e.getMessage());
                e.printStackTrace();
                return;
            }

            System.exit(0);
        } else if (IJ.isLinux()) {
            var perms = Files.getPosixFilePermissions(tmp);
            perms.add(PosixFilePermission.OWNER_EXECUTE);
            perms.add(PosixFilePermission.GROUP_EXECUTE);
            perms.add(PosixFilePermission.OTHERS_EXECUTE);
            Files.setPosixFilePermissions(tmp, perms);
            ProcessBuilder pb = new ProcessBuilder(
                    tmp.toAbsolutePath().toString(),
                    Long.toString(pid),
                    inst.toAbsolutePath().toString(),
                    baseDir.toAbsolutePath().toString()
            );
            try {
                Process p = pb.start();
            } catch (Exception e) {
                IJ.error("Updater", "Failed to run elevator: " + e.getMessage());
                e.printStackTrace();
                return;
            }

            System.exit(0);
        } else if (IJ.isMacOSX()) {
            // Mac sandbox prevents running extracted scripts, and the update requires a manual step anyways,
            // so just do everyting manually.
            ProcessBuilder pb = new ProcessBuilder(
                    "open",
                    inst.toAbsolutePath().toString()
            );
            try {
                Process p = pb.start();
            } catch (Exception e) {
                IJ.error("Updater", "Failed to run elevator: " + e.getMessage());
                e.printStackTrace();
                return;
            }

            System.exit(0);
        }

        throw new IllegalStateException("Unknown OS: Could not handle update");
    }

    public byte[] downloadAndComputeHash(SpecificVersion.FileEntry fileEntry, int maxRetries) throws Exception {
        // Download file
        byte[] buffer = null;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            System.out.printf("Attempt %d of %d...%n", attempt, maxRetries);

            buffer = downloadFile(fileEntry.url(), fileEntry.sha256(), maxRetries, attempt);
            if (buffer != null) break;
        }

        // Download signature
        byte[] signatureBuffer = null;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            System.out.printf("Attempt %d of %d...%n", attempt, maxRetries);

            signatureBuffer = downloadFile(fileEntry.signatureUrl(), fileEntry.signatureSha256(), maxRetries, attempt);
            if (signatureBuffer != null) break;
        }

        System.out.println("Downloaded file and signature.");

        // Verify signature
        if (buffer != null && signatureBuffer != null) {
            System.out.println("Verifying signature...");
            var bundle = Bundle.from(new InputStreamReader(new ByteArrayInputStream(signatureBuffer)));
            var options = VerificationOptions.builder().addCertificateMatchers(
                    VerificationOptions.CertificateMatcher.fulcio()
                            .subjectAlternativeName(StringMatcher.string(CERTIFICATE_IDENTITY))
                            // See https://docs.sigstore.dev/quickstart/verification-cheat-sheet/#verifying-a-signature-created-by-a-workflow
                            .issuer(StringMatcher.string("https://token.actions.githubusercontent.com"))
                            .build()
            ).build();

            /*try {
                var signingCert = bundle.getCertPath();
                var leafCert = Certificates.getLeaf(signingCert);
                var certpath = Certificates.toCertPath(leafCert);
                for (Certificate certificate : certpath.getCertificates()) {
                    if (certificate instanceof X509Certificate x509Certificate) {
                        System.out.println(x509Certificate.getIssuerAlternativeNames());
                        System.out.println(x509Certificate.getSubjectAlternativeNames());
                    }
                    System.out.println(certificate);
                }
                System.out.println(Certificates.toCertPath(leafCert));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }*/

            try {
                var verifier = new KeylessVerifier.Builder().sigstorePublicDefaults().build();
                verifier.verify(HexFormat.of().parseHex(fileEntry.sha256()), bundle, options);
                System.out.println("Signature verified.");
            } catch (InvalidAlgorithmParameterException | SigstoreConfigurationException | NoSuchAlgorithmException |
                     InvalidKeySpecException | CertificateException e) {
                IJ.log(e.getMessage());
                IJ.error("Updater", "Signature verification failed");
                throw new RuntimeException(e);
            } catch (KeylessVerificationException e) {
                IJ.error("Updater", "Signature verification failed, the installer was not signed by the AIJ repository.");
                throw new RuntimeException(e);
            }

            return buffer;
        }

        return null;
    }

    private static byte[] downloadFile(String url, String hash, int maxRetries, int attempt) throws NoSuchAlgorithmException, IOException, InterruptedException, URISyntaxException {
        MessageDigest md = MessageDigest.getInstance("SHA256");

        byte[] buffer;
        try (InputStream in = new DigestInputStream(new ProgressTrackingInputStream(streamForUri(new URI(url))), md)) {
            buffer = in.readAllBytes();
        } catch (Exception e) {
            System.err.printf("Download error on attempt %d: %s%n", attempt, e.getMessage());
            if (attempt == maxRetries) throw e;
            wait(attempt);
            return null;
        }

        String actualSha256 = toHex(md.digest());
        System.out.println("Computed SHA256: " + actualSha256);
        System.out.println("Expected SHA256: " + hash);

        if (actualSha256.equalsIgnoreCase(hash)) {
            System.out.println("SHA256 matches expected.");
            return buffer;
        } else {
            System.err.printf("SHA256 mismatch on attempt %d (%s vs %s).%n",
                    attempt, actualSha256, hash);

            if (attempt == maxRetries) {
                throw new RuntimeException("SHA256 did not match after " + maxRetries + " attempts");
            }

            wait(attempt);
        }
        return null;
    }

    private static void wait(int attempt) {
        try {
            Thread.sleep(2_000L);
        } catch (InterruptedException ignored) { }
    }

    private static String toHex(byte[] bytes) {
        return HexFormat.of().formatHex(bytes);
    }

    public static BufferedReader readerForUri(URI uri, Charset charset) throws IOException, InterruptedException {
        String scheme = uri.getScheme();
        if (scheme == null || scheme.equalsIgnoreCase("file")) {
            return Files.newBufferedReader(Paths.get(uri), charset);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .GET()
                .build();

        HttpResponse<InputStream> response = HTTP_CLIENT.send(
                request,
                HttpResponse.BodyHandlers.ofInputStream()
        );

        if (response.statusCode() != 200) {
            throw new IllegalStateException("Failed to access file");
        }

        return new BufferedReader(new InputStreamReader(response.body(), charset));
    }

    public static ProgressTrackingInputStream.SizedInputStream streamForUri(URI uri) throws IOException, InterruptedException {
        String scheme = uri.getScheme();
        if (scheme == null || scheme.equalsIgnoreCase("file")) {
            var path = Paths.get(uri);
            return new ProgressTrackingInputStream.SizedInputStream(Files.newInputStream(path), Files.size(path));
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .GET()
                .build();

        HttpResponse<InputStream> response = HTTP_CLIENT.send(
                request,
                HttpResponse.BodyHandlers.ofInputStream()
        );

        if (response.statusCode() != 200) {
            throw new IllegalStateException("Failed to access file");
        }

        var contentLength = -1L;
        Optional<String> maybeCL = response.headers().firstValue("Content-Length");
        if (maybeCL.isPresent()) {
            try {
                contentLength = Long.parseLong(maybeCL.get());
            } catch (NumberFormatException ignored) {
            }
        }

        return new ProgressTrackingInputStream.SizedInputStream(response.body(), contentLength);
    }

    public static BufferedReader readerForUri(URI uri) throws IOException, InterruptedException {
        return readerForUri(uri, StandardCharsets.UTF_8);
    }

    public static Path getBaseDirectory(Class<?> cls) {
        try {
            var pd = cls.getProtectionDomain();
            var cs = pd.getCodeSource();
            if (cs == null) {
                throw new IllegalStateException("No code source for " + cls);
            }

            var location = cs.getLocation();  // e.g. file:/path/to/your.jar or file:/path/to/classes/
            var path = Paths.get(location.toURI());

            if (Files.isRegularFile(path)) {
                // Running from a JAR: use its parent folder
                return path.getParent();
            } else {
                // Running from classes directory
                return path;
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failed to resolve code source location", e);
        }
    }

    public void dialog() {
        var meta = fetchVersions();
        var versions = meta.versions();
        versions.sort(Comparator.comparing(MetaVersion.VersionEntry::version).reversed());

        var filterType = EnumSet.of(MetaVersion.ReleaseType.RELEASE);

        if (ENABLE_DAILY_BUILDS.get()) {
            filterType.add(MetaVersion.ReleaseType.DAILY_BUILD);
        }

        if (ENABLE_PRERELEASES.get()) {
            filterType.add(MetaVersion.ReleaseType.PRERELEASE);
        }

        var filteredVersions = versions.stream().filter(v -> filterType.contains(v.releaseType())).toList();

        var d = new JDialog(IJ.getInstance(), "AstroImageJ Updater");

        var b = Box.createVerticalBox();

        MultiLineLabel msg;
        if (hasUpdateAvailable()) {
            msg = new MultiLineLabel("""
                    A new version of AstroImageJ is available.
                    \s
                    You are currently running AstroImageJ %s.
                    \s
                    Click "Ok" to download and install the version you have selected below.
                    After a successful download, AstroImageJ will close.
                    Restart AstroImageJ to run the updated version.
                    \s
                    Click "Cancel" to continue using the current version.
            """.formatted(IJ.getAstroVersion()));
        } else {
            msg = new MultiLineLabel("""
                You are currently running AstroImageJ %s.
                \s
                To upgrade or downgrade to a different version, select it below.
                Click "OK" to download and install the version you have selected below.
                After a successful download, AstroImageJ will close.
                Restart AstroImageJ to run the updated version.
                \s
                Click "Cancel" to continue using the current version.
                """.formatted(IJ.getAstroVersion()));
        }
        b.add(msg);

        var enablePrereleases = new JCheckBox("Show Prereleases", ENABLE_PRERELEASES.get());
        b.add(enablePrereleases);

        var enableDailyBuilds = new JCheckBox("Show Daily Builds", ENABLE_DAILY_BUILDS.get());
        b.add(enableDailyBuilds);

        var updateCheckOnStartup = new JCheckBox("Perform Update Check on startup", Prefs.getBoolean(DO_UPDATE_NOTIFICATION, true));
        b.add(updateCheckOnStartup);

        var selector = new JComboBox<>(new Vector<>(filteredVersions));
        selector.setRenderer(new ToolTipRenderer());
        var selectorArea = Box.createHorizontalBox();
        selectorArea.add(Box.createHorizontalStrut(10));
        selectorArea.add(selector);
        selectorArea.add(Box.createHorizontalStrut(10));
        b.add(selectorArea);
        b.add(Box.createVerticalStrut(10));

        enableDailyBuilds.addActionListener($ -> ENABLE_DAILY_BUILDS.set(enableDailyBuilds.isSelected()));
        enablePrereleases.addActionListener($ -> ENABLE_PRERELEASES.set(enablePrereleases.isSelected()));

        ENABLE_DAILY_BUILDS.addListener(((key, newValue) -> {
            var filter = EnumSet.of(MetaVersion.ReleaseType.RELEASE);

            if (newValue) {
                filter.add(MetaVersion.ReleaseType.DAILY_BUILD);
            }

            if (ENABLE_PRERELEASES.get()) {
                filter.add(MetaVersion.ReleaseType.PRERELEASE);
            }

            var filtered = versions.stream().filter(v -> filter.contains(v.releaseType())).toList();

            selector.removeAllItems();
            selector.setModel(new DefaultComboBoxModel<>(new Vector<>(filtered)));
        }));

        ENABLE_PRERELEASES.addListener(((key, newValue) -> {
            var filter = EnumSet.of(MetaVersion.ReleaseType.RELEASE);

            if (newValue) {
                filter.add(MetaVersion.ReleaseType.PRERELEASE);
            }

            if (ENABLE_DAILY_BUILDS.get()) {
                filter.add(MetaVersion.ReleaseType.DAILY_BUILD);
            }

            var filtered = versions.stream().filter(v -> filter.contains(v.releaseType())).toList();

            selector.removeAllItems();
            selector.setModel(new DefaultComboBoxModel<>(new Vector<>(filtered)));
        }));

        updateCheckOnStartup.addActionListener($ -> {
            Prefs.set(DO_UPDATE_NOTIFICATION.substring(1), updateCheckOnStartup.isSelected());
        });

        var buttons = Box.createHorizontalBox();

        var yes = new JButton("Ok");
        var cancel = new JButton("Cancel");

        yes.addActionListener($ -> {
            if (((MetaVersion.VersionEntry) selector.getSelectedItem()).version().equals(new SemanticVersion(IJ.getAstroVersion()))) {
                IJ.error("You are already running the selected version of AstroImageJ.");
                return;
            }

            d.dispose();
            try {
                Executors.newSingleThreadExecutor().submit(() -> {
                    try {
                        downloadSpecificVersion((MetaVersion.VersionEntry) selector.getSelectedItem());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        cancel.addActionListener($ -> {
            d.dispose();
            System.out.println(selector.getSelectedItem());
        });

        var changes = new JButton("Changelog");
        changes.addActionListener($ -> {
            try {
                BrowserOpener.openURL("https://astroimagej.com/releases");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        buttons.add(Box.createHorizontalStrut(10));
        buttons.add(changes);
        buttons.add(Box.createHorizontalGlue());

        buttons.add(yes);
        buttons.add(cancel);
        buttons.add(Box.createHorizontalStrut(10));

        b.add(buttons);
        b.add(Box.createVerticalStrut(10));

        d.add(b);
        d.pack();
        d.doLayout();

        UIHelper.setCenteredOnScreen(d, IJ.getInstance());

        d.setVisible(true);
    }

    @Override
    public void run(String arg) {
        if ("check".equals(arg)) {
            updateCheck();
            return;
        }

        update();
    }

    private record Download(byte[] bytes) {}

    private static String getScriptPath() {
        if (IJ.isWindows()) {
            return "Astronomy/updater/windows.bat";
        }

        if (IJ.isLinux()) {
            return "Astronomy/updater/linux.sh";
        }

        if (IJ.isMacOSX()) {
            return "Astronomy/updater/mac.sh";
        }

        throw new IllegalStateException();
    }
}
