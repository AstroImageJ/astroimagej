package astroj;

import ij.*;
import ij.gui.*;
import ij.process.*;

import java.awt.*;
import java.util.*;

import astroj.json.simple.*;
import static astroj.json.simple.JSONValue.*;
import astroj.json.simple.parser.*;

import java.text.*;
import java.net.*;
import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 
 */
public class Astrometry //implements KeyListener
	{
    ImagePlus imp, imp2, impOriginal, impRaw;
    int width, height;
    String defaultAstrometryUrlBase = "http://nova.astrometry.net";
//    String urlBase = "http://supernova.astrometry.net/";
    boolean useAlternateAstrometryServer = false;
    String alternateAstrometryUrlBase = "http://127.0.0.1:8080";    
    String slash = "/";
    boolean showLog = true;
    boolean showLogDateTime = true;
    boolean resaveRaw = false;
    boolean removeBackStars = true;
    Calendar cal = Calendar.getInstance();
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
    
    Centroid centroid = new Centroid();
    
    boolean processStack = true;
    int startSlice = 1;
    int endSlice = 1;   
    
    int maxRetries = 1; // 1 -> 2 tries
    int retries = 0;
    
    String userKey = "";
    
    boolean autoSave = false;
    boolean DPSaveRawWithWCS = false;
    boolean skipIfHasWCS = true;
    
    boolean annotate = true;
    double annotateRadius = 30;
    boolean addAnnotationsToHeader = true;
    
    boolean useMedianFilter = true;
    int medianFilterRadius = 2;
    
    double minPeakFindToleranceSTDEV = 1;
    boolean useMaxPeakFindValue = false;
    double maxPeakFindValue = 50000;
    
    int maxNumStars = 50;
    
    boolean useCentroid = false;
    boolean backPlane = false;
    double apertureRadius = 20;
    double apertureBack1 = 30;
    double apertureBack2 = 40;
    
    boolean useScale = false;
    double scaleEstimate = 1;  //arcsecperpixel
    double scaleError = 0.1;   //arcsecperpixel   

    
    boolean useRaDec = false;
    double ra = 0;  //degrees
    double dec = 0;  //degrees
    double raDecRadius = 40.0;  //degrees   
    
    boolean useDistortionOrder = true;
    int distortionOrder = 2;  //number of SIP distortion coefficients requested
    int minOrder = 2;
    int maxOrder = 9;
        
    boolean canceled = false;
    boolean setupCanceled = false;
    boolean setupActive = false;
    
    double scale_lower = 0.9;
    double scale_upper = 1.1;     
    
    private int[] dirOffset;                        // pixel offsets of neighbor pixels for direct addressing
    final static int[] DIR_X_OFFSET = new int[] {  0,  1,  1,  1,  0, -1, -1, -1 };
    final static int[] DIR_Y_OFFSET = new int[] { -1, -1,  0,  1,  1,  1,  0, -1 };  
    
    final static byte MAXIMUM = (byte)1;            // marks local maxima (irrespective of noise tolerance)
    final static byte LISTED = (byte)2;             // marks points currently in the list
    final static byte PROCESSED = (byte)4;          // marks points processed previously
    final static byte MAX_AREA = (byte)8;           // marks areas near a maximum, within the tolerance
    final static byte EQUAL = (byte)16;             // marks contigous maximum points of equal level
    final static byte MAX_POINT = (byte)32;         // marks a single point standing for a maximum   
    
    final static int FAILED = 0;
    final static int SUCCESS = 1;
    final static int SKIPPED = 2;
    final static int CANCELED = 3;
    
    String defaultAnnotationColor = "orange";
    String[] colors = IJU.colors;
    
    boolean gotProcessingResponse = false;
    boolean gotFailedResponse = false;
    
    Map setupData;
    String boundary = "================9876543210==";
    String mime = "";   
    
    int npoints, slice;
    boolean saveActive = false;
    boolean notDP = true;
    double[] xdpoints;
    double[] ydpoints;    
    private Color colorWCS = new Color(255,190,0);//(226,105,11);
    private Color sourceColor = new Color(33,120,181);
    OverlayCanvas ocanvas;
    DecimalFormat uptoTwoPlaces = new DecimalFormat("0.##", IJU.dfs);
    String lineend = "\r\n";
    String session_string = "";
    Object subid_int = 0;
    String sourceLocations;
    public AstrometrySetup astrometrySetup;

	public Astrometry () 
		{
        Locale.setDefault(IJU.locale);
        if (IJ.isWindows()) slash="\\";
        }
        
    public int solve(ImagePlus impIn, boolean runSetup, AstroConverter acc, boolean useSexagesimal, boolean useLog, boolean useLogDateTime, ImagePlus impRawIn, String rawPath)
        {
        notDP = (acc == null);
        
        canceled = false;
        retries = 0;
        impOriginal = impIn;
        impRaw = impRawIn;
        ocanvas = OverlayCanvas.getOverlayCanvas (impOriginal);
        startSlice = 1;
        endSlice = impOriginal.getStackSize();

        width = impOriginal.getWidth();
        height = impOriginal.getHeight();  
        
        setupCanceled = false;
        setupActive = false;
        if (runSetup) 
            {
            setupActive = true;
            astrometrySetup = new AstrometrySetup();
            setupCanceled = astrometrySetup.start(1, impOriginal.getCurrentSlice(), impOriginal.getStackSize(), "START", acc, useSexagesimal);
            if (setupCanceled)
                {
                canceled = true;
                return CANCELED;
                }
            setupActive = false;
            }
        
        getPrefs();
        
        if (!useAlternateAstrometryServer && userKey.trim().equals(""))
            {
            IJ.showMessage("Login Error","<html>User Key is empty.<br>"+
                           "Obtain a user key at nova.astrometry.net and enter it<br>"+
                           "into the \"User Key:\" box in the Astrometry Settings panel."); 
            return FAILED;
            }
        
        resaveRaw = !notDP && DPSaveRawWithWCS && impRaw != null && impRaw.getStackSize() == 1 && rawPath != null && !rawPath.equals("");
        
        if (!notDP)
            {
            ra = acc.getRAJ2000();
            dec = acc.getDecJ2000();
            showLog = useLog;
            showLogDateTime = useLogDateTime;
            }

        minPeakFindToleranceSTDEV *= impOriginal.getStatistics().stdDev;
        scale_lower = scaleEstimate - scaleError;
        scale_upper = scaleEstimate + scaleError;        
        
        String resultText = "";
        URL apiURL = null;
        URL loginURL = null;
        URL uploadURL = null; 
        URL subCheckURL = null;
        URL jobCheckURL = null;
        URL getFileURL = null;
        JSONParser parser = new JSONParser();   

        try {
            apiURL = new URL((useAlternateAstrometryServer?alternateAstrometryUrlBase:defaultAstrometryUrlBase)+"/api/");
            loginURL = new URL(apiURL + "login");

            Map loginData = new LinkedHashMap();
            loginData.put("apikey",userKey.trim());

            String login = "request-json=" + URLEncoder.encode(toJSONString(loginData),"UTF-8");

            URLConnection astrometryCon = loginURL.openConnection();
            astrometryCon.setConnectTimeout(60000);
            astrometryCon.setReadTimeout(60000);
            astrometryCon.setDoOutput(true);
            astrometryCon.setDoInput(true);

            DataOutputStream wr = new DataOutputStream(astrometryCon.getOutputStream());
            wr.writeBytes(login);
            wr.close();

            BufferedReader in = new BufferedReader(new InputStreamReader(astrometryCon.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) 
                {
                resultText += inputLine;
                }            
            //log("Login result="+resultText);
            JSONObject result = (JSONObject)parser.parse(resultText);
            String stat = (String)result.get("status");
            if (stat.equals("error"))
                {
                log("Astrometry.net login error, exiting.");
                return FAILED;
                }
            else 
                {
                IJ.showStatus("Login status: "+stat);
                session_string = (String)result.get("session");
                IJ.showStatus("Session: "+session_string);
                }
            }
        catch (IOException ioe){
            log("IO Exception during astrometry.net login: "+ioe.getLocalizedMessage());
            return FAILED;
            }        
        catch (astroj.json.simple.parser.ParseException pe){
            log("JSON Parse Exception in astrometry.net login response: "+pe);
            return FAILED;
            } 
//        log("Session: "+session_string);
        if (!processStack)
            {
            startSlice = impOriginal.getCurrentSlice();
            endSlice = impOriginal.getCurrentSlice();
            }
        if (startSlice<1) startSlice = 1;
        if (startSlice>impOriginal.getStackSize()) startSlice = impOriginal.getStackSize();
        if (endSlice  >impOriginal.getStackSize()) endSlice   = impOriginal.getStackSize();
        if (endSlice  <startSlice) endSlice = startSlice;
        int previousSlice = -1;
        for (slice=startSlice; slice<=endSlice; slice++)
            { 
            if (canceled) return CANCELED;
            
            if (previousSlice == slice)
                {
                retries++;
                }
            else
                {
                previousSlice = slice;
                retries = 0;
                }
            
            impOriginal.setSlice(slice);
            
            if (skipIfHasWCS)
                {
                WCS wcs = new WCS(impOriginal);
                if (wcs.hasWCS())
                    {
                    if (notDP) 
                        {
                        log("Astrometry status for "+(impOriginal.getStackSize()==1?impOriginal.getTitle():"slice "+slice)+": SKIPPED (already has valid WCS headers)");
                        continue;
                        }                    
                    else
                        {
                        return SKIPPED;
                        }
                    }
                }
//            imp = new ImagePlus(impOriginal.getStack().getSliceLabel(slice), impOriginal.getStack().getProcessor(slice) ); 
            imp = new ImagePlus("Working_"+IJU.getSliceFilename(impOriginal, slice), impOriginal.getStack().getProcessor(slice).duplicate() );
            imp.setCalibration(impOriginal.getCalibration());  
            imp.setFileInfo(impOriginal.getFileInfo());
            
//            ocanvas.removeAnnotateRois();
            ocanvas.removeAstrometryAnnotateRois();
            impOriginal.updateAndDraw();

//            IJ.runPlugIn(impOriginal, "Clear_Overlay", "");
//            imp = impOriginal..duplicate();        
//            IJ.log("starting median");
            if (useMedianFilter)
                {
                IJ.showStatus("Median filter image");
                IJ.run(imp, "Median...", "radius="+medianFilterRadius);
                }
//            IJ.log("finished median");
//            IJ.run(imp,"Remove Outliers...","radius=2 threshold=50 which=Bright stack");
            IJ.showStatus("Finding sources");
            if (canceled) return CANCELED;
            findMaxima(minPeakFindToleranceSTDEV);
//            if (IJ.escapePressed())return false;
            if (canceled) return CANCELED;

            sourceLocations = "";

            for (int i=0; i<npoints; i++)
                {
                sourceLocations += ""+(xdpoints[i]) + " \t " + (height - ydpoints[i]) + lineend;
                }
            sourceLocations += lineend;

//            log(sourceLocations);  

            setupData = new LinkedHashMap();
            setupData.put("allow_commercial_use", "d");
            setupData.put("allow_modifications", "d");
            setupData.put("publicly_visible", "n");
            setupData.put("image_width", width);
            setupData.put("image_height", height);
            if (useScale)
                {
                setupData.put("scale_units", "arcsecperpix");
                setupData.put("scale_type", "ul"); //"ul" or "ev"
                setupData.put("scale_lower", scale_lower);
                setupData.put("scale_upper", scale_upper);
                }
//                setupData.put("scale_est", 0.39);
//                setupData.put("scale_err", 0.02);
//                }
            if (useRaDec)
                {
                setupData.put("center_ra", ra*15.0);
                setupData.put("center_dec", dec);
                setupData.put("radius", raDecRadius/60.0);
                }
            setupData.put("downsample_factor", 1);
            setupData.put("crpix_center", true);
            if (useDistortionOrder)
                {
                if (distortionOrder < minOrder) distortionOrder = minOrder;
                if (distortionOrder > maxOrder) distortionOrder = maxOrder;
                setupData.put("tweak_order", distortionOrder);
                }
            else
                {
                setupData.put("tweak_order", 0);
                }
    //        setupData.put("parity", 0);
            setupData.put("session", session_string);

            boundary = "================9876543210==";

            mime = "--"+boundary+lineend+
                    "Content-Type: text/plain"+lineend+
                    "MIME-Version: 1.0"+lineend+
                    "Content-disposition: form-data;name=\"request-json\""+lineend+
                    lineend+
                    toJSONString(setupData)+lineend+
                    "--"+boundary+lineend+
                    "Content-Type: application/octet-stream"+lineend+
                    "MIME-Version: 1.0"+lineend+
                    "Content-disposition: form-data;name=\"file\";filename=\"maxima.tbl\""+lineend+
                    lineend+
                    sourceLocations+
                    "--"+boundary+"--"+lineend;
//            IJ.log(mime);
            if (canceled) return CANCELED;
            try {
                uploadURL = new URL((useAlternateAstrometryServer?alternateAstrometryUrlBase:defaultAstrometryUrlBase)+"/api/upload");
                HttpURLConnection uploadCon = (HttpURLConnection)uploadURL.openConnection();
                uploadCon.setConnectTimeout(60000);
                uploadCon.setReadTimeout(60000);
                uploadCon.setUseCaches(false);

                uploadCon.setDoOutput(true);
                uploadCon.setDoInput(true);            

    //            uploadCon.setRequestMethod("POST");
    //            uploadCon.setRequestProperty("Connection", "Keep-Alive");
                uploadCon.setRequestProperty("Content-Type", "multipart/form-data;boundary="+boundary);

                DataOutputStream wr = new DataOutputStream(uploadCon.getOutputStream());
                wr.writeBytes(mime);
                wr.close();
                if (canceled) return CANCELED;
                BufferedReader in = new BufferedReader(new InputStreamReader(uploadCon.getInputStream()));
                String inputLine;
                resultText = "";
                while ((inputLine = in.readLine()) != null)
                    {
                    if (canceled) return CANCELED;
                    resultText += inputLine;
                    }         
//                log(resultText);


                JSONObject result = (JSONObject)parser.parse(resultText);
                String stat = (String)result.get("status");
                if (!stat.equals("success"))
                    {
                    log("astrometry.net upload status for "+(impOriginal.getStackSize()==1?impOriginal.getTitle():"slice "+slice)+" : "+stat);
                    log("astrometry.net upload error for "+(impOriginal.getStackSize()==1?impOriginal.getTitle():"slice "+slice)+". Aborting.");
                    return FAILED;
                    }
                else 
                    {
                    IJ.showStatus("Upload status: "+stat);
                    subid_int = result.get("subid");
                    IJ.showStatus("Submision ID: "+subid_int.toString());
//                    log("Astrometry.net submision ID: "+subid_int.toString());
                    }
                }
            catch (IOException ioe){ 
                log("IO Exception during astrometry.net upload for "+(impOriginal.getStackSize()==1?impOriginal.getTitle():"slice "+slice)+" : "+ioe.getLocalizedMessage());
                return FAILED;
                }  
            catch (astroj.json.simple.parser.ParseException pe){
                log("JSON Parse Exception in astrometry.net upload response for "+(impOriginal.getStackSize()==1?impOriginal.getTitle():"slice "+slice)+" : "+pe);
                return FAILED;
                }         

    //        IJ.wait(5000);
            if (canceled) return CANCELED;
    //        Check submission status
            JSONObject result2 = null;
            Object job_id_list = null;
    //        Object processing_finished = null;
            try {
                subCheckURL = new URL((useAlternateAstrometryServer?alternateAstrometryUrlBase:defaultAstrometryUrlBase)+"/api/submissions/"+subid_int.toString());

                BufferedReader in = null;
                Boolean still_processing = true;
                int n_failed_attempts = 0; 
                int maxWaitTime = useAlternateAstrometryServer?600:120;

                String inputLine = null;
                while (still_processing && n_failed_attempts < maxWaitTime)
                    {
                    if (canceled) return CANCELED;
                    URLConnection subCheckCon = subCheckURL.openConnection();
                    subCheckCon.setConnectTimeout(60000);
                    subCheckCon.setReadTimeout(60000);                
                    try {
                        in = new BufferedReader(new InputStreamReader(subCheckCon.getInputStream()));
                        inputLine = null;
                        resultText = "";
                        while ((inputLine = in.readLine()) != null)
                            {
                            resultText += inputLine;
                            }            
//                        log(resultText);
                        result2 = (JSONObject)parser.parse(resultText); 
                        job_id_list = result2.get("jobs");
    //                    processing_finished = result2.get("processing_finished");
    //                    log("Job ID List = "+job_id_list.toString()); 
                        if (job_id_list.toString().equals("[]") || job_id_list.toString().equals("[null]")) // || processing_finished.toString().equals("None"))
                            {
//                            log(job_id_list.toString());
                            String status = "Astrometry submission "+subid_int.toString()+" processing";
//                            log("Astrometry status: "+jobStatus);
                            for (int n = 0; n<n_failed_attempts % 10; n++)
                                {
                                status += " .";
                                }
                            IJ.showStatus(status);                            
                            still_processing = true;
                            n_failed_attempts++;
                            IJ.wait(1000);
                            }
                        else
                            {
//                            IJ.log("job_id_list="+job_id_list.toString());
                            still_processing = false;
                            }
                        }
                    catch (IOException ioe){ 
                        log("Astrometry submission "+subid_int.toString()+" doesn't exist yet for "+(impOriginal.getStackSize()==1?impOriginal.getTitle():"slice "+slice)+" : "+ioe.getLocalizedMessage());
                        n_failed_attempts++;
                        IJ.wait(1000);
                        } 
                    catch (astroj.json.simple.parser.ParseException pe){
                        log("JSON Parse Exception during astrometry check for submission "+subid_int.toString()+" for "+(impOriginal.getStackSize()==1?impOriginal.getTitle():"slice "+slice)+" : "+pe);
                        n_failed_attempts++;
                        IJ.wait(1000);
                        }
                    }

                if (n_failed_attempts >= maxWaitTime)
                    {
                    IJ.showStatus("The submission "+subid_int.toString()+" has timed out for "+(impOriginal.getStackSize()==1?impOriginal.getTitle():"slice "+slice)+".");
                    
                    if (retries<maxRetries) 
                        {
                        log("Astrometry submission ID "+subid_int.toString()+" timed out for "+(impOriginal.getStackSize()==1?impOriginal.getTitle()+".":"slice "+slice+".")+" Resubmitting.");
                        slice -= 1;
                        }
                    else
                        {
                        log("Astrometry submission "+subid_int.toString()+" for "+(impOriginal.getStackSize()==1?impOriginal.getTitle():"slice "+slice)+" was unsuccessful after "+(maxRetries+1)+" tries. Skipping Image.");
//                        log("Astrometry.net job ID "+job_id_list.toString()+" timed out for "+(impOriginal.getStackSize()==1?impOriginal.getTitle()+".":"slice "+slice+".")+" Aborting.");
                        if (slice >= endSlice) return FAILED;
                        }
                    continue;
                    }
                }
            catch (IOException ioe){ 
                log("IO Exception during during astrometry check for submission "+subid_int.toString()+" for "+(impOriginal.getStackSize()==1?impOriginal.getTitle()+".":"slice "+slice+"."));
                return FAILED;
                } 
            if (canceled) return CANCELED;
            String jobID = job_id_list.toString().replace("[", "").replace("]", "");
            IJ.showStatus("Job ID: "+jobID);
//            IJ.showStatus("Processing finished: "+processing_finished); 
            
    //        Check job status
            JSONObject result3 = null;
            String jobStatus = null;

            try {
                jobCheckURL = new URL((useAlternateAstrometryServer?alternateAstrometryUrlBase:defaultAstrometryUrlBase)+"/api/jobs/"+jobID);

                BufferedReader in = null;
                Boolean still_processing = true;
                int n_failed_attempts = 0; 
                int maxWaitTime = notDP ? 600 : 120;
                gotProcessingResponse = false;
                gotFailedResponse = false;                

                String inputLine = null;
                while (still_processing && n_failed_attempts < maxWaitTime)
                    {
                    if (canceled) return CANCELED;
                    URLConnection jobCheckCon = jobCheckURL.openConnection();
                    jobCheckCon.setConnectTimeout(60000);
                    jobCheckCon.setReadTimeout(60000);                
                    try {

                        in = new BufferedReader(new InputStreamReader(jobCheckCon.getInputStream()));
                        inputLine = null;
                        resultText = "";
                        while ((inputLine = in.readLine()) != null)
                            {
                            resultText += inputLine;
                            }            
    //                    log(resultText);
                        result3 = (JSONObject)parser.parse(resultText); 
                        jobStatus = (String)result3.get("status");
                        if (jobStatus.equals("success"))
                            {
                            IJ.showStatus("Astrometry job "+jobID+": SOLVED");
                            if (notDP) log("Astrometry job "+jobID+" for "+(impOriginal.getStackSize()==1?impOriginal.getTitle():"slice "+slice)+": SOLVED");
                            still_processing = false;
                            }
                        else if (jobStatus.equals("failure") || jobStatus.equals("error"))
                            {
                            log("Astrometry job "+jobID+" for "+(impOriginal.getStackSize()==1?impOriginal.getTitle():"slice "+slice)+": "+jobStatus+". Resubmitting.");
                            gotFailedResponse = true;
                            break;
                            }
                        else if (jobStatus.equals("processing"))
                            {
                            log("Astrometry job "+jobID+" for "+(impOriginal.getStackSize()==1?impOriginal.getTitle():"slice "+slice)+": "+jobStatus+". Resubmitting.");
                            gotProcessingResponse = true;
                            break;
                            }                        
                        else
                            {
                            String status = "Astrometry job "+jobID+" "+jobStatus;
//                            log("Astrometry status: "+jobStatus);
                            for (int n = 0; n<n_failed_attempts % 10; n++)
                                {
                                status += " .";
                                }
                            IJ.showStatus(status);
                            n_failed_attempts++;
                            IJ.wait(1000);
                            }                    
                        }
                    catch (IOException ioe){ 
                        log("Astrometry job ("+jobID+") check error for "+(impOriginal.getStackSize()==1?impOriginal.getTitle():"slice "+slice)+" : "+ioe.getLocalizedMessage());
                        return FAILED;
                        } 
                    catch (astroj.json.simple.parser.ParseException pe){
                        log("JSON Parse Exception during astrometry.net job ("+jobID+") status check for "+(impOriginal.getStackSize()==1?impOriginal.getTitle():"slice "+slice)+" : "+pe);
                        return FAILED;
                        }
                    }
                
                if (gotProcessingResponse || gotFailedResponse) 
                    {
                    if (retries<maxRetries) 
                        {
                        slice -= 1;
                        }
                    else
                        {
                        log("Astrometry job "+jobID+" for "+(impOriginal.getStackSize()==1?impOriginal.getTitle():"slice "+slice)+" was unsuccessful after "+(maxRetries+1)+" tries. Skipping Image.");
                        if (slice >= endSlice) return FAILED;
                        }
                    continue;
                    }

                if (n_failed_attempts >= maxWaitTime)
                    {
                    IJ.showStatus("Astrometry job "+jobID+" has timed out for slice "+slice+".");
                    log("Astrometry job "+jobID+" has timed out for  "+(impOriginal.getStackSize()==1?impOriginal.getTitle()+".":"slice "+slice+".")+" Skipping Image.");
                    if (impOriginal.getStackSize() > 1)
                        continue;
                    else
                        return FAILED;
                    }
                }
            catch (IOException ioe){ 
                log("IO Exception during astrometry job ("+jobID+") status check for "+(impOriginal.getStackSize()==1?impOriginal.getTitle()+".":"slice "+slice+".")+" : "+ioe.getLocalizedMessage());
                return FAILED;
                } 

            if (canceled) return CANCELED;
            int len = 0;
            String[] wcsHeader = null;
            try {
    //            getFileURL = new URL("http://nova.astrometry.net/api/jobs/"+jobID+"/annotations");
                getFileURL = new URL((useAlternateAstrometryServer?alternateAstrometryUrlBase:defaultAstrometryUrlBase)+"/wcs_file/"+jobID);
                URLConnection fileDownloadCon = getFileURL.openConnection();
                fileDownloadCon.setConnectTimeout(60000);
                fileDownloadCon.setReadTimeout(60000);
                fileDownloadCon.setDoOutput(true);
                fileDownloadCon.setDoInput(true);

                BufferedReader in = new BufferedReader(new InputStreamReader(fileDownloadCon.getInputStream()));
                String inputLine = in.readLine();

                len = inputLine.length()/80;
                wcsHeader = new String[len];
                if (inputLine != null && !inputLine.equals(""))
                    {
                    for (int i=0; i<len; i++)
                        {
                        wcsHeader[i] = inputLine.substring(i*80, (i+1)*80);
                        }  
//                    for (int i=0; i<len; i++)
//                        log(wcsHeader[i]);
                    }
                else
                    {
                    log("Failed to retrieve WCS headers for "+(impOriginal.getStackSize()==1?impOriginal.getTitle()+".":"slice "+slice+"."));
                    if (impOriginal.getStackSize() > 1)
                        continue;
                    else
                        return FAILED;
                    }
                }
            catch (IOException ioe){
                log("IO Exception during astrometry.net file download for "+(impOriginal.getStackSize()==1?impOriginal.getTitle():"slice "+slice)+" : "+ioe.getLocalizedMessage());
                return FAILED;
                }        
            if (canceled) return CANCELED;
            impOriginal.setSlice(slice);
            String[] header = FitsJ.getHeader(impOriginal);
            if (header == null)
                {
                header = new String[5];
                header[0] = FitsJ.createCard("SIMPLE", "T", "Created by AIJ");
                header[1] = FitsJ.createCard("NAXIS", "2", "number of data axes");
                header[2] = FitsJ.createCard("NAXIS1", ""+width, "length of data axis 1");
                header[3] = FitsJ.createCard("NAXIS2", ""+height, "length of data axis 2");
                header[4] = FitsJ.createCard("END", "", "");
                }
            String[] headerRaw = null;
            if (resaveRaw) 
                {
                headerRaw = FitsJ.getHeader(impRaw);
                if (headerRaw == null)
                    {
                    headerRaw = new String[5];
                    headerRaw[0] = FitsJ.createCard("SIMPLE", "T", "Created by AIJ");
                    headerRaw[1] = FitsJ.createCard("NAXIS", "2", "number of data axes");
                    headerRaw[2] = FitsJ.createCard("NAXIS1", ""+width, "length of data axis 1");
                    headerRaw[3] = FitsJ.createCard("NAXIS2", ""+height, "length of data axis 2");
                    headerRaw[4] = FitsJ.createCard("END", "", "");
                    }                
                }
//            if (header == null) header = new String[]{FitsJ.pad("END", 80)};
            int wcsCardNum = -1;
            int cardNum = -1;
            String cardString = "";
            String keyword = "";
            String[] keywords = {
                "WCSAXES",
                "CTYPE1",
                "CTYPE2",
                "CUNIT1",
                "CUNIT2",
                "EQUINOX",
                "EPOCH",
                "LONPOLE",
                "LATPOLE",
                "CRVAL1",
                "CRVAL2",
                "CRPIX1",
                "CRPIX2",
                "CD1_1",
                "CD1_2",
                "CD2_1",
                "CD2_2",
                "IMAGEW",
                "IMAGEH",
                "A_ORDER",
                "B_ORDER",
                "AP_ORDER",
                "BP_ORDER"};

            Calendar cal = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");        
            header = FitsJ.addHistory("WCS created by AIJ link to Astronomy.net website", header);
            if (resaveRaw) headerRaw = FitsJ.addHistory("WCS created by AIJ link to Astronomy.net website", headerRaw);
            header = FitsJ.addHistory("WCS created on "+sdf.format(cal.getTime()), header); 
            if (resaveRaw) headerRaw = FitsJ.addHistory("WCS created on "+sdf.format(cal.getTime()), headerRaw);


            for (int i=0; i<keywords.length; i++)
                {
                cardNum = FitsJ.findCardWithKey(keywords[i], header);
                if (cardNum != -1)
                    {
                    header = FitsJ.removeCards(keywords[i], header);
                    }
                if (resaveRaw)
                    {
                    cardNum = FitsJ.findCardWithKey(keywords[i], headerRaw);
                    if (cardNum != -1)
                        {
                        headerRaw = FitsJ.removeCards(keywords[i], headerRaw);
                        }
                    }
                wcsCardNum = FitsJ.findCardWithKey(keywords[i], wcsHeader);
                if (wcsCardNum != -1)
                    {
                    header = FitsJ.addCard(wcsHeader[wcsCardNum], header);
                    if (resaveRaw)
                        {
                        headerRaw = FitsJ.addCard(wcsHeader[wcsCardNum], headerRaw);
                        }
                    }
                }

            for (int i=0; i<10; i++) {
                for (int j=0; j<10; j++) {
                    cardNum = FitsJ.findCardWithKey("A_"+i+"_"+j, header);
                    if (cardNum != -1) {
                        header = FitsJ.removeCards("A_"+i+"_"+j, header);
                        }
                    if (resaveRaw) {
                        cardNum = FitsJ.findCardWithKey("A_"+i+"_"+j, headerRaw);
                        if (cardNum != -1) {
                            headerRaw = FitsJ.removeCards("A_"+i+"_"+j, headerRaw);
                            }
                        }
                    cardNum = FitsJ.findCardWithKey("B_"+i+"_"+j, header);
                    if (cardNum != -1) {
                        header = FitsJ.removeCards("B_"+i+"_"+j, header);
                        }
                    if (resaveRaw) {
                        cardNum = FitsJ.findCardWithKey("B_"+i+"_"+j, headerRaw);
                        if (cardNum != -1) {
                            headerRaw = FitsJ.removeCards("B_"+i+"_"+j, headerRaw);
                            }
                        }
                    cardNum = FitsJ.findCardWithKey("AP_"+i+"_"+j, header);
                    if (cardNum != -1) {
                        header = FitsJ.removeCards("AP_"+i+"_"+j, header);
                        }
                    if (resaveRaw) {
                        cardNum = FitsJ.findCardWithKey("AP_"+i+"_"+j, headerRaw);
                        if (cardNum != -1) {
                            headerRaw = FitsJ.removeCards("AP_"+i+"_"+j, headerRaw);
                            }
                        }
                    cardNum = FitsJ.findCardWithKey("BP_"+i+"_"+j, header);
                    if (cardNum != -1) {
                        header = FitsJ.removeCards("BP_"+i+"_"+j, header);
                        }
                    if (resaveRaw) {
                        cardNum = FitsJ.findCardWithKey("BP_"+i+"_"+j, headerRaw);
                        if (cardNum != -1) {
                            headerRaw = FitsJ.removeCards("BP_"+i+"_"+j, headerRaw);
                            }
                        }
                    }
                }

            for (int i=0; i<10; i++) {
                for (int j = 0; j < 10; j++) {
                    wcsCardNum = FitsJ.findCardWithKey("A_"+i+"_"+j, wcsHeader);
                    if (wcsCardNum != -1) {
                        header = FitsJ.addCard(wcsHeader[wcsCardNum], header);
                        if (resaveRaw) {
                            headerRaw = FitsJ.addCard(wcsHeader[wcsCardNum], headerRaw);
                        }
                    }
                }
            }

            for (int i=0; i<10; i++) {
                for (int j = 0; j < 10; j++) {
                    wcsCardNum = FitsJ.findCardWithKey("B_"+i+"_"+j, wcsHeader);
                    if (wcsCardNum != -1) {
                        header = FitsJ.addCard(wcsHeader[wcsCardNum], header);
                        if (resaveRaw) {
                            headerRaw = FitsJ.addCard(wcsHeader[wcsCardNum], headerRaw);
                        }
                    }
                }
            }

            for (int i=0; i<10; i++) {
                for (int j = 0; j < 10; j++) {
                    wcsCardNum = FitsJ.findCardWithKey("AP_"+i+"_"+j, wcsHeader);
                    if (wcsCardNum != -1) {
                        header = FitsJ.addCard(wcsHeader[wcsCardNum], header);
                        if (resaveRaw) {
                            headerRaw = FitsJ.addCard(wcsHeader[wcsCardNum], headerRaw);
                        }
                    }
                }
            }

            for (int i=0; i<10; i++) {
                for (int j = 0; j < 10; j++) {
                    wcsCardNum = FitsJ.findCardWithKey("BP_"+i+"_"+j, wcsHeader);
                    if (wcsCardNum != -1) {
                        header = FitsJ.addCard(wcsHeader[wcsCardNum], header);
                        if (resaveRaw) {
                            headerRaw = FitsJ.addCard(wcsHeader[wcsCardNum], headerRaw);
                        }
                    }
                }
            }

            if (canceled) return CANCELED;
            saveActive = true;
            
            impOriginal.setSlice(slice);
            FitsJ.putHeader(impOriginal,header);
            if (resaveRaw) FitsJ.putHeader(impRaw,headerRaw);
            saveActive = false;
            
            if (impOriginal.getCanvas() instanceof AstroCanvas)
                {
                ((AstroStackWindow)impOriginal.getWindow()).setAstroProcessor(true);
                impOriginal.getWindow().repaint();
                }
            if (canceled) return CANCELED;
            
            ocanvas.removeAstrometryAnnotateRois();
//            ocanvas.clearRois();
            impOriginal.updateAndDraw();

//            IJ.runPlugIn(impOriginal, "Clear_Overlay", "");
//            IJ.runPlugIn(impOriginal, "Clear_Overlay", "");
            if (annotate)
                {
                
                FitsJ.putHeader(impOriginal, FitsJ.removeAstrometryAnnotateCards(FitsJ.getHeader(impOriginal)));
                if (resaveRaw) FitsJ.putHeader(impRaw, FitsJ.removeAstrometryAnnotateCards(FitsJ.getHeader(impRaw)));   
                
                JSONObject objectsJSON = null;
                String[] stars = null;
                try {
                    getFileURL = new URL((useAlternateAstrometryServer?alternateAstrometryUrlBase:defaultAstrometryUrlBase)+"/api/jobs/"+jobID+"/annotations");
                    URLConnection fileDownloadCon = getFileURL.openConnection();
                    fileDownloadCon.setConnectTimeout(60000);
                    fileDownloadCon.setReadTimeout(60000);
                    fileDownloadCon.setDoOutput(true);
                    fileDownloadCon.setDoInput(true);

                    BufferedReader in = new BufferedReader(new InputStreamReader(fileDownloadCon.getInputStream()));
                    String inputLine = in.readLine();
        //            inputLine.replaceAll("\\{\"annotations\"\\: \\[", "");
//                    log(inputLine);

                    stars = inputLine.split("\\, \\{");
//                    for (int i=0; i<stars.length; i++)
//                        {
//                        log(stars[i]);
//                        }
                    int startName = -1;
                    int startX = -1;
                    int startY = -1;
                    int endName = -1;
                    int endX = -1;
                    int endY = -1;
                    String name = "";
                    String x = "";
                    String y = "";

                    for (int i=0; i<stars.length; i++)
                        {
                        if (canceled) return CANCELED;
                        startName = stars[i].indexOf("names");
                        startX = stars[i].indexOf("pixelx");
                        startY = stars[i].indexOf("pixely");
                        if (startName != -1) endName = stars[i].indexOf('"', startName+10);
                        if (startX != -1) endX = stars[i].indexOf(',', startX+9);
                        if (startY != -1) endY = stars[i].indexOf(',', startY+9);
                        if (startY != -1 && endY == -1) endY = stars[i].indexOf('}', startY+9);
//                        log("startName="+startName+"  endName="+endName+"  startX="+startX+"  endX="+endX+"   startY="+startY+"   endY="+endY);
                        if (startName != -1 && endName != -1 && startX != -1 && endX != -1 && startY != -1 && endY != -1)
                            {
                            try {
                                name = stars[i].substring(startName+10, endName);
                                if (name.contains("\\u"))
                                    {
                                    ArrayList<String> pieces = new ArrayList<String>();
                                    while(name.contains("\\u")){
                                        pieces.add(name.substring(0,name.indexOf("\\u")));//add the bit before the /uXXXX
                                        char c = (char)Integer.parseInt(name.substring(name.indexOf("\\u")+2,name.indexOf("\\u")+6), 16);
                                        name = name.substring(name.indexOf("\\u")+6,name.length());
                                        pieces.add(c+"");//add the unicode
                                        }
                                    String temp = "";
                                    for(String s : pieces){
                                        temp += s;//put humpty dumpty back together again
                                        }
                                    name = temp + name;
                                    }
                                x = stars[i].substring(startX+9, endX);
                                y = stars[i].substring(startY+9, endY);
//                                log (name);
//                                log (x);
//                                log (y);
                                addAnnotateRoi (impOriginal, true, false, true, false, Double.parseDouble(x) - 0.5, height - (Double.parseDouble(y) - 0.5), annotateRadius, name, IJU.colorOf(defaultAnnotationColor));
                                }                        
                            catch (NumberFormatException nfe){}                         
                            }
                        }
                    impOriginal.draw();
                    }
                catch (IOException ioe){
                    log("IO Exception during astrometry.net objects_in_field download for "+(impOriginal.getStackSize()==1?impOriginal.getTitle():"slice "+slice)+" : "+ioe.getLocalizedMessage());
                    return FAILED;
                    } 
                }
            if (notDP && (autoSave || impOriginal.getStack().isVirtual()))
                {
                boolean saveSuccess = false;
                if (canceled) return CANCELED;
                saveActive = true;
                impOriginal.setSlice(slice);
                String imageDirname = impOriginal.getOriginalFileInfo().directory;
                String imageFilename = IJU.getSliceFilename(impOriginal, slice);

                if (impOriginal.getStack().isVirtual())
                    {
//                    imp2 = new ImagePlus(impOriginal.getStack().getSliceLabel(slice), impOriginal.getStack().getProcessor(slice) ); 
                    imp2 = new ImagePlus("WCS_"+imageFilename, impOriginal.getStack().getProcessor(slice).duplicate() ); 
                    imp2.setCalibration(impOriginal.getCalibration());  
                    imp2.setFileInfo(impOriginal.getFileInfo());
                    FitsJ.putHeader(imp2,header);
//                    IJ.saveAs(imp2,""+imageFilename.substring(imageFilename.lastIndexOf('.')),imageDirname+imageFilename);
                    if (autoSave)
                        {
                        saveSuccess = IJU.saveFile(imp2,imageDirname+imageFilename, showLog, showLogDateTime, "");
                        }
                    else
                        {
                        String wcsPath = imageDirname+"wcs";
                        File wcsDirectory = new File (wcsPath);
                        if (wcsDirectory.isFile())
                            {
                            if (showLog) IJU.log("ERROR: Save directory \""+wcsPath+"\" is a file, not a directory.", showLogDateTime);
                            return FAILED;
                            }
                        if (!wcsDirectory.isDirectory())
                            {
                            if (!wcsDirectory.mkdir())
                                {
                                if (showLog) IJU.log("ERROR: Could not create save-to directory \""+wcsPath+"\".");
                                return FAILED;
                                }
                            }
                        saveSuccess = IJU.saveFile(imp2,wcsPath+slash+imageFilename, showLog, showLogDateTime, "");                        
                        }
                    }
                else
                    {
//                    IJ.saveAs(impOriginal,""+imageFilename.substring(imageFilename.lastIndexOf('.')),imageDirname+imageFilename);
                    saveSuccess = IJU.saveFile(impOriginal, imageDirname+imageFilename, showLog, showLogDateTime, "");
                    }    
                saveActive = false;
                if (!saveSuccess)
                    {
                    return FAILED;
                    }
                }
            else if (resaveRaw)
                {
                if (!IJU.saveFile(impRaw, rawPath, showLog, showLogDateTime, "raw science"))
                    {
                    return FAILED;
                    }                
                }
            else if (annotate && (slice != endSlice || !notDP))
                {
                if (canceled) return CANCELED;
                IJ.wait(1000);
                }
            }

        return SUCCESS;
        }
    
   
    public void setShowLog(boolean show)
        {
        showLog = show;
        }  
    
    
    void log(String message)
        {
        if (showLog)
            {
            if (showLogDateTime)
                {
                cal = Calendar.getInstance();
                IJ.log("["+sdf.format(cal.getTime())+"]      "+message);
                }
            else
                {
                IJ.log(message);
                } 
            }
        }    
    
        public void findMaxima(double tolerance) {
            ImageProcessor ip = imp.getProcessor();
            if (dirOffset == null) makeDirectionOffsets(ip);
            Rectangle roi = ip.getRoi();
            byte[] mask = ip.getMaskArray();
            ByteProcessor typeP = new ByteProcessor(width, height);     //will be a notepad for pixel types
            byte[] types = (byte[])typeP.getPixels();
            float globalMin = Float.MAX_VALUE;
            float globalMax = -Float.MAX_VALUE;
            for (int y=roi.y; y<roi.y+roi.height; y++) {         //find local minimum/maximum now
                for (int x=roi.x; x<roi.x+roi.width; x++) {      //ImageStatistics won't work if we have no ImagePlus
                    float v = ip.getPixelValue(x, y);
                    if (globalMin>v) globalMin = v;
                    if (globalMax<v) globalMax = v;
                }
            }

            if (canceled) return;
            IJ.showStatus("Getting sorted maxima...");
            long[] maxPoints = getSortedMaxPoints(ip, typeP, true, false, globalMin, globalMax, ImageProcessor.NO_THRESHOLD); 
            if (canceled) return;
            IJ.showStatus("Analyzing  maxima...");
            float maxSortingError = 0;
            if (ip instanceof FloatProcessor)   //sorted sequence may be inaccurate by this value
                maxSortingError = 1.1f * ((globalMax-globalMin)/2e9f);
            analyzeAndMarkMaxima(ip, typeP, maxPoints, true, false, globalMin, tolerance, maxSortingError);


        } // findMaxima
    
        
        /** Find all local maxima (irrespective whether they finally qualify as maxima or not)
        * @param ip    The image to be analyzed
        * @param typeP A byte image, same size as ip, where the maximum points are marked as MAXIMUM
        *              (do not use it as output: for rois, the points are shifted w.r.t. the input image)
        * @param excludeEdgesNow Whether to exclude edge pixels
        * @param isEDM     Whether ip is a float Euclidian distance map
        * @param globalMin The minimum value of the image or roi
        * @param threshold The threshold (calibrated) below which no pixels are processed. Ignored if ImageProcessor.NO_THRESHOLD
        * @return          Maxima sorted by value. In each array element (long, i.e., 64-bit integer), the value
        *                  is encoded in the upper 32 bits and the pixel offset in the lower 32 bit
        * Note: Do not use the positions of the points marked as MAXIMUM in typeP, they are invalid for images with a roi.
        */    
        long[] getSortedMaxPoints(ImageProcessor ip, ByteProcessor typeP, boolean excludeEdgesNow,
                boolean isEDM, float globalMin, float globalMax, double threshold) {
            Rectangle roi = ip.getRoi();
            byte[] types =  (byte[])typeP.getPixels();
            int nMax = 0;  //counts local maxima
            boolean checkThreshold = threshold!=ImageProcessor.NO_THRESHOLD;
            //long t0 = System.currentTimeMillis();
            for (int y=roi.y; y<roi.y+roi.height; y++) {         // find local maxima now
                if (y%50==0 && canceled) return null;
                for (int x=roi.x, i=x+y*width; x<roi.x+roi.width; x++, i++) {      // for better performance with rois, restrict search to roi
                    float v = ip.getPixelValue(x,y);
                    float vTrue = v;  
                    if (v==globalMin) continue;
                    if (excludeEdgesNow && (x==0 || x==width-1 || y==0 || y==height-1)) continue;
                    if (checkThreshold && v<threshold) continue;
                    boolean isMax = true;
                    /* check wheter we have a local maximum.
                    Note: For an EDM, we need all maxima: those of the EDM-corrected values
                    (needed by findMaxima) and those of the raw values (needed by cleanupMaxima) */
                    boolean isInner = (y!=0 && y!=height-1) && (x!=0 && x!=width-1); //not necessary, but faster than isWithin
                    for (int d=0; d<8; d++) {                         // compare with the 8 neighbor pixels
                        if (isInner || isWithin(x, y, d)) {
                            float vNeighbor = ip.getPixelValue(x+DIR_X_OFFSET[d], y+DIR_Y_OFFSET[d]);
                            float vNeighborTrue = vNeighbor;
                            if (vNeighbor > v && vNeighborTrue > vTrue) {
                                isMax = false;
                                break;
                            }
                        }
                    }
                    if (isMax) {
                        types[i] = MAXIMUM;
                        nMax++;
                    }
                } // for x
            } // for y
            if (canceled) return null;
            //long t1 = System.currentTimeMillis();IJ.log("markMax:"+(t1-t0));

            float vFactor = (float)(2e9/(globalMax-globalMin)); //for converting float values into a 32-bit int
            long[] maxPoints = new long[nMax];                  //value (int) is in the upper 32 bit, pixel offset in the lower
            int iMax = 0;
            for (int y=roi.y; y<roi.y+roi.height; y++)           //enter all maxima into an array
                for (int x=roi.x, p=x+y*width; x<roi.x+roi.width; x++, p++)
                    if (types[p]==MAXIMUM) {
                        float fValue = ip.getPixelValue(x,y);
                        int iValue = (int)((fValue-globalMin)*vFactor); //32-bit int, linear function of float value
                        maxPoints[iMax++] = (long)iValue<<32|p;
                    }
            //long t2 = System.currentTimeMillis();IJ.log("makeArray:"+(t2-t1));
            if (canceled) return null;
            Arrays.sort(maxPoints);                                 //sort the maxima by value
            //long t3 = System.currentTimeMillis();IJ.log("sort:"+(t3-t2));
            return maxPoints;
        } //getSortedMaxPoints       
        
        
        
        
        /** Check all maxima in list maxPoints, mark type of the points in typeP
            * @param ip             the image to be analyzed
            * @param typeP          8-bit image, here the point types are marked by type: MAX_POINT, etc.
            * @param maxPoints      input: a list of all local maxima, sorted by height. Lower 32 bits are pixel offset
            * @param excludeEdgesNow whether to avoid edge maxima
            * @param isEDM          whether ip is a (float) Euclidian distance map
            * @param globalMin      minimum pixel value in ip
            * @param tolerance      minimum pixel value difference for two separate maxima
            * @param maxSortingError sorting may be inaccurate, sequence may be reversed for maxima having values
            *                       not deviating from each other by more than this (this could be a result of
            *                       precision loss when sorting ints instead of floats, or because sorting does not
            *                       take the height correction in 'trueEdmHeight' into account
            */
        void analyzeAndMarkMaxima(ImageProcessor ip, ByteProcessor typeP, long[] maxPoints, boolean excludeEdgesNow,
                boolean isEDM, float globalMin, double tolerance, float maxSortingError) {
                byte[] types =  (byte[])typeP.getPixels();
                int nMax = maxPoints.length;
                int [] pList = new int[width*height];       //here we enter points starting from a maximum
                Vector xyVector = new Vector();;
                Roi roi = null;
                if (imp!=null)
                    roi = imp.getRoi();	    

                for (int iMax=nMax-1; iMax>=0; iMax--) {    //process all maxima now, starting from the highest
                    if (iMax%100 == 0 && canceled) return;
                    int offset0 = (int)maxPoints[iMax];     //type cast gets 32 lower bits, where pixel index is encoded
                    //int offset0 = maxPoints[iMax].offset;
                    if ((types[offset0]&PROCESSED)!=0)      //this maximum has been reached from another one, skip it
                        continue;
                    //we create a list of connected points and start the list at the current maximum
                    int x0 = offset0 % width;               
                    int y0 = offset0 / width;
                    float v0 = ip.getPixelValue(x0,y0);
                    boolean sortingError;
                    do {                                    //repeat if we have encountered a sortingError
                        if (canceled) return;
                        pList[0] = offset0;
                        types[offset0] |= (EQUAL|LISTED);   //mark first point as equal height (to itself) and listed
                        int listLen = 1;                    //number of elements in the list
                        int listI = 0;                      //index of current element in the list
                        boolean isEdgeMaximum = (x0==0 || x0==width-1 || y0==0 || y0==height-1);
                        sortingError = false;       //if sorting was inaccurate: a higher maximum was not handled so far
                        boolean maxPossible = true;         //it may be a true maximum
                        double xEqual = x0;                 //for creating a single point: determine average over the
                        double yEqual = y0;                 //  coordinates of contiguous equal-height points
                        int nEqual = 1;                     //counts xEqual/yEqual points that we use for averaging
                        do {                                //while neigbor list is not fully processed (to listLen)
                            int offset = pList[listI];
                            int x = offset % width;
                            int y = offset / width;
                            //if(x==18&&y==20)IJ.write("x0,y0="+x0+","+y0+"@18,20;v0="+v0+" sortingError="+sortingError);
                            boolean isInner = (y!=0 && y!=height-1) && (x!=0 && x!=width-1); //not necessary, but faster than isWithin
                            for (int d=0; d<8; d++) {       //analyze all neighbors (in 8 directions) at the same level
                                int offset2 = offset+dirOffset[d];
                                if ((isInner || isWithin(x, y, d)) && (types[offset2]&LISTED)==0) {
                                    if ((types[offset2]&PROCESSED)!=0) {
                                        maxPossible = false; //we have reached a point processed previously, thus it is no maximum now
                                        //if(x0<25&&y0<20)IJ.write("x0,y0="+x0+","+y0+":stop at processed neighbor from x,y="+x+","+y+", dir="+d);
                                        break;
                                    }
                                    int x2 = x+DIR_X_OFFSET[d];
                                    int y2 = y+DIR_Y_OFFSET[d];
                                    float v2 = ip.getPixelValue(x2, y2);
                                    if (v2 > v0 + maxSortingError) {
                                        maxPossible = false;    //we have reached a higher point, thus it is no maximum
                                        //if(x0<25&&y0<20)IJ.write("x0,y0="+x0+","+y0+":stop at higher neighbor from x,y="+x+","+y+", dir="+d+",value,value2,v2-v="+v0+","+v2+","+(v2-v0));
                                        break;
                                    } else if (v2 >= v0-(float)tolerance) {
                                        if (v2 > v0) {          //maybe this point should have been treated earlier
                                            sortingError = true;
                                            offset0 = offset2;
                                            v0 = v2;
                                            x0 = x2;
                                            y0 = y2;

                                        }
                                        pList[listLen] = offset2;
                                        listLen++;              //we have found a new point within the tolerance
                                        types[offset2] |= LISTED;
                                        if (x2==0 || x2==width-1 || y2==0 || y2==height-1) {
                                            isEdgeMaximum = true;
                                            if (excludeEdgesNow) {
                                                maxPossible = false;
                                                break;          //we have an edge maximum;
                                            }
                                        }
                                        if (v2==v0) {           //prepare finding center of equal points (in case single point needed)
                                            types[offset2] |= EQUAL;
                                            xEqual += x2;
                                            yEqual += y2;
                                            nEqual ++;
                                        }
                                    }
                                } // if isWithin & not LISTED
                            } // for directions d
                            listI++;
                        } while (listI < listLen);

                        if (sortingError)  {				  //if x0,y0 was not the true maximum but we have reached a higher one
                            for (listI=0; listI<listLen; listI++)
                                types[pList[listI]] = 0;	//reset all points encountered, then retry
                        } else {
                            int resetMask = ~(maxPossible?LISTED:(LISTED|EQUAL));
                            xEqual /= nEqual;
                            yEqual /= nEqual;
                            double minDist2 = 1e20;
                            int nearestI = 0;
                            for (listI=0; listI<listLen; listI++) {
                                int offset = pList[listI];
                                int x = offset % width;
                                int y = offset / width;
                                types[offset] &= resetMask;		//reset attributes no longer needed
                                types[offset] |= PROCESSED;		//mark as processed
                                if (maxPossible) {
                                    types[offset] |= MAX_AREA;
                                    if ((types[offset]&EQUAL)!=0) {
                                        double dist2 = (xEqual-x)*(double)(xEqual-x) + (yEqual-y)*(double)(yEqual-y);
                                        if (dist2 < minDist2) {
                                            minDist2 = dist2;	//this could be the best "single maximum" point
                                            nearestI = listI;
                                        }
                                    }
                                }
                            } // for listI
                            if (maxPossible) {
                                int offset = pList[nearestI];
                                types[offset] |= MAX_POINT;
                                if (!isEdgeMaximum) {
                                    int x = offset % width;
                                    int y = offset / width;
                                    if ((roi==null || roi.contains(x, y)) && (!useMaxPeakFindValue || ip.getPixelValue(x,y)< maxPeakFindValue))
                                        xyVector.addElement(new int[] {x, y});
                                }
                            }
                        } //if !sortingError
                    } while (sortingError);				//redo if we have encountered a higher maximum: handle it now.
                } // for all maxima iMax

                if (canceled) return;
                if (xyVector!=null) {
                    
                    npoints = xyVector.size();
                    
                    if (npoints>0) 
                        {            
                        if (npoints>maxNumStars) npoints = maxNumStars;
                        xdpoints = new double[npoints];
                        ydpoints = new double[npoints];
                        int[] xy;
                        
                        for (int i=0; i<npoints; i++) {
                            xy = (int[])xyVector.elementAt(i);
                            if (useCentroid && centroid.measure(imp, xy[0], xy[1], apertureRadius, apertureBack1, apertureBack2, useCentroid, backPlane, removeBackStars))
                                {
                                xdpoints[i]=centroid.xCenter;
                                ydpoints[i]=centroid.yCenter;
                                if (i % 100 == 0)
                                    {
                                    if (canceled) return;
                                    String status = "Finding centroids";
                                    for (int n = 0; n<(i/100) % 10; n++)
                                        {
                                        status += " .";
                                        }
                                    IJ.showStatus(status);                                
                                    }
                                }
                            else
                                {
                                xdpoints[i] = xy[0]+0.5;
                                ydpoints[i] = xy[1]+0.5;
                                }
                            }
                        if (impOriginal!=null) {
                            for (int i=0; i<npoints; i++)
                                {
                                addAnnotateRoi (impOriginal, true, useCentroid, false, true, xdpoints[i], ydpoints[i], useCentroid?apertureRadius:annotateRadius, "", sourceColor);
                                }
                            impOriginal.draw();
                        }
                    } 
                }
            } //void analyzeAndMarkMaxima
        


        /** create an array of offsets within a pixel array for directions in clockwise order:
        * 0=(x,y-1), 1=(x+1,y-1), ... 7=(x-1,y)
        * Also creates further class variables:
        * width, height, and the following three values needed for storing coordinates in single ints for watershed:
        * intEncodeXMask, intEncodeYMask and intEncodeShift.
        * E.g., for width between 129 and 256, xMask=0xff and yMask = 0xffffff00 are bitwise masks
        * for x and y, respectively, and shift=8 is the bit shift to get y from the y-masked value
        * Returns as class variables: the arrays of the offsets to the 8 neighboring pixels
        * and the array maskAndShift for watershed
        */
        void makeDirectionOffsets(ImageProcessor ip) {
            int shift = 0, mult=1;
            do {
                shift++; mult*=2;
            }
            while (mult < width);
//            intEncodeXMask = mult-1;
//            intEncodeYMask = ~intEncodeXMask;
//            intEncodeShift = shift;
            //IJ.log("masks (hex):"+Integer.toHexString(xMask)+","+Integer.toHexString(xMask)+"; shift="+shift);
            dirOffset  = new int[] {-width, -width+1, +1, +width+1, +width, +width-1,   -1, -width-1 };
            //dirOffset is created last, so check for it being null before makeDirectionOffsets
            //(in case we have multiple threads using the same MaximumFinder)
        }      
        
        
        /** returns whether the neighbor in a given direction is within the image
        * NOTE: it is assumed that the pixel x,y itself is within the image!
        * Uses class variables width, height: dimensions of the image
        * @param x         x-coordinate of the pixel that has a neighbor in the given direction
        * @param y         y-coordinate of the pixel that has a neighbor in the given direction
        * @param direction the direction from the pixel towards the neighbor (see makeDirectionOffsets)
        * @return          true if the neighbor is within the image (provided that x, y is within)
        */
        boolean isWithin(int x, int y, int direction) {
            int xmax = width - 1;
            int ymax = height -1;
            switch(direction) {
                case 0:
                    return (y>0);
                case 1:
                    return (x<xmax && y>0);
                case 2:
                    return (x<xmax);
                case 3:
                    return (x<xmax && y<ymax);
                case 4:
                    return (y<ymax);
                case 5:
                    return (x>0 && y<ymax);
                case 6:
                    return (x>0);
                case 7:
                    return (x>0 && y>0);
            }
            return false;   //to make the compiler happy :-)
        } // isWithin  
        
	protected void addAnnotateRoi (ImagePlus implus, boolean showCir, boolean isCentered, boolean showLab, boolean isSourceROI, double x, double y, double rad, String labelText, Color col)
		{
    	AnnotateRoi roi = new AnnotateRoi (showCir, isCentered, showLab, true, x, y, rad, labelText, col);
        roi.setIsSourceROI(isSourceROI);
        roi.setImage (implus);
        OverlayCanvas overlayCanvas = OverlayCanvas.getOverlayCanvas(implus);
		overlayCanvas.add (roi);
        if (!isSourceROI && addAnnotationsToHeader)
            {
            String value = "'"+uptoTwoPlaces.format(IJU.ijX2fitsX(x))+","+uptoTwoPlaces.format(IJU.ijY2fitsY(impOriginal.getHeight(), y))+","+uptoTwoPlaces.format(rad)+","+(showCir?"1":"0")+","+
                                                   (isCentered?"1":"0")+","+(showLab?"1":"0")+",1,"+IJU.colorNameOf(col)+"'";  //the last '1' indicates from astrometry
            FitsJ.putHeader(impOriginal, FitsJ.addAnnotateCard(value, labelText, FitsJ.getHeader(impOriginal)));
            if (resaveRaw) FitsJ.putHeader(impRaw, FitsJ.addAnnotateCard(value, labelText, FitsJ.getHeader(impRaw)));
            }
		}     
    public void setAstrometryCanceled()
        {
        canceled = true;
        }

    public boolean isSetupActive()
        {
        return setupActive;
        }    
    
    
    public boolean getAstrometryCanceled()
        {
        return canceled;
        }    
    
//        /** Handle the key typed event from the image canvas. */
//    public void keyTyped(KeyEvent e) {
//
//    }
//
//    /** Handle the key-pressed event from the image canvas. */
//    public void keyPressed(KeyEvent e) {
//        int keyCode = e.getKeyCode();
//        if (keyCode == KeyEvent.VK_ESCAPE)//IJ.escapePressed())
//            {
//            IJ.beep();
//            canceled = true;
//            }
//        else if (keyCode == KeyEvent.VK_ENTER)
//            {
//            }
//    }
//
//    /** Handle the key-released event from the image canvas. */
//    public void keyReleased(KeyEvent e) {
//
//    }    
    
    public void getPrefs()
        {
        
        userKey = Prefs.get ("astrometry.userKey",userKey);
        useAlternateAstrometryServer = Prefs.get ("astrometry.useAlternateAstrometryServer", useAlternateAstrometryServer);
        alternateAstrometryUrlBase = Prefs.get ("astrometry.alternateAstrometryUrlBase",alternateAstrometryUrlBase);        
        processStack = Prefs.get ("astrometry.processStack",processStack);
        startSlice = (int)Prefs.get ("astrometry.startSlice",startSlice);
        endSlice = (int)Prefs.get ("astrometry.endSlice",endSlice);   
        defaultAnnotationColor = Prefs.get("Astronomy_Tool.defaultAnnotationColor", defaultAnnotationColor);
        autoSave = Prefs.get ("astrometry.autoSave", autoSave);
        DPSaveRawWithWCS = Prefs.get ("astrometry.DPSaveRawWithWCS", DPSaveRawWithWCS);
        skipIfHasWCS = Prefs.get ("astrometry.skipIfHasWCS", skipIfHasWCS);

        annotate = Prefs.get ("astrometry.annotate", annotate);
        annotateRadius = Prefs.get ("astrometry.annotateRadius",annotateRadius);
        addAnnotationsToHeader = Prefs.get ("astrometry.addAnnotationsToHeader", addAnnotationsToHeader);

        useMedianFilter = Prefs.get ("astrometry.useMedianFilter", useMedianFilter);
        medianFilterRadius = (int)Prefs.get ("astrometry.medianFilterRadius",medianFilterRadius);   

        minPeakFindToleranceSTDEV = Prefs.get ("astrometry.minPeakFindToleranceSTDEV",minPeakFindToleranceSTDEV);
        useMaxPeakFindValue = Prefs.get ("astrometry.useMaxPeakFindValue", useMaxPeakFindValue);
        maxPeakFindValue = Prefs.get ("astrometry.maxPeakFindValue",maxPeakFindValue);

        maxNumStars = (int)Prefs.get ("astrometry.maxNumStars",maxNumStars);
        useDistortionOrder = Prefs.get ("astrometry.useDistortionOrder", useDistortionOrder);
        distortionOrder = (int)Prefs.get ("astrometry.distortionOrder",distortionOrder);

        useCentroid = Prefs.get ("astrometry.useCentroid", useCentroid);
        removeBackStars = Prefs.get("aperture.removebackstars", removeBackStars);
        backPlane = Prefs.get("aperture.backplane", backPlane);
        apertureRadius = Prefs.get ("astrometry.apertureRadius",apertureRadius);
        apertureBack1 = Prefs.get ("astrometry.apertureBack1",apertureBack1);
        apertureBack2 = Prefs.get ("astrometry.apertureBack2",apertureBack2);

        useScale = Prefs.get ("astrometry.useScale", useScale);
        scaleEstimate = Prefs.get ("astrometry.scaleEstimate",scaleEstimate);
        scaleError = Prefs.get ("astrometry.scaleError",scaleError);


        useRaDec = Prefs.get ("astrometry.useRaDec", useRaDec);
        ra = Prefs.get ("astrometry.ra",ra);
        dec = Prefs.get ("astrometry.dec",dec);
        raDecRadius = Prefs.get ("astrometry.raDecRadius",raDecRadius);   

        showLog = Prefs.get ("astrometry.showLog", showLog);
        }
	}

