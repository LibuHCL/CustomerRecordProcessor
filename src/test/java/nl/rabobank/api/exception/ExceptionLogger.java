package nl.rabobank.api.exception;

import java.util.ArrayList;
import java.util.List;

public class ExceptionLogger {

  private boolean expectException;
  private List<SvmException> serviceExceptions = new ArrayList<>();

  public ExceptionLogger() {
    expectException = true;
  }

  public void addServiceExceptions(SvmException exception) {
    if (!expectException) {
      throw exception;
    }
    serviceExceptions.add(exception);
  }

  public List<SvmException> getServiceExceptions() {
    return serviceExceptions;
  }
}
