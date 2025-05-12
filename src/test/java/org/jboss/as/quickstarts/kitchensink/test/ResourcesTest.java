package org.jboss.as.quickstarts.kitchensink.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;

import org.jboss.as.quickstarts.kitchensink.util.Resources;
import org.junit.Test;

/**
 * Unit test for Resources utility class
 */
public class ResourcesTest {

    @Test
    public void testResourcesClassLoads() {
        // Simply verify that the Resources class can be instantiated
        Resources resources = new Resources();
        assertNotNull(resources);
    }

    @Test
    public void testLoggerCreation() {
        // This is a basic test that doesn't rely on CDI injection
        // Just verify we can create a logger directly
        Logger logger = Logger.getLogger(ResourcesTest.class.getName());
        assertNotNull(logger);
        assertTrue(logger.getName().contains("ResourcesTest"));
    }
}