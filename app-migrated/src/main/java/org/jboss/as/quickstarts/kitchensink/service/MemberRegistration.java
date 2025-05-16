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
package org.jboss.as.quickstarts.kitchensink.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import org.jboss.as.quickstarts.kitchensink.model.Member;
import org.jboss.logging.Logger;

@ApplicationScoped
public class MemberRegistration {

    private static final Logger LOG = Logger.getLogger(MemberRegistration.class);

    @Inject
    Event<Member> memberEventSrc;

    // Custom exception for duplicate email
    public static class EmailAlreadyExistsException extends Exception {
        public EmailAlreadyExistsException(String message) {
            super(message);
        }
    }

    // @Transactional // Temporarily removed to test if this is causing connection resets
    public void register(Member member) throws EmailAlreadyExistsException {
        LOG.info("Registering member: " + member.email);

        // Check for email uniqueness using Panache query
        if (Member.find("email", member.email).firstResult() != null) {
            LOG.warn("Email already exists: " + member.email);
            throw new EmailAlreadyExistsException("Email already exists: " + member.email);
        }

        // Persist the member using Panache Active Record pattern
        member.persist();
        LOG.info("Member persisted: " + member.email + " with ID: " + member.id);

        // Fire CDI event
        memberEventSrc.fire(member);
        LOG.info("Fired member registration event for: " + member.email);
    }
}
