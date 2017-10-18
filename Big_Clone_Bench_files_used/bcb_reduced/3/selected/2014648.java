package org.hmaciel.sisingr.seguridadClave;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Date;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.hmaciel.sisingr.ejb.entity.PersonBean;
import org.hmaciel.sisingr.ejb.role.PatientBean;

@Stateless
public class CU_AsignarClave implements ICU_AsignarClave {

    @PersistenceContext
    private EntityManager em;

    String password;

    public String asignarClave(PatientBean patBean) {
        PersonBean p = (PersonBean) patBean.getPlayer();
        String ext = patBean.getIds().get(0).getExtension();
        String usuario = ext.substring(0, ext.length() - 1) + "-" + ext.substring(ext.length() - 1);
        String clave = genPass();
        if (existeUsuario(usuario)) {
            jos_users ju = getUsuario(usuario);
            ju.setPassword(clave);
            em.merge(ju);
        } else {
            String nombre = p.toString();
            String mail = usuario + "@paciente.com";
            jos_users ju = new jos_users();
            ju.setActivation("");
            ju.setBlock(0);
            ju.setEmail(mail);
            ju.setGid(18);
            ju.setLastvisitDate(null);
            ju.setName(nombre);
            ju.setParams("");
            ju.setPassword(clave);
            ju.setRegisterDate(null);
            ju.setSendEmail(0);
            ju.setUsername(usuario);
            ju.setUsertype("Paciente");
            em.persist(ju);
            em.flush();
            jos_core_acl_aro jcaa = new jos_core_acl_aro();
            jcaa.setHidden(0);
            jcaa.setName(nombre);
            jcaa.setOrder_value(0);
            jcaa.setSection_value("users");
            jcaa.setValue(ju.getId() + "");
            em.persist(jcaa);
            em.flush();
            jos_core_acl_groups_aro_map jcagam = new jos_core_acl_groups_aro_map();
            jcagam.setAro_id(jcaa.getId());
            jcagam.setGroup_id(34);
            jcagam.setSection_value("");
            em.persist(jcagam);
        }
        return password;
    }

    public String genPass() {
        String salto = "Z1mX502qLt2JTcW9MTDTGBBw8VBQQmY2";
        String clave = (int) (Math.random() * 10) + "" + (int) (Math.random() * 10) + "" + (int) (Math.random() * 10) + "" + (int) (Math.random() * 10) + "" + (int) (Math.random() * 10) + "" + (int) (Math.random() * 10) + "" + (int) (Math.random() * 10);
        password = clave;
        String claveConSalto = clave + salto;
        MessageDigest m = null;
        try {
            m = MessageDigest.getInstance("MD5");
            m.update(claveConSalto.getBytes("utf-8"), 0, claveConSalto.length());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String claveCifrada = new BigInteger(1, m.digest()).toString(16);
        return claveCifrada + ":" + salto;
    }

    @SuppressWarnings("unchecked")
    public boolean existeUsuario(String username) {
        List existe = em.createQuery("select u from jos_users u where u.username=:username").setParameter("username", username).getResultList();
        return (existe.isEmpty()) ? false : true;
    }

    @SuppressWarnings("unchecked")
    public jos_users getUsuario(String username) {
        List user = em.createQuery("select u from jos_users u where u.username=:username").setParameter("username", username).getResultList();
        return (jos_users) ((user.isEmpty()) ? null : user.get(0));
    }
}
