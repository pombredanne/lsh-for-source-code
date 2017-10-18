package gov.ca.bdo.modeling.dsm2.map.server;

import gov.ca.bdo.modeling.dsm2.map.client.model.StudyInfo;
import gov.ca.bdo.modeling.dsm2.map.client.service.DSM2InputService;
import gov.ca.bdo.modeling.dsm2.map.server.data.DSM2ModelFile;
import gov.ca.bdo.modeling.dsm2.map.server.data.DSM2Study;
import gov.ca.bdo.modeling.dsm2.map.server.persistence.DSM2ModelFileDAOImpl;
import gov.ca.bdo.modeling.dsm2.map.server.persistence.DSM2StudyDAO;
import gov.ca.bdo.modeling.dsm2.map.server.persistence.DSM2StudyDAOImpl;
import gov.ca.bdo.modeling.dsm2.map.server.utils.PMF;
import gov.ca.bdo.modeling.dsm2.map.server.utils.Utils;
import gov.ca.dsm2.input.model.DSM2Model;
import gov.ca.dsm2.input.parser.InputTable;
import gov.ca.dsm2.input.parser.Parser;
import gov.ca.dsm2.input.parser.Tables;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import javax.jdo.PersistenceManager;
import org.apache.tools.ant.filters.StringInputStream;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

@SuppressWarnings("serial")
public class DSM2InputServiceImpl extends RemoteServiceServlet implements DSM2InputService {

    public DSM2Model getInputModel(String studyName) {
        String email = Utils.getCurrentUserEmail();
        return getInputModel(studyName, email);
    }

    public DSM2Model getInputModelForKey(String key) {
        DSM2Study studyForSharingKey = getStudyForSharingKey(key);
        if (studyForSharingKey == null) {
            return null;
        }
        return getInputModel(studyForSharingKey.getStudyName(), studyForSharingKey.getOwnerName());
    }

    public DSM2Model getInputModel(String studyName, String email) {
        Parser inputParser = new Parser();
        String hydro_echo_inp = "";
        String gis_inp = "";
        try {
            PersistenceManager persistenceManager = PMF.get().getPersistenceManager();
            try {
                DSM2ModelFileDAOImpl dao = new DSM2ModelFileDAOImpl(persistenceManager);
                List<DSM2ModelFile> filesForStudy = dao.getFilesForStudy(studyName, email);
                if (filesForStudy.size() == 0) {
                    return null;
                }
                for (DSM2ModelFile dsm2ModelFile : filesForStudy) {
                    if (dsm2ModelFile.getName().equals("hydro_echo_inp")) {
                        hydro_echo_inp = dsm2ModelFile.getContents();
                    }
                    if (dsm2ModelFile.getName().equals("gis_inp")) {
                        gis_inp = dsm2ModelFile.getContents();
                    }
                }
            } finally {
                persistenceManager.close();
            }
            InputStream inputStream = new StringInputStream(hydro_echo_inp);
            Tables model = inputParser.parseModel(inputStream);
            inputParser.parseAndAddToModel(model, new StringInputStream(gis_inp));
            return model.toDSM2Model();
        } catch (Exception ex) {
            ex.printStackTrace();
            return new DSM2Model();
        }
    }

    public void saveModel(String studyName, DSM2Model model) {
        Parser inputParser = new Parser();
        String hydro_echo_inp = "";
        String gis_inp = "";
        PersistenceManager persistenceManager = PMF.get().getPersistenceManager();
        try {
            DSM2ModelFileDAOImpl dao = new DSM2ModelFileDAOImpl(persistenceManager);
            String email = Utils.getCurrentUserEmail();
            List<DSM2ModelFile> filesForStudy = dao.getFilesForStudy(studyName, email);
            if (filesForStudy.size() == 0) {
                return;
            }
            for (DSM2ModelFile dsm2ModelFile : filesForStudy) {
                if (dsm2ModelFile.getName().equals("hydro_echo_inp")) {
                    hydro_echo_inp = dsm2ModelFile.getContents();
                }
                if (dsm2ModelFile.getName().equals("gis_inp")) {
                    gis_inp = dsm2ModelFile.getContents();
                }
            }
            InputStream inputStream = new StringInputStream(hydro_echo_inp);
            Tables tables = inputParser.parseModel(inputStream);
            inputParser.parseAndAddToModel(tables, new StringInputStream(gis_inp));
            tables.fromDSM2Model(model);
            StringBuilder hydro_echo_inp_builder = new StringBuilder();
            StringBuilder gis_inp_builder = new StringBuilder();
            for (InputTable table : tables.getTables()) {
                if (table.getName().contains("GIS")) {
                    gis_inp_builder.append(table.toStringRepresentation());
                } else {
                    hydro_echo_inp_builder.append(table.toStringRepresentation());
                }
            }
            for (DSM2ModelFile dsm2ModelFile : filesForStudy) {
                if (dsm2ModelFile.getName().equals("hydro_echo_inp")) {
                    dsm2ModelFile.setContents(hydro_echo_inp_builder.toString());
                }
                if (dsm2ModelFile.getName().equals("gis_inp")) {
                    dsm2ModelFile.setContents(gis_inp_builder.toString());
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            persistenceManager.close();
        }
    }

    public String[] getStudyNames() {
        PersistenceManager persistenceManager = PMF.get().getPersistenceManager();
        try {
            DSM2ModelFileDAOImpl dao = new DSM2ModelFileDAOImpl(persistenceManager);
            String email = Utils.getCurrentUserEmail();
            Collection<String> studyNames = dao.getStudyNamesForCurrentUser(email);
            String[] studyNamesArray = new String[studyNames.size()];
            return studyNames.toArray(studyNamesArray);
        } catch (Exception e) {
            e.printStackTrace();
            return new String[0];
        } finally {
            persistenceManager.close();
        }
    }

    public void removeStudy(String studyName) {
        PersistenceManager persistenceManager = PMF.get().getPersistenceManager();
        try {
            DSM2ModelFileDAOImpl dao = new DSM2ModelFileDAOImpl(persistenceManager);
            String email = Utils.getCurrentUserEmail();
            List<DSM2ModelFile> filesForStudy = dao.getFilesForStudy(studyName, email);
            if (filesForStudy.size() == 0) {
                return;
            }
            for (DSM2ModelFile dsm2ModelFile : filesForStudy) {
                dao.deleteObject(dsm2ModelFile);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            persistenceManager.close();
        }
    }

    public String showInputFor(String studyName, String inputName, String email) {
        PersistenceManager persistenceManager = PMF.get().getPersistenceManager();
        try {
            DSM2ModelFileDAOImpl dao = new DSM2ModelFileDAOImpl(persistenceManager);
            List<DSM2ModelFile> filesForStudy = dao.getFilesForStudy(studyName, email);
            if (filesForStudy.size() == 0) {
                return "";
            }
            for (DSM2ModelFile dsm2ModelFile : filesForStudy) {
                if (dsm2ModelFile.getName().equals(inputName)) {
                    return dsm2ModelFile.getContents();
                }
            }
            return "";
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        } finally {
            persistenceManager.close();
        }
    }

    public String showInputForKey(String studyKey, String inputName) {
        DSM2Study studyForSharingKey = getStudyForSharingKey(studyKey);
        String studyName = studyForSharingKey.getStudyName();
        String ownerName = studyForSharingKey.getOwnerName();
        return showInputFor(studyName, inputName, ownerName);
    }

    public String showInput(String studyName, String inputName) {
        String email = Utils.getCurrentUserEmail();
        return showInputFor(studyName, inputName, email);
    }

    public String showGISInput(String studyName) {
        return showInput(studyName, "gis_inp");
    }

    public String showHydroInput(String studyName) {
        return showInput(studyName, "hydro_echo_inp");
    }

    public String generateSharingKey(String studyName) {
        UserService userService = UserServiceFactory.getUserService();
        String key = studyName + userService.getCurrentUser().getUserId();
        try {
            byte messageDigest[] = MessageDigest.getInstance("MD5").digest(key.getBytes());
            StringBuffer hexString = new StringBuffer();
            for (byte element : messageDigest) {
                hexString.append(Integer.toHexString(0xFF & element));
            }
            key = hexString.toString();
            storeSharingKey(studyName, key);
        } catch (Exception ex) {
            return null;
        }
        return key;
    }

    private void storeSharingKey(String studyName, String key) {
        PersistenceManager persistenceManager = PMF.get().getPersistenceManager();
        try {
            DSM2StudyDAO dao = new DSM2StudyDAOImpl(persistenceManager);
            String email = Utils.getCurrentUserEmail();
            DSM2Study study = dao.getStudyForName(studyName, email);
            if (study == null) {
                study = new DSM2Study();
                study.setDateFirstShared(new Date());
                study.setOwnerName(email);
                study.setSharingKey(key);
                study.setStudyName(studyName);
                dao.createObject(study);
            } else {
                study.setSharingKey(key);
            }
            dao.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            persistenceManager.close();
        }
    }

    public String getStudyNameForSharingKey(String key) {
        DSM2Study study = getStudyForSharingKey(key);
        if (study == null) {
            return "";
        } else {
            return study.getOwnerName() + "::" + study.getStudyName();
        }
    }

    public DSM2Study getStudyForSharingKey(String key) {
        PersistenceManager persistenceManager = PMF.get().getPersistenceManager();
        try {
            DSM2StudyDAO dao = new DSM2StudyDAOImpl(persistenceManager);
            DSM2Study study = dao.getStudyForSharingKey(key);
            return study;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            persistenceManager.close();
        }
    }

    public DSM2Model getInputModelForStudy(StudyInfo info) {
        PersistenceManager persistenceManager = PMF.get().getPersistenceManager();
        DSM2Model model = null;
        try {
            DSM2StudyDAO dao = new DSM2StudyDAOImpl(persistenceManager);
            DSM2Study study = dao.getStudyForName(info.getName(), info.getOwnerEmail());
            if (study == null) {
                study = dao.getStudyForSharingKey(info.getSharingKey());
            }
            if (study == null) {
                return null;
            }
            String sharingType = study.getSharingType();
            if (sharingType.equals(StudyInfo.SHARING_PRIVATE)) {
                String userEmails = study.getSharedUsersEmails();
                if (userEmails == null) {
                    return null;
                }
                String currentEmail = Utils.getCurrentUserEmail();
                if (currentEmail == null) {
                    return null;
                }
                String[] emails = userEmails.split(",");
                boolean found = false;
                for (String email : emails) {
                    if (currentEmail.equals(email)) {
                        found = true;
                        break;
                    }
                }
                if (found) {
                    model = this.getInputModel(study.getStudyName(), study.getOwnerName());
                }
            } else if (sharingType.equals(StudyInfo.SHARING_PUBLIC)) {
                model = this.getInputModel(study.getStudyName(), study.getOwnerName());
            } else if (sharingType.equals(StudyInfo.SHARING_UNLISTED)) {
                model = this.getInputModel(study.getStudyName(), study.getOwnerName());
            } else {
                return null;
            }
            return model;
        } catch (Exception e) {
            e.printStackTrace();
            return model;
        } finally {
            persistenceManager.close();
        }
    }

    public List<StudyInfo> getStudies() {
        return null;
    }
}
