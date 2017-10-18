package net.mjrz.fm.actions;

import java.io.*;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.*;
import org.apache.log4j.Logger;

/**
 * @author Mjrz contact@mjrz.net
 * 
 */
public class UpgradeAction {

    public static final String UPGRADE_LOCATION_URL = "http://localhost/dist/flist.txt";

    static final char DELIM = '^';

    private static Logger logger = Logger.getLogger(UpgradeAction.class.getName());

    public UpgradeAction() {
    }

    public ArrayList<String[]> getFileList() throws Exception {
        ArrayList<String[]> ret = new ArrayList<String[]>();
        URL url = new URL(UPGRADE_LOCATION_URL);
        System.out.println("md5URL: " + url);
        BufferedReader in = null;
        try {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            int status = conn.getResponseCode();
            if (status == 200) {
                in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while (true) {
                    String s = in.readLine();
                    if (s == null) break;
                    System.out.println(s);
                    int pos = s.indexOf(DELIM);
                    if (pos >= 0) {
                        int npos = s.indexOf(DELIM, pos + 1);
                        if (npos < 0) continue;
                        String name = s.substring(0, pos);
                        String loc = s.substring(pos + 1, npos);
                        String md5 = s.substring(npos + 1);
                        System.out.println("*" + name + ":" + loc + ":" + md5);
                        String line[] = { name, loc, md5 };
                        ret.add(line);
                    }
                }
            }
            return ret;
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    public ActionResponse executeAction(ActionRequest request) throws Exception {
        ActionResponse resp = new ActionResponse();
        ArrayList<String[]> flist = getFileList();
        downloadFiles(flist, resp);
        if (resp.getErrorCode() == ActionResponse.NOERROR) {
            upgrade(flist, resp);
        } else {
            cleanup(flist);
        }
        return resp;
    }

    private void downloadFiles(ArrayList<String[]> fileList, ActionResponse resp) {
        String dir = System.getProperty("user.dir");
        File f = new File(dir);
        System.out.println("Installation directory: " + f.getAbsolutePath());
        try {
            for (String[] line : fileList) {
                System.out.println("line[1]" + line[1]);
                URL url = new URL(line[1]);
                System.out.println("URL:" + url);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                int status = conn.getResponseCode();
                if (status == 200) {
                    InputStream in = conn.getInputStream();
                    File outfile = new File(line[0] + ".dnld");
                    BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outfile));
                    System.out.println("Writing to file..." + outfile.getAbsolutePath());
                    int bytes = 0;
                    while (true) {
                        int c = in.read();
                        if (c < 0) break;
                        out.write(c);
                        bytes++;
                    }
                    out.close();
                    String local = getMD5Sum(outfile);
                    System.out.println("Bytes written: " + bytes);
                    if (local.equals(line[2])) {
                        System.out.println("MD5Sums match...");
                    } else {
                        resp.setErrorCode(ActionResponse.GENERAL_ERROR);
                        resp.setErrorMessage("Unable to validate all files. Please upgrade manually.");
                        break;
                    }
                } else {
                    resp.setErrorCode(ActionResponse.GENERAL_ERROR);
                    resp.setErrorMessage("HTTP Error [" + status + "]");
                    break;
                }
            }
        } catch (Exception e) {
            resp.setErrorCode(ActionResponse.GENERAL_ERROR);
            resp.setErrorMessage("Exception occured: " + e.getMessage());
        }
    }

    private void upgrade(ArrayList<String[]> fileList, ActionResponse resp) {
        for (String[] line : fileList) {
            String fname = line[0];
            System.out.println("Upgrading file: " + fname);
            File src = new File(fname + ".dnld");
            File dest = new File(fname);
            FileInputStream fis = null;
            FileOutputStream fos = null;
            try {
                fis = new FileInputStream(src);
                fos = new FileOutputStream(dest);
                while (true) {
                    int c = fis.read();
                    if (c == -1) break;
                    fos.write(c);
                }
                System.out.println("Upgrade success..." + fname);
                fis.close();
                boolean delete = src.delete();
                System.out.println("Delete source file suceeded? " + delete);
            } catch (Exception e) {
                logger.error(net.mjrz.fm.utils.MiscUtils.stackTrace2String(e));
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (Exception e) {
                    }
                }
            }
        }
    }

    private void cleanup(ArrayList<String[]> flist) throws Exception {
    }

    private String getMD5Sum(File f) {
        InputStream is = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            is = new FileInputStream(f);
            byte[] buffer = new byte[8192];
            int read = 0;
            while ((read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            byte[] md5sum = digest.digest();
            BigInteger bigInt = new BigInteger(1, md5sum);
            String output = bigInt.toString(16);
            System.out.println("MD5: " + output);
            return output;
        } catch (Exception e) {
            throw new RuntimeException("Unable to process file for MD5", e);
        } finally {
            try {
                if (is != null) is.close();
            } catch (IOException e) {
                throw new RuntimeException("Unable to close input stream for MD5 calculation", e);
            }
        }
    }

    public static void main(String args[]) throws Exception {
        UpgradeAction a = new UpgradeAction();
        ActionRequest req = new ActionRequest();
        a.executeAction(req);
    }
}
