package com.amidasoft.lincat.session;

import com.amidasoft.lincat.entity.Contactes;
import com.amidasoft.lincat.entity.Empreses;
import com.amidasoft.lincat.entity.EmpresesSeccions;
import com.amidasoft.lincat.entity.Seccions;
import com.amidasoft.lincat.entity.Usuaris;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.regex.Pattern;
import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Begin;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.annotations.security.Delete;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.log.Log;
import org.jboss.seam.security.Credentials;
import org.jboss.seam.security.Identity;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.cache.JbossCacheProvider;

/**
 *
 * @author ricard
 */
@Name("empresaFeta")
@Scope(ScopeType.SESSION)
@Stateful
public class AltaEmpresaBean implements AltaEmpresa {

    @In(create = true)
    UsuarisHome usuarisHome;

    private Empreses instance;

    private Contactes unContacte = new Contactes();

    @In(create = true)
    @Out
    private FacesMessages facesMessages;

    @Logger
    private Log log;

    @In
    Identity identity;

    @PersistenceContext
    private EntityManager em;

    @In(create = true)
    Credentials credentials;

    private boolean empresaCreada;

    @Create
    public void crea() {
        carregaEmpresaActual();
        this.empresaCreada = false;
        System.err.print("EMPRESA CARREGADA: " + this.instance.getNom() + "; ID: " + instance.getId());
    }

    @Destroy
    @Delete
    @Remove
    public void destrueix() {
    }

    public AltaEmpresaBean() {
        super();
    }

    public boolean comprovaDades() {
        boolean nom = instance.getNom().length() > 0;
        boolean nif = instance.getNif().length() > 0;
        boolean login = instance.getUsuaris().getLogin().length() > 0;
        boolean password1 = instance.getUsuaris().getPassword().length() > 0;
        boolean password2 = instance.getUsuaris().getPassword2().length() > 0;
        boolean passwordsIguals = instance.getUsuaris().getPassword().equals(instance.getUsuaris().getPassword2());
        boolean emailContacte = instance.getEmailContacte().length() > 0 && ((Pattern.compile("(\\w+)@(\\w+\\.)(\\w+)(\\.\\w+)*")).matcher(instance.getEmailContacte())).matches();
        if (!nom) {
            facesMessages.add("Si us plau, introdueix un nom correcte");
        }
        if (!nif) {
            facesMessages.add("Si us plau, introdueix un CIF correcte");
        }
        if (!login) {
            facesMessages.add("Si us plau, introdueix un login correcte");
        }
        if (!password1) {
            facesMessages.add("Si us plau, introdueix el password1");
        }
        if (!password2) {
            facesMessages.add("Si us plau, introdueix el password2");
        }
        if (password1 && password2 && !passwordsIguals) {
            facesMessages.add("T'has equivocat amb el password. Entra'l de nou als dos camps.");
        }
        if (!emailContacte) {
            facesMessages.add("Si us plau, introdueix un email de contacte correcte");
        }
        boolean retorn = nom && nif && login && password1 && password2 && passwordsIguals && emailContacte;
        return retorn;
    }

    public boolean comprovaDadesActualitzacio() {
        boolean nom = instance.getNom().length() > 0;
        boolean nif = instance.getNif().length() > 0;
        boolean emailContacte = instance.getEmailContacte().length() > 0 && ((Pattern.compile("(\\w+)@(\\w+\\.)(\\w+)(\\.\\w+)*")).matcher(instance.getEmailContacte())).matches();
        if (!nom) {
            facesMessages.add("Si us plau, introdueix un nom correcte");
        }
        if (!nif) {
            facesMessages.add("Si us plau, introdueix un CIF correcte");
        }
        if (!emailContacte) {
            facesMessages.add("Si us plau, introdueix un email de contacte correcte");
        }
        boolean retorn = nom && nif && emailContacte;
        System.out.println(".--.-.-.-.-.-.-.-DADES CORRECTES!");
        return retorn;
    }

    @TransactionAttribute
    private String guardaLogo(String retorn) throws Exception {
        System.out.println(" -- - - - - - -- - - - instance.getLogo() = " + this.instance.getLogo() + "    $");
        byte[] imatgeBytes;
        imatgeBytes = instance.getLogoImatge();
        String ext = instance.getLogo().substring(instance.getLogo().lastIndexOf("."));
        String pathLogos = this.getPathLogos();
        String path = pathLogos + this.instance.getId() + ext;
        File imgPath = new File(path);
        System.out.println("-La imatge es guardarà a " + path);
        FileOutputStream imatgePerGuardar = new FileOutputStream(imgPath);
        try {
            imatgePerGuardar.write(imatgeBytes);
        } catch (Exception e) {
            retorn = "";
            log.error("Una excepcio guardant el logo: " + e.getMessage());
            StringWriter w = new StringWriter();
            e.printStackTrace(new PrintWriter(w));
            log.error(w.toString());
        } finally {
            imatgePerGuardar.close();
        }
        this.em.joinTransaction();
        instance = this.em.find(Empreses.class, instance.getId());
        instance.setLogo(instance.getId() + ext);
        this.em.persist(instance);
        this.em.flush();
        System.err.print("NOM DEL LOGO: " + this.instance.getLogo());
        return retorn;
    }

    private String xifraPassword() throws Exception {
        String password2 = instance.getUsuaris().getPassword2();
        MessageDigest m = MessageDigest.getInstance("MD5");
        m.update(password2.getBytes(), 0, password2.length());
        password2 = new BigInteger(1, m.digest()).toString(16);
        return password2;
    }

    private void guardaUsuari(Usuaris usuari) throws Exception {
        usuari.setLogin(instance.getUsuaris().getLogin());
        usuari.setPassword(instance.getUsuaris().getPassword());
        usuari.setPassword(xifraPassword());
        usuari.setNomComplet(instance.getNom());
        usuari.setEsAdministrador(false);
        usuari.setDataAlta(Calendar.getInstance().getTime());
        usuari.setDataBaixa(null);
    }

    @Begin(join = true)
    public String guardaEmpresa() {
        String retorn = "/AltaEmpresaSeccionsComarques.xhtml";
        try {
            if (comprovaDades()) {
                try {
                    Usuaris usuari = new Usuaris();
                    if (instance.getUsuaris().getPassword().equals(instance.getUsuaris().getPassword2())) {
                        System.out.println("--------------- Passwords iguals: CONTINUEM! -------------");
                        Query q = this.em.createQuery("select u.login from Usuaris u where u.login=?");
                        q.setParameter(1, instance.getUsuaris().getLogin());
                        if (q.getResultList().isEmpty()) {
                            System.out.println("-------------- Login valid, seguim! ------------");
                            guardaUsuari(usuari);
                            instance.setUsuaris(null);
                            System.err.print("ID EMPRESA: " + instance.getId());
                            this.em.persist(instance);
                            instance = em.find(Empreses.class, instance.getId());
                            System.err.print("ID EMPRESA: " + instance.getId());
                            this.em.flush();
                            log.debug("Fet el persist de l'empresa");
                            if (instance.getLogoImatge() != null && instance.getLogo().lastIndexOf(".") > 0) {
                                System.out.println("------------- ANEM A GUARDAR EL LOGO! ---------------");
                                retorn = guardaLogo(retorn);
                            } else {
                                System.out.println("------------- NO HI HA LOGO PER GUARDAR! -------------");
                            }
                            Set<Empreses> empresesUsuari = new HashSet<Empreses>();
                            empresesUsuari.add(instance);
                            usuari.setEmpreseses(empresesUsuari);
                            this.em.persist(usuari);
                            usuari = this.em.find(Usuaris.class, usuari.getId());
                            instance.setUsuaris(usuari);
                            this.em.flush();
                            this.credentials.setUsername(this.instance.getUsuaris().getLogin());
                            this.empresaCreada = true;
                        } else {
                            System.out.println("----------- Login ja existent! ----------------");
                            facesMessages.add("Aquest nom d'usuari ja està ocupat");
                            throw new Exception("Aquest nom d'usuari ja està ocupat");
                        }
                    } else {
                        System.out.println("----------------- Passwords diferents! No s'ha guardat res. -----------------");
                        facesMessages.add("T'has equivocat amb el password.");
                        retorn = "";
                    }
                } catch (Exception e) {
                    retorn = "/AltaEmpresa.xhtml";
                    log.error("Una excepcio al guardar l'empresa!");
                    StringWriter w = new StringWriter();
                    e.printStackTrace(new PrintWriter(w));
                    log.error(w.toString());
                    facesMessages.add(e.getMessage());
                }
            } else {
                retorn = "/AltaEmpresa.xhtml";
            }
        } catch (Exception e) {
            facesMessages.add(e.getMessage());
            log.error(e);
            retorn = "/AltaEmpresa.xhtml";
        }
        return retorn;
    }

    public void carregaEmpresaActual() {
        System.out.println(" --------- COMENCEM!");
        if (this.em == null) {
            em = (EntityManager) Component.getInstance("entityManager", true);
        }
        String valorEM = (em != null ? em.toString() : "null");
        System.out.println("   EntityManager: " + valorEM);
        boolean registrat = true;
        if (this.credentials != null) {
            System.out.println("BREAKPOINT 1");
            if (this.credentials.getUsername() != null && this.credentials.getUsername().length() > 0) {
                System.out.println("BREAKPOINT 2");
                String valorCredentials = (credentials == null) ? "null" : credentials.getUsername();
                System.out.println("BREAKPOINT 3");
                Query q = this.em.createQuery("select e.id from Empreses e where e.usuaris.login = ?");
                q.setParameter(1, this.credentials.getUsername());
                System.out.println("BREAKPOINT 4");
                Integer empresaId = (Integer) q.getSingleResult();
                System.out.println("BREAKPOINT 5");
                instance = this.em.find(Empreses.class, empresaId);
                System.out.println("HEM CARREGAT UNA EMPRESA DE NOM: " + instance.getNom() + " I ID:" + instance.getId());
            } else {
                registrat = false;
            }
        } else {
            registrat = false;
        }
        if (!registrat) {
            if (this.instance == null) {
                this.instance = new Empreses();
                instance.setUsuaris(new Usuaris());
                System.out.println("HEM FET UN NEW PER L'INSTANCE!");
            }
        } else {
            System.out.println("Esta registrat amb el nom de " + credentials.getUsername());
        }
    }

    private void copiaDeInstance(Empreses e) {
        e.setDescripcio(this.instance.getDescripcio());
        e.setEmpresesFormacionses(this.instance.getEmpresesFormacionses());
        e.setEmpresesFormacionses_1(this.instance.getEmpresesFormacionses_1());
        e.setIndexActivitat(this.instance.getIndexActivitat());
        e.setLogo(this.instance.getLogo());
        System.out.println("Logo assignat a Empresa e: " + this.instance.getLogo());
        e.setLogoImatge(this.instance.getLogoImatge());
        e.setMagatzemM2(this.instance.getMagatzemM2());
        e.setNif(this.instance.getNif());
        e.setNom(this.instance.getNom());
        e.setNumCamions(this.instance.getNumCamions());
        e.setNumCotxes(this.instance.getNumCotxes());
        e.setNumFurgonetes(this.instance.getNumFurgonetes());
        e.setNumTecnics(this.instance.getNumTecnics());
        e.setNumTurismes(this.instance.getNumTurismes());
        e.setTransientSeccions(this.instance.getTransientSeccions());
        e.setUsuaris(this.instance.getUsuaris());
        e.setVenteses(this.instance.getVenteses());
    }

    @TransactionAttribute
    public String actualitzaEmpresa() {
        if (comprovaDadesActualitzacio()) {
            System.out.println("Logo que s'ha de posar: " + (this.instance.getLogo().equals("") ? "_blanc_" : this.instance.getLogo()));
            this.actualitzaLogo();
            System.err.print("Logo actualitzat, si cal");
            Empreses e = this.em.find(Empreses.class, this.instance.getId());
            String logobdd = (String) this.em.createQuery("select e.logo from Empreses e where e.id = " + this.instance.getId()).getSingleResult();
            System.out.println("Logo de la bdd.: " + logobdd);
            this.instance.setLogo(logobdd);
            copiaDeInstance(e);
            this.em.joinTransaction();
            this.em.flush();
        }
        return "/AltaEmpresaSeccionsComarques.seam";
    }

    @TransactionAttribute
    public void eliminaLogo() {
        String directoriLogos = this.getPathLogos();
        File f = new File(directoriLogos);
        File[] fitxers = f.listFiles();
        String nomLogo = "" + this.instance.getId() + ".";
        int i = 0;
        boolean fi = i >= fitxers.length;
        boolean trobat = fitxers[i].getName().startsWith(nomLogo, 0);
        while (!trobat && !fi) {
            System.err.print("fitxer: " + fitxers[i].getAbsolutePath());
            i++;
            fi = i >= fitxers.length;
            if (!fi) {
                trobat = fitxers[i].getName().startsWith(nomLogo, 0);
            }
        }
        if (trobat) {
            System.err.print("fitxer a eliminar: " + fitxers[i].getAbsolutePath());
            fitxers[i].delete();
        } else {
            System.err.print("No s'ha trobat cap logo que comences per " + nomLogo);
        }
        this.em.joinTransaction();
        this.instance = this.em.find(Empreses.class, this.instance.getId());
        this.instance.setLogo("");
        this.em.persist(this.instance);
        this.em.flush();
        System.out.println(".-.-.-.-.-.-.- Logo antic eliminat ok -.-.-.-.-.-.-.-");
    }

    private void actualitzaLogo() {
        Empreses empresa = this.em.find(Empreses.class, this.instance.getId());
        String logoNou = "" + this.instance.getLogo();
        if (empresa.getLogo() != null && !"".equals(empresa.getLogo()) && !"".equals(logoNou)) {
            System.out.println("ANEM A ACTUALITZAR EL LOGO!");
            try {
                eliminaLogo();
                this.instance.setLogo(logoNou);
                guardaLogo("/AltaEmpresa.seam");
            } catch (Exception e) {
                System.out.println("Una excepcio guardant el logo: " + e.getMessage());
                StringWriter w = new StringWriter();
                e.printStackTrace(new PrintWriter(w));
            }
        } else {
            System.out.println("NO S'HAN FET CANVIS AL LOGO! El logo actual es: " + this.instance.getLogo());
        }
    }

    public void setInstance(Empreses instance) {
        this.instance = instance;
    }

    public void setinstance(Empreses instance) {
        this.instance = instance;
    }

    public Empreses getInstance() {
        return instance;
    }

    public Empreses getinstance() {
        return instance;
    }

    public void setUnContacte(Contactes unContacte) {
        this.unContacte = unContacte;
    }

    public Contactes getUnContacte() {
        return unContacte;
    }

    public void wire() {
        if (this.credentials != null) {
            if (this.credentials.getUsername() != null) {
                getInstance();
                Usuaris usuaris = (usuarisHome == null) ? null : usuarisHome.getDefinedInstance();
                if (usuaris != null) {
                    this.instance.setUsuaris(usuaris);
                }
            }
        }
    }

    public boolean isWired() {
        return true;
    }

    public String veureEmpresa() {
        String valorCredentials;
        String valorEM;
        System.err.print("ABANS:");
        valorCredentials = (credentials != null ? credentials.getUsername() : "null");
        System.err.print("   Credentials: " + valorCredentials);
        valorEM = (em != null ? em.toString() : "null");
        System.err.print("   EntityManager: " + valorEM);
        System.err.print("   Nom: " + instance.getNom());
        System.err.print("   NIF/CIF: " + instance.getNif());
        System.err.print("   Usuari: " + instance.getUsuaris().getLogin());
        if (credentials == null) {
            credentials = new Credentials();
        }
        credentials.setUsername(instance.getUsuaris().getLogin());
        credentials.setPassword(instance.getUsuaris().getPassword());
        valorCredentials = (credentials != null ? credentials.getUsername() : "null");
        System.err.print("   Credentials: " + valorCredentials);
        carregaEmpresaActual();
        System.err.print("DESPRES:");
        valorCredentials = (credentials != null ? credentials.getUsername() : "null");
        System.err.print("   Credentials: " + valorCredentials);
        valorEM = (em != null ? em.toString() : "null");
        System.err.print("   EntityManager: " + valorEM);
        System.err.print("   S'ha carregat l'empresa");
        System.err.print("   Nom: " + instance.getNom());
        System.err.print("   NIF/CIF: " + instance.getNif());
        System.err.print("   Usuari: " + instance.getUsuaris().getLogin());
        return "/AltaEmpresa.seam";
    }

    public boolean isEmpresaCreada() {
        return empresaCreada;
    }

    public void setEmpresaCreada(boolean empresaCreada) {
        this.empresaCreada = empresaCreada;
    }

    /**
     * Indica que cal tornar a la pantalla principal.
     * @return cadena identificativa de la vista a mostrar,
     */
    public String tornaAPrincipal() {
        return "/principal.xhtml";
    }

    public String getPathAplicacio() {
        ResourceBundle propietats = ResourceBundle.getBundle("propietats");
        return propietats.getString("pathAplicacio");
    }

    public String getSubpathLogos() {
        ResourceBundle propietats = ResourceBundle.getBundle("propietats");
        return propietats.getString("subpathLogos_save");
    }

    public String getPathLogos() {
        return this.getPathAplicacio() + this.getSubpathLogos();
    }

    public String creaNou() {
        this.instance = new Empreses();
        System.out.println(".-.-.-.-.-.-.-.-.-.-.- HE PASSAT PER EL empresaFeta.creaNou() !!!!!!!!!!!!!!!!!!!!!!!! hauria d'estar tot en blanc, ara...");
        return "/AltaEmpresa.seam";
    }
}
