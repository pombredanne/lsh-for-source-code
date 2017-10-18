package sisi.movimenti;

import java.io.*;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Date;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.zkoss.zk.ui.Components;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.SuspendNotAllowedException;
import org.zkoss.zk.ui.event.ClientInfoEvent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.InputEvent;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listhead;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Separator;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Window;
import org.zkoss.zul.Paging;
import org.zkoss.zul.event.PagingEvent;
import net.sf.jasperreports.engine.JasperRunManager;
import sisi.EsportaExcel;
import sisi.General;
import sisi.nominativi.Nominativi;
import sisi.nominativi.NominativiController;
import sisi.operazioni.*;
import sisi.punti.StoricoPunti;
import sisi.punti.StoricoPuntiController;
import sisi.tipimov.Tipimov;
import sisi.users.CfgUtenti;
import sisi.users.CfgUtentiController;

@SuppressWarnings("serial")
public class MovimentiWindow extends Window implements org.zkoss.zk.ui.ext.AfterCompose {

    private Window finestra;

    private Textbox cercaMov;

    private Listbox boxMovimenti;

    private Listhead headMovimenti;

    @SuppressWarnings("unchecked")
    public List tutti_i_movimenti;

    private Object MovItem;

    private ListModelList listModelList;

    private final String PAG_MOVIMENTI = "pagMovimenti";

    private int quantitaMovimenti = 0;

    private String nomeUtente;

    private String cerca = "";

    private boolean lPrimaVolta = true, lPulsanteAltriCampi = true;

    private String theme = "";

    private Toolbarbutton buttonAltriCampi, buttonNuovo, buttonModifica, buttonCancella, buttonStampa, btnStampa2, buttonExcel, buttonDuplica;

    private Vbox vbox1;

    private Separator sep1;

    private Datebox dataDal, dataAl;

    private List<?> dirittiUtente;

    public void afterCompose() {
        theme = General.getTheme();
        Components.wireVariables(this, this);
        nomeUtente = (String) Executions.getCurrent().getDesktop().getSession().getAttribute("User");
        dirittiUtente = new CfgUtentiController().getListDirittiUtente(nomeUtente);
        if (dirittiUtente.contains("010101")) {
            buttonNuovo.setDisabled(true);
        }
        if (dirittiUtente.contains("010102")) {
            buttonModifica.setDisabled(true);
        }
        if (dirittiUtente.contains("010103")) {
            buttonCancella.setDisabled(true);
        }
        if (dirittiUtente.contains("010104")) {
            buttonStampa.setDisabled(true);
        }
        if (dirittiUtente.contains("010105")) {
            btnStampa2.setDisabled(true);
        }
        if (dirittiUtente.contains("010106")) {
            buttonExcel.setDisabled(true);
        }
        if (dirittiUtente.contains("010107")) {
            buttonDuplica.setDisabled(true);
        }
        finestra.setTitle("Movimenti");
        listModelList = new ListModelList();
        quantitaMovimenti = new MovtestaController().getCountMovtesta(null);
        Paging pag = (Paging) getFellow(PAG_MOVIMENTI);
        pag.setTotalSize(quantitaMovimenti);
        CfgUtenti cfgUtente = new CfgUtentiController().getCfgUtente(nomeUtente, "MOVIMENTI", false);
        if (cfgUtente.getUtente() != null && !cfgUtente.getUtente().isEmpty()) {
            new General().impostoLunghezzaColonne((Listbox) headMovimenti.getListbox(), cfgUtente.getColonne());
        }
        Components.addForwards(this, this);
        pag.addEventListener("onPaging", new EventListener() {

            public void onEvent(Event event) {
                PagingEvent pe = (PagingEvent) event;
                Paging pag = (Paging) getFellow(PAG_MOVIMENTI);
                final int PAGE_SIZE = pag.getPageSize();
                int pgno = pe.getActivePage();
                int ofs = pgno * PAGE_SIZE;
                redraw(ofs, PAGE_SIZE);
            }
        });
        finestra.addEventListener("onClientInfo", new EventListener() {

            public void onEvent(Event event) {
                final ClientInfoEvent evt = (ClientInfoEvent) event;
                int height = evt.getDesktopHeight();
                int pageSize;
                if (theme.equalsIgnoreCase("classicblue")) {
                    pageSize = Math.max((height - 160) / 17, 10);
                } else {
                    pageSize = Math.max((height - 160) / 28, 10);
                }
                boxMovimenti.setRows(pageSize);
                Paging pag = (Paging) getFellow(PAG_MOVIMENTI);
                pag.setPageSize(pageSize);
                int pgno = pag.getActivePage();
                int ofs = pgno * pageSize;
                redraw(ofs, pageSize);
            }
        });
        finestra.addEventListener("onMaximize", new EventListener() {

            public void onEvent(Event event) {
                int heightFin = Integer.valueOf(finestra.getHeight().replace("px", "")).intValue();
                int pageSize;
                if (theme.equalsIgnoreCase("classicblue")) {
                    pageSize = Math.max((heightFin - 160) / 17, 10);
                } else {
                    pageSize = Math.max((heightFin - 160) / 28, 10);
                }
                boxMovimenti.setRows(pageSize);
                Paging pag = (Paging) getFellow(PAG_MOVIMENTI);
                pag.setPageSize(pageSize);
                int pgno = pag.getActivePage();
                int ofs = pgno * pageSize;
                redraw(ofs, pageSize);
            }
        });
        if (quantitaMovimenti < 1000) {
            cercaMov.addEventListener("onChanging", new EventListener() {

                public void onEvent(Event event) {
                    final InputEvent evt = (InputEvent) event;
                    String valore = evt.getValue();
                    cercaMov(valore);
                    cercaMov.setFocus(true);
                }
            });
        } else {
            cercaMov.addEventListener("onOK", new EventListener() {

                public void onEvent(Event event) {
                    String valore = cercaMov.getValue();
                    cercaMov(valore);
                    cercaMov.setFocus(true);
                }
            });
        }
        headMovimenti.addEventListener("onColSize", new EventListener() {

            public void onEvent(Event event) {
                new General().utenteSalvaLunghezzaColonne((Listbox) headMovimenti.getListbox(), nomeUtente, "MOVIMENTI");
            }
        });
        cercaMov.setTooltiptext("Inserire il testo da cercare:" + '\r' + "Tipo movimento (codice o descrizione)" + '\r' + "Numero documento" + '\r' + "Nominativo");
    }

    @SuppressWarnings("unchecked")
    private void redraw(int firstResult, int maxResults) {
        Listbox lst = boxMovimenti;
        lst.getItems().clear();
        int quanti = Math.min(quantitaMovimenti, maxResults);
        List<Movtesta> tutti_i_movimenti = new MovtestaController().getListMovtesta(cerca, dataDal.getValue(), dataAl.getValue(), firstResult, quanti);
        listModelList = new ListModelList();
        listModelList.addAll(tutti_i_movimenti);
        boxMovimenti.setModel(listModelList);
        boxMovimenti.setItemRenderer(new RenderBrowseMovimenti());
        if (lPrimaVolta) {
            lPrimaVolta = false;
            Paging pag = (Paging) getFellow(PAG_MOVIMENTI);
            int nPag = pag.getPageCount();
            pag.setActivePage(nPag - 1);
            final int PAGE_SIZE = pag.getPageSize();
            int pgno = nPag - 1;
            int ofs = pgno * PAGE_SIZE;
            redraw(ofs, PAGE_SIZE);
            if (boxMovimenti.getItemCount() > 0) {
                boxMovimenti.setSelectedIndex(boxMovimenti.getItemCount() - 1);
            }
        }
    }

    public void onClick$buttonCercaMov(Event event) {
        cercaMov(cercaMov.getValue());
    }

    void cercaMov(String txt) {
        if (txt == null) {
            cerca = cercaMov.getValue().trim();
        } else {
            cerca = txt.trim();
        }
        if (cerca.length() > 0 && cerca.length() < 3) {
            return;
        }
        String msgFinestra = "Movimenti";
        if (cerca.isEmpty()) {
        } else {
            msgFinestra += " - filtro ricerca: " + cerca;
        }
        if (dataDal.getValue() != null || dataAl.getValue() != null) {
            msgFinestra += " - Data documento";
        }
        if (dataDal.getValue() != null) {
            msgFinestra += " dal " + sisi.General.formatoFecha(dataDal.getValue());
        }
        if (dataAl.getValue() != null) {
            msgFinestra += " al " + sisi.General.formatoFecha(dataAl.getValue());
        }
        finestra.setTitle(msgFinestra);
        this.boxMovimenti.clearSelection();
        quantitaMovimenti = new MovtestaController().getCountMovtesta(cerca, dataDal.getValue(), dataAl.getValue());
        Paging pag = (Paging) getFellow(PAG_MOVIMENTI);
        pag.setTotalSize(quantitaMovimenti);
        final int PAGE_SIZE = pag.getPageSize();
        redraw(0, PAGE_SIZE);
    }

    public void onChange$dataDal(Event event) {
        cercaMov(null);
    }

    public void onChange$dataAl(Event event) {
        cercaMov(null);
    }

    public void onClick$buttonAltriCampi(Event event) {
        altriCampi();
    }

    private void altriCampi() {
        if (lPulsanteAltriCampi) {
            buttonAltriCampi.setLabel("Nascondi altri campi");
            vbox1.setVisible(true);
        } else {
            buttonAltriCampi.setLabel("Altri campi di ricerca");
            vbox1.setVisible(false);
        }
        sep1.setVisible(vbox1.isVisible());
        lPulsanteAltriCampi = !lPulsanteAltriCampi;
    }

    public void onClick$buttonStampa(Event event) throws InterruptedException {
        nuovaStampaDocumento();
    }

    @SuppressWarnings("unchecked")
    private void nuovaStampaDocumento() {
        int nIndex = boxMovimenti.getSelectedIndex();
        if (nIndex == -1) {
            new General().MsgBox("Selezionare un Documento da Stampare", "Information");
        } else {
            MovItem = boxMovimenti.getSelectedItem().getAttribute("rigaTestaMovimento");
            String codTipMov = ((Movtesta) MovItem).getCodtipmov();
            sisi.tipimov.Tipimov cTipoMov = new sisi.tipimov.TipimovController().getTipimovXCodice(codTipMov);
            boolean lStampa = (cTipoMov.getTmstampa() != null && cTipoMov.getTmstampa().equalsIgnoreCase("S"));
            String fincatoStampa = cTipoMov.getTmfincato();
            if (fincatoStampa == null || fincatoStampa.isEmpty() || !lStampa) {
                new General().MsgBox("Documento senza formato o non stampabile", "Errore");
                return;
            }
            String cTipmov = cTipoMov.getTmtipmov();
            if ("1237".contains(cTipmov) && "23".contains(cTipoMov.getTmcontro())) {
            } else {
                try {
                    Messagebox.show("Stampa non ancora implementata per questo tipo di documento", "Information", Messagebox.OK, Messagebox.INFORMATION);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return;
            }
            HashMap map = new HashMap();
            map.put("codTipoMovimento", codTipMov);
            map.put("boxMovimenti", boxMovimenti);
            Window finestraSceltaFormato = (Window) Executions.createComponents("/scegli_formato.zul", null, map);
            try {
                finestraSceltaFormato.doModal();
            } catch (SuspendNotAllowedException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void stampaDocumento() {
        try {
            int nIndex = boxMovimenti.getSelectedIndex();
            if (nIndex == -1) {
                Messagebox.show("Selezionare un Documento da Stampare", "Information", Messagebox.OK, Messagebox.INFORMATION);
            } else {
                MovItem = boxMovimenti.getSelectedItem().getAttribute("rigaTestaMovimento");
                String codTipMov = ((Movtesta) MovItem).getCodtipmov();
                sisi.tipimov.Tipimov cTipoMov = new sisi.tipimov.TipimovController().getTipimovXCodice(codTipMov);
                String cTipmov = cTipoMov.getTmtipmov();
                boolean lStampa = (cTipoMov.getTmstampa() != null && cTipoMov.getTmstampa().equalsIgnoreCase("S"));
                String fincatoStampa = cTipoMov.getTmfincato();
                if (fincatoStampa == null || fincatoStampa.isEmpty() || !lStampa) {
                    new General().MsgBox("Documento senza formato o non stampabile", "Errore");
                    return;
                }
                if (!new General().esisteReport(fincatoStampa)) {
                    new General().MsgBox("Formato inesistente: " + fincatoStampa.trim(), "Errore");
                    return;
                }
                if ("1237".contains(cTipmov) && "23".contains(cTipoMov.getTmcontro())) {
                    (Sessions.getCurrent()).setAttribute("idDocStampa", "" + ((Movtesta) MovItem).getTid());
                    sisi.movimenti.MovimentiDS ds = new sisi.movimenti.MovimentiDS();
                    String nomFile = new sisi.General().percorsoFincati() + "/" + fincatoStampa.trim() + ".jasper";
                    (Sessions.getCurrent()).setAttribute("nomefile", nomFile);
                    (Sessions.getCurrent()).setAttribute("datasource", ds);
                    (Sessions.getCurrent()).setAttribute("tipostampa", "pdf");
                    Executions.getCurrent().sendRedirect("/stampaReport.zul", "_blank");
                } else {
                    Messagebox.show("Stampa non ancora implementata per questo tipo di documento", "Information", Messagebox.OK, Messagebox.INFORMATION);
                }
            }
        } catch (SuspendNotAllowedException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public void onClick$buttonPeriodico(Event event) {
        try {
            int nIndex = boxMovimenti.getSelectedIndex();
            if (nIndex == -1) {
                Messagebox.show("Selezionare un Documento", "Information", Messagebox.OK, Messagebox.INFORMATION);
            } else {
                MovItem = boxMovimenti.getSelectedItem().getAttribute("rigaTestaMovimento");
                String codTipMov = ((Movtesta) MovItem).getCodtipmov();
                sisi.tipimov.Tipimov cTipoMov = new sisi.tipimov.TipimovController().getTipimovXCodice(codTipMov);
                String cTipmov = cTipoMov.getTmtipmov();
                if ("1237".contains(cTipmov) && "2".contains(cTipoMov.getTmcontro())) {
                    HashMap map = new HashMap();
                    map.put("id", "" + ((Movtesta) MovItem).getTid());
                    Window finestra4 = (Window) Executions.createComponents("/fatturePeriodiche.zul", null, map);
                    finestra4.doModal();
                } else {
                    Messagebox.show("Procedura non ancora implementata per questo tipo di documento", "Information", Messagebox.OK, Messagebox.INFORMATION);
                }
            }
        } catch (SuspendNotAllowedException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public void onClick$buttonDuplica(Event event) {
        try {
            int nIndex = boxMovimenti.getSelectedIndex();
            if (nIndex == -1) {
                Messagebox.show("Selezionare un Documento", "Information", Messagebox.OK, Messagebox.INFORMATION);
            } else {
                MovItem = boxMovimenti.getSelectedItem().getAttribute("rigaTestaMovimento");
                String codTipMov = ((Movtesta) MovItem).getCodtipmov();
                sisi.tipimov.Tipimov cTipoMov = new sisi.tipimov.TipimovController().getTipimovXCodice(codTipMov);
                String cTipmov = cTipoMov.getTmtipmov();
                if ("1237".contains(cTipmov) && "2".contains(cTipoMov.getTmcontro())) {
                    HashMap map = new HashMap();
                    map.put("id", "" + ((Movtesta) MovItem).getTid());
                    map.put("boxMovimenti", boxMovimenti);
                    Window finestra4 = (Window) Executions.createComponents("/duplicaDocs.zul", null, map);
                    finestra4.doModal();
                } else {
                    Messagebox.show("Procedura non ancora implementata per questo tipo di documento", "Information", Messagebox.OK, Messagebox.INFORMATION);
                }
            }
        } catch (SuspendNotAllowedException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public void onClick$buttonExcel(Event event) throws IOException {
        EsportaExcel oExcel = new sisi.EsportaExcel();
        oExcel.create("SISI");
        String[] intestazioni = { "Movimento", "Numero", "Data Doc.", "Importo", "Nominativo" };
        int[] lunghezzaColonne = { 12000, 3000, 3000, 3000, 12000 };
        oExcel.intestazione(intestazioni);
        int quanti = quantitaMovimenti;
        int quantiXPagina = 20;
        int pagine = (quanti / quantiXPagina) + 1;
        for (int i = 0; i < pagine; i++) {
            quantiXPagina = Math.min(quantiXPagina, quantitaMovimenti);
            int inizio = i * quantiXPagina;
            int fine = quantiXPagina;
            if (i == pagine - 1) {
                int ultimaPagina = quantitaMovimenti - ((pagine - 1) * quantiXPagina);
                quantiXPagina = Math.min(quantiXPagina, ultimaPagina);
                fine = quantiXPagina;
            }
            List lista = new MovtestaController().getListMovtesta(cercaMov.getValue(), dataDal.getValue(), dataAl.getValue(), inizio, fine);
            for (Object object : lista) {
                Movtesta mt = (Movtesta) object;
                String numdoc = mt.getTndoc();
                Date datadoc = mt.getTddoc();
                numdoc = (numdoc == null ? "" : numdoc);
                BigDecimal importo = mt.getTimpon();
                BigDecimal iva = mt.getTiva();
                importo = (importo == null ? BigDecimal.ZERO : importo);
                iva = (iva == null ? BigDecimal.ZERO : iva);
                importo = importo.add(iva);
                Tipimov tm = mt.getTipomovimento();
                String descrizioneMovimento = "";
                if (tm != null) {
                    descrizioneMovimento = tm.getTmdes();
                }
                Nominativi nom = mt.getNominativi();
                String descrizioneNominativo = "";
                if (nom != null) {
                    descrizioneNominativo = nom.getNomnom();
                }
                if (mt.getCodnom() != null && !mt.getCodnom().isEmpty()) {
                    descrizioneNominativo = mt.getCodnom() + " - " + descrizioneNominativo;
                }
                Object[] dettaglio = { mt.getCodtipmov() + descrizioneMovimento, numdoc, datadoc, importo, descrizioneNominativo };
                oExcel.dettaglio(dettaglio);
            }
        }
        oExcel.lenghtCols(lunghezzaColonne);
        String nomeFile = new sisi.General().percorsoFincati() + "/" + "movimenti.xls";
        oExcel.saveFile(nomeFile);
    }

    public void onDoubleClicked(Event event) throws Exception {
        modificaRiga();
    }

    public void onClick$buttonModifica(Event event) throws InterruptedException {
        modificaRiga();
    }

    public void onOK$boxMovimenti() throws InterruptedException {
        modificaRiga();
    }

    @SuppressWarnings("unchecked")
    private void modificaRiga() throws InterruptedException {
        if (dirittiUtente.contains("010102")) {
            return;
        }
        try {
            int nIndex = boxMovimenti.getSelectedIndex();
            if (nIndex == -1) {
                Messagebox.show("Selezionare una riga da Modificare", "Information", Messagebox.OK, Messagebox.INFORMATION);
            } else {
                MovItem = boxMovimenti.getSelectedItem().getAttribute("rigaTestaMovimento");
                Movtesta editMov2 = new MovtestaController().refreshMovtesta((Movtesta) MovItem);
                MovItem = editMov2;
                if (editMov2 == null) {
                    new General().errorBox("Il movimento risulta cancellato", "ERRORE");
                    return;
                }
                Collection corpo = ((Movtesta) MovItem).getCorpomovimento();
                String codTipMov = ((Movtesta) MovItem).getCodtipmov();
                sisi.tipimov.Tipimov cTipoMov = new sisi.tipimov.TipimovController().getTipimovXCodice(codTipMov);
                if (cTipoMov.getCodtipmov().isEmpty()) {
                    new General().MsgBox("Manca il tipo di movimento", "ERRORE");
                    return;
                }
                String cTipmov = cTipoMov.getTmtipmov();
                if ("12347".contains(cTipmov)) {
                    HashMap map = new HashMap();
                    map.put("id", "" + ((Movtesta) MovItem).getTid());
                    map.put("boxMovimenti", boxMovimenti);
                    map.put("coleccion", corpo);
                    Window finestra4 = (Window) Executions.createComponents("/editMovimenti.zul", null, map);
                    finestra4.doModal();
                } else {
                    HashMap map = new HashMap();
                    map.put("id", "" + ((Movtesta) MovItem).getTid());
                    map.put("boxMovimenti", boxMovimenti);
                    List riferimenti = ((Movtesta) MovItem).getRiferimentimovimento();
                    map.put("coleccion", riferimenti);
                    Window finestra4 = (Window) Executions.createComponents("/editPagamenti.zul", null, map);
                    finestra4.doModal();
                }
            }
        } catch (SuspendNotAllowedException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public void onClick$buttonNuovo(Event event) {
        try {
            HashMap map = new HashMap();
            map.put("boxMovimenti", boxMovimenti);
            Window finestra4 = (Window) Executions.createComponents("/nuovoMovimento.zul", null, map);
            finestra4.doModal();
        } catch (SuspendNotAllowedException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void onClick$buttonCancella(Event event) throws InterruptedException {
        int nIndex = boxMovimenti.getSelectedIndex();
        if (nIndex != -1) {
            if (boxMovimenti.getSelectedIndex() >= 0) {
                MovItem = boxMovimenti.getSelectedItem().getAttribute("rigaTestaMovimento");
                Movtesta editMov2 = new MovtestaController().refreshMovtesta((Movtesta) MovItem);
                if (editMov2 == null) {
                    new General().errorBox("Il movimento risulta gi� cancellato", "ERRORE");
                    return;
                }
                MovItem = editMov2;
                if (Messagebox.show("Conferma Cancellazione Movimento: " + ((Movtesta) MovItem).getCodtipmov() + " " + (((Movtesta) MovItem).getTndoc() != null ? ((Movtesta) MovItem).getTndoc() : ((Movtesta) MovItem).getNummov()) + "?", "Cancella Movimento", Messagebox.YES | Messagebox.NO, Messagebox.QUESTION) == Messagebox.YES) {
                    new MovtestaController().removeMovtesta((Movtesta) MovItem);
                    new OperazioniController().nuovaOperazione("Cancella Movimento: " + ((Movtesta) MovItem).getCodtipmov() + "-" + (((Movtesta) MovItem).getTndoc() != null ? ((Movtesta) MovItem).getTndoc() : ((Movtesta) MovItem).getNummov()), "movimenti");
                    boxMovimenti.removeItemAt(nIndex);
                    if (((Movtesta) MovItem).getCodnom() != null && !((Movtesta) MovItem).getCodnom().isEmpty()) {
                        StoricoPunti storicoPunti = new StoricoPuntiController().getStoricoPunti(((Movtesta) MovItem).getCodtipmov(), ((Movtesta) MovItem).getNummov());
                        if (storicoPunti != null) {
                            new StoricoPuntiController().removeStoricoPunti(storicoPunti);
                        }
                        int puntiAttuali = new StoricoPuntiController().getTotalePunti(((Movtesta) MovItem).getCodnom());
                        Nominativi nominativo = ((Movtesta) MovItem).getNominativi();
                        nominativo.setPunti(puntiAttuali);
                        new NominativiController().updateNominativi(nominativo);
                    }
                }
            } else {
                Messagebox.show("Selezionare una riga da Cancellare", "Information", Messagebox.OK, Messagebox.INFORMATION);
            }
        }
    }

    @SuppressWarnings("unchecked")
    void stampaDocumento2(String nomeJasper, sisi.movimenti.MovimentiDS ds, String nomeFilePDF) {
        InputStream is = null;
        try {
            String percorso = new sisi.General().percorsoFincati();
            String jasperFile = percorso + "/" + nomeJasper;
            File file = new File(jasperFile);
            FileInputStream fis = new FileInputStream(file);
            is = fis;
            final Map params = new HashMap();
            params.put("PATH_IMG", new sisi.General().percorsoImg());
            final byte[] buf = JasperRunManager.runReportToPdf(is, params, ds);
            Filedownload.save(new ByteArrayInputStream(buf), "application/pdf", nomeFilePDF);
            fis.close();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    public void onClick$btnStampa2() {
        try {
            int nIndex = boxMovimenti.getSelectedIndex();
            if (nIndex == -1) {
                Messagebox.show("Selezionare un Documento da Stampare", "Information", Messagebox.OK, Messagebox.INFORMATION);
            } else {
                MovItem = boxMovimenti.getSelectedItem().getAttribute("rigaTestaMovimento");
                Movtesta testa = new sisi.movimenti.MovtestaController().getMovtestaXNumeroMov(((Movtesta) MovItem).getNummov());
                String codTipMov = testa.getCodtipmov();
                sisi.tipimov.Tipimov cTipoMov = new sisi.tipimov.TipimovController().getTipimovXCodice(codTipMov);
                String cTipmov = cTipoMov.getTmtipmov();
                boolean lStampa = (cTipoMov.getTmstampa() != null && cTipoMov.getTmstampa().equalsIgnoreCase("S"));
                String fincatoStampa = cTipoMov.getTmfincato();
                if (fincatoStampa == null || fincatoStampa.isEmpty() || !lStampa) {
                    new General().MsgBox("Documento senza formato o non stampabile", "Errore");
                    return;
                }
                if (!new General().esisteReport(fincatoStampa)) {
                    new General().MsgBox("Formato inesistente: " + fincatoStampa.trim(), "Errore");
                    return;
                }
                Nominativi nominativo = new sisi.nominativi.NominativiController().getNominativoXCodice(testa.getCodnom(), false);
                if ("1237".contains(cTipmov) && "23".contains(cTipoMov.getTmcontro())) {
                    (Sessions.getCurrent()).setAttribute("idDocStampa", "" + testa.getTid());
                    sisi.movimenti.MovimentiDS ds = new sisi.movimenti.MovimentiDS();
                    String nomeFilePDF = cTipoMov.getCodtipmov().trim() + "-" + testa.getTndoc().trim() + "-" + (testa.getTddoc().getYear() + 1900) + "-" + nominativo.getNomnom().trim();
                    nomeFilePDF = nomeFilePDF.replaceAll(" ", "_");
                    nomeFilePDF = nomeFilePDF.replaceAll("/", "_");
                    nomeFilePDF = nomeFilePDF.replaceAll("\\.", "_");
                    nomeFilePDF += ".pdf";
                    stampaDocumento2(fincatoStampa.trim() + ".jasper", ds, nomeFilePDF);
                } else {
                    Messagebox.show("Stampa non ancora implementata per questo tipo di documento", "Information", Messagebox.OK, Messagebox.INFORMATION);
                }
            }
        } catch (SuspendNotAllowedException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("deprecation")
    public void esportaDocumento(int numMov) {
        try {
            Movtesta testa = new sisi.movimenti.MovtestaController().getMovtestaXNumeroMov(numMov);
            if (testa.getNummov() == 0) {
                System.out.println("Movimento non trovato!!!");
                return;
            }
            String codTipMov = testa.getCodtipmov();
            sisi.tipimov.Tipimov cTipoMov = new sisi.tipimov.TipimovController().getTipimovXCodice(codTipMov);
            String cTipmov = cTipoMov.getTmtipmov();
            boolean lStampa = (cTipoMov.getTmstampa() != null && cTipoMov.getTmstampa().equalsIgnoreCase("S"));
            String fincatoStampa = cTipoMov.getTmfincato();
            if (fincatoStampa == null || fincatoStampa.isEmpty() || !lStampa) {
                new General().MsgBox("Documento senza formato o non stampabile", "Errore");
                return;
            }
            if (!new General().esisteReport(fincatoStampa)) {
                new General().MsgBox("Formato inesistente: " + fincatoStampa.trim(), "Errore");
                return;
            }
            Nominativi nominativo = new sisi.nominativi.NominativiController().getNominativoXCodice(testa.getCodnom(), false);
            if ("1237".contains(cTipmov) && "23".contains(cTipoMov.getTmcontro())) {
                (Sessions.getCurrent()).setAttribute("idDocStampa", "" + testa.getTid());
                sisi.movimenti.MovimentiDS ds = new sisi.movimenti.MovimentiDS();
                String nomeFilePDF = cTipoMov.getTmdes().trim() + "-" + testa.getTndoc().trim() + "-" + (testa.getTddoc().getYear() + 1900) + "-" + nominativo.getNomnom() + ".pdf";
                nomeFilePDF = nomeFilePDF.replaceAll(" ", "_");
                nomeFilePDF = nomeFilePDF.replaceAll("/", "");
                stampaDocumento2(fincatoStampa.trim() + ".jasper", ds, nomeFilePDF);
            } else {
                Messagebox.show("Stampa non ancora implementata per questo tipo di documento", "Information", Messagebox.OK, Messagebox.INFORMATION);
            }
        } catch (SuspendNotAllowedException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void codifica() {
        byte testo[] = "Tutto il mio testo da decodificare".getBytes();
        try {
            MessageDigest algorithm = MessageDigest.getInstance("MD5");
            algorithm.reset();
            algorithm.update(testo);
            byte messageDigest[] = algorithm.digest();
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < messageDigest.length; i++) {
                hexString.append(Integer.toHexString(messageDigest[i]));
            }
            String foo = messageDigest.toString();
            System.out.println("Testo codificato in md5 " + foo + " --> " + hexString.toString());
        } catch (NoSuchAlgorithmException nsae) {
        }
    }
}
