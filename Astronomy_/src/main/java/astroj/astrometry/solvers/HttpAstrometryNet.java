package astroj.astrometry.solvers;

import astroj.Astrometry;
import astroj.json.simple.JSONArray;
import astroj.json.simple.JSONObject;
import astroj.json.simple.parser.JSONParser;
import astroj.json.simple.parser.ParseException;
import ij.IJ;
import ij.Prefs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;

import static astroj.json.simple.JSONValue.toJSONString;

/**
 * Plate solver backed by the Astrometry.net HTTP API.
 */
public final class HttpAstrometryNet implements PlateSolver {
    private static final int CONNECT_TIMEOUT_MS = 60_000;
    private static final int READ_TIMEOUT_MS = 60_000;
    // Timeouts are expressed as a number of polls (each poll is followed by a wait of
    // the configured polling interval), matching the original Astrometry behavior.
    private static final int DEFAULT_SUBMISSION_MAX_POLLS = 900;
    private static final int ALTERNATE_SERVER_MAX_POLLS = 120;
    public static final int DEFAULT_JOB_MAX_POLLS = 600;

    private final JSONParser parser = new JSONParser();
    private final String serverUrl;
    private final int pollingTimeoutMs;
    private final int submissionMaxPolls;
    private final int jobMaxPolls;
    private final String session;
    private final String loginError;
    private final boolean fetchAnnotations;
    private final BooleanSupplier cancelled;
    private final int maxRetries;

    public HttpAstrometryNet(boolean fetchAnnotations, BooleanSupplier cancelled, int maxRetries) {
        this(fetchAnnotations, cancelled, maxRetries, DEFAULT_JOB_MAX_POLLS);
    }

    /**
     * @param jobMaxPolls maximum number of status polls for a submitted job before it is
     *                    considered timed out
     */
    public HttpAstrometryNet(boolean fetchAnnotations, BooleanSupplier cancelled, int maxRetries, int jobMaxPolls) {
        boolean useAlternate = Prefs.get("astrometry.useAlternateAstrometryServer", false);
        String alternateUrl = Prefs.get("astrometry.alternateAstrometryUrlBase", "http://127.0.0.1:8080");
        String apiKey = Prefs.get("astrometry.userKey", "").trim();

        serverUrl = trimTrailingSlash(useAlternate
                ? alternateUrl
                : "https://nova.astrometry.net");
        pollingTimeoutMs = Math.max(1, useAlternate
                ? Astrometry.CUSTOM_POLLING.get()
                : Astrometry.ASTRO_NET_POLLING.get()) * 1000;
        submissionMaxPolls = useAlternate ? ALTERNATE_SERVER_MAX_POLLS : DEFAULT_SUBMISSION_MAX_POLLS;
        this.jobMaxPolls = Math.max(1, jobMaxPolls);

        String obtainedSession = null;
        String error = null;
        if (apiKey.isEmpty() && !useAlternate) {
            error = "Astrometry.net user key is empty. Enter a key in Astrometry Settings.";
        } else {
            try {
                Map<String, Object> request = new LinkedHashMap<>();
                request.put("apikey", apiKey);
                JSONObject response = postFormJson(serverUrl + "/api/login", "request-json",
                        toJSONString(request));
                String status = stringValue(response.get("status"));
                if ("error".equalsIgnoreCase(status)) {
                    error = "Astrometry.net login failed: " + response;
                } else {
                    obtainedSession = stringValue(response.get("session"));
                    if (obtainedSession == null || obtainedSession.isBlank()) {
                        error = "Astrometry.net login response did not contain a session.";
                    } else {
                        IJ.showStatus("Astrometry.net login successful");
                    }
                }
            } catch (Exception e) {
                error = "Astrometry.net login failed: " + e.getMessage();
            }
        }
        session = obtainedSession;
        loginError = error;
        this.fetchAnnotations = fetchAnnotations;
        this.cancelled = cancelled;
        this.maxRetries = maxRetries;
    }

    @Override
    public Result solve(double[] xs, double[] ys,
                        int imageWidth, int imageHeight,
                        double scaleLower, double scaleUpper,
                        double initialRa, double initialDec, double searchRadius,
                        boolean useDistortionOrder, double distortionOrder) {
        if (loginError != null) {
            return new Result.Failure(loginError);
        }
        if (xs == null || ys == null || xs.length != ys.length || xs.length == 0) {
            return new Result.Failure("No valid source positions were provided.");
        }

        int attempts = Math.max(0, maxRetries) + 1;
        Result lastResult = null;
        for (int attempt = 1; attempt <= attempts; attempt++) {
            if (isCancelled(cancelled)) {
                return new Result.Failure("Plate solving canceled.", true);
            }
            try {
                lastResult = solveAttempt(xs, ys, imageWidth, imageHeight, scaleLower, scaleUpper,
                        initialRa, initialDec, searchRadius, useDistortionOrder, distortionOrder);
                if (!(lastResult instanceof Result.Failure(String message, boolean cancelled1)) || cancelled1) {
                    return lastResult;
                }
                if (attempt < attempts) {
                    IJ.log("Astrometry.net attempt " + attempt + " failed; retrying: " + message);
                }
            } catch (CancellationException e) {
                return new Result.Failure("Plate solving canceled.", true);
            } catch (Exception e) {
                lastResult = new Result.Failure("Astrometry.net solve failed: " + e.getMessage());
                if (attempt < attempts) IJ.log("Astrometry.net attempt " + attempt + " failed; retrying: " + e.getMessage());
            }
        }
        return lastResult == null ? new Result.Failure("Plate solving failed.") : lastResult;
    }

    @Override
    public boolean requiresAwait() {
        return true;
    }

    private Result solveAttempt(double[] xs, double[] ys,
                                int imageWidth, int imageHeight,
                                double scaleLower, double scaleUpper,
                                double initialRa, double initialDec, double searchRadius,
                                boolean useDistortionOrder, double distortionOrder) throws Exception {
        checkCancelled(cancelled);
        Map<String, Object> submission = new LinkedHashMap<>();
        submission.put("allow_commercial_use", "d");
        submission.put("allow_modifications", "d");
        submission.put("publicly_visible", "n");
        submission.put("image_width", imageWidth);
        submission.put("image_height", imageHeight);
        submission.put("downsample_factor", 1);
        submission.put("crpix_center", true);
        submission.put("session", session);

        if (Double.isFinite(scaleLower) && Double.isFinite(scaleUpper)
                && scaleLower > 0 && scaleUpper >= scaleLower) {
            submission.put("scale_units", "arcsecperpix");
            submission.put("scale_type", "ul");
            submission.put("scale_lower", scaleLower);
            submission.put("scale_upper", scaleUpper);
        }
        if (Double.isFinite(initialRa) && Double.isFinite(initialDec)
                && initialDec >= -90 && initialDec <= 90
                && Double.isFinite(searchRadius) && searchRadius > 0) {
            submission.put("center_ra", initialRa * 15.0);
            submission.put("center_dec", initialDec);
            submission.put("radius", searchRadius / 60.0);
        }
        submission.put("tweak_order", useDistortionOrder
                ? Math.clamp((int) distortionOrder, 2, 9)
                : 0);

        StringBuilder sources = new StringBuilder(xs.length * 24);
        int count = 0;
        for (int i = 0; i < xs.length; i++) {
            if (!Double.isFinite(xs[i]) || !Double.isFinite(ys[i])) {
                continue;
            }
            // Astrometry.net source-list coordinates have an origin at the
            // lower-left; ImageJ coordinates have an origin at the upper-left.
            sources.append(xs[i]).append(" \t ")
                    .append(imageHeight - ys[i]).append("\r\n");
            count++;
        }
        if (count == 0) {
            return new Result.Failure("No finite source positions were provided.");
        }
        sources.append("\r\n");

        checkCancelled(cancelled);
        JSONObject upload = postMultipart(serverUrl + "/api/upload",
                toJSONString(submission), sources.toString());
        if (!"success".equalsIgnoreCase(stringValue(upload.get("status")))) {
            return new Result.Failure("Astrometry.net upload failed: " + upload);
        }
        Object submissionId = upload.get("subid");
        if (submissionId == null) {
            return new Result.Failure("Astrometry.net upload response did not contain a submission ID.");
        }
        IJ.showStatus("Astrometry.net submission " + submissionId + " queued");

        String jobId = waitForJobId(submissionId.toString(), cancelled);
        if (jobId == null) {
            return new Result.Failure("Timed out waiting for Astrometry.net submission " + submissionId + ".");
        }
        String status = waitForJob(jobId, cancelled);
        if (!"success".equals(status)) {
            return new Result.Failure("Astrometry.net job " + jobId + " ended with status " + status + ".");
        }

        checkCancelled(cancelled);
        String wcs = getText(serverUrl + "/wcs_file/" + jobId, cancelled).trim();
        if (wcs.isEmpty()) {
            return new Result.Failure("Astrometry.net returned an empty WCS header for job " + jobId + ".");
        }
        var annotations = fetchAnnotations ? getAnnotations(jobId, cancelled) : null;
        checkCancelled(cancelled);
        IJ.showStatus("Astrometry.net job " + jobId + ": SOLVED");
        return new Result.Success(wcs, annotations);
    }

    private List<AstrometryObject> getAnnotations(String jobId, BooleanSupplier cancelled) {
        if (jobId == null || jobId.isBlank() || isCancelled(cancelled)) return null;
        try {
            var objJson = parseJson(getText(serverUrl + "/api/jobs/" + jobId + "/annotations", cancelled));
            if (!(objJson.get("annotations") instanceof JSONArray annos)) {
                return null;
            }
            var annotations = new ArrayList<AstrometryObject>();
            for (var anno : annos) {
                if (!(anno instanceof JSONObject annoJson)) {
                    continue;
                }
                // Number, not Double: the JSON parser returns Long for integer-valued literals.
                if (!(annoJson.get("radius") instanceof Number radius)) {
                    continue;
                }
                if (!(annoJson.get("pixelx") instanceof Number pixelX)) {
                    continue;
                }
                if (!(annoJson.get("pixely") instanceof Number pixelY)) {
                    continue;
                }
                if (!(annoJson.get("type") instanceof String type)) {
                    continue;
                }
                if (!(annoJson.get("names") instanceof JSONArray jsonNames)) {
                    continue;
                }
                var names = new ArrayList<String>();
                for (var name : jsonNames) {
                    if (!(name instanceof String nameStr)) {
                        continue;
                    }
                    names.add(nameStr);
                }
                annotations.add(new AstrometryObject(radius.doubleValue(), type, names,
                        pixelX.doubleValue(), pixelY.doubleValue()));
            }

            return annotations;
        } catch (Exception e) {
            IJ.log("Could not retrieve Astrometry.net annotations for job " + jobId + ": " + e.getMessage());
            return null;
        }
    }

    private String waitForJobId(String submissionId, BooleanSupplier cancelled) throws Exception {
        for (int poll = 0; poll < submissionMaxPolls; poll++) {
            checkCancelled(cancelled);
            try {
                JSONObject response = getJson(serverUrl + "/api/submissions/" + submissionId, cancelled);
                Object jobs = response.get("jobs");
                if (jobs instanceof java.util.List<?> list) {
                    for (Object job : list) {
                        if (job != null && !"null".equals(job.toString())) {
                            return job.toString();
                        }
                    }
                }
                IJ.showStatus("Astrometry submission " + submissionId + " processing"
                        + " .".repeat(poll % 10));
            } catch (IOException | ParseException e) {
                // The submission may not exist yet, or the server may be briefly unavailable.
                // Count it as a poll and keep waiting rather than failing the attempt.
                IJ.log("Astrometry submission " + submissionId + " check error: " + e.getMessage());
            }
            pause(pollingTimeoutMs, cancelled);
        }
        return null;
    }

    private String waitForJob(String jobId, BooleanSupplier cancelled) throws Exception {
        for (int poll = 0; poll < jobMaxPolls; poll++) {
            checkCancelled(cancelled);
            JSONObject response = getJson(serverUrl + "/api/jobs/" + jobId, cancelled);
            String status = stringValue(response.get("status"));
            if ("success".equalsIgnoreCase(status)) {
                return "success";
            }
            if ("failure".equalsIgnoreCase(status) || "error".equalsIgnoreCase(status)) {
                return status;
            }
            IJ.showStatus("Astrometry job " + jobId + " " + status);
            pause(pollingTimeoutMs, cancelled);
        }
        return "timeout";
    }

    private JSONObject postFormJson(String url, String field, String json) throws Exception {
        String body = field + "=" + URLEncoder.encode(json, StandardCharsets.UTF_8);
        HttpURLConnection connection = open(url);
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        try (OutputStream out = connection.getOutputStream()) {
            out.write(body.getBytes(StandardCharsets.UTF_8));
        }
        return parseJson(readResponse(connection));
    }

    private JSONObject postMultipart(String url, String requestJson, String sourceList) throws Exception {
        String boundary = "----------------AIJ" + Long.toHexString(System.nanoTime());
        HttpURLConnection connection = open(url);
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        try (OutputStream out = connection.getOutputStream()) {
            write(out, "--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"request-json\"\r\n"
                    + "Content-Type: text/plain; charset=UTF-8\r\n\r\n"
                    + requestJson + "\r\n");
            write(out, "--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"file\"; filename=\"maxima.tbl\"\r\n"
                    + "Content-Type: application/octet-stream\r\n\r\n");
            write(out, sourceList);
            write(out, "\r\n--" + boundary + "--\r\n");
        }
        return parseJson(readResponse(connection));
    }

    private JSONObject getJson(String url, BooleanSupplier cancelled) throws Exception {
        checkCancelled(cancelled);
        HttpURLConnection connection = open(url);
        connection.setRequestMethod("GET");
        return parseJson(readResponse(connection));
    }

    private String getText(String url, BooleanSupplier cancelled) throws IOException {
        checkCancelled(cancelled);
        HttpURLConnection connection = open(url);
        connection.setRequestMethod("GET");
        return readResponse(connection);
    }

    private HttpURLConnection open(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setUseCaches(false);
        connection.setRequestProperty("Accept", "application/json, text/plain, */*");
        return connection;
    }

    private String readResponse(HttpURLConnection connection) throws IOException {
        int code = connection.getResponseCode();
        var stream = code >= 400 ? connection.getErrorStream() : connection.getInputStream();
        if (stream == null) {
            throw new IOException("HTTP " + code + " from " + connection.getURL());
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
            if (code >= 400) {
                throw new IOException("HTTP " + code + " from " + connection.getURL() + ": " + result);
            }
            return result.toString();
        } finally {
            connection.disconnect();
        }
    }

    private JSONObject parseJson(String text) throws Exception {
        Object parsed = parser.parse(text);
        if (!(parsed instanceof JSONObject object)) {
            throw new IOException("Expected a JSON object but received: " + text);
        }
        return object;
    }

    private static boolean isCancelled(BooleanSupplier cancelled) {
        return Thread.currentThread().isInterrupted() || (cancelled != null && cancelled.getAsBoolean());
    }

    private static void checkCancelled(BooleanSupplier cancelled) {
        if (isCancelled(cancelled)) throw new CancellationException("Plate solving canceled");
    }

    private static void pause(int millis, BooleanSupplier cancelled) throws InterruptedException {
        long deadline = System.nanoTime() + millis * 1_000_000L;
        while (System.nanoTime() < deadline) {
            checkCancelled(cancelled);
            long remaining = deadline - System.nanoTime();
            Thread.sleep(Math.clamp(remaining / 1_000_000L, 1, 100));
        }
        checkCancelled(cancelled);
    }

    private static void write(OutputStream out, String text) throws IOException {
        out.write(text.getBytes(StandardCharsets.UTF_8));
    }

    private static String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private static String trimTrailingSlash(String url) {
        String result = url == null ? "" : url.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    @Override
    public void close() throws Exception {

    }
}