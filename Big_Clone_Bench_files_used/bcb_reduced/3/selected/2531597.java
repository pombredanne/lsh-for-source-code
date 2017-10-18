package Control;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import Entity.*;
import java.security.*;

public class CMember extends javax.servlet.http.HttpServlet implements javax.servlet.Servlet {

    static final long serialVersionUID = 1L;

    private HttpServletRequest request;

    private HttpServletResponse response;

    private enum Action {

        CREATEFRIENDS, DELETEFRIENDS, REGIST, EDITINFO
    }

    public CMember() {
        super();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.request = request;
        this.response = response;
        this.request.setCharacterEncoding("big5");
        this.Actions();
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.request = request;
        this.response = response;
        this.request.setCharacterEncoding("big5");
        this.Actions();
    }

    private void Actions() throws ServletException, IOException {
        switch(Action.valueOf(this.request.getParameter("a"))) {
            case CREATEFRIENDS:
                this.createFriends();
                break;
            case DELETEFRIENDS:
                this.deleteFriends();
                break;
            case REGIST:
                this.regist();
                break;
            case EDITINFO:
                this.editInfo();
                break;
        }
    }

    private void createFriends() throws IOException {
        EMember f = new EMember();
        f.readMember(this.request.getParameter("friend"));
        if (f.getId() != 0) {
            EMember m = new EMember();
            m.readMember(Integer.parseInt(this.request.getParameter("member")));
            m.createFriends(f.getId());
        }
        this.response.sendRedirect("Member/readFriends.jsp");
    }

    private void deleteFriends() throws IOException {
        HttpSession s = this.request.getSession();
        EMember m = new EMember();
        m.readMember((Integer) s.getAttribute("mid"));
        m.deleteFriends(Integer.parseInt(this.request.getParameter("fid")));
        this.response.sendRedirect("Member/index.jsp");
    }

    private void regist() throws IOException {
        EMember m = new EMember();
        m.setEmail(this.request.getParameter("email"));
        m.setPassword(this.sha1(this.request.getParameter("password")));
        m.setName(this.request.getParameter("name"));
        m.createMember();
        this.response.sendRedirect("index.jsp");
    }

    private void editInfo() throws IOException {
        EMember m = new EMember();
        m.readMember(Integer.parseInt(this.request.getParameter("mid")));
        if (!this.request.getParameter("password").equals("")) m.setPassword(this.request.getParameter("password"));
        m.setName(this.request.getParameter("name"));
        m.updateMembet();
        this.response.sendRedirect("Member/index.jsp");
    }

    private String sha1(String s) {
        String encrypt = s;
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            sha.update(s.getBytes());
            byte[] digest = sha.digest();
            final StringBuffer buffer = new StringBuffer();
            for (int i = 0; i < digest.length; ++i) {
                final byte b = digest[i];
                final int value = (b & 0x7F) + (b < 0 ? 128 : 0);
                buffer.append(value < 16 ? "0" : "");
                buffer.append(Integer.toHexString(value));
            }
            encrypt = buffer.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return encrypt;
    }
}
