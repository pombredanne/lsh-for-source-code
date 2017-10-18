package org.homedns.krolain.MochaJournal;

import org.homedns.krolain.MochaJournal.Protocol.ProxySettings;
import java.util.prefs.*;
import org.homedns.krolain.util.InstallInfo;
import java.util.Vector;
import org.homedns.krolain.MochaJournal.LJData.LJMoods;
import org.homedns.krolain.MochaJournal.LJData.LJMoods.MoodInfo;
import java.util.Locale;
import com.swabunga.spell.engine.SpellDictionaryHashMap;
import java.io.File;

/**
 *
 * @author  jsmith
 */
public class JLJSettings extends java.lang.Object implements java.io.Serializable {

    private static JLJSettings m_Setting = null;

    public Locale m_ProgLocale;

    private String m_szDictFile;

    private SpellDictionaryHashMap m_Dictionary;

    public ProxySettings m_Proxy;

    public int m_iLookFeelIdx;

    public String m_szLookFeelFile;

    public String m_szBrowser;

    public boolean m_bSavePwd;

    public boolean m_bPolling;

    public boolean m_bPollUpdate;

    public int m_iLastUserIdx;

    public Vector m_UsrList;

    public Vector m_PwdList;

    public LJMoods m_Moods;

    public java.awt.Rectangle m_MainWndRect;

    public boolean m_bFirstRun;

    /** Creates a new instance of Settings */
    public JLJSettings() {
        m_ProgLocale = Locale.getDefault();
        m_szDictFile = "en.dict";
        m_iLookFeelIdx = SetupDlg.DEFAULT_INDEX;
        m_szLookFeelFile = "";
        m_Proxy = new ProxySettings();
        m_bFirstRun = true;
        m_bSavePwd = false;
        m_szBrowser = "";
        m_iLastUserIdx = -1;
        m_UsrList = new java.util.Vector();
        m_PwdList = new java.util.Vector();
        m_Moods = new LJMoods();
        m_Dictionary = null;
        java.awt.Dimension dim = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        m_MainWndRect = new java.awt.Rectangle((dim.width - 400) / 2, (dim.height - 500) / 2, 400, 500);
    }

    public static JLJSettings GetSettings() {
        if (m_Setting == null) m_Setting = new JLJSettings();
        return m_Setting;
    }

    public synchronized void LoadDictionary(String dictFile) {
        if ((m_Dictionary == null) || (m_szDictFile.compareToIgnoreCase(dictFile) != 0)) {
            try {
                m_szDictFile = dictFile;
                String szDictFile = InstallInfo.getInstallPath() + "lang" + java.io.File.separatorChar + JLJSettings.GetSettings().m_szDictFile;
                m_Dictionary = new SpellDictionaryHashMap(new File(szDictFile));
            } catch (java.io.IOException e) {
                System.err.println(e);
            } catch (java.lang.Exception e) {
                System.err.println(e);
            }
        }
    }

    public synchronized String GetDictionaryFile() {
        return m_szDictFile;
    }

    public SpellDictionaryHashMap GetDictionary() {
        return m_Dictionary;
    }

    public void getProgLang(String szProgLocale) {
        if (szProgLocale.length() == 0) m_ProgLocale.getDefault(); else {
            String szLang, szCountry;
            szLang = szProgLocale.substring(0, 2);
            if (szProgLocale.indexOf('_') != 0) {
                szCountry = szProgLocale.substring(3, 5);
                m_ProgLocale = new Locale(szLang, szCountry);
            } else m_ProgLocale = new Locale(szLang);
        }
    }

    public void setDicFile(String var) {
        m_szDictFile = var;
    }

    public void setLookFeel(int var) {
        m_iLookFeelIdx = var;
    }

    public void setLookFeelName(String var) {
        m_szLookFeelFile = var;
    }

    public void setBrowser(String var) {
        m_szBrowser = var;
    }

    public void setPollState(boolean var) {
        m_bPolling = var;
    }

    public void setPollUpdate(boolean var) {
        m_bPollUpdate = var;
    }

    public void setSavePwd(boolean var) {
        m_bSavePwd = var;
    }

    public void setLastUsrIdx(int var) {
        m_iLastUserIdx = var;
    }

    public void setWndPos(java.awt.Rectangle var) {
        m_MainWndRect = var;
    }

    public void setMoods(LJMoods var) {
        m_Moods = var;
    }

    public void setProxy(ProxySettings var) {
        m_Proxy = var;
    }

    public void setFirstRun(boolean var) {
        m_bFirstRun = var;
    }

    public void setUserList(Vector var) {
        m_UsrList = var;
    }

    public void setLocale(String szProgLocale) {
        if (szProgLocale.length() == 0) m_ProgLocale.getDefault(); else {
            String szLang, szCountry;
            szLang = szProgLocale.substring(0, 2);
            if (szProgLocale.indexOf('_') != 0) {
                szCountry = szProgLocale.substring(3, 5);
                m_ProgLocale = new Locale(szLang, szCountry);
            } else m_ProgLocale = new Locale(szLang);
        }
    }

    public static boolean LoadSettings() {
        String szPath = System.getProperty("user.home");
        szPath += System.getProperty("file.separator") + "MochaJournal" + System.getProperty("file.separator") + "settings.xml";
        File backup = new File(szPath);
        if (backup.exists()) {
            try {
                java.beans.XMLDecoder decode = new java.beans.XMLDecoder(new java.io.FileInputStream(backup));
                m_Setting = (JLJSettings) decode.readObject();
                decode.close();
            } catch (java.io.FileNotFoundException e) {
                System.err.println(e);
            } catch (java.io.IOException e) {
                System.err.println(e);
            } catch (java.lang.Exception e) {
                System.err.println(e);
            }
        }
        if (m_Setting == null) m_Setting = new JLJSettings();
        if (!InstallInfo.loadInstallPath()) {
            java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("org.homedns.krolain.MochaJournal.JLJ");
            String szMessage = bundle.getString("string.error.no.settings");
            String szTitle = bundle.getString("app.title");
            javax.swing.JOptionPane.showMessageDialog(null, szMessage, szTitle, javax.swing.JOptionPane.ERROR_MESSAGE);
            return false;
        }
        InstallInfo.setBundle("lang/JLJ", "org.homedns.krolain.MochaJournal.JLJ", m_Setting.m_ProgLocale);
        m_Setting.LoadLoginInfo();
        m_Setting.LoadDictionary(m_Setting.m_szDictFile);
        return true;
    }

    public String getProgLang() {
        String szProgLocale;
        szProgLocale = m_ProgLocale.getLanguage();
        if (m_ProgLocale.getCountry().length() != 0) szProgLocale += "_" + m_ProgLocale.getCountry();
        return szProgLocale;
    }

    public String getDicFile() {
        return m_szDictFile;
    }

    public int getLookFeel() {
        return m_iLookFeelIdx;
    }

    public String getLookFeelName() {
        return m_szLookFeelFile;
    }

    public String getBrowser() {
        return m_szBrowser;
    }

    public boolean getPollState() {
        return m_bPolling;
    }

    public boolean getPollUpdate() {
        return m_bPollUpdate;
    }

    public boolean getSavePwd() {
        return m_bSavePwd;
    }

    public int getLastUsrIdx() {
        return m_iLastUserIdx;
    }

    public java.awt.Rectangle getWndPos() {
        return m_MainWndRect;
    }

    public LJMoods getMoods() {
        return m_Moods;
    }

    public ProxySettings getProxy() {
        return m_Proxy;
    }

    public boolean getFirstRun() {
        return m_bFirstRun;
    }

    public Vector getUserList() {
        return m_UsrList;
    }

    public String getLocale() {
        String szProgLocale;
        szProgLocale = m_ProgLocale.getLanguage();
        if (m_ProgLocale.getCountry().length() != 0) szProgLocale += "_" + m_ProgLocale.getCountry();
        return szProgLocale;
    }

    public boolean SaveSettings() {
        String szPath = System.getProperty("user.home");
        szPath += System.getProperty("file.separator") + "MochaJournal";
        File file = new File(szPath);
        file.mkdirs();
        java.io.File backup = new File(file, "settings.xml");
        try {
            if (!backup.exists()) backup.createNewFile();
            java.beans.XMLEncoder encode = new java.beans.XMLEncoder(new java.io.FileOutputStream(backup));
            encode.writeObject(this);
            encode.flush();
            encode.close();
        } catch (java.io.FileNotFoundException e) {
            System.err.println(e);
        } catch (java.io.IOException e) {
            System.err.println(e);
        } catch (java.lang.Exception e) {
            System.err.println(e);
        }
        SaveLoginInfo();
        return true;
    }

    private void LoadLoginInfo() {
        m_PwdList.removeAllElements();
        String szTemp = null;
        int iIndex = 0;
        int iSize = m_UsrList.size();
        for (int i = 0; i < iSize; i++) m_PwdList.add("");
        try {
            if ((m_UsrList.size() > 0) && m_bSavePwd) {
                char[] MD5PWD = new char[80];
                java.util.Arrays.fill(MD5PWD, (char) 0);
                java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-1");
                String szPath = System.getProperty("user.home");
                szPath += System.getProperty("file.separator") + "MochaJournal" + System.getProperty("file.separator") + "user.dat";
                java.io.File file = new java.io.File(szPath);
                if (file.exists()) {
                    java.io.FileInputStream br = new java.io.FileInputStream(file);
                    byte[] szEncryptPwd = null;
                    int iLine = 0;
                    while (br.available() > 0) {
                        md.reset();
                        md.update(((String) m_UsrList.get(iLine)).getBytes());
                        byte[] DESUSR = md.digest();
                        byte alpha = 0;
                        for (int i2 = 0; i2 < DESUSR.length; i2++) alpha += DESUSR[i2];
                        iSize = br.read();
                        if (iSize > 0) {
                            szEncryptPwd = new byte[iSize];
                            br.read(szEncryptPwd);
                            char[] cPwd = new char[iSize];
                            for (int i = 0; i < iSize; i++) {
                                int iChar = (int) szEncryptPwd[i] - (int) alpha;
                                if (iChar < 0) iChar += 256;
                                cPwd[i] = (char) iChar;
                            }
                            m_PwdList.setElementAt(new String(cPwd), iLine);
                        }
                        iLine++;
                    }
                }
            }
        } catch (java.security.NoSuchAlgorithmException e) {
            System.err.println(e);
        } catch (java.io.IOException e3) {
            System.err.println(e3);
        }
    }

    private void SaveLoginInfo() {
        int iSize;
        try {
            if (m_bSavePwd) {
                byte[] MD5PWD = new byte[80];
                java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-1");
                String szPath = System.getProperty("user.home");
                szPath += System.getProperty("file.separator") + "MochaJournal";
                java.io.File file = new java.io.File(szPath);
                if (!file.exists()) file.mkdirs();
                file = new java.io.File(file, "user.dat");
                if (!file.exists()) file.createNewFile();
                java.io.FileOutputStream pw = new java.io.FileOutputStream(file);
                iSize = m_PwdList.size();
                for (int iIndex = 0; iIndex < iSize; iIndex++) {
                    md.reset();
                    md.update(((String) m_UsrList.get(iIndex)).getBytes());
                    byte[] DESUSR = md.digest();
                    byte alpha = 0;
                    for (int i = 0; i < DESUSR.length; i++) alpha += DESUSR[i];
                    String pwd = (String) m_PwdList.get(iIndex);
                    if (pwd.length() > 0) {
                        java.util.Arrays.fill(MD5PWD, (byte) 0);
                        int iLen = pwd.length();
                        pw.write(iLen);
                        for (int i = 0; i < iLen; i++) {
                            int iDiff = (int) pwd.charAt(i) + (int) alpha;
                            int c = iDiff % 256;
                            MD5PWD[i] = (byte) c;
                            pw.write((byte) c);
                        }
                    } else pw.write(0);
                }
                pw.flush();
            }
        } catch (java.security.NoSuchAlgorithmException e) {
            System.err.println(e);
        } catch (java.io.IOException e3) {
            System.err.println(e3);
        }
    }
}
