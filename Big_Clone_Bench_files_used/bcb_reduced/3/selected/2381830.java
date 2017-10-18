package org.expasy.jpl.demo;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.expasy.jpl.bio.exceptions.JPLAAByteUndefinedException;
import org.expasy.jpl.bio.exceptions.JPLAASequenceBuilderException;
import org.expasy.jpl.bio.sequence.JPLAASequence;
import org.expasy.jpl.bio.sequence.JPLIAASequence;
import org.expasy.jpl.bio.sequence.io.fasta.JPLFastaReader;
import org.expasy.jpl.bio.sequence.tools.positions.cutter.JPLCleaver;
import org.expasy.jpl.bio.sequence.tools.positions.cutter.JPLRegExpCutter;
import org.expasy.jpl.bio.sequence.tools.positions.cutter.JPLTrypsinKRnotPCutter;

/**
 * Profile: X peptide length, Y total nb of AA
 * 
 * @author celine
 * 
 */
public class LengthProfilerExample {

    static Log logger = LogFactory.getLog(LengthProfilerExample.class);

    private String fastaFile;

    private int highMassThreshold;

    private int lowMassThreshold;

    public HashMap<Integer, Integer> allMassProfile = new HashMap<Integer, Integer>();

    public int allPeptidesNb = 0;

    public HashMap<Integer, Integer> highMassProfile = new HashMap<Integer, Integer>();

    public int highPeptidesNb = 0;

    public HashMap<Integer, Integer> mediumMassProfile = new HashMap<Integer, Integer>();

    public int mediumPeptidesNb = 0;

    public HashMap<Integer, Integer> smallMassProfile = new HashMap<Integer, Integer>();

    public int smallPeptidesNb = 0;

    private JPLCleaver enzyme;

    private JPLCleaver trypsin;

    /**
	 * @param fastaFile
	 * @param massThreshold
	 */
    public LengthProfilerExample(String fastaFile, int lowMassThreshold, int massThreshold, int nbMissedCleavage) {
        this.fastaFile = fastaFile;
        this.lowMassThreshold = lowMassThreshold;
        this.highMassThreshold = massThreshold;
        this.enzyme = new JPLCleaver(new JPLTrypsinKRnotPCutter());
        this.enzyme.setNumberOfMissedCleavage(nbMissedCleavage);
    }

    /**
	 * @param fastaFile
	 * @param massThreshold
	 */
    public LengthProfilerExample(String fastaFile, int lowMassThreshold, int massThreshold, int nbMissedCleavage, JPLRegExpCutter enzyme, boolean withTrypsin) {
        this.fastaFile = fastaFile;
        this.lowMassThreshold = lowMassThreshold;
        this.highMassThreshold = massThreshold;
        if (withTrypsin) {
            this.trypsin = new JPLCleaver(new JPLTrypsinKRnotPCutter());
            this.trypsin.setNumberOfMissedCleavage(nbMissedCleavage);
        }
        this.enzyme = new JPLCleaver(enzyme);
        this.enzyme.setNumberOfMissedCleavage(nbMissedCleavage);
    }

    public LengthProfilerExample(String fastaFile, int lowMassThreshold, int massThreshold, int nbMissedCleavage, JPLRegExpCutter enzyme) {
        this(fastaFile, lowMassThreshold, massThreshold, nbMissedCleavage, enzyme, false);
    }

    public void profile() {
        try {
            JPLFastaReader fastaScanner = new JPLFastaReader(fastaFile);
            String nextSequence = "";
            while (fastaScanner.hasNext()) {
                try {
                    nextSequence = fastaScanner.nextFastaSequence();
                    JPLIAASequence sequence = new JPLAASequence.Builder(nextSequence).build();
                    List<JPLIAASequence> peptides = null;
                    if (trypsin != null) {
                        trypsin.digest(sequence);
                        List<JPLIAASequence> trypticPeptides = trypsin.getDigests();
                        for (JPLIAASequence trypPepSeq : trypticPeptides) {
                            enzyme.digest(trypPepSeq);
                            peptides = enzyme.getDigests();
                            for (JPLIAASequence pepSeq : peptides) {
                                treatSeq(pepSeq);
                            }
                        }
                    } else {
                        enzyme.digest(sequence);
                        peptides = enzyme.getDigests();
                        for (JPLIAASequence pepSeq : peptides) {
                            treatSeq(pepSeq);
                        }
                    }
                } catch (JPLAASequenceBuilderException e) {
                    logger.error(e.getMessage() + " ( " + nextSequence + ")");
                } catch (JPLAAByteUndefinedException e) {
                    logger.error("In " + nextSequence + ":" + e.getMessage() + ": " + " byte mass undefined.");
                }
            }
        } catch (IOException e1) {
            logger.error(e1.getMessage() + ": " + fastaFile + " read error.");
        }
    }

    private void treatSeq(JPLIAASequence pepSeq) throws JPLAAByteUndefinedException {
        double neutralMass = pepSeq.getNeutralMass();
        int length = pepSeq.length();
        int value = 0;
        allPeptidesNb++;
        if (allMassProfile.containsKey(new Integer(length))) value = allMassProfile.get(length).intValue();
        allMassProfile.put(length, new Integer(value + length));
        if (neutralMass > highMassThreshold) {
            highPeptidesNb++;
            value = 0;
            if (highMassProfile.containsKey(new Integer(length))) {
                value = highMassProfile.get(length).intValue();
            }
            highMassProfile.put(length, new Integer(value + length));
        } else if (neutralMass > lowMassThreshold && neutralMass < highMassThreshold) {
            mediumPeptidesNb++;
            value = 0;
            if (mediumMassProfile.containsKey(new Integer(length))) {
                value = mediumMassProfile.get(length).intValue();
            }
            mediumMassProfile.put(length, new Integer(value + length));
        } else {
            smallPeptidesNb++;
            value = 0;
            if (smallMassProfile.containsKey(new Integer(length))) {
                value = smallMassProfile.get(length).intValue();
            }
            smallMassProfile.put(length, new Integer(value + length));
        }
    }

    public void display() {
        logger.info("\nAll peptides\n");
        displayStats(allMassProfile.entrySet());
        logger.info("\nHigher peptides\n");
        displayStats(highMassProfile.entrySet());
        logger.info("\nMedium peptides\n");
        displayStats(mediumMassProfile.entrySet());
        logger.info("\nSmall peptides\n");
        displayStats(smallMassProfile.entrySet());
        logger.info("\nAll nb of peptides\n" + allPeptidesNb);
        logger.info("\nHigh nb of peptides\n" + highPeptidesNb);
        logger.info("\nMedium nb of peptides\n" + mediumPeptidesNb);
        logger.info("\nSmall nb of peptides\n" + smallPeptidesNb);
    }

    private void displayStats(Set<Entry<Integer, Integer>> profile) {
        logger.info("Length\tTotal nb of AA");
        Iterator<Entry<Integer, Integer>> it = profile.iterator();
        Entry<Integer, Integer> oneEntry;
        for (int i = 0; i < profile.size(); i++) {
            oneEntry = it.next();
            logger.info((oneEntry.getKey()) + "\t" + oneEntry.getValue());
        }
    }

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        String filename = "uniprot-organism-4932.fasta";
        test4(filename);
    }

    public static void test1(String filename) {
        LengthProfilerExample lpSPTR = new LengthProfilerExample(ClassLoader.getSystemResource(filename).getFile(), 800, 3000, 0);
        lpSPTR.profile();
        lpSPTR.display();
    }

    public static void test2(String filename) {
        String strPattern = "(?<=[DE])";
        JPLRegExpCutter enzyme = new JPLRegExpCutter(strPattern);
        LengthProfilerExample lpSPTRGluC = new LengthProfilerExample(ClassLoader.getSystemResource(filename).getFile(), 800, 3000, 0, enzyme);
        lpSPTRGluC.profile();
        lpSPTRGluC.display();
    }

    public static void test3(String filename, JPLRegExpCutter enzyme) {
        LengthProfilerExample lpSPTRGluC = new LengthProfilerExample(ClassLoader.getSystemResource(filename).getFile(), 800, 3000, 1, enzyme, true);
        lpSPTRGluC.profile();
        lpSPTRGluC.display();
    }

    public static void test4(String filename) {
        JPLRegExpCutter aspN = new JPLRegExpCutter("(?=[D])");
        LengthProfilerExample lpSPTRAspN = new LengthProfilerExample(ClassLoader.getSystemResource(filename).getFile(), 800, 3000, 1, aspN);
        lpSPTRAspN.profile();
        lpSPTRAspN.display();
    }
}
