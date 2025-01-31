/*
 * Copyright © 2018-2022, Commonwealth Scientific and Industrial Research
 * Organisation (CSIRO) ABN 41 687 119 230. Licensed under the CSIRO Open Source
 * Software Licence Agreement.
 */

package au.csiro.pathling.fhirpath.literal;

import static au.csiro.pathling.fhirpath.literal.StringLiteral.escapeFhirPathString;
import static au.csiro.pathling.fhirpath.literal.StringLiteral.unescapeFhirPathString;
import static au.csiro.pathling.utilities.Preconditions.check;
import static au.csiro.pathling.utilities.Strings.unSingleQuote;

import au.csiro.pathling.fhirpath.Comparable;
import au.csiro.pathling.fhirpath.FhirPath;
import au.csiro.pathling.fhirpath.Materializable;
import au.csiro.pathling.fhirpath.element.StringPath;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nonnull;
import lombok.Getter;
import org.apache.spark.sql.Column;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.hl7.fhir.r4.model.Enumerations.FHIRDefinedType;
import org.hl7.fhir.r4.model.PrimitiveType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Type;

/**
 * Represents a FHIRPath string literal.
 *
 * @author John Grimes
 */
@Getter
public class StringLiteralPath extends LiteralPath implements Materializable<PrimitiveType>,
    Comparable {

  protected StringLiteralPath(@Nonnull final Dataset<Row> dataset, @Nonnull final Column idColumn,
      @Nonnull final Type literalValue) {
    super(dataset, idColumn, literalValue);
    check(literalValue instanceof PrimitiveType);
  }

  /**
   * Returns a new instance, parsed from a FHIRPath literal.
   *
   * @param fhirPath The FHIRPath representation of the literal
   * @param context An input context that can be used to build a {@link Dataset} to represent the
   * literal
   * @return A new instance of {@link LiteralPath}
   */
  @Nonnull
  public static StringLiteralPath fromString(@Nonnull final String fhirPath,
      @Nonnull final FhirPath context) {
    // Remove the surrounding single quotes and unescape the string according to the rules within
    // the FHIRPath specification.
    String value = unSingleQuote(fhirPath);
    value = unescapeFhirPathString(value);

    return new StringLiteralPath(context.getDataset(), context.getIdColumn(),
        new StringType(value));
  }

  @Nonnull
  @Override
  public String getExpression() {
    return "'" + escapeFhirPathString(getLiteralValue().getValueAsString()) + "'";
  }

  @Nonnull
  @Override
  public PrimitiveType getLiteralValue() {
    return (PrimitiveType) literalValue;
  }

  @Nonnull
  @Override
  public String getJavaValue() {
    return getLiteralValue().getValueAsString();
  }

  @Override
  @Nonnull
  public Function<Comparable, Column> getComparison(@Nonnull final ComparisonOperation operation) {
    return Comparable.buildComparison(this, operation.getSparkFunction());
  }

  @Override
  public boolean isComparableTo(@Nonnull final Class<? extends Comparable> type) {
    return StringPath.getComparableTypes().contains(type);
  }

  @Nonnull
  @Override
  public Optional<PrimitiveType> getValueFromRow(@Nonnull final Row row, final int columnNumber) {
    return StringPath.valueFromRow(row, columnNumber, FHIRDefinedType.STRING);
  }

  @Override
  public boolean canBeCombinedWith(@Nonnull final FhirPath target) {
    return super.canBeCombinedWith(target) || target instanceof StringPath;
  }

}
