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
        // Panache entities typically have ID assigned on persist,
        // but for mocking repository responses, we might set it.
        // Or, ensure the Member class allows setting ID if it's not auto-generated before persist.
        // For now, let's assume we can set it for test data.
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
                .statusCode(204); // Based on MemberResourceRESTService logic
        // .body(equalTo("[]")); // And it returns an empty array string for NO_CONTENT
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

        // Mock successful registration
        // The service will call validator.validate(member) first.
        // For simplicity, assume validation passes. We'll test validation failure separately.
        // The service then calls registrationService.register(member).
        // The register method in MemberRegistration might modify the member (e.g., assign an ID).
        // We need to simulate this if the response relies on it.
        org.mockito.Mockito.doAnswer(
                        invocation -> {
                            Member memberArg = invocation.getArgument(0);
                            memberArg.id = 3L; // Simulate ID assignment
                            return null; // void method
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
        newMember.name = ""; // Blank name - should fail validation
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
                .statusCode(400) // Bad Request due to validation
                // Check for the specific validation error message for 'name'
                // The actual error structure comes from createViolationResponse
                .body(
                        "name",
                        equalTo("size must be between 1 and 25")); // Adjusted: @Size triggers for
        // blank
        // Member.name
    }

    @Test
    public void testCreateMemberApi_validationFailure_nameTooLong() {
        Member newMember = new Member();
        newMember.name = "ThisNameIsDefinitelyWayTooLongForTheValidation"; // Exceeds 25 chars
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
        newMember.name = "Name123"; // Contains numbers
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
        newMember.email = null; // Null email
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
        newMember.email = "invalidemail"; // Invalid email format
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
        newMember.phoneNumber = null; // Null phone number

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
        newMember.phoneNumber = "12345"; // Too short

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
        newMember.phoneNumber = "1234567890123"; // Too long

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
        newMember.phoneNumber = "123-456-7890"; // Contains non-digits

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
                        equalTo(
                                "numeric value out of bounds (<12 digits>.<0 digits> expected)")); // Adjusted: @Digits triggers
    }

    @Test
    public void testCreateMemberApi_conflict_emailAlreadyExists() throws Exception {
        Member newMember = new Member();
        newMember.name = "Test User";
        newMember.email = "existing.email@example.com";
        newMember.phoneNumber = "1122334455";

        // Mock the registration service to throw EmailAlreadyExistsException
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
                .statusCode(409) // Conflict
                .body("email", equalTo("Email already exists"));
    }

    @Test
    public void testCreateMemberApi_conflict_idProvidedInRequest() {
        Member newMember = new Member();
        newMember.id = 123L; // ID is provided, which is not allowed for creation
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
                .statusCode(409) // Conflict
                .body(
                        "id",
                        equalTo(
                                "ID must not be set for new member registration. It will be auto-generated."));
    }
}
