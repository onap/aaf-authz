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
package org.onap.aaf.cadi.cm.test;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import static org.mockito.Mockito.*;
import org.junit.*;
import org.mockito.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.KeyPair;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.crypto.Cipher;

import org.onap.aaf.cadi.cm.CertException;
import org.onap.aaf.cadi.cm.Factory;
import org.onap.aaf.cadi.cm.Factory.StripperInputStream;
import org.onap.aaf.misc.env.Env;
import org.onap.aaf.misc.env.LogTarget;
import org.onap.aaf.misc.env.TimeTaken;
import org.onap.aaf.misc.env.Trans;

public class JU_Factory {

	@Mock
	Trans transMock;

	@Mock
	TimeTaken timeTakenMock;

	@Mock
	LogTarget logTargetMock;

	@Mock
	X509Certificate x509CertMock;

	@Mock
	Certificate certMock;

	@Mock
	Principal subjectDN;

	private final String resourceDirName = "src/test/resources";
	private File resourceDir;
	private File publicKeyFile;
	private File privateKeyFile;
	private File certFile;
	
	private static final String message = "The quick brown fox jumps over the lazy dog.";

	private static final String subjectDNText = "subjectDN";
	private static final String certText = "Some text that might be included in a certificate";

	@Before
	public void setup() throws CertificateEncodingException {
		MockitoAnnotations.initMocks(this);

		resourceDir = new File(resourceDirName);
		resourceDir.mkdirs();
		publicKeyFile = new File(resourceDirName, "/publicKey");
		privateKeyFile = new File(resourceDirName, "/privateKey");
		publicKeyFile.delete();
		privateKeyFile.delete();

		certFile = new File(resourceDirName + "/exampleCertificate.cer");

		when(transMock.start(anyString(), anyInt())).thenReturn(timeTakenMock);
		when(transMock.debug()).thenReturn(logTargetMock);

		when(subjectDN.toString()).thenReturn(subjectDNText);

		when(x509CertMock.getSubjectDN()).thenReturn(subjectDN);
		when(x509CertMock.getEncoded()).thenReturn(certText.getBytes());

		when(certMock.getEncoded()).thenReturn(certText.getBytes());
	}

	@After
	public void tearDown() {
		publicKeyFile = new File(resourceDirName, "/publicKey");
		privateKeyFile = new File(resourceDirName, "/privateKey");
		publicKeyFile.delete();
		privateKeyFile.delete();

		if (resourceDir.list().length == 0) {
			resourceDir.delete();
		}
	}

	@Test
	public void generateKeyPairTest() throws Exception {
		// This instatiation isn't actually necessary, but it gets coverage
		Cipher encryptor = Factory.pkCipher();
		Cipher decryptor = Factory.pkCipher();

		KeyPair kp1 = Factory.generateKeyPair(transMock);
		encryptor = Factory.pkCipher(kp1.getPublic(), true);
		decryptor = Factory.pkCipher(kp1.getPrivate(), false);
		byte[] encrypedMessage1 = encryptor.doFinal(message.getBytes(StandardCharsets.UTF_8));
		String output1 = new String(decryptor.doFinal(encrypedMessage1));
		assertThat(output1, is(message));

		// coverage
		when(transMock.start("Generate KeyPair", Env.SUB)).thenReturn(null);
		KeyPair kp2 = Factory.generateKeyPair(transMock);
		encryptor = Factory.pkCipher(kp2.getPublic(), true);
		decryptor = Factory.pkCipher(kp2.getPrivate(), false);
		byte[] encrypedMessage2 = encryptor.doFinal(message.getBytes(StandardCharsets.UTF_8));
		String output2 = new String(decryptor.doFinal(encrypedMessage2));
		assertThat(output2, is(message));

		KeyPair kp3 = Factory.generateKeyPair(null);
		encryptor = Factory.pkCipher(kp3.getPublic(), true);
		decryptor = Factory.pkCipher(kp3.getPrivate(), false);
		byte[] encrypedMessage3 = encryptor.doFinal(message.getBytes(StandardCharsets.UTF_8));
		String output3 = new String(decryptor.doFinal(encrypedMessage3));
		assertThat(output3, is(message));
	}

	@Test
	public void keyStringManipTest() throws Exception {
		KeyPair kp = Factory.generateKeyPair(transMock);

		String publicKeyString = Factory.toString(transMock, kp.getPublic());
		String privateKeyString = Factory.toString(transMock, kp.getPrivate());

		assertThat(publicKeyString.startsWith("-----BEGIN PUBLIC KEY-----"), is(true));
		assertThat(publicKeyString.endsWith("-----END PUBLIC KEY-----\n"), is(true));

		assertThat(privateKeyString.startsWith("-----BEGIN PRIVATE KEY-----"), is(true));
		assertThat(privateKeyString.endsWith("-----END PRIVATE KEY-----\n"), is(true));

		PublicKey publicKey = Factory.toPublicKey(transMock, cleanupString(publicKeyString));
		PrivateKey privateKey = Factory.toPrivateKey(transMock, cleanupString(privateKeyString));

		Cipher encryptor = Factory.pkCipher(publicKey, true);
		Cipher decryptor = Factory.pkCipher(privateKey, false);
		byte[] encrypedMessage = encryptor.doFinal(message.getBytes(StandardCharsets.UTF_8));
		String output = new String(decryptor.doFinal(encrypedMessage));
		assertThat(output, is(message));
	}

	@Test
	public void keyFileManipTest() throws Exception {
		KeyPair kp = Factory.generateKeyPair(transMock);

		String privateKeyString = Factory.toString(transMock, kp.getPrivate());
		writeToFile(privateKeyFile, privateKeyString, "Header:this line has a header");

		PublicKey publicKey = kp.getPublic();
		PrivateKey privateKey = Factory.toPrivateKey(transMock, privateKeyFile);

		Cipher encryptor = Factory.pkCipher(publicKey, true);
		Cipher decryptor = Factory.pkCipher(privateKey, false);
		byte[] encrypedMessage = encryptor.doFinal(message.getBytes(StandardCharsets.UTF_8));
		String output = new String(decryptor.doFinal(encrypedMessage));
		assertThat(output, is(message));
	}

	@Test
	public void certToStringTest() throws IOException, CertException, CertificateEncodingException {
		String certString;
		when(logTargetMock.isLoggable()).thenReturn(true);

		certString = Factory.toString(transMock, x509CertMock);
		assertThat(certString.startsWith("-----BEGIN CERTIFICATE-----"), is(true));
		assertThat(certString.endsWith("-----END CERTIFICATE-----\n"), is(true));

		certString = Factory.toString(transMock, certMock);
		assertThat(certString.startsWith("-----BEGIN CERTIFICATE-----"), is(true));
		assertThat(certString.endsWith("-----END CERTIFICATE-----\n"), is(true));

		try {
			certString = Factory.toString(transMock, (Certificate)null);
			fail("Should have thrown an exception");
		} catch (CertException e) {
			assertThat(e.getMessage(), is("Certificate not built"));
		}

		when(certMock.getEncoded()).thenThrow(new CertificateEncodingException());
		try {
			certString = Factory.toString(transMock, certMock);
			fail("Should have thrown an exception");
		} catch (CertException e) {
		}

		// coverage
		when(logTargetMock.isLoggable()).thenReturn(false);
		certString = Factory.toString(transMock, x509CertMock);
	}

	@Test
	public void toX509Test() throws CertificateException, IOException, CertException {
		String output;
		Collection<? extends Certificate> certs;
		when(logTargetMock.isLoggable()).thenReturn(true);

		String certString = readFromFile(certFile, false);

		certs = Factory.toX509Certificate(certString);
		// Contrived way of getting a Certificate out of a Collection
		output = Factory.toString(transMock, certs.toArray(new Certificate[0])[0]);
		assertThat(output, is(certString));

		certs = Factory.toX509Certificate(transMock, certFile);
		// Contrived way of getting a Certificate out of a Collection
		output = Factory.toString(transMock, certs.toArray(new Certificate[0])[0]);
		assertThat(output, is(certString));

		List<String> certStrings = new ArrayList<String>();
		certStrings.add(certString);
		certStrings.add(certString);
		certs = Factory.toX509Certificate(certStrings);
		// Contrived way of getting a Certificate out of a Collection
		// it doesn't matter which one we get - they're the same
		output = Factory.toString(transMock, certs.toArray(new Certificate[0])[0]);
		assertThat(output, is(certString));
	}

	@Test
	public void stripperTest() throws Exception {
		KeyPair kp = Factory.generateKeyPair(transMock);
		String privateKeyString = Factory.toString(transMock, kp.getPrivate());
		writeToFile(privateKeyFile, privateKeyString, "Header:this line has a header");

		StripperInputStream stripper = new StripperInputStream(privateKeyFile);

		String expected = cleanupString(privateKeyString);
		byte[] buffer = new byte[10000];
		stripper.read(buffer);
		String output = new String(buffer, 0, expected.length());
		assertThat(output, is(expected));
		stripper.close();

		// coverage
		stripper = new StripperInputStream(new FileInputStream(privateKeyFile));
		stripper.close();
		stripper = new StripperInputStream(new BufferedReader(new FileReader(privateKeyFile)));
		stripper.close();
		stripper.close();  // also coverage...
	}

	@Test
	public void binaryTest() throws IOException {
		String output = new String(Factory.binary(certFile));
		String expected = readFromFile(certFile, true);
		assertThat(output, is(expected));
	}

	@Test
	public void signatureTest() throws Exception {
		KeyPair kp = Factory.generateKeyPair(transMock);
		String signedString = "Something that needs signing";
		byte[] signedBytes = Factory.sign(transMock, signedString.getBytes(), kp.getPrivate());
		String output = Factory.toSignatureString(signedBytes);
		assertThat(output.startsWith("-----BEGIN SIGNATURE-----"), is(true));
		assertThat(output.endsWith("-----END SIGNATURE-----\n"), is(true));
		assertThat(Factory.verify(transMock, signedString.getBytes(), signedBytes, kp.getPublic()), is(true));
	}

	// TODO: Ian - finish these tests
	// @Test
	// public void base64ISTest() throws Exception {
	// 	KeyPair kp = Factory.generateKeyPair(transMock);

	// 	String privateKeyString = Factory.toString(transMock, kp.getPrivate());
	// 	String cleaned = cleanupString(privateKeyString);
	// 	System.out.println(cleaned);
	// 	writeToFile(privateKeyFile, cleaned);
	// 	Base64InputStream b64is = new Base64InputStream(privateKeyFile);
	// 	byte[] buffer = new byte[10000];
	// 	b64is.read(buffer);
	// 	System.out.println(new String(buffer));
	// 	b64is.close();
	// }

	// @Test
	// public void getSecurityProviderTest() {
	// }

	private String cleanupString(String str) {
		String[] lines = str.split("\n", 0);
		List<String> rawLines = new ArrayList<String>();
		for (int i = 0; i < lines.length - 2; i++) {
			rawLines.add(lines[i + 1]);
		}
		return String.join("", rawLines);
	}

	private void writeToFile(File file, String contents, String header) throws Exception {
		PrintWriter writer = new PrintWriter(file, "UTF-8");
		if (header != null) {
			writer.println(header);
		}
		writer.println(contents);
		writer.close();
	}

	private String readFromFile(File file, boolean addCR) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(file));
		StringBuilder sb = new StringBuilder();
		String line;
		while ((line = br.readLine()) != null) {
			String lineEnd = (addCR) ? "\r\n" : "\n";
			sb.append(line + lineEnd);
		}
		br.close();
		return sb.toString();
	}

}
