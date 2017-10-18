package com.google.code.booktogether.service.util;

import java.security.MessageDigest;
import java.security.SecureRandom;

/**
 * 사용자 패스워드 만들때 쓰임
 * @author ParkHaeCheol
 *
 */
public class PasswordAuthenticator {

    /**
	 * 패스워드 Key역할하는 Salt만들기 
	 * @return
	 */
    public static byte[] generatorSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[12];
        random.nextBytes(salt);
        return salt;
    }

    /**
	 * 패스워드 암호화 하기
	 * @param password 입력문자열
	 * @param salt 패스워드 임의이 키
	 * @return 암호화된 패스워드
	 * @throws Exception
	 */
    public static byte[] createPasswordDigest(String password, byte[] salt) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(salt);
        md.update(password.getBytes("UTF8"));
        byte[] digest = md.digest();
        return digest;
    }
}
