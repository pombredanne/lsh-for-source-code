package siac.com.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.model.ListDataModel;
import javax.faces.model.SelectItem;
import siac.com.dao.AuthUtilizadorDao;
import siac.com.entity.AccMoeda;
import siac.com.entity.AuthUtilizador;
import siac.com.util.EncriptacaoUtil;
import siac.com.util.JsfUtil;
import siac.com.util.PaginaUtil;

@ManagedBean
@SessionScoped
public class AuthUtilizadorController {

    private AuthUtilizadorDao dao;

    private AuthUtilizador infoBean;

    private ListDataModel<AuthUtilizador> listaInfos;

    private String estadoCorrente;

    private String paginaDestino;

    private String tituloPagina;

    private String confirmaPassword;

    public AuthUtilizadorController() {
        this.infoBean = new AuthUtilizador();
        this.dao = new AuthUtilizadorDao();
        this.populatarLista();
    }

    public boolean validaLogin() {
        if ((infoBean.getUsername() == null || infoBean.getUsername().trim().toUpperCase().equals("")) && (infoBean.getPassword() == null || infoBean.getPassword().trim().toUpperCase().equals(""))) return false;
        return true;
    }

    private void populatarLista() {
        this.estadoCorrente = JsfUtil.ESTADO_PESQUISAR;
        this.tituloPagina = ResourceBundle.getBundle("siac.com.idioma.mensagens_pt_PT").getString("authUtilizadorTituloListar");
        this.listaInfos = new ListDataModel<AuthUtilizador>(this.dao.findAll());
    }

    /**
	 * Prepara view detalhe
	 */
    public String preparaDetalhes() {
        this.setEstadoCorrente(JsfUtil.ESTADO_DETALHES);
        this.tituloPagina = ResourceBundle.getBundle("siac.com.idioma.mensagens_pt_PT").getString("authUtilizadorTituloDetalhes");
        return null;
    }

    public String novo() {
        this.infoBean = new AuthUtilizador();
        return PaginaUtil.AUTHUTILIZADOR_CRUD;
    }

    /**
	 * Prepara view adicionar
	 */
    public String preparaAdicionar() {
        this.setEstadoCorrente(JsfUtil.ESTADO_ADICIONAR);
        this.tituloPagina = ResourceBundle.getBundle("siac.com.idioma.mensagens_pt_PT").getString("authUtilizadorTituloRegistar");
        return novo();
    }

    /**
	 * Adiciona usuario
	 */
    public String adicionar() {
        String senha = this.infoBean.getPassword();
        if (!senha.equals(this.confirmaPassword)) {
            JsfUtil.addErrorMessage(null, ResourceBundle.getBundle("siac.com.idioma.mensagens_pt_PT").getString("authUtilizadorConfirmarPasswordRequiredMessage"));
            return null;
        }
        try {
            byte[] b = EncriptacaoUtil.digest(infoBean.getPassword().getBytes(), "md5");
            infoBean.setPassword(EncriptacaoUtil.byteArrayToHexString(b));
            Date dt = new Date();
            infoBean.setDataRegisto((Date) dt.clone());
            infoBean.setDataAlteracao((Date) dt.clone());
            this.dao.create(infoBean);
            JsfUtil.addSuccessMessage(ResourceBundle.getBundle("siac.com.idioma.mensagens_pt_PT").getString("geralRegistoCriado"));
            return this.preparaAdicionar();
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, ResourceBundle.getBundle("siac.com.idioma.mensagens_pt_PT").getString("geralErroDePersistencia"));
            return null;
        }
    }

    /**
	 * Prepara view editar
	 */
    public String preparaEditar() {
        this.setEstadoCorrente(JsfUtil.ESTADO_ACTUALIZAR);
        this.tituloPagina = ResourceBundle.getBundle("siac.com.idioma.mensagens_pt_PT").getString("authUtilizadorTituloAlterar");
        return PaginaUtil.AUTHUTILIZADOR_CRUD;
    }

    /**
	 * Edita usu·rio
	 */
    public String editar() {
        String senha = this.infoBean.getPassword();
        if (!senha.equals(this.confirmaPassword)) {
            JsfUtil.addErrorMessage(null, ResourceBundle.getBundle("siac.com.idioma.mensagens_pt_PT").getString("authUtilizadorConfirmarPasswordRequiredMessage"));
            return null;
        }
        try {
            byte[] b = EncriptacaoUtil.digest(infoBean.getPassword().getBytes(), "md5");
            infoBean.setPassword(EncriptacaoUtil.byteArrayToHexString(b));
            Date dt = new Date();
            infoBean.setDataAlteracao((Date) dt.clone());
            this.dao.edit(infoBean);
            JsfUtil.addSuccessMessage(ResourceBundle.getBundle("siac.com.idioma.mensagens_pt_PT").getString("geralRegistoAlterado"));
            this.setEstadoCorrente(JsfUtil.ESTADO_FORMVAZIO);
            return null;
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, ResourceBundle.getBundle("siac.com.idioma.mensagens_pt_PT").getString("geralErroDePersistencia"));
            return null;
        }
    }

    /**
	 * Prepara view eliminar
	 */
    public String preparaEliminar() {
        this.setEstadoCorrente(JsfUtil.ESTADO_ELIMINAR);
        this.tituloPagina = ResourceBundle.getBundle("siac.com.idioma.mensagens_pt_PT").getString("authUtilizadorTituloEliminar");
        return PaginaUtil.AUTHUTILIZADOR_CRUD;
    }

    /**
	 * Exclui usuario
	 */
    public String eliminar() {
        try {
            this.dao.remove(infoBean);
            JsfUtil.addSuccessMessage(ResourceBundle.getBundle("siac.com.idioma.mensagens_pt_PT").getString("geralRegistoEliminado"));
            this.setEstadoCorrente(JsfUtil.ESTADO_FORMVAZIO);
            return null;
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, ResourceBundle.getBundle("siac.com.idioma.mensagens_pt_PT").getString("geralErroDePersistencia"));
            return null;
        }
    }

    /**
	 * Referente ao bot„o voltar
	 */
    public String voltar() {
        this.populatarLista();
        return PaginaUtil.AUTHUTILIZADOR_LISTA;
    }

    public SelectItem[] getItemsAvailableSelectOneEmpety() {
        List<AccMoeda> lista = new ArrayList<AccMoeda>();
        return JsfUtil.getSelectItems(lista, true);
    }

    public SelectItem[] getItemsAvailableSelectMany() {
        return JsfUtil.getSelectItems(this.dao.findAll(), false);
    }

    public SelectItem[] getItemsAvailableSelectOne() {
        return JsfUtil.getSelectItems(this.dao.findAll(), true);
    }

    public boolean isEstadoPesquisar() {
        String state = this.getEstadoCorrente();
        return (state == null || JsfUtil.ESTADO_PESQUISAR.equals(state));
    }

    public boolean isEstadoAdicionar() {
        return JsfUtil.ESTADO_ADICIONAR.equals(this.getEstadoCorrente());
    }

    public boolean isEstadoActualizar() {
        return JsfUtil.ESTADO_ACTUALIZAR.equals(this.getEstadoCorrente());
    }

    public boolean isEstadoEliminar() {
        return JsfUtil.ESTADO_ELIMINAR.equals(this.getEstadoCorrente());
    }

    public boolean isEstadoDetalhe() {
        return JsfUtil.ESTADO_DETALHES.equals(this.getEstadoCorrente());
    }

    public boolean isEstadoCrud() {
        return (this.isEstadoAdicionar() || this.isEstadoActualizar() || this.isEstadoEliminar() || this.isEstadoDetalhe());
    }

    public String getEstadoCorrente() {
        return estadoCorrente;
    }

    public void setEstadoCorrente(String estadoCorrente) {
        this.estadoCorrente = estadoCorrente;
    }

    public AuthUtilizadorDao getDao() {
        return dao;
    }

    public void setDao(AuthUtilizadorDao dao) {
        this.dao = dao;
    }

    public AuthUtilizador getInfoBean() {
        return infoBean;
    }

    public void setInfoBean(AuthUtilizador infoBean) {
        this.infoBean = infoBean;
    }

    public ListDataModel<AuthUtilizador> getListaInfos() {
        return listaInfos;
    }

    public void setListaInfos(ListDataModel<AuthUtilizador> listaInfos) {
        this.listaInfos = listaInfos;
    }

    public String getPaginaDestino() {
        return paginaDestino;
    }

    public void setPaginaDestino(String paginaDestino) {
        this.paginaDestino = paginaDestino;
    }

    public String getTituloPagina() {
        return tituloPagina;
    }

    public void setTituloPagina(String tituloPagina) {
        this.tituloPagina = tituloPagina;
    }

    /**
	 * @return the confirmaPassword
	 */
    public String getConfirmaPassword() {
        return confirmaPassword;
    }

    /**
	 * @param confirmaPassword the confirmaPassword to set
	 */
    public void setConfirmaPassword(String confirmaPassword) {
        this.confirmaPassword = confirmaPassword;
    }
}
