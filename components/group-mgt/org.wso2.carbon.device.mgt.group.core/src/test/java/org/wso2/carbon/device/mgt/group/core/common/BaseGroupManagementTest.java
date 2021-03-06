/*
 *   Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.wso2.carbon.device.mgt.group.core.common;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.w3c.dom.Document;
import org.wso2.carbon.base.MultitenantConstants;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.device.mgt.common.DeviceManagementException;
import org.wso2.carbon.device.mgt.core.util.DeviceManagerUtil;
import org.wso2.carbon.device.mgt.group.core.TestUtils;
import org.wso2.carbon.device.mgt.group.core.dao.GroupManagementDAOFactory;

import javax.sql.DataSource;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class BaseGroupManagementTest {

    private static final Log log = LogFactory.getLog(BaseGroupManagementTest.class);
    private DataSource dataSource;

    @BeforeSuite public void setupDataSource() throws Exception {
        this.initDataSource();
        this.initSQLScript();
        this.initializeCarbonContext();
    }

    public void initDataSource() {
        try {
            this.dataSource = this.getDataSource(this.readDataSourceConfig());
            GroupManagementDAOFactory.init(dataSource);
        } catch (DeviceManagementException e) {
            String msg = "Error occurred while obtaining datasource";
            log.error(msg, e);
            Assert.fail(msg, e);
        }
    }

    @BeforeClass public abstract void init();

    private DataSource getDataSource(DataSourceConfig config) {
        PoolProperties properties = new PoolProperties();
        properties.setUrl(config.getUrl());
        properties.setDriverClassName(config.getDriverClassName());
        properties.setUsername(config.getUser());
        properties.setPassword(config.getPassword());
        return new org.apache.tomcat.jdbc.pool.DataSource(properties);
    }

    private void initializeCarbonContext() {

        if (System.getProperty("carbon.home") == null) {
            File file = new File("src/test/resources/carbon-home");
            if (file.exists()) {
                System.setProperty("carbon.home", file.getAbsolutePath());
            }
            file = new File("../resources/carbon-home");
            if (file.exists()) {
                System.setProperty("carbon.home", file.getAbsolutePath());
            }
            file = new File("../../resources/carbon-home");
            if (file.exists()) {
                System.setProperty("carbon.home", file.getAbsolutePath());
            }
            file = new File("../../../resources/carbon-home");
            if (file.exists()) {
                System.setProperty("carbon.home", file.getAbsolutePath());
            }
        }

        PrivilegedCarbonContext.getThreadLocalCarbonContext()
                .setTenantDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(MultitenantConstants.SUPER_TENANT_ID);
    }

    private DataSourceConfig readDataSourceConfig() throws DeviceManagementException {
        String fileSrc = "src/test/resources/config/datasource/data-source-config.xml";
        try {
            File file = new File(fileSrc);
            Document doc = DeviceManagerUtil.convertToDocument(file);
            JAXBContext testDBContext = JAXBContext.newInstance(DataSourceConfig.class);
            Unmarshaller unmarshaller = testDBContext.createUnmarshaller();
            return (DataSourceConfig) unmarshaller.unmarshal(doc);
        } catch (JAXBException e) {
            throw new DeviceManagementException("Error occurred while reading data source configuration from " + fileSrc, e);
        }
    }

    private void initSQLScript() throws Exception {
        Connection conn = null;
        Statement stmt = null;
        try {
            conn = this.getDataSource().getConnection();
            stmt = conn.createStatement();
            stmt.executeUpdate("RUNSCRIPT FROM './src/test/resources/sql/h2.sql'");
        } finally {
            TestUtils.cleanupResources(conn, stmt, null);
        }
    }

    @AfterSuite public void deleteData() {
        Connection conn = null;
        try {
            conn = getDataSource().getConnection();
            conn.setAutoCommit(false);

            this.cleanupGroupData(conn);

            conn.commit();
        } catch (SQLException e) {
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException e1) {
                log.error("Error occurred while roll-backing the transaction", e);
            }
            String msg = "Error occurred while cleaning up temporary data generated during test execution";
            log.error(msg, e);
            Assert.fail(msg, e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    log.warn("Error occurred while closing the connection", e);
                }
            }
        }
    }

    private void cleanupGroupData(Connection conn) throws SQLException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("DELETE FROM DM_GROUP");
            stmt.execute();
        } finally {
            if (stmt != null) {
                stmt.close();
            }
        }
    }

    public DataSource getDataSource() {
        return dataSource;
    }

}
