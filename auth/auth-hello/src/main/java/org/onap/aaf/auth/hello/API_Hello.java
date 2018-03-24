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

package org.onap.aaf.auth.hello;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.onap.aaf.auth.env.AuthzTrans;
import org.onap.aaf.auth.hello.AAF_Hello.API;
import org.onap.aaf.auth.rserv.HttpCode;
import org.onap.aaf.auth.rserv.HttpMethods;
import org.onap.aaf.misc.env.Env;
import org.onap.aaf.misc.env.TimeTaken;

/**
 * API Apis
 * @author Jonathan
 *
 */
public class API_Hello {


	// Hide Public Constructor
	private API_Hello() {}
	
	/**
	 * Normal Init level APIs
	 * 
	 * @param oauthHello
	 * @param facade
	 * @throws Exception
	 */
	public static void init(final AAF_Hello oauthHello) throws Exception {
		////////
		// Overall APIs
		///////
		oauthHello.route(HttpMethods.GET,"/hello/:perm*",API.TOKEN,new HttpCode<AuthzTrans, AAF_Hello>(oauthHello,"Hello OAuth"){
			@Override
			public void handle(AuthzTrans trans, HttpServletRequest req, HttpServletResponse resp) throws Exception {
				resp.setStatus(200 /* OK */);
				ServletOutputStream os = resp.getOutputStream();
				os.print("Hello AAF ");
				String perm = pathParam(req, "perm");
				if(perm!=null && perm.length()>0) {
					os.print('(');
					os.print(req.getUserPrincipal().getName());
					TimeTaken tt = trans.start("Authorize perm", Env.REMOTE);
					try {
						if(req.isUserInRole(perm)) {
							os.print(" has ");
						} else {
							os.print(" does not have ");
						}
					} finally {
						tt.done();
					}
					os.print("Permission: ");
					os.print(perm);
					os.print(')');
				}
				os.println();
				
				trans.info().printf("Said 'Hello' to %s, Authentication type: %s",trans.getUserPrincipal().getName(),trans.getUserPrincipal().getClass().getSimpleName());
			}
		}); 

	}
}
