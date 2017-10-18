package com.sun.tools.javac.main;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.MissingResourceException;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.annotation.processing.Processor;
import com.sun.tools.javac.code.Source;
import com.sun.tools.javac.file.CacheFSInfo;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.jvm.Target;
import com.sun.tools.javac.main.JavacOption.Option;
import com.sun.tools.javac.main.RecognizedOptions.OptionHelper;
import com.sun.tools.javac.util.*;
import com.sun.tools.javac.processing.AnnotationProcessingError;
import static com.sun.tools.javac.main.OptionName.*;

/** This class provides a commandline interface to the GJC compiler.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class Main {

    /** The name of the compiler, for use in diagnostics.
     */
    String ownName;

    /** The writer to use for diagnostic output.
     */
    PrintWriter out;

    /**
     * If true, certain errors will cause an exception, such as command line
     * arg errors, or exceptions in user provided code.
     */
    boolean apiMode;

    /** Result codes.
     */
    static final int EXIT_OK = 0, EXIT_ERROR = 1, EXIT_CMDERR = 2, EXIT_SYSERR = 3, EXIT_ABNORMAL = 4;

    private Option[] recognizedOptions = RecognizedOptions.getJavaCompilerOptions(new OptionHelper() {

        public void setOut(PrintWriter out) {
            Main.this.out = out;
        }

        public void error(String key, Object... args) {
            Main.this.error(key, args);
        }

        public void printVersion() {
            Log.printLines(out, getLocalizedString("version", ownName, JavaCompiler.version()));
        }

        public void printFullVersion() {
            Log.printLines(out, getLocalizedString("fullVersion", ownName, JavaCompiler.fullVersion()));
        }

        public void printHelp() {
            help();
        }

        public void printXhelp() {
            xhelp();
        }

        public void addFile(File f) {
            if (!filenames.contains(f)) filenames.append(f);
        }

        public void addClassName(String s) {
            classnames.append(s);
        }
    });

    /**
     * Construct a compiler instance.
     */
    public Main(String name) {
        this(name, new PrintWriter(System.err, true));
    }

    /**
     * Construct a compiler instance.
     */
    public Main(String name, PrintWriter out) {
        this.ownName = name;
        this.out = out;
    }

    /** A table of all options that's passed to the JavaCompiler constructor.  */
    private Options options = null;

    /** The list of source files to process
     */
    public ListBuffer<File> filenames = null;

    /** List of class files names passed on the command line
     */
    public ListBuffer<String> classnames = null;

    /** Print a string that explains usage.
     */
    void help() {
        Log.printLines(out, getLocalizedString("msg.usage.header", ownName));
        for (int i = 0; i < recognizedOptions.length; i++) {
            recognizedOptions[i].help(out);
        }
        out.println();
    }

    /** Print a string that explains usage for X options.
     */
    void xhelp() {
        for (int i = 0; i < recognizedOptions.length; i++) {
            recognizedOptions[i].xhelp(out);
        }
        out.println();
        Log.printLines(out, getLocalizedString("msg.usage.nonstandard.footer"));
    }

    /** Report a usage error.
     */
    void error(String key, Object... args) {
        if (apiMode) {
            String msg = getLocalizedString(key, args);
            throw new PropagatedException(new IllegalStateException(msg));
        }
        warning(key, args);
        Log.printLines(out, getLocalizedString("msg.usage", ownName));
    }

    /** Report a warning.
     */
    void warning(String key, Object... args) {
        Log.printLines(out, ownName + ": " + getLocalizedString(key, args));
    }

    public Option getOption(String flag) {
        for (Option option : recognizedOptions) {
            if (option.matches(flag)) return option;
        }
        return null;
    }

    public void setOptions(Options options) {
        if (options == null) throw new NullPointerException();
        this.options = options;
    }

    public void setAPIMode(boolean apiMode) {
        this.apiMode = apiMode;
    }

    /** Process command line arguments: store all command line options
     *  in `options' table and return all source filenames.
     *  @param flags    The array of command line arguments.
     */
    public List<File> processArgs(String[] flags) {
        int ac = 0;
        while (ac < flags.length) {
            String flag = flags[ac];
            ac++;
            Option option = null;
            if (flag.length() > 0) {
                int firstOptionToCheck = flag.charAt(0) == '-' ? 0 : recognizedOptions.length - 1;
                for (int j = firstOptionToCheck; j < recognizedOptions.length; j++) {
                    if (recognizedOptions[j].matches(flag)) {
                        option = recognizedOptions[j];
                        break;
                    }
                }
            }
            if (option == null) {
                error("err.invalid.flag", flag);
                return null;
            }
            if (option.hasArg()) {
                if (ac == flags.length) {
                    error("err.req.arg", flag);
                    return null;
                }
                String operand = flags[ac];
                ac++;
                if (option.process(options, flag, operand)) return null;
            } else {
                if (option.process(options, flag)) return null;
            }
        }
        if (!checkDirectory(D)) return null;
        if (!checkDirectory(S)) return null;
        String sourceString = options.get(SOURCE);
        Source source = (sourceString != null) ? Source.lookup(sourceString) : Source.DEFAULT;
        String targetString = options.get(TARGET);
        Target target = (targetString != null) ? Target.lookup(targetString) : Target.DEFAULT;
        if (Character.isDigit(target.name.charAt(0))) {
            if (target.compareTo(source.requiredTarget()) < 0) {
                if (targetString != null) {
                    if (sourceString == null) {
                        warning("warn.target.default.source.conflict", targetString, source.requiredTarget().name);
                    } else {
                        warning("warn.source.target.conflict", sourceString, source.requiredTarget().name);
                    }
                    return null;
                } else {
                    target = source.requiredTarget();
                    options.put("-target", target.name);
                }
            } else {
                if (targetString == null && !source.allowGenerics()) {
                    target = Target.JDK1_4;
                    options.put("-target", target.name);
                }
            }
        }
        String showClass = options.get("showClass");
        if (showClass != null) {
            if (showClass.equals("showClass")) showClass = "com.sun.tools.javac.Main";
            showClass(showClass);
        }
        return filenames.toList();
    }

    private boolean checkDirectory(OptionName optName) {
        String value = options.get(optName);
        if (value == null) return true;
        File file = new File(value);
        if (!file.exists()) {
            error("err.dir.not.found", value);
            return false;
        }
        if (!file.isDirectory()) {
            error("err.file.not.directory", value);
            return false;
        }
        return true;
    }

    /** Programmatic interface for main function.
     * @param args    The command line parameters.
     */
    public int compile(String[] args) {
        Context context = new Context();
        JavacFileManager.preRegister(context);
        int result = compile(args, context);
        if (fileManager instanceof JavacFileManager) {
            ((JavacFileManager) fileManager).close();
        }
        return result;
    }

    public int compile(String[] args, Context context) {
        return compile(args, context, List.<JavaFileObject>nil(), null);
    }

    /** Programmatic interface for main function.
     * @param args    The command line parameters.
     */
    public int compile(String[] args, Context context, List<JavaFileObject> fileObjects, Iterable<? extends Processor> processors) {
        if (options == null) options = Options.instance(context);
        filenames = new ListBuffer<File>();
        classnames = new ListBuffer<String>();
        JavaCompiler comp = null;
        try {
            if (args.length == 0 && fileObjects.isEmpty()) {
                help();
                return EXIT_CMDERR;
            }
            List<File> files;
            try {
                files = processArgs(CommandLine.parse(args));
                if (files == null) {
                    return EXIT_CMDERR;
                } else if (files.isEmpty() && fileObjects.isEmpty() && classnames.isEmpty()) {
                    if (options.isSet(HELP) || options.isSet(X) || options.isSet(VERSION) || options.isSet(FULLVERSION)) return EXIT_OK;
                    if (JavaCompiler.explicitAnnotationProcessingRequested(options)) {
                        error("err.no.source.files.classes");
                    } else {
                        error("err.no.source.files");
                    }
                    return EXIT_CMDERR;
                }
            } catch (java.io.FileNotFoundException e) {
                Log.printLines(out, ownName + ": " + getLocalizedString("err.file.not.found", e.getMessage()));
                return EXIT_SYSERR;
            }
            boolean forceStdOut = options.isSet("stdout");
            if (forceStdOut) {
                out.flush();
                out = new PrintWriter(System.out, true);
            }
            context.put(Log.outKey, out);
            boolean batchMode = (options.isUnset("nonBatchMode") && System.getProperty("nonBatchMode") == null);
            if (batchMode) CacheFSInfo.preRegister(context);
            fileManager = context.get(JavaFileManager.class);
            comp = JavaCompiler.instance(context);
            if (comp == null) return EXIT_SYSERR;
            Log log = Log.instance(context);
            if (!files.isEmpty()) {
                comp = JavaCompiler.instance(context);
                List<JavaFileObject> otherFiles = List.nil();
                JavacFileManager dfm = (JavacFileManager) fileManager;
                for (JavaFileObject fo : dfm.getJavaFileObjectsFromFiles(files)) otherFiles = otherFiles.prepend(fo);
                for (JavaFileObject fo : otherFiles) fileObjects = fileObjects.prepend(fo);
            }
            comp.compile(fileObjects, classnames.toList(), processors);
            if (log.expectDiagKeys != null) {
                if (log.expectDiagKeys.isEmpty()) {
                    Log.printLines(log.noticeWriter, "all expected diagnostics found");
                    return EXIT_OK;
                } else {
                    Log.printLines(log.noticeWriter, "expected diagnostic keys not found: " + log.expectDiagKeys);
                    return EXIT_ERROR;
                }
            }
            if (comp.errorCount() != 0) return EXIT_ERROR;
        } catch (IOException ex) {
            ioMessage(ex);
            return EXIT_SYSERR;
        } catch (OutOfMemoryError ex) {
            resourceMessage(ex);
            return EXIT_SYSERR;
        } catch (StackOverflowError ex) {
            resourceMessage(ex);
            return EXIT_SYSERR;
        } catch (FatalError ex) {
            feMessage(ex);
            return EXIT_SYSERR;
        } catch (AnnotationProcessingError ex) {
            if (apiMode) throw new RuntimeException(ex.getCause());
            apMessage(ex);
            return EXIT_SYSERR;
        } catch (ClientCodeException ex) {
            throw new RuntimeException(ex.getCause());
        } catch (PropagatedException ex) {
            throw ex.getCause();
        } catch (Throwable ex) {
            if (comp == null || comp.errorCount() == 0 || options == null || options.isSet("dev")) bugMessage(ex);
            return EXIT_ABNORMAL;
        } finally {
            if (comp != null) {
                try {
                    comp.close();
                } catch (ClientCodeException ex) {
                    throw new RuntimeException(ex.getCause());
                }
            }
            filenames = null;
            options = null;
        }
        return EXIT_OK;
    }

    /** Print a message reporting an internal error.
     */
    void bugMessage(Throwable ex) {
        Log.printLines(out, getLocalizedString("msg.bug", JavaCompiler.version()));
        ex.printStackTrace(out);
    }

    /** Print a message reporting a fatal error.
     */
    void feMessage(Throwable ex) {
        Log.printLines(out, ex.getMessage());
        if (ex.getCause() != null && options.isSet("dev")) {
            ex.getCause().printStackTrace(out);
        }
    }

    /** Print a message reporting an input/output error.
     */
    void ioMessage(Throwable ex) {
        Log.printLines(out, getLocalizedString("msg.io"));
        ex.printStackTrace(out);
    }

    /** Print a message reporting an out-of-resources error.
     */
    void resourceMessage(Throwable ex) {
        Log.printLines(out, getLocalizedString("msg.resource"));
        ex.printStackTrace(out);
    }

    /** Print a message reporting an uncaught exception from an
     * annotation processor.
     */
    void apMessage(AnnotationProcessingError ex) {
        Log.printLines(out, getLocalizedString("msg.proc.annotation.uncaught.exception"));
        ex.getCause().printStackTrace(out);
    }

    /** Display the location and checksum of a class. */
    void showClass(String className) {
        out.println("javac: show class: " + className);
        URL url = getClass().getResource('/' + className.replace('.', '/') + ".class");
        if (url == null) out.println("  class not found"); else {
            out.println("  " + url);
            try {
                final String algorithm = "MD5";
                byte[] digest;
                MessageDigest md = MessageDigest.getInstance(algorithm);
                DigestInputStream in = new DigestInputStream(url.openStream(), md);
                try {
                    byte[] buf = new byte[8192];
                    int n;
                    do {
                        n = in.read(buf);
                    } while (n > 0);
                    digest = md.digest();
                } finally {
                    in.close();
                }
                StringBuilder sb = new StringBuilder();
                for (byte b : digest) sb.append(String.format("%02x", b));
                out.println("  " + algorithm + " checksum: " + sb);
            } catch (Exception e) {
                out.println("  cannot compute digest: " + e);
            }
        }
    }

    private JavaFileManager fileManager;

    /** Find a localized string in the resource bundle.
     *  @param key     The key for the localized string.
     */
    public static String getLocalizedString(String key, Object... args) {
        try {
            if (messages == null) messages = new JavacMessages(javacBundleName);
            return messages.getLocalizedString("javac." + key, args);
        } catch (MissingResourceException e) {
            throw new Error("Fatal Error: Resource for javac is missing", e);
        }
    }

    public static void useRawMessages(boolean enable) {
        if (enable) {
            messages = new JavacMessages(javacBundleName) {

                @Override
                public String getLocalizedString(String key, Object... args) {
                    return key;
                }
            };
        } else {
            messages = new JavacMessages(javacBundleName);
        }
    }

    private static final String javacBundleName = "com.sun.tools.javac.resources.javac";

    private static JavacMessages messages;
}
