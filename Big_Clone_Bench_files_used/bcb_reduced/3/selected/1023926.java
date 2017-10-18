package br.com.guaraba.commons.util;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.text.Normalizer;

public final class StringUtils {

    public static String removerAcentos(String acentuada) {
        CharSequence cs = new StringBuilder(acentuada);
        return Normalizer.normalize(cs, Normalizer.Form.NFKD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    private static final char[] SPECIAL_CHARACTERS = { '<', '>', '%', ';', ')', '(', '&', '+', '-', '\'', '\"', '.', '_', ',', ':', '=', '$', '@', '/' };

    public static boolean isNullOrEmpty(String string) {
        return isNullOrEmpty(string, true);
    }

    public static boolean isNullOrEmpty(String string, boolean trim) {
        return (string == null || ((trim) ? string.trim().length() == 0 : string.length() == 0));
    }

    public static String removeSpecialCharacters(String value) {
        return removeSpecialCharacters(value, null);
    }

    public static String removeSpecialCharacters(String value, char... caracteresIgnorados) {
        for (char caractere : SPECIAL_CHARACTERS) {
            if (!contains(caracteresIgnorados, caractere)) {
                while (value.indexOf(String.valueOf(caractere)) > -1) {
                    String aux = value.substring(0, value.indexOf(String.valueOf(caractere)));
                    aux += value.substring(value.indexOf(String.valueOf(caractere)) + 1);
                    value = aux;
                }
            }
        }
        return value;
    }

    /**
	 * Verifica se um determinado char é contido na coleção de char informado
	 * 
	 * @param caracteres
	 *            vetor a ser verificado
	 * @param caractere
	 *            caractere a ser verificado
	 * @return
	 */
    private static boolean contains(char[] caracteres, char caractere) {
        if (caracteres != null) {
            for (char c : caracteres) {
                if (c == caractere) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
	 * 
	 * makeMD5.
	 * 
	 * @return MD5 "checksum" para um input dado. Será usado para encriptar as senhas.
	 */
    public static String makeMD5(String input) throws Exception {
        String dstr = null;
        byte[] digest;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(input.getBytes());
            digest = md.digest();
            dstr = new BigInteger(1, digest).toString(16);
            if (dstr.length() % 2 > 0) {
                dstr = "0" + dstr;
            }
        } catch (Exception e) {
            throw new Exception("Erro inesperado em makeMD5(): " + e.toString(), e);
        }
        return dstr;
    }

    /**
	 * Metodo verifica se os determinatos caractres proibidos estão contidos em uma <b>String</b> 
	 * 
	 * @param palavra
	 *            String a ser verificada
	 * @return retorna <b>true</b> se existir caractres proibidos ou <b>false</b> se não<br>
	 *         não houver
	 */
    public static boolean verificaCaracterEspeciais(String palavra) {
        return verificaCaracterEspeciais(palavra, ',');
    }

    /**
	 * Metodo verifica se os determinatos caractres proibidos estão contidos em uma <b>String</b> 
	 * 
	 * @param palavra
	 *            String a ser verificada
	 * @param caracteresIgnorados
	 *            Array de caracteres que serão aceitos
	 * @return retorna <b>true</b> se existir caractres proibidos ou <b>false</b> se não<br>
	 *         não houver
	 */
    public static boolean verificaCaracterEspeciais(String palavra, char... caracteresIgnorados) {
        if (palavra == null) {
            return false;
        }
        for (char caracter : SPECIAL_CHARACTERS) {
            if (!contains(caracteresIgnorados, caracter)) {
                if (palavra.indexOf(caracter) > -1) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
	 * Gera uma String aleatória com o tamanho de caracteres informado via parametro
	 * @param quantidadeDeCaracteres
	 * 			quantidade de caracteres que terá a string aleatoria
	 * @return
	 * 		retorna uma String aleatória com o tamanho de caracteres informado via parametro
	 */
    public static String getRandomString(Long quantidadeDeCaracteres) {
        StringBuffer sb = new StringBuffer();
        int c = 'A';
        int r1 = 0;
        for (int i = 0; i < quantidadeDeCaracteres; i++) {
            r1 = (int) (Math.random() * 3);
            switch(r1) {
                case 0:
                    c = '0' + (int) (Math.random() * 10);
                    break;
                case 1:
                    c = 'a' + (int) (Math.random() * 26);
                    break;
                case 2:
                    c = 'A' + (int) (Math.random() * 26);
                    break;
            }
            sb.append((char) c);
        }
        return sb.toString();
    }

    public static String formatar(String valor, String mascara) {
        String dado = "";
        for (int i = 0; i < valor.length(); i++) {
            char c = valor.charAt(i);
            if (Character.isDigit(c)) {
                dado += c;
            }
        }
        int indMascara = mascara.length();
        int indCampo = dado.length();
        for (; indCampo > 0 && indMascara > 0; ) {
            if (mascara.charAt(--indMascara) == '#') {
                indCampo--;
            }
        }
        String saida = "";
        for (; indMascara < mascara.length(); indMascara++) {
            saida += ((mascara.charAt(indMascara) == '#') ? dado.charAt(indCampo++) : mascara.charAt(indMascara));
        }
        return saida;
    }

    public static String formatarCpf(String cpf) {
        while (cpf.length() < 11) {
            cpf = "0" + cpf;
        }
        return formatar(cpf, "###.###.###-##");
    }

    public static String formatarCnpj(String cnpj) {
        while (cnpj.length() < 14) {
            cnpj = "0" + cnpj;
        }
        return formatar(cnpj, "##.###.###/####-##");
    }

    public static String formatarTelefone(String telefone) {
        while (telefone.length() < 14) {
            telefone = "0" + telefone;
        }
        return formatar(telefone, "(##) ####-####");
    }
}
