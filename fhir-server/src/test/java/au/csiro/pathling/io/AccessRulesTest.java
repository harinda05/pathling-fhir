/*
 * Copyright © 2018-2022, Commonwealth Scientific and Industrial Research
 * Organisation (CSIRO) ABN 41 687 119 230. Licensed under the CSIRO Open Source
 * Software Licence Agreement.
 */

package au.csiro.pathling.io;

import static org.junit.jupiter.api.Assertions.assertThrows;

import au.csiro.pathling.config.Configuration;
import au.csiro.pathling.config.ImportConfiguration;
import au.csiro.pathling.errors.SecurityError;
import java.util.Arrays;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Test some basic Orderable behaviour across different FhirPath types.
 *
 * @author Piotr Szul
 */
@Tag("UnitTest")
class AccessRulesTest {

  final AccessRules accessRules;

  AccessRulesTest() {
    final Configuration configuration = new Configuration();
    final ImportConfiguration importConf = new ImportConfiguration();
    configuration.setImport(importConf);
    importConf.setAllowableSources(Arrays.asList("file:///usr/share/import", "s3://data/"));
    this.accessRules = new AccessRules(configuration);
  }

  @Test
  void allowsConfiguredSources() {
    accessRules.checkCanImportFrom("file:///usr/share/import");
    accessRules.checkCanImportFrom("file:///usr/share/import/somefile.json");
  }

  @Test
  void prohibitsAccessToUnlistedSources() {
    assertThrows(SecurityError.class,
        () -> accessRules.checkCanImportFrom("file:///usr/share/other"));
  }

  @Test
  void operatesOnPhysicalUrls() {
    accessRules.checkCanImportFrom("s3a://data/file.json");
    assertThrows(SecurityError.class, () -> accessRules.checkCanImportFrom("s3://data/file.json"));
  }

}
