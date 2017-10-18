package org.expasy.jpl.demos;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.expasy.jpl.commons.base.io.DecimalFormatFactory;
import org.expasy.jpl.commons.collection.Interval;
import org.expasy.jpl.core.mol.chem.ChemicalFacade;
import org.expasy.jpl.core.mol.chem.MassCalculator;
import org.expasy.jpl.core.mol.modif.ModificationFactory;
import org.expasy.jpl.core.mol.polymer.BioPolymerUtils;
import org.expasy.jpl.core.mol.polymer.pept.Peptide;
import org.expasy.jpl.core.mol.polymer.pept.cutter.CleavageSiteCutter;
import org.expasy.jpl.core.mol.polymer.pept.cutter.DigestedPeptide;
import org.expasy.jpl.core.mol.polymer.pept.cutter.Digester;
import org.expasy.jpl.core.mol.polymer.pept.cutter.Peptidase;
import org.expasy.jpl.core.mol.polymer.pept.fragmenter.PeptideFragmentationException;
import org.expasy.jpl.core.mol.polymer.pept.matcher.AAMotifMatcher;
import org.expasy.jpl.core.mol.polymer.pept.rule.EditionRule;
import org.expasy.jpl.core.mol.polymer.pept.rule.PeptideEditorFactory;
import org.expasy.jpl.core.mol.polymer.pept.rule.EditionRule.EditionAction;
import org.expasy.jpl.core.mol.polymer.pept.term.CTerminus;
import org.expasy.jpl.core.mol.polymer.pept.term.NTerminus;
import org.expasy.jpl.io.mol.fasta.FastaEntry;
import org.expasy.jpl.io.mol.fasta.FastaHeaderFormatManager;
import org.expasy.jpl.io.mol.fasta.FastaReader;

/**
 * This application digests a database of proteins given for given enzymes.
 * 
 * @author nikitin
 * 
 */
public class Dig2MZ {

    /********************* the default values **********************/
    private static final EditionRule CYS_CAM;

    /** the default double precision */
    private static int DEFAULT_PRECISION = 6;

    /** the default number of missed cleavage */
    private static int DEFAULT_MISSED_CLEAVAGES = 1;

    /** the default number of max methionine oxidated */
    private static int DEFAULT_MAX_OXIDATED_MET = 0;

    /** the default pH */
    private static double DEFAULT_PH = 2.3;

    /** the default peptide charge filter */
    private static Set<Integer> DEFAULT_PEPT_CHARGE_FILTER = new HashSet<Integer>(Arrays.asList(2, 3));

    /** the default peptide length filter */
    private static Integer DEFAULT_PEPT_LEN_FILTER = 6;

    /** the default peptide mz min filter */
    private static Integer DEFAULT_PEPT_MZ_MIN_FILTER = 400;

    /** the default peptide mz max filter (included) */
    private static Integer DEFAULT_PEPT_MZ_MAX_FILTER = 2000;

    /** the default protease */
    private static String DEFAULT_ENZYME = "Trypsin";

    private static MassCalculator DEFAULT_MASS_TYPE = MassCalculator.getMonoAccuracyInstance();

    private static List<Integer> DEFAULT_OUTPUT_FIELDS_IDX = Arrays.asList(1, 2, 3, 4, 5);

    private static String DEFAULT_FIELD_DELIMITER = "\t";

    private static List<String> OUTPUT_FIELDS = Arrays.asList("MZ", "Charge", "Enzyme", "Seq", "MC", "Mods");

    /********************* the actors ******************************/
    private String version = "1.21";

    /** the peptidases */
    private Set<Digester> digesters;

    /********************* the parameters **************************/
    private Properties settings;

    /** the file name for fasta data */
    private String filename = "";

    /** the field delimiter for output */
    private String fieldDelimiter;

    /** the mass calculator */
    private MassCalculator massCalc;

    /** the print formatter for doubles */
    private NumberFormat formatter;

    /** the precision for doubles */
    private int precision;

    /** the fragment charges filter for output */
    private Set<Integer> peptChargesFilter;

    /** the fragment length filter for output */
    private int peptLenFilter;

    /** the interval of mz filter */
    private Interval peptMzFilter;

    /** the output fields */
    private List<Integer> fields;

    /** the file with all settings */
    private File inputFile;

    /** the number of maximum missed-cleavages */
    private int missedCleavages;

    /** the number of methionine oxidated */
    private int maxMetOxNum = 0;

    /** display settings */
    private boolean isDisplaySettings;

    private boolean isCysCamEnabled = false;

    static {
        try {
            CYS_CAM = EditionRule.newInstance("Cys_CAM", AAMotifMatcher.newInstance("C"), EditionAction.newFixedModifAction(ModificationFactory.valueOf(ChemicalFacade.getMolecule("H3C2NO"))));
        } catch (ParseException e) {
            throw new IllegalStateException("cannot create modif rules", e);
        }
    }

    public static void main(String[] args) throws IOException, org.apache.commons.cli.ParseException {
        Dig2MZ app = new Dig2MZ();
        app.setDefaultSettings();
        app.parseCommandLine(args);
        try {
            app.updateParameters();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        if (app.isDisplaySettings) {
            app.displayParams();
        }
        try {
            StringBuilder sb = new StringBuilder();
            for (int field : app.fields) {
                sb.append(OUTPUT_FIELDS.get(field - 1) + app.fieldDelimiter);
            }
            sb.delete(sb.length() - 1, sb.length());
            System.out.println(sb);
            app.digestProteins(app.filename);
        } catch (ParseException e) {
            System.err.println(e);
            System.exit(2);
        } catch (PeptideFragmentationException e) {
            System.err.println(e);
            System.exit(3);
        }
    }

    public String digest2String(DigestedPeptide digest, double mz, String protAccessionNumber) {
        StringBuilder sb = new StringBuilder();
        for (int field : fields) {
            switch(field) {
                case 1:
                    sb.append(formatter.format(mz) + fieldDelimiter);
                    break;
                case 2:
                    sb.append(digest.getCharge() + fieldDelimiter);
                    break;
                case 3:
                    sb.append(digest.getEnzyme().getId() + fieldDelimiter);
                    break;
                case 4:
                    sb.append(digest.getPeptide().toAAString() + fieldDelimiter);
                    break;
                case 5:
                    sb.append(digest.getMC() + fieldDelimiter);
                    break;
                case 7:
                    sb.append(protAccessionNumber + fieldDelimiter);
                    break;
                case 6:
                    sb.append((digest.hasModifs() ? Integer.toString(digest.getModifs().getNumOfLocModif()) : "0") + fieldDelimiter);
                    break;
            }
        }
        sb.delete(sb.length() - 1, sb.length());
        return sb.toString();
    }

    public void setDefaultSettings() {
        settings = new Properties();
        settings.setProperty("delimiter", DEFAULT_FIELD_DELIMITER);
        settings.setProperty("average", "false");
        settings.setProperty("non-verbose", "false");
        settings.setProperty("cyscam", "false");
        settings.setProperty("enzymes", DEFAULT_ENZYME);
        settings.setProperty("fields", "1,2,3,4,5");
        settings.setProperty("mc-max-num", "1");
        settings.setProperty("oximet-max-num", "0");
        settings.setProperty("precision", "6");
        settings.setProperty("pept-charge-filter", "2,3");
        settings.setProperty("pept-len-filter", "6");
        settings.setProperty("pept-mz-lower-filter", "400");
        settings.setProperty("pept-mz-upper-filter", "2000");
    }

    public void updateParameters() throws ParseException {
        double peptLowerMz = 0;
        double peptUpperMz = 0;
        for (Object o : settings.keySet()) {
            String paramName = (String) o;
            String paramValue = settings.getProperty(paramName);
            if (paramName.equals("average")) {
                if (paramValue.equals("true")) {
                    massCalc = MassCalculator.getAvgAccuracyInstance();
                } else {
                    massCalc = MassCalculator.getMonoAccuracyInstance();
                }
            } else if (paramName.equals("non-verbose")) {
                if (paramValue.equals("true")) {
                    isDisplaySettings = false;
                } else {
                    isDisplaySettings = true;
                }
            } else if (paramName.equals("enzymes")) {
                digesters = parseEnzymes(paramValue, missedCleavages);
            } else if (paramName.equals("fields")) {
                fields = parseFields(paramValue);
            } else if (paramName.equals("delimiter")) {
                fieldDelimiter = paramValue;
            } else if (paramName.equals("pept-charge-filter")) {
                peptChargesFilter = parseCharges(paramValue);
            } else if (paramName.equals("pept-len-filter")) {
                peptLenFilter = Integer.parseInt(paramValue);
            } else if (paramName.equals("pept-mz-lower-filter")) {
                peptLowerMz = Double.parseDouble(paramValue);
            } else if (paramName.equals("pept-mz-upper-filter")) {
                peptUpperMz = Double.parseDouble(paramValue);
            } else if (paramName.equals("mc-max-num")) {
                missedCleavages = Integer.parseInt(paramValue);
            } else if (paramName.equals("oximet-max-num")) {
                maxMetOxNum = Integer.parseInt(paramValue);
            } else if (paramName.equals("precision")) {
                precision = Integer.parseInt(paramValue);
                formatter = DecimalFormatFactory.valueOf(precision);
            } else if (paramName.equals("cyscam")) {
                if (paramValue.equals("true")) {
                    isCysCamEnabled = true;
                } else {
                    isCysCamEnabled = false;
                }
            }
        }
        peptMzFilter = new Interval.Builder(peptLowerMz, peptUpperMz).includeUpperBound().build();
        checkValues();
    }

    public void checkValues() {
        if (missedCleavages < 0) {
            System.err.println(missedCleavages + ": bad missed cleavage value (have to be positive)");
            System.exit(3);
        }
        if (maxMetOxNum < 0) {
            System.err.println(maxMetOxNum + ": bad max number of methionine sites value (have to be positive)");
            System.exit(4);
        } else if (maxMetOxNum > 5) {
            System.err.println("warning: " + maxMetOxNum + " cannot compute more than 5 missed cleaved per protein (set to 5)");
        }
        if (precision < 0) {
            System.err.println(precision + ": bad precision value (have to be positive)");
            System.exit(5);
        }
        if (peptLenFilter < 0) {
            System.err.println(peptLenFilter + ": bad peptide length value (have to be positive)");
            System.exit(6);
        }
    }

    public void displayParams() {
        System.out.println("# ==================================================================");
        System.out.println("# Generated by " + getClass().getSimpleName() + " v." + version);
        System.out.println("#");
        if (inputFile != null) {
            System.out.println("# Settings extracted from file         " + inputFile.getName());
            System.out.println("#");
        }
        System.out.println("# Input Proteins ---------------------------------------------------");
        System.out.println("#\tread from file                 " + filename);
        if (isCysCamEnabled) {
            System.out.println("#\tfixed modification             CYS_CAM");
        }
        if (maxMetOxNum > 0) {
            System.out.println("#\tmodified (var) by                MET_OXIDATED (" + maxMetOxNum + " sites max)");
        }
        System.out.println("#\tdigested by enzyme             " + digesters);
        System.out.println("# Digested Peptides ------------------------------------------------");
        System.out.println("#\tfiltered with charges          " + peptChargesFilter);
        System.out.println("#\tfiltered with length           >= " + peptLenFilter);
        System.out.println("#\tfiltered over mzs in interval  " + peptMzFilter);
        System.out.println("#\twith masses computed in mode   " + massCalc.getAccuracy());
        System.out.println("# Output Fields ----------------------------------------------------");
        System.out.println("#\tall                            " + OUTPUT_FIELDS);
        System.out.println("#\tselected indices               " + fields);
        System.out.println("# Real");
        System.out.println("#\tDecimal precision format       " + precision);
        System.out.println("# ==================================================================");
    }

    @SuppressWarnings("static-access")
    public static Options createShortOptions() {
        Options options = new Options();
        StringBuilder sb = new StringBuilder();
        for (Peptidase peptidase : Peptidase.getAllPooledPeptidases()) {
            sb.append(peptidase.getId());
            sb.append(", ");
        }
        sb.delete(sb.length() - 2, sb.length());
        options.addOption("h", "help", false, "print this message.");
        options.addOption("v", "version", false, "print the version info.");
        options.addOption(OptionBuilder.withLongOpt("non-verbose").withDescription("non verbose mode (no header for settings).").create());
        options.addOption(OptionBuilder.withLongOpt("cyscam").withDescription("fixed modifications of all protein's cysteins by S-carboxamidomethyl " + "cysteines (CysCAM, +57 Da).").create());
        options.addOption("i", "setting-file", true, "give a property file with all input settings.");
        options.addOption("q", "pept-charge-filter", true, "define a filter over charges on digested peptides " + "as a sequence of integers and/or intervals like in 1, 2, 3:5, 10:7\n" + "by default: " + DEFAULT_PEPT_CHARGE_FILTER + ".");
        options.addOption("L", "pept-len-filter", true, "define a filter over length of digested peptides\nby default: " + DEFAULT_PEPT_LEN_FILTER + ".");
        options.addOption("l", "pept-mz-lower-filter", true, "define the lower mz bound (included) of digested peptides\nby default: " + DEFAULT_PEPT_MZ_MIN_FILTER + ".");
        options.addOption("u", "pept-mz-upper-filter", true, "define the upper mz bound (included) of digested peptides\nby default: " + DEFAULT_PEPT_MZ_MAX_FILTER + ".");
        options.addOption("e", "enzymes", true, "define enzymes that digest proteins separately with:\n" + "enzyme name among '" + sb + "'.\nor custom motifs respecting the following grammar:\n" + "<pre-cut> <cut-token> <post-cut>\n" + "<cut-token> := '|'\n" + "<pre-cut> := (<AA> or <AA-class>)+\n" + "<post-cut> := (<AA> or <AA-class>)+\n" + "<AA> := [A-Z]\n" + "<AA-class> := '[' AA+ ']'\n" + "by default: " + DEFAULT_ENZYME + ".");
        options.addOption("p", "precision", true, "define the decimal precision for any mass-to-charge ratio\n" + "by default: " + DEFAULT_PRECISION + ".");
        options.addOption("a", "average", false, "set the average mass mode for peptide mass calculation\n" + "by default: " + DEFAULT_MASS_TYPE.getAccuracy() + ".");
        sb = new StringBuilder();
        for (int i = 0; i < OUTPUT_FIELDS.size(); i++) {
            sb.append(i + 1);
            sb.append(":");
            sb.append(OUTPUT_FIELDS.get(i));
            sb.append(", ");
        }
        sb.delete(sb.length() - 2, sb.length());
        options.addOption("d", "delimiter", true, "define the field delimiter to display" + "\nby default: \\t.");
        options.addOption("f", "fields", true, "define the fields to display (" + sb + ")\nby default: " + DEFAULT_OUTPUT_FIELDS_IDX + ".");
        options.addOption("m", "mc-max-num", true, "define the number of maximum missed cleavages (for digestion)\n" + "by default: " + DEFAULT_MISSED_CLEAVAGES + ".");
        options.addOption("o", "oximet-max-num", true, "define the number of maximum oxidated methionines\n" + "by default: " + DEFAULT_MAX_OXIDATED_MET + ".");
        return options;
    }

    /**
	 * Parse command line and update the settings.
	 * 
	 * @param args the arguments.
	 * @throws org.apache.commons.cli.ParseException
	 * 
	 * @throws ParseException if parsing of parameters failed.
	 */
    public void parseCommandLine(String[] args) throws org.apache.commons.cli.ParseException {
        Options options = null;
        CommandLineParser parser = null;
        options = createShortOptions();
        parser = new PosixParser();
        HelpFormatter usage = new HelpFormatter();
        try {
            CommandLine line = parser.parse(options, args);
            if (line.hasOption("h")) {
                usage.printHelp(getClass().getSimpleName() + " <fasta>", options, true);
                System.exit(0);
            }
            if (line.hasOption("v")) {
                System.out.println(getClass().getSimpleName() + " v." + version);
                System.exit(0);
            }
            if (line.hasOption("i")) {
                String filename = line.getOptionValue("i");
                inputFile = new File(filename);
                if (inputFile.exists()) {
                    try {
                        settings.load(new FileInputStream(inputFile));
                    } catch (IOException e) {
                        System.err.println(e.getMessage() + ": cannot load properties from " + filename);
                        usage.printHelp(getClass().getSimpleName() + " <fasta>", options, true);
                        System.exit(4);
                    }
                } else {
                    System.err.println(filename + ": no such settings file name");
                    usage.printHelp(getClass().getSimpleName() + " <fasta>", options, true);
                    System.exit(4);
                }
            }
            if (line.hasOption("non-verbose")) {
                settings.setProperty("non-verbose", "true");
            }
            if (line.hasOption("cyscam")) {
                settings.setProperty("cyscam", "true");
            }
            if (line.hasOption("a")) {
                settings.setProperty("average", "true");
            }
            if (line.hasOption("f")) {
                settings.setProperty("fields", line.getOptionValue("f"));
            }
            if (line.hasOption("d")) {
                settings.setProperty("delimiter", line.getOptionValue("d"));
            }
            if (line.hasOption("q")) {
                settings.setProperty("pept-charge-filter", line.getOptionValue("q"));
            }
            if (line.hasOption("L")) {
                settings.setProperty("pept-len-filter", line.getOptionValue("L"));
            }
            if (line.hasOption("l")) {
                settings.setProperty("pept-mz-lower-filter", line.getOptionValue("l"));
            }
            if (line.hasOption("u")) {
                settings.setProperty("pept-mz-upper-filter", line.getOptionValue("u"));
            }
            if (line.hasOption("o")) {
                settings.setProperty("oximet-max-num", line.getOptionValue("o"));
            }
            if (line.hasOption("m")) {
                settings.setProperty("mc-max-num", line.getOptionValue("m"));
            }
            if (line.hasOption("e")) {
                settings.put("enzymes", line.getOptionValue("e"));
            }
            if (line.hasOption("p")) {
                settings.put("precision", line.getOptionValue("p"));
            }
            if (line.getArgList().size() == 0) {
                System.err.println("missing file name");
                usage.printHelp(getClass().getSimpleName() + " <fasta>", options, true);
                System.exit(1);
            } else if (line.getArgList().size() > 1) {
                System.err.println("too many arguments");
                usage.printHelp(getClass().getSimpleName() + " <fasta>", options, true);
                System.exit(2);
            } else {
                filename = line.getArgs()[0];
            }
        } catch (org.apache.commons.cli.ParseException exp) {
            System.out.println("Unexpected exception:" + exp.getMessage());
            usage.printHelp(getClass().getSimpleName() + " <fasta>", options, true);
            System.exit(3);
        }
    }

    /**
	 * Parse output fields from a given string.
	 * 
	 * @param param the string to get field index from.
	 * @return a list of fields.
	 * 
	 * @throws ParseException if not well formatted.
	 */
    private static List<Integer> parseFields(String param) throws ParseException {
        List<Integer> fields = new ArrayList<Integer>();
        String[] fs = param.split(",\\s*");
        for (String f : fs) {
            int field = Integer.parseInt(f);
            if (field < 1 || field > OUTPUT_FIELDS.size()) {
                throw new ParseException(field + ": bad field index", -1);
            }
            fields.add(field);
        }
        return fields;
    }

    /**
	 * Parse enzyme(s) from a given string.
	 * 
	 * @param param the string to get enzymes from.
	 * @return a list of enzymes.
	 * 
	 * @throws ParseException if not well formatted.
	 */
    public static Set<Digester> parseEnzymes(String line, int missedCleavageNum) throws ParseException {
        Set<Digester> s = new HashSet<Digester>();
        CleavageSiteCutter cutter = null;
        String[] enzymes = line.split(",\\s*");
        Digester digester;
        Peptidase peptidase;
        for (String enz : enzymes) {
            if (enz.indexOf('|') != -1) {
                cutter = new CleavageSiteCutter.Builder(enz).build();
                peptidase = Peptidase.valueOf(cutter);
            } else {
                peptidase = Peptidase.getInstance(enz);
            }
            if (peptidase == null) {
                throw new ParseException("enzyme " + enz + " is unknown", -1);
            }
            digester = Digester.newInstance(peptidase);
            if (missedCleavageNum > 0) {
                digester.setNumberOfMissedCleavage(missedCleavageNum);
            }
            s.add(digester);
        }
        return s;
    }

    /**
	 * Parse charges from a given string.
	 * 
	 * @param param the string to get charges from.
	 * @return a set of charges.
	 * 
	 * @throws ParseException if not well formatted.
	 */
    public static Set<Integer> parseCharges(String param) throws ParseException {
        Set<Integer> charges = new HashSet<Integer>();
        if (param != null) {
            Pattern pattern = Pattern.compile("\\d+(\\:\\d+)*");
            Matcher matcher = pattern.matcher(param);
            while (matcher.find()) {
                String value = matcher.group();
                if (value.indexOf(":") != -1) {
                    int sepCount = value.replaceAll("[^:]", "").length();
                    if (sepCount > 0) {
                        if (sepCount == 1) {
                            String[] se = value.split(":");
                            int from = Integer.parseInt(se[0]);
                            int to = Integer.parseInt(se[1]);
                            int tmp = 0;
                            if (to < from) {
                                tmp = from;
                                from = to;
                                to = tmp;
                            }
                            for (int i = from; i <= to; i++) {
                                charges.add(i);
                            }
                        } else {
                            throw new ParseException("bad syntax: " + value + "\n", -1);
                        }
                    }
                } else {
                    charges.add(Integer.parseInt(value));
                }
            }
        }
        return charges;
    }

    public Set<Peptide> makeModifications(Peptide peptide) {
        Set<Peptide> peptides = new HashSet<Peptide>();
        PeptideEditorFactory cysCamFactory = PeptideEditorFactory.newInstance(CYS_CAM);
        Peptide startingPeptide = null;
        if (isCysCamEnabled) {
            startingPeptide = cysCamFactory.transform(peptide).iterator().next();
        } else {
            startingPeptide = peptide;
        }
        peptides.add(startingPeptide);
        if (maxMetOxNum > 0) {
            try {
                EditionRule metOxidation = EditionRule.newInstance("Oxidation_M", AAMotifMatcher.newInstance("[M]"), EditionAction.newVariableModifAction(ModificationFactory.valueOf(ChemicalFacade.getMolecule("O2"))));
                PeptideEditorFactory methOxfactory = PeptideEditorFactory.newInstance(metOxidation);
                methOxfactory.setMaxSiteNumber(maxMetOxNum);
                peptides.addAll(methOxfactory.transform(startingPeptide));
            } catch (ParseException e) {
                throw new IllegalStateException("cannot create modif rule", e);
            }
        }
        return peptides;
    }

    /**
	 * Digest all fasta entries.
	 * 
	 * @param filename the fasta filename.
	 * 
	 * @throws ParseException if parsing failed.
	 * @throws IOException if file does not exist.
	 */
    public Iterator<DigestedPeptide> digestProteins(String filename) throws ParseException, IOException, PeptideFragmentationException {
        FastaReader fastaScanner = FastaReader.newInstance();
        FastaHeaderFormatManager manager = FastaHeaderFormatManager.newInstance();
        manager.addHeaderFormat(FastaHeaderFormatManager.getDefaultFormat());
        fastaScanner.setHeaderManager(manager);
        fastaScanner.parse(new File(filename));
        Iterator<FastaEntry> it = fastaScanner.iterator();
        List<DigestedPeptide> digests = new ArrayList<DigestedPeptide>();
        while (it.hasNext()) {
            FastaEntry nextEntry = it.next();
            Peptide protein = new Peptide.Builder(nextEntry.getSequence()).nterm(NTerminus.PROT_N).cterm(CTerminus.PROT_C).ambiguityEnabled().build();
            Set<Peptide> derivativeProteins = makeModifications(protein);
            for (Peptide derivativeProtein : derivativeProteins) {
                for (Digester digester : digesters) {
                    Set<DigestedPeptide> peptides = makeDigestion(derivativeProtein, digester);
                    for (DigestedPeptide digest : peptides) {
                        if (digest.isAmbiguous()) {
                            System.err.println("cannot compute mass of peptide " + digest);
                            continue;
                        }
                        if (digest.length() < peptLenFilter) {
                            continue;
                        }
                        double q = BioPolymerUtils.getNetCharge(digest.getPeptide(), DEFAULT_PH);
                        Set<Integer> peptCharges = new HashSet<Integer>();
                        peptCharges.add((int) Math.floor(q));
                        peptCharges.add((int) Math.ceil(q));
                        for (int charge : peptCharges) {
                            if (peptChargesFilter.contains(charge)) {
                                digest = new DigestedPeptide.Builder(digest.getPeptide()).mc(digest.getMC()).charge(charge).enzyme(digester.getPeptidase()).build();
                                double mz = massCalc.getMz(digest.getPeptide());
                                if (peptMzFilter.contains(mz)) {
                                    System.out.println(digest2String(digest, mz, nextEntry.getHeader().getProtID()));
                                }
                            }
                        }
                    }
                }
            }
        }
        return digests.iterator();
    }

    public Set<DigestedPeptide> makeDigestion(Peptide peptide, Digester digester) throws ParseException {
        digester.digest(peptide);
        return digester.getDigests();
    }
}
