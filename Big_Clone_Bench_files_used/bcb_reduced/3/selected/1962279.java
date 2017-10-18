package de.tum.in.eist.poll.server;

import de.tum.in.eist.poll.shared.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public final class Database {

    public static final String HOME_PATH = System.getProperty("user.home") + File.separator + "EIST_Poll" + File.separator;

    public Database() {
    }

    /**
	 * Adds a feedback to the list and writes it into the file.
	 * 
	 * @param lecture The lecture where the feedback is added.
	 * @param feedback The feedback that is added.
	 */
    public static void addFeedbackToLecture(Lecture lecture, Feedback feedback) {
        List<Feedback> feedbackForLecture = (List<Feedback>) lectureToFeedback.get(lecture);
        if (feedbackForLecture == null) {
            feedbackForLecture = new LinkedList<Feedback>();
            feedbackForLecture.add(feedback);
            lectureToFeedback.put(lecture, feedbackForLecture);
        } else {
            feedbackForLecture.add(feedback);
        }
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(HOME_PATH + "Lectures.csv"));
            for (Lecture lect : lectures) {
                int feedSize = lectureToFeedback.get(lect).size();
                List<Feedback> listFeed = lectureToFeedback.get(lect);
                if (lect == lecture) {
                    lect.addStudent(feedback.getStudent());
                }
                writer.write(lect.getLectureID() + ";" + lect.getLectureTitle() + ";" + lect.getLecturer().getName() + ";" + feedSize);
                writer.newLine();
                for (Feedback feed : listFeed) {
                    String[] posCom = feed.getPosComm().split(System.getProperty("line.separator"));
                    String[] negCom = feed.getNegComm().split(System.getProperty("line.separator"));
                    writer.write(Database.getHash(feed.getStudent().getName()));
                    writer.newLine();
                    writer.write(feed.getBewertung() + ";" + posCom.length + ";" + negCom.length);
                    writer.newLine();
                    for (int i = 0; i < posCom.length; i++) {
                        writer.write(posCom[i]);
                        writer.newLine();
                    }
                    for (int i = 0; i < negCom.length; i++) {
                        writer.write(negCom[i]);
                        writer.newLine();
                    }
                }
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Checks if the account exists.
	 * @param name accountname
	 * @param password accountpassword
	 */
    public static boolean isValidInput(String name, String password) {
        User user = getUser(name);
        if (user != null) return user.getPassword().equals(password); else return false;
    }

    /**
	 * Get the account information by name.
	 */
    public static User getUser(String name) {
        if (users == null) {
            users = new LinkedList<User>();
            users.addAll(getStudents());
            users.addAll(getLecturers());
        }
        for (Iterator<User> iterator = users.iterator(); iterator.hasNext(); ) {
            User user = (User) iterator.next();
            if (user.getName().equals(name)) return user;
        }
        return null;
    }

    /**
	 * Get all the students that are stored on the server (or file). If there is
	 * no information stored, the method creates a file with two students (Student_1, Student_2) in the
	 * home folder (see HOME_PATH).
	 * @return A list of all the students.
	 */
    public static List<Student> getStudents() {
        if (students == null) {
            students = new LinkedList<Student>();
            String filePath = HOME_PATH + "Student.csv";
            try {
                readStudents();
            } catch (FileNotFoundException e) {
                try {
                    File folder = new File(HOME_PATH);
                    folder.mkdirs();
                    BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));
                    writer.write("Student_1;new");
                    writer.newLine();
                    writer.write("Student_2;new");
                    writer.newLine();
                    writer.close();
                    readStudents();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return students;
    }

    /**
	 * Get all the lecturers that are stored on the server (or file). If there is
	 * no information stored, the method creates a file with two lecturers (Prof, Maalej) in the
	 * home folder (see HOME_PATH).
	 * @return A list of all the lecturers.
	 */
    public static List<Lecturer> getLecturers() {
        if (lecturers == null) {
            lecturers = new LinkedList<Lecturer>();
            String filePath = HOME_PATH + "Lecturer.csv";
            try {
                readLecturers();
            } catch (FileNotFoundException e) {
                BufferedWriter writer;
                try {
                    File folder = new File(HOME_PATH);
                    folder.mkdirs();
                    writer = new BufferedWriter(new FileWriter(filePath));
                    writer.write("Prof;new");
                    writer.newLine();
                    writer.write("Maalej;new");
                    writer.newLine();
                    writer.close();
                    readLecturers();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return lecturers;
    }

    /**
	 * Get all the lectures that are stored on the server (or file). If there is
	 * no information stored, the method creates a file with three lectures (IN0001, IN0002, IN0003) in the
	 * home folder (see HOME_PATH).
	 * @return A list of all the lectures with the feedbacks.
	 */
    public static List<Lecture> getLectures() {
        if (lectures == null) {
            lectures = new LinkedList<Lecture>();
            String filePath = HOME_PATH + "Lectures.csv";
            try {
                readLectures();
            } catch (FileNotFoundException e) {
                BufferedWriter writer;
                try {
                    File folder = new File(HOME_PATH);
                    folder.mkdirs();
                    writer = new BufferedWriter(new FileWriter(filePath));
                    writer.write("IN0001;EIST_1;Maalej;0");
                    writer.newLine();
                    writer.write("IN0002;EIST_2;Maalej;0");
                    writer.newLine();
                    writer.write("IN0003;EIST_3;Maalej;0");
                    writer.newLine();
                    writer.close();
                    readLectures();
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return lectures;
    }

    /**
	 * Get a lecture by its Lecture ID.
	 */
    public static Lecture getLecture(String lectureID) {
        for (Iterator<Lecture> iterator = lectures.iterator(); iterator.hasNext(); ) {
            Lecture lecture = (Lecture) iterator.next();
            if (lecture.getLectureID().equals(lectureID)) return lecture;
        }
        return null;
    }

    /**
	 * Get all the feedbacks that are attached to a lecture.
	 */
    public static List<Feedback> getFeedback(Lecture lecture) {
        return lectureToFeedback.get(lecture);
    }

    /**
	 * Read the students from a file. The student information are stored in the Student.csv
	 * file (The file is in the home folder (see HOME_PATH). 
	 */
    private static void readStudents() throws IOException, FileNotFoundException {
        String filePath = HOME_PATH + "Student.csv";
        BufferedReader read = new BufferedReader(new FileReader(filePath));
        String line;
        StringTokenizer tokenizer;
        while ((line = read.readLine()) != null) {
            tokenizer = new StringTokenizer(line, ";");
            String name = tokenizer.nextToken();
            String passwd = tokenizer.nextToken();
            students.add(new Student(name, passwd));
        }
        read.close();
    }

    /**
	 * Read the lectures from a file. The lecture information are stored in the Lectures.csv
	 * file (The file is in the home folder (see HOME_PATH). 
	 */
    private static void readLectures() throws FileNotFoundException, IOException {
        String filePath = HOME_PATH + "Lectures.csv";
        BufferedReader read = new BufferedReader(new FileReader(filePath));
        String line;
        StringTokenizer tokenizer;
        while ((line = read.readLine()) != null) {
            tokenizer = new StringTokenizer(line, ";");
            String lecID = tokenizer.nextToken();
            String title = tokenizer.nextToken();
            String lect = tokenizer.nextToken();
            int numbFeed = Integer.parseInt(tokenizer.nextToken());
            User prLec = getUser(lect);
            Lecture lecture = new Lecture(lecID, title, (Lecturer) prLec);
            lectures.add(lecture);
            List<Feedback> feedbackForLecture = new LinkedList<Feedback>();
            for (int i = 0; i < numbFeed; i++) {
                line = read.readLine();
                Student feedStud = null;
                for (Student stud : Database.getStudents()) {
                    if (Database.getHash(stud.getName()).compareTo(line) == 0) {
                        feedStud = stud;
                        break;
                    }
                }
                if (feedStud == null) throw new IOException();
                line = read.readLine();
                tokenizer = new StringTokenizer(line, ";");
                int bewert = Integer.parseInt(tokenizer.nextToken());
                int linePos = Integer.parseInt(tokenizer.nextToken());
                int lineNeg = Integer.parseInt(tokenizer.nextToken());
                String comPos = "";
                String comNeg = "";
                for (int j = 0; j < linePos; j++) {
                    comPos += read.readLine();
                }
                for (int j = 0; j < lineNeg; j++) {
                    comNeg += read.readLine();
                }
                feedbackForLecture.add(new Feedback(lecture, feedStud, bewert, comNeg, comPos));
            }
            lectureToFeedback.put(lecture, feedbackForLecture);
        }
        read.close();
    }

    /**
	 * Read the lecturer from a file. The lecturers information are stored in the Lectures.csv
	 * file (The file is in the home folder (see HOME_PATH). 
	 */
    private static void readLecturers() throws IOException, FileNotFoundException {
        String filePath = HOME_PATH + "Lecturer.csv";
        BufferedReader read = new BufferedReader(new FileReader(filePath));
        String line;
        StringTokenizer tokenizer;
        while ((line = read.readLine()) != null) {
            tokenizer = new StringTokenizer(line, ";");
            String name = tokenizer.nextToken();
            String passwd = tokenizer.nextToken();
            lecturers.add(new Lecturer(name, passwd));
        }
        read.close();
    }

    /**
	 * Creates a hash from a string (uses SHA).
	 */
    private static String getHash(String string) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA");
            String result = "";
            for (Byte b : md.digest(string.getBytes())) {
                result += Integer.toHexString(0xff & b);
            }
            return result;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return "";
        }
    }

    private static List<Lecture> lectures;

    private static List<User> users;

    private static List<Student> students;

    private static List<Lecturer> lecturers;

    private static Map<Lecture, List<Feedback>> lectureToFeedback = new LinkedHashMap<Lecture, List<Feedback>>();
}
