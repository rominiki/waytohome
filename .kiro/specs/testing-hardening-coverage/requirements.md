# Requirements Document

## Introduction

This specification defines comprehensive testing requirements to raise test quality and coverage across the WayToHome application to production level. The system will integrate Testcontainers for real PostgreSQL integration testing, JaCoCo for coverage measurement, and thorough negative-path testing across all endpoints to ensure robustness and reliability.

## Glossary

- **Test_Suite**: The complete collection of automated tests for the WayToHome application
- **Testcontainers**: A Java library that provides lightweight, throwaway PostgreSQL instances for integration testing
- **Integration_Test**: A test that validates multiple components working together with real dependencies
- **Coverage_Metric**: A measurement of code execution during test runs, expressed as a percentage
- **Service_Layer**: The business logic layer containing service classes that implement application functionality
- **Negative_Path_Test**: A test that validates system behavior when given invalid inputs or unauthorized requests
- **Happy_Path_Test**: A test that validates the primary user journey through the system with valid inputs
- **BaseIntegrationTest**: An abstract test class that provides common Testcontainers setup for all integration tests
- **JaCoCo**: Java Code Coverage tool that measures and reports test coverage
- **Edge_Case**: Boundary conditions or exceptional scenarios that must be handled correctly

## Requirements

### Requirement 1: Testcontainers Integration

**User Story:** As a developer, I want integration tests to run against a real PostgreSQL database, so that I can catch database-specific issues that H2 in-memory databases would miss.

#### Acceptance Criteria

1. WHEN the test suite runs THEN the system SHALL use Testcontainers to provide a real PostgreSQL 16 container
2. WHEN an integration test starts THEN the system SHALL automatically start the PostgreSQL container and configure Spring datasource properties
3. WHEN an integration test completes THEN the system SHALL clean up the container resources automatically
4. THE BaseIntegrationTest class SHALL provide the Testcontainers configuration for all integration tests to extend
5. WHEN multiple integration tests run THEN the system SHALL reuse the same PostgreSQL container instance for performance

### Requirement 2: End-to-End Happy Path Testing

**User Story:** As a developer, I want a complete end-to-end test of the main user journey, so that I can verify all components work together correctly.

#### Acceptance Criteria

1. WHEN the happy path test runs THEN the system SHALL successfully execute user registration with valid credentials
2. WHEN a user is registered THEN the system SHALL successfully authenticate the user and return a valid JWT token
3. WHEN a landlord user is authenticated THEN the system SHALL successfully create a new listing
4. WHEN a listing is created THEN the system SHALL allow an admin user to approve the listing
5. WHEN a listing is approved THEN the system SHALL allow a student user to favorite the listing
6. WHEN a listing is favorited THEN the system SHALL allow the student to start a conversation with the landlord
7. WHEN a conversation is started THEN the system SHALL allow both parties to send and receive messages
8. THE end-to-end test SHALL complete the entire journey from registration to messaging in a single test flow

### Requirement 3: Coverage Measurement and Enforcement

**User Story:** As a developer, I want automated coverage measurement, so that I can ensure the service layer has sufficient test coverage.

#### Acceptance Criteria

1. WHEN the build runs THEN the system SHALL generate a JaCoCo coverage report in HTML and XML formats
2. WHEN coverage is measured THEN the system SHALL report coverage percentages for the service layer
3. THE service layer coverage SHALL be at least 70 percent
4. WHEN coverage thresholds are not met THEN the build SHALL fail with a clear error message
5. THE coverage report SHALL be accessible in the target/site/jacoco directory after build completion

### Requirement 4: Negative Path Testing for Authentication and Authorization

**User Story:** As a developer, I want comprehensive negative path tests, so that I can verify the system handles invalid requests and unauthorized access correctly.

#### Acceptance Criteria

1. WHEN a request is made without an authentication token THEN the system SHALL return HTTP 401 Unauthorized
2. WHEN a request is made with an invalid or expired token THEN the system SHALL return HTTP 401 Unauthorized
3. WHEN a user attempts an action without the required role THEN the system SHALL return HTTP 403 Forbidden
4. WHEN a student user attempts to create a listing THEN the system SHALL return HTTP 403 Forbidden
5. WHEN a landlord user attempts to approve a listing THEN the system SHALL return HTTP 403 Forbidden
6. WHEN a non-admin user attempts to access admin endpoints THEN the system SHALL return HTTP 403 Forbidden

### Requirement 5: Negative Path Testing for Resource Operations

**User Story:** As a developer, I want tests that validate error handling for missing or invalid resources, so that I can ensure the API provides clear error responses.

#### Acceptance Criteria

1. WHEN a request references a non-existent resource ID THEN the system SHALL return HTTP 404 Not Found
2. WHEN a request contains invalid input data THEN the system SHALL return HTTP 400 Bad Request with validation errors
3. WHEN a user attempts to register with an existing email THEN the system SHALL return HTTP 409 Conflict
4. WHEN a user attempts to favorite the same listing twice THEN the system SHALL return HTTP 409 Conflict
5. WHEN a user attempts to start multiple conversations for the same listing THEN the system SHALL return HTTP 409 Conflict

### Requirement 6: Edge Case Testing

**User Story:** As a developer, I want tests for boundary conditions and edge cases, so that I can ensure the system handles exceptional scenarios gracefully.

#### Acceptance Criteria

1. WHEN a request contains empty or whitespace-only required fields THEN the system SHALL reject the request with HTTP 400
2. WHEN a request contains fields exceeding maximum length constraints THEN the system SHALL reject the request with HTTP 400
3. WHEN a listing search uses boundary values for price or area THEN the system SHALL return correct results including boundary matches
4. WHEN pagination parameters are at boundaries (zero, negative, very large) THEN the system SHALL handle them correctly or reject invalid values
5. WHEN concurrent operations attempt to modify the same resource THEN the system SHALL handle race conditions correctly

### Requirement 7: Custom Exception Coverage

**User Story:** As a developer, I want every custom exception to be tested, so that I can verify exception handling paths work correctly.

#### Acceptance Criteria

1. THE test suite SHALL include at least one test that triggers DuplicateEmailException
2. THE test suite SHALL include at least one test that triggers ResourceNotFoundException
3. WHEN a custom exception is thrown THEN the GlobalExceptionHandler SHALL catch it and return the appropriate HTTP status and error message
4. THE tests SHALL verify that exception responses include meaningful error messages for debugging

### Requirement 8: Repository Integration Testing

**User Story:** As a developer, I want repository tests to run against real PostgreSQL, so that I can verify database queries and constraints work correctly with actual database behavior.

#### Acceptance Criteria

1. WHEN repository tests run THEN the system SHALL use Testcontainers-provided PostgreSQL instance
2. THE test suite SHALL include tests for custom repository queries and specifications
3. THE test suite SHALL verify database constraints (unique, foreign key, not null) enforce correctly
4. WHEN repository tests complete THEN the system SHALL roll back transactions to maintain test isolation

### Requirement 9: Test Organization and Maintainability

**User Story:** As a developer, I want well-organized test classes, so that I can easily locate and maintain tests.

#### Acceptance Criteria

1. THE test suite SHALL use nested test classes to group positive, negative, and edge case tests
2. THE test classes SHALL use descriptive names that clearly indicate what is being tested
3. THE BaseIntegrationTest class SHALL provide common setup and configuration that all integration tests inherit
4. WHEN adding new tests THEN developers SHALL extend BaseIntegrationTest for integration tests requiring database access
