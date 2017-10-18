package teamcal.retriever;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringBufferInputStream;
import java.security.MessageDigest;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class TeamcalRetriever {

    private static final String XML_HEADER = "<?xml version=\"1.0\" ?><!DOCTYPE table [<!ENTITY nbsp \"l\" >]>";

    private static final InputStream XSLT_FILE = TeamcalRetriever.class.getResourceAsStream("/teamcal/retriever/resources/extract.xslt");

    ;

    private static String MAIN_WEB_PAGE = "http://www.sciops.esa.int/index.php?project=CSG&page=teamcal";

    private static String TEAMCAL_WEB_PAGE = "http://www.sciops.esa.int/esac_csg_teamcal/index.php";

    private HttpClient m_client;

    private File m_rootDir;

    public TeamcalRetriever(File rootDir) {
        m_client = new HttpClient();
        m_rootDir = rootDir;
        init();
    }

    private void init() {
        try {
            PostMethod postRequest = new PostMethod(MAIN_WEB_PAGE);
            postRequest.setRequestBody(setLoginParams());
            m_client.executeMethod(postRequest);
            GetMethod getRequest = new GetMethod(TEAMCAL_WEB_PAGE);
            getRequest.setQueryString(setQueryParams());
            m_client.executeMethod(getRequest);
            retrieveBody(getRequest);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void retrieveBody(HttpMethod request) {
        try {
            StringBuffer sw = new StringBuffer();
            final int BUFFER_SIZE = 1 << 10 << 3;
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead = -1;
            InputStream in = request.getResponseBodyAsStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, "ISO-8859-1"));
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            String line;
            boolean active = false;
            StringBuffer all = new StringBuffer();
            StringBuffer partialDoc = new StringBuffer();
            Document mainDoc = builder.newDocument();
            Element br = mainDoc.createElement("holidays");
            mainDoc.appendChild(br);
            while ((line = reader.readLine()) != null) {
                if (line.equals("<table CLASS=\"month\" CELLSPACING=\"0\">")) {
                    if (partialDoc.length() > 0) {
                        Document tableDoc = builder.parse(new StringBufferInputStream(XML_HEADER + partialDoc.toString()));
                        NodeList nl = tableDoc.getElementsByTagName("table");
                        Element tableNode = (Element) nl.item(0);
                        mainDoc.adoptNode(tableNode);
                        br.appendChild(tableNode);
                        partialDoc = new StringBuffer();
                    }
                    active = true;
                    partialDoc.append(line);
                } else if (active) {
                    active = !line.equals("</table>");
                    if (line.indexOf("-button\"") == -1) {
                        partialDoc.append(line);
                    }
                }
                all.append(line);
            }
            in.close();
            TransformerFactory tfact = TransformerFactory.newInstance();
            Source xmlSource = new DOMSource(mainDoc);
            Date date = new Date();
            Format formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            String timeStamp = formatter.format(date);
            String teamcalFile = m_rootDir.getAbsolutePath() + File.separatorChar + "teamcal-" + timeStamp + ".xml";
            Source xsltSource = new StreamSource(XSLT_FILE);
            Result result = new StreamResult(new File(teamcalFile));
            Transformer trans = tfact.newTransformer(xsltSource);
            trans.transform(xmlSource, result);
            String prevTeamcalFile = getPreviousExecutionDir(m_rootDir);
            if (prevTeamcalFile != null) {
                String newFileMD5 = computeDigest(MessageDigest.getInstance("MD5"), loadBytes(teamcalFile));
                String lastFileMD5 = computeDigest(MessageDigest.getInstance("MD5"), loadBytes(prevTeamcalFile));
                if (!lastFileMD5.equals(newFileMD5)) {
                    sendDailyResume(loadBytes(teamcalFile));
                }
            } else {
                sendDailyResume(loadBytes(teamcalFile));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static byte[] loadBytes(String name) {
        FileInputStream in = null;
        try {
            in = new FileInputStream(name);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int bytesread = 0;
            byte[] tbuff = new byte[512];
            while ((bytesread = in.read(tbuff)) != -1) {
                buffer.write(tbuff, 0, bytesread);
            }
            return buffer.toByteArray();
        } catch (IOException e) {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e2) {
                }
            }
            return null;
        }
    }

    private NameValuePair[] setLoginParams() {
        NameValuePair[] data = { new NameValuePair("user", "randres"), new NameValuePair("pass", "BRABOJO"), new NameValuePair("submit", "Login") };
        return data;
    }

    private NameValuePair[] setQueryParams() {
        NameValuePair[] data = { new NameValuePair("groupfilter", "Herschel_SW_maint"), new NameValuePair("modefilter", "standard"), new NameValuePair("month_id", "6"), new NameValuePair("year_id", "2010"), new NameValuePair("show_id", "2"), new NameValuePair("btn_appy", "Apply") };
        return data;
    }

    private void sendDailyResume(byte[] bs) {
        Properties props = new Properties();
        props.setProperty("mail.smtp.host", "scimail.esac.esa.int");
        props.setProperty("mail.smtp.port", "25");
        props.setProperty("mail.smtp.user", "randres@sciops.esa.int");
        props.setProperty("mail.smtp.auth", "true");
        Session session = Session.getDefaultInstance(props);
        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress("randres@sciops.esa.int"));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress("randres@sciops.esa.int"));
            message.setSubject("[HOLIDAY] Some changes on holidays");
            message.setText("<h1>Hi!</h1><p>The holiday monitor has logged the following activity in the server.</p><pre>" + new String(bs) + "</pre>", "ISO-8859-1", "html");
            Transport t = session.getTransport("smtp");
            t.connect("randres@sciops.esa.int", "W=1\\T=2");
            t.sendMessage(message, message.getAllRecipients());
            t.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String computeDigest(MessageDigest currentAlgorithm, byte[] b) {
        currentAlgorithm.reset();
        currentAlgorithm.update(b);
        byte[] hash = currentAlgorithm.digest();
        String d = " ";
        int usbyte = 0;
        for (int i = 0; i < hash.length; i += 2) {
            usbyte = hash[i] & 0xFF;
            if (usbyte < 16) d += "0" + Integer.toHexString(usbyte); else d += Integer.toHexString(usbyte);
            usbyte = hash[i + 1] & 0xFF;
            if (usbyte < 16) d += "0" + Integer.toHexString(usbyte) + " "; else d += Integer.toHexString(usbyte) + " ";
        }
        return d.toUpperCase();
    }

    private static String getPreviousExecutionDir(File rootDir) {
        String previousExecutionDir = null;
        FilenameFilter ff = new TeamCalDirectoryFilter();
        String[] fl = rootDir.list(ff);
        Arrays.sort(fl);
        if (fl.length > 0) {
            previousExecutionDir = fl[fl.length - 1];
        }
        return previousExecutionDir;
    }

    public static void main(String[] args) {
        long tstart = System.currentTimeMillis();
        TeamcalRetriever hr = new TeamcalRetriever(new File("/home/randres/teamcal"));
        long tend = System.currentTimeMillis();
        System.out.println((tend - tstart) / 1000);
    }
}

class TeamCalDirectoryFilter implements FilenameFilter {

    public TeamCalDirectoryFilter() {
    }

    public boolean accept(File file, String name) {
        return name.startsWith("teamcal") && file.isFile();
    }
}
