package org.freeworld.medialauncher.model.access.hash;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import org.apache.log4j.Logger;

public class HashFactory {

    private static final int BUFFER_CHUNK_MAX = 4096;

    private static Logger logger = Logger.getLogger(HashFactory.class);

    private static final Hash NULL_HASH = new NullHash();

    private static final HashPair MD5_PAIR = new HashPair("MD5", MD5Hash.class);

    private static final HashPair SHA_PAIR = new HashPair("SHA", Sha1Hash.class);

    private static final HashPair[] hashes = new HashPair[] { MD5_PAIR, SHA_PAIR };

    public static Hash computeHash(File file) {
        try {
            if (file.canRead()) {
                FileInputStream fis = new FileInputStream(file);
                try {
                    return computeHash(fis, getHashingAlgorithm());
                } finally {
                    try {
                        fis.close();
                    } catch (IOException e) {
                    }
                }
            } else return NULL_HASH;
        } catch (FileNotFoundException e) {
            logger.warn("Could not hash " + file, e);
            return NULL_HASH;
        }
    }

    public static Hash computeHash(InputStream stream, HashPair pair) {
        byte[] retr = null;
        try {
            MessageDigest md = MessageDigest.getInstance(pair.hashType);
            md.reset();
            int count = 0;
            byte[] bytes = new byte[BUFFER_CHUNK_MAX];
            while ((count = stream.read(bytes)) > 0) {
                if (count == BUFFER_CHUNK_MAX) md.update(bytes); else md.update(bytes, 0, count);
            }
            retr = md.digest();
        } catch (Throwable t) {
            return new NullHash();
        }
        Hash hash;
        try {
            hash = pair.hashClass.newInstance();
        } catch (Throwable t) {
            logger.error("Could not generate a hash instance of " + pair.hashClass, t);
            return NULL_HASH;
        }
        hash.setHashBytes(retr);
        return hash;
    }

    public static Hash getNullHash() {
        return NULL_HASH;
    }

    public static HashPair getHashingAlgorithm() {
        return hashes[0];
    }

    private static class HashPair {

        public String hashType = null;

        public Class<? extends Hash> hashClass = null;

        public HashPair(String hashType, Class<? extends Hash> hashClass) {
            this.hashType = hashType;
            this.hashClass = hashClass;
        }
    }
}
