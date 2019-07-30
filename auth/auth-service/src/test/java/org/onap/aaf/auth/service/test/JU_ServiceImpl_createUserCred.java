package org.onap.aaf.auth.service.test;

import static org.mockito.Mockito.*;

import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.onap.aaf.auth.dao.CachedDAO;
import org.onap.aaf.auth.dao.cass.CredDAO;
import org.onap.aaf.auth.dao.cass.UserRoleDAO;
import org.onap.aaf.auth.env.AuthzTrans;
import org.onap.aaf.auth.layer.Result;
import org.onap.aaf.auth.org.OrganizationException;
import org.onap.aaf.cadi.Hash;
import org.onap.aaf.cadi.util.FQI;
import org.onap.aaf.misc.env.Trans;

import aaf.v2_0.CredRequest;
import junit.framework.Assert;

@RunWith(MockitoJUnitRunner.class)
public class JU_ServiceImpl_createUserCred extends JU_BaseServiceImpl  {
	@Mock 
    private Result<CredDAO.Data> rcdd;	
	
	@Before
	public void setUp() throws Exception {
	    super.setUp();
	}

    @Test
    public void validCreateNewIsOwner() throws OrganizationException {
    	CredRequest cr = credRequest1();
    	final String fqi = "bob@people.onap.org";
    	when(trans.user()).thenReturn(fqi);
    	when(org.isValidPassword(trans, cr.getId(),cr.getPassword())).thenReturn("");
    	when(org.isValidCred(trans, cr.getId())).thenReturn(true);
    	when(org.canHaveMultipleCreds(cr.getId())).thenReturn(true);
		when(org.getIdentity(trans, cr.getId())).thenReturn(orgIdentity);
		when(orgIdentity.isFound()).thenReturn(true);
		final String ns = "org.onap.sample";
	    when(question.userRoleDAO().read(trans, fqi, ns+".owner")).thenReturn(Result.ok(listOf(urData(fqi,ns,"owner",100))));
	    when(question.nsDAO().read(trans, ns)).thenReturn(Result.ok(nsData(ns)));
	    when(question.credDAO().readID(trans, cr.getId())).thenReturn(Result.ok(emptyList(CredDAO.Data.class)));
	    when(question.credDAO().create(any(AuthzTrans.class), any(CredDAO.Data.class) )).thenReturn(Result.ok(credDataFound(cr,100)));
	    when(question.credDAO().readNS(trans, ns)).thenReturn(Result.ok(listOf(credDataFound(cr,100))));
	    Result<?> result = acsi.createUserCred(trans,cr);
	    // Owner may do FIRST Creds
    	Assert.assertEquals(Result.OK,result.status);
    }

    @Test
    public void validCreateNewOnlyAdmin() throws OrganizationException {
    	CredRequest cr = credRequest1();
    	final String fqi = "bob@people.onap.org";
    	when(trans.user()).thenReturn(fqi);
    	when(org.isValidPassword(trans, cr.getId(),cr.getPassword())).thenReturn("");
    	when(org.isValidCred(trans, cr.getId())).thenReturn(true);
    	when(org.canHaveMultipleCreds(cr.getId())).thenReturn(true);
		when(org.getIdentity(trans, cr.getId())).thenReturn(orgIdentity);
		when(orgIdentity.isFound()).thenReturn(true);
		final String ns = "org.onap.sample";
	    when(question.userRoleDAO().read(trans, fqi, ns+".owner")).thenReturn(Result.ok(emptyList(UserRoleDAO.Data.class)));
	    when(question.userRoleDAO().read(trans, fqi, ns+".admin")).thenReturn(Result.ok(listOf(urData(fqi,ns,"admin",100))));
	    when(question.nsDAO().read(trans, ns)).thenReturn(Result.ok(nsData(ns)));
	    when(question.credDAO().readID(trans, cr.getId())).thenReturn(Result.ok(emptyList(CredDAO.Data.class)));
	    when(question.credDAO().create(any(AuthzTrans.class), any(CredDAO.Data.class) )).thenReturn(Result.ok(credDataFound(cr,100)));
	    when(question.credDAO().readNS(trans, ns)).thenReturn(Result.ok(listOf(credDataFound(cr,100))));
	    Result<?> result = acsi.createUserCred(trans,cr);
	    // Admins may not do FIRST Creds
    	Assert.assertEquals(Result.ERR_Denied,result.status);
    }

    @Test
    public void validCreateExisting() throws OrganizationException {
    	CredRequest cr = credRequest1();
    	when(org.isValidPassword(trans, cr.getId(),cr.getPassword())).thenReturn("");
    	when(org.isValidCred(trans, cr.getId())).thenReturn(true);
    	when(org.canHaveMultipleCreds(cr.getId())).thenReturn(true);
		when(org.getIdentity(trans, cr.getId())).thenReturn(orgIdentity);
		when(orgIdentity.isFound()).thenReturn(true);
		String ns = "org.onap.sample";
	    when(question.nsDAO().read(trans, ns)).thenReturn(Result.ok(nsData(ns)));
	    
	    CredDAO.Data cdd = credDataFound(cr,100);
	    when(question.credDAO().create(any(AuthzTrans.class), any(CredDAO.Data.class) )).thenReturn(Result.ok(cdd));
	    when(question.credDAO().readID(trans, cr.getId())).thenReturn(Result.ok(listOf(cdd)));

	    Result<?> result = acsi.createUserCred(trans,cr);
    	Assert.assertEquals(Result.OK,result.status);
    }

    private CredRequest credRequest1() {
    	CredRequest cr = new CredRequest();
    	cr.setId("m12345@sample.onap.org");
    	cr.setPassword("BobAndWeave");
    	cr.setType(CredDAO.RAW);
    	return cr;
    }
    
   private CredDAO.Data credDataFound(CredRequest cr, int days) {
    	CredDAO.Data cdd = new CredDAO.Data();
    	cdd.id = cr.getId();
    	cdd.ns = FQI.reverseDomain(cr.getId());
    	cdd.other = 12345;
    	cdd.tag = "1355434";
    	cdd.type = CredDAO.BASIC_AUTH_SHA256;
    	try {
			cdd.cred = ByteBuffer.wrap(Hash.hashSHA256(cr.getPassword().getBytes()));
		} catch (NoSuchAlgorithmException e) {
			Assert.fail(e.getMessage());
		}
    	GregorianCalendar gc = new GregorianCalendar();
    	gc.add(GregorianCalendar.DAY_OF_YEAR, days);
    	cdd.expires = gc.getTime();
    	return cdd;
    }
    
}