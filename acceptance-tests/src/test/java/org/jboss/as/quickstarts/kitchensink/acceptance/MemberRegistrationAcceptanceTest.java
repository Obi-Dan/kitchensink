package org.jboss.as.quickstarts.kitchensink.acceptance;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.json.Json;
import jakarta.json.JsonObject;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MemberRegistrationAcceptanceTest {

    private static final String BASE_URL = "http://localhost:8080/kitchensink/rest/members";

    @BeforeAll
    public static void setup() {
        RestAssured.baseURI = BASE_URL;
        // It's good practice to clear any existing members or ensure a clean state before tests if possible.
        // However, without direct DB access or a dedicated cleanup API endpoint, this can be tricky.
        // For now, we'll assume the tests can run independently or the environment is reset between runs.
    }

    @Test
    public void testRegisterNewMemberSuccessfully() {
        // REQ-1.1.1: The system shall allow users to register new members with name, email, and phone number.
        // REQ-3.1.4: The API shall support creating new members (POST /members).
        // REQ-3.1.5: The API shall return appropriate HTTP status codes (200...)
        // REQ-3.2.3: For successful operations, the API shall return appropriate success status codes.
        // REQ-3.2.4: The API shall produce and consume JSON formatted data.

        long timestamp = System.currentTimeMillis(); // To ensure unique email for each run
        String uniqueEmail = "testuser_" + timestamp + "@example.com";

        JsonObject newMember = Json.createObjectBuilder()
                .add("name", "Test User")
                .add("email", uniqueEmail)
                .add("phoneNumber", "1234567890")
                .build();

        Response response = given()
                .contentType(ContentType.JSON)
                .body(newMember.toString())
                .when()
                .post()
                .then()
                .statusCode(200) // Assuming 200 OK for successful creation as per REQ-3.1.5 & REQ-3.2.3
                .contentType(ContentType.JSON)
                .body("name", equalTo("Test User"))
                .body("email", equalTo(uniqueEmail))
                .body("phoneNumber", equalTo("1234567890"))
                .body("id", notNullValue()) // REQ-1.1.4: Unique identifier generated
                .extract().response();

        System.out.println("Registered Member: " + response.asString());

        // REQ-1.1.4: Verify unique identifier (already checked by notNullValue() but could be more specific)
        // We can also make a GET request to /members/{id} to verify persistence if needed,
        // but that would be testing REQ-1.3.2 and REQ-3.1.3, which should be in a separate test.
    }
}
