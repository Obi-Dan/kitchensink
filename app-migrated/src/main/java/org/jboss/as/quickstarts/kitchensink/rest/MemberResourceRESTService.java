/*
 * JBoss, Home of Professional Open Source
 * Copyright 2023, Red Hat, Inc. and/or its affiliates, and individual
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
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.jboss.as.quickstarts.kitchensink.model.Member;
import org.jboss.as.quickstarts.kitchensink.model.MemberRepository;
import org.jboss.as.quickstarts.kitchensink.service.MemberRegistration;
import org.jboss.logging.Logger;

/**
 * JAX-RS Example
 *
 * <p>This class produces a RESTful service to read/write the contents of the members table.
 */
@Path("/members") // Class-level path for API methods, relative to /rest global prefix
public class MemberResourceRESTService {

    private static final Logger LOG = Logger.getLogger(MemberResourceRESTService.class);

    @Inject Validator validator;
    @Inject MemberRegistration registrationService;
    @Inject MemberRepository memberRepository;

    // UI Injections & methods still commented out
    // @Inject @Location("Member/index.html") Template index;

    @GET
    @Path("/ping") // Effective path: /rest/members/ping
    @Produces(MediaType.TEXT_PLAIN)
    public String pingGet() { // Return String directly
        LOG.info("API: Ping GET received at /rest/members/ping! (simple return)");
        return "pong_get_direct";
    }

    @POST
    @Path("/simplest") // Effective path: /rest/members/simplest
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response createMemberApiMinimal(String name) {
        LOG.info(
                "API: createMemberApiMinimal received name: "
                        + name
                        + " at /rest/members/simplest");
        return Response.ok("Created minimal: " + name).type(MediaType.TEXT_PLAIN).build();
    }

    // Restore original API methods
    @GET
    @Path("") // Effective: /rest/members
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllMembersApi() {
        LOG.info("API: Listing all members (ordered by name)");
        List<Member> members = memberRepository.listAll(Sort.by("name"));
        if (members.isEmpty()) {
            LOG.info("API: No members found.");
            return Response.status(Response.Status.NO_CONTENT).entity("[]").build();
        }
        return Response.ok(members).build();
    }

    @GET
    @Path("/{id}") // Effective: /rest/members/{id}
    @Produces(MediaType.APPLICATION_JSON)
    public Response lookupMemberByIdApi(@PathParam("id") Long id) {
        LOG.info("API --- Attempting to lookup member by id (Long): " + id);
        Member member =
                memberRepository
                        .findByIdOptional(id)
                        .orElseThrow(
                                () -> {
                                    LOG.warn("API --- Member not found for id: " + id);
                                    return new WebApplicationException(
                                            "Member with id of " + id + " does not exist.",
                                            Response.Status.NOT_FOUND);
                                });
        LOG.info(
                "API --- Successfully found member: "
                        + member.email
                        + " with id: "
                        + member.getId());
        return Response.ok(member).build();
    }

    @POST
    @Path("") // Effective: /rest/members
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createMemberApi(Member member) {
        LOG.info(
                "API: Received createMemberApi request for email: "
                        + (member != null ? member.email : "null member object"));
        if (member == null) {
            LOG.error("API: Member object is null in createMemberApi");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Member data is required.")
                    .build();
        }
        if (member.getId() != null) {
            LOG.warn("API: Member payload for creation contains an ID: " + member.getId());
            Map<String, String> responseObj = new HashMap<>();
            responseObj.put(
                    "id",
                    "ID must not be set for new member registration. It will be auto-generated.");
            return Response.status(Response.Status.CONFLICT).entity(responseObj).build();
        }

        LOG.info(
                "API: Attempting to create member: " + member.email + " with name: " + member.name);
        try {
            LOG.info("API: Validating member bean for: " + member.email);
            validateMemberBean(member);
            LOG.info("API: Calling registration service for: " + member.email);
            registrationService.register(member);
            LOG.info(
                    "API: Member registered successfully: "
                            + member.email
                            + " with ID: "
                            + member.id);
            return Response.status(Response.Status.CREATED).entity(member).build();

        } catch (ConstraintViolationException ce) {
            LOG.warn("API: ConstraintViolationException for member: " + member.email, ce);
            return createViolationResponse(ce.getConstraintViolations());
        } catch (MemberRegistration.EmailAlreadyExistsException e) {
            LOG.warn("API: EmailAlreadyExistsException for: " + member.email, e);
            Map<String, String> responseObj = new HashMap<>();
            responseObj.put("email", "Email already exists");
            return Response.status(Response.Status.CONFLICT).entity(responseObj).build();
        } catch (Exception e) {
            LOG.error(
                    "API: Generic Exception creating member: "
                            + member.email
                            + " - "
                            + e.getMessage(),
                    e);
            Map<String, String> responseObj = new HashMap<>();
            responseObj.put("error", "An unexpected error occurred: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(responseObj)
                    .build();
        }
    }

    // UI Methods are still commented out
    /*
    ...
    */

    private void validateMemberBean(Member member) throws ConstraintViolationException {
        Set<ConstraintViolation<Member>> violations = validator.validate(member);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(new HashSet<>(violations));
        }
    }

    private Response createViolationResponse(Set<ConstraintViolation<?>> violations) {
        LOG.warnf(
                "Validation violations found: %s",
                violations.stream()
                        .map(v -> v.getPropertyPath().toString() + ": " + v.getMessage())
                        .collect(Collectors.joining(", ")));

        Map<String, String> responseObj = new HashMap<>();
        for (ConstraintViolation<?> violation : violations) {
            responseObj.put(violation.getPropertyPath().toString(), violation.getMessage());
        }
        return Response.status(Response.Status.BAD_REQUEST).entity(responseObj).build();
    }
}
