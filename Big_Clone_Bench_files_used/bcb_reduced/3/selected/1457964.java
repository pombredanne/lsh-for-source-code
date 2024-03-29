package org.apache.nutch.crawl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import org.apache.hadoop.io.MD5Hash;
import org.apache.nutch.parse.Parse;
import org.apache.nutch.parse.ParseImpl;
import org.apache.nutch.protocol.Content;
import org.apache.nutch.util.StringUtil;
import org.apache.nutch.util.NutchConfiguration;

/**
 * <p>An implementation of a page signature. It calculates an MD5 hash
 * of a plain text "profile" of a page. In case there is no text, it
 * calculates a hash using the {@link MD5Signature}.</p>
 * <p>The algorithm to calculate a page "profile" takes the plain text version of
 * a page and performs the following steps:
 * <ul>
 * <li>remove all characters except letters and digits, and bring all characters
 * to lower case,</li>
 * <li>split the text into tokens (all consecutive non-whitespace characters),</li>
 * <li>discard tokens equal or shorter than MIN_TOKEN_LEN (default 2 characters),</li>
 * <li>sort the list of tokens by decreasing frequency,</li>
 * <li>round down the counts of tokens to the nearest multiple of QUANT
 * (<code>QUANT = QUANT_RATE * maxFreq</code>, where <code>QUANT_RATE</code> is 0.01f
 * by default, and <code>maxFreq</code> is the maximum token frequency). If
 * <code>maxFreq</code> is higher than 1, then QUANT is always higher than 2 (which
 * means that tokens with frequency 1 are always discarded).</li>
 * <li>tokens, which frequency after quantization falls below QUANT, are discarded.</li>
 * <li>create a list of tokens and their quantized frequency, separated by spaces,
 * in the order of decreasing frequency.</li>
 * </ul>
 * This list is then submitted to an MD5 hash calculation.
 * 
 * @author Andrzej Bialecki &lt;ab@getopt.org&gt;
 */
public class TextProfileSignature extends Signature {

    Signature fallback = new MD5Signature();

    public byte[] calculate(Content content, Parse parse) {
        int MIN_TOKEN_LEN = getConf().getInt("db.signature.text_profile.min_token_len", 2);
        float QUANT_RATE = getConf().getFloat("db.signature.text_profile.quant_rate", 0.01f);
        HashMap tokens = new HashMap();
        String text = null;
        if (parse != null) text = parse.getText();
        if (text == null || text.length() == 0) return fallback.calculate(content, parse);
        StringBuffer curToken = new StringBuffer();
        int maxFreq = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                curToken.append(Character.toLowerCase(c));
            } else {
                if (curToken.length() > 0) {
                    if (curToken.length() > MIN_TOKEN_LEN) {
                        String s = curToken.toString();
                        Token tok = (Token) tokens.get(s);
                        if (tok == null) {
                            tok = new Token(0, s);
                            tokens.put(s, tok);
                        }
                        tok.cnt++;
                        if (tok.cnt > maxFreq) maxFreq = tok.cnt;
                    }
                    curToken.setLength(0);
                }
            }
        }
        if (curToken.length() > MIN_TOKEN_LEN) {
            String s = curToken.toString();
            Token tok = (Token) tokens.get(s);
            if (tok == null) {
                tok = new Token(0, s);
                tokens.put(s, tok);
            }
            tok.cnt++;
            if (tok.cnt > maxFreq) maxFreq = tok.cnt;
        }
        Iterator it = tokens.values().iterator();
        ArrayList profile = new ArrayList();
        int QUANT = Math.round(maxFreq * QUANT_RATE);
        if (QUANT < 2) {
            if (maxFreq > 1) QUANT = 2; else QUANT = 1;
        }
        while (it.hasNext()) {
            Token t = (Token) it.next();
            t.cnt = (t.cnt / QUANT) * QUANT;
            if (t.cnt < QUANT) {
                continue;
            }
            profile.add(t);
        }
        Collections.sort(profile, new TokenComparator());
        StringBuffer newText = new StringBuffer();
        it = profile.iterator();
        while (it.hasNext()) {
            Token t = (Token) it.next();
            if (newText.length() > 0) newText.append("\n");
            newText.append(t.toString());
        }
        return MD5Hash.digest(newText.toString()).getDigest();
    }

    private static class Token {

        public int cnt;

        public String val;

        public Token(int cnt, String val) {
            this.cnt = cnt;
            this.val = val;
        }

        public String toString() {
            return val + " " + cnt;
        }
    }

    private static class TokenComparator implements Comparator {

        public int compare(Object o1, Object o2) {
            Token t1 = (Token) o1;
            Token t2 = (Token) o2;
            return t2.cnt - t1.cnt;
        }
    }

    public static void main(String[] args) throws Exception {
        TextProfileSignature sig = new TextProfileSignature();
        sig.setConf(NutchConfiguration.create());
        HashMap res = new HashMap();
        File[] files = new File(args[0]).listFiles();
        for (int i = 0; i < files.length; i++) {
            FileInputStream fis = new FileInputStream(files[i]);
            BufferedReader br = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
            StringBuffer text = new StringBuffer();
            String line = null;
            while ((line = br.readLine()) != null) {
                if (text.length() > 0) text.append("\n");
                text.append(line);
            }
            br.close();
            byte[] signature = sig.calculate(null, new ParseImpl(text.toString(), null));
            res.put(files[i].toString(), signature);
        }
        Iterator it = res.keySet().iterator();
        while (it.hasNext()) {
            String name = (String) it.next();
            byte[] signature = (byte[]) res.get(name);
            System.out.println(name + "\t" + StringUtil.toHexString(signature));
        }
    }
}
