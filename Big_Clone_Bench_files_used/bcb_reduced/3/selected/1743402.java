package uk.ac.ebi.rhea.biopax.level3;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.apache.log4j.Logger;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level2.conversion;
import org.biopax.paxtools.model.level3.BiochemicalReaction;
import org.biopax.paxtools.model.level3.CellularLocationVocabulary;
import org.biopax.paxtools.model.level3.Conversion;
import org.biopax.paxtools.model.level3.ConversionDirectionType;
import org.biopax.paxtools.model.level3.EntityReference;
import org.biopax.paxtools.model.level3.NucleicAcid;
import org.biopax.paxtools.model.level3.PhysicalEntity;
import org.biopax.paxtools.model.level3.Protein;
import org.biopax.paxtools.model.level3.Provenance;
import org.biopax.paxtools.model.level3.PublicationXref;
import org.biopax.paxtools.model.level3.RelationshipXref;
import org.biopax.paxtools.model.level3.SimplePhysicalEntity;
import org.biopax.paxtools.model.level3.Stoichiometry;
import org.biopax.paxtools.model.level3.Transport;
import org.biopax.paxtools.model.level3.TransportWithBiochemicalReaction;
import org.biopax.paxtools.model.level3.UnificationXref;
import org.biopax.paxtools.model.level3.Xref;
import uk.ac.ebi.biobabel.citations.DataSource;
import uk.ac.ebi.cdb.webservice.Citation;
import uk.ac.ebi.rhea.biopax.IBiopaxBiochemicalReaction;
import uk.ac.ebi.rhea.biopax.LocationCV;
import uk.ac.ebi.rhea.biopax.ReactionsRelationshipCV;
import uk.ac.ebi.rhea.domain.Coefficient.Type;
import uk.ac.ebi.rhea.domain.Database;
import uk.ac.ebi.rhea.domain.Direction;
import uk.ac.ebi.rhea.domain.Macromolecule;
import uk.ac.ebi.rhea.domain.Micromolecule;
import uk.ac.ebi.rhea.domain.Qualifier;
import uk.ac.ebi.rhea.domain.Reaction;
import uk.ac.ebi.rhea.domain.ReactionException;
import uk.ac.ebi.rhea.domain.ReactionParticipant;
import uk.ac.ebi.rhea.domain.ReactionParticipant.Location;
import uk.ac.ebi.rhea.domain.Side;
import uk.ac.ebi.rhea.domain.SimpleReaction;
import uk.ac.ebi.rhea.domain.XRef;
import uk.ac.ebi.rhea.mapper.util.CompoundDbProxyException;
import uk.ac.ebi.rhea.mapper.util.CompoundDbProxyFactory;
import uk.ac.ebi.rhea.mapper.util.IChebiHelper;
import uk.ac.ebi.xchars.SpecialCharacters;
import uk.ac.ebi.xchars.domain.EncodingType;

/**
 * Adapter class to convert Rhea's {@link Reaction}s to BioPAX's
 * <code>biochemicalReaction</code> objects and vice-versa.
 * <br>
 * Note that any changes made to the object used in the constructor
 * won't reflect in the other side of the adapter. For example:
 * <pre>
 * Reaction r = new Reaction(...);
 * Model model = ...;
 * BiopaxBiochemicalReaction bbr = new BiopaxBiochemicalReaction(r, model);
 * BiochemicalReaction biopaxSide = bbr.getBiopaxBiochemicalReaction();
 * r.addXref(new Xref(...));
 * biopaxSide = bbr.getBiopaxBiochemicalReaction(); // won't have the new Xref
 * </pre>
 * @author rafalcan
 */
public class BiopaxBiochemicalReaction implements IBiopaxBiochemicalReaction {

    private static final Logger LOGGER = Logger.getLogger(BiopaxBiochemicalReaction.class);

    /**
	 * Table mapping modified groups/residues in a macromolecule to their
	 * unmodified counterparts. Required to populate Rhea model with chemical
	 * moieties in macromolecules in order to check stoichiometry.
	 */
    private static Map<Object, Object> MODIFIED2UNMODIFIED;

    static {
        try {
            Properties props = new Properties();
            props.load(BiopaxBiochemicalReaction.class.getClassLoader().getResourceAsStream("modified2unmodified.properties"));
            MODIFIED2UNMODIFIED = new HashMap<Object, Object>(props);
        } catch (IOException e) {
            LOGGER.error("Unable to load map modified-unmodified", e);
        }
    }

    private Reaction rheaReaction;

    private Conversion biopaxConversion;

    /**
	 * 
	 * @param br
	 * @param direction
     * @param chebiHelper
	 * @throws ReactionException
	 * @throws CompoundDbProxyException
	 * @deprecated use {@link #BiopaxBiochemicalReaction(BiochemicalReaction,
	 * 		Direction)} instead.
	 */
    public BiopaxBiochemicalReaction(BiochemicalReaction br, Direction direction, IChebiHelper chebiHelper) throws ReactionException, CompoundDbProxyException, IOException {
        this(br, direction);
    }

    /**
	 * Creates a Rhea {@link Reaction} from a BioPAX {@link BiochemicalReaction}.
	 * <br>
	 * Some BioPAX properties have been ignored in this implementation:
	 * <ul>
	 *   <li><code>NAME</code></li>
	 *   <li><code>EC-NUMBER</code></li>
	 *   <li><code>DELTA-G</code></li>
	 *   <li><code>DELTA-H</code></li>
	 *   <li><code>DELTA-S</code></li>
	 *   <li><code>KEQ</code></li>
	 * </ul>
	 * <code>SPONTANEOUS</code> is only taken into account as a hint for the
	 * reaction direction, as there is no spontaneity concept in Rhea.
	 * <br>
	 * Biopax's <code>COMMENT</code>s are concatenated in Rhea's {@link Reaction}
	 * single public comment, with newline characters in between.
	 * <br>
	 * BioPAX <code>PublicationXref</code>s are converted to Rhea
	 * {@link Citation}s. Other BioPAX <code>XREF</code>s are converted to
	 * Rhea {@link XRef}s without distinction between <code>UnificationXref</code>
	 * and <code>RelationshipXref</code>.
	 * @param br a BioPAX {@link BiochemicalReaction}.
	 * @param direction the direction of the reaction (possibly taken from the
	 * 		DIRECTION property of a wrapping catalysis in BioPAX). If
	 * 		<code>null</code>, it is inferred from the <code>br</code> object.
     * @throws ReactionException in case of problem while adding participants
     * 		to the Rhea reaction.
	 * @throws CompoundDbProxyException in case of problem retrieving compounds from
	 * 		ChEBI.
     * @throws IOException
	 */
    public BiopaxBiochemicalReaction(BiochemicalReaction br, Direction direction) throws ReactionException, CompoundDbProxyException, IOException {
        this.biopaxConversion = br;
        Database source = null;
        Set<Database> sources = EnumSet.noneOf(Database.class);
        for (Provenance ds : br.getDataSource()) {
            for (String dsName : ds.getName()) {
                try {
                    source = Database.valueOf(dsName.toUpperCase());
                    sources.add(source);
                } catch (Exception e) {
                    LOGGER.error("Unknown dataSource database: " + dsName);
                }
            }
        }
        if (sources.isEmpty()) {
            LOGGER.warn("No valid datasource found, using Rhea instead");
            source = Database.RHEA;
        } else if (sources.size() > 1) {
            StringBuilder sb = new StringBuilder("More than one datasource found: ").append(sources).append("; arbitrarily using ").append(source);
            LOGGER.warn(sb.toString());
        }
        rheaReaction = new Reaction(null, source);
        rheaReaction.setDirection(direction != null ? direction : getReactionDirection(br));
        if (br instanceof Transport) rheaReaction.addQualifier(Qualifier.TR);
        Map<Side, Map<EntityReference, Macromolecule>> macroMap = new HashMap<Side, Map<EntityReference, Macromolecule>>();
        for (Stoichiometry stoich : br.getParticipantStoichiometry()) {
            PhysicalEntity pe = stoich.getPhysicalEntity();
            EntityReference per = ((SimplePhysicalEntity) pe).getEntityReference();
            Side side = br.getLeft().contains(pe) ? Side.LEFT : Side.RIGHT;
            int coef = (int) stoich.getStoichiometricCoefficient();
            BiopaxPhysicalEntity bpep = new BiopaxPhysicalEntity(pe);
            CellularLocationVocabulary clv = pe.getCellularLocation();
            Location location = null;
            ReactionParticipant rp = ReactionParticipant.valueOf(bpep.getRheaCompound(), coef, Type.F, location);
            rheaReaction.addParticipant(rp, side, true);
            if (per != null && (pe instanceof Protein || pe instanceof NucleicAcid)) {
                Map<EntityReference, Macromolecule> sideMap = macroMap.get(side);
                if (sideMap == null) {
                    sideMap = new HashMap<EntityReference, Macromolecule>();
                    macroMap.put(side, sideMap);
                }
                sideMap.put(per, (Macromolecule) bpep.getRheaCompound());
            }
        }
        addUnmodifiedMoieties(macroMap);
        Set<String> ecs = new HashSet<String>();
        for (String ec : br.getECNumber()) {
            ecs.add(ec);
        }
        LOGGER.info(ecs.isEmpty() ? "No EC numbers found" : "EC numbers found: " + ecs);
        StringBuilder commentSb = null;
        for (String comment : br.getComment()) {
            if (commentSb == null) commentSb = new StringBuilder(); else if (commentSb.length() > 0) commentSb.append('\n');
            commentSb.append(comment);
        }
        if (commentSb != null) rheaReaction.setDataComment(commentSb.toString());
        for (Xref x : br.getXref()) {
            if (x instanceof PublicationXref) {
                try {
                    DataSource ds = DataSource.fromName(x.getDb());
                    if (ds == null) {
                        LOGGER.warn("Unknown publication source name: " + x.getDb());
                        continue;
                    }
                    StringBuilder sb = new StringBuilder(x.getId());
                    if (x.getIdVersion() != null) sb.append('.').append(x.getIdVersion());
                    Citation cit = new Citation();
                    cit.setDataSource(ds.toString());
                    cit.setExternalId(sb.toString());
                    rheaReaction.addCitation(cit);
                } catch (Exception e) {
                    LOGGER.error("Unable to add citation: " + e.getMessage());
                    LOGGER.debug(x.toString(), e);
                }
            } else {
                try {
                    Database db = Database.fromName(x.getDb());
                    if (db == null) {
                        LOGGER.warn("Unknown database name: " + x.getDb());
                        continue;
                    }
                    StringBuilder sb = new StringBuilder(x.getId());
                    if (x.getIdVersion() != null) sb.append('.').append(x.getIdVersion());
                    rheaReaction.addXref(new XRef(db, sb.toString()));
                } catch (Exception e) {
                    LOGGER.error("Unable to add xref: " + e.getMessage());
                    LOGGER.debug(x.toString(), e);
                }
            }
        }
    }

    public BiopaxBiochemicalReaction(Reaction reaction, Model model, String rheaRelease) {
        this(reaction, model, rheaRelease, null);
    }

    /**
	 * Constructor from a Rhea {@link Reaction} object.
	 * Some BioPAX properties have been ignored in this implementation:
	 * <ul>
	 *   <li><code>DELTA-G</code></li>
	 *   <li><code>DELTA-H</code></li>
	 *   <li><code>DELTA-S</code></li>
	 *   <li><code>KEQ</code></li>
	 *   <li><code>SPONTANEOUS</code></li>
	 * </ul>
	 * Rhea {@link Citation}s are translated into BioPAX {@link PublicationXref}s.
	 * <br>
	 * Rhea {@link XRef}s are all translated as BioPAX {@link UnificationXref}s.
	 * <br>
	 * The IUBMB flag - i.e. whether the reaction is specified in the NC-IUBMB
	 * enzyme classification - is exported as a BioPAX comment.
	 * <br>
	 * Any children of the Rhea reaction are translated as BioPAX
	 * {@link RelationshipXref}s.
	 * <br>
	 * @param reaction a Rhea Reaction
	 * @param model a BioPAX model to add the new BioPAX
	 * 		{@link BiochemicalReaction} to. In case of error, it remains
	 * 		unchanged (no dirty reactions are added).
	 * @param rheaRelease Rhea release number.
     * @param nsPrefix a namespace prefix to apply to the RDF ID.
	 */
    public BiopaxBiochemicalReaction(Reaction reaction, Model model, String rheaRelease, String nsPrefix) {
        try {
            this.rheaReaction = reaction;
            String reactionBpId = BiopaxLevel3.fixId(nsPrefix, getBiopaxId(reaction), false);
            biopaxConversion = (BiochemicalReaction) model.getByID(reactionBpId);
            if (biopaxConversion != null) {
                return;
            }
            biopaxConversion = reaction.is(Qualifier.TR) ? reaction.isOnlyTransport() ? model.addNew(Transport.class, reactionBpId) : model.addNew(TransportWithBiochemicalReaction.class, reactionBpId) : model.addNew(BiochemicalReaction.class, reactionBpId);
            biopaxConversion.addName(SpecialCharacters.getInstance(null).xml2Display(reaction.getTextualRepresentation(), EncodingType.CHEBI_CODE));
            Set<Provenance> ds = BiopaxLevel3.getBpDataSource(reaction.getSource(), model);
            for (Provenance prov : ds) {
                biopaxConversion.addDataSource(prov);
            }
            if (reaction.getReactionComment() != null) {
                biopaxConversion.addComment(reaction.getReactionComment());
            }
            biopaxConversion.addComment("RHEA:Status=" + reaction.getStatus().getLabel());
            biopaxConversion.addComment("RHEA:IUBMB=" + reaction.isIubmb());
            for (Qualifier q : Qualifier.values()) {
                biopaxConversion.addComment("RHEA:" + q.getLabel() + "=" + reaction.is(q));
            }
            if (reaction.isComplex()) {
                StringBuilder sb = new StringBuilder("Complex, ").append(reaction.getChildren().size()).append(reaction.isStepwise() ? " steps." : " coupled reactions.");
                biopaxConversion.addComment(sb.toString());
            }
            if (!reaction.isAbstract()) {
                for (Side side : Side.values()) {
                    for (ReactionParticipant rp : reaction.getSide(side)) {
                        BiopaxPhysicalEntity bpe = new BiopaxPhysicalEntity(rp.getCompound(), model, nsPrefix, rp.getLocation());
                        switch(side) {
                            case LEFT:
                                biopaxConversion.addLeft(bpe.getBiopaxPhysicalEntity());
                                break;
                            case RIGHT:
                                biopaxConversion.addRight(bpe.getBiopaxPhysicalEntity());
                                break;
                        }
                        if (Type.F.equals(rp.getCoefficient().getType())) {
                            String stoichId = BiopaxLevel3.fixId(nsPrefix, getBiopaxId(rp, reaction.getId(), side), false);
                            Stoichiometry stoich = (Stoichiometry) model.getByID(stoichId);
                            if (stoich == null) {
                                stoich = model.addNew(Stoichiometry.class, stoichId);
                                stoich.setPhysicalEntity(bpe.getBiopaxPhysicalEntity());
                                stoich.setStoichiometricCoefficient(rp.getCoef());
                            }
                            biopaxConversion.addParticipantStoichiometry(stoich);
                        }
                    }
                }
                ConversionDirectionType cd = null;
                switch(reaction.getDirection()) {
                    case LR:
                        cd = ConversionDirectionType.LEFT_TO_RIGHT;
                        break;
                    case BI:
                        cd = ConversionDirectionType.REVERSIBLE;
                        break;
                    case RL:
                        cd = ConversionDirectionType.RIGHT_TO_LEFT;
                        break;
                }
                biopaxConversion.setConversionDirection(cd);
                biopaxConversion.addComment("RHEA:Direction=" + reaction.getDirection().toString());
            }
            if (reaction.getXrefs() != null) {
                for (XRef rx : reaction.getXrefs()) {
                    ReactionsRelationshipCV rel = null;
                    switch(rx.getDatabase()) {
                        case UNIPROT:
                        case INTENZ:
                            rel = ReactionsRelationshipCV.CONTROLLER;
                    }
                    biopaxConversion.addXref(BiopaxLevel3.getBpXref(rx, rel, model, nsPrefix));
                    if (rx.getDatabase().equals(Database.INTENZ)) {
                        ((BiochemicalReaction) biopaxConversion).addECNumber(rx.getName());
                    }
                }
            }
            if (reaction.getId() > Reaction.NO_ID_ASSIGNED) {
                biopaxConversion.addXref(BiopaxLevel3.getBpXref(new XRef(Database.RHEA, rheaRelease, reaction.getId().toString(), null, null, null), null, model, nsPrefix));
            }
            if (reaction.getCitations() != null) {
                for (Citation cit : reaction.getCitations()) {
                    biopaxConversion.addXref(BiopaxLevel3.getBpPublicationXref(cit, model, nsPrefix));
                }
            }
            EnumMap<Direction, Long> siblings = reaction.getSiblings();
            if (siblings != null) {
                for (Direction dir : siblings.keySet()) {
                    Long siblingId = siblings.get(dir);
                    ReactionsRelationshipCV rel = ReactionsRelationshipCV.getFromDirection(dir);
                    RelationshipXref relXref = (RelationshipXref) BiopaxLevel3.getBpXref(new XRef(Database.RHEA, siblingId.toString()), rel, model, nsPrefix);
                    biopaxConversion.addXref(relXref);
                }
            }
            if (reaction.isComplex()) {
                for (SimpleReaction child : reaction.getChildren()) {
                    ReactionsRelationshipCV rel = reaction.isStepwise() ? ReactionsRelationshipCV.STEP : ReactionsRelationshipCV.COUPLED;
                    RelationshipXref relXref = (RelationshipXref) BiopaxLevel3.getBpXref(new XRef(Database.RHEA, child.getReaction().getId().toString()), rel, model, nsPrefix);
                    biopaxConversion.addXref(relXref);
                }
                Long overallId = reaction.getOverallId();
                if (overallId != null) {
                    RelationshipXref relXref = (RelationshipXref) BiopaxLevel3.getBpXref(new XRef(Database.RHEA, overallId.toString()), ReactionsRelationshipCV.OVERALL, model, nsPrefix);
                    biopaxConversion.addXref(relXref);
                }
            } else {
                Set<Long> decompositionIds = reaction.getDecompositionIds();
                if (decompositionIds != null) {
                    for (Long decompositionId : decompositionIds) {
                        RelationshipXref relXref = (RelationshipXref) BiopaxLevel3.getBpXref(new XRef(Database.RHEA, decompositionId.toString()), ReactionsRelationshipCV.DECOMPOSITION, model, nsPrefix);
                        biopaxConversion.addXref(relXref);
                    }
                }
            }
        } catch (RuntimeException e) {
            LOGGER.error("Unable to convert to BioPAX - RHEA:" + reaction.getId().toString());
            model.remove(biopaxConversion);
            throw e;
        }
    }

    /**
	 * Adds any missing unmodified moieties to macromolecules.
	 * @param macroMap a map of macromolecules, keyed by reaction side and
	 * 		entityReference (see BioPAX specification). Macromolecules with
	 * 		the same entityReference in opposite sides should have
	 * 		corresponding modified/unmodified moieties.
	 * @throws CompoundDbProxyException in case of trouble retrieving the compound
	 * 		from ChEBI.
     * @throws IOException
	 * @see #MODIFIED2UNMODIFIED
	 */
    private void addUnmodifiedMoieties(Map<Side, Map<EntityReference, Macromolecule>> macroMap) throws CompoundDbProxyException, IOException {
        Map<EntityReference, Macromolecule> leftMacros = macroMap.get(Side.LEFT);
        if (leftMacros != null) {
            for (EntityReference er : leftMacros.keySet()) {
                Macromolecule leftMacro = leftMacros.get(er);
                Macromolecule rightMacro = macroMap.get(Side.RIGHT).get(er);
                if (rightMacro != null) {
                    Collection<Micromolecule> leftMacroMoieties = leftMacro.getMoieties();
                    Collection<Micromolecule> rightMacroMoieties = rightMacro.getMoieties();
                    Macromolecule unmodified = null, modified = null;
                    if (leftMacroMoieties.isEmpty() && rightMacroMoieties.isEmpty()) {
                        LOGGER.warn("No moieties defined for " + er);
                    } else if (leftMacroMoieties.isEmpty()) {
                        unmodified = leftMacro;
                        modified = rightMacro;
                    } else if (rightMacroMoieties.isEmpty()) {
                        unmodified = rightMacro;
                        modified = leftMacro;
                    } else {
                        LOGGER.warn("Multiple moieties defined for " + er);
                    }
                    if (unmodified != null) {
                        String modMoietyAcc = modified.getMoieties().iterator().next().getAccession();
                        Micromolecule unmodMoiety = getUnmodifiedMoiety(modMoietyAcc);
                        if (unmodMoiety != null) {
                            unmodified.addMoiety(unmodMoiety);
                        } else {
                            LOGGER.warn("No unmodified counterpart found for " + modMoietyAcc);
                        }
                    }
                }
            }
        }
    }

    /**
	 * Retrieves an unmodified small molecule. Every modified moiety has an
	 * unmodified counterpart.
	 * @param modMoietyAcc The accession of the modified counterpart.
	 * @return a small molecule (the unmodified counterpart of the passed
	 * 		accession), or <code>null</code> if the correspondence is not
	 * 		defined.
	 * @throws CompoundDbProxyException in case of trouble retrieving the compound
	 * 		from ChEBI.
     * @throws IOException
	 * @see #MODIFIED2UNMODIFIED
	 */
    private Micromolecule getUnmodifiedMoiety(String modMoietyAcc) throws CompoundDbProxyException, IOException {
        Micromolecule unmodifiedMoiety = null;
        Object o = MODIFIED2UNMODIFIED.get(modMoietyAcc);
        if (o != null) {
            if (o instanceof Micromolecule) {
                unmodifiedMoiety = (Micromolecule) o;
            } else {
                unmodifiedMoiety = (Micromolecule) CompoundDbProxyFactory.getInstance().getProxy(Database.forAccession(modMoietyAcc)).getCompoundById(modMoietyAcc);
                MODIFIED2UNMODIFIED.put(modMoietyAcc, unmodifiedMoiety);
            }
        }
        return unmodifiedMoiety;
    }

    /**
	 * Gets the directionality of a BioPAX {@link BiochemicalReaction},
	 * independently of any wrapping catalysis, according to BioPAX
	 * specification and conventions.
	 * @param br a BioPAX {@link BiochemicalReaction}
	 * @return a direction
	 */
    protected static Direction getReactionDirection(BiochemicalReaction br) {
        Direction dir = Direction.UN;
        if (br.getConversionDirection() != null) {
            switch(br.getConversionDirection()) {
                case LEFT_TO_RIGHT:
                    dir = Direction.LR;
                    break;
                case RIGHT_TO_LEFT:
                    dir = Direction.RL;
                    break;
                case REVERSIBLE:
                    dir = Direction.BI;
                    break;
            }
        }
        return dir;
    }

    /**
	 * Computes a unique RDF ID for a reaction.
	 * @param reaction a Rhea {@link Reaction}
	 * @return a unique RDF ID without namespace prefix.
	 */
    protected static String getBiopaxId(Reaction reaction) {
        String id = null;
        if (reaction.getId() > Reaction.NO_ID_ASSIGNED) {
            id = reaction.getId().toString();
        } else {
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                md.update(reaction.getTextualRepresentation().getBytes());
                byte[] digestBytes = md.digest();
                StringBuilder digesterSb = new StringBuilder(32);
                for (int i = 0; i < digestBytes.length; i++) {
                    int intValue = digestBytes[i] & 0xFF;
                    if (intValue < 0x10) digesterSb.append('0');
                    digesterSb.append(Integer.toHexString(intValue));
                }
                id = digesterSb.toString();
            } catch (NoSuchAlgorithmException e) {
            }
        }
        return id;
    }

    /**
	 * Computes a unique RDF ID for a Rhea {@link ReactionParticipant}.
	 * @param rp a Rhea {@link ReactionParticipant}.
	 * @param reactionId the ID of the reaction the participant belongs to.
	 * @param side the side of the reaction where the participant appears.
	 * @return a unique RDF ID.
	 */
    private static String getBiopaxId(ReactionParticipant rp, Long reactionId, Side side) {
        return new StringBuilder(reactionId.toString()).append('/').append(side.toString()).append('/').append(rp.getCompound().getId()).toString();
    }

    /**
	 * Retrieves the Rhea side of this adaptor.
	 * @return a Rhea {@link Reaction}
	 */
    public Reaction getRheaReaction() {
        return rheaReaction;
    }

    /**
	 * Retrieves the BioPAX side of this adaptor.
	 * @return a BioPAX {@link BiochemicalReaction}
	 */
    public BiochemicalReaction getBiopaxBiochemicalReaction() {
        return (BiochemicalReaction) biopaxConversion;
    }

    public Object getBiopaxConversion() {
        return biopaxConversion;
    }
}
