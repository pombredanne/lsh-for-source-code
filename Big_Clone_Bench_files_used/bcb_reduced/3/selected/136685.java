package fi.hiit.cutehip.utils;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import fi.hiit.framework.crypto.SHA1Digest;
import fi.hiit.framework.utils.Helpers;

;

public class Puzzle {

    public static final int KEY_MAT_LENGTH = 48;

    public static final int SOLUTION_LENGTH = 8;

    public static final BigInteger ONES_MASK = new BigInteger("FFFFFFFFFFFFFFFF", 16);

    public static final BigInteger ZERO_MASK = new BigInteger("0", 16);

    public static final int MASK_LENGTH = 8;

    public static final int HIT_LENGTH = 16;

    public static final int RAND_LENGTH = 8;

    public Puzzle(byte[] rand, byte[] solution, byte[] hitI, byte[] hitR, int complexity) {
        if (rand != null) System.arraycopy(rand, 0, __rand, 0, rand.length);
        if (solution != null) System.arraycopy(solution, 0, __solution, 0, solution.length);
        System.arraycopy(hitI, 0, __hitI, 0, hitI.length);
        System.arraycopy(hitR, 0, __hitR, 0, hitR.length);
        if (complexity < 64 && complexity > 0) __complexity = complexity;
    }

    public Puzzle(long rand, long solution, byte[] hitI, byte[] hitR, int complexity) {
        this(null, null, hitI, hitR, complexity);
        __rand[0] = (byte) ((rand >> 56) & 0xFF);
        __rand[1] = (byte) ((rand >> 48) & 0xFF);
        __rand[2] = (byte) ((rand >> 40) & 0xFF);
        __rand[3] = (byte) ((rand >> 32) & 0xFF);
        __rand[4] = (byte) ((rand >> 24) & 0xFF);
        __rand[5] = (byte) ((rand >> 16) & 0xFF);
        __rand[6] = (byte) ((rand >> 8) & 0xFF);
        __rand[7] = (byte) (rand & 0xFF);
        __solution[0] = (byte) ((solution >> 56) & 0xFF);
        __solution[1] = (byte) ((solution >> 48) & 0xFF);
        __solution[2] = (byte) ((solution >> 40) & 0xFF);
        __solution[3] = (byte) ((solution >> 32) & 0xFF);
        __solution[4] = (byte) ((solution >> 24) & 0xFF);
        __solution[5] = (byte) ((solution >> 16) & 0xFF);
        __solution[6] = (byte) ((solution >> 8) & 0xFF);
        __solution[7] = (byte) (solution & 0xFF);
    }

    public int getComplexity() {
        return __complexity;
    }

    public void setComplexity(int complexity) {
        if (complexity < 64 && complexity > 0) __complexity = complexity;
    }

    public byte[] getHitR() {
        return __hitR;
    }

    public void setHitR(byte[] hitR) {
        System.arraycopy(hitR, 0, __hitR, 0, hitR.length);
    }

    public byte[] getHitI() {
        return __hitI;
    }

    public void setHitI(byte[] hitI) {
        System.arraycopy(hitI, 0, __hitI, 0, hitI.length);
    }

    public byte[] getSolution() {
        return __solution;
    }

    public void setSolution(byte[] solution) {
        System.arraycopy(solution, 0, __solution, 0, solution.length);
    }

    public byte[] getRandom() {
        return __rand;
    }

    public void setRandom(byte[] rand) {
        System.arraycopy(rand, 0, __rand, 0, rand.length);
    }

    public static boolean solve(Puzzle puzzle, long ms) {
        byte[] keyMaterial = new byte[Puzzle.KEY_MAT_LENGTH];
        byte[] randBytes = new byte[Puzzle.SOLUTION_LENGTH];
        byte[] solution = new byte[Puzzle.SOLUTION_LENGTH];
        int offset = 0;
        int shift = Puzzle.MASK_LENGTH * 8 - puzzle.getComplexity();
        long startTime = System.currentTimeMillis();
        long currentTime = System.currentTimeMillis();
        SHA1Digest md = new SHA1Digest();
        SecureRandom rand = new SecureRandom();
        BigInteger shiftMask = Puzzle.ONES_MASK.shiftRight(shift);
        System.arraycopy(puzzle.getRandom(), 0, keyMaterial, offset, puzzle.getRandom().length);
        offset = puzzle.getRandom().length;
        System.arraycopy(puzzle.getHitI(), 0, keyMaterial, offset, puzzle.getHitI().length);
        offset += puzzle.getHitI().length;
        System.arraycopy(puzzle.getHitR(), 0, keyMaterial, offset, puzzle.getHitR().length);
        offset += puzzle.getHitR().length;
        while (true) {
            currentTime = System.currentTimeMillis();
            if (currentTime - startTime > ms) break;
            rand.nextBytes(randBytes);
            System.arraycopy(randBytes, 0, keyMaterial, offset, randBytes.length);
            System.arraycopy(md.digest(keyMaterial), md.HASH_SIZE - Puzzle.SOLUTION_LENGTH, solution, 0, Puzzle.SOLUTION_LENGTH);
            BigInteger solutionMASK = new BigInteger(solution);
            if (Arrays.equals(solutionMASK.and(shiftMask).toByteArray(), Puzzle.ZERO_MASK.toByteArray())) {
                puzzle.setSolution(randBytes);
                return true;
            }
        }
        return false;
    }

    public static boolean verify(Puzzle puzzle) {
        byte[] keyMaterial = new byte[Puzzle.KEY_MAT_LENGTH];
        byte[] randBytes = new byte[Puzzle.SOLUTION_LENGTH];
        byte[] hash = new byte[Puzzle.SOLUTION_LENGTH];
        int offset = 0;
        int shift = Puzzle.MASK_LENGTH * 8 - puzzle.getComplexity();
        long startTime = System.currentTimeMillis();
        long currentTime = System.currentTimeMillis();
        SHA1Digest md = new SHA1Digest();
        SecureRandom rand = new SecureRandom();
        BigInteger shiftMask = Puzzle.ONES_MASK.shiftRight(shift);
        System.arraycopy(puzzle.getRandom(), 0, keyMaterial, offset, puzzle.getRandom().length);
        offset = puzzle.getRandom().length;
        System.arraycopy(puzzle.getHitI(), 0, keyMaterial, offset, puzzle.getHitI().length);
        offset = puzzle.getHitI().length;
        System.arraycopy(puzzle.getHitR(), 0, keyMaterial, offset, puzzle.getHitR().length);
        offset = puzzle.getHitR().length;
        System.arraycopy(puzzle.getSolution(), 0, keyMaterial, offset, puzzle.getSolution().length);
        System.arraycopy(md.digest(keyMaterial), md.HASH_SIZE - Puzzle.SOLUTION_LENGTH, hash, 0, Puzzle.SOLUTION_LENGTH);
        BigInteger solutionMASK = new BigInteger(hash);
        if (Arrays.equals(solutionMASK.and(shiftMask).toByteArray(), Puzzle.ZERO_MASK.toByteArray())) return true;
        return false;
    }

    private byte[] __rand = new byte[Puzzle.RAND_LENGTH];

    private byte[] __hitI = new byte[Puzzle.HIT_LENGTH];

    private byte[] __hitR = new byte[Puzzle.HIT_LENGTH];

    ;

    private byte[] __solution = new byte[Puzzle.RAND_LENGTH];

    ;

    private int __complexity = 64;
}
