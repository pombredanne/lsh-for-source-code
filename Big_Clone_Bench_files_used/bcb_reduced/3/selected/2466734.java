package org.zeleos.zwt.tools.packager.zecma262;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.logging.Logger;
import org.zeleos.zwt.parser.zecma262.ASTZInclude;
import org.zeleos.zwt.parser.zecma262.ASTZNamespace;
import org.zeleos.zwt.parser.zecma262.ASTZStatement;
import org.zeleos.zwt.parser.zecma262.ParseException;
import org.zeleos.zwt.parser.zecma262.SimpleNode;
import org.zeleos.zwt.parser.zecma262.ZECMA262;
import org.zeleos.zwt.parser.zecma262.ZECMA262VisitorException;
import org.zeleos.zwt.serializer.zecma262.ZECMA262SerializerVisitor;
import org.zeleos.zwt.tools.merger.zecma262.ZECMA262MergerVisitor;
import org.zeleos.zwt.tools.merger.zecma262.ZECMA262ZIncludeComparator;
import org.zeleos.zwt.tools.merger.zecma262.ZECMA262ZNamespaceComparator;
import org.zeleos.zwt.tools.packager.Packager;
import org.zeleos.zwt.tools.packager.PackagerException;

/**
 * <p>A ZECMA262 packager is used to package a ZECMA262 class.</p>
 * 
 * <p>It is possible to obfuscate and optimize the output.</p>
 * 
 * <p>The result of the packaging operation can be one or more JavaScript 
 * files (if the <code>splitOutput</code> is set to true). The file is named 
 * by the qualified name of the class to package: 
 * <code>[namespace].[className].js</code></p>
 * 
 * <p>If the output is split then it generates one file per class path in a 
 * directory following the same naming convention as above. The name of the 
 * files are a Digest of their content.</p>
 * 
 * @author Jeremy KUHN
 *
 */
public class ZECMA262Packager implements Packager {

    private static Logger LOGGER = Logger.getLogger(ZECMA262Packager.class.getName());

    private String[] zwtClassPath;

    private Set<ASTZInclude> includes;

    private Set<ASTZNamespace> namespaces;

    private ZECMA262SerializerVisitor serializeVisitor;

    private Writer data;

    private HashMap<String, StringWriter> zwtPathData;

    private boolean obfuscate = true;

    private boolean splitOutput = false;

    /**
	 * Constructs a ZECMA262 packager.
	 */
    public ZECMA262Packager() {
        this(null);
    }

    /**
	 * Constructs a ZECMA262 packager.
	 * @param zwtClassPath The paths to the classes to package.
	 */
    public ZECMA262Packager(String[] zwtClassPath) {
        this.zwtClassPath = zwtClassPath;
        this.init();
    }

    /**
	 * Initialize the packager.
	 */
    private void init() {
        LOGGER.info("==== Zeleos ZECMA262 Packager ====");
        StringBuilder cp = new StringBuilder();
        for (String classPath : this.zwtClassPath) {
            cp.append(classPath).append(";");
        }
        LOGGER.info("Class path: " + cp);
        this.zwtPathData = new HashMap<String, StringWriter>();
        this.reset();
        LOGGER.info("--------------------------------------------------------------------------------");
    }

    /**
	 * Reset the packager.
	 */
    public void reset() {
        this.includes = new TreeSet<ASTZInclude>(new ZECMA262ZIncludeComparator());
        this.namespaces = new TreeSet<ASTZNamespace>(new ZECMA262ZNamespaceComparator());
    }

    public void packageClass(String zwtClass, File outputDir) throws PackagerException {
        LOGGER.info("Packaging " + zwtClass);
        try {
            this.serializeVisitor = new ZECMA262SerializerVisitor();
            this.serializeVisitor.setFormat(false);
            this.serializeVisitor.setIncludeComments(false);
            if (this.obfuscate) {
                this.serializeVisitor.setOptimize(true);
            } else {
                this.serializeVisitor.setOptimize(false);
            }
            outputDir.mkdirs();
            File mergeFile = new File(outputDir, zwtClass + ".js");
            this.data = new PrintWriter(mergeFile);
            this.packageClass(zwtClass);
            this.data.flush();
            this.data.close();
        } catch (IOException e) {
            throw new PackagerException("Error writing output.", e);
        }
        if (this.splitOutput) {
            File pathOutputDir = new File(outputDir, zwtClass);
            pathOutputDir.mkdir();
            for (Entry<String, StringWriter> entry : this.zwtPathData.entrySet()) {
                FileWriter pathOutputWriter;
                try {
                    String keyDigest = this.digest(entry.getValue().toString());
                    File pathMergeFile = new File(pathOutputDir, keyDigest + ".js");
                    pathOutputWriter = new FileWriter(pathMergeFile);
                } catch (NoSuchAlgorithmException e) {
                    throw new PackagerException("Error writing path output.", e);
                } catch (IOException e) {
                    throw new PackagerException("Error writing path output.", e);
                }
                try {
                    pathOutputWriter.write(entry.getValue().toString());
                } catch (IOException e) {
                    throw new PackagerException("Error writing path output.", e);
                } finally {
                    try {
                        pathOutputWriter.flush();
                        pathOutputWriter.close();
                    } catch (IOException e) {
                    }
                }
            }
        }
    }

    /**
	 * Digests the input.
	 * @param input The input to digest.
	 * @return The digest string
	 * @throws NoSuchAlgorithmException
	 * @throws UnsupportedEncodingException
	 */
    private String digest(String input) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] md5hash = new byte[64];
        md.update(input.getBytes("iso-8859-1"), 0, input.length());
        md5hash = md.digest();
        return this.convertToHex(md5hash);
    }

    /**
	 * Converts the data to an hex string.
	 * @param data The data to convert.
	 * @return The Hex string.
	 */
    private String convertToHex(byte[] data) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9)) buf.append((char) ('0' + halfbyte)); else buf.append((char) ('a' + (halfbyte - 10)));
                halfbyte = data[i] & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }

    /**
	 * Packages the specified class.
	 * @param zwtClass The class to package.
	 * @throws PackagerException If an error occurred during the packaging 
	 * operation.
	 */
    private void packageClass(String zwtClass) throws PackagerException {
        zwtClass = zwtClass.trim();
        URL jsURL = null;
        InputStream jsInput = null;
        StringWriter pathData = null;
        for (String jsClassPathEntry : this.zwtClassPath) {
            try {
                if (!jsClassPathEntry.trim().endsWith("/")) {
                    jsClassPathEntry = jsClassPathEntry + "/";
                }
                URL pathURL = new URL(jsClassPathEntry.trim());
                pathData = this.zwtPathData.get(pathURL.toString());
                if (pathData == null) {
                    pathData = new StringWriter();
                    this.zwtPathData.put(pathURL.toString(), pathData);
                    pathData.append("/* ").append(pathURL.toString()).append(" */");
                    pathData.append(System.getProperty("line.separator"));
                    pathData.append(System.getProperty("line.separator"));
                }
                jsURL = new URL(jsClassPathEntry.trim() + zwtClass.replace('.', '/') + ".js");
                jsInput = jsURL.openStream();
                break;
            } catch (MalformedURLException e) {
                throw new PackagerException("Error opening connection to: " + zwtClass + " (" + jsURL + ")", e);
            } catch (IOException e) {
            }
        }
        if (jsInput == null) {
            throw new PackagerException("Class " + zwtClass + " not found.");
        }
        try {
            ZECMA262 sourceParser = new ZECMA262(jsInput);
            ZECMA262MergerVisitor mergerVisitor = new ZECMA262MergerVisitor();
            SimpleNode n1 = sourceParser.ZStart();
            n1.jjtAccept(mergerVisitor, null);
            for (Iterator<ASTZInclude> includesIterator = mergerVisitor.getIncludes().iterator(); includesIterator.hasNext(); ) {
                ASTZInclude current = includesIterator.next();
                if (this.includes.contains(current)) {
                    includesIterator.remove();
                }
            }
            for (ASTZInclude jsInclude : mergerVisitor.getIncludes()) {
                if (this.includes.add(jsInclude)) {
                    this.packageClass(((String) ((SimpleNode) jsInclude.jjtGetChild(0)).jjtGetFirstToken().image).replace("'", ""));
                }
            }
            if (this.namespaces.add(mergerVisitor.getNamespace())) {
                mergerVisitor.getNamespace().jjtAccept(this.serializeVisitor, this.data);
                mergerVisitor.getNamespace().jjtAccept(this.serializeVisitor, pathData);
            }
            for (ASTZStatement node : mergerVisitor.getStatements()) {
                if (node != null) {
                    node.jjtAccept(this.serializeVisitor, this.data);
                    node.jjtAccept(this.serializeVisitor, pathData);
                }
            }
        } catch (ParseException e) {
            throw new PackagerException("Error parsing class: " + zwtClass + " (" + jsURL + ")", e);
        } catch (ZECMA262VisitorException e) {
            throw new PackagerException("Error parsing class: " + zwtClass + " (" + jsURL + ")", e);
        }
    }

    /**
	 * Returns the paths to the ZWt classes.
	 * @return The ZWT class path.
	 */
    public String[] getJsClassPath() {
        return zwtClassPath;
    }

    /**
	 * Sets the ZWT class path.
	 * @param jsClassPath The ZWT class path to set.
	 */
    public void setJsClassPath(String[] jsClassPath) {
        this.zwtClassPath = jsClassPath;
    }

    /**
	 * Determines whether the packager obfuscates the output.
	 * @return true if the packager obfuscates the output, false otherwise.
	 */
    public boolean isObfuscate() {
        return obfuscate;
    }

    /**
	 * Sets the packager to obfuscate the output.
	 * @param obfuscate true to obfuscate the output, false otherwise.
	 */
    public void setObfuscate(boolean obfuscate) {
        this.obfuscate = obfuscate;
    }

    /**
	 * Determines whether the packager splits the output. 
	 * @return true if the packager splits the output, false otherwise. 
	 */
    public boolean isSplitOutput() {
        return splitOutput;
    }

    /**
	 * Sets the packager to split the output.
	 * @param splitOutput true to split the output, false otherwise.
	 */
    public void setSplitOutput(boolean splitOutput) {
        this.splitOutput = splitOutput;
    }
}
