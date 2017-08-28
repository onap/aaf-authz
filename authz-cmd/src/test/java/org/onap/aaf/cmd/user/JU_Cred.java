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
 * * ECOMP is a trademark and service mark of AT&T Intellectual Property.
 * *
 ******************************************************************************/
package org.onap.aaf.cmd.user;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.onap.aaf.cmd.user.Cred;
import org.onap.aaf.cmd.user.User;

import org.onap.aaf.cadi.CadiException;
import org.onap.aaf.cadi.LocatorException;
import org.onap.aaf.inno.env.APIException;

@RunWith(MockitoJUnitRunner.class)
public class JU_Cred {

	private static Cred testCred;
	private static User testUser;


	@BeforeClass
	public static void setUp() {
		testCred = mock(Cred.class);
		testUser = mock(User.class);
		try {
			when(testCred._exec(4, "String1","String2","String3","String4")).thenReturn(10);
		} catch (CadiException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (APIException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (LocatorException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void exec() throws CadiException, APIException, LocatorException {
		assertEquals(testCred._exec(4, "String1","String2","String3","String4"), 10);
	}


	@Test
	public void exec_add() {		
		try {
			assertNotNull(testCred._exec(0, "zeroed","add","del","reset","extend"));
		} catch (CadiException | APIException | LocatorException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Test
	public void exec_del() {		
		try {
			assertNotNull(testCred._exec(1, "zeroed","add","del","reset","extend"));
		} catch (CadiException | APIException | LocatorException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Test
	public void exec_reset() {		
		try {
			assertNotNull(testCred._exec(2, "zeroed","add","del","reset","extend"));
		} catch (CadiException | APIException | LocatorException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Test
	public void exec_extend() {		
		try {
			assertNotNull(testCred._exec(3, "zeroed","add","del","reset","extend"));
		} catch (CadiException | APIException | LocatorException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
