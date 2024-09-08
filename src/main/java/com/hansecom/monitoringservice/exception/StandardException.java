package com.hansecom.monitoringservice.exception;

import com.hansecom.monitoringservice.rest.inbound.openapi.model.InvalidParamDetail;
import com.hansecom.monitoringservice.rest.inbound.openapi.model.ProblemDetail;
import io.opentelemetry.api.trace.Span;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Generic exception. */
@EqualsAndHashCode(callSuper = false)
@Getter
public class StandardException extends RuntimeException {

  private static final String TYPE = "https://www.hansecom.com/errors/";

  private final ProblemDetail problemDetail;

  private final ErrorType errorType;

  private final HttpStatusCode httpStatus;

  /**
   * Constructor with cause of the exception.
   *
   * @param cause cause of the exception
   * @param problemDetail problem details
   * @param errorType error type
   * @param httpStatus http status to return
   */
  public StandardException(
      final Throwable cause,
      final ProblemDetail problemDetail,
      final ErrorType errorType,
      final HttpStatusCode httpStatus) {

    super(cause);
    this.problemDetail = problemDetail;
    this.errorType = errorType;
    this.httpStatus = httpStatus;
  }

  /**
   * Constructor with message of the exception.
   *
   * @param message message of the exception
   * @param problemDetail problem details
   * @param errorType error type
   * @param httpStatus http status to return
   */
  public StandardException(
      final String message,
      final ProblemDetail problemDetail,
      final ErrorType errorType,
      final HttpStatusCode httpStatus) {

    super(message);
    this.problemDetail = problemDetail;
    this.errorType = errorType;
    this.httpStatus = httpStatus;
  }

  /**
   * Creates a default exception for an internal server error.
   *
   * @param cause the cause of the error
   * @return the newly created {@link StandardException}
   */
  public static StandardException internalServerError(final Throwable cause) {

    HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
    String code = "internal_server_error";

    return new StandardException(
        cause,
        defaultProblemDetail(code)
            .status(httpStatus.value())
            .title("Internal error on the server")
            .code(code)
            .detail(
                "Some internal server error happen. Please provide the traceId %s to the support team."
                    .formatted(getTraceId())),
        ErrorType.TECH,
        httpStatus);
  }

  /**
   * Creates a default exception for problem with request.
   *
   * @param cause the cause of the error
   * @return the newly created {@link StandardException}
   */
  public static StandardException problemWithRequest(final ErrorResponseException cause) {

    String code = "problem_with_request";

    return new StandardException(
        cause,
        defaultProblemDetail(code)
            .status(cause.getStatusCode().value())
            .title("There is a problem with the request")
            .code(code)
            .detail(cause.getMessage()),
        ErrorType.FUNC,
        cause.getStatusCode());
  }

  /**
   * Creates a default exception for invalid request params.
   *
   * @param cause the cause of the error
   * @param invalidParamDetail array with invalid fields
   * @return the newly created {@link StandardException}
   */
  public static StandardException invalidRequestParams(
      final Exception cause, final List<InvalidParamDetail> invalidParamDetail) {

    HttpStatus httpStatus = HttpStatus.BAD_REQUEST;
    String code = "invalid_request_params";

    return new StandardException(
        cause,
        defaultProblemDetail(code)
            .status(httpStatus.value())
            .title("Your request parameters didn't validate")
            .code(code)
            .detail(extractValueFromExceptionMessage(cause))
            .invalidParams(invalidParamDetail),
        ErrorType.FUNC,
        httpStatus);
  }

  /**
   * Creates a default exception for error max jobs allowed.
   *
   * @param maxJobs max jobs allowed in the system
   * @return the newly created {@link StandardException}
   */
  public static StandardException maxJobsAllowed(final int maxJobs) {

    HttpStatus httpStatus = HttpStatus.UNPROCESSABLE_ENTITY;
    String message = "You have reach the system limit of %s jobs".formatted(maxJobs);
    String code = "max_jobs_reach";

    return new StandardException(
        message,
        defaultProblemDetail(code)
            .status(httpStatus.value())
            .title("Max jobs reached")
            .code(code)
            .detail(message),
        ErrorType.FUNC,
        httpStatus);
  }

  /**
   * Creates a default exception for error invalid date range.
   *
   * @return the newly created {@link StandardException}
   */
  public static StandardException invalidDateRange() {

    HttpStatus httpStatus = HttpStatus.BAD_REQUEST;
    String message = "Invalid date range. From must be before to and to must be after from";
    String code = "invalid_date_range";

    return new StandardException(
        message,
        defaultProblemDetail(code)
            .status(httpStatus.value())
            .title("Invalid date range")
            .code(code)
            .detail(message),
        ErrorType.FUNC,
        httpStatus);
  }

  /**
   * Creates a default exception for error job name already exists.
   *
   * @param jobName the name of the job
   * @return the newly created {@link StandardException}
   */
  public static StandardException jobNameAlreadyExists(final String jobName) {

    HttpStatus httpStatus = HttpStatus.CONFLICT;

    String message = "Job with name %s already exists".formatted(jobName);
    String code = "job_already_exists";

    return new StandardException(
        message,
        defaultProblemDetail(code)
            .status(httpStatus.value())
            .title("Job already exists")
            .code(code)
            .detail(message)
            .putAdditionalProperty("jobName", jobName),
        ErrorType.FUNC,
        httpStatus);
  }

  /**
   * Creates a {@link ProblemDetail} with default values.
   *
   * @param code code to append to the url.
   * @return the newly created {@link ProblemDetail}
   */
  public static ProblemDetail defaultProblemDetail(final String code) {
    return new ProblemDetail().type(TYPE + code).traceId(getTraceId());
  }

  /**
   * Retrieves the trace id.
   *
   * @return the trace id
   */
  private static String getTraceId() {
    return Span.current().getSpanContext().getTraceId();
  }

  /**
   * Returns if the type of the exception is technical.
   *
   * @return true if error is technical, otherwise false
   */
  public boolean isTech() {
    return ErrorType.TECH.equals(errorType);
  }

  /**
   * Normally stack traces of exceptions are concatenated in the final message with ':'. This method
   * splits the message by ':' and returns the latest, which normally is the most relevant part.
   *
   * <p>This was primarily done to remove beans and method names information from the message that
   * is returned to the user.
   *
   * <p>Rule exceptions:
   *
   * <ul>
   *   <li>MethodArgumentTypeMismatchException: in this case the last part of exception only by
   *       itself is not relevant.
   * </ul>
   *
   * @param ex the exception to extract the message of
   * @return the last part of the message after ':' or the exception message itself is criteria was
   *     not meet
   */
  private static String extractValueFromExceptionMessage(final Exception ex) {

    return Optional.of(ex)
        .filter(e -> !(e instanceof MethodArgumentTypeMismatchException))
        .map(Throwable::getMessage)
        .map(msg -> msg.split(":"))
        .map(msg -> msg[msg.length - 1])
        .map(String::trim)
        .orElse(ex.getMessage());
  }

  /** Enum for indicating if the error was of functional or technical nature. */
  @Getter
  @AllArgsConstructor
  public enum ErrorType {
    TECH,
    FUNC
  }
}
