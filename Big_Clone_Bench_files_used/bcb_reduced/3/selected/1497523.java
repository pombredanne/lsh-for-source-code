package service_manager;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class WindowsPlatformHandler extends PlatformHandler {

    public static final String SIMPLE_SERVICE_MANAGER_LOCATION = "/service_manager/SimpleServiceManager.exe";

    public static final String EXE_CONFIG_FILE = "service.cfg";

    File simpleServiceManagerExe;

    File nativeAppPath;

    String stdOut;

    String stdErr;

    /**
   * Constructor
   * @param serviceConfiguration
   */
    public WindowsPlatformHandler(ServiceConfiguration serviceConfiguration) throws ServiceManagerException {
        super(serviceConfiguration);
    }

    /**
   * Install a windows service 
   * @throws ServiceManagerException
   */
    public void install() throws ServiceManagerException, IOException, InterruptedException {
        ensureNativeWindowsApplicationExists();
        setupFiles();
        writeConfigFile();
        String args[];
        if (serviceConfiguration.getWindowsInstallPassword() == null) {
            args = new String[2];
            args[0] = simpleServiceManagerExe.getAbsolutePath();
            args[1] = "/install";
        } else {
            args = new String[3];
            args[0] = simpleServiceManagerExe.getAbsolutePath();
            args[1] = "/install";
            args[2] = serviceConfiguration.getWindowsInstallPassword();
        }
        runExternalCommand(args, true);
    }

    /**
   * Uninstall a windows service
   * @throws ServiceManagerException
   */
    public void uninstall() throws ServiceManagerException, IOException, InterruptedException {
        ensureNativeWindowsApplicationExists();
        writeConfigFile();
        String args[] = new String[2];
        args[0] = simpleServiceManagerExe.getAbsolutePath();
        args[1] = "/uninstall";
        runExternalCommand(args, true);
    }

    /**
   * Start an installed service
   * 
   * @throws ServiceManagerException
   */
    public void start() throws ServiceManagerException, IOException, InterruptedException {
        ensureNativeWindowsApplicationExists();
        writeConfigFile();
        String args[] = new String[2];
        args[0] = simpleServiceManagerExe.getAbsolutePath();
        args[1] = "/start";
        runExternalCommand(args, true);
    }

    /**
   * Stop an installed service
   * 
   * @throws ServiceManagerException
   */
    public void stop() throws ServiceManagerException, IOException, InterruptedException {
        ensureNativeWindowsApplicationExists();
        writeConfigFile();
        String args[] = new String[2];
        args[0] = simpleServiceManagerExe.getAbsolutePath();
        args[1] = "/stop";
        runExternalCommand(args, true);
    }

    /**
   * Check if a windows service is installed
   * @returns True if the service is installed
   * @throws ServiceManagerException
   */
    public boolean isInstalled() throws ServiceManagerException, IOException, InterruptedException {
        ensureNativeWindowsApplicationExists();
        writeConfigFile();
        String args[] = new String[2];
        args[0] = simpleServiceManagerExe.getAbsolutePath();
        args[1] = "/installed";
        int errorCode = runExternalCommand(args, false);
        if (errorCode == 0) {
            return true;
        }
        return false;
    }

    /**
   * Check if a windows service is running
   * @returns True if the service is running
   * @throws ServiceManagerException
   */
    public boolean isRunning() throws ServiceManagerException, IOException, InterruptedException {
        ensureNativeWindowsApplicationExists();
        writeConfigFile();
        String args[] = new String[2];
        args[0] = simpleServiceManagerExe.getAbsolutePath();
        args[1] = "/running";
        int errorCode = runExternalCommand(args, false);
        if (errorCode == 0) {
            return true;
        }
        return false;
    }

    /**
   * Run the configured command as administrator.
   *
   * @returns The programs error code.
   */
    public int administratorRun() throws ServiceManagerException, IOException, InterruptedException {
        ensureNativeWindowsApplicationExists();
        writeConfigFile();
        String args[] = new String[2];
        args[0] = simpleServiceManagerExe.getAbsolutePath();
        args[1] = "/run";
        return runExternalCommand(args, true);
    }

    /**
   * Run an external command and throw an exception if it does not return error code = 0 
   * 
   * @param args The arguments for the shell command, the first argument is the native exe file.
   * @param throwExceptionOnError If true then a ServiceManagerException is thrown if the native 
                                  windows code does not return an error code of 0.
                                  If false then this method returns true if the native windows code
                                  returns an error code of 0 and false if the native windows code 
                                  does not return an error code of 0. 
   * @return The programs error code
   */
    public int runExternalCommand(String args[], boolean throwExceptionOnError) throws ServiceManagerException, IOException, InterruptedException {
        ShellCmd shellCmd = new ShellCmd();
        int errorCode = shellCmd.runSysCmd(args);
        stdOut = shellCmd.getStdOut();
        stdErr = shellCmd.getStdErr();
        if (throwExceptionOnError && errorCode != 0) {
            throw new ServiceManagerException(args[0] + " returned and error (error code = " + errorCode + ").");
        }
        return errorCode;
    }

    /**
   * Get all text read from stdout when the command was executed when administratorRun was called previously.
   * 
   * @return The text read from stdout
   */
    public String getStdOut() {
        if (stdOut == null) {
            return "";
        }
        return stdOut;
    }

    /**
   * Get all text read from stderr when the command was executed when administratorRun was called previously.
   * 
   * @return The text read from stderr
   */
    public String getStdErr() {
        if (stdErr == null) {
            return "";
        }
        return stdErr;
    }

    /**
   * @return True if called on a windows platform
   */
    public boolean isPlatformSupported() {
        if (WindowsPlatformHandler.IsWindowsPlatform()) {
            return true;
        }
        return false;
    }

    /**
   * 
   * @return true if running on a windows operating system. 
   */
    public static boolean IsWindowsPlatform() {
        if (PlatformHandler.GetOSName().startsWith("Windows")) {
            return true;
        } else {
            return false;
        }
    }

    /**
   * Get the root directory of the boot disk
   * @return
   */
    public static File GetRootPath() throws ServiceManagerException {
        File userPath = new File(System.getProperty("user.home"));
        File roots[] = File.listRoots();
        File root = null;
        for (File r : roots) {
            if (userPath.getAbsolutePath().startsWith(r.getAbsolutePath())) {
                root = r;
                break;
            }
        }
        if (root == null) {
            throw new ServiceManagerException("Failed to find the root directory of " + userPath);
        }
        return root;
    }

    /**
   * Ensure that the required windows native code exists (SimpleServieManager.exe)
   */
    private void ensureNativeWindowsApplicationExists() throws ServiceManagerException {
        String startupPath = System.getProperty("user.dir");
        File root = new File(startupPath);
        if (!root.canWrite()) {
            throw new ServiceManagerException("Unable to write to " + root);
        }
        nativeAppPath = new File(root, "." + serviceConfiguration.getServiceName() + "_service");
        serviceConfiguration.setShellCommandPath(nativeAppPath.getAbsolutePath());
        if (!nativeAppPath.isDirectory()) {
            if (!nativeAppPath.mkdir()) {
                throw new ServiceManagerException("Failed to create the " + nativeAppPath.getAbsolutePath() + " path.");
            }
        }
        if (!nativeAppPath.canWrite()) {
            throw new ServiceManagerException("Unable to write to the " + nativeAppPath.getAbsolutePath() + " path.");
        }
        simpleServiceManagerExe = new File(nativeAppPath, "SimpleServiceManager.exe");
        try {
            boolean installEXE = false;
            FileOutputStream fos = null;
            if (!simpleServiceManagerExe.exists()) {
                installEXE = true;
            }
            if (simpleServiceManagerExe.exists()) {
                try {
                    byte[] newEXEContents = readBinaryFileFromJar(SIMPLE_SERVICE_MANAGER_LOCATION);
                    byte[] oldEXEContents = readBinaryFileFromDisk(simpleServiceManagerExe);
                    byte newDigest[] = getMD5Digest(newEXEContents);
                    byte oldDigest[] = getMD5Digest(oldEXEContents);
                    if (!MessageDigest.isEqual(newDigest, oldDigest)) {
                        installEXE = true;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    String msg = e.getLocalizedMessage();
                    if (msg == null) {
                        throw new ServiceManagerException("Unable to check the SimpleServiceManager.exe file version.");
                    } else {
                        throw new ServiceManagerException("Unable to check the SimpleServiceManager.exe file version: " + msg);
                    }
                }
            }
            if (installEXE) {
                try {
                    byte[] newEXEContents = readBinaryFileFromJar(SIMPLE_SERVICE_MANAGER_LOCATION);
                    fos = new FileOutputStream(simpleServiceManagerExe);
                    fos.write(newEXEContents);
                } finally {
                    if (fos != null) {
                        fos.close();
                        fos = null;
                    }
                }
            }
        } catch (IOException e) {
            throw new ServiceManagerException("Failed to create the " + simpleServiceManagerExe + " file.");
        }
    }

    private byte[] getMD5Digest(byte[] values) throws FileNotFoundException, NoSuchAlgorithmException, IOException {
        MessageDigest complete = MessageDigest.getInstance("MD5");
        complete.update(values, 0, values.length);
        return complete.digest();
    }

    /**
   * Read the contents of a file from the jar
   * 
   * @param fileName
   * @return The bytes read from the file
   * @throws IOException
   */
    private byte[] readBinaryFileFromJar(String fileName) throws IOException {
        InputStream input = getClass().getResourceAsStream(fileName);
        ByteArrayOutputStream output = new ByteArrayOutputStream(1024);
        byte[] buffer = new byte[512];
        int bytes;
        while ((bytes = input.read(buffer)) > 0) {
            output.write(buffer, 0, bytes);
        }
        input.close();
        return output.toByteArray();
    }

    /**
   * Read a file from disk
   * 
   * @param fileName
   * @return The bytes read from the file
   * @throws IOException
   */
    private byte[] readBinaryFileFromDisk(File theFile) throws IOException {
        if (theFile.isFile()) {
            byte contents[] = new byte[(int) theFile.length()];
            BufferedInputStream bis = null;
            try {
                bis = new BufferedInputStream(new FileInputStream(theFile));
                bis.read(contents);
            } finally {
                if (bis != null) {
                    try {
                        bis.close();
                    } catch (IOException e) {
                    }
                }
            }
            return contents;
        } else {
            throw new FileNotFoundException(theFile.getAbsolutePath() + " file not found.");
        }
    }

    /**
   * write the configuration file into the path where the native windows code sits. 
   * The native windows code reads this file on execution.
   * @throws ServiceManagerException
   * @throws IOException
   */
    private void writeConfigFile() throws ServiceManagerException, IOException {
        File configFile = new File(nativeAppPath, WindowsPlatformHandler.EXE_CONFIG_FILE);
        Writer writer = null;
        try {
            writer = new FileWriter(configFile);
            writer.write("service_name=" + serviceConfiguration.getServiceName() + "\r\n");
            writer.write("service_description=" + serviceConfiguration.getServiceDescription() + "\r\n");
            writer.write("command=" + serviceConfiguration.getShellCommand() + "\r\n");
            writer.write("command_directory=" + serviceConfiguration.getShellCommandPath() + "\r\n");
            writer.write("command_args=" + serviceConfiguration.getShellCommandArgs() + "\r\n");
            if (serviceConfiguration.getLogFile() != null && serviceConfiguration.getLogFile().length() > 0) {
                writer.write("logfile=" + serviceConfiguration.getLogFile() + "\r\n");
            }
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    /**
   * Call a method for the service type to setup/copy any files required in the service launcher path.
   * @throws ServiceManagerException
   * @throws IOException
   */
    private void setupFiles() throws ServiceManagerException, IOException {
        serviceConfiguration.updateWindowsFiles(nativeAppPath);
    }

    public void setServiceConfiguration(ServiceConfiguration serviceConfiguration) throws ServiceManagerException {
        this.serviceConfiguration = serviceConfiguration;
        validateAttributes();
    }

    public ServiceConfiguration getServiceConfiguration() {
        return serviceConfiguration;
    }
}
