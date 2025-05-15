package org.jboss.as.quickstarts.kitchensink.test;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jboss.as.quickstarts.kitchensink.data.MemberRepository;
import org.jboss.as.quickstarts.kitchensink.model.Member;
import org.jboss.as.quickstarts.kitchensink.rest.MemberResourceRESTService;
import org.jboss.as.quickstarts.kitchensink.service.MemberRegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

// java.util.logging.Logger

// import jakarta.validation.Validator; // Spring Boot provides this, mock if specific validation
// behavior is tested

@WebMvcTest(MemberResourceRESTService.class)
public class MemberRestResourceTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private MemberRepository memberRepository;

  @MockBean private MemberRegistrationService memberRegistrationService;

  // @MockBean // Validator is auto-configured by Spring. Only mock if you need to control its
  // behavior.
  // private Validator validator;

  @Autowired private ObjectMapper objectMapper; // For converting objects to JSON strings

  private List<Member> membersList;

  @BeforeEach
  void setUp() {
    membersList = new ArrayList<>();
    membersList.add(createMember(1L, "John Doe", "john@example.com", "1234567890"));
    membersList.add(createMember(2L, "Jane Doe", "jane@example.com", "0987654321"));
  }

  private Member createMember(Long id, String name, String email, String phone) {
    Member member = new Member();
    // ID would normally be set by persistence layer, but for test data, we might set it.
    member.setId(id);
    member.setName(name);
    member.setEmail(email);
    member.setPhoneNumber(phone);
    return member;
  }

  @Test
  public void testListAllMembers() throws Exception {
    given(memberRepository.findAllByOrderByNameAsc()).willReturn(membersList);

    mockMvc
        .perform(get("/rest/members"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].name", is("John Doe")))
        .andExpect(jsonPath("$[1].name", is("Jane Doe")));
  }

  @Test
  public void testLookupMemberById_Found() throws Exception {
    Member member = membersList.get(0);
    given(memberRepository.findById(1L)).willReturn(Optional.of(member));

    mockMvc
        .perform(get("/rest/members/{id}", 1L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name", is(member.getName())))
        .andExpect(jsonPath("$.email", is(member.getEmail())));
  }

  @Test
  public void testLookupMemberById_NotFound() throws Exception {
    given(memberRepository.findById(99L)).willReturn(Optional.empty());

    mockMvc.perform(get("/rest/members/{id}", 99L)).andExpect(status().isNotFound());
  }

  @Test
  public void testCreateMember_Success() throws Exception {
    Member newMember = createMember(null, "New Guy", "newguy@example.com", "5550100123");
    // Mock the registration service if it's called and does something we need to verify beyond
    // controller logic
    // For example, if register method returns the persisted member or throws specific exceptions
    // Mockito.doNothing().when(memberRegistration).register(any(Member.class)); // If void and no
    // side effects to check here

    // If MemberRegistration.register is void and we are testing the happy path controlled by the
    // REST service logic:
    // The controller calls memberRegistration.register(member) and then returns
    // ResponseEntity.ok().build();
    // We assume the validator passes for this happy path test.

    mockMvc
        .perform(
            post("/rest/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newMember)))
        .andExpect(status().isOk());

    // Optionally verify that registration.register was called
    // Mockito.verify(memberRegistration).register(ArgumentMatchers.any(Member.class));
  }

  // Add more tests for createMember (validation errors, conflict, etc.)
  // For example, test for ConstraintViolationException:
  // @Test
  // public void testCreateMember_ValidationError() throws Exception {
  //     Member invalidMember = createMember(null, "", "notanemail", "short"); // Invalid data
  //     // You might need to mock the validator if not using @Valid on entity in controller and
  // relying on manual validation call
  //     // Set<ConstraintViolation<Member>> violations = new HashSet<>();
  //     // violations.add(...); // create a mock ConstraintViolation
  //     // given(validator.validate(any(Member.class))).willReturn(violations);

  //     mockMvc.perform(post("/api/members")
  //             .contentType(MediaType.APPLICATION_JSON)
  //             .content(objectMapper.writeValueAsString(invalidMember)))
  //             .andExpect(status().isBadRequest())
  //             .andExpect(jsonPath("$.name").exists()) // Or whatever your error response format
  // is
  //             .andExpect(jsonPath("$.email").exists());
  // }

  // Test for email already exists (CONFLICT)
  // @Test
  // public void testCreateMember_EmailConflict() throws Exception {
  //     Member existingEmailMember = createMember(null, "Conflict User", "john@example.com",
  // "1112223333");
  //     // Assume john@example.com already exists. This check is done by
  // repository.findByEmail(...).isPresent()
  //     // in the controller before calling registration.register().
  //
  // given(memberRepository.findByEmail("john@example.com")).willReturn(Optional.of(membersList.get(0)));

  //     // We don't need to mock validator.validate() if the data is otherwise valid
  //     // Set<ConstraintViolation<Member>> noViolations = new HashSet<>();
  //     // given(validator.validate(any(Member.class))).willReturn(noViolations);

  //     mockMvc.perform(post("/api/members")
  //             .contentType(MediaType.APPLICATION_JSON)
  //             .content(objectMapper.writeValueAsString(existingEmailMember)))
  //             .andExpect(status().isConflict())
  //             .andExpect(jsonPath("$.email", is("Email taken")));
  // }
}
