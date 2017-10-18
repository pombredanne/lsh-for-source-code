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
import org.apache.log4j.Logger;
import org.glite.security.voms.FQAN;
import org.glite.security.voms.VOMSAttribute;

public class Keystore {

    private String basePath = "";

    static Logger logger = Logger.getLogger(Keystore.class.getName());

    private int errorCode = -1;

    public Keystore(String path) {
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

    private boolean saveVOMSFile(String filename, String vo_permission) {
        try {
            File f = new File(filename);
            if (!f.createNewFile()) {
                logger.error("Error in create file: " + filename);
                return false;
            }
            FileOutputStream fos = new FileOutputStream(filename);
            String virtualOrganization = vo_permission + "\n";
            fos.write(virtualOrganization.getBytes());
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

    public boolean storeKey(String lfn, String key, String iv, String dn, String vo_permission) {
        try {
            String hashLfn = getHash(lfn);
            File dir = new File(basePath + "//" + hashLfn);
            if (!dir.exists()) {
                dir.mkdirs();
            } else {
                errorCode = 8;
                return false;
            }
            String fileKey = basePath + "//" + hashLfn + "//key";
            String fileIV = basePath + "//" + hashLfn + "//iv";
            String fileDN = basePath + "//" + hashLfn + "//dn";
            String fileVOMS = basePath + "//" + hashLfn + "//voms";
            if ((saveFile(fileKey, key)) && (saveFile(fileIV, iv)) && (saveFile(fileDN, dn)) && (saveVOMSFile(fileVOMS, vo_permission))) return true; else {
                errorCode = 2;
                return false;
            }
        } catch (Exception exc) {
            logger.error("Error in storeKey: " + exc.getMessage());
            return false;
        }
    }

    public String getKey(String lfn, String dn) {
        String fileKey = basePath + "//" + getHash(lfn) + "//key";
        return readFile(fileKey);
    }

    public String getIV(String lfn, String dn) {
        String fileIV = basePath + "//" + getHash(lfn) + "//iv";
        return readFile(fileIV);
    }

    public boolean deleteKey(String lfn) {
        String folderPath = basePath + "//" + getHash(lfn);
        File f = new File(folderPath + "//key");
        if ((!f.exists()) || (!f.delete())) return false;
        f = new File(folderPath + "//iv");
        if ((!f.exists()) || (!f.delete())) return false;
        f = new File(folderPath + "//dn");
        if ((!f.exists()) || (!f.delete())) return false;
        f = new File(folderPath + "//voms");
        if ((!f.exists()) || (!f.delete())) return false;
        f = new File(folderPath);
        if ((!f.exists()) || (!f.delete())) return false;
        return true;
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

    /**
	  * The byte[] returned by MessageDigest does not have a nice
	  * textual representation, so some form of encoding is usually performed.
	  *
	  * This implementation follows the example of David Flanagan's book
	  * "Java In A Nutshell", and converts a byte array into a String
	  * of hex characters.
	  *
	  * Another popular alternative is to use a "Base64" encoding.
	  */
    static String hexEncode(byte[] aInput) {
        StringBuffer result = new StringBuffer();
        char[] digits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
        for (int idx = 0; idx < aInput.length; ++idx) {
            byte b = aInput[idx];
            result.append(digits[(b & 0xf0) >> 4]);
            result.append(digits[b & 0x0f]);
        }
        return result.toString();
    }
}
