package nl.rabobank.api.exception;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.rabobank.api.valueobject.Error;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class ExceptionMapper extends ResponseEntityExceptionHandler {
  private static final Logger LOG = Logger.getLogger(ExceptionMapper.class.getName());

  @ExceptionHandler(NoSuchFileException.class)
  private ResponseEntity<Object> mapFileNotFound(
      final NoSuchFileException exception, final WebRequest webRequest) {
    final Error errorPO = new Error(exception.getMessage());

    final HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    return handleExceptionInternal(exception, errorPO, headers, NOT_FOUND, webRequest);
  }

  @ExceptionHandler(IOException.class)
  private ResponseEntity<Object> mapNotFound(
      final IOException exception, final WebRequest webRequest) {

    final Error errorPO = new Error(exception.getMessage());

    final HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    return handleExceptionInternal(exception, errorPO, headers, INTERNAL_SERVER_ERROR, webRequest);
  }

  @ExceptionHandler(SvmException.class)
  private ResponseEntity<Error> handleSvmException(final SvmException exception) {
    LOG.log(Level.WARNING, exception.getMessage());
    final Error errorPO = new Error(exception.getMessage());
    return new ResponseEntity(errorPO, exception.getStatus());
  }

  @ExceptionHandler(IllegalStateException.class)
  private ResponseEntity<Object> handleServerErrorException(final Exception exception) {
    LOG.log(Level.WARNING, exception.getMessage());
    final Error errorPO = new Error(exception.getMessage());
    return new ResponseEntity(errorPO, INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(java.lang.Exception.class)
  public final ResponseEntity<Error> handleAllExceptions(Exception ex, WebRequest request) {

    Error errorPO = new Error(ex.getMessage());
    return new ResponseEntity<>(errorPO, new HttpHeaders(), INTERNAL_SERVER_ERROR);
  }
}
