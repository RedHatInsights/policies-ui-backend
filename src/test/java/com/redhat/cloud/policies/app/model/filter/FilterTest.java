package com.redhat.cloud.policies.app.model.filter;

import io.quarkus.panache.common.Parameters;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FilterTest {

    @Test
    public void testNoParams() {
        Filter builder = new Filter();
        assertEquals("", builder.getQuery());
        assertEquals(new Parameters().map(), builder.getParameters().map());
    }

    @Test
    public void testOneParam() {
        Filter builder = new Filter();
        builder.and("foo", Filter.Operator.EQUAL, "bar");
        assertEquals("foo = :foo", builder.getQuery());
        assertEquals(Parameters.with("foo", "bar").map(), builder.getParameters().map());
    }

    @Test
    public void testWithAnd() {
        Filter builder = new Filter();
        builder
                .and("foo", Filter.Operator.EQUAL, "bar")
                .and("fake", Filter.Operator.LIKE, "mix");
        assertEquals("foo = :foo and fake LIKE :fake", builder.getQuery());
        assertEquals(Parameters
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
        assertEquals("foo = :foo or fake LIKE :fake", builder.getQuery());
        assertEquals(Parameters
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
        assertEquals("foo = :foo and fake LIKE :fake or monkey = :monkey", builder.getQuery());
        assertEquals(Parameters
                        .with("foo", "bar")
                        .and("fake", "mix")
                        .and("monkey", "island").map(),
                builder.getParameters().map()
        );
    }
}
