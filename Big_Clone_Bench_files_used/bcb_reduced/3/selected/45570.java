package testing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import data.DataInfo;

public class PredictionTester {

    private static int[] trainingUserIDs;

    private static short[] trainingMoviesIDs;

    private static byte[] trainingRatings;

    public static void loadTrainingData(boolean userIndexed) {
        String filename = DataInfo.downloadFolder + "betterprobe.txt";
        if (userIndexed) {
            filename = DataInfo.downloadFolder + "betterprobeuserindexed.txt";
        }
        System.out.println("Using probe file: " + filename);
        File probeFile = new File(filename);
        int numProbe = 1408395;
        Notifier n = new Notifier(numProbe, "Loading probe set: [", "]");
        int count = 0;
        trainingUserIDs = new int[numProbe];
        trainingMoviesIDs = new short[numProbe];
        trainingRatings = new byte[numProbe];
        try {
            BufferedReader in = new BufferedReader(new FileReader(probeFile));
            if (!in.ready()) throw new IOException();
            String line = "";
            short movieID = 0;
            int userID = 0;
            while ((line = in.readLine()) != null) {
                if (line.contains(":")) {
                    if (userIndexed) {
                        userID = Integer.parseInt(line.substring(0, line.length() - 1));
                    } else {
                        movieID = Short.parseShort(line.substring(0, line.length() - 1));
                    }
                } else {
                    ArrayList<String> tokens = PredictionTester.splitLine(line, ',');
                    if (userIndexed) {
                        movieID = Short.parseShort(tokens.get(1));
                    } else {
                        userID = Integer.parseInt(tokens.get(1));
                    }
                    byte rating = Byte.parseByte(tokens.get(0));
                    if (userID < 1) {
                        System.err.println("Invalid user ID: " + userID);
                        System.err.println("Line: " + line);
                    }
                    trainingUserIDs[count] = userID;
                    trainingMoviesIDs[count] = movieID;
                    trainingRatings[count] = rating;
                    n.next();
                    count++;
                }
            }
            in.close();
        } catch (IOException e) {
            System.out.println(e);
        }
        System.out.println("Loaded " + count + " entries in the probe set.");
    }

    public static void createQualifyingSubmission(NetflixPredictor predictionClass, String outputFilename) {
        File qualFile = new File(DataInfo.downloadFolder + "qualifying.txt");
        File outputFile = new File(outputFilename);
        Notifier n = new Notifier(2834601, "Predicting qualify set: [", "]");
        try {
            BufferedReader in = new BufferedReader(new FileReader(qualFile));
            BufferedWriter out = new BufferedWriter(new FileWriter(outputFile));
            if (!in.ready()) throw new IOException();
            String line = "";
            short movieID = 0;
            while ((line = in.readLine()) != null) {
                if (line.contains(":")) {
                    movieID = Short.parseShort(line.substring(0, line.length() - 1));
                    out.write(movieID + ":");
                    out.newLine();
                } else {
                    ArrayList<String> tokens = splitLine(line, ',');
                    int userID = Integer.parseInt(tokens.get(0));
                    double prediction = predictionClass.predictRating(userID, movieID);
                    String predict = "" + prediction;
                    if (predict.length() > 5) {
                        predict = predict.substring(0, 5);
                    }
                    out.write("" + predict);
                    out.newLine();
                }
                n.next();
            }
            in.close();
            out.close();
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    public static String getMD5FromFile(String filename) {
        InputStream is = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            File f = new File(filename);
            is = new FileInputStream(f);
            byte[] buffer = new byte[8192];
            int read = 0;
            while ((read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            byte[] md5sum = digest.digest();
            BigInteger bigInt = new BigInteger(1, md5sum);
            String output = bigInt.toString(16);
            return output;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        throw new RuntimeException("Could not get MD5 from file: " + filename);
    }

    public static ArrayList<String> splitLine(String line, char separator) {
        ArrayList<String> tokens = new ArrayList<String>();
        String cur = "";
        for (char c : line.toCharArray()) {
            if (c == separator) {
                tokens.add(cur);
                cur = "";
            } else {
                cur += c;
            }
        }
        tokens.add(cur);
        return tokens;
    }

    public static double getProbeError(NetflixPredictor predictionClass) {
        return getProbeError(predictionClass, false, false);
    }

    public static double getProbeError(NetflixPredictor predictionClass, boolean userIndexed) {
        return getProbeError(predictionClass, userIndexed, false);
    }

    public static double getProbeError(NetflixPredictor predictionClass, boolean userIndexed, boolean debug) {
        long start = System.currentTimeMillis();
        PredictionTester.loadTrainingData(userIndexed);
        final int trainingLength = trainingUserIDs.length;
        double[] error = new double[trainingLength];
        double squaredError = 0.0;
        double absSquaredError = 0.0;
        Notifier n = new Notifier(trainingRatings.length, "Testing on probe set: [", "]");
        for (int i = 0; i < trainingLength; i++) {
            double guess = predictionClass.predictRating(trainingUserIDs[i], trainingMoviesIDs[i]);
            double e = guess - trainingRatings[i];
            error[i] = e;
            squaredError += e * e;
            if (e > 0) absSquaredError += e * e; else absSquaredError -= e * e;
            if (debug && i % 50 == 0 && i > 0) {
                long timeTaken = System.currentTimeMillis() - start;
                System.out.println(i + " ratings in " + (timeTaken / 1000) + " seconds. avg: " + (timeTaken / i) + " ms each");
                System.out.println("Avg error so far: " + Math.sqrt(squaredError / i));
            }
            n.next();
        }
        String sign = "";
        if (absSquaredError < 0) sign = "-";
        double absRMSE = Math.abs(absSquaredError);
        absRMSE /= trainingLength;
        absRMSE = Math.sqrt(absRMSE);
        System.out.println("The mean absolute squared error is: " + sign + absRMSE);
        return Math.sqrt(squaredError / trainingLength);
    }
}
