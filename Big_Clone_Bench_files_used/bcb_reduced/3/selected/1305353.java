package com.enerjy.index.plugin.eclipse.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import com.enerjy.common.EnerjyException;
import com.enerjy.common.ext.StringEscapeUtils;
import com.enerjy.common.util.StreamUtils;

class ShareDialog extends TitleAreaDialog {

    private static final String DLG_NAME = "shareDialog.name";

    private static final String DLG_EMAIL = "shareDialog.email";

    private static final String DLG_MESSAGE = "shareDialog.message";

    private static final int VERTICAL_INDENT = 15;

    private static final int WIDTH_HINT = 100;

    private static final int MAX_EMAIL = 100;

    private static final int MAX_MESSAGE = 500;

    private static final String MESSAGE_DEFAULT = "Check out my code's Enerjy Index score!";

    private static final String PREVIEW = "<html><body style=\"font-size: small;\"><div style=\"font-style: italic;\">From: Enerjy Index &lt;index@enerjy.com&gt;" + "<br/>Reply-To: {0} &lt;{1}&gt;<br/>To: {2}<br/>Subject: {0}''s Enerjy Index score</div>" + "<div bgcolor=\"#ffffff\">\n" + "    <div align=\"center\" style=\"margin-top: 30px\">\n" + "        <table width=\"600\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" bgcolor=\"#ffffff\">\n" + "            <tr>\n" + "                <td valign=\"middle\" bgcolor=\"#000000\" width=\"600\" height=\"79\" style=\"color: #ffffff; font-size: 14px; text-indent: 30px;\">\n" + "                    <img src=\"http://www.enerjy.com/share/barswide3.jpg\" alt=\"What''s your Enerjy Score?\"  style=\"display: block;\">\n" + "                </td>\n" + "            </tr>\n" + "\n" + "            <tr>\n" + "                <td valign=\"top\">\n" + "                    <table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" bgcolor=\"#ffffff\" style=\"border-left: 1px solid #6D7073; border-right: 1px solid #6D7073;\">\n" + "\n" + "                        <tr>\n" + "                            <td align=\"left\" valign=\"top\" width=\"30\">\n" + "                                <img src=\"http://www.enerjy.com/share/spacer_20h.gif\" alt=\"\">\n" + "                            </td>\n" + "\n" + "                            <td valign=\"top\" align=\"left\" width=\"540\">\n" + "                                <p style=\"font-size: 12px; margin: 0px auto; padding: 0px;\">&nbsp;</p>\n" + "                                <p style=\"font-family: verdana, tahoma, sans-serif; font-size: 11px; margin: 0px 0px 12px; color: #4D4D4D; line-height: 1.4em;\">\n" + "                                {3}\n" + "                                </p>\n" + "\n" + "                                <table cellspacing=\"2\" cellpadding=\"0\" border=\"0\"><tr>\n" + "                                  <td><p style=\"font-family: verdana, tahoma, sans-serif; font-size: 11px; margin: 15px 0px 15px 20px; color: #4D4D4D; line-height: 1.4em;\">\n" + "                                      My Enerjy Score:</p></td>\n" + "                                  <td><img src=\"http://www.enerjy.com/share/{5}.gif\" /></td>\n" + "                                </tr></table>\n" + "\n" + "                                <p style=\"font-family: verdana, tahoma, sans-serif; font-size: 11px; margin: 0px 0px 30px; color: #4D4D4D; line-height: 1.4em;\">\n" + "                                You can see how your Java code stacks up by downloading Enerjy yourself at <a href=\"http://www.enerjy.com\" style=\"color: #148498\">enerjy.com</a>. Once you know your Enerjy Score, be sure to let me know!\n" + "                                </p>\n" + "\n" + "                            </td>\n" + "                            <td align=\"left\" valign=\"top\" width=\"30\">\n" + "                                <img src=\"http://www.enerjy.com/share/spacer_20h.gif\" alt=\"\">\n" + "                            </td>\n" + "                        </tr>\n" + "\n" + "                        <tr>\n" + "                            <td colspan=\"3\">\n" + "                                <p style=\"font-family: verdana, tahoma, sans-serif; font-size: 10px; margin: 0px auto; padding: 0px; color: #CC5600; text-align: center;\">\n" + "                                Enerjy Software &bull; 900 Cummings Center, #326T &bull; Beverly, MA 01915 &bull; 866-598-9876</p>\n" + "                                <p style=\"font-size: 12px; margin: 0px auto; padding: 0px;\">&nbsp;</p>\n" + "                            </td>\n" + "                        </tr>\n" + "\n" + "\n" + "                    </table>\n" + "                </td>\n" + "            </tr>\n" + "\n" + "\n" + "            <tr>\n" + "                <td bgcolor=\"#000\" width=\"600\" height=\"22\">\n" + "                    <img src=\"http://www.enerjy.com/share/roundbottom.jpg\" alt=\"\" style=\"display: block\">\n" + "                </td>\n" + "            </tr>\n" + "\n" + "        </table>\n" + "    </div>\n" + "</div></body></html>\n";

    private final String score;

    private Browser browser = null;

    private Text name = null;

    private Text email = null;

    private Text recpt = null;

    private Text message = null;

    ShareDialog(Shell parentShell, String score) {
        super(parentShell);
        this.score = score;
        setShellStyle(SWT.SHELL_TRIM | SWT.RESIZE);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite main = new Composite(parent, SWT.NONE);
        main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        main.setLayout(new GridLayout(1, true));
        Label info = new Label(main, SWT.WRAP);
        GridData gd = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        gd.widthHint = WIDTH_HINT;
        info.setLayoutData(gd);
        info.setText("Share your workspace's Enerjy Index value with others by entering their email address below.  An email will " + "be sent with the contents below.  You can include multiple recipients by separating each with a comma.");
        Composite controls = new Composite(main, SWT.NONE);
        controls.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true, false));
        GridLayout layout = new GridLayout(2, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        controls.setLayout(layout);
        Label prompt = new Label(controls, SWT.NONE);
        prompt.setText("Your name:");
        name = new Text(controls, SWT.BORDER);
        gd = new GridData(SWT.DEFAULT, SWT.DEFAULT, false, false);
        gd.widthHint = WIDTH_HINT * 2;
        name.setLayoutData(gd);
        prompt = new Label(controls, SWT.NONE);
        prompt.setText("Your email:");
        email = new Text(controls, SWT.BORDER);
        gd = new GridData(SWT.DEFAULT, SWT.DEFAULT, false, false);
        gd.widthHint = WIDTH_HINT * 2;
        email.setLayoutData(gd);
        prompt = new Label(controls, SWT.NONE);
        prompt.setText("Recipient(s):");
        Composite panel = new Composite(controls, SWT.NONE);
        GridLayout inner = new GridLayout(2, false);
        inner.marginHeight = 0;
        inner.marginWidth = 0;
        panel.setLayout(inner);
        panel.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true, false));
        recpt = new Text(panel, SWT.BORDER);
        recpt.setTextLimit(MAX_EMAIL);
        gd = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        recpt.setLayoutData(gd);
        Button address = new Button(panel, SWT.NONE);
        address.setText("Import...");
        address.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                ContactsDialog dlg = new ContactsDialog(getShell());
                if (Dialog.OK != dlg.open()) {
                    return;
                }
                String[] items = dlg.getEmails();
                if (0 == items.length) {
                    return;
                }
                StringBuffer val = new StringBuffer(recpt.getText());
                if (0 != val.length() && !val.toString().endsWith(",")) {
                    val.append(',');
                }
                val.append(items[0]);
                for (int x = 1; x < items.length; x++) {
                    val.append(',').append(items[x]);
                }
                recpt.setText(val.toString());
            }
        });
        prompt = new Label(controls, SWT.NONE);
        prompt.setText("Brief message:");
        prompt.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
        message = new Text(controls, SWT.BORDER | SWT.MULTI);
        message.setTextLimit(MAX_MESSAGE);
        gd = new GridData(SWT.FILL, SWT.FILL, true, false);
        gd.widthHint = WIDTH_HINT;
        gd.heightHint = VERTICAL_INDENT * 3;
        message.setLayoutData(gd);
        Group group = new Group(main, SWT.SHADOW_IN);
        gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        gd.verticalIndent = VERTICAL_INDENT;
        gd.heightHint = VERTICAL_INDENT * 5;
        group.setLayoutData(gd);
        group.setLayout(new GridLayout(1, true));
        group.setText("Preview:");
        browser = new Browser(group, SWT.BORDER);
        browser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        ModifyListener listener = new ModifyListener() {

            public void modifyText(ModifyEvent e) {
                updatePreview();
            }
        };
        name.addModifyListener(listener);
        email.addModifyListener(listener);
        recpt.addModifyListener(listener);
        message.addModifyListener(listener);
        browser.addLocationListener(new LocationAdapter() {

            @Override
            public void changing(LocationEvent event) {
                if (event.location.startsWith("http:")) {
                    event.doit = false;
                }
            }
        });
        updateFieldDefaults();
        updatePreview();
        getShell().setText("Share your Enerjy Index score");
        setTitle("Email Your Enerjy Index");
        setMessage("Enter your details and recipient(s) to email your Enerjy Index score");
        if (0 != email.getText().length()) {
            recpt.setFocus();
        }
        return main;
    }

    @Override
    protected void okPressed() {
        String nameVal = name.getText().trim();
        String emailVal = email.getText().trim();
        String recptVal = recpt.getText().trim();
        String missing = "";
        if (0 == nameVal.length()) {
            missing = "Name";
        }
        if (0 == emailVal.length()) {
            if (0 != missing.length()) {
                missing += ", ";
            }
            missing += "Your email";
        }
        if (0 == recptVal.length()) {
            if (0 != missing.length()) {
                missing += ", ";
            }
            missing += "Recipient(s)";
        }
        if (0 != missing.length()) {
            String fieldTxt = (-1 == missing.indexOf(',')) ? "field is" : "fields are";
            MessageDialog.openError(getShell(), "Missing required information", "The following " + fieldTxt + " required: " + missing);
            setMessage("Missing required information: " + missing, IMessageProvider.ERROR);
            return;
        }
        IDialogSettings settings = IndexActivator.getDefault().getDialogSettings();
        settings.put(DLG_NAME, nameVal);
        settings.put(DLG_EMAIL, emailVal);
        settings.put(DLG_MESSAGE, message.getText().trim());
        String send = "";
        try {
            send = sendMail();
        } catch (IOException e) {
            send = e.getMessage();
        } catch (EnerjyException e) {
            send = e.getMessage();
        }
        if (null != send) {
            MessageDialog.openError(getShell(), "Error Sending Email", "The following error occurred while attempting to send the email:\n" + send);
            setMessage("Error sending email: " + send, IMessageProvider.ERROR);
            return;
        }
        MessageDialog.openInformation(getShell(), "Message Sent", "Your email message was successfully sent.");
        super.okPressed();
    }

    private void updateFieldDefaults() {
        IDialogSettings settings = IndexActivator.getDefault().getDialogSettings();
        updateField(name, settings, DLG_NAME, System.getProperty("user.name", ""));
        updateField(email, settings, DLG_EMAIL, "");
        updateField(message, settings, DLG_MESSAGE, MESSAGE_DEFAULT);
    }

    private void updatePreview() {
        String msg = StringEscapeUtils.escapeHtml(message.getText());
        String text = MessageFormat.format(PREVIEW, name.getText(), email.getText(), recpt.getText(), msg, score, calcScoreImage());
        browser.setText(text);
    }

    private String sendMail() throws IOException {
        String msg = StringEscapeUtils.escapeHtml(message.getText());
        StringBuffer buf = new StringBuffer();
        buf.append(encode("n", name.getText()));
        buf.append("&").append(encode("e", email.getText()));
        buf.append("&").append(encode("r", recpt.getText()));
        buf.append("&").append(encode("m", msg));
        buf.append("&").append(encode("s", score));
        buf.append("&").append(encode("i", calcScoreImage()));
        buf.append("&").append(encode("c", digest(recpt.getText() + "_" + score)));
        URL url = new URL("http://www.enerjy.com/share/mailit.php");
        URLConnection conn = url.openConnection();
        conn.setDoOutput(true);
        OutputStreamWriter writer = null;
        BufferedReader reader = null;
        boolean haveOk = false;
        try {
            writer = new OutputStreamWriter(conn.getOutputStream());
            writer.write(buf.toString());
            writer.flush();
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            for (String line = reader.readLine(); null != line; line = reader.readLine()) {
                if (line.startsWith("err:")) {
                    return line.substring(4);
                } else if (line.equals("ok")) {
                    haveOk = true;
                }
            }
        } finally {
            StreamUtils.close(writer);
            StreamUtils.close(reader);
        }
        if (!haveOk) {
            return "The server did not return a response.";
        }
        return null;
    }

    private String calcScoreImage() {
        int scoreVal = -1;
        try {
            scoreVal = (int) Math.floor(Double.parseDouble(score) * 10.0);
        } catch (NumberFormatException e) {
            scoreVal = -1;
        }
        String img = "NaN";
        if (-1 != scoreVal) {
            img = String.valueOf(scoreVal);
        }
        return img;
    }

    private static void updateField(Text field, IDialogSettings settings, String key, String defaultVal) {
        String val = settings.get(key);
        if (null == val) {
            val = defaultVal;
        }
        if (null == val) {
            val = "";
        }
        field.setText(val);
    }

    private static String digest(String val) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(val.trim().getBytes());
            byte[] digest = md.digest();
            StringBuffer buf = new StringBuffer();
            for (byte b : digest) {
                String hexString = Integer.toHexString(b);
                int length = hexString.length();
                if (length > 2) {
                    hexString = hexString.substring(length - 2, length);
                } else if (length < 2) {
                    hexString = "0" + hexString;
                }
                buf.append(hexString);
            }
            return buf.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new EnerjyException("Could not create digest: MD5", e);
        }
    }

    private static String encode(String key, String val) {
        try {
            return URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(val.trim(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new EnerjyException("Bad encoding: UTF-8", e);
        }
    }
}
