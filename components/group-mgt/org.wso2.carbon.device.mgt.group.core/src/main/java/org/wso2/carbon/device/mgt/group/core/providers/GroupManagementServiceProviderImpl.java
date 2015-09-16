/*
 *  Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.wso2.carbon.device.mgt.group.core.providers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.device.mgt.common.Device;
import org.wso2.carbon.device.mgt.common.DeviceIdentifier;
import org.wso2.carbon.device.mgt.common.DeviceManagementException;
import org.wso2.carbon.device.mgt.common.TransactionManagementException;
import org.wso2.carbon.device.mgt.core.util.DeviceManagerUtil;
import org.wso2.carbon.device.mgt.group.common.DeviceGroup;
import org.wso2.carbon.device.mgt.group.common.GroupManagementException;
import org.wso2.carbon.device.mgt.group.common.GroupUser;
import org.wso2.carbon.device.mgt.group.core.dao.GroupDAO;
import org.wso2.carbon.device.mgt.group.core.dao.GroupManagementDAOException;
import org.wso2.carbon.device.mgt.group.core.dao.GroupManagementDAOFactory;
import org.wso2.carbon.device.mgt.group.core.internal.DeviceGroupBroker;
import org.wso2.carbon.device.mgt.group.core.internal.GroupManagementDataHolder;
import org.wso2.carbon.user.api.Permission;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.api.UserStoreManager;
import org.wso2.carbon.user.core.util.UserCoreUtil;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupManagementServiceProviderImpl implements GroupManagementServiceProvider {

    private static Log log = LogFactory.getLog(GroupManagementServiceProviderImpl.class);

    private GroupDAO groupDAO;

    public GroupManagementServiceProviderImpl() {
        this.groupDAO = GroupManagementDAOFactory.getGroupDAO();
    }

    @Override public int createGroup(DeviceGroup deviceGroup, String defaultRole, String[] defaultPermissions)
            throws GroupManagementException {
        if (deviceGroup == null) {
            throw new GroupManagementException("DeviceGroup cannot be null", new NullPointerException());
        }
        DeviceGroupBroker groupBroker = new DeviceGroupBroker(deviceGroup);
        int tenantId = DeviceManagerUtil.getTenantId();
        int groupId = -1;
        try {
            GroupManagementDAOFactory.beginTransaction();
            groupId = this.groupDAO.addGroup(groupBroker, tenantId);
            GroupManagementDAOFactory.commitTransaction();
        } catch (GroupManagementDAOException e) {
            GroupManagementDAOFactory.rollbackTransaction();
            throw new GroupManagementException("Error occurred while adding deviceGroup " +
                    "'" + deviceGroup.getName() + "' to database", e);
        } catch (TransactionManagementException e) {
            throw new GroupManagementException("Error occurred while initiating transaction", e);
        } finally {
            GroupManagementDAOFactory.closeConnection();
        }
        if (groupId == -1) {
            return -1;
        }
        groupBroker.setId(groupId);
        addSharing(groupBroker.getOwner(), groupBroker.getId(), defaultRole, defaultPermissions);
        if (log.isDebugEnabled()) {
            log.debug("DeviceGroup added: " + groupBroker.getName());
        }
        return groupId;
    }

    @Override public boolean updateGroup(DeviceGroup deviceGroup) throws GroupManagementException {
        if (deviceGroup == null) {
            throw new GroupManagementException("DeviceGroup cannot be null", new NullPointerException());
        }
        try {
            GroupManagementDAOFactory.beginTransaction();
            int sqlReturn = this.groupDAO.updateGroup(deviceGroup);
            GroupManagementDAOFactory.commitTransaction();
            return (sqlReturn != -1);
        } catch (GroupManagementDAOException e) {
            GroupManagementDAOFactory.rollbackTransaction();
            throw new GroupManagementException("Error occurred while modifying deviceGroup " +
                    "'" + deviceGroup.getName() + "'", e);
        } catch (TransactionManagementException e) {
            throw new GroupManagementException("Error occurred while initiating transaction", e);
        } finally {
            GroupManagementDAOFactory.closeConnection();
        }
    }

    @Override public boolean deleteGroup(int groupId) throws GroupManagementException {
        String roleName;
        DeviceGroup deviceGroup = getGroup(groupId);
        if (deviceGroup == null) {
            return false;
        }
        List<String> groupRoles = getRoles(groupId);
        for (String role : groupRoles) {
            if (role != null) {
                roleName = role.replace("Internal/group-" + groupId + "-", "");
                removeSharing(groupId, roleName);
            }
        }
        List<Device> groupDevices = getDevices(groupId);
        try {
            for (Device device : groupDevices) {
                device.setGroupId(0);
                GroupManagementDataHolder.getInstance().getDeviceManagementService().modifyEnrollment(device);
            }
        } catch (DeviceManagementException e) {
            throw new GroupManagementException("Error occurred while removing device from group", e);
        }
        try {
            GroupManagementDAOFactory.beginTransaction();
            int sqlReturn = this.groupDAO.deleteGroup(groupId);
            GroupManagementDAOFactory.commitTransaction();
            if (log.isDebugEnabled()) {
                log.debug("DeviceGroup " + deviceGroup.getName() + " removed: " + (sqlReturn != -1));
            }
            return (sqlReturn != -1);
        } catch (GroupManagementDAOException e) {
            GroupManagementDAOFactory.rollbackTransaction();
            throw new GroupManagementException("Error occurred while removing group " +
                    "'" + groupId + "' data", e);
        } catch (TransactionManagementException e) {
            throw new GroupManagementException("Error occurred while initiating transaction", e);
        } finally {
            GroupManagementDAOFactory.closeConnection();
        }
    }

    @Override public DeviceGroup getGroup(int groupId) throws GroupManagementException {
        DeviceGroup deviceGroup;
        try {
            GroupManagementDAOFactory.openConnection();
            deviceGroup = this.groupDAO.getGroup(groupId);
        } catch (GroupManagementDAOException e) {
            throw new GroupManagementException("Error occurred while obtaining group " + groupId, e);
        } catch (SQLException e) {
            throw new GroupManagementException("Error occurred while opening a connection to the data source", e);
        } finally {
            GroupManagementDAOFactory.closeConnection();
        }
        if (deviceGroup != null) {
            DeviceGroupBroker groupBroker = new DeviceGroupBroker(deviceGroup);
            groupBroker.setDevices(this.getDevices(groupId));
            groupBroker.setUsers(this.getUsers(groupId));
            groupBroker.setRoles(this.getRoles(groupId));
            return groupBroker.getGroup();
        } else {
            return null;
        }
    }

    @Override public List<DeviceGroup> findGroups(String groupName, String owner) throws GroupManagementException {
        List<DeviceGroup> deviceGroups = new ArrayList<>();
        try {
            int tenantId = DeviceManagerUtil.getTenantId();
            GroupManagementDAOFactory.openConnection();
            deviceGroups = this.groupDAO.getGroups(groupName, tenantId);
        } catch (GroupManagementDAOException e) {
            throw new GroupManagementException("Error occurred while finding group " + groupName, e);
        } catch (SQLException e) {
            throw new GroupManagementException("Error occurred while opening a connection to the data source", e);
        } finally {
            GroupManagementDAOFactory.closeConnection();
        }
        List<DeviceGroup> groupsWithData = new ArrayList<>();
        for (DeviceGroup deviceGroup : deviceGroups) {
            DeviceGroupBroker groupBroker = new DeviceGroupBroker(deviceGroup);
            groupBroker.setDevices(this.getDevices(deviceGroup.getId()));
            groupBroker.setUsers(this.getUsers(deviceGroup.getId()));
            groupBroker.setRoles(this.getRoles(deviceGroup.getId()));
            groupsWithData.add(groupBroker.getGroup());
        }
        return groupsWithData;
    }

    @Override public List<DeviceGroup> getGroups(String username) throws GroupManagementException {
        UserStoreManager userStoreManager;
        try {
            int tenantId = DeviceManagerUtil.getTenantId();
            userStoreManager = GroupManagementDataHolder.getInstance().getRealmService().getTenantUserRealm(tenantId)
                    .getUserStoreManager();
            String[] roleList = userStoreManager.getRoleListOfUser(username);
            Map<Integer, DeviceGroup> groups = new HashMap<>();
            for (String role : roleList) {
                if (role != null && role.contains("Internal/group-")) {
                    int groupId = Integer.parseInt(role.split("-")[1]);
                    if (!groups.containsKey(groupId)) {
                        DeviceGroup deviceGroup = getGroup(groupId);
                        groups.put(groupId, deviceGroup);
                    }
                }
            }
            return new ArrayList<>(groups.values());
        } catch (UserStoreException e) {
            throw new GroupManagementException("Error occurred while getting user store manager", e);
        }
    }

    @Override public int getGroupCount(String username) throws GroupManagementException {
        return this.getGroups(username).size();
    }

    @Override public boolean shareGroup(String username, int groupId, String sharingRole)
            throws GroupManagementException {
        UserStoreManager userStoreManager;
        String[] roles = new String[1];
        try {
            DeviceGroup deviceGroup = getGroup(groupId);
            if (deviceGroup == null) {
                return false;
            }
            int tenantId = DeviceManagerUtil.getTenantId();
            userStoreManager = GroupManagementDataHolder.getInstance().getRealmService().getTenantUserRealm(tenantId)
                    .getUserStoreManager();
            roles[0] = "Internal/group-" + groupId + "-" + sharingRole;
            userStoreManager.updateRoleListOfUser(username, null, roles);
            return true;
        } catch (UserStoreException userStoreEx) {
            String errorMsg = "User store error in adding user " + username + " to group id:" + groupId;
            log.error(errorMsg, userStoreEx);
            throw new GroupManagementException(errorMsg, userStoreEx);
        }
    }

    @Override public boolean unShareGroup(String username, int groupId, String sharingRole)
            throws GroupManagementException {
        UserStoreManager userStoreManager;
        String[] roles = new String[1];
        try {
            DeviceGroup deviceGroup = getGroup(groupId);
            if (deviceGroup == null) {
                return false;
            }
            int tenantId = DeviceManagerUtil.getTenantId();
            userStoreManager = GroupManagementDataHolder.getInstance().getRealmService().getTenantUserRealm(tenantId)
                    .getUserStoreManager();
            roles[0] = "Internal/group-" + groupId + "-" + sharingRole;
            userStoreManager.updateRoleListOfUser(username, roles, null);
            return true;
        } catch (UserStoreException userStoreEx) {
            String errorMsg = "User store error in adding user " + username + " to group id:" + groupId;
            log.error(errorMsg, userStoreEx);
            throw new GroupManagementException(errorMsg, userStoreEx);
        }
    }

    @Override public boolean addSharing(String username, int groupId, String roleName, String[] permissions)
            throws GroupManagementException {
        UserStoreManager userStoreManager;
        String role;
        String[] userNames = new String[1];
        try {
            DeviceGroup deviceGroup = getGroup(groupId);
            if (deviceGroup == null) {
                return false;
            }
            int tenantId = DeviceManagerUtil.getTenantId();
            userStoreManager = GroupManagementDataHolder.getInstance().getRealmService().getTenantUserRealm(tenantId)
                    .getUserStoreManager();
            role = "Internal/group-" + groupId + "-" + roleName;
            userNames[0] = username;
            Permission[] carbonPermissions = new Permission[permissions.length];
            for (int i = 0; i < permissions.length; i++) {
                carbonPermissions[i] = new Permission(permissions[i], CarbonConstants.UI_PERMISSION_ACTION);
            }
            userStoreManager.addRole(role, userNames, carbonPermissions);
            return true;
        } catch (UserStoreException userStoreEx) {
            String errorMsg = "User store error in adding role to group id:" + groupId;
            log.error(errorMsg, userStoreEx);
            throw new GroupManagementException(errorMsg, userStoreEx);
        }
    }

    @Override public boolean removeSharing(int groupId, String roleName) throws GroupManagementException {
        UserStoreManager userStoreManager;
        String role;
        try {
            DeviceGroup deviceGroup = getGroup(groupId);
            if (deviceGroup == null) {
                return false;
            }
            int tenantId = DeviceManagerUtil.getTenantId();
            userStoreManager = GroupManagementDataHolder.getInstance().getRealmService().getTenantUserRealm(tenantId)
                    .getUserStoreManager();
            role = "Internal/group-" + groupId + "-" + roleName;
            userStoreManager.deleteRole(role);
            return true;
        } catch (UserStoreException userStoreEx) {
            String errorMsg = "User store error in adding role to group id:" + groupId;
            log.error(errorMsg, userStoreEx);
            throw new GroupManagementException(errorMsg, userStoreEx);
        }
    }

    @Override public List<String> getRoles(int groupId) throws GroupManagementException {
        UserStoreManager userStoreManager;
        String[] roles;
        List<String> groupRoles;
        try {
            int tenantId = DeviceManagerUtil.getTenantId();
            userStoreManager = GroupManagementDataHolder.getInstance().getRealmService().getTenantUserRealm(tenantId)
                    .getUserStoreManager();
            roles = userStoreManager.getRoleNames();
            groupRoles = new ArrayList<>();
            for (String r : roles) {
                if (r != null && r.contains("Internal/group-" + groupId + "-")) {
                    groupRoles.add(r.replace("Internal/group-" + groupId + "-", ""));
                }
            }
            return groupRoles;
        } catch (UserStoreException userStoreEx) {
            String errorMsg = "User store error in adding role to group id:" + groupId;
            log.error(errorMsg, userStoreEx);
            throw new GroupManagementException(errorMsg, userStoreEx);
        }
    }

    @Override public List<String> getRoles(String username, int groupId) throws GroupManagementException {
        UserStoreManager userStoreManager;
        List<String> groupRoleList = new ArrayList<>();
        try {
            int tenantId = DeviceManagerUtil.getTenantId();
            userStoreManager = GroupManagementDataHolder.getInstance().getRealmService().getTenantUserRealm(tenantId)
                    .getUserStoreManager();
            String[] roleList = userStoreManager.getRoleListOfUser(username);
            for (String role : roleList) {
                if (role != null && role.contains("Internal/group-" + groupId)) {
                    String roleName = role.replace("Internal/group-" + groupId + "-", "");
                    groupRoleList.add(roleName);
                }
            }
            return groupRoleList;
        } catch (UserStoreException e) {
            throw new GroupManagementException("Error occurred while getting user store manager", e);
        }
    }

    @Override public List<GroupUser> getUsers(int groupId) throws GroupManagementException {
        UserStoreManager userStoreManager;
        Map<String, GroupUser> groupUserHashMap = new HashMap<>();
        try {
            int tenantId = DeviceManagerUtil.getTenantId();
            userStoreManager = GroupManagementDataHolder.getInstance().getRealmService().getTenantUserRealm(tenantId)
                    .getUserStoreManager();
            List<String> rolesForGroup = this.getRoles(groupId);
            for (String role : rolesForGroup) {
                String[] users = userStoreManager.getUserListOfRole("Internal/group-" + groupId + "-" + role);
                for (String user : users) {
                    GroupUser groupUser;
                    if (groupUserHashMap.containsKey(user)) {
                        groupUser = groupUserHashMap.get(user);
                        groupUser.getRoles().add(role);
                    } else {
                        groupUser = new GroupUser();
                        groupUser.setUsername(user);
                        groupUser.setRoles(new ArrayList<String>());
                        groupUser.getRoles().add(role);
                        groupUserHashMap.put(user, groupUser);
                    }
                }
            }
            return new ArrayList<>(groupUserHashMap.values());
        } catch (UserStoreException userStoreEx) {
            String errorMsg = "User store error in fetching user list for group id:" + groupId;
            log.error(errorMsg, userStoreEx);
            throw new GroupManagementException(errorMsg, userStoreEx);
        }
    }

    @Override public List<Device> getDevices(int groupId) throws GroupManagementException {
        List<Device> devicesInGroup;
        try {
            devicesInGroup = GroupManagementDataHolder.getInstance().getDeviceManagementService()
                    .getDevices(groupId);
            return devicesInGroup;
        } catch (DeviceManagementException e) {
            throw new GroupManagementException("Error occurred while getting devices in group", e);
        }
    }

    @Override public boolean addDevice(DeviceIdentifier deviceId, int groupId) throws GroupManagementException {
        Device device;
        DeviceGroup deviceGroup;
        try {
            device = GroupManagementDataHolder.getInstance().getDeviceManagementService().getDevice(deviceId);
            deviceGroup = this.getGroup(groupId);
            if (device == null || deviceGroup == null) {
                return false;
            }
            device.setGroupId(deviceGroup.getId());
            GroupManagementDataHolder.getInstance().getDeviceManagementService().modifyEnrollment(device);
        } catch (DeviceManagementException e) {
            throw new GroupManagementException("Error occurred while adding device in to deviceGroup", e);
        }
        return true;
    }

    @Override public boolean removeDevice(DeviceIdentifier deviceId, int groupId) throws GroupManagementException {
        Device device;
        DeviceGroup deviceGroup;
        try {
            device = GroupManagementDataHolder.getInstance().getDeviceManagementService().getDevice(deviceId);
            deviceGroup = this.getGroup(groupId);
            if (device == null || deviceGroup == null) {
                return false;
            }
            device.setGroupId(0);
            GroupManagementDataHolder.getInstance().getDeviceManagementService().modifyEnrollment(device);
        } catch (DeviceManagementException e) {
            throw new GroupManagementException("Error occurred while removing device from deviceGroup", e);
        }
        return true;
    }

    @Override public String[] getPermissions(String username, int groupId) throws GroupManagementException {
        UserRealm userRealm;
        List<String> roles = getRoles(username, groupId);
        int tenantId = DeviceManagerUtil.getTenantId();
        try {
            userRealm = GroupManagementDataHolder.getInstance().getRealmService().getTenantUserRealm(tenantId);
            List<String> lstPermissions = new ArrayList<>();
            String[] resourceIds = userRealm.getAuthorizationManager().getAllowedUIResourcesForUser(username, "/");
            if (resourceIds != null) {
                for (String resourceId : resourceIds) {
                    for (String roleName : roles) {
                        if (userRealm.getAuthorizationManager().
                                isRoleAuthorized("Internal/group-" + groupId + "-" + roleName, resourceId,
                                        CarbonConstants.UI_PERMISSION_ACTION)) {
                            lstPermissions.add(resourceId);
                        }
                    }
                }
            }
            String[] permissions = lstPermissions.toArray(new String[lstPermissions.size()]);
            return UserCoreUtil.optimizePermissions(permissions);
        } catch (UserStoreException e) {
            throw new GroupManagementException("Error occurred while getting user realm", e);
        }
    }

    @Override public List<DeviceGroup> getGroups(String username, String permission) throws GroupManagementException {
        UserRealm userRealm;
        int tenantId = DeviceManagerUtil.getTenantId();
        Map<Integer, DeviceGroup> groups = new HashMap<>();
        try {
            userRealm = GroupManagementDataHolder.getInstance().getRealmService().getTenantUserRealm(tenantId);
            String[] roles = userRealm.getUserStoreManager().getRoleListOfUser(username);
            for (String role : roles) {
                if (role != null && role.contains("Internal/group-") && userRealm.getAuthorizationManager()
                        .isRoleAuthorized(role, permission, CarbonConstants.UI_PERMISSION_ACTION)) {
                    int groupId = Integer.parseInt(role.split("-")[1]);
                    if (!groups.containsKey(groupId)) {
                        DeviceGroup deviceGroup = getGroup(groupId);
                        groups.put(groupId, deviceGroup);
                    }
                }
            }
            return new ArrayList<>(groups.values());
        } catch (UserStoreException e) {
            throw new GroupManagementException("Error occurred while getting user realm", e);
        }
    }

    @Override public boolean isAuthorized(String username, int groupId, String permission)
            throws GroupManagementException {
        UserRealm userRealm;
        int tenantId = DeviceManagerUtil.getTenantId();
        try {
            userRealm = GroupManagementDataHolder.getInstance().getRealmService().getTenantUserRealm(tenantId);
            List<String> roles = this.getRoles(username, groupId);
            for (String role : roles) {
                if (userRealm.getAuthorizationManager()
                        .isRoleAuthorized("Internal/group-" + groupId + "-" + role, permission,
                                CarbonConstants.UI_PERMISSION_ACTION)) {
                    return true;
                }
            }
            return false;
        } catch (UserStoreException e) {
            throw new GroupManagementException("Error occurred while getting user realm", e);
        }
    }
}
