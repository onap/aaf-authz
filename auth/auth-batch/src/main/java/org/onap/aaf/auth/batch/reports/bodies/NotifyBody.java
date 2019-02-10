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
package org.onap.aaf.auth.batch.reports.bodies;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.onap.aaf.auth.batch.reports.Notify;
import org.onap.aaf.auth.env.AuthzTrans;
import org.onap.aaf.cadi.Access;
import org.onap.aaf.misc.env.APIException;

public abstract class NotifyBody {
	private static final String DUPL = "<td style=\"text-indent: 4em;\">''</td>";
	private static final Map<String,NotifyBody> bodyMap = new HashMap<>();

	protected Map<String,List<List<String>>> rows;
	private final String name;
	private final String type;
	private String date;
	private int escalation;
	
	public NotifyBody(final String type, final String name) {
		rows = new TreeMap<>();
		this.name = name;
		this.type = type;
		date="";
		escalation = 1;
	}
	
	public void store(List<String> row) {
		if(!row.isEmpty()) {
			if("info".equals(row.get(0))) {
				if(row.size()>2) {
					date = row.get(2);
				}
				if(row.size()>3) {
					escalation = Integer.parseInt(row.get(3));
				}
				return;
			} else if(type.equals(row.get(0))) {
				String user = user(row);
				if(user!=null) {
					List<List<String>> lss = rows.get(user); 
					if(lss == null) {
						lss = new ArrayList<>();
						rows.put(user,lss);
					}
					lss.add(row);
				}
			}
		}
	}

	public String name() {
		return name;
	}
	
	public String date() {
		return date;
	}
	public int escalation() {
		return escalation;
	}
	
	public Set<String> users() {
		return rows.keySet();
	}
	
	/**
	 * ID must be set from Row for Email lookup
	 * 
	 * @param trans
	 * @param n
	 * @param id
	 * @param row
	 * @return
	 */
	public abstract boolean body(AuthzTrans trans, StringBuilder sb, int indent, Notify n, String id);
	
	/**
	 * Return "null" if user not found in row... Code will handle.
	 * @param row
	 * @return
	 */
	protected abstract String user(List<String> row);
	
	/**
	 * Get Notify Body based on key of
	 * type|name
	 */
	public static NotifyBody get(String key) {
		return bodyMap.get(key);
	}
	
	/**
	 * Return set of loaded NotifyBodies
	 * 
	 */
	public static Collection<NotifyBody> getAll() {
		// Note: The same Notify Body is entered several times with different keys.
		// Therefore, need a Set of Values, not all the Values.
		Set<NotifyBody> set = new HashSet<>();
		set.addAll(bodyMap.values());
		return set;
	}
	
	/**
	 * @param propAccess 
	 * @throws URISyntaxException 
	 * 
	 */
	public static void load(Access access) throws APIException, IOException {
		// class load available NotifyBodies
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		Package pkg = NotifyBody.class.getPackage();
		String path = pkg.getName().replace('.', '/');
		URL url = cl.getResource(path);
		if(url == null) {
			throw new APIException("Cannot load resources from " + path);
		}
		File dir;
		try {
			dir = new File(url.toURI());
		} catch (URISyntaxException e) {
			throw new APIException(e);
		}
		if(dir.exists()) {
			String[] files = dir.list();
			if(files!=null) {
				for(String sf : files) {
					int dot = sf.indexOf('.');
					if(dot>=0) {
						String cls = pkg.getName()+'.'+sf.substring(0,dot);
						try {
							Class<?> c = cl.loadClass(cls);
							if(c!=null) {
								if(!Modifier.isAbstract(c.getModifiers())) {
									Constructor<?> cst = c.getConstructor(Access.class);
									NotifyBody nb = (NotifyBody)cst.newInstance(access);
									if(nb!=null) {
										bodyMap.put("info|"+nb.name, nb);
										bodyMap.put(nb.type+'|'+nb.name, nb);
									}
								}
							}
						} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}
	

	protected void println(StringBuilder sb, int indent, Object ... objs) {
		for(int i=0;i<indent;++i) {
			sb.append(' ');
		}
		for(Object o : objs) {
			sb.append(o.toString());
		}
		sb.append('\n');
	}
	
	protected void printCell(StringBuilder sb, int indent, String current, String prev) {
		if(current.equals(prev)) {
			println(sb,indent,DUPL);
		} else {
			println(sb,indent,"<td>",current,"</td>");
		}
	}

}
