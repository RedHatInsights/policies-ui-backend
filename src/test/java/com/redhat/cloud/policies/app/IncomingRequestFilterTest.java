package com.redhat.cloud.policies.app;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.Header;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static com.redhat.cloud.policies.app.AbstractITest.API_BASE_V1_0;
import static io.restassured.RestAssured.given;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.emptyString;

@QuarkusTest
public class IncomingRequestFilterTest {

    @Test
    void testNullOrgId() {
        Header identityHeader = buildIdentityHeader(null);
        given()
                .header(identityHeader)
                .when().get(API_BASE_V1_0 + "/policies/")
                .then()
                .statusCode(401)
                .body(emptyString());
    }

    @Test
    void testEmptyOrgId() {
        Header identityHeader = buildIdentityHeader("");
        given()
                .header(identityHeader)
                .when().get(API_BASE_V1_0 + "/policies/")
                .then()
                .statusCode(401)
                .body(emptyString());
    }

    @Test
    void testBlankOrgId() {
        Header identityHeader = buildIdentityHeader("   ");
        given()
                .header(identityHeader)
                .when().get(API_BASE_V1_0 + "/policies/")
                .then()
                .statusCode(401)
                .body(emptyString());
    }

    private static Header buildIdentityHeader(String orgId) {
        JsonObject user = new JsonObject();
        user.put("username", "johndoe");

        JsonObject identity = new JsonObject();
        identity.put("type", "User");
        identity.put("account_number", "account-id");
        identity.put("org_id", orgId);
        identity.put("user", user);

        JsonObject header = new JsonObject();
        header.put("identity", identity);

        String identityHeaderValue = new String(Base64.getEncoder().encode(header.encode().getBytes(UTF_8)), UTF_8);

        return new Header("x-rh-identity", identityHeaderValue);
    }
}
