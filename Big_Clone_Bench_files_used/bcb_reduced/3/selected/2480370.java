package com.smssalama.storage.sms;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import com.smssalama.security.Security;
import com.smssalama.storage.framework.ObjectStorageException;

public class SMSMessageOut extends SMSMessage {

    public static final int PENDING = 0;

    public static final int SENT = 1;

    public static final int FAILED = -1;

    private String messageText;

    private int status;

    public SMSMessageOut() {
        super(null, 0);
    }

    public SMSMessageOut(String phoneNumber, long timestamp, String messageText) {
        super(phoneNumber, timestamp);
        this.messageText = messageText;
    }

    protected void store(DataOutputStream stream) throws IOException, ObjectStorageException {
        super.store(stream);
        stream.writeInt(this.status);
        stream.writeUTF(this.messageText);
    }

    protected void load(DataInputStream stream) throws IOException, ObjectStorageException {
        super.load(stream);
        this.status = stream.readInt();
        this.messageText = stream.readUTF();
    }

    public String getMessageText() {
        return this.messageText;
    }

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public byte[] encrypt(String sharedKey) throws Exception {
        byte[] key = sharedKey != null ? sharedKey.getBytes("UTF-8") : new byte[] {};
        byte[] data = this.messageText.getBytes();
        byte[] digest = Security.digest(this.messageText.getBytes());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(digest);
        out.write(data);
        byte[] encryptedData = Security.encrypt(key, out.toByteArray());
        return encryptedData;
    }
}
