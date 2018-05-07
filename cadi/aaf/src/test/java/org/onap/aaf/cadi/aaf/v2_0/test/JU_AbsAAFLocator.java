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

package org.onap.aaf.cadi.aaf.v2_0.test;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import org.junit.*;
import org.mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.onap.aaf.cadi.Access;
import org.onap.aaf.cadi.Locator.Item;
import org.onap.aaf.cadi.LocatorException;
import org.onap.aaf.cadi.PropAccess;
import org.onap.aaf.cadi.aaf.v2_0.AbsAAFLocator;
import org.onap.aaf.cadi.aaf.v2_0.AbsAAFLocator.LocatorCreator;
import org.onap.aaf.cadi.config.Config;
import org.onap.aaf.misc.env.impl.BasicTrans;

public class JU_AbsAAFLocator {

	@Mock private LocatorCreator locatorCreatorMock;

	private PropAccess access;
	private URI uri;

	private static final String uriString = "example.com";

	@Before
	public void setup() throws URISyntaxException {
		MockitoAnnotations.initMocks(this);

		access = new PropAccess(new PrintStream(new ByteArrayOutputStream()), new String[0]);
		access.setProperty(Config.CADI_LATITUDE, "38.62");  // St Louis approx lat
		access.setProperty(Config.CADI_LONGITUDE, "90.19");  // St Louis approx lon

		uri = new URI(uriString);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		AbsAAFLocator.setCreator(null);
	}

	@Test
	public void test() throws LocatorException {
		AAFLocatorStub loc;

		// Test with http
		loc = new AAFLocatorStub(access, "httpname");
		assertThat(loc.getName(), is("httpname"));
		assertThat(loc.getVersion(), is(Config.AAF_DEFAULT_VERSION));
		assertThat(loc.toString(), is("AAFLocator for " + "httpname" + " on " + loc.getURI()));

		loc = new AAFLocatorStub(access, "name");
		assertThat(loc.getName(), is("name"));
		assertThat(loc.getVersion(), is(Config.AAF_DEFAULT_VERSION));
		loc = new AAFLocatorStub(access, "name:v2.0");
		assertThat(loc.getName(), is("name"));
		assertThat(loc.getVersion(), is("v2.0"));
	}

	@Test
	public void createTest() throws LocatorException {
		AbsAAFLocator.setCreator(locatorCreatorMock);

		assertThat(AbsAAFLocator.create("nonsense"), is(nullValue()));
		assertThat(AbsAAFLocator.create("nonsense/locate"), is(nullValue()));
		assertThat(AbsAAFLocator.create("nonsense/locate/"), is(nullValue()));
		assertThat(AbsAAFLocator.create("nonsense/locate//"), is(nullValue()));
		assertThat(AbsAAFLocator.create("nonsense/locate/name:v2.0"), is(nullValue()));

		assertThat(AbsAAFLocator.create("http/locate/name:v2.0"), is(nullValue()));

		doReturn(mock(AbsAAFLocator.class)).when(locatorCreatorMock).create(anyString(), anyString());
		assertThat(AbsAAFLocator.create("http/locate/name:v2.0/path"), is(not(nullValue())));

		AbsAAFLocator.setCreator(null);
		assertThat(AbsAAFLocator.create("http/locate/name:v2.0"), is(nullValue()));

		assertThat(AbsAAFLocator.create("http"), is(not(nullValue())));

		AbsAAFLocator.setCreator(locatorCreatorMock);
		assertThat(AbsAAFLocator.create("first", "second"), is(not(nullValue())));
	}

	@Test
	public void nameFromLocatorURITest() throws LocatorException, URISyntaxException {
		AAFLocatorStub loc = new AAFLocatorStub(access, "name:v2.0");
		assertThat(loc.getNameFromURI(new URI("example.com")), is("example.com"));
		assertThat(loc.getNameFromURI(new URI("example.com/extra/stuff")), is("example.com/extra/stuff"));
		assertThat(loc.getNameFromURI(new URI("example.com/locate/stuff")), is("stuff")); // n' stuff
	}

	@Test
	public void setSelfTest() throws LocatorException {
		AbsAAFLocator.setCreatorSelf("host", 8000);
		AbsAAFLocator.setCreator(null);
		AbsAAFLocator.setCreatorSelf("host", 8000);
		(new AAFLocatorStub(access, "name:v2.0")).setSelf("host", 8000);  // oof
	}

	@Test
	public void coverage() throws LocatorException {
		AAFLocatorStub loc = new AAFLocatorStub(access, "name:v2.0");
		assertThat(loc.get(null), is(nullValue()));

		try {
			loc.get(mock(Item.class));
			fail("Should've thrown an exception");
		} catch (Exception e) {
		}

		try {
			loc.invalidate(mock(Item.class));
			fail("Should've thrown an exception");
		} catch (Exception e) {
		}

		try {
			loc.best();
			fail("Should've thrown an exception");
		} catch (Exception e) {
		}

		assertThat(loc.first(), is(nullValue()));

		assertThat(loc.hasItems(), is(false));
		assertThat(loc.next(null), is(nullValue()));

		try {
			loc.next(mock(Item.class));
			fail("Should've thrown an exception");
		} catch (Exception e) {
		}

		loc.destroy();


		assertThat(loc.exposeGetURI(uri), is(uri));

		assertThat(loc.setPathInfo("pathInfo"), is(not(nullValue())));
		assertThat(loc.setQuery("query"), is(not(nullValue())));
		assertThat(loc.setFragment("fragment"), is(not(nullValue())));
		
		assertThat(loc.exposeGetURI(uri), is(not(uri)));
		
		try {
			loc.setPathInfo("\\~!@[]\\[\\]#$%^&*()_+");
			loc.setQuery("\\~!@[]\\[\\]#$%^&*()_+");
			loc.setFragment("\\~!@[]\\[\\]#$%^&*()_+");
			loc.exposeGetURI(uri);
			fail("Should've thrown an exception");
		} catch (Exception e) {
		}
	}


	@Test(expected = LocatorException.class)
	public void throwsTest() throws LocatorException {
		@SuppressWarnings("unused")
		AAFLocatorStub loc = new AAFLocatorStub(new PropAccess(), "name");
	}

	private class AAFLocatorStub extends AbsAAFLocator<BasicTrans> {
		public AAFLocatorStub(Access access, String name) throws LocatorException {
			super(access, name, 10000L);
		}
		@Override public boolean refresh() { return false; }
		@Override protected URI getURI() { return uri; }
		public String getName() { return name; }
		public String getVersion() { return version; }
		public String getNameFromURI(URI uri) { return nameFromLocatorURI(uri); }
		public URI exposeGetURI(URI uri) throws LocatorException { return super.getURI(uri); }
	}

}
