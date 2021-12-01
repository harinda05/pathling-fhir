package au.csiro.pathling.encoders2

import au.csiro.pathling.encoders.SchemaConverter
import au.csiro.pathling.encoders.datatypes.DataTypeMappings
import au.csiro.pathling.encoders2.SchemaTraversal.isCollection
import ca.uhn.fhir.context._
import org.apache.spark.sql.types._
import org.hl7.fhir.instance.model.api.IBase

class SchemaConverterVisitor(val fhirContext: FhirContext, val dataTypeMappings: DataTypeMappings) extends
  SchemaVisitorWithTypeMappings[DataType, StructField] {

  override def buildComposite(fields: Seq[StructField], definition: BaseRuntimeElementCompositeDefinition[_]): DataType = {
    StructType(fields)
  }

  override def buildElement(elementName: String, elementType: DataType, elementDefinition: BaseRuntimeElementDefinition[_]): StructField = {
    StructField(elementName, elementType)
  }

  override def buildArrayValue(childDefinition: BaseRuntimeChildDefinition, elementDefinition: BaseRuntimeElementDefinition[_], elementName: String,
                               compositeBuilder: (SchemaVisitor[DataType, StructField], BaseRuntimeElementCompositeDefinition[_]) => DataType): DataType = {
    ArrayType(buildSimpleValue(childDefinition, elementDefinition, elementName, compositeBuilder))
  }

  override def buildPrimitiveDatatype(primitive: RuntimePrimitiveDatatypeDefinition): DataType = {
    dataTypeMappings.primitiveToDataType(primitive)
  }

  override def buildPrimitiveDatatypeNarrative: DataType = DataTypes.StringType

  override def buildPrimitiveDatatypeXhtmlHl7Org(xhtmlHl7Org: RuntimePrimitiveDatatypeXhtmlHl7OrgDefinition): DataType = DataTypes.StringType

  override def buildValue(childDefinition: BaseRuntimeChildDefinition, elementDefinition: BaseRuntimeElementDefinition[_], elementName: String,
                          compositeBuilder: (SchemaVisitor[DataType, StructField], BaseRuntimeElementCompositeDefinition[_]) => DataType): Seq[StructField] = {
    val customEncoder = dataTypeMappings.customEncoder(elementDefinition, elementName)
    customEncoder.map(_.schema2(if (isCollection(childDefinition)) Some(ArrayType(_)) else None)).getOrElse(
      super.buildValue(childDefinition, elementDefinition, elementName, compositeBuilder)
    )
  }

}

class SchemaConverter2(val fhirContext: FhirContext, val dataTypeMappings: DataTypeMappings, val maxNestingLevel: Int) extends SchemaConverter {

  private[encoders2] def compositeSchema(compositeElementDefinition: BaseRuntimeElementCompositeDefinition[_ <: IBase]): DataType = {
    // TODO: unify the traversal
    new SchemaTraversal[DataType, StructField](maxNestingLevel)
      .enterComposite(new SchemaConverterVisitor(fhirContext, dataTypeMappings), compositeElementDefinition)
  }

  override def resourceSchema(resourceDefinition: RuntimeResourceDefinition): StructType = {
    new SchemaTraversal[DataType, StructField](maxNestingLevel)
      .enterResource(new SchemaConverterVisitor(fhirContext, dataTypeMappings), resourceDefinition).asInstanceOf[StructType]
  }
}