package com.proyectobloj.presentation.actions.admin;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import org.apache.log4j.Logger;
import com.proyectobloj.common.Constants;
import com.proyectobloj.dto.IComentariosDto;
import com.proyectobloj.dto.IConfiguracionDto;
import com.proyectobloj.dto.IEtiquetasDto;
import com.proyectobloj.dto.ILinksDto;
import com.proyectobloj.dto.IMenuDto;
import com.proyectobloj.dto.IPostDto;
import com.proyectobloj.dto.IUsuarioDto;
import com.proyectobloj.persistence.model.ComentariosModel;
import com.proyectobloj.persistence.model.ConfiguracionModel;
import com.proyectobloj.persistence.model.EtiquetasModel;
import com.proyectobloj.persistence.model.LinksModel;
import com.proyectobloj.persistence.model.MenuModel;
import com.proyectobloj.persistence.model.PostModel;
import com.proyectobloj.persistence.model.UsuarioModel;
import com.proyectobloj.presentation.actions.genericas.BaseAction;
import com.proyectobloj.resources.configuration.Configure;

public class AdminAction extends BaseAction {

    private String user = null;

    private String password = null;

    private Logger log = Logger.getLogger(this.getClass().toString());

    private IConfiguracionDto configuracionModel = new ConfiguracionModel();

    private String paginaAdmin = "";

    private IPostDto postModel = new PostModel();

    private IMenuDto menuModel = new MenuModel();

    private String[] selectEtiquetas;

    public String adminLogin() {
        char[] HEXADECIMAL = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
        try {
            if (this.user == null) {
                getServletRequest().setAttribute("admin", "1");
                return SUCCESS;
            } else {
                MessageDigest md = MessageDigest.getInstance("MD5");
                byte[] bytes = md.digest(this.password.getBytes());
                StringBuilder sb = new StringBuilder(2 * bytes.length);
                for (int i = 0; i < bytes.length; i++) {
                    int low = (int) (bytes[i] & 0x0f);
                    int high = (int) ((bytes[i] & 0xf0) >> 4);
                    sb.append(HEXADECIMAL[high]);
                    sb.append(HEXADECIMAL[low]);
                }
                this.password = sb.toString();
                IUsuarioDto usuarioModel = new UsuarioModel();
                usuarioModel.setUsuario(this.user);
                usuarioModel.setPassword(this.password);
                Integer usuarios = this.getIAdminFacade().checkUsuario(usuarioModel);
                if (usuarios == 1) {
                    Integer rol = this.getIAdminFacade().getRol(usuarioModel);
                    if (rol == 255) {
                        getServletRequest().getSession().setAttribute("admin", "admin");
                        return "index";
                    } else {
                        addActionError("No tienes suficientes permisos");
                        getServletRequest().setAttribute("admin", "1");
                        return SUCCESS;
                    }
                } else {
                    addActionError("Login incorrecto");
                    getServletRequest().setAttribute("admin", "1");
                    return SUCCESS;
                }
            }
        } catch (Exception ex) {
            this.log.error("Error en adminLogin:\n" + this.getStacktrace(ex));
            return "error";
        }
    }

    public String editarLinks() {
        try {
            this.setLLinks(getIPrincipalFacade().getLinks());
            getServletRequest().setAttribute("lLinks", this.getLLinks());
            this.paginaAdmin = "editarlinks";
            return SUCCESS;
        } catch (Exception ex) {
            this.log.error("Error en editarLinks:\n" + this.getStacktrace(ex));
            return "error";
        }
    }

    public String doAddLink() {
        Integer success = 0;
        try {
            if (getServletRequest().getParameter("url") != null && getServletRequest().getParameter("nombre") != null) {
                String url = getServletRequest().getParameter("url").toString();
                String nombre = getServletRequest().getParameter("nombre").toString();
                ILinksDto linkModel = new LinksModel();
                linkModel.setLink(url);
                linkModel.setNombre(nombre);
                success = this.getIPrincipalFacade().addLink(linkModel);
            }
        } catch (Exception ex) {
            this.log.error("Error en doAddLink:\n" + this.getStacktrace(ex));
        }
        List lJson = new ArrayList();
        lJson.add(success);
        this.setJsonModel(lJson);
        return SUCCESS;
    }

    public String borrarLink() {
        Integer success = 0;
        try {
            if (getServletRequest().getParameter("idLinks") != null) {
                String idLinks = getServletRequest().getParameter("idLinks").toString();
                ILinksDto linkModel = new LinksModel();
                linkModel.setIdLinks(idLinks);
                success = this.getIPrincipalFacade().borrarLink(linkModel);
            }
        } catch (Exception ex) {
            this.log.error("Error en borrarLink:\n" + this.getStacktrace(ex));
        }
        List lJson = new ArrayList();
        lJson.add(success);
        this.setJsonModel(lJson);
        return SUCCESS;
    }

    public String editarLink() {
        try {
            String idLinks = "";
            if (getServletRequest().getParameter("idLinks") != null) {
                idLinks = getServletRequest().getParameter("idLinks").toString();
            }
            ILinksDto linkModel = new LinksModel();
            linkModel.setIdLinks(idLinks);
            this.setLLinks(getIPrincipalFacade().getLinks(linkModel));
            if (this.getLLinks().size() != 0) {
                linkModel = (LinksModel) this.getLLinks().get(0);
            } else {
                linkModel = new LinksModel();
            }
            getServletRequest().setAttribute("link", linkModel);
            return SUCCESS;
        } catch (Exception ex) {
            this.log.error("Error en editarLink:\n" + this.getStacktrace(ex));
            return "error";
        }
    }

    public String doEditarLink() {
        Integer success = 0;
        try {
            if (getServletRequest().getParameter("url") != null && getServletRequest().getParameter("nombre") != null && getServletRequest().getParameter("idLinks") != null) {
                String url = getServletRequest().getParameter("url").toString();
                String nombre = getServletRequest().getParameter("nombre").toString();
                String idLinks = getServletRequest().getParameter("idLinks").toString();
                ILinksDto linkModel = new LinksModel();
                linkModel.setLink(url);
                linkModel.setNombre(nombre);
                linkModel.setIdLinks(idLinks);
                success = this.getIPrincipalFacade().editarLink(linkModel);
            }
        } catch (Exception ex) {
            this.log.error("Error en doEditarLink:\n" + this.getStacktrace(ex));
        }
        List lJson = new ArrayList();
        lJson.add(success);
        this.setJsonModel(lJson);
        return SUCCESS;
    }

    public String configurarBlog() {
        try {
            this.setConfiguracionModel(this.getIAdminFacade().getConfiguracion());
            List lThemes = this.getIAdminFacade().getThemes();
            getServletRequest().setAttribute("lThemes", lThemes);
            this.paginaAdmin = "configurarblog";
            return SUCCESS;
        } catch (Exception ex) {
            this.log.error("Error en configurarBlog:\n" + this.getStacktrace(ex));
            return "error";
        }
    }

    public String doConfigurarBlog() {
        Integer success = 0;
        try {
            success = this.getIAdminFacade().setConfiguracion(this.getConfiguracionModel());
            if (success == 1) {
                Configure.setProperty(Constants.NOMBREBLOG, this.getConfiguracionModel().getNombreBlog());
                Configure.setProperty(Constants.DESCRIPCIONBLOG, this.getConfiguracionModel().getDescripcionBlog());
                Configure.setProperty(Constants.NOMBREAUTOR, this.getConfiguracionModel().getAutorBlog());
                Configure.setProperty(Constants.THEME, this.getConfiguracionModel().getTheme());
                Configure.setProperty(Constants.POSTPORPAGINA, this.getConfiguracionModel().getPostPorPagina());
            }
        } catch (Exception ex) {
            this.log.error("Error en doConfigurarBlog:\n" + this.getStacktrace(ex));
        }
        List lJson = new ArrayList();
        lJson.add(success);
        this.setJsonModel(lJson);
        return SUCCESS;
    }

    public String menuAdmin() {
        this.paginaAdmin = "menuadmin";
        return SUCCESS;
    }

    public String editarPost() {
        try {
            if (getServletRequest().getParameter("post") == null) {
                this.setLPost(this.getIPrincipalFacade().getPosts());
                getServletRequest().setAttribute("lPosts", this.getLPost());
                this.paginaAdmin = "listaposts";
                return SUCCESS;
            } else {
                String link = getServletRequest().getParameter("post").toString();
                this.postModel.setLink(link);
                List lPost = this.getIPrincipalFacade().getEntrada(postModel);
                this.postModel = (IPostDto) lPost.get(0);
                this.setLEtiquetas(this.getIPrincipalFacade().getEtiquetas());
                this.selectEtiquetas = new String[this.getLEtiquetas().size()];
                for (int i = 0; i < this.getLEtiquetas().size(); i++) {
                    IEtiquetasDto etiquetasModel = new EtiquetasModel();
                    etiquetasModel = (IEtiquetasDto) this.getLEtiquetas().get(i);
                    this.selectEtiquetas[i] = etiquetasModel.getEtiqueta();
                }
                String etiquetasModel = this.postModel.getEtiquetas();
                String[] selectEtiquetasValue = etiquetasModel.split(";");
                getServletRequest().setAttribute("selectEtiquetasValue", selectEtiquetasValue);
                this.paginaAdmin = "nuevopost";
                return SUCCESS;
            }
        } catch (Exception ex) {
            this.log.error("Error en editarPost:\n" + this.getStacktrace(ex));
            return "error";
        }
    }

    public String addPost() {
        try {
            this.setLEtiquetas(this.getIPrincipalFacade().getEtiquetas());
            this.selectEtiquetas = new String[this.getLEtiquetas().size()];
            for (int i = 0; i < this.getLEtiquetas().size(); i++) {
                IEtiquetasDto etiquetasModel = new EtiquetasModel();
                etiquetasModel = (IEtiquetasDto) this.getLEtiquetas().get(i);
                this.selectEtiquetas[i] = etiquetasModel.getEtiqueta();
            }
            this.paginaAdmin = "nuevopost";
            return SUCCESS;
        } catch (Exception ex) {
            this.log.error("Error en addPost:\n" + this.getStacktrace(ex));
            return "error";
        }
    }

    public String doAddPost() {
        try {
            if (this.postModel.getContenido() != "") {
                this.postModel.setAutor(Configure.getProperty(Constants.NOMBREAUTOR));
                this.postModel.setComentarios("0");
                this.postModel.setFecha(getFechaActual());
                String link = "";
                link = formatearLink(this.postModel.getTitulo());
                this.postModel.setLink(link);
                String etiquetas = "";
                String[] selectEtiquetas = this.postModel.getSelectEtiquetas();
                for (int i = 0; i < selectEtiquetas.length; i++) {
                    etiquetas += selectEtiquetas[i] + ";";
                }
                if (!this.postModel.getTituloAntiguo().equalsIgnoreCase("")) {
                    IEtiquetasDto etiquetasModel = new EtiquetasModel();
                    String[] etiquetasRestar = this.postModel.getEtiquetas().split(";");
                    etiquetasModel.setEtiquetas(etiquetasRestar);
                    this.getIPrincipalFacade().restaEtiquetas(etiquetasModel);
                    this.postModel.setEtiquetas(etiquetas);
                    this.getIPrincipalFacade().updatePost(this.postModel);
                } else {
                    this.postModel.setEtiquetas(etiquetas);
                    this.getIPrincipalFacade().insertPost(this.postModel);
                }
                IEtiquetasDto etiquetasModel = new EtiquetasModel();
                etiquetasModel.setEtiquetas(selectEtiquetas);
                this.getIPrincipalFacade().sumaEtiquetas(etiquetasModel);
                this.setLEtiquetas(this.getIPrincipalFacade().getEtiquetas());
                Integer numeroEtiqueta = 0;
                String etiquetasNuevas = "";
                for (int a = 0; a < selectEtiquetas.length; a++) {
                    Boolean existe = false;
                    String getEtiqueta = "";
                    for (int b = 0; b < this.getLEtiquetas().size(); b++) {
                        IEtiquetasDto etiquetasModel2 = (EtiquetasModel) this.getLEtiquetas().get(b);
                        getEtiqueta = etiquetasModel2.getEtiqueta();
                        if (selectEtiquetas[a].equalsIgnoreCase(getEtiqueta)) {
                            existe = true;
                        }
                    }
                    if (existe == false) {
                        etiquetasNuevas += selectEtiquetas[a] + ";";
                        numeroEtiqueta++;
                    }
                }
                String[] addEtiquetas = etiquetasNuevas.split(";");
                if (addEtiquetas[0] != "") {
                    etiquetasModel.setEtiquetas(addEtiquetas);
                    this.getIPrincipalFacade().addEtiquetas(etiquetasModel);
                }
                return "index";
            } else {
                return "error";
            }
        } catch (Exception ex) {
            this.log.error("Error en doAddPost:\n" + this.getStacktrace(ex));
            return "error";
        }
    }

    public String formatearLink(String tituloPost) {
        String link = "";
        link = tituloPost.toLowerCase();
        link = link.replaceAll("&#\\d\\d\\d", "");
        link = link.replaceAll("_", "");
        link = link.replaceAll("�", "ny");
        link = link.replaceAll("�", "a");
        link = link.replaceAll("�", "e");
        link = link.replaceAll("�", "i");
        link = link.replaceAll("�", "o");
        link = link.replaceAll("�", "u");
        link = link.replaceAll("�", "NY");
        link = link.replaceAll("�", "A");
        link = link.replaceAll("�", "E");
        link = link.replaceAll("�", "I");
        link = link.replaceAll("�", "O");
        link = link.replaceAll("�", "P");
        link = link.replaceAll("\\W\\s", "");
        link = link.replaceAll("[(]", "").replaceAll("[)]", "");
        link = link.trim();
        link = link.replaceAll(" {2,}", "");
        link = link.replaceAll("[:]", "-");
        link = link.replaceAll("[.]", "-");
        link = link.replaceAll("[,]", "-");
        link = link.replaceAll(" ", "-");
        return link;
    }

    public String getFechaActual() {
        String fechaActual;
        Calendar fecha = Calendar.getInstance();
        int dia = fecha.get(Calendar.DAY_OF_MONTH);
        int mes = fecha.get(Calendar.MONTH) + 1;
        int year = fecha.get(Calendar.YEAR);
        int hora = fecha.get(Calendar.HOUR_OF_DAY);
        int minutos = fecha.get(Calendar.MINUTE);
        int segundos = fecha.get(Calendar.SECOND);
        String diastr = Integer.valueOf(dia).toString();
        String messtr = Integer.valueOf(mes).toString();
        String horastr = Integer.valueOf(hora).toString();
        String minutosstr = Integer.valueOf(minutos).toString();
        String segundosstr = Integer.valueOf(segundos).toString();
        if (diastr.length() == 1) {
            diastr = "0" + diastr;
        }
        if (messtr.length() == 1) {
            messtr = "0" + messtr;
        }
        if (horastr.length() == 1) {
            horastr = "0" + horastr;
        }
        if (minutosstr.length() == 1) {
            minutosstr = "0" + minutosstr;
        }
        if (segundosstr.length() == 1) {
            segundosstr = "0" + segundosstr;
        }
        fechaActual = Integer.valueOf(year).toString() + "-" + mes + "-" + diastr;
        fechaActual += " " + horastr + ":" + minutosstr + ":" + segundosstr;
        return fechaActual;
    }

    public String borrarPost() {
        List response = new ArrayList();
        try {
            if (getServletRequest().getParameter("post") != null) {
                String link = getServletRequest().getParameter("post").toString();
                IPostDto postModel = new PostModel();
                postModel.setLink(link);
                Integer success = this.getIPrincipalFacade().borrarPost(postModel);
                if (success == 1) {
                    postModel = this.getIPrincipalFacade().getPost(postModel);
                    IEtiquetasDto etiquetasModel = new EtiquetasModel();
                    String[] etiquetasRestar = postModel.getEtiquetas().split(";");
                    etiquetasModel.setEtiquetas(etiquetasRestar);
                    this.getIPrincipalFacade().restaEtiquetas(etiquetasModel);
                }
                response.add(success);
            }
        } catch (Exception ex) {
            this.log.error("Error en borrarPost:\n" + this.getStacktrace(ex));
            response.add("0");
        }
        this.setJsonModel(response);
        return SUCCESS;
    }

    public String borrarComentario() {
        List response = new ArrayList();
        try {
            if (getServletRequest().getParameter("post") != null || getServletRequest().getParameter("idComentario") != null) {
                String link = getServletRequest().getParameter("post").toString();
                String idComentario = getServletRequest().getParameter("idComentario").toString();
                IPostDto postModel = new PostModel();
                postModel.setLink(link);
                IComentariosDto comentarioModel = new ComentariosModel();
                comentarioModel.setIdComentarios(idComentario);
                Integer success = this.getIPrincipalFacade().borrarComentario(postModel, comentarioModel);
                response.add(success);
                if (success == 1) {
                    List lPost = this.getIPrincipalFacade().getEntrada(postModel);
                    postModel = (IPostDto) lPost.get(0);
                    response.add(postModel.getComentarios());
                }
            }
        } catch (Exception ex) {
            this.log.error("Error en borrarComentario:\n" + this.getStacktrace(ex));
            response.add("0");
        }
        this.setJsonModel(response);
        return SUCCESS;
    }

    public String personalizarMenu() {
        try {
            List lMenu = getIPrincipalFacade().getMenu();
            getServletRequest().setAttribute("lMenu", lMenu);
            this.paginaAdmin = "personalizarmenu";
            return SUCCESS;
        } catch (Exception ex) {
            this.log.error("Error en personalizarMenu:\n" + this.getStacktrace(ex));
            return "error";
        }
    }

    public String addPagina() {
        try {
            this.paginaAdmin = "addpagina";
            return SUCCESS;
        } catch (Exception ex) {
            this.log.error("Error en addPagina:\n" + this.getStacktrace(ex));
            return "error";
        }
    }

    public String doAddPagina() {
        try {
            if (this.menuModel.getContenido() != "") {
                if (!this.menuModel.getTituloAntiguo().equalsIgnoreCase("")) {
                    this.getIPrincipalFacade().updatePagina(this.menuModel);
                } else {
                    this.getIPrincipalFacade().insertPagina(this.menuModel);
                }
                return "index";
            } else {
                return "error";
            }
        } catch (Exception ex) {
            this.log.error("Error en doAddPagina:\n" + this.getStacktrace(ex));
            return "error";
        }
    }

    public String borrarPagina() {
        Integer success = 0;
        try {
            if (getServletRequest().getParameter("idPagina") != null) {
                String idPagina = getServletRequest().getParameter("idPagina").toString();
                IMenuDto menuModel = new MenuModel();
                menuModel.setIdMenu(idPagina);
                success = this.getIPrincipalFacade().borrarPagina(menuModel);
            }
        } catch (Exception ex) {
            this.log.error("Error en borrarPagina:\n" + this.getStacktrace(ex));
        }
        List lJson = new ArrayList();
        lJson.add(success);
        this.setJsonModel(lJson);
        return SUCCESS;
    }

    public String editarPagina() {
        try {
            if (getServletRequest().getParameter("idPagina") != null) {
                String idPagina = getServletRequest().getParameter("idPagina").toString();
                this.menuModel.setIdMenu(idPagina);
                this.menuModel = getIPrincipalFacade().getMenu(menuModel);
                this.paginaAdmin = "addpagina";
                return SUCCESS;
            } else {
                return "error";
            }
        } catch (Exception ex) {
            this.log.error("Error en editarPagina:\n" + this.getStacktrace(ex));
            return "error";
        }
    }

    public String getUser() {
        return this.user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public IConfiguracionDto getConfiguracionModel() {
        return this.configuracionModel;
    }

    public void setConfiguracionModel(IConfiguracionDto configuracionModel) {
        this.configuracionModel = configuracionModel;
    }

    public String getPaginaAdmin() {
        return this.paginaAdmin;
    }

    public void setPaginaAdmin(String paginaAdmin) {
        this.paginaAdmin = paginaAdmin;
    }

    public IPostDto getPostModel() {
        return this.postModel;
    }

    public void setPostModel(IPostDto postModel) {
        this.postModel = postModel;
    }

    public String[] getSelectEtiquetas() {
        return this.selectEtiquetas;
    }

    public void setSelectEtiquetas(String[] selectEtiquetas) {
        this.selectEtiquetas = selectEtiquetas;
    }

    public IMenuDto getMenuModel() {
        return this.menuModel;
    }

    public void setMenuModel(IMenuDto menuModel) {
        this.menuModel = menuModel;
    }
}
