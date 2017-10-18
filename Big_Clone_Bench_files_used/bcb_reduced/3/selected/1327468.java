package connex.core.net;

import net.jxta.pipe.PipeService;
import net.jxta.protocol.PipeAdvertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.id.IDFactory;
import java.io.FileInputStream;
import net.jxta.document.MimeMediaType;
import java.security.MessageDigest;
import connex.core.WS.*;
import net.jxta.document.*;
import java.io.*;
import net.jxta.pipe.*;
import net.jxta.peergroup.*;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;
import java.net.URISyntaxException;
import java.net.URI;
import net.jxta.id.ID;

/**
 * <p>Title: </p>
 *
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright (c) 2005</p>
 *
 * <p>Company: </p>
 *
 * @author Hisham Khalil <a HREF="mailto:hishberlin@hotmail.com">hishberlin@hotmail.com</a>
 * @version 1.0
 */
public class PipeUtils {

    /**
     *
     */
    static final Logger LOG = Logger.getLogger(PipeUtils.class);

    /**
     *
     * @param name String
     * @param disc String
     * @return PipeAdvertisement
     */
    public static PipeAdvertisement createPipeAdv(String name, String disc) {
        PipeAdvertisement pipeAdv;
        PipeID id = IDFactory.newPipeID(WorkspaceManager.getInstance().getCurrentWorkspace().getPeerGroup().getPeerGroupID());
        pipeAdv = (PipeAdvertisement) AdvertisementFactory.newAdvertisement(PipeAdvertisement.getAdvertisementType());
        pipeAdv.setPipeID(id);
        pipeAdv.setDescription(disc);
        pipeAdv.setName(name);
        pipeAdv.setType(PipeService.UnicastType);
        return pipeAdv;
    }

    /**
     * Generates an MD5 digest hash of the string: clearText1 + clearText2 or: clearTextID if clearText2 was blank.<p>
     * @param clearText1 String A string that is to be hashed. This can be any string used for hashing or hiding data.
     * @param clearText2 String
     * @return byte[]
     * @throws Exception
     */
    public static final byte[] generateHash(String clearText1, String clearText2) throws Exception {
        String id;
        if (clearText2 == null) {
            id = clearText1;
        } else {
            id = clearText1 + clearText2;
        }
        byte[] buffer = id.getBytes();
        MessageDigest algorithm = null;
        algorithm = MessageDigest.getInstance("MD5");
        algorithm.reset();
        algorithm.update(buffer);
        byte[] digest1 = algorithm.digest();
        return digest1;
    }

    /**
     * Create a PipeID based on the digest of the clearText1 and clearText1.
     * @param peerGroupID Parent peer group ID.
     * @param clearText1 String
     * @param clearText2 String
     * @return PipeID
     */
    public static final PipeID createPipeID(PeerGroupID peerGroupID, String clearText1, String clearText2) {
        byte[] digest = null;
        try {
            digest = generateHash(clearText1, clearText2);
        } catch (Exception ex) {
        }
        return IDFactory.newPipeID(peerGroupID, digest);
    }

    /**
     * Reads a local stored  PipeAdvertisement from file and return a  PipeAdvertisement Object
     * @param file File
     * @return PipeAdvertisement
     */
    public static PipeAdvertisement readPipeAdvertisement(File file) {
        PipeAdvertisement PipeAdv = null;
        try {
            FileInputStream is = new FileInputStream(file);
            StructuredDocument doc = StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, is);
            XMLElement e = (XMLElement) doc.getParent();
            PipeAdv = (PipeAdvertisement) AdvertisementFactory.newAdvertisement(e);
            is.close();
        } catch (Throwable e) {
            System.out.println("failed : " + e);
        }
        return PipeAdv;
    }

    /**
     *
     * @param name String
     * @param listener PipeMsgListener
     * @return PipePair
     */
    public static PipePair createWorkspacePipe(PeerGroup pg, String name, PipeMsgListener listener) {
        LOG.setLevel(Level.ERROR);
        InputPipe inputPipe = null;
        OutputPipe outputPipe = null;
        PipeAdvertisement pipeAdvt = (PipeAdvertisement) AdvertisementFactory.newAdvertisement(PipeAdvertisement.getAdvertisementType());
        PipeID pid = createPipeID(pg.getPeerGroupID(), name, null);
        pipeAdvt.setPipeID(pid);
        pipeAdvt.setName(name);
        pipeAdvt.setType("JxtaPropagate");
        try {
            pg.getDiscoveryService().publish(pipeAdvt);
            ;
            pg.getDiscoveryService().remotePublish(pipeAdvt);
            ;
            outputPipe = pg.getPipeService().createOutputPipe(pipeAdvt, 2000L);
            inputPipe = pg.getPipeService().createInputPipe(pipeAdvt, listener);
        } catch (Exception e) {
            if (LOG.isEnabledFor(Level.ERROR)) {
                LOG.error(e.getMessage());
            }
        }
        return new PipePair(inputPipe, outputPipe);
    }

    /**
     * Creats new inputPipe.
     * @param listener PipeMsgListener
     * @return InputPipe
     */
    public static InputPipe createInputPipe(PipeMsgListener listener) {
        LOG.setLevel(Level.ERROR);
        InputPipe privInputPipe = null;
        PipeAdvertisement pipeAdvt = (PipeAdvertisement) AdvertisementFactory.newAdvertisement(PipeAdvertisement.getAdvertisementType());
        PipeID id = IDFactory.newPipeID(WorkspaceManager.getInstance().getCurrentWorkspace().getPeerGroup().getPeerGroupID());
        pipeAdvt.setPipeID(id);
        pipeAdvt.setName(WorkspaceManager.getInstance().getCurrentWorkspace().getPeerGroup().getPeerName() + " : " + WorkspaceManager.getInstance().getCurrentWorkspace().getPeerGroup().getPeerID().toString());
        pipeAdvt.setType("JxtaUnicast");
        try {
            WorkspaceManager.getInstance().getCurrentWorkspace().getPeerGroup().getDiscoveryService().publish(pipeAdvt);
            ;
            WorkspaceManager.getInstance().getCurrentWorkspace().getPeerGroup().getDiscoveryService().remotePublish(pipeAdvt);
            ;
            privInputPipe = WorkspaceManager.getInstance().getCurrentWorkspace().getPeerGroup().getPipeService().createInputPipe(pipeAdvt, listener);
        } catch (Exception e) {
            if (LOG.isEnabledFor(Level.ERROR)) {
                LOG.error(e.getMessage());
            }
        }
        return privInputPipe;
    }

    public static OutputPipe createOutputPipe(String backId) {
        LOG.setLevel(Level.ERROR);
        OutputPipe outputPipe = null;
        PipeAdvertisement pipeAdvt = (PipeAdvertisement) AdvertisementFactory.newAdvertisement(PipeAdvertisement.getAdvertisementType());
        ID id = ID.nullID;
        try {
            id = IDFactory.fromURI(new URI(backId));
        } catch (URISyntaxException use) {
            use.printStackTrace();
        }
        pipeAdvt.setPipeID(id);
        pipeAdvt.setName("ConneXPipe");
        pipeAdvt.setType("JxtaUnicast");
        try {
            outputPipe = WorkspaceManager.getInstance().getCurrentWorkspace().getPeerGroup().getPipeService().createOutputPipe(pipeAdvt, 20000);
        } catch (IOException ex) {
            if (LOG.isEnabledFor(Level.ERROR)) {
                LOG.error(ex.getMessage());
            }
        }
        return outputPipe;
    }
}
