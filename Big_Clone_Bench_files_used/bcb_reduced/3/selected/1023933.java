package org.gudy.azureus2.pluginsimpl.local.ui.config;

import java.security.MessageDigest;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SHA1Hasher;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.plugins.ui.config.PasswordParameter;
import org.gudy.azureus2.pluginsimpl.local.PluginConfigImpl;

public class PasswordParameterImpl extends ParameterImpl implements PasswordParameter {

    protected byte[] defaultValue;

    protected int encoding_type;

    public PasswordParameterImpl(PluginConfigImpl config, String key, String label, int _encoding_type, byte[] _default_value) {
        super(config, key, label);
        encoding_type = _encoding_type;
        if (_default_value == null) {
            defaultValue = new byte[0];
        } else {
            defaultValue = encode(_default_value);
        }
        config.notifyParamExists(getKey());
        COConfigurationManager.setByteDefault(getKey(), defaultValue);
    }

    public byte[] getDefaultValue() {
        return (defaultValue);
    }

    public void setValue(String plain_password) {
        byte[] encoded;
        if (plain_password == null || plain_password.length() == 0) {
            encoded = new byte[0];
        } else {
            encoded = encode(plain_password);
        }
        config.setUnsafeByteParameter(getKey(), encoded);
    }

    public int getEncodingType() {
        return (encoding_type);
    }

    public byte[] getValue() {
        return config.getUnsafeByteParameter(getKey(), getDefaultValue());
    }

    protected byte[] encode(String str) {
        try {
            return (encode(encoding_type == ET_MD5 ? str.getBytes("UTF-8") : str.getBytes()));
        } catch (Throwable e) {
            Debug.out(e);
            return (null);
        }
    }

    protected byte[] encode(byte[] bytes) {
        if (encoding_type == ET_SHA1) {
            SHA1Hasher hasher = new SHA1Hasher();
            return (hasher.calculateHash(bytes));
        } else if (encoding_type == ET_MD5) {
            try {
                return (MessageDigest.getInstance("md5").digest(bytes));
            } catch (Throwable e) {
                Debug.printStackTrace(e);
            }
        }
        return (bytes);
    }
}
