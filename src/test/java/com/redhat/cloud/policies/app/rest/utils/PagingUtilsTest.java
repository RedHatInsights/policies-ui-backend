package com.redhat.cloud.policies.app.rest.utils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import com.redhat.cloud.policies.app.model.ColumnGetter;
import com.redhat.cloud.policies.app.model.ColumnInfo;
import com.redhat.cloud.policies.app.model.pager.Page;
import com.redhat.cloud.policies.app.model.pager.Pager;
import io.quarkus.panache.common.Sort;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.jboss.resteasy.specimpl.ResteasyUriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PagingUtilsTest {

    ColumnGetter columnGetter;

    @BeforeEach
    void mockColumnGetter() {
        this.columnGetter = mock(ColumnGetter.class);

        when(columnGetter.get(eq("foo"))).thenReturn(new ColumnInfo("foo", "foo", true, true));
        when(columnGetter.get(eq("bar"))).thenReturn(new ColumnInfo("bar", "bar", true, true));
        when(columnGetter.get(eq("foobar"))).thenReturn(new ColumnInfo("foobar", "foobar", false, true));
        when(columnGetter.get(eq("is_enabled"))).thenReturn(new ColumnInfo("is_enabled", "isEnabled", true, true));
        when(columnGetter.get(eq("guy"))).thenReturn(new ColumnInfo("guy", "guy", true, true));
    }

    @Test
    void extractDefaultPager() throws URISyntaxException {
        Pager pager = getPagerFromUriString("https://foo");
        assertEquals(50, pager.getLimit());
        assertEquals(0, pager.getOffset());
    }

    @Test
    void extractPager() throws URISyntaxException {
        Pager pager = getPagerFromUriString("https://foo?offset=12&limit=100");
        assertEquals(100, pager.getLimit());
        assertEquals(12, pager.getOffset());
    }

    @Test
    void extractPagerInvalidOffset() throws URISyntaxException {
        UriInfo info = new ResteasyUriInfo(new URI("https://foo?offset=bar&limit=100"));
        assertThrows(IllegalArgumentException.class, () -> {
            PagingUtils.extractPager(info, columnGetter);
        });
    }

    @Test
    void extractPagerInvalidLimit() throws URISyntaxException {
        UriInfo info = new ResteasyUriInfo(new URI("https://foo?offset=12&limit=foo"));
        assertThrows(IllegalArgumentException.class, () -> {
            PagingUtils.extractPager(info, columnGetter);
        });
    }

    @Test
    void extractPagerSort() throws URISyntaxException {
        Pager pager = getPagerFromUriString("https://foo?sortColumn=foo&sortDirection=desc");
        assertEquals(1, pager.getSort().getColumns().size());
        assertEquals("foo", pager.getSort().getColumns().get(0).getName());
        assertEquals(Sort.Direction.Descending, pager.getSort().getColumns().get(0).getDirection());
    }

    @Test
    public void testExtractPagerSortWithDefaultDirection() throws URISyntaxException {
        Pager pager = getPagerFromUriString("https://foo?sortColumn=foobar");
        assertEquals(1, pager.getSort().getColumns().size());
        assertEquals("foobar", pager.getSort().getColumns().get(0).getName());
        assertEquals(Sort.Direction.Ascending, pager.getSort().getColumns().get(0).getDirection());
    }

    @Test
    public void testExtractPagerSortMultiple() throws URISyntaxException {
        Pager pager = getPagerFromUriString("https://foo?sortColumn=foo&sortDirection=desc&sortColumn=bar&sortDirection=asc");
        assertEquals(2, pager.getSort().getColumns().size());
        assertEquals("foo", pager.getSort().getColumns().get(0).getName());
        assertEquals(Sort.Direction.Descending, pager.getSort().getColumns().get(0).getDirection());
        assertEquals("bar", pager.getSort().getColumns().get(1).getName());
        assertEquals(Sort.Direction.Ascending, pager.getSort().getColumns().get(1).getDirection());
    }

    @Test
    public void testExtractPagerSortWrongField() throws URISyntaxException {
        UriInfo info = new ResteasyUriInfo(new URI("https://foo?sortColumn=nonexisting&sortDirection=asc"));
        assertThrows(IllegalArgumentException.class, () -> {
            PagingUtils.extractPager(info, columnGetter);
        });
    }

    @Test
    public void testExtractPagerSortWrongField2() throws URISyntaxException {
        when(columnGetter.get(eq("filteronly"))).thenReturn(new ColumnInfo("filteronly", "filteronly", true, false));
        UriInfo info = new ResteasyUriInfo(new URI("https://foo?sortColumn=filteronly"));
        assertThrows(IllegalArgumentException.class, () -> {
            PagingUtils.extractPager(info, columnGetter);
        });
    }

    @Test
    public void testExtractPagerSortWrongDirection() throws URISyntaxException {
        UriInfo info = new ResteasyUriInfo(new URI("https://foo?sortColumn=foo&sortDirection=bar"));
        assertThrows(IllegalArgumentException.class, () -> {
            PagingUtils.extractPager(info, columnGetter);
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
    void sortOrderOnly2() throws URISyntaxException {
        String str = "http://foo?sortDirection=descending";
        Pager pager = getPagerFromUriString(str);
        Sort.Column column = pager.getSort().getColumns().get(0);
        assertEquals("mtime", column.getName());
        assertEquals(Sort.Direction.Descending, column.getDirection());
    }

    private Pager getPagerFromUriString(String str) throws URISyntaxException {
        UriInfo info = new ResteasyUriInfo(new URI(str));
        return PagingUtils.extractPager(info, columnGetter);
    }

    @Test
    public void extractFilter() throws URISyntaxException {
        Pager pager = getPagerFromUriString("https://foo?filter[guy]=brush");
        assertEquals("brush", pager.getFilter().getParameters().map().get("guy"));
    }

    @Test
    public void extractFilterCustomOperator() throws URISyntaxException {
        Pager pager = getPagerFromUriString("https://foo?filter[guy]=%25brush%25&filter:op[guy]=like");
        assertEquals("%brush%", pager.getFilter().getParameters().map().get("guy"));
        assertEquals("guy LIKE :guy", pager.getFilter().getQuery());
    }

    @Test
    public void extractBadFilterBooleanOperator() throws URISyntaxException {
        UriInfo info = new ResteasyUriInfo(new URI("https://foo?filter[bar]=true&filter:op[bar]=boolean_is"));
        assertThrows(IllegalArgumentException.class, () -> {
            PagingUtils.extractPager(info, columnGetter);
        });
    }

    @Test
    public void extractBadFilterField() throws URISyntaxException {
        UriInfo info = new ResteasyUriInfo(new URI("https://foo?filter[nonexistent]=value"));
        assertThrows(IllegalArgumentException.class, () -> {
            PagingUtils.extractPager(info, columnGetter);
        });
    }

    @Test
    public void extractBadFilterField2() throws URISyntaxException {
        when(columnGetter.get(eq("sortableonly"))).thenReturn(new ColumnInfo("sortableonly", "sortableonly", false, true));
        UriInfo info = new ResteasyUriInfo(new URI("https://foo?filter[sortableonly]=value"));
        assertThrows(IllegalArgumentException.class, () -> {
            PagingUtils.extractPager(info, columnGetter);
        });
    }

    @Test
    public void extractFilterBooleanOperator() throws URISyntaxException {
        Pager pager = getPagerFromUriString("https://foo?filter[is_enabled]=true&filter:op[is_enabled]=boolean_is");
        assertEquals(true, pager.getFilter().getParameters().map().get("isEnabled"));
    }

    @Test
    public void extractFilterBooleanOperator2() throws URISyntaxException {
        Pager pager = getPagerFromUriString("https://foo?filter[is_enabled]=true");
        assertEquals(true, pager.getFilter().getParameters().map().get("isEnabled"));
    }

    @Test
    public void extractFilterMultipleParams() throws URISyntaxException {
        when(columnGetter.get(eq("raw"))).thenReturn(new ColumnInfo("raw", "raw", true, true));
        Pager pager = getPagerFromUriString("https://foo?filter[guy]=brush&filter[foo]=bar&filter[raw]=ewf");
        assertEquals("brush", pager.getFilter().getParameters().map().get("guy"));
        assertEquals("bar", pager.getFilter().getParameters().map().get("foo"));
        assertEquals("ewf", pager.getFilter().getParameters().map().get("raw"));
    }

    @Test
    public void extractFilterInvalidOperator() throws URISyntaxException {
        UriInfo info = new ResteasyUriInfo(new URI("https://foo?filter[bar]=true&filter:op[bar]=wrong"));
        assertThrows(IllegalArgumentException.class, () -> {
            PagingUtils.extractPager(info, columnGetter);
        });
    }

    @Test
    public void responseBuilder() {
        Page<String> page = new Page<>(
                List.of("Hello", "World"),
                Pager.builder().itemsPerPage(10).page(0).build(),
                54
        );
        Response response = PagingUtils.responseBuilder(page).build();

        assertEquals("54", response.getHeaderString("TotalCount"));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void responseBuilderEmpty() {
        Page<String> page = new Page<>(
                List.of(),
                Pager.builder().itemsPerPage(10).page(0).build(),
                0
        );
        Response response = PagingUtils.responseBuilder(page).build();

        assertEquals(null, response.getHeaderString("TotalCount"));
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }


}
