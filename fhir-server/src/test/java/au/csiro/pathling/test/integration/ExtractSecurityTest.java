/*
 * Copyright © 2018-2022, Commonwealth Scientific and Industrial Research
 * Organisation (CSIRO) ABN 41 687 119 230. Licensed under the CSIRO Open Source
 * Software Licence Agreement.
 */

package au.csiro.pathling.test.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verifyNoInteractions;

import au.csiro.pathling.extract.ResultRegistry;
import au.csiro.pathling.io.ResultReader;
import java.net.URI;
import java.net.URISyntaxException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.sparkproject.jetty.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;

/**
 * @author John Grimes
 */
@Tag("Tranche2")
class ExtractSecurityTest extends IntegrationTest {

  @MockBean
  ResultRegistry resultRegistry;

  @MockBean
  ResultReader resultReader;

  @LocalServerPort
  int port;

  @Autowired
  TestRestTemplate restTemplate;

  @Test
  void illegalResultId() throws URISyntaxException {
    final String uri = "http://localhost:" + port
        + "/fhir/$result?id=..%2F..%2F..%2Fsomeotherproject%2Fresults%2F123abc";
    final ResponseEntity<String> response = restTemplate
        .exchange(uri, HttpMethod.GET, RequestEntity.get(new URI(uri)).build(), String.class);
    assertEquals(HttpStatus.NOT_FOUND_404, response.getStatusCode().value());
    verifyNoInteractions(resultRegistry, resultReader);
  }

}
