/*
 * Copyright © 2018-2022, Commonwealth Scientific and Industrial Research
 * Organisation (CSIRO) ABN 41 687 119 230. Licensed under the CSIRO Open Source
 * Software Licence Agreement.
 */

package au.csiro.pathling.security;

import static au.csiro.pathling.utilities.Preconditions.checkArgument;

import au.csiro.pathling.config.Configuration;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoderFactory;
import org.springframework.stereotype.Component;

/**
 * A JWT decoder that is capable of using the issuer claim within the token to retrieve the JWKS.
 *
 * @author John Grimes
 */
@Component
@Profile("server & !ga4gh")
@ConditionalOnProperty(prefix = "pathling", name = "auth.enabled", havingValue = "true")
public class PathlingJwtDecoderFactory implements JwtDecoderFactory<Configuration> {

  @Nonnull
  private final PathlingJwtDecoderBuilder builder;

  /**
   * @param builder a builder that can create a {@link JwtDecoder}
   */
  public PathlingJwtDecoderFactory(@Nonnull final PathlingJwtDecoderBuilder builder) {
    this.builder = builder;
  }

  @Override
  public JwtDecoder createDecoder(@Nullable final Configuration configuration) {
    checkArgument(configuration != null, "configuration cannot be null");
    return builder.build(configuration);
  }

  /**
   * @param configuration that controls the behaviour of the decoder factory
   * @param factory a factory that can create a {@link JwtDecoder}
   * @return a shiny new {@link JwtDecoder}
   */
  @Bean
  public static JwtDecoder pathlingJwtDecoder(@Nullable final Configuration configuration,
      @Nonnull final PathlingJwtDecoderFactory factory) {
    return factory.createDecoder(configuration);
  }

}
