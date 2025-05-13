package org.jboss.as.quickstarts.kitchensink.acceptance;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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

    private String generateUniqueEmail() {
        return "testuser_" + System.currentTimeMillis() + "@example.com";
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

    // --- Validation Failure Tests --- 

    @Test
    @Order(2)
    public void testRegisterMemberWithInvalidName_TooLong() {
        // REQ-1.2.1: Member names must be between 1 and 25 characters
        String invalidName = "ThisNameIsDefinitelyMuchTooLongForTheValidationCriteria"; // > 25 chars
        JsonObject payload = Json.createObjectBuilder()
                .add("name", invalidName)
                .add("email", generateUniqueEmail())
                .add("phoneNumber", generateValidPhoneNumber())
                .build();

        given()
            .contentType(ContentType.JSON)
            .body(payload.toString())
        .when()
            .post()
        .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .body("name", containsString("size must be between 1 and 25")); // Adjust error message if needed
    }

    @Test
    @Order(3)
    public void testRegisterMemberWithInvalidName_ContainsNumbers() {
        // REQ-1.2.1: Member names must not contain numbers
        String invalidName = "NameWith123Numbers";
        JsonObject payload = Json.createObjectBuilder()
                .add("name", invalidName)
                .add("email", generateUniqueEmail())
                .add("phoneNumber", generateValidPhoneNumber())
                .build();

        given()
            .contentType(ContentType.JSON)
            .body(payload.toString())
        .when()
            .post()
        .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .body("name", equalTo("Must not contain numbers"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"plainaddress", "@missingusername.com", "username@.com", "username@domain."})
    @Order(4)
    public void testRegisterMemberWithInvalidEmailFormat(String invalidEmail) {
        // REQ-1.2.2: Email addresses must be valid according to standard email format validation.
        JsonObject payload = Json.createObjectBuilder()
                .add("name", generateValidName())
                .add("email", invalidEmail)
                .add("phoneNumber", generateValidPhoneNumber())
                .build();

        given()
            .contentType(ContentType.JSON)
            .body(payload.toString())
        .when()
            .post()
        .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .body("email", containsString("must be a well-formed email address")); // Common Bean Validation message for @Email
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345", "123456789", "1234567890123"}) // Too short (<10) and too long (>12)
    @Order(5)
    public void testRegisterMemberWithInvalidPhoneNumber_Length(String invalidPhone) {
        // REQ-1.2.3: Phone numbers must be between 10 and 12 digits
        JsonObject payload = Json.createObjectBuilder()
                .add("name", generateValidName())
                .add("email", generateUniqueEmail())
                .add("phoneNumber", invalidPhone)
                .build();

        given()
            .contentType(ContentType.JSON)
            .body(payload.toString())
        .when()
            .post()
        .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .body("phoneNumber", anyOf(containsString("size must be between 10 and 12"), containsString("numeric value out of bounds (<12 digits>.<0 digits> expected)")));
    }

    @Test
    @Order(6)
    public void testRegisterMemberWithInvalidPhoneNumber_NonNumeric() {
        // REQ-1.2.3: Phone numbers must contain only numeric characters
        String invalidPhone = "123-456-7890";
        JsonObject payload = Json.createObjectBuilder()
                .add("name", generateValidName())
                .add("email", generateUniqueEmail())
                .add("phoneNumber", invalidPhone)
                .build();

        given()
            .contentType(ContentType.JSON)
            .body(payload.toString())
        .when()
            .post()
        .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .body("phoneNumber", equalTo("numeric value out of bounds (<12 digits>.<0 digits> expected)"));
    }

    @Test
    @Order(7) // Ensure this runs after a successful registration if it reuses data
    public void testRegisterMemberWithDuplicateEmail() {
        // REQ-1.1.2: Each member must have a unique email address in the system.
        // REQ-3.2.2: For duplicate email addresses, the API shall return a specific conflict response.

        // Step 1: Register an initial member to ensure the email exists
        String existingEmail = generateUniqueEmail(); // Use a fresh email for this test sequence
        String initialName = generateValidName();
        String initialPhone = generateValidPhoneNumber();

        JsonObject initialMemberPayload = Json.createObjectBuilder()
                .add("name", initialName)
                .add("email", existingEmail)
                .add("phoneNumber", initialPhone)
                .build();

        given()
            .contentType(ContentType.JSON)
            .body(initialMemberPayload.toString())
        .when()
            .post()
        .then()
            .statusCode(200); // Assuming 200 for successful creation as per other tests

        // Step 2: Attempt to register another member with the same email
        String duplicateName = "Duplicate " + generateValidName();
        String duplicatePhone = generateValidPhoneNumber(); // Different phone

        JsonObject duplicateMemberPayload = Json.createObjectBuilder()
                .add("name", duplicateName)
                .add("email", existingEmail) // Same email
                .add("phoneNumber", duplicatePhone)
                .build();

        given()
            .contentType(ContentType.JSON)
            .body(duplicateMemberPayload.toString())
        .when()
            .post()
        .then()
            .statusCode(409) // REQ-3.1.5: Expect 409 Conflict for duplicate email
            .contentType(ContentType.JSON)
            // The actual error message structure for duplicates needs to be verified.
            // Assuming it's similar to other validation errors, e.g., {"email": "Email address is already in use"}
            // Or it might be a general message in the body.
            // For now, let's check for a body that contains the word "duplicate" or "conflict" or "unique" related to email.
            // This assertion might need adjustment based on actual API response.
            .body("email", containsString("Email taken")) // Corrected based on actual error message
            // Alternative: .body(containsString("Email address already exists"));
            // Alternative: .body(containsString("Unique constraint violation"));
            ;
    }

    @Test
    @Order(8)
    public void testRetrieveMemberById_Success() {
        // REQ-1.3.2: The system shall support retrieving a single member by their unique identifier.
        // REQ-3.1.3: The API shall support retrieving a single member by ID (GET /members/{id}).

        // Step 1: Register a new member to get a valid ID
        String testName = generateValidName();
        String uniqueEmail = generateUniqueEmail();
        String testPhone = generateValidPhoneNumber();

        JsonObject newMemberPayload = Json.createObjectBuilder()
                .add("name", testName)
                .add("email", uniqueEmail)
                .add("phoneNumber", testPhone)
                .build();

        given()
            .contentType(ContentType.JSON)
            .body(newMemberPayload.toString())
        .when()
            .post()
        .then()
            .statusCode(200);

        // Step 2: Get all members to find the ID of the newly created one
        Response getAllResponse = given()
            .accept(ContentType.JSON)
        .when()
            .get()
        .then()
            .statusCode(200)
            .extract().response();
        
        List<Map<String, Object>> allMembers = getAllResponse.jsonPath().getList("$");
        Integer memberId = null;
        for (Map<String, Object> member : allMembers) {
            if (uniqueEmail.equals(member.get("email"))) {
                memberId = ((Number) member.get("id")).intValue();
                break;
            }
        }
        assertNotNull(memberId, "Could not find newly created member to retrieve its ID.");

        // Step 3: Retrieve the member by its ID
        given()
            .accept(ContentType.JSON)
            .pathParam("id", memberId)
        .when()
            .get("/{id}")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("id", equalTo(memberId))
            .body("name", equalTo(testName))
            .body("email", equalTo(uniqueEmail))
            .body("phoneNumber", equalTo(testPhone));
    }

    @Test
    @Order(9)
    public void testRetrieveMemberById_NotFound() {
        // REQ-1.3.2, REQ-3.1.3, REQ-3.1.5 (404 for not found)
        long nonExistentId = 999999L; // Assuming this ID will not exist

        given()
            .accept(ContentType.JSON)
            .pathParam("id", nonExistentId)
        .when()
            .get("/{id}")
        .then()
            .statusCode(404);
            // Optionally, check for an empty body or a specific error message if the API provides one for 404.
            // For now, just checking the status code.
    }
}
