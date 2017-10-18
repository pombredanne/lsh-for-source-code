package br.org.databasetools.core.security;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import br.org.databasetools.core.ApplicationContext;
import br.org.databasetools.core.connection.TransactionManager;
import br.org.databasetools.core.view.window.IWindow;

public abstract class SecurityManager {

    public static final int OPERACAO_VISUALIZAR = 0;

    public static final int OPERACAO_INSERIR = 1;

    public static final int OPERACAO_ALTERAR = 2;

    public static final int OPERACAO_EXCLUIR = 3;

    public static final int OPERACAO_EXECUTAR = 4;

    protected static Log LOG = LogFactory.getLog(SecurityManager.class);

    protected static SecurityManager me;

    protected SecurityUserBean userLogged;

    protected HashMap<Integer, SecurityUserBean> userCached = new HashMap<Integer, SecurityUserBean>();

    protected SecurityManager() {
    }

    public static SecurityManager getInstance() {
        return me;
    }

    public SecurityUserBean findUserByID(int userId) throws SecurityException {
        StringBuffer sql = new StringBuffer();
        sql.append("select usu.* from teseu.usuario usu ");
        sql.append(" where usu.usu_id = ? ");
        try {
            ResultSet rs = TransactionManager.getInstance().openQuery(sql.toString(), new Object[] { userId });
            SecurityUserBean bean = null;
            if (rs.next()) {
                bean = new SecurityUserBean();
                bean.setFilialId(rs.getInt("fil_id"));
                bean.setId(rs.getInt("usu_id"));
                bean.setNome(rs.getString("usu_nome"));
                bean.setLogin(rs.getString("usu_login"));
                bean.setSituacao(rs.getInt("usu_situacao"));
            } else {
                throw new SecurityException("N�o foi poss�vel recuperar o usu�rio com esta identifica��o.");
            }
            TransactionManager.getInstance().closeQuery(rs);
            return bean;
        } catch (SecurityException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SecurityException("Falhou a consulta do usu�rio!", ex);
        }
    }

    public SecurityTaskBean findTaskByCode(String taskCode) throws SecurityException {
        StringBuffer sql = new StringBuffer();
        sql.append("select tar.* from teseu.tarefa tar ");
        sql.append(" where tar.tar_codigo = ? ");
        try {
            ResultSet rs = TransactionManager.getInstance().openQuery(sql.toString(), new Object[] { taskCode });
            SecurityTaskBean bean = null;
            if (rs.next()) {
                bean = new SecurityTaskBean();
                bean.setId(rs.getInt("tar_id"));
                bean.setCodigo(rs.getString("tar_codigo"));
                bean.setCodigoArvore(rs.getString("tar_codigoarvore"));
                bean.setDescricao(rs.getString("tar_descricao"));
                bean.setClasse(rs.getString("tar_classe"));
                bean.setDica(rs.getString("tar_dica"));
                bean.setItemMenuPrincipal(rs.getInt("tar_itemmenuprincipal"));
                bean.setIconeBarraPrincipal(rs.getString("tar_iconebarraprincipal"));
                bean.setAtalhoBarraPrincipal(rs.getInt("tar_atalhobarraprincipal"));
                bean.setOrdemApresentacao(rs.getInt("tar_ordemapresentacao"));
                bean.setTeclaAtalho(rs.getString("tar_teclaatalho"));
                bean.setControlaAcesso(rs.getInt("tar_controlaacesso"));
            } else {
                throw new SecurityException("N�o foi poss�vel recuperar uma tarefa com esta identifica��o: " + taskCode);
            }
            TransactionManager.getInstance().closeQuery(rs);
            return bean;
        } catch (SecurityException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SecurityException("Falhou a consulta das tarefas!", ex);
        }
    }

    public boolean isTaskAllowed(Integer userId, String taskCode) {
        SecurityUserBean userb = this.userCached.get(userId);
        if (userb.getTasks() == null) userb.setTasks(getUserAllowedTasks(userb.getId()));
        if ((userb != null) && (userb.getTask(taskCode) != null)) {
            SecurityTaskBean taskb = userb.getTask(taskCode);
            return (taskb.getExecutar() == 1);
        }
        return false;
    }

    public boolean isTaskAllowed(String taskCode, int operacao) {
        SecurityUserBean userb = this.userCached.get(getUsuarioLogado().getId());
        if (userb.getTasks() == null) userb.setTasks(getUserAllowedTasks(userb.getId()));
        if ((userb != null) && (userb.getTask(taskCode) != null)) {
            SecurityTaskBean taskb = userb.getTask(taskCode);
            switch(operacao) {
                case OPERACAO_VISUALIZAR:
                    return (taskb.getVisualizar() == 1);
                case OPERACAO_INSERIR:
                    return (taskb.getInserir() == 1);
                case OPERACAO_ALTERAR:
                    return (taskb.getAlterar() == 1);
                case OPERACAO_EXCLUIR:
                    return (taskb.getExcluir() == 1);
                case OPERACAO_EXECUTAR:
                    return (taskb.getExecutar() == 1);
            }
        }
        return false;
    }

    public boolean isTaskAllowed(int userId, String taskCode, int operacao) {
        SecurityUserBean userb = this.userCached.get(userId);
        if (userb.getTasks() == null) userb.setTasks(getUserAllowedTasks(userb.getId()));
        if ((userb != null) && (userb.getTask(taskCode) != null)) {
            SecurityTaskBean taskb = userb.getTask(taskCode);
            switch(operacao) {
                case OPERACAO_VISUALIZAR:
                    return (taskb.getVisualizar() == 1);
                case OPERACAO_INSERIR:
                    return (taskb.getInserir() == 1);
                case OPERACAO_ALTERAR:
                    return (taskb.getAlterar() == 1);
                case OPERACAO_EXCLUIR:
                    return (taskb.getExcluir() == 1);
                case OPERACAO_EXECUTAR:
                    return (taskb.getExecutar() == 1);
            }
        }
        return false;
    }

    public boolean isUserLogged(int userId) {
        if ((this.userLogged != null) && (this.userLogged.getId() == userId)) {
            return true;
        }
        return false;
    }

    public boolean isUser(int userId) {
        try {
            getUsuarioCached(userId);
            return true;
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
            return false;
        }
    }

    public boolean isUserActive(int userId) {
        try {
            SecurityUserBean bean = getUsuarioCached(userId);
            return (bean != null) && (bean.getSituacao() == 1);
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
            return false;
        }
    }

    public boolean isUserAndPasswordValid(int userId, String password) throws Exception {
        SecurityUserBean bean = this.getUsuarioCached(userId);
        if (bean != null) {
            String tmp = bean.getLogin();
            String encripty = encryptGR(password);
            return tmp.trim().equals(encripty.trim());
        } else {
            throw new SecurityException("Usuario n�o foi encontrado! [Usu�rio: " + userId + "]");
        }
    }

    public SecurityUserBean getUsuarioLogado() {
        return this.userLogged;
    }

    protected SecurityUserBean getUsuarioCached(int userId) throws Exception {
        if ((this.userCached != null) && (this.userCached.containsKey(userId))) {
            return this.userCached.get(userId);
        } else {
            SecurityUserBean bean = findUserByID(userId);
            if (this.userCached == null) {
                this.userCached = new HashMap<Integer, SecurityUserBean>();
            }
            this.userCached.put(new Integer(userId), bean);
            return bean;
        }
    }

    protected abstract List<SecurityTaskBean> getUserAllowedTasks(int userId);

    public abstract void login() throws Exception;

    public abstract void loginConfirm() throws Exception;

    public abstract void loginConfirm(IWindow parent) throws Exception;

    public abstract SecurityUserBean loginUser() throws Exception;

    public abstract SecurityUserBean loginUser(IWindow parent, String title) throws Exception;

    public void login(Integer userId, String password) throws Exception {
        try {
            if (userId == null) throw new SecurityException("O usu�rio n�o foi informado");
            if (password == null) throw new SecurityException("A senha n�o foi informada");
            if (isUser(userId) == false) throw new SecurityException("Usuario informado n�o existe");
            if (isUserActive(userId) == false) throw new SecurityException("Usuario informado n�o esta ativo");
            if (isUserAndPasswordValid(userId, password) == false) throw new SecurityException("A senha informada esta errada.");
            this.userLogged = getUsuarioCached(userId);
            this.userLogged.setTasks(this.getUserAllowedTasks(userId));
            this.userLogged.setLoginDigitado(password);
        } catch (SecurityException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SecurityException("N�o foi poss�vel efetuar o login!", ex);
        }
    }

    public void loginDeveloper() throws Exception {
        try {
            Integer userId = Integer.parseInt(ApplicationContext.getInstance().getProperty("login.id"));
            Integer filialId = Integer.parseInt(ApplicationContext.getInstance().getProperty("login.filial.id"));
            String userNome = ApplicationContext.getInstance().getProperty("login.name");
            this.userLogged = findUserByID(userId);
            this.userLogged.setFilialId(filialId);
            this.userLogged.setNome(userNome);
            this.userLogged.setSituacao(1);
            this.userLogged.setTasks(this.getUserAllowedTasks(this.userLogged.getId()));
            this.userCached.put(this.userLogged.getId(), this.userLogged);
            this.userLogged.setLoginDigitado("");
        } catch (Exception ex) {
            throw new SecurityException("As vari�veis necess�rias para login autom�tico n�o est�o setadas.");
        }
    }

    public void loginScheduler() throws Exception {
        try {
            if (ApplicationContext.getInstance().getProperty("scheduler.user.id") == null) {
                throw new SecurityException("As vari�veis necess�rias para login autom�tico scheduler n�o est�o setadas. \nNao encontrei scheduler.user.id");
            }
            Integer userId = Integer.parseInt(ApplicationContext.getInstance().getProperty("scheduler.user.id"));
            Integer filialId = Integer.parseInt(ApplicationContext.getInstance().getProperty("login.filial.id"));
            String userNome = ApplicationContext.getInstance().getProperty("login.name");
            this.userLogged = findUserByID(userId);
            this.userLogged.setFilialId(filialId);
            this.userLogged.setNome(userNome);
            this.userLogged.setSituacao(1);
            this.userLogged.setTasks(this.getUserAllowedTasks(this.userLogged.getId()));
            this.userCached.put(this.userLogged.getId(), this.userLogged);
            this.userLogged.setLoginDigitado("");
        } catch (Exception ex) {
            throw new SecurityException("As vari�veis necess�rias para login autom�tico n�o est�o setadas.", ex);
        }
    }

    public static String encrypt(String word) throws SecurityException {
        try {
            return encryptGR(word);
        } catch (Exception ex) {
            throw new SecurityException(ex);
        }
    }

    public static String encryptMD5(String word) throws SecurityException {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            BigInteger hash = new BigInteger(1, md.digest(word.getBytes()));
            String s = hash.toString(16);
            if (s.length() % 2 != 0) s = "0" + s;
            return s;
        } catch (Exception ex) {
            throw new SecurityException(ex);
        }
    }

    public static String encryptGR(String value) {
        String nova = "";
        String velha = value.trim();
        int qte = velha.length();
        int soma = 0;
        int letra = 0;
        int mudado = 0;
        String muda = new StringBuffer(velha).reverse().toString();
        for (int i = 0; i < qte; i++) {
            soma = soma + muda.charAt(i);
        }
        for (int i = 0; i < qte; i++) {
            letra = muda.charAt(i);
            mudado = soma % letra;
            if ((mudado > 0) && (mudado < 255)) {
                nova = nova + (char) mudado;
            } else {
                nova = nova + (char) 5;
            }
        }
        while (nova.length() < 20) {
            nova = nova + (char) 32;
        }
        return nova;
    }
}
