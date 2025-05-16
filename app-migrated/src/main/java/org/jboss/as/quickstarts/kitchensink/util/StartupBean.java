package org.jboss.as.quickstarts.kitchensink.util; // Placing in util as per original project structure for Resources.java

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.bson.Document;
import org.jboss.as.quickstarts.kitchensink.model.Member;

@ApplicationScoped
public class StartupBean {

    void onStart(@Observes StartupEvent ev) {
        // Get the collection for Member using Panache static method
        MongoCollection<Member> memberCollection = Member.mongoCollection();
        // Or for Document if specific Document operations are needed not covered by Panache typed collection:
        // MongoCollection<Document> memberDocumentCollection = Member.mongoCollection(Document.class);

        // Create a unique index on the "email" field if it doesn't exist
        memberCollection.createIndex(Indexes.ascending("email"), new IndexOptions().unique(true));
        System.out.println("Ensured unique index on 'email' field for collection: " + memberCollection.getNamespace().getCollectionName());

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