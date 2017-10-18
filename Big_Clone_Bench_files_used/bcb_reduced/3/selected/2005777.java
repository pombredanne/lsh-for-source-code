package com.mycompany.turisti.services;

import com.mycompany.turisti.data.Action;
import com.mycompany.turisti.data.Person;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author David
 */
public class PersonDao extends AbstraktDao {

    private List<Person> persons;

    @Transactional
    public Person getUser(String email) {
        try {
            Query q1 = em.createQuery("SELECT p FROM Person p WHERE p.email = :email");
            q1.setParameter("email", email);
            Person p = (Person) q1.getSingleResult();
            return p;
        } catch (NoResultException e) {
            return null;
        }
    }

    public Person getUserbyID(Long id) {
        Query q1 = em.createQuery("SELECT p FROM Person p WHERE p.id = :id");
        q1.setParameter("id", id);
        Person p = (Person) q1.getSingleResult();
        return p;
    }

    public void saveUser(Person person) {
        tx.begin();
        em.persist(person);
        tx.commit();
    }

    public boolean isRegister(Action a, Person p) {
        for (Iterator<Action> i = p.getActions().iterator(); i.hasNext(); ) {
            Action item = (Action) i.next();
            System.out.println("ID AKCE: " + item.getAction_id() + "n");
            System.out.println("ID AKCE: " + a.getAction_id() + "n");
            if (item.getAction_id().compareTo(a.getAction_id()) == 0) {
                return true;
            }
            ;
        }
        return false;
    }

    @Transactional
    public List<Person> getPersonsByAction(Action a) {
        List<Person> registeredPerson = new ArrayList<Person>(0);
        List<Person> allPersons = getUsers();
        for (Person p : allPersons) {
            if (isRegister(a, p)) {
                registeredPerson.add(p);
            }
            ;
        }
        return registeredPerson;
    }

    public void addActionToPerson(Action a, Person p) {
        p.getActions().add(a);
        tx.begin();
        em.merge(p);
        tx.commit();
    }

    public void removeActionToPerson(Action a, Person p) {
        for (Iterator<Action> i = p.getActions().iterator(); i.hasNext(); ) {
            Action item = (Action) i.next();
            if (item.getAction_id().compareTo(a.getAction_id()) == 0) {
                p.getActions().remove(item);
            }
            ;
        }
        tx.begin();
        em.merge(p);
        tx.commit();
    }

    @Transactional
    public List<Person> getUsers() {
        this.persons = em.createQuery("from Person").getResultList();
        System.out.println(this.persons.size());
        return this.persons;
    }

    public String sayHello(String name) {
        return "Vítejte, " + name + " !";
    }

    @Transactional
    public List<Person> getAllUsers() {
        List<Person> person = em.createQuery("from Person").getResultList();
        return person;
    }

    public void removeUser(Person p) {
        Long idU = p.getId();
        System.out.println("ID uzivatele kreho bych chtel smazat" + idU);
        tx.begin();
        Person per = em.find(Person.class, idU);
        em.remove(per);
        tx.commit();
    }

    public Boolean isOwner(Action a, Person p) {
        if (a.getOrganizatorId().compareTo(p.getId()) == 0) {
            return true;
        } else if (p.getOpravneni().compareTo(1) == 0) {
            return true;
        } else {
            return false;
        }
    }

    public void updatePerson(Person person) {
        tx.begin();
        Person oldPerson = em.find(Person.class, person.getId());
        oldPerson.setEmail(person.getEmail());
        oldPerson.setHeslo(person.getHeslo());
        oldPerson.setJmeno(person.getJmeno());
        oldPerson.setPrijmeni(person.getPrijmeni());
        oldPerson.setTelefon(person.getTelefon());
        em.merge(oldPerson);
        tx.commit();
    }

    public List<Action> getLoginAction(Person p) {
        List<Action> act = new ArrayList<Action>();
        for (Iterator<Action> i = p.getActions().iterator(); i.hasNext(); ) {
            Action item = (Action) i.next();
            if (isRegister(item, p)) {
                act.add(item);
            }
        }
        return act;
    }

    public String mdHesla(String password) {
        String hashword = null;
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(password.getBytes());
            BigInteger hash = new BigInteger(1, md5.digest());
            hashword = hash.toString(16);
        } catch (NoSuchAlgorithmException nsae) {
        }
        return hashword;
    }
}
