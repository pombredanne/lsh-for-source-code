package bioevent.core;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import bioevent.db.Convertor;
import bioevent.db.DBLayer;
import weka.core.Stopwords;
import dragon.nlp.tool.PorterStemmer;
import dragon.nlp.tool.lemmatiser.EngLemmatiser;
import edu.umass.cs.mallet.base.pipe.iterator.FileIterator;

public class Util {

    public static StanfordParser parser;

    /**
	 * Create new file if not exists
	 * @param path
	 * @return true if new file created
	 */
    public static boolean createFileIfNotExists(String path) {
        boolean result = false;
        File modelFile = new File(path);
        if (!modelFile.exists()) {
            try {
                result = modelFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public static void generateParseFilesIfnotExist(String file_path) {
        List<String> textRaw = null;
        String tokenizationFile = file_path.replace(".txt", ".tok");
        if (Util.fileExists(tokenizationFile)) textRaw = Util.loadLineByLine(tokenizationFile); else textRaw = Util.loadLineByLine(file_path);
        String ptb_file_path = file_path.replace(".txt", ".ptb");
        String sd_file_path = file_path.replace(".txt", ".sd");
        String pos_file_path = file_path.replace(".txt", ".pos");
        if (Util.fileExists(ptb_file_path) && Util.fileExists(sd_file_path) && Util.fileExists(pos_file_path)) return;
        ArrayList<String> parseTrees = new ArrayList<String>();
        ArrayList<String> dependencies = new ArrayList<String>();
        ArrayList<String> tagged_sentences = new ArrayList<String>();
        for (int i = 0; i < textRaw.size(); i++) {
            String line = textRaw.get(i);
            if (!Util.fileExists(ptb_file_path) || !Util.fileExists(sd_file_path)) {
                if (Util.parser == null) Util.parser = new StanfordParser();
                Util.parser.parse(line);
                parseTrees.add(Util.parser.getPenn());
                dependencies.add(Util.parser.getDependencies());
            }
            if (!Util.fileExists(pos_file_path)) tagged_sentences.add(Util.parser.getTagged(line));
        }
        Util.createFileIfNotExists(ptb_file_path, parseTrees);
        Util.createFileIfNotExists(sd_file_path, dependencies);
        Util.createFileIfNotExists(pos_file_path, tagged_sentences);
    }

    public static void createFileIfNotExists(String path, List<String> contentLines) {
        File file = new File(path);
        if (!file.exists()) {
            try {
                file.createNewFile();
                BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                for (int i = 0; i < contentLines.size(); i++) {
                    String line = contentLines.get(i);
                    writer.write(line + "\n");
                }
                writer.flush();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
	 * 
	 * @param path
	 * @return true if created
	 */
    public static boolean createFolderIfNotExists(String path) {
        boolean result = false;
        File modelFolder = new File(path);
        if (!modelFolder.exists() || !modelFolder.isDirectory()) {
            result = modelFolder.mkdir();
        }
        return result;
    }

    /**
	 * 
	 * @param inputString
	 * @return MD5 hash of given string
	 * @throws UnsupportedEncodingException
	 * @throws NoSuchAlgorithmException
	 */
    public static String getStringDigest(String inputString) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(inputString.getBytes(), 0, inputString.length());
        return new BigInteger(1, md.digest()).toString(16);
    }

    /**
	 * print message if condition is not correct
	 * @param condition
	 * @param message
	 */
    public static void assertion(boolean condition, String message) {
        if (!condition) {
            Util.log("Assertion Failed: " + message, 3);
        }
    }

    /**
	 * 
	 * @param x
	 * @return
	 */
    public static double sigmoid(double x) {
        return ((1 / (1 + Math.pow(Math.E, (-1 * x)))) - 0.5) * 2;
    }

    /**
	 * Run synchronous shell command and wait till it finishes 
	 * @param command
	 */
    public static void runShellCommand(String command) {
        try {
            Util.log(command, 2);
            Runtime rt = Runtime.getRuntime();
            Process pr = rt.exec(command);
            BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            String line = null;
            while ((line = input.readLine()) != null) {
                Util.log(line, 2);
            }
            int exitVal = pr.waitFor();
            Util.log("Exited with error code " + exitVal, 2);
        } catch (Exception e) {
            System.out.println(e.toString());
            e.printStackTrace();
        }
    }

    /**
	 * Read whole file content into a string
	 * @param filePath
	 * @return The File content
	 * @throws IOException
	 */
    public static String readWholeFile(String filePath) throws IOException {
        StringBuffer fileData = new StringBuffer(1000);
        FileReader fr = new FileReader(filePath);
        BufferedReader reader = new BufferedReader(fr);
        char[] buf = new char[1024];
        int numRead = 0;
        while ((numRead = reader.read(buf)) != -1) {
            String readData = String.valueOf(buf, 0, numRead);
            fileData.append(readData);
            buf = new char[1024];
        }
        reader.close();
        fr.close();
        return fileData.toString();
    }

    /**
	 * Parse the given XML file
	 * @param is
	 * @return Document object of parsed file
	 */
    public static Document parseXML(InputStream is) {
        Document ret = null;
        DocumentBuilderFactory domFactory;
        DocumentBuilder builder;
        try {
            domFactory = DocumentBuilderFactory.newInstance();
            domFactory.setValidating(false);
            domFactory.setNamespaceAware(false);
            builder = domFactory.newDocumentBuilder();
            ret = builder.parse(is);
        } catch (Exception ex) {
            Util.log("unable to load XML: " + ex, 3);
        }
        return ret;
    }

    /**
	 * Customized definition of stop words for Artifact
	 * @param wordArtifact
	 * @return
	 */
    public static boolean isStopWord(Artifact wordArtifact) {
        boolean isStopWord = false;
        if (wordArtifact.getArtifactType() != Artifact.Type.Word) return isStopWord;
        if (wordArtifact.getTextContent().length() < 2 || Stopwords.isStopword(wordArtifact.getTextContent())) isStopWord = true;
        return isStopWord;
    }

    /**
	 * Customized definition of stop words for word
	 * @param word
	 * @return
	 */
    public static boolean isStopWord(String word) {
        boolean isStopWord = false;
        if (word.length() < 2 || Stopwords.isStopword(word) || word.matches("\\W+")) isStopWord = true;
        return isStopWord;
    }

    /**
	 * Porter stem
	 * @param word
	 * @return stemmed word
	 */
    public static String getWordPorterStem(String word) {
        PorterStemmer stemmer = new PorterStemmer();
        String stemmed_word = stemmer.stem(word).toLowerCase();
        return stemmed_word;
    }

    /**
	 * Split by non-chars and return longest part
	 * @param value
	 * @return Cleaned value
	 */
    public static String cleanWordValue(String value) {
        String cleaned_value = value.replaceAll("\\d", "0").replaceAll("^\\-", "").replaceAll("\\-$", "").toLowerCase();
        return cleaned_value;
    }

    public static FileIterator getFilesInDirectory(String root) {
        ExtensionFilter filter = new ExtensionFilter(".txt");
        FileIterator file_iterator = new FileIterator(new File(root), filter, FileIterator.LAST_DIRECTORY);
        return file_iterator;
    }

    public static String getTermByTermPorter(String phrase) {
        String[] words = phrase.split(" ");
        String rootString = "";
        for (int i = 0; i < words.length; i++) {
            rootString += Util.getWordPorterStem(words[i]) + " ";
        }
        return rootString.trim();
    }

    static EngLemmatiser lemmatiser = new EngLemmatiser("nlpdata/lemmatiser", true, false);

    /**
	 * Get porter stem of each word in phrase
	 * @param phrase
	 * @return phrase each word converted to porter stem
	 */
    public static String getTermByTermWordnet(String phrase) {
        String[] words = phrase.split(" ");
        String rootString = "";
        for (int i = 0; i < words.length; i++) {
            if (!Util.isStopWord(words[i])) {
                rootString += lemmatiser.stem(words[i]) + " ";
            }
        }
        return rootString.trim();
    }

    static String getDateTime() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        return dateFormat.format(date);
    }

    static boolean mute = false;

    /**
    * print message to log file or to standard output
    * priority will be used to control verbosity: 
    * 	Priority=3 : for very critical messages and errors
    * 	Priority=2 : for progress reporting messages
    * 	Priority=1 : for very low level messages used for debugging or very detail information
    */
    public static void log(String message, int priority) {
        String message_with_time = getDateTime() + " " + message;
        if (priority < 3) {
            if (!mute) System.out.println(message_with_time);
        } else System.err.println(message_with_time);
    }

    public static DBLayer db = new DBLayer();

    public static String prepareSQLString(String sqlString) {
        sqlString = sqlString.replace("\\", "\\\\").replace("'", "''").replace("%", "\\%").replace("_", "\\_");
        return sqlString;
    }

    public static List<String> loadLineByLine(String file_path) {
        List<String> lines = new ArrayList<String>();
        try {
            File f = new File(file_path);
            if (!f.exists()) return lines;
            BufferedReader br1 = new BufferedReader(new FileReader(f));
            while (br1.ready()) {
                String line = br1.readLine();
                lines.add(line);
            }
            br1.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lines;
    }

    public static boolean fileExists(String path) {
        File modelFile = new File(path);
        return modelFile.exists();
    }

    public static void logUpdate(String message) {
        System.out.print("\r" + message);
    }

    public static String calculateQueryTrainConditions() {
        String trainQueryConditions = "for_train=1";
        if (!Configuration.ReleaseMode) trainQueryConditions = "for_train=1 AND not file LIKE '%devel%'";
        return trainQueryConditions;
    }

    public static String getModelFilePath(int trigger_type_id, String trainFile) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        String trainQueryConditions = calculateQueryTrainConditions();
        if (trainFile == null) trainFile = Convertor.getExampleFilePath(trainQueryConditions, true, true, trigger_type_id);
        String[] fileparts = trainFile.split("/");
        String curTypemodelFile = Configuration.getValue("ModelFolder") + fileparts[fileparts.length - 1].replace(".svnlight", ".model");
        return curTypemodelFile;
    }

    public static String castForRegex(String textContent) {
        return textContent.replace("\\", "\\\\").replace("/", "\\/").replace("*", "\\*").replace("+", "\\+").replace(".", "\\.").replace("?", "\\?").replace(")", "\\)").replace("{", "\\{").replace("}", "\\}").replace("(", "\\(").replace("[", "\\[").replace("]", "\\]").replace("%", "\\%");
    }

    public static String decastRegex(String textContent) {
        return textContent.replace("\\\\", "\\").replace("\\/", "/").replace("\\*", "*").replace("\\+", "+").replace("\\.", ".").replace("\\?", "?").replace("\\)", ")").replace("\\_", "_").replace("\\{", "{").replace("\\}", "}").replace("\\(", "(").replace("\\[", "[").replace("\\]", "]").replace("\\%", "%");
    }

    public static boolean compressedEqual(String textContent, String textContent2) {
        textContent = decastCharTags(decastRegex(textContent.replace(" ", "").replace(" ", "").replaceAll("\\.\\.+", "..")).replace("[", "(").replace("]", ")"));
        textContent2 = decastCharTags(decastRegex(textContent2.replace(" ", "").replace(" ", "").replaceAll("\\.\\.+", "..")).replace("[", "(").replace("]", ")"));
        if (textContent.equalsIgnoreCase(textContent2)) return true;
        return false;
    }

    public static String decastCharTags(String str) {
        return str.replace("&apos;", "'").replace("&quot;", "\"").replace("&amp;", "&").replace("&lt;", "(").replace("&gt;", ")");
    }

    public static String compress(String text) {
        return text.replace(" ", "").replace(" ", "");
    }

    public static void mute() {
        mute = true;
    }
}
