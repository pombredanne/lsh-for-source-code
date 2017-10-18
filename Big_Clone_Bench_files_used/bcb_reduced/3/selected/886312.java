package org.omegat.core;

import java.util.TreeMap;

/**
 * An entry in the index
 *
 * @author Keith Godfrey
 */
public class IndexEntry extends Object {

    public IndexEntry(String wrd) {
        m_refTree = new TreeMap();
    }

    public TreeMap getTreeMap() {
        return (TreeMap) m_refTree.clone();
    }

    public void addReference(StringEntry ref) {
        String s = String.valueOf(ref.digest());
        if (!m_refTree.containsKey(s)) m_refTree.put(s, ref);
    }

    private TreeMap m_refTree;
}
