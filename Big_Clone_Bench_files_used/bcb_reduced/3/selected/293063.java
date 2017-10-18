package org.ceno.ui;

import org.ceno.communication.cli.CommunicationException;
import org.ceno.communication.cli.IConnectorService;
import org.ceno.communication.cli.IDigesterService;
import org.ceno.communication.cli.IEventCommunicator;
import org.ceno.communication.cli.IServerUnavailableDispatcher;
import org.ceno.communication.srv.ICommunicatorService;
import org.ceno.model.Developer;
import org.ceno.model.DeveloperResourceState;
import org.ceno.model.DeveloperResourceStates;
import org.ceno.model.Resource;
import org.ceno.model.impl.ModelFactoryImpl;
import org.ceno.protocol.event.SynchResourceStateEvent;
import org.ceno.tracker.cli.ICenoMessagesService;
import org.ceno.tracker.cli.ICenoTrackerService;
import org.ceno.tracker.cli.IDeveloperResourceStatesObserverService;
import org.ceno.tracker.cli.IMessagesObserverService;
import org.ceno.tracker.cli.IPeriodicSchedulerService;
import org.ceno.ui.handlers.ErrorDialogServerUnavailableObserver;
import org.ceno.ui.views.ClientsView;
import java.util.Date;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

    private static ILog log;

    private static Activator plugin;

    public static final String PLUGIN_ID = "org.ceno.ui";

    /**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
    public static Activator getDefault() {
        return Activator.plugin;
    }

    /**
	 * Returns an image descriptor for the image file at the given plug-in
	 * relative path
	 * 
	 * @param path
	 *            the path
	 * @return the image descriptor
	 */
    public static ImageDescriptor getImageDescriptor(final String path) {
        return imageDescriptorFromPlugin(PLUGIN_ID, path);
    }

    public static IWorkbenchWindow getWorkbenchWindow() {
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window == null) {
            final IWorkbenchWindow[] workbenchWindows = PlatformUI.getWorkbench().getWorkbenchWindows();
            if (workbenchWindows.length > 0) {
                window = workbenchWindows[0];
            }
        }
        return window;
    }

    public static void log(final int severity, final String message) {
        log.log(new Status(severity, PLUGIN_ID, message));
    }

    public static void log(final int severity, final String message, final Throwable throwable) {
        log.log(new Status(severity, PLUGIN_ID, message, throwable));
    }

    private IEventCommunicator cliService;

    private IConnectorService connService;

    private ClientsView currentClientsView;

    private Developer developer;

    private IDeveloperResourceStatesObserverService drsObserverService;

    private IMessagesObserverService msgObserverService;

    private ICenoMessagesService msgService;

    private IPartListener partListener;

    private IPeriodicSchedulerService schedulerService;

    private IServerUnavailableDispatcher serverUnavailableDispatcher;

    private ICommunicatorService srvService;

    private ICenoTrackerService trackerService;

    private IDigesterService digesterService;

    /**
	 * The constructor
	 */
    public Activator() {
    }

    private Resource buildResource(final IEditorInput input) {
        final Resource resource = ModelFactoryImpl.eINSTANCE.createResource();
        if (input instanceof IFileEditorInput) {
            resource.setFqName(((IFileEditorInput) input).getFile().getFullPath().toPortableString());
        } else {
            resource.setFqName(input.getName());
        }
        return resource;
    }

    /**
	 * @return the cliService
	 */
    public IEventCommunicator getCliService() {
        return cliService;
    }

    /**
	 * @return the connService
	 */
    public IConnectorService getConnService() {
        return connService;
    }

    /**
	 * @return the currentClientsView
	 */
    public ClientsView getCurrentClientsView() {
        return currentClientsView;
    }

    /**
	 * @return the developer
	 */
    public Developer getDeveloper() {
        return developer;
    }

    /**
	 * @return the drsObserverService
	 */
    public IDeveloperResourceStatesObserverService getDrsObserverService() {
        return drsObserverService;
    }

    /**
	 * @return the msgObserverService
	 */
    public IMessagesObserverService getMsgObserverService() {
        return msgObserverService;
    }

    /**
	 * @return the msgService
	 */
    public ICenoMessagesService getMsgService() {
        return msgService;
    }

    /**
	 * @return the partListener
	 */
    public IPartListener getPartListener() {
        return partListener;
    }

    /**
	 * @return the schedulerService
	 */
    public IPeriodicSchedulerService getSchedulerService() {
        return schedulerService;
    }

    /**
	 * @return the digesterService
	 */
    public IDigesterService getDigesterService() {
        return digesterService;
    }

    /**
	 * @return the srvService
	 */
    public ICommunicatorService getSrvService() {
        return srvService;
    }

    /**
	 * @return the trackerService
	 */
    public ICenoTrackerService getTrackerService() {
        return trackerService;
    }

    /**
	 * Called after login to set developer with id from backend
	 * 
	 * @param d
	 * @since 0.0.2
	 */
    public void initDeveloper(final Developer d) {
        this.developer.setId(d.getId());
        getMsgObserverService().setCurrentDeveloper(this.developer);
    }

    /**
	 * @param currentClientsView
	 *            the currentClientsView to set
	 */
    public void setCurrentClientsView(final ClientsView currentClientsView) {
        this.currentClientsView = currentClientsView;
    }

    /**
	 * @param developer
	 *            the developer to set
	 */
    public void setDeveloper(final Developer developer) {
        this.developer = developer;
    }

    /**
	 * @param partListener
	 *            the partListener to set
	 */
    public void setPartListener(final IPartListener partListener) {
        this.partListener = partListener;
    }

    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);
        Activator.plugin = this;
        Activator.log = Platform.getLog(context.getBundle());
        final ServiceReference srvServiceReference = context.getServiceReference(ICommunicatorService.class.getCanonicalName());
        if (srvServiceReference != null) {
            srvService = (ICommunicatorService) context.getService(srvServiceReference);
            log(Status.INFO, "Found Srv Service");
        }
        final ServiceReference cliServiceReference = context.getServiceReference(IEventCommunicator.class.getCanonicalName());
        if (cliServiceReference != null) {
            cliService = (IEventCommunicator) context.getService(cliServiceReference);
            log(Status.INFO, "Found EventCommunicator Service");
        }
        final ServiceReference connServiceReference = context.getServiceReference(IConnectorService.class.getCanonicalName());
        if (connServiceReference != null) {
            connService = (IConnectorService) context.getService(connServiceReference);
            log(Status.INFO, "Found ceno Connection Service");
        }
        final ServiceReference msgObserverServiceReference = context.getServiceReference(IMessagesObserverService.class.getCanonicalName());
        if (connServiceReference != null) {
            msgObserverService = (IMessagesObserverService) context.getService(msgObserverServiceReference);
            log(Status.INFO, "Found ceno Message Observer Service");
        }
        final ServiceReference msgServiceReference = context.getServiceReference(ICenoMessagesService.class.getCanonicalName());
        if (connServiceReference != null) {
            msgService = (ICenoMessagesService) context.getService(msgServiceReference);
            log(Status.INFO, "Found ceno Message Service");
        }
        final ServiceReference drsObserverServiceReference = context.getServiceReference(IDeveloperResourceStatesObserverService.class.getCanonicalName());
        if (connServiceReference != null) {
            drsObserverService = (IDeveloperResourceStatesObserverService) context.getService(drsObserverServiceReference);
            log(Status.INFO, "Found ceno Developer Resource Observer Service");
        }
        final ServiceReference schedulerServiceReference = context.getServiceReference(IPeriodicSchedulerService.class.getCanonicalName());
        if (schedulerServiceReference != null) {
            schedulerService = (IPeriodicSchedulerService) context.getService(schedulerServiceReference);
            log(Status.INFO, "Found ceno Scheduler Service");
        }
        final ServiceReference trackerServiceReference = context.getServiceReference(ICenoTrackerService.class.getCanonicalName());
        if (trackerServiceReference != null) {
            trackerService = (ICenoTrackerService) context.getService(trackerServiceReference);
            log(Status.INFO, "Found ceno Tracker Service");
        }
        final ServiceReference srvUnavailableServiceReference = context.getServiceReference(IServerUnavailableDispatcher.class.getCanonicalName());
        if (srvUnavailableServiceReference != null) {
            serverUnavailableDispatcher = (IServerUnavailableDispatcher) context.getService(srvUnavailableServiceReference);
            log(Status.INFO, "Found ceno srv unavailble dispatcher Service");
        }
        final ServiceReference digesterServiceReference = context.getServiceReference(IDigesterService.class.getCanonicalName());
        if (digesterServiceReference != null) {
            digesterService = (IDigesterService) context.getService(digesterServiceReference);
            log(Status.INFO, "Found ceno digester Service");
        }
        final ErrorDialogServerUnavailableObserver edsuo = new ErrorDialogServerUnavailableObserver();
        serverUnavailableDispatcher.registerServerUnavailableListener(edsuo);
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        Activator.plugin = null;
        super.stop(context);
    }

    public void synchOpenedResources() {
        final IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window != null) {
            final IWorkbenchPage page = window.getActivePage();
            final IEditorReference[] editorReferences = page.getEditorReferences();
            final DeveloperResourceStates developerResourceStates = ModelFactoryImpl.eINSTANCE.createDeveloperResourceStates();
            developerResourceStates.setDeveloper(Activator.getDefault().getDeveloper());
            Activator.getDefault().getDigesterService();
            for (final IEditorReference editor : editorReferences) {
                try {
                    final Resource openResource = buildResource(editor.getEditorInput());
                    final DeveloperResourceState resourceState = ModelFactoryImpl.eINSTANCE.createDeveloperResourceState();
                    resourceState.setResource(openResource);
                    resourceState.setDeveloper(developer);
                    resourceState.setTimestamp(new Date());
                    resourceState.setDigest(digest(editor.getEditorInput()));
                    developerResourceStates.getStates().add(resourceState);
                } catch (final PartInitException e) {
                    Activator.log(Status.ERROR, e.getMessage());
                }
            }
            final SynchResourceStateEvent srse = new SynchResourceStateEvent(developerResourceStates);
            try {
                cliService.communicate(srse);
            } catch (final CommunicationException e) {
                Activator.log(Status.ERROR, e.getMessage());
            }
        }
    }

    public String digest(final IEditorInput input) {
        String result = null;
        if (input instanceof IFileEditorInput) {
            if (digesterService != null) {
                try {
                    result = digesterService.digest(((IFileEditorInput) input).getFile().getContents());
                } catch (final Exception e) {
                    Activator.log(Status.ERROR, "Could not generate digest for resource " + input);
                }
            }
        }
        return result;
    }

    public static Resource buildResource(final IFile resourceFile) {
        final Resource resource = ModelFactoryImpl.eINSTANCE.createResource();
        resource.setFqName(resourceFile.getFullPath().toPortableString());
        return resource;
    }
}
