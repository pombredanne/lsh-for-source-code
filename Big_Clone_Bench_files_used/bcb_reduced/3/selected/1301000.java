package core.simulation;

import gui.EasyBotAppException;
import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.Vector;
import org.apache.commons.io.FileUtils;
import org.xml.sax.SAXException;
import resources.digesters.ClassManager;
import resources.digesters.Factory;
import resources.digesters.Plugin;
import utils.defines.Defines;
import utils.images.RecognizedShape;
import utils.logging.Logger;
import components.gps.GPS;
import components.robot.Playable;
import components.robot.Robot;

/**
 * Esta clase representa una simulaci�n de robots en un escenario. Contiene
 * un arreglo con todos los robots que participan en la simulaci�n, como
 * tambi�n, informaci�n relacionada al <i>timing</i> de la ejecuci�n.<br><br>
 * 
 * Solo se permite una �nica simulaci�n por vez (no deber�an existir dos o
 * m�s instancias simult�neamente). Antes de crear una nueva instancia
 * <code>Simulation</code> deber�n liberarse todos los recursos de la
 * simulaci�n anterior, invocando al m�todo {@link #dispose() dispose}.
 * En cualquier momento podr� obtenerse una referencia a la simulaci�n actual
 * mediante la invocaci�n del m�todo {@link #getCurrent() getCurrent}.<br><br>
 * 
 * Implementa la interfaz <code>Playable</code> junto con los controles
 * de ejecuci�n.<br><br>
 * 
 * Esta clase no puede ser extendida.
 */
public final class Simulation implements Playable {

    private static Simulation simulation;

    private Vector robots;

    private GPS gps;

    private int type;

    private String baseSimulationDirectory;

    private String name;

    private String comment;

    private int[][] places;

    private int[] place;

    private int targetPlace;

    private long stepTime;

    private boolean playbackSimualtion;

    private long duration;

    private boolean allOk;

    /**
	 * Construye una nueva simulaci�n, sin robots, y con un tiempo de paso
	 * de {@link Defines#DEFAULT_STEP_TIME Defines.DEFAULT_STEP_TIME}
	 * milisegundos.
	 */
    public Simulation() {
        baseSimulationDirectory = null;
        robots = new Vector();
        simulation = this;
        playbackSimualtion = true;
        places = null;
        place = new int[Defines.MAX_PLACES];
        stepTime = Defines.DEFAULT_STEP_TIME;
        duration = 0;
        setAllOk(false);
    }

    /**
	 * Establece el tipo de simulaci�n, que debe pertencer a los definidos
	 * en {@link utils.defines.Defines utils.defines.Defines}.
	 * @param t	tipo de simulaci�n.
	 */
    public void setType(int t) {
        type = t;
    }

    /**
	 * Retorna el tipo de simulaci�n.
	 * @return	tipo de simulaci�n, definido en
	 * 			{@link utils.defines.Defines utils.defines.Defines}.
	 */
    public int getType() {
        return type;
    }

    /**
	 * Agrega un nuevo robot a la simulaci�n.
	 * @param robot	robot a agregar.
	 */
    public void addRobot(Object robot) {
        robots.addElement(robot);
    }

    /**
	 * Retorna la lista de robots que integran la simulaci�n.
	 * @return lista de robots.
	 */
    public Vector getRobotArray() {
        return robots;
    }

    /**
	 * Retorna una referencia al robot (perteneciente a la simulaci�n) cuyo
	 * nombre es <code>name</code>, o <code>null</code> si no existe.
	 * @param name	nombre (un�voco) del robot a buscar. 
	 * @return		referencia al robot, o <code>null</code> si no existe.
	 */
    public Robot getRobotByName(String name) {
        if (robots != null) for (int i = 0; i < robots.size(); i++) if (((Robot) robots.get(i)).getName().compareToIgnoreCase(name) == 0) return (Robot) robots.get(i);
        return null;
    }

    /**
	 * Elimina de la simulaci�n el robot indicado por par�metro, y retorna
	 * <code>true</code> si tuvo �xito, o <code>false</code> si el robot no
	 * exist�a.
	 * @param robot		referencia al robot a eliminar.
	 * @return			<code>true</code> si tuvo �xito, o <code>false</code>
	 * 					si no exist�a.
	 */
    public boolean removeRobot(Object robot) {
        return robots.removeElement(robot);
    }

    public void removeAllRobots() {
        robots.removeAllElements();
    }

    /**
	 * Retorna una referencia a la simulaci�n actual.
	 * @return	referencia a la simulaci�n actual, o <code>null</code> si no
	 * 			existe una simulaci�n creada. 
	 */
    public static Simulation getCurrent() {
        return simulation;
    }

    /**
	 * Libera los recursos de la simulaci�n. Este m�todo deber� invocarse
	 * antes de crear una nueva simulaci�n.
	 */
    public void dispose() {
        for (int i = 0; robots != null && i < robots.size(); i++) if (!((Robot) robots.get(i)).isDisposed()) ((Robot) robots.get(i)).dispose();
        if (gps != null && !gps.isDisposed()) gps.dispose();
        simulation = null;
    }

    public boolean isDisposed() {
        return (simulation == null);
    }

    /**
	 * Establece el <i>tiempo de paso</code>, que es el per�odo que insume
	 * la simulaci�n para pasar de un estado al siguiente. Cambios en este
	 * valor permiten ejecutar la simulaci�n a diferentes velocidades (a
	 * menor <i>tiempo de paso</code>, mayor velocidad de ejecuci�n).
	 * @param sT	tiempo de paso en milisegundos.
	 */
    public void setStepTime(long sT) {
        stepTime = sT;
    }

    /**
	 * Retorna el <i>tiempo de paso</code> de la simulaci�n. 
	 * @return tiempo de paso en milisegundos.
	 */
    public long getStepTime() {
        return stepTime;
    }

    /**
	 * Permite establecer cu�l es el <i>lugar objetivo</i> de la simulaci�n.
	 * Los robots pertenecientes a la simulaci�n deber�n navegar el escenario
	 * hasta alcanzar un destino, que est� identificado por el par�metro
	 * <code>targetPlace</code>.<br><br>
	 * Esto aplica, principalmente, a la exploraci�n de laberintos.
	 * @param targetPlace	id del lugar objetivo.
	 */
    public void setTargetPlace(int targetPlace) {
        this.targetPlace = targetPlace;
    }

    /**
	 * Retorna el <i>lugar objetivo</i> de la simulaci�n.
	 * @return id del lugar objetivo.
	 */
    public int getTargetPlace() {
        return targetPlace;
    }

    /**
	 * Define un posible <i>camino</i> (bidireccional) entre los lugares
	 * <code>fromPlace</code> y <code>toPlace</code>. Esto significa que,
	 * en un escenario, los robots podr�n desplazarse entre esos lugares,
	 * en cualquier sentido.<br><br>
	 * Esto aplica, principalmente, a la exploraci�n de laberintos. 
	 * @param fromPlace	lugar desde donde puede alcanzarse <code>toPlace</code>.
	 * @param toPlace	lugar desde donde puede alcanzarse <code>fromPlace</code>.
	 */
    public void setPlace(int fromPlace, int toPlace) {
        if (places == null) {
            int nPlaces = Simulation.getCurrent().getGps().getMazeItems().recognizedColoredIcons.length;
            places = new int[nPlaces][nPlaces];
            for (int i = 0; i < nPlaces; i++) for (int j = 0; j < nPlaces; j++) places[i][j] = 0;
        }
        if (places[fromPlace - 1][toPlace - 1] == 0) {
            places[fromPlace - 1][toPlace - 1] = 1;
        }
    }

    public void setPlaces(Places places) {
        this.places = places.getPlaces();
    }

    /**
	 * Retorna la matriz de caminos (conexiones) entre los lugares que conforman
	 * el escenario. Esta matriz es cuadrada, de N por N (siendo N la cantidad
	 * de lugares), con valores 0 � 1. Si el elemento (i,j) es 0, significa que
	 * no se puede transitar directamente entre los lugares i y j. Si el
	 * elemento (i,j) es 1, entonces existe un camino directo entre ambos
	 * lugares.<br><br>
	 * Esto aplica, principalmente, a la exploraci�n de laberintos.
	 * @return matriz de conexiones.
	 */
    public int[][] getPlaces() {
        return places;
    }

    /**
	 * Determina el lugar actual en que se haya un robot, en base a los colores
	 * que se perciben desde all�.<br><br>
	 * Esto aplica, principalmente, a la exploraci�n de laberintos.
	 * @param identifiers		colores percibidoes desde el lugar actual.
	 * @return					id del lugar actual o <code>ERROR</code> si
	 * 							no se pudo resolver.
	 */
    public int getPlace(Color[] identifiers) {
        int i, j, placeId;
        boolean encontrado;
        if (identifiers == null) {
            Logger.error("Error resolviendo el lugar en getPlace(): identificadores nulos.");
            return Defines.ERROR;
        }
        if (Simulation.getCurrent().getGps().getMazeItems() == null || Simulation.getCurrent().getGps().getMazeItems().recognizedColoredIcons == null) {
            Logger.error("No se pudieron obtener los �conos coloreados del GPS.");
            return Defines.ERROR;
        }
        RecognizedShape[] icons = Simulation.getCurrent().getGps().getMazeItems().recognizedColoredIcons;
        for (i = 0; i < icons.length; i++) {
            placeId = Integer.valueOf(icons[i].shapeId).intValue();
            encontrado = false;
            for (j = 0; !encontrado && j < identifiers.length; j++) if (icons[i].color.equals(identifiers[j])) encontrado = true;
            if (encontrado) place[placeId - 1] = 1; else place[placeId - 1] = 0;
        }
        encontrado = false;
        for (i = 0; !encontrado && i < places.length; i++) {
            for (j = 0; j < places[i].length; j++) if (i != j && places[i][j] != place[j]) break;
            if (j == places[i].length) encontrado = true;
        }
        if (encontrado) return i; else return Defines.ERROR;
    }

    /**
	 * Elimina de la simulaci�n todos los lugares y conexiones entre ellos.
	 */
    public void removePlaces() {
        places = null;
    }

    public synchronized void play() {
        for (int i = 0; i < robots.size(); i++) ((Robot) robots.get(i)).play();
    }

    public synchronized void pause() {
        for (int i = 0; i < robots.size(); i++) ((Robot) robots.get(i)).pause();
    }

    public synchronized void stop() {
        for (int i = 0; i < robots.size(); i++) ((Robot) robots.get(i)).stop();
        for (int i = 0; i < robots.size(); i++) ((Robot) robots.get(i)).join();
    }

    public synchronized boolean canPlay() {
        return false;
    }

    public synchronized boolean isPlaying() {
        return false;
    }

    /**
	 * Establece la ubicaci�n en la cual se almacenar�n todos los datos
	 * persistentes y de configuraci�n de la simulaci�n.
	 * @param dir ruta en donde se almacenar� la simulaci�n.
	 */
    public void setBaseSimulationDirectory(String dir) {
        baseSimulationDirectory = new String(dir + "\\");
    }

    /**
	 * Retorna la ruta en donde se almacena la simulaci�n.
	 * @return ruta.
	 */
    public String getBaseSimulationDirectory() {
        return baseSimulationDirectory;
    }

    /**
	 * Almacena la configuraci�n de la simulaci�n, en la ubicaci�n indicada
	 * por {@link #setBaseSimulationDirectory(String) setBaseSimulationDirectory}.
	 * @throws EasyBotAppException 
	 */
    public void saveSimulationConfig() throws EasyBotAppException {
        try {
            File xmlFile = new File(Simulation.getCurrent().getBaseSimulationDirectory() + "/simulation.xml");
            File rulesFile = new File("src/core/simulation/simulation-rules.xml");
            File simDir = new File(Simulation.getCurrent().getBaseSimulationDirectory());
            PrintWriter xmlWriter = new PrintWriter(new FileOutputStream(xmlFile));
            xmlWriter.println("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>");
            xmlWriter.println("<simulation name=\"" + this.getName() + "\" >");
            xmlWriter.println("\t<path>" + this.getBaseSimulationDirectory() + "</path>");
            xmlWriter.println("\t<type>" + this.getType() + "</type>");
            xmlWriter.println("\t<comment>" + this.getComment() + "</comment>");
            xmlWriter.println("\t<steptime>" + this.getStepTime() + "</steptime>");
            for (int i = 0; i < this.getRobotArray().size(); i++) {
                String file = ((Robot) this.getRobotArray().get(i)).getXMLConfigFile();
                xmlWriter.println("\t<robot>");
                xmlWriter.println("\t\t<xmlfile>" + this.getBaseSimulationDirectory() + "\\components\\" + file + "</xmlfile>");
                Plugin robot = ClassManager.getInstance().getPluginByName(((Robot) this.getRobotArray().get(i)).getClass().getCanonicalName());
                xmlWriter.println("\t\t<xml-rules>" + robot.getContext().getPath() + robot.getXmlRulesFile() + "</xml-rules>");
                xmlWriter.println("\t</robot>");
            }
            xmlWriter.println("\t<gps>");
            xmlWriter.println("\t\t<xmlfile>" + this.getBaseSimulationDirectory() + "\\components\\gps.xml</xmlfile>");
            Plugin gps = ClassManager.getInstance().getPluginByName(Simulation.getCurrent().getGps().getClass().getCanonicalName());
            xmlWriter.println("\t\t<xml-rules>" + gps.getContext().getPath() + gps.getXmlRulesFile() + "</xml-rules>");
            xmlWriter.println("\t</gps>");
            xmlWriter.println("\t<places>");
            for (int i = 0; i < places.length; i++) {
                String row = new String();
                for (int j = 0; j < places[i].length; j++) row = row.concat(places[i][j] + Places.COLUMN_DELIMITER);
                xmlWriter.println("\t\t<place>" + row + "</place>");
            }
            xmlWriter.println("\t</places>");
            xmlWriter.println("</simulation>");
            xmlWriter.close();
            FileUtils.copyFileToDirectory(rulesFile, simDir);
        } catch (Exception e) {
            throw new EasyBotAppException(e.getMessage());
        }
    }

    /**
	 * Establece el nombre de la simulaci�n. No hay restricciones de univocidad
	 * en este dato.
	 * @param n	nombre (no necesariamente un�voco) de la simulaci�n.
	 */
    public void setName(String n) {
        name = new String(n);
    }

    /**
	 * Retorna el nombre de la simulaci�n.
	 * @return nombre (no necesariamente un�voco).
	 */
    public String getName() {
        return name;
    }

    /**
	 * Permite establecer un comentario descriptivo de la simulaci�n.
	 * @param c	comentario.
	 */
    public void setComment(String c) {
        comment = new String(c);
    }

    /**
	 * Retorna la descripci�n establecida con
	 * {@link #setComment(String) setComment}.
	 * @return comentario descriptivo de la simulaci�n.
	 */
    public String getComment() {
        return comment;
    }

    public void setPlaybackSimualtion(boolean playbackSimualtion) {
        this.playbackSimualtion = playbackSimualtion;
    }

    public boolean isPlaybackSimualtion() {
        return playbackSimualtion;
    }

    private long getRobotSleepCounts() {
        long counts = 0;
        Vector robots = getRobotArray();
        for (int i = 0; i < robots.size(); i++) counts += ((Robot) robots.get(i)).getSleepCounts();
        if (robots.size() > 0) return counts / robots.size(); else return 0;
    }

    /**
	 * La duraci�n de la simulaci�n se calcula en base a la cantidad
	 * de veces promedio que se durmieron los robots, multiplicado
	 * por el tiempo <code>stepTime</code> por defecto. Si bien no es
	 * el tiempo real, es una muy buena aproximaci�n. 
	 * @return duraci�n en milisegundos.
	 */
    public long getDuration() {
        duration = getRobotSleepCounts() * Defines.DEFAULT_STEP_TIME;
        return duration;
    }

    public String getFormattedDuration() {
        long hours, minutes, seconds, millis;
        long duration = this.getDuration();
        hours = duration / 3600000;
        minutes = (duration - (hours * 3600000)) / 60000;
        seconds = (duration - (hours * 3600000 + minutes * 60000)) / 1000;
        millis = duration - (hours * 3600000 + minutes * 60000 + seconds * 1000);
        DecimalFormat formatter = new DecimalFormat("##00");
        return formatter.format(hours) + ":" + formatter.format(minutes) + ":" + formatter.format(seconds) + ":" + formatter.format(millis);
    }

    public void unLoad() {
        simulation.dispose();
    }

    public static void load(String rootDir) throws SimulationException {
        try {
            Factory factory = new Factory(rootDir + File.separator + "simulation.xml", rootDir + File.separator + "simulation-rules.xml");
            if (factory == null) throw new SimulationException("No pudo crearse la factor�a para " + "levantar la simulaci�n.");
            simulation = (Simulation) factory.digest();
            if (simulation == null) throw new SimulationException("No pudo cargarse la simulaci�n.");
        } catch (IOException e) {
            throw new SimulationException(e.getMessage());
        } catch (SAXException e) {
            throw new SimulationException(e.getMessage());
        }
    }

    public void digestRobots(String xmlFile, String xmlRulesFile) throws SimulationException {
        try {
            Factory factory = new Factory(xmlFile, xmlRulesFile);
            if (factory == null) throw new SimulationException("No pudo crearse la factor�a para " + "levantar los robots.");
            Robot robot = (Robot) factory.digest();
            if (robot == null) throw new SimulationException("No pudo cargarse el robot.");
            simulation.addRobot(robot);
        } catch (IOException e) {
            throw new SimulationException(e.getMessage());
        } catch (SAXException e) {
            throw new SimulationException(e.getMessage());
        }
    }

    public void digestGPS(String xmlFile, String xmlRulesFile) throws SimulationException {
        try {
            Factory factory = new Factory(xmlFile, xmlRulesFile);
            if (factory == null) throw new SimulationException("No pudo crearse la factor�a para " + "levantar el GPS.");
            GPS gps = (GPS) factory.digest();
            if (gps == null) throw new SimulationException("No pudo cargarse el GPS.");
            simulation.setGps(gps);
        } catch (IOException e) {
            throw new SimulationException(e.getMessage());
        } catch (SAXException e) {
            throw new SimulationException(e.getMessage());
        }
    }

    public void setAllOk(boolean allOk) {
        this.allOk = allOk;
    }

    public boolean isAllOk() {
        return allOk;
    }

    public GPS getGps() {
        return gps;
    }

    public void setGps(GPS gps) {
        this.gps = gps;
    }
}
