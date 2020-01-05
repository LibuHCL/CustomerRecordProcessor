package nl.rabobank.api.exception;

import org.springframework.http.HttpStatus;

/**
 * Custom system exception that indicates the intent of the error from the business functionality;
 */
public class SvmException extends RuntimeException {

  private static final long serialVersionUID = -3275370300491891874L;

  private final HttpStatus status;

  public SvmException(final HttpStatus status, final String message) {
    super(message);
    this.status = determineStatus(status);
  }

  public SvmException(final HttpStatus status, final String message, final Throwable cause) {
    super(message, cause);
    this.status = determineStatus(status);
  }

  private HttpStatus determineStatus(final HttpStatus status) {
    if (status == null) {
      return HttpStatus.INTERNAL_SERVER_ERROR;
    } else {
      return status;
    }
  }

  public HttpStatus getStatus() {
    return status;
  }
}
