package nl.rabobank.api.service;

import java.util.List;
import java.util.Optional;
import nl.rabobank.api.domain.CustomerResponse;
import org.springframework.web.multipart.MultipartFile;

public interface CustomerService {

  Optional<List<CustomerResponse>> upload(MultipartFile uploadedFile, String fileType);
}
