package org.swemas.security;

import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import org.swemas.core.Module;
import org.swemas.core.ModuleException;
import org.swemas.core.ModuleNotFoundException;
import org.swemas.core.event.IEventDispatchingChannel;
import org.swemas.core.kernel.IKernel;
import sun.misc.BASE64Encoder;

/**
 * @author Alexey Chernov
 * 
 */
public class SwDataGenerator extends Module implements IDataGenerationChannel {

    /**
	 * @param kernel
	 * @throws ModuleException
	 */
    public SwDataGenerator(IKernel kernel) throws InvocationTargetException {
        super(kernel);
        try {
            _md = MessageDigest.getInstance("SHA-512");
            try {
                IEventDispatchingChannel ev = (IEventDispatchingChannel) kernel().getChannel(IEventDispatchingChannel.class);
                ev.event(new AlgorithmEvent(null, getClass().getCanonicalName(), "SHA-512", EventCode.DigestAlgorithmUsed.getCode()));
            } catch (ModuleNotFoundException m) {
            }
        } catch (NoSuchAlgorithmException e) {
            try {
                _md = MessageDigest.getInstance("SHA-256");
                try {
                    IEventDispatchingChannel ev = (IEventDispatchingChannel) kernel().getChannel(IEventDispatchingChannel.class);
                    ev.event(new AlgorithmEvent(null, getClass().getCanonicalName(), "SHA-256", EventCode.DigestAlgorithmUsed.getCode()));
                } catch (ModuleNotFoundException m) {
                }
            } catch (NoSuchAlgorithmException e1) {
                try {
                    _md = MessageDigest.getInstance("SHA-1");
                    try {
                        IEventDispatchingChannel ev = (IEventDispatchingChannel) kernel().getChannel(IEventDispatchingChannel.class);
                        ev.event(new AlgorithmEvent(null, getClass().getCanonicalName(), "SHA-1", EventCode.DigestAlgorithmUsed.getCode()));
                    } catch (ModuleNotFoundException m) {
                    }
                } catch (NoSuchAlgorithmException e2) {
                    try {
                        _md = MessageDigest.getInstance("MD5");
                        try {
                            IEventDispatchingChannel ev = (IEventDispatchingChannel) kernel().getChannel(IEventDispatchingChannel.class);
                            ev.event(new AlgorithmEvent(null, getClass().getCanonicalName(), "MD5", EventCode.DigestAlgorithmUsed.getCode()));
                        } catch (ModuleNotFoundException m) {
                        }
                    } catch (NoSuchAlgorithmException e3) {
                        SecurityException s = new SecurityException(e3, getClass().getCanonicalName(), ErrorCode.NoAlgorithmError.getCode());
                        throw new InvocationTargetException(new ModuleException(s, getClass().getCanonicalName(), org.swemas.core.ErrorCode.ObjectConstructionError.getCode()));
                    }
                }
            }
        }
    }

    @Override
    public String alphanum(String str) {
        return str.replaceAll("[^a-zA-Z0-9]", "");
    }

    @Override
    public String generateString(int length) {
        BigInteger b = new BigInteger(length * 8, getRandom());
        return hash(alphanum(encode(b.toByteArray())));
    }

    @Override
    public String hash(String src) {
        return alphanum(encode(bhash(src.getBytes())));
    }

    @Override
    public String encode(byte[] src) {
        return getEncoder().encode(src);
    }

    protected SecureRandom getRandom() {
        return _random;
    }

    ;

    protected MessageDigest getDigest() {
        return _md;
    }

    ;

    protected BASE64Encoder getEncoder() {
        return _encoder;
    }

    ;

    private synchronized byte[] bhash(byte[] src) {
        _md.update(src);
        return _md.digest();
    }

    private SecureRandom _random = new SecureRandom();

    private BASE64Encoder _encoder = new sun.misc.BASE64Encoder();

    private MessageDigest _md;
}
