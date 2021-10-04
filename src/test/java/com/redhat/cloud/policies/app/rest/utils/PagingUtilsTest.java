package com.redhat.cloud.policies.app.rest.utils;

import com.redhat.cloud.policies.app.model.pager.Page;
import com.redhat.cloud.policies.app.model.pager.Pager;
import io.quarkus.panache.common.Sort;
import org.jboss.resteasy.specimpl.ResteasyUriInfo;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PagingUtilsTest {

    @Test
    public void testExtractDefaultPager() throws URISyntaxException {
        Pager pager = getPagerFromUriString("https://foo");
        Assert.assertEquals(50, pager.getLimit());
        Assert.assertEquals(0, pager.getOffset());
    }

    @Test
    public void testExtractPager() throws URISyntaxException {
        Pager pager = getPagerFromUriString("https://foo?offset=12&limit=100");
        Assert.assertEquals(100, pager.getLimit());
        Assert.assertEquals(12, pager.getOffset());
    }

    @Test
    public void testExtractPagerInvalidOffset() throws URISyntaxException {
        UriInfo info = new ResteasyUriInfo(new URI("https://foo?offset=bar&limit=100"));
        assertThrows(IllegalArgumentException.class, () -> {
            PagingUtils.extractPager(info);
        });
    }

    @Test
    public void testExtractPagerInvalidLimit() throws URISyntaxException {
        UriInfo info = new ResteasyUriInfo(new URI("https://foo?offset=12&limit=foo"));
        assertThrows(IllegalArgumentException.class, () -> {
            PagingUtils.extractPager(info);
        });
    }

    @Test
    public void testExtractPagerSort() throws URISyntaxException {
        Pager pager = getPagerFromUriString("https://foo?sortColumn=foo&sortDirection=desc");
        Assert.assertEquals(1, pager.getSort().getColumns().size());
        Assert.assertEquals("foo", pager.getSort().getColumns().get(0).getName());
        Assert.assertEquals(Sort.Direction.Descending, pager.getSort().getColumns().get(0).getDirection());
    }

    @Test
    public void testExtractPagerSortWithDefaultDirection() throws URISyntaxException {
        Pager pager = getPagerFromUriString("https://foo?sortColumn=foobar");
        Assert.assertEquals(1, pager.getSort().getColumns().size());
        Assert.assertEquals("foobar", pager.getSort().getColumns().get(0).getName());
        Assert.assertEquals(Sort.Direction.Ascending, pager.getSort().getColumns().get(0).getDirection());
    }

    @Test
    public void testExtractPagerSortMultiple() throws URISyntaxException {
        Pager pager = getPagerFromUriString(
                "https://foo?sortColumn=foo&sortDirection=desc&sortColumn=bar&sortDirection=asc");
        Assert.assertEquals(2, pager.getSort().getColumns().size());
        Assert.assertEquals("foo", pager.getSort().getColumns().get(0).getName());
        Assert.assertEquals(Sort.Direction.Descending, pager.getSort().getColumns().get(0).getDirection());
        Assert.assertEquals("bar", pager.getSort().getColumns().get(1).getName());
        Assert.assertEquals(Sort.Direction.Ascending, pager.getSort().getColumns().get(1).getDirection());
    }

    @Test
    public void testExtractPagerSortWrongDirection() throws URISyntaxException {
        UriInfo info = new ResteasyUriInfo(new URI("https://foo?sortColumn=foo&sortDirection=bar"));
        assertThrows(IllegalArgumentException.class, () -> {
            PagingUtils.extractPager(info);
        });
    }

    @Test
    void testSortOrderOnly1() throws URISyntaxException {
        String str = "http://foo?sortDirection=aSc";
        Pager pager = getPagerFromUriString(str);
        Sort.Column column = pager.getSort().getColumns().get(0);
        assertEquals("mtime", column.getName());
        assertEquals(Sort.Direction.Ascending, column.getDirection());
    }

    @Test
    void testSortOrderOnly2() throws URISyntaxException {
        String str = "http://foo?sortDirection=descending";
        Pager pager = getPagerFromUriString(str);
        Sort.Column column = pager.getSort().getColumns().get(0);
        assertEquals("mtime", column.getName());
        assertEquals(Sort.Direction.Descending, column.getDirection());
    }

    private Pager getPagerFromUriString(String str) throws URISyntaxException {
        UriInfo info = new ResteasyUriInfo(new URI(str));
        Pager pager = PagingUtils.extractPager(info);
        return pager;
    }

    @Test
    public void testExtractFilter() throws URISyntaxException {
        Pager pager = getPagerFromUriString("https://foo?filter[guy]=brush");
        Assert.assertEquals("brush", pager.getFilter().getParameters().map().get("guy"));
    }

    @Test
    public void testExtractFilterCustomOperator() throws URISyntaxException {
        Pager pager = getPagerFromUriString("https://foo?filter[guy]=%25brush%25&filter:op[guy]=like");
        Assert.assertEquals("%brush%", pager.getFilter().getParameters().map().get("guy"));
        Assert.assertEquals("guy LIKE :guy", pager.getFilter().getQuery());
    }

    @Test
    public void testExtractBadFilterBooleanOperator() throws URISyntaxException {
        UriInfo info = new ResteasyUriInfo(new URI("https://foo?filter[bar]=true&filter:op[bar]=boolean_is"));
        try {
            PagingUtils.extractPager(info);
        } catch (IllegalArgumentException e) {
            return;
        }
        Assert.assertTrue("Should not reach this", false);
    }

    @Test
    public void testExtractFilterBooleanOperator() throws URISyntaxException {
        Pager pager = getPagerFromUriString("https://foo?filter[is_enabled]=true&filter:op[is_enabled]=boolean_is");
        Assert.assertEquals(true, pager.getFilter().getParameters().map().get("is_enabled"));
    }

    @Test
    public void testExtractFilterBooleanOperator2() throws URISyntaxException {
        Pager pager = getPagerFromUriString("https://foo?filter[is_enabled]=true");
        Assert.assertEquals(true, pager.getFilter().getParameters().map().get("is_enabled"));
    }

    @Test
    public void testExtractFilterMultipleParams() throws URISyntaxException {
        Pager pager = getPagerFromUriString("https://foo?filter[guy]=brush&filter[foo]=bar&filter[raw]=ewf");
        Assert.assertEquals("brush", pager.getFilter().getParameters().map().get("guy"));
        Assert.assertEquals("bar", pager.getFilter().getParameters().map().get("foo"));
        Assert.assertEquals("ewf", pager.getFilter().getParameters().map().get("raw"));
    }

    @Test
    public void testExtractFilterInvalidOperator() throws URISyntaxException {
        UriInfo info = new ResteasyUriInfo(new URI("https://foo?filter[bar]=true&filter:op[bar]=wrong"));
        assertThrows(IllegalArgumentException.class, () -> {
            PagingUtils.extractPager(info);
        });
    }

    @Test
    public void testResponseBuilder() {
        Page<String> page = new Page<>(List.of("Hello", "World"), Pager.builder().itemsPerPage(10).page(0).build(), 54);
        Response response = PagingUtils.responseBuilder(page).build();

        Assert.assertEquals("54", response.getHeaderString("TotalCount"));
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void testResponseBuilderEmpty() {
        Page<String> page = new Page<>(List.of(), Pager.builder().itemsPerPage(10).page(0).build(), 0);
        Response response = PagingUtils.responseBuilder(page).build();

        Assert.assertEquals(null, response.getHeaderString("TotalCount"));
        Assert.assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

}
