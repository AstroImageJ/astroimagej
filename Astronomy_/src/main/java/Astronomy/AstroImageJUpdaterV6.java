package Astronomy;

import Astronomy.updater.MetaVersion;
import Astronomy.updater.SpecificVersion;
import ij.IJ;
import ij.ImageJ;
import ij.astro.util.ProgressTrackingInputStream;
import ij.gui.MultiLineLabel;

import javax.net.ssl.*;
import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;

import static java.nio.file.attribute.PosixFilePermission.*;

public class AstroImageJUpdaterV6 {
    private static final URI META;
    private static final HttpClient INSECURE_CLIENT;
    private static final TrustManager MOCK_MANAGER = new X509ExtendedTrustManager() {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {

        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {

        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {

        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {

        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    };

    static {
        try {
            META = new URI("https://astroimagej.com/meta/versions.json");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        try {
            //todo actually setup certificate on the domain
            //todo github seems to handle this?
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{MOCK_MANAGER}, new SecureRandom());

            SSLParameters sslParams = sslContext.getDefaultSSLParameters();
            sslParams.setEndpointIdentificationAlgorithm(null);

            INSECURE_CLIENT = HttpClient.newBuilder()
                    .sslContext(sslContext)
                    .sslParameters(sslParams)
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .build();

        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public MetaVersion fetchVersions() {
        return MetaVersion.readJson(META);
    }

    public void downloadSpecificVersion(MetaVersion.VersionEntry entry) throws Exception {
        var version = SpecificVersion.readJson(new URI(entry.url()));

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

        SpecificVersion.FileEntry fileEntry = null;
        for (SpecificVersion.FileEntry file : version.files()) {
            if (file.matchesSystem()) {
                fileEntry = file;
            }
        }

        if (fileEntry == null) {
            //todo
        }

        var pid = ProcessHandle.current().pid();

        // Manually manage temp folder and deletion as Windows doesn't automatically clean them
        var tmpFolder = Path.of(System.getProperty("java.io.tmpdir")).resolve("aij-updater");

        Files.walkFileTree(tmpFolder,
                new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult postVisitDirectory(
                            Path dir, IOException exc) throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(
                            Path file, BasicFileAttributes attrs)
                            throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }
                });

        if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
            Files.createDirectory(tmpFolder, PosixFilePermissions.asFileAttribute(EnumSet
                    .of(OWNER_READ, OWNER_WRITE, OWNER_EXECUTE)));
        } else {
            Files.createDirectory(tmpFolder);
        }

        var tmp = tmpFolder.resolve("updateScript" + (IJ.isWindows() ? ".bat" : ".sh"));

        // Copy script to location for execution
        Files.copy(Objects.requireNonNull(AstroImageJUpdaterV6.class.getClassLoader().getResourceAsStream(getScriptPath())), tmp, StandardCopyOption.REPLACE_EXISTING);

        var inst = tmpFolder.resolve("installer");

        Files.write(inst, downloadAndComputeHash(fileEntry, 5));

        if (IJ.isWindows()) {
            ProcessBuilder pb = new ProcessBuilder(
                    "cmd", "/c",
                    "start", "\"\"", "/b",
                    tmp.toAbsolutePath().toString(),
                    Long.toString(pid),
                    inst.toAbsolutePath().toString(),
                    baseDir.toAbsolutePath().toString()
            );
            Process p = pb.start();

            System.exit(0);
        } else if (IJ.isMacOSX() || IJ.isLinux()) {
            var perms = Files.getPosixFilePermissions(tmp);
            perms.add(PosixFilePermission.OWNER_EXECUTE);
            Files.setPosixFilePermissions(tmp, perms);
            ProcessBuilder pb = new ProcessBuilder(
                    tmp.toAbsolutePath().toString(),
                    Long.toString(pid),
                    inst.toAbsolutePath().toString(),
                    baseDir.toAbsolutePath().toString()
            );
            Process p = pb.start();

            System.exit(0);
        }

        //todo error
    }

    public byte[] downloadAndComputeHash(SpecificVersion.FileEntry fileEntry, int maxRetries) throws Exception {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            System.out.printf("Attempt %d of %d...%n", attempt, maxRetries);

            MessageDigest md = MessageDigest.getInstance("SHA256");

            byte[] buffer;
            try (InputStream in = new DigestInputStream(new ProgressTrackingInputStream(streamForUri(new URI(fileEntry.url()))), md)) {
                buffer = in.readAllBytes();
            } catch (Exception e) {
                System.err.printf("Download error on attempt %d: %s%n", attempt, e.getMessage());
                if (attempt == maxRetries) throw e;
                wait(attempt);
                continue;
            }

            String actualSha256 = toHex(md.digest());
            System.out.println("Computed SHA256: " + actualSha256);
            System.out.println("Expected SHA256: " + fileEntry.sha256());

            if (actualSha256.equalsIgnoreCase(fileEntry.sha256())) {
                System.out.println("SHA256 matches expected.");
                return buffer;
            } else {
                System.err.printf("SHA256 mismatch on attempt %d (%s vs %s).%n",
                        attempt, actualSha256, fileEntry.sha256());

                if (attempt == maxRetries) {
                    throw new RuntimeException("SHA256 did not match after " + maxRetries + " attempts");
                }

                wait(attempt);
            }
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

        HttpResponse<InputStream> response = INSECURE_CLIENT.send(
                request,
                HttpResponse.BodyHandlers.ofInputStream()
        );

        if (response.statusCode() != 200) {
            throw new IllegalStateException("Failed to access file");
        }

        return new BufferedReader(new InputStreamReader(response.body(), charset));
    }

    public static InputStream streamForUri(URI uri) throws IOException, InterruptedException {
        String scheme = uri.getScheme();
        if (scheme == null || scheme.equalsIgnoreCase("file")) {
            return Files.newInputStream(Paths.get(uri));
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .GET()
                .build();

        HttpResponse<InputStream> response = INSECURE_CLIENT.send(
                request,
                HttpResponse.BodyHandlers.ofInputStream()
        );

        if (response.statusCode() != 200) {
            throw new IllegalStateException("Failed to access file");
        }

        return response.body();
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
        versions.sort(Comparator.comparing(MetaVersion.VersionEntry::version));
        var releaseOnlyVersions = versions.stream()
                .filter(v -> v.releaseType() == MetaVersion.ReleaseType.RELEASE).toList();

        var d = new JDialog(IJ.getInstance(), "AstroImageJ Updater");

        var b = Box.createVerticalBox();

        b.add(new MultiLineLabel("""
                You are currently running AstroImageJ %s.
                \s
                To upgrade or downgrade to a different version, select it below.
                Click "OK" to download and install the version you have selected below.
                After a successful download, AstroImageJ will close.
                Restart AstroImageJ to run the updated version.
                \s
                Click "Cancel" to continue using the current version.
                """.formatted(IJ.getAstroVersion())));

        var enablePrereleases = new JCheckBox("Show Prereleases", false);
        b.add(enablePrereleases);

        var selector = new JComboBox<>(new Vector<>(releaseOnlyVersions));
        b.add(selector);

        enablePrereleases.addActionListener($ -> {
            if (enablePrereleases.isSelected()) {
                selector.removeAllItems();
                selector.setModel(new DefaultComboBoxModel<>(new Vector<>(versions)));
            } else {
                selector.removeAllItems();
                selector.setModel(new DefaultComboBoxModel<>(new Vector<>(releaseOnlyVersions)));
            }
        });

        var buttons = Box.createHorizontalBox();

        var yes = new JButton("Ok");
        var cancel = new JButton("Cancel");

        yes.addActionListener($ -> {
            d.dispose();
            System.out.println(selector.getSelectedItem());
            try {
                downloadSpecificVersion((MetaVersion.VersionEntry) selector.getSelectedItem());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        cancel.addActionListener($ -> {
            d.dispose();
            System.out.println(selector.getSelectedItem());
        });

        buttons.add(yes);
        buttons.add(cancel);

        b.add(buttons);

        d.add(b);
        d.pack();
        d.doLayout();
        d.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            AstroImageJUpdaterV6.META.hashCode();
            var u = new AstroImageJUpdaterV6();
            u.dialog();
        });
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
