package ru.net.romikk.keepass;

import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.engines.TwofishEngine;
import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.*;
import org.bouncycastle.util.encoders.Hex;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * $Id: $
 */
public class Database {

    private Header header;

    private Group[] groups;

    private Entry[] entries;

    private int DB_VERSION = 0x00030002;

    private BlockCipher aesEngine = new AESEngine();

    private BlockCipher twofishEngine = new TwofishEngine();

    private byte[] masterKey;

    private byte[] passwordHash;

    private File dbFile;

    public Database(String file) {
        this(new File(file));
    }

    public Database(File file) {
        this.dbFile = file;
    }

    public void setMasterKey(byte[] masterKey) {
        this.masterKey = masterKey;
    }

    public void setPasswordHash(byte[] passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Header getHeader() throws IOException {
        return header;
    }

    public Group[] getGroups() throws IOException {
        return groups;
    }

    public Entry[] getEntries() {
        return entries;
    }

    public void decrypt() throws IOException, InvalidCipherTextException, NoSuchAlgorithmException {
        FileChannel channel = new FileInputStream(this.dbFile).getChannel();
        ByteBuffer content;
        try {
            ByteBuffer bb = ByteBuffer.allocate(Header.LENGTH).order(ByteOrder.LITTLE_ENDIAN);
            channel.read(bb);
            this.header = new Header(bb);
            if ((header.getVersion() & 0xFFFFFF00) != (DB_VERSION & 0xFFFFFF00)) {
                throw new IOException("Unsupproted version: " + Integer.toHexString(this.header.getVersion()));
            }
            content = ByteBuffer.allocate((int) (channel.size() - channel.position())).order(ByteOrder.LITTLE_ENDIAN);
            channel.read(content);
        } finally {
            channel.close();
        }
        decryptContent(content.array());
        content.rewind();
        this.groups = new Group[this.header.getGroups()];
        for (int i = 0; i < this.groups.length; i++) {
            short fieldType;
            GroupBuilder builder = new GroupBuilder();
            while ((fieldType = content.getShort()) != -1) {
                if (fieldType == 0) {
                    continue;
                }
                int fieldSize = content.getInt();
                builder.readField(fieldType, fieldSize, content);
            }
            content.getInt();
            this.groups[i] = builder.buildGroup();
        }
        this.entries = new Entry[this.header.getEntries()];
        for (int i = 0; i < this.entries.length; i++) {
            short fieldType;
            EntryBuilder builder = new EntryBuilder();
            while ((fieldType = content.getShort()) != -1) {
                if (fieldType == 0) {
                    continue;
                }
                int fieldSize = content.getInt();
                builder.readField(fieldType, fieldSize, content);
            }
            content.getInt();
            this.entries[i] = builder.buildEntry();
        }
    }

    private void decryptContent(byte[] data) throws InvalidCipherTextException, IOException {
        byte[] transformedKey = transformKey(masterKey);
        SHA256Digest sha256 = new SHA256Digest();
        sha256.update(this.header.getMasterSeed(), 0, this.header.getMasterSeed().length);
        sha256.update(transformedKey, 0, transformedKey.length);
        byte[] finalKey = new byte[32];
        sha256.doFinal(finalKey, 0);
        performAESDecrypt(finalKey, data);
        int idx = 1;
        while (data[data.length - idx] != 0) {
            idx++;
        }
        sha256.reset();
        sha256.update(data, 0, data.length - idx + 1);
        byte[] hash = new byte[32];
        sha256.doFinal(hash, 0);
        if (!Arrays.equals(hash, this.header.getContentsHash())) {
            throw new IOException("Decryption failed! Incorrect password and/or master key.");
        }
    }

    private void performAESDecrypt(byte[] key, byte[] data) throws IOException, InvalidCipherTextException {
        KeyParameter keyParameter = new KeyParameter(key);
        BufferedBlockCipher cbcCipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(this.aesEngine), new ZeroBytePadding());
        cbcCipher.init(false, new ParametersWithIV(keyParameter, this.header.getEncryptionIV()));
        byte[] result = new byte[data.length];
        int outputLen = cbcCipher.processBytes(data, 0, data.length, result, 0);
        cbcCipher.doFinal(result, outputLen);
        System.arraycopy(result, 0, data, 0, data.length);
    }

    private byte[] transformKey(byte[] keyToTransform) throws IOException, InvalidCipherTextException {
        KeyParameter masterSeed2 = new KeyParameter(this.header.getMasterSeed2());
        BufferedBlockCipher ecbCipher = new BufferedBlockCipher(this.aesEngine);
        ecbCipher.init(true, masterSeed2);
        byte[] result = new byte[keyToTransform.length];
        for (int i = 0; i < this.header.getKeyEncRounds(); i++) {
            int outputLen = ecbCipher.processBytes(keyToTransform, 0, keyToTransform.length, result, 0);
            ecbCipher.doFinal(result, outputLen);
            System.arraycopy(result, 0, keyToTransform, 0, keyToTransform.length);
        }
        SHA256Digest sha256 = new SHA256Digest();
        sha256.update(keyToTransform, 0, keyToTransform.length);
        sha256.doFinal(result, 0);
        return result;
    }

    public static void main(String[] args) throws Exception {
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        Database db = new Database("Database.kdb");
        db.setMasterKey(Hex.decode("83b62ec2690df02ce7b2f94208469decd93fb0d2febbc2408c86ae7860f5d6af"));
        db.setPasswordHash(sha256.digest("password".getBytes()));
        db.decrypt();
        for (Group g : db.getGroups()) System.out.println(g);
        for (Entry e : db.getEntries()) System.out.println(e);
    }
}
