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

    public CommandThread(TransFrame tf) {
        setName("Command thread");
        m_transFrame = tf;
        m_config = new ProjectProperties();
        m_strEntryHash = new HashMap(4096);
        m_strEntryList = new ArrayList();
        m_glosEntryHash = new HashMap(2048);
        m_glosEntryList = new ArrayList();
        m_srcTextEntryArray = new ArrayList(4096);
        m_indexHash = new HashMap(8192);
        m_modifiedFlag = false;
        m_extensionList = new ArrayList(32);
        m_extensionMapList = new ArrayList(32);
        m_requestQueue = new LinkedList();
        m_projWin = null;
        m_saveCount = -1;
        m_saveThread = null;
    }

    public void run() {
        RequestPacket pack = new RequestPacket();
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

    protected synchronized void messageBoard(boolean post, RequestPacket pack) {
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
        m_extensionList.clear();
        m_extensionMapList.clear();
        m_strEntryList.clear();
        m_glosEntryList.clear();
        m_srcTextEntryArray.clear();
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
            uiMessageSetMessageText(tf, evtStr);
            if (loadProject() == false) {
                evtStr = OStrings.CT_CANCEL_LOAD;
                uiMessageSetMessageText(tf, evtStr);
                return;
            }
            if (numEntries() <= 0) throw new IOException("empty project");
            tf.finishLoadProject();
            uiMessageDisplayEntry(tf, true);
            if (m_saveCount == -1) evtStr = OStrings.CT_LOADING_INDEX;
            uiMessageSetMessageText(tf, evtStr);
            buildIndex();
            evtStr = OStrings.CT_LOADING_GLOSSARY;
            uiMessageSetMessageText(tf, evtStr);
            buildGlossary();
            String status = OStrings.CT_FUZZY_X_OF_Y;
            buildNearList(m_strEntryList, status);
            loadTM();
            evtStr = OStrings.CT_LOADING_WORDCOUNT;
            uiMessageSetMessageText(tf, evtStr);
            buildWordCounts();
            uiMessageDisplayEntry(tf, false);
            uiMessageSetMessageText(tf, "");
            m_saveCount = 2;
        } catch (InterruptedIOException e1) {
            ;
        } catch (IOException e) {
            String msg = OStrings.TF_LOAD_ERROR;
            displayError(msg, e);
        }
    }

    static class MessageRelay implements Runnable {

        public MessageRelay(TransFrame tf, int cmd) {
            m_cmdNum = 0;
            m_tf = tf;
            if ((cmd >= 1) || (cmd <= 4)) m_cmdNum = cmd;
        }

        public MessageRelay(TransFrame tf, int cmd, boolean param) {
            m_cmdNum = 0;
            m_tf = tf;
            if (cmd == 5) {
                m_bParam = param;
                m_cmdNum = cmd;
            }
        }

        public MessageRelay(TransFrame tf, int cmd, String msg) {
            m_cmdNum = 0;
            m_tf = tf;
            if ((cmd == 6) || (cmd == 7)) {
                m_cmdNum = cmd;
                m_msg = msg;
            }
        }

        public MessageRelay(TransFrame tf, int cmd, String msg, Throwable e) {
            m_cmdNum = 0;
            m_tf = tf;
            if ((cmd == 8) || (cmd == 9)) {
                m_cmdNum = cmd;
                m_msg = msg;
                m_throw = e;
            }
        }

        public void run() {
            switch(m_cmdNum) {
                case 1:
                    m_tf.doPseudoTrans();
                    break;
                case 2:
                    m_tf.doNextEntry();
                    break;
                case 3:
                    m_tf.doPrevEntry();
                    break;
                case 4:
                    m_tf.doRecycleTrans();
                    break;
                case 5:
                    m_tf.displayEntry(m_bParam);
                    break;
                case 6:
                    m_tf.doGotoEntry(m_msg);
                    break;
                case 7:
                    m_tf.setMessageText(m_msg);
                    break;
                case 8:
                    m_tf.displayWarning(m_msg, m_throw);
                    break;
                case 9:
                    m_tf.displayError(m_msg, m_throw);
                    break;
                default:
                    break;
            }
            ;
        }

        String m_msg = "";

        boolean m_bParam = false;

        int m_cmdNum = 0;

        Throwable m_throw = null;

        TransFrame m_tf = null;
    }

    public static void uiMessageDoPseudoTrans(TransFrame tf) {
        MessageRelay msg = new MessageRelay(tf, 1);
        SwingUtilities.invokeLater(msg);
    }

    public static void uiMessageDoNextEntry(TransFrame tf) {
        MessageRelay msg = new MessageRelay(tf, 2);
        SwingUtilities.invokeLater(msg);
    }

    public static void uiMessageDoPrevEntry(TransFrame tf) {
        MessageRelay msg = new MessageRelay(tf, 3);
        SwingUtilities.invokeLater(msg);
    }

    public static void uiMessageDoRecycleTrans(TransFrame tf) {
        MessageRelay msg = new MessageRelay(tf, 4);
        SwingUtilities.invokeLater(msg);
    }

    public static void uiMessageDisplayEntry(TransFrame tf, boolean update) {
        MessageRelay msg = new MessageRelay(tf, 5, update);
        SwingUtilities.invokeLater(msg);
    }

    public static void uiMessageDoGotoEntry(TransFrame tf, String str) {
        MessageRelay msg = new MessageRelay(tf, 6, str);
        SwingUtilities.invokeLater(msg);
    }

    public static void uiMessageSetMessageText(TransFrame tf, String str) {
        MessageRelay msg = new MessageRelay(tf, 7, str);
        SwingUtilities.invokeLater(msg);
    }

    public static void uiMessageDisplayWarning(TransFrame tf, String str, Throwable e) {
        MessageRelay msg = new MessageRelay(tf, 8, str, e);
        SwingUtilities.invokeLater(msg);
    }

    public static void uiMessageDisplayError(TransFrame tf, String str, Throwable e) {
        MessageRelay msg = new MessageRelay(tf, 9, str, e);
        SwingUtilities.invokeLater(msg);
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

    public ArrayList getContext(int num, int low, int high) {
        ArrayList arr = new ArrayList(low + high + 1);
        StringEntry strEntry;
        SourceTextEntry srcTextEntry;
        String s;
        SourceTextEntry baseSTE = (SourceTextEntry) m_srcTextEntryArray.get(num);
        String sFile = baseSTE.getSrcFile();
        for (int i = num - low; i < num + high + 1; i++) {
            if (i == num) continue;
            if ((i < 0) || (i >= numEntries()) || (i == num)) {
                arr.add(null);
                continue;
            }
            srcTextEntry = (SourceTextEntry) m_srcTextEntryArray.get(i);
            if (sFile.compareTo(srcTextEntry.getSrcFile()) != 0) {
                arr.add(null);
                continue;
            }
            EntryData data = getEntryTextOnly(i);
            if (data.trans.equals("")) s = data.srcText; else s = data.trans;
            arr.add(s);
        }
        return arr;
    }

    public EntryData getEntry(int num) {
        ExchangeRequest exchange = new ExchangeRequest();
        exchange.detail = 1;
        exchange.entryNum = num;
        exchangeEntryData(exchange);
        return exchange.data;
    }

    public EntryData getEntryTextOnly(int num) {
        ExchangeRequest exchange = new ExchangeRequest();
        exchange.detail = 2;
        exchange.entryNum = num;
        exchangeEntryData(exchange);
        return exchange.data;
    }

    public String getTranslationOnly(int num) {
        ExchangeRequest exchange = new ExchangeRequest();
        exchange.detail = 2;
        exchange.entryNum = num;
        exchangeEntryData(exchange);
        return exchange.data.trans;
    }

    public String getTranslation(String src) {
        ExchangeRequest exchange = new ExchangeRequest();
        exchange.detail = 3;
        exchange.srcText = src;
        exchangeEntryData(exchange);
        return exchange.trans;
    }

    public void setTranslation(int entryNum, String trans) {
        ExchangeRequest exchange = new ExchangeRequest();
        exchange.detail = 4;
        exchange.entryNum = entryNum;
        exchange.trans = trans;
        exchangeEntryData(exchange);
    }

    protected synchronized void exchangeEntryData(ExchangeRequest exch) {
        switch(exch.detail) {
            case 1:
                getEntrySynchronized(exch);
                break;
            case 2:
                getEntryTextOnlySynchronized(exch);
                break;
            case 3:
                getTranslationSynchronized(exch);
                break;
            case 4:
                setTranslationSynchronized(exch);
                break;
            default:
                System.out.println("ERROR - untrapped exchange call");
        }
    }

    protected class ExchangeRequest {

        ExchangeRequest() {
            detail = 0;
        }

        public int entryNum;

        public String trans;

        public String srcText;

        public EntryData data;

        public int detail;
    }

    protected void getEntrySynchronized(ExchangeRequest exchange) {
        EntryData data = new EntryData();
        SourceTextEntry srcTextEntry = (SourceTextEntry) m_srcTextEntryArray.get(exchange.entryNum);
        StringEntry strEntry = srcTextEntry.getStrEntry();
        data.srcText = strEntry.getSrcText();
        data.trans = strEntry.getTrans();
        data.file = srcTextEntry.getSrcFile();
        data.partialWords = m_partialWords;
        data.totalWords = m_totalWords;
        data.currentWords = m_currentWords;
        LinkedList lst = strEntry.getNearList();
        NearString ns;
        double score = 0;
        if (lst.size() > 0) {
            data.addNearTerms(lst);
        }
        lst = strEntry.getGlosList();
        if (lst.size() > 0) {
            data.setGlosTerms(lst);
        }
        exchange.data = data;
    }

    protected void getEntryTextOnlySynchronized(ExchangeRequest exchange) {
        EntryData data = new EntryData();
        SourceTextEntry srcTextEntry = (SourceTextEntry) m_srcTextEntryArray.get(exchange.entryNum);
        StringEntry strEntry = srcTextEntry.getStrEntry();
        data.srcText = strEntry.getSrcText();
        data.trans = strEntry.getTrans();
        exchange.data = data;
    }

    protected void getTranslationSynchronized(ExchangeRequest exchange) {
        exchange.trans = exchange.srcText;
        SourceTextEntry srcTE;
        StringEntry strEntry;
        strEntry = (StringEntry) m_strEntryHash.get(exchange.srcText);
        if (strEntry != null) {
            String s;
            s = strEntry.getTrans();
            if (s != null) exchange.trans = s;
        }
    }

    public void setTranslationSynchronized(ExchangeRequest exchange) {
        m_modifiedFlag = true;
        SourceTextEntry srcTextEntry = (SourceTextEntry) m_srcTextEntryArray.get(exchange.entryNum);
        StringEntry strEntry = srcTextEntry.getStrEntry();
        int num = 0;
        if (strEntry.getTrans() == null) {
            num = strEntry.getParentList().size();
            m_currentWords -= (num * strEntry.getWordCount());
        }
        strEntry.setTranslation(exchange.trans);
    }

    public void compileProject() throws IOException {
        int i;
        int j;
        String srcRoot = m_config.getSrcRoot();
        String locRoot = m_config.getLocRoot();
        String t;
        String s;
        boolean err = false;
        save();
        FileHandler fh = null;
        HandlerMaster hm = new HandlerMaster();
        ArrayList fileList = new ArrayList(256);
        boolean ignore;
        String filename;
        String destFileName;
        int namePos;
        String shortName;
        try {
            File tm;
            DataOutputStream dos;
            StringEntry se;
            tm = new File(m_config.getProjFileBase() + OConsts.TM_EXTENSION);
            dos = new DataOutputStream(new FileOutputStream(tm));
            dos.writeUTF(OConsts.TM_FILE_IDENT);
            dos.writeInt(OConsts.TM_CUR_VERSION);
            dos.writeUTF(m_config.getProjName());
            for (i = 0; i < m_strEntryList.size(); i++) {
                se = (StringEntry) m_strEntryList.get(i);
                s = se.getSrcText();
                t = se.getTrans();
                dos.writeUTF(s);
                dos.writeUTF(t);
            }
            dos.writeUTF(OConsts.UTF8_END_OF_LIST);
            dos.close();
        } catch (IOException e) {
            System.out.println("Exception encountered: " + e);
            System.out.println("Unable to build TM file");
            err = true;
        }
        buildDirList(fileList, new File(srcRoot));
        File fname;
        for (i = 0; i < fileList.size(); i++) {
            filename = (String) fileList.get(i);
            destFileName = m_config.getLocRoot() + filename.substring(m_config.getSrcRoot().length() + 1);
            ignore = false;
            fname = new File(destFileName);
            if (!fname.exists()) {
                if (fname.mkdir() == false) {
                    throw new IOException("Can't create target language " + "directory " + destFileName);
                }
            }
        }
        fileList.clear();
        buildFileList(fileList, new File(srcRoot));
        for (i = 0; i < fileList.size(); i++) {
            filename = (String) fileList.get(i);
            destFileName = m_config.getLocRoot() + filename.substring(m_config.getSrcRoot().length() + 1);
            ignore = false;
            namePos = filename.lastIndexOf(File.separator) + 1;
            shortName = filename.substring(namePos);
            for (j = 0; j < m_ignoreList.size(); j++) {
                if (shortName.equals(m_ignoreList.get(j))) {
                    ignore = true;
                    LFileCopy.copy(filename, destFileName);
                    break;
                }
            }
            if (ignore == true) continue;
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
                continue;
            }
            String midName = filename.substring(srcRoot.length());
            s = srcRoot + midName;
            t = locRoot + midName;
            fh.write(s, t);
        }
        if (err == true) {
            throw new IOException("Can't build TM file");
        }
    }

    public void save() {
        if (m_modifiedFlag == false) return;
        forceSave(false);
    }

    protected synchronized void forceSave(boolean corruptionDanger) {
        if (m_saveCount <= 0) return; else if (m_saveCount == 1) m_saveCount = 0;
        try {
            if (corruptionDanger == false) System.out.println("changes detected - saving project");
            EntryData entryData;
            ListIterator it;
            String trans;
            StringEntry strEntry;
            String s = m_config.getInternal() + OConsts.STATUS_EXTENSION;
            if (corruptionDanger) {
                s += OConsts.STATUS_RECOVER_EXTENSION;
            } else {
                File backup = new File(s + OConsts.BACKUP_EXTENSION);
                File orig = new File(s);
                if (orig.exists()) orig.renameTo(backup);
            }
            File outFile = new File(s);
            DataOutputStream dos;
            dos = new DataOutputStream(new FileOutputStream(outFile));
            dos.writeUTF(OConsts.STATUS_FILE_IDENT);
            dos.writeInt(OConsts.STATUS_CUR_VERSION);
            for (int i = 0; i < m_srcTextEntryArray.size(); i++) {
                entryData = getEntryTextOnly(i);
                dos.writeUTF(entryData.srcText);
                dos.writeUTF(entryData.trans);
            }
            dos.writeUTF(OConsts.UTF8_END_OF_LIST);
            dos.close();
            m_modifiedFlag = false;
        } catch (IOException e) {
            String msg = OStrings.CT_ERROR_SAVING_PROJ;
            displayError(msg, e);
            if (corruptionDanger == false) {
                String s = m_config.getInternal() + OConsts.STATUS_EXTENSION;
                File backup = new File(s + OConsts.BACKUP_EXTENSION);
                File orig = new File(s);
                if (backup.exists()) backup.renameTo(orig);
            }
        }
        if ((m_modifiedFlag == false) && (corruptionDanger == false)) {
            String s = m_config.getInternal() + OConsts.STATUS_EXTENSION;
            File backup = new File(s + OConsts.BACKUP_EXTENSION);
            if (backup.exists()) backup.delete();
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
        srcTextEntry.set(strEntry, file, m_srcTextEntryArray.size());
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
            buildDefaultHandlerFile(hand);
            buildDefaultIgnoreFile();
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
                    String msg = OStrings.CT_ERROR_CREATE + " (.../tm/)";
                    throw new IOException(msg);
                }
            }
            m_config.buildProjFile();
        } catch (IOException e) {
            String msg = "failed to create project";
            displayError(msg, e);
        }
    }

    public TreeMap findAll(String tokenList) {
        if (m_indexReady == false) return null;
        TreeMap foundList = null;
        String str = tokenList.toLowerCase();
        String s;
        StringTokenizer st = new StringTokenizer(str);
        while (st.hasMoreTokens()) {
            s = stripString(st.nextToken());
            if (s == null) continue;
            if (foundList == null) foundList = find(s); else foundList = refineQuery(s, foundList);
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
            DataInputStream dis = new DataInputStream(new FileInputStream(proj));
            String ident = dis.readUTF();
            if (ident.compareTo(OConsts.STATUS_FILE_IDENT) != 0) {
                throw new IOException("unrecognized status file");
            }
            int vers = dis.readInt();
            if (vers != OConsts.STATUS_CUR_VERSION) throw new IOException("unsupported version");
            srcText = dis.readUTF();
            int cnt = 0;
            while (srcText.equals(OConsts.UTF8_END_OF_LIST) == false) {
                obj = m_strEntryHash.get(srcText);
                strEntry = (StringEntry) obj;
                trans = dis.readUTF();
                if (strEntry != null) strEntry.setTranslation(trans);
                srcText = dis.readUTF();
            }
            dis.close();
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
        ;
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
                        if (loc.equals("")) addGlosEntry(src); else addGlosEntry(src, loc);
                    }
                } else {
                    System.out.println(OStrings.CT_DONT_RECOGNIZE_GLOS_FILE + fname);
                }
            }
        } else {
            throw new IOException("can't access glossary directory");
        }
        m_ignoreList = new ArrayList(32);
        String ignoreName = m_config.getInternal() + OConsts.IGNORE_LIST;
        File ignoreFile = new File(ignoreName);
        if (ignoreFile.exists()) {
            String str;
            try {
                tab.load(ignoreName);
                for (i = 0; i < tab.numRows(); i++) {
                    str = tab.get(i, 0);
                    str.trim();
                    System.out.println("ignoring file " + str);
                    m_ignoreList.add(str);
                }
            } catch (IOException e) {
                System.out.println(OStrings.CT_ERROR_LOADING_IGNORE_FILE);
            }
        } else System.out.println(OStrings.CT_ERROR_FINDING_IGNORE_FILE);
        String handlerName = m_config.getInternal() + OConsts.HANDLER_LIST;
        File handlerFile = new File(handlerName);
        if (handlerFile.exists()) {
            String ext;
            String map;
            try {
                tab.load(handlerName);
                for (i = 0; i < tab.numRows(); i++) {
                    ext = tab.get(i, 0);
                    map = tab.get(i, 1);
                    if (ext.equals("") || (map.equals(""))) continue;
                    ext.trim();
                    map.trim();
                    if (ext.charAt(0) == '.') ext = ext.substring(1);
                    if (map.charAt(0) == '.') map = map.substring(1);
                    m_extensionList.add(ext);
                    m_extensionMapList.add(map);
                    System.out.println("mapping '." + ext + "' to '." + map + "'");
                }
            } catch (IOException e) {
                System.out.println(OStrings.CT_ERROR_LOADING_HANDLER_FILE);
            }
        } else System.out.println(OStrings.CT_ERROR_FINDING_HANDLER_FILE);
        FileHandler fh = null;
        HandlerMaster hm = new HandlerMaster();
        ArrayList srcFileList = new ArrayList(256);
        File root = new File(m_config.getSrcRoot());
        buildFileList(srcFileList, root);
        boolean ignore;
        int namePos;
        String shortName;
        String filename;
        for (i = 0; i < srcFileList.size(); i++) {
            filename = (String) srcFileList.get(i);
            ignore = false;
            namePos = filename.lastIndexOf(File.separator) + 1;
            shortName = filename.substring(namePos);
            for (j = 0; j < m_ignoreList.size(); j++) {
                if (shortName.equals(m_ignoreList.get(j))) {
                    ignore = true;
                    break;
                }
            }
            if (ignore == true) continue;
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
            m_projWin.addFile(filename.substring(m_config.getSrcRoot().length()), numEntries());
            System.out.println("loading file '" + filename + "'");
            fh.load(filename);
        }
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
        StringTokenizer st;
        IndexEntry indexEntry;
        for (int i = 0; i < m_strEntryList.size(); i++) {
            wordCount = 0;
            strEntry = (StringEntry) m_strEntryList.get(i);
            s = strEntry.getSrcText();
            st = new StringTokenizer(s);
            while (st.hasMoreTokens()) {
                s = stripString(st.nextToken());
                if (s == null) continue;
                s = s.toLowerCase();
                indexEntry = (IndexEntry) m_indexHash.get(s);
                if (indexEntry == null) {
                    indexEntry = new IndexEntry(s);
                    m_indexHash.put(s, indexEntry);
                }
                indexEntry.addReference(strEntry);
                wordCount++;
            }
            strEntry.setWordCount(wordCount);
        }
    }

    protected void buildGlossary() {
        int i;
        StringEntry glosEntry;
        StringEntry strEntry;
        String glosStr;
        String glosStrLow;
        String s;
        int pos;
        TreeMap foundList;
        for (i = 0; i < m_glosEntryList.size(); i++) {
            glosEntry = (StringEntry) m_glosEntryList.get(i);
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
        StringTokenizer st;
        String tok;
        String curWord = null;
        String lastWord = null;
        String wordPair;
        int i, j;
        byte[] candAttr;
        byte[] strAttr;
        double ratio;
        double pairRatio;
        Integer len = new Integer(seList.size());
        for (i = 0; i < seList.size(); i++) {
            if (i % 10 == 0) {
                Object[] obj = { new Integer(i), len };
                uiMessageSetMessageText(m_transFrame, MessageFormat.format(evtStr, obj));
            }
            if (i == 1) {
                uiMessageDisplayEntry(m_transFrame, false);
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
                ratio = 1.0 * freqList.getCountN(j) / cand.getWordCount();
                if (ratio < OConsts.NEAR_THRESH) continue;
                candList.clear();
                candPairList.clear();
                wordFreq = (StringFreqData) masterWordFreq.clone();
                pairFreq = (StringFreqData) masterPairFreq.clone();
                buildFreqTable(null, cand, wordFreq, candList, pairFreq, candPairList);
                ratio = wordFreq.getMatchRatio();
                if (ratio < OConsts.NEAR_THRESH) continue;
                pairRatio = pairFreq.getMatchRatio();
                if (pairRatio < OConsts.PAIR_THRESH) continue;
                candAttr = buildAttrList(candList, wordFreq, pairFreq);
                strAttr = buildAttrList(wordList, wordFreq, pairFreq);
                ratio = Math.sqrt(ratio * pairRatio);
                if (seList == m_strEntryList) {
                    registerNear(strEntry, cand, ratio, strAttr, candAttr);
                } else {
                    cand.addNearString(strEntry, ratio, candAttr, strAttr, m_nearProj);
                }
            }
        }
        if (m_nearLog != null) {
            try {
                m_nearLog.close();
                m_nearLog = null;
            } catch (IOException e) {
                ;
            }
        }
    }

    protected void loadTM() throws IOException, FileNotFoundException, InterruptedIOException {
        String status = OStrings.CT_TM_X_OF_Y;
        String[] fileList;
        String lang;
        File f;
        int i;
        String fname;
        ArrayList strEntryList = new ArrayList(m_strEntryList.size());
        DataInputStream dis;
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
            f = new File(m_config.getTMRoot() + fname);
            dis = new DataInputStream(new FileInputStream(f));
            String ident = dis.readUTF();
            if (ident.compareTo(OConsts.TM_FILE_IDENT) != 0) {
                dis.close();
                continue;
            }
            int vers = dis.readInt();
            if (vers != OConsts.TM_CUR_VERSION) {
                throw new IOException("unsupported translation memory file version (" + fname + ")");
            }
            System.out.println("Processing TM file '" + fname + "'");
            m_nearProj = dis.readUTF();
            while (true) {
                if (dis.available() <= 0) break;
                src = dis.readUTF();
                if (src.compareTo(OConsts.UTF8_END_OF_LIST) == 0) {
                    break;
                }
                trans = dis.readUTF();
                se = new StringEntry(src);
                se.setTranslation(trans);
                strEntryList.add(se);
            }
            buildNearList(strEntryList, status + " (" + fname + ")");
            dis.close();
        }
        m_nearProj = null;
    }

    protected void addGlosEntry(String srcText) {
        if (m_glosEntryHash.get(srcText) == null) {
            StringEntry strEntry = new StringEntry(srcText);
            m_glosEntryHash.put(srcText, strEntry);
            m_glosEntryList.add(strEntry);
        }
    }

    protected void addGlosEntry(String srcText, String locText) {
        if (m_glosEntryHash.get(srcText) == null) {
            StringEntry strEntry = new StringEntry(srcText);
            strEntry.setTranslation(locText);
            m_glosEntryHash.put(srcText, strEntry);
            m_glosEntryList.add(strEntry);
        }
    }

    protected void buildFileList(ArrayList lst, File rootDir) {
        int i;
        File[] flist = rootDir.listFiles();
        for (i = 0; i < Array.getLength(flist); i++) {
            if (flist[i].isDirectory()) {
                continue;
            }
            lst.add(flist[i].getAbsolutePath());
        }
        for (i = 0; i < Array.getLength(flist); i++) {
            if (flist[i].isDirectory()) {
                buildFileList(lst, flist[i]);
            }
        }
    }

    protected void buildDirList(ArrayList lst, File rootDir) {
        int i;
        File[] flist = rootDir.listFiles();
        for (i = 0; i < Array.getLength(flist); i++) {
            if (flist[i].isDirectory()) {
                lst.add(flist[i].getAbsolutePath());
                buildDirList(lst, flist[i]);
            }
        }
    }

    protected void displayWarning(String msg, Throwable e) {
        if (m_transFrame == null) {
            System.out.println(OStrings.LD_WARNING + " " + msg);
        } else uiMessageDisplayWarning(m_transFrame, msg, e);
    }

    protected void displayError(String msg, Throwable e) {
        if (m_transFrame == null) {
            System.out.println(OStrings.LD_ERROR + " " + msg);
        } else uiMessageDisplayError(m_transFrame, msg, e);
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
                file = ste.getSrcFile();
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
                ofp.close();
            } catch (IOException e2) {
                ;
            }
        }
    }

    protected void buildFreqTable(FreqList freqList, StringEntry strEntry, StringFreqData wordFreq, ArrayList wordList, StringFreqData pairFreq, ArrayList pairList) {
        StringTokenizer st;
        String tok = null;
        String s = null;
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
        String lastTok = "";
        long dig;
        st = new StringTokenizer(strEntry.getSrcText());
        while (st.hasMoreTokens()) {
            tok = st.nextToken();
            ad.reset();
            if ((s = stripString(tok)) == null) continue;
            lastWord = curWord;
            curWord = s.toLowerCase();
            wordPair = lastWord + curWord;
            ad.update(curWord.getBytes());
            curSD = new StringData(ad.getValue(), tok);
            wordList.add(curSD);
            if (freqList == null) wordFreq.sub(curSD.getDigest(), tok); else wordFreq.add(curSD.getDigest(), tok);
            dig = curSD.getDigest();
            if (lastSD != null) dig += (lastSD.getDigest() << 32);
            pairSD = new StringData(dig, lastTok + tok);
            pairList.add(pairSD);
            if (freqList != null) pairFreq.sub(pairSD.getDigest(), wordPair); else pairFreq.add(pairSD.getDigest(), wordPair);
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
        if (freqList != null) pairFreq.sub(curSD.getDigest(), tok); else pairFreq.add(curSD.getDigest(), tok);
    }

    protected byte[] buildAttrList(ArrayList tokList, StringFreqData wordFreq, StringFreqData pairFreq) {
        byte[] attr = new byte[tokList.size()];
        StringData tokData;
        StringData freqData;
        StringData low;
        StringData high;
        int i = 0;
        byte uniqNearMask = (StringData.UNIQ | StringData.NEAR);
        for (i = 0; i < tokList.size(); i++) {
            attr[i] = 0;
            tokData = (StringData) tokList.get(i);
            freqData = wordFreq.getObj(tokData.getDigest());
            attr[i] |= freqData.getAttr() & uniqNearMask;
            low = pairFreq.getObj(tokData.getLow());
            high = pairFreq.getObj(tokData.getHigh());
            if (freqData.hasAttr(StringData.NEAR)) {
                if (((low.getAttr() | high.getAttr()) & uniqNearMask) == 0) {
                    attr[i] &= ~StringData.NEAR;
                }
            }
            if (low.isUnique() || high.isUnique()) attr[i] |= StringData.PAIR;
        }
        return attr;
    }

    protected boolean loadNearList() {
        boolean res = false;
        int cnt = 0;
        byte[] parData;
        byte[] nearData;
        int len;
        try {
            StringEntry strEntry;
            StringEntry cand;
            String src;
            String trans;
            double ratio;
            String s = m_config.getProjFileBase() + OConsts.FUZZY_EXTENSION;
            FileInputStream fis = new FileInputStream(s);
            DataInputStream dis = new DataInputStream(fis);
            String ident = dis.readUTF();
            if (ident.compareTo(OConsts.FUZZY_FILE_IDENT) != 0) {
                throw new IOException("unrecognized fuzzy file");
            }
            int vers = dis.readInt();
            if (vers != 1) throw new IOException("unsupported version");
            while (dis.available() > 0) {
                src = dis.readUTF();
                trans = dis.readUTF();
                ratio = dis.readDouble();
                len = dis.readInt();
                parData = new byte[len];
                dis.read(parData);
                len = dis.readInt();
                nearData = new byte[len];
                dis.read(nearData);
                strEntry = (StringEntry) m_strEntryHash.get(src);
                cand = (StringEntry) m_strEntryHash.get(trans);
                if ((strEntry == null) || (cand == null)) throw new IOException();
                m_ignoreNearLog = true;
                registerNear(strEntry, cand, ratio, parData, nearData);
                cnt++;
            }
            res = true;
        } catch (IOException e) {
            if (cnt > 0) res = true;
        }
        return res;
    }

    protected void registerNear(StringEntry strEntry, StringEntry cand, double ratio, ArrayList parDataArray, ArrayList nearDataArray) {
        int i;
        byte[] pData = new byte[parDataArray.size()];
        for (i = 0; i < pData.length; i++) pData[i] = ((StringData) parDataArray.get(i)).getAttr();
        byte[] nData = new byte[nearDataArray.size()];
        for (i = 0; i < nData.length; i++) nData[i] = ((StringData) nearDataArray.get(i)).getAttr();
        registerNear(strEntry, cand, ratio, pData, nData);
    }

    protected void registerNear(StringEntry strEntry, StringEntry cand, double ratio, byte[] parData, byte[] nearData) {
        strEntry.addNearString(cand, ratio, parData, nearData, null);
    }

    protected void buildDefaultHandlerFile(HandlerMaster hand) throws IOException {
        String name = m_config.getInternal() + OConsts.HANDLER_LIST;
        File handler = new File(name);
        if (handler.exists()) {
        }
        LTabFileReader tab = new LTabFileReader();
        int i;
        String ext = "";
        ArrayList handlers = hand.getHandlerList();
        for (i = 0; i < handlers.size(); i++) {
            ext += ((FileHandler) handlers.get(i)).preferredExtension();
            ext += " ";
        }
        tab.addLine("# Filename Extension Mapping File");
        tab.addLine("#");
        tab.addLine("# This version of OmegaT recognizes the following " + "file types: ");
        tab.addLine("#     " + ext);
        tab.addLine("# If there are files in the project that conform to " + "one of these");
        tab.addLine("# formats but have a different extension, you can " + "map the existing");
        tab.addLine("# file to one of these types below.");
        tab.addLine("#");
        tab.addLine("# To create a mapping, type in the file extension " + "used in your project");
        tab.addLine("# and then the extension of the file parser you wish " + "to be used for this");
        tab.addLine("# file, seperated by a tab character.  For example, " + "to map the file ");
        tab.addLine("# extension .swx to .html enter");
        tab.addLine("# swx <tab> html");
        tab.addLine("# without the '#' character or spaces, and where <tab> " + "represents the");
        tab.addLine("# tab key on the keyboard.  Do not use space, periods " + " or any other");
        tab.addLine("# punctuation when specifying the file extensions");
        tab.addLine("#");
        tab.addLine("# The following defines the default mapping from " + ".htm to .html");
        tab.addLine("htm\thtml");
        tab.write(name);
    }

    protected void buildDefaultIgnoreFile() throws IOException {
        String name = m_config.getInternal() + OConsts.IGNORE_LIST;
        File ignore = new File(name);
        if (ignore.exists()) {
            return;
        }
        LTabFileReader tab = new LTabFileReader();
        tab.addLine("# Ignore File List");
        tab.addLine("# ");
        tab.addLine("# You can cause certain files in the source language " + "directory to");
        tab.addLine("# be omitted from processing, and hence translation, " + "by listing them");
        tab.addLine("# in this file");
        tab.addLine("# ");
        tab.addLine("# To cause a file to be skipped, type it in below, " + "one filename per ");
        tab.addLine("# line, and do not use any spaces or extra characters " + "(like '#' for example");
        tab.addLine("# The single default entry below will cause the file " + "'DO_NOT_TRANSLATE.txt'");
        tab.addLine("# to be ignored.");
        tab.addLine("# ");
        tab.addLine("DO_NOT_TRANSLATE.txt");
        tab.addLine("");
        tab.write(name);
    }

    protected static int charType(char c) {
        int type = 0;
        if ((c == '.') || (c == '-') || (c == '@')) {
            type = 1;
        } else {
            int t = Character.getType(c);
            switch(t) {
                case Character.DASH_PUNCTUATION:
                case Character.START_PUNCTUATION:
                case Character.END_PUNCTUATION:
                case Character.CONNECTOR_PUNCTUATION:
                case Character.OTHER_PUNCTUATION:
                case Character.MATH_SYMBOL:
                case Character.CURRENCY_SYMBOL:
                case Character.MODIFIER_SYMBOL:
                    type = 2;
                default:
                    type = 0;
            }
            ;
        }
        return type;
    }

    public static String stripString(String token) {
        char[] str = new char[token.length() + 1];
        char[] tmp = new char[token.length() + 1];
        String s;
        char c;
        int cnt = 0;
        int tmpCnt = 0;
        boolean textFound = false;
        int type;
        boolean ignore;
        int j;
        token.getChars(0, token.length(), str, 0);
        for (int i = 0; i < token.length(); i++) {
            ignore = false;
            c = str[i];
            type = charType(c);
            if (textFound == false) {
                if (type == 0) textFound = true; else if (type == 1) ignore = true;
            }
            if (type == 2) {
                ignore = true;
            }
            if (ignore == false) {
                if (type == 0) {
                    for (j = 0; j < tmpCnt; j++) {
                        str[cnt++] = tmp[j];
                    }
                    tmpCnt = 0;
                    str[cnt++] = c;
                } else if (type == 1) {
                    tmp[tmpCnt++] = c;
                }
            }
        }
        if (textFound == false) return null; else {
            s = new String(str, 0, cnt);
            return s;
        }
    }

    public static void dumpEntry(String val, String file) {
        System.out.println(" val: " + val);
        System.out.println("file: " + file);
        System.out.println("");
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

    private SaveThread m_saveThread = null;

    private int m_saveCount;

    public static CommandThread core = null;

    private ProjectProperties m_config;

    private DataOutputStream m_nearLog = null;

    private boolean m_ignoreNearLog = false;

    private boolean m_modifiedFlag = false;

    private boolean m_stop = false;

    private boolean m_indexReady = true;

    private LinkedList m_requestQueue;

    private int m_totalWords = 0;

    private int m_partialWords = 0;

    private int m_currentWords;

    private String m_nearProj = null;

    private TransFrame m_transFrame = null;

    private ProjectFrame m_projWin = null;

    private boolean m_glosFlag;

    private HashMap m_strEntryHash;

    private ArrayList m_strEntryList;

    private ArrayList m_srcTextEntryArray;

    private HashMap m_glosEntryHash;

    private ArrayList m_glosEntryList;

    private HashMap m_indexHash;

    private ArrayList m_ignoreList;

    private ArrayList m_extensionList;

    ;

    private ArrayList m_extensionMapList;

    ;
}
