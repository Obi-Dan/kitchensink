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

import jakarta.enterprise.context.ApplicationScoped;
// import jakarta.inject.Inject; // Commenting out unused for now
import jakarta.ws.rs.Consumes;
// import jakarta.ws.rs.GET; // Commenting out unused for now
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
// import jakarta.ws.rs.core.Response; // Commenting out unused for now
import org.jboss.logging.Logger;

/**
 * JAX-RS Example
 *
 * <p>This class produces a RESTful service to read/write the contents of the members table.
 */
@ApplicationScoped
// No class-level @Path
public class MemberResourceRESTService {

    private static final Logger LOG = Logger.getLogger(MemberResourceRESTService.class);

    // @Inject Validator validator;
    // @Inject MemberRegistration registrationService;
    // @Inject MemberRepository memberRepository;
    // @Inject @Location("Member/index.html") Template index;

    @POST
    @Path("/members/ping") // Path relative to /rest
    @Produces(MediaType.TEXT_PLAIN)
    public String pingPost() {
        LOG.info("API: Ping POST received at /rest/members/ping!");
        return "pong_post";
    }

    @POST
    @Path("/members/simplest") // Path relative to /rest
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String createMemberApiMinimal(String name) {
        LOG.info(
                "API: createMemberApiMinimal received name: "
                        + name
                        + " at /rest/members/simplest");
        return "Created minimal: " + name;
    }

    /* Temporarily comment out all other methods
        @GET
        @Path("/members") // Path relative to /rest
        @Produces(MediaType.APPLICATION_JSON)
        public Response getAllMembersApi() {
            LOG.info("API: Listing all members (ordered by name)");
            return Response.ok("getAllMembersApi placeholder").build();
        }

        @GET
        @Path("/members/{id}") // Path relative to /rest
        @Produces(MediaType.APPLICATION_JSON)
        public Response lookupMemberByIdApi(@PathParam("id") Long id) {
            LOG.info("API --- Attempting to lookup member by id (Long): " + id);
            return Response.ok("lookupMemberByIdApi placeholder id: "+id).build();
        }

        @POST
        @Path("/members") // Path relative to /rest
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response createMemberApi(Member member) {
            LOG.info(
                    "API: Received createMemberApi request for email: "
                            + (member != null ? member.email : "null member object"));
            // ... simplified logic or placeholder ...
            return Response.status(Response.Status.CREATED).entity("createMemberApi placeholder").build();
        }

        // UI Methods
        @GET
        @Path("/kitchensink") // This path would now be /rest/kitchensink which is wrong for target UI path
        @Produces(MediaType.TEXT_HTML)
        public TemplateInstance getWebUi() {
            LOG.info("Serving UI page - placeholder");
            return index.data("members", Collections.emptyList())
                    .data("newMember", new Member())
                    .data("errors", Collections.emptyMap())
                    .data("globalMessages", Collections.emptyList());
        }

        @POST
        @Path("/kitchensink/register")
        @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
        @Produces(MediaType.TEXT_HTML)
        public TemplateInstance registerViaUi(
                @FormParam("name") String name,
                @FormParam("email") String email,
                @FormParam("phoneNumber") String phoneNumber) {
            LOG.info("UI: Registration attempt for email: " + email + " - placeholder");
            return index.data("members", Collections.emptyList())
                    .data("newMember", new Member())
                    .data("errors", Collections.emptyMap())
                    .data("globalMessages", Collections.singletonList(Map.of("type", "valid", "text", "Registered Placeholder!")));
        }

        @GET
        @Path("/kitchensink/members/{id}")
        @Produces(MediaType.TEXT_HTML)
        public TemplateInstance getMemberByIdUi(@PathParam("id") Long id) {
            LOG.info("UI: Looking up member by id: " + id + " - placeholder");
            return index.data("members", Collections.emptyList())
                    .data("newMember", new Member())
                    .data("errors", Collections.emptyMap())
                    .data("globalMessages", Collections.emptyList());
        }
    */
}
