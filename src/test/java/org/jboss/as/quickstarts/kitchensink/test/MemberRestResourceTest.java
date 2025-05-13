package org.jboss.as.quickstarts.kitchensink.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.executable.ExecutableValidator;
import jakarta.validation.metadata.BeanDescriptor;
import jakarta.ws.rs.core.Response;

import org.jboss.as.quickstarts.kitchensink.data.MemberRepository;
import org.jboss.as.quickstarts.kitchensink.model.Member;
import org.jboss.as.quickstarts.kitchensink.rest.MemberResourceRESTService;
import org.jboss.as.quickstarts.kitchensink.service.MemberRegistration;
import org.junit.Before;
import org.junit.Test;

/**
 * Simple happy path test for MemberResourceRESTService
 */
public class MemberRestResourceTest {

    private MemberResourceRESTService restService;
    private TestMemberRepository repository;
    private TestMemberRegistration registration;
    private TestValidator validator;
    private List<Member> members;

    @Before
    public void setup() throws Exception {
        // Create the REST service
        restService = new MemberResourceRESTService();

        // Create test data
        members = new ArrayList<>();
        members.add(createMember(1L, "John Doe", "john@example.com", "1234567890"));
        members.add(createMember(2L, "Jane Doe", "jane@example.com", "0987654321"));

        // Create test dependencies
        repository = new TestMemberRepository();
        repository.setMembers(members);
        registration = new TestMemberRegistration();
        validator = new TestValidator();

        // Inject dependencies via reflection
        injectDependency(restService, "repository", repository);
        injectDependency(restService, "registration", registration);
        injectDependency(restService, "validator", validator);
        injectDependency(restService, "log", Logger.getLogger(MemberResourceRESTService.class.getName()));
    }

    @Test
    public void testListAllMembers() {
        // Call the REST method
        List<Member> returnedMembers = restService.listAllMembers();

        // Verify the result
        assertNotNull(returnedMembers);
        assertEquals(2, returnedMembers.size());
        assertEquals("John Doe", returnedMembers.get(0).getName());
        assertEquals("jane@example.com", returnedMembers.get(1).getEmail());
    }

    @Test
    public void testLookupMemberById() {
        // Call the REST method
        Member member = restService.lookupMemberById(1L);

        // Verify the result
        assertNotNull(member);
        assertEquals("John Doe", member.getName());
        assertEquals("john@example.com", member.getEmail());
    }

    @Test
    public void testCreateMember() throws Exception {
        // Create a new member to register
        Member newMember = createMember(null, "New User", "new@example.com", "5551234567");

        // Call the REST method
        Response response = restService.createMember(newMember);

        // Verify the result
        assertNotNull(response);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("New User", registration.getLastRegisteredMember().getName());
        assertEquals("new@example.com", registration.getLastRegisteredMember().getEmail());
    }

    // Helper method to inject dependencies using reflection
    private void injectDependency(Object target, String fieldName, Object dependency) throws Exception {
        Field field = MemberResourceRESTService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, dependency);
    }

    // Helper method to create a test member
    private Member createMember(Long id, String name, String email, String phone) {
        Member member = new Member();
        member.setId(id);
        member.setName(name);
        member.setEmail(email);
        member.setPhoneNumber(phone);
        return member;
    }

    // Test implementation of MemberRepository
    private static class TestMemberRepository extends MemberRepository {
        private List<Member> members = new ArrayList<>();

        public void setMembers(List<Member> members) {
            this.members = members;
        }

        @Override
        public List<Member> findAllOrderedByName() {
            return members;
        }

        @Override
        public Member findById(Long id) {
            for (Member member : members) {
                if (id.equals(member.getId())) {
                    return member;
                }
            }
            return null;
        }

        @Override
        public Member findByEmail(String email) {
            for (Member member : members) {
                if (email.equals(member.getEmail())) {
                    return member;
                }
            }
            throw new jakarta.persistence.NoResultException("Email not found: " + email);
        }
    }

    // Test implementation of MemberRegistration
    private static class TestMemberRegistration extends MemberRegistration {
        private Member lastRegisteredMember;

        @Override
        public void register(Member member) throws Exception {
            this.lastRegisteredMember = member;
        }

        public Member getLastRegisteredMember() {
            return lastRegisteredMember;
        }
    }

    // Test implementation of Validator
    private static class TestValidator implements Validator {
        @Override
        public <T> Set<ConstraintViolation<T>> validate(T object, Class<?>... groups) {
            return new HashSet<>(); // Return empty set - no validation errors
        }

        @Override
        public <T> Set<ConstraintViolation<T>> validateProperty(T object, String propertyName, Class<?>... groups) {
            return new HashSet<>(); // Return empty set - no validation errors
        }

        @Override
        public <T> Set<ConstraintViolation<T>> validateValue(Class<T> beanType, String propertyName, Object value, Class<?>... groups) {
            return new HashSet<>(); // Return empty set - no validation errors
        }

        @Override
        public BeanDescriptor getConstraintsForClass(Class<?> clazz) {
            return null;
        }

        @Override
        public <T> T unwrap(Class<T> type) {
            return null;
        }

        @Override
        public ExecutableValidator forExecutables() {
            return null;
        }
    }
}