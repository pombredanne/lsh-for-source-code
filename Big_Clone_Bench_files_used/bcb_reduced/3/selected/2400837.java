package org.jchains.internal;

import java.awt.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.Enumeration;
import java.util.Properties;
import javax.swing.Spring;
import javax.swing.SpringLayout;
import org.jchains.CORBA.PermissionTransferPackage.EnvironmentEntry;
import org.jchains.CORBA.PermissionTransferPackage.Stacktrace;
import org.jchains.CORBA.PermissionTransferPackage.StacktraceSeqHolder;

public class Util {

    public static String tidyFilename(String theName) {
        theName = theName.replace(':', '_');
        theName = theName.replace('/', '_');
        theName = theName.replace('\\', '_');
        return theName;
    }

    public static final EnvironmentEntry[] emptyret = new EnvironmentEntry[] {};

    public static EnvironmentEntry[] propsToEnvironmentEntry(Properties p) {
        EnvironmentEntry[] ret = emptyret;
        if (p.size() == 0) return ret;
        ret = new EnvironmentEntry[p.size()];
        int i = 0;
        for (Enumeration<Object> e = p.keys(); e.hasMoreElements(); ) {
            EnvironmentEntry ee = new EnvironmentEntry();
            ee.theKey = (String) e.nextElement();
            ee.theValue = p.getProperty(ee.theKey);
            ret[i++] = ee;
        }
        return ret;
    }

    public static void waitMillis(int wait) {
        try {
            Thread.sleep(wait);
        } catch (Exception e) {
            System.out.println("Get another JDK, this is b0rken");
            System.exit(-1);
        }
    }

    public static StacktraceSeqHolder Ste2String(StackTraceElement[] ste) {
        Stacktrace[] arr = new Stacktrace[ste.length];
        for (int i = 0; i < ste.length; i++) {
            arr[i] = new Stacktrace();
            arr[i].classname = Util.NullHelper(ste[i].getClassName());
            arr[i].filename = Util.NullHelper(ste[i].getFileName());
            arr[i].methodname = Util.NullHelper(ste[i].getMethodName());
            arr[i].isnative = ste[i].isNativeMethod();
            arr[i].linenumber = (short) ste[i].getLineNumber();
        }
        return new StacktraceSeqHolder(arr);
    }

    public static String ste2sum(StackTraceElement[] ste, int position) {
        String str = "";
        int i = position;
        str += Util.NullHelper(ste[i].getClassName());
        str += Util.NullHelper(ste[i].getFileName());
        str += Util.NullHelper(ste[i].getMethodName());
        str += ste[i].isNativeMethod();
        str += (short) ste[i].getLineNumber();
        return str;
    }

    public static String ste2sum(StackTraceElement[] ste) {
        String str = "";
        for (int i = 0; i < ste.length; i++) {
            str += Util.NullHelper(ste[i].getClassName());
            str += Util.NullHelper(ste[i].getFileName());
            str += Util.NullHelper(ste[i].getMethodName());
            str += ste[i].isNativeMethod();
            str += (short) ste[i].getLineNumber();
        }
        return str;
    }

    public StacktraceSeqHolder Ste2String(StackTraceElement[] ste, int position) {
        Stacktrace[] arr = new Stacktrace[1];
        arr[0] = new Stacktrace();
        arr[0].classname = Util.NullHelper(ste[position].getClassName());
        arr[0].filename = Util.NullHelper(ste[position].getFileName());
        arr[0].methodname = Util.NullHelper(ste[position].getMethodName());
        arr[0].isnative = ste[position].isNativeMethod();
        arr[0].linenumber = (short) ste[position].getLineNumber();
        return new StacktraceSeqHolder(arr);
    }

    public static String shortDecode(String name) {
        String ename = "";
        try {
            ename = URLDecoder.decode(name, "UTF-8");
        } catch (Exception e) {
            System.err.println("Don't use this JDK, it's b0rken");
            System.exit(-1);
        }
        return ename;
    }

    public static String shortcutEncode(String z) {
        String ename = "";
        try {
            ename = URLEncoder.encode(z, "UTF-8");
        } catch (Exception e) {
            System.out.println("Don't use this JDK, it's b0rken");
            System.exit(-1);
        }
        return ename;
    }

    public static String NullHelper(String str) {
        if (str == null) {
            return "";
        }
        return str;
    }

    public static synchronized String getMD5forString(String tmp) {
        MessageDigest digest = md;
        if (tmp == null || tmp == "") tmp = "<empty>";
        digest.reset();
        digest.update(tmp.getBytes());
        byte[] hash = digest.digest();
        String dig = new String(hash);
        return dig;
    }

    public static MessageDigest md = null;

    static {
        try {
            Util.md = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
        }
    }

    /**
	     * A debugging utility that prints to stdout the component's
	     * minimum, preferred, and maximum sizes.
	     */
    public static void printSizes(Component c) {
        System.out.println("minimumSize = " + c.getMinimumSize());
        System.out.println("preferredSize = " + c.getPreferredSize());
        System.out.println("maximumSize = " + c.getMaximumSize());
    }

    /**
	     * Aligns the first <code>rows</code> * <code>cols</code>
	     * components of <code>parent</code> in
	     * a grid. Each component is as big as the maximum
	     * preferred width and height of the components.
	     * The parent is made just big enough to fit them all.
	     *
	     * @param rows number of rows
	     * @param cols number of columns
	     * @param initialX x location to start the grid at
	     * @param initialY y location to start the grid at
	     * @param xPad x padding between cells
	     * @param yPad y padding between cells
	     */
    public static void makeGrid(Container parent, int rows, int cols, int initialX, int initialY, int xPad, int yPad) {
        SpringLayout layout;
        try {
            layout = (SpringLayout) parent.getLayout();
        } catch (ClassCastException exc) {
            System.err.println("The first argument to makeGrid must use SpringLayout.");
            return;
        }
        Spring xPadSpring = Spring.constant(xPad);
        Spring yPadSpring = Spring.constant(yPad);
        Spring initialXSpring = Spring.constant(initialX);
        Spring initialYSpring = Spring.constant(initialY);
        int max = rows * cols;
        Spring maxWidthSpring = layout.getConstraints(parent.getComponent(0)).getWidth();
        Spring maxHeightSpring = layout.getConstraints(parent.getComponent(0)).getWidth();
        for (int i = 1; i < max; i++) {
            SpringLayout.Constraints cons = layout.getConstraints(parent.getComponent(i));
            maxWidthSpring = Spring.max(maxWidthSpring, cons.getWidth());
            maxHeightSpring = Spring.max(maxHeightSpring, cons.getHeight());
        }
        for (int i = 0; i < max; i++) {
            SpringLayout.Constraints cons = layout.getConstraints(parent.getComponent(i));
            cons.setWidth(maxWidthSpring);
            cons.setHeight(maxHeightSpring);
        }
        SpringLayout.Constraints lastCons = null;
        SpringLayout.Constraints lastRowCons = null;
        for (int i = 0; i < max; i++) {
            SpringLayout.Constraints cons = layout.getConstraints(parent.getComponent(i));
            if (i % cols == 0) {
                lastRowCons = lastCons;
                cons.setX(initialXSpring);
            } else {
                cons.setX(Spring.sum(lastCons.getConstraint(SpringLayout.EAST), xPadSpring));
            }
            if (i / cols == 0) {
                cons.setY(initialYSpring);
            } else {
                cons.setY(Spring.sum(lastRowCons.getConstraint(SpringLayout.SOUTH), yPadSpring));
            }
            lastCons = cons;
        }
        SpringLayout.Constraints pCons = layout.getConstraints(parent);
        pCons.setConstraint(SpringLayout.SOUTH, Spring.sum(Spring.constant(yPad), lastCons.getConstraint(SpringLayout.SOUTH)));
        pCons.setConstraint(SpringLayout.EAST, Spring.sum(Spring.constant(xPad), lastCons.getConstraint(SpringLayout.EAST)));
    }

    private static SpringLayout.Constraints getConstraintsForCell(int row, int col, Container parent, int cols) {
        SpringLayout layout = (SpringLayout) parent.getLayout();
        Component c = parent.getComponent(row * cols + col);
        return layout.getConstraints(c);
    }

    /**
	     * Aligns the first <code>rows</code> * <code>cols</code>
	     * components of <code>parent</code> in
	     * a grid. Each component in a column is as wide as the maximum
	     * preferred width of the components in that column;
	     * height is similarly determined for each row.
	     * The parent is made just big enough to fit them all.
	     *
	     * @param rows number of rows
	     * @param cols number of columns
	     * @param initialX x location to start the grid at
	     * @param initialY y location to start the grid at
	     * @param xPad x padding between cells
	     * @param yPad y padding between cells
	     */
    public static void makeCompactGrid(Container parent, int rows, int cols, int initialX, int initialY, int xPad, int yPad) {
        SpringLayout layout;
        try {
            layout = (SpringLayout) parent.getLayout();
        } catch (ClassCastException exc) {
            System.err.println("The first argument to makeCompactGrid must use SpringLayout.");
            return;
        }
        Spring x = Spring.constant(initialX);
        for (int c = 0; c < cols; c++) {
            Spring width = Spring.constant(0);
            for (int r = 0; r < rows; r++) {
                width = Spring.max(width, getConstraintsForCell(r, c, parent, cols).getWidth());
            }
            for (int r = 0; r < rows; r++) {
                SpringLayout.Constraints constraints = getConstraintsForCell(r, c, parent, cols);
                constraints.setX(x);
                constraints.setWidth(width);
            }
            x = Spring.sum(x, Spring.sum(width, Spring.constant(xPad)));
        }
        Spring y = Spring.constant(initialY);
        for (int r = 0; r < rows; r++) {
            Spring height = Spring.constant(0);
            for (int c = 0; c < cols; c++) {
                height = Spring.max(height, getConstraintsForCell(r, c, parent, cols).getHeight());
            }
            for (int c = 0; c < cols; c++) {
                SpringLayout.Constraints constraints = getConstraintsForCell(r, c, parent, cols);
                constraints.setY(y);
                constraints.setHeight(height);
            }
            y = Spring.sum(y, Spring.sum(height, Spring.constant(yPad)));
        }
        SpringLayout.Constraints pCons = layout.getConstraints(parent);
        pCons.setConstraint(SpringLayout.SOUTH, y);
        pCons.setConstraint(SpringLayout.EAST, x);
    }
}
