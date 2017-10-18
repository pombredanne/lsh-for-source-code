package de.cyrus.slandy;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import de.cyrus.slandy.SimpleGestureFilter.SimpleGestureListener;

public class SlandyActivity extends Activity implements SimpleGestureListener, constants {

    /** Called when the activity is first created. */
    private EditText lastnameField;

    private EditText firstnameField;

    private EditText passwordField;

    private EditText cuonnameField;

    private EditText cuonPasswordField;

    private EditText status;

    private Spinner spinner;

    private String Lastname;

    private String Firstname;

    private String Password;

    private String CuonUsername;

    private String CuonPassword;

    private String Grid;

    private String singleUserUUID;

    private XMLRPCClient client;

    private URI uri;

    private URI cuonUri;

    private EditText tvLocal;

    private EditText eSendText;

    private Spinner spinnerActions;

    private Spinner spinnerSay;

    private ArrayAdapter<CharSequence> adapter_actions;

    private ArrayAdapter<CharSequence> adapter_say;

    public Timer timer1;

    public int counter;

    public Handler aliveHandler;

    public String sCut = "\\|";

    public String cCut1 = Character.toString((char) 172);

    public String cCut2 = Character.toString((char) 175);

    public static final String PREFS_NAME = "Slandy-Prefs";

    private HashMap cuonUser;

    private HashMap tabs;

    public List<String> liTabs = new ArrayList<String>();

    private String activeTab;

    private int activeModul;

    private HashMap avatare;

    private HashMap currentParcel;

    private boolean cuonMode = false;

    private SimpleGestureFilter detector;

    private cuon oCuon;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cuonUser = new HashMap();
        tabs = new HashMap<String, String>();
        oCuon = new cuon();
        tabs.put("LocalChat", "Local Chat\n");
        liTabs.add("LocalChat");
        activeTab = "LocalChat";
        activeModul = 0;
        avatare = new HashMap<String, String>();
        setContentView(R.layout.main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        detector = new SimpleGestureFilter(this, this);
        StringBuilder builder = new StringBuilder();
        adapter_actions = ArrayAdapter.createFromResource(this, R.array.actions_array, android.R.layout.simple_spinner_item);
        List<CharSequence> new_says = new ArrayList<CharSequence>();
        adapter_say = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, new_says);
        adapter_say.add("Local Chat");
        spinner = (Spinner) findViewById(R.id.spinner1);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.grid_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        status = (EditText) findViewById(R.id.status);
        lastnameField = (EditText) findViewById(R.id.eLastname);
        firstnameField = (EditText) findViewById(R.id.eFirstname);
        passwordField = (EditText) findViewById(R.id.ePassword);
        cuonnameField = (EditText) findViewById(R.id.eCuonUser);
        cuonPasswordField = (EditText) findViewById(R.id.eCuonPassword);
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        lastnameField.setText(settings.getString("Lastname", "No"));
        firstnameField.setText(settings.getString("Firstname", "No"));
        passwordField.setText(settings.getString("Password", "No"));
        cuonnameField.setText(settings.getString("CuonUsername", "No"));
        cuonPasswordField.setText(settings.getString("CuonPassword", "No"));
        spinner.setSelection(settings.getInt("Grid", 0));
        Timer timer1 = new Timer();
        counter = 1;
        singleUserUUID = new String();
        System.out.println("Hello==");
        aliveHandler = new Handler();
        currentParcel = new HashMap<String, String>();
    }

    public void cuonMain() {
        oCuon.start();
        System.out.println("cuon is running = ");
        setContentView(R.layout.cuon_main);
    }

    public void onbSOKclicked(View view) {
        System.out.println("cuon ok click ");
        String s1 = ((EditText) findViewById(R.id.eSearch1)).getText().toString();
        String s2 = ((EditText) findViewById(R.id.eSearch2)).getText().toString();
        String s3 = ((EditText) findViewById(R.id.eSearch3)).getText().toString();
        String s4 = ((EditText) findViewById(R.id.eSearch4)).getText().toString();
        setContentView(R.layout.cuon_out_address1);
        EditText eAddress = (EditText) findViewById(R.id.eAddress);
        EditText ePhone1 = (EditText) findViewById(R.id.ePhone1);
        EditText ePhone2 = (EditText) findViewById(R.id.ePhone2);
        oCuon.searchAdress(s1, s2, s3, s4, eAddress, ePhone1, ePhone2);
    }

    public void on_phone1_dial(View view) {
        System.out.println("Phone 1 is clicked ");
        EditText ePhone1 = (EditText) findViewById(R.id.ePhone1);
        String Number = ePhone1.getText().toString();
        call(Number);
    }

    public void on_phone2_dial(View view) {
        System.out.println("Phone 1 is clicked ");
        EditText ePhone2 = (EditText) findViewById(R.id.ePhone2);
        String Number = ePhone2.getText().toString();
        call(Number);
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

    public void on_bSCancel_clicked(View view) {
        setContentView(R.layout.main);
    }

    public void on_exit_to_cuon(View view) {
        setContentView(R.layout.cuon_main);
    }

    public boolean dispatchTouchEvent(MotionEvent me) {
        this.detector.onTouchEvent(me);
        return super.dispatchTouchEvent(me);
    }

    public void onSwipe(int direction) {
        String str = "";
        int pos = liTabs.indexOf(activeTab);
        int lastPos = liTabs.size() - 1;
        switch(direction) {
            case SimpleGestureFilter.SWIPE_RIGHT:
                switch(activeModul) {
                    case mLocalChat:
                    case mIM:
                    case mGroupIM:
                    case 3:
                        if (pos > 0) {
                            setNewText(liTabs.get(pos - 1).toString());
                        }
                        break;
                    case mCuon:
                        oCuon.swipeRight();
                        break;
                }
                break;
            case SimpleGestureFilter.SWIPE_LEFT:
                switch(activeModul) {
                    case 0:
                    case 1:
                    case 2:
                    case 3:
                        if (pos < lastPos) {
                            setNewText(liTabs.get(pos + 1).toString());
                        }
                        break;
                }
                break;
            case SimpleGestureFilter.SWIPE_DOWN:
                break;
            case SimpleGestureFilter.SWIPE_UP:
                break;
        }
        Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
    }

    public void onDoubleTap() {
        Toast.makeText(this, "Double Tap", Toast.LENGTH_SHORT).show();
    }

    private Runnable new_alive = new Runnable() {

        public void run() {
            if (!singleUserUUID.isEmpty()) {
                XMLRPCMethod method = new XMLRPCMethod("Slandy.get_alive", new XMLRPCMethodCallback() {

                    public void callFinished(Object result) {
                        try {
                            Map<String, String> data = (Map<String, String>) result;
                            String localChat = data.get("Chat").toString();
                            if (localChat.trim().compareTo("NONE") != 0) {
                                String[] liChat = localChat.split(sCut);
                                for (String sChat : liChat) {
                                    if (activeTab == "LocalChat") {
                                        tvLocal.append(sChat.replace(sCut, ""));
                                    } else {
                                        tabs.put("LocalChat", tabs.get("LocalChat").toString() + sChat.replace(sCut, ""));
                                    }
                                }
                            }
                            String cpChat = data.get("CurrentParcel").toString();
                            System.out.println("CurrentParcel = " + cpChat);
                            if (cpChat.trim().compareTo("NONE") != 0) {
                                String[] liChat = cpChat.split(sCut);
                                for (String sChat : liChat) {
                                    System.out.println("CurrentParcel2 = " + sChat);
                                    String[] liS = sChat.split(cCut2);
                                    for (String cChat : liS) {
                                        System.out.println("CurrentParcel3 = " + cChat);
                                        String[] liO = cChat.split(cCut1);
                                        System.out.println("CurrentParcel4 = " + liO[0] + ", " + liO[1]);
                                        currentParcel.put(liO[0].toString(), liO[1].toString());
                                        System.out.println("CurrentParcel5");
                                    }
                                }
                                setNewParcelInfo();
                            }
                            String imChat = data.get("IM").toString();
                            System.out.println("IM = " + imChat);
                            if (imChat.trim().compareTo("NONE") != 0) {
                                String[] liChat = imChat.split(sCut);
                                for (String sChat : liChat) {
                                    int firstReturn = sChat.indexOf("\n");
                                    System.out.println("firstReturn = " + firstReturn);
                                    String UUID = sChat.substring(0, 36);
                                    System.out.println("UUID = " + UUID);
                                    String Name = sChat.substring(36, firstReturn);
                                    System.out.println("Name = " + Name);
                                    String sendDate = sChat.substring(firstReturn + 1, firstReturn + 6);
                                    String Msg = sChat.substring(firstReturn + 7);
                                    System.out.println("IM2 = " + UUID + ", " + Name + ", " + Msg + ", " + sendDate);
                                    String newMsg = sendDate + " " + Name + ":" + Msg;
                                    if (tabs.containsKey(UUID)) {
                                        String newIM = tabs.get(UUID).toString();
                                        tabs.put(UUID, newIM + newMsg);
                                    } else {
                                        tabs.put(UUID, newMsg);
                                        liTabs.add(UUID);
                                        avatare.put(Name, UUID);
                                        addName2Spinner(Name);
                                    }
                                    if (activeTab == UUID) {
                                        tvLocal.setText(tabs.get(UUID).toString());
                                    }
                                    System.out.println("Tab4 = " + Name);
                                }
                            }
                        } catch (Exception sE) {
                            System.out.println("Error1");
                            System.out.println(sE + " " + sE.getClass().getName() + " " + sE.getMessage());
                        }
                    }
                });
                Object[] params = { singleUserUUID };
                method.call(params);
            }
            aliveHandler.postDelayed(this, 3000);
        }
    };

    private void setNewParcelInfo() {
        System.out.println("cp name " + currentParcel.get("Name").toString());
        if (!tabs.containsKey("CurrentParcel")) {
            liTabs.add("CurrentParcel");
        }
        tabs.put("CurrentParcel", currentParcel.get("Name").toString() + "\n\n" + currentParcel.get("Description").toString());
    }

    public void addName2Spinner(String Name) {
        adapter_say.add(Name);
        adapter_say.notifyDataSetChanged();
        spinnerSay.setAdapter(adapter_say);
    }

    public void onGridSay(View view) {
        XMLRPCMethod method = new XMLRPCMethod("Slandy.sendLocal", new XMLRPCMethodCallback() {

            public void callFinished(Object result) {
                System.out.println(result.toString());
            }
        });
        String sSay = eSendText.getText().toString();
        Object[] params = { singleUserUUID, sSay, "0", "Normal" };
        method.call(params);
    }

    public void onBackToMain(View view) {
        setContentView(R.layout.main);
    }

    public void onExit(View view) {
        aliveHandler.removeCallbacks(new_alive);
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

    public void onLoginToGrid(View view) {
        counter = 0;
        Firstname = firstnameField.getText().toString();
        Lastname = lastnameField.getText().toString();
        Password = passwordField.getText().toString();
        CuonUsername = cuonnameField.getText().toString();
        CuonPassword = cuonPasswordField.getText().toString();
        Grid = (String) spinner.getSelectedItem();
        long spinnerPosition = spinner.getSelectedItemId();
        System.out.println("Spinner Position is " + spinnerPosition);
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("Lastname", Lastname);
        editor.putString("Firstname", Firstname);
        editor.putString("Password", Password);
        editor.putString("CuonUsername", CuonUsername);
        editor.putString("CuonPassword", CuonPassword);
        editor.putInt("Grid", (int) spinnerPosition);
        editor.commit();
        try {
            String[] liUri1 = CuonUsername.split("@");
            String[] liUri2 = liUri1[1].split(",");
            cuonUser.put("Name", liUri1[0].toString());
            System.out.println("Connect to " + liUri2[0].toString());
            uri = URI.create(liUri2[0].toString());
            cuonUser.put("client", liUri2[1].toString());
        } catch (Exception cE) {
            System.out.println("Error0");
            System.out.println(cE + " " + cE.getClass().getName() + " " + cE.getMessage());
            uri = URI.create("localhost:9012");
        }
        client = new XMLRPCClient(uri);
        XMLRPCMethod cmethod = new XMLRPCMethod("Database.createSessionID", new XMLRPCMethodCallback() {

            public void callFinished(Object result) {
                System.out.println("cuon sid = " + result.toString());
                cuonUser.put("SessionID", singleUserUUID);
                cuonMode = true;
                oCuon.init(client, cuonUser);
                System.out.println("init cuon");
                XMLRPCMethod method = new XMLRPCMethod("Slandy.Login", new XMLRPCMethodCallback() {

                    public void callFinished(Object result) {
                        singleUserUUID = (result.toString());
                        if (singleUserUUID.compareTo("Test") != 0) {
                            aliveHandler.removeCallbacks(new_alive);
                            aliveHandler.postDelayed(new_alive, 1000);
                        }
                        setContentView(R.layout.local_chat);
                        tvLocal = (EditText) findViewById(R.id.tvLocal);
                        eSendText = (EditText) findViewById(R.id.eSendText);
                        spinnerActions = (Spinner) findViewById(R.id.spinnerActions);
                        adapter_actions.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerActions.setAdapter(adapter_actions);
                        spinnerActions.setSelection(0);
                        spinnerActions.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

                            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
                                switch(pos) {
                                    case 0:
                                        tvLocal.setText(tabs.get("LocalChat").toString());
                                        activeTab = "LocalChat";
                                        activeModul = mLocalChat;
                                        break;
                                    case 1:
                                        System.out.println("Pos 1");
                                        activeTab = "IM";
                                        activeModul = mIM;
                                        break;
                                    case 2:
                                        System.out.println("Pos 2");
                                        activeTab = "GroupIM";
                                        activeModul = mGroupIM;
                                        break;
                                    case 3:
                                        activeTab = "CUON";
                                        try {
                                            if (cuonMode) {
                                                activeModul = mCuon;
                                                cuonMain();
                                            }
                                        } catch (Exception cE) {
                                            System.out.println("Error2");
                                            System.out.println(cE + " " + cE.getClass().getName() + " " + cE.getMessage());
                                        }
                                        break;
                                    case 4:
                                        try {
                                            System.out.println("cp = " + tabs.get("CurrentParcel").toString());
                                            tvLocal.setText(tabs.get("CurrentParcel").toString());
                                            activeTab = "CurrentParcel";
                                        } catch (Exception cE) {
                                            System.out.println("Error2");
                                            System.out.println(cE + " " + cE.getClass().getName() + " " + cE.getMessage());
                                        }
                                        break;
                                }
                            }

                            public void onNothingSelected(AdapterView<?> adapterView) {
                                return;
                            }
                        });
                        spinnerSay = (Spinner) findViewById(R.id.spinnerSay);
                        adapter_say.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerSay.setAdapter(adapter_say);
                        spinnerSay.setSelection(0);
                        spinnerSay.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

                            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
                                System.out.println("Spinner say selected pos " + pos);
                                String ItemUUID;
                                if (pos > 0) {
                                    ItemUUID = avatare.get(adapterView.getItemAtPosition(pos).toString()).toString();
                                } else {
                                    ItemUUID = "LocalChat";
                                }
                                setNewText(ItemUUID);
                            }

                            public void onNothingSelected(AdapterView<?> adapterView) {
                                return;
                            }
                        });
                    }
                });
                Object[] params = { Firstname, Lastname, Password, 1 };
                method.call(params);
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

    private void setNewText(String ItemUUID) {
        String newTab;
        newTab = tabs.get(ItemUUID).toString();
        tvLocal.setText(newTab);
        activeTab = ItemUUID;
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
