package ognlscript;

import ognlscript.block.*;
import java.util.*;
import java.util.regex.*;
import java.io.*;
import adv.tools.TextTools;

/**
 * Alberto Vilches Ratón
 * User: avilches
 * Date: 07-oct-2006
 * Time: 10:49:11
 * To change this template use File | Settings | File Templates.
 */
public class FileParser {

    private static final Pattern TRIMESPACES = Pattern.compile("\\s{1,}");

    public static String trimSpaces(String line) {
        return line == null ? "" : TRIMESPACES.matcher(line).replaceAll(" ").trim();
    }

    /**
     * Lee un fichero, lo convierte en un List.
     * Elimina blancos/tabs repetidos por 1 espacio sencillo
     * Elimina blancos laterales
     * Cambia "  " por " "
     *
     * @param reader
     * @return
     * @throws java.io.IOException
     */
    public static List<Line> toList(Reader reader) throws IOException {
        if (reader == null) {
            return new ArrayList<Line>();
        }
        List<Line> lines = new ArrayList<Line>();
        BufferedReader br = null;
        if (!(reader instanceof BufferedReader)) {
            br = new BufferedReader(reader);
        } else {
            br = (BufferedReader) reader;
        }
        String line = br.readLine();
        int n = 0;
        while (line != null) {
            n++;
            line = line.trim();
            if (line.length() > 0) {
                lines.add(new Line(n, line));
            }
            line = br.readLine();
        }
        br.close();
        return lines;
    }

    public static void prepare(List<Line> list, String joinLineChar, String startSingleCommentChar, String startMultiCommentChar, String endMultiCommentChar) throws OgnlscriptCompileException {
        prepare(list, joinLineChar, startSingleCommentChar, startMultiCommentChar, endMultiCommentChar, null);
    }

    public static void prepare(List<Line> list, String joinLineChar, String startSingleCommentChar, String startMultiCommentChar, String endMultiCommentChar, StringDigester dig) {
        if (list == null) {
            return;
        }
        joinLines(list, joinLineChar, dig);
        removeComments(list, startSingleCommentChar, startMultiCommentChar, endMultiCommentChar);
    }

    private static void removeComments(List<Line> list, String startSingleCommentChar, String startMultiCommentChar, String endMultiCommentChar) {
        boolean inComment = false;
        Line myLine = null;
        for (ListIterator<Line> i = list.listIterator(); i.hasNext(); ) {
            myLine = i.next();
            String line = myLine.getText();
            if (inComment && line.contains(endMultiCommentChar)) {
                inComment = false;
                int pos = line.indexOf(endMultiCommentChar);
                line = line.substring(pos + endMultiCommentChar.length());
                myLine.setText(line);
            }
            if (inComment) {
                i.remove();
            } else {
                line = extractMultiLineCommentInOneLine(line, startMultiCommentChar, endMultiCommentChar);
                int pos = TextTools.quotedIndexOf(line, startSingleCommentChar);
                if (pos > -1) {
                    line = line.substring(0, pos);
                }
                pos = TextTools.quotedIndexOf(line, startMultiCommentChar);
                if (pos > -1) {
                    line = line.substring(0, pos);
                    inComment = true;
                }
                if (line.trim().length() == 0) {
                    i.remove();
                } else {
                    myLine.setText(line);
                }
            }
        }
        if (inComment) {
            throw new RuntimeException("Comentario multiple " + startMultiCommentChar + " no cerrado con " + endMultiCommentChar + " en linea " + myLine.getNumber());
        }
    }

    private static String extractMultiLineCommentInOneLine(String line, String startMultiCommentChar, String endMultiCommentChar) {
        int init = TextTools.quotedIndexOf(line, startMultiCommentChar);
        if (init > -1) {
            int end = line.indexOf(endMultiCommentChar, init);
            if (end > -1) {
                line = extractMultiLineCommentInOneLine(line.substring(0, init) + line.substring(end + endMultiCommentChar.length()), startMultiCommentChar, endMultiCommentChar);
            }
        }
        return line;
    }

    private static void joinLines(List<Line> list, String joinLineChar, StringDigester dig) {
        StringBuilder previous = null;
        int skipped = 0;
        Line myLine = null;
        for (ListIterator<Line> i = list.listIterator(); i.hasNext(); ) {
            myLine = i.next();
            String line = myLine.getText();
            if (dig != null) {
                line = dig.digest(line);
            }
            if (previous == null) {
                if (line.endsWith(joinLineChar)) {
                    line = line.substring(0, line.length() - 1).trim() + " ";
                    previous = new StringBuilder(line);
                    i.remove();
                    skipped++;
                } else {
                    if (line.length() == 0) {
                        i.remove();
                    } else {
                        if (dig != null) {
                            myLine.setText(line);
                        }
                    }
                }
            } else {
                if (joinLineChar != null && line.endsWith(joinLineChar)) {
                    line = line.substring(0, line.length() - 1).trim() + " ";
                    previous.append(line);
                    i.remove();
                    skipped++;
                } else {
                    line = previous.append(line).toString();
                    previous = null;
                    myLine.setText(line);
                    skipped = 0;
                }
            }
        }
        if (previous != null) {
            String line = myLine.getText();
            line = line.substring(0, line.length() - 1).trim();
            if (line.length() > 0) {
                list.add(myLine);
            }
        }
    }

    public static void main(String[] args) throws IOException, OgnlscriptCompileException {
        Reader r = new FileReader("/Users/avilches/Work/Proy/Local/Aventura/src/kenshira/system/vampiro/test.java");
        List<Line> l = toList(r);
        prepare(l, "\\", "//", "/*", "*/", null);
        r.close();
        for (Line line : l) {
            System.out.println(line);
        }
    }
}
