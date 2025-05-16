package org.jboss.as.quickstarts.kitchensink;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class MemberResourceTest {
    @Test
    void testHelloEndpoint() {
        given()
          .when().get("/rest/members")
          .then()
             .statusCode(200)
             .body(is("Hello from Quarkus REST"));
    }

}