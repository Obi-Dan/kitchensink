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

import io.quarkus.panache.common.Sort; // Added import for Sort
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.bson.types.ObjectId; // For MongoDB ObjectId
import org.jboss.as.quickstarts.kitchensink.model.Member;
import org.jboss.logging.Logger; // Changed to JBoss Logging
// Removed: MemberRepository and MemberRegistration imports for now, will use Panache static methods or re-introduce later.

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
// import java.util.logging.Logger; // Removed java.util.logging.Logger

@Path("/members")
@RequestScoped
public class MemberResourceRESTService {

    // No @Inject needed for static Logger with JBoss Logging if used as follows:
    private static final Logger log = Logger.getLogger(MemberResourceRESTService.class);

    @Inject
    private Validator validator;

    // Placeholder for future MemberRegistration service if complex logic is needed beyond persist.
    // For now, direct persistence is used.

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Member> listAllMembers() {
        return Member.listAll(Sort.ascending("name")); // Corrected sort
    }

    @GET
    @Path("/{id}") // Changed from id:[0-9][0-9]* to support ObjectId string
    @Produces(MediaType.APPLICATION_JSON)
    public Member lookupMemberById(@PathParam("id") String id) {
        ObjectId objectId;
        try {
            objectId = new ObjectId(id);
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException("Invalid ID format", Response.Status.BAD_REQUEST);
        }
        Member member = Member.findById(objectId);
        if (member == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        return member;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createMember(Member member) {
        Response.ResponseBuilder builder;
        try {
            validateMember(member);
            // In a real app, member object from request might be a DTO.
            // Here we assume it's the entity directly for simplicity matching original.
            member.id = null; // Ensure Panache generates a new ID if one was accidentally passed
            member.persist(); // Using Panache Active Record
            // Optionally, fire an event here if other parts of the app need to react
            // @Inject Event<Member> memberEventSrc; memberEventSrc.fire(member);
            builder = Response.status(Response.Status.CREATED).entity(member); // Return created member with ID
        } catch (ConstraintViolationException ce) {
            builder = createViolationResponse(ce.getConstraintViolations());
        } catch (ValidationException e) {
            Map<String, String> responseObj = new HashMap<>();
            responseObj.put("email", "Email taken or other validation error: " + e.getMessage());
            builder = Response.status(Response.Status.CONFLICT).entity(responseObj);
        } catch (Exception e) {
            log.error("Error creating member: " + e.getMessage(), e);
            Map<String, String> responseObj = new HashMap<>();
            responseObj.put("error", "An unexpected error occurred.");
            builder = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(responseObj);
        }
        return builder.build();
    }

    private void validateMember(Member member) throws ConstraintViolationException, ValidationException {
        Set<ConstraintViolation<Member>> violations = validator.validate(member);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(new HashSet<>(violations));
        }
        if (member.getEmail() != null && emailAlreadyExists(member.getEmail(), member.id)) {
            throw new ValidationException("Email already exists");
        }
    }

    private Response.ResponseBuilder createViolationResponse(Set<ConstraintViolation<?>> violations) {
        log.debug("Validation completed. Violations found: " + violations.size());
        Map<String, String> responseObj = new HashMap<>();
        for (ConstraintViolation<?> violation : violations) {
            responseObj.put(violation.getPropertyPath().toString(), violation.getMessage());
        }
        return Response.status(Response.Status.BAD_REQUEST).entity(responseObj);
    }

    // Check if email exists for a *different* member (if id is provided for an update scenario)
    private boolean emailAlreadyExists(String email, ObjectId currentMemberId) {
        Member existingMember = Member.find("email", email).firstResult();
        if (existingMember == null) {
            return false; // Email does not exist
        }
        if (currentMemberId == null) {
            return true; // Creating new member and email exists
        }
        // Updating existing member, check if found email belongs to a different member
        return !existingMember.id.equals(currentMemberId);
    }
}
