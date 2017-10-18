package net.sourceforge.retriever.fetcher.resource;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * <code>ResourceMonitor</code> implementation that keeps its state in main memory.
 */
public class MemoryResourceMonitor implements ResourceMonitor {

    private MessageDigest messageDigest;

    private final Map<String, MonitoringInfo> monitoringInfoByURL = Collections.synchronizedMap(new HashMap<String, MonitoringInfo>());

    /**
	 * Creates a <code>MemoryResourceMonitor</code> object.
	 */
    public MemoryResourceMonitor() {
        try {
            this.messageDigest = MessageDigest.getInstance("SHA-1");
        } catch (final NoSuchAlgorithmException e) {
        }
    }

    /**
	 * Adds monitoring information to some resource.
	 * 
	 * @param resource The resource being monitored.
	 */
    public void addMonitoringInfo(final Resource resource) {
        final String url = resource.getURL().toExternalForm();
        final MonitoringInfo monitoringInfo = getMonitoringInfo(url);
        monitoringInfo.setContentFresh(this.contentChanged(url, this.getContent(resource), monitoringInfo));
        resource.setMonitoringInfo(monitoringInfo);
    }

    private MonitoringInfo getMonitoringInfo(final String url) {
        MonitoringInfo monitoringInfo = this.monitoringInfoByURL.get(url);
        if (monitoringInfo == null) {
            monitoringInfo = new MonitoringInfo();
            this.monitoringInfoByURL.put(url, monitoringInfo);
        }
        return monitoringInfo;
    }

    private boolean contentChanged(final String url, final String content, final MonitoringInfo monitoringInfo) {
        messageDigest.update(content.getBytes());
        final String contentRepresentation = new String(messageDigest.digest()).trim();
        final String storedContentRepresentation = monitoringInfo.getContentRepresentation();
        final boolean contentChanged = !contentRepresentation.equals(storedContentRepresentation);
        if (contentChanged) {
            monitoringInfo.setContentRepresentation(contentRepresentation);
            monitoringInfo.addDateToChangeHistory(new Date());
        }
        return contentChanged;
    }

    private String getContent(final Resource resource) {
        if (resource.getInputStream() == null) return "";
        try {
            final StringBuilder content = new StringBuilder();
            this.parseInputStreamIntoStringBuilder(resource.getInputStream(), content, resource.getCharset());
            return content.toString();
        } catch (final IOException e) {
            return "";
        } finally {
            try {
                resource.getInputStream().reset();
            } catch (final IOException e) {
            }
        }
    }

    private void parseInputStreamIntoStringBuilder(final InputStream inputStream, final StringBuilder content, final String charset) throws IOException {
        final int BUFFER_SIZE = 2048;
        final byte[] buffer = new byte[BUFFER_SIZE];
        int length = inputStream.read(buffer, 0, BUFFER_SIZE);
        while (length > -1) {
            content.append(new String(buffer, 0, length, (charset == null ? "UTF-8" : charset)));
            length = inputStream.read(buffer, 0, BUFFER_SIZE);
        }
    }
}
