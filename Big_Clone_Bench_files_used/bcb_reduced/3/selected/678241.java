package org.expasy.jpl.tools.dig;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.collections15.Transformer;
import org.expasy.jpl.commons.app.AbstractApplicationParameters;
import org.expasy.jpl.commons.app.JPLTerminalApplication;
import org.expasy.jpl.commons.base.cond.Condition;
import org.expasy.jpl.commons.base.cond.ConditionImpl;
import org.expasy.jpl.commons.base.cond.operator.OperatorManager;
import org.expasy.jpl.commons.base.task.TerminalProgressBar;
import org.expasy.jpl.core.mol.chem.MassCalculator;
import org.expasy.jpl.core.mol.polymer.BioPolymerUtils;
import org.expasy.jpl.core.mol.polymer.pept.Peptide;
import org.expasy.jpl.core.mol.polymer.pept.cutter.DigestedPeptide;
import org.expasy.jpl.core.mol.polymer.pept.cutter.Digester;
import org.expasy.jpl.core.mol.polymer.pept.fragmenter.PeptideFragmentationException;
import org.expasy.jpl.core.mol.polymer.pept.term.CTerminus;
import org.expasy.jpl.core.mol.polymer.pept.term.NTerminus;
import org.expasy.jpl.io.mol.fasta.FastaEntry;
import org.expasy.jpl.io.mol.fasta.FastaReader;

/**
 * http://web.expasy.org/compute_pi/
 * 
 * This application digests proteins with given enzymes and present a set of
 * peptides with many informations such as:
 * 
 * <ul>
 * <li>the sequence</li>
 * <li>the monoisotopic mass weigh</li>
 * <li>the isolelectric point</li>
 * </ul>
 * 
 * @author nikitin
 * 
 * @version 1.0.0
 * 
 */
public class ProteinDigester implements JPLTerminalApplication {

    private static final Transformer<DigestedPeptide, Integer> DIGEST_LEN;

    @SuppressWarnings("unused")
    private static final Transformer<DigestedPeptide, Boolean> IS_DIGEST_AMBIGUOUS;

    private final Parameters params;

    private Condition<DigestedPeptide> filter;

    static {
        DIGEST_LEN = new Transformer<DigestedPeptide, Integer>() {

            public Integer transform(DigestedPeptide digest) {
                return digest.length();
            }
        };
        IS_DIGEST_AMBIGUOUS = new Transformer<DigestedPeptide, Boolean>() {

            public Boolean transform(DigestedPeptide digest) {
                return digest.isAmbiguous();
            }
        };
    }

    ProteinDigester(final String[] args) {
        params = new Parameters(this.getClass(), args);
        if (params.getPeptLenFilter() > 0) {
            filter = new ConditionImpl.Builder<DigestedPeptide, Integer>(params.getPeptLenFilter()).accessor(DIGEST_LEN).operator(OperatorManager.GREATER_EQUALS).build();
        }
    }

    public static void main(String[] args) throws IOException {
        final ProteinDigester app = new ProteinDigester(args);
        app.params.setHeader(app.getClass().getSimpleName() + " v" + app.params.getVersion() + " developed by Fred Nikitin.\n" + "Copyright (c) 2011 Proteome Informatics Group in Swiss " + "Institute of Bioinformatics.\n");
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
	 * @throws JPLParseException if parsing failed.
	 * @throws IOException if file does not exist.
	 */
    public Iterator<DigestedPeptide> digestProteins(File fastaFile, PrintWriter tsvPw) throws ParseException, IOException, PeptideFragmentationException {
        FastaReader reader = FastaReader.newInstance();
        reader.setProgressBar(TerminalProgressBar.indeterminate());
        reader.parse(fastaFile);
        Iterator<FastaEntry> it = reader.iterator();
        List<DigestedPeptide> digests = new ArrayList<DigestedPeptide>();
        while (it.hasNext()) {
            FastaEntry nextEntry = it.next();
            Peptide protein = new Peptide.Builder(nextEntry.getSequence()).nterm(NTerminus.PROT_N).cterm(CTerminus.PROT_C).ambiguityEnabled().build();
            for (Digester digester : params.getDigesters()) {
                if (filter != null) {
                    digester.setCondition(filter);
                }
                digester.digest(protein);
                for (DigestedPeptide digest : digester.getDigests()) {
                    tsvPw.append(digest2String(digest));
                    tsvPw.append("\n");
                    tsvPw.flush();
                }
            }
        }
        return digests.iterator();
    }

    public String digest2String(DigestedPeptide digest) {
        StringBuilder sb = new StringBuilder();
        NumberFormat formatter = params.getFormatter();
        String fieldDelimiter = params.getFieldDelimiter();
        MassCalculator massCalc = params.getMassCalc();
        double pI = BioPolymerUtils.getIsoelectricPoint(digest.getPeptide());
        for (int field : params.getFields()) {
            switch(field) {
                case 1:
                    sb.append(digest.getPeptide().toAAString() + fieldDelimiter);
                    break;
                case 2:
                    if (!digest.isAmbiguous()) {
                        double mw = massCalc.getMass(digest.getPeptide());
                        sb.append(formatter.format(mw) + fieldDelimiter);
                    } else {
                        sb.append("NA" + fieldDelimiter);
                    }
                    break;
                case 3:
                    sb.append(formatter.format(pI) + fieldDelimiter);
                    break;
                case 4:
                    sb.append(digest.getEnzyme().getId() + fieldDelimiter);
                    break;
                case 5:
                    sb.append(digest.getMC() + fieldDelimiter);
                    break;
            }
        }
        sb.delete(sb.length() - 1, sb.length());
        return sb.toString();
    }

    @Override
    public AbstractApplicationParameters getParameters() {
        return params;
    }

    @Override
    public void run() throws Exception {
        try {
            File tsvFile = params.getTSVFile();
            PrintWriter pw = new PrintWriter(tsvFile);
            StringBuilder sb = new StringBuilder();
            for (int field : params.getFields()) {
                sb.append(Parameters.OUTPUT_FIELDS.get(field - 1) + params.getFieldDelimiter());
            }
            sb.delete(sb.length() - 1, sb.length());
            pw.append(sb);
            pw.append("\n");
            digestProteins(params.getFastaFile(), pw);
            pw.flush();
            pw.close();
        } catch (ParseException e) {
            System.err.println(e);
            System.exit(2);
        } catch (PeptideFragmentationException e) {
            System.err.println(e);
            System.exit(3);
        }
    }
}
