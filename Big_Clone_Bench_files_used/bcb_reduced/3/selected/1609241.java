package org.expasy.jpl.tools.fastaDecoy;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.expasy.jpl.commons.app.AbstractApplicationParameters;
import org.expasy.jpl.commons.app.JPLTerminalApplication;
import org.expasy.jpl.commons.base.process.ProcessException;
import org.expasy.jpl.core.mol.polymer.pept.Peptide;
import org.expasy.jpl.core.mol.polymer.pept.cutter.DigestedPeptide;
import org.expasy.jpl.core.mol.polymer.pept.cutter.Digester;
import org.expasy.jpl.core.mol.polymer.pept.fragmenter.FragmentationType;
import org.expasy.jpl.core.mol.polymer.pept.fragmenter.PeptideFragmentationException;
import org.expasy.jpl.core.mol.polymer.pept.fragmenter.PeptideFragmenter;
import org.expasy.jpl.core.mol.polymer.pept.term.CTerminus;
import org.expasy.jpl.core.mol.polymer.pept.term.NTerminus;
import org.expasy.jpl.core.ms.spectrum.PeakListImpl;
import org.expasy.jpl.io.mol.fasta.FastaEntry;
import org.expasy.jpl.io.mol.fasta.FastaHeader;
import org.expasy.jpl.io.mol.fasta.FastaHeaderFormatManager;
import org.expasy.jpl.io.mol.fasta.FastaReader;
import org.expasy.jpl.io.mol.fasta.FastaUtils;
import org.expasy.jpl.io.mol.fasta.FastaWriter;
import org.expasy.jpl.msmatch.PeakListMatcherImpl;
import org.expasy.jpl.msmatch.scorer.NCorrScorer;

public class FastaDecoy implements JPLTerminalApplication {

    private final Parameters params;

    HashMap<String, String> uniquePeptides;

    FastaDecoy(final String[] args) {
        params = new Parameters(this.getClass(), args);
    }

    public static void main(String[] args) throws IOException {
        final FastaDecoy app = new FastaDecoy(args);
        app.params.setHeader(app.getClass().getSimpleName() + " v" + app.params.getVersion() + " developed by Markus Mueller.\n" + "Copyright (c) 2012 Proteome Informatics Group in Swiss " + "Institute of Bioinformatics.\n");
        try {
            if (app.params.isVerbose()) {
                System.out.println(app.params.getParamInfos());
            }
            app.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	 * Digest all fasta entries.
	 * 
	 * @param filename the fasta filename.
	 * 
	 * @throws ParseException if parsing failed.
	 * @throws IOException if file does not exist.
	 * @throws ProcessException
	 */
    public Iterator<DigestedPeptide> digestProteins(File fastaFile, File decoyFile) throws ParseException, IOException, PeptideFragmentationException, ProcessException {
        FastaReader reader = FastaReader.newInstance();
        FastaHeaderFormatManager manager = params.getFormatMgr();
        reader.setHeaderManager(manager);
        FastaWriter writer = FastaWriter.newInstance();
        reader.parse(fastaFile);
        Iterator<FastaEntry> it = reader.iterator();
        uniquePeptides = new HashMap<String, String>();
        List<DigestedPeptide> digests = new ArrayList<DigestedPeptide>();
        writer.open(decoyFile);
        while (it.hasNext()) {
            FastaEntry orgEntry = it.next();
            if (params.doAppend()) {
                writer.add(orgEntry);
            }
            FastaEntry decoyEntry = null;
            String sequenceString = orgEntry.getSequence();
            String decoyProteinSeq = "";
            FastaHeader decoyHeader = FastaUtils.makeDecoyHeader(orgEntry.getHeader(), params.getDecoyFlag(), params.getDecoyFlagPosition());
            if (params.getMethod().equals("proteinReverse")) {
                decoyProteinSeq = FastaUtils.reverseSequence(sequenceString);
            } else if (params.getMethod().equals("proteinShuffle")) {
                decoyProteinSeq = FastaUtils.shuffleSequence(sequenceString);
            } else if (params.getMethod().equals("peptideShuffle")) {
                Peptide protein = new Peptide.Builder(orgEntry.getSequence()).nterm(NTerminus.PROT_N).cterm(CTerminus.PROT_C).ambiguityEnabled().build();
                Digester digester = params.getDigester();
                digester.digest(protein);
                for (DigestedPeptide digest : digester.getAllDigests()) {
                    String peptideSeq = digest.getSymbolSequence().toString();
                    if (uniquePeptides.containsKey(peptideSeq)) {
                        decoyProteinSeq += uniquePeptides.get(peptideSeq);
                    } else {
                        String shuffledSeq = peptideSeq;
                        if (peptideSeq.length() > params.getPeptLenFilter()) {
                            if (params.getKeepTermini().equalsIgnoreCase("cterm")) {
                                shuffledSeq = shuffleSequenceKeepCTerm(peptideSeq);
                            } else if (params.getKeepTermini().equalsIgnoreCase("nterm")) {
                                shuffledSeq = shuffleSequenceKeepNTerm(peptideSeq);
                            } else {
                                shuffledSeq = shuffleSequence(peptideSeq);
                            }
                        }
                        uniquePeptides.put(peptideSeq, shuffledSeq);
                        decoyProteinSeq += shuffledSeq;
                    }
                }
            }
            decoyEntry = new FastaEntry(decoyHeader, decoyProteinSeq);
            writer.add(decoyEntry);
        }
        writer.close();
        return digests.iterator();
    }

    public String digest2String(DigestedPeptide digest) {
        return digest.getPeptide().toAAString();
    }

    private String shuffleSequenceKeepCTerm(String peptideSeq) throws ProcessException, IOException {
        String shuffledSeq = FastaUtils.shuffleSequenceKeepCTerm(peptideSeq);
        String shuffledSeqMin = shuffledSeq;
        double minSim = 1.0;
        if (params.getSimilarityThreshold() > 0 && !peptideSeq.matches(".*[BJOUXZ]+.*")) {
            for (int j = 0; j < params.getNumberShuffleTrials(); j++) {
                double sim = calculateMSSim(peptideSeq, shuffledSeq);
                if (sim < minSim) {
                    minSim = sim;
                    shuffledSeqMin = shuffledSeq;
                }
                if (sim < params.getSimilarityThreshold() && !uniquePeptides.containsKey(shuffledSeq)) break;
                shuffledSeq = FastaUtils.shuffleSequenceKeepCTerm(peptideSeq);
            }
        }
        return shuffledSeqMin;
    }

    private String shuffleSequenceKeepNTerm(String peptideSeq) throws ProcessException {
        String shuffledSeq = FastaUtils.shuffleSequenceKeepNTerm(peptideSeq);
        String shuffledSeqMin = shuffledSeq;
        double minSim = 1.0;
        if (params.getSimilarityThreshold() > 0 && !peptideSeq.matches(".*[BJOUXZ]+.*")) {
            for (int j = 0; j < params.getNumberShuffleTrials(); j++) {
                double sim = calculateMSSim(peptideSeq, shuffledSeq);
                if (sim < minSim) {
                    minSim = sim;
                    shuffledSeqMin = shuffledSeq;
                }
                if (sim < params.getSimilarityThreshold() && !uniquePeptides.containsKey(shuffledSeq)) break;
                shuffledSeq = FastaUtils.shuffleSequenceKeepNTerm(peptideSeq);
            }
        }
        return shuffledSeqMin;
    }

    private String shuffleSequence(String peptideSeq) throws ProcessException {
        String shuffledSeq = FastaUtils.shuffleSequence(peptideSeq);
        String shuffledSeqMin = shuffledSeq;
        double minSim = 1.0;
        if (params.getSimilarityThreshold() > 0 && !peptideSeq.matches(".*[BJOUXZ]+.*")) {
            for (int j = 0; j < params.getNumberShuffleTrials(); j++) {
                double sim = calculateMSSim(peptideSeq, shuffledSeq);
                if (sim < minSim) {
                    minSim = sim;
                    shuffledSeqMin = shuffledSeq;
                }
                if (sim < params.getSimilarityThreshold() && !uniquePeptides.containsKey(shuffledSeq)) break;
                shuffledSeq = FastaUtils.shuffleSequence(peptideSeq);
            }
        }
        return shuffledSeqMin;
    }

    private double calculateMSSim(String peptide1, String peptide2) throws ProcessException {
        Peptide prec1 = new Peptide.Builder(peptide1).protons(1).build();
        Peptide prec2 = new Peptide.Builder(peptide2).protons(1).build();
        PeptideFragmenter fragmenter = new PeptideFragmenter.Builder(FragmentationType.BY).build();
        fragmenter.process(prec1);
        PeakListImpl theoPeakList1 = fragmenter.getPeakList();
        fragmenter.process(prec2);
        PeakListImpl theoPeakList2 = fragmenter.getPeakList();
        PeakListMatcherImpl matcher = PeakListMatcherImpl.withTol(0.01);
        matcher.disableConflictResolution();
        matcher.setScorer(NCorrScorer.getInstance());
        matcher.computeMatch(theoPeakList1, theoPeakList2);
        return matcher.getScore();
    }

    @Override
    public AbstractApplicationParameters getParameters() {
        return params;
    }

    @Override
    public void run() throws Exception {
        try {
            File decoyFile = params.getOutFile();
            digestProteins(params.getFastaFile(), decoyFile);
        } catch (ParseException e) {
            System.err.println(e);
            System.exit(2);
        } catch (PeptideFragmentationException e) {
            System.err.println(e);
            System.exit(3);
        }
    }
}
