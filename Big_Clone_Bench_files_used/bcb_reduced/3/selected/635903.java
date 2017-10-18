package com.smssalama.storage.sms;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import com.smssalama.security.Security;
import com.smssalama.storage.framework.ObjectStorageException;

public class SMSMessageIn extends SMSMessage {

    private byte[] data;

    private boolean encrypted;

    public SMSMessageIn() {
        super(null, 0);
    }

    public SMSMessageIn(String phoneNumber, long timestamp, byte[] encryptedData) {
        super(phoneNumber, timestamp);
        this.data = encryptedData;
        this.encrypted = true;
    }

    protected void store(DataOutputStream stream) throws IOException, ObjectStorageException {
        super.store(stream);
        stream.writeBoolean(this.encrypted);
        stream.writeInt(this.data.length);
        stream.write(this.data);
    }

    protected void load(DataInputStream stream) throws IOException, ObjectStorageException {
        super.load(stream);
        this.encrypted = stream.readBoolean();
        this.data = new byte[stream.readInt()];
        stream.read(this.data, 0, this.data.length);
    }

    public void decrypt(String sharedKey) throws Exception {
        byte[] key = sharedKey != null ? sharedKey.getBytes("UTF-8") : new byte[] {};
        byte[] decryptedMessage = Security.decrypt(key, this.data);
        ByteArrayInputStream stream = new ByteArrayInputStream(decryptedMessage);
        byte[] digest = new byte[Security.getDigestSize()];
        byte[] message = null;
        stream.read(digest, 0, digest.length);
        message = new byte[stream.available()];
        stream.read(message);
        message = new String(message).trim().getBytes();
        byte[] digest2 = Security.digest(message);
        if (digest2.length != digest.length) {
            throw new DecodingException();
        }
        for (int i = 0; i < digest.length; i++) {
            if (digest[i] != digest2[i]) {
                throw new DecodingException();
            }
        }
        this.data = message;
        this.encrypted = false;
    }

    public boolean isEncrypted() {
        return this.encrypted;
    }

    public byte[] getData() {
        return this.data;
    }

    public String getMessageText() {
        if (this.encrypted) {
            throw new RuntimeException("The message is not decrypted");
        }
        return new String(this.data);
    }
}
