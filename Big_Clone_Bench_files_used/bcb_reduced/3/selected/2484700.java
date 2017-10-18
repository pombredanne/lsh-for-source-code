package com.ibm.aglets.tahiti;

import java.net.URL;
import java.security.cert.Certificate;
import com.ibm.awb.misc.Archive;
import com.ibm.awb.misc.Archive.Entry;
import com.ibm.awb.misc.JarArchive;
import com.ibm.maf.ClassName;

class JarAgletClassLoader extends AgletClassLoader {

    JarArchive _jar = null;

    JarAgletClassLoader(String name, Certificate cert) throws java.io.IOException {
        this(new URL(name), cert);
    }

    JarAgletClassLoader(URL codebase, Certificate cert) throws java.io.IOException {
        super(checkAndTrim(codebase), cert);
        this._jar = new com.ibm.awb.misc.JarArchive(codebase.openStream());
        Archive.Entry ae[] = this._jar.entries();
        this._digest_table = new DigestTable(ae.length);
        for (Entry element : ae) {
            this._digest_table.setDigest(element.name(), element.digest());
        }
    }

    private static URL checkAndTrim(URL codeBase) throws java.io.IOException {
        String f = codeBase.getFile();
        if ((f != null) && f.toLowerCase().endsWith(".jar")) {
            System.out.println(f);
            f = f.substring(0, f.lastIndexOf('/') + 1);
            System.out.println(f);
            return new URL(codeBase, f);
        }
        return codeBase;
    }

    @Override
    public Archive getArchive(ClassName[] t) {
        if (this.match(t)) {
            return this._jar;
        } else {
            return null;
        }
    }

    @Override
    protected synchronized byte[] getResourceAsByteArray(String filename) {
        return this._jar.getResourceAsByteArray(filename);
    }

    @Override
    public void importArchive(Archive a) {
        Archive.Entry ae[] = a.entries();
        for (Entry element : ae) {
            long digest = this._digest_table.getDigest(element.name());
            if (digest == 0) {
                throw new RuntimeException("Cannot Add JarArchive!");
            }
        }
    }

    static boolean isJarFile(URL codebase) {
        String f = codebase.getFile();
        return (f != null) && f.toLowerCase().endsWith(".jar");
    }
}
