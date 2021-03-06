/*
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * you may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.device.mgt.core;

import org.wso2.carbon.device.mgt.common.*;
import org.wso2.carbon.device.mgt.common.app.mgt.Application;
import org.wso2.carbon.device.mgt.common.app.mgt.ApplicationManagementException;
import org.wso2.carbon.device.mgt.common.app.mgt.ApplicationManager;
import org.wso2.carbon.device.mgt.common.license.mgt.License;
import org.wso2.carbon.device.mgt.common.license.mgt.LicenseManagementException;
import org.wso2.carbon.device.mgt.common.operation.mgt.Operation;
import org.wso2.carbon.device.mgt.common.operation.mgt.OperationManagementException;
import org.wso2.carbon.device.mgt.common.spi.DeviceManagementService;
import org.wso2.carbon.device.mgt.core.common.TestDataHolder;
import org.wso2.carbon.device.mgt.core.service.DeviceManagementProviderService;

import java.util.List;

public class TestDeviceManagementService implements DeviceManagementService {

    private String providerType;
    private String tenantDomain;

    public TestDeviceManagementService(String deviceType,String tenantDomain){
        providerType = deviceType;
        this.tenantDomain = tenantDomain;
    }
    @Override
    public String getType() {
        return providerType;
    }

    @Override
    public String getProviderTenantDomain() { return tenantDomain;}

    @Override
    public boolean isSharedWithAllTenants() {
        return true;
    }

    @Override
    public String[] getSharedTenantsDomain() {
        return null;
    }

    @Override
    public void init() throws DeviceManagementException {

    }

    @Override
    public DeviceManager getDeviceManager() {
        return new TestDeviceManager();
    }

    @Override
    public ApplicationManager getApplicationManager() {
        return null;
    }

    @Override
    public Application[] getApplications(String domain, int pageNumber, int size)
            throws ApplicationManagementException {
        return new Application[0];
    }

    @Override
    public void updateApplicationStatus(DeviceIdentifier deviceId, Application application, String status)
            throws ApplicationManagementException {

    }

    @Override
    public String getApplicationStatus(DeviceIdentifier deviceId, Application application)
            throws ApplicationManagementException {
        return null;
    }

    @Override
    public void installApplication(Operation operation, List<DeviceIdentifier> deviceIdentifiers)
            throws ApplicationManagementException {

    }
}
