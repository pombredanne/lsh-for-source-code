package it.aton.proj.dem.configmanager.impl;

import it.aton.proj.dem.commons.util.xml.XMLBuilder;
import it.aton.proj.dem.commons.util.xml.lwxpath.XPath;
import it.aton.proj.dem.configmanager.service.ConfigurationManager;
import it.aton.proj.dem.configmanager.service.XMLConfigurator;
import it.aton.proj.dem.foundation.service.M3Log;
import it.aton.proj.dem.transport.service.Message;
import it.aton.proj.dem.transport.service.QueueConfigException;
import it.aton.proj.dem.transport.service.Transport;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Set;

/**
 * {@inheritDoc}
 */
public class ConfigurationManagerImpl implements ConfigurationManager {

    private static final String CSUM_PASSEPARTOUT = "6184";

    private final Runnable stopper;

    private final Collection<XMLConfigurator> services;

    private final String distroName;

    private final String distroVersion;

    private final Transport transport;

    /**
	 * Costruttore del servizio
	 * 
	 * @param services
	 *            array dei servizi registrati
	 * @param stopper
	 *            thread di esecuzione del sistema
	 */
    public ConfigurationManagerImpl(Collection<XMLConfigurator> services, Thread stopper, String distroName, String distroVersion, Transport transport) {
        this.services = services;
        this.stopper = stopper;
        this.distroName = distroName;
        this.distroVersion = distroVersion;
        this.transport = transport;
        Runtime.getRuntime().addShutdownHook(stopper);
    }

    private static XPath XP_MAC = XPath.compile("/configuration/@mac");

    /**
	 * {@inheritDoc}
	 * 
	 * @throws IOException
	 */
    public void applyXMLConfiguration(String xmlConfig) throws IOException {
        M3Log.debug("*** Configuration reset start");
        try {
            xmlConfig = xmlConfig.trim();
            String csum = XP_MAC.getFirstValue(xmlConfig);
            if (csum == null) throw new IOException("Checksum verify failed.");
            if (!CSUM_PASSEPARTOUT.equals(csum)) {
                xmlConfig = xmlConfig.replace(" mac=\"" + csum + "\"", "");
                String csum2 = calcChecksumNoWhiteSpace(xmlConfig);
                if (!csum2.equals(csum)) throw new IOException("Checksum verify failed.");
            }
            for (XMLConfigurator service : services) if (service != null) service.applyXMLConfiguration(xmlConfig);
            try {
                transport.publish("systemMessages", new Message("SYSTEM", "<event id=\"config_loaded\"/>", -1));
            } catch (QueueConfigException e) {
            }
            M3Log.info("*** Configuration reset complete");
        } catch (IOException ex) {
            M3Log.error("*** ERROR resetting configuration: %s", ex.getMessage());
            throw ex;
        } catch (RuntimeException ex) {
            M3Log.error("*** ERROR resetting configuration: %s", ex.getMessage());
            throw ex;
        }
    }

    /**
	 * {@inheritDoc}
	 */
    public String getXMLConfigSnippet(Set<String> limit, String title, String comment) throws IOException {
        try {
            StringWriter sw = new StringWriter();
            if (title != null) {
                XMLBuilder xml = new XMLBuilder("title");
                xml.writeText(title);
                sw.write(xml.toString());
                sw.write("\r\n");
            }
            if (comment != null) {
                XMLBuilder xml = new XMLBuilder("comment");
                xml.writeText(comment);
                sw.write(xml.toString());
                sw.write("\r\n");
            }
            for (XMLConfigurator service : services) if (service != null) {
                sw.write(service.getXMLConfiguration(limit));
                sw.write("\r\n");
            }
            sw.write("</configuration>");
            sw.close();
            String baseXml = sw.toString().trim();
            String csum = calcChecksumNoWhiteSpace("<configuration>" + baseXml);
            StringBuilder ret = new StringBuilder("<configuration mac=\"");
            ret.append(csum);
            ret.append("\">\r\n");
            ret.append(baseXml);
            return wipeWhiteLines(ret.toString());
        } catch (IOException ioe) {
            M3Log.error("In getting general configuration", ioe);
            throw ioe;
        }
    }

    public String getXMLConfiguration(String title, String comment) throws IOException {
        return getXMLConfigSnippet(null, title, comment);
    }

    private static String wipeWhiteLines(String str) throws IOException {
        BufferedReader br = new BufferedReader(new StringReader(str));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            if (!line.trim().isEmpty()) {
                sb.append(line);
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    private static final byte[] SECRET = new byte[] { 0x17, 0x69, 0x33, 0x00, 0x04, 0x56, (byte) 0x8F, (byte) 0xFE, (byte) 0xAB, 0x1E, 0x1B, 0x11, 0x17, (byte) 0xAA, 0x19, (byte) 0xE9 };

    private static String calcChecksumNoWhiteSpace(String str) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(SECRET);
            char[] chars = str.toCharArray();
            StringBuilder trimmed = new StringBuilder();
            for (char c : chars) if (c != ' ' && c != '\r' && c != '\n' && c != '\t') trimmed.append(c);
            byte[] md5 = md.digest(trimmed.toString().getBytes());
            long ret = ((long) md5[0]) + (((long) md5[1]) << 8) + (((long) md5[2]) << 16) + (((long) md5[3]) << 24) + (((long) md5[4]) << 32) + (((long) md5[5]) << 40) + (((long) md5[6]) << 48) + (((long) md5[7]) << 56);
            return Long.toHexString(ret);
        } catch (NoSuchAlgorithmException e) {
            return "0";
        }
    }

    /**
	 * {@inheritDoc}
	 */
    public void shutdownSystem() {
        stopper.run();
    }

    /**
	 * {@inheritDoc}
	 */
    public String getDistroName() {
        return distroName;
    }

    /**
	 * {@inheritDoc}
	 */
    public String getDistroVersion() {
        return distroVersion;
    }
}
