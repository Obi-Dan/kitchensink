package org.jboss.as.quickstarts.kitchensink.service;

import io.quarkus.test.junit.QuarkusTest;
// import jakarta.enterprise.event.Event; // No longer directly used for mock/verify
import jakarta.inject.Inject;
import org.jboss.as.quickstarts.kitchensink.model.Member;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
public class MemberRegistrationTest {

    @Inject
    MemberRegistration memberRegistration;

    // Event<Member> memberEventSrc; // Event firing will be tested implicitly by observers or integration tests

    @BeforeEach
    @AfterEach
    public void cleanupDatabase() {
        // It's good practice to ensure a clean state for each test method.
        // Delete all members to avoid interference between tests.
        Member.deleteAll();
    }

    @Test
    public void testRegister() throws Exception {
        Member newMember = new Member();
        newMember.setName("Test Reg");
        newMember.setEmail("test.reg.service@example.com"); // Unique email for this test
        newMember.setPhoneNumber("1231231234");

        // Ensure no member with this email exists before the test
        Member.delete("email", newMember.getEmail());

        memberRegistration.register(newMember);

        // Verify member was persisted
        assertNotNull(newMember.id, "Member ID should be set after registration");
        Member persistedMember = Member.findById(newMember.id);
        assertNotNull(persistedMember, "Member should be findable in DB after registration");
        assertEquals("Test Reg", persistedMember.getName());
        assertEquals(newMember.getEmail(), persistedMember.getEmail());

        // Cleanup: It's often better to do cleanup in @AfterEach to ensure it runs even if assertions fail.
        // However, if we want to ensure this specific member is gone, explicit delete is fine too.
        // Since @AfterEach calls Member.deleteAll(), this explicit delete is somewhat redundant but harmless.
        if (persistedMember != null) {
             Member.deleteById(persistedMember.id);
        }
    }
} 