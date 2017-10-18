package alfresco.module.sword.service;

import alfresco.module.sword.AmsetCollection;
import alfresco.module.sword.IWorkspace;
import alfresco.module.sword.Workspace;
import alfresco.module.sword.mvc.IServiceDocumentContext;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.purl.sword.base.SWORDException;

/**
 * Checks the deposit request parameters against the <code>AmsetCollection</code>
 * details.  The 'hardwire' I think refered to the establishment of
 * collections using property files.  Poor name.
 *
 * @author clayton
 */
public class HardwireDepositValidator implements IDepositValidator {

    public boolean validateDestination(IServiceDocumentContext ctx, String collectionLocation) {
        List<Workspace> workspaceList = ctx.getServiceDocument().getService().getWorkspaceList();
        for (Workspace ws : workspaceList) {
            List<AmsetCollection> collectionList = ws.getCollections();
            for (AmsetCollection ac : collectionList) {
                if (collectionLocation.equals(ac.getLocation())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     *
     * @param fileContents
     * @return
     * @throws SWORDException
     */
    public boolean validateFileContents(byte[] fileContents) {
        if (fileContents == null || !(fileContents.length > 0)) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * 
     * @param fileName
     * @return
     */
    public boolean validateFileName(String fileName) {
        if (fileName == null || fileName.equals("")) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * String <code>accepts</code> is a comma delimited list of mime types eg
     * [image/gif,image/png,image/jpg]
     * @param accepts
     * @param mimeType
     * @return
     */
    public boolean validateMimeType(String accepts, String mimeType) {
        if (accepts != null && mimeType != null && accepts.indexOf(mimeType) != -1) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 
     * @param acceptPackagingTypes
     * @param packagingType
     * @return
     */
    public boolean validateContentPackagingType(String acceptPackagingTypes, String packagingType) {
        if (acceptPackagingTypes != null && packagingType != null && acceptPackagingTypes.indexOf(packagingType) != -1) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 
     * @param ctx
     * @param verbose
     * @return
     */
    public boolean validateVerbose(IServiceDocumentContext ctx, boolean verbose) {
        if (verbose && !ctx.getServiceDocument().getService().getServiceDetails().isVerbose()) {
            return false;
        } else {
            return true;
        }
    }

    public boolean validateOnBehalfOf(IServiceDocumentContext ctx) {
        if (!ctx.getServiceDocument().getService().getServiceDetails().isOnBehalfOfPermitted()) {
            return false;
        } else {
            return true;
        }
    }

    public boolean validateMd5(IServiceDocumentContext ctx) {
        if (!ctx.getServiceDocument().getService().getServiceDetails().isMD5Permitted()) {
            return false;
        } else {
            return true;
        }
    }

    public boolean checkMd5(byte[] fileContents, String hash) {
        String amsetHashString = null;
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(fileContents);
            BigInteger amsetHash = new BigInteger(1, md5.digest());
            amsetHashString = amsetHash.toString(16);
        } catch (NoSuchAlgorithmException nsae) {
            nsae.printStackTrace();
            return false;
        }
        if (!amsetHashString.equals(hash)) {
            return false;
        } else {
            return true;
        }
    }

    public boolean validateContentPackaging(IServiceDocumentContext ctx) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean validateFileSize(IServiceDocumentContext ctx, byte[] fileContents) {
        if (fileContents.length > ctx.getServiceDocument().getService().getServiceDetails().getMaxUploadSize() * 1000) {
            return false;
        } else {
            return true;
        }
    }
}
