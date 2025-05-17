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

// For sorting
// import io.quarkus.qute.Location; // Qute still commented out
// import io.quarkus.qute.Template; // Qute still commented out
// import io.quarkus.qute.TemplateInstance; // Qute still commented out
import jakarta.enterprise.context.ApplicationScoped; // Re-add if needed, or keep as plain JAX-RS
import jakarta.ws.rs.Consumes;
// import jakarta.ws.rs.FormParam; // Qute still commented out
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
// import java.util.ArrayList; // Qute still commented out
// import java.util.Collections; // Qute still commented out
import org.jboss.logging.Logger;

/**
 * JAX-RS Example
 *
 * <p>This class produces a RESTful service to read/write the contents of the members table.
 */
@Path("/members") // Class-level path for API methods, relative to /rest global prefix
@ApplicationScoped
public class MemberResourceRESTService {

    private static final Logger LOG = Logger.getLogger(MemberResourceRESTService.class);

    // @Inject Validator validator; // Keep commented for simplified test
    // @Inject MemberRegistration registrationService; // Keep commented
    // @Inject MemberRepository memberRepository; // Keep commented

    @GET
    @Path("/ping") // Effective path: /rest/members/ping
    @Produces(MediaType.TEXT_PLAIN)
    public Response pingPost() { // Method name can remain pingPost for test consistency
        LOG.info("API: Ping GET received at /rest/members/ping!");
        return Response.ok("pong_get").type(MediaType.TEXT_PLAIN).build();
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

    // All other original API and UI methods remain commented out for this debug step
    /*
    @GET
    @Path("") // Effective: /rest/members
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllMembersApi() {
        // ...
    }

    @GET
    @Path("/{id}") // Effective: /rest/members/{id}
    @Produces(MediaType.APPLICATION_JSON)
    public Response lookupMemberByIdApi(@PathParam("id") Long id) {
        // ...
    }

    @POST
    @Path("") // Effective: /rest/members
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createMemberApi(Member member) {
      // ...
    }
    */
}
