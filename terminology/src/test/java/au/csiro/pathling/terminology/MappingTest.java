/*
 * Copyright © 2018-2022, Commonwealth Scientific and Industrial Research
 * Organisation (CSIRO) ABN 41 687 119 230. Licensed under the CSIRO Open Source
 * Software Licence Agreement.
 */

package au.csiro.pathling.terminology;

import static au.csiro.pathling.test.TestResources.assertJson;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

abstract class MappingTest {

  IParser jsonParser;

  @Nullable
  TestInfo testInfo;

  @BeforeEach
  void setUp(@Nonnull final TestInfo testInfo) {
    jsonParser = FhirContext.forR4().newJsonParser();
    this.testInfo = testInfo;
  }

  void assertRequest(
      @Nonnull final IBaseResource resource) {
    //noinspection ConstantConditions,OptionalGetWithoutIsPresent
    assertRequest(resource, testInfo.getTestMethod().get().getName());
  }

  void assertRequest(
      @Nonnull final IBaseResource resource, @Nonnull final String name) {
    final String actualJson = jsonParser.encodeResourceToString(resource);

    //noinspection ConstantConditions,OptionalGetWithoutIsPresent
    final Path expectedPath = Paths
        .get("requests", testInfo.getTestClass().get().getSimpleName(),
            name + ".json");
    assertJson(expectedPath.toString(), actualJson);
  }

}
