package org.expasy.jpl.bio.sequence.tools.positions.cutter;

import static org.junit.Assert.*;
import java.util.List;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.expasy.jpl.bio.exceptions.JPLEmptySequenceException;
import org.expasy.jpl.bio.sequence.JPLAASequence;
import org.expasy.jpl.bio.sequence.JPLIAASequence;
import org.expasy.jpl.bio.sequence.tools.positions.cutter.JPLCleaver;
import org.expasy.jpl.bio.sequence.tools.positions.cutter.JPLRegExpCutter;
import org.expasy.jpl.bio.sequence.tools.positions.cutter.JPLTrypsinKRnotPCutter;
import org.expasy.jpl.utils.parser.JPLParseException;
import org.junit.Before;
import org.junit.Test;

public class JPLCleaverTest {

    private static Log logger = LogFactory.getLog(JPLCleaverTest.class);

    private JPLIAASequence sequence;

    private JPLCleaver trypsin;

    private JPLCleaver regexTrypsin;

    @Before
    public void setUp() throws Exception {
        sequence = new JPLAASequence.Builder("RESALYTNIKALASKR").build();
        trypsin = new JPLCleaver(new JPLTrypsinKRnotPCutter());
        String trypsinPattern = "(?<=[KR])(?=[^P])";
        regexTrypsin = new JPLCleaver(new JPLRegExpCutter(trypsinPattern));
    }

    @Test
    public void testSetNumberOfMissedCleavage() {
        trypsin.setNumberOfMissedCleavage(2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetNumberOfMissedCleavageError() {
        trypsin.setNumberOfMissedCleavage(143);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadDigest() {
        sequence = null;
        trypsin.digest(sequence);
    }

    @Test
    public void testDigest() {
        trypsin.digest(sequence);
        regexTrypsin.digest(sequence);
        List<JPLIAASequence> digests = trypsin.getDigests();
        List<JPLIAASequence> digests2 = regexTrypsin.getDigests();
        logger.info(digests);
        assertEquals(digests.size(), digests2.size(), 0);
        assertEquals(digests.size(), 4, 0);
    }

    @Test
    public void testDigestionWithMissedCleavages() throws JPLEmptySequenceException {
        trypsin.setNumberOfMissedCleavage(2);
        trypsin.digest(sequence);
        regexTrypsin.setNumberOfMissedCleavage(2);
        regexTrypsin.digest(sequence);
        List<JPLIAASequence> digests = trypsin.getDigests();
        Set<JPLIAASequence> digestSet = trypsin.getUniqueDigests();
        Set<JPLIAASequence> digests2 = regexTrypsin.getUniqueDigests();
        logger.info(digests);
        assertEquals(digestSet.size(), digests2.size(), 0);
        assertEquals(digestSet.size(), 8, 0);
    }

    @Test
    public void testOutOfBoundDigestion() throws JPLEmptySequenceException, JPLParseException {
        JPLCleaver enzyme = new JPLCleaver(new JPLRegExpCutter("(?<=[DE])"));
        String sequenceString = "MASRKLRDQIVIATKFTTDYKGYDVGKGKSANFCGNHKRSLHVSVRDSLRKLQTDWIDIL" + "YVHWWDYMSSIEEVMDSLHILVQQGKVLYLGVSDTPAWVVSAANYYATSHGKTPFSIYQG" + "KWNVLNRDFERDIIPMARHFGMALAPWDVMGGGRFQSKKAVEERKKKGEGLRTFFGTSEQ" + "TDMEVKISEALLKVAEEHGTESVTAIAIAYVRSKAKHVFPLVGGRKIEHLKQNIEALSIK" + "LTPEQIKYLESIVPFDVGFPTNFIGDDPAVTKKPSFLTEMSAKISFED";
        JPLIAASequence sequence = new JPLAASequence.Builder(sequenceString).build();
        enzyme.digest(sequence);
        List<JPLIAASequence> digests = enzyme.getDigests();
        logger.info(digests);
    }

    @Test
    public void testNumberOfCleavageSite() throws JPLEmptySequenceException, JPLParseException {
        JPLCleaver enzyme = new JPLCleaver(new JPLRegExpCutter("(?<=[DE])"));
        String sequenceString = "ASLLTAMSDAQISFD";
        JPLIAASequence sequence = new JPLAASequence.Builder(sequenceString).build();
        enzyme.digest(sequence);
        assertEquals(1, enzyme.getNumOfCleavageSites(), 0);
    }

    @Test
    public void testNumberOfCleavageSiteForAspN() throws JPLEmptySequenceException, JPLParseException {
        JPLCleaver enzyme = new JPLCleaver(new JPLRegExpCutter("(?=D)"));
        String sequenceString = "DFVESNTIFNLNTVK";
        JPLIAASequence sequence = new JPLAASequence.Builder(sequenceString).build();
        enzyme.digest(sequence);
        assertEquals(0, enzyme.getNumOfCleavageSites(), 0);
    }

    @Test
    public void testNumberOfCleavageSiteFromTrypsin() throws JPLEmptySequenceException, JPLParseException {
        String sequenceString = "ASLLTAMRSAQISFK";
        JPLIAASequence sequence = new JPLAASequence.Builder(sequenceString).build();
        regexTrypsin.digest(sequence);
        assertEquals(1, regexTrypsin.getNumOfCleavageSites(), 0);
    }
}
