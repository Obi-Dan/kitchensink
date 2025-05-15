package org.jboss.as.quickstarts.kitchensink.web;

import jakarta.validation.Valid;
import java.util.List;
import org.jboss.as.quickstarts.kitchensink.data.MemberRepository;
import org.jboss.as.quickstarts.kitchensink.model.Member;
import org.jboss.as.quickstarts.kitchensink.service.MemberRegistration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Spring MVC Controller for handling web requests related to member registration and listing. It
 * uses Thymeleaf for view rendering.
 */
@Controller
public final class MemberWebController {

  private static final String REGISTER_MEMBER_VIEW = "register-member";
  private static final String MEMBERS_VIEW = "members";
  private static final String REDIRECT_MEMBERS_VIEW = "redirect:/members";
  private static final String REDIRECT_REGISTER_MEMBER_VIEW = "redirect:/" + REGISTER_MEMBER_VIEW;

  /** Repository for member data access. */
  @Autowired private MemberRepository memberRepository;

  /** Service for member registration logic. */
  @Autowired private MemberRegistration memberRegistration; // Injecting the service

  /**
   * Displays a list of all registered members. The view "members.html" will be rendered.
   *
   * @param model The Spring UI model.
   * @return The name of the view to render.
   */
  @GetMapping("/members")
  public String listMembers(final Model model) {
    final List<Member> members = memberRepository.findAllByOrderByNameAsc();
    model.addAttribute("members", members);
    // This will resolve to src/main/resources/templates/members.html
    return MEMBERS_VIEW;
  }

  /**
   * Shows the member registration form. The view "register-member.html" will be rendered.
   *
   * @param model The Spring UI model.
   * @return The name of the view to render.
   */
  @GetMapping("/register")
  public String showRegistrationForm(final Model model) {
    // For form backing object
    model.addAttribute("newMember", new Member());
    // This will resolve to src/main/resources/templates/register-member.html
    return REGISTER_MEMBER_VIEW;
  }

  /**
   * Processes the submission of the member registration form. Validates the new member's data,
   * checks for email uniqueness, registers the member, and then redirects to the member list page
   * with a success or error message.
   *
   * @param newMember The member object populated from the form.
   * @param bindingResult Results of the validation.
   * @param model The Spring UI model.
   * @param redirectAttributes For adding flash attributes on redirect.
   * @return A redirect string to the member list or the registration form view name.
   */
  @PostMapping("/register")
  public String registerMember(
      @Valid @ModelAttribute("newMember") final Member newMember,
      final BindingResult bindingResult,
      final Model model,
      final RedirectAttributes redirectAttributes) {
    if (bindingResult.hasErrors()) {
      // If validation errors, return to the registration form page
      // The model already contains newMember with its validation errors
      return REGISTER_MEMBER_VIEW;
    }
    try {
      // The MemberResourceRESTService has emailAlreadyExists check inside
      // validateMember, which throws ValidationException. The
      // MemberRegistration service itself does not do this check directly.
      // For a web flow, it might be better to explicitly check for unique email
      // here or ensure the service layer consistently throws an exception that
      // can be caught and handled.
      // For now, let's assume MemberResourceRESTService.emailAlreadyExists is
      // accessible or replicated here.
      // OR, rely on the exception handling in MemberResourceRESTService if we call
      // it, but we call memberRegistration service here.

      // Let's add a direct call to the logic that was in
      // MemberResourceRESTService.emailAlreadyExists
      // This logic should ideally be in MemberRegistration or a validator.
      if (memberRepository.findByEmail(newMember.getEmail()).isPresent()) {
        // Manually add an error for email uniqueness if not covered by @Valid
        // or a custom validator. This specific error was handled by a
        // custom ValidationException previously.
        bindingResult.rejectValue("email", "error.newMember", "Email address is already in use.");
        return REGISTER_MEMBER_VIEW;
      }

      memberRegistration.register(newMember);
      redirectAttributes.addFlashAttribute(
          "successMessage", "Member " + newMember.getName() + " registered successfully!");
      // Redirect to member list on success
      return REDIRECT_MEMBERS_VIEW;
    } catch (Exception e) {
      // Catch other exceptions (like the one from service if email is taken,
      // if not caught above)
      // String errorMessage = getRootErrorMessage(e); // Re-implement or simplify
      redirectAttributes.addFlashAttribute(
          "errorMessage", "Registration failed: " + e.getMessage());
      return REDIRECT_REGISTER_MEMBER_VIEW;
    }
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public String handleException(final Exception e, final Model model) {
    model.addAttribute("errorMessage", "An unexpected error occurred: " + e.getMessage());
    // Optionally, return a specific error view
    // return "error-page";
    return MEMBERS_VIEW; // Or a more generic error view
  }
}
