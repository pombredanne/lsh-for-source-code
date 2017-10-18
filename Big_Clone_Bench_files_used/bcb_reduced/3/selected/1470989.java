package org.expasy.jpl.demo;

import java.io.IOException;
import org.expasy.jpl.bio.exceptions.JPLEmptySequenceException;
import org.expasy.jpl.bio.sequence.JPLAASequence;
import org.expasy.jpl.bio.sequence.JPLIAASequence;
import org.expasy.jpl.bio.sequence.io.fasta.JPLFastaEntry;
import org.expasy.jpl.bio.sequence.io.fasta.JPLFastaHeader;
import org.expasy.jpl.bio.sequence.io.fasta.JPLFastaReader;
import org.expasy.jpl.bio.sequence.io.fasta.JPLFastaWriter;
import org.expasy.jpl.bio.sequence.tools.positions.cutter.JPLCleaver;
import org.expasy.jpl.bio.sequence.tools.positions.cutter.JPLTrypsinKRnotPCutter;
import org.expasy.jpl.commons.ms.peaklist.JPLIPeakList;
import org.expasy.jpl.commons.ms.peaklist.JPLPeakList;
import org.expasy.jpl.insilico.ms.peak.JPLITheoMSPeak;
import org.expasy.jpl.insilico.ms.peak.JPLTheoMSPeak;
import org.expasy.jpl.utils.parser.JPLParseException;

public class IOFastaExample {

    static String filename = "test.fasta";

    /**
	 * @param args
	 * @throws IOException
	 * @throws JPLEmptySequenceException
	 * @throws JPLParseException
	 */
    public static void main(String[] args) throws IOException, JPLParseException, JPLEmptySequenceException {
        writeSequence();
        readSequence();
    }

    public static void readPeaks() {
        JPLIPeakList peakList = new JPLPeakList(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5, 6.6 });
        JPLITheoMSPeak peak = new JPLTheoMSPeak(12.4);
        for (int i = 0; i < peakList.getNbPeak(); i++) {
            System.out.println(peakList.getMzAt(i));
        }
        System.out.println(peak);
    }

    public static void writeSequence() throws IOException {
        JPLIAASequence sequence = new JPLAASequence.Builder("MEKKSIAGLCFLFLVL" + "FVAQEVVVQSEAKTCENLVDTYRGPCFTTGSCDDHCKNKEHLLS" + "GRCRDDVRCWCTRNC").build();
        JPLFastaHeader header = new JPLFastaHeader("Q4U9M9");
        JPLFastaEntry entry = new JPLFastaEntry(header, sequence.toAAString());
        JPLFastaWriter writer = new JPLFastaWriter(filename);
        writer.addEntry(entry);
        writer.flush();
    }

    public static void readSequence() throws IOException {
        JPLFastaReader fastaScanner = new JPLFastaReader(filename);
        while (fastaScanner.hasNext()) {
            String nextSequence = fastaScanner.nextFastaSequence();
            System.out.println(nextSequence);
        }
    }

    public void cutPeptideWithTrypsin() throws JPLParseException, JPLEmptySequenceException {
        JPLTrypsinKRnotPCutter trypsin = new JPLTrypsinKRnotPCutter();
        JPLIAASequence sequence = new JPLAASequence.Builder("SALYTNIKALAS").build();
        JPLCleaver protease = new JPLCleaver(trypsin);
        protease.digest(sequence);
    }
}
