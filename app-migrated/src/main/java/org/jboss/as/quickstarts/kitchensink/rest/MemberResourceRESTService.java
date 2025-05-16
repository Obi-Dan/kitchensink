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

import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import java.util.Collections;
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

/**
 * JAX-RS Example
 *
 * <p>This class produces a RESTful service to read/write the contents of the members table.
 */
@Path("/app") // Changed base path to /app for UI and API separation if needed
@ApplicationScoped
public class MemberResourceRESTService {

    private static final Logger LOG = Logger.getLogger(MemberResourceRESTService.class);

    @Inject Validator validator;

    @Inject MemberRegistration registration;

    @Inject
    @Location("Member/index.html") // Specify the path to the template
    Template index; // Injects templates/Member/index.html based on resource method return type

    // REST API methods (prefixed with /api for clarity, original /members path can be kept if no
    // conflict)
    @GET
    @Path("/api/members")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Member> listAllMembersApi() {
        LOG.info("API: Listing all members (ordered by name)");
        return Member.list("ORDER BY name");
    }

    @GET
    @Path("/api/members/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Member lookupMemberByIdApi(@PathParam("id") String id) {
        LOG.info("API: Looking up member by id: " + id);
        ObjectId objectId;
        try {
            objectId = new ObjectId(id);
        } catch (IllegalArgumentException e) {
            LOG.warn("API: Invalid ObjectId format for id: " + id);
            throw new WebApplicationException(
                    "Invalid member ID format", Response.Status.BAD_REQUEST);
        }
        Member member = Member.findById(objectId);
        if (member == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        return member;
    }

    @POST
    @Path("/api/members")
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
        LOG.info(
                "API: Attempting to create member: " + member.email + " with name: " + member.name);
        Response.ResponseBuilder builder;
        try {
            LOG.info("API: Validating member bean for: " + member.email);
            validateMemberBean(member);
            LOG.info("API: Calling registration service for: " + member.email);
            registration.register(member);
            LOG.info("API: Registration service call completed for: " + member.email);
            builder = Response.ok(member);
        } catch (ConstraintViolationException ce) {
            LOG.warn("API: ConstraintViolationException for member: " + member.email, ce);
            builder = createViolationResponse(ce.getConstraintViolations());
        } catch (MemberRegistration.EmailAlreadyExistsException e) {
            LOG.warn("API: EmailAlreadyExistsException for: " + member.email, e);
            Map<String, String> responseObj = new HashMap<>();
            responseObj.put("email", "Email already exists");
            builder = Response.status(Response.Status.CONFLICT).entity(responseObj);
        } catch (Exception e) {
            LOG.error(
                    "API: Generic Exception creating member: "
                            + member.email
                            + " - "
                            + e.getMessage(),
                    e);
            Map<String, String> responseObj = new HashMap<>();
            responseObj.put("error", "An unexpected error occurred: " + e.getMessage());
            builder = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(responseObj);
        }
        LOG.info("API: Building response for createMemberApi for email: " + member.email);
        return builder.build();
    }

    // UI Serving Methods
    @GET
    @Path("/ui") // Path for the main UI page
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getUIPage() {
        LOG.info("Serving UI page");
        List<Member> members = Member.list("ORDER BY name");
        return index.data("members", members)
                .data("newMember", new Member()) // Empty member for the form
                .data("errors", Collections.emptyMap()) // No errors initially
                .data("globalMessages", Collections.emptyList());
    }

    @POST
    @Path("/ui/register")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance registerMemberFromUI(MultivaluedMap<String, String> formParams) {
        String name = formParams.getFirst("name");
        String email = formParams.getFirst("email");
        String phoneNumber = formParams.getFirst("phoneNumber");

        Member newMember = new Member(name, email, phoneNumber);
        Map<String, String> errors = new HashMap<>();
        List<Map<String, String>> globalMessages =
                new java.util.ArrayList<>(); // List of Maps for type/text

        try {
            validateMemberBean(newMember);
            registration.register(newMember);
            globalMessages.add(Map.of("type", "valid", "text", "Registered!"));
            List<Member> members = Member.list("ORDER BY name");
            return index.data("members", members)
                    .data("newMember", new Member())
                    .data("errors", Collections.emptyMap())
                    .data("globalMessages", globalMessages);

        } catch (ConstraintViolationException ce) {
            ce.getConstraintViolations()
                    .forEach(v -> errors.put(v.getPropertyPath().toString(), v.getMessage()));
            globalMessages.add(Map.of("type", "invalid", "text", "Validation errors occurred."));
        } catch (MemberRegistration.EmailAlreadyExistsException e) {
            errors.put("email", "Email already exists");
            globalMessages.add(
                    Map.of("type", "invalid", "text", "This email is already registered."));
        } catch (Exception e) {
            LOG.error("Error during UI registration: " + e.getMessage(), e);
            globalMessages.add(
                    Map.of(
                            "type",
                            "error",
                            "text",
                            "An unexpected error occurred during registration."));
        }

        List<Member> members = Member.list("ORDER BY name");
        return index.data("members", members)
                .data("newMember", newMember)
                .data("errors", errors)
                .data("globalMessages", globalMessages);
    }

    private void validateMemberBean(Member member) throws ConstraintViolationException {
        Set<ConstraintViolation<Member>> violations = validator.validate(member);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(new HashSet<>(violations));
        }
    }

    private Response.ResponseBuilder createViolationResponse(
            Set<ConstraintViolation<?>> violations) {
        LOG.warnf(
                "Validation violations found: %s",
                violations.stream()
                        .map(v -> v.getPropertyPath().toString() + ": " + v.getMessage())
                        .collect(Collectors.joining(", ")));

        Map<String, String> responseObj = new HashMap<>();
        for (ConstraintViolation<?> violation : violations) {
            responseObj.put(violation.getPropertyPath().toString(), violation.getMessage());
        }
        return Response.status(Response.Status.BAD_REQUEST).entity(responseObj);
    }
}
