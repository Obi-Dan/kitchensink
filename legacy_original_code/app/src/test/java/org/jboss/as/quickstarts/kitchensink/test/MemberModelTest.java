package org.jboss.as.quickstarts.kitchensink.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.jboss.as.quickstarts.kitchensink.model.Member;
import org.junit.Before;
import org.junit.Test;

/** Comprehensive unit tests for Member class including validation */
public class MemberModelTest {

  private Validator validator;

  @Before
  public void setupValidator() {
    ValidatorFactory factory =
        Validation.byDefaultProvider()
            .configure()
            .messageInterpolator(new ParameterMessageInterpolator())
            .buildValidatorFactory();
    validator = factory.getValidator();
  }

  @Test
  public void testValidMember() {
    // Create a valid member
    Member member = new Member();
    member.setName("John Doe");
    member.setEmail("john@example.com");
    member.setPhoneNumber("1234567890");

    // Validate the member
    Set<ConstraintViolation<Member>> violations = validator.validate(member);
    assertEquals(0, violations.size());
  }

  @Test
  public void testAllGettersAndSetters() {
    // Test all getters and setters for complete coverage
    Member member = new Member();

    // Test id property
    Long id = 1L;
    member.setId(id);
    assertEquals(id, member.getId());

    // Test name property
    String name = "Jane Smith";
    member.setName(name);
    assertEquals(name, member.getName());

    // Test email property
    String email = "jane@example.com";
    member.setEmail(email);
    assertEquals(email, member.getEmail());

    // Test phoneNumber property
    String phoneNumber = "9876543210";
    member.setPhoneNumber(phoneNumber);
    assertEquals(phoneNumber, member.getPhoneNumber());
  }

  @Test
  public void testNameValidation() {
    Member member = createValidMember();

    // Test null name
    member.setName(null);
    assertHasViolation(member, "name", "must not be null");

    // Test empty name
    member.setName("");
    assertHasViolation(member, "name", "size must be between 1 and 25");

    // Test name too long
    member.setName("This name is way too long for the validation constraint");
    assertHasViolation(member, "name", "size must be between 1 and 25");

    // Test name with numbers (should fail pattern validation)
    member.setName("John123");
    assertHasViolation(member, "name", "Must not contain numbers");
  }

  @Test
  public void testEmailValidation() {
    Member member = createValidMember();

    // Test null email
    member.setEmail(null);
    assertHasViolation(member, "email", "must not be null");

    // Test empty email
    member.setEmail("");
    assertHasViolation(member, "email", "must not be empty");

    // Test invalid email format
    member.setEmail("not-an-email");
    assertHasViolation(member, "email", "must be a well-formed email address");
  }

  @Test
  public void testPhoneNumberValidation() {
    Member member = createValidMember();

    // Test null phone number
    member.setPhoneNumber(null);
    assertHasViolation(member, "phoneNumber", "must not be null");

    // Test phone number too short
    member.setPhoneNumber("123456789");
    assertHasViolation(member, "phoneNumber", "size must be between 10 and 12");

    // Test phone number too long
    member.setPhoneNumber("1234567890123");
    assertHasViolation(member, "phoneNumber", "size must be between 10 and 12");

    // Test phone number with non-digits
    member.setPhoneNumber("123-456-7890");
    assertHasViolation(member, "phoneNumber", "numeric value out of bounds");
  }

  /** Helper method to create a valid member for testing */
  private Member createValidMember() {
    Member member = new Member();
    member.setName("John Doe");
    member.setEmail("john@example.com");
    member.setPhoneNumber("1234567890");
    return member;
  }

  /** Helper method to check for a specific violation */
  private void assertHasViolation(Member member, String property, String message) {
    Set<ConstraintViolation<Member>> violations = validator.validate(member);

    // If no violations found, fail the test
    if (violations.isEmpty()) {
      fail("Expected validation violation for property: " + property);
      return;
    }

    // Check if we have the expected violation
    boolean foundViolation = false;
    for (ConstraintViolation<Member> violation : violations) {
      if (violation.getPropertyPath().toString().equals(property)
          && violation.getMessage().contains(message)) {
        foundViolation = true;
        break;
      }
    }

    // Assert that we found the expected violation
    assertTrue(
        "Expected violation for property '"
            + property
            + "' with message containing '"
            + message
            + "'",
        foundViolation);
  }
}
