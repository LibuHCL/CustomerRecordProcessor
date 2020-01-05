package nl.rabobank.api;

import static org.hamcrest.CoreMatchers.is;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import javax.ws.rs.core.MediaType;
import org.apache.commons.io.IOUtils;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@RunWith(SpringRunner.class)
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
public class CustomerResourceIT {

  private static final String CUSTOMER_UPLOAD_URL = "/customers/upload";
  @Value("classpath:records.xml")
  Resource xmlResource;
  @Value("classpath:records.csv")
  Resource csvResource;
  @Value("classpath:records.pdf")
  Resource invalidFormat;
  @Value("classpath:records_with_no_error.xml")
  Resource xmlValidResource;
  private byte[] uploadedXMLFile;
  private byte[] uploadedCSVFile;
  private byte[] uploadedInvalidFormatFile;
  private byte[] uploadedValidXmlFile;
  private MockMultipartFile multipartXMLFile;
  private MockMultipartFile multipartCSVFile;
  private MockMultipartFile inValidFormatFile;
  private MockMultipartFile noErrorXMLFile;
  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Before
  public void setUp() throws IOException {
    uploadedXMLFile = IOUtils.toByteArray(xmlResource.getInputStream());
    uploadedCSVFile = IOUtils.toByteArray(csvResource.getInputStream());
    uploadedInvalidFormatFile = IOUtils.toByteArray(invalidFormat.getInputStream());
    uploadedValidXmlFile = IOUtils.toByteArray(xmlValidResource.getInputStream());

    multipartXMLFile =
        new MockMultipartFile(
            "uploadedFile", "records", MediaType.APPLICATION_XML, uploadedXMLFile);

    multipartCSVFile =
        new MockMultipartFile(
            "uploadedFile", "records", MediaType.APPLICATION_JSON, uploadedCSVFile);

    inValidFormatFile =
        new MockMultipartFile(
            "uploadedFile", "records", MediaType.MULTIPART_FORM_DATA, uploadedInvalidFormatFile);
    noErrorXMLFile =
        new MockMultipartFile(
            "uploadedFile",
            "records_with_no_error",
            MediaType.APPLICATION_XML,
            uploadedValidXmlFile);
  }

  @Test
  public void check_upload_endpoint_availability_without_file() throws Exception {

    mockMvc
        .perform(MockMvcRequestBuilders.multipart(CUSTOMER_UPLOAD_URL))
        .andExpect(MockMvcResultMatchers.status().is4xxClientError());
  }

  @Test
  public void check_upload_endpoint_availability_with_service_up() throws Exception {

    mockMvc
        .perform(MockMvcRequestBuilders.multipart(CUSTOMER_UPLOAD_URL).file(multipartCSVFile))
        .andExpect(MockMvcResultMatchers.status().isOk());
  }

  @Test
  public void check_invalid_upload_file_format() throws Exception {

    mockMvc
        .perform(
            MockMvcRequestBuilders.multipart(CUSTOMER_UPLOAD_URL)
                .file(inValidFormatFile)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isBadRequest())
        .andExpect(MockMvcResultMatchers.jsonPath("$.message").isString());
  }

  @Test
  public void check_csv_file_upload_successful() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.multipart(CUSTOMER_UPLOAD_URL).file(multipartCSVFile))
        .andExpect(MockMvcResultMatchers.status().isOk());
  }

  @Test
  public void check_csv_file_upload_with_errors() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.multipart(CUSTOMER_UPLOAD_URL).file(multipartCSVFile))
        .andExpect(MockMvcResultMatchers.jsonPath("$.length()", is(3)));
  }

  @Test
  public void check_csv_file_upload_with_reference_errors() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.multipart(CUSTOMER_UPLOAD_URL).file(multipartCSVFile))
        .andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(3)))
        .andExpect(MockMvcResultMatchers.jsonPath("$.[0].reference").isNumber())
        .andExpect(MockMvcResultMatchers.jsonPath("$.[0].reference").value(112806))
        .andExpect(MockMvcResultMatchers.jsonPath("$.[0].description").exists());
  }

  @Test
  public void check_xml_file_upload_with_no_reference_errors() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.multipart(CUSTOMER_UPLOAD_URL)
                .file(noErrorXMLFile)
                .contentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA))
        .andExpect(MockMvcResultMatchers.status().isNotFound());
  }

  @Test
  public void check_csv_file_upload_with_no_reference_errors() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.multipart(CUSTOMER_UPLOAD_URL)
                .file(noErrorXMLFile)
                .contentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA))
        .andExpect(
            MockMvcResultMatchers.jsonPath("$.message")
                .value("no customer validation record errors"));
  }
}
