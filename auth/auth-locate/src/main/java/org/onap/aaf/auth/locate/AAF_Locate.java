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


package org.onap.aaf.auth.locate;

import java.io.File;
import java.net.URI;
import java.util.Map;

import javax.servlet.Filter;

import org.onap.aaf.auth.cache.Cache;
import org.onap.aaf.auth.cache.Cache.Dated;
import org.onap.aaf.auth.dao.CassAccess;
import org.onap.aaf.auth.dao.cass.LocateDAO;
import org.onap.aaf.auth.direct.DirectLocatorCreator;
import org.onap.aaf.auth.direct.DirectRegistrar;
import org.onap.aaf.auth.env.AuthzEnv;
import org.onap.aaf.auth.env.AuthzTrans;
import org.onap.aaf.auth.env.AuthzTransFilter;
import org.onap.aaf.auth.locate.api.API_AAFAccess;
import org.onap.aaf.auth.locate.api.API_Api;
import org.onap.aaf.auth.locate.api.API_Find;
import org.onap.aaf.auth.locate.api.API_Proxy;
import org.onap.aaf.auth.locate.facade.LocateFacadeFactory;
import org.onap.aaf.auth.locate.facade.LocateFacade_1_0;
import org.onap.aaf.auth.locate.mapper.Mapper.API;
import org.onap.aaf.auth.rserv.HttpMethods;
import org.onap.aaf.auth.server.AbsService;
import org.onap.aaf.auth.server.JettyServiceStarter;
import org.onap.aaf.auth.server.Log4JLogIt;
import org.onap.aaf.cadi.CadiException;
import org.onap.aaf.cadi.Locator;
import org.onap.aaf.cadi.LocatorException;
import org.onap.aaf.cadi.PropAccess;
import org.onap.aaf.cadi.aaf.v2_0.AAFAuthn;
import org.onap.aaf.cadi.aaf.v2_0.AAFConHttp;
import org.onap.aaf.cadi.aaf.v2_0.AAFLurPerm;
import org.onap.aaf.cadi.aaf.v2_0.AAFTrustChecker;
import org.onap.aaf.cadi.aaf.v2_0.AbsAAFLocator;
import org.onap.aaf.cadi.config.Config;
import org.onap.aaf.cadi.register.Registrant;
import org.onap.aaf.misc.env.APIException;
import org.onap.aaf.misc.env.Data;
import org.onap.aaf.misc.env.Env;

import com.datastax.driver.core.Cluster;

public class AAF_Locate extends AbsService<AuthzEnv, AuthzTrans> {
	private static final String DOT_LOCATOR = ".locator";

	private static final String USER_PERMS = "userPerms";
	private LocateFacade_1_0 facade; // this is the default Facade
	private LocateFacade_1_0 facade_1_0_XML;
	public Map<String, Dated> cacheUser;
	public final AAFAuthn<?> aafAuthn;
	public final AAFLurPerm aafLurPerm;
	private Locator<URI> gui_locator;
	public final long expireIn;
	private final Cluster cluster;
	public final LocateDAO locateDAO;
	private Locator<URI> dal;
	private final String aaf_service_name;
	private final String aaf_gui_name;

	
	/**
	 * Construct AuthzAPI with all the Context Supporting Routes that Authz needs
	 * 
	 * @param env
	 * @param si 
	 * @param dm 
	 * @param decryptor 
	 * @throws APIException 
	 */
	public AAF_Locate(final AuthzEnv env) throws Exception {
		super(env.access(), env);
		aaf_service_name = app_name.replace(DOT_LOCATOR, ".service");
		aaf_gui_name = app_name.replace(DOT_LOCATOR, ".gui");
		
		expireIn = Long.parseLong(env.getProperty(Config.AAF_USER_EXPIRES, Config.AAF_USER_EXPIRES_DEF));

		// Initialize Facade for all uses
		AuthzTrans trans = env.newTransNoAvg();

		cluster = org.onap.aaf.auth.dao.CassAccess.cluster(env,null);
		locateDAO = new LocateDAO(trans,cluster,CassAccess.KEYSPACE);

		// Have AAFLocator object Create DirectLocators for Location needs
		AbsAAFLocator.setCreator(new DirectLocatorCreator(env, locateDAO));

		aafLurPerm = aafCon().newLur();
		// Note: If you need both Authn and Authz construct the following:
		aafAuthn = aafCon().newAuthn(aafLurPerm);


		facade = LocateFacadeFactory.v1_0(env,locateDAO,trans,Data.TYPE.JSON);   // Default Facade
		facade_1_0_XML = LocateFacadeFactory.v1_0(env,locateDAO,trans,Data.TYPE.XML);

		synchronized(env) {
			if(cacheUser == null) {
				cacheUser = Cache.obtain(USER_PERMS);
				Cache.startCleansing(env, USER_PERMS);
			}
		}


		////////////////////////////////////////////////////////////////////////////
		// Time Critical
		//  These will always be evaluated first
		////////////////////////////////////////////////////////////////////////
		API_AAFAccess.init(this,facade);
		API_Find.init(this, facade);
		API_Proxy.init(this, facade);
		
		////////////////////////////////////////////////////////////////////////
		// Management APIs
		////////////////////////////////////////////////////////////////////////
		// There are several APIs around each concept, and it gets a bit too
		// long in this class to create.  The initialization of these Management
		// APIs have therefore been pushed to StandAlone Classes with static
		// init functions
		API_Api.init(this, facade);

		////////////////////////////////////////////////////////////////////////
		// Default Function
		////////////////////////////////////////////////////////////////////////
		API_AAFAccess.initDefault(this,facade);
		
	}

	
	/**
	 * Setup XML and JSON implementations for each supported Version type
	 * 
	 * We do this by taking the Code passed in and creating clones of these with the appropriate Facades and properties
	 * to do Versions and Content switches
	 * 
	 */
	public void route(HttpMethods meth, String path, API api, LocateCode code) throws Exception {
		String version = "1.0";
		// Get Correct API Class from Mapper
		Class<?> respCls = facade.mapper().getClass(api); 
		if(respCls==null) throw new Exception("Unknown class associated with " + api.getClass().getName() + ' ' + api.name());
		// setup Application API HTML ContentTypes for JSON and Route
		String application = applicationJSON(respCls, version);
		route(env,meth,path,code,application,"application/json;version="+version,"*/*","*");

		// setup Application API HTML ContentTypes for XML and Route
		application = applicationXML(respCls, version);
		route(env,meth,path,code.clone(facade_1_0_XML,false),application,"text/xml;version="+version);
		
		// Add other Supported APIs here as created
	}
	
	public void routeAll(HttpMethods meth, String path, API api, LocateCode code) throws Exception {
		route(env,meth,path,code,""); // this will always match
	}


	/* (non-Javadoc)
	 * @see org.onap.aaf.auth.server.AbsServer#_newAAFConHttp()
	 */
	@Override
	protected AAFConHttp _newAAFConHttp() throws CadiException {
		try {
			if(dal==null) {
				dal = AbsAAFLocator.create(aaf_service_name,Config.AAF_DEFAULT_VERSION);
			}
			// utilize pre-constructed DirectAAFLocator
			return new AAFConHttp(env.access(),dal);
		} catch (APIException | LocatorException e) {
			throw new CadiException(e);
		}

	}

	public Locator<URI> getGUILocator() throws LocatorException {
		if(gui_locator==null) {
			gui_locator = AbsAAFLocator.create(aaf_gui_name,Config.AAF_DEFAULT_VERSION);
		}
		return gui_locator;
	}


	@Override
	public Filter[] filters() throws CadiException, LocatorException {
		try {
			return new Filter[] {
				new AuthzTransFilter(env, aafCon(), 
					new AAFTrustChecker((Env)env)
				)};
		} catch (NumberFormatException e) {
			throw new CadiException("Invalid Property information", e);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public Registrant<AuthzEnv>[] registrants(final int port) throws CadiException {
		return new Registrant[] {
			new DirectRegistrar(access,locateDAO,app_name,app_version,port)
		};
	}

	@Override
	public void destroy() {
		Cache.stopTimer();
		if(cluster!=null) {
			cluster.close();
		}
		super.destroy();
	}

	public static void main(final String[] args) {
		try {
			String propsFile = getArg(AAF_LOG4J_PREFIX, args, "org.osaaf")+".log4j.props";
			String log_dir = getArg(Config.CADI_LOGDIR,args,"./logs");
			String log_level = getArg(Config.CADI_LOGLEVEL,args,"INFO");
			File logs = new File(log_dir);
			if(!logs.isDirectory()) {
				logs.delete();
			}
			if(!logs.exists()) {
				logs.mkdirs();
			}
			Log4JLogIt logIt = new Log4JLogIt(log_dir,log_level,propsFile, "locate");
			PropAccess propAccess = new PropAccess(logIt,args);

 			AAF_Locate service = new AAF_Locate(new AuthzEnv(propAccess));
			JettyServiceStarter<AuthzEnv,AuthzTrans> jss = new JettyServiceStarter<AuthzEnv,AuthzTrans>(service);
			jss.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
