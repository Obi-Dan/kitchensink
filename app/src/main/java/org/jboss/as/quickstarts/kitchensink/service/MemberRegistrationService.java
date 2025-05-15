package org.jboss.as.quickstarts.kitchensink.service;

import org.jboss.as.quickstarts.kitchensink.model.Member;

/** Interface for member registration operations. */
public interface MemberRegistrationService {

  /**
   * Registers a new member.
   *
   * @param member The member to register.
   */
  void register(Member member);
}
