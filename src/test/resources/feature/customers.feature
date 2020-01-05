Feature:Customer Statement Processor

  Scenario:when user uploads an valid xml file that has errors then expect processing of the records
    Given An customer file with name "records.xml"
    When Validating the uploaded file
    Then Successful customer domain will be returned

  Scenario:when user uploads an valid csv file that has errors then expect processing of the records
    Given An customer file with name "records.csv"
    When Validating the uploaded file
    Then Failed customer records with transaction reference is returned

  Scenario:when user uploads an invalid file that has errors then expect no processing of the records
    Given An customer file with name "records.pdf"
    When Validating the uploaded file
    Then Invalid file format error is returned

  Scenario:when user uploads an valid file that do not have errors then expect processing of the records
    Given An customer file with name "records_with_no_error.csv"
    When Validating the uploaded file
    Then Empty records are returned

  Scenario:Post an customer record xml file that process the file and reports If there are any errors.
    Given An customer file with name "records_with_no_error.xml"
    When I upload the file containing the customer data
    Then I receive "no customer validation record errors" message


