import java.io.*;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

public class CheckSum {

    Display display = new Display();

    Shell shell = new Shell(display, SWT.ON_TOP | SWT.CLOSE | SWT.TITLE);

    Text file, checksum;

    Combo hash;

    Button browse, run;

    public CheckSum() {
        init();
        shell.setLocation(shell.getClientArea().width / 2, shell.getClientArea().height / 2);
        shell.pack();
        shell.open();
        while (!shell.isDisposed()) if (!display.readAndDispatch()) display.sleep();
        display.dispose();
    }

    private void init() {
        shell.setText("KG's Checksum");
        GridLayout layout = new GridLayout();
        layout.numColumns = 4;
        shell.setLayout(layout);
        GridData data = new GridData();
        Label hashlabel = new Label(shell, SWT.NONE);
        hashlabel.setText("Choose a hash fx:  ");
        data.horizontalSpan = 1;
        data.grabExcessHorizontalSpace = true;
        hashlabel.setLayoutData(data);
        hash = new Combo(shell, SWT.READ_ONLY);
        hash.setItems(new String[] { "MD5", "SHA-1", "Adler-32" });
        hash.select(0);
        data = new GridData();
        data.horizontalSpan = 2;
        data.grabExcessHorizontalSpace = true;
        hash.setLayoutData(data);
        Label label = new Label(shell, SWT.NONE);
        label.setText("Choose a file to perform a Checksum on:");
        data.horizontalSpan = 4;
        data.horizontalAlignment = GridData.FILL;
        data.verticalIndent = 5;
        data.grabExcessHorizontalSpace = true;
        label.setLayoutData(data);
        file = new Text(shell, SWT.NONE);
        data = new GridData();
        data.horizontalSpan = 3;
        data.horizontalAlignment = GridData.FILL;
        data.grabExcessHorizontalSpace = true;
        file.setLayoutData(data);
        browse = new Button(shell, SWT.PUSH);
        browse.setText("Browse");
        data = new GridData();
        data.horizontalSpan = 1;
        browse.setLayoutData(data);
        browse.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                FileDialog dialog = new FileDialog(shell, SWT.NULL);
                String path = dialog.open();
                if (path != null) {
                    File file = new File(path);
                    if (file.isFile()) displayFiles(new String[] { file.toString() }); else displayFiles(file.list());
                }
            }
        });
        run = new Button(shell, SWT.PUSH);
        run.setText("Checksum!");
        data = new GridData();
        data.horizontalAlignment = GridData.CENTER;
        data.horizontalSpan = 4;
        run.setLayoutData(data);
        run.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                int index = hash.getSelectionIndex();
                switch(index) {
                    case 0:
                        createMD5Checksum();
                        break;
                    case 1:
                        createSHA1Checksum();
                        break;
                    case 2:
                        createAdler32CheckSum();
                }
            }
        });
        checksum = new Text(shell, SWT.BORDER);
        checksum.setEditable(false);
        data = new GridData();
        data.horizontalAlignment = SWT.FILL;
        data.horizontalAlignment = SWT.FILL;
        data.horizontalSpan = 4;
        checksum.setLayoutData(data);
    }

    private void createAdler32CheckSum() {
        checksum.setText("Checking...");
        try {
            String filename = file.getText();
            FileInputStream fis = new FileInputStream(new File(filename));
            CheckedInputStream cis = new CheckedInputStream(fis, new CRC32());
            BufferedInputStream in = new BufferedInputStream(cis);
            while (in.read() != -1) {
            }
            checksum.setText(Long.toString(cis.getChecksum().getValue()));
        } catch (Exception e) {
            checksum.setText("Error:  " + e.getMessage());
        }
    }

    public void createMD5Checksum() {
        checksum.setText("Checking...");
        try {
            InputStream fis = new FileInputStream(file.getText());
            byte[] buffer = new byte[1024];
            MessageDigest complete = MessageDigest.getInstance("MD5");
            int numRead;
            do {
                numRead = fis.read(buffer);
                if (numRead > 0) {
                    complete.update(buffer, 0, numRead);
                }
            } while (numRead != -1);
            fis.close();
            checksum.setText(new BigInteger(1, complete.digest()).toString(16));
        } catch (Exception e) {
            checksum.setText("Error:  " + e.getMessage());
        }
    }

    public byte[] createChecksum(String filename) {
        try {
            InputStream fis = new FileInputStream(filename);
            byte[] buffer = new byte[1024];
            MessageDigest complete = MessageDigest.getInstance("SHA1");
            int numRead;
            do {
                numRead = fis.read(buffer);
                if (numRead > 0) {
                    complete.update(buffer, 0, numRead);
                }
            } while (numRead != -1);
            fis.close();
            return complete.digest();
        } catch (Exception e) {
            checksum.setText("Error:  " + e.getMessage());
        }
        return null;
    }

    public void createSHA1Checksum() {
        try {
            byte[] b = createChecksum(file.getText());
            String result = "";
            for (int i = 0; i < b.length; i++) {
                result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
            }
            checksum.setText(result);
        } catch (Exception e) {
            checksum.setText("Error:  " + e.getMessage());
        }
    }

    public void displayFiles(String[] files) {
        for (int i = 0; files != null && i < files.length; i++) {
            file.setText(files[i]);
            file.setEditable(true);
        }
    }

    public static void main(String[] args) {
        new CheckSum();
    }
}
