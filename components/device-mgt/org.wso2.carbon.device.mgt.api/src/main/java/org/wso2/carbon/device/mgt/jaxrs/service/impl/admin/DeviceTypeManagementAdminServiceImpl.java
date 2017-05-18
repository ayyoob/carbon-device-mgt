/*
 *   Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.device.mgt.jaxrs.service.impl.admin;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.device.mgt.common.DeviceManagementException;
import org.wso2.carbon.device.mgt.core.dto.DeviceType;
import org.wso2.carbon.device.mgt.extensions.device.type.template.HTTPDeviceTypeManagerService;
import org.wso2.carbon.device.mgt.jaxrs.service.api.admin.DeviceTypeManagementAdminService;
import org.wso2.carbon.device.mgt.jaxrs.util.DeviceMgtAPIUtils;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/admin/device-types")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DeviceTypeManagementAdminServiceImpl implements DeviceTypeManagementAdminService {

    private static final Log log = LogFactory.getLog(DeviceTypeManagementAdminServiceImpl.class);

    @Override
    @POST
    public Response addDeviceType(DeviceType deviceType) {
        if (deviceType != null) {
            try {
                if (DeviceMgtAPIUtils.getDeviceManagementService().getDeviceType(deviceType.getName()) != null) {
                    String msg = "Device type already available, " + deviceType.getName();
                    return Response.status(Response.Status.CONFLICT).entity(msg).build();
                }
                HTTPDeviceTypeManagerService httpDeviceTypeManagerService = new HTTPDeviceTypeManagerService
                        (deviceType.getName(), deviceType.getDeviceTypeMetaDefinition());
                DeviceMgtAPIUtils.getDeviceManagementService().registerDeviceType(httpDeviceTypeManagerService);
                return Response.status(Response.Status.OK).build();
            } catch (DeviceManagementException e) {
                String msg = "Error occurred at server side while fetching device list.";
                log.error(msg, e);
                return Response.serverError().entity(msg).build();
            }
        } else {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

    @Override
    @PUT
    public Response updateDeviceType(DeviceType deviceType) {
        if (deviceType != null) {
            try {
                if (DeviceMgtAPIUtils.getDeviceManagementService().getDeviceType(deviceType.getName()) == null) {
                    String msg = "Device type does not exist, " + deviceType.getName();
                    return Response.status(Response.Status.BAD_REQUEST).entity(msg).build();
                }
                HTTPDeviceTypeManagerService httpDeviceTypeManagerService = new HTTPDeviceTypeManagerService
                        (deviceType.getName(), deviceType.getDeviceTypeMetaDefinition());
                DeviceMgtAPIUtils.getDeviceManagementService().registerDeviceType(httpDeviceTypeManagerService);
                return Response.status(Response.Status.OK).build();
            } catch (DeviceManagementException e) {
                String msg = "Error occurred at server side while fetching device list.";
                log.error(msg, e);
                return Response.serverError().entity(msg).build();
            }
        } else {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }
}