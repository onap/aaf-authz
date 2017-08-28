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
package org.onap.aaf.authz.service.api;

import static org.junit.Assert.*;

import org.junit.Test;
import org.mockito.Mock;
import org.onap.aaf.authz.facade.AuthzFacade;
import org.onap.aaf.authz.service.AuthAPI;
import org.onap.aaf.authz.service.api.API_NS;

public class JU_API_NS {
	API_NS api_Ns;
	@Mock
	AuthAPI authzAPI;
	AuthzFacade facade;

	
	@SuppressWarnings("static-access")
	@Test
	public void testInit(){
		try {
			api_Ns.init(authzAPI, facade);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
