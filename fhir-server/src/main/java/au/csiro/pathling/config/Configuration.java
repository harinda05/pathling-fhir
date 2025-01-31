/*
 * Copyright © 2018-2022, Commonwealth Scientific and Industrial Research
 * Organisation (CSIRO) ABN 41 687 119 230. Licensed under the CSIRO Open Source
 * Software Licence Agreement.
 */

package au.csiro.pathling.config;

import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Defines all the configuration options for the Pathling server.
 * <p>
 * See {@code application.yml} for default values.
 *
 * @author John Grimes
 */
@ConfigurationProperties(prefix = "pathling")
@Validated
@Data
@ToString(doNotUseGetters = true)
public class Configuration {

  /**
   * Controls the description of this server displayed within the FHIR CapabilityStatement.
   */
  @NotNull
  private String implementationDescription;

  /**
   * If this variable is set, all errors will be reported to a Sentry service, e.g.
   * `https://abc123@sentry.io/123456`.
   */
  @Nullable
  private String sentryDsn;

  /**
   * Sets the environment that will be sent with Sentry reports.
   */
  @Nullable
  private String sentryEnvironment;

  @Nonnull
  public Optional<String> getSentryDsn() {
    return Optional.ofNullable(sentryDsn);
  }

  @Nonnull
  public Optional<String> getSentryEnvironment() {
    return Optional.ofNullable(sentryEnvironment);
  }

  @NotNull
  private SparkConfiguration spark;

  @NotNull
  private StorageConfiguration storage;

  @NotNull
  private TerminologyConfiguration terminology;

  @NotNull
  private AuthorizationConfiguration auth;

  @NotNull
  private HttpCachingConfiguration httpCaching;

  @NotNull
  private CorsConfiguration cors;

  // Handle the `import` property outside of Lombok, as import is a Java keyword.
  @Getter(AccessLevel.NONE)
  @Setter(AccessLevel.NONE)
  @NotNull
  private ImportConfiguration import_;

  @NotNull
  private AsyncConfiguration async;

  @Nonnull
  public ImportConfiguration getImport() {
    return import_;
  }

  public void setImport(@Nonnull final ImportConfiguration import_) {
    this.import_ = import_;
  }

  @NotNull
  private EncodingConfiguration encoding;

}
