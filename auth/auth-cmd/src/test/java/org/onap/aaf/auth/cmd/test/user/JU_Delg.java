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
package org.onap.aaf.auth.cmd.test.user;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.junit.Assert.fail;

import org.junit.Before;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.onap.aaf.auth.cmd.AAFcli;
import org.onap.aaf.auth.env.AuthzEnv;
import org.onap.aaf.cadi.CadiException;
import org.onap.aaf.cadi.Locator;
import org.onap.aaf.cadi.LocatorException;
import org.onap.aaf.cadi.PropAccess;
import org.onap.aaf.cadi.SecuritySetter;
import org.onap.aaf.cadi.client.Future;
import org.onap.aaf.cadi.client.Rcli;
import org.onap.aaf.misc.env.APIException;
import org.onap.aaf.misc.rosetta.env.RosettaDF;

import aaf.v2_0.DelgRequest;

import org.onap.aaf.auth.cmd.user.Delg;
import org.onap.aaf.auth.cmd.user.User;
import org.onap.aaf.auth.cmd.test.HMangrStub;

public class JU_Delg {
	
	private Delg delg;

	@Mock private SecuritySetter<HttpURLConnection> ssMock;
	@Mock private Locator<URI> locMock;
	@Mock private Writer wrtMock;
	@Mock private Rcli<HttpURLConnection> clientMock;
	@Mock private Future<DelgRequest> delgRequestFutureMock;

	private PropAccess access;
	private HMangrStub hman;	
	private AuthzEnv aEnv;
	private AAFcli aafcli;
	
	@SuppressWarnings("unchecked")
	@Before
	public void setUp() throws NoSuchFieldException, SecurityException, Exception, IllegalAccessException {
		MockitoAnnotations.initMocks(this);

		when(clientMock.create(any(String.class), any(RosettaDF.class), any(DelgRequest.class))).thenReturn(delgRequestFutureMock);
		when(clientMock.delete(any(String.class), any(RosettaDF.class), any(DelgRequest.class))).thenReturn(delgRequestFutureMock);
		when(clientMock.update(any(String.class), any(RosettaDF.class), any(DelgRequest.class))).thenReturn(delgRequestFutureMock);

		hman = new HMangrStub(access, locMock, clientMock);
		access = new PropAccess(new PrintStream(new ByteArrayOutputStream()), new String[0]);
		aEnv = new AuthzEnv();
		aafcli = new AAFcli(access, aEnv, wrtMock, hman, null, ssMock);

		delg = new Delg(new User(aafcli));
	}

	@Test
	public void testAdd() throws APIException, LocatorException, CadiException, URISyntaxException {
		delg.exec(0, new String[] {"add", "id", "delegate"});

		when(delgRequestFutureMock.get(any(Integer.class))).thenReturn(true);
		delg.exec(0, new String[] {"add", "id", "delegate"});
		
		delg.exec(0, new String[] {"add", "id", "delegate", "2000-01-01"});
		
		try {
			delg.exec(0, new String[] {"add", "id", "delegate", "invalid date format"});
			fail("Should have thrown an exception");
		} catch (CadiException e) {
		}
	}
	
	@Test
	public void testUpd() throws APIException, LocatorException, CadiException, URISyntaxException {
		delg.exec(0, new String[] {"upd", "id", "delegate"});

		when(delgRequestFutureMock.get(any(Integer.class))).thenReturn(true);
		delg.exec(0, new String[] {"upd", "id", "delegate"});
		
		delg.exec(0, new String[] {"upd", "id", "delegate", "2000-01-01"});
		
		try {
			delg.exec(0, new String[] {"upd", "id", "delegate", "invalid date format"});
			fail("Should have thrown an exception");
		} catch (CadiException e) {
		}
	}
	
	@Test
	public void testDel() throws APIException, LocatorException, CadiException, URISyntaxException {
		delg.exec(0, new String[] {"del", "id"});

		when(delgRequestFutureMock.get(any(Integer.class))).thenReturn(true);
		delg.exec(0, new String[] {"del", "id"});
	}
	
	@Test
	public void testDetailedHelp() {
		StringBuilder sb = new StringBuilder();
		delg.detailedHelp(0, sb);
	}
}
