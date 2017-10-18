import java.util.*;

class IndexEntry extends Object {

    public IndexEntry(String wrd) {
        m_word = wrd;
        m_refTree = new TreeMap();
    }

    public TreeMap getTreeMap() {
        return (TreeMap) m_refTree.clone();
    }

    public void addReference(StringEntry ref) {
        String s = String.valueOf(ref.digest());
        if (!m_refTree.containsKey(s)) m_refTree.put(s, ref);
    }

    public String getWord() {
        return m_word;
    }

    private TreeMap m_refTree;

    private String m_word;
}
