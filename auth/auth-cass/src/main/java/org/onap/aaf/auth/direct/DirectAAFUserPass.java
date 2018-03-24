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

package org.onap.aaf.auth.direct;

import static org.onap.aaf.auth.layer.Result.OK;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.onap.aaf.auth.dao.DAOException;
import org.onap.aaf.auth.dao.hl.Question;
import org.onap.aaf.auth.env.AuthzEnv;
import org.onap.aaf.auth.env.AuthzTrans;
import org.onap.aaf.auth.layer.Result;
import org.onap.aaf.cadi.CredVal;

/**
 * DirectAAFUserPass is intended to provide password Validation directly from Cassandra Database, and is only
 * intended for use in AAF itself.  The normal "AAF Taf" objects are, of course, clients.
 * 
 * @author Jonathan
 *
 */
public class DirectAAFUserPass implements CredVal {
	private final AuthzEnv env;
	private final Question question;
	
	public DirectAAFUserPass(AuthzEnv env, Question question) {
		this.env = env;
		this.question = question;
	}

	@Override
	public boolean validate(String user, Type type, byte[] pass, Object state) {
			try {
				AuthzTrans trans;
				if(state !=null) {
					if(state instanceof AuthzTrans) {
						trans = (AuthzTrans)state;
					} else {
						trans = env.newTransNoAvg();
						if(state instanceof HttpServletRequest) {
							trans.set((HttpServletRequest)state);
						}
					}
				} else {
					trans = env.newTransNoAvg();
				}
				Result<Date> result = question.doesUserCredMatch(trans, user, pass);
				trans.logAuditTrail(env.info());
				switch(result.status) {
					case OK:
						return true;
					default:
						String ip = trans.ip()==null?"":(", ip="+trans.ip());
						env.warn().log(user, "failed password validation" + ip + ':',result.errorString());
				}
			} catch (DAOException e) {
				env.error().log(e,"Cannot validate user/pass from cassandra");
			}
		return false;
	}
}
