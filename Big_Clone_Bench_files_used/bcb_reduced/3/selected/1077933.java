package util.string;

import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import util.io.FileInput;

/**
 * 
 * @author Sergio
 *
 */
public class StringAnalysis {

    static final String englishStopWordsPath = "/home/sergio/Dropbox/data/english_stop_words.txt";

    /**
	 * Hashset containing the English stop words given in the englishStopWordsPath file
	 */
    static final HashSet<String> englishStopWords = initStopWords(englishStopWordsPath);

    public static String hashStringMD5(String string) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(string.getBytes());
        byte byteData[] = md.digest();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < byteData.length; i++) {
            sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
        }
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < byteData.length; i++) {
            String hex = Integer.toHexString(0xff & byteData[i]);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public static boolean isDate(String s) {
        return false;
    }

    public static boolean isNumber(String s) {
        if (s.matches(".*[a-zA-Z]+.*")) return false;
        return true;
    }

    public static boolean areOnlyCharacters(String s) {
        if (s.matches(".*[^\\w]+.*")) return false;
        if (s.matches(".*[0-9]+.*")) return false;
        return true;
    }

    public static boolean isStopWord(String a) {
        if (englishStopWords.contains(a.trim().toLowerCase())) return true;
        return false;
    }

    /**
	 * 
	 * 
	 * @param path
	 * @return
	 */
    public static HashSet<String> initStopWords(String path) {
        HashSet<String> set = new HashSet<String>();
        FileInput in = new FileInput(path);
        String line = in.readString();
        while (line != null) {
            set.add(line.trim());
            line = in.readString();
        }
        return set;
    }

    /**
	 * Normalize an entry of the AOL query log
	 * 
	 * The normalization is carried out by:
	 * 	Deleting stop words
	 *  Deleting www., http:\\,  .com, .net, .nl, file extensions, etc
	 *  Deleting whiteSpaces (only single whitespaces are kept)
	 *  
	 * 
	 * @param query
	 * @return
	 */
    public static String queryEntryNormalization(String query) {
        String n = query;
        n = n.replaceAll("(\\.[a-z]{2,3})+$", "");
        n = n.replaceAll("\\.[a-z]{2,3}\\s+", " ");
        n = n.replaceAll("www\\.|http:\\\\|http://|http://www\\.|ww\\.", "");
        n = n.replaceAll("\\.|,|:|;|'|-", " ");
        n = n.replaceAll("\\s+", " ");
        String query_words[] = n.split("\\s+");
        String new_query = "";
        for (int i = 0; i < query_words.length; i++) {
            if (!englishStopWords.contains(query_words[i].trim())) {
                new_query += query_words[i] + " ";
            }
        }
        if (new_query.length() == 0) return query;
        return new_query.substring(0, new_query.length() - 1);
    }

    public static String tagNormalization(String query) {
        String n = query;
        n = n.replaceAll(",|:|;|'|-|/|_|&|\\+", "\t");
        n = n.replaceAll("\\.|,|:|;|'|-|/|_|&", "\t");
        n = n.replaceAll("kids", "\tkids\t");
        n = n.replaceAll("tutorials", "\tutorials\t");
        n = n.replaceAll("stuff", "\tstuff\t");
        n = n.replaceAll("links", "\tlinks\t");
        n = n.replaceAll("children", "\tchildren\t");
        n = n.replaceAll("elementary", "\telementary\t");
        n = n.replaceAll("machines", "\tmachines\t");
        n = n.replaceAll("machine", "\tmachine\t");
        n = n.replaceAll("practice", "\tpractice\t");
        n = n.replaceAll("maths", "\tmaths\t");
        n = n.replaceAll("teaching", "\teaching\t");
        n = n.replaceAll("science", "\tscience\t");
        n = n.replaceAll("interactive", "\tinteractive\t");
        n = n.replaceAll("student", "\tstudent\t");
        n = n.replaceAll("reading", "\treading\t");
        n = n.replaceAll("recommended", "\trecommended\t");
        n = n.replaceAll("websites", "\twebsites\t");
        n = n.replaceAll("science", "\tscience\t");
        n = n.replaceAll("language", "\tlanguage\t");
        n = n.replaceAll("resources", "\tresources\t");
        n = n.replaceAll("multimedia", "\tmultimedia\t");
        n = n.replaceAll("information", "\tinformation\t");
        if (n.contains("homeschool")) {
            n = n.replaceAll("homeschool", "\thomeschool\t");
        } else if (n.contains("preschool")) {
            n = n.replaceAll("school", "\tschool\t");
        } else if (n.contains("school")) {
            n = n.replaceAll("school", "\tschool\t");
        }
        n = n.replaceAll("internet", "\tinternet\t");
        n = n.replaceAll("fractions", "\tfractions\t");
        n = n.replaceAll("english", "\tenglish\t");
        n = n.replaceAll("english", "\tenglish\t");
        n = n.replaceAll("printables", "\tprintables\t");
        if (n.contains("educational")) {
            n = n.replaceAll("educational", "\teducational\t");
        } else if (n.contains("education")) {
            n = n.replaceAll("education", "\teducation\t");
        }
        if (n.contains("game")) {
            n = n.replaceAll("game", "\tgame\t");
        } else if (n.contains("games")) {
            n = n.replaceAll("games", "\tgames\t");
        }
        n = n.replaceAll("for", "\tfor\t");
        n = n.replaceAll("\\s+", "\t");
        n = n.replaceAll("\\s+", "\t");
        if (n.startsWith("\t")) {
            n = n.replaceFirst("\t", "");
        }
        return n;
    }

    private static int Minimum(int a, int b, int c) {
        int mi;
        mi = a;
        if (b < mi) {
            mi = b;
        }
        if (c < mi) {
            mi = c;
        }
        return mi;
    }

    public static int getEditDistance(String s, String t) {
        int d[][];
        int n;
        int m;
        int i;
        int j;
        char s_i;
        char t_j;
        int cost;
        n = s.length();
        m = t.length();
        if (n == 0) {
            return m;
        }
        if (m == 0) {
            return n;
        }
        d = new int[n + 1][m + 1];
        for (i = 0; i <= n; i++) {
            d[i][0] = i;
        }
        for (j = 0; j <= m; j++) {
            d[0][j] = j;
        }
        for (i = 1; i <= n; i++) {
            s_i = s.charAt(i - 1);
            for (j = 1; j <= m; j++) {
                t_j = t.charAt(j - 1);
                if (s_i == t_j) {
                    cost = 0;
                } else {
                    cost = 1;
                }
                d[i][j] = Minimum(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + cost);
            }
        }
        return d[n][m];
    }

    public static int getWordEditDistance(String s, String t) {
        int d[][];
        int n;
        int m;
        int i;
        int j;
        String s_i;
        String t_j;
        int cost;
        String s_word[] = s.trim().split("\\s+");
        String t_word[] = t.trim().split("\\s+");
        n = s_word.length;
        m = t_word.length;
        if (n == 0) {
            return m;
        }
        if (m == 0) {
            return n;
        }
        d = new int[n + 1][m + 1];
        for (i = 0; i <= n; i++) {
            d[i][0] = i;
        }
        for (j = 0; j <= m; j++) {
            d[0][j] = j;
        }
        for (i = 1; i <= n; i++) {
            s_i = s_word[i - 1];
            for (j = 1; j <= m; j++) {
                t_j = t_word[j - 1];
                if (s_i.equals(t_j)) {
                    cost = 0;
                } else {
                    cost = 1;
                }
                d[i][j] = Minimum(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + cost);
            }
        }
        return d[n][m];
    }

    public static String hashSetToString(HashSet<String> set) {
        Iterator<String> iter = set.iterator();
        String types_concat = iter.next();
        while (iter.hasNext()) {
            types_concat += "\t" + iter.next();
        }
        return types_concat;
    }

    public static String LinkedListToString(LinkedList<String> set) {
        if (set == null) return null;
        if (set.size() == 0) return "";
        String types_concat = set.get(0);
        for (int i = 1; i < set.size(); i++) {
            types_concat += "\t" + set.get(i);
        }
        return types_concat;
    }

    public static void queryLength(String path) {
        FileInput in = new FileInput(path);
        String line = in.readString();
        while (line != null) {
            String t[] = line.trim().split("\\s+");
            System.out.println(t.length);
            line = in.readString();
        }
    }

    public static void getRandomSample(String path) {
        FileInput in = new FileInput(path);
        String line = in.readString();
        int count = 0;
        while (line != null) {
            double i = Math.random();
            if (i > 0.95) {
                System.out.println(line);
                count++;
            }
            line = in.readString();
        }
    }

    public static void main(String[] args) throws NoSuchAlgorithmException {
        String a = "/home/sergio/projects/CODFC/queryAnalysis/freq/financial_unique_queries.txt";
        String b = "ww.someth-ing.cza.nl";
        a = "/home/sergio/Documents/borreme.txt";
        a = "The_Maltese_Falcon_%28novel%29";
        System.out.println(URLDecoder.decode(a));
        System.out.println(areOnlyCharacters("gdd`ay"));
    }
}
