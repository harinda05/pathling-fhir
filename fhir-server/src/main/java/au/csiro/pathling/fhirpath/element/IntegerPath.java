/*
 * Copyright © 2018-2020, Commonwealth Scientific and Industrial Research
 * Organisation (CSIRO) ABN 41 687 119 230. Licensed under the CSIRO Open Source
 * Software Licence Agreement.
 */

package au.csiro.pathling.fhirpath.element;

import au.csiro.pathling.errors.InvalidUserInputError;
import au.csiro.pathling.fhirpath.Comparable;
import au.csiro.pathling.fhirpath.Materializable;
import au.csiro.pathling.fhirpath.NonLiteralPath;
import au.csiro.pathling.fhirpath.Numeric;
import au.csiro.pathling.fhirpath.literal.DecimalLiteralPath;
import au.csiro.pathling.fhirpath.literal.IntegerLiteralPath;
import au.csiro.pathling.fhirpath.literal.NullLiteralPath;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nonnull;
import org.apache.spark.sql.Column;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.types.LongType;
import org.hl7.fhir.r4.model.Enumerations.FHIRDefinedType;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.PositiveIntType;
import org.hl7.fhir.r4.model.PrimitiveType;
import org.hl7.fhir.r4.model.UnsignedIntType;

/**
 * Represents a FHIRPath expression which refers to an integer typed element.
 *
 * @author John Grimes
 */
public class IntegerPath extends ElementPath implements Materializable<PrimitiveType>, Comparable,
    Numeric {

  private static final ImmutableSet<Class<? extends Comparable>> COMPARABLE_TYPES = ImmutableSet
      .of(IntegerPath.class, IntegerLiteralPath.class, DecimalPath.class, DecimalLiteralPath.class,
          NullLiteralPath.class);

  /**
   * @param expression The FHIRPath representation of this path
   * @param dataset A {@link Dataset} that can be used to evaluate this path against data
   * @param idColumn A {@link Column} within the dataset containing the identity of the subject
   * resource
   * @param valueColumn A {@link Column} within the dataset containing the values of the nodes
   * @param singular An indicator of whether this path represents a single-valued collection
   * @param fhirType The FHIR datatype for this path, note that there can be more than one FHIR
   * type
   */
  public IntegerPath(@Nonnull final String expression, @Nonnull final Dataset<Row> dataset,
      @Nonnull final Optional<Column> idColumn, @Nonnull final Column valueColumn,
      final boolean singular, @Nonnull final FHIRDefinedType fhirType) {
    super(expression, dataset, idColumn, valueColumn, singular, fhirType);
  }

  @Nonnull
  @Override
  public Optional<PrimitiveType> getValueFromRow(@Nonnull final Row row, final int columnNumber) {
    if (row.isNullAt(columnNumber)) {
      return Optional.empty();
    }
    final int value;
    if (row.schema().fields()[columnNumber].dataType() instanceof LongType) {
      try {
        // Currently some functions such as count currently return an Integer type, even though
        // their return values can theoretically exceed the maximum value permitted for an integer.
        // This guard allows us to handle this situation in a safe way. In the future, we will
        // implement the "as" operator to allow the user to explicitly use a Decimal where large
        // values are possible.
        value = Math.toIntExact(row.getLong(columnNumber));
      } catch (final ArithmeticException e) {
        throw new InvalidUserInputError(
            "Attempt to return an Integer value greater than the maximum value permitted for this type");
      }
    } else {
      value = row.getInt(columnNumber);
    }
    switch (getFhirType()) {
      case UNSIGNEDINT:
        return Optional.of(new UnsignedIntType(value));
      case POSITIVEINT:
        return Optional.of(new PositiveIntType(value));
      default:
        return Optional.of(new IntegerType(value));
    }
  }

  @Nonnull
  public static ImmutableSet<Class<? extends Comparable>> getComparableTypes() {
    return COMPARABLE_TYPES;
  }

  @Override
  public Function<Comparable, Column> getComparison(final ComparisonOperation operation) {
    return Comparable.buildComparison(this, operation.getSparkFunction());
  }

  @Override
  public boolean isComparableTo(@Nonnull final Class<? extends Comparable> type) {
    return COMPARABLE_TYPES.contains(type);
  }

  @Nonnull
  @Override
  public Function<Numeric, NonLiteralPath> getMathOperation(@Nonnull final MathOperation operation,
      @Nonnull final String expression, @Nonnull final Dataset<Row> dataset) {
    return buildMathOperation(this, operation, expression, dataset, getFhirType());
  }

  /**
   * Builds a math operation result for an Integer-like path.
   *
   * @param source The left operand for the operation
   * @param operation The type of {@link au.csiro.pathling.fhirpath.Numeric.MathOperation}
   * @param expression The FHIRPath expression to use in the result
   * @param dataset The {@link Dataset} to use in the result
   * @param fhirType The {@link FHIRDefinedType} to use in the result
   * @return A {@link Function} that takes a {@link Numeric} as a parameter, and returns a {@link
   * NonLiteralPath}
   */
  @Nonnull
  public static Function<Numeric, NonLiteralPath> buildMathOperation(@Nonnull final Numeric source,
      @Nonnull final MathOperation operation, @Nonnull final String expression,
      @Nonnull final Dataset<Row> dataset, @Nonnull final FHIRDefinedType fhirType) {
    return target -> {
      Column valueColumn = operation.getSparkFunction()
          .apply(source.getValueColumn(), target.getValueColumn());
      switch (operation) {
        case ADDITION:
        case SUBTRACTION:
        case MULTIPLICATION:
        case MODULUS:
          if (target instanceof DecimalPath || target instanceof DecimalLiteralPath) {
            valueColumn = valueColumn.cast("int");
          }
          return new IntegerPath(expression, dataset, source.getIdColumn(), valueColumn, true,
              fhirType);
        case DIVISION:
          final Column numerator = source.getValueColumn().cast("decimal");
          valueColumn = operation.getSparkFunction().apply(numerator, target.getValueColumn());
          return new DecimalPath(expression, dataset, source.getIdColumn(), valueColumn, true,
              FHIRDefinedType.DECIMAL);
        default:
          throw new AssertionError("Unsupported math operation encountered: " + operation);
      }
    };
  }

}
