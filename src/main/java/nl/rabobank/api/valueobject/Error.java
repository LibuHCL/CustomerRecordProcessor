package nl.rabobank.api.valueobject;

public class Error {

  private String message;

  public Error(String message) {
    this.message = message;
  }

  public String getMessage() {
    return message;
  }
}
