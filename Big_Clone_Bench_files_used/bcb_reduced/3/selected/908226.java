package comdemo.internal.identity;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import com.thebitstream.comserver.auth.IAuthorize;
import com.thebitstream.comserver.identity.IClientIdentity;

public class FaceBookToken implements IClientIdentity, IAuthorize {

    private String salt;

    @Override
    public String readId(Object[] params) {
        String[] chunks = params[0].toString().split("-");
        System.out.print("user id is" + chunks[2]);
        return chunks[2];
    }

    @Override
    public String readType(Object[] params) {
        String[] chunks = params[0].toString().split("-");
        return chunks[3];
    }

    @Override
    public boolean appConnect(Object[] params) {
        String[] chunks = params[0].toString().split("-");
        if (chunks.length < 4) return false;
        String addtionalParams = "";
        for (int i = 4; i < chunks.length; i++) {
            addtionalParams = addtionalParams + "-" + chunks[i];
        }
        try {
            return checkToken(chunks[1], chunks[2], chunks[3], addtionalParams, chunks[0]);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean checkToken(String expirationTime, String uid, String type, String additionalParams, String tokenToCheck) throws NoSuchAlgorithmException {
        String sessionid = expirationTime + uid + type + additionalParams + salt;
        byte[] defaultBytes = sessionid.getBytes();
        MessageDigest algorithm = MessageDigest.getInstance("MD5");
        algorithm.reset();
        algorithm.update(defaultBytes);
        byte messageDigest[] = algorithm.digest();
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < messageDigest.length; i++) {
            String int_s = Integer.toHexString(0xFF & messageDigest[i]);
            if (int_s.length() > 1) {
                hexString.append(int_s);
            } else {
                hexString.append("0" + int_s);
            }
        }
        if (tokenToCheck.contentEquals(hexString)) {
            return true;
        } else {
            return false;
        }
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }
}
