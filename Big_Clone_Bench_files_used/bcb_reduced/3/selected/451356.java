package pubweb.supernode.sched.dhs;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.Security;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.TreeSet;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import pubweb.IntegrityException;
import pubweb.InternalException;
import pubweb.Job;
import pubweb.JobProcess;
import pubweb.NotEnoughWorkersException;
import pubweb.supernode.sched.DynamicParameterContainer;
import pubweb.supernode.sched.RequirementsContainer;
import pubweb.supernode.sched.Scheduler;
import pubweb.supernode.sched.SchedulerListener;
import pubweb.supernode.sched.StaticParameterContainer;

public class DoubleHashingScheduler extends Scheduler implements Serializable {

    private MigrationThread migrationThread;

    private boolean instantMode = false;

    public DoubleHashingScheduler(SchedulerListener listener) {
        super(listener);
        this.initThread();
    }

    public DoubleHashingScheduler(SchedulerListener listener, boolean instantMode) {
        super(listener);
        if (!instantMode) this.initThread(); else this.migrationThread = new MigrationThread();
        this.instantMode = instantMode;
    }

    public void setListener(SchedulerListener listener) {
        this.listener = listener;
    }

    public void initThread() {
        migrationThread = new MigrationThread();
        migrationThread.start();
    }

    private static final BigInteger MAX_VALUE_INTEGER = new BigInteger("2").pow(256);

    private static final BigDecimal MAX_VALUE_DECIMAL = new BigDecimal(2).pow(256);

    private static MessageDigest md;

    static {
        Security.addProvider(new BouncyCastleProvider());
        try {
            md = MessageDigest.getInstance("SHA-256", "BC");
        } catch (Exception e) {
            System.err.println("message digest init failed:");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static byte[] g(JobProcess process) {
        byte[] buf = new byte[12];
        long id = process.getJob().getId();
        int pid = process.getPid();
        for (int i = 0; i < 8; i++) {
            buf[7 - i] = (byte) (id >>> (i * 8));
        }
        for (int i = 0; i < 4; i++) {
            buf[11 - i] = (byte) (pid >>> (i * 8));
        }
        return buf;
    }

    public static double h(String guid, byte[] processHash) {
        md.reset();
        md.update(guid.getBytes());
        byte[] hash = md.digest(processHash);
        BigInteger x = new BigInteger(hash);
        if (x.signum() == -1) {
            x = MAX_VALUE_INTEGER.add(x);
        }
        return new BigDecimal(x).divide(MAX_VALUE_DECIMAL).doubleValue();
    }

    @Override
    public void jobDied(Job job) throws IntegrityException, InternalException {
        synchronized (migrationThread) {
            DoubleHashedJob dhjob = jobs.remove(job);
            if (dhjob == null) throw new IntegrityException("no such job");
            for (Peer p : peers.values()) {
                for (JobProcess proc : dhjob.processes) {
                    if (proc != null) {
                        p.instances.remove(proc);
                    }
                }
            }
        }
    }

    @Override
    public JobProcess[] locateJob(Job job) throws IntegrityException, InternalException {
        synchronized (migrationThread) {
            if (!jobs.containsKey(job)) throw new IntegrityException("no such job");
            return jobs.get(job).processes;
        }
    }

    @Override
    public JobProcess locateJobProcess(Job job, int pid) throws IntegrityException, InternalException {
        synchronized (migrationThread) {
            if (!jobs.containsKey(job)) throw new IntegrityException("no such job");
            return jobs.get(job).processes[pid];
        }
    }

    @Override
    public void newJob(Job job, RequirementsContainer requirements) throws InternalException, NotEnoughWorkersException {
        if (peers.size() < 1) throw new NotEnoughWorkersException();
        DoubleHashedJob dhjob = new DoubleHashedJob(job.getNumberOfProcessors());
        for (int i = 0; i < job.getNumberOfProcessors(); i++) {
            dhjob.processes[i] = new JobProcess(job, i, null);
            dhjob.g[i] = g(dhjob.processes[i]);
            for (Peer p : peers.values()) {
                ProcessSpecificPeerData pd = new ProcessSpecificPeerData(p, h(p.guid, dhjob.g[i]));
                if (p.isActive()) {
                    dhjob.h[i].add(pd);
                }
                p.instances.put(dhjob.processes[i], pd);
            }
            String lowestPeer = dhjob.h[i].first().peer.guid;
            dhjob.processes[i].setWorker(lowestPeer);
            dhjob.guid[i] = lowestPeer;
        }
        try {
            listener.startProcesses(dhjob.processes);
            synchronized (migrationThread) {
                jobs.put(job, dhjob);
            }
        } catch (Exception e) {
            throw new InternalException("could not start processes", e);
        }
    }

    @Override
    public void processFinished(JobProcess process) throws IntegrityException, InternalException {
        synchronized (migrationThread) {
            DoubleHashedJob dhjob = jobs.get(process.getJob());
            if (dhjob.processes[process.getPid()] == null) throw new IntegrityException("Double Hashing Scheduler: a process finished, but had already finished");
            JobProcess key = dhjob.processes[process.getPid()];
            dhjob.processes[process.getPid()] = null;
            for (Peer p : peers.values()) {
                p.instances.remove(key);
            }
            boolean allNull = true;
            for (int i = 0; i < dhjob.processes.length; i++) {
                if (dhjob.processes[i] != null) allNull = false;
            }
            if (allNull) jobs.remove(process.getJob());
        }
    }

    @Override
    public void processMigrated(JobProcess process, String srcGuid) throws IntegrityException, InternalException {
        synchronized (migrationThread) {
            DoubleHashedJob dhjob = jobs.get(process.getJob());
            if (dhjob.processes[process.getPid()] == null) throw new IntegrityException("Double Hashing Scheduler: a process migrated, but had already finished");
            if (!dhjob.processes[process.getPid()].getWorker().equals(srcGuid)) throw new IntegrityException("Double Hashing Scheduler: a process migrated, but didnt run on pretended srcGuid");
            dhjob.processes[process.getPid()] = process;
            dhjob.guid[process.getPid()] = process.getWorker();
            dhjob.isMigrating[process.getPid()] = false;
            if (!this.instantMode) migrationThread.notify();
        }
    }

    @Override
    public void workerJoined(String guid, StaticParameterContainer staticParams, DynamicParameterContainer dynamicParams) throws InternalException {
        Peer peer = new Peer(guid, dynamicParams.availCpuPower, staticParams.cpuPower);
        peers.put(guid, peer);
        for (DoubleHashedJob dhjob : jobs.values()) {
            for (int i = 0; i < dhjob.processes.length; i++) {
                if (dhjob.processes[i] != null) {
                    ProcessSpecificPeerData pd = new ProcessSpecificPeerData(peer, h(guid, dhjob.g[i]));
                    if (peer.isActive()) {
                        dhjob.h[i].add(pd);
                    }
                    peer.instances.put(dhjob.processes[i], pd);
                }
            }
        }
        synchronized (migrationThread) {
            for (DoubleHashedJob job : jobs.values()) {
                for (int i = 0; i < job.processes.length; i++) {
                    if (job.processes[i] != null) {
                        String target = job.h[i].first().peer.guid;
                        if (!job.guid[i].equals(target)) {
                            job.targetGuid[i] = target;
                        }
                    }
                }
            }
            if (this.instantMode) executeMigrations(); else migrationThread.notify();
        }
    }

    @Override
    public void workerLeft(String guid) throws InternalException {
        Peer peer = peers.remove(guid);
        for (DoubleHashedJob dhjob : jobs.values()) {
            for (int i = 0; i < dhjob.processes.length; i++) {
                if (dhjob.processes[i] != null) {
                    dhjob.h[i].remove(peer.instances.get(dhjob.processes[i]));
                    if (dhjob.processes[i].getWorker().equals(guid)) {
                        String lowestPeer = dhjob.h[i].first().peer.guid;
                        dhjob.processes[i].setWorker(lowestPeer);
                        dhjob.guid[i] = lowestPeer;
                    }
                }
            }
        }
    }

    @Override
    public void workerLoadChanged(String guid, DynamicParameterContainer dynamicParams) throws InternalException {
        bulkWorkerLoadChanged(guid, dynamicParams);
        bulkWorkerLoadUpdatesComplete();
    }

    public void bulkWorkerLoadChanged(String guid, DynamicParameterContainer dynamicParams) throws InternalException {
        Peer p = peers.get(guid);
        for (DoubleHashedJob dhjob : jobs.values()) {
            for (int i = 0; i < dhjob.processes.length; i++) {
                if (dhjob.processes[i] != null) {
                    dhjob.h[i].remove(p.instances.get(dhjob.processes[i]));
                }
            }
        }
        p.setCpuAvailability(dynamicParams.availCpuPower);
        if (p.isActive()) {
            for (DoubleHashedJob dhjob : jobs.values()) {
                for (int i = 0; i < dhjob.processes.length; i++) {
                    if (dhjob.processes[i] != null) {
                        dhjob.h[i].add(p.instances.get(dhjob.processes[i]));
                    }
                }
            }
        }
    }

    public void bulkWorkerLoadUpdatesComplete() throws InternalException {
        synchronized (migrationThread) {
            for (DoubleHashedJob job : jobs.values()) {
                for (int i = 0; i < job.processes.length; i++) {
                    if (job.processes[i] != null) {
                        String target = job.h[i].first().peer.guid;
                        if (!job.guid[i].equals(target)) {
                            job.targetGuid[i] = target;
                        }
                    }
                }
            }
            if (this.instantMode) executeMigrations(); else migrationThread.notify();
        }
    }

    private Hashtable<String, Peer> peers = new Hashtable<String, Peer>();

    private Hashtable<Job, DoubleHashedJob> jobs = new Hashtable<Job, DoubleHashedJob>();

    public static class DoubleHashedJob implements Serializable {

        @SuppressWarnings("unchecked")
        public DoubleHashedJob(int number) {
            processes = new JobProcess[number];
            g = new byte[number][];
            isMigrating = new boolean[number];
            targetGuid = new String[number];
            guid = new String[number];
            h = new TreeSet[number];
            for (int i = 0; i < number; i++) {
                isMigrating[i] = false;
                targetGuid[i] = null;
                h[i] = new TreeSet<ProcessSpecificPeerData>();
            }
        }

        public JobProcess[] processes;

        public byte[] g[];

        public boolean isMigrating[];

        public String targetGuid[];

        public String guid[];

        public TreeSet<ProcessSpecificPeerData>[] h;

        private static final long serialVersionUID = -7687128163665005714L;
    }

    public static class Peer implements Serializable {

        public String guid;

        private double slope;

        private double cpuFactor;

        private double cpuAvailability;

        public Peer(String guid, double avail, double factor) {
            this.guid = guid;
            this.cpuFactor = factor;
            setCpuAvailability(avail);
        }

        public void setCpuAvailability(double avail) {
            cpuAvailability = avail;
            if (cpuAvailability > 0.0) {
                slope = 1.0 / (cpuFactor * cpuAvailability);
            } else {
                slope = Double.MAX_VALUE;
            }
        }

        public boolean isActive() {
            return cpuAvailability > 0.0;
        }

        public double getSlope() {
            return slope;
        }

        public double getCpuFactor() {
            return cpuFactor;
        }

        public HashMap<JobProcess, ProcessSpecificPeerData> instances = new HashMap<JobProcess, ProcessSpecificPeerData>();

        private static final long serialVersionUID = -9026617337677764430L;
    }

    public static class ProcessSpecificPeerData implements Comparable<ProcessSpecificPeerData>, Serializable {

        public Peer peer;

        public double h;

        public ProcessSpecificPeerData(Peer p, double h) {
            peer = p;
            this.h = h;
        }

        public int compareTo(ProcessSpecificPeerData p) {
            return (int) Math.signum(h * peer.slope - p.h * p.peer.slope);
        }
    }

    public Hashtable<String, Peer> getPeers() {
        return peers;
    }

    public Hashtable<Job, DoubleHashedJob> getJobs() {
        return jobs;
    }

    public Object getScheduleSnapshot() {
        return this;
    }

    public class MigrationThread extends Thread implements Serializable {

        public void run() {
            while (true) {
                try {
                    synchronized (migrationThread) {
                        this.wait();
                        executeMigrations();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        private static final long serialVersionUID = -1116322396335811724L;
    }

    private void executeMigrations() {
        for (DoubleHashedJob dhjob : jobs.values()) {
            for (int i = 0; i < dhjob.processes.length; i++) {
                if (dhjob.processes[i] != null && !dhjob.isMigrating[i] && dhjob.targetGuid[i] != null && !dhjob.guid[i].equals(dhjob.targetGuid[i])) {
                    try {
                        dhjob.isMigrating[i] = true;
                        listener.migrateProcess(dhjob.guid[i], dhjob.targetGuid[i], new JobProcess(dhjob.processes[i].getJob(), dhjob.processes[i].getPid(), dhjob.processes[i].getWorker()));
                    } catch (Exception e) {
                        System.err.println("DHS: migrate process command failed:");
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private static final long serialVersionUID = -6287886530233685900L;
}
