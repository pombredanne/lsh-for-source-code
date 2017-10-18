package otm.httpslinger;

import java.net.InetAddress;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.io.OutputStreamWriter;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.util.StringTokenizer;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import jargs.gnu.CmdLineParser;
import java.security.*;
import javax.net.ssl.*;

public class HttpSlinger {

    private String url;

    private int port;

    private int sslport;

    private String servlet;

    private String username;

    private String password;

    private String inputFile;

    private boolean stripHttp;

    private String transport;

    private String authMethod;

    private String realm;

    private boolean basic;

    private boolean digest;

    private String qop;

    private String nonce;

    private String opaque;

    private String algorithm;

    private String cnonce;

    private String nonceCount;

    private String method;

    private String uri;

    private static final int TIMEOUT_DURATION = 15000;

    private String response;

    public static boolean consoleout = true;

    public HttpSlinger(String url, int port, String servlet, String username, String password, String transport, String authMethod, String inputFile) {
        this.url = url;
        this.port = port;
        this.servlet = servlet;
        this.username = username;
        this.password = password;
        this.inputFile = inputFile;
        this.stripHttp = false;
        this.transport = transport;
        this.authMethod = authMethod;
        this.basic = false;
        this.digest = false;
        this.cnonce = new String("0a4f113b");
        this.nonceCount = new String("00000001");
        this.method = new String("POST");
        this.uri = new String("/" + servlet);
    }

    private static final char[] digit = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    private byte[] bytesToHex(byte[] bytes) {
        int len = bytes.length, val, hIndex, lIndex, i;
        byte[] hex = new byte[len * 2];
        for (i = 0; i < len; i++) {
            val = (bytes[i] + 256) % 256;
            hIndex = val >> 4;
            lIndex = val & 0xf;
            hex[i * 2 + 0] = (byte) digit[hIndex];
            hex[i * 2 + 1] = (byte) digit[lIndex];
        }
        return hex;
    }

    public String digestResponse() {
        String digest = null;
        if (null == nonce) return null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(username.getBytes());
            md.update(":".getBytes());
            md.update(realm.getBytes());
            md.update(":".getBytes());
            md.update(password.getBytes());
            byte[] d = md.digest();
            if (null != algorithm && -1 != (algorithm.toLowerCase()).indexOf("md5-sess")) {
                md = MessageDigest.getInstance("MD5");
                md.update(d);
                md.update(":".getBytes());
                md.update(nonce.getBytes());
                md.update(":".getBytes());
                md.update(cnonce.getBytes());
                d = md.digest();
            }
            byte[] a1 = bytesToHex(d);
            md = MessageDigest.getInstance("MD5");
            md.update(method.getBytes());
            md.update(":".getBytes());
            md.update(uri.getBytes());
            d = md.digest();
            byte[] a2 = bytesToHex(d);
            md = MessageDigest.getInstance("MD5");
            md.update(a1);
            md.update(":".getBytes());
            md.update(nonce.getBytes());
            md.update(":".getBytes());
            if (null != qop) {
                md.update(nonceCount.getBytes());
                md.update(":".getBytes());
                md.update(cnonce.getBytes());
                md.update(":".getBytes());
                md.update(qop.getBytes());
                md.update(":".getBytes());
            }
            md.update(a2);
            d = md.digest();
            byte[] r = bytesToHex(d);
            digest = new String(r);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return digest;
    }

    public void stripHttp(boolean stripHttp) {
        this.stripHttp = stripHttp;
    }

    private int getRealm(String data) {
        boolean https = "https".equals(transport);
        Socket socket = null;
        try {
            InetAddress addr = InetAddress.getByName(url);
            if (https) {
                Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
                SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                SSLSocket sslSocket = (SSLSocket) factory.createSocket(addr, port);
                sslSocket.getSession();
                socket = (Socket) sslSocket;
            } else {
                socket = new Socket();
                socket.connect(new InetSocketAddress(addr, port), TIMEOUT_DURATION);
            }
            OutputStream wr = socket.getOutputStream();
            wr.write(("POST /" + servlet + " HTTP/1.1\r\n").getBytes("UTF-8"));
            wr.write(("Connection: close\r\n").getBytes("UTF-8"));
            wr.write(("Host: " + url + ":" + port + "\r\n").getBytes("UTF-8"));
            wr.write(("Accept: */*\r\n").getBytes("UTF-8"));
            wr.write(("Content-Length: " + data.getBytes("UTF-8").length + "\r\n").getBytes("UTF-8"));
            wr.write(("Content-Type: application/soap+xml;charset=UTF-8\r\n").getBytes("UTF-8"));
            wr.write(("\r\n").getBytes("UTF-8"));
            wr.write(data.getBytes("UTF-8"));
            wr.flush();
            BufferedReader rd = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String line;
            while ((line = rd.readLine()) != null) {
                StringTokenizer st = new StringTokenizer(line, ",");
                while (st.hasMoreTokens()) {
                    String token = st.nextToken();
                    if (-1 != (token.toLowerCase()).indexOf("www-authenticate: digest")) {
                        digest = true;
                    } else if (-1 != (token.toLowerCase()).indexOf("www-authenticate: basic")) {
                        basic = true;
                    }
                    if (-1 != (token.toLowerCase()).indexOf("realm")) {
                        realm = token.substring(token.indexOf("=") + 2, token.length() - (token.endsWith(",") ? 2 : 1));
                    } else if (-1 != (token.toLowerCase()).indexOf("nonce")) {
                        nonce = token.substring(token.indexOf("=") + 2, token.length() - (token.endsWith(",") ? 2 : 1));
                    } else if (-1 != (token.toLowerCase()).indexOf("qop")) {
                        qop = token.substring(token.indexOf("=") + 2, token.length() - (token.endsWith(",") ? 2 : 1));
                        if (!qop.endsWith("auth") && (-1 == qop.indexOf("auth,"))) qop = null; else qop = "auth";
                    } else if (-1 != (token.toLowerCase()).indexOf("opaque")) {
                        opaque = token.substring(token.indexOf("=") + 2, token.length() - (token.endsWith(",") ? 2 : 1));
                    } else if (-1 != (token.toLowerCase()).indexOf("algorithm")) {
                        algorithm = token.substring(token.indexOf("=") + 2, token.length() - (token.endsWith(",") ? 2 : 1));
                    }
                }
            }
            rd.close();
            socket.close();
            return 0;
        } catch (Exception e) {
            e.printStackTrace();
            try {
                if (socket != null) socket.close();
            } catch (IOException ie) {
            }
            return 2;
        }
    }

    public int postData() {
        String data = getInputFileContents();
        if (!authMethod.equals("anon")) {
            if (getRealm(data) != 0) return 2;
        }
        boolean https = "https".equals(transport);
        Socket socket = null;
        try {
            InetAddress addr = InetAddress.getByName(url);
            if (https) {
                Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
                SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                SSLSocket sslSocket = (SSLSocket) factory.createSocket(addr, port);
                sslSocket.getSession();
                socket = (Socket) sslSocket;
            } else {
                socket = new Socket();
                socket.connect(new InetSocketAddress(addr, port), TIMEOUT_DURATION);
            }
            OutputStream wr = socket.getOutputStream();
            wr.write(("POST /" + servlet + " HTTP/1.1\r\n").getBytes("UTF-8"));
            if (authMethod.equals("digest") && (true == digest)) {
                String digest = digestResponse();
                if (null == digest) throw new Exception("BAD digest challenge");
                String nonceMsg = (null != nonce) ? ("nonce=\"" + nonce + "\", ") : "";
                String opaqueMsg = (null != opaque) ? ("opaque=\"" + opaque + "\", ") : "";
                String qopMsg = (null != qop) ? ("qop=\"" + qop + "\", " + "nc=" + nonceCount + ", " + "cnonce=\"" + cnonce + "\", ") : "";
                wr.write(("Authorization: Digest " + "username=\"" + username + "\", " + "realm=\"" + realm + "\", " + nonceMsg + "uri=\"" + uri + "\", " + opaqueMsg + qopMsg + "response=\"" + digest + "\"\r\n").getBytes("UTF-8"));
            } else if (authMethod.equals("basic") && (true == basic)) {
                wr.write(("Authorization: Basic " + Base64Coder.encode(username + ":" + password) + "\r\n").getBytes("UTF-8"));
            } else if (!authMethod.equals("anon")) {
                System.out.println(authMethod + " not supported by the tested target");
                System.exit(2);
            }
            wr.write(("Host: " + url + ":" + port + "\r\n").getBytes("UTF-8"));
            wr.write(("Accept: */*\r\n").getBytes("UTF-8"));
            wr.write(("Content-Type: application/soap+xml;charset=UTF-8\r\n").getBytes("UTF-8"));
            wr.write(("Content-Length: " + data.length() + "\r\n").getBytes("UTF-8"));
            wr.write(("Connection: close\r\n").getBytes("UTF-8"));
            wr.write(("User-Agent: opentestman 0.2.0\r\n").getBytes("UTF-8"));
            wr.write(("\r\n").getBytes("UTF-8"));
            wr.write(data.getBytes("UTF-8"));
            wr.flush();
            BufferedReader rd = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String line, savedLine = "", completeLine = "";
            int chunkSize = 0, actLen = 0;
            boolean chunk = false, xmlcontent = false, end = false;
            while ((line = rd.readLine()) != null) {
                if (line.equals("HTTP/1.1 401 Unauthorized")) {
                    return 2;
                }
                if (!xmlcontent) {
                    if (0 <= (line.toLowerCase()).indexOf("chunked")) chunk = true;
                    if (!line.startsWith("<")) {
                        savedLine = line;
                        if (!stripHttp) System.out.println(line);
                        continue;
                    } else {
                        xmlcontent = true;
                        completeLine += line;
                        if (chunk) {
                            actLen += line.length();
                            try {
                                chunkSize = Integer.parseInt(savedLine, 16);
                            } catch (NumberFormatException e) {
                                chunkSize = -1;
                                end = true;
                            }
                        }
                    }
                } else {
                    if (chunk) {
                        if (chunkSize == actLen) {
                            try {
                                chunkSize = Integer.parseInt(line, 16);
                                if (0 == chunkSize) end = true;
                            } catch (NumberFormatException e) {
                                chunkSize = -1;
                                end = true;
                            }
                            actLen = 0;
                        } else if (!end) {
                            completeLine += line;
                            actLen += line.length();
                        }
                    } else completeLine += line;
                }
            }
            response = completeLine;
            if (consoleout) {
                System.out.println(completeLine);
            }
            rd.close();
            socket.close();
            return 0;
        } catch (Exception e) {
            e.printStackTrace();
            try {
                if (socket != null) socket.close();
            } catch (IOException ie) {
            }
            return 2;
        }
    }

    public String getResponse() {
        return response;
    }

    private String getInputFileContents() {
        StringBuffer contents = new StringBuffer();
        BufferedReader input = null;
        try {
            input = new BufferedReader(new FileReader(inputFile));
            String line = null;
            while ((line = input.readLine()) != null) {
                contents.append(line);
                contents.append(System.getProperty("line.separator"));
            }
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
            System.exit(2);
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(2);
        } finally {
            try {
                if (input != null) {
                    input.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                System.exit(2);
            }
        }
        return contents.toString();
    }

    public static void main(String[] args) {
        CmdLineParser parser = new CmdLineParser();
        CmdLineParser.Option urlOption = parser.addStringOption("url");
        CmdLineParser.Option portOption = parser.addIntegerOption("port");
        CmdLineParser.Option servletOption = parser.addStringOption("servlet");
        CmdLineParser.Option usernameOption = parser.addStringOption("username");
        CmdLineParser.Option passwordOption = parser.addStringOption("password");
        CmdLineParser.Option filenameOption = parser.addStringOption("file");
        CmdLineParser.Option stripHttpOption = parser.addBooleanOption("striphttp");
        CmdLineParser.Option transportOption = parser.addStringOption("transport");
        CmdLineParser.Option authOption = parser.addStringOption("auth");
        try {
            parser.parse(args);
        } catch (CmdLineParser.OptionException e) {
            System.err.println(e.getMessage());
            printUsage();
            System.exit(2);
        }
        Integer port = null;
        String url = null;
        String servlet = null;
        String username = null;
        String password = null;
        String filename = null;
        Boolean stripHttpValue = null;
        String transport = null;
        String auth = null;
        port = (Integer) parser.getOptionValue(portOption);
        url = (String) parser.getOptionValue(urlOption);
        servlet = (String) parser.getOptionValue(servletOption);
        username = (String) parser.getOptionValue(usernameOption);
        password = (String) parser.getOptionValue(passwordOption);
        filename = (String) parser.getOptionValue(filenameOption);
        transport = (String) parser.getOptionValue(transportOption);
        auth = (String) parser.getOptionValue(authOption);
        stripHttpValue = (Boolean) parser.getOptionValue(stripHttpOption, Boolean.FALSE);
        if (null == url || null == servlet || null == port || null == username || null == password || null == auth || null == transport || null == filename) {
            System.out.println("Please provide all required command-line arguments");
            printUsage();
            System.exit(2);
        }
        HttpSlinger request = new HttpSlinger(url, port.intValue(), servlet, username, password, transport, auth, filename);
        request.stripHttp(stripHttpValue.booleanValue());
        int retry = 5, rc;
        while (((rc = request.postData()) != 0) && (--retry != 0)) ;
        System.exit(rc);
    }

    private static void printUsage() {
        System.out.println("Usage: HttpSlinger <{--url} aUrl> <{--port} aPort> <{--servlet} aServlet>");
        System.out.println("                   <{--username} aUsername> <{--password} aPassword> <{--file} inputfile>");
        System.out.println("                   <{--transport <http/https>}>");
        System.out.println("                   <{--auth <basic/digest/anon>}>");
        System.out.println("                   <{--striphttp}>");
    }
}
