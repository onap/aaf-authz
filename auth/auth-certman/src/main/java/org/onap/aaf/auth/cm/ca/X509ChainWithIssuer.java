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
package org.onap.aaf.auth.cm.ca;

import java.io.IOException;
import java.io.Reader;
import java.security.Principal;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;

import org.onap.aaf.cadi.cm.CertException;
import org.onap.aaf.cadi.cm.Factory;

public class X509ChainWithIssuer extends X509andChain {
	private String issuerDN;

	public X509ChainWithIssuer(X509ChainWithIssuer orig, X509Certificate x509) {
		super(x509,orig.trustChain);
		issuerDN=orig.issuerDN;		
	}
	
	public X509ChainWithIssuer(final List<? extends Reader> rdrs) throws IOException, CertException {
		// Trust Chain.  Last one should be the CA
		Collection<? extends Certificate> certs;
		X509Certificate x509;
		for(Reader rdr : rdrs) {
			if(rdr!=null) { // cover for badly formed array
				byte[] bytes = Factory.decode(rdr);
				try {
					certs = Factory.toX509Certificate(bytes);
				} catch (CertificateException e) {
					throw new CertException(e);
				}
				for(Certificate c : certs) {
					x509=(X509Certificate)c;
					Principal subject = x509.getSubjectDN();
					if(subject!=null) {
						if(cert==null) { // first in Trust Chain
							issuerDN= subject.toString();
						}
						addTrustChainEntry(x509);
						cert=x509; // adding each time makes sure last one is signer.
					}
				}
			}
		}
	}
	
	public String getIssuerDN() {
		return issuerDN;
	}

}