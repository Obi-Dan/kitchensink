package org.jboss.as.quickstarts.kitchensink.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.jboss.as.quickstarts.kitchensink.model.Member;
import org.junit.Test;

/** Simple unit test for Member class without validation */
public class MemberSimpleTest {

  @Test
  public void testBasicMemberProperties() {
    // Create a member object
    Member member = new Member();

    // Test id property
    Long id = 1L;
    member.setId(id);
    assertEquals(id, member.getId());
    assertNotNull(member.getId());

    // Test name property
    String name = "John Doe";
    member.setName(name);
    assertEquals(name, member.getName());

    // Test email property
    String email = "john@example.com";
    member.setEmail(email);
    assertEquals(email, member.getEmail());

    // Test phoneNumber property
    String phoneNumber = "1234567890";
    member.setPhoneNumber(phoneNumber);
    assertEquals(phoneNumber, member.getPhoneNumber());
  }
}
