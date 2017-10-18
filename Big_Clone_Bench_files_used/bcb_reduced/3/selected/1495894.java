package gnu.java.rmi.server;

import gnu.java.lang.reflect.TypeSignature;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Method;
import java.security.MessageDigest;

public class RMIHashes {

    public static long getMethodHash(Method meth) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA");
            ByteArrayOutputStream digest_out = new ByteArrayOutputStream();
            DataOutputStream data_out = new DataOutputStream(digest_out);
            StringBuffer sbuf = new StringBuffer();
            sbuf.append(meth.getName());
            sbuf.append('(');
            Class params[] = meth.getParameterTypes();
            for (int i = 0; i < params.length; i++) sbuf.append(TypeSignature.getEncodingOfClass(params[i]));
            sbuf.append(')');
            Class rcls = meth.getReturnType();
            if (rcls != Void.TYPE) sbuf.append(TypeSignature.getEncodingOfClass(rcls)); else sbuf.append('V');
            data_out.writeUTF(sbuf.toString());
            data_out.flush();
            data_out.close();
            md.update(digest_out.toByteArray());
            byte[] sha = md.digest();
            long result = 0;
            int len = sha.length < 8 ? sha.length : 8;
            for (int i = 0; i < len; i++) result += (long) (sha[i] & 0xFF) << (8 * i);
            return result;
        } catch (Exception _) {
            return -1L;
        }
    }

    public static long getInterfaceHash(Class clazz) {
        return clazz.hashCode();
    }
}
