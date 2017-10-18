package ca.eandb.jdcp.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import org.apache.log4j.Logger;
import ca.eandb.jdcp.job.JobExecutionException;
import ca.eandb.jdcp.job.JobExecutionWrapper;
import ca.eandb.jdcp.job.ParallelizableJob;
import ca.eandb.jdcp.job.TaskDescription;
import ca.eandb.jdcp.job.TaskWorker;
import ca.eandb.jdcp.remote.TaskService;
import ca.eandb.jdcp.server.scheduling.PrioritySerialTaskScheduler;
import ca.eandb.jdcp.server.scheduling.TaskScheduler;
import ca.eandb.util.ClassUtil;
import ca.eandb.util.UnexpectedException;
import ca.eandb.util.concurrent.BackgroundThreadFactory;
import ca.eandb.util.progress.ProgressMonitor;
import ca.eandb.util.progress.ProgressMonitorFactory;
import ca.eandb.util.rmi.Serialized;

/**
 * A <code>JobService</code> implementation.
 * @author Brad Kimmel
 */
public final class TemporaryJobServer implements TaskService {

    /** Serialization version ID. */
    private static final long serialVersionUID = -5172589787776509569L;

    /**
	 * The default amount of time (in seconds) to instruct workers to idle for
	 * if there are no tasks to be processed.
	 */
    private static final int DEFAULT_IDLE_SECONDS = 10;

    /** The <code>Logger</code> for this class. */
    private static final Logger logger = Logger.getLogger(TemporaryJobServer.class);

    /** The <code>Random</code> number generator (for generating task IDs). */
    private static final Random rand = new Random();

    /**
	 * The <code>ProgressMonitorFactory</code> to use to create
	 * <code>ProgressMonitor</code>s for reporting overall progress of
	 * individual jobs.
	 * @see ca.eandb.util.progress.ProgressMonitor
	 */
    private final ProgressMonitorFactory monitorFactory;

    /**
	 * The <code>TaskScheduler</code> to use to select from multiple tasks to
	 * assign to workers.
	 */
    private final TaskScheduler scheduler;

    /**
	 * A <code>Map</code> for looking up <code>ScheduledJob</code> structures
	 * by the corresponding job ID.
	 * @see ca.eandb.jdcp.server.TemporaryJobServer.ScheduledJob
	 */
    private final Map<UUID, ScheduledJob> jobs = new HashMap<UUID, ScheduledJob>();

    /** An <code>Executor</code> to use to run asynchronous tasks. */
    private final Executor executor;

    /**
	 * The <code>TaskDescription</code> to use to notify workers that no tasks
	 * are available to be performed.
	 */
    private TaskDescription idleTask = new TaskDescription(null, 0, DEFAULT_IDLE_SECONDS);

    /**
	 * Creates a new <code>JobServer</code>.
	 * @param outputDirectory The directory to write job results to.
	 * @param monitorFactory The <code>ProgressMonitorFactory</code> to use to
	 * 		create <code>ProgressMonitor</code>s for individual jobs.
	 * @param scheduler The <code>TaskScheduler</code> to use to assign
	 * 		tasks.
	 * @param classManager The <code>ParentClassManager</code> to use to
	 * 		store and retrieve class definitions.
	 * @param executor The <code>Executor</code> to use to run bits of code
	 * 		that should not hold up the remote caller.
	 */
    public TemporaryJobServer(ProgressMonitorFactory monitorFactory, TaskScheduler scheduler, Executor executor) throws IllegalArgumentException {
        this.monitorFactory = monitorFactory;
        this.scheduler = scheduler;
        this.executor = executor;
        logger.info("TemporaryJobServer created");
    }

    /**
	 * Creates a new <code>JobServer</code>.
	 * @param monitorFactory The <code>ProgressMonitorFactory</code> to use to
	 * 		create <code>ProgressMonitor</code>s for individual jobs.
	 * @param scheduler The <code>TaskScheduler</code> to use to assign
	 * 		tasks.
	 */
    public TemporaryJobServer(ProgressMonitorFactory monitorFactory, TaskScheduler scheduler) throws IllegalArgumentException {
        this(monitorFactory, scheduler, Executors.newCachedThreadPool(new BackgroundThreadFactory()));
    }

    /**
	 * Creates a new <code>JobServer</code>.
	 * @param monitorFactory The <code>ProgressMonitorFactory</code> to use to
	 * 		create <code>ProgressMonitor</code>s for individual jobs.
	 */
    public TemporaryJobServer(ProgressMonitorFactory monitorFactory) throws IllegalArgumentException {
        this(monitorFactory, new PrioritySerialTaskScheduler());
    }

    private final Object complete = new Object();

    public void waitForCompletion() throws InterruptedException {
        synchronized (complete) {
            complete.wait();
        }
    }

    public boolean isComplete() {
        return jobs.isEmpty();
    }

    public UUID submitJob(ParallelizableJob job, String description) throws ClassNotFoundException, JobExecutionException {
        ScheduledJob sched = new ScheduledJob(job, description, monitorFactory.createProgressMonitor(description));
        jobs.put(sched.id, sched);
        try {
            sched.scheduleNextTask();
        } catch (JobExecutionException e) {
            handleJobExecutionException(e, sched.id);
            throw e;
        }
        if (logger.isInfoEnabled()) {
            logger.info("Job submitted (" + sched.id.toString() + "): " + description);
        }
        return sched.id;
    }

    public void cancelJob(UUID jobId) throws IllegalArgumentException {
        if (!jobs.containsKey(jobId)) {
            throw new IllegalArgumentException("No job with provided Job ID");
        }
        removeScheduledJob(jobId, false);
    }

    public Serialized<TaskWorker> getTaskWorker(UUID jobId) throws IllegalArgumentException, SecurityException {
        ScheduledJob sched = jobs.get(jobId);
        if (sched != null) {
            return sched.worker;
        }
        throw new IllegalArgumentException("No submitted job with provided Job ID");
    }

    public synchronized TaskDescription requestTask() throws SecurityException {
        TaskDescription taskDesc = scheduler.getNextTask();
        if (taskDesc != null) {
            ScheduledJob sched = jobs.get(taskDesc.getJobId());
            try {
                sched.scheduleNextTask();
            } catch (JobExecutionException e) {
                handleJobExecutionException(e, sched.id);
            }
            return taskDesc;
        }
        return idleTask;
    }

    public void submitTaskResults(final UUID jobId, final int taskId, final Serialized<Object> results) throws SecurityException {
        ScheduledJob sched = jobs.get(jobId);
        if (sched != null) {
            sched.submitTaskResults(taskId, results);
        }
    }

    public void reportException(final UUID jobId, final int taskId, final Exception e) throws SecurityException, RemoteException {
        ScheduledJob sched = jobs.get(jobId);
        if (sched != null) {
            sched.reportException(taskId, e);
        }
    }

    public BitSet getFinishedTasks(UUID[] jobIds, int[] taskIds) throws IllegalArgumentException, SecurityException, RemoteException {
        if (jobIds == null || taskIds == null) {
            return new BitSet(0);
        }
        if (jobIds.length != taskIds.length) {
            throw new IllegalArgumentException("jobIds.length != taskIds.length");
        }
        BitSet finished = new BitSet(jobIds.length);
        for (int i = 0; i < jobIds.length; i++) {
            UUID jobId = jobIds[i];
            int taskId = taskIds[i];
            if (taskId != 0) {
                finished.set(i, jobId == null || !scheduler.contains(jobId, taskId));
            } else {
                ScheduledJob sched = jobs.get(jobId);
                try {
                    finished.set(i, sched == null || sched.job.isComplete());
                } catch (JobExecutionException e) {
                    sched.reportException(0, e);
                }
            }
        }
        return finished;
    }

    public byte[] getClassDefinition(String name, UUID jobId) throws SecurityException {
        return getClassDefinition(name);
    }

    public byte[] getClassDigest(String name, UUID jobId) {
        return getClassDigest(name);
    }

    private byte[] getClassDigest(String name) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            Class<?> cl = Class.forName(name);
            ClassUtil.getClassDigest(cl, md5);
            return md5.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new UnexpectedException(e);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private byte[] getClassDefinition(String name) {
        try {
            Class<?> cl = Class.forName(name);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            ClassUtil.writeClassToStream(cl, stream);
            return stream.toByteArray();
        } catch (ClassNotFoundException e) {
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    public void setIdleTime(int idleSeconds) throws IllegalArgumentException, SecurityException {
        idleTask = new TaskDescription(null, 0, idleSeconds);
        if (logger.isInfoEnabled()) {
            logger.info("Idle time set to " + Integer.toString(idleSeconds));
        }
    }

    public void setJobPriority(UUID jobId, int priority) throws IllegalArgumentException, SecurityException {
        if (!jobs.containsKey(jobId)) {
            throw new IllegalArgumentException("No job with provided Job ID");
        }
        scheduler.setJobPriority(jobId, priority);
        if (logger.isInfoEnabled()) {
            logger.info("Set job " + jobId.toString() + " priority to " + Integer.toString(priority));
        }
    }

    /**
	 * Handles a <code>JobExcecutionException</code> thrown by a job managed
	 * by this server.
	 * @param e The <code>JobExecutionException</code> that was thrown by the
	 * 		job.
	 * @param jobId The <code>UUID</code> identifying the job that threw the
	 * 		exception.
	 */
    private void handleJobExecutionException(JobExecutionException e, UUID jobId) {
        logger.error("Exception thrown from job " + jobId.toString(), e);
        removeScheduledJob(jobId, false);
    }

    /**
	 * Removes a job.
	 * @param jobId The <code>UUID</code> identifying the job to be removed.
	 * @param complete A value indicating whether the job has been completed.
	 */
    private void removeScheduledJob(UUID jobId, boolean complete) {
        ScheduledJob sched = jobs.remove(jobId);
        if (sched != null) {
            if (complete) {
                sched.monitor.notifyComplete();
                if (logger.isInfoEnabled()) {
                    logger.info("Job complete (" + jobId.toString() + ")");
                }
            } else {
                sched.monitor.notifyCancelled();
                if (logger.isInfoEnabled()) {
                    logger.info("Job cancelled (" + jobId.toString() + ")");
                }
            }
            jobs.remove(jobId);
            scheduler.removeJob(jobId);
        }
        if (jobs.isEmpty()) {
            synchronized (this.complete) {
                this.complete.notifyAll();
            }
        }
    }

    /**
	 * Represents a <code>ParallelizableJob</code> that has been submitted
	 * to this <code>JobMasterServer</code>.
	 * @author Brad Kimmel
	 */
    private class ScheduledJob {

        /** The <code>ParallelizableJob</code> to be processed. */
        public JobExecutionWrapper job;

        /** The <code>UUID</code> identifying the job. */
        public final UUID id;

        /** A description of the job. */
        public final String description;

        /** The <code>TaskWorker</code> to use to process tasks for the job. */
        public Serialized<TaskWorker> worker;

        /**
		 * The <code>ProgressMonitor</code> to use to monitor the progress of
		 * the <code>Job</code>.
		 */
        public final ProgressMonitor monitor;

        /**
		 * Initializes the scheduled job.
		 * @param job The <code>ParallelizableJob</code> to run.
		 * @param description A description of the job.
		 * @param monitor The <code>ProgressMonitor</code> to use to monitor
		 * 		the progress of the <code>ParallelizableJob</code>.
		 * @throws JobExecutionException If the job throws an exception.
		 */
        public ScheduledJob(ParallelizableJob job, String description, ProgressMonitor monitor) throws JobExecutionException {
            this.id = UUID.randomUUID();
            this.description = description;
            this.monitor = monitor;
            this.monitor.notifyStatusChanged("Awaiting job submission");
            this.job = new JobExecutionWrapper(job);
            this.worker = new Serialized<TaskWorker>(this.job.worker());
            this.monitor.notifyStatusChanged("");
            this.job.initialize();
        }

        /**
		 * Submits the results for a task associated with this job.
		 * @param taskId The ID of the task whose results are being submitted.
		 * @param results The serialized results.
		 */
        public void submitTaskResults(int taskId, Serialized<Object> results) {
            TaskDescription taskDesc = scheduler.remove(id, taskId);
            if (taskDesc != null) {
                Object task = taskDesc.getTask().get();
                Runnable command = new TaskResultSubmitter(this, task, results, monitor);
                try {
                    executor.execute(command);
                } catch (RejectedExecutionException e) {
                    command.run();
                }
            }
        }

        /**
		 * Reports an exception thrown by a worker while processing a task for
		 * this job.
		 * @param taskId The ID of the task that was being processed.
		 * @param ex The exception that was thrown.
		 */
        public synchronized void reportException(int taskId, Exception ex) {
            if (taskId != 0) {
                logger.error("A worker reported an exception while processing the job", ex);
            } else {
                logger.error("A worker reported an exception while processing a task (" + Integer.toString(taskId) + ")", ex);
            }
        }

        /**
		 * Generates a unique task identifier.
		 * @return The generated task ID.
		 */
        private int generateTaskId() {
            int taskId;
            do {
                taskId = rand.nextInt();
            } while (taskId != 0 && scheduler.contains(id, taskId));
            return taskId;
        }

        /**
		 * Obtains and schedules the next task for this job.
		 * @throws JobExecutionException If the job throws an exception while
		 * 		attempting to obtain the next task.
		 */
        public void scheduleNextTask() throws JobExecutionException {
            Object task = job.getNextTask();
            if (task != null) {
                int taskId = generateTaskId();
                TaskDescription desc = new TaskDescription(id, taskId, task);
                scheduler.add(desc);
            }
        }

        /**
		 * Writes the results of a <code>ScheduledJob</code> to the output
		 * directory.
		 * @param sched The <code>ScheduledJob</code> to write results for.
		 * @throws JobExecutionException If the job throws an exception.
		 */
        private synchronized void finalizeJob() throws JobExecutionException {
            assert (job.isComplete());
            job.finish();
            if (logger.isInfoEnabled()) {
                logger.info(String.format("Job %s completed", id));
            }
        }
    }

    /**
	 * A <code>Runnable</code> task for submitting task results asynchronously.
	 * @author Brad Kimmel
	 */
    private class TaskResultSubmitter implements Runnable {

        /**
		 * The <code>ScheduledJob</code> associated with the task whose results
		 * are being submitted.
		 */
        private final ScheduledJob sched;

        /**
		 * The <code>Object</code> describing the task whose results are being
		 * submitted.
		 */
        private final Object task;

        /** The serialized task results. */
        private final Serialized<Object> results;

        /** The <code>ProgressMonitor</code> to report job progress to. */
        private final ProgressMonitor monitor;

        /**
		 * Creates a new <code>TaskResultSubmitter</code>.
		 * @param sched The <code>ScheduledJob</code> associated with the task
		 * 		whose results are being submitted.
		 * @param task The <code>Object</code> describing the task whose
		 * 		results are being submitted.
		 * @param results The serialized task results.
		 * @param monitor The <code>ProgressMonitor</code> to report job
		 * 		progress to.
		 */
        public TaskResultSubmitter(ScheduledJob sched, Object task, Serialized<Object> results, ProgressMonitor monitor) {
            this.sched = sched;
            this.task = task;
            this.results = results;
            this.monitor = monitor;
        }

        public void run() {
            if (task != null) {
                try {
                    synchronized (sched.job) {
                        sched.job.submitTaskResults(task, results.deserialize(), monitor);
                    }
                    if (sched.job.isComplete()) {
                        sched.finalizeJob();
                        removeScheduledJob(sched.id, true);
                    }
                } catch (JobExecutionException e) {
                    handleJobExecutionException(e, sched.id);
                } catch (ClassNotFoundException e) {
                    logger.error("Exception thrown submitting results of task for job " + sched.id.toString(), e);
                    removeScheduledJob(sched.id, false);
                } catch (Exception e) {
                    logger.error("Exception thrown while attempting to submit task results for job " + sched.id.toString(), e);
                    removeScheduledJob(sched.id, false);
                }
            }
        }
    }
}
