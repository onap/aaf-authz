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
 * *
 ******************************************************************************/
package org.onap.aaf.auth.cmd.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.onap.aaf.auth.cmd.AAFcli;
import org.onap.aaf.auth.cmd.BaseCmd;
import org.onap.aaf.cadi.CadiException;
import org.onap.aaf.cadi.LocatorException;
import org.onap.aaf.cadi.client.Future;
import org.onap.aaf.misc.env.APIException;

import aaf.v2_0.History;

@RunWith(MockitoJUnitRunner.class)
public class JU_BaseCmd {

	private static AAFcli cli;
	private static BaseCmd bCmd;

//	@BeforeClass
//	public static void setUp() throws APIException, LocatorException, GeneralSecurityException, IOException {
//		cli = JU_AAFCli.getAAfCli();
//		bCmd = new BaseCmd<>(cli, "testString");
//	}

//	@Test
//	public void exec() throws CadiException, APIException, LocatorException {
//		assertEquals(bCmd._exec(4, "add", "del", "reset", "extend"), 0);
//
//	}
//	
//	@Test
//	public void exec1() throws CadiException, APIException, LocatorException {
//		assertEquals(bCmd._exec(0, "add", "del", "reset", "extend"), 0);
//
//	}

//	@Test
//	public void error() throws CadiException, APIException, LocatorException {
//		boolean noError = true;
//		Future<String> future = new Future<String>() {
//
//			@Override
//			public boolean get(int timeout) throws CadiException {
//				// TODO Auto-generated method stub
//				return false;
//			}
//
//			@Override
//			public int code() {
//				// TODO Auto-generated method stub
//				return 0;
//			}
//
//			@Override
//			public String body() {
//				// TODO Auto-generated method stub
//				return "{%}";
//			}
//
//			@Override
//			public String header(String tag) {
//				// TODO Auto-generated method stub
//				return null;
//			}
//		};
//		try {
//			//TODO: Gabe [JUnit] Not visible for junit
//			bCmd.error(future);
//		} catch (Exception e) {
//			noError = false;
//		}
//		assertEquals(noError, true);
//
//	}
//
//
//
//	@Test
//	public void activity() throws DatatypeConfigurationException {
//		boolean noError = true;
//		History history = new History();
//		History.Item item = new History.Item();
//		item.setTarget("target");
//		item.setUser("user");
//		item.setMemo("memo");
//
//		GregorianCalendar c = new GregorianCalendar();
//		c.setTime(new Date());
//		XMLGregorianCalendar date = DatatypeFactory.newInstance().newXMLGregorianCalendar(c);
//		item.setTimestamp(date);
//		history.getItem().add(item);
//		try {
//			bCmd.activity(history, "history");
//		} catch (Exception e) {
//			noError = false;
//		}
//		assertEquals(noError, true);
//
//	}
//
//	@Test
//	public void activity1() throws DatatypeConfigurationException {
//		boolean noError = true;
//		History history = new History();
//		History.Item item = new History.Item();
//		item.setTarget("target");
//		item.setUser("user");
//		item.setMemo("memo");
//
//		GregorianCalendar c = new GregorianCalendar();
//		c.setTime(new Date());
//		XMLGregorianCalendar date = DatatypeFactory.newInstance().newXMLGregorianCalendar(c);
//		item.setTimestamp(date);
//		history.getItem().add(item);
//		try {
//			bCmd.activity(history, "1[]");
//		} catch (Exception e) {
//			noError = false;
//		}
//		assertEquals(noError, true);
//
//	}
//	
//
//
//	@Test
//	public void error1() {
//		boolean noError = true;
//		Future<String> future = new Future<String>() {
//
//			@Override
//			public boolean get(int timeout) throws CadiException {
//				// TODO Auto-generated method stub
//				return false;
//			}
//
//			@Override
//			public int code() {
//				// TODO Auto-generated method stub
//				return 0;
//			}
//
//			@Override
//			public String body() {
//				// TODO Auto-generated method stub
//				return "{<html><code>1</code></html>";
//			}
//
//			@Override
//			public String header(String tag) {
//				// TODO Auto-generated method stub
//				return null;
//			}
//		};
//		try {
//			bCmd.error(future);
//		} catch (Exception e) {
//			noError = false;
//		}
//		assertEquals(noError, true);
//
//	}

//	@Test
//	public void error2() {
//		boolean noError = true;
//		Future<String> future = new Future<String>() {
//
//			@Override
//			public boolean get(int timeout) throws CadiException {
//				// TODO Auto-generated method stub
//				return false;
//			}
//
//			@Override
//			public int code() {
//				// TODO Auto-generated method stub
//				return 0;
//			}
//
//			@Override
//			public String body() {
//				// TODO Auto-generated method stub
//				return "other";
//			}
//
//			@Override
//			public String header(String tag) {
//				// TODO Auto-generated method stub
//				return null;
//			}
//		};
//		try {
//			bCmd.error(future);
//		} catch (Exception e) {
//			noError = false;
//		}
//		assertEquals(noError, true);
//
//	}
	
	@Test						//TODO: Temporary fix AAF-111
	public void netYetTested() {
		fail("Tests not yet implemented");
	}

}
