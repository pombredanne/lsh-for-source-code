package org.jdonkey.proto;

import java.io.*;
import java.util.*;

/**
 * @author Pola
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class KnownFile extends Observable {

    String fileName;

    long fileSize;

    long fileType;

    ArrayList hashList;

    ArrayList tagList;

    String directory;

    byte fileHash[];

    long date;

    /**
	* CKnownFile
	*/
    public KnownFile() {
        fileType = 2;
        fileSize = 0;
        date = 0;
        hashList = new ArrayList();
        tagList = new ArrayList();
        fileHash = new byte[16];
    }

    /**
	* CreateFromFile
	* @param inDirectory
	* @param inFileName
	* @return bool
	*/
    public boolean createFromFile(String inDirectory, String inFileName) {
        directory = inDirectory;
        String nameBuffer = inDirectory + File.separator + inFileName;
        File file = new File(nameBuffer);
        fileName = inFileName;
        fileSize = file.length();
        InputStream is;
        try {
            is = new FileInputStream(file);
        } catch (FileNotFoundException fnfe) {
            return false;
        }
        long toGo = fileSize;
        int hashCount;
        for (hashCount = 0; toGo >= Constants.PARTSIZE; ) {
            hashList.add(createHashFromFile(is, Constants.PARTSIZE));
            toGo -= Constants.PARTSIZE;
            hashCount++;
        }
        byte[] lastHash = createHashFromFile(is, toGo);
        if (hashCount == 0) fileHash = lastHash; else {
            hashList.add(lastHash);
            byte buffer[] = new byte[hashList.size() * 16];
            for (int i = 0; i < hashList.size(); i++) System.arraycopy(hashList.get(i), 0, buffer, i * 16, 16);
            fileHash = createHashFromString(buffer, buffer.length);
        }
        date = file.lastModified();
        return true;
    }

    /**
	* GetFileTypePtr
	* @return long *
	*/
    public long getFileType() {
        return fileType;
    }

    /**
	* GetPath
	* @return char *
	*/
    public String getPath() {
        return directory;
    }

    /**
	* SetPath
	* @param path
	*/
    public void setPath(String path) {
        directory = path;
    }

    /**
	* IsPartFile
	* @return bool
	*/
    public boolean isPartFile() {
        return false;
    }

    /**
	* LoadFromFile
	* @param file
	* @return bool
	*/
    public boolean loadFromFile(InputStream is) {
        return (loadDateFromFile(is) && loadHashsetFromFile(is, false) && loadTagsFromFile(is));
    }

    /**
	* WriteToFile
	* @param file
	* @return bool
	*/
    public boolean writeToFile(OutputStream os) {
        try {
            FileUtil.addUInt32(os, date);
            FileUtil.addBytes(os, fileHash);
            int parts = hashList.size();
            FileUtil.addUInt16(os, parts);
            for (Iterator i = hashList.iterator(); i.hasNext(); ) FileUtil.addBytes(os, (byte[]) i.next());
            long tagCount = tagList.size() + 5;
            FileUtil.addUInt32(os, tagCount);
            TagBean nameTag = new TagBean(TagBean.TAG_ST_FILENAME, fileName);
            nameTag.writeTagToFile(os);
            TagBean sizeTag = new TagBean(TagBean.TAG_ST_FILESIZE, fileSize);
            sizeTag.writeTagToFile(os);
            TagBean attag1 = new TagBean(TagBean.TAG_ST_ATTRANSFERED, 0);
            attag1.writeTagToFile(os);
            TagBean attag2 = new TagBean(TagBean.TAG_ST_ATREQUESTED, 0);
            attag2.writeTagToFile(os);
            TagBean attag3 = new TagBean(TagBean.TAG_ST_ATACCEPTED, 0);
            attag3.writeTagToFile(os);
            for (int j = 0; j < tagCount - 5; j++) {
                TagBean tag = (TagBean) tagList.get(j);
                tag.writeTagToFile(os);
            }
            return true;
        } catch (IOException ioe) {
            return false;
        }
    }

    /**
	* GetFileName
	* @return char *
	*/
    public String getFileName() {
        return fileName;
    }

    /**
	* GetFileHash
	* @return uchar *
	*/
    public byte[] getFileHash() {
        return fileHash;
    }

    /**
	* GetFileSize
	* @return long
	*/
    public long getFileSize() {
        return fileSize;
    }

    /**
	* GetFileDate
	* @return long
	*/
    public long getFileDate() {
        return date;
    }

    /**
	* GetHashCount
	* @return int
	*/
    public int getHashCount() {
        return hashList.size();
    }

    /**
	* GetPartHash
	* @param part
	* @return uchar*
	*/
    public byte[] getPartHash(int part) {
        if (part >= hashList.size()) return null;
        return (byte[]) hashList.get(part);
    }

    /**
	* GetPartCount
	* @return int
	*/
    public int getPartCount() {
        int result = (int) (fileSize / Constants.PARTSIZE);
        if (fileSize % Constants.PARTSIZE > 0) result++;
        return result;
    }

    /**
	* LoadHashsetFromFile
	* @param file
	* @param checkHash
	* @return bool
	*/
    public boolean loadHashsetFromFile(InputStream is, boolean checkHash) {
        try {
            byte[] checkId = new byte[16];
            is.read(checkId);
            int parts = FileUtil.readUInt16(is);
            for (int i = 0; i < parts; i++) {
                byte[] curHash = new byte[16];
                is.read(curHash);
                hashList.add(curHash);
            }
            fileHash = checkId;
            if (!checkHash) return true;
            if (!hashList.isEmpty()) {
                byte[] buffer = new byte[hashList.size() * 16];
                for (int i = 0; i != hashList.size(); i++) System.arraycopy(hashList.get(i), 0, buffer, i * 16, 16);
                checkId = createHashFromString(buffer, buffer.length);
            }
            if (checkId.equals(fileHash)) return true; else {
                hashList = new ArrayList();
                return false;
            }
        } catch (IOException ioe) {
            return false;
        }
    }

    /**
	* LoadTagsFromFile
	* @param file
	* @return bool
	*/
    protected boolean loadTagsFromFile(InputStream is) {
        try {
            long tagCount;
            tagCount = FileUtil.readUInt32(is);
            for (long j = 0; j < tagCount; j++) {
                TagBean newTag = new TagBean(is);
                switch(newTag.getSpecialTag()) {
                    case TagBean.TAG_ST_FILENAME:
                        fileName = newTag.getStringValue();
                        break;
                    case TagBean.TAG_ST_FILESIZE:
                        fileSize = newTag.getIntValue();
                        break;
                    default:
                        tagList.add(newTag);
                }
            }
            return true;
        } catch (IOException ioe) {
            return false;
        }
    }

    /**
	* LoadDateFromFile
	* @param file
	* @return bool
	*/
    protected boolean loadDateFromFile(InputStream is) {
        try {
            date = FileUtil.readUInt32(is);
            return true;
        } catch (IOException ioe) {
            return false;
        }
    }

    /**
	* CreateHashFromFile
	* @param file
	* @param Length
	* @param Output
	*/
    protected byte[] createHashFromFile(File file, long length) {
        InputStream is = null;
        try {
            is = new FileInputStream(file);
            byte[] result = createHashFromFile(is, length);
            is.close();
            return result;
        } catch (IOException ioe) {
            if (is != null) try {
                is.close();
            } catch (IOException ioe2) {
            }
            return null;
        }
    }

    /**
	* CreateHashFromFile
	* @param file
	* @param Length
	* @param Output
	*/
    protected byte[] createHashFromFile(InputStream is, long length) {
        try {
            byte[] buffer = new byte[1024];
            MD4 md4 = new MD4();
            for (long chunks = length / buffer.length; chunks > 0; chunks--) {
                is.read(buffer);
                md4.update(buffer, 0, buffer.length);
            }
            is.read(buffer, 0, ((int) length % buffer.length));
            md4.update(buffer, 0, ((int) length % buffer.length));
            return md4.digest();
        } catch (IOException ioe) {
            return null;
        }
    }

    /**
	* CreateHashFromFile
	* @param file
	* @param Length
	* @param Output
	*/
    protected byte[] createHashFromFile(RandomAccessFile file, long length) {
        try {
            byte[] buffer = new byte[1024];
            MD4 md4 = new MD4();
            for (long chunks = length / buffer.length; chunks > 0; chunks--) {
                file.readFully(buffer);
                md4.update(buffer, 0, buffer.length);
            }
            file.readFully(buffer, 0, ((int) length % buffer.length));
            md4.update(buffer, 0, ((int) length % buffer.length));
            return md4.digest();
        } catch (IOException ioe) {
            return null;
        }
    }

    /**
	* CreateHashFromString
	* @param in_string
	* @param Length
	* @param Output
	*/
    protected byte[] createHashFromString(byte[] buffer, long length) {
        MD4 md4 = new MD4();
        md4.update(buffer, 0, (int) length);
        return md4.digest();
    }

    public String toString() {
        StringBuffer stBuffer = new StringBuffer();
        stBuffer.append("ed2k_hash\n\nfilesize: ");
        stBuffer.append(fileSize);
        stBuffer.append(" bytes = ");
        stBuffer.append(hashList.size());
        stBuffer.append(" partial hashes\n\n");
        for (int i = 0; i < hashList.size(); i++) {
            byte[] hash = (byte[]) hashList.get(i);
            stBuffer.append("Block ");
            stBuffer.append(i);
            stBuffer.append(": ");
            stBuffer.append(hash2String(hash));
            stBuffer.append('\n');
        }
        stBuffer.append('\n');
        stBuffer.append("final donkey hash is ");
        stBuffer.append(hash2String(fileHash));
        stBuffer.append("\n\ned2k://|file|");
        stBuffer.append(fileName);
        stBuffer.append('|');
        stBuffer.append(fileSize);
        stBuffer.append('|');
        stBuffer.append(hash2String(fileHash));
        stBuffer.append('|');
        return stBuffer.toString();
    }

    private String hash2String(byte[] hash) {
        char idstrtmp[] = new char[32];
        for (int i = 0; i < hash.length; i++) {
            int m = hash[i] & 0xFF;
            int j = m % 16;
            int k = (m - j) / 16;
            idstrtmp[i * 2] = Character.forDigit(k, 16);
            idstrtmp[i * 2 + 1] = Character.forDigit(j, 16);
        }
        return new String(idstrtmp);
    }
}
