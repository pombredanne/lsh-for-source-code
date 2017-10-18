package jp.locky.locdisp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.placelab.spotter.SpotterException;
import org.placelab.spotter.WiFiSpotter;
import jp.locky.util.LockyCode;
import jp.locky.util.WiFiData;

public class ConvertSortedWLDB {

    /** Default WiFi Location Database */
    public static final String DEFAULT_DB_FILENAME = "." + File.separator + "data" + File.separator + "default.wldb";

    /** WiFi Location DB��1���R�[�h�̃T�C�Y�i�P�ʁFbyte�j */
    private static final int WLDB_RECORD_SIZE = 16;

    /** WiFi Location DB��1���R�[�h��ID�̃T�C�Y�i�P�ʁFbyte�j */
    private static final int WLDB_ID_SIZE = 8;

    private static int byteToUInt(byte b) {
        int i = b;
        return (i < 0) ? (i + 256) : i;
    }

    public void openDB(InputStream dbFile) throws IOException, SpotterException {
        DataInputStream dis = new DataInputStream(dbFile);
        byte[] bytes = new byte[WLDB_RECORD_SIZE];
        while ((dis.read(bytes)) != -1) {
            byte[] hashedID = new byte[WLDB_ID_SIZE];
            byte[] lockyCode = new byte[LockyCode.SIZE];
            for (int i = 0; i < WLDB_ID_SIZE; i++) {
                hashedID[i] = bytes[i];
            }
            for (int i = 0; i < LockyCode.SIZE; i++) {
                lockyCode[i] = bytes[WLDB_ID_SIZE + i];
            }
        }
        dis.close();
    }

    /**
	 * BSSID��ESSID�̃n�b�V����Ԃ�
	 * @param ID �n�b�V����������O��ID
	 * @return �n�b�V�����ID
	 */
    public static byte[] getHashedID(String ID) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.reset();
            md5.update(ID.getBytes());
            byte[] digest = md5.digest();
            byte[] bytes = new byte[WLDB_ID_SIZE];
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = digest[i];
            }
            return bytes;
        } catch (NoSuchAlgorithmException exception) {
            System.err.println("Java VM is not compatible");
            return null;
        }
    }

    public void createDBFile(File fileDirectory, File targetFile) {
        try {
            File[] files = fileDirectory.listFiles();
            WiFiData[] wifi = WiFiData.analyzeLogFile(files, false);
            DataOutputStream dos = new DataOutputStream(new FileOutputStream(targetFile, false));
            for (int i = 0; i < wifi.length; i++) {
                LockyCode code = new LockyCode(wifi[i].LATITUDE, wifi[i].LONGITUDE);
                String idstr = wifi[i].BSSID + wifi[i].SSID;
                byte[] hashedID = getHashedID(idstr);
                for (int j = 0; j < hashedID.length; j++) {
                    dos.writeByte(byteToUInt(hashedID[j]));
                }
                byte[] lockyCode = code.toByteArray();
                for (int j = 0; j < lockyCode.length; j++) {
                    dos.writeByte(byteToUInt(lockyCode[j]));
                }
                dos.flush();
            }
            dos.close();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public static void main(String args[]) {
        ConvertSortedWLDB cwdb = new ConvertSortedWLDB();
    }
}
