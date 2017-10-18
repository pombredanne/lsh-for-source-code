package dpdesktop;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.Comparator;
import java.util.Date;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.View;

/**
 * Provides some helpful static variables,  functions or classes.
 * 
 * @author Heiner Reinhardt
 */
public class DPDesktopUtility {

    /**
     * A static final varaible representing a comparator for columns, 
     * that have date as strings cells. While this will render complete 
     * dates, TIME_COMPARE is able to render time only. By the way the parsing 
     * format is specified by DPDesktopDataLocal.DATE_FORMAT. 
     * @see #TIME_COMPARE
     */
    public static final Comparator DATE_COMPARE = new Comparator<String>() {

        public int compare(String o1, String o2) {
            try {
                Date d1 = DPDesktopDataLocal.DATE_FORMAT.parse(o1);
                Date d2 = DPDesktopDataLocal.DATE_FORMAT.parse(o2);
                return d1.compareTo(d2);
            } catch (ParseException ex) {
                return 0;
            }
        }
    };

    /**
     * A static final varaible representing a comparator for columns, 
     * that have time as strings cells. While this will only be able to 
     * render time strings, DATE_COMPARE is able to parse complete dates. 
     * By the way the parsing format is specified by 
     * DPDesktopDataLocal.TIME_FORMAT 
     * @see #DATE_COMPARE
     */
    public static final Comparator TIME_COMPARE = new Comparator<String>() {

        public int compare(String o1, String o2) {
            try {
                Date d1 = DPDesktopDataLocal.TIME_FORMAT.parse(o1);
                Date d2 = DPDesktopDataLocal.TIME_FORMAT.parse(o2);
                return d1.compareTo(d2);
            } catch (ParseException ex) {
                return 0;
            }
        }
    };

    /**
     * Class providing a nice rendering for table cells. 
     */
    public static class MyTableCellRenderer extends JTextArea implements TableCellRenderer {

        /**
         * Setting up the cell renderer. 
         */
        public MyTableCellRenderer() {
            super();
            setWrapStyleWord(true);
            setLineWrap(true);
            setAlignmentY(SwingConstants.TOP);
        }

        /**
         * Returns the rendered object as a JTextArea. 
         * @param table Specifies the table, which needs the rendered component.
         * @param value Specifies the value, to be rendered. 
         * @param isSelected Specifies whether the object is selected. 
         * @param hasFocus Specifies whether the object has focus. 
         * @param row Specifies row of the table 
         * @param column Specifies column of the table
         * @return The full rendered JTextArea as Object. 
         */
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (isSelected) {
                super.setForeground(table.getSelectionForeground());
                super.setBackground(table.getSelectionBackground());
            } else {
                super.setForeground(table.getForeground());
                super.setBackground(table.getBackground());
            }
            setFont(table.getFont());
            setEnabled(table.isEnabled());
            setValue(table, row, column, value);
            return this;
        }

        /**
         * Function to provide that the row height fits with the 
         * highest cell and sets the value. 
         * @param table The table 
         * @param row Row to be set
         * @param column Column to be set
         * @param value Value to be set 
         */
        protected void setValue(JTable table, int row, int column, Object value) {
            if (value != null) {
                String text = "";
                text = value.toString();
                setText(text);
                View view = getUI().getRootView(this);
                view.setSize((float) table.getColumnModel().getColumn(column).getWidth() - 3, -1);
                float y = view.getPreferredSpan(View.Y_AXIS);
                int h = (int) Math.ceil(y + 3);
                if (table.getRowHeight(row) < h) {
                    table.setRowHeight(row, h);
                }
            } else {
                setText("");
            }
        }
    }

    /**
     * A simple function, checking if a file is available. If not the 
     * function will create a empty file specified by a filename.
     * @param file Filename 
     */
    public static void fileCheck(String file) {
        File d = new File(DPDesktopDataLocal.DIR);
        if (!d.exists()) {
            d.mkdir();
        }
        File f = new File(file);
        try {
            if (!f.exists()) {
                f.createNewFile();
            }
        } catch (IOException e) {
            DPDesktopError.critical(DPDesktopError.ERROR_CREATE_FILE + file, e);
        }
    }

    /**
     * A simple function providing highlighting for a substring in a string. 
     * For example "Hallo" is the string, and "all" is the substring. Something 
     * like "H<format>all</format>o" will be returned. The exact format is 
     * specified inside the function. 
     * @param string String with substring inside.
     * @param substring The string which needs to be highlighted. 
     * @return String with highlighted/ searched string. 
     */
    public static String highlight(String string, String substring) {
        String tmpData = string.toLowerCase();
        String tmpSearch = substring.toLowerCase();
        int index = tmpData.indexOf(tmpSearch);
        if (index >= 0) {
            string = string.substring(0, index) + "<span style=\"color: blue; font-weight: bold;\" >" + string.substring(index, index + substring.length()) + "</span>" + string.substring(index + substring.length());
        }
        return string;
    }

    /**
     * A simple function returning the md5 string to the given string. It has 
     * the same behavior like md5(..) known from PHP. 
     * @param string String to be hashed. 
     * @return The md5-hashed string. 
     * @throws NoSuchAlgorithmException Will be thrown if hash algorithm MD5 is 
     * not available in the runtime environment.
     */
    public static String md5(String string) throws NoSuchAlgorithmException {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.reset();
        md5.update(string.getBytes());
        byte[] result = md5.digest();
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < result.length; i++) {
            hexString.append(Integer.toHexString((result[i] & 0xFF) | 0x100).toLowerCase().substring(1, 3));
        }
        return hexString.toString();
    }
}
