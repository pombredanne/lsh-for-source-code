package org.expasy.jpl.perf;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
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
import org.expasy.jpl.insilico.ms.fragmentation.fragmenter.JPLPeptideFragmenter;
import org.expasy.jpl.insilico.ms.peak.JPLITheoSeqBasedMSPeak;
import org.expasy.jpl.insilico.ms.peak.JPLTheoSeqBasedMSPeak;
import org.expasy.jpl.insilico.ms.peaklist.JPLTheoMSnPeakList;

/**
 * Testing digestion + fragmentation of all proteins from uniprot.
 * 
 * <pre>
 * [INFO] DeprecatedProteinsDigesterNFragmenter - 100 fasta sequences red (26194206000 ns.)
 * [INFO] DeprecatedProteinsDigesterNFragmenter - 200 fasta sequences red (48089699000 ns.)
 * [INFO] DeprecatedProteinsDigesterNFragmenter - 300 fasta sequences red (69896150000 ns.)
 * [INFO] DeprecatedProteinsDigesterNFragmenter - 400 fasta sequences red (86323735000 ns.)
 * [INFO] DeprecatedProteinsDigesterNFragmenter - 500 fasta sequences red (111681967000 ns.)
 * [INFO] DeprecatedProteinsDigesterNFragmenter - 600 fasta sequences red (141877618000 ns.)
 * [INFO] DeprecatedProteinsDigesterNFragmenter - 700 fasta sequences red (173504473000 ns.)
 * [INFO] DeprecatedProteinsDigesterNFragmenter - 800 fasta sequences red (208268760000 ns.)
 * [INFO] DeprecatedProteinsDigesterNFragmenter - 900 fasta sequences red (238504971000 ns.)
 * [INFO] DeprecatedProteinsDigesterNFragmenter - 1000 fasta sequences red (272835342000 ns.)
 * </pre>
 */
public class DeprecatedProteinsDigesterNFragmenter {

    private static Log logger = LogFactory.getLog(DeprecatedProteinsDigesterNFragmenter.class);

    static long startTime;

    static String fastaFilename;

    static int fragmentNumberGenerated = 0;

    static double meanPeptideSize = 0;

    static JPLCleaver trypsin;

    static JPLPeptideFragmenter fragmenter;

    static Set<JPLIMSnPeakType> fragmentTypes;

    static int fragmentTypeNumber;

    static boolean fragmentSwissprot = true;

    static boolean isDebug = true;

    public static void main(String[] args) throws Exception {
        double startTime = 0;
        setUp();
        if (!fragmentSwissprot) {
            fastaFilename = "proteins.fasta";
        } else {
            fastaFilename = "uniprot-organism_subtilis.fasta";
        }
        if (!isDebug) {
            startTime = System.nanoTime();
        }
        mainTest();
        if (!isDebug) {
            System.out.println((System.nanoTime() - startTime) + " ns.");
        }
    }

    public static void setUp() throws Exception {
        setFastaFileName("proteins.fasta");
        trypsin = new JPLCleaver(new JPLTrypsinKRnotPCutter());
        fragmenter = new JPLPeptideFragmenter(EnumSet.of(JPLFragmentationType.AX, JPLFragmentationType.BY, JPLFragmentationType.IMMONIUM));
        Set<JPLITerminus> fragmentTypes = fragmenter.getFragmentTypes();
        fragmentTypeNumber = fragmentTypes.size();
    }

    public static void mainTest() {
        startTime = System.nanoTime();
        logger.info("Entering fragmentation benchmarking of file " + fastaFilename + " ...");
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
    public static void scanNProcessFastaFile(String filename) throws IOException, JPLEmptySequenceException {
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
                }
            } catch (JPLAASequenceBuilderException e) {
                if (logger.isDebugEnabled()) {
                    logger.warn(e.getMessage() + " ( " + nextSequence + ")");
                }
            }
            if (logger.isInfoEnabled()) {
                if (((i + 1) % 100) == 0) {
                    logger.info((i + 1) + " fasta sequences red (" + (System.nanoTime() - startTime) + " ns.)");
                }
                i++;
            }
        }
        if (logger.isInfoEnabled()) {
            logger.info("Total of " + i + " fasta protein sequences red.");
            logger.info("Total of " + j + " digested peptides (avg size:" + (meanPeptideSize / j) + " aas)");
            logger.info("Total of " + fragmentNumberGenerated + " generated fragments.");
        }
    }

    public static void fragmentAllPeptides(List<JPLIAASequence> peptides) throws JPLEmptySequenceException {
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

    public static JPLTheoMSnPeakList fragmentPrecursor(JPLTheoSeqBasedMSPeak precursor) throws JPLEmptySequenceException, JPLAAByteUndefinedException {
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

    public static void setFastaFileName(String filename) {
        fastaFilename = filename;
    }
}
