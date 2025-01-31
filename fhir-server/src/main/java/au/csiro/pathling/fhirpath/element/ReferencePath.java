/*
 * Copyright © 2018-2022, Commonwealth Scientific and Industrial Research
 * Organisation (CSIRO) ABN 41 687 119 230. Licensed under the CSIRO Open Source
 * Software Licence Agreement.
 */

package au.csiro.pathling.fhirpath.element;

import au.csiro.pathling.fhirpath.Referrer;
import au.csiro.pathling.fhirpath.ResourcePath;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nonnull;
import org.apache.spark.sql.Column;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.hl7.fhir.r4.model.Enumerations.FHIRDefinedType;
import org.hl7.fhir.r4.model.Enumerations.ResourceType;

/**
 * Represents a FHIRPath expression which is a resource reference.
 *
 * @author John Grimes
 */
public class ReferencePath extends ElementPath implements Referrer {

  protected ReferencePath(@Nonnull final String expression, @Nonnull final Dataset<Row> dataset,
      @Nonnull final Column idColumn, @Nonnull final Optional<Column> eidColumn,
      @Nonnull final Column valueColumn, final boolean singular,
      @Nonnull final Optional<ResourcePath> currentResource,
      @Nonnull final Optional<Column> thisColumn, @Nonnull final FHIRDefinedType fhirType) {
    super(expression, dataset, idColumn, eidColumn, valueColumn, singular, currentResource,
        thisColumn, fhirType);
  }

  @Nonnull
  public Set<ResourceType> getResourceTypes() {
    if (getDefinition().isPresent()) {
      return getDefinition().get().getReferenceTypes();
    } else {
      return Collections.emptySet();
    }
  }

  @Nonnull
  public Column getReferenceColumn() {
    return Referrer.referenceColumnFor(this);
  }

  @Nonnull
  public Column getResourceEquality(@Nonnull final ResourcePath resourcePath) {
    return Referrer.resourceEqualityFor(this, resourcePath);
  }

  @Nonnull
  public Column getResourceEquality(@Nonnull final Column targetId,
      @Nonnull final Column targetCode) {
    return Referrer.resourceEqualityFor(this, targetCode, targetId);
  }

  @Nonnull
  @Override
  public Optional<ElementDefinition> getChildElement(@Nonnull final String name) {
    // We only encode the reference and display elements of the Reference type.
    if (name.equals("reference") || name.equals("display")) {
      return super.getChildElement(name);
    } else {
      return Optional.empty();
    }
  }

}
