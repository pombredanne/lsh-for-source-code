package net.sf.sail.emf.launch;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import net.sf.sail.emf.bridge.Base64;

public class BundlePoster {

    private static final Logger logger = Logger.getLogger(BundlePoster.class.getName());

    private boolean isConnected = false;

    public class BundlePostThread extends Thread {

        boolean success = false;

        Throwable postException;

        private byte[] portfolioBytes;

        public BundlePostThread(byte[] portfolioBytes) {
            this.portfolioBytes = portfolioBytes;
        }

        public void doPost(long timeoutTime) throws Exception {
            long currentTimeMillis = System.currentTimeMillis();
            long remainingTime = timeoutTime - currentTimeMillis;
            if (remainingTime <= 0) {
                throw new Exception("Ran out of time before posting bundle");
            }
            start();
            join(remainingTime);
            if (isAlive()) {
                logger.info((new Date()).toString() + ": Time's up.");
                if (!isConnected) {
                    logger.info((new Date()).toString() + ": bundle poster not connected, giving up");
                    interrupt();
                    join(100);
                    postException = new TimeoutException("Unable to connect in a timely manner.");
                } else if (isConnected) {
                    logger.info((new Date()).toString() + ": connected, giving more time");
                    join(2 * 60 * 1000);
                    if (isAlive()) {
                        logger.info((new Date()).toString() + ": time's up again");
                        logger.info((new Date()).toString() + ": interrupting this time");
                        interrupt();
                        join(100);
                        postException = new TimeoutException("Bundle upload happening too slowly.");
                    }
                }
            }
            if (postException instanceof Exception) {
                throw (Exception) postException;
            }
            if (postException instanceof Error) {
                throw (Error) postException;
            }
        }

        public void run() {
            try {
                postInternal(portfolioBytes);
            } catch (Exception e) {
                postException = e;
            }
        }
    }

    private boolean b64gzip = true;

    private String postUrl;

    /**
	 * If the bytes can not be posted successfully an exception will be thrown.
	 * Otherwise the bytes will have been set successfully.
	 * The post will give up after 60 seconds, and in that case will throw
	 * an exception.
	 * 
	 * @param portfolioBytes
	 * @throws Exception
	 */
    public void post(byte[] portfolioBytes) throws Exception {
        long timeoutTime = System.currentTimeMillis() + 60000;
        post(portfolioBytes, timeoutTime);
    }

    /**
	 * If the bytes can not be posted successfully an exception will be thrown.
	 * Otherwise the bytes will have been set successfully.  The post will
	 * give up if it doesn't finish before the timeoutTime. 
	 * 
	 * @param portfolioBytes
	 * @throws Exception
	 */
    public void post(byte[] portfolioBytes, long timeoutTime) throws Exception {
        BundlePostThread bundlePostThread = new BundlePostThread(portfolioBytes);
        bundlePostThread.doPost(timeoutTime);
    }

    /**
	 * process the stream at this location and send
	 * it to the server.  If there is any problem posting
	 * an exception will be thrown
	 * 
	 * @param portoflioStream
	 * @throws IOException 
	 * @throws TransformerException 
	 * @throws NoSuchAlgorithmException 
	 */
    public void postInternal(byte[] portfolioBytes) throws IOException, TransformerException, NoSuchAlgorithmException {
        ByteArrayInputStream portfolioStream = new ByteArrayInputStream(portfolioBytes);
        TransformerFactory factory = TransformerFactory.newInstance();
        factory.setErrorListener(new ErrorListener() {

            public void error(TransformerException arg0) throws TransformerException {
                arg0.printStackTrace();
            }

            public void fatalError(TransformerException arg0) throws TransformerException {
                arg0.printStackTrace();
            }

            public void warning(TransformerException arg0) throws TransformerException {
                arg0.printStackTrace();
            }
        });
        URL xsltUrl = getClass().getResource("portfolioPost.xslt");
        InputStream xsltStream = xsltUrl.openStream();
        Source xsltSource = new StreamSource(xsltStream, xsltUrl.toExternalForm());
        Transformer transformer = null;
        transformer = factory.newTransformer(xsltSource);
        Source inputSource = new StreamSource(portfolioStream);
        ByteArrayOutputStream transformed = new ByteArrayOutputStream();
        Result outputResult = new StreamResult(transformed);
        transformer.transform(inputSource, outputResult);
        byte[] outBytes = transformed.toByteArray();
        MessageDigest md;
        String localMD5Sum = null;
        md = MessageDigest.getInstance("MD5");
        md.update(outBytes, 0, outBytes.length);
        localMD5Sum = new BigInteger(1, md.digest()).toString(16);
        logger.info("Local md5: " + localMD5Sum);
        while (localMD5Sum.length() < 32) {
            localMD5Sum = "0" + localMD5Sum;
        }
        URL postRealUrl = new URL(getPostUrl());
        HttpURLConnection postConnection = (HttpURLConnection) postRealUrl.openConnection();
        postConnection.setRequestMethod("POST");
        postConnection.setDoOutput(true);
        postConnection.setRequestProperty("Content-Type", "application/xml");
        if (isB64Gzip()) {
            postConnection.setRequestProperty("Content-Encoding", "b64gzip");
        }
        if (localMD5Sum != null) {
            postConnection.setRequestProperty("Content-md5", localMD5Sum);
        }
        OutputStream postOut = postConnection.getOutputStream();
        isConnected = true;
        logger.info((new Date()).toString() + ": Bundle post connection established");
        if (isB64Gzip()) {
            postOut = new Base64.OutputStream(postOut, Base64.ENCODE);
            postOut = new GZIPOutputStream(postOut);
        }
        logger.info((new Date()).toString() + ": Starting bundle post data transaction");
        postOut.write(outBytes);
        postOut.flush();
        postOut.close();
        InputStream postIn = null;
        try {
            postIn = postConnection.getInputStream();
            logger.info((new Date()).toString() + ": Output stream finished, input stream established.");
        } catch (IOException e) {
            BundlePostException postException = new BundlePostException(postConnection.getResponseCode(), postUrl + ": " + postConnection.getResponseMessage());
            String errorMessage = getErrorMessage(postConnection);
            if (errorMessage != null) {
                postException.setResponseBody(errorMessage);
            }
            throw postException;
        }
        byte[] inBytes = new byte[1000];
        postIn.read(inBytes);
        postIn.close();
        int responseCode = postConnection.getResponseCode();
        String responseMsg = postConnection.getResponseMessage();
        String remoteMD5Sum = postConnection.getHeaderField("Content-md5");
        postConnection.disconnect();
        logger.info((new Date()).toString() + ": Bundle post connection closed");
        if ((responseCode / 100) != 2) {
            throw new BundlePostException(responseCode, postUrl + ": " + responseMsg);
        }
        if (!localMD5Sum.equals(remoteMD5Sum)) {
            String msg = "Bundle MD5 mismatch!\n" + "Local: " + localMD5Sum + "\nRemote: " + remoteMD5Sum;
            logger.severe(msg);
            throw new BundlePostException(responseCode, postUrl + ": " + msg);
        }
    }

    public String getPostUrl() {
        return postUrl;
    }

    public void setPostUrl(String postUrl) {
        this.postUrl = postUrl;
    }

    public boolean isB64Gzip() {
        return b64gzip;
    }

    public void setB64Gzip(boolean gzip) {
        this.b64gzip = gzip;
    }

    public static String getErrorMessage(HttpURLConnection connection) {
        InputStream errorStream = connection.getErrorStream();
        try {
            if (errorStream != null) {
                InputStreamReader reader = new InputStreamReader(errorStream);
                StringBuffer errorBodyBuf = new StringBuffer();
                char[] chars = new char[100];
                while (reader.ready()) {
                    int len = reader.read(chars);
                    errorBodyBuf.append(chars, 0, len);
                }
                return errorBodyBuf.toString();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
