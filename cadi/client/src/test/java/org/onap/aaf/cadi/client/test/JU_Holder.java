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
package org.onap.aaf.cadi.client.test;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import org.junit.Test;

import org.onap.aaf.cadi.client.Holder;

public class JU_Holder {

	@Test
	public void test() {
		String str1 = "a string";
		String str2 = "another string";
		Holder<String> holder = new Holder<String>(str1);
		assertThat(holder.get(), is(str1));
		assertThat(holder.toString(), is(str1));
		
		holder.set(str2);
		assertThat(holder.get(), is(str2));
		assertThat(holder.toString(), is(str2));
	}

}
