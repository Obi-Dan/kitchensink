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
import jakarta.ws.rs.core.Response; // Not strictly needed for UI methods returning TemplateInstance
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jboss.as.quickstarts.kitchensink.model.Member;
import org.jboss.as.quickstarts.kitchensink.model.MemberRepository;
import org.jboss.as.quickstarts.kitchensink.service.MemberRegistration;
import org.jboss.logging.Logger;

@Path("/kitchensink")
@ApplicationScoped
public class KitchensinkUIResource {

    private static final Logger LOG = Logger.getLogger(KitchensinkUIResource.class);

    @Inject Validator validator;

    @Inject MemberRegistration registrationService;

    @Inject MemberRepository memberRepository;

    @Inject
    @Location("Member/index.html")
    Template index;

    @GET
    @Path("") // Serves the main UI page at /kitchensink
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getWebUi() {
        LOG.info("UI: Serving main page at /kitchensink");
        List<Member> members = memberRepository.listAll(Sort.by("name"));
        Member newMember = new Member(); // For the form
        return index.data("members", members)
                .data("newMember", newMember)
                .data("errors", Collections.emptyMap())
                .data("globalMessages", Collections.emptyList());
    }

    @POST
    @Path("/register") // Handles form submission from /kitchensink to /kitchensink/register
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance registerViaUi(
            @FormParam("name") String name,
            @FormParam("email") String email,
            @FormParam("phoneNumber") String phoneNumber) {
        LOG.info("UI: Registration attempt for email: " + email + " via /kitchensink/register");
        Member newMember = new Member();
        newMember.name = name;
        newMember.email = email;
        newMember.phoneNumber = phoneNumber;

        Map<String, String> errors = new HashMap<>();
        List<Map<String, String>> globalMessages = new ArrayList<>();

        try {
            Set<ConstraintViolation<Member>> violations = validator.validate(newMember);
            if (!violations.isEmpty()) {
                throw new ConstraintViolationException(violations);
            }
            registrationService.register(newMember);
            globalMessages.add(Map.of("type", "valid", "text", "Registered!"));
            newMember = new Member(); // Clear form

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
    @Path("/members/{id}") // Serves a specific member view, e.g., /kitchensink/members/123
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getMemberByIdUi(@PathParam("id") Long id) {
        LOG.info(
                "UI: Looking up member by id: " + id + " for UI view at /kitchensink/members/{id}");
        Member member =
                memberRepository
                        .findByIdOptional(id)
                        .orElseThrow(
                                () ->
                                        new WebApplicationException(
                                                "Member with id of " + id + " does not exist.",
                                                Response.Status.NOT_FOUND));
        // Re-use index, but ideally would have a detail template
        List<Member> membersList = (member != null) ? List.of(member) : Collections.emptyList();
        return index.data("members", membersList)
                .data("newMember", new Member())
                .data("errors", Collections.emptyMap())
                .data("globalMessages", Collections.emptyList());
    }
}
