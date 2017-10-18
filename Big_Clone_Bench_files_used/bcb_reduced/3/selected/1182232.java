package install4jvalidator;

import com.install4j.api.actions.AbstractInstallAction;
import com.install4j.api.context.InstallerContext;
import com.install4j.api.context.UserCanceledException;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.security.MessageDigest;
import java.util.List;
import java.util.Vector;

/**
 * Validator for installer
 * @author twak (c) Short Fuze Ltd, 20 September 2005
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY twak ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <copyright holder> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
public class MscopeValidator extends AbstractInstallAction {

    public static String FILE_LOCATION = "http://www.twak.org/hashes.txt";

    private static boolean DEBUG = true;

    private MscopeScreenValidator screen;

    public MscopeValidator() {
    }

    public MscopeValidator(MscopeScreenValidator screen) {
        this();
        this.screen = screen;
    }

    public boolean install(InstallerContext installerContext) throws UserCanceledException {
        return checkFileAgainstWebsite(installerContext.getInstallerFile(), FILE_LOCATION);
    }

    public boolean checkFileAgainstWebsite(File file, String website) {
        List<String> hashes = getHashesFrom(website);
        if (hashes == null) {
            screen.println("I can't get the checksums, continuing offline...");
            return true;
        }
        String hash = hashFile(file);
        for (String s : hashes) if (s.compareTo(hash) == 0) {
            screen.println("File check passed!");
            return true;
        }
        screen.println("Warning - your file may be corrupted! \nTry downloading  again.");
        return false;
    }

    private String hashFile(File f) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[65536];
            int plip = 0;
            InputStream in = new BufferedInputStream(new FileInputStream(f));
            int length = 1;
            while (true) {
                if (++plip > 16) {
                    plip = 0;
                    screen.print(".");
                }
                length = in.read(buffer);
                if (length <= 0) break;
                md.update(buffer, 0, length);
            }
            screen.println();
            return byteArrayToHexString(md.digest());
        } catch (Exception X) {
            return "invalid hash";
        }
    }

    public String byteArrayToHexString(byte[] in) {
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < in.length; i++) {
            String aByte = Integer.toHexString(0xFF & in[i]);
            if (aByte.length() == 1) aByte = "0" + aByte;
            hexString.append(aByte);
        }
        return hexString.toString();
    }

    private List<String> getHashesFrom(String webPage) {
        Vector<String> out = new Vector();
        try {
            URL url = new URL(webPage);
            BufferedReader r = new BufferedReader(new InputStreamReader(url.openStream()));
            String line;
            while ((line = r.readLine()) != null) {
                out.add(line);
            }
        } catch (Exception X) {
            return null;
        }
        return out;
    }

    private byte[] readFullyAsBytes(File file) throws IOException {
        screen.println("reading file " + file);
        byte[] bytes = new byte[(int) file.length()];
        FileInputStream in = new FileInputStream(file);
        in.read(bytes);
        in.close();
        return bytes;
    }

    public static void main(String[] args) {
        System.out.println("result is " + new MscopeValidator().checkFileAgainstWebsite(new File("othon_windows_1.exe"), FILE_LOCATION));
    }
}
