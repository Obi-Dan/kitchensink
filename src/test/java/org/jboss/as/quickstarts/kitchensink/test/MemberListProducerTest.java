package org.jboss.as.quickstarts.kitchensink.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.NotificationOptions;
import jakarta.enterprise.util.TypeLiteral;

import org.jboss.as.quickstarts.kitchensink.data.MemberListProducer;
import org.jboss.as.quickstarts.kitchensink.data.MemberRepository;
import org.jboss.as.quickstarts.kitchensink.model.Member;
import org.junit.Before;
import org.junit.Test;

/**
 * Simple happy path test for MemberListProducer
 */
public class MemberListProducerTest {

    private MemberListProducer producer;
    private TestMemberRepository repository;
    private List<Member> members;

    @Before
    public void setup() throws Exception {
        // Create the producer
        producer = new MemberListProducer();

        // Create test dependencies
        repository = new TestMemberRepository();

        // Create test data
        members = new ArrayList<>();
        members.add(createMember(1L, "John Doe", "john@example.com", "1234567890"));
        members.add(createMember(2L, "Jane Doe", "jane@example.com", "0987654321"));
        repository.setMembers(members);

        // Inject dependencies via reflection
        injectDependency(producer, "repository", repository);

        // Create a no-op event handler
        TestMemberEventSource eventSource = new TestMemberEventSource();
        injectDependency(producer, "memberListEventSrc", eventSource);
    }

    @Test
    public void testHappyPath() throws Exception {
        // Call the method that is normally triggered by CDI events
        producer.onMemberListChanged(null);

        // Get the members field through reflection
        Field membersField = MemberListProducer.class.getDeclaredField("members");
        membersField.setAccessible(true);
        List<Member> producerMembers = (List<Member>) membersField.get(producer);

        // Verify that the producer has the members from the repository
        assertNotNull(producerMembers);
        assertEquals(2, producerMembers.size());
        assertEquals("John Doe", producerMembers.get(0).getName());
        assertEquals("john@example.com", producerMembers.get(0).getEmail());
        assertEquals("Jane Doe", producerMembers.get(1).getName());
        assertEquals("jane@example.com", producerMembers.get(1).getEmail());
    }

    @Test
    public void testGetMembers() throws Exception {
        // Call the method that is normally triggered by CDI events
        producer.onMemberListChanged(null);

        // Call the producer method
        List<Member> producedMembers = producer.getMembers();

        // Verify that the produced members match the expected list
        assertNotNull(producedMembers);
        assertEquals(2, producedMembers.size());
        assertEquals("John Doe", producedMembers.get(0).getName());
        assertEquals("jane@example.com", producedMembers.get(1).getEmail());
    }

    // Helper method to inject dependencies using reflection
    private void injectDependency(Object target, String fieldName, Object dependency) throws Exception {
        Field field = MemberListProducer.class.getDeclaredField(fieldName);
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
        private List<Member> members;

        public void setMembers(List<Member> members) {
            this.members = members;
        }

        @Override
        public List<Member> findAllOrderedByName() {
            return members;
        }
    }

    // Test implementation of Event<List<Member>>
    private static class TestMemberEventSource implements Event<List<Member>> {
        @Override
        public void fire(List<Member> event) {
            // No-op for testing
        }

        @Override
        public <U extends List<Member>> CompletionStage<U> fireAsync(U event) {
            // No-op for testing
            return null;
        }

        @Override
        public <U extends List<Member>> CompletionStage<U> fireAsync(U event, NotificationOptions options) {
            // No-op for testing
            return null;
        }

        @Override
        public Event<List<Member>> select(java.lang.annotation.Annotation... qualifiers) {
            return this;
        }

        @Override
        public <U extends List<Member>> Event<U> select(Class<U> subtype, java.lang.annotation.Annotation... qualifiers) {
            return null;
        }

        @Override
        public <U extends List<Member>> Event<U> select(TypeLiteral<U> subtype, java.lang.annotation.Annotation... qualifiers) {
            return null;
        }
    }
}