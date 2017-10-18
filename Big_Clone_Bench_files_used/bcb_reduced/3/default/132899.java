import java.security.MessageDigest;
import java.sql.*;
import java.util.Stack;

/**
 *
 * @author Jose
 */
public class NewClass {

    private Connection con = null;

    private String url = "jdbc:sqlserver://";

    private String serverName = "";

    private String portNumber = "";

    private String databaseName = "";

    private String userName = "";

    private String password = "";

    private String selectMethod = "cursor";

    private Stack<Integer> ControlConexiones = new Stack();

    public String RutaBackup = "";

    public NewClass() {
    }

    public NewClass(String serverName, String databaseName, String userName, String password, String RutaBackup) {
        this.serverName = serverName;
        this.databaseName = databaseName;
        this.userName = userName;
        this.password = password;
        this.RutaBackup = RutaBackup;
    }

    public int getNumeroConexiones() {
        return ControlConexiones.size();
    }

    private String getConnectionUrl() {
        return url + serverName + ";databaseName=" + databaseName + ";selectMethod=" + selectMethod + ";";
    }

    public Connection IniciarConexion() {
        if (con == null) {
            try {
                Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
                con = java.sql.DriverManager.getConnection(getConnectionUrl(), userName, password);
                if (con != null) {
                    System.out.println("ConexiÃ³n correcta.");
                    ControlConexiones.push(1);
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
            return con;
        }
        return con;
    }

    /**
     * FunciÃ³n que permite ejecutar una instruccion SELECT
     * @param consulta Comando SQL con codigo SELECT
     * @return  ResultSet con los resultados de la consulta
     */
    public ResultSet EjecutarConsulta(String consulta) {
        try {
            Statement stat = con.createStatement();
            ResultSet ret = stat.executeQuery(consulta);
            return ret;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * MÃ©todo que permite ejecutar instrucciones distintas a la SELECT.
     * @param consulta  Instrucciones SQL
     */
    public boolean EjecutarInstruccion(String consulta) {
        try {
            Statement stat = con.createStatement();
            stat.execute(consulta);
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    /**
     * Cierra la conexion del objeto.
     */
    public boolean CerrarConexion() {
        try {
            if (con != null) {
                con.close();
                ControlConexiones.pop();
            }
            con = null;
            System.out.println("Conexion cerrada.");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Metodo que realiza un respaldo completo de la BD.
     * @param ruta Carpeta donde se ubicara el respaldo.     
     */
    public boolean Backup(String ruta) {
        return EjecutarInstruccion("EXEC dbo.HacerBackup '" + ruta + "';");
    }

    /**
     * Realiza una recuperaciÃ³n a partir de un archivo; si la conexiÃ³n se encuentra abierta se cerrara.
     * @param ruta Ruta del archivo de respaldo.
     */
    public boolean Restore(String ruta) {
        if (!ControlConexiones.isEmpty()) {
            return false;
        }
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            Connection master = java.sql.DriverManager.getConnection(getConnectionUrl(), userName, password);
            if (master != null) {
                System.out.println("ConexiÃ³n correcta con master.");
                Statement stat = master.createStatement();
                stat.execute("use master;" + "RESTORE DATABASE " + databaseName + " " + "FROM DISK ='" + ruta + "';");
                master.close();
                System.out.println("ConexiÃ³n con master cerrada.");
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error de seguimiento en getConnection() : " + e.getMessage());
        }
        return false;
    }

    public static String MD5(String clear) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] b = md.digest(clear.getBytes());
        int size = b.length;
        StringBuilder h = new StringBuilder(size);
        for (int i = 0; i < size; i++) {
            int u = b[i] & 255;
            if (u < 16) {
                h.append("0").append(Integer.toHexString(u));
            } else {
                h.append(Integer.toHexString(u));
            }
        }
        return h.toString();
    }

    static final String baseTable = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

    private static String encode(byte[] bytes) {
        StringBuffer tmp = new StringBuffer();
        int i = 0;
        byte pos;
        for (i = 0; i < (bytes.length - bytes.length % 3); i += 3) {
            pos = (byte) ((bytes[i] >> 2) & 63);
            tmp.append(baseTable.charAt(pos));
            pos = (byte) (((bytes[i] & 3) << 4) + ((bytes[i + 1] >> 4) & 15));
            tmp.append(baseTable.charAt(pos));
            pos = (byte) (((bytes[i + 1] & 15) << 2) + ((bytes[i + 2] >> 6) & 3));
            tmp.append(baseTable.charAt(pos));
            pos = (byte) (((bytes[i + 2]) & 63));
            tmp.append(baseTable.charAt(pos));
            if (((i + 2) % 56) == 0) {
                tmp.append("\r\n");
            }
        }
        if (bytes.length % 3 != 0) {
            if (bytes.length % 3 == 2) {
                pos = (byte) ((bytes[i] >> 2) & 63);
                tmp.append(baseTable.charAt(pos));
                pos = (byte) (((bytes[i] & 3) << 4) + ((bytes[i + 1] >> 4) & 15));
                tmp.append(baseTable.charAt(pos));
                pos = (byte) ((bytes[i + 1] & 15) << 2);
                tmp.append(baseTable.charAt(pos));
                tmp.append("=");
            } else if (bytes.length % 3 == 1) {
                pos = (byte) ((bytes[i] >> 2) & 63);
                tmp.append(baseTable.charAt(pos));
                pos = (byte) ((bytes[i] & 3) << 4);
                tmp.append(baseTable.charAt(pos));
                tmp.append("==");
            }
        }
        return tmp.toString();
    }

    /**
	 * Encode a String object. 
	 * 
	 * @param src a String object to be encoded with Base64 schema. 
	 * @return encoded String object. 
	 */
    public static String encode(String src) {
        return encode(src.getBytes());
    }

    public static byte[] decode(String src) throws Exception {
        byte[] bytes = null;
        StringBuffer buf = new StringBuffer(src);
        int i = 0;
        char c = ' ';
        char oc = ' ';
        while (i < buf.length()) {
            oc = c;
            c = buf.charAt(i);
            if (oc == '\r' && c == '\n') {
                buf.deleteCharAt(i);
                buf.deleteCharAt(i - 1);
                i -= 2;
            } else if (c == '\t') {
                buf.deleteCharAt(i);
                i--;
            } else if (c == ' ') {
                i--;
            }
            i++;
        }
        if (buf.length() % 4 != 0) {
            ;
        }
        bytes = new byte[3 * (buf.length() / 4)];
        int index = 0;
        for (i = 0; i < buf.length(); i += 4) {
            byte data = 0;
            int nGroup = 0;
            for (int j = 0; j < 4; j++) {
                char theChar = buf.charAt(i + j);
                if (theChar == '=') {
                    data = 0;
                } else {
                    data = getBaseTableIndex(theChar);
                }
                if (data == -1) {
                    ;
                }
                nGroup = 64 * nGroup + data;
            }
            bytes[index] = (byte) (255 & (nGroup >> 16));
            index++;
            bytes[index] = (byte) (255 & (nGroup >> 8));
            index++;
            bytes[index] = (byte) (255 & (nGroup));
            index++;
        }
        byte[] newBytes = new byte[index];
        for (i = 0; i < index; i++) {
            newBytes[i] = bytes[i];
        }
        return newBytes;
    }

    /**
	 * Find index number in base table for a given character. 
	 * 
	 */
    protected static byte getBaseTableIndex(char c) {
        byte index = -1;
        for (byte i = 0; i < baseTable.length(); i++) {
            if (baseTable.charAt(i) == c) {
                index = i;
                break;
            }
        }
        return index;
    }

    public static void main(String[] args) throws Exception {
        NewClass prueba = new NewClass();
        String encodedString = prueba.encode("bases2");
        System.out.println("coding <bases2> : " + encodedString);
        byte[] decodedBytes = prueba.decode(encodedString);
        String decodedString = new String(decodedBytes).trim();
        System.out.println("decoding <" + encodedString + "> : <" + decodedString + ">");
        prueba = new NewClass("localhost", "Bases2", "sa", decodedString, "");
        prueba.IniciarConexion();
        prueba.CerrarConexion();
    }
}
