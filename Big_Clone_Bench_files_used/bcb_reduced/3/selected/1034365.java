package org.ourgrid.acceptance.util;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.easymock.classextension.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.ourgrid.common.config.Configuration;
import org.ourgrid.common.executor.ExecutorHandle;
import org.ourgrid.common.executor.IntegerExecutorHandle;
import org.ourgrid.common.interfaces.Worker;
import org.ourgrid.common.interfaces.WorkerExecutionServiceClient;
import org.ourgrid.common.interfaces.control.WorkerControl;
import org.ourgrid.common.interfaces.management.RemoteWorkerManagement;
import org.ourgrid.common.interfaces.management.WorkerManagement;
import org.ourgrid.common.interfaces.status.WorkerStatusProvider;
import org.ourgrid.common.interfaces.to.WorkAccounting;
import org.ourgrid.common.spec.worker.WorkerSpec;
import org.ourgrid.common.util.JavaFileUtil;
import org.ourgrid.common.util.OS;
import org.ourgrid.worker.WorkerComponent;
import org.ourgrid.worker.WorkerConfiguration;
import org.ourgrid.worker.WorkerConstants;
import org.ourgrid.worker.controller.WorkerManagementClientFailureController;
import org.ourgrid.worker.controller.actions.BeginAllocationAction;
import org.ourgrid.worker.dao.FileTransferDAO;
import org.ourgrid.worker.sysmonitor.interfaces.WorkerSysInfoCollector;
import br.edu.ufcg.lsd.commune.Application;
import br.edu.ufcg.lsd.commune.container.ContainerContext;
import br.edu.ufcg.lsd.commune.container.ObjectDeployment;
import br.edu.ufcg.lsd.commune.container.control.ApplicationManager;
import br.edu.ufcg.lsd.commune.container.servicemanager.ServiceManager;
import br.edu.ufcg.lsd.commune.container.servicemanager.actions.RepetitionRunnable;
import br.edu.ufcg.lsd.commune.identification.ServiceID;
import br.edu.ufcg.lsd.commune.network.signature.SignatureProperties;
import br.edu.ufcg.lsd.commune.processor.filetransfer.IncomingTransferHandle;
import br.edu.ufcg.lsd.commune.processor.filetransfer.OutgoingTransferHandle;

public class WorkerAcceptanceUtil extends AcceptanceUtil {

    public WorkerAcceptanceUtil(ContainerContext context) {
        super(context);
    }

    public static final String SEP = File.separator;

    public static final String WORKER_TEST_DIR = "test" + SEP + "acceptance" + SEP + "worker" + SEP;

    protected static final String PROPERTIES_FILENAME = WORKER_TEST_DIR + "worker.properties";

    public static final String DEF_PLAYPEN_ROOT_PATH = "test" + File.separator + "tmp" + File.separator + "playpen";

    public static final String DEF_INVALID_PLAYPEN_ROOT_PATH = "test" + File.separator + "tmp" + File.separator + "invalid_playpen";

    public static final String DEF_STORAGE_ROOT_PATH = "test" + File.separator + "tmp" + File.separator + "storage";

    public static final String DEF_INVALID_STORAGE_ROOT_PATH = "test" + File.separator + "tmp" + File.separator + "invalid_storage";

    @Before
    public void setUp() throws Exception {
        System.setProperty("OGROOT", ".");
        Configuration.getInstance(WorkerConfiguration.WORKER);
        deleteEnvDirs();
    }

    @After
    public void tearDown() throws Exception {
        deleteEnvDirs();
        if (application != null && !application.getContainerDAO().isStopped()) {
            application.stop();
        }
    }

    public String simulateAuthentication() {
        Configuration conf = WorkerConfiguration.getInstance(WorkerConfiguration.WORKER);
        String peerPublicKey = "peerPublicKey";
        conf.setProperty(WorkerConfiguration.PROP_PEER_PUBLIC_KEY, peerPublicKey);
        return peerPublicKey;
    }

    public void deleteEnvDirs() throws IOException {
        removeDirectory(context.getProperties().get(WorkerConfiguration.PROP_PLAYPEN_ROOT));
        removeDirectory(context.getProperties().get(WorkerConfiguration.PROP_STORAGE_DIR));
    }

    public RepetitionRunnable createExecutorRunnable(Application workerComponent, int executionId) {
        ExecutorHandle handle = new IntegerExecutorHandle(executionId);
        ObjectDeployment objectDeployment = getWorkerControlDeployment();
        return new RepetitionRunnable(workerComponent.getContainer(), (ApplicationManager) objectDeployment.getObject(), WorkerConstants.EXECUTOR_ACTION_NAME, new WorkerExecutionHandle(handle, null));
    }

    public BeginAllocationAction createBeginAllocationRunnable() {
        return new BeginAllocationAction(null, null);
    }

    public RepetitionRunnable createReportWorkAccountingRunnable(Application workerComponent) {
        ObjectDeployment objectDeployment = getWorkerControlDeployment();
        return new RepetitionRunnable(workerComponent.getContainer(), (ApplicationManager) objectDeployment.getObject(), WorkerConstants.REPORT_WORK_ACCOUNTING_ACTION_NAME, null);
    }

    public WorkerSpec createWorkerSpecMock(ServiceID entityIDA) {
        WorkerSpec workerSpecA = EasyMock.createNiceMock(WorkerSpec.class);
        org.easymock.classextension.EasyMock.expect(workerSpecA.getServiceID()).andReturn(entityIDA).anyTimes();
        EasyMock.replay(workerSpecA);
        return workerSpecA;
    }

    public WorkerSpec createWorkerSpec(String userName, String serverName) {
        Map<String, String> spec = new HashMap<String, String>();
        spec.put(WorkerSpec.ATT_USERNAME, userName);
        spec.put(WorkerSpec.ATT_SERVERNAME, serverName);
        return new WorkerSpec(spec);
    }

    public ServiceManager getServiceManager() {
        ObjectDeployment deployment = getWorkerControlDeployment();
        return deployment.getServiceManager();
    }

    public WorkerManagement getWorkerManagement() {
        ObjectDeployment deployment = getWorkerManagementDeployment();
        return (WorkerManagement) deployment.getObject();
    }

    public ObjectDeployment getWorkerManagementDeployment() {
        return getTestProxy(application, WorkerConstants.LOCAL_WORKER_MANAGEMENT);
    }

    public void runRepeatedAction(WorkerComponent component, Serializable handler, String actionName) {
        component.getScheduledAction(actionName).run(handler, getServiceManager());
    }

    public WorkerControl getWorkerControl() {
        ObjectDeployment deployment = getWorkerControlDeployment();
        return (WorkerControl) deployment.getObject();
    }

    public ObjectDeployment getWorkerControlDeployment() {
        return application.getObject(Application.CONTROL_OBJECT_NAME);
    }

    public WorkerSysInfoCollector getWorkerSysInfoCollector() {
        ObjectDeployment deployment = getWorkerSysInfoCollectorObjectDeplyment();
        return (WorkerSysInfoCollector) deployment.getObject();
    }

    public ObjectDeployment getWorkerSysInfoCollectorObjectDeplyment() {
        return application.getObject(WorkerConstants.WORKER_SYSINFO_COLLECTOR);
    }

    public WorkerExecutionServiceClient getWorkerExecutionClient() {
        ObjectDeployment deployment = getContainerObject(application, WorkerConstants.WORKER_EXECUTION_CLIENT);
        return (WorkerExecutionServiceClient) deployment.getObject();
    }

    public WorkerManagementClientFailureController getMasterPeerMonitor(WorkerComponent component) {
        ObjectDeployment deployment = getMasterPeerMonitorDeployment(component);
        return (WorkerManagementClientFailureController) deployment.getObject();
    }

    public ObjectDeployment getMasterPeerMonitorDeployment(WorkerComponent component) {
        return component.getObject(WorkerConstants.LOCAL_WORKER_MANAGEMENT_CLIENT_MONITOR);
    }

    public WorkAccounting getWorkerAccountingReporter() {
        return (WorkAccounting) getTestProxy(application, WorkerConstants.WORKER_ACCOUNTING_REPORTER).getObject();
    }

    public WorkerStatusProvider getWorkerStatusProvider() {
        return (WorkerStatusProvider) application.getObject(Application.CONTROL_OBJECT_NAME).getObject();
    }

    public Worker getWorker() {
        ObjectDeployment deployment = getWorkerDeployment();
        Worker worker = null;
        if (deployment != null) {
            worker = (Worker) deployment.getObject();
        }
        return worker;
    }

    public ObjectDeployment getWorkerDeployment() {
        return application.getObject(WorkerConstants.WORKER);
    }

    public RemoteWorkerManagement getRemoteWorkerManagement() {
        ObjectDeployment deployment = getRemoteWorkerManagementDeployment();
        RemoteWorkerManagement rmw = null;
        if (deployment != null) {
            rmw = (RemoteWorkerManagement) deployment.getObject();
        }
        return rmw;
    }

    public ObjectDeployment getRemoteWorkerManagementDeployment() {
        return application.getObject(WorkerConstants.REMOTE_WORKER_MANAGEMENT);
    }

    public boolean isWorkerBound() {
        return (getWorker() != null);
    }

    public boolean isRemoteWorkerManagementBound() {
        return (getRemoteWorkerManagement() != null);
    }

    public Map<String, Object> createWorkerProperties() {
        return createWorkerProperties(false);
    }

    public Map<String, Object> createWorkerProperties(boolean withIdlenessDetector) {
        return createWorkerProperties(DEF_PLAYPEN_ROOT_PATH, DEF_STORAGE_ROOT_PATH, withIdlenessDetector);
    }

    public Map<String, Object> createWorkerProperties(String playpenRootPath, String storageRootPath, boolean withIdlenessDetector) {
        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put(SignatureProperties.PROP_PUBLIC_KEY, context.getProperty(SignatureProperties.PROP_PUBLIC_KEY));
        properties.put(WorkerConfiguration.PROP_PEER_PUBLIC_KEY, context.getProperty(WorkerConfiguration.PROP_PEER_PUBLIC_KEY));
        if (withIdlenessDetector) {
            properties.put(WorkerConfiguration.PROP_IDLENESS_DETECTOR, "yes");
        } else {
            properties.put(WorkerConfiguration.PROP_IDLENESS_DETECTOR, WorkerConfiguration.DEF_PROP_IDLENESS_DETECTOR);
        }
        properties.put(WorkerConfiguration.PROP_PLAYPEN_ROOT, playpenRootPath.replace("\\\\", "\\"));
        properties.put(WorkerConfiguration.PROP_STORAGE_DIR, storageRootPath);
        return properties;
    }

    public static File createDirectory(String path, boolean readOnly) {
        File playpenDir = new File(path);
        if (!playpenDir.exists()) {
            playpenDir.mkdirs();
        }
        if (readOnly) {
            playpenDir.setReadable(true, false);
            playpenDir.setWritable(false, false);
        } else {
            JavaFileUtil.setReadAndWrite(playpenDir);
        }
        return playpenDir;
    }

    public static void removeDirectory(String path) throws IOException {
        File playPenDir = new File(path);
        JavaFileUtil.setReadAndWrite(playPenDir);
        if (playPenDir.exists() && playPenDir.isDirectory()) {
            deleteFilesInDir(playPenDir);
            playPenDir.delete();
        }
    }

    private static void deleteFilesInDir(File directory) throws IOException {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                JavaFileUtil.setReadAndWrite(file);
                if (file.isDirectory()) {
                    deleteFilesInDir(file);
                }
                file.delete();
            }
        }
    }

    public static boolean directoryExists(String path) {
        File file = new File(path);
        return file.exists();
    }

    public static boolean directoryContainsFiles(String path) {
        File file = new File(path);
        if (file.isDirectory()) {
            return file.listFiles().length != 0;
        }
        return false;
    }

    public static boolean createFile(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            return file.createNewFile();
        }
        return false;
    }

    public static String generateHexadecimalCodedString(String stringToBeCoded) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
        digest.update(stringToBeCoded.getBytes());
        byte[] hashedKey = digest.digest();
        final int radix = 16;
        String result = "";
        for (byte b : hashedKey) {
            int unsignedByte = b + 128;
            result += Integer.toString(unsignedByte, radix);
        }
        return result;
    }

    public OutgoingTransferHandle getOutgoingTransferHandle(WorkerComponent component, String fileName) {
        FileTransferDAO ftDAO = component.getDAO(FileTransferDAO.class);
        List<OutgoingTransferHandle> handles = ftDAO.getUploadingFileHandles();
        for (OutgoingTransferHandle handle : handles) {
            if (handle.getLocalFileName().equals(fileName)) {
                return handle;
            }
        }
        return null;
    }

    public IncomingTransferHandle getIncomingTransferHandle(WorkerComponent component, String fileName) {
        FileTransferDAO ftDAO = component.getDAO(FileTransferDAO.class);
        List<IncomingTransferHandle> handles = ftDAO.getIncomingFileHandles();
        for (IncomingTransferHandle handle : handles) {
            if (handle.getLocalFileName().equals(fileName)) {
                return handle;
            }
        }
        return null;
    }

    public static void setAnnotationsWorkerSpec(WorkerSpec workerSpec, List<String> tagsWorker) {
        for (String tag : tagsWorker) {
            workerSpec.addAnnotation(tag, tag);
        }
    }

    public static void setAnnotationsWorkerSpec(WorkerSpec workerSpec, Map<String, String> annotationsWorker) {
        workerSpec.setAnnotations(annotationsWorker);
    }
}
