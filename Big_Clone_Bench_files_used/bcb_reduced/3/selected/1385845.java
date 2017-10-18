package es.caib.sistra.plugins.firma.impl.caib;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import es.caib.loginModule.client.SeyconPrincipal;
import es.caib.signatura.api.Signature;
import es.caib.signatura.api.Signer;
import es.caib.signatura.api.SignerFactory;
import es.caib.util.FirmaUtil;

/**
 * Utilidad para manejo de firma CAIB
 */
public class UtilFirmaCAIB {

    private Signer sigTradise;

    public static String DEFAULT_CONTENT_TYPE = "application/x-caib-authentication";

    public static String USEALWAYSDEFAULTCONTENTTYPE_APPLET_PARAM = "useAlwaysDefaultContentType";

    public boolean useAlwaysDefaultContentType = false;

    public static final String CHARSET = "UTF-8";

    public UtilFirmaCAIB(boolean useAlwaysDefaultContentType) {
        this.useAlwaysDefaultContentType = useAlwaysDefaultContentType;
    }

    public UtilFirmaCAIB() {
        this(false);
    }

    /**
	 * Inicializa entorno firma
	 * @return
	 */
    public void iniciarDispositivo() throws Exception {
        SignerFactory sf = new SignerFactory();
        sigTradise = sf.getSigner();
        if (sigTradise == null) {
            throw new Exception("No se ha podido obtener el firmador");
        }
    }

    /**
	 * Obtiene lista de certificados del dispositivo iniciado
	 * @return
	 */
    public String[] getCertList(String contentType) throws Exception {
        contentType = !useAlwaysDefaultContentType ? contentType : DEFAULT_CONTENT_TYPE;
        System.out.println("Getting cert list for content type: " + contentType);
        String[] certList = sigTradise.getCertList(contentType);
        return certList;
    }

    /**
	 * Realiza la firma de una cadena
	 * @param cadena
	 * @return
	 */
    public String firmarCadena(String cadena, String certificado, String password, String contentType) throws Exception {
        ByteArrayInputStream bis = new ByteArrayInputStream(cadena.getBytes(CHARSET));
        return this.firmaInputStream(bis, certificado, password, contentType);
    }

    /**
	 * Realiza la firma de un fichero
	 * @param cadena
	 * @return
	 */
    public String firmarFichero(String path, String certificado, String password, String contentType) throws Exception {
        FileInputStream fis = new FileInputStream(path);
        return this.firmaInputStream(fis, certificado, password, contentType);
    }

    /**
	 * Realiza la firma de un fichero en b64
	 * @param cadena
	 * @return
	 */
    public String firmarFicheroB64(String b64, String certificado, String password, String contentType) throws Exception {
        ByteArrayInputStream bis = new ByteArrayInputStream(UtilFirmaCAIB.base64ToBytes(b64, true));
        return this.firmaInputStream(bis, certificado, password, contentType);
    }

    /**
	 * Firma InputStream
	 * @param is
	 * @param certificado
	 * @param password
	 * @return
	 * @throws Exception
	 */
    public String firmaInputStream(InputStream is, String certificado, String password, String contentType) throws Exception {
        contentType = !useAlwaysDefaultContentType ? contentType : DEFAULT_CONTENT_TYPE;
        System.out.println("Signing with content type " + contentType);
        Signature signatureData = sigTradise.sign(is, certificado, password, contentType);
        String serializada = serializaFirmaToString(signatureData);
        return serializada;
    }

    /**
	 * Verifica una firma
	 * @param datos
	 * @return
	 */
    public boolean verificarFirma(InputStream datos, Signature firma) throws Exception {
        return verificarFirma(datos, firma, false);
    }

    /**
	 * Verifica una firma
	 * @param cadena
	 * @return
	 */
    public boolean verificarFirma(String cadena, Signature firma) throws Exception {
        return verificarFirma(cadena, firma, false);
    }

    /**
	 * Verifica una firma (en caso de que lleve un timestamp incompleto intenta rellenarlo)
	 * @param datos
	 * @return
	 */
    public boolean verificarFirma(InputStream datos, Signature firma, boolean completarTimestamp) throws Exception {
        if (completarTimestamp) {
            return sigTradise.verifyAPosterioriTimeStamp(datos, firma);
        } else {
            return sigTradise.verify(datos, firma);
        }
    }

    /**
	 * Verifica una firma  (en caso de que lleve un timestamp incompleto intenta rellenarlo)
	 * @param cadena
	 * @return
	 */
    public boolean verificarFirma(String cadena, Signature firma, boolean completarTimestamp) throws Exception {
        InputStream datos = new ByteArrayInputStream(getBytes(cadena));
        return verificarFirma(datos, firma, completarTimestamp);
    }

    /**
	 * Serializa firma
	 * @param signatureData
	 * @return
	 */
    public static String serializaFirmaToString(Signature signatureData) throws Exception {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        ObjectOutputStream oout = new ObjectOutputStream(o);
        oout.writeObject(signatureData);
        oout.close();
        o.close();
        byte data[] = o.toByteArray();
        StringBuffer buffer = new StringBuffer(data.length * 2);
        for (int i = 0; i < data.length; i++) {
            int value = data[i];
            if (value < 0) value += 256;
            buffer.append(toHexChar(value / 16));
            buffer.append(toHexChar(value % 16));
        }
        return buffer.toString();
    }

    private static char toHexChar(int i) {
        if (i < 10) return (char) (48 + i); else return (char) (97 + (i - 10));
    }

    /**
	 * Deserializa firma. 
	 * @param serializada
	 * @return
	 */
    public static Signature deserializaFirmaFromString(String serializada) throws Exception {
        byte b[] = new byte[serializada.length() / 2];
        int i = 0;
        int j = 0;
        while (i < serializada.length()) {
            int c = getHexValue(serializada.charAt(i++)) * 16;
            c += getHexValue(serializada.charAt(i++));
            b[j++] = (byte) c;
        }
        ByteArrayInputStream in = new ByteArrayInputStream(b);
        ObjectInputStream objIn = new ObjectInputStream(in);
        Signature signature;
        try {
            signature = (Signature) objIn.readObject();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        objIn.close();
        in.close();
        return signature;
    }

    private static int getHexValue(char c) {
        if (c >= '0' && c <= '9') return c - 48;
        if (c >= 'A' && c <= 'F') return (c - 65) + 10;
        if (c >= 'a' && c <= 'f') return (c - 97) + 10; else throw new RuntimeException("Invalid hex character " + c);
    }

    public static byte[] serializaFirmaToBytes(Signature signatureData) throws Exception {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        ObjectOutputStream oout = new ObjectOutputStream(o);
        oout.writeObject(signatureData);
        oout.close();
        o.close();
        return o.toByteArray();
    }

    public static Signature deserializaFirmaFromBytes(byte[] serialicedSignature) throws Exception {
        ByteArrayInputStream is = new ByteArrayInputStream(serialicedSignature);
        ObjectInputStream ois = new ObjectInputStream(is);
        Signature firma = (Signature) ois.readObject();
        ois.close();
        return firma;
    }

    /**
     * Pasa cadena a B64 escapando caracteres para envio
     * @param cadena
     * @return
     * @throws Exception
     */
    public static String cadenaToBase64(String cadena) throws Exception {
        return bytesToBase64(getBytes(cadena));
    }

    /**
     * 
     * @param cadena
     * @return
     * @throws Exception
     */
    public static String cadenaToBase64UrlSafe(String cadena) throws Exception {
        return bytesToBase64UrlSafe(getBytes(cadena));
    }

    /**
     * @param cadena
     * @return
     */
    private static String bytesToBase64UrlSafe(byte[] bytes) throws Exception {
        return bytesToBase64(bytes, true);
    }

    /**
     * Pasa cadena a B64
     * @param cadena
     * @return
     */
    private static String bytesToBase64(byte[] bytes) throws Exception {
        return bytesToBase64(bytes, false);
    }

    /**
     * Pasa cadena a B64
     * @param cadena
     * @return
     */
    private static String bytesToBase64(byte[] bytes, boolean safe) throws Exception {
        char[] chars = Base64Coder.encode(bytes);
        String b64 = new String(chars);
        if (safe) b64 = escapeChars64UrlSafe(b64);
        return b64;
    }

    public static String base64ToCadena(String cadenaB64) throws Exception {
        return getString(base64ToBytes(cadenaB64));
    }

    public static String base64UrlSafeToCadena(String cadenaB64) throws Exception {
        return getString(base64UrlSafeToBytes(cadenaB64));
    }

    /**
     * Pasa a cadena una cadena en B64
     * @param cadena
     * @return
     */
    public static byte[] base64ToBytes(String cadenaB64) throws Exception {
        return base64ToBytes(cadenaB64, false);
    }

    public static byte[] base64ToBytes(String cadenaB64, boolean safe) throws Exception {
        if (safe) cadenaB64 = unescapeChars64UrlSafe(cadenaB64);
        return Base64Coder.decode(cadenaB64);
    }

    /**
     * Pasa a cadena una cadena en B64 escapando caracteres para envio
     * @param cadena
     * @return
     */
    private static byte[] base64UrlSafeToBytes(String cadenaB64) throws Exception {
        return base64ToBytes(cadenaB64, true);
    }

    public static byte[] certificateToBytes(X509Certificate cert) throws Exception {
        return cert.getEncoded();
    }

    public static X509Certificate bytesToCertificate(byte[] cert) throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(cert));
    }

    public static String generaHash(String cad) throws Exception {
        byte[] datos = cad.getBytes(FirmaUtil.CHARSET);
        MessageDigest dig = MessageDigest.getInstance("SHA-512");
        return new String(encodeHex(dig.digest(datos)));
    }

    public static char[] encodeHex(byte[] data) {
        int l = data.length;
        char[] out = new char[l << 1];
        for (int i = 0, j = 0; i < l; i++) {
            out[j++] = DIGITS[(0xF0 & data[i]) >>> 4];
            out[j++] = DIGITS[0x0F & data[i]];
        }
        return out;
    }

    /** 
     * Used building output as Hex 
     */
    private static final char[] DIGITS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    private static String escapeChars64UrlSafe(String cad) {
        cad = cad.replaceAll("\\+", "-");
        cad = cad.replaceAll("/", "_");
        cad = cad.replaceAll("[\\n\\r]", "");
        return cad;
    }

    private static String unescapeChars64UrlSafe(String cad) {
        cad = cad.replaceAll("-", "+");
        cad = cad.replaceAll("_", "/");
        return cad;
    }

    private static byte[] getBytes(String cadena) throws Exception {
        return cadena.getBytes(CHARSET);
    }

    private static String getString(byte[] bytes) throws Exception {
        return new String(bytes, CHARSET);
    }

    public static String isAlwaysDefaultContentTypeForSignature() {
        String strUseAlwaysDefaultContentType = null;
        try {
            strUseAlwaysDefaultContentType = System.getProperty("caibsignature.useAlwaysDefaultContentType");
        } catch (Exception exc) {
        }
        strUseAlwaysDefaultContentType = strUseAlwaysDefaultContentType != null ? strUseAlwaysDefaultContentType : "false";
        return strUseAlwaysDefaultContentType;
    }

    /**
     * Genera smime
     * 
     * @param document
     * @param signature
     * @param smime
     */
    public void generarSMIME(InputStream document, Signature signature, OutputStream smime) throws Exception {
        this.sigTradise.generateSMIME(document, signature, smime);
    }

    /**
     * Genera smime
     * 
     * @param document
     * @param signature
     * @param smime
     */
    public void generarSMIME(InputStream document, Signature signature[], OutputStream smime) throws Exception {
        this.sigTradise.generateSMIMEParalell(document, signature, smime);
    }

    /**
	 * Obtiene DNI certificado
	 * @param cert certificado
	 * @return	 dni
	 */
    public static String getDNI(Signature signature) throws Exception {
        SeyconPrincipal sp = new SeyconPrincipal(signature.getCert());
        return sp.getNif();
    }

    /**
	 * Obtiene nombre y apellidos
	 * @param cert
	 * @return
	 */
    public static String getNombreApellidos(Signature signature) throws Exception {
        SeyconPrincipal sp = new SeyconPrincipal(signature.getCert());
        return sp.getFullName();
    }
}
