package com.screenrunner.data;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;
import com.screenrunner.ui.SplashScreen;

public class SongIndex {

    private static final int MAXDEPTH = 4;

    public static String[] tokenizeString(String source) {
        source = source.replaceAll("[\\s\\-\\_\\(\\)\\[\\]\\<\\>\\&\\,\\|]+", "_");
        source = source.replaceAll("[\\W]", "");
        source = source.replaceAll("\\_+", " ");
        source = source.toLowerCase();
        String[] tokens = source.split("\\s+");
        Vector<String> toks = new Vector<String>();
        for (int i = 0; i < tokens.length; i++) {
            if (!isStopWord(tokens[i])) toks.add(tokens[i]);
        }
        return toks.toArray(new String[toks.size()]);
    }

    private static final String[] stopWords = new String[] { "the", "an", "a" };

    public static boolean isStopWord(String word) {
        if (word.length() < 2) return true;
        for (int i = 0; i < stopWords.length; i++) {
            if (stopWords[i].equalsIgnoreCase(word)) return true;
        }
        return false;
    }

    private static class SongIndexHandler implements ContentHandler {

        private SongIndex idx;

        private Stack<IndexNode> nodeStack = new Stack<IndexNode>();

        private StringBuilder chars = new StringBuilder();

        public SongIndexHandler(SongIndex index) {
            idx = index;
        }

        @Override
        public void startDocument() throws SAXException {
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
            if (localName.equals("i")) {
                idx._songHashes.clear();
                idx._roots.clear();
            } else if (localName.equals("h")) {
                String key = atts.getValue("k");
                String hash = atts.getValue("h");
                idx._songHashes.put(key, hash);
            } else if (localName.equals("ns")) {
                nodeStack.clear();
            } else if (localName.equals("n")) {
                String token = atts.getValue("t");
                IndexNode n = new IndexNode(token);
                if (nodeStack.size() > 0) {
                    nodeStack.peek().getChildren().add(n);
                } else {
                    idx._roots.add(n);
                }
                nodeStack.push(n);
            } else if (localName.equals("m")) {
                chars = new StringBuilder();
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            chars.append(ch, start, length);
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (localName.equals("m")) {
                nodeStack.peek().getMatches().add(chars.toString());
            } else if (localName.equals("n")) {
                nodeStack.pop();
            }
        }

        @Override
        public void endDocument() throws SAXException {
        }

        @Override
        public void endPrefixMapping(String prefix) throws SAXException {
        }

        @Override
        public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
        }

        @Override
        public void processingInstruction(String target, String data) throws SAXException {
        }

        @Override
        public void setDocumentLocator(Locator locator) {
        }

        @Override
        public void skippedEntity(String name) throws SAXException {
        }

        @Override
        public void startPrefixMapping(String prefix, String uri) throws SAXException {
        }
    }

    public static SongIndex fromFile(String filename) throws InvalidClassException {
        try {
            SongIndex si = new SongIndex();
            XMLReader parser = XMLReaderFactory.createXMLReader();
            parser.setContentHandler(new SongIndexHandler(si));
            parser.parse(new InputSource(new GZIPInputStream(new FileInputStream(filename))));
            return si;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Vector<IndexNode> _roots;

    private Hashtable<String, String> _songHashes;

    private boolean _modified;

    public SongIndex() {
        _roots = new Vector<IndexNode>();
        _songHashes = new Hashtable<String, String>();
        _modified = false;
    }

    public boolean isModified() {
        return _modified;
    }

    public boolean songNeedsReindexing(String hash, String songKey) {
        if (songKey == null || songKey.length() == 0) return false;
        if (hash == null) return false;
        if (!_songHashes.containsKey(songKey)) return true;
        if (!_songHashes.get(songKey).contentEquals(hash)) return true;
        return false;
    }

    private String hashSong(Song s) {
        if (s == null) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(s.getTitle().getBytes());
            digest.update(s.getAllLyrics().getBytes());
            String hash = Base64.encodeBytes(digest.digest());
            return hash;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void indexSong(Song s, String songKey) {
        indexSong(s, songKey, null);
    }

    public void indexSong(Song s, String songKey, SplashScreen splash) {
        String hash = hashSong(s);
        if (songNeedsReindexing(hash, songKey)) {
            if (splash != null) splash.setStatusText(I18n.get("ui/splash/indexing", songKey));
            if (_songHashes.containsKey(songKey)) removeSong(songKey);
            processTokens(tokenizeString(s.getTitle()), songKey);
            processTokens(tokenizeString(s.getAllLyrics()), songKey);
            _songHashes.put(songKey, hash);
            _modified = true;
        }
    }

    public void removeSong(String songKey) {
        for (int i = 0; i < _roots.size(); i++) {
            IndexNode child = _roots.get(i);
            if (child.containsKey(songKey)) {
                removeSongRecursive(child, songKey);
                _modified = true;
            }
        }
    }

    private void removeSongRecursive(IndexNode parent, String songKey) {
        for (int i = 0; i < parent.getChildren().size(); i++) {
            IndexNode child = parent.getChildren().get(i);
            if (child.containsKey(songKey)) {
                removeSongRecursive(child, songKey);
            }
        }
        parent.removeKey(songKey);
    }

    public void updateIndex(Hashtable<String, Song> songList) {
        Vector<String> toRemove = new Vector<String>();
        Vector<String> toAdd = new Vector<String>();
        Enumeration<String> indexKeys = _songHashes.keys();
        while (indexKeys.hasMoreElements()) {
            String key = indexKeys.nextElement();
            if (!songList.containsKey(key)) {
                toRemove.add(key);
            } else {
                String hash = hashSong(songList.get(key));
                if (!_songHashes.get(key).contentEquals(hash)) {
                    toAdd.add(key);
                }
            }
        }
        indexKeys = songList.keys();
        while (indexKeys.hasMoreElements()) {
            String key = indexKeys.nextElement();
            if (!_songHashes.contains(key)) {
                toAdd.add(key);
            }
        }
        for (int i = 0; i < toRemove.size(); i++) {
            removeSong(toRemove.get(i));
        }
        for (int i = 0; i < toAdd.size(); i++) {
            indexSong(songList.get(toAdd.get(i)), toAdd.get(i), null);
        }
    }

    public int getWordCount() {
        return _roots.size();
    }

    public String[] find(String search) {
        Hashtable<String, SearchResult> results = new Hashtable<String, SearchResult>();
        String[] tokens = tokenizeString(search);
        if (tokens == null || tokens.length == 0) return null;
        for (int i = 0; i < tokens.length + MAXDEPTH; i++) {
            findTokenList(results, _roots, tokens, Math.max(0, i - MAXDEPTH), Math.min(i, tokens.length - 1), 1);
        }
        Enumeration<String> keys = results.keys();
        SearchResult[] tosort = new SearchResult[results.size()];
        int i = 0;
        while (keys.hasMoreElements()) {
            String song = keys.nextElement();
            tosort[i++] = results.get(song);
        }
        Arrays.sort(tosort);
        String[] rv = new String[tosort.length];
        int last = tosort.length - 1;
        for (i = 0; i < tosort.length; i++) {
            rv[i] = tosort[last - i].getKey();
        }
        return rv;
    }

    private boolean findTokenList(Hashtable<String, SearchResult> results, Vector<IndexNode> parent, String[] tokens, int first, int last, int score) {
        IndexNode inode = findChild(parent, tokens[first], false, last == (tokens.length - 1));
        if (inode != null) {
            if (first < last) {
                boolean rv = findTokenList(results, inode.getChildren(), tokens, first + 1, last, score * 10);
                if (rv == false) {
                    updateResults(results, inode.getMatches(), score);
                    return true;
                } else {
                    return true;
                }
            } else {
                updateResults(results, inode.getMatches(), score);
                return true;
            }
        } else {
            return false;
        }
    }

    private void updateResults(Hashtable<String, SearchResult> results, Vector<String> songs, int score) {
        for (int i = 0; i < songs.size(); i++) {
            if (!results.containsKey(songs.get(i))) {
                results.put(songs.get(i), new SearchResult(songs.get(i), score));
            } else {
                results.get(songs.get(i)).incrementScore(score);
            }
        }
    }

    private void processTokens(String[] tokens, String songKey) {
        for (int t = 0; t < tokens.length + MAXDEPTH; t++) {
            addRecursive(_roots, tokens, Math.max(0, t - MAXDEPTH), Math.min(t, tokens.length - 1), songKey);
        }
    }

    private void addRecursive(Vector<IndexNode> parent, String[] tokens, int first, int last, String songKey) {
        if (tokens.length == 0) return;
        IndexNode node = addToken(parent, tokens[first], songKey);
        if (first < last) {
            addRecursive(node.getChildren(), tokens, first + 1, last, songKey);
        }
    }

    private IndexNode findChild(Vector<IndexNode> parent, String token, boolean create, boolean partial) {
        IndexNode partialMatch = null;
        for (int i = 0; i < parent.size(); i++) {
            if (parent.get(i).getToken().contentEquals(token)) {
                return parent.get(i);
            } else if (partial && partialMatch == null) {
                if (parent.get(i).getToken().startsWith(token)) {
                    partialMatch = parent.get(i);
                }
            }
        }
        if (create) {
            IndexNode inode = new IndexNode(token);
            parent.add(inode);
            return inode;
        } else {
            if (partial) {
                return partialMatch;
            } else {
                return null;
            }
        }
    }

    private IndexNode addToken(Vector<IndexNode> list, String token, String songKey) {
        IndexNode inode = findChild(list, token, true, false);
        for (int i = 0; i < inode.getMatches().size(); i++) {
            if (inode.getMatches().get(i).contentEquals(songKey)) return inode;
        }
        inode.getMatches().add(songKey);
        return inode;
    }

    private void serialize(Writer writer) {
        try {
            writer.write("<?xml version=\"1.0\" ?>\n");
            writer.write("<i>\n");
            writer.write("<hs>\n");
            Enumeration<String> keys = _songHashes.keys();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement();
                writer.write("<h k=\"" + key + "\" h=\"" + _songHashes.get(key) + "\"/>\n");
            }
            writer.write("</hs>\n");
            writer.write("<ns>\n");
            for (int i = 0; i < _roots.size(); i++) {
                serializeNode(writer, _roots.get(i), 0);
            }
            writer.write("</ns>\n");
            writer.write("</i>");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void serializeNode(Writer writer, IndexNode node, int depth) throws IOException {
        String t = "";
        writer.write(t + "<n t=\"" + node.getToken() + "\">\n");
        writer.write(t + "<ms>");
        for (int i = 0; i < node.getMatches().size(); i++) {
            if (node.getMatches().get(i).trim().length() > 0) writer.write("<m>" + node.getMatches().get(i) + "</m>");
        }
        writer.write(t + "</ms>\n");
        writer.write(t + "<cn>\n");
        for (int i = 0; i < node.getChildren().size(); i++) {
            serializeNode(writer, node.getChildren().get(i), 0);
        }
        writer.write(t + "</cn>\n");
        writer.write(t + "</n>\n");
    }

    public void saveAs(String filename) {
        try {
            GZIPOutputStream gzos = new GZIPOutputStream(new FileOutputStream(filename));
            OutputStreamWriter writer = new OutputStreamWriter(gzos);
            serialize(writer);
            writer.close();
            _modified = false;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
