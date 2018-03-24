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

package org.onap.aaf.auth.cmd.ns;

import org.onap.aaf.auth.cmd.AAFcli;
import org.onap.aaf.auth.cmd.BaseCmd;
import org.onap.aaf.auth.cmd.Param;
import org.onap.aaf.auth.rserv.HttpMethods;
import org.onap.aaf.cadi.CadiException;
import org.onap.aaf.cadi.LocatorException;
import org.onap.aaf.cadi.client.Future;
import org.onap.aaf.cadi.client.Rcli;
import org.onap.aaf.cadi.client.Retryable;
import org.onap.aaf.misc.env.APIException;

public class Admin extends BaseCmd<NS> {
	private final static String[] options = {"add","del"};

	public Admin(NS ns) throws APIException {
		super(ns,"admin",
				new Param(optionsToString(options),true),
				new Param("ns-name",true),
				new Param("id[,id]*",true)
		);
	}

	@Override
	public int _exec(int _idx, final String ... args) throws CadiException, APIException, LocatorException {
	    	int idx = _idx;
		final int option = whichOption(options, args[idx++]);
		final String ns = args[idx++];
		final String ids[] = args[idx++].split(",");

		return same(new Retryable<Integer>() {
			@Override
			public Integer code(Rcli<?> client) throws CadiException, APIException {	
				Future<Void> fp = null;
				for(String id : ids) {
					id = fullID(id);
					String verb;
					switch(option) {
						case 0: 
							fp = client.create("/authz/ns/"+ns+"/admin/"+id,Void.class);
							verb = " added to ";
							break;
						case 1: 
							fp = client.delete("/authz/ns/"+ns+"/admin/"+id,Void.class);
							verb = " deleted from ";
							break;
						default:
							throw new CadiException("Bad Argument");
					};
				
					if(fp.get(AAFcli.timeout())) {
						pw().append("Admin ");
						pw().append(id);
						pw().append(verb);
						pw().println(ns);
					} else {
						error(fp);
						return fp.code();
					}
					
				}
				return fp==null?500:fp.code();
			}
		});
	}

	@Override
	public void detailedHelp(int _indent, StringBuilder sb) {
	    	int indent = _indent;
		detailLine(sb,indent,"Add or Delete Administrator to/from Namespace");
		indent+=4;
		detailLine(sb,indent,"name - Name of Namespace");
		detailLine(sb,indent,"id   - Credential of Person(s) to be Administrator");
		sb.append('\n');
		detailLine(sb,indent,"aafcli will call API on each ID presented.");
		indent-=4;
		api(sb,indent,HttpMethods.POST,"authz/ns/<ns>/admin/<id>",Void.class,true);
		api(sb,indent,HttpMethods.DELETE,"authz/ns/<ns>/admin/<id>",Void.class,false);
	}

}
