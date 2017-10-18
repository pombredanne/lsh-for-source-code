package netxrv.jnlp.jardescriptor;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import netxrv.jnlp.util.JarFile2;

public class NormalizedJarDescriptor {

    public static String createNormalizedDescriptor(JarFile2 jarFile) {
        Collection<String> entryDescriptors = new TreeSet<String>();
        Iterator<ZipEntry> entryIter = jarFile.getJarEntries();
        while (entryIter.hasNext()) {
            entryDescriptors.add(buildEntryDescriptor(entryIter.next(), jarFile));
        }
        StringBuilder builder = new StringBuilder();
        for (String descriptor : entryDescriptors) {
            builder.append(descriptor + "\n");
        }
        return builder.toString();
    }

    public static String createNormalizedJarDescriptorDigest(String path) throws Exception {
        String descriptor = createNormalizedDescriptor(new JarFile2(path));
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(descriptor.getBytes());
            byte[] messageDigest = digest.digest();
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < messageDigest.length; i++) {
                hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    private static String buildEntryDescriptor(ZipEntry entry, JarFile2 jarFile) {
        if (entry.getName().endsWith(".jar")) {
            return buildJarEntryDescriptor(entry, jarFile);
        }
        return buildFileEntryDescriptor(entry);
    }

    private static String buildFileEntryDescriptor(ZipEntry entry) {
        StringBuilder builder = new StringBuilder();
        builder.append(entry.getName());
        builder.append(":");
        builder.append(entry.getSize());
        builder.append(":");
        builder.append(entry.getCrc());
        return builder.toString();
    }

    private static synchronized String buildJarEntryDescriptor(ZipEntry entry, JarFile2 jarFile) {
        StringBuilder builder = new StringBuilder();
        builder.append(entry.getName());
        builder.append(":");
        try {
            String path = jarFile.extractTempFile(entry.getName(), ".jar");
            builder.append(createNormalizedJarDescriptorDigest(path));
            new File(path).delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return builder.toString();
    }

    public static void main(String[] args) {
        try {
            System.out.println(createNormalizedDescriptor(new JarFile2(args[0])));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
