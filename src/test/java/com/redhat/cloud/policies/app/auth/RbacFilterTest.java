package com.redhat.cloud.policies.app.auth;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.redhat.cloud.policies.app.auth.models.RbacRaw;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.security.Principal;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.jboss.resteasy.core.interception.jaxrs.PreMatchContainerRequestContext;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.spi.HttpRequest;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

@QuarkusTest
public class RbacFilterTest {
    @InjectMock
    RbacClient rbacClient;

    @InjectMock
    RhIdPrincipal user;

    @InjectMock
    RhIdPrincipal userPrincipal;

    @Inject
    RbacFilter rbacFilter;

    @Test
    void testAbortsOnRBACError() throws Exception {
        Mockito.when(rbacClient.getRbacInfo(Mockito.any())).thenThrow(RuntimeException.class);

        HttpRequest request = MockHttpRequest.get("/");
        PreMatchContainerRequestContext context = spy(new PreMatchContainerRequestContext(request, null, null));

        rbacFilter.filter(context);
        verifyAbortedAsForbidden(context);

        verify(user, times(0)).setRbac(Mockito.anyBoolean(), Mockito.anyBoolean());
        verify(userPrincipal, times(0)).setRbac(Mockito.anyBoolean(), Mockito.anyBoolean());
    }

    @Test
    void testSetsPermssionsOnPrincipals() throws Exception {
        RbacRaw rbacResult = mock(RbacRaw.class);
        Mockito.when(rbacResult.canRead("policies", "policies")).thenReturn(true);
        Mockito.when(rbacResult.canWrite("policies", "policies")).thenReturn(false);
        Mockito.when(rbacClient.getRbacInfo(Mockito.any())).thenReturn(rbacResult);

        HttpRequest request = MockHttpRequest.get("/");
        PreMatchContainerRequestContext context = spy(new PreMatchContainerRequestContext(request, null, null));

        context.setSecurityContext(securityContext());

        rbacFilter.filter(context);
        verify(context, Mockito.times(0)).abortWith(Mockito.any());

        // two calls are made, as user and userPrincipal are the same instances
        verify(user, times(2)).setRbac(true, false);
        verify(userPrincipal, times(2)).setRbac(true, false);
    }

    @Test
    void testSomePathsAlwaysAllowed() throws Exception {
        Mockito.when(rbacClient.getRbacInfo(Mockito.any())).thenThrow(RuntimeException.class);

        HttpRequest request = MockHttpRequest.get("/admin");
        PreMatchContainerRequestContext context = spy(new PreMatchContainerRequestContext(request, null, null));
        rbacFilter.filter(context);
        verify(context, times(0)).abortWith(any());

        request = MockHttpRequest.get("/api/policies/v1.0/status");
        context = spy(new PreMatchContainerRequestContext(request, null, null));
        rbacFilter.filter(context);
        verify(context, times(0)).abortWith(any());

        verify(user, times(0)).setRbac(Mockito.anyBoolean(), Mockito.anyBoolean());
        verify(userPrincipal, times(0)).setRbac(Mockito.anyBoolean(), Mockito.anyBoolean());
    }

    void verifyAbortedAsForbidden(ContainerRequestContext context) {
        ArgumentCaptor<Response> response = ArgumentCaptor.forClass(Response.class);
        verify(context).abortWith(response.capture());
        assertEquals(Response.Status.FORBIDDEN, response.getValue().getStatusInfo());
    }

    SecurityContext securityContext() {
        return new SecurityContext() {
            @Override
            public Principal getUserPrincipal() {
                return userPrincipal;
            }
            @Override
            public boolean isUserInRole(String string) { return true; }
            @Override
            public boolean isSecure() { return true; }
            @Override
            public String getAuthenticationScheme() { return "BASIC"; }
        };
    }

}
