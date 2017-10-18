package org.p2pws.jxta;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import net.jxta.document.Advertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.Document;
import net.jxta.document.MimeMediaType;
import net.jxta.id.IDFactory;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.pipe.PipeID;
import net.jxta.pipe.PipeService;
import net.jxta.protocol.PipeAdvertisement;
import org.p2pws.ServiceDescriptor;
import org.p2pws.URIFactory;

/**
 * @author panisson
 *
 */
public class JxtaUtil implements URIFactory {

    public static final PeerGroupID InfrastructurePeerGroupID;

    static {
        PeerGroupID infrastructurePeerGroupID = null;
        try {
            infrastructurePeerGroupID = (PeerGroupID) IDFactory.fromURI(new URI("urn:jxta:uuid-9AB310FDB28043008B69B1865E15F35602"));
        } catch (URISyntaxException e) {
        }
        InfrastructurePeerGroupID = infrastructurePeerGroupID;
    }

    /**
     * 
     * @param epr
     * @return
     */
    public static PipeAdvertisement createPipeAdvertisement(ServiceDescriptor descriptor) {
        PipeID socketID = getPipeIDForService(descriptor);
        PipeAdvertisement advertisement = (PipeAdvertisement) AdvertisementFactory.newAdvertisement(PipeAdvertisement.getAdvertisementType());
        advertisement.setPipeID(socketID);
        advertisement.setType(PipeService.UnicastType);
        advertisement.setName("Socket for endpoint " + descriptor.getEpr());
        return advertisement;
    }

    /**
     * FIXME: This version creates the pipe id using the service name. This is not valid
     * in some circumstances
     */
    public static PipeID getPipeIDForService(ServiceDescriptor descriptor) {
        PipeID id = null;
        URI uri = descriptor.getUri();
        if (uri != null) {
            try {
                id = (PipeID) IDFactory.fromURI(uri);
            } catch (URISyntaxException e) {
                throw new RuntimeException("Error creating id for pipe " + uri, e);
            }
        }
        if (id == null) {
            MessageDigest md = null;
            try {
                md = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
            }
            String idToHash = descriptor.getName();
            if (descriptor.getHost() != null) {
                idToHash += descriptor.getHost();
            }
            md.update(idToHash.getBytes());
            id = IDFactory.newPipeID(InfrastructurePeerGroupID, md.digest());
        }
        return id;
    }

    public URI getUriForService(ServiceDescriptor descriptor) {
        return getPipeIDForService(descriptor).toURI();
    }

    /**
     * Convert this Advertisement to a String
     */
    public static String toString(Advertisement advert) throws IOException {
        Document doc = advert.getDocument(new MimeMediaType("text/xml"));
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        doc.sendToStream(bos);
        return bos.toString();
    }

    /**
     * Convert this Advertisement to a String
     */
    public static byte[] toByteArray(Advertisement advert) throws IOException {
        Document doc = advert.getDocument(new MimeMediaType("text/xml"));
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        doc.sendToStream(bos);
        return bos.toByteArray();
    }
}
