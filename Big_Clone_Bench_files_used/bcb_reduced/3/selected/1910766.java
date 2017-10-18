package adv.language.beans;

import adv.language.*;
import adv.live.ConstantsLive;
import adv.runtime.wraper.ModuloWraper;
import adv.runtime.wraper.ObjWraper;
import adv.tools.TextTools;
import adv.web.AdvOgnlscriptContext;
import ognl.*;
import ognlscript.*;
import ognlscript.block.*;
import java.io.Serializable;
import java.util.*;
import java.lang.reflect.*;
import java.lang.reflect.Method;

public class Modulo implements Constants, Serializable {

    public static final boolean PERMITE_FUNCIONES_GLOBALES = false;

    private String parentUser = null;

    private String parentPro = null;

    private Map<String, Obj> objectsById = Collections.EMPTY_MAP;

    private Map<String, FunctionBlock> functions = Collections.EMPTY_MAP;

    private Map<String, AmbiguousGroup> objectNameIndex = Collections.EMPTY_MAP;

    private Map<String, Fullname> fullnameById = Collections.EMPTY_MAP;

    private Map<String, Object> consts = Collections.EMPTY_MAP;

    private Map<String, String> define = Collections.EMPTY_MAP;

    private Set<String> relaciones = Collections.EMPTY_SET;

    private Grammar grammar;

    private int maxObjectNameWordCount = 0;

    private String userid;

    private String projectid;

    private Date compilationDate;

    private long compilationTime;

    private String hash;

    ModuloWraper moduloWraperRoot;

    ModuloWraper moduloWraper;

    List<String> warnings = Collections.EMPTY_LIST;

    List<CompileParserException> errors = new ArrayList<CompileParserException>();

    public void clear() {
        parentUser = null;
        parentPro = null;
        objectsById.clear();
        functions.clear();
        objectNameIndex.clear();
        fullnameById.clear();
        consts.clear();
        grammar.clear();
        maxObjectNameWordCount = 0;
        hash = null;
    }

    public ModuloWraper getModuloWraperRoot() {
        if (moduloWraperRoot == null) {
            moduloWraperRoot = new ModuloWraper(true);
        }
        return moduloWraperRoot;
    }

    public ModuloWraper getModuloWraper() {
        if (moduloWraper == null) {
            moduloWraper = new ModuloWraper(false);
        }
        return moduloWraper;
    }

    public Date getCompilationDate() {
        return compilationDate;
    }

    public long getCompilationTime() {
        return compilationTime;
    }

    public String getId() {
        return userid + "/" + projectid;
    }

    public String getProjectid() {
        return projectid;
    }

    public String getUserid() {
        return userid;
    }

    public Modulo() {
    }

    public Modulo(String userid, String projectid) {
        this.userid = userid;
        this.projectid = projectid;
        grammar = new Grammar(this);
    }

    public Grammar getGrammar() {
        return grammar;
    }

    public void startDigest() {
        compilationTime = System.currentTimeMillis();
        errors.clear();
    }

    public void endDigest() throws CompileParserException {
        try {
            validate();
        } catch (CompileParserException e1) {
            errors.add(new CompileParserException("Error en validacion: " + e1.getMessage(), e1));
        }
        if (!errors.isEmpty()) {
            clear();
        }
        compilationTime = System.currentTimeMillis() - compilationTime;
        compilationDate = new Date();
    }

    public List<CompileParserException> getErrors() {
        return errors;
    }

    private static class DigestingBlock {

        Modulo owner;

        String name;

        List<Line> lines = new ArrayList<Line>();

        int type;

        boolean inline = false;

        Obj obj;

        Action action;

        private DigestingBlock(Modulo owner) {
            this.owner = owner;
        }

        void addLine(Line line) {
            lines.add(line);
        }

        boolean isFunction() {
            return obj == null && action == null;
        }

        boolean isAction() {
            return obj == null && action != null;
        }

        boolean isObject() {
            return obj != null && !obj.isClass();
        }

        boolean isClass() {
            return obj != null && obj.isClass();
        }

        public String getTypeName() {
            if (isAction()) {
                return "Accion [" + action.getName() + "]";
            } else if (isObject()) {
                return "Objeto [" + obj.getId() + "]";
            } else if (isClass()) {
                return "Clase [" + obj.getId() + "]";
            } else if (isFunction()) {
                return "Funcion [" + name + "]";
            }
            return null;
        }

        public void addBlock(String moduleName, List<CompileParserException> errors) throws CompileParserException {
            if (isAction()) {
                owner.addWarning("[Compilando accion " + action.getName() + "]");
                action.digest(moduleName, lines, errors);
                owner.getGrammar().addAction(action);
            } else if (isObject()) {
                owner.addWarning("[Compilando objeto " + obj.getId() + "]");
                obj.digest(moduleName, lines, errors);
                owner.addObject(obj);
            } else if (isClass()) {
                owner.addWarning("[Compilando clase " + obj.getId() + "]");
                obj.digest(moduleName, lines, errors);
                owner.addClass(obj);
            } else if (isFunction()) {
                owner.addWarning("[Compilando funcion " + name + "]");
                owner.addFunction(name, lines);
            }
        }
    }

    public Modulo digest(String moduleName, List<Line> lines) {
        Line lastLine = null;
        DigestingBlock df = null;
        for (Line myLine : lines) {
            try {
                String line = myLine.getText();
                lastLine = myLine;
                if (df != null) {
                    if ((line.equals(END_FUNC) && df.isFunction()) || (line.equals(ENDACTION) && df.isAction()) || (line.equals(ENDOBJECT) && df.isObject()) || (line.equals(ENDCLASS) && df.isClass())) {
                        df.addBlock(moduleName, errors);
                        df = null;
                    } else {
                        df.addLine(myLine);
                    }
                } else {
                    if (line.equals(END_FUNC) || line.equals(ENDACTION) || line.equals(ENDOBJECT) || line.equals(ENDCLASS)) {
                        throw new CompileParserException("Finalizacion de bloque con [" + line + "] sin haberlo abierto previamente");
                    } else if ((line.startsWith(DEF_FUNC + " "))) {
                        df = parseMethod(myLine);
                        if (df.inline) {
                            df.addBlock(moduleName, errors);
                            df = null;
                        }
                    } else if (line.startsWith(OBJECT + " ") || line.startsWith(CLASS + " ")) {
                        df = new DigestingBlock(this);
                        df.obj = new Obj(line, this);
                    } else if (line.startsWith(ACTION + " ")) {
                        df = new DigestingBlock(this);
                        df.action = new Action(this.getGrammar(), line);
                    } else if (!digestLine(RELATION, line) && !digestLine(CONST, line) && !digestLine(DEFINE, line) && !digestLine(INCLUDE, line) && !digestLine(PARSER_TIPOSNOMBRE, line, grammar, "addGenNum") && !digestLine(PARSER_ARTICULO, line, grammar, "addArticle") && !digestLine(PARSER_PRONOMBRE, line, grammar, "addPronombre") && !digestLine(PARSER_INFINITIVO, line, grammar, "addInfinitive") && !digestLine(PARSER_TOKEN, line, grammar, "addToken") && !digestLine(PARSER_TOKENEXCL, line, grammar, "addTokenExcluyente")) {
                        throw new CompileParserException("Linea desconocida [" + line + "]");
                    }
                }
            } catch (Throwable e) {
                df = null;
                CompileParserException toThrow = null;
                if (e instanceof CompileParserException) {
                    toThrow = (CompileParserException) e;
                } else {
                    toThrow = new CompileParserException(e);
                }
                if (toThrow.getLineNumber() == -1 && lastLine != null) {
                    toThrow.setLineNumber(lastLine.getNumber());
                }
                toThrow.setModuleName(moduleName);
                errors.add(toThrow);
            }
        }
        if (df != null) {
            errors.add(new CompileParserException(df.getTypeName() + " sin finalizar"));
        }
        return this;
    }

    private boolean digestLine(String token, String line) throws CompileParserException {
        String methodName = "parse" + normalize(token);
        return digestLine(token, line, this, methodName);
    }

    private boolean digestLine(String token, String line, Object target, String methodName) throws CompileParserException {
        try {
            if (!line.startsWith(token)) {
                return false;
            }
            if (line.length() == token.length()) {
                throw new CompileParserException("Definicion " + line + " vacia ");
            }
            line = line.substring(token.length() + 1);
            Method method = target.getClass().getMethod(methodName, new Class[] { String.class });
            method.invoke(target, line);
            return true;
        } catch (NoSuchMethodException e) {
            throw new CompileParserException("No existe metodo para parsear " + token, e);
        } catch (IllegalAccessException e) {
            throw new CompileParserException("Acceso ilegal al parsear " + token, e);
        } catch (InvocationTargetException e) {
            if (e.getTargetException() != null) if (e.getTargetException() instanceof CompileParserException) {
                throw (CompileParserException) e.getTargetException();
            } else {
                throw new CompileParserException(e.getTargetException());
            } else {
                throw new CompileParserException("Error al parsear " + token, e);
            }
        }
    }

    private String normalize(String in) {
        StringBuilder sb = new StringBuilder();
        boolean capitalizeNext = true;
        for (int x = 0; x < in.length(); x++) {
            char c = in.charAt(x);
            if (capitalizeNext) {
                c = Character.toUpperCase(c);
                capitalizeNext = false;
            } else if (c == 32) {
                capitalizeNext = true;
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private DigestingBlock parseMethod(Line myLine) throws CompileParserException {
        String line = myLine.getText();
        if (!line.startsWith(DEF_FUNC + " ")) {
            return null;
        }
        line = line.substring(DEF_FUNC.length() + 1).trim();
        DigestingBlock df = new DigestingBlock(this);
        int pos = line.indexOf(":");
        if (pos > 0) {
            String code = line.substring(pos + 1).trim();
            line = line.substring(0, pos).trim();
            if (code.length() == 0) {
                throw new CompileParserException("Metodo en linea vacio: " + line);
            }
            myLine.setText(code);
            df.addLine(myLine);
            df.inline = true;
        }
        df.name = line;
        return df;
    }

    private void validate() throws CompileParserException {
        compileConst();
        checkCircularObjects();
        indexObjectsName();
        validateObjectsAndHash();
        validateInjectedFunctions();
        transformConst();
        getGrammar().indexAndValidateActions();
        if (hasParent()) {
            maxObjectNameWordCount = Math.max(maxObjectNameWordCount, getParentModulo().getMaxObjectNameWordCount());
        }
    }

    private void compileConst() throws CompileParserException {
        if (consts == null || consts.size() == 0) {
            return;
        }
        AdvOgnlscriptContext contextoTemporal = AdvOgnlscriptContext.newTemporalContextForConstants();
        LimitedMap rootDefines = new LimitedMap(new HashMap(), true, true, true);
        for (String key : consts.keySet()) {
            String expr = (String) consts.get(key);
            try {
                OgnlExpression parsed = new OgnlExpression(expr);
                Object o = parsed.getValue(contextoTemporal, rootDefines);
                if (o != null && !inmutable(o) && !(o instanceof LimitedCollection)) {
                    throw new CompileParserException("Error en [" + CONST + " " + key + "]: solo se permiten valores constantes de texto, numeros, objetos o colecciones (lists, maps y sets).");
                }
                rootDefines.setReadonly(false);
                rootDefines.put(key, o);
                rootDefines.setReadonly(true);
                consts.put(key, o);
            } catch (Exception e1) {
                throw new CompileParserException("Error en la expresion [" + CONST + " " + key + "]", e1);
            }
        }
    }

    private void validateInjectedFunctions() throws CompileParserException {
        for (Map.Entry<String, FunctionBlock> entry : functions.entrySet()) {
            int pos = entry.getKey().indexOf(".");
            if (pos > 0) {
                String objFunc = entry.getKey().substring(0, pos);
                if (objFunc.length() > 0 && !containsObject(objFunc, true)) {
                    throw new CompileParserException("Error en funcion global de objeto [" + entry.getKey() + "]. Objeto " + objFunc + " no existe.");
                }
            }
        }
    }

    private void validateObjectsAndHash() throws CompileParserException {
        Map<String, Obj> fullnames = new HashMap<String, Obj>();
        Set<String> hashIds = new TreeSet<String>();
        Map<String, ObjWraper> values = new HashMap<String, ObjWraper>();
        for (Obj obj : getObjects(true)) {
            values.put(obj.getId(), ObjWraper.newObjWraper(obj, obj.getFullname()));
        }
        Map root = new LimitedMap(values, true, true, false);
        AdvOgnlscriptContext contextoTemporal = AdvOgnlscriptContext.newTemporalContextForObjVars();
        for (Obj obj : objectsById.values()) {
            compileObjVars(obj, contextoTemporal, root);
            hashIds.add(obj.getId());
            if (obj.getFullname() != null && !obj.isClass()) {
                Obj other = fullnames.get(obj.getFullname().getName().toLowerCase());
                if (other != null) {
                    addWarning("[Aviso] El nombre principal '" + obj.getFullname().getName().toLowerCase() + "' del objeto [" + obj.getId() + "] es el mismo que el del objeto [" + other.getId() + "]. Esto puede provocar que no se puedan distinguir entre si, aunque tengan sinonimos distintos, si los dos objetos se encuentran en el mismo sitio y el sistema los lista juntos.");
                }
                fullnames.put(obj.getFullname().getName().toLowerCase(), obj);
            }
        }
        hash = TextTools.getMD5(hashIds.toString());
    }

    private void indexObjectsName() throws CompileParserException {
        objectNameIndex = new TreeMap<String, AmbiguousGroup>();
        fullnameById = new LinkedHashMap<String, Fullname>();
        for (Obj obj : objectsById.values()) {
            if (!obj.isClass()) {
                obj.fixFullname();
                if (obj.getFullname() == null && !obj.getSynonimous().isEmpty()) {
                    throw new CompileParserException("Error en objeto/clase [" + obj.getId() + "]: Se han definido sinonimos, pero no un nombre.");
                }
                indexObjectByName(obj);
            }
        }
    }

    private void checkCircularObjects() throws CompileParserException {
        for (Obj obj : objectsById.values()) {
            if (obj.getSuperId() != null && obj.getSuper() == null) {
                throw new CompileParserException("Error en objeto/clase [" + obj.getId() + "]: La clase de la que hereda [" + obj.getSuperId() + "] no existe.");
            }
            if (obj.getSuperId() != null && !obj.getSuper().isClass()) {
                throw new CompileParserException("Error en objeto/clase [" + obj.getId() + "]: La clase de la que hereda [" + obj.getSuperId() + "] debe ser una clase, pero es un objeto.");
            }
            if (checkCircularClass(obj)) {
                throw new CompileParserException("Error en objeto/clase [" + obj.getId() + "]: Se ha detectado referencia circular en herencia.");
            }
        }
        for (Obj obj : objectsById.values()) {
            String relation = obj.getRelation(true);
            if (relation != null && !containsRelacion(relation, true)) {
                throw new CompileParserException("Error en objeto/clase [" + obj.getId() + "]: Relacion [" + relation + "] invalida, solo se permite " + getRelaciones(true));
            }
            String startPosition = obj.getStartPosition(true);
            if (startPosition != null) {
                Obj possibleLocation = getObject(startPosition, true);
                if (possibleLocation == null) {
                    throw new CompileParserException("Error en objeto/clase [" + obj.getId() + "]: La situacion del objeto no existe [" + startPosition + "]");
                }
                if (checkCircularRelation(obj)) {
                    throw new CompileParserException("Error en objeto/clase [" + obj.getId() + "]: Se ha detectado referencia circular en relacion (en objeto hijo de otro)");
                }
            }
            if (containsConst(obj.getId(), true)) {
                throw new CompileParserException("Error en objeto/clase [" + obj.getId() + "]: Existe una constante con el mismo nombre.");
            }
        }
    }

    private void compileObjVars(Obj obj, AdvOgnlscriptContext contextoTemporal, Map rootObjetos) throws CompileParserException {
        for (Iterator ii = obj.getAttributes(false).entrySet().iterator(); ii.hasNext(); ) {
            Map.Entry entry = (Map.Entry) ii.next();
            String key = (String) entry.getKey();
            String expr = (String) entry.getValue();
            if (expr != null) {
                Object m = null;
                try {
                    if (obj.isConstantAttribute(key, true)) {
                        contextoTemporal.setInmutableCollections(true);
                    } else {
                        contextoTemporal.setInmutableCollections(false);
                    }
                    OgnlExpression parsed = new OgnlExpression(expr);
                    m = parsed.getValue(contextoTemporal, rootObjetos);
                } catch (Exception e) {
                    throw new CompileParserException("Error en objeto [" + obj.getId() + "] atributo [" + key + "]: Error al evaluar la expresion.", e);
                }
                if (m != null && !inmutable(m) && !isCollection(m)) {
                    throw new CompileParserException("Error en objeto [" + obj.getId() + "] atributo [" + key + "]: solo se permiten valores constantes (literales de texto, valores booleanos, numeros y colecciones).");
                }
                if (m != null && isCollection(m)) {
                    obj.addCollectionAttribute(key);
                }
                entry.setValue(m);
            }
        }
    }

    private boolean checkCircularRelation(Obj obj) {
        Obj parent = getObject(obj.getStartPosition(true), true);
        while (parent != null) {
            if (parent.getId().equals(obj.getId())) {
                return true;
            }
            parent = getObject(parent.getStartPosition(true), true);
        }
        return false;
    }

    private boolean checkCircularClass(Obj obj) {
        Obj parent = obj.getSuper();
        while (parent != null) {
            if (parent.getId().equals(obj.getId())) {
                return true;
            }
            parent = parent.getSuper();
        }
        return false;
    }

    private void addClass(Obj obj) throws CompileParserException {
        Obj exists = getObject(obj.getId(), true);
        if (exists != null) {
            throw new CompileParserException("Ya existe un objeto o clase con el nombre de la clase [" + obj.getId() + "]");
        }
        if (objectsById == Collections.EMPTY_MAP) {
            objectsById = new LinkedHashMap<String, Obj>();
        }
        objectsById.put(obj.getId(), obj);
    }

    public void addObject(Obj obj) throws CompileParserException {
        Obj exists = getObject(obj.getId(), true);
        if (exists != null) {
            throw new CompileParserException("Ya existe un objeto o clase con el nombre del objeto [" + obj.getId() + "]");
        }
        if (objectsById == Collections.EMPTY_MAP) {
            objectsById = new LinkedHashMap<String, Obj>();
        }
        objectsById.put(obj.getId(), obj);
    }

    public void indexObjectByName(Obj obj) throws CompileParserException {
        for (Iterator i = obj.createSynonimous().entrySet().iterator(); i.hasNext(); ) {
            Map.Entry entry = (Map.Entry) i.next();
            String name = (String) entry.getKey();
            Fullname fullname = (Fullname) entry.getValue();
            if (!grammar.containsGenNum(fullname.getType(), true)) {
                throw new CompileParserException("Error en nombre/sinonimo [" + fullname.getName() + "] del objeto: Genero y numero [" + fullname.getType() + "] no esta definido en la gramatica.");
            }
            AmbiguousGroup ambiguousGroup = objectNameIndex.get(name);
            if (ambiguousGroup == null) {
                ambiguousGroup = new AmbiguousGroup();
                ambiguousGroup.add(fullname);
                objectNameIndex.put(name, ambiguousGroup);
            } else {
                ambiguousGroup.add(fullname);
            }
            fullnameById.put(fullname.getFullnameId(), fullname);
            maxObjectNameWordCount = Math.max(maxObjectNameWordCount, ambiguousGroup.getMaxWordCount());
        }
    }

    public void parseConst(String line) throws CompileParserException {
        int p = TextTools.findSeparator(line);
        if (p > 0) {
            String key = line.substring(0, p).trim();
            String value = line.substring(p + 1).trim();
            try {
                TextTools.validateNumName(key, "_");
            } catch (Exception e) {
                throw new CompileParserException("Error de formato en [" + CONST + " " + line + "], nombre [" + key + "]", e);
            }
            if (ReservedWords.isReserved(key)) {
                throw new CompileParserException("Error en [" + CONST + " " + line + "], constante [" + key + "] es un nombre reservado.");
            } else if (containsConst(key, true)) {
                throw new CompileParserException("Error en [" + CONST + " " + line + "], constante [" + key + "]. Ya existe una constante con ese nombre.");
            }
            if (consts == Collections.EMPTY_MAP) {
                consts = new LinkedHashMap<String, Object>();
            }
            consts.put(key, value);
        } else {
            throw new CompileParserException("Definicion de constante incorrecta: [" + CONST + " " + line + "]");
        }
    }

    public void parseDefine(String line) throws CompileParserException {
        int p = TextTools.findSeparator(line);
        if (p > 0) {
            String key = line.substring(0, p).trim();
            String value = line.substring(p + 1).trim();
            if (containsDefine(key, true)) {
                throw new CompileParserException("Error en [" + DEFINE + " " + line + "],  duplicada");
            }
            if (define == Collections.EMPTY_MAP) {
                define = new LinkedHashMap<String, String>();
            }
            define.put(key, value);
        } else {
            throw new CompileParserException("Definicion incorrecta: [" + DEFINE + " " + line + "]");
        }
    }

    public static boolean inmutable(Object o) {
        return (o instanceof Number || o instanceof String || o instanceof Boolean || o instanceof ObjWraper || o instanceof LimitedCollection);
    }

    public static boolean isCollection(Object o) {
        return (o instanceof ArrayList || o instanceof LinkedHashSet || o instanceof LinkedHashMap);
    }

    public void parseRelation(String line) throws CompileParserException {
        for (StringTokenizer st = new StringTokenizer(line, ","); st.hasMoreElements(); ) {
            String tok = st.nextToken().trim();
            try {
                TextTools.validateNumName(tok, "_");
            } catch (Exception e) {
                throw new CompileParserException("Error de formato en [" + RELATION + " " + line + "], [" + tok + "]", e);
            }
            if (relaciones == Collections.EMPTY_SET) {
                relaciones = new LinkedHashSet<String>();
            }
            relaciones.add(tok);
        }
    }

    void addFunction(String function, List<Line> commandList) throws CompileParserException {
        try {
            Set<String> args = null;
            if (function.endsWith(")")) {
                int pos = function.indexOf("(");
                if (pos > -1) {
                    String arglist = function.substring(pos + 1, function.length() - 1).trim();
                    args = new LinkedHashSet<String>();
                    for (StringTokenizer st = new StringTokenizer(arglist, ","); st.hasMoreTokens(); ) {
                        String arg = st.nextToken().trim();
                        args.add(arg);
                    }
                    function = function.substring(0, pos);
                }
            }
            FunctionBlock cb = AdvFunctionFactory.getInstance().newFunction(this, function, args, commandList);
            for (StringTokenizer st = new StringTokenizer(function, ","); st.hasMoreTokens(); ) {
                String nameFunc = st.nextToken().trim();
                _addFunction(cb, nameFunc, args);
            }
        } catch (OgnlscriptCompileException e) {
            CompileParserException ce = new CompileParserException(e);
            ce.setMethodName(function);
            ce.setLineNumber(e.getLineNumber());
            throw ce;
        }
    }

    private void _addFunction(FunctionBlock cb, String nameToParse, Set<String> args) throws CompileParserException {
        boolean almo = false;
        if (nameToParse.startsWith("#")) {
            nameToParse = nameToParse.substring(1);
            almo = true;
        }
        int colonPos = nameToParse.indexOf(".");
        if (almo && colonPos > -1) {
            throw new CompileParserException("Error en funcion global [" + nameToParse + "]. No se puede especificar el objeto o clase y prefijo '#' a la vez");
        } else if (!almo && colonPos == -1) {
            throw new CompileParserException("Error en funcion global [" + nameToParse + "]. Se debe especificar el objeto o clase al que pertenece con '.' o especificar prefijo '#'");
        }
        String realFuncname;
        String objFunc;
        if (colonPos > 0) {
            realFuncname = nameToParse.substring(colonPos + 1).trim();
            objFunc = nameToParse.substring(0, colonPos).trim();
        } else {
            realFuncname = nameToParse;
            objFunc = null;
        }
        try {
            TextTools.validateNumName(realFuncname, VALIDCHARS_IDENTIFIERS);
        } catch (Exception e) {
            throw new CompileParserException("Error de formato en funcion global '" + nameToParse + "'", e);
        }
        if (almo) {
            if (ConstantsLive.CTX_RESERVED.contains(realFuncname) || OgnlContext.RESERVED_KEYS.containsKey(realFuncname)) {
                throw new CompileParserException("Error en funcion global " + nameToParse + ". El nombre '" + realFuncname + "' es un nombre reservado.");
            }
        }
        if (ReservedWords.isReserved(realFuncname)) {
            throw new CompileParserException("Error en funcion global " + nameToParse + ". '" + realFuncname + "' es un nombre reservado.");
        } else if (containsFunction(nameToParse, true)) {
            throw new CompileParserException("Error en funcion global " + nameToParse + ". Funcion repetida.");
        }
        if (functions == Collections.EMPTY_MAP) {
            functions = new LinkedHashMap<String, FunctionBlock>();
        }
        functions.put((almo ? "#" : "") + nameToParse, cb);
    }

    public String dump(boolean withCodeFunctions, boolean withCodeObject) {
        return dumpConst() + dumpGrammar() + dumpObjects(withCodeObject) + dumpFunctions(withCodeFunctions);
    }

    public String dumpObjects(boolean withCode) {
        StringBuilder sb = new StringBuilder();
        for (Iterator i = objectsById.values().iterator(); i.hasNext(); ) {
            Obj o = (Obj) i.next();
            sb.append(o.dump(withCode));
            if (i.hasNext()) {
                sb.append("\n\n");
            }
        }
        return sb.toString();
    }

    public String dumpFunctions(boolean withCodeFunctions) {
        StringBuilder sb = new StringBuilder();
        for (Iterator i = functions.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry entry = (Map.Entry) i.next();
            sb.append("\n").append(entry.getKey()).append(":");
            if (withCodeFunctions) {
                sb.append("\n").append(((FunctionBlock) entry.getValue()).dump(1));
            }
        }
        return sb.toString();
    }

    public String dumpConst() {
        StringBuilder sb = new StringBuilder();
        return sb.toString();
    }

    public String dumpGrammarFull() {
        return grammar.dumpActionsFull();
    }

    public String dumpGrammar() {
        return grammar.dumpActions();
    }

    public String dumpSynonimous() {
        StringBuilder sb = new StringBuilder();
        for (Iterator i = objectNameIndex.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry entry = (Map.Entry) i.next();
            String name = (String) entry.getKey();
            AmbiguousGroup ambiguousGroup = (AmbiguousGroup) entry.getValue();
            sb.append(ambiguousGroup.getItems().size()).append(":\"").append(name).append("\" -> ").append(ambiguousGroup.getItems()).append("\n");
        }
        sb.append("Total objetos/sinonimos:").append(objectsById.size()).append("/").append(objectNameIndex.size()).append("\n");
        return sb.append("Numero de palabras del nombre compuesto mas largo:").append(maxObjectNameWordCount).append("\n").toString();
    }

    public boolean containsObject(String id, boolean searchInParents) {
        boolean encontrado = objectsById.containsKey(id);
        if (encontrado) {
            encontrado = !objectsById.get(id).isClass();
        }
        return encontrado || (searchInParents && hasParent() && getParentModulo().containsObject(id, searchInParents));
    }

    public Obj getObject(String name, boolean searchInParents) {
        Obj obj = objectsById.get(name);
        if (obj == null && searchInParents && hasParent()) {
            return getParentModulo().getObject(name, searchInParents);
        }
        return obj;
    }

    public boolean containsClass(String id, boolean searchInParents) {
        boolean encontrado = objectsById.containsKey(id);
        if (encontrado) {
            encontrado = objectsById.get(id).isClass();
        }
        return encontrado || (searchInParents && hasParent() && getParentModulo().containsClass(id, searchInParents));
    }

    public Obj getClass(String name, boolean searchInParents) {
        Obj obj = objectsById.get(name);
        if (obj != null && obj.isClass()) {
            return obj;
        }
        if (obj == null && searchInParents && hasParent()) {
            return getParentModulo().getClass(name, searchInParents);
        }
        return obj;
    }

    public boolean containsFunction(String func, boolean searchInParents) {
        boolean encontrado = functions.containsKey(func);
        encontrado = encontrado || (searchInParents && hasParent() && getParentModulo().containsFunction(func, searchInParents));
        return encontrado;
    }

    public FunctionBlock getFunction(String func, boolean searchInParents) {
        FunctionBlock cb = functions.get(func);
        if (cb == null && searchInParents && hasParent()) {
            return getParentModulo().getFunction(func, searchInParents);
        }
        return cb;
    }

    public boolean containsRelacion(String id, boolean searchInParents) {
        return relaciones.contains(id) || (searchInParents && hasParent() && getParentModulo().containsRelacion(id, searchInParents));
    }

    public boolean containsConst(String id, boolean searchInParents) {
        return consts.containsKey(id) || (searchInParents && hasParent() && getParentModulo().containsConst(id, searchInParents));
    }

    public boolean containsDefine(String id, boolean searchInParents) {
        return define.containsKey(id) || (searchInParents && hasParent() && getParentModulo().containsDefine(id, searchInParents));
    }

    public Object getConstant(String key, boolean searchInParents) {
        boolean encontrado = consts.containsKey(key);
        if (!encontrado && searchInParents && hasParent()) {
            return getParentModulo().getConstant(key, searchInParents);
        }
        return consts.get(key);
    }

    public String getDefine(String key, boolean searchInParents) {
        boolean encontrado = define.containsKey(key);
        if (!encontrado && searchInParents && hasParent()) {
            return getParentModulo().getDefine(key, searchInParents);
        }
        return define.get(key);
    }

    public Object execute(AdvOgnlscriptContext context, String funcName, Object args[]) throws NoSuchMethodException, RuntimeGameException {
        FunctionBlock cb = getFunction(funcName, true);
        if (cb != null) {
            String oldFuncname = context.getFunctionName();
            Object oldroot = context.getRoot();
            try {
                context.setFunctionName(funcName);
                context.setRoot(context.getModulo().getModuloWraperRoot());
                return AdvFunctionFactory.getInstance().execute(funcName, cb, context, args);
            } catch (OgnlscriptTraceableException e) {
                e.setScope(funcName);
                throw new RuntimeGameException("Error en " + funcName + "():", e);
            } catch (OgnlscriptRuntimeException e) {
                throw new RuntimeGameException("Error en " + funcName + "():", e);
            } finally {
                context.setRoot(oldroot);
                context.setFunctionName(oldFuncname);
            }
        } else {
            throw new NoSuchMethodException("Metodo no encotrado: " + funcName);
        }
    }

    public Object executeSecure(AdvOgnlscriptContext context, String func) throws RuntimeGameException {
        return executeSecure(context, func, null);
    }

    public Object executeSecure(AdvOgnlscriptContext context, String func, Object[] args) throws RuntimeGameException {
        try {
            return execute(context, func, args);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    public void parseInclude(String parent) throws CompileParserException {
        int p = parent.indexOf("/");
        if (p > 1) {
            this.parentUser = parent.substring(0, p);
            this.parentPro = parent.substring(p + 1);
            if (!parentUser.equals("system") && !parentUser.equals(userid)) {
                throw new CompileParserException("Libreria a incluir solo puede pertenece al usuario system o tu propio usuario [" + userid + "]");
            }
            Modulo d = getParentModulo();
            if (d == null) {
                throw new CompileParserException("Libreria a incluir no existe: " + parent);
            } else if (d.hasParent()) {
                throw new CompileParserException("Solo se pueden incluir librerias");
            }
        } else {
            throw new CompileParserException("Identificador de libreria incorrecto: " + parent);
        }
    }

    public boolean hasParent() {
        return parentUser != null;
    }

    public boolean hasMain() {
        return containsObject(ConstantsLive.OBJ_MAIN, true);
    }

    public Modulo getParentModulo() {
        if (parentUser == null) return null;
        return ModuloMng.getInstance().getSecureAdv(parentPro, parentUser);
    }

    public int getMaxObjectNameWordCount() {
        return maxObjectNameWordCount;
    }

    public AmbiguousGroup getObjectGroup(String name) throws RuntimeGameException {
        AmbiguousGroup group = objectNameIndex.get(name);
        AmbiguousGroup parentGroup = null;
        if (hasParent()) {
            parentGroup = getParentModulo().getObjectGroup(name);
        }
        if (group != null && parentGroup != null) {
            return new AmbiguousGroup(group, parentGroup);
        } else if (group != null) {
            return group;
        } else if (parentGroup != null) {
            return parentGroup;
        }
        return null;
    }

    public Collection<Obj> getObjects(boolean withParent) {
        if (withParent && hasParent()) {
            Set<Obj> all = new LinkedHashSet<Obj>(objectsById.values());
            all.addAll(getParentModulo().getObjects(withParent));
            return all;
        }
        return objectsById.values();
    }

    public Map<String, Obj> getObjectsMap(boolean withParent) {
        if (withParent && hasParent()) {
            Map<String, Obj> all = new LinkedHashMap<String, Obj>(objectsById);
            all.putAll(getParentModulo().getObjectsMap(withParent));
            return all;
        }
        return objectsById;
    }

    public Map<String, FunctionBlock> getFunctions(boolean withParent) {
        if (withParent && hasParent()) {
            Map<String, FunctionBlock> all = new LinkedHashMap<String, FunctionBlock>(functions);
            all.putAll(getParentModulo().getFunctions(withParent));
            return all;
        }
        return functions;
    }

    public Set<String> getRelaciones(boolean withParent) {
        Set<String> all = new LinkedHashSet<String>();
        if (withParent && hasParent()) {
            all.addAll(getParentModulo().getRelaciones(withParent));
        }
        for (Iterator i = relaciones.iterator(); i.hasNext(); ) {
            String key = (String) i.next();
            all.add(key);
        }
        return all;
    }

    public Map<String, Object> getConstantMap(boolean withParent) {
        if (withParent && hasParent()) {
            Map<String, Object> all = new LinkedHashMap<String, Object>(consts);
            all.putAll(getParentModulo().getConstantMap(withParent));
            return all;
        }
        return consts;
    }

    public Map<String, Fullname> getFullnames(boolean withParent) {
        if (withParent && hasParent()) {
            Map<String, Fullname> all = new LinkedHashMap<String, Fullname>(fullnameById);
            all.putAll(getParentModulo().getFullnames(withParent));
            return all;
        }
        return fullnameById;
    }

    public Fullname getFullname(String fullnameid, boolean withParent) {
        Fullname name = fullnameById.get(fullnameid);
        if (name == null && withParent && hasParent()) {
            return getParentModulo().getFullname(fullnameid, withParent);
        }
        return name;
    }

    public String getHash() {
        return hash;
    }

    public String getDefaultParent() {
        return getDefine(ConstantsLive.DEFINE_DEFAULTPARENT, true);
    }

    public void addWarning(String m) {
        if (warnings == Collections.EMPTY_LIST) {
            warnings = new ArrayList<String>();
        }
        warnings.add(m);
    }

    public List<String> getWarnings() {
        return warnings;
    }

    long lastAccess = 0;

    public long getLastAccess() {
        return lastAccess;
    }

    public void touch() {
        lastAccess = System.currentTimeMillis();
    }

    public String toString() {
        return "Modulo " + getId() + " " + getHash();
    }

    public boolean findAST(FinderAST finder, boolean parents) {
        for (FunctionBlock fb : getFunctions(parents).values()) {
            if (fb.findAST(finder, fb.getName()) == true) {
                return true;
            }
        }
        for (Obj obj : getObjects(parents)) {
            if (obj.findAST(finder) == true) {
                return true;
            }
        }
        return false;
    }

    public List<FinderASTResult> findConstant(final Object value, boolean parents) {
        final List<FinderASTResult> l = new ArrayList<FinderASTResult>();
        findAST(new FinderAST() {

            public boolean process(OgnlExpression expression, String method, Node node) {
                if (node instanceof ASTConst) {
                    ASTConst constante = (ASTConst) node;
                    if (value == null) {
                        if (constante.getValue() == null) {
                            l.add(new FinderASTResult(expression, method));
                        }
                    } else {
                        if (value.equals(constante.getValue())) {
                            l.add(new FinderASTResult(expression, method));
                        }
                    }
                }
                return false;
            }
        }, parents);
        return l;
    }

    public List<FinderASTResult> findCall(final String name, boolean parents) {
        final List<FinderASTResult> l = new ArrayList<FinderASTResult>();
        findAST(new FinderAST() {

            public boolean process(OgnlExpression expression, String method, Node node) {
                if (node instanceof ASTMethod) {
                    ASTMethod m = (ASTMethod) node;
                    if (name.equals(m.getMethodName())) {
                        l.add(new FinderASTResult(expression, method));
                    }
                }
                return false;
            }
        }, parents);
        return l;
    }

    public List<FinderASTResult> findContextVariable(final String name, boolean parents) {
        final List<FinderASTResult> l = new ArrayList<FinderASTResult>();
        findAST(new FinderAST() {

            public boolean process(OgnlExpression expression, String method, Node node) {
                if (node instanceof ASTVarRef) {
                    ASTVarRef varRef = (ASTVarRef) node;
                    if (name.equals(varRef.getName())) {
                        l.add(new FinderASTResult(expression, method));
                    }
                }
                return false;
            }
        }, parents);
        return l;
    }

    public List<FinderASTResult> findVariable(final String name, boolean parents) {
        final List<FinderASTResult> l = new ArrayList<FinderASTResult>();
        findAST(new FinderAST() {

            public boolean process(OgnlExpression expression, String method, Node node) {
                if (node instanceof ASTProperty) {
                    ASTProperty varRef = (ASTProperty) node;
                    String prop = varRef.getPropertyName();
                    if (prop != null && name.equals(prop)) {
                        l.add(new FinderASTResult(expression, method));
                    }
                }
                return false;
            }
        }, parents);
        return l;
    }

    public List<FinderASTResult> transformConst() {
        final List<FinderASTResult> l = new ArrayList<FinderASTResult>();
        findAST(new FinderAST() {

            public boolean process(OgnlExpression expression, String method, Node node) {
                if (node instanceof ASTProperty) {
                    ASTProperty varRef = (ASTProperty) node;
                    String prop = varRef.getPropertyName();
                    if (prop != null && containsConst(prop, true)) {
                        Object o = getConstant(prop, true);
                        if (o == null || o instanceof String || o instanceof Boolean || o instanceof Number) {
                            varRef.setConstant(o);
                        }
                    }
                }
                return false;
            }
        }, false);
        return l;
    }

    public int indexSize() {
        int size = 0;
        for (Fullname f : fullnameById.values()) {
            size += f.getName().length();
        }
        return size;
    }
}
