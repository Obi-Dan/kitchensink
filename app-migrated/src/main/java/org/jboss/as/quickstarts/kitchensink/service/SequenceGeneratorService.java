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
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Updates;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.Document;

@ApplicationScoped
public class SequenceGeneratorService {

    @Inject MongoDatabase database;

    private static final String COUNTERS_COLLECTION_NAME = "counters";
    private static final String SEQUENCE_FIELD_NAME = "seq";

    public Long getNextSequence(String sequenceName) {
        MongoCollection<Document> countersCollection =
                database.getCollection(COUNTERS_COLLECTION_NAME);

        Document sequenceDocument =
                countersCollection.findOneAndUpdate(
                        Filters.eq("_id", sequenceName),
                        Updates.inc(SEQUENCE_FIELD_NAME, 1L),
                        new FindOneAndUpdateOptions()
                                .upsert(true)
                                .returnDocument(ReturnDocument.AFTER));
        // $inc operator will create the field and set it to the increment value if the field does
        // not exist on an existing document.
        // If upsert creates the document, $inc will create the field with the incremented value
        // (e.g., 1 if incrementing by 1).
        if (sequenceDocument != null && sequenceDocument.getLong(SEQUENCE_FIELD_NAME) != null) {
            return sequenceDocument.getLong(SEQUENCE_FIELD_NAME);
        } else {
            // This path should ideally not be taken if upsert and $inc work as expected.
            // It might indicate the document was inserted by upsert but the value wasn't returned
            // as expected,
            // or a more complex race condition on the very first creation.
            // Re-querying might be an option here, but for now, rely on DataSeeder to initialize.
            throw new RuntimeException(
                    "Unable to retrieve or generate sequence for: "
                            + sequenceName
                            + ". Sequence document or field was null after findOneAndUpdate.");
        }
    }

    public void initializeSequence(String sequenceName, long initialValue) {
        MongoCollection<Document> countersCollection =
                database.getCollection(COUNTERS_COLLECTION_NAME);
        Document counter = countersCollection.find(Filters.eq("_id", sequenceName)).first();
        if (counter == null) {
            countersCollection.insertOne(
                    new Document("_id", sequenceName).append(SEQUENCE_FIELD_NAME, initialValue));
        }
    }
}
