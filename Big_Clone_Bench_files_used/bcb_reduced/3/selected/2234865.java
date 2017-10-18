package org.expasy.jpl.tools.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.expasy.jpl.commons.app.AbstractApplicationParameters;
import org.expasy.jpl.commons.app.JPLTerminalApplication;
import org.expasy.jpl.commons.base.task.TerminalProgressBar;

/**
 * This application recomputes the scan indices in mzXML files.
 * 
 * @author nikitin
 * 
 * @version 1.0.0
 * 
 */
public class MzXMLReIndexer implements JPLTerminalApplication {

    private final Parameters params;

    MzXMLReIndexer(final String[] args) {
        params = new Parameters(this.getClass(), args);
    }

    public static void main(final String[] args) {
        final MzXMLReIndexer app = new MzXMLReIndexer(args);
        app.params.setHeader(app.getClass().getSimpleName() + " v" + app.params.getVersion() + " developed by Fred Nikitin.\n" + "Copyright (c) 2011 Proteome Informatics Group in Swiss " + "Institute of Bioinformatics.\n");
        try {
            if (app.params.isVerbose()) {
                System.out.println(app.params.getParamInfos());
            }
            app.run();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    @Override
    public AbstractApplicationParameters getParameters() {
        return params;
    }

    @Override
    public void run() throws Exception {
        Pattern startScanPattern = Pattern.compile("<scan num=\"(\\d+)\"");
        Pattern endMsRunPattern = Pattern.compile("</msRun>");
        RandomAccessFile rin = new RandomAccessFile(params.getMzXMLFile(), "rw");
        File outFile = new File(params.getOutputFileName());
        if (params.isVerbose()) {
            System.out.println("Processing bytes...");
        }
        TerminalProgressBar progressBar = null;
        if (params.isVerbose()) {
            progressBar = TerminalProgressBar.newInstance(0, (int) rin.length());
        }
        if (outFile.exists()) {
            outFile.delete();
        }
        RandomAccessFile rout = new RandomAccessFile(outFile, "rw");
        FileChannel channel = rin.getChannel();
        String line = null;
        Map<Integer, Long> offsets = new HashMap<Integer, Long>();
        long lastLineOffset = 0;
        while ((line = rin.readLine()) != null) {
            Matcher startScanMatcher = startScanPattern.matcher(line);
            Matcher endMsRunMatcher = endMsRunPattern.matcher(line);
            if (startScanMatcher.find()) {
                offsets.put(Integer.parseInt(startScanMatcher.group(1)), lastLineOffset);
            } else if (endMsRunMatcher.find()) {
                break;
            }
            lastLineOffset = rin.getFilePointer();
            if (params.isVerbose()) {
                progressBar.setValue((int) lastLineOffset);
            }
        }
        long indexOffset = rin.getFilePointer();
        if (params.isVerbose()) {
            progressBar.setValue((int) indexOffset);
        }
        channel.transferTo(0, indexOffset, rout.getChannel());
        byte[] indexBytes = makeScanIndices(offsets, indexOffset);
        rout.getChannel().write(ByteBuffer.wrap(indexBytes), indexOffset);
        long sha1Offset = indexOffset + indexBytes.length;
        String sha1 = "29688c4392a9a81c83e0d18b5779d6aaf6089d3f";
        StringBuilder sb = new StringBuilder(sha1);
        sb.append("</sha1>\n</mzXML>\n");
        rout.getChannel().write(ByteBuffer.wrap(sb.toString().getBytes()), sha1Offset);
        if (params.isVerbose()) {
            progressBar.setValue((int) rin.length());
        }
    }

    /**
	 * Generate the byte offsets of each scan.
	 * 
	 * @param offsets the scan offsets.
	 * @param indexOffset the index offset.
	 * @return the end of mzXML document from <index name="scan"> element in
	 *         bytes.
	 */
    private static byte[] makeScanIndices(Map<Integer, Long> offsets, long indexOffset) {
        StringBuilder sb = new StringBuilder();
        sb.append("<index name=\"scan\">\n");
        List<Integer> scanIds = new ArrayList<Integer>(offsets.keySet());
        Collections.sort(scanIds);
        for (Integer scanId : scanIds) {
            sb.append("  <offset id=\"");
            sb.append(scanId);
            sb.append("\">");
            sb.append(offsets.get(scanId));
            sb.append("</offset>\n");
        }
        sb.append("  </index>\n");
        sb.append("  <indexOffset>");
        sb.append(indexOffset);
        sb.append("</indexOffset>\n");
        sb.append("  <sha1>");
        return sb.toString().getBytes();
    }

    public static String generateSha1(byte[] input) throws Exception {
        System.out.println("input byte length: " + input.length);
        MessageDigest hash = MessageDigest.getInstance("SHA1");
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(input);
        DigestInputStream digestInputStream = new DigestInputStream(byteArrayInputStream, hash);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int ch;
        while ((ch = digestInputStream.read()) >= 0) {
            System.out.println((char) ch);
            byteArrayOutputStream.write(ch);
        }
        byte[] newInput = byteArrayOutputStream.toByteArray();
        System.out.println("in digest : " + new String(digestInputStream.getMessageDigest().digest()));
        byteArrayOutputStream = new ByteArrayOutputStream();
        DigestOutputStream digestOutputStream = new DigestOutputStream(byteArrayOutputStream, hash);
        digestOutputStream.write(newInput);
        digestOutputStream.close();
        String sha1 = new String(digestOutputStream.getMessageDigest().digest());
        System.out.println("out digest: " + sha1);
        return sha1;
    }
}
