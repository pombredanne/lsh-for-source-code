package org.expasy.jpl.insilico.ms.fragmentation.fragmenter;

import java.lang.ClassLoader;
import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import junit.framework.Assert;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.expasy.jpl.bio.exceptions.JPLAAByteUndefinedException;
import org.expasy.jpl.bio.exceptions.JPLAASequenceBuilderException;
import org.expasy.jpl.bio.exceptions.JPLEmptySequenceException;
import org.expasy.jpl.bio.sequence.JPLAASequence;
import org.expasy.jpl.bio.sequence.JPLIAASequence;
import org.expasy.jpl.bio.sequence.JPLITerminus;
import org.expasy.jpl.bio.sequence.io.fasta.JPLFastaReader;
import org.expasy.jpl.bio.sequence.tools.positions.cutter.JPLCleaver;
import org.expasy.jpl.bio.sequence.tools.positions.cutter.JPLTrypsinKRnotPCutter;
import org.expasy.jpl.commons.ms.peak.JPLIMSnPeakType;
import org.expasy.jpl.insilico.exceptions.JPLPrecursorUnfragmentableException;
import org.expasy.jpl.insilico.ms.fragmentation.JPLFragmentationType;
import org.expasy.jpl.insilico.ms.peak.JPLITheoSeqBasedMSPeak;
import org.expasy.jpl.insilico.ms.peak.JPLTheoSeqBasedMSPeak;
import org.expasy.jpl.insilico.ms.peaklist.JPLTheoMSnPeakList;
import org.junit.Before;
import org.junit.Test;

/**
 * Testing digestion + fragmentation of all proteins from uniprot.
 *
 */
public class JPLFastaProteinFragmenterTest {

    public static final int BACKBONE_FRAGMODE = 1;

    public static final int IMMONIUM_FRAGMODE = 2;

    public static final int ALL_FRAGMODE = BACKBONE_FRAGMODE | IMMONIUM_FRAGMODE;

    private static HashMap<String, Integer> precursorOccNumber2fragment;

    private static int numOfRedundantPeptide2Fragment;

    private static int numOfPeptides;

    private static Log logger = LogFactory.getLog(JPLFastaProteinFragmenterTest.class);

    static String logConfFilename = "log4j.properties";

    static long startTime;

    static int fragMode;

    boolean enableFragmentationRedundancyStat = false;

    String fastaFilename;

    static int fragmentNumberGenerated = 0;

    static double meanPeptideSize = 0;

    JPLCleaver trypsin;

    JPLPeptideFragmenter fragmenter;

    JPLImmoniumGenerator immoniumFactory;

    Set<JPLIMSnPeakType> fragmentTypes;

    int fragmentTypeNumber;

    static {
        precursorOccNumber2fragment = new HashMap<String, Integer>();
    }

    @Before
    public void setUp() throws Exception {
        setFastaFileName("proteins.fasta");
        setFragmentationMode(ALL_FRAGMODE);
        trypsin = new JPLCleaver(new JPLTrypsinKRnotPCutter());
        fragmenter = new JPLPeptideFragmenter(EnumSet.of(JPLFragmentationType.AX, JPLFragmentationType.BY));
        immoniumFactory = new JPLImmoniumGenerator();
        Set<JPLITerminus> fragmentTypes = fragmenter.getFragmentTypes();
        fragmentTypeNumber = fragmentTypes.size();
        setEnableFragmentationRedundancyStat(true);
    }

    public static void setFragmentationMode(int fragMode) {
        JPLFastaProteinFragmenterTest.fragMode = fragMode;
    }

    @Test
    public void mainTest() {
        startTime = System.nanoTime();
        logger.info("Entering uniprot fragmentation benchmarking ...");
        try {
            scanNProcessFastaFile(ClassLoader.getSystemResource(fastaFilename).getFile());
        } catch (IOException e) {
            logger.fatal(e.getMessage() + ": " + fastaFilename + " read error.");
        } catch (JPLEmptySequenceException e) {
            logger.fatal(e.getMessage() + ": " + " empty sequence.");
        }
        logger.info("Benchmarking over.");
        logger.info("Test exec : " + (System.nanoTime() - startTime) + " ns.");
    }

    /**
	 * Read fasta file
	 * 
	 * @param filename (optionally gzipped) fasta filename to get sequence from. 
	 * @throws IOException 
	 * @throws JPLEmptySequenceException 
	 * @throws JPLAAByteUndefinedException 
	 * @throws Exception if ...
	 */
    public void scanNProcessFastaFile(String filename) throws IOException, JPLEmptySequenceException {
        JPLFastaReader fastaScanner = new JPLFastaReader(filename);
        int i = 0;
        int j = 0;
        while (fastaScanner.hasNext()) {
            String nextSequence = fastaScanner.nextFastaSequence();
            try {
                JPLIAASequence sequence = new JPLAASequence.Builder(nextSequence).build();
                if (logger.isDebugEnabled()) {
                    logger.debug("Trypsin digestion on " + sequence + "(" + sequence.length() + " aas)");
                }
                if (!sequence.isAmbiguous()) {
                    trypsin.digest(sequence);
                    List<JPLIAASequence> peptides = trypsin.getDigests();
                    if (logger.isInfoEnabled()) {
                        j += peptides.size();
                    }
                    fragmentAllPeptides(peptides);
                } else {
                    logger.warn(sequence + " has ambiguities");
                }
            } catch (JPLAASequenceBuilderException e) {
                if (logger.isDebugEnabled()) {
                    logger.warn(e.getMessage() + " ( " + nextSequence + ")");
                }
            }
            if (logger.isInfoEnabled()) {
                if (((i + 1) % 10000) == 0) {
                    logger.info((i + 1) + " fasta sequences red (" + (System.nanoTime() - startTime) + " ns.)");
                }
                if (((j + 1) % 10000) == 0) {
                    logger.info((j + 1) + " digests peptides sequences.");
                }
                if (((fragmentNumberGenerated + 1) % 10000) == 0) {
                    logger.info((i + 1) + " fasta sequences red.");
                    logger.info((fragmentNumberGenerated + 1) + " fragmented ions sequences.");
                }
                i++;
            }
        }
        if (logger.isInfoEnabled()) {
            logger.info("Total of " + i + " fasta protein sequences red.");
            logger.info("Total of " + j + " digested peptides (avg size:" + (meanPeptideSize / j) + " aas)");
            logger.info("Total of " + fragmentNumberGenerated + " generated fragments.");
            if (enableFragmentationRedundancyStat) {
                logger.info(numOfRedundantPeptide2Fragment + "/" + numOfPeptides + " redondant fragmentation operations (" + (numOfRedundantPeptide2Fragment * 100. / numOfPeptides) + "% )");
            }
        }
        Assert.assertEquals(6, i);
        Assert.assertEquals(284, j);
        Assert.assertEquals(2372, (int) meanPeptideSize);
        Assert.assertEquals(7416, fragmentNumberGenerated);
        Assert.assertEquals(11, numOfRedundantPeptide2Fragment);
        Assert.assertEquals(141, numOfPeptides);
    }

    public void fragmentAllPeptides(List<JPLIAASequence> peptides) throws JPLEmptySequenceException {
        int charge = 1;
        for (JPLIAASequence peptideSeq : peptides) {
            int currentGeneratedFragmentNumber = 0;
            if (logger.isInfoEnabled()) {
                meanPeptideSize += peptideSeq.length();
                if (logger.isDebugEnabled()) {
                    logger.debug("+" + charge + " charged " + fragmenter.getFragmentTypes() + " fragments to generate from :\n" + peptideSeq + " (" + peptideSeq.getStartIndexInRoot() + ":" + peptideSeq.getEndIndexInRoot() + ")");
                }
            }
            try {
                if (peptideSeq.length() >= JPLITheoSeqBasedMSPeak.MIN_FRAGMENTABLE_LENGTH) {
                    String seqString = peptideSeq.toAAString();
                    if (enableFragmentationRedundancyStat) {
                        if (precursorOccNumber2fragment.containsKey(seqString)) {
                            numOfRedundantPeptide2Fragment++;
                        } else {
                            precursorOccNumber2fragment.put(seqString, 1);
                        }
                        numOfPeptides++;
                    }
                    JPLTheoSeqBasedMSPeak peptidePrecursor = new JPLTheoSeqBasedMSPeak(peptideSeq, charge);
                    JPLTheoMSnPeakList peakList = fragmentPrecursor(peptidePrecursor);
                    if (logger.isInfoEnabled()) {
                        currentGeneratedFragmentNumber = peakList.getNbPeak();
                        fragmentNumberGenerated += currentGeneratedFragmentNumber;
                        if (logger.isDebugEnabled()) {
                            logger.debug(currentGeneratedFragmentNumber + " peaks generated : \n" + peakList);
                        }
                    }
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("not fragmentable");
                    }
                }
            } catch (JPLAAByteUndefinedException e) {
                if (logger.isDebugEnabled()) {
                    logger.warn(e.getMessage() + " : Fragmentation impossible" + " of current peptide due to undefined byte AA " + "in the root sequence : \n (" + peptideSeq + ")");
                } else {
                    break;
                }
            }
        }
    }

    public JPLTheoMSnPeakList fragmentPrecursor(JPLTheoSeqBasedMSPeak precursor) throws JPLEmptySequenceException, JPLAAByteUndefinedException {
        if (logger.isDebugEnabled()) {
            logger.debug("fragmentation of " + precursor);
        }
        try {
            fragmenter.setFragmentablePrecursor(precursor);
            fragmenter.generateIonFragments();
            return fragmenter.getPeakList();
        } catch (JPLPrecursorUnfragmentableException e) {
            if (logger.isDebugEnabled()) {
                logger.warn(e.getMessage());
            }
        }
        return JPLTheoMSnPeakList.emptyInstance();
    }

    public void setFastaFileName(String filename) {
        fastaFilename = filename;
    }

    public void setEnableFragmentationRedundancyStat(boolean bool) {
        enableFragmentationRedundancyStat = bool;
    }
}
