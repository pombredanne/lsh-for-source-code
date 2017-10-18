package net.sourceforge.liftoff.builder;

import java.util.Hashtable;
import java.util.Vector;
import java.util.Enumeration;
import java.util.Properties;
import java.util.StringTokenizer;
import net.sourceforge.liftoff.builder.zip.*;
import java.io.*;
import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.*;
import java.beans.PropertyChangeListener;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 *
 *
 */
public class StructDescr {

    private static String dummyBean = "installer_properties";

    private static SwingPropertyChangeSupport psup = new SwingPropertyChangeSupport(dummyBean);

    /**
     * A Hashtable for the fields in a structure.
     */
    private static Hashtable structs = null;

    /**
     * The current database of properties.
     */
    private static Properties data = null;

    /**
     * true if data were modified sinc las save.
     */
    private static boolean modified = false;

    private static Hashtable stdlist = new Hashtable();

    /** location for files */
    private static String buildDir = "";

    /**
     * the package tree
     */
    private static RootNode packageTree = null;

    /**
     * a vector of installation types
     */
    private static TypeListModel installTypes = new TypeListModel();

    private static DefaultTreeModel packageTreeModel = null;

    /** current language prefix */
    private static String langPrefix = "";

    private static String[] installableTypes = { "archive", "exec", "file", "lib", "template" };

    private static String[] langPrefixArray = { "en", "de" };

    /**
     * The constructor for this class is private because there is no
     * instance for this class.
     */
    private StructDescr() {
    }

    ;

    public static String getBuildDir() {
        return buildDir;
    }

    public static String[] getInstallableTypes() {
        return installableTypes;
    }

    /**
     * return true if the data was modified.
     */
    public static boolean isModified() {
        return modified;
    }

    /**
     * set the current language prefix.
     */
    public static void setLangPrefix(String newPf) {
        if (newPf == null) newPf = "";
        langPrefix = newPf;
        psup.firePropertyChange("", null, null);
        setProperty("language", newPf);
    }

    public static String[] getLangArray() {
        return langPrefixArray;
    }

    /**
     * Initialize field descriptions for all structs.
     */
    private static void init() {
        structs = new Hashtable();
        FieldDescr[] product = { new FieldDescr("product", "author", "Author", "", false), new FieldDescr("product", "string", "Description", "", false), new FieldDescr("product", "version", "Version", "", false), new FieldDescr("java", "minVersion", "Java version required", "", false), new FieldDescr("product", "root", "Project root", "net.sourceforge.liftoff.builder.DirnameEditor", false), new FieldDescr("destination", "package_prefix", "default prefix", "", false), new FieldDescr("license", "licenseText", "License text", "", false), new FieldDescr("product", "readmeText", "Readme text", "", false), new FieldDescr("builder", "extra_files", "Extra file list", "net.sourceforge.liftoff.builder.FilenameEditor", false), new FieldDescr("product", "urlbase", "URL to download this software", "", false), new FieldDescr("uninstall", "location", "The location of the Uninstall.Class file", "net.sourceforge.liftoff.builder.LocationEditor", false), new FieldDescr("card", "skipClasspathCard", "Skip the classpath editor", "net.sourceforge.liftoff.builder.BooleanEditor", false), new FieldDescr("destination", "standardIfRoot", "Standard destinations, even when root", "net.sourceforge.liftoff.builder.BooleanEditor", false), new FieldDescr("image", "side", "Side Image", "", false) };
        structs.put("product", product);
        FieldDescr[] pack = { new FieldDescr("product.package", "label", "Label", "", false), new FieldDescr("product.package", "required", "required", "net.sourceforge.liftoff.builder.BooleanEditor", false), new FieldDescr("product.package", "getFromURL", "Get from URL", "net.sourceforge.liftoff.builder.BooleanEditor", false) };
        structs.put("package", pack);
        FieldDescr[] type = { new FieldDescr("product.type", "label", "Label", "", false), new FieldDescr("product.type", "contains", "packages", "", false), new FieldDescr("product.type", "default", "is default", "net.sourceforge.liftoff.builder.BooleanEditor", false) };
        structs.put("type", type);
        FieldDescr[] archive = { new FieldDescr("archive", "location", "location", "net.sourceforge.liftoff.builder.LocationEditor", false), new FieldDescr("archive", "name", "target name", "", false), new FieldDescr("archive", "package", "package", "", false), new FieldDescr("archive", "source", "source file", "net.sourceforge.liftoff.builder.FilenameEditor", false) };
        structs.put("archive", archive);
        FieldDescr[] libs = { new FieldDescr("lib", "archive", "archive name", "", false), new FieldDescr("lib", "class", "class to search", "", false), new FieldDescr("lib", "package", "package", "", false), new FieldDescr("lib", "location", "location", "net.sourceforge.liftoff.builder.LocationEditor", false), new FieldDescr("lib", "install_missing", "target (if missing)", "", false), new FieldDescr("lib", "source", "source file", "net.sourceforge.liftoff.builder.FilenameEditor", false) };
        structs.put("lib", libs);
        FieldDescr[] template = { new FieldDescr("template", "location", "location", "net.sourceforge.liftoff.builder.LocationEditor", false), new FieldDescr("template", "target", "target name", "", false), new FieldDescr("template", "package", "package", "", false), new FieldDescr("template", "template", "template name", "", false), new FieldDescr("template", "source", "source file", "net.sourceforge.liftoff.builder.FilenameEditor", false) };
        structs.put("template", template);
        FieldDescr[] exec = { new FieldDescr("exec", "class", "java class", "", false), new FieldDescr("exec", "location", "location", "net.sourceforge.liftoff.builder.LocationEditor", false), new FieldDescr("exec", "target", "target name", "", false), new FieldDescr("exec", "package", "package", "", false), new FieldDescr("exec", "template.unix", "unix template", "", false), new FieldDescr("exec", "template.win", "win template", "", false), new FieldDescr("exec", "source", "source file", "net.sourceforge.liftoff.builder.FilenameEditor", false) };
        structs.put("exec", exec);
        FieldDescr[] file = { new FieldDescr("file", "location", "location", "net.sourceforge.liftoff.builder.LocationEditor", false), new FieldDescr("file", "target", "target name", "", false), new FieldDescr("file", "package", "package", "", false), new FieldDescr("file", "source", "source file", "net.sourceforge.liftoff.builder.FilenameEditor", false) };
        structs.put("file", file);
    }

    /**
     * Get the field descriptions for a structure. <p>
     *
     * This method returns null if the structure was not found.
     *
     * @param structName The name of the structure.
     * @return an array of field descriptions or null on error.
     */
    public static FieldDescr[] getStruct(String structName) {
        if (structs == null) init();
        return (FieldDescr[]) structs.get(structName);
    }

    /**
     * Load the installer properties.
     *
     * @param from The filename to load.
     */
    public static void loadProperties(String from) {
        File inFile = new File(from);
        if (!inFile.exists()) {
            return;
        }
        inFile = new File(inFile.getAbsolutePath());
        buildDir = inFile.getParent() + File.separator;
        System.err.println("build dir is " + buildDir);
        try {
            FileInputStream is = null;
            is = new FileInputStream(from);
            BufferedReader bi = new BufferedReader(new InputStreamReader(is));
            String magicLine = bi.readLine();
            if (!magicLine.startsWith("#liftoff dist file")) {
                System.err.println("Not a liftoff properties file");
                return;
            }
            is.close();
            is = new FileInputStream(from);
            data = new Properties();
            data.load(is);
            is.close();
        } catch (IOException e) {
            System.err.println("can not load installer properties");
        }
        if (data.get("product.root") != null) {
            buildDir = (String) data.get("product.root") + File.separator;
        }
        if (packageTreeModel == null) initPackageTree();
        packageTree.clear();
        packageTree.getPackages();
        for (int i = 0; i < installableTypes.length; i++) {
            String it = installableTypes[i];
            String items = getProperty(it + "s");
            if (items == null) continue;
            StringTokenizer tok = new StringTokenizer(items, ",");
            while (tok.hasMoreTokens()) {
                String inst = tok.nextToken();
                String pkg = getProperty(it + "." + inst + ".package");
                if (pkg == null) {
                    System.err.println("No package for installable " + it + "." + inst);
                    continue;
                }
                PackageNode pn = packageTree.getPackageNode(pkg);
                if (pn == null) {
                    System.err.println("Package " + pkg + " not found for installable " + it + "." + inst);
                    continue;
                }
                InstallableNode inf = new InstallableNode(inst, it);
                pn.addNode(inf);
            }
        }
        packageTreeModel.reload();
        Enumeration en = data.keys();
        while (en.hasMoreElements()) {
            String name = (String) en.nextElement();
            psup.firePropertyChange(name, null, data.get(name));
        }
        installTypes.removeAll();
        String types = getProperty("product.types");
        if (types != null) {
            StringTokenizer tok = new StringTokenizer(types, ",");
            while (tok.hasMoreTokens()) {
                installTypes.addElement(tok.nextToken());
            }
        }
        System.err.println("structure loaded");
        modified = false;
        psup.firePropertyChange("", null, null);
    }

    /**
     * store the installer properties.
     */
    public static void saveProperties(String filename) {
        try {
            FileOutputStream os = new FileOutputStream(filename);
            saveProperties(os);
            os.close();
        } catch (IOException e) {
            System.err.println("can not save installer properties");
        }
        modified = false;
        psup.firePropertyChange("", null, null);
    }

    /**
     * Save the installer properties to the given output stream.
     *
     * @param os Stream to write to.
     */
    public static void saveProperties(OutputStream os) {
        PrintStream out = new PrintStream(os);
        out.println("#liftoff dist file");
        FieldDescr[] fields = StructDescr.getStruct("product");
        for (int i = 0; i < fields.length; i++) {
            out.println(fields[i]);
        }
        installTypes.save(out);
        for (int i = 0; i < installableTypes.length; i++) {
            String list = packageTree.getInstallableIdents(installableTypes[i]);
            if (!list.equals("")) {
                out.println(installableTypes[i] + "s=" + list);
            }
        }
        packageTree.save(out);
        out.flush();
    }

    /**
     * add a package to the package tree.
     *
     * @param package_name name of this package.
     */
    public static void addPackage(String packageName) {
        addStruct("package", packageName);
        packageTree.addPackage(packageName);
        packageTreeModel.reload();
    }

    /**
     * Add an installable to a package. <p>
     *
     * This method adds an a new leaf node to the package tree.
     *
     * @param name Identifier for the new package.
     * @param struct Name of the structure to add.
     * @param pkg    Name of the package the installable should be in.
     */
    public static void addInstallable(String name, String struct, String pkg) {
        addStruct(struct, name);
        setProperty(struct + "." + name + ".package", pkg);
        PackageNode pn = packageTree.getPackageNode(pkg);
        InstallableNode inf = new InstallableNode(name, struct);
        pn.addNode(inf);
        packageTreeModel.reload();
    }

    /**
     * Remove a node from the package tree.
     *
     * @param node Reference of the node ro delete.
     */
    public static void removeNode(TreeNode node) {
        if (node instanceof PackageNode) {
            packageTree.removePackage((PackageNode) node);
        } else if (node instanceof InstallableNode) {
            PackageNode pn = (PackageNode) node.getParent();
            pn.removeNode((InstallableNode) node);
        }
        packageTreeModel.reload();
    }

    /**
     * Initialize the data model for the package tree.
     */
    private static void initPackageTree() {
        packageTree = new RootNode();
        packageTreeModel = new DefaultTreeModel(packageTree, true);
    }

    /**
     * Get the root of the package tree.
     */
    public static TreeModel getPackageTreeModel() {
        if (packageTreeModel == null) initPackageTree();
        return packageTreeModel;
    }

    /**
     * get the list model for the type lists.
     */
    public static ListModel getTypeListModel() {
        return installTypes;
    }

    /**
     * Add an isntallation type.
     */
    public static void addInstallType(String type) {
        if (installTypes.contains(type)) {
            JOptionPane.showMessageDialog(null, "there is already a type " + type, "duplicate", JOptionPane.ERROR_MESSAGE);
            return;
        }
        addStruct("type", type);
        installTypes.addElement(type);
    }

    /**
     * Remove an isntallation type.
     */
    public static void removeInstallType(String type) {
        installTypes.removeElement(type);
    }

    /**
     * Get a property by its name and language. <p>
     *
     * This method returns null if the property is not set for 
     * the given language.
     *
     */
    public static String getProperty(String name, String lang) {
        if (data == null) return null;
        String xname = name;
        if (lang.length() != 0) {
            xname = lang + "." + xname;
        }
        return data.getProperty(xname);
    }

    /**
     * Get a property from data.
     *
     * If the property is not found for the current lang prefix
     * try to find it in tha default language.
     *
     */
    public static String getProperty(String name) {
        if (data == null) return null;
        String result = null;
        result = getProperty(name, langPrefix);
        if (result != null) return result;
        return data.getProperty(name, "");
    }

    /**
     * Set a property in data.
     *
     * @param name the property name.
     * @param value the value for this property.
     */
    public static void setProperty(String name, String value) {
        if (data == null) data = new Properties();
        Object old = data.get(name);
        if (langPrefix.length() != 0) {
            name = langPrefix + "." + name;
        }
        data.put(name, value);
        modified = true;
        psup.firePropertyChange(name, old, value);
    }

    /**
     * Add an empty structure to the properties.<p>
     *
     * @param struct the name of the structure.
     * @param ident  an identifier to add.
     */
    public static void addStruct(String struct, String ident) {
        FieldDescr[] fields = getStruct(struct);
        for (int i = 0; i < fields.length; i++) {
            fields[i].setValue(ident, "");
        }
    }

    /**
     * Remove structure from the "data" properties.
     *
     * @param struct the name of the structure.
     * @param ident  an identifier to add.
     */
    public static void removeStruct(String struct, String ident) {
    }

    public static void addPropertyChangeListener(PropertyChangeListener listener) {
        psup.addPropertyChangeListener(listener);
    }

    public static void addPropertyChangeListener(String propname, PropertyChangeListener listener) {
        psup.addPropertyChangeListener(propname, listener);
    }

    public static void removePropertyChangeListener(PropertyChangeListener listener) {
        psup.removePropertyChangeListener(listener);
    }

    public static void removePropertyChangeListener(String propname, PropertyChangeListener listener) {
        psup.removePropertyChangeListener(propname, listener);
    }

    /**
     * create a list of files to be put into the archive.
     * <p>
     * This is done by 
     * <ul>
     *   <li>get the content of the standard list.
     *   <li>add the files from the file list for the current project.
     *   <li>add the files from packages of the current project.
     * </ul>
     * The list itself is a hashtable, the key is the target name 
     * (name in archive).
     */
    public static Hashtable createFileList(boolean withInstaller) {
        Hashtable result;
        if (withInstaller) {
            result = (Hashtable) stdlist.clone();
        } else {
            result = new Hashtable();
        }
        String extraName = getProperty("builder.extra_files");
        if ((extraName != null) && (extraName.length() > 0)) {
            Hashtable extraFiles = readFileList(extraName, null);
            Enumeration en = extraFiles.elements();
            while (en.hasMoreElements()) {
                FileRec rec = (FileRec) en.nextElement();
                if (!withInstaller && rec.getLocation().equals("installer")) continue;
                result.put(rec.getLocation() + "/" + rec.getTargetName(), rec);
            }
        }
        packageTree.addFilesToList(result);
        return result;
    }

    /**
     * create an distribution zip archive.<p>
     *
     *
     * @param os         an output stream to write the archive to.
     * @param fileList   a vector of FileRec's that describe wich files to 
     *                   put into the archive.
     * @param cic        if not null, the data from this stream will 
     *                   placed at the begining of the output stream.
     * @param instOnly   only write files for the package 'installer'.
     */
    public static void writeZip(OutputStream os, Hashtable fileList, InputStream cic, String newClassName, boolean instOnly) {
        if (cic != null) {
            Main.log.println("adding class file ");
            try {
                ClassRenamer.copyClass(cic, os, newClassName);
            } catch (IOException e) {
                Main.log.println("Can not write zip : " + e);
                return;
            }
        }
        ZipOutputStream myZip = new ZipOutputStream(os);
        myZip.setLevel(9);
        try {
            Main.log.println("add installer properties");
            ZipEntry ze = new ZipEntry("installer.properties");
            myZip.putNextEntry(ze);
            saveProperties(myZip);
            myZip.closeEntry();
        } catch (IOException e) {
            Main.log.println("Can not write zip : " + e);
            return;
        }
        Vector values = new Vector(fileList.values());
        java.util.Collections.sort(values);
        Enumeration en = values.elements();
        while (en.hasMoreElements()) {
            FileRec rec = (FileRec) en.nextElement();
            MessageDigest sha = null;
            if (rec.getFileSize() < 0) {
                Main.log.println(">>>>\nOoops :  can not find " + rec.getSourceName() + "\n>>>> skip this file");
                continue;
            }
            try {
                sha = MessageDigest.getInstance("SHA-1");
            } catch (NoSuchAlgorithmException e) {
                System.err.println("can't get instance of an SHA-1 Digest Object");
                sha = null;
            }
            byte[] buffer = new byte[4096];
            if (instOnly && rec.getFromURL()) {
                try {
                    Main.log.println("Calculate message digest for " + rec.getSourceName());
                    FileInputStream is = new FileInputStream(rec.getSourceName());
                    long size = 0;
                    int bytes = 0;
                    while ((bytes = is.read(buffer)) > 0) {
                        if (sha != null) sha.update(buffer, 0, bytes);
                        size += bytes;
                    }
                    is.close();
                    rec.setSize(size);
                    byte[] hash = sha.digest();
                    rec.setDigest(hash);
                } catch (IOException e) {
                    Main.log.println("Can not calculate message digest : " + e);
                }
                continue;
            }
            Main.log.println("adding " + rec.getSourceName());
            ZipEntry ze;
            if (rec.getLocation().equals("_top_")) {
                ze = new ZipEntry(rec.getTargetName());
            } else {
                ze = new ZipEntry(rec.getLocation() + "/" + rec.getTargetName());
            }
            try {
                FileInputStream is = new FileInputStream(rec.getSourceName());
                myZip.putNextEntry(ze);
                long size = 0;
                int bytes = 0;
                while ((bytes = is.read(buffer)) > 0) {
                    if (sha != null) sha.update(buffer, 0, bytes);
                    myZip.write(buffer, 0, bytes);
                    size += bytes;
                }
                is.close();
                myZip.closeEntry();
                rec.setSize(size);
                byte[] hash = sha.digest();
                rec.setDigest(hash);
            } catch (IOException e) {
                Main.log.println("Can not write zip : " + e);
            }
        }
        Main.log.println("adding filelist");
        try {
            ZipEntry ze = new ZipEntry("installer.filelist");
            myZip.putNextEntry(ze);
            PrintStream ps = new PrintStream(myZip);
            en = values.elements();
            while (en.hasMoreElements()) {
                FileRec rec = (FileRec) en.nextElement();
                ps.println(rec.asListEntry());
            }
            myZip.closeEntry();
        } catch (IOException e) {
            Main.log.println("Can not write filelist : " + e);
        }
        try {
            myZip.close();
        } catch (IOException e) {
            Main.log.println("Can not close zip : " + e);
        }
    }

    /**
     * Create a Zip Archive.<p>
     *
     * @param ZipName file to create.
     * @param cic     an InputStream wich data should be placed at the 
     *                start of the zip archive.
     */
    public static void createZip(String zipName, InputStream cic) {
        Hashtable fileList = createFileList(false);
        File zipFile = new File(zipName);
        if (zipFile.exists()) {
            Object[] options = { "Overwrite", "Cancel" };
            int answer = JOptionPane.showOptionDialog(null, "a File named " + zipName + " already exists, overwrite it ?", "File exists", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
            if (answer != 0) return;
        }
        try {
            if (zipFile.exists()) zipFile.delete();
            RandomAccessFile rf = new RandomAccessFile(zipName, "rw");
            SeekableOutputStream os = new SeekableOutputStream(rf);
            int prio = Thread.currentThread().getPriority();
            Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
            writeZip(os, fileList, null, null, false);
            Thread.currentThread().setPriority(prio);
            os.close();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "can not open file " + zipName + " : " + e, "failed", JOptionPane.ERROR_MESSAGE);
            return;
        }
    }

    /**
     * Create a self extracting class.
     */
    public static void createClass(String className, String outdir, String newClassName, boolean notAll) {
        if (!outdir.endsWith(File.separator)) outdir = outdir + File.separator;
        File cf = new File(className);
        String outname = null;
        InputStream cis = null;
        Hashtable fileList = createFileList(true);
        if (cf.exists() && cf.canRead()) {
            if (newClassName == null) outname = outdir + cf.getName(); else outname = outdir + newClassName + ".class";
            try {
                cis = new FileInputStream(className);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "can not open file " + className + " : " + e, "failed", JOptionPane.ERROR_MESSAGE);
                return;
            }
            File zipFile = new File(outname);
            if (zipFile.exists()) {
                Object[] options = { "Overwrite", "Cancel" };
                int answer = JOptionPane.showOptionDialog(null, "a File named " + outname + " already exists, overwrite it ?", "File exists", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
                if (answer != 0) return;
            }
            try {
                if (zipFile.exists()) zipFile.delete();
                RandomAccessFile rf = new RandomAccessFile(outname, "rw");
                SeekableOutputStream os = new SeekableOutputStream(rf);
                int prio = Thread.currentThread().getPriority();
                Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
                writeZip(os, fileList, cis, newClassName, notAll);
                Thread.currentThread().setPriority(prio);
                os.close();
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "can not create file " + outname + " : " + e, "failed", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } else {
            JOptionPane.showMessageDialog(null, "can not open file " + className, "failed", JOptionPane.ERROR_MESSAGE);
            return;
        }
    }

    /**
     * read the list of standard files to place in the
     * archive.
     */
    public static void readStdList() {
        String fname = Main.properties.getProperty("stdlist");
        if (fname == null) {
            JOptionPane.showMessageDialog(null, "no name for the standard file list given", "warning", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String dname = Main.properties.getProperty("installer");
        if (dname == null) {
            JOptionPane.showMessageDialog(null, "no directory for the installer given", "warning", JOptionPane.ERROR_MESSAGE);
            return;
        }
        stdlist = readFileList(fname, null);
    }

    /**
     * read a file list.<p>
     *
     * If no absolute pathname given, use the directory of the list
     * as the pathname for the source.
     *
     * @param fname name of the list file
     * @param path  absolute path to the files in the list.
     */
    public static Hashtable readFileList(String fname, String path) {
        if (path == null) {
            File listfile = new File(fname);
            listfile = new File(listfile.getAbsolutePath());
            path = listfile.getParent();
        }
        if (!path.endsWith(File.separator)) {
            path += File.separator;
        }
        Hashtable result = new Hashtable();
        InputStream is = null;
        try {
            is = new FileInputStream(fname);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "can not open file \n    \"" + fname + "\" \njava exception :\n    " + e, "failed", JOptionPane.ERROR_MESSAGE);
            return result;
        }
        String pkg = "installer";
        String loc = "installer";
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String str = null;
            while ((str = br.readLine()) != null) {
                str = str.trim();
                if (str.length() == 0) continue;
                if (str.startsWith("#")) continue;
                if (str.startsWith("@")) {
                    str = str.substring(1);
                    StringTokenizer tok = new StringTokenizer(str, ":");
                    if (tok.countTokens() < 2) {
                        System.err.println("Error in this line : \n   \"" + str + "\nformat is @<package>:<location>");
                    }
                    if (tok.countTokens() > 2) {
                        System.err.println("Warning to many tokens while reading " + "a file list (in this line) \n   \"" + str + "\"");
                    }
                    loc = tok.nextToken();
                    pkg = tok.nextToken();
                    continue;
                }
                StringTokenizer tok = new StringTokenizer(str);
                int tokens = tok.countTokens();
                if (tokens > 3) {
                    System.err.println("to many tokens while reading a file list " + "(in this line) \n   \"" + str + "\"");
                }
                if (tokens < 2) {
                    System.err.println("not enough tokens, skip");
                    continue;
                }
                int type = FileRec.FT_BINARY;
                String ft = tok.nextToken();
                if ("t".equals(ft)) {
                    type = FileRec.FT_TEXT;
                }
                String target = tok.nextToken();
                String source = "";
                if (tokens > 2) {
                    source = tok.nextToken();
                    if (!new File(source).isAbsolute()) {
                        source = path + source;
                    }
                } else {
                    source = path + target;
                }
                result.put(target, new FileRec(source, target, pkg, loc, type));
            }
            is.close();
        } catch (IOException e) {
        }
        return result;
    }
}
