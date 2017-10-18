package md5cracker;

import java.security.MessageDigest;
import distributeCrack.Combinations;
import tenzin.CharNotFoundException;

/**
 * This Class presents methods and utility to perform a brute force 
 * algorithm against a user supplied MD5 hex string.It has useful functionality 
 * to specify a range of string to search, ability to stop an incomplete search 
 * to get the string under analysis and also specify the alphabet from 
 * which the string is generated. It uses the class tenzin.Fstring 
 * to generate strings and specify the alphabet list.
 * 
 * @author Tenzin Dakpa 
 * @version 12 Dec 2008
 */
public class MD5Cracker {

    private String originalKey;

    private Combinations myf;

    private String seed;

    public boolean runCrack;

    public int loopCount;

    /**
     * the Constructor takes the MD5 hex string to be cracked, a list of 
     * alphabet in the form of a string eg "abc" or "123" and an arbitrary
     * string  which is the users guess
     */
    public MD5Cracker(String givenKey, String alphabet, String startseed) {
        originalKey = givenKey;
        myf = new Combinations(alphabet);
        seed = startseed;
        runCrack = true;
        loopCount = 0;
    }

    /**
     *  
     * A method that returns a hex sting from a given string using the MD5 hash.
     * The return string is in lower case.
     * @param  toCode  "mySecretKey"
     * @return     md5 hash 
     */
    public String computeMD5(String toCode) {
        MessageDigest md;
        byte rr[];
        String result = "";
        try {
            md = MessageDigest.getInstance("MD5");
            rr = md.digest(toCode.getBytes("US-ASCII"));
            String tempS = "";
            int j = 0;
            for (int i = 0; i < 16; i++) {
                tempS = String.format("%x", rr[i]);
                j = tempS.length();
                if (j == 2) {
                    result = result.concat(tempS);
                } else if (j == 1) {
                    result = result.concat("0" + tempS);
                } else if (j > 2) {
                    result = result.concat(tempS.substring(14));
                }
            }
        } catch (Exception e) {
            System.out.println("excption in method computeMD5  " + e.getMessage());
        }
        return result;
    }

    /**
     * this method in conjunction with the findKey method allows the user to get
     * the string computed in the last run.The parameter determines only the 
     * printout message.These are not necessary and can be safely ignored.
     * note:  not much use in the remote clients
     * @param result findKey("thisGuess","thatGuess")
     * @return string "correctGuess"
     */
    public String getResult(boolean result) {
        if (result) {
            System.out.println("!!!---Crack succesfull: key is " + seed + "and was found after " + loopCount + "loop");
        } else System.out.println("Crack Unsuccesfull: last key tested is " + seed + " after " + loopCount + "loops ");
        return seed;
    }

    /**
     *  
     * A method that cycles through all the string permutation between the parameter 
     * one and two, looking for the string that gives the hash key stored in an 
     * instance of this class.
     * Note that it does not bother  to check the endString. 
     * @param  newSeed   "fromhere"
     * @param  ensString  "tothere" 
     * @return true if the hash was successful false otherwise
     */
    public boolean findKey(long newSeed, long endString) throws CharNotFoundException {
        seed = myf.getKey(newSeed);
        String tempSeed = new String(seed);
        String hash;
        int i = 0;
        loopCount = 0;
        String prev;
        runCrack = true;
        do {
            hash = this.computeMD5(tempSeed);
            prev = new String(tempSeed);
            loopCount = i;
            i++;
            tempSeed = myf.getKey(newSeed + i);
        } while (!(hash.equals(originalKey) || (i == endString) || !runCrack));
        if (hash.equals(originalKey)) {
            System.out.println("!!!---Crack successful: key is " + prev + " and it's hash is " + hash + "found after " + i + "loop");
            seed = prev;
            return true;
        } else {
            System.out.println("Crack unsuccessful:" + "no key found between ( " + newSeed + " )and ( " + endString + " ) after " + i + " loop");
            seed = prev;
            return false;
        }
    }
}
