package org.jboss.as.quickstarts.kitchensink.acceptance;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.json.Json;
import jakarta.json.JsonObject;

import java.util.ArrayList; // Added for type safety
import java.util.List;
import java.util.Map;
import java.util.Random;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class) // Ensure ordered execution
public class MemberRegistrationAcceptanceTest {

    // Updated BASE_URL for Quarkus app
    private static final String HOST = "localhost";
    private static final String BASE_PATH = "/rest/members";
    private static int PORT = 8080; // Default port

    private static Random random = new Random();
    private static String lastCreatedMemberId; // To store ID for retrieval tests

    @BeforeAll
    public static void setup() {
        String portFromProperty = System.getProperty("app.host.port");
        if (portFromProperty != null && !portFromProperty.isEmpty()) {
            try {
                PORT = Integer.parseInt(portFromProperty);
            } catch (NumberFormatException e) {
                System.err.println("Warning: Could not parse app.host.port system property '" + portFromProperty + "'. Using default port " + PORT);
            }
        }

        RestAssured.baseURI = "http://" + HOST;
        RestAssured.port = PORT;
        RestAssured.basePath = BASE_PATH;

        // Cleanup any existing data by calling a DELETE all endpoint if available, or handle in tests.
        // For now, tests will try to use unique data.
    }

    private String generateValidPhoneNumber() {
        int length = 10 + random.nextInt(3); 
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    private String generateValidName() {
        String[] firstParts = {"Test", "User", "Sample", "Valid", "John", "Jane", "Alpha", "Beta"};
        String[] lastParts = {"Person", "Doe", "Smith", "Test", "User"};
        String name = firstParts[random.nextInt(firstParts.length)] + " " + lastParts[random.nextInt(lastParts.length)];
        if (name.length() > 25) {
            name = name.substring(0, 25);
        }
        return name;
    }

    private String generateUniqueEmail() {
        return "acceptancetest_" + System.currentTimeMillis() + "_" + random.nextInt(1000) + "@example.com";
    }

    @Test
    @Order(1) 
    public void testRegisterNewMemberSuccessfully() {
        long timestamp = System.currentTimeMillis();
        String testName = generateValidName();
        String uniqueEmail = "testuser_" + timestamp + "_accept@example.com";
        String testPhone = generateValidPhoneNumber();

        JsonObject newMemberPayload = Json.createObjectBuilder()
                .add("name", testName)
                .add("email", uniqueEmail)
                .add("phoneNumber", testPhone)
                .build();

        Response postResponse = given()
            .contentType(ContentType.JSON)
            .body(newMemberPayload.toString())
        .when()
            .post()
        .then()
            .statusCode(201) // Quarkus returns 201 Created
            .contentType(ContentType.JSON) // Quarkus returns the created entity
            .body("id", notNullValue())
            .body("name", equalTo(testName))
            .body("email", equalTo(uniqueEmail))
            .extract().response();
        
        lastCreatedMemberId = postResponse.jsonPath().getString("id"); // Store ObjectId as String
        assertNotNull(lastCreatedMemberId, "Created member ID should not be null");
        assertTrue(lastCreatedMemberId.matches("^[a-f0-9]{24}$*"), "ID should be a valid ObjectId string");

        // Verify by GETTING the specific member
        given()
            .accept(ContentType.JSON)
            .pathParam("id", lastCreatedMemberId)
        .when()
            .get("/{id}")
        .then()
            .statusCode(200)
            .body("id", equalTo(lastCreatedMemberId))
            .body("name", equalTo(testName))
            .body("email", equalTo(uniqueEmail));
    }

    // --- Validation Failure Tests --- 

    @Test
    @Order(2)
    public void testRegisterMemberWithInvalidName_TooLong() {
        String invalidName = "ThisNameIsDefinitelyMuchTooLongForTheValidationCriteriaIndeedYesItIs";
        JsonObject payload = Json.createObjectBuilder()
                .add("name", invalidName)
                .add("email", generateUniqueEmail())
                .add("phoneNumber", generateValidPhoneNumber())
                .build();

        given().contentType(ContentType.JSON).body(payload.toString()).when().post().then()
            .statusCode(400).contentType(ContentType.JSON).body("name", containsString("size must be between 1 and 25"));
    }

    @Test
    @Order(3)
    public void testRegisterMemberWithInvalidName_ContainsNumbers() {
        String invalidName = "NameWith123Numbers";
        JsonObject payload = Json.createObjectBuilder()
                .add("name", invalidName)
                .add("email", generateUniqueEmail())
                .add("phoneNumber", generateValidPhoneNumber())
                .build();

        given().contentType(ContentType.JSON).body(payload.toString()).when().post().then()
            .statusCode(400).contentType(ContentType.JSON).body("name", equalTo("Must not contain numbers"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"plainaddress", "@missingusername.com", "username@.com", "username@domain."})
    @Order(4)
    public void testRegisterMemberWithInvalidEmailFormat(String invalidEmail) {
        JsonObject payload = Json.createObjectBuilder()
                .add("name", generateValidName())
                .add("email", invalidEmail)
                .add("phoneNumber", generateValidPhoneNumber())
                .build();

        given().contentType(ContentType.JSON).body(payload.toString()).when().post().then()
            .statusCode(400).contentType(ContentType.JSON).body("email", containsString("must be a well-formed email address"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345", "123456789", "1234567890123"}) 
    @Order(5)
    public void testRegisterMemberWithInvalidPhoneNumber_Length(String invalidPhone) {
        JsonObject payload = Json.createObjectBuilder()
                .add("name", generateValidName())
                .add("email", generateUniqueEmail())
                .add("phoneNumber", invalidPhone)
                .build();

        given().contentType(ContentType.JSON).body(payload.toString()).when().post().then()
            .statusCode(400).contentType(ContentType.JSON)
            .body("phoneNumber", anyOf(
                containsString("size must be between 10 and 12"), 
                containsString("numeric value out of bounds")
            ));
            // Original check for "numeric value out of bounds" might be too specific to Hibernate/JPA if @Digits was the primary source.
            // Bean Validation @Size is more general for length.
    }

    @Test
    @Order(6)
    public void testRegisterMemberWithInvalidPhoneNumber_NonNumeric() {
        String invalidPhone = "123-456-7890"; // This should be caught by @Digits if it implies numbers only
                                         // Or by a @Pattern if one were added. Current Member only has @Digits.
                                         // The @Digits annotation implies numeric. The failure message might vary.
        JsonObject payload = Json.createObjectBuilder()
                .add("name", generateValidName())
                .add("email", generateUniqueEmail())
                .add("phoneNumber", invalidPhone)
                .build();

        given().contentType(ContentType.JSON).body(payload.toString()).when().post().then()
            .statusCode(400).contentType(ContentType.JSON)
            .body("phoneNumber", containsString("numeric value out of bounds") ); // Updated expected message
            // The original test expected "numeric value out of bounds". This might change with Quarkus/Panache validation.
            // Let's expect what Bean Validation typically says for @Digits or a general conversion error.
            // Actually, @Digits constraint is on the *number* of digits. It doesn't inherently forbid non-digits if the string isn't parsed as a number first.
            // The message "numeric value out of bounds" indicates it WAS parsed as number. If it contains non-digits, parsing as number fails earlier or validation for @Digits fails.
            // The original Member.java has @Digits(fraction = 0, integer = 12) AND @Column(name = "phone_number") for a String field.
            // For a String field, @Digits applies to the string representation. The old message was likely from Hibernate Validator specific interpretation.
            // With standard Bean Validation, for a String field and @Digits, if it contains non-digits, it won't match. Let's use a generic failure or check a more specific message from Quarkus HV.
    }

    @Test
    @Order(7)
    public void testRegisterMemberWithDuplicateEmail() {
        String existingEmail = generateUniqueEmail();
        JsonObject initialMemberPayload = Json.createObjectBuilder()
                .add("name", generateValidName()).add("email", existingEmail).add("phoneNumber", generateValidPhoneNumber()).build();

        given().contentType(ContentType.JSON).body(initialMemberPayload.toString()).when().post().then().statusCode(201);

        JsonObject duplicateMemberPayload = Json.createObjectBuilder()
                .add("name", "Duplicate Name").add("email", existingEmail).add("phoneNumber", generateValidPhoneNumber()).build();

        given().contentType(ContentType.JSON).body(duplicateMemberPayload.toString()).when().post().then()
            .statusCode(409).contentType(ContentType.JSON).body("email", equalTo("Email taken"));
    }

    @Test
    @Order(8)
    public void testRetrieveMemberById_Success() {
        // This test now relies on lastCreatedMemberId being set by testRegisterNewMemberSuccessfully
        // Or create a new one here for independence if @Order is not strictly guaranteed across all test runners/setups
        if (lastCreatedMemberId == null) { // Fallback if Order(1) didn't run or failed to set id
            Response resp = given().contentType(ContentType.JSON).body(Json.createObjectBuilder()
                .add("name", "Fallback GetUser").add("email", generateUniqueEmail()).add("phoneNumber", generateValidPhoneNumber()).build().toString())
                .when().post().then().statusCode(201).extract().response();
            lastCreatedMemberId = resp.jsonPath().getString("id");
        }
        assertNotNull(lastCreatedMemberId, "No member ID available for get by ID test.");

        given().accept(ContentType.JSON).pathParam("id", lastCreatedMemberId).when().get("/{id}").then()
            .statusCode(200).contentType(ContentType.JSON).body("id", equalTo(lastCreatedMemberId));
            // .body("name", equalTo(testName)) // Name is not known if using fallback
    }

    @Test
    @Order(9)
    public void testRetrieveMemberById_NotFound() {
        String nonExistentId = new org.bson.types.ObjectId().toString(); // Use BSON ObjectId for a valid format but non-existent ID
        given().accept(ContentType.JSON).pathParam("id", nonExistentId).when().get("/{id}").then().statusCode(404);
    }

    @Test
    @Order(10)
    public void testListAllMembers_IsOrderedByName() {
        String nameAlice = "Alice Wonderland";
        String nameBob = "Bob The Builder";
        String nameCharlie = "Charlie Chaplin";
        String emailAlice = generateUniqueEmail(); String emailBob = generateUniqueEmail(); String emailCharlie = generateUniqueEmail();

        // Clean slate for this specific test
        List<Map<String, Object>> existingMembers = given().accept(ContentType.JSON).when().get().then().statusCode(200).extract().jsonPath().getList("$");
        for (Map<String, Object> member : existingMembers) {
            given().pathParam("id", member.get("id").toString()).when().delete("/{id}").then().assertThat().statusCode(anyOf(is(200), is(204), is(404)));
        }

        JsonObject memberC = Json.createObjectBuilder().add("name", nameCharlie).add("email", emailCharlie).add("phoneNumber", generateValidPhoneNumber()).build();
        JsonObject memberA = Json.createObjectBuilder().add("name", nameAlice).add("email", emailAlice).add("phoneNumber", generateValidPhoneNumber()).build();
        JsonObject memberB = Json.createObjectBuilder().add("name", nameBob).add("email", emailBob).add("phoneNumber", generateValidPhoneNumber()).build();

        given().contentType(ContentType.JSON).body(memberC.toString()).when().post().then().statusCode(201);
        given().contentType(ContentType.JSON).body(memberA.toString()).when().post().then().statusCode(201);
        given().contentType(ContentType.JSON).body(memberB.toString()).when().post().then().statusCode(201);

        Response response = given().accept(ContentType.JSON).when().get().then().statusCode(200).contentType(ContentType.JSON).extract().response();
        List<Map<String, Object>> allMembers = response.jsonPath().getList("$");

        List<String> names = new ArrayList<>();
        for (Map<String, Object> member : allMembers) {
            names.add((String) member.get("name"));
        }
        // Only check the relevant members for this test based on known names
        List<String> testNames = names.stream().filter(name -> name.equals(nameAlice) || name.equals(nameBob) || name.equals(nameCharlie)).collect(java.util.stream.Collectors.toList());

        assertEquals(3, testNames.size(), "Expected to find the 3 test members.");
        assertEquals(nameAlice, testNames.get(0), "First member should be Alice.");
        assertEquals(nameBob, testNames.get(1), "Second member should be Bob.");
        assertEquals(nameCharlie, testNames.get(2), "Third member should be Charlie.");
    }
}
