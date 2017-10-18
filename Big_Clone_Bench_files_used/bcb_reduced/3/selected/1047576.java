package adv.language.beans;

import adv.language.CompileParserException;
import adv.language.Constants;
import adv.live.ConstantsLive;
import adv.runtime.wraper.ObjWraper;
import ognlscript.Line;
import java.io.Serializable;
import java.util.*;

/**
 * Alberto Vilches Ratón
 * User: avilches
 * Date: 01-oct-2006
 * Time: 12:49:32
 * To change this template use File | Settings | File Templates.
 */
public class Action implements Serializable, ConstantsLive, Constants {

    Set<Method> methods = new LinkedHashSet<Method>();

    SelectorGroup obj1SelectorGroup = SelectorGroup.EMPTY;

    SelectorGroup obj2SelectorGroup = SelectorGroup.EMPTY;

    String name;

    String superName;

    String defLine;

    Grammar grammar;

    Object novalidate = null;

    int prioridad = 0;

    private Obj obj;

    public Action(Grammar grammar, String defLine) throws CompileParserException {
        this.grammar = grammar;
        this.defLine = defLine;
        defLine = defLine.substring(ACTION.length() + 1);
        int count = 0;
        for (StringTokenizer stringTokenizer = new StringTokenizer(defLine, "("); stringTokenizer.hasMoreTokens(); ) {
            String toke = stringTokenizer.nextToken().trim();
            if (count == 0) {
                int pos = toke.indexOf(":");
                if (pos > 0) {
                    name = toke.substring(0, pos);
                    superName = toke.substring(pos + 1);
                } else {
                    name = toke;
                }
            } else if (count == 1) {
                obj1SelectorGroup = parseSelectorGroup(grammar, defLine, toke);
            } else if (count == 2) {
                obj2SelectorGroup = parseSelectorGroup(grammar, defLine, toke);
            } else {
                throw new CompileParserException("Especificacion incorrecta, demasiados parametros en accion [" + defLine + "]");
            }
            count++;
        }
    }

    public void digest(String moduleName, List<Line> lines, List<CompileParserException> errors) throws CompileParserException {
        lines = createObject(moduleName, lines, errors);
        addMethods(lines);
    }

    public Obj getObj() {
        return obj;
    }

    private List createObject(String moduleName, List<Line> lines, List<CompileParserException> errors) throws CompileParserException {
        List<Line> funcs = Collections.EMPTY_LIST;
        boolean extracting = false;
        if (lines != null) {
            for (ListIterator l = lines.listIterator(); l.hasNext(); ) {
                Line myLine = (Line) l.next();
                String line = myLine.getText();
                extracting = extracting || line.startsWith(Constants.VAR + " ") || line.endsWith(":") || line.startsWith(DEF_FUNC + " ");
                if (extracting) {
                    if (funcs == Collections.EMPTY_LIST) funcs = new ArrayList<Line>();
                    funcs.add(myLine);
                    l.remove();
                }
            }
        }
        obj = new Obj(name, superName, false, null, null, grammar.getModulo()).digest(moduleName, funcs, errors);
        obj.setAction(this);
        grammar.getModulo().addObject(obj);
        return lines;
    }

    public void validate() throws CompileParserException {
        Object v = obj.getAttribute(Constants.PRIORITY, true);
        if (v != null) {
            try {
                prioridad = Integer.valueOf(v + "");
            } catch (NumberFormatException e) {
                throw new CompileParserException("Error en action [" + name + "]: Valor de prioridad no es numerico [" + v + "]");
            }
        }
        novalidate = obj.getAttribute(Constants.NOVALIDATE, true);
    }

    private SelectorGroup parseSelectorGroup(Grammar grammar, String line, String toke) throws CompileParserException {
        if (!toke.endsWith(")")) {
            throw new CompileParserException("Parametro incorrecto debe empezar con [(] y acabar con [)] en accion [" + line + "]");
        }
        SelectorGroup sg = new SelectorGroup(grammar, toke, line);
        return sg;
    }

    public Set<Method> getMethods() {
        return methods;
    }

    public String getName() {
        return name;
    }

    public SelectorGroup getObj1SelectorGroup() {
        return obj1SelectorGroup;
    }

    public SelectorGroup getObj2SelectorGroup() {
        return obj2SelectorGroup;
    }

    public void addMethods(List<Line> methodList) throws CompileParserException {
        int count = 0;
        if (methodList != null) {
            for (Line myLine : methodList) {
                String methodLine = myLine.getText();
                String id = name + ++count;
                Method method = new Method(this, methodLine, id);
                methods.add(method);
            }
        }
    }

    public String dumpFull() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(":\n");
        for (Iterator m = methods.iterator(); m.hasNext(); ) {
            Method met = (Method) m.next();
            sb.append(met.id).append(": ").append(met.modulo).append(" -> ");
            sb.append(met.getDefLine()).append("\n");
            sb.append(met.getTodasFrasesPosibles(true));
        }
        return sb.append("\n").toString();
    }

    public String dump() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(":\n");
        for (Iterator m = methods.iterator(); m.hasNext(); ) {
            Method met = (Method) m.next();
            sb.append("\t").append(met.modulo).append("\n");
        }
        return sb.append("\n").toString();
    }

    public String toString() {
        return getName();
    }

    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Action action = (Action) o;
        return name.equals(action.name);
    }

    public Grammar getGrammar() {
        return grammar;
    }

    public int hashCode() {
        return name != null ? name.hashCode() : super.hashCode();
    }

    public int getPrioridad() {
        return prioridad;
    }

    public String getDefLine() {
        return defLine;
    }

    public Set<String> getTodasFrasesPosibles(boolean conpronombres) {
        Set<String> frases = new TreeSet<String>();
        for (Method m : methods) {
            frases.addAll(m.getTodasFrasesPosibles(conpronombres));
        }
        return frases;
    }

    Collection<Action> similares = Collections.EMPTY_SET;

    Collection<Action> casiSimilares = Collections.EMPTY_SET;

    public void addSimilar(Action act) {
        if (similares == Collections.EMPTY_SET) {
            similares = new LinkedHashSet<Action>();
        }
        similares.add(act);
    }

    public void addCasiSimilar(Action act) {
        if (casiSimilares == Collections.EMPTY_SET) {
            casiSimilares = new LinkedHashSet<Action>();
        }
        casiSimilares.add(act);
    }

    public Collection<Action> getSimilares() {
        return similares;
    }

    public Collection<Action> getCasiSimilares() {
        return casiSimilares;
    }

    public boolean canValidate(ObjWraper info1) {
        if (novalidate != null) {
            if (novalidate instanceof ObjWraper && info1.equals(novalidate) || novalidate instanceof Collection && ((Collection) novalidate).contains(info1)) {
                return false;
            }
        }
        return true;
    }
}
