# Rules Digest
Generated from `good-practices.md` (rule lines only, no code samples or tables).
If this digest and `good-practices.md` ever disagree, `good-practices.md` wins.
Cite rules by number. Read the full section in the Standards file when implementing it.

## 1. Project folder structure and package naming
- **Rule 1.1** Organize tests and request classes by **feature** (`auth`, `dashboard`, `employee`). NEVER organize by sprint or user story (`Sprint_1_Story_101`). Stories are temporary; features persist. Story folders produce duplicated coverage and obsolete tests.
- **Rule 1.2** Keep six top-level packages with fixed responsibilities: `tests`, `endpoints`, `payloads`, `client`, `specs`, `utils`.
- **Rule 1.3** A change to an API route, header, or payload MUST require editing exactly one framework class.
- **Rule 1.4** Name classes by what they are:
  - Request classes: `<Action><Resource>Request` (`AddEmployeeRequest`)
  - Request bodies: `<Resource>Payload` (`EmployeePayload`)
  - Response bodies: `<Resource>Response` (`EmployeeResponse`)
  - Tests: `<Action><Resource>Test` (`AddEmployeeTest`)

### Maven layout
- **Rule 1.5** The whole framework lives in `src/test/java`, and every dependency uses `test` scope. Move the framework packages to `src/main/java` (and their libraries to `compile` scope) only if the framework will be published as a JAR for other repositories.
- **Rule 1.6** JSON Schema files go in `src/test/resources/schemas/`. Maven does not put non-Java files from `src/test/java` on the classpath.
- **Rule 1.7** NEVER give a framework class the same name as a library class. `RequestSpecBuilder` and `ResponseSpecBuilder` are REST Assured classes; the framework classes are `RequestSpecFactory` and `ResponseSpecFactory`.

### Dependencies
- **Rule 1.8** Declare versions in `<properties>`. Use the latest **stable** release of every dependency and plugin from Maven Central when starting a new framework (never RC, milestone, alpha, beta, or snapshot). The versions in the example below are illustrative and may be outdated. When a library has moved to a newer major version, read its release notes or migration guide before adapting the code samples in this document.
- **Rule 1.9** WireMock 3 uses group ID `org.wiremock`. `com.github.tomakehurst` is the old WireMock 2 group.

## 2. Design patterns and abstraction layers

### 2.1 One class per request
- **Rule 2.1** Every API operation is one `final` class that extends `BaseApiRequest`. The class owns the HTTP method and the route.
- **Rule 2.2** Everything **mandatory** (path IDs, body) is a constructor argument. The compiler then rejects an incomplete request.
- **Rule 2.3** Everything **optional** (filters, paging, optional headers) is a named fluent method that returns the request type.
- **Rule 2.4** Request classes contain no assertions and no test logic. `send()` returns the REST Assured `Response`.
- **Rule 2.5** A request class extends **only** `BaseApiRequest`. NEVER make one request extend another (`UpdateEmployeeRequest extends AddEmployeeRequest`). HTTP verbs are different operations, not an IS-A relationship. Share fields by **composition**: both requests take the same `EmployeePayload`.
- **Rule 2.6** Tests MUST NOT call `given()`, write routes, or set headers. They only create request objects and call `send()`.
- **Rule 2.7** Use path parameters to identify one resource (`/employees/{id}`). Use query parameters to filter or page a collection (`/employees?department=QA`).

### 2.2 HTTP engine
- **Rule 2.8** `RestClient` is the only class that calls `given()`.
- **Rule 2.9** NEVER set static REST Assured globals (`RestAssured.baseURI`, `RestAssured.requestSpecification`). They are shared across threads. All settings go through spec objects.

### 2.3 Specifications
- **Rule 2.10** Base URI, base path, `Content-Type`, `Accept`, config, and filters are set once in `RequestSpecFactory`.
- **Rule 2.11** Response preconditions (status code, content type) are reusable `ResponseSpecification` objects from `ResponseSpecFactory`.

### 2.4 DTOs
- **Rule 2.12** Bodies are typed DTOs serialized by Jackson. NEVER build JSON with string concatenation.
- **Rule 2.13** Response DTOs use `@JsonIgnoreProperties(ignoreUnknown = true)`, so a new backend field does not break tests.
- **Rule 2.14** Request DTOs use `@JsonInclude(NON_NULL)`, so optional fields can be left out.

### 2.5 Test levels
- **Rule 2.15 (no wrong-level testing)** Exhaustive negative input checks (nulls, invalid formats, length boundaries) and exhaustive 401/403/404 permutations belong in developer-owned unit tests or API gateway validation rules. NEVER automate dozens of them at integration or system level. It inflates run time and flakiness without adding value.
- **Rule 2.16** Integration and system tests focus on:
  - Service-to-service communication
  - Payload contracts
  - HTTP status mapping across boundaries
  - Workflow outcomes
- **Rule 2.17** A single call is not an integration test. An integration scenario passes state across calls, for example:
  1. Create an employee and extract the `id`
  2. Assign the employee to a department
  3. Read the dashboard metrics and verify the count changed
- **Rule 2.18 (no black-box blindness)** Get and read the system architecture diagrams before designing suites. Know the API gateway, service boundaries, databases, caches, queues, load balancers, and identity provider.
- **Rule 2.19** If authentication is delegated to an external identity provider (Google, OAuth, SSO), do not write exhaustive negative authentication tests against the internal API. That logic is not in the system under test.

## 3. Configuration and data management

### 3.1 Environment configuration
- **Rule 3.1** NEVER hardcode hosts, ports, credentials, or tokens in Java code.
- **Rule 3.2** One properties file per environment, selected at run time with `-Denv=<name>`.
- **Rule 3.3** Resolution order: system property → environment variable → properties file. CI can override any value without file edits.
- **Rule 3.4** Secrets come from environment variables or the CI secret store. NEVER commit them.
- **Rule 3.5** A missing key fails immediately with a clear message. No silent defaults.

### 3.2 Backend infrastructure dependencies
- **Rule 3.6** NEVER use `Thread.sleep` to wait for async state. Poll with a timeout.
- **Rule 3.7** Treat an intermittent failure caused by one of these components as a signal to investigate, not something to retry until it passes.

### 3.3 Test data
- **Rule 3.8** **Static reference data** (lookup tables, country codes, fixed configuration) is loaded from external configuration files.
- **Rule 3.9** **Dynamic test data** (employees, accounts) is created at run time, with unique values per test so parallel runs do not collide.
- **Rule 3.10** Each test creates its own data. Tests MUST NOT depend on data created by another test.

### 3.4 Teardown
- **Rule 3.11** Clean up through the **API**, with `Delete...Request` objects. API teardown respects business rules and does not lock the database during parallel runs. The full strategy is in §3.6.
- **Rule 3.12** Direct database resets (`DatabaseUtils`) are only for environment initialization **before** the suite starts.

### 3.5 Authentication
- **Rule 3.13** `TokenManager` is the only place that logs in. It fetches a token on demand, caches it for its valid lifetime, and `RequestSpecFactory` injects it.
- **Rule 3.14** NEVER hardcode tokens and NEVER log in inside individual tests.
- **Rule 3.15** The token cache is thread-safe.
- **Rule 3.16** A failed login throws immediately with the status code. It must not surface later as a 401 in an unrelated test.

### 3.6 Cleanup strategy
- **Rule 3.17** Cleanup registration is automatic. Every request class that creates a resource overrides `cleanupFor(Response)` and returns the request that deletes it. `RestClient` registers it as soon as the create call succeeds, before the test continues. Tests contain no cleanup code.
- **Rule 3.18** Cleanup runs automatically after every test method, whether it passed, failed, or threw. `SuiteLifecycleListener` triggers it. Resources are deleted in reverse order of creation, so children go before parents.
- **Rule 3.19** Every delete result is verified:
  - `2xx`: deleted
  - `404`: already gone (for example, the test deleted it itself)
  - Any other status, or an exception: a **cleanup failure**
- **Rule 3.20** A cleanup failure NEVER changes the test result and NEVER stops the remaining cleanup actions.
- **Rule 3.21** Arrange steps use `sendAsSetup(expectedStatus)`. A wrong status throws `SetupFailedException`, so the report shows a setup failure and not a product defect.
- **Rule 3.22** Create data inside the test method or in `@BeforeMethod`. NEVER create shared data in `@BeforeClass` or `@BeforeSuite`: cleanup is scoped to one test method on one thread.
- **Rule 3.23** NEVER use `@Test(timeOut = ...)`. TestNG runs such a test on a separate thread, and the cleanup registry is bound to the test thread. Set timeouts in the HTTP config and in Awaitility.
- **Rule 3.24** All dynamic test data carries a marker that identifies it as automation data (for example the `@enterprise.test` email domain). A pre-suite sweep uses the marker to delete leftovers from earlier runs.

## 4. Logging and Allure reporting
- **Rule 4.1** A CI failure MUST be diagnosable from the report alone, without a local debug run.
- **Rule 4.2** For every API call, the filter layer captures and attaches:
  1. Base URI, route, HTTP method, query and path parameters
  2. Request headers, with authorization values masked
  3. Request body
  4. Status code, status line, and latency in milliseconds
  5. Response headers and response body
- **Rule 4.3** Secrets never reach logs or reports. Mask `Authorization` and cookie headers, and mask password and token fields in bodies.
- **Rule 4.4** `DiagnosticFilter` is the **single source** of HTTP telemetry. The same masked data goes to Allure (full detail) and to the console (summary). NEVER use `log().all()` or REST Assured's own log filters: they do not mask body fields.
- **Rule 4.5** Every framework action is an Allure step inside the test that triggered it: each request, the login done by `TokenManager`, and the `Cleanup` step.
- **Rule 4.6** Allure is the stakeholder report. Tag tests with `@Feature`, `@Story`, and `@Severity` so the report shows pass/fail by feature.
- **Rule 4.7** The report MUST separate three failure types (Allure categories):
  - **Application defects:** wrong business result, wrong status code, invalid schema
  - **Test setup failures:** an arrange step failed (`SetupFailedException`)
  - **Automation infrastructure failures:** environment down, timeouts, login failure, queue stalls
- **Rule 4.8** Cleanup failures are not test failures. They appear in the test's `Cleanup` step, in the console log, and as the `leaked.resources` count in the Allure Environment panel (§3.6).
- **Rule 4.9** `SuiteLifecycleListener` writes `environment.properties` (environment name, base URI, leaked count) and copies `categories.json` into the Allure results directory when the suite ends.
- **Rule 4.10** Framework code logs through SLF4J only. NEVER use `System.out`.
- **Rule 4.11** SHOULD: log failed requests as a cURL command so developers can replay the exact call.

## 5. Assertions and validation rules

### Rule 5.1 — Assertion execution standard
- **Hard assertions MUST be used for execution prerequisites:** status code, `Content-Type`, and token validity. If these fail, stop. Field checks on an error body are noise.
- **Soft assertions MUST be used for multi-field payload verification.** All field failures are collected and reported together at the end of the test.

### Other rules
- **Rule 5.2** Validate the payload contract with JSON Schema at integration level. A `200` alone proves nothing; some services return `200` around an error body.
- **Rule 5.3** Deserialize responses into DTOs for field assertions. A string path such as `"data[0].id"` still compiles after a field is renamed and fails only at run time. Use JsonPath/GPath for quick collection checks.
- **Rule 5.4 (no over-asserting)** Assert business-critical identifiers, schemas, status codes, and state changes. NEVER assert cosmetic text (`"Record updated successfully!"`), static notification messages, or headers with no business value.
- **Rule 5.5** For collections, use `hasItems` or `containsInAnyOrder` unless the contract guarantees order.
- **Rule 5.6** Some secure APIs return `404` instead of `403` for another customer's resource, to hide whether it exists. Assert what the API contract documents.
- **Rule 5.7** A delete test verifies the result twice: the delete status, then a read that confirms the resource is gone. If the API caches reads, poll for the `404` (Rule 3.6).

## 6. TestNG and execution

### 6.1 Lifecycle without a BaseTest
- **Rule 6.1** Test classes MUST NOT extend a base test class.
- **Rule 6.2** Per-test needs (request defaults, tokens, logging) are provided by the framework layers when a request is sent.
- **Rule 6.3** Suite-wide lifecycle is one TestNG **listener**, `SuiteLifecycleListener`. It validates the environment at start, runs cleanup after every test method, and writes the report files at the end.
- **Rule 6.4** The listener is registered through the Java `ServiceLoader` file, so it applies to every suite and IDE run and cannot be left out of a suite XML.

### 6.2 Orchestration
- **Rule 6.5** Tag every test with groups (`smoke`, `regression`, plus feature groups). Define one suite file per purpose.
- **Rule 6.6** Run in parallel. This is safe because request objects are created per test, test data is unique, and specs are never shared mutable state.
- **Rule 6.7** Use `@DataProvider` for data-driven tests, with one DTO per row.
- **Rule 6.8** Retries handle only transient network or environment errors, at most once. NEVER use retries to hide flaky test code or application defects. Move flaky tests to a `quarantine` group and fix them.

### 6.3 Risk-based regression
- **Rule 6.9** Do not run every test on every build. Select by risk: core business paths and areas affected by the change first.
- **Rule 6.10** Remove redundant, low-value, and cosmetic tests from blocking gates. Consolidate linear scripts into feature workflow tests.

### 6.4 Planning
- **Rule 6.11** NEVER commit to a calendar date for "100% automation". Plan by sprint capacity:
  1. Calculate the available engineering capacity for the sprint.
  2. Prioritize the automation backlog by business risk and workflow value.
  3. Run a proof of concept for each technical unknown before committing dependent work.
  4. Commit only to what fits the sprint.
- **Rule 6.12** A technical unknown gets a time-boxed PoC task first. Example: extracting an email verification link through the Gmail API versus an internal mail server.

## Appendix B: Service virtualization with WireMock
- **Rule B.1** WireMock replaces the **dependencies** of the system under test. Configure the system to call WireMock's URL. Tests still call the real system.
- **Rule B.2** Deployment modes:
  - Embedded server on a dynamic port (system runs locally or in the same CI job)
  - Standalone JAR or Docker (shared environments)
  - WireMock Cloud (hosted)
- **Rule B.3** `mappings/` holds request-matching rules. `__files/` holds response bodies referenced by `bodyFileName`. Use an inline body only for small responses.
- **Rule B.4** Reset stubs between tests: `resetAll()`, or `POST /__admin/reset` on a standalone server.
- **Rule B.5** A **stub** returns a canned response. A **mock** also verifies that the expected call was made (`verify(...)`).
- **Rule B.6** Use stubs to simulate failures that are unsafe to trigger for real: 500, 502, 504, delays, and connection faults. Keep delays short.
- **Rule B.7** An unmatched request returns 404 with a "closest stub" diff. Read it first when debugging.

## Appendix C: Framework vs. lightweight tooling
- **Rule C.1** Short-lived projects (weeks to about three months): use lightweight tooling such as Postman collections. A layered Java framework will not reach break-even before the project ends.
- **Rule C.2** Multi-year products: a layered Java/REST Assured framework pays back through low maintenance, scalability, and CI/CD integration.
- **Rule C.3** Always include maintenance cost in the estimate.
