package org.amiwall.policy;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.TreeSet;
import java.util.SortedSet;
import org.amiwall.instrument.Instrument;
import org.amiwall.plugin.Install;
import org.amiwall.user.AbstractIdName;
import org.apache.log4j.Logger;
import org.jdom.Element;
import org.amiwall.instrument.WebMailRequests;
import org.amiwall.instrument.TotalRequestBandwidth;
import org.amiwall.instrument.TotalResponseBandwidth;
import java.util.ArrayList;
import org.jdom.DataConversionException;

/**
 *  Description of the Class
 *
 *@author    Nick Cunnah
 */
public abstract class AbstractRule extends AbstractIdName implements Rule, Install {

    private static Logger log = Logger.getLogger("org.amiwall.policy.AbstractRule");

    /**
     *  Description of the Field
     */
    protected List periodics = null;

    Instrument instrument = null;

    double level = 0;

    /**
     *  Description of the Method
     *
     *@exception  Exception  Description of the Exception
     */
    public void activate() throws Exception {
        if (instrument == null) {
            throw new NullPointerException("instrument is NULL, this needs to be configured");
        }
        instrument.activate();
        for (Iterator i = periodics.iterator(); i.hasNext(); ) {
            Periodic periodic = (Periodic) i.next();
            periodic.activate();
        }
    }

    /**
     *  Description of the Method
     */
    public void deactivate() {
        instrument.deactivate();
        for (Iterator i = periodics.iterator(); i.hasNext(); ) {
            Periodic periodic = (Periodic) i.next();
            periodic.deactivate();
        }
    }

    /**
     *  Description of the Method
     *
     *@exception  Exception  Description of the Exception
     */
    public void install() throws Exception {
        if (instrument instanceof Install) {
            ((Install) instrument).install();
        }
    }

    /**
     *  Description of the Method
     *
     *@exception  Exception  Description of the Exception
     */
    public void uninstall() throws Exception {
        if (instrument instanceof Install) {
            ((Install) instrument).uninstall();
        }
    }

    /**
     *  Sets the periodics attribute of the Rule object
     *
     *@param  periodics  The new periodics value
     */
    public void setPeriodics(List periodics) {
        this.periodics = periodics;
    }

    /**
     *  Gets the periods attribute of the Rule object
     *
     *@return    The periods value
     */
    public List getPeriodics() {
        return periodics;
    }

    /**
     *  Sets the instrument attribute of the Rule object
     *
     *@param  instrument  The new instrument value
     */
    public void setInstrument(Instrument instrument) {
        this.instrument = instrument;
    }

    /**
     *  Gets the instrument attribute of the Rule object
     *
     *@return    The instrument value
     */
    public Instrument getInstrument() {
        return instrument;
    }

    /**
     *  Sets the level attribute of the Rule object
     *
     *@param  level  The new level value
     */
    public void setLevel(double level) {
        this.level = level;
    }

    /**
     *  Gets the level attribute of the Rule object
     *
     *@return    The level value
     */
    public double getLevel() {
        return level;
    }

    /**
     *  Gets the good attribute of the Rule object
     *
     *@param  score  Description of the Parameter
     *@return        The good value
     */
    public boolean isThisScoreGood(double score) {
        return score <= 1.0;
    }

    /**
     *  Gets the thisGood attribute of the AbstractRule object
     *
     *@param  levels  Description of the Parameter
     *@return         The thisGood value
     */
    public boolean isThisGood(Map levels) {
        long total = 0;
        for (Iterator i = levels.values().iterator(); i.hasNext(); ) {
            Long level = (Long) i.next();
            total += level.longValue();
        }
        return isThisScoreGood(score(total));
    }

    /**
     *  Description of the Method
     *
     *@param  level  Description of the Parameter
     *@return        Description of the Return Value
     */
    public double score(double level) {
        return level / this.level;
    }

    /**
     *  Description of the Method
     *
     *@param  levels  Description of the Parameter
     *@return         Description of the Return Value
     */
    public double score(Map levels) {
        double total = 0;
        for (Iterator i = levels.values().iterator(); i.hasNext(); ) {
            Long level = (Long) i.next();
            total += level.longValue();
        }
        return score(total);
    }

    /**
     *  Description of the Method
     *
     *@param  metrics  Description of the Parameter
     *@return          Description of the Return Value
     */
    public double scoreMetrics(Map metrics) {
        double total = 0;
        for (Iterator i = metrics.keySet().iterator(); i.hasNext(); ) {
            DateHolder dateHolder = (DateHolder) i.next();
            Map metric = (Map) metrics.get(dateHolder);
            double score = score(metric);
            total += score;
        }
        return total;
    }

    /**
     *  Description of the Method
     *
     *@param  digester  Description of the Parameter
     *@param  root      Description of the Parameter     
     */
    public void digest(Element root) {
        try {
            setId(root.getAttribute("id").getLongValue());
        } catch (DataConversionException e) {
            log.error("Failed to set id", e);
        }
        try {
            setLevel(Double.parseDouble(root.getChildTextTrim("level")));
        } catch (NullPointerException e) {
            setLevel(0.0);
        }
        digestInstrument(root);
        digestPeriodics(root.getChild("periodics"));
        if (log.isDebugEnabled()) log.debug("done");
    }

    void digestInstrument(Element root) {
        for (Iterator i = root.getChildren().iterator(); i.hasNext(); ) {
            Element child = (Element) i.next();
            Instrument instrument = getInstrument(child.getName());
            if (instrument != null) {
                instrument.digest(child);
                setInstrument(instrument);
                break;
            }
        }
    }

    protected Instrument getInstrument(String name) {
        if (name.equals("TotalRequestBandwidth")) {
            return new TotalRequestBandwidth();
        } else if (name.equals("TotalResponseBandwidth")) {
            return new TotalResponseBandwidth();
        } else if (name.equals("WebMailRequests")) {
            return new WebMailRequests();
        }
        return null;
    }

    void digestPeriodics(Element root) {
        periodics = digestPeriodicsFactory(root);
    }

    public static List digestPeriodicsFactory(Element root) {
        ArrayList periodics = new ArrayList();
        for (Iterator i = root.getChildren().iterator(); i.hasNext(); ) {
            Element child = (Element) i.next();
            Periodic periodic = null;
            if (child.getName().equals("DailyPeriodic")) {
                periodic = new DailyPeriodic();
            } else if (child.getName().equals("WeeklyPeriodic")) {
                periodic = new WeeklyPeriodic();
            }
            if (periodic != null) {
                periodic.digest(child);
                periodics.add(periodic);
            }
        }
        return periodics;
    }

    /**
     *  Gets the scoreString attribute of the AbstractRule object
     *
     *@param  score  Description of the Parameter
     *@return        The scoreString value
     */
    public String getScoreString(double score) {
        return Integer.toString(((int) (score * 100))) + "%";
    }

    /**
     *  Description of the Method
     *
     *@param  score          Description of the Parameter
     *@param  feedbackScore  Description of the Parameter
     *@return                Description of the Return Value
     */
    public boolean provideFeedback(double score, double feedbackScore) {
        return score >= feedbackScore;
    }

    /**
     *  Gets the description attribute of the Max object
     *
     *@return    The description value
     */
    public String getDescription() {
        return getLevelString() + " " + getInstrument().getDescription();
    }

    /**
     *  Gets the periodics attribute of the AbstractRule object
     *
     *@param  startTime  Description of the Parameter
     *@param  endTime    Description of the Parameter
     *@return            The periodics value
     */
    public SortedSet getPeriods(long startTime, long endTime) {
        TreeSet set = new TreeSet(new Comparator() {

            public int compare(Object o1, Object o2) {
                if (o1 == o2) return 0;
                Period p1 = (Period) o1;
                Period p2 = (Period) o2;
                long s1 = p1.getStart();
                long s2 = p2.getStart();
                if (s1 != s2) {
                    return (int) (s1 - s2);
                } else {
                    return (int) (p1.getEnd() - p2.getEnd());
                }
            }

            public boolean equals(Object o) {
                return this == o;
            }
        });
        for (Iterator k = periodics.iterator(); k.hasNext(); ) {
            Periodic periodic = (Periodic) k.next();
            set.addAll(periodic.getPeriods(startTime, endTime));
        }
        return set;
    }
}
