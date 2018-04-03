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
package org.onap.aaf.cadi.test.taf;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import static org.mockito.Mockito.*;
import org.junit.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;

import org.onap.aaf.cadi.Access;
import org.onap.aaf.cadi.CachedPrincipal.Resp;
import org.onap.aaf.cadi.taf.TafResp;
import org.onap.aaf.cadi.taf.TafResp.RESP;

import org.onap.aaf.cadi.taf.NullTaf;

public class JU_NullTaf {

	// @Mock
	// LifeFore lfMock;

// 	@Before
// 	public void setup() throws Exception {
// 		MockitoAnnotations.initMocks(this);
// 	}

	@Test
	public void test() throws IOException {
		NullTaf nt = new NullTaf();
		TafResp singleton1 = nt.validate(null);
		TafResp singleton2 = nt.validate(null, null, null);
		Resp singleton3 = nt.revalidate(null, null);
		
		assertThat(singleton1, is(singleton2));
		
		assertFalse(singleton1.isValid());
		
		assertThat(singleton1.isAuthenticated(), is(RESP.NO_FURTHER_PROCESSING));
		
		assertThat(singleton1.desc(), is("All Authentication denied"));
		
		assertThat(singleton1.authenticate(), is(RESP.NO_FURTHER_PROCESSING));
		
		assertThat(singleton1.getPrincipal(), is(nullValue()));
		
		assertThat(singleton1.getAccess(), is(Access.NULL));
		
		assertTrue(singleton1.isFailedAttempt());

		assertThat(singleton3, is(Resp.NOT_MINE));
	}

}
