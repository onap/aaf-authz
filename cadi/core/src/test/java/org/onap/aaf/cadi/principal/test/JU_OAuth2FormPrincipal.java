/*******************************************************************************
 * ============LICENSE_START====================================================
 * * org.onap.aaf
 * * ===========================================================================
 * * Copyright © 2017 AT&T Intellectual Property. All rights reserved.
 * * ===========================================================================
 * * Licensed under the Apache License, Version 2.0 (the "License");
 * * you may not use this file except in compliance with the License.
 * * You may obtain a copy of the License at
 * * <p>
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 * * <p>
 *  * Unless required by applicable law or agreed to in writing, software
 * * distributed under the License is distributed on an "AS IS" BASIS,
 * * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * * See the License for the specific language governing permissions and
 * * limitations under the License.
 * * ============LICENSE_END====================================================
 * *
 * *
 ******************************************************************************/

package org.onap.aaf.cadi.principal.test;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import org.junit.*;

import org.onap.aaf.cadi.principal.OAuth2FormPrincipal;

public class JU_OAuth2FormPrincipal {

    private String username = "user";
    private String id = "id";

    @Test
    public void accessorsTest() {
        OAuth2FormPrincipal oauth = new OAuth2FormPrincipal(id, username);
        assertThat(oauth.getName(), is(username));
        assertThat(oauth.client_id(), is(id));
        assertThat(oauth.tag(), is("OAuth"));
    }

    @Test
    public void personalNameTest() {
        OAuth2FormPrincipal oauth = new OAuth2FormPrincipal(id, username);
        assertThat(oauth.personalName(), is(username + "|" + id));

        oauth = new OAuth2FormPrincipal(id, null);
        assertThat(oauth.personalName(), is(id));

        oauth = new OAuth2FormPrincipal(id, id);
        assertThat(oauth.personalName(), is(id));
    }

}
