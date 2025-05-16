package org.jboss.as.quickstarts.kitchensink.model;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
// import org.bson.types.ObjectId; // ObjectId is part of PanacheMongoEntity's id field

import java.util.List;

@QuarkusTest
public class MemberPersistenceTest {

    // Helper method to clean up data before/after tests if needed
    // Transactional for deleteAll/persist might be needed if not using active record directly in test setup
    // For Panache active record, operations are generally auto-committed or handled by test transaction.
    private void cleanup() {
        Member.deleteAll();
    }

    @BeforeEach
    @AfterEach
    public void clearDatabase() {
        cleanup();
    }

    @Test
    public void testMemberPersistenceAndRetrieval() {
        Assertions.assertEquals(0, Member.count(), "Database should be empty at start of test");

        // Create a new member
        Member member = new Member();
        member.setName("Test User");
        member.setEmail("test.user@example.com");
        member.setPhoneNumber("1234567890");

        // Persist the member using active record pattern
        member.persist();
        Assertions.assertNotNull(member.id, "Member ID should not be null after persist");

        // Verify count
        Assertions.assertEquals(1, Member.count(), "Should have 1 member after persist");

        // Find by ID
        Member foundById = Member.findById(member.id);
        Assertions.assertNotNull(foundById, "Member should be found by ID");
        Assertions.assertEquals("Test User", foundById.getName());
        Assertions.assertEquals(member.id, foundById.id);

        // Find by email (custom query using PanacheQL style for PanacheMongoEntity)
        Member foundByEmail = Member.find("email", "test.user@example.com").firstResult();
        Assertions.assertNotNull(foundByEmail, "Member should be found by email");
        Assertions.assertEquals("Test User", foundByEmail.getName());

        // List all
        List<Member> members = Member.listAll();
        Assertions.assertNotNull(members);
        Assertions.assertEquals(1, members.size(), "ListAll should return 1 member");
        Assertions.assertEquals("Test User", members.get(0).getName());

        // Update
        foundById.setName("Updated Test User");
        foundById.update(); // or foundById.persistOrUpdate() or Member.update(foundById) depending on Panache flavor
        
        Member updatedMember = Member.findById(member.id);
        Assertions.assertNotNull(updatedMember);
        Assertions.assertEquals("Updated Test User", updatedMember.getName());

        // Delete by ID using active record
        boolean deleted = Member.deleteById(member.id);
        Assertions.assertTrue(deleted, "DeleteById should return true for existing member");
        Assertions.assertEquals(0, Member.count(), "Database should be empty after delete");

        // Verify not found after delete
        Assertions.assertNull(Member.findById(member.id), "Member should be null after delete");
    }

    @Test
    public void testDuplicateEmailConstraint() {
        // This test will rely on the unique index being created on MongoDB for the 'email' field.
        // Panache itself won't throw a client-side validation error for this before hitting the DB
        // unless we add custom validation logic or a repository layer that checks first.
        // The database will reject the second insert if the unique index is in place.

        Member member1 = new Member("User One", "duplicate@example.com", "1112223333");
        member1.persist();
        Assertions.assertEquals(1, Member.count());

        Member member2 = new Member("User Two", "duplicate@example.com", "4445556666");
        try {
            member2.persist(); 
            // If the unique index works, the above line should throw an exception from MongoDB driver
            // e.g., com.mongodb.MongoWriteException with error code E11000 duplicate key error
            Assertions.fail("Should have thrown an exception due to duplicate email");
        } catch (Exception e) { 
            // Check for MongoDB specific duplicate key error, e.g. using ((MongoWriteException) e).getError().getCode() == 11000
            // For now, a generic catch is fine for this basic test, but more specific check is better.
            Assertions.assertTrue(e.getMessage().toLowerCase().contains("duplicate key") || e.getMessage().toLowerCase().contains("e11000"),
                    "Exception message should indicate a duplicate key error. Got: " + e.getMessage());
        }
        Assertions.assertEquals(1, Member.count(), "Count should remain 1 after failed duplicate insert");
    }
} 