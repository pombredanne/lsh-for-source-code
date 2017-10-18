package org.ceno.ui.handlers;

import org.ceno.communication.cli.CommunicationException;
import org.ceno.model.Resource;
import org.ceno.protocol.event.ModifyResourceEvent;
import org.ceno.ui.Activator;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;

/**
 * Required to send {@see ModifyResourceEvent} to ceno server.
 * 
 * @author Andre Albert &lt;andre.albert82@googlemail.com&gt
 * @created 13.07.2010
 * @since 0.0.6
 */
public class ResourceChangeListener implements IResourceChangeListener {

    /**
	 * {@inheritDoc}
	 **/
    public void resourceChanged(final IResourceChangeEvent changeEvent) {
        try {
            changeEvent.getDelta().accept(new DeltaFileFinder());
        } catch (final CoreException e) {
            Activator.log(Status.ERROR, "Could not fetch modified Resource", e);
        }
    }
}

class DeltaFileFinder implements IResourceDeltaVisitor {

    public boolean visit(final IResourceDelta delta) {
        final IResource res = delta.getResource();
        if (res instanceof IFile) {
            final IFile resourceFile = (IFile) res;
            if (!resourceFile.getFileExtension().equals("class")) {
                final Resource modifiedResource = Activator.buildResource(resourceFile);
                final ModifyResourceEvent event = new ModifyResourceEvent(modifiedResource, Activator.getDefault().getDeveloper());
                if (Activator.getDefault().getDigesterService() != null) {
                    try {
                        final String digest = Activator.getDefault().getDigesterService().digest(resourceFile.getContents());
                        event.setDigest(digest);
                    } catch (final Exception e) {
                        Activator.log(Status.ERROR, "Could not generate digest for resource " + modifiedResource);
                    }
                }
                try {
                    Activator.getDefault().getCliService().communicate(event);
                } catch (final CommunicationException e) {
                    Activator.log(Status.ERROR, "Could not communicate modifiedResource to ceno", e);
                }
            }
            return false;
        }
        return true;
    }
}
