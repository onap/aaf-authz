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

package org.onap.aaf.cadi.config;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;

import org.onap.aaf.cadi.AbsUserCache;
import org.onap.aaf.cadi.Access;
import org.onap.aaf.cadi.Access.Level;
import org.onap.aaf.cadi.CachingLur;
import org.onap.aaf.cadi.CadiException;
import org.onap.aaf.cadi.Connector;
import org.onap.aaf.cadi.CredVal;
import org.onap.aaf.cadi.Locator;
import org.onap.aaf.cadi.Lur;
import org.onap.aaf.cadi.PropAccess;
import org.onap.aaf.cadi.Symm;
import org.onap.aaf.cadi.TrustChecker;
import org.onap.aaf.cadi.lur.EpiLur;
import org.onap.aaf.cadi.lur.LocalLur;
import org.onap.aaf.cadi.lur.NullLur;
import org.onap.aaf.cadi.taf.HttpEpiTaf;
import org.onap.aaf.cadi.taf.HttpTaf;
import org.onap.aaf.cadi.taf.basic.BasicHttpTaf;
import org.onap.aaf.cadi.taf.cert.X509Taf;
import org.onap.aaf.cadi.taf.dos.DenialOfServiceTaf;

/**
 * Create a Consistent Configuration mechanism, even when configuration styles are as vastly different as
 * Properties vs JavaBeans vs FilterConfigs...
 * 
 * @author Jonathan
 *
 */
public class Config {

	public static final String UTF_8 = "UTF-8";

	// Property Names associated with configurations.
	// As of 1.0.2, these have had the dots removed so as to be compatible with JavaBean style
	// configurations as well as property list style.
	public static final String HOSTNAME = "hostname";
	public static final String CADI_PROP_FILES = "cadi_prop_files"; // Additional Properties files (separate with ;)
	public static final String CADI_LOGLEVEL = "cadi_loglevel";
	public static final String CADI_LOGDIR = "cadi_logdir";
	public static final String CADI_LOGNAME = "cadi_logname";
	public static final String CADI_KEYFILE = "cadi_keyfile";
	public static final String CADI_KEYSTORE = "cadi_keystore";
	public static final String CADI_KEYSTORE_PASSWORD = "cadi_keystore_password";
	public static final String CADI_ALIAS = "cadi_alias";
	public static final String CADI_LOGINPAGE_URL = "cadi_loginpage_url";
	public static final String CADI_LATITUDE = "cadi_latitude";
	public static final String CADI_LONGITUDE = "cadi_longitude";


	public static final String CADI_KEY_PASSWORD = "cadi_key_password";
	public static final String CADI_TRUSTSTORE = "cadi_truststore";
	public static final String CADI_TRUSTSTORE_PASSWORD = "cadi_truststore_password";
	public static final String CADI_X509_ISSUERS = "cadi_x509_issuers";
	public static final String CADI_TRUST_MASKS="cadi_trust_masks";
	public static final String CADI_TRUST_PERM="cadi_trust_perm"; //  IDs with this perm can utilize the "AS " user concept
	public static final String CADI_PROTOCOLS = "cadi_protocols";
	public static final String CADI_NOAUTHN = "cadi_noauthn";
	public static final String CADI_LOC_LIST = "cadi_loc_list";
	
	public static final String CADI_USER_CHAIN_TAG = "cadi_user_chain";
	public static final String CADI_USER_CHAIN = "USER_CHAIN";
	
	public static final String CADI_OAUTH2_URL="cadi_oauth2_url";
	public static final String CADI_TOKEN_DIR = "cadi_token_dir";

	public static final String CSP_DOMAIN = "csp_domain";
	public static final String CSP_HOSTNAME = "csp_hostname";
	public static final String CSP_DEVL_LOCALHOST = "csp_devl_localhost";
	public static final String CSP_USER_HEADER = "CSP_USER";
	public static final String CSP_SYSTEMS_CONF = "CSPSystems.conf";
    public static final String CSP_SYSTEMS_CONF_FILE = "csp_systems_conf_file";
    
    public static final String HTTPS_PROTOCOLS = "https.protocols";
    public static final String HTTPS_CIPHER_SUITES = "https.cipherSuites";
    public static final String HTTPS_CLIENT_PROTOCOLS="jdk.tls.client.protocols";
    public static final String HTTPS_CIPHER_SUITES_DEFAULT="TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256,TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA,"
    		+ "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA,TLS_RSA_WITH_AES_128_CBC_SHA,TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA,"
    		+ "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA,TLS_DHE_RSA_WITH_AES_128_CBC_SHA,TLS_DHE_DSS_WITH_AES_128_CBC_SHA,"
    		+ "TLS_ECDHE_ECDSA_WITH_RC4_128_SHA,TLS_ECDHE_RSA_WITH_RC4_128_SHA,TLS_ECDH_ECDSA_WITH_RC4_128_SHA,"
    		+ "TLS_ECDH_RSA_WITH_RC4_128_SHA,TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA,TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA,"
    		+ "TLS_ECDH_ECDSA_WITH_3DES_EDE_CBC_SHA,TLS_ECDH_RSA_WITH_3DES_EDE_CBC_SHA,TLS_EMPTY_RENEGOTIATION_INFO_SCSV";
    		 

	public static final String LOCALHOST_ALLOW = "localhost_allow";
	public static final String LOCALHOST_DENY = "localhost_deny";
	
	public static final String BASIC_REALM = "basic_realm";  // what is sent to the client 
	public static final String BASIC_WARN = "basic_warn";  // Warning of insecure channel 
	public static final String USERS = "local_users";
	public static final String GROUPS = "local_groups";
	public static final String WRITE_TO = "local_writeto"; // dump RBAC to local file in Tomcat Style (some apps use)
	
	public static final String OAUTH_CLIENT_ID="client_id";
	public static final String OAUTH_CLIENT_SECRET="client_secret";
	
	public static final String AAF_ENV = "aaf_env";
	public static final String AAF_URL = "aaf_url"; //URL for AAF... Use to trigger AAF configuration
	public static final String AAF_ROOT_NS = "aaf_root_ns";
	public static final String AAF_ROOT_COMPANY = "aaf_root_company";
	public static final String AAF_LOCATE_URL = "aaf_locate_url"; //URL for AAF locator
	private static final String AAF_LOCATE_URL_TAG = "AAF_LOCATE_URL"; // Name of Above for use in Config Variables.
	public static final String AAF_APPID = "aaf_id";
	public static final String AAF_APPPASS = "aaf_password";
	public static final String AAF_LUR_CLASS = "aaf_lur_class";
	public static final String AAF_TAF_CLASS = "aaf_taf_class";
	public static final String AAF_TAF_CLASS_DEF = "org.osaaf.cadi.aaf.v2_0.AAFTaf";
	public static final String AAF_CONNECTOR_CLASS = "aaf_connector_class";
	public static final String AAF_LOCATOR_CLASS = "aaf_locator_class";
	public static final String AAF_CONN_TIMEOUT = "aaf_conn_timeout";
	public static final String AAF_CONN_TIMEOUT_DEF = "3000";
	public static final String AAF_CONN_IDLE_TIMEOUT = "aaf_conn_idle_timeout"; // only for Direct Jetty Access.
	public static final String AAF_CONN_IDLE_TIMEOUT_DEF = "10000"; // only for Direct Jetty Access.
	
	public static final String AAF_CALL_TIMEOUT = "aaf_timeout";
	public static final String AAF_CALL_TIMEOUT_DEF = "5000";
	public static final String AAF_USER_EXPIRES = "aaf_user_expires";
	public static final String AAF_USER_EXPIRES_DEF = "600000"; // Default is 10 mins
	public static final String AAF_CLEAN_INTERVAL = "aaf_clean_interval";
	public static final String AAF_CLEAN_INTERVAL_DEF = "30000"; // Default is 30 seconds
	public static final String AAF_REFRESH_TRIGGER_COUNT = "aaf_refresh_trigger_count";
	public static final String AAF_REFRESH_TRIGGER_COUNT_DEF = "3"; // Default is 10 mins
	
	public static final String AAF_HIGH_COUNT = "aaf_high_count";
	public static final String AAF_HIGH_COUNT_DEF = "1000"; // Default is 1000 entries
	public static final String AAF_PERM_MAP = "aaf_perm_map";
	public static final String AAF_COMPONENT = "aaf_component";
	public static final String AAF_CERT_IDS = "aaf_cert_ids";
	public static final String AAF_DEBUG_IDS = "aaf_debug_ids"; // comma delimited
	public static final String AAF_DEFAULT_VERSION = "2.0";
	public static final String AAF_DATA_DIR = "aaf_data_dir"; // AAF processes and Components only.


	
	public static final String GW_URL = "gw_url";
	public static final String CM_URL = "cm_url";
	public static final String CM_TRUSTED_CAS = "cm_trusted_cas";

	public static final String PATHFILTER_URLPATTERN = "pathfilter_urlpattern";
	public static final String PATHFILTER_STACK = "pathfilter_stack";
	public static final String PATHFILTER_NS = "pathfilter_ns";
	public static final String PATHFILTER_NOT_AUTHORIZED_MSG = "pathfilter_not_authorized_msg";

	// This one should go unpublic
	public static final String AAF_DEFAULT_REALM = "aaf_default_realm";
	private static String defaultRealm="none";

	public static final String AAF_DOMAIN_SUPPORT = "aaf_domain_support";
	public static final String AAF_DOMAIN_SUPPORT_DEF = ".com";

	// OAUTH2
	public static final String AAF_OAUTH2_TOKEN_URL = "aaf_oauth2_token_url";
	public static final String AAF_OAUTH2_INTROSPECT_URL = "aaf_oauth2_introspect_url";
	public static final String AAF_ALT_OAUTH2_TOKEN_URL = "aaf_alt_oauth2_token_url";
	public static final String AAF_ALT_OAUTH2_INTROSPECT_URL = "aaf_alt_oauth2_introspect_url";
	public static final String AAF_ALT_OAUTH2_DOMAIN = "aaf_alt_oauth2_domain"; 
	public static final String AAF_ALT_CLIENT_ID = "aaf_alt_oauth2_client_id";
	public static final String AAF_ALT_CLIENT_SECRET = "aaf_alt_oauth2_client_secret";
	public static final String AAF_OAUTH2_HELLO_URL = "aaf_oauth2_hello_url";

	
	
	public static void setDefaultRealm(Access access) throws CadiException {
		try {
			boolean hasCSP;
			try {
				Class.forName("org.osaaf.cadi.taf.csp.CSPTaf");
				hasCSP=true;
			} catch(ClassNotFoundException e) {
				hasCSP = logProp(access,Config.CSP_DOMAIN, null)!=null;
			}
			defaultRealm = logProp(access,Config.AAF_DEFAULT_REALM,
					hasCSP?"csp.att.com":
					logProp(access,Config.BASIC_REALM,
						logProp(access,HOSTNAME,InetAddress.getLocalHost().getHostName())
						)
					);
		} catch (UnknownHostException e) {
			//defaultRealm="none";
		}
	}

	public static HttpTaf configHttpTaf(Connector con, SecurityInfoC<HttpURLConnection> si, TrustChecker tc, CredVal up, Lur lur, Object ... additionalTafLurs) throws CadiException {
		Access access = si.access;
		/////////////////////////////////////////////////////
		// Setup AAFCon for any following
		/////////////////////////////////////////////////////
		Class<?> aafConClass = loadClass(access,"org.onap.aaf.cadi.aaf.v2_0.AAFCon");
		Object aafcon = null;
		if(con!=null && aafConClass!=null && aafConClass.isAssignableFrom(con.getClass())) {
			aafcon = con;
		} else if(lur != null) {
			Field f = null;
			try {
				f = lur.getClass().getField("aaf");
				aafcon = f.get(lur);
			} catch (Exception nsfe) {
			}
		}
	
		
		boolean hasDirectAAF = hasDirect("DirectAAFLur",additionalTafLurs);
		// IMPORTANT!  Don't attempt to load AAF Connector if there is no AAF URL
		String aafURL = access.getProperty(AAF_URL,null);
		if(!hasDirectAAF && aafcon==null && aafURL!=null) {
			aafcon = loadAAFConnector(si, aafURL);	
		}
		
		HttpTaf taf;
		// Setup Host, in case Network reports an unusable Hostname (i.e. VTiers, VPNs, etc)
		String hostname = logProp(access, HOSTNAME,null);
		if(hostname==null) {
			try {
				hostname = InetAddress.getLocalHost().getHostName();
			} catch (UnknownHostException e1) {
				throw new CadiException("Unable to determine Hostname",e1);
			}
		}
		
		access.log(Level.INIT, "Hostname set to",hostname);
		// Get appropriate TAFs
		ArrayList<HttpTaf> htlist = new ArrayList<HttpTaf>();

		/////////////////////////////////////////////////////
		// Add a Denial of Service TAF
		// Note: how IPs and IDs are added are up to service type.
		// They call "DenialOfServiceTaf.denyIP(String) or denyID(String)
		/////////////////////////////////////////////////////
		htlist.add(new DenialOfServiceTaf(access));

		/////////////////////////////////////////////////////
		// Configure Client Cert TAF
		/////////////////////////////////////////////////////
		
		String truststore = logProp(access, CADI_TRUSTSTORE,null);
		if(truststore!=null) {
			String truststore_pwd = access.getProperty(CADI_TRUSTSTORE_PASSWORD,null);
			if(truststore_pwd!=null) {
				if(truststore_pwd.startsWith(Symm.ENC)) {
					try {
						truststore_pwd = access.decrypt(truststore_pwd,false);
					} catch (IOException e) {
						throw new CadiException(CADI_TRUSTSTORE_PASSWORD + " cannot be decrypted",e);
					}
				}
				try {
					htlist.add(new X509Taf(access,lur));
					access.log(Level.INIT,"Certificate Authorization enabled");
				} catch (SecurityException e) {
					access.log(Level.INIT,"AAFListedCertIdentity cannot be instantiated. Certificate Authorization is now disabled",e);
				} catch (IllegalArgumentException e) {
					access.log(Level.INIT,"AAFListedCertIdentity cannot be instantiated. Certificate Authorization is now disabled",e);
				} catch (CertificateException e) {
					access.log(Level.INIT,"Certificate Authorization failed, it is disabled",e);
				} catch (NoSuchAlgorithmException e) {
					access.log(Level.INIT,"Certificate Authorization failed, wrong Security Algorithm",e);
				}
			}
		} else {
			access.log(Level.INIT,"Certificate Authorization not enabled");
		}
		
		/////////////////////////////////////////////////////
		// Configure Basic Auth (local content)
		/////////////////////////////////////////////////////
		boolean hasOAuthDirectTAF = hasDirect("DirectOAuthTAF", additionalTafLurs);
		String basic_realm = logProp(access, BASIC_REALM,null);
		String aafCleanup = logProp(access, AAF_USER_EXPIRES,AAF_USER_EXPIRES_DEF); // Default is 10 mins
		long userExp = Long.parseLong(aafCleanup);
		boolean basic_warn = "TRUE".equals(access.getProperty(BASIC_WARN,"FALSE"));

		if(!hasDirectAAF) {
			HttpTaf aaftaf=null;
			if(!hasOAuthDirectTAF) {
				if(basic_realm!=null) {
					@SuppressWarnings("unchecked")
					Class<HttpTaf> obasicCls = (Class<HttpTaf>)loadClass(access,"org.osaaf.cadi.obasic.OBasicHttpTaf");
					if(obasicCls!=null) {
						try {
							String tokenurl = logProp(access,Config.AAF_OAUTH2_TOKEN_URL, null);
							String introspecturl = logProp(access,Config.AAF_OAUTH2_INTROSPECT_URL, null);
							if(tokenurl==null || introspecturl==null) {
								access.log(Level.INIT,"Both tokenurl and introspecturl are required. Oauth Authorization is disabled.");
							}
							Constructor<HttpTaf> obasicConst = obasicCls.getConstructor(PropAccess.class,String.class, String.class, String.class);
							htlist.add(obasicConst.newInstance(access,basic_realm,tokenurl,introspecturl));
							access.log(Level.INIT,"Oauth supported Basic Authorization is enabled");
						} catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
						}
					} else if(up!=null) {
						access.log(Level.INIT,"Basic Authorization is enabled using realm",basic_realm);
						// Allow warning about insecure channel to be turned off
						if(!basic_warn)access.log(Level.INIT,"WARNING! The basic_warn property has been set to false.",
								" There will be no additional warning if Basic Auth is used on an insecure channel"
								);
						htlist.add(new BasicHttpTaf(access, up, basic_realm, userExp, basic_warn));
						access.log(Level.INIT,"Basic Authorization is enabled");
					}
				} else {
					access.log(Level.INIT,"Local Basic Authorization is disabled.  Enable by setting basic_realm=<appropriate realm, i.e. my.att.com>");
				}
			
				/////////////////////////////////////////////////////
				// Configure AAF Driven Basic Auth
				/////////////////////////////////////////////////////
				if(aafcon==null) {
					access.log(Level.INIT,"AAF Connection (AAFcon) is null.  Cannot create an AAF TAF");
				} else if(aafURL==null) {
					access.log(Level.INIT,"No AAF URL in properties, Cannot create an AAF TAF");
				} else {// There's an AAF_URL... try to configure an AAF 
					String aafTafClassName = logProp(access, AAF_TAF_CLASS,AAF_TAF_CLASS_DEF);
					// Only 2.0 available at this time
					if(AAF_TAF_CLASS_DEF.equals(aafTafClassName)) { 
						try {
							Class<?> aafTafClass = loadClass(access,aafTafClassName);
							if(aafTafClass!=null) {
								Constructor<?> cstr = aafTafClass.getConstructor(Connector.class,boolean.class,AbsUserCache.class);
								if(cstr!=null) {
									if(lur instanceof AbsUserCache) {
										aaftaf = (HttpTaf)cstr.newInstance(aafcon,basic_warn,lur);
									} else {
										cstr = aafTafClass.getConstructor(Connector.class,boolean.class);
										if(cstr!=null) {
											aaftaf = (HttpTaf)cstr.newInstance(aafcon,basic_warn);
										}
									}
									if(aaftaf==null) {
										access.log(Level.INIT,"ERROR! AAF TAF Failed construction.  NOT Configured");
									} else {
										access.log(Level.INIT,"AAF TAF Configured to ",aafURL);
										// Note: will add later, after all others configured
									}
								}
							} else {
								access.log(Level.INIT, "There is no AAF TAF class available: %s. AAF TAF not configured.",aafTafClassName);
							}
						} catch(Exception e) {
							access.log(Level.INIT,"ERROR! AAF TAF Failed construction.  NOT Configured",e);
						}
					}
				}
			}
			
			/////////////////////////////////////////////////////
			// Configure OAuth TAF
			/////////////////////////////////////////////////////
			if(!hasOAuthDirectTAF) {
				String oauth_token_url = logProp(access,Config.AAF_OAUTH2_TOKEN_URL,null);
				Class<?> oadtClss;
				try {
					oadtClss = Class.forName("org.osaaf.authz.oauth.OAuthDirectTAF");
				} catch (ClassNotFoundException e1) {
					oadtClss = null;
				}
				if(additionalTafLurs!=null && additionalTafLurs.length>0 && (oadtClss!=null && additionalTafLurs[0].getClass().isAssignableFrom(oadtClss))) {
					htlist.add((HttpTaf)additionalTafLurs[0]);
					String array[] = new String[additionalTafLurs.length-1];
					if(array.length>0) {
						System.arraycopy(htlist, 1, array, 0, array.length);
					}
					additionalTafLurs = array;
					access.log(Level.INIT,"OAuth2 Direct is enabled");
				} else if(oauth_token_url!=null) {
					String oauth_introspect_url = logProp(access,Config.AAF_OAUTH2_INTROSPECT_URL,null);
					@SuppressWarnings("unchecked")
					Class<HttpTaf> oaTCls = (Class<HttpTaf>)loadClass(access,"org.osaaf.cadi.oauth.OAuth2HttpTaf");
					if(oaTCls!=null) {
						Class<?> oaTTmgrCls = loadClass(access, "org.osaaf.cadi.oauth.TokenMgr");
						if(oaTTmgrCls!=null) {
							try {
								Method oaTTmgrGI = oaTTmgrCls.getMethod("getInstance",PropAccess.class,String.class,String.class);
								Object oaTTmgr = oaTTmgrGI.invoke(null /*this is static method*/,access,oauth_token_url,oauth_introspect_url);
								Constructor<HttpTaf> oaTConst = oaTCls.getConstructor(Access.class,oaTTmgrCls);
								htlist.add(oaTConst.newInstance(access,oaTTmgr));
								access.log(Level.INIT,"OAuth2 TAF is enabled");
							} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | InstantiationException e) {
								access.log(Level.INIT,"OAuth2HttpTaf cannot be instantiated. OAuth2 is disabled",e);
							}
						}
					}
				} else {
					access.log(Level.INIT,"OAuth TAF is not configured");
				}
			}
	
			/////////////////////////////////////////////////////
			// Adding BasicAuth (AAF) last, after other primary Cookie Based
			// Needs to be before Cert... see below
			/////////////////////////////////////////////////////
			if(aaftaf!=null) {
				htlist.add(aaftaf);
			}
		}	

		/////////////////////////////////////////////////////
		// Any Additional Lurs passed in Constructor
		/////////////////////////////////////////////////////
		if(additionalTafLurs!=null) {
			for(Object additional : additionalTafLurs) {
				if(additional instanceof HttpTaf) {
					htlist.add((HttpTaf)additional);
					access.printf(Level.INIT,"%s Authentication is enabled",additional.getClass().getSimpleName());
				} else if(hasOAuthDirectTAF) {
					Class<?> daupCls;
					try {
						daupCls = Class.forName("org.onap.aaf.auth.direct.DirectAAFUserPass");
					} catch (ClassNotFoundException e) {
						daupCls = null;
					}
					if(daupCls != null && additional.getClass().isAssignableFrom(daupCls)) {
						htlist.add(new BasicHttpTaf(access, (CredVal)additional , basic_realm, userExp, basic_warn));
						access.printf(Level.INIT,"Direct BasicAuth Authentication is enabled",additional.getClass().getSimpleName());
					}
				}
			}
		}
		
		/////////////////////////////////////////////////////
		// Create EpiTaf from configured TAFs
		/////////////////////////////////////////////////////
		if(htlist.size()==1) {
			// just return the one
			taf = htlist.get(0);
		} else {
			HttpTaf[] htarray = new HttpTaf[htlist.size()];
			htlist.toArray(htarray);
			Locator<URI> locator = loadLocator(si, logProp(access, AAF_LOCATE_URL, null));
			
			taf = new HttpEpiTaf(access,locator, tc, htarray); // ok to pass locator == null
			String level = logProp(access, CADI_LOGLEVEL, null);
			if(level!=null) {
				access.setLogLevel(Level.valueOf(level));
			}
		}
		
		return taf;
	}
	
	public static String logProp(Access access,String tag, String def) {
		String rv = access.getProperty(tag, def);
		if(rv == null) {
			access.log(Level.INIT,tag,"is not set");
		} else {
			access.log(Level.INIT,tag,"is set to",rv);
		}
		return rv;
	}
	
	public static Lur configLur(SecurityInfoC<HttpURLConnection> si, Connector con, Object ... additionalTafLurs) throws CadiException {
		Access access = si.access;
		List<Lur> lurs = new ArrayList<Lur>();
		
		/////////////////////////////////////////////////////
		// Configure a Local Property Based RBAC/LUR
		/////////////////////////////////////////////////////
		try {
			String users = access.getProperty(USERS,null);
			String groups = access.getProperty(GROUPS,null);

			if(groups!=null || users!=null) {
				LocalLur ll;
				lurs.add(ll = new LocalLur(access, users, groups)); // note b64==null is ok.. just means no encryption.
				
				String writeto = access.getProperty(WRITE_TO,null);
				if(writeto!=null) {
					String msg = UsersDump.updateUsers(writeto, ll);
					if(msg!=null) access.log(Level.INIT,"ERROR! Error Updating ",writeto,"with roles and users:",msg);
				}
			}
		} catch (IOException e) {
			throw new CadiException(e);
		}

		/////////////////////////////////////////////////////
		// Configure the OAuth Lur (if any)
		/////////////////////////////////////////////////////
		String token_url = logProp(access,AAF_OAUTH2_TOKEN_URL, null);
		String introspect_url = logProp(access,AAF_OAUTH2_INTROSPECT_URL, null);
		if(token_url!=null && introspect_url !=null) {
			try {
				Class<?> olurCls = loadClass(access, "org.osaaf.cadi.olur.OLur");
				if(olurCls!=null) {
					Constructor<?> olurCnst = olurCls.getConstructor(PropAccess.class,String.class,String.class);
					Lur olur = (Lur)olurCnst.newInstance(access,token_url,introspect_url);
					lurs.add(olur);
					access.log(Level.INIT, "OAuth2 LUR enabled");
				} else {
					access.log(Level.INIT,"AAF/OAuth LUR plugin is not available.");
				}
			} catch (NoSuchMethodException| SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				access.log(e,"AAF/OAuth LUR could not be constructed with given Constructors.");
			} 
		} else {
			access.log(Level.INIT, "OAuth2 Lur disabled");
		}

		if(con!=null) { // try to reutilize connector
			lurs.add(con.newLur());
		} else { 
			/////////////////////////////////////////////////////
			// Configure the AAF Lur (if any)
			/////////////////////////////////////////////////////
			String aafURL = logProp(access,AAF_URL,null); // Trigger Property
			String aaf_env = access.getProperty(AAF_ENV,null);
			if(aaf_env == null && aafURL!=null && access instanceof PropAccess) { // set AAF_ENV from AAF_URL
				int ec = aafURL.indexOf("envContext=");
				if(ec>0) {
					ec += 11; // length of envContext=
					int slash = aafURL.indexOf('/', ec);
					if(slash>0) {
						aaf_env = aafURL.substring(ec, slash);
						((PropAccess)access).setProperty(AAF_ENV, aaf_env);
						access.printf(Level.INIT, "Setting aaf_env to %s from aaf_url value",aaf_env);
					}
				}
			}

			// Don't configure AAF if it is using DirectAccess
			if(!hasDirect("DirectAAFLur",additionalTafLurs)) {
				if(aafURL==null) {
					access.log(Level.INIT,"No AAF LUR properties, AAF will not be loaded");
				} else {// There's an AAF_URL... try to configure an AAF
					String aafLurClassStr = logProp(access,AAF_LUR_CLASS,"org.osaaf.cadi.aaf.v2_0.AAFLurPerm");
					////////////AAF Lur 2.0 /////////////
					if(aafLurClassStr!=null && aafLurClassStr.startsWith("org.osaaf.cadi.aaf.v2_0")) { 
						try {
							Object aafcon = loadAAFConnector(si, aafURL);
							if(aafcon==null) {
								access.log(Level.INIT,"AAF LUR class,",aafLurClassStr,"cannot be constructed without valid AAFCon object.");
							} else {
								Class<?> aafAbsAAFCon = loadClass(access, "org.osaaf.cadi.aaf.v2_0.AAFCon");
								if(aafAbsAAFCon!=null) {
									Method mNewLur = aafAbsAAFCon.getMethod("newLur");
									Object aaflur = mNewLur.invoke(aafcon);
				
									if(aaflur==null) {
										access.log(Level.INIT,"ERROR! AAF LUR Failed construction.  NOT Configured");
									} else {
										access.log(Level.INIT,"AAF LUR Configured to ",aafURL);
										lurs.add((Lur)aaflur);
										String debugIDs = logProp(access,Config.AAF_DEBUG_IDS, null);
										if(debugIDs !=null && aaflur instanceof CachingLur) {
											((CachingLur<?>)aaflur).setDebug(debugIDs);
										}
									}
								}
							}
						} catch (Exception e) {
							access.log(e,"AAF LUR class,",aafLurClassStr,"could not be constructed with given Constructors.");
						}
					} 
				}
			}
		}

		/////////////////////////////////////////////////////
		// Any Additional passed in Constructor
		/////////////////////////////////////////////////////
		if(additionalTafLurs!=null) {
			for(Object additional : additionalTafLurs) {
				if(additional instanceof Lur) {
					lurs.add((Lur)additional);
					access.log(Level.INIT, additional);
				}
			}
		}

		/////////////////////////////////////////////////////
		// Return a Lur based on how many there are... 
		/////////////////////////////////////////////////////
		switch(lurs.size()) {
			case 0: 
				access.log(Level.INIT,"WARNING! No CADI LURs configured");
				// Return a NULL Lur that does nothing.
				return new NullLur();
			case 1:
				return lurs.get(0); // Only one, just return it, save processing
			default:
				// Multiple Lurs, use EpiLUR to handle
				Lur[] la = new Lur[lurs.size()];
				lurs.toArray(la);
				return new EpiLur(la);
		}
	}
	
	private static boolean hasDirect(String simpleClassName, Object[] additionalTafLurs) {
		if(additionalTafLurs!=null) {
			for(Object tf : additionalTafLurs) {
				if(tf.getClass().getSimpleName().equals(simpleClassName)) {
					return true;
				}
			}
		}
		return false;
	}

	private static final String COM_ATT_CADI_AAF_V2_0_AAF_CON_HTTP = "org.osaaf.cadi.aaf.v2_0.AAFConHttp";
	public static Object loadAAFConnector(SecurityInfoC<HttpURLConnection> si, String aafURL) {
		Access access = si.access;
		Object aafcon = null;
		Class<?> aafConClass = null;

		try {
			if(aafURL!=null) {
				String aafConnector = access.getProperty(AAF_CONNECTOR_CLASS, COM_ATT_CADI_AAF_V2_0_AAF_CON_HTTP);
			if(COM_ATT_CADI_AAF_V2_0_AAF_CON_HTTP.equals(aafConnector)) {
					aafConClass = loadClass(access, COM_ATT_CADI_AAF_V2_0_AAF_CON_HTTP);
					if(aafConClass!=null) {
						for(Constructor<?> c : aafConClass.getConstructors()) {
							List<Object> lo = new ArrayList<Object>();
							for(Class<?> pc : c.getParameterTypes()) {
								if(pc.equals(PropAccess.class)) {
									lo.add(access);
								} else if(pc.equals(Locator.class)) {
									lo.add(loadLocator(si, aafURL));
								} else {
									continue;
								}
							}
							if(c.getParameterTypes().length!=lo.size()) {
								continue; // back to another Constructor
							} else {
								aafcon = c.newInstance(lo.toArray());
							}
							break;
						}
					}
				}
				if(aafcon!=null) {
					String mechid = logProp(access,Config.AAF_APPID, null);
					String pass = access.getProperty(Config.AAF_APPPASS, null);
					if(mechid!=null && pass!=null) {
						try {
							Method basicAuth = aafConClass.getMethod("basicAuth", String.class, String.class);
							basicAuth.invoke(aafcon, mechid,pass);
						} catch (NoSuchMethodException nsme) {
							// it's ok, don't use
						}
					}
				}
			}
		} catch (Exception e) {
			access.log(e,"AAF Connector could not be constructed with given Constructors.");
		}
		
		return aafcon;
	}

	public static Class<?> loadClass(Access access, String className) {
		Class<?> cls=null;
		try {
			cls = access.classLoader().loadClass(className);
		} catch (ClassNotFoundException cnfe) {
			try {
				cls = access.getClass().getClassLoader().loadClass(className);
			} catch (ClassNotFoundException cnfe2) {
				// just return null
			}
		}
		return cls;
	}


	@SuppressWarnings("unchecked")
	public static Locator<URI> loadLocator(SecurityInfoC<HttpURLConnection> si, final String _url) {
		Access access = si.access;
		Locator<URI> locator = null;
		if(_url==null) {
			access.log(Level.INIT,"No URL passed to 'loadLocator'. Disabled");
		} else {
			String url = _url, replacement;
			int idxAAF_LOCATE_URL;
			if((idxAAF_LOCATE_URL=_url.indexOf(AAF_LOCATE_URL_TAG))>0 && ((replacement=access.getProperty(AAF_LOCATE_URL, null))!=null)) {
				url = replacement + "/locate" + _url.substring(idxAAF_LOCATE_URL+AAF_LOCATE_URL_TAG.length());
			}
	
			try {
				Class<?> lcls = loadClass(access,"org.onap.aaf.cadi.aaf.v2_0.AAFLocator");
				if(lcls==null) {
					throw new CadiException("Need to include aaf-cadi-aaf jar for AAFLocator");
				}
				// First check for preloaded
				try {
					Method meth = lcls.getMethod("create",String.class);
					locator = (Locator<URI>)meth.invoke(null,url);
				} catch (Exception e) {
					locator = null;
				}
				if(locator==null) {
					URI locatorURI = new URI(url);
					Constructor<?> cnst = lcls.getConstructor(new Class[] {SecurityInfoC.class,URI.class});
					locator = (Locator<URI>)cnst.newInstance(new Object[] {si,locatorURI});
					int port = locatorURI.getPort();
					String portS = port<0?"":(":"+locatorURI.getPort());
					
					access.log(Level.INFO, "AAFLocator enabled using " + locatorURI.getScheme() +"://"+locatorURI.getHost() + portS);
				} else {
					access.log(Level.INFO, "AAFLocator enabled using preloaded " + locator.getClass().getSimpleName());
				}
			} catch (InvocationTargetException e) {
				access.log(Level.INIT,e.getTargetException().getMessage(),"AAFLocator for",url,"could not be created.",e);
			} catch (Exception e) {
				access.log(Level.INIT,"AAFLocator for",url,"could not be created.",e);
			}
		}
		return locator;
	}

	// Set by CSP, or is hostname.
	public static String getDefaultRealm() {
		return defaultRealm;
	}

}

