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
package org.jboss.as.quickstarts.kitchensink.data;

// import jakarta.enterprise.context.ApplicationScoped; // Replaced by Spring Data JPA management
// import jakarta.inject.Inject;
// import jakarta.persistence.EntityManager;
// import jakarta.persistence.criteria.CriteriaBuilder;
// import jakarta.persistence.criteria.CriteriaQuery;
// import jakarta.persistence.criteria.Root;
import java.util.List;
import java.util.Optional;
import org.jboss.as.quickstarts.kitchensink.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// @ApplicationScoped // No longer needed, Spring Data JPA handles instantiation
@Repository // Optional, but good for clarity and component scanning if customized
public interface MemberRepository extends JpaRepository<Member, Long> {

  // findById(Long id) is provided by JpaRepository

  /**
   * Finds a member by their email address. Spring Data JPA will implement this based on the method
   * name. The original implementation threw NoResultException; {@link Optional} is used here for
   * cleaner handling of cases where the email is not found.
   *
   * @param email The email address to search for.
   * @return An {@link Optional} containing the found member, or an empty Optional if no member is
   *     found with the given email.
   */
  Optional<Member> findByEmail(String email);

  /**
   * Finds all members and orders them by name in ascending order. Spring Data JPA will implement
   * this based on the method name.
   *
   * @return A list of all members, ordered by name.
   */
  List<Member> findAllByOrderByNameAsc();

  /*
  // Original findByEmail implementation for reference:
  public Member findByEmail(String email) {
      CriteriaBuilder cb = em.getCriteriaBuilder();
      CriteriaQuery<Member> criteria = cb.createQuery(Member.class);
      Root<Member> member = criteria.from(Member.class);
      // Swap criteria statements if you would like to try out type-safe criteria queries, a new
      // feature in JPA 2.0
      // criteria.select(member).where(cb.equal(member.get(Member_.email), email));
      criteria.select(member).where(cb.equal(member.get("email"), email));
      return em.createQuery(criteria).getSingleResult();
  }

  // Original findAllOrderedByName implementation for reference:
  public List<Member> findAllOrderedByName() {
      CriteriaBuilder cb = em.getCriteriaBuilder();
      CriteriaQuery<Member> criteria = cb.createQuery(Member.class);
      Root<Member> member = criteria.from(Member.class);
      // Swap criteria statements if you would like to try out type-safe criteria queries, a new
      // feature in JPA 2.0
      // criteria.select(member).orderBy(cb.asc(member.get(Member_.name)));
      criteria.select(member).orderBy(cb.asc(member.get("name")));
      return em.createQuery(criteria).getResultList();
  }
  */
}
