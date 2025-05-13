package org.jboss.as.quickstarts.kitchensink.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;

import org.jboss.as.quickstarts.kitchensink.controller.MemberController;
import org.jboss.as.quickstarts.kitchensink.model.Member;
import org.jboss.as.quickstarts.kitchensink.service.MemberRegistration;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Simple happy path test for MemberController
 */
public class MemberControllerTest {

    private MemberController controller;
    private TestFacesContext facesContext;
    private Member newMember;
    private TestMemberRegistration registration;

    @Before
    public void setup() throws Exception {
        // Initialize controller
        controller = new MemberController();

        // Initialize test dependencies
        facesContext = new TestFacesContext();
        registration = new TestMemberRegistration();

        // Inject dependencies via reflection
        injectDependency(controller, "facesContext", facesContext);
        injectDependency(controller, "memberRegistration", registration);

        // Initialize the controller (normally done via @PostConstruct)
        controller.initNewMember();

        // Get a reference to the newMember for assertions
        Field newMemberField = MemberController.class.getDeclaredField("newMember");
        newMemberField.setAccessible(true);
        newMember = (Member) newMemberField.get(controller);
    }

    @Test
    public void testHappyPath() throws Exception {
        // Set up test data
        newMember.setName("John Doe");
        newMember.setEmail("john@example.com");
        newMember.setPhoneNumber("1234567890");

        // Call the register method
        controller.register();

        // Verify successful registration
        assertEquals("John Doe", registration.getLastRegisteredMember().getName());
        assertEquals("john@example.com", registration.getLastRegisteredMember().getEmail());
        assertEquals("1234567890", registration.getLastRegisteredMember().getPhoneNumber());

        // Verify that success message was added to FacesContext
        assertEquals(1, facesContext.getMessageList().size());
        FacesMessage message = facesContext.getMessageList().get(0);
        assertEquals(FacesMessage.SEVERITY_INFO, message.getSeverity());
        assertEquals("Registered!", message.getSummary());

        // Verify that a new member was initialized after successful registration
        assertNotNull(newMember);
        assertEquals(null, newMember.getName());
        assertEquals(null, newMember.getEmail());
        assertEquals(null, newMember.getPhoneNumber());
    }

    // Helper method to inject dependencies using reflection
    private void injectDependency(Object target, String fieldName, Object dependency) throws Exception {
        Field field = MemberController.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, dependency);
    }

    // Test implementation of FacesContext
    private static class TestFacesContext extends FacesContext {
        private List<FacesMessage> messageList = new ArrayList<>();

        public List<FacesMessage> getMessageList() {
            return messageList;
        }

        @Override
        public void addMessage(String clientId, FacesMessage message) {
            messageList.add(message);
        }

        // Required overrides with minimal implementations
        @Override
        public Iterator<String> getClientIdsWithMessages() {
            return Collections.emptyIterator();
        }

        @Override
        public Iterator<FacesMessage> getMessages() {
            return messageList.iterator();
        }

        @Override
        public Iterator<FacesMessage> getMessages(String clientId) {
            return Collections.emptyIterator();
        }

        @Override
        public jakarta.faces.application.Application getApplication() {
            return null;
        }

        @Override
        public jakarta.faces.application.FacesMessage.Severity getMaximumSeverity() {
            return null;
        }

        @Override
        public jakarta.faces.component.UIViewRoot getViewRoot() {
            return null;
        }

        @Override
        public void setViewRoot(jakarta.faces.component.UIViewRoot viewRoot) {
        }

        @Override
        public jakarta.faces.context.ExternalContext getExternalContext() {
            return null;
        }

        @Override
        public jakarta.faces.context.PartialViewContext getPartialViewContext() {
            return null;
        }

        @Override
        public jakarta.faces.context.ResponseStream getResponseStream() {
            return null;
        }

        @Override
        public void setResponseStream(jakarta.faces.context.ResponseStream responseStream) {
        }

        @Override
        public jakarta.faces.context.ResponseWriter getResponseWriter() {
            return null;
        }

        @Override
        public void setResponseWriter(jakarta.faces.context.ResponseWriter responseWriter) {
        }

        @Override
        public void release() {
        }

        @Override
        public jakarta.faces.render.RenderKit getRenderKit() {
            return null;
        }

        @Override
        public boolean isReleased() {
            return false;
        }

        @Override
        public void renderResponse() {
        }

        @Override
        public void responseComplete() {
        }

        @Override
        public jakarta.faces.lifecycle.Lifecycle getLifecycle() {
            return null;
        }

        @Override
        public boolean getRenderResponse() {
            return false;
        }

        @Override
        public boolean getResponseComplete() {
            return false;
        }
    }

    // Test implementation of MemberRegistration
    private static class TestMemberRegistration extends MemberRegistration {
        private Member lastRegisteredMember;

        @Override
        public void register(Member member) throws Exception {
            this.lastRegisteredMember = member;
        }

        public Member getLastRegisteredMember() {
            return lastRegisteredMember;
        }
    }
}