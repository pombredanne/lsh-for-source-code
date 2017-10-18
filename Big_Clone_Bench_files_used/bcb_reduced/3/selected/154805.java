package org.fao.waicent.kids.giews.communication.providermodule;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.fao.waicent.db.dbConnectionManager;
import org.fao.waicent.db.dbConnectionManagerPool;
import org.fao.waicent.kids.giews.communication.utility.GIEWSException;
import org.fao.waicent.kids.giews.communication.utility.Resources;

/**
 * <p>Title: ExportProject</p>
 *
 * </p>
 *
 * @author A. Tamburo
 * @version 1, last modified by A. Tamburo, 15/11/05
 */
public class ExportUtility {

    String database_ini;

    public ExportUtility(String db_ini) {
        this.database_ini = db_ini;
    }

    /**
     * exportProject
     *
     * @version 1, last modified by A. Tamburo, 20/10/05
    */
    public static int exportProject(String urlPath, int gaul_code, File file) {
        try {
            String pathEnc = URLEncoder.encode(file.getParent() + "," + file.getName(), "UTF-8");
            String tmp = urlPath + "?what=GIEWS_EXPORT_PROJECTS&setting=true," + pathEnc + "," + gaul_code;
            URL u = new URL(tmp);
            u.getHost();
            if (file.exists()) return (int) file.length();
            return -1;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * exportLayer
     *
     * @version 1, last modified by A. Tamburo, 05/09/06
    */
    public synchronized int exportLayer(String homePath, File file, byte type, int layerID) {
        String dirTo = "" + file.getParent();
        String filename2 = "" + file.getName();
        String query = "";
        String nameCol = "";
        String idCol = "";
        if (type == Resources.RASTER_LAYER) {
            query = "select Raster_Path from rasterlayer where Raster_ID=" + layerID;
            nameCol = "Raster_Path";
        } else if (type == Resources.FEATURE_LAYER) {
            query = "select Feature_Path from featurelayer where Feature_ID=" + layerID;
            nameCol = "Feature_Path";
        } else return -1;
        Statement stmt = null;
        ResultSet rs = null;
        Connection con = popConnection();
        String path = null;
        try {
            stmt = con.createStatement();
            rs = stmt.executeQuery(query);
            int id;
            if (rs.next()) {
                path = rs.getString(nameCol);
            }
        } catch (Exception e) {
            e.printStackTrace();
            pushConnection(con);
            return -1;
        } finally {
            pushConnection(con);
        }
        if (path != null) {
            String dir = "";
            if (type == Resources.RASTER_LAYER) {
                dir += path;
            } else {
                dir += homePath;
                dir = dir.substring(0, dir.lastIndexOf(File.separator));
                dir = dir.substring(0, dir.lastIndexOf(File.separator)) + File.separator + path;
            }
            String filenameIn = dir.substring(0, dir.lastIndexOf("."));
            File fileIn = new File(dir);
            File dirTof = new File(dirTo);
            if (!dirTof.exists()) {
                dirTof.mkdir();
            }
            String outFilename = dirTo + File.separator + filename2;
            ZipOutputStream out = null;
            try {
                out = new ZipOutputStream(new FileOutputStream(outFilename));
            } catch (FileNotFoundException e) {
                return -1;
            }
            String ext = dir.substring(dir.lastIndexOf("."));
            try {
                if (ext.compareToIgnoreCase(".shp") == 0) {
                    this.addZipFile(filenameIn + ext, out);
                    this.addZipFile(filenameIn + ".shx", out);
                    this.addZipFile(filenameIn + ".dbf", out);
                } else if (ext.compareToIgnoreCase(".mif") == 0) {
                    this.addZipFile(filenameIn + ext, out);
                    this.addZipFile(filenameIn + ".mid", out);
                } else {
                    this.addZipFile(filenameIn + ext, out);
                }
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
                File f = new File(outFilename);
                f.delete();
                return -1;
            }
        }
        if (file.exists()) return (int) file.length();
        return -1;
    }

    /**
    * exportDataset
    *
    * @version 1, last modified by A. Tamburo, 12/01/06
    */
    public static int exportDataset(String urlPath, File file, int ID) {
        try {
            String pathEnc = URLEncoder.encode(file.getParent() + "," + file.getName(), "UTF-8");
            String tmp = urlPath + "?what=GIEWS_EXPORT_DATASETS&setting=" + ID + "," + pathEnc;
            URL u = new URL(tmp);
            u.getContent();
            if (file.exists()) return (int) file.length();
            return -1;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
    * generateDigest
    *
    * @version 1, last modified by A. Tamburo, 12/01/06
    */
    public static byte[] generateDigest(String filename) throws GIEWSException {
        File file = new File(filename);
        if (!file.exists()) {
            throw new GIEWSException("file not found");
        }
        byte[] toChapter1Digest = null;
        try {
            FileInputStream fileStream = new FileInputStream(file);
            ZipInputStream zin = new ZipInputStream(fileStream);
            MessageDigest md = MessageDigest.getInstance("SHA");
            int byteRead = 1;
            byte buffer[] = new byte[1000];
            ZipEntry ze = zin.getNextEntry();
            md = MessageDigest.getInstance("SHA");
            while (ze != null) {
                byteRead = 1;
                if (ze.getName() == "Projects.xml") {
                    while (byteRead > 0) {
                        byteRead = zin.read(buffer);
                        md.update(buffer);
                    }
                }
                zin.closeEntry();
                ze = zin.getNextEntry();
            }
            toChapter1Digest = md.digest();
            fileStream.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw new GIEWSException(e.getMessage());
        }
        return toChapter1Digest;
    }

    /**
        * popConnection
        *
        * @return Connection
        * @version 1, last modified by A. Tamburo, 16/12/05
        */
    private Connection popConnection() {
        dbConnectionManager manager = dbConnectionManagerPool.getConnectionManager(database_ini);
        Connection con = manager.popConnection();
        return con;
    }

    /**
        * pushConnection
        *
        * @param Connection
        * @version 1, last modified by A. Tamburo, 16/12/05
    */
    private void pushConnection(Connection con) {
        dbConnectionManager manager = dbConnectionManagerPool.getConnectionManager(database_ini);
        manager.pushConnection(con);
    }

    private void addZipFile(String filenameIn, ZipOutputStream out) throws FileNotFoundException, IOException {
        byte[] buf = new byte[1024];
        File fileIn = new File(filenameIn);
        out.putNextEntry(new ZipEntry(fileIn.getName()));
        FileInputStream in = new FileInputStream(fileIn);
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        out.closeEntry();
        in.close();
    }
}
