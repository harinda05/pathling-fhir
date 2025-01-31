/*
 * Copyright © 2018-2022, Commonwealth Scientific and Industrial Research
 * Organisation (CSIRO) ABN 41 687 119 230. Licensed under the CSIRO Open Source
 * Software Licence Agreement.
 */

package au.csiro.pathling;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;
import javax.annotation.Nonnull;

/**
 * Retrieves version information from the properties file created during the build, making it
 * available to other areas of the application.
 *
 * @author John Grimes
 */
public class PathlingVersion {

  private static final String GIT_PROPERTIES_FILE_NAME = "git.properties";
  private static final String BUILD_VERSION_PROPERTY = "git.build.version";
  private static final String GIT_SHA_PROPERTY = "git.commit.id.abbrev";

  @Nonnull
  private final Properties gitProperties = new Properties();

  /**
   * Default constructor for creating a new PathlingVersion instance.
   */
  public PathlingVersion() {
    initialiseGitProperties();
  }

  private void initialiseGitProperties() {
    final InputStream gitPropertiesStream = getClass().getClassLoader()
        .getResourceAsStream(GIT_PROPERTIES_FILE_NAME);
    if (gitPropertiesStream != null) {
      try {
        gitProperties.load(gitPropertiesStream);
      } catch (final IOException e) {
        throw new RuntimeException("Unable to read property file: " + GIT_PROPERTIES_FILE_NAME);
      }
    } else {
      throw new RuntimeException(
          "Required property file not found in classpath: " + GIT_PROPERTIES_FILE_NAME);
    }
  }

  /**
   * @return the POM version of the application
   */
  public Optional<String> getBuildVersion() {
    return Optional.ofNullable(gitProperties.getProperty(BUILD_VERSION_PROPERTY));
  }

  /**
   * @return the major version component of the POM version
   * @see <a href="https://semver.org/spec/v2.0.0.html">Semantic Versioning 2.0.0</a>
   */
  @Nonnull
  public Optional<String> getMajorVersion() {
    return getBuildVersion().map(version -> version.split("\\.")[0]);
  }

  /**
   * @return a descriptive version that includes the POM version and the Git commit SHA at the time
   * of the build
   */
  public Optional<String> getDescriptiveVersion() {
    if (getBuildVersion().isEmpty()) {
      return Optional.empty();
    }
    final Optional<String> gitShaProperty = Optional.ofNullable(
        gitProperties.getProperty(GIT_SHA_PROPERTY));
    return gitShaProperty.map(sha -> String.format("%s+%s", getBuildVersion().get(), sha));
  }

}
