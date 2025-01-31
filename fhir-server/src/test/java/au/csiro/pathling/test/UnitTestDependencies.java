/*
 * Copyright © 2018-2022, Commonwealth Scientific and Industrial Research
 * Organisation (CSIRO) ABN 41 687 119 230. Licensed under the CSIRO Open Source
 * Software Licence Agreement.
 */

package au.csiro.pathling.test;

import au.csiro.pathling.PathlingVersion;
import au.csiro.pathling.async.SparkListener;
import au.csiro.pathling.config.Configuration;
import au.csiro.pathling.encoders.FhirEncoders;
import au.csiro.pathling.fhir.TerminologyClient;
import au.csiro.pathling.fhir.TerminologyServiceFactory;
import au.csiro.pathling.spark.Spark;
import au.csiro.pathling.terminology.TerminologyService;
import au.csiro.pathling.test.stubs.TestTerminologyServiceFactory;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.apache.spark.sql.SparkSession;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * @author John Grimes
 */
@Component
@Profile("unit-test")
class UnitTestDependencies {

  @Bean
  @ConditionalOnMissingBean
  @Nonnull
  static PathlingVersion version() {
    return new PathlingVersion();
  }

  @Bean
  @ConditionalOnMissingBean
  @Nonnull
  static SparkSession sparkSession(@Nonnull final Configuration configuration,
      @Nonnull final Environment environment,
      @Nonnull final Optional<SparkListener> sparkListener) {
    return Spark.build(configuration, environment, sparkListener);
  }

  @Bean
  @ConditionalOnMissingBean
  @Nonnull
  static FhirContext fhirContext() {
    return FhirContext.forR4();
  }

  @Bean
  @ConditionalOnMissingBean
  @Nonnull
  static IParser jsonParser(@Nonnull final FhirContext fhirContext) {
    return fhirContext.newJsonParser();
  }

  @Bean
  @ConditionalOnMissingBean
  @Nonnull
  static FhirEncoders fhirEncoders() {
    return FhirEncoders.forR4().getOrCreate();
  }

  @Bean
  @ConditionalOnMissingBean
  @Nonnull
  static TerminologyClient terminologyClient() {
    return SharedMocks.getOrCreate(TerminologyClient.class);
  }

  @Bean
  @ConditionalOnMissingBean
  @Nonnull
  static TerminologyService terminologyService() {
    return SharedMocks.getOrCreate(TerminologyService.class);
  }

  @Bean
  @ConditionalOnMissingBean
  @Nonnull
  static TerminologyServiceFactory terminologyClientFactory() {
    return new TestTerminologyServiceFactory();
  }

}
