/**
r * ============LICENSE_START====================================================
 * org.onap.aaf
 * ===========================================================================
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 * ===========================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 *      http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END====================================================
 *
 */

package org.onap.aaf.cadi.aaf.v2_0;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;

import org.onap.aaf.cadi.Access;
import org.onap.aaf.cadi.CadiException;
import org.onap.aaf.cadi.Locator;
import org.onap.aaf.cadi.Locator.Item;
import org.onap.aaf.cadi.LocatorException;
import org.onap.aaf.cadi.SecuritySetter;
import org.onap.aaf.cadi.client.AbsTransferSS;
import org.onap.aaf.cadi.client.Rcli;
import org.onap.aaf.cadi.client.Retryable;
import org.onap.aaf.cadi.config.Config;
import org.onap.aaf.cadi.config.SecurityInfoC;
import org.onap.aaf.cadi.http.HBasicAuthSS;
import org.onap.aaf.cadi.http.HMangr;
import org.onap.aaf.cadi.http.HRcli;
import org.onap.aaf.cadi.http.HTokenSS;
import org.onap.aaf.cadi.http.HTransferSS;
import org.onap.aaf.cadi.http.HX509SS;
import org.onap.aaf.cadi.principal.BasicPrincipal;
import org.onap.aaf.cadi.principal.TaggedPrincipal;
import org.onap.aaf.misc.env.APIException;

public class AAFConHttp extends AAFCon<HttpURLConnection> {
    private final HMangr hman;

    public AAFConHttp(Access access) throws CadiException, LocatorException {
        super(access,Config.AAF_URL,SecurityInfoC.instance(access, HttpURLConnection.class));
        hman = new HMangr(access,Config.loadLocator(si, access.getProperty(Config.AAF_URL,null)));
    }

    protected SecuritySetter<HttpURLConnection> bestSS(SecurityInfoC<HttpURLConnection> si) throws CadiException {
        return si.defSS;
    }

    public AAFConHttp(Access access, String tag) throws CadiException, LocatorException {
        super(access,tag,SecurityInfoC.instance(access, HttpURLConnection.class));
        hman = new HMangr(access,Config.loadLocator(si, access.getProperty(tag,tag/*try the content itself*/)));
    }

    public AAFConHttp(Access access, String urlTag, SecurityInfoC<HttpURLConnection> si) throws CadiException, LocatorException {
        super(access,urlTag,si);
        hman = new HMangr(access,Config.loadLocator(si, access.getProperty(urlTag,null)));
    }

    public AAFConHttp(Access access, Locator<URI> locator) throws CadiException, LocatorException {
        super(access,Config.AAF_URL,SecurityInfoC.instance(access, HttpURLConnection.class));
        hman = new HMangr(access,locator);
    }

    public AAFConHttp(Access access, Locator<URI> locator, SecurityInfoC<HttpURLConnection> si) throws CadiException, LocatorException, APIException {
        super(access,Config.AAF_URL,si);
        hman = new HMangr(access,locator);
    }

    public AAFConHttp(Access access, Locator<URI> locator, SecurityInfoC<HttpURLConnection> si, String tag) throws CadiException, LocatorException, APIException {
        super(access,tag,si);
        hman = new HMangr(access, locator);
    }

    private AAFConHttp(AAFCon<HttpURLConnection> aafcon, String url) throws LocatorException {
        super(aafcon);
        si=aafcon.si;
        hman = new HMangr(aafcon.access,Config.loadLocator(si, url));
    }

    @Override
    public AAFCon<HttpURLConnection> clone(String url) throws LocatorException {
        return new AAFConHttp(this,url);
    }

    /* (non-Javadoc)
     * @see org.onap.aaf.cadi.aaf.v2_0.AAFCon#basicAuth(java.lang.String, java.lang.String)
     */
    @Override
    public SecuritySetter<HttpURLConnection> basicAuth(String user, String password) throws CadiException {
        if (password.startsWith("enc:")) {
            try {
                password = access.decrypt(password, true);
            } catch (IOException e) {
                throw new CadiException("Error decrypting password",e);
            }
        }
        try {
            return new HBasicAuthSS(si,user,password);
        } catch (IOException e) {
            throw new CadiException("Error creating HBasicAuthSS",e);
        }
    }

    public SecuritySetter<HttpURLConnection> x509Alias(String alias) throws CadiException {
        try {
            return set(new HX509SS(alias,si));
        } catch (Exception e) {
            throw new CadiException("Error creating X509SS",e);
        }
    }

    /* (non-Javadoc)
     * @see org.onap.aaf.cadi.aaf.v2_0.AAFCon#rclient(java.net.URI, org.onap.aaf.cadi.SecuritySetter)
     */
    @Override
    protected Rcli<HttpURLConnection> rclient(URI ignoredURI, SecuritySetter<HttpURLConnection> ss) throws CadiException {
        if (hman.loc==null) {
            throw new CadiException("No Locator set in AAFConHttp"); 
        }
        try {
            return new HRcli(hman, hman.loc.best() ,ss);
        } catch (Exception e) {
            throw new CadiException(e);
        }
    }

    @Override
    public Rcli<HttpURLConnection> rclient(Locator<URI> loc, SecuritySetter<HttpURLConnection> ss) throws CadiException {
        try {
            HMangr newHMan = new HMangr(access, loc);
            return new HRcli(newHMan,newHMan.loc.best(),ss);
        } catch (Exception e) {
            throw new CadiException(e);
        }
    }
    @Override
    public AbsTransferSS<HttpURLConnection> transferSS(TaggedPrincipal principal) {
        return new HTransferSS(principal, app,si);
    }

    /* (non-Javadoc)
     * @see org.onap.aaf.cadi.aaf.v2_0.AAFCon#basicAuthSS(java.security.Principal)
     */
    @Override
    public SecuritySetter<HttpURLConnection> basicAuthSS(BasicPrincipal principal) throws CadiException {
        try {
            return new HBasicAuthSS(principal,si);
        } catch (IOException e) {
            throw new CadiException("Error creating HBasicAuthSS",e);
        }
    }

    @Override
    public SecuritySetter<HttpURLConnection> tokenSS(final String client_id, final String accessToken) throws CadiException {
        try {
            return new HTokenSS(si, client_id, accessToken);
        } catch (IOException e) {
            throw new CadiException(e);
        }
    }

    public HMangr hman() {
        return hman;
    }

    @Override
    public <RET> RET best(Retryable<RET> retryable) throws LocatorException, CadiException, APIException {
        return hman.best(si.defSS, retryable);
    }

    /* (non-Javadoc)
     * @see org.onap.aaf.cadi.aaf.v2_0.AAFCon#bestForUser(org.onap.aaf.cadi.SecuritySetter, org.onap.aaf.cadi.client.Retryable)
     */
    @Override
    public <RET> RET bestForUser(GetSetter getSetter, Retryable<RET> retryable) throws LocatorException, CadiException, APIException {
        return hman.best(getSetter.get(this), retryable);
    }

    /* (non-Javadoc)
     * @see org.onap.aaf.cadi.aaf.v2_0.AAFCon#initURI()
     */
    @Override
    protected URI initURI() {
        try {
            Item item = hman.loc.best();
            if (item!=null) {
                return hman.loc.get(item);
            }
        } catch (LocatorException e) {
            access.log(e, "Error in AAFConHttp obtaining initial URI");
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.onap.aaf.cadi.aaf.v2_0.AAFCon#setInitURI(java.lang.String)
     */
    @Override
    protected void setInitURI(String uriString) {
        // Using Locator, not URLString, which is mostly for DME2
    }

}
