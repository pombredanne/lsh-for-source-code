package org.libertya.sugarInterface.utils;

import java.io.File;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Level;
import org.apache.axis.encoding.Base64;
import org.openXpertya.model.MPreference;
import org.openXpertya.util.CLogger;
import org.openXpertya.util.Env;
import org.openXpertya.util.ValueNamePair;
import com.sugarcrm.www.sugarcrm.Entry_value;
import com.sugarcrm.www.sugarcrm.Error_value;
import com.sugarcrm.www.sugarcrm.Get_entry_list_result;
import com.sugarcrm.www.sugarcrm.Get_entry_result;
import com.sugarcrm.www.sugarcrm.Name_value;
import com.sugarcrm.www.sugarcrm.Note_attachment;
import com.sugarcrm.www.sugarcrm.Set_entries_result;
import com.sugarcrm.www.sugarcrm.Set_entry_result;
import com.sugarcrm.www.sugarcrm.Sugarsoap;
import com.sugarcrm.www.sugarcrm.SugarsoapLocator;
import com.sugarcrm.www.sugarcrm.SugarsoapPortType;
import com.sugarcrm.www.sugarcrm.User_auth;

public class SugarSoapInstance {

    private SugarsoapPortType port = null;

    private String sessionID = null;

    private static CLogger log = CLogger.getCLogger(SugarSoapInstance.class);

    public SugarSoapInstance(String url, String user, String pass) {
        try {
            Sugarsoap service = new SugarsoapLocator();
            port = service.getsugarsoapPort(new java.net.URL(url));
            User_auth userAuth = new User_auth();
            userAuth.setUser_name(user);
            MessageDigest md = MessageDigest.getInstance("MD5");
            String password = getHexString(md.digest(pass.getBytes()));
            userAuth.setPassword(password);
            userAuth.setVersion("0.1");
            Set_entry_result loginRes = port.login(userAuth, "sugarsoap");
            sessionID = loginRes.getId();
        } catch (Exception ex) {
            log.log(Level.SEVERE, "Error al conectarse al WebService Sugar: " + ex.toString());
            ex.printStackTrace();
        }
    }

    public boolean isConnected() {
        return (port != null && sessionID != null);
    }

    public boolean close() {
        boolean sucessful = false;
        try {
            Error_value errorValue = port.logout(sessionID);
            if (errorValue.getNumber().equals("0")) sucessful = true;
        } catch (Exception ex) {
            log.log(Level.SEVERE, "Error al realizar el logout de sugar: " + ex.toString());
        }
        return sucessful;
    }

    public SugarsoapPortType getPort() {
        return port;
    }

    public String getSessionId() {
        return sessionID;
    }

    public static String getHexString(byte[] data) throws Exception {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9)) buf.append((char) ('0' + halfbyte)); else buf.append((char) ('a' + (halfbyte - 10)));
                halfbyte = data[i] & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }

    public Object[] getSugarAccounts(String name) {
        ArrayList list = new ArrayList();
        try {
            String[] fields = { "id", "name" };
            Get_entry_list_result entryList = getPort().get_entry_list(getSessionId(), "Accounts", "accounts.name like '%" + name + "%'", "name", 0, fields, 99, 0);
            System.out.println("Error: " + entryList.getError().getDescription());
            Entry_value[] entryValues = entryList.getEntry_list();
            for (int i = 0; i < entryValues.length; i++) {
                Name_value[] nameValues = entryValues[i].getName_value_list();
                list.add(new ValueNamePair(nameValues[0].getValue(), nameValues[1].getValue()));
            }
            ValueNamePair[] retValue = new ValueNamePair[list.size()];
            list.toArray(retValue);
            return retValue;
        } catch (Exception ex) {
            log.log(Level.SEVERE, "Error al buscar las cuentas en sugar: " + ex.toString());
            ex.printStackTrace();
        }
        return null;
    }

    public Properties getAccountData(String id, String[] fields) {
        Properties prop = new Properties();
        try {
            String[] ids = { id };
            Get_entry_result entryResult = getPort().get_entries(getSessionId(), "Accounts", ids, fields);
            System.out.println("Error: " + entryResult.getError().getDescription());
            if (entryResult.getError().getNumber().equals("0")) {
                Entry_value[] entryValues = entryResult.getEntry_list();
                if (entryValues.length > 0) {
                    Name_value[] nameValues = entryValues[0].getName_value_list();
                    for (int i = 0; i < nameValues.length; i++) {
                        prop.setProperty(nameValues[i].getName(), nameValues[i].getValue());
                    }
                }
            }
        } catch (Exception ex) {
            log.log(Level.SEVERE, "Error al buscar las cuentas en sugar: " + ex.toString());
            ex.printStackTrace();
        }
        return prop;
    }

    /**
	     * @author: DISYTEL - Horacio Alvarez
	     * @date: 2009-08-20
	     * @description: Sugar-WebService - Crear la Nota en Sugar. Recibe como parametro el nombre y la descripcion
	     * de la misma. 
	     * @return ID de la nota creada.
	     */
    public String createSugarNote(String name) {
        String note_id = null;
        Name_value[][] name_value_list = new Name_value[1][1];
        Name_value noteValue1 = new Name_value();
        noteValue1.setName("name");
        noteValue1.setValue(name);
        name_value_list[0][0] = noteValue1;
        try {
            Set_entries_result entriesResult = port.set_entries(sessionID, "Notes", name_value_list);
            if (entriesResult.getError().getNumber().equals("0")) note_id = entriesResult.getIds()[0]; else log.log(Level.SEVERE, entriesResult.getError().getDescription());
        } catch (Exception ex) {
            log.log(Level.SEVERE, "Error al crear la nota en sugar : " + ex.toString());
            ex.printStackTrace();
        }
        return note_id;
    }

    /**
	     * @author: DISYTEL - Horacio Alvarez
	     * @date: 2009-08-20
	     * @description: Sugar-WebService - Adjunta el reporte Jasper en formato PDF a la nota.
	     * Los valores predeterminados @OpportunityAttachJasperName y @SugarNoteName deben estar
	     * obligatoriamente creados.
	     * @param noteID - Id de la nota.
	     */
    public boolean setSugarNoteAttachment(String noteID, File attachment, String fileName) {
        boolean sucessful = false;
        try {
            Note_attachment attach = new Note_attachment();
            byte[] bytes = Env.getBytesFromFile(attachment);
            String encoded = Base64.encode(bytes);
            attach.setFile(encoded);
            attach.setFilename(fileName);
            attach.setId(noteID);
            Set_entry_result saveRes = port.set_note_attachment(sessionID, attach);
            if (saveRes.getError().getNumber().equals("0")) sucessful = true; else log.log(Level.SEVERE, saveRes.getError().getDescription());
        } catch (Exception ex) {
            log.log(Level.SEVERE, "Error al adjuntar el pdf: " + ex.toString());
            ex.printStackTrace();
        }
        return sucessful;
    }

    /**
	     * @author: DISYTEL - Horacio Alvarez
	     * @date: 2009-08-20
	     * @description: Sugar-WebService - Relaciona la Nota creada con la Oportunidad.
	     * @param: noteID - el id de la nota creada.
	     */
    public boolean relateSugarNoteToOpportunity(String noteID, String opportunityId) {
        boolean sucessful = false;
        try {
            Error_value error = port.relate_note_to_module(sessionID, noteID, "Opportunities", opportunityId);
            if (error.getNumber().equals("0")) sucessful = true; else log.log(Level.SEVERE, error.getDescription());
        } catch (Exception ex) {
            log.log(Level.SEVERE, "Error al relacionar la nota con la oportunidad: " + ex.toString());
            ex.printStackTrace();
        }
        return sucessful;
    }

    public String[] getOpportunityFields(Integer opportunityNo, String[] fields) {
        String[] results = new String[fields.length];
        String sugarOpportunityNoFieldName = MPreference.GetCustomPreferenceValue("SugarOpportunityNoFieldName");
        try {
            Get_entry_list_result entryList = port.get_entry_list(sessionID, "Opportunities", sugarOpportunityNoFieldName + " = " + opportunityNo, "name", 0, fields, 99, 0);
            System.out.println("Error: " + entryList.getError().getDescription());
            Entry_value[] entryValues = entryList.getEntry_list();
            if (entryValues.length > 0) {
                Name_value[] nameValues = entryValues[0].getName_value_list();
                System.out.println("ID: " + entryValues[0].getId());
                for (int j = 0; j < nameValues.length; j++) results[j] = nameValues[j].getValue();
            }
        } catch (Exception ex) {
            log.log(Level.SEVERE, "Error al setear los campos de la oportunidad: " + ex.toString());
            ex.printStackTrace();
        }
        return results;
    }
}
