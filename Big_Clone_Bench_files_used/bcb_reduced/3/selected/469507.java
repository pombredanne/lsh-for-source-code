package com.antares.commons.util;

import static com.antares.sirius.base.Constants.DEFAULT_DATE_FORMAT;
import static com.antares.sirius.base.Constants.DEFAULT_DECIMAL_FORMAT;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.MessageDigest;
import java.sql.Blob;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.acegisecurity.Authentication;
import org.acegisecurity.context.SecurityContextHolder;
import org.apache.log4j.Logger;
import org.apache.struts.upload.FormFile;
import org.hibernate.Hibernate;
import org.springframework.context.MessageSource;
import com.antares.sirius.model.PersistentObject;
import com.google.gson.Gson;

public class Utils {

    private static final Logger logger = Logger.getLogger(Utils.class);

    private static MessageSource messageSource;

    private static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(DEFAULT_DATE_FORMAT);

    private static DecimalFormat DECIMAL_FORMAT = new DecimalFormat(DEFAULT_DECIMAL_FORMAT);

    public void setMessageSource(MessageSource messageSource) {
        Utils.messageSource = messageSource;
    }

    public void setDatePattern(String datePattern) {
        Utils.DATE_FORMAT = new SimpleDateFormat(datePattern);
    }

    /**
	 * Resuelve el valor del text representado por la clave key.
	 * 
	 * @param key clave a resolver.
	 * @return
	 */
    public static String getMessage(String key) {
        String result = null;
        try {
            result = messageSource.getMessage(key, null, null);
        } catch (Exception e) {
            logger.debug("Error al resolver la clave " + key, e);
        }
        return result;
    }

    /**
	 * Formatea la fecha usando el formato predeterminado del sistema
	 * 
	 * @param fecha a formatear
	 * @return
	 */
    public static String formatDate(Date date) {
        String strDate = "";
        if (date != null) {
            strDate = DATE_FORMAT.format(date);
        }
        return strDate;
    }

    /**
	 * Parsea la fecha usando el formato predeterminado del sistema
	 * 
	 * @param dateStr fecha a parsear
	 * @return
	 */
    public static Date parseDate(String dateStr) {
        Date date = null;
        try {
            if (isNotNullNorEmpty(dateStr)) {
                date = DATE_FORMAT.parse(dateStr);
            }
        } catch (ParseException e) {
        }
        return date;
    }

    /**
	 * Formatea el n�mero double usando el formato predeterminado del sistema
	 * 
	 * @param number double a formatear
	 * @return
	 */
    public static String formatDouble(Double number) {
        String strDouble = "";
        if (number != null) {
            strDouble = DECIMAL_FORMAT.format(number);
        }
        return strDouble;
    }

    /**
	 * Parsea el n�mero double usando el formato predeterminado del sistema, en caso de no poder parsearlo devuelve null
	 * 
	 * @param doubleStr double a parsear
	 * @return
	 */
    public static Double parseDouble(String doubleStr) {
        Double doubleNum = null;
        if (isNotNullNorEmpty(doubleStr)) {
            ParsePosition parsePosition = new ParsePosition(0);
            Number num = DECIMAL_FORMAT.parse(doubleStr, parsePosition);
            if (parsePosition.getIndex() == doubleStr.length()) {
                BigDecimal bd = new BigDecimal(num.doubleValue()).setScale(2, RoundingMode.HALF_UP);
                doubleNum = bd.doubleValue();
            }
        }
        return doubleNum;
    }

    /**
	 * Verifica si el string pasado por parametro es double o no
	 * 
	 * @param doubleStr string a verificar
	 * @return
	 */
    public static boolean isDouble(String doubleStr) {
        boolean isDouble = false;
        try {
            if (isNotNullNorEmpty(doubleStr)) {
                DECIMAL_FORMAT.parse(doubleStr).doubleValue();
                isDouble = true;
            }
        } catch (ParseException e) {
        }
        return isDouble;
    }

    /**
	 * Parsea el n�mero entero, en caso de no poder parsearlo devuelve null
	 * 
	 * @param intStr entero a parsear
	 * @return
	 */
    public static Integer parseInteger(String intStr) {
        Integer integer = null;
        try {
            if (isNotNullNorEmpty(intStr)) {
                integer = Integer.parseInt(intStr);
            }
        } catch (NumberFormatException e) {
        }
        return integer;
    }

    /**
	 * Calcula el porcentaje de un numero en un total. Si el total es 0, se devuelve 0
	 * 
	 * @param num numero cuyo porcentaje se quiere calcular
	 * @param total numero que representa el 100%
	 * @return
	 */
    public static BigDecimal calcularPorcentaje(BigDecimal num, BigDecimal total) {
        BigDecimal porcentaje = BigDecimal.ZERO;
        if (!BigDecimal.ZERO.equals(total)) {
            porcentaje = num.multiply(new BigDecimal(100)).divide(total, 2, RoundingMode.HALF_UP);
        }
        return porcentaje;
    }

    /**
	 * Evalua si el string que recibe por parametro es null o un strint vacio
	 * 
	 * @param str string a evaluar
	 * @return
	 */
    public static boolean isNullOrEmpty(String str) {
        return str == null || str.trim().length() == 0;
    }

    /**
	 * Evalua si el string que recibe por parametro no es null ni es un strint vacio
	 * 
	 * @param str string a evaluar
	 * @return
	 */
    public static boolean isNotNullNorEmpty(String str) {
        return str != null && str.trim().length() > 0;
    }

    /**
	 * Genera un hash del String usando MD5
	 * 
	 * @param str String a hashear
	 * @return String con el hash MD5
	 */
    public static String encode(String str) {
        String md5Str = null;
        try {
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(str.getBytes("UTF8"));
            byte[] hash = digest.digest();
            md5Str = "";
            for (int i = 0; i < hash.length; i++) {
                md5Str += Integer.toHexString((0x000000ff & hash[i]) | 0xffffff00).substring(6);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return md5Str;
    }

    /**
	 * Busca de forma recursiva en las clases padre la primera clase gen�rica parametrizable
	 * 
	 * @param clazz clase
	 * @return
	 */
    @SuppressWarnings("unchecked")
    public static ParameterizedType findParameterizedType(Class clazz) {
        ParameterizedType type = null;
        if (clazz != null) {
            if (clazz.getGenericSuperclass() instanceof ParameterizedType) {
                type = (ParameterizedType) clazz.getGenericSuperclass();
            } else {
                type = findParameterizedType(clazz.getSuperclass());
            }
        }
        return type;
    }

    /**
	 * Verifica que el usuario este autenticado en el sistema.
	 * 
	 * @return true si esta autenticado, false si no lo est�
	 */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && !"annonymous".equals(authentication.getName());
    }

    /**
	 * Retorna el username del usuario logueado en el sistema.
	 * 
	 * @return username del usuario logueado en el sistema
	 */
    public static String getUsername() {
        String username = null;
        if (isAuthenticated()) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            username = authentication.getName();
        }
        return username;
    }

    /**
	 * Construye un String con el nombre del getter de la propiedad pasada por parametros.
	 * Ej: Si propertyName == "nombre", el metodo devolvera "getNombre".
	 * 
	 * @param propertyName nombre de la propiedad
	 * @return nombre del getter
	 */
    public static String getterName(String propertyName) {
        String getterName = "";
        if (isNotNullNorEmpty(propertyName)) {
            getterName += "get" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
        }
        return getterName;
    }

    /**
	 * Obtiene el valor de una propiedad del persistentObject. Esta propiedad puede ser llamada a traves de una cadena de metodos.
	 * Para ello, debe indicarse los nombres de los atributos separados por "." en el propertyName. Por ejemplo:
	 * Si propertyName="estadoProyect.id" este metodo va a retornar el resultado de persistentObject.getEstadoProyecto().getId()
	 * En este caso, el metodo se llama de forma recursiva la cantidad de veces que sea necesario.
	 *  
	 * @param persistentObject objeto cuya propiedad se quiere obtener
	 * @param propertyName nombre de la propiedad
	 * @return valor de la propiedad
	 */
    public static Object getPropertyValue(PersistentObject persistentObject, String propertyName) {
        Object rval = null;
        try {
            if (propertyName.indexOf(".") > 0) {
                Class<?> clazz = persistentObject.getClass();
                Method method = clazz.getMethod(Utils.getterName(propertyName.substring(0, propertyName.indexOf("."))), new Class[] {});
                PersistentObject newPersistentObject = (PersistentObject) method.invoke(persistentObject, new Object[] {});
                rval = getPropertyValue(newPersistentObject, propertyName.substring(propertyName.indexOf(".") + 1));
            } else {
                Class<?> clazz = persistentObject.getClass();
                Method method = clazz.getMethod(Utils.getterName(propertyName), new Class[] {});
                rval = method.invoke(persistentObject, new Object[] {});
            }
        } catch (Exception e) {
            System.out.println("Problema al intentar acceder a la propiedad " + propertyName + " de la clase" + persistentObject.getClass().getSimpleName());
            e.printStackTrace();
        }
        return rval;
    }

    /**
	 * Devuelve un Blob con el contenido del FormFile.
	 * 
	 * @param formFile FormFile con el contenido del archivo a transformar en Blob
	 * @return Blob con el contenido del archivo
	 */
    public static Blob createBlob(FormFile formFile) {
        Blob blob = null;
        try {
            blob = Hibernate.createBlob(formFile.getInputStream());
        } catch (IOException e) {
        }
        return blob;
    }

    /**
	 * Convierte a JSON un objeto usando Gson.
	 * 
	 * @param obj objeto a convertir a JSON
	 * @return String que contiene el objeto en JSON
	 */
    public static String convertToJSON(Object obj) {
        String json = null;
        Gson gson = new Gson();
        json = (gson.toJson(obj));
        return json;
    }
}
