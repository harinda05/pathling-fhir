/*
 * Copyright © 2018-2022, Commonwealth Scientific and Industrial Research
 * Organisation (CSIRO) ABN 41 687 119 230. Licensed under the CSIRO Open Source
 * Software Licence Agreement.
 */

package au.csiro.pathling.io;

import javax.annotation.Nonnull;
import org.hl7.fhir.r4.model.Enumerations.ResourceType;

/**
 * Methods relating to the persistence of data.
 *
 * @author John Grimes
 */
public abstract class PersistenceScheme {

  /**
   * @param warehouseUrl the URL of the warehouse location
   * @param databaseName the name of the database within the warehouse
   * @param resourceType the resource type to be read or written to
   * @return the URL of the resource within the warehouse
   */
  @Nonnull
  public static String getTableUrl(@Nonnull final String warehouseUrl,
      @Nonnull final String databaseName, @Nonnull final ResourceType resourceType) {
    return String.join("/", warehouseUrl, databaseName, fileNameForResource(resourceType));
  }

  /**
   * @param resourceType A HAPI {@link ResourceType} describing the type of resource
   * @return The filename that should be used
   */
  @Nonnull
  public static String fileNameForResource(@Nonnull final ResourceType resourceType) {
    return resourceType.toCode() + ".parquet";
  }

  /**
   * @param s3Url The S3 URL that should be converted
   * @return A S3A URL
   */
  @Nonnull
  public static String convertS3ToS3aUrl(@Nonnull final String s3Url) {
    return s3Url.replaceFirst("s3:", "s3a:");
  }

  /**
   * @param s3aUrl The S3A URL that should be converted
   * @return A S3 URL
   */
  @Nonnull
  public static String convertS3aToS3Url(@Nonnull final String s3aUrl) {
    return s3aUrl.replaceFirst("s3a:", "s3:");
  }

}
