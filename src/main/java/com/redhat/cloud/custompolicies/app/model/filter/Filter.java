package com.redhat.cloud.custompolicies.app.model.filter;

import io.quarkus.panache.common.Parameters;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;


public class Filter implements Cloneable {

    private Parameters parameters = new Parameters();
    private StringBuilder query = new StringBuilder();

    public Filter() {

    }

    private Filter(Map<String, Object> map, StringBuilder query) {
        for (String key : map.keySet()) {
            this.parameters.and(key, map.get(key));
        }
        this.query = query;
    }

    public String getQuery() {
        return this.query.toString();
    }

    public Parameters getParameters() {
        return this.parameters;
    }

    public Filter and(String field, Operator operator, Object value) {
        return this.add(field, operator, "and", value);
    }

    public Filter or(String field, Operator operator, Object value) {
        return this.add(field, operator, "or", value);
    }

    private Filter add(String field, Operator operator, String type, Object value) {
        if (operator.equals(Operator.ILIKE) && value instanceof String) {
            value = ((String) value).toLowerCase();
        }
        this.parameters.and(field, value);

        if (query.length() > 0) {
            query.append(" ");
            query.append(type);
            query.append(" ");
        }

        if (operator.equals(Operator.ILIKE)) {
            query.append("LOWER(");
        }
        query.append(field);
        if (operator.equals(Operator.ILIKE)) {
            query.append(")");
        }

        query.append(" ");
        query.append(operator.getOperation());
        query.append(" :");
        query.append(field);

        return this;
    }

    @Override
    public Object clone() {
        return new Filter(this.parameters.map(), new StringBuilder(this.query));
    }

    public enum Operator {
        EQUAL("="),
        LIKE("LIKE"),
        ILIKE("LIKE"),
        NOT_EQUAL("!="),
        BOOLEAN_IS("IS");

        String operation;

        Operator(String operation) {
            this.operation = operation;
        }

        String getOperation() {
            return this.operation;
        }

        public static Operator fromName(String name) {
            final String upperCaseName = name.toUpperCase();
            Optional<Operator> result = Arrays.stream(Operator.values())
                    .filter(val -> val.name().equals(upperCaseName))
                    .findAny();
            if (result.isPresent()) {
                return result.get();
            }
            throw new IllegalArgumentException("Unknown Filter.Operator requested: [" + upperCaseName + "]");
        }

    }

}
