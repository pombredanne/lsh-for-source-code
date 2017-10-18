package br.ufc.quixada.adrs.util;

import br.ufc.quixada.adrs.model.Administrador;
import br.ufc.quixada.adrs.model.Adrs;
import br.ufc.quixada.adrs.model.Endereco;
import br.ufc.quixada.adrs.model.Prazo;
import br.ufc.quixada.adrs.model.Produtor;
import br.ufc.quixada.adrs.model.Supervisor;
import br.ufc.quixada.adrs.model.Usuario;
import br.ufc.quixada.adrs.model.Visita;
import br.ufc.quixada.adrs.model.Visitante;
import br.ufc.quixada.adrs.service.ProdutorService;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import org.slf4j.LoggerFactory;
import sun.misc.BASE64Encoder;

/**
 *
 * @author Caio
 */
public final class UtilAdrs {

    /**
     * Overcomes the default constructor. Utility classes should be final and must not have public constructors.
     */
    private UtilAdrs() {
    }

    public static void upperCase(Usuario a) {
        if (a.getApelido() != null) {
            a.setApelido(a.getApelido().toUpperCase(Locale.US));
        }
        a.setNome(a.getNome().toUpperCase(Locale.US));
        if (a.getEndereco().getLocalidade() != null) {
            a.getEndereco().setLocalidade(a.getEndereco().getLocalidade().toUpperCase(Locale.US));
        }
    }

    public static void upperCase(Endereco a) {
        if (a.getLocalidade() != null) {
            a.setLocalidade(a.getLocalidade().toUpperCase(Locale.US));
        }
    }

    public static void upperCase(Supervisor supervisor) {
        upperCase(supervisor.getUsuario());
    }

    public static void upperCase(Administrador a) {
        upperCase(a.getUsuario());
    }

    public static void upperCase(Visitante a) {
        upperCase(a.getUsuario());
    }

    public static void upperCase(Adrs a) {
        upperCase(a.getUsuario());
    }

    public static void upperCase(Produtor a) {
        upperCase(a.getUsuario());
        if (a.getConjuge() != null) {
            a.setConjuge(a.getConjuge().toUpperCase(Locale.US));
        }
        if (a.getMae() != null) {
            a.setMae(a.getMae().toUpperCase(Locale.US));
        }
        if (a.getPai() != null) {
            a.setPai(a.getPai().toUpperCase(Locale.US));
        }
    }

    public static Date treatToDate(String param) {
        if (param != null && param.trim().length() == 10) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            sdf.setLenient(false);
            try {
                return sdf.parse(param);
            } catch (Exception ex) {
                return null;
            }
        }
        return null;
    }

    public static String treatToString(Date param) {
        if (param != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            String result = sdf.format(param);
            return result;
        }
        return "";
    }

    public static String treatToLongString(Date param) {
        if (param != null) {
            Locale locale = new Locale("pt", "BR");
            SimpleDateFormat formatter = new SimpleDateFormat("EEEE, dd 'de' MMMM 'de' yyyy", locale);
            String data = formatter.format(param);
            return data;
        }
        return "";
    }

    public static boolean validaData(String data) {
        if (data != null && data.trim().length() == 10) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            sdf.setLenient(false);
            try {
                if (sdf.parse(data).before(sdf.parse("01/01/1990"))) {
                    return false;
                }
                return true;
            } catch (Exception ex) {
                return false;
            }
        }
        return false;
    }

    public static boolean isNumber(char a) {
        if (a == '0' || a == '1' || a == '2' || a == '3' || a == '4' || a == '5' || a == '6' || a == '7' || a == '8' || a == '9') {
            return true;
        }
        return false;
    }

    public static boolean isNumber(String a) {
        if (a == null) {
            return false;
        }
        for (int i = 0; i < a.length(); i++) {
            if (!isNumber(a.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static List<Adrs> filtrarAdrsAtrasadosNoPrazo(Prazo prazo) {
        List<Adrs> adrssAtrasadosDoPrazo = new ArrayList<Adrs>(prazo.getListaAdrs());
        for (int j = 0; j < prazo.getListaAdrs().size(); j++) {
            Adrs adrs = prazo.getListaAdrs().get(j);
            List<Produtor> produtoresAtrasados = new ArrayList<Produtor>();
            ProdutorService produtrS = new ProdutorService();
            if (prazo.isSanitario()) {
                produtoresAtrasados = produtrS.getProdutoresForaPrazoSanitario(prazo, adrs);
            } else if (prazo.isQuantitativo()) {
                produtoresAtrasados = produtrS.getProdutoresForaPrazoQuantitativo(prazo, adrs);
            } else if (prazo.isQualitativo()) {
                produtoresAtrasados = produtrS.getProdutoresForaPrazoQualitativo(prazo, adrs);
            }
            if (produtoresAtrasados.isEmpty()) {
                removeAdrsDaListaDeAtrasados(adrssAtrasadosDoPrazo, adrs);
            } else {
                removeAdrsDaListaDeAtrasados(adrssAtrasadosDoPrazo, adrs);
                adrs.setProdutoresPendentes(produtoresAtrasados);
                adrssAtrasadosDoPrazo.add(adrs);
            }
        }
        return adrssAtrasadosDoPrazo;
    }

    static void removeAdrsDaListaDeAtrasados(List<Adrs> adrss, Adrs adrs) {
        for (int i = 0; i < adrss.size(); i++) {
            if (adrss.get(i).getId().equals(adrs.getId())) {
                adrss.remove(i);
                break;
            }
        }
    }

    public static Boolean verificaNumeroInteiro(String numero) {
        if (numero == null) {
            return false;
        }
        try {
            if (!numero.trim().isEmpty()) {
                Long.parseLong(numero);
            }
            return true;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }

    public static Boolean verificaNumeroReal(String numero) {
        if (numero == null) {
            return false;
        }
        try {
            if (!numero.trim().isEmpty()) {
                Double.parseDouble(numero);
            }
            return true;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }

    public static String criptografar(String senha) {
        if (senha == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(senha.getBytes());
            BASE64Encoder encoder = new BASE64Encoder();
            return encoder.encode(digest.digest());
        } catch (NoSuchAlgorithmException ns) {
            LoggerFactory.getLogger(UtilAdrs.class).error(Msg.EXCEPTION_MESSAGE, UtilAdrs.class.getSimpleName(), ns);
            return senha;
        }
    }

    public static String novoMetodoDeCriptografarParaMD5QueNaoFoiUtilizadoAinda(String input) {
        if (input == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(input.getBytes("UTF-8"));
            BigInteger hash = new BigInteger(1, digest.digest());
            String output = hash.toString(16);
            if (output.length() < 32) {
                int sizeDiff = 32 - output.length();
                do {
                    output = "0" + output;
                } while (--sizeDiff > 0);
            }
            return output;
        } catch (NoSuchAlgorithmException ns) {
            LoggerFactory.getLogger(UtilAdrs.class).error(Msg.EXCEPTION_MESSAGE, UtilAdrs.class.getSimpleName(), ns);
            return input;
        } catch (UnsupportedEncodingException e) {
            LoggerFactory.getLogger(UtilAdrs.class).error(Msg.EXCEPTION_MESSAGE, UtilAdrs.class.getSimpleName(), e);
            return input;
        }
    }

    public static Date getMesAnterior(Date data) {
        if (data == null) {
            return null;
        }
        Calendar cal = GregorianCalendar.getInstance();
        cal.setTime(data);
        cal.add(Calendar.MONTH, -1);
        return cal.getTime();
    }

    public static Date getMesPosterior(Date data) {
        if (data == null) {
            return null;
        }
        Calendar cal = GregorianCalendar.getInstance();
        cal.setTime(data);
        cal.add(Calendar.MONTH, 1);
        return cal.getTime();
    }

    public static boolean isCurrentMonth(Date data) {
        if (data == null) {
            return false;
        }
        Calendar cal = GregorianCalendar.getInstance();
        Calendar calCurrent = GregorianCalendar.getInstance();
        cal.setTime(data);
        calCurrent.setTime(new Date());
        return cal.get(Calendar.MONTH) == calCurrent.get(Calendar.MONTH) && cal.get(Calendar.YEAR) == calCurrent.get(Calendar.YEAR);
    }

    public static String treatToMonthYearString(Date param) {
        if (param != null) {
            Locale locale = new Locale("pt", "BR");
            SimpleDateFormat formatter = new SimpleDateFormat("MMMM 'de' yyyy", locale);
            String data = formatter.format(param);
            return data;
        }
        return "";
    }

    public static String formataCPF(String cpf) {
        if (cpf == null || cpf.trim().length() != 11) {
            return cpf;
        }
        StringBuilder sb = new StringBuilder(cpf);
        sb.insert(3, '.');
        sb.insert(7, '.');
        sb.insert(11, '-');
        return sb.toString();
    }

    public static String desFormataCPF(String cpf) {
        if (cpf == null) {
            return cpf;
        }
        return retornaNumeros(cpf);
    }

    public static boolean diferencaDatas(Produtor produtor) {
        Calendar dataInicio = GregorianCalendar.getInstance();
        dataInicio.setTime(produtor.getDataCriacao());
        Calendar dataFinal = GregorianCalendar.getInstance();
        double diferenca = dataFinal.getTimeInMillis() - dataInicio.getTimeInMillis();
        int tempoDia = 1000 * 60 * 60 * 24;
        double diasDiferenca = diferenca / tempoDia;
        if (diasDiferenca < 31D) {
            produtor.setNovoProdutor(true);
            return true;
        }
        return false;
    }

    public static void destacarProdutor(Produtor produtor) {
        if (produtor != null) {
            produtor.getUsuario().setNome("*" + produtor.getUsuario().getNome());
        }
    }

    public static void marcarProdutoresNovos(List<Produtor> produtores) {
        if (produtores != null) {
            for (Produtor produtor : produtores) {
                if (diferencaDatas(produtor)) {
                    destacarProdutor(produtor);
                }
            }
        }
    }

    public static String retornaNumeros(String entrada) {
        if (entrada == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < entrada.length(); i++) {
            if (isNumber(entrada.charAt(i))) {
                sb.append(entrada.charAt(i));
            }
        }
        return sb.toString();
    }

    public static boolean verificarSQLInjection(String login) {
        if (login != null && !login.trim().isEmpty()) {
            char[] sql = login.toCharArray();
            for (int i = 0; i < sql.length; i++) {
                if (sql[i] == '\'' || sql[i] == '\'' || sql[i] == '\"' || sql[i] == ';') {
                    return true;
                }
            }
        }
        return false;
    }

    public static String gerarPalavraAleatoriamente() {
        String[] vogais = { "a", "e", "i", "o", "u" };
        String[] consoantes = { "b", "c", "d", "f", "g", "h", "nh", "lh", "ch", "j", "k", "l", "m", "n", "p", "qu", "r", "rr", "s", "ss", "t", "v", "w", "x", "y", "z" };
        StringBuilder palavra = new StringBuilder();
        int tamanho_palavra = 4;
        int contar_silabas = 0;
        while (contar_silabas < tamanho_palavra) {
            int vogalEscolhida = (int) (Math.round(Math.random() * 10) / 2);
            while (vogalEscolhida < 0 || vogalEscolhida > vogais.length - 1) {
                vogalEscolhida = (int) (Math.round(Math.random() * 10) / 2);
            }
            String vogal = vogais[vogalEscolhida];
            int consoanteEscolhida = (int) (Math.round(Math.random() * 10 + Math.random() * 10 + Math.random() * 10));
            while (consoanteEscolhida < 0 || consoanteEscolhida > consoantes.length - 1) {
                consoanteEscolhida = (int) (Math.round(Math.random() * 10 + Math.random() * 10 + Math.random() * 10));
            }
            String consoante = consoantes[consoanteEscolhida];
            palavra.append(consoante).append(vogal);
            contar_silabas++;
        }
        return palavra.toString();
    }

    public static boolean validaCampoTexto(String parametro) {
        if (parametro == null || parametro.trim().isEmpty()) {
            return false;
        }
        return true;
    }

    public static String getPrimeiraDataQuestionario(List<Produtor> produtores) {
        Date primeira = new Date();
        List<Visita> visitas;
        for (Produtor p : produtores) {
            visitas = p.getVisitas();
            if (visitas == null) {
                continue;
            }
            for (Visita v : visitas) {
                Date dAux = v.getData();
                if (primeira.after(dAux)) {
                    primeira = dAux;
                }
            }
        }
        return UtilAdrs.treatToString(primeira);
    }

    public static String getUltimaDataQuestionario(List<Produtor> produtores) {
        Date ultima = null;
        List<Visita> visitas;
        for (Produtor p : produtores) {
            visitas = p.getVisitas();
            if (visitas == null) {
                continue;
            }
            for (Visita v : visitas) {
                Date dAux = v.getData();
                if (ultima == null || ultima.before(dAux)) {
                    ultima = dAux;
                }
            }
        }
        return UtilAdrs.treatToString(ultima);
    }
}
