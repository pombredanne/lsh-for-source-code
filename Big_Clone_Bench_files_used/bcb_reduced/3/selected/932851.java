package br.com.hs.nfe.common.util;

import br.com.hs.nfe.common.exception.HSCommonException;
import br.com.hs.nfe.common.to.ChaveAcessoTO;
import br.com.hs.nfe.common.to.NotaFiscalTO;
import com.sun.xml.internal.bind.v2.util.FatalAdapter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Calendar;

/**
 * Classe responsável por tratar a chave de acesso da NF-e.
 * @author Ranlive Hrysyk
 */
public class ChaveAcessoHelper {

    private static final DecimalFormat DIGITO_FORMAT = new DecimalFormat();

    private static final DecimalFormat DATA_MODELO_FORMAT = new DecimalFormat();

    private static final DecimalFormat SERIE_FORMAT = new DecimalFormat();

    private static final DecimalFormat CODIGO_NUMERICO_FORMAT = new DecimalFormat();

    private static final DecimalFormat NUMERO_NFE_FORMAT = new DecimalFormat();

    private static final DecimalFormat TIPO_EMISSAO_FORMAT = new DecimalFormat();

    private static final DecimalFormat DOCUMENTO_FORMAT = new DecimalFormat();

    private static final int TAMANHO_CHAVE = 44;

    private static int[] CODIGO_NUMERICO_ARRAY = { 3, 2, 2, 2, 2, 2, 2, 3 };

    private static final String ALGORITMO_DIGEST = "SHA-1";

    static {
        DIGITO_FORMAT.applyPattern("0");
        DATA_MODELO_FORMAT.applyPattern("00");
        SERIE_FORMAT.applyPattern("000");
        CODIGO_NUMERICO_FORMAT.applyPattern("00000000");
        NUMERO_NFE_FORMAT.applyPattern("000000000");
        DOCUMENTO_FORMAT.applyPattern("00000000000000");
        TIPO_EMISSAO_FORMAT.applyPattern("0");
    }

    /**
     * Cria o objeto ChaveAcessoTO pela String do número da chave acesso.
     */
    public static ChaveAcessoTO createChaveAcessoTO(String chaveAcesso) throws HSCommonException {
        if (chaveAcesso == null) {
            ParseException e = new ParseException(HSMessageConstants.MSG_CHAVE_ACESSO_NULL, 0);
            throw new HSCommonException(e.getMessage(), e);
        }
        if (chaveAcesso.trim().length() != TAMANHO_CHAVE) {
            ParseException e = new ParseException(MessageFormat.format(HSMessageConstants.MSG_CHAVE_ACESSO_DIGITS, new Object[] { chaveAcesso.trim().length() }), 0);
            throw new HSCommonException(e.getMessage(), e);
        }
        ChaveAcessoTO chaveAcessoTO = new ChaveAcessoTO();
        try {
            chaveAcessoTO.setUFEmitente(chaveAcesso.substring(0, 2));
            chaveAcessoTO.setAno(chaveAcesso.substring(2, 4));
            chaveAcessoTO.setMes(chaveAcesso.substring(4, 6));
            chaveAcessoTO.setCNPJEmitente(chaveAcesso.substring(6, 20));
            chaveAcessoTO.setModelo(chaveAcesso.substring(20, 22));
            chaveAcessoTO.setSerie(chaveAcesso.substring(22, 25));
            chaveAcessoTO.setNumeroNFe(chaveAcesso.substring(25, 34));
            chaveAcessoTO.setTipoEmissao(chaveAcesso.substring(34, 35));
            chaveAcessoTO.setCodigoNumerico(chaveAcesso.substring(35, 43));
            chaveAcessoTO.setDigito(chaveAcesso.substring(43, 44));
        } catch (NumberFormatException nfe) {
            ParseException e = new ParseException(HSMessageConstants.MSG_CHAVE_ACESSO_NUMBER, 0);
            throw new HSCommonException(e.getMessage(), e);
        }
        return chaveAcessoTO;
    }

    /**
     * Cria o objeto ChaveAcessoTO.
     */
    public static ChaveAcessoTO createChaveAcessoTO(NotaFiscalTO notaFiscalTO) throws ParseException {
        if (notaFiscalTO == null) {
            throw new ParseException(HSMessageConstants.MSG_OBJECT_NULL, 0);
        }
        ChaveAcessoTO chaveAcesso = new ChaveAcessoTO();
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(notaFiscalTO.getDataEmissao());
            chaveAcesso.setUFEmitente(notaFiscalTO.getCUF());
            chaveAcesso.setAno(DateHelper.formatarDataHora(notaFiscalTO.getDEmi(), DateHelper.DATE_YEAR_FORMAT));
            chaveAcesso.setMes(DateHelper.formatarDataHora(notaFiscalTO.getDEmi(), DateHelper.DATE_MONTH_FORMAT));
            chaveAcesso.setCNPJEmitente(notaFiscalTO.getEmitenteTO().getCNPJ());
            chaveAcesso.setModelo(notaFiscalTO.getModelo());
            chaveAcesso.setSerie(notaFiscalTO.getSerie());
            chaveAcesso.setNumeroNFe(notaFiscalTO.getNumero());
            chaveAcesso.setCodigoNumerico(notaFiscalTO.getCodigoNumericoChaveAcesso());
            chaveAcesso.setTipoEmissao(notaFiscalTO.getTipoEmissao().getCodigo());
            if (StringHelper.isBlankOrNull(notaFiscalTO.getDigitoVerificador())) {
                notaFiscalTO.setDigitoVerificador(gerarDigitoChaveAcesso(notaFiscalTO));
            }
            chaveAcesso.setDigito(notaFiscalTO.getDigitoVerificador());
        } catch (NumberFormatException nfe) {
            throw new ParseException(HSMessageConstants.MSG_CHAVE_ACESSO_NUMBER, 0);
        }
        return chaveAcesso;
    }

    /**
     * Obtém um String com a chave de acesso.
     */
    public static String gerarChaveAcesso(ChaveAcessoTO chaveAcessoTO) {
        return gerarChaveAcesso(chaveAcessoTO.getUFEmitente(), chaveAcessoTO.getAno(), chaveAcessoTO.getMes(), chaveAcessoTO.getCNPJEmitente(), chaveAcessoTO.getModelo(), chaveAcessoTO.getSerie(), chaveAcessoTO.getNumeroNFe(), chaveAcessoTO.getTipoEmissao(), chaveAcessoTO.getCodigoNumerico(), chaveAcessoTO.getDigito());
    }

    /**
     * Valida o digito da chave de acesso.
     */
    public static boolean verificarDigitoChaveAcesso(String chaveAcesso) {
        if ((chaveAcesso == null) || (chaveAcesso.length() != 44)) {
            return false;
        }
        String digito = gerarDigitoChaveAcesso(chaveAcesso);
        return digito.equals(chaveAcesso.substring(43));
    }

    /**
     * Cria a concatenação de String da chave de acesso com o digito.
     */
    public static String gerarChaveAcesso(String ufEmitente, String anoEmissao, String mesEmissao, String documentoEmitente, String modelo, String serie, String numero, String tipoEmissao, String codigoNumerico, String digito) {
        StringBuilder sb = new StringBuilder();
        sb.append(DATA_MODELO_FORMAT.format(Integer.parseInt(ufEmitente))).append(DATA_MODELO_FORMAT.format(Integer.parseInt(anoEmissao))).append(DATA_MODELO_FORMAT.format(Integer.parseInt(mesEmissao))).append(DOCUMENTO_FORMAT.format(Long.parseLong(documentoEmitente))).append(DATA_MODELO_FORMAT.format(Integer.parseInt(modelo))).append(SERIE_FORMAT.format(Integer.parseInt(serie))).append(NUMERO_NFE_FORMAT.format(Integer.parseInt(numero))).append(TIPO_EMISSAO_FORMAT.format(Integer.parseInt(tipoEmissao)));
        if (StringHelper.isNumeric(codigoNumerico)) {
            sb.append(CODIGO_NUMERICO_FORMAT.format(Integer.parseInt(codigoNumerico)));
        }
        if (StringHelper.isNumeric(digito)) {
            sb.append(DIGITO_FORMAT.format(Integer.parseInt(digito)));
        }
        return sb.toString();
    }

    /**
     * Cria a concatenação de String da chave de acesso sem o dígito.
     */
    public static String gerarChaveAcesso(String ufEmitente, String anoEmissao, String mesEmissao, String documentoEmitente, String modelo, String serie, String numero, String tipoEmissao, String codigoNumerico) {
        return new StringBuilder().append(DATA_MODELO_FORMAT.format(Integer.parseInt(ufEmitente))).append(DATA_MODELO_FORMAT.format(Integer.parseInt(anoEmissao))).append(DATA_MODELO_FORMAT.format(Integer.parseInt(mesEmissao))).append(DOCUMENTO_FORMAT.format(Long.parseLong(documentoEmitente))).append(DATA_MODELO_FORMAT.format(Integer.parseInt(modelo))).append(SERIE_FORMAT.format(Integer.parseInt(serie))).append(NUMERO_NFE_FORMAT.format(Integer.parseInt(numero))).append(TIPO_EMISSAO_FORMAT.format(Integer.parseInt(tipoEmissao))).append(CODIGO_NUMERICO_FORMAT.format(Integer.parseInt(codigoNumerico))).toString();
    }

    /**
     * Calcula o digito da chave de acesso.
     */
    public static String gerarDigitoChaveAcesso(String chaveAcesso) {
        String nfe = StringHelper.getDigits(chaveAcesso);
        int primeiro_digito = 0;
        int calculo = Integer.parseInt(nfe.substring(0, 1)) * 4 + Integer.parseInt(nfe.substring(1, 2)) * 3 + Integer.parseInt(nfe.substring(2, 3)) * 2 + Integer.parseInt(nfe.substring(3, 4)) * 9 + Integer.parseInt(nfe.substring(4, 5)) * 8 + Integer.parseInt(nfe.substring(5, 6)) * 7 + Integer.parseInt(nfe.substring(6, 7)) * 6 + Integer.parseInt(nfe.substring(7, 8)) * 5 + Integer.parseInt(nfe.substring(8, 9)) * 4 + Integer.parseInt(nfe.substring(9, 10)) * 3 + Integer.parseInt(nfe.substring(10, 11)) * 2 + Integer.parseInt(nfe.substring(11, 12)) * 9 + Integer.parseInt(nfe.substring(12, 13)) * 8 + Integer.parseInt(nfe.substring(13, 14)) * 7 + Integer.parseInt(nfe.substring(14, 15)) * 6 + Integer.parseInt(nfe.substring(15, 16)) * 5 + Integer.parseInt(nfe.substring(16, 17)) * 4 + Integer.parseInt(nfe.substring(17, 18)) * 3 + Integer.parseInt(nfe.substring(18, 19)) * 2 + Integer.parseInt(nfe.substring(19, 20)) * 9 + Integer.parseInt(nfe.substring(20, 21)) * 8 + Integer.parseInt(nfe.substring(21, 22)) * 7 + Integer.parseInt(nfe.substring(22, 23)) * 6 + Integer.parseInt(nfe.substring(23, 24)) * 5 + Integer.parseInt(nfe.substring(24, 25)) * 4 + Integer.parseInt(nfe.substring(25, 26)) * 3 + Integer.parseInt(nfe.substring(26, 27)) * 2 + Integer.parseInt(nfe.substring(27, 28)) * 9 + Integer.parseInt(nfe.substring(28, 29)) * 8 + Integer.parseInt(nfe.substring(29, 30)) * 7 + Integer.parseInt(nfe.substring(30, 31)) * 6 + Integer.parseInt(nfe.substring(31, 32)) * 5 + Integer.parseInt(nfe.substring(32, 33)) * 4 + Integer.parseInt(nfe.substring(33, 34)) * 3 + Integer.parseInt(nfe.substring(34, 35)) * 2 + Integer.parseInt(nfe.substring(35, 36)) * 9 + Integer.parseInt(nfe.substring(36, 37)) * 8 + Integer.parseInt(nfe.substring(37, 38)) * 7 + Integer.parseInt(nfe.substring(38, 39)) * 6 + Integer.parseInt(nfe.substring(39, 40)) * 5 + Integer.parseInt(nfe.substring(40, 41)) * 4 + Integer.parseInt(nfe.substring(41, 42)) * 3 + Integer.parseInt(nfe.substring(42, 43)) * 2;
        if (calculo < 11) {
            primeiro_digito = 11 - calculo;
        } else if (calculo % 11 <= 1) {
            primeiro_digito = 0;
        } else {
            primeiro_digito = 11 - calculo % 11;
        }
        return String.valueOf(primeiro_digito);
    }

    /**
     * Calcula um código numérico aleatório.
     */
    public static String gerarCodigoNumerico(String xml) {
        MessageDigest sha = null;
        try {
            sha = MessageDigest.getInstance(ALGORITMO_DIGEST);
        } catch (NoSuchAlgorithmException e) {
        }
        byte[] nfeHash = sha.digest(((String) XMLHelper.getTagConteudo(xml, HSConstants.ID_NFE, false).get(0)).getBytes());
        long codigoNumerico = 0L;
        int hashIndex = 0;
        for (int i = 0; i < CODIGO_NUMERICO_ARRAY.length; i++) {
            byte[] algarismoBytes = Arrays.copyOfRange(nfeHash, hashIndex, hashIndex + CODIGO_NUMERICO_ARRAY[i]);
            int somaBytes = somarBytes(algarismoBytes);
            int algarismo = somarInteiro(somaBytes);
            codigoNumerico = (long) ((double) codigoNumerico + (double) algarismo * Math.pow(10.0D, i));
            hashIndex += CODIGO_NUMERICO_ARRAY[i];
        }
        return CODIGO_NUMERICO_FORMAT.format(codigoNumerico);
    }

    /**
     * Soma inteiros.
     */
    private static int somarInteiro(int numero) {
        int numeroAtual = numero;
        int somaAtual = 0;
        while (numeroAtual > 0) {
            somaAtual += numeroAtual % 10;
            numeroAtual /= 10;
        }
        if (somaAtual / 10 > 0) {
            return somarInteiro(somaAtual);
        }
        return somaAtual;
    }

    /**
     * Soma Byte Array.
     */
    private static int somarBytes(byte[] bytes) {
        int soma = 0;
        for (int i = 0; i < bytes.length; i++) {
            soma += bytes[i];
        }
        return soma;
    }

    /**
     * Obtém um String com o valor formatado da chave de acesso.
     */
    public static String formatarChaveAcesso(ChaveAcessoTO chaveAcessoTO) {
        StringBuilder chaveAcesso = new StringBuilder();
        chaveAcesso.append(DATA_MODELO_FORMAT.format(Integer.parseInt(chaveAcessoTO.getUFEmitente())));
        chaveAcesso.append("-");
        chaveAcesso.append(DATA_MODELO_FORMAT.format(Integer.parseInt(chaveAcessoTO.getAno())));
        chaveAcesso.append("/");
        chaveAcesso.append(DATA_MODELO_FORMAT.format(Integer.parseInt(chaveAcessoTO.getMes())));
        chaveAcesso.append("-");
        chaveAcesso.append(StringHelper.cnpjFormat(chaveAcessoTO.getCNPJEmitente()));
        chaveAcesso.append("-");
        chaveAcesso.append(DATA_MODELO_FORMAT.format(Integer.parseInt(chaveAcessoTO.getModelo())));
        chaveAcesso.append("-");
        chaveAcesso.append(SERIE_FORMAT.format(Integer.parseInt(chaveAcessoTO.getSerie())));
        chaveAcesso.append("-");
        chaveAcesso.append(StringHelper.formatarNotaFiscal(chaveAcessoTO.getNumeroNFe()));
        chaveAcesso.append("-");
        chaveAcesso.append(chaveAcessoTO.getTipoEmissao());
        chaveAcesso.append("-");
        chaveAcesso.append(formatarNota(chaveAcessoTO.getCodigoNumerico()));
        chaveAcesso.append("-").append(chaveAcessoTO.getDigito());
        return chaveAcesso.toString();
    }

    /**
     * Obtém uma String com o número da nota formatado
     */
    private static String formatarNota(String codNum) {
        StringBuilder sb = new StringBuilder();
        if (!StringHelper.isBlankOrNull(codNum)) {
            String notaCompleto = StringHelper.completarComZerosAEsquerda(codNum, 8);
            sb.append(notaCompleto.substring(0, 2)).append(".");
            sb.append(notaCompleto.substring(2, 5)).append(".");
            sb.append(notaCompleto.substring(5, 8));
        }
        return sb.toString();
    }

    /**
     * Obtém uma String com o número da chave de acesso em quartetos.
     */
    public static String formatarChaveAcesso4x4(String chave) {
        if (!StringHelper.isBlankOrNull(chave)) {
            if (chave.length() == 44) {
                chave = chave.substring(0, 4) + " " + chave.substring(4, 8) + " " + chave.substring(8, 12) + " " + chave.substring(12, 16) + " " + chave.substring(16, 20) + " " + chave.substring(20, 24) + " " + chave.substring(24, 28) + " " + chave.substring(28, 32) + " " + chave.substring(32, 36) + " " + chave.substring(36, 40) + " " + chave.substring(40, 44);
                return chave;
            }
            return chave;
        }
        return "";
    }

    /**
     * Obtém a String da chave de acesso.
     */
    public static String gerarChaveAcesso(NotaFiscalTO notaFiscalTO) {
        if (StringHelper.isNumeric(notaFiscalTO.getCodigoNumericoChaveAcesso())) {
            String dv = gerarDigitoChaveAcesso(notaFiscalTO);
            notaFiscalTO.setDigitoVerificador(dv);
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(notaFiscalTO.getDataEmissao());
        return gerarChaveAcesso(notaFiscalTO.getCUF(), DATA_MODELO_FORMAT.format(calendar.get(1) % 100), DATA_MODELO_FORMAT.format(calendar.get(2) + 1), notaFiscalTO.getEmitenteTO().getCNPJ(), notaFiscalTO.getModelo(), notaFiscalTO.getSerie(), notaFiscalTO.getNumero(), notaFiscalTO.getTipoEmissao().getCodigo(), notaFiscalTO.getCodigoNumericoChaveAcesso(), notaFiscalTO.getDigitoVerificador());
    }

    /**
     * Calcula o digito verificado da chave de acesso.
     */
    public static String gerarDigitoChaveAcesso(NotaFiscalTO notaFiscalTO) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(notaFiscalTO.getDataEmissao());
        String chaveAcessoSemDV = gerarChaveAcesso(notaFiscalTO.getCUF(), DateHelper.formatarDataHora(notaFiscalTO.getDEmi(), DateHelper.DATE_YEAR_FORMAT), DateHelper.formatarDataHora(notaFiscalTO.getDEmi(), DateHelper.DATE_MONTH_FORMAT), notaFiscalTO.getEmitenteTO().getCNPJ(), notaFiscalTO.getModelo(), notaFiscalTO.getSerie(), notaFiscalTO.getNumero(), notaFiscalTO.getTipoEmissao().getCodigo(), notaFiscalTO.getCodigoNumericoChaveAcesso());
        return gerarDigitoChaveAcesso(chaveAcessoSemDV);
    }
}
