/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.device.mgt.jaxrs.api;

import io.swagger.annotations.*;
import org.wso2.carbon.apimgt.annotations.api.*;
import org.wso2.carbon.device.mgt.common.device.details.DeviceWrapper;
import org.wso2.carbon.device.mgt.common.search.SearchContext;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Device search related operations such as getting device information.
 */
@API(name = "Device Search", version = "1.0.0", context = "/devicemgt_admin/search", tags = {"devicemgt_admin"})

// Below Api is for swagger annotations
@Path("/search")
@Api(value = "DeviceSearch", description = "Device searching related operations can be found here.")
@SuppressWarnings("NonJaxWsWebServices")
public interface DeviceSearch {

    @GET
    @ApiOperation(
            consumes = MediaType.APPLICATION_JSON,
            httpMethod = "GET",
            value = "Advanced Search for Devices via the Console",
            notes = "Carry out an advanced search via the EMM console",
            response = DeviceWrapper.class,
            responseContainer = "List")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = DeviceWrapper.class, responseContainer = "List"),
            @ApiResponse(code = 500, message = "Error occurred while searching the device information")
            })
    @Permission(scope = "device-search", permissions = {"/permission/admin/device-mgt/admin/devices/list"})
    Response getFilteredDeviceInfo(@ApiParam(name = "enrollmentCertificates", value = "List of search conditions",
                                    required = true) SearchContext searchContext);
}
