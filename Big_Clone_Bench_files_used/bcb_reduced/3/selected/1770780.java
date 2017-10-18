package br.com.jnfe.service;

import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.annotation.Resource;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import br.com.jnfe.base.TransportKeyStoreBean;
import br.com.jnfe.base.util.SecurityUtils;
import br.com.jnfe.core.standalone.DefaultJNFeServidorInstaller;

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
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:/META-INF/spring/hibernate-context.xml", "classpath:/META-INF/spring/core-context.xml", "classpath:/META-INF/spring/partner-context.xml", "classpath:/META-INF/spring/document-context.xml", "classpath:/META-INF/spring/message-context.xml", "classpath:/META-INF/spring/jnfe-base-context.xml", "classpath:/META-INF/spring/jnfe-core-context.xml", "classpath:/META-INF/spring/jnfe-core-install-context.xml" })
@Transactional
public class CacertsTester {

    private static int DEFAULT_SERVER_PORT = 443;

    private static String TUSTSTORE_PASSWORD = "changeit";

    /**
	 * Test connection
	 * 
	 * @throws Exception
	 */
    @Test
    public void connect() throws Exception {
        logger.info(transportKeyStore.toString());
        KeyStore trustStore = SecurityUtils.openTrustStore(TUSTSTORE_PASSWORD.toCharArray());
        logger.info("Armaz�m de chaves seguras (cacerts) aberto: {}", trustStore.getProvider());
        X509TrustManager trustManager = openTrustManager(trustStore);
        logger.info("Gerenciador de chaves aberto: {}", trustManager.toString());
        connectSSL(trustManager, instaladorServicosEstaduais.getHostHomologacao());
        X509Certificate[] chain = ((TrustManagerDecorator) trustManager).chain;
        if (chain == null) {
            logger.warn("N�o pode obter cadeia de certifica��o do servidor.");
            return;
        }
        logger.info("Certificados requeridos: ");
        MessageDigest sha1 = MessageDigest.getInstance("SHA1");
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        for (int i = 0; i < chain.length; i++) {
            X509Certificate cert = chain[i];
            logger.info("Subject[{}] {}", i, cert.getSubjectDN());
            logger.info("Issuer[{}]  {}", i, cert.getIssuerDN());
            sha1.update(cert.getEncoded());
            logger.info("sha[{}]     {}", i, toHexString(sha1.digest()));
            md5.update(cert.getEncoded());
            logger.info("md5[{}]     {}", i, toHexString(md5.digest()));
        }
    }

    /**
     * Testa a conex�o SSL com o host.
     * 
     * @param tm
     * @param host
     * @throws Exception
     */
    protected void connectSSL(X509TrustManager tm, String host) throws Exception {
        SSLSocket socket = customSocketFactory((TrustManagerDecorator) tm, host, DEFAULT_SERVER_PORT);
        socket.setSoTimeout(10000);
        logger.info("Iniciando SSL handshake...");
        socket.startHandshake();
        socket.close();
        logger.info("Conex�o estabelecida com sucesso.");
    }

    /**
     * Gerenciador de chaves do usu�rio.
     * 
     * @param ts
     * 
     * @throws Exception
     */
    protected X509TrustManager openTrustManager(KeyStore ts) throws Exception {
        KeyManagerFactory kmf = transportKeyStore.openTransportKeyManagerFactory();
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
    protected SSLSocket customSocketFactory(TrustManagerDecorator tm, String host, int port) throws Exception {
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
            throw new UnsupportedOperationException();
        }

        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            throw new UnsupportedOperationException();
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            for (X509Certificate cert : chain) {
                logger.info("Certificado do servidor: {}.", cert.toString());
            }
            this.chain = chain;
            tm.checkServerTrusted(chain, authType);
        }

        public SSLContext createSSLContext() throws Exception {
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(kmf.getKeyManagers(), new TrustManager[] { tm }, null);
            return context;
        }
    }

    private TransportKeyStoreBean transportKeyStore;

    private DefaultJNFeServidorInstaller instaladorServicosEstaduais;

    @Resource
    public void setTransportKeyStore(TransportKeyStoreBean transportKeyStore) {
        this.transportKeyStore = transportKeyStore;
    }

    @Resource
    public void setInstaladorServicosEstaduais(DefaultJNFeServidorInstaller instaladorServicosEstaduais) {
        this.instaladorServicosEstaduais = instaladorServicosEstaduais;
    }

    private static final Logger logger = LoggerFactory.getLogger(CacertsTester.class);
}
