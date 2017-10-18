package ge.telasi.tasks.model;

import java.io.Serializable;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import sun.misc.BASE64Encoder;

/**
 * @author dimitri
 */
@Entity
@Table(name = "users")
@NamedQueries({ @NamedQuery(name = "User.all", query = "SELECT u FROM User u ORDER BY u.username"), @NamedQuery(name = "User.allActive", query = "SELECT u FROM User u WHERE u.status = 1 AND u.isNewBit=0 ORDER BY u.username"), @NamedQuery(name = "User.authorize", query = "SELECT u FROM User u WHERE u.username = :username"), @NamedQuery(name = "User.activeAdmins", query = "SELECT u FROM User u WHERE u.adminBit = 1 AND u.status = 1 AND u.isNewBit=0 ORDER BY u.username") })
public class User extends Modifiable implements Serializable {

    private static final long serialVersionUID = -5340793597710585040L;

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "USERS_SEQ")
    @SequenceGenerator(name = "USERS_SEQ", sequenceName = "USERS_SEQ", allocationSize = 1)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "structure_id")
    private Structure structure;

    @Column(name = "username")
    private String username;

    @Column(name = "password")
    private String password;

    @Column(name = "code")
    private String code;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "first_name2")
    private String firstName2;

    @Column(name = "last_name2")
    private String lastName2;

    @Column(name = "position")
    private String position;

    @Column(name = "phone_work")
    private String workPhone;

    @Column(name = "mobile")
    private String mobile;

    @Column(name = "location")
    private String location;

    @Column(name = "email")
    private String email;

    @Column(name = "is_new")
    private int isNewBit;

    @Column(name = "is_admin")
    private int adminBit;

    @Column(name = "notify_by_email")
    private int notifyByEmailBit;

    @Column(name = "notify_by_mobile")
    private int notifyByMobileBit;

    @Column(name = "last_task_seq")
    private int lastTaskSequence;

    @ManyToMany(mappedBy = "users")
    private List<Group> groups = new ArrayList<Group>();

    @ManyToMany
    @JoinTable(name = "user_relations", joinColumns = @JoinColumn(name = "assistant_id", referencedColumnName = "id"), inverseJoinColumns = @JoinColumn(name = "boss_id", referencedColumnName = "id"))
    private List<User> bosses = new ArrayList<User>();

    @ManyToMany(mappedBy = "bosses")
    private List<User> assistants = new ArrayList<User>();

    @Column(name = "binary_id")
    private Long binaryId;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Structure getStructure() {
        return structure;
    }

    public void setStructure(Structure structure) {
        this.structure = structure;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        String encrypted = password;
        try {
            encrypted = encrypt(encrypted);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        this.password = encrypted;
    }

    public String getPassword() {
        return password;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFirstName2() {
        return firstName2;
    }

    public void setFirstName2(String firstName2) {
        this.firstName2 = firstName2;
    }

    public String getLastName2() {
        return lastName2;
    }

    public void setLastName2(String lastName2) {
        this.lastName2 = lastName2;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getWorkPhone() {
        return workPhone;
    }

    public void setWorkPhone(String workPhone) {
        this.workPhone = workPhone;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isNew() {
        return isNewBit == 1;
    }

    public void setNew(boolean isNew) {
        this.isNewBit = isNew ? 1 : 0;
    }

    public boolean isAdmin() {
        return adminBit == 1;
    }

    public void setAdmin(boolean admin) {
        this.adminBit = admin ? 1 : 0;
    }

    public boolean isNotifyByEmail() {
        return notifyByEmailBit == 1;
    }

    public void setNotifyByEmail(boolean notify) {
        this.notifyByEmailBit = notify ? 1 : 0;
    }

    public boolean isNotifyByMobile() {
        return notifyByMobileBit == 1;
    }

    public void setNotifyByMobile(boolean notify) {
        this.notifyByMobileBit = notify ? 1 : 0;
    }

    public int getLastTaskSequence() {
        return lastTaskSequence;
    }

    public void setLastTaskSequence(int lastTaskSequence) {
        this.lastTaskSequence = lastTaskSequence;
    }

    public List<Group> getGroups() {
        return groups;
    }

    public void setGroups(List<Group> groups) {
        this.groups = groups;
    }

    public List<User> getBosses() {
        return bosses;
    }

    public void setBosses(List<User> bosses) {
        this.bosses = bosses;
    }

    public List<User> getAssistants() {
        return assistants;
    }

    public void setAssistants(List<User> assistants) {
        this.assistants = assistants;
    }

    public Long getBinaryId() {
        return binaryId;
    }

    public void setBinaryId(Long binaryId) {
        this.binaryId = binaryId;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public String getFullName2() {
        return firstName2 + " " + lastName2;
    }

    public boolean matches(String password) {
        if (this.password == null) {
            return false;
        }
        String encrypted = password;
        try {
            encrypted = encrypt(password);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return this.password.equals(encrypted);
    }

    protected String encrypt(String text) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA");
        md.update(text.getBytes("UTF-8"));
        byte raw[] = md.digest();
        String hash = (new BASE64Encoder()).encode(raw);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (id == null) {
            return false;
        }
        if (obj instanceof User) {
            User user = (User) obj;
            return id.equals(user.getId());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
