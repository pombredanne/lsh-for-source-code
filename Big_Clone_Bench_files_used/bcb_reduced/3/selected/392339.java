package de.iritgo.aktera.authentication.defaultauth.module;

import de.iritgo.aktera.authentication.defaultauth.entity.AkteraGroup;
import de.iritgo.aktera.model.ModelException;
import de.iritgo.aktera.model.ModelRequest;
import de.iritgo.aktera.persist.CreateHandler;
import de.iritgo.aktera.persist.PersistenceException;
import de.iritgo.aktera.persist.PersistentFactory;
import de.iritgo.simplelife.string.StringTools;
import org.apache.avalon.framework.logger.Logger;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Database creation.
 */
public class ModuleCreateHandler extends CreateHandler {

    /**
	 * @see de.iritgo.aktera.persist.CreateHandler#createTables(ModelRequest,
	 *      de.iritgo.aktera.persist.PersistentFactory, java.sql.Connection,
	 *      Logger)
	 */
    @Override
    public void createTables(ModelRequest request, PersistentFactory persistentFactory, Connection connection, Logger logger) throws ModelException, PersistenceException, SQLException {
        createTable("AkteraGroup", "id serial primary key", "name varchar(255) not null", "protect boolean", "title varchar(255)", "visible boolean");
        createTable("AkteraGroupEntry", "groupId int4 not null", "id serial primary key", "position int4 not null", "userId int4 not null");
        createTable("keelgroupmembers", "GroupName varchar(80) not null", "UniqId int4 not null");
        createTable("keelusers", "email varchar(132)", "ldapName varchar(120)", "userName varchar(80) not null", "passwd varchar(255)", "uniqid serial primary key");
        createTable("keelgroups", "Descrip varchar(132) not null", "GroupName varchar(80) not null");
    }

    /**
	 * @see de.iritgo.aktera.persist.CreateHandler#createData(de.iritgo.aktera.persist.PersistentFactory,
	 *      java.sql.Connection, Logger, ModelRequest)
	 */
    @Override
    public void createData(PersistentFactory persistentFactory, Connection connection, Logger logger, ModelRequest request) throws ModelException, PersistenceException, SQLException {
        createInstanceSecurity("de.iritgo.aktera.persist.defaultpersist.DefaultPersistent", "keel.user", "root", "*");
        createInstanceSecurity("de.iritgo.aktera.persist.defaultpersist.DefaultPersistent", "keel.group", "root", "*");
        createInstanceSecurity("de.iritgo.aktera.persist.defaultpersist.DefaultPersistent", "keel.groupmembers", "root", "*");
        createInstanceSecurity("de.iritgo.aktera.persist.defaultpersist.DefaultPersistent", "keel.user", "anonymous", "L");
        createInstanceSecurity("de.iritgo.aktera.persist.defaultpersist.DefaultPersistent", "keel.group", "anonymous", "L");
        createInstanceSecurity("de.iritgo.aktera.persist.defaultpersist.DefaultPersistent", "keel.groupmembers", "anonymous", "L");
        createInstanceSecurity("de.iritgo.aktera.persist.defaultpersist.DefaultPersistent", "keel.user", "guest", "L");
        createInstanceSecurity("de.iritgo.aktera.persist.defaultpersist.DefaultPersistent", "keel.group", "guest", "L");
        createInstanceSecurity("de.iritgo.aktera.persist.defaultpersist.DefaultPersistent", "keel.groupmembers", "guest", "L");
        update("INSERT INTO keelgroups (GroupName, Descrip) values ('admin', 'Administrator')");
        update("INSERT INTO keelgroups (GroupName, Descrip) values ('manager', 'Manager')");
        update("INSERT INTO keelgroups (GroupName, Descrip) values ('user', 'User')");
        update("INSERT INTO keelusers (uniqid, userName, email, passwd)" + " values (0, 'anonymous', 'anonymous@unknown', '" + StringTools.digest("") + "')");
        update("INSERT INTO keelusers (uniqid, userName, email, passwd)" + " values (1, 'admin', 'admin@unknown', '" + StringTools.digest("admin") + "')");
        update("INSERT INTO keelusers (uniqid, userName, email, passwd)" + " values (2, 'manager', 'manager@unknown', '" + StringTools.digest("manager") + "')");
        update("ALTER SEQUENCE keelusers_uniqid_seq START WITH 3");
        update("INSERT INTO keelgroupmembers (UniqId, GroupName) values (0, 'anonymous')");
        update("INSERT INTO keelgroupmembers (UniqId, GroupName) values (1, 'root')");
        update("INSERT INTO keelgroupmembers (UniqId, GroupName) values (1, 'admin')");
        update("INSERT INTO keelgroupmembers (UniqId, GroupName) values (1, 'manager')");
        update("INSERT INTO keelgroupmembers (UniqId, GroupName) values (1, 'user')");
        update("INSERT INTO keelgroupmembers (UniqId, GroupName) values (2, 'manager')");
        update("INSERT INTO keelgroupmembers (UniqId, GroupName) values (2, 'user')");
        update("INSERT INTO AkteraGroup (name, protect, title, visible) values ('" + AkteraGroup.GROUP_NAME_ADMINISTRATOR + "', true, '$Aktera:administrators', false)");
        update("INSERT INTO AkteraGroup (name, protect, title, visible) values ('" + AkteraGroup.GROUP_NAME_MANAGER + "', true, '$Aktera:managers', false)");
        update("INSERT INTO AkteraGroup (name, protect, title, visible) values ('" + AkteraGroup.GROUP_NAME_USER + "', true, '$Aktera:users', true)");
        update("INSERT INTO AkteraGroupEntry (groupId, userId, position) values (1, 1, 1)");
        update("INSERT INTO AkteraGroupEntry (groupId, userId, position) values (2, 2, 1)");
    }
}
