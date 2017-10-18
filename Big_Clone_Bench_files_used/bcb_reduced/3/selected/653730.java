package cvut.fel.mobilevoting.murinrad.communications;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import javax.security.cert.CertificateEncodingException;
import org.apache.http.HttpHost;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import cvut.fel.mobilevoting.murinrad.R;
import cvut.fel.mobilevoting.murinrad.datacontainers.QuestionData;
import cvut.fel.mobilevoting.murinrad.datacontainers.ServerData;
import cvut.fel.mobilevoting.murinrad.views.QuestionsView;

/**
 * Class that manages the actual connection to the server and sends the requests
 * 
 * @author Radovan Murin
 * 
 */
public class Connection extends Thread implements Runnable, ConnectionInterface {

    DefaultHttpClient connection;

    Handler conThread;

    Context context;

    boolean connected = false;

    boolean exc = false;

    int port = 0;

    QuestionsView parent;

    ServerData server;

    SchemeRegistry schemeRegistry = null;

    String usingScheme = "http";

    Connection instance;

    boolean run = true;

    @Override
    public void run() {
        while (true) {
            if (run) InitializeUnsecure();
        }
    }

    public Connection(ServerData server, QuestionsView parent) {
        this.parent = parent;
        this.server = server;
        this.instance = this;
        start();
    }

    /**
	 * Initializes the HTTP connection
	 */
    public void InitializeUnsecure() {
        try {
            run = false;
            port = server.getPort();
            HttpParams params = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(params, 3000);
            HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
            HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);
            schemeRegistry = new SchemeRegistry();
            Scheme http = new Scheme("http", new PlainSocketFactory(), port);
            schemeRegistry.register(http);
            HttpPost post = new HttpPost(server.getAddress());
            SingleClientConnManager cm = new SingleClientConnManager(post.getParams(), schemeRegistry);
            connection = new DefaultHttpClient(cm, params);
            this.connection.addResponseInterceptor(new Interceptor(this));
            notifyOfProggress();
            postAndRecieve("OPTIONS", "/", null, null, true);
            instance = this;
        } catch (IllegalStateException ex) {
        } catch (Exception ex) {
            Log.e("Android mobile voting", "INIT HTTP error " + ex.toString());
            showNoConError();
        }
    }

    /**
	 * Forces the initialisation of the connection helper method as
	 * Thread.stop() is deprecated
	 */
    public void forceInit() {
        run = true;
    }

    /**
	 * Initializes the HTTPs connection
	 * 
	 * @param sslPort
	 *            the number of the port the server should be listening for
	 *            SSL/TLS connections
	 */
    public void InitializeSecure(int sslPort) {
        if (sslPort != -1) {
            SSLSocketFactory sslf = null;
            SSLSocket s = null;
            port = sslPort;
            try {
                KeyStore trusted = KeyStore.getInstance(KeyStore.getDefaultType());
                trusted.load(null, null);
                sslf = new MySSLSocketFactory(trusted);
                Log.w("Android mobile voting", "1");
                sslf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
                Log.w("Android mobile voting", "2");
                BasicHttpParams params = new BasicHttpParams();
                Log.w("Android mobile voting", "3");
                HttpConnectionParams.setConnectionTimeout(params, 500);
                Log.w("Android mobile voting", "4");
                s = (SSLSocket) sslf.connectSocket(sslf.createSocket(), server.getAddress(), sslPort, null, 0, params);
                if (exc) {
                    SSLSession ssls = null;
                    ssls = s.getSession();
                    final javax.security.cert.X509Certificate[] x = ssls.getPeerCertificateChain();
                    for (int i = 0; i < x.length; i++) {
                        parent.mHandler.post(new Runnable() {

                            @Override
                            public void run() {
                                try {
                                    parent.askForTrust(getThumbPrint(x[0]), instance);
                                } catch (NoSuchAlgorithmException e) {
                                    e.printStackTrace();
                                } catch (CertificateEncodingException e) {
                                    e.printStackTrace();
                                } catch (final Exception ex) {
                                    parent.mHandler.post(new Runnable() {

                                        @Override
                                        public void run() {
                                            parent.showToast(ex.toString());
                                        }
                                    });
                                    Log.w("Android Mobile Voting", "400 Error");
                                    parent.finish();
                                }
                            }
                        });
                    }
                }
                s.startHandshake();
                Scheme https = new Scheme("https", sslf, sslPort);
                schemeRegistry.register(https);
                usingScheme = "https";
                port = sslPort;
                if (!exc) retrieveQuestions();
            } catch (final Exception ex) {
                parent.mHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        parent.showToast(ex.toString());
                    }
                });
                parent.finish();
            }
        } else {
            parent.mHandler.post(new Runnable() {

                @Override
                public void run() {
                    parent.showNoSSLDialog(instance);
                }
            });
        }
    }

    public boolean parseResponceCode(String data) {
        Log.d("Android mobile voting", data);
        if (data.contains("400")) {
            parent.mHandler.post(new Runnable() {

                @Override
                public void run() {
                    parent.showToast(parent.getString(R.string.ErrorMsg400));
                }
            });
            Log.w("Android Mobile Voting", "400 Error");
            parent.finish();
            return false;
        }
        if (data.contains("401")) {
            parent.mHandler.post(new Runnable() {

                @Override
                public void run() {
                    parent.showToast(parent.getString(R.string.ErrorMsg401));
                }
            });
            Log.w("Android Mobile Voting", "401 Error");
            parent.finish();
            return false;
        }
        if (data.contains("403")) {
            parent.mHandler.post(new Runnable() {

                @Override
                public void run() {
                    parent.showToast(parent.getString(R.string.ErrorMsg403));
                }
            });
            Log.w("Android Mobile Voting", "403 Error");
            parent.finish();
            return false;
        }
        if (data.contains("404")) {
            parent.mHandler.post(new Runnable() {

                @Override
                public void run() {
                    parent.showToast(parent.getString(R.string.ErrorMsg404));
                }
            });
            Log.w("Android Mobile Voting", "404 Error");
            parent.finish();
            return false;
        }
        if (data.contains("419")) {
            parent.mHandler.post(new Runnable() {

                @Override
                public void run() {
                    parent.showToast(parent.getString(R.string.ErrorMsg419));
                }
            });
            Log.w("Android Mobile Voting", "419 Error");
            parent.finish();
            return false;
        }
        if (data.contains("200")) {
            Log.w("Android Mobile Voting", "200 OK");
            return true;
        }
        return false;
    }

    @Override
    public void closeConnection() {
        this.stop();
        parent.finish();
    }

    public void postAnswers(ArrayList<QuestionData> answers) {
        Log.i("Android Mobile Voting", "in posting method");
        String xml = XMLMaker.XMLMaker.buildAnswer(answers);
        try {
            postAndRecieve("POST", "/", null, xml, true);
            postAndRecieve("GET", "/", null, null, true);
        } catch (IOException ex) {
            parent.finish();
        }
    }

    public void postAndRecieve(String method, String URL, ArrayList<BasicHeader> headers, String body, boolean authenticate) throws IOException {
        BasicHttpEntityEnclosingRequest request = new BasicHttpEntityEnclosingRequest(method, URL);
        BasicHeader head = null;
        BasicHeader h2 = null;
        if (authenticate) {
            head = new BasicHeader("ID", server.getLogin());
            h2 = new BasicHeader("Password", server.getPassword());
        }
        BasicHttpEntity entity = new BasicHttpEntity();
        if (body != null) {
            InputStream in = null;
            Log.i("Android Mobile Voting", "loading entity");
            try {
                in = new ByteArrayInputStream(body.getBytes("UTF-8"));
            } catch (UnsupportedEncodingException e1) {
                e1.printStackTrace();
            }
            entity.setContent(in);
            request.setEntity(entity);
        }
        request.addHeader(head);
        request.addHeader(h2);
        if (headers != null) {
        }
        HttpHost g = new HttpHost(server.getAddress(), port, usingScheme);
        connection.execute(g, request);
        Log.e("Android Mobile Voting", "sending");
    }

    @Override
    public QuestionsView getParent() {
        return parent;
    }

    /**
	 * 
	 * 
	 * http://stackoverflow.com/questions/1270703/how-to-retrieve-compute-an-
	 * x509-certificates-thumbprint-in-java
	 * 
	 * 
	 * 
	 * @param cert
	 * @return a string of the certificate thumb print
	 * @throws NoSuchAlgorithmException
	 * @throws CertificateEncodingException
	 */
    public static String getThumbPrint(javax.security.cert.X509Certificate cert) throws NoSuchAlgorithmException, CertificateEncodingException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] der = null;
        der = cert.getEncoded();
        md.update(der);
        byte[] digest = md.digest();
        return hexify(digest);
    }

    /**
	 * 
	 * Gets the hex code from a byte array
	 * http://stackoverflow.com/questions/1270703/how-to-retrieve-compute-an-
	 * x509-certificates-thumbprint-in-java
	 * 
	 * @param bytes
	 * @return a string of hex data
	 */
    public static String hexify(byte bytes[]) {
        char[] hexDigits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
        StringBuffer buf = new StringBuffer(bytes.length * 2);
        for (int i = 0; i < bytes.length; ++i) {
            buf.append(hexDigits[(bytes[i] & 0xf0) >> 4]);
            buf.append(hexDigits[bytes[i] & 0x0f]);
        }
        return buf.toString();
    }

    /**
	 * Custom SSLSocket factory that enables ignoring bad certificate if the
	 * user wishses to do so
	 * 
	 * @author Murko
	 * 
	 */
    public class MySSLSocketFactory extends SSLSocketFactory {

        SSLContext sslContext = SSLContext.getInstance("TLS");

        public MySSLSocketFactory(KeyStore truststore) throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, UnrecoverableKeyException {
            super(truststore);
            TrustManager tm = new X509TrustManager() {

                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    Log.w("Android mobile voting", "CERTIFICATE ERROR1");
                }

                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    try {
                        getDefaultTrust().checkServerTrusted(chain, authType);
                    } catch (CertificateException ex) {
                        Log.w("Android Mobile Voting", "Custom X509TrustException");
                        exc = true;
                    }
                }

                public X509Certificate[] getAcceptedIssuers() {
                    Log.w("Android mobile voting", "CERTIFICATE ERROR1");
                    return null;
                }
            };
            sslContext.init(null, new TrustManager[] { tm }, null);
        }

        @Override
        public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException, UnknownHostException {
            return sslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
        }

        @Override
        public Socket createSocket() throws IOException {
            return sslContext.getSocketFactory().createSocket();
        }
    }

    /**
	 * retrieves the questions and sends them to the view Starts the connection
	 */
    public void retrieveQuestions() {
        Thread t = new Thread() {

            @Override
            public void run() {
                Looper.prepare();
                try {
                    postAndRecieve("GET", "/", null, null, true);
                } catch (Exception ex) {
                }
                Looper.loop();
            }
        };
        t.start();
    }

    /**
	 * Sends a message to the GUI thread to display a connection error
	 */
    public void showNoConError() {
        Thread t = new Thread() {

            @Override
            public void run() {
                Looper.prepare();
                try {
                    parent.showConnectionError();
                } catch (Exception ex) {
                }
                Looper.loop();
            }
        };
        t.start();
    }

    /**
	 * Alters the state of the conenction progres dialog - on/off
	 */
    private void notifyOfProggress() {
        parent.mHandler.post(new Runnable() {

            @Override
            public void run() {
                parent.showNextProgres();
            }
        });
    }

    /**
	 * http://www.coderanch.com/t/207318/sockets/java/do-hold-Java-default-SSL a
	 * getter method for outputting the defauld certificate validator
	 * 
	 * @return
	 */
    private X509TrustManager getDefaultTrust() {
        TrustManagerFactory trustManagerFactory = null;
        try {
            trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        } catch (NoSuchAlgorithmException e1) {
            e1.printStackTrace();
        }
        try {
            trustManagerFactory.init((KeyStore) null);
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }
        System.out.println("JVM Default Trust Managers:");
        for (TrustManager trustManager : trustManagerFactory.getTrustManagers()) {
            System.out.println(trustManager);
            if (trustManager instanceof X509TrustManager) {
                X509TrustManager x509TrustManager = (X509TrustManager) trustManager;
                return x509TrustManager;
            }
        }
        return null;
    }
}
