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

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import io.quarkus.arc.profile.UnlessBuildProfile;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.as.quickstarts.kitchensink.model.Member;
import org.jboss.as.quickstarts.kitchensink.model.MemberRepository;
import org.jboss.logging.Logger;

@UnlessBuildProfile("test")
@ApplicationScoped
public class DataSeeder {

    private static final Logger LOG = Logger.getLogger(DataSeeder.class);
    private static final String MEMBER_ID_SEQUENCE_NAME = "memberId";

    @Inject MongoClient mongoClient;

    @ConfigProperty(name = "quarkus.mongodb.database")
    String databaseName;

    @Inject MemberRepository memberRepository;

    @Inject SequenceGeneratorService sequenceGenerator;

    private MongoDatabase getDatabase() {
        return mongoClient.getDatabase(databaseName);
    }

    void onStart(@Observes StartupEvent ev) {
        LOG.info("DataSeeder: Checking and seeding initial data if necessary.");

        try {
            MongoCollection<Member> memberCollection =
                    getDatabase().getCollection("members", Member.class);
            memberCollection.createIndex(
                    Indexes.ascending("email"), new IndexOptions().unique(true));
            LOG.info(
                    "Successfully ensured unique index exists on 'email' field for 'members' collection.");
        } catch (Exception e) {
            LOG.error("Failed to create unique index on email for members collection", e);
        }

        sequenceGenerator.initializeSequence(MEMBER_ID_SEQUENCE_NAME, -1L);
        LOG.info(
                "Initialized sequence '"
                        + MEMBER_ID_SEQUENCE_NAME
                        + "' to -1 if it did not exist (for IDs starting at 0).");

        if (memberRepository.count() == 0) {
            LOG.info("No members found. Seeding initial data.");
            Member defaultMember = new Member();
            defaultMember.setId(sequenceGenerator.getNextSequence(MEMBER_ID_SEQUENCE_NAME));
            defaultMember.name = "John Smith";
            defaultMember.email = "john.smith@mailinator.com";
            defaultMember.phoneNumber = "2125551212";
            memberRepository.persist(defaultMember);
            LOG.info(
                    "Default member '"
                            + defaultMember.name
                            + "' seeded with ID: "
                            + defaultMember.getId());
        } else {
            LOG.info(
                    "DataSeeder: Members already exist, no seeding required. Count: "
                            + memberRepository.count());
        }
    }
}
