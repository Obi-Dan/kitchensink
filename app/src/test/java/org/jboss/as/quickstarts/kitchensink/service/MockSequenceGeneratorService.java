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

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Mock implementation of SequenceGeneratorService for testing purposes. This version does not
 * interact with MongoDB and provides a simple in-memory sequence.
 */
@Alternative
@Priority(1) // Ensures this alternative is chosen over the default one during tests
@ApplicationScoped
public class MockSequenceGeneratorService extends SequenceGeneratorService {

    private final Map<String, AtomicLong> sequences = new ConcurrentHashMap<>();

    public MockSequenceGeneratorService() {
        // Call to super() is implicit. The superclass @Inject fields for MongoDB
        // won't be an issue because the superclass (real SequenceGeneratorService)
        // should be vetoed by @UnlessBuildProfile("test") in the test context where this mock is
        // active.
    }

    @Override
    public Long getNextSequence(String sequenceName) {
        // Start sequences at 0, so first call for a new sequenceName returns 0.
        return sequences.computeIfAbsent(sequenceName, k -> new AtomicLong(-1L)).incrementAndGet();
    }

    @Override
    public void initializeSequence(String sequenceName, long initialValue) {
        // Store the initial value, adjusted by -1 because getNextSequence pre-increments.
        // For consistency with getNextSequence starting new sequences at 0 (after incrementing from
        // -1):
        sequences.put(sequenceName, new AtomicLong(initialValue - 1));
    }
}
