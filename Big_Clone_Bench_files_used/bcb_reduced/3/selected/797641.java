package net.bull.javamelody;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Données statistiques d'une requête identifiée, hors paramètres dynamiques comme un identifiant,
 * et sur la période considérée selon le pilotage du collector par l'intermédiaire counter.
 *
 * Les méthodes d'une instance de cette classe ne sont pas thread-safe.
 * L'état d'une instance doit être accédé ou modifié par l'intermédiaire d'une instance de Counter,
 * qui gérera les accès concurrents sur les instances de cette classe.
 * @author Emeric Vernat
 */
class CounterRequest implements Cloneable, Serializable {

    private static final long serialVersionUID = -4301825473892026959L;

    private final String name;

    private final String id;

    private long hits;

    private long durationsSum;

    private long durationsSquareSum;

    private long maximum;

    private long cpuTimeSum;

    private long systemErrors;

    private long responseSizesSum;

    private long childHits;

    private long childDurationsSum;

    private String stackTrace;

    @SuppressWarnings("all")
    private Map<String, Long> childRequestsExecutionsByRequestId;

    /**
	 * Interface du contexte d'une requête en cours.
	 */
    interface ICounterRequestContext {

        /**
		 * @return Nombre de hits du compteur fils pour ce contexte.
		 */
        int getChildHits();

        /**
		 * @return Temps total du compteur fils pour ce contexte.
		 */
        int getChildDurationsSum();

        /**
		 * @return Nombres d'exécutions par requêtes filles
		 */
        Map<String, Long> getChildRequestsExecutionsByRequestId();
    }

    /**
	 * Constructeur.
	 * @param name Nom de la requête
	 * @param counterName Nom du counter
	 */
    CounterRequest(String name, String counterName) {
        super();
        assert name != null;
        assert counterName != null;
        this.name = name;
        this.id = buildId(name, counterName);
    }

    /**
	 * @return Nom de la requête
	 */
    String getName() {
        return name;
    }

    /**
	 * @return Identifiant de la requête, construit à partir de son nom et du nom du counter
	 */
    String getId() {
        return id;
    }

    /**
	 * @return Nombre d'exécution de cette requête
	 */
    long getHits() {
        return hits;
    }

    /**
	 * @return Somme des temps d'exécution de cette requête
	 */
    long getDurationsSum() {
        return durationsSum;
    }

    /**
	 * @return Moyenne des temps d'exécution
	 */
    int getMean() {
        if (hits > 0) {
            return (int) (durationsSum / hits);
        }
        return -1;
    }

    /**
	 * @return écart type (ou sigma, dit "standard deviation" en anglais)
	 */
    int getStandardDeviation() {
        if (hits > 0) {
            return (int) Math.sqrt((durationsSquareSum - (double) durationsSum * durationsSum / hits) / (hits - 1));
        }
        return -1;
    }

    /**
	 * @return Maximum des temps d'exécution de cette requête
	 */
    long getMaximum() {
        return maximum;
    }

    /**
	 * @return Somme temps cpu pour l'exécution de cette requête
	 */
    long getCpuTimeSum() {
        return cpuTimeSum;
    }

    /**
	 * @return Moyenne des temps cpu pour l'exécution de cette requête
	 */
    int getCpuTimeMean() {
        if (hits > 0) {
            return (int) (cpuTimeSum / hits);
        }
        return -1;
    }

    /**
	 * @return Pourcentage des erreurs systèmes dans l'exécution de cette requête
	 */
    float getSystemErrorPercentage() {
        if (hits > 0) {
            return Math.min(100f * systemErrors / hits, 100f);
        }
        return 0;
    }

    /**
	 * @return Moyenne des tailles des réponses (http en particulier)
	 */
    int getResponseSizeMean() {
        if (hits > 0) {
            return (int) (responseSizesSum / hits);
        }
        return -1;
    }

    /**
	 * @return Booléen selon qu'il existe des requêtes filles (sql en particulier)
	 */
    boolean hasChildHits() {
        return childHits > 0;
    }

    /**
	 * @return Nombre moyen d'exécutions des requêtes filles (sql en particulier)
	 */
    int getChildHitsMean() {
        if (hits > 0) {
            return (int) (childHits / hits);
        }
        return -1;
    }

    /**
	 * @return Moyenne des temps d'exécutions des requêtes filles (sql en particulier)
	 */
    int getChildDurationsMean() {
        if (hits > 0) {
            return (int) (childDurationsSum / hits);
        }
        return -1;
    }

    /**
	 * @return Map des nombres d'exécutions par requêtes filles
	 */
    Map<String, Long> getChildRequestsExecutionsByRequestId() {
        if (childRequestsExecutionsByRequestId == null) {
            return Collections.emptyMap();
        }
        synchronized (this) {
            return new LinkedHashMap<String, Long>(childRequestsExecutionsByRequestId);
        }
    }

    boolean containsChildRequest(String requestId) {
        if (childRequestsExecutionsByRequestId == null) {
            return false;
        }
        synchronized (this) {
            return childRequestsExecutionsByRequestId.containsKey(requestId);
        }
    }

    /**
	 * @return Dernière stack trace
	 */
    String getStackTrace() {
        return stackTrace;
    }

    void addHit(long duration, long cpuTime, boolean systemError, String systemErrorStackTrace, int responseSize) {
        hits++;
        durationsSum += duration;
        durationsSquareSum += duration * duration;
        if (duration > maximum) {
            maximum = duration;
        }
        cpuTimeSum += cpuTime;
        if (systemError) {
            systemErrors++;
        }
        if (systemErrorStackTrace != null) {
            stackTrace = systemErrorStackTrace;
        }
        responseSizesSum += responseSize;
    }

    void addChildHits(ICounterRequestContext context) {
        childHits += context.getChildHits();
        childDurationsSum += context.getChildDurationsSum();
    }

    void addChildRequests(Map<String, Long> childRequests) {
        if (childRequests != null && !childRequests.isEmpty()) {
            if (childRequestsExecutionsByRequestId == null) {
                childRequestsExecutionsByRequestId = new LinkedHashMap<String, Long>(childRequests);
            } else {
                for (final Map.Entry<String, Long> entry : childRequests.entrySet()) {
                    final String requestId = entry.getKey();
                    Long nbExecutions = childRequestsExecutionsByRequestId.get(requestId);
                    if (nbExecutions == null) {
                        nbExecutions = entry.getValue();
                    } else {
                        nbExecutions += entry.getValue();
                    }
                    childRequestsExecutionsByRequestId.put(requestId, nbExecutions);
                }
            }
        }
    }

    void addHits(CounterRequest request) {
        assert request != null;
        if (request.hits != 0) {
            hits += request.hits;
            durationsSum += request.durationsSum;
            durationsSquareSum += request.durationsSquareSum;
            if (request.maximum > maximum) {
                maximum = request.maximum;
            }
            cpuTimeSum += request.cpuTimeSum;
            systemErrors += request.systemErrors;
            responseSizesSum += request.responseSizesSum;
            childHits += request.childHits;
            childDurationsSum += request.childDurationsSum;
            if (request.stackTrace != null) {
                stackTrace = request.stackTrace;
            }
            addChildRequests(request.childRequestsExecutionsByRequestId);
        }
    }

    void removeHits(CounterRequest request) {
        assert request != null;
        if (request.hits != 0) {
            hits -= request.hits;
            durationsSum -= request.durationsSum;
            durationsSquareSum -= request.durationsSquareSum;
            if (request.maximum >= maximum) {
                if (hits > 0) {
                    maximum = durationsSum / hits;
                } else {
                    maximum = -1;
                }
            }
            cpuTimeSum -= request.cpuTimeSum;
            systemErrors -= request.systemErrors;
            responseSizesSum -= request.responseSizesSum;
            childHits -= request.childHits;
            childDurationsSum -= request.childDurationsSum;
            removeChildHits(request);
        }
    }

    private void removeChildHits(CounterRequest request) {
        if (request.childRequestsExecutionsByRequestId != null && childRequestsExecutionsByRequestId != null) {
            for (final Map.Entry<String, Long> entry : request.childRequestsExecutionsByRequestId.entrySet()) {
                final String requestId = entry.getKey();
                Long nbExecutions = childRequestsExecutionsByRequestId.get(requestId);
                if (nbExecutions != null) {
                    nbExecutions = Math.max(nbExecutions - entry.getValue(), 0);
                    if (nbExecutions == 0) {
                        childRequestsExecutionsByRequestId.remove(requestId);
                        if (childRequestsExecutionsByRequestId.isEmpty()) {
                            childRequestsExecutionsByRequestId = null;
                            break;
                        }
                    } else {
                        childRequestsExecutionsByRequestId.put(requestId, nbExecutions);
                    }
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public CounterRequest clone() {
        try {
            final CounterRequest clone = (CounterRequest) super.clone();
            if (childRequestsExecutionsByRequestId != null) {
                clone.childRequestsExecutionsByRequestId = getChildRequestsExecutionsByRequestId();
            }
            return clone;
        } catch (final CloneNotSupportedException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String buildId(String name, String counterName) {
        final MessageDigest messageDigest = getMessageDigestInstance();
        messageDigest.update(name.getBytes());
        final byte[] digest = messageDigest.digest();
        final StringBuilder sb = new StringBuilder(digest.length * 2);
        sb.append(counterName);
        int j;
        for (final byte element : digest) {
            j = element < 0 ? 256 + element : element;
            if (j < 16) {
                sb.append('0');
            }
            sb.append(Integer.toHexString(j));
        }
        return sb.toString();
    }

    private static MessageDigest getMessageDigestInstance() {
        try {
            return MessageDigest.getInstance("SHA-1");
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return getClass().getSimpleName() + "[name=" + getName() + ", hits=" + getHits() + ", id=" + getId() + ']';
    }
}
