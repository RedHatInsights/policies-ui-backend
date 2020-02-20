package com.redhat.cloud.custompolicies.app.rest.utils;

import com.redhat.cloud.custompolicies.app.model.pager.Page;
import com.redhat.cloud.custompolicies.app.model.pager.Pager;
import io.quarkus.panache.common.Sort;
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
        Assert.assertEquals(10, pager.getLimit());
        Assert.assertEquals(0, pager.getOffset());
    }

    @Test
    public void testExtractPager() throws URISyntaxException {
        UriInfo info = new ResteasyUriInfo(new URI("https://foo?page=12&pageSize=100"));
        Pager pager = PagingUtils.extractPager(info);
        Assert.assertEquals(100, pager.getLimit());
        Assert.assertEquals(12, pager.getOffset());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExtractPagerInvalidPage() throws URISyntaxException {
        UriInfo info = new ResteasyUriInfo(new URI("https://foo?page=bar&pageSize=100"));
        PagingUtils.extractPager(info);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExtractPagerInvalidPageSize() throws URISyntaxException {
        UriInfo info = new ResteasyUriInfo(new URI("https://foo?page=12&pageSize=foo"));
        PagingUtils.extractPager(info);
    }

    @Test
    public void testExtractPagerSort() throws URISyntaxException {
        UriInfo info = new ResteasyUriInfo(new URI("https://foo?sortColumn=foo&sortDirection=desc"));
        Pager pager = PagingUtils.extractPager(info);
        Assert.assertEquals(1, pager.getSort().getColumns().size());
        Assert.assertEquals("foo", pager.getSort().getColumns().get(0).getName());
        Assert.assertEquals(Sort.Direction.Descending, pager.getSort().getColumns().get(0).getDirection());
    }

    @Test
    public void testExtractPagerSortWithDefaultDirection() throws URISyntaxException {
        UriInfo info = new ResteasyUriInfo(new URI("https://foo?sortColumn=foobar"));
        Pager pager = PagingUtils.extractPager(info);
        Assert.assertEquals(1, pager.getSort().getColumns().size());
        Assert.assertEquals("foobar", pager.getSort().getColumns().get(0).getName());
        Assert.assertEquals(Sort.Direction.Ascending, pager.getSort().getColumns().get(0).getDirection());
    }

    @Test
    public void testExtractPagerSortMultiple() throws URISyntaxException {
        UriInfo info = new ResteasyUriInfo(new URI("https://foo?sortColumn=foo&sortDirection=desc&sortColumn=bar&sortDirection=asc"));
        Pager pager = PagingUtils.extractPager(info);
        Assert.assertEquals(2, pager.getSort().getColumns().size());
        Assert.assertEquals("foo", pager.getSort().getColumns().get(0).getName());
        Assert.assertEquals(Sort.Direction.Descending, pager.getSort().getColumns().get(0).getDirection());
        Assert.assertEquals("bar", pager.getSort().getColumns().get(1).getName());
        Assert.assertEquals(Sort.Direction.Ascending, pager.getSort().getColumns().get(1).getDirection());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExtractPagerSortWrongDirection() throws URISyntaxException {
        UriInfo info = new ResteasyUriInfo(new URI("https://foo?sortColumn=foo&sortDirection=bar"));
        PagingUtils.extractPager(info);
    }

    @Test
    public void testExtractFilter() throws URISyntaxException {
        UriInfo info = new ResteasyUriInfo(new URI("https://foo?filter[guy]=brush"));
        Pager pager = PagingUtils.extractPager(info);
        Assert.assertEquals("brush", pager.getFilter().getParameters().map().get("guy"));
    }

    @Test
    public void testExtractFilterCustomOperator() throws URISyntaxException {
        UriInfo info = new ResteasyUriInfo(new URI("https://foo?filter[guy]=%25brush%25&filter:op[guy]=like"));
        Pager pager = PagingUtils.extractPager(info);
        Assert.assertEquals("%brush%", pager.getFilter().getParameters().map().get("guy"));
        Assert.assertEquals("guy LIKE :guy", pager.getFilter().getQuery());
    }

    @Test
    public void testExtractFilterBooleanOperator() throws URISyntaxException {
        UriInfo info = new ResteasyUriInfo(new URI("https://foo?filter[bar]=true&filter:op[bar]=boolean_is"));
        Pager pager = PagingUtils.extractPager(info);
        Assert.assertEquals(true, pager.getFilter().getParameters().map().get("bar"));
    }

    @Test
    public void testExtractFilterMultipleParams() throws URISyntaxException {
        UriInfo info = new ResteasyUriInfo(new URI("https://foo?filter[guy]=brush&filter[foo]=bar&filter[raw]=ewf"));
        Pager pager = PagingUtils.extractPager(info);
        Assert.assertEquals("brush", pager.getFilter().getParameters().map().get("guy"));
        Assert.assertEquals("bar", pager.getFilter().getParameters().map().get("foo"));
        Assert.assertEquals("ewf", pager.getFilter().getParameters().map().get("raw"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExtractFilterInvalidOperator() throws URISyntaxException {
        UriInfo info = new ResteasyUriInfo(new URI("https://foo?filter[bar]=true&filter:op[bar]=wrong"));
        PagingUtils.extractPager(info);
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
        Assert.assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }


}
