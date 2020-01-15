package com.redhat.cloud.custompolicies.app.rest.utils;

import com.redhat.cloud.custompolicies.app.model.pager.Page;
import com.redhat.cloud.custompolicies.app.model.pager.Pager;
import org.jboss.resteasy.specimpl.ResteasyUriInfo;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public class PagingUtilsTest {

    @Test
    public void testExtractDefaultPager() throws URISyntaxException {
        UriInfo info = new ResteasyUriInfo(new URI("https://foo"));
        Pager pager = PagingUtils.extractPager(info);
        Assert.assertEquals(10, pager.getItemsPerPage());
        Assert.assertEquals(0, pager.getPage());
    }

    @Test
    public void testExtractPager() throws URISyntaxException {
        UriInfo info = new ResteasyUriInfo(new URI("https://foo?page=12&pageSize=100"));
        Pager pager = PagingUtils.extractPager(info);
        Assert.assertEquals(100, pager.getItemsPerPage());
        Assert.assertEquals(12, pager.getPage());
    }

    @Test
    public void testResponseBuilder() {
        Page<String> page = new Page<>(
                List.of("Hello", "World"),
                Pager.builder().itemsPerPage(10).page(0).build(),
                54
        );
        Response response = PagingUtils.responseBuilder(page).build();

        Assert.assertEquals("54", response.getHeaderString("TotalCount"));
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void testResponseBuilderEmpty() {
        Page<String> page = new Page<>(
                List.of(),
                Pager.builder().itemsPerPage(10).page(0).build(),
                0
        );
        Response response = PagingUtils.responseBuilder(page).build();

        Assert.assertEquals(null, response.getHeaderString("TotalCount"));
        Assert.assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }


}
