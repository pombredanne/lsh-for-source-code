package org.expasy.jpl.core.mol.polymer.pept.cutter;

import static org.junit.Assert.assertEquals;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import junit.framework.Assert;
import org.apache.commons.collections15.Transformer;
import org.expasy.jpl.commons.base.cond.Condition;
import org.expasy.jpl.commons.base.cond.ConditionImpl;
import org.expasy.jpl.commons.base.cond.operator.impl.OperatorLowerThan;
import org.expasy.jpl.core.mol.chem.ChemicalFacade;
import org.expasy.jpl.core.mol.modif.ModificationFactory;
import org.expasy.jpl.core.mol.polymer.pept.Peptide;
import org.expasy.jpl.core.mol.polymer.pept.matcher.AAMotifMatcher;
import org.expasy.jpl.core.mol.polymer.pept.rule.EditionRule;
import org.expasy.jpl.core.mol.polymer.pept.rule.PeptideEditorFactory;
import org.expasy.jpl.core.mol.polymer.pept.rule.EditionRule.EditionAction;
import org.expasy.jpl.core.mol.polymer.pept.term.CTerminus;
import org.expasy.jpl.core.mol.polymer.pept.term.NTerminus;
import org.junit.Before;
import org.junit.Test;

public class JPLDigesterTest {

    private Peptide sequence;

    private Peptidase trypsin;

    private Digester digester;

    @Before
    public void setUp() throws Exception {
        sequence = new Peptide.Builder("RESALYTNIKALASKR").build();
        trypsin = Peptidase.getInstance("Trypsin");
        digester = Digester.newInstance(trypsin);
    }

    @Test
    public void testSetNumberOfMissedCleavage() {
        digester.setNumberOfMissedCleavage(2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetNumberOfMissedCleavageError() {
        digester.setNumberOfMissedCleavage(143);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadDigest() {
        sequence = null;
        digester.digest(sequence);
    }

    @Test
    public void testDigestedPeptideAmbiguity() {
        Peptide protein = new Peptide.Builder("MQRSTATGCFKXL").ambiguityEnabled().cterm(CTerminus.PROT_C).nterm(NTerminus.PROT_N).build();
        digester.digest(protein);
        Assert.assertTrue(protein.isAmbiguous());
        Iterator<DigestedPeptide> iter = digester.getDigests().iterator();
        boolean isAmb = false;
        while (iter.hasNext()) {
            DigestedPeptide digest = iter.next();
            if (digest.isAmbiguous()) {
                isAmb = true;
            }
        }
        Assert.assertTrue(isAmb);
    }

    @Test
    public void testDigestionWithMissedCleavages() {
        digester.setNumberOfMissedCleavage(2);
        digester.digest(sequence);
        final Set<DigestedPeptide> digestSet = digester.getDigests();
        System.out.println(digestSet);
        assertEquals(8, digestSet.size());
    }

    @Test
    public void testDigestionWithMissedCleavages1() throws ParseException {
        Peptide peptide = new Peptide.Builder("QQDDFGKSVTDCTSNFCLFQSNSK").build();
        EditionRule cysCAM = EditionRule.newInstance("Cys_CAM", AAMotifMatcher.newInstance("C"), EditionAction.newFixedModifAction(ModificationFactory.valueOf(ChemicalFacade.getMolecule("C2H3NO"))));
        PeptideEditorFactory factory = PeptideEditorFactory.newInstance(cysCAM);
        Peptide digest = factory.transform(peptide).iterator().next();
        digester.setNumberOfMissedCleavage(1);
        digester.digest(digest);
        final Set<DigestedPeptide> digestSet = digester.getDigests();
        System.out.println(digestSet);
        for (DigestedPeptide digestedPept : digestSet) {
            if (digestedPept.getMC() == 1) {
                Assert.assertEquals("QQDDFGKSVTDC(C2H3NO)TSNFC(C2H3NO)LFQSNSK", digestedPept.toAAString());
            }
        }
    }

    @Test
    public void testOutOfBoundDigestion() {
        final Peptidase enzyme = Peptidase.getInstance("enz", new CleavageSiteCutter.Builder("[DE]|X").build());
        digester = Digester.newInstance(enzyme);
        final String sequenceString = "MASRKLRDQIVIATKFTTDYKGYDVGKGKSANFCGNHKRSLHVSVRDSLRKLQTDWIDIL" + "YVHWWDYMSSIEEVMDSLHILVQQGKVLYLGVSDTPAWVVSAANYYATSHGKTPFSIYQG" + "KWNVLNRDFERDIIPMARHFGMALAPWDVMGGGRFQSKKAVEERKKKGEGLRTFFGTSEQ" + "TDMEVKISEALLKVAEEHGTESVTAIAIAYVRSKAKHVFPLVGGRKIEHLKQNIEALSIK" + "LTPEQIKYLESIVPFDVGFPTNFIGDDPAVTKKPSFLTEMSAKISFED";
        final Peptide sequence = new Peptide.Builder(sequenceString).build();
        digester.digest(sequence);
        final Set<DigestedPeptide> digests = digester.getDigests();
        System.out.println(digests);
    }

    @Test
    public void testNumberOfCleavageSite() {
        final Peptidase enzyme = Peptidase.getInstance("enz", new CleavageSiteCutter.Builder("[DE]|X").build());
        final String sequenceString = "ASLLTAMSDAQISFD";
        digester = Digester.newInstance(enzyme);
        final Peptide sequence = new Peptide.Builder(sequenceString).build();
        digester.digest(sequence);
        assertEquals(1, digester.getNumOfCleavageSites(), 0);
    }

    @Test
    public void testNumberOfCleavageSiteForAspN() {
        final Peptidase enzyme = Peptidase.getInstance("aspn", new CleavageSiteCutter.Builder("X|D").build());
        final String sequenceString = "DFVESNTIFNLNTVK";
        digester = Digester.newInstance(enzyme);
        final Peptide sequence = new Peptide.Builder(sequenceString).build();
        digester.digest(sequence);
        assertEquals(0, digester.getNumOfCleavageSites(), 0);
    }

    @Test
    public void testNumberOfCleavageSiteFromTrypsin() {
        final String sequenceString = "ASLLTAMRSAQISFK";
        final Peptide sequence = new Peptide.Builder(sequenceString).build();
        digester.digest(sequence);
        assertEquals(1, digester.getNumOfCleavageSites(), 0);
    }

    @Test
    public void testDigestionWithFilter() {
        final String sequenceString = "ASLLTAMRSAQISFK";
        final Peptide sequence = new Peptide.Builder(sequenceString).build();
        Transformer<DigestedPeptide, Integer> digestLength = new Transformer<DigestedPeptide, Integer>() {

            public Integer transform(DigestedPeptide digest) {
                return digest.length();
            }
        };
        Condition<DigestedPeptide> cond = new ConditionImpl.Builder<DigestedPeptide, Integer>(8).accessor(digestLength).operator(OperatorLowerThan.newInstance()).build();
        digester.setCondition(cond);
        digester.digest(sequence);
        Assert.assertEquals(1, digester.getDigests().size());
    }

    @Test
    public void testSeqOfPeptidasesDigestion() {
        final String sequenceString = "ASLLTAMDRSAQISFK";
        final Peptide sequence = new Peptide.Builder(sequenceString).build();
        final Peptidase aspn = Peptidase.getInstance("aspn", new CleavageSiteCutter.Builder("X|D").build());
        Digester digester1 = Digester.newInstance(aspn);
        Digester digester2 = Digester.newInstance(trypsin);
        digester1.digest(sequence);
        System.out.println("set of digests by aspn: " + digester1.getDigests());
        Assert.assertEquals(2, digester1.getDigests().size());
        Set<DigestedPeptide> digests = new HashSet<DigestedPeptide>();
        for (DigestedPeptide pep : digester1.getDigests()) {
            digester2.digest(pep.getPeptide());
            if (digester2.hasDigests()) {
                digests.addAll(digester2.getDigests());
            }
        }
        System.out.println("set of digests by aspn+trypsin: " + digests);
        Assert.assertEquals(2, digester1.getDigests().size());
    }

    @Test
    public void testCnbrDigest() {
        final String sequenceString = "AHNGMRWPTG";
        final Peptide sequence = new Peptide.Builder(sequenceString).build();
        Peptidase cnbr = Peptidase.getInstance("CNBr");
        digester = Digester.newInstance(cnbr);
        digester.digest(sequence);
        Set<DigestedPeptide> digests = digester.getDigests();
        System.out.println(digests);
        assertEquals(1, digester.getNumOfCleavageSites(), 0);
    }

    @Test
    public void testDigest2() {
        final String sequenceString = "AHNGMRWPTG";
        final Peptide sequence = new Peptide.Builder(sequenceString).build();
        Peptidase cnbr = Peptidase.getInstance("CNBr");
        digester = Digester.newInstance(cnbr);
        digester.digest(sequence);
        Set<DigestedPeptide> digests = digester.getDigests();
        System.out.println(digests);
        assertEquals(1, digester.getNumOfCleavageSites(), 0);
    }

    @Test
    public void testCustomPeptidase() {
        String sequenceString = "HKRDMHSNKR";
        Peptide sequence = new Peptide.Builder(sequenceString).build();
        CleavageSiteCutter siteCutter = new CleavageSiteCutter.Builder("X|[DN] or [DN]|X").build();
        Peptidase yass = Peptidase.getInstance("custom", siteCutter);
        digester = Digester.newInstance(yass);
        digester.digest(sequence);
        Set<DigestedPeptide> digests = digester.getDigests();
        Assert.assertEquals(5, digests.size());
        digester.setNumberOfMissedCleavage(1);
        digester.digest(sequence);
        digests = digester.getDigests();
        Assert.assertEquals(9, digests.size());
        digester.setNumberOfMissedCleavage(2);
        digester.digest(sequence);
        digests = digester.getDigests();
        Assert.assertEquals(12, digests.size());
        Iterator<Integer> iter = siteCutter.iterator(sequence);
        List<Integer> indices = new ArrayList<Integer>();
        while (iter.hasNext()) {
            indices.add(iter.next());
        }
        Assert.assertEquals(Arrays.asList(3, 4, 7, 8), indices);
    }
}
