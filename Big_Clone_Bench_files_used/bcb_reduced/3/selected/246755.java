package de.mediumster.model;

import java.io.Serializable;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Sort;
import org.hibernate.annotations.SortType;
import org.hibernate.validator.Email;
import org.hibernate.validator.Length;
import org.hibernate.validator.Past;
import de.mediumster.model.acl.UserGroup;
import de.mediumster.model.hibernate.Required;

/**
 * Diese Klasse definiert einen Benutzer der Bibliothek.
 * 
 * @author Sebastian Gerken
 * @author Hannes Rempel
 */
@SuppressWarnings("serial")
@Entity(name = "users")
@Table(uniqueConstraints = @UniqueConstraint(columnNames = { "firstName", "lastName", "birthday" }))
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "Mediumster")
public class User implements Principal, Serializable, Comparable<User> {

    /**
	 * Aktivit�t des Benutzers; nur aktive Benutzer d�rfen sich anmelden und Medien ausleihen
	 */
    private boolean active;

    @Required(message = "{required.birthday}")
    @Past(message = "Das Geburtsdatum muss in der Vergangenheit liegen!")
    @Temporal(TemporalType.DATE)
    private Date birthday;

    /**
	 * Wohnort des Benutzers
	 */
    @Required(message = "{required.city}")
    private String city;

    /**
	 * Emailadresse des Benutzers
	 */
    @Required(message = "{required.email}")
    @Email(message = "Die angegebene Emailadresse ist ung�ltig!")
    @Length(max = 64, message = "Die L�nge der Email ist auf 64 Zeichen begrenzt!")
    @Column(length = 64)
    private String email;

    /**
	 * die verliehen Exemplaren des Benutzers
	 */
    @OneToMany(targetEntity = Exemplar.class, mappedBy = "user")
    @Sort(type = SortType.NATURAL)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "Mediumster")
    private SortedSet<Exemplar> exemplars = new TreeSet<Exemplar>();

    /**
	 * Vorname des Kunden
	 */
    @Required(message = "{required.firstName}")
    @Length(max = 32, message = "Die L�nge des Vornamens ist auf 32 Zeichen begrenzt!")
    @Column(length = 32)
    private String firstName;

    /**
	 * Berechtigungsgruppe des Benutzers
	 */
    @Enumerated(EnumType.STRING)
    @Column(name = "groupName", length = 16)
    private UserGroup group;

    /**
	 * Vorname des Benutzers
	 */
    @Required(message = "{required.lastName}")
    @Length(max = 32, message = "Die L�nge des Nachnames ist auf 32 Zeichen begrenzt!")
    @Column(length = 32)
    private String lastName;

    /**
	 * eindeutiger Anmeldename
	 */
    @Required(message = "{required.login}")
    @Length(max = 16, message = "Die L�nge des Benutzernames ist auf 32 Zeichen begrenzt!")
    @Column(length = 16, unique = true)
    private String login;

    /**
	 * MD5-verschl�sseltes Password
	 */
    @Required(message = "{required.password}")
    @Length(max = 32, message = "Das Passwort ist zu lang! Ein Fehler im Programm?")
    @Column(length = 32)
    private String password;

    /**
	 * Telefonnummer des Benutzers
	 */
    @Required(message = "{required.phone}")
    @Length(max = 32, message = "Die L�nge der Telefonnummer ist auf 32 Zeichen begrenzt!")
    @Column(length = 32)
    private String phone;

    /**
	 * Postleitzahl des Benutzers
	 */
    @Required(message = "{required.postalCode}")
    @Length(min = 5, max = 5, message = "Die Postleitzahl muss 5 Zeichen lang sein!")
    @Column(length = 5)
    private String postalCode;

    /**
	 * die Reservierten Medien des Benutzers
	 */
    @OneToMany(targetEntity = Reservation.class, mappedBy = "user")
    @Sort(type = SortType.NATURAL)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "Mediumster")
    private SortedSet<Reservation> reservations = new TreeSet<Reservation>();

    /**
	 * Stra�enname des Benutzers
	 */
    @Required(message = "{required.streetName}")
    @Length(max = 64, message = "Die L�nge des Stra�ennamens ist auf 64 Zeichen begrenzt!")
    @Column(length = 64)
    private String streetName;

    /**
	 * Hausnummer des Benutzers
	 */
    @Required(message = "{required.streetNumber}")
    @Length(max = 6, message = "Die L�nge der Hausnummer ist auf 6 Zeichen begrenzt!")
    @Column(length = 6)
    private String streetNumber;

    /**
	 * eindeutige Benutzernummer
	 */
    @Id
    @GeneratedValue
    private int userId;

    /**
	 * Der Kontruktor gibt einen Benuter zur�ck
	 */
    public User() {
    }

    /**
	 * Diese Methode f�gt dem Benutzer eine Reservierung f�r ein Medium hinzu.
	 * 
	 * @param medium
	 *            Medium
	 * @return den Benutzer selbst
	 */
    public User addReservation(Medium medium) {
        if (medium != null) {
            Reservation reservation = new Reservation(this, medium);
            medium.addReservation(reservation);
            addReservation(reservation);
        }
        return this;
    }

    /**
	 * Diese Methode f�gt dem Benutzer eine Reservierung hinzu.
	 * 
	 * @param reservation
	 *            Reservierung
	 * @return den Benutzer selbst
	 */
    public User addReservation(Reservation reservation) {
        reservations.add(reservation);
        return this;
    }

    /**
	 * Diese Methode vergleicht diesen Kunden mit dem �bergebenen Kunden um sie sortieren zu k�nnen.
	 * Die Kunden werden nach der Kundennummer sortiert.
	 * 
	 * @param user
	 *            Vergleichsbenutzer
	 * @return ein negativer Wert, wenn dieser Kunde kleiner als der �bergebene Kunde ist; der Wert
	 *         0, wenn beide Kunden gleich sind; ein positiver Wert, wenn dieser Kunde gr��er ist
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
    public int compareTo(final User user) {
        return getUserId() - user.getUserId();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        User other = (User) obj;
        if (birthday == null) {
            if (other.birthday != null) {
                return false;
            }
        } else if (!birthday.equals(other.birthday)) {
            return false;
        }
        if (firstName == null) {
            if (other.firstName != null) {
                return false;
            }
        } else if (!firstName.equals(other.firstName)) {
            return false;
        }
        if (lastName == null) {
            if (other.lastName != null) {
                return false;
            }
        } else if (!lastName.equals(other.lastName)) {
            return false;
        }
        return true;
    }

    /**
	 * Diese Methode gibt das Geburtsdatum zur�ck.
	 * 
	 * @return Geburstdatum
	 */
    public Date getBirthday() {
        return birthday;
    }

    /**
	 * Diese Methode gibt den Wohnort zur�ck.
	 * 
	 * @return Wohnort
	 */
    public String getCity() {
        return city;
    }

    /**
	 * Diese Methode gibt Emailaddresse zur�ck.
	 * 
	 * @return Emailadresse
	 */
    public String getEmail() {
        return email;
    }

    /**
	 * Diese Methode gibt die entlihenden Exemplare zur�ck.
	 * 
	 * @return Liste mit Exemplaren
	 */
    public SortedSet<Exemplar> getExemplars() {
        return exemplars;
    }

    /**
	 * Diese Methode gibt den Vornamen zur�ck.
	 * 
	 * @return Vorname
	 */
    public String getFirstName() {
        return firstName;
    }

    /**
	 * Diese Methode gibt Benutzergruppe zur�ck.
	 * 
	 * @return Benutzergruppe
	 */
    public UserGroup getGroup() {
        if (group == null) {
            return UserGroup.anonymous;
        }
        return group;
    }

    /**
	 * Diese Methode gibt den Nachnamen zur�ck.
	 * 
	 * @return Nachname
	 */
    public String getLastName() {
        return lastName;
    }

    /**
	 * Diese Methode gibt den Anmeldenamen zur�ck.
	 * 
	 * @return Anmeldename
	 */
    public String getLogin() {
        return login;
    }

    /**
	 * Diese Methode gibt den Vornamen und Nachnamen zur�ck.
	 * 
	 * @return Vorname + Nachname
	 */
    @Override
    public String getName() {
        return getFirstName() + " " + getLastName();
    }

    /**
	 * Diese Methode gibt die Telefonnummer zur�ck.
	 * 
	 * @return Telefonnummer des Benutzes
	 */
    public String getPhone() {
        return phone;
    }

    /**
	 * Diese Methode gibt die Postleitzahl zur�ck.
	 * 
	 * @return Postleitzahl des Benutzes
	 */
    public String getPostalCode() {
        return postalCode;
    }

    /**
	 * Diese Methode gibt die Reservierung zu einem Medium zur�ck.
	 * 
	 * @param medium
	 *            Medium
	 * @return die bestehende {@link Reservation Reservierung} oder {@code null}
	 */
    public Reservation getReservation(Medium medium) {
        for (Reservation reservation : reservations) {
            if (reservation.getMedium().equals(medium)) {
                return reservation;
            }
        }
        return null;
    }

    /**
	 * Diese Methode gibt Reservierungen zur�ck.
	 * 
	 * @return Liste mit Reservierungen
	 */
    public Collection<Reservation> getReservations() {
        return reservations;
    }

    /**
	 * Diese Methode gibt den Stra�ennamen zur�ck.
	 * 
	 * @return Stra�enname
	 */
    public String getStreetName() {
        return streetName;
    }

    /**
	 * Diese Methode gibt die Hausnummer zur�ck.
	 * 
	 * @return Hausnummer
	 */
    public String getStreetNumber() {
        return streetNumber;
    }

    /**
	 * Diese Methode gibt die Benutzernummer zur�ck.
	 * 
	 * @return Benutzernummer
	 */
    public int getUserId() {
        return userId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (birthday == null ? 0 : birthday.hashCode());
        result = prime * result + (firstName == null ? 0 : firstName.hashCode());
        result = prime * result + (lastName == null ? 0 : lastName.hashCode());
        return result;
    }

    /**
	 * Diese Methode generiert einen MD5-Hash zu einem Passwort.
	 * 
	 * @param password
	 *            Passwort
	 * @return Hash des Passworts
	 */
    private String hashPassword(String password) {
        if (password != null && password.trim().length() > 0) {
            try {
                MessageDigest md5 = MessageDigest.getInstance("MD5");
                md5.update(password.trim().getBytes());
                BigInteger hash = new BigInteger(1, md5.digest());
                return hash.toString(16);
            } catch (NoSuchAlgorithmException nsae) {
            }
        }
        return null;
    }

    /**
	 * Diese Methode gibt die Aktivit�t zur�ck.
	 * 
	 * @return {@code true}, wenn der Benutzer aktiv ist
	 */
    public boolean isActive() {
        return active;
    }

    /**
	 * Diese Methode �berpr�ft, ob das �bergebene Passwort mit dem gespeicherten Passwort
	 * �bereinstimmt.
	 * 
	 * @param password
	 *            Passwort
	 * @return {@code true}, wenn beide Passw�rter identisch sind
	 */
    public boolean isPassword(String password) {
        if (this.password != null) {
            return this.password.equals(hashPassword(password));
        }
        return false;
    }

    /**
	 * Diese Methode setzt die Aktivit�t.
	 * 
	 * @param activity
	 *            Aktivit�t
	 */
    public User setActive(boolean activity) {
        active = activity;
        return this;
    }

    /**
	 * Diese Methode setzt das Geburtsdatum.
	 * 
	 * @param birthday
	 *            Geburtsdatum
	 * @return der Benutzer selbst
	 */
    public User setBirthday(Date birthday) {
        if (birthday != null) {
            this.birthday = birthday;
        }
        return this;
    }

    /**
	 * Diese Methode setzt das Geburstdatum.
	 * 
	 * @param birthday
	 *            Geburtsdatum
	 * @return der Benutzer selbst
	 */
    public User setBirthday(String birthday) {
        if (birthday != null && birthday.trim().length() > 0) {
            final List<Locale> locales = new ArrayList<Locale>();
            locales.add(Locale.GERMAN);
            locales.add(Locale.ENGLISH);
            for (final Locale locale : locales) {
                for (int i = 3; i >= 0; i--) {
                    try {
                        final DateFormat df = DateFormat.getDateInstance(i, locale);
                        this.birthday = df.parse(birthday);
                        return this;
                    } catch (final ParseException e) {
                    }
                }
            }
        }
        return this;
    }

    /**
	 * Diese Methode setzt den Wohnort.
	 * 
	 * @param city
	 *            Wohnort
	 * @return der Benutzer selbst
	 */
    public User setCity(String city) {
        if (city != null && city.trim().length() > 0) {
            this.city = city.trim();
        }
        return this;
    }

    /**
	 * Diese Methode setzt die Emailadresse.
	 * 
	 * @param email
	 *            Emailadresse
	 * @return der Benutzer selbst
	 */
    public User setEmail(String email) {
        if (email != null && email.trim().length() > 0) {
            this.email = email.trim();
        }
        return this;
    }

    /**
	 * Diese Methode setzt den Vornamen.
	 * 
	 * @param firstName
	 *            Vorname
	 */
    public User setFirstName(final String firstName) {
        if (firstName != null && firstName.trim().length() > 0) {
            this.firstName = firstName.trim();
        }
        return this;
    }

    /**
	 * Diese Methode setzt die Berechtigungsgruppe.
	 * 
	 * @param group
	 *            Berechtigungsgruppe
	 * @return der Benutzer selbst
	 */
    public User setGroup(UserGroup group) {
        if (group != null) {
            this.group = group;
        }
        return this;
    }

    /**
	 * Diese Methode setzt den Nachnamen.
	 * 
	 * @param lastName
	 *            Nachname
	 */
    public User setLastName(final String lastName) {
        if (lastName != null && lastName.trim().length() > 0) {
            this.lastName = lastName.trim();
        }
        return this;
    }

    /**
	 * Diese Methode setzt den Anmeldenamen
	 * 
	 * @param login
	 *            Anmeldename
	 * @return der Benutzer selbst
	 */
    public User setLogin(String login) {
        if (login != null && login.trim().length() > 0) {
            this.login = login.trim().toLowerCase();
        }
        return this;
    }

    /**
	 * Diese Methode setzt das Passwort.
	 * 
	 * @param password
	 *            Passwort
	 * @return der Benutzer selbst
	 */
    public User setPassword(String password) {
        String hash = hashPassword(password);
        if (hash != null) {
            this.password = hash;
        }
        return this;
    }

    /**
	 * Diese Methode setzt die Telefonnummer
	 * 
	 * @param phone
	 *            Telefonnummer
	 * @return der Benutzer selbst
	 */
    public User setPhone(String phone) {
        if (phone != null && phone.trim().length() > 0) {
            this.phone = phone.trim();
        }
        return this;
    }

    /**
	 * Diese Methode setzt den Wohnort.
	 * 
	 * @param postalCode
	 *            Wohnort
	 * @return der Benutzer selbst
	 */
    public User setPostalCode(String postalCode) {
        if (postalCode != null && postalCode.trim().length() > 0) {
            this.postalCode = postalCode.trim();
        }
        return this;
    }

    /**
	 * Diese Methode setzt den Stra�ennamen
	 * 
	 * @param streetName
	 *            Stra�enname
	 * @return der Benutzer selbst
	 */
    public User setStreetName(String streetName) {
        if (streetName != null && streetName.trim().length() > 0) {
            this.streetName = streetName.trim();
        }
        return this;
    }

    /**
	 * Diese Methode setzt die Hausnummer
	 * 
	 * @param streetNumber
	 *            Hausnummer
	 * @return der Benutzer selbst
	 */
    public User setStreetNumber(String streetNumber) {
        if (streetNumber != null && streetNumber.trim().length() > 0) {
            this.streetNumber = streetNumber.trim();
        }
        return this;
    }

    /**
	 * Diese Methode gibt den Vornamen und Nachnamen zur�ck.
	 * 
	 * @return Vorname + Nachname
	 */
    @Override
    public String toString() {
        return getName();
    }
}
