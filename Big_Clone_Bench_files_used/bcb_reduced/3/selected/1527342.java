package org.expasy.jpl.perf;

import java.io.IOException;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.expasy.jpl.bio.exceptions.JPLAAByteUndefinedException;
import org.expasy.jpl.bio.exceptions.JPLAASequenceBuilderException;
import org.expasy.jpl.bio.exceptions.JPLEmptySequenceException;
import org.expasy.jpl.bio.sequence.JPLAASequence;
import org.expasy.jpl.bio.sequence.JPLIAASequence;
import org.expasy.jpl.bio.sequence.io.fasta.JPLFastaReader;
import org.expasy.jpl.bio.sequence.tools.positions.cutter.JPLCleaver;
import org.expasy.jpl.bio.sequence.tools.positions.cutter.JPLTrypsinKRnotPCutter;

/**
 * Testing digestion + fragmentation of all proteins from uniprot.
 * 
 */
public class DeprecatedProteinsDigester {

    private static Log logger = LogFactory.getLog(DeprecatedProteinsDigester.class);

    static long startTime;

    static String fastaFilename;

    static JPLCleaver trypsin;

    static boolean isDebug = true;

    public static void main(String[] args) throws Exception {
        double startTime = 0;
        setUp();
        fastaFilename = "uniprot-organism_subtilis.fasta";
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
    }

    public static void mainTest() {
        startTime = System.nanoTime();
        logger.info("Entering digest benchmark of file " + fastaFilename + " ...");
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
                }
            } catch (JPLAASequenceBuilderException e) {
                if (logger.isDebugEnabled()) {
                    logger.warn(e.getMessage() + " ( " + nextSequence + ")");
                }
            }
            if (logger.isInfoEnabled()) {
                if (((i + 1) % 1000) == 0) {
                    logger.info((i + 1) + " fasta sequences red (" + (System.nanoTime() - startTime) + " ns.)");
                }
                i++;
            }
        }
        if (logger.isInfoEnabled()) {
            logger.info("Total of " + i + " fasta protein sequences red.");
            logger.info("Total of " + j + " digested peptides.");
        }
    }

    public static void setFastaFileName(String filename) {
        fastaFilename = filename;
    }
}
