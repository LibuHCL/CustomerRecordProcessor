package nl.rabobank.api.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import nl.rabobank.api.domain.CustomerResponse;
import nl.rabobank.api.exception.SvmException;
import nl.rabobank.api.service.CustomerService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Resource which exposes the customer service endpoints. GET for retrieval and POST for uploading
 * the customer statement records.
 */
@RestController
@RequestMapping(value = "/customers", produces = MediaType.APPLICATION_JSON)
@Api(value = "customer", tags = "CustomerResource")
@Slf4j
public class CustomerResource {

  @Autowired CustomerService customerService;

  @PostMapping(path = "/upload")
  @ApiOperation(value = "uploads the monthly customer data from xml0 or csv format")
  public ResponseEntity<List<CustomerResponse>> upload(
      @RequestParam(required = true) MultipartFile uploadedFile) {
    List<CustomerResponse> customerResponses = new ArrayList<>();
    Optional<List<CustomerResponse>> customerResponse =
        customerService.upload(uploadedFile, uploadedFile.getContentType());
    if (customerResponse.isPresent()) {
      if (CollectionUtils.isEmpty(customerResponse.get())) {
        throw new SvmException(HttpStatus.NOT_FOUND, "no customer validation record errors");
      }
      return new ResponseEntity<>(customerResponse.get(), HttpStatus.OK);
    }

    return new ResponseEntity<>(customerResponses, HttpStatus.OK);
  }
}
