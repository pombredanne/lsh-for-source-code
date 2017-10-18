package org.nees.rbnb;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.MalformedURLException;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.nees.rbnb.RBNBFrameUtility;
import com.rbnb.sapi.ChannelMap;
import com.rbnb.sapi.Source;
import com.rbnb.sapi.SAPIException;

/**
 * FlexTpsSource
 * 
 * @author Jason P. Hanley
 * @author Terry E. Weymouth
 * @author Paul Hubbard
 * @author Lawrence J. Miller
 * 
 * adopted from org.nees.buffalo.video.AxisSource written by Lord Jason P.
 * Hanley
 * 
 * TpsSouce - takes an image stream from an FlexTPS source and put the JPEG
 * images into RBNB with timestamps.
 * @see org.nees.buffalo.axis.AxisSource
 */
public class FlexTpsSource extends MJPEGSource {

    private static final int DEFAULT_FPS = 10;

    private int cameraFPS = DEFAULT_FPS;

    private String urlStreamName = null;

    private String urlUsername = null;

    private String urlPassword = null;

    private String completeCameraURLwarningMsg = "ERROR: the \'R\' parameter is mutually exclusive the \'A\', " + "\'F\', \'N\',and \'T\' parameters.";

    private String urlType = null;

    private URL mjpegURL = null;

    private String cameraURLString;

    private boolean hasCompleteCameraURL = false;

    private static final String DEFAULT_VERSION = "1.x.x";

    private String urlVersion = DEFAULT_VERSION;

    public static void main(String[] args) {
        final FlexTpsSource a = new FlexTpsSource();
        if (a.parseArgs(args)) {
            a.start();
        }
        Runtime.getRuntime().addShutdownHook(new Thread() {

            public void run() {
                a.stop();
            }
        });
    }

    protected String getCVSVersionString() {
        return ("$LastChangedDate: 2008-04-15 20:12:19 -0400 (Tue, 15 Apr 2008) $\n" + "$LastChangedRevision: 36 $\n" + "$LastChangedBy: ljmiller.ucsd $\n" + "$HeadURL: http://oss-dataturbine.googlecode.com/svn/trunk/apps/oss-apps/src/org/nees/rbnb/FlexTpsSource.java $");
    }

    protected Options setOptions() {
        Options opt = setBaseOptions(new Options());
        opt.addOption("F", true, "Frames per second *" + DEFAULT_FPS + " (mutually exclusive with R)");
        opt.addOption("N", true, "User defined stream name (required - mutually exclusive with R)");
        opt.addOption("T", true, "Feed name (required - mutually exclusive with R)");
        opt.addOption("R", true, "flexTPS video source URL (required - mutually exclusive with A, F, N, and T)");
        opt.addOption("U", true, "Username (optional)");
        opt.addOption("P", true, "Password (optional)");
        opt.addOption("V", true, "Version (0.4.x or 1.x.x) *" + DEFAULT_VERSION + ")");
        return opt;
    }

    protected boolean setArgs(CommandLine cmd) {
        if (!setBaseArgs(cmd)) return false;
        if (cmd.hasOption('A')) {
            if (cmd.hasOption('R')) {
                System.err.println(completeCameraURLwarningMsg);
                return false;
            }
            String a = cmd.getOptionValue('A');
            if (a != null) hostName = a;
        }
        if (cmd.hasOption('N')) {
            if (cmd.hasOption('R')) {
                System.err.println(completeCameraURLwarningMsg);
                return false;
            }
            String a = cmd.getOptionValue('N');
            if (a != null) urlStreamName = a;
        }
        if (cmd.hasOption('F')) {
            if (cmd.hasOption('R')) {
                System.err.println(completeCameraURLwarningMsg);
                return false;
            }
            String a = cmd.getOptionValue('F');
            if (a != null) try {
                Integer i = new Integer(a);
                int value = i.intValue();
                cameraFPS = value;
            } catch (NumberFormatException nfe) {
                System.out.println("Please enter a numeric value for frames per second value (-F option). " + a + " is not valid!");
                return false;
            }
        }
        if (cmd.hasOption('T')) {
            if (cmd.hasOption('R')) {
                System.err.println(completeCameraURLwarningMsg);
                return false;
            }
            String a = cmd.getOptionValue('T');
            if (a != null) urlType = a;
        }
        if (cmd.hasOption('R')) {
            String a = cmd.getOptionValue('R');
            if (cmd.hasOption('A') || cmd.hasOption('F') || cmd.hasOption('N') || cmd.hasOption('T')) {
                System.err.println(completeCameraURLwarningMsg);
                return false;
            }
            if (a != null) {
                cameraURLString = a;
                String urlSplit[] = a.split("/");
                cameraFPS = Integer.parseInt(urlSplit[urlSplit.length - 1]);
                System.out.println("WARNING: use of the \'R\' option will not allow robotic PTZ camera control by rbnb clients.");
            }
            this.hasCompleteCameraURL = true;
        }
        if (cmd.hasOption('V')) {
            String a = cmd.getOptionValue('V');
            if (a != null) urlVersion = a;
        }
        if (cmd.hasOption('U')) {
            String a = cmd.getOptionValue('U');
            if (a != null) urlUsername = a;
        }
        if (cmd.hasOption('P')) {
            String a = cmd.getOptionValue('P');
            if (a != null) urlPassword = a;
        }
        if (getHostName() == null && !this.hasCompleteCameraURL) {
            System.err.println("TPS camera hostname or complete URL is required. Use FlexTpsSource -h for help");
            printUsage();
            return false;
        }
        if (urlType == null && !this.hasCompleteCameraURL) {
            System.err.println("TPS camera stream type is required. Use FlexTpsSource -h for help");
            printUsage();
            return false;
        }
        if (urlStreamName == null && !this.hasCompleteCameraURL) {
            System.err.println("TPS camera stream name is required. Use FlexTpsSource -h for help");
            printUsage();
            return false;
        }
        if (!this.hasCompleteCameraURL) {
            if (urlVersion.equals("0.4.x")) {
                cameraURLString = "http://" + getHostName() + "/feeds/" + urlType + "/nph-mjpeg_stream.pl?stream=" + urlStreamName;
            } else {
                cameraURLString = "http://" + getHostName() + "/feeds/" + urlType + "/" + urlStreamName + "/mjpeg/" + cameraFPS;
            }
        }
        cameraURLString = cameraURLString.replaceAll(" ", "%20");
        try {
            mjpegURL = new URL(cameraURLString);
        } catch (MalformedURLException e) {
            System.err.println("URL is malformed; URL = " + cameraURLString);
            disconnect();
            return false;
        }
        System.out.println("Starting FlexTpsSource with... \n" + " flexTPS video source URL = " + ((this.hasCompleteCameraURL) ? mjpegURL : cameraURLString) + "\n" + " RBNB Server = " + getServer() + "\n" + " RBNB Source name = " + getRBNBClientName() + "\n" + " RBNB Channel name = " + getRBNBChannelName() + "\n" + "\nUse FlexTpsSource -h to see all arguments.");
        return true;
    }

    /**
   * 
   *
   */
    protected URLConnection getCameraConnection() {
        URL cameraURL = getMJPEGURL();
        if (cameraURL == null) {
            System.err.println("Camera URL is null.");
            disconnect();
            return null;
        }
        URLConnection cameraConnection;
        try {
            cameraConnection = cameraURL.openConnection();
        } catch (IOException e) {
            System.err.println("Failed to connect to video host with " + cameraURL);
            disconnect();
            return null;
        }
        if (urlUsername != null) {
            if (urlPassword == null) urlPassword = "";
            if (urlVersion.equals("0.4.x")) {
                String authString = urlUsername + ":" + urlPassword;
                String encodedAuthString = new sun.misc.BASE64Encoder().encode(authString.getBytes());
                cameraConnection.setRequestProperty("Authorization", "Basic " + encodedAuthString);
            } else {
                try {
                    String sha1Hex = encodeSHA1Hex(urlUsername + encodeSHA1Hex(urlPassword));
                    String cookie = "flexTPS_session=" + urlUsername + "%3D" + sha1Hex;
                    cameraConnection.setRequestProperty("Cookie", cookie);
                } catch (Exception e) {
                    System.out.println("Unable to to username and password.");
                    e.printStackTrace();
                    disconnect();
                    return null;
                }
            }
        }
        return cameraConnection;
    }

    private static double parseISO8601Date(String dateString) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date time = sdf.parse(dateString);
            String subSecondsString = dateString.substring(20, dateString.length() - 1);
            double subSeconds = Integer.parseInt(subSecondsString) / 1000000d;
            double timeStamp = time.getTime() / 1000d + subSeconds;
            return timeStamp;
        } catch (Exception e) {
            System.err.println("Unabled to parse: " + dateString + ".");
            e.printStackTrace();
            return -1;
        }
    }

    private static String encodeSHA1Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA1");
            byte[] digest = md.digest(s.getBytes());
            return toHexString(digest);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String toHexString(byte bytes[]) {
        StringBuffer retString = new StringBuffer();
        for (int i = 0; i < bytes.length; ++i) {
            retString.append(Integer.toHexString(0x0100 + (bytes[i] & 0x00FF)).substring(1));
        }
        return retString.toString();
    }

    protected URL getMJPEGURL() {
        return mjpegURL;
    }

    protected int getFPS() {
        return this.cameraFPS;
    }
}
