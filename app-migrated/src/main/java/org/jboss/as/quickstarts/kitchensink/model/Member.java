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

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// For the ID type if explicitly using ObjectId

// @XmlRootElement is not typically used with Panache entities unless also serving as JAX-RS
// response
// For MongoDB, unique constraints are usually handled by creating a unique index on the collection.
@MongoEntity(collection = "members")
public class Member
        extends PanacheMongoEntity { // Inherit from PanacheMongoEntity for Active Record

    // PanacheMongoEntity provides an 'id' field of type ObjectId by default.
    // If you need to rename it or use a different type, you can declare it:
    // public ObjectId id; // This is inherited

    @NotNull
    @Size(min = 1, max = 25)
    @Pattern(regexp = "[^0-9]*", message = "Must not contain numbers")
    public String name; // Made public for Panache Active Record pattern, or use getters/setters

    @NotNull @NotEmpty @Email public String email; // Made public

    @NotNull
    @Size(min = 10, max = 12)
    @Digits(fraction = 0, integer = 12)
    public String phoneNumber; // Made public, JPA @Column(name = "phone_number") is not needed

    // Constructors, getters, and setters can be added if needed,
    // but Panache Active Record allows direct field access.
    // For more complex logic or encapsulation, use methods.

    // Example: Default constructor (often needed by frameworks)
    public Member() {}

    // Example: Convenience constructor
    public Member(String name, String email, String phoneNumber) {
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
    }

    // Getters and Setters (optional with Panache Active Record, but good practice for encapsulation
    // if preferred)

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    // PanacheMongoEntity provides an id of type ObjectId.
    // If you need a String representation of the id:
    public String getStringId() {
        return id != null ? id.toString() : null;
    }
}
