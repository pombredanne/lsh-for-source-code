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

    public void deleteFile(String pathFile) {
        File f = new File(pathFile);
        if (!f.exists()) throw new IllegalArgumentException("Delete: no such file or directory: " + pathFile);
        if (!f.canWrite()) throw new IllegalArgumentException("Delete: write protected: " + pathFile);
        if (f.isDirectory()) {
            String[] files = f.list();
            if (files.length > 0) throw new IllegalArgumentException("Delete: directory not empty: " + pathFile);
        }
        boolean success = f.delete();
        if (!success) throw new IllegalArgumentException("Delete: deletion failed");
    }

    public synchronized String executeRule(HttpServletRequest req, HttpSession session, String ruleListName) throws FactoryConfigurationError, Exception {
        String instance_folder = this.getND_path() + "\\UBLinstance";
        MultipartRequest multi = null;
        int maxUploadSize = 50 * 1024 * 1024;
        try {
            multi = new MultipartRequest(req, instance_folder, maxUploadSize);
        } catch (Exception e) {
            e.printStackTrace();
        }
        ;
        File myFile = multi.getFile("uploadfile");
        String filePath = multi.getOriginalFileName("uploadfile");
        String sourceFile = filePath;
        String userName = (String) session.getAttribute("user_name");
        String receiver = (String) session.getAttribute("receiver");
        String ublType = (String) session.getAttribute("ubl_type");
        session.putValue("source_file", sourceFile);
        DBmanager DBm = new DBmanager();
        DBm.connect(nomeDB, user, passwd);
        ArrayList auxList = DBm.getRuleByUserNameAndDocType(receiver, ublType);
        DBm.disConnect();
        ArrayList rulesList = (ArrayList) auxList.get(0);
        ArrayList rulesNameList = (ArrayList) auxList.get(1);
        MakeRule mr = new MakeRule();
        int ruleListLenght = rulesList.size();
        int i = 0;
        String ruleTemp;
        String ruleNameTemp;
        boolean existingrule = false;
        String mrule = "package Negotiation.com.sample;\n" + "import org.w3c.dom.Document;\n";
        while (i < ruleListLenght) {
            existingrule = true;
            ruleTemp = (String) rulesList.get(i);
            ruleNameTemp = (String) rulesNameList.get(i);
            mrule = mrule + mr.createRule(ruleTemp, ruleNameTemp);
            i++;
        }
        if (!existingrule) {
            mrule = "";
        } else {
            mr.writeRule(mrule, this.getRepository_path() + "\\" + "NegotiationRepository" + "\\" + ruleListName + ".drl");
        }
        DroolsTest dt = new DroolsTest();
        Document tmp = dt.goDrools(this.getRepository_path() + "\\" + "NegotiationRepository" + "\\" + ruleListName + ".drl", instance_folder + "\\" + sourceFile);
        OutputFormat format = new OutputFormat(tmp);
        format.setIndenting(true);
        XMLSerializer serializer = new XMLSerializer(new FileOutputStream(new File(this.getND_path() + "\\UBLinferred\\" + userName + "_" + ublType + ".xml")), format);
        serializer.serialize(tmp);
        deleteFile(this.getRepository_path() + "\\" + "NegotiationRepository" + "\\" + ruleListName + ".drl");
        return sourceFile;
    }

    public void makeRule(String rule, String ruleName, String inferredFile) throws IOException {
        MakeRule mr = new MakeRule();
        String mrule = mr.createRule(rule, ruleName);
        mr.writeRule(mrule, inferredFile);
    }

    public synchronized void doPost(HttpServletRequest req, HttpServletResponse response) throws IOException, ServletException {
        HttpSession session = req.getSession();
        String op = (String) req.getParameter("op");
        if (op == null) {
            op = "apply_rule";
        }
        System.out.println("ALESSANDRA ----------------> op " + op);
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
            if (op.equals("create_rule")) {
                check_rule(session);
                DBmanager DBm = new DBmanager();
                String user_name = (String) session.getAttribute("user_name");
                user_name = user_name.trim();
                String ubl_type = (String) session.getAttribute("ubl_type");
                Calendar data = Calendar.getInstance();
                java.util.Date date = data.getTime();
                String rule_name = (String) session.getAttribute("rule_name_fv");
                String antecedents = (String) req.getParameter("p_antecedents");
                String consequences = (String) req.getParameter("p_consequences");
                String rule = antecedents + " --> " + consequences;
                DBm.connect(nomeDB, user, passwd);
                DBm.insertRule(user_name, ubl_type, date.toString(), rule_name, rule);
                DBm.disConnect();
                session.putValue("rule_name_fv", "");
                ret_url = "../create_rule.jsp";
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
                                        e.printStackTrace();
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
