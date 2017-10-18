package net.sourceforge.jwapi.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

public class StringUtils {

    private static HashMap<String, Integer> htmlMapping = new HashMap<String, Integer>();

    static {
        htmlMapping.put("nbsp", 160);
        htmlMapping.put("iexcl", 161);
        htmlMapping.put("cent", 162);
        htmlMapping.put("pound", 163);
        htmlMapping.put("curren", 164);
        htmlMapping.put("yen", 165);
        htmlMapping.put("brvbar", 166);
        htmlMapping.put("sect", 167);
        htmlMapping.put("uml", 168);
        htmlMapping.put("copy", 169);
        htmlMapping.put("ordf", 170);
        htmlMapping.put("laquo", 171);
        htmlMapping.put("not", 172);
        htmlMapping.put("shy", 173);
        htmlMapping.put("reg", 174);
        htmlMapping.put("macr", 175);
        htmlMapping.put("deg", 176);
        htmlMapping.put("plusmn", 177);
        htmlMapping.put("sup2", 178);
        htmlMapping.put("sup3", 179);
        htmlMapping.put("acute", 180);
        htmlMapping.put("micro", 181);
        htmlMapping.put("para", 182);
        htmlMapping.put("middot", 183);
        htmlMapping.put("cedil", 184);
        htmlMapping.put("sup1", 185);
        htmlMapping.put("ordm", 186);
        htmlMapping.put("raquo", 187);
        htmlMapping.put("frac14", 188);
        htmlMapping.put("frac12", 189);
        htmlMapping.put("frac34", 190);
        htmlMapping.put("iquest", 191);
        htmlMapping.put("Agrave", 192);
        htmlMapping.put("Aacute", 193);
        htmlMapping.put("Acirc", 194);
        htmlMapping.put("Atilde", 195);
        htmlMapping.put("Auml", 196);
        htmlMapping.put("Aring", 197);
        htmlMapping.put("AElig", 198);
        htmlMapping.put("Ccedil", 199);
        htmlMapping.put("Egrave", 200);
        htmlMapping.put("Eacute", 201);
        htmlMapping.put("Ecirc", 202);
        htmlMapping.put("Euml", 203);
        htmlMapping.put("Igrave", 204);
        htmlMapping.put("Iacute", 205);
        htmlMapping.put("Icirc", 206);
        htmlMapping.put("Iuml", 207);
        htmlMapping.put("ETH", 208);
        htmlMapping.put("Ntilde", 209);
        htmlMapping.put("Ograve", 210);
        htmlMapping.put("Oacute", 211);
        htmlMapping.put("Ocirc", 212);
        htmlMapping.put("Otilde", 213);
        htmlMapping.put("Ouml", 214);
        htmlMapping.put("times", 215);
        htmlMapping.put("Oslash", 216);
        htmlMapping.put("Ugrave", 217);
        htmlMapping.put("Uacute", 218);
        htmlMapping.put("Ucirc", 219);
        htmlMapping.put("Uuml", 220);
        htmlMapping.put("Yacute", 221);
        htmlMapping.put("THORN", 222);
        htmlMapping.put("szlig", 223);
        htmlMapping.put("agrave", 224);
        htmlMapping.put("aacute", 225);
        htmlMapping.put("acirc", 226);
        htmlMapping.put("atilde", 227);
        htmlMapping.put("auml", 228);
        htmlMapping.put("aring", 229);
        htmlMapping.put("aelig", 230);
        htmlMapping.put("ccedil", 231);
        htmlMapping.put("egrave", 232);
        htmlMapping.put("eacute", 233);
        htmlMapping.put("ecirc", 234);
        htmlMapping.put("euml", 235);
        htmlMapping.put("igrave", 236);
        htmlMapping.put("iacute", 237);
        htmlMapping.put("icirc", 238);
        htmlMapping.put("iuml", 239);
        htmlMapping.put("eth", 240);
        htmlMapping.put("ntilde", 241);
        htmlMapping.put("ograve", 242);
        htmlMapping.put("oacute", 243);
        htmlMapping.put("ocirc", 244);
        htmlMapping.put("otilde", 245);
        htmlMapping.put("ouml", 246);
        htmlMapping.put("divide", 247);
        htmlMapping.put("oslash", 248);
        htmlMapping.put("ugrave", 249);
        htmlMapping.put("uacute", 250);
        htmlMapping.put("ucirc", 251);
        htmlMapping.put("uuml", 252);
        htmlMapping.put("yacute", 253);
        htmlMapping.put("thorn", 254);
        htmlMapping.put("yuml", 255);
        htmlMapping.put("fnof", 402);
        htmlMapping.put("Alpha", 913);
        htmlMapping.put("Beta", 914);
        htmlMapping.put("Gamma", 915);
        htmlMapping.put("Delta", 916);
        htmlMapping.put("Epsilon", 917);
        htmlMapping.put("Zeta", 918);
        htmlMapping.put("Eta", 919);
        htmlMapping.put("Theta", 920);
        htmlMapping.put("Iota", 921);
        htmlMapping.put("Kappa", 922);
        htmlMapping.put("Lambda", 923);
        htmlMapping.put("Mu", 924);
        htmlMapping.put("Nu", 925);
        htmlMapping.put("Xi", 926);
        htmlMapping.put("Omicron", 927);
        htmlMapping.put("Pi", 928);
        htmlMapping.put("Rho", 929);
        htmlMapping.put("Sigma", 931);
        htmlMapping.put("Tau", 932);
        htmlMapping.put("Upsilon", 933);
        htmlMapping.put("Phi", 934);
        htmlMapping.put("Chi", 935);
        htmlMapping.put("Psi", 936);
        htmlMapping.put("Omega", 937);
        htmlMapping.put("alpha", 945);
        htmlMapping.put("beta", 946);
        htmlMapping.put("gamma", 947);
        htmlMapping.put("delta", 948);
        htmlMapping.put("epsilon", 949);
        htmlMapping.put("zeta", 950);
        htmlMapping.put("eta", 951);
        htmlMapping.put("theta", 952);
        htmlMapping.put("iota", 953);
        htmlMapping.put("kappa", 954);
        htmlMapping.put("lambda", 955);
        htmlMapping.put("mu", 956);
        htmlMapping.put("nu", 957);
        htmlMapping.put("xi", 958);
        htmlMapping.put("omicron", 959);
        htmlMapping.put("pi", 960);
        htmlMapping.put("rho", 961);
        htmlMapping.put("sigmaf", 962);
        htmlMapping.put("sigma", 963);
        htmlMapping.put("tau", 964);
        htmlMapping.put("upsilon", 965);
        htmlMapping.put("phi", 966);
        htmlMapping.put("chi", 967);
        htmlMapping.put("psi", 968);
        htmlMapping.put("omega", 969);
        htmlMapping.put("thetasym", 977);
        htmlMapping.put("upsih", 978);
        htmlMapping.put("piv", 982);
        htmlMapping.put("bull", 8226);
        htmlMapping.put("hellip", 8230);
        htmlMapping.put("prime", 8242);
        htmlMapping.put("Prime", 8243);
        htmlMapping.put("oline", 8254);
        htmlMapping.put("frasl", 8260);
        htmlMapping.put("weierp", 8472);
        htmlMapping.put("image", 8465);
        htmlMapping.put("real", 8476);
        htmlMapping.put("trade", 8482);
        htmlMapping.put("alefsym", 8501);
        htmlMapping.put("larr", 8592);
        htmlMapping.put("uarr", 8593);
        htmlMapping.put("rarr", 8594);
        htmlMapping.put("darr", 8595);
        htmlMapping.put("harr", 8596);
        htmlMapping.put("crarr", 8629);
        htmlMapping.put("lArr", 8656);
        htmlMapping.put("uArr", 8657);
        htmlMapping.put("rArr", 8658);
        htmlMapping.put("dArr", 8659);
        htmlMapping.put("hArr", 8660);
        htmlMapping.put("forall", 8704);
        htmlMapping.put("part", 8706);
        htmlMapping.put("exist", 8707);
        htmlMapping.put("empty", 8709);
        htmlMapping.put("nabla", 8711);
        htmlMapping.put("isin", 8712);
        htmlMapping.put("notin", 8713);
        htmlMapping.put("ni", 8715);
        htmlMapping.put("prod", 8719);
        htmlMapping.put("sum", 8721);
        htmlMapping.put("minus", 8722);
        htmlMapping.put("lowast", 8727);
        htmlMapping.put("radic", 8730);
        htmlMapping.put("prop", 8733);
        htmlMapping.put("infin", 8734);
        htmlMapping.put("ang", 8736);
        htmlMapping.put("and", 8743);
        htmlMapping.put("or", 8744);
        htmlMapping.put("cap", 8745);
        htmlMapping.put("cup", 8746);
        htmlMapping.put("int", 8747);
        htmlMapping.put("there4", 8756);
        htmlMapping.put("sim", 8764);
        htmlMapping.put("cong", 8773);
        htmlMapping.put("asymp", 8776);
        htmlMapping.put("ne", 8800);
        htmlMapping.put("equiv", 8801);
        htmlMapping.put("le", 8804);
        htmlMapping.put("ge", 8805);
        htmlMapping.put("sub", 8834);
        htmlMapping.put("sup", 8835);
        htmlMapping.put("nsub", 8836);
        htmlMapping.put("sube", 8838);
        htmlMapping.put("supe", 8839);
        htmlMapping.put("oplus", 8853);
        htmlMapping.put("otimes", 8855);
        htmlMapping.put("perp", 8869);
        htmlMapping.put("sdot", 8901);
        htmlMapping.put("lceil", 8968);
        htmlMapping.put("rceil", 8969);
        htmlMapping.put("lfloor", 8970);
        htmlMapping.put("rfloor", 8971);
        htmlMapping.put("lang", 9001);
        htmlMapping.put("rang", 9002);
        htmlMapping.put("loz", 9674);
        htmlMapping.put("spades", 9824);
        htmlMapping.put("clubs", 9827);
        htmlMapping.put("hearts", 9829);
        htmlMapping.put("diams", 9830);
        htmlMapping.put("quot", 34);
        htmlMapping.put("amp", 38);
        htmlMapping.put("lt", 60);
        htmlMapping.put("gt", 62);
        htmlMapping.put("OElig", 338);
        htmlMapping.put("oelig", 339);
        htmlMapping.put("Scaron", 352);
        htmlMapping.put("scaron", 353);
        htmlMapping.put("Yuml", 376);
        htmlMapping.put("circ", 710);
        htmlMapping.put("tilde", 732);
        htmlMapping.put("ensp", 8194);
        htmlMapping.put("emsp", 8195);
        htmlMapping.put("thinsp", 8201);
        htmlMapping.put("zwnj", 8204);
        htmlMapping.put("zwj", 8205);
        htmlMapping.put("lrm", 8206);
        htmlMapping.put("rlm", 8207);
        htmlMapping.put("ndash", 8211);
        htmlMapping.put("mdash", 8212);
        htmlMapping.put("lsquo", 8216);
        htmlMapping.put("rsquo", 8217);
        htmlMapping.put("sbquo", 8218);
        htmlMapping.put("ldquo", 8220);
        htmlMapping.put("rdquo", 8221);
        htmlMapping.put("bdquo", 8222);
        htmlMapping.put("dagger", 8224);
        htmlMapping.put("Dagger", 8225);
        htmlMapping.put("permil", 8240);
        htmlMapping.put("lsaquo", 8249);
        htmlMapping.put("rsaquo", 8250);
        htmlMapping.put("euro", 8364);
    }

    public static String unescapeHTML(String s) {
        StringBuilder result = new StringBuilder(s.length());
        int ampInd = s.indexOf("&");
        int lastEnd = 0;
        while (ampInd >= 0) {
            int nextAmp = s.indexOf("&", ampInd + 1);
            int nextSemi = s.indexOf(";", ampInd + 1);
            if (nextSemi != -1 && (nextAmp == -1 || nextSemi < nextAmp)) {
                int value = -1;
                String escape = s.substring(ampInd + 1, nextSemi);
                try {
                    if (escape.startsWith("#")) {
                        value = Integer.parseInt(escape.substring(1), 10);
                    } else {
                        if (htmlMapping.containsKey(escape)) {
                            value = (htmlMapping.get(escape));
                        }
                    }
                } catch (NumberFormatException x) {
                }
                result.append(s.substring(lastEnd, ampInd));
                lastEnd = nextSemi + 1;
                if (value >= 0 && value <= 0xffff) {
                    result.append((char) value);
                } else {
                    result.append("&").append(escape).append(";");
                }
            }
            ampInd = nextAmp;
        }
        result.append(s.substring(lastEnd));
        return result.toString();
    }

    public static String getMD5Hash(String str) {
        try {
            MessageDigest algorithm = MessageDigest.getInstance("MD5");
            byte[] defaultBytes = str.toString().getBytes();
            algorithm.reset();
            algorithm.update(defaultBytes);
            byte messageDigest[] = algorithm.digest();
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < messageDigest.length; i++) {
                String hex = Integer.toHexString(0xFF & messageDigest[i]);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
}
