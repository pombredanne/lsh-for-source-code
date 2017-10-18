package postgres;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import messages.AuthenticationCleartextPassword;
import messages.AuthenticationCryptPassword;
import messages.AuthenticationKerberosV5;
import messages.AuthenticationMD5Password;
import messages.AuthenticationOk;
import messages.AuthenticationSCMCredential;
import messages.ColumnData;
import messages.DataRow;
import messages.DefaultPostgreSQLMessageVisitor;
import messages.ParameterStatus;
import messages.PasswordMessage;
import messages.PostgreSQLMatcher;
import messages.Query;
import messages.ReadyForQuery;
import messages.StartupMessage;
import messages.StartupParameter;
import messages.Terminate;

public class Main extends DefaultPostgreSQLMessageVisitor {

    public static void main(String[] args) throws UnknownHostException, IOException {
        Main app = new Main();
        app.run();
    }

    private Encoder encoder;

    private OutputStream out;

    private Properties properties;

    private boolean finish;

    private Map<String, String> parameters = new HashMap<String, String>();

    private boolean queryDone = false;

    public void run() throws UnknownHostException, IOException {
        properties = new Properties();
        properties.load(new FileInputStream("src/postgres/database.properties"));
        Socket socket = new Socket("localhost", 5432);
        out = socket.getOutputStream();
        InputStream in = socket.getInputStream();
        encoder = new Encoder();
        List<StartupParameter> parameters = new ArrayList<StartupParameter>();
        parameters.add(new StartupParameter("user", (String) properties.get("user")));
        parameters.add(new StartupParameter("database", (String) properties.get("database")));
        StartupMessage mes = new StartupMessage(parameters);
        byte[] data = encoder.encode(mes);
        out.write(data);
        PostgreSQLMatcher matcher = new PostgreSQLMatcher();
        finish = false;
        InputBuffer buffer = new InputBuffer(in);
        while (!finish) {
            Message m = matcher.match(buffer);
            System.out.println(m);
            m.visit(this);
        }
    }

    public void visit(AuthenticationOk message) {
        System.out.println("auth ok");
    }

    public void visit(AuthenticationKerberosV5 message) {
        System.out.println("auth kerberos v5");
    }

    public void visit(AuthenticationCleartextPassword message) {
        System.out.println("auth cleartext");
    }

    public void visit(AuthenticationCryptPassword message) {
        System.out.println("auth crypt");
    }

    public void visit(AuthenticationMD5Password message) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(((String) properties.get("password") + (String) properties.get("user")).getBytes("iso8859-1"));
            String newValue = toHexString(md5.digest()) + new String(message.getSalt(), "iso8859-1");
            md5.reset();
            md5.update(newValue.getBytes("iso8859-1"));
            newValue = toHexString(md5.digest());
            PasswordMessage mes = new PasswordMessage("md5" + newValue);
            byte[] data = encoder.encode(mes);
            out.write(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static final String HEX_DIGITS = "0123456789abcdef";

    private static String toHexString(byte[] v) {
        StringBuffer sb = new StringBuffer(v.length * 2);
        for (int i = 0; i < v.length; i++) {
            int b = v[i] & 0xFF;
            sb.append(HEX_DIGITS.charAt(b >>> 4)).append(HEX_DIGITS.charAt(b & 0xF));
        }
        return sb.toString();
    }

    public void visit(AuthenticationSCMCredential message) {
        System.out.println("auth scm");
    }

    public void visit(ParameterStatus message) {
        parameters.put(message.getParameterName(), message.getParameterValue());
    }

    public void visit(ReadyForQuery message) {
        try {
            if (queryDone) {
                Terminate mes = new Terminate();
                byte[] data = encoder.encode(mes);
                out.write(data);
                finish = true;
                return;
            }
            Query mes = new Query(properties.getProperty("sqlrequest"));
            byte[] data = encoder.encode(mes);
            out.write(data);
            queryDone = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void visit(DataRow data) {
        System.out.println("======================");
        for (ColumnData column : data.getColumns()) {
            int length = column.getDataLength();
            if (length > 0) {
                for (int i = 0; i < length; ++i) {
                    System.out.print(Integer.toHexString(column.getData()[i]));
                }
                System.out.println();
            }
        }
        System.out.println("======================");
    }
}
