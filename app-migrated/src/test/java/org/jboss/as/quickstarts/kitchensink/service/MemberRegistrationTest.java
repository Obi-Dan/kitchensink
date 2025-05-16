/*
 * JBoss, Home of Professional Open Source
 * Copyright 2024, Red Hat, Inc. and/or its affiliates, and individual
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
package org.jboss.as.quickstarts.kitchensink.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.quarkus.test.junit.QuarkusTest;
// import jakarta.enterprise.event.Event; // No longer directly used for mock/verify
import jakarta.inject.Inject;
import org.jboss.as.quickstarts.kitchensink.model.Member;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class MemberRegistrationTest {

    @Inject MemberRegistration memberRegistration;

    // Event<Member> memberEventSrc; // Event firing will be tested implicitly by observers or
    // integration tests

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

        // Cleanup: It's often better to do cleanup in @AfterEach to ensure it runs even if
        // assertions fail.
        // However, if we want to ensure this specific member is gone, explicit delete is fine too.
        // Since @AfterEach calls Member.deleteAll(), this explicit delete is somewhat redundant but
        // harmless.
        if (persistedMember != null) {
            Member.deleteById(persistedMember.id);
        }
    }
}
