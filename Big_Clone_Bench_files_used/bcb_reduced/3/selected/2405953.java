package net.sf.wtk.ant.namespaces;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.w3c.dom.Document;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.SAXException;

public class Namespaces4AntTask extends Task {

    private File srcFile;

    private File destFile;

    public void setSrcFile(File srcFile) {
        this.srcFile = srcFile;
    }

    public void setDestFile(File destFile) {
        this.destFile = destFile;
    }

    @Override
    public void execute() throws BuildException {
        try {
            File thisFile = new File(getLocation().getFileName());
            if (srcFile == null) {
                srcFile = new File(thisFile.getParentFile(), "build.xml");
            }
            if (destFile == null) {
                destFile = new File(thisFile.getParentFile(), "build.import.xml");
            }
            if (destFile.exists() && (srcFile.lastModified() <= destFile.lastModified())) {
                return;
            }
            System.out.println("Transforming " + srcFile + " to " + destFile);
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            builderFactory.setNamespaceAware(false);
            DocumentBuilder documentBuilder = builderFactory.newDocumentBuilder();
            Document srcDoc = documentBuilder.parse(srcFile);
            Document destDoc = documentBuilder.newDocument();
            NameSpaceIntroducer transformer = new NameSpaceIntroducer(srcFile);
            destDoc = (Document) transformer.transform(srcDoc, destDoc);
            if (destDoc.getFeature("Core", "3.0") == null) {
                throw new BuildException("Document object model included in the current Java runtime does not support version 3.0.");
            }
            DOMImplementationLS lsImplementation = (DOMImplementationLS) destDoc.getImplementation().getFeature("LS", "3.0");
            LSSerializer serializer = lsImplementation.createLSSerializer();
            boolean updatedFromWithinInclude = getOwningTarget().getName().contains(":");
            final byte[] oldHash;
            if (destFile.exists()) {
                if (updatedFromWithinInclude) {
                    oldHash = getTextHash(destFile);
                } else {
                    oldHash = null;
                }
                destFile.delete();
            } else {
                oldHash = null;
            }
            LSOutput output = lsImplementation.createLSOutput();
            FileOutputStream outputStream = new FileOutputStream(destFile);
            output.setByteStream(outputStream);
            output.setEncoding("UTF-8");
            serializer.write(destDoc, output);
            outputStream.close();
            destFile.setReadOnly();
            checkRealUpdate: if (updatedFromWithinInclude) {
                if (oldHash != null) {
                    byte[] newHash = getTextHash(destFile);
                    if (Arrays.equals(oldHash, newHash)) {
                        break checkRealUpdate;
                    } else {
                        throw new BuildException("Imported build script was updated. Please re-build.");
                    }
                }
                throw new BuildException("Imported build script of dependency was out of date. Please re-build.");
            }
        } catch (ParserConfigurationException ex) {
            throw new BuildException(ex);
        } catch (SAXException ex) {
            throw new BuildException(ex);
        } catch (IOException ex) {
            throw new BuildException(ex);
        }
    }

    private byte[] getTextHash(File file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.reset();
            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
            try {
                while (true) {
                    String line = in.readLine();
                    if (line == null) {
                        break;
                    }
                    byte[] buffer = line.getBytes();
                    digest.update(buffer);
                }
            } finally {
                in.close();
            }
            return digest.digest();
        } catch (NoSuchAlgorithmException ex) {
            throw new BuildException("Cannot compute file hash.", ex);
        } catch (IOException ex) {
            throw new BuildException("Cannot compute file hash.", ex);
        }
    }
}
