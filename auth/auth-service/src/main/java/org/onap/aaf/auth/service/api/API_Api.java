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

package org.onap.aaf.auth.service.api;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpStatus;
import org.onap.aaf.auth.env.AuthzTrans;
import org.onap.aaf.auth.layer.Result;
import org.onap.aaf.auth.rserv.HttpMethods;
import org.onap.aaf.auth.service.AAF_Service;
import org.onap.aaf.auth.service.Code;
import org.onap.aaf.auth.service.facade.AuthzFacade;
import org.onap.aaf.auth.service.mapper.Mapper.API;
import org.onap.aaf.cadi.Symm;

/**
 * API Apis
 * @author Jonathan
 *
 */
public class API_Api {
	// Hide Public Constructor
	private API_Api() {}
	
	/**
	 * Normal Init level APIs
	 * 
	 * @param authzAPI
	 * @param facade
	 * @throws Exception
	 */
	public static void init(final AAF_Service authzAPI, AuthzFacade facade) throws Exception {
		////////
		// Overall APIs
		///////
		authzAPI.route(HttpMethods.GET,"/api",API.API,new Code(facade,"Document API", true) {
			@Override
			public void handle(AuthzTrans trans, HttpServletRequest req, HttpServletResponse resp) throws Exception {
				Result<Void> r = context.getAPI(trans,resp,authzAPI);
				if(r.isOK()) {
					resp.setStatus(HttpStatus.OK_200);
				} else {
					context.error(trans,resp,r);
				}
			}
		});

		////////
		// Overall Examples
		///////
		authzAPI.route(HttpMethods.GET,"/api/example/*",API.VOID,new Code(facade,"Document API", true) {
			@Override
			public void handle(AuthzTrans trans, HttpServletRequest req, HttpServletResponse resp) throws Exception {
				String pathInfo = req.getPathInfo();
				int question = pathInfo.lastIndexOf('?');
				
				pathInfo = pathInfo.substring(13, question<0?pathInfo.length():question);// IMPORTANT, this is size of "/api/example/"
				String nameOrContextType=Symm.base64noSplit.decode(pathInfo);
				Result<Void> r = context.getAPIExample(trans,resp,nameOrContextType,
						question>=0 && "optional=true".equalsIgnoreCase(req.getPathInfo().substring(question+1))
						);
				if(r.isOK()) {
					resp.setStatus(HttpStatus.OK_200);
				} else {
					context.error(trans,resp,r);
				}
			}
		});

	}
}
