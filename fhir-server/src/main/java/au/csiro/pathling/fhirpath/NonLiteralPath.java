/*
 * Copyright © 2018-2020, Commonwealth Scientific and Industrial Research
 * Organisation (CSIRO) ABN 41 687 119 230. Licensed under the CSIRO Open Source
 * Software Licence Agreement.
 */

package au.csiro.pathling.fhirpath;

import static au.csiro.pathling.QueryHelpers.*;
import static au.csiro.pathling.utilities.Strings.randomShortString;

import au.csiro.pathling.fhirpath.element.ElementDefinition;
import au.csiro.pathling.utilities.Preconditions;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import lombok.Getter;
import org.apache.spark.sql.Column;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.functions;
import org.apache.spark.sql.types.DataTypes;

/**
 * Represents any FHIRPath expression which is not a literal.
 *
 * @author John Grimes
 */
@Getter
public abstract class NonLiteralPath implements FhirPath {

  @Nonnull
  protected final String expression;

  @Nonnull
  protected final Dataset<Row> dataset;

  @Nonnull
  protected final Optional<Column> idColumn;

  @Nonnull
  protected final Optional<Column> eidColumn;

  @Nonnull
  protected final Column valueColumn;

  protected final boolean singular;

  /**
   * Returns an expression representing a resource (other than the subject resource) that this path
   * originated from. This is used in {@code reverseResolve} for joining between the subject
   * resource and a reference within a foreign resource.
   */
  @Nonnull
  protected Optional<ResourcePath> foreignResource;

  /**
   * For paths that traverse from the {@code $this} keyword, this column refers to the values in the
   * collection. This is so that functions that operate over collections can construct a result that
   * is based on the original input using the argument alone, without having to join from the input
   * to the argument (which has problems relating to the generation of duplicate rows).
   */
  @Nonnull
  protected Optional<Column> thisColumn;

  protected NonLiteralPath(@Nonnull final String expression, @Nonnull final Dataset<Row> dataset,
      @Nonnull final Optional<Column> idColumn, @Nonnull final Optional<Column> eidColumn,
      @Nonnull final Column valueColumn,
      final boolean singular, @Nonnull final Optional<ResourcePath> foreignResource,
      @Nonnull final Optional<Column> thisColumn) {

    // precondition: singular paths should have empty eidColumn
    Preconditions.check(!singular || eidColumn.isEmpty());


    this.expression = expression;
    this.singular = singular;
    this.foreignResource = foreignResource;

    final String hash = randomShortString();
    final String idColumnName = hash + ID_COLUMN_SUFFIX;
    final String eidColumnName = hash + EID_COLUMN_SUFFIX;
    final String valueColumnName = hash + VALUE_COLUMN_SUFFIX;
    final String thisColumnName = hash + THIS_COLUMN_SUFFIX;

    Dataset<Row> hashedDataset = dataset;
    if (idColumn.isPresent()) {
      hashedDataset = dataset.withColumn(idColumnName, idColumn.get());
    }
    if (eidColumn.isPresent()) {
      hashedDataset = hashedDataset.withColumn(eidColumnName, eidColumn.get());
    }

    if (thisColumn.isPresent()) {
      hashedDataset = hashedDataset.withColumn(thisColumnName, thisColumn.get());
    }

    if (idColumn.isPresent()) {
      this.idColumn = Optional.of(hashedDataset.col(idColumnName));
    } else {
      this.idColumn = Optional.empty();
    }

    if (eidColumn.isPresent()) {
      this.eidColumn = Optional.of(hashedDataset.col(eidColumnName));
    } else {
      this.eidColumn = Optional.empty();
    }

    if (thisColumn.isPresent()) {
      this.thisColumn = Optional.of(hashedDataset.col(thisColumnName));
    } else {
      this.thisColumn = Optional.empty();
    }

    hashedDataset = hashedDataset.withColumn(valueColumnName, valueColumn);
    this.valueColumn = hashedDataset.col(valueColumnName);

    // @TODO: EID OPT
    // ONLY SELECT THE FIELDS STARTING WITH CURRENRT HASH
    this.dataset = applySelection(hashedDataset, this.idColumn);
  }

  @Override
  public boolean hasOrder() {
    return isSingular() || eidColumn.isPresent();
  }

  @Nonnull
  @Override
  public Dataset<Row> getOrderedDataset() {
    Preconditions.checkState(hasOrder(), "Orderable path expected");
    return eidColumn.map(c -> getDataset().orderBy(c)).orElse(getDataset());
  }

  @Nonnull
  @Override
  public Column getOrderingColumn() {
    Preconditions.checkState(hasOrder(), "Orderable path expected");
    return eidColumn.orElse(functions.lit(null))
        .cast(DataTypes.createArrayType(DataTypes.IntegerType));
  }

  /**
   * Returns the specified child of this path, if there is one.
   *
   * @param name The name of the child element
   * @return an {@link ElementDefinition} object
   */
  @Nonnull
  public abstract Optional<ElementDefinition> getChildElement(@Nonnull final String name);

  /**
   * Creates a copy of this NonLiteralPath with an updated {@link Dataset}, ID and value {@link
   * Column}s.
   *
   * @param expression an updated expression to describe the new NonLiteralPath
   * @param dataset the new Dataset that can be used to evaluate this NonLiteralPath against data
   * @param idColumn the new resource identity column
   * @param eidColumn the new element identity column
   * @param valueColumn the new expression value column
   * @param singular the new singular value
   * @param thisColumn a column containing the collection being iterated, for cases where a path is
   * being created to represent the {@code $this} keyword
   * @return a new instance of NonLiteralPath
   */
  @Nonnull
  public abstract NonLiteralPath copy(@Nonnull String expression, @Nonnull Dataset<Row> dataset,
      @Nonnull Optional<Column> idColumn, @Nonnull Optional<Column> eidColumn,
      @Nonnull Column valueColumn, boolean singular,
      @Nonnull Optional<Column> thisColumn);

  /**
   * Gets a this {@link Column} from any of the inputs, if there is one.
   *
   * @param inputs a collection of objects
   * @return a {@link Column}, if one was found
   */
  @Nonnull
  public static Optional<Column> findThisColumn(@Nonnull final Object... inputs) {
    return Stream.of(inputs)
        .filter(input -> input instanceof NonLiteralPath)
        .map(path -> (NonLiteralPath) path)
        .filter(path -> path.getThisColumn().isPresent())
        .findFirst()
        .flatMap(NonLiteralPath::getThisColumn);
  }


  /**
   * Gets a this {@link Column} from any of the inputs, if there is one.
   *
   * @param inputs a collection of objects
   * @return a {@link Column}, if one was found
   */
  @Nonnull
  public static Optional<Column> findEidColumn(@Nonnull final Object... inputs) {
    return Stream.of(inputs)
        .filter(input -> input instanceof NonLiteralPath)
        .map(path -> (NonLiteralPath) path)
        .filter(path -> path.getEidColumn().isPresent())
        .findFirst()
        .flatMap(NonLiteralPath::getEidColumn);
  }

  /**
   * Constructs a this column for this path as a structure with two fields `eid` and `value`.
   *
   * @return this column.
   */
  @Nonnull
  public Column makeThisColumn() {
    // Construct this based on input value and eid
    // It is important to alias this column here as it is passed without
    // renaming through {@link NonLiteralPath} constructor.
    final String hash = randomShortString();
    return functions.struct(
        //functions.monotonically_increasing_id().alias("uuid"),
        getOrderingColumn().alias("eid"),
        getValueColumn().alias("value")).alias(hash + THIS_COLUMN_SUFFIX);
  }

  @Nonnull
  public Column expandEid(Column indexColumn, Column valueColumn) {
    final Column eidColumn = getOrderingColumn();
    return functions.when(valueColumn.isNull(),
        functions.lit(null).cast(DataTypes.createArrayType(DataTypes.IntegerType)))
        .otherwise(
            functions.when(eidColumn.isNull(), functions.array(indexColumn)).otherwise(
                functions.concat(eidColumn, functions.array(indexColumn))
            )
        );
  }


}
