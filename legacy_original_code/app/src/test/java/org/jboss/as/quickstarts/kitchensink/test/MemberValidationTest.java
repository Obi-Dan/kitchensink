package org.jboss.as.quickstarts.kitchensink.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.jboss.as.quickstarts.kitchensink.model.Member;
import org.junit.Test;

/** Simple unit test for Member class */
public class MemberValidationTest {

  @Test
  public void testMemberProperties() {
    // Create a member object
    Member member = new Member();

    // Set properties
    member.setName("John Doe");
    member.setEmail("john@example.com");
    member.setPhoneNumber("1234567890");

    // Test getters
    assertEquals("John Doe", member.getName());
    assertEquals("john@example.com", member.getEmail());
    assertEquals("1234567890", member.getPhoneNumber());

    // Test ID handling
    member.setId(1L);
    assertEquals(Long.valueOf(1L), member.getId());
    assertNotNull(member.getId());
  }
}
