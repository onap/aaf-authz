/**
 * ============LICENSE_START====================================================
 * org.onap.aaf
 * ===========================================================================
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2019 IBM.
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

package org.onap.aaf.auth.cm.service;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.onap.aaf.auth.cm.AAF_CM;
import org.onap.aaf.auth.cm.ca.CA;
import org.onap.aaf.auth.cm.ca.X509andChain;
import org.onap.aaf.auth.cm.cert.BCFactory;
import org.onap.aaf.auth.cm.cert.CSRMeta;
import org.onap.aaf.auth.cm.data.CertDrop;
import org.onap.aaf.auth.cm.data.CertRenew;
import org.onap.aaf.auth.cm.data.CertReq;
import org.onap.aaf.auth.cm.data.CertResp;
import org.onap.aaf.auth.cm.util.ArtiDaoDataAndResultPOJO;
import org.onap.aaf.auth.cm.util.InetAddressAndResultPOJO;
import org.onap.aaf.auth.cm.util.StringAndListStringAndResultPOJO;
import org.onap.aaf.auth.cm.validation.CertmanValidator;
import org.onap.aaf.auth.common.Define;
import org.onap.aaf.auth.dao.CassAccess;
import org.onap.aaf.auth.dao.cass.ArtiDAO;
import org.onap.aaf.auth.dao.cass.CacheInfoDAO;
import org.onap.aaf.auth.dao.cass.CertDAO;
import org.onap.aaf.auth.dao.cass.CertDAO.Data;
import org.onap.aaf.auth.dao.cass.CredDAO;
import org.onap.aaf.auth.dao.cass.HistoryDAO;
import org.onap.aaf.auth.dao.cass.Status;
import org.onap.aaf.auth.dao.hl.Question;
import org.onap.aaf.auth.env.AuthzTrans;
import org.onap.aaf.auth.layer.Result;
import org.onap.aaf.auth.org.Organization;
import org.onap.aaf.auth.org.Organization.Identity;
import org.onap.aaf.auth.org.OrganizationException;
import org.onap.aaf.cadi.Access.Level;
import org.onap.aaf.cadi.Hash;
import org.onap.aaf.cadi.Permission;
import org.onap.aaf.cadi.aaf.AAFPermission;
import org.onap.aaf.cadi.config.Config;
import org.onap.aaf.cadi.configure.CertException;
import org.onap.aaf.cadi.configure.Factory;
import org.onap.aaf.cadi.util.FQI;
import org.onap.aaf.misc.env.APIException;
import org.onap.aaf.misc.env.util.Chrono;

public class CMService {
    // If we add more CAs, may want to parameterize
    private static final int STD_RENEWAL = 30;
    private static final int MAX_RENEWAL = 60;
    private static final int MIN_RENEWAL = 10;
    // Limit total requests
    private static final int MAX_X509s = 200; // Need a "LIMIT Exception" DB.
    private static final String MAX_X509S_TAG = "cm_max_x509s"; // be able to adjust limit in future

    public static final String REQUEST = "request";
    public static final String IGNORE_IPS = "ignoreIPs";
    public static final String RENEW = "renew";
    public static final String DROP = "drop";
    public static final String DOMAIN = "domain";
    public static final String DYNAMIC_SANS="dynamic_sans";

    private static final String CERTMAN = "certman";
    private static final String ACCESS = "access";

    private static final String[] NO_NOTES = new String[0];
    private final Permission root_read_permission;
	private final String aaf_ns;

    private final CertDAO certDAO;
    private final CredDAO credDAO;
    private final ArtiDAO artiDAO;
    private AAF_CM certManager;
    private Boolean allowIgnoreIPs;
    private AAFPermission limitOverridePerm;
    private int max_509s;

    // @SuppressWarnings("unchecked")
    public CMService(final AuthzTrans trans, AAF_CM certman) throws APIException, IOException {
        // Jonathan 4/2015 SessionFilter unneeded... DataStax already deals with
        // Multithreading well

        HistoryDAO hd = new HistoryDAO(trans, certman.cluster, CassAccess.KEYSPACE);
        CacheInfoDAO cid = new CacheInfoDAO(trans, hd);
        certDAO = new CertDAO(trans, hd, cid);
        credDAO = new CredDAO(trans, hd, cid);
        artiDAO = new ArtiDAO(trans, hd, cid);

        this.certManager = certman;

        aaf_ns = trans.getProperty(Config.AAF_ROOT_NS, Config.AAF_ROOT_NS_DEF);
        root_read_permission=new AAFPermission(
                aaf_ns,
                ACCESS,
                "*",
                "read"
        );
        try {
            max_509s = Integer.parseInt(trans.env().getProperty(MAX_X509S_TAG,Integer.toString(MAX_X509s)));
        } catch (Exception e) {
            trans.env().log(e, "");
            max_509s = MAX_X509s;
        }
        limitOverridePerm = new AAFPermission(Define.ROOT_NS(),"certman","quantity","override");
        allowIgnoreIPs = Boolean.valueOf(certman.access.getProperty(Config.CM_ALLOW_IGNORE_IPS, "false"));
        if(allowIgnoreIPs) {
            trans.env().access().log(Level.INIT, "Allowing DNS Evaluation to be turned off with <ns>.certman|<ca name>|"+IGNORE_IPS);
        }
    }

    private boolean skipReverseDNS(String remoteAddr) {
        List<String> requesterWhitelist = createWhitelist();
        return requesterWhitelist.contains(remoteAddr);
    }

    private List<String> createWhitelist() {
        ArrayList<String> requesterWhitelist = new ArrayList<>();
        String rawWhitelist = certManager.access.getProperty(Config.CM_REQUESTER_WHITELIST, null);
        if (rawWhitelist != null && !rawWhitelist.isEmpty()) {
            requesterWhitelist.addAll(Arrays.asList(rawWhitelist.split(",")));
        }
        return requesterWhitelist;
    }

    private boolean wereMachinesPassedIntoRequest (List<String> fqdns) {
        return !fqdns.isEmpty();
    }

    private boolean doesRequesterHaveChangePermissionInNamespaceRequested (String mechNS) {
        // Policy 6: Requester must be granted Change permission in Namespace requested
        // TODO this seems to have never been implemented and no such check is performed
        return mechNS != null;
    }

    private List<String> populateFQDNS(List<String> fqdns, String key, AuthzTrans trans, CA ca) {
        // Note: Many Cert Impls require FQDN in "CN=" to be in the SANS as well.  Therefore, the "fqdn" variable
        // includes main ID plus ADDITIONAL SANS at all times.
        // TODO this logic is muddled and the if case appears to never be reached, and if it did wouldn't work
        // TODO anyway. Under a do no harm approach however, it was left as is.
        if(fqdns.isEmpty()) {
            fqdns = new ArrayList<>();
            fqdns.add(key);
        } else {
            // Only Template or Dynamic permitted to pass in FQDNs
            if (fqdns.get(0).startsWith("*")) { // Domain set
                if (!trans.fish(new AAFPermission(null, ca.getPermType(), ca.getName(), DOMAIN))) {
                    return null;
                }
            }
        }
        return fqdns;
    }

    private StringAndListStringAndResultPOJO setSponsor(AuthzTrans trans, List<String> fqdns, boolean domain_based,
                                                        boolean ignoreIPs, boolean skipReverseDNS, String mechid,
                                                        boolean dynamic_sans, String key, String mechNS, CA ca)
            throws Exception {
        StringAndListStringAndResultPOJO stringAndListStringOrResult = new StringAndListStringAndResultPOJO();
        stringAndListStringOrResult.setResult(null);
        stringAndListStringOrResult.setString(null);
        stringAndListStringOrResult.setStringList(null);

        Organization org = trans.org();

        InetAddressAndResultPOJO primaryOrResult = findInetAddress(fqdns, domain_based, ignoreIPs, skipReverseDNS, trans);
        InetAddress primary = primaryOrResult.getInetAddress();
        if (primary == null && primaryOrResult.getResult() != null) {
            stringAndListStringOrResult.setResult(primaryOrResult.getResult());
            return stringAndListStringOrResult;
        }

        final String host = setHost(ignoreIPs, skipReverseDNS, fqdns, primary);
        if (host == null) {
            stringAndListStringOrResult.setResult(Result.err(Result.ERR_Denied,
                    "Request not made from matching IP (%s)", fqdns.get(0)));
            return stringAndListStringOrResult;
        }

        ArtiDaoDataAndResultPOJO dataOrResult = createAdd(trans, mechid, host, dynamic_sans, key, domain_based, fqdns);

        ArtiDAO.Data add = dataOrResult.getData();
        if (add == null) {
            stringAndListStringOrResult.setResult(dataOrResult.getResult());
            return stringAndListStringOrResult;
        }

        fqdns = addArtifactListedFQDNs(dynamic_sans, add, fqdns);

        if (!isConfigNotYetExpired(add)) {
            stringAndListStringOrResult.setResult(Result.err(Result.ERR_Policy, "Configuration for %s %s is expired %s",
                    add.mechid, add.machine, Chrono.dateFmt.format(add.expires)));
            return stringAndListStringOrResult;
        }

        Identity muser = org.getIdentity(trans, add.mechid);
        if (!isMechIdCurrent(muser)) {
            stringAndListStringOrResult.setResult(Result.err(Result.ERR_Policy, "AppID '%s' must exist in %s",
                    add.mechid,org.getName()));
            return stringAndListStringOrResult;
        }

        Identity ouser = muser.responsibleTo();
        if (!isSponsorCurrent(ouser)) {
            stringAndListStringOrResult.setResult(Result.err(Result.ERR_Policy, "%s does not have a current sponsor at %s",
                    add.mechid, org.getName()));
            return stringAndListStringOrResult;
        } else if (!isSponsorResponsibleForMechId(ouser)) {
            stringAndListStringOrResult.setResult(Result.err(Result.ERR_Policy, "%s reports that %s cannot be responsible for %s",
                    org.getName(), trans.user()));
            return stringAndListStringOrResult;
        }

        // Set Email from most current Sponsor
        String email = ouser.email();

        keepArtifactDataCurrent(ouser, add, trans);

        if (!isCallerMechIdOrHasPermissions(trans, mechid, mechNS, ca)) {
            stringAndListStringOrResult.setResult(Result.err(Status.ERR_Denied, "%s must have access to modify x509 certs in NS %s",
                    trans.user(), mechNS));
            return stringAndListStringOrResult;
        }

        if(!isIPAddressPermissionGranted(trans, ca, fqdns)) {
            stringAndListStringOrResult.setResult(Result.err(Status.ERR_Denied,
                    "Machines include a IP Address.  IP Addresses are not allowed except by Permission"));
            return stringAndListStringOrResult;
        }

        // Make sure Primary is the first in fqdns
        primaryIsFirstInFQDNS(fqdns, trans, primary, ignoreIPs, skipReverseDNS);

        stringAndListStringOrResult.setString(email);
        stringAndListStringOrResult.setStringList(fqdns);

        return stringAndListStringOrResult;
    }

    private InetAddressAndResultPOJO findInetAddress(List<String> fqdns, boolean domain_based, boolean ignoreIPs,
                                                     boolean skipReverseDNS, AuthzTrans trans) throws UnknownHostException {
        InetAddressAndResultPOJO inetAddressOrResult = new InetAddressAndResultPOJO();
        inetAddressOrResult.setInetAddress(null);
        inetAddressOrResult.setResult(null);

        InetAddress primary = null;
        if (!fqdns.isEmpty()) { // Passed in FQDNS, validated above
            // Accept domain wild cards, but turn into real machines
            // Need *domain.com:real.machine.domain.com:san.machine.domain.com:...
            if (domain_based) { // Domain set
                // check for Permission in Add Artifact?
                String domain = fqdns.get(0).substring(1); // starts with *, see above
                fqdns.remove(0);
                if (fqdns.isEmpty()) {
                     inetAddressOrResult.setResult(Result.err(Result.ERR_Denied,
                            "Requests using domain require machine declaration"));
                }

                if (!ignoreIPs && !skipReverseDNS) {
                    InetAddress ia = InetAddress.getByName(fqdns.get(0));
                    if (ia == null) {
                        inetAddressOrResult.setResult(Result.err(Result.ERR_Denied,
                                "Request not made from matching IP matching domain"));
                    } else if (ia.getHostName().endsWith(domain)) {
                        primary = ia;
                    }
                }
            } else {
                // Passed in FQDNs, but not starting with *
                if (!ignoreIPs) {
                    for (String cn : fqdns) {
                        try {
                            InetAddress[] ias = InetAddress.getAllByName(cn);
                            Set<String> potentialSanNames = new HashSet<>();
                            for (InetAddress ia1 : ias) {
                                InetAddress ia2 = InetAddress.getByAddress(ia1.getAddress());
                                String ip = trans.ip();
                                if (primary == null && ip.equals(ia1.getHostAddress())) {
                                    primary = ia1;
                                } else if (!cn.equals(ia1.getHostName())
                                        && !ia2.getHostName().equals(ia2.getHostAddress())) {
                                    potentialSanNames.add(ia1.getHostName());
                                }
                            }
                        } catch (UnknownHostException e1) {
                            trans.debug().log(e1);
                            inetAddressOrResult.setResult(Result.err(Result.ERR_BadData, "There is no DNS lookup for %s", cn));
                        }
                    }
                }
            }
        }
        if (inetAddressOrResult.getResult() == null) {
            inetAddressOrResult.setInetAddress(primary);
        }
        return inetAddressOrResult;
    }

    private String setHost(boolean ignoreIPs, boolean skipReverseDNS, List<String> fqdns, InetAddress primary) {
        String host;
        if (ignoreIPs || skipReverseDNS) {
            host = fqdns.get(0);
        } else if (primary == null) {
            host = null;
        } else {
            String thost = primary.getHostName();
            if (thost == null) {
                host = primary.getHostAddress();
            }  else {
                host = thost;
            }
        }
        return host;
    }

    private ArtiDaoDataAndResultPOJO createAdd(AuthzTrans trans, String mechid, String host, boolean dynamic_sans,
                                               String key, boolean domain_based, List<String> fqdns) {
        ArtiDaoDataAndResultPOJO artiDaoDataOrResult = new ArtiDaoDataAndResultPOJO();
        artiDaoDataOrResult.setData(null);
        artiDaoDataOrResult.setResult(null);

        ArtiDAO.Data add = null;
        Result<List<ArtiDAO.Data>> ra = artiDAO.read(trans, mechid, host);
        if (ra.isOKhasData()) {
            add = ra.value.get(0); // single key
            if(dynamic_sans && (add.sans!=null && !add.sans.isEmpty())) { // do not allow both Dynamic and Artifact SANS
                artiDaoDataOrResult.setResult(Result.err(Result.ERR_Denied,
                        "Authorization must not include SANS when doing Dynamic SANS (%s, %s)", mechid, key));
            }
        } else {
            if(domain_based) {
                ra = artiDAO.read(trans, mechid, key);
                if (ra.isOKhasData()) { // is the Template available?
                    add = populateAddFields(ra, host, fqdns);
                    Result<ArtiDAO.Data> rc = artiDAO.create(trans, add); // Create new Artifact from Template
                    if (rc.notOK()) {
                        artiDaoDataOrResult.setResult(Result.err(rc));
                    }
                } else {
                    artiDaoDataOrResult.setResult(Result.err(Result.ERR_Denied,"No Authorization Template for %s, %s", mechid, key));
                }
            } else {
                artiDaoDataOrResult.setResult(Result.err(Result.ERR_Denied,"No Authorization found for %s, %s", mechid, key));
            }
        }

        if (artiDaoDataOrResult.getResult() == null) {
            artiDaoDataOrResult.setData(add);
        }
        return artiDaoDataOrResult;
    }

    private ArtiDAO.Data populateAddFields(Result<List<ArtiDAO.Data>> ra, String host, List<String> fqdns) {
        ArtiDAO.Data add = ra.value.get(0);
        add.machine = host;
        for (String s : fqdns) {
            if (!s.equals(add.machine)) {
                add.sans(true).add(s);
            }
        }
        return add;
    }

    private List<String> addArtifactListedFQDNs(boolean dynamic_sans, ArtiDAO.Data add, List<String> fqdns) {
        // Add Artifact listed FQDNs
        if(!dynamic_sans) {
            if (add.sans != null) {
                for (String s : add.sans) {
                    if (!fqdns.contains(s)) {
                        fqdns.add(s);
                    }
                }
            }
        }
        return fqdns;
    }

    private boolean isConfigNotYetExpired(ArtiDAO.Data add) {
        // Policy 2: If Config marked as Expired, do not create or renew
        Date now = new Date();
        return add.expires == null || !now.after(add.expires);
    }

    private boolean isMechIdCurrent(Identity muser) {
        // Policy 3: MechID must be current
        return muser != null && muser.isFound();
    }

    private boolean isSponsorCurrent(Identity ouser) {
        // Policy 4: Sponsor must be current
        return ouser != null && ouser.isFound();
    }

    private boolean isSponsorResponsibleForMechId(Identity ouser) {
        return ouser.mayOwn() == null;
    }

    private void keepArtifactDataCurrent(Identity ouser, ArtiDAO.Data add, AuthzTrans trans) throws OrganizationException {
        // Policy 5: keep Artifact data current
        if (!ouser.fullID().equals(add.sponsor)) {
            add.sponsor = ouser.fullID();
            artiDAO.update(trans, add);
        }
    }

    private boolean isCallerMechIdOrHasPermissions(AuthzTrans trans, String mechid, String mechNS, CA ca) {
        // Policy 7: Caller must be the MechID or have specifically delegated
        // permissions
        return trans.user().equals(mechid)
                || trans.fish(new AAFPermission(mechNS, CERTMAN, ca.getName(), REQUEST));
    }

    private boolean isIPAddressPermissionGranted(AuthzTrans trans, CA ca, List<String> fqdns) {
        // Policy 8: IP Addresses allowed in Certs only by Permission
        if(!trans.fish(new AAFPermission(aaf_ns,CERTMAN, ca.getName(), "ip"))) {
            for(String fqdn : fqdns) {
                if(CA.IPV4_PATTERN.matcher(fqdn).matches() || CA.IPV6_PATTERN.matcher(fqdn).matches()) {
                    return false;
                }
            }
        }
        return true;
    }

    private void primaryIsFirstInFQDNS(List<String> fqdns, AuthzTrans trans, InetAddress primary, boolean ignoreIPs, boolean skipReverseDNS) {
        if (fqdns.size() > 1) {
            for (int i = 0; i < fqdns.size(); ++i) {
                if (primary == null && !ignoreIPs && !skipReverseDNS) {
                    trans.error().log("CMService var primary is null");
                } else {
                    String fg = fqdns.get(i);
                    if ((fg != null && primary != null && fg.equals(primary.getHostName()))&&(i != 0)) {
                        String tmp = fqdns.get(0);
                        fqdns.set(0, primary.getHostName());
                        fqdns.set(i, tmp);
                    }
                }
            }
        }
    }

    private CSRMeta setCSRMeta(String type, CA ca, String mechid, String email, List<String> fqdns, AuthzTrans trans)
            throws CertException {
        CSRMeta csrMeta;
        switch (type) {
            case Config.CM_REQUEST_TYPE_NORM:
                // default path, places the mechid in the OU
                csrMeta = BCFactory.createCSRMeta(ca, mechid, email, fqdns);
                break;
            case Config.CM_REQUEST_TYPE_SERVER:
                // server cert, we feed the OU a static for now
                // TODO evaluate ease of modifying the jar usage to include the actual OU
                csrMeta = BCFactory.createCSRMeta(ca, trans.org().getName(), email, fqdns);
                break;
            default:
                return null;
        }
        csrMeta.environment(ca.getEnv());
        return csrMeta;
    }

    private boolean canRequesterMakeMoreCerts(AuthzTrans trans, String mechid, String cn) throws CertificateException {
        if(!trans.fish(limitOverridePerm)) {
            Result<List<CertDAO.Data>> existing = certDAO.readID(trans, mechid);
            if(existing.isOK()) {
                int count = 0;
                Date now = new Date();
                for (CertDAO.Data cdd : existing.value) {
                    Collection<? extends Certificate> certs = Factory.toX509Certificate(cdd.x509);
                    for (Certificate cert : certs) {
                        X509Certificate x509 = (X509Certificate) cert;
                        if ((x509.getNotAfter().after(now) && x509.getSubjectDN().getName().contains(cn)) && (++count > max_509s)) {
                            break;
                        }
                    }
                }
                return count <= max_509s;
            }
        }
        return true;
    }

    private CertResp createCertificate(X509andChain x509ac, CA ca, String mechid, AuthzTrans trans, CSRMeta csrMeta, List<String> notes)
            throws CertException, NoSuchAlgorithmException, IOException {
        X509Certificate x509 = x509ac.getX509();
        CertDAO.Data cdd = new CertDAO.Data();
        cdd.ca = ca.getName();
        cdd.serial = x509.getSerialNumber();
        cdd.id = mechid;
        cdd.x500 = x509.getSubjectDN().getName();
        cdd.x509 = Factory.toString(trans, x509);

        certDAO.create(trans, cdd);

        CredDAO.Data crdd = new CredDAO.Data();
        crdd.other = Question.random.nextInt();
        crdd.cred = getChallenge256SaltedHash(csrMeta.challenge(), crdd.other);
        crdd.expires = x509.getNotAfter();
        crdd.id = mechid;
        crdd.ns = Question.domain2ns(crdd.id);
        crdd.type = CredDAO.CERT_SHA256_RSA;
        crdd.tag = cdd.ca + '|' + cdd.serial.toString();
        credDAO.create(trans, crdd);

        return new CertResp(trans, ca, x509, csrMeta, x509ac.getTrustChain(), compileNotes(notes));
    }

    private Result<CertResp> requestCertFunctional(final AuthzTrans trans, final Result<CertReq> req, final CA ca,
                                                   boolean skipReverseDNS, String type) {
        if (req.isOK()) {
            String key = req.value.fqdns.get(0);

            String mechNS = FQI.reverseDomain(req.value.mechid);

            List<String> notes = null;
            List<String> fqdns = req.value.fqdns;
            boolean dynamic_sans = trans.fish(new AAFPermission(null, ca.getPermType(), ca.getName(),DYNAMIC_SANS));
            boolean ignoreIPs = trans.fish(new AAFPermission(mechNS, CERTMAN, ca.getName(), IGNORE_IPS));

            if (!wereMachinesPassedIntoRequest(fqdns)) {
                return Result.err(Result.ERR_BadData, "No Machines passed in Request");
            }

            if (!doesRequesterHaveChangePermissionInNamespaceRequested(mechNS)) {
                return Result.err(Status.ERR_Denied, "%s does not reflect a valid AAF Namespace", req.value.mechid);
            }

            boolean domain_based = fqdns.get(0).startsWith("*");
            fqdns = populateFQDNS(fqdns, key, trans, ca);
            if (fqdns == null) {
                return Result.err(Result.ERR_Denied,
                        "Domain based Authorizations (" + req.value.fqdns.get(0) + ") requires Exception");
            }

            String email;
            try {
                StringAndListStringAndResultPOJO emailAndFQDNSOrResult = setSponsor(trans, fqdns, domain_based,
                        ignoreIPs, skipReverseDNS, req.value.mechid,  dynamic_sans, key, mechNS, ca);
                email = emailAndFQDNSOrResult.getString();
                fqdns = emailAndFQDNSOrResult.getStringList();
                if (email == null || fqdns == null) {
                    return emailAndFQDNSOrResult.getResult();
                }
            } catch (Exception e) {
                trans.error().log(e);
                return Result.err(Status.ERR_Denied,
                        "AppID Sponsorship cannot be determined at this time.  Try later.");
            }

            CSRMeta csrMeta;
            try {
                csrMeta = setCSRMeta(type, ca, req.value.mechid, email, fqdns, trans);
                if (csrMeta == null) {
                    return Result.err(Result.ERR_BadData, "invalid request");
                }

                // Before creating, make sure they don't have too many
                String cn = "CN=" + csrMeta.cn();
                if (!canRequesterMakeMoreCerts(trans, req.value.mechid, cn)) {
                    return Result.err(Result.ERR_Denied, "There are too many Certificates generated for " + cn + " for " + req.value.mechid);
                }
                // Here is where we send off to CA for Signing.
                X509andChain x509ac = ca.sign(trans, csrMeta);
                if (x509ac == null) {
                    return Result.err(Result.ERR_ActionNotCompleted, "x509 Certificate not signed by CA");
                }
                trans.info().printf("X509 Subject: %s", x509ac.getX509().getSubjectDN());

                CertResp cr = createCertificate(x509ac, ca, req.value.mechid, trans, csrMeta, notes);
                return Result.ok(cr);
            } catch (Exception e) {
                trans.debug().log(e);
                return Result.err(Result.ERR_ActionNotCompleted, e.getMessage());
            }
        } else {
            return Result.err(req);
        }
    }

    public Result<CertResp> requestCert(final AuthzTrans trans, final Result<CertReq> req, final CA ca) {
        return requestCertFunctional(trans, req, ca, false, Config.CM_REQUEST_TYPE_NORM);
    }

    public Result<CertResp> requestServerCert(final AuthzTrans trans, final Result<CertReq> req, String remoteAddr, final CA ca) {
        return requestCertFunctional(trans, req, ca, skipReverseDNS(remoteAddr), Config.CM_REQUEST_TYPE_SERVER);
    }

    public Result<CertResp> renewCert(AuthzTrans trans, Result<CertRenew> renew) {
        if (renew.isOK()) {
            return Result.err(Result.ERR_NotImplemented, "Not implemented yet");
        } else {
            return Result.err(renew);
        }
    }

    public Result<Void> dropCert(AuthzTrans trans, Result<CertDrop> drop) {
        if (drop.isOK()) {
            return Result.err(Result.ERR_NotImplemented, "Not implemented yet");
        } else {
            return Result.err(drop);
        }
    }

    public Result<List<Data>> readCertsByMechID(AuthzTrans trans, String mechID) {
        // Policy 1: To Read, must have NS Read or is Sponsor
        String ns = Question.domain2ns(mechID);
        try {
            if (trans.user().equals(mechID) || trans.fish(new AAFPermission(ns,ACCESS, "*", "read"))
                    || (trans.org().validate(trans, Organization.Policy.OWNS_MECHID, null, mechID)) == null) {
                return certDAO.readID(trans, mechID);
            } else {
                return Result.err(Result.ERR_Denied, "%s is not the ID, Sponsor or NS Owner/Admin for %s at %s",
                        trans.user(), mechID, trans.org().getName());
            }
        } catch (OrganizationException e) {
            return Result.err(e);
        }
    }

    public Result<CertResp> requestPersonalCert(AuthzTrans trans, CA ca) {
        if (ca.inPersonalDomains(trans.getUserPrincipal())) {
            Organization org = trans.org();

            // Policy 1: MechID must be current
            Identity ouser;
            try {
                ouser = org.getIdentity(trans, trans.user());
            } catch (OrganizationException e1) {
                trans.debug().log(e1);
                ouser = null;
            }
            if (ouser == null) {
                return Result.err(Result.ERR_Policy, "Requesting User must exist in %s", org.getName());
            }

            // Set Email from most current Sponsor

            CSRMeta csrMeta;
            try {
                csrMeta = BCFactory.createPersonalCSRMeta(ca, trans.user(), ouser.email());
                X509andChain x509ac = ca.sign(trans, csrMeta);
                if (x509ac == null) {
                    return Result.err(Result.ERR_ActionNotCompleted, "x509 Certificate not signed by CA");
                }
                X509Certificate x509 = x509ac.getX509();
                CertDAO.Data cdd = new CertDAO.Data();
                cdd.ca = ca.getName();
                cdd.serial = x509.getSerialNumber();
                cdd.id = trans.user();
                cdd.x500 = x509.getSubjectDN().getName();
                cdd.x509 = Factory.toString(trans, x509);
                certDAO.create(trans, cdd);

                CertResp cr = new CertResp(trans, ca, x509, csrMeta, x509ac.getTrustChain(), compileNotes(null));
                return Result.ok(cr);
            } catch (Exception e) {
                trans.debug().log(e);
                return Result.err(Result.ERR_ActionNotCompleted, e.getMessage());
            }
        } else {
            return Result.err(Result.ERR_Denied, trans.user(), " not supported for CA", ca.getName());
        }
    }

    ///////////////
    // Artifact
    //////////////
    public Result<Void> createArtifact(AuthzTrans trans, List<ArtiDAO.Data> list) {
        CertmanValidator v = new CertmanValidator().artisRequired(list, 1);
        if (v.err()) {
            return Result.err(Result.ERR_BadData, v.errs());
        }
        for (ArtiDAO.Data add : list) {
            try {
                // Policy 1: MechID must exist in Org
                Identity muser = trans.org().getIdentity(trans, add.mechid);
                if (muser == null) {
                    return Result.err(Result.ERR_Denied, "%s is not valid for %s", add.mechid, trans.org().getName());
                }

                // Policy 2: MechID must have valid Organization Owner
                Identity emailUser;
                if (muser.isPerson()) {
                    emailUser = muser;
                } else {
                    Identity ouser = muser.responsibleTo();
                    if (ouser == null) {
                        return Result.err(Result.ERR_Denied, "%s is not a valid Sponsor for %s at %s", trans.user(),
                                add.mechid, trans.org().getName());
                    }

                    // Policy 3: Calling ID must be MechID Owner
                    if (!trans.user().startsWith(ouser.id())) {
                        return Result.err(Result.ERR_Denied, "%s is not the Sponsor for %s at %s", trans.user(),
                                add.mechid, trans.org().getName());
                    }
                    emailUser = ouser;
                }

                // Policy 4: Renewal Days are between 10 and 60 (constants, may be
                // parameterized)
                if (add.renewDays < MIN_RENEWAL) {
                    add.renewDays = STD_RENEWAL;
                } else if (add.renewDays > MAX_RENEWAL) {
                    add.renewDays = MAX_RENEWAL;
                }

                // Policy 5: If Notify is blank, set to Owner's Email
                if (add.notify == null || add.notify.length() == 0) {
                    add.notify = "mailto:" + emailUser.email();
                }

                // Policy 6: Only do Domain by Exception
                if (add.machine.startsWith("*")) { // Domain set
                    CA ca = certManager.getCA(add.ca);
                    if (!trans.fish(new AAFPermission(ca.getPermNS(),ca.getPermType(), add.ca, DOMAIN))) {
                        return Result.err(Result.ERR_Denied, "Domain Artifacts (%s) requires specific Permission",
                                add.machine);
                    }
                }

                // Set Sponsor from Golden Source
                add.sponsor = emailUser.fullID();

            } catch (OrganizationException e) {
                return Result.err(e);
            }
            // Add to DB
            Result<ArtiDAO.Data> rv = artiDAO.create(trans, add);
            //  come up with Partial Reporting Scheme, or allow only one at a time.
            if (rv.notOK()) {
                return Result.err(rv);
            }
        }
        return Result.ok();
    }

    public Result<List<ArtiDAO.Data>> readArtifacts(AuthzTrans trans, ArtiDAO.Data add) throws OrganizationException {
        CertmanValidator v = new CertmanValidator().keys(add);
        if (v.err()) {
            return Result.err(Result.ERR_BadData, v.errs());
        }
        Result<List<ArtiDAO.Data>> data = artiDAO.read(trans, add);
        if (data.notOKorIsEmpty()) {
            return data;
        }
        add = data.value.get(0);
        if (trans.user().equals(add.mechid)
                || trans.fish(root_read_permission,
                new AAFPermission(add.ns,ACCESS, "*", "read"),
                new AAFPermission(add.ns,CERTMAN, add.ca, "read"),
                new AAFPermission(add.ns,CERTMAN, add.ca, REQUEST))
                || (trans.org().validate(trans, Organization.Policy.OWNS_MECHID, null, add.mechid)) == null) {
            return data;
        } else {
            return Result.err(Result.ERR_Denied,
                    "%s is not %s, is not the sponsor, and doesn't have delegated permission.", trans.user(),
                    add.mechid, add.ns + ".certman|" + add.ca + "|read or ...|request"); // note: reason is set by 2nd
            // case, if 1st case misses
        }

    }

    public Result<List<ArtiDAO.Data>> readArtifactsByMechID(AuthzTrans trans, String mechid)
            throws OrganizationException {
        CertmanValidator v = new CertmanValidator();
        v.nullOrBlank("mechid", mechid);
        if (v.err()) {
            return Result.err(Result.ERR_BadData, v.errs());
        }
        String ns = FQI.reverseDomain(mechid);

        String reason;
        if (trans.fish(new AAFPermission(ns, ACCESS, "*", "read"))
                || (reason = trans.org().validate(trans, Organization.Policy.OWNS_MECHID, null, mechid)) == null) {
            return artiDAO.readByMechID(trans, mechid);
        } else {
            return Result.err(Result.ERR_Denied, reason); // note: reason is set by 2nd case, if 1st case misses
        }

    }

    public Result<List<ArtiDAO.Data>> readArtifactsByMachine(AuthzTrans trans, String machine) {
        CertmanValidator v = new CertmanValidator();
        v.nullOrBlank("machine", machine);
        if (v.err()) {
            return Result.err(Result.ERR_BadData, v.errs());
        }

        //  do some checks?

        return artiDAO.readByMachine(trans, machine);
    }

    public Result<List<ArtiDAO.Data>> readArtifactsByNs(AuthzTrans trans, String ns) {
        CertmanValidator v = new CertmanValidator();
        v.nullOrBlank("ns", ns);
        if (v.err()) {
            return Result.err(Result.ERR_BadData, v.errs());
        }

        //  do some checks?
        return artiDAO.readByNs(trans, ns);
    }

    public Result<Void> updateArtifact(AuthzTrans trans, List<ArtiDAO.Data> list) throws OrganizationException {
        CertmanValidator v = new CertmanValidator();
        v.artisRequired(list, 1);
        if (v.err()) {
            return Result.err(Result.ERR_BadData, v.errs());
        }

        // Check if requesting User is Sponsor
        //  Shall we do one, or multiples?
        for (ArtiDAO.Data add : list) {
            // Policy 1: MechID must exist in Org
            Identity muser = trans.org().getIdentity(trans, add.mechid);
            if (muser == null) {
                return Result.err(Result.ERR_Denied, "%s is not valid for %s", add.mechid, trans.org().getName());
            }

            // Policy 2: MechID must have valid Organization Owner
            Identity ouser = muser.responsibleTo();
            if (ouser == null) {
                return Result.err(Result.ERR_Denied, "%s is not a valid Sponsor for %s at %s", trans.user(), add.mechid,
                        trans.org().getName());
            }

            // Policy 3: Renewal Days are between 10 and 60 (constants, may be
            // parameterized)
            if (add.renewDays < MIN_RENEWAL) {
                add.renewDays = STD_RENEWAL;
            } else if (add.renewDays > MAX_RENEWAL) {
                add.renewDays = MAX_RENEWAL;
            }

            // Policy 4: Data is always updated with the latest Sponsor
            // Add to Sponsor, to make sure we are always up to date.
            add.sponsor = ouser.fullID();

            // Policy 5: If Notify is blank, set to Owner's Email
            if (add.notify == null || add.notify.length() == 0) {
                add.notify = "mailto:" + ouser.email();
            }
            // Policy 6: Only do Domain by Exception
            if (add.machine.startsWith("*")) { // Domain set
                CA ca = certManager.getCA(add.ca);
                if (ca == null) {
                    return Result.err(Result.ERR_BadData, "CA is required in Artifact");
                }
                if (!trans.fish(new AAFPermission(null,ca.getPermType(), add.ca, DOMAIN))) {
                    return Result.err(Result.ERR_Denied, "Domain Artifacts (%s) requires specific Permission",
                            add.machine);
                }
            }

            // Policy 7: only Owner may update info
            if (trans.user().startsWith(ouser.id())) {
                return artiDAO.update(trans, add);
            } else {
                return Result.err(Result.ERR_Denied, "%s may not update info for %s", trans.user(), muser.fullID());
            }
        }
        return Result.err(Result.ERR_BadData, "No Artifacts to update");
    }

    public Result<Void> deleteArtifact(AuthzTrans trans, String mechid, String machine) throws OrganizationException {
        CertmanValidator v = new CertmanValidator();
        v.nullOrBlank("mechid", mechid).nullOrBlank("machine", machine);
        if (v.err()) {
            return Result.err(Result.ERR_BadData, v.errs());
        }

        Result<List<ArtiDAO.Data>> rlad = artiDAO.read(trans, mechid, machine);
        if (rlad.notOKorIsEmpty()) {
            return Result.err(Result.ERR_NotFound, "Artifact for %s %s does not exist.", mechid, machine);
        }

        return deleteArtifact(trans, rlad.value.get(0));
    }

    private Result<Void> deleteArtifact(AuthzTrans trans, ArtiDAO.Data add) throws OrganizationException {
        // Policy 1: Record should be delete able only by Existing Sponsor.
        String sponsor = null;
        Identity muser = trans.org().getIdentity(trans, add.mechid);
        if (muser != null) {
            Identity ouser = muser.responsibleTo();
            if (ouser != null) {
                sponsor = ouser.fullID();
            }
        }
        // Policy 1.a: If Sponsorship is deleted in system of Record, then
        // accept deletion by sponsor in Artifact Table
        if (sponsor == null) {
            sponsor = add.sponsor;
        }

        String ns = FQI.reverseDomain(add.mechid);

        if (trans.fish(new AAFPermission(ns,ACCESS, "*", "write")) || trans.user().equals(sponsor)) {
            return artiDAO.delete(trans, add, false);
        }
        return Result.err(Result.ERR_Denied, "%1 is not allowed to delete this item", trans.user());
    }

    public Result<Void> deleteArtifact(AuthzTrans trans, List<ArtiDAO.Data> list) {
        CertmanValidator v = new CertmanValidator().artisRequired(list, 1);
        if (v.err()) {
            return Result.err(Result.ERR_BadData, v.errs());
        }

        try {
            boolean partial = false;
            Result<Void> result = null;
            for (ArtiDAO.Data add : list) {
                result = deleteArtifact(trans, add);
                if (result.notOK()) {
                    partial = true;
                }
            }
            if (result == null) {
                result = Result.err(Result.ERR_BadData, "No Artifacts to delete");
            } else if (partial) {
                result.partialContent(true);
            }
            return result;
        } catch (Exception e) {
            return Result.err(e);
        }
    }

    private String[] compileNotes(List<String> notes) {
        String[] rv;
        if (notes == null) {
            rv = NO_NOTES;
        } else {
            rv = new String[notes.size()];
            notes.toArray(rv);
        }
        return rv;
    }

    private ByteBuffer getChallenge256SaltedHash(String challenge, int salt) throws NoSuchAlgorithmException {
        ByteBuffer bb = ByteBuffer.allocate(Integer.SIZE + challenge.length());
        bb.putInt(salt);
        bb.put(challenge.getBytes());
        byte[] hash = Hash.hashSHA256(bb.array());
        return ByteBuffer.wrap(hash);
    }
}
