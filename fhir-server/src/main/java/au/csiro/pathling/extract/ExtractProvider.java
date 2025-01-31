/*
 * Copyright © 2018-2022, Commonwealth Scientific and Industrial Research
 * Organisation (CSIRO) ABN 41 687 119 230. Licensed under the CSIRO Open Source
 * Software Licence Agreement.
 */

package au.csiro.pathling.extract;

import static au.csiro.pathling.fhir.FhirServer.resourceTypeFromClass;
import static au.csiro.pathling.utilities.Preconditions.checkNotNull;

import au.csiro.pathling.async.AsyncSupported;
import au.csiro.pathling.security.OperationAccess;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.IResourceProvider;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Enumerations.ResourceType;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Parameters;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * HAPI resource provider that provides an entry point for the "extract" type-level operation.
 *
 * @author John Grimes
 * @see <a href="https://pathling.csiro.au/docs/extract.html">Extract</a>
 */
@Component
@Scope("prototype")
@Profile("server")
public class ExtractProvider implements IResourceProvider {

  @Nonnull
  private final ExtractExecutor extractExecutor;

  @Nonnull
  private final Class<? extends IBaseResource> resourceClass;

  @Nonnull
  private final ResourceType resourceType;

  /**
   * @param extractExecutor an instance of {@link ExtractExecutor} to process requests
   * @param resourceClass the resource class that this provider will receive requests for
   */
  public ExtractProvider(@Nonnull final ExtractExecutor extractExecutor,
      @Nonnull final Class<? extends IBaseResource> resourceClass) {
    this.extractExecutor = extractExecutor;
    this.resourceClass = resourceClass;
    resourceType = resourceTypeFromClass(resourceClass);
  }

  @Override
  public Class<? extends IBaseResource> getResourceType() {
    return resourceClass;
  }

  /**
   * Extended FHIR operation: "extract".
   *
   * @param column a list of column expressions
   * @param filter a list of filter expressions
   * @param limit a maximum number of rows to return
   * @param request the {@link HttpServletRequest} details
   * @param requestDetails the {@link RequestDetails} containing HAPI inferred info
   * @param response the {@link HttpServletResponse} response
   * @return {@link Parameters} object representing the result
   */
  @Operation(name = "$extract", idempotent = true)
  @AsyncSupported
  public Parameters extract(
      @Nullable @OperationParam(name = "column") final List<String> column,
      @Nullable @OperationParam(name = "filter") final List<String> filter,
      @Nullable @OperationParam(name = "limit") final IntegerType limit,
      @SuppressWarnings("unused") @Nullable final HttpServletRequest request,
      @SuppressWarnings("unused") @Nullable final RequestDetails requestDetails,
      @SuppressWarnings("unused") @Nullable final HttpServletResponse response) {
    return invoke(column, filter, limit, requestDetails);
  }

  @OperationAccess("extract")
  private Parameters invoke(@Nullable final List<String> column,
      @Nullable final List<String> filter, @Nullable final IntegerType limit,
      @Nullable final RequestDetails requestDetails) {
    checkNotNull(requestDetails);

    final String requestId = requestDetails.getRequestId();
    final String resultId = requestId != null
                            ? requestId
                            : UUID.randomUUID().toString();

    final ExtractRequest query = new ExtractRequest(resourceType, Optional.ofNullable(column),
        Optional.ofNullable(filter), Optional.ofNullable(limit).map(IntegerType::getValue),
        resultId);
    final ExtractResponse result = extractExecutor.execute(query,
        requestDetails.getFhirServerBase());

    return result.toParameters();
  }

}
