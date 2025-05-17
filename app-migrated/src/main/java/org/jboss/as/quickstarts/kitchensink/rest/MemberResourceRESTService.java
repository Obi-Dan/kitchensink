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
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.logging.Logger;

/**
 * JAX-RS Example
 *
 * <p>This class produces a RESTful service to read/write the contents of the members table.
 */
@ApplicationScoped
public class MemberResourceRESTService {

    private static final Logger LOG = Logger.getLogger(MemberResourceRESTService.class);

    @POST
    @Path("/rest/members/ping")
    @Produces(MediaType.TEXT_PLAIN)
    public String pingPost() {
        LOG.info("API: Ping POST received!");
        return "pong_post";
    }

    @POST
    @Path("/rest/members/simplest")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String createMemberApiMinimal(String name) {
        LOG.info("API: createMemberApiMinimal received name: " + name);
        return "Created minimal: " + name;
    }
}
