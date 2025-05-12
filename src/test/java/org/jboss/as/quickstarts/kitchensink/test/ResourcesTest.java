package org.jboss.as.quickstarts.kitchensink.test;

import static org.junit.Assert.assertNotNull;

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
} 