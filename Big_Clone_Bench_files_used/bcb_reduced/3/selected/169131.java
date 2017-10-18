package br.absolut.util.function;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import javax.el.ELContext;
import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import javax.faces.context.FacesContext;
import javax.swing.text.MaskFormatter;

public class Funcoes {

    public static final String MASCARA_CPF = "###.###.###-##";

    public static final String MASCARA_CEP = "##.###-###";

    public static final String MASCARA_TELEFONE = "####-####";

    public static final String MASCARA_CNPJ = "##.###.###/####-##";

    public static final String MASCARA_DINHEIRO = "###.###.###.###,##";

    public static String retiraCaracteresFormatacao(String texto) {
        texto = texto.replace(".", "");
        texto = texto.replace("-", "");
        texto = texto.replace("/", "");
        return texto;
    }

    public static Double stringToDouble(String valor) {
        Double c = null;
        DecimalFormatSymbols dfs = new DecimalFormatSymbols();
        dfs.setDecimalSeparator(',');
        dfs.setGroupingSeparator('.');
        DecimalFormat nf = (DecimalFormat) DecimalFormat.getInstance(Locale.ENGLISH);
        nf.setDecimalFormatSymbols(dfs);
        try {
            c = nf.parse(valor).doubleValue();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return c;
    }

    public static String utilDateToStringDDMMYYYY(Date data) {
        String dataAux = "";
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
        dataAux = format.format(data);
        return dataAux;
    }

    public static java.sql.Date utilDateToSqlDate(Date data) {
        java.sql.Date dataSql = new java.sql.Date(data.getTime());
        return dataSql;
    }

    public static Date StringDDMMYYYYToUtilDate(String d) throws Exception {
        SimpleDateFormat formato = new SimpleDateFormat("dd/MM/yyyy");
        Date data = null;
        try {
            data = formato.parse(d);
        } catch (ParseException e) {
            throw new Exception();
        }
        return data;
    }

    public static Date somarData(Date data, int qtdDias) {
        Date nova_data = new Date(data.getTime() + ((1000 * 24 * 60 * 60) * qtdDias));
        return nova_data;
    }

    public static String formataValor(Double valor) {
        String aux;
        if (valor != 0.0) {
            aux = NumberFormat.getCurrencyInstance().format(valor);
            aux = aux.replace("R$ ", "");
        } else {
            aux = "0,00";
        }
        return aux;
    }

    public static String formataValor(String valor) {
        Double aux = new Double(valor);
        return formataValor(aux);
    }

    public static String aplicaMascara(String campo, String mascara) throws ParseException {
        if (campo == null) campo = "";
        MaskFormatter mask = new MaskFormatter(mascara);
        mask.setValueContainsLiteralCharacters(false);
        return mask.valueToString(campo);
    }

    public static String hashMD5(String senha) {
        String hash = "";
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        BigInteger bigInteger = new BigInteger(1, md.digest(senha.getBytes()));
        hash = bigInteger.toString();
        return hash;
    }

    @SuppressWarnings("unchecked")
    public static Object getBean(String nameBean, Class classe) {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        ELContext elContext = facesContext.getELContext();
        ExpressionFactory ef = facesContext.getApplication().getExpressionFactory();
        ValueExpression ve = ef.createValueExpression(elContext, "#{" + nameBean + "}", classe);
        return ve.getValue(elContext);
    }

    public static double calcularValorDesconto(double desconto, double total) {
        double aux = total * (desconto / 100);
        return aux;
    }

    public static double calculaLucro(double lucro, double valor) {
        double aux = valor * (lucro / 100);
        return aux;
    }

    public static Date gerarVariacaoData(Date data, int qtdVariacao, int tipoCalendar) {
        GregorianCalendar dataAtual = new GregorianCalendar();
        dataAtual.setTimeInMillis(data.getTime());
        switch(tipoCalendar) {
            case Constantes.TIPO_CALENDAR_DIA:
                dataAtual.add(Calendar.DAY_OF_MONTH, qtdVariacao);
                break;
            case Constantes.TIPO_CALENDAR_MES:
                dataAtual.add(Calendar.MONTH, qtdVariacao);
                break;
            case Constantes.TIPO_CALENDAR_ANO:
                dataAtual.add(Calendar.YEAR, qtdVariacao);
                break;
        }
        return dataAtual.getTime();
    }
}
