package testrunner;

import java.io.*;
import java.net.MalformedURLException;
import java.util.*;
import java.util.regex.*;
import org.apache.log4j.Logger;
import org.xml.sax.SAXException;
import junit.framework.TestCase;
import com.meterware.httpunit.*;
import testdata.*;
import javax.activation.*;
import org.pdfbox.pdfparser.*;
import org.pdfbox.util.*;
import org.pdfbox.pdmodel.*;

/**
 * the class User represents a user that accesses a webapplication with a browser
 */
@SuppressWarnings("null")
public class User {

    private static final Logger logger = Logger.getLogger(User.class);

    /**
	 * the WebConversation object is the interface to HttpUnit
	 * that simulates the browser
	 */
    protected WebConversation webConversation;

    /**
	 * name of the user
	 */
    protected String userName;

    /**
	 * the current window in the browser
	 */
    protected WebWindow currentWindow;

    /**
	 * the window that was opened last
	 */
    protected WebWindow lastOpenedWindow;

    /**
	 * the window that was closed last
	 */
    protected WebWindow lastClosedWindow;

    /**
	 * true if the current window is a popup
	 */
    protected boolean isPopup;

    /**
	 * creates a new user and initialises the webconversation
	 * 
	 * @param url the start url
	 * @param name the name of the user
	 */
    @SuppressWarnings("nls")
    public User(String url, String name) {
        webConversation = new WebConversation();
        this.userName = name;
        webConversation.addWindowListener(new WebWindowListener() {

            public void windowOpened(WebClient client, WebWindow window) {
                lastOpenedWindow = window;
            }

            public void windowClosed(WebClient client, WebWindow window) {
                lastClosedWindow = window;
            }
        });
        try {
            webConversation.getResponse(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
    }

    /**
	 * checks if a frame with a given name is currently active
	 * 
	 * @param frame name of the frame
	 * @return the WebResponse of the frame
	 */
    @SuppressWarnings("nls")
    private WebResponse checkFrame(String frame) {
        WebResponse response = null;
        String[] frameNames = webConversation.getFrameNames();
        for (String frameName : frameNames) {
            if (frameName.equals(frame)) {
                response = webConversation.getFrameContents(frame);
            }
        }
        TestCase.assertNotNull(userName + ": The frame '" + frame + "' cannot be found", response);
        return response;
    }

    /**
	 * checks if a form with a given name is in a certain response
	 * 
	 * @param response the current frame content
	 * @param form the name of the form
	 * @return the WebForm object
	 */
    @SuppressWarnings("nls")
    private WebForm checkForm(WebResponse response, String form) {
        WebForm webForm = null;
        try {
            webForm = response.getFormWithName(form);
            TestCase.assertNotNull(userName + ": The form '" + form + "' cannot be found" + "\n\n" + response.getText(), webForm);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
        return webForm;
    }

    /**
	 * navigates to a page with the given navigationpath
	 * 
	 * @param navigationPath path to a certain webpage
	 * @param sleepTime time to wait between each navigation (in seconds)
	 * @param exceptions true, if JavaScript-Exceptions have to be suppressed 
	 */
    @SuppressWarnings("nls")
    protected void navigate(List<PageElement> navigationPath, int sleepTime, boolean exceptions) {
        for (PageElement pageElement : navigationPath) {
            WebElementType type = pageElement.getType();
            waiting(sleepTime, userName);
            switch(type) {
                case BUTTON:
                    this.clickButton(pageElement, exceptions, false);
                    break;
                case NAMELESSBUTTON:
                    this.clickButton(pageElement, exceptions, true);
                    break;
                case LINK:
                    this.clickLink(pageElement, false);
                    break;
                case NAMELESSLINK:
                    this.clickLink(pageElement, true);
                    break;
                case URL:
                    this.loadUrl(pageElement);
                    break;
                default:
                    throw new SecurityException(type.toString() + " is not a valid type for navigation!");
            }
        }
    }

    /**
	 * finds the button with the specified properties in the specified frame and clicks it
	 * 
	 * @param pageElement contains information about the button
	 * @param exceptions true if the JavaScript-Exceptions have to be suppressed
	 * @param nameless true if the button will be identified by its "value"-attribute
	 *                 false if the button will be identified by its "name"-attribute
	 */
    @SuppressWarnings("nls")
    private void clickButton(PageElement pageElement, boolean exceptions, boolean nameless) {
        String name = pageElement.getName();
        String frame = pageElement.getFrame();
        WebResponse response = webConversation.getCurrentPage();
        if (!(frame.equals("none"))) {
            response = checkFrame(frame);
        }
        try {
            HTMLElement[] elements = null;
            if (nameless) {
                elements = response.getElementsWithAttribute("value", name);
                TestCase.assertFalse(userName + ": No HTML-Element with the value '" + name + "' found!" + "\n\n" + response.getText(), elements.length == 0);
                TestCase.assertFalse(userName + ": More than one HTML-Element with the value '" + name + "' found!" + "\n\n" + response.getText(), elements.length > 1);
                TestCase.assertTrue(userName + ": The HTML-Element with the value '" + name + "' is no button!" + "\n\n" + response.getText(), elements[0] instanceof Button);
            } else {
                elements = response.getElementsWithName(name);
                TestCase.assertFalse(userName + ": No HTML-Element with the name '" + name + "' found!" + "\n\n" + response.getText(), elements.length == 0);
                TestCase.assertFalse(userName + ": More than one HTML-Element with the name '" + name + "' found!" + "\n\n" + response.getText(), elements.length > 1);
                TestCase.assertTrue(userName + ": The HTML-Element with the name '" + name + "' is no button!" + "\n\n" + response.getText(), elements[0] instanceof Button);
            }
            Button button = (Button) elements[0];
            TestCase.assertFalse(userName + ": The button: " + name + " is disabled" + "\n \n " + response.getText(), button.isDisabled());
            if (exceptions) {
                HttpUnitOptions.setExceptionsThrownOnScriptError(false);
                button.click();
                HttpUnitOptions.setExceptionsThrownOnScriptError(true);
            } else {
                button.click();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
        logger.debug(userName + ": Button '" + name + "' clicked successfully");
    }

    /**
	 * finds the link with the specified properties in the specified frame and clicks it
	 * 
	 * @param pageElement contains information about the link
	 */
    @SuppressWarnings("nls")
    private void clickLink(PageElement pageElement, boolean nameless) {
        String name = pageElement.getName();
        String frame = pageElement.getFrame();
        WebResponse response = webConversation.getCurrentPage();
        if (!(frame.equals("none"))) {
            response = checkFrame(frame);
        }
        try {
            WebLink[] links = response.getLinks();
            WebLink wantedLink = null;
            if (nameless) {
                for (WebLink link : links) {
                    if (link.getURLString().equals(name)) {
                        wantedLink = link;
                    }
                }
            } else {
                for (WebLink link : links) {
                    if (link.getText().equals(name)) {
                        wantedLink = link;
                    }
                }
            }
            TestCase.assertNotNull(userName + ": The link: " + name + " cannot be found" + "\n\n" + response.getText(), wantedLink);
            wantedLink.click();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.debug(userName + ": Link '" + name + "' clicked successfully");
    }

    /**
	 * directly load an url into a frame
	 *
	 * @param pageElement contains information about the url request
	 */
    @SuppressWarnings("nls")
    private void loadUrl(PageElement pageElement) {
        String url = pageElement.getName().replaceAll("&amp;", "&");
        String target = pageElement.getFrame();
        WebResponse.Scriptable scriptable = webConversation.getFrameContents(target).getScriptableObject();
        try {
            scriptable.setLocation(url);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
        logger.debug(userName + ": URL: " + url + " loaded successfully");
    }

    /**
	 * inserts the form data into a certain form in a certain frame
	 * 
	 * @param fieldValues the form data
	 * @param exceptions true if JavaScript-Exception have to be suppressed
	 */
    @SuppressWarnings("nls")
    protected void insertContent(FieldValues fieldValues, boolean exceptions) {
        List<FieldValue> fieldValueList = fieldValues.getFieldValueList();
        String frame = fieldValues.getFrame();
        String form = fieldValues.getForm();
        WebResponse response = webConversation.getCurrentPage();
        if (fieldValueList.isEmpty()) {
            return;
        }
        if (!(frame.equals("none"))) {
            response = checkFrame(frame);
        }
        WebForm webForm = checkForm(response, form);
        for (FieldValue fieldValue : fieldValueList) {
            String name = fieldValue.getName();
            String entry = fieldValue.getEntry();
            FieldType type = fieldValue.getType();
            switch(type) {
                case NORMAL:
                    this.setParameter(webForm, name, entry);
                    break;
                case CHECKBOX:
                    this.setCheckbox(webForm, fieldValue);
                    break;
                case FILEUPLOAD:
                    this.setFileUpload(response, name, entry);
                    break;
                default:
                    throw new SecurityException(type.toString() + " is not a valid type for insertion");
            }
        }
    }

    /**
	 * sets a parameter in a given form to a specified value
	 * 
	 * @param webForm the form that contains the parameter
	 * @param parameterName the name of the parameter
	 * @param parameterValue the value of the parameter
	 */
    @SuppressWarnings("nls")
    private void setParameter(WebForm webForm, String name, String value) {
        TestCase.assertTrue(userName + ": The form '" + webForm.getName() + "' does not contain the parameter '" + name + "'", webForm.hasParameterNamed(name));
        WebForm.Scriptable scriptable = webForm.getScriptableObject();
        scriptable.setParameterValue(name, value);
        logger.debug(userName + ": Parameter '" + name + "' successfully set to '" + value + "'");
    }

    /**
	 * sets the checkbox or resets it
	 * 
	 * @param webForm Name der Form mit der Checkbox
	 * @param fieldValue enth�lt die Infos �ber die Checkbox
	 */
    @SuppressWarnings("nls")
    private void setCheckbox(WebForm webForm, FieldValue fieldValue) {
        String name = fieldValue.getName();
        String value = fieldValue.getEntry();
        String checkboxValue = fieldValue.getCheckboxValue();
        HttpUnitOptions.setExceptionsThrownOnScriptError(false);
        boolean status = Boolean.valueOf(value);
        if (checkboxValue == null) {
            webForm.setCheckbox(name, status);
            logger.debug(userName + ": Checkbox '" + name + "' successfully set to '" + value + "'");
        } else {
            webForm.setCheckbox(name, checkboxValue, status);
            logger.debug(userName + ": Checkbox '" + name + "' successfully set to '" + checkboxValue + "'");
        }
        HttpUnitOptions.setExceptionsThrownOnScriptError(true);
    }

    /**
	 * find an HTML-Element, typically a table cell with a given
	 * "title"-attribute and simulate an "onclick"-event
	 *
	 * @param pageElement contains information about the url request
	 */
    @SuppressWarnings("nls")
    private void setFileUpload(WebResponse response, String name, String entry) {
        WebForm wantedForm = null;
        try {
            WebForm[] webForms = response.getForms();
            for (WebForm aktForm : webForms) {
                if (aktForm.isFileParameter(name)) {
                    wantedForm = aktForm;
                }
            }
        } catch (SAXException e) {
            e.printStackTrace();
        }
        TestCase.assertNotNull(userName + ": No file parameter named '" + name + "' could be found!", wantedForm);
        File file = new File(entry);
        TestCase.assertNotNull(userName + ": No local file named '" + entry + "' could be found!", file);
        FileDataSource mimeType = new FileDataSource(file);
        UploadFileSpec[] upFile = new UploadFileSpec[1];
        upFile[0] = new UploadFileSpec(file, mimeType.getContentType());
        wantedForm.setParameter(name, upFile);
        logger.debug(userName + ": file parameter '" + name + "' successfully set to '" + entry + "'");
    }

    /**
	 * compares the form data of a webpage with given form data
	 * 
	 * @param fieldValues the form data to compare with
	 * @param exceptions true if JavaScript-Exceptions have to be suppressed
	 */
    @SuppressWarnings("nls")
    protected void compareContent(FieldValues fieldValues, boolean exceptions) {
        try {
            List<FieldValue> fieldValueList = fieldValues.getFieldValueList();
            if (fieldValueList.isEmpty()) {
                return;
            }
            String frame = fieldValues.getFrame();
            WebResponse response = webConversation.getCurrentPage();
            if (!(frame.equals("none"))) {
                response = checkFrame(frame);
            }
            String form = fieldValues.getForm();
            WebForm webForm = checkForm(response, form);
            for (FieldValue fieldValue : fieldValueList) {
                String name = fieldValue.getName();
                String value = fieldValue.getEntry();
                FieldType type = fieldValue.getType();
                TestCase.assertNotNull(userName + ": The parameter '" + name + "' in the form '" + webForm.getName() + "' could not be found!", webForm.hasParameterNamed(name));
                switch(type) {
                    case NORMAL:
                    case FILEUPLOAD:
                        TestCase.assertEquals(userName + ": The parameter '" + name + "' does not contain the value '" + value + "'" + "\n\n" + response.getText(), webForm.getParameterValue(name), value);
                        break;
                    default:
                        throw new SecurityException(type.toString() + " is not a valid type for comparison");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Vergleicht den Inhalt der aktuellen Webseite mit dem IPage-Objekt
	 * 
	 * @param iPage die zu pr�fende IPage
	 */
    @SuppressWarnings("nls")
    protected void comparePage(IPage iPage) {
        if (iPage instanceof Page) {
            String frame = ((Page) iPage).getName();
            List<PageElement> pageElements = ((Page) iPage).getElements();
            for (PageElement pageElement : pageElements) {
                compareElement(frame, pageElement);
            }
        } else {
            if (iPage instanceof FrameSet) {
                String frame = ((FrameSet) iPage).getName();
                if (!(frame.equals("none"))) {
                    checkFrame(frame);
                }
                List<IPage> iPages = ((FrameSet) iPage).getFrames();
                for (IPage iPageChild : iPages) {
                    comparePage(iPageChild);
                }
            } else {
                throw new SecurityException("The IPage is not a valid type");
            }
        }
    }

    /**
	 * checks all additional elements
	 * 
	 * @param additionalElements Array aus PageElements
	 */
    protected void compareAdditionalElements(List<PageElement> additionalElements) {
        for (PageElement pageElement : additionalElements) {
            compareElement(pageElement.getFrame(), pageElement);
        }
    }

    /**
	 * check if a single page element exists in the current page
	 * 
	 * @param frame name of the frame where the element is placed or "none"
	 * @param pageElement the element to check
	 */
    @SuppressWarnings("nls")
    private void compareElement(String frame, PageElement pageElement) {
        WebResponse response = webConversation.getCurrentPage();
        if (!(frame.equals("none"))) {
            response = checkFrame(frame);
        }
        String name = pageElement.getName();
        boolean existing = pageElement.isExisting();
        WebElementType type = pageElement.getType();
        if (existing) {
            logger.debug(userName + ": Check if the PageElement '" + name + "' does exist");
        } else {
            logger.debug(userName + ": Check if the PageElement '" + name + "' does not exist");
        }
        try {
            switch(type) {
                case NAMELESSBUTTON:
                    boolean exists = false;
                    HTMLElement[] htmlElements = response.getElementsWithAttribute("value", name);
                    for (HTMLElement htmlElement : htmlElements) {
                        if (htmlElement.getClassName().equalsIgnoreCase("button")) {
                            exists = true;
                        }
                    }
                    if (existing) {
                        TestCase.assertTrue(userName + ": The button '" + name + "' in frame '" + frame + "' does not exist" + "\n\n" + response.getText(), exists);
                    } else {
                        TestCase.assertFalse(userName + ": The button '" + name + "' in frame '" + frame + "' does exist" + "\n\n" + response.getText(), exists);
                    }
                    break;
                case BUTTON:
                    exists = false;
                    htmlElements = response.getElementsWithName(name);
                    for (HTMLElement htmlElement : htmlElements) {
                        if (htmlElement.getClassName().equalsIgnoreCase("button")) {
                            exists = true;
                        }
                    }
                    if (existing) {
                        TestCase.assertTrue(userName + ": The button '" + name + "' in frame '" + frame + "' does not exist" + "\n\n" + response.getText(), exists);
                    } else {
                        TestCase.assertFalse(userName + ": The button '" + name + "' in frame '" + frame + "' does exist" + "\n\n" + response.getText(), exists);
                    }
                    break;
                case LINK:
                    WebLink link = response.getLinkWith(name);
                    if (existing) {
                        TestCase.assertNotNull(userName + ": The link '" + name + "' in frame '" + frame + "' does not exist" + "\n\n" + response.getText(), link);
                    } else {
                        TestCase.assertNull(userName + ": The link '" + name + "' in frame '" + frame + "' does exist" + "\n\n" + response.getText(), link);
                    }
                    break;
                case NAMELESSLINK:
                    WebLink[] links = response.getLinks();
                    WebLink wantedLink = null;
                    for (WebLink currentLink : links) {
                        if (currentLink.getURLString().equals(name)) {
                            wantedLink = currentLink;
                        }
                    }
                    if (existing) {
                        TestCase.assertNotNull(userName + ": The namelessLink '" + name + "' does not exist" + "\n\n" + response.getText(), wantedLink);
                    } else {
                        TestCase.assertNull(userName + ": The namelessLink '" + name + "' does exist" + "\n\n" + response.getText(), wantedLink);
                    }
                    break;
                case TEXT:
                    String text = response.getText();
                    Pattern p1 = Pattern.compile(name, Pattern.DOTALL);
                    Matcher m1 = p1.matcher(text);
                    boolean matchesText = m1.find();
                    if (existing) {
                        TestCase.assertTrue(userName + ": The regular expression '" + name + "' cannot not be matched againt the text" + "\n\n" + response.getText(), matchesText);
                    } else {
                        TestCase.assertFalse(userName + ": The regular expression '" + name + "' can be matched to the text" + "\n\n" + response.getText(), matchesText);
                    }
                    break;
                case DOWNLOAD:
                    FileInputStream fis = new java.io.FileInputStream(name);
                    if (fis == null) throw new SecurityException("The file '" + name + "' could not be opened");
                    String downloadHash = getMD5CheckSum(response.getInputStream());
                    String fileHash = getMD5CheckSum(fis);
                    if (existing) {
                        TestCase.assertEquals("The checksums are not equal", downloadHash, fileHash);
                    } else {
                        TestCase.assertNotSame("The checksums are equal", downloadHash, fileHash);
                    }
                    break;
                case PDF:
                    PDFParser parser = new PDFParser(response.getInputStream());
                    parser.parse();
                    PDFTextStripper stripper = new PDFTextStripper();
                    PDDocument doc = parser.getPDDocument();
                    String pdftext = stripper.getText(doc);
                    Pattern p2 = Pattern.compile(name, Pattern.DOTALL);
                    Matcher m2 = p2.matcher(pdftext);
                    boolean matchesPdf = m2.find();
                    if (existing) {
                        TestCase.assertTrue("The regular expression '" + name + "' cannot be matched against the PDF text '" + pdftext + "'", matchesPdf);
                    } else {
                        TestCase.assertFalse("The regular expression '" + name + "' can be matched against the PDF text '" + pdftext + "'", matchesPdf);
                    }
                    doc.close();
                    break;
                default:
                    throw new SecurityException(type.toString() + " is not a valid type for comparison");
            }
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * generates an MD5 hash of a given input stream
	 * 
	 * @param is the input stream to be used
	 * @return the hash as a string
	 */
    @SuppressWarnings("nls")
    private String getMD5CheckSum(java.io.InputStream is) {
        byte[] buffer = new byte[1024];
        String checkSum = new String();
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            int numRead;
            do {
                numRead = is.read(buffer);
                if (numRead > 0) {
                    digest.update(buffer, 0, numRead);
                }
            } while (numRead != -1);
            is.close();
            checkSum = new String(digest.digest());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return checkSum;
    }

    /**
	 * make the current thread sleep for a certain time
	 * 
	 * @param maxSeconds number of seconds to wait
	 * @param user name of the 
	 */
    @SuppressWarnings("nls")
    public static void waiting(int maxSeconds, String userName) {
        if (maxSeconds > 0) {
            Random random = new Random();
            int randomNumber = random.nextInt((maxSeconds * 1000) - 500);
            logger.debug(userName + ": Waiting for " + (randomNumber + 500) + " milliseconds");
            try {
                Thread.sleep(randomNumber + 500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
