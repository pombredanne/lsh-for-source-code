package pl.szpadel.android.gadu.packets;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Arrays;
import pl.szpadel.android.gadu.App;
import pl.szpadel.android.gadu.Status;

public class Login80 extends SendPacket {

    @SuppressWarnings("unused")
    private static final String TAG = "Login80";

    public int uin;

    public byte[] lang = new byte[2];

    public byte hash_type = 0x02;

    public byte[] hash = new byte[64];

    public int status = 0x0004;

    public int flags = 0x00100000 | 0x00800000;

    public int features = 0x00000001 | 0x00000002 | 0x00000004 | 0x00000040;

    public int local_ip = 0;

    public short local_port = 0;

    public int external_ip = 0;

    public short external_port = 0;

    public byte image_size = 0;

    public byte unknown1 = 0x64;

    public String version;

    public String description;

    public Login80(int u, int seed, String password, Status initialStatus) {
        super(TYPE_LOGIN80);
        version = App.getInstance().getGGClientVersion();
        uin = u;
        description = initialStatus.getDescription();
        status = initialStatus.getGGStatus();
        try {
            MessageDigest alg = MessageDigest.getInstance("SHA1");
            ByteBuffer bb = ByteBuffer.allocate(4);
            bb.putInt(Integer.reverseBytes(seed));
            byte[] seedBytes = bb.array();
            byte[] passwordBytes;
            passwordBytes = password.getBytes("UTF-8");
            alg.update(passwordBytes);
            alg.update(seedBytes);
            byte[] result = alg.digest();
            Arrays.fill(hash, (byte) 0);
            ByteBuffer hashBuffer = ByteBuffer.wrap(hash);
            hashBuffer.put(result);
        } catch (Exception e) {
            ;
        }
        lang[0] = 'p';
        lang[1] = 'l';
    }

    @Override
    protected void writeToBuffer(ByteBuffer buffer) throws BufferOverflowException {
        buffer.putInt(uin);
        buffer.put(lang);
        buffer.put(hash_type);
        buffer.put(hash);
        buffer.putInt(status);
        buffer.putInt(flags);
        buffer.putInt(features);
        buffer.putInt(local_ip);
        buffer.putShort(local_port);
        buffer.putInt(external_ip);
        buffer.putShort(external_port);
        buffer.put(image_size);
        buffer.put(unknown1);
        putString(buffer, version);
        putString(buffer, description);
    }
}
