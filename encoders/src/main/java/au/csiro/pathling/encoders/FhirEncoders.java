/*
 * This is a modified version of the Bunsen library, originally published at
 * https://github.com/cerner/bunsen.
 *
 * Bunsen is copyright 2017 Cerner Innovation, Inc., and is licensed under
 * the Apache License, version 2.0 (http://www.apache.org/licenses/LICENSE-2.0).
 *
 * These modifications are copyright © 2018-2022, Commonwealth Scientific
 * and Industrial Research Organisation (CSIRO) ABN 41 687 119 230. Licensed
 * under the CSIRO Open Source Software Licence Agreement.
 *
 */

package au.csiro.pathling.encoders;

import au.csiro.pathling.encoders.datatypes.DataTypeMappings;
import au.csiro.pathling.encoders1.EncoderBuilder1;
import au.csiro.pathling.encoders1.SchemaConverter1;
import au.csiro.pathling.encoders2.EncoderBuilder2;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.context.RuntimeResourceDefinition;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.apache.spark.sql.catalyst.encoders.ExpressionEncoder;
import org.hl7.fhir.instance.model.api.IBaseResource;
import scala.collection.JavaConverters;

/**
 * Spark Encoders for FHIR Resources. This object is thread safe.
 */
public class FhirEncoders {

  /**
   * Cache of Encoders instances.
   */
  private static final Map<EncodersKey, FhirEncoders> ENCODERS = new HashMap<>();

  /**
   * Cache of mappings between Spark and FHIR types.
   */
  private static final Map<FhirVersionEnum, DataTypeMappings> DATA_TYPE_MAPPINGS = new HashMap<>();

  /**
   * Cache of FHIR contexts.
   */
  private static final Map<FhirVersionEnum, FhirContext> FHIR_CONTEXTS = new HashMap<>();

  /**
   * The FHIR context used by the encoders instance.
   */
  private final FhirContext context;

  /**
   * The data type mappings used by the encoders instance.
   */
  private final DataTypeMappings mappings;

  /**
   * Cached encoders to avoid having to re-create them.
   */
  private final Map<Integer, ExpressionEncoder<?>> encoderCache = new HashMap<>();

  /**
   * The maximum nesting level for expansion of recursive data types.
   */
  private final int maxNestingLevel;


  /**
   * Indicates if FHIR extension should be enabled.
   */
  private final boolean enableExtensions;


  /**
   * The encoder version to use.
   */
  private final int encoderVersion;

  /**
   * Consumers should generally use the {@link #forR4()} method, but this is made available for test
   * purposes and additional experimental mappings.
   *
   * @param context the FHIR context to use.
   * @param mappings mappings between Spark and FHIR data types.
   * @param maxNestingLevel maximum nesting level for expansion of recursive data types.
   * @param enableExtensions true if  if FHIR extension should be enabled.
   * @param encoderVersion the encoder version to use.
   */
  public FhirEncoders(final FhirContext context, final DataTypeMappings mappings,
      final int maxNestingLevel, boolean enableExtensions, int encoderVersion) {
    this.enableExtensions = enableExtensions;

    if (encoderVersion != 1 && encoderVersion != 2) {
      throw new IllegalArgumentException(
          "Unsupported encoder version: " + encoderVersion + ". Valid version are 1 or 2");
    }
    this.context = context;
    this.mappings = mappings;
    this.maxNestingLevel = maxNestingLevel;
    this.encoderVersion = encoderVersion;
  }

  /**
   * Returns the FHIR context for the given version. This is effectively a cache so consuming code
   * does not need to recreate the context repeatedly.
   *
   * @param fhirVersion the version of FHIR to use
   * @return the FhirContext
   */
  public static FhirContext contextFor(final FhirVersionEnum fhirVersion) {

    synchronized (FHIR_CONTEXTS) {

      FhirContext context = FHIR_CONTEXTS.get(fhirVersion);

      if (context == null) {

        context = new FhirContext(fhirVersion);

        FHIR_CONTEXTS.put(fhirVersion, context);
      }

      return context;
    }
  }

  /**
   * Returns the {@link DataTypeMappings} instance for the given FHIR version.
   *
   * @param fhirVersion the FHIR version for the data type mappings.
   * @return a DataTypeMappings instance.
   */
  static DataTypeMappings mappingsFor(final FhirVersionEnum fhirVersion) {

    synchronized (DATA_TYPE_MAPPINGS) {

      DataTypeMappings mappings = DATA_TYPE_MAPPINGS.get(fhirVersion);

      if (mappings == null) {
        final String dataTypesClassName;

        if (fhirVersion == FhirVersionEnum.R4) {
          dataTypesClassName = "au.csiro.pathling.encoders.datatypes.R4DataTypeMappings";
        } else {
          throw new IllegalArgumentException("Unsupported FHIR version: " + fhirVersion);
        }

        try {

          mappings = (DataTypeMappings) Class.forName(dataTypesClassName).getDeclaredConstructor()
              .newInstance();

          DATA_TYPE_MAPPINGS.put(fhirVersion, mappings);

        } catch (final Exception createClassException) {

          throw new IllegalStateException("Unable to create the data mappings "
              + dataTypesClassName
              + ". This is typically because the HAPI FHIR dependencies for "
              + "the underlying data model are note present. Make sure the "
              + " hapi-fhir-structures-* and hapi-fhir-validation-resources-* "
              + " jars for the desired FHIR version are available on the class path.",
              createClassException);
        }
      }

      return mappings;
    }
  }

  /**
   * Returns a builder to create encoders for FHIR R4.
   *
   * @return a builder for encoders.
   */
  public static Builder forR4() {

    return forVersion(FhirVersionEnum.R4);
  }

  /**
   * Returns a builder to create encoders for the given FHIR version.
   *
   * @param fhirVersion the version of FHIR to use.
   * @return a builder for encoders.
   */
  public static Builder forVersion(final FhirVersionEnum fhirVersion) {
    return new Builder(fhirVersion);
  }

  /**
   * Returns an encoder for the given FHIR resource.
   *
   * @param resourceName the type of the resource to encode.
   * @param <T> the type of the resource to be encoded.
   * @return an encoder for the resource.
   */
  public final <T extends IBaseResource> ExpressionEncoder<T> of(final String resourceName) {

    final RuntimeResourceDefinition definition = context.getResourceDefinition(resourceName);
    //noinspection unchecked
    return of((Class<T>) definition.getImplementingClass());
  }

  /**
   * Returns an encoder for the given FHIR resource.
   *
   * @param type the type of the resource to encode.
   * @param <T> the type of the resource to be encoded.
   * @return an encoder for the resource.
   */
  public final <T extends IBaseResource> ExpressionEncoder<T> of(final Class<T> type) {

    final RuntimeResourceDefinition definition =
        context.getResourceDefinition(type);

    final int key = type.getName().hashCode();

    synchronized (encoderCache) {

      //noinspection unchecked
      ExpressionEncoder<T> encoder = (ExpressionEncoder<T>) encoderCache.get(key);

      if (encoder == null) {
        if (encoderVersion == 2) {
          //noinspection unchecked
          encoder = (ExpressionEncoder<T>)
              EncoderBuilder2.of(definition,
                  context,
                  mappings,
                  maxNestingLevel,
                  enableExtensions);
        } else if (encoderVersion == 1) {
          //noinspection unchecked
          encoder = (ExpressionEncoder<T>)
              EncoderBuilder1.of(definition,
                  context,
                  mappings,
                  new SchemaConverter1(context, mappings, maxNestingLevel),
                  JavaConverters.asScalaBuffer(Collections.emptyList()));
        } else {
          throw new IllegalArgumentException(
              "Unsupported encoderVersion: " + encoderVersion + ". Only 1 and 2 are supported.");
        }
        encoderCache.put(key, encoder);
      }

      return encoder;
    }
  }

  /**
   * Returns the version of FHIR used by encoders produced by this instance.
   *
   * @return the version of FHIR used by encoders produced by this instance.
   */
  public FhirVersionEnum getFhirVersion() {

    return context.getVersion().getVersion();
  }

  /**
   * Immutable key to look up a matching encoders instance by configuration.
   */
  private static class EncodersKey {

    final FhirVersionEnum fhirVersion;
    final int maxNestingLevel;
    final boolean enableExtensions;
    final int encoderVersion;

    EncodersKey(final FhirVersionEnum fhirVersion, int maxNestingLevel, boolean enableExtensions,
        int encoderVersion) {
      this.fhirVersion = fhirVersion;
      this.maxNestingLevel = maxNestingLevel;
      this.enableExtensions = enableExtensions;
      this.encoderVersion = encoderVersion;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      EncodersKey that = (EncodersKey) o;
      return maxNestingLevel == that.maxNestingLevel &&
          enableExtensions == that.enableExtensions &&
          encoderVersion == that.encoderVersion &&
          fhirVersion == that.fhirVersion;
    }

    @Override
    public int hashCode() {
      return Objects.hash(fhirVersion, maxNestingLevel, enableExtensions, encoderVersion);
    }
  }

  /**
   * Encoder builder. Today only the FHIR version is specified, but future builders may allow
   * customization of the profile used.
   */
  public static class Builder {

    private final static int DEFAULT_ENCODER_VERSION = 2;
    private static final boolean DEFAULT_ENABLE_EXTENSIONS = false;
    private final FhirVersionEnum fhirVersion;
    private int maxNestingLevel;
    private int encoderVersion;
    private boolean enableExtensions;

    Builder(final FhirVersionEnum fhirVersion) {
      this.fhirVersion = fhirVersion;
      this.maxNestingLevel = 0;
      this.encoderVersion = DEFAULT_ENCODER_VERSION;
      this.enableExtensions = DEFAULT_ENABLE_EXTENSIONS;
    }

    /**
     * Set the maximum nesting level for recursive data types. Zero (0) indicates that all direct or
     * indirect fields of type T in element of type T should be skipped.
     *
     * @param maxNestingLevel the maximum nesting level
     * @return this builder
     */
    public Builder withMaxNestingLevel(int maxNestingLevel) {
      this.maxNestingLevel = maxNestingLevel;
      return this;
    }

    public Builder withV1() {
      this.encoderVersion = 1;
      return this;
    }

    @SuppressWarnings("unused")
    public Builder withV2() {
      this.encoderVersion = 2;
      return this;
    }

    @SuppressWarnings("unused")
    public Builder enableExtensions(final boolean enable) {
      this.enableExtensions = enable;
      return this;
    }

    /**
     * Get or create an {@link FhirEncoders} instance that matches the builder's configuration.
     *
     * @return an Encoders instance.
     */
    public FhirEncoders getOrCreate() {

      final EncodersKey key = new EncodersKey(fhirVersion, maxNestingLevel,
          enableExtensions, encoderVersion);

      synchronized (ENCODERS) {

        FhirEncoders encoders = ENCODERS.get(key);

        // No instance with the given configuration found,
        // so create one.
        if (encoders == null) {

          final FhirContext context = contextFor(fhirVersion);
          final DataTypeMappings mappings = mappingsFor(fhirVersion);
          encoders = new FhirEncoders(context, mappings, maxNestingLevel,
              enableExtensions,
              encoderVersion);
          ENCODERS.put(key, encoders);
        }
        return encoders;
      }
    }
  }
}
