import java.util.*;
import java.util.zip.*;
import java.io.*;
import java.awt.*;
import java.text.*;
import java.awt.event.*;
import java.lang.Math.*;
import java.lang.reflect.*;
import javax.swing.*;
import java.util.*;

class CommandThread extends Thread {

    protected CriticalSection m_messageBoardCritSection;

    protected CriticalSection m_saveCriticalSection;

    public CommandThread(TransFrame tf) {
        if (core != null) return;
        core = this;
        setName("Command thread");
        m_transFrame = tf;
        m_config = new ProjectProperties();
        m_strEntryHash = new HashMap(4096);
        m_strEntryList = new ArrayList();
        m_glosEntryHash = new HashMap(2048);
        m_glosEntryList = new ArrayList();
        m_srcTextEntryArray = new ArrayList(4096);
        m_tmList = new ArrayList();
        m_orphanedList = new ArrayList();
        m_indexHash = new HashMap(8192);
        m_modifiedFlag = false;
        m_extensionList = new ArrayList(32);
        m_extensionMapList = new ArrayList(32);
        m_requestQueue = new LinkedList();
        m_projWin = null;
        m_saveCount = -1;
        m_saveThread = null;
        m_messageBoardCritSection = new CriticalSection("CommandThread::MessageBoard");
        m_saveCriticalSection = new CriticalSection("CommandThread::Save");
        HTMLParser.initEscCharLookupTable();
        m_prefManager = PreferenceManager.pref;
        if (m_prefManager == null) m_prefManager = new PreferenceManager(OConsts.PROJ_PREFERENCE);
    }

    public void run() {
        RequestPacket pack = new RequestPacket();
        m_saveThread = new SaveThread();
        try {
            while (m_stop == false) {
                try {
                    sleep(40);
                } catch (InterruptedException e) {
                    ;
                }
                pack.reset();
                messageBoardCheck(pack);
                switch(pack.type) {
                    case RequestPacket.NO_OP:
                        break;
                    case RequestPacket.LOAD:
                        requestLoad(pack);
                        break;
                    case RequestPacket.SAVE:
                        save();
                        break;
                }
            }
            m_prefManager.save();
            m_saveThread.signalStop();
            m_saveThread.interrupt();
            core = null;
        } catch (RuntimeException re) {
            forceSave(true);
            String msg = OStrings.CT_FATAL_ERROR;
            m_transFrame.fatalError(msg, re);
        }
    }

    public void messageBoardPost(RequestPacket pack) {
        messageBoard(true, pack);
    }

    public void messageBoardCheck(RequestPacket pack) {
        messageBoard(false, pack);
    }

    protected void messageBoard(boolean post, RequestPacket pack) {
        {
            if (CommandThread.core == null) return;
            if (post == true) {
                m_requestQueue.add(pack);
                CommandThread.core.interrupt();
            } else {
                if (m_requestQueue.size() > 0) {
                    pack.set((RequestPacket) m_requestQueue.removeFirst());
                }
            }
        }
    }

    public void signalStop() {
        m_stop = true;
        CommandThread.core.interrupt();
    }

    protected void requestUnload() {
        if (m_strEntryList.size() > 0) {
            if (m_saveCount >= 0) m_saveCount = 1;
            save();
        }
        m_strEntryHash.clear();
        m_glosEntryHash.clear();
        m_indexHash.clear();
        m_tmList.clear();
        m_orphanedList.clear();
        m_extensionList.clear();
        m_extensionMapList.clear();
        m_strEntryList.clear();
        m_glosEntryList.clear();
        m_srcTextEntryArray.clear();
        if (m_projWin != null) {
            if (m_projWin.isVisible()) {
                m_projWin.hide();
                m_projWin.reset();
                m_projWin.buildDisplay();
                m_projWin.show();
            } else {
                m_projWin.reset();
                m_projWin.buildDisplay();
            }
        }
        if (m_transFrame != null) {
            MessageRelay.uiMessageUnloadProject(m_transFrame);
        }
        m_totalWords = 0;
        m_partialWords = 0;
        m_currentWords = 0;
        m_nearProj = null;
    }

    protected void requestLoad(RequestPacket pack) {
        TransFrame tf = (TransFrame) pack.obj;
        try {
            requestUnload();
            String evtStr;
            evtStr = OStrings.CT_LOADING_PROJECT;
            MessageRelay.uiMessageSetMessageText(tf, evtStr);
            if (loadProject() == false) {
                evtStr = OStrings.CT_CANCEL_LOAD;
                MessageRelay.uiMessageSetMessageText(tf, evtStr);
                return;
            }
            if (numEntries() <= 0) throw new IOException("empty project");
            tf.finishLoadProject();
            MessageRelay.uiMessageDisplayEntry(tf);
            if (m_saveCount == -1) m_saveThread.start();
            evtStr = OStrings.CT_LOADING_INDEX;
            MessageRelay.uiMessageSetMessageText(tf, evtStr);
            buildIndex();
            evtStr = OStrings.CT_LOADING_GLOSSARY;
            MessageRelay.uiMessageSetMessageText(tf, evtStr);
            buildGlossary();
            String status = OStrings.CT_FUZZY_X_OF_Y;
            buildNearList(m_strEntryList, status);
            try {
                loadTM();
            } catch (IOException e) {
                String msg = OStrings.TF_TM_LOAD_ERROR;
                displayError(msg, e);
            }
            evtStr = OStrings.CT_LOADING_WORDCOUNT;
            MessageRelay.uiMessageSetMessageText(tf, evtStr);
            buildWordCounts();
            MessageRelay.uiMessageFuzzyInfo(tf);
            MessageRelay.uiMessageSetMessageText(tf, "");
            m_saveCount = 2;
        } catch (InterruptedIOException e1) {
            ;
        } catch (IOException e) {
            String msg = OStrings.TF_LOAD_ERROR;
            displayError(msg, e);
            requestUnload();
        }
    }

    public void pseudoTranslate() {
        String str;
        int ps;
        SourceTextEntry srcTE = null;
        StringEntry se = null;
        for (int i = 0; i < m_srcTextEntryArray.size(); i++) {
            srcTE = (SourceTextEntry) m_srcTextEntryArray.get(i);
            se = srcTE.getStrEntry();
            str = srcTE.getTranslation();
            if ((str == null) || (str.equals(""))) {
                str = "omega - " + se.getSrcText();
            }
            se.setTranslation(str);
        }
    }

    protected void buildTMXFile(String filename) throws IOException {
        int i;
        String s;
        String t;
        File tm;
        DataOutputStream dos;
        StringEntry se;
        String srcLang = getPreference(OConsts.PREF_SRCLANG);
        String locLang = getPreference(OConsts.PREF_LOCLANG);
        FileOutputStream fos = new FileOutputStream(filename);
        OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF8");
        BufferedWriter out = new BufferedWriter(osw);
        String str = "<?xml version=\"1.0\"?>\n";
        str += "<!DOCTYPE tmx SYSTEM \"tmx11.dtd\">\n";
        str += "<tmx version=\"1.1\">\n";
        str += "  <header\n";
        str += "    creationtool=\"OmegaT\"\n";
        str += "    creationtoolversion=\"1\"\n";
        str += "    segtype=\"paragraph\"\n";
        str += "    o-tmf=\"OmegaT TMX\"\n";
        str += "    adminlang=\"EN-US\"\n";
        str += "    srclang=\"" + srcLang + "\"\n";
        str += "    datatype=\"plaintext\"\n";
        str += "  >\n";
        str += "  </header>\n";
        str += "  <body>\n";
        out.write(str, 0, str.length());
        for (i = 0; i < m_strEntryList.size(); i++) {
            se = (StringEntry) m_strEntryList.get(i);
            s = XMLStreamReader.makeValidXML(se.getSrcText(), null);
            t = XMLStreamReader.makeValidXML(se.getTrans(), null);
            if (t.equals("")) continue;
            str = "    <tu>\n";
            str += "      <tuv lang=\"" + srcLang + "\">\n";
            str += "        <seg>" + s + "</seg>\n";
            str += "      </tuv>\n";
            str += "      <tuv lang=\"" + locLang + "\">\n";
            str += "        <seg>" + t + "</seg>\n";
            str += "      </tuv>\n";
            str += "    </tu>\n";
            out.write(str, 0, str.length());
        }
        TransMemory transMem;
        for (i = 0; i < m_orphanedList.size(); i++) {
            transMem = (TransMemory) m_orphanedList.get(i);
            s = XMLStreamReader.makeValidXML(transMem.source, null);
            t = XMLStreamReader.makeValidXML(transMem.target, null);
            if (t.equals("")) continue;
            str = "    <tu>\n";
            str += "      <tuv lang=\"" + srcLang + "\">\n";
            str += "        <seg>" + s + "</seg>\n";
            str += "      </tuv>\n";
            str += "      <tuv lang=\"" + locLang + "\">\n";
            str += "        <seg>" + t + "</seg>\n";
            str += "      </tuv>\n";
            str += "    </tu>\n";
            out.write(str, 0, str.length());
        }
        str = "  </body>\n";
        str += "</tmx>\n";
        out.write(str, 0, str.length());
        out.close();
    }

    public ArrayList validateTags() {
        int i, j;
        String s;
        String t;
        ArrayList srcTags = new ArrayList(32);
        ArrayList locTags = new ArrayList(32);
        ArrayList suspects = new ArrayList(16);
        StringEntry se;
        SourceTextEntry ste;
        for (i = 0; i < m_srcTextEntryArray.size(); i++) {
            ste = (SourceTextEntry) m_srcTextEntryArray.get(i);
            se = ste.getStrEntry();
            s = se.getSrcText();
            t = se.getTrans();
            StaticUtils.buildTagList(s, srcTags);
            StaticUtils.buildTagList(t, locTags);
            if (srcTags.size() != locTags.size()) suspects.add(ste); else {
                for (j = 0; j < srcTags.size(); j++) {
                    s = (String) srcTags.get(j);
                    t = (String) locTags.get(j);
                    if (s.equals(t) == false) {
                        suspects.add(ste);
                        break;
                    }
                }
            }
            srcTags.clear();
            locTags.clear();
        }
        return suspects;
    }

    public void compileProject() throws IOException {
        if (m_strEntryHash.size() == 0) return;
        int i;
        int j;
        String srcRoot = m_config.getSrcRoot();
        String locRoot = m_config.getLocRoot();
        boolean err = false;
        String s;
        String t;
        save();
        FileHandler fh = null;
        HandlerMaster hm = new HandlerMaster();
        ArrayList fileList = new ArrayList(256);
        boolean ignore;
        String filename;
        String destFileName;
        int namePos;
        String shortName;
        String fname = m_config.getProjRoot() + m_config.getProjName() + OConsts.TMX_EXTENSION;
        try {
            buildTMXFile(fname);
        } catch (IOException e) {
            System.out.println("ERROR - unable to build new TMX file");
            err = true;
        }
        StaticUtils.buildDirList(fileList, new File(srcRoot));
        File destFile;
        for (i = 0; i < fileList.size(); i++) {
            filename = (String) fileList.get(i);
            destFileName = m_config.getLocRoot() + filename.substring(m_config.getSrcRoot().length());
            ignore = false;
            destFile = new File(destFileName);
            if (!destFile.exists()) {
                if (destFile.mkdir() == false) {
                    throw new IOException("Can't create target language " + "directory " + destFileName);
                }
            }
        }
        fileList.clear();
        StaticUtils.buildFileList(fileList, new File(srcRoot), true);
        for (i = 0; i < fileList.size(); i++) {
            filename = (String) fileList.get(i);
            destFileName = m_config.getLocRoot() + filename.substring(m_config.getSrcRoot().length());
            ignore = false;
            namePos = filename.lastIndexOf(File.separator) + 1;
            shortName = filename.substring(namePos);
            int extPos = filename.lastIndexOf('.') + 1;
            String ext = filename.substring(extPos);
            for (j = 0; j < m_extensionList.size(); j++) {
                if (ext.equals(m_extensionList.get(j)) == true) {
                    ext = (String) m_extensionMapList.get(j);
                    break;
                }
            }
            fh = hm.findPreferredHandler(ext);
            if (fh == null) {
                System.out.println(OStrings.CT_NO_FILE_HANDLER + " (." + ext + ")");
                System.out.println(OStrings.CT_COPY_FILE + " '" + filename.substring(m_config.getSrcRoot().length()) + "'");
                LFileCopy.copy(filename, destFileName);
                continue;
            }
            if (fh.getType().equals(OConsts.FH_HTML_TYPE)) {
                if (StaticUtils.isXMLFile(filename) == true) {
                    FileHandler fhx = hm.findPreferredHandler(OConsts.FH_XML_BASED_HTML);
                    if (fhx != null) {
                        System.out.println(OStrings.CT_HTMLX_MASQUERADE + " (" + filename + ")");
                        fh = fhx;
                    }
                }
            }
            String midName = filename.substring(srcRoot.length());
            s = srcRoot + midName;
            t = locRoot + midName;
            m_transFrame.setMessageText(OStrings.CT_COMPILE_FILE_MX + midName);
            fh.write(s, t);
        }
        m_transFrame.setMessageText(OStrings.CT_COMPILE_DONE_MX);
        if (err == true) {
            throw new IOException("Can't build TM file");
        }
    }

    public void save() {
        if (m_modifiedFlag == false) return;
        forceSave(false);
    }

    public void markAsDirty() {
        m_modifiedFlag = true;
    }

    protected void forceSave(boolean corruptionDanger) {
        {
            m_prefManager.save();
            if (m_saveCount <= 0) return; else if (m_saveCount == 1) m_saveCount = 0;
            String s = m_config.getInternal() + OConsts.STATUS_EXTENSION;
            if (corruptionDanger) {
                s += OConsts.STATUS_RECOVER_EXTENSION;
            } else {
                File backup = new File(s + OConsts.BACKUP_EXTENSION);
                File orig = new File(s);
                if (orig.exists()) orig.renameTo(backup);
            }
            try {
                buildTMXFile(s);
                m_modifiedFlag = false;
            } catch (IOException e) {
                String msg = OStrings.CT_ERROR_SAVING_PROJ;
                displayError(msg, e);
                if (corruptionDanger == false) {
                    s = m_config.getInternal() + OConsts.STATUS_EXTENSION;
                    File backup = new File(s + OConsts.BACKUP_EXTENSION);
                    File orig = new File(s);
                    if (backup.exists()) backup.renameTo(orig);
                }
            }
            if ((m_modifiedFlag == false) && (corruptionDanger == false)) {
                s = m_config.getInternal() + OConsts.STATUS_EXTENSION;
                File backup = new File(s + OConsts.BACKUP_EXTENSION);
                if (backup.exists()) backup.delete();
            }
        }
    }

    public void addEntry(String srcText, String file) {
        SourceTextEntry srcTextEntry = null;
        StringEntry strEntry = null;
        srcTextEntry = new SourceTextEntry();
        strEntry = (StringEntry) m_strEntryHash.get(srcText);
        if (strEntry == null) {
            strEntry = new StringEntry(srcText);
            m_strEntryHash.put(srcText, strEntry);
            m_strEntryList.add(strEntry);
        }
        srcTextEntry.set(strEntry, m_curFile, m_srcTextEntryArray.size());
        m_srcTextEntryArray.add(srcTextEntry);
    }

    public void createProject() {
        HandlerMaster hand = new HandlerMaster();
        try {
            if (m_config.createNew() == false) return;
            File proj = new File(m_config.getProjRoot());
            if (!proj.isDirectory()) {
                if (proj.mkdirs() == false) {
                    String msg = OStrings.CT_ERROR_CREATE;
                    throw new IOException(msg);
                }
            }
            File internal = new File(m_config.getInternal());
            if (!internal.isDirectory()) {
                if (internal.mkdirs() == false) {
                    String msg = OStrings.CT_ERROR_CREATE;
                    throw new IOException(msg);
                }
            }
            File src = new File(m_config.getSrcRoot());
            if (!src.isDirectory()) {
                if (src.mkdirs() == false) {
                    String msg = OStrings.CT_ERROR_CREATE + " (.../src/)";
                    throw new IOException(msg);
                }
            }
            File glos = new File(m_config.getGlosRoot());
            if (!glos.isDirectory()) {
                if (glos.mkdirs() == false) {
                    String msg = OStrings.CT_ERROR_CREATE + " (.../glos/)";
                    throw new IOException(msg);
                }
            }
            File tm = new File(m_config.getTMRoot());
            if (!tm.isDirectory()) {
                if (tm.mkdirs() == false) {
                    String msg = OStrings.CT_ERROR_CREATE + " (.../tm/)";
                    throw new IOException(msg);
                }
            }
            File loc = new File(m_config.getLocRoot());
            if (!loc.isDirectory()) {
                if (loc.mkdirs() == false) {
                    String msg = OStrings.CT_ERROR_CREATE + " (.../target/)";
                    throw new IOException(msg);
                }
            }
            m_config.buildProjFile();
        } catch (IOException e) {
            String msg = "failed to create project";
            displayError(msg, e);
        }
    }

    public ArrayList findAllExact(String text, boolean srcLang) {
        ArrayList foundList = new ArrayList();
        String str = text.toLowerCase();
        SourceTextEntry ste;
        String segment;
        for (int i = 0; i < m_srcTextEntryArray.size(); i++) {
            ste = (SourceTextEntry) m_srcTextEntryArray.get(i);
            if (srcLang) segment = ste.getStrEntry().getSrcText(); else segment = ste.getTranslation();
            segment = segment.toLowerCase();
            if (segment.indexOf(str) >= 0) {
                foundList.add(ste);
            }
        }
        return foundList;
    }

    public TreeMap findAll(String tokens) {
        if (m_indexReady == false) return null;
        TreeMap foundList = null;
        String str = tokens.toLowerCase();
        Token tok;
        ArrayList tokenList = new ArrayList();
        StaticUtils.tokenizeText(tokens, tokenList);
        for (int i = 0; i < tokenList.size(); i++) {
            tok = (Token) tokenList.get(i);
            if (tok.text.equals("")) continue;
            if (foundList == null) foundList = find(tok.text); else foundList = refineQuery(tok.text, foundList);
            if (foundList == null) break;
        }
        return foundList;
    }

    public TreeMap find(String wrd) {
        if (m_indexReady == false) return null;
        String local = wrd.toLowerCase();
        TreeMap tree = null;
        IndexEntry index = null;
        index = (IndexEntry) m_indexHash.get(local);
        if (index != null) tree = index.getTreeMap();
        return tree;
    }

    public TreeMap refineQuery(String wrd, TreeMap foundList) {
        if (m_indexReady == false) return null;
        if (foundList == null) return null;
        TreeMap tree = find(wrd);
        if (tree == null) return null;
        TreeMap queryTree = null;
        StringEntry strEntry;
        String s;
        Object obj;
        while ((tree.size() > 0) && (foundList.size() > 0)) {
            obj = tree.firstKey();
            strEntry = (StringEntry) foundList.remove(obj);
            if (strEntry != null) {
                if (queryTree == null) queryTree = new TreeMap();
                s = String.valueOf(strEntry.digest());
                queryTree.put(s, strEntry);
            }
            tree.remove(obj);
        }
        return queryTree;
    }

    protected void loadTranslations() {
        String srcText;
        String trans;
        String s;
        StringEntry strEntry;
        SourceTextEntry srcTextEntry;
        Object obj;
        File proj;
        try {
            proj = new File(m_config.getInternal() + OConsts.STATUS_EXTENSION);
            if (proj.exists() == false) {
                System.out.println("Can't find saved translation file '" + proj + "'");
                return;
            }
        } catch (SecurityException se) {
            String msg = "Security error encountered loading " + "project file";
            displayError(msg, se);
            return;
        }
        try {
            loadTMXFile(proj.getAbsolutePath(), "UTF-8", true);
        } catch (IOException e) {
            String msg = "problem encountered loading " + "project status file";
            displayError(msg, e);
        }
    }

    protected boolean loadProject() throws IOException, InterruptedIOException {
        int i;
        int j;
        m_ignoreNearLog = false;
        if (m_config.loadExisting() == false) return false;
        File dir = new File(m_config.getGlosRoot());
        String[] fileList;
        String fname;
        String src;
        String loc;
        LTabFileReader tab = new LTabFileReader();
        if (dir.isDirectory()) {
            fileList = dir.list();
            for (i = 0; i < fileList.length; i++) {
                fname = fileList[i];
                if (fname.endsWith(".tab")) {
                    System.out.println("Loading glossary file " + fname);
                    tab.load(dir.getAbsolutePath() + File.separator + fname);
                    for (j = 0; j < tab.numRows(); j++) {
                        src = tab.get(j, 0);
                        loc = tab.get(j, 1);
                        String com = tab.get(j, 2);
                        if (m_glosEntryHash.get(src) == null) {
                            GlossaryEntry glosEntry = new GlossaryEntry(src, loc, com);
                            m_glosEntryHash.put(src, glosEntry);
                            m_glosEntryList.add(glosEntry);
                        }
                    }
                } else {
                    System.out.println(OStrings.CT_DONT_RECOGNIZE_GLOS_FILE + fname);
                }
            }
        } else {
            throw new IOException("can't access glossary directory");
        }
        StaticUtils.loadFileMappings(m_extensionList, m_extensionMapList);
        FileHandler fh = null;
        HandlerMaster hm = new HandlerMaster();
        ArrayList srcFileList = new ArrayList(256);
        File root = new File(m_config.getSrcRoot());
        StaticUtils.buildFileList(srcFileList, root, true);
        boolean ignore;
        int namePos;
        String shortName;
        String filename;
        int numEntries = 0;
        for (i = 0; i < srcFileList.size(); i++) {
            filename = (String) srcFileList.get(i);
            ignore = false;
            namePos = filename.lastIndexOf(File.separator) + 1;
            shortName = filename.substring(namePos);
            int extPos = filename.lastIndexOf('.');
            String ext = filename.substring(extPos + 1);
            for (j = 0; j < m_extensionList.size(); j++) {
                if (ext.equals(m_extensionList.get(j)) == true) {
                    ext = (String) m_extensionMapList.get(j);
                    break;
                }
            }
            fh = hm.findPreferredHandler(ext);
            if (fh == null) {
                System.out.println(OStrings.CT_NO_FILE_HANDLER + " (." + ext + ")");
                continue;
            }
            if (fh.getType().equals(OConsts.FH_HTML_TYPE)) {
                if (StaticUtils.isXMLFile(filename) == true) {
                    FileHandler fhx = hm.findPreferredHandler(OConsts.FH_XML_BASED_HTML);
                    if (fhx != null) fh = fhx;
                }
            }
            String filepath = filename.substring(m_config.getSrcRoot().length());
            m_projWin.addFile(filepath, numEntries());
            System.out.println("loading file '" + filepath + "'");
            m_transFrame.setMessageText(OStrings.CT_LOAD_FILE_MX + filepath);
            m_curFile = new ProjectFileData();
            m_curFile.name = filename;
            m_curFile.firstEntry = m_srcTextEntryArray.size();
            fh.load(filename);
            m_curFile.lastEntry = m_srcTextEntryArray.size() - 1;
        }
        m_curFile = null;
        m_projWin.setNumEntries(numEntries());
        loadTranslations();
        m_projWin.buildDisplay();
        m_projWin.show();
        m_projWin.toFront();
        return true;
    }

    protected void buildIndex() {
        StringEntry strEntry;
        int wordCount;
        String s;
        Token tok;
        int i, j;
        ArrayList tokenList = new ArrayList();
        IndexEntry indexEntry;
        for (i = 0; i < m_strEntryList.size(); i++) {
            strEntry = (StringEntry) m_strEntryList.get(i);
            s = strEntry.getSrcText();
            wordCount = StaticUtils.tokenizeText(s, tokenList);
            for (j = 0; j < tokenList.size(); j++) {
                tok = (Token) tokenList.get(j);
                if (tok.text.equals("")) continue;
                s = tok.text.toLowerCase();
                indexEntry = (IndexEntry) m_indexHash.get(s);
                if (indexEntry == null) {
                    indexEntry = new IndexEntry(s);
                    m_indexHash.put(s, indexEntry);
                }
                indexEntry.addReference(strEntry);
            }
            strEntry.setWordCount(wordCount);
        }
    }

    protected void buildGlossary() {
        int i;
        GlossaryEntry glosEntry;
        StringEntry strEntry;
        String glosStr;
        String glosStrLow;
        String s;
        int pos;
        TreeMap foundList;
        for (i = 0; i < m_glosEntryList.size(); i++) {
            glosEntry = (GlossaryEntry) m_glosEntryList.get(i);
            glosStr = glosEntry.getSrcText();
            foundList = findAll(glosStr);
            glosStrLow = glosStr.toLowerCase();
            if (foundList == null) continue;
            Object obj;
            while (foundList.size() > 0) {
                obj = foundList.firstKey();
                strEntry = (StringEntry) foundList.remove(obj);
                if (strEntry == null) continue;
                s = strEntry.getSrcText().toLowerCase();
                pos = s.indexOf(glosStrLow);
                if (pos >= 0) {
                    strEntry.addGlosString(glosEntry);
                }
            }
        }
    }

    protected void buildNearList(ArrayList seList, String status) throws InterruptedIOException {
        String evtStr = status;
        ArrayList pairList = new ArrayList(32);
        ArrayList candPairList = new ArrayList(32);
        ArrayList wordList = new ArrayList(32);
        ArrayList candList = new ArrayList(32);
        StringFreqData masterWordFreq = new StringFreqData(64);
        StringFreqData masterPairFreq = new StringFreqData(64);
        StringFreqData wordFreq = new StringFreqData(64);
        StringFreqData pairFreq = new StringFreqData(64);
        FreqList freqList = new FreqList(256);
        StringEntry strEntry;
        StringEntry cand;
        String tok;
        String curWord = null;
        String lastWord = null;
        String wordPair;
        int i, j;
        byte[] candAttr;
        byte[] strAttr;
        double ratio;
        double wordRatio;
        double pairRatio;
        Integer len = new Integer(seList.size());
        for (i = 0; i < seList.size(); i++) {
            if (i % 10 == 0) {
                Object[] obj = { new Integer(i), len };
                MessageRelay.uiMessageSetMessageText(m_transFrame, MessageFormat.format(evtStr, obj));
                try {
                    sleep(10);
                } catch (InterruptedException ie) {
                    ;
                }
            }
            if (i == 1) {
                MessageRelay.uiMessageFuzzyInfo(m_transFrame);
            }
            if (m_stop == true) {
                throw new InterruptedIOException("");
            }
            pairList.clear();
            wordList.clear();
            masterWordFreq.reset();
            masterPairFreq.reset();
            freqList.reset();
            strEntry = (StringEntry) seList.get(i);
            buildFreqTable(freqList, strEntry, masterWordFreq, wordList, masterPairFreq, pairList);
            for (j = 0; j < freqList.len(); j++) {
                cand = (StringEntry) freqList.getObj(j);
                ratio = 1.0 * freqList.getCountN(j);
                if (cand.getWordCount() > strEntry.getWordCount()) ratio /= strEntry.getWordCount(); else ratio /= cand.getWordCount();
                if (ratio < OConsts.NEAR_THRESH) continue;
                candList.clear();
                candPairList.clear();
                wordFreq = (StringFreqData) masterWordFreq.clone();
                pairFreq = (StringFreqData) masterPairFreq.clone();
                buildFreqTable(null, cand, wordFreq, candList, pairFreq, candPairList);
                wordRatio = wordFreq.getMatchRatio();
                if (wordRatio < OConsts.NEAR_THRESH) continue;
                pairRatio = pairFreq.getMatchRatio();
                if (pairRatio < OConsts.PAIR_THRESH) continue;
                ratio = Math.sqrt(wordRatio * pairRatio);
                candAttr = buildAttrList(candList, wordFreq, pairFreq);
                strAttr = buildAttrList(wordList, wordFreq, pairFreq);
                if (seList == m_strEntryList) {
                    strEntry.addNearString(cand, ratio, strAttr, candAttr, null);
                } else {
                    cand.addNearString(strEntry, ratio, candAttr, strAttr, m_nearProj);
                }
            }
        }
    }

    protected void loadTM() throws IOException, FileNotFoundException, InterruptedIOException {
        String[] fileList;
        String lang;
        File f;
        int i;
        String fname;
        ArrayList strEntryList = new ArrayList(m_strEntryList.size());
        DataInputStream dis;
        String ext;
        String src;
        String trans;
        StringEntry se;
        String base = m_config.getProjRoot() + File.separator;
        strEntryList.clear();
        f = new File(m_config.getTMRoot());
        fileList = f.list();
        for (i = 0; i < fileList.length; i++) {
            strEntryList.clear();
            fname = fileList[i];
            ext = fname.substring(fname.lastIndexOf('.'));
            fname = m_config.getTMRoot();
            if (fname.endsWith(File.separator) == false) fname += File.separator;
            fname += fileList[i];
            m_nearProj = fileList[i];
            if (ext.equalsIgnoreCase(OConsts.TMX_EXTENSION)) loadTMXFile(fname, "UTF-8", false); else if (ext.equalsIgnoreCase(OConsts.TMW_EXTENSION)) loadTMXFile(fname, "ISO-8859-1", false);
        }
        m_nearProj = null;
    }

    protected void loadTMXFile(String fname, String encoding, boolean isProject) throws IOException, FileNotFoundException, InterruptedIOException {
        String status = OStrings.CT_TM_X_OF_Y;
        TMXReader tmx = new TMXReader(encoding);
        String src;
        String trans;
        StringEntry se;
        TransMemory tm;
        try {
            tmx.loadFile(fname);
            int num = tmx.numSegments();
            ArrayList strEntryList = new ArrayList(num + 2);
            for (int i = 0; i < num; i++) {
                src = tmx.getSourceSegment(i);
                trans = tmx.getTargetSegment(i);
                if (isProject) {
                    se = (StringEntry) m_strEntryHash.get(src);
                    if (se == null) {
                        tm = new TransMemory(src, trans, fname);
                        m_orphanedList.add(tm);
                        m_tmList.add(tm);
                        se = new StringEntry(src);
                        se.setTranslation(trans);
                        int wc = StaticUtils.tokenizeText(src, null);
                        se.setWordCount(wc);
                    }
                    se.setTranslation(trans);
                    strEntryList.add(se);
                } else {
                    m_tmList.add(new TransMemory(src, trans, fname));
                    se = new StringEntry(src);
                    se.setTranslation(trans);
                    int wc = StaticUtils.tokenizeText(src, null);
                    se.setWordCount(wc);
                    strEntryList.add(se);
                }
            }
            if (strEntryList.size() > 0) {
                buildNearList(strEntryList, status + " (" + fname + ")");
            }
        } catch (ParseException e) {
            throw new IOException("Parse error in '" + fname + "'\n" + e);
        }
    }

    protected void displayWarning(String msg, Throwable e) {
        if (m_transFrame == null) {
            System.out.println(OStrings.LD_WARNING + " " + msg);
        } else MessageRelay.uiMessageDisplayWarning(m_transFrame, msg, e);
    }

    protected void displayError(String msg, Throwable e) {
        if (m_transFrame == null) {
            System.out.println(OStrings.LD_ERROR + " " + msg);
        } else MessageRelay.uiMessageDisplayError(m_transFrame, msg, e);
    }

    protected void buildWordCounts() {
        ListIterator it;
        StringEntry se;
        LinkedList pl;
        m_totalWords = 0;
        m_partialWords = 0;
        int words = 0;
        it = m_strEntryList.listIterator();
        while (it.hasNext()) {
            se = (StringEntry) it.next();
            pl = se.getParentList();
            words = se.getWordCount();
            m_partialWords += words;
            m_totalWords += (words * pl.size());
        }
        String fn = m_config.getInternal() + OConsts.WORD_CNT_FILE_EXT;
        FileWriter ofp = null;
        try {
            ofp = new FileWriter(fn);
            ofp.write("Word count in unique segments: " + m_partialWords + "\n");
            ofp.write("Total word count: " + m_totalWords + "\n");
            it = m_srcTextEntryArray.listIterator();
            SourceTextEntry ste;
            String curFile = "";
            String file;
            words = 0;
            int totWords = 0;
            ListIterator it2;
            while (it.hasNext()) {
                ste = (SourceTextEntry) it.next();
                file = ste.getSrcFile().name;
                if (curFile.compareTo(file) != 0) {
                    if (curFile.length() > 0) ofp.write(curFile + "\t" + totWords + "\n");
                    curFile = file;
                    totWords = 0;
                }
                words = ste.getStrEntry().getWordCount();
                totWords += words;
                m_currentWords += words;
            }
            if (curFile.length() > 0) {
                ofp.write(curFile + "\t" + totWords + "\n\n");
            }
            ofp.write("Words remaining to translate: " + m_currentWords + "\n");
            ofp.close();
        } catch (IOException e) {
            try {
                if (ofp != null) ofp.close();
            } catch (IOException e2) {
                ;
            }
        }
    }

    protected void buildFreqTable(FreqList freqList, StringEntry strEntry, StringFreqData wordFreq, ArrayList wordList, StringFreqData pairFreq, ArrayList pairList) {
        String tok = null;
        String curWord = null;
        String lastWord = null;
        String wordPair = null;
        Adler32 ad = new Adler32();
        StringData curSD = null;
        StringData lastSD = null;
        StringData pairSD = null;
        TreeMap foundList = null;
        StringEntry se = null;
        Object obj = null;
        String lastTok = " ";
        long dig;
        ArrayList tokenList = new ArrayList();
        StaticUtils.tokenizeText(strEntry.getSrcText(), tokenList);
        for (int i = 0; i < tokenList.size(); i++) {
            tok = ((Token) tokenList.get(i)).text;
            ad.reset();
            lastWord = curWord;
            curWord = tok.toLowerCase();
            wordPair = lastWord + curWord;
            ad.update(curWord.getBytes());
            curSD = new StringData(ad.getValue(), tok);
            wordList.add(curSD);
            if (freqList == null) wordFreq.sub(curSD.getDigest(), tok); else wordFreq.add(curSD.getDigest(), tok);
            dig = curSD.getDigest();
            if (lastSD != null) dig += (lastSD.getDigest() << 32);
            pairSD = new StringData(dig, lastTok + tok);
            pairList.add(pairSD);
            if (freqList == null) pairFreq.sub(pairSD.getDigest(), wordPair); else pairFreq.add(pairSD.getDigest(), wordPair);
            curSD.setLow(pairSD.getDigest());
            if (lastSD != null) lastSD.setHigh(pairSD.getDigest());
            if (freqList != null) {
                foundList = find(curWord);
                if (foundList != null) {
                    while (foundList.size() > 0) {
                        obj = foundList.firstKey();
                        se = (StringEntry) foundList.remove(obj);
                        if (se != strEntry) freqList.add(se);
                    }
                }
            }
            lastTok = tok;
            lastSD = curSD;
        }
        if (curWord == null) return;
        ad.reset();
        ad.update(curWord.getBytes());
        pairSD = new StringData(ad.getValue(), tok);
        curSD.setHigh(pairSD.getDigest());
        pairList.add(pairSD);
        if (freqList == null) pairFreq.sub(curSD.getDigest(), tok); else pairFreq.add(curSD.getDigest(), tok);
    }

    protected byte[] buildAttrList(ArrayList tokList, StringFreqData wordFreq, StringFreqData pairFreq) {
        byte[] attr = new byte[tokList.size()];
        StringData tokData;
        StringData freqData;
        StringData low;
        StringData high;
        for (int i = 0; i < tokList.size(); i++) {
            attr[i] = 0;
            tokData = (StringData) tokList.get(i);
            freqData = wordFreq.getObj(tokData.getDigest());
            if (freqData.isUnique()) {
                attr[i] |= StringData.UNIQ;
            }
        }
        for (int i = 0; i < tokList.size() - 1; i++) {
            tokData = (StringData) tokList.get(i);
            low = pairFreq.getObj(tokData.getLow());
            high = pairFreq.getObj(tokData.getHigh());
            if (low.isUnique() || high.isUnique()) {
                attr[i] |= StringData.PAIR;
            }
        }
        return attr;
    }

    public PreferenceManager getPrefManager() {
        return m_prefManager;
    }

    public String getPreference(String str) {
        return m_prefManager.getPreference(str);
    }

    public void setPreference(String name, String value) {
        m_prefManager.setPreference(name, value);
    }

    public void savePreferences() {
        m_prefManager.save();
    }

    public String getOrSetPreference(String name, String value) {
        String val = m_prefManager.getPreference(name);
        if (val.equals("")) {
            val = value;
            setPreference(name, value);
        }
        return val;
    }

    public SourceTextEntry getSTE(int num) {
        if (num >= 0) return (SourceTextEntry) m_srcTextEntryArray.get(num); else return null;
    }

    public StringEntry getStringEntry(String srcText) {
        return (StringEntry) m_strEntryHash.get(srcText);
    }

    public String sourceRoot() {
        return m_config.getSrcRoot();
    }

    public String projName() {
        return m_config.getProjName();
    }

    public int numEntries() {
        return m_srcTextEntryArray.size();
    }

    public void setProjWin(ProjectFrame win) {
        m_projWin = win;
    }

    public TransFrame getTransFrame() {
        return m_transFrame;
    }

    public ArrayList getTransMemory() {
        return m_tmList;
    }

    public StringEntry getStringEntry(int n) {
        return (StringEntry) m_strEntryList.get(n);
    }

    private SaveThread m_saveThread = null;

    private int m_saveCount;

    public static CommandThread core = null;

    private ProjectProperties m_config;

    private boolean m_ignoreNearLog = false;

    private boolean m_modifiedFlag = false;

    protected PreferenceManager m_prefManager = null;

    private boolean m_stop = false;

    private boolean m_indexReady = true;

    private LinkedList m_requestQueue;

    private int m_totalWords = 0;

    private int m_partialWords = 0;

    private int m_currentWords;

    private String m_nearProj = null;

    protected ProjectFileData m_curFile = null;

    private TransFrame m_transFrame = null;

    private ProjectFrame m_projWin = null;

    private boolean m_glosFlag;

    private HashMap m_strEntryHash;

    private ArrayList m_strEntryList;

    private ArrayList m_srcTextEntryArray;

    private HashMap m_glosEntryHash;

    private ArrayList m_glosEntryList;

    private HashMap m_indexHash;

    protected ArrayList m_tmList;

    protected ArrayList m_orphanedList;

    private ArrayList m_extensionList;

    ;

    private ArrayList m_extensionMapList;

    ;
}
