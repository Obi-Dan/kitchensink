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
package org.jboss.as.quickstarts.kitchensink.rest;

import io.quarkus.panache.common.Sort;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.bson.types.ObjectId;
import org.jboss.as.quickstarts.kitchensink.model.Member;
import org.jboss.as.quickstarts.kitchensink.service.MemberRegistration;
import org.jboss.logging.Logger;

@Path("/members") // Base path for API still /rest/members due to application.properties
// UI paths will be sub-paths here or a different @Path class for UI
@ApplicationScoped // Changed from @RequestScoped to allow @Inject on Template if it needs broader
// scope
public class MemberResourceRESTService {

    private static final Logger log = Logger.getLogger(MemberResourceRESTService.class);

    @Inject Validator validator;

    @Inject MemberRegistration registrationService; // Inject the registration service

    @Inject Template member; // Injects the member.html template

    @Context // Inject UriInfo
    UriInfo uriInfo;

    // API Endpoints (existing)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Member> listAllMembers() {
        return Member.listAll(Sort.ascending("name"));
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Member lookupMemberById(@PathParam("id") String id) {
        ObjectId objectId;
        try {
            objectId = new ObjectId(id);
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException("Invalid ID format", Response.Status.BAD_REQUEST);
        }
        Member memberEntity = Member.findById(objectId);
        if (memberEntity == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        return memberEntity;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createMember(Member newMember) { // API endpoint for JSON
        Response.ResponseBuilder builder;
        try {
            newMember.id = null;
            validateMember(newMember, null);
            registrationService.register(newMember);
            builder = Response.status(Response.Status.CREATED).entity(newMember);
        } catch (ConstraintViolationException ce) {
            builder = createViolationResponse(ce.getConstraintViolations());
        } catch (ValidationException e) {
            Map<String, String> responseObj = new HashMap<>();
            if (e.getMessage() != null && e.getMessage().startsWith("Email already exists")) {
                responseObj.put("email", "Email taken");
            } else {
                // Generic validation error if not the specific duplicate email case
                responseObj.put(
                        "validationError",
                        e.getMessage() != null ? e.getMessage() : "Unknown validation error");
            }
            builder = Response.status(Response.Status.CONFLICT).entity(responseObj);
        } catch (Exception e) {
            log.error("Error creating member via API: " + e.getMessage(), e);
            Map<String, String> responseObj = new HashMap<>();
            responseObj.put("error", "An unexpected error occurred via API.");
            builder = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(responseObj);
        }
        return builder.build();
    }

    @DELETE
    @Path("/{id}")
    public Response deleteMember(@PathParam("id") String id) {
        ObjectId objectId;
        try {
            objectId = new ObjectId(id);
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException("Invalid ID format for delete operation", Response.Status.BAD_REQUEST);
        }

        boolean deleted = Member.deleteById(objectId);
        if (deleted) {
            return Response.noContent().build(); // 204 No Content
        } else {
            // If deleteById returns false, it means the entity was not found for that id
            throw new WebApplicationException("Member with id " + id + " not found for delete.", Response.Status.NOT_FOUND);
        }
    }

    // UI Endpoints
    @GET
    @Path("/ui")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getMemberUIPage(
            @QueryParam("successMessage") String successMessage,
            @QueryParam("newMemberName") String newMemberName, // For repopulating form on error
            @QueryParam("newMemberEmail") String newMemberEmail,
            @QueryParam("newMemberPhoneNumber") String newMemberPhoneNumber,
            @QueryParam("validationErrors")
                    List<String> validationErrors // Simplified error display
            ) {
        List<Member> members = Member.listAll(Sort.ascending("name"));
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("members", members);

        Member repopulateMember = new Member();
        if (newMemberName != null) repopulateMember.setName(newMemberName);
        if (newMemberEmail != null) repopulateMember.setEmail(newMemberEmail);
        if (newMemberPhoneNumber != null) repopulateMember.setPhoneNumber(newMemberPhoneNumber);
        templateData.put("newMember", repopulateMember);

        if (successMessage != null && !successMessage.isBlank()) {
            templateData.put("flash_successMessage", successMessage);
        }
        if (validationErrors != null && !validationErrors.isEmpty()) {
            // This is a simplified error display. Qute template expects map for validationMessages.
            // For more detailed field-specific errors, this would need more structure.
            templateData.put("flash_errorMessages", validationErrors);
        }
        return member.data(templateData);
    }

    @POST
    @Path("/ui/register")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response registerMemberFromUI(
            @FormParam("name") String name,
            @FormParam("email") String email,
            @FormParam("phoneNumber") String phoneNumber) {

        log.infof("[UI Register] Attempting to register member. Name: '%s', Email: '%s', Phone: '%s'", name, email, phoneNumber);

        Member newMember = new Member();
        newMember.setName(name);
        newMember.setEmail(email);
        newMember.setPhoneNumber(phoneNumber);

        UriBuilder redirectUriBuilder = uriInfo.getBaseUriBuilder().path("rest").path("members").path("ui");
        log.debugf("[UI Register] Initial redirectUriBuilder path: %s", redirectUriBuilder.build().getPath());

        try {
            log.debug("[UI Register] Attempting to validate member...");
            validateMember(newMember, null); // Pass null for currentMemberId
            log.info("[UI Register] Member validation successful.");

            log.debug("[UI Register] Attempting to call registrationService.register()...");
            registrationService.register(newMember);
            log.info("[UI Register] registrationService.register() successful. Member ID: " + newMember.id);

            // TEMPORARY DEBUG: Redirect to a simple known path
            URI redirectUri = uriInfo.getBaseUriBuilder().path("q/health/live").build();
            log.infof("[UI Register] TEMPORARY DEBUG - Redirecting to: %s", redirectUri.toString());

        } catch (ConstraintViolationException ce) {
            log.warn("[UI Register] ConstraintViolationException caught: " + ce.getMessage());
            if (ce.getConstraintViolations() != null && !ce.getConstraintViolations().isEmpty()) {
                List<String> errors =
                        ce.getConstraintViolations().stream()
                                .map(cv -> {
                                    String errorMsg = cv.getPropertyPath() + ": " + cv.getMessage();
                                    log.debug("[UI Register] Validation error: " + errorMsg);
                                    return errorMsg;
                                })
                                .collect(Collectors.toList());
                redirectUriBuilder.queryParam("validationErrors", errors.toArray(new String[0]));
            } else {
                log.warn("[UI Register] ConstraintViolationException had no violations, adding generic error.");
                redirectUriBuilder.queryParam("validationErrors", "Validation failed with no specific messages.");
            }
            redirectUriBuilder.queryParam("newMemberName", name);
            redirectUriBuilder.queryParam("newMemberEmail", email);
            redirectUriBuilder.queryParam("newMemberPhoneNumber", phoneNumber);
            log.debug("[UI Register] Added validationErrors and member data to redirect.");

        } catch (ValidationException ve) { // Custom validation like duplicate email
            log.warn("[UI Register] ValidationException caught: " + ve.getMessage());
            redirectUriBuilder.queryParam("validationErrors", ve.getMessage());
            redirectUriBuilder.queryParam("newMemberName", name);
            redirectUriBuilder.queryParam("newMemberEmail", email);
            redirectUriBuilder.queryParam("newMemberPhoneNumber", phoneNumber);
            log.debug("[UI Register] Added ValidationException message and member data to redirect.");

        } catch (Exception e) {
            log.error("[UI Register] Unexpected Exception caught: " + e.getMessage(), e);
            redirectUriBuilder.queryParam("validationErrors", "An unexpected error occurred during registration.");
            // Also pass back form fields if possible, in case it's a recoverable issue client-side or for user reference
            redirectUriBuilder.queryParam("newMemberName", name);
            redirectUriBuilder.queryParam("newMemberEmail", email);
            redirectUriBuilder.queryParam("newMemberPhoneNumber", phoneNumber);
            log.debug("[UI Register] Added generic error message and member data to redirect due to unexpected exception.");
        }

        URI redirectUri = redirectUriBuilder.build();
        log.infof("[UI Register] Final redirect URI: %s", redirectUri.toString());
        return Response.seeOther(redirectUri).build();
    }

    // Shared validation logic
    private void validateMember(Member memberToValidate, ObjectId currentMemberId)
            throws ConstraintViolationException, ValidationException {
        Set<ConstraintViolation<Member>> violations = validator.validate(memberToValidate);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(new HashSet<>(violations));
        }
        if (memberToValidate.getEmail() != null
                && emailAlreadyExists(memberToValidate.getEmail(), currentMemberId)) {
            throw new ValidationException(
                    "Email already exists for user: " + memberToValidate.getEmail());
        }
    }

    private Response.ResponseBuilder createViolationResponse(
            Set<ConstraintViolation<?>> violations) {
        log.debug("Validation completed. Violations found: " + violations.size());
        Map<String, String> responseObj = new HashMap<>();
        for (ConstraintViolation<?> violation : violations) {
            responseObj.put(violation.getPropertyPath().toString(), violation.getMessage());
        }
        return Response.status(Response.Status.BAD_REQUEST).entity(responseObj);
    }

    private boolean emailAlreadyExists(String email, ObjectId currentMemberId) {
        // Find by email - Panache MongoEntity provides find method
        Member existingMember = Member.find("email", email).firstResult();
        if (existingMember == null) {
            return false;
        }
        // If we are checking for a new member (currentMemberId is null), then any existing is a
        // duplicate.
        if (currentMemberId == null) {
            return true;
        }
        // If updating an existing member, it's a duplicate if the found email belongs to a
        // different member.
        return !existingMember.id.equals(currentMemberId);
    }
}
