package uk.ac.ebi.rhea.biopax.level2;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Logger;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level2.biochemicalReaction;
import org.biopax.paxtools.model.level2.conversion;
import org.biopax.paxtools.model.level2.dataSource;
import org.biopax.paxtools.model.level2.physicalEntityParticipant;
import org.biopax.paxtools.model.level2.publicationXref;
import org.biopax.paxtools.model.level2.relationshipXref;
import org.biopax.paxtools.model.level2.transport;
import org.biopax.paxtools.model.level2.transportWithBiochemicalReaction;
import org.biopax.paxtools.model.level2.unificationXref;
import org.biopax.paxtools.model.level2.xref;
import uk.ac.ebi.biobabel.citations.DataSource;
import uk.ac.ebi.cdb.webservice.Citation;
import uk.ac.ebi.rhea.domain.Database;
import uk.ac.ebi.rhea.domain.Direction;
import uk.ac.ebi.rhea.domain.Qualifier;
import uk.ac.ebi.rhea.domain.Reaction;
import uk.ac.ebi.rhea.domain.ReactionException;
import uk.ac.ebi.rhea.domain.ReactionParticipant;
import uk.ac.ebi.rhea.domain.Side;
import uk.ac.ebi.rhea.domain.SimpleReaction;
import uk.ac.ebi.rhea.domain.XRef;
import uk.ac.ebi.rhea.mapper.util.IChebiHelper;
import uk.ac.ebi.xchars.SpecialCharacters;
import uk.ac.ebi.xchars.domain.EncodingType;

/**
 * Apapter class to convert Rhea's {@link Reaction}s to BioPAX's
 * <code>biochemicalReaction</code> objects and viceversa.
 * <br>
 * Note that any changes made to the object used in the constructor
 * won't reflect in the other side of the adapter. For example:
 * <pre>
 * Reaction r = new Reaction(...);
 * Model model = ...;
 * BiopaxBiochemicalReaction bbr = new BiopaxBiochemicalReaction(r, model);
 * biochemicalReaction biopaxSide = bbr.getBiopaxBiochemicalReaction();
 * r.addXref(new Xref(...));
 * biopaxSide = bbr.getBiopaxBiochemicalReaction(); // won't have the new xref
 * </pre>
 * @author rafalcan
 */
public class BiopaxBiochemicalReaction {

    private static final Logger LOGGER = Logger.getLogger(BiopaxBiochemicalReaction.class);

    private Reaction rheaReaction;

    private conversion biopaxConversion;

    private static final Map<String, Qualifier> qualifierMap = new HashMap();

    private static final Map<String, Direction> directionMap = new HashMap();

    static {
        qualifierMap.put("RHEA:Transport", Qualifier.TR);
        qualifierMap.put("RHEA:Class of reactions", Qualifier.CR);
        qualifierMap.put("RHEA:Polymerization", Qualifier.PO);
        qualifierMap.put("RHEA:Chemically balanced", Qualifier.CB);
        qualifierMap.put("RHEA:Mapped", Qualifier.MA);
        qualifierMap.put("RHEA:Formuled", Qualifier.FO);
        directionMap.put("bidirectional", Direction.BI);
        directionMap.put("left to right", Direction.LR);
        directionMap.put("right to left", Direction.RL);
        directionMap.put("undefined", Direction.UN);
    }

    /**
	 * 
	 * @param br
	 * @param direction
	 * @throws ReactionException
	 * @deprecated use {@link #BiopaxBiochemicalReaction(biochemicalReaction,
	 * 		Direction, IChebiHelper) instead.
	 */
    public BiopaxBiochemicalReaction(biochemicalReaction br, Direction direction) throws ReactionException {
        this(br, direction, null);
    }

    /**
	 * Creates a Rhea {@link Reaction} from a BioPAX {@link biochemicalReaction}.
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
	 * BioPAX <code>publicationXref</code>s are converted to Rhea
	 * {@link Citation}s. Other BioPAX <code>XREF</code>s are converted to
	 * Rhea {@link XRef}s without distinction between <code>unificationXref</code>
	 * and <code>relationshipXref</code>.
	 * @param br a BioPAX {@link biochemicalReaction}.
	 * @param direction the direction of the reaction (possibly taken from the
	 * 		DIRECTION property of a wrapping catalysis in BioPAX). If
	 * 		<code>null</code>, it is inferred from the <code>br</code> object.
	 * @param chebiHelper a ChEBI helper to define accurately compounds
	 * 		contained in the BioPAX model.
	 */
    public BiopaxBiochemicalReaction(biochemicalReaction br, Direction direction, IChebiHelper chebiHelper) throws ReactionException {
        this.biopaxConversion = br;
        Database source = null;
        Set<Database> sources = new HashSet<Database>();
        for (dataSource ds : br.getDATA_SOURCE()) {
            for (String dsName : ds.getNAME()) {
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
        rheaReaction.setId(Long.parseLong(br.getRDFId().split("#")[1]));
        Direction dir = (direction == null) ? getReactionDirection(br) : direction;
        rheaReaction.setDirection(dir);
        if (br instanceof transport) rheaReaction.addQualifier(Qualifier.TR);
        for (physicalEntityParticipant pep : br.getLEFT()) {
            BiopaxPhysicalEntityParticipant bpep = new BiopaxPhysicalEntityParticipant(pep, chebiHelper);
            rheaReaction.addParticipant(bpep.getRheaParticipant(), Side.LEFT, true);
        }
        for (physicalEntityParticipant pep : br.getRIGHT()) {
            BiopaxPhysicalEntityParticipant bpep = new BiopaxPhysicalEntityParticipant(pep, chebiHelper);
            rheaReaction.addParticipant(bpep.getRheaParticipant(), Side.RIGHT, true);
        }
        Set<String> ecs = new HashSet<String>();
        for (String ec : br.getEC_NUMBER()) {
            ecs.add(ec);
            rheaReaction.addEnzymeClassification(ec);
        }
        LOGGER.info(ecs.isEmpty() ? "No EC numbers found" : "EC numbers found: " + ecs);
        StringBuilder commentSb = null;
        for (String comment : br.getCOMMENT()) {
            if (commentSb == null) commentSb = new StringBuilder(); else if (commentSb.length() > 0) commentSb.append('\n');
            commentSb.append(comment);
            String[] commentInfo = comment.split("=");
            if (commentInfo.length == 2) {
                if (commentInfo[1].equals("true")) {
                    if (qualifierMap.containsKey(commentInfo[0])) {
                        rheaReaction.addQualifier(qualifierMap.get(commentInfo[0]));
                    }
                } else if (commentInfo[0].equals("RHEA:Direction")) {
                    rheaReaction.setDirection(directionMap.get(commentInfo[1]));
                }
            } else {
                LOGGER.warn("Unhandled comment: " + comment);
            }
        }
        if (commentSb != null) rheaReaction.setDataComment(commentSb.toString());
        for (xref x : br.getXREF()) {
            if (x instanceof publicationXref) {
                try {
                    DataSource ds = DataSource.fromName(x.getDB());
                    if (ds == null) {
                        LOGGER.warn("Unknown publication source name: " + x.getDB());
                        continue;
                    }
                    StringBuilder sb = new StringBuilder(x.getID());
                    if (x.getID_VERSION() != null) sb.append('.').append(x.getID_VERSION());
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
                    Database db = Database.fromName(x.getDB());
                    if (db == null) {
                        LOGGER.warn("Unknown database name: " + x.getDB());
                        continue;
                    }
                    StringBuilder sb = new StringBuilder(x.getID());
                    if (x.getID_VERSION() != null) sb.append('.').append(x.getID_VERSION());
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

    public BiopaxBiochemicalReaction(Reaction reaction, Model model, String rheaRelease, String nsPrefix) {
        if (!Direction.UN.equals(reaction.getDirection())) {
        }
        try {
            this.rheaReaction = reaction;
            String reactionBpId = Biopax.fixId(nsPrefix, getBiopaxId(reaction), false);
            if (model.getByID(reactionBpId) != null) {
                biopaxConversion = (biochemicalReaction) model.getByID(reactionBpId);
                return;
            }
            biopaxConversion = reaction.is(Qualifier.TR) ? reaction.isOnlyTransport() ? model.addNew(transport.class, reactionBpId) : model.addNew(transportWithBiochemicalReaction.class, reactionBpId) : model.addNew(biochemicalReaction.class, reactionBpId);
            biopaxConversion.setNAME(SpecialCharacters.getInstance(null).xml2Display(reaction.getTextualRepresentation(), EncodingType.CHEBI_CODE));
            dataSource ds = Biopax.getBpDataSource(reaction.getSource(), model);
            biopaxConversion.addDATA_SOURCE(ds);
            if (reaction.getReactionComment() != null) {
                biopaxConversion.addCOMMENT(reaction.getReactionComment());
            }
            biopaxConversion.addCOMMENT("RHEA:Status=" + reaction.getStatus().getLabel());
            biopaxConversion.addCOMMENT("RHEA:IUBMB=" + reaction.isIubmb());
            for (Qualifier q : Qualifier.values()) {
                biopaxConversion.addCOMMENT("RHEA:" + q.getLabel() + "=" + reaction.is(q));
            }
            if (reaction.isComplex()) {
                StringBuilder sb = new StringBuilder("Complex, ").append(reaction.getChildren().size()).append(reaction.isStepwise() ? " steps." : " coupled reactions.");
                biopaxConversion.addCOMMENT(sb.toString());
            }
            if (!reaction.isAbstract()) {
                for (ReactionParticipant rp : reaction.getSide(Side.LEFT)) {
                    biopaxConversion.addLEFT(new BiopaxPhysicalEntityParticipant(rp, model, reaction.getId(), Side.LEFT, nsPrefix).getBiopaxParticipant());
                }
                for (ReactionParticipant rp : reaction.getSide(Side.RIGHT)) {
                    biopaxConversion.addRIGHT(new BiopaxPhysicalEntityParticipant(rp, model, reaction.getId(), Side.RIGHT, nsPrefix).getBiopaxParticipant());
                }
                biopaxConversion.addCOMMENT("RHEA:Direction=" + reaction.getDirection().toString());
            }
            if (reaction.getXrefs() != null) {
                for (XRef rx : reaction.getXrefs()) {
                    String rel = null;
                    switch(rx.getDatabase()) {
                        case UNIPROT:
                        case INTENZ:
                            rel = "controller";
                    }
                    biopaxConversion.addXREF(Biopax.getBpXref(rx, rel, model, nsPrefix));
                    if (rx.getDatabase().equals(Database.INTENZ)) {
                        ((biochemicalReaction) biopaxConversion).addEC_NUMBER(rx.getName());
                    }
                }
            }
            if (reaction.getId() > Reaction.NO_ID_ASSIGNED) {
                biopaxConversion.addXREF(Biopax.getBpXref(new XRef(Database.RHEA, rheaRelease, reaction.getId().toString(), null, null, null), null, model, nsPrefix));
            }
            if (reaction.getCitations() != null) {
                for (Citation cit : reaction.getCitations()) {
                    biopaxConversion.addXREF(Biopax.getBpPublicationXref(cit, model, nsPrefix));
                }
            }
            EnumMap<Direction, Long> siblings = reaction.getSiblings();
            if (siblings != null) {
                for (Direction dir : siblings.keySet()) {
                    Long siblingId = siblings.get(dir);
                    relationshipXref relXref = (relationshipXref) Biopax.getBpXref(new XRef(Database.RHEA, rheaRelease, siblingId.toString(), null, null, null), dir.name().toLowerCase(), model, nsPrefix);
                    relXref.setRELATIONSHIP_TYPE(dir.toString());
                    biopaxConversion.addXREF(relXref);
                }
            }
            if (reaction.isComplex()) {
                for (SimpleReaction child : reaction.getChildren()) {
                    relationshipXref relXref = (relationshipXref) Biopax.getBpXref(new XRef(Database.RHEA, child.getReaction().getId().toString()), reaction.isStepwise() ? "step" : "coupled", model, nsPrefix);
                    relXref.setRELATIONSHIP_TYPE(reaction.isStepwise() ? "step" : "coupled");
                    biopaxConversion.addXREF(relXref);
                }
                Long overallId = reaction.getOverallId();
                if (overallId != null) {
                    relationshipXref relXref = (relationshipXref) Biopax.getBpXref(new XRef(Database.RHEA, overallId.toString()), "overall", model, nsPrefix);
                    relXref.setRELATIONSHIP_TYPE("overall");
                    biopaxConversion.addXREF(relXref);
                }
            } else {
                Set<Long> decompositionIds = reaction.getDecompositionIds();
                if (decompositionIds != null) {
                    for (Long decompositionId : decompositionIds) {
                        relationshipXref relXref = (relationshipXref) Biopax.getBpXref(new XRef(Database.RHEA, decompositionId.toString()), "decomposition", model, nsPrefix);
                        relXref.setRELATIONSHIP_TYPE("decomposition");
                        biopaxConversion.addXREF(relXref);
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
	 * Gets the directionality of a BioPAX {@link biochemicalReaction},
	 * independently of any wrapping catalysis, according to BioPAX
	 * specification and conventions.
	 * If there is a SPONTANEOUS property, this decides. Otherwise, it
	 * defaults to LR.
	 * @param br a BioPAX {@link biochemicalReaction}
	 * @return a direction
	 */
    protected static Direction getReactionDirection(biochemicalReaction br) {
        Direction dir = Direction.LR;
        if (br.getSPONTANEOUS() != null) {
            switch(br.getSPONTANEOUS()) {
                case L_R:
                case NOT_SPONTANEOUS:
                    break;
                case R_L:
                    dir = Direction.RL;
            }
        }
        return dir;
    }

    /**
	 * Computes a - hopefuly unique - RDF ID for a reaction.
	 * @param reaction a Rhea {@link Reaction}
	 * @return a unique RDF ID without namespace prefix.
	 */
    public static String getBiopaxId(Reaction reaction) {
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
	 * Retrieves the Rhea side of this adaptor.
	 * @return a Rhea {@link Reaction}
	 */
    public Reaction getRheaReaction() {
        return rheaReaction;
    }

    /**
	 * Retrieves the BioPAX side of this adaptor.
	 * @return a BioPAX {@link biochemicalReaction}
	 * @deprecated use {@link #getBiopaxConversion()} instead, now we are also
	 * 		dealing with pure transports.
	 */
    public biochemicalReaction getBiopaxBiochemicalReaction() {
        return (biochemicalReaction) biopaxConversion;
    }

    /**
	 * Retrieves the BioPAX side of this adaptor.
	 * @return a BioPAX {@link biochemicalReaction}
	 */
    public conversion getBiopaxConversion() {
        return biopaxConversion;
    }
}
