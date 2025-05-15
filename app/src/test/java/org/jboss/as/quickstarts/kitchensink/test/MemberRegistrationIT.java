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
package org.jboss.as.quickstarts.kitchensink.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.logging.Logger;
import org.jboss.as.quickstarts.kitchensink.data.MemberRepository;
import org.jboss.as.quickstarts.kitchensink.model.Member;
import org.jboss.as.quickstarts.kitchensink.service.MemberRegistration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
public class MemberRegistrationIT {

  @Autowired private MemberRegistration memberRegistration;

  @Autowired private MemberRepository memberRepository;

  private static final Logger log = Logger.getLogger(MemberRegistrationIT.class.getName());

  @Test
  public void testRegister() {
    Member newMember = new Member();
    newMember.setName("Jane Doe Integration");
    newMember.setEmail("jane.integration@mailinator.com");
    newMember.setPhoneNumber("3135551234");

    memberRegistration.register(newMember);

    assertNotNull(newMember.getId(), "Member ID should not be null after registration");
    log.info(newMember.getName() + " was persisted with id " + newMember.getId());

    Optional<Member> fetchedMemberOpt = memberRepository.findById(newMember.getId());
    assertTrue(
        fetchedMemberOpt.isPresent(), "Member should be findable in repository after registration");

    Member fetchedMember = fetchedMemberOpt.get();
    assertEquals("Jane Doe Integration", fetchedMember.getName());
    assertEquals("jane.integration@mailinator.com", fetchedMember.getEmail());
  }
}
