package com.hansecom.monitoringservice.rest.inbound;

import static java.util.stream.StreamSupport.stream;

import com.hansecom.monitoringservice.exception.StandardException;
import com.hansecom.monitoringservice.rest.inbound.openapi.model.InvalidParamDetail;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.validation.ValidationException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Class with logic to handle errors and respond according to RFC 7807 responses.
 *
 * @see org.springframework.http.ProblemDetail
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  /**
   * Handles StandardException.
   *
   * @param ex the exception to handle
   * @param req the http request
   * @return error response
   */
  @ExceptionHandler(StandardException.class)
  public ResponseEntity<Object> handleStandardException(
      final StandardException ex, final HttpServletRequest req) {

    if (ex.isTech()) {
      log.error("Controlled error", ex);
    }

    return handleErrorResponse(ex, req);
  }

  /**
   * Handles all other exception that are not being handled.
   *
   * @param ex the exception to handle
   * @return error response
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<Object> handleException(final Exception ex, final HttpServletRequest req) {

    log.error("Unexpected error", ex);

    return handleErrorResponse(StandardException.internalServerError(ex), req);
  }

  /**
   * Handles ErrorResponseException.
   *
   * @param ex the exception to handle
   * @param req the http request
   * @return error response
   */
  @ExceptionHandler(ErrorResponseException.class)
  public ResponseEntity<Object> handleResponseStatusException(
      final ErrorResponseException ex, final HttpServletRequest req) {

    log.error("Response error", ex);

    return handleErrorResponse(StandardException.problemWithRequest(ex), req);
  }

  /**
   * Handles request validation exceptions.
   *
   * @param ex the exception to handle
   * @param req the http request
   * @return error response
   */
  @ExceptionHandler({
    ValidationException.class,
    HttpMessageConversionException.class,
    ServletException.class,
    MethodArgumentTypeMismatchException.class
  })
  public ResponseEntity<Object> handleInvalidRequests(
      final Exception ex, final HttpServletRequest req) {

    log.warn("Validation error", ex);

    List<InvalidParamDetail> invalidParamDetail = null;

    if (ex instanceof final ConstraintViolationException constraintViolationException) {

      invalidParamDetail =
          Stream.ofNullable(constraintViolationException.getConstraintViolations())
              .flatMap(Collection::stream)
              .map(
                  constraintViolation -> {
                    final String originalName =
                        Optional.ofNullable(constraintViolation.getPropertyPath())
                            .map(Path::toString)
                            .orElse("");

                    final Optional<Path.Node> variableNode =
                        stream(constraintViolation.getPropertyPath().spliterator(), false)
                            .reduce((previous, next) -> next);

                    final String variableName =
                        variableNode.isPresent() ? variableNode.get().getName() : originalName;

                    return new InvalidParamDetail()
                        .name(variableName)
                        .reason(constraintViolation.getMessage());
                  })
              .toList();
    }

    if (ex
        instanceof final MethodArgumentTypeMismatchException methodArgumentTypeMismatchException) {
      invalidParamDetail =
          List.of(
              new InvalidParamDetail()
                  .name(methodArgumentTypeMismatchException.getName())
                  .reason(methodArgumentTypeMismatchException.getMessage()));
    }

    return handleErrorResponse(StandardException.invalidRequestParams(ex, invalidParamDetail), req);
  }

  /**
   * Create the error response and send some metrics.
   *
   * @param error the error
   * @param req the http request
   * @return the error response
   */
  private ResponseEntity<Object> handleErrorResponse(
      final StandardException error, final HttpServletRequest req) {

    // error metrics should go here

    return new ResponseEntity<>(error.getProblemDetail(), error.getHttpStatus());
  }
}
