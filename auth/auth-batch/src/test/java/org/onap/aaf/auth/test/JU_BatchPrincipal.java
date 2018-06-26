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

package org.onap.aaf.auth.test;

import static org.junit.Assert.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.onap.aaf.auth.BatchPrincipal;

import junit.framework.Assert;

import static org.mockito.Mockito.*;
import org.junit.Test;

public class JU_BatchPrincipal {

	BatchPrincipal bPrincipal;
	
	@Test
	public void testBatchPrincipal() {
		bPrincipal = new BatchPrincipal("name");
		Assert.assertEquals("batch:name", bPrincipal.getName());
		Assert.assertEquals("Btch", bPrincipal.tag());
	}

}
