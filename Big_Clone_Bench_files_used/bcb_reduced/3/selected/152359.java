package net.cobra84.jstream.plugins;

import java.nio.*;
import java.security.*;
import net.cobra84.jstream.*;

public final class PluginP_MD5 extends ProcessorPlugin {

    private static final String NAME = "Message digest processor";

    private static final String AUTHOR = "Jeremy.J <joly.jeremy@gmail.com>";

    private static final String DESCRIPTION = null;

    private static final String VERSION = "1.2";

    private static final boolean MULTITHREAD_CAPABLE = false;

    MessageDigest _messageDigest;

    public PluginP_MD5() {
        super(NAME, AUTHOR, DESCRIPTION, VERSION, MULTITHREAD_CAPABLE);
        setProperty("outputFormat", "hex");
    }

    public PluginP_MD5(String outputFormat) {
        super(NAME, AUTHOR, DESCRIPTION, VERSION, MULTITHREAD_CAPABLE);
        setProperty("outputFormat", outputFormat);
    }

    public void init() throws PluginException {
        try {
            _messageDigest = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            throw new PluginException(e.getMessage());
        }
    }

    public void process(JSBuffer jsBuffer) throws PluginException {
        _messageDigest.update(jsBuffer.getBuffer());
        jsBuffer.setBuffer(null);
    }

    public void finalProcess(JSBuffer jsBuffer) throws PluginException {
        byte[] digest = _messageDigest.digest();
        int dataSize = 0;
        if (getProperty("outputFormat").equals("hex")) {
            dataSize = 32;
            digest = toHex(digest);
        } else {
            dataSize = 16;
        }
        jsBuffer.allocateNewBuffer(dataSize, digest);
    }

    public void unload() {
        _messageDigest = null;
    }

    private byte[] toHex(byte[] value) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < value.length; ++i) {
            sb.append(Integer.toHexString((value[i] & 0xFF) | 0x100).toUpperCase().substring(1, 3));
        }
        return sb.toString().getBytes();
    }
}
