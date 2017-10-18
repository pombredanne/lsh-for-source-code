package ch.unibas.jmeter.snmp.sampler.filewriter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import org.apache.jmeter.samplers.AbstractSampler;
import org.apache.jmeter.samplers.Entry;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testbeans.TestBean;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;
import ch.unibas.debug.Debug;

public class FileWriterSampler extends AbstractSampler implements TestBean {

    private static final long serialVersionUID = -5455190521679899604L;

    private static final Logger log = LoggingManager.getLoggerForClass();

    private String fileName;

    private boolean bufferedIo;

    private long numberOfKBytes;

    private boolean checkMd5;

    private byte[] md5Digest;

    public SampleResult sample(Entry e) {
        SampleResult result = new SampleResult();
        boolean isOK = false;
        result.setSampleLabel(getName());
        result.sampleStart();
        isOK = writeFile(result);
        if (isOK && isCheckMd5()) {
            isOK = readFile(result);
        }
        result.sampleEnd();
        if (isOK) {
            result.setResponseMessage("Wrote " + numberOfKBytes + " KB");
            result.setResponseData(checkMd5 ? md5Digest : "OK".getBytes());
            result.setResponseCode("0");
        } else {
            result.setResponseCode("400");
        }
        result.setSuccessful(isOK);
        return result;
    }

    private boolean readFile(SampleResult result) {
        File file = new File(getFileName());
        if (!file.exists()) {
            String msg = "File " + getFileName() + " does not exist, error checking file.";
            log.info(msg);
            result.setResponseMessage(msg);
            return false;
        }
        InputStream fis = null;
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            if (isBufferedIo()) {
                Debug.debug("Buffered IO", 3);
                fis = new BufferedInputStream(new FileInputStream(file));
            } else {
                Debug.debug("NON Buffered IO", 3);
                fis = new FileInputStream(file);
            }
            int i;
            while ((i = fis.read()) != -1) {
                md5.update((byte) i);
            }
            if (!MessageDigest.isEqual(md5Digest, md5.digest())) {
                result.setResponseMessage("MD5 digest not identical");
                return false;
            }
            result.setResponseMessage(new String(md5Digest));
        } catch (IOException e) {
            log.error("Unable to read file", e);
            result.setResponseMessage(e.getMessage());
            return false;
        } catch (NoSuchAlgorithmException e1) {
            log.error("Error creating the digest", e1);
            result.setResponseMessage(e1.getMessage());
            return false;
        } finally {
            try {
                fis.close();
            } catch (IOException e) {
            }
        }
        return true;
    }

    private boolean writeFile(SampleResult result) {
        File file = new File(getFileName());
        if (file.exists()) {
            log.info("Deleting file " + getFileName() + " since it allready exists.");
            file.delete();
        }
        Random random = new Random(System.currentTimeMillis());
        try {
            MessageDigest md5 = null;
            if (isCheckMd5()) {
                md5 = MessageDigest.getInstance("MD5");
            }
            OutputStream fos;
            if (isBufferedIo()) {
                Debug.debug("Buffered IO", 3);
                fos = new BufferedOutputStream(new FileOutputStream(file));
            } else {
                Debug.debug("NON Buffered IO", 3);
                fos = new FileOutputStream(file);
            }
            long numberOfBytes = numberOfKBytes * 1024;
            for (long i = 0; i < numberOfBytes; i++) {
                int b = random.nextInt();
                fos.write(b);
                if (md5 != null) {
                    md5.update((byte) b);
                }
            }
            fos.close();
            fos.flush();
            if (md5 != null) {
                md5Digest = md5.digest();
            }
            result.setBytes((int) numberOfBytes);
        } catch (IOException e) {
            log.error("Unable to write file", e);
            result.setResponseMessage(e.getMessage());
            return false;
        } catch (NoSuchAlgorithmException e1) {
            log.error("Error creating the digest", e1);
            result.setResponseMessage(e1.getMessage());
            return false;
        }
        return true;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setNumberOfKBytes(long numberOfKBytes) {
        this.numberOfKBytes = numberOfKBytes;
    }

    public long getNumberOfKBytes() {
        return numberOfKBytes;
    }

    public void setCheckMd5(boolean checkMd5) {
        this.checkMd5 = checkMd5;
    }

    public boolean isCheckMd5() {
        return checkMd5;
    }

    public void setBufferedIo(boolean bufferedIo) {
        this.bufferedIo = bufferedIo;
    }

    public boolean isBufferedIo() {
        return bufferedIo;
    }
}
