package java.br.com.jnfe.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
	 * Adaptei esta classe a partir de dicas do GUJ. Acidentalmente apaguei os cr�ditos,
	 * se algu�m puder indic�-los, agrade�o.
	 * 
	 * Sua finalidade � verificar se a conex�o com o servidor pode funcionar de forma 
	 * independente. 
	 * 
	 * Ela requer uma keystore no formato pkcs12, de onde s�o extra�das as chaves para
	 * estabelecer uma conex�o SSL.
	 * 
	 * Os certificados apresentados pelo servidor n�o sobreescrevem aqueles que j� est�o na
	 * jvm em "cacerts", ao inv�s disto � criado um novo armaz�m de acordo com campos est�ticos
	 * desta classe.
	 * 
	 * @author originalmente?
	 * @author Mauricio Fernandes de Castro
	 *
	 */
public class InstallCert {

    private static String CONNECTION_KEYSTORE_LOCATION = "/home/mauricio/Desktop/iserv.pfx";

    private static String EXTRACTED_KEYSTORE_LOCATION = "/home/mauricio/Desktop/cacerts";

    /**
		 * Bootstrap the code...
		 * 
		 * @param args
		 * @throws Exception
		 */
    public static void main(String[] args) throws Exception {
        String host;
        int port;
        char[] passphrase1;
        char[] passphrase2;
        if ((args.length > 0)) {
            String[] c = args[0].split(":");
            host = c[0];
            port = (c.length == 1) ? 443 : Integer.parseInt(c[1]);
            String p1 = (args.length == 1) ? "changeit" : args[1];
            passphrase1 = p1.toCharArray();
            String p2 = (args.length == 2) ? "" : args[2];
            passphrase2 = p2.toCharArray();
        } else {
            System.out.println("Uso: java InstallCert <host>[:port] [senha1] [senha2]");
            System.out.println("Onde senha1 � a senha do armazem seguro, que por default � 'changeit' (sem os 's)");
            System.out.println("e senha2 � a senha do armazem com a sua chave particular e certificado.");
            return;
        }
        KeyStore ts = openTrustStore(passphrase1);
        X509TrustManager tm = openTrustManager(ts, CONNECTION_KEYSTORE_LOCATION, passphrase2);
        SSLSocket socket = customSocketFactory((TrustManagerDecorator) tm, host, port);
        socket.setSoTimeout(10000);
        try {
            System.out.println("Starting SSL handshake...");
            socket.startHandshake();
            socket.close();
            System.out.println();
            System.out.println("No errors, certificate is already trusted");
        } catch (SSLException e) {
            System.out.println();
            e.printStackTrace(System.out);
        }
        X509Certificate[] chain = ((TrustManagerDecorator) tm).chain;
        if (chain == null) {
            System.out.println("Could not obtain server certificate chain");
            return;
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.println();
        System.out.println("Server sent " + chain.length + " certificate(s):");
        System.out.println();
        MessageDigest sha1 = MessageDigest.getInstance("SHA1");
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        for (int i = 0; i < chain.length; i++) {
            X509Certificate cert = chain[i];
            System.out.println(" " + (i + 1) + " Subject " + cert.getSubjectDN());
            System.out.println("   Issuer  " + cert.getIssuerDN());
            sha1.update(cert.getEncoded());
            System.out.println("   sha1    " + toHexString(sha1.digest()));
            md5.update(cert.getEncoded());
            System.out.println("   md5     " + toHexString(md5.digest()));
            System.out.println();
        }
        System.out.println("Enter certificate to add to trusted keystore or 'q' to quit: [1]");
        String line = reader.readLine().trim();
        int k;
        try {
            k = (line.length() == 0) ? 0 : Integer.parseInt(line) - 1;
        } catch (NumberFormatException e) {
            System.out.println("KeyStore not changed");
            return;
        }
        X509Certificate cert = chain[k];
        String alias = host + "-" + (k + 1);
        ts.setCertificateEntry(alias, cert);
        OutputStream out = new FileOutputStream(EXTRACTED_KEYSTORE_LOCATION);
        ts.store(out, passphrase1);
        out.close();
        System.out.println();
        System.out.println(cert);
        System.out.println();
        System.out.println("Added certificate to keystore '" + EXTRACTED_KEYSTORE_LOCATION + "' using alias '" + alias + "'");
    }

    /**
	     * Abre o armaz�m de chaves confi�veis.
	     * 
	     * @param passphrase
	     * 
	     * @throws Exception
	     */
    public static KeyStore openTrustStore(char[] passphrase) throws Exception {
        File file = new File(EXTRACTED_KEYSTORE_LOCATION);
        if (file.isFile() == false) {
            char SEP = File.separatorChar;
            File dir = new File(System.getProperty("java.home") + SEP + "lib" + SEP + "security");
            file = new File(dir, EXTRACTED_KEYSTORE_LOCATION);
            if (file.isFile() == false) {
                file = new File(dir, "cacerts");
            }
        }
        System.out.println("Carregando armaz�m " + file + "...");
        InputStream in = new FileInputStream(file);
        KeyStore ts = KeyStore.getInstance(KeyStore.getDefaultType());
        ts.load(in, passphrase);
        in.close();
        return ts;
    }

    /**
	     * Gerenciador de chaves do usu�rio.
	     * 
	     * @param ts
	     * @param keyStoreLocation
	     * @param passphrase
	     * 
	     * @throws Exception
	     */
    public static X509TrustManager openTrustManager(KeyStore ts, String keyStoreLocation, char[] passphrase) throws Exception {
        KeyStore ksKeys = KeyStore.getInstance("pkcs12");
        ksKeys.load(new FileInputStream(keyStoreLocation), passphrase);
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ksKeys, passphrase);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ts);
        X509TrustManager tm = new TrustManagerDecorator((X509TrustManager) tmf.getTrustManagers()[0], kmf);
        return tm;
    }

    /**
	     * Uma f�brica de conex�es SSL.
	     * 
	     * @param tm
	     * @param host
	     * @param port
	     * 
	     * @throws Exception
	     */
    public static SSLSocket customSocketFactory(TrustManagerDecorator tm, String host, int port) throws Exception {
        SSLContext context = tm.createSSLContext();
        SSLSocketFactory factory = context.getSocketFactory();
        System.out.println("Opening connection to " + host + ":" + port + "...");
        SSLSocket socket = (SSLSocket) factory.createSocket(host, port);
        return socket;
    }

    private static final char[] HEXDIGITS = "0123456789abcdef".toCharArray();

    /**
	     * Queremos somente caracteres que o usu�rio possa ler...
	     * 
	     * @param bytes
	     */
    private static String toHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 3);
        for (int b : bytes) {
            b &= 0xff;
            sb.append(HEXDIGITS[b >> 4]);
            sb.append(HEXDIGITS[b & 15]);
            sb.append(' ');
        }
        return sb.toString();
    }

    /**
	     * Decorador para preservar a cadeia de confian�a durante a verifica��o e
	     * auxiliar na cria��o de contextos SSL.
	     */
    private static class TrustManagerDecorator implements X509TrustManager {

        private final X509TrustManager tm;

        private X509Certificate[] chain;

        private KeyManagerFactory kmf;

        TrustManagerDecorator(X509TrustManager tm, KeyManagerFactory kmf) {
            this.tm = tm;
            this.kmf = kmf;
        }

        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            this.chain = chain;
            System.out.println("Saving chain of " + chain.length + " certificates.");
            tm.checkServerTrusted(chain, authType);
        }

        public SSLContext createSSLContext() throws Exception {
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(kmf.getKeyManagers(), new TrustManager[] { this }, null);
            return context;
        }
    }
}
