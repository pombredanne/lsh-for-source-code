package adv.test;

import adv.language.Constants;
import ognlscript.Ognlscript;
import ognlscript.OgnlscriptContext;
import ognlscript.OgnlscriptRuntimeException;
import ognlscript.ResultObject;
import ognlscript.block.FunctionBlock;
import ognlscript.OgnlscriptCompileException;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Alberto Vilches Ratón
 * User: avilches
 * Date: 30-sep-2006
 * Time: 11:59:11
 * To change this template use File | Settings | File Templates.
 */
public class TestOgnlscript implements Constants {

    public static void main(String[] args) throws IOException, OgnlscriptCompileException {
        Map contextValues = new HashMap();
        contextValues.put("ctx1", "valctx1");
        Map mapa = new HashMap();
        mapa.put("atributo", "valor");
        Map root = new HashMap();
        root.put("mapa", mapa);
        long start = System.currentTimeMillis();
        Ognlscript ognlscript = new Ognlscript(JOINLINECHAR, STARTSINGLECOMMENTLINE, STARTMULTICOMMENT, ENDMULTICOMMENT, CR);
        ognlscript.setGrantClasses(GRANTCLASSES);
        FunctionBlock cb = ognlscript.digest(new File("c:\\work\\Proy\\Local\\Aventura\\doc\\testtodo.txt"), null);
        long COUNT = 1000;
        System.out.println("----- EXECUTE ----");
        OgnlscriptContext context = new OgnlscriptContext(new HashMap());
        context.setRoot(root);
        ResultObject result = null;
        try {
            result = ognlscript.execute(cb, context);
        } catch (OgnlscriptRuntimeException e) {
            e.printStackTrace();
        }
        System.out.println("----- RESULTS ----");
        System.out.println("Return:" + (result != null ? result.getResult() : null) + " (" + (result == null ? "no se devolvio nada" : (result.getResult() == null ? "null" : result.getResult().getClass().toString())) + ")");
        System.out.println("StdOut:" + context.getResponse().getStdOut());
        System.out.println("StdErr:" + context.getResponse().getStdErr());
        long end = System.currentTimeMillis() - start;
        System.out.println("Total " + end + " ms");
        COUNT = COUNT * 1000;
        System.out.println((double) ((double) end / (double) COUNT) + " seg cada iteracion");
        System.out.println("---------");
        System.out.println("root " + root);
        System.out.println("context " + context.getValues());
    }
}
