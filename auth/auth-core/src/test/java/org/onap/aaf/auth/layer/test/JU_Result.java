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
package org.onap.aaf.auth.layer.test;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.onap.aaf.auth.layer.Result;

public class JU_Result {
	Result result;
//	@Mock
//	RV value;
	int status=0;
	String details = "details"; 
	String[] variables;
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Before
	public void setUp(){
		//TODO: Gabe [JUnit] Not visible for junit
		//result = new Result(result, status, details, variables);
	}

//	@Test
//	public void testPartialContent() {
//		Result Res = result.partialContent(true);
//		System.out.println("Res" +Res);
//		assertEquals(details,Res.toString());
//		
//	}
	
	@Test
	public void netYetTested() {
		fail("Tests not yet implemented");
	}

}
