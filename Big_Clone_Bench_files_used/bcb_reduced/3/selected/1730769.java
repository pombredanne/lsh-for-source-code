package com.ecyrd.jspwiki;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.lang.time.StopWatch;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.ecyrd.jspwiki.attachment.Attachment;
import com.ecyrd.jspwiki.event.WikiEvent;
import com.ecyrd.jspwiki.event.WikiEventListener;
import com.ecyrd.jspwiki.event.WikiEventUtils;
import com.ecyrd.jspwiki.event.WikiPageEvent;
import com.ecyrd.jspwiki.filters.BasicPageFilter;
import com.ecyrd.jspwiki.modules.InternalModule;
import com.ecyrd.jspwiki.providers.ProviderException;
import com.ecyrd.jspwiki.providers.WikiPageProvider;
import com.madzone.gae.filem.FileInputStream;
import com.madzone.gae.filem.FileOutputStream;

public class ReferenceManager extends BasicPageFilter implements InternalModule, WikiEventListener {

    /** Maps page wikiname to a Collection of pages it refers to. The Collection
     *  must contain Strings. The Collection may contain names of non-existing
     *  pages.
     */
    private Map<String, Collection<String>> m_refersTo;

    private Map<String, Collection<String>> m_unmutableRefersTo;

    /** Maps page wikiname to a Set of referring pages. The Set must
     *  contain Strings. Non-existing pages (a reference exists, but not a file
     *  for the page contents) may have an empty Set in m_referredBy.
     */
    private Map<String, Set<String>> m_referredBy;

    private Map<String, Set<String>> m_unmutableReferredBy;

    /** The WikiEngine that owns this object. */
    private WikiEngine m_engine;

    private boolean m_matchEnglishPlurals = false;

    private static Logger log = Logger.getLogger(ReferenceManager.class.getName());

    private static final String SERIALIZATION_FILE = "/WIKI_ROOT/TMP/refmgr.ser";

    private static final String SERIALIZATION_DIR = "/WIKI_ROOT/TMP/refmgr-attr";

    /** We use this also a generic serialization id */
    private static final long serialVersionUID = 4L;

    /**
     *  Builds a new ReferenceManager.
     *
     *  @param engine The WikiEngine to which this is managing references to.
     */
    public ReferenceManager(WikiEngine engine) {
        m_refersTo = new HashMap<String, Collection<String>>();
        m_referredBy = new HashMap<String, Set<String>>();
        m_engine = engine;
        m_matchEnglishPlurals = TextUtil.getBooleanProperty(engine.getWikiProperties(), WikiEngine.PROP_MATCHPLURALS, m_matchEnglishPlurals);
        m_unmutableReferredBy = Collections.unmodifiableMap(m_referredBy);
        m_unmutableRefersTo = Collections.unmodifiableMap(m_refersTo);
    }

    /**
     *  Does a full reference update.  Does not sync; assumes that you do it afterwards.
     */
    @SuppressWarnings("unchecked")
    private void updatePageReferences(WikiPage page) throws ProviderException {
        String content = m_engine.getPageManager().getPageText(page.getName(), WikiPageProvider.LATEST_VERSION);
        TreeSet<String> res = new TreeSet<String>();
        Collection<String> links = m_engine.scanWikiLinks(page, content);
        res.addAll(links);
        Collection attachments = m_engine.getAttachmentManager().listAttachments(page);
        for (Iterator atti = attachments.iterator(); atti.hasNext(); ) {
            res.add(((Attachment) (atti.next())).getName());
        }
        internalUpdateReferences(page.getName(), res);
    }

    /**
     *  Initializes the entire reference manager with the initial set of pages
     *  from the collection.
     *
     *  @param pages A collection of all pages you want to be included in the reference
     *               count.
     *  @since 2.2
     *  @throws ProviderException If reading of pages fail.
     */
    public void initialize(Collection pages) throws ProviderException {
        log.log(Level.INFO, "Initializing new ReferenceManager with " + pages.size() + " initial pages.");
        StopWatch sw = new StopWatch();
        sw.start();
        log.log(Level.INFO, "Starting cross reference scan of WikiPages");
        try {
            long saved = unserializeFromDisk();
            for (Iterator it = pages.iterator(); it.hasNext(); ) {
                WikiPage page = (WikiPage) it.next();
                unserializeAttrsFromDisk(page);
            }
            Iterator it = pages.iterator();
            while (it.hasNext()) {
                WikiPage page = (WikiPage) it.next();
                if (page instanceof Attachment) {
                } else {
                    page = m_engine.getPage(page.getName());
                    if (page.getLastModified() == null) {
                        log.log(Level.SEVERE, "Provider returns null lastModified.  Please submit a bug report.");
                    } else if (page.getLastModified().getTime() > saved) {
                        updatePageReferences(page);
                    }
                }
            }
        } catch (Exception e) {
            log.log(Level.INFO, "Unable to unserialize old refmgr information, rebuilding database: " + e.getMessage(), e);
            buildKeyLists(pages);
            Iterator it = pages.iterator();
            while (it.hasNext()) {
                WikiPage page = (WikiPage) it.next();
                if (page instanceof Attachment) {
                } else {
                    updatePageReferences(page);
                    serializeAttrsToDisk(page);
                }
            }
            serializeToDisk();
        }
        sw.stop();
        log.log(Level.INFO, "Cross reference scan done in " + sw);
        WikiEventUtils.addWikiEventListener(m_engine.getPageManager(), WikiPageEvent.PAGE_DELETED, this);
    }

    /**
     *  Reads the serialized data from the disk back to memory.
     *  Returns the date when the data was last written on disk
     */
    @SuppressWarnings("unchecked")
    private synchronized long unserializeFromDisk() throws IOException, ClassNotFoundException {
        ObjectInputStream in = null;
        long saved = 0L;
        try {
            StopWatch sw = new StopWatch();
            sw.start();
            File f = new File(SERIALIZATION_FILE);
            in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(f)));
            long ver = in.readLong();
            if (ver != serialVersionUID) {
                throw new IOException("File format has changed; I need to recalculate references.");
            }
            saved = in.readLong();
            m_refersTo = (Map) in.readObject();
            m_referredBy = (Map) in.readObject();
            in.close();
            m_unmutableReferredBy = Collections.unmodifiableMap(m_referredBy);
            m_unmutableRefersTo = Collections.unmodifiableMap(m_refersTo);
            sw.stop();
            log.log(Level.INFO, "Read serialized data successfully in " + sw);
        } finally {
            if (in != null) in.close();
        }
        return saved;
    }

    /**
     *  Serializes hashmaps to disk.  The format is private, don't touch it.
     */
    private synchronized void serializeToDisk() {
        ObjectOutputStream out = null;
        try {
            StopWatch sw = new StopWatch();
            sw.start();
            File f = new File(SERIALIZATION_FILE);
            out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(f)));
            out.writeLong(serialVersionUID);
            out.writeLong(System.currentTimeMillis());
            out.writeObject(m_refersTo);
            out.writeObject(m_referredBy);
            out.close();
            sw.stop();
            log.log(Level.INFO, "serialization done - took " + sw);
        } catch (IOException e) {
            log.log(Level.SEVERE, "Unable to serialize!");
            try {
                if (out != null) out.close();
            } catch (IOException ex) {
            }
        }
    }

    private String getHashFileName(String pageName) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        byte[] dig;
        try {
            dig = digest.digest(pageName.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new InternalWikiException("AAAAGH!  UTF-8 is gone!  My eyes!  It burns...!");
        }
        return TextUtil.toHexString(dig) + ".cache";
    }

    /**
     *  Reads the serialized data from the disk back to memory.
     *  Returns the date when the data was last written on disk
     */
    private synchronized long unserializeAttrsFromDisk(WikiPage p) throws IOException, ClassNotFoundException {
        ObjectInputStream in = null;
        long saved = 0L;
        try {
            StopWatch sw = new StopWatch();
            sw.start();
            File f = new File(SERIALIZATION_DIR);
            f = new File(f, getHashFileName(p.getName()));
            com.madzone.gae.filem.model.File gaeFile = com.madzone.gae.filem.model.BinaryFile.getByName(f.getAbsolutePath());
            if (gaeFile == null) {
                return 0L;
            }
            log.log(Level.INFO, "Deserializing attributes for " + p.getName());
            in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(f)));
            long ver = in.readLong();
            if (ver != serialVersionUID) {
                log.log(Level.INFO, "File format has changed; cannot deserialize.");
                return 0L;
            }
            saved = in.readLong();
            String name = in.readUTF();
            if (!name.equals(p.getName())) {
                log.log(Level.INFO, "File name does not match (" + name + "), skipping...");
                return 0L;
            }
            long entries = in.readLong();
            for (int i = 0; i < entries; i++) {
                String key = in.readUTF();
                Object value = in.readObject();
                p.setAttribute(key, value);
                log.log(Level.INFO, "   attr: " + key + "=" + value);
            }
            in.close();
            sw.stop();
            log.log(Level.INFO, "Read serialized data for " + name + " successfully in " + sw);
            p.setHasMetadata();
        } catch (NoSuchAlgorithmException e) {
            log.log(Level.SEVERE, "No MD5!?!");
        } finally {
            if (in != null) in.close();
        }
        return saved;
    }

    /**
     *  Serializes hashmaps to disk.  The format is private, don't touch it.
     */
    private synchronized void serializeAttrsToDisk(WikiPage p) {
        ObjectOutputStream out = null;
        StopWatch sw = new StopWatch();
        sw.start();
        try {
            File f = new File(SERIALIZATION_DIR);
            f = new File(f, getHashFileName(p.getName()));
            Set entries = p.getAttributes().entrySet();
            if (entries.size() == 0) {
                return;
            }
            out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(f)));
            out.writeLong(serialVersionUID);
            out.writeLong(System.currentTimeMillis());
            out.writeUTF(p.getName());
            out.writeLong(entries.size());
            for (Iterator i = entries.iterator(); i.hasNext(); ) {
                Map.Entry e = (Map.Entry) i.next();
                if (e.getValue() instanceof Serializable) {
                    out.writeUTF((String) e.getKey());
                    out.writeObject(e.getValue());
                }
            }
            out.close();
        } catch (IOException e) {
            log.log(Level.SEVERE, "Unable to serialize!");
            try {
                if (out != null) out.close();
            } catch (IOException ex) {
            }
        } catch (NoSuchAlgorithmException e) {
            log.log(Level.SEVERE, "No MD5 algorithm!?!");
        } finally {
            sw.stop();
            log.log(Level.INFO, "serialization for " + p.getName() + " done - took " + sw);
        }
    }

    /**
     *  After the page has been saved, updates the reference lists.
     *  
     *  @param context {@inheritDoc}
     *  @param content {@inheritDoc}
     */
    public void postSave(WikiContext context, String content) {
        WikiPage page = context.getPage();
        updateReferences(page.getName(), context.getEngine().scanWikiLinks(page, content));
        serializeAttrsToDisk(page);
    }

    /**
     * Updates the m_referedTo and m_referredBy hashmaps when a page has been
     * deleted.
     * <P>
     * Within the m_refersTo map the pagename is a key. The whole key-value-set
     * has to be removed to keep the map clean.
     * Within the m_referredBy map the name is stored as a value. Since a key
     * can have more than one value we have to delete just the key-value-pair
     * referring page:deleted page.
     *
     *  @param page Name of the page to remove from the maps.
     */
    public synchronized void pageRemoved(WikiPage page) {
        String pageName = page.getName();
        pageRemoved(pageName);
    }

    private void pageRemoved(String pageName) {
        Collection<String> refTo = m_refersTo.get(pageName);
        if (refTo != null) {
            Iterator itRefTo = refTo.iterator();
            while (itRefTo.hasNext()) {
                String referredPageName = (String) itRefTo.next();
                Set<String> refBy = m_referredBy.get(referredPageName);
                if (refBy == null) throw new InternalWikiException("Refmgr out of sync: page " + pageName + " refers to " + referredPageName + ", which has null referrers.");
                refBy.remove(pageName);
                m_referredBy.remove(referredPageName);
                if (!(refBy.isEmpty() && !m_engine.pageExists(referredPageName))) {
                    m_referredBy.put(referredPageName, refBy);
                }
            }
            log.log(Level.INFO, "Removing from m_refersTo HashMap key:value " + pageName + ":" + m_refersTo.get(pageName));
            m_refersTo.remove(pageName);
        }
        Set<String> refBy = m_referredBy.get(pageName);
        if (refBy == null || refBy.isEmpty()) {
            m_referredBy.remove(pageName);
        }
        serializeToDisk();
        try {
            File f = new File(SERIALIZATION_DIR);
            f = new File(f, getHashFileName(pageName));
        } catch (NoSuchAlgorithmException e) {
            log.log(Level.SEVERE, "What do you mean - no such algorithm?", e);
        }
    }

    /**
     *  Updates the referred pages of a new or edited WikiPage. If a refersTo
     *  entry for this page already exists, it is removed and a new one is built
     *  from scratch. Also calls updateReferredBy() for each referenced page.
     *  <P>
     *  This is the method to call when a new page has been created and we
     *  want to a) set up its references and b) notify the referred pages
     *  of the references. Use this method during run-time.
     *
     *  @param page Name of the page to update.
     *  @param references A Collection of Strings, each one pointing to a page this page references.
     */
    public synchronized void updateReferences(String page, Collection references) {
        internalUpdateReferences(page, references);
        serializeToDisk();
    }

    /**
     *  Updates the referred pages of a new or edited WikiPage. If a refersTo
     *  entry for this page already exists, it is removed and a new one is built
     *  from scratch. Also calls updateReferredBy() for each referenced page.
     *  <p>
     *  This method does not synchronize the database to disk.
     *
     *  @param page Name of the page to update.
     *  @param references A Collection of Strings, each one pointing to a page this page references.
     */
    private void internalUpdateReferences(String page, Collection references) {
        page = getFinalPageName(page);
        Collection oldRefTo = m_refersTo.get(page);
        m_refersTo.remove(page);
        TreeSet<String> cleanedRefs = new TreeSet<String>();
        for (Iterator i = references.iterator(); i.hasNext(); ) {
            String ref = (String) i.next();
            ref = getFinalPageName(ref);
            cleanedRefs.add(ref);
        }
        m_refersTo.put(page, cleanedRefs);
        if (!m_referredBy.containsKey(page)) {
            m_referredBy.put(page, new TreeSet<String>());
        }
        cleanReferredBy(page, oldRefTo, cleanedRefs);
        Iterator<String> it = cleanedRefs.iterator();
        while (it.hasNext()) {
            String referredPageName = it.next();
            updateReferredBy(getFinalPageName(referredPageName), page);
        }
    }

    /**
     * Returns the refers-to list. For debugging.
     * 
     * @return The refers-to list.
     */
    protected Map getRefersTo() {
        return m_refersTo;
    }

    /**
     * Returns the referred-by list. For debugging.
     * 
     * @return Referred-by lists.
     */
    protected Map getReferredBy() {
        return m_referredBy;
    }

    /**
     * Cleans the 'referred by' list, removing references by 'referrer' to
     * any other page. Called after 'referrer' is removed.
     */
    private void cleanReferredBy(String referrer, Collection oldReferred, Collection<String> newReferred) {
        if (oldReferred == null) return;
        Iterator it = oldReferred.iterator();
        while (it.hasNext()) {
            String referredPage = (String) it.next();
            Set oldRefBy = m_referredBy.get(referredPage);
            if (oldRefBy != null) {
                oldRefBy.remove(referrer);
            }
            if (((oldRefBy == null) || (oldRefBy.isEmpty())) && (m_engine.pageExists(referredPage) == false)) {
                m_referredBy.remove(referredPage);
            }
        }
    }

    /**
     *  When initially building a ReferenceManager from scratch, call this method
     * BEFORE calling updateReferences() with a full list of existing page names.
     * It builds the refersTo and referredBy key lists, thus enabling
     * updateReferences() to function correctly.
     * <P>
     * This method should NEVER be called after initialization. It clears all mappings
     * from the reference tables.
     *
     * @param pages   a Collection containing WikiPage objects.
     */
    private synchronized void buildKeyLists(Collection pages) {
        m_refersTo.clear();
        m_referredBy.clear();
        if (pages == null) return;
        Iterator it = pages.iterator();
        try {
            while (it.hasNext()) {
                WikiPage page = (WikiPage) it.next();
                m_referredBy.put(page.getName(), new TreeSet<String>());
                m_refersTo.put(page.getName(), null);
            }
        } catch (ClassCastException e) {
            log.log(Level.SEVERE, "Invalid collection entry in ReferenceManager.buildKeyLists().", e);
        }
    }

    /**
     * Marks the page as referred to by the referrer. If the page does not
     * exist previously, nothing is done. (This means that some page, somewhere,
     * has a link to a page that does not exist.)
     * <P>
     * This method is NOT synchronized. It should only be referred to from
     * within a synchronized method, or it should be made synced if necessary.
     */
    private void updateReferredBy(String page, String referrer) {
        if (m_matchEnglishPlurals) {
            String p2 = page.endsWith("s") ? page.substring(0, page.length() - 1) : page + "s";
            if (referrer.equals(p2)) {
                return;
            }
        }
        Set<String> referrers = m_referredBy.get(page);
        if (referrers == null) {
            referrers = new TreeSet<String>();
            m_referredBy.put(page, referrers);
        }
        referrers.add(referrer);
    }

    /**
     * Clears the references to a certain page so it's no longer in the map.
     *
     * @param pagename  Name of the page to clear references for.
     */
    public synchronized void clearPageEntries(String pagename) {
        pagename = getFinalPageName(pagename);
        Collection<String> c = m_refersTo.get(pagename);
        if (c != null) {
            for (String key : c) {
                Collection<?> dref = m_referredBy.get(key);
                dref.remove(pagename);
            }
        }
        m_referredBy.remove(pagename);
        m_refersTo.remove(pagename);
    }

    /**
     *  Finds all unreferenced pages. This requires a linear scan through
     *  m_referredBy to locate keys with null or empty values.
     *  
     *  @return The Collection of Strings
     */
    public synchronized Collection findUnreferenced() {
        ArrayList<String> unref = new ArrayList<String>();
        for (String key : m_referredBy.keySet()) {
            Set<?> refs = getReferenceList(m_referredBy, key);
            if (refs == null || refs.isEmpty()) {
                unref.add(key);
            }
        }
        return unref;
    }

    /**
     * Finds all references to non-existant pages. This requires a linear
     * scan through m_refersTo values; each value must have a corresponding
     * key entry in the reference Maps, otherwise such a page has never
     * been created.
     * <P>
     * Returns a Collection containing Strings of unreferenced page names.
     * Each non-existant page name is shown only once - we don't return information
     * on who referred to it.
     * 
     * @return A Collection of Strings
     */
    public synchronized Collection findUncreated() {
        TreeSet<String> uncreated = new TreeSet<String>();
        Collection<Collection<String>> allReferences = m_refersTo.values();
        for (Collection<String> refs : allReferences) {
            if (refs != null) {
                for (String aReference : refs) {
                    if (m_engine.pageExists(aReference) == false) {
                        uncreated.add(aReference);
                    }
                }
            }
        }
        return uncreated;
    }

    /**
     *  Searches for the given page in the given Map, and returns
     *  the set of references.  This method also takes care of English plural
     *  matching.
     *  
     *  @param coll The Map to search in
     *  @param pagename The name to find.
     *  @return The references list.
     */
    private <T> Set<T> getReferenceList(Map<String, Set<T>> coll, String pagename) {
        Set<T> refs = coll.get(pagename);
        if (m_matchEnglishPlurals) {
            Set<T> refs2;
            if (pagename.endsWith("s")) {
                refs2 = coll.get(pagename.substring(0, pagename.length() - 1));
            } else {
                refs2 = coll.get(pagename + "s");
            }
            if (refs2 != null) {
                if (refs != null) refs.addAll(refs2); else refs = refs2;
            }
        }
        return refs;
    }

    public synchronized Collection findReferrers(String pagename) {
        Set<String> refs = getReferenceList(m_referredBy, pagename);
        if (refs == null || refs.isEmpty()) {
            return null;
        }
        return refs;
    }

    /**
     *  Returns all pages that refer to this page.  Note that this method
     *  returns an unmodifiable Map, which may be abruptly changed.  So any
     *  access to any iterator may result in a ConcurrentModificationException.
     *  <p>
     *  The advantages of using this method over findReferrers() is that
     *  it is very fast, as it does not create a new object.  The disadvantages
     *  are that it does not do any mapping between plural names, and you
     *  may end up getting a ConcurrentModificationException.
     *
     * @param pageName Page name to query.
     * @return A Set of Strings containing the names of all the pages that refer
     *         to this page.  May return null, if the page does not exist or
     *         has not been indexed yet.
     * @since 2.2.33
     */
    public Set findReferredBy(String pageName) {
        return m_unmutableReferredBy.get(getFinalPageName(pageName));
    }

    /**
     *  Returns all pages that this page refers to.  You can use this as a quick
     *  way of getting the links from a page, but note that it does not link any
     *  InterWiki, image, or external links.  It does contain attachments, though.
     *  <p>
     *  The Collection returned is unmutable, so you cannot change it.  It does reflect
     *  the current status and thus is a live object.  So, if you are using any
     *  kind of an iterator on it, be prepared for ConcurrentModificationExceptions.
     *  <p>
     *  The returned value is a Collection, because a page may refer to another page
     *  multiple times.
     *
     * @param pageName Page name to query
     * @return A Collection of Strings containing the names of the pages that this page
     *         refers to. May return null, if the page does not exist or has not
     *         been indexed yet.
     * @since 2.2.33
     */
    public Collection findRefersTo(String pageName) {
        return m_unmutableRefersTo.get(getFinalPageName(pageName));
    }

    public int deepHashCode() {
        boolean failed = true;
        int signature = 0;
        while (failed) {
            signature = 0;
            try {
                signature ^= m_referredBy.hashCode();
                signature ^= m_refersTo.hashCode();
                failed = false;
            } catch (ConcurrentModificationException e) {
                Thread.yield();
            }
        }
        return signature;
    }

    /**
     *  Returns a list of all pages that the ReferenceManager knows about.
     *  This should be roughly equivalent to PageManager.getAllPages(), but without
     *  the potential disk access overhead.  Note that this method is not guaranteed
     *  to return a Set of really all pages (especially during startup), but it is
     *  very fast.
     *
     *  @return A Set of all defined page names that ReferenceManager knows about.
     *  @since 2.3.24
     */
    public Set findCreated() {
        return new HashSet<String>(m_refersTo.keySet());
    }

    private String getFinalPageName(String orig) {
        try {
            String s = m_engine.getFinalPageName(orig);
            if (s == null) s = orig;
            return s;
        } catch (ProviderException e) {
            log.log(Level.SEVERE, "Error while trying to fetch a page name; trying to cope with the situation.", e);
            return orig;
        }
    }

    /**
     *  {@inheritDoc}
     */
    public void actionPerformed(WikiEvent event) {
        if ((event instanceof WikiPageEvent) && (event.getType() == WikiPageEvent.PAGE_DELETED)) {
            String pageName = ((WikiPageEvent) event).getPageName();
            if (pageName != null) {
                pageRemoved(pageName);
            }
        }
    }
}
