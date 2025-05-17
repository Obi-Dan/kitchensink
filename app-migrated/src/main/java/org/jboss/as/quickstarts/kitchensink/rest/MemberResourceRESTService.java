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
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collections;
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
@Path("/app")
@ApplicationScoped
public class MemberResourceRESTService {

    private static final Logger LOG = Logger.getLogger(MemberResourceRESTService.class);

    @Inject Validator validator;

    @Inject MemberRegistration registrationService;

    @Inject MemberRepository memberRepository;

    @Inject
    @Location("Member/index.html")
    Template index;

    @GET
    @Path("/api/members")
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
    @Path("/api/members/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response lookupMemberByIdApi(@PathParam("id") Long id) {
        LOG.info("API: Looking up member by id: " + id);
        Member member =
                memberRepository
                        .findByIdOptional(id)
                        .orElseThrow(
                                () ->
                                        new WebApplicationException(
                                                "Member with id of " + id + " does not exist.",
                                                Response.Status.NOT_FOUND));
        LOG.info("API: Found member: " + member.email);
        return Response.ok(member).build();
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
        Response.ResponseBuilder builder;
        try {
            LOG.info("API: Validating member bean for: " + member.email);
            validateMemberBean(member);
            LOG.info("API: Calling registration service for: " + member.email);
            registrationService.register(member);
            LOG.info("API: Registration service call completed for: " + member.email);
            builder = Response.status(Response.Status.CREATED).entity(member);
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

    @GET
    @Path("/ui")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getWebUi() {
        LOG.info("Serving UI page");
        List<Member> members = memberRepository.listAll(Sort.by("name"));
        Member newMember = new Member();
        return index.data("members", members)
                .data("newMember", newMember)
                .data("errors", Collections.emptyMap())
                .data("globalMessages", Collections.emptyList());
    }

    @POST
    @Path("/ui/register")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance registerViaUi(
            @FormParam("name") String name,
            @FormParam("email") String email,
            @FormParam("phoneNumber") String phoneNumber) {
        LOG.info("UI: Registration attempt for email: " + email);
        Member newMember = new Member();
        newMember.name = name;
        newMember.email = email;
        newMember.phoneNumber = phoneNumber;

        Map<String, String> errors = new HashMap<>();
        List<Map<String, String>> globalMessages = new ArrayList<>();

        try {
            validateMemberBean(newMember);
            registrationService.register(newMember);
            globalMessages.add(Map.of("type", "valid", "text", "Registered!"));
            newMember = new Member();

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

        List<Member> members = memberRepository.listAll(Sort.by("name"));
        return index.data("members", members)
                .data("newMember", newMember)
                .data("errors", errors)
                .data("globalMessages", globalMessages);
    }

    @GET
    @Path("/ui/members/{id}")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getMemberByIdUi(@PathParam("id") Long id) {
        LOG.info("UI: Looking up member by id: " + id);
        Member member =
                memberRepository
                        .findByIdOptional(id)
                        .orElseThrow(
                                () ->
                                        new WebApplicationException(
                                                "Member with id of " + id + " does not exist.",
                                                Response.Status.NOT_FOUND));
        List<Member> membersList = (member != null) ? List.of(member) : Collections.emptyList();
        return index.data("members", membersList)
                .data("newMember", new Member())
                .data("errors", Collections.emptyMap())
                .data("globalMessages", Collections.emptyList());
    }

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
