package ij.astro.util;

import ij.astro.logging.AIJLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class LeapSeconds {
    protected Double[] leapSecJD;
    protected Double[] TAIminusUTC;
    protected Double[] baseMJD;
    protected Double[] baseMJDMultiplier;
    protected double leapSec;
    protected double[] utDateEOI = {2000, 1, 1, 12};
    protected double jdEOI = 2451545.0;
    protected double jdNow;
    protected Consumer<String> logger = AIJLogger::log;

    /**
     * Gives the number of leap seconds for the provided mjd.
     */
    public double getLeapSeconds(double mjd) {
        var utDateNow = SkyAlgorithmsTimeUtil.UTDateNow();
        jdNow = SkyAlgorithmsTimeUtil.CalcJD((int)utDateNow[0], (int)utDateNow[1], (int)utDateNow[2], utDateNow[3]);
        jdEOI = mjd;
        utDateEOI = SkyAlgorithmsTimeUtil.UTDateFromJD(mjd);
        if (!getTAIminusUTC()) {
            getTAIminusUTC(); //try a second time with default leap second arrays if current leap second arrays are null or different lengths
        }

        return leapSec;
    }

    protected boolean getTAIminusUTC() {
        leapSec = 0.0;
        if (leapSecJD == null || TAIminusUTC == null || baseMJD == null || baseMJDMultiplier == null ||
                leapSecJD.length < 2 || TAIminusUTC.length < 2 || baseMJD.length < 2 || baseMJDMultiplier.length < 2 ||
                leapSecJD.length != TAIminusUTC.length || leapSecJD.length != baseMJD.length || leapSecJD.length != baseMJDMultiplier.length) {
            initDefaultLeapSecs();
            try {
                getIERSLeapSecTable();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }
        if (jdEOI < leapSecJD[0]) {
            estimateLeapSecs();
            return true;
        }
        for (int i = 1; i < leapSecJD.length; i++) {
            if (jdEOI >= leapSecJD[i - 1] && jdEOI < leapSecJD[i]) {
                leapSec = TAIminusUTC[i - 1] + (jdEOI - 2400000 - baseMJD[i - 1]) * baseMJDMultiplier[i - 1];
                return true;
            }
        }
        if (jdEOI >= leapSecJD[leapSecJD.length - 1] && jdEOI <= jdNow + 365.0) {
            leapSec = TAIminusUTC[TAIminusUTC.length - 1] + (jdEOI - 2400000 - baseMJD[TAIminusUTC.length - 1]) * baseMJDMultiplier[TAIminusUTC.length - 1];
            return true;
        }
        estimateLeapSecs();
        return true;
    }

    protected void initDefaultLeapSecs() {
        leapSecJD = new Double[]{
                2437300.5,
                2437512.5,
                2437665.5,
                2438334.5,
                2438395.5,
                2438486.5,
                2438639.5,
                2438761.5,
                2438820.5,
                2438942.5,
                2439004.5,
                2439126.5,
                2439887.5,
                2441317.5,
                2441499.5,
                2441683.5,
                2442048.5,
                2442413.5,
                2442778.5,
                2443144.5,
                2443509.5,
                2443874.5,
                2444239.5,
                2444786.5,
                2445151.5,
                2445516.5,
                2446247.5,
                2447161.5,
                2447892.5,
                2448257.5,
                2448804.5,
                2449169.5,
                2449534.5,
                2450083.5,
                2450630.5,
                2451179.5,
                2453736.5,
                2454832.5,
                2456109.5,
                2457204.5,
                2457754.5
        };

        TAIminusUTC = new Double[]{
                1.4228180,
                1.3728180,
                1.8458580,
                1.9458580,
                3.2401300,
                3.3401300,
                3.4401300,
                3.5401300,
                3.6401300,
                3.7401300,
                3.8401300,
                4.3131700,
                4.2131700,
                10.0,
                11.0,
                12.0,
                13.0,
                14.0,
                15.0,
                16.0,
                17.0,
                18.0,
                19.0,
                20.0,
                21.0,
                22.0,
                23.0,
                24.0,
                25.0,
                26.0,
                27.0,
                28.0,
                29.0,
                30.0,
                31.0,
                32.0,
                33.0,
                34.0,
                35.0,
                36.0,
                37.0
        };


        baseMJD = new Double[]{
                37300.0,
                37300.0,
                37665.0,
                37665.0,
                38761.0,
                38761.0,
                38761.0,
                38761.0,
                38761.0,
                38761.0,
                38761.0,
                39126.0,
                39126.0,
                41317.0,
                41317.0,
                41317.0,
                41317.0,
                41317.0,
                41317.0,
                41317.0,
                41317.0,
                41317.0,
                41317.0,
                41317.0,
                41317.0,
                41317.0,
                41317.0,
                41317.0,
                41317.0,
                41317.0,
                41317.0,
                41317.0,
                41317.0,
                41317.0,
                41317.0,
                41317.0,
                41317.0,
                41317.0,
                41317.0,
                41317.0,
                41317.0
        };

        baseMJDMultiplier = new Double[]{
                0.001296,
                0.001296,
                0.0011232,
                0.0011232,
                0.001296,
                0.001296,
                0.001296,
                0.001296,
                0.001296,
                0.001296,
                0.001296,
                0.002592,
                0.002592,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0
        };

    }

    protected void getIERSLeapSecTable() throws IOException {
        URL leapSecTable = new URL("https://hpiers.obspm.fr/iers/bul/bulc/Leap_Second.dat");
        URLConnection leapSecTableCon = leapSecTable.openConnection();
        leapSecTableCon.setConnectTimeout(10000);
        leapSecTableCon.setReadTimeout(10000);
        BufferedReader in = new BufferedReader(new InputStreamReader(leapSecTableCon.getInputStream()));
        ArrayList<Double> leapJD = new ArrayList<>(Arrays.asList(leapSecJD));
        ArrayList<Double> leapSEC = new ArrayList<>(Arrays.asList(TAIminusUTC));
        ArrayList<Double> leapbaseMJD = new ArrayList<>(Arrays.asList(baseMJD));
        ArrayList<Double> leapbaseMJDMultiplier = new ArrayList<>(Arrays.asList(baseMJDMultiplier));
        double jd, leap, base;
        double oldjd = 0;
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            if (inputLine.startsWith("#")) continue;
            String[] parsed = Pattern.compile("(\\s+)").split(inputLine);
            base = Double.parseDouble(parsed[1]);
            jd = base + 2400000.5;
            if (jd < oldjd) {
                logger.accept("Julian Date table values are not in increasing order");
                break;
            }
            if (jd <= leapSecJD[leapSecJD.length - 1]) { // The current values are fine, so only want to get new leap seconds
                continue;
            }
            leap = Double.parseDouble(parsed[5]);
            leapJD.add(jd);
            leapSEC.add(leap);
            leapbaseMJD.add(41317.0);
            leapbaseMJDMultiplier.add(0D);
            oldjd = jd;
        }
        if (leapJD.size() > 0) {
            leapSecJD = leapJD.toArray(leapSecJD);
            TAIminusUTC = leapSEC.toArray(TAIminusUTC);
            baseMJD = leapbaseMJD.toArray(baseMJD);
            baseMJDMultiplier = leapbaseMJDMultiplier.toArray(baseMJDMultiplier);
        }

        in.close();
    }

    protected void estimateLeapSecs() {
        double y = utDateEOI[0] + (utDateEOI[1] - 0.5) / 12.0;
        double u;
        double dt;
        if (y < -1999) {
            y = -1999;
            u = (y - 1820) / 100;
            dt = -20 + 32 * u * u;
        } else if (y >= -1999 && y < -500.0) {
            u = (y - 1820) / 100;
            dt = -20 + 32 * u * u;
        } else if (y >= -500 && y < 500.0) {
            u = y / 100;
            double u2 = u * u;
            double u3 = u2 * u;
            double u4 = u3 * u;
            double u5 = u4 * u;
            double u6 = u5 * u;
            dt = 10583.6 - 1014.41 * u + 33.78311 * u2 - 5.952053 * u3 - 0.1798452 * u4 + 0.022174192 * u5 + 0.0090316521 * u6;
        } else if (y >= 500 && y < 1600.0) {
            u = (y - 1000) / 100;
            double u2 = u * u;
            double u3 = u2 * u;
            double u4 = u3 * u;
            double u5 = u4 * u;
            double u6 = u5 * u;
            dt = 1574.2 - 556.01 * u + 71.23472 * u2 + 0.319781 * u3 - 0.8503463 * u4 - 0.005050998 * u5 + 0.0083572073 * u6;
        } else if (y >= 1600 && y < 1700.0) {
            u = y - 1600;
            double u2 = u * u;
            double u3 = u2 * u;
            dt = 120 - 0.9808 * u - 0.01532 * u2 + u3 / 7129;
        } else if (y >= 1700 && y < 1800.0) {
            u = y - 1700;
            double u2 = u * u;
            double u3 = u2 * u;
            double u4 = u3 * u;
            dt = 8.83 + 0.1603 * u - 0.0059285 * u2 + 0.00013336 * u3 - u4 / 1174000;
        } else if (y >= 1800 && y < 1860.0) {
            u = y - 1800;
            double u2 = u * u;
            double u3 = u2 * u;
            double u4 = u3 * u;
            double u5 = u4 * u;
            double u6 = u5 * u;
            double u7 = u6 * u;
            dt = 13.72 - 0.332447 * u + 0.0068612 * u2 + 0.0041116 * u3 - 0.00037436 * u4 + 0.0000121272 * u5 - 0.0000001699 * u6 + 0.000000000875 * u7;
        } else if (y >= 1860 && y < 1900.0) {
            u = y - 1860;
            double u2 = u * u;
            double u3 = u2 * u;
            double u4 = u3 * u;
            double u5 = u4 * u;
            dt = 7.62 + 0.5737 * u - 0.251754 * u2 + 0.01680668 * u3 - 0.0004473624 * u4 + u5 / 233174;
        } else if (y >= 1900 && y < 1920.0) {
            u = y - 1900;
            double u2 = u * u;
            double u3 = u2 * u;
            double u4 = u3 * u;
            dt = -2.79 + 1.494119 * u - 0.0598939 * u2 + 0.0061966 * u3 - 0.000197 * u4;
        } else if (y >= 1920 && y < 1941.0) {
            u = y - 1920;
            double u2 = u * u;
            double u3 = u2 * u;
            dt = 21.20 + 0.84493 * u - 0.076100 * u2 + 0.0020936 * u3;
        } else if (y >= 1941 && y < 1961.0) {
            u = y - 1950;
            double u2 = u * u;
            double u3 = u2 * u;
            dt = 29.07 + 0.407 * u - u2 / 233 + u3 / 2547;
        } else if (y >= 1961 && y < 1986.0) {
            u = y - 1975;
            double u2 = u * u;
            double u3 = u2 * u;
            dt = 45.45 + 1.067 * u - u2 / 260 - u3 / 718;
        } else if (y >= 1986 && y < 2005.0) {
            u = y - 2000;
            double u2 = u * u;
            double u3 = u2 * u;
            double u4 = u3 * u;
            double u5 = u4 * u;
            dt = 63.86 + 0.3345 * u - 0.060374 * u2 + 0.0017275 * u3 + 0.000651814 * u4 + 0.00002373599 * u5;
        } else if (y >= 2005 && y < 2050.0) {
            u = y - 2000;
            double u2 = u * u;
            dt = 62.92 + 0.32217 * u + 0.005589 * u2;
        } else if (y >= 2050 && y < 2150.0) {
            dt = -20 + 32 * ((y - 1820) / 100) * ((y - 1820) / 100) - 0.5628 * (2150 - y);
        } else if (y >= 2150 && y < 3000.0) {
            u = (y - 1820) / 100;
            double u2 = u * u;
            dt = -20 + 32 * u2;
        } else {
            y = 3000.0;
            u = (y - 1820) / 100;
            double u2 = u * u;
            dt = -20 + 32 * u2;
        }
        leapSec = dt - 32.184;
    }

}
