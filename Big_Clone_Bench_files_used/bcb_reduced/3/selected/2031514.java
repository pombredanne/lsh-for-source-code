package au.csiro.atnf.rpfits.tools;

import java.io.File;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.zip.CRC32;
import au.csiro.atnf.rpfits.Project;

/**
 * Class describes a rpfits file.
 * 
 * Copyright 2010-2011, CSIRO Australia All rights reserved.
 */
public class RPFile {

    long fpos;

    byte[] fbuf;

    static final int FBLEN = 64000;

    int fbcnt;

    long fmax;

    long callCounter;

    String name;

    String shortName;

    ArrayList scans;

    String arrayConfig;

    private RandomAccessFile in;

    File file;

    RPScan lastScan;

    Project project;

    boolean eof;

    CRC32 crc32;

    MessageDigest md5;

    public static void main(String[] args) {
        try {
            RPFile file = new RPFile(args[0]);
            file.parse();
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public RPFile(String fileName) throws Exception {
        name = fileName;
        file = new File(fileName);
        scans = new ArrayList();
        eof = false;
        fpos = fmax = 0;
        fbcnt = 0;
        fbuf = new byte[FBLEN];
        shortName = file.getName();
        crc32 = new CRC32();
        md5 = MessageDigest.getInstance("MD5");
        arrayConfig = "Unknown";
    }

    public RPFile(File rpFile) throws Exception {
        file = rpFile;
        name = file.getName();
        scans = new ArrayList();
        eof = false;
        fpos = fmax = 0;
        fbcnt = 0;
        fbuf = new byte[FBLEN];
        shortName = file.getName();
        crc32 = new CRC32();
        md5 = MessageDigest.getInstance("MD5");
        arrayConfig = "Unknown";
    }

    public void parse() throws Exception {
        in = new RandomAccessFile(file, "r");
        RPScan scan;
        int scanNo = 0;
        while ((scan = RPScan.readScan(this)) != null) {
            scanNo++;
            lastScan = scan;
            scan.m_num = scanNo;
            scan.m_fileName = shortName;
            addScan(scan);
            if (eof) break;
        }
        project = createProject();
        if (!isATCA()) arrayConfig = project.getInstrument(); else arrayConfig = identifyArray();
    }

    Project createProject() {
        String code = "Unknown";
        int i = shortName.indexOf("_") + 6, j = i + 4;
        if (i > 6) {
            for (j = i; j < shortName.length(); j++) if (!Character.isLetter(shortName.charAt(j))) break;
            for (; j < shortName.length(); j++) if (!Character.isDigit(shortName.charAt(j))) break;
            code = shortName.substring(i, j);
        }
        String instrument = "Unknown";
        if (isMopra()) instrument = "Mopra"; else if (isParkes()) instrument = "Parkes"; else if (isATCA()) instrument = "ATCA";
        Project pr = new Project(code, instrument);
        return pr;
    }

    public String identifyArray() {
        for (int i = 0; i < getScansNum(); i++) {
            RPScan scan = getScan(i);
            arrayConfig = RPArrayConfig.classify_array(scan);
            if (!arrayConfig.equals("Unknown")) break;
        }
        return arrayConfig;
    }

    public File getFile() {
        return file;
    }

    public String getInstrument() {
        return project.getInstrument();
    }

    public RPScan getScan(int i) {
        return (RPScan) scans.get(i);
    }

    public RPScan getScanByNum(int i) {
        for (int j = 0; j < scans.size(); j++) {
            RPScan scan = (RPScan) scans.get(j);
            if (scan.m_num == i) return scan;
        }
        return null;
    }

    public int getScansNum() {
        return scans.size();
    }

    public void addScan(RPScan scan) {
        scans.add(scan);
    }

    public void addScan(int i, RPScan scan) {
        scans.add(i, scan);
    }

    boolean isATCA() {
        return isInstrument("atca");
    }

    boolean isMopra() {
        return isInstrument("mopra");
    }

    boolean isParkes() {
        return isInstrument("parkes");
    }

    boolean isInstrument(String name) {
        RPScan sc = getScan(0);
        if (sc == null) return false;
        RPCard card = sc.getCard("INSTRUME");
        if (card == null) return false;
        String inst = card.getValue().toLowerCase();
        if (inst.indexOf(name.toLowerCase()) >= 0) return true;
        return false;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList getScans() {
        return scans;
    }

    public void setScans(ArrayList scans) {
        this.scans = scans;
    }

    public Project getProject() {
        return project;
    }

    int read(byte[] arr) throws Exception {
        long rr = 0;
        if (callCounter >= 129) rr = in.getFilePointer();
        int rd = in.read(arr);
        if (rd > 0) {
            if (fpos + rd > fmax) {
                if (fbcnt > 0) {
                    update(fbuf, 0, fbcnt);
                }
                fbcnt = 0;
                int over = (int) (fpos + rd - fmax);
                update(arr, rd - over, over);
            }
            fpos += rd;
            if (fpos > fmax) fmax = fpos;
        }
        if (in.getFilePointer() != fpos) throw new Exception("fpos != getFilePointer()"); else callCounter++;
        return rd;
    }

    byte readByte() throws Exception {
        long rr = 0;
        if (callCounter >= 129) rr = in.getFilePointer();
        byte rc = in.readByte();
        fpos++;
        if (fpos > fmax) {
            if (fbcnt == FBLEN) {
                update(fbuf, 0, fbcnt);
                fbcnt = 0;
            }
            fbcnt++;
            fbuf[fbcnt - 1] = rc;
            fmax = fpos;
        }
        if (in.getFilePointer() != fpos) throw new Exception("fpos != getFilePointer()"); else callCounter++;
        return rc;
    }

    long getFilePointer() throws Exception {
        if (in.getFilePointer() != fpos) throw new Exception("fpos != getFilePointer()"); else callCounter++;
        return fpos;
    }

    void seek(long pos) throws Exception {
        if (pos <= fmax) {
            fpos = pos;
            in.seek(pos);
            long rr = 0;
            if (callCounter >= 129) rr = in.getFilePointer();
            if (in.getFilePointer() != fpos) throw new Exception("fpos != getFilePointer()"); else callCounter++;
            return;
        }
        in.seek(fmax);
        fpos = fmax;
        long haveRead = 0;
        if (callCounter >= 129) haveRead = in.getFilePointer();
        long toRead = pos - fmax;
        do {
            if (fbcnt > 0) {
                update(fbuf, 0, fbcnt);
            }
            fbcnt = 0;
            int portion = fbuf.length;
            if (portion > toRead) portion = (int) toRead;
            toRead -= portion;
            fbcnt = in.read(fbuf, 0, portion);
            if (fbcnt > 0) fpos += fbcnt;
        } while (toRead > 0 && fbcnt > 0);
        fmax = fpos;
        if (fbcnt < 0) fbcnt = 0;
        if (in.getFilePointer() != fpos) throw new Exception("fpos != getFilePointer()"); else callCounter++;
    }

    int skipBytes(int num) throws Exception {
        long rr = 0;
        if (callCounter >= 129) rr = in.getFilePointer();
        long nextpos = fpos + num, curpos = fpos;
        seek(nextpos);
        if (nextpos != fpos) return (int) (fpos - curpos);
        if (in.getFilePointer() != fpos) throw new Exception("fpos != getFilePointer()"); else callCounter++;
        return num;
    }

    void update(byte[] arr, int off, int len) {
        crc32.update(arr, off, len);
        md5.update(arr, off, len);
    }

    public String getCRC32() {
        if (fbcnt > 0) update(fbuf, 0, fbcnt);
        fbcnt = 0;
        return "" + crc32.getValue();
    }

    public String getMD5() {
        if (fbcnt > 0) update(fbuf, 0, fbcnt);
        fbcnt = 0;
        byte[] data = md5.digest();
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9)) buf.append((char) ('0' + halfbyte)); else buf.append((char) ('a' + (halfbyte - 10)));
                halfbyte = data[i] & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }

    public void close() {
        try {
            if (in != null) in.close();
        } catch (Exception e) {
            in = null;
        }
    }

    public String getArrayConfig() {
        return arrayConfig;
    }
}
