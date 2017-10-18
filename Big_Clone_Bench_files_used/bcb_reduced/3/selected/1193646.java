package br.ufal.tci.nexos.arcolive.persistence;

import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Hashtable;
import br.ufal.tci.nexos.arcolive.participant.ArCoLIVEParticipant;
import br.ufal.tci.nexos.arcolive.util.MD5Crypt;
import br.ufal.tci.nexos.arcolive.util.PermissionManipulator;

public class ArCoLIVEPersistenceDatabaseImpl implements ArCoLIVEPersistence {

    private GenericDatabaseManager databaseManager = null;

    private String _query;

    protected ArCoLIVEPersistenceDatabaseImpl() {
        this.databaseManager = new GenericDatabaseManager();
    }

    public boolean connect() {
        try {
            this.databaseManager.connect();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean isConnected() {
        return this.databaseManager.isConnected();
    }

    public GenericResultSet getAllCommands() {
        _query = "SELECT * FROM Command";
        return this.databaseManager.queryAllRecords(_query);
    }

    public GenericResultSet getAllCommandsOfService(int serviceId, boolean justAvailable) {
        String condition = "";
        if (justAvailable) {
            condition = " AND enabled = 'Y'";
        }
        _query = "SELECT Command.* FROM Command WHERE Command.serviceId = " + serviceId + condition;
        return this.databaseManager.queryAllRecords(_query);
    }

    public GenericResultSet getParametersCommand(int commandId) {
        _query = "SELECT * FROM CommandParameters WHERE commandId = " + commandId;
        return this.databaseManager.queryAllRecords(_query);
    }

    public GenericResultSet getOneCommand(int commandId) {
        _query = "SELECT * FROM Command WHERE commandId = " + commandId;
        return this.databaseManager.queryOneRecord(_query);
    }

    public boolean insertCommand(String name, String description, int implementationId, String implementationClasspath) {
        _query = "INSERT INTO Command VALUES ('', '" + name + "', '" + description + "', " + implementationId + ", " + implementationClasspath + ")";
        return this.databaseManager.executeQuery(_query);
    }

    public boolean updateCommand(int commandId, String name, String description, int implementationId, String implementationClasspath) {
        _query = "UPDATE Command SET name = " + name + ", description = '" + description + "', implementationId = " + implementationId + ", implementationClasspath = '" + implementationClasspath + "' WHERE commandId = " + commandId;
        return this.databaseManager.executeQuery(_query);
    }

    public boolean removeCommand(int commandId) {
        _query = "DELETE FROM Command WHERE commandId = " + commandId;
        return this.databaseManager.executeQuery(_query);
    }

    public GenericResultSet getAllCountries() {
        _query = "SELECT * FROM Country";
        return this.databaseManager.queryAllRecords(_query);
    }

    public GenericResultSet getOneCountry(int countryId) {
        _query = "SELECT * FROM Country WHERE countryId = " + countryId;
        return this.databaseManager.queryOneRecord(_query);
    }

    public GenericResultSet getAllParticipants() {
        _query = "SELECT * FROM Participant";
        return this.databaseManager.queryAllRecords(_query);
    }

    public GenericResultSet getOneParticipant(int participantId) {
        _query = "SELECT * FROM Participant WHERE participantId = " + participantId;
        return this.databaseManager.queryOneRecord(_query);
    }

    public GenericResultSet getOneParticipant(String username) {
        _query = "SELECT * FROM Participant WHERE username = '" + username + "'";
        return this.databaseManager.queryOneRecord(_query);
    }

    public boolean authenticateUser(String username, String password) {
        _query = "SELECT participantId FROM Participant WHERE username = '" + username + "' AND password = '" + password + "'";
        GenericResultSet loginInformation = this.databaseManager.queryAllRecords(_query);
        return (loginInformation.size() == 1);
    }

    public boolean insertParticipant(String fullname, String email, String username, String password, int participantGroupIds[], String location, String city, String state, int countryId, boolean enabled, String msnMessager, String yahooMessager, String aolMessager) {
        String participantGroupProfile = "";
        String participantGroupQueries[] = new String[participantGroupIds.length];
        char participantEnabled = (enabled) ? 'Y' : 'N';
        participantGroupProfile = participantGroupProfile.substring(0, participantGroupProfile.length() - 1);
        byte[] b = null;
        try {
            b = MD5Crypt.digest(password.getBytes(), "md5");
        } catch (NoSuchAlgorithmException e) {
            return false;
        }
        password = MD5Crypt.byteArrayToHexString(b);
        _query = "INSERT INTO Participant VALUES ('', '" + fullname + "', '" + email + "', '" + username + "', password('" + password + "'))";
        if (this.databaseManager.queryInTransaction(_query, false)) {
            int participantId = this.databaseManager.getLastInsertedId();
            if (participantId == -1) {
                this.databaseManager.rollback();
                return false;
            }
            _query = "INSERT INTO ParticipantProfile VALUES (" + participantId + ", '" + location + "', '" + city + "', '" + state + "', " + countryId + ", '" + participantEnabled + "', '" + participantGroupProfile + "', '" + msnMessager + "', '" + yahooMessager + "', '" + aolMessager + "')";
            if (this.databaseManager.queryInTransaction(_query, false)) {
                for (int i = 0; i < participantGroupIds.length; i++) {
                    participantGroupQueries[i] = "INSERT INTO ParticipantGroup VALUES (" + participantGroupIds[i] + ", " + participantId + ")";
                }
                return this.databaseManager.queryInTransaction(participantGroupQueries, true);
            } else {
                return false;
            }
        }
        return false;
    }

    public boolean changeParticipantStatus(int participantId, boolean enabled) {
        String enabledStr = (enabled) ? "Y" : "N";
        _query = "UPDATE Participant SET enabled = '" + enabledStr + "' WHERE participantId = " + participantId;
        return this.databaseManager.executeQuery(_query);
    }

    public boolean updateParticipant(int participantId, String fullname, String email, String username, String password, int[] participantGroupIds, String location, String city, String state, int countryId, String msnMessager, String yahooMessager, String aolMessager) {
        String participantGroupProfile = "";
        String attribution = "";
        String participantGroupQueries[] = new String[participantGroupIds.length + 1];
        if (participantGroupIds != null) {
            for (int i = 0; i < participantGroupIds.length; i++) {
                participantGroupProfile = participantGroupProfile + participantGroupIds[i] + ":";
            }
            attribution += "participantGroupIds = '" + participantGroupProfile + "', ";
        }
        if (!fullname.equals("")) {
            attribution += "fullname = '" + fullname + "', ";
        }
        if (!email.equals("")) {
            attribution += "email = '" + email + "', ";
        }
        if (!username.equals("")) {
            attribution += "username = '" + username + "', ";
        }
        if (!password.equals("")) {
            attribution += "password = '" + password + "', ";
        }
        if (!location.equals("")) {
            attribution += "location = '" + location + "', ";
        }
        if (!city.equals("")) {
            attribution += "city = '" + city + "', ";
        }
        if (!state.equals("")) {
            attribution += "state = '" + state + "', ";
        }
        if (countryId != 0) {
            attribution += "countryId = '" + countryId + "', ";
        }
        if (!msnMessager.equals("")) {
            attribution += "msnMessager = '" + msnMessager + "', ";
        }
        if (!yahooMessager.equals("")) {
            attribution += "yahooMessager = '" + yahooMessager + "', ";
        }
        if (!aolMessager.equals("")) {
            attribution += "aolMessager" + aolMessager + "', ";
        }
        byte[] b = null;
        try {
            b = MD5Crypt.digest(password.getBytes(), "md5");
            password = MD5Crypt.byteArrayToHexString(b);
        } catch (NoSuchAlgorithmException e) {
            return false;
        }
        participantGroupProfile = participantGroupProfile.substring(0, participantGroupProfile.length() - 1);
        _query = "UPDATE Participant, ParticipantProfile SET" + attribution + " WHERE participantId = " + participantId + " Participant.profileId = ParticipantProfile.profileId";
        if (this.databaseManager.queryInTransaction(_query, false)) {
            participantGroupQueries[0] = "DELETE FROM ParticipantGroup WHERE participantId = " + participantId;
            for (int i = 1; i < participantGroupIds.length; i++) {
                participantGroupQueries[i] = "INSERT INTO ParticipantGroup VALUES (" + participantGroupIds[i] + ", " + participantId + ")";
            }
            return this.databaseManager.queryInTransaction(participantGroupQueries, true);
        } else {
            return false;
        }
    }

    public boolean removeParticipant(int participantId) {
        _query = "DELETE FROM Participant WHERE participantId = " + participantId;
        return this.databaseManager.executeQuery(_query);
    }

    public GenericResultSet getAllParticipantGroups() {
        _query = "SELECT * FROM Groups";
        return this.databaseManager.queryAllRecords(_query);
    }

    public GenericResultSet getOneParticipantGroup(int groupId) {
        _query = "SELECT * FROM Groups WHERE groupId = " + groupId;
        return this.databaseManager.queryOneRecord(_query);
    }

    public boolean insertParticipantGroup(String name, String description, int instanceId) {
        _query = "INSERT INTO Groups VALUES ('', '" + name + "', '" + description + "', " + instanceId + ")";
        return this.databaseManager.executeQuery(_query);
    }

    public boolean updateParticipantGroup(int groupId, String name, String description, int instanceId) {
        _query = "UPDATE Groups SET name = " + name + ", description = '" + description + "', instanceId = " + instanceId + " WHERE groupId = " + groupId;
        return this.databaseManager.executeQuery(_query);
    }

    public boolean removeParticipantGroup(int groupId) {
        _query = "DELETE FROM Groups WHERE groupId = " + groupId;
        return this.databaseManager.executeQuery(_query);
    }

    public boolean insertParticipantInGroup(int groupId, int participantId) {
        _query = "INSERT INTO ParticipantGroup VALUES (" + groupId + ", " + participantId + ")";
        return this.databaseManager.executeQuery(_query);
    }

    public boolean isParticipantInGroup(int groupId, int participantId) {
        _query = "SELECT groupId FROM ParticipantGroup WHERE participantId = " + participantId + " AND groupId = " + groupId;
        GenericResultSet participantGroup = this.databaseManager.queryAllRecords(_query);
        return (participantGroup.size() == 1);
    }

    public GenericResultSet getAllServers() {
        _query = "SELECT * FROM Server";
        return this.databaseManager.queryAllRecords(_query);
    }

    public GenericResultSet getOneServer(int serverId) {
        _query = "SELECT * FROM Server WHERE serverId = " + serverId;
        return this.databaseManager.queryOneRecord(_query);
    }

    public boolean insertServer(String serverName, String serverIP, int port) {
        _query = "INSERT INTO Server VALUES ('', '" + serverName + "', '" + serverIP + "', " + port + ")";
        return this.databaseManager.executeQuery(_query);
    }

    public boolean updateServer(int serverId, String serverName, String serverIP) {
        _query = "UPDATE Server SET serverName = " + serverName + ", serverIP = '" + serverIP + "' WHERE serverId = " + serverId;
        return this.databaseManager.executeQuery(_query);
    }

    public boolean removeServer(int serverId) {
        _query = "DELETE FROM Server WHERE serverId = " + serverId;
        return this.databaseManager.executeQuery(_query);
    }

    public GenericResultSet getAllServices() {
        _query = "SELECT * FROM Service";
        return this.databaseManager.queryAllRecords(_query);
    }

    public GenericResultSet getAvailableService() {
        _query = "SELECT * FROM Service WHERE enabled = 'Y'";
        return this.databaseManager.queryAllRecords(_query);
    }

    public GenericResultSet getOneService(int serviceId) {
        _query = "SELECT * FROM Service WHERE serviceId = " + serviceId;
        return this.databaseManager.queryOneRecord(_query);
    }

    public boolean insertService(String name, String description, String implementationClasspath) {
        _query = "INSERT INTO Service VALUES ('', '" + name + "', '" + description + "', '" + implementationClasspath + "')";
        return this.databaseManager.executeQuery(_query);
    }

    public boolean updateService(int serviceId, String name, String description, String implementationClasspath) {
        _query = "UPDATE Service SET name = " + name + ", description = '" + description + "', implementationClasspath = '" + implementationClasspath + "' WHERE serviceId = " + serviceId;
        return this.databaseManager.executeQuery(_query);
    }

    public boolean removeService(int serviceId) {
        _query = "DELETE FROM Service WHERE serviceId = " + serviceId;
        return this.databaseManager.executeQuery(_query);
    }

    public GenericResultSet getServiceParameters(int serviceId) {
        _query = "SELECT parameterId FROM ServiceParameter WHERE serviceId = " + serviceId;
        return this.databaseManager.queryAllRecords(_query);
    }

    public GenericResultSet getAllServiceInstance(int serviceId, boolean justAvailable) {
        String availableCondition = (justAvailable) ? "ServiceInstance.enabled = 'Y' AND " : "";
        _query = "SELECT Instance.* FROM Instance, Service, ServiceInstance WHERE " + availableCondition + " Service.serviceId = " + serviceId + " AND Service.serviceId = ServiceInstance.serviceId AND ServiceInstance.instanceId = Instance.instanceId";
        return this.databaseManager.queryAllRecords(_query);
    }

    public GenericResultSet getOneServiceInstance(int serviceInstanceId) {
        _query = "SELECT * FROM Instance WHERE instanceId = " + serviceInstanceId;
        return this.databaseManager.queryOneRecord(_query);
    }

    public boolean insertServiceInstance(String name, String description, boolean enabled, int maxConnection, int serviceId, int managerId) {
        _query = "INSERT INTO Instance VALUES ('', '" + name + "', '" + description + "', " + maxConnection + ")";
        System.out.println(_query);
        if (this.databaseManager.queryInTransaction(_query, false)) {
            System.out.println("ENTROOOOOUUU!");
            char instanceEnabled = (enabled) ? 'Y' : 'N';
            int instanceId = this.databaseManager.getLastInsertedId();
            _query = "INSERT INTO ServiceInstance VALUES (" + instanceId + ", " + serviceId + ", " + managerId + ", '" + instanceEnabled + "')";
            return this.databaseManager.queryInTransaction(_query, true);
        }
        System.out.println("RETORNOU FALSOOOOOOOOOOO");
        return false;
    }

    public boolean updateServiceInstance(int serviceInstanceId, String name, String description, boolean enabled, int maxConnection, int serviceId, int managerId) {
        char instanceEnabled = (enabled) ? 'Y' : 'N';
        _query = "UPDATE Instance, ServiceInstance SET name = '" + name + "', description = '" + description + "', maxConnection = " + maxConnection + ", managerId = " + managerId + ", enabled = '" + instanceEnabled + "' WHERE Instance.instanceId = " + serviceInstanceId + " AND Instance.instanceId = ServiceInstance.instanceId";
        return this.databaseManager.executeQuery(_query);
    }

    public boolean removeServiceInstance(int serviceInstanceId) {
        _query = "DELETE FROM Instance WHERE instanceId = " + serviceInstanceId;
        return this.databaseManager.executeQuery(_query);
    }

    public String getInstanceImplementationClass(int instanceId) {
        _query = "SELECT implementationClasspath FROM Service, ServiceInstanceId WHERE Instance.instanceId = " + instanceId + " Instance.instanceId = ServiceInstance.instanceId AND Service.serviceId = ServiceInstance.serviceId";
        GenericResultSet result = this.databaseManager.queryOneRecord(_query);
        return result.getValue("implementationClasspath");
    }

    public GenericResultSet getAllServiceInstanceParameter(int instanceId) {
        _query = "SELECT serviceParameterName, serviceParameterdescription, implementationClasspath, ServiceParameter.serviceParameterId, value FROM ServiceParameter, InstanceParameter WHERE InstanceParameter.instanceId = " + instanceId + " AND InstanceParameter.serviceParameterId = ServiceParameter.serviceParameterId";
        return this.databaseManager.queryAllRecords(_query);
    }

    public GenericResultSet getOneServiceInstanceParameter(int instanceId, int serviceParameterId) {
        _query = "SELECT serviceParameterName, serviceParameterdescription, implementationClasspath, ServiceParameter.serviceParameterId, value FROM ServiceParameter, InstanceParameter WHERE InstanceParameter.instanceId = " + instanceId + " AND InstanceParameter.serviceParameterId = " + serviceParameterId + " AND InstanceParameter.serviceParameterId = ServiceParameter.serviceParameterId";
        return this.databaseManager.queryOneRecord(_query);
    }

    public GenericResultSet getParameter(String serviceParameterName) {
        _query = "SELECT * FROM ServiceParameter WHERE serviceParameterName = '" + serviceParameterName + "'";
        return this.databaseManager.queryOneRecord(_query);
    }

    public GenericResultSet getParticipantPermission(int participantId, int serviceId, int serviceInstanceId) {
        String condition = "";
        if ((serviceId != -1) && (serviceId != 0)) {
            condition += " AND Command.serviceId = " + serviceId;
        }
        _query = "SELECT Command.*, permission FROM Command, PermissionParticipant WHERE objectId = " + participantId + " AND instanceId = " + serviceInstanceId + " AND Command.enabled = 'Y' AND Command.commandId = PermissionParticipant.commandId" + condition;
        GenericResultSet permissionParticipant = this.databaseManager.queryAllRecords(_query);
        if (serviceInstanceId != -1) {
            _query = "SELECT groupId FROM Groups WHERE instanceId = " + serviceInstanceId;
            GenericResultSet instanceGroup = this.databaseManager.queryAllRecords(_query);
            if (instanceGroup.next()) {
                int groupId = instanceGroup.getIntValue("groupId");
                if (this.isParticipantInGroup(groupId, participantId)) {
                    _query = "SELECT Command.*, permission FROM Command, PermissionParticipantGroup WHERE PermissionParticipantGroup.objectId " + groupId + " AND Command.enabled = 'Y' AND Command.commandId = PermissionParticipantGroup.commandId" + condition;
                    GenericResultSet permissionParticipantGroup = this.databaseManager.queryAllRecords(_query);
                    permissionParticipant = PermissionManipulator.mergePermissions("commandId", permissionParticipant, permissionParticipantGroup);
                }
            }
            _query = "SELECT Command.*, permission FROM Command, PermissionInstance WHERE instanceId = " + serviceInstanceId + " AND Command.enabled = 'Y' AND Command.commandId = PermissionInstance.commandId" + condition;
            GenericResultSet permissionInstance = this.databaseManager.queryAllRecords(_query);
            permissionParticipant = PermissionManipulator.mergePermissions("commandId", permissionParticipant, permissionInstance);
        }
        _query = "SELECT Command.*, permission FROM Command, PermissionGeneral WHERE Command.commandId = PermissionGeneral.commandId AND Command.enabled = 'Y'";
        GenericResultSet permissionGeneral = this.databaseManager.queryAllRecords(_query);
        permissionParticipant = PermissionManipulator.mergePermissions("commandId", permissionParticipant, permissionGeneral);
        permissionParticipant.beforeFirst();
        return permissionParticipant;
    }

    public boolean isCommandPermitted(int commandId, ArCoLIVEParticipant participant, int serviceId, int serviceInstanceId) {
        int participantId = participant.getParticipantDescriptor().getParticipantId();
        GenericResultSet permissions = this.getParticipantPermission(participantId, serviceId, serviceInstanceId);
        Hashtable condition = new Hashtable();
        condition.put("commandId", Integer.toString(commandId));
        condition.put("permission", "permitted");
        if (participantId == 0) {
            condition.put("authenticated", "N");
        }
        GenericResultSet search = permissions.searchByOneField(condition);
        System.out.println(search.size() + " QUANTIDADEEEEEEEEEE");
        return (search.size() == 1);
    }

    public boolean verifyCommandPermission(int commandId, int participantId, int serviceId, int serviceInstanceId) {
        GenericResultSet permissions = this.getParticipantPermission(participantId, serviceId, serviceInstanceId);
        Hashtable condition = new Hashtable();
        condition.put("commandId", Integer.toString(commandId));
        condition.put("permission", "permitted");
        GenericResultSet search = permissions.searchByOneField(condition);
        return (search.size() == 1);
    }

    public boolean isCommandPermitted(int commandId, ArCoLIVEParticipant participant, int serviceId) {
        return this.isCommandPermitted(commandId, participant, serviceId, -1);
    }

    public boolean isCommandPermitted(int commandId, ArCoLIVEParticipant participant) {
        return this.isCommandPermitted(commandId, participant, -1, -1);
    }

    public boolean grantParticipantPermission(int commandId, int objectId, int serviceId, int serviceInstanceId, String object) {
        if (!this.verifyCommandPermission(commandId, objectId, serviceId, serviceInstanceId)) {
            _query = "INSERT INTO " + object + " VALUES (" + serviceInstanceId + ")";
            return this.databaseManager.executeQuery(_query);
        }
        return true;
    }

    public boolean revokeParticipantPermission(int commandId, int objectId, int serviceId, int serviceInstanceId, String object, boolean hard) {
        if (this.verifyCommandPermission(commandId, objectId, serviceId, serviceInstanceId)) {
            if (hard) {
                _query = "SELECT commandId FROM " + object + " WHERE commandId = " + commandId + " AND instanceId = " + serviceInstanceId + " AND objectId = " + objectId;
                GenericResultSet genericResultSet = this.databaseManager.queryAllRecords(_query);
                if (genericResultSet.size() != 0) {
                    _query = "UPDATE " + object + " SET permission = 'not permitted' WHERE commandId = " + commandId + " AND instanceId = " + serviceInstanceId + " AND objectId = " + objectId;
                } else {
                    _query = "INSERT INTO " + object + " VALUES (" + serviceInstanceId + ", " + commandId + ", " + objectId + ", 'not permitted')";
                }
                return this.databaseManager.executeQuery(_query);
            } else {
                _query = "DELETE FROM " + object + " WHERE commandId = " + commandId + " AND instanceId = " + serviceInstanceId + " AND objectId = " + objectId;
                return this.databaseManager.executeQuery(_query);
            }
        } else {
            if (!hard) {
                _query = "DELETE FROM " + object + " WHERE commandId = " + commandId + " AND instanceId = " + serviceInstanceId + " AND objectId = " + objectId;
                return this.databaseManager.executeQuery(_query);
            }
        }
        return true;
    }

    public static void main(String[] args) {
        ArCoLIVEPersistenceDatabaseImpl db = new ArCoLIVEPersistenceDatabaseImpl();
        if (db.connect()) {
            GenericResultSet rs = db.getParticipantPermission(1, 0, 1);
            while (rs.next()) {
                System.out.println("commandId " + rs.getValue("commandId"));
            }
        }
    }
}
