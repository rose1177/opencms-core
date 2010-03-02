/*
 * File   : $Source: /alkacon/cvs/opencms/src-setup/org/opencms/setup/db/update6to7/postgresql/CmsUpdateDBAlterTables.java,v $
 * Date   : $Date: 2010/02/24 12:44:23 $
 * Version: $Revision: 1.1 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Management System
 *
 * Copyright (c) 2002 - 2009 Alkacon Software GmbH (http://www.alkacon.com)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * For further information about Alkacon Software GmbH, please see the
 * company website: http://www.alkacon.com
 *
 * For further information about OpenCms, please see the
 * project website: http://www.opencms.org
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.opencms.setup.db.update6to7.postgresql;

import org.opencms.setup.CmsSetupDb;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * PostgreSQL implementation of the generic Alter Table class.<p>
 * 
 * @author Ivan Codarin
 * @author Georgi Naplatanov
 *
 * @version $Revision: 1.1 $ 
 * 
 * @since 7.0.2
 */
public class CmsUpdateDBAlterTables extends org.opencms.setup.db.update6to7.CmsUpdateDBAlterTables {

    /** Constant for the sql query to alter table field.<p> */
    private static final String QUERY_ALTER_FIELD = "Q_ALTER_FIELD_TYPE";

    /** Constant for the sql query to set default value of table field.<p> */
    private static final String QUERY_SET_DEFAULT_VALUE = "Q_ALTER_FIELD_SET_DEFAULT";

    /** Constant for the sql query to drop default value of table field.<p> */
    private static final String QUERY_DROP_DEFAULT_VALUE = "Q_ALTER_FIELD_DROP_DEFAULT";

    /** Constant for the sql query to set not null values of table field.<p> */
    private static final String QUERY_SET_NOT_NULL = "Q_ALTER_FIELD_SET_NOT_NULL";

    /** Constant for the sql query to drop not null constraint of table field.<p> */
    private static final String QUERY_DROP_NOT_NULL = "Q_ALTER_FIELD_DROP_NOT_NULL";

    /** Constant for the sql replacement of the field name.<p> */
    private static final String REPLACEMENT_FIELD_NAME = "${fieldname}";

    /** Constant for the sql replacement of the field type.<p> */
    private static final String REPLACEMENT_FIELD_TYPE = "${fieldtype}";

    /** Constant for the sql replacement of the field default value.<p> */
    private static final String REPLACEMENT_FIELD_DEFAULT_VALUE = "${defaultvalue}";

    /** Constant for the SQL query properties.<p> */
    private static final String QUERY_PROPERTY_FILE_ORACLE = "cms_alter_remaining_queries.properties";

    private static final String DROP_NOT_NULL = "0";
    private static final String SET_NOT_NULL = "1";
    private static final String NO_CHANGE = "2";

    private static final String DV_NO_CHANGE = null;
    private static final String DV_DROP = "-- droping the default value --";

    /**
     * Array contains differences after upgrade and clean install of OpenCms 7.0.1.<p>
     * <pre>
     *  [0] - table name
     *  [1] - field name
     *  [3] - database type of the field
     *  [4] - SET/DROP NULL
     *  [5] - DEFAULT VALUE information
     * </pre>
     */
    private static final String[][] DB_ARRAY = {
        {"cms_groups", "group_name", "varchar(128)", NO_CHANGE, DV_NO_CHANGE},
        {"cms_groups", "group_ou", "", SET_NOT_NULL, DV_NO_CHANGE},

        {"cms_offline_properties", "property_value", "varchar(2048)", NO_CHANGE, DV_NO_CHANGE},

        {"cms_offline_propertydef", "propertydef_type", "", NO_CHANGE, DV_DROP},

        {"cms_offline_resources", "date_content", "", NO_CHANGE, DV_DROP},
        {"cms_offline_resources", "project_lastmodified", "", SET_NOT_NULL, DV_NO_CHANGE},
        {"cms_offline_resources", "resource_version", "", SET_NOT_NULL, DV_DROP},

        {"cms_offline_structure", "resource_path", "varchar(1024)", DROP_NOT_NULL, DV_NO_CHANGE},
        {"cms_offline_structure", "structure_state", "integer", NO_CHANGE, DV_NO_CHANGE},
        {"cms_offline_structure", "date_released", "", SET_NOT_NULL, DV_NO_CHANGE},
        {"cms_offline_structure", "date_expired", "", SET_NOT_NULL, DV_NO_CHANGE},
        {"cms_offline_structure", "structure_version", "", NO_CHANGE, DV_DROP},

        {"cms_online_properties", "property_value", "varchar(2048)", NO_CHANGE, DV_NO_CHANGE},

        {"cms_online_propertydef", "propertydef_type", "", NO_CHANGE, DV_DROP},

        {"cms_online_resources", "resource_state", "integer", NO_CHANGE, DV_NO_CHANGE},
        {"cms_online_resources", "date_content", "", NO_CHANGE, DV_DROP},
        {"cms_online_resources", "project_lastmodified", "", SET_NOT_NULL, DV_NO_CHANGE},
        {"cms_online_resources", "resource_version", "", NO_CHANGE, DV_DROP},

        {"cms_online_structure", "resource_path", "varchar(1024)", DROP_NOT_NULL, DV_NO_CHANGE},
        {"cms_online_structure", "structure_state", "integer", NO_CHANGE, DV_NO_CHANGE},
        {"cms_online_structure", "structure_version", "", NO_CHANGE, DV_DROP},

        {"cms_projectresources", "resource_path", "varchar(1024)", NO_CHANGE, DV_NO_CHANGE},

        {"cms_projects", "project_ou", "", SET_NOT_NULL, DV_NO_CHANGE},

        {"cms_publish_history", "resource_path", "varchar(1024)", NO_CHANGE, DV_NO_CHANGE},

        {"cms_resource_locks", "resource_path", "", DROP_NOT_NULL, DV_NO_CHANGE},

        {"cms_staticexport_links", "link_rfs_path", "varchar(1024)", DROP_NOT_NULL, DV_NO_CHANGE},
        {"cms_staticexport_links", "link_parameter", "varchar(1024)", NO_CHANGE, DV_NO_CHANGE},
        {"cms_staticexport_links", "link_timestamp", "", SET_NOT_NULL, DV_NO_CHANGE},

        {"cms_users", "user_name", "varchar(128)", NO_CHANGE, DV_NO_CHANGE},
        {"cms_users", "user_password", "varchar(64)", NO_CHANGE, DV_NO_CHANGE},
        {"cms_users", "user_firstname", "varchar(128)", NO_CHANGE, DV_NO_CHANGE},
        {"cms_users", "user_lastname", "varchar(128)", NO_CHANGE, DV_NO_CHANGE},
        {"cms_users", "user_email", "varchar(128)", NO_CHANGE, DV_NO_CHANGE},
        {"cms_users", "user_ou", "", SET_NOT_NULL, DV_NO_CHANGE},
        {"cms_users", "user_datecreated", "", NO_CHANGE, DV_DROP}

    };

    /**
     * Constructor.<p>
     * 
     * @throws IOException if the sql queries properties file could not be read
     */
    public CmsUpdateDBAlterTables()
    throws IOException {

        super();
        loadQueryProperties(getPropertyFileLocation() + QUERY_PROPERTY_FILE_ORACLE);
    }

    /**
     * @see org.opencms.setup.db.A_CmsUpdateDBPart#internalExecute(org.opencms.setup.CmsSetupDb)
     */
    @Override
    protected void internalExecute(CmsSetupDb dbCon) throws SQLException {

        super.internalExecute(dbCon);
        fixSchema(dbCon);
    }

    private void initReplaceser(Map<String, String> replacer, String tableName, String fieldName) {

        replacer.clear();
        replacer.put(REPLACEMENT_TABLENAME, tableName);
        replacer.put(REPLACEMENT_FIELD_NAME, fieldName);
    }

    private void fixSchema(CmsSetupDb dbCon) throws SQLException {

        Map<String, String> replacer = new HashMap<String, String>();
        String tableName;
        String fieldName;
        String fieldType;
        String nullAcceptInfo;
        String defaultValue;
        String query;
        for (int i = 0; i < DB_ARRAY.length; i++) {
            tableName = DB_ARRAY[i][0];
            fieldName = DB_ARRAY[i][1];
            fieldType = DB_ARRAY[i][2];
            nullAcceptInfo = DB_ARRAY[i][3];
            defaultValue = DB_ARRAY[i][4];

            if ((fieldType != null) && (fieldType.length() > 0)) {
                initReplaceser(replacer, tableName, fieldName);
                replacer.put(REPLACEMENT_FIELD_TYPE, fieldType);
                query = readQuery(QUERY_ALTER_FIELD);
                dbCon.updateSqlStatement(query, replacer, null);
            }

            if ((nullAcceptInfo != null) && (!nullAcceptInfo.equals(NO_CHANGE))) {
                String q;
                if (nullAcceptInfo.equals(DROP_NOT_NULL)) {
                    q = QUERY_DROP_NOT_NULL;
                } else {
                    q = QUERY_SET_NOT_NULL;
                }
                query = readQuery(q);
                initReplaceser(replacer, tableName, fieldName);
                dbCon.updateSqlStatement(query, replacer, null);
            }

            if ((defaultValue != null) && (defaultValue.length() > 0)) {
                initReplaceser(replacer, tableName, fieldName);
                if (defaultValue.equals(DV_DROP)) {
                    query = readQuery(QUERY_DROP_DEFAULT_VALUE);
                    dbCon.updateSqlStatement(query, replacer, null);
                } else {
                    query = readQuery(QUERY_SET_DEFAULT_VALUE);
                    replacer.put(REPLACEMENT_FIELD_DEFAULT_VALUE, defaultValue);
                    dbCon.updateSqlStatement(query, replacer, null);
                }
            }

        }

    }

}