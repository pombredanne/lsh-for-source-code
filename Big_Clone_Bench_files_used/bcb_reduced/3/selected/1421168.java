package vydavky.service.ejb;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import javax.persistence.PersistenceException;
import oracle.toplink.essentials.exceptions.DatabaseException;
import vydavky.client.ciselniky.TypNavratu;
import vydavky.client.objects.clientserver.NavratValue;
import vydavky.client.utils.ClientUtils;

/**
 * Trieda s utilitami pre vrstvu EJB.
 */
public final class BeanUtils {

    /**
   * Neviditelny konstruktor braniaci instanciaci triedy.
   */
    private BeanUtils() {
    }

    /** Meno PU, ktoru ma AS pouzivat. */
    public static final String PERSISTENCE_UNIT = "VydavkyAS-ejbPU";

    /** Meno sekvencie generujucej IDcka (nazov z DB). */
    public static final String SEQUENCE_NAME = "MAIN_APP_SEQ";

    /** Meno sekvencie generujucej IDcka (platne len v kontexte AS). */
    public static final String GENERATOR = "MainAppSeq";

    /** Casto pouzivany retazec. */
    public static final String POUZITE = "Objekt sa pouziva";

    /** Casto pouzivany retazec. */
    public static final String DUPLICITA = "Duplicita";

    /** Select pre zistovanie ci je clovek pouzity. */
    public static final String SELECT_POUZITIE_LUDI = "SELECT * FROM pouzitie_ludi pl WHERE pl.clovek_id = #clovekId";

    /** Select pre zistovanie ci je skupina pouzita. */
    public static final String SELECT_POUZITIE_SKUPIN = "SELECT * FROM pouzitie_skupin ps WHERE ps.skupina_id = #skupinaId";

    /** Select pre zistovanie ci je projekt pouzity. */
    public static final String SELECT_POUZITIE_PROJEKTOV = "SELECT * FROM pouzitie_projektov pp WHERE pp.projekt_id = #projektId";

    /** Select pre zistovanie ci je transakcia pouzita. */
    public static final String SELECT_POUZITIE_TRANSAKCII = "SELECT * FROM pouzitie_transakcii pt WHERE pt.transakcia_id = #transakciaId";

    /** Select pre zistovanie ci je mena pouzita. */
    public static final String SELECT_POUZITIE_MIEN = "SELECT * FROM transakcie t WHERE t.mena_id = #menaId";

    /** Select pre zistovanie ci je typ vydavku pouzity. */
    public static final String SELECT_POUZITIE_TYPOV_VYDAVKU = "SELECT * FROM transakcie t WHERE t.typ_vydavku_id = #typVydavkuId";

    /** Zaklad selectu pre filtrovany vyber transakcii. */
    public static final String SELECT_FILTER_TRANSAKCII_ZAKLAD = "SELECT t FROM Transakcie t " + "LEFT JOIN FETCH t.objekty " + "LEFT JOIN FETCH t.menaId " + "LEFT JOIN FETCH t.typVydavkuId " + "LEFT JOIN FETCH t.projektId " + "LEFT JOIN FETCH t.ludiaTransakcieCollection " + "LEFT JOIN FETCH t.skupinyTransakcieCollection ";

    /** Select pre vyber transakcii podla ucastnika. */
    public static final String SELECT_FILTER_TRANSAKCII_UCASTNIK = "SELECT t.* FROM transakcie t " + "  WHERE t.transakcia_id IN ( " + "    (SELECT DISTINCT t.transakcia_id FROM transakcie t, ludia_transakcie lst WHERE t.transakcia_id = lst.transakcia_id " + "        AND lst.clovek_id = ?1" + "        :rola)" + "   UNION ALL " + "    (SELECT DISTINCT t.transakcia_id FROM transakcie t, skupiny_transakcie lst WHERE t.transakcia_id = lst.transakcia_id " + "        AND lst.skupina_id = ?2 " + "        :rola)  )";

    /** Select pre vyber transakcii podla popisu. */
    public static final String SELECT_FILTER_TRANSAKCII_POPIS = "SELECT t.* FROM transakcie t, objekty o WHERE t.transakcia_id = o.objekt_id AND o.storno = 0 AND t.popis LIKE '%:popis%'";

    /** Delete pre zmazanie ludi zo zadanej transakcie. */
    public static final String DELETE_LUDIA_TRANSAKCIE = "DELETE FROM ludia_transakcie lt WHERE lt.transakcia_id = #transakcia_id";

    /** Delete pre zmazanie skupin zo zadanej transakcie. */
    public static final String DELETE_SKUPINY_TRANSAKCIE = "DELETE FROM skupiny_transakcie st WHERE st.transakcia_id = #transakcia_id";

    /** Select pre zistenie, ci su vsetci ludia z danej transakcie aj v projekte
   * tejto transakcie. */
    public static final String SELECT_LUDIA_V_PROJEKTE_TRANSAKCIE = "SELECT COUNT(lp.clovek_id) FROM ludia_projekty lp WHERE lp.clovek_id IN (:ludia) AND lp.projekt_id = :projektId";

    /** Select pre zistenie, ci su vsetky skupiny z danej transakcie aj v projekte
   * tejto transakcie. */
    public static final String SELECT_SKUPINY_V_PROJEKTE_TRANSAKCIE = "SELECT COUNT(sp.skupina_id) FROM skupiny_projekty sp WHERE sp.skupina_id IN (:skupiny) AND sp.projekt_id = :projektId";

    /** Insert pre vlozenie noveho zamku. */
    public static final String INSERT_ZAMOK = "INSERT INTO zamky (user_id, objekt_id) VALUES (#userId, #objektId)";

    /** Delete pre zmazanie vsetkych zamkov daneho pouzivatela. */
    public static final String DELETE_ZAMOK = "DELETE FROM zamky z WHERE z.user_id = #userId";

    /** Insert pre vlozenie zaznamu o stupni zamknutia. */
    public static final String INSERT_ZAMOK_STUPEN = "INSERT INTO zamky_stupen (user_id, objekt_id) VALUES (#userId, #objektId)";

    /** Delete pre zmazanie vsetkych stupnov zamknutia daneho pouzivatela. */
    public static final String DELETE_ZAMOK_STUPEN = "DELETE FROM zamky_stupen zs WHERE zs.user_id = #userId";

    /** Insert pre vlozenie noveho systemoveho zamku. */
    public static final String INSERT_SYSTEMOVY_ZAMOK = "INSERT INTO zamky_system (user_id, zamok_id) VALUES (#userId, #zamokId)";

    /** Delete pre zmazanie vsetkych systemovych zamkov daneho pouzivatela. */
    public static final String DELETE_SYSTEMOVY_ZAMOK = "DELETE FROM zamky_system zs WHERE zs.user_id = #userId";

    /**
   * Prelozi vynimku tak, aby jej bolo rozumiet aj mimo AS.
   *
   * @param e Vynimka potencialne nestravitelna klientom.
   * @return Zabalena vynimka zo vstupu tak, aby jej rozumel klient.
   */
    public static NavratValue prelozVynimku(final Exception e) {
        if (e instanceof PersistenceException) {
            if (ClientUtils.getStackTrace(e).contains("ORA-00001: ")) {
                return new NavratValue(TypNavratu.PORUSENIE_PK, "Zlyhalo ukladanie - porusenie primarneho kluca", ClientUtils.getStackTrace(e));
            }
            return new NavratValue(TypNavratu.CHYBA, "Zlyhalo ukladanie - PersistenceException", ClientUtils.getStackTrace(e));
        }
        if (e instanceof DatabaseException) {
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw, true);
            e.printStackTrace(pw);
            pw.flush();
            sw.flush();
            return new NavratValue(TypNavratu.CHYBA, "DatabaseException", sw.toString());
        }
        return new NavratValue(TypNavratu.CHYBA, "Zlyhalo ukladanie", e);
    }

    /**
   * Prevedie zoznam poloziek na stringovu reprezentaciu, teda
   * <code>toString</code>-y kazdej polozky oddelene ciarkami.
   * Pouziva na to <code>Collection.toString</code>, z ktoreho nasledne odstrani
   * uvodne [ a zaverecne ].
   * @param <T> Cokolvek sa da napchat do collection.
   * @param l Zoznam poloziek.
   * @return Stringova reprezentacia zoznamu.
   */
    public static <T> String listToString(final Collection<T> l) {
        return l.toString().replaceAll("\\[", "").replaceAll("\\]", "");
    }

    private static String convertToHex(final byte[] data) {
        final StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9)) {
                    buf.append((char) ('0' + halfbyte));
                } else {
                    buf.append((char) ('a' + (halfbyte - 10)));
                }
                halfbyte = data[i] & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }

    /**
   * Vrati SHA-1 zo zadaneho retazca.
   *
   * @param text Retazec, ktory ma byt zahashovany.
   * @return Hash zadaneho retazca.
   * @throws java.security.NoSuchAlgorithmException
   * @throws java.io.UnsupportedEncodingException
   */
    public static String SHA1(final String text) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        final MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(text.getBytes("utf-8"), 0, text.length());
        return convertToHex(md.digest());
    }
}
