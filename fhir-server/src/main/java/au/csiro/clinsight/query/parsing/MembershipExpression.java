/*
 * Copyright © Australian e-Health Research Centre, CSIRO. All rights reserved.
 */

package au.csiro.clinsight.query.parsing;

import static au.csiro.clinsight.fhir.definitions.ResolvedElement.ResolvedElementType.PRIMITIVE;
import static au.csiro.clinsight.query.QueryWrangling.convertUpstreamLateralViewsToInlineQueries;
import static au.csiro.clinsight.query.parsing.Join.JoinType.EXISTS_JOIN;
import static au.csiro.clinsight.query.parsing.ParseResult.ParseResultType.COLLECTION;

import java.util.SortedSet;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * An expression (identified by the "in" keyword) that tests whether the expression on the left-hand
 * side is in the collection described by the expression on the right hand side.
 *
 * @author John Grimes
 */
public class MembershipExpression {

  @Nonnull
  public ParseResult invoke(@Nullable ParseResult left, @Nonnull ParseResult right) {
    // Build a select expression which tests whether there is a code on the right-hand side of the
    // left join, returning a boolean.
    String resourceTable = (String) right.getFromTables().toArray()[0];
    String selectExpression =
        "SELECT " + resourceTable + ".id, IFNULL(MAX(" + right.getSqlExpression()
            + " = " + left.getSqlExpression() + "), FALSE) AS result";

    // Add the new join to the joins from the input, and convert any lateral views to inline
    // queries.
    SortedSet<Join> subqueryJoins = convertUpstreamLateralViewsToInlineQueries(right.getJoins());

    // Convert the set of views into an inline query. This is necessary due to the fact that we have
    // two levels of aggregation, one to aggregate possible multiple codes into a single exists or
    // not boolean expression, and the second to perform the requested aggregations across any
    // groupings (e.g. counting).
    String joinAlias = right.getJoins().last().getTableAlias() + "Membership";
    String joinExpressions = subqueryJoins.stream().map(Join::getExpression)
        .collect(Collectors.joining(" "));
    String existsJoinExpression =
        "LEFT JOIN (" + selectExpression + " FROM " + resourceTable + " " + joinExpressions
            + " GROUP BY 1) " + joinAlias + " ON " + resourceTable + ".id = "
            + joinAlias + ".id";
    String existsSelect = joinAlias + ".result";

    // Clear the old joins out of the right-hand expression and replace them with the new join to
    // the inline query.
    Join existsJoin = new Join(existsJoinExpression, joinAlias, EXISTS_JOIN, joinAlias);
    right.getJoins().clear();
    right.getJoins().add(existsJoin);
    right.setResultType(COLLECTION);
    right.setElementType(PRIMITIVE);
    right.setElementTypeCode("boolean");
    right.setSqlExpression(existsSelect);
    return right;
  }

}
