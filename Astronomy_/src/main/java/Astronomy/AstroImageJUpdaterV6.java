package Astronomy;

import Astronomy.updater.MetaVersion;
import Astronomy.updater.SpecificVersion;
import ij.IJ;
import ij.ImageJ;
import ij.gui.MultiLineLabel;

import javax.net.ssl.*;
import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Vector;

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

    //todo only move files to final destination if all succeed
    public void downloadSpecificVersion(MetaVersion.VersionEntry entry) throws Exception {
        var version = SpecificVersion.readJson(new URI(entry.url()));

        var baseDir = getBaseDirectory(ImageJ.class).toAbsolutePath().normalize();
        System.out.println(baseDir);
        for (SpecificVersion.FileEntry fe : version.files()) {
            fe.os();//todo os check

            Path destDir = baseDir
                    .resolve(fe.destination().isBlank() ? "" : fe.destination())
                    .normalize();

            // **Security check**: must stay under baseDir
            if (!destDir.startsWith(baseDir.normalize())) {
                throw new IOException("Invalid destination escapes baseDir: " + fe.destination());
            }

            // Ensure directory exists
            Files.createDirectories(destDir);

            // Final target path
            Path targetFile = destDir.resolve(fe.name());

            System.out.printf("Downloading %s -> %s%n", fe.url(), targetFile);

            downloadAndComputeHash(fe.url(), targetFile, 4, fe.md5());
        }
    }

    public boolean downloadAndComputeHash(String urlStr, Path finalFile, int maxRetries, String expectedMd5) throws Exception {
        var tempFile = Files.createTempFile(finalFile.getParent(), finalFile.getFileName().toString(), null);

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            System.out.printf("Attempt %d of %d...%n", attempt, maxRetries);

            MessageDigest md = MessageDigest.getInstance("MD5");//todo sha256 - 64chars of hex

            try (InputStream in = new DigestInputStream(streamForUri(new URI(urlStr)), md);
                 FileOutputStream out = new FileOutputStream(tempFile.toFile())) {

                byte[] buffer = new byte[8 * 1024];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            } catch (Exception e) {
                System.err.printf("Download error on attempt %d: %s%n", attempt, e.getMessage());
                if (attempt == maxRetries) throw e;
                wait(attempt);
                continue;
            }

            String actualMd5 = toHex(md.digest());
            System.out.println("Computed MD5: " + actualMd5);
            System.out.println("Expected MD5: " + expectedMd5);

            if (actualMd5.equalsIgnoreCase(expectedMd5)) {
                //todo try this instead?
                // https://stackoverflow.com/questions/65062547/what-is-the-java-nio-file-files-equivalent-of-java-io-file-setwritable
                if (!Files.isWritable(finalFile)) {
                    if (!finalFile.toFile().setWritable(true, true)) {
                        System.out.println("Failed to set file permissions");
                        return false;
                    }
                }
                //todo instead of writing to temp file, keep in memory and write directly like current updater does
                Files.move(tempFile, finalFile, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("MD5 matches expected. File moved to " + finalFile);
                return true;
            } else {
                System.err.printf("MD5 mismatch on attempt %d (%s vs %s).%n",
                        attempt, actualMd5, expectedMd5);

                Files.deleteIfExists(tempFile);

                if (attempt == maxRetries) {
                    throw new RuntimeException("MD5 did not match after " + maxRetries + " attempts");
                }

                wait(attempt);
            }
        }

        return false;
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
                //todo impl
            } else {

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
}
