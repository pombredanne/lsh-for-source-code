package formula1lei;

import data.*;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import org.hibernate.Session;
import util.HibernateUtil;

public class Utilis {

    private static String convertToHex(byte[] data) {
        StringBuilder buf = new StringBuilder();
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

    private static String hashPass(String p) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md;
        md = MessageDigest.getInstance("SHA-1");
        byte[] sha1hash = new byte[40];
        md.update(p.getBytes("iso-8859-1"), 0, p.length());
        sha1hash = md.digest();
        return convertToHex(sha1hash);
    }

    public static String getHashed(String s) {
        try {
            s = hashPass(s);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(Utilis.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(Utilis.class.getName()).log(Level.SEVERE, null, ex);
        }
        return s;
    }

    public static ArrayList<Users> loadUsers() {
        Session s = HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        ArrayList<Users> users = (ArrayList<Users>) s.createQuery("From Users").list();
        s.close();
        return users;
    }

    public static void fillTeams(JList l, ArrayList<Short> cods, String nome) {
        cods.clear();
        ArrayList<Equipas> lEquipas = new ArrayList<Equipas>();
        Session s = HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        if (nome.length() == 0) {
            lEquipas = (ArrayList<Equipas>) s.createQuery("from Equipas where deleted=0").list();
        } else {
            lEquipas = (ArrayList<Equipas>) s.createQuery("from Equipas where nome like '%" + nome + "%' and deleted=0").list();
        }
        s.close();
        l.removeAll();
        DefaultListModel model = new DefaultListModel();
        l.setModel(model);
        int i = 0;
        for (Equipas e : lEquipas) {
            model.add(i, e.getNome());
            i++;
            cods.add(new Short(e.getCodequipa()));
        }
    }

    public static void fillCars(JList l, ArrayList<Short> cods, String nome) {
        cods.clear();
        ArrayList<Carros> lCars = new ArrayList<Carros>();
        Session s = HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        if (nome.length() == 0) {
            lCars = (ArrayList<Carros>) s.createQuery("from Carros where deleted=0").list();
        } else {
            lCars = (ArrayList<Carros>) s.createQuery("from Carros where chassis like '%" + nome + "%' and deleted=0").list();
        }
        s.close();
        l.removeAll();
        DefaultListModel model = new DefaultListModel();
        l.setModel(model);
        int i = 0;
        for (Carros c : lCars) {
            model.add(i, c.getChassis());
            i++;
            cods.add(new Short(c.getCodcarro()));
        }
    }

    public static void fillProvas(JList l, ArrayList<Short> cods, String nome) {
        cods.clear();
        ArrayList<Provas> lProvas = new ArrayList<Provas>();
        Session s = HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        if (nome.length() == 0) {
            lProvas = (ArrayList<Provas>) s.createQuery("from Provas where deleted=0").list();
        } else {
            lProvas = (ArrayList<Provas>) s.createQuery("from Provas where nomecircuito like '%" + nome + "%' and deleted=0").list();
        }
        s.close();
        l.removeAll();
        DefaultListModel model = new DefaultListModel();
        l.setModel(model);
        int i = 0;
        for (Provas p : lProvas) {
            model.add(i, p.getNomecircuito());
            i++;
            cods.add(new Short(p.getCodprova()));
        }
    }

    public static void fillCountriesL(JList l, ArrayList<Short> cods, String nome) {
        cods.clear();
        ArrayList<Paises> lPaises = new ArrayList<Paises>();
        Session s = HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        if (nome.length() == 0) {
            lPaises = (ArrayList<Paises>) s.createQuery("from Paises where deleted=0").list();
        } else {
            lPaises = (ArrayList<Paises>) s.createQuery("from Paises where pais like '%" + nome + "%' and deleted=0").list();
        }
        s.close();
        l.removeAll();
        DefaultListModel model = new DefaultListModel();
        l.setModel(model);
        int i = 0;
        for (Paises p : lPaises) {
            model.add(i, p.getPais());
            i++;
            cods.add(new Short(p.getCodpais()));
        }
    }

    public static void fillPneus(JList l, ArrayList<Short> cods, String nome) {
        cods.clear();
        ArrayList<Pneus> lPneus = new ArrayList<Pneus>();
        Session s = HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        if (nome.length() == 0) {
            lPneus = (ArrayList<Pneus>) s.createQuery("from Pneus where deleted=0").list();
        } else {
            lPneus = (ArrayList<Pneus>) s.createQuery("from Pneus where pneu like '%" + nome + "%' and deleted=0").list();
        }
        s.close();
        l.removeAll();
        DefaultListModel model = new DefaultListModel();
        l.setModel(model);
        int i = 0;
        for (Pneus p : lPneus) {
            model.add(i, p.getPneu());
            i++;
            cods.add(new Short(p.getCodpneu()));
        }
    }

    public static void fillGas(JList l, ArrayList<Short> cods, String nome) {
        cods.clear();
        ArrayList<Combustiveis> lGas = new ArrayList<Combustiveis>();
        Session s = HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        if (nome.length() == 0) {
            lGas = (ArrayList<Combustiveis>) s.createQuery("from Combustiveis where deleted=0").list();
        } else {
            lGas = (ArrayList<Combustiveis>) s.createQuery("from Combustiveis where combustivel like '%" + nome + "%' and deleted=0").list();
        }
        s.close();
        l.removeAll();
        DefaultListModel model = new DefaultListModel();
        l.setModel(model);
        int i = 0;
        for (Combustiveis p : lGas) {
            model.add(i, p.getCombustivel());
            i++;
            cods.add(new Short(p.getCodcombustivel()));
        }
    }

    public static void fillPilotosList(JList l, ArrayList<Short> cods, String nome) {
        cods.clear();
        ArrayList<Pilotos> lPilotos = new ArrayList<Pilotos>();
        Session s = HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        if (nome.length() == 0) {
            lPilotos = (ArrayList<Pilotos>) s.createQuery("from Pilotos where deleted=0").list();
        } else {
            lPilotos = (ArrayList<Pilotos>) s.createQuery("from Pilotos where nome like '%" + nome + "%' and deleted=0").list();
        }
        s.close();
        l.removeAll();
        DefaultListModel model = new DefaultListModel();
        l.setModel(model);
        int i = 0;
        for (Pilotos p : lPilotos) {
            model.add(i, p.getNome());
            i++;
            cods.add(new Short(p.getCodpiloto()));
        }
    }

    public static void fillPilotosPeJL(JList l, ArrayList<Short> codsPils, String nomeEquipa, Short codtemporada) {
        codsPils.clear();
        ArrayList<Pilotos> lPilotos = new ArrayList<Pilotos>();
        Session s = HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        lPilotos = (ArrayList<Pilotos>) s.createQuery("select pe.pilotos from Pilotosequipas pe where pe.equipas.nome='" + nomeEquipa + "' and pe.deleted=0 and pe.temporada.codtemporada=" + codtemporada).list();
        s.close();
        l.removeAll();
        DefaultListModel model = new DefaultListModel();
        l.setModel(model);
        int i = 0;
        for (Pilotos p : lPilotos) {
            model.add(i, p.getNome());
            i++;
            codsPils.add(new Short(p.getCodpiloto()));
        }
    }

    public static void fillPilotosResJL(JList l, ArrayList<Short> codsPils, String nomeProva, Short codtemporada) {
        codsPils.clear();
        ArrayList<Pilotos> lPilotos = new ArrayList<Pilotos>();
        Session s = HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        lPilotos = (ArrayList<Pilotos>) s.createQuery("select res.pilotos from Resultados res where res.provas.nomecircuito='" + nomeProva + "' and res.deleted=0 and res.temporada.codtemporada=" + codtemporada).list();
        s.close();
        l.removeAll();
        DefaultListModel model = new DefaultListModel();
        l.setModel(model);
        int i = 0;
        for (Pilotos p : lPilotos) {
            model.add(i, p.getNome());
            i++;
            codsPils.add(new Short(p.getCodpiloto()));
        }
    }

    public static void fillPilotosResJL(JList l, ArrayList<Short> codsPils, String nomeProva, String nomePiloto, Short codtemporada) {
        codsPils.clear();
        ArrayList<Pilotos> lPilotos = new ArrayList<Pilotos>();
        Session s = HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        lPilotos = (ArrayList<Pilotos>) s.createQuery("select res.pilotos from Resultados res where res.provas.nomecircuito='" + nomeProva + "' and res.deleted=0 and res.pilotos.nome like '%" + nomePiloto + "%' and res.temporada.codtemporada=" + codtemporada).list();
        s.close();
        l.removeAll();
        DefaultListModel model = new DefaultListModel();
        l.setModel(model);
        int i = 0;
        for (Pilotos p : lPilotos) {
            model.add(i, p.getNome());
            i++;
            codsPils.add(new Short(p.getCodpiloto()));
        }
    }

    public static int repeatedResult(Resultados re) {
        ArrayList<Resultados> res = new ArrayList<Resultados>();
        Session s = HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        res = (ArrayList<Resultados>) s.createQuery("from Resultados res" + " where res.posicaocorrida=" + re.getPosicaocorrida() + " and res.pilotos.codpiloto!=" + re.getPilotos().getCodpiloto() + " and res.temporada.codtemporada=" + re.getTemporada().getCodtemporada() + " and res.provas.codprova=" + re.getProvas().getCodprova()).list();
        if (res.size() > 0) {
            return 1;
        } else {
            res = (ArrayList<Resultados>) s.createQuery("from Resultados res" + " where res.posicaoqualificacao=" + re.getPosicaoqualificacao() + " and res.pilotos.codpiloto!=" + re.getPilotos().getCodpiloto() + " and res.temporada.codtemporada=" + re.getTemporada().getCodtemporada() + " and res.provas.codprova=" + re.getProvas().getCodprova()).list();
            if (res.size() > 0) {
                return 2;
            } else {
                return 0;
            }
        }
    }

    public static void fillDays(JComboBox cb) {
        cb.removeAllItems();
        for (int i = 1; i <= 31; i++) {
            cb.addItem(i);
        }
    }

    public static void fillMonths(JComboBox cb) {
        cb.removeAllItems();
        for (int i = 1; i <= 12; i++) {
            cb.addItem(i);
        }
    }

    public static void fillYears(JComboBox cb) {
        cb.removeAllItems();
        GregorianCalendar today = new GregorianCalendar();
        for (int i = 1900; i <= today.get(today.YEAR); i++) {
            cb.addItem(i);
        }
    }

    public static void fillCountries(JComboBox cb, ArrayList<Short> codsPais) {
        codsPais.clear();
        Session s = HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        ArrayList<Paises> paises = (ArrayList<Paises>) s.createQuery("From Paises where deleted=0").list();
        s.close();
        cb.removeAllItems();
        for (Paises p : paises) {
            cb.addItem(p.getPais());
            codsPais.add(new Short(p.getCodpais()));
        }
    }

    public static void fillPneusCB(JComboBox cb, ArrayList<Short> codsPneus) {
        codsPneus.clear();
        Session s = HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        ArrayList<Pneus> pneus = (ArrayList<Pneus>) s.createQuery("From Pneus where deleted=0").list();
        s.close();
        cb.removeAllItems();
        for (Pneus p : pneus) {
            cb.addItem(p.getPneu());
            codsPneus.add(new Short(p.getCodpneu()));
        }
    }

    public static void fillGasCB(JComboBox cb, ArrayList<Short> codsGas) {
        codsGas.clear();
        Session s = HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        ArrayList<Combustiveis> gas = (ArrayList<Combustiveis>) s.createQuery("From Combustiveis where deleted=0").list();
        s.close();
        cb.removeAllItems();
        for (Combustiveis g : gas) {
            cb.addItem(g.getCombustivel());
            codsGas.add(new Short(g.getCodcombustivel()));
        }
    }

    public static void fillTeamCB(JComboBox cb, ArrayList<Short> codsTeams) {
        codsTeams.clear();
        Session s = HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        ArrayList<Equipas> teams = (ArrayList<Equipas>) s.createQuery("From Equipas where deleted=0").list();
        s.close();
        cb.removeAllItems();
        for (Equipas e : teams) {
            cb.addItem(e.getNome());
            codsTeams.add(new Short(e.getCodequipa()));
        }
    }

    public static void fillTempCB(JComboBox cb, ArrayList<Short> codsTemps) {
        codsTemps.clear();
        Session s = HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        ArrayList<Temporada> temps = (ArrayList<Temporada>) s.createQuery("From Temporada where deleted=0").list();
        s.close();
        cb.removeAllItems();
        for (Temporada t : temps) {
            cb.addItem(t.getAno());
            codsTemps.add(new Short(t.getCodtemporada()));
        }
    }

    public static GregorianCalendar dateToCalendar(Date d) {
        GregorianCalendar date = new GregorianCalendar();
        date.setTime(d);
        return date;
    }

    public static int giveCountryIndex(String pais, JComboBox cb) {
        boolean found = false;
        int i = 0;
        while (!found) {
            if (cb.getItemAt(i).equals(pais)) {
                found = true;
            } else {
                i++;
            }
        }
        return i;
    }

    public static int giveMonthIndex(Date d) {
        boolean found = false;
        int i = 1;
        GregorianCalendar g = dateToCalendar(d);
        int mon = g.get(GregorianCalendar.MONTH);
        while (!found) {
            if (mon == i) {
                found = true;
            } else {
                i++;
            }
        }
        return i;
    }

    public static int giveDayIndex(Date d) {
        boolean found = false;
        int i = 1;
        GregorianCalendar g = dateToCalendar(d);
        int day = g.get(GregorianCalendar.DAY_OF_MONTH);
        while (!found) {
            if (day == i) {
                found = true;
            } else {
                i++;
            }
        }
        return i;
    }

    public static int giveYearIndex(Date d) {
        boolean found = false;
        int i = 1, y = 1900;
        GregorianCalendar g = dateToCalendar(d);
        int year = g.get(GregorianCalendar.YEAR);
        while (!found) {
            if (year == y) {
                found = true;
            } else {
                i++;
                y++;
            }
        }
        return i;
    }

    static int giveGasIndex(Combustiveis comb, JComboBox cb) {
        boolean found = false;
        int i = 0;
        while (!found) {
            if (cb.getItemAt(i).equals(comb.getCombustivel())) {
                found = true;
            } else {
                i++;
            }
        }
        return i;
    }

    static int givePneuIndex(Pneus pneu, JComboBox cb) {
        boolean found = false;
        int i = 0;
        while (!found) {
            if (cb.getItemAt(i).equals(pneu.getPneu())) {
                found = true;
            } else {
                i++;
            }
        }
        return i;
    }

    static int giveTeamIndex(Equipas team, JComboBox cb) {
        boolean found = false;
        int i = 0;
        while (!found) {
            if (cb.getItemAt(i).equals(team.getNome())) {
                found = true;
            } else {
                i++;
            }
        }
        return i;
    }

    static int giveTempIndex(Temporada temp, JComboBox cb) {
        boolean found = false;
        int i = 0;
        while (!found) {
            if (cb.getItemAt(i).equals(temp.getAno())) {
                found = true;
            } else {
                i++;
            }
        }
        return i;
    }

    public void changeIcon(JLabel label, String icon) {
        try {
            label.setIcon(new javax.swing.ImageIcon(getClass().getResource("/formula1lei/resources/" + icon)));
        } catch (NullPointerException ex) {
            label.setIcon(new javax.swing.ImageIcon(System.getProperty("user.dir") + File.separatorChar + icon));
            Icon icon1 = label.getIcon();
            if (icon1.getIconHeight() == -1) {
                label.setIcon(new javax.swing.ImageIcon(getClass().getResource("/formula1lei/resources/" + "default.jpg")));
            }
        }
    }

    public void changeIconCar(JLabel label, String icon) {
        try {
            label.setIcon(new javax.swing.ImageIcon(getClass().getResource("/formula1lei/resources/" + icon)));
        } catch (NullPointerException ex) {
            label.setIcon(new javax.swing.ImageIcon(System.getProperty("user.dir") + File.separatorChar + icon));
            Icon icon1 = label.getIcon();
            if (icon1.getIconHeight() == -1) {
                label.setIcon(new javax.swing.ImageIcon(getClass().getResource("/formula1lei/resources/" + "defaultCar.jpg")));
            }
        }
    }

    public static void update(Object o) {
        Session s = HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        s.update(o);
        s.getTransaction().commit();
    }

    public static void save(Object o) {
        Session s = HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        s.save(o);
        s.getTransaction().commit();
    }

    public static void delete(Object o) {
        Session s = HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        s.delete(o);
        s.getTransaction().commit();
    }

    public static void lastIndex(JList list) {
        list.setSelectedIndex(list.getLastVisibleIndex());
    }

    public static void updateTiresComps(JList list, JComboBox cb, ArrayList<Short> cods) {
        Utilis.fillPneus(list, cods, "");
        Utilis.fillPneusCB(cb, cods);
    }

    public static void updateGasComps(JList list, JComboBox cb, ArrayList<Short> cods) {
        Utilis.fillGas(list, cods, "");
        Utilis.fillGasCB(cb, cods);
    }
}
