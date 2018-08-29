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
package org.onap.aaf.cadi.http;

import java.io.IOException;
import java.net.HttpURLConnection;

import org.onap.aaf.cadi.CadiException;
import org.onap.aaf.cadi.SecuritySetter;
import org.onap.aaf.cadi.config.Config;
import org.onap.aaf.cadi.config.SecurityInfoC;
import org.onap.aaf.cadi.config.SecurityInfoInit;
import org.onap.aaf.misc.env.APIException;

/**
 * This class will pick out the best default SS for Clients per Client type
 * 
 * @author jg1555
 *
 */
public class HSecurityInfoInit implements SecurityInfoInit<HttpURLConnection> {

	@Override
	public SecuritySetter<HttpURLConnection> bestDefault(SecurityInfoC<HttpURLConnection> si) throws CadiException {
		try {
			if(si.defaultAlias!=null) {
				si.set(new HX509SS(si));
			} else if(si.access.getProperty(Config.AAF_APPID, null)!=null &&
					  si.access.getProperty(Config.AAF_APPPASS, null)!=null) {
				si.set(new HBasicAuthSS(si));
			}
		} catch (APIException | IOException e) {
			throw new CadiException(e);
		}
		return si.defSS;
	}

	@Override
	public Class<HttpURLConnection> getClientClass() {
		return HttpURLConnection.class;
	}

}
