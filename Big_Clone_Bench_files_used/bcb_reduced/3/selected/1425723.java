package net.sf.downloadr;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.sf.downloadr.flickr.Config;
import net.sf.downloadr.flickr.ServiceArgument;

public class SignatureGenerator {

    private MessageDigest md5Algorithm;

    private Config config;

    @SuppressWarnings("unchecked")
    public String generateSignature(List serviceArguments) {
        return this.generateSignature((ServiceArgument[]) serviceArguments.toArray(new ServiceArgument[serviceArguments.size()]));
    }

    @SuppressWarnings("unchecked")
    public String generateSignature(ServiceArgument[] serviceArguments) {
        List<ServiceArgument> argList = Arrays.asList(serviceArguments);
        Collections.sort(argList);
        StringBuffer plainText = new StringBuffer();
        plainText.append(config.getSharedSecret());
        for (ServiceArgument serviceArgument : argList) {
            plainText.append(serviceArgument.getName() + serviceArgument.getValue());
        }
        return md5(plainText.toString());
    }

    protected String md5(String input) {
        this.md5Algorithm.update(input.getBytes());
        StringBuffer md5 = new StringBuffer();
        byte[] digestedBytes = this.md5Algorithm.digest();
        for (byte digestedByte : digestedBytes) {
            String hex = Integer.toHexString(0xFF & digestedByte);
            hex = hex.length() < 2 ? "0" + hex : hex;
            md5.append(hex);
        }
        return md5.toString();
    }

    public void setMd5Algorithm(MessageDigest md5Algorithm) {
        this.md5Algorithm = md5Algorithm;
    }

    public void setConfig(Config config) {
        this.config = config;
    }
}
