package com.bifrostbridge.testinfrastructure.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import com.bifrostbridge.testinfrastructure.dao.TISecurityException;
import com.bifrostbridge.testinfrastructure.model.Roles;
import com.bifrostbridge.testinfrastructure.model.User;

public class SystemUtils {

    public static byte[] generatePasswordHash(String s) {
        byte[] password = { 00 };
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(s.getBytes());
            password = md5.digest();
            return password;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return password;
    }

    public static void ensureAdminRole(User myUser, String string) throws TISecurityException {
        if ((myUser.getRole() != Roles.ADMIN)) {
            throw new TISecurityException(string);
        }
    }

    public static void ensureAdminOrBetaRole(User myUser, String string) throws TISecurityException {
        if ((myUser.getRole() != Roles.ADMIN)) {
            if ((myUser.getRole() != Roles.BETA_CANDIDATE)) {
                throw new TISecurityException(string);
            }
        }
    }

    public static void rejectCandidateRoles(User user, String string) throws TISecurityException {
        if ((user.getRole() == Roles.CANDIDATE) || (user.getRole() == Roles.BETA_CANDIDATE)) {
            throw new TISecurityException("User " + user + " does not have admin role");
        }
    }

    public static void ensureBetaCandidate(User user) throws TISecurityException {
        if (user.getRole() == Roles.ADMIN) {
            return;
        }
        if ((user.getRole() != Roles.BETA_CANDIDATE)) {
            throw new TISecurityException("User " + user + " must have BETA CANDIDATE role");
        }
    }
}
