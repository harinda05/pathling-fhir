/*
 * Copyright © 2018-2022, Commonwealth Scientific and Industrial Research
 * Organisation (CSIRO) ABN 41 687 119 230. Licensed under the CSIRO Open Source
 * Software Licence Agreement.
 */

package au.csiro.pathling.update;

import au.csiro.pathling.async.AsyncSupported;
import au.csiro.pathling.security.OperationAccess;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Parameters;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Enables the bulk import of data into the server.
 *
 * @author John Grimes
 */
@Component
@Profile("server")
@Slf4j
public class ImportProvider {

  @Nonnull
  private final ImportExecutor executor;

  /**
   * @param executor An {@link ImportExecutor} to use in executing import requests
   */
  public ImportProvider(@Nonnull final ImportExecutor executor) {
    this.executor = executor;
  }

  /**
   * Accepts a request of type `application/fhir+ndjson` and overwrites the warehouse tables with
   * the contents. Does not currently support any sort of incremental update or appending to the
   * warehouse tables.
   * <p>
   * Each input will be treated as a file containing only one type of resource type. Bundles are not
   * currently given any special treatment. Each resource type is assumed to appear in the list only
   * once - multiple occurrences will result in the last input overwriting the previous ones.
   *
   * @param parameters A FHIR {@link Parameters} object describing the import request
   * @param request the {@link HttpServletRequest} details
   * @param requestDetails the {@link RequestDetails} containing HAPI inferred info
   * @param response the {@link HttpServletResponse} response
   * @return A FHIR {@link OperationOutcome} resource describing the result
   */
  @Operation(name = "$import")
  @SuppressWarnings("UnusedReturnValue")
  @OperationAccess("import")
  @AsyncSupported
  public OperationOutcome importOperation(@ResourceParam final Parameters parameters,
      @SuppressWarnings("unused") @Nullable final HttpServletRequest request,
      @SuppressWarnings("unused") @Nullable final RequestDetails requestDetails,
      @SuppressWarnings("unused") @Nullable final HttpServletResponse response) {
    return executor.execute(parameters);
  }

}
