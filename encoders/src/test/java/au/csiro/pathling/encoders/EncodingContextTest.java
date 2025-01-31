/*
 * This is a modified version of the Bunsen library, originally published at
 * https://github.com/cerner/bunsen.
 *
 * Bunsen is copyright 2017 Cerner Innovation, Inc., and is licensed under
 * the Apache License, version 2.0 (http://www.apache.org/licenses/LICENSE-2.0).
 *
 * These modifications are copyright © 2018-2022, Commonwealth Scientific
 * and Industrial Research Organisation (CSIRO) ABN 41 687 119 230. Licensed
 * under the CSIRO Open Source Software Licence Agreement.
 *
 */

package au.csiro.pathling.encoders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.RuntimeResourceDefinition;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;

public class EncodingContextTest {

  private final FhirContext fhirContext = FhirContext.forR4();

  @SuppressWarnings("ReturnOfNull")
  @Test
  public void testCorrectNestingLevels() {

    final RuntimeResourceDefinition patientDefinition = fhirContext
        .getResourceDefinition(Patient.class);

    final RuntimeResourceDefinition conditionDefinition = fhirContext
        .getResourceDefinition(Condition.class);

    // start with a new context
    EncodingContext.runWithContext(() -> {

      assertEquals(0, EncodingContext.currentNestingLevel(patientDefinition));
      assertEquals(0, EncodingContext.currentNestingLevel(conditionDefinition));

      // Enter Patient
      EncodingContext.withDefinition(patientDefinition, () -> {
        assertEquals(1, EncodingContext.currentNestingLevel(patientDefinition));
        assertEquals(0, EncodingContext.currentNestingLevel(conditionDefinition));

        // Enter Patient
        EncodingContext.withDefinition(patientDefinition, () -> {
          assertEquals(2, EncodingContext.currentNestingLevel(patientDefinition));
          assertEquals(0, EncodingContext.currentNestingLevel(conditionDefinition));

          // Enter Condition
          EncodingContext.withDefinition(conditionDefinition, () -> {
            assertEquals(2, EncodingContext.currentNestingLevel(patientDefinition));
            assertEquals(1, EncodingContext.currentNestingLevel(conditionDefinition));
            return null;
          });
          assertEquals(2, EncodingContext.currentNestingLevel(patientDefinition));
          assertEquals(0, EncodingContext.currentNestingLevel(conditionDefinition));
          return null;
        });
        assertEquals(1, EncodingContext.currentNestingLevel(patientDefinition));
        assertEquals(0, EncodingContext.currentNestingLevel(conditionDefinition));
        return null;
      });
      assertEquals(0, EncodingContext.currentNestingLevel(patientDefinition));
      assertEquals(0, EncodingContext.currentNestingLevel(conditionDefinition));
      return null;
    });
  }


  @Test
  public void testFailsWithoutContext() {

    final RuntimeResourceDefinition patientDefinition = fhirContext
        .getResourceDefinition(Patient.class);

    assertThrows(AssertionError.class,
        () -> EncodingContext.currentNestingLevel(patientDefinition),
        "Current EncodingContext does not exists.");
  }


  @SuppressWarnings("ReturnOfNull")
  @Test
  public void testFailsWhenNestedContextIsCreated() {

    EncodingContext.runWithContext(() -> {
      assertThrows(AssertionError.class, () -> EncodingContext.runWithContext(() -> null),
          "There should be no current context");
      return null;
    });
  }

}
