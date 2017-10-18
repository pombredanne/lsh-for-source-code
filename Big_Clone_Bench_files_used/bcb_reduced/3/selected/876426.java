package jp.locky.toolkit;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Hashtable;
import jp.locky.util.LockyCode;
import jp.locky.util.WiFiData;

/**
 * WiFi Location DB��p�����ʒu���胉�C�u����
 *    for PlaceEngine Client
 * 
 * @author Hiroshi Yoshida and Nobuo Kawaguchi
 */
public class LockyToolkitPE {

    /** Default WiFi Location Database */
    public static final String DEFAULT_DB_FILENAME = "." + File.separator + "data" + File.separator + "default.wldb";

    /** WiFi Location DB��1���R�[�h�̃T�C�Y�i�P�ʁFbyte�j */
    private static final int WLDB_RECORD_SIZE = 16;

    /** WiFi Location DB��1���R�[�h��ID�̃T�C�Y�i�P�ʁFbyte�j */
    private static final int WLDB_ID_SIZE = 8;

    /**
	 * Key: Hashed ID
	 * <br>Value: LockyCode
	 */
    private Hashtable<HashID, byte[]> wifiTable = new Hashtable<HashID, byte[]>();

    private PlaceEngineClient peClient;

    private WiFiData[] lastData;

    private int lastNumber_;

    private static class HashID {

        private byte[] key_;

        public HashID(byte[] key) {
            super();
            key_ = key;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof HashID) {
                HashID rhs = (HashID) obj;
                return Arrays.equals(key_, rhs.key_);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(key_);
        }

        public String toString() {
            return hexdump(key_);
        }
    }

    private static int byteToUInt(byte b) {
        int i = b;
        return (i < 0) ? (i + 256) : i;
    }

    private static String hexdump(byte[] bar) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < bar.length; ++i) {
            if (i != 0) sb.append(",");
            String s = Integer.toHexString(byteToUInt(bar[i]));
            if (s.length() == 1) sb.append("0");
            sb.append(s);
        }
        return sb.toString();
    }

    /**
	 * �����ŌĂяo�����R���X�g���N�^
	 * <br>GUI�����ŏ���
	 */
    public LockyToolkitPE() {
    }

    public void exit() {
        System.exit(0);
    }

    /**
	 * BSSID��ESSID�̃n�b�V����Ԃ�
	 * @param ID �n�b�V����������O��ID
	 * @return �n�b�V�����ID
	 */
    protected byte[] getHashedID(String ID) {
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
            exit();
            return null;
        }
    }

    /**
	 * �Ō�̐���Ɏg�p��������LAN�����擾����
	 * @return jp.locky.util.WiFiData�^�z��
	 */
    public WiFiData[] getLastData() {
        return lastData;
    }

    /**
	 * ���ݒn��\��LockyCode��Ԃ��܂��D������Ȃ��Ƃ���null��Ԃ��܂��D
	 * @return ���ݒn��\��LockyCode
	 * @throws SpotterException 
	 */
    public LockyCode getLockyCode() {
        peClient.getMeasurement();
        lastNumber_ = peClient.numberOfReadings();
        if (lastNumber_ < 0) {
            System.err.println("Please Install PlaceEngine Client");
            lastData = null;
            return null;
        } else if (lastNumber_ == 0) {
            lastData = null;
            return null;
        }
        WiFiData[] wifiData = peClient.getWiFiData();
        LockyCode lc = null;
        for (int i = 0; i < lastNumber_; i++) {
            String idstr = wifiData[i].BSSID + wifiData[i].SSID;
            HashID hashedID = new HashID(getHashedID(idstr));
            if (wifiTable.containsKey(hashedID)) {
                byte[] code = wifiTable.get(hashedID);
                lc = new LockyCode(code);
                wifiData[i].LATITUDE = lc.getLatitude();
                wifiData[i].LONGITUDE = lc.getLongitude();
                wifiData[i].HEIGHT = lc.getHeight();
                wifiData[i].AREA = lc.getArea();
            }
        }
        lastData = wifiData;
        wifiData = WiFiData.sort(wifiData);
        for (int i = 0; i < wifiData.length; i++) {
            if (wifiData[i].LATITUDE != 0.0) {
                return new LockyCode(wifiData[i].LATITUDE, wifiData[i].LONGITUDE, wifiData[i].HEIGHT, wifiData[i].AREA);
            }
        }
        return null;
    }

    /**
	 * �W���ݒ肳�ꂽ�f�[�^�x�[�X�t�@�C�����J��
	 * @throws FileNotFoundException 
	 */
    public void openDB() throws FileNotFoundException {
        try {
            openDB(new FileInputStream(DEFAULT_DB_FILENAME));
        } catch (IOException ex) {
            throw new FileNotFoundException(DEFAULT_DB_FILENAME);
        }
    }

    /**
     * �f�[�^�x�[�X�t�@�C�����w�肵�ĊJ��
     * <br>WiFi, DB�̉��ꂩ�̏���Ɏ��s�����ꍇ�̓V�X�e�����I��������
     * @throws SpotterException 
     */
    public void openDB(InputStream dbFile) throws IOException {
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
            wifiTable.put(new HashID(hashedID), lockyCode);
        }
        dis.close();
        peClient = new PlaceEngineClient();
    }

    /**
	 * ���݊ϑ����Ă���WiFi�̐�
	 * @return WiFi�̊ϑ���
	 */
    public int numberOfReadings() {
        peClient.getMeasurement();
        return peClient.numberOfReadings();
    }

    public void createDBFile(String fileDirectory, File targetFile) {
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter("wldbData.txt"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        AllFileListSearch afls = new AllFileListSearch();
        try {
            File[] files = afls.listFiles(fileDirectory, "*.log");
            DataOutputStream dos = new DataOutputStream(new FileOutputStream(targetFile, false));
            WiFiData[] wifi = WiFiData.analyzeLogFile(files, false);
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
            bw.write("bssid_number :" + wifi.length);
            bw.newLine();
            int bytesize = dos.size();
            int megabytesize = bytesize / (1024 * 1024);
            dos.close();
            bw.write("data_size :" + megabytesize);
            bw.newLine();
            bw.close();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public void createDBFile(File fileDirectory, File targetFile) {
        try {
            File[] files = fileDirectory.listFiles();
            WiFiData[] wifi = WiFiData.analyzeLogFile(files, false);
            DataOutputStream dos = new DataOutputStream(new FileOutputStream(targetFile, false));
            for (int i = 0; i < wifi.length; i++) {
                LockyCode code = new LockyCode(wifi[i].LATITUDE, wifi[i].LONGITUDE);
                if (code.getLatitude() == wifi[i].LATITUDE) {
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
            }
            dos.close();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        new LockyToolkitPE().createDBFile("D:\\locky\\home", new File("C:\\Program Files\\Apache Group\\Tomcat 4.1\\webapps\\member\\default.wldb"));
    }

    /** getWiFiTable
	 *  returns WiFi Location Database
	 * @return Hashtable<HashID, byte[]>  wifiTable
	 * 
	 */
    public Hashtable<HashID, byte[]> getWiFiTable() {
        return wifiTable;
    }

    public int getLastNumber() {
        return lastNumber_;
    }
}
