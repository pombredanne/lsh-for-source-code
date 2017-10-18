package de.uni_bremen.informatik.p2p.peeranha42.core.network.wrapper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.zip.CRC32;
import net.jxta.document.MimeMediaType;
import net.jxta.endpoint.ByteArrayMessageElement;
import net.jxta.endpoint.Message;
import org.apache.log4j.Logger;
import sun.misc.BASE64Encoder;

/**
 * 
 * @author lippold
 *
 * SplitWrapper is used to transparently split Messages with more bytes
 * than partsize into smaller chunks. This is needed for transport.
 */
public class SplitWrapper implements MessageWrapper {

    final Hashtable messages = new Hashtable();

    static final Logger LOG = Logger.getLogger(SplitWrapper.class);

    static final int partsize = 14500;

    ArrayList wrapArr = new ArrayList();

    ArrayList unwrapArr = new ArrayList();

    /**
     * Splits a Message with total size less than 2,147,483,647 bytes [i.e.
     * (2^31)-1 bytes, approx 2 GB] in small messages with size less than
     * 'partsize' for sending. Currently, JXTA has errors if Messages have size
     * over 16kb, so we use only 14.5 kb in ~10 Ethernet-Frames with Message
     * overhead.
     * 
     * Btw.: You shouldn't send very large messages, it costs too much RAM.
     * 
     * @param msg Array of Messages to be splitted
     * @return a (slightly larger) array of splitted message
     */
    public Message[] wrap(Message[] msg) {
        for (int i = 0; i < msg.length; i++) {
            wrapAnalyze(msg[i]);
        }
        wrapArr.trimToSize();
        final Message[] ret = (Message[]) wrapArr.toArray(new Message[0]);
        wrapArr = new ArrayList();
        return ret;
    }

    synchronized void wrapAnalyze(Message msg) {
        if (msg.getByteLength() > partsize) {
            split(msg);
        } else wrapArr.add(msg);
    }

    void split(Message msg) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] message = null;
        try {
            final ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(msg);
            oos.close();
            message = baos.toByteArray();
            baos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        final long crc = crc(message);
        final String digest = digest(message);
        final int parts = message.length / partsize;
        int i;
        Message newMsg;
        ByteArrayMessageElement bams;
        for (i = 0; i < parts; i++) {
            byte[] part = new byte[partsize];
            for (int k = 0; k < partsize; k++) {
                part[k] = message[partsize * i + k];
            }
            final String properties = crc + " " + partsize + " " + i * partsize + " / " + message.length + " " + digest;
            bams = new ByteArrayMessageElement(properties, MimeMediaType.AOS, part, null);
            newMsg = new Message();
            newMsg.addMessageElement("SplitWrapper", bams);
            wrapArr.add(newMsg);
        }
        final int last = message.length - partsize * (message.length / partsize);
        byte[] lastpart = new byte[last];
        for (int k = 0; k < last; k++) {
            lastpart[k] = message[partsize * i + k];
        }
        final String properties = crc + " " + partsize + " " + i * partsize + " / " + message.length + " " + digest;
        bams = new ByteArrayMessageElement(properties, MimeMediaType.AOS, lastpart, null);
        newMsg = new Message();
        newMsg.addMessageElement("SplitWrapper", bams);
        wrapArr.add(newMsg);
    }

    /**
     * Takes multiple messages and when one final message arrived puts out the
     * resulting message
     * 
     * @param msg
     *            Array of Messages to be recomposed
     * @return an array of recomposed messages if successful, null otherwise
     */
    public Message[] unwrap(Message[] msg) {
        for (int i = 0; i < msg.length; i++) {
            unwrapAnalyze(msg[i]);
        }
        unwrapArr.trimToSize();
        final Message[] ret = (Message[]) unwrapArr.toArray(new Message[0]);
        unwrapArr = new ArrayList();
        return ret;
    }

    synchronized void unwrapAnalyze(Message msg) {
        boolean wrapped = false;
        final Message.ElementIterator mit = msg.getMessageElementsOfNamespace("SplitWrapper");
        while (mit.hasNext()) {
            wrapped = true;
            final Object o = mit.next();
            if (o instanceof ByteArrayMessageElement) {
                final ByteArrayMessageElement el = (ByteArrayMessageElement) o;
                final String name[] = el.getElementName().split(" ");
                if (name.length != 6) {
                    LOG.fatal("Message could not be unwrapped: Incorrect String in ByteArrayMessageElement:" + el.getElementName());
                    unwrapArr.add(msg);
                } else concat(el, name);
            }
        }
        if (!wrapped) unwrapArr.add(msg);
    }

    synchronized void concat(ByteArrayMessageElement el, String[] name) {
        final long crc = Long.parseLong(name[0]);
        final int partsize = Integer.parseInt(name[1]);
        final int offset = Integer.parseInt(name[2]);
        final int arraysize = Integer.parseInt(name[4]);
        final String digest = name[5];
        ByteArray message = null;
        message = (ByteArray) messages.get(digest + " " + crc);
        if (message == null) {
            message = new ByteArray(new byte[arraysize], partsize);
            messages.put(digest + " " + crc, message);
        }
        message.write(el.getBytes(false), offset);
        if (message.complete()) {
            final byte[] bmsg = message.getBytes();
            if (crc(bmsg) != crc) {
                LOG.fatal("CRC of composed message is wrong. Deleting message.");
                messages.remove(digest + " " + crc);
                return;
            }
            Message retmsg = null;
            final ByteArrayInputStream bin = new ByteArrayInputStream(bmsg);
            try {
                final ObjectInputStream oin = new ObjectInputStream(bin);
                retmsg = (Message) oin.readObject();
                unwrapArr.add(retmsg);
            } catch (Exception e) {
                LOG.fatal("Could not create Message from ByteArrayInputStream. Cause:" + e.toString());
            }
            messages.remove(digest + " " + crc);
        }
    }

    public long crc(byte[] msg) {
        final CRC32 crc = new CRC32();
        crc.update(msg);
        return crc.getValue();
    }

    public String digest(byte[] msg) {
        try {
            final MessageDigest dig = MessageDigest.getInstance("MD2");
            final BASE64Encoder encoder = new BASE64Encoder();
            return encoder.encode(dig.digest(msg));
        } catch (Exception e) {
            LOG.fatal("Digest MD2 not supported");
            return "digest not supported";
        }
    }

    class ByteArray {

        int processedParts = 0;

        int parts;

        byte[] bytes;

        protected ByteArray(byte[] b, int partsize) {
            bytes = b;
            parts = b.length / partsize + 1;
        }

        protected byte[] getBytes() {
            return bytes;
        }

        protected void write(byte[] b, int offset) {
            for (int i = 0; i < b.length; i++) {
                bytes[offset + i] = b[i];
            }
            processedParts++;
        }

        protected boolean complete() {
            return parts == processedParts;
        }

        protected int getTotalSize() {
            return bytes.length;
        }
    }
}
