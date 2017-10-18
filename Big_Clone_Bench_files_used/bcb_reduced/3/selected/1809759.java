package es.caib.redose.persistence.ejb;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import javax.ejb.CreateException;
import net.sf.hibernate.Hibernate;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Query;
import net.sf.hibernate.Session;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.StringUtils;
import es.caib.redose.model.Documento;
import es.caib.redose.model.Firma;
import es.caib.redose.model.LogOperacion;
import es.caib.redose.model.Plantilla;
import es.caib.redose.model.PlantillaIdioma;
import es.caib.redose.model.TipoOperacion;
import es.caib.redose.model.TipoUso;
import es.caib.redose.model.Ubicacion;
import es.caib.redose.model.Uso;
import es.caib.redose.model.Version;
import es.caib.redose.model.VersionCustodia;
import es.caib.redose.modelInterfaz.ConstantesRDS;
import es.caib.redose.modelInterfaz.DocumentoRDS;
import es.caib.redose.modelInterfaz.DocumentoVerifier;
import es.caib.redose.modelInterfaz.ExcepcionRDS;
import es.caib.redose.modelInterfaz.KeyVerifier;
import es.caib.redose.modelInterfaz.ReferenciaRDS;
import es.caib.redose.modelInterfaz.TransformacionRDS;
import es.caib.redose.modelInterfaz.UsoRDS;
import es.caib.redose.persistence.delegate.DelegateUtil;
import es.caib.redose.persistence.delegate.PlantillaDelegate;
import es.caib.redose.persistence.delegate.UbicacionDelegate;
import es.caib.redose.persistence.delegate.VersionDelegate;
import es.caib.redose.persistence.formateadores.FormateadorDocumento;
import es.caib.redose.persistence.formateadores.FormateadorDocumentoFactory;
import es.caib.redose.persistence.plugin.PluginAlmacenamientoRDS;
import es.caib.redose.persistence.plugin.PluginClassCache;
import es.caib.redose.persistence.util.ConversorOpenOffice;
import es.caib.redose.persistence.util.UtilRDS;
import es.caib.sistra.plugins.NoExistePluginException;
import es.caib.sistra.plugins.PluginFactory;
import es.caib.sistra.plugins.custodia.PluginCustodiaIntf;
import es.caib.sistra.plugins.firma.FirmaIntf;
import es.caib.sistra.plugins.firma.PluginFirmaIntf;
import es.caib.util.StringUtil;
import es.indra.util.pdf.BarcodeStamp;
import es.indra.util.pdf.ObjectStamp;
import es.indra.util.pdf.SelloEntradaStamp;
import es.indra.util.pdf.TextoStamp;
import es.indra.util.pdf.UtilPDF;

/**
 * SessionBean que implementa la interfaz del RDS para los dem�s
 * m�dulos de la Plataforma Telem�tica.
 *
 * @ejb.bean
 *  name="redose/persistence/RdsFacade"
 *  jndi-name="es.caib.redose.persistence.RdsFacade"
 *  type="Stateless"
 *  view-type="remote" *  transaction-type="Container"
 *
 * @ejb.transaction type="Required"
 * 
 * 
 * 
 * TODO: Hay que implementar acceso local a los EJBs
 *
 */
public abstract class RdsFacadeEJB extends HibernateEJB {

    private static final String LISTAR_USOS = "LIUS";

    private static final String NUEVO_DOCUMENTO = "NUDO";

    private static final String ELIMININAR_USOS = "ELUO";

    private static final String ELIMINAR_USO = "ELUS";

    private static final String NUEVO_USO = "NUUS";

    private static final String CONSULTAR_DOCUMENTO_FORMATEADO = "CODF";

    private static final String CONSULTAR_DOCUMENTO = "CODO";

    private static final String ACTUALIZAR_DOCUMENTO = "ACDO";

    private static final String ASOCIAR_FIRMA = "AFDO";

    private static final String ACTUALIZAR_FICHERO = "ACFI";

    private static final String BORRADO_AUTOMATICO_DOCUMENTO_SIN_USOS = "BODO";

    private String URL_VERIFIER = null;

    private String TEXT_VERIFIER = null;

    private String ENTORNO = null;

    private String OPENOFFICE_HOST = null;

    private String OPENOFFICE_PUERTO = null;

    private boolean existeCustodia = false;

    /**
     * @ejb.create-method
     * @ejb.permission unchecked = "true"
     */
    public void ejbCreate() throws CreateException {
        super.ejbCreate();
        try {
            Properties props = DelegateUtil.getConfiguracionDelegate().obtenerConfiguracion();
            URL_VERIFIER = props.getProperty("sistra.url") + "/redosefront/init.do?id=";
            TEXT_VERIFIER = props.getProperty("verifier.text");
            ENTORNO = props.getProperty("entorno");
            OPENOFFICE_HOST = props.getProperty("openoffice.host");
            OPENOFFICE_PUERTO = props.getProperty("openoffice.port");
            try {
                PluginFactory.getInstance().getPluginCustodia();
                existeCustodia = true;
            } catch (NoExistePluginException nep) {
                existeCustodia = false;
            }
        } catch (Exception ex) {
            log.error("No se pueden acceder propiedades modulo", ex);
            throw new CreateException("No se pueden obtener propiedades modulo");
        }
    }

    /**
	 * Inserta un documento en el RDS
	 * 
     * @ejb.interface-method
     * @ejb.permission role-name="${role.user}"
     * @ejb.permission role-name="${role.auto}"
     */
    public ReferenciaRDS insertarDocumento(DocumentoRDS documento) throws ExcepcionRDS {
        ReferenciaRDS ref = this.grabarDocumento(documento, true);
        this.doLogOperacion(getUsuario(), NUEVO_DOCUMENTO, "inserci�n documento " + ref.getCodigo());
        return ref;
    }

    /**
	 * Inserta un documento en el RDS permitiendo transformar el documento (p.e. convertir a PDF). <br/>
	 * 
     * @ejb.interface-method
     * @ejb.permission role-name="${role.user}"
     * @ejb.permission role-name="${role.auto}"
     */
    public ReferenciaRDS insertarDocumento(DocumentoRDS documento, TransformacionRDS transformacion) throws ExcepcionRDS {
        try {
            if (transformacion.existeTransformacion() && documento.getFirmas() != null && documento.getFirmas().length > 0) {
                throw new Exception("Si se realiza transformacion el documento no puede llevar asociadas firmas, ya que se modifica el documento");
            }
            if (transformacion.isConvertToPDF() && !verificarExtensionConversionPDF(documento.getExtensionFichero())) {
                throw new Exception("No se permite la conversion a PDF para la extension " + documento.getExtensionFichero().toLowerCase());
            }
            if (transformacion.isBarcodePDF() && !transformacion.isConvertToPDF() && !"pdf".equalsIgnoreCase(documento.getExtensionFichero())) {
                throw new Exception("El barcode solo se aplica sobre pdf");
            }
            if (transformacion.isConvertToPDF()) {
                byte[] pdf = this.convertirFicheroAPDF(documento.getDatosFichero(), documento.getExtensionFichero());
                documento.setDatosFichero(pdf);
                documento.setExtensionFichero("pdf");
                documento.setNombreFichero(documento.getNombreFichero().substring(0, documento.getNombreFichero().lastIndexOf(".")) + ".pdf");
            }
            ReferenciaRDS ref = this.grabarDocumento(documento, true);
            documento.setReferenciaRDS(ref);
            if (transformacion.isBarcodePDF()) {
                this.stampBarCodeVerifier(documento, null, null);
                this.grabarDocumento(documento, false);
            }
            this.doLogOperacion(getUsuario(), NUEVO_DOCUMENTO, "inserci�n documento " + ref.getCodigo());
            return ref;
        } catch (Exception ex) {
            throw new ExcepcionRDS("Error al insertar documento aplicando transformacion", ex);
        }
    }

    /**
     * Actualiza un documento en el RDS
     * 
     * @ejb.interface-method
     * @ejb.permission role-name="${role.user}"
     */
    public void actualizarDocumento(DocumentoRDS documento) throws ExcepcionRDS {
        this.grabarDocumento(documento, false);
        this.doLogOperacion(getUsuario(), ACTUALIZAR_DOCUMENTO, "actualizaci�n documento " + documento.getReferenciaRDS().getCodigo());
    }

    /**
     * Actualiza el fichero de un documento en el RDS. Recalcula el hash y elimina las firmas asociadas.
     * 
     * @ejb.interface-method
     * @ejb.permission role-name="${role.user}"
     */
    public void actualizarFichero(ReferenciaRDS ref, byte[] datos) throws ExcepcionRDS {
        DocumentoRDS doc = this.consultarDocumento(ref, false);
        doc.setDatosFichero(datos);
        FirmaIntf[] firmas = {};
        doc.setFirmas(firmas);
        this.grabarDocumento(doc, false);
        this.doLogOperacion(getUsuario(), ACTUALIZAR_FICHERO, "actualizaci�n fichero " + doc.getReferenciaRDS().getCodigo());
    }

    /**
     * A�adir firma a un documento en el RDS
     * 
     * @ejb.interface-method
     * @ejb.permission role-name="${role.user}"
     */
    public void asociarFirmaDocumento(ReferenciaRDS refRds, FirmaIntf firma) throws ExcepcionRDS {
        Session session = getSession();
        try {
            Documento doc = (Documento) session.load(Documento.class, new Long(refRds.getCodigo()));
            if ("S".equals(doc.getBorrado())) throw new ExcepcionRDS("El documento " + doc.getCodigo() + " ha sido borrado por no tener usos");
            Hibernate.initialize(doc.getFirmas());
            byte[] bytesFirma = getBytesFirma(firma);
            DocumentoRDS documento = this.consultarDocumento(refRds, true);
            if (!this.verificarFirma(documento.getDatosFichero(), firma)) {
                throw new ExcepcionRDS("Error al verificar la firma del documento");
            }
            Firma fir = new Firma();
            fir.setFirma(bytesFirma);
            fir.setFormato(firma.getFormatoFirma());
            doc.addFirma(fir);
            session.update(doc);
            if (documento.getFirmas() == null) {
                FirmaIntf[] firmasIntf = { firma };
                documento.setFirmas(firmasIntf);
            } else {
                FirmaIntf[] firmasIntf = new FirmaIntf[documento.getFirmas().length + 1];
                for (int i = 0; i < documento.getFirmas().length; i++) {
                    firmasIntf[i] = documento.getFirmas()[i];
                }
                firmasIntf[documento.getFirmas().length] = firma;
                documento.setFirmas(firmasIntf);
            }
            custodiarDocumento(documento, doc, session);
        } catch (HibernateException he) {
            log.error("Error asociando firma a documento", he);
            throw new ExcepcionRDS("Error asociando firma a documento", he);
        } catch (ExcepcionRDS rdse) {
            throw rdse;
        } catch (Exception exc) {
            log.error("Error obteniendo bytes de la firma ", exc);
            throw new ExcepcionRDS("Error obteniendo bytes de la firma ", exc);
        } finally {
            close(session);
        }
        this.doLogOperacion(getUsuario(), ASOCIAR_FIRMA, "asociar firma a documento " + refRds.getCodigo());
    }

    /**
     *	Crea un uso para un documento 
     * 
     * @ejb.interface-method
     * @ejb.permission role-name="${role.user}"
     * @ejb.permission role-name="${role.auto}"
     */
    public void crearUso(UsoRDS usoRDS) throws ExcepcionRDS {
        Session session = getSession();
        try {
            if (usoRDS.getReferenciaRDS() == null) {
                log.error("No se ha indicado referencia RDS para crear uso");
                throw new ExcepcionRDS("No se ha indicado referencia RDS para crear uso");
            }
            if (usoRDS.getTipoUso() == null) {
                log.error("No se ha indicado tipo de uso para crear uso");
                throw new ExcepcionRDS("No se ha indicado tipo de uso para crear uso");
            }
            if (usoRDS.getReferencia() == null) {
                log.error("No se ha indicado referencia para crear uso");
                throw new ExcepcionRDS("No se ha indicado referencia para crear uso");
            }
            TipoUso tipoUso;
            try {
                tipoUso = (TipoUso) session.load(TipoUso.class, usoRDS.getTipoUso());
            } catch (Exception e) {
                log.error("No existe tipo de uso " + usoRDS.getTipoUso());
                throw new ExcepcionRDS("No existe tipo de uso " + usoRDS.getTipoUso(), e);
            }
            Documento documento;
            try {
                documento = (Documento) session.load(Documento.class, new Long(usoRDS.getReferenciaRDS().getCodigo()));
            } catch (Exception e) {
                log.error("No existe documento " + usoRDS.getReferenciaRDS().getCodigo());
                throw new ExcepcionRDS("No existe documento " + usoRDS.getReferenciaRDS().getCodigo(), e);
            }
            if ("S".equals(documento.getBorrado())) throw new ExcepcionRDS("El documento " + documento.getCodigo() + " ha sido borrado por no tener usos");
            if (!documento.getClave().equals(usoRDS.getReferenciaRDS().getClave())) {
                log.error("Clave de la referencia RDS no concuerda");
                throw new ExcepcionRDS("Clave de la referencia RDS no concuerda");
            }
            Uso uso = new Uso();
            uso.setTipoUso(tipoUso);
            uso.setDocumento(documento);
            uso.setFecha(new Date());
            uso.setReferencia(usoRDS.getReferencia());
            uso.setFechaSello(usoRDS.getFechaSello());
            session.save(uso);
        } catch (HibernateException he) {
            log.error("Error insertando uso", he);
            throw new ExcepcionRDS("Error insertando uso", he);
        } finally {
            close(session);
        }
        this.doLogOperacion(getUsuario(), NUEVO_USO, "creaci�n uso " + usoRDS.getTipoUso() + " para documento " + usoRDS.getReferenciaRDS().getCodigo() + "( referencia: " + usoRDS.getReferencia() + ")");
    }

    /**
     * Consulta un documento del RDS (datos del documento y fichero asociado)
     * 
     * @ejb.interface-method
     * @ejb.permission role-name="${role.user}"
     * @ejb.permission role-name="${role.auto}"
     */
    public DocumentoRDS consultarDocumento(ReferenciaRDS refRds) throws ExcepcionRDS {
        DocumentoRDS doc = consultarDocumento(refRds, true);
        return doc;
    }

    /**
     * Consulta un documento del RDS. Permite indicar si s�lo se recuperan los datos del documento o tambi�n el fichero asociado
     * 
     * @ejb.interface-method
     * @ejb.permission role-name="${role.user}"
     * @ejb.permission role-name="${role.auto}"
     */
    public DocumentoRDS consultarDocumento(ReferenciaRDS refRds, boolean recuperarFichero) throws ExcepcionRDS {
        Session session = getSession();
        Documento documento;
        DocumentoRDS documentoRDS;
        try {
            documento = (Documento) session.load(Documento.class, new Long(refRds.getCodigo()));
            if ("S".equals(documento.getBorrado())) throw new ExcepcionRDS("El documento " + documento.getCodigo() + " ha sido borrado por no tener usos");
            if (!documento.getClave().equals(refRds.getClave())) {
                throw new ExcepcionRDS("La clave no coincide");
            }
            Hibernate.initialize(documento.getFirmas());
            documentoRDS = establecerCamposDocumentoRDS(documento);
        } catch (HibernateException he) {
            log.error("Error consultando uso", he);
            throw new ExcepcionRDS("Error consultando uso", he);
        } finally {
            close(session);
        }
        try {
            if (recuperarFichero) {
                PluginAlmacenamientoRDS plugin = obtenerPluginAlmacenamiento(documento.getUbicacion().getPluginAlmacenamiento());
                documentoRDS.setDatosFichero(plugin.obtenerFichero(documento.getCodigo()));
            }
        } catch (Exception e) {
            log.error("No se ha podido obtener fichero en ubicaci�n " + documento.getUbicacion().getCodigoUbicacion(), e);
            throw new ExcepcionRDS("Error al guardar fichero", e);
        }
        this.doLogOperacion(getUsuario(), CONSULTAR_DOCUMENTO, "consulta documento " + refRds.getCodigo());
        return documentoRDS;
    }

    /**
     * Consulta un documento del RDS de tipo estructurado formateado con una plantilla
     * 
     * @ejb.interface-method
     * @ejb.permission role-name="${role.user}"
     * @ejb.permission role-name="${role.auto}"
     */
    public DocumentoRDS consultarDocumentoFormateado(ReferenciaRDS refRds, String idioma) throws ExcepcionRDS {
        Session session = getSession();
        DocumentoRDS documentoRDS;
        try {
            documentoRDS = consultarDocumento(refRds);
            if (!documentoRDS.isEstructurado()) return documentoRDS;
            Documento documento = (Documento) session.load(Documento.class, new Long(refRds.getCodigo()));
            PlantillaIdioma plantilla = null;
            if (documento.getVersion().getPlantillas().size() <= 0) {
                return documentoRDS;
            }
            if (documento.getPlantilla() != null) {
                plantilla = (PlantillaIdioma) documento.getPlantilla().getTraduccion(idioma);
            } else {
                for (Iterator it = documento.getVersion().getPlantillas().iterator(); it.hasNext(); ) {
                    Plantilla p = (Plantilla) it.next();
                    if (p.getDefecto() == 'S') {
                        plantilla = (PlantillaIdioma) p.getTraduccion(idioma);
                        break;
                    }
                }
                if (plantilla == null) {
                    plantilla = (PlantillaIdioma) (((Plantilla) documento.getVersion().getPlantillas().iterator().next()).getTraduccion(idioma));
                }
            }
            List usos = listarUsos(refRds);
            FormateadorDocumento format = FormateadorDocumentoFactory.getInstance().getFormateador(plantilla.getPlantilla().getFormateador().getClase());
            DocumentoRDS docFormateado = format.formatearDocumento(documentoRDS, plantilla, usos);
            boolean docValido = true;
            if (plantilla.getPlantilla().getSello() == 'S') {
                if (!stampSello(docFormateado, usos)) docValido = false;
            }
            if (plantilla.getPlantilla().getBarcode() == 'S' && docValido) {
                stampBarCodeVerifier(docFormateado, plantilla.getPlantilla().getTipo(), idioma);
            }
            if (isBorrador() || !docValido) {
                stampBorrador(docFormateado);
            }
            this.doLogOperacion(getUsuario(), CONSULTAR_DOCUMENTO_FORMATEADO, "consulta documento formateado " + refRds.getCodigo());
            return docFormateado;
        } catch (Exception he) {
            log.error("No se ha podido obtener documento formateado ", he);
            throw new ExcepcionRDS("No se ha podido obtener documento formateado ", he);
        } finally {
            close(session);
        }
    }

    /**
     * Consulta un documento del RDS de tipo estructurado formateado con una plantilla
     * generando una copia para el interesado y otra para la administracion
     * 
     * @ejb.interface-method
     * @ejb.permission role-name="${role.user}"
     * @ejb.permission role-name="${role.auto}"
     */
    public DocumentoRDS consultarDocumentoFormateadoCopiasInteresadoAdmon(ReferenciaRDS refRds, String idioma) throws ExcepcionRDS {
        try {
            DocumentoRDS documentoRDS = consultarDocumentoFormateado(refRds, idioma);
            documentoRDS.setDatosFichero(this.generarCopiasInteresadoAdministracion(documentoRDS.getDatosFichero()));
            return documentoRDS;
        } catch (Exception he) {
            log.error("No se ha podido obtener documento formateado ", he);
            throw new ExcepcionRDS("No se ha podido obtener documento formateado ", he);
        }
    }

    /**
     * Consulta un documento del RDS de tipo estructurado formateado con una plantilla
     * 
     * @ejb.interface-method
     * @ejb.permission role-name="${role.user}"
     * @ejb.permission role-name="${role.auto}"
     */
    public DocumentoRDS consultarDocumentoFormateado(ReferenciaRDS refRds, String tipoPlantilla, String idioma) throws ExcepcionRDS {
        Session session = getSession();
        DocumentoRDS documentoRDS;
        try {
            documentoRDS = consultarDocumento(refRds);
            if (!documentoRDS.isEstructurado()) return documentoRDS;
            Documento documento = (Documento) session.load(Documento.class, new Long(refRds.getCodigo()));
            if (!documento.getClave().equals(refRds.getClave())) {
                throw new ExcepcionRDS("La clave no coincide");
            }
            PlantillaIdioma plantilla = null;
            for (Iterator it = documento.getVersion().getPlantillas().iterator(); it.hasNext(); ) {
                Plantilla p = (Plantilla) it.next();
                if (p.getTipo().equals(tipoPlantilla)) {
                    plantilla = (PlantillaIdioma) p.getTraduccion(idioma);
                    break;
                }
            }
            if (plantilla == null) {
                throw new Exception("No se encuentra plantilla");
            }
            List usos = listarUsos(refRds);
            FormateadorDocumento format = FormateadorDocumentoFactory.getInstance().getFormateador(plantilla.getPlantilla().getFormateador().getClase());
            DocumentoRDS docFormateado = format.formatearDocumento(documentoRDS, plantilla, usos);
            boolean docValido = true;
            if (plantilla.getPlantilla().getSello() == 'S') {
                if (!stampSello(docFormateado, usos)) docValido = false;
            }
            if (plantilla.getPlantilla().getBarcode() == 'S' && docValido) {
                stampBarCodeVerifier(docFormateado, tipoPlantilla, idioma);
            }
            if (isBorrador() || !docValido) {
                stampBorrador(docFormateado);
            }
            this.doLogOperacion(getUsuario(), CONSULTAR_DOCUMENTO_FORMATEADO, "consulta documento formateado " + refRds.getCodigo());
            return docFormateado;
        } catch (Exception he) {
            log.error("No se ha podido obtener documento formateado ", he);
            throw new ExcepcionRDS("No se ha podido obtener documento formateado ", he);
        } finally {
            close(session);
        }
    }

    /**
    * 
    * Formatea un documento que no existe en el RDS a partir de una plantilla
    * 
    * @param documentoRDS XML a formatear. Debe tener establecidos los siguientes atributos: datosFichero,nombreFichero y titulo
    * @param modelo Modelo
    * @param version Version
    * @param tipoPlantilla Plantilla (si es nula se utiliza la por defecto)
    * @param idioma Idioma
    * @return
    * @throws ExcepcionRDS
    * 
    * @ejb.interface-method
    * @ejb.permission role-name="${role.user}"
    * @ejb.permission role-name="${role.auto}"
    */
    public DocumentoRDS formatearDocumento(DocumentoRDS documentoRDS, String modelo, int version, String tipoPlantilla, String idioma) throws ExcepcionRDS {
        try {
            VersionDelegate vd = DelegateUtil.getVersionDelegate();
            Version v = vd.obtenerVersionCompleta(modelo, version);
            if (v == null) {
                throw new Exception("No se encuentra versi�n documento");
            }
            PlantillaIdioma plantilla = null;
            for (Iterator it = v.getPlantillas().iterator(); it.hasNext(); ) {
                Plantilla p = (Plantilla) it.next();
                if (StringUtils.isEmpty(tipoPlantilla)) {
                    if (p.getDefecto() == 'S') {
                        plantilla = (PlantillaIdioma) p.getTraduccion(idioma);
                        break;
                    }
                } else {
                    if (p.getTipo().equals(tipoPlantilla)) {
                        plantilla = (PlantillaIdioma) p.getTraduccion(idioma);
                        break;
                    }
                }
            }
            if (plantilla == null) {
                throw new Exception("No se encuentra plantilla");
            }
            List usos = new ArrayList();
            FormateadorDocumento format = FormateadorDocumentoFactory.getInstance().getFormateador(plantilla.getPlantilla().getFormateador().getClase());
            DocumentoRDS docFormateado = format.formatearDocumento(documentoRDS, plantilla, usos);
            boolean docValido = true;
            if (plantilla.getPlantilla().getSello() == 'S') {
                if (!stampSello(docFormateado, usos)) docValido = false;
            }
            if (isBorrador() || !docValido) {
                stampBorrador(docFormateado);
            }
            return docFormateado;
        } catch (Exception he) {
            log.error("No se ha podido formatear documento ", he);
            throw new ExcepcionRDS("No se ha podido formatear documento ", he);
        }
    }

    /**
     * Elimina uso para un documento del RDS
     * 
     * @ejb.interface-method
     * @ejb.permission role-name="${role.user}"
     */
    public void eliminarUso(UsoRDS usoRDS) throws ExcepcionRDS {
        Session session = getSession();
        try {
            TipoUso tipoUso;
            try {
                tipoUso = (TipoUso) session.load(TipoUso.class, usoRDS.getTipoUso());
            } catch (Exception e) {
                log.error("No existe tipo de uso " + usoRDS.getTipoUso());
                throw new ExcepcionRDS("No existe tipo de uso " + usoRDS.getTipoUso(), e);
            }
            Documento documento;
            try {
                if (usoRDS.getReferenciaRDS() == null) throw new Exception("El uso no indica una refererencia RDS");
                documento = (Documento) session.load(Documento.class, new Long(usoRDS.getReferenciaRDS().getCodigo()));
            } catch (Exception e) {
                log.error("No existe documento " + usoRDS.getReferenciaRDS().getCodigo());
                throw new ExcepcionRDS("No existe documento " + usoRDS.getReferenciaRDS().getCodigo(), e);
            }
            if (!documento.getClave().equals(usoRDS.getReferenciaRDS().getClave())) {
                log.error("Clave de la referencia RDS no concuerda");
                throw new ExcepcionRDS("Clave de la referencia RDS no concuerda");
            }
            Query query = session.createQuery("FROM Uso AS u WHERE u.documento = :documento and u.tipoUso = :tipoUso and u.referencia = :referencia");
            query.setParameter("documento", documento);
            query.setParameter("tipoUso", tipoUso);
            query.setParameter("referencia", usoRDS.getReferencia());
            List result = query.list();
            if (result.isEmpty()) {
                return;
            }
            for (int i = 0; i < result.size(); i++) {
                Uso uso = (Uso) result.get(i);
                session.delete(uso);
            }
            documentoSinUsos(session, documento);
        } catch (Exception he) {
            log.error("No se ha podido eliminar uso", he);
            throw new ExcepcionRDS("No se ha podido eliminar uso", he);
        } finally {
            close(session);
        }
        this.doLogOperacion(getUsuario(), ELIMINAR_USO, "eliminar uso " + usoRDS.getTipoUso() + " para documento " + usoRDS.getReferenciaRDS().getCodigo() + " ( referencia = " + usoRDS.getReferencia() + ")");
    }

    /**
     * Eliminar usos que tienen una determinada referencia para varios documentos del RDS
     * 
     * @ejb.interface-method
     * @ejb.permission role-name="${role.user}"
     */
    public void eliminarUsos(String tipoUsoId, String referencia) throws ExcepcionRDS {
        Session session = getSession();
        try {
            TipoUso tipoUso;
            try {
                tipoUso = (TipoUso) session.load(TipoUso.class, tipoUsoId);
            } catch (Exception e) {
                log.error("No existe tipo de uso " + tipoUsoId);
                throw new ExcepcionRDS("No existe tipo de uso " + tipoUsoId, e);
            }
            Query query = session.createQuery("FROM Uso AS u WHERE u.tipoUso = :tipoUso and u.referencia = :referencia");
            query.setParameter("tipoUso", tipoUso);
            query.setParameter("referencia", referencia);
            List result = query.list();
            if (result.isEmpty()) {
                return;
            }
            for (int i = 0; i < result.size(); i++) {
                Uso uso = (Uso) result.get(i);
                Documento documento = uso.getDocumento();
                session.delete(uso);
                documentoSinUsos(session, documento);
            }
        } catch (Exception he) {
            log.error("No se ha podido eliminar usos documento", he);
            throw new ExcepcionRDS("No se ha podido eliminar usos documento", he);
        } finally {
            close(session);
        }
        this.doLogOperacion(getUsuario(), ELIMININAR_USOS, "eliminar usos " + tipoUsoId + " ( referencia = " + referencia + ")");
    }

    /**
     * Consulta usos para un documento del RDS
     * 
     * @ejb.interface-method
     * @ejb.permission role-name="${role.user}"
     * @ejb.permission role-name="${role.auto}"
     */
    public List listarUsos(ReferenciaRDS refRDS) throws ExcepcionRDS {
        Session session = getSession();
        List listaUsosRDS;
        try {
            Documento documento;
            try {
                documento = (Documento) session.load(Documento.class, new Long(refRDS.getCodigo()));
            } catch (Exception e) {
                log.error("No existe documento " + refRDS.getCodigo());
                throw new ExcepcionRDS("No existe documento " + refRDS.getCodigo(), e);
            }
            if ("S".equals(documento.getBorrado())) throw new ExcepcionRDS("El documento " + documento.getCodigo() + " ha sido borrado por no tener usos");
            if (!documento.getClave().equals(refRDS.getClave())) {
                log.error("Clave de la referencia RDS no concuerda");
                throw new ExcepcionRDS("Clave de la referencia RDS no concuerda");
            }
            Query query = session.createQuery("FROM Uso AS u WHERE u.documento = :documento");
            query.setParameter("documento", documento);
            List result = query.list();
            listaUsosRDS = new ArrayList();
            if (result.isEmpty()) {
                return listaUsosRDS;
            }
            for (int i = 0; i < result.size(); i++) {
                Uso uso = (Uso) result.get(i);
                UsoRDS usoRDS = new UsoRDS();
                usoRDS.setTipoUso(uso.getTipoUso().getCodigo());
                usoRDS.setReferencia(uso.getReferencia());
                usoRDS.setReferenciaRDS(refRDS);
                usoRDS.setFechaSello(uso.getFechaSello());
                listaUsosRDS.add(usoRDS);
            }
        } catch (HibernateException he) {
            log.error("Error al listar usos", he);
            throw new ExcepcionRDS("Error al listar usos", he);
        } finally {
            close(session);
        }
        this.doLogOperacion(getUsuario(), LISTAR_USOS, "listar usos documento " + refRDS.getCodigo());
        return listaUsosRDS;
    }

    /**
     * Verifica documento formateado generado por la plataforma
     * 
     * @ejb.interface-method
     * @ejb.permission unchecked = "true"
     */
    public DocumentoVerifier verificarDocumento(KeyVerifier key) throws ExcepcionRDS {
        try {
            ReferenciaRDS referenciaRDS = ResolveRDS.getInstance().resuelveRDS(key.getIdDocumento().longValue());
            if (!key.verifyClaveRDS(referenciaRDS.getClave())) throw new ExcepcionRDS("Clave de acceso incorrecta");
            DocumentoRDS docRDS = null;
            docRDS = consultarDocumento(referenciaRDS, true);
            DocumentoRDS docRDSFormat = consultarDocumentoFormateado(referenciaRDS, key.getPlantillaDocumento(), key.getIdiomaDocumento());
            DocumentoVerifier docVer = new DocumentoVerifier();
            docVer.setTitulo(docRDS.getTitulo());
            docVer.setEstructurado(docRDS.isEstructurado());
            docVer.setNombreFichero(docRDS.getNombreFichero());
            docVer.setDatosFichero(docRDS.getDatosFichero());
            ;
            docVer.setNombreFicheroFormateado(docRDSFormat.getNombreFichero());
            docVer.setDatosFicheroFormateado(docRDSFormat.getDatosFichero());
            docVer.setExtensionFichero(docRDS.getExtensionFichero());
            docVer.setFechaRDS(docRDS.getFechaRDS());
            docVer.setFirmas(docRDS.getFirmas());
            return docVer;
        } catch (Exception ex) {
            throw new ExcepcionRDS("Error al verificar documento", ex);
        }
    }

    /**
     * Cambia de UA un documento
     * 
     * @ejb.interface-method
     * @ejb.permission role-name = "${role.user}"
     */
    public void cambiarUnidadAdministrativa(ReferenciaRDS refRDS, Long codUA) throws ExcepcionRDS {
        Session session = getSession();
        try {
            Documento documento;
            try {
                documento = (Documento) session.load(Documento.class, new Long(refRDS.getCodigo()));
            } catch (Exception e) {
                log.error("No existe documento " + refRDS.getCodigo());
                throw new ExcepcionRDS("No existe documento " + refRDS.getCodigo(), e);
            }
            if ("S".equals(documento.getBorrado())) throw new ExcepcionRDS("El documento " + documento.getCodigo() + " ha sido borrado por no tener usos");
            if (!documento.getClave().equals(refRDS.getClave())) {
                log.error("Clave de la referencia RDS no concuerda");
                throw new ExcepcionRDS("Clave de la referencia RDS no concuerda");
            }
            documento.setUnidadAdministrativa(codUA);
            session.update(documento);
        } catch (Exception ex) {
            throw new ExcepcionRDS("Error al cambiar UA del documento", ex);
        } finally {
            close(session);
        }
    }

    /**
     * Convierte un fichero a PDF/A. Debe tener una extensi�n permitida: "doc","docx","ppt","xls","odt","jpg","txt"
     * 
     *
     * 
     * @ejb.interface-method
     * @ejb.permission role-name = "${role.user}"
     */
    public byte[] convertirFicheroAPDF(byte[] documento, String extension) throws ExcepcionRDS {
        try {
            if (!verificarExtensionConversionPDF(extension)) {
                throw new Exception("No se permite la conversion a PDF para la extension " + extension.toLowerCase());
            }
            if (StringUtils.isBlank(OPENOFFICE_HOST) || StringUtils.isBlank(OPENOFFICE_PUERTO)) {
                throw new Exception("No se ha configurado los parametros de conexion al OpenOffice");
            }
            ConversorOpenOffice cof = new ConversorOpenOffice(OPENOFFICE_HOST, OPENOFFICE_PUERTO);
            byte[] documentoConvertido = cof.convertirFitxer(documento, extension.toLowerCase(), "pdf");
            return documentoConvertido;
        } catch (Exception ex) {
            throw new ExcepcionRDS("Error al convertir documento a PDF", ex);
        }
    }

    /**
     *  Guarda documento en el RDS
     */
    private ReferenciaRDS grabarDocumento(DocumentoRDS documento, boolean nuevo) throws ExcepcionRDS {
        Session session = getSession();
        Documento doc;
        try {
            if (nuevo) {
                doc = new Documento();
            } else {
                doc = (Documento) session.load(Documento.class, new Long(documento.getReferenciaRDS().getCodigo()));
                if ("S".equals(doc.getBorrado())) throw new ExcepcionRDS("El documento " + documento.getReferenciaRDS().getCodigo() + " ha sido borrado por no tener usos");
            }
            establecerCamposDocumento(doc, documento, nuevo);
            if (nuevo) {
                session.save(doc);
            } else {
                session.update(doc);
            }
            custodiarDocumento(documento, doc, session);
        } catch (HibernateException he) {
            log.error("Error insertando documento", he);
            throw new ExcepcionRDS("Error insertando documento", he);
        } catch (Exception e) {
            log.error("Error insertando documento", e);
            throw new ExcepcionRDS("Error insertando documento", e);
        } finally {
            close(session);
        }
        try {
            PluginAlmacenamientoRDS plugin = obtenerPluginAlmacenamiento(doc.getUbicacion().getPluginAlmacenamiento());
            plugin.guardarFichero(doc.getCodigo(), documento.getDatosFichero());
        } catch (Exception e) {
            log.error("No se ha podido guardar fichero en ubicaci�n " + documento.getCodigoUbicacion(), e);
            throw new ExcepcionRDS("Error al guardar fichero", e);
        }
        ReferenciaRDS ref = new ReferenciaRDS();
        ref.setCodigo(doc.getCodigo().longValue());
        ref.setClave(doc.getClave());
        return ref;
    }

    /**
     * Sincroniza con custodia los documentos
     * @throws HibernateException 
     */
    private void custodiarDocumento(DocumentoRDS documento, Documento doc, Session session) throws HibernateException, Exception {
        if (existeCustodia && "S".equals(doc.getVersion().getModelo().getCustodiar() + "")) {
            log.debug("Custodiando documento del tipo " + doc.getVersion().getModelo().getModelo() + " - " + doc.getVersion().getVersion());
            if (doc.getFirmas() != null && doc.getFirmas().size() > 0) {
                log.debug("Documento con firmas, insertamos en custodia");
                if (documento.getReferenciaRDS() == null) {
                    documento.setReferenciaRDS(new ReferenciaRDS(doc.getCodigo().longValue(), doc.getClave()));
                }
                PluginCustodiaIntf pluginCustodia = PluginFactory.getInstance().getPluginCustodia();
                String custodia = pluginCustodia.custodiarDocumento(documento);
                List custodiasDocumento = null;
                Query query;
                query = session.createQuery("FROM VersionCustodia AS version WHERE version.documento.codigo = :codigoDocumento and version.codigo != :idVersion");
                query.setParameter("codigoDocumento", doc.getCodigo());
                query.setParameter("idVersion", custodia);
                custodiasDocumento = query.list();
                if (custodiasDocumento != null) {
                    log.debug("Marcamos custodias anteriores para borrar");
                    for (int i = 0; i < custodiasDocumento.size(); i++) {
                        VersionCustodia cust = (VersionCustodia) custodiasDocumento.get(i);
                        cust.setBorrar('S');
                        cust.setFecha(new Date());
                        session.update(cust);
                    }
                }
                VersionCustodia custodiaDocumento = (VersionCustodia) session.get(VersionCustodia.class, custodia);
                log.debug("Custodiamos version actual");
                if (custodiaDocumento == null) {
                    custodiaDocumento = new VersionCustodia();
                    custodiaDocumento.setCodigo(custodia);
                    custodiaDocumento.setDocumento(doc);
                    custodiaDocumento.setFecha(new Date());
                    custodiaDocumento.setBorrar('N');
                    session.save(custodiaDocumento);
                } else {
                    custodiaDocumento.setDocumento(doc);
                    custodiaDocumento.setFecha(new Date());
                    custodiaDocumento.setBorrar('N');
                    session.update(custodiaDocumento);
                }
            } else {
                log.debug("Documento sin firmas, borramos de custodia");
                Query query = session.createQuery("FROM VersionCustodia AS version WHERE version.documento.codigo = :codigoDocumento and version.borrar = 'N'");
                query.setParameter("codigoDocumento", doc.getCodigo());
                List custodiasDocumento = query.list();
                if (custodiasDocumento != null) {
                    for (int i = 0; i < custodiasDocumento.size(); i++) {
                        VersionCustodia cust = (VersionCustodia) custodiasDocumento.get(i);
                        cust.setBorrar('S');
                        cust.setFecha(new Date());
                        session.update(cust);
                    }
                }
            }
        }
    }

    /**
     * Verifica que est�n los campos obligatorios
     */
    private void establecerCamposDocumento(Documento doc, DocumentoRDS documento, boolean nuevo) throws ExcepcionRDS {
        if (documento.getCodigoUbicacion() == null) throw new ExcepcionRDS("No se ha indicado c�digo de ubicaci�n");
        if (documento.getDatosFichero() == null || documento.getDatosFichero().length <= 0) throw new ExcepcionRDS("No se han establecido los datos del fichero");
        if (documento.getExtensionFichero() == null) throw new ExcepcionRDS("No se ha indicado la extensi�n del fichero");
        if (documento.getModelo() == null) throw new ExcepcionRDS("No se ha indicado el modelo del documento");
        if (documento.getNombreFichero() == null) throw new ExcepcionRDS("No se ha indicado el nombre del fichero");
        if (documento.getTitulo() == null) throw new ExcepcionRDS("No se ha indicado el t�tulo del documento");
        if (documento.getUnidadAdministrativa() == -1) throw new ExcepcionRDS("No se ha indicado la Unidad Administrativa responsable del documento");
        if (documento.getVersion() == -1) throw new ExcepcionRDS("No se ha indicado la versi�n del documento");
        Version version;
        try {
            VersionDelegate vd = DelegateUtil.getVersionDelegate();
            version = vd.obtenerVersion(documento.getModelo(), documento.getVersion());
            if (version == null) {
                log.error("No existe versi�n " + documento.getModelo() + " - " + documento.getVersion());
                throw new ExcepcionRDS("No existe modelo/version en RDS: " + documento.getModelo() + " / " + documento.getVersion());
            }
        } catch (Exception e) {
            log.error("No se ha podido obtener versi�n " + documento.getModelo() + " - " + documento.getVersion(), e);
            throw new ExcepcionRDS("No se ha podido obtener modelo / version en RDS", e);
        }
        Ubicacion ubicacion;
        try {
            UbicacionDelegate ud = DelegateUtil.getUbicacionDelegate();
            ubicacion = ud.obtenerUbicacion(documento.getCodigoUbicacion());
            if (ubicacion == null) {
                log.error("No existe ubicaci�n " + documento.getCodigoUbicacion());
                throw new ExcepcionRDS("No existe ubicaci�n en RDS");
            }
        } catch (Exception e) {
            log.error("No se ha podido obtener ubicaci�n " + documento.getCodigoUbicacion(), e);
            throw new ExcepcionRDS("No se ha podido obtener ubicaci�n en RDS", e);
        }
        if (StringUtils.isNotEmpty(documento.getPlantilla())) {
            try {
                Plantilla plantilla;
                PlantillaDelegate pl = DelegateUtil.getPlantillaDelegate();
                plantilla = pl.obtenerPlantilla(version, documento.getPlantilla());
                if (plantilla == null) {
                    log.error("No existe plantilla " + documento.getPlantilla() + " para modelo " + version.getModelo() + " - version " + version.getVersion());
                    throw new ExcepcionRDS("No existe plantilla " + documento.getPlantilla() + " para modelo " + version.getModelo() + " - version " + version.getVersion());
                }
                doc.setPlantilla(plantilla);
            } catch (Exception e) {
                log.error("No se ha podido obtener plantilla espec�fica " + documento.getPlantilla(), e);
                throw new ExcepcionRDS("No se ha podido obtener plantilla espec�fica ", e);
            }
        } else {
            doc.setPlantilla(null);
        }
        doc.setVersion(version);
        if (!nuevo) {
            if (ubicacion.getCodigo().longValue() != doc.getUbicacion().getCodigo().longValue()) {
                throw new ExcepcionRDS("No se permite cambiar de ubicaci�n al actualizar documento");
            }
        } else {
            doc.setUbicacion(ubicacion);
        }
        if (!nuevo) {
            if (documento.getReferenciaRDS() == null) throw new ExcepcionRDS("No se ha indicado la referencia RDS");
            if (!doc.getClave().equals(documento.getReferenciaRDS().getClave())) {
                throw new ExcepcionRDS("La clave no coincide");
            }
        }
        doc.setTitulo(documento.getTitulo());
        if (documento.getNif() != null) {
            doc.setNif(documento.getNif().replaceAll("-", ""));
        }
        doc.setUsuarioSeycon(documento.getUsuarioSeycon());
        doc.setUnidadAdministrativa(new Long(documento.getUnidadAdministrativa()));
        doc.setNombreFichero(documento.getNombreFichero());
        doc.setExtensionFichero(documento.getExtensionFichero());
        if (!nuevo) {
            doc.getFirmas().removeAll(doc.getFirmas());
        }
        if (documento.getFirmas() != null) {
            for (int i = 0; i < documento.getFirmas().length; i++) {
                Firma firma = new Firma();
                FirmaIntf signature = documento.getFirmas()[i];
                try {
                    firma.setFirma(getBytesFirma(signature));
                    firma.setFormato(signature.getFormatoFirma());
                } catch (Exception exc) {
                    throw new ExcepcionRDS("Error obteniendo bytes de la firma ", exc);
                }
                if (!this.verificarFirma(documento.getDatosFichero(), signature)) {
                    throw new ExcepcionRDS("Error al verificar la firma del documento");
                }
                doc.addFirma(firma);
            }
        }
        doc.setFecha(new Timestamp(System.currentTimeMillis()));
        try {
            doc.setHashFichero(generaHash(documento.getDatosFichero()));
        } catch (Exception e) {
            log.error("No se ha podido calcular el hash", e);
            throw new ExcepcionRDS("No se ha podido calcular el hash", e);
        }
        if (nuevo) doc.setClave(generarClave());
    }

    /**
     * Obtiene plugin almacenamiento
     * @param classNamePlugin
     * @return
     */
    private PluginAlmacenamientoRDS obtenerPluginAlmacenamiento(String classNamePlugin) throws Exception {
        return PluginClassCache.getInstance().getPluginAlmacenamientoRDS(classNamePlugin);
    }

    /**
     * Mapea Documento a DocumentoRDS
     * @param doc
     * @return
     * @throws ExcepcionRDS
     */
    private DocumentoRDS establecerCamposDocumentoRDS(Documento doc) throws ExcepcionRDS {
        DocumentoRDS documentoRDS = new DocumentoRDS();
        ReferenciaRDS ref = new ReferenciaRDS();
        ref.setCodigo(doc.getCodigo().longValue());
        ref.setClave(doc.getClave());
        documentoRDS.setReferenciaRDS(ref);
        documentoRDS.setCodigoUbicacion(doc.getUbicacion().getCodigoUbicacion());
        documentoRDS.setEstructurado(doc.getVersion().getModelo().getEstructurado() == 'S');
        documentoRDS.setExtensionFichero(doc.getExtensionFichero());
        documentoRDS.setFechaRDS(doc.getFecha());
        documentoRDS.setHashFichero(doc.getHashFichero());
        documentoRDS.setModelo(doc.getVersion().getModelo().getModelo());
        documentoRDS.setVersion(doc.getVersion().getVersion());
        documentoRDS.setNif(doc.getNif());
        documentoRDS.setUsuarioSeycon(doc.getUsuarioSeycon());
        documentoRDS.setTitulo(doc.getTitulo());
        documentoRDS.setNombreFichero(doc.getNombreFichero());
        documentoRDS.setUnidadAdministrativa(doc.getUnidadAdministrativa().longValue());
        if (doc.getPlantilla() != null) {
            documentoRDS.setPlantilla(doc.getPlantilla().getTipo());
        }
        if (doc.getFirmas().size() > 0) {
            int i = 0;
            FirmaIntf ls_firmas[] = new FirmaIntf[doc.getFirmas().size()];
            for (Iterator it = doc.getFirmas().iterator(); it.hasNext(); ) {
                try {
                    Firma f = (Firma) it.next();
                    ls_firmas[i] = this.getFirma(f.getFirma(), f.getFormato());
                    i++;
                } catch (Exception exc) {
                    throw new ExcepcionRDS("Error obteniendo string de la firma ", exc);
                }
            }
            documentoRDS.setFirmas(ls_firmas);
        }
        return documentoRDS;
    }

    private void doLogOperacion(String idAplicacion, String idTipoOperacion, String mensaje) throws ExcepcionRDS {
        Session session = getSession();
        try {
            doLogOperacionImpl(idAplicacion, idTipoOperacion, mensaje, session);
        } catch (Exception e) {
            throw new ExcepcionRDS("No se ha podido grabar en log", e);
        } finally {
            close(session);
        }
    }

    private void doLogOperacionImpl(String idUsuario, String idTipoOperacion, String mensaje, Session session) throws HibernateException {
        TipoOperacion tipoOperacion = (TipoOperacion) session.load(TipoOperacion.class, idTipoOperacion);
        LogOperacion log = new LogOperacion();
        log.setUsuarioSeycon(idUsuario);
        log.setTipoOperacion(tipoOperacion);
        log.setDescripcionOperacion(mensaje);
        log.setFecha(new Timestamp(System.currentTimeMillis()));
        session.save(log);
    }

    /**
     * Obtiene usuario autenticado
     * @return
     */
    private String getUsuario() {
        if (this.ctx.getCallerPrincipal() != null) return this.ctx.getCallerPrincipal().getName(); else return "";
    }

    private void documentoSinUsos(Session session, Documento documento) throws Exception {
        String ls_plugin, ls_ubicacion;
        Query query = session.createQuery("FROM Uso AS u WHERE u.documento = :documento");
        query.setParameter("documento", documento);
        List result = query.list();
        if (result.isEmpty()) {
            ls_plugin = documento.getUbicacion().getPluginAlmacenamiento();
            ls_ubicacion = documento.getUbicacion().getCodigoUbicacion();
            session.delete(documento);
            try {
                PluginAlmacenamientoRDS plugin = obtenerPluginAlmacenamiento(ls_plugin);
                plugin.eliminarFichero(documento.getCodigo());
            } catch (Exception e) {
                log.error("No se ha podido eliminar fichero en ubicaci�n " + ls_ubicacion, e);
                throw new ExcepcionRDS("Error al eliminar fichero", e);
            }
            doLogOperacionImpl(getUsuario(), BORRADO_AUTOMATICO_DOCUMENTO_SIN_USOS, "Borrado autom�tico de documento " + documento.getCodigo() + " por no tener usos", session);
        }
    }

    /**
     * Genera clave de acceso al documento (Cadena de 10 car�cteres)
     * @return
     */
    private String generarClave() {
        Random r = new Random();
        String clave = "";
        int ca = Character.getNumericValue('a');
        for (int i = 0; i < 10; i++) {
            clave += Character.forDigit(ca + r.nextInt(26), Character.MAX_RADIX);
        }
        return clave;
    }

    /**
     * Genera hash documento
     */
    private String generaHash(byte[] datos) throws Exception {
        MessageDigest dig = MessageDigest.getInstance(ConstantesRDS.HASH_ALGORITMO);
        return new String(Hex.encodeHex(dig.digest(datos)));
    }

    private byte[] getBytesFirma(FirmaIntf firma) throws Exception {
        PluginFirmaIntf plgFirma = PluginFactory.getInstance().getPluginFirma();
        return plgFirma.parseFirmaToBytes(firma);
    }

    private FirmaIntf getFirma(byte[] byteArrayFirma, String formatoFirma) throws Exception {
        PluginFirmaIntf plgFirma = PluginFactory.getInstance().getPluginFirma();
        return plgFirma.parseFirmaFromBytes(byteArrayFirma, formatoFirma);
    }

    /**
     * Verifica firma del documento (completa timestamp si es necesario)
     * 
     * @param datosDocumento
     * @param firma
     * @return
     */
    private boolean verificarFirma(byte[] datosDocumento, FirmaIntf firma) {
        try {
            PluginFirmaIntf plgFirma = PluginFactory.getInstance().getPluginFirma();
            return plgFirma.verificarFirma(new ByteArrayInputStream(datosDocumento), firma);
        } catch (Exception e) {
            log.error("Error al verificar firma: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Realiza stamp 
     * @param doc
     * @throws Exception
     */
    private void stampBarCodeVerifier(DocumentoRDS doc, String plantilla, String idioma) throws Exception {
        KeyVerifier key = new KeyVerifier(doc.getReferenciaRDS(), plantilla, idioma);
        String url = URL_VERIFIER;
        String text = TEXT_VERIFIER;
        String urlVerificacion = url + key.getKeyEncoded();
        doc.setUrlVerificacion(urlVerificacion);
        ObjectStamp stamps[] = new ObjectStamp[3];
        BarcodeStamp bc = new BarcodeStamp();
        bc.setTexto(urlVerificacion);
        bc.setTipo(BarcodeStamp.BARCODE_PDF417);
        bc.setPage(0);
        bc.setX(350);
        bc.setY(19);
        bc.setRotation(0);
        bc.setOverContent(true);
        bc.setXScale(new Float(100));
        bc.setYScale(new Float(100));
        stamps[0] = bc;
        TextoStamp tx = new TextoStamp();
        tx.setTexto(bc.getTexto());
        tx.setFontName("Helvetica-Bold");
        tx.setFontSize(7);
        tx.setX(290);
        tx.setY(13);
        stamps[1] = tx;
        TextoStamp tx2 = new TextoStamp();
        tx2.setTexto(text);
        tx2.setFontSize(6);
        tx2.setX(330);
        tx2.setY(44);
        stamps[2] = tx2;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        UtilPDF.stamp(bos, new ByteArrayInputStream(doc.getDatosFichero()), stamps);
        doc.setDatosFichero(bos.toByteArray());
        bos.close();
    }

    /**
     * Realiza stamp para indicar que es un borrador
     * @param doc
     * @throws Exception
     */
    private void stampBorrador(DocumentoRDS doc) throws Exception {
        ObjectStamp[] textos = new ObjectStamp[1];
        textos[0] = new TextoStamp();
        ((TextoStamp) textos[0]).setTexto("Sense validesa");
        ((TextoStamp) textos[0]).setFontSize(85);
        ((TextoStamp) textos[0]).setFontColor(Color.LIGHT_GRAY);
        textos[0].setPage(0);
        textos[0].setX(100);
        textos[0].setY(300);
        textos[0].setRotation(45f);
        textos[0].setOverContent(false);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        UtilPDF.stamp(bos, new ByteArrayInputStream(doc.getDatosFichero()), textos);
        doc.setDatosFichero(bos.toByteArray());
        bos.close();
    }

    /**
     * Realiza stamp de sello (Toma en cuenta tambien registro salida):
     * 		- No hay uso prereg/reg o preenv/env: marcamos doc como no v�lido
     * 		- Hay uso prereg/preenv: introducimos espacio para sello en 1� pag y en todas el num de prereg + dc
     * 		- Hay uso reg/env: en todas las pag el num de reg/env
     * @param docFormateado
     * @param usos
     * 
     * @return devuelve si se ha puesto el sello
     */
    private boolean stampSello(DocumentoRDS doc, List usos) throws Exception {
        ObjectStamp textos[];
        UsoRDS usoSello;
        usoSello = UtilRDS.obtenerNumeroEntrada(usos);
        if (usoSello == null) {
            usoSello = UtilRDS.obtenerNumeroSalida(usos);
        }
        if (usoSello == null) return false;
        String txtNumRegistro, txtDC, txtData;
        int numText = 1;
        if (usoSello.getReferencia().startsWith("PRE")) {
            txtNumRegistro = "Num. Preregistre: " + usoSello.getReferencia();
            txtDC = "D�git Control: " + StringUtil.calculaDC(usoSello.getReferencia());
            txtData = "Data Preregistre: ";
            numText++;
        } else {
            txtNumRegistro = "Num. Registre: " + usoSello.getReferencia();
            txtData = "Data Registre: ";
            txtDC = "";
        }
        if (usoSello.getFechaSello() != null) {
            numText++;
        }
        textos = new ObjectStamp[numText];
        textos[0] = new TextoStamp();
        ((TextoStamp) textos[0]).setTexto(txtNumRegistro + "  " + txtDC);
        textos[0].setPage(0);
        textos[0].setX(340);
        textos[0].setY(815);
        textos[0].setOverContent(true);
        if (usoSello.getFechaSello() != null) {
            numText--;
            textos[numText] = new TextoStamp();
            ((TextoStamp) textos[numText]).setTexto(txtData + StringUtil.fechaACadena(usoSello.getFechaSello(), "dd/MM/yyyy HH:mm"));
            textos[numText].setPage(0);
            textos[numText].setX(340);
            textos[numText].setY(805);
            textos[numText].setOverContent(true);
        }
        if (txtDC.length() > 0) {
            numText--;
            textos[numText] = new SelloEntradaStamp();
            textos[numText].setPage(1);
            textos[numText].setX(565);
            textos[numText].setY(802);
            textos[numText].setOverContent(true);
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        UtilPDF.stamp(bos, new ByteArrayInputStream(doc.getDatosFichero()), textos);
        doc.setDatosFichero(bos.toByteArray());
        bos.close();
        return true;
    }

    private boolean isBorrador() {
        try {
            String text = ENTORNO;
            if (text != null && text.equals("PRODUCCION")) return false;
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private byte[] generarCopiasInteresadoAdministracion(byte[] pdf) throws Exception {
        ObjectStamp[] textos = new ObjectStamp[1];
        textos[0] = new TextoStamp();
        ((TextoStamp) textos[0]).setFontSize(7);
        ((TextoStamp) textos[0]).setFontColor(Color.LIGHT_GRAY);
        textos[0].setPage(0);
        textos[0].setX(200);
        textos[0].setY(20);
        textos[0].setOverContent(false);
        ((TextoStamp) textos[0]).setTexto("Exemplar per a l'Administraci�");
        ByteArrayInputStream bis = new ByteArrayInputStream(pdf);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        UtilPDF.stamp(bos, bis, textos);
        byte[] contentPDF1 = bos.toByteArray();
        bis.close();
        bos.close();
        ((TextoStamp) textos[0]).setTexto("Exemplar per al sol�licitant");
        bis = new ByteArrayInputStream(pdf);
        bos = new ByteArrayOutputStream();
        UtilPDF.stamp(bos, bis, textos);
        byte[] contentPDF2 = bos.toByteArray();
        bis.close();
        bos.close();
        bos = new ByteArrayOutputStream();
        InputStream pdfs[] = { new ByteArrayInputStream(contentPDF1), new ByteArrayInputStream(contentPDF2) };
        UtilPDF.concatenarPdf(bos, pdfs);
        byte[] content = bos.toByteArray();
        pdfs[0].close();
        pdfs[1].close();
        bos.close();
        return content;
    }

    /**
     * Verifica si la extension puede convertirse a PDF
     * @param extension
     * @return
     */
    private boolean verificarExtensionConversionPDF(String extension) {
        String[] extensiones = { "doc", "docx", "ppt", "xls", "odt", "jpg", "txt" };
        for (int i = 0; i < extensiones.length; i++) {
            if (extension.toLowerCase().equals(extensiones[i])) {
                return true;
            }
        }
        return false;
    }
}
