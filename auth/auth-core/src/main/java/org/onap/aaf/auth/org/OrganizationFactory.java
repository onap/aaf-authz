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

package org.onap.aaf.auth.org;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.onap.aaf.auth.env.AuthzTrans;
import org.onap.aaf.cadi.util.FQI;
import org.onap.aaf.misc.env.Env;
import org.onap.aaf.misc.env.impl.BasicEnv;

/**
 * Organization Plugin Mechanism
 * 
 * Define a NameSpace for the company (i.e. com.att), and put in Properties as 
 * "Organization.[your NS" and assign the supporting Class.  
 * 
 * Example:
 * Organization.com.att=org.onap.aaf.auth.org.test.att.ATT
 *
 * @author Pavani, Jonathan
 *
 */
public class OrganizationFactory {
	private static final String ORGANIZATION_DOT = "Organization.";
	private static Organization defaultOrg = null;
	private static Map<String,Organization> orgs = new ConcurrentHashMap<String,Organization>();
	public static Organization init(BasicEnv env) throws OrganizationException {
		int idx = ORGANIZATION_DOT.length();
		Organization org,firstOrg = null;
		
		for(Entry<Object, Object> es : env.getProperties().entrySet()) {
			String key = es.getKey().toString();
			if(key.startsWith(ORGANIZATION_DOT)) {
				org = obtain(env,key.substring(idx));
				if(firstOrg==null) {
					firstOrg = org;
				}
			}
		}
		if(defaultOrg == null) {
			defaultOrg = firstOrg;
		}
		return defaultOrg;
	}
	public static Organization obtain(Env env,final String theNS) throws OrganizationException {
		String orgNS;
		if(theNS.indexOf('@')>=0) {
			orgNS=FQI.reverseDomain(theNS);
		} else {
			orgNS=theNS;
		}
		Organization org = orgs.get(orgNS);
		if(org == null) {
			String orgClass = env.getProperty(ORGANIZATION_DOT+orgNS);
			if(orgClass == null) {
				env.warn().log("There is no Organization." + orgNS + " property");
			} else {
				for(Organization o : orgs.values()) {
					if(orgClass.equals(o.getClass().getName())) {
						org = o;
					}
				}
				if(org==null) {
					try {
						@SuppressWarnings("unchecked")
						Class<Organization> cls = (Class<Organization>) Class.forName(orgClass);
						Constructor<Organization> cnst = cls.getConstructor(Env.class,String.class);
						org = cnst.newInstance(env,orgNS);
					} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | 
							InstantiationException | IllegalAccessException | IllegalArgumentException | 
							InvocationTargetException e) {
						env.error().log(e, "Error on Organization Construction");
						throw new OrganizationException(e);
					}
				}
				orgs.put(orgNS, org);
				if("true".equalsIgnoreCase(env.getProperty(orgNS+".default"))) {
					defaultOrg = org;
				}

			}
			if(org==null) {
				if(defaultOrg!=null) {
					org=defaultOrg;
					orgs.put(orgNS, org);
				}
			}
		}
		
		return org;
	}

	public static Organization get(AuthzTrans trans) throws OrganizationException {
		String domain = FQI.reverseDomain(trans.user());
		Organization org = orgs.get(domain);
		if(org==null) {
			org = defaultOrg; // can be null, btw, unless set.
		}
		return org;
	}
}
