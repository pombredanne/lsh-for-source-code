package budgettracker;

import java.io.File;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Set;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 *
 * @author schwier
 */
public class IORoutines {

    private byte[] key = null;

    private String hint = "";

    private boolean use_encryption = false;

    private final int line_length_bytes = 70;

    private final int file_identifier_bytes = 17;

    private final int passphrase_bytes = 68;

    private final int enc_identifier_bytes = 17;

    private final int enc_num_accounts_bytes = 4;

    private final int account_name_bytes = 64;

    private final int account_num_env_bytes = 4;

    private final int env_name_bytes = 64;

    private final int env_num_income_bytes = 4;

    private final int trans_name_bytes = 55;

    private final int trans_date_bytes = 8;

    private final int trans_value_bytes = 4;

    IORoutines() {
    }

    IORoutines(boolean encrypt, String passphrase) {
        use_encryption = encrypt;
        hint = passphrase;
    }

    /**
     * Given an account structure, archive transactions in the structure
     * @param accounts
     * @return empty string if success, error message if not
     */
    public String archive_budget(ArrayList<Account> accounts) {
        DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
        Calendar cal = Calendar.getInstance();
        String current_date = df.format(cal.getTime());
        String date_string = JOptionPane.showInputDialog("Archive all transactions on and before?", current_date);
        if (date_string != null) {
            Date check_date = BTMath.convertDateStringToDate(date_string);
            if (check_date == null) {
                return "Invalid date entered";
            }
            String formatted_date = df.format(check_date);
            formatted_date = formatted_date.replaceAll("/", "-");
            File default_file = new File("BudgetTrackerArchive-" + formatted_date + ".btf");
            JFileChooser fileSelector = new JFileChooser();
            FileNameExtensionFilter fnef = new FileNameExtensionFilter("Budget Tracker Files", "btf", "ebtf");
            fileSelector.setFileFilter(fnef);
            fileSelector.setDialogTitle("Select filename to save");
            fileSelector.setSelectedFile(default_file);
            int return_value = fileSelector.showOpenDialog(null);
            if (return_value == JFileChooser.APPROVE_OPTION) {
                File archive_file = fileSelector.getSelectedFile();
                IORoutines ior = new IORoutines();
                boolean archiveSuccess = ior.save_budget(archive_file, accounts, date_string);
                if (archiveSuccess) return ("Success:" + date_string); else return "Save error";
            }
        }
        return "Archive cancelled";
    }

    /**
     * Given an account and the available account structure, create the
     * account structure from the byte array located in the file.
     * @param budget_file file containing the byte array
     * @param accounts account structure to create
     * @return true if success, false if fail
     */
    public boolean load_budget(File budget_file, ArrayList<Account> accounts) {
        FileInputStream fis;
        BufferedInputStream bis;
        try {
            fis = new FileInputStream(budget_file);
            bis = new BufferedInputStream(fis);
        } catch (FileNotFoundException fnfe) {
            System.err.println("Error: could not read file");
            return false;
        }
        byte[] file_header = new byte[line_length_bytes];
        byte[] passphrase_header = new byte[line_length_bytes];
        byte[] encryption_header = new byte[line_length_bytes];
        byte[] data_portion = new byte[(int) (budget_file.length() - line_length_bytes * 3)];
        try {
            bis.read(file_header);
            bis.read(passphrase_header);
            bis.read(encryption_header);
            bis.read(data_portion);
            bis.close();
            fis.close();
        } catch (IOException ioe) {
            return false;
        }
        if (file_header[0] == 'H') {
            byte[] file_identifier = new byte[file_identifier_bytes];
            System.arraycopy(file_header, 1, file_identifier, 0, file_identifier_bytes);
            String file_id = new String(file_identifier);
            if (!file_id.equals("BudgetTrackerFile")) return false;
        } else return false;
        boolean encryption_being_used = (file_header[file_identifier_bytes + 2] == 'Y');
        if (passphrase_header[0] == 'P') {
            byte[] prepassphrase = new byte[passphrase_bytes];
            System.arraycopy(passphrase_header, 1, prepassphrase, 0, passphrase_bytes);
            hint = new String(prepassphrase);
        }
        byte[] data_ready = null;
        if (encryption_being_used) {
        } else {
            byte[] num_accounts = null;
            if (encryption_header[0] == 'D') {
                byte[] file_readable = new byte[enc_identifier_bytes];
                System.arraycopy(encryption_header, 1, file_readable, 0, enc_identifier_bytes);
                String encryption_identifier = new String(file_readable);
                if (!encryption_identifier.equals("TheFileIsReadable")) return false;
                num_accounts = new byte[enc_num_accounts_bytes];
                System.arraycopy(encryption_header, enc_identifier_bytes + 2, num_accounts, 0, enc_num_accounts_bytes);
            }
            data_ready = data_portion;
        }
        boolean success = convertByteArrayToData(data_ready, accounts);
        return true;
    }

    /**
     * Given a file and the account structure, save the account data out as
     * a byte array.
     *
     * File Header:
     * |H|BudgetTrackerFile|00|Y/N|--49 bytes unused|00| (start indexes: 0, 1, 18, 19, 20, 69)
     *
     * Passphrase Header:
     * |P|Passphrase|00| (start indexes: 0, 1, 69)
     *
     * Encryption header:
     * |D|TheFileIsReadable|00|# accounts|--46 bytes unused|00| (start indexes: 0, 1, 18, 19, 23, 69)
     *
     * @param budget_file file to save byte array into
     * @param accounts account structure to save
     * @param condition_date date to condition the save on, null if save all
     * @return true if success, false if fail
     */
    public boolean save_budget(File budget_file, ArrayList<Account> accounts, String condition_date) {
        Calendar date;
        if (condition_date.equals("")) {
            date = null;
        } else {
            date = Calendar.getInstance();
            date.setTime(BTMath.convertDateStringToDate(condition_date));
        }
        FileOutputStream fos;
        BufferedOutputStream bos;
        try {
            fos = new FileOutputStream(budget_file);
            bos = new BufferedOutputStream(fos);
        } catch (FileNotFoundException fnfe) {
            return false;
        }
        byte[] file_header = new byte[line_length_bytes];
        file_header[0] = 'H';
        String file_identifier = "BudgetTrackerFile";
        System.arraycopy(file_identifier.getBytes(), 0, file_header, 1, file_identifier_bytes);
        if (use_encryption) file_header[file_identifier_bytes + 2] = 'Y'; else file_header[file_identifier_bytes + 2] = 'N';
        byte[] passphrase_header = new byte[line_length_bytes];
        passphrase_header[0] = 'P';
        if (hint.length() > passphrase_bytes) {
            hint = hint.substring(0, passphrase_bytes - 1);
        }
        System.arraycopy(hint.getBytes(), 0, passphrase_header, 1, hint.length());
        byte[] encryption_header = new byte[line_length_bytes];
        encryption_header[0] = 'D';
        String encryption_identifier = "TheFileIsReadable";
        System.arraycopy(encryption_identifier.getBytes(), 0, encryption_header, 1, enc_identifier_bytes);
        byte[] num_accounts = intToByteArray(accounts.size());
        System.arraycopy(num_accounts, 0, encryption_header, enc_identifier_bytes + 2, num_accounts.length);
        try {
            bos.write(file_header);
            bos.write(passphrase_header);
            bos.write(encryption_header);
        } catch (IOException ioe) {
            return false;
        }
        byte[] data;
        if (date == null) data = convertDataToByteArray(accounts); else data = convertDataToByteArrayByDate(accounts, date);
        if (data == null) return false;
        byte[] data_to_write = null;
        if (use_encryption) {
        } else {
            data_to_write = data;
        }
        try {
            bos.write(data_to_write);
            bos.flush();
            bos.close();
        } catch (IOException ioe) {
            return false;
        }
        return true;
    }

    /**
     * From the stored account information, convert the accounts into a byte array for storage
     *
     * All lines are 70-bytes long
     *
     * Account Line:
     * |A|Name|#envelopes|00|
     *
     * Envelope Line:
     * |E|Name|# income|00|
     *
     * Transaction Line:
     * |T|I/E|Name|Value|00|
     *
     * Transaction values are stored as the cent integer converted to a 4-byte array.
     *
     * @param accounts ArrayList of accounts
     * @return one dimensional byte array containing all account information
     */
    private byte[] convertDataToByteArray(ArrayList<Account> accounts) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (Account a : accounts) {
            byte[] account_line = new byte[line_length_bytes];
            account_line[0] = 'A';
            String acct_name = a.getName();
            if (acct_name.length() > account_name_bytes) {
                acct_name = acct_name.substring(0, account_name_bytes - 1);
            }
            System.arraycopy(acct_name.getBytes(), 0, account_line, 1, acct_name.length());
            byte[] num_envelopes = intToByteArray(a.getNumberEnvelopes());
            System.arraycopy(num_envelopes, 0, account_line, account_name_bytes + 1, account_num_env_bytes);
            try {
                baos.write(account_line);
            } catch (IOException ioe) {
                return null;
            }
            Iterator<Envelope> e_iter = a.getEnvelopeIterator();
            while (e_iter.hasNext()) {
                Envelope e = e_iter.next();
                byte[] envelope_line = new byte[line_length_bytes];
                envelope_line[0] = 'E';
                String env_name = e.getName();
                if (env_name.length() > env_name_bytes) {
                    env_name = env_name.substring(0, env_name_bytes - 1);
                }
                System.arraycopy(env_name.getBytes(), 0, envelope_line, 1, env_name.length());
                byte[] num_income = intToByteArray(e.getMapSize());
                System.arraycopy(num_income, 0, envelope_line, env_name_bytes + 1, env_num_income_bytes);
                try {
                    baos.write(envelope_line);
                } catch (IOException ioe) {
                    return null;
                }
                Set<Integer> keys = e.getKeys();
                for (Integer id : keys) {
                    Transaction t = e.getTransactionByID(id);
                    byte[] t_line = new byte[line_length_bytes];
                    t_line[0] = 'T';
                    if (t.isExpense()) t_line[1] = 'E'; else t_line[1] = 'I';
                    String trans_name = t.getName();
                    if (trans_name.length() > trans_name_bytes) {
                        trans_name = trans_name.substring(0, trans_name_bytes - 1);
                    }
                    System.arraycopy(trans_name.getBytes(), 0, t_line, 2, trans_name.length());
                    DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
                    String date = df.format(t.getDate().getTime());
                    System.arraycopy(date.getBytes(), 0, t_line, trans_name_bytes + 2, date.length());
                    byte[] value = intToByteArray(BTMath.stringToCents(t.getValue()));
                    System.arraycopy(value, 0, t_line, trans_name_bytes + trans_date_bytes + 2, trans_value_bytes);
                    try {
                        baos.write(t_line);
                    } catch (IOException ioe) {
                        return null;
                    }
                }
            }
        }
        try {
            baos.flush();
        } catch (IOException ioe) {
            return null;
        }
        byte[] all_data = baos.toByteArray();
        try {
            baos.close();
        } catch (IOException ioe) {
            return null;
        }
        return all_data;
    }

    /**
     * From the stored account information, convert the accounts into a byte array for storage.
     * Only store transactions if they occur on or before the specified date. Only store
     * envelopes and accounts if they contain transactions on or before the specified date.
     *
     * All lines are 70-bytes long
     *
     * Account Line:
     * |A|Name|#envelopes|00|
     *
     * Envelope Line:
     * |E|Name|# income|00|
     *
     * Transaction Line:
     * |T|I/E|Name|Value|00|
     *
     * Transaction values are stored as the cent integer converted to a 4-byte array.
     *
     * @param accounts ArrayList of accounts
     * @param date Date to condition the transaction write on
     * @return one dimensional byte array containing selected account information
     */
    private byte[] convertDataToByteArrayByDate(ArrayList<Account> accounts, Calendar date) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (Account a : accounts) {
            boolean wrote_account = false;
            byte[] account_line = new byte[line_length_bytes];
            account_line[0] = 'A';
            String acct_name = a.getName();
            if (acct_name.length() > account_name_bytes) {
                acct_name = acct_name.substring(0, account_name_bytes - 1);
            }
            System.arraycopy(acct_name.getBytes(), 0, account_line, 1, acct_name.length());
            byte[] num_envelopes = intToByteArray(a.getNumberEnvelopes());
            System.arraycopy(num_envelopes, 0, account_line, account_name_bytes + 1, account_num_env_bytes);
            Iterator<Envelope> e_iter = a.getEnvelopeIterator();
            while (e_iter.hasNext()) {
                Envelope e = e_iter.next();
                boolean wrote_envelope = false;
                byte[] envelope_line = new byte[line_length_bytes];
                envelope_line[0] = 'E';
                String env_name = e.getName();
                if (env_name.length() > env_name_bytes) {
                    env_name = env_name.substring(0, env_name_bytes - 1);
                }
                System.arraycopy(env_name.getBytes(), 0, envelope_line, 1, env_name.length());
                byte[] num_income = intToByteArray(e.getMapSize());
                System.arraycopy(num_income, 0, envelope_line, env_name_bytes + 1, env_num_income_bytes);
                Set<Integer> keys = e.getKeys();
                for (Integer id : keys) {
                    Transaction t = e.getTransactionByID(id);
                    int compare = date.compareTo(t.getDate());
                    if (compare >= 0) {
                        try {
                            if (!wrote_account) {
                                baos.write(account_line);
                                wrote_account = true;
                            }
                            if (!wrote_envelope) {
                                baos.write(envelope_line);
                                wrote_envelope = true;
                            }
                        } catch (IOException ioe) {
                        }
                    } else continue;
                    byte[] t_line = new byte[line_length_bytes];
                    t_line[0] = 'T';
                    if (t.isExpense()) t_line[1] = 'E'; else t_line[1] = 'I';
                    String trans_name = t.getName();
                    if (trans_name.length() > trans_name_bytes) {
                        trans_name = trans_name.substring(0, trans_name_bytes - 1);
                    }
                    System.arraycopy(trans_name.getBytes(), 0, t_line, 2, trans_name.length());
                    DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
                    String date_string = df.format(t.getDate().getTime());
                    System.arraycopy(date_string.getBytes(), 0, t_line, trans_name_bytes + 2, date_string.length());
                    byte[] value = intToByteArray(BTMath.stringToCents(t.getValue()));
                    System.arraycopy(value, 0, t_line, trans_name_bytes + trans_date_bytes + 2, trans_value_bytes);
                    try {
                        baos.write(t_line);
                    } catch (IOException ioe) {
                        return null;
                    }
                }
            }
        }
        try {
            baos.flush();
        } catch (IOException ioe) {
            return null;
        }
        byte[] all_data = baos.toByteArray();
        try {
            baos.close();
        } catch (IOException ioe) {
            return null;
        }
        return all_data;
    }

    /**
     * Given an input 1-dimensional, byte array turn the data bytes into
     * the Account information
     * @param bytearray account information in byte array form
     * @param accounts data structure to store account information
     * @return true if success, false if fail
     */
    private boolean convertByteArrayToData(byte[] bytearray, ArrayList<Account> accounts) {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytearray);
        byte[] buffer = new byte[line_length_bytes];
        try {
            Account a = null;
            Envelope e = null;
            while (bais.read(buffer) != -1) {
                if (buffer[0] == 'A') {
                    byte[] name = new byte[account_name_bytes];
                    System.arraycopy(buffer, 1, name, 0, account_name_bytes);
                    byte[] num_env = new byte[account_num_env_bytes];
                    System.arraycopy(buffer, account_name_bytes + 1, num_env, 0, account_num_env_bytes);
                    a = new Account((new String(name)).trim());
                    accounts.add(a);
                } else if (buffer[0] == 'E') {
                    byte[] name = new byte[env_name_bytes];
                    System.arraycopy(buffer, 1, name, 0, env_name_bytes);
                    byte[] num_inc = new byte[env_num_income_bytes];
                    System.arraycopy(buffer, env_name_bytes + 1, num_inc, 0, env_num_income_bytes);
                    e = new Envelope((new String(name)).trim());
                    if (a != null) a.addEnvelope(e);
                } else if (buffer[0] == 'T') {
                    boolean isExpense = (buffer[1] == 'E');
                    byte[] name = new byte[trans_name_bytes];
                    System.arraycopy(buffer, 2, name, 0, trans_name_bytes);
                    byte[] date = new byte[trans_date_bytes];
                    System.arraycopy(buffer, trans_name_bytes + 2, date, 0, trans_date_bytes);
                    byte[] weekly = new byte[trans_value_bytes];
                    System.arraycopy(buffer, trans_name_bytes + trans_date_bytes + 2, weekly, 0, trans_value_bytes);
                    String date_string = new String(date);
                    Calendar calendar = Calendar.getInstance();
                    DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
                    Date date_value = null;
                    try {
                        date_value = df.parse(date_string);
                    } catch (ParseException pe) {
                        continue;
                    }
                    calendar.setTime(date_value);
                    String value = BTMath.centsToString(byteArrayToInt(weekly, 0));
                    if (e != null) e.addTransaction(calendar, isExpense, (new String(name)).trim(), value);
                }
                Arrays.fill(buffer, (byte) 0);
            }
        } catch (IOException ioe) {
            return false;
        }
        return true;
    }

    /**
     * Given a JTable, export the table contents to the specified file as
     * a CSV file
     * @param save_file
     * @param table
     * @return true if successful write, false if not
     */
    public boolean exportAsCSV(File save_file, javax.swing.JTable table) {
        int rows = table.getRowCount();
        int cols = table.getColumnCount();
        String output = "";
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                output += "\"" + table.getValueAt(i, j).toString() + "\"";
                if (j < cols - 1) output += ",";
            }
            output += "\n";
        }
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(save_file));
            bw.write(output);
            bw.flush();
            bw.close();
        } catch (IOException ioe) {
            return false;
        }
        return true;
    }

    /**
     * From a given input string, calculate the 128-bit hash values using
     * MD5. Set the internal key variable with the byte array and return if
     * the key was successfully set.
     * @param s input string
     * @return true if success, false if fail
     */
    public boolean setKey(String s) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            key = null;
            return false;
        }
        byte[] input = s.getBytes();
        md.update(input);
        key = md.digest();
        return true;
    }

    /**
	 * Returns a byte array containing the two's-complement representation of the integer.<br>
	 * The byte array will be in big-endian byte-order with a fixes length of 4
	 * (the least significant byte is in the 4th element).<br>
	 * <br>
	 * <b>Example:</b><br>
	 * <code>intToByteArray(258)</code> will return { 0, 0, 1, 2 },<br>
	 * <code>BigInteger.valueOf(258).toByteArray()</code> returns { 1, 2 }.
	 * @param integer The integer to be converted.
	 * @return The byte array of length 4.
	 */
    private byte[] intToByteArray(final int integer) {
        int byteNum = (40 - Integer.numberOfLeadingZeros(integer < 0 ? ~integer : integer)) / 8;
        byte[] byteArray = new byte[4];
        for (int n = 0; n < byteNum; n++) byteArray[3 - n] = (byte) (integer >>> (n * 8));
        return (byteArray);
    }

    /**
     * Convert the byte array to an int starting from the given offset.
     *
     * @param b The byte array
     * @param offset The array offset
     * @return The integer
     */
    private int byteArrayToInt(byte[] b, int offset) {
        int value = 0;
        for (int i = 0; i < 4; i++) {
            int shift = (4 - 1 - i) * 8;
            value += (b[i + offset] & 0x000000FF) << shift;
        }
        return value;
    }
}
