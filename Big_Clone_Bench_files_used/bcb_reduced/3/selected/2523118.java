package org.infoeng.icws.clients;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.DigestInputStream;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.interfaces.DSAPrivateKey;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;
import java.util.Random;
import org.infoeng.icws.documents.CertificationRequest;
import org.infoeng.icws.documents.InformationIdentifier;
import org.infoeng.icws.documents.InformationCurrencySeries;
import org.infoeng.icws.utils.IssuanceUtils;
import org.infoeng.icws.utils.Utils;
import org.bouncycastle.openssl.PEMReader;

public class URLIssuanceClient {

    private static Properties issuanceProps;

    private static String usageStr = "\n" + "Usage: org.infoeng.icws.clients.URLIssuanceClient PARAMETER-FILE URL SERIES-TITLE [TEXT-UNDERLIER]\n" + "\n" + "   PARAMETER-FILE is the XML parameters file specifying the issuance parameters.\n" + "   URL is the unform resource locator to be used for generating information currency.\n" + "   SERIES-TITLE is the title of the series (this may be the title of the URL).\n" + "   TEXT-UNDERLIER is an optional string to be included as a string underlier\n" + "                  (this may be the RFC822 email address of the resource author).\n" + "\n" + "   For issuance, the PARAMETER-FILE must be a XML Java parameter file \n" + "  (see http://java.sun.com/dtd/properties.dtd), including the properties:\n" + "\n" + "   \"digest_algorithm\": the digest algorithm used to create the digest value of the resource.\n" + "\n" + "   \"certification_user\": the username provided to the issuer. \n" + "\n" + "   \"certification_endpoint\": the SOAP endpoint address of the issuer.\n" + "\n" + "   \"certification_keyfile\": the file containing the key for authenticating the issuance request - this \n" + "                            must be a SDSI key for this version of the program.\n" + "\n" + "   \"ics_output_directory\": the directory for output of the received information currency.\n" + "\n" + "   \"trustStore\": if the connection is over a SOAP endpoint, this is the Java \n" + "                 keystore file with the endpoint's SSL certificate.\n" + "\n" + "   \"trustStorePassword\": if the connection is over a SOAP endpoint, \n" + "                         this is the password to access the Java keystore specified in trustStore.\n\n";

    public static void main(String[] args) {
        try {
            if (!(args.length == 3 || args.length == 4)) {
                System.out.print(usageStr);
                System.exit(1);
            }
            issuanceProps = new Properties();
            issuanceProps.loadFromXML(new FileInputStream(args[0]));
            int loglevel = -1;
            try {
                int tmpInt = new Integer(issuanceProps.getProperty("client_loglevel")).intValue();
                if (tmpInt > 0 || tmpInt < 6) loglevel = tmpInt;
            } catch (java.lang.Exception e) {
            }
            URL certURL = new URL(args[1]);
            File tmpFile = File.createTempFile("issuance", ".url");
            System.out.println(" writing url " + certURL.toString() + " to file " + tmpFile.toURL().toString());
            FileOutputStream fos = new FileOutputStream(tmpFile);
            URLConnection certConnection = certURL.openConnection();
            InputStream certIS = certConnection.getInputStream();
            while (true) {
                int certInt = certIS.read();
                if (certInt == -1) break;
                fos.write(certInt);
            }
            FileInputStream fileFIS = new FileInputStream(tmpFile);
            MessageDigest md = MessageDigest.getInstance(issuanceProps.getProperty("digest_algorithm"));
            DigestInputStream dis = new DigestInputStream(fileFIS, md);
            fos.close();
            fos = new FileOutputStream("/dev/null");
            Utils.bufferedCopy(dis, fos);
            String digestValue = Utils.toHex(dis.getMessageDigest().digest());
            tmpFile.delete();
            String urlStr = certURL.toString();
            CertificationRequest certReq = new CertificationRequest();
            PrivateKey privKey = (PrivateKey) getPrivateKey(issuanceProps);
            certReq.addInformationIdentifier(new InformationIdentifier(urlStr, digestValue));
            String seriesTitle = null;
            if (args.length == 4 && args[3] != null) {
                certReq.addInformationIdentifier(new InformationIdentifier(args[3].trim()));
                seriesTitle = new String("Contribution of " + args[3].trim() + " to " + args[1] + ": " + args[2]);
            } else {
                seriesTitle = new String("" + args[1] + ": " + args[2] + "");
            }
            certReq.setTitle(seriesTitle);
            certReq.setNotAfter(new Date(System.currentTimeMillis() + 20000));
            certReq.setNotBefore(new Date());
            certReq.sign(privKey);
            if (loglevel > 3) System.out.println("CertificationRequest:\n" + certReq.toString() + "");
            if (!certReq.checkFields()) {
                System.out.println("Fields not filled!");
                return;
            }
            if (loglevel == 5) {
                File crFile = File.createTempFile("certification-request-", ".xml");
                java.io.FileWriter crWriter = new java.io.FileWriter(crFile);
                crWriter.write(certReq.toString());
                crWriter.close();
                System.out.println("Wrote certification request to " + crFile.getCanonicalPath() + ".");
            }
            String certResponse = IssuanceUtils.processCertificationRequest(issuanceProps, certReq.toString());
            if (certResponse == null) {
                System.out.println("No information currency series was received.");
            }
            if (certResponse != null) {
                InformationCurrencySeries ics = new InformationCurrencySeries(new ByteArrayInputStream(Utils.convertEntities(certResponse).getBytes()));
                try {
                    File outputFile = File.createTempFile("ics-", ".xml", new File(issuanceProps.getProperty("ics_output_directory")));
                    if (outputFile == null) {
                        System.out.println("Error writing information currency file.  Received information currency series is:");
                        System.out.println(ics.toString());
                        System.exit(1);
                    }
                    FileWriter fw = new FileWriter(outputFile);
                    fw.write("<!-- " + Utils.addEntities(seriesTitle) + " -->\n");
                    fw.write(ics.toString());
                    fw.flush();
                    fw.close();
                    System.out.println(" Wrote information currency series to " + outputFile.toString());
                    System.exit(0);
                } catch (java.lang.Exception e) {
                    System.out.println(" Error writing information currency file.  Received information currency series is:");
                    System.out.println(ics.toString());
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        } catch (java.lang.Exception e) {
            e.printStackTrace();
        }
    }

    private static PrivateKey getPrivateKey(Properties icwsProps) throws Exception {
        try {
            String certKeyFile = icwsProps.getProperty("certification_keyfile");
            if (certKeyFile == null) throw new Exception("certification_keyfile must be defined!");
            System.out.println("Reading key from " + certKeyFile + ".");
            java.security.Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
            PEMReader pr = new PEMReader(new BufferedReader(new FileReader(new File(certKeyFile))));
            KeyPair kp = (KeyPair) pr.readObject();
            DSAPrivateKey dpk = (DSAPrivateKey) kp.getPrivate();
            return dpk;
        } catch (java.lang.Exception e) {
            throw new Exception(e);
        }
    }
}
