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

import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import org.jboss.as.quickstarts.kitchensink.model.Member;
import org.jboss.as.quickstarts.kitchensink.model.MemberRepository;
import org.jboss.logging.Logger;

@ApplicationScoped
public class MemberRegistration {

    private static final Logger LOG = Logger.getLogger(MemberRegistration.class);
    private static final String MEMBER_ID_SEQUENCE_NAME = "memberId";

    @Inject MemberRepository memberRepository;

    @Inject SequenceGeneratorService sequenceGenerator;

    @Inject Event<Member> memberEventSrc;

    // Custom exception for duplicate email
    public static class EmailAlreadyExistsException extends Exception {
        public EmailAlreadyExistsException(String message) {
            super(message);
        }
    }

    @Timed(
            value = "members.registration.service.time",
            description = "Time taken to register a member via service")
    @Counted(
            value = "members.registration.service.count",
            description = "Number of member registration attempts via service")
    public void register(Member member) throws EmailAlreadyExistsException {
        if (member == null) {
            LOG.error("REG_SVC: Attempt to register a null member.");
            throw new IllegalArgumentException("Member to register cannot be null.");
        }
        LOG.info("REG_SVC: Attempting to register member: " + member.email);

        LOG.info("REG_SVC: Checking email uniqueness for: " + member.email);
        if (memberRepository.findByEmail(member.email).isPresent()) { // USE REPOSITORY
            LOG.warn("REG_SVC: Email already exists: " + member.email);
            throw new EmailAlreadyExistsException("Email already exists: " + member.email);
        }

        Long newId = sequenceGenerator.getNextSequence(MEMBER_ID_SEQUENCE_NAME);
        member.setId(newId);
        LOG.info("REG_SVC: Assigned new ID " + newId + " to member: " + member.email);

        LOG.info("REG_SVC: Persisting member: " + member.email + " with ID: " + member.getId());
        memberRepository.persist(member); // USE REPOSITORY
        LOG.info("REG_SVC: Member persisted: " + member.email + " with ID: " + member.getId());

        LOG.info("REG_SVC: Firing member registration event for: " + member.email);
        memberEventSrc.fire(member);
        LOG.info("REG_SVC: Fired member registration event for: " + member.email);
    }

    // emailExists method was effectively inlined into register or uses repository directly
    // public boolean emailExists(String email) {
    //     return memberRepository.findByEmail(email).isPresent();
    // }
}
