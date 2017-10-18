package net.sf.tomcatdeployer.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Server;
import org.apache.catalina.ServerFactory;
import org.apache.catalina.Service;
import org.apache.tomcat.util.modeler.Registry;

public class TomcatHelperService implements HelperService {

    private Server server = null;

    private Service catalina = null;

    private Engine engine = null;

    private Host host = null;

    private ObjectName check = null;

    private MBeanServer mBeanServer = null;

    public TomcatHelperService() {
        server = ServerFactory.getServer();
        catalina = server.findService("Catalina");
        engine = (Engine) catalina.getContainer();
        host = (Host) engine.findChild("localhost");
        mBeanServer = Registry.getRegistry(null, null).getMBeanServer();
        try {
            check = new ObjectName(engine.getName() + ":type=Deployer,host=" + host.getName());
        } catch (MalformedObjectNameException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Translate a virtual path to a real path e.g: /sgxotc/WEB-INF/web.xml to
	 * /home/tomcat/6/webapp/sgxotc/WEB-INF/web.xml
	 * 
	 * @param virtualPath
	 *            The virtual full path of the file
	 * @return The real path
	 */
    private String translate(String virtualPath) {
        return null;
    }

    /**
	 * 
	 * @param context
	 * @return
	 */
    private String findContextRealPath(String context) {
        Context ctx = findContext(context);
        System.out.println("Context Object found: " + ctx);
        if (ctx != null) {
            return ctx.getServletContext().getRealPath("/");
        } else {
            return null;
        }
    }

    private Context findContext(String context) {
        if (context.equals("/")) {
            context = "";
        }
        Context ctx = (Context) host.findChild(context);
        return ctx;
    }

    public String checkSum(String context, String file) {
        String ctxRoot = findContextRealPath(context);
        String realFile = ctxRoot + file;
        realFile = replace(realFile, '\\', File.separatorChar);
        realFile = replace(realFile, '/', File.separatorChar);
        return checkSum(realFile);
    }

    private String checkSum(String path) {
        File f = new File(path);
        if (f.isDirectory() || !f.exists() || !f.canRead()) {
            return "0";
        } else {
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                FileInputStream fis = new FileInputStream(f);
                byte[] buffer = new byte[4096];
                while (true) {
                    int read = fis.read(buffer);
                    if (read == -1) {
                        break;
                    }
                    if (read == 0) {
                        continue;
                    }
                    md.update(buffer, 0, read);
                }
                fis.close();
                return byteArrayToHexString(md.digest());
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
                return "0";
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return "0";
            } catch (IOException e) {
                e.printStackTrace();
                return "0";
            }
        }
    }

    private String byteArrayToHexString(byte in[]) {
        byte ch = 0x00;
        int i = 0;
        if (in == null || in.length <= 0) return null;
        String pseudo[] = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F" };
        StringBuffer out = new StringBuffer(in.length * 2);
        while (i < in.length) {
            ch = (byte) (in[i] & 0xF0);
            ch = (byte) (ch >>> 4);
            ch = (byte) (ch & 0x0F);
            out.append(pseudo[(int) ch]);
            ch = (byte) (in[i] & 0x0F);
            out.append(pseudo[(int) ch]);
            i++;
        }
        String rslt = new String(out);
        return rslt;
    }

    public List listFiles(String context) {
        String realPath = findContextRealPath(context);
        if (realPath == null) {
            return new ArrayList();
        }
        List result = new LinkedList();
        List files = new LinkedList();
        File contextRoot = new File(realPath);
        String rootAbsPath = contextRoot.getAbsolutePath();
        listFiles(contextRoot, files);
        for (Iterator iter = files.iterator(); iter.hasNext(); ) {
            File nextFile = (File) iter.next();
            Map nextFileInfo = new HashMap();
            nextFileInfo.put("lastModified", new Long(nextFile.lastModified()));
            nextFileInfo.put("size", new Long(nextFile.length()));
            String name = nextFile.getAbsolutePath().substring(rootAbsPath.length());
            name = replace(name, '\\', '/');
            nextFileInfo.put("name", name);
            nextFileInfo.put("type", nextFile.isDirectory() ? "D" : "F");
            result.add(nextFileInfo);
        }
        return result;
    }

    private void listFiles(File dir, List toAddTo) {
        if (dir == null || !dir.isDirectory() || !dir.exists()) {
            return;
        } else {
            File[] children = dir.listFiles(new FilenameFilter() {

                public boolean accept(File dir, String name) {
                    return !"..".equals(name) && !".".equals(name);
                }
            });
            for (int i = 0; i < children.length; i++) {
                if (children[i].isDirectory()) {
                    listFiles(children[i], toAddTo);
                }
                toAddTo.add(children[i]);
            }
        }
    }

    public boolean reloadContext(String context) {
        if (context == null) {
            return false;
        }
        Context ctx = findContext(context);
        if (ctx == null) {
            String[] params = { context };
            String[] signature = { "java.lang.String" };
            try {
                mBeanServer.invoke(this.check, "check", params, signature);
            } catch (InstanceNotFoundException e) {
                e.printStackTrace();
            } catch (MBeanException e) {
                e.printStackTrace();
            } catch (ReflectionException e) {
                e.printStackTrace();
            }
            ctx = (Context) host.findChild(context);
            if (ctx != null && ctx.getConfigured()) {
                return true;
            }
            return false;
        }
        try {
            return reload(ctx);
        } catch (LifecycleException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean reload(Context ctx) throws LifecycleException {
        if (ctx == null) {
            return false;
        }
        String path = ctx.getPath();
        if (path.length() == 0) {
            path = "/";
        }
        if (!ctx.getConfigured()) {
        } else {
            ((Lifecycle) ctx).start();
        }
        ctx.reload();
        return true;
    }

    public boolean removeFile(String context, String path) {
        String realPath = makeRealPath(context, path);
        File rf = new File(realPath);
        return delete(rf);
    }

    private String replace(String src, char what, char rep) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < src.length(); i++) {
            if (src.charAt(i) == what) {
                sb.append(rep);
            } else {
                sb.append(src.charAt(i));
            }
        }
        return sb.toString();
    }

    private String makeRealPath(String context, String path) {
        String realPath = findContextRealPath(context);
        if (realPath == null) {
            String ROOT = findContext("/TomcatHelper").getServletContext().getRealPath("/");
            realPath = ROOT + ".." + context + path;
            return realPath;
        }
        System.out.println("Context real path " + realPath);
        if (realPath == null) {
            return null;
        } else {
            System.out.println("1");
            if (realPath.endsWith(File.separator)) {
                realPath = realPath.substring(0, realPath.length() - 1);
            }
            System.out.println("2");
            realPath = realPath + path;
            System.out.println("3");
            System.out.println("RP = " + realPath);
            realPath = replace(realPath, '/', File.separatorChar);
            realPath = replace(realPath, '\\', File.separatorChar);
            System.out.println("Real path made: " + realPath);
            return realPath;
        }
    }

    private boolean delete(File path) {
        if (path.exists()) {
            if (!path.isDirectory()) {
                return path.delete();
            }
            File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++) {
                delete(files[i]);
            }
        }
        return path.delete();
    }

    public boolean saveAs(String context, String path, InputStream source) {
        System.out.println("Saving to " + context + path);
        String realPath = makeRealPath(context, path);
        System.out.println("Saving as " + realPath);
        if (realPath == null) {
            return false;
        } else {
            File toWrite = new File(realPath);
            File parent = toWrite.getParentFile();
            if (parent != null) {
                if (!parent.exists()) {
                    if (!parent.mkdirs()) {
                        System.out.println("Cannot mkdir " + parent);
                        return false;
                    }
                }
            }
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(toWrite);
                byte[] buf = new byte[1024];
                while (true) {
                    int read = source.read(buf);
                    if (read == -1) {
                        break;
                    }
                    if (read == 0) {
                        continue;
                    }
                    fos.write(buf, 0, read);
                }
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
                if (source != null) {
                    try {
                        source.close();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }

    public boolean touchFile(String context, String filePath) {
        String realPath = makeRealPath(context, filePath);
        if (realPath == null) {
            return false;
        } else {
            return new File(realPath).setLastModified(System.currentTimeMillis());
        }
    }

    public Date getServerTime() {
        return new Date();
    }

    public boolean mkdir(String context, String path) {
        String realPath = makeRealPath(context, path);
        File toCreate = new File(realPath);
        if (!toCreate.exists()) {
            return toCreate.mkdirs();
        } else {
            if (toCreate.isFile()) {
                return false;
            } else {
                return true;
            }
        }
    }
}
