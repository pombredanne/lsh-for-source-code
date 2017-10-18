package recursos;

import java.util.*;
import javax.swing.text.rtf.RTFEditorKit;
import java.text.*;
import java.io.*;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.*;
import javax.servlet.ServletContext;

public class Util {

    static final int TAMANHO_BUFFER = 2048;

    private static DadosCliente dadoscliente = new DadosCliente();

    public static String getData() {
        GregorianCalendar data = new GregorianCalendar();
        int d = data.get(Calendar.DATE);
        int m = data.get(Calendar.MONTH) + 1;
        int a = data.get(Calendar.YEAR);
        String dia = d < 10 ? "0" + d : d + "";
        String mes = m < 10 ? "0" + m : m + "";
        String ano = a + "";
        return dia + "/" + mes + "/" + ano;
    }

    public static String getDia(String data) {
        String resp = "";
        if (data.length() == 10) {
            resp = data.substring(0, 2);
        }
        return resp;
    }

    public static String getMes(String data) {
        String resp = "";
        if (data.length() == 10) {
            resp = data.substring(3, 5);
        }
        return resp;
    }

    public static String getAno(String data) {
        String resp = "";
        if (data.length() == 10) {
            resp = data.substring(6);
        }
        return resp;
    }

    public static GregorianCalendar Hora2GC(String hora) {
        try {
            String divide[] = hora.split(":");
            int h = Integer.parseInt(divide[0]);
            int m = Integer.parseInt(divide[1]);
            return new GregorianCalendar(0, 0, 0, h, m, 0);
        } catch (Exception e) {
            return null;
        }
    }

    public static String getDiaSemana(int dia, int mes, int ano) {
        String resp = "";
        try {
            GregorianCalendar data = new GregorianCalendar(ano, mes - 1, dia);
            int dia_semana = data.get(Calendar.DAY_OF_WEEK);
            switch(dia_semana) {
                case 1:
                    resp = "Domingo";
                    break;
                case 2:
                    resp = "Segunda";
                    break;
                case 3:
                    resp = "Ter�a";
                    break;
                case 4:
                    resp = "Quarta";
                    break;
                case 5:
                    resp = "Quinta";
                    break;
                case 6:
                    resp = "Sexta";
                    break;
                case 7:
                    resp = "S�bado";
                    break;
            }
        } catch (Exception e) {
            resp = "ERRO: " + e.toString();
        }
        return resp;
    }

    public static String getDiaSemana(String data) {
        int dia = Integer.parseInt(Util.getDia(data));
        int mes = Integer.parseInt(Util.getMes(data));
        int ano = Integer.parseInt(Util.getAno(data));
        return getDiaSemana(dia, mes, ano);
    }

    public static String getHora() {
        SimpleTimeZone pdt = new SimpleTimeZone(-3 * 60 * 60 * 1000, "GMT-3:00");
        pdt.setStartRule(Calendar.OCTOBER, 2, Calendar.SUNDAY, 0);
        pdt.setEndRule(Calendar.FEBRUARY, -1, Calendar.SUNDAY, 0);
        GregorianCalendar data = new GregorianCalendar();
        int h = data.get(Calendar.HOUR_OF_DAY);
        int m = data.get(Calendar.MINUTE);
        String hora = h < 10 ? "0" + h : h + "";
        String minuto = m < 10 ? "0" + m : m + "";
        return hora + ":" + minuto;
    }

    public static long getDifTime(String data1, String hora1, String data2, String hora2) {
        long dif = 0;
        if (Util.isNull(data1) || Util.isNull(hora1) || Util.isNull(data2) || Util.isNull(hora2)) {
            return 0;
        }
        try {
            int d1 = Integer.parseInt(getDia(data1));
            int m1 = Integer.parseInt(getMes(data1));
            int a1 = Integer.parseInt(getAno(data1));
            int d2 = Integer.parseInt(getDia(data2));
            int m2 = Integer.parseInt(getMes(data2));
            int a2 = Integer.parseInt(getAno(data2));
            int h1 = Integer.parseInt(hora1.substring(0, 2));
            int mi1 = Integer.parseInt(hora1.substring(3, 5));
            int h2 = Integer.parseInt(hora2.substring(0, 2));
            int mi2 = Integer.parseInt(hora2.substring(3, 5));
            GregorianCalendar g1 = new GregorianCalendar(a1, m1 - 1, d1, h1, mi1);
            GregorianCalendar g2 = new GregorianCalendar(a2, m2 - 1, d2, h2, mi2);
            dif = Math.abs(g2.getTimeInMillis() - g1.getTimeInMillis());
        } catch (NumberFormatException ex) {
            ex.printStackTrace();
        }
        return (dif / 1000) / 60;
    }

    public static long getDifDate(String data1, String data2) {
        long dif = 0;
        if (Util.isNull(data1) || Util.isNull(data2)) {
            return 0;
        }
        try {
            int d1 = Integer.parseInt(getDia(data1));
            int m1 = Integer.parseInt(getMes(data1));
            int a1 = Integer.parseInt(getAno(data1));
            int d2 = Integer.parseInt(getDia(data2));
            int m2 = Integer.parseInt(getMes(data2));
            int a2 = Integer.parseInt(getAno(data2));
            GregorianCalendar g1 = new GregorianCalendar(a1, m1 - 1, d1, 0, 0);
            GregorianCalendar g2 = new GregorianCalendar(a2, m2 - 1, d2, 0, 0);
            dif = g2.getTimeInMillis() - g1.getTimeInMillis();
        } catch (NumberFormatException ex) {
            ex.printStackTrace();
        }
        return (dif / 1000) / 60 / 60 / 24;
    }

    public static String formataNumero(String numero, int digitos) {
        String resp = "";
        for (int i = 1; i <= digitos - numero.length(); i++) {
            resp += "0";
        }
        resp += numero;
        return resp;
    }

    public static String formataDataInvertida(String data) {
        if (data == null) {
            return null;
        }
        if (data.length() >= 10) {
            String dia = data.substring(0, 2);
            String mes = data.substring(3, 5);
            String ano = data.substring(6, 10);
            return ano + "-" + mes + "-" + dia;
        } else {
            return "";
        }
    }

    public static String trataEspacos(String nome) {
        String resp = "";
        char letra;
        for (int i = 0; i < nome.length(); i++) {
            letra = nome.charAt(i);
            if (letra == '\n') {
                resp += "<br>";
            } else if (letra == ' ') {
                resp += "&nbsp;";
            } else {
                resp += letra;
            }
        }
        return resp;
    }

    public static String formataData(String data) {
        if (data != null && data.length() >= 10) {
            String ano = data.substring(0, 4);
            String mes = data.substring(5, 7);
            String dia = data.substring(8, 10);
            return dia + "/" + mes + "/" + ano;
        } else {
            return "";
        }
    }

    public static boolean dataValida(int dia, int mes, int ano) {
        int vetordias[] = { 0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };
        if ((ano % 4 == 0 && ano % 100 != 0) || ano % 400 == 0) {
            vetordias[2] = 29;
        }
        if (dia >= 1 && dia <= vetordias[mes]) {
            return true;
        } else {
            return false;
        }
    }

    public static String formataData(int dia, int mes, int ano) {
        String sdia = "" + dia;
        String smes = "" + mes;
        String sano = "" + ano;
        if (dia < 10) {
            sdia = "0" + sdia;
        }
        if (mes < 10) {
            smes = "0" + smes;
        }
        if (dataValida(dia, mes, ano)) {
            return sdia + "/" + smes + "/" + sano;
        } else {
            return "";
        }
    }

    public static String formataHora(String hora) {
        try {
            if (hora == null) {
                hora = "";
            }
            if (hora.length() > 5) {
                hora = hora.substring(0, 5);
            }
            return hora;
        } catch (Exception e) {
            return e.toString();
        }
    }

    public static String formataHoraHHMMSS(String hora) {
        try {
            if (hora == null) {
                hora = "00:00:00";
            }
            if (hora.length() == 5) {
                hora = hora + ":00";
            }
            return hora;
        } catch (Exception e) {
            return e.toString();
        }
    }

    public static String formatCurrency(String valor) {
        if (isNull(valor)) {
            return "";
        }
        try {
            double d_valor = Double.parseDouble(valor);
            String resp = "";
            NumberFormat moneyFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
            resp = moneyFormat.format(d_valor);
            resp = "R$&nbsp;" + resp.substring(3);
            return resp;
        } catch (Exception e) {
            return "Valor:" + valor + " ERRO: " + e.toString();
        }
    }

    public static String formatCurrencyOld(String valor) {
        String resp = "0";
        if (valor != null && !valor.equals("")) {
            try {
                float lvalor = Float.parseFloat(valor);
                lvalor = (float) (Math.round(lvalor * 100)) / 100;
                valor = lvalor + "";
                resp = valor.replace('.', ',');
                int virgula = resp.indexOf(',');
                String decimal = resp.substring(virgula + 1);
                if (decimal.length() > 2) {
                    resp = resp.substring(0, virgula + 3);
                } else if (decimal.length() < 2) {
                    resp = resp + "0";
                }
                String inteira = resp.substring(0, virgula);
                String aux = "";
                int cont = 0;
                for (int j = inteira.length() - 1; j >= 0; j--) {
                    if (cont % 3 == 0 && cont != 0) {
                        aux = inteira.charAt(j) + "." + aux;
                    } else {
                        aux = inteira.charAt(j) + aux;
                    }
                    cont++;
                }
                resp = inteira;
            } catch (Exception e) {
                resp = e.toString();
            }
        }
        return resp;
    }

    public static String getArquivo(String path) {
        int barra = path.lastIndexOf('\\');
        if (barra != -1) {
            return path.substring(barra + 1);
        } else {
            return path;
        }
    }

    public static int emMinutos(String hora) {
        try {
            int h = Integer.parseInt(hora.substring(0, 2));
            int m = Integer.parseInt(hora.substring(3, 5));
            return (h * 60) + m;
        } catch (Exception e) {
            return 0;
        }
    }

    public static String emHoras(int minutos) {
        int h = minutos / 60;
        int m = minutos % 60;
        String hora, minuto;
        if (h < 10) {
            hora = "0" + h;
        } else {
            hora = "" + h;
        }
        if (m < 10) {
            minuto = "0" + m;
        } else {
            minuto = "" + m;
        }
        return hora + ":" + minuto;
    }

    public static String formataTextoLista(String texto) {
        String resp = "";
        if (texto == null) {
            return "";
        }
        try {
            resp = texto.replace("\t", "&nbsp;&nbsp;&nbsp;&nbsp;");
        } catch (Exception e) {
            resp = "ERRO no Util.formataTextoLista: " + e.toString();
        }
        return resp;
    }

    public static String formataTexto(String texto) {
        if (texto == null) {
            return "";
        }
        try {
            if (texto.length() == 10 && texto.charAt(4) == '-' && texto.charAt(7) == '-') {
                texto = Util.formataData(texto);
                return texto;
            }
            texto = texto.replace('\'', '�');
            return texto;
        } catch (Exception e) {
            return "ERRO: " + e.toString();
        }
    }

    public static String criaPaginacao(String pagina, int numPag, int qtdeporpagina, int total) {
        String paginacao = "";
        int comeco, fim;
        int totalPaginas = total / qtdeporpagina;
        if (totalPaginas * qtdeporpagina < total) {
            totalPaginas++;
        }
        if (numPag <= 5) {
            comeco = 1;
            fim = totalPaginas < 10 ? totalPaginas : 10;
        } else {
            comeco = numPag - 5;
            fim = totalPaginas < numPag + 5 ? totalPaginas : numPag + 5;
        }
        for (int i = comeco; i <= fim; i++) {
            if (i == numPag) {
                paginacao += "<font color='#6F0000'><b>" + i + "</b></font> ";
            } else {
                paginacao += "<a style='text-decoration:underline' title='P�g. " + i + "' href=\"Javascript:navegacao('" + pagina + "'," + i + ")\">" + i + "</a> ";
            }
        }
        if (total == 0) {
            paginacao += " <b>(Nenhum registro encontrado)</b>";
        } else if (total == 1) {
            paginacao += " <b>(" + total + " registro encontrado)</b>";
        } else {
            paginacao += " <b>(" + total + " registros encontrados)</b>";
        }
        paginacao += "<input type='hidden' name='numPag' value='" + numPag + "'>";
        return paginacao;
    }

    public static boolean isNull(String dado) {
        if (dado == null) {
            return true;
        }
        dado = dado.trim();
        if (dado.equals("")) {
            return true;
        }
        if (dado.equalsIgnoreCase("null")) {
            return true;
        }
        return false;
    }

    public static String RTF2HTML(String entrada) {
        StringReader stream;
        if (entrada == null) {
            return "";
        }
        try {
            entrada = entrada.replace('\'', '�');
            stream = new StringReader(entrada);
            RTFEditorKit kit = new RTFEditorKit();
            javax.swing.text.Document doc = kit.createDefaultDocument();
            kit.read(stream, doc, 0);
            return doc.getText(0, doc.getLength());
        } catch (Exception e) {
            return e.toString();
        }
    }

    public static String converteDHWtoPNG(String caminhoupload, String caminhoperl, String caminhoj2sdk, String nomearquivo) {
        try {
            String programa1[] = { caminhoperl + "perl.exe", caminhoupload + "dhw2ps.pl", "--ps", caminhoupload + nomearquivo };
            String programa2[] = { caminhoj2sdk + "java", "-cp", caminhoupload + "digimemo.jar", "DigiPSToPng", caminhoupload, nomearquivo };
            Process p1 = Runtime.getRuntime().exec(programa1);
            Process p2 = Runtime.getRuntime().exec(programa2);
            return "OK";
        } catch (Exception e) {
            return e.toString();
        }
    }

    public static String trim(String str) {
        try {
            if (str == null) {
                return "";
            } else {
                str = str.replace("'", "''");
                return str.trim();
            }
        } catch (Exception e) {
            return "";
        }
    }

    public static String trataNulo(String valor, String novo) {
        if (Util.isNull(valor)) {
            return novo;
        }
        return valor;
    }

    /**
     *    1 - Valor a arredondar.
     *    2 - Quantidade de casas depois da v�rgula.
     *    3 - Arredondar para cima ou para baixo?
     *        Para cima = 0 (ceil)
     *        Para baixo = 1 ou qualquer outro inteiro (floor)
     **/
    public static double arredondar(String valor, int casas, int tipo) {
        if (isNull(valor)) {
            return 0;
        } else {
            return arredondar(Double.parseDouble(valor), casas, tipo);
        }
    }

    public static double arredondar(double valor, int casas, int tipo) {
        double arredondado = valor;
        arredondado *= (Math.pow(10, casas));
        if (tipo == 0) {
            arredondado = Math.ceil(arredondado);
        } else if (tipo == 1) {
            arredondado = Math.floor(arredondado);
        } else {
            arredondado = Math.round(arredondado);
        }
        arredondado /= (Math.pow(10, casas));
        return arredondado;
    }

    public static void criaArquivo(String file, String memo) {
        try {
            FileWriter fw = new FileWriter(file);
            fw.write(memo);
            fw.close();
        } catch (Exception er) {
        }
    }

    private static int getDias(int mes, int ano) {
        int vetorDias[] = { 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };
        if ((ano % 4 == 0 && ano % 100 != 0) || ano % 400 == 0) {
            vetorDias[1] = 29;
        }
        if (mes > 0) {
            return vetorDias[mes - 1];
        } else {
            return vetorDias[11];
        }
    }

    public static String getIdade(String nascimento) {
        return getIdade(nascimento, Util.getData());
    }

    public static String getIdade(String nascimento, String data) {
        if (Util.isNull(nascimento) || nascimento.length() != 10) {
            return "";
        }
        if (Util.isNull(data)) {
            data = Util.getData();
        }
        int anos, meses, dias;
        String Sanos, Smeses, Sdias;
        int dia = Integer.parseInt(nascimento.substring(0, 2));
        int mes = Integer.parseInt(nascimento.substring(3, 5));
        int ano = Integer.parseInt(nascimento.substring(6));
        GregorianCalendar dataRef = toDate(data);
        int diahoje = dataRef.get(Calendar.DATE);
        int meshoje = dataRef.get(Calendar.MONTH) + 1;
        int anohoje = dataRef.get(Calendar.YEAR);
        anos = anohoje - ano;
        if (meshoje < mes) {
            anos--;
            meses = 12 - (mes - meshoje);
            if (diahoje < dia) {
                meses--;
                dias = (getDias(meshoje - 1, anohoje) - dia) + diahoje;
            } else {
                dias = diahoje - dia;
            }
        } else if (meshoje == mes) {
            if (diahoje < dia) {
                anos--;
                meses = 11;
                dias = getDias(meshoje - 1, anohoje) - dia + diahoje;
            } else {
                meses = 0;
                dias = diahoje - dia;
            }
        } else {
            meses = meshoje - mes;
            if (diahoje < dia) {
                meses--;
                dias = getDias(meshoje - 1, meshoje) - dia + diahoje;
            } else {
                dias = diahoje - dia;
            }
        }
        if (anos == 0) {
            Sanos = "";
        } else {
            Sanos = anos + "a ";
        }
        if (meses == 0) {
            Smeses = "";
        } else {
            Smeses = meses + "m ";
        }
        if (dias == 0) {
            Sdias = "";
        } else {
            Sdias = dias + "d ";
        }
        return (Sanos + Smeses + Sdias);
    }

    public static void compactar(String arqEntrada, String arqSaida) {
        int i, cont;
        byte[] dados = new byte[TAMANHO_BUFFER];
        File f = null;
        BufferedInputStream origem = null;
        FileInputStream streamDeEntrada = null;
        BufferedOutputStream buffer = null;
        FileOutputStream destino = null;
        ZipOutputStream saida = null;
        ZipEntry entry = null;
        try {
            destino = new FileOutputStream(arqSaida);
            buffer = new BufferedOutputStream(destino);
            saida = new ZipOutputStream(buffer);
            File arquivo = new File(arqEntrada);
            if (arquivo.isFile() && !(arquivo.getName()).equals(arqSaida)) {
                System.out.println("Compactando: " + arquivo.getName());
                streamDeEntrada = new FileInputStream(arquivo);
                origem = new BufferedInputStream(streamDeEntrada, TAMANHO_BUFFER);
                entry = new ZipEntry(arquivo.getName());
                saida.putNextEntry(entry);
                while ((cont = origem.read(dados, 0, TAMANHO_BUFFER)) != -1) {
                    saida.write(dados, 0, cont);
                }
                origem.close();
            }
            saida.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String soNumeros(String valor) {
        String resp = "";
        char ch;
        for (int i = 0; i < valor.length(); i++) {
            ch = valor.charAt(i);
            if (ch == '1' || ch == '2' || ch == '3' || ch == '4' || ch == '5' || ch == '6' || ch == '7' || ch == '8' || ch == '9' || ch == '0') {
                resp += ch;
            }
        }
        return resp;
    }

    /**
     * Calcula carimbo MD5 sobre um string dado.
     * @param pBase O string base.
     * @param pCharSet O character set no qual o string base deve ser considerado
     *        valor fixo "ISO8859_1"
     * @return O carimbo MD5, na forma de um string.
     */
    public static String digest(String pBase, String pCharSet) {
        String wdgs = null;
        try {
            MessageDigest wmd = MessageDigest.getInstance("MD5");
            wmd.reset();
            wmd.update(pBase.getBytes(pCharSet));
            byte[] wdg = wmd.digest();
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < wdg.length; i++) {
                String w_dup = Integer.toHexString(0xFF & wdg[i]);
                if (w_dup.length() < 2) {
                    w_dup = "0" + w_dup;
                }
                hexString.append(w_dup);
            }
            wdgs = hexString.toString();
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        } finally {
            return wdgs;
        }
    }

    public static String getStatusPage(String strurl) {
        String resp = "";
        try {
            URL url = new URL(strurl);
            resp = url.getContent().toString();
            return "OK";
        } catch (MalformedURLException e) {
            resp = e.toString();
        } catch (IOException e) {
            resp = e.toString();
        } catch (Exception e) {
            resp = e.toString();
        }
        return resp;
    }

    public static String getPagina(String strurl) {
        String resp = "";
        Authenticator.setDefault(new Autenticador());
        try {
            URL url = new URL(strurl);
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String str;
            while ((str = in.readLine()) != null) {
                resp += str;
            }
            in.close();
        } catch (MalformedURLException e) {
            resp = e.toString();
        } catch (IOException e) {
            resp = e.toString();
        } catch (Exception e) {
            resp = e.toString();
        }
        return resp;
    }

    public static boolean existeArquivo(String path) {
        String baseFolder = Propriedade.getCampo("baseFolder");
        File f = new File(baseFolder + "//" + path);
        return f.exists();
    }

    public static String[] verificaAtualizacoes() {
        String resp[] = new String[2];
        try {
            String atual[] = new Atualizacao().getUltimaVersao();
            String dados = Util.getPagina("http://www.katusis.com.br/buscaatualizacoes.asp?v=" + atual[0]);
            resp = dados.split("#");
            if (resp.length == 1) {
                resp = new String[2];
                resp[0] = "no";
                resp[1] = "no";
            }
        } catch (Exception e) {
            resp[0] = "ERRO: " + e.toString();
        }
        return resp;
    }

    public static boolean terminaCom(String texto, String termino) {
        String fim = texto.substring(texto.length() - termino.length(), texto.length());
        if (fim.equalsIgnoreCase(termino)) {
            return true;
        } else {
            return false;
        }
    }

    public static String vetorToString(String vetor[]) {
        String resp = "";
        int i = 0;
        if (vetor == null || vetor.length == 0) {
            return resp;
        }
        for (i = 0; i < vetor.length - 1; i++) {
            resp += vetor[i] + ", ";
        }
        resp += vetor[i];
        return resp;
    }

    public static void insereUsuario(String cod_usuario, ServletContext app) {
        boolean achou = false;
        Vector lista = (Vector) app.getAttribute("codigos");
        if (lista == null) {
            lista = new Vector();
        }
        for (int i = 0; i < lista.size(); i++) {
            if (cod_usuario.equals((String) lista.get(i))) {
                achou = true;
            }
        }
        if (!achou) {
            lista.add(cod_usuario);
        }
        app.setAttribute("codigos", lista);
    }

    public static void removeUsuario(String cod_usuario, ServletContext app) {
        Vector lista = (Vector) app.getAttribute("codigos");
        if (lista != null) {
            lista.removeElement(cod_usuario);
        }
        app.setAttribute("codigos", lista);
    }

    public static String getUF(String uf) {
        String resp = "";
        String ufs[] = { "", "AC", "AL", "AM", "AP", "BA", "CE", "DF", "ES", "GO", "MA", "MG", "MS", "MT", "PA", "PB", "PE", "PI", "PR", "RJ", "RN", "RO", "RR", "RS", "SC", "SE", "SP", "TO" };
        int i = 0;
        resp = "<select name='uf' id='uf' class='caixa'>\n";
        for (i = 0; i < ufs.length; i++) {
            if (!Util.isNull(uf) && uf.equals(ufs[i])) {
                resp += "<option value='" + ufs[i] + "' selected>" + ufs[i] + "</option>\n";
            } else {
                resp += "<option value='" + ufs[i] + "'>" + ufs[i] + "</option>\n";
            }
        }
        resp += "</select>\n";
        return resp;
    }

    public static boolean pertence(String valor, Object vetor[]) {
        boolean resp = false;
        if (Util.isNull(valor) || vetor == null) {
            return resp;
        }
        valor = valor.trim();
        for (int i = 0; i < vetor.length; i++) {
            String valorVetor = vetor[i].toString().trim();
            if (valor.equalsIgnoreCase(valorVetor)) {
                resp = true;
                i = vetor.length;
            }
        }
        return resp;
    }

    public static String Vector2String(Vector vetor) {
        String resp = "";
        if (vetor == null) {
            return "";
        }
        for (int i = 0; i < vetor.size() - 1; i++) {
            resp += vetor.get(i).toString() + ",";
        }
        if (vetor.size() > 0) {
            resp += vetor.get(vetor.size() - 1).toString();
        }
        return resp;
    }

    public static String freeRTE_Preload(String content) {
        String resp = content;
        resp = resp.replace("\n", " ");
        resp = resp.replace("\r", " ");
        resp = resp.replace("\t", " ");
        resp = resp.replace("'", "\"");
        return resp;
    }

    public static GregorianCalendar toTime(String hora, int dia, int mes, int ano) {
        int h, m, s;
        GregorianCalendar resp;
        try {
            h = Integer.parseInt(hora.substring(0, 2));
            m = Integer.parseInt(hora.substring(3, 5));
            resp = new GregorianCalendar(ano, mes - 1, dia, h, m);
            return resp;
        } catch (Exception e) {
            return null;
        }
    }

    public static String toString(GregorianCalendar data) {
        int dia, mes, ano;
        String sd, sm, sa;
        try {
            dia = data.get(Calendar.DATE);
            mes = data.get(Calendar.MONTH) + 1;
            ano = data.get(Calendar.YEAR);
            sd = (dia < 10) ? "0" + dia : "" + dia;
            sm = (mes < 10) ? "0" + mes : "" + mes;
            sa = "" + ano;
            return sd + "/" + sm + "/" + sa;
        } catch (Exception e) {
            return "Erro na convers�o para String: " + e.toString();
        }
    }

    public static GregorianCalendar toDate(String data) {
        if (Util.isNull(data) || data.length() != 10) {
            return null;
        }
        try {
            int dia = Integer.parseInt(data.substring(0, 2));
            int mes = Integer.parseInt(data.substring(3, 5));
            int ano = Integer.parseInt(data.substring(6));
            GregorianCalendar gc = new GregorianCalendar(ano, mes - 1, dia);
            return gc;
        } catch (Exception e) {
            return null;
        }
    }

    public static String addDias(String data, int dias) {
        int dia, mes, ano;
        String resp;
        dia = Integer.parseInt(data.substring(0, 2));
        mes = Integer.parseInt(data.substring(3, 5));
        ano = Integer.parseInt(data.substring(6));
        GregorianCalendar gc = new GregorianCalendar(ano, mes - 1, dia + dias - 1);
        resp = toString(gc);
        return resp;
    }

    public static String trataValorNulo(String valor) {
        if (Util.isNull(valor)) {
            return "null";
        } else {
            return "'" + valor + "'";
        }
    }

    public static String cortaString(String str, int limite) {
        String resp = "";
        if (Util.isNull(str)) {
            return "";
        }
        if (str.length() > limite) {
            resp = str.substring(0, limite);
        } else {
            resp = str;
        }
        return resp;
    }

    public static String getBotoes(String pagina, String pesq, int tipo) {
        return getBotoes(pagina, pesq, tipo, false);
    }

    public static String getBotoes(String pagina, String pesq, int tipo, boolean imprime) {
        String resp = "";
        try {
            Banco banco = new Banco();
            resp += "<tr align='center' valign='top'>\n";
            resp += " <td width='100%'>\n";
            resp += "  <table width='100%' border='0' cellpadding='0' cellspacing='0' class='table'>\n";
            resp += "   <tr>\n";
            resp += "    <td class='tdMedium' style='text-align:center'>\n";
            resp += "      	<button type='button' name='novo' id='novo' class='botao' style='width:70px' onClick='clickBotaoNovo()'><img src='images/16.gif' height='17'>&nbsp;&nbsp;&nbsp;&nbsp;Novo</button>\n";
            resp += "    </td>\n";
            resp += "    <td class='tdMedium' style='text-align:center'>\n";
            resp += "     	<button type='button' name='salvar' id='salvar' class='botao' style='width:70px' onClick='clickBotaoSalvar()'><img src='images/gravamini.gif' height='17'>&nbsp;&nbsp;&nbsp;&nbsp;Salvar</button>\n";
            resp += "    </td>\n";
            resp += "    <td class='tdMedium' style='text-align:center'>\n";
            resp += "           <button type='button' name='exclui' id='exclui' class='botao' style='width:70px' onClick='clickBotaoExcluir()'><img src='images/delete.gif' width='13' height='17'>&nbsp;&nbsp;&nbsp;&nbsp;Excluir</button>\n";
            resp += "     </td>\n";
            if (imprime) {
                resp += "<td class='tdMedium' style='text-align:center'><a href='Javascript:imprime()' title='Imprime Ficha Modelo'><img src='images/print.gif' border='0'></a></td>";
            }
            resp += "     <td class='tdMedium' style='text-align:left'>\n";
            resp += "	<input type='text' name='pesq' id='pesq' class='caixa' value='" + pesq + "' style='width:" + ((imprime) ? "90" : "150") + "px' onKeyPress=\"buscarEnter(event, '" + pagina + "');\">&nbsp;\n";
            resp += "           <select name='tipo' id='tipo' class='caixa'>\n";
            resp += "		<option value='1'" + banco.getSel(tipo, 1) + ">Exata</option>\n";
            resp += "		<option value='2'" + banco.getSel(tipo, 2) + ">In�cio</option>\n";
            resp += "		<option value='3'" + banco.getSel(tipo, 3) + ">Meio</option>\n";
            resp += "	    </select>\n";
            resp += "	<button type='button' class='botao' style='width:80px' onClick=\"buscar('" + pagina + "');\">";
            resp += "       <img src='images/busca.gif' height='17'>&nbsp;Consultar</button></td>\n";
            resp += "   </tr>\n";
            resp += "  </table>\n";
            resp += " </td>\n";
            resp += "</tr>\n";
        } catch (Exception e) {
            resp = "ERRO: " + e.toString();
        }
        return resp;
    }

    public static String htmltotext(String texto) {
        try {
            Pattern pDel = Pattern.compile("(?s)<.*?>");
            Pattern pText = Pattern.compile("(.*)");
            Matcher m;
            if (texto != null) {
                m = pDel.matcher(texto);
                texto = m.replaceAll(" ");
                texto = texto.trim();
                m = pText.matcher(texto);
                if (m.matches()) {
                    texto = m.group(1);
                    texto = texto.trim();
                }
                return texto;
            }
            return texto;
        } catch (Exception e) {
            return "ERRO no htmltotext: " + e.toString();
        }
    }

    public static boolean primeiraVez(String codcli, String data, boolean importaseveio) {
        boolean resp = true;
        String sql = "SELECT agendamento_id FROM agendamento ";
        sql += "WHERE ativo='S' AND codcli=" + codcli;
        sql += " AND data < '" + formataDataInvertida(data) + "' ";
        if (importaseveio) {
            sql += "AND status <> 3";
        }
        String aux = new Banco().getValor("agendamento_id", sql);
        if (Util.isNull(aux)) {
            resp = true;
        } else {
            resp = false;
        }
        return resp;
    }

    public static String tiraAcento(String palavra) {
        if (Util.isNull(palavra)) return "";
        String resp = palavra.toLowerCase();
        resp = resp.replace("�", "a");
        resp = resp.replace("�", "a");
        resp = resp.replace("�", "a");
        resp = resp.replace("�", "a");
        resp = resp.replace("�", "a");
        resp = resp.replace("�", "e");
        resp = resp.replace("�", "e");
        resp = resp.replace("�", "e");
        resp = resp.replace("�", "e");
        resp = resp.replace("�", "i");
        resp = resp.replace("�", "i");
        resp = resp.replace("�", "i");
        resp = resp.replace("�", "i");
        resp = resp.replace("�", "o");
        resp = resp.replace("�", "o");
        resp = resp.replace("�", "o");
        resp = resp.replace("�", "o");
        resp = resp.replace("�", "o");
        resp = resp.replace("�", "u");
        resp = resp.replace("�", "u");
        resp = resp.replace("�", "u");
        resp = resp.replace("�", "u");
        resp = resp.replace("�", "c");
        resp = resp.replace("�", "n");
        return resp;
    }

    public static String removeCaracteres(String palavra) {
        String resp = "";
        String chvalidos = "abcdefghijklmnopqrstuvwxyz1234567890 .,-+:/";
        if (Util.isNull(palavra)) return "";
        palavra = palavra.toLowerCase();
        for (int i = 0; i < palavra.length(); i++) {
            if (chvalidos.indexOf(palavra.charAt(i)) >= 0) resp += palavra.charAt(i);
        }
        return resp;
    }

    public static String trataTermo(String termo) {
        if (Util.isNull(termo)) return "";
        termo = tiraAcento(termo);
        termo = removeCaracteres(termo);
        termo = termo.trim();
        termo = termo.toUpperCase();
        return termo;
    }

    public static String formataDataHora(String datahora) {
        String resp = "";
        if (Util.isNull(datahora)) return "";
        try {
            resp = Util.formataData(datahora.substring(0, 10));
            resp += " " + datahora.substring(11);
        } catch (Exception e) {
            resp = "ERRO: " + e.toString();
        }
        return resp;
    }

    public static float emMinutos(GregorianCalendar hora) {
        int h, m;
        h = hora.get(Calendar.HOUR_OF_DAY);
        m = hora.get(Calendar.MINUTE);
        return (h * 60) + m;
    }

    public static GregorianCalendar addMinutos(GregorianCalendar data, int minutos) {
        int h, m, dia, mes, ano;
        GregorianCalendar resp;
        dia = data.get(Calendar.DATE);
        mes = data.get(Calendar.MONTH);
        ano = data.get(Calendar.YEAR);
        h = data.get(Calendar.HOUR_OF_DAY);
        m = data.get(Calendar.MINUTE);
        resp = new GregorianCalendar(ano, mes, dia, h, m + minutos);
        return resp;
    }

    public static String GC2HHMM(GregorianCalendar hora) {
        int h, m;
        String resp, hs = "", ms = "";
        try {
            h = hora.get(Calendar.HOUR_OF_DAY);
            m = hora.get(Calendar.MINUTE);
            if (h < 10) {
                hs = "0";
            }
            if (m < 10) {
                ms = "0";
            }
            resp = hs + h + ":" + ms + m;
            return resp;
        } catch (Exception e) {
            return "Erro na convers�o para String: " + e.toString();
        }
    }

    public static String getValorMoney(String valor) {
        String resp = valor;
        if (Util.isNull(valor)) return "0";
        if (resp.indexOf(".") >= 0 && resp.indexOf(",") >= 0 && resp.indexOf(".") < resp.indexOf(",")) {
            resp = resp.replace(".", "");
            resp = resp.replace(",", ".");
        } else if (resp.indexOf(".") >= 0 && resp.indexOf(",") >= 0 && resp.indexOf(".") > resp.indexOf(",")) {
            resp = resp.replace(",", "");
        } else if (resp.indexOf(",") >= 0) {
            resp = resp.replace(",", ".");
        }
        return resp;
    }
}
