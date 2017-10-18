package gov.nasa.jpf.jvm;

import gov.nasa.jpf.Config;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class JPF_java_security_MessageDigest {

    static MessageDigest[] digests;

    public static void init(Config conf) {
        digests = new MessageDigest[32];
    }

    static int getNewIndex() {
        int n = digests.length;
        for (int i = 0; i < n; i++) {
            if (digests[i] == null) {
                return i;
            }
        }
        MessageDigest[] newd = new MessageDigest[n + 32];
        System.arraycopy(digests, 0, newd, 0, digests.length);
        digests = newd;
        return n;
    }

    static MessageDigest getDigest(MJIEnv env, int objRef) {
        int id = env.getIntField(objRef, "id");
        return digests[id];
    }

    public static int init0__Ljava_lang_String_2__I(MJIEnv env, int objRef, int algRef) {
        String algorithm = env.getStringObject(algRef);
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            int id = getNewIndex();
            digests[id] = md;
            return id;
        } catch (NoSuchAlgorithmException x) {
            env.throwException("java.security.NoSuchAlgorithmException", algorithm);
            return -1;
        }
    }

    public static int digest___3B___3B(MJIEnv env, int objRef, int inputRef) {
        MessageDigest md = getDigest(env, objRef);
        byte[] input = env.getByteArrayObject(inputRef);
        byte[] res = md.digest(input);
        return env.newByteArray(res);
    }

    public static void finalize____(MJIEnv env, int objRef) {
        int id = env.getIntField(objRef, "id");
        digests[id] = null;
    }
}
