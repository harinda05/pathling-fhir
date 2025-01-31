/*
 * Copyright © 2018-2022, Commonwealth Scientific and Industrial Research
 * Organisation (CSIRO) ABN 41 687 119 230. Licensed under the CSIRO Open Source
 * Software Licence Agreement.
 */

package au.csiro.pathling.test.assertions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import au.csiro.pathling.fhirpath.ResourcePath;
import javax.annotation.Nonnull;
import org.hl7.fhir.r4.model.Enumerations.ResourceType;

/**
 * @author John Grimes
 */
public class ResourcePathAssertion extends BaseFhirPathAssertion<ResourcePathAssertion> {

  @Nonnull
  private final ResourcePath fhirPath;

  ResourcePathAssertion(@Nonnull final ResourcePath fhirPath) {
    super(fhirPath);
    this.fhirPath = fhirPath;
  }

  @Nonnull
  public ResourcePathAssertion hasResourceType(@Nonnull final ResourceType type) {
    assertEquals(type, fhirPath.getResourceType());
    return this;
  }

}
