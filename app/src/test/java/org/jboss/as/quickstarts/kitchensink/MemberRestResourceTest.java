/*
 * JBoss, Home of Professional Open Source
 * Copyright 2023, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.quickstarts.kitchensink;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import io.quarkus.panache.common.Sort;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.restassured.RestAssured;
import jakarta.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.jboss.as.quickstarts.kitchensink.model.Member;
import org.jboss.as.quickstarts.kitchensink.model.MemberRepository;
import org.jboss.as.quickstarts.kitchensink.service.MemberRegistration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

@QuarkusTest
public class MemberRestResourceTest {

    @InjectMock MemberRepository memberRepository;
    @InjectMock MemberRegistration memberRegistration;

    private List<Member> membersList;

    @BeforeEach
    public void setup() {
        System.out.println(
                "Setting up MemberRestResourceTest. MemberRepository mock: " + memberRepository);
        membersList = new ArrayList<>();
        membersList.add(createMember(0L, "John Doe", "john.doe@example.com", "1234567890"));
        membersList.add(createMember(1L, "Jane Doe", "jane.doe@example.com", "0987654321"));
    }

    private Member createMember(Long id, String name, String email, String phone) {
        Member member = new Member();
        member.id = id;
        member.name = name;
        member.email = email;
        member.phoneNumber = phone;
        return member;
    }

    @Test
    public void testGetAllMembersApi_whenMembersExist() {
        when(memberRepository.listAll(ArgumentMatchers.any(Sort.class))).thenReturn(membersList);

        RestAssured.given()
                .log()
                .all()
                .when()
                .get("/rest/app/api/members")
                .then()
                .log()
                .all()
                .statusCode(200)
                .contentType(MediaType.APPLICATION_JSON)
                .body("$", hasSize(2))
                .body("[0].name", equalTo("John Doe"))
                .body("[0].email", equalTo("john.doe@example.com"))
                .body("[1].name", equalTo("Jane Doe"))
                .body("[1].email", equalTo("jane.doe@example.com"));
    }

    @Test
    public void testGetAllMembersApi_whenNoMembersExist() {
        when(memberRepository.listAll(ArgumentMatchers.any(Sort.class)))
                .thenReturn(Collections.emptyList());

        RestAssured.given()
                .log()
                .all()
                .when()
                .get("/rest/app/api/members")
                .then()
                .log()
                .all()
                .statusCode(204);
    }

    @Test
    public void testLookupMemberByIdApi_whenMemberExists() {
        Member member = membersList.get(0);
        when(memberRepository.findByIdOptional(ArgumentMatchers.eq(member.id)))
                .thenReturn(Optional.of(member));

        RestAssured.given()
                .log()
                .all()
                .when()
                .get("/rest/app/api/members/" + member.id)
                .then()
                .log()
                .all()
                .statusCode(200)
                .contentType(MediaType.APPLICATION_JSON)
                .body("name", equalTo(member.name))
                .body("email", equalTo(member.email))
                .body("phoneNumber", equalTo(member.phoneNumber));
    }

    @Test
    public void testLookupMemberByIdApi_whenMemberDoesNotExist() {
        Long nonExistentId = 999L;
        when(memberRepository.findByIdOptional(ArgumentMatchers.eq(nonExistentId)))
                .thenReturn(Optional.empty());

        RestAssured.given()
                .log()
                .all()
                .when()
                .get("/rest/app/api/members/" + nonExistentId)
                .then()
                .log()
                .all()
                .statusCode(404);
    }

    @Test
    public void testCreateMemberApi_success() throws Exception {
        Member newMember = new Member();
        newMember.name = "Test User";
        newMember.email = "test.user@example.com";
        newMember.phoneNumber = "1122334455";

        org.mockito.Mockito.doAnswer(
                        invocation -> {
                            Member memberArg = invocation.getArgument(0);
                            memberArg.id = 3L;
                            return null;
                        })
                .when(memberRegistration)
                .register(ArgumentMatchers.any(Member.class));

        RestAssured.given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(newMember)
                .log()
                .all()
                .when()
                .post("/rest/app/api/members")
                .then()
                .log()
                .all()
                .statusCode(201)
                .body("id", equalTo(3))
                .body("name", equalTo(newMember.name))
                .body("email", equalTo(newMember.email));
    }

    @Test
    public void testCreateMemberApi_validationFailure_blankName() {
        Member newMember = new Member();
        newMember.name = "";
        newMember.email = "valid.email@example.com";
        newMember.phoneNumber = "1234567890";

        RestAssured.given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(newMember)
                .log()
                .all()
                .when()
                .post("/rest/app/api/members")
                .then()
                .log()
                .all()
                .statusCode(400)
                .body("name", equalTo("size must be between 1 and 25"));
    }

    @Test
    public void testCreateMemberApi_validationFailure_nameTooLong() {
        Member newMember = new Member();
        newMember.name = "ThisNameIsDefinitelyWayTooLongForTheValidation";
        newMember.email = "valid.email@example.com";
        newMember.phoneNumber = "1234567890";

        RestAssured.given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(newMember)
                .log()
                .all()
                .when()
                .post("/rest/app/api/members")
                .then()
                .log()
                .all()
                .statusCode(400)
                .body("name", equalTo("size must be between 1 and 25"));
    }

    @Test
    public void testCreateMemberApi_validationFailure_nameWithNumbers() {
        Member newMember = new Member();
        newMember.name = "Name123";
        newMember.email = "valid.email@example.com";
        newMember.phoneNumber = "1234567890";

        RestAssured.given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(newMember)
                .log()
                .all()
                .when()
                .post("/rest/app/api/members")
                .then()
                .log()
                .all()
                .statusCode(400)
                .body("name", equalTo("Must not contain numbers"));
    }

    @Test
    public void testCreateMemberApi_validationFailure_nullEmail() {
        Member newMember = new Member();
        newMember.name = "Valid Name";
        newMember.email = null;
        newMember.phoneNumber = "1234567890";

        RestAssured.given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(newMember)
                .log()
                .all()
                .when()
                .post("/rest/app/api/members")
                .then()
                .log()
                .all()
                .statusCode(400)
                .body("email", equalTo("must not be null"));
    }

    @Test
    public void testCreateMemberApi_validationFailure_invalidEmailFormat() {
        Member newMember = new Member();
        newMember.name = "Valid Name";
        newMember.email = "invalidemail";
        newMember.phoneNumber = "1234567890";

        RestAssured.given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(newMember)
                .log()
                .all()
                .when()
                .post("/rest/app/api/members")
                .then()
                .log()
                .all()
                .statusCode(400)
                .body("email", equalTo("must be a well-formed email address"));
    }

    @Test
    public void testCreateMemberApi_validationFailure_nullPhoneNumber() {
        Member newMember = new Member();
        newMember.name = "Valid Name";
        newMember.email = "valid.email@example.com";
        newMember.phoneNumber = null;

        RestAssured.given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(newMember)
                .log()
                .all()
                .when()
                .post("/rest/app/api/members")
                .then()
                .log()
                .all()
                .statusCode(400)
                .body("phoneNumber", equalTo("must not be null"));
    }

    @Test
    public void testCreateMemberApi_validationFailure_phoneNumberTooShort() {
        Member newMember = new Member();
        newMember.name = "Valid Name";
        newMember.email = "valid.email@example.com";
        newMember.phoneNumber = "12345";

        RestAssured.given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(newMember)
                .log()
                .all()
                .when()
                .post("/rest/app/api/members")
                .then()
                .log()
                .all()
                .statusCode(400)
                .body("phoneNumber", equalTo("size must be between 10 and 12"));
    }

    @Test
    public void testCreateMemberApi_validationFailure_phoneNumberTooLong() {
        Member newMember = new Member();
        newMember.name = "Valid Name";
        newMember.email = "valid.email@example.com";
        newMember.phoneNumber = "1234567890123";

        String phoneNumberError =
                RestAssured.given()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(newMember)
                        .log()
                        .all()
                        .when()
                        .post("/rest/app/api/members")
                        .then()
                        .log()
                        .all()
                        .statusCode(400)
                        .extract()
                        .path("phoneNumber");

        assertTrue(
                "size must be between 10 and 12".equals(phoneNumberError)
                        || "numeric value out of bounds (<12 digits>.<0 digits> expected)"
                                .equals(phoneNumberError),
                "Phone number validation message did not match expected options. Got: "
                        + phoneNumberError);
    }

    @Test
    public void testCreateMemberApi_validationFailure_phoneNumberWithNonDigits() {
        Member newMember = new Member();
        newMember.name = "Valid Name";
        newMember.email = "valid.email@example.com";
        newMember.phoneNumber = "123-456-7890";

        RestAssured.given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(newMember)
                .log()
                .all()
                .when()
                .post("/rest/app/api/members")
                .then()
                .log()
                .all()
                .statusCode(400)
                .body(
                        "phoneNumber",
                        equalTo("numeric value out of bounds (<12 digits>.<0 digits> expected)"));
    }

    @Test
    public void testCreateMemberApi_conflict_emailAlreadyExists() throws Exception {
        Member newMember = new Member();
        newMember.name = "Test User";
        newMember.email = "existing.email@example.com";
        newMember.phoneNumber = "1122334455";

        org.mockito.Mockito.doThrow(
                        new MemberRegistration.EmailAlreadyExistsException("Email already exists"))
                .when(memberRegistration)
                .register(ArgumentMatchers.any(Member.class));

        RestAssured.given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(newMember)
                .log()
                .all()
                .when()
                .post("/rest/app/api/members")
                .then()
                .log()
                .all()
                .statusCode(409)
                .body("email", equalTo("Email already exists"));
    }

    @Test
    public void testCreateMemberApi_conflict_idProvidedInRequest() {
        Member newMember = new Member();
        newMember.id = 123L;
        newMember.name = "Test User With ID";
        newMember.email = "id.provided@example.com";
        newMember.phoneNumber = "1122334455";

        RestAssured.given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(newMember)
                .log()
                .all()
                .when()
                .post("/rest/app/api/members")
                .then()
                .log()
                .all()
                .statusCode(409)
                .body(
                        "id",
                        equalTo(
                                "ID must not be set for new member registration. It will be auto-generated."));
    }

    @Test
    public void testCreateMemberApi_nullMember() {
        RestAssured.given()
                .contentType(MediaType.APPLICATION_JSON)
                .body("null")
                .log()
                .all()
                .when()
                .post("/rest/app/api/members")
                .then()
                .log()
                .all()
                .statusCode(400)
                .body(equalTo("Member data is required."));
    }

    @Test
    public void testCreateMemberApi_genericExceptionDuringRegistration() throws Exception {
        Member newMember = new Member();
        newMember.name = "Test User GenEx";
        newMember.email = "test.genex@example.com";
        newMember.phoneNumber = "1234567890";

        org.mockito.Mockito.doThrow(new RuntimeException("Simulated generic error"))
                .when(memberRegistration)
                .register(ArgumentMatchers.any(Member.class));

        RestAssured.given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(newMember)
                .log()
                .all()
                .when()
                .post("/rest/app/api/members")
                .then()
                .log()
                .all()
                .statusCode(500)
                .body("error", equalTo("An unexpected error occurred: Simulated generic error"));
    }

    @Test
    public void testGetWebUi_whenMembersExist() {
        // Arrange
        when(memberRepository.listAll(ArgumentMatchers.any(Sort.class))).thenReturn(membersList);

        // Act & Assert HTTP Response
        String htmlResponse =
                RestAssured.given()
                        .log()
                        .all()
                        .when()
                        .get("/rest/app/ui") // Path to the UI endpoint
                        .then()
                        .log()
                        .all()
                        .statusCode(200)
                        .contentType(MediaType.TEXT_HTML)
                        .extract()
                        .asString(); // Extract HTML for further optional assertions

        // Verify interaction with the (mocked) repository
        Mockito.verify(memberRepository).listAll(ArgumentMatchers.any(Sort.class));

        // Optional: Assert that key data appears in the HTML
        // This can be brittle if the HTML structure changes frequently.
        assertTrue(htmlResponse.contains("John Doe"));
        assertTrue(htmlResponse.contains("jane.doe@example.com"));
    }

    @Test
    public void testGetWebUi_whenNoMembersExist() {
        // Arrange
        when(memberRepository.listAll(ArgumentMatchers.any(Sort.class)))
                .thenReturn(Collections.emptyList());

        // Act & Assert HTTP Response
        String htmlResponse =
                RestAssured.given()
                        .log()
                        .all()
                        .when()
                        .get("/rest/app/ui") // Path to the UI endpoint
                        .then()
                        .log()
                        .all()
                        .statusCode(200)
                        .contentType(MediaType.TEXT_HTML)
                        .extract()
                        .asString();

        // Verify interaction with the (mocked) repository
        Mockito.verify(memberRepository).listAll(ArgumentMatchers.any(Sort.class));

        // Optional: Assert that a message about no members is shown, or key elements are absent
        // For example, check that the member table body is empty or a specific "no members" message
        // is present.
        // This depends on the actual HTML structure when no members are available.
        // For now, we'll just check that common member data is NOT present.
        assertTrue(!htmlResponse.contains("John Doe"));
        assertTrue(!htmlResponse.contains("jane.doe@example.com"));
        // Add more specific assertions based on how your template handles an empty list
    }

    @Test
    public void testRegisterViaUi_genericException() throws Exception {
        // Arrange
        // Mock registrationService to throw a generic RuntimeException
        Mockito.doThrow(new RuntimeException("Simulated generic UI error"))
                .when(memberRegistration)
                .register(ArgumentMatchers.any(Member.class));

        // Mock repository to return an empty list for the subsequent page render
        when(memberRepository.listAll(ArgumentMatchers.any(Sort.class)))
                .thenReturn(Collections.emptyList());

        // Act & Assert HTTP Response
        String htmlResponse =
                RestAssured.given()
                        .formParam("name", "Error User")
                        .formParam("email", "error.user@example.com")
                        .formParam("phoneNumber", "0000000000")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .log()
                        .all()
                        .when()
                        .post("/rest/app/ui/register")
                        .then()
                        .log()
                        .all()
                        .statusCode(200) // UI still renders, but with an error message
                        .contentType(MediaType.TEXT_HTML)
                        .extract()
                        .asString();

        // Verify that register was called
        Mockito.verify(memberRegistration).register(ArgumentMatchers.any(Member.class));

        // Verify that listAll was called for rendering the page again
        Mockito.verify(memberRepository).listAll(ArgumentMatchers.any(Sort.class));

        // Assert that the generic error message is present in the HTML
        // This depends on how your template displays globalMessages of type "error"
        assertTrue(htmlResponse.contains("An unexpected error occurred during registration."));
    }
}
