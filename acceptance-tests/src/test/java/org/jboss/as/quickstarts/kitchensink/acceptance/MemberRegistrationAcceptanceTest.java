package org.jboss.as.quickstarts.kitchensink.acceptance;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Order;

import jakarta.json.Json;
import jakarta.json.JsonObject;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MemberRegistrationAcceptanceTest {

    private static final String BASE_URL = "http://localhost:8080/kitchensink/rest/members";
    private static Random random = new Random();

    @BeforeAll
    public static void setup() {
        RestAssured.baseURI = BASE_URL;
        // It's good practice to clear any existing members or ensure a clean state before tests if possible.
        // However, without direct DB access or a dedicated cleanup API endpoint, this can be tricky.
        // For now, we'll assume the tests can run independently or the environment is reset between runs.
    }

    private String generateValidPhoneNumber() {
        // REQ-1.2.3: Phone numbers must be between 10 and 12 digits
        int length = 10 + random.nextInt(3); // 10, 11, or 12 digits
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    private String generateValidName() {
        // REQ-1.2.1: Member names must be between 1 and 25 characters and must not contain numbers.
        // Simple name generator for testing
        String[] firstParts = {"Test", "User", "Sample", "Valid", "John", "Jane", "Alpha", "Beta"};
        String[] lastParts = {"Person", "Doe", "Smith", "Test", "User"};
        String name = firstParts[random.nextInt(firstParts.length)] + " " + lastParts[random.nextInt(lastParts.length)];
        if (name.length() > 25) {
            name = name.substring(0, 25);
        }
        return name;
    }

    @Test
    @Order(1) // Ensure this runs first if other tests depend on a member existing
    public void testRegisterNewMemberSuccessfully() {
        // REQ-1.1.1: The system shall allow users to register new members with name, email, and phone number.
        // REQ-3.1.4: The API shall support creating new members (POST /members).
        // REQ-3.1.5: The API shall return appropriate HTTP status codes (200...)
        // REQ-3.2.3: For successful operations, the API shall return appropriate success status codes.
        // REQ-3.2.4: The API consumes JSON. Successful POST currently returns empty body.

        long timestamp = System.currentTimeMillis();
        String testName = generateValidName();
        String uniqueEmail = "testuser_" + timestamp + "@example.com";
        String testPhone = generateValidPhoneNumber();

        System.out.println("Attempting to register member with:");
        System.out.println("  Name: " + testName);
        System.out.println("  Email: " + uniqueEmail);
        System.out.println("  Phone: " + testPhone);

        JsonObject newMemberPayload = Json.createObjectBuilder()
                .add("name", testName)
                .add("email", uniqueEmail)
                .add("phoneNumber", testPhone)
                .build();

        // Step 1: POST the new member
        Response postResponse = given()
            .contentType(ContentType.JSON)
            .body(newMemberPayload.toString())
        .when()
            .post();
        
        System.out.println("POST Response Status: " + postResponse.getStatusCode());
        System.out.println("POST Response Body: " + postResponse.getBody().asString());

        postResponse.then()
            .statusCode(200) // REQ-3.1.5: Expect 200 OK for successful creation
            .body(equalTo("")); // Verify that the response body is empty as per current app behavior
            // .contentType(ContentType.JSON) // Cannot assert this if body is empty and no C-T header is sent

        // Step 2: GET all members and find the newly created one to verify its details and ID generation
        // This indirectly verifies REQ-1.1.4 (ID generation)
        Response getResponse = given()
            .accept(ContentType.JSON)
        .when()
            .get()
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .extract().response();

        JsonPath jsonPath = getResponse.jsonPath();
        List<Map<String, Object>> allMembers = jsonPath.getList("$");

        Map<String, Object> foundMember = null;
        for (Map<String, Object> member : allMembers) {
            if (uniqueEmail.equals(member.get("email"))) {
                foundMember = member;
                break;
            }
        }

        assertNotNull(foundMember, "Newly created member with email '" + uniqueEmail + "' not found in GET /members response.");
        assertEquals(testName, foundMember.get("name"), "Name of retrieved member does not match.");
        assertEquals(testPhone, foundMember.get("phoneNumber"), "Phone number of retrieved member does not match.");
        assertNotNull(foundMember.get("id"), "ID of retrieved member should not be null (REQ-1.1.4).");
        assertTrue(foundMember.get("id") instanceof Number || foundMember.get("id").toString().matches("\\d+"), "Member ID should be a number.");

        System.out.println("Successfully registered and verified member: " + foundMember);
    }
}
