package org.expasy.jpl.demo;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.expasy.jpl.bio.exceptions.JPLAAByteUndefinedException;
import org.expasy.jpl.bio.exceptions.JPLEmptySequenceException;
import org.expasy.jpl.bio.sequence.JPLAASequence;
import org.expasy.jpl.bio.sequence.JPLIAASequence;
import org.expasy.jpl.bio.sequence.io.fasta.JPLFastaEntry;
import org.expasy.jpl.bio.sequence.io.fasta.JPLFastaHeaderFormatManager;
import org.expasy.jpl.bio.sequence.io.fasta.JPLFastaReader;
import org.expasy.jpl.bio.sequence.tools.positions.cutter.JPLCleaver;
import org.expasy.jpl.bio.sequence.tools.positions.cutter.JPLTrypsinKRnotPCutter;
import org.expasy.jpl.commons.ms.filtering.filter.JPLPeakTypeConditions;
import org.expasy.jpl.commons.ms.peak.JPLIMSnPeakType;
import org.expasy.jpl.commons.ms.peak.JPLMSnPeakType;
import org.expasy.jpl.insilico.exceptions.JPLPrecursorUnfragmentableException;
import org.expasy.jpl.insilico.ms.fragmentation.JPLFragmentationType;
import org.expasy.jpl.insilico.ms.fragmentation.fragmenter.JPLPeptideFragmenter;
import org.expasy.jpl.insilico.ms.peak.JPLITheoSeqBasedMSPeak;
import org.expasy.jpl.insilico.ms.peak.JPLTheoSeqBasedMSPeak;
import org.expasy.jpl.insilico.ms.peaklist.JPLTheoMSnPeakList;
import org.expasy.jpl.utils.condition.JPLIConditionalExpression;
import org.expasy.jpl.utils.parser.JPLParseException;

/**
 * Params profiles: -f three_prots.fasta --fasta-file three_prots.fasta
 * --help/-v --version/-h
 * 
 * @author def
 * 
 */
public class YetAnotherFragmenter {

    static String version = "1.0";

    static JPLPeptideFragmenter fragmenter;

    static String filename = "";

    static Set<Integer> charges;

    static Set<JPLIMSnPeakType> peakTypes;

    static Set<JPLFragmentationType> fragmentationTypes;

    static int maxCharge = 1;

    static JPLIConditionalExpression fragmentCondition;

    /**
	 * @param args
	 * @throws IOException
	 * @throws IOException
	 * @throws JPLAAByteUndefinedException
	 * @throws JPLParseException
	 * @throws JPLEmptySequenceException
	 * @throws JPLEmptySequenceException
	 * @throws JPLParseException
	 */
    public static void main(String[] args) throws IOException, JPLEmptySequenceException, JPLAAByteUndefinedException {
        try {
            parseCommandLine(args);
        } catch (JPLParseException e) {
            System.err.println(e);
            System.exit(1);
        }
        System.out.println("Parameters: ");
        System.out.println("file: " + filename);
        System.out.println("charges: " + charges);
        System.out.println("fragmentation types: " + fragmentationTypes);
        System.out.println("fragment types: " + peakTypes);
        fragmenter = new JPLPeptideFragmenter(fragmentationTypes);
        try {
            displayFragments(filename, maxCharge);
        } catch (JPLParseException e) {
            System.err.println(e);
            System.exit(2);
        }
    }

    private static Options createShortOptions() {
        Options options = new Options();
        options.addOption("h", "help", false, "print this message");
        options.addOption("v", "version", false, "print the version info");
        options.addOption("f", "fasta-file", true, "define the fasta file name");
        options.addOption("q", "charge-numbers", true, "define the list of charge for fragments (ex: 1,2,3:5,10:7)");
        options.addOption("t", "frag-types", true, "define the fragment type(s)");
        return options;
    }

    @SuppressWarnings("static-access")
    private static Options createLongOptions() {
        Option help = new Option("help", "print this message");
        Option version = new Option("version", "print the version info");
        Option fastaFile = OptionBuilder.withArgName("fasta file").hasArg().withDescription("define the fasta file name").create("fasfile");
        Option peakTypes = OptionBuilder.withArgName("peak type(s)").hasArg().withDescription("define the fragment types to compute").create("fragtypes");
        Option chargeTypes = OptionBuilder.withArgName("charge number(s)").hasArg().withDescription("define the fragment charge numbers to compute").create("chargenumbers");
        Options options = new Options();
        options.addOption(help);
        options.addOption(version);
        options.addOption(fastaFile);
        options.addOption(peakTypes);
        options.addOption(chargeTypes);
        return options;
    }

    private static void parseCommandLine(String[] args) throws JPLParseException {
        Options options = null;
        CommandLineParser parser = null;
        if (true) {
            options = createShortOptions();
            parser = new PosixParser();
        } else {
            options = createLongOptions();
            parser = new GnuParser();
        }
        HelpFormatter usage = new HelpFormatter();
        try {
            CommandLine line = parser.parse(options, args);
            if (line.hasOption("h")) {
                usage.printHelp("yaf", options, true);
                System.exit(0);
            }
            if (line.hasOption("v")) {
                System.out.println("version = " + version);
                System.exit(0);
            }
            filename = line.getOptionValue("f");
            charges = parseCharges(line.getOptionValue("q"));
            peakTypes = parsePeakTypes(line.getOptionValue("t"));
            if (peakTypes.size() == 0) {
                peakTypes.add(JPLMSnPeakType.B_PEAK);
                peakTypes.add(JPLMSnPeakType.Y_PEAK);
            }
            if (charges.size() == 0) {
                charges.add(1);
            }
            if (filename == null) {
                System.err.println("missing file name");
                usage.printHelp("yaf", options, true);
                System.exit(1);
            }
        } catch (ParseException exp) {
            System.out.println("Unexpected exception:" + exp.getMessage());
        }
    }

    private static Set<Integer> parseCharges(String line) {
        Set<Integer> charges = new HashSet<Integer>();
        if (line != null) {
            Pattern pattern = Pattern.compile("\\d+(\\:\\d+)*");
            Matcher matcher = pattern.matcher(line);
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
                            System.err.println("bad syntax: " + value + "\n");
                        }
                    }
                } else {
                    charges.add(Integer.parseInt(value));
                }
            }
        }
        return charges;
    }

    private static Set<JPLIMSnPeakType> parsePeakTypes(String fragments) throws JPLParseException {
        Set<JPLIMSnPeakType> types = new HashSet<JPLIMSnPeakType>();
        fragmentationTypes = new HashSet<JPLFragmentationType>();
        System.out.println(".. parse " + fragments);
        for (String fragment : fragments.split(",")) {
            JPLIMSnPeakType type = JPLMSnPeakType.getPeakType(fragment);
            if (type != null) {
                types.add(type);
                if (type == JPLMSnPeakType.A_PEAK || type == JPLMSnPeakType.X_PEAK) {
                    fragmentationTypes.add(JPLFragmentationType.AX);
                } else if (type == JPLMSnPeakType.B_PEAK || type == JPLMSnPeakType.Y_PEAK) {
                    fragmentationTypes.add(JPLFragmentationType.BY);
                } else if (type == JPLMSnPeakType.C_PEAK || type == JPLMSnPeakType.Z_PEAK) {
                    fragmentationTypes.add(JPLFragmentationType.CZ);
                }
            } else if (fragment.equals("i")) {
                types.add(JPLMSnPeakType.I_PEAK);
                fragmentationTypes.add(JPLFragmentationType.IMMONIUM);
            } else if (fragment.equals("p")) {
                types.add(JPLMSnPeakType.p_PEAK);
                fragmentationTypes.add(JPLFragmentationType.PRECURSOR);
            } else {
                System.err.println("water/ammonium losses: not yet implemented");
            }
        }
        fragmentCondition = new JPLPeakTypeConditions.Builder("term=" + fragments).build();
        return types;
    }

    private static void displayFragments(String filename, int charge) throws JPLEmptySequenceException, JPLParseException, JPLAAByteUndefinedException, IOException {
        JPLFastaReader fastaScanner = new JPLFastaReader(filename);
        fastaScanner.setHeaderFormat(JPLFastaHeaderFormatManager.SWISSPROT_HEADER);
        while (fastaScanner.hasNext()) {
            JPLFastaEntry nextEntry = fastaScanner.next();
            Set<JPLIAASequence> set = makeTrypsinDigestion(nextEntry.getSequence());
            Iterator<JPLIAASequence> peptides = set.iterator();
            while (peptides.hasNext()) {
                Map<JPLITheoSeqBasedMSPeak, JPLTheoMSnPeakList> peptideFragments;
                JPLIAASequence peptide = peptides.next();
                peptideFragments = makeFragmentation(peptide, charge);
                if (!peptideFragments.isEmpty()) {
                    writeMGF(peptide.toAAString(), peptideFragments);
                }
            }
        }
    }

    private static Set<JPLIAASequence> makeTrypsinDigestion(String sequence) throws JPLEmptySequenceException, JPLParseException {
        JPLCleaver trypsin = new JPLCleaver(new JPLTrypsinKRnotPCutter());
        trypsin.digest(new JPLAASequence.Builder(sequence).build());
        return trypsin.getUniqueDigests();
    }

    private static Map<JPLITheoSeqBasedMSPeak, JPLTheoMSnPeakList> makeFragmentation(JPLIAASequence seq, int charge) throws JPLEmptySequenceException, JPLAAByteUndefinedException {
        Map<JPLITheoSeqBasedMSPeak, JPLTheoMSnPeakList> peptideFragments = new HashMap<JPLITheoSeqBasedMSPeak, JPLTheoMSnPeakList>();
        JPLITheoSeqBasedMSPeak precursor = new JPLTheoSeqBasedMSPeak(seq, charge);
        try {
            fragmenter.setFragmentablePrecursor(precursor.getSequence(), charge);
            fragmenter.generateIonFragments();
            JPLTheoMSnPeakList pl = fragmenter.getPeakList(fragmentCondition);
            peptideFragments.put(precursor, pl);
        } catch (JPLPrecursorUnfragmentableException e) {
            System.err.print("# " + e.getMessage() + "\n");
        }
        return peptideFragments;
    }

    public static void writeMGF(String title, Map<JPLITheoSeqBasedMSPeak, JPLTheoMSnPeakList> peptideFragments) {
        for (JPLITheoSeqBasedMSPeak precPeak : peptideFragments.keySet()) {
            String peptideName = precPeak.getSequence().toAAString() + "/" + precPeak.getChargeState();
            JPLTheoMSnPeakList fragments = peptideFragments.get(precPeak);
            System.out.print("\n\n");
            System.out.println("BEGIN ION");
            System.out.println("TITLE=" + peptideName);
            System.out.println("PEPMASS=" + precPeak.getMz());
            System.out.println("CHARGE=" + precPeak.getChargeState());
            int n = fragments.getNbPeak();
            for (int i = 0; i < n; i++) {
                System.out.println(fragments.getMzAt(i) + "\t" + fragments.getChargeAt(i));
            }
            System.out.println("END IONS");
        }
    }
}
