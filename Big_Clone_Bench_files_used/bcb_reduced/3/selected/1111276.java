package com.cbsgmbh.xi.af.as2.as2;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.DigestInputStream;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.PrivilegedAction;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.MimeBodyPart;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.security.PasswordCredential;
import javax.security.auth.Subject;
import com.cbsgmbh.xi.af.as2.crypt.exceptions.CryptException;
import com.cbsgmbh.xi.af.as2.jca.SPIConnectionRequestInfo;
import com.cbsgmbh.xi.af.as2.util.Transformer;
import com.cbsgmbh.xi.af.trace.helpers.BaseTracer;
import com.cbsgmbh.xi.af.trace.helpers.BaseTracerSapImpl;
import com.cbsgmbh.xi.af.trace.helpers.Tracer;
import com.cbsgmbh.xi.af.trace.helpers.TracerCategories;
import com.sap.aii.utilxi.base64.api.Base64;

/**
 * Utility class for AS2 Implementation
 * @author Andreas Schmidsberger
 */
public class Util {

    private static final BaseTracer baseTracer = new BaseTracerSapImpl(Util.class.getName(), TracerCategories.APP_ADAPTER_HTTP);

    public static final String AS2_VERSION_10 = "1.0";

    public static final String MIME_VERSION_10 = "1.0";

    /**
	 * Returns current date in given standard
	 * @param format Date format
	 * @return String Date
	 */
    public static String getCurrentDate(String format) {
        final Tracer tracer = baseTracer.entering("getCurrentDate(String format)", new Object[] { format });
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        String date = formatter.format(cal.getTime());
        tracer.leaving();
        return date;
    }

    /**
	 * Returns current date in default format
	 * Format: "EEE, dd MMM yyyy HH:mm:ss z"
	 * @return String Date
	 */
    public static String getCurrentDate() {
        String date = getCurrentDate("EEE, dd MMM yyyy HH:mm:ss z");
        return date;
    }

    /**
	 * Getter for current timestamp
	 * @return String (timestamp with format yyyyMMddHHmmss) 
	 */
    public static String getTimestamp() {
        String date = getCurrentDate("yyyyMMddHHmmss");
        return date;
    }

    /**
	 * Getter for local host name
	 * @return String (local host name)
	 * @throws UnknownHostException
	 */
    public static String getHostName() throws UnknownHostException {
        InetAddress host = InetAddress.getLocalHost();
        return host.getHostName();
    }

    /**
	 * Calculate MIME Content-Length parameter
	 * @param bodyPart MimeBodyPart the contentLength is calculated of
	 * @return String
	 * @throws AS2Exception
	 */
    public static String getContentLength(MimeBodyPart bodyPart) throws AS2Exception {
        final Tracer tracer = baseTracer.entering("getContentLength(MimeBodyPart bodyPart)");
        AS2Exception as2ex = null;
        String contentLength;
        try {
            if (bodyPart != null) {
                ByteArrayOutputStream data = new ByteArrayOutputStream();
                copy(bodyPart.getInputStream(), data);
                contentLength = Integer.toString(data.size());
            } else {
                String error = "Content-Length cannot be calculated. SMimeBodyPart is null.";
                tracer.error(error);
                as2ex = new AS2Exception(error);
                tracer.throwing(as2ex);
                throw as2ex;
            }
        } catch (Exception ex) {
            String error = "Content-Length cannot be calculated. Exception occurred: " + ex.getMessage();
            tracer.catched(ex);
            as2ex = new AS2Exception(error, ex);
            tracer.throwing(as2ex);
            throw as2ex;
        }
        tracer.leaving();
        return contentLength;
    }

    /**
	 * Generates messageId of MDN
	 * @param fromParty sender party
	 * @param toParty	receiver party
	 * @return String
	 */
    public static String generateMessageId(String fromParty, String toParty) {
        final Tracer tracer = baseTracer.entering("generateMessageId(String fromParty, String toParty)");
        StringBuffer buf = new StringBuffer();
        String date = Util.getCurrentDate("yyyyMMddHHmmssSSSZ");
        buf.append("<AS2-").append(date);
        DecimalFormat randomFormatter = new DecimalFormat("0000");
        int randomInt = (new Random()).nextInt(10000);
        buf.append("-").append(randomFormatter.format(randomInt));
        buf.append("@").append(fromParty).append("_").append(toParty).append(">");
        tracer.leaving();
        return buf.toString();
    }

    /**
	 * Calculates the MIC (message integrity check) of the given SMimeBodyPart
	 * @param part SMimeBodyPart the MIC is calculated of
	 * @param digest Message digest algorithm
	 * @param includeHeaders boolean value if headers are included in calculation
	 * @return String
	 * @throws GeneralSecurityException
	 * @throws MessagingException
	 * @throws IOException
	 * @throws CryptException
	 * @throws CloneNotSupportedException
	 */
    public static String calculateMIC(MimeBodyPart part, String digest, boolean includeHeaders) throws GeneralSecurityException, MessagingException, IOException, CryptException, CloneNotSupportedException {
        final Tracer tracer = baseTracer.entering("calculateMIC(MimeBodyPart part, String digest, boolean includeHeaders)", new Object[] { "part", digest, new Boolean(includeHeaders) });
        byte[] mic = null;
        MessageDigest md = MessageDigest.getInstance(digest.toUpperCase());
        md = (MessageDigest) md.clone();
        tracer.info("MessageDigest object has been created.");
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        byte[] data = null;
        if (includeHeaders) {
            data = Transformer.convertInputStreamToByteArray(part.getDataHandler().getDataSource().getInputStream());
            tracer.info("Part has been converted to byte array.");
        } else {
            Util.copy(part.getInputStream(), bOut);
            tracer.info("Part object has been copied to output stream.");
            data = bOut.toByteArray();
        }
        InputStream bIn = Util.trimCRLFPrefix(data);
        tracer.info("CRLFPrefix has been dropped.");
        DigestInputStream digIn = new DigestInputStream(bIn, md);
        for (byte[] buf = new byte[4096]; digIn.read(buf) >= 0; ) ;
        bOut.close();
        mic = digIn.getMessageDigest().digest();
        String micString = new String(Base64.encode(mic));
        StringBuffer micResult = new StringBuffer(micString);
        micResult.append(", ").append(digest);
        tracer.leaving();
        return micResult.toString();
    }

    /**
	 * Calculates the MIC (message integrity check) of the given byte array
	 * @param byte[] message the MIC is calculated of
	 * @param digest Message digest algorithm
	 * @return String
	 * @throws GeneralSecurityException
	 * @throws MessagingException
	 * @throws IOException
	 * @throws CryptException
	 * @throws CloneNotSupportedException
	 */
    public static String calculateMIC(byte[] message, String digest) throws GeneralSecurityException, MessagingException, IOException {
        final Tracer tracer = baseTracer.entering("calculateMIC2(byte[] message, String digest)", new Object[] { "part", digest });
        byte mic[];
        MessageDigest md = MessageDigest.getInstance(digest);
        DigestInputStream digIn = new DigestInputStream(new ByteArrayInputStream(message), md);
        for (byte buf[] = new byte[4096]; digIn.read(buf) >= 0; ) {
        }
        mic = digIn.getMessageDigest().digest();
        digIn.close();
        String micString = new String(Base64.encode(mic));
        micString += ", " + digest;
        tracer.leaving();
        return micString;
    }

    /**
	 * Copies InputStream in in OutputStream out
	 * @param in InputStream
	 * @param out OutputStream
	 * @return void
	 * @throws IOException
	 */
    public static int copy(InputStream in, OutputStream out) throws IOException {
        final Tracer tracer = baseTracer.entering("copy(InputStream in, OutputStream out)");
        int totalCount = 0;
        byte[] buf = new byte[4096];
        int count = 0;
        do {
            if (count < 0) break;
            count = in.read(buf);
            totalCount += count;
            if (count > 0) out.write(buf, 0, count);
        } while (true);
        tracer.leaving();
        return totalCount;
    }

    /**
	 * @param data byte[]
	 * @return InputStream
	 */
    public static InputStream trimCRLFPrefix(byte[] data) {
        final Tracer tracer = baseTracer.entering("trimCRLFPrefix(byte[] data)");
        ByteArrayInputStream bIn = new ByteArrayInputStream(data);
        int scanPos = 0;
        for (int len = data.length; scanPos < len - 1; ) {
            if ((new String(data, scanPos, 2)).equals("\r\n")) {
                bIn.read();
                bIn.read();
                scanPos += 2;
            } else {
                tracer.leaving();
                return bIn;
            }
        }
        tracer.leaving();
        return bIn;
    }

    /**
     * Helper method for JCA Security (PasswordCredential)
     * 
     * @param managedConnectionFactory
     * @param subject
     * @param connectionRequestInfo
     * @return PasswordCredential
     */
    @SuppressWarnings("unchecked")
    public static PasswordCredential getPasswordCredential(final ManagedConnectionFactory managedConnectionFactory, final Subject subject, ConnectionRequestInfo connectionRequestInfo) throws ResourceException {
        final Tracer tracer = baseTracer.entering("getPasswordCredential(final ManagedConnectionFactory managedConnectionFactory, final Subject subject, ConnectionRequestInfo connectionRequestInfo)");
        PasswordCredential passwordCredential = null;
        if ((subject == null) && !(connectionRequestInfo == null)) {
            SPIConnectionRequestInfo spiConnectionRequestInfo = (SPIConnectionRequestInfo) connectionRequestInfo;
            passwordCredential = new PasswordCredential(spiConnectionRequestInfo.getUserName(), spiConnectionRequestInfo.getPassword().toCharArray());
            passwordCredential.setManagedConnectionFactory(managedConnectionFactory);
        } else {
            passwordCredential = (PasswordCredential) AccessController.doPrivileged(new PrivilegedAction() {

                public Object run() {
                    Set credentials = subject.getPrivateCredentials(PasswordCredential.class);
                    Iterator it = credentials.iterator();
                    while (it.hasNext()) {
                        PasswordCredential tempPasswordCredential = (PasswordCredential) it.next();
                        if (tempPasswordCredential.getManagedConnectionFactory().equals(managedConnectionFactory)) {
                            return tempPasswordCredential;
                        }
                    }
                    return null;
                }
            });
            if (passwordCredential == null) throw new SecurityException("Aborted. Reason: PasswordCredential not found");
        }
        tracer.leaving();
        return passwordCredential;
    }

    /**
     * Internal helper routine to compare two objects
     *
     * @param o1	First object to compare
     * @param o2	Second object to compare
     * @return boolean True of objects are equal 
     */
    public static boolean isEqualObject(Object o1, Object o2) {
        if (o1 == null) {
            return (o2 == null);
        }
        return o1.equals(o2);
    }

    /**
     * Compares two string with <code>null</code> consideration
     * (ra implementation specific)
     *
     * @param a string to compare
     * @param b string to compare
     * @return True if both strings are equal or both null
     **/
    public static boolean isEqualString(String a, String b) {
        if (a == null) {
            return (b == null);
        }
        return a.equals(b);
    }
}
