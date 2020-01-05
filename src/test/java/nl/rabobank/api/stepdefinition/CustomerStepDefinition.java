package nl.rabobank.api.stepdefinition;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.empty;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import nl.rabobank.api.domain.CustomerResponse;
import nl.rabobank.api.exception.ExceptionLogger;
import nl.rabobank.api.exception.SvmException;
import nl.rabobank.api.service.CustomerService;
import nl.rabobank.api.service.impl.CustomerServiceImpl;
import nl.rabobank.api.utils.CustomerUtilityProcessor;
import org.junit.Assert;
import org.springframework.web.multipart.MultipartFile;

public class CustomerStepDefinition {
  private MultipartFile multipartFile;
  private File file;
  private Optional<List<CustomerResponse>> customerResponse;
  private CustomerService customerService = new CustomerServiceImpl();
  private ExceptionLogger exceptionLogger;
  private Response apiResponse;

  @Given("An customer file with name {string}")
  public MultipartFile file_for_processing(String customerFile) throws IOException {
    file = CustomerUtilityProcessor.readCustomerRecord(customerFile);
    multipartFile = CustomerUtilityProcessor.convertToMultiPartFormat(file);
    return multipartFile;
  }

  @When("^Validating the uploaded file$")
  public void processCustomerRecord() {
    try {
      customerResponse = customerService.upload(multipartFile, multipartFile.getContentType());
    } catch (SvmException serviceException) {
      exceptionLogger = new ExceptionLogger();
      exceptionLogger.addServiceExceptions(serviceException);
    }
  }

  @When("I upload the file containing the customer data")
  public void uploadCustomerRecords() {

    apiResponse = CustomerUtilityProcessor.postCustomerRecordsToEndpoint(file);
  }

  @Then("Successful customer domain will be returned")
  public void verificationForValidCustomer() {
    customerResponse.ifPresent(Assert::assertNotNull);
  }

  @Then("Failed customer records with transaction reference is returned")
  public void verificationForFailedTransactionReferenceCustomerRecords() {
    if (customerResponse.isPresent()) {
      Assert.assertEquals(
          "No Failed transcation reference customer records",
          3,
          customerResponse.orElse(Collections.emptyList()).stream().count());
      Assert.assertEquals(
          "Mismatch in transcation reference number",
          Integer.valueOf(112806),
          customerResponse.get().stream()
              .filter(reference -> reference.getReference().equals(112806))
              .findFirst()
              .get()
              .getReference());
      Assert.assertEquals(
          "Mismatch in transcation description",
          "Flowers from Erik de Vries",
          customerResponse.get().stream()
              .filter(
                  reference ->
                      reference.getDescription().equalsIgnoreCase("Flowers from Erik de Vries"))
              .findFirst()
              .get()
              .getDescription());
    }
  }

  @Then("Invalid file format error is returned")
  public void verificationForInvalidFileFormat() {
    List<SvmException> svmExceptions = new ArrayList<>(exceptionLogger.getServiceExceptions());
    Assert.assertThat(exceptionLogger.getServiceExceptions(), is(not(empty())));
    Assert.assertEquals(
        "Incorrect message for uploading wrong format file",
        "Invalid file format",
        svmExceptions.get(0).getMessage());
  }

  @Then("Empty records are returned")
  public void verificationForValidAndNoErrorFile() {
    if (customerResponse.isPresent()) {
      Assert.assertEquals(
          "Failed transcation records still exist",
          0,
          customerResponse.orElse(Collections.emptyList()).stream().count());
    }
  }

  @Then("I receive {string} message")
  public void verificationForCustomerUploadEndpoint(String message) {
    JsonPath jsonPath = apiResponse.jsonPath();
    Assert.assertEquals(message, jsonPath.get("message"));
  }
}
