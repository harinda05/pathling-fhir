/*
 * Copyright © 2018-2022, Commonwealth Scientific and Industrial Research
 * Organisation (CSIRO) ABN 41 687 119 230. Licensed under the CSIRO Open Source
 * Software Licence Agreement.
 */

package au.csiro.pathling.security;

import au.csiro.pathling.config.Configuration;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Web security configuration for Pathling.
 *
 * @see <a
 * href="https://auth0.com/docs/quickstart/backend/java-spring-security5/01-authorization">Spring
 * Security 5 Java API: Authorization</a>
 * @see <a
 * href="https://stackoverflow.com/questions/51079564/spring-security-antmatchers-not-being-applied-on-post-requests-and-only-works-wi/51088555">Spring
 * security antMatchers not being applied on POST requests and only works with GET</a>
 */
@EnableWebSecurity
@Profile("server")
@Slf4j
public class SecurityConfiguration {

  private final Configuration configuration;

  @Value("${pathling.auth.enabled}")
  private boolean authEnabled;

  public SecurityConfiguration(@Nonnull final Configuration configuration) {
    this.configuration = configuration;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(@Nonnull final HttpSecurity http)
      throws Exception {
    // Will use the bean of class CorsConfigurationSource as configuration provider.
    http.cors();

    if (authEnabled) {
      http.authorizeRequests()
          // The following requests do not require authentication.
          .mvcMatchers(HttpMethod.GET,
              "/metadata",   // Server capabilities operation
              "/OperationDefinition/**",  // GET on OperationDefinition resources
              "/.well-known/**")          // SMART configuration endpoint
          .permitAll()
          // Anything else needs to be authenticated.
          .anyRequest()
          .authenticated()
          .and()
          .oauth2ResourceServer()
          .jwt();

    } else {
      http
          // Without this POST requests fail with 403 Forbidden.
          .csrf().disable()
          .authorizeRequests().anyRequest().permitAll();
    }

    return http.build();
  }

  /**
   * Constructs Spring CORS configuration.
   *
   * @return CORS configuration source
   */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    final CorsConfiguration cors = new CorsConfiguration();
    cors.setAllowedOrigins(configuration.getCors().getAllowedOrigins());
    cors.setAllowedOriginPatterns(configuration.getCors().getAllowedOriginPatterns());
    cors.setAllowedMethods(configuration.getCors().getAllowedMethods());
    cors.setAllowedHeaders(configuration.getCors().getAllowedHeaders());
    cors.setExposedHeaders(configuration.getCors().getExposedHeaders());
    cors.setMaxAge(configuration.getCors().getMaxAge());
    cors.setAllowCredentials(configuration.getAuth().isEnabled());

    final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", cors);
    return source;
  }

}
