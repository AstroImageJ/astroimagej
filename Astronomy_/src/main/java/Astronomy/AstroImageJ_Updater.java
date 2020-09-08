package Astronomy;//package ij.plugin;
import ij.*;
import ij.gui.*;
import ij.plugin.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.SecureRandom; 
import java.security.GeneralSecurityException;


/** This plugin implements the Help/Update AstroImageJ command. */
public class AstroImageJ_Updater implements PlugIn {

    public static final String URL6 = "http://www.astro.louisville.edu/software/astroimagej/updates";
    
    public static final String URL8 = "http://www.astro.louisville.edu/software/astroimagej/updates/updatesjava8";
    
    public static String URL = URL6;
    
    String[] versionPieces = IJ.getAstroVersion().split("\\.");
    int majorVersion = Integer.parseInt(versionPieces[0]);
    
    static {
        final TrustManager[] trustAllCertificates = new TrustManager[] {
            new X509TrustManager() {
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null; // Not relevant.
                }
                @Override
                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    // Do nothing. Just allow them all.
                }
                @Override
                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    // Do nothing. Just allow them all.
                }
            }
        };

        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCertificates, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (GeneralSecurityException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

	public void run(String arg) { 
		
//        if (arg.equals("menus"))
//			{updateMenus(); return;}
		if (IJ.getApplet()!=null) return;
		//File file = new File(Prefs.getHomeDir() + File.separator + "ij.jar");
		//if (isMac() && !file.exists())
		//	file = new File(Prefs.getHomeDir() + File.separator + "ImageJ.app/Contents/Resources/Java/ij.jar");

        
		URL url = getClass().getResource("/ij/IJ.class");
		String ij_jar = url == null ? null : url.toString().replaceAll("%20", " ");
		if (ij_jar==null || !ij_jar.startsWith("jar:file:")) {
			error("Could not determine location of ij.jar");
			return;
		}
		int exclamation = ij_jar.indexOf('!');
		ij_jar = ij_jar.substring(9, exclamation);
		if (IJ.debugMode) IJ.log("Updater: "+ij_jar);
		File file1 = new File(ij_jar);
		if (!file1.exists()) {
			error("File not found: "+file1.getPath());
			return;
		}
		if (!file1.canWrite()) {
			String msg = "No write access: "+file1.getPath();
			error(msg);
			return;
		}
        
		url = getClass().getResource("/AstroImageJ_Updater.class");
		String Astronomy_jar = url == null ? null : url.toString().replaceAll("%20", " ");
		if (Astronomy_jar==null || !Astronomy_jar.startsWith("jar:file:")) {
			error("Could not determine location of Astronomy_.jar");
			return;
		}
		exclamation = Astronomy_jar.indexOf('!');
		Astronomy_jar = Astronomy_jar.substring(9, exclamation);
		if (IJ.debugMode) IJ.log("Updater: "+Astronomy_jar);
		File file2 = new File(Astronomy_jar);
		if (!file2.exists()) {
			error("File not found: "+file2.getPath());
			return;
		}
		if (!file2.canWrite()) {
			String msg = "No write access: "+file2.getPath();
			error(msg);
			return;
		}        
        
//		url = getClass().getResource(file.getParent().replace('\\', '/') +"/macros/StartupMacros.txt");
//		String Startup_Macros = url == null ? null : url.toString().replaceAll("%20", " ");
//        IJ.showMessage(Startup_Macros);
//		if (Startup_Macros==null || !Startup_Macros.startsWith("jar:file:")) {
//			error("Could not determine location of StartupMacros.txt");
//			return;
//		}
//		exclamation = Startup_Macros.indexOf('!');
//		Startup_Macros = Startup_Macros.substring(9, exclamation);
//		if (IJ.debugMode) IJ.log("Updater: "+Startup_Macros);
		File file3 = new File(file2.getParent() + "/../macros/StartupMacros.txt");
		if (!file3.exists()) {
			error("File not found: "+file3.getPath());
			return;
		}
		if (!file3.canWrite()) {
			String msg = "No write access: "+file3.getPath();
			error(msg);
			return;
		}    
        
        File file4=null;
        if (IJ.isMacOSX())
            {
            file4 = new File(file1.getParent() + "/../../Info.plist");
            if (!file4.exists()) 
                {
                file4 = new File(file1.getParent() + "/../Info.plist");
                }
            }
        
        
        if (majorVersion>3)
            {
            URL = URL8;
            }
        
		String[] list = openUrlAsList(URL+"/versions.txt");
        if (list == null )
            {
            IJ.showMessage("Network Error", "<html>Network Error!<br>"+
                                                  "Check your internet connection. If good, the update<br>"+
                                                  "server at the University of Louisville may be down.<br>"+
                                                  "In that case, try the update at a later time.</html>");
            return;
            }
		int count = list.length + 1;
		String[] versions = new String[count];
		String[] urls = new String[count];
        String[] AstronomyUrls = new String[count];
        String[] StartupMacrosUrls = new String[count];
//		String uv = getUpgradeVersion();
//		if (uv==null) return;
		versions[0] = "daily build";//"v"+uv;
		urls[0] = URL+"/ij.jar";//"/upgrade/ij.jar";
        AstronomyUrls[0] = URL+"/Astronomy_.jar";
        StartupMacrosUrls[0] = URL+"/StartupMacros.txt";
		if (versions[0]==null) return;
		for (int i=1; i<count; i++) {
			String version = list[i-1];
			versions[i] = version;//.substring(0,version.length()-1); // remove letter
			urls[i] = URL+"/ij"+version+".jar";//.substring(1,2)+version.substring(3,6)+".jar";
            AstronomyUrls[i] = URL+"/Astronomy_"+version+".jar";
            StartupMacrosUrls[i] = URL+"/StartupMacros"+version+".txt";
		}
//		versions[count-1] = "daily build";
//		urls[count-1] = URL+"/ij.jar";
		int choice = showDialog(versions);
		if (choice==-1) return;
//		if (!versions[choice].startsWith("daily") && versions[choice].compareTo("v1.39")<0
//		&& Menus.getCommands().get("AstroImageJ Updater")==null) {
//			String msg = "This command is not available in versions of ImageJ prior\n"+
//			"to 1.39 so you will need to install the plugin version at\n"+
//			"<"+URL+"/plugins/imagej-updater.html>.";
//			if (!IJ.showMessageWithCancel("Update AstroImageJ", msg))
//				return;
//		}
        
       
		byte[] jar = getFile(urls[choice], "ij"+(choice==0?"":versions[choice])+".jar");
		if (jar==null) {
			error("Unable to download ij.jar from "+urls[choice]);
			return;
		}
		byte[] Astro_jar = getFile(AstronomyUrls[choice], "Astronomy_"+(choice==0?"":versions[choice])+".jar");
		if (Astro_jar==null) {
			error("Unable to download Astronomy_.jar from "+AstronomyUrls[choice]);
			return;
		}
        byte[] StartupMacro = getFile(StartupMacrosUrls[choice], "StartupMacros"+(choice==0?"":versions[choice])+".txt");
		if (StartupMacro==null) {
			error("Unable to download StartupMacros.txt from "+StartupMacrosUrls[choice]);
			return;
		}
		//file.renameTo(new File(file.getParent()+File.separator+"ij.bak"));
//		if (version().compareTo("1.37v")>=0)
		Prefs.savePreferences();
		// if (!renameJar(file)) return; // doesn't work on Vista

		saveFile(file1, jar);
        saveFile(file2, Astro_jar);
        saveFile(file3, StartupMacro);
        
        if (IJ.isMacOSX() && file4!=null)
            {
            if (!file4.exists()) {
                IJ.showStatus("AIJ version number not updated in Info.plist. File not found.");
                error("AIJ version number not updated in Info.plist. File not found: "+file4.getPath());
                System.exit(0);
                }
            if (!file4.canRead()) {
                IJ.showStatus("AIJ version number not updated in Info.plist. No read access.");
                error("AIJ version number not updated in Info.plist. No read access: "+file4.getPath());
                System.exit(0);
                }        
            if (!file4.canWrite()) {
                IJ.showStatus("AIJ version number not updated in Info.plist. No write access.");
                error("AIJ version number not updated in Info.plist. No write access: "+file4.getPath());
                System.exit(0);
                }       
            IJ.showStatus("Updating Info.plist");
            Scanner inFile=null;
            List<String> plist = new ArrayList<String>();
            try {
                inFile = new Scanner(file4);
                while(inFile.hasNextLine())
                    {
                    plist.add(inFile.nextLine());
                    }
                }
            catch (IOException e)
                {
                IJ.showStatus("AIJ version number not updated in Info.plist. Error reading file.");
                error("AIJ version number not updated in Info.plist. Error reading "+file4.getPath());
                System.exit(0);
                }
            if (inFile!=null) inFile.close();
            String[] infoPlist = plist.toArray(new String[plist.size()]);
            String newVersion = versions[choice];
            if (choice==0)
                {
                if (versions.length<2)
                    {
                    newVersion = "db0.0.0";
                    }
                else
                    {
                    String[] subVer = versions[1].split("\\.");
                    int minorVer = parseInteger(subVer[subVer.length-1], 98);
                    minorVer++;
                    subVer[subVer.length-1]=Integer.toString(minorVer);
                    newVersion = "db"+subVer[0];
                    if (subVer.length>1)
                        {
                        for (int i=1; i<subVer.length;i++)
                            {
                            newVersion+="."+subVer[i];
                            }
                        }
                    }
                }
            for (int i=0; i<infoPlist.length;i++)
                {
                if (infoPlist[i].contains("<key>CFBundleVersion</key>"))
                        {
                        if ((i+1) < infoPlist.length)
                            {
                            infoPlist[i+1]="\t<string>"+newVersion+"</string>";
                            }
                        }
                if (infoPlist[i].contains("<key>CFBundleShortVersionString</key>"))
                        {
                        if ((i+1) < infoPlist.length)
                            {
                            infoPlist[i+1]="\t<string>"+newVersion+"</string>";
                            }
                        }            
                }        

//        for (String s : infoPlist)
//            {
//            IJ.log(s);
//            }
            file4.delete();
            try {
                FileWriter fileWriter = new FileWriter(file4);
                for (int i=0; i<infoPlist.length;i++)
                    {
                    fileWriter.write(infoPlist[i] + "\n");
                    }
                fileWriter.flush();
                fileWriter.close();
                } 
            catch (IOException e) 
                {
                IJ.showStatus("AIJ version number not updated in Info.plist. Error writing file.");
                error("AIJ version number not updated in Info.plist. Error writing "+file4.getPath());
                }
            }  
//		if (choice<count-1) // force macro Function Finder to download fresh list
//			new File(IJ.getDirectory("macros")+"functions.html").delete();
		System.exit(0);
	}

	int showDialog(String[] versions) {
		GenericDialog gd = new GenericDialog("AstroImageJ Updater");
		gd.addChoice("Upgrade To:", versions, versions[(versions.length > 0 ? 0 : 0)]);
		String msg = 
			"You are currently running AstroImageJ "+IJ.getAstroVersion()+".\n"+
			" \n"+
			"Click \"OK\", to download and install the selected upgrade.\n"+
            "After a successful download, AstroImageJ will exit.\n"+
			"Restart AstroImageJ to run the upgraded version.\n";

		gd.addMessage(msg);
        gd.addHelp(URL+"/release_notes.html");
        gd.setHelpLabel("Release Notes");
		gd.showDialog();
		if (gd.wasCanceled())
			return -1;
		else
			return gd.getNextChoiceIndex();
	}

//	String getUpgradeVersion() {
//		String url = IJ.URL+"/notes.html";
//		String notes = openUrlAsString(url, 20);
//		if (notes==null) {
//			error("Unable to connect to "+IJ.URL+". You\n"
//				+"may need to use the Edit>Options>Proxy Settings\n"
//				+"command to configure ImageJ to use a proxy server.");
//			return null;
//		}
//		int index = notes.indexOf("Version ");
//		if (index==-1) {
//			error("Release notes are not in the expected format");
//			return null;
//		}
//		String version = notes.substring(index+8, index+13);
//		return version;
//	}

	String openUrlAsString(String address, int maxLines) {
		StringBuffer sb;
		try {
			URL url = new URL(address);
			InputStream in = url.openStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			sb = new StringBuffer();
			int count = 0;
			String line;
			while ((line=br.readLine())!=null && count++<maxLines)
				sb.append (line + "\n");
			in.close ();
		} catch (IOException e) {sb = null;}
			return sb!=null?new String(sb):null;
	}

	byte[] getFile(String address, String name) {
		byte[] data;
		try {
			URL url = new URL(address);
			IJ.showStatus("Connecting to "+URL);
			URLConnection uc = url.openConnection();
			int len = uc.getContentLength();
			if (len<=0)
				return null;
			IJ.showStatus("Downloading "+name+" ("+IJ.d2s((double)len/1048576,1)+"MB)");
			InputStream in = uc.getInputStream();
			data = new byte[len];
			int n = 0;
			while (n < len) {
				int count = in.read(data, n, len - n);
				if (count<0)
					throw new EOFException();
	   			 n += count;
				IJ.showProgress(n, len);
			}
			in.close();
		} catch (IOException e) {
			return null;
		}
		return data;
	}

	/*Changes the name of ij.jar to ij-old.jar
	boolean renameJar(File f) {
		File backup = new File(Prefs.getHomeDir() + File.separator + "ij-old.jar");
		if (backup.exists()) {
			if (!backup.delete()) {
				error("Unable to delete backup: "+backup.getPath());
				return false;
			}
		}
		if (!f.renameTo(backup)) {
			error("Unable to rename to ij-old.jar: "+f.getPath());
			return false;
		}
		return true;
	}
	*/

	void saveFile(File f, byte[] data) {
		try {
			FileOutputStream out = new FileOutputStream(f);
			out.write(data, 0, data.length);
			out.close();
		} catch (IOException e) {
		}
	}

	String[] openUrlAsList(String address) {
		IJ.showStatus("Connecting to "+URL);
		Vector v = new Vector();
		try {
			URL url = new URL(address);
            //url.openConnection().setReadTimeout(10000);
            //url.openConnection().setConnectTimeout(10000);
           
			InputStream in = url.openStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String line;
			while (true) {
				line = br.readLine();
				if (line==null) break;
				if (!line.equals("")) v.addElement(line);
			}
			br.close();
		} catch(Exception e) { }
        if (v.size()==0) return null;
		String[] lines = new String[v.size()];
		v.copyInto((String[])lines);
		IJ.showStatus("");
		return lines;
	}

	// Use reflection to get version since early versions
	// of ImageJ do not have the IJ.getVersion() method.
//	String version() {
//		String version = "";
//		try {
//			Class ijClass = ImageJ.class;
//			Field field = ijClass.getField("VERSION");
//			version = (String)field.get(ijClass);
//		}catch (Exception ex) {}
//		return version;
//	}
    
	public static int parseInteger(String s, int defaultValue) {
		if (s==null) return defaultValue;
		try {
			defaultValue = Integer.parseInt(s);
            } 
        catch (NumberFormatException e) {}
		return defaultValue;			
	}    

	boolean isMac() {
		String osname = System.getProperty("os.name");
		return osname.startsWith("Mac");
	}
	
	void error(String msg) {
        IJ.beep();
		IJ.error("AstroImageJ Updater", msg);
	}
	
//	void updateMenus() {
//		if (IJ.debugMode) {
//			long start = System.currentTimeMillis();
//			Menus.updateImageJMenus();
//			IJ.log("Refresh Menus: "+(System.currentTimeMillis()-start)+" ms");
//		} else
//			Menus.updateImageJMenus();
//	}

}
