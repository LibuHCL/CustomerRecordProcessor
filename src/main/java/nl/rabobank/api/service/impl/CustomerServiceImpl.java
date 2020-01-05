package nl.rabobank.api.service.impl;

import static java.util.stream.Collectors.groupingBy;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import nl.rabobank.api.domain.CustomerResponse;
import nl.rabobank.api.exception.SvmException;
import nl.rabobank.api.model.Records;
import nl.rabobank.api.service.CustomerService;
import nl.rabobank.api.valueobject.CSVRecord;
import nl.rabobank.api.valueobject.XMLRecord;
import org.apache.commons.io.FileUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.supercsv.cellprocessor.ParseBigDecimal;
import org.supercsv.cellprocessor.ParseInt;
import org.supercsv.cellprocessor.constraint.NotNull;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvBeanReader;
import org.supercsv.io.ICsvBeanReader;
import org.supercsv.prefs.CsvPreference;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

@Service
public class CustomerServiceImpl implements CustomerService {

  private static XMLRecord apply(Records.Record xmlRecord) {
    return new XMLRecord.RecordBuilder(
            xmlRecord.getReference(),
            xmlRecord.getDescription(),
            xmlRecord.getStartBalance(),
            xmlRecord.getMutation(),
            xmlRecord.getEndBalance())
        .build();
  }

  @Override
  public Optional<List<CustomerResponse>> upload(MultipartFile uploadedFile, String fileType) {
    List<CustomerResponse> customerResponse;

    if (Stream.of("application/xml", "application/octet-stream")
        .anyMatch(fileType::equals)) {
      List<XMLRecord> overAllFailedCustomerXMLRecords = new ArrayList<>();
      Document customerDataAsDocument = null;
      try {
        customerDataAsDocument = convertToXMLFromByteArray(uploadedFile.getBytes());
      } catch (IOException e) {
        throw new SvmException(HttpStatus.INTERNAL_SERVER_ERROR, "conversion error to xml");
      }
      String customerDataAsString = convertToStringFromDocument(customerDataAsDocument);
      Optional<Records> customerRecords = convertToPOJOFromXMLString(customerDataAsString);
      List<Records.Record> customerRecord = customerRecords.get().getRecord();
      List<XMLRecord> overAllCustomerRecordsFromXML =
          customerRecord.stream().map(CustomerServiceImpl::apply).collect(Collectors.toList());
      List<XMLRecord> validationFailedTxnCSVRecords =
          getValidationFailedTxnXMLRecords(overAllCustomerRecordsFromXML);
      List<XMLRecord> validationFailedMutationCSVRecords =
          getValidationFailedMutationXMLRecords(overAllCustomerRecordsFromXML);
      overAllFailedCustomerXMLRecords.addAll(validationFailedTxnCSVRecords);
      overAllFailedCustomerXMLRecords.addAll(validationFailedMutationCSVRecords);
      customerResponse =
          overAllFailedCustomerXMLRecords.stream()
              .map(
                  xmlRecord ->
                      new CustomerResponse(xmlRecord.getReference(), xmlRecord.getDescription()))
              .collect(Collectors.toList());

      return Optional.of(customerResponse);
    } else if (Stream.of("text/csv", "application/json").anyMatch(fileType::equals)) {
      List<CSVRecord> overAllCustomerRecordsFromCSV = new ArrayList<>();
      List<CSVRecord> overAllFailedCustomerCSVRecords = new ArrayList<>();
      try {
        parseCustomerRecordsFromCSV(uploadedFile, overAllCustomerRecordsFromCSV);
      } catch (IOException e) {
        throw new SvmException(HttpStatus.INTERNAL_SERVER_ERROR, "parsing error from CSV");
      }
      List<CSVRecord> validationFailedTxnCSVRecords =
          getValidationFailedTxnCSVRecords(overAllCustomerRecordsFromCSV);
      List<CSVRecord> validationFailedMutationCSVRecords =
          getValidationFailedMutationCSVRecords(overAllCustomerRecordsFromCSV);
      overAllFailedCustomerCSVRecords.addAll(validationFailedTxnCSVRecords);
      overAllFailedCustomerCSVRecords.addAll(validationFailedMutationCSVRecords);
      customerResponse =
          overAllFailedCustomerCSVRecords.stream()
              .map(
                  csvRecord ->
                      new CustomerResponse(csvRecord.getReference(), csvRecord.getDescription()))
              .collect(Collectors.toList());
      return Optional.of(customerResponse);
    }
    throw new SvmException(HttpStatus.BAD_REQUEST, "Invalid file format");
  }

  private List<XMLRecord> getValidationFailedMutationXMLRecords(
      List<XMLRecord> overAllCustomerRecordsFromXML) {
    Predicate<XMLRecord> xmlRecordMutationPredicate =
        xmlRecord ->
            String.valueOf(xmlRecord.getMutation()).startsWith("+")
                ? xmlRecord
                    .getStartBalance()
                    .add(xmlRecord.getMutation())
                    .equals(xmlRecord.getEndBalance())
                : xmlRecord
                    .getStartBalance()
                    .subtract(xmlRecord.getMutation())
                    .equals(xmlRecord.getEndBalance());
    return overAllCustomerRecordsFromXML.stream()
        .filter(xmlRecordMutationPredicate)
        .collect(Collectors.toList());
  }

  private List<XMLRecord> getValidationFailedTxnXMLRecords(
      List<XMLRecord> overAllCustomerRecordsFromXML) {
    Predicate<List<XMLRecord>> xmlRecordTranscationReferencePredicate =
        xmlRecords -> xmlRecords.size() > 1;
    return getTransactionReferenceXMLValidation(overAllCustomerRecordsFromXML).values().stream()
        .filter(xmlRecordTranscationReferencePredicate)
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }

  private List<CSVRecord> getValidationFailedMutationCSVRecords(
      List<CSVRecord> overAllCustomerRecordsFromCSV) {
    Predicate<CSVRecord> csvRecordMutationPredicate =
        csvRecord ->
            String.valueOf(csvRecord.getMutation()).startsWith("+")
                ? csvRecord
                    .getStartBalance()
                    .add(csvRecord.getMutation())
                    .equals(csvRecord.getEndBalance())
                : csvRecord
                    .getStartBalance()
                    .subtract(csvRecord.getMutation())
                    .equals(csvRecord.getEndBalance());
    return overAllCustomerRecordsFromCSV.stream()
        .filter(csvRecordMutationPredicate)
        .collect(Collectors.toList());
  }

  private List<CSVRecord> getValidationFailedTxnCSVRecords(
      List<CSVRecord> overAllCustomerRecordsFromXML) {
    Predicate<List<CSVRecord>> csvRecordTranscationReferencePredicate =
        csvRecords -> csvRecords.size() > 1;
    return getTransactionReferenceCSVValidation(overAllCustomerRecordsFromXML).values().stream()
        .filter(csvRecordTranscationReferencePredicate)
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }

  private Map<Integer, List<CSVRecord>> getTransactionReferenceCSVValidation(
      List<CSVRecord> overAllCustomerRecordsFromXML) {
    return overAllCustomerRecordsFromXML.stream()
        .collect(groupingBy(CSVRecord::validateReferenceAttribute));
  }

  private Map<Integer, List<XMLRecord>> getTransactionReferenceXMLValidation(
      List<XMLRecord> overAllCustomerRecordsFromXML) {
    return overAllCustomerRecordsFromXML.stream()
        .collect(groupingBy(XMLRecord::validateReferenceAttribute));
  }

  private List<CSVRecord> parseCustomerRecordsFromCSV(
      MultipartFile customerData, List<CSVRecord> overAllCustomerRecordsFromCSV)
      throws IOException {
    try (ICsvBeanReader csvBeanReader =
        new CsvBeanReader(
            new FileReader("src/test/resources/customer_records.csv"), CsvPreference.STANDARD_PREFERENCE); ) {
      CSVRecord record;
      FileUtils.writeByteArrayToFile(new File("src/test/resources/customer_records.csv"), customerData.getBytes());
      final String[] nameMapping =
          new String[] {
            "reference", "accountNumber", "description", "startBalance", "mutation", "endBalance"
          };
      final String[] header = csvBeanReader.getHeader(true);
      final CellProcessor[] cellProcessors = getProcessors();
      while ((record = csvBeanReader.read(CSVRecord.class, nameMapping, cellProcessors)) != null) {
        overAllCustomerRecordsFromCSV.add(record);
      }
    } catch (IOException exception) {
      throw new IOException(
          "exception occurred while processing customer csv file with message", exception);
    }

    return overAllCustomerRecordsFromCSV;
  }

  private CellProcessor[] getProcessors() {
    return new CellProcessor[] {
      new NotNull(new ParseInt()),
      new org.supercsv.cellprocessor.Optional(),
      new NotNull(),
      new NotNull(new ParseBigDecimal()),
      new NotNull(new ParseBigDecimal()),
      new NotNull(new ParseBigDecimal())
    };
  }

  private Document convertToXMLFromByteArray(byte[] customerData) {
    Document document = null;
    try {
      DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
      documentBuilderFactory.setValidating(false);
      documentBuilderFactory.setNamespaceAware(true);
      documentBuilderFactory.setIgnoringElementContentWhitespace(true);
      documentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
      document = builder.parse(new ByteArrayInputStream(customerData));
    } catch (ParserConfigurationException | SAXException | IOException exception) {
      throw new SvmException(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage());
    }
    return document;
  }

  private String convertToStringFromDocument(Document customerDataAsDocument) {
    try {
      StringWriter stringWriter = new StringWriter();
      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      Transformer transformer = transformerFactory.newTransformer();
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
      transformer.setOutputProperty(OutputKeys.METHOD, "xml");
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

      transformer.transform(new DOMSource(customerDataAsDocument), new StreamResult(stringWriter));
      return stringWriter.toString();
    } catch (TransformerException exception) {
      throw new SvmException(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage());
    }
  }

  private Optional<Records> convertToPOJOFromXMLString(String customerDataAsString) {
    Records records = new Records();
    try {
      JAXBContext jaxbContext = JAXBContext.newInstance(Records.class);
      Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
      records = (Records) unmarshaller.unmarshal(new StringReader(customerDataAsString));

    } catch (JAXBException jaxbException) {
      throw new SvmException(HttpStatus.INTERNAL_SERVER_ERROR, jaxbException.getMessage());
    }
    return Optional.of(records);
  }
}
