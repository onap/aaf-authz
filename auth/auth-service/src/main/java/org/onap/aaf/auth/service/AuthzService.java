/**
 * ============LICENSE_START====================================================
 * org.onap.aaf
 * ===========================================================================
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 * ===========================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 *      http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END====================================================
 *
 */

package org.onap.aaf.auth.service;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.onap.aaf.auth.dao.DAOException;
import org.onap.aaf.auth.dao.cass.NsType;
import org.onap.aaf.auth.env.AuthzTrans;
import org.onap.aaf.auth.layer.Result;
import org.onap.aaf.auth.service.mapper.Mapper;

public interface AuthzService<NSS,PERMS,PERMKEY,ROLES,USERS,USERROLES,DELGS,CERTS,KEYS,REQUEST,HISTORY,ERR,APPROVALS> {
    public Mapper<NSS,PERMS,PERMKEY,ROLES,USERS,USERROLES,DELGS,CERTS,KEYS,REQUEST,HISTORY,ERR,APPROVALS> mapper();

/***********************************
 * NAMESPACE 
 ***********************************/
    /**
     * <p>
     * @param trans
     * @param user
     * @param ns
     * @return
     * @throws DAOException 
     * @throws  
     */
    public Result<Void> createNS(AuthzTrans trans, REQUEST request, NsType type);

    /**
     * <p>
     * @param trans
     * @param ns
     * @return
     */
    public Result<Void> addAdminNS(AuthzTrans trans, String ns, String id);

    /**
     * <p>
     * @param trans
     * @param ns
     * @return
     */
    public Result<Void> delAdminNS(AuthzTrans trans, String ns, String id);

    /**
     * <p>
     * @param trans
     * @param ns
     * @param id
     * @return
     */
    public Result<Void> addResponsibleNS(AuthzTrans trans, String ns, String id);

    /**
     * <p>
     * @param trans
     * @param ns
     * @param id
     * @return
     */
    public Result<Void> delResponsibleNS(AuthzTrans trans, String ns, String id);

    /**
     * <p>
     * @param trans
     * @param ns
     * @param key
     * @param value
     * @return
     */
    public Result<Void> createNsAttrib(AuthzTrans trans, String ns, String key, String value);

    /**
     * <p>
     * @param trans
     * @param ns
     * @param key
     * @param value
     * @return
     */
    public Result<?> updateNsAttrib(AuthzTrans trans, String ns, String key, String value);

    /**
     * <p>
     * @param trans
     * @param ns
     * @param key
     * @return
     */
    public Result<Void> deleteNsAttrib(AuthzTrans trans, String ns, String key);

    /**
     * <p>
     * @param trans
     * @param ns
     * @param key
     * @return
     */
    public Result<KEYS> readNsByAttrib(AuthzTrans trans, String key);


    /**
     * <p>
     * @param trans
     * @param ns
     * @return
     */
    public Result<NSS> getNSbyName(AuthzTrans trans, String ns, boolean full);

    /**
     * <p>
     * @param trans
     * @param user
     * @return
     */
    public Result<NSS> getNSbyAdmin(AuthzTrans trans, String user, boolean full);

    /**
     * <p>
     * @param trans
     * @param user
     * @return
     */
    public Result<NSS> getNSbyResponsible(AuthzTrans trans, String user, boolean full);

    /**
     * <p>
     * @param trans
     * @param user
     * @return
     */
    public Result<NSS> getNSbyEither(AuthzTrans trans, String user, boolean full);

    /**
     * <p>
     * @param trans
     * @param parent
     * @return
     */
    public Result<NSS> getNSsChildren(AuthzTrans trans, String parent);

    /**
     * <p>
     * @param trans
     * @param req
     * @return
     */
    public Result<Void> updateNsDescription(AuthzTrans trans, REQUEST req);

    /**
     * <p>
     * @param trans
     * @param ns
     * @param user
     * @return
     * @throws DAOException
     */
    public Result<Void> deleteNS(AuthzTrans trans, String ns);

/***********************************
 * PERM 
 ***********************************/
    /**
     * <p>
     * @param trans
     * @param rreq
     * @return
     * @throws DAOException 
     * @throws MappingException
     */
    public Result<Void> createPerm(AuthzTrans trans, REQUEST rreq);

    /**
     * <p>
     * @param trans
     * @param childPerm
     * @return
     * @throws DAOException 
     */
    public Result<PERMS> getPermsByType(AuthzTrans trans, String perm);

    /**
     * <p>
     * @param trans
     * @param type
     * @param instance
     * @param action
     * @return
     */
    public Result<PERMS> getPermsByName(AuthzTrans trans, String type,
            String instance, String action);

    /**
     * Gets all the permissions for a user across all the roles it is assigned to
     * @param userName
     * @return
     * @throws Exception 
     * @throws Exception
     */
    public Result<PERMS> getPermsByUser(AuthzTrans trans, String userName);

    /**
     * Gets all the permissions for a user across all the roles it is assigned to, filtered by NS (Scope)
     * <p>
     * @param trans
     * @param user
     * @param scopes
     * @return
     */
    public Result<PERMS> getPermsByUserScope(AuthzTrans trans, String user, String[] scopes);


    /**
     * Gets all the permissions for a user across all the roles it is assigned to
     * <p>
     * Add AAF Perms representing the "MayUser" calls if
     *     1) Allowed
     *  2) User has equivalent permission
     * <p>
     * @param userName
     * @return
     * @throws Exception 
     * @throws Exception
     */
    public Result<PERMS> getPermsByUser(AuthzTrans trans, PERMS perms, String userName);

    /**
     * <p>
     * Gets all the permissions for a user across all the roles it is assigned to
     * <p>
     * @param roleName
     * @return
     * @throws Exception
     */
    public Result<PERMS> getPermsByRole(AuthzTrans trans, String roleName);

    /**
     * <p>
     * @param trans
     * @param ns
     * @return
     */
    public Result<PERMS> getPermsByNS(AuthzTrans trans, String ns);

    /**
     * rename permission
     * <p>
     * @param trans
     * @param rreq
     * @param isRename
     * @param origType
     * @param origInstance
     * @param origAction
     * @return
     */
    public Result<Void> renamePerm(AuthzTrans trans, REQUEST rreq, String origType, String origInstance, String origAction);

    /**
     * <p>
     * @param trans
     * @param req
     * @return
     */
    public Result<Void> updatePermDescription(AuthzTrans trans, REQUEST req);

    /**
     * <p>
     * @param trans
     * @param from
     * @return
     */
    public Result<Void> resetPermRoles(AuthzTrans trans, REQUEST from);

    /**
     * <p>
     * @param trans
     * @param from
     * @return
     * @throws Exception
     */
    public Result<Void> deletePerm(AuthzTrans trans, REQUEST from);

    /**
     * <p>
     * @param trans
     * @param user
     * @param perm
     * @param type
     * @param action
     * @return
     * @throws Exception
     */
    Result<Void> deletePerm(AuthzTrans trans, String perm, String type, String action);

/***********************************
 * ROLE 
 ***********************************/
    /**
     * <p>
     * @param trans
     * @param user
     * @param role
     * @param approvers
     * @return
     * @throws DAOException 
     * @throws Exception
     */
    public Result<Void> createRole(AuthzTrans trans, REQUEST req);

    /**
     * <p>
     * @param trans
     * @param role
     * @return
     */
    public Result<ROLES> getRolesByName(AuthzTrans trans, String role);

    /**
     * <p>
     * @param trans
     * @param user
     * @return
     * @throws DAOException 
     */
    public Result<ROLES> getRolesByUser(AuthzTrans trans, String user);

    /**
     * <p>
     * @param trans
     * @param user
     * @return
     */
    public Result<ROLES> getRolesByNS(AuthzTrans trans, String user);

    /**
     * <p>
     * @param trans
     * @param name
     * @return
     */
    public Result<ROLES> getRolesByNameOnly(AuthzTrans trans, String name);

    /**
     * <p>
     * @param trans
     * @param type
     * @param instance
     * @param action
     * @return
     */
    public Result<ROLES> getRolesByPerm(AuthzTrans trans, String type, String instance, String action);

    /**
     * <p>
     * @param trans
     * @param req
     * @return
     */
    public Result<Void> updateRoleDescription(AuthzTrans trans, REQUEST req);

    /**
     * <p>
     * @param trans
     * @param rreq
     * @return
     * @throws DAOException
     */
    public Result<Void> addPermToRole(AuthzTrans trans, REQUEST rreq);


    /**
     * <p>
     * @param trans
     * @param rreq
     * @return
     * @throws DAOException
     */
    Result<Void> delPermFromRole(AuthzTrans trans, REQUEST rreq);

    /**
     *  Itemized key delete
     * @param trans
     * @param role
     * @param type
     * @param instance
     * @param action
     * @return
     */
    public Result<Void> delPermFromRole(AuthzTrans trans, String role, String type, String instance, String action);

    /**
     * <p>
     * @param trans
     * @param user
     * @param role
     * @return
     * @throws DAOException 
     * @throws MappingException 
     */
    public Result<Void> deleteRole(AuthzTrans trans, String role);

    /**
     * <p>
     * @param trans
     * @param req
     * @return
     */
    public Result<Void> deleteRole(AuthzTrans trans, REQUEST req);

/***********************************
 * CRED 
 ***********************************/

    /**
     * <p>
     * @param trans
     * @param from
     * @return
     */
    Result<Void> createUserCred(AuthzTrans trans, REQUEST from);

    /**
     * <p>
     * @param trans
     * @param from
     * @return
     */
    Result<Void> resetUserCred(AuthzTrans trans, REQUEST from);

    /**
     * <p>
     * @param trans
     * @param from
     * @param days
     * @return
     */
    Result<Void> extendUserCred(AuthzTrans trans, REQUEST from, String days);

    /**
     * <p>
     * @param trans
     * @param ns
     * @return
     */
    public Result<USERS> getCredsByNS(AuthzTrans trans, String ns);

    /**
     * <p>
     * @param trans
     * @param id
     * @return
     */
    public Result<USERS> getCredsByID(AuthzTrans trans, String id);

    /**
     * <p>
     * @param trans
     * @param req
     * @param id
     * @return
     */
    public Result<CERTS> getCertInfoByID(AuthzTrans trans, HttpServletRequest req, String id);

    /**
     * <p>
     * @param trans
     * @param credReq
     * @return
     */
    public Result<Void> deleteUserCred(AuthzTrans trans, REQUEST credReq);

    /**
     * <p>
     * @param trans
     * @param user
     * @return
     * @throws Exception
     */
    public Result<Date> doesCredentialMatch(AuthzTrans trans, REQUEST credReq);

    /**
     * <p>
     * @param trans
     * @param basicAuth
     * @return
     */
    public Result<Date> validateBasicAuth(AuthzTrans trans, String basicAuth);

    /**
     * <p>
     * @param trans
     * @param role
     * @return
     */
    public Result<USERS> getUsersByRole(AuthzTrans trans, String role);

    /**
     * <p>
     * @param trans
     * @param role
     * @return
     */
    public Result<USERS> getUserInRole(AuthzTrans trans, String user, String role);

    /**
     * <p>
     * @param trans
     * @param type
     * @param instance
     * @param action
     * @return
     */
    public Result<USERS> getUsersByPermission(AuthzTrans trans,String type, String instance, String action);




/***********************************
 * USER-ROLE 
 ***********************************/
    /**
     * <p>
     * @param trans
     * @param user
     * @param request
     * @return
     * @throws Exception
     */
    public Result<Void> createUserRole(AuthzTrans trans, REQUEST request);

    /**
     * <p>
     * @param trans
     * @param role
     * @return
     */
    public Result<USERROLES> getUserRolesByRole(AuthzTrans trans, String role);

    /**
     * <p>
     * @param trans
     * @param role
     * @return
     */
    public Result<USERROLES> getUserRolesByUser(AuthzTrans trans, String user);

    /*
     * Note: Removed "resetRolesForUsers" because it was too dangerous, and
     *       removed "resetUsersForRoles" because it was being misused.
     */

    /**
     * <p>
     * @param trans
     * @param user
     * @param role
     * @return
     */
    public Result<Void> extendUserRole(AuthzTrans trans, String user,
    String role);

    /**
     * <p>
     * @param trans
     * @param user
     * @param usr
     * @param role
     * @return
     * @throws DAOException 
     */
    public Result<Void> deleteUserRole(AuthzTrans trans, String usr, String role);



/***********************************
 * HISTORY 
 ***********************************/
    /**
     * <p>
     * @param trans
     * @param user
     * @param yyyymm
     * @return
     */
    public Result<HISTORY> getHistoryByUser(AuthzTrans trans, String user, int[] yyyymm, int sort);

    /**
     * <p>
     * @param trans
     * @param subj
     * @param yyyymm
     * @param sort
     * @return
     */
    public Result<HISTORY> getHistoryByRole(AuthzTrans trans, String subj, int[] yyyymm, int sort);

    /**
     * <p>
     * @param trans
     * @param subj
     * @param yyyymm
     * @param sort
     * @return
     */
    public Result<HISTORY> getHistoryByPerm(AuthzTrans trans, String subj, int[] yyyymm, int sort);

    /**
     * <p>
     * @param trans
     * @param subj
     * @param yyyymm
     * @param sort
     * @return
     */
    public Result<HISTORY> getHistoryByNS(AuthzTrans trans, String subj, int[] yyyymm, int sort);

    /**
     * <p>
     * @param trans
     * @param target
     * @param yyyymm
     * @param sort
     * @return
     */
    public Result<HISTORY> getHistoryBySubject(AuthzTrans trans, String subject, String target, int[] yyyymm, int sort);

/***********************************
 * DELEGATE 
 ***********************************/
    /**
     * <p>
     * @param trans
     * @param delegates
     * @return
     * @throws Exception
     */
    public Result<Void> createDelegate(AuthzTrans trans, REQUEST reqDelegate);

    /**
     * <p>
     * @param trans
     * @param delegates
     * @return
     * @throws Exception
     */
    public Result<Void> updateDelegate(AuthzTrans trans, REQUEST reqDelegate);

    /**
     * <p>
     * @param trans
     * @param userName
     * @param delegate
     * @return
     * @throws Exception
     */
    public Result<Void> deleteDelegate(AuthzTrans trans, REQUEST reqDelegate);

    /**
     * <p>
     * @param trans
     * @param userName
     * @return
     */
    public Result<Void> deleteDelegate(AuthzTrans trans, String userName);

    /**
     * <p>
     * @param trans
     * @param user
     * @return
     * @throws Exception
     */
    public Result<DELGS> getDelegatesByUser(AuthzTrans trans, String user);


    /**
     * <p>
     * @param trans
     * @param delegate
     * @return
     */
    public Result<DELGS> getDelegatesByDelegate(AuthzTrans trans, String delegate);

/***********************************
 * APPROVAL 
 ***********************************/
    /**
     * <p>
     * @param trans
     * @param user
     * @param approver
     * @param status
     * @return
     */
    public Result<Void> updateApproval(AuthzTrans trans, APPROVALS approvals);

    /**
     * <p>
     * @param trans
     * @param user
     * @return
     */
    public Result<APPROVALS> getApprovalsByUser(AuthzTrans trans, String user);

    /**
     * <p>
     * @param trans
     * @param ticket
     * @return
     */
    public Result<APPROVALS> getApprovalsByTicket(AuthzTrans trans, String ticket);

    /**
     * <p>
     * @param trans
     * @param approver
     * @return
     */
    public Result<APPROVALS> getApprovalsByApprover(AuthzTrans trans, String approver);

    /**
     * <p>
     * @param trans
     * @param cname
     * @return
     */
    public Result<Void> cacheClear(AuthzTrans trans, String cname);

    /**
     * <p>
     * @param trans
     * @param cname
     * @param segment
     * @return
     */
    public Result<Void> cacheClear(AuthzTrans trans, String cname, int[] segment);

    /**
     * <p>
     * @param trans
     */
    public void dbReset(AuthzTrans trans);

}
