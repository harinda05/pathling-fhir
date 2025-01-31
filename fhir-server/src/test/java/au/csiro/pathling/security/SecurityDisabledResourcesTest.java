/*
 * Copyright © 2018-2022, Commonwealth Scientific and Industrial Research
 * Organisation (CSIRO) ABN 41 687 119 230. Licensed under the CSIRO Open Source
 * Software Licence Agreement.
 */

package au.csiro.pathling.security;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = {"pathling.auth.enabled=false"})
class SecurityDisabledResourcesTest extends SecurityTestForResources {

  @Test
  void testPassIfResourceWriteWithNoAuth() {
    assertWriteSuccess();
  }

  @Test
  void testPassIfResourceUpdateWithNoAuth() {
    assertUpdateSuccess();
  }

  @Test
  void testPassIfResourceReadWithNoAuth() {
    assertReadSuccess();
  }

}
