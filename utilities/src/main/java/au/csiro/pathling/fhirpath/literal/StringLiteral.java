package au.csiro.pathling.fhirpath.literal;

import javax.annotation.Nonnull;

public abstract class StringLiteral {

  /**
   * On the way back out, we only do the minimal escaping to guarantee syntactical correctness.
   *
   * @param value the value to apply escaping to
   * @return the escaped result
   */
  @Nonnull
  public static String escapeFhirPathString(@Nonnull final String value) {
    return value.replace("'", "\\'");
  }

  /**
   * This method implements the rules for dealing with strings in the FHIRPath specification.
   *
   * @param value the string to be unescaped
   * @return the unescaped result
   * @see <a href="https://hl7.org/fhirpath/index.html#string">String</a>
   */
  @Nonnull
  public static String unescapeFhirPathString(@Nonnull String value) {
    value = value.replaceAll("\\\\/", "/");
    value = value.replaceAll("\\\\f", "\u000C");
    value = value.replaceAll("\\\\n", "\n");
    value = value.replaceAll("\\\\r", "\r");
    value = value.replaceAll("\\\\t", "\u0009");
    value = value.replaceAll("\\\\`", "`");
    value = value.replaceAll("\\\\'", "'");
    return value.replaceAll("\\\\\\\\", "\\\\");
  }

}
