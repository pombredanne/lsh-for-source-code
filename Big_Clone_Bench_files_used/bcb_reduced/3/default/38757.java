import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5checksum {

    private static String xmloutput = "";

    public static void main(String[] args) {
        if (args.length != 0) {
            if (args[0].equals("?") || args[0].equals("-help") || args[0].equals("/?")) {
                help();
                return;
            } else {
                credits();
            }
        } else {
            credits();
            File md5sum = new File("md5sum.xml");
            if (md5sum.exists()) {
                SAXParse parser = new SAXParse();
                int ok = 0;
                int error = 0;
                int total = 0;
                System.out.println("Checking " + parser.fileList.size() + " files...");
                for (int i = 0; i < parser.fileList.size(); i++) {
                    Files file = parser.fileList.get(i);
                    File f = new File(file.fileName);
                    if (f.exists()) {
                        if (f.isFile()) {
                            total++;
                            String md5 = "";
                            try {
                                md5 = computeMD5(f);
                            } catch (NoSuchAlgorithmException nsae) {
                                nsae.printStackTrace();
                            } catch (FileNotFoundException fnfe) {
                                fnfe.printStackTrace();
                            }
                            if (md5.equals(file.md5sum)) {
                                System.out.println("[" + total + "]\t[OK]\t" + file.fileName);
                                ok++;
                            } else {
                                System.err.println("[" + total + "]\t[ERROR]\t" + file.fileName + " calculated md5: " + md5 + " - given md5: " + file.md5sum);
                                error++;
                            }
                        }
                    }
                }
                System.out.println("Done!\n\nTotal files: " + total + "\tOK: " + ok + "\tERRORS: " + error + "\n");
            } else {
                FilenameFilter filter = new FilenameFilter() {

                    public boolean accept(File dir, String name) {
                        if (name.equals("md5checksum.jar") || name.equals("md5checksum.bat")) return false; else return name.contains("");
                    }
                };
                File directory = new File(".");
                String tempList[] = directory.list(filter);
                xmloutput = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<List>\n";
                if (tempList.length > 0) {
                    System.out.println("Generating checksums...");
                    int counter = 0;
                    for (int i = 0; i < tempList.length; i++) {
                        String name = tempList[i];
                        File f = new File(name);
                        if (f.exists()) {
                            if (f.isFile()) {
                                counter++;
                                String md5 = "";
                                try {
                                    md5 = computeMD5(f);
                                } catch (NoSuchAlgorithmException e) {
                                    e.printStackTrace();
                                } catch (FileNotFoundException e) {
                                    e.printStackTrace();
                                }
                                Files files = new Files(name, md5);
                                xmloutput += files.toXMLFormat();
                                System.out.println("[" + counter + "]\t" + files.fileName);
                            }
                        }
                    }
                    xmloutput += "</List>";
                    try {
                        File file = new File("md5sum.xml");
                        OutputStream fout = new FileOutputStream(file);
                        OutputStream bout = new BufferedOutputStream(fout);
                        OutputStreamWriter output = new OutputStreamWriter(bout, "UTF-8");
                        output.write(xmloutput);
                        output.close();
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                    System.out.println("Done!\n\nTotal files: " + counter + "\n");
                } else {
                    System.out.println("No file found.");
                }
            }
        }
    }

    private static void credits() {
        String credits = "\t\t*** md5checksum v0.1 ***\n" + "\t\t   by Daniele Migliau" + "\nFor help: java -jar md5checksum.jar ?\n";
        System.out.println(credits);
    }

    public static void help() {
        String help = "\t\t*** md5checksum v0.1 ***\n" + "\t\t   by Daniele Migliau\n\n" + "HELP\nSimple program which calculates md5sums.\n\n" + "To calculate md5sums:\n" + "\tput md5checksum.jar and md5checksum.bat into the folder containing the files to be checked.\n" + "\tdouble click checksum.bat\n\n" + "A file (md5sum.xml) will be created. Now just zip the whole folder and send it!\n\n" + "To check if what you received is correct:\n" + "\tdouble click checksum.bat\n\n" + "Feel free to redistribute!";
        System.out.println(help);
    }

    private static String computeMD5(File f) throws NoSuchAlgorithmException, FileNotFoundException {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        InputStream is = new FileInputStream(f);
        byte[] buffer = new byte[8192];
        int read = 0;
        try {
            while ((read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            byte[] md5sum = digest.digest();
            BigInteger bigInt = new BigInteger(1, md5sum);
            return bigInt.toString(16).toUpperCase();
        } catch (IOException e) {
            throw new RuntimeException("Unable to process file for MD5", e);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                throw new RuntimeException("Unable to close input stream for MD5 calculation", e);
            }
        }
    }
}
