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
package org.jboss.as.quickstarts.kitchensink.model;

import io.quarkus.mongodb.panache.PanacheMongoRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

@ApplicationScoped
public class MemberRepository implements PanacheMongoRepositoryBase<Member, Long> {

    public Optional<Member> findByEmail(String email) {
        return Optional.ofNullable(find("email", email).firstResult());
    }

    // Explicitly implement findByIdOptional to ensure correct querying for Long _id
    public Optional<Member> findByIdOptional(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        // Query against the actual MongoDB document field name "_id"
        return Optional.ofNullable(find("_id", id).firstResult());
    }

    // PanacheMongoRepositoryBase provides common methods like:
    // findByIdOptional(ID id)
    // listAll(Sort sort)
    // persist(Entity entity)
    // count()
    // etc.
}
