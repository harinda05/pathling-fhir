---
sidebar_position: 5
---

# Configuration

Pathling is distributed in two forms: as a JAR file, and a Docker image. The
easiest way to configure Pathling is through environment variables.

If environment variables are problematic for your deployment, Pathling can be
also configured in a variety of other ways as supported by the Spring Boot
framework (see
[Spring Boot Reference Documentation: Externalized Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/spring-boot-features.html#boot-features-external-config))
.

## Configuration variables

### General

- `server.port` - (default: `8080`) The port which the server should bind to and
  listen for HTTP connections.
- `server.servlet.context-path` - A prefix to add to the API endpoint, e.g. a
  value of `/foo` would cause the FHIR endpoint to be changed to `/foo/fhir`.
- `pathling.implementationDescription` - (default:
  `Yet Another Pathling Server`) Controls the content of the
  `implementation.description` element within the server's
  [CapabilityStatement](https://hl7.org/fhir/R4/http.html#capabilities).
- `JAVA_TOOL_OPTIONS` - (default in Docker image: `-Xmx2g`) Allows for the
  configuration of arbitrary options on the Java VM that Pathling runs within.

Additionally, you can set any variable supported by Spring Boot, see
[Spring Boot Reference Documentation: Common Application properties](https://docs.spring.io/spring-boot/docs/current/reference/html/appendix-application-properties.html#common-application-properties)
.

### Import

- `pathling.import.allowableSources` - (default: `file:///usr/share/staging`) A
  set of URL prefixes which are allowable for use within the import operation.
  **Important note**: a trailing slash should be used in cases where an attacker
  could create an alternative URL with the same prefix, e.g. `s3://some-bucket`
  would also match `s3://some-bucket-alternative`.

### Asynchronous processing

- `pathling.async.enabled` - (default: `true`) Enables asynchronous processing
  for those operations that support it, when explicitly requested.

### Encoding

- `pathling.encoding.maxNestingLevel` - (default: `3`) Controls the maximum
  depth of nested element data that is encoded upon import. This affects certain
  elements within FHIR resources that contain recursive references, e.g.
  [QuestionnaireResponse.item](https://hl7.org/fhir/R4/questionnaireresponse.html)
  .
- `pathling.encoding.enableExtensions` - (default: `true`) Enables support for
  FHIR extensions.
- `pathling.encoding.openTypes` - (default: `boolean`,`code`,`date`,`dateTime`,
  `decimal`,`integer`,`string`,`Coding`,`CodeableConcept`,`Address`,`Identifier`
  ,`Reference`) The list of types that are encoded within open types`,`
  such as extensions. This default list was taken from the data types that are
  common to extensions found in widely-used IGs, such as the US and AU base
  profiles. In general, you will get the best query performance by encoding your
  data with the shortest possible list.

### Storage

- `pathling.storage.warehouseUrl` - (default: `file:///usr/share/warehouse`) The
  base URL at which Pathling will look for data files, and where it will save
  data received within [import](./operations/import) requests. Can be an
  [Amazon S3](https://aws.amazon.com/s3/) (`s3://`),
  [HDFS](https://hadoop.apache.org/docs/r1.2.1/hdfs_design.html) (`hdfs://`) or
  filesystem (`file://`) URL.
- `pathling.storage.databaseName` - (default: `default`) The subdirectory within
  the warehouse path used to read and write data.
- `pathling.storage.aws.anonymousAccess` - (default: `true`) Public S3 buckets
  can be accessed by default, set this to false to access protected buckets.
- `pathling.storage.aws.accessKeyId` - Authentication details for connecting to
  a protected Amazon S3 bucket.
- `pathling.storage.aws.secretAccessKey` - Authentication details for connecting
  to a protected Amazon S3 bucket.
- `pathling.storage.aws.assumedRole` - The ARN of an IAM role to be assumed
  using STS.
  See [Temporary security credentials in IAM](https://docs.aws.amazon.com/IAM/latest/UserGuide/id_credentials_temp.html)
  .

### Apache Spark

- `pathling.spark.appName` - (default: `pathling`) Controls the application name
  that Pathling will be identified as within any Spark cluster that it
  participates in.
- `pathling.spark.explainQueries` - (default: `false`) If set to true, Spark
  query plans will be written to the logs.
- `pathling.spark.cacheDatasets` - (default: `true`) This controls whether the
  built-in caching within Spark is used for resource datasets and search
  results. It may be useful to turn this off for large datasets in
  memory-constrained environments.
- `pathling.spark.compactionThreshold` - (default: `10`) When a table is
  updated, the number of partitions is checked. If the number exceeds this
  threshold, the table will be repartitioned back to the default number of
  partitions. This prevents large numbers of small updates causing poor
  subsequent query performance.

Any Spark configuration variable can be set within Pathling directly. See
[Spark Configuration](https://spark.apache.org/docs/latest/configuration.html)
for the full list.

Here are a few that you might be particularly interested in:

- `spark.master` - (default: `local[*]`) Address of the master node of an
  [Apache Spark](https://spark.apache.org/) cluster to use for processing data,
  see [Master URLs](https://spark.apache.org/docs/latest/submitting-applications.html#master-urls)
  .
- `spark.executor.memory` - (default: `1g`) The quantity of memory available for
  each child task to process data within, in the same format as JVM memory
  strings with a size unit suffix (`k`, `m`, `g` or `t`) (e.g. `512m`, `2g`).
- `spark.sql.shuffle.partitions` - (default: `2`) This option controls the
  number of data partitions used to distribute data between child tasks. This
  can be tuned to higher numbers for larger data sets. It also controls the
  granularity of requests made to the configured terminology service.

This is the default Spark configuration:

```yaml
spark:
  master: local[*]
  sql:
    adaptive:
      enabled: true
      coalescePartitions:
        enabled: true
    extensions: io.delta.sql.DeltaSparkSessionExtension
    catalog:
      spark_catalog: org.apache.spark.sql.delta.catalog.DeltaCatalog
  databricks:
    delta:
      schema:
        autoMerge:
          enabled: true
  scheduler:
    mode: FAIR
```

### Terminology service

- `pathling.terminology.enabled` - (default: `true`) Enables use of terminology
  functions within queries.
- `pathling.terminology.serverUrl` - (default:
  `https://tx.ontoserver.csiro.au/fhir`) The endpoint of the
  [FHIR terminology service](https://hl7.org/fhir/R4/terminology-service.html)
  (R4) that the server can use to resolve terminology queries.
- `pathling.terminology.socketTimeout` - (default: `60000`) The maximum period
  (in milliseconds) that the server should wait for incoming data from the
  terminology service.
- `pathling.terminology.verboseLogging` - (default: `false`) Setting this option
  to `true` will enable additional logging of the details of requests between
  the server and the terminology service.
- `pathling.terminology.authentication.enabled` - (default: `false`) Enables
  authentication for requests to the terminology service.
- `pathling.terminology.authentication.tokenEndpoint`, 
  `pathling.terminology.authentication.clientId`, 
  `pathling.terminology.authentication.clientSecret` - Authentication details
  for connecting to a terminology service that requires authentication, using
  [OAuth 2.0 client credentials flow](https://datatracker.ietf.org/doc/html/rfc6749#section-4.4).

### Authorization

- `pathling.auth.enabled` - (default: `false`) Enables authorization. If this
  option is set to `true`, `pathling.auth.issuer` and
  `pathling.auth.audience` options must also be set.
- `pathling.auth.issuer` - Configures the issuing domain for bearer tokens, e.g.
  `https://pathling.au.auth0.com/`. Must match the contents of
  the [issuer claim](https://tools.ietf.org/html/rfc7519#section-4.1.1)
  within bearer tokens.
- `pathling.auth.audience` - Configures the audience for bearer tokens, which is
  the FHIR endpoint that tokens are intended to be authorised for, e.g.
  `https://pathling.csiro.au/fhir`. Must match the contents of the
  [audience claim](https://tools.ietf.org/html/rfc7519#section-4.1.3)
  within bearer tokens.
- `pathling.auth.ga4ghPassports.patientIdSystem` - (default:
  `http://www.australiangenomics.org.au/id/study-number`) When GA4GH passport
  authentication is enabled, this option configures the
  [identifier](https://hl7.org/fhir/R4/datatypes.html#identifier) system that is
  used to identify and control access to patient data.
- `pathling.auth.ga4ghPassports.allowedVisaIssuers` - (default: `[]`) When GA4GH
  passport authentication is enabled, this option configures the list of
  endpoints that are allowed to issue visas.

### HTTP Caching

- `pathling.httpCaching.vary` - (default: `Accept`, `Accept-Encoding`, `Prefer`,
  `Authorization`) A list of values to return within the `Vary` header.
- `pathling.httpCaching.cacheableControl` - (default: `must-revalidate`,
  `max-age=1`) A list of values to return within the `Cache-Control` header, for
  cacheable responses.
- `pathling.httpCaching.uncacheableControl` - (default: `no-store`) A list of
  values to return within the `Cache-Control` header, for uncacheable responses.

### Cross-Origin Resource Sharing (CORS)

See the
[Cross-Origin Resource Sharing](https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS)
specification for more information about the meaning of the different headers
that are controlled by this configuration.

- `pathling.cors.allowedOrigins` - (default: `[empty]`) This is a
  comma-delimited list of domain names that controls which domains are permitted
  to access the server per the `Access-Control-Allow-Origin` header. The value
  `*` can be used with this parameter, but only when `pathling.auth.enabled` is
  set to false. To use wildcards when authorization is enabled, please use
  `pathling.cors.allowedOriginPatterns`.
- `pathling.cors.allowedOriginPatterns` - (default: `[empty]`) This is a
  comma-delimited list of domain names that controls which domains are permitted
  to access the server per the `Access-Control-Allow-Origin` header. It differs
  from `pathling.cors.allowedOrigins` in that it supports wildcard patterns,
  e.g. `https://*.somedomain.com`.
- `pathling.cors.allowedMethods` - (default: `OPTIONS,GET,POST`) This is a
  comma-delimited list of HTTP methods permitted via the
  `Access-Control-Allow-Methods` header.
- `pathling.cors.allowedHeaders` - (default: `Content-Type,Authorization`) This
  is a comma-delimited list of HTTP headers permitted via the
  `Access-Control-Allow-Headers` header.
- `pathling.cors.exposedHeaders` - (default: `Content-Location,X-Progress`) This
  is a comma-delimited list of HTTP headers that are permitted to be exposed via
  the `Access-Control-Expose-Headers` header.
- `pathling.cors.maxAge` - (default: `600`) Controls how long the results of a
  preflight request can be cached via the `Access-Control-Max-Age` header.

### Monitoring

- `pathling.sentryDsn` - If this variable is set, all errors will be reported to
  a
  [Sentry](https://sentry.io) service, e.g. `https://abc123@sentry.io/123456`.
- `pathling.sentryEnvironment` - If this variable is set, this will be sent as
  the environment when reporting errors to Sentry.

## Server base

There are a number of operations within the Pathling FHIR API that pass back
URLs referring back to API endpoints. The host and protocol components of these
URLs are automatically detected based upon the details of the incoming request.

In some cases it might be desirable to override the hostname and protocol,
particularly where Pathling is being hosted behind some sort of proxy. To
account for this, Pathling also supports the use of the
[X-Forwarded-Proto](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-Forwarded-Proto)
,
[X-Forwarded-Host](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-Forwarded-Host)
and `X-Forwarded-Port` headers to override the protocol, hostname and port
within URLs sent back by the API.

## Spark compatibility

Pathling can also be run directly within an Apache Spark cluster as a persistent
application.

For compatibility, Pathling runs Spark 3.3.0 (Scala 2.12), with Hadoop version
3.3.3.
