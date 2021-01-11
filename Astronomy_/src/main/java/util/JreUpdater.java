package util;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.file.TVFS;
import ij.IJ;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Objects;

public class JreUpdater {

    private static String os = getOs();
    private static String hw_bitness = "64"; // AIJ only supports 64bit (though probably works on 32bit if default memory is lowered)
    private static String arch = getArch();
    private static String separator = "&";
    private static int javaVersion = 13;
    private static String baseUrl = "https://api.azul.com/zulu/download/community/v1.0/bundles/latest/";

    public static void updateJre() {
        URL detailUrl;
        if ((detailUrl = getDetailUrl(javaVersion)) != null) {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(detailUrl.openStream()));
                StringBuilder detailsStr = new StringBuilder();
                String in;
                while ((in = reader.readLine()) != null) detailsStr.append(in);

                JsonObject details = Json.parse(detailsStr.toString()).asObject();
                String downloadUrl = details.getString("url", "");
                String sha256Hash = details.getString("sha256_hash", "");
                String md5Hash = details.getString("md5_hash", "");
                String ext = details.getString("ext", "");
                Number size = details.getLong("size", 0);
                String name = details.getString("name", "").replace("." + ext, "");
                String type = details.getString("bundle_type", "jre");
                if (os.equals("macos")) {
                    name += "/zulu-" + javaVersion + "." + type + "/Contents/Home";
                }
                if (downloadFile(downloadUrl, ext, size)) {
                    TFile tf = new TFile(Paths.get("", "tempJRE" + "." + ext, name).toString());
                    Path temp = Paths.get("", os.equals("windows") ? "tempJRE" : os.equals("macos") ? "../PlugIns/jre/Contents/Home" : "jre");

                    tf.mv(temp.toFile());

                    //Files.move(temp, Paths.get("", "jre"), StandardCopyOption.REPLACE_EXISTING);
                    Paths.get("", "tempJRE" + "." + ext).toFile().delete();

                    TVFS.umount();

                    if (os.equals("windows")) {
                        IJ.log("Running elevator...");
                        runElevator();
                    }

                } else {
                    IJ.error("Failed to download JRE");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        updateJre();
    }

    private static void runElevator() {
        ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", "start /wait \"AIJ Java Updater\" TIMEOUT /T 5 /NOBREAK && rmdir /Q /S jre && ren tempJRE jre");
        builder.directory(Paths.get("").toAbsolutePath().toFile());
        IJ.log(Paths.get("").toAbsolutePath().toString());
        try {
            builder.start();
            System.exit(0);
        } catch (IOException e) {
            IJ.log(e.getMessage());
        }

    }

    private static boolean downloadFile(String link, String ext, Number size) {
        try {
            URL binaryUrl = new URL(link);
            ReadableByteChannel channel = Channels.newChannel(binaryUrl.openStream());
            FileOutputStream fos = new FileOutputStream(Paths.get("", "tempJRE" + "." + ext).toFile());
            FileChannel fChannel = fos.getChannel();
            long tSize = fChannel.transferFrom(channel, 0, Long.MAX_VALUE);
            fChannel.close();
            fos.close();
            channel.close();
            //TODO check file hashes to confirm download
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static String getHash(String hashType, File file) {
        try {
            MessageDigest digest = MessageDigest.getInstance(hashType);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    private static URL getDetailUrl(int javaMajorVersion) {
        try {
            return new URL(urlBuilder(javaMajorVersion));
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String urlBuilder(int javaMajorVersion) {
        String url = baseUrl + '?' +
                "jdk_version" + '=' + javaMajorVersion + separator +
                "os" + '=' + os + separator +
                "arch" + '=' + arch + separator +
                "hw_bitness" + '=' + hw_bitness + separator +
                "ext" + '=' + (os.equals("linux") ? "tar.gz" : "zip");
        return url;
    }

    /**
     * Transform current OS into a string the api can handle.
     * Acceptable values: linux, linux_musl, macos, windows, solaris, qnx
     */
    private static String getOs() {
        String osname = System.getProperty("os.name", "other").toLowerCase(Locale.ENGLISH);
        if (osname.contains("mac") || osname.contains("darwin")) { // Darwin is macos, sometimes reported wrongly
            return "macos";
        } else if (osname.contains("win")) {
            return "windows";
        } else if (osname.contains("nux")) {
            return "linux";
        } else {
            return osname;
        }
    }

    /**
     * Transform current arch into a string the api can handle.
     */
    private static String getArch() {
        String arch = System.getProperty("os.arch").toLowerCase(Locale.ENGLISH);
        if (arch.contains("arm") || arch.contains("aarch")) {
            return "arm";
        } else if (arch.contains("mips")) {
            return "mips";
        } else if (arch.contains("x86") || arch.contains("i3") || arch.contains("i7") || arch.contains("amd")) {
            return "x86";
        } else {
            return arch;
        }
    }



}
