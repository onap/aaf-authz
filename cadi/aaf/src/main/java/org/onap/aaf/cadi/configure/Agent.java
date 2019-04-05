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

package org.onap.aaf.cadi.configure;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.onap.aaf.cadi.Access;
import org.onap.aaf.cadi.CadiException;
import org.onap.aaf.cadi.CmdLine;
import org.onap.aaf.cadi.LocatorException;
import org.onap.aaf.cadi.PropAccess;
import org.onap.aaf.cadi.Symm;
import org.onap.aaf.cadi.aaf.client.ErrMessage;
import org.onap.aaf.cadi.aaf.v2_0.AAFCon;
import org.onap.aaf.cadi.aaf.v2_0.AAFConHttp;
import org.onap.aaf.cadi.client.Future;
import org.onap.aaf.cadi.client.Rcli;
import org.onap.aaf.cadi.client.Retryable;
import org.onap.aaf.cadi.config.Config;
import org.onap.aaf.cadi.config.SecurityInfoC;
import org.onap.aaf.cadi.http.HBasicAuthSS;
import org.onap.aaf.cadi.locator.SingleEndpointLocator;
import org.onap.aaf.cadi.sso.AAFSSO;
import org.onap.aaf.cadi.util.Chmod;
import org.onap.aaf.cadi.util.FQI;
import org.onap.aaf.misc.env.APIException;
import org.onap.aaf.misc.env.Data.TYPE;
import org.onap.aaf.misc.env.Env;
import org.onap.aaf.misc.env.TimeTaken;
import org.onap.aaf.misc.env.Trans;
import org.onap.aaf.misc.env.util.Chrono;
import org.onap.aaf.misc.env.util.Split;
import org.onap.aaf.misc.rosetta.env.RosettaDF;
import org.onap.aaf.misc.rosetta.env.RosettaEnv;

import aaf.v2_0.Perm;
import aaf.v2_0.Perms;
import certman.v1_0.Artifacts;
import certman.v1_0.Artifacts.Artifact;
import certman.v1_0.CertInfo;
import certman.v1_0.CertificateRequest;
import locate.v1_1.Configuration;
import locate.v1_1.Configuration.Props;

public class Agent {
    private static final String HASHES = "################################################################";
    private static final String PRINT = "print";
    private static final String FILE = "file";
    public static final String PKCS12 = "pkcs12";
    public static final String JKS = "jks";
    private static final String SCRIPT="script";
    
    private static final String CM_VER = "1.0";
    public static final int PASS_SIZE = 24;
    private static int TIMEOUT;
    
    private static RosettaDF<CertificateRequest> reqDF;
    private static RosettaDF<CertInfo> certDF;
    private static RosettaDF<Artifacts> artifactsDF;
    private static RosettaDF<Configuration> configDF;
    private static RosettaDF<Perms> permDF;
    private static ErrMessage errMsg;
    private static Map<String,PlaceArtifact> placeArtifact;
    private static RosettaEnv env;
    
    private static boolean doExit;
    private static AAFCon<?> aafcon;
    
    private static List<String> CRED_TAGS = Arrays.asList(new String[] {
            Config.CADI_KEYFILE,
            Config.AAF_APPID, Config.AAF_APPPASS,
            Config.CADI_KEYSTORE, Config.CADI_KEYSTORE_PASSWORD, Config.CADI_KEY_PASSWORD,
            Config.CADI_TRUSTSTORE,Config.CADI_TRUSTSTORE_PASSWORD,
            Config.CADI_ALIAS, Config.CADI_X509_ISSUERS
            });


    public static void main(String[] args) {
        int exitCode = 0;
        doExit = true;
        if (args.length>0 && "cadi".equals(args[0])) {
            String[] newArgs = new String[args.length-1];
            System.arraycopy(args, 1, newArgs, 0, newArgs.length);
            if (newArgs.length==0) {
                System.out.println(HASHES);
                System.out.println("Note: Cadi CmdLine is a separate component.  When running with\n\t"
                        + "Agent, always preface with \"cadi\",\n\tex: cadi keygen [<keyfile>]");
                System.out.println(HASHES);
            }
            CmdLine.main(newArgs);
        } else {
            try {
                AAFSSO aafsso=null;
                PropAccess access;
                
                if (args.length>1 && args[0].equals("validate") ) {
                    int idx = args[1].indexOf('=');
                    aafsso = null;
                    access = new PropAccess(
                                (idx<0?Config.CADI_PROP_FILES:args[1].substring(0, idx))+
                                '='+
                                (idx<0?args[1]:args[1].substring(idx+1)));
                } else {
                    aafsso= new AAFSSO(args, new AAFSSO.ProcessArgs() {
                        @Override
                        public Properties process(String[] args, Properties props) {
                            if (args.length>1) {
                                if (!args[0].equals("keypairgen")) {
                                    props.put(Config.AAF_APPID, args[1]);
                                }    
                            }
                            return props;
                        }
                    });
                    access = aafsso.access();
                }
                    
                if (aafsso!=null && aafsso.loginOnly()) {
                    aafsso.setLogDefault();
                    aafsso.writeFiles();
                    System.out.println("AAF SSO information created in ~/.aaf");
                } else {
                    env = new RosettaEnv(access.getProperties());
                    Deque<String> cmds = new ArrayDeque<String>();
                    for (String p : args) {
                    	int eq;
                        if ("-noexit".equalsIgnoreCase(p)) {
                            doExit = false;
                        } else if ((eq=p.indexOf('=')) < 0) {
                            cmds.add(p);
                        } else {
                        	access.setProperty(p.substring(0,eq), p.substring(eq+1));
                        }
                    }
                    
                    if (cmds.size()==0) {
                        if (aafsso!=null) {
                            aafsso.setLogDefault();
                        }
                        // NOTE: CHANGE IN CMDS should be reflected in AAFSSO constructor, to get FQI->aaf-id or not
                        System.out.println("Usage: java -jar <cadi-aaf-*-full.jar> cmd [<tag=value>]*");
                        System.out.println("   create     <FQI> [<machine>]");
                        System.out.println("   read       <FQI> [<machine>]");
                        System.out.println("   update     <FQI> [<machine>]");
                        System.out.println("   delete     <FQI> [<machine>]");
                        System.out.println("   copy       <FQI> <machine> <newmachine>[,<newmachine>]*");
                        System.out.println("   place      <FQI> [<machine>]");
                        System.out.println("   showpass   <FQI> [<machine>]");
                        System.out.println("   check      <FQI> [<machine>]");
                        System.out.println("   keypairgen <FQI>");
                        System.out.println("   config     <FQI>");
                        System.out.println("   validate   <NS>.props>");
                        System.out.println("   --- Additional Tool Access ---");
                        System.out.println("     ** Type with no params for Tool Help");
                        System.out.println("     ** If using with Agent, preface with \"cadi\"");
                        System.out.println("   cadi <cadi tool params, see -?>");
                        
                        if (doExit) {
                            System.exit(1);
                        }
                    }
                    
                    TIMEOUT = Integer.parseInt(env.getProperty(Config.AAF_CONN_TIMEOUT, "5000"));
                
                    reqDF = env.newDataFactory(CertificateRequest.class);
                    artifactsDF = env.newDataFactory(Artifacts.class);
                    certDF = env.newDataFactory(CertInfo.class);
                    configDF = env.newDataFactory(Configuration.class);
                    permDF = env.newDataFactory(Perms.class);
                    errMsg = new ErrMessage(env);
        
                    placeArtifact = new HashMap<>();
                    placeArtifact.put(JKS, new PlaceArtifactInKeystore(JKS));
                    placeArtifact.put(PKCS12, new PlaceArtifactInKeystore(PKCS12));
                    placeArtifact.put(FILE, new PlaceArtifactInFiles());
                    placeArtifact.put(PRINT, new PlaceArtifactOnStream(System.out));
                    placeArtifact.put(SCRIPT, new PlaceArtifactScripts());
                    
                    Trans trans = env.newTrans();
                    String token;
                    if ((token=access.getProperty("oauth_token"))!=null) {
                        trans.setProperty("oauth_token", token);
                    }
                    try {
                        if (aafsso!=null) {
                        // show Std out again
                            aafsso.setLogDefault();
                            aafsso.setStdErrDefault();
                            
                            // if CM_URL can be obtained, add to sso.props, if written
                            String cm_url = getProperty(access,env,false, Config.CM_URL,Config.CM_URL_DEF);
                            if (cm_url!=null) {
                                aafsso.addProp(Config.CM_URL, cm_url);
                            }
                            aafsso.writeFiles();
                        }
    
                        
    
                        String cmd = cmds.removeFirst();
                        switch(cmd) {
                            case "place":
                                placeCerts(trans,aafcon(access),cmds);
                                break;
                            case "create":
                                createArtifact(trans, aafcon(access),cmds);
                                break;
                            case "read":
                                readArtifact(trans, aafcon(access), cmds);
                                break;
                            case "copy":
                                copyArtifact(trans, aafcon(access), cmds);
                                break;
                            case "update":
                                updateArtifact(trans, aafcon(access), cmds);
                                break;
                            case "delete":
                                deleteArtifact(trans, aafcon(access), cmds);
                                break;
                            case "showpass":
                                showPass(trans, aafcon(access), cmds);
                                break;
                            case "keypairgen":
                                keypairGen(trans, access, cmds);
                                break;
                            case "config":
                            	config(trans,access,args,cmds);
                                break;
                            case "validate":
                                validate(access);
                                break;
                            case "check":
                                try {
                                    exitCode = check(trans,aafcon(access),cmds);
                                } catch (Exception e) {
                                    exitCode = 1;
                                    throw e;
                                }
                                break;
                            default:
                                AAFSSO.cons.printf("Unknown command \"%s\"\n", cmd);
                        }
                    } finally {
                        StringBuilder sb = new StringBuilder();
                        trans.auditTrail(4, sb, Trans.REMOTE);
                        if (sb.length()>0) {
                            trans.info().log("Trans Info\n",sb);
                        }
                    }
                    if (aafsso!=null) {
                        aafsso.close();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (exitCode != 0 && doExit) {
            System.exit(exitCode);
        }
    }

    private static synchronized AAFCon<?> aafcon(Access access) throws APIException, CadiException, LocatorException {
        if (aafcon==null) {
            aafcon = new AAFConHttp(access,Config.CM_URL);
        }
        return aafcon;
    }

    private static String getProperty(PropAccess pa, Env env, boolean secure, String tag, String prompt, Object ... def) {
        String value;
        if ((value=pa.getProperty(tag))==null) {
            if (secure) {
                value = new String(AAFSSO.cons.readPassword(prompt, def));
            } else {
                value = AAFSSO.cons.readLine(prompt,def).trim();
            }
            if (value!=null) {
                if (value.length()>0) {
                    pa.setProperty(tag,value);
                    env.setProperty(tag,value);
                } else if (def.length==1) {
                    value=def[0].toString();
                    pa.setProperty(tag,value);
                    env.setProperty(tag,value);
                }
            }
        }
        return value;
    }

    private static String fqi(Deque<String> cmds) {
        if (cmds.size()<1) {
            String alias = env.getProperty(Config.CADI_ALIAS);
            return alias!=null?alias:AAFSSO.cons.readLine("AppID: ");
        }
        return cmds.removeFirst();    
    }

    private static String machine(Deque<String> cmds) throws UnknownHostException {
        if (cmds.size()>0) {
            return cmds.removeFirst();
        } else {
            String mach = env.getProperty(Config.HOSTNAME);
            return mach!=null?mach:InetAddress.getLocalHost().getHostName();
        }
    }

    private static String[] machines(Deque<String> cmds)  {
        String machines;
        if (cmds.size()>0) {
            machines = cmds.removeFirst();
        } else {
            machines = AAFSSO.cons.readLine("Machines (sep by ','): ");
        }
        return Split.split(',', machines);
    }

    private static void createArtifact(Trans trans, AAFCon<?> aafcon, Deque<String> cmds) throws Exception {
        final String mechID = fqi(cmds);
        final String machine = machine(cmds);

        Artifacts artifacts = new Artifacts();
        Artifact arti = new Artifact();
        artifacts.getArtifact().add(arti);
        arti.setMechid(mechID!=null?mechID:AAFSSO.cons.readLine("AppID: "));
        arti.setMachine(machine!=null?machine:AAFSSO.cons.readLine("Machine (%s): ",InetAddress.getLocalHost().getHostName()));
        arti.setCa(AAFSSO.cons.readLine("CA: (%s): ","aaf"));
        
        String resp = AAFSSO.cons.readLine("Types [file,pkcs12,jks,script] (%s): ", PKCS12);
        for (String s : Split.splitTrim(',', resp)) {
            arti.getType().add(s);
        }
        // Always do Script
        if (!resp.contains(SCRIPT)) {
            arti.getType().add(SCRIPT);
        }

        // Note: Sponsor is set on Creation by CM
        String configRootName = FQI.reverseDomain(arti.getMechid());
        arti.setNs(AAFSSO.cons.readLine("Namespace (%s): ",configRootName));
        arti.setDir(AAFSSO.cons.readLine("Directory (%s): ", System.getProperty("user.dir")));
        arti.setOsUser(AAFSSO.cons.readLine("OS User (%s): ", System.getProperty("user.name")));
        arti.setRenewDays(Integer.parseInt(AAFSSO.cons.readLine("Renewal Days (%s):", "30")));
        arti.setNotification(toNotification(AAFSSO.cons.readLine("Notification (mailto owner):", "")));
        
        TimeTaken tt = trans.start("Create Artifact", Env.REMOTE);
        try {
            Future<Artifacts> future = aafcon.client(CM_VER).create("/cert/artifacts", artifactsDF, artifacts);
            if (future.get(TIMEOUT)) {
                trans.info().printf("Call to AAF Certman successful %s, %s",arti.getMechid(), arti.getMachine());
            } else {
                trans.error().printf("Call to AAF Certman failed, %s",
                    errMsg.toMsg(future));
            }
        } finally {
            tt.done();
        }
    }

    private static String toNotification(String notification) {
        if (notification==null) {
            notification="";
        } else if (notification.length()>0) {
            if (notification.indexOf(':')<0) {
                notification = "mailto:" + notification;
            }
        }
        return notification;
    }
    

    private static void readArtifact(Trans trans, AAFCon<?> aafcon, Deque<String> cmds) throws Exception {
        String mechID = fqi(cmds);
        String machine = machine(cmds);

        TimeTaken tt = trans.start("Read Artifact", Env.SUB);
        try {
            Future<Artifacts> future = aafcon.client(CM_VER)
                    .read("/cert/artifacts/"+mechID+'/'+machine, artifactsDF,"Authorization","Bearer " + trans.getProperty("oauth_token"));
    
            if (future.get(TIMEOUT)) {
                boolean printed = false;
                for (Artifact a : future.value.getArtifact()) {
                    AAFSSO.cons.printf("AppID:          %s\n",a.getMechid()); 
                    AAFSSO.cons.printf("  Sponsor:       %s\n",a.getSponsor()); 
                    AAFSSO.cons.printf("Machine:         %s\n",a.getMachine()); 
                    AAFSSO.cons.printf("CA:              %s\n",a.getCa()); 
                    StringBuilder sb = new StringBuilder();
                    boolean first = true;
                    for (String t : a.getType()) {
                        if (first) {first=false;}
                        else{sb.append(',');}
                        sb.append(t);
                    }
                    AAFSSO.cons.printf("Types:           %s\n",sb);
                    AAFSSO.cons.printf("Namespace:       %s\n",a.getNs()); 
                    AAFSSO.cons.printf("Directory:       %s\n",a.getDir());
                    AAFSSO.cons.printf("O/S User:        %s\n",a.getOsUser());
                    AAFSSO.cons.printf("Renew Days:      %d\n",a.getRenewDays());
                    AAFSSO.cons.printf("Notification     %s\n",a.getNotification());
                    printed = true;
                }
                if (!printed) {
                    AAFSSO.cons.printf("Artifact for %s %s does not exist\n", mechID, machine);
                }
            } else {
                trans.error().log(errMsg.toMsg(future));
            }
        } finally {
            tt.done();
        }
    }
    
    private static void copyArtifact(Trans trans, AAFCon<?> aafcon, Deque<String> cmds) throws Exception {
        String mechID = fqi(cmds);
        String machine = machine(cmds);
        String[] newmachs = machines(cmds);
        if (machine==null || newmachs == null) {
            trans.error().log("No machines listed to copy to");
        } else {
            TimeTaken tt = trans.start("Copy Artifact", Env.REMOTE);
            try {
                Future<Artifacts> future = aafcon.client(CM_VER)
                        .read("/cert/artifacts/"+mechID+'/'+machine, artifactsDF);
            
                if (future.get(TIMEOUT)) {
                    boolean printed = false;
                    for (Artifact a : future.value.getArtifact()) {
                        for (String m : newmachs) {
                            a.setMachine(m);
                            Future<Artifacts> fup = aafcon.client(CM_VER).update("/cert/artifacts", artifactsDF, future.value);
                            if (fup.get(TIMEOUT)) {
                                trans.info().printf("Copy of %s %s successful to %s",mechID,machine,m);
                            } else {
                                trans.error().printf("Call to AAF Certman failed, %s",
                                    errMsg.toMsg(fup));
                            }
    
                            printed = true;
                        }
                    }
                    if (!printed) {
                        AAFSSO.cons.printf("Artifact for %s %s does not exist", mechID, machine);
                    }
                } else {
                    trans.error().log(errMsg.toMsg(future));
                }
            } finally {
                tt.done();
            }
        }
    }

    private static void updateArtifact(Trans trans, AAFCon<?> aafcon, Deque<String> cmds) throws Exception {
        String mechID = fqi(cmds);
        String machine = machine(cmds);

        TimeTaken tt = trans.start("Update Artifact", Env.REMOTE);
        try {
            Future<Artifacts> fread = aafcon.client(CM_VER)
                    .read("/cert/artifacts/"+mechID+'/'+machine, artifactsDF);
    
            if (fread.get(TIMEOUT)) {
                Artifacts artifacts = new Artifacts();
                for (Artifact a : fread.value.getArtifact()) {
                    Artifact arti = new Artifact();
                    artifacts.getArtifact().add(arti);
                    
                    AAFSSO.cons.printf("For %s on %s\n", a.getMechid(),a.getMachine());
                    arti.setMechid(a.getMechid());
                    arti.setMachine(a.getMachine());
                    arti.setCa(AAFSSO.cons.readLine("CA: (%s): ",a.getCa()));
                    StringBuilder sb = new StringBuilder();
                    boolean first = true;
                    for (String t : a.getType()) {
                        if (first) {first=false;}
                        else{sb.append(',');}
                        sb.append(t);
                    }
    
                    String resp = AAFSSO.cons.readLine("Types [file,jks,pkcs12] (%s): ", sb);
                    for (String s : Split.splitTrim(',', resp)) {
                        arti.getType().add(s);
                    }
                    // Always do Script
                    if (!resp.contains(SCRIPT)) {
                        arti.getType().add(SCRIPT);
                    }

                    // Note: Sponsor is set on Creation by CM
                    arti.setNs(AAFSSO.cons.readLine("Namespace (%s): ",a.getNs()));
                    arti.setDir(AAFSSO.cons.readLine("Directory (%s): ", a.getDir()));
                    arti.setOsUser(AAFSSO.cons.readLine("OS User (%s): ", a.getOsUser()));
                    arti.setRenewDays(Integer.parseInt(AAFSSO.cons.readLine("Renew Days (%s):", a.getRenewDays())));
                    arti.setNotification(toNotification(AAFSSO.cons.readLine("Notification (%s):", a.getNotification())));
    
                }
                if (artifacts.getArtifact().size()==0) {
                    AAFSSO.cons.printf("Artifact for %s %s does not exist", mechID, machine);
                } else {
                    Future<Artifacts> fup = aafcon.client(CM_VER).update("/cert/artifacts", artifactsDF, artifacts);
                    if (fup.get(TIMEOUT)) {
                        trans.info().printf("Call to AAF Certman successful %s, %s",mechID,machine);
                    } else {
                        trans.error().printf("Call to AAF Certman failed, %s",
                            errMsg.toMsg(fup));
                    }
                }
            } else {
                trans.error().printf("Call to AAF Certman failed, %s %s, %s",
                        errMsg.toMsg(fread),mechID,machine);
            }
        } finally {
            tt.done();
        }
    }
    
    private static void deleteArtifact(Trans trans, AAFCon<?> aafcon, Deque<String> cmds) throws Exception {
        String mechid = fqi(cmds);
        String machine = machine(cmds);
        
        TimeTaken tt = trans.start("Delete Artifact", Env.REMOTE);
        try {
            Future<Void> future = aafcon.client(CM_VER)
                    .delete("/cert/artifacts/"+mechid+"/"+machine,"application/json" );
    
            if (future.get(TIMEOUT)) {
                trans.info().printf("Call to AAF Certman successful %s, %s",mechid,machine);
            } else {
                trans.error().printf("Call to AAF Certman failed, %s %s, %s",
                    errMsg.toMsg(future),mechid,machine);
            }
        } finally {
            tt.done();
        }
    }

    

    private static boolean placeCerts(Trans trans, AAFCon<?> aafcon, Deque<String> cmds) throws Exception {
        boolean rv = false;
        String mechID = fqi(cmds);
        String machine = machine(cmds);
        String[] fqdns = Split.split(':', machine);
        String key;
        if (fqdns.length>1) {
            key = fqdns[0];
            machine = fqdns[1];
        } else {
            key = machine;
        }
        
        TimeTaken tt = trans.start("Place Artifact", Env.REMOTE);
        try {
            Future<Artifacts> acf = aafcon.client(CM_VER)
                    .read("/cert/artifacts/"+mechID+'/'+key, artifactsDF);
            if (acf.get(TIMEOUT)) {
                if (acf.value.getArtifact()==null || acf.value.getArtifact().isEmpty()) {
                    AAFSSO.cons.printf("===> There are no artifacts for %s on machine '%s'\n", mechID, key);
                } else {
                    for (Artifact a : acf.value.getArtifact()) {
                        String osID = System.getProperty("user.name");
                        if (a.getOsUser().equals(osID)) {
                            CertificateRequest cr = new CertificateRequest();
                            cr.setMechid(a.getMechid());
                            cr.setSponsor(a.getSponsor());
                            for (int i=0;i<fqdns.length;++i) {
                                cr.getFqdns().add(fqdns[i]);
                            }
                            Future<String> f = aafcon.client(CM_VER)
                                    .updateRespondString("/cert/" + a.getCa()+"?withTrust",reqDF, cr);
                            if (f.get(TIMEOUT)) {
                                CertInfo capi = certDF.newData().in(TYPE.JSON).load(f.body()).asObject();
                                for (String type : a.getType()) {
                                    PlaceArtifact pa = placeArtifact.get(type);
                                    if (pa!=null) {
                                        if (rv = pa.place(trans, capi, a,machine)) {
                                            notifyPlaced(a,rv);
                                        }
                                    }
                                }
                                // Cover for the above multiple pass possibilities with some static Data, then clear per Artifact
                            } else {
                                trans.error().log(errMsg.toMsg(f));
                            }
                        } else {
                            trans.error().log("You must be OS User \"" + a.getOsUser() +"\" to place Certificates on this box");
                        }
                    }
                }
                PropHolder.writeAll();
            } else {
                trans.error().log(errMsg.toMsg(acf));
            }
        } finally {
            tt.done();
        }
        return rv;
    }
    
    private static void notifyPlaced(Artifact a, boolean rv) {
    }

    private static void showPass(Trans trans, AAFCon<?> aafcon, Deque<String> cmds) throws Exception {
        String mechID = fqi(cmds);
        String machine = machine(cmds);

        TimeTaken tt = trans.start("Show Password", Env.REMOTE);
        try {
            Future<Artifacts> acf = aafcon.client(CM_VER)
                    .read("/cert/artifacts/"+mechID+'/'+machine, artifactsDF);
            if (acf.get(TIMEOUT)) {
                // Have to wait for JDK 1.7 source...
                //switch(artifact.getType()) {
                if (acf.value.getArtifact()==null || acf.value.getArtifact().isEmpty()) {
                    AAFSSO.cons.printf("No Artifacts found for %s on %s ", mechID, machine);
                } else {
                    String id = aafcon.defID();
                    boolean allowed;
                    for (Artifact a : acf.value.getArtifact()) {
                        allowed = id!=null && (id.equals(a.getSponsor()) ||
                                (id.equals(a.getMechid()) 
                                        && aafcon.securityInfo().defSS.getClass().isAssignableFrom(HBasicAuthSS.class)));
                        if (!allowed) {
                            Future<String> pf = aafcon.client(CM_VER).read("/cert/may/" + 
                                    a.getNs()+"|certman|"+a.getCa()+"|showpass","*/*");
                            if (pf.get(TIMEOUT)) {
                                allowed = true;
                            } else {
                                trans.error().log(errMsg.toMsg(pf));
                            }
                        }
                        if (allowed) {
                            File dir = new File(a.getDir());
                            Properties props = new Properties();
                            FileInputStream fis = new FileInputStream(new File(dir,a.getNs()+".cred.props"));
                            try {
                                props.load(fis);
                                File chalFile = new File(dir,a.getNs()+".chal");
                                if(chalFile.exists()) {
                                    fis.close();
	                                fis = new FileInputStream(chalFile);
	                                props.load(fis);
                                }
                            } finally {
                                fis.close();
                            }
                            
                            File f = new File(dir,a.getNs()+".keyfile");
                            if (f.exists()) {
                                Symm symm = ArtifactDir.getSymm(f);
                                
                                for (Iterator<Entry<Object,Object>> iter = props.entrySet().iterator(); iter.hasNext();) {
                                    Entry<Object,Object> en = iter.next();
                                    if (en.getValue().toString().startsWith("enc:")) {
                                        System.out.printf("%s=%s\n", en.getKey(), symm.depass(en.getValue().toString()));
                                    }
                                }
                            } else {
                                trans.error().printf("%s.keyfile must exist to read passwords for %s on %s",
                                        f.getAbsolutePath(),a.getMechid(), a.getMachine());
                            }
                        }
                    }
                }
            } else {
                trans.error().log(errMsg.toMsg(acf));
            }
        } finally {
            tt.done();
        }

    }
    
    private static void keypairGen(final Trans trans, final PropAccess access, final Deque<String> cmds) throws IOException {
        final String fqi = fqi(cmds);
        final String ns = FQI.reverseDomain(fqi);
        File dir = new File(access.getProperty(Config.CADI_ETCDIR,".")); // default to current Directory
        File f = new File(dir,ns+".key");
        
        if (f.exists()) {
            String line = AAFSSO.cons.readLine("%s exists. Overwrite? (y/n): ", f.getCanonicalPath());
            if (!"Y".equalsIgnoreCase(line)) {
                System.out.println("Canceling...");
                return;
            }
        }
        
        KeyPair kp = Factory.generateKeyPair(trans);
        ArtifactDir.write(f, Chmod.to400, Factory.toString(trans, kp.getPrivate()));
        System.out.printf("Wrote %s\n", f.getCanonicalFile());

        f=new File(dir,ns+".pubkey");
        ArtifactDir.write(f, Chmod.to644, Factory.toString(trans, kp.getPublic()));
        System.out.printf("Wrote %s\n", f.getCanonicalFile());
    }
    
    private static void config(Trans trans, PropAccess propAccess, String[] args, Deque<String> cmds) throws Exception {
        TimeTaken tt = trans.start("Get Configuration", Env.REMOTE);
        try {
	        final String fqi = fqi(cmds);
	        Artifact arti = new Artifact();
	        arti.setDir(propAccess.getProperty(Config.CADI_ETCDIR, "."));
	        arti.setNs(FQI.reverseDomain(fqi));
	        
            PropHolder loc = PropHolder.get(arti, "location.props");
            PropHolder cred = PropHolder.get(arti,"cred.props");
            PropHolder app= PropHolder.get(arti,"props");
            for(String c : args) {
            	int idx = c.indexOf('=');
            	if(idx>0) {
            		app.add(c.substring(0,idx), c.substring(idx+1));
            	}
            }
            app.add(Config.CADI_PROP_FILES, loc.getPath()+':'+cred.getPath());

            for (String tag : LOC_TAGS) {
            	loc.add(tag, getProperty(propAccess, trans, false, tag, "%s: ",tag));
            }
            
            cred.add(Config.CADI_KEYFILE, cred.getKeyPath());
            final String ssoAppID = propAccess.getProperty(Config.AAF_APPID);
            if(fqi.equals(ssoAppID)) {
            	cred.addEnc(Config.AAF_APPPASS, propAccess, null);
            // only Ask for Password when starting scratch
            } else if(propAccess.getProperty(Config.CADI_PROP_FILES)==null) {
            	char[] pwd = AAFSSO.cons.readPassword("Password for %s: ", fqi);
            	if(pwd.length>0) {
            		cred.addEnc(Config.AAF_APPPASS, new String(pwd));
            	}
            }
            
            app.add(Config.AAF_LOCATE_URL, propAccess, null);
            for(Entry<Object, Object> aaf_loc_prop : propAccess.getProperties().entrySet()) {
            	String key = aaf_loc_prop.getKey().toString();
            	if(key.startsWith("aaf_locator")) {
            		app.add(key, aaf_loc_prop.getValue().toString());
            	}
            }
            
            app.add(Config.AAF_APPID, fqi);
            app.add(Config.AAF_URL, propAccess, Config.AAF_URL_DEF);

            String cts = propAccess.getProperty(Config.CADI_TRUSTSTORE);
            if (cts!=null) {
                File origTruststore = new File(cts);
                if (!origTruststore.exists()) {
                    // Try same directory as cadi_prop_files
                    String cpf = propAccess.getProperty(Config.CADI_PROP_FILES);
                    if (cpf!=null) {
                        for (String f : Split.split(File.pathSeparatorChar, cpf)) {
                            File fcpf = new File(f);
                            if (fcpf.exists()) {
                                int lastSep = cts.lastIndexOf(File.pathSeparator);
                                origTruststore = new File(fcpf.getParentFile(),lastSep>=0?cts.substring(lastSep):cts);
                                if (origTruststore.exists()) { 
                                    break;
                                }
                            }
                        }
                        if (!origTruststore.exists()) {
                            throw new CadiException(cts + " does not exist");
                        }
                    }
                    
                }
                File newTruststore = new File(app.getDir(),origTruststore.getName());
                if (!newTruststore.exists()) {
                    Files.copy(origTruststore.toPath(), newTruststore.toPath());
                }
                
                cred.add(Config.CADI_TRUSTSTORE, propAccess, newTruststore.getCanonicalPath());
                cred.addEnc(Config.CADI_TRUSTSTORE_PASSWORD, propAccess, "changeit" /* Java default */);
                    
                String cpf = propAccess.getProperty(Config.CADI_PROP_FILES);
                if (cpf!=null){
                    for (String f : Split.split(File.pathSeparatorChar, cpf)) {
                        System.out.format("Reading %s\n",f);
                        FileInputStream fis = new FileInputStream(f); 
                        try {
                            Properties props = new Properties();
                            props.load(fis);
                            for (Entry<Object, Object> prop : props.entrySet()) {
                            	boolean lower = true;
                            	String key = prop.getKey().toString();
                            	for(int i=0;lower && i<key.length();++i) {
                            		if(Character.isUpperCase(key.charAt(i))) {
                            			lower = false;
                            		}
                            	}
                            	if(lower) {
                                	PropHolder ph = CRED_TAGS.contains(key)?cred:app;
                                	if(key.endsWith("_password")) {
                                		ph.addEnc(key, prop.getValue().toString());
                                	} else {
                                		ph.add(key, prop.getValue().toString());
                                	}
                            	}
                            }
                        } finally {
                            fis.close();
                        }
                    }
                } else {
                    aafcon = aafcon(propAccess);
                    if (aafcon!=null) { // get Properties from Remote AAF
                        final String locator = getProperty(propAccess,aafcon.env,false,Config.AAF_LOCATE_URL,"AAF Locator URL: ");

                        Future<Configuration> acf = aafcon.client(new SingleEndpointLocator(locator))
                                .read("/configure/"+fqi+"/aaf", configDF);
                        if (acf.get(TIMEOUT)) {
                            for (Props props : acf.value.getProps()) {
                            	PropHolder ph = CRED_TAGS.contains(props.getTag())?cred:app;
                            	if(props.getTag().endsWith("_password")) {
                            		ph.addEnc(props.getTag(), props.getValue());
                            	} else {
                            		ph.add(props.getTag(), props.getValue());
                            	}
                            }
                        } else if (acf.code()==401){
                            trans.error().log("Bad Password sent to AAF");
                        } else if (acf.code()==404){
                            trans.error().log("This version of AAF does not support remote Properties");
                        } else {
                            trans.error().log(errMsg.toMsg(acf));
                        }
                    }
                }
            }
            
            PropHolder.writeAll();
        } finally {
            tt.done();
        }
    }


    private static List<String> LOC_TAGS = Arrays.asList(new String[] {Config.CADI_LATITUDE, Config.CADI_LONGITUDE});
    
    private static void validate(final PropAccess pa) throws LocatorException, CadiException, APIException {
        System.out.println("Validating Configuration...");
        final AAFCon<?> aafcon = new AAFConHttp(pa,Config.AAF_URL,new SecurityInfoC<HttpURLConnection>(pa));
        aafcon.best(new Retryable<Void>() {
            @Override
            public Void code(Rcli<?> client) throws CadiException, ConnectException, APIException {
                Future<Perms> fc = client.read("/authz/perms/user/"+aafcon.defID(),permDF);
                if (fc.get(aafcon.timeout)) {
                    System.out.print("Success connecting to ");
                    System.out.println(client.getURI());
                    System.out.print("   Permissions for ");
                    System.out.println(aafcon.defID());
                    for (Perm p : fc.value.getPerm()) {
                        System.out.print('\t');
                        System.out.print(p.getType());
                        System.out.print('|');
                        System.out.print(p.getInstance());
                        System.out.print('|');
                        System.out.println(p.getAction());
                    }
                } else {
                    System.err.println("Error: " + fc.code() + ' ' + fc.body());
                }
                return null;
            }
        });
    }

    /**
     * Check returns Error Codes, so that Scripts can know what to do
     * 
     *   0 - Check Complete, nothing to do
     *   1 - General Error
     *   2 - Error for specific Artifact - read check.msg
     *   10 - Certificate Updated - check.msg is email content
     *   
     * @param trans
     * @param aafcon
     * @param cmds
     * @return
     * @throws Exception
     */
    private static int check(Trans trans, AAFCon<?> aafcon, Deque<String> cmds) throws Exception {
        int exitCode=1;
        String mechID = fqi(cmds);
        String machine = machine(cmds);
        
        TimeTaken tt = trans.start("Check Certificate", Env.REMOTE);
        try {
        
            Future<Artifacts> acf = aafcon.client(CM_VER)
                    .read("/cert/artifacts/"+mechID+'/'+machine, artifactsDF);
            if (acf.get(TIMEOUT)) {
                // Have to wait for JDK 1.7 source...
                //switch(artifact.getType()) {
                if (acf.value.getArtifact()==null || acf.value.getArtifact().isEmpty()) {
                    AAFSSO.cons.printf("No Artifacts found for %s on %s", mechID, machine);
                } else {
                    String id = aafcon.defID();
                    GregorianCalendar now = new GregorianCalendar();
                    for (Artifact a : acf.value.getArtifact()) {
                        if (id.equals(a.getMechid())) {
                            File dir = new File(a.getDir());
                            Properties props = new Properties();
                            FileInputStream fis = new FileInputStream(new File(dir,a.getNs()+".props"));
                            try {
                                props.load(fis);
                            } finally {
                                fis.close();
                            }
                            
                            String prop;                        
                            File f;
    
                            if ((prop=trans.getProperty(Config.CADI_KEYFILE))==null ||
                                !(f=new File(prop)).exists()) {
                                    trans.error().printf("Keyfile must exist to check Certificates for %s on %s",
                                        a.getMechid(), a.getMachine());
                            } else {
                                String ksf = trans.getProperty(Config.CADI_KEYSTORE);
                                String ksps = trans.getProperty(Config.CADI_KEYSTORE_PASSWORD);
                                if (ksf==null || ksps == null) {
                                    trans.error().printf("Properties %s and %s must exist to check Certificates for %s on %s",
                                            Config.CADI_KEYSTORE, Config.CADI_KEYSTORE_PASSWORD,a.getMechid(), a.getMachine());
                                } else {
                                    Symm symm = ArtifactDir.getSymm(f);

                                    KeyStore ks = KeyStore.getInstance("JKS");
                                    
                                    fis = new FileInputStream(ksf);
                                    try {
                                        ks.load(fis,symm.depass(ksps).toCharArray());
                                    } finally {
                                        fis.close();
                                    }
                                    X509Certificate cert = (X509Certificate)ks.getCertificate(mechID);
                                    String msg = null;

                                    if (cert==null) {
                                        msg = String.format("X509Certificate does not exist for %s on %s in %s",
                                                a.getMechid(), a.getMachine(), ksf);
                                        trans.error().log(msg);
                                        exitCode = 2;
                                    } else {
                                        GregorianCalendar renew = new GregorianCalendar();
                                        renew.setTime(cert.getNotAfter());
                                        renew.add(GregorianCalendar.DAY_OF_MONTH,-1*a.getRenewDays());
                                        if (renew.after(now)) {
                                            msg = String.format("X509Certificate for %s on %s has been checked on %s. It expires on %s; it will not be renewed until %s.\n", 
                                                    a.getMechid(), a.getMachine(),Chrono.dateOnlyStamp(now),cert.getNotAfter(),Chrono.dateOnlyStamp(renew));
                                            trans.info().log(msg);
                                            exitCode = 0; // OK
                                        } else {
                                            trans.info().printf("X509Certificate for %s on %s expiration, %s, needs Renewal.\n", 
                                                    a.getMechid(), a.getMachine(),cert.getNotAfter());
                                            cmds.offerLast(mechID);
                                            cmds.offerLast(machine);
                                            if (placeCerts(trans,aafcon,cmds)) {
                                                msg = String.format("X509Certificate for %s on %s has been renewed. Ensure services using are refreshed.\n", 
                                                        a.getMechid(), a.getMachine());
                                                exitCode = 10; // Refreshed
                                            } else {
                                                msg = String.format("X509Certificate for %s on %s attempted renewal, but failed. Immediate Investigation is required!\n", 
                                                        a.getMechid(), a.getMachine());
                                                exitCode = 1; // Error Renewing
                                            }
                                        }
                                    }
                                    if (msg!=null) {
                                        FileOutputStream fos = new FileOutputStream(a.getDir()+'/'+a.getNs()+".msg");
                                        try {
                                            fos.write(msg.getBytes());
                                        } finally {
                                            fos.close();
                                        }
                                    }
                                }
                                
                            }
                        }
                    }
                }
            } else {
                trans.error().log(errMsg.toMsg(acf));
                exitCode=1;
            }
        } finally {
            tt.done();
        }
        return exitCode;
    }

}
            
        


