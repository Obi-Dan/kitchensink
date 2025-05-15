/*
 * JBoss, Home of Professional Open Source
 * Copyright 2015, Red Hat, Inc. and/or its affiliates, and individual
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
package org.jboss.as.quickstarts.kitchensink.rest;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.jboss.as.quickstarts.kitchensink.data.MemberRepository;
import org.jboss.as.quickstarts.kitchensink.model.Member;
import org.jboss.as.quickstarts.kitchensink.service.MemberRegistrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * RESTful service for member registration and retrieval.
 *
 * <p>This class provides endpoints to interact with the members data.
 */
@RestController
@RequestMapping("/rest/members")
public class MemberResourceRESTService {

  /** Logger for this class. */
  private static final Logger LOGGER = LoggerFactory.getLogger(MemberResourceRESTService.class);

  /** JSR-303 validator instance. */
  @Autowired private Validator validator;

  /** Repository for member data access. */
  @Autowired private MemberRepository repository;

  /** Service for member registration logic. */
  @Autowired private MemberRegistrationService registration;

  /**
   * Lists all registered members, ordered by name.
   *
   * @return A list of all members.
   */
  @GetMapping
  public List<Member> listAllMembers() {
    return repository.findAllByOrderByNameAsc();
  }

  /**
   * Looks up a member by their ID.
   *
   * @param id The ID of the member to look up.
   * @return The found member.
   * @throws ResponseStatusException if no member is found with the given ID.
   */
  @GetMapping("/{id:[0-9]+}")
  public Member lookupMemberById(@PathVariable("id") final long id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));
  }

  /**
   * Creates a new member.
   *
   * <p>Validates the member data and then registers the member. Returns different HTTP status codes
   * based on the outcome.
   *
   * @param member The member data from the request body.
   * @return ResponseEntity indicating success, validation failure, or other errors.
   */
  @PostMapping
  public ResponseEntity<?> createMember(@RequestBody final Member member) {
    try {
      validateMember(member);
      registration.register(member);
      return ResponseEntity.ok().build();
    } catch (ConstraintViolationException ce) {
      return ResponseEntity.badRequest()
          .body(buildValidationResponse(ce.getConstraintViolations()));
    } catch (ValidationException e) {
      var responseObj = new HashMap<String, String>();
      responseObj.put("email", "Email taken");
      return ResponseEntity.status(HttpStatus.CONFLICT)
          .body(Collections.unmodifiableMap(responseObj));
    } catch (Exception e) {
      LOGGER.error("Unexpected error during member creation: ", e);
      var responseObj = new HashMap<String, String>();
      responseObj.put("error", "An unexpected error occurred: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Collections.unmodifiableMap(responseObj));
    }
  }

  /**
   * Validates the given member.
   *
   * @param member The member to validate.
   * @throws ConstraintViolationException if validation rules are violated.
   * @throws ValidationException if the email already exists.
   */
  private void validateMember(final Member member)
      throws ConstraintViolationException, ValidationException {
    final Set<ConstraintViolation<Member>> violations = validator.validate(member);
    if (!violations.isEmpty()) {
      throw new ConstraintViolationException(violations);
    }
    if (emailAlreadyExists(member.getEmail())) {
      throw new ValidationException("Unique Email Violation");
    }
  }

  /**
   * Builds a response map from a set of constraint violations.
   *
   * @param violations The set of constraint violations.
   * @return A map where keys are property paths and values are violation messages.
   */
  private Map<String, String> buildValidationResponse(
      final Set<ConstraintViolation<?>> violations) {
    LOGGER.trace("Validation completed. violations found: " + violations.size());
    var errorMap =
        violations.stream()
            .collect(
                Collectors.toMap(
                    viol -> viol.getPropertyPath().toString(),
                    ConstraintViolation::getMessage,
                    (existingMessage, newMessage) -> existingMessage + "; " + newMessage));
    return Collections.unmodifiableMap(errorMap);
  }

  /**
   * Checks if an email address already exists in the repository.
   *
   * @param email The email address to check.
   * @return True if the email exists, false otherwise.
   */
  public boolean emailAlreadyExists(final String email) {
    return repository.findByEmail(email).isPresent();
  }
}
