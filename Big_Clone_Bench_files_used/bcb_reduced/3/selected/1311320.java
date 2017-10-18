package org.redwood.business.etl.logfileextractor;

import org.redwood.tools.MonitorMessenger;
import org.redwood.tools.UniqueKeyGenerator;
import java.security.MessageDigest;
import javax.ejb.SessionContext;
import java.rmi.RemoteException;

public abstract class LogFileExtractor {

    protected MonitorMessenger messenger = new MonitorMessenger();

    protected UniqueKeyGenerator primkeygen = new UniqueKeyGenerator();

    protected String login = "";

    protected String password = "";

    protected String websiteid = "";

    protected String datasourceid = "";

    protected String server = "";

    protected String sfilename = "";

    protected String spath = null;

    protected java.util.Date datetransferred = null;

    protected java.util.Date filedate = null;

    protected long filesize = 0;

    protected String logfileformat = "";

    protected String apacheformat = "";

    protected byte[] buffer;

    protected SessionContext sessionContext = null;

    /**
   * There must be one ejbCreate() method per create() method on the Home interface,
   * and with the same signature.
   *
   * @exception RemoteException If the instance could not perform the function
   *            requested by the container
   */
    public void ejbCreate() throws RemoteException {
    }

    /**
   * This method is called when the instance is activated from its "passive" state.
   * The instance should acquire any resource that it has released earlier in the
   * ejbPassivate() method.
   * This method is called with no transaction context.
   *
   * @exception RemoteException - Thrown if the instance could not perform the function
   *            requested by the container
   */
    public void ejbActivate() throws RemoteException {
    }

    /**
   * This method is called before the instance enters the "passive" state.
   * The instance should release any resources that it can re-acquire later in the
   * ejbActivate() method.
   * After the passivate method completes, the instance must be in a state that
   * allows the container to use the Java Serialization protocol to externalize
   * and store away the instance's state.
   * This method is called with no transaction context.
   *
   * @exception RemoteException - Thrown if the instance could not perform the function
   *            requested by the container
   */
    public void ejbPassivate() throws RemoteException {
    }

    /**
   * A container invokes this method before it ends the life of the session object.
   * This happens as a result of a client's invoking a remove operation, or when a
   * container decides to terminate the session object after a timeout.
   * This method is called with no transaction context.
   *
   * @exception RemoteException - Thrown if the instance could not perform the function
   *            requested by the container
   */
    public void ejbRemove() throws RemoteException {
    }

    /**
   * Sets the associated session context. The container calls this method after the instance
   * creation.
   * The enterprise Bean instance should store the reference to the context object
   * in an instance variable.
   * This method is called with no transaction context.
   *
   * @param sessionContext - A SessionContext interface for the instance.
   * @exception RemoteException - Thrown if the instance could not perform the function
   *            requested by the container because of a system-level error.
   */
    public void setSessionContext(SessionContext sessionContext) throws RemoteException {
        this.sessionContext = sessionContext;
    }

    protected void initValues() {
        websiteid = "";
        datasourceid = "";
        server = "";
        sfilename = "";
        spath = null;
        datetransferred = null;
        filedate = null;
        filesize = 0;
        apacheformat = "";
        logfileformat = "";
        buffer = null;
        login = "";
        password = "";
    }

    protected String getMD5Hash(MessageDigest md5) {
        StringBuffer sb = new StringBuffer();
        try {
            byte[] md5Byte = md5.digest();
            for (int i = 0; i < md5Byte.length; i++) {
                sb.append(Integer.toHexString((0xff & md5Byte[i])));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new String(sb.toString());
    }
}
