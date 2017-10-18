package com.inout.ejb;

import com.inout.dto.cierreDTO;
import com.inout.dto.marcaDTO;
import com.inout.entities.Cierre;
import com.inout.entities.Marca;
import com.inout.util.converters;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author pablo
 */
@Stateless
public class cierre implements cierreLocal {

    @EJB
    private marcaLocal marca;

    @PersistenceContext
    private EntityManager em;

    @Override
    public Boolean cerrarMes(Date desde, Date hasta) {
        try {
            Cierre cierre = new Cierre();
            cierre.setAno((short) desde.getYear());
            cierre.setMes(converters.convertirMes(desde.getMonth()));
            List<Marca> lista = marca.obtenerMarcasPorFechaAbierto(desde, hasta, null);
            if (lista != null && lista.size() > 0) {
                String toStringArray = "";
                em.persist(cierre);
                em.flush();
                for (Marca marcaEntity : lista) {
                    marcaEntity.setCierre(cierre);
                    marcaEntity.setCerrado(Boolean.TRUE);
                    marcaEntity.setCierre(cierre);
                    toStringArray += marcaEntity.toString();
                    em.merge(marcaEntity);
                    em.flush();
                }
                cierre.setCodigo(hash(toStringArray, "MD5"));
                em.merge(cierre);
                em.flush();
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            System.out.println("Ocurrio un error al cerrar el mes " + e.getMessage());
        }
        return false;
    }

    @Override
    public Cierre convertirDTOCierre(cierreDTO CierreDTO) {
        Cierre cierre = new Cierre();
        cierre.setAno(CierreDTO.getAno());
        cierre.setId(CierreDTO.getID());
        cierre.setMes(CierreDTO.getMes());
        return cierre;
    }

    @Override
    public cierreDTO convertirCierreDTO(Cierre cierre) {
        cierreDTO CierreDTO = new cierreDTO();
        CierreDTO.setAno(cierre.getAno());
        CierreDTO.setID(cierre.getId());
        CierreDTO.setMes(cierre.getMes());
        CierreDTO.setSeguridad(cierre.getCodigo());
        return CierreDTO;
    }

    private String hash(String text, String algorithm) throws NoSuchAlgorithmException {
        byte[] hash = MessageDigest.getInstance(algorithm).digest(text.getBytes());
        BigInteger bi = new BigInteger(1, hash);
        String result = bi.toString(16);
        if (result.length() % 2 != 0) {
            return "0" + result;
        }
        return result;
    }
}
