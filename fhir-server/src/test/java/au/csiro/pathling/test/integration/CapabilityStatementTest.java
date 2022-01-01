/*
 * Copyright © 2018-2022, Commonwealth Scientific and Industrial Research
 * Organisation (CSIRO) ABN 41 687 119 230. Licensed under the CSIRO Open Source
 * Software Licence Agreement.
 */

package au.csiro.pathling.test.integration;

import static au.csiro.pathling.test.assertions.Assertions.assertJson;

import au.csiro.pathling.Configuration;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

/**
 * @author John Grimes
 */
class CapabilityStatementTest extends IntegrationTest {

  @LocalServerPort
  private int port;

  @Autowired
  private TestRestTemplate restTemplate;

  @Test
  void capabilityStatement() throws JSONException {
    final String response = restTemplate
        .getForObject("http://localhost:" + port + "/fhir/metadata", String.class);
    assertJson("responses/CapabilityStatementTest/capabilityStatement.CapabilityStatement.json",
        response, JSONCompareMode.LENIENT);
  }


  @Test
  void cors() throws JSONException {
    HttpHeaders corsHeaders  = new HttpHeaders();
    corsHeaders.setOrigin("http://foo.bar");
    corsHeaders.setAccessControlRequestMethod(HttpMethod.GET);

    ResponseEntity<String> response = restTemplate
        .exchange("http://localhost:" + port + "/fhir/metadata", HttpMethod.OPTIONS,
            new HttpEntity<String>(corsHeaders),
            String.class);

    System.out.println(response);
  }



}