/*
 * Copyright © 2018-2022, Commonwealth Scientific and Industrial Research
 * Organisation (CSIRO) ABN 41 687 119 230. Licensed under the CSIRO Open Source
 * Software Licence Agreement.
 */

package au.csiro.pathling.security.ga4gh;

import au.csiro.pathling.config.Configuration;
import au.csiro.pathling.config.AuthorizationConfiguration;
import au.csiro.pathling.security.OidcConfiguration;
import au.csiro.pathling.security.PathlingJwtDecoderBuilder;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.stereotype.Component;

/**
 * @author John Grimes
 */
@Component
@Profile("server & ga4gh")
public class PassportDecoderBuilder extends PathlingJwtDecoderBuilder {

  /**
   * @param oidcConfiguration configuration used to instantiate the builder
   */
  public PassportDecoderBuilder(@Nonnull final OidcConfiguration oidcConfiguration) {
    super(oidcConfiguration);
  }

  @Override
  public JwtDecoder build(@Nonnull final Configuration configuration) {
    final AuthorizationConfiguration auth = getAuthConfiguration(configuration);

    // The passport decoder is the same as the regular Pathling decoder with the exception that the
    // audience claim is not required.
    final List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
    auth.getIssuer().ifPresent(i -> validators.add(new JwtIssuerValidator(i)));
    return buildDecoderWithValidators(validators);
  }

}
