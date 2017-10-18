package br.furb.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * M�todos utilit�rios
 * 
 * @author Jean
 */
public class Utils {

    /**
	 * @param hour1
	 * @param hour2
	 * @return A diferen�a entre as duas datas
	 */
    public static Date getHoursDiference(Date hour1, Date hour2) {
        long hora = (hour2.getTime() - hour1.getTime()) / (1000 * 60 * 60);
        long min = (hour2.getTime() - hour1.getTime()) / (1000 * 60);
        int difHora = (int) min / 60;
        int difMin = (int) (min - (hora * 60));
        String diference = (String.valueOf(difHora).length() == 2 ? String.valueOf(difHora) : "0" + String.valueOf(difHora)) + ":" + (String.valueOf(difMin).length() == 2 ? String.valueOf(difMin) : "0" + String.valueOf(difMin));
        Date returnDif = getStringToTime(diference, "HH:mm");
        return returnDif;
    }

    /**
	 * Converte uma data de String para {@link Date}
	 * 
	 * @param dateTimeDDMMYYYY_HHMMSS
	 *            - A data no formato String
	 * @return A data
	 */
    public static Date getStringToDateTime(String dateTimeDDMMYYYY_HHMMSS) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Date date = format.parse(dateTimeDDMMYYYY_HHMMSS);
            return date;
        } catch (Exception ex) {
            return null;
        }
    }

    public static Date getStringToDate(String dateDDMMYYYY) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
            Date date = format.parse(dateDDMMYYYY);
            return date;
        } catch (Exception ex) {
            return null;
        }
    }

    public static Date getStringToTime(String timeHHMMSS, String formatString) {
        try {
            SimpleDateFormat format = new SimpleDateFormat(formatString);
            Date date = format.parse(timeHHMMSS);
            return date;
        } catch (Exception ex) {
            return null;
        }
    }

    public static Date getStringToTime(String timeHHMMSS) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
            Date date = format.parse(timeHHMMSS);
            return date;
        } catch (Exception ex) {
            return null;
        }
    }

    public static String getTimeToString(Date time) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
            return format.format(time);
        } catch (Exception ex) {
            return "";
        }
    }

    public static String getTimeToString(Date time, String formatString) {
        try {
            SimpleDateFormat format = new SimpleDateFormat(formatString);
            return format.format(time);
        } catch (Exception ex) {
            return "";
        }
    }

    public static String getDateTimeToString(Date date) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            return format.format(date);
        } catch (Exception ex) {
            return "";
        }
    }

    public static String getDateToString(Date date) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
            return format.format(date);
        } catch (Exception ex) {
            return "";
        }
    }

    public static String concatString(String[] strings) {
        if ((strings == null) || (strings.length == 0)) return "";
        StringBuffer concat = new StringBuffer();
        for (String str : strings) {
            concat.append(str);
        }
        return concat.toString();
    }

    public static String concatString(String string1, String string2) {
        StringBuffer concat = new StringBuffer();
        concat.append(string1);
        concat.append(string2);
        return concat.toString();
    }

    public static Boolean isEmptyString(String value) {
        if (value == null) return true; else {
            if ("".equals(value)) {
                return true;
            } else {
                return false;
            }
        }
    }

    public static String generateHash(String string, String algoritmo) {
        try {
            MessageDigest md = MessageDigest.getInstance(algoritmo);
            md.update(string.getBytes());
            byte[] result = md.digest();
            int firstPart;
            int lastPart;
            StringBuilder sBuilder = new StringBuilder();
            for (int i = 0; i < result.length; i++) {
                firstPart = ((result[i] >> 4) & 0xf) << 4;
                lastPart = result[i] & 0xf;
                if (firstPart == 0) sBuilder.append("0");
                sBuilder.append(Integer.toHexString(firstPart | lastPart));
            }
            return sBuilder.toString();
        } catch (NoSuchAlgorithmException ex) {
            return null;
        }
    }

    public static String generateSHA1Hash(String string) {
        return Utils.generateHash(string, "SHA-1");
    }

    /**
	 * Arredonda um valor para duas casas decimais
	 * 
	 * @param valor
	 *            - o valor que deseja arredondar
	 * @return o double para 2 casas decimais
	 */
    public static double arredondar(double valor) {
        double arredondado = valor;
        arredondado *= (Math.pow(10, 2));
        arredondado = Math.ceil(arredondado);
        arredondado /= (Math.pow(10, 2));
        return arredondado;
    }

    /**
	 * Recupera um arquivo do disco em Array de Byte
	 * 
	 * @param sourcePath
	 *            o caminho completo para Ler o arquivo do disco
	 * @return - os bytes do arquivo
	 */
    public static byte[] convertDocToByteArray(String sourcePath) {
        byte[] byteArray = null;
        try {
            File file = new File(sourcePath);
            InputStream inputStream = new FileInputStream(file);
            byteArray = new byte[(int) file.length()];
            inputStream.read(byteArray);
            inputStream.close();
        } catch (FileNotFoundException e) {
            System.out.println("File Not found" + e);
        } catch (IOException e) {
            System.out.println("IO Ex" + e);
        }
        return byteArray;
    }

    /**
	 * Salva Array De Byte em Disco
	 * 
	 * @param b
	 *            - oarrayde bytes
	 * @param sourcePath
	 *            - sourcePath deve ser o caminho completo para salvar o arquivo
	 *            no disco
	 */
    public static void convertByteArrayToFile(byte[] b, String sourcePath) {
        File file = null;
        try {
            file = new File(sourcePath);
            FileOutputStream fos = new FileOutputStream(file, true);
            fos.write(b);
            fos.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    /**
	 * Excluir arquivo do disco
	 * 
	 * @param sourcePath
	 *            - o caminho completo para excluir o arquivo do disco
	 */
    public static void deleteFile(String sourcePath) {
        try {
            File file = new File(sourcePath);
            if (file.exists()) file.delete();
        } catch (Exception e) {
            System.out.println("Exeption" + e);
        }
    }
}
