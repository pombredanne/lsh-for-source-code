import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Date;
import java.security.Security;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.sql.*;
import Negotiation.*;
import Negotiation.com.sample.*;
import Negotiation.DBconn.*;
import Negotiation.txt.*;
import com.oreilly.servlet.MultipartRequest;
import javax.xml.parsers.FactoryConfigurationError;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Document;
import org.apache.xml.serialize.OutputFormat;
import ipa.abilities.smix.webservice.pojos.test.*;
import java.io.File;
import java.io.PrintStream;
import java.net.URL;
import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.xml.namespace.QName;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;

public class ServletCreateRule extends HttpServlet {

    String nomeDB = "negotiation_db";

    String user = "root";

    String passwd = "";

    Properties props = System.getProperties();

    public ServletCreateRule() {
    }

    public void init(ServletConfig servletconfig) {
    }

    public String getND_path() {
        ResourceBundle resourceBundle = ResourceBundle.getBundle("negotiation");
        String p_repository_path = resourceBundle.getString("ND_path");
        return p_repository_path;
    }

    public String getRepository_path() {
        ResourceBundle resourceBundle = ResourceBundle.getBundle("abilities_bus");
        String p_repository_path = resourceBundle.getString("repository_path");
        return p_repository_path;
    }

    public String getAP_path() {
        ResourceBundle resourceBundle = ResourceBundle.getBundle("abilities_portal");
        String p_AP_path = resourceBundle.getString("AP_path");
        return p_AP_path;
    }

    public String getServer() {
        ResourceBundle resourceBundle = ResourceBundle.getBundle("abilities_portal");
        String p_server = resourceBundle.getString("server");
        return p_server;
    }

    public void check_login(String login, String pwd, HttpSession session) {
        DBmanager DBm = new DBmanager();
        DBm.connect(nomeDB, user, passwd);
        if (DBm.loginChecker(login, pwd)) {
            session.putValue("last_access", DBm.getLastAccess(login));
            Calendar data = Calendar.getInstance();
            java.util.Date date = data.getTime();
            DBm.updateLastAccess(login, date.toString());
            session.putValue("check_login", "ok");
            session.putValue("user_name", login);
            session.setAttribute("user_name", login);
            System.out.println("USER NAME=" + login);
            session.putValue("pwd", pwd);
        } else {
            session.putValue("check_login", "no");
        }
        DBm.disConnect();
    }

    public void check_rule(HttpSession session) {
        if (true) {
            session.putValue("check_rule", "ok");
        } else {
            session.putValue("check_rule", "no");
        }
    }

    public synchronized String executeRule(HttpServletRequest req, HttpSession session, String ruleListName) throws FactoryConfigurationError, Exception {
        String instance_folder = this.getND_path() + "\\UBLinstance";
        MultipartRequest multi = null;
        int maxUploadSize = 50 * 1024 * 1024;
        try {
            multi = new MultipartRequest(req, instance_folder, maxUploadSize);
        } catch (Exception e) {
        }
        ;
        File myFile = multi.getFile("uploadfile");
        String filePath = multi.getOriginalFileName("uploadfile");
        String sourceFile = filePath;
        String userName = (String) session.getAttribute("user_name");
        String ublType = (String) session.getAttribute("ubl_type");
        session.putValue("source_file", sourceFile);
        DroolsTest dt = new DroolsTest();
        Document tmp = dt.goDrools(this.getRepository_path() + "\\" + "NegotiationRepository" + "\\" + ruleListName + ".drl", instance_folder + "\\" + sourceFile);
        OutputFormat format = new OutputFormat(tmp);
        format.setIndenting(true);
        XMLSerializer serializer = new XMLSerializer(new FileOutputStream(new File(this.getND_path() + "\\UBLinferred\\" + userName + "_" + ublType + ".xml")), format);
        serializer.serialize(tmp);
        return sourceFile;
    }

    public void makeRule(String rule, String ruleName, String inferredFile) throws IOException {
        MakeRule mr = new MakeRule();
        String mrule = mr.createRule(rule, ruleName);
        mr.writeRule(mrule, inferredFile);
    }

    public void prove() {
    }

    public void send(String file2send) throws Exception {
        TestAbilitiesWSASync ipa;
        Service service = new Service();
        Call call = (Call) service.createCall();
        call.setTargetEndpointAddress(new URL(getServer()));
        call.setOperationName(new QName("urn:ipa:abilities", "createSendAbMessageASync"));
        Object obj[] = new Object[15];
        obj[0] = new String("TestMessageID");
        obj[1] = new Double(123.40000000000001D);
        obj[2] = new String("TestMessageCorrelationID");
        obj[3] = new String("TestProcessID");
        obj[4] = new Double(123.40000000000001D);
        obj[5] = new String("TestState");
        obj[6] = new String("TestUser");
        obj[7] = new String("TestPassword");
        obj[8] = new String("MD5");
        obj[9] = new String("TestAsynch");
        obj[10] = (new String[] { "receiver1" });
        obj[11] = new String("TestRecon");
        obj[12] = new String("TestNegot");
        DataHandler foo[] = new DataHandler[1];
        foo[0] = new DataHandler(new FileDataSource(new File(getAP_path() + "\\inbox\\test.zip")));
        obj[13] = foo;
        obj[14] = new DataHandler(new FileDataSource(new File(getAP_path() + "\\inbox\\" + file2send + ".xml")));
        call.setProperty("attachment_encapsulation_format", "axis.attachment.style.mtom");
        call.setProperty("javax.xml.rpc.encodingstyle.namespace.uri", "document");
        call.invokeOneWay(obj);
    }

    public synchronized void doPost(HttpServletRequest req, HttpServletResponse response) throws IOException, ServletException {
        this.prove();
        HttpSession session = req.getSession();
        String op = (String) req.getParameter("op");
        String actual_page = (String) session.getAttribute("actual_page");
        if (actual_page != null && actual_page.equals("apply_rules")) {
            op = "apply_rule";
        }
        String ret_url = null;
        if (op.equals("check_login")) {
            String login = (String) req.getParameter("login");
            String pwd = (String) req.getParameter("pwd");
            check_login(login, pwd, session);
            ret_url = "../login.jsp";
        } else {
            if (op.equals("send")) {
                try {
                    String file2send = (String) req.getParameter("file2send");
                    send(file2send);
                    ret_url = "../inbox.jsp";
                } catch (Exception e) {
                }
            } else {
                if (op.equals("new_user")) {
                    String uid = (String) req.getParameter("uid");
                    DBmanager DBm = new DBmanager();
                    DBm.connect(nomeDB, user, passwd);
                    if (DBm.checklogin(uid)) {
                        session.putValue("new_login", "uid_false");
                        ret_url = "../login.jsp";
                    } else {
                        String password = (String) req.getParameter("password");
                        String password2 = (String) req.getParameter("password2");
                        if (!password.equals(password2)) {
                            session.putValue("new_login", "false");
                            ret_url = "../login.jsp";
                        } else {
                            String name = (String) req.getParameter("name");
                            String surname = (String) req.getParameter("surname");
                            String address = (String) req.getParameter("address");
                            String city = (String) req.getParameter("city");
                            String zip = (String) req.getParameter("zip");
                            String country = (String) req.getParameter("country");
                            String phone = (String) req.getParameter("phone");
                            String fax = (String) req.getParameter("fax");
                            String email = (String) req.getParameter("email");
                            String web = (String) req.getParameter("web");
                            String result = "";
                            String encryptedString = uid + password;
                            int i;
                            java.security.MessageDigest md = null;
                            try {
                                md = java.security.MessageDigest.getInstance("md5");
                            } catch (NoSuchAlgorithmException ex) {
                                ex.printStackTrace();
                            }
                            byte[] pw = encryptedString.getBytes();
                            for (i = 0; i < pw.length; i++) {
                                int vgl = pw[i];
                                if (vgl < 0) vgl += 256;
                                if (32 < vgl) md.update(pw[i]);
                            }
                            byte[] bresult = md.digest();
                            result = "";
                            for (i = 0; i < bresult.length; i++) {
                                int counter = bresult[i];
                                if (counter < 0) counter += 256;
                                String counterStr = Integer.toString(counter, 16);
                                while (counterStr.length() < 2) counterStr = '0' + counterStr;
                                result += counterStr;
                            }
                            DBm.connect(nomeDB, user, passwd);
                            DBm.insert_new_user(uid, result, name, surname, address, city, zip, country, phone, fax, email, web);
                            DBm.disConnect();
                            session.putValue("new_login", "true");
                            String Dir = getAP_path() + "\\outbox\\" + uid;
                            (new File(Dir)).mkdirs();
                            Dir = getAP_path() + "\\inbox\\" + uid;
                            (new File(Dir)).mkdirs();
                            ret_url = "../login.jsp";
                        }
                    }
                    DBm.disConnect();
                } else {
                    if (op.equals("check_name_rule")) {
                        String user_name = (String) session.getAttribute("user_name");
                        String ubl_type = (String) session.getAttribute("ubl_type");
                        String rule_name = (String) req.getParameter("rule_name");
                        DBmanager DBm = new DBmanager();
                        DBm.connect(nomeDB, user, passwd);
                        if (DBm.titleChecker(user_name, rule_name, ubl_type)) {
                            session.putValue("check_name", "no");
                        } else {
                            session.putValue("check_name", "ok");
                            session.putValue("rule_name_fv", rule_name);
                        }
                        ret_url = "../create_rule.jsp";
                        DBm.disConnect();
                    } else {
                        if (op.equals("Continue")) {
                            String ubl_type = (String) req.getParameter("ubl_type");
                            session.putValue("ubl_type", ubl_type);
                            ret_url = "../home_page.jsp";
                        } else {
                            if (op.equals("del_rule")) {
                                String delList = (String) req.getParameter("delList");
                                StringTokenizer st = new StringTokenizer(delList, ",");
                                ArrayList delL = new ArrayList();
                                int count = 0;
                                while (st.hasMoreTokens()) {
                                    delL.add(count, st.nextToken());
                                    count++;
                                }
                                String user_name = (String) session.getAttribute("user_name");
                                String ubl_type = (String) session.getAttribute("ubl_type");
                                DBmanager DBm = new DBmanager();
                                DBm.connect(nomeDB, user, passwd);
                                DBm.deleteRule(user_name, ubl_type, delL);
                                DBm.disConnect();
                                ret_url = "../show_rules.jsp";
                            } else {
                                if (op.equals("apply_rule")) {
                                    try {
                                        String userName = (String) session.getAttribute("user_name");
                                        String receiver = (String) session.getAttribute("receiver");
                                        String ublType = (String) session.getAttribute("ubl_type");
                                        session.putValue("receiver", receiver);
                                        executeRule(req, session, userName + "_" + ublType);
                                        session.putValue("nameInferred", receiver + "_" + ublType);
                                        ret_url = "../apply_rules.jsp";
                                    } catch (Exception e) {
                                    }
                                    ;
                                } else {
                                }
                            }
                        }
                    }
                }
            }
        }
        response.setContentType("text/html");
        response.sendRedirect(ret_url);
    }
}
