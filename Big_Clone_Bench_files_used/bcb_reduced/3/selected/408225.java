package start;

import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import jaxb.server.FolderConf;
import jaxb.server.IpConf;
import jaxb.server.JaasConf;
import jaxb.server.KerberosConf;
import jaxb.server.LoggerConf;
import jaxb.server.MailConf;
import jaxb.server.PortConf;
import jaxb.server.Servconfig;

public class ConfigManager {

    public static final String CONF_NAME = "serverconf.xml";

    public static final String HASH_FILE = "confDigest";

    private Console javaConsole;

    private BufferedReader myConsole;

    private static SecretKey key;

    Servconfig conf;

    public static SecretKey getEncKey() {
        if (key != null) return key; else return null;
    }

    public ConfigManager() {
        myConsole = new BufferedReader(new InputStreamReader(System.in));
    }

    public void initConf() throws IOException {
        String pwd = null;
        if (!new File(HASH_FILE).exists()) {
            System.out.println("Please enter a password (it will be requested each time to encrypt" + " some of your sensible data):");
            pwd = myConsole.readLine();
            String hash = getHash(pwd);
            FileOutputStream fos = new FileOutputStream(HASH_FILE);
            fos.write(hash.getBytes());
            fos.flush();
            fos.close();
        } else {
            System.out.println("Please enter password:");
            try {
                javaConsole = System.console();
                pwd = String.valueOf(javaConsole.readPassword());
            } catch (Exception ex) {
                pwd = myConsole.readLine();
            }
            String hash = getHash(pwd);
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(HASH_FILE)));
            String hashCheck = reader.readLine();
            reader.close();
            if (!hash.equals(hashCheck)) throw new IOException("Entered password is wrong!");
        }
        KeyGenerator keygen;
        try {
            keygen = KeyGenerator.getInstance("AES");
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            random.setSeed(pwd.getBytes());
            keygen.init(128, random);
            key = keygen.generateKey();
            getOrSetConfig();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public int getServerPort() {
        return conf.getServerPort();
    }

    public String getKerberosServer() {
        return conf.getKerberos().getServer();
    }

    public String getKerberosPrinc() {
        return conf.getKerberos().getPrincipalName();
    }

    public String getPrincipalPasswd() {
        return decrypt(conf.getKerberos().getPrincipalPwd());
    }

    public JaasConf getJaasConf() {
        return conf.getKerberos().getJaas();
    }

    public PortConf getPortConf() {
        return conf.getPorts();
    }

    public FolderConf getFolderConf() {
        return conf.getFolders();
    }

    public LoggerConf getLogger() {
        return conf.getLogs();
    }

    public IpConf getIPConf() {
        return conf.getBan();
    }

    public String getMailServer() {
        return conf.getMail().getSmtpserver();
    }

    public String getMailAddress() {
        return conf.getMail().getMail();
    }

    public String getMailPasswd() {
        return decrypt(conf.getMail().getPassword());
    }

    public boolean getEncMode() {
        return conf.isEncryptedFiles();
    }

    /**
	 * Example
	 * @param args
	 * @throws IOException
	 */
    public static void main(String[] args) throws IOException {
        ConfigManager cm = new ConfigManager();
        cm.initConf();
        System.out.println("kerberos principal: " + cm.getKerberosPrinc());
    }

    private String getHash(String password) {
        byte[] byteDigest = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.reset();
            byteDigest = digest.digest(password.getBytes());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return byteToHex(byteDigest);
    }

    private String encrypt(String toEncr) {
        String encrypted = null;
        Cipher ciph;
        try {
            ciph = Cipher.getInstance("AES");
            ciph.init(Cipher.ENCRYPT_MODE, key);
            encrypted = byteToHex(ciph.doFinal(toEncr.getBytes()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return encrypted;
    }

    private String decrypt(String toDec) {
        String decrypted = null;
        try {
            Cipher ciph = Cipher.getInstance("AES");
            ciph.init(Cipher.DECRYPT_MODE, key);
            decrypted = new String((ciph.doFinal(hex2Byte(toDec))));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return decrypted;
    }

    private byte[] hex2Byte(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    private String byteToHex(byte[] data) {
        StringBuffer strbuf = new StringBuffer(data.length * 2);
        int i;
        for (i = 0; i < data.length; i++) {
            if (((int) data[i] & 0xff) < 0x10) strbuf.append("0");
            strbuf.append(Long.toString((int) data[i] & 0xff, 16));
        }
        return strbuf.toString();
    }

    private void getOrSetConfig() throws IOException {
        if (!(new File(CONF_NAME).exists())) {
            String read = null;
            conf = new Servconfig();
            System.out.println("Some questions in order to initialize your Strongbox server\n");
            System.out.println("On which port will server listen?");
            conf.setServerPort(Integer.parseInt(myConsole.readLine()));
            System.out.println("********************* KERBEROS *******************************");
            System.out.println("(remember you will have to provide external file named Authserver.config with " + "jaas configuration)\n");
            KerberosConf kc = new KerberosConf();
            System.out.println("Kerberos server address:");
            kc.setServer(myConsole.readLine());
            JaasConf jc = new JaasConf();
            jc.setJaasConfFilename("AuthServer.config");
            jc.setJaasRule("AuthServer");
            kc.setJaas(jc);
            BufferedReader inAuth = new BufferedReader(new InputStreamReader(ConfigManager.class.getResourceAsStream("/AuthServer.config")));
            PrintWriter outAuth = new PrintWriter(new FileOutputStream("AuthServer.config"));
            while ((read = inAuth.readLine()) != null) outAuth.write(read + "\n");
            inAuth.close();
            outAuth.close();
            System.out.println("Principal username (with @ suffix):");
            kc.setPrincipalName(myConsole.readLine());
            System.out.println("Principal password: (it will be encrypted don't worry)");
            kc.setPrincipalPwd(encrypt(myConsole.readLine()));
            conf.setKerberos(kc);
            System.out.println("********************** Ports **********************************\n");
            PortConf pc = new PortConf();
            System.out.println("Enter base port address for data transfer: ");
            pc.setBase(Integer.parseInt(myConsole.readLine()));
            System.out.println("Enter port range: ");
            pc.setRange(Integer.parseInt(myConsole.readLine()));
            conf.setPorts(pc);
            System.out.println("********************** User Folders ***************************\n");
            FolderConf fc = new FolderConf();
            System.out.println("Root folder:");
            fc.setRoot(myConsole.readLine());
            fc.setUser("user_folders");
            (new File(fc.getRoot() + "/" + fc.getUser())).mkdir();
            fc.setConfig("user_configs");
            (new File(fc.getRoot() + "/" + fc.getConfig())).mkdir();
            fc.setPolicies("policies");
            fc.setLogs("server_logs");
            conf.setFolders(fc);
            (new File(fc.getRoot() + "/" + fc.getPolicies())).mkdir();
            BufferedReader inPolicy = new BufferedReader(new InputStreamReader(ConfigManager.class.getResourceAsStream("/policies/policy.xml")));
            PrintWriter outPolicy = new PrintWriter(new FileOutputStream(fc.getRoot() + "/" + fc.getPolicies() + "/policy.xml"), true);
            read = null;
            while ((read = inPolicy.readLine()) != null) outPolicy.write(read + "\n");
            inPolicy.close();
            outPolicy.close();
            System.out.println("********************** Logger *********************************\n");
            System.out.println("Select a logger level:");
            System.out.println("0. just local logs");
            System.out.println("1. both local and remote logs");
            System.out.print(":");
            LoggerConf lc = new LoggerConf();
            lc.setSecureLevel(Integer.parseInt(myConsole.readLine()));
            if (lc.getSecureLevel() != 0) {
                System.out.println("Remote logger server address: ");
                lc.setLoggerServer(myConsole.readLine());
                System.out.println("Remote logger server port: ");
                lc.setLoggerPort(Integer.parseInt(myConsole.readLine()));
                System.out.println("Remote logger service name (with @ suffix): ");
                lc.setLoggerService(myConsole.readLine());
                System.out.println("In case remote logger is not reacheable you wan to to be " + "alerted with an email message?Often this is a sign of a DoS attack: an attacket" + " may want to shut down your logger server");
                System.out.println("0. no");
                System.out.println("1. yes");
                if (myConsole.readLine().equals("1")) lc.setAdminMail(true); else lc.setAdminMail(false);
            }
            conf.setLogs(lc);
            (new File(fc.getRoot() + "/" + fc.getLogs())).mkdir();
            System.out.println("************************ IP BAN ******************************");
            IpConf ic = new IpConf();
            System.out.println("N error tries before ban:<0 for unlimited tries (no ip ban)>");
            ic.setNTriesBan(Integer.parseInt(myConsole.readLine()));
            if (ic.getNTriesBan() != 0) {
                System.out.println("Ban time in minutes:<0 for unlimited ban>");
                ic.setBanTimeM(Integer.parseInt(myConsole.readLine()));
            }
            System.out.println("You want to be alerted via mail if someone's banned?");
            System.out.println("0. no");
            System.out.println("1. yes");
            if (myConsole.readLine().equals("1")) ic.setAdminMail(true); else ic.setAdminMail(false);
            conf.setBan(ic);
            if ((ic.isAdminMail()) || ((lc.getSecureLevel() != 0) && lc.isAdminMail())) {
                System.out.println("*********************** Mail conf *************************\n");
                MailConf mc = new MailConf();
                System.out.println("Enter your admin mail address: ");
                mc.setMail(myConsole.readLine());
                System.out.println("Enter your smtp server address: ");
                mc.setSmtpserver(myConsole.readLine());
                System.out.println("Enter your mail password (it will be crypted don't worry)");
                mc.setPassword(encrypt(myConsole.readLine()));
                System.out.println("For mail to work you'll have to provide a .p12 external " + "file named as the part before @ in your mail and with the same PIN of your password");
                conf.setMail(mc);
            }
            System.out.println("Lastly, you want your user's files be encrypted?While this adds " + " a very strong security, mind that server's performances could be affected");
            System.out.println("0. no");
            System.out.println("1. yes");
            if (myConsole.readLine().equals("1")) conf.setEncryptedFiles(true); else conf.setEncryptedFiles(false);
            System.out.println("\n\nStrongbox server configured, you that you can still modify");
            System.out.println("config parameters modifying " + CONF_NAME + " file");
            System.out.println("Server is starting but no user is currently registered: use strongboxadd ");
            System.out.println("utility to add some users and then restart server");
            System.out.println("Enjoy your Strongbox experience!");
            conf.saveToFile(CONF_NAME);
        } else conf = Servconfig.getFromFile(CONF_NAME);
    }
}
