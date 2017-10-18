package org.oosterveld.runaround;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.zip.CRC32;
import org.w3c.dom.Document;
import com.google.code.facebookapi.IFacebookRestClient;
import com.google.code.facebookapi.ProfileField;

public class FbConnect {

    public static String render_fbconnect_init_js() {
        String html = String.format("<script src=\"%s/js/api_lib/v0.4/FeatureLoader.js.php\" type=\"text/javascript\"></script>" + "<script type=\"text/javascript\">" + "FB.init(\"%s\", \"xd_receiver.jsp\");" + "</script>" + "<script src=\"fbconnect.js\" type=\"text/javascript\"></script>", Core.get_static_root(), Core.get_api_key());
        String already_logged_in = "false";
        Display.onloadRegister(String.format("facebook_onload(%s);", already_logged_in));
        return html;
    }

    public static String render_fbconnect_button() {
        return render_fbconnect_button("medium");
    }

    public static String render_fbconnect_button(String size) {
        return "<fb:login-button " + "size=\"" + size + "\" background=\"light\" length=\"long\" " + "onlogin=\"facebook_onlogin_ready();\"></fb:login-button>\n";
    }

    public static String email_get_public_hash(String email) {
        try {
            if (email != null) {
                email = email.trim().toLowerCase();
                CRC32 crc32 = new CRC32();
                crc32.reset();
                crc32.update(email.getBytes());
                MessageDigest md5 = MessageDigest.getInstance("MD5");
                md5.reset();
                return crc32.getValue() + " " + new String(md5.digest(email.getBytes()));
            }
        } catch (Exception e) {
        }
        return "";
    }

    public static Object facebook_get_fields(IFacebookRestClient<Object> userClient, long fb_uid, Collection<ProfileField> fields) {
        try {
            ArrayList<Long> ids = new ArrayList<Long>();
            ids.add(fb_uid);
            Object doc = userClient.users_getInfo(ids, fields);
            return doc;
        } catch (Exception e) {
            return null;
        }
    }
}
