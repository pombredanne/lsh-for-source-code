package org.lindenb.scifoaf;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.lindenb.lib.ncbi.pubmed.PubmedRecord;
import org.lindenb.lib.xml.XMLUtilities;

/**
 * @author lindenb
 *
 * <code>SciFOAF</code>
 */
public class SciFOAF extends JFrame {

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 1L;

    private Transformer pubmedXFormer = null;

    Author mainAuthor;

    AuthorList authors;

    JComboBox listMaxFetch;

    PaperList papers;

    LaboratoryList laboratories;

    RelationShipList whoknowwho;

    JLabel infoLabel;

    TaskList taskList;

    JPanel cardPanel;

    CardLayout cardLayout;

    HashMap questions;

    /**
     * Constructor for <code>SciFOAF</code>
     * @param pmid
     */
    public SciFOAF() {
        super("SciFOAF");
        this.mainAuthor = null;
        this.authors = new AuthorList();
        this.papers = new PaperList();
        this.whoknowwho = new RelationShipList();
        this.laboratories = new LaboratoryList();
        this.taskList = new TaskList();
        this.questions = new HashMap();
        this.infoLabel = new JLabel();
        JPanel pane0 = new JPanel(new BorderLayout());
        JPanel pane2 = new JPanel(new BorderLayout());
        pane2.add(new JLabel(getIcon("info.png")), BorderLayout.WEST);
        this.infoLabel.setOpaque(true);
        this.infoLabel.setBackground(Color.WHITE);
        pane2.add(infoLabel, BorderLayout.CENTER);
        pane0.add(pane2, BorderLayout.SOUTH);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        setBounds(50, 50, dim.width - 100, dim.height - 100);
        this.cardPanel = new JPanel((this.cardLayout = new CardLayout()));
        this.taskList.addTask(new Task(this, QPromptPMID.KEY));
        this.questions.put(QPromptPMID.KEY, new QPromptPMID(this));
        this.questions.put(QAnalysePaper.KEY, new QAnalysePaper(this));
        this.questions.put(QAddAuthor.KEY, new QAddAuthor(this));
        for (Iterator iter = questions.keySet().iterator(); iter.hasNext(); ) {
            Question q = (Question) questions.get(iter.next());
            this.cardPanel.add(q.create(), q.getKey());
        }
        JPanel borderedPane = new JPanel(new BorderLayout());
        borderedPane.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        pane0.add(borderedPane, BorderLayout.CENTER);
        borderedPane.add(this.cardPanel, BorderLayout.CENTER);
        JPanel topPane = new JPanel(new BorderLayout(10, 10));
        topPane.add(new JLabel(getIcon("foaf.png"), JLabel.LEFT), BorderLayout.WEST);
        topPane.add(new JLabel(getBundle("sciFoafTitle")), BorderLayout.CENTER);
        JPanel pane1 = new JPanel();
        pane1.setLayout(new BoxLayout(pane1, BoxLayout.PAGE_AXIS));
        topPane.add(pane1, BorderLayout.EAST);
        JButton button = createButton("rdf.gif");
        button.setText("Display RDF");
        button.setBorderPainted(false);
        button.setToolTipText("Display the current RDF/XML file in a new Window");
        button.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                createRDFFrame();
            }
        });
        pane1.add(button);
        button = createButton("disk.gif");
        button.setText("Save RDF as...");
        button.setBorderPainted(false);
        button.setToolTipText("Save the RDF/XML file in a file");
        button.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                saveAs();
            }
        });
        pane1.add(button);
        this.listMaxFetch = new JComboBox(new Integer[] { new Integer(5), new Integer(10), new Integer(20), new Integer(50), new Integer(100), new Integer(200) });
        this.listMaxFetch.setSelectedIndex(2);
        pane1.add(new JLabel("Max. articles to fetch"));
        pane1.add(new JScrollPane(this.listMaxFetch));
        pane1.add(new JLabel());
        pane0.add(topPane, BorderLayout.NORTH);
        setContentPane(pane0);
        runTopTask();
    }

    /**
 * @param label
 * @return
 */
    String getBundle(String label) {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle(getClass().getPackage().getName().replace('.', '/') + "/Labels", Locale.US, this.getClass().getClassLoader());
            return bundle.getString(label);
        } catch (Exception err) {
            return "Cannot find bundle " + label;
        }
    }

    /**
     * setInformation
     * @param s
     */
    public void setInformation(String s) {
        this.infoLabel.setText(s);
        this.infoLabel.paintImmediately(0, 0, this.infoLabel.getWidth(), this.infoLabel.getHeight());
    }

    JPanel article2panel(PubmedRecord rec, Question question) {
        JPanel pane = new JPanel(new FlowLayout(FlowLayout.CENTER));
        if (pubmedXFormer == null) {
            try {
                TransformerFactory factory = TransformerFactory.newInstance();
                InputStream in = getClass().getResourceAsStream("pubmed2html.xsl");
                if (in == null) {
                    pane.add(new JLabel("Cannot get XSLT"));
                    return pane;
                }
                Templates template = factory.newTemplates(new StreamSource(in));
                pubmedXFormer = template.newTransformer();
            } catch (Exception e) {
                pubmedXFormer = null;
                pane.add(new JLabel("" + e));
                return pane;
            }
        }
        Source source = new DOMSource(rec.getNode());
        StringWriter resultstr = new StringWriter();
        Result result = new StreamResult(resultstr);
        try {
            pubmedXFormer.transform(source, result);
        } catch (Exception e) {
            pane.add(new JLabel("" + e));
            return pane;
        }
        JEditorPane editorPane = new JEditorPane("text/html", resultstr.toString());
        editorPane.addHyperlinkListener(question);
        editorPane.setEditable(false);
        editorPane.setCaretPosition(0);
        editorPane.setAutoscrolls(true);
        JScrollPane scroll = new JScrollPane(editorPane, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        Dimension dim = getSize();
        dim.width *= 0.75;
        dim.height *= 0.5;
        scroll.setPreferredSize(dim);
        scroll.setMaximumSize(dim);
        pane.add(scroll);
        return pane;
    }

    public void runTopTask() {
        Task task = taskList.getTopTask();
        Question q = (Question) questions.get(task.tasktype);
        if (q != null) {
            setInformation("Updating task " + q);
            q.show();
        }
    }

    public ImageIcon getAuthorIcon() {
        switch((int) (Math.random() * 4.0)) {
            case 0:
                return getIcon("mendel.jpeg");
            case 1:
                return getIcon("pasteur.jpeg");
            case 2:
                return getIcon("watson.jpeg");
        }
        return getIcon("jacob.jpeg");
    }

    public ImageIcon getIcon(String name) {
        URL url = getClass().getResource("icons/" + name);
        if (url == null) {
            System.err.println("Cannot get icon " + name);
            return null;
        }
        return new ImageIcon(url);
    }

    public JButton createButton(String icon) {
        ImageIcon img = getIcon(icon);
        if (img == null) {
            return new JButton(icon);
        }
        return new JButton(img);
    }

    public JCheckBox createCheckBox(String text) {
        Icon ok = getIcon("ok.png");
        Icon cancel = getIcon("cancel.png");
        JCheckBox cb = new JCheckBox(text, cancel);
        cb.setSelectedIcon(ok);
        return cb;
    }

    /**
     * createRDFFrame
     */
    public void createRDFFrame() {
        if (authors.getAuthorCount() == 0) return;
        try {
            JFrame rdfframe = new JFrame();
            rdfframe.setTitle("application/rdf+xml");
            rdfframe.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            StringWriter strw = new StringWriter();
            print(new PrintWriter(strw, true));
            JTextArea textArea = new JTextArea(strw.toString());
            rdfframe.setContentPane(new JScrollPane(textArea));
            Rectangle r = getBounds();
            r.setBounds(r.x + 10, r.y + 10, r.width - 20, r.height - 20);
            rdfframe.setBounds(r);
            rdfframe.setVisible(true);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Cannot display RDF:\n" + e, "Error", JOptionPane.WARNING_MESSAGE);
            e.printStackTrace();
        }
    }

    Properties getFetchingNCBIProperties() {
        Properties prop = new Properties();
        Integer n = (Integer) this.listMaxFetch.getSelectedItem();
        if (n == null) n = new Integer(2);
        prop.setProperty("retmax", n.toString());
        return prop;
    }

    public void saveAs() {
        if (authors.getAuthorCount() == 0) return;
        try {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setSelectedFile(new File("foaf.xml"));
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                saveAs(fileChooser.getSelectedFile());
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Cannot save foaf file:\n" + e, "Error", JOptionPane.WARNING_MESSAGE);
        }
    }

    public void saveAs(File f) {
        if (f == null) return;
        try {
            FileWriter out = new FileWriter(f);
            print(new PrintWriter(out, true));
            out.flush();
            out.close();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Cannot save " + f + ":\n" + e, "Error", JOptionPane.WARNING_MESSAGE);
            e.printStackTrace();
        }
    }

    public void print(PrintWriter out) {
        out.println("<?xml version=\"1.0\"?>\n" + "<?xml-stylesheet type=\"text/xsl\" href=\"http://www.urbigene.com/foaf/foaf2html.xsl\" ?>\n" + "<rdf:RDF \n" + "xml:lang=\"en\" \n" + "xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"  \n" + "xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\" \n" + "xmlns=\"http://xmlns.com/foaf/0.1/\" \n" + "xmlns:foaf=\"http://xmlns.com/foaf/0.1/\" \n" + "xmlns:dc=\"http://purl.org/dc/elements/1.1/\">\n");
        out.println("<!-- generated with SciFoaf http://www.urbigene.com/foaf -->");
        if (this.mainAuthor == null && this.authors.getAuthorCount() > 0) {
            this.mainAuthor = this.authors.getAuthorAt(0);
        }
        if (this.mainAuthor != null) {
            out.println("<foaf:PersonalProfileDocument rdf:about=\"\">\n" + "\t<foaf:primaryTopic rdf:nodeID=\"" + this.mainAuthor.getID() + "\"/>\n" + "\t<foaf:maker rdf:resource=\"mailto:plindenbaum@yahoo.fr\"/>\n" + "\t<dc:title>FOAF for " + XMLUtilities.escape(this.mainAuthor.getName()) + "</dc:title>\n" + "\t<dc:description>\n" + "\tFriend-of-a-Friend description for " + XMLUtilities.escape(this.mainAuthor.getName()) + "\n" + "\t</dc:description>\n" + "</foaf:PersonalProfileDocument>\n\n");
        }
        for (int i = 0; i < this.laboratories.size(); ++i) {
            Laboratory lab = this.laboratories.getLabAt(i);
            out.println("<foaf:Group rdf:ID=\"laboratory_ID" + i + "\" >");
            out.println("\t<foaf:name>" + XMLUtilities.escape(lab.toString()) + "</foaf:name>");
            for (int j = 0; j < lab.getAuthorCount(); ++j) {
                out.println("\t<foaf:member rdf:resource=\"#" + lab.getAuthorAt(j).getID() + "\" />");
            }
            out.println("</foaf:Group>\n\n");
        }
        for (int i = 0; i < this.authors.size(); ++i) {
            Author author = authors.getAuthorAt(i);
            out.println("<foaf:Person rdf:ID=\"" + xmlName(author.getID()) + "\" >");
            out.println("\t<foaf:name>" + xmlName(author.getName()) + "</foaf:name>");
            out.println("\t<foaf:title>Dr</foaf:title>");
            out.println("\t<foaf:family_name>" + xmlName(author.getLastName()) + "</foaf:family_name>");
            if (author.getForeName() != null && author.getForeName().length() > 2) {
                out.println("\t<foaf:firstName>" + xmlName(author.getForeName()) + "</foaf:firstName>");
            }
            String prop = author.getProperty("foaf:mbox");
            if (prop != null) {
                String tokens[] = prop.split("[\t ]+");
                for (int j = 0; j < tokens.length; ++j) {
                    if (tokens[j].trim().length() == 0) continue;
                    if (tokens[j].equals("mailto:")) continue;
                    if (!tokens[j].startsWith("mailto:")) tokens[j] = "mailto:" + tokens[j];
                    try {
                        MessageDigest md = MessageDigest.getInstance("SHA");
                        md.update(tokens[j].getBytes());
                        byte[] digest = md.digest();
                        out.print("\t<foaf:mbox_sha1sum>");
                        for (int k = 0; k < digest.length; k++) {
                            String hex = Integer.toHexString(digest[k]);
                            if (hex.length() == 1) hex = "0" + hex;
                            hex = hex.substring(hex.length() - 2);
                            out.print(hex);
                        }
                        out.println("</foaf:mbox_sha1sum>");
                    } catch (Exception err) {
                        out.println("\t<foaf:mbox rdf:resource=\"" + tokens[j] + "\" />");
                    }
                }
            }
            prop = author.getProperty("foaf:nick");
            if (prop != null) {
                String tokens[] = prop.split("[\t ]+");
                for (int j = 0; j < tokens.length; ++j) {
                    if (tokens[j].trim().length() == 0) continue;
                    out.println("\t<foaf:surname>" + XMLUtilities.escape(tokens[j]) + "</foaf:surname>");
                }
            }
            prop = author.getProperty("foaf:homepage");
            if (prop != null) {
                String tokens[] = prop.split("[\t ]+");
                for (int j = 0; j < tokens.length; ++j) {
                    if (!tokens[j].trim().startsWith("http://")) continue;
                    if (tokens[j].trim().equals("http://")) continue;
                    out.println("\t<foaf:homepage  rdf:resource=\"" + XMLUtilities.escape(tokens[j].trim()) + "\"/>");
                }
            }
            out.println("\t<foaf:publications rdf:resource=\"http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=pubmed&amp;cmd=Search&amp;itool=pubmed_Abstract&amp;term=" + author.getTerm() + "\"/>");
            prop = author.getProperty("foaf:img");
            if (prop != null) {
                String tokens[] = prop.split("[\t ]+");
                for (int j = 0; j < tokens.length; ++j) {
                    if (!tokens[j].trim().startsWith("http://")) continue;
                    if (tokens[j].trim().equals("http://")) continue;
                    out.println("\t<foaf:depiction rdf:resource=\"" + XMLUtilities.escape(tokens[j].trim()) + "\"/>");
                }
            }
            AuthorList knows = this.whoknowwho.getKnown(author);
            for (int j = 0; j < knows.size(); ++j) {
                out.println("\t<foaf:knows rdf:resource=\"#" + xmlName(knows.getAuthorAt(j).getID()) + "\" />");
            }
            Paper publications[] = this.papers.getAuthorPublications(author).toArray();
            if (!(publications.length == 0)) {
                HashSet meshes = new HashSet();
                for (int j = 0; j < publications.length; ++j) {
                    meshes.addAll(publications[j].meshTerms);
                }
                for (Iterator itermesh = meshes.iterator(); itermesh.hasNext(); ) {
                    MeshTerm meshterm = (MeshTerm) itermesh.next();
                    out.println("\t<foaf:interest>\n" + "\t\t<rdf:Description rdf:about=\"" + meshterm.getURL() + "\">\n" + "\t\t\t<dc:title>" + XMLUtilities.escape(meshterm.toString()) + "</dc:title>\n" + "\t\t</rdf:Description>\n" + "\t</foaf:interest>");
                }
            }
            out.println("</foaf:Person>\n\n");
        }
        Paper paperarray[] = this.papers.toArray();
        for (int i = 0; i < paperarray.length; ++i) {
            out.println("<foaf:Document rdf:about=\"http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&amp;db=pubmed&amp;dopt=Abstract&amp;list_uids=" + paperarray[i].getPMID() + "\">");
            out.println("<dc:title>" + XMLUtilities.escape(paperarray[i].getTitle()) + "</dc:title>");
            for (Iterator iter = paperarray[i].authors.iterator(); iter.hasNext(); ) {
                Author author = (Author) iter.next();
                out.println("<dc:author rdf:resource=\"#" + XMLUtilities.escape(author.getID()) + "\"/>");
            }
            out.println("</foaf:Document>");
        }
        out.println("</rdf:RDF>");
    }

    private static String xmlName(String s) {
        s = XMLUtilities.escape(s);
        s.replaceAll("[\\t% ]", "_");
        s.replaceAll("\\+", "");
        return s;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        try {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
            }
            SciFOAF prg = new SciFOAF();
            prg.setInformation("Starting");
            prg.setVisible(true);
        } catch (Exception err) {
            err.printStackTrace();
        }
    }
}
