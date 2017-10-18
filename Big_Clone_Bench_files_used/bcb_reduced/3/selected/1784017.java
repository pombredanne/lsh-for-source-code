package gr.ageorgiadis.util.browser;

import gr.ageorgiadis.util.HexStringHelper;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.html.HTMLCollection;
import org.w3c.dom.html.HTMLFormElement;
import org.w3c.dom.html.HTMLInputElement;
import org.w3c.dom.html.HTMLOptionElement;
import org.w3c.dom.html.HTMLSelectElement;
import org.w3c.dom.html.HTMLTextAreaElement;

/**
 * The default implementation of the ElementHandler interface. Apart from 
 * defining a (NO-OP) implementation for each method defined in the 
 * ElementHandler, it also handles the multiplicity of HTMLInputElement
 * objects, depending on their type.
 * 
 * @author AGeorgiadis
 */
public class FormDataHandler implements ElementHandler {

    private static final Log logger = LogFactory.getLog(FormDataHandler.class);

    protected boolean emptyElementsIncluded = true;

    public void setEmptyElementsIncluded(boolean emptyElementsIncluded) {
        logger.debug("Setting emptyElementsIncluded to: " + emptyElementsIncluded);
        this.emptyElementsIncluded = emptyElementsIncluded;
    }

    /**
	 * <p>Further drive the event into separate event handler methods, based on
	 * the type of the HTMLInputElement found. Handling available for types:
	 * <ul>
	 * <li>text
	 * <li>password
	 * <li>button
	 * <li>submit
	 * <li>file
	 * <li>checkbox
	 * <li>radio
	 * <li>hidden
	 */
    public final void onHTMLInputElement(HTMLInputElement element) {
        String type = element.getType();
        if ("text".equals(type)) {
            onHTMLInputTextElement(element);
        } else if ("password".equals(type)) {
            onHTMLInputPasswordElement(element);
        } else if ("button".equals(type)) {
            onHTMLInputButtonElement(element);
        } else if ("submit".equals(type)) {
            onHTMLInputSubmitElement(element);
        } else if ("file".equals(type)) {
            onHTMLInputFileElement(element);
        } else if ("checkbox".equals(type)) {
            onHTMLInputCheckboxElement(element);
        } else if ("radio".equals(type)) {
            onHTMLInputRadioElement(element);
        } else if ("hidden".equals(type)) {
            onHTMLInputHiddenElement(element);
        }
    }

    @Override
    public final void onHTMLFormElement(HTMLFormElement element) {
        onHTMLForm(element.getId(), element.getName());
    }

    public void onHTMLForm(String id, String name) {
        logger.info("Form: id=" + id + ", name=" + name);
    }

    @Override
    public final void onHTMLSelectElement(HTMLSelectElement element) {
        Object selectObject = onHTMLSelect(element.getName(), element.getMultiple());
        HTMLCollection options = element.getOptions();
        if (options != null) {
            for (int i = 0; i < options.getLength(); i++) {
                HTMLOptionElement optionElement = (HTMLOptionElement) options.item(i);
                onHTMLOptionElement(optionElement, selectObject);
            }
        } else {
            NodeList nl = element.getChildNodes();
            for (int i = 0; i < nl.getLength(); i++) {
                Node node = nl.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equalsIgnoreCase("option")) {
                    onHTMLOptionElement((HTMLOptionElement) node, selectObject);
                }
            }
        }
    }

    public Object onHTMLSelect(String name, boolean multiple) {
        logger.info("Select: name=" + name + ", multiple=" + multiple);
        return null;
    }

    @Override
    public final void onHTMLOptionElement(HTMLOptionElement element, Object selectObject) {
        String value = element.getValue();
        boolean selected = element.getSelected();
        onHTMLOption(value, selected, selectObject);
    }

    public void onHTMLOption(String value, boolean selected, Object selectObject) {
        logger.info("Option: value=" + value + ", selected=" + selected + ", selectObject=" + selectObject);
    }

    @Override
    public final void onHTMLTextAreaElement(HTMLTextAreaElement element) {
        String name = element.getName();
        String value = adjustNullOrEmptyValues(element.getValue());
        if (value == null) {
            logger.warn("Skipping empty textarea element; element.name=" + name);
            return;
        }
        onHTMLTextArea(name, value);
    }

    public void onHTMLTextArea(String name, String value) {
        logger.info("TextArea: name=" + name + ", value=" + value);
    }

    public final void onHTMLInputButtonElement(HTMLInputElement element) {
        String name = element.getName();
        String value = element.getValue();
        onHTMLInputButton(name, value);
    }

    public void onHTMLInputButton(String name, String value) {
        logger.info("InputButton: name=" + name + ", value=" + value);
    }

    public final void onHTMLInputCheckboxElement(HTMLInputElement element) {
        String name = element.getName();
        String value = element.getValue();
        boolean checked = element.getChecked();
        onHTMLInputCheckbox(name, value, checked);
    }

    public void onHTMLInputCheckbox(String name, String value, boolean checked) {
        logger.info("InputCheckbox: name=" + name + ", value=" + value + ", checked=" + checked);
    }

    public final void onHTMLInputFileElement(HTMLInputElement element) {
        String name = element.getName();
        String filename = element.getValue();
        if (filename == null || filename.trim().length() == 0) {
            logger.debug("File input element is empty; element.name=" + element.getName());
            return;
        }
        try {
            File file = new File(filename);
            FileInputStream fis = new FileInputStream(file);
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] buffer = new byte[1024];
            int count = 0;
            while ((count = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, count);
            }
            fis.close();
            byte[] digestBytes = digest.digest();
            String hashValue = HexStringHelper.toHexString(digestBytes);
            onHTMLInputFile(name, filename, hashValue);
        } catch (FileNotFoundException e) {
            logger.warn("File not found: " + filename, e);
        } catch (IOException e) {
            logger.warn("I/O error: " + filename, e);
        } catch (NoSuchAlgorithmException e) {
            logger.warn("No such algorithm exception: " + e);
        }
    }

    public void onHTMLInputFile(String name, String filename, String hashValue) {
        logger.info("InputFile: name=" + name + ", filename=" + filename + ", hashValue=" + hashValue);
    }

    public void onHTMLInputPasswordElement(HTMLInputElement element) {
        String name = element.getName();
        String value = adjustNullOrEmptyValues(element.getValue());
        if (value == null) {
            logger.warn("Skipping empty password input element; element.name=" + name);
            return;
        }
        onHTMLInputPassword(name, value);
    }

    public void onHTMLInputPassword(String name, String value) {
        logger.info("InputPassword: name=" + name + ", value=" + value);
    }

    public final void onHTMLInputRadioElement(HTMLInputElement element) {
        String name = element.getName();
        String value = element.getValue();
        boolean checked = element.getChecked();
        onHTMLInputRadio(name, value, checked);
    }

    public void onHTMLInputRadio(String name, String value, boolean checked) {
        logger.info("InputRadio: name=" + name + ", value=" + value + ", checked=" + checked);
    }

    public final void onHTMLInputSubmitElement(HTMLInputElement element) {
        String name = element.getName();
        String value = element.getValue();
        if (value == null || value.trim().length() == 0) {
            logger.debug("Submit input element is empty; element.name=" + name);
            return;
        }
        onHTMLInputSubmit(name, value);
    }

    public void onHTMLInputSubmit(String name, String value) {
        logger.info("InputSubmit: name=" + name + ", value=" + value);
    }

    public final void onHTMLInputTextElement(HTMLInputElement element) {
        String name = element.getName();
        String value = adjustNullOrEmptyValues(element.getValue());
        if (value == null) {
            logger.warn("Skipping empty text input element; element.name=" + name);
            return;
        }
        onHTMLInputText(name, value);
    }

    public void onHTMLInputText(String name, String value) {
        logger.info("InputText: name=" + name + ", value=" + value);
    }

    public final void onHTMLInputHiddenElement(HTMLInputElement element) {
        String name = element.getName();
        String value = adjustNullOrEmptyValues(element.getValue());
        if (value == null) {
            logger.warn("Skipping empty hidden input element; element.name=" + name);
            return;
        }
        onHTMLInputHidden(name, value);
    }

    public void onHTMLInputHidden(String name, String value) {
        logger.info("InputHidden: name=" + name + ", value=" + value);
    }

    private String adjustNullOrEmptyValues(String value) {
        if (value == null || value.length() == 0) {
            if (emptyElementsIncluded) {
                return "";
            } else {
                return null;
            }
        } else {
            return value;
        }
    }
}
