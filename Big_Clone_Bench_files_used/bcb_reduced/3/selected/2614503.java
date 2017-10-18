package com.imoresoft.magic.top;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;
import com.imoresoft.magic.entity.Result;
import com.imoresoft.magic.entity.ResultCode;
import com.imoresoft.magic.exception.ProException;
import com.taobao.api.TaobaoObject;
import com.taobao.api.TaobaoRequest;
import com.taobao.api.TaobaoResponse;

public class TaobaoUtil {

    private static final Logger logger = LoggerFactory.getLogger(TaobaoUtil.class);

    /**
	 * @param TaobaoResponse
	 * @return 9000:成功返回; -9004:session失效; -9002:获取总行数为null;
	 *         -9001:返回的response为null; -9003:其他错误
	 */
    public static Result parseTaobaoApiResponse(TaobaoResponse rsp) {
        Result result = new Result();
        if (rsp == null) {
            result.setCode(ResultCode.TOP_CALL_ERROR_RESPONSE_NULL);
            result.setMsg("返回的response为null");
        } else if (rsp.isSuccess()) {
            result.setCode(ResultCode.TOP_CALL_SUCCESS);
            result.setMsg("调用成功");
        } else if (TopConstants.ERROR_CODE_INVALID_SESSION.equals(rsp.getErrorCode())) {
            result.setCode(ResultCode.TOP_CALL_ERROR_RESPONSE_SESSION_INVALID);
            result.setMsg("session失效");
        } else {
            result.setCode(ResultCode.TOP_CALL_ERROR_RESPONSE_OTHER);
            String msg = "{EXCEPTION_CODE:" + rsp.getErrorCode() + ",EXCEPTION_MSG:" + rsp.getMsg() + ";EXCEPTION_SUB_CODE:" + rsp.getSubCode() + ",EXCEPTION_SUB_MSG:" + rsp.getSubMsg() + "}";
            result.setMsg(msg);
        }
        return result;
    }

    public static void parseTaobaoResponse(TaobaoResponse rsp, String desc) throws ProException {
        if (rsp == null) {
            throw new ProException(ResultCode.TOP_SERVER_EXCEPTION, "TaobaoResponse is null");
        }
        if (rsp.isSuccess() == false) {
            throw new ProException(ResultCode.TOP_SERVER_EXCEPTION, desc, rsp.getErrorCode(), rsp.getMsg(), rsp.getSubCode(), rsp.getSubMsg());
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends TaobaoObject> List<T> getInternalBeanList(TaobaoResponse rsp, Type beanType) {
        try {
            Method m = GenericUtils.findListGetMethodByReturnType(rsp.getClass(), beanType);
            return (List<T>) m.invoke(rsp, new Object[] {});
        } catch (IllegalArgumentException e) {
            logger.error(e.getMessage(), e);
        } catch (IllegalAccessException e) {
            logger.error(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            logger.error(e.getMessage(), e);
        } catch (NoSuchMethodException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    public static void fillResponseWithBeanList(TaobaoResponse rsp, Type beanType, List<TaobaoObject> beans) {
        try {
            Method m = GenericUtils.findListSetMethodByParameterType(rsp.getClass(), beanType);
            m.invoke(rsp, new Object[] { beans });
        } catch (IllegalArgumentException e) {
            logger.error(e.getMessage(), e);
        } catch (IllegalAccessException e) {
            logger.error(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            logger.error(e.getMessage(), e);
        } catch (NoSuchMethodException e) {
            logger.error(e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends TaobaoObject> T getInternalBean(Object rsp, Type beanType) {
        try {
            Method m = GenericUtils.findGetMethodByReturnType(rsp.getClass(), beanType);
            return (T) m.invoke(rsp, new Object[] {});
        } catch (IllegalArgumentException e) {
            logger.error(e.getMessage(), e);
        } catch (IllegalAccessException e) {
            logger.error(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            logger.error(e.getMessage(), e);
        } catch (NoSuchMethodException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    public static void setPageNo(TaobaoRequest request, long page) {
        if (page <= 0) throw new IllegalArgumentException("The page number is start from 1...");
        try {
            Object[] args = new Object[] { Long.valueOf(page) };
            GenericUtils.invoke(request, "setPageNo", args);
            logger.info("PageNo被设置为" + page);
        } catch (SecurityException e) {
            logger.error(e.getMessage(), e);
        } catch (NoSuchMethodException e) {
            logger.error(e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            logger.error(e.getMessage(), e);
        } catch (IllegalAccessException e) {
            logger.error(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static void setPageSize(TaobaoRequest request, long pageSize) {
        if (pageSize <= 0) throw new IllegalArgumentException("The page size should be positive...");
        try {
            Object[] args = new Object[] { Long.valueOf(pageSize) };
            GenericUtils.invoke(request, "setPageSize", args);
            logger.info("PageSize被设置为" + pageSize);
        } catch (SecurityException e) {
            logger.error(e.getMessage(), e);
        } catch (NoSuchMethodException e) {
            logger.error(e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            logger.error(e.getMessage(), e);
        } catch (IllegalAccessException e) {
            logger.error(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static String getNick(TaobaoRequest request) {
        try {
            Object[] args = new Object[] {};
            return GenericUtils.invoke(request, "getNick", args);
        } catch (SecurityException e) {
            logger.error(e.getMessage(), e);
        } catch (NoSuchMethodException e) {
            logger.error(e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            logger.error(e.getMessage(), e);
        } catch (IllegalAccessException e) {
            logger.error(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    public static Long getTotalResults(TaobaoResponse response) throws IllegalArgumentException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Long totalResults = GenericUtils.invoke(response, "getTotalResults", null);
        if (totalResults == null) {
            logger.info("取出总记录数NULL");
            totalResults = new Long(0);
            return totalResults;
        }
        logger.info("取出总记录数" + totalResults);
        return totalResults;
    }

    public static boolean validateSign(String sign, Map<String, String> params, String secret) throws Exception {
        boolean flag = false;
        if (sign != null && params != null && secret != null && sign.equals(Signature(params, secret))) {
            flag = true;
        }
        return flag;
    }

    public static String Signature(Map<String, String> params, String secret) throws Exception {
        String result = "";
        try {
            String top_appkey = params.get("top_appkey");
            String top_parameters = params.get("top_parameters");
            String top_session = params.get("top_session");
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest((top_appkey + top_parameters + top_session + secret).getBytes());
            BASE64Encoder encode = new BASE64Encoder();
            result = encode.encode(digest);
        } catch (Exception ex) {
            throw new Exception("Sign Error !");
        }
        return result;
    }

    public static Map<String, String> convertBase64StringtoMap(String str) {
        if (str == null) return null;
        BASE64Decoder decoder = new BASE64Decoder();
        String keyValues = null;
        try {
            keyValues = new String(decoder.decodeBuffer(str), "GB2312");
        } catch (Exception e) {
            logger.error(str + "不是一个合法的BASE64编码字符串");
            return null;
        }
        if (keyValues == null || keyValues.length() <= 0) return null;
        String[] keyValueArray = keyValues.split("&");
        Map<String, String> params = new HashMap<String, String>();
        for (String keyValue : keyValueArray) {
            String[] param = keyValue.split("=");
            if (param == null || param.length != 2) {
                return null;
            }
            params.put(param[0], param[1]);
        }
        return params;
    }
}
