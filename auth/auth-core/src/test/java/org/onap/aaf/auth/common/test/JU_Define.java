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
package org.onap.aaf.auth.common.test;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.onap.aaf.cadi.CadiException;
import org.onap.aaf.cadi.config.Config;
import org.onap.aaf.misc.env.Env;
import static org.junit.Assert.*;

//import com.att.authz.common.Define;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
//TODO: Gabe [JUnit] class path/ class Define is missing, com.att.authz.common also missing
public class JU_Define {
	//Define define;
	public static String ROOT_NS="NS.Not.Set";
	public static String ROOT_COMPANY=ROOT_NS;
	
	@Mock 
	Env envMock;
	
	
//	@Before
//	public void setUp(){
//		define = new Define();
//	}
//
//	@Test
//	public void testSet() throws CadiException {
//		PowerMockito.when(envMock.getProperty(Config.AAF_ROOT_NS)).thenReturn("aaf_root_ns");
//		PowerMockito.when(envMock.getProperty(Config.AAF_ROOT_COMPANY)).thenReturn("aaf_root_company");
//		//PowerMockito.when(envMock.init().log()).thenReturn(null);
//		//PowerMockito.doNothing().doThrow(new CadiException()).when(envMock).init().log(Matchers.anyString());
//		//define.set(envMock);
//	}

	@Test
	public void netYetTested() {
		fail("Tests not yet implemented");
	}
}
