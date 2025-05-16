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

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.jboss.as.quickstarts.kitchensink.model.Member;
import org.jboss.logging.Logger;

@ApplicationScoped
public class DataSeeder {

    private static final Logger LOG = Logger.getLogger(DataSeeder.class);

    // Injecting the collection directly to create an index. PanacheEntityBase doesn't expose this
    // directly.
    // This approach is robust for index creation.
    // Alternatively, for PanacheReactiveMongoEntity, you can use
    // .mongoDatabase().getCollection(...)
    // For PanacheMongoEntity, we can get the collection via Member.mongoCollection()

    public void onStart(@Observes StartupEvent ev) {
        LOG.info("DataSeeder: Checking and seeding initial data if necessary.");

        // Create Unique Index on Email if it doesn't exist
        try {
            MongoCollection<Member> memberCollection = Member.mongoCollection();
            // Check if index already exists (optional, createIndex is idempotent if options are
            // same)
            // For simplicity, we just try to create it.
            memberCollection.createIndex(
                    Indexes.ascending("email"), new IndexOptions().unique(true));
            LOG.info(
                    "Successfully ensured unique index exists on 'email' field for 'members' collection.");
        } catch (Exception e) {
            // Log error, but don't prevent startup. MongoDB might already have it, or other issues.
            LOG.error("Error creating unique index on email: " + e.getMessage());
        }

        // Seed initial member data
        if (Member.count() == 0) {
            LOG.info("No members found. Seeding initial data.");
            Member john = new Member();
            john.name = "John Smith";
            john.email = "john.smith@mailinator.com";
            john.phoneNumber = "2125551212";
            john.persist();
            LOG.info("Default member 'John Smith' seeded.");
        } else {
            LOG.info("Database already contains members. No seeding performed.");
        }
    }
}
