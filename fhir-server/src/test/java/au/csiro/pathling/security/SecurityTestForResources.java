/*
 * Copyright © 2018-2022, Commonwealth Scientific and Industrial Research
 * Organisation (CSIRO) ABN 41 687 119 230. Licensed under the CSIRO Open Source
 * Software Licence Agreement.
 */

package au.csiro.pathling.security;

import static org.junit.jupiter.api.Assertions.assertTrue;

import au.csiro.pathling.io.Database;
import au.csiro.pathling.test.builders.ResourceDatasetBuilder;
import java.io.File;
import java.util.Collections;
import org.apache.spark.sql.SparkSession;
import org.hl7.fhir.r4.model.Enumerations.ResourceType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;


/**
 * @see <a
 * href="https://docs.spring.io/spring-security/site/docs/5.2.x/reference/html/test.html">Spring
 * Security - Testing</a>
 * @see <a
 * href="https://stackoverflow.com/questions/58289509/in-spring-boot-test-how-do-i-map-a-temporary-folder-to-a-configuration-property">In
 * Spring Boot Test, how do I map a temporary folder to a configuration property?</a>
 */
@ActiveProfiles({"core", "unit-test"})
abstract class SecurityTestForResources extends SecurityTest {

  @Autowired
  Database database;

  @Autowired
  SparkSession spark;

  @TempDir
  @SuppressWarnings({"unused", "WeakerAccess"})
  static File testRootDir;

  @DynamicPropertySource
  @SuppressWarnings("unused")
  static void registerProperties(final DynamicPropertyRegistry registry) {
    // TODO: This is a bit messy - maybe we should abstract the physical warehouse out, so that it
    //  could be mocked
    final File warehouseDir = new File(testRootDir, "default");
    assertTrue(warehouseDir.mkdir());
    registry.add("pathling.storage.warehouseUrl",
        () -> "file://" + testRootDir.toPath().toString().replaceFirst("/$", ""));
  }

  @AfterEach
  void tearDown() {
    spark.sqlContext().clearCache();
  }

  void assertWriteSuccess() {
    database.overwrite(ResourceType.ACCOUNT,
        new ResourceDatasetBuilder(spark).withIdColumn().build());
  }

  void assertUpdateSuccess() {
    database.merge(ResourceType.ACCOUNT, Collections.emptyList());
  }

  void assertReadSuccess() {
    database.read(ResourceType.ACCOUNT);
  }
}
