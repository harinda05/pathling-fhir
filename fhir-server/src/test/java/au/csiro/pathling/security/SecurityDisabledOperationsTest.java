/*
 * Copyright © 2018-2022, Commonwealth Scientific and Industrial Research
 * Organisation (CSIRO) ABN 41 687 119 230. Licensed under the CSIRO Open Source
 * Software Licence Agreement.
 */

package au.csiro.pathling.security;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;


/**
 * @see <a
 * href="https://docs.spring.io/spring-security/site/docs/5.2.x/reference/html/test.html">Spring
 * Security - Testing</a>
 * @see <a
 * href="https://stackoverflow.com/questions/58289509/in-spring-boot-test-how-do-i-map-a-temporary-folder-to-a-configuration-property">In
 * Spring Boot Test, how do I map a temporary folder to a configuration property?</a>
 */
@TestPropertySource(properties = {"pathling.auth.enabled=false"})
class SecurityDisabledOperationsTest extends SecurityTestForOperations {

  @Test
  void testPassIfImportWithNoAuth() {
    assertImportSuccess();
  }

  @Test
  void testPassIfAggregateWithNoAuth() {
    assertAggregateSuccess();
  }

  @Test
  void testPassIfSearchWithNoAuth() {
    assertSearchSuccess();
    assertSearchWithFilterSuccess();
  }

  @Test
  void testPassIfUpdateWithNoAuth() {
    assertUpdateSuccess();
  }

  @Test
  void testPassIfBatchWithNoAuth() {
    assertBatchSuccess();
  }

}
