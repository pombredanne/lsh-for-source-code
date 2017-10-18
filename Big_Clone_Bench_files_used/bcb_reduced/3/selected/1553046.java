package net.sf.wwusmart.algorithms.framework;

import net.sf.wwusmart.algorithms.filter.*;
import net.sf.wwusmart.algorithms.combination.*;
import java.util.*;
import java.io.*;
import java.security.*;
import net.sf.wwusmart.database.*;
import net.sf.wwusmart.gui.*;
import net.sf.wwusmart.helper.Couple;
import net.sf.wwusmart.helper.UserAbortionException;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Thilo
 * @version $Rev: 777 $
 */
public class AlgorithmManager {

    private static final AlgorithmManager instance = new AlgorithmManager();

    private List<String> pluginDirectories;

    private Map<String, MatchAlgorithm> matchAlgorithms;

    private Map<String, FilterAlgorithm> filterAlgorithms;

    private Map<String, CombinationAlgorithm> combinationAlgorithms;

    private AlgorithmManager() {
        combinationAlgorithms = new HashMap<String, CombinationAlgorithm>(5);
        {
            CombinationAlgorithm a;
            a = new CombinationByOrder();
            combinationAlgorithms.put(a.getPersistentIdentifier(), a);
            a = new CombinationFunctionMaxAlgorithm();
            combinationAlgorithms.put(a.getPersistentIdentifier(), a);
            a = new CombinationFunctionMinAlgorithm();
            combinationAlgorithms.put(a.getPersistentIdentifier(), a);
            a = new CombinationFunctionBayesAlgorithm();
            combinationAlgorithms.put(a.getPersistentIdentifier(), a);
            a = new CombinationFunctionArithmeticMeanAlgorithm();
            combinationAlgorithms.put(a.getPersistentIdentifier(), a);
            a = new CombinationFunctionWeightedArithmeticMeanAlgorithm();
            combinationAlgorithms.put(a.getPersistentIdentifier(), a);
            a = new CombinationFunctionMedianAlgorithm();
            combinationAlgorithms.put(a.getPersistentIdentifier(), a);
        }
    }

    public void deleteAlgorithm(DescriptorAlgorithm a) throws SQLException {
        if (a instanceof MatchAlgorithm) {
            matchAlgorithms.remove(a.getPersistentIdentifier());
            DataManager.getInstance().deleteAlgorithm(a);
        } else {
            throw new IllegalArgumentException("Deleting of Filter Algorithms is not allowed.");
        }
    }

    public void initialize(boolean enableNativePlugins) {
        if (enableNativePlugins) {
            NativePluginConnector.getInstance().initialize();
        } else {
            NativePluginConnector.getInstance().disable();
        }
    }

    public void addAlgorithms(String[] filenames) throws IOException, SQLException {
        List<ChangedAlgorithmInfo> changed = new Vector<ChangedAlgorithmInfo>();
        FilenameLoop: for (String filename : filenames) {
            MatchAlgorithm aNew;
            if (JavaPluginConnector.knownFileType(filename)) {
                aNew = new JavaAlgorithm(filename, computeHash(filename));
            } else if (NativePluginConnector.knownFileType(filename)) {
                aNew = new NativeAlgorithm(filename, computeHash(filename));
            } else {
                throw new RuntimeException("Unknown plugin file extension from file `" + filename + "'");
            }
            try {
                aNew.initialize();
            } catch (Exception e) {
                Logger.getLogger(ParametersSetting.class.getName()).log(Level.FINE, "Failed to process file `" + filename + "'" + e);
                Logger.getLogger(ParametersSetting.class.getName()).log(Level.INFO, " ==> Skipping file `" + filename + "': Invalid smart plugin (" + e.getMessage() + ").");
                continue FilenameLoop;
            }
            changed.add(new ChangedAlgorithmInfo(aNew));
        }
        if (!changed.isEmpty()) {
            processChangedAlgorithms(changed);
        } else {
            GuiController.getInstance().displayMessage("No usable plugin files were found! Please check your plugin directories.");
        }
    }

    public MatchAlgorithm getMatchAlgorithm(String persistentIdentifier) {
        return matchAlgorithms.get(persistentIdentifier);
    }

    public FilterAlgorithm getFilterAlgorithm(String persistentIdentifier) {
        return filterAlgorithms.get(persistentIdentifier);
    }

    public CombinationAlgorithm getCombinationAlgorithm(String persistentIdentifier) {
        return combinationAlgorithms.get(persistentIdentifier);
    }

    public Algorithm getAlgorithm(String persistentIdentifier) {
        if (getMatchAlgorithm(persistentIdentifier) != null) {
            return getMatchAlgorithm(persistentIdentifier);
        }
        if (getFilterAlgorithm(persistentIdentifier) != null) {
            return getFilterAlgorithm(persistentIdentifier);
        }
        if (getCombinationAlgorithm(persistentIdentifier) != null) {
            return getCombinationAlgorithm(persistentIdentifier);
        }
        return null;
    }

    /**
     * Ruturn all Algorithms currently applicable.
     * @return All Algorithms currently applicable.
     */
    public Collection<MatchAlgorithm> getAllMatchAlgorithms() {
        Collection<MatchAlgorithm> col = matchAlgorithms.values();
        MatchAlgorithm[] arr = new MatchAlgorithm[col.size()];
        col.toArray(arr);
        Arrays.sort(arr, new Comparator<MatchAlgorithm>() {

            public int compare(MatchAlgorithm o1, MatchAlgorithm o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        return Arrays.asList(arr);
    }

    public Collection<FilterAlgorithm> getAllFilterAlgorithms() {
        Collection<FilterAlgorithm> col = filterAlgorithms.values();
        FilterAlgorithm[] arr = new FilterAlgorithm[col.size()];
        col.toArray(arr);
        Arrays.sort(arr, new Comparator<FilterAlgorithm>() {

            public int compare(FilterAlgorithm o1, FilterAlgorithm o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        return Arrays.asList(arr);
    }

    public List<DescriptorAlgorithm> getAllDescriptorAlgorithms() {
        List<DescriptorAlgorithm> result = new Vector(filterAlgorithms.size() + matchAlgorithms.size());
        result.addAll(getAllMatchAlgorithms());
        result.addAll(getAllFilterAlgorithms());
        return result;
    }

    public Collection<CombinationAlgorithm> getAllCombinationAlgorithms() {
        return combinationAlgorithms.values();
    }

    /**
     * Browse through pluginDirectories to check if new algorithms are
     * available of if known algorithms have changed or were removed.
     */
    public void checkForAlgorithms() throws IOException, SQLException {
        Logger.getLogger(MainWindow.class.getName()).log(Level.INFO, "Checking plugin files...");
        matchAlgorithms = new HashMap<String, MatchAlgorithm>();
        filterAlgorithms = new HashMap<String, FilterAlgorithm>();
        Collection<MatchAlgorithm> matchAlgorithmCollection = DataManager.getInstance().getAllMatchAlgorithms();
        for (MatchAlgorithm a : matchAlgorithmCollection) {
            matchAlgorithms.put(a.getPersistentIdentifier(), a);
        }
        Collection<FilterAlgorithm> filterAlgorithmCollection = DataManager.getInstance().getAllFilterAlgorithms();
        for (FilterAlgorithm a : filterAlgorithmCollection) {
            filterAlgorithms.put(a.getPersistentIdentifier(), a);
        }
        if (filterAlgorithms.size() == 0) {
            filterAlgorithms = new HashMap<String, FilterAlgorithm>(6);
            {
                FilterAlgorithm f;
                f = new FilterVertexCount();
                if (f.getApplicableTypes().contains(DataManager.getInstance().getDatabaseMode())) {
                    filterAlgorithms.put(f.getPersistentIdentifier(), f);
                }
                f = new FilterTags();
                if (f.getApplicableTypes().contains(DataManager.getInstance().getDatabaseMode())) {
                    filterAlgorithms.put(f.getPersistentIdentifier(), f);
                }
                f = new FilterTimestamp();
                if (f.getApplicableTypes().contains(DataManager.getInstance().getDatabaseMode())) {
                    filterAlgorithms.put(f.getPersistentIdentifier(), f);
                }
                f = new FilterSource();
                if (f.getApplicableTypes().contains(DataManager.getInstance().getDatabaseMode())) {
                    filterAlgorithms.put(f.getPersistentIdentifier(), f);
                }
                f = new FilterExistingDescriptor();
                if (f.getApplicableTypes().contains(DataManager.getInstance().getDatabaseMode())) {
                    filterAlgorithms.put(f.getPersistentIdentifier(), f);
                }
            }
            Logger.getLogger(ParametersSetting.class.getName()).log(Level.INFO, " ==> adding filter algorithms to database");
            for (FilterAlgorithm f : filterAlgorithms.values()) {
                DataManager dm = DataManager.getInstance();
                int key = dm.addAlgorithm(f);
                f.setPrimaryKey(key);
            }
        }
        List<ChangedAlgorithmInfo> changed = new Vector<ChangedAlgorithmInfo>();
        HashMap<String, MatchAlgorithm> mapFileAlgorithm = new HashMap<String, MatchAlgorithm>(matchAlgorithms.size());
        for (MatchAlgorithm a : matchAlgorithms.values()) {
            mapFileAlgorithm.put(a.getFile().getCanonicalPath(), a);
        }
        Set<String> scannedDirectories = new HashSet<String>(pluginDirectories.size());
        ProcessDirectory: for (String path : pluginDirectories) {
            File dir = new File(path);
            if (!dir.canRead()) {
                throw new IOException("Cannot read `" + dir.getPath() + "'.");
            }
            if (!dir.isDirectory()) {
                throw new IOException("`" + dir.getPath() + "' is not a directory.");
            }
            if (scannedDirectories.contains(dir.getCanonicalPath())) {
                Logger.getLogger(ParametersSetting.class.getName()).log(Level.INFO, "Directory `" + dir.getPath() + "' is contained in plugins direcotry listing twice.");
                continue ProcessDirectory;
            }
            scannedDirectories.add(dir.getCanonicalPath());
            ProcessFile: for (File f : dir.listFiles()) {
                if (f.isDirectory()) {
                    continue ProcessFile;
                }
                if ((!NativePluginConnector.knownFileType(f.getName()) || !NativePluginConnector.getInstance().isEnabled()) && !JavaPluginConnector.knownFileType(f.getName())) {
                    Logger.getLogger(ParametersSetting.class.getName()).log(Level.INFO, " ==> Skipping file `" + f.getPath() + "': Not an allowed plugin file.");
                    continue ProcessFile;
                }
                MatchAlgorithm aOld = mapFileAlgorithm.get(f.getCanonicalPath());
                byte[] newHash = computeHash(f);
                if (aOld != null) {
                    mapFileAlgorithm.remove(f.getCanonicalPath());
                    if (aOld.checkHash(newHash)) {
                        try {
                            aOld.initialize();
                        } catch (Exception e) {
                            Logger.getLogger(ParametersSetting.class.getName()).log(Level.WARNING, "Could not initalize plugin `" + f.getPath() + "'", e);
                            Logger.getLogger(ParametersSetting.class.getName()).log(Level.INFO, " ==> Skipping file `" + f.getPath() + "': Unexpected errors initializing plugin.");
                            mapFileAlgorithm.put(aOld.getFile().getCanonicalPath(), aOld);
                        }
                        continue ProcessFile;
                    }
                }
                MatchAlgorithm aNew;
                if (JavaPluginConnector.knownFileType(f.getPath())) {
                    aNew = new JavaAlgorithm(f.getPath(), newHash);
                } else if (NativePluginConnector.knownFileType(f.getPath())) {
                    aNew = new NativeAlgorithm(f.getPath(), newHash);
                } else {
                    Logger.getLogger(ParametersSetting.class.getName()).log(Level.INFO, " ==> Skipping file `" + f.getPath() + "': Unknown plugin file extension.");
                    continue ProcessFile;
                }
                try {
                    aNew.initialize();
                } catch (Exception e) {
                    Logger.getLogger(ParametersSetting.class.getName()).log(Level.FINE, "Failed to initiaize file `" + f.getPath() + "'", e);
                    if (aOld != null) {
                        Logger.getLogger(ParametersSetting.class.getName()).log(Level.INFO, "New version of `" + f.getPath() + "' could not be initialized. Will be hadled as lost plugin.");
                        mapFileAlgorithm.put(aOld.getFile().getCanonicalPath(), aOld);
                    }
                    Logger.getLogger(ParametersSetting.class.getName()).log(Level.INFO, " ==> Skipping file `" + f.getPath() + "': Invalid smart plugin (" + e.getMessage() + ").");
                    continue ProcessFile;
                }
                if (!aNew.getApplicableTypes().contains(DataManager.getInstance().getDatabaseMode())) {
                    if (aOld != null) {
                        Logger.getLogger(ParametersSetting.class.getName()).log(Level.INFO, "Old version of `" + f.getPath() + "' did support current database mode `" + DataManager.getInstance().getDatabaseMode() + "', but new version does not. Will be hadled as lost plugin.");
                        mapFileAlgorithm.put(aOld.getFile().getCanonicalPath(), aOld);
                    }
                    Logger.getLogger(ParametersSetting.class.getName()).log(Level.INFO, " ==> Skipping file `" + f.getPath() + "': current database mode `" + DataManager.getInstance().getDatabaseMode() + "' not supported.");
                    continue ProcessFile;
                }
                changed.add(new ChangedAlgorithmInfo(aOld, aNew));
            }
        }
        if (!mapFileAlgorithm.isEmpty()) {
            List<LostAlgorithmInfo> lostAlgorithms = new Vector<LostAlgorithmInfo>(mapFileAlgorithm.size());
            for (MatchAlgorithm a : mapFileAlgorithm.values()) {
                lostAlgorithms.add(new LostAlgorithmInfo(a));
            }
            net.sf.wwusmart.gui.GuiController.getInstance().reportLostAlgorithms(lostAlgorithms);
            for (LostAlgorithmInfo lai : lostAlgorithms) {
                if (lai.isDelete()) {
                    DataManager.getInstance().deleteAlgorithm(lai.getAlgorithm());
                }
                matchAlgorithms.remove(lai.getAlgorithm().getPersistentIdentifier());
            }
            mapFileAlgorithm.values().clear();
        }
        if (!changed.isEmpty()) {
            processChangedAlgorithms(changed);
        }
    }

    private void processChangedAlgorithms(List<ChangedAlgorithmInfo> changed) throws SQLException {
        DataManager dm = DataManager.getInstance();
        GuiController.getInstance().askForNewAlgorithms(changed);
        List<DescriptorAlgorithm> computeList = new LinkedList<DescriptorAlgorithm>();
        for (ChangedAlgorithmInfo c : changed) {
            MatchAlgorithm aOld = c.getOld();
            MatchAlgorithm aNew = c.getNew();
            switch(c.getAction()) {
                case COMPUTE_DESCRIPTORS:
                    if (c.isNew()) {
                        int key = dm.addAlgorithm(aNew);
                        aNew.setPrimaryKey(key);
                        matchAlgorithms.put(aNew.getPersistentIdentifier(), aNew);
                    } else {
                        aNew.setPrimaryKey(aOld.getPrimaryKey());
                        dm.deleteDescriptors(aOld);
                        matchAlgorithms.remove(aOld.getPersistentIdentifier());
                        dm.setAlgorithm(aNew);
                        matchAlgorithms.put(aNew.getPersistentIdentifier(), aNew);
                    }
                    computeList.add(aNew);
                    break;
                case KEEP_OLD_DESCRIPTORS:
                    aNew.setPrimaryKey(aOld.getPrimaryKey());
                    dm.setAlgorithm(aNew);
                    matchAlgorithms.remove(aOld.getPersistentIdentifier());
                    matchAlgorithms.put(aNew.getPersistentIdentifier(), aNew);
                    break;
                case SKIP:
                    if (aOld != null) {
                        matchAlgorithms.remove(aOld.getPersistentIdentifier());
                    }
                    break;
            }
        }
        if (!computeList.isEmpty() && dm.getTotalNumberOfShapes() > 0) {
            try {
                computeDescriptors(computeList, null);
            } catch (UserAbortionException ex) {
                Logger.getLogger(AlgorithmManager.class.getName()).log(Level.FINE, "Descriptor computation aborted by user.");
            }
        }
    }

    /**
     * Will compute all descriptors not currently in the database
     * for all shapes in shapeList
     * for all algorithms in algoList.
     *
     * @param algoList List of algorithms to compute the descriptors for,
     * pass null here to select all algorithms in database.
     * @param shapeList List of shapes to compute the descriptors for,
     * pass null here to select all shapes in database.
     *
     * @throws java.sql.SQLException if DataManager has an exeception when accessing the database
     * @throws UserAbortedException if user aborts computation
     */
    public void computeDescriptors(Collection<DescriptorAlgorithm> algoList, Collection<Shape> shapeList) throws SQLException, UserAbortionException {
        computeDescriptors(algoList, shapeList, null);
    }

    /**
     * Will compute all descriptors not currently in the database
     * for all shapes in shapeList
     * for all algorithms in algoList.
     *
     * @param algoList List of algorithms to compute the descriptors for,
     * pass null here to select all algorithms in database.
     * @param shapeList List of shapes to compute the descriptors for,
     * pass null here to select all shapes in database.
     * @param algorithmManagementPanel If an {@linkplain AlgorithmManagementPanel} is beeing shown,
     * pass a reference here to let the computation thread update panel content
     * when computation has been finished. Pass null otherwise.
     *
     * @throws java.sql.SQLException if DataManager has an exeception when accessing the database
     * @throws UserAbortedException if user aborts computation
     */
    public void computeDescriptors(Collection<DescriptorAlgorithm> algoList, Collection<Shape> shapeList, AlgorithmManagementPanel algorithmManagementPanel) throws SQLException, UserAbortionException {
        DataManager dm = DataManager.getInstance();
        boolean allAlgorithms = algoList == null;
        boolean allShapes = (shapeList == null) || (shapeList.size() == dm.getTotalNumberOfShapes());
        int totalShapesToDo = 0;
        if (allAlgorithms) {
            algoList = AlgorithmManager.getInstance().getAllDescriptorAlgorithms();
        }
        List<Couple<DescriptorAlgorithm, Couple<Collection<Shape>, Integer>>> algorithmShapelistNumberdone = new Vector<Couple<DescriptorAlgorithm, Couple<Collection<Shape>, Integer>>>(algoList.size());
        if (allShapes) {
            for (DescriptorAlgorithm algo : algoList) {
                Collection<Shape> shapesToDo = dm.getShapesWithNoncomputedDescriptors(algo);
                if (shapesToDo.size() == 0) {
                    continue;
                }
                algorithmShapelistNumberdone.add(new Couple(algo, new Couple(shapesToDo, new Integer(0))));
                totalShapesToDo += shapesToDo.size();
            }
        } else {
            for (DescriptorAlgorithm algo : algoList) {
                Collection<Shape> shapesToDo = dm.getShapesWithNoncomputedDescriptors(algo);
                shapesToDo.retainAll(shapeList);
                if (shapesToDo.size() == 0) {
                    continue;
                }
                algorithmShapelistNumberdone.add(new Couple(algo, new Couple(shapesToDo, new Integer(0))));
                totalShapesToDo += shapesToDo.size();
            }
        }
        computeDescriptorsRunComputation(algorithmShapelistNumberdone, totalShapesToDo, algorithmManagementPanel);
    }

    private void computeDescriptorsRunComputation(final List<Couple<DescriptorAlgorithm, Couple<Collection<Shape>, Integer>>> algorithmShapelistNumberdone, final int totalShapesToDo, final AlgorithmManagementPanel algorithmManagementPanel) throws UserAbortionException {
        if (algorithmShapelistNumberdone.size() == 0) {
            Logger.getLogger(AlgorithmManager.class.getName()).log(Level.FINEST, "computeDescriptor called, but none of the specified descriptors needs to be computed.");
            return;
        }
        final ComputeDescriptorsDialog showProgress = GuiController.getInstance().createComputationProgress(algorithmShapelistNumberdone, totalShapesToDo);
        Thread job = new Thread() {

            private static final int SLEEP_MILLI_SECS = 100;

            @Override
            public void run() {
                while (!showProgress.isReady()) {
                    try {
                        sleep(SLEEP_MILLI_SECS);
                    } catch (InterruptedException ex) {
                    }
                }
                DataManager dm = DataManager.getInstance();
                compute: for (Couple<DescriptorAlgorithm, Couple<Collection<Shape>, Integer>> a : algorithmShapelistNumberdone) {
                    for (Shape s : a.getB().getA()) {
                        try {
                            dm.getDescriptor(a.getA(), s);
                        } catch (SQLException ex) {
                            Logger.getLogger(AlgorithmManager.class.getName()).log(Level.SEVERE, "Couldn't compute descriptor of shape `" + s.getName() + "' for algorithm `" + a.getA().getName() + "'.", ex);
                            break compute;
                        }
                        showProgress.nextShape();
                        if (showProgress.isAborted()) {
                            break compute;
                        }
                    }
                    if (showProgress.isAborted()) {
                        break compute;
                    }
                    showProgress.nextAlgorithm();
                }
                showProgress.setVisible(false);
                showProgress.dispose();
                if (algorithmManagementPanel != null) {
                    algorithmManagementPanel.update();
                }
            }
        };
        job.start();
        showProgress.setVisible(true);
        if (showProgress.isAborted()) {
            throw new UserAbortionException();
        }
    }

    /**
     * Clears lists of known algorithms. This method must be called when closing
     * the connection to algo database to make sure algorithms can be
     * loaded again on successive database connections.
     * We definitely want to reload the algorithms (instead of holding them in
     * memory even when closing the database to avoid the overhead of loading
     * the same library file more than once) to make sure assocciation of
     * algorithm object and database algorithm primary key is correct.
     */
    public void revokeAllAlgorithms() {
        matchAlgorithms.clear();
        filterAlgorithms.clear();
        NativePluginConnector.getInstance().revokeAllAlgorithms();
    }

    private static byte[] computeHash(String filename) throws IOException {
        File f = new File(filename);
        return computeHash(f);
    }

    private static byte[] computeHash(File f) throws IOException {
        MessageDigest messagedigest;
        try {
            messagedigest = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("This sould not have happend -- a very strange error must have occured.");
        }
        byte[] md = new byte[8192];
        InputStream in = new FileInputStream(f);
        int n = 0;
        while ((n = in.read(md)) > -1) {
            messagedigest.update(md, 0, n);
        }
        return messagedigest.digest();
    }

    public List<String> getPluginDirectories() {
        return pluginDirectories;
    }

    public void setPluginDirectories(List<String> val) {
        pluginDirectories = val;
    }

    public void addPluginDirectory(String path) {
        pluginDirectories.add(path);
    }

    public static AlgorithmManager getInstance() {
        return instance;
    }
}
