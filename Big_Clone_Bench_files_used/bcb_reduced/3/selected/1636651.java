package com.tensegrity.palojava.http;

import java.io.IOException;
import java.net.ConnectException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import com.tensegrity.palojava.CellInfo;
import com.tensegrity.palojava.ConnectionInfo;
import com.tensegrity.palojava.CubeInfo;
import com.tensegrity.palojava.DatabaseInfo;
import com.tensegrity.palojava.DbConnection;
import com.tensegrity.palojava.DimensionInfo;
import com.tensegrity.palojava.ElementInfo;
import com.tensegrity.palojava.ExportContextInfo;
import com.tensegrity.palojava.HierarchyInfo;
import com.tensegrity.palojava.LockInfo;
import com.tensegrity.palojava.PaloException;
import com.tensegrity.palojava.PaloInfo;
import com.tensegrity.palojava.PropertyInfo;
import com.tensegrity.palojava.RuleInfo;
import com.tensegrity.palojava.ServerInfo;
import com.tensegrity.palojava.events.ServerEvent;
import com.tensegrity.palojava.events.ServerListener;
import com.tensegrity.palojava.http.handlers.CellHandler;
import com.tensegrity.palojava.http.handlers.CubeHandler;
import com.tensegrity.palojava.http.handlers.DatabaseHandler;
import com.tensegrity.palojava.http.handlers.DimensionHandler;
import com.tensegrity.palojava.http.handlers.ElementHandler;
import com.tensegrity.palojava.http.handlers.HandlerRegistry;
import com.tensegrity.palojava.http.handlers.RuleHandler;
import com.tensegrity.palojava.http.handlers.ServerHandler;
import com.tensegrity.palojava.http.loader.HttpCubeLoader;
import com.tensegrity.palojava.http.loader.HttpDatabaseLoader;
import com.tensegrity.palojava.http.loader.HttpDimensionLoader;
import com.tensegrity.palojava.http.loader.HttpElementLoader;
import com.tensegrity.palojava.http.loader.HttpFunctionLoader;
import com.tensegrity.palojava.http.loader.HttpHierarchyLoader;
import com.tensegrity.palojava.http.loader.HttpPropertyLoader;
import com.tensegrity.palojava.http.loader.HttpRuleLoader;
import com.tensegrity.palojava.impl.ConnectionInfoImpl;
import com.tensegrity.palojava.impl.PropertyInfoImpl;
import com.tensegrity.palojava.loader.CubeLoader;
import com.tensegrity.palojava.loader.DatabaseLoader;
import com.tensegrity.palojava.loader.DimensionLoader;
import com.tensegrity.palojava.loader.ElementLoader;
import com.tensegrity.palojava.loader.FunctionLoader;
import com.tensegrity.palojava.loader.HierarchyLoader;
import com.tensegrity.palojava.loader.PaloInfoLoader;
import com.tensegrity.palojava.loader.PropertyLoader;
import com.tensegrity.palojava.loader.RuleLoader;

/**
 * <code></code>
 * TODO DOCUMENT ME
 * 
 * @author ArndHouben
 * @version $Id: HttpConnection.java,v 1.42 2008/10/09 07:31:16 ArndHouben Exp $
 */
public class HttpConnection implements DbConnection {

    private final long SID_RENEWAL_THRESHOLD = 20000;

    private final HandlerRegistry handlerRegistry;

    private final ConnectionInfoImpl connectionInfo;

    private final HttpClient httpClient;

    private final HashSet listeners = new HashSet();

    private final HashMap<String, PaloInfoLoader> loaders = new HashMap<String, PaloInfoLoader>();

    private String sid;

    private int srvToken;

    private long ttl;

    private boolean ownChange;

    /** 
     * used to periodically check for changes within palo server and for a
     * renewal of the server sid
     */
    private Timer httpTimer;

    private final int timeout;

    HttpConnection(String host, String port, int timeout) throws PaloException {
        httpClient = new HttpClient(this);
        connectionInfo = new ConnectionInfoImpl(host, port);
        handlerRegistry = new HandlerRegistry(this);
        this.timeout = timeout;
        try {
            httpClient.reconnect(timeout);
        } catch (IOException ioex) {
            throw new PaloException("Could not connect to palo server at host '" + host + "' on port '" + port + "'", ioex);
        }
    }

    public final boolean supportsRules() {
        return RuleLoader.supportsRules(this);
    }

    public final boolean isConnected() {
        return httpClient.isConnected();
    }

    public final void disconnect() {
        try {
            httpClient.disconnect();
            stopTimer();
        } catch (IOException ioex) {
            throw new PaloException("Could not correctly disconnect from palo server!!", ioex);
        }
    }

    public final ConnectionInfo getInfo() {
        return connectionInfo;
    }

    public final HttpClient getHttpClient() {
        return httpClient;
    }

    public final synchronized String getSID() {
        return sid;
    }

    public final String[] send(String request) throws ConnectException, IOException {
        return httpClient.send(request);
    }

    public final synchronized void setServerToken(int srvToken) {
        if (this.srvToken != srvToken) fireServerEvent(ServerEvent.SERVER_CHANGED);
        this.srvToken = srvToken;
    }

    public final synchronized void addServerListener(ServerListener listener) {
        listeners.add(listener);
    }

    public final synchronized void removeServerListener(ServerListener listener) {
        listeners.remove(listener);
    }

    /**
	 * For <b>intrernal</b> event handling only. 
	 * @param type
	 */
    final synchronized void fireServerEvent(final int type) {
        boolean ownChange = isOwnChange();
        if (type == ServerEvent.SERVER_CHANGED && ownChange) setOwnChange(false);
        if (ownChange) return;
        ServerEvent ev = new ServerEvent() {

            public int getType() {
                return type;
            }
        };
        ArrayList copy = new ArrayList(listeners);
        for (Iterator it = copy.iterator(); it.hasNext(); ) ((ServerListener) it.next()).serverStructureChanged(ev);
    }

    public final boolean login(String user, String pass) {
        if (user == null || pass == null) return false;
        connectionInfo.setData("com.tensegrity.palojava.pass#" + user, pass);
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(pass.getBytes());
            pass = asHexString(md.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new PaloException("Failed to create encrypted password for " + "user '" + user + "'!", ex);
        }
        connectionInfo.setUser(user);
        connectionInfo.setPassword(pass);
        return loginInternal(user, pass);
    }

    public final CubeInfo addCube(DatabaseInfo database, String name, DimensionInfo[] dimensions) {
        try {
            setOwnChange(true);
            CubeHandler cubeHandler = handlerRegistry.getCubeHandler();
            return cubeHandler.create(database, name, dimensions);
        } catch (IOException e) {
            throw new PaloException(e.getLocalizedMessage(), e);
        }
    }

    public final CubeInfo addCube(DatabaseInfo database, String name, DimensionInfo[] dimensions, int type) {
        try {
            setOwnChange(true);
            CubeHandler cubeHandler = handlerRegistry.getCubeHandler();
            return cubeHandler.create(database, name, dimensions, type);
        } catch (IOException e) {
            throw new PaloException(e.getLocalizedMessage(), e);
        }
    }

    public final DatabaseInfo addDatabase(String database) {
        try {
            setOwnChange(true);
            DatabaseHandler dbHandler = handlerRegistry.getDatabaseHandler();
            return dbHandler.create(database);
        } catch (IOException e) {
            throw new PaloException(e.getLocalizedMessage(), e);
        }
    }

    public final DimensionInfo addDimension(DatabaseInfo database, String name) {
        try {
            setOwnChange(true);
            DimensionHandler dimHandler = handlerRegistry.getDimensionHandler();
            return dimHandler.create(database, name);
        } catch (IOException e) {
            throw new PaloException(e.getLocalizedMessage(), e);
        }
    }

    public final DimensionInfo addDimension(DatabaseInfo database, String name, int type) {
        try {
            setOwnChange(true);
            DimensionHandler dimHandler = handlerRegistry.getDimensionHandler();
            return dimHandler.create(database, name, type);
        } catch (IOException e) {
            throw new PaloException(e.getLocalizedMessage(), e);
        }
    }

    public final ElementInfo addElement(DimensionInfo dimension, String name, int type, ElementInfo[] children, double[] weights) {
        try {
            setOwnChange(true);
            ElementHandler elHandler = handlerRegistry.getElementHandler();
            return elHandler.create(dimension, name, type, children, weights);
        } catch (IOException e) {
            throw new PaloException(e.getLocalizedMessage(), e);
        }
    }

    public final ElementInfo addElement(HierarchyInfo hierarchy, String name, int type, ElementInfo[] children, double[] weights) {
        try {
            setOwnChange(true);
            ElementHandler elHandler = handlerRegistry.getElementHandler();
            return elHandler.create(hierarchy.getDimension(), name, type, children, weights);
        } catch (IOException e) {
            throw new PaloException(e.getLocalizedMessage(), e);
        }
    }

    public final void addConsolidations(ElementInfo element, ElementInfo[] children, double[] weights) {
        try {
            setOwnChange(true);
            ElementHandler elHandler = handlerRegistry.getElementHandler();
            elHandler.append(element, children, weights);
        } catch (IOException e) {
            throw new PaloException(e.getLocalizedMessage(), e);
        }
    }

    public final void clear(DimensionInfo dimension) {
        try {
            setOwnChange(true);
            DimensionHandler dimHandler = handlerRegistry.getDimensionHandler();
            dimHandler.clear(dimension);
        } catch (IOException e) {
            throw new PaloException(e.getLocalizedMessage(), e);
        }
    }

    public final void clear(CubeInfo cube) {
        try {
            CubeHandler cubeHandler = handlerRegistry.getCubeHandler();
            cubeHandler.clear(cube, null, true);
        } catch (IOException e) {
            throw new PaloException(e.getLocalizedMessage(), e);
        }
    }

    public final void clear(CubeInfo cube, ElementInfo[][] area) {
        try {
            CubeHandler cubeHandler = handlerRegistry.getCubeHandler();
            cubeHandler.clear(cube, area, false);
        } catch (IOException e) {
            throw new PaloException(e.getLocalizedMessage(), e);
        }
    }

    public final boolean delete(ElementInfo element) {
        try {
            setOwnChange(true);
            ElementHandler elHandler = handlerRegistry.getElementHandler();
            return elHandler.destroy(element);
        } catch (IOException e) {
            throw new PaloException(e.getLocalizedMessage(), e);
        }
    }

    public final boolean delete(CubeInfo cube) {
        try {
            setOwnChange(true);
            CubeHandler cubeHandler = handlerRegistry.getCubeHandler();
            return cubeHandler.destroy(cube);
        } catch (IOException e) {
            throw new PaloException(e.getLocalizedMessage(), e);
        }
    }

    public final boolean delete(DatabaseInfo database) {
        try {
            setOwnChange(true);
            DatabaseHandler dbHandler = handlerRegistry.getDatabaseHandler();
            return dbHandler.destroy(database);
        } catch (IOException e) {
            throw new PaloException(e.getLocalizedMessage(), e);
        }
    }

    public final boolean delete(DimensionInfo dimension) {
        try {
            setOwnChange(true);
            DimensionHandler dimHandler = handlerRegistry.getDimensionHandler();
            return dimHandler.delete(dimension);
        } catch (IOException e) {
            throw new PaloException(e.getLocalizedMessage(), e);
        }
    }

    public final CubeInfo[] getCubes(DatabaseInfo database) {
        try {
            DatabaseHandler dbHandler = handlerRegistry.getDatabaseHandler();
            return dbHandler.getAllCubes(database);
        } catch (IOException e) {
            throw new PaloException(e.getLocalizedMessage(), e);
        }
    }

    public final CubeInfo[] getCubes(DatabaseInfo database, int typeMask) {
        try {
            DatabaseHandler dbHandler = handlerRegistry.getDatabaseHandler();
            return dbHandler.getCubes(database, typeMask);
        } catch (IOException e) {
            throw new PaloException(e.getLocalizedMessage(), e);
        }
    }

    public final CubeInfo[] getCubes(DimensionInfo dimension) {
        try {
            DimensionHandler dimHandler = handlerRegistry.getDimensionHandler();
            return dimHandler.getCubes(dimension);
        } catch (IOException e) {
            throw new PaloException(e.getLocalizedMessage(), e);
        }
    }

    public final CellInfo getData(CubeInfo cube, ElementInfo[] coordinate) {
        try {
            CellHandler cellHandler = handlerRegistry.getCellHandler();
            return cellHandler.getValue(cube, coordinate);
        } catch (IOException e) {
            throw new PaloException("Failed to receive cell data from cube: " + cube.getName(), e);
        }
    }

    public final CellInfo[] getDataArea(CubeInfo cube, ElementInfo[][] coordinates) {
        try {
            CellHandler cellHandler = handlerRegistry.getCellHandler();
            return cellHandler.getCellArea(cube, coordinates);
        } catch (IOException e) {
            throw new PaloException(e.getLocalizedMessage(), e);
        }
    }

    public final CellInfo[] getDataArray(CubeInfo cube, ElementInfo[][] coordinates) {
        try {
            CellHandler cellHandler = handlerRegistry.getCellHandler();
            return cellHandler.getValues(cube, coordinates);
        } catch (IOException e) {
            throw new PaloException(e.getLocalizedMessage(), e);
        }
    }

    public final CellInfo[] getDataExport(CubeInfo cube, ExportContextInfo exportContext) {
        try {
            CellHandler cellHandler = handlerRegistry.getCellHandler();
            return cellHandler.export(cube, exportContext);
        } catch (IOException e) {
            throw new PaloException(e.getLocalizedMessage(), e);
        }
    }

    public final DatabaseInfo[] getDatabases() {
        try {
            ServerHandler srvHandler = handlerRegistry.getServerHandler();
            return srvHandler.getDatabases();
        } catch (IOException e) {
            throw new PaloException(e.getLocalizedMessage(), e);
        }
    }

    public final DimensionInfo[] getDimensions(DatabaseInfo database) {
        try {
            DatabaseHandler dbHandler = handlerRegistry.getDatabaseHandler();
            if (database.getType() == DatabaseInfo.TYPE_SYSTEM) return dbHandler.getDimensions(database, 1 << 2);
            return dbHandler.getAllDimensions(database);
        } catch (IOException e) {
            throw new PaloException(e.getLocalizedMessage(), e);
        }
    }

    public HierarchyInfo[] getHierarchies(DimensionInfo dim) {
        return new HierarchyInfo[] { dim.getDefaultHierarchy() };
    }

    public HierarchyInfo getHierarchy(DimensionInfo dimension, String id) {
        return dimension.getDefaultHierarchy();
    }

    public final ElementInfo getElementAt(DimensionInfo dimension, int position) {
        try {
            DimensionHandler dimHandler = handlerRegistry.getDimensionHandler();
            return dimHandler.getElementAt(dimension, position);
        } catch (IOException e) {
            throw new PaloException(e.getLocalizedMessage(), e);
        }
    }

    public final ElementInfo getElementAt(HierarchyInfo hierarchy, int position) {
        try {
            DimensionHandler dimHandler = handlerRegistry.getDimensionHandler();
            return dimHandler.getElementAt(hierarchy.getDimension(), position);
        } catch (IOException e) {
            throw new PaloException(e.getLocalizedMessage(), e);
        }
    }

    public final ElementInfo[] getElements(DimensionInfo dimension) {
        try {
            DimensionHandler dimHandler = handlerRegistry.getDimensionHandler();
            return dimHandler.getElements(dimension);
        } catch (IOException e) {
            throw new PaloException(e.getLocalizedMessage(), e);
        }
    }

    public final ElementInfo[] getElements(HierarchyInfo hierarchy) {
        try {
            DimensionHandler dimHandler = handlerRegistry.getDimensionHandler();
            return dimHandler.getElements(hierarchy.getDimension());
        } catch (IOException e) {
            throw new PaloException(e.getLocalizedMessage(), e);
        }
    }

    public final DimensionInfo[] getDimensions(DatabaseInfo database, int typeMask) {
        try {
            DatabaseHandler dbHandler = handlerRegistry.getDatabaseHandler();
            return dbHandler.getDimensions(database, typeMask);
        } catch (IOException e) {
            throw new PaloException(e.getLocalizedMessage(), e);
        }
    }

    public final ServerInfo getServerInfo() {
        try {
            ServerHandler srvHandler = handlerRegistry.getServerHandler();
            return srvHandler.getInfo();
        } catch (IOException e) {
            throw new PaloException(e.getLocalizedMessage(), e);
        }
    }

    public final void load(CubeInfo cube) {
        try {
            CubeHandler cubeHandler = handlerRegistry.getCubeHandler();
            cubeHandler.load(cube);
        } catch (IOException e) {
            throw new PaloException(e.getLocalizedMessage(), e);
        }
    }

    public final void load(DatabaseInfo database) {
        try {
            DatabaseHandler dbHandler = handlerRegistry.getDatabaseHandler();
            dbHandler.load(database);
        } catch (IOException e) {
            throw new PaloException(e.getLocalizedMessage(), e);
        }
    }

    public final void move(ElementInfo element, int newPosition) {
        try {
            setOwnChange(true);
            ElementHandler elHandler = handlerRegistry.getElementHandler();
            elHandler.move(element, newPosition);
        } catch (IOException e) {
            throw new PaloException(e.getLocalizedMessage(), e);
        }
    }

    public final String parseRule(CubeInfo cube, String ruleDefinition, String functions) {
        try {
            RuleHandler ruleHandler = handlerRegistry.getRuleHandler();
            return ruleHandler.parse(cube, ruleDefinition, functions);
        } catch (IOException e) {
            throw new PaloException(e.getLocalizedMessage(), e);
        }
    }

    public final void reload(CubeInfo cube) {
        try {
            CubeHandler cubeHandler = handlerRegistry.getCubeHandler();
            cubeHandler.reload(cube);
        } catch (IOException e) {
            throw new PaloException(e.getLocalizedMessage(), e);
        }
    }

    public final void reload(DatabaseInfo database) {
        try {
            DatabaseHandler dbHandler = handlerRegistry.getDatabaseHandler();
            dbHandler.reload(database);
        } catch (IOException e) {
            throw new PaloException(e.getLocalizedMessage(), e);
        }
    }

    public final void reload(DimensionInfo dimension) {
        try {
            DimensionHandler dimHandler = handlerRegistry.getDimensionHandler();
            dimHandler.reload(dimension);
        } catch (IOException e) {
            throw new PaloException(e.getLocalizedMessage(), e);
        }
    }

    public final void reload(ElementInfo element) {
        try {
            ElementHandler elHandler = handlerRegistry.getElementHandler();
            elHandler.reload(element);
        } catch (IOException e) {
            throw new PaloException(e.getLocalizedMessage(), e);
        }
    }

    public final void rename(ElementInfo element, String newName) {
        try {
            setOwnChange(true);
            ElementHandler elHandler = handlerRegistry.getElementHandler();
            elHandler.rename(element, newName);
        } catch (IOException e) {
            throw new PaloException(e.getLocalizedMessage(), e);
        }
    }

    public final void rename(DimensionInfo dimension, String newName) {
        try {
            setOwnChange(true);
            DimensionHandler dimHandler = handlerRegistry.getDimensionHandler();
            dimHandler.rename(dimension, newName);
        } catch (IOException e) {
            throw new PaloException(e.getLocalizedMessage(), e);
        }
    }

    public final void rename(CubeInfo cube, String newName) {
        try {
            setOwnChange(true);
            CubeHandler cubeHandler = handlerRegistry.getCubeHandler();
            cubeHandler.rename(cube, newName);
        } catch (IOException e) {
            throw new PaloException(e.getLocalizedMessage(), e);
        }
    }

    public final boolean save(CubeInfo cube) {
        try {
            setOwnChange(true);
            CubeHandler cubeHandler = handlerRegistry.getCubeHandler();
            return cubeHandler.save(cube);
        } catch (IOException e) {
            throw new PaloException(e.getLocalizedMessage(), e);
        }
    }

    public final boolean save(DatabaseInfo database) {
        try {
            setOwnChange(true);
            DatabaseHandler dbHandler = handlerRegistry.getDatabaseHandler();
            return dbHandler.save(database);
        } catch (IOException e) {
            throw new PaloException(e.getLocalizedMessage(), e);
        }
    }

    public final boolean save(ServerInfo server) {
        try {
            setOwnChange(true);
            ServerHandler srvHandler = handlerRegistry.getServerHandler();
            return srvHandler.save();
        } catch (IOException e) {
            throw new PaloException(e.getLocalizedMessage(), e);
        }
    }

    public final void setDataArray(CubeInfo cube, ElementInfo[][] coordinates, Object[] values, boolean add, int splashMode, boolean notifyEventProcessors) {
        try {
            CellHandler cellHandler = handlerRegistry.getCellHandler();
            cellHandler.replaceValues(cube, coordinates, values, add, splashMode, notifyEventProcessors);
        } catch (IOException e) {
            throw new PaloException(e.getLocalizedMessage(), e);
        }
    }

    public final void setDataNumericSplashed(CubeInfo cube, ElementInfo[] coordinate, double value, int splashMode) {
        try {
            CellHandler cellHandler = handlerRegistry.getCellHandler();
            cellHandler.replaceValue(cube, coordinate, new Double(value), splashMode);
        } catch (IOException e) {
            throw new PaloException(e.getLocalizedMessage(), e);
        }
    }

    public final void setDataString(CubeInfo cube, ElementInfo[] coordinate, String value) {
        try {
            CellHandler cellHandler = handlerRegistry.getCellHandler();
            cellHandler.replaceValue(cube, coordinate, value, CellInfo.SPLASH_MODE_DISABLED);
        } catch (IOException e) {
            throw new PaloException(e.getLocalizedMessage(), e);
        }
    }

    public final void unload(CubeInfo cube) {
        try {
            CubeHandler cubeHandler = handlerRegistry.getCubeHandler();
            cubeHandler.unload(cube);
        } catch (IOException e) {
            throw new PaloException(e.getLocalizedMessage(), e);
        }
    }

    public final void update(ElementInfo element, int type, String[] children, double[] weights) {
        try {
            setOwnChange(true);
            ElementHandler elHandler = handlerRegistry.getElementHandler();
            elHandler.update(element, type, children, weights);
        } catch (IOException e) {
            throw new PaloException(e.getLocalizedMessage(), e);
        }
    }

    public void update(RuleInfo rule, String definition, String externalIdentifier, boolean useIt, String comment) {
        try {
            setOwnChange(true);
            RuleHandler ruleHandler = handlerRegistry.getRuleHandler();
            ruleHandler.update(rule, definition, externalIdentifier, useIt, comment);
        } catch (IOException e) {
            throw new PaloException(e.getLocalizedMessage(), e);
        }
    }

    public final void ping() throws PaloException {
        try {
            ServerHandler srvHandler = handlerRegistry.getServerHandler();
            srvHandler.getInfo();
        } catch (IOException e) {
            throw new PaloException("Connection lost!! Maybe palo server is down.");
        }
    }

    public final String listFunctions() {
        try {
            RuleHandler ruleHandler = handlerRegistry.getRuleHandler();
            return ruleHandler.listFunctions();
        } catch (IOException e) {
            throw new PaloException(e.getLocalizedMessage(), e);
        }
    }

    public final RuleInfo createRule(CubeInfo cube, String definition) {
        try {
            setOwnChange(true);
            RuleHandler ruleHandler = handlerRegistry.getRuleHandler();
            return ruleHandler.create(cube, definition);
        } catch (IOException e) {
            throw new PaloException(e.getLocalizedMessage(), e);
        }
    }

    public final RuleInfo createRule(CubeInfo cube, String definition, String externalIdentifier, boolean useIt, String comment) {
        try {
            setOwnChange(true);
            RuleHandler ruleHandler = handlerRegistry.getRuleHandler();
            return ruleHandler.create(cube, definition, externalIdentifier, useIt, comment);
        } catch (IOException e) {
            throw new PaloException(e.getLocalizedMessage(), e);
        }
    }

    public final boolean delete(RuleInfo rule) {
        try {
            setOwnChange(true);
            RuleHandler ruleHandler = handlerRegistry.getRuleHandler();
            return ruleHandler.delete(rule);
        } catch (IOException e) {
            throw new PaloException(e.getLocalizedMessage(), e);
        }
    }

    public final boolean delete(String ruleId, CubeInfo cube) {
        try {
            setOwnChange(true);
            RuleHandler ruleHandler = handlerRegistry.getRuleHandler();
            return ruleHandler.delete(ruleId, cube);
        } catch (IOException e) {
            throw new PaloException(e.getLocalizedMessage(), e);
        }
    }

    public final RuleInfo[] getRules(CubeInfo cube) {
        try {
            CubeHandler cubeHandler = handlerRegistry.getCubeHandler();
            return cubeHandler.getRules(cube);
        } catch (PaloException pe) {
            return new RuleInfo[0];
        } catch (IOException e) {
            throw new PaloException(e.getLocalizedMessage(), e);
        }
    }

    public final String getRule(CubeInfo cube, ElementInfo[] coordinate) {
        try {
            CellHandler cellHandler = handlerRegistry.getCellHandler();
            return cellHandler.getRule(cube, coordinate);
        } catch (IOException e) {
            throw new PaloException("Failed to receive rule for cell from cube: " + cube.getName(), e);
        }
    }

    public final PropertyInfo getProperty(String id) {
        throw new RuntimeException("Currently not supported.");
    }

    public final RuleInfo getRule(CubeInfo cube, String id) {
        try {
            RuleHandler ruleHandler = handlerRegistry.getRuleHandler();
            return ruleHandler.getInfo(cube, id);
        } catch (IOException e) {
            throw new PaloException(e.getLocalizedMessage(), e);
        }
    }

    public final CubeInfo getAttributeCube(DimensionInfo dimension) {
        try {
            String attrCubeId = dimension.getAttributeCube();
            if (attrCubeId == null || attrCubeId.equals("")) return null;
            CubeHandler cubeHandler = handlerRegistry.getCubeHandler();
            return cubeHandler.getAttributeCube(dimension);
        } catch (IOException e) {
            throw new PaloException(e.getLocalizedMessage(), e);
        }
    }

    public final DimensionInfo getAttributeDimension(DimensionInfo dimension) {
        try {
            String attrId = dimension.getAttributeDimension();
            if (attrId == null || attrId.equals("")) return null;
            DimensionHandler dimHandler = handlerRegistry.getDimensionHandler();
            return dimHandler.getAttributeDimension(dimension);
        } catch (IOException e) {
            throw new PaloException(e.getLocalizedMessage(), e);
        }
    }

    public final DatabaseInfo getDatabase(String id) {
        try {
            DatabaseHandler dbHandler = handlerRegistry.getDatabaseHandler();
            return dbHandler.getInfo(id);
        } catch (IOException e) {
            throw new PaloException(e.getLocalizedMessage(), e);
        }
    }

    public final CubeInfo getCube(DatabaseInfo database, String id) {
        try {
            CubeHandler cubeHandler = handlerRegistry.getCubeHandler();
            return cubeHandler.getInfo(database, id);
        } catch (IOException e) {
            throw new PaloException(e.getLocalizedMessage(), e);
        }
    }

    public DimensionInfo getDimension(DatabaseInfo database, String id) {
        try {
            DimensionHandler dimHandler = handlerRegistry.getDimensionHandler();
            return dimHandler.getInfo(database, id);
        } catch (IOException e) {
            throw new PaloException(e.getLocalizedMessage(), e);
        }
    }

    public ElementInfo getElement(DimensionInfo dimension, String id) {
        try {
            ElementHandler elHandler = handlerRegistry.getElementHandler();
            return elHandler.getInfo(dimension, id);
        } catch (IOException e) {
            throw new PaloException(e.getLocalizedMessage(), e);
        }
    }

    public ElementInfo getElement(HierarchyInfo hierarchy, String id) {
        try {
            ElementHandler elHandler = handlerRegistry.getElementHandler();
            return elHandler.getInfo(hierarchy.getDimension(), id);
        } catch (IOException e) {
            throw new PaloException(e.getLocalizedMessage(), e);
        }
    }

    public final LockInfo[] getLocks(CubeInfo cube) {
        try {
            CubeHandler cubeHandler = handlerRegistry.getCubeHandler();
            return cubeHandler.listLocks(cube);
        } catch (IOException e) {
            throw new PaloException(e.getLocalizedMessage(), e);
        }
    }

    public final LockInfo requestLock(CubeInfo cube, ElementInfo[][] area) {
        try {
            CubeHandler cubeHandler = handlerRegistry.getCubeHandler();
            return cubeHandler.requestLock(cube, area);
        } catch (IOException e) {
        }
        return null;
    }

    public final boolean rollback(CubeInfo cube, LockInfo lock, int steps) {
        try {
            CubeHandler cubeHandler = handlerRegistry.getCubeHandler();
            return cubeHandler.rollback(cube, lock, steps);
        } catch (IOException e) {
            throw new PaloException(e.getLocalizedMessage(), e);
        }
    }

    public final boolean commit(CubeInfo cube, LockInfo lock) {
        try {
            CubeHandler cubeHandler = handlerRegistry.getCubeHandler();
            return cubeHandler.commit(cube, lock);
        } catch (IOException e) {
            throw new PaloException(e.getLocalizedMessage(), e);
        }
    }

    public final CubeLoader getCubeLoader(DatabaseInfo database) {
        String key = HttpCubeLoader.class.getName() + database.getId();
        PaloInfoLoader loader = loaders.get(key);
        if (loader == null) {
            loader = new HttpCubeLoader(this, database);
            loaders.put(key, loader);
        }
        return (HttpCubeLoader) loader;
    }

    public DatabaseLoader getDatabaseLoader() {
        String key = HttpDatabaseLoader.class.getName();
        PaloInfoLoader loader = loaders.get(key);
        if (loader == null) {
            loader = new HttpDatabaseLoader(this);
            loaders.put(key, loader);
        }
        return (DatabaseLoader) loader;
    }

    public DimensionLoader getDimensionLoader(DatabaseInfo database) {
        String key = HttpDimensionLoader.class.getName() + database.getId();
        PaloInfoLoader loader = loaders.get(key);
        if (loader == null) {
            loader = new HttpDimensionLoader(this, database);
            loaders.put(key, loader);
        }
        return (HttpDimensionLoader) loader;
    }

    public ElementLoader getElementLoader(DimensionInfo dimension) {
        return getElementLoader(dimension.getDefaultHierarchy());
    }

    public ElementLoader getElementLoader(HierarchyInfo hierarchy) {
        String dbId = hierarchy.getDimension().getDatabase().getId();
        String key = HttpElementLoader.class.getName() + "#" + dbId + "#" + hierarchy.getDimension().getId();
        PaloInfoLoader loader = loaders.get(key);
        if (loader == null) {
            loader = new HttpElementLoader(this, hierarchy);
            loaders.put(key, loader);
        }
        return (HttpElementLoader) loader;
    }

    public FunctionLoader getFunctionLoader() {
        String key = HttpFunctionLoader.class.getName();
        PaloInfoLoader loader = loaders.get(key);
        if (loader == null) {
            loader = new HttpFunctionLoader(this);
            loaders.put(key, loader);
        }
        return (HttpFunctionLoader) loader;
    }

    public HierarchyLoader getHierarchyLoader(DimensionInfo dimension) {
        String key = HttpHierarchyLoader.class.getName() + "#" + dimension.getDatabase().getId() + "#" + dimension.getId();
        PaloInfoLoader loader = loaders.get(key);
        if (loader == null) {
            loader = new HttpHierarchyLoader(this, dimension);
            loaders.put(key, loader);
        }
        return (HttpHierarchyLoader) loader;
    }

    public RuleLoader getRuleLoader(CubeInfo cube) {
        String dbId = cube.getDatabase().getId();
        String key = HttpRuleLoader.class.getName() + "#" + dbId + "#" + cube.getId();
        PaloInfoLoader loader = loaders.get(key);
        if (loader == null) {
            loader = new HttpRuleLoader(this, cube);
            loaders.put(key, loader);
        }
        return (HttpRuleLoader) loader;
    }

    public PropertyLoader getPropertyLoader() {
        String key = HttpPropertyLoader.class.getName();
        PaloInfoLoader loader = loaders.get(key);
        if (loader == null) {
            loader = new HttpPropertyLoader(this);
            loaders.put(key, loader);
        }
        return (HttpPropertyLoader) loader;
    }

    public PropertyLoader getTypedPropertyLoader(PaloInfo paloObject) {
        String key = HttpPropertyLoader.class.getName() + "#" + paloObject.getId();
        PaloInfoLoader loader = loaders.get(key);
        if (loader == null) {
            loader = new HttpPropertyLoader(this, paloObject);
            loaders.put(key, loader);
        }
        return (HttpPropertyLoader) loader;
    }

    public PropertyLoader getPropertyLoader(DatabaseInfo database) {
        return null;
    }

    public PropertyLoader getPropertyLoader(DimensionInfo dimension) {
        return null;
    }

    public PropertyLoader getPropertyLoader(RuleInfo rule) {
        return null;
    }

    public PropertyLoader getPropertyLoader(ElementInfo element) {
        return null;
    }

    final void serverDown() {
        try {
            httpClient.disconnect();
            fireServerEvent(ServerEvent.SERVER_DOWN);
        } catch (IOException ex) {
            throw new PaloException("Could not disconnect...");
        }
    }

    /**
	 * Checks if the session id is still valid. If it is going to expire soon,
	 * a new login is performed to update the sid
	 * @throws IOException 
	 * @throws NoSuchAlgorithmException 
	 */
    final synchronized void ensureConnection() throws IOException {
        long currTime = System.currentTimeMillis();
        if ((ttl - currTime) < SID_RENEWAL_THRESHOLD) {
            System.err.println("sid renewal");
            loginInternal(connectionInfo.getUsername(), connectionInfo.getPassword());
        }
    }

    /**
	 * Do a reconnect without loosing internal maps!! I.e. only the socket
	 * connection to palo server is renewed as well as a new login...
	 * @throws ConnectException
	 * @throws IOException
	 */
    final void reconnect() throws IOException {
        reconnect(timeout);
    }

    final void reconnect(int timeout) throws IOException {
        if (httpClient == null) return;
        httpClient.reconnect(timeout);
        loginInternal(connectionInfo.getUsername(), connectionInfo.getPassword());
    }

    private final synchronized void stopTimer() {
        if (httpTimer != null) {
            httpTimer.cancel();
            httpTimer = null;
        }
    }

    /**
	 * Starts internal timer task which checks every 20sec if server is still
	 * available. If not a ConnectionEvent.SERVER_DOWN is raised...
	 *
	 */
    private final synchronized void startTimer() {
        stopTimer();
        ConnectionTimerTask task = new ConnectionTimerTask(this);
        httpTimer = new Timer();
        httpTimer.scheduleAtFixedRate(task, 1000, 20000);
    }

    private final synchronized boolean loginInternal(String user, String pass) {
        long currTime = System.currentTimeMillis();
        ServerHandler srvHandler = handlerRegistry.getServerHandler();
        try {
            String[] loginInfo = srvHandler.login(user, pass);
            sid = loginInfo[0];
            ttl = Long.parseLong(loginInfo[1]);
            ttl = currTime + (ttl * 1000);
            startTimer();
            return true;
        } catch (IOException e) {
            throw new PaloException(e.getLocalizedMessage(), e);
        } catch (PaloException pe) {
            return false;
        }
    }

    /**
	 * Returns a hex string representation of the given byte array
	 * @param bytes
	 * @return
	 */
    private final String asHexString(byte[] bytes) {
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < bytes.length; i++) {
            result.append(Integer.toHexString(0x0100 + (bytes[i] & 0x00FF)).substring(1));
        }
        return result.toString();
    }

    private final synchronized void setOwnChange(boolean ownChange) {
        this.ownChange = ownChange;
    }

    private final synchronized boolean isOwnChange() {
        return ownChange;
    }

    public PropertyInfo createNewProperty(String id, String value, PropertyInfo parent, int type, boolean readOnly) {
        return new PropertyInfoImpl(id, value, parent, type, readOnly);
    }
}

/**
 * {@<describe>}
 * <p>
 * A simple <code>TimeTask</code> which ensures the server connection so that 
 * the user is not forced to do it manually
 * </p>
 * {@</describe>}
 *
 * @author ArndHouben
 * 
 */
class ConnectionTimerTask extends TimerTask {

    private HttpConnection httpConnection;

    public ConnectionTimerTask(HttpConnection httpConnection) {
        this.httpConnection = httpConnection;
    }

    public void run() {
        try {
            if (httpConnection.isConnected()) {
                httpConnection.ensureConnection();
                httpConnection.ping();
            } else {
                httpConnection.reconnect();
            }
        } catch (ConnectException cex) {
            httpConnection.serverDown();
        } catch (IOException ioex) {
        } catch (PaloException pex) {
            httpConnection.serverDown();
        }
    }
}
