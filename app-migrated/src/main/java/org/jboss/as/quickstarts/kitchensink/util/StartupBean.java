/*
 * JBoss, Home of Professional Open Source
 * Copyright 2024, Red Hat, Inc. and/or its affiliates, and individual
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
package org.jboss.as.quickstarts.kitchensink.util; // Placing in util as per original project

// structure for Resources.java

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.jboss.as.quickstarts.kitchensink.model.Member;

@ApplicationScoped
public class StartupBean {

    void onStart(@Observes StartupEvent ev) {
        // Get the collection for Member using Panache static method
        MongoCollection<Member> memberCollection = Member.mongoCollection();
        // Or for Document if specific Document operations are needed not covered by Panache typed
        // collection:
        // MongoCollection<Document> memberDocumentCollection =
        // Member.mongoCollection(Document.class);

        // Create a unique index on the "email" field if it doesn't exist
        memberCollection.createIndex(Indexes.ascending("email"), new IndexOptions().unique(true));
        System.out.println(
                "Ensured unique index on 'email' field for collection: "
                        + memberCollection.getNamespace().getCollectionName());

        // Data Seeding (from migration plan)
        if (Member.count() == 0) {
            Member john = new Member();
            john.setName("John Smith"); // Use setter
            john.setEmail("john.smith@mailinator.com"); // Use setter
            john.setPhoneNumber("2125551212"); // Use setter
            john.persist();
            System.out.println("Seeded initial member: John Smith");
        }
    }
}
