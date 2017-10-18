package gawky.incubator.smail;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertStore;
import java.security.cert.CertificateFactory;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import java.util.TimeZone;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.smime.SMIMECapabilitiesAttribute;
import org.bouncycastle.asn1.smime.SMIMECapability;
import org.bouncycastle.asn1.smime.SMIMECapabilityVector;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.mail.smime.SMIMEEnvelopedGenerator;
import org.bouncycastle.mail.smime.SMIMESignedGenerator;

/**
 * <TITLE> Send</TITLE> This class handles the creation of email, signing,
 * encryption and dispatching the email to the client.
 * 
 * @author Basit Hussain
 * @file Send.java
 * @version 1.0
 * @date 30/08/2004
 */
public class Runner {

    public static void main(String[] args) {
        Runner run = new Runner(null, 123, new String[] { "c:/test.dat" }, "ingo@ingoharbeck.de");
        prop.setProperty("SMTP_SERVER", "bfslnxwall.bertelsmann.de");
        prop.setProperty("SENDER", "ingo.harbeck@bertelsmann.de");
        prop.setProperty("REPLY_TO", "ingo.harbeck@bertelsmann.de");
        prop.setProperty("CERT", "D:/work/smail/cert/cert.pem");
        prop.setProperty("P12", "D:/work/smail/cert/bff.store");
        prop.setProperty("ALIAS", "mykey");
        prop.setProperty("PRV_KEY_PASSWD", "passphrase");
        prop.setProperty("SMTP_USERNAME", "");
        prop.setProperty("SMTP_PASSWORD", "");
        prop.setProperty("HTML_PATH", "c:/");
        prop.setProperty("HTML_IMAGE", "test2.dat");
        run.setHtml("TESTWETESTETET");
        run.SendMail();
    }

    private PrivateKey senderKey = null;

    private X509Certificate caCert = null;

    private X509Certificate senderCert = null;

    private X509Certificate rcptCert = null;

    static Properties prop;

    private Session session = null;

    private boolean octContent = false;

    private boolean sent = false;

    private ArrayList pdfFiles;

    private String xmlFile;

    private String html1 = "";

    private String html2 = "";

    private String html3 = "";

    private String html4 = "";

    private String html5 = "";

    private String html6 = "";

    private String fileNames[] = new String[1];

    private String email = "";

    public boolean noCert = false;

    Transport transport;

    DataOutputStream dos;

    public Runner(String configFile, int merchantNo, String[] fileNames, String merchantemail) {
        this.fileNames = fileNames;
        prop = new Properties();
        email = merchantemail;
    }

    /**
	 * This method is the caller to send the complete email to the specified
	 * client. It handles the complete flow.
	 * 
	 * @return int -Status code to confirm the status of the email.
	 */
    public int SendMail() {
        try {
            Security.addProvider(new BouncyCastleProvider());
            try {
                sent = false;
                noCert = false;
                boolean isSigning = true;
                boolean isEncrypting = true;
                String host = prop.getProperty("SMTP_SERVER");
                System.setProperty("mail.smtp.host", host);
                System.setProperty("mail.smtp.auth", "false");
                String sender = prop.getProperty("SENDER");
                String rcpt = email;
                String replyto = prop.getProperty("REPLY_TO");
                session = Session.getDefaultInstance(System.getProperties(), null);
                String tempMail = email.toLowerCase();
                if (noCert) {
                    return 2;
                }
                senderCert = getCert(prop.getProperty("CERT"), email);
                senderKey = getKey(prop.getProperty("P12"), prop.getProperty("ALIAS"));
                System.out.println("After creating message");
                MimeMessage msg = new MimeMessage(session);
                msg.setFrom(new InternetAddress(sender, "QNB"));
                InternetAddress[] to2 = { new InternetAddress(rcpt) };
                InternetAddress[] replyto2 = { new InternetAddress(replyto) };
                msg.setRecipients(Message.RecipientType.TO, rcpt);
                msg.setReplyTo(replyto2);
                msg.setSubject("Your Merchant e-Statement");
                msg.setSentDate(new Date());
                msg.saveChanges();
                MimeBodyPart plainMsg = createMessage(sender, rcpt, replyto);
                MimeMultipart mmp = sign(plainMsg);
                System.out.println("After Signing");
                msg.setContent(mmp);
                msg.saveChanges();
                System.out.println("set MEssage");
                String sss[] = { "Content-Type:", "To:" };
                Enumeration e = msg.getAllHeaderLines();
                msg.saveChanges();
                System.out.println("after save changes");
                transport = null;
                transport = session.getTransport("smtp");
                System.out.println("after getting transport");
                transport.connect(host, prop.getProperty("SMTP_USERNAME"), prop.getProperty("SMTP_PASSWORD"));
                System.out.println("b4 sending");
                transport.sendMessage(msg, msg.getAllRecipients());
                sent = true;
                transport.close();
            } catch (Exception mex) {
                sent = false;
                try {
                    transport.close();
                } catch (Exception ex) {
                    Thread.sleep(5000);
                }
                if (mex instanceof java.net.SocketException) {
                    System.out.println("SOCKET EXCEPTION");
                    Thread.sleep(5000);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
        if (sent) {
            return 1;
        } else {
            return 0;
        }
    }

    /**
	 * This method creates the complete email package with the attachments.
	 * 
	 * @param from -
	 *            Email Sender
	 * @param to -
	 *            Email Receiver
	 * @return MimeBodyPart Required in the SendMail() method.
	 */
    private MimeBodyPart createMessage(String from, String to, String replyto) {
        try {
            MimeBodyPart m = null;
            {
                BodyPart mbp_text = new MimeBodyPart();
                mbp_text.setContent(html1 + html2 + html3 + html4 + html5, "text/html");
                System.out.println("FILE ATTACHING");
                MimeMultipart mp = new MimeMultipart();
                mp.addBodyPart(mbp_text);
                int totalFiles = fileNames.length;
                String filenm;
                MimeBodyPart mbp55;
                DataSource src;
                for (int i = 0; i < totalFiles; i++) {
                    filenm = fileNames[i];
                    System.out.println("FILE NAME IN SEND: " + filenm);
                    mbp55 = new MimeBodyPart();
                    src = new FileDataSource(filenm);
                    mbp55.setDataHandler(new DataHandler(src));
                    filenm = extractFileName(filenm);
                    mbp55.setFileName(filenm);
                    mp.addBodyPart(mbp55);
                    System.out.println("FILE ATTACHED");
                }
                System.out.println("After the attaching");
                m = new MimeBodyPart();
                m.setContent(mp);
            }
            System.out.println("returning from the create mail");
            return m;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Unexpected error: " + e.toString());
        }
    }

    /**
	 * Signing the emails
	 * 
	 * @param MimeBodypart
	 * @return MimeMultipart Required format in the SendMail() method.
	 */
    private MimeMultipart sign(MimeBodyPart msg) {
        try {
            ArrayList certList = new ArrayList();
            certList.add(senderCert);
            CertStore certsAndcrls = CertStore.getInstance("Collection", new CollectionCertStoreParameters(certList), "BC");
            ASN1EncodableVector signedAttrs = new ASN1EncodableVector();
            SMIMECapabilityVector caps = new SMIMECapabilityVector();
            caps.addCapability(SMIMECapability.dES_EDE3_CBC);
            caps.addCapability(SMIMECapability.rC2_CBC, 128);
            caps.addCapability(SMIMECapability.dES_CBC);
            signedAttrs.add(new SMIMECapabilitiesAttribute(caps));
            SMIMESignedGenerator gen = new SMIMESignedGenerator();
            System.out.println(" Private Key :" + senderKey);
            System.out.println("Sender Cert :" + senderCert);
            gen.addSigner(senderKey, senderCert, SMIMESignedGenerator.DIGEST_SHA1, new AttributeTable(signedAttrs), null);
            gen.addCertificatesAndCRLs(certsAndcrls);
            MimeMultipart mm = gen.generate(msg, "BC");
            return mm;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
	 * Encryption of the email after signing.
	 * 
	 * @param MimeMessage -
	 *            The transformed package after the signing process.
	 * @return MimeBodyPart Required format in the SendMail() method.
	 */
    private MimeBodyPart encrypt(MimeMessage msg) {
        try {
            SMIMEEnvelopedGenerator genE = new SMIMEEnvelopedGenerator();
            genE.addKeyTransRecipient(rcptCert);
            MessageDigest dig = MessageDigest.getInstance("SHA1", "BC");
            dig.update(rcptCert.getPublicKey().getEncoded());
            genE.addKeyTransRecipient(rcptCert.getPublicKey(), dig.digest());
            MimeBodyPart mpp = genE.generate(msg, SMIMEEnvelopedGenerator.DES_EDE3_CBC, "BC");
            return mpp;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
	 * This method extracts the private key from the provided key.
	 * 
	 * @param keyFile -
	 *            Key file from which the private key has to be extracted.
	 * @param alias -
	 *            Alias for the key
	 * @return PrivateKey Extracted private key
	 */
    private PrivateKey getKey(String keyFile, String alias) {
        try {
            FileInputStream fis = new FileInputStream(keyFile);
            KeyStore ks = KeyStore.getInstance("JKS");
            char[] pw = prop.getProperty("PRV_KEY_PASSWD").toCharArray();
            System.out.println("loading keystore");
            ks.load(fis, pw);
            char[] pw2 = prop.getProperty("PRV_KEY_PASSWD").toCharArray();
            return (PrivateKey) ks.getKey(alias, pw2);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
	 * This method extracts the certificate from the certstore.
	 * 
	 * @param certFile -
	 *            Certificate File
	 * @param rcptEmail -
	 *            Email of the Receipient
	 * @return X509Certificate Certificate.
	 */
    private X509Certificate getCert(String certFile, String rcptEmail) {
        try {
            FileInputStream fis = new FileInputStream(certFile);
            DataInputStream dis = new DataInputStream(fis);
            byte[] data = new byte[dis.available()];
            dis.readFully(data);
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            CertificateFactory fact = CertificateFactory.getInstance("X509");
            X509Certificate cert = (X509Certificate) fact.generateCertificate(bais);
            return cert;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            noCert = true;
            return null;
        } catch (NullPointerException e) {
            e.printStackTrace();
            System.out.println("NULL POINTER");
            noCert = true;
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            noCert = true;
            return null;
        }
    }

    public String extractFileName(String filename) {
        String temp = "";
        String fl;
        for (int i = 0; i < filename.length(); i++) {
            fl = String.valueOf(filename.charAt(i));
            if (fl.equals("/")) {
                temp = "";
            } else {
                temp = temp + fl;
            }
        }
        return temp;
    }

    public String getDate() {
        Calendar cal = Calendar.getInstance(TimeZone.getDefault());
        String DATE_FORMAT = "yyyy-MM-dd--HH-mm-ss";
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        String today = sdf.format(cal.getTime());
        return today;
    }

    public void setHtml(String data) {
        html1 = data;
        html2 = html3 = html4 = html5 = "";
    }

    String getHtmlDate() {
        return (DateFormat.getDateInstance(DateFormat.FULL).format(new Date()));
    }
}
