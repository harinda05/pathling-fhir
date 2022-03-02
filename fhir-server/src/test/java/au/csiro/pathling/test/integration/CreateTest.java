/*
 * Copyright © 2018-2022, Commonwealth Scientific and Industrial Research
 * Organisation (CSIRO) ABN 41 687 119 230. Licensed under the CSIRO Open Source
 * Software Licence Agreement.
 */

package au.csiro.pathling.test.integration;

import static au.csiro.pathling.test.helpers.TestHelpers.getResourceAsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.URI;
import java.net.URISyntaxException;
import org.hl7.fhir.r4.model.Enumerations.ResourceType;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = {
    "pathling.storage.warehouseUrl=" + IntegrationTest.INDIVIDUAL_TEST_WAREHOUSE,
    "pathling.storage.databaseName=CreateTest"
})
public class CreateTest extends ModificationTest {

  public static final CustomComparator ID_BLIND_COMPARATOR = new CustomComparator(
      JSONCompareMode.LENIENT, new Customization("id", (o1, o2) -> true));

  @Override
  protected String getTestName() {
    return "CreateTest";
  }

  @Test
  void create() throws URISyntaxException {
    // Check the total Patient count.
    final int expectedCount = Math.toIntExact(resourceReader.read(ResourceType.PATIENT).count());
    assertPatientCount(expectedCount);

    // Send a create request with a new Patient resource.
    final String request = getResourceAsString("requests/CreateTest/create.Patient.json");
    final String url = "http://localhost:" + port + "/fhir/Patient";
    final ResponseEntity<String> response = restTemplate
        .exchange(url, HttpMethod.POST, RequestEntity.post(new URI(url))
            .contentType(FHIR_MEDIA_TYPE)
            .accept(FHIR_MEDIA_TYPE)
            .body(request), String.class);
    assertEquals(201, response.getStatusCode().value());
    assertNotNull(response.getBody());
    JSONAssert.assertEquals(request, response.getBody(), ID_BLIND_COMPARATOR);

    // Get the new patient resource via search and verify its contents.
    final Patient patient = (Patient) jsonParser.parseResource(response.getBody());
    final String patientId = patient.getIdElement().getIdPart().replace("Patient/", "");
    getPatientResult(patientId);

    // Check that the new Patient count is now one more than it was previously.
    assertPatientCount(expectedCount + 1);
  }

}
