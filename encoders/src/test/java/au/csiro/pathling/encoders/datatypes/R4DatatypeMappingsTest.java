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

package au.csiro.pathling.encoders.datatypes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import au.csiro.pathling.schema.ElementCtx;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.RuntimeChildChoiceDefinition;
import ca.uhn.fhir.context.RuntimeChildExtension;
import ca.uhn.fhir.context.RuntimeCompositeDatatypeDefinition;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.Test;
import scala.collection.JavaConverters;

public class R4DatatypeMappingsTest {

  private final static FhirContext fhirContext = FhirContext.forR4();

  // As defined in: https://www.hl7.org/fhir/datatypes.html#open
  private final static List<String> ALLOWED_R4_OPEN_ELEMENT_TYPES = Arrays.asList(
      "base64Binary",
      "boolean",
      "canonical",
      "code",
      "date",
      "dateTime",
      "decimal",
      "id",
      "instant",
      "integer",
      "markdown",
      "oid",
      "positiveInt",
      "string",
      "time",
      "unsignedInt",
      "uri",
      "url",
      "uuid",
      "Address",
      "Age",
      "Annotation",
      "Attachment",
      "CodeableConcept",
      "Coding",
      "ContactPoint",
      "Count",
      "Distance",
      "Duration",
      "HumanName",
      "Identifier",
      "Money",
      "Period",
      "Quantity",
      "Range",
      "Ratio",
      "Reference",
      "SampledData",
      "Signature",
      "Timing",
      "ContactDetail",
      "Contributor",
      "DataRequirement",
      "Expression",
      "ParameterDefinition",
      "RelatedArtifact",
      "TriggerDefinition",
      "UsageContext",
      "Dosage",
      "Meta");

  private final DataTypeMappings dataTypeMappings = new R4DataTypeMappings();

  @Test
  public void testCorrectChoicesOnOpenElementType() {
    final ElementCtx<Object, Object> ex = ElementCtx.forExtension(fhirContext);
    final RuntimeChildExtension extension = (RuntimeChildExtension) ex.childDefinition();
    final RuntimeCompositeDatatypeDefinition extensionChild = (RuntimeCompositeDatatypeDefinition) extension
        .getChildByName("extension");

    // This is how we can access the 'value' child od RuntimeChildExtension.
    final RuntimeChildChoiceDefinition openTypeElement = (RuntimeChildChoiceDefinition) extensionChild
        .getChildByName("value[x]");

    final Set<String> actualOpenElementUniqueChildNames = JavaConverters
        .seqAsJavaList(dataTypeMappings.getValidChoiceTypes(openTypeElement)).stream()
        .map(openTypeElement::getChildNameByDatatype)
        .collect(Collectors.toUnmodifiableSet());

    final Set<String> expectedOpenElementTypes = ALLOWED_R4_OPEN_ELEMENT_TYPES.stream()
        .map(typeName -> "value" + StringUtils.capitalize(typeName))
        .collect(Collectors.toUnmodifiableSet());

    assertEquals(expectedOpenElementTypes, actualOpenElementUniqueChildNames);
  }
}
