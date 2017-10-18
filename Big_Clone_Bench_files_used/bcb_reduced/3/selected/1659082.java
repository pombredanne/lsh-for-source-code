package org.opensih.gdq.Seguridad;

import java.security.MessageDigest;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.faces.context.FacesContext;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.security.Identity;
import org.opensih.gdq.ControladoresCU.IMantenimiento;
import org.opensih.gdq.Modelo.UnidadEjecutora;

@Stateless
@Name("utilsGDQ")
public class Utils implements IUtils {

    @EJB
    IMantenimiento logica;

    @PersistenceContext
    private EntityManager em;

    @In
    Identity identity;

    public String obtenerIP() {
        HttpServletRequest httpServletRequest = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        return httpServletRequest.getRemoteAddr();
    }

    @SuppressWarnings("unchecked")
    public String buscarUE(String ip) {
        List<UnidadEjecutora> lista = em.createQuery("SELECT ue FROM UnidadEjecutora ue WHERE ue.rangoIps LIKE '%'||:ip||'%'").setParameter("ip", ip).getResultList();
        if (lista.isEmpty()) {
            int i = ip.lastIndexOf('.');
            ip = ip.substring(0, i);
            lista = em.createQuery("SELECT ue FROM UnidadEjecutora ue WHERE ue.rangoIps LIKE '%'||:ip||'%'").setParameter("ip", ip).getResultList();
        }
        if (!lista.isEmpty()) return lista.get(0).getCodigo();
        return null;
    }

    public String buscarUE() {
        List<UnidadEjecutora> ues = logica.listarUnidades();
        for (UnidadEjecutora ue : ues) {
            if (identity.hasRole(ue.getCodigo())) return ue.getCodigo();
        }
        return null;
    }

    public UnidadEjecutora devolverUE() {
        List<UnidadEjecutora> ues = logica.listarUnidades();
        for (UnidadEjecutora ue : ues) {
            if (identity.hasRole(ue.getCodigo())) return em.find(UnidadEjecutora.class, ue.getCodigo());
        }
        return null;
    }

    private static String convertToHex(byte[] data) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9)) buf.append((char) ('0' + halfbyte)); else buf.append((char) ('a' + (halfbyte - 10)));
                halfbyte = data[i] & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }

    public String MD5(String text) {
        try {
            MessageDigest md;
            md = MessageDigest.getInstance("MD5");
            byte[] md5hash = new byte[32];
            md.update(text.getBytes("iso-8859-1"), 0, text.length());
            md5hash = md.digest();
            return convertToHex(md5hash);
        } catch (Exception e) {
            System.out.println(e.toString());
        }
        return null;
    }
}
