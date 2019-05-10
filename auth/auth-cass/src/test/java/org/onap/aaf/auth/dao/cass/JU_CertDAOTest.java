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

package org.onap.aaf.auth.dao.cass;

import static org.mockito.MockitoAnnotations.initMocks;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.nio.ByteBuffer;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.onap.aaf.auth.dao.AbsCassDAO;
import org.onap.aaf.auth.dao.AbsCassDAO.CRUD;
import org.onap.aaf.auth.dao.AbsCassDAO.PSInfo;
import org.onap.aaf.auth.dao.CassAccess;
import org.onap.aaf.auth.env.AuthzEnv;
import org.onap.aaf.auth.env.AuthzTrans;
import org.onap.aaf.auth.layer.Result;
import org.onap.aaf.cadi.config.Config;
import org.onap.aaf.misc.env.APIException;
import org.onap.aaf.misc.env.Decryptor;
import org.onap.aaf.misc.env.Env;
import org.onap.aaf.misc.env.LogTarget;
import org.onap.aaf.misc.env.TimeTaken;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

public class JU_CertDAOTest {

	@Mock
    AuthzTrans trans;
	@Mock
	Cluster cluster;
	@Mock
	Session session;
	@Mock
	AuthzEnv env;
	@Mock
	LogTarget logTarget;
	
	@Before
	public void setUp() throws APIException, IOException {
		initMocks(this);
		Mockito.doReturn(Mockito.mock(LogTarget.class)).when(trans).warn();
		Mockito.doReturn(Mockito.mock(LogTarget.class)).when(trans).error();
		Mockito.doReturn(Mockito.mock(LogTarget.class)).when(trans).debug();
		Mockito.doReturn(Mockito.mock(LogTarget.class)).when(trans).info();
		Mockito.doReturn(Mockito.mock(LogTarget.class)).when(trans).init();
		Mockito.doReturn("100").when(trans).getProperty(Config.CADI_LATITUDE);
		Mockito.doReturn("100").when(trans).getProperty(Config.CADI_LONGITUDE);
		Mockito.doReturn(session).when(cluster).connect("test");
	}
	
	@Test
	public void testInit() {
		TimeTaken tt = Mockito.mock(TimeTaken.class);
		Mockito.doReturn(tt).when(trans).start("CertDAO CREATE", Env.REMOTE);
		Mockito.doReturn(tt).when(trans).start("Clear Reset Deque", Env.SUB);
		Mockito.doReturn(tt).when(trans).start("New Cassandra Session", Env.SUB);
		Mockito.doReturn(tt).when(trans).start("Preparing PSInfo CREATE on CertDAO", Env.SUB);
		Mockito.doNothing().when(tt).done();
		CertDAO.Data data  = new CertDAO.Data();
		PSInfo createPS = Mockito.mock(PSInfo.class);
		Result<ResultSet> rs = new Result<ResultSet>(null,0,"test",new String[0]);
		Mockito.doReturn(rs).when(createPS).exec(trans, "CertDAOImpl CREATE", data);
		
		CertDAOImpl daoObj=null;
		try {
			daoObj = new CertDAOImpl(trans, cluster, "test",data, createPS);
		} catch (APIException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	@Test
	public void testCertLoader(){
		
		Class<?> innerClass = CertDAO.class.getDeclaredClasses()[0];
        Constructor<?> constructor = innerClass.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        try {
			Object obj = constructor.newInstance(1);
			Method innnerClassMtd;
				
			CertDAO.Data data  = new CertDAO.Data();
			Row row = Mockito.mock(Row.class);
			ByteBuffer bbObj = ByteBuffer.allocateDirect(10);
			bbObj.limit(7);
			bbObj.put(0, new Byte("0"));
			bbObj.put(1, new Byte("1"));
			bbObj.put(2, new Byte("2"));
			Mockito.doReturn(bbObj).when(row).getBytesUnsafe(1);
			
			innnerClassMtd = innerClass.getMethod("load", new Class[] {CertDAO.Data.class, Row.class});
			innnerClassMtd.invoke(obj, new Object[] {data, row});
			
			innnerClassMtd = innerClass.getDeclaredMethod("key", new Class[] {CertDAO.Data.class, Integer.TYPE, Object[].class });
			innnerClassMtd.invoke(obj, new Object[] {data, 1, new Object[] {"test","test","test"} });
//			
			innnerClassMtd = innerClass.getDeclaredMethod("body", new Class[] {CertDAO.Data.class, Integer.TYPE, Object[].class });
			innnerClassMtd.invoke(obj, new Object[] {data, 1, new Object[] {"test","test","test","test","test","test","test","test","test","test","test"} });
			
//			DataInputStream in  = Mockito.mock(DataInputStream.class);
////			Mockito.doReturn(100).when(in).read();
////			Mockito.doReturn(100).when(in).readInt();
//			innnerClassMtd = innerClass.getDeclaredMethod("unmarshal", new Class[] {ArtiDAO.Data.class, DataInputStream.class });
//			innnerClassMtd.invoke(obj, new Object[] {data, in});
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}
	
	@Test
	public void testWasMOdified() {
		TimeTaken tt = Mockito.mock(TimeTaken.class);
		Mockito.doReturn(tt).when(trans).start("CertDAO CREATE", Env.REMOTE);
		Mockito.doReturn(tt).when(trans).start("Clear Reset Deque", Env.SUB);
		Mockito.doReturn(tt).when(trans).start("New Cassandra Session", Env.SUB);
		Mockito.doReturn(tt).when(trans).start("Preparing PSInfo CREATE on CertDAO", Env.SUB);
		Mockito.doReturn(tt).when(trans).start("DELETE APPROVAL",Env.REMOTE);
		Mockito.doNothing().when(tt).done();
		CertDAO.Data data  = new CertDAO.Data();
		PSInfo createPS = Mockito.mock(PSInfo.class);
		
		HistoryDAO historyDAO = Mockito.mock(HistoryDAO.class);
		Result<ResultSet> rs1 = new Result<ResultSet>(null,0,"test",new String[0]);
		Mockito.doReturn(rs1).when(historyDAO).create(Mockito.any(), Mockito.any());
		
		CacheInfoDAO cacheInfoDAO = Mockito.mock(CacheInfoDAO.class);
		Mockito.doReturn(rs1).when(cacheInfoDAO).touch(trans, CertDAO.TABLE, new int[1]);
		
		CertDAO daoObj = null;
		try {
			daoObj = new CertDAO(trans, historyDAO, cacheInfoDAO);
		} catch (APIException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		daoObj.wasModified(trans, CRUD.create, data, new String[] {"test"});
		
		rs1 = new Result<ResultSet>(null,1,"test",new String[0]);
		Mockito.doReturn(rs1).when(historyDAO).create(Mockito.any(), Mockito.any());
		
		Mockito.doReturn(rs1).when(cacheInfoDAO).touch(trans, CertDAO.TABLE, new int[1]);
		
		try {
			daoObj = new CertDAO(trans, historyDAO, cacheInfoDAO);
		} catch (APIException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		daoObj.wasModified(trans, CRUD.create, data, new String[] {"test"});

		daoObj.wasModified(trans, CRUD.delete, data, new String[] {"test"});
		daoObj.wasModified(trans, CRUD.delete, data, new String[] {"test", null});
		daoObj.wasModified(trans, CRUD.delete, data, new String[] {"test", "test"});
		daoObj.wasModified(trans, CRUD.delete, data, new String[] {null});
		daoObj.wasModified(trans, CRUD.delete, data, new String[] {});
		
		try {
			CertDAO.Data data1  = Mockito.mock(CertDAO.Data.class);
			Mockito.doThrow(new IOException()).when(data1).bytify();
			Mockito.doReturn(new int[1]).when(data1).invalidate(Mockito.any());
			daoObj.wasModified(trans, CRUD.delete, data1, new String[] {});
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Test
	public void testRead() {
		TimeTaken tt = Mockito.mock(TimeTaken.class);
		Mockito.doReturn(tt).when(trans).start("CertDAO CREATE", Env.REMOTE);
		Mockito.doReturn(tt).when(trans).start("CertDAO READ", Env.REMOTE);
		Mockito.doReturn(tt).when(trans).start("Clear Reset Deque", Env.SUB);
		Mockito.doReturn(tt).when(trans).start("New Cassandra Session", Env.SUB);
		Mockito.doReturn(tt).when(trans).start("Preparing PSInfo CREATE on CertDAO", Env.SUB);
		Mockito.doReturn(tt).when(trans).start("DELETE APPROVAL",Env.REMOTE);
		Mockito.doReturn("100").when(trans).getProperty(CassAccess.CASSANDRA_CLUSTERS_PORT,"100");
		Mockito.doReturn("100").when(trans).getProperty(CassAccess.CASSANDRA_CLUSTERS_PORT,"9042");
		Mockito.doReturn("100").when(trans).getProperty(CassAccess.CASSANDRA_CLUSTERS_USER_NAME,"100");
		Mockito.doReturn("100").when(trans).getProperty(CassAccess.CASSANDRA_CLUSTERS_USER_NAME,null);
		Mockito.doReturn(Mockito.mock(Decryptor.class)).when(trans).decryptor();
		Mockito.doNothing().when(tt).done();
		CertDAO.Data data  = new CertDAO.Data();
		PSInfo createPS = Mockito.mock(PSInfo.class);
		
		HistoryDAO historyDAO = Mockito.mock(HistoryDAO.class);
		Result<ResultSet> rs1 = new Result<ResultSet>(null,0,"test",new String[0]);
		Mockito.doReturn(rs1).when(historyDAO).create(Mockito.any(), Mockito.any());
		
		CacheInfoDAO cacheInfoDAO = Mockito.mock(CacheInfoDAO.class);
		Mockito.doReturn(rs1).when(cacheInfoDAO).touch(trans, CertDAO.TABLE, new int[1]);
		
		CertDAO daoObj = null;
		try {
			daoObj = new CertDAO(trans, historyDAO, cacheInfoDAO);
		} catch (APIException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		daoObj.read(trans, new Object[] {"test", BigInteger.ONE});
		Field cbField;
		try {
			cbField = CassAccess.class.getDeclaredField("cb");
			
			cbField.setAccessible(true);
//	        modifiersField.setInt(nsDaoField, nsDaoField.getModifiers() & ~Modifier.FINAL);
	        
			cbField.set(null, null);
		} catch (NoSuchFieldException | SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		daoObj.readX500(trans, "test");
		
		try {
			cbField = CassAccess.class.getDeclaredField("cb");
			
			cbField.setAccessible(true);
//	        modifiersField.setInt(nsDaoField, nsDaoField.getModifiers() & ~Modifier.FINAL);
	        
			cbField.set(null, null);
		} catch (NoSuchFieldException | SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		daoObj.readID(trans, "test");	
	}

	
	@Test
	public void testSecondConstructor() {
		TimeTaken tt = Mockito.mock(TimeTaken.class);
		Mockito.doReturn(tt).when(trans).start("CertDAO CREATE", Env.REMOTE);
		Mockito.doReturn(tt).when(trans).start("Clear Reset Deque", Env.SUB);
		Mockito.doReturn(tt).when(trans).start("New Cassandra Session", Env.SUB);
		Mockito.doReturn(tt).when(trans).start("Preparing PSInfo CREATE on CertDAO", Env.SUB);
		Mockito.doReturn(tt).when(trans).start("DELETE APPROVAL",Env.REMOTE);
		Mockito.doNothing().when(tt).done();
		CertDAO.Data data  = new CertDAO.Data();
		CacheInfoDAO cacheInfoDAO = Mockito.mock(CacheInfoDAO.class);
		HistoryDAO historyDAO = Mockito.mock(HistoryDAO.class);

		try {
			CertDAO daoObj = new CertDAO(trans, historyDAO, cacheInfoDAO);
		} catch (APIException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

class CertDAOImpl extends CertDAO{

	public CertDAOImpl(AuthzTrans trans, Cluster cluster, String keyspace,CertDAO.Data data,PSInfo createPS  ) throws APIException, IOException {
		super(trans, cluster, keyspace);
		this.createPS = createPS;
//		setPs(this, createPS, "psByUser");
//		setPs(this, createPS, "psByApprover");
//		setPs(this, createPS, "psByTicket");
//		setPs(this, createPS, "psByStatus");
//		setSession(this, Mockito.mock(Session.class));
	}
	
	public CertDAOImpl(AuthzTrans trans, Cluster cluster, String keyspace,PSInfo readPS  ) throws APIException, IOException {
		super(trans, cluster, keyspace);
		this.readPS = readPS;
	}
	

	public void setPs(CertDAOImpl CertDAOObj, PSInfo psInfoObj, String methodName) {
		Field nsDaoField;
		try {
			nsDaoField = CertDAO.class.getDeclaredField(methodName);
			
			nsDaoField.setAccessible(true);
	        // remove final modifier from field
	        Field modifiersField = Field.class.getDeclaredField("modifiers");
	        modifiersField.setAccessible(true);
//	        modifiersField.setInt(nsDaoField, nsDaoField.getModifiers() & ~Modifier.FINAL);
	        
	        nsDaoField.set(CertDAOObj, psInfoObj);
		} catch (NoSuchFieldException | SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	public void setSession(CertDAOImpl CertDAOObj, Session session) {
		Field nsDaoField;
		try {
			nsDaoField = AbsCassDAO.class.getDeclaredField("session");
			
			nsDaoField.setAccessible(true);
	        // remove final modifier from field
	        Field modifiersField = Field.class.getDeclaredField("modifiers");
	        modifiersField.setAccessible(true);
//	        modifiersField.setInt(nsDaoField, nsDaoField.getModifiers() & ~Modifier.FINAL);
	        nsDaoField.set(CertDAOObj, session);
		} catch (NoSuchFieldException | SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
