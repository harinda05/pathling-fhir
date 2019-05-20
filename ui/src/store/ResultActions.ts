/*
 * Copyright © Australian e-Health Research Centre, CSIRO. All rights reserved.
 */

import http, { AxiosPromise, CancelTokenSource } from "axios";
import { Dispatch } from "redux";

import {
  OpOutcomeError,
  opOutcomeFromJsonResponse
} from "../fhir/OperationOutcome";
import { GlobalState } from "./index";
import { Aggregation, Filter, Grouping, QueryState } from "./QueryReducer";
import {
  AggregationRequestParameter,
  FilterRequestParameter,
  GroupingRequestParameter,
  Parameter,
  Parameters
} from "../fhir/Types";
import { catchError, clearError } from "./ErrorActions";

interface SendQueryRequest {
  type: "SEND_QUERY_REQUEST";
  cancel: CancelTokenSource;
}

interface ReceiveQueryResult {
  type: "RECEIVE_QUERY_RESULT";
  result: Parameters;
  query: QueryState;
}

interface CatchQueryError {
  type: "CATCH_QUERY_ERROR";
  message: string;
  opOutcome?: OpOutcomeError;
}

interface ClearResult {
  type: "CLEAR_RESULT";
}

export type ResultAction =
  | SendQueryRequest
  | ReceiveQueryResult
  | CatchQueryError
  | ClearResult;

export const sendQueryRequest = (
  cancel: CancelTokenSource
): SendQueryRequest => ({
  type: "SEND_QUERY_REQUEST",
  cancel
});

export const receiveQueryResult = (
  result: Parameters,
  query: QueryState
): ReceiveQueryResult => ({
  type: "RECEIVE_QUERY_RESULT",
  result,
  query
});

export const catchQueryError = (
  message: string,
  opOutcome?: OpOutcomeError
): CatchQueryError => ({
  type: "CATCH_QUERY_ERROR",
  message,
  opOutcome
});

export const clearResult = () => ({ type: "CLEAR_RESULT" });

const aggregationToParam = (
  aggregation: Aggregation
): AggregationRequestParameter => {
  const param = {
    name: "aggregation",
    part: [
      {
        name: "expression",
        valueString: aggregation.expression
      }
    ]
  };
  if (aggregation.label) {
    param.part.push({
      name: "label",
      valueString: aggregation.label
    });
  }
  return param;
};

const groupingToParam = (grouping: Grouping): GroupingRequestParameter => {
  const param = {
    name: "grouping",
    part: [
      {
        name: "expression",
        valueString: grouping.expression
      }
    ]
  };
  if (grouping.label) {
    param.part.push({
      name: "label",
      valueString: grouping.label
    });
  }
  return param;
};

const filterToParam = (filter: Filter): FilterRequestParameter => ({
  name: "filter",
  valueString: filter.expression
});

/**
 * Fetches a result based on the current query within state, then dispatches the
 * relevant actions to signal either a successful or error response.
 */
export const fetchQueryResult = (fhirServer: string) => (
  dispatch: Dispatch,
  getState: () => GlobalState
): AxiosPromise => {
  const aggregations = getState().query.aggregations,
    groupings = getState().query.groupings,
    filters = getState().query.filters,
    aggregationParams: Parameter[] = aggregations.map(aggregationToParam),
    groupingParams: Parameter[] = groupings.map(groupingToParam),
    filterParams: Parameter[] = filters.map(filterToParam),
    query: Parameters = {
      resourceType: "Parameters",
      parameter: aggregationParams.concat(groupingParams).concat(filterParams)
    };

  if (aggregations.length === 0) {
    dispatch(catchQueryError("Query must have at least one aggregation."));
  }
  if (getState().error) dispatch(clearError());
  let cancel = http.CancelToken.source();
  const result = http
    .post(`${fhirServer}/$aggregate-query`, query, {
      headers: {
        "Content-Type": "application/fhir+json",
        Accept: "application/fhir+json"
      },
      cancelToken: cancel.token
    })
    .then(response => {
      if (response.data.resourceType !== "Parameters")
        throw "Response is not of type Parameters.";
      const result = response.data;
      dispatch(receiveQueryResult(result, getState().query));
      return result;
    })
    .catch(error => {
      // Don't report an error if this is a request cancellation.
      if (http.isCancel(error)) return;
      if (
        error.response &&
        error.response.headers["content-type"].includes("application/fhir+json")
      ) {
        const opOutcome = opOutcomeFromJsonResponse(error.response.data);
        dispatch(catchQueryError(opOutcome.message, opOutcome));
        dispatch(catchError(opOutcome.message, opOutcome));
      } else {
        dispatch(catchQueryError(error.message));
        dispatch(catchError(error.message));
      }
    });
  dispatch(sendQueryRequest(cancel));
  return result;
};

/**
 * Cancels any outstanding request and clears the result state.
 */
export const cancelAndClearResult = () => (
  dispatch: Dispatch,
  getState: () => GlobalState
): void => {
  const cancel = getState().result.cancel;
  if (cancel) cancel.cancel();
  dispatch(clearResult());
};
