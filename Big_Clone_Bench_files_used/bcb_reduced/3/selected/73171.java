package org.amiwall.policy;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.amiwall.plugin.AbstractPlugin;
import org.apache.log4j.Logger;
import org.jdom.Element;

/**
 *  This is the abstract class for holding repeating periods of time, Eg 9am->10am every day.
 *
 *@author    Nick Cunnah
 */
public abstract class Periodic extends AbstractPlugin {

    private static Logger log = Logger.getLogger("org.amiwall.policy.Periodic");

    /**
     *  Description of the Field
     */
    protected Time start = null;

    /**
     *  Description of the Field
     */
    protected Time end = null;

    /**
     *  Description of the Method
     *
     *@param  digester  Description of the Parameter
     *@param  root      Description of the Parameter
     */
    public void digest(Element root) {
        Element se = root.getChild("start");
        if (se != null) {
            start = new Time();
            start.digest(se);
        } else {
            start = null;
        }
        Element ee = root.getChild("end");
        if (ee != null) {
            end = new Time();
            end.digest(ee);
        } else {
            end = null;
        }
    }

    /**
     *  Sets the start attribute of the Period object
     *
     *@param  start  The new start value
     */
    public void setStart(Time start) {
        this.start = start;
    }

    /**
     *  Gets the startTime attribute of the Period object
     *
     *@return    The startTime value
     */
    public long getStart() {
        return start.getTime();
    }

    /**
     *  Sets the endTime attribute of the Period object
     *
     *@param  end  The new end value
     */
    public void setEnd(Time end) {
        this.end = end;
    }

    /**
     *  Gets the endTime attribute of the Period object
     *
     *@return    The endTime value
     */
    public long getEnd() {
        return end.getTime();
    }

    /**
     *  Description of the Method
     *
     *@return    Description of the Return Value
     */
    public boolean currentlyWithinPeriod() {
        return isWithinPeriod(System.currentTimeMillis());
    }

    /**
     *  Gets the withinPeriod attribute of the Period object
     *
     *@param  time  Description of the Parameter
     *@return       The withinPeriod value
     */
    public boolean isWithinPeriod(long time) {
        long normalisedTime = time - getStartOfPeriod(time);
        return getStart() < normalisedTime && normalisedTime < getEnd();
    }

    /**
     *  Gets the startOfPeriod attribute of the Period object
     *
     *@return    The startOfPeriod value
     */
    public long getStartOfCurrentPeriod() {
        return getStartOfPeriod(System.currentTimeMillis());
    }

    /**
     *  Gets the startOfPeriod attribute of the Period object
     *
     *@param  timeWithinPeriod  Description of the Parameter
     *@return                   The startOfPeriod value
     */
    public abstract long getStartOfPeriod(long timeWithinPeriod);

    /**
     *  Gets the duration attribute of the Period object
     *
     *@return    The duration value
     */
    public abstract long getDuration();

    /**
     *  Description of the Method
     *
     *@exception  Exception  Description of the Exception
     */
    public void activate() throws Exception {
        if (start.getTime() > end.getTime()) {
            throw new IllegalStateException(start + " is not before " + end);
        }
    }

    /**
     *  Gets the instancesWithin attribute of the Period object
     *
     *@param  start  Description of the Parameter
     *@param  end    Description of the Parameter
     *@return        The instancesWithin value
     */
    public List getPeriods(long start, long end) {
        if (end < start) {
            throw new IllegalArgumentException("start must be before end");
        }
        List dates = new ArrayList();
        long realStart = getStartOfPeriod(start) + getStart();
        long realEnd = getStartOfPeriod(end) + getEnd();
        long len = getEnd() - getStart();
        for (long s = realStart; s < realEnd; s += getDuration()) {
            long[] date = new long[2];
            if (s < start) {
                date[0] = start;
            } else {
                date[0] = s;
            }
            if (s + len > end) {
                date[1] = end;
            } else {
                date[1] = s + len;
            }
            if (date[0] < date[1]) {
                dates.add(new Period(date[0], date[1]));
            }
        }
        return dates;
    }

    /**
     *  Description of the Method
     *
     *@param  layout  Description of the Parameter
     *@return         Description of the Return Value
     */
    public String toString(String layout) {
        return ((start == null) ? "NULL" : start.toString(layout)) + "->" + ((end == null) ? "NULL" : end.toString(layout));
    }
}
