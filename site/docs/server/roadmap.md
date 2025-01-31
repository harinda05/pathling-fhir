---
sidebar_position: 10
---

# Roadmap

We are continually adding new features to the various different components of
Pathling. If you are interested in specific functionality that doesn't exist
yet, please [get into contact](https://pathling.csiro.au/#contact) with us
or [create an issue](https://github.com/aehrc/pathling/issues/new) to help us
understand your use case.

## Quantity support

This change will add support for Quantity types, including literals support for
equality and comparison operators. It also introduces calendar durations and
support for date arithmetic.

See also:

- [Quantity type](https://github.com/aehrc/pathling/issues/340)
- [Date/time arithmetic](https://github.com/aehrc/pathling/issues/341)

## Terminology functions: `property` and `designation`

These two functions will round out the terminology functionality of the Pathling
operations, allowing virtually any knowledge held by the terminology server
about concepts to be accessed within expressions.

See also:

- [FHIRPath function: property](https://github.com/aehrc/pathling/issues/366)
- [FHIRPath function: designation](https://github.com/aehrc/pathling/issues/519)

## Ordering

The implementation of an `order` function will allow for the arbitrary
re-ordering of resources and elements within expressions.

See [FHIRPath function: order](https://github.com/aehrc/pathling/issues/448).

## Improved FHIRPath support

Implementation of a number of functions is planned:

### Aggregate functions

- `approxCountDistinct`
- `correlation`
- `countDistinct`
- `covariance[Pop]`
- `kurtosis`
- `last`
- `max`
- `mean`
- `min`
- `percentileApprox`
- `product`
- `skewness`
- `stdDev[Pop]`
- `sumDistinct`
- `variance[Pop]`

### Regular functions

- `contains`
- `startsWith`
- `endsWith`

See [Arbitrary function construction](https://github.com/aehrc/pathling/issues/510)
.

## R integration

Language-specific APIs will be developed that will allow users of R
to access the functionality within Pathling within their own language
environment.

See [R integration](https://github.com/aehrc/pathling/issues/193).

## Subscriptions

Work is planned to implement
[FHIR Subscriptions](https://www.hl7.org/fhir/R4/subscription.html) within
Pathling. Push messaging relating to changes within the data (using criteria
described using FHIRPath expressions) could be used as an engine for driving
sophisticated alert systems within the clinical setting.

See [Subscriptions](https://github.com/aehrc/pathling/issues/164).

## Temporal query

Some types of data are captured within the FHIR model using dates and timestamps
to describe their temporal aspects. Others are updated in place, and
information about the previous value and the time of update is effectively lost
when the change is made.

This change will expand upon the work done on incremental update to add the
ability to query the history of FHIR resources as they were updated within the
Pathling data store. This will include the ability to query the state of a
resource at a point in time and compare it to other versions of that resource.

See [Temporal query](https://github.com/aehrc/pathling/issues/350).

## Project board

You can see more planned features, in greater detail, on the
[Pathling project board](https://github.com/aehrc/pathling/projects/1) on
GitHub.
