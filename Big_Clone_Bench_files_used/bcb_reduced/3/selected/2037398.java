package cowsultants.itracker.ejb.client.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.Vector;
import sun.misc.BASE64Encoder;
import cowsultants.itracker.ejb.client.exceptions.PasswordException;
import cowsultants.itracker.ejb.client.models.NameValuePairModel;
import cowsultants.itracker.ejb.client.models.PermissionModel;
import cowsultants.itracker.ejb.client.models.UserModel;
import cowsultants.itracker.ejb.client.resources.ITrackerResources;

public class UserUtilities implements AuthenticationConstants {

    public static final char[] alphabet = new char[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'm', 'n', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' };

    public static final int STATUS_DELETED = -1;

    public static final int STATUS_ACTIVE = 1;

    public static final int STATUS_LOCKED = 2;

    /** User Admin Permission.  Currently this is equivalent to super user, since the permission can't be granted, and is only available to an admin. */
    public static final int PERMISSION_USER_ADMIN = -1;

    /** Product Admin Permission */
    public static final int PERMISSION_PRODUCT_ADMIN = 1;

    /** Issue Create Permission */
    public static final int PERMISSION_CREATE = 2;

    /** Issue Edit Permission.  Users with this permission can edit any issue in the project. */
    public static final int PERMISSION_EDIT = 3;

    /** Issue Close Permission.  Users with this permission can close issues in the project. */
    public static final int PERMISSION_CLOSE = 4;

    /** Issue Assign to Self Permission.  Users with this permission can assign issues to themselves. */
    public static final int PERMISSION_ASSIGN_SELF = 5;

    /** Issue Assign to Others Permissions.  Users with this permission can assign issues to anyone, given than those users have the ability to recieve the assignment. */
    public static final int PERMISSION_ASSIGN_OTHERS = 6;

    /** View All Issues Permission.  Users can view all issues in the project. */
    public static final int PERMISSION_VIEW_ALL = 7;

    /** View Users Issues Permission.  Users can view thier own issues.  This includes ones they are the creator or owner of. */
    public static final int PERMISSION_VIEW_USERS = 8;

    /** Edit Users Issues Permission.  Users with this permission can edit any issue they created or own.
      * They are limited to editing the description, adding history entries, and adding attachments. */
    public static final int PERMISSION_EDIT_USERS = 9;

    /** Issue Unassign Self Permission.  Users with this permission can unassign issues they own. */
    public static final int PERMISSION_UNASSIGN_SELF = 10;

    /** Issue Assignable.  Users with this permission can be assigned any issue in the system.  To determine if a user can
      * be assigned an issue, it will be a combination of users with EDIT_ALL, users with EDIT_USERS if they are the creator,
      * and users with this permission and EDIT_USERS. */
    public static final int PERMISSION_ASSIGNABLE = 11;

    /** Create for Others.  Users with this permission are allowed to create issues on behalf of other users.  The system
      * will treat the issue as if the other user had created it.  The actual creator will be logged in the audit log. */
    public static final int PERMISSION_CREATE_OTHERS = 12;

    /** Full edit permission.  This defines what levelof editing a user has for an issue.  Without this permission, users will
        be limited to editing only the description, attachments, custom fields, and history of an issue. */
    public static final int PERMISSION_EDIT_FULL = 13;

    public static final int REGISTRATION_TYPE_ADMIN = 1;

    public static final int REGISTRATION_TYPE_SELF = 2;

    public static final int REGISTRATION_TYPE_IMPORT = 3;

    public static final int PREF_HIDE_ASSIGNED = 1;

    public static final int PREF_HIDE_UNASSIGNED = 2;

    public static final int PREF_HIDE_CREATED = 4;

    public static final int PREF_HIDE_WATCHED = 8;

    public UserUtilities() {
    }

    public static String getStatusName(int value) {
        return getStatusName(value, ITrackerResources.getLocale());
    }

    public static String getStatusName(int value, Locale locale) {
        return ITrackerResources.getString(ITrackerResources.KEY_BASE_USER_STATUS + value, locale);
    }

    public static HashMap getStatusNames() {
        return getStatusNames(ITrackerResources.getLocale());
    }

    public static HashMap getStatusNames(Locale locale) {
        HashMap statuses = new HashMap();
        statuses.put(Integer.toString(STATUS_DELETED), getStatusName(STATUS_DELETED, locale));
        statuses.put(Integer.toString(STATUS_ACTIVE), getStatusName(STATUS_ACTIVE, locale));
        statuses.put(Integer.toString(STATUS_LOCKED), getStatusName(STATUS_LOCKED, locale));
        return statuses;
    }

    public static String getPermissionName(int value) {
        return getPermissionName(value, ITrackerResources.getLocale());
    }

    public static String getPermissionName(int value, Locale locale) {
        return ITrackerResources.getString(ITrackerResources.KEY_BASE_PERMISSION + value, locale);
    }

    public static NameValuePairModel[] getPermissionNames() {
        return getPermissionNames(ITrackerResources.getLocale());
    }

    public static NameValuePairModel[] getPermissionNames(Locale locale) {
        NameValuePairModel[] permissions = new NameValuePairModel[13];
        permissions[0] = new NameValuePairModel(getPermissionName(PERMISSION_CREATE, locale), Integer.toString(PERMISSION_CREATE));
        permissions[1] = new NameValuePairModel(getPermissionName(PERMISSION_CREATE_OTHERS, locale), Integer.toString(PERMISSION_CREATE_OTHERS));
        permissions[2] = new NameValuePairModel(getPermissionName(PERMISSION_EDIT, locale), Integer.toString(PERMISSION_EDIT));
        permissions[3] = new NameValuePairModel(getPermissionName(PERMISSION_EDIT_USERS, locale), Integer.toString(PERMISSION_EDIT_USERS));
        permissions[4] = new NameValuePairModel(getPermissionName(PERMISSION_EDIT_FULL, locale), Integer.toString(PERMISSION_EDIT_FULL));
        permissions[5] = new NameValuePairModel(getPermissionName(PERMISSION_CLOSE, locale), Integer.toString(PERMISSION_CLOSE));
        permissions[6] = new NameValuePairModel(getPermissionName(PERMISSION_ASSIGNABLE, locale), Integer.toString(PERMISSION_ASSIGNABLE));
        permissions[7] = new NameValuePairModel(getPermissionName(PERMISSION_ASSIGN_SELF, locale), Integer.toString(PERMISSION_ASSIGN_SELF));
        permissions[8] = new NameValuePairModel(getPermissionName(PERMISSION_UNASSIGN_SELF, locale), Integer.toString(PERMISSION_UNASSIGN_SELF));
        permissions[9] = new NameValuePairModel(getPermissionName(PERMISSION_ASSIGN_OTHERS, locale), Integer.toString(PERMISSION_ASSIGN_OTHERS));
        permissions[10] = new NameValuePairModel(getPermissionName(PERMISSION_VIEW_ALL, locale), Integer.toString(PERMISSION_VIEW_ALL));
        permissions[11] = new NameValuePairModel(getPermissionName(PERMISSION_VIEW_USERS, locale), Integer.toString(PERMISSION_VIEW_USERS));
        permissions[12] = new NameValuePairModel(getPermissionName(PERMISSION_PRODUCT_ADMIN, locale), Integer.toString(PERMISSION_PRODUCT_ADMIN));
        return permissions;
    }

    /**
      * Genrates a new random password.  The password that is returned is in plain text.
      * @return a new ramdon plaintext password
      */
    public static String generatePassword() throws PasswordException {
        StringBuffer buf = new StringBuffer();
        Random rand = new Random();
        for (int i = 0; i < 8; i++) {
            buf.append((rand.nextInt(2) == 0 ? Character.toUpperCase(alphabet[rand.nextInt(34)]) : alphabet[rand.nextInt(34)]));
        }
        return buf.toString();
    }

    /**
      * Returns an encrypted (digest) password from a plain text password.
      * @param password the plain text password to encrypt.
      * @return the encrypted password
      */
    public static String encryptPassword(String password) throws PasswordException {
        String hash = null;
        if (password != null && !password.equals("")) {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(password.getBytes("UTF-8"));
                byte raw[] = md.digest();
                hash = (new BASE64Encoder()).encode(raw);
            } catch (NoSuchAlgorithmException nsae) {
                throw new PasswordException(PasswordException.SYSTEM_ERROR);
            } catch (UnsupportedEncodingException uee) {
                throw new PasswordException(PasswordException.SYSTEM_ERROR);
            }
        }
        return hash;
    }

    /**
      * Checks to see if the user is a super user.
      * @param permissions the permissions of a user to check
      * @return true is the user is a super user
      */
    public static boolean isSuperUser(HashMap permissions) {
        if (permissions == null) {
            return false;
        }
        Boolean superUser = (Boolean) permissions.get(Integer.toString(-1));
        if (superUser != null && superUser.booleanValue()) {
            return true;
        }
        return false;
    }

    /**
      * Returns true if the user has the required permission in any project.
      * @param permissions a HashMap of the user's permissions
      * @param permissionNeeded the permission to check for
      */
    public static boolean hasPermission(HashMap permissions, int permissionNeeded) {
        if (permissions == null) {
            return false;
        }
        Set keySet = permissions.keySet();
        Boolean superUser = (Boolean) permissions.get(Integer.toString(-1));
        if (superUser != null && superUser.booleanValue()) {
            return true;
        }
        for (Iterator iterator = keySet.iterator(); iterator.hasNext(); ) {
            Integer projectId = (Integer) iterator.next();
            if (hasPermission(permissions, projectId, permissionNeeded)) {
                return true;
            }
        }
        return false;
    }

    /**
      * Returns true if the user has any of required permissions in any project.
      * @param permissions a HashMap of the user's permissions
      * @param permissionsNeeded a list of permissions that can fulfill the permission check
      */
    public static boolean hasPermission(HashMap permissions, int[] permissionsNeeded) {
        if (permissions == null) {
            return false;
        }
        Set keySet = permissions.keySet();
        Boolean superUser = (Boolean) permissions.get(Integer.toString(-1));
        if (superUser != null && superUser.booleanValue()) {
            return true;
        }
        for (Iterator iterator = keySet.iterator(); iterator.hasNext(); ) {
            Integer projectId = (Integer) iterator.next();
            if (hasPermission(permissions, projectId, permissionsNeeded)) {
                return true;
            }
        }
        return false;
    }

    /**
      * Returns true if the user has the required permission for the given project.
      * @param permissions a HashMap of the user's permissions
      * @param projectId the project that the permission is required for
      * @param permissionNeeded the permission to check for
      */
    public static boolean hasPermission(HashMap permissions, Integer projectId, int permissionNeeded) {
        if (permissions == null) {
            return false;
        }
        Boolean superUser = (Boolean) permissions.get(Integer.toString(-1));
        if (superUser != null && superUser.booleanValue()) {
            return true;
        }
        if (projectId != null && projectId.intValue() > 0) {
            HashSet projectPermissions = (HashSet) permissions.get(projectId);
            if (projectPermissions != null && projectPermissions.contains(Integer.toString(permissionNeeded))) {
                return true;
            }
        }
        return false;
    }

    /**
      * Returns true if the user has any of required permissions for the given project.
      * @param permissions a HashMap of the user's permissions
      * @param projectId the project that the permission is required for
      * @param permissionsNeeded a list of permissions that can fulfill the permission check
      */
    public static boolean hasPermission(HashMap permissions, Integer projectId, int[] permissionsNeeded) {
        if (permissions == null) {
            return false;
        }
        Boolean superUser = (Boolean) permissions.get(Integer.toString(-1));
        if (superUser != null && superUser.booleanValue()) {
            return true;
        }
        if (projectId != null && projectId.intValue() > 0) {
            HashSet projectPermissions = (HashSet) permissions.get(projectId);
            if (projectPermissions != null) {
                for (int i = 0; i < permissionsNeeded.length; i++) {
                    if (projectPermissions.contains(Integer.toString(permissionsNeeded[i]))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static String getInitial(String name) {
        return (name != null && name.length() > 0 ? name.substring(0, 1).toUpperCase() + "." : "");
    }

    public static String getPermissionsToString(HashMap permissions) {
        StringBuffer buf = new StringBuffer();
        if (permissions != null) {
            Set keySet = permissions.keySet();
            Boolean superAdmin = (Boolean) permissions.get(Integer.toString(-1));
            if (superAdmin != null && superAdmin.booleanValue()) {
                buf.append("ITracker Super User\n\n");
            }
            for (Iterator iterator = keySet.iterator(); iterator.hasNext(); ) {
                Object project = iterator.next();
                if (project instanceof Integer) {
                    Integer projectId = (Integer) project;
                    if (projectId.intValue() >= 0) {
                        HashSet projectPermissions = (HashSet) permissions.get(projectId);
                        if (projectPermissions != null) {
                            for (Iterator iteratorInner = projectPermissions.iterator(); iteratorInner.hasNext(); ) {
                                buf.append("Project: " + projectId + "    Permission: " + getPermissionName(Integer.parseInt((String) iteratorInner.next())) + "\n");
                            }
                        }
                    }
                }
            }
        }
        if (buf.length() == 0) {
            buf.append("User has no permissions.");
        }
        return buf.toString();
    }

    public static PermissionModel[] createPermissionArray(UserModel user, Integer projectId, int[] permissions) {
        PermissionModel[] permissionsArray = new PermissionModel[0];
        Vector permissionsVector = new Vector();
        if (user.isSuperUser()) {
            permissionsVector.add(new PermissionModel(new Integer(-1), -1, user.getLogin(), user.getId()));
        }
        for (int i = 0; i < permissions.length; i++) {
            permissionsVector.add(new PermissionModel(projectId, permissions[i], user.getLogin(), user.getId()));
        }
        permissionsArray = new PermissionModel[permissionsVector.size()];
        permissionsVector.copyInto(permissionsArray);
        return permissionsArray;
    }

    public static HashMap permissionsArrayToMap(PermissionModel[] permissionsArray) {
        HashMap permissionMap = new HashMap();
        if (permissionsArray != null) {
            for (int i = 0; i < permissionsArray.length; i++) {
                if (permissionsArray[i].getProjectId() == null || permissionsArray[i].getUserId() == null) {
                    continue;
                }
                if (permissionsArray[i].getProjectId().intValue() == -1 && permissionsArray[i].getPermissionType() == -1) {
                    permissionMap.put(Integer.toString(-1), new Boolean(true));
                } else {
                    if (permissionMap.get(permissionsArray[i].getProjectId()) == null) {
                        HashSet projectPermissions = new HashSet();
                        permissionMap.put(permissionsArray[i].getProjectId(), projectPermissions);
                    }
                    ((HashSet) permissionMap.get(permissionsArray[i].getProjectId())).add(Integer.toString(permissionsArray[i].getPermissionType()));
                }
            }
        }
        return permissionMap;
    }

    public static PermissionModel[] permissionsMapToArray(HashMap permissions) {
        PermissionModel[] permissionsArray = new PermissionModel[0];
        Vector permissionsVector = new Vector();
        if (permissions != null) {
            Set keySet = permissions.keySet();
            for (Iterator iterator = keySet.iterator(); iterator.hasNext(); ) {
                Object project = iterator.next();
                if (!(project instanceof Integer)) {
                    continue;
                }
                Integer projectId = (Integer) project;
                if (projectId.intValue() < 0) {
                    continue;
                }
                HashSet projectPermissions = (HashSet) permissions.get(projectId);
                if (projectPermissions == null) {
                    continue;
                }
                for (Iterator iteratorInner = projectPermissions.iterator(); iteratorInner.hasNext(); ) {
                    try {
                        permissionsVector.add(new PermissionModel(projectId, Integer.parseInt((String) iteratorInner.next())));
                    } catch (Exception e) {
                    }
                }
            }
        }
        permissionsArray = new PermissionModel[permissionsVector.size()];
        permissionsVector.copyInto(permissionsArray);
        return permissionsArray;
    }

    /**
      * Returns whether the user is currently hiding a particular section on the myItracker page.
      * @param section the section to check if it is hidden
      * @param an integer of all sections the user is hiding
      * @return true if the current section is hidden
      */
    public static boolean hideIndexSection(int section, int sections) {
        return ((section & sections) == section);
    }

    public static Integer[] getHiddenIndexSections(int sections) {
        Vector sectionsVector = new Vector();
        if (hideIndexSection(PREF_HIDE_ASSIGNED, sections)) {
            sectionsVector.add(new Integer(PREF_HIDE_ASSIGNED));
        }
        if (hideIndexSection(PREF_HIDE_UNASSIGNED, sections)) {
            sectionsVector.add(new Integer(PREF_HIDE_UNASSIGNED));
        }
        if (hideIndexSection(PREF_HIDE_CREATED, sections)) {
            sectionsVector.add(new Integer(PREF_HIDE_CREATED));
        }
        if (hideIndexSection(PREF_HIDE_WATCHED, sections)) {
            sectionsVector.add(new Integer(PREF_HIDE_WATCHED));
        }
        Integer[] sectionsArray = new Integer[sectionsVector.size()];
        sectionsVector.copyInto(sectionsArray);
        return sectionsArray;
    }
}
