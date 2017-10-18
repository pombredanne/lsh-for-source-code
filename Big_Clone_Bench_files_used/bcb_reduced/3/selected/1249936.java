package net.diet_rich.jabak.main.restore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;
import javax.swing.JFileChooser;
import net.diet_rich.jabak.core.database.DataBase;
import net.diet_rich.jabak.core.database.DataTable.Data;
import net.diet_rich.jabak.core.database.DataTable.Reference;
import net.diet_rich.jabak.core.database.DataTable.Repository;
import net.diet_rich.jabak.core.datafile.DataFileReader;
import net.diet_rich.jabak.main.Common;
import net.diet_rich.util.CmdLineParser;
import net.diet_rich.util.StringSettings;
import net.diet_rich.util.StringUtils;

/**
 * main entry class for restoring files from backup repositories.
 * 
 * @author Georg Dietrich
 */
public class Restore {

    /**
	 * main entry point for restoring files from backup repositories. the
	 * command line arguments understood are listed in {@link RestoreSwitches}.
	 * 
	 * @param args restore command line arguments
	 * @throws Exception
	 */
    public static void main(String[] args) throws Exception {
        final StringSettings stringSettings = new StringSettings("commandline");
        stringSettings.set(CmdLineParser.parseCommandLineArgs(args));
        Common.detectFileInArg0(stringSettings);
        Common.tryLoadConfigFile(stringSettings);
        if (!StringUtils.evalBoolString(stringSettings.get(RestoreSwitches.nodefaultconfigfile))) Common.tryLoadDefConfigFile(stringSettings, false);
        final RestoreSettings settings = new RestoreSettings();
        settings.source.stringSet(stringSettings.get(RestoreSwitches.source));
        settings.target.stringSet(stringSettings.get(RestoreSwitches.target));
        settings.hashall = StringUtils.evalBoolString(stringSettings.get(RestoreSwitches.hashall));
        settings.noGUI = StringUtils.evalBoolString(stringSettings.get(RestoreSwitches.noGUI));
        if (settings.noGUI) new Restore().call(settings);
        new RestoreFrameMethods(settings);
    }

    void call(RestoreSettings settings) throws Exception {
        File dbdir = new File(settings.source.get());
        String dbpath = dbdir.getPath() + File.separator + dbdir.getName() + ".db.gz";
        String datapath = dbdir.getPath();
        DataBase database = settings.database;
        if (database == null) database = Common.loadDataBase(dbpath);
        File target = new File(settings.target.get());
        if (!target.exists()) if (!target.mkdirs()) throw new RuntimeException("can't create target path");
        if (!target.canWrite()) throw new RuntimeException("can't write in target path");
        String base = target.getPath() + File.separator;
        boolean hashall = settings.hashall;
        DataFileReader reader = new DataFileReader(new File(datapath), dbdir.getName());
        Repository lastRep = settings.repository;
        if (lastRep == null) lastRep = database.repository(database.repositories() - 1);
        SortedMap<Reference, String> map = settings.restoreSet;
        if (map == null) {
            map = new TreeMap<Reference, String>();
            for (String path : database.entryPaths()) {
                List<Reference> refs = database.pathEntry(path);
                Reference ref = refs.get(refs.size() - 1);
                if (!ref.repositories.contains(lastRep)) continue;
                map.put(ref, path);
            }
        }
        Repository curRep = lastRep;
        for (Entry<Reference, String> entry : map.entrySet()) {
            String path = entry.getValue();
            Reference ref = entry.getKey();
            if (ref.data == null) {
                boolean created = new File(base + path).mkdirs();
                if (Debug.level > 1 && !created) System.err.println("problem creating directory " + base + path);
                continue;
            }
            if (ref.data.repository != curRep) {
                curRep = ref.data.repository;
                JFileChooser chooser = new JFileChooser();
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                chooser.setDialogType(JFileChooser.OPEN_DIALOG);
                chooser.setDialogTitle("please choose " + curRep.name);
                chooser.setCurrentDirectory(new File(datapath).getParentFile());
                chooser.setMultiSelectionEnabled(false);
                if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) throw new RuntimeException("restore aborted by user");
                datapath = chooser.getSelectedFile().getPath();
                reader = new DataFileReader(new File(datapath), new File(datapath).getName());
            }
            writeAndCheckFile(reader, base, path, database.hash(database.dataIndex(ref.data)), ref, hashall);
        }
    }

    /**
	 * write a file and check whether it's o.k.
	 */
    private void writeAndCheckFile(DataFileReader reader, String base, String path, String hash, Reference ref, boolean hashall) throws Exception {
        Data data = ref.data;
        File file = new File(base + path);
        file.getParentFile().mkdirs();
        if (Debug.level > 1) System.err.println("read file " + data.file + " at index " + data.index);
        OutputStream output = new FileOutputStream(file);
        if (hashall) output = new DigestOutputStream(output, MessageDigest.getInstance("MD5"));
        reader.read(output, data.index, data.file);
        output.close();
        if (hashall) {
            String filehash = StringUtils.toHex(((DigestOutputStream) output).getMessageDigest().digest());
            if (!hash.equals(filehash)) throw new RuntimeException("hash wasn't equal for " + file);
        }
        file.setLastModified(ref.lastmod);
        if (file.length() != data.size) throw new RuntimeException("corrupted file " + file);
    }
}
