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

package au.csiro.pathling.encoders2

import au.csiro.pathling.encoders.EncoderContext
import au.csiro.pathling.encoders.datatypes.DataTypeMappings
import au.csiro.pathling.encoders2.SchemaVisitor.isCollection
import ca.uhn.fhir.context._


/**
 * Default implementation of [[SchemaProcessor]] delegating relevant functionality to [[DataTypeMappings]]
 *
 * @tparam DT the type which represents the final result of traversing a resource (or composite), e.g: for a schema converter this can be [[org.apache.spark.sql.types.DataType]].
 * @tparam SF the type which represents the result of traversing an element of a composite, e.g: for a schema converter this can be [[org.apache.spark.sql.types.StructField]].
 */
trait SchemaProcessorWithTypeMappings[DT, SF] extends SchemaProcessor[DT, SF] with EncoderContext {

  override def shouldExpandChild(definition: BaseRuntimeElementCompositeDefinition[_], childDefinition: BaseRuntimeChildDefinition): Boolean = {

    // do not expand extensions as they require custom handling
    val expandExtension = !childDefinition.isInstanceOf[RuntimeChildExtension]
    expandExtension && !dataTypeMappings.skipField(definition, childDefinition)
  }

  override def buildValue(childDefinition: BaseRuntimeChildDefinition, elementDefinition: BaseRuntimeElementDefinition[_], elementName: String): Seq[SF] = {
    val value = if (isCollection(childDefinition)) {
      buildArrayValue(childDefinition, elementDefinition, elementName)
    } else {
      buildSimpleValue(childDefinition, elementDefinition, elementName)
    }
    Seq(buildElement(elementName, value, elementDefinition))
  }

  /**
   * Builds the representation of a singular element.
   *
   * @param childDefinition   the element child definition.
   * @param elementDefinition the element definition.
   * @param elementName       the element name.
   * @return the representation of the singular element.
   */
  def buildSimpleValue(childDefinition: BaseRuntimeChildDefinition, elementDefinition: BaseRuntimeElementDefinition[_], elementName: String): DT = {
    childDefinition match {
      case enumChildDefinition: RuntimeChildPrimitiveEnumerationDatatypeDefinition =>
        buildEnumPrimitive(elementDefinition.asInstanceOf[RuntimePrimitiveDatatypeDefinition],
          enumChildDefinition)
      case _ =>
        elementDefinition match {
          case composite: BaseRuntimeElementCompositeDefinition[_] => compositeBuilder(composite)
          case primitive: RuntimePrimitiveDatatypeDefinition => buildPrimitiveDatatype(primitive)
          case xhtmlHl7Org: RuntimePrimitiveDatatypeXhtmlHl7OrgDefinition => buildPrimitiveDatatypeXhtmlHl7Org(xhtmlHl7Org)
          case _ => throw new IllegalArgumentException("Cannot process element: " + elementName + " with definition: " + elementDefinition)
        }
    }
  }

  /**
   * Builds the representation of a collection element.
   *
   * @param childDefinition   the element child definition.
   * @param elementDefinition the element definition.
   * @param elementName       the element name.
   * @return the representation of a collection element.
   */
  def buildArrayValue(childDefinition: BaseRuntimeChildDefinition, elementDefinition: BaseRuntimeElementDefinition[_], elementName: String): DT

  /**
   * Builds the representation of a named element.
   *
   * @param elementName  the name of the element.
   * @param elementValue the representation of the element value.
   * @param definition   the element definition.
   * @return the representation of the named element.
   */
  def buildElement(elementName: String, elementValue: DT, definition: BaseRuntimeElementDefinition[_]): SF

  /**
   * Builds the representation of a primitive data type.
   *
   * @param primitive the primitive data type definition.
   * @return the primitive representation.
   */
  def buildPrimitiveDatatype(primitive: RuntimePrimitiveDatatypeDefinition): DT

  /**
   * Builds the representation of a xhtmlHl7Org primitive.
   *
   * @param xhtmlHl7Org the definition of the xhtmlHl7Org primitive
   * @return the representation for the primitive.
   */
  def buildPrimitiveDatatypeXhtmlHl7Org(xhtmlHl7Org: RuntimePrimitiveDatatypeXhtmlHl7OrgDefinition): DT

  /**
   * Builds the representation of an Enum primitive.
   *
   * @param enumDefinition      the enum child definition.
   * @param enumChildDefinition the enum element definition.
   * @return the representation for the primitive.
   */
  def buildEnumPrimitive(enumDefinition: RuntimePrimitiveDatatypeDefinition,
                         enumChildDefinition: RuntimeChildPrimitiveEnumerationDatatypeDefinition): DT = {
    buildPrimitiveDatatype(enumDefinition)
  }


  def buildExtensionValue(): DT = {
    val extensionNode = ExtensionSupport.extension(fhirContext)
    buildArrayValue(extensionNode.childDefinition, extensionNode.elementDefinition, extensionNode.elementName)
  }
}