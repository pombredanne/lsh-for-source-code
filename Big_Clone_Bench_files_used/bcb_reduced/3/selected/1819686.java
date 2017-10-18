package com.google.code.p.keytooliui.ktl.util.jarsigner;

import com.google.code.p.keytooliui.shared.lang.*;
import com.google.code.p.keytooliui.shared.swing.optionpane.*;
import com.google.code.p.keytooliui.shared.util.jarsigner.*;
import sun.misc.BASE64Encoder;
import com.google.code.p.keytooliui.shared.security.util.MySignatureFile;
import sun.security.util.ManifestDigester;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.jar.*;

public abstract class KTLKprOpenSignAbs extends KTLKprOpenAbs {

    private static final String _f_s_strClass = "com.google.code.p.keytooliui.ktl.util.jarsigner.KTLKprOpenSignAbs.";

    private static final String _f_s_strDigestAlgoSuffix = "-Digest";

    private static BASE64Encoder _s_b64 = new BASE64Encoder();

    /**
        if any code error, exit
        else if any error, return nil (calling method should display error dialog) 
    **/
    private static String _s_updateDigest(MessageDigest mdtMessageDigest, InputStream ism) {
        String strMethod = KTLKprOpenSignAbs._f_s_strClass + "_s_updateDigest(...)";
        if (mdtMessageDigest == null || ism == null) MySystem.s_printOutExit(strMethod, "nil arg");
        byte[] bytsBuffer = new byte[com.google.code.p.keytooliui.shared.util.jar.S_JarOutputStream.f_s_intLengthBytsBuffer];
        int intChr = 0;
        try {
            while ((intChr = ism.read(bytsBuffer)) > 0) mdtMessageDigest.update(bytsBuffer, 0, intChr);
            ism.close();
        } catch (IOException excIO) {
            excIO.printStackTrace();
            MySystem.s_printOutError(strMethod, "excIO caught");
            return null;
        }
        return KTLKprOpenSignAbs._s_b64.encode(mdtMessageDigest.digest());
    }

    private static Manifest _s_getManifestFile(JarFile jfeInput) {
        String strMethod = _f_s_strClass + "_s_getManifestFile(...)";
        if (jfeInput == null) MySystem.s_printOutExit(strMethod, "nil jfeInput");
        JarEntry jeyFileManifest = jfeInput.getJarEntry(com.google.code.p.keytooliui.shared.util.jar.S_Manifest.f_s_strPathRelManifest);
        if (jeyFileManifest == null) {
            return new Manifest();
        }
        Manifest manManifest = new Manifest();
        Enumeration enuEntries = jfeInput.entries();
        while (enuEntries.hasMoreElements()) {
            JarEntry jeyCur = (JarEntry) enuEntries.nextElement();
            if (!com.google.code.p.keytooliui.shared.util.jar.S_Manifest.f_s_strPathRelManifest.equalsIgnoreCase(jeyCur.getName())) continue;
            try {
                InputStream ismCur = jfeInput.getInputStream(jeyCur);
                manManifest.read(ismCur);
            } catch (IOException excIO) {
                excIO.printStackTrace();
                MySystem.s_printOutError(strMethod, "excIO caught");
                return null;
            }
            break;
        }
        return manManifest;
    }

    private static Map<String, Attributes> _s_getMapCleanedUpManifest(Manifest manManifest, JarFile jfeInput) {
        String strMethod = _f_s_strClass + "_s_getMapCleanedUpManifest(...)";
        if (manManifest == null || jfeInput == null) MySystem.s_printOutExit(strMethod, "nil arg");
        Map<String, Attributes> mapManifestEntries = manManifest.getEntries();
        Iterator itr = mapManifestEntries.keySet().iterator();
        while (itr.hasNext()) {
            String str = (String) itr.next();
            if (jfeInput.getEntry(str) == null) itr.remove();
        }
        return mapManifestEntries;
    }

    private static boolean _s_writeJarEntry(Frame frmOwner, JarEntry jey, JarFile jfeInput, JarOutputStream josOutput) {
        String strMethod = _f_s_strClass + "_s_writeJarEntry(...)";
        if (jey == null || jfeInput == null || josOutput == null) MySystem.s_printOutExit(strMethod, "nil arg");
        if (!com.google.code.p.keytooliui.shared.util.jar.S_JarOutputStream.s_writeEntry(josOutput, jey, jfeInput, frmOwner)) {
            MySystem.s_printOutError(strMethod, "failed, jey.getName()=" + jey.getName() + ", jfeInput.getName()=" + jfeInput.getName());
            String strBody = "Failed to sign jar file!";
            OPAbstract.s_showDialogError(frmOwner, strBody);
            return false;
        }
        return true;
    }

    private static Map<String, Attributes> _s_createEntries(Manifest manManifest, JarFile jfeInput) {
        String strMethod = _f_s_strClass + "_s_createEntries(...)";
        if (manManifest == null) MySystem.s_printOutExit(strMethod, "nil manManifest");
        if (manManifest.getEntries().size() > 0) return _s_getMapCleanedUpManifest(manManifest, jfeInput);
        com.google.code.p.keytooliui.shared.util.jar.S_Manifest.s_fill(manManifest);
        return manManifest.getEntries();
    }

    private static boolean _s_updateManifestDigest(Manifest manManifest, JarFile jfeInput, MessageDigest mdtMessageDigest, Map<String, Attributes> mapEntries) {
        String strMethod = _f_s_strClass + "_s_updateManifestDigest(...)";
        if (manManifest == null || jfeInput == null || mdtMessageDigest == null || mapEntries == null) {
            MySystem.s_printOutExit(strMethod, "nil arg");
        }
        Enumeration enuEntry = jfeInput.entries();
        while (enuEntry.hasMoreElements()) {
            JarEntry jeyCur = (JarEntry) enuEntry.nextElement();
            if (jeyCur.getName().startsWith(com.google.code.p.keytooliui.shared.util.jar.S_Manifest.f_s_strDirParentManifest)) continue; else if (manManifest.getAttributes(jeyCur.getName()) != null) {
                InputStream ismCur = null;
                try {
                    ismCur = jfeInput.getInputStream(jeyCur);
                } catch (IOException excIO) {
                    excIO.printStackTrace();
                    MySystem.s_printOutError(strMethod, "excIO caught");
                    return false;
                }
                String strValue = _s_updateDigest(mdtMessageDigest, ismCur);
                if (strValue == null) {
                    MySystem.s_printOutError(strMethod, "nil strValue");
                    return false;
                }
                Attributes attAttributes = manManifest.getAttributes(jeyCur.getName());
                attAttributes.putValue(UtilCrtX509.f_s_strDigestAlgoSHA1 + KTLKprOpenSignAbs._f_s_strDigestAlgoSuffix, strValue);
            } else if (!jeyCur.isDirectory()) {
                InputStream ismCur = null;
                try {
                    ismCur = jfeInput.getInputStream(jeyCur);
                } catch (IOException excIO) {
                    excIO.printStackTrace();
                    MySystem.s_printOutError(strMethod, "excIO caught");
                    return false;
                }
                String strValue = _s_updateDigest(mdtMessageDigest, ismCur);
                if (strValue == null) {
                    MySystem.s_printOutError(strMethod, "nil strValue");
                    return false;
                }
                Attributes attAttributes = new Attributes();
                attAttributes.putValue(UtilCrtX509.f_s_strDigestAlgoSHA1 + KTLKprOpenSignAbs._f_s_strDigestAlgoSuffix, strValue);
                mapEntries.put(jeyCur.getName(), attAttributes);
            }
        }
        return true;
    }

    protected String _strPathAbsOpenJarSource_ = null;

    protected String _strPathAbsSaveJarTarget_ = null;

    protected boolean _doJob_(File fleOpenJarUnsigned, File fleSaveJarSigned, KeyStore kstOpen, String strAliasKpr, PrivateKey keyPrivateKpr) {
        String strMethod = "_doJob_(...)";
        X509Certificate[] crtsX509Unordered = UtilCrtX509.s_getX509CertificateChain(kstOpen, strAliasKpr, false);
        if (crtsX509Unordered == null) {
            MySystem.s_printOutError(this, strMethod, "nil crtsX509Unordered");
            return false;
        }
        JarFile jfeInput = null;
        try {
            jfeInput = new JarFile(fleOpenJarUnsigned);
        } catch (IOException excIO) {
            excIO.printStackTrace();
            MySystem.s_printOutError(this, strMethod, "excIO caught");
            String strBody = "Got IO exception";
            strBody += "\n" + excIO.getMessage();
            strBody += "\n\n  Unsigned JAR path=" + fleOpenJarUnsigned.getAbsolutePath();
            return false;
        }
        JarOutputStream jos = com.google.code.p.keytooliui.shared.util.jar.S_JarOutputStream.s_create(fleSaveJarSigned);
        if (jos == null) {
            MySystem.s_printOutError(this, strMethod, "failed");
            String strBody = "Failed to sign JAR.";
            strBody += "\n  " + fleSaveJarSigned.getAbsolutePath();
            OPAbstract.s_showDialogError(super._frmOwner_, strBody);
            return false;
        }
        if (!_assignSigFileBaseName(strAliasKpr)) {
            MySystem.s_printOutError(this, strMethod, "failed");
            return false;
        }
        if (!_signJarFile(jfeInput, jos, keyPrivateKpr, crtsX509Unordered)) {
            MySystem.s_printOutError(this, strMethod, "failed");
            return false;
        }
        return true;
    }

    protected KTLKprOpenSignAbs(Frame frmOwner, String strPathAbsOpenKst, char[] chrsPasswdOpenKst, String strProviderKst, String strPathAbsOpenJarSource, String strPathAbsSaveJarTarget, String strNameBaseSigFile) {
        super(frmOwner, strPathAbsOpenKst, chrsPasswdOpenKst, strProviderKst);
        this._strPathAbsOpenJarSource_ = strPathAbsOpenJarSource;
        this._strPathAbsSaveJarTarget_ = strPathAbsSaveJarTarget;
        this._strNameBaseSigFile = strNameBaseSigFile;
    }

    private String _strNameBaseSigFile = null;

    /** TEMPO*/
    private boolean _signJarFile(JarFile jfeInput, JarOutputStream jos, PrivateKey keyPrivate, X509Certificate[] crtsX509) {
        String strMethod = "_signJarFile(...)";
        try {
            Manifest manManifest = _s_getManifestFile(jfeInput);
            if (manManifest == null) {
                MySystem.s_printOutError(this, strMethod, "nil manManifest");
                String strBody = "Failed to sign jar file!";
                OPAbstract.s_showDialogError(super._frmOwner_, strBody);
                return false;
            }
            Map<String, Attributes> mapEntries = _s_createEntries(manManifest, jfeInput);
            if (mapEntries == null) {
                MySystem.s_printOutError(this, strMethod, "nil mapEntries");
                String strBody = "Failed to sign jar file!";
                OPAbstract.s_showDialogError(super._frmOwner_, strBody);
                return false;
            }
            MessageDigest mdtMessageDigest = MessageDigest.getInstance(UtilCrtX509.f_s_strDigestAlgoSHA1);
            if (!_s_updateManifestDigest(manManifest, jfeInput, mdtMessageDigest, mapEntries)) {
                MySystem.s_printOutError(this, strMethod, "! _s_updateManifestDigest(manManifest, jfeInput, mdtMessageDigest, mapEntries)");
                String strBody = "Failed to sign jar file!";
                OPAbstract.s_showDialogError(super._frmOwner_, strBody);
                return false;
            }
            MySignatureFile sfeSignatureFile = _createSignatureFile(manManifest, mdtMessageDigest);
            if (sfeSignatureFile == null) {
                MySystem.s_printOutError(this, strMethod, "nil sfeSignatureFile");
                String strBody = "Failed to sign jar file!";
                OPAbstract.s_showDialogError(super._frmOwner_, strBody);
                return false;
            }
            String strAlgoSignature = new String(crtsX509[0].getSigAlgName());
            MySignatureFile.Block blkSignatureFileBlock = sfeSignatureFile.generateBlock(keyPrivate, crtsX509, true, strAlgoSignature);
            if (!com.google.code.p.keytooliui.shared.util.jar.S_JarOutputStream.s_writeManifest(jos, manManifest, super._frmOwner_)) {
                MySystem.s_printOutError(this, strMethod, "failed");
                return false;
            }
            if (!com.google.code.p.keytooliui.shared.util.jar.S_JarOutputStream.s_writeEntry(jos, sfeSignatureFile, super._frmOwner_)) {
                MySystem.s_printOutError(this, strMethod, "failed");
                return false;
            }
            if (!com.google.code.p.keytooliui.shared.util.jar.S_JarOutputStream.s_writeEntry(jos, blkSignatureFileBlock, super._frmOwner_)) {
                MySystem.s_printOutError(this, strMethod, "failed");
                return false;
            }
            String strMetaNameSigFile = sfeSignatureFile.getMetaName();
            String strMetaNameSigFileBlock = blkSignatureFileBlock.getMetaName();
            Enumeration metaEntries = jfeInput.entries();
            while (metaEntries.hasMoreElements()) {
                JarEntry jeyMetaInf = (JarEntry) metaEntries.nextElement();
                if (jeyMetaInf.getName().startsWith(com.google.code.p.keytooliui.shared.util.jar.S_Manifest.f_s_strDirParentManifest) && !(com.google.code.p.keytooliui.shared.util.jar.S_Manifest.f_s_strPathRelManifest.equalsIgnoreCase(jeyMetaInf.getName()) || strMetaNameSigFile.equalsIgnoreCase(jeyMetaInf.getName()) || strMetaNameSigFileBlock.equalsIgnoreCase(jeyMetaInf.getName()))) {
                    if (!_s_writeJarEntry(super._frmOwner_, jeyMetaInf, jfeInput, jos)) {
                        MySystem.s_printOutError(this, strMethod, "failed");
                        return false;
                    }
                }
            }
            Enumeration enuEntry = jfeInput.entries();
            while (enuEntry.hasMoreElements()) {
                JarEntry jey = (JarEntry) enuEntry.nextElement();
                if (!jey.getName().startsWith(com.google.code.p.keytooliui.shared.util.jar.S_Manifest.f_s_strDirParentManifest)) {
                    if (!_s_writeJarEntry(super._frmOwner_, jey, jfeInput, jos)) {
                        MySystem.s_printOutError(this, strMethod, "failed");
                        return false;
                    }
                }
            }
            if (!com.google.code.p.keytooliui.shared.util.jar.S_JarOutputStream.s_close(jos)) {
                MySystem.s_printOutError(this, strMethod, "failed");
                String strBody = "Failed to sign JAR.";
                OPAbstract.s_showDialogError(super._frmOwner_, strBody);
                return false;
            }
            jos = null;
            jfeInput.close();
        } catch (Exception exc) {
            exc.printStackTrace();
            MySystem.s_printOutError(this, strMethod, "exc caught");
            String strBody = "Got exception";
            strBody += "\n  " + exc.getMessage();
            strBody += "\n\nMore in session.log";
            OPAbstract.s_showDialogError(super._frmOwner_, strBody);
            return false;
        }
        return true;
    }

    /**
        if sigFileNameBase already assigned, check for valid ~
        else uses JarSigner's algo:
        EXCERPT FROM J2SDK 1.4.0 doc, jarsigner:
        
            << If no -sigfile option appears on the command line, 
            the base file name for the .SF and .DSA files will be the first 8 characters 
            of the alias name specified on the command line, 
            all converted to upper case. 
            
            If the alias name has fewer than 8 characters, the full alias name is used. 
            
            If the alias name contains any characters that are not allowed in a signature file name, 
            each such character is converted to an underscore ("_") character in forming the file name. 
            Legal characters include letters, digits, underscores, and hyphens. >>
        
    **/
    private boolean _assignSigFileBaseName(String strAliasKpr) {
        String strMethod = "_assignSigFileBaseName(strAliasKpr)";
        if (strAliasKpr == null) {
            MySystem.s_printOutError(this, strMethod, "nil strAliasKpr");
            return false;
        }
        if (this._strNameBaseSigFile != null) {
            if (com.google.code.p.keytooliui.ktl.util.filter.StringFilterUI.s_isAllowedSigfile(this._strNameBaseSigFile)) return true;
            MySystem.s_printOutError(this, strMethod, "not allowed value, this._strNameBaseSigFile=" + this._strNameBaseSigFile);
            return false;
        }
        String str = new String();
        String strAccepted = com.google.code.p.keytooliui.shared.swing.text.PlainDocumentFilter.f_s_strAlphaNumeric;
        strAccepted += "_";
        strAccepted += "-";
        for (int i = 0; i < strAliasKpr.length(); i++) {
            if (i > 7) break;
            String strCur = strAliasKpr.substring(i, i + 1);
            if (strAccepted.indexOf(strCur) == -1) str += "_"; else str += strCur.toUpperCase();
        }
        this._strNameBaseSigFile = str;
        return true;
    }

    /** TEMPO*/
    private MySignatureFile _createSignatureFile(Manifest manManifest, MessageDigest mdtMessageDigest) {
        String strMethod = "_createSignatureFile(...)";
        byte[] bytsManifest = com.google.code.p.keytooliui.shared.util.jar.S_Manifest.s_toByteArray(manManifest, super._frmOwner_);
        if (bytsManifest == null) {
            MySystem.s_printOutError(this, strMethod, "nil bytsManifest");
            return null;
        }
        ManifestDigester mdrManifestDigester = new ManifestDigester(bytsManifest);
        if (this._strNameBaseSigFile == null) {
            MySystem.s_printOutError(this, strMethod, "nil this._strNameBaseSigFile");
            return null;
        }
        MessageDigest[] mdts = new MessageDigest[] { mdtMessageDigest };
        return new MySignatureFile(mdts, manManifest, mdrManifestDigester, this._strNameBaseSigFile, true);
    }
}
