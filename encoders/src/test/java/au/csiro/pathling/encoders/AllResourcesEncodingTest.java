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

import static au.csiro.pathling.encoders.SchemaConverterTest.OPEN_TYPES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import au.csiro.pathling.encoders.datatypes.R4DataTypeMappings;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.RuntimeResourceDefinition;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.apache.spark.sql.catalyst.InternalRow;
import org.apache.spark.sql.catalyst.encoders.ExpressionEncoder;
import org.apache.spark.sql.catalyst.encoders.ExpressionEncoder.Serializer;
import org.apache.spark.sql.types.StructType;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Base;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import scala.collection.JavaConverters;

public class AllResourcesEncodingTest {

  private final static FhirContext FHIR_CONTEXT = FhirContext.forR4();
  private final static FhirEncoders FHIR_ENCODERS = FhirEncoders.forR4()
      .withMaxNestingLevel(2)
      .withOpenTypes(OPEN_TYPES)
      .withExtensionsEnabled(true)
      .getOrCreate();


  private final static SchemaConverter SCHEMA_CONVERTER_L2 = new SchemaConverter(FHIR_CONTEXT,
      new R4DataTypeMappings(),
      EncoderConfig.apply(2, JavaConverters.asScalaSet(OPEN_TYPES).toSet(), true));


  // TODO: Remove when the corresponding issues are fixed (#375)
  final static Set<String> EXCLUDED_RESOURCES = ImmutableSet.of(
      "Parameters",
      // Collections are not supported for custom encoders for: condition-> RuntimeIdDatatypeDefinition[id, IdType]
      "Task",
      // Collections are not supported for custom encoders for: condition-> RuntimeIdDatatypeDefinition[id, IdType]
      "StructureDefinition",
      // Collections are not supported for custom encoders for: condition-> RuntimeIdDatatypeDefinition[id, IdType]
      "StructureMap",
      // Collections are not supported for custom encoders for: condition-> RuntimeIdDatatypeDefinition[id, IdType]
      "Bundle"
      // scala.MatchError: RuntimeElementDirectResource[DirectChildResource, IBaseResource] (of class ca.uhn.fhir.context.RuntimeElementDirectResource)
  );

  public static Stream<Class<? extends IBaseResource>> input() {
    return FHIR_CONTEXT.getResourceTypes().stream()
        .filter(rn -> !EXCLUDED_RESOURCES.contains(rn))
        .map(FHIR_CONTEXT::getResourceDefinition)
        .map(RuntimeResourceDefinition::getImplementingClass);
  }


  @ParameterizedTest
  @MethodSource("input")
  public void testConverterSchemaMatchesEncoder(
      @Nonnull final Class<? extends IBaseResource> resourceClass) {
    final StructType schema = SCHEMA_CONVERTER_L2.resourceSchema(resourceClass);
    final ExpressionEncoder<? extends IBaseResource> encoder = FHIR_ENCODERS
        .of(resourceClass);
    assertEquals(schema.treeString(), encoder.schema().treeString());
  }

  @ParameterizedTest
  @MethodSource("input")
  public void testCanEncodeDecodeResource(
      @Nonnull final Class<? extends IBaseResource> resourceClass) throws Exception {

    final ExpressionEncoder<? extends IBaseResource> encoder = FHIR_ENCODERS
        .of(resourceClass);

    final ExpressionEncoder<? extends IBaseResource> resolvedEncoder = EncoderUtils
        .defaultResolveAndBind(encoder);
    final IBaseResource resourceInstance = resourceClass.getDeclaredConstructor().newInstance();
    resourceInstance.setId("someId");

    final Serializer<? extends IBaseResource> serializer = resolvedEncoder
        .createSerializer();

    //noinspection unchecked
    final InternalRow serializedRow = ((Serializer<IBaseResource>) serializer)
        .apply(resourceInstance);

    final IBaseResource deserializedResource = resolvedEncoder.createDeserializer()
        .apply(serializedRow);

    assertTrue(((Base) resourceInstance).equalsDeep((Base) deserializedResource));
  }

}
