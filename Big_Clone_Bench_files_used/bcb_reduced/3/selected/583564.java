package com.jcompressor.type;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrBuilder;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import com.jcompressor.JcompressorContext;
import com.jcompressor.entities.YuiOptions;
import com.jcompressor.exceptions.JcompressorException;
import com.jcompressor.utils.ListOfFiles;

/**
 * @author Scott Carnett
 */
public abstract class JcompressorType implements Runnable {

    private JcompressorContext jcompressorContext;

    private String fileName;

    private String extension;

    private LinkedList<InputStream> streams;

    private YuiOptions yui;

    /**
	 * @see java.lang.Runnable#run()
	 */
    @Override
    public void run() {
        if (this.merge()) {
            this.compress();
        }
    }

    /**
	 * Merges some file streams
	 */
    private boolean merge() {
        try {
            final SequenceInputStream stream = new SequenceInputStream(new ListOfFiles(this.streams));
            final MessageDigest digester = MessageDigest.getInstance("MD5");
            final OutputStream mergedOutput = new FileOutputStream(this.getMergedOutputFile());
            byte[] buffer = new byte[4096];
            int length = 0;
            while ((length = stream.read(buffer)) > 0) {
                digester.update(buffer, 0, buffer.length);
                mergedOutput.write(buffer, 0, length);
            }
            final String checksum = new BigInteger(1, digester.digest()).toString(16);
            try {
                final SAXBuilder builder = new SAXBuilder();
                final Document document = builder.build(this.getJcompressorContext().getHomePath() + "/resources.xml");
                final Element rootNode = document.getRootElement();
                final List<Object> templates = rootNode.getChildren("template");
                for (final Object templateObj : templates) {
                    final Element templateElement = (Element) templateObj;
                    if (StringUtils.equals("template", templateElement.getName())) {
                        final String uri = templateElement.getAttributeValue("uri");
                        System.out.println(uri);
                        if (StringUtils.equals(this.getJcompressorContext().getUri(), uri)) {
                            for (final Object resourceObj : templateElement.getChildren("resource")) {
                                final Element resourceElement = (Element) resourceObj;
                                System.out.println(resourceElement.getChildText("file"));
                                System.out.println(resourceElement.getChildText("checksum"));
                            }
                        }
                    }
                }
            } catch (JDOMException e) {
                e.printStackTrace();
            }
            stream.close();
            mergedOutput.close();
            return true;
        } catch (IOException e) {
            throw new JcompressorException("An error has occurred while concatenating file streams", e);
        } catch (NoSuchAlgorithmException e) {
            throw new JcompressorException(e);
        }
    }

    public abstract void compress();

    public JcompressorContext getJcompressorContext() {
        return this.jcompressorContext;
    }

    public String getFileName() {
        return this.fileName;
    }

    public String getExtension() {
        return this.extension;
    }

    public String getOutputPath() {
        final StrBuilder builder = new StrBuilder();
        builder.append(this.getJcompressorContext().getHomePath());
        builder.append('/');
        builder.append(this.getFileName());
        builder.append('.');
        builder.append(this.getExtension());
        return builder.toString();
    }

    public File getOutputFile() {
        return new File(this.getOutputPath());
    }

    public String getMergedOutputPath() {
        final StrBuilder builder = new StrBuilder(this.getOutputPath());
        builder.append(".merged");
        return builder.toString();
    }

    public File getMergedOutputFile() {
        return new File(this.getMergedOutputPath());
    }

    public LinkedList<InputStream> getStreams() {
        return this.streams;
    }

    public YuiOptions getYui() {
        return this.yui;
    }

    public void setJcompressorContext(final JcompressorContext jcompressorContext) {
        this.jcompressorContext = jcompressorContext;
    }

    public void setFileName(final String fileName) {
        this.fileName = fileName;
    }

    public void setExtension(final String extension) {
        this.extension = extension;
    }

    public void setStreams(final LinkedList<InputStream> streams) {
        this.streams = streams;
    }

    public void setYui(final YuiOptions yui) {
        this.yui = yui;
    }
}
