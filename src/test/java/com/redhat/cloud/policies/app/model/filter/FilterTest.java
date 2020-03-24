package com.redhat.cloud.policies.app.model.filter;

import io.quarkus.panache.common.Parameters;
import org.junit.Assert;
import org.junit.Test;

public class FilterTest {

    @Test
    public void testNoParams() {
        Filter builder = new Filter();
        Assert.assertEquals("", builder.getQuery());
        Assert.assertEquals(new Parameters().map(), builder.getParameters().map());
    }

    @Test
    public void testOneParam() {
        Filter builder = new Filter();
        builder.and("foo", Filter.Operator.EQUAL, "bar");
        Assert.assertEquals("foo = :foo", builder.getQuery());
        Assert.assertEquals(Parameters.with("foo", "bar").map(), builder.getParameters().map());
    }

    @Test
    public void testWithAnd() {
        Filter builder = new Filter();
        builder
                .and("foo", Filter.Operator.EQUAL, "bar")
                .and("fake", Filter.Operator.LIKE, "mix");
        Assert.assertEquals("foo = :foo and fake LIKE :fake", builder.getQuery());
        Assert.assertEquals(Parameters
                .with("foo", "bar")
                .and("fake", "mix").map(),
                builder.getParameters().map()
        );
    }

    @Test
    public void testWithOr() {
        Filter builder = new Filter();
        builder
                .or("foo", Filter.Operator.EQUAL, "bar")
                .or("fake", Filter.Operator.LIKE, "mix");
        Assert.assertEquals("foo = :foo or fake LIKE :fake", builder.getQuery());
        Assert.assertEquals(Parameters
                        .with("foo", "bar")
                        .and("fake", "mix").map(),
                builder.getParameters().map()
        );
    }
    @Test
    public void testWithOrAnd() {
        Filter builder = new Filter();
        builder
                .or("foo", Filter.Operator.EQUAL, "bar")
                .and("fake", Filter.Operator.LIKE, "mix")
                .or("monkey", Filter.Operator.EQUAL, "island");
        Assert.assertEquals("foo = :foo and fake LIKE :fake or monkey = :monkey", builder.getQuery());
        Assert.assertEquals(Parameters
                        .with("foo", "bar")
                        .and("fake", "mix")
                        .and("monkey", "island").map(),
                builder.getParameters().map()
        );
    }
}
