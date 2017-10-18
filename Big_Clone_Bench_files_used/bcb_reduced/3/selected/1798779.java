package org.petero.droidfish.engine;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.GZIPInputStream;
import android.content.Context;

/** Stockfish engine running as process, started from assets resource. */
public class InternalStockFish extends ExternalEngine {

    public InternalStockFish(Context context, Report report) {
        super(context, "", report);
    }

    /** @inheritDoc */
    @Override
    public final void setStrength(int strength) {
        setOption("Skill Level", strength / 50);
    }

    private final long readCheckSum(File f) {
        InputStream is = null;
        try {
            is = new FileInputStream(f);
            DataInputStream dis = new DataInputStream(is);
            return dis.readLong();
        } catch (IOException e) {
            return 0;
        } finally {
            if (is != null) try {
                is.close();
            } catch (IOException ex) {
            }
        }
    }

    private final void writeCheckSum(File f, long checkSum) {
        OutputStream os = null;
        try {
            os = new FileOutputStream(f);
            DataOutputStream dos = new DataOutputStream(os);
            dos.writeLong(checkSum);
        } catch (IOException e) {
        } finally {
            if (os != null) try {
                os.close();
            } catch (IOException ex) {
            }
        }
    }

    private final long computeAssetsCheckSum(String sfExe) {
        InputStream is = null;
        try {
            is = context.getAssets().open(sfExe);
            if (sfExe.endsWith(".mygz")) is = new GZIPInputStream(is);
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] buf = new byte[8192];
            while (true) {
                int len = is.read(buf);
                if (len <= 0) break;
                md.update(buf, 0, len);
            }
            byte[] digest = md.digest(new byte[] { 0 });
            long ret = 0;
            for (int i = 0; i < 8; i++) {
                ret ^= ((long) digest[i]) << (i * 8);
            }
            return ret;
        } catch (IOException e) {
            return -1;
        } catch (NoSuchAlgorithmException e) {
            return -1;
        } finally {
            if (is != null) try {
                is.close();
            } catch (IOException ex) {
            }
        }
    }

    @Override
    protected void copyFile(File from, File to) throws IOException {
        final String sfExe = EngineUtil.internalStockFishName();
        long oldCSum = readCheckSum(new File(intSfPath));
        long newCSum = computeAssetsCheckSum(sfExe);
        if (oldCSum == newCSum) return;
        if (to.exists()) to.delete();
        to.createNewFile();
        InputStream is = context.getAssets().open(sfExe);
        if (sfExe.endsWith(".mygz")) is = new GZIPInputStream(is);
        OutputStream os = new FileOutputStream(to);
        try {
            byte[] buf = new byte[8192];
            while (true) {
                int len = is.read(buf);
                if (len <= 0) break;
                os.write(buf, 0, len);
            }
        } finally {
            if (is != null) try {
                is.close();
            } catch (IOException ex) {
            }
            if (os != null) try {
                os.close();
            } catch (IOException ex) {
            }
        }
        writeCheckSum(new File(intSfPath), newCSum);
    }
}
