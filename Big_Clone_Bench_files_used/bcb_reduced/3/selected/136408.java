package br.com.pleno.core.util;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.NbBundle;

/**
 *
 * @author Lourival Almeida
 */
public class Utilitario {

    private static StringBuffer sb = new StringBuffer();

    private static DecimalFormatSymbols dfs;

    private static NumberFormat formatterHora;

    private static final String hexDigits = "0123456789abcdef";

    static {
        dfs = new DecimalFormatSymbols();
        dfs.setDecimalSeparator(',');
        formatterHora = new DecimalFormat("###,###,###,###.##", dfs);
        TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"));
    }

    /**
     * Retorna um stringbuffer para ser utilizado por aplica��es.
     */
    public static StringBuffer getSB(boolean limparStringBuffer) {
        if (limparStringBuffer) sb.delete(0, sb.length());
        return sb;
    }

    public static Date parse(String data) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        return sdf.parse(data);
    }

    public static String format(Date data) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        return sdf.format(data);
    }

    public static String format(Date data, String formato) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat(formato);
        return sdf.format(data);
    }

    /**
     * Retorna uma data com timezone correto
     */
    public static Date getDateTimeZone() {
        return new Date();
    }

    public static String formatarHora(Date data) throws Exception {
        return format(data, "HH:mm:ss");
    }

    public static String formatarDataHora(Date data) throws Exception {
        return format(data, "dd/MM/yyyy HH:mm:ss");
    }

    /**
     * Transforma um valor em horas para milisegundos
     */
    public static long transformarHoraMili(long horas) {
        return horas * 3600000;
    }

    /**
     * Transforma um valor em milisegundos para horas
     * Obs.: Esta opera��o perder� precis�o dos valores.
     */
    public static BigDecimal transformarMiliHora(long mili) {
        BigDecimal miliSeconds = new BigDecimal(mili);
        BigDecimal taxa = new BigDecimal(3600000);
        BigDecimal res = miliSeconds.divide(taxa, BigDecimal.ROUND_DOWN);
        return res;
    }

    /**
     * Formata o valor como uma hora
     */
    public static String formatarDouble(BigDecimal valor) {
        return formatterHora.format(valor.doubleValue());
    }

    /**
     * Exibir mensagem de erro na tela
     */
    public static void mostrarMensagemErro(Messages mensagem) {
        NotifyDescriptor nd = new NotifyDescriptor.Message(NbBundle.getMessage(Messages.class, mensagem.getChave()), NotifyDescriptor.ERROR_MESSAGE);
        DialogDisplayer.getDefault().notify(nd);
    }

    /**
     * Exibir uma mensagem informativa para o usu�rio.
     */
    public static void mostrarMensagemInformativa(Messages mensagem) {
        NotifyDescriptor nd = new NotifyDescriptor.Message(NbBundle.getMessage(Messages.class, mensagem.getChave()), NotifyDescriptor.INFORMATION_MESSAGE);
        DialogDisplayer.getDefault().notify(nd);
    }

    public static String mostrarDialogoEntrada(String tituloJanela, String campo) {
        NotifyDescriptor.InputLine il = new NotifyDescriptor.InputLine(tituloJanela, campo);
        DialogDisplayer.getDefault().notify(il);
        return il.getInputText();
    }

    public static void mostrarPainel(Object painel) {
        DialogDescriptor descritor = new DialogDescriptor(painel, "Texto");
        DialogDisplayer.getDefault().notify(descritor);
    }

    public static String formataHoraMinutoSegundo(long mili) {
        int nHoras = (int) (mili / 3600000);
        int nHorasResto = (int) (mili % 3600000);
        getSB(true).append(String.valueOf(nHoras));
        getSB(false).append("hs : ");
        nHoras = (int) (nHorasResto / 60000);
        nHorasResto = (int) (nHorasResto % 60000);
        getSB(false).append(String.valueOf(nHoras));
        getSB(false).append("min : ");
        nHoras = (int) (nHorasResto / 1000);
        getSB(false).append(String.valueOf(nHoras));
        getSB(false).append("s");
        return getSB(false).toString();
    }

    public static String stringHexa(byte[] b) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < b.length; i++) {
            int j = ((int) b[i]) & 0xFF;
            buf.append(hexDigits.charAt(j / 16));
            buf.append(hexDigits.charAt(j % 16));
        }
        return buf.toString();
    }

    public static byte[] gerarHash(String frase) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(frase.getBytes());
            return md.digest();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
}
