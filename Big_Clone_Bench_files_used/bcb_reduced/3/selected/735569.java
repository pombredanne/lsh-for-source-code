package org.bintrotter.afiles;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import org.bintrotter.afiles.Files;

public class Hasher {

    public static boolean SHA1(int piece, Files files) throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        MappedByteBuffer[] mbb = files.mmap(piece);
        for (MappedByteBuffer b : mbb) md.update(b);
        for (int i = 0; i < mbb.length; ++i) mbb[i] = null;
        mbb = null;
        return Arrays.equals(md.digest(), files.metafile.pieceHash(piece));
    }
}
