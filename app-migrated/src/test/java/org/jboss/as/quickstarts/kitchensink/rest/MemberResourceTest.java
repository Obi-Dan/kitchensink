package org.jboss.as.quickstarts.kitchensink.rest;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.bson.types.ObjectId;
import org.jboss.as.quickstarts.kitchensink.model.Member;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MemberResourceTest {

    private static String createdMemberId;

    @BeforeEach
    @AfterEach
    public void cleanup() {
        Member.deleteAll();
    }

    @Test
    @Order(1)
    void testCreateMember() {
        Member newMember = new Member();
        newMember.setName("Jane Doe");
        newMember.setEmail("jane.doe@example.com");
        newMember.setPhoneNumber("0987654321");

        String responseBody = given()
            .contentType(ContentType.JSON)
            .body(newMember)
            .when().post("/rest/members")
            .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("name", is("Jane Doe"))
                .body("email", is("jane.doe@example.com"))
                .extract().body().asString();
        
        createdMemberId = io.restassured.path.json.JsonPath.from(responseBody).getString("id");
        System.out.println("Created member ID: " + createdMemberId);
    }

    @Test
    @Order(2)
    void testListAllMembersAfterCreate() {
        Member member = new Member("Setup User", "setup@example.com", "111000111");
        member.persist();
        createdMemberId = member.id.toString();

        given()
            .when().get("/rest/members")
            .then()
                .statusCode(200)
                .body("", hasSize(1))
                .body("[0].name", is("Setup User"));
    }

    @Test
    @Order(3)
    void testLookupMemberById() {
        Member memberToTest = new Member("Specific User", "specific@example.com", "222333444");
        memberToTest.persist();
        String testId = memberToTest.id.toString();

        given()
            .pathParam("id", testId)
            .when().get("/rest/members/{id}")
            .then()
                .statusCode(200)
                .body("id", is(testId))
                .body("name", is("Specific User"));
    }

    @Test
    @Order(4)
    void testLookupMemberById_NotFound() {
        String nonExistentId = new ObjectId().toString();
        given()
            .pathParam("id", nonExistentId)
            .when().get("/rest/members/{id}")
            .then()
                .statusCode(404);
    }

    @Test
    @Order(5)
    void testCreateMember_ValidationError() {
        Member invalidMember = new Member();
        invalidMember.setName("NameWithDigits123");
        invalidMember.setEmail("not-an-email");
        invalidMember.setPhoneNumber("123");

        given()
            .contentType(ContentType.JSON)
            .body(invalidMember)
            .when().post("/rest/members")
            .then()
                .statusCode(400)
                .body("name", notNullValue())
                .body("email", notNullValue())
                .body("phoneNumber", notNullValue());
    }
    
    @Test
    @Order(6)
    void testCreateMember_DuplicateEmail() {
        Member member1 = new Member("First User", "duplicate.email@example.com", "1234567890");
        given()
            .contentType(ContentType.JSON)
            .body(member1)
            .when().post("/rest/members")
            .then()
                .statusCode(201);

        Member member2 = new Member("Second User", "duplicate.email@example.com", "0987654321");
        given()
            .contentType(ContentType.JSON)
            .body(member2)
            .when().post("/rest/members")
            .then()
                .statusCode(409)
                .body("email", containsString("Email taken"));
    }

    @Test
    @Order(7)
    void testListAllMembers_Empty() {
        Member.deleteAll(); 
        given()
          .when().get("/rest/members")
          .then()
             .statusCode(200)
             .body("", empty());
    }
}