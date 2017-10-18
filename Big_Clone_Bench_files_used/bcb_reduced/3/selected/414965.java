package com.amidasoft.lincat.session;

import com.amidasoft.lincat.entity.Usuaris;
import com.amidasoft.lincat.entity.Empreses;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.ResourceBundle;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.log.Log;
import org.jboss.seam.security.Credentials;
import org.jboss.seam.security.Identity;

@Name("authenticator")
public class Authenticator {

    @Logger
    Log log;

    @In
    Identity identity;

    @In
    Credentials credentials;

    @In(required = false, scope = ScopeType.SESSION)
    @Out
    private AltaEmpresa empresaFeta;

    @In
    private EntityManager entityManager;

    public boolean authenticate() {
        if (empresaFeta == null) empresaFeta = new AltaEmpresaBean();
        log.info("authenticating {0}", credentials.getUsername());
        boolean bo;
        try {
            String passwordEncriptat = credentials.getPassword();
            MessageDigest m = MessageDigest.getInstance("MD5");
            m.update(passwordEncriptat.getBytes(), 0, passwordEncriptat.length());
            passwordEncriptat = new BigInteger(1, m.digest()).toString(16);
            Query q = entityManager.createQuery("select usuari from Usuaris usuari where usuari.login=? and usuari.password=?");
            q.setParameter(1, credentials.getUsername());
            q.setParameter(2, passwordEncriptat);
            Usuaris usuari = (Usuaris) q.getSingleResult();
            bo = (usuari != null);
            if (bo) {
                if (usuari.isEsAdministrador()) {
                    identity.addRole("admin");
                } else {
                    carregaDadesEmpresa();
                    log.info("nom de l'empresa: " + empresaFeta.getInstance().getNom());
                }
            }
        } catch (Throwable t) {
            log.error(t);
            bo = false;
        }
        log.info("L'usuari {0} s'ha identificat bé? : {1} ", credentials.getUsername(), bo ? "si" : "no");
        return bo;
    }

    private void carregaDadesEmpresa() {
        Query q = entityManager.createQuery("select e from Empreses e where e.usuaris.login = ?");
        q.setParameter(1, credentials.getUsername());
        this.empresaFeta.setinstance((Empreses) q.getSingleResult());
        log.info("he carregat l'empresa " + this.credentials.getUsername());
    }
}
