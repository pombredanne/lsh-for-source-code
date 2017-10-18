package edu.ucla.sspace.text;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * A class for preprocessing all types of documents.  This approach was used by
 * Rohde et al. (2004) for processing USENET articles.
 *
 */
public class DocumentPreprocessor {

    private final Set<DocHash> processedDocs;

    private final Set<String> validWords;

    /**
     * Constructs a {@code DocumentPreprocessor} with an empty word list 
     */
    public DocumentPreprocessor() {
        processedDocs = Collections.synchronizedSet(new HashSet<DocHash>());
        validWords = new HashSet<String>();
    }

    /**
     * Constructs a {@code DocumentPreprocessor} where the provided file 
     * contains the list all valid words for the output documents.
     *
     * @param wordList a file containing a list of all valid words for
     *        outputting
     */
    public DocumentPreprocessor(File wordList) throws IOException {
        processedDocs = Collections.synchronizedSet(new HashSet<DocHash>());
        validWords = new HashSet<String>();
        WordIterator it = new WordIterator(new BufferedReader(new FileReader(wordList)));
        while (it.hasNext()) {
            validWords.add(it.next());
        }
        addKeyTokens();
    }

    /**
     * A Constructor purely for test purposes.  Pass in an array of words which
     * will serve the roll as a word list.
     *
     */
    public DocumentPreprocessor(String[] wordList) {
        processedDocs = Collections.synchronizedSet(new HashSet<DocHash>());
        validWords = new HashSet<String>();
        for (String word : wordList) {
            validWords.add(word);
        }
        addKeyTokens();
    }

    private void addKeyTokens() {
        String keyTokens[] = { "'", "!", ".", "?", ",", ";", "(", ")", "[", "]", "/", ":", "\"", "&", "<", ">", "<num", "<url>", "<emote>", "<slash>", "dollars" };
        for (String keyToken : keyTokens) validWords.add(keyToken);
    }

    /**
     * Processes the provided document and returns the cleaned version of the
     * document.
     *
     * @param document a document to process
     *
     * @return a cleaned version of the document
     */
    public String process(String document) {
        return process(document, false);
    }

    /**
     * Processes the provided document and returns the cleaned version of the
     * document.
     *
     * @param document a document to process
     *
     * @param removeWords If true, any word which is not found in the provided
     * word list is removed from the cleaned document.
     *
     * @return a cleaned version of the document
     */
    public String process(String document, boolean removeWords) {
        document = StringUtils.unescapeHTML(document);
        document = document.replaceAll("<.*?>", "");
        document = document.replaceAll("<", " < ");
        document = document.replaceAll(">", " > ");
        StringTokenizer st = new StringTokenizer(document);
        StringBuilder urlized = new StringBuilder(document.length());
        while (st.hasMoreTokens()) {
            String tok = st.nextToken();
            if (tok.endsWith("?")) {
                urlized.append(tok.substring(0, tok.length() - 1)).append(" ?");
            } else if (tok.endsWith(",")) {
                urlized.append(tok.substring(0, tok.length() - 1)).append(" ,");
            } else if (tok.endsWith(".")) {
                urlized.append(tok.substring(0, tok.length() - 1)).append(" .");
            } else if (tok.contains("@") && tok.contains(".")) {
                urlized.append("<URL>");
            } else if (tok.startsWith("http") || tok.startsWith("ftp")) {
                urlized.append("<URL>");
            } else if (tok.matches("[0-9]+")) {
                urlized.append("<NUM>");
            } else if (tok.equals("/")) {
                urlized.append("<slash>");
            } else if ((tok.length() == 2 || tok.length() == 3) && (tok.equals(":)") || tok.equals(":(") || tok.equals(":/") || tok.equals(":\\") || tok.equals(":|") || tok.equals(":[") || tok.equals(":]") || tok.equals(":X") || tok.equals(":|") || tok.equals(":[") || tok.equals(":]") || tok.equals(":X") || tok.equals(":D"))) {
                urlized.append("<EMOTE>");
            } else {
                urlized.append(tok);
            }
            urlized.append(" ");
        }
        document = urlized.toString().trim();
        document = document.replaceAll("'", " ' ");
        document = document.replaceAll("!", " ! ");
        document = document.replaceAll("\\.", " . ");
        document = document.replaceAll("\\?", " ? ");
        document = document.replaceAll(";", " ; ");
        document = document.replaceAll(",", " , ");
        document = document.replaceAll("\\(", " ( ");
        document = document.replaceAll("\\)", " ) ");
        document = document.replaceAll("\\[", " [ ");
        document = document.replaceAll("\\]", " ] ");
        document = document.replaceAll("/", " / ");
        document = document.replaceAll(":", " : ");
        document = document.replaceAll("\"", " \" ");
        document = document.replaceAll("-", " - ");
        document = document.replaceAll("=", " = ");
        st = new StringTokenizer(document);
        StringBuilder shortWords = new StringBuilder(document.length());
        while (st.hasMoreTokens()) {
            String tok = st.nextToken();
            if (tok.length() <= 20) {
                shortWords.append(tok).append(" ");
            }
        }
        document = shortWords.toString().trim();
        document = document.toLowerCase();
        st = new StringTokenizer(document);
        StringBuilder dollarized = new StringBuilder(document.length());
        while (st.hasMoreTokens()) {
            String tok = st.nextToken();
            if (tok.startsWith("$")) {
                String s = tok.substring(1);
                if (s.matches("[0-9]+")) dollarized.append("<num>").append(" dollars ");
            } else {
                dollarized.append(tok).append(" ");
            }
        }
        document = dollarized.toString().trim();
        document = document.replaceAll("[^\\w\\s;:\\(\\)\\[\\]'!/&?\",\\.<>]", "");
        if (validWords.size() > 0) {
            int totalTokens = 0;
            int actualWords = 0;
            st = new StringTokenizer(document);
            StringBuilder cleanedDoc = new StringBuilder(document.length());
            while (st.hasMoreTokens()) {
                String tok = st.nextToken();
                totalTokens++;
                if (validWords.contains(tok)) {
                    actualWords++;
                    if (removeWords) cleanedDoc.append(tok).append(" ");
                }
            }
            if (actualWords / (double) (totalTokens) < .4) {
                return "";
            }
            if (removeWords) document = cleanedDoc.toString();
        }
        return document;
    }

    /**
     * A class that represents the hashed contents of a document.  This class
     * allows long documents to be quickly compared.  This class uses the MD5
     * algorithm to compute the hash.
     */
    private static class DocHash {

        private final byte[] hash;

        private final int hashCode;

        public DocHash(String article) {
            hash = hash(article);
            hashCode = hash[3] << 24 | hash[2] << 16 | hash[1] << 8 | hash[0];
        }

        public boolean equals(Object o) {
            return o != null && o instanceof DocHash && Arrays.equals(hash, ((DocHash) o).hash);
        }

        private static byte[] hash(String article) {
            try {
                MessageDigest md5 = MessageDigest.getInstance("MD5");
                return md5.digest(article.getBytes());
            } catch (NoSuchAlgorithmException nsae) {
                throw new Error(nsae);
            }
        }

        public int hashCode() {
            return hashCode;
        }
    }
}
