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

package org.onap.aaf.auth.batch.update;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.onap.aaf.auth.batch.Batch;
import org.onap.aaf.auth.batch.BatchPrincipal;
import org.onap.aaf.auth.batch.approvalsets.ApprovalSet;
import org.onap.aaf.auth.batch.approvalsets.Pending;
import org.onap.aaf.auth.batch.approvalsets.URApprovalSet;
import org.onap.aaf.auth.batch.helpers.BatchDataView;
import org.onap.aaf.auth.batch.helpers.CQLBatch;
import org.onap.aaf.auth.batch.helpers.CQLBatchLoop;
import org.onap.aaf.auth.batch.helpers.LastNotified;
import org.onap.aaf.auth.batch.helpers.NS;
import org.onap.aaf.auth.batch.helpers.Notification;
import org.onap.aaf.auth.batch.helpers.Notification.TYPE;
import org.onap.aaf.auth.batch.helpers.Role;
import org.onap.aaf.auth.batch.helpers.UserRole;
import org.onap.aaf.auth.batch.reports.Notify;
import org.onap.aaf.auth.batch.reports.bodies.NotifyPendingApprBody;
import org.onap.aaf.auth.dao.cass.UserRoleDAO;
import org.onap.aaf.auth.env.AuthzTrans;
import org.onap.aaf.auth.layer.Result;
import org.onap.aaf.auth.org.OrganizationException;
import org.onap.aaf.cadi.Access;
import org.onap.aaf.cadi.CadiException;
import org.onap.aaf.cadi.client.Holder;
import org.onap.aaf.cadi.util.CSV;
import org.onap.aaf.misc.env.APIException;
import org.onap.aaf.misc.env.TimeTaken;
import org.onap.aaf.misc.env.Trans;
import org.onap.aaf.misc.env.util.Chrono;

public class Approvals extends Batch {
    private final Access access;
	private final AuthzTrans noAvg;
	private BatchDataView dataview;
	private List<CSV> csvList;
	private GregorianCalendar now;
	private final Notify notify;
	

    public Approvals(AuthzTrans trans) throws APIException, IOException, OrganizationException {
        super(trans.env());
        notify = new Notify(trans);
        access = env.access();
        noAvg = env.newTransNoAvg();
        noAvg.setUser(new BatchPrincipal("batch:Approvals"));
        session = cluster.connect();
        dataview = new BatchDataView(noAvg,session,dryRun);
        NS.load(trans, session, NS.v2_0_11);
        Role.load(trans, session);
        UserRole.load(trans, session, UserRole.v2_0_11);

        now = new GregorianCalendar();
        
        csvList = new ArrayList<>();
        File f;
        if(args().length>0) {
        	for(int i=0;i<args().length;++i) {
        		f = new File(logDir(), args()[i]);
        		if(f.exists()) {
	        		csvList.add(new CSV(env.access(),f).processAll());
        		} else {
	            	trans.error().printf("CSV File %s does not exist",f.getAbsolutePath());
        		}
        	}
        } else {
        	f = new File(logDir(), "Approvals"+Chrono.dateOnlyStamp()+".csv");
        	if(f.exists()) {
        		csvList.add(new CSV(env.access(),f).processAll());
			} else {
	        	trans.error().printf("CSV File %s does not exist",f.getAbsolutePath());
			}
        }
    }

    @Override
    protected void run(AuthzTrans trans) {
    	Map<String,Pending> mpending = new TreeMap<>();
		Holder<Integer> count = new Holder<>(0);
		final CQLBatchLoop cbl = new CQLBatchLoop(new CQLBatch(noAvg.info(),session),100,dryRun);
        for(CSV approveCSV : csvList) {
        	TimeTaken tt = trans.start("Load Analyzed Reminders",Trans.SUB,approveCSV.name());
        	try {
				approveCSV.visit(row -> {
					switch(row.get(0)) {
						case Pending.REMIND:
							try {
								String user = row.get(1);
								Pending p = new Pending(row);
								Pending mp = mpending.get(user);
								if(mp==null) {
									mpending.put(user, p);
								} else {
									mp.inc(p); // FYI, unlikely
								}
								count.set(count.get()+1);
							} catch (ParseException e) {
								trans.error().log(e);
							} 
						break;
					}
				});
			} catch (IOException | CadiException e) {
				e.printStackTrace();
				// .... but continue with next row
        	} finally {
        		tt.done();
        	}
        }
        trans.info().printf("Processed %d Reminder Rows", count.get());

        count.set(0);
        for(CSV approveCSV : csvList) {
        	TimeTaken tt = trans.start("Processing %s's UserRoles",Trans.SUB,approveCSV.name());
        	try {
				approveCSV.visit(row -> {
					switch(row.get(0)) {
						case UserRole.APPROVE_UR:
							UserRoleDAO.Data urdd = UserRole.row(row);
							// Create an Approval
							ApprovalSet uras = new URApprovalSet(noAvg, now, dataview, () -> {
								return urdd;
							});
							Result<Void> rw = uras.write(noAvg);
							if(rw.isOK()) {
								Pending p = new Pending();
								Pending mp = mpending.get(urdd.user);
								if(mp==null) {
									mpending.put(urdd.user, p);
								} else {
									mp.inc(p);
								}
								count.set(count.get()+1);
							} else {
								trans.error().log(rw.errorString());
							}
							break;
					}
				});
				dataview.flush();
			} catch (IOException | CadiException e) {
				e.printStackTrace();
				// .... but continue with next row
	    	} finally {
	    		tt.done();
	    	}
            trans.info().printf("Processed %d UserRoles", count.get());

            count.set(0);
        	NotifyPendingApprBody npab = new NotifyPendingApprBody(access);

        	GregorianCalendar gc = new GregorianCalendar();
        	gc.add(GregorianCalendar.DAY_OF_MONTH, 7);
        	Date oneWeek = gc.getTime();
        	CSV.Saver rs = new CSV.Saver();
        	
        	tt = trans.start("Obtain Last Notifications", Trans.SUB);
        	LastNotified lastN;
        	try {
        		lastN = new LastNotified(session);
        		lastN.add(mpending.keySet());
        	} finally {
        		tt.done();
        	}
        	
        	Pending p;
        	tt = trans.start("Notify for Pending", Trans.SUB);
        	try {
        		for(Entry<String, Pending> es : mpending.entrySet()) {
        			p = es.getValue();
        			Date dateLastNotified = lastN.lastNotified(es.getKey());
        			if(p.newApprovals() || dateLastNotified==null || dateLastNotified.after(oneWeek) ) {
        				rs.row("appr", es.getKey(),p.qty(),batchEnv);
        				npab.store(rs.asList());
        				if(notify.notify(noAvg, npab)>0) {
        					// Update
        					cbl.preLoop();
        					update(cbl.inc(),es.getKey(),Notification.TYPE.OA);
        				}
        			}
        		}
        	} finally {
        		tt.done();
        	}
            trans.info().printf("Created %d Notifications", count.get());
	    }
    }
    
    private void update(StringBuilder sb, String user, TYPE oa) {
    	sb.append("UPDATE authz.notify SET last=dateof(now()) WHERE user='");
    	sb.append(user);
    	sb.append("' AND type=");
    	sb.append(oa.idx());
    	sb.append(';');
		
	}

	@Override
    protected void _close(AuthzTrans trans) {
    	if(session!=null) {
    		session.close();
    		session = null;
    	}
    }
}
