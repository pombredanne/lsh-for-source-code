package resources.digesters;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.Properties;
import org.xml.sax.SAXException;
import utils.IoUtil;
import utils.logging.Logger;
import components.Component;
import components.ComponentGUI;

/**
 * Un <i>plug-in</i> es una extensi�n que se puede agregar a EasyBot.
 * Por ejemplo, un robot y todos los componentes que lo conforman, son
 * definidos como plug-ins. <br>
 *
 * Un plug-in se crea a partir de un documento XML, y de un conjunto de
 * reglas (tambi�n en formato XML) que permiten establecer los datos del
 * estado del plug-in creado.
 */
public class Plugin implements PluginInterface {

    private String name;

    private String guiClassName;

    private String pluginDescription;

    private String simulationType;

    private String componentType;

    private String xmlFile;

    private String xmlRulesFile;

    private URL context;

    public Plugin(URL fileName, URL context) throws PluginException {
        Properties bundle = new Properties();
        try {
            bundle.load(new FileInputStream(IoUtil.url2file(fileName)));
        } catch (Exception e) {
            Logger.error(e.getMessage());
            throw new PluginException(e.getMessage());
        }
        if (bundle != null) {
            this.xmlFile = bundle.getProperty("XMLFile");
            this.xmlRulesFile = bundle.getProperty("XMLRulesFile");
            this.name = bundle.getProperty("Name");
            this.setPluginDescription(bundle.getProperty("PluginDescription"));
            this.componentType = bundle.getProperty("ComponentType");
            this.simulationType = bundle.getProperty("ForSimulation");
            this.guiClassName = bundle.getProperty("GUIClass");
            this.setContext(context);
        } else {
            Logger.error("Null bundle.");
            throw new PluginException("Null bundle.");
        }
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setGuiClassName(String guiClassName) {
        this.guiClassName = guiClassName;
    }

    public String getGuiClassName() {
        return guiClassName;
    }

    public void setComponentType(int type) {
        this.componentType = String.valueOf(type);
    }

    public int getComponentType() {
        return Integer.parseInt(componentType);
    }

    public void setSimulationType(int simulationType) {
        this.simulationType = String.valueOf(simulationType);
    }

    public int getSimulationType() {
        return Integer.parseInt(simulationType);
    }

    public void setPluginDescription(String pluginDescription) {
        this.pluginDescription = pluginDescription;
    }

    public String getPluginDescription() {
        return pluginDescription;
    }

    public Component makeIntance() throws PluginException {
        try {
            Factory factory = new Factory(IoUtil.url2file(getContext()) + File.separator + getXmlFile(), IoUtil.url2file(getContext()) + File.separator + getXmlRulesFile());
            if (factory == null) throw new PluginException("No pudo crearse la factor�a para " + "levantar el componente.");
            Component component = (Component) factory.digest();
            if (component == null) throw new PluginException("No pudo cargarse el componente.");
            return component;
        } catch (IOException e) {
            throw new PluginException(e.getMessage());
        } catch (SAXException e) {
            throw new PluginException(e.getMessage());
        }
    }

    public ComponentGUI makeGUInstance(Component component) throws PluginException {
        Class componentGUIDef;
        try {
            componentGUIDef = Class.forName(guiClassName);
            Class[] componentGuiArgsClass = new Class[] { Component.class };
            Object[] componentArgs = new Object[] { component };
            Constructor argConstructor = componentGUIDef.getConstructor(componentGuiArgsClass);
            ComponentGUI guiIntance = (ComponentGUI) argConstructor.newInstance(componentArgs);
            if (guiIntance != null) return guiIntance;
        } catch (Exception e) {
            Logger.error(e.getMessage());
            throw new PluginException(e.getMessage());
        }
        return null;
    }

    public void setXmlFile(String xmlFile) {
        this.xmlFile = xmlFile;
    }

    public String getXmlFile() {
        return xmlFile;
    }

    public void setXmlRulesFile(String xmlRulesFile) {
        this.xmlRulesFile = xmlRulesFile;
    }

    public String getXmlRulesFile() {
        return xmlRulesFile;
    }

    public void setContext(URL context) {
        this.context = context;
    }

    public URL getContext() {
        return context;
    }
}
