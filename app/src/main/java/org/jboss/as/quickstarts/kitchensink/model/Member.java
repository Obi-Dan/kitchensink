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
package org.jboss.as.quickstarts.kitchensink.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * Represents a member entity with contact information. This class is a JPA entity and is also used
 * for JAXB marshalling.
 */
@SuppressWarnings("serial")
@Entity
@XmlRootElement
@Table(uniqueConstraints = @UniqueConstraint(columnNames = "email"))
public class Member implements Serializable {

  /** Maximum size for a member's name. */
  private static final int NAME_MAX_SIZE = 25;

  /** Minimum size for a member's phone number. */
  private static final int PHONE_MIN_SIZE = 10;

  /** Maximum size for a member's phone number. */
  private static final int PHONE_MAX_SIZE = 12;

  /** Maximum number of digits for a member's phone number. */
  private static final int PHONE_MAX_DIGITS = 12;

  /** The unique identifier for the member. */
  @Id @GeneratedValue private Long id;

  /** The name of the member. Must be between 1 and 25 characters, and contain no numbers. */
  @NotNull
  @Size(min = 1, max = NAME_MAX_SIZE)
  @Pattern(regexp = "[^0-9]*", message = "Must not contain numbers")
  private String name;

  /** The email address of the member. Must be a valid email format and unique. */
  @NotNull @NotEmpty @Email private String email;

  /** The phone number of the member. Must be between 10 and 12 digits. */
  @NotNull
  @Size(min = PHONE_MIN_SIZE, max = PHONE_MAX_SIZE)
  @Digits(fraction = 0, integer = PHONE_MAX_DIGITS)
  @Column(name = "phone_number")
  private String phoneNumber;

  /**
   * Gets the member's ID.
   *
   * @return The ID of the member.
   */
  public Long getId() {
    return id;
  }

  /**
   * Sets the member's ID.
   *
   * @param id The new ID for the member.
   */
  public void setId(final Long id) {
    this.id = id;
  }

  /**
   * Gets the member's name.
   *
   * @return The name of the member.
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the member's name.
   *
   * @param name The new name for the member.
   */
  public void setName(final String name) {
    this.name = name;
  }

  /**
   * Gets the member's email address.
   *
   * @return The email address of the member.
   */
  public String getEmail() {
    return email;
  }

  /**
   * Sets the member's email address.
   *
   * @param email The new email address for the member.
   */
  public void setEmail(final String email) {
    this.email = email;
  }

  /**
   * Gets the member's phone number.
   *
   * @return The phone number of the member.
   */
  public String getPhoneNumber() {
    return phoneNumber;
  }

  /**
   * Sets the member's phone number.
   *
   * @param phoneNumber The new phone number for the member.
   */
  public void setPhoneNumber(final String phoneNumber) {
    this.phoneNumber = phoneNumber;
  }
}
