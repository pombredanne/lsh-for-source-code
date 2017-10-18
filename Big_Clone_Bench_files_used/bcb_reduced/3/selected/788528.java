package net.sourceforge.liftoff.installer.items;

import java.io.*;
import net.sourceforge.liftoff.installer.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.DigestOutputStream;

/**
 * an template is similar to a file but instead simple
 * copy operations it needs some text replacement.
 */
public class InstallableTemplate extends FSInstallable {

    /** location of the template input */
    protected String temploc = "";

    /**
     * create a new InstallableTemplate.
     *
     * @param ident    an identifier for this object.
     * @param source   the name of the source.
     * @param location the location where this file should be copied to.
     * @param target   the name of the target.
     * @param temploc  the location for the template.
     * @param size     size of the template.
     */
    public InstallableTemplate(String ident, String source, String location, String target, String temploc, long size) {
        super(ident, source, location, target, size);
        this.temploc = temploc;
    }

    public static InstallableTemplate createFromProperties(InstallableContainer cont, String ident) {
        String location = Info.getInstProperty("template." + ident + ".location");
        String pkg = Info.getInstProperty("template." + ident + ".package");
        String target = Info.getInstProperty("template." + ident + ".target");
        String template = Info.getInstProperty("template." + ident + ".template");
        if (template == null) {
            System.err.println("no template name given for template " + ident);
            return null;
        }
        if (pkg == null) pkg = "base";
        if (target == null) {
            System.err.println("no target name given for template " + ident);
            return null;
        }
        if (location == null) {
            System.err.println("no location name given for template " + ident);
            return null;
        }
        InstallableFile fi = cont.lookupFile(template);
        if (fi == null) {
            System.err.println("no file named " + template + " in the filelist");
            return null;
        }
        long size = fi.getSize();
        InstallableTemplate result = new InstallableTemplate(ident, template, location, target, fi.location, size);
        result.setPackage(pkg);
        return result;
    }

    /**
     * call this method from the install method to do a 
     * substitution operation for this installable.
     *
     * FIXME: need to calculate the MD on the InputStream too.
     *
     * @param moni an Install Monitor that monitors the copy
     *             operation.
     */
    protected boolean subst(InstallMonitor moni) throws AbortInstallException {
        String fullName = Info.getSystemActions().getTargetName(location, target);
        moni.showCopyOp(source, fullName);
        if (!backupFile(moni, fullName)) return false;
        MessageDigest sha = null;
        try {
            sha = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            System.err.println("can get instance of an SHA-1 Digest Object");
            sha = null;
        }
        InputStream is = null;
        boolean retry = true;
        while (retry) {
            try {
                is = Info.getInstallationSource().getFile(source, temploc);
                retry = false;
            } catch (IOException e) {
                retry = moni.showIOException(e);
                if (retry == false) {
                    throw new AbortInstallException();
                }
            }
        }
        if (is == null) {
            System.err.println("can not open template " + source + " for substitution");
            return false;
        }
        OutputStream os = Info.getSystemActions().openOutputFile(location, target);
        if (os == null) {
            System.err.println("can not open target target " + fullName + " for substitution");
            return false;
        }
        OutputStream ds = null;
        if (sha != null) {
            ds = new DigestOutputStream(os, sha);
            ((DigestOutputStream) ds).on(true);
        } else {
            ds = os;
        }
        SubstFilter subst = new SubstFilter(is);
        subst.setOutput(new PrintStream(ds));
        try {
            subst.scan();
            ds.flush();
            ds.close();
            is.close();
        } catch (IOException e) {
            System.err.println("error while copying from" + source + " to " + fullName + ": " + e);
            return false;
        }
        if (sha != null) {
            instdigest = sha.digest();
        }
        installedName = fullName;
        wasInstalled = true;
        File instf = new File(fullName);
        lastModified = instf.lastModified();
        size = instf.length();
        return true;
    }

    /**
     * Install the template.
     */
    public boolean install(InstallMonitor moni) throws AbortInstallException {
        return subst(moni);
    }

    public String toString() {
        return "InstallableTemplate(" + ident + " source = " + source + " target = " + location + ":" + target + " size = " + size + ")";
    }
}
