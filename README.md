# Rabobank Customer Statement Processor

# Table of Contents
1. Introduction
2. Input
3. Output
4. Expectation
5. Technology Stack
6. Execution

## Introduction
Rabobank receives monthly deliveries of customer statement records. This information is delivered in two formats, CSV and XML.  These records need to be validated.

## Input
The format of the file is a simplified version of the MT940 format. The format is as follows:

| Field  | Description |
| ------------- | ------------- |
| Transaction reference  | A numeric value  |
| Account number  | An IBAN  |
| TStart Balance  | The starting balance in Euros  |
| Mutation  | Either an addition (+) or a deduction (-)  |
| Description  | Free text  |
| End Balance  | The end balance in Euros |

## Output
There are two validations:

* All transaction references should be unique

* The end balance needs to be validated

## Expectation
At the end of the processing, a report needs to be created which will display both the transaction reference and description of each of the failed records.

## Technology Stack

* Java 8
* Maven
* Spring boot service
* REST api with MapStruct
* Lombok
* Integration testcase with MockMVC
* BDD testcase with Apache cucumber
* TDD based development

## Execution

mvn spring-boot:run or If using an IDE open the main application class RabobankBackendApplication and run It.

To validate the integration test cases once the application is UP run the test classes that ends with IT.
