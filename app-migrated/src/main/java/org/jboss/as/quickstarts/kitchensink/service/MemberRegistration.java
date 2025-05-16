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

    @Inject Event<Member> memberEventSrc;

    // Custom exception for duplicate email
    public static class EmailAlreadyExistsException extends Exception {
        public EmailAlreadyExistsException(String message) {
            super(message);
        }
    }

    // @Transactional // Temporarily removed to test if this is causing connection resets
    public void register(Member member) throws EmailAlreadyExistsException {
        LOG.info(
                "REG_SVC: Attempting to register member: "
                        + (member != null ? member.email : "null member"));
        if (member == null) {
            LOG.error("REG_SVC: Member object is null");
            // Consider throwing a more specific application exception or a WebApplicationException
            // if this service is only called from REST
            throw new IllegalArgumentException("Member cannot be null");
        }

        LOG.info("REG_SVC: Checking email uniqueness for: " + member.email);
        Member existingMember = null;
        try {
            existingMember = Member.find("email", member.email).firstResult();
        } catch (Exception e) {
            LOG.error("REG_SVC: Error during Member.find for email: " + member.email, e);
            // Wrap and rethrow to allow REST layer to handle it as a 500 error or specific DB error
            throw new RuntimeException("Database error during email check", e);
        }

        if (existingMember != null) {
            LOG.warn("REG_SVC: Email already exists: " + member.email);
            throw new EmailAlreadyExistsException("Email already exists: " + member.email);
        }

        LOG.info("REG_SVC: Persisting member: " + member.email);
        try {
            member.persist();
        } catch (Exception e) {
            LOG.error("REG_SVC: Error during member.persist for email: " + member.email, e);
            // Wrap and rethrow
            throw new RuntimeException("Database error during persist", e);
        }
        LOG.info("REG_SVC: Member persisted: " + member.email + " with ID: " + member.id);

        LOG.info("REG_SVC: Firing member registration event for: " + member.email);
        try {
            memberEventSrc.fire(member);
            LOG.info("REG_SVC: Fired member registration event for: " + member.email);
        } catch (Exception e) {
            LOG.error("REG_SVC: Error firing CDI event for: " + member.email, e);
            // Decide if this should be a fatal error or just logged
        }
    }
}
