package adv.language;

import adv.db.bean.ProyectoBean;
import adv.db.bean.ProyectoManager;
import adv.language.beans.Modulo;
import adv.tools.TextTools;
import ognlscript.FileParser;
import ognlscript.Line;
import java.io.*;
import java.sql.SQLException;
import java.util.*;
import com.opensymphony.oscache.general.GeneralCacheAdministrator;
import com.opensymphony.oscache.base.NeedsRefreshException;

/**
 * Alberto Vilches Ratón
 * User: avilches
 * Date: 24-ene-2007
 * Time: 17:54:20
 * To change this template use File | Settings | File Templates.
 */
public class ModuloMng {

    private static ModuloMng ourInstance = new ModuloMng();

    private double expressionsTotal = 0;

    private double millisTotal = 0;

    private double requests = 0;

    private double maxExpressionsPerRequest = 0;

    private double maxMillisPerRequest = 0;

    public void registerRequest(int expressions, double millis) {
        expressionsTotal += expressions;
        millisTotal += millis;
        requests++;
        maxExpressionsPerRequest = Math.max(expressions, maxExpressionsPerRequest);
        maxMillisPerRequest = Math.max(expressions, maxMillisPerRequest);
    }

    private GeneralCacheAdministrator cache;

    public static final String ADV_EXTENSION = ".k";

    private static final Comparator SORTER = new Comparator() {

        public int compare(Object o1, Object o2) {
            return ((File) o1).getName().compareTo(((File) o2).getName());
        }
    };

    public static ModuloMng getInstance() {
        return ourInstance;
    }

    private ModuloMng() {
    }

    public void config() {
        if (cache == null) {
            cache = new GeneralCacheAdministrator();
        }
    }

    Set lastAccess = new TreeSet();

    public boolean isInCache(String aliaspro, String userlogin) {
        try {
            cache.getFromCache(userlogin + "/" + aliaspro);
            return true;
        } catch (NeedsRefreshException e) {
            cache.putInCache(userlogin + "/" + aliaspro, null);
            cache.removeEntry(userlogin + "/" + aliaspro);
            return false;
        }
    }

    public Modulo getAdv(String aliaspro, String userlogin) {
        try {
            return (Modulo) cache.getFromCache(userlogin + "/" + aliaspro);
        } catch (NeedsRefreshException e) {
            return load(aliaspro, userlogin);
        }
    }

    public File getCodeRoot(ProyectoBean pro) throws IOException {
        return getCodeRoot(pro.getAlias(), pro.getUsuario());
    }

    public File getCodeRoot(String aliaspro, String userlogin) throws IOException {
        File file = new File(Config.getMng().getDirCode().getCanonicalPath() + File.separatorChar + userlogin + File.separatorChar + aliaspro);
        return file;
    }

    public File getLogDir(ProyectoBean pro) throws IOException {
        return getLogDir(pro.getAlias(), pro.getUsuario());
    }

    public File getLogDir(String aliaspro, String userlogin) throws IOException {
        File file = new File(Config.getMng().getDirCode().getCanonicalPath() + File.separatorChar + userlogin + File.separatorChar + aliaspro + File.separator + "log");
        return file;
    }

    public File[] getFiles(ProyectoBean pro, boolean allFiles) throws IOException {
        return getFiles(pro.getAlias(), pro.getUsuario(), allFiles);
    }

    public File[] getFiles(ProyectoBean pro, boolean allFiles, Set<String> filterInclude) throws IOException {
        return getFiles(pro.getAlias(), pro.getUsuario(), allFiles, filterInclude);
    }

    public File[] getFiles(String aliaspro, String userlogin, boolean allFiles) throws IOException {
        return getFiles(aliaspro, userlogin, allFiles, null);
    }

    public File[] getFiles(String aliaspro, String userlogin, final boolean allFiles, final Set<String> filterInclude) throws IOException {
        File files[] = getCodeRoot(aliaspro, userlogin).listFiles(new FileFilter() {

            public boolean accept(File file) {
                String name = file.getName();
                return file.isFile() && (allFiles || name.endsWith(ADV_EXTENSION)) && ((filterInclude == null || filterInclude.contains(name)));
            }
        });
        if (files != null && files.length > 0) {
            Arrays.sort(files, SORTER);
        }
        return files != null ? files : new File[] {};
    }

    public Modulo load(String aliaspro, String userlogin) {
        return load(aliaspro, userlogin, null);
    }

    public Modulo load(String aliaspro, String userlogin, Set<String> filenames) {
        try {
            ProyectoBean pb = ProyectoManager.getInstance().loadByPrimaryKey(aliaspro, userlogin);
            if (pb == null) {
                throw new RuntimeException("Proyecto " + userlogin + "/" + aliaspro + " no existe.");
            }
            List<String> loadedFiles = new ArrayList<String>();
            Modulo modulo = new Modulo(pb.getUsuario(), pb.getAlias());
            File files[] = getFiles(pb, false, filenames);
            modulo.startDigest();
            for (File file : files) {
                if (filenames == null && file.getName().startsWith("!")) {
                    continue;
                }
                loadedFiles.add(file.getName());
                Reader r = new InputStreamReader(new FileInputStream(file), Config.getMng().getEncoding());
                List<Line> lines = FileParser.toList(r);
                TextTools.prepare(lines);
                modulo.digest(file.getName(), lines);
                if (!modulo.getErrors().isEmpty()) {
                    pb.setExcepcion("{Fichero: " + file.getName() + "}\n" + TextTools.printStackTrace(modulo.getErrors().get(0)));
                    pb.setHash(null);
                    break;
                }
            }
            modulo.endDigest();
            modulo.addWarning(loadedFiles.size() + " ficheros procesados: " + loadedFiles);
            if (modulo.getErrors().isEmpty()) {
                pb.setExcepcion(null);
                pb.setHash(modulo.getHash());
            }
            cache.putInCache(pb.getUsuario() + "/" + pb.getAlias(), modulo);
            ProyectoManager.getInstance().save(pb);
            return modulo;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean create(ProyectoBean pro, String filename) throws IOException {
        File file = getFile(pro, filename);
        if (file.exists()) {
            return false;
        }
        FileOutputStream fos = new FileOutputStream(file, false);
        fos.close();
        return true;
    }

    public boolean delete(ProyectoBean project, String filename) throws IOException {
        File file = getFile(project, filename);
        if (file.exists()) {
            file.delete();
            if (!file.exists()) {
                load(project.getAlias(), project.getUsuario());
                return true;
            }
        }
        return false;
    }

    public File getFile(ProyectoBean pro, String filename) throws IOException {
        File path = getCodeRoot(pro);
        File file = new File(path, filename);
        if (!file.getCanonicalPath().startsWith(path.getCanonicalPath())) {
            throw new IOException("Intento de acceso a archivo fuera de la carpeta de usuario.");
        }
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        return file;
    }

    public Modulo save(ProyectoBean pro, String filename, String src) throws IOException, SQLException {
        saveData(pro, filename, src);
        return load(pro.getAlias(), pro.getUsuario());
    }

    public void saveData(ProyectoBean pro, String filename, String src) throws IOException {
        File file = getFile(pro, filename);
        if (!file.exists()) {
            throw new IOException("Fichero " + filename + " no existe. Imposible guardar.");
        }
        PrintWriter printer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), Config.getMng().getEncoding()), true);
        TextTools.print(new StringReader(src), printer);
        printer.close();
    }

    public Modulo getSecureAdv(ProyectoBean pro) {
        return getSecureAdv(pro.getAlias(), pro.getUsuario());
    }

    public Modulo getSecureAdv(String aliaspro, String userlogin) {
        Modulo d = getAdv(aliaspro, userlogin);
        if (!d.getErrors().isEmpty()) {
            throw new RuntimeException(d.getErrors().get(0));
        }
        d.touch();
        return d;
    }

    public void remove(String alias) {
        cache.removeEntry(alias);
    }

    public void destroy() {
        cache.destroy();
    }
}
