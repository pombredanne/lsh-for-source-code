package pyConnect;

import java.security.MessageDigest;
import processing.core.PApplet;
import xmlrpclib.XmlrpcClient;

/**
 * pyClient is the main class and used to connect to a python server.
 * @author arne.alder
 * @category networking
 *
 */
public class pyClient {

    /**
   * Representing the PApplet owner.
   */
    public PApplet parent;

    /**
   * Representing the Client instance.
   * @see xmlrpclib.XmlrpcClient.XmlrpcClient
   */
    public XmlrpcClient client;

    /**
   * Saving the server address like "http://127.0.0.1:8765".
   */
    public String address;

    /**
   * Saving the client user name because pyCon is multiuser capable.
   */
    public String username;

    /**
   * Saving the md5 sessionid given from server.
   */
    private String sessionid;

    /**
   * Used to get chances from getting global status over (global) ping.
   */
    private String prev_global_pingmsg;

    /**
   * Representing the connection status.
   */
    public boolean connected;

    /**
   * Creates an new instance of a Processing - Python xmlRPC-Connection.
   * @param address The server address (somthing like "http://127.0.0.1:1234").
   */
    public pyClient(PApplet curParent, String addr) {
        parent = curParent;
        address = addr;
        client = new XmlrpcClient(address);
        username = "";
        sessionid = "";
        prev_global_pingmsg = "";
        connected = false;
    }

    /**
   * Executes the named function with a list of Objects as parameters on the server.
   * @param funcion Name of the function witch shall executed on the server.
   * @param params An Object array with all parameters for the named function.
   * @return A value defined by the called function.
   */
    public Object execute(String func, Object[] params) {
        if (!connected) {
            return null;
        }
        Object response = null;
        try {
            if (sessionid != "") {
                Object[] result = new Object[] { "[|||]" + sessionid + "[|||]" };
                response = client.execute(func, (Object[]) PApplet.concat(result, params));
            } else {
                response = client.execute(func, params);
            }
        } catch (Exception e) {
            PApplet.println("[ERROR]: PPI:  " + e);
            return null;
        }
        PApplet.println(func + " = " + response);
        return response;
    }

    /**
   * Executes the named function with a String as parameter on the server.
   * @param funcion Name of the function witch shall executed on the server.
   * @param params A String parameter for the named function.
   * @return A value defined by the called function.
   */
    public Object execute(String func, String params) {
        return execute(func, new Object[] { params });
    }

    /**
   * Executes the named function with a boolean as parameter on the server.
   * @param funcion Name of the function witch shall executed on the server.
   * @param params A boolean parameter for the named function.
   * @return A value defined by the called function.
   */
    public Object execute(String func, boolean params) {
        return execute(func, new Object[] { params });
    }

    /**
   * Executes the named function with an integer as parameter on the server.
   * @param funcion Name of the function witch shall executed on the server.
   * @param params An integer parameter for the named function.
   * @return A value defined by the called function.
   */
    public Object execute(String func, int params) {
        return execute(func, new Object[] { params });
    }

    /**
   * Executes the named function with a float as parameter on the server.
   * @param funcion Name of the function witch shall executed on the server.
   * @param params A float parameter for the named function.
   * @return A value defined by the called function.
   */
    public Object execute(String func, float params) {
        return execute(func, new Object[] { params });
    }

    /**
   * Executes the named function with a couple of bytes as parameter on the server.
   * @param funcion Name of the function witch shall executed on the server.
   * @param params A list of bytes as parameter for the named function.
   * @return A value defined by the called function.
   */
    public Object execute(String func, byte[] params) {
        return execute(func, new Object[] { params });
    }

    /**
   * Executes the named function with two Objects as parameter on the server.
   * @param funcion Name of the function witch shall executed on the server.
   * @param params Two Objects as parameter for the named function.
   * @return A value defined by the called function.
   */
    public Object execute(String func, Object param1, Object param2) {
        return execute(func, new Object[] { param1, param2 });
    }

    /**
   * Executes the named function with three Objects as parameter on the server.
   * @param funcion Name of the function witch shall executed on the server.
   * @param params Three Objects as parameter for the named function.
   * @return A value defined by the called function.
   */
    public Object execute(String func, Object param1, Object param2, Object param3) {
        return execute(func, new Object[] { param1, param2, param3 });
    }

    /**
   * Executes the named function with four Objects as parameter on the server.
   * @param funcion Name of the function witch shall executed on the server.
   * @param params Four Objects as parameter for the named function.
   * @return A value defined by the called function.
   */
    public Object execute(String func, Object param1, Object param2, Object param3, Object param4) {
        return execute(func, new Object[] { param1, param2, param3, param4 });
    }

    /**
   * Connecting to a xmlRPC Python Server at the given address as user.
   * @param Name The name of user of the client.
   * @param Password The password of the user of the client.
   * @return The connection status.
   * True if connected to the server.
   */
    public boolean connect(String sname, String spasswd) {
        String time = PApplet.str(UnixTimeStamp());
        String token = md5(md5(spasswd) + time);
        username = sname;
        connected = true;
        sessionid = "";
        Object response = execute("handshake", sname, time, token);
        if (response != null) {
            String[] answers = PApplet.split((String) response, " ");
            if (!answers[0].equals("ok")) {
                PApplet.println("[ERROR]: connecting " + address + " refused. Cause: " + answers[0]);
                connected = false;
            } else {
                sessionid = answers[1];
            }
        }
        return connected;
    }

    /**
   * Executes a stop signal on the server.
   * @return The answer on the stop signal.
   */
    public Object stop() {
        return execute("StopServer", true);
    }

    private Object _buildin_global_trigger(String func, Object[] params, boolean global) {
        Object response = null;
        if (global) {
            response = execute(func + "_global", params);
        } else {
            response = execute(func, params);
        }
        return response;
    }

    /**
   * Returns a server site defined user specific variable.
   * @param variablename The name of the requested variable.
   * @return The Varaible or an error from server.
   */
    public String ping(String varname) {
        return ping(varname, false);
    }

    /**
   * Returns a server site defined variable.
   * @param variablename The name of the requested variable.
   * @param global Defines if the variable is from global array (true) or user specific (false).
   * @return The Varaible or an error from server.
   */
    public String ping(String varname, boolean global) {
        Object response = null;
        Object[] params = null;
        if (global) {
            params = new Object[] { varname, prev_global_pingmsg };
        } else {
            params = new Object[] { varname };
        }
        response = _buildin_global_trigger("ping", params, global);
        if (response != null) {
            if (((String) response).equals("ok")) {
                return null;
            } else {
                String[] answers = PApplet.split((String) response, "=");
                if (answers[0].equals("changed")) {
                    if (global) {
                        prev_global_pingmsg = answers[1];
                    }
                    return answers[1];
                }
            }
        }
        return null;
    }

    /**
   * Reset the last requested global variable answer to "".
   */
    public void resetPing() {
        prev_global_pingmsg = "";
    }

    /**
   * Setting an user specific ping message with a new content.
   * @param variablename The name of the user specific ping message.
   * @param message The ping message witch will showen by requesting the given name by ping.
   * @return An answer or error from server.
   */
    public String setChanged(String varname, String msg) {
        return setChanged(varname, msg, false);
    }

    /**
   * Setting a global or user specific ping message with a new content.
   * @param variablename The name of the ping message.
   * @param message The ping message witch will showen by requesting the given name by ping.
   * @param global Defines if the ping message is from global array (true) or user specific (false).
   * @return An answer or error from server.
   */
    public String setChanged(String varname, String msg, boolean global) {
        Object response = _buildin_global_trigger("setChanged", new Object[] { varname, msg }, global);
        if (response != null) {
            return (String) response;
        }
        return null;
    }

    /**
   * Setting an user specific variable with a new content.
   * @param variablename The name of the user specific variable.
   * @param variablecontent The content witch will showen by requesting the given variable by ping.
   * @return An answer or error from server.
   */
    public String setVariable(String varname, String var) {
        return setVariable(varname, var, false);
    }

    /**
   * Setting a global or user specific variable with a new content.
   * @param variablename The name of the variable.
   * @param variablecontent The content witch will showen by requesting the given variable by ping.
   * @param global Defines if the variable is from global array (true) or user specific (false).
   * @return An answer or error from server.
   */
    public String setVariable(String varname, String var, boolean global) {
        Object response = _buildin_global_trigger("setVariable", new Object[] { varname, var }, global);
        if (response != null) {
            return (String) response;
        }
        return null;
    }

    /**
   * Getting an user specific variable from server.
   * @param variablename The name of the user specific variable.
   * @return The varaible or an error from server.
   */
    public String getVariable(String varname) {
        return getVariable(varname, false);
    }

    /**
   * Getting a global or user specific variable from server.
   * @param variablename The name of the variable.
   * @param global Defines if the variable is from global array (true) or user specific (false).
   * @return The varaible or an error from server.
   */
    public String getVariable(String varname, boolean global) {
        Object response = _buildin_global_trigger("getVariable", new Object[] { varname }, global);
        if (response != null) {
            return (String) response;
        }
        return null;
    }

    /**
   * Generating a simple and <strong>not</strong> correct POSIX Timestamp.
   * @return The Timestamp as String.
   */
    public static int UnixTimeStamp() {
        int result = (((((PApplet.year() - 1970) * 12 + PApplet.month()) * 30 + PApplet.day()) * 24 + PApplet.hour()) * 60 + PApplet.minute()) * 60 + PApplet.second();
        return result;
    }

    /**
   * Generates a MD5 digest from a String.
   * @param plaintext A String witch will be digest.
   * @return The md5 digest as String.
   */
    public static String md5(String plain) {
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            PApplet.println("[ERROR]: md5()   " + e);
            return "";
        }
        md5.reset();
        md5.update(plain.getBytes());
        byte[] result = md5.digest();
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < result.length; i += 1) {
            hexString.append(Integer.toHexString(0xFF & result[i]));
        }
        return hexString.toString();
    }
}
