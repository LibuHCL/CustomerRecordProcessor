package nl.rabobank.api.utils;

import static io.restassured.RestAssured.given;

import io.restassured.builder.MultiPartSpecBuilder;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

public class CustomerUtilityProcessor {

  private static final String CUSTOMER_URL = "http://localhost:8080/customers/upload";

  public static File readCustomerRecord(String fileName) {
    return new File(
        Objects.requireNonNull(
                CustomerUtilityProcessor.class.getClassLoader().getResource(fileName))
            .getFile());
  }

  public static MultipartFile convertToMultiPartFormat(File file) throws IOException {
    String fileType = Files.probeContentType(file.toPath());
    if (StringUtils.equals("text/xml", fileType)) {
      return new MockMultipartFile(
          file.getName(), "", "application/xml", IOUtils.toByteArray(new FileInputStream(file)));
    } else if (StringUtils.equals("application/vnd.ms-excel", fileType)) {
      return new MockMultipartFile(
          file.getName(), "", "text/csv", IOUtils.toByteArray(new FileInputStream(file)));
    }
    return new MockMultipartFile(
        file.getName(),
        "",
        FilenameUtils.getExtension(file.getPath()),
        IOUtils.toByteArray(new FileInputStream(file)));
  }

  public static Response postCustomerRecordsToEndpoint(File file) {
    Response response = null;
    RequestSpecBuilder builder = new RequestSpecBuilder();
    builder.addMultiPart("uploadedFile", file);
    RequestSpecification requestSpec = builder.build();
    try {
      response =
          given()
              .multiPart(
                  new MultiPartSpecBuilder(file)
                      .mimeType(Files.probeContentType(file.toPath()))
                      .build())
              .spec(requestSpec)
              .when()
              .post(CUSTOMER_URL);
    } catch (IOException exception) {
      exception.getMessage();
    }

    return response;
  }
}
