package uk.ac.ebi.mg.xchg.upload;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import com.pri.adob.ADOB;
import com.pri.util.ProgressListener;
import com.pri.util.stream.StreamPump;

public class UploadCore implements Recipient {

    private File workDir;

    private Map<String, TransactionInfo> tranz;

    public UploadCore(File wd) {
        workDir = wd;
        workDir.mkdirs();
        tranz = new TreeMap<String, TransactionInfo>();
        File[] tds = workDir.listFiles();
        for (File td : tds) {
            File ti = new File(td, Constants.metaFileName);
            if (ti.exists()) {
                try {
                    ObjectInputStream ois = new ObjectInputStream(new FileInputStream(ti));
                    TransactionInfo tidata = (TransactionInfo) ois.readObject();
                    ois.close();
                    tranz.put(td.getName(), tidata);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public boolean closeTransaction(String key, boolean commit) throws UploadException {
        TransactionInfo ti = tranz.get(key);
        if (ti == null) return false;
        File td = new File(workDir, key);
        if (!commit) {
            File[] files = td.listFiles();
            for (File f : files) f.delete();
            td.delete();
            return true;
        } else {
            for (Map.Entry<String, FileInfo> me : ti.getFileInfos().entrySet()) {
                if (me.getValue().getStatus() == FileInfo.STATUS.ASSEMBLED) continue;
                if (me.getValue().getStatus() == FileInfo.STATUS.CHUNKED) throw new UploadException(me.getValue().getName(), UploadException.FILE_INCOMPLETE);
                File outFile = new File(workDir, key + File.separator + me.getValue().getName());
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(outFile);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                Chunk prev = null;
                try {
                    for (Chunk ch : me.getValue().getChunks()) {
                        File chunkFile = getChunkFile(key, me.getValue().getName(), ch.getBegin(), ch.getEnd() - ch.getBegin(), ch.getID());
                        FileInputStream fis = new FileInputStream(chunkFile);
                        if (prev != null && prev.getEnd() > ch.getBegin()) fis.skip(prev.getEnd() - ch.getBegin());
                        StreamPump.doPump(fis, fos, false);
                        prev = ch;
                    }
                    fos.close();
                } catch (IOException e) {
                    try {
                        fos.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    outFile.delete();
                    throw new UploadException("Remote IO error", e, UploadException.IOError);
                }
                me.getValue().setStatus(FileInfo.STATUS.ASSEMBLED);
                for (Chunk ch : me.getValue().getChunks()) {
                    File chunkFile = getChunkFile(key, me.getValue().getName(), ch.getBegin(), ch.getEnd() - ch.getBegin(), ch.getID());
                    chunkFile.delete();
                }
                me.getValue().getChunks().clear();
            }
            tranz.remove(key);
            new File(workDir, key + File.separator + Constants.metaFileName).delete();
            return true;
        }
    }

    public boolean createTransaction(String key) {
        if (tranz.containsKey(key)) return false;
        tranz.put(key, new TransactionInfo());
        new File(workDir, key).mkdir();
        return true;
    }

    public void deleteChunk(String key, String fileKey, int chunkID) {
        TransactionInfo ti = tranz.get(key);
        Iterator<Chunk> it = ti.getFileInfo(fileKey).getChunks().iterator();
        while (it.hasNext()) {
            Chunk ch = it.next();
            if (chunkID == ch.getID()) {
                it.remove();
                File chFile = getChunkFile(key, fileKey, ch.getBegin(), ch.getEnd() - ch.getBegin(), ch.getID());
                chFile.delete();
                return;
            }
        }
    }

    public byte[] getFileSum(String key, String fileKey, long fileSize) throws UploadException {
        TransactionInfo ti = tranz.get(key);
        if (ti == null) {
            return null;
        }
        FileInfo fi = ti.getFileInfo(fileKey);
        if (fi == null) {
            return null;
        }
        if (fi.getDigest() != null) return fi.getDigest();
        List<Chunk> chlst = fi.getChunks();
        if (fi.getStatus() == FileInfo.STATUS.CHUNKED) throw new UploadException(fileKey, UploadException.FILE_INCOMPLETE);
        if (fi.getStatus() == FileInfo.STATUS.ASSEMBLED) {
            byte[] dig;
            try {
                dig = new FileDigest(new File(workDir, key + File.separator + fileKey)).getDigest();
            } catch (IOException e) {
                e.printStackTrace();
                throw new UploadException("Error in FileDigest", e, UploadException.IOError);
            }
            fi.setDigest(dig);
            return dig;
        }
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance(Constants.hashAlgorithm);
        } catch (NoSuchAlgorithmException e) {
        }
        Chunk prev = null;
        for (Chunk ch : chlst) {
            try {
                FileInputStream fis = new FileInputStream(getChunkFile(key, fileKey, ch.getBegin(), ch.getEnd() - ch.getBegin(), ch.getID()));
                if (prev != null && prev.getEnd() > ch.getBegin()) fis.skip(prev.getEnd() - ch.getBegin());
                byte[] buffer = new byte[10000];
                int l;
                while ((l = fis.read(buffer)) > 0) {
                    md.update(buffer, 0, l);
                }
                fis.close();
                prev = ch;
            } catch (IOException e) {
                e.printStackTrace();
                throw new UploadException("IO Error", e, UploadException.IOError);
            }
        }
        fi.setDigest(md.digest());
        return fi.getDigest();
    }

    public TransactionInfo getTransactionInfo(String key) {
        return tranz.get(key);
    }

    public void startFileTransfer(String key, String fileKey, long length) {
        tranz.get(key).addFileInfo(fileKey, length);
    }

    public void uploadChunk(String key, String fileKey, long begin, ADOB filePartADOB, @SuppressWarnings("unused") ProgressListener lsn) throws UploadException {
        try {
            String fname = key + File.separator + fileKey + '#' + begin + '#';
            File outFile = new File(workDir, fname);
            FileOutputStream fos = new FileOutputStream(outFile);
            byte[] buf = new byte[1000];
            MessageDigest md = null;
            try {
                md = MessageDigest.getInstance(Constants.hashAlgorithm);
            } catch (NoSuchAlgorithmException e) {
            }
            InputStream is = filePartADOB.getInputStream();
            int n;
            long total = 0;
            try {
                while ((n = is.read(buf)) > 0) {
                    md.update(buf, 0, n);
                    fos.write(buf, 0, n);
                    total += n;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            is.close();
            fos.close();
            System.out.println("Uploaded: " + total);
            TransactionInfo ti = tranz.get(key);
            int cid = ti.addFileChunk(fileKey, new Chunk(begin, begin + total, md.digest()));
            outFile.renameTo(getChunkFile(key, fileKey, begin, total, cid));
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File(workDir, key + File.separator + Constants.metaFileName)));
            oos.writeObject(ti);
            oos.close();
            filePartADOB.release();
        } catch (IOException e) {
            e.printStackTrace();
            throw new UploadException("IO Error", e, UploadException.IOError);
        }
    }

    private File getChunkFile(String trnaKey, String fileKey, long begin, long total, int cid) {
        return new File(workDir, trnaKey + File.separator + fileKey + '#' + begin + '#' + (begin + total - 1) + '@' + cid);
    }
}
