package org.jcryptool.crypto.flexiprovider.engines.messagedigest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.log4j.Logger;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.ui.PartInitException;
import org.jcryptool.core.operations.editors.EditorsManager;
import org.jcryptool.core.operations.util.PathEditorInput;
import org.jcryptool.crypto.flexiprovider.descriptors.IFlexiProviderOperation;
import org.jcryptool.crypto.flexiprovider.engines.FlexiProviderEngine;
import org.jcryptool.crypto.flexiprovider.engines.FlexiProviderEnginesPlugin;
import de.flexiprovider.api.MessageDigest;
import de.flexiprovider.api.Registry;
import de.flexiprovider.api.exceptions.NoSuchAlgorithmException;

public class MessageDigestEngine extends FlexiProviderEngine {

    /** The log4j logger */
    private static final Logger logger = FlexiProviderEnginesPlugin.getLogManager().getLogger(MessageDigestEngine.class.getName());

    private MessageDigest digest;

    public MessageDigestEngine() {
    }

    public void init(IFlexiProviderOperation operation) {
        logger.debug("initializing message digest engine");
        this.operation = operation;
        try {
            digest = Registry.getMessageDigest(operation.getAlgorithmDescriptor().getAlgorithmName());
            initialized = true;
        } catch (NoSuchAlgorithmException e) {
            logger.error("NoSuchAlgorithmException while initializing a message digest", e);
        }
    }

    public void perform() {
        if (initialized) {
            InputStream inputStream = initInput(operation.getInput());
            OutputStream outputStream = initOutput(operation.getOutput());
            try {
                int i;
                while ((i = inputStream.read()) != -1) {
                    digest.update((byte) i);
                }
                outputStream.write(digest.digest());
                inputStream.close();
                outputStream.close();
                if (operation.getOutput().equals("<Editor>")) {
                    EditorsManager.getInstance().openNewHexEditor(new PathEditorInput(URIUtil.toPath(getOutputURI())));
                }
            } catch (IOException e) {
                logger.error("IOException while performing a message digest", e);
            } catch (PartInitException e) {
                logger.error("PartInitException while performing a message digest", e);
            }
        }
    }
}
