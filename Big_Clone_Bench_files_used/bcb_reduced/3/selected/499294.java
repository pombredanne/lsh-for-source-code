package org.ceno.ui.handlers;

import org.ceno.communication.cli.CommunicationException;
import org.ceno.communication.cli.IEventCommunicator;
import org.ceno.model.Developer;
import org.ceno.model.DeveloperResourceState;
import org.ceno.model.MulticastMessage;
import org.ceno.model.Resource;
import org.ceno.model.impl.ModelFactoryImpl;
import org.ceno.protocol.event.CloseResourceEvent;
import org.ceno.protocol.event.IEvent;
import org.ceno.protocol.event.OpenResourceEvent;
import org.ceno.protocol.event.SendMessageEvent;
import org.ceno.tracker.cli.ICenoTrackerService;
import org.ceno.ui.Activator;
import org.ceno.ui.dialog.OpenOpenedResourceWarningDialog;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Bridges the gap between eclipse part events and ceno events. communicates
 * resource events to ceno server.
 * 
 * @author Andre Albert &lt;andre.albert82@googlemail.com&gt
 * @created 25.12.2009
 * @since 0.0.1
 */
public class WorkbenchPartListenerEventAdapter implements IPartListener {

    /**
	 * {@inheritDoc}
	 **/
    public void partActivated(final IWorkbenchPart part) {
    }

    /**
	 * {@inheritDoc}
	 **/
    public void partBroughtToTop(final IWorkbenchPart part) {
    }

    /**
	 * {@inheritDoc}
	 **/
    public void partClosed(final IWorkbenchPart part) {
        if (part instanceof IEditorPart) {
            Activator.log(Status.INFO, part + " closed");
            final Resource resource = buildResource(((IEditorPart) part).getEditorInput());
            final IEvent closeEvent = new CloseResourceEvent(resource, Activator.getDefault().getDeveloper());
            try {
                Activator.getDefault().getCliService().communicate(closeEvent);
            } catch (final CommunicationException e) {
                Activator.log(Status.ERROR, "Could not communicate partClosed to ceno", e);
            }
        }
    }

    /**
	 * {@inheritDoc}
	 **/
    public void partDeactivated(final IWorkbenchPart part) {
    }

    /**
	 * {@inheritDoc}
	 **/
    public void partOpened(final IWorkbenchPart part) {
        if (part instanceof IEditorPart) {
            Activator.log(Status.INFO, part + " opened");
            final Resource resource = buildResource(((IEditorPart) part).getEditorInput());
            final OpenResourceEvent openEvent = new OpenResourceEvent(resource, Activator.getDefault().getDeveloper());
            final String digest = Activator.getDefault().digest(((IEditorPart) part).getEditorInput());
            openEvent.setDigest(digest);
            try {
                final ICenoTrackerService trackerService = Activator.getDefault().getTrackerService();
                if (trackerService != null) {
                    final Collection<DeveloperResourceState> devResStates = trackerService.findDeveloperResourceStatesOfResource(resource.getFqName());
                    final Developer currentDeveloper = Activator.getDefault().getDeveloper();
                    final Iterator<DeveloperResourceState> drsIter = devResStates.iterator();
                    while (drsIter.hasNext()) {
                        if (drsIter.next().getDeveloper().getName().equals(currentDeveloper.getName())) {
                            drsIter.remove();
                        }
                    }
                    if (!devResStates.isEmpty()) {
                        final OpenOpenedResourceWarningDialog oorwd = new OpenOpenedResourceWarningDialog(part.getSite().getShell(), devResStates, resource);
                        if (oorwd.open() == Dialog.OK) {
                            final String messageText = oorwd.getMessageText();
                            if (oorwd.isSendToEditors()) {
                                sendMulticatMessage(resource, messageText);
                            }
                        }
                    }
                }
                final IEventCommunicator cliService = Activator.getDefault().getCliService();
                if (cliService != null) {
                    cliService.communicate(openEvent);
                }
            } catch (final CommunicationException e) {
                Activator.log(Status.ERROR, "Could not communicate partOpened to ceno", e);
            }
        }
    }

    private Resource buildResource(final IEditorInput input) {
        Resource resource = null;
        if (input instanceof IFileEditorInput) {
            final IFile resourceFile = ((IFileEditorInput) input).getFile();
            resource = Activator.buildResource(resourceFile);
        }
        return resource;
    }

    private void sendMulticatMessage(final Resource group, final String message) {
        final MulticastMessage mm = ModelFactoryImpl.eINSTANCE.createMulticastMessage();
        mm.setGroup(group);
        mm.setMessageText(message);
        mm.setSender(Activator.getDefault().getDeveloper());
        mm.setTimestamp(new Date());
        final IEvent event = new SendMessageEvent(mm);
        try {
            Activator.getDefault().getCliService().communicate(event);
        } catch (final CommunicationException e) {
            Activator.log(Status.ERROR, "Could not send multicast message");
        }
    }
}
