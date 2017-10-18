package biketreeDatastructures;

import java.util.GregorianCalendar;
import java.util.Calendar;
import java.util.Date;
import java.security.*;
import java.math.BigInteger;

/**
 *
 * @author Godwin
 */
public class BikeShopMember extends BikeTreeDatastructure implements BikeTreeDatastructure.HasFirstAndLastName, BikeTreeDatastructure.HasName {

    private int warnings, studentID, privilegeLevel;

    private String firstName, lastName, notes, email, phone, address, driversLicence, memberType, userName, password;

    private boolean waiverSigned, membershipPaid, eventMail, volunteerMail, steeringMail;

    private Date joinDate, membershipExpiryDate, lastSignin;

    private Double totalHours, credit;

    private Basket basket;

    public BikeShopMember(int membershipID, String membershipType, String first, String last, String memberNotes, String emailAddress, String phoneNumber, String mailingAddress, boolean waiver, int warningCount, int studentid, String drivers, boolean events, boolean volunteer, boolean steering, Date joined, Date lastIn, Date expires, Double hours, double bikeRootCredit, String username, String pass, int priv) {
        super(membershipID);
        credit = bikeRootCredit;
        membershipPaid = false;
        joinDate = joined;
        lastSignin = lastIn;
        totalHours = hours;
        membershipExpiryDate = expires;
        if (membershipExpiryDate != null && !membershipExpiryDate.before(new Date())) {
            membershipPaid = true;
        }
        setUserName(username);
        password = pass;
        setPrivileges(priv);
        setMemberType(membershipType);
        setFirstName(first);
        setLastName(last);
        setNotes(memberNotes);
        setEmail(emailAddress);
        setPhone(phoneNumber);
        setAddress(mailingAddress);
        setWaiverSigned(waiver);
        setWarnings(warningCount);
        setStudentID(studentid);
        setDriversLicence(drivers);
        setMailingLists(events, volunteer, steering);
    }

    /**
 * Creates a new member with no name and all the defaults
 */
    public BikeShopMember() {
        super();
        membershipPaid = false;
        lastSignin = null;
        totalHours = 0.0;
        joinDate = new Date();
        credit = 0.0;
        basket = null;
    }

    public BikeShopMember(BikeShopVisitor visitor) {
        this(visitor.getID(), visitor.getMemberType(), visitor.getFirstName(), visitor.getLastName(), visitor.getName(), visitor.getEmail(), visitor.getPhone(), visitor.getAddress(), visitor.isWaiverSigned(), visitor.getWarnings(), visitor.getStudentID(), visitor.getDriversLicence(), visitor.isOnEventsList(), visitor.isOnVolunteerList(), visitor.isOnSteeringList(), visitor.getJoinDate(), visitor.getLastSignin(), visitor.getMembershipExpiryDate(), visitor.getTotalHours(), visitor.getCredit(), visitor.getUserName(), visitor.getPassword(), visitor.getPrivileges());
    }

    /**
 * 
 * @return  the member type
 */
    public String getMemberType() {
        return memberType;
    }

    /**
 *
 * @return  the warning level
 */
    public int getWarnings() {
        return warnings;
    }

    /**
 * Sets the member's basket
 * @param   b       the  new basket
 */
    public void setBasket(Basket b) {
        basket = b;
    }

    /**
 *
 * @return          The member's current basket
 */
    public Basket getBasket() {
        return basket;
    }

    /**
 * Adds a sale item to this member's basket with default prices
 * @param item      The item to add to the member's basket
 * @param quantity  The ammount of this item to add
 * @param user      The BikeShopMember that made the sale
 */
    public void addToBasket(SaleItem item, int quantity, BikeShopMember user) {
        if (basket == null) {
            basket = new Basket(this);
        }
        basket.setSeller(user);
        basket.addItemToBasket(item, quantity);
    }

    /**
 * @return          true if the member has any items in their basket, false otherwise
 */
    public boolean hasBasket() {
        return basket != null;
    }

    /**
 * Sets the member type
 * @param type      the type to set
 */
    public void setMemberType(String type) {
        memberType = type;
    }

    /**
 * Sets the user's warning level
 * @param newWarnings   The new warning level to set
 */
    public void setWarnings(int newWarnings) {
        warnings = newWarnings;
    }

    /**
 * @return              The member's first name
 */
    public String getFirstName() {
        return firstName;
    }

    /**
 * Sets the member's first name
 * @param name          The name to be set
 */
    public void setFirstName(String name) {
        firstName = name;
    }

    /**
 * @return              The member's last name
 */
    public String getLastName() {
        return lastName;
    }

    /**
 * Sets the member's last name
 * @param name          The new last name
 */
    public void setLastName(String name) {
        lastName = name;
    }

    /**
 * @return              Notes about the member taken by BikeShopMembers
 */
    public String getNotes() {
        return notes;
    }

    /**
 * Sets the member's notes
 * @param newNotes      Notes about the member
 */
    public void setNotes(String newNotes) {
        notes = newNotes;
    }

    /**
 * @return              The member's email
 */
    public String getEmail() {
        return email;
    }

    /**
 * Sets the member's email address
 * @param newEmail      The new email address
 */
    public void setEmail(String newEmail) {
        email = newEmail;
    }

    /**
 * If your phone is ringing, this method will get it for you
 * @return              The member's phone number
 */
    public String getPhone() {
        return phone;
    }

    /**
 * Sets the member's phone number
 * @param newPhone      The member's new phone number
 */
    public void setPhone(String newPhone) {
        phone = newPhone;
    }

    /**
 * @return              The member's current address
 */
    public String getAddress() {
        return address;
    }

    /**
 * Sets the member's address
 * @param newAddress    The member's new address
 */
    public void setAddress(String newAddress) {
        address = newAddress;
    }

    /**
 * @return              true if the member has signed their waiver, false otherwise
 */
    public boolean isWaiverSigned() {
        return waiverSigned;
    }

    /**
 * Sets whether or not the member's waiver has been signed
 * @param signed        set to true if the user has signed their membership
 */
    public void setWaiverSigned(boolean signed) {
        waiverSigned = signed;
    }

    /**
 * @return              true if the member has paid less than a year ago
 */
    public boolean isMembershipPaid() {
        return membershipPaid;
    }

    /**
 * Sets the member's expiry date to one year from now.
 */
    public void renewMembership() {
        membershipPaid = true;
        GregorianCalendar cal = new GregorianCalendar();
        cal.add(Calendar.YEAR, 1);
        membershipExpiryDate = cal.getTime();
    }

    /**
 * @return              the date the member originally joined
 */
    public Date getJoinDate() {
        return joinDate;
    }

    /**
 * @return              the member's membership expiry date
 */
    public Date getMembershipExpiryDate() {
        return membershipExpiryDate;
    }

    /**
 * @return              The date when the user last signed in
 */
    public Date getLastSignin() {
        return lastSignin;
    }

    /**
 * @return              The total number of hours the member has spent in the shop
 */
    public Double getTotalHours() {
        return totalHours;
    }

    /**
 * Sets the student ID
 * @param id            The member's student id
 */
    public void setStudentID(int newID) {
        studentID = newID;
    }

    /**
 * @return              The member's setudent ID
 */
    public int getStudentID() {
        return studentID;
    }

    /**
 * Sets the member's driver's license number
 * @param licence       The member's driver's license number
 */
    public void setDriversLicence(String licence) {
        driversLicence = licence;
    }

    /**
 * @return              The member's driver's license number
 */
    public String getDriversLicence() {
        return driversLicence;
    }

    /**
 * @return              true if the member is on the events mailing list, false otherwise
 */
    public boolean isOnEventsList() {
        return eventMail;
    }

    /**
 *               true if the member is on the volunteering mailing list, false otherwise
 * @return
 */
    public boolean isOnVolunteerList() {
        return volunteerMail;
    }

    /**
 *               true if the member is on the steering (organizational) mailing list, false otherwise
 * @return
 */
    public boolean isOnSteeringList() {
        return steeringMail;
    }

    /**
 * Sets which mailing lists the user is signed up for
 * @param events        Set to true if the member wants to be signed up to recieve emails about events
 * @param volunteer     Set to true if the member wants to be signed up to recieve emails about volunteering
 * @param steering      Set to true if the member wants to be signed up to recieve emails about meetings ect.
 */
    public void setMailingLists(boolean events, boolean volunteer, boolean steering) {
        eventMail = events;
        volunteerMail = volunteer;
        steeringMail = steering;
    }

    /**
 * Takes two ArrayList objects of BikeShopMember objects, one presumably with more data than the other, calculates the
 *  intersection but uses values from bigList in the returned array.  This is useful since a full array of members
 *  was loaded from the database at the start of the program, and all fields were calculated, which takes some time
 *  so if we want to preform a search quickly we don't have to recalculate those fields, we can just get basic info
 *  and this method will fill in the rest.
 * @param bigList       An array of BikeShopMember objects with all fields calculated
 * @param smallList     An array of BikeShopMember objects without calculated fields
 * @return              An array of size equal to the smaller of the two arrays, although in most cases, smallList
 *                          will have a smaller size than bigList. All objects in the array will have the same ids
 *                          as smallList but have the data from each BikeShopMember in bigList.
 */
    public static java.util.ArrayList<BikeShopMember> matchMembers(java.util.ArrayList<BikeShopMember> bigList, java.util.ArrayList<BikeShopMember> smallList) {
        if (bigList == null || smallList == null) {
            return null;
        }
        java.util.ArrayList<BikeShopMember> result = new java.util.ArrayList<BikeShopMember>();
        for (int i = 0; i < bigList.size(); i++) {
            for (int j = 0; j < smallList.size(); j++) {
                if (bigList.get(i).equals(smallList.get(j))) {
                    result.add(bigList.get(i));
                    j = smallList.size();
                }
            }
        }
        return result;
    }

    /**
 * @return the first name plus the last name, example "John Doe"
 */
    @Override
    public String toString() {
        return getLastName() + ", " + getFirstName();
    }

    /**
 * Compares this member to another with a string that was used to to search for members
 * @param m         The BikeShopMember to compare to
 * @param compare   The string that was used to find these members
 * @return          1, -1, or 0 depending on whether or not this member is worth more than the other
 */
    public int compareTo(BikeShopMember m, String compare) {
        String c = compare.toLowerCase();
        if (m.equals(this)) {
            return 0;
        } else if ((getLastName().toLowerCase().startsWith(c) || getFirstName().toLowerCase().startsWith(c)) && !(m.getLastName().toLowerCase().startsWith(c) || m.getFirstName().toLowerCase().startsWith(c))) {
            return -1;
        } else if (!(getLastName().toLowerCase().startsWith(c) || getFirstName().toLowerCase().startsWith(c)) && (m.getLastName().toLowerCase().startsWith(c) || m.getFirstName().toLowerCase().startsWith(c))) {
            return 1;
        }
        if (m instanceof BikeShopVisitor && !(this instanceof BikeShopVisitor)) {
            return 1;
        } else if (!(m instanceof BikeShopVisitor) && (this instanceof BikeShopVisitor)) {
            return -1;
        }
        if (getTotalHours() == m.getTotalHours()) {
            return (getFirstName().contains(c) ? getFirstName() : getLastName()).compareToIgnoreCase(m.getFirstName().contains(c) ? m.getFirstName() : m.getLastName());
        }
        return (getTotalHours() > m.getTotalHours() ? -1 : 1);
    }

    /**
 * @return          true if the member is currently signed in, false otherwise
 */
    public boolean isSignedIn() {
        return this instanceof BikeShopVisitor;
    }

    /**
 * @return          The member's available credit
 */
    public double getCredit() {
        return credit;
    }

    /**
 * Sets the member's available credit
 * @param newCredit     the new ammount of credit
 */
    public void setCredit(double newCredit) {
        credit = newCredit;
    }

    /**
 * Adds or subtracts credit
 * @param addedCredit       Positive values will add the credit, negative values will subtract
 */
    public void addToCredit(double addedCredit) {
        credit += addedCredit;
    }

    public void setJoinDate(Date jd) {
        joinDate = jd;
    }

    public void setLastVisit(Date lv) {
        lastSignin = lv;
    }

    public void setTotalHours(Double th) {
        totalHours = th;
    }

    /**
 * @return      This user's username
 */
    public String getUserName() {
        return userName;
    }

    /**
 * @return      Gets this user's privledges
 */
    public int getPrivileges() {
        return privilegeLevel;
    }

    /**
 * @return      The md5 encrypted password
 */
    public String getPassword() {
        return password;
    }

    /**
 * Changes the user name
 * @param name      the new user name
 */
    public void setUserName(String name) {
        userName = name;
    }

    /**
 * Resets and encrypts the password
 * @param newPassword       an unencrypted password
 */
    public void setPassword(String newPassword) {
        password = getMD5(newPassword);
    }

    /**
 * Sets the user's privledges
 * @param newPrivileges     The new privledges
 */
    public void setPrivileges(int newPrivileges) {
        privilegeLevel = newPrivileges;
    }

    /**
 * Checks to see if the password is correct
 * @param passwordAttempt   An unencrypted password
 * @return                  true if the passwords match, false otherwise
 */
    public boolean checkPassword(String passwordAttempt) {
        if (password == null) {
            return false;
        }
        return getMD5(passwordAttempt).compareToIgnoreCase(password) == 0;
    }

    /**
 * Encrypts a string to MD5
 * @param pass              The unencrypted string
 * @return                  The encrypted string
 */
    public static String getMD5(String pass) {
        byte[] passBytes = pass.getBytes();
        try {
            MessageDigest algorithm = MessageDigest.getInstance("MD5");
            algorithm.reset();
            algorithm.update(passBytes);
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(passBytes);
            BigInteger number = new BigInteger(1, messageDigest);
            return number.toString(16);
        } catch (NoSuchAlgorithmException e) {
            throw new Error("invalid JRE: have not 'MD5' impl.", e);
        }
    }

    public String getName() {
        return userName;
    }

    public boolean equals(BikeShopVisitor visitor) {
        if (visitor == null) {
            return false;
        }
        return getID() == visitor.getMemberID();
    }

    public boolean hasUserAccount() {
        return userName != null;
    }
}
