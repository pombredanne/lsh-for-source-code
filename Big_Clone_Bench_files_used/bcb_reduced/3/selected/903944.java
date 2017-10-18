package org.codebistro.util;

import com.google.common.io.CharStreams;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Writer;
import java.security.MessageDigest;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Poor man's database: transparently save 'subject' to the 'backingFile' on disk.
 * see TestPersister.java for usage scenario
 */
public class Persister<T> {

    static Logger log = LoggerFactory.getLogger(Persister.class);

    Provider<File> backingFile;

    T subject;

    Class<T> subjectClass;

    Timer timer;

    ObjectMapper mapper;

    byte[] storedDigest;

    long pollingPeriod;

    public Persister(Provider<File> backingFile, Class<T> subjectClass) {
        this.backingFile = backingFile;
        this.subject = null;
        this.subjectClass = subjectClass;
        this.mapper = new ObjectMapper();
        mapper.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
        this.pollingPeriod = 2000;
        createTimerTask();
    }

    public synchronized T get() {
        try {
            if (subject == null) {
                File backingFile = this.backingFile.provide();
                if (backingFile.exists()) {
                    String value = CharStreams.toString(new FileReader(backingFile));
                    subject = mapper.readValue(value, subjectClass);
                    storedDigest = calculateDigest(value);
                } else subject = subjectClass.newInstance();
            }
            return subject;
        } catch (Exception e) {
            throw Bark.unchecker(e);
        }
    }

    void createTimerTask() {
        timer = new Timer("Persister<" + subjectClass.getName() + ">");
        timer.schedule(new TimerTask() {

            public void run() {
                watchdog();
            }
        }, pollingPeriod, pollingPeriod);
    }

    synchronized void watchdog() {
        if (subject == null) return;
        try {
            String value = mapper.writeValueAsString(subject);
            byte[] digest = calculateDigest(value);
            if (storedDigest == null || !equals(digest, storedDigest)) {
                if (log.isTraceEnabled()) log.trace("Storing '{}'", Strings14.limit(value, 100, "..."));
                store(value);
                storedDigest = digest;
            }
        } catch (Exception e) {
            throw Bark.unchecker(e);
        }
    }

    void store(String value) {
        if (subject == null) return;
        File backingFile = this.backingFile.provide();
        try {
            Writer writer = new FileWriter(backingFile);
            writer.write(value);
            writer.close();
        } catch (Exception e) {
            throw new Bark("Writing to '" + backingFile.getAbsolutePath() + "'", e);
        }
    }

    byte[] calculateDigest(String value) {
        try {
            MessageDigest mg = MessageDigest.getInstance("SHA1");
            mg.update(value.getBytes());
            return mg.digest();
        } catch (Exception e) {
            throw Bark.unchecker(e);
        }
    }

    public synchronized void close() {
        watchdog();
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    public void setPollingPeriod(long pollingPeriod) {
        this.pollingPeriod = pollingPeriod;
    }

    /**
     *  This function is needed because byte[].equals(byte[]) returns false on a legal set!
     *  So silly it's not even funny.
     */
    static boolean equals(byte[] a, byte[] b) {
        if (a.length != b.length) return false;
        for (int i = 0; i < a.length; i++) if (a[i] != b[i]) return false;
        return true;
    }
}
