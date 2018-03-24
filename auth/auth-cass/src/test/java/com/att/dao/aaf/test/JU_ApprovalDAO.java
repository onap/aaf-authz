/**
 * ============LICENSE_START====================================================
 * org.onap.aaf
 * ===========================================================================
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 * ===========================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END====================================================
 *
 */

package com.att.dao.aaf.test;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.junit.Test;
import org.onap.aaf.auth.dao.cass.ApprovalDAO;
import org.onap.aaf.auth.dao.cass.ApprovalDAO.Data;
import org.onap.aaf.auth.layer.Result;

public class JU_ApprovalDAO  extends AbsJUCass {
	@Test
	public void testCRUD() throws Exception {
		ApprovalDAO rrDAO = new ApprovalDAO(trans, cluster, AUTHZ);
		ApprovalDAO.Data data = new ApprovalDAO.Data();
		
		// Note, Create creates the time id
		data.ticket = UUID.randomUUID(); // normally, read from Future object
		data.user = "testid@test.com";
		data.approver = "mySuper@att.com";
		data.type = "supervisor";
		data.status = "pending";
		data.operation = "C";
		data.updated = new Date();
		data.memo = "Sing Hey for the break of day";
		data.last_notified = null;
		
		try {
			// Test create
			Result<Data> rc = rrDAO.create(trans, data);
			if(rc.isOKhasData()) { // Create creates the TIMEID.
				data = rc.value;
			}
			
			// Test Read by Ticket
			Result<List<ApprovalDAO.Data>> rlad;
			rlad = rrDAO.readByTicket(trans, data.ticket);
			assertTrue(rlad.isOK());
			assertEquals(1,rlad.value.size());
			compare(data,rlad.value.get(0));
			
			// Hold onto original ID for deletion, and read tests
			UUID id = rlad.value.get(0).id;
			
			try {
				// Test Read by User
				rlad = rrDAO.readByUser(trans, data.user);
				assertTrue(rlad.isOKhasData());
				boolean ok = false;
				for(ApprovalDAO.Data a : rlad.value) {
					if(a.id.equals(id)) {
						ok = true;
						compare(data,a);
					}
				}
				assertTrue(ok);
	
				// Test Read by Approver
				rlad = rrDAO.readByApprover(trans, data.approver);
				assertTrue(rlad.isOKhasData());
				ok = false;
				for(ApprovalDAO.Data a : rlad.value) {
					if(a.id.equals(id)) {
						ok = true;
						compare(data,a);
					}
				}
				assertTrue(ok);
	
				// Test Read by ID
				rlad = rrDAO.read(trans, id);
				assertTrue(rlad.isOKhasData());
				ok = false;
				for(ApprovalDAO.Data a : rlad.value) {
					if(a.id.equals(id)) {
						ok = true;
						compare(data,a);
					}
				}
				assertTrue(ok);
	
				// Test Update
				data.status = "approved";
				data.id = id;
				assertTrue(rrDAO.update(trans, data).isOK());
				
				rlad = rrDAO.read(trans, id);
				assertTrue(rlad.isOKhasData());
				ok = false;
				for(ApprovalDAO.Data a : rlad.value) {
					if(a.id.equals(id)) {
						ok = true;
						compare(data,a);
					}
				}
				assertTrue(ok);

			} finally {
				// Delete
				data.id = id;
				rrDAO.delete(trans, data, true);
				rlad = rrDAO.read(trans, id);
				assertTrue(rlad.isOK());
				assertTrue(rlad.isEmpty());
			}
			
		} finally {
			rrDAO.close(trans);
		}
	}

	private void compare(Data d1, Data d2) {
		assertEquals(d1.id.toString(),d2.id.toString());
		assertEquals(d1.ticket.toString(),d2.ticket.toString());
		assertEquals(d1.user,d2.user);
		assertEquals(d1.approver,d2.approver);
		assertEquals(d1.type,d2.type);
		assertEquals(d1.status,d2.status);
		assertEquals(d1.operation,d2.operation);
		//assertEquals(d1.updated,d2.updated);
		assertEquals(d1.memo,d2.memo);
		assertEquals(d1.last_notified,d2.last_notified);
	}

	
	
}
