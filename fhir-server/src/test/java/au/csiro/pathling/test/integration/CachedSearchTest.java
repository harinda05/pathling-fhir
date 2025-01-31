/*
 * Copyright © 2018-2022, Commonwealth Scientific and Industrial Research
 * Organisation (CSIRO) ABN 41 687 119 230. Licensed under the CSIRO Open Source
 * Software Licence Agreement.
 */

package au.csiro.pathling.test.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import org.apache.spark.sql.SparkSession;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;

/**
 * @author John Grimes
 */
@Tag("Tranche2")
class CachedSearchTest extends IntegrationTest {

  @Autowired
  SparkSession spark;

  @LocalServerPort
  int port;

  @Autowired
  TestRestTemplate restTemplate;

  @Test
  void searchWithNoFilter() throws URISyntaxException {
    final String uri = "http://localhost:" + port + "/fhir/Patient?_summary=false";
    final ResponseEntity<String> response = restTemplate
        .exchange(uri, HttpMethod.GET, RequestEntity.get(new URI(uri)).build(), String.class);
    assertTrue(response.getStatusCode().is2xxSuccessful());
    final List<String> eTags = response.getHeaders().get("ETag");
    assertNotNull(eTags);
    assertEquals(1, eTags.size());
    final String eTag = eTags.get(0);
    final ResponseEntity<String> response2 = restTemplate
        .exchange(uri, HttpMethod.GET, RequestEntity.get(new URI(uri))
            .header("If-None-Match", new String[]{eTag})
            .build(), String.class);
    assertEquals(304, response2.getStatusCode().value());
  }

}
