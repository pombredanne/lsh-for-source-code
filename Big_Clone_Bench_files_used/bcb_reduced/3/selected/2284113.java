package de.grogra.gpuflux.jocl;

import static org.jocl.CL.CL_CONTEXT_DEVICES;
import static org.jocl.CL.CL_MEM_COPY_HOST_PTR;
import static org.jocl.CL.CL_MEM_USE_HOST_PTR;
import static org.jocl.CL.CL_PROGRAM_BUILD_LOG;
import static org.jocl.CL.clBuildProgram;
import static org.jocl.CL.clCreateBuffer;
import static org.jocl.CL.clCreateProgramWithSource;
import static org.jocl.CL.clGetContextInfo;
import static org.jocl.CL.clGetProgramBuildInfo;
import static org.jocl.CL.clReleaseContext;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.AbstractList;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Vector;
import org.jocl.CL;
import org.jocl.CLException;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_context;
import org.jocl.cl_device_id;
import org.jocl.cl_program;
import de.grogra.gpuflux.FluxSettings;
import de.grogra.gpuflux.jocl.JOCLDevice;
import de.grogra.gpuflux.utils.ByteArray;
import de.grogra.pf.boot.Main;
import de.grogra.pf.ui.Workbench;

public class JOCLContext {

    public final int KERNEL_VERSION = 12;

    public static final boolean logResourceManagement = false;

    private String log = "";

    private AbstractList<JOCLDevice> devices;

    private cl_device_id[] device_id_array;

    private cl_context context;

    private boolean hasLittleEndian = false;

    private boolean hasBigEndian = false;

    private Vector<String> extensions = new Vector<String>();

    private String extensionOptions = " ";

    public JOCLContext(cl_context context, AbstractList<cl_device_id> device_ids) {
        this.context = context;
        devices = new ArrayList<JOCLDevice>(device_ids.size());
        if (device_ids != null) {
            for (cl_device_id device_id : device_ids) {
                JOCLDevice device = new JOCLDevice(this, device_id);
                log += device + "\n";
                devices.add(device);
                hasLittleEndian |= device.isLittleEndian();
                hasBigEndian |= (!device.isLittleEndian());
            }
            device_id_array = new cl_device_id[devices.size()];
            for (int i = 0; i < devices.size(); i++) device_id_array[i] = devices.get(i).getClDevice();
            String extensionList[] = devices.get(0).getExtensionList();
            for (String extension : extensionList) {
                if (extension.length() != 0) {
                    boolean shared = true;
                    for (JOCLDevice device : devices) {
                        boolean found = false;
                        for (String extension2 : device.getExtensionList()) {
                            if (!extension2.equals(extension)) found = true;
                        }
                        shared &= found;
                    }
                    if (shared) {
                        extensions.add(extension);
                        extensionOptions += " -D " + extension;
                    }
                }
            }
            log += "    Shared Extensions: " + extensions + "\n\n";
        }
    }

    public void finalize() throws Throwable {
        clReleaseContext(context);
        super.finalize();
    }

    public static cl_device_id[] getDevices(cl_context context) {
        long numBytes[] = new long[1];
        clGetContextInfo(context, CL_CONTEXT_DEVICES, 0, null, numBytes);
        int numDevices = (int) numBytes[0] / Sizeof.cl_device_id;
        cl_device_id[] device_ids = new cl_device_id[numDevices];
        clGetContextInfo(context, CL_CONTEXT_DEVICES, numBytes[0], Pointer.to(device_ids), null);
        return device_ids;
    }

    public AbstractList<JOCLDevice> getDevices() {
        return devices;
    }

    public String getIdentifier() {
        String identifier = "";
        for (int i = 0; i < devices.size(); i++) identifier += devices.get(i).getIdentifier() + "__";
        return identifier;
    }

    ;

    public String getLog() {
        return log;
    }

    ;

    private HashMap<String, JOCLProgram> programCache = new HashMap<String, JOCLProgram>();

    public synchronized JOCLProgram getProgram(String key) {
        JOCLProgram program = (JOCLProgram) programCache.get(key);
        return program;
    }

    public synchronized JOCLProgram loadProgram(String key, JOCLSource source, String options) throws IOException {
        JOCLProgram program = (JOCLProgram) programCache.get(key);
        if (program == null && FluxSettings.getOCLPrecompile()) {
            program = loadProgramFromDisk(key, options);
        }
        if (program == null) {
            cl_program cpProgram = clCreateProgramWithSource(context, 1, new String[] { source.getSource() }, null, null);
            try {
                String buildOptions = options + extensionOptions + " -Werror";
                clBuildProgram(cpProgram, device_id_array.length, device_id_array, buildOptions, null, null);
            } catch (CLException e) {
                throw new IOException(getBuildLog(cpProgram));
            }
            program = new JOCLProgram(cpProgram, key, "    " + key + extensionOptions + getBuildLog(cpProgram));
            cacheBinariesOnDisk(program);
        }
        if (program != null) {
            programCache.put(key, program);
        }
        return program;
    }

    public String getCachedProgramKey(cl_device_id program_device, String programKey) {
        String program_key = JOCLProgram.getIdenifier(programKey);
        String device_key = JOCLDevice.getIdentifier(program_device);
        return program_key + "___" + device_key;
    }

    public String getCachedProgramPath(String cacheKey) {
        String dirName = System.getProperty("java.io.tmpdir") + System.getProperty("file.separator") + "GroIMP" + System.getProperty("file.separator");
        String fileName = hashString(cacheKey);
        String path = dirName + fileName + ".tmp";
        (new File(dirName)).mkdir();
        return path;
    }

    private String hashString(String key) {
        MessageDigest digest;
        try {
            digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(key.getBytes());
            byte[] hash = digest.digest();
            BigInteger bi = new BigInteger(1, hash);
            return String.format("%0" + (hash.length << 1) + "X", bi) + KERNEL_VERSION;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return "" + key.hashCode();
        }
    }

    public JOCLProgram loadProgramFromDisk(String key, String options) throws IOException {
        int binary_status[] = new int[devices.size()];
        long lengths[] = new long[devices.size()];
        byte binaries[][] = new byte[devices.size()][];
        for (int i = 0; i < devices.size(); i++) {
            String cache_key = getCachedProgramKey(devices.get(i).getClDevice(), key);
            String path = getCachedProgramPath(cache_key);
            try {
                binaries[i] = readFile(path, cache_key);
                lengths[i] = binaries[i].length;
            } catch (IOException e) {
                return null;
            }
        }
        cl_program cpProgram;
        try {
            cpProgram = CL.clCreateProgramWithBinary(context, device_id_array.length, device_id_array, lengths, binaries, binary_status, null);
            for (int status : binary_status) {
                if (status != CL.CL_SUCCESS) return null;
            }
        } catch (org.jocl.CLException exp) {
            return null;
        }
        try {
            String buildOptions = options + extensionOptions + " -Werror";
            clBuildProgram(cpProgram, device_id_array.length, device_id_array, buildOptions, null, null);
        } catch (CLException e) {
            System.out.println(getBuildLog(cpProgram));
            return null;
        }
        JOCLProgram program = new JOCLProgram(cpProgram, key, "    " + key + extensionOptions + getBuildLog(cpProgram));
        return program;
    }

    private byte[] readFile(String filename, String key) throws IOException {
        File file = new File(filename);
        DataInputStream in = new DataInputStream(new FileInputStream(file));
        int key_length_in = in.readInt();
        byte[] key_in = new byte[key_length_in];
        in.read(key_in);
        if (!(new String(key_in)).equals(key)) {
            Main.getLogger().warning("GPUFlux: cached kernel collision.\nCACHED KEY:\n" + key_in + "\nREQUESTED KEY:\n" + key);
            return null;
        }
        int binary_length_in = in.readInt();
        byte[] data = new byte[binary_length_in];
        in.read(data);
        in.close();
        return data;
    }

    public void cacheBinariesOnDisk(JOCLProgram program) {
        byte[][] binaries = program.getBinarieDatas();
        cl_device_id[] program_devices = program.getDevices();
        for (int i = 0; i < program_devices.length; i++) {
            if (program_devices[i] != null && binaries[i].length > 0) {
                String cache_key = getCachedProgramKey(program_devices[i], program.getName());
                String path = getCachedProgramPath(cache_key);
                try {
                    DataOutputStream out = new DataOutputStream(new FileOutputStream(path));
                    int keyLength = cache_key.getBytes().length;
                    out.writeInt(keyLength);
                    out.write(cache_key.getBytes());
                    int binaryLength = binaries[i].length;
                    out.writeInt(binaryLength);
                    out.write(binaries[i]);
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String getBuildLog(cl_program cpProgram) {
        String buildLog = "";
        for (JOCLDevice device : devices) {
            buildLog += getBuildLog(cpProgram, device.getClDevice());
        }
        return buildLog;
    }

    public JOCLBuffer createBuffer(int size, long flags) {
        return new JOCLBuffer(clCreateBuffer(context, flags, Math.max(size + 3, 4), null, null), size);
    }

    public JOCLBuffer createBufferFromByteArray(ByteArray byteArray, long flags) {
        if (byteArray.size() == 0) return createBuffer(0, flags & (~CL_MEM_COPY_HOST_PTR) & (~CL_MEM_USE_HOST_PTR));
        return new JOCLBuffer(clCreateBuffer(context, flags, byteArray.size() + 3, Pointer.to(byteArray.getBuffer()), null), byteArray.size());
    }

    public boolean hasDevices() {
        return (devices != null) && devices.size() != 0;
    }

    public cl_context getClContext() {
        return context;
    }

    private static String getBuildLog(cl_program program, cl_device_id device) {
        long size[] = new long[1];
        clGetProgramBuildInfo(program, device, CL_PROGRAM_BUILD_LOG, 0, null, size);
        byte buffer[] = new byte[(int) size[0]];
        clGetProgramBuildInfo(program, device, CL_PROGRAM_BUILD_LOG, buffer.length, Pointer.to(buffer), null);
        return new String(buffer, 0, buffer.length - 1);
    }

    public boolean hasLittleEndian() {
        return hasLittleEndian;
    }

    public boolean hasBigEndian() {
        return hasBigEndian;
    }

    public String getExtensions() {
        String str = "";
        for (String extension : extensions) {
            str += extension + " ";
        }
        return str;
    }

    public String getDeviceNames() {
        String names = "";
        for (JOCLDevice device : devices) {
            names += device.getName() + "\n";
        }
        return names;
    }
}
