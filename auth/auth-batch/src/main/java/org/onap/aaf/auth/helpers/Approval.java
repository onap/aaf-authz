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

package org.onap.aaf.auth.helpers;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;
import java.util.UUID;

import org.onap.aaf.auth.dao.cass.ApprovalDAO;
import org.onap.aaf.auth.env.AuthzTrans;
import org.onap.aaf.auth.layer.Result;
import org.onap.aaf.misc.env.Env;
import org.onap.aaf.misc.env.TimeTaken;
import org.onap.aaf.misc.env.Trans;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SimpleStatement;
import com.datastax.driver.core.Statement;

public class Approval implements CacheChange.Data  {
	public static final String RE_APPROVAL_IN_ROLE = "Re-Approval in Role '";
	public static final String RE_VALIDATE_ADMIN = "Re-Validate as Administrator for AAF Namespace '";
	public static final String RE_VALIDATE_OWNER = "Re-Validate Ownership for AAF Namespace '";

	public static TreeMap<String,List<Approval>> byApprover = new TreeMap<String,List<Approval>>();
	public static TreeMap<String,List<Approval>> byUser = new TreeMap<String,List<Approval>>();
	public static TreeMap<UUID,List<Approval>> byTicket = new TreeMap<UUID,List<Approval>>();
	private final static CacheChange<Approval> cache = new CacheChange<Approval>(); 
	
	public final ApprovalDAO.Data add;
	private String role;
	
	public Approval(UUID id, UUID ticket, String approver, Date last_notified, 
			String user, String memo, String operation, String status, String type, long updated) {
		add = new ApprovalDAO.Data();
		add.id = id;
		add.ticket = ticket;
		add.approver = approver;
		add.last_notified = last_notified;
		add.user = user;
		add.memo = memo;
		add.operation = operation;
		add.status = status;
		add.type = type;
		add.updated = new Date(updated);
		role = roleFromMemo(memo);
	}
	
	public static String roleFromMemo(String memo) {
		if(memo==null) {
			return null;
		}
		int first = memo.indexOf('\'');
		if(first>=0) {
			int second = memo.indexOf('\'', ++first);
			if(second>=0) {
				String role = memo.substring(first, second);
				if(memo.startsWith(RE_VALIDATE_ADMIN)) {
					return role + ".admin";
				} else if(memo.startsWith(RE_VALIDATE_OWNER)) {
					return role + ".owner";
				} else if(memo.startsWith(RE_APPROVAL_IN_ROLE)) {
					return role;
				}
			}
		}
		return null;
	}

	public static void load(Trans trans, Session session, Creator<Approval> creator ) {
		trans.info().log( "query: " + creator.select() );
        TimeTaken tt = trans.start("Load Notify", Env.REMOTE);
       
        ResultSet results;
		try {
	        Statement stmt = new SimpleStatement(creator.select());
	        results = session.execute(stmt);
        } finally {
        	tt.done();
        }
		int count = 0;
        tt = trans.start("Process Notify", Env.SUB);

        try {
	        	List<Approval> ln;
	        	for(Row row : results.all()) {
	        		++count;
			        try {
				        	Approval app = creator.create(row);
				        	String person = app.getApprover();
				        	if(person!=null) {
					        ln = byApprover.get(person);
					        	if(ln==null) {
					        		ln = new ArrayList<Approval>();
					        		byApprover.put(app.getApprover(), ln);
					        	}
					        	ln.add(app);
				        	}
				        	
				        	
				        person = app.getUser();
				        	if(person!=null) {
				        		ln = byUser.get(person);
					        	if(ln==null) {
					        		ln = new ArrayList<Approval>();
					        		byUser.put(app.getUser(), ln);
					        	}
					        	ln.add(app);
				        	}
				        	UUID ticket = app.getTicket();
				        	if(ticket!=null) {
					        	ln = byTicket.get(ticket);
					        	if(ln==null) {
					        		ln = new ArrayList<Approval>();
					        		byTicket.put(app.getTicket(), ln);
					        	}
					        ln.add(app);
				        	}
			        } finally {
			        	tt.done();
			        }
	        	}
        } finally {
        	tt.done();
        	trans.info().log("Found",count,"Approval Records");
        }
	}
	
	@Override
	public void expunge() {
		List<Approval> la = byApprover.get(getApprover());
		if(la!=null) {
			la.remove(this);
		}
		
		la = byUser.get(getUser());
		if(la!=null) {
			la.remove(this);
		}
		UUID ticket = this.add==null?null:this.add.ticket;
		if(ticket!=null) {
			la = byTicket.get(this.add.ticket);
			if(la!=null) {
				la.remove(this);
			}
		}
	}

	public void update(AuthzTrans trans, ApprovalDAO apprDAO, boolean dryRun) {
		if(dryRun) {
			trans.info().printf("Would update Approval %s, %s, last_notified %s",add.id,add.status,add.last_notified);
		} else {
			trans.info().printf("Update Approval %s, %s, last_notified %s",add.id,add.status,add.last_notified);
			apprDAO.update(trans, add);
		}
	}

	public static Creator<Approval> v2_0_17 = new Creator<Approval>() {
		@Override
		public Approval create(Row row) {
			return new Approval(row.getUUID(0), row.getUUID(1), row.getString(2), row.getTimestamp(3),
					row.getString(4),row.getString(5),row.getString(6),row.getString(7),row.getString(8)
					,row.getLong(9)/1000);
		}

		@Override
		public String select() {
			return "select id,ticket,approver,last_notified,user,memo,operation,status,type,WRITETIME(status) from authz.approval";
		}
	};

	/**
	 * @return the lastNotified
	 */
	public Date getLast_notified() {
		return add.last_notified;
	}
	/**
	 * @param lastNotified the lastNotified to set
	 */
	public void setLastNotified(Date last_notified) {
		add.last_notified = last_notified;
	}
	/**
	 * @return the status
	 */
	public String getStatus() {
		return add.status;
	}
	/**
	 * @param status the status to set
	 */
	public void setStatus(String status) {
		add.status = status;
	}
	/**
	 * @return the id
	 */
	public UUID getId() {
		return add.id;
	}
	/**
	 * @return the ticket
	 */
	public UUID getTicket() {
		return add.ticket;
	}
	/**
	 * @return the approver
	 */
	public String getApprover() {
		return add.approver;
	}
	/**
	 * @return the user
	 */
	public String getUser() {
		return add.user;
	}
	/**
	 * @return the memo
	 */
	public String getMemo() {
		return add.memo;
	}
	/**
	 * @return the operation
	 */
	public String getOperation() {
		return add.operation;
	}
	/**
	 * @return the type
	 */
	public String getType() {
		return add.type;
	}
	public void lapsed() {
		add.ticket=null;
		add.status="lapsed";
	}
	
	public String getRole() {
		return role;
	}
	
	public String toString() {
		return getUser() + ' ' + getMemo();
	}

	public void delayDelete(AuthzTrans trans, ApprovalDAO ad, boolean dryRun, String text) {
		if(dryRun) {
			trans.info().log(text,"- Would Delete: Approval",getId(),"on ticket",getTicket(),"for",getApprover());
		} else {
			Result<Void> rv = ad.delete(trans, add, false);
			if(rv.isOK()) {
				trans.info().log(text,"- Deleted: Approval",getId(),"on ticket",getTicket(),"for",getApprover());
				cache.delayedDelete(this);
			} else {
				trans.info().log(text,"- Failed to Delete Approval",getId());
			}
		}
	}
	

	public static void resetLocalData() {
		cache.resetLocalData();
	}
	
	public static int sizeForDeletion() {
		return cache.cacheSize();
	}

	public static void delayDelete(AuthzTrans noAvg, ApprovalDAO apprDAO, boolean dryRun, List<Approval> list, String text) {
		if(list!=null) {
			for(Approval a : list) {
				a.delayDelete(noAvg, apprDAO, dryRun,text);
			}
		}
	}

	public static boolean pendingDelete(Approval a) {
		return cache.contains(a);
	}

}
