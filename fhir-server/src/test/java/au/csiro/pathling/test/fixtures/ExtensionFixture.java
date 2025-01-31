/*
 * Copyright © 2018-2022, Commonwealth Scientific and Industrial Research
 * Organisation (CSIRO) ABN 41 687 119 230. Licensed under the CSIRO Open Source
 * Software Licence Agreement.
 */

package au.csiro.pathling.test.fixtures;

import static au.csiro.pathling.utilities.Preconditions.checkPresent;

import au.csiro.pathling.fhirpath.ResourcePath;
import au.csiro.pathling.test.helpers.SparkHelpers;
import au.csiro.pathling.test.helpers.SparkHelpers.IdAndValueColumns;
import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.spark.sql.Column;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;

public interface ExtensionFixture {

  Row MANY_EXT_ROW_1 = RowFactory
      .create("ext3", "uuid:myExtension", "string3", 102);
  Row MANY_EXT_ROW_2 = RowFactory
      .create("ext4", "uuid:myExtension", "string4", 103);

  List<Row> MANY_MY_EXTENSIONS = Arrays.asList(
      RowFactory.create("ext1", "uuid:someExtension", "string1", 100),
      RowFactory.create("ext2", "uuid:someExtension", "string2", 101),
      MANY_EXT_ROW_1,
      MANY_EXT_ROW_2
  );

  Row ONE_EXT_ROW_1 = RowFactory
      .create("ext2", "uuid:myExtension", "string2", 201);

  List<Row> ONE_MY_EXTENSION = Arrays.asList(
      RowFactory.create("ext1", "uuid:otherExtension", "string1", 200),
      ONE_EXT_ROW_1
  );
  List<Row> NO_MY_EXTENSIONS = Arrays.asList(
      RowFactory.create("ext1", "uuid:otherExtension", "string1", 300),
      RowFactory.create("ext2", "uuid:someExtension", "string2", 301)
  );

  static Map<Object, Object> oneEntryMap(final int i, final List<Row> manyMyExtensions) {
    return ImmutableMap.builder().put(i, manyMyExtensions).build();
  }

  static Map<Object, Object> nullEntryMap(final int key) {
    final Map<Object, Object> nullEntryMap = new HashMap<>();
    nullEntryMap.put(key, null);
    return nullEntryMap;
  }

  static Dataset<Row> toElementDataset(final Dataset<Row> resourceLikeDataset,
      final ResourcePath baseResourcePath) {
    // Construct element dataset from the resource dataset so that the resource path
    // can be used as the current resource for this element path.

    final IdAndValueColumns idAndValueColumns = SparkHelpers
        .getIdAndValueColumns(resourceLikeDataset, true);
    final Dataset<Row> resourceDataset = baseResourcePath.getDataset();
    final Column[] elementColumns = Stream.of(
            idAndValueColumns.getId().named().name(),
            checkPresent(idAndValueColumns.getEid()).named().name(),
            idAndValueColumns.getValues().get(0).named().name(), "_extension")
        .map(baseResourcePath::getElementColumn)
        .toArray(Column[]::new);

    return resourceDataset.select(elementColumns);
  }

}
