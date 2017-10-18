package net.jtools.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URLDecoder;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Reference;
import org.jpattern.helper.Helper;
import org.jpattern.mapper.Mapper;
import org.jtools.mapper.MapperHelper;
import org.jtools.mapper.helper.CoalesceHelper;
import org.jtools.meta.meta_inf.antlib.AntDef;
import org.jtools.meta.meta_inf.antlib.AntLib;
import org.jtools.meta.meta_inf.antlib.DefType;
import org.jtools.util.CommonUtils;
import org.jtools.util.herbert.FileInfoOld;

@AntLib(@AntDef(type = DefType.TASK, value = "fileinfo"))
public class FileInfoTask extends Task {

    public static enum Status {

        UNDEF, NOT_FOUND, DIR, FILE
    }

    public static class Classify {

        private final CoalesceHelper coalesce;

        public Classify(CoalesceHelper classifier) {
            this.coalesce = classifier;
        }

        public void add(MapperHelper classifier) {
            coalesce.add(classifier);
        }

        public void add(Helper<Mapper<String[], String>> classifier) {
            coalesce.add(classifier);
        }

        public void add(Mapper<String[], String> classifier) {
            coalesce.add(classifier);
        }
    }

    public static class Rate {

        private final CoalesceHelper coalesce;

        public Rate(CoalesceHelper classifier) {
            this.coalesce = classifier;
        }

        public void add(MapperHelper classifier) {
            coalesce.add(classifier);
        }

        public void add(Helper<Mapper<Integer, String>> classifier) {
            coalesce.add(classifier);
        }

        public void add(Mapper<Integer, String> classifier) {
            coalesce.add(classifier);
        }
    }

    private File root;

    private String file;

    private String algorithms;

    private Status status = Status.UNDEF;

    private List<Object> out;

    private String reportFormat;

    private CoalesceHelper classifier = null;

    private CoalesceHelper rating = null;

    private Classify classify;

    private Rate rate;

    private CoalesceHelper classifier() {
        if (classifier == null) classifier = new CoalesceHelper();
        return classifier;
    }

    private CoalesceHelper rating() {
        if (rating == null) rating = new CoalesceHelper();
        return rating;
    }

    public Classify createClassify() {
        if (classify == null) classify = new Classify(classifier());
        return classify;
    }

    public Rate createRate() {
        if (rate == null) rate = new Rate(rating());
        return rate;
    }

    public void setRoot(File root) {
        this.root = root.getAbsoluteFile();
    }

    public void setFile(String file) {
        this.file = file;
    }

    public void setAlgorithms(String algorithm) {
        this.algorithms = algorithm;
    }

    public void setResultRefid(Reference ref) {
        ResultContainer rc = (ResultContainer) ref.getReferencedObject();
        out = rc.getLines();
    }

    public void setReportFormat(String format) {
        this.reportFormat = format;
    }

    private String hash(String algorithmName, File path) throws NoSuchAlgorithmException, IOException {
        MessageDigest algorithm = MessageDigest.getInstance(algorithmName);
        FileInputStream fis = new FileInputStream(path);
        BufferedInputStream bis = new BufferedInputStream(fis);
        DigestInputStream dis = new DigestInputStream(bis, algorithm);
        while (dis.read() != -1) ;
        byte[] hash = algorithm.digest();
        return CommonUtils.hex(hash);
    }

    @Override
    public void execute() throws BuildException {
        FileInfoOld result = new FileInfoOld();
        StringBuilder autoFormat = null;
        if (reportFormat == null) {
            autoFormat = new StringBuilder(200);
            autoFormat.append("%s;%s;%d;%d;%d");
        }
        URI rootURI = root.toURI();
        try {
            result.setRoot(rootURI.toURL());
        } catch (MalformedURLException e) {
            throw new BuildException("rootURL", e);
        }
        File x = new File(file);
        File path = (x.isAbsolute() ? x : new File(root, file)).getAbsoluteFile();
        URI fileURI = rootURI.relativize(path.toURI().normalize());
        result.setPath(fileURI);
        if (!path.exists()) {
            status = Status.NOT_FOUND;
            System.out.println(getClass().getName() + ": PATH NOT FOUND '" + path + "'");
        } else if (!path.isFile()) {
            status = Status.DIR;
            System.out.println(getClass().getName() + ": PATH IS DIR '" + path + "'");
        } else {
            status = Status.FILE;
            out.add(result);
            result.setLength(path.length());
            result.setLastModified(path.lastModified());
            if (algorithms != null) {
                List<String[]> hashes = new ArrayList<String[]>();
                result.setHashes(hashes);
                for (StringTokenizer algs = new StringTokenizer(algorithms, ",;"); algs.hasMoreTokens(); ) {
                    String alg = algs.nextToken();
                    if (autoFormat != null) autoFormat.append(";%s");
                    try {
                        hashes.add(new String[] { alg, hash(alg, path) });
                    } catch (Exception e) {
                        throw new BuildException(e);
                    }
                }
            }
            if (rating != null) {
                try {
                    Integer attributes = rating.toMapper(Integer.class, String.class).map(URLDecoder.decode(fileURI.toString(), "UTF-8"));
                    if (attributes != null) result.setRating(attributes);
                } catch (Exception e) {
                    throw new BuildException(e);
                }
            }
            if (classifier != null) {
                try {
                    String[] attributes = classifier.toMapper(new String[] {}.getClass(), String.class).map(URLDecoder.decode(fileURI.toString(), "UTF-8"));
                    if (attributes != null) {
                        List<String> attrList = new ArrayList<String>(attributes.length);
                        result.setAttributes(attrList);
                        for (String s : attributes) {
                            attrList.add(s == null ? "" : s);
                            if (autoFormat != null) autoFormat.append(";%s");
                        }
                    }
                } catch (Exception e) {
                    throw new BuildException(e);
                }
            }
            result.setReportFormat(reportFormat != null ? reportFormat : autoFormat.toString());
        }
    }
}
