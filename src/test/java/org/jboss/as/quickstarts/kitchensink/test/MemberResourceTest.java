package org.jboss.as.quickstarts.kitchensink.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import jakarta.persistence.NoResultException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Path;
import jakarta.validation.Validator;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.jboss.as.quickstarts.kitchensink.data.MemberRepository;
import org.jboss.as.quickstarts.kitchensink.model.Member;
import org.jboss.as.quickstarts.kitchensink.rest.MemberResourceRESTService;
import org.jboss.as.quickstarts.kitchensink.service.MemberRegistration;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for MemberResourceRESTService
 */
public class MemberResourceTest {

    @Mock
    private Validator validator;

    @Mock
    private MemberRepository repository;

    @Mock
    private MemberRegistration registration;

    @Mock
    private Logger log;

    private MemberResourceRESTService memberResource;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);

        memberResource = new MemberResourceRESTService();

        // Set up mocked dependencies via reflection
        try {
            java.lang.reflect.Field validatorField = MemberResourceRESTService.class.getDeclaredField("validator");
            validatorField.setAccessible(true);
            validatorField.set(memberResource, validator);

            java.lang.reflect.Field repositoryField = MemberResourceRESTService.class.getDeclaredField("repository");
            repositoryField.setAccessible(true);
            repositoryField.set(memberResource, repository);

            java.lang.reflect.Field registrationField = MemberResourceRESTService.class.getDeclaredField("registration");
            registrationField.setAccessible(true);
            registrationField.set(memberResource, registration);

            java.lang.reflect.Field logField = MemberResourceRESTService.class.getDeclaredField("log");
            logField.setAccessible(true);
            logField.set(memberResource, log);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set up test dependencies", e);
        }
    }

    @Test
    public void testListAllMembers() {
        // Set up mock return value
        List<Member> expectedMembers = new ArrayList<>();
        expectedMembers.add(createMember(1L, "John", "john@example.com", "1234567890"));
        expectedMembers.add(createMember(2L, "Jane", "jane@example.com", "0987654321"));

        when(repository.findAllOrderedByName()).thenReturn(expectedMembers);

        // Execute method
        List<Member> actualMembers = memberResource.listAllMembers();

        // Verify
        assertEquals(expectedMembers, actualMembers);
        verify(repository).findAllOrderedByName();
    }

    @Test
    public void testLookupMemberById_Found() {
        // Set up mock return value
        Long memberId = 1L;
        Member expectedMember = createMember(memberId, "John", "john@example.com", "1234567890");

        when(repository.findById(memberId)).thenReturn(expectedMember);

        // Execute method
        Member actualMember = memberResource.lookupMemberById(memberId);

        // Verify
        assertEquals(expectedMember, actualMember);
        verify(repository).findById(memberId);
    }

    @Test(expected = WebApplicationException.class)
    public void testLookupMemberById_NotFound() {
        // Set up mock return value
        Long memberId = 1L;
        when(repository.findById(memberId)).thenReturn(null);

        // Execute method - should throw exception
        memberResource.lookupMemberById(memberId);
    }

    @Test
    public void testCreateMember_Success() throws Exception {
        // Create member and set up mocks
        Member member = createMember(null, "John", "john@example.com", "1234567890");

        // Set up validator to return no violations
        Set<ConstraintViolation<Member>> emptyViolations = new HashSet<>();
        when(validator.validate(member)).thenReturn(emptyViolations);

        // Set up repository to indicate email doesn't exist
        when(repository.findByEmail(member.getEmail())).thenThrow(new NoResultException());

        // Execute method
        Response response = memberResource.createMember(member);

        // Verify
        verify(registration).register(member);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void testCreateMember_ValidationError() throws Exception {
        // Create member
        Member member = createMember(null, "John", "john@example.com", "1234567890");

        // Set up validator to return violations
        Set<ConstraintViolation<Member>> violations = new HashSet<>();
        ConstraintViolation<Member> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("email");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("Invalid email");
        violations.add(violation);

        when(validator.validate(member)).thenReturn(violations);

        // Execute method
        Response response = memberResource.createMember(member);

        // Verify
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        // Check response entity
        @SuppressWarnings("unchecked")
        Map<String, String> entity = (Map<String, String>) response.getEntity();
        assertEquals("Invalid email", entity.get("email"));
    }

    @Test
    public void testCreateMember_DuplicateEmail() throws Exception {
        // Create member
        Member member = createMember(null, "John", "john@example.com", "1234567890");

        // Set up validator to return no violations
        Set<ConstraintViolation<Member>> emptyViolations = new HashSet<>();
        when(validator.validate(member)).thenReturn(emptyViolations);

        // Set up repository to indicate email already exists
        Member existingMember = createMember(1L, "Existing", "john@example.com", "1111111111");
        when(repository.findByEmail(member.getEmail())).thenReturn(existingMember);

        // Execute method
        Response response = memberResource.createMember(member);

        // Verify
        assertEquals(Status.CONFLICT.getStatusCode(), response.getStatus());

        // Check response entity
        @SuppressWarnings("unchecked")
        Map<String, String> entity = (Map<String, String>) response.getEntity();
        assertEquals("Email taken", entity.get("email"));
    }

    @Test
    public void testCreateMember_OtherException() throws Exception {
        // Create member
        Member member = createMember(null, "John", "john@example.com", "1234567890");

        // Set up validator to return no violations
        Set<ConstraintViolation<Member>> emptyViolations = new HashSet<>();
        when(validator.validate(member)).thenReturn(emptyViolations);

        // Set up repository to indicate email doesn't exist
        when(repository.findByEmail(member.getEmail())).thenThrow(new NoResultException());

        // Set up registration to throw exception
        String errorMessage = "Database connection failed";
        doThrow(new Exception(errorMessage)).when(registration).register(member);

        // Execute method
        Response response = memberResource.createMember(member);

        // Verify
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        // Check response entity
        @SuppressWarnings("unchecked")
        Map<String, String> entity = (Map<String, String>) response.getEntity();
        assertEquals(errorMessage, entity.get("error"));
    }

    @Test
    public void testEmailAlreadyExists_Exists() {
        // Set up
        String email = "john@example.com";
        Member existingMember = createMember(1L, "John", email, "1234567890");
        when(repository.findByEmail(email)).thenReturn(existingMember);

        // Execute and verify
        assertTrue(memberResource.emailAlreadyExists(email));
    }

    @Test
    public void testEmailAlreadyExists_NotExists() {
        // Set up
        String email = "john@example.com";
        when(repository.findByEmail(email)).thenThrow(new NoResultException());

        // Execute and verify
        assertFalse(memberResource.emailAlreadyExists(email));
    }

    /**
     * Helper method to create a member for testing
     */
    private Member createMember(Long id, String name, String email, String phoneNumber) {
        Member member = new Member();
        member.setId(id);
        member.setName(name);
        member.setEmail(email);
        member.setPhoneNumber(phoneNumber);
        return member;
    }
}