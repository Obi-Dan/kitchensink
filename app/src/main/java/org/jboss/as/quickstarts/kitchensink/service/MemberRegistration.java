/*
 * JBoss, Home of Professional Open Source
 * Copyright 2015, Red Hat, Inc. and/or its affiliates, and individual
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

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.jboss.as.quickstarts.kitchensink.model.Member;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Commented out original EE imports:
// import jakarta.ejb.Stateless; // Replaced by @Service and @Transactional
// import jakarta.enterprise.event.Event; // Replaced by Spring ApplicationEventPublisher
// import jakarta.inject.Inject; // Replaced by @Autowired

/**
 * Represents an event published when a new member is registered. This class is a placeholder and
 * could be made more specific.
 */
class MemberRegisteredEvent extends org.springframework.context.ApplicationEvent {
  /** The member that was registered. */
  private final Member member;

  /**
   * Constructs a new MemberRegisteredEvent.
   *
   * @param source The component that published the event (i.e., {@link MemberRegistration}).
   * @param registeredMember The member that was registered.
   */
  MemberRegisteredEvent(final Object source, final Member registeredMember) {
    super(source);
    this.member = registeredMember;
  }

  /**
   * Gets the registered member.
   *
   * @return The registered member.
   */
  public Member getMember() {
    return member;
  }
}

/**
 * Service class for registering new members.
 *
 * <p>This service handles the business logic for member registration, including persisting the
 * member and publishing an event.
 */
@Service
public class MemberRegistration implements MemberRegistrationService {

  private static final Logger LOGGER = LoggerFactory.getLogger(MemberRegistration.class);

  /** The JPA EntityManager for database interactions. */
  @PersistenceContext private EntityManager em;

  /** The Spring ApplicationEventPublisher for publishing events. */
  @Autowired private ApplicationEventPublisher applicationEventPublisher;

  /**
   * Registers a new member.
   *
   * <p>This method persists the member to the database and publishes a {@link
   * MemberRegisteredEvent}.
   *
   * @param member The member to register.
   */
  @Transactional
  @Override
  public void register(final Member member) {
    LOGGER.info("Registering " + member.getName());
    em.persist(member);
    applicationEventPublisher.publishEvent(new MemberRegisteredEvent(this, member));
  }
}
