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
package org.onap.aaf.authz.gw;

import org.onap.aaf.authz.env.AuthzTrans;
import org.onap.aaf.authz.gw.facade.GwFacade;
import org.onap.aaf.cssa.rserv.HttpCode;

public abstract class GwCode extends HttpCode<AuthzTrans, GwFacade> implements Cloneable {
	public boolean useJSON;

	public GwCode(GwFacade facade, String description, boolean useJSON, String ... roles) {
		super(facade, description, roles);
		this.useJSON = useJSON;
	}
	
	public <D extends GwCode> D clone(GwFacade facade, boolean useJSON) throws Exception {
		@SuppressWarnings("unchecked")
		D d = (D)clone();
		d.useJSON = useJSON;
		d.context = facade;
		return d;
	}
	
}
