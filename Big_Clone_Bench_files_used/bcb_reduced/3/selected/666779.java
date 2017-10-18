package pl.edu.agh.ssm.LongMethods;

import java.util.*;
import java.security.*;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidParameterSpecException;
import javax.crypto.spec.DHParameterSpec;

public class Hash {

    public void longProc2(int param) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] temp = new byte[param];
        temp = md.digest(temp);
    }

    public void longProc1() throws NoSuchAlgorithmException, InvalidParameterSpecException, InvalidAlgorithmParameterException {
        String method = "DSA";
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(method);
        AlgorithmParameterGenerator paramGen = AlgorithmParameterGenerator.getInstance(method);
        paramGen.init(512);
        AlgorithmParameters params = paramGen.generateParameters();
        AlgorithmParameterSpec dhSkipParamSpec = null;
        if (method == "DH") dhSkipParamSpec = params.getParameterSpec(DHParameterSpec.class); else if (method == "DSA") dhSkipParamSpec = params.getParameterSpec(java.security.spec.DSAParameterSpec.class);
        if (dhSkipParamSpec != null) kpg.initialize(dhSkipParamSpec);
        KeyPair temp = kpg.generateKeyPair();
    }

    public static void main(String args[]) {
        Hash h = new Hash();
        try {
            Date d = new Date();
            h.longProc1();
            h.longProc2(1000);
            System.out.println("czas : " + ((new Date()).getTime() - d.getTime()));
        } catch (java.security.GeneralSecurityException e) {
            e.printStackTrace();
        }
    }
}
