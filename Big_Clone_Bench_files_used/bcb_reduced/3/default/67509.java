import com.rbnb.sapi.ChannelMap;
import com.rbnb.sapi.ChannelTree;
import com.rbnb.sapi.ChannelTree.Node;
import com.rbnb.sapi.PlugIn;
import com.rbnb.sapi.PlugInChannelMap;
import com.rbnb.sapi.Sink;
import com.rbnb.utility.ArgHandler;
import com.rbnb.utility.RBNBProcess;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.security.*;
import java.util.Stack;

public class SignaturePlugIn implements Runnable {

    private Stack threadStack;

    private String signature = new String("");

    private String piAddress = new String("localhost:3333");

    private String sAddress = new String("localhost:3333");

    private String sinkName = new String("SigSink");

    private String pluginName = new String("Signature");

    private PlugIn plugin = null;

    private Sink sink = null;

    private int debug = 0;

    private long timeout = 60000;

    public static void main(String[] args) {
        SignaturePlugIn spi = new SignaturePlugIn();
        try {
            ArgHandler ah = new ArgHandler(args);
            if (ah.checkFlag('a')) {
                String addressL = ah.getOption('a');
                if (addressL != null) spi.setPiAddress(addressL);
            }
            if (ah.checkFlag('b')) {
                String addressL = ah.getOption('b');
                if (addressL != null) spi.setSAddress(addressL);
            }
            if (ah.checkFlag('t')) {
                long to = Long.parseLong(ah.getOption('t'));
                if (to >= -1) spi.setTimeout(to);
            }
            if (ah.checkFlag('n')) {
                String name = ah.getOption('n');
                if (name != null) spi.setPiName(name);
            }
            if (ah.checkFlag('s')) {
                String name = ah.getOption('s');
                if (name != null) spi.setSignature(name);
            }
        } catch (Exception e) {
            System.err.println("SignaturePlugIn argument exception " + e.getMessage());
            e.printStackTrace();
            RBNBProcess.exit(0);
        }
        spi.run();
    }

    public SignaturePlugIn() {
        plugin = new PlugIn();
        threadStack = new Stack();
    }

    public void setPiAddress(String address) {
        this.piAddress = address;
    }

    public void setSAddress(String address) {
        this.sAddress = address;
    }

    public void setTimeout(long to) {
        this.timeout = to;
    }

    public void setPiName(String name) {
        this.pluginName = name;
    }

    public void setSignature(String name) {
        this.signature = name;
    }

    public void run() {
        try {
            plugin.OpenRBNBConnection(piAddress, pluginName);
            System.err.println("Signature pluginName: " + pluginName);
            while (true) {
                PlugInChannelMap picm = plugin.Fetch(-1);
                AnswerRequest a;
                if (threadStack.empty()) a = new AnswerRequest(); else a = (AnswerRequest) threadStack.pop();
                a.setRequestMap(picm);
                new Thread(a).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class AnswerRequest implements Runnable {

        private PlugInChannelMap picm;

        private Sink sink;

        private long lastConnect = System.currentTimeMillis();

        AnswerRequest() {
            sink = new Sink();
            try {
                sink.OpenRBNBConnection(sAddress, sinkName);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void setRequestMap(PlugInChannelMap requestMap) {
            this.picm = requestMap;
        }

        public void run() {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-1");
                ChannelMap cm = new ChannelMap();
                for (int i = 0; i < picm.NumberOfChannels(); i++) {
                    cm.Add(picm.GetName(i));
                }
                String[] folder = picm.GetFolderList();
                for (int i = 0; i < folder.length; i++) {
                    cm.AddFolder(folder[i]);
                }
                sink.Request(cm, picm.GetRequestStart(), picm.GetRequestDuration(), picm.GetRequestReference());
                cm = sink.Fetch(timeout);
                if (cm.GetIfFetchTimedOut()) {
                    System.err.println("Signature Data Fetch Timed Out!");
                    picm.Clear();
                } else {
                    md.reset();
                    folder = cm.GetFolderList();
                    for (int i = 0; i < folder.length; i++) picm.AddFolder(folder[i]);
                    int sigIdx = -1;
                    for (int i = 0; i < cm.NumberOfChannels(); i++) {
                        String chan = cm.GetName(i);
                        if (chan.endsWith("/_signature")) {
                            sigIdx = i;
                            continue;
                        }
                        int idx = picm.GetIndex(chan);
                        if (idx == -1) idx = picm.Add(chan);
                        picm.PutTimeRef(cm, i);
                        picm.PutDataRef(idx, cm, i);
                        md.update(cm.GetData(i));
                        md.update((new Double(cm.GetTimeStart(i))).toString().getBytes());
                    }
                    if (cm.NumberOfChannels() > 0) {
                        byte[] amd = md.digest(signature.getBytes());
                        if (sigIdx >= 0) {
                            if (MessageDigest.isEqual(amd, cm.GetDataAsByteArray(sigIdx)[0])) {
                                System.err.println(pluginName + ": signature matched for: " + cm.GetName(0));
                            } else {
                                System.err.println(pluginName + ": failed signature test, sending null response");
                                picm.Clear();
                            }
                        } else {
                            System.err.println(pluginName + ": _signature attached for: " + cm.GetName(0));
                            int idx = picm.Add("_signature");
                            picm.PutTime(0., 0.);
                            picm.PutDataAsByteArray(idx, amd);
                        }
                    }
                }
                plugin.Flush(picm);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (threadStack.size() < 4) threadStack.push(this); else sink.CloseRBNBConnection();
        }
    }
}
