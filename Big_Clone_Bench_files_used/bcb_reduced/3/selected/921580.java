package system.controller;

import java.io.IOException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import system.model.tb_user;
import com.sysnet_pioneer.auth.webUser;
import com.sysnet_pioneer.controller.Controller;
import com.sysnet_pioneer.variables.globalVars;

public class authController extends Controller {

    public String MD5(String md5) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] array = md.digest(md5.getBytes());
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < array.length; ++i) {
                sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1, 3));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
        }
        return null;
    }

    public void login() {
        if (super.getMethod().equals("POST")) {
            String userName = globalVars.request.getParameter("username");
            String passWord = globalVars.request.getParameter("password");
            tb_user users = new tb_user();
            JSONArray userJson = users.select().all().createCondition("user_username='" + userName + "'").getJson();
            try {
                Object obj = JSONValue.parse(userJson.toJSONString());
                JSONArray array = (JSONArray) obj;
                JSONObject obj2 = (JSONObject) array.get(0);
                String dbPassword = obj2.get("user_password").toString();
                if (this.MD5(passWord).equals(dbPassword)) {
                    webUser.sessionStart();
                    globalVars.session.setAttribute("userId", obj2.get("user_id_pk"));
                    globalVars.response.sendRedirect("index.jsp");
                    globalVars.hasTemplate = true;
                } else {
                    super.redirect("auth/login&error=1");
                    globalVars.hasTemplate = true;
                }
            } catch (Exception e) {
                super.redirect("auth/login&error=2");
                globalVars.hasTemplate = true;
            }
        } else {
            super.renderView("login");
        }
    }

    public void logout() {
        globalVars.session.invalidate();
        try {
            globalVars.response.sendRedirect("index.jsp");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
