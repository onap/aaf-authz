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

package org.onap.aaf.auth.dao.cached;

import org.onap.aaf.auth.dao.CIDAO;
import org.onap.aaf.auth.dao.CachedDAO;
import org.onap.aaf.auth.dao.cass.NsDAO;
import org.onap.aaf.auth.env.AuthzTrans;

public class CachedNSDAO extends CachedDAO<AuthzTrans, NsDAO, NsDAO.Data> {
    public CachedNSDAO(NsDAO dao, CIDAO<AuthzTrans> info, long expiresIn) {
        super(dao, info, NsDAO.CACHE_SEG, expiresIn);
    }
}
