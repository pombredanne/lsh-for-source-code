package it.infn.catania.authentication.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.locks.Lock;
import org.apache.log4j.Logger;
import org.glite.security.voms.FQAN;
import org.glite.security.voms.VOMSAttribute;

public class ReplicaSelection {

    private String basePath = "";

    static Logger logger = Logger.getLogger(Keystore.class.getName());

    private int errorCode = -1;

    public ReplicaSelection(String path) {
        basePath = path;
    }

    public String getHash(String str) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(str.getBytes());
            byte[] toChapter1Digest = md.digest();
            return Keystore.hexEncode(toChapter1Digest);
        } catch (Exception e) {
            logger.error("Error in creating DN hash: " + e.getMessage());
            return null;
        }
    }

    private String[] readVOMSFile(String filename) {
        try {
            FileInputStream fis = new FileInputStream(filename);
            String vo_perm = "";
            byte[] temp = new byte[fis.available()];
            fis.read(temp);
            vo_perm = new String(temp);
            logger.debug("Read vo_perm =" + vo_perm);
            String[] fqan = vo_perm.split(":");
            for (int i = 0; i < fqan.length; i++) logger.debug("fqan[" + i + "]=" + fqan[i]);
            fis.close();
            return fqan;
        } catch (FileNotFoundException exc) {
            logger.error("Error in readFile: " + exc.getMessage());
            return null;
        } catch (IOException exc) {
            logger.error("Error in readFile: " + exc.getMessage());
            return null;
        }
    }

    private boolean saveFile(String filename, String data) {
        try {
            File f = new File(filename);
            if (!f.createNewFile()) {
                logger.error("Error in create file: " + filename);
                return false;
            }
            FileOutputStream fos = new FileOutputStream(filename);
            fos.write(data.getBytes());
            fos.close();
            return true;
        } catch (FileNotFoundException exc) {
            logger.error("Error in saveFile: " + exc.getMessage());
            return false;
        } catch (IOException exc) {
            logger.error("Error in saveFile: " + exc.getMessage());
            return false;
        }
    }

    private String readFile(String filename) {
        try {
            byte[] temp = new byte[1024];
            FileInputStream fis = new FileInputStream(filename);
            int num_bytes = fis.read(temp);
            fis.close();
            byte[] data = new byte[num_bytes];
            System.arraycopy(temp, 0, data, 0, num_bytes);
            return new String(data);
        } catch (FileNotFoundException exc) {
            logger.error("Error in readFile: " + exc.getMessage());
            return null;
        } catch (IOException exc) {
            logger.error("Error in readFile: " + exc.getMessage());
            return null;
        }
    }

    public int read_SE_count(String se) {
        try {
            File dir = new File(basePath + "/replicaSelection/" + se);
            File file = new File(basePath + "/replicaSelection/" + se + "/" + "count");
            String fileSE = basePath + "/replicaSelection/" + se + "/" + "count";
            int seCount = 0;
            String seCountString = null;
            if (!dir.exists()) {
                return 0;
            } else {
                seCountString = readFile(fileSE);
                seCount = Integer.parseInt(seCountString);
                return seCount;
            }
        } catch (Exception exc) {
            logger.error("Error in read_SE_count: " + exc.getMessage());
            return -1;
        }
    }

    public synchronized boolean increase_SE(String se) {
        try {
            File dir = new File(basePath + "/replicaSelection/" + se);
            File file = new File(basePath + "/replicaSelection/" + se + "/" + "count");
            String fileSE = basePath + "/replicaSelection/" + se + "/" + "count";
            int seCount = 0;
            String seCountString = null;
            BufferedWriter output = null;
            if (!dir.exists()) {
                synchronized (this) {
                    dir.mkdirs();
                    seCount = 1;
                    seCountString = Integer.toString(seCount);
                    if (saveFile(fileSE, seCountString)) {
                        return true;
                    } else {
                        errorCode = 2;
                        return false;
                    }
                }
            } else if (!file.exists()) {
                seCount = 1;
                seCountString = Integer.toString(seCount);
                synchronized (this) {
                    if (saveFile(fileSE, seCountString)) {
                        return true;
                    } else {
                        errorCode = 2;
                        return false;
                    }
                }
            } else {
                synchronized (this) {
                    seCountString = readFile(fileSE);
                    seCount = Integer.parseInt(seCountString);
                    seCount++;
                    seCountString = Integer.toString(seCount);
                    output = new BufferedWriter(new FileWriter(file));
                    output.write(seCountString);
                    output.close();
                }
                return true;
            }
        } catch (Exception exc) {
            logger.error("Error in increase_SE: " + exc.getMessage());
            return false;
        }
    }

    public synchronized boolean decrease_SE(String se) {
        try {
            File dir = new File(basePath + "/replicaSelection/" + se);
            File file = new File(basePath + "/replicaSelection/" + se + "/" + "count");
            String fileSE = basePath + "/replicaSelection/" + se + "/" + "count";
            int seCount = 0;
            String seCountString = null;
            BufferedWriter output = null;
            Object lock = new Object();
            if (!dir.exists()) {
                return true;
            } else if (!file.exists()) {
                return true;
            } else {
                synchronized (this) {
                    seCountString = readFile(fileSE);
                    seCount = Integer.parseInt(seCountString);
                    if (seCount > 0) {
                        seCount--;
                    } else {
                        return true;
                    }
                    seCountString = Integer.toString(seCount);
                    output = new BufferedWriter(new FileWriter(file));
                    output.write(seCountString);
                    output.close();
                }
                return true;
            }
        } catch (Exception exc) {
            logger.error("Error in decrease_SE: " + exc.getMessage());
            return false;
        }
    }

    public boolean isAuthorizedDN(String lfn, String dn) {
        String fileDN = basePath + "//" + getHash(lfn) + "//dn";
        String dn_auth = readFile(fileDN);
        if (dn_auth == null) {
            errorCode = 7;
            return false;
        }
        if (dn.equals(dn_auth)) return true; else {
            errorCode = 3;
            return false;
        }
    }

    public boolean isAuthorizedUser(String lfn, String dn, VOMSAttribute vomsAttr) {
        String fileDN = basePath + "//" + getHash(lfn) + "//dn";
        String dn_auth = readFile(fileDN);
        String fileVOMS = basePath + "//" + getHash(lfn) + "//voms";
        if (dn_auth == null) {
            errorCode = 7;
            return false;
        }
        if (dn.equals(dn_auth)) return true; else {
            String[] vo_perms = readVOMSFile(fileVOMS);
            int search = 1;
            String virtualOrganization = vomsAttr.getVO() + "\n";
            logger.debug("virtualOrganization=" + virtualOrganization);
            for (int i = 0; (i < vo_perms.length) && (search == 1); i++) if (vo_perms[i].equals(virtualOrganization)) search = 0;
            if (search == 0) return true;
            List listAttr = vomsAttr.getListOfFQAN();
            ListIterator listIterator = listAttr.listIterator();
            while (listIterator.hasNext() && (search == 1)) {
                FQAN fqan = ((FQAN) listIterator.next());
                String temp = fqan.getFQAN() + "\n";
                logger.debug("FQAN=" + temp);
                for (int i = 0; (i < vo_perms.length) && (search == 1); i++) if (vo_perms[i].equals(temp)) search = 0;
            }
            if (search == 0) return true;
            errorCode = 3;
            return false;
        }
    }

    public int getErrorCode() {
        return errorCode;
    }
}
