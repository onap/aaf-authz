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

package org.onap.aaf.auth.gui.pages;

import java.io.IOException;
import java.net.ConnectException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.onap.aaf.auth.env.AuthzTrans;
import org.onap.aaf.auth.gui.AAF_GUI;
import org.onap.aaf.auth.gui.BreadCrumbs;
import org.onap.aaf.auth.gui.NamedCode;
import org.onap.aaf.auth.gui.Page;
import org.onap.aaf.auth.gui.Table;
import org.onap.aaf.auth.gui.Table.Cells;
import org.onap.aaf.auth.gui.table.AbsCell;
import org.onap.aaf.auth.gui.table.RefCell;
import org.onap.aaf.auth.gui.table.TableData;
import org.onap.aaf.auth.gui.table.TextCell;
import org.onap.aaf.cadi.CadiException;
import org.onap.aaf.cadi.client.Future;
import org.onap.aaf.cadi.client.Rcli;
import org.onap.aaf.cadi.client.Retryable;
import org.onap.aaf.misc.env.APIException;
import org.onap.aaf.misc.env.Env;
import org.onap.aaf.misc.env.TimeTaken;
import org.onap.aaf.misc.xgen.Cache;
import org.onap.aaf.misc.xgen.DynamicCode;
import org.onap.aaf.misc.xgen.html.HTMLGen;

import aaf.v2_0.Approval;
import aaf.v2_0.Approvals;

public class PendingRequestsShow extends Page {
	public static final String HREF = "/gui/myrequests";
	public static final String NAME = "MyRequests";
	static final String WEBPHONE = "http://webphone.att.com/cgi-bin/webphones.pl?id=";
	private static DateFormat createdDF = new SimpleDateFormat("yyyy-MM-dd");
	
	public PendingRequestsShow(final AAF_GUI gui, final Page ... breadcrumbs) throws APIException, IOException {
		super(gui.env, NAME,HREF, NO_FIELDS,
			new BreadCrumbs(breadcrumbs), 
			new NamedCode(true,"expedite") {
			@Override
			public void code(final Cache<HTMLGen> cache, final HTMLGen hgen) throws APIException, IOException {
				cache.dynamic(hgen, new DynamicCode<HTMLGen, AAF_GUI, AuthzTrans>() {
					@Override
					public void code(final AAF_GUI gui, final AuthzTrans trans,	final Cache<HTMLGen> cache, final HTMLGen hgen)	throws APIException, IOException {
						hgen
							.leaf("p", "class=expedite_request").text("These are your submitted Requests that are awaiting Approval. ")
							.br()
							.text("To Expedite a Request: ")
							.leaf("a","href=#expedite_directions","onclick=divVisibility('expedite_directions');")
								.text("Click Here").end()
							.divID("expedite_directions", "style=display:none");
						hgen
							.incr(HTMLGen.OL)
							.incr(HTMLGen.LI)
							.leaf("a","href="+ApprovalForm.HREF+"?user="+trans.user(), "id=userApprove")
							.text("Copy This Link")
							.end()
							.end()
							.incr(HTMLGen.LI)
							.text("Send it to the Approver Listed")
							.end()
							.end()
							.text("NOTE: Using this link, the Approver will only see your requests. You only need to send this link once!")
							.end()
							.end();
					}
				});
			}
		},
			new Table<AAF_GUI,AuthzTrans>("Pending Requests",gui.env.newTransNoAvg(),new Model(), "class=std")
		);
					

	}

	/**
	 * Implement the Table Content for Requests by User
	 * 
	 * @author Jeremiah
	 *
	 */
	private static class Model extends TableData<AAF_GUI,AuthzTrans> {
		private static final String CSP_ATT_COM = "@csp.att.com";
		final long NUM_100NS_INTERVALS_SINCE_UUID_EPOCH = 0x01b21dd213814000L;
		private static final String[] headers = new String[] {"Request Date","Status","Memo","Approver"};

		@Override
		public String[] headers() {
			return headers;
		}
		
		@Override
		public Cells get(final AuthzTrans trans, final AAF_GUI gui) {
			final ArrayList<AbsCell[]> rv = new ArrayList<AbsCell[]>();
			try {
				gui.clientAsUser(trans.getUserPrincipal(), new Retryable<Void>() {
					@Override
					public Void code(Rcli<?> client)throws CadiException, ConnectException, APIException {
						TimeTaken tt = trans.start("AAF Get Approvals by User",Env.REMOTE);
						try {
							Future<Approvals> fa = client.read("/authz/approval/user/"+trans.user(),gui.getDF(Approvals.class));
							if(fa.get(5000)) {
								tt.done();
								tt = trans.start("Load Data", Env.SUB);
								if(fa.value!=null) {
									List<Approval> approvals = fa.value.getApprovals();
									Collections.sort(approvals, new Comparator<Approval>() {
										@Override
										public int compare(Approval a1, Approval a2) {
											UUID id1 = UUID.fromString(a1.getId());
											UUID id2 = UUID.fromString(a2.getId());
											return id1.timestamp()<=id2.timestamp()?1:-1;
										}
									});
									
									String prevTicket = null;
									for(Approval a : approvals) {
										String approver = a.getApprover();
										String approverShort = approver.substring(0,approver.indexOf('@'));
										
										AbsCell tsCell = null;
										String ticket = a.getTicket();
										if (ticket==null || ticket.equals(prevTicket)) {
											tsCell = AbsCell.Null;
										} else {
											UUID id = UUID.fromString(a.getId());
											tsCell = new RefCell(createdDF.format((id.timestamp() - NUM_100NS_INTERVALS_SINCE_UUID_EPOCH)/10000),
													RequestDetail.HREF + "?ticket=" + ticket,false);
											prevTicket = ticket;
										}
										
										AbsCell approverCell = null;
										if (approver.endsWith(CSP_ATT_COM)) {
											approverCell = new RefCell(approver, WEBPHONE + approverShort,true);
										} else {
											approverCell = new TextCell(approver);
										}
										AbsCell[] sa = new AbsCell[] {
											tsCell,
											new TextCell(a.getStatus()),
											new TextCell(a.getMemo()),
											approverCell
										};
										rv.add(sa);
									}
								}
							} else {
								gui.writeError(trans, fa, null, 0);
							}
						} finally {
							tt.done();
						}


						return null;
					}
				});
			} catch (Exception e) {
				trans.error().log(e);
			}
			return new Cells(rv,null);
		}
	}
}
