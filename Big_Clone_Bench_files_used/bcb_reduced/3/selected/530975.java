package org.hmaciel.descop.ejb.controladores;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.hmaciel.descop.ejb.act.DocClinBean;
import org.hmaciel.descop.ejb.entity.OrganizationBean;
import org.hmaciel.descop.ejb.role.EmploymentBean;
import org.hmaciel.descop.ejb.role.RoleBean;
import org.hmaciel.descop.otros.PlantillaBean;
import org.hmaciel.descop.otros.Sala;
import org.hmaciel.descop.otros.XMLUtils;
import org.hmaciel.descop.otros.Anexo.AnexoDescripcion;
import org.hmaciel.descop.otros.JefeServicio.IJefesCUso;
import org.hmaciel.descop.otros.Puntaje.Tabla_Pago;
import org.hmaciel.descop.otros.Usuarios.Ilogica;
import org.hmaciel.descop.otros.Usuarios.Usuario;
import org.opensih.servicioJMX.invocador.InvocadorService2;
import org.opensih.servicioJMX.invocador.InvocadorServiceBean2;

@Stateless
public class Migracion implements IMigracion {

    @EJB
    Ilogica usuarios;

    @EJB
    IJefesCUso logicaJ;

    @PersistenceContext
    private EntityManager em;

    @SuppressWarnings("unchecked")
    public List<String> obtenerDocClins(int i, int j) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        List<DocClinBean> docs = em.createQuery("select d from DocClinBean d").getResultList();
        List<String> resultado = new LinkedList<String>();
        int x = 0;
        for (DocClinBean d : docs) {
            if (x >= i && x <= j) {
                try {
                    List<AnexoDescripcion> list2 = em.createQuery("select a from AnexoDescripcion a where a.ii_extDoc=:id_a").setParameter("id_a", d.getIi().getExtension()).getResultList();
                    if (list2.isEmpty()) {
                        resultado.add(d.makeCDA(""));
                    } else {
                        String anexo = "\r\n\r\n" + "Anexo de la Descrpci�n Operatoria" + "\r\n" + "Autor:" + list2.get(0).getAutor() + " " + "Fecha:" + sdf.format(list2.get(0).getEffectiveTime()) + "\r\n" + list2.get(0).getText().substring(5, list2.get(0).getText().length() - 6);
                        resultado.add(d.makeCDA(anexo));
                    }
                    System.out.println("Se creo CDA de DescOp: " + x);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Fallo en la numero: " + x + " ID: " + d.getIi().getExtension());
                }
            }
            x++;
        }
        return resultado;
    }

    @SuppressWarnings("unchecked")
    public List<String> obtenerDocClins2(int i, int j) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        List<DocClinBean> docs = em.createQuery("select d from DocClinBean d").getResultList();
        List<String> resultado = new LinkedList<String>();
        int x = 0;
        for (DocClinBean d : docs) {
            if (x >= i && x <= j) {
                try {
                    List<AnexoDescripcion> list2 = em.createQuery("select a from AnexoDescripcion a where a.ii_extDoc=:id_a").setParameter("id_a", d.getIi().getExtension()).getResultList();
                    if (list2.isEmpty()) {
                        grabarArchivo2(x + ".txt", d.makeCDA(""));
                    } else {
                        String anexo = "\r\n\r\n" + "Anexo de la Descrpci�n Operatoria" + "\r\n" + "Autor:" + list2.get(0).getAutor() + " " + "Fecha:" + sdf.format(list2.get(0).getEffectiveTime()) + "\r\n" + list2.get(0).getText().substring(5, list2.get(0).getText().length() - 6);
                        grabarArchivo2(x + ".txt", d.makeCDA(anexo));
                    }
                    System.out.println("Se creo CDA de DescOp: " + x);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Fallo en la numero: " + x + " ID: " + d.getIi().getExtension());
                }
            }
            x++;
        }
        return resultado;
    }

    @SuppressWarnings("unchecked")
    public String obtenerDoc(String doc) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        List<DocClinBean> docs = em.createQuery("select d from DocClinBean d join d.id ids where ids.extension=:id_doc").setParameter("id_doc", doc).getResultList();
        String resultado = "";
        if (!docs.isEmpty()) {
            DocClinBean d = docs.get(0);
            try {
                List<AnexoDescripcion> list2 = em.createQuery("select a from AnexoDescripcion a where a.ii_extDoc=:id_a").setParameter("id_a", d.getIi().getExtension()).getResultList();
                if (list2.isEmpty()) {
                    resultado = d.makeCDA("");
                } else {
                    String anexo = "\r\n\r\n" + "Anexo de la Descrpci�n Operatoria" + "\r\n" + "Autor:" + list2.get(0).getAutor() + " " + "Fecha:" + sdf.format(list2.get(0).getEffectiveTime()) + "\r\n" + list2.get(0).getText().substring(5, list2.get(0).getText().length() - 6);
                    resultado = d.makeCDA(anexo);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return resultado;
    }

    @SuppressWarnings("unchecked")
    public List<String> obtenerPlantillas() {
        List<PlantillaBean> plants = em.createQuery("select p from PlantillaBean p").getResultList();
        List<String> result = new LinkedList<String>();
        Usuario user;
        String tex;
        for (PlantillaBean p : plants) {
            user = usuarios.obtenerUsuario(p.getAutor());
            if (user != null) {
                tex = "<root>" + "<autor>" + XMLUtils.parse(user.getCi()) + "</autor>" + "<nombre>" + XMLUtils.parse(p.getNombre()) + "</nombre>" + "<texto>" + XMLUtils.parse(p.getTexto()) + "</texto>" + "<checksum>" + MD5(user.getCi() + p.getNombre() + p.getTexto()) + "</checksum>" + "</root>";
                result.add(tex);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public List<String> obtenerPago() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        List<Tabla_Pago> list = em.createQuery("select t from Tabla_Pago t").getResultList();
        List<String> result = new LinkedList<String>();
        String tex;
        for (Tabla_Pago t : list) {
            tex = "<root>" + "<Asa>" + XMLUtils.parse(t.getAsa()) + "</Asa>" + "<CiAn1>" + ((t.getCiAn1() == null) ? "" : XMLUtils.parse(t.getCiAn1().trim())) + "</CiAn1>" + "<CiAn2>" + ((t.getCiAn2() == null) ? "" : XMLUtils.parse(t.getCiAn2().trim())) + "</CiAn2>" + "<CiAy1>" + ((t.getCiAy1() == null) ? "" : XMLUtils.parse(t.getCiAy1().trim())) + "</CiAy1>" + "<CiAy2>" + ((t.getCiAy2() == null) ? "" : XMLUtils.parse(t.getCiAy2().trim())) + "</CiAy2>" + "<CiAy3>" + ((t.getCiAy3() == null) ? "" : XMLUtils.parse(t.getCiAy3().trim())) + "</CiAy3>" + "<CiCir1>" + ((t.getCiCir1() == null) ? "" : XMLUtils.parse(t.getCiCir1().trim())) + "</CiCir1>" + "<CiCir2>" + ((t.getCiCir2() == null) ? "" : XMLUtils.parse(t.getCiCir2().trim())) + "</CiCir2>" + "<CiJefe>" + ((t.getCiJefe() == null) ? "" : XMLUtils.parse(t.getCiJefe().trim())) + "</CiJefe>" + "<CiJefeAn>" + ((t.getCiJefeAn() == null) ? "" : XMLUtils.parse(t.getCiJefeAn().trim())) + "</CiJefeAn>" + "<CiPaciente>" + ((t.getCiPaciente() == null) ? "" : XMLUtils.parse(t.getCiPaciente().trim())) + "</CiPaciente>" + "<CiResp>" + ((t.getCiResp() == null) ? "" : XMLUtils.parse(t.getCiResp().trim())) + "</CiResp>" + "<Codigo>" + XMLUtils.parse(t.getCodigo()) + "</Codigo>" + "<IdDoc>" + XMLUtils.parse(t.getIdDoc()) + "</IdDoc>" + "<NombreAn1>" + ((t.getNombreAn1() == null) ? "" : XMLUtils.parse(t.getNombreAn1())) + "</NombreAn1>" + "<NombreAn2>" + ((t.getNombreAn2() == null) ? "" : XMLUtils.parse(t.getNombreAn2())) + "</NombreAn2>" + "<NombreAy1>" + ((t.getNombreAy1() == null) ? "" : XMLUtils.parse(t.getNombreAy1())) + "</NombreAy1>" + "<NombreAy2>" + ((t.getNombreAy2() == null) ? "" : XMLUtils.parse(t.getNombreAy2())) + "</NombreAy2>" + "<NombreAy3>" + ((t.getNombreAy3() == null) ? "" : XMLUtils.parse(t.getNombreAy3())) + "</NombreAy3>" + "<NombreCir1>" + ((t.getNombreCir1() == null) ? "" : XMLUtils.parse(t.getNombreCir1())) + "</NombreCir1>" + "<NombreCir2>" + ((t.getNombreCir2() == null) ? "" : XMLUtils.parse(t.getNombreCir2())) + "</NombreCir2>" + "<NombreJefe>" + ((t.getNombreJefe() == null) ? "" : XMLUtils.parse(t.getNombreJefe())) + "</NombreJefe>" + "<NombreJefeAn>" + ((t.getNombreJefeAn() == null) ? "" : XMLUtils.parse(t.getNombreJefeAn())) + "</NombreJefeAn>" + "<NombrePaciente>" + ((t.getNombrePaciente() == null) ? "" : XMLUtils.parse(t.getNombrePaciente())) + "</NombrePaciente>" + "<NombreResp>" + ((t.getNombreResp() == null) ? "" : XMLUtils.parse(t.getNombreResp())) + "</NombreResp>" + "<Otros>" + XMLUtils.parse(t.getOtros()) + "</Otros>" + "<PtosAnestesista>" + t.getPtosAnestesista() + "</PtosAnestesista>" + "<PtosAyudante>" + t.getPtosAyudante() + "</PtosAyudante>" + "<PtosCirujano>" + t.getPtosCirujano() + "</PtosCirujano>" + "<Servicio>" + XMLUtils.parse(t.getServicio()) + "</Servicio>" + "<FinCirugia>" + sdf.format(t.getFinCirugia()) + "</FinCirugia>" + "<InicioCirugia>" + sdf.format(t.getInicioCirugia()) + "</InicioCirugia>" + "<checksum>" + MD5(t.getAsa() + ((t.getCiAn1() == null) ? "" : t.getCiAn1().trim()) + ((t.getCiAn2() == null) ? "" : t.getCiAn2().trim()) + ((t.getCiAy1() == null) ? "" : t.getCiAy1().trim()) + ((t.getCiAy2() == null) ? "" : t.getCiAy2().trim()) + ((t.getCiAy3() == null) ? "" : t.getCiAy3().trim()) + ((t.getCiCir1() == null) ? "" : t.getCiCir1().trim()) + ((t.getCiCir2() == null) ? "" : t.getCiCir2().trim()) + ((t.getCiJefe() == null) ? "" : t.getCiJefe().trim()) + ((t.getCiJefeAn() == null) ? "" : t.getCiJefeAn().trim()) + ((t.getCiPaciente() == null) ? "" : t.getCiPaciente().trim()) + ((t.getCiResp() == null) ? "" : t.getCiResp().trim()) + t.getCodigo() + t.getIdDoc() + ((t.getNombreAn1() == null) ? "" : t.getNombreAn1()) + ((t.getNombreAn2() == null) ? "" : t.getNombreAn2()) + ((t.getNombreAy1() == null) ? "" : t.getNombreAy1()) + ((t.getNombreAy2() == null) ? "" : t.getNombreAy2()) + ((t.getNombreAy3() == null) ? "" : t.getNombreAy3()) + ((t.getNombreCir1() == null) ? "" : t.getNombreCir1()) + ((t.getNombreCir2() == null) ? "" : t.getNombreCir2()) + ((t.getNombreJefe() == null) ? "" : t.getNombreJefe()) + ((t.getNombreJefeAn() == null) ? "" : t.getNombreJefeAn()) + ((t.getNombrePaciente() == null) ? "" : t.getNombrePaciente()) + ((t.getNombreResp() == null) ? "" : t.getNombreResp()) + t.getOtros() + t.getPtosAnestesista() + t.getPtosAyudante() + t.getPtosCirujano() + t.getServicio() + sdf.format(t.getFinCirugia()) + sdf.format(t.getInicioCirugia())) + "</checksum>" + "</root>";
            result.add(tex);
        }
        return result;
    }

    public List<String> obtenerUsuarios() {
        List<Usuario> users = usuarios.listarUsuarios2();
        List<String> result = new LinkedList<String>();
        String tex;
        for (Usuario u : users) {
            String roles = "";
            String roles2 = "";
            for (String rol : u.getRoles()) {
                roles += "<privilegio>" + XMLUtils.parse(rol) + "</privilegio>";
                roles2 += rol;
            }
            String hab = "";
            if (u.isEstaHabilitado()) hab = "SI"; else hab = "NO";
            tex = "<root>" + "<ci>" + XMLUtils.parse(u.getCi().trim()) + "</ci>" + "<nombre>" + XMLUtils.parse(u.getNombre()) + "</nombre>" + "<apellido>" + XMLUtils.parse(u.getApellido()) + "</apellido>" + "<habilitado>" + hab + "</habilitado>" + "<password>" + MD5(u.getPassword()) + "</password>" + roles + "<checksum>" + MD5(u.getCi().trim() + u.getNombre() + hab + MD5(u.getPassword()) + u.getApellido() + roles2) + "</checksum>" + "</root>";
            result.add(tex);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public List<String> obtenerTecnicos() {
        List<EmploymentBean> tecs = em.createQuery("select e from EmploymentBean e").getResultList();
        List<String> result = new LinkedList<String>();
        String tex;
        for (EmploymentBean t : tecs) {
            String hab = "";
            if (t.getStatusCode().getCode().equals("activo")) hab = "SI"; else hab = "NO";
            tex = "<root>" + "<ci>" + XMLUtils.parse(t.getPlayer().getId().get(0).getRoot().trim() + "_" + t.getPlayer().getId().get(0).getExtension().trim()) + "</ci>" + "<nombre>" + XMLUtils.parse(t.getPlayer().getName().get(0).given()) + "</nombre>" + "<apellido>" + XMLUtils.parse(t.getPlayer().getName().get(0).family()) + "</apellido>" + "<rol>" + XMLUtils.parse(t.getJobCode().getTipoPart()) + "</rol>" + "<habilitado>" + hab + "</habilitado>" + "<caja>" + XMLUtils.parse(t.getIds().get(0).getExtension()) + "</caja>" + "<checksum>" + MD5(t.getPlayer().getName().get(0).given() + hab + t.getJobCode().getTipoPart() + t.getIds().get(0).getExtension() + t.getPlayer().getName().get(0).family() + t.getPlayer().getId().get(0).getRoot().trim() + "_" + t.getPlayer().getId().get(0).getExtension().trim()) + "</checksum>" + "</root>";
            result.add(tex);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public List<String> obtenerServicios() {
        List<OrganizationBean> list = em.createQuery("select org from OrganizationBean org join org.determinerCode determinerCode where determinerCode.code='Servicio'").getResultList();
        List<String> result = new LinkedList<String>();
        String tex;
        for (OrganizationBean s : list) {
            RoleBean j = logicaJ.buscarJefe(s);
            tex = "<root>" + "<nombre>" + XMLUtils.parse(s.toString().trim()) + "</nombre>" + "<jefe>" + ((j != null) ? XMLUtils.parse(j.getPlayer().getId().get(0).getExtension()) : "") + "</jefe>" + "<habilitado>SI</habilitado>" + "<checksum>" + MD5(((j != null) ? j.getPlayer().getId().get(0).getExtension() : "") + s.toString().trim()) + "</checksum>" + "</root>";
            result.add(tex);
        }
        list = em.createQuery("select org from OrganizationBean org join org.determinerCode determinerCode where determinerCode.code='Servicio Deshabilitado'").getResultList();
        for (OrganizationBean s : list) {
            RoleBean j = logicaJ.buscarJefe(s);
            tex = "<root>" + "<nombre>" + XMLUtils.parse(s.toString().trim()) + "</nombre>" + "<jefe>" + ((j != null) ? XMLUtils.parse(j.getPlayer().getId().get(0).getExtension()) : "") + "</jefe>" + "<habilitado>NO</habilitado>" + "<checksum>" + MD5(((j != null) ? j.getPlayer().getId().get(0).getExtension() : "") + s.toString().trim()) + "</checksum>" + "</root>";
            result.add(tex);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public List<String> obtenerSalas() {
        List<Sala> salas = em.createQuery("select s from Sala s").getResultList();
        List<String> result = new LinkedList<String>();
        String tex;
        for (Sala s : salas) {
            tex = "<root>" + "<nombre>" + XMLUtils.parse(s.getNombre()) + "</nombre>" + "<checksum>" + MD5(s.getNombre()) + "</checksum>" + "</root>";
            result.add(tex);
        }
        return result;
    }

    public String obtenerUE() {
        InvocadorService2 inv = InvocadorServiceBean2.getInstance();
        return inv.getUnidadEjecutora();
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

    private String MD5(String text) {
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

    public static void grabarArchivo(String nom, String texto) {
        Writer output = null;
        File file = new File(nom);
        try {
            output = new BufferedWriter(new FileWriter(file));
            output.write(texto);
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void grabarArchivo2(String nom, String texto) {
        try {
            Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(nom), "UTF-8"));
            out.write(texto);
            out.close();
        } catch (Exception e) {
        }
    }
}
