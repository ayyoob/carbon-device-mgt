/*
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.device.mgt.core.dto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

@XmlRootElement public class DeviceType implements Serializable {

    private static final long serialVersionUID = 7927802716452548282L;
    private int id;
    private String name;

    public DeviceType() {
    }

    public DeviceType(String name) {
        this.name = name;
    }

    @XmlElement public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @XmlElement public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
