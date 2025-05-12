package org.jboss.as.quickstarts.kitchensink.test;

import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.eq;

import java.util.logging.Logger;

import jakarta.enterprise.event.Event;
import jakarta.persistence.EntityManager;

import org.jboss.as.quickstarts.kitchensink.model.Member;
import org.jboss.as.quickstarts.kitchensink.service.MemberRegistration;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for MemberRegistration service
 */
public class MemberRegistrationTest {

    @Mock
    private Logger log;

    @Mock
    private EntityManager em;

    @Mock
    private Event<Member> memberEventSrc;

    private MemberRegistration registration;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);

        registration = new MemberRegistration();

        // Set up mocked dependencies via reflection
        try {
            java.lang.reflect.Field logField = MemberRegistration.class.getDeclaredField("log");
            logField.setAccessible(true);
            logField.set(registration, log);

            java.lang.reflect.Field emField = MemberRegistration.class.getDeclaredField("em");
            emField.setAccessible(true);
            emField.set(registration, em);

            java.lang.reflect.Field eventField = MemberRegistration.class.getDeclaredField("memberEventSrc");
            eventField.setAccessible(true);
            eventField.set(registration, memberEventSrc);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set up test dependencies", e);
        }
    }

    @Test
    public void testRegister() throws Exception {
        // Create test member
        Member member = new Member();
        member.setName("John Doe");
        member.setEmail("john@example.com");
        member.setPhoneNumber("1234567890");

        // Call method
        registration.register(member);

        // Verify interactions
        verify(log).info("Registering " + member.getName());
        verify(em).persist(member);
        verify(memberEventSrc).fire(eq(member));
    }
}