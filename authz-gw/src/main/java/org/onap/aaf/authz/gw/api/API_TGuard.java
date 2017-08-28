/*******************************************************************************
 * ============LICENSE_START====================================================
 * * org.onap.aaf
 * * ===========================================================================
 * * Copyright © 2017 AT&T Intellectual Property. All rights reserved.
 * * ===========================================================================
 * * Licensed under the Apache License, Version 2.0 (the "License");
 * * you may not use this file except in compliance with the License.
 * * You may obtain a copy of the License at
 * * 
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 * * 
 *  * Unless required by applicable law or agreed to in writing, software
 * * distributed under the License is distributed on an "AS IS" BASIS,
 * * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * * See the License for the specific language governing permissions and
 * * limitations under the License.
 * * ============LICENSE_END====================================================
 * *
 * * ECOMP is a trademark and service mark of AT&T Intellectual Property.
 * *
 ******************************************************************************/
package org.onap.aaf.authz.gw.api;

import static org.onap.aaf.authz.layer.Result.OK;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.onap.aaf.authz.env.AuthzTrans;
import org.onap.aaf.authz.gw.GwAPI;
import org.onap.aaf.authz.gw.GwCode;
import org.onap.aaf.authz.gw.facade.GwFacade;
import org.onap.aaf.authz.gw.mapper.Mapper.API;
import org.onap.aaf.authz.layer.Result;
import org.onap.aaf.cssa.rserv.HttpMethods;

import com.att.aft.dme2.internal.jetty.http.HttpStatus;

/**
 * API Apis
 *
 */
public class API_TGuard {
	/**
	 * Normal Init level APIs
	 * 
	 * @param gwAPI
	 * @param facade
	 * @throws Exception
	 */
	public static void init(final GwAPI gwAPI, GwFacade facade) throws Exception {
		String aftenv = gwAPI.env.getProperty(API_AAFAccess.AFT_ENVIRONMENT);
		if(aftenv==null) throw new Exception(API_AAFAccess.AFT_ENVIRONMENT + " must be set");

		////////
		// Do not deploy these to PROD
		///////
		if(API_AAFAccess.AFTUAT.equals(aftenv)) {
			gwAPI.route(HttpMethods.GET,"/tguard/:path*",API.VOID,new GwCode(facade,"TGuard Test", true) {
				@Override
				public void handle(AuthzTrans trans, HttpServletRequest req, HttpServletResponse resp) throws Exception {
					Result<Void> r = context.getAPI(trans,resp,gwAPI);
					switch(r.status) {
						case OK:
							resp.setStatus(HttpStatus.OK_200);
							break;
						default:
							context.error(trans,resp,r);
					}
				}
			});
		}
	}
}
