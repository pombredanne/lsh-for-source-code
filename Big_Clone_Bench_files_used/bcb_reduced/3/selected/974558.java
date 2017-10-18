package potlatch;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.io.File;
import java.security.AccessControlException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathExpressionException;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.tokaf.MultipleRatings;
import org.tokaf.QueryFinder;
import org.tokaf.RatingsContainer;
import org.tokaf.TopKElement;
import org.tokaf.UserRatings;
import org.tokaf.algorithm.Algorithm;
import org.tokaf.algorithm.FaginsAlgorithm;
import org.tokaf.algorithm.NRAAlgorithm;
import org.tokaf.algorithm.NaiveAlgorithm;
import org.tokaf.algorithm.ThresholdAlgorithm;
import org.tokaf.bestcolumnfinder.DerivationInPointFinder;
import org.tokaf.bestcolumnfinder.SimpleColumnFinder;
import org.tokaf.datasearcher.DataModifier;
import org.tokaf.datasearcher.DataSearcher;
import org.tokaf.datasearcher.SesameRepositorySearcher;
import org.tokaf.datasearcher.SesameSearcher;
import org.tokaf.normalizer.Normalizer;
import org.tokaf.normalizer.SimpleNormalizer;
import org.tokaf.rater.Rater;
import org.tokaf.rater.WeightAverage;
import org.tokaf.InducedRatings;
import javax.swing.JSplitPane;

/**
 * @author Alan Eckhardt
 * 
 */
public class MainPotlatchWindow extends JFrame {

    /**
	 * 
	 */
    private static final long serialVersionUID = -7237549895512677335L;

    private JPanel jContentPane1 = null;

    private JTabbedPane jTabbedTopPane = null;

    private JPanel jPanelNastaveni = null;

    String path = "";

    String user = "";

    String password = "";

    String passwordProperty;

    String administratorProperty;

    String readOnlyProperty;

    QueryFinder queryFinder = new QueryFinder("queries.xml", "Sesame");

    SesameRepositorySearcher master = new SesameRepositorySearcher();

    HashMap windowPages = new HashMap();

    HashMap windowPagesNumbers = new HashMap();

    int K = 100;

    String algorithmName = "org.tokaf.algorithm.NaiveAlgorithm";

    private String namespace = null;

    private JPanel jPanelModify = null;

    private JPanel jPanelResults = null;

    private JScrollPane jScrollPaneResults = null;

    private JList jListResults = null;

    private ArrayList results = new ArrayList();

    private Algorithm alg = null;

    private JPanel jPanelProgress = null;

    private JProgressBar[] jProgressBars = null;

    private JButton jButtonCascadeAlgorithm = null;

    private JPanel jPanelPartialResults = null;

    private JButton jButton = null;

    private JButton jButtonDistributeRatings = null;

    protected boolean readOnly = false;

    protected boolean adminMode = false;

    protected String userRatingPredicate;

    protected String userComputedRatingPredicate;

    protected String superRatingPredicate;

    protected String superComputedRatingPredicate;

    protected String userClassName;

    /**
	 * This method initializes jContentPane1
	 * 
	 * @return javax.swing.JPanel
	 */
    private JPanel getJContentPane1() {
        if (jContentPane1 == null) {
            jContentPane1 = new JPanel();
            jContentPane1.setLayout(new BorderLayout());
            jContentPane1.add(getJTabbedTopPane(), java.awt.BorderLayout.CENTER);
        }
        return jContentPane1;
    }

    /**
	 * This method initializes jTabbedTopPane
	 * 
	 * @return javax.swing.JTabbedPane
	 */
    private JTabbedPane getJTabbedTopPane() {
        if (jTabbedTopPane == null) {
            jTabbedTopPane = new JTabbedPane();
            for (int i = 0; i < windowPagesNumbers.size(); i++) {
                String name = (String) windowPagesNumbers.get(Integer.toString(i + 1));
                JPanel a = (JPanel) windowPages.get(name);
                jTabbedTopPane.addTab(name, null, a, BorderLayout.CENTER);
            }
            jTabbedTopPane.addTab("Hled�n� kompromisu", null, getJPanelNastaveni(), BorderLayout.CENTER);
        }
        return jTabbedTopPane;
    }

    private JPanel getPage(String name) {
        javax.xml.xpath.XPathFactory factory = javax.xml.xpath.XPathFactory.newInstance();
        javax.xml.xpath.XPath xpath = factory.newXPath();
        javax.xml.xpath.XPathExpression expression = null;
        try {
            expression = xpath.compile("/Settings/Pages/Page[@name = \"" + name + "\"]/Namespace");
            String namespace = expression.evaluate(new org.xml.sax.InputSource("settings.xml"));
            expression = xpath.compile("/Settings/Pages/Page[@name = \"" + name + "\"]/TypeOfPage");
            String typeOfPage = expression.evaluate(new org.xml.sax.InputSource("settings.xml"));
            expression = xpath.compile("/Settings/Pages/Page[@name = \"" + name + "\"]/TypeOfEntity");
            String typeOfEntity = expression.evaluate(new org.xml.sax.InputSource("settings.xml"));
            int a = 1;
            ArrayList predicates = new ArrayList();
            while (true) {
                expression = xpath.compile("/Settings/Pages/Page[@name = \"" + name + "\"]/InducedPredicates/InducedPredicate[" + a + "]");
                String predicate = expression.evaluate(new org.xml.sax.InputSource("settings.xml"));
                if (predicate == null || predicate.equals("")) break;
                predicates.add(predicate);
                a++;
            }
            a = 1;
            ArrayList classes = new ArrayList();
            while (true) {
                expression = xpath.compile("/Settings/Pages/Page[@name = \"" + name + "\"]/InducedClasses/InducedClass[" + a + "]");
                String className = expression.evaluate(new org.xml.sax.InputSource("settings.xml"));
                if (className == null || className.equals("")) break;
                classes.add(className);
                a++;
            }
            expression = xpath.compile("/Settings/Pages/Page[@name = \"" + name + "\"]/InducedType");
            String inducedType = expression.evaluate(new org.xml.sax.InputSource("settings.xml"));
            InducedRatings ir = null;
            if (inducedType != null && !inducedType.equals("")) {
                String classesString[] = new String[classes.size()];
                String predicatesString[] = new String[predicates.size()];
                for (int i = 0; i < predicates.size() && i < classes.size(); i++) {
                    classesString[i] = classes.get(i).toString();
                    predicatesString[i] = predicates.get(i).toString();
                }
                ir = new InducedRatings(namespace, queryFinder, master, master, inducedType, predicatesString, classesString);
            }
            a = 1;
            String column;
            ArrayList columns = new ArrayList();
            while (a > 0) {
                expression = xpath.compile("/Settings/Pages/Page[@name = \"" + name + "\"]/ColumnNames/ColumnName[" + a + "]");
                column = expression.evaluate(new org.xml.sax.InputSource("settings.xml"));
                if (column == null || column.equals("")) break;
                columns.add(column);
                a++;
            }
            String[] columnNames = new String[columns.size()];
            for (int i = 0; i < columnNames.length; i++) columnNames[i] = (String) columns.get(i);
            if (typeOfPage.equals("SimpleEditRDFSesamePanel")) {
                SesameRepositorySearcher data = new SesameRepositorySearcher();
                data.initSource(master);
                if (ir != null) {
                    SimpleEditRDFSesamePanel serp = new SimpleEditRDFSesamePanel(readOnly, data, queryFinder, columnNames, typeOfEntity, namespace, user, userRatingPredicate, userComputedRatingPredicate);
                    serp.setInducedRatings(ir);
                    return serp;
                }
                SimpleEditRDFSesamePanel panel = new SimpleEditRDFSesamePanel(readOnly, data, queryFinder, columnNames, typeOfEntity, namespace, user, userRatingPredicate, userComputedRatingPredicate);
                return panel;
            } else if (typeOfPage.equals("EditRDFSesamePanel")) {
                SesameRepositorySearcher data = new SesameRepositorySearcher();
                data.initSource(master);
                if (ir != null) {
                    EditRDFSesamePanel serp = new EditRDFSesamePanel(readOnly, data, queryFinder, columnNames, typeOfEntity, namespace, user, userRatingPredicate, userComputedRatingPredicate);
                    serp.setInducedRatings(ir);
                    return serp;
                }
                EditRDFSesamePanel panel = new EditRDFSesamePanel(readOnly, data, queryFinder, columnNames, typeOfEntity, namespace, user, userRatingPredicate, userComputedRatingPredicate);
                return panel;
            } else if (typeOfPage.equals("VaryingEditRDFSesamePanel")) {
                SesameRepositorySearcher data = new SesameRepositorySearcher();
                data.initSource(master);
                VaryingEditRDFSesamePanel panel = new VaryingEditRDFSesamePanel(!adminMode, data, queryFinder, columnNames, typeOfEntity, namespace, user, userRatingPredicate, userComputedRatingPredicate);
                expression = xpath.compile("/Settings/Pages/Page[@name = \"" + name + "\"]/DateTimeType");
                String tempType = expression.evaluate(new org.xml.sax.InputSource("settings.xml"));
                if (tempType != null && !tempType.equals("")) panel.dateTimeType = tempType;
                expression = xpath.compile("/Settings/Pages/Page[@name = \"" + name + "\"]/DecimalType");
                tempType = expression.evaluate(new org.xml.sax.InputSource("settings.xml"));
                if (tempType != null && !tempType.equals("")) panel.decimalType = tempType;
                expression = xpath.compile("/Settings/Pages/Page[@name = \"" + name + "\"]/FloatType");
                tempType = expression.evaluate(new org.xml.sax.InputSource("settings.xml"));
                if (tempType != null && !tempType.equals("")) panel.floatType = tempType;
                expression = xpath.compile("/Settings/Pages/Page[@name = \"" + name + "\"]/IntegerType");
                tempType = expression.evaluate(new org.xml.sax.InputSource("settings.xml"));
                if (tempType != null && !tempType.equals("")) panel.integerType = tempType;
                expression = xpath.compile("/Settings/Pages/Page[@name = \"" + name + "\"]/LiteralType");
                tempType = expression.evaluate(new org.xml.sax.InputSource("settings.xml"));
                if (tempType != null && !tempType.equals("")) panel.literalType = tempType;
                return panel;
            }
        } catch (XPathExpressionException e1) {
            e1.printStackTrace();
        }
        return null;
    }

    /**
	 * This method initializes jPanelNastaveni
	 * 
	 * @return javax.swing.JPanel
	 */
    private JPanel getJPanelNastaveni() {
        if (jPanelNastaveni == null) {
            jPanelNastaveni = new JPanel();
            jPanelNastaveni.setLayout(new BorderLayout());
            jPanelNastaveni.add(getJPanelResults(), java.awt.BorderLayout.CENTER);
            jPanelNastaveni.add(getJPanelModify(), java.awt.BorderLayout.SOUTH);
        }
        return jPanelNastaveni;
    }

    private void fillComboBox(JComboBox comboBox, DataSearcher data) {
        while (data.getPosistion() != -1) {
            if (data.getField(0) instanceof URI) {
                comboBox.addItem(((URI) data.getField(0)).getLocalName());
            } else comboBox.addItem(data.getField(0).toString());
            if (data.advance() == -1) break;
        }
    }

    /**
	 * This method initializes jPanelModify
	 * 
	 * @return javax.swing.JPanel
	 */
    private JPanel getJPanelModify() {
        if (jPanelModify == null) {
            jPanelModify = new JPanel();
            jPanelModify.setPreferredSize(new java.awt.Dimension(10, 40));
            jPanelModify.add(getJButtonCascadeAlgorithm(), null);
            jPanelModify.add(getJButton(), null);
            jPanelModify.add(getJButtonDistributeRatings(), null);
        }
        return jPanelModify;
    }

    /**
	 * This method initializes jPanelResults
	 * 
	 * @return javax.swing.JPanel
	 */
    private JPanel getJPanelResults() {
        if (jPanelResults == null) {
            GridLayout gridLayout = new GridLayout();
            gridLayout.setRows(1);
            jPanelResults = new JPanel();
            jPanelResults.setLayout(gridLayout);
            jPanelResults.add(getJSplitPaneResulrs(), null);
        }
        return jPanelResults;
    }

    /**
	 * This method initializes jScrollPaneResults
	 * 
	 * @return javax.swing.JScrollPane
	 */
    private JScrollPane getJScrollPaneResults() {
        if (jScrollPaneResults == null) {
            jScrollPaneResults = new JScrollPane();
            jScrollPaneResults.setViewportView(getJListResults());
        }
        return jScrollPaneResults;
    }

    /**
	 * This method initializes jListResults
	 * 
	 * @return javax.swing.JList
	 */
    private JList getJListResults() {
        if (jListResults == null) {
            ArrayListTopKElementModel model = new ArrayListTopKElementModel(results);
            jListResults = new JList(model);
            jListResults.addListSelectionListener(new javax.swing.event.ListSelectionListener() {

                public void valueChanged(javax.swing.event.ListSelectionEvent e) {
                    int index = jListResults.getSelectedIndex();
                    ArrayListTopKElementModel model = (ArrayListTopKElementModel) jListResults.getModel();
                    TopKElement el = model.getElement(index);
                    for (int i = 0; i < jPanelPartialResults.getComponentCount(); i++) {
                        JPanel panel = (JPanel) jPanelPartialResults.getComponent(i);
                        Component[] comp = panel.getComponents();
                        for (int j = 0; j < comp.length; j++) {
                            if (comp[j] instanceof JScrollPane) {
                                JScrollPane partialResults = (JScrollPane) comp[j];
                                JViewport port = (JViewport) partialResults.getComponent(0);
                                JList list = (JList) port.getComponent(0);
                                int ind = ((ArrayListTopKElementModel) list.getModel()).getElementIndex(el);
                                if (ind != -1) list.setSelectedIndex(ind); else {
                                    for (int k = 0; k < el.getLength(); k++) {
                                        if (!el.isNull(k)) ind = ((ArrayListTopKElementModel) list.getModel()).getElementIndex(el.getRatingObject(k).toString());
                                        if (ind != -1) {
                                            list.setSelectedIndex(ind);
                                            break;
                                        }
                                    }
                                    if (ind == -1) list.setSelectedIndices(new int[0]);
                                }
                            }
                        }
                    }
                }
            });
        }
        return jListResults;
    }

    private Algorithm getAlg(DataSearcher[] data, Rater r, Normalizer[] norm, int pocet) {
        alg = new NaiveAlgorithm(data, r, new SimpleColumnFinder(), namespace, pocet);
        return alg;
    }

    private double[] getPreferencesOnClass(String type, String[] ratings) {
        SesameRepositorySearcher search = new SesameRepositorySearcher();
        search.initSource(master);
        ArrayList params = new ArrayList();
        params.add("Hodnocen�");
        params.add(namespace + type);
        String preferQuery = queryFinder.getQuery("GetPreferencesForClass", 1, params);
        search.initSearch(preferQuery);
        double vahy[] = new double[ratings.length];
        while (search.hasNext()) {
            Object pref = search.getField(0);
            Object rat = search.getField(1);
            for (int i = 0; i < vahy.length; i++) {
                if (ratings[i].equals(rat.toString())) {
                    try {
                        vahy[i] = Double.parseDouble(pref.toString());
                    } catch (Exception ex) {
                        vahy[i] = 0;
                    }
                }
            }
            if (search.advance() == -1) break;
        }
        return vahy;
    }

    private long initSearch(String type) {
        Calendar zacatek = Calendar.getInstance();
        ArrayList params = new ArrayList();
        params.add("Hodnocen�");
        String predicateQuery = queryFinder.getQuery("GetListOfPredicates", 1, params);
        SesameRepositorySearcher predData = new SesameRepositorySearcher();
        predData.initSource(master);
        predData.initSearch(predicateQuery);
        ArrayList columns = new ArrayList();
        ArrayList predicates = new ArrayList();
        while (predData.hasNext()) {
            SesameRepositorySearcher newData = new SesameRepositorySearcher();
            newData.initSource(master);
            columns.add(newData);
            params = new ArrayList();
            params.add(predData.getField(0).toString());
            params.add(namespace + type);
            String tripQuery = queryFinder.getQuery("GetListOfTypedEntities", 2, params);
            newData.initSearch(tripQuery);
            newData.sort(2);
            predicates.add(predData.getField(0).toString());
            if (predData.advance() == -1) break;
        }
        double vahy[] = new double[columns.size()];
        Object[] o = predicates.toArray();
        Normalizer[] norm = new Normalizer[columns.size()];
        String[] s = new String[columns.size()];
        DataSearcher[] data = new DataSearcher[columns.size()];
        for (int i = 0; i < columns.size(); i++) {
            norm[i] = new SimpleNormalizer();
            s[i] = o[i].toString();
            data[i] = (DataSearcher) columns.get(i);
        }
        vahy = getPreferencesOnClass(type, s);
        WeightAverage r = new WeightAverage(s, vahy);
        int pocet = 10;
        pocet = K;
        getAlg(data, r, norm, pocet);
        Calendar konec = Calendar.getInstance();
        return konec.getTimeInMillis() - zacatek.getTimeInMillis();
    }

    /**
	 * This method initializes jPanelProgress
	 * 
	 * @return javax.swing.JPanel
	 */
    private JPanel getJPanelProgress() {
        if (jPanelProgress == null) {
            jPanelProgress = new JPanel();
            jPanelProgress.setPreferredSize(new java.awt.Dimension(100, 100));
        }
        return jPanelProgress;
    }

    /**
	 * This method initializes jButtonCascadeAlgorithm
	 * 
	 * @return javax.swing.JButton
	 */
    private JButton getJButtonCascadeAlgorithm() {
        if (jButtonCascadeAlgorithm == null) {
            jButtonCascadeAlgorithm = new JButton();
            jButtonCascadeAlgorithm.setText("Hodnocen� lokalit");
            jButtonCascadeAlgorithm.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent e) {
                    try {
                        int pocet = K;
                        String[] types = new String[] { "http://www.muaddib.wz.cz/Ter�n", "http://www.muaddib.wz.cz/V�let", "http://www.muaddib.wz.cz/M�sto" };
                        String[] properties = new String[] { "http://www.muaddib.wz.cz/m�Ter�n", "http://www.muaddib.wz.cz/bylaC�lemV�letu", "http://www.muaddib.wz.cz/m�lZa��tekV" };
                        MultipleRatings mr = new MultipleRatings(master, master, queryFinder, namespace, algorithmName, pocet);
                        double[] weightsFinal = getAverageRating(types, mr.getUsers());
                        ArrayList res = mr.getFirstLocal("V�letn�Lokalita", types, weightsFinal, properties);
                        writeTopList(res);
                        DataSearcher[] subTypes = mr.getTypeBySubtypes("V�letn�Lokalita", types, weightsFinal, properties);
                        ArrayList al[] = new ArrayList[subTypes.length];
                        jPanelPartialResults.removeAll();
                        for (int i = 0; i < al.length; i++) {
                            al[i] = new ArrayList();
                            while (subTypes[i].hasNext()) {
                                TopKElement el = new TopKElement(subTypes[i].getField(0), subTypes[i].getNormalizer().Normalize(subTypes[i].getField(2)));
                                al[i].add(el);
                                subTypes[i].advance();
                            }
                            JPanel panel = new JPanel();
                            panel.setLayout(new BorderLayout());
                            JLabel label = new JLabel();
                            try {
                                URIImpl uri = new URIImpl(subTypes[i].getName());
                                label.setText(uri.getLocalName());
                            } catch (java.lang.IllegalArgumentException ex) {
                                label.setText(subTypes[i].getName());
                            }
                            JScrollPane partialResults = new JScrollPane();
                            JList list = new JList(new ArrayListTopKElementModel(al[i]));
                            partialResults.setViewportView(list);
                            panel.add(label, java.awt.BorderLayout.NORTH);
                            panel.add(partialResults, java.awt.BorderLayout.CENTER);
                            jPanelPartialResults.add(panel, null);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    jPanelPartialResults.updateUI();
                    jListResults = null;
                    jScrollPaneResults.setViewportView(getJListResults());
                    alg = null;
                }
            });
        }
        return jButtonCascadeAlgorithm;
    }

    /**
	 * This method initializes jPanelPartialResults
	 * 
	 * @return javax.swing.JPanel
	 */
    private JPanel getJPanelPartialResults() {
        if (jPanelPartialResults == null) {
            jPanelPartialResults = new JPanel();
            GridLayout gridLayout = new GridLayout();
            gridLayout.setRows(2);
            jPanelPartialResults.setLayout(gridLayout);
        }
        return jPanelPartialResults;
    }

    private double[] getAverageRating(String[] classes, String[] users) {
        UserRatings ur = new UserRatings(namespace, queryFinder, master, master);
        double[][] weights = new double[users.length][];
        for (int i = 0; i < weights.length; i++) {
            weights[i] = new double[classes.length];
            ur.setUserName(users[i]);
            for (int j = 0; j < classes.length; j++) {
                try {
                    if (ur.getRatingName() != null) weights[i][j] = ur.getUserRatingOnEntity(classes[j]); else weights[i][j] = 0;
                } catch (Exception ex) {
                    weights[i][j] = 0;
                }
            }
        }
        double[] weightsFinal = new double[classes.length];
        for (int j = 0; j < classes.length; j++) {
            weightsFinal[j] = 0;
            for (int i = 0; i < weights.length; i++) {
                weightsFinal[j] += weights[i][j];
            }
            weightsFinal[j] /= weights.length;
        }
        return weightsFinal;
    }

    /**
	 * This method initializes jButton
	 * 
	 * @return javax.swing.JButton
	 */
    private JButton getJButton() {
        if (jButton == null) {
            jButton = new JButton();
            jButton.setText("Hodnocen� atribut�");
            jButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent e) {
                    try {
                        int pocet = K;
                        String[] types = new String[] { "http://www.muaddib.wz.cz/Ter�n", "http://www.muaddib.wz.cz/V�let", "http://www.muaddib.wz.cz/M�sto" };
                        String[] properties = new String[] { "http://www.muaddib.wz.cz/m�Ter�n", "http://www.muaddib.wz.cz/bylaC�lemV�letu", "http://www.muaddib.wz.cz/m�lZa��tekV" };
                        MultipleRatings mr = new MultipleRatings((SesameSearcher) master.clone(), (SesameSearcher) master.clone(), queryFinder, namespace, algorithmName, pocet);
                        double[] weightsFinal = getAverageRating(types, mr.getUsers());
                        ArrayList res = mr.getFirstGlobal("V�letn�Lokalita", types, weightsFinal, properties);
                        writeTopList(res);
                        ArrayList sub[] = new ArrayList[types.length];
                        jPanelPartialResults.removeAll();
                        for (int i = 0; i < sub.length; i++) {
                            Algorithm a = mr.getTypeByGlobalPreferences(types[i]);
                            sub[i] = a.getTopK();
                            JPanel panel = new JPanel();
                            panel.setLayout(new BorderLayout());
                            JLabel label = new JLabel();
                            try {
                                URIImpl uri = new URIImpl(types[i]);
                                label.setText(uri.getLocalName());
                            } catch (java.lang.IllegalArgumentException ex) {
                                label.setText(types[i]);
                            }
                            JScrollPane partialResults = new JScrollPane();
                            JList list = new JList(new ArrayListTopKElementModel(sub[i]));
                            partialResults.setViewportView(list);
                            panel.add(label, java.awt.BorderLayout.NORTH);
                            panel.add(partialResults, java.awt.BorderLayout.CENTER);
                            jPanelPartialResults.add(panel, null);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    jPanelPartialResults.updateUI();
                    jListResults = null;
                    jScrollPaneResults.setViewportView(getJListResults());
                    alg = null;
                }
            });
        }
        return jButton;
    }

    private void copyRatings(String[] userRating, String[] newUserRating, String className) {
        UserRatings ur = new UserRatings(namespace, queryFinder, master, master);
        for (int i = 0; i < userRating.length && i < newUserRating.length; i++) {
            ur.setRatingNames(userRating[i], newUserRating[i]);
            ur.copyUserRatingsOnClass(className, false);
        }
    }

    private void copyRatings(String[] types, String[] properties, String subType, MultipleRatings mr, String[] userRating, String[] newUserRating) {
        try {
            for (int i = 0; i < userRating.length && i < newUserRating.length; i++) {
                mr.copyRatingsToComputedRatings(userRating[i], newUserRating[i], properties, "http://www.w3.org/1999/02/22-rdf-syntax-ns#Property");
                mr.copyRatingsToComputedRatings(userRating[i], newUserRating[i], types, "http://www.w3.org/2000/01/rdf-schema#Class");
                mr.copyRatingsToComputedRatings(userRating[i], newUserRating[i], new String[] { subType }, "http://www.w3.org/2000/01/rdf-schema#Class");
                mr.copyRatingsToComputedRatings(userRating[i], newUserRating[i], subType);
                mr.copyRatingsToComputedRatings(userRating[i], newUserRating[i], types[0]);
            }
        } catch (Exception e1) {
            System.out.println("** Error during updating ratings **");
            e1.printStackTrace();
        }
    }

    /**
	 * This method initializes jButtonDistributeRatings
	 * 
	 * @return javax.swing.JButton
	 */
    private JButton getJButtonDistributeRatings() {
        if (jButtonDistributeRatings == null) {
            jButtonDistributeRatings = new JButton();
            jButtonDistributeRatings.setText("Spo��tat hodnocen�");
            jButtonDistributeRatings.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent e) {
                    int pocet = K;
                    MultipleRatings mr = new MultipleRatings((SesameSearcher) master.clone(), (SesameSearcher) master.clone(), queryFinder, namespace, algorithmName, pocet);
                    String[] computedUserRatings = mr.getRatings(true);
                    String[] userRatings = mr.getRatings(false);
                    Arrays.sort(userRatings);
                    Arrays.sort(computedUserRatings);
                    copyRatings(userRatings, computedUserRatings, "http://www.w3.org/2000/01/rdf-schema#Resource");
                    String[] types = new String[] { "http://www.muaddib.wz.cz/Terrain", "http://www.muaddib.wz.cz/Trip", "http://www.muaddib.wz.cz/City" };
                    String[] properties = new String[] { "http://www.muaddib.wz.cz/hasTerrain", "http://www.muaddib.wz.cz/wasTrippedBy", "http://www.muaddib.wz.cz/hasStartingCity" };
                    mr = new MultipleRatings((SesameSearcher) master.clone(), (SesameSearcher) master.clone(), queryFinder, namespace, algorithmName, pocet);
                    userRatings = mr.getRatings(false);
                    String[] normUserRatings = mr.getRatings(true);
                    String[] users = mr.getUsers();
                    Arrays.sort(userRatings);
                    Arrays.sort(normUserRatings);
                    types = new String[] { "http://www.muaddib.wz.cz/Trip" };
                    String subType = "http://www.muaddib.wz.cz/City";
                    properties = new String[] { "http://www.muaddib.wz.cz/hasStartingCity" };
                    types = new String[] { "http://www.muaddib.wz.cz/V�let" };
                    subType = "http://www.muaddib.wz.cz/M�sto";
                    properties = new String[] { "http://www.muaddib.wz.cz/m�lZa��tekV" };
                    InducedRatings ir = new InducedRatings(namespace, queryFinder, (SesameSearcher) master.clone(), (SesameSearcher) master.clone(), subType, properties, types);
                    for (int i = 0; i < users.length; i++) {
                        try {
                            ir.setIducedRatingOnClass(users[i]);
                        } catch (Exception e1) {
                            System.out.println("** Error during updating ratings **");
                            e1.printStackTrace();
                        }
                    }
                    types = new String[] { "http://www.muaddib.wz.cz/Trip" };
                    subType = "http://www.muaddib.wz.cz/TripLocation";
                    properties = new String[] { "http://www.muaddib.wz.cz/hasTripLocation" };
                    types = new String[] { "http://www.muaddib.wz.cz/V�let" };
                    subType = "http://www.muaddib.wz.cz/V�letn�Lokalita";
                    properties = new String[] { "http://www.muaddib.wz.cz/bylVLokalit�" };
                    ir = new InducedRatings(namespace, queryFinder, (SesameSearcher) master.clone(), (SesameSearcher) master.clone(), subType, properties, types);
                    for (int i = 0; i < users.length; i++) {
                        try {
                            ir.setIducedRatingOnClass(users[i]);
                        } catch (Exception e1) {
                            System.out.println("** Error during updating ratings **");
                            e1.printStackTrace();
                        }
                    }
                    types = new String[] { "http://www.muaddib.wz.cz/TripLocation" };
                    subType = "http://www.muaddib.wz.cz/Terrain";
                    properties = new String[] { "http://www.muaddib.wz.cz/hasTerrain" };
                    types = new String[] { "http://www.muaddib.wz.cz/V�letn�Lokalita" };
                    subType = "http://www.muaddib.wz.cz/Ter�n";
                    properties = new String[] { "http://www.muaddib.wz.cz/m�Ter�n" };
                    ir = new InducedRatings(namespace, queryFinder, (SesameSearcher) master.clone(), (SesameSearcher) master.clone(), subType, properties, types);
                    for (int i = 0; i < users.length; i++) {
                        try {
                            ir.setIducedRatingOnClass(users[i]);
                        } catch (Exception e1) {
                            System.out.println("** Error during updating ratings **");
                            e1.printStackTrace();
                        }
                    }
                }
            });
        }
        return jButtonDistributeRatings;
    }

    /**
	 * This method initializes jJMenuBar
	 * 
	 * @return javax.swing.JMenuBar
	 */
    private JMenuBar getJJMenuBar() {
        if (jJMenuBar == null) {
            jJMenuBar = new JMenuBar();
            jJMenuBar.add(getJMainSystemMenu());
            jJMenuBar.add(getJMainSettingsMenu());
        }
        return jJMenuBar;
    }

    /**
	 * This method initializes jSplitPaneResulrs
	 * 
	 * @return javax.swing.JSplitPane
	 */
    private JSplitPane getJSplitPaneResulrs() {
        if (jSplitPaneResulrs == null) {
            jSplitPaneResulrs = new JSplitPane();
            jSplitPaneResulrs.setRightComponent(getJPanelPartialResults());
            jSplitPaneResulrs.setLeftComponent(getJScrollPaneResults());
        }
        return jSplitPaneResulrs;
    }

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        JFrame.setDefaultLookAndFeelDecorated(true);
        MainPotlatchWindow a = new MainPotlatchWindow();
        a.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        a.setVisible(true);
    }

    private Node getSubNodesByName(NodeList nodeList, String name) {
        if (nodeList == null) return null;
        for (int i = 0; i < nodeList.getLength(); i++) {
            if (nodeList.item(i).getNodeName().equals(name)) return nodeList.item(i);
        }
        return null;
    }

    private String getValue(String value, Node settingsNode) {
        NodeList methods = settingsNode.getChildNodes();
        Node pathNode = getSubNodesByName(methods, value);
        pathNode = pathNode.getFirstChild();
        if (pathNode == null) return "";
        return pathNode.getNodeValue();
    }

    private void parseBaseSettings() {
        try {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(new File("settings.xml"));
            doc.getDocumentElement().normalize();
            NodeList listOfSettings = doc.getElementsByTagName("Settings");
            Node settingsNode = listOfSettings.item(0);
            path = getValue("Path", settingsNode);
            master.initSource(path);
            namespace = getValue("Namespace", settingsNode);
            userRatingPredicate = getValue("UserRatingPredicate", settingsNode);
            userComputedRatingPredicate = getValue("UserComputedRatingPredicate", settingsNode);
            superRatingPredicate = getValue("SuperRatingPredicate", settingsNode);
            superComputedRatingPredicate = getValue("SuperComputedRatingPredicate", settingsNode);
            userClassName = getValue("UserClassName", settingsNode);
            passwordProperty = getValue("PasswordProperty", settingsNode);
            administratorProperty = getValue("AdministratorProperty", settingsNode);
            readOnlyProperty = getValue("ReadOnlyProperty", settingsNode);
            RatingsContainer.computedRatingPredicate = this.userComputedRatingPredicate;
            RatingsContainer.ratingPredicate = this.userRatingPredicate;
            RatingsContainer.ratingName = superRatingPredicate;
            RatingsContainer.computedRatingName = superComputedRatingPredicate;
            RatingsContainer.userClassName = userClassName;
        } catch (SAXParseException err) {
            System.out.println("** Parsing error" + ", line " + err.getLineNumber() + ", uri " + err.getSystemId());
            System.out.println(" " + err.getMessage());
        } catch (SAXException e) {
            Exception x = e.getException();
            ((x == null) ? e : x).printStackTrace();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void parseGUISettings() {
        try {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(new File("settings.xml"));
            doc.getDocumentElement().normalize();
            NodeList listOfSettings = doc.getElementsByTagName("Settings");
            Node settingsNode = listOfSettings.item(0);
            NodeList tmp = settingsNode.getChildNodes();
            for (int i = 0; i < tmp.getLength(); i++) {
                Node pagesNode = tmp.item(i);
                if (pagesNode.getNodeName().equals("Pages")) {
                    NodeList pages = pagesNode.getChildNodes();
                    for (int j = 0; j < pages.getLength(); j++) {
                        Node page = pages.item(j);
                        if (page.getNodeName().equals("Page")) {
                            NamedNodeMap names = page.getAttributes();
                            Node name = names.getNamedItem("name");
                            Node number = names.getNamedItem("number");
                            String namee = name.getNodeValue();
                            String numbere = number.getNodeValue();
                            JPanel a = this.getPage(namee);
                            windowPages.put(namee, a);
                            windowPagesNumbers.put(numbere, namee);
                        }
                    }
                }
            }
        } catch (SAXParseException err) {
            System.out.println("** Parsing error" + ", line " + err.getLineNumber() + ", uri " + err.getSystemId());
            System.out.println(" " + err.getMessage());
        } catch (SAXException e) {
            Exception x = e.getException();
            ((x == null) ? e : x).printStackTrace();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private String getBytes(String in) throws NoSuchAlgorithmException {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(in.getBytes());
        byte[] passWordBytes = md5.digest();
        String s = "[";
        for (int i = 0; i < passWordBytes.length; i++) s += passWordBytes[i] + ", ";
        s = s.substring(0, s.length() - 2);
        s += "]";
        return s;
    }

    private boolean isReadOnly(String user) {
        SesameRepositorySearcher data = new SesameRepositorySearcher();
        data.initSource(master);
        ArrayList params = new ArrayList();
        try {
            URIImpl uri = new URIImpl(user);
            params.add(user);
        } catch (java.lang.IllegalArgumentException ex) {
            params.add(namespace + user);
        }
        params.add(namespace + readOnlyProperty);
        String query = queryFinder.getQuery("GetListOfEntities", 2, params);
        data.initSearch(query);
        if (data.hasNext()) {
            String value = data.getField(2).toString();
            if (value.toLowerCase().equals("true".toLowerCase())) return true;
            data.advance();
        }
        return false;
    }

    private boolean isAdmin(String user) {
        SesameRepositorySearcher data = new SesameRepositorySearcher();
        data.initSource(master);
        ArrayList params = new ArrayList();
        try {
            URIImpl uri = new URIImpl(user);
            params.add(user);
        } catch (java.lang.IllegalArgumentException ex) {
            params.add(namespace + user);
        }
        params.add(namespace + administratorProperty);
        String query = queryFinder.getQuery("GetListOfEntities", 2, params);
        data.initSearch(query);
        if (data.hasNext()) {
            String value = data.getField(2).toString();
            if (value.toLowerCase().equals("true".toLowerCase())) return true;
            data.advance();
        }
        return false;
    }

    private void checkPassword(String user, String password) {
        SesameRepositorySearcher data = new SesameRepositorySearcher();
        data.initSource(master);
        ArrayList params = new ArrayList();
        try {
            URIImpl uri = new URIImpl(user);
            params.add(user);
        } catch (java.lang.IllegalArgumentException ex) {
            params.add(namespace + user);
        }
        params.add(namespace + passwordProperty);
        String query = queryFinder.getQuery("GetListOfEntities", 2, params);
        data.initSearch(query);
        if (data.hasNext()) {
            String pass = data.getField(2).toString();
            try {
                String s = getBytes(password);
                if (!(s).equals(pass)) {
                    throw new AccessControlException("Nespr�vn� heslo.");
                }
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        } else throw new AccessControlException("U�ivatel \"" + user + "\" neexistuje.");
    }

    /**
	 * This is the default constructor
	 */
    public MainPotlatchWindow() {
        super();
        parseBaseSettings();
        UserDialog.ConnectOptionNames = new String[] { "P�ihl�sit", "Zru�it" };
        UserDialog.ConnectTitle = "P�ihl�en� k Potlatch";
        UserDialog a = new UserDialog(new String[] { "Jm�no: ", "Heslo: " }, new boolean[] { false, true });
        if (a.result != 0) {
            System.exit(1);
        }
        while (true) {
            user = a.fields[0];
            password = a.fields[1];
            try {
                checkPassword(user, password);
            } catch (AccessControlException e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Chyba p�i p�ihla�ov�n�", JOptionPane.ERROR_MESSAGE);
                a.getIDandPassword(new String[] { user });
                continue;
            }
            adminMode = isAdmin(user);
            readOnly = isReadOnly(user);
            break;
        }
        parseGUISettings();
        initialize();
    }

    /**
	 * This method initializes this
	 * 
	 * @return void
	 */
    private void initialize() {
        this.setTitle("Potlatch");
        this.setJMenuBar(getJJMenuBar());
        this.setContentPane(getJContentPane1());
        this.setPreferredSize(new java.awt.Dimension(650, 219));
        this.setMinimumSize(new java.awt.Dimension(680, 129));
        this.setSize(new java.awt.Dimension(771, 467));
    }

    public void writeTopList(ArrayList topKlist) {
        results = new ArrayList();
        for (int i = 0; i < topKlist.size(); i++) {
            URIImpl impl = new URIImpl(((TopKElement) topKlist.get(i)).name.toString());
            String s = impl.getLocalName();
            s += " - ";
            double d = ((TopKElement) topKlist.get(i)).rating;
            d *= 100;
            d = Math.round(d);
            d /= 100;
            s += Double.toString(d) + "\n";
            results.add(((TopKElement) topKlist.get(i)));
        }
        jListResults = null;
        jScrollPaneResults.setViewportView(getJListResults());
    }

    public void writeWindowOutput(ArrayList topKlist, Algorithm alg, DataSearcher[] data, long initTime, long runTime) {
        writeTopList(topKlist);
    }

    private JMenu jMainSettingsMenu = null;

    private JMenu jMainSystemMenu = null;

    private JMenuItem jMenuItemHeslo = null;

    private JMenuBar jJMenuBar = null;

    private JSplitPane jSplitPaneResulrs = null;

    /**
	 * This method initializes jMainMenu
	 * 
	 * @return javax.swing.JMenu
	 */
    private JMenu getJMainSystemMenu() {
        if (jMainSystemMenu == null) {
            jMainSystemMenu = new JMenu();
            jMainSystemMenu.setText("Syst�m");
            JMenuItem itemTemp = new JMenuItem("Konec", new ImageIcon("images/ctxsample_hide.gif"));
            itemTemp.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent e) {
                    System.exit(0);
                }
            });
            jMainSystemMenu.add(itemTemp);
        }
        return jMainSystemMenu;
    }

    /**
	 * This method initializes jMainMenu
	 * 
	 * @return javax.swing.JMenu
	 */
    private JMenu getJMainSettingsMenu() {
        if (jMainSettingsMenu == null) {
            jMainSettingsMenu = new JMenu();
            jMainSettingsMenu.setText("Nastaven�");
            jMainSettingsMenu.add(getJMenuItemHeslo());
        }
        return jMainSettingsMenu;
    }

    /**
	 * This method initializes jMenuItemHeslo
	 * 
	 * @return javax.swing.JMenuItem
	 */
    private JMenuItem getJMenuItemHeslo() {
        if (jMenuItemHeslo == null) {
            jMenuItemHeslo = new JMenuItem("Heslo", new ImageIcon("images/ctxsample_hide.gif"));
            jMenuItemHeslo.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent e) {
                    UserDialog.ConnectOptionNames = new String[] { "Ulo�it", "Zru�it" };
                    UserDialog.ConnectTitle = "Zm�na hesla";
                    UserDialog a = new UserDialog(new String[] { "Star� heslo: ", "Nov� heslo: ", "Potvr�te nov� heslo: " }, new boolean[] { true, true, true });
                    if (a.result != 0) {
                        return;
                    }
                    try {
                        checkPassword(user, a.fields[0]);
                    } catch (AccessControlException ex) {
                        JOptionPane.showMessageDialog((Component) e.getSource(), ex.getMessage(), "Chyba p�i zm�n� hesla", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    if (!a.fields[1].equals(a.fields[2])) {
                        JOptionPane.showMessageDialog((Component) e.getSource(), "Nov� hesla se neshoduj�.", "Chyba p�i zm�n� hesla", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    try {
                        String bytes = getBytes(a.fields[1]);
                        master.deleteTriple(namespace + user, namespace + passwordProperty, getBytes(password), "http://www.w3.org/2001/XMLSchema#string");
                        master.deleteTriple(namespace + user, namespace + passwordProperty, getBytes(password));
                        master.addTriple(namespace + user, namespace + passwordProperty, bytes, "http://www.w3.org/2001/XMLSchema#string");
                    } catch (NoSuchAlgorithmException e1) {
                        e1.printStackTrace();
                    }
                }
            });
        }
        return jMenuItemHeslo;
    }
}

class ArrayListTopKElementModel extends DefaultListModel {

    /**
	 * 
	 */
    private static final long serialVersionUID = -8988325563821540060L;

    public ArrayListTopKElementModel(ArrayList al) {
        this.al = al;
    }

    private ArrayList al;

    public ArrayList getArrayList() {
        return al;
    }

    public ArrayList setArrayList(ArrayList al) {
        this.al = al;
        return al;
    }

    public int getSize() {
        return al.size();
    }

    public Object getElementAt(int index) {
        if (index >= al.size()) return null;
        URIImpl impl = new URIImpl(((TopKElement) al.get(index)).name.toString());
        String s = impl.getLocalName();
        s += " - ";
        double d = ((TopKElement) al.get(index)).rating;
        d *= 100;
        d = Math.round(d);
        d /= 100;
        s += Double.toString(d) + "\n";
        return s;
    }

    public int getElementIndex(TopKElement el) {
        for (int i = 0; i < al.size(); i++) {
            if (((TopKElement) al.get(i)).name.equals(el.name)) {
                return i;
            }
        }
        return -1;
    }

    public int getElementIndex(String name) {
        for (int i = 0; i < al.size(); i++) {
            if (((TopKElement) al.get(i)).name.equals(name)) {
                return i;
            }
        }
        return -1;
    }

    public TopKElement getElement(int index) {
        return (TopKElement) al.get(index);
    }
}

;

/**
 * 
 * @author http://www.cs.cornell.edu/html/cs513-sp99/proj.02.LoginDialog.html
 * 
 */
class UserDialog {

    public String fields[];

    String captions[];

    boolean[] passwords;

    int result;

    UserDialog(String[] captions, boolean[] passwords) {
        this.captions = captions;
        this.passwords = passwords;
        fields = new String[captions.length];
        getIDandPassword(null);
    }

    static String[] ConnectOptionNames = { "P�ihl�sit", "Zru�it" };

    static String ConnectTitle = "P�ihl�en� k Potlatch";

    void getIDandPassword(String[] captionsEdit) {
        JPanel connectionPanel;
        connectionPanel = new JPanel(false);
        connectionPanel.setLayout(new BoxLayout(connectionPanel, BoxLayout.X_AXIS));
        JPanel namePanel = new JPanel(false);
        namePanel.setLayout(new GridLayout(0, 1));
        for (int i = 0; i < captions.length; i++) {
            JLabel userNameLabel = new JLabel(captions[i], JLabel.RIGHT);
            namePanel.add(userNameLabel);
        }
        JPanel fieldPanel = new JPanel(false);
        fieldPanel.setLayout(new GridLayout(0, 1));
        for (int i = 0; i < captions.length; i++) {
            JTextField userNameField;
            if (!passwords[i]) userNameField = new JTextField(""); else userNameField = new JPasswordField("");
            if (captionsEdit != null && captionsEdit.length > i) userNameField.setText(captionsEdit[i]);
            userNameField.setPreferredSize(new java.awt.Dimension(100, 20));
            fieldPanel.add(userNameField);
        }
        connectionPanel.add(namePanel);
        connectionPanel.add(fieldPanel);
        result = JOptionPane.showOptionDialog(null, connectionPanel, ConnectTitle, JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE, null, ConnectOptionNames, ConnectOptionNames[0]);
        if (result != 0) {
            return;
        } else for (int i = 0; i < captions.length; i++) {
            JTextField c = (JTextField) fieldPanel.getComponent(i);
            fields[i] = c.getText();
        }
    }
}
