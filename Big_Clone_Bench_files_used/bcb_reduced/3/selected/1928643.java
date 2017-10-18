package com.baldwin.www.config;

import java.security.MessageDigest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import com.baldwin.www.datahandler.BaseDAO;
import com.baldwin.www.datahandler.DatabaseHandler;

/** �û��Ļ����,�������û��Ļ���ݵ��úͱ���
 */
public class UserDao extends BaseDAO {

    /**
     * ���캯�� ���û���Ĺ��캯��
     * 
     * @param dbh
     */
    public UserDao(DatabaseHandler dbh) {
        super(dbh);
    }

    private String sql;

    private ResultSet rs;

    /**<b>����:</b>�����û�
			 * @param	userName	�û���
			 * @param	passwd		����
			 * @return	true		�ɹ�<br>
			 * 			false	 	ʧ��
			 */
    public boolean add(UserInfo info) {
        sql = String.format("INSERT INTO tab_UserInfo (UserName,PassWord,Sex,Email,UserType,CreateTime)VALUES('%s','%s','%s','%s','%s',current_timestamp)", info.getUserName(), info.getPassWord(), info.getSex(), info.getEmail(), info.getUserType());
        int ret = dbh.executeUpdate(sql);
        if (ret < 1) return false;
        return true;
    }

    /**<b>����:</b>��֤�û�
			 * @param userName	�û���
			 * @param passwd	�����md5ֵ
			 * @return	true	�ɹ�<br>
			 * 			false	ʧ��
			 */
    public boolean validate(String userName, String passwd) {
        sql = String.format("SELECT passwd FROM tab_UserInfo WHERE name='%s'", userName);
        rs = dbh.executeQuery(sql);
        try {
            if (!rs.next()) {
                rs.close();
                return false;
            }
            String passwd1 = rs.getString("passwd");
            rs.close();
            if (passwd.equals(getMD5(passwd1))) return true; else return false;
        } catch (SQLException e) {
            return false;
        }
    }

    /**<b>����:</b>����û�id
			 * @param name	�û���
			 * @return		�û�id	�ɹ�<br>
			 * 				null	ʧ��
			 */
    public String getId(String name) {
        sql = "SELECT id FROM tab_UserInfo WHERE name='" + name + "'";
        rs = dbh.executeQuery(sql);
        try {
            if (!rs.next()) {
                rs.close();
                return null;
            }
            String id = rs.getString("id");
            rs.close();
            return id;
        } catch (SQLException e) {
            return null;
        }
    }

    public int getCount() {
        int total = 0;
        try {
            String sql = "SELECT COUNT(id) as total FROM tab_UserInfo ";
            ResultSet rs = dbh.executeQuery(sql);
            if (rs != null) {
                if (rs.next()) {
                    total = rs.getInt("total");
                }
                rs.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return total;
    }

    public int getCount(String userType) {
        int total = 0;
        try {
            String sql = "SELECT COUNT(id) as total FROM tab_UserInfo where userType='" + userType + "'";
            ResultSet rs = dbh.executeQuery(sql);
            if (rs != null) {
                if (rs.next()) {
                    total = rs.getInt("total");
                }
                rs.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return total;
    }

    /**<b>����:</b>�������
			 * @param	userId	�û�id
			 * @return	�û�����	�ɹ�<br>
			 * 			null	ʧ��
			 */
    public UserInfo getInfobyname(String username) {
        sql = String.format("SELECT * FROM tab_UserInfo WHERE UserName='%s'", username);
        rs = dbh.executeQuery(sql);
        UserInfo info = new UserInfo();
        try {
            if (rs != null) {
                rs.next();
                info = new UserInfo();
                info.setId((rs.getInt("id")));
                info.setUserName(dbh.ISO2GBK(rs.getString("UserName")));
                info.setPassWord(rs.getString("PassWord"));
                info.setSex(rs.getString("Sex"));
                info.setEmail(rs.getString("Email"));
                info.setCreateTime(rs.getString("CreateTime"));
                info.setUserType(rs.getString("UserType"));
                rs.close();
            }
            return info;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public UserInfo getInfo(String userId) {
        sql = "SELECT * FROM tab_UserInfo WHERE id='" + userId + "'";
        rs = dbh.executeQuery(sql);
        UserInfo info = new UserInfo();
        try {
            if (rs != null) {
                rs.next();
                info = new UserInfo();
                info.setId((rs.getInt("id")));
                info.setUserName(dbh.ISO2GBK(rs.getString("UserName")));
                info.setPassWord(rs.getString("PassWord"));
                info.setSex(rs.getString("Sex"));
                info.setEmail(rs.getString("Email"));
                info.setCreateTime(rs.getString("CreateTime"));
                info.setUserType(rs.getString("UserType"));
                rs.close();
            }
            return info;
        } catch (SQLException e) {
            return null;
        }
    }

    public ArrayList<UserInfo> getUserList(int offset, int pagesize) {
        String sql = "SELECT * FROM tab_UserInfo";
        sql = convertPageSQL(sql, offset, pagesize);
        ResultSet rs = dbh.executeQuery(sql);
        ArrayList<UserInfo> list = new ArrayList<UserInfo>();
        if (rs != null) {
            try {
                while (rs.next()) {
                    UserInfo info = new UserInfo();
                    info.setId((rs.getInt("id")));
                    info.setUserName(dbh.ISO2GBK(rs.getString("UserName")));
                    info.setPassWord(rs.getString("PassWord"));
                    info.setSex(rs.getString("Sex"));
                    info.setEmail(rs.getString("Email"));
                    info.setCreateTime(rs.getString("CreateTime"));
                    info.setUserType(rs.getString("UserType"));
                    list.add(info);
                    info = null;
                }
                rs.close();
                return list;
            } catch (Exception e) {
                e.printStackTrace();
                return list;
            }
        }
        return list;
    }

    public ArrayList<UserInfo> getUserList(String userType, int offset, int pagesize) {
        String sql = "SELECT * FROM tab_UserInfo where UserType='" + userType + "'";
        sql = convertPageSQL(sql, offset, pagesize);
        ResultSet rs = dbh.executeQuery(sql);
        ArrayList<UserInfo> list = new ArrayList<UserInfo>();
        if (rs != null) {
            try {
                while (rs.next()) {
                    UserInfo info = new UserInfo();
                    info.setId((rs.getInt("id")));
                    info.setUserName(dbh.ISO2GBK(rs.getString("UserName")));
                    info.setPassWord(rs.getString("PassWord"));
                    info.setSex(rs.getString("Sex"));
                    info.setEmail(rs.getString("Email"));
                    info.setCreateTime(rs.getString("CreateTime"));
                    info.setUserType(rs.getString("UserType"));
                    list.add(info);
                    info = null;
                }
                rs.close();
                return list;
            } catch (Exception e) {
                e.printStackTrace();
                return list;
            }
        }
        return list;
    }

    /**<b>����:</b>�����û�����
			 * @param 	info	�û�������
			 * @return	true	�ɹ�<br>
			 *			false	ʧ��
			 */
    public boolean updateInfo(UserInfo info) {
        String sql = String.format("UPDATE tab_UserInfo SET  " + "PassWord='%s'," + "Sex='%s'," + "Email='%s'," + "UserType='%s'  WHERE id=%s", info.getPassWord(), info.getSex(), info.getEmail(), info.getUserType(), info.getId());
        int ret = dbh.executeUpdate(sql);
        if (ret > 0) return true; else return false;
    }

    /**<b>����:</b>�����û�����¼ʱ��
			 * @param	userId		�û�id
			 * @return	true		�ɹ�<br>
			 * 			false		ʧ��
			 */
    public boolean updateLastLoginTime(String userId) {
        sql = "UPDATE tab_UserInfo SET lastLoginTime=current_timestamp WHERE id='" + userId + "'";
        dbh.executeUpdate(sql);
        return true;
    }

    /**<b>����:</b>�����û����һ�η�����Ʒ��ʱ��
			 * @param	userId		�û�id
			 * @return	true		�ɹ�<br>
			 * 			false		ʧ��
			 */
    public boolean updateLastPublishTime(String userId) {
        sql = "UPDATE tab_UserInfo SET lastPublishTime=current_timestamp WHERE id='" + userId + "'";
        int ret = dbh.executeUpdate(sql);
        if (ret > 0) return true; else return false;
    }

    /**<b>����:</b>ɾ���û�
			 * @param 	userName	�û���
			 * @return	true		�ɹ�<br>
			 * 			false		ʧ��
			 */
    public boolean deletebyname(String userName) {
        sql = "DELETE FROM tab_UserInfo WHERE name='" + userName + "'";
        dbh.executeUpdate(sql);
        return true;
    }

    /**<b>����:</b>ɾ���û�
			 * @param 	userName	�û���
			 * @return	true		�ɹ�<br>
			 * 			false		ʧ��
			 */
    public boolean delete(String id) {
        sql = "DELETE FROM tab_UserInfo WHERE id='" + id + "'";
        dbh.executeUpdate(sql);
        return true;
    }

    /**<b>����:</b>����ַ��md5��
			 * @param 	s			Ҫ���ܵĴ�
			 * @return	���ܺ�Ĵ�	�ɹ�<br>
			 * 			null		ʧ��
			 */
    public String getMD5(String s) {
        char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
        try {
            byte[] strTemp = s.getBytes();
            MessageDigest mdTemp = MessageDigest.getInstance("MD5");
            mdTemp.update(strTemp);
            byte[] md = mdTemp.digest();
            int j = md.length;
            char str[] = new char[j * 2];
            int k = 0;
            for (int i = 0; i < j; i++) {
                byte byte0 = md[i];
                str[k++] = hexDigits[byte0 >>> 4 & 0xf];
                str[k++] = hexDigits[byte0 & 0xf];
            }
            return new String(str);
        } catch (Exception e) {
            return null;
        }
    }

    /**<b>����:</b>�����û��ļ���
			 * @param 	userId	�û�id	
			 * @param 	value	�������ӻ������,>0Ϊ����,<0Ϊ����
			 * @return	true	�ɹ�<br>
			 * 			false	ʧ��
			 */
    public boolean updateGrade(String userId, int value) {
        sql = "UPDATE tab_UserInfo SET grade=grade" + value + " WHERE id='" + userId + "'";
        int ret = dbh.executeUpdate(sql);
        if (ret < 1) return false;
        return true;
    }

    /**<b>����:</b>�Ƿ������Ƶ��û���
			 * @param 	userName	�û���
			 * @return	true		�����Ƶ�<br>
			 * 			false		�����Ƶ�
			 */
    public boolean isLimit(String userName) {
        sql = "SELECT name FROM tab_user_limit WHERE name='" + userName + "'";
        rs = dbh.executeQuery(sql);
        try {
            if (rs.next()) {
                rs.close();
                return true;
            }
            rs.close();
            return false;
        } catch (SQLException e) {
            return true;
        }
    }

    public int isExist(String name) {
        String sql = String.format("SELECT count(ID) AS total FROM tab_UserInfo WHERE UserName='%s'", name);
        ResultSet rs = dbh.executeQuery(sql);
        int total = 0;
        if (rs != null) {
            try {
                rs.next();
                total = rs.getInt("total");
                rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
                return total;
            }
        }
        return total;
    }

    /**<b>����:</b>��������û�����б�
			 * @return	�����б�	�ɹ�<br>
			 * 			null	ʧ��
			 */
    public String[] getLimitList() {
        try {
            sql = "SELECT count(*) FROM tab_user_limit";
            rs = dbh.executeQuery(sql);
            rs.next();
            int count = rs.getInt(1);
            String list[] = new String[count];
            rs.close();
            sql = "SELECT name FROM tab_user_limit";
            rs = dbh.executeQuery(sql);
            int i = 0;
            while (rs.next()) {
                list[i] = dbh.ISO2GBK(rs.getString("name"));
                i++;
            }
            rs.close();
            return list;
        } catch (SQLException e) {
            return null;
        }
    }

    /**<b>����:</b>�����û�������
			 * @param	userName	�û���
			 * @return	true		�ɹ�<br>
			 * 			false		ʧ��
			 */
    public boolean addLimit(String userName) {
        sql = "INSERT INTO tab_user_limit (name) VALUES('" + userName + "')";
        int ret = dbh.executeUpdate(sql);
        if (ret < 1) return false; else return true;
    }

    /**<b>����:</b>ɾ���û�������
			 * @param	userName	�û���
			 * @return	true		�ɹ�<br>
			 * 			false		ʧ��
			 */
    public boolean delLimit(String userName) {
        sql = "DELETE FROM tab_user_limit WHERE name='" + userName + "'";
        int ret = dbh.executeUpdate(sql);
        if (ret < 1) return false; else return true;
    }
}
