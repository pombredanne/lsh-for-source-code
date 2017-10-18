package de.iritgo.aktera.aktario.module;

import de.iritgo.aktera.model.ModelException;
import de.iritgo.aktera.model.ModelRequest;
import de.iritgo.aktera.persist.CreateHandler;
import de.iritgo.aktera.persist.PersistenceException;
import de.iritgo.aktera.persist.Persistent;
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
	 * @see de.iritgo.aktera.persist.CreateHandler#createTables(ModelRequest, de.iritgo.aktera.persist.PersistentFactory, java.sql.Connection, Logger)
	 */
    @Override
    public void createTables(ModelRequest request, PersistentFactory persistentFactory, Connection connection, Logger logger) throws ModelException, PersistenceException, SQLException {
        createTable("ApplicationInstance", "applicationId varchar(80)", "id bigint not null", "name varchar(80)");
        createTable("AktarioUser", "email varchar(80)", "fullName varchar(80)", "id bigint not null", "name varchar(80)", "password varchar(255)", "role int4");
        createTable("AktarioUserPreferences", "alwaysDrawWindowContents int4", "colorScheme varchar(64)", "id bigint not null", "language varchar(16)");
        createTable("AktarioUserProfile", "id bigint not null");
        createTable("AktarioUserRegistry", "id bigint not null");
        createTable("IritgoNamedObjects", "id bigint not null", "name varchar(80) not null", "userId bigint not null");
        createTable("IritgoObjectList", "attribute varchar(64) not null", "elemId bigint not null", "elemType varchar(64) not null", "id bigint not null", "type varchar(64) not null");
        createTable("IritgoProperties", "name varchar(80) not null", "value varchar(80)");
        createTable("IritgoUser", "email varchar(80)", "id bigint not null", "name varchar(80)", "password varchar(255)");
        createTable("Participant", "id bigint not null");
        createTable("ParticipantGroup", "id bigint not null", "iritgoUserName varchar(80)");
        createTable("Room", "id bigint not null", "name varchar(80)");
    }

    /**
	 * @see de.iritgo.aktera.persist.CreateHandler#createData(de.iritgo.aktera.persist.PersistentFactory, java.sql.Connection, Logger, ModelRequest)
	 */
    @Override
    public void createData(PersistentFactory persistentFactory, Connection connection, Logger logger, ModelRequest request) throws ModelException, PersistenceException, SQLException {
        update("INSERT INTO IritgoProperties (name, value) values ('persist.ids.nextvalue', '1000000')");
        createInstanceSecurity("de.iritgo.aktera.ui.listing.List", "aktera.aktario.list-participants", "user", "*");
        Persistent persistent = persistentFactory.create("aktera.AktarioUserRegistry");
        persistent.setField("id", new Integer(11000));
        persistent.add();
        Persistent iritgoUser = persistentFactory.create("aktera.IritgoUser");
        iritgoUser.setField("id", new Integer(10100));
        iritgoUser.setField("name", "admin");
        iritgoUser.setField("password", StringTools.digest("admin"));
        iritgoUser.setField("email", "admin@unknown");
        iritgoUser.add();
        Persistent aktarioUser = persistentFactory.create("aktera.AktarioUser");
        aktarioUser.setField("id", new Integer(10100));
        aktarioUser.setField("name", "admin");
        aktarioUser.setField("fullName", "Administrator");
        aktarioUser.setField("password", StringTools.digest("admin"));
        aktarioUser.setField("email", "admin@localhost");
        aktarioUser.setField("role", new Integer(0));
        aktarioUser.add();
        Persistent aktarioPreferences = persistentFactory.create("aktera.AktarioUserPreferences");
        aktarioPreferences.setField("id", new Integer(10100));
        aktarioPreferences.setField("colorScheme", "com.jgoodies.looks.plastic.theme.KDE");
        aktarioPreferences.setField("language", "de");
        aktarioPreferences.setField("alwaysDrawWindowContents", new Integer(1));
        aktarioPreferences.add();
        Persistent aktarioProfile = persistentFactory.create("aktera.AktarioUserProfile");
        aktarioProfile.setField("id", new Integer(10100));
        aktarioProfile.add();
        Persistent iritgoObjectList = persistentFactory.create("aktera.IritgoObjectList");
        iritgoObjectList.setField("id", new Integer(11000));
        iritgoObjectList.setField("type", "AktarioUserRegistry");
        iritgoObjectList.setField("attribute", "users");
        iritgoObjectList.setField("elemType", "AktarioUser");
        iritgoObjectList.setField("elemId", new Integer(10100));
        iritgoObjectList.add();
        iritgoObjectList = persistentFactory.create("aktera.IritgoObjectList");
        iritgoObjectList.setField("id", new Integer(11000));
        iritgoObjectList.setField("type", "AktarioUserRegistry");
        iritgoObjectList.setField("attribute", "profiles");
        iritgoObjectList.setField("elemType", "AktarioUserProfile");
        iritgoObjectList.setField("elemId", new Integer(10100));
        iritgoObjectList.add();
        iritgoObjectList = persistentFactory.create("aktera.IritgoObjectList");
        iritgoObjectList.setField("id", new Integer(10100));
        iritgoObjectList.setField("type", "AktarioUserProfile");
        iritgoObjectList.setField("attribute", "preferences");
        iritgoObjectList.setField("elemType", "AktarioUserPreferences");
        iritgoObjectList.setField("elemId", new Integer(10100));
        iritgoObjectList.add();
        Persistent iritgoNamedObjects = persistentFactory.create("aktera.IritgoNamedObjects");
        iritgoNamedObjects.setField("userId", new Integer(10100));
        iritgoNamedObjects.setField("name", "AktarioUserPreferences");
        iritgoNamedObjects.setField("id", new Integer(10100));
        iritgoNamedObjects.add();
        iritgoNamedObjects = persistentFactory.create("aktera.IritgoNamedObjects");
        iritgoNamedObjects.setField("userId", new Integer(10100));
        iritgoNamedObjects.setField("name", "AktarioUserProfile");
        iritgoNamedObjects.setField("id", new Integer(10100));
        iritgoNamedObjects.add();
        iritgoNamedObjects = persistentFactory.create("aktera.IritgoNamedObjects");
        iritgoNamedObjects.setField("userId", new Integer(10100));
        iritgoNamedObjects.setField("name", "AktarioUser");
        iritgoNamedObjects.setField("id", new Integer(10100));
        iritgoNamedObjects.add();
        iritgoUser = persistentFactory.create("aktera.IritgoUser");
        iritgoUser.setField("id", new Integer(10101));
        iritgoUser.setField("name", "manager");
        iritgoUser.setField("password", StringTools.digest("manager"));
        iritgoUser.setField("email", "manager@unknown");
        iritgoUser.add();
        aktarioUser = persistentFactory.create("aktera.AktarioUser");
        aktarioUser.setField("id", new Integer(10101));
        aktarioUser.setField("name", "manager");
        aktarioUser.setField("fullName", "Manager");
        aktarioUser.setField("password", StringTools.digest("manager"));
        aktarioUser.setField("email", "manager@unknown");
        aktarioUser.setField("role", new Integer(0));
        aktarioUser.add();
        aktarioPreferences = persistentFactory.create("aktera.AktarioUserPreferences");
        aktarioPreferences.setField("id", new Integer(10101));
        aktarioPreferences.setField("colorScheme", "com.jgoodies.looks.plastic.theme.KDE");
        aktarioPreferences.setField("language", "de");
        aktarioPreferences.setField("alwaysDrawWindowContents", new Integer(1));
        aktarioPreferences.add();
        aktarioProfile = persistentFactory.create("aktera.AktarioUserProfile");
        aktarioProfile.setField("id", new Integer(10101));
        aktarioProfile.add();
        iritgoObjectList = persistentFactory.create("aktera.IritgoObjectList");
        iritgoObjectList.setField("id", new Integer(11000));
        iritgoObjectList.setField("type", "AktarioUserRegistry");
        iritgoObjectList.setField("attribute", "users");
        iritgoObjectList.setField("elemType", "AktarioUser");
        iritgoObjectList.setField("elemId", new Integer(10101));
        iritgoObjectList.add();
        iritgoObjectList = persistentFactory.create("aktera.IritgoObjectList");
        iritgoObjectList.setField("id", new Integer(11000));
        iritgoObjectList.setField("type", "AktarioUserRegistry");
        iritgoObjectList.setField("attribute", "profiles");
        iritgoObjectList.setField("elemType", "AktarioUserProfile");
        iritgoObjectList.setField("elemId", new Integer(10101));
        iritgoObjectList.add();
        iritgoObjectList = persistentFactory.create("aktera.IritgoObjectList");
        iritgoObjectList.setField("id", new Integer(10101));
        iritgoObjectList.setField("type", "AktarioUserProfile");
        iritgoObjectList.setField("attribute", "preferences");
        iritgoObjectList.setField("elemType", "AktarioUserPreferences");
        iritgoObjectList.setField("elemId", new Integer(10101));
        iritgoObjectList.add();
        iritgoNamedObjects = persistentFactory.create("aktera.IritgoNamedObjects");
        iritgoNamedObjects.setField("userId", new Integer(10101));
        iritgoNamedObjects.setField("name", "AktarioUserPreferences");
        iritgoNamedObjects.setField("id", new Integer(10101));
        iritgoNamedObjects.add();
        iritgoNamedObjects = persistentFactory.create("aktera.IritgoNamedObjects");
        iritgoNamedObjects.setField("userId", new Integer(10101));
        iritgoNamedObjects.setField("name", "AktarioUserProfile");
        iritgoNamedObjects.setField("id", new Integer(10101));
        iritgoNamedObjects.add();
        iritgoNamedObjects = persistentFactory.create("aktera.IritgoNamedObjects");
        iritgoNamedObjects.setField("userId", new Integer(10101));
        iritgoNamedObjects.setField("name", "AktarioUser");
        iritgoNamedObjects.setField("id", new Integer(10101));
        iritgoNamedObjects.add();
    }
}
