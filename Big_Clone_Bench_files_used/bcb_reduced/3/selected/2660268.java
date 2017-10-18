package websrvl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.security.NoSuchAlgorithmException;
import java.net.Socket;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import sun.misc.BASE64Decoder;

/**
 *
 * @author SANSIG, BURGLIN, BOUARISSA
 */
public class ThreadClient extends Thread {

    Socket sdClient;

    boolean isAuth;

    Server pSrvl;

    Trame t_user;

    OutputStream out;

    BufferedReader in;

    UserInfo u_info;

    String droit;

    public ThreadClient(Socket sd, Server srvl) throws IOException {
        super();
        this.sdClient = sd;
        this.isAuth = false;
        this.pSrvl = srvl;
        this.out = sdClient.getOutputStream();
        this.in = new BufferedReader(new InputStreamReader(sdClient.getInputStream()));
        this.t_user = new Trame(this.pSrvl);
        this.u_info = new UserInfo();
    }

    @Override
    public void run() {
        pSrvl.writeOnServer("Nouveau client avec ip=" + this.sdClient.getInetAddress());
        try {
            t_user.analyze(this.sdClient);
            traiter_requete();
            this.in.close();
            this.out.close();
            this.sdClient.close();
            this.pSrvl.writeOnServer("Fin de transaction du client ip=" + this.sdClient.getInetAddress() + "\r\n");
        } catch (Exception e) {
            this.pSrvl.writeOnServer("Exception générée par le serveur pour le client ip=" + this.sdClient.getInetAddress());
        }
    }

    void traiter_requete() throws IOException, NoSuchAlgorithmException {
        if (isConnected()) {
            if (this.t_user.req[0].equals("POST")) {
                if (this.t_user.req[1].equals("file")) sendFile(); else if (this.t_user.req[1].equals("addDos")) {
                    boolean ajout = ajoutDossier();
                    System.out.println("Ajout dossier : " + ajout);
                    if (ajout) sendPrincipalePage();
                } else if (this.t_user.req[1].contains("uploadFile")) uploadFile(); else if (this.t_user.req[1].equals("uploadFolder")) uploadFolder(); else unknowQuery();
            } else {
                if (this.t_user.req[1].equals("index") || this.t_user.req[1].equals("")) sendPrincipalePage(); else if (this.t_user.req[1].contains("download")) sendDownloadFolder(); else if (this.t_user.req[1].equals("upload")) upload(); else if (this.t_user.req[1].equals("right")) setRight(); else if (this.t_user.req[1].contains("modifListe")) {
                    boolean modif = ModiflisteUser();
                    System.out.println("Ajout dossier : " + modif);
                    if (modif) sendPrincipalePage();
                } else unknowPage();
            }
        } else {
            if (this.t_user.req[0].equals("POST")) {
                if (this.t_user.req[1].equals("register")) {
                    register();
                }
            }
            if (this.t_user.header.containsKey("Authorization")) {
                setNewUserOnline();
            } else {
                String html = "<html><head><title>Inscription</title>" + "</head><body><form method='POST' action='register'>" + "<h3>Inscription</h3><br>Nom : <input name='nom' type='text'>" + "<br>Mot de passe : <input name='password' type='password'>" + "<br>Droit : <input name='droit' type='text'>" + "<br><input type='submit' value='Envoyer'></form></body></html>";
                String response = "HTTP/1.1 401 Unauthorized \r\n";
                response += "WWW-Authenticate: Basic realm=\"private Access\" \r\n";
                response += "Server: " + this.pSrvl.s_name + " \r\n";
                response += "Date: " + new Date().toString() + " \r\n";
                response += "Content-Type: text/html; charset=ISO-8859-4\r\n";
                response += "Connection: close \r\n";
                response += "Content-Length: " + html.length() + "\r\n\r\n";
                response += html + "\r\n";
                out.write(response.getBytes());
                out.flush();
            }
        }
    }

    private String md5(String str) throws NoSuchAlgorithmException {
        byte[] hash = MessageDigest.getInstance("MD5").digest(str.getBytes());
        StringBuilder hashString = new StringBuilder();
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(hash[i]);
            if (hex.length() == 1) {
                hashString.append('0');
                hashString.append(hex.charAt(hex.length() - 1));
            } else hashString.append(hex.substring(hex.length() - 2));
        }
        return hashString.toString();
    }

    private boolean loadUserInfo() throws IOException, NoSuchAlgorithmException {
        String headerValue = this.t_user.header.get("Authorization").toString();
        String split[] = new String[2];
        split = headerValue.split(" ");
        String passWordSend = split[1];
        BASE64Decoder decodeur = new BASE64Decoder();
        split = new String(decodeur.decodeBuffer(passWordSend)).split(":");
        String passDecode = split[1];
        String passSendDecodeMd5 = md5(passDecode);
        String pseudo = split[0];
        int OKconnect = connect(pseudo, passSendDecodeMd5);
        if (OKconnect == 0) {
            System.out.println(" connect OK ");
            return true;
        } else {
            switch(OKconnect) {
                case 1:
                    System.out.println("Erreur fichier user.txt");
                    break;
                case 2:
                    System.out.println("Erreur : mauvais Mdp");
                    break;
                case 3:
                    System.out.println("Erreur : Pseudo non trouvé");
                    break;
            }
            return false;
        }
    }

    private void setNewUserOnline() throws IOException, NoSuchAlgorithmException {
        if (loadUserInfo()) {
            this.pSrvl.online_user.put(this.u_info.getSession(), this.u_info);
            sendPrincipalePage();
        } else {
            String html = "<html><head><title></title></head><body><form method='POST' action='register'><h3>Inscription</h3><br>Nom : <input name='nom' type='text'><br>Mot de passe : <input name='password' type='password'>" + "<br>Droit : <input name='droit' type='text'>" + "<input type='submit' value='Envoyer'></form></body></html>";
            String response = "HTTP/1.1 401 Unauthorized \r\n";
            response += "WWW-Authenticate: Basic realm=\"private Access\" \r\n";
            response += "Server: " + this.pSrvl.s_name + " \r\n";
            response += "Date: " + new Date().toString() + " \r\n";
            response += "Content-Type: text/html; charset=ISO-8859-4\r\n";
            response += "Connection: close \r\n";
            response += "Content-Length: " + html.length() + "\r\n\r\n";
            response += html + "\r\n";
            out.write(response.getBytes());
            out.flush();
        }
    }

    private boolean register() throws NoSuchAlgorithmException, IOException {
        String split_data[] = new String[3];
        split_data = this.t_user.corps.split("&");
        String split_pseudo[] = new String[2];
        split_pseudo = split_data[0].split("=");
        String split_pass[] = new String[2];
        split_pass = split_data[1].split("=");
        String split_droit[] = new String[2];
        split_droit = split_data[2].split("=");
        char droits = split_droit[1].charAt(0);
        String passMd5 = md5(split_pass[1]);
        return configNewUser(Character.toString(droits), split_pseudo[1], passMd5);
    }

    private boolean isConnected() {
        if (this.t_user.header.containsKey("Cookie")) {
            String cookieValue = this.t_user.header.get("Cookie").toString();
            String split[] = new String[2];
            split = cookieValue.split("=");
            String session = split[1];
            if (this.pSrvl.online_user.containsKey(session)) {
                UserInfo u = (UserInfo) this.pSrvl.online_user.get(session);
                this.u_info = u;
                return true;
            } else return false;
        } else return false;
    }

    private void sendPrincipalePage() throws IOException {
        String html = "<html><head><title>Serveur http</title></head><body><h3 style='display:inline;'>Vous etes en ligne!</h3> <br><br>>>>Bienvenue M." + this.u_info.getName() + "<br><br>";
        if (this.u_info.getRight().equals("1")) {
            html += "<form method='GET' action='download' style='display:inline'><input type='submit' value='Download'></form><input type='submit' value='Upload'>";
        } else if (this.u_info.getRight().equals("2")) {
            html += "<form method='GET' action='download' style='display:inline'><input type='submit' value='Download'></form><form method='GET' action='upload' style='display:inline'><input type='submit' value='Upload' ></form>";
        } else {
            html += "<input type='submit' value='Download'><input type='submit' value='Upload'>";
        }
        html += "</form></body></html>";
        String response = "HTTP/1.1 200 OK \r\n";
        response += "Server: " + this.pSrvl.s_name + "\r\n";
        response += "Date: " + new Date().toString() + "\r\n";
        response += "Content-Type: text/html; charset=ISO-8859-4\r\n";
        response += "Set-Cookie: " + this.pSrvl.s_name + "=" + this.u_info.getSession() + " \r\n";
        response += "Connection: close \r\n";
        response += "Content-Length: " + html.length() + "\r\n\r\n";
        response += html + "\r\n";
        this.out.write(response.getBytes());
        this.out.flush();
    }

    private void unknowPage() throws IOException {
        String html = "<html><head><title></title></head><body><h3>Page introuvable!</h3><br><a href='index'>Page principale</a></body></html>";
        String response = "HTTP/1.1 200 OK \r\n";
        response += "Server: " + this.pSrvl.s_name + "\r\n";
        response += "Date: " + new Date().toString() + "\r\n";
        response += "Content-Type: text/html; charset=ISO-8859-4\r\n";
        response += "Connection: close \r\n";
        response += "Content-Length: " + html.length() + "\r\n\r\n";
        response += html + "\r\n";
        this.out.write(response.getBytes());
        this.out.flush();
    }

    private void unknowQuery() throws IOException {
        String html = "<html><head><title></title></head><body><h3>Requète incorrecte!</h3><br><a href='index'>Page principale</a></body></html>";
        String response = "HTTP/1.1 200 OK \r\n";
        response += "Server: " + this.pSrvl.s_name + "\r\n";
        response += "Date: " + new Date().toString() + "\r\n";
        response += "Content-Type: text/html; charset=ISO-8859-4\r\n";
        response += "Connection: close \r\n";
        response += "Content-Length: " + html.length() + "\r\n\r\n";
        response += html + "\r\n";
        this.out.write(response.getBytes());
        this.out.flush();
    }

    private boolean configNewUser(String droits, String pseudo, String password) {
        char download, upload;
        int dr = Integer.parseInt(droits);
        switch(dr) {
            case 1:
                download = '1';
                upload = '0';
                break;
            case 2:
                download = '1';
                upload = '1';
                break;
            default:
                download = '0';
                upload = '0';
                break;
        }
        String fichier = this.pSrvl.s_headPath + "user.txt";
        String ligne;
        try {
            InputStream ips = new FileInputStream(fichier);
            InputStreamReader ipsr = new InputStreamReader(ips);
            BufferedReader br = new BufferedReader(ipsr);
            while ((ligne = br.readLine()) != null) {
                String element[] = new String[4];
                element = ligne.split(":");
                if (element[0].equals(pseudo)) {
                    br.close();
                    return false;
                }
            }
            if ((pseudo.equals("user")) || (pseudo.equals("dossier")) || (pseudo.equals("utilisateur"))) return false;
            File f = new File(this.pSrvl.s_headPath + "/dossiers/" + pseudo);
            f.mkdir();
            FileWriter fw = new FileWriter(fichier, true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter fichierSortie = new PrintWriter(bw);
            fichierSortie.println(pseudo + ":" + password + ":" + download + ":" + upload);
            fichierSortie.flush();
            fichierSortie.close();
            FileWriter fu = new FileWriter(this.pSrvl.s_headPath + "/utilisateurs/" + pseudo + ".txt");
            BufferedWriter bu = new BufferedWriter(fu);
            PrintWriter fichierUser = new PrintWriter(bu);
            fichierUser.println("common:" + pseudo);
            fichierUser.close();
            System.out.println("Le fichier " + pseudo + ".txt a été crée!");
            return true;
        } catch (Exception e) {
            System.out.println(e.toString());
            return false;
        }
    }

    public int connect(String pseudo, String password) {
        int download, upload;
        String fichier = this.pSrvl.s_headPath + "user.txt";
        String ligne;
        try {
            InputStream ips = new FileInputStream(fichier);
            InputStreamReader ipsr = new InputStreamReader(ips);
            BufferedReader br = new BufferedReader(ipsr, 256);
            while ((ligne = br.readLine()) != null) {
                String element[] = new String[4];
                element = ligne.split(":");
                if (element[0].compareTo(pseudo) == 0) {
                    if (element[1].compareTo(password) == 0) {
                        download = Integer.parseInt(element[2]);
                        upload = Integer.parseInt(element[3]);
                        droit = String.valueOf(upload + download);
                        br.close();
                        this.u_info.setValue(pseudo, password, droit, new Date());
                        return 0;
                    } else {
                        droit = "0";
                        br.close();
                        return 2;
                    }
                }
            }
            droit = "0";
            br.close();
            return 3;
        } catch (Exception e) {
            System.out.println("Erreur connect : " + e.toString());
            return 1;
        }
    }

    private void sendDownloadFolder() {
        System.out.println("DownLoadFolder");
        try {
            String privatePath = this.pSrvl.s_headPath + "utilisateurs/" + this.u_info.getName() + ".txt";
            String user = "", folder = "";
            String[] cheminP = {};
            if (this.t_user.req[1].contains("?") && this.t_user.req[1].contains("&")) {
                String split_data[] = new String[2];
                split_data = this.t_user.req[1].split("&");
                String folder_data = split_data[0], user_data = split_data[1];
                String split_folder[] = new String[2];
                split_folder = folder_data.split("=");
                folder = split_folder[1];
                String split_nom[] = new String[2];
                split_nom = user_data.split("=");
                user = split_nom[1];
                if (folder.contains("%21")) cheminP = folder.split("%21"); else if (folder.contains("!")) cheminP = folder.split("!"); else {
                    cheminP = new String[1];
                    cheminP[0] = folder;
                }
                privatePath = this.pSrvl.s_headPath + "utilisateurs/" + user + ".txt";
            } else if (this.t_user.req[1].contains("=")) {
                String split_data[] = new String[2];
                split_data = this.t_user.req[1].split("=");
                user = split_data[1];
                privatePath = this.pSrvl.s_headPath + "utilisateurs/" + user + ".txt";
            }
            String html = "<html><head><title>" + this.pSrvl.s_name + "</title></head><body><h3>DownLoad - Dossiers disponibles</h3>";
            html += "<a href='index'>Index</a>";
            if (!folder.equals("")) {
                String navig_folder = folder;
                navig_folder = navig_folder.replace("%21", "!");
                if (navig_folder.contains("!")) {
                    String[] navig_split = navig_folder.split("!");
                    html += "/";
                    for (int i = 0; i < navig_split.length; ++i) {
                        String navig_path = navig_split[0] + "!";
                        if (navig_split.length < 0) navig_path = navig_path.substring(0, navig_path.length() - 1);
                        for (int j = 1; j <= i; ++j) {
                            navig_path += navig_split[j] + "!";
                        }
                        if (navig_path.contains("!")) navig_path = navig_path.substring(0, navig_path.length() - 1);
                        html += "<a href='download?folder=" + navig_path + "&user=" + user + "'>" + navig_split[i] + "</a>/";
                    }
                } else html += "/<a href='download?folder=" + navig_folder + "&user=" + user + "'>" + navig_folder + "</a>/";
            }
            html += "<table style='border-collapse:collapse; border:1px solid black;' width='40%'><tr><td>Dossiers</td><td>Createur</td><td>Download</td>";
            InputStream ips = new FileInputStream(privatePath);
            InputStreamReader ipsr = new InputStreamReader(ips);
            BufferedReader br = new BufferedReader(ipsr);
            String ligne;
            while ((ligne = br.readLine()) != null) {
                if (ligne.contains(":")) {
                    String split[] = new String[2];
                    split = ligne.split(":");
                    String chemin[];
                    if (split[0].contains("!")) {
                        chemin = split[0].split("!");
                    } else {
                        chemin = new String[1];
                        chemin[0] = split[0];
                    }
                    if (chemin.length == (cheminP.length + 1)) {
                        int j = 0;
                        while ((j < cheminP.length) && (chemin[j].compareTo(cheminP[j]) == 0)) j++;
                        if (j == cheminP.length) {
                            user = split[1];
                            String folderName = chemin[j];
                            if (folder.compareTo("") != 0) {
                                String add = "";
                                for (int k = 0; k < cheminP.length; k++) add += cheminP[k] + "!";
                                folderName = add + folderName;
                            }
                            if (folderName.compareTo("common") == 0) html += "<tr style='border:1px solid black'><td width='20%'><form method='GET' action='download'><input type='hidden' value='common' name='user'><input type='submit' value='Common'></form></td><td width='20%'>Common</td><td/></tr>"; else html += "<tr style='border:1px solid black'><td width='20%'><form method='GET' action='download'><input type='hidden' value='" + folderName + "' name='folder'><input type='hidden' value='" + user + "' name='user'><input type='submit' value='" + chemin[j] + "'></form></td><td width='20%'>" + user + "</td><td/></tr>";
                        }
                    }
                }
            }
            html += "<tr/>";
            folder = folder.replace("%21", "/");
            folder = folder.replace("!", "/");
            File f = new File(this.pSrvl.s_headPath + "dossiers/" + user + "/" + folder);
            File list[] = f.listFiles();
            if (user.compareTo("common") == 0) if (folder.compareTo("") == 0) folder = "common"; else folder = "common!" + folder;
            for (int i = 0; i < list.length; ++i) {
                String fileName = list[i].getName();
                if ((!fileName.equals(user + ".txt")) && fileName.contains(".")) {
                    if (fileName.lastIndexOf("\\") + 1 != fileName.lastIndexOf(".")) {
                        html += "<tr style='border:1px solid black'><td width='20%'>" + list[i].getName() + "</td><td width='20%'>" + user + "</td><td>";
                        html += "<form method='POST' action='file'><input type='hidden' value='" + folder + "' name='folder'><input type='hidden' value='" + user + "' name='user'><input type='hidden' value='" + list[i].getName() + "' name='file'><input type='submit' value='GET'></form></td></tr>";
                    }
                }
            }
            br.close();
            html += "</table>";
            folder = folder.replace("/", "!");
            if (user.equals(this.u_info.getName())) {
                html += "<br><form method='POST' action='addDos'><input type='hidden' value='" + folder + "' name='folder'><input type='hidden' value='" + user + "' name='user'>Nouveau dossier : <input type='text' name='file'><input type='submit' value='Ajouter'></form>";
            }
            html += setRight();
            html += "</body></html>";
            String response = "HTTP/1.1 200 OK \r\n";
            response += "Server: " + this.pSrvl.s_name + "\r\n";
            response += "Date: " + new Date().toString() + "\r\n";
            response += "Content-Type: text/html; charset=ISO-8859-4\r\n";
            response += "Connection: close \r\n";
            response += "Content-Length: " + html.length() + "\r\n\r\n";
            response += html + "\r\n";
            this.out.write(response.getBytes());
            this.out.flush();
        } catch (Exception ex) {
            System.out.println("Erreur lecture des dossiers");
        }
    }

    private void sendFile() throws FileNotFoundException, IOException {
        System.out.println("SendFile");
        String split_data[] = new String[3];
        System.out.println(this.t_user.corps);
        split_data = this.t_user.corps.split("&");
        String folder_data = split_data[0], user_data = split_data[1], file_data = split_data[2];
        String split_folder[] = new String[2];
        split_folder = folder_data.split("=");
        String folder = split_folder[1];
        if (folder.contains("%21")) folder = folder.replace("%21", "!");
        if (folder.compareTo("common") == 0) folder = "";
        String split_user[] = new String[2];
        split_user = user_data.split("=");
        String user = split_user[1];
        String split_file[] = new String[2];
        split_file = file_data.split("=");
        String file = split_file[1];
        folder = folder.replace("%2F", "/");
        folder = folder.replace("!", "/");
        if (folder.contains("common/")) user = "";
        File f = new File(this.pSrvl.s_headPath + "dossiers/" + user + "/" + folder + "/" + file);
        int i = file.lastIndexOf(".");
        String extension = file.substring(i + 1, file.length());
        if (this.pSrvl.s_auth_file_extension.containsKey(extension)) {
            String mime = (String) this.pSrvl.s_auth_file_extension.get(extension);
            String response = "HTTP/1.1 200 OK \r\n";
            response += "Server: " + this.pSrvl.s_name + " \r\n";
            response += "Date: " + new Date().toString() + " \r\n";
            response += "Connection: close \r\n";
            response += "Content-Length: " + f.length() + " \r\n";
            response += "Content-Type: " + mime + " \r\n";
            response += "Content-Disposition: attachment; filename=\"" + file + "\" \r\n\r\n";
            this.out.write(response.getBytes());
            byte[] data = new byte[(int) f.length()];
            FileInputStream fis = new FileInputStream(f);
            BufferedInputStream bis = new BufferedInputStream(fis);
            bis.read(data, 0, data.length);
            this.out.write(data, 0, data.length);
            this.out.flush();
        } else {
            String html = "<html><head><title>" + this.pSrvl.s_name + "</title></head><body><h3>Extension de fichier non prise en compte!</h3></body></table>";
            String response = "HTTP/1.1 200 OK \r\n";
            response += "Server: " + this.pSrvl.s_name + " \r\n";
            response += "Date: " + new Date().toString() + " \r\n";
            response += "Connection: close \r\n";
            response += "Content-Length: " + html.length() + " \r\n";
            response += "Content-Type: text/html \r\n\r\n";
            response += html;
            this.out.write(response.getBytes());
            this.out.flush();
        }
    }

    private void upload() throws IOException {
        InputStream ips = new FileInputStream(this.pSrvl.s_headPath + "utilisateurs/" + this.u_info.getName() + ".txt");
        InputStreamReader ipsr = new InputStreamReader(ips);
        BufferedReader br = new BufferedReader(ipsr);
        String ligne;
        String html = "<h3> Dossiers accessibles en upload</h3>";
        html += "<a href='index'>Index</a><br><br>";
        html += "<form method='POST' action='uploadFolder'>";
        html += "<label for='info'>Dossiers accessibles: </label>";
        html += "<select name='folder' id='folder' size='1'>";
        while ((ligne = br.readLine()) != null) {
            String split[] = ligne.split(":");
            String user = split[1], path = split[0];
            if (user.equals(this.u_info.getName())) {
                if (path.equals("common")) html += "<option value='" + split[0] + "&user=common'>" + path.replace("!", "/"); else html += "<option value='" + split[0] + "&user=" + split[1] + "'>" + path.replace("!", "/");
            }
        }
        ips = new FileInputStream(this.pSrvl.s_headPath + "utilisateurs/common.txt");
        ipsr = new InputStreamReader(ips);
        br = new BufferedReader(ipsr);
        while ((ligne = br.readLine()) != null) {
            String split_com[] = ligne.split(":");
            html += "<option value='common!" + split_com[0] + "&user=" + split_com[1] + "'>common/" + split_com[0].replace("!", "/");
        }
        html += "</select><input type='submit' value='Upload'></form>";
        String response = "HTTP/1.1 200 OK \r\n";
        response += "Server: " + this.pSrvl.s_name + " \r\n";
        response += "Date: " + new Date().toString() + " \r\n";
        response += "Connection: close \r\n";
        response += "Content-Length: " + html.length() + " \r\n";
        response += "Content-Type: text/html \r\n\r\n";
        response += html;
        this.out.write(response.getBytes());
        this.out.flush();
    }

    private void uploadFolder() throws IOException {
        String data = this.t_user.corps;
        data = data.replace("%21", "!");
        data = data.replace("%26", "&");
        data = data.replace("%3D", "=");
        System.out.println(data);
        String split[] = data.split("&");
        String user_data = split[1], path_data = split[0];
        String split_user[] = user_data.split("=");
        String user = split_user[1];
        String split_path[] = path_data.split("=");
        String path = split_path[1];
        String html = "<h3> Dossiers accessibles en upload</h3>";
        html += "<a href='index'>Index</a>|<a href='right'>Acces</a><br><br>";
        html += "<form method='POST' action='uploadFile?folder=" + path + "&user=" + user + "' enctype='multipart/form-data'>";
        html += "</select><br><input type='file' name='file'><input type='submit' value='Upload'></form>";
        String response = "HTTP/1.1 200 OK \r\n";
        response += "Server: " + this.pSrvl.s_name + " \r\n";
        response += "Date: " + new Date().toString() + " \r\n";
        response += "Connection: close \r\n";
        response += "Content-Length: " + html.length() + " \r\n";
        response += "Content-Type: text/html \r\n\r\n";
        response += html;
        this.out.write(response.getBytes());
        this.out.flush();
    }

    private void uploadFile() throws IOException {
        String dataReq = this.t_user.req[1];
        dataReq = dataReq.substring(dataReq.indexOf("?") + 1, dataReq.length());
        String split_req[] = dataReq.split("&");
        String user_data = split_req[1], path_data = split_req[0];
        String split_user[] = user_data.split("=");
        String user = split_user[1];
        String split_path[] = path_data.split("=");
        String path = split_path[1];
        String html = null;
        InputStream ips = new FileInputStream(this.pSrvl.s_headPath + "utilisateurs/" + this.u_info.getName() + ".txt");
        InputStreamReader ipsr = new InputStreamReader(ips);
        BufferedReader br = new BufferedReader(ipsr);
        String ligne;
        boolean isAuth = false;
        while ((ligne = br.readLine()) != null) {
            String split_fData[] = ligne.split(":");
            if (split_fData[1].equals(this.u_info.getName()) || user.equals("common")) isAuth = true;
        }
        if (user.equals(this.u_info.getName()) || user.equals("common")) isAuth = true;
        if (!isAuth) html = "<html><head><title>" + this.pSrvl.s_name + "</title></head><body><h3>Vous n'etes pas autorise a uploade!</h3></body></table>"; else html = "<html><head><title>" + this.pSrvl.s_name + "</title></head><body><h3>Upload effecute!</h3><a href='index'>Index</a></body></table>";
        if (isAuth) {
            System.out.println("Start uploading ...");
            byte[] data = this.t_user.data;
            String sData = new String(data, 0, this.t_user.sizeOfData);
            String content = new String(sData);
            int charLu = 2;
            String content_type = (String) this.t_user.header.get("Content-Type");
            String boundary = content_type.substring(content_type.indexOf("boundary=") + 9, content_type.length());
            sData = sData.substring(sData.indexOf("--" + boundary), sData.lastIndexOf("--" + boundary + "--") + 4 + boundary.length());
            int deb = content.length() - sData.length();
            sData = sData.substring(boundary.length() + 2, sData.length());
            charLu += boundary.length() + 2;
            BufferedReader reader = new BufferedReader(new StringReader(sData));
            String fileName = null;
            ligne = reader.readLine();
            ligne = reader.readLine();
            charLu += 2;
            while (!ligne.isEmpty()) {
                if (ligne.contains("filename")) {
                    String split[] = ligne.split(";");
                    for (int i = 0; i < split.length; ++i) {
                        if (split[i].contains("filename")) {
                            String split_fileName[] = split[i].split("=");
                            fileName = split_fileName[1];
                            fileName = fileName.replace("\"", "");
                        }
                    }
                }
                charLu += ligne.length();
                ligne = reader.readLine();
                charLu += 2;
            }
            int beginData = charLu + deb - 2;
            int endData = this.t_user.sizeOfData - boundary.length() - 4 - 2 - 2;
            path = path.replace("!", "/");
            FileOutputStream fos = null;
            System.out.println("|" + path + "|" + user);
            System.out.println(this.pSrvl.s_headPath + "dossiers/" + path + "/" + fileName);
            System.out.println(this.pSrvl.s_headPath + "dossiers/" + user + "/" + path + "/" + fileName);
            if (user.equals("common")) {
                fos = new FileOutputStream(this.pSrvl.s_headPath + "dossiers/" + path + "/" + fileName);
            } else fos = new FileOutputStream(this.pSrvl.s_headPath + "dossiers/" + user + "/" + path + "/" + fileName);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            bos.write(data, beginData, endData - beginData);
            bos.flush();
            bos.close();
        }
        String response = "HTTP/1.1 200 OK \r\n";
        response += "Server: " + this.pSrvl.s_name + " \r\n";
        response += "Date: " + new Date().toString() + " \r\n";
        response += "Connection: close \r\n";
        response += "Content-Length: " + html.length() + " \r\n";
        response += "Content-Type: text/html \r\n\r\n";
        response += html;
        this.out.write(response.getBytes());
        this.out.flush();
    }

    public boolean ajoutDossier() {
        System.out.println("AddDossier");
        String split_data[] = new String[3];
        split_data = this.t_user.corps.split("&");
        String folder_data = split_data[0], user_data = split_data[1], file_name = split_data[2];
        String split_folder[] = new String[2];
        split_folder = folder_data.split("=");
        String folder;
        if (split_folder.length > 1) folder = split_folder[1]; else folder = "";
        if (folder.compareTo("common") == 0) folder = "";
        String split_user[] = new String[2];
        split_user = user_data.split("=");
        String user = "common";
        if (split_user.length > 1) user = split_user[1]; else return false;
        String split_file[] = new String[2];
        split_file = file_name.split("=");
        String file;
        if (split_file.length > 1) file = split_file[1]; else return false;
        String users[] = {};
        try {
            folder = folder.replace("!", "/");
            folder = folder.replace("%21", "/");
            if (folder.contains("common/")) user = "";
            File f = new File(this.pSrvl.s_headPath + "dossiers/" + user + "/" + folder + "/" + file);
            boolean status = f.mkdir();
            if (!status) return false;
            if (folder.compareTo("") != 0) {
                folder = folder.replace("/", "!");
                folder = folder.concat("!");
            }
            FileWriter fw = null;
            System.out.println("folder=" + folder + " - file=" + file);
            fw = new FileWriter(this.pSrvl.s_headPath + "FichierDossier/" + user + "!" + folder + file + ".txt");
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter fichierSortie = new PrintWriter(bw);
            if (folder.contains("common!")) user = "common";
            fichierSortie.println("createur:" + user);
            FileWriter fp = new FileWriter(this.pSrvl.s_headPath + "utilisateurs/" + user + ".txt", true);
            BufferedWriter bp = new BufferedWriter(fp);
            PrintWriter fichierUserp = new PrintWriter(bp);
            if (folder.contains("common!")) folder = folder.substring(folder.indexOf("common!") + 7);
            System.out.println("Add dossier user=" + user);
            fichierUserp.println(folder + file + ":" + user);
            fichierUserp.close();
            for (int i = 0; i < users.length; i++) {
                if (users[i].compareTo(user) != 0) {
                    fichierSortie.println(users[i]);
                    FileWriter fu = new FileWriter(this.pSrvl.s_headPath + "utilisateurs/" + users[i] + ".txt", true);
                    BufferedWriter bu = new BufferedWriter(fu);
                    PrintWriter fichierUser = new PrintWriter(bu);
                    fichierUser.println(folder + file + ":" + user);
                    fichierUser.close();
                }
            }
            System.out.println("Le dossier " + file + " a été créé!");
            fichierSortie.close();
            FileWriter fu = new FileWriter(this.pSrvl.s_headPath + "dossier.txt", true);
            BufferedWriter bu = new BufferedWriter(fu);
            PrintWriter dossier = new PrintWriter(bu);
            dossier.println(user + "!" + folder + file + ":" + user);
            dossier.close();
            return true;
        } catch (Exception e) {
            System.out.println("Erreur listeTousDos : " + e.toString());
            return false;
        }
    }

    private String setRight() {
        try {
            String folder = "", user = "common";
            String code = "";
            String[] cheminP = {};
            if (this.t_user.req[1].contains("?")) {
                String split_data[] = new String[2];
                split_data = this.t_user.req[1].split("&");
                String[] split_folder = split_data[0].split("=");
                String[] split_user = split_data[1].split("=");
                folder = split_folder[1];
                user = split_user[1];
                if (folder.contains("%21")) cheminP = folder.split("%21"); else if (folder.contains("!")) cheminP = folder.split("!"); else {
                    cheminP = new String[1];
                    cheminP[0] = folder;
                }
                if (folder.contains("%21")) folder = folder.replace("%21", "!");
            }
            if (user.compareTo("common") != 0) {
                code += "<table border='1px solid black'><form methode='POST' action='modifListe'><input type='hidden' name='folder' value='" + folder + "'/><input type='hidden' name='user' value='" + user + "'/>";
                String[] listeUser = listeUser();
                String[] listeUpl = listeUserUpload(user, folder);
                String liste = Arrays.asList(listeUpl).toString();
                if (user.equals(this.u_info.getName()) && listeUser.length > 1) {
                    code += "<tr><td colspan='2'>Authorisation download: </td></tr>";
                    for (int i = 0; i < listeUser.length; i++) {
                        if (listeUser[i].compareTo(user) != 0) {
                            if (liste.contains(listeUser[i])) code += "<tr><td width='20%'><input type='checkbox' name='choix[]' value='" + listeUser[i] + "' checked='checked' /></td><td width='20%'> " + listeUser[i] + " </td></tr>"; else code += "<tr><td width='20%'><input type='checkbox' name='choix[]' value='" + listeUser[i] + "'></td><td width='20%'> " + listeUser[i] + " </td></tr>";
                        }
                    }
                    code += "<tr><td colspan='2' align='center'><input type='submit' value='Modifier'/></td></tr>";
                }
                code += "</form></table>";
            }
            return code;
        } catch (Exception ex) {
            System.out.println("Erreur setRigth : " + ex);
            return "";
        }
    }

    private String[] listeUser() {
        String ligne;
        ArrayList<String> lister = new ArrayList<String>();
        String fichier = this.pSrvl.s_headPath + "user.txt";
        int i = 0;
        try {
            InputStream ips = new FileInputStream(fichier);
            InputStreamReader ipsr = new InputStreamReader(ips);
            BufferedReader br = new BufferedReader(ipsr, 256);
            while ((ligne = br.readLine()) != null) {
                String element[] = new String[4];
                element = ligne.split(":");
                lister.add(element[0]);
                i++;
            }
            String[] ret = new String[i];
            ret = lister.toArray(ret);
            return ret;
        } catch (Exception e) {
            System.out.println("Erreur listeUser : " + e.toString());
            return null;
        }
    }

    private String[] listeUserUpload(String user, String folder) {
        ArrayList<String> lister = new ArrayList<String>();
        String ligne;
        String fichier = this.pSrvl.s_headPath + "FichierDossier/" + user + "!" + folder + ".txt";
        int i = 0;
        try {
            InputStream ips = new FileInputStream(fichier);
            InputStreamReader ipsr = new InputStreamReader(ips);
            BufferedReader br = new BufferedReader(ipsr, 256);
            ligne = br.readLine();
            String[] creat = ligne.split(":");
            lister.add(creat[1]);
            i++;
            while ((ligne = br.readLine()) != null) {
                lister.add(ligne);
                i++;
            }
            String[] ret = new String[i];
            ret = lister.toArray(ret);
            return ret;
        } catch (Exception e) {
            System.out.println("Erreur listeUserUpload : " + e.toString());
            return null;
        }
    }

    private boolean ModiflisteUser() {
        String ligne;
        String createur = "";
        String folder = "";
        String[] liste = {};
        if (this.t_user.req[1].contains("?")) {
            String split_data[] = new String[2];
            split_data = this.t_user.req[1].split("&");
            String[] split_folder = split_data[0].split("=");
            String[] split_user = split_data[1].split("=");
            int nbUser = split_data.length - 2;
            folder = split_folder[1];
            if (folder.contains("%21")) folder = folder.replace("%21", "!");
            createur = split_user[1];
            liste = new String[nbUser];
            int pos = 2;
            for (int k = 1; k <= nbUser; k++) {
                String[] tmp = split_data[pos].split("=");
                pos++;
                liste[k - 1] = tmp[1];
            }
        }
        String fichier = this.pSrvl.s_headPath + "FichierDossier/" + createur + "!" + folder + ".txt";
        try {
            InputStream ips = new FileInputStream(fichier);
            InputStreamReader ipsr = new InputStreamReader(ips);
            BufferedReader br = new BufferedReader(ipsr, 256);
            ArrayList<String> listeold = new ArrayList<String>();
            String element[] = null;
            if ((ligne = br.readLine()) != null) {
                element = ligne.split(":");
                createur = element[1];
            } else createur = "common";
            while ((ligne = br.readLine()) != null) {
                listeold.add(ligne);
            }
            br.close();
            supprimerDroit(folder, createur, listeold);
            FileWriter fu = new FileWriter(fichier);
            BufferedWriter bu = new BufferedWriter(fu);
            PrintWriter dossier = new PrintWriter(bu);
            dossier.println("createur:" + createur);
            for (int i = 0; i < liste.length; i++) {
                if (liste[i].compareTo(createur) != 0) {
                    dossier.println(liste[i]);
                    FileWriter fw = new FileWriter(this.pSrvl.s_headPath + "utilisateurs/" + liste[i] + ".txt", true);
                    BufferedWriter bw = new BufferedWriter(fw);
                    PrintWriter fichierUser = new PrintWriter(bw);
                    fichierUser.println(folder + ":" + createur);
                    fichierUser.close();
                }
            }
            dossier.close();
            return true;
        } catch (Exception e) {
            System.out.println("Erreur modifListe : " + e.toString());
            return false;
        }
    }

    private void supprimerDroit(String folder, String createur, ArrayList<String> listeold) {
        try {
            String[] ret = new String[listeold.size()];
            ret = listeold.toArray(ret);
            String ligne;
            for (int i = 0; i < ret.length; i++) {
                if (ret[i].compareTo(createur) != 0) {
                    ArrayList<String> listdos = new ArrayList<String>();
                    InputStream ips = new FileInputStream(this.pSrvl.s_headPath + "utilisateurs/" + ret[i] + ".txt");
                    InputStreamReader ipsr = new InputStreamReader(ips);
                    BufferedReader br = new BufferedReader(ipsr, 256);
                    while ((ligne = br.readLine()) != null) {
                        String[] element = ligne.split(":");
                        if ((element[1].compareTo(createur) != 0) || (element[0].compareTo(folder) != 0)) {
                            listdos.add(ligne);
                        }
                    }
                    br.close();
                    String[] dos = new String[listdos.size()];
                    dos = listdos.toArray(dos);
                    File f = new File(this.pSrvl.s_headPath + "utilisateurs/" + ret[i] + ".txt");
                    f.delete();
                    FileWriter fw = new FileWriter(this.pSrvl.s_headPath + "utilisateurs/" + ret[i] + ".txt");
                    BufferedWriter bw = new BufferedWriter(fw);
                    PrintWriter fichierUser = new PrintWriter(bw);
                    for (int j = 0; j < dos.length; j++) fichierUser.println(dos[j]);
                    fichierUser.close();
                }
            }
        } catch (Exception ex) {
            System.out.println("Erreur supprimer : " + ex);
        }
    }
}
