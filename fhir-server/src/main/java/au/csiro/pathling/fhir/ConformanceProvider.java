/*
 * Copyright © 2018-2022, Commonwealth Scientific and Industrial Research
 * Organisation (CSIRO) ABN 41 687 119 230. Licensed under the CSIRO Open Source
 * Software Licence Agreement.
 */

package au.csiro.pathling.fhir;

import static au.csiro.pathling.fhir.OperationDefinitionProvider.SYSTEM_LEVEL_OPERATIONS;
import static au.csiro.pathling.security.OidcConfiguration.ConfigItem.AUTH_URL;
import static au.csiro.pathling.security.OidcConfiguration.ConfigItem.REVOKE_URL;
import static au.csiro.pathling.security.OidcConfiguration.ConfigItem.TOKEN_URL;
import static au.csiro.pathling.utilities.Preconditions.checkPresent;

import au.csiro.pathling.Configuration;
import au.csiro.pathling.PathlingVersion;
import au.csiro.pathling.io.ResourceReader;
import au.csiro.pathling.security.OidcConfiguration;
import ca.uhn.fhir.rest.annotation.Metadata;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.IServerConformanceProvider;
import ca.uhn.fhir.rest.server.RestfulServer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementImplementationComponent;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementKind;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestComponent;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestResourceComponent;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestResourceOperationComponent;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestSecurityComponent;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementSoftwareComponent;
import org.hl7.fhir.r4.model.CapabilityStatement.ResourceInteractionComponent;
import org.hl7.fhir.r4.model.CapabilityStatement.RestfulCapabilityMode;
import org.hl7.fhir.r4.model.CapabilityStatement.SystemInteractionComponent;
import org.hl7.fhir.r4.model.CapabilityStatement.SystemRestfulInteraction;
import org.hl7.fhir.r4.model.CapabilityStatement.TypeRestfulInteraction;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Enumerations.FHIRVersion;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.UriType;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * This class provides a customised CapabilityStatement describing the functionality of the
 * analytics server.
 *
 * @author John Grimes
 * @see <a href="https://hl7.org/fhir/R4/capabilitystatement.html">CapabilityStatement</a>
 */
@Component
@Profile("server")
@Slf4j
public class ConformanceProvider implements IServerConformanceProvider<CapabilityStatement> {

  /**
   * The base URI for canonical URIs.
   */
  public static final String URI_BASE = "https://pathling.csiro.au/fhir";

  private static final String FHIR_RESOURCE_BASE = "http://hl7.org/fhir/StructureDefinition/";
  private static final String RESTFUL_SECURITY_URI = "http://terminology.hl7.org/CodeSystem/restful-security-service";
  private static final String RESTFUL_SECURITY_CODE = "SMART-on-FHIR";
  private static final String SMART_OAUTH_URI = "http://fhir-registry.smarthealthit.org/StructureDefinition/oauth-uris";
  private static final String UNKNOWN_VERSION = "UNKNOWN";

  @Nonnull
  private final Configuration configuration;

  @Nonnull
  private final ResourceReader resourceReader;

  @Nonnull
  private final Optional<OidcConfiguration> oidcConfiguration;

  @Nonnull
  private Optional<RestfulServer> restfulServer;

  @Nonnull
  private final PathlingVersion version;

  /**
   * @param configuration a {@link Configuration} object controlling the behaviour of the capability
   * statement
   * @param oidcConfiguration a {@link OidcConfiguration} object containing configuration retrieved
   * from OIDC discovery
   * @param resourceReader a {@link ResourceReader} to use in checking which resources are
   * available
   * @param version a {@link PathlingVersion} object containing version information for the server
   */
  public ConformanceProvider(@Nonnull final Configuration configuration,
      @Nonnull final Optional<OidcConfiguration> oidcConfiguration,
      @Nonnull final ResourceReader resourceReader,
      @Nonnull final PathlingVersion version) {
    this.configuration = configuration;
    this.oidcConfiguration = oidcConfiguration;
    this.resourceReader = resourceReader;
    this.version = version;
    restfulServer = Optional.empty();
  }

  @Override
  @Metadata(cacheMillis = 0)
  public CapabilityStatement getServerConformance(
      @Nullable final HttpServletRequest httpServletRequest,
      @Nullable final RequestDetails requestDetails) {
    log.debug("Received request for server capabilities");

    final CapabilityStatement capabilityStatement = new CapabilityStatement();
    capabilityStatement.setUrl(getCapabilityUri());
    capabilityStatement.setVersion(version.getBuildVersion().orElse(UNKNOWN_VERSION));
    capabilityStatement.setTitle("Pathling FHIR API");
    capabilityStatement.setName("pathling-fhir-api");
    capabilityStatement.setStatus(PublicationStatus.ACTIVE);
    capabilityStatement.setExperimental(true);
    capabilityStatement.setPublisher("Australian e-Health Research Centre, CSIRO");
    capabilityStatement.setCopyright(
        "Dedicated to the public domain via CC0: https://creativecommons.org/publicdomain/zero/1.0/");
    capabilityStatement.setKind(CapabilityStatementKind.INSTANCE);

    final CapabilityStatementSoftwareComponent software = new CapabilityStatementSoftwareComponent(
        new StringType("Pathling"));
    software.setVersion(version.getDescriptiveVersion().orElse(UNKNOWN_VERSION));
    capabilityStatement.setSoftware(software);

    final CapabilityStatementImplementationComponent implementation =
        new CapabilityStatementImplementationComponent(
            new StringType(configuration.getImplementationDescription()));
    final Optional<String> serverBase = getServerBase(Optional.ofNullable(httpServletRequest));
    serverBase.ifPresent(implementation::setUrl);
    capabilityStatement.setImplementation(implementation);

    capabilityStatement.setFhirVersion(FHIRVersion._4_0_1);
    capabilityStatement.setFormat(Arrays.asList(new CodeType("json"), new CodeType("xml")));
    capabilityStatement.setRest(buildRestComponent());

    return capabilityStatement;
  }

  @Nonnull
  private List<CapabilityStatementRestComponent> buildRestComponent() {
    final List<CapabilityStatementRestComponent> rest = new ArrayList<>();
    final CapabilityStatementRestComponent server = new CapabilityStatementRestComponent();
    server.setMode(RestfulCapabilityMode.SERVER);
    server.setSecurity(buildSecurity());
    server.setResource(buildResources());
    server.setOperation(buildOperations());
    server.setInteraction(buildSystemLevelInteractions());
    rest.add(server);
    return rest;
  }

  @Nonnull
  private CapabilityStatementRestSecurityComponent buildSecurity() {
    final CapabilityStatementRestSecurityComponent security = new CapabilityStatementRestSecurityComponent();
    security.setCors(true);
    if (configuration.getAuth().isEnabled()) {
      final OidcConfiguration checkedConfig = checkPresent(oidcConfiguration);

      final CodeableConcept smart = new CodeableConcept(
          new Coding(RESTFUL_SECURITY_URI, RESTFUL_SECURITY_CODE, RESTFUL_SECURITY_CODE));
      smart.setText("OAuth2 using SMART-on-FHIR profile (see http://docs.smarthealthit.org)");
      security.getService().add(smart);

      final Optional<String> authUrl = checkedConfig.get(AUTH_URL);
      final Optional<String> tokenUrl = checkedConfig.get(TOKEN_URL);
      final Optional<String> revokeUrl = checkedConfig.get(REVOKE_URL);
      if (authUrl.isPresent() || tokenUrl.isPresent() || revokeUrl.isPresent()) {
        final Extension oauthUris = new Extension(SMART_OAUTH_URI);
        authUrl.ifPresent(url -> oauthUris.addExtension("authorize", new UriType(url)));
        tokenUrl.ifPresent(url -> oauthUris.addExtension("token", new UriType(url)));
        revokeUrl.ifPresent(url -> oauthUris.addExtension("revoke", new UriType(url)));
        security.addExtension(oauthUris);
      }
    }
    return security;
  }

  @Nonnull
  private List<CapabilityStatementRestResourceComponent> buildResources() {
    final List<CapabilityStatementRestResourceComponent> resources = new ArrayList<>();
    final Set<Enumerations.ResourceType> availableToRead = resourceReader
        .getAvailableResourceTypes();
    final Set<Enumerations.ResourceType> availableResourceTypes =
        availableToRead.isEmpty()
        ? EnumSet.noneOf(Enumerations.ResourceType.class)
        : EnumSet.copyOf(availableToRead);

    for (final Enumerations.ResourceType resourceType : availableResourceTypes) {
      final CapabilityStatementRestResourceComponent resource =
          new CapabilityStatementRestResourceComponent(new CodeType(resourceType.toCode()));
      resource.setProfile(FHIR_RESOURCE_BASE + resourceType.toCode());

      // Add the search operation to all resources.
      final ResourceInteractionComponent search = new ResourceInteractionComponent();
      search.setCode(TypeRestfulInteraction.SEARCHTYPE);
      resource.getInteraction().add(search);

      // Add the create and update operations to all resources.
      final ResourceInteractionComponent create = new ResourceInteractionComponent();
      final ResourceInteractionComponent update = new ResourceInteractionComponent();
      create.setCode(TypeRestfulInteraction.CREATE);
      update.setCode(TypeRestfulInteraction.UPDATE);
      resource.getInteraction().add(create);
      resource.getInteraction().add(update);

      // Add the `aggregate` operation to all resources.
      final CanonicalType aggregateOperationUri = new CanonicalType(getOperationUri("aggregate"));
      final CapabilityStatementRestResourceOperationComponent aggregateOperation =
          new CapabilityStatementRestResourceOperationComponent(new StringType("aggregate"),
              aggregateOperationUri);
      resource.addOperation(aggregateOperation);

      // Add the `fhirPath` search parameter to all resources.
      final CapabilityStatementRestResourceOperationComponent searchOperation = new CapabilityStatementRestResourceOperationComponent();
      searchOperation.setName("fhirPath");
      searchOperation.setDefinition(getOperationUri("search"));
      resource.addOperation(searchOperation);

      resources.add(resource);
    }

    // Add the read operation to the OperationDefinition resource.
    final String opDefCode = Enumerations.ResourceType.OPERATIONDEFINITION.toCode();
    final CapabilityStatementRestResourceComponent opDefResource =
        new CapabilityStatementRestResourceComponent(new CodeType(opDefCode));
    opDefResource.setProfile(FHIR_RESOURCE_BASE + opDefCode);
    final ResourceInteractionComponent readInteraction = new ResourceInteractionComponent();
    readInteraction.setCode(TypeRestfulInteraction.READ);
    opDefResource.addInteraction(readInteraction);
    resources.add(opDefResource);

    return resources;
  }

  @Nonnull
  private List<CapabilityStatementRestResourceOperationComponent> buildOperations() {
    final List<CapabilityStatementRestResourceOperationComponent> operations = new ArrayList<>();

    for (final String name : SYSTEM_LEVEL_OPERATIONS) {
      final CanonicalType operationUri = new CanonicalType(getOperationUri(name));
      final CapabilityStatementRestResourceOperationComponent operation =
          new CapabilityStatementRestResourceOperationComponent(new StringType(name),
              operationUri);
      operations.add(operation);
    }

    return operations;
  }

  @Nonnull
  private List<SystemInteractionComponent> buildSystemLevelInteractions() {
    final List<SystemInteractionComponent> interactions = new ArrayList<>();
    final SystemInteractionComponent interaction = new SystemInteractionComponent();
    interaction.setCode(SystemRestfulInteraction.BATCH);
    interactions.add(interaction);
    return interactions;
  }

  @Nonnull
  private String getCapabilityUri() {
    return URI_BASE + "/CapabilityStatement/pathling-fhir-api-" + version.getMajorVersion()
        .orElse(UNKNOWN_VERSION);
  }

  @Nonnull
  private String getOperationUri(final String name) {
    return URI_BASE + "/OperationDefinition/" + name + "-" + version.getMajorVersion()
        .orElse(UNKNOWN_VERSION);
  }

  @Override
  public void setRestfulServer(@Nullable final RestfulServer restfulServer) {
    this.restfulServer = Optional.ofNullable(restfulServer);
  }

  @Nonnull
  private Optional<String> getServerBase(
      @Nonnull final Optional<HttpServletRequest> httpServletRequest) {
    if (httpServletRequest.isEmpty() || restfulServer.isEmpty()) {
      log.warn("Attempted to get server base URL, HTTP servlet request or RestfulServer missing");
      return Optional.empty();
    } else {
      final ServletContext servletContext =
          (ServletContext) httpServletRequest.get()
              .getAttribute(RestfulServer.SERVLET_CONTEXT_ATTRIBUTE);
      return Optional.ofNullable(restfulServer.get().getServerAddressStrategy()
          .determineServerBase(servletContext, httpServletRequest.get()));
    }
  }

}
