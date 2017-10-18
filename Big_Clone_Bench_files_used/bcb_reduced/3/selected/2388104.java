package de.cyrus.cuonAddress;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import org.apache.http.conn.HttpHostConnectException;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import de.cyrus.cuonAddress.SimpleGestureFilter.SimpleGestureListener;

public class cuonAddressActivity extends Activity implements SimpleGestureListener, constants {

    private EditText cuonnameField;

    private EditText cuonPasswordField;

    private EditText cuonHostField;

    private EditText cuonClientField;

    private EditText status;

    private String Password;

    private String CuonUsername;

    private String CuonPassword;

    private String CuonHost;

    private String CuonClient;

    private String singleUserUUID;

    private XMLRPCClient client;

    private URI uri;

    private URI cuonUri;

    private EditText tvLocal;

    private EditText eSendText;

    private Spinner spinnerActions;

    private ArrayAdapter<CharSequence> adapter_actions;

    private ArrayAdapter<CharSequence> adapter_say;

    public Timer timer1;

    public int counter;

    public Handler aliveHandler;

    public String sCut = "\\|";

    public static final String PREFS_NAME = "cuonAddress-Prefs";

    private HashMap cuonUser;

    private String activeTab;

    private String activePartnerTab;

    private int activeModul;

    private boolean cuonMode = false;

    private SimpleGestureFilter detector;

    private cuon oCuon;

    private dms oDMS;

    private notes oNotes;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cuonUser = new HashMap();
        oCuon = new cuon();
        oDMS = new dms();
        oNotes = new notes();
        activeTab = "LocalChat";
        activeModul = 0;
        setContentView(R.layout.main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        detector = new SimpleGestureFilter(this, this);
        StringBuilder builder = new StringBuilder();
        adapter_actions = ArrayAdapter.createFromResource(this, R.array.actions_array, android.R.layout.simple_spinner_item);
        List<CharSequence> new_says = new ArrayList<CharSequence>();
        adapter_say = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, new_says);
        adapter_say.add("Local Chat");
        status = (EditText) findViewById(R.id.status);
        cuonnameField = (EditText) findViewById(R.id.eCuonUser);
        cuonPasswordField = (EditText) findViewById(R.id.eCuonPassword);
        cuonHostField = (EditText) findViewById(R.id.eCuonHost);
        cuonClientField = (EditText) findViewById(R.id.eCuonClient);
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        cuonnameField.setText(settings.getString("CuonUsername", "No"));
        cuonPasswordField.setText(settings.getString("CuonPassword", "No"));
        cuonHostField.setText(settings.getString("CuonHost", "No"));
        cuonClientField.setText(settings.getString("CuonClient", "No"));
        Timer timer1 = new Timer();
        counter = 1;
        singleUserUUID = new String();
        System.out.println("Hello==");
        aliveHandler = new Handler();
    }

    public void cuonMain() {
        oCuon.start();
        oDMS.start();
        oNotes.start();
        System.out.println("cuon is running = ");
        setContentView(R.layout.cuon_main);
    }

    public void setXmlTextFieldsAddress() {
        oCuon.eAddress = (EditText) findViewById(R.id.eAddress);
        oCuon.ePhone1 = (EditText) findViewById(R.id.ePhone1);
        oCuon.ePhone2 = (EditText) findViewById(R.id.ePhone2);
        oCuon.eEmail1 = (EditText) findViewById(R.id.eEmail1);
    }

    public void setXmlTextFieldsDMS(int newModul) {
        oDMS.eTitle = (EditText) findViewById(R.id.eDmsTitle);
        oDMS.eCategory = (EditText) findViewById(R.id.eDmsCategory);
        oDMS.eSub = (EditText) findViewById(R.id.eDmsSubwords);
        oDMS.activeModul = newModul;
        if (newModul == mCuonAddress1) {
            activeModul = mCuonDmsAddress1;
            oDMS.searchAddressInfo(oCuon.actualAddressID);
        } else if (newModul == mCuonPartner1) {
            activeModul = mCuonDmsPartner1;
            oDMS.searchAddressInfo(oCuon.actualPartnerID);
        }
    }

    public void onbSOKclicked(View view) {
        System.out.println("cuon ok click ");
        String s1 = ((EditText) findViewById(R.id.eSearch1)).getText().toString();
        String s2 = ((EditText) findViewById(R.id.eSearch2)).getText().toString();
        String s3 = ((EditText) findViewById(R.id.eSearch3)).getText().toString();
        String s4 = ((EditText) findViewById(R.id.eSearch4)).getText().toString();
        setContentView(R.layout.cuon_out_address1);
        setXmlTextFieldsAddress();
        activeModul = mCuonAddress1;
        oCuon.searchAdress(s1, s2, s3, s4);
    }

    public void on_partner_clicked(View view) {
        System.out.println("cuon is running = ");
        setContentView(R.layout.cuon_out_partner1);
        System.out.println("Search partner ");
        EditText ePartnerAddress = (EditText) findViewById(R.id.ePartnerAddress);
        EditText ePhone1 = (EditText) findViewById(R.id.ePartnerPhone1);
        EditText ePhone2 = (EditText) findViewById(R.id.ePartnerPhone2);
        EditText ePhone3 = (EditText) findViewById(R.id.ePartnerPhone3);
        activeModul = mCuonPartner1;
        oCuon.searchPartner(ePartnerAddress, ePhone1, ePhone2, ePhone3);
    }

    public void on_bNotes_clicked(View view) {
        System.out.println("cuon is running = ");
        setContentView(R.layout.cuon_notes);
        System.out.println("Show notes ");
        oNotes.liTitle = getResources().getStringArray(R.array.notes_title);
        oNotes.eTitle = (EditText) findViewById(R.id.name_of_note);
        oNotes.eText = (EditText) findViewById(R.id.note_text);
        activeModul = mCuonAddressNote;
        oNotes.activeModul = mCuonAddressNote;
        oNotes.show(oCuon.actualAddressID);
    }

    public void on_info_search_clicked(View view) {
        System.out.println("dms is running = ");
        setContentView(R.layout.cuon_dms);
        oDMS.eSearchTitle = (EditText) findViewById(R.id.eDmsSearchTitle);
        oDMS.eSearchCategory = (EditText) findViewById(R.id.eDmsSearchCategory);
        oDMS.eSearchSub = (EditText) findViewById(R.id.eDmsSearchSubwords);
    }

    public void on_address_info_search_clicked(View view) {
        oDMS.esTitle = oDMS.eSearchTitle.getText().toString();
        oDMS.esCategory = oDMS.eSearchCategory.getText().toString();
        oDMS.esSub = oDMS.eSearchSub.getText().toString();
        setContentView(R.layout.cuon_out_dms);
        setXmlTextFieldsDMS(activeModul);
    }

    public void on_dms_load_clicked(View view) {
        System.out.println("dms load = ");
        loadInfo();
    }

    public void loadInfo() {
        if (oDMS.actualDmsID > 0) {
            System.out.println("Show dms Info" + oDMS.actualDmsID);
            XMLRPCMethod method = new XMLRPCMethod("DMS.sl_loadPDF", new XMLRPCMethodCallback() {

                public void callFinished(Object result) {
                    File sPDF = new File("none");
                    String Data = (String) result;
                    oDMS.actualPDF = oDMS.saveFile(Data);
                    try {
                        loadDocInReader(oDMS.actualPDF);
                    } catch (Exception ex) {
                        System.out.println("error dms20");
                        System.out.println(ex + " " + ex.getClass().getName() + " " + ex.getMessage());
                    }
                }
            });
            Object[] params = { cuonUser, oDMS.actualDmsID };
            method.call(params);
        }
    }

    public void on_phone1_dial(View view) {
        System.out.println("Phone 1 is clicked ");
        EditText ePhone1 = (EditText) findViewById(R.id.ePhone1);
        String Number = ePhone1.getText().toString();
        call(Number);
    }

    public void on_phone2_dial(View view) {
        System.out.println("Phone 2 is clicked ");
        EditText ePhone2 = (EditText) findViewById(R.id.ePhone2);
        String Number = ePhone2.getText().toString();
        call(Number);
    }

    public void on_email1_clicked(View view) {
        System.out.println("Email 1 is clicked ");
        EditText eEmail1 = (EditText) findViewById(R.id.eEmail1);
        String email_address = eEmail1.getText().toString();
        send_email(email_address);
    }

    public void on_partner_phone1_dial(View view) {
        System.out.println("Partner Phone 1 is clicked ");
        EditText ePartnerPhone1 = (EditText) findViewById(R.id.ePartnerPhone1);
        String Number = ePartnerPhone1.getText().toString();
        call(Number);
    }

    public void on_partner_phone2_dial(View view) {
        System.out.println("Partner Phone 2 is clicked ");
        EditText ePartnerPhone2 = (EditText) findViewById(R.id.ePartnerPhone2);
        String Number = ePartnerPhone2.getText().toString();
        call(Number);
    }

    public void on_partner_phone3_dial(View view) {
        System.out.println("Partner Phone 3 is clicked ");
        EditText ePartnerPhone3 = (EditText) findViewById(R.id.ePartnerPhone3);
        String Number = ePartnerPhone3.getText().toString();
        call(Number);
    }

    public void on_partner_email_clicked(View view) {
        System.out.println("Email 1 is clicked ");
        EditText eEmail1 = (EditText) findViewById(R.id.ePartnerEmail1);
        String email_address = eEmail1.getText().toString();
        send_email(email_address);
    }

    public void call(String Number) {
        try {
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + Number));
            startActivity(callIntent);
        } catch (ActivityNotFoundException ae) {
            System.out.println("Call failed" + ae.getMessage());
        }
    }

    public void send_email(String email_address) {
        Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
        String[] recipients = new String[] { email_address, "" };
        emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, recipients);
        emailIntent.setType("text/plain");
        startActivity(Intent.createChooser(emailIntent, "Send mail..."));
    }

    public void on_bSCancel_clicked(View view) {
        setContentView(R.layout.main);
    }

    public void on_exit_to_cuon(View view) {
        setContentView(R.layout.cuon_main);
    }

    public void on_exit_to_address(View view) {
        setContentView(R.layout.cuon_out_address1);
        setXmlTextFieldsAddress();
        oCuon.setAddressFields(oCuon.actualIndex);
    }

    public void on_exit_to_dms(View view) {
        setContentView(R.layout.cuon_dms);
    }

    public void on_bCancelNotes_clicked(View view) {
        setContentView(R.layout.cuon_out_address1);
        setXmlTextFieldsAddress();
        oCuon.setAddressFields(oCuon.actualIndex);
    }

    public boolean dispatchTouchEvent(MotionEvent me) {
        this.detector.onTouchEvent(me);
        return super.dispatchTouchEvent(me);
    }

    public void onSwipe(int direction) {
        String str = "";
        switch(direction) {
            case SimpleGestureFilter.SWIPE_RIGHT:
                switch(activeModul) {
                    case mCuon:
                    case mCuonAddress1:
                    case mCuonPartner1:
                        oCuon.swipeRight();
                        break;
                    case mCuonDmsAddress1:
                    case mCuonDmsPartner1:
                        oDMS.swipeRight();
                        break;
                    case mCuonAddressNote:
                        oNotes.swipeRight();
                        break;
                }
                break;
            case SimpleGestureFilter.SWIPE_LEFT:
                switch(activeModul) {
                    case mCuon:
                    case mCuonAddress1:
                    case mCuonPartner1:
                        oCuon.swipeLeft();
                        break;
                    case mCuonDmsAddress1:
                    case mCuonDmsPartner1:
                        oDMS.swipeLeft();
                        break;
                    case mCuonAddressNote:
                        oNotes.swipeLeft();
                        break;
                }
                break;
            case SimpleGestureFilter.SWIPE_DOWN:
                break;
            case SimpleGestureFilter.SWIPE_UP:
                break;
        }
    }

    public void onDoubleTap() {
        switch(activeModul) {
            case mCuonAddress1:
                try {
                    String actualAddress = oCuon.getActualAddress();
                    String[] liAddress = actualAddress.split("\n");
                    int iSize = liAddress.length;
                    String Street = liAddress[3].replace(" ", "+");
                    String City = liAddress[4].replace(" ", "+");
                    System.out.println("Address = " + Street + "+" + City);
                    Intent viewIntent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + liAddress[0] + "+" + Street + "+" + City));
                    startActivity(viewIntent);
                } catch (ActivityNotFoundException ae) {
                    System.out.println("Call failed" + ae.getMessage());
                }
                break;
        }
    }

    public void onBackToMain(View view) {
        setContentView(R.layout.main);
    }

    public void onExit(View view) {
        this.finish();
    }

    private static String convertToHex(byte[] data) {
        StringBuffer buf = new StringBuffer();
        int length = data.length;
        for (int i = 0; i < length; ++i) {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9)) buf.append((char) ('0' + halfbyte)); else buf.append((char) ('a' + (halfbyte - 10)));
                halfbyte = data[i] & 0x0F;
            } while (++two_halfs < 1);
        }
        return buf.toString();
    }

    public static String SHA512(String text) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md;
        md = MessageDigest.getInstance("SHA-512");
        byte[] sha1hash = new byte[40];
        md.update(text.getBytes("UTF-8"), 0, text.length());
        sha1hash = md.digest();
        return convertToHex(sha1hash);
    }

    void loadDocInReader(File doc) throws ActivityNotFoundException, Exception {
        try {
            Intent intent = new Intent();
            Uri path = Uri.fromFile(doc);
            System.out.println("dms load pdf viewer with " + path);
            intent.setPackage("com.adobe.reader");
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.setDataAndType(path, "application/pdf");
            startActivity(intent);
        } catch (ActivityNotFoundException activityNotFoundException) {
            activityNotFoundException.printStackTrace();
            throw activityNotFoundException;
        } catch (Exception otherException) {
            otherException.printStackTrace();
            throw otherException;
        }
    }

    public void onLoginToGrid(View view) {
        counter = 0;
        CuonUsername = cuonnameField.getText().toString();
        CuonPassword = cuonPasswordField.getText().toString();
        CuonHost = cuonHostField.getText().toString();
        CuonClient = cuonClientField.getText().toString();
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("CuonUsername", CuonUsername);
        editor.putString("CuonPassword", CuonPassword);
        editor.putString("CuonHost", CuonHost);
        editor.putString("CuonClient", CuonClient);
        editor.commit();
        try {
            cuonUser.put("Name", CuonUsername);
            System.out.println("Connect to " + CuonHost);
            uri = URI.create(CuonHost);
            cuonUser.put("client", CuonClient);
        } catch (Exception cE) {
            System.out.println("Error0");
            System.out.println(cE + " " + cE.getClass().getName() + " " + cE.getMessage());
            uri = URI.create("localhost:9012");
        }
        client = new XMLRPCClient(uri);
        XMLRPCMethod cmethod = new XMLRPCMethod("Database.createSessionID", new XMLRPCMethodCallback() {

            public void callFinished(Object result) {
                System.out.println("cuon sid = " + result.toString());
                singleUserUUID = result.toString();
                cuonUser.put("SessionID", singleUserUUID);
                cuonUser.put("userType", "cuon");
                if (singleUserUUID.compareTo("Test") != 0) {
                    cuonMode = true;
                    oCuon.init(client, cuonUser);
                    oDMS.init(client, cuonUser);
                    oNotes.init(client, cuonUser);
                    System.out.println("init cuon");
                    checkVersion();
                    cuonMain();
                }
            }
        });
        try {
            System.out.println("sha512 hex = " + cuonUser.get("Name").toString() + ", " + SHA512(CuonPassword));
            Object[] cparams = { cuonUser.get("Name").toString(), CuonPassword };
            cmethod.call(cparams);
        } catch (NoSuchAlgorithmException nsa) {
            Object[] c1params = { "Test", "Test" };
            cmethod.call(c1params);
        } catch (UnsupportedEncodingException uee) {
            Object[] c1params = { "Test", "Test" };
            cmethod.call(c1params);
        }
    }

    public void checkVersion() {
        XMLRPCMethod method = new XMLRPCMethod("Database.getAnroidInfo", new XMLRPCMethodCallback() {

            public void callFinished(Object result) {
                System.out.println("cuon versioncheck = " + result.toString());
                if (Boolean.valueOf(result.toString())) {
                    System.out.println("cuon versioncheck 1 is true");
                } else {
                    System.out.println("cuon versioncheck 1 is false");
                    printToast("This is an old Version of the App., please download the new Version from www.cuon.org!");
                }
            }
        });
        Object[] params = { "Smartphone", "cuonAddress", cuonAddressVersion[0], cuonAddressVersion[1], cuonAddressVersion[2] };
        method.call(params);
    }

    void printToast(String msg) {
        Toast toast = Toast.makeText(this, msg, 2000);
        toast.show();
    }

    interface XMLRPCMethodCallback {

        void callFinished(Object result);
    }

    class XMLRPCMethod extends Thread {

        private String method;

        private Object[] params;

        private Handler handler;

        private XMLRPCMethodCallback callBack;

        public XMLRPCMethod(String method, XMLRPCMethodCallback callBack) {
            this.method = method;
            this.callBack = callBack;
            handler = new Handler();
        }

        public void call() {
            call(null);
        }

        public void call(Object[] params) {
            this.params = params;
            start();
        }

        @Override
        public void run() {
            try {
                final long t0 = System.currentTimeMillis();
                final Object result = client.callEx(method, params);
                final long t1 = System.currentTimeMillis();
                handler.post(new Runnable() {

                    public void run() {
                        callBack.callFinished(result);
                    }
                });
            } catch (final XMLRPCFault e) {
                handler.post(new Runnable() {

                    public void run() {
                        status.setTextColor(0xffff8080);
                        Log.d("Test", "error", e);
                    }
                });
            } catch (final XMLRPCException e) {
                handler.post(new Runnable() {

                    public void run() {
                        Throwable couse = e.getCause();
                        if (couse instanceof HttpHostConnectException) {
                            status.setText("Cannot connect to " + uri.getHost() + "\nMake sure server.py on your development host is running !!!");
                        } else {
                            status.setText("Error " + e.getMessage());
                        }
                        Log.d("Test", "error", e);
                    }
                });
            }
        }
    }
}
