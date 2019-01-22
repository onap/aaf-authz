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

package org.onap.aaf.cadi;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import org.onap.aaf.cadi.config.Config;
import org.onap.aaf.cadi.config.SecurityInfo;

public class PropAccess implements Access {
    // Sonar says cannot be static... it's ok.  not too many PropAccesses created.
    private final SimpleDateFormat iso8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    public static final Level DEFAULT = Level.AUDIT;
    
    private Symm symm;
    private int level;
    private Properties props;
    private List<String> recursionProtection = null;
    private LogIt logIt;
    private String name;

    public PropAccess() {
        logIt = new StreamLogIt(System.out);
        init(null);
    }
    
    /**
     * This Constructor soly exists to instantiate Servlet Context Based Logging that will call "init" later.
     * @param sc
     */
    protected PropAccess(Object o) {
        logIt = new StreamLogIt(System.out);
        props = new Properties();
    }
    
    public PropAccess(String ... args) {
        this(System.out,args);
    }
    
    public PropAccess(PrintStream ps, String[] args) {
        logIt = new StreamLogIt(ps==null?System.out:ps);
        init(logIt,args);
    }
    
    public PropAccess(LogIt logit, String[] args) {
        init(logit, args);
    }
    
    public PropAccess(Properties p) {
        this(System.out,p);
    }
    
    public PropAccess(PrintStream ps, Properties p) {
        logIt = new StreamLogIt(ps==null?System.out:ps);
        init(p);
    }
    
    protected void init(final LogIt logIt, final String[] args) {
        this.logIt = logIt;
        Properties nprops=new Properties();
        int eq;
        for (String arg : args) {
            if ((eq=arg.indexOf('='))>0) {
                nprops.setProperty(arg.substring(0, eq),arg.substring(eq+1));
            }
        }
        init(nprops);
    }
    
    protected void init(Properties p) {
        // Make sure these two are set before any changes in Logging
        name = "cadi";
        level=DEFAULT.maskOf();
        
        props = new Properties();
        // First, load related System Properties
        for (Entry<Object,Object> es : System.getProperties().entrySet()) {
            String key = es.getKey().toString();
            for (String start : new String[] {"HOSTNAME","cadi_","aaf_","cm_"}) {
                if (key.startsWith(start)) {
                    props.put(key, es.getValue());
                }
            }            
        }
        // Second, overlay or fill in with Passed in Props
        if (p!=null) {
            props.putAll(p);
        }
        
        // Third, load any Chained Property Files
        load(props.getProperty(Config.CADI_PROP_FILES));
        
        String sLevel = props.getProperty(Config.CADI_LOGLEVEL); 
        if (sLevel!=null) {
            level=Level.valueOf(sLevel).maskOf(); 
        }
        // Setup local Symmetrical key encryption
        if (symm==null) {
            try {
                symm = Symm.obtain(this);
            } catch (CadiException e) {
                System.err.append("FATAL ERROR: Cannot obtain Key Information.");
                e.printStackTrace(System.err);
                System.exit(1);
            }
        }
        
        name = props.getProperty(Config.CADI_LOGNAME, name);
        
        specialConversions();
    }

    private void specialConversions() {
        // Critical - if no Security Protocols set, then set it.  We'll just get messed up if not
        if (props.get(Config.CADI_PROTOCOLS)==null) {
            props.setProperty(Config.CADI_PROTOCOLS, SecurityInfo.HTTPS_PROTOCOLS_DEFAULT);
        }
        
        Object temp;
        temp=props.get(Config.CADI_PROTOCOLS);
        if (props.get(Config.HTTPS_PROTOCOLS)==null && temp!=null) {
            props.put(Config.HTTPS_PROTOCOLS, temp);
        }
        
        if (temp!=null) {
            if ("1.7".equals(System.getProperty("java.specification.version")) 
                    && (temp==null || (temp instanceof String && ((String)temp).contains("TLSv1.2")))) {
                System.setProperty(Config.HTTPS_CIPHER_SUITES, Config.HTTPS_CIPHER_SUITES_DEFAULT);
            }
        }
    }

    private void load(String cadi_prop_files) {
        if (cadi_prop_files==null) {
            return;
        }
        String prevKeyFile = props.getProperty(Config.CADI_KEYFILE);
        int prev = 0, end = cadi_prop_files.length();
        int idx;
        String filename;
        while (prev<end) {
            idx = cadi_prop_files.indexOf(File.pathSeparatorChar,prev);
            if (idx<0) {
                idx = end;
            }
            File file = new File(filename=cadi_prop_files.substring(prev,idx));
            if (file.exists()) {
                printf(Level.INIT,"Loading CADI Properties from %s",file.getAbsolutePath());
                try {
                    FileInputStream fis = new FileInputStream(file);
                    try {
                        props.load(fis);
                        // Recursively Load
                        String chainProp = props.getProperty(Config.CADI_PROP_FILES);
                        if (chainProp!=null) {
                            if (recursionProtection==null) {
                                recursionProtection = new ArrayList<>();
                                recursionProtection.add(cadi_prop_files);
                            }
                            if (!recursionProtection.contains(chainProp)) {
                                recursionProtection.add(chainProp);
                                load(chainProp); // recurse
                            }
                        }
                    } finally {
                        fis.close();
                    }
                } catch (Exception e) {
                    log(e,filename,"cannot be opened");
                }
            } else {
                printf(Level.WARN,"Warning: recursive CADI Property %s does not exist",file.getAbsolutePath());
            }
            prev = idx+1;
        }
        
        // Trim 
        for (Entry<Object, Object> es : props.entrySet()) {
            Object value = es.getValue();
            if (value instanceof String) {
                String trim = ((String)value).trim();
                // Remove Beginning/End Quotes, which might be there if mixed with Bash Props
                int s = 0, e=trim.length()-1;
                if (s<e && trim.charAt(s)=='"' && trim.charAt(e)=='"') {
                    trim=trim.substring(s+1,e);
                }
                if (trim!=value) { // Yes, I want OBJECT equals
                    props.setProperty((String)es.getKey(), trim);
                }
            }
        }
        // Reset Symm if Keyfile Changes:
        String newKeyFile = props.getProperty(Config.CADI_KEYFILE);
        if ((prevKeyFile!=null && newKeyFile!=null) || (newKeyFile!=null && !newKeyFile.equals(prevKeyFile))) {
            try {
                symm = Symm.obtain(this);
            } catch (CadiException e) {
                System.err.append("FATAL ERROR: Cannot obtain Key Information.");
                e.printStackTrace(System.err);
                System.exit(1);
            }

            prevKeyFile=newKeyFile;
        }
        
        String loglevel = props.getProperty(Config.CADI_LOGLEVEL);
        if (loglevel!=null) {
            try {
                level=Level.valueOf(loglevel).maskOf();
            } catch (IllegalArgumentException e) {
                printf(Level.ERROR,"%s=%s is an Invalid Log Level",Config.CADI_LOGLEVEL,loglevel);
            }
        }
        
        specialConversions();
    }
    
    @Override
    public void load(InputStream is) throws IOException {
        props.load(is);
        load(props.getProperty(Config.CADI_PROP_FILES));
    }

    @Override
    public void log(Level level, Object ... elements) {
        if (willLog(level)) {
            logIt.push(level,elements);
        }
    }

    protected StringBuilder buildMsg(Level level, Object[] elements) {
        return buildMsg(name,iso8601,level,elements);
    }

    public static StringBuilder buildMsg(final String name, final SimpleDateFormat sdf, Level level, Object[] elements) { 
        StringBuilder sb = new StringBuilder(sdf.format(new Date()));
        sb.append(' ');
        sb.append(level.name());
        sb.append(" [");
        sb.append(name);
        
        int end = elements.length;
        if (end<=0) {
            sb.append("] ");
        } else {
            int idx = 0;
            if(elements[idx]!=null  && 
            	elements[idx] instanceof Integer) {
                sb.append('-');
                sb.append(elements[idx]);
                ++idx;
            }
            sb.append("] ");
            write(true,sb,elements);
        }
        return sb;
    }
    
    private static boolean write(boolean first, StringBuilder sb, Object[] elements) {
    	String s;
        for (Object o : elements) {
            if (o!=null) {
            	if(o.getClass().isArray()) {
            		first = write(first,sb,(Object[])o);
            	} else {
	                s=o.toString();
	                if (first) {
	                    first = false;
	                } else {
	                    int l = s.length();
	                    if (l>0)    {
	                        switch(s.charAt(l-1)) {
	                            case ' ':
	                                break;
	                            default:
	                                sb.append(' ');
	                        }
	                    }
	                }
	                sb.append(s);
            	}
            }
        }
        return first;
    }

    @Override
    public void log(Exception e, Object... elements) {
    	StringWriter sw = new StringWriter();
    	PrintWriter pw = new PrintWriter(sw);
    	pw.println();
    	e.printStackTrace(pw);
        log(Level.ERROR,elements,sw.toString());
    }

    @Override
    public void printf(Level level, String fmt, Object... elements) {
        if (willLog(level)) {
            log(level,String.format(fmt, elements));
        }
    }

    @Override
    public void setLogLevel(Level level) {
        this.level = level.maskOf();
    }

    @Override
    public boolean willLog(Level level) {
        return level.inMask(this.level);
    }

    @Override
    public ClassLoader classLoader() {
        return ClassLoader.getSystemClassLoader();
    }

    @Override
    public String getProperty(String tag, String def) {
        return props.getProperty(tag,def);
    }

    @Override
    public String decrypt(String encrypted, boolean anytext) throws IOException {
        return (encrypted!=null && (anytext==true || encrypted.startsWith(Symm.ENC)))
            ? symm.depass(encrypted)
            : encrypted;
    }
    
    public String encrypt(String unencrypted) throws IOException {
        return Symm.ENC+symm.enpass(unencrypted);
    }

    //////////////////
    // Additional
    //////////////////
    public String getProperty(String tag) {
        return props.getProperty(tag);
    }
    

    public Properties getProperties() {
        return props;
    }

    public void setProperty(String tag, String value) {
        if (value!=null) {
            props.put(tag, value);
            if (Config.CADI_KEYFILE.equals(tag)) {
                // reset decryption too
                try {
                    symm = Symm.obtain(this);
                } catch (CadiException e) {
                    System.err.append("FATAL ERROR: Cannot obtain Key Information.");
                    e.printStackTrace(System.err);
                    System.exit(1);
                }
            }
        }
    }

    public interface LogIt {
        public void push(Level level, Object ... elements) ;
    }
    
    private class StreamLogIt implements LogIt {
        private PrintStream ps;
        
        public StreamLogIt(PrintStream ps) {
            this.ps = ps;
        }
        @Override
        public void push(Level level, Object ... elements) {
            ps.println(buildMsg(level,elements));
            ps.flush();
        }
    }

    public void set(LogIt logit) {
        logIt = logit;
    }

    public void setStreamLogIt(PrintStream ps) {
        logIt = new StreamLogIt(ps);
    }

    public String toString() {
    	return props.toString();
    }
}
