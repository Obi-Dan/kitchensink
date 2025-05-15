package org.jboss.as.quickstarts.kitchensink;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** The main class for the Spring Boot application. */
@SpringBootApplication
@SuppressWarnings({"checkstyle:FinalClass", "checkstyle:HideUtilityClassConstructor"})
public class KitchensinkApplication {

  /** Public constructor required by Spring Boot. */
  public KitchensinkApplication() {
    // Required by Spring Boot
  }

  /**
   * Main method to start the Spring Boot application.
   *
   * @param args Command line arguments.
   */
  public static void main(final String[] args) {
    SpringApplication.run(KitchensinkApplication.class, args);
  }
}
