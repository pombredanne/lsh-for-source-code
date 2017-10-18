package SEAlib;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.regex.*;
import java.security.*;

public class SEANetManager implements Runnable {

    private HttpURLConnection http;

    private String url, hashAlgorithmName;

    private Thread thread;

    private int runState = 0;

    private Queue requests;

    private List dirtyData;

    private Object lock = new Object();

    private boolean serverUptodate = true, useCookies, digestSessionValue;

    private static final int HTTP_TIMEOUT = 2000;

    public SEANetManager() {
        if (SEAUser.getUser() == null) throw new RuntimeException("No user session initialized");
        this.url = SEAUser.getUser().getURL();
        thread = new Thread(this);
        thread.setDaemon(true);
        requests = new LinkedList();
        dirtyData = new LinkedList();
        setAuthentication(false, true, "MD5");
    }

    private class Request {

        public static final int WRITE_DATA = 0, READ_DATA = 1, INVOKE_SERVICE = 2;

        private int type;

        private String[] params;

        private String name, output;

        private boolean isProcessed, isFinalized;

        private final Object lock = new Object();

        public Request(int type, String name, String[] params) {
            this.type = type;
            this.name = name;
            this.params = params;
            this.output = "An error occured in processing request";
            isProcessed = isFinalized = false;
        }

        public Request(int type) {
            this(type, null, null);
        }

        public Request(int type, String[] params) {
            this(type, "", params);
        }

        public int getType() {
            return type;
        }

        public String getParameter(int i) {
            return (params == null || (i >= params.length)) ? null : params[i];
        }

        public String getName() {
            return name;
        }

        public int getNumParameters() {
            return (params == null) ? 0 : params.length;
        }

        public void setProcessed(boolean value) {
            isProcessed = value;
        }

        public String getOutput() {
            return output;
        }

        public void setOutput(String output) {
            this.output = output;
        }

        public void finalize() {
            isFinalized = true;
            synchronized (lock) {
                lock.notify();
            }
        }

        public void waitOnCompletion() throws InterruptedException {
            synchronized (lock) {
                while (!isFinalized) lock.wait();
            }
        }

        public boolean isProcessed() {
            return isProcessed;
        }
    }

    public void invalidate(SEAUser.Preference node) {
        synchronized (lock) {
            dirtyData.add(node);
            serverUptodate = false;
        }
    }

    private String getReturnString(String xml) {
        Pattern p = Pattern.compile(".*<\\s*return.*>(.*)<\\s*/return\\s*>.*");
        Matcher m = p.matcher(xml);
        return (m.matches()) ? m.group(1) : null;
    }

    public String invokeWebService(String name, String[] params) throws SEAUser.ConnectionException {
        Request request = new Request(Request.INVOKE_SERVICE, name, params);
        addRequest(request);
        try {
            request.waitOnCompletion();
        } catch (InterruptedException e) {
            throw new SEAUser.ConnectionException(e.getMessage());
        }
        if (!request.isProcessed()) throw new SEAUser.ConnectionException(request.getOutput());
        return request.getOutput();
    }

    public void setXMLReadMode(boolean value) {
    }

    public void setXMLWriteMode(boolean value) {
    }

    public void setAuthentication(boolean useCookies, boolean digestSessionValue, String algorithm) {
        this.useCookies = useCookies;
        this.digestSessionValue = digestSessionValue;
        this.hashAlgorithmName = algorithm.toUpperCase();
    }

    private String computeHash(String str) {
        StringBuffer hexBuffer = new StringBuffer();
        byte[] bytes;
        int i;
        try {
            MessageDigest hashAlgorithm = MessageDigest.getInstance(hashAlgorithmName);
            hashAlgorithm.reset();
            hashAlgorithm.update(str.getBytes());
            bytes = hashAlgorithm.digest();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
        for (i = 0; i < bytes.length; i++) hexBuffer.append(((bytes[i] >= 0 && bytes[i] <= 15) ? "0" : "") + Integer.toHexString(bytes[i] & 0xFF));
        return hexBuffer.toString();
    }

    public void readData(String[] params) throws SEAUser.ConnectionException {
        Request request;
        addRequest(request = new Request(Request.READ_DATA, params));
        try {
            request.waitOnCompletion();
        } catch (InterruptedException e) {
            throw new SEAUser.ConnectionException(e.getMessage());
        }
        if (!request.isProcessed()) throw new SEAUser.ConnectionException(request.getOutput());
    }

    private void addRequest(Request r) {
        requests.add(r);
    }

    public synchronized void run() {
        OutputStreamWriter out;
        InputStream inputStream;
        BufferedReader reader;
        Request request = null;
        String str, tmp, sessionValue;
        int i;
        if (!thread.isAlive()) {
            runState = 1;
            thread.start();
            return;
        }
        if (runState > 1) return;
        runState++;
        sessionValue = SEAUser.getUser().getSessionValue();
        if (digestSessionValue) sessionValue = ((tmp = computeHash(sessionValue)) == null) ? "none" : tmp;
        while (true) {
            while ((request = (Request) requests.poll()) != null) {
                try {
                    http = (HttpURLConnection) new URL(url).openConnection();
                    http.setDoOutput(true);
                    http.setDoInput(true);
                    http.setRequestMethod("POST");
                    http.setRequestProperty("Content-Type", "text/xml; charset=ISO-8859-1");
                    if (useCookies) http.setRequestProperty("Cookie", SEAUser.getUser().getSessionName() + "=" + sessionValue);
                    http.setConnectTimeout(HTTP_TIMEOUT);
                    switch(request.getType()) {
                        case Request.READ_DATA:
                            http.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                            http.connect();
                            out = new OutputStreamWriter(http.getOutputStream());
                            if (!useCookies) out.write("session_value=" + sessionValue);
                            for (i = 0; i < request.getNumParameters(); i += 2) out.write('&' + request.getParameter(i) + '=' + request.getParameter(i + 1));
                            out.flush();
                            out.close();
                            inputStream = http.getInputStream();
                            SEAUser.getUser().parseData(inputStream);
                            inputStream.close();
                            break;
                        case Request.WRITE_DATA:
                            http.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                            http.connect();
                            out = new OutputStreamWriter(http.getOutputStream());
                            if (!useCookies) out.write("session_value=" + sessionValue);
                            for (i = 0; i < request.getNumParameters(); i += 2) out.write('&' + request.getParameter(i) + '=' + request.getParameter(i + 1));
                            out.flush();
                            out.close();
                            break;
                        case Request.INVOKE_SERVICE:
                            http.connect();
                            out = new OutputStreamWriter(http.getOutputStream());
                            out.write("<?xml version='1.0' encoding='ISO-8859-1'?><SOAP-ENV:Envelope SOAP-ENV:encodingStyle='http://schemas.xmlsoap.org/soap/encoding/' xmlns:SOAP-ENV='http://schemas.xmlsoap.org/soap/envelope/' xmlns:xsd='http://www.w3.org/2001/XMLSchema' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xmlns:SOAP-ENC='http://schemas.xmlsoap.org/soap/encoding/'>");
                            if ((str = request.getName()) == null) {
                                request.setOutput("No specified web service name");
                                break;
                            }
                            out.write("<SOAP-ENV:Body><" + str + ">");
                            if (!useCookies) out.write("<session_value xsi:type='xsd:string'>" + sessionValue + "</session_value>");
                            for (i = 0; i < request.getNumParameters(); i += 2) out.write("<" + (tmp = request.getParameter(i)) + " xsi:type='xsd:string'>" + request.getParameter(i + 1) + "</" + tmp + ">");
                            out.write("</" + str + "></SOAP-ENV:Body></SOAP-ENV:Envelope>");
                            out.flush();
                            out.close();
                            reader = new BufferedReader(new InputStreamReader(http.getInputStream()));
                            str = "";
                            while ((tmp = reader.readLine()) != null) str += tmp;
                            reader.close();
                            request.setOutput(getReturnString(str));
                            break;
                    }
                    request.setProcessed(true);
                } catch (Exception e) {
                    request.setOutput(e.getMessage());
                } finally {
                    request.finalize();
                    http.disconnect();
                }
            }
            synchronized (lock) {
                if (!serverUptodate) {
                    serverUptodate = true;
                    String[] params = new String[dirtyData.size() << 1];
                    Iterator itr = dirtyData.iterator();
                    for (i = 0; itr.hasNext(); i += 2) {
                        SEAUser.Preference pref = (SEAUser.Preference) itr.next();
                        params[i] = pref.getPath();
                        params[i + 1] = pref.getStringValue();
                    }
                    if (params.length > 1) addRequest(new Request(Request.WRITE_DATA, params));
                }
            }
            try {
                thread.sleep(200);
            } catch (InterruptedException e) {
            }
        }
    }
}
