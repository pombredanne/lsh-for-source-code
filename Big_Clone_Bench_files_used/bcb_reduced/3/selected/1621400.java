package org.expasy.jpl.perf;

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.expasy.jpl.bio.exceptions.JPLAAByteUndefinedException;
import org.expasy.jpl.bio.exceptions.JPLEmptySequenceException;
import org.expasy.jpl.core.mol.chem.JPLMassCalculator;
import org.expasy.jpl.core.mol.polymer.pept.JPLPeptide;
import org.expasy.jpl.core.mol.polymer.pept.cutter.JPLDigestedPeptide;
import org.expasy.jpl.core.mol.polymer.pept.cutter.JPLDigester;
import org.expasy.jpl.core.mol.polymer.pept.cutter.JPLPeptidase;
import org.expasy.jpl.core.mol.polymer.pept.fragmenter.JPLFragmentationType;
import org.expasy.jpl.core.mol.polymer.pept.fragmenter.JPLPeptideFragmentationException;
import org.expasy.jpl.core.mol.polymer.pept.fragmenter.JPLPeptideFragmenter;
import org.expasy.jpl.core.ms.spectrum.JPLIMSPeakList;
import org.expasy.jpl.core.ms.spectrum.JPLMSPeakList;
import org.expasy.jpl.core.util.model.parser.JPLParseException;
import org.expasy.jpl.io.mol.fasta.JPLFastaEntry;
import org.expasy.jpl.io.mol.fasta.JPLFastaReader;

/**
 * 
 * <pre>
 * [INFO] ProteinsDigesterNFragmenter - 100 fasta sequences red (9939670000 ns.)
 * [INFO] ProteinsDigesterNFragmenter - 200 fasta sequences red (18875829000 ns.)
 * [INFO] ProteinsDigesterNFragmenter - 300 fasta sequences red (26996064000 ns.)
 * [INFO] ProteinsDigesterNFragmenter - 400 fasta sequences red (32611715000 ns.)
 * [INFO] ProteinsDigesterNFragmenter - 500 fasta sequences red (40365409000 ns.)
 * [INFO] ProteinsDigesterNFragmenter - 600 fasta sequences red (47674966000 ns.)
 * [INFO] ProteinsDigesterNFragmenter - 700 fasta sequences red (54073574000 ns.)
 * [INFO] ProteinsDigesterNFragmenter - 800 fasta sequences red (61336873000 ns.)
 * [INFO] ProteinsDigesterNFragmenter - 900 fasta sequences red (67562835000 ns.)
 * [INFO] ProteinsDigesterNFragmenter - 1000 fasta sequences red (74062168000 ns.)
 * [INFO] ProteinsDigesterNFragmenter - 1100 fasta sequences red (80900374000 ns.)
 * [INFO] ProteinsDigesterNFragmenter - 1200 fasta sequences red (90960140000 ns.)
 * [INFO] ProteinsDigesterNFragmenter - 1300 fasta sequences red (98116860000 ns.)
 * [INFO] ProteinsDigesterNFragmenter - 1400 fasta sequences red (111321976000 ns.)
 * [INFO] ProteinsDigesterNFragmenter - 1500 fasta sequences red (118441158000 ns.)
 * [INFO] ProteinsDigesterNFragmenter - 1600 fasta sequences red (122698739000 ns.)
 * [INFO] ProteinsDigesterNFragmenter - 1700 fasta sequences red (127617612000 ns.)
 * [INFO] ProteinsDigesterNFragmenter - 1800 fasta sequences red (134888881000 ns.)
 * [INFO] ProteinsDigesterNFragmenter - 1900 fasta sequences red (141880253000 ns.)
 * [INFO] ProteinsDigesterNFragmenter - 2000 fasta sequences red (148745119000 ns.)
 * [INFO] ProteinsDigesterNFragmenter - 2100 fasta sequences red (154101194000 ns.)
 * [INFO] ProteinsDigesterNFragmenter - 2200 fasta sequences red (159839246000 ns.)
 * [INFO] ProteinsDigesterNFragmenter - 2300 fasta sequences red (165013901000 ns.)
 * [INFO] ProteinsDigesterNFragmenter - 2400 fasta sequences red (170186212000 ns.)
 * [INFO] ProteinsDigesterNFragmenter - 2500 fasta sequences red (176009753000 ns.)
 * [INFO] ProteinsDigesterNFragmenter - 2600 fasta sequences red (181481542000 ns.)
 * [INFO] ProteinsDigesterNFragmenter - 2700 fasta sequences red (186889058000 ns.)
 * [INFO] ProteinsDigesterNFragmenter - 2800 fasta sequences red (192052222000 ns.)
 * [INFO] ProteinsDigesterNFragmenter - 2900 fasta sequences red (196162050000 ns.)
 * [INFO] ProteinsDigesterNFragmenter - 3000 fasta sequences red (200717249000 ns.)
 * [INFO] ProteinsDigesterNFragmenter - 3100 fasta sequences red (206025712000 ns.)
 * [INFO] ProteinsDigesterNFragmenter - 3200 fasta sequences red (210407758000 ns.)
 * [INFO] ProteinsDigesterNFragmenter - 3300 fasta sequences red (213035816000 ns.)
 * [INFO] ProteinsDigesterNFragmenter - 3400 fasta sequences red (217089579000 ns.)
 * [INFO] ProteinsDigesterNFragmenter - 3500 fasta sequences red (222114001000 ns.)
 * [INFO] ProteinsDigesterNFragmenter - 3600 fasta sequences red (226076920000 ns.)
 * [INFO] ProteinsDigesterNFragmenter - 3700 fasta sequences red (230840886000 ns.)
 * [INFO] ProteinsDigesterNFragmenter - 3800 fasta sequences red (235759518000 ns.)
 * [INFO] ProteinsDigesterNFragmenter - 3900 fasta sequences red (240649519000 ns.)
 * [INFO] ProteinsDigesterNFragmenter - 4000 fasta sequences red (245779316000 ns.)
 * [INFO] ProteinsDigesterNFragmenter - 4100 fasta sequences red (250403403000 ns.)
 * [INFO] ProteinsDigesterNFragmenter - 4200 fasta sequences red (255158158000 ns.)
 * [INFO] ProteinsDigesterNFragmenter - Total of 4254 fasta protein sequences red.
 * [INFO] ProteinsDigesterNFragmenter - Total of 139071 digested peptides (avg size:9.068993535676022 aas)
 * [INFO] ProteinsDigesterNFragmenter - Total of 4678261 generated fragments.
 * [INFO] ProteinsDigesterNFragmenter - Benchmarking over.
 * [INFO] ProteinsDigesterNFragmenter - Test exec : 257889553000 ns.
 * </pre>
 */
public class ProteinsDigesterNFragmenter {

    private static Log logger = LogFactory.getLog(ProteinsDigesterNFragmenter.class);

    static String logConfFilename = "log4j.properties";

    static long startTime;

    static String fastaFilename;

    static int fragmentNumberGenerated = 0;

    static double meanPeptideSize = 0;

    static JPLDigester digester;

    static JPLPeptideFragmenter fragmenter;

    JPLMassCalculator massCalc = JPLMassCalculator.getMonoAccuracyInstance();

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
        fastaFilename = "proteins.fasta";
        JPLPeptidase trypsin = JPLPeptidase.getInstance("Trypsin");
        digester = JPLDigester.newInstance(trypsin);
        fragmenter = new JPLPeptideFragmenter.Builder(EnumSet.of(JPLFragmentationType.AX, JPLFragmentationType.BY, JPLFragmentationType.IMMONIUM)).build();
    }

    public static void mainTest() {
        startTime = System.nanoTime();
        logger.info("Entering uniprot fragmentation benchmarking ...");
        try {
            scanNProcessFastaFile(ClassLoader.getSystemResource(fastaFilename).getFile());
        } catch (final IOException e) {
            logger.fatal(e.getMessage() + ": " + fastaFilename + " I/O error.");
        } catch (JPLParseException e) {
            logger.fatal(e.getMessage() + ": " + fastaFilename + " parse error.");
        }
        logger.info("Benchmarking over.");
        logger.info("Test exec : " + (System.nanoTime() - startTime) + " ns.");
    }

    /**
	 * Read fasta file
	 * 
	 * @param filename (optionally gzipped) fasta filename to get sequence from.
	 * @throws IOException
	 * @throws JPLParseException
	 * @throws JPLEmptySequenceException
	 * @throws JPLAAByteUndefinedException
	 * @throws Exception if ...
	 */
    public static void scanNProcessFastaFile(final String filename) throws IOException, JPLParseException {
        if (logger.isInfoEnabled()) {
            logger.info("open filename " + filename);
        }
        final JPLFastaReader reader = JPLFastaReader.newInstance();
        reader.parse(new File(filename));
        Iterator<JPLFastaEntry> it = reader.iterator();
        int i = 0;
        int j = 0;
        while (it.hasNext()) {
            final String nextSequence = it.next().getSequence();
            final JPLPeptide sequence = new JPLPeptide.Builder(nextSequence).ambiguityEnabled().build();
            if (logger.isDebugEnabled()) {
                logger.debug("Trypsin digestion on " + sequence + "(" + sequence.length() + " aas)");
            }
            if (!sequence.isAmbiguous()) {
                digester.digest(sequence);
                final Set<JPLDigestedPeptide> peptides = digester.getDigests();
                if (logger.isInfoEnabled()) {
                    j += peptides.size();
                }
                fragmentAllPeptides(peptides);
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

    public static void fragmentAllPeptides(final Set<JPLDigestedPeptide> peptides) {
        final int charge = 1;
        for (final JPLDigestedPeptide digest : peptides) {
            int currentGeneratedFragmentNumber = 0;
            if (logger.isInfoEnabled()) {
                meanPeptideSize += digest.length();
            }
            if (JPLPeptideFragmenter.isFragmentable(digest)) {
                final JPLIMSPeakList peakList = fragmentPrecursor(digest.getPeptide(), charge);
                if (logger.isInfoEnabled()) {
                    currentGeneratedFragmentNumber = peakList.size();
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
        }
    }

    public static JPLIMSPeakList fragmentPrecursor(final JPLPeptide precursor, int charge) {
        if (logger.isDebugEnabled()) {
            logger.debug("fragmentation of " + precursor);
        }
        try {
            fragmenter.setFragmentablePrecursor(precursor, charge);
            fragmenter.generateFragments();
            return fragmenter.getPeakList();
        } catch (final JPLPeptideFragmentationException e) {
            if (logger.isDebugEnabled()) {
                logger.warn(e.getMessage());
            }
        }
        return JPLMSPeakList.emptyInstance();
    }
}
