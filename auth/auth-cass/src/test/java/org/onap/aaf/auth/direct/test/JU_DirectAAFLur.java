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
package org.onap.aaf.auth.direct.test;

import static org.junit.Assert.assertTrue;

import java.security.Principal;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.aaf.auth.dao.hl.Question;
import org.onap.aaf.auth.direct.DirectAAFLur;
import org.onap.aaf.auth.env.AuthzEnv;
import org.onap.aaf.cadi.Permission;
import org.powermock.modules.junit4.PowerMockRunner;
@RunWith(PowerMockRunner.class)
public class JU_DirectAAFLur {
	
public static AuthzEnv env;
public static Question question;
public DirectAAFLur directAAFLur;



	@Before
	public void setUp()
	{
	directAAFLur = new DirectAAFLur(env, question);	
	}
	
	@Test
	public void testFish()
	{
		
	Principal bait = null;
	Permission pond=null;
	directAAFLur.fish(bait, pond);	
	
	assertTrue(true);
		
	}
	
}
