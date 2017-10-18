package com.marianmedla.jane16.lib.txt;

import java.security.MessageDigest;
import com.marianmedla.jane16.lib.U;

/**
 * Basic entity of word in a text.
 * Every word is part of a text ( even just one word is a part of one word text)
 * @author marianmedla
 *
 */
public class Word {

    public String text;

    public char[] chtext;

    public int position;

    public String md5;

    public Word(String text, int position, MessageDigest msgdiggest) {
        this.text = text;
        this.chtext = text.toCharArray();
        this.position = position;
        byte[] b = (text + position).getBytes();
        md5 = U.toHex(msgdiggest.digest(b));
    }
}
