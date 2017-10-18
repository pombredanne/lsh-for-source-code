package org.lindenb.tool.cloneit;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import org.lindenb.bio.GeneticCode;
import org.lindenb.bio.NucleotideUtils;
import org.lindenb.io.IOUtils;
import org.lindenb.lang.ThrowablePane;
import org.lindenb.swing.table.AbstractGenericTableModel;
import org.lindenb.util.Algorithms;
import org.lindenb.util.Assert;
import org.lindenb.util.XObject;
import org.lindenb.util.iterator.AbstractIterator;
import org.lindenb.util.iterator.ExtendedIterator;
import org.lindenb.util.iterator.FilterIterator;
import org.lindenb.util.iterator.YIterator;

enum Orientation {

    Forward, Reverse
}

enum CutType {

    BLUNT, OVERHANG_3, OVERHANG_5
}

/**
 * Enzyme
 * @author pierre
 *
 */
class Enzyme extends XObject implements Comparable<Enzyme> {

    private String name;

    private String fullSite;

    private String site;

    private String provider;

    private int pos3_5;

    private int pos5_3;

    private boolean palindromic = false;

    public Enzyme(String name, String fullSite, String provider) {
        this.name = name;
        this.fullSite = fullSite;
        this.site = "";
        this.provider = provider;
        this.palindromic = fullSite.indexOf("^") != -1;
        if (this.palindromic) {
            this.pos5_3 = fullSite.indexOf("^");
            this.site = fullSite.substring(0, this.pos5_3).toUpperCase() + fullSite.substring(this.pos5_3 + 1).toUpperCase();
            this.pos3_5 = this.site.length() - this.pos5_3;
        } else {
            int var_parenthese_gauche = this.fullSite.indexOf('(');
            int var_slash = this.fullSite.indexOf('/', var_parenthese_gauche + 1);
            int var_parenthese_droite = fullSite.indexOf(')', var_slash + 1);
            this.site = this.fullSite.substring(0, var_parenthese_gauche).toUpperCase();
            this.pos5_3 = this.site.length() + Integer.parseInt(fullSite.substring(var_parenthese_gauche + 1, var_slash));
            this.pos3_5 = this.site.length() + Integer.parseInt(fullSite.substring(var_slash + 1, var_parenthese_droite));
            while (this.site.length() < Math.max(this.pos5_3, this.pos3_5)) {
                this.site += "N";
            }
        }
    }

    public boolean isPalindromic() {
        return this.palindromic;
    }

    public int size() {
        return this.getSite().length();
    }

    public String getProviders() {
        return provider;
    }

    public String getName() {
        return name;
    }

    public String getSite() {
        return site;
    }

    public String getFullSite() {
        return fullSite;
    }

    public int compareTo(Enzyme o) {
        if (o == this) return 0;
        return getName().compareTo(o.getName());
    }

    public String getURL() {
        StringBuilder b = new StringBuilder("http://rebase.neb.com?");
        b.append(getName().replaceAll("[ ]", ""));
        b.append(".html");
        return b.toString();
    }

    public char getCharAt(int pos, Orientation sens) {
        if (sens.equals(Orientation.Forward)) {
            return (getSite().charAt(pos));
        }
        return complementary(getSite().charAt((size() - 1) - pos));
    }

    public int getPos3_5(Orientation sens) {
        return (sens.equals(Orientation.Forward) ? pos3_5 : (size() - 1) - pos5_3);
    }

    public int getPos5_3(Orientation sens) {
        return (sens.equals(Orientation.Forward) ? pos5_3 : (size() - 1) - pos5_3);
    }

    public int getOverhang(Orientation sens) {
        return (getPos5_3(sens) - getPos3_5(sens));
    }

    public int getOverhang() {
        return (getOverhang(Orientation.Forward) < 0 ? getOverhang(Orientation.Reverse) : getOverhang(Orientation.Forward));
    }

    public boolean isSiteDegenerate() {
        for (char c : new char[] { 'N', 'Y', 'R', 'M', 'K', 'S', 'W', 'B', 'D', 'H', 'V' }) {
            if (getSite().indexOf(c) != -1) return true;
        }
        return false;
    }

    public CutType getType() {
        if (pos3_5 == pos5_3) return (CutType.BLUNT); else if (pos3_5 < pos5_3) return (CutType.OVERHANG_3);
        return (CutType.OVERHANG_5);
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }

    @Override
    public int hashCode() {
        return getFullSite().hashCode();
    }

    @Override
    public String toString() {
        return getName().toString() + "[" + getFullSite() + "]";
    }

    public float getWeight() {
        float n = 0f;
        for (int i = 0; i < size(); ++i) {
            switch(Character.toLowerCase(getSite().charAt(i))) {
                case 'a':
                case 't':
                case 'g':
                case 'c':
                    n += 1f;
                    break;
                case 'y':
                case 'r':
                case 'm':
                case 'k':
                case 's':
                case 'w':
                    n += 0.5f;
                    break;
                case 'b':
                case 'd':
                case 'h':
                case 'v':
                    n += 0.25;
                    break;
                case 'n':
                    n += 0f;
                    break;
                default:
                    Assert.assertUnreachableStatement();
                    break;
            }
        }
        return n;
    }

    public static boolean DNAcmp(char plasmid, char b) {
        b = Character.toUpperCase(b);
        switch(Character.toUpperCase(plasmid)) {
            case 'A':
                return "ANDHVMWR".indexOf(b) != -1;
            case 'T':
                return "TNYBDHKW".indexOf(b) != -1;
            case 'G':
                return "GNRBDVKS".indexOf(b) != -1;
            case 'C':
                return "CNYBHVMS".indexOf(b) != -1;
            case 'N':
                return false;
            default:
                return false;
        }
    }

    private static char complementary(char c) {
        switch(Character.toLowerCase(c)) {
            case 'a':
                return 'T';
            case 't':
                return 'A';
            case 'g':
                return 'C';
            case 'c':
                return 'G';
            case 'n':
                return 'N';
            case 'y':
                return 'R';
            case 'r':
                return 'Y';
            case 'm':
                return 'K';
            case 'k':
                return 'M';
            case 's':
                return 'S';
            case 'w':
                return 'W';
            case 'b':
                return 'V';
            case 'd':
                return 'H';
            case 'h':
                return 'D';
            case 'v':
                return 'B';
            case '*':
                return '*';
            default:
                return '?';
        }
    }
}

class IntegerField extends JTextField {

    private static final long serialVersionUID = 1L;

    IntegerField() {
        super();
        setDocument(new PlainDocument() {

            private static final long serialVersionUID = 1L;

            @Override
            public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
                StringBuilder b = new StringBuilder(str.length());
                for (int i = 0; i < str.length(); ++i) {
                    if (!Character.isDigit(str.charAt(i))) continue;
                    b.append(str.charAt(i));
                }
                super.insertString(offs, b.toString(), a);
            }
        });
        setText("0");
    }

    public int getValue() {
        return Integer.parseInt(getText());
    }

    public void setValue(int v) {
        setText(String.valueOf(v));
    }
}

interface EnzymeSelector {

    public boolean accept(Enzyme e);
}

class EnzymeList extends AbstractGenericTableModel<Enzyme> implements Iterable<Enzyme>, Cloneable {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    static final String COLS[] = { "Use", "Name", "Site" };

    private Vector<Enzyme> enzymes = new Vector<Enzyme>();

    private HashMap<Enzyme, Boolean> enzymeUsage = new HashMap<Enzyme, Boolean>();

    private HashMap<String, Enzyme> site2enzyme = new HashMap<String, Enzyme>();

    private HashMap<String, TreeSet<Enzyme>> site2isoschyzomers = new HashMap<String, TreeSet<Enzyme>>();

    private static final String RE_BASE[] = { "AarI", "CACCTGC(4/8)", "AatII", "GACGT^C", "Acc36I", "ACCTGC(4/8)", "Acc65I", "G^GTACC", "AccBSI", "CCGCTC(-3/-3)", "AccI", "GT^MKAC", "AccII", "CG^CG", "AciI", "CCGC(-3/-1)", "AcsI", "R^AATTY", "AfaI", "GT^AC", "AflII", "C^TTAAG", "AflIII", "A^CRYGT", "AluI", "AG^CT", "Alw21I", "GWGCW^C", "Alw26I", "GTCTC(1/5)", "AlwNI", "CAGNNN^CTG", "Aor51HI", "AGC^GCT", "ApaI", "GGGCC^C", "ApaLI", "G^TGCAC", "AscI", "GG^CGCGCC", "AvaI", "C^YCGRG", "AvaII", "G^GWCC", "AviII", "TGC^GCA", "BalI", "TGG^CCA", "BamHI", "G^GATCC", "BanII", "GRGCY^C", "BbeI", "GGCGC^C", "BbvCI", "CCTCAGC(-5/-2)", "BceAI", "ACGGC(12/14)", "BcnI", "CC^SGG", "BfiI", "ACTGGG(5/4)", "BfrBI", "ATG^CAT", "BfuI", "GTATCC(6/5)", "BglI", "GCCNNNN^NGGC", "BglII", "A^GATCT", "BlnI", "C^CTAGG", "BpiI", "GAAGAC(2/6)", "Bpu10I", "CCTNAGC(-5/-2)", "Bpu1102I", "GC^TNAGC", "BsaWI", "W^CCGGW", "BscBI", "GGN^NCC", "BseDI", "C^CNNGG", "BseGI", "GGATG(2/0)", "BseMI", "GCAATG(2/0)", "BseMII", "CTCAG(10/8)", "BseNI", "ACTGG(1/-1)", "BseRI", "GAGGAG(10/8)", "BseSI", "GKGCM^C", "BseXI", "GCAGC(8/12)", "BsgI", "GTGCAG(16/14)", "Bsh1285I", "CGRY^CG", "Bsh1365I", "GATNN^NNATC", "BsiYI", "CCNNNNN^NNGG", "BsmFI", "GGGAC(10/14)", "BsmI", "GAATGC(1/-1)", "Bsp120I", "G^GGCCC", "Bsp1286I", "GDGCH^C", "Bsp1407I", "T^GTACA", "BspPI", "GGATC(4/5)", "BssHII", "G^CGCGC", "BssKI", "^CCNGG", "Bst1107I", "GTA^TAC", "Bst2BI", "CACGAG(-5/-1)", "BstAPI", "GCANNNN^NTGC", "BstBAI", "YAC^GTR", "BstDSI", "C^CRYGG", "BstSFI", "C^TRYAG", "BstXI", "CCANNNNN^NTGG", "BtrI", "CACGTC(-3/-3)", "BtsI", "GCAGTG(2/0)", "Cac8I", "GCN^NGC", "Cfr10I", "R^CCGGY", "Cfr13I", "G^GNCC", "ClaI", "AT^CGAT", "CpoI", "CG^GWCCG", "Csp6I", "G^TAC", "CviTI", "RG^CY", "DdeI", "C^TNAG", "DpnI", "GA^TC", "DraI", "TTT^AAA", "DraIII", "CACNNN^GTG", "DseDI", "GACNNNN^NNGTC", "EaeI", "Y^GGCCR", "Eam1104I", "CTCTTC(1/4)", "Eam1105I", "GACNNN^NNGTC", "EciI", "GGCGGA(11/9)", "Ecl136II", "GAG^CTC", "Eco31I", "GGTCTC(1/5)", "Eco52I", "C^GGCCG", "Eco57I", "CTGAAG(16/14)", "Eco57MI", "CTGRAG(16/14)", "Eco64I", "G^GYRCC", "Eco81I", "CC^TNAGG", "EcoO109I", "RG^GNCCY", "EcoO65I", "G^GTNACC", "EcoRI", "G^AATTC", "EcoRII", "^CCWGG", "EcoRV", "GAT^ATC", "EcoT14I", "C^CWWGG", "EcoT22I", "ATGCA^T", "EheI", "GGC^GCC", "Esp3I", "CGTCTC(1/5)", "FbaI", "T^GATCA", "FokI", "GGATG(9/13)", "FseI", "GGCCGG^CC", "FspAI", "RTGC^GCAY", "GsuI", "CTGGAG(16/14)", "HaeII", "RGCGC^Y", "HaeIII", "GG^CC", "HgaI", "GACGC(5/10)", "HhaI", "GCG^C", "Hin6I", "G^CGC", "HincII", "GTY^RAC", "HindIII", "A^AGCTT", "HinfI", "G^ANTC", "HpaI", "GTT^AAC", "HphI", "GGTGA(8/7)", "Hpy188I", "TCN^GA", "Hpy188III", "TC^NNGA", "Hpy8I", "GTN^NAC", "Hpy99I", "CGWCG^", "HpyF44III", "TG^CA", "KasI", "G^GCGCC", "Kpn2I", "T^CCGGA", "KpnI", "GGTAC^C", "LweI", "GCATC(5/9)", "MabI", "A^CCWGGT", "MaeII", "A^CGT", "MaeIII", "^GTNAC", "MboI", "^GATC", "MboII", "GAAGA(8/7)", "MflI", "R^GATCY", "MluI", "A^CGCGT", "MnlI", "CCTC(7/6)", "MseI", "T^TAA", "Msp17I", "GR^CGYC", "MspA1I", "CMG^CKG", "MspI", "C^CGG", "MssI", "GTTT^AAAC", "MunI", "C^AATTG", "MvaI", "CC^WGG", "MwoI", "GCNNNNN^NNGC", "NaeI", "GCC^GGC", "NarI", "GG^CGCC", "NcoI", "C^CATGG", "NdeI", "CA^TATG", "NgoAIV", "G^CCGGC", "NheI", "G^CTAGC", "NlaIII", "CATG^", "NmuCI", "^GTSAC", "NotI", "GC^GGCCGC", "NruI", "TCG^CGA", "NspI", "RCATG^Y", "NspV", "TT^CGAA", "OliI", "CACNN^NNGTG", "PacI", "TTAAT^TAA", "PciI", "A^CATGT", "PinAI", "A^CCGGT", "PmaCI", "CAC^GTG", "PpsI", "GAGTC(4/5)", "Ppu10I", "A^TGCAT", "PshAI", "GACNN^NNGTC", "PsiI", "TTA^TAA", "Psp1406I", "AA^CGTT", "Psp5II", "RG^GWCCY", "PstI", "CTGCA^G", "PvuI", "CGAT^CG", "PvuII", "CAG^CTG", "RcaI", "T^CATGA", "SacI", "GAGCT^C", "SalI", "G^TCGAC", "SanDI", "GG^GWCCC", "SapI", "GCTCTTC(1/4)", "SatI", "GC^NGC", "ScaI", "AGT^ACT", "SchI", "GAGTC(5/5)", "ScrFI", "CC^NGG", "SfiI", "GGCCNNNN^NGGCC", "SgfI", "GCGAT^CGC", "SgrAI", "CR^CCGGYG", "SmaI", "CCC^GGG", "SmiI", "ATTT^AAAT", "SmiMI", "CAYNN^NNRTG", "SmlI", "C^TYRAG", "SmuI", "CCCGC(4/6)", "SnaBI", "TAC^GTA", "SpeI", "A^CTAGT", "SphI", "GCATG^C", "SrfI", "GCCC^GGGC", "Sse8387I", "CCTGCA^GG", "Sse9I", "^AATT", "SspI", "AAT^ATT", "SstII", "CCGC^GG", "StuI", "AGG^CCT", "SunI", "C^GTACG", "TaaI", "ACN^GT", "TaiI", "ACGT^", "TaqI", "T^CGA", "TatI", "W^GTACW", "TauI", "GCSG^C", "TfiI", "G^AWTC", "TseI", "G^CWGC", "TspRI", "CASTGNN^", "Tth111I", "GACN^NNGTC", "Van91I", "CCANNNN^NTGG", "VspI", "AT^TAAT", "XagI", "CCTNN^NNNAGG", "XbaI", "T^CTAGA", "XcmI", "CCANNNNN^NNNNTGG", "XhoI", "C^TCGAG", "XmaI", "C^CCGGG", "XmnI", "GAANN^NNTTC", "XspI", "C^TAG" };

    public static EnzymeList newEnzymeList() {
        EnzymeList L = new EnzymeList();
        for (int i = 0; i < RE_BASE.length; i += 2) {
            Enzyme e = new Enzyme(RE_BASE[i + 0], RE_BASE[i + 1], "");
            L.enzymes.add(e);
            L.enzymeUsage.put(e, true);
        }
        return L;
    }

    public EnzymeList() {
    }

    @SuppressWarnings("unchecked")
    EnzymeList(EnzymeList cp) {
        this();
        this.enzymes = (Vector<Enzyme>) cp.enzymes.clone();
        this.enzymeUsage = (HashMap<Enzyme, Boolean>) cp.enzymeUsage.clone();
    }

    @Override
    public Enzyme elementAt(int rowIndex) {
        return this.enzymes.elementAt(rowIndex);
    }

    public Enzyme getEnzymeAt(int index) {
        return elementAt(index);
    }

    public void addEnzyme(Enzyme e) {
        TreeSet<Enzyme> set = this.site2isoschyzomers.get(e.getFullSite());
        if (set == null) {
            set = new TreeSet<Enzyme>();
            this.site2isoschyzomers.put(e.getFullSite(), set);
        }
        set.add(e);
        Enzyme old = this.site2enzyme.get(e.getFullSite());
        if (old != null) {
            if (old.getProviders().length() < e.getProviders().length()) {
                int index = this.enzymes.indexOf(old);
                this.enzymes.setElementAt(e, index);
                this.site2enzyme.put(e.getFullSite(), e);
                fireTableRowsUpdated(index, index);
            }
            return;
        }
        this.enzymes.add(e);
        this.site2enzyme.put(e.getFullSite(), e);
        fireTableRowsInserted(getEnzymeCount() - 1, getEnzymeCount() - 1);
    }

    public int getEnzymeCount() {
        return this.enzymes.size();
    }

    @Override
    public String getColumnName(int column) {
        return COLS[column];
    }

    public int getColumnCount() {
        return COLS.length;
    }

    public int getRowCount() {
        return getEnzymeCount();
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == 0;
    }

    @Override
    public Class<?> getColumnClass(int column) {
        switch(column) {
            case 0:
                return Boolean.class;
            case 1:
                return String.class;
            case 2:
                return String.class;
        }
        return null;
    }

    @Override
    public Object getValueOf(Enzyme e, int column) {
        switch(column) {
            case 0:
                return isEnzymeEnabled(e);
            case 1:
                return e.getName();
            case 2:
                return e.getFullSite();
        }
        return null;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (columnIndex == 0) {
            Boolean b = (Boolean) aValue;
            setEnzymeEnabled(getEnzymeAt(rowIndex), b);
            fireTableCellUpdated(rowIndex, columnIndex);
        }
    }

    public boolean isEnzymeEnabled(Enzyme e) {
        return enzymeUsage.get(e);
    }

    public void setEnzymeEnabled(Enzyme e, boolean enabled) {
        enzymeUsage.put(e, enabled);
    }

    public Iterator<Enzyme> iterator() {
        return enzymes.iterator();
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return new EnzymeList(this);
    }

    public static EnzymeList read(BufferedReader in) throws IOException {
        EnzymeList el = new EnzymeList();
        String line;
        String name = null, site = null;
        while ((line = in.readLine()) != null) {
            if (!line.startsWith("<") || line.length() < 3 || line.charAt(2) != '>') continue;
            switch(line.charAt(1)) {
                case '1':
                    name = line.substring(3).trim();
                    break;
                case '5':
                    site = line.substring(3).trim();
                    break;
                case '7':
                    {
                        String provider = line.substring(3).trim();
                        int n = site.indexOf("^");
                        if (n != -1 && site.indexOf("^", n + 1) == -1 && provider.length() > 0) {
                            Enzyme e = new Enzyme(name, site, provider);
                            el.addEnzyme(e);
                        }
                        name = null;
                        site = null;
                        break;
                    }
            }
        }
        return el;
    }
}

/**
 * Site
 * @author pierre
 *
 */
class Site extends XObject implements Comparable<Site> {

    private Plasmid seq;

    private Enzyme enzyme;

    private int loc;

    private Orientation orient;

    public Site(Plasmid seq, Enzyme enz, int loc, Orientation orient) {
        this.seq = seq;
        this.enzyme = enz;
        this.loc = loc;
        this.orient = orient;
    }

    public Enzyme getEnzyme() {
        return enzyme;
    }

    public Plasmid getSequence() {
        return seq;
    }

    public Orientation getOrientation() {
        return orient;
    }

    public int getPosition() {
        return this.loc;
    }

    public int getPos5_3() {
        return this.getPosition() + getEnzyme().getPos5_3(getOrientation());
    }

    public int getPos3_5() {
        return this.getPosition() + getEnzyme().getPos3_5(getOrientation());
    }

    public int end() {
        return getPosition() + getEnzyme().size();
    }

    public int compareTo(Site o) {
        assert (o != null);
        if (o == this) return 0;
        assert (o.getSequence() == getSequence());
        return getPosition() - o.getPosition();
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this;
    }

    @Override
    public int hashCode() {
        return getPosition();
    }

    @Override
    public String toString() {
        return getEnzyme().toString() + " on " + getSequence().getName() + " at " + getPosition();
    }

    public char relativeAt(int index) {
        return getSequence().at(getPosition() + index);
    }
}

class IndexIterator implements Iterator<Integer> {

    private int cur;

    private int end;

    public IndexIterator(int beg, int end) {
        this.cur = beg;
        this.end = end;
        if (this.cur > this.end) throw new IllegalArgumentException();
    }

    @Override
    public boolean hasNext() {
        return cur < end;
    }

    @Override
    public Integer next() {
        return cur++;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}

/**
 * SiteList
 * @author pierre
 *
 */
class SiteList implements Iterable<Site> {

    private static final Algorithms<Site, Integer> SORT_BY_LOC = new Algorithms<Site, Integer>(new Comparator<Integer>() {

        public int compare(Integer o1, Integer o2) {
            return o1.compareTo(o1);
        }
    }) {

        @Override
        public Integer getKey(Site value) {
            return value.getPosition();
        }
    };

    private Vector<Site> sites = new Vector<Site>(1000, 100);

    public SiteList() {
    }

    public void addSite(Site site) {
        this.sites.addElement(site);
    }

    public void clear() {
        this.sites.clear();
    }

    public void sort() {
        Collections.sort(this.sites);
    }

    private int lower_bound(int position) {
        return SORT_BY_LOC.lower_bound(this.sites, position);
    }

    private int upper_bound(int position) {
        return SORT_BY_LOC.upper_bound(this.sites, position);
    }

    public IndexIterator listSitesIn(int start, int end) {
        return new IndexIterator(lower_bound(start), upper_bound(end));
    }

    public AbstractIterator<Integer> listSitesOut(int start, int end) {
        return new YIterator<Integer>(new IndexIterator(0, lower_bound(start)), new IndexIterator(upper_bound(end), this.sites.size()));
    }

    public ExtendedIterator<Integer> listSitesOut(Enzyme e, int start, int end) {
        return new FilterIterator<Integer>(listSitesOut(start, end), e) {

            @Override
            public boolean accept(Integer index) {
                return getSiteAt(index).getEnzyme().equals(getUserData());
            }
        };
    }

    public ExtendedIterator<Integer> listSitesIn(Enzyme e, int start, int end) {
        return new FilterIterator<Integer>(listSitesIn(start, end), e) {

            @Override
            public boolean accept(Integer index) {
                return getSiteAt(index).getEnzyme().equals(getUserData());
            }
        };
    }

    public Site getSiteAt(int i) {
        return this.sites.elementAt(i);
    }

    public int getSiteCount() {
        return this.sites.size();
    }

    public Iterator<Site> iterator() {
        return sites.iterator();
    }

    /** remove sites with enzyme in 'set' */
    public void removeEnzymes(Set<Enzyme> enzToRemove) {
        int j = 0;
        for (int i = 0; i < this.sites.size(); ++i) {
            if (!enzToRemove.contains(getSiteAt(i).getEnzyme())) {
                this.sites.setElementAt(this.sites.elementAt(i), j);
                j++;
            }
        }
        sites.setSize(j);
    }
}

enum Polymerase {

    NO_TREATMENT, POLYMERASE
}

class SiteUsage {

    private Site site;

    private Polymerase polymerase;

    public SiteUsage(Site site, Polymerase polymerase) {
        this.site = site;
        this.polymerase = polymerase;
    }

    public Polymerase getPolymerase() {
        return polymerase;
    }

    public Plasmid getSequence() {
        return getSite().getSequence();
    }

    public int getPosition() {
        return getSite().getPosition();
    }

    public Enzyme getEnzyme() {
        return getSite().getEnzyme();
    }

    public Site getSite() {
        return site;
    }

    @Override
    public String toString() {
        return getSite().toString() + (polymerase == Polymerase.NO_TREATMENT ? "" : " Treated with a Polymerase.");
    }

    String print() {
        StringBuilder lines[] = new StringBuilder[3];
        for (int i = 0; i < lines.length; ++i) lines[i] = new StringBuilder();
        for (int i = -10; i <= getSite().getEnzyme().size() + 10; ++i) {
            char c = getSite().relativeAt(i);
            if (i > 0 || i >= getSite().getEnzyme().size()) c = Character.toLowerCase(c);
            lines[0].append(c);
            lines[1].append(NucleotideUtils.complement(c));
        }
        return lines[0].append("\n").append(lines[1]).append("\n").append(lines[2]).append("\n").toString();
    }
}

class HemiStrategy {

    protected SiteUsage siteUsages[] = new SiteUsage[2];

    protected boolean frameMatter;

    public HemiStrategy(SiteUsage s1, SiteUsage s2, boolean frameMatter) {
        siteUsages[0] = s1;
        siteUsages[1] = s2;
        this.frameMatter = frameMatter;
    }

    public int leftOverhang() {
        return left5_3() - left3_5();
    }

    public int rightOverhang() {
        return right5_3() - right3_5();
    }

    public int left5_3() {
        return priv_5_3(0);
    }

    public int right5_3() {
        return priv_5_3(1);
    }

    public int left3_5() {
        return priv_3_5(0);
    }

    public int right3_5() {
        return priv_3_5(1);
    }

    private int priv_5_3(int side) {
        if (siteUsages[side].getPolymerase() == Polymerase.NO_TREATMENT) {
            Site s = siteUsages[side].getSite();
            return s.getPosition() + s.getEnzyme().getPos5_3(s.getOrientation());
        } else {
            return priv_polymerase(side);
        }
    }

    private int priv_3_5(int side) {
        if (siteUsages[side].getPolymerase() == Polymerase.NO_TREATMENT) {
            Site s = siteUsages[side].getSite();
            return s.getPosition() + s.getEnzyme().getPos3_5(s.getOrientation());
        } else {
            return priv_polymerase(side);
        }
    }

    private int priv_polymerase(int side) {
        Site s = siteUsages[side].getSite();
        if (side == 0) {
            return s.getPosition() + s.getEnzyme().getPos3_5(s.getOrientation());
        } else {
            return s.getPosition() + s.getEnzyme().getPos5_3(s.getOrientation());
        }
    }

    public static boolean Fct_Identique(char a, char b) {
        return a == b;
    }

    public boolean isValid() {
        if (!Fct_Compatible2(this.siteUsages[0].getSite().getSequence(), left5_3(), left3_5(), this.siteUsages[1].getSite().getSequence(), right5_3(), right3_5())) return false;
        if (this.frameMatter) {
            return this.siteUsages[0].getSite().getSequence().getFrameAt(left5_3()) == this.siteUsages[1].getSite().getSequence().getFrameAt(right5_3());
        }
        return true;
    }

    static boolean Fct_Compatible2(Plasmid SeqA, int varA_5_3, int varA_3_5, Plasmid SeqB, int varB_5_3, int varB_3_5) {
        int overhang = (varA_5_3 - varA_3_5);
        if ((overhang != (varB_5_3 - varB_3_5))) {
            return false;
        } else if (overhang == 0) {
            return true;
        } else if (overhang > 0) {
            for (int i = 0; i < overhang; i++) {
                if (!Fct_Identique(SeqA.at(varA_3_5 + i), SeqB.at(varB_3_5 + i))) {
                    return false;
                }
            }
            return true;
        } else {
            for (int i = 0; i < -overhang; i++) {
                if (!Fct_Identique(SeqA.at(varA_5_3 + i), SeqB.at(varB_5_3 + i))) return false;
            }
            return true;
        }
    }
}

class Strategy implements Comparable<Strategy> {

    private HemiStrategy hemiStgy[] = new HemiStrategy[2];

    public Strategy(HemiStrategy s1, HemiStrategy s2) {
        hemiStgy[0] = s1;
        hemiStgy[1] = s2;
    }

    @Override
    public int compareTo(Strategy o) {
        return 0;
    }

    /**
	Display the sequence from site_5 to site_3
	where the cuting site of an enzyme will be found a sign '/' will be printed
	if the sequence position is in FRAME a sign '.' will be printed
	if the sequence position is in FRAME the translation codon will be printed
	****/
    private void print(PrintWriter out, SiteUsage site_5, SiteUsage site_3) {
        StringBuilder lines[] = new StringBuilder[] { new StringBuilder(), new StringBuilder(), new StringBuilder() };
        Plasmid sequence = site_5.getSequence();
        Assert.assertTrue(site_5.getSequence() == site_3.getSequence());
        out.print("  Digest " + sequence + " with ");
        out.print(site_5.getEnzyme() + " (" + site_5.getPosition());
        if (site_5.getEnzyme() != site_3.getEnzyme()) {
            out.print(") and ");
            out.print(site_3.getEnzyme() + " (" + site_3.getPosition() + ")");
        } else {
            if (site_5.getSite() == site_3.getSite()) {
                out.print(")");
            } else {
                out.print(").\n");
            }
        }
        lines[0].append("  5'  --");
        lines[1].append("  3'  --");
        lines[2].append("  NH2   ");
        for (int i = site_5.getPosition() - 4; i <= site_3.getPosition() + site_3.getEnzyme().size() + 4; ++i) {
            if (site_3.getPosition() - (site_5.getPosition() + site_5.getEnzyme().size()) > 50 && i == site_5.getPosition() + site_5.getEnzyme().size() + 10) {
                i = site_3.getPosition() - 10;
                lines[0].append("--  --");
                lines[1].append("--  --");
                lines[2].append("--  --");
                continue;
            }
            if (sequence.isInFrame(i)) {
                lines[0].append(".");
                lines[1].append(".");
                lines[2].append(" " + GeneticCode.getStandard().translate(sequence.at(i), sequence.at(i + 1), sequence.at(i + 2)));
            } else {
                lines[2].append(" ");
            }
            if (i == site_5.getSite().getPos5_3() && i == site_5.getSite().getPos3_5()) {
                lines[0].append("/");
                lines[1].append("/");
                lines[2].append(" ");
            } else if (i == site_5.getSite().getPos5_3()) {
                lines[0].append("/");
                lines[1].append(" ");
                lines[2].append(" ");
            } else if (i == site_5.getSite().getPos3_5()) {
                lines[0].append(" ");
                lines[1].append("/");
                lines[2].append(" ");
            }
            if (i == site_3.getSite().getPos5_3() && i == site_3.getSite().getPos3_5()) {
                lines[0].append("/");
                lines[1].append("/");
                lines[2].append(" ");
            } else if (i == site_3.getSite().getPos5_3()) {
                lines[0].append("/");
                lines[1].append(" ");
                lines[2].append(" ");
            } else if (i == site_3.getSite().getPos3_5()) {
                lines[0].append(" ");
                lines[1].append("/");
                lines[2].append(" ");
            }
            lines[0].append(sequence.at(i));
            lines[1].append(NucleotideUtils.complement(sequence.at(i)));
        }
        lines[0].append("--  3'");
        lines[1].append("--  5'");
        lines[2].append(".COOH");
        out.println(lines[0]);
        out.println(lines[1]);
        out.println(lines[2]);
        out.println();
    }

    void print(PrintWriter out) {
        print(out, hemiStgy[0].siteUsages[0], hemiStgy[1].siteUsages[1]);
        print(out, hemiStgy[0].siteUsages[1], hemiStgy[1].siteUsages[0]);
    }

    @Override
    public String toString() {
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);
        print(out);
        out.flush();
        return sw.toString();
    }
}

class Polylinker {

    String name;

    String sequences[];

    Polylinker(String name, String s1, String s2) {
        this.name = name;
        this.sequences = new String[] { s1, s2 };
    }
}

class PolylinkerHit {

    Polylinker polylinker;

    int start, end;

    PolylinkerHit(Polylinker polylinker, int start, int end) {
        this.polylinker = polylinker;
        this.start = start;
        this.end = end;
    }

    @Override
    public String toString() {
        return polylinker.name + " " + start + "-" + end;
    }
}

/**
 * Plasmid
 * @author pierre
 *
 */
abstract class Plasmid {

    private static Polylinker POLYLINKER[] = new Polylinker[] { new Polylinker("pGAD424", "CCAAAAAAAGAGATC", "TTCAGTATCTACGATTCAT"), new Polylinker("T7/T3", "aattaaccctcactaaaggg", "taatacgactcactataggg"), new Polylinker("pTOPO", "ACCATGATTACGCCAAGCTTG", "ATACGACTCACTATAGGGCGA"), new Polylinker("pFASTBAC", "TATTCCGGATTATTCATACC", "GATTATGATCCTCTAGTACTTCTCGAC"), new Polylinker("Baculo", "ttttactgttttcgtaacagtttt", "cggatttccttgaagagagta"), new Polylinker("pBK", "ggtctatataagcagagctggt", "acaggaaacagctatgaccttg"), new Polylinker("GEX", "atcgaaggtcg", "tcagtcagtcacgatg"), new Polylinker("pET25B", "TAATACGACTCACTATA", "CCCGTTTAGAGGCCCCAAGGGGTTA"), new Polylinker("pIIIMS2-1", "ttccggctagaactagtggatcc", "tcgactctagaggatcg"), new Polylinker("pIIIMS2-2", "agagtcgacctgcaggcatgcaagctg", "gctagaactagtggatcc"), new Polylinker("pGBT9", "CAGTTGACTGTATCGCCG", "GCCCGGAATTAGCTTGG"), new Polylinker("pGADGL", "CCAAAAAAAGAGATC", "ACTATAGGGCGAATTGG"), new Polylinker("pcDNAFLAG", "atggactacaaggacgacgatgacaa", "cttggtaccgagctcggatcc"), new Polylinker("pcDNA3", "CACTATAGGGAGACCC", "AGGTGACACTATAGAATA") };

    private String name = "";

    private byte sequence[];

    private SiteList sites = new SiteList();

    protected int atgPosition = -1;

    public Plasmid() {
    }

    public SiteList getSites() {
        return sites;
    }

    public Site getSiteAt(int index) {
        return getSites().getSiteAt(index);
    }

    public int getSiteCount() {
        return getSites().getSiteCount();
    }

    public void digest(EnzymeList rebase, EnzymeSelector selector) {
        getSites().clear();
        for (Enzyme e : rebase) {
            if (selector != null && !selector.accept(e)) continue;
            for (Orientation orient : Orientation.values()) {
                for (int i = 0; i < size(); ++i) {
                    int j = 0;
                    for (j = 0; j < e.size(); ++j) {
                        if (!Enzyme.DNAcmp(at(i + j), e.getCharAt(j, orient))) {
                            break;
                        }
                    }
                    if (j == e.size()) {
                        getSites().addSite(new Site(this, e, i, orient));
                    }
                }
                if (e.isPalindromic()) break;
            }
        }
        getSites().sort();
    }

    public char at(int index) {
        return (char) sequence[index % size()];
    }

    public int size() {
        return sequence.length;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean isVector() {
        return false;
    }

    public boolean isInsert() {
        return false;
    }

    public Vecteur asVector() {
        return Vecteur.class.cast(this);
    }

    public Insert asInsert() {
        return Insert.class.cast(this);
    }

    public void setSequence(String s) {
        byte array[] = new byte[s.length()];
        int j = 0;
        for (int k = 0; k < s.length(); ++k) {
            if (Character.isWhitespace(s.charAt(k))) continue;
            if (Character.isDigit(s.charAt(k))) continue;
            if (!Character.isLetter(s.charAt(k))) continue;
            array[j++] = (byte) s.charAt(k);
        }
        this.sequence = new byte[j];
        System.arraycopy(array, 0, this.sequence, 0, this.sequence.length);
        System.err.println("setSequence " + getName() + " " + size());
    }

    public abstract int findATG();

    public abstract String getSpanColor();

    public int indexOf(CharSequence substr, int pos) {
        for (int i = pos; i + substr.length() < size(); ++i) {
            int j = 0;
            for (j = 0; j < substr.length(); ++j) {
                if (substr.charAt(j) != at(i + j)) break;
            }
            if (j == substr.length()) return i;
        }
        return -1;
    }

    public PolylinkerHit findPolylinker(Polylinker polylinker) {
        String forward = polylinker.sequences[0];
        String reverse = polylinker.sequences[1];
        forward = forward.toUpperCase();
        reverse = NucleotideUtils.reverseComplement(reverse.toUpperCase());
        int n1 = indexOf(forward, 0);
        if (n1 != -1) {
            int n2 = indexOf(reverse, n1 + forward.length());
            if (n2 != -1) return new PolylinkerHit(polylinker, n1, n2 + reverse.length());
        }
        String s = forward;
        forward = NucleotideUtils.reverseComplement(reverse);
        reverse = NucleotideUtils.reverseComplement(s);
        n1 = indexOf(forward, 0);
        if (n1 != -1) {
            int n2 = indexOf(reverse, n1 + forward.length());
            if (n2 != -1) return new PolylinkerHit(polylinker, n1, n2 + reverse.length());
        }
        return null;
    }

    public PolylinkerHit findPolylinker() {
        for (Polylinker polylinker : POLYLINKER) {
            PolylinkerHit p = findPolylinker(polylinker);
            if (p != null) {
                return p;
            }
        }
        return null;
    }

    public int getFrameAt(int pos) {
        if (this.atgPosition == -1) return -1;
        int i = pos % 3;
        switch(this.atgPosition % 3) {
            case (0):
                return (i);
            case (1):
                switch(i) {
                    case (0):
                        return (2);
                    case (1):
                        return (0);
                    case (2):
                        return (1);
                }
                break;
            case (2):
                switch(i) {
                    case (0):
                        return (1);
                    case (1):
                        return (2);
                    case (2):
                        return (0);
                }
        }
        Assert.assertUnreachableStatement();
        return -1;
    }

    public boolean isInFrame(int position) {
        if (this.atgPosition == -1) return false;
        return getFrameAt(position) == 0;
    }

    public String getDescription() {
        return getName() + " size:" + size() + " sites:" + getSites().getSiteCount();
    }

    @Override
    public String toString() {
        return getName() + " (" + size() + " pb)";
    }

    public int countIn(Enzyme e, int beg, int end) {
        return getSites().listSitesIn(e, beg, end).count();
    }

    public int countOut(Enzyme e, int beg, int end) {
        return getSites().listSitesOut(e, beg, end).count();
    }
}

/**
 * Vecteur
 * @author pierre
 *
 */
class Vecteur extends Plasmid {

    static final int BOX5 = 0;

    static final int BOX3 = 1;

    int polylinker[] = new int[2];

    @Override
    public boolean isVector() {
        return true;
    }

    @Override
    public String getSpanColor() {
        return "vector";
    }

    @Override
    public String getDescription() {
        return "VECTOR " + super.getDescription() + " [" + this.polylinker[0] + "-" + this.polylinker[1] + "]";
    }

    public boolean isInPolylinker(int loc) {
        return polylinker[BOX5] <= loc && loc <= polylinker[BOX3];
    }

    public static Vecteur readFasta(BufferedReader in) throws IOException {
        Vecteur plasmid = new Vecteur();
        String line = null;
        StringBuilder b = new StringBuilder();
        while ((line = in.readLine()) != null) {
            if (line.trim().length() == 0) continue;
            if (line.startsWith(">")) {
                plasmid.setName(line.substring(1));
                break;
            } else {
                plasmid.setName("VECTOR");
                b.append(line);
                break;
            }
        }
        while ((line = in.readLine()) != null) {
            b.append(line);
        }
        plasmid.setSequence(b.toString());
        System.err.println(plasmid.getName() + " " + plasmid.size());
        return plasmid;
    }

    @Override
    public int findATG() {
        int ATGpos = -1;
        GeneticCode code = GeneticCode.getStandard();
        int _max = -1;
        for (int i = 0; i <= 2; ++i) {
            int vara = 0;
            for (int j = this.polylinker[0] + i; j >= 0; j -= 3) {
                if (code.isStopCodon(code.translate(at(j), at(j + 1), at(j + 1)))) {
                    break;
                } else vara++;
            }
            if (vara > _max) {
                _max = vara;
                ATGpos = this.polylinker[0] + i;
            }
            vara = 0;
            for (int j = this.polylinker[1] + i; j < size(); j += 3) {
                if (code.isStopCodon(code.translate(at(j), at(j + 1), at(j + 1)))) {
                    break;
                } else vara++;
            }
            if (vara > _max) {
                _max = vara;
                ATGpos = this.polylinker[1] + i;
            }
            vara = 0;
            for (int j = this.polylinker[0] + i; j <= this.polylinker[1]; j += 3) {
                if (code.isStopCodon(code.translate(at(j), at(j + 1), at(j + 1)))) break; else vara++;
            }
            if (vara > _max) {
                _max = vara;
                ATGpos = this.polylinker[0] + i;
            }
        }
        return ATGpos;
    }
}

/**
 * 
 * @author pierre
 *
 */
class Insert extends Plasmid {

    static final int BOX5 = 0;

    static final int BOX5_INT = 1;

    static final int BOX3_INT = 2;

    static final int BOX3 = 3;

    int polylinker[] = new int[4];

    @Override
    public boolean isInsert() {
        return true;
    }

    @Override
    public String getSpanColor() {
        return "insert";
    }

    @Override
    public String getDescription() {
        return "INSERT " + super.getDescription() + " [" + this.polylinker[0] + "-" + this.polylinker[1] + "-" + this.polylinker[2] + "-" + this.polylinker[3] + "]";
    }

    public boolean isInPolylinker5(int loc) {
        return polylinker[0] <= loc && loc <= polylinker[1];
    }

    public boolean isInPolylinker3(int loc) {
        return polylinker[2] <= loc && loc <= polylinker[3];
    }

    public static Insert readFasta(BufferedReader in) throws IOException {
        System.err.println("loading");
        Insert plasmid = new Insert();
        String line = null;
        StringBuilder b = new StringBuilder();
        while ((line = in.readLine()) != null) {
            if (line.trim().length() == 0) continue;
            if (line.startsWith(">")) {
                plasmid.setName(line.substring(1));
                break;
            } else {
                plasmid.setName("INSERT");
                b.append(line);
                break;
            }
        }
        while ((line = in.readLine()) != null) {
            b.append(line);
        }
        System.err.println("ok");
        plasmid.setSequence(b.toString());
        System.err.println("I size=" + plasmid.size());
        return plasmid;
    }

    @Override
    public int findATG() {
        GeneticCode code = GeneticCode.getStandard();
        int max = 0;
        int ATGPos = -1;
        for (int i = 0; i <= 2; i++) {
            int vara = 0;
            for (int j = polylinker[0] + i; j <= polylinker[3]; j += 3) {
                if (!code.isStopCodon(code.translate(at(j), at(j + 1), at(j + 2)))) {
                    vara++;
                    if (vara > max) {
                        max = vara;
                        ATGPos = polylinker[0] + i;
                    }
                } else {
                    vara = 0;
                }
            }
        }
        return ATGPos;
    }
}

abstract class CloneItProgram implements Runnable {

    Insert insert = new Insert();

    EnzymeList rebase = new EnzymeList();

    public void setEnzymeList(EnzymeList enzymes) {
        this.rebase = new EnzymeList(enzymes);
    }

    public void message(Object o) {
        System.err.println(o);
    }

    public boolean canceled() {
        return false;
    }

    public abstract void run();
}

class SubCloning extends CloneItProgram {

    int maxPartialDigestionCount = 0;

    boolean usePolymerase = false;

    boolean useCIAP = false;

    Vecteur vector = new Vecteur();

    Plasmid plasmids[] = new Plasmid[] { super.insert, vector };

    boolean inFrameNH2 = false;

    boolean inFrameCOOH = false;

    int max_num_stgies = 100;

    void digest() {
        for (Plasmid p : this.plasmids) {
            message("Digest " + p.getName());
            if (canceled()) break;
            p.digest(this.rebase, new EnzymeSelector() {

                @Override
                public boolean accept(Enzyme e) {
                    return e.getWeight() > 5f;
                }
            });
            message(p.getName() + " " + p.getSites().getSiteCount());
        }
    }

    @Override
    public void run() {
        digest();
        HashSet<Enzyme> removeInsert = new HashSet<Enzyme>();
        for (Enzyme enz : this.rebase) {
            if (canceled()) break;
            if (this.insert.countIn(enz, this.insert.polylinker[Insert.BOX5_INT] + 1, this.insert.polylinker[Insert.BOX3_INT] - 1) > maxPartialDigestionCount) {
                removeInsert.add(enz);
            }
        }
        message(this.insert.getDescription());
        HashSet<Enzyme> removeVector = new HashSet<Enzyme>();
        for (Enzyme enz : this.rebase) {
            if (canceled()) break;
            if (this.vector.countOut(enz, this.vector.polylinker[Vecteur.BOX5] - 1, this.vector.polylinker[Vecteur.BOX3] + 1) > maxPartialDigestionCount) {
                message("Remove " + enz + " from " + vector.getName());
                removeVector.add(enz);
            }
        }
        message(this.vector.getDescription());
        message("Loop");
        Vector<Strategy> strategies = new Vector<Strategy>(200);
        for (int indexI5 = 0; indexI5 < this.insert.getSiteCount(); ++indexI5) {
            Site siteI5 = this.insert.getSiteAt(indexI5);
            if (siteI5.getPosition() < this.insert.polylinker[Insert.BOX5]) continue;
            if (siteI5.getPosition() > this.insert.polylinker[Insert.BOX5_INT]) break;
            if (canceled()) break;
            message("I5 " + siteI5);
            for (Polymerase usePolI5 : Polymerase.values()) {
                if (!usePolymerase && usePolI5 == Polymerase.POLYMERASE) continue;
                if (usePolI5 == Polymerase.POLYMERASE && siteI5.getEnzyme().getType() == CutType.BLUNT) continue;
                for (int indexV5 = 0; indexV5 < this.vector.getSiteCount(); ++indexV5) {
                    Site siteV5 = this.vector.getSiteAt(indexV5);
                    if (siteV5.getPosition() < this.vector.polylinker[Vecteur.BOX5]) continue;
                    if (siteV5.getPosition() > this.vector.polylinker[Vecteur.BOX3]) break;
                    if (canceled()) break;
                    for (Polymerase usePolV5 : Polymerase.values()) {
                        if (!usePolymerase && usePolV5 == Polymerase.POLYMERASE) continue;
                        if (usePolV5 == Polymerase.POLYMERASE && siteV5.getEnzyme().getType() == CutType.BLUNT) continue;
                        HemiStrategy leftStgy = new HemiStrategy(new SiteUsage(siteV5, usePolV5), new SiteUsage(siteI5, usePolI5), this.inFrameNH2);
                        if (!leftStgy.isValid()) continue;
                        for (int indexV3 = indexV5; indexV3 < this.vector.getSiteCount(); ++indexV3) {
                            Site siteV3 = this.vector.getSites().getSiteAt(indexV3);
                            if (siteV3.getPosition() > this.vector.polylinker[Vecteur.BOX3]) break;
                            for (Polymerase usePolV3 : Polymerase.values()) {
                                if (!usePolymerase && usePolV3 == Polymerase.POLYMERASE) continue;
                                if (usePolV3 == Polymerase.POLYMERASE && siteV3.getEnzyme().getType() == CutType.BLUNT) continue;
                                if (!useCIAP) {
                                    HemiStrategy stgy = new HemiStrategy(new SiteUsage(siteV5, usePolV5), new SiteUsage(siteV3, usePolV3), this.inFrameCOOH);
                                    if (stgy.isValid()) continue;
                                }
                                for (int indexI3 = indexI5 + 1; indexI3 < this.insert.getSiteCount(); ++indexI3) {
                                    Site siteI3 = this.insert.getSites().getSiteAt(indexI3);
                                    if (siteI3.getPosition() < this.insert.polylinker[Insert.BOX3_INT]) continue;
                                    if (siteI3.getPosition() > this.insert.polylinker[Insert.BOX3]) break;
                                    for (Polymerase usePolI3 : Polymerase.values()) {
                                        if (!usePolymerase && usePolI3 == Polymerase.POLYMERASE) continue;
                                        if (usePolI3 == Polymerase.POLYMERASE && siteI3.getEnzyme().getType() == CutType.BLUNT) continue;
                                        HemiStrategy rightStgy = new HemiStrategy(new SiteUsage(siteI3, usePolI3), new SiteUsage(siteV3, usePolV3), false);
                                        if (!rightStgy.isValid()) {
                                            continue;
                                        }
                                        Strategy strategy = new Strategy(leftStgy, rightStgy);
                                        strategies.add(strategy);
                                        Collections.sort(strategies);
                                        if (strategies.size() > this.max_num_stgies) {
                                            strategies.setSize(this.max_num_stgies);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        echo(strategies);
    }

    public void echo(Vector<Strategy> strategies) {
        for (Strategy stgy : strategies) {
            System.out.println(stgy);
        }
    }
}

abstract class CloneItBase {

    protected EnzymeList rebase = null;

    protected String userDefinedRebasePath = null;

    protected String program = null;

    protected Vecteur vector = null;

    protected Insert insert = null;

    protected int vbox[] = new int[] { -1, -1 };

    protected int ibox[] = new int[] { -1, -1, -1, -1 };

    protected boolean inFrameNH2 = false;

    protected boolean inFrameCOOH = false;

    protected int iATG = -1;

    protected int vATG = -1;

    protected int max_stgies = 100;

    protected boolean use_polymerase = false;

    protected boolean use_ciap = false;

    abstract void message(Object o);

    protected EnzymeList downloadRebase() throws IOException {
        final String FTP_REBASE = "ftp://ftp.neb.com/pub/rebase/";
        String url = FTP_REBASE + "VERSION";
        message("Fetching " + url);
        BufferedReader in = new BufferedReader(new InputStreamReader(new URL(url).openStream()));
        String line = in.readLine();
        in.close();
        if (line == null) throw new IOException("Cannot get latest version in " + url);
        message("Version " + line);
        url = FTP_REBASE + "allenz." + line.trim();
        message("Fetching " + url);
        in = new BufferedReader(new InputStreamReader(new URL(url).openStream()));
        EnzymeList el = EnzymeList.read(in);
        in.close();
        return el;
    }

    public void run() {
        if (userDefinedRebasePath != null) {
            message("Getting Rebase from " + userDefinedRebasePath);
            try {
                BufferedReader in = IOUtils.openReader(this.userDefinedRebasePath);
                this.rebase = EnzymeList.read(in);
                in.close();
            } catch (IOException err) {
                throw new RuntimeException(err);
            }
        }
        if (this.rebase == null && System.getenv("REBASE_PATH") != null) {
            String path = System.getenv("REBASE_PATH");
            message("Getting Rebase from $REBASE_PATH=" + path);
            try {
                BufferedReader in = IOUtils.openReader(path);
                this.rebase = EnzymeList.read(in);
                in.close();
            } catch (IOException err) {
                throw new RuntimeException(err);
            }
        } else if (this.rebase == null) {
            message("Undefined $REBASE_PATH");
        }
        if (this.rebase == null) {
            message("Getting Rebase from NEB");
            try {
                this.rebase = downloadRebase();
            } catch (IOException err) {
                throw new RuntimeException(err);
            }
        }
        message(this.rebase.getEnzymeCount());
        if (this.rebase.getEnzymeCount() == 0) {
            throw new IllegalArgumentException("Rebase is empty");
        }
        if (program == null) throw new IllegalArgumentException("Undefined program"); else if (program.equals("subcloning")) {
            if (this.vector == null) throw new IllegalArgumentException("undefined vector");
            if (this.vector.size() == 0) throw new IllegalArgumentException("Empty vector");
            if (this.insert == null) throw new IllegalArgumentException("undefined insert");
            if (this.insert.size() == 0) throw new IllegalArgumentException("Empty insert");
            if (this.ibox[0] == -1 && this.ibox[1] == -1 && this.ibox[2] == -1 && this.ibox[3] == -1) {
                message("Searching Polylinker in " + this.insert.getName());
                PolylinkerHit p = this.insert.findPolylinker();
                if (p == null) throw new IllegalArgumentException("Cannot find Polylinker of " + this.insert.getName());
                message(p);
                this.ibox[0] = p.start;
                this.ibox[3] = p.end;
                int n = (this.ibox[3] - this.ibox[0]) / 10;
                this.ibox[1] = this.ibox[0] + n;
                this.ibox[2] = this.ibox[3] - n;
            }
            if (this.vbox[0] == -1 && this.vbox[1] == -1) {
                message("Searching Polylinker in " + this.vector.getName());
                PolylinkerHit p = this.vector.findPolylinker();
                if (p == null) throw new IllegalArgumentException("Cannot find Polylinker of " + this.vector.getName());
                message(p);
                this.vbox[0] = p.start;
                this.vbox[1] = p.end;
            }
            if (this.ibox[0] >= this.ibox[1] || this.ibox[1] >= this.ibox[2] || this.ibox[2] >= this.ibox[3]) {
                throw new IllegalArgumentException("In Insert " + this.insert.getName() + " bad Polylinker");
            }
            if (this.vbox[Vecteur.BOX5] >= this.vbox[Vecteur.BOX3]) {
                throw new IllegalArgumentException("In Vector " + this.vector.getName() + " bad Polylinker");
            }
            SubCloning app = new SubCloning();
            app.rebase = this.rebase;
            app.vector = this.vector;
            app.insert = this.insert;
            app.vector.polylinker[Vecteur.BOX5] = vbox[Vecteur.BOX5];
            app.vector.polylinker[Vecteur.BOX3] = vbox[Vecteur.BOX3];
            app.insert.polylinker[Insert.BOX5] = ibox[Insert.BOX5];
            app.insert.polylinker[Insert.BOX5_INT] = ibox[Insert.BOX5_INT];
            app.insert.polylinker[Insert.BOX3_INT] = ibox[Insert.BOX3_INT];
            app.insert.polylinker[Insert.BOX3] = ibox[Insert.BOX3];
            app.inFrameNH2 = this.inFrameNH2;
            app.inFrameCOOH = this.inFrameCOOH;
            app.useCIAP = this.use_ciap;
            app.usePolymerase = this.use_polymerase;
            app.max_num_stgies = this.max_stgies;
            if (this.iATG != -1) {
                app.insert.atgPosition = this.iATG;
            }
            if (this.vATG != -1) {
                app.vector.atgPosition = this.vATG;
            }
            if (this.inFrameCOOH || this.inFrameNH2) {
                if (app.insert.atgPosition == -1) {
                    app.insert.atgPosition = app.insert.findATG();
                }
                if (app.vector.atgPosition == -1) {
                    app.vector.atgPosition = app.vector.findATG();
                }
            }
            app.plasmids = new Plasmid[] { app.insert, app.vector };
            app.run();
        } else {
            throw new IllegalArgumentException("Unknown program :" + this.program);
        }
    }
}

class Standalone extends CloneItBase {

    @Override
    void message(Object o) {
        System.out.println("[LOG]" + o);
    }

    void exec(String args[]) throws IOException {
        int optind = 0;
        String vectorSource = null;
        String insertSource = null;
        while (optind < args.length) {
            if (args[optind].equals("-h")) {
                System.err.println("\t-h help(this screen)");
            } else if (args[optind].equals("-v")) {
                vectorSource = args[++optind];
            } else if (args[optind].equals("-vbox")) {
                String pos[] = args[++optind].split("[,]");
                if (pos.length != 4) throw new IllegalArgumentException("expected 2 integeres in " + args[optind]);
                this.vbox[0] = Integer.parseInt(pos[0].trim());
                this.vbox[1] = Integer.parseInt(pos[1].trim());
            } else if (args[optind].equals("-ibox")) {
                String pos[] = args[++optind].split("[,]");
                if (pos.length != 4) throw new IllegalArgumentException("expected 2 integeres in " + args[optind]);
                for (int i = 0; i < 4; ++i) {
                    this.ibox[i] = Integer.parseInt(pos[i].trim());
                }
            } else if (args[optind].equals("-i")) {
                insertSource = args[++optind];
            } else if (args[optind].equals("-rebase")) {
                super.userDefinedRebasePath = args[++optind];
            } else if (args[optind].equals("-program")) {
                super.program = args[++optind];
            } else if (args[optind].equals("-nh2")) {
                super.inFrameNH2 = true;
            } else if (args[optind].equals("-cooh")) {
                super.inFrameCOOH = true;
            } else if (args[optind].equals("-ciap")) {
                super.use_ciap = true;
            } else if (args[optind].equals("-pol")) {
                super.use_polymerase = true;
            } else if (args[optind].equals("-max")) {
                super.max_stgies = Integer.parseInt(args[++optind].trim());
                ;
            } else if (args[optind].equals("--")) {
                ++optind;
                break;
            } else if (args[optind].startsWith("-")) {
                throw new IllegalArgumentException("Unknown option " + args[optind]);
            } else {
                break;
            }
            ++optind;
        }
        if (optind != args.length) {
            throw new IllegalArgumentException("Too many arguments");
        }
        if (vectorSource != null) {
            BufferedReader in = IOUtils.openReader(vectorSource);
            super.vector = Vecteur.readFasta(in);
            in.close();
        }
        if (insertSource != null) {
            BufferedReader in = IOUtils.openReader(insertSource);
            super.insert = Insert.readFasta(in);
            in.close();
        }
        run();
    }
}

public class CloneIt extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final int INSERT = 0;

    private static final int VECTOR = 1;

    private EnzymeList rebase = EnzymeList.newEnzymeList();

    private IntegerField spinnerInsertBox[] = new IntegerField[4];

    private IntegerField spinnerInsertATG;

    private IntegerField spinnerVectorBox[] = new IntegerField[2];

    private IntegerField spinnerVectorATG;

    private JTextArea seqArea[] = new JTextArea[2];

    private JTextField seqName[] = new JTextField[2];

    public CloneIt() {
        super(new BorderLayout(5, 5));
        JPanel pane0 = new JPanel(new GridLayout(0, 1, 10, 10));
        this.add(pane0, BorderLayout.CENTER);
        Font smallFont = new Font("Dialog", Font.PLAIN, 9);
        this.seqArea[0] = new JTextArea();
        this.seqArea[0].setFont(new Font("Courier", Font.PLAIN, 10));
        this.seqArea[1] = new JTextArea();
        this.seqArea[1].setFont(new Font("Courier", Font.PLAIN, 10));
        JPanel pane = new JPanel(new GridLayout(0, 1, 5, 5));
        pane0.add(pane);
        JPanel pane2 = new JPanel(new GridLayout(1, 0, 5, 5));
        pane.add(pane2);
        JPanel pane3 = new JPanel(new BorderLayout(5, 5));
        Box vBox = Box.createVerticalBox();
        JPanel pane4 = new JPanel(new GridLayout(1, 0, 5, 5));
        vBox.add(pane4);
        pane4.add(this.seqName[0] = new JTextField("Insert", 20));
        this.seqName[0].setFont(smallFont);
        this.seqName[0].setBorder(BorderFactory.createTitledBorder("Name"));
        pane4.add(this.spinnerInsertATG = new IntegerField());
        spinnerInsertATG.setBorder(BorderFactory.createTitledBorder("ATG"));
        spinnerInsertATG.setFont(smallFont);
        pane4 = new JPanel(new GridLayout(1, 0, 5, 5));
        vBox.add(pane4);
        pane4.add(new JLabel("5'", JLabel.RIGHT));
        pane4.add(spinnerInsertBox[0] = new IntegerField());
        spinnerInsertBox[0].setBorder(BorderFactory.createTitledBorder("5'"));
        pane4.add(spinnerInsertBox[1] = new IntegerField());
        spinnerInsertBox[1].setBorder(BorderFactory.createTitledBorder("5' int"));
        pane4.add(spinnerInsertBox[2] = new IntegerField());
        spinnerInsertBox[2].setBorder(BorderFactory.createTitledBorder("3' int"));
        pane4.add(spinnerInsertBox[3] = new IntegerField());
        spinnerInsertBox[3].setBorder(BorderFactory.createTitledBorder("3'"));
        for (IntegerField spin : spinnerInsertBox) {
            spin.setFont(smallFont);
        }
        pane3.add(vBox, BorderLayout.NORTH);
        pane3.setBorder(BorderFactory.createTitledBorder("Insert"));
        pane2.add(pane3);
        pane3.add(new JScrollPane(this.seqArea[VECTOR]), BorderLayout.CENTER);
        pane3 = new JPanel(new BorderLayout());
        vBox = Box.createVerticalBox();
        pane4 = new JPanel(new GridLayout(1, 0, 5, 5));
        vBox.add(pane4);
        pane4.add(this.seqName[1] = new JTextField("Vector", 20));
        this.seqName[1].setBorder(BorderFactory.createTitledBorder("Name"));
        seqName[1].setFont(smallFont);
        pane4.add(this.spinnerVectorATG = new IntegerField());
        spinnerVectorATG.setBorder(BorderFactory.createTitledBorder("ATG"));
        spinnerVectorATG.setFont(smallFont);
        pane4 = new JPanel(new GridLayout(1, 0, 5, 5));
        vBox.add(pane4);
        pane4.add(spinnerVectorBox[0] = new IntegerField());
        spinnerVectorBox[0].setBorder(BorderFactory.createTitledBorder("5'"));
        pane4.add(spinnerVectorBox[1] = new IntegerField());
        spinnerVectorBox[1].setBorder(BorderFactory.createTitledBorder("3'"));
        for (IntegerField spin : spinnerVectorBox) {
            spin.setFont(smallFont);
        }
        pane3.add(vBox, BorderLayout.NORTH);
        pane3.setBorder(BorderFactory.createTitledBorder("Vector"));
        pane2.add(pane3);
        pane3.add(new JScrollPane(this.seqArea[INSERT]), BorderLayout.CENTER);
        JPanel pane5 = new JPanel(new GridLayout(0, 2, 10, 10));
        pane0.add(pane5);
        JPanel pane6 = new JPanel(new BorderLayout());
        JTable table = new JTable(this.rebase);
        pane6.add(new JScrollPane(table));
        pane5.add(pane6);
        JTabbedPane tabbedPane = new JTabbedPane();
        pane5.add(tabbedPane);
        JPanel panel7 = new JPanel();
        tabbedPane.addTab("CloneIt", panel7);
        JPanel panel8 = new JPanel(new GridLayout(1, 0, 3, 3));
        panel7.add(panel8, BorderLayout.SOUTH);
        panel8.add(new JProgressBar());
        panel8.add(new JButton(new AbstractAction("CloneIt") {

            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) {
                doMenuCloneIt();
            }
        }));
        tabbedPane.addTab("Parameters", new JPanel(new BorderLayout()));
    }

    public EnzymeList getEnzymeList() {
        return this.rebase;
    }

    public void doMenuCloneIt() {
        SubCloning prog = new SubCloning();
        for (int i = 0; i < 2; ++i) {
            prog.plasmids[i].setSequence(this.seqArea[i].getText());
            prog.plasmids[i].setName(this.seqName[i].getText().trim());
            prog.plasmids[i].digest(getEnzymeList(), new EnzymeSelector() {

                @Override
                public boolean accept(Enzyme e) {
                    return true;
                }
            });
        }
        if (!fillInsertData(prog.insert)) return;
        Thread t = new Thread(prog);
        t.start();
        try {
            t.join();
        } catch (InterruptedException e) {
        }
    }

    private boolean fillInsertData(Insert insert) {
        int bounds[] = new int[4];
        for (int i = 0; i < this.spinnerInsertBox.length; ++i) {
            bounds[i] = this.spinnerInsertBox[i].getValue();
            if (i > 0 && bounds[i - 1] >= bounds[i]) {
                JOptionPane.showMessageDialog(this, "Error with Insert bounds: " + bounds[i] + "<" + bounds[i - 1], "Error", JOptionPane.ERROR_MESSAGE, null);
                return false;
            }
        }
        if (bounds[3] >= insert.size()) {
            JOptionPane.showMessageDialog(this, "Error with Insert bounds: length=" + insert.size() + "<=" + bounds[3], "Error", JOptionPane.ERROR_MESSAGE, null);
            return false;
        }
        return true;
    }

    public void readSequence(int seq, InputStream is) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(is));
        String line;
        String name = null;
        StringBuilder b = new StringBuilder();
        while ((line = in.readLine()) != null) {
            if (line.startsWith(">")) {
                if (name != null) break;
                name = line.substring(1).trim();
            } else if (name != null) {
                b.append(line).append("\n");
            }
        }
        if (name == null) throw new IOException("No sequence was found");
        this.seqArea[seq].setText(b.toString());
        this.seqName[seq].setText(name);
        this.seqName[seq].setCaretPosition(0);
        this.seqArea[seq].setCaretPosition(0);
        Vecteur tmp = new Vecteur();
        tmp.setSequence(b.toString());
        PolylinkerHit bounds = tmp.findPolylinker();
        if (bounds != null) {
            if (seq == INSERT) {
                int n = bounds.start;
                int m = bounds.end;
                this.spinnerInsertBox[0].setValue(n);
                this.spinnerInsertBox[1].setValue(n + (m - n) / 10);
                this.spinnerInsertBox[2].setValue(m - (m - n) / 10);
                this.spinnerInsertBox[3].setValue(m);
            } else {
                this.spinnerVectorBox[0].setValue(bounds.start);
                this.spinnerVectorBox[1].setValue(bounds.end);
            }
        }
    }

    /**
	 * @param args
	 */
    public static void createAndShowGUI() {
        try {
            CloneIt cloneit = new CloneIt();
            JFrame window = new JFrame("CloneIt");
            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            window.setBounds(100, 100, screen.width - 200, screen.height - 200);
            window.setContentPane(cloneit);
            window.setVisible(true);
        } catch (Exception e) {
            ThrowablePane.show(null, e);
        }
    }

    public static void main(String[] args) {
        try {
            if (args.length != 0) {
                new Standalone().exec(args);
            } else {
                JFrame.setDefaultLookAndFeelDecorated(true);
                JDialog.setDefaultLookAndFeelDecorated(true);
                javax.swing.SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        createAndShowGUI();
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
