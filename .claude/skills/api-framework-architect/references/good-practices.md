# Enterprise API Test Automation Framework — Architecture Reference

Stack: Java 17, REST Assured, TestNG, Jackson, Allure, Maven. WireMock is optional (Appendix B).

Rules use **MUST / SHOULD / NEVER** and are numbered per section (`Rule 2.3`) so reviews can cite them.

Two design decisions shape everything below:

- **One class per API request.** Every operation the framework can call is its own class. Mandatory inputs are constructor arguments, so a test cannot forget one.
- **No `BaseTest`.** Test classes extend nothing. Configuration, authentication, and logging live in the framework layers and are reached by composition.

## Contents

1. [Project folder structure and package naming](#1-project-folder-structure-and-package-naming)
2. [Design patterns and abstraction layers](#2-design-patterns-and-abstraction-layers)
3. [Configuration and data management](#3-configuration-and-data-management)
4. [Logging and Allure reporting](#4-logging-and-allure-reporting)
5. [Assertions and validation rules](#5-assertions-and-validation-rules)
6. [TestNG and execution](#6-testng-and-execution)
7. [Anti-patterns to avoid](#7-anti-patterns-to-avoid)
- [Appendix A: HTTP status codes](#appendix-a-http-status-codes)
- [Appendix B: Service virtualization with WireMock](#appendix-b-service-virtualization-with-wiremock)
- [Appendix C: Framework vs. lightweight tooling](#appendix-c-framework-vs-lightweight-tooling)

---

## 1. Project folder structure and package naming

- **Rule 1.1** Organize tests and request classes by **feature** (`auth`, `dashboard`, `employee`). NEVER organize by sprint or user story (`Sprint_1_Story_101`). Stories are temporary; features persist. Story folders produce duplicated coverage and obsolete tests.
- **Rule 1.2** Keep six top-level packages with fixed responsibilities: `tests`, `endpoints`, `payloads`, `client`, `specs`, `utils`.
- **Rule 1.3** A change to an API route, header, or payload MUST require editing exactly one framework class.
- **Rule 1.4** Name classes by what they are:
  - Request classes: `<Action><Resource>Request` (`AddEmployeeRequest`)
  - Request bodies: `<Resource>Payload` (`EmployeePayload`)
  - Response bodies: `<Resource>Response` (`EmployeeResponse`)
  - Tests: `<Action><Resource>Test` (`AddEmployeeTest`)

### Package hierarchy

```text
com.enterprise.qa.automation
├── tests/                         Feature-based execution layer
│   ├── auth/                      LoginTest.java
│   ├── dashboard/                 DashboardMetricsTest.java
│   └── employee/
│       ├── AddEmployeeTest.java
│       ├── UpdateEmployeeTest.java
│       ├── DeleteEmployeeTest.java
│       └── SearchEmployeeTest.java
├── endpoints/                     One class per API request, grouped by feature
│   ├── auth/                      LoginRequest.java
│   ├── dashboard/                 GetDashboardMetricsRequest.java
│   └── employee/
│       ├── AddEmployeeRequest.java
│       ├── GetEmployeeRequest.java
│       ├── UpdateEmployeeRequest.java
│       ├── DeleteEmployeeRequest.java
│       └── SearchEmployeeRequest.java
├── payloads/
│   ├── pojos/                     EmployeePayload, EmployeeResponse, LoginPayload, LoginResponse
│   └── factories/                 EmployeeDataFactory.java   (dynamic test data)
├── client/                        HTTP engine
│   ├── BaseApiRequest.java        Abstract parent of every request class
│   ├── RestClient.java            Executes a request
│   ├── ApiClientFactory.java      Builds the REST Assured config (timeouts)
│   └── DiagnosticFilter.java      Captures request/response telemetry
├── specs/
│   ├── RequestSpecFactory.java
│   └── ResponseSpecFactory.java
└── utils/
    ├── EnvironmentConfig.java
    ├── TokenManager.java
    ├── DatabaseUtils.java         Pre-suite environment initialization only
    ├── CleanupRegistry.java       Verified, automatic cleanup of created resources
    ├── SetupFailedException.java
    ├── SuiteLifecycleListener.java
    └── TransientRetryAnalyzer.java
```

### Maven layout

```text
api-test-framework/
├── pom.xml
└── src/test/
    ├── java/com/enterprise/qa/automation/...     (the hierarchy above)
    └── resources/
        ├── config/      dev.properties, staging.properties
        ├── schemas/     employee.schema.json
        ├── suites/      smoke.xml, regression.xml
        ├── wiremock/    mappings/, __files/
        ├── META-INF/services/org.testng.ITestNGListener
        ├── allure.properties
        ├── categories.json
        └── logback-test.xml
```

- **Rule 1.5** The whole framework lives in `src/test/java`, and every dependency uses `test` scope. Move the framework packages to `src/main/java` (and their libraries to `compile` scope) only if the framework will be published as a JAR for other repositories.
- **Rule 1.6** JSON Schema files go in `src/test/resources/schemas/`. Maven does not put non-Java files from `src/test/java` on the classpath.
- **Rule 1.7** NEVER give a framework class the same name as a library class. `RequestSpecBuilder` and `ResponseSpecBuilder` are REST Assured classes; the framework classes are `RequestSpecFactory` and `ResponseSpecFactory`.

### Dependencies

- **Rule 1.8** Declare versions in `<properties>`. Use the latest **stable** release of every dependency and plugin from Maven Central when starting a new framework (never RC, milestone, alpha, beta, or snapshot). The versions in the example below are illustrative and may be outdated. When a library has moved to a newer major version, read its release notes or migration guide before adapting the code samples in this document.
- **Rule 1.9** WireMock 3 uses group ID `org.wiremock`. `com.github.tomakehurst` is the old WireMock 2 group.

```xml
<properties>
    <maven.compiler.release>17</maven.compiler.release>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <rest-assured.version>5.5.6</rest-assured.version>
    <jackson.version>2.18.2</jackson.version>
    <testng.version>7.12.0</testng.version>
    <allure.version>2.29.1</allure.version>
    <awaitility.version>4.2.2</awaitility.version>
    <slf4j.version>2.0.16</slf4j.version>
    <logback.version>1.5.12</logback.version>
    <wiremock.version>3.13.1</wiremock.version>
    <surefire.version>3.5.2</surefire.version>
    <suite>src/test/resources/suites/regression.xml</suite>
</properties>

<dependencies>
    <dependency>
        <groupId>io.rest-assured</groupId>
        <artifactId>rest-assured</artifactId>
        <version>${rest-assured.version}</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>io.rest-assured</groupId>
        <artifactId>json-schema-validator</artifactId>
        <version>${rest-assured.version}</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
        <version>${jackson.version}</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testng</groupId>
        <artifactId>testng</artifactId>
        <version>${testng.version}</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>io.qameta.allure</groupId>
        <artifactId>allure-testng</artifactId>
        <version>${allure.version}</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.awaitility</groupId>
        <artifactId>awaitility</artifactId>
        <version>${awaitility.version}</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-api</artifactId>
        <version>${slf4j.version}</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>ch.qos.logback</groupId>
        <artifactId>logback-classic</artifactId>
        <version>${logback.version}</version>
        <scope>test</scope>
    </dependency>
    <!-- Optional: only when downstream dependencies are virtualized -->
    <dependency>
        <groupId>org.wiremock</groupId>
        <artifactId>wiremock-standalone</artifactId>
        <version>${wiremock.version}</version>
        <scope>test</scope>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>${surefire.version}</version>
            <configuration>
                <suiteXmlFiles>
                    <suiteXmlFile>${suite}</suiteXmlFile>
                </suiteXmlFiles>
            </configuration>
        </plugin>
    </plugins>
</build>
```

Run: `mvn test -Denv=staging -Dsuite=src/test/resources/suites/smoke.xml`

> The framework creates Allure steps with `Allure.step(...)` lambdas, so the AspectJ agent is not needed. Add the agent only if you start using `@Step` or `@Attachment` annotations.

---

## 2. Design patterns and abstraction layers

Tests describe **what** the business flow does. Framework layers own **how** HTTP is done.

| Layer | Package | Responsibility |
|---|---|---|
| Request classes | `endpoints` | One class per API operation: verb, route, params, headers, body |
| DTOs | `payloads.pojos` | Typed request and response bodies. No JSON strings in tests |
| HTTP engine | `client` | Executes any request; config; telemetry |
| Specifications | `specs` | Shared request defaults and response preconditions |
| Infrastructure | `utils` | Environment config, tokens, suite lifecycle |
| Tests | `tests` | Build data → send requests → assert → clean up |

### 2.1 One class per request

- **Rule 2.1** Every API operation is one `final` class that extends `BaseApiRequest`. The class owns the HTTP method and the route.
- **Rule 2.2** Everything **mandatory** (path IDs, body) is a constructor argument. The compiler then rejects an incomplete request.
- **Rule 2.3** Everything **optional** (filters, paging, optional headers) is a named fluent method that returns the request type.
- **Rule 2.4** Request classes contain no assertions and no test logic. `send()` returns the REST Assured `Response`.
- **Rule 2.5** A request class extends **only** `BaseApiRequest`. NEVER make one request extend another (`UpdateEmployeeRequest extends AddEmployeeRequest`). HTTP verbs are different operations, not an IS-A relationship. Share fields by **composition**: both requests take the same `EmployeePayload`.
- **Rule 2.6** Tests MUST NOT call `given()`, write routes, or set headers. They only create request objects and call `send()`.
- **Rule 2.7** Use path parameters to identify one resource (`/employees/{id}`). Use query parameters to filter or page a collection (`/employees?department=QA`).

How the OOP principles apply:

| Principle | Where |
|---|---|
| Abstraction | `BaseApiRequest` defines what a request is; tests never see HTTP mechanics |
| Encapsulation | Each request class hides its route, verb, and parameter names |
| Inheritance | One level only: concrete request → `BaseApiRequest` |
| Polymorphism | `RestClient.send(BaseApiRequest)` executes any request type the same way |

```java
package com.enterprise.qa.automation.client;

import com.enterprise.qa.automation.utils.SetupFailedException;
import io.restassured.http.Method;
import io.restassured.response.Response;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class BaseApiRequest {

    private final Method method;
    private final String path;
    private final Map<String, Object> pathParams = new LinkedHashMap<>();
    private final Map<String, Object> queryParams = new LinkedHashMap<>();
    private final Map<String, String> headers = new LinkedHashMap<>();
    private Object body;

    protected BaseApiRequest(Method method, String path) {
        this.method = method;
        this.path = path;
    }

    // Building blocks for subclasses only
    protected final void pathParam(String name, Object value)  { pathParams.put(name, value); }
    protected final void queryParam(String name, Object value) { queryParams.put(name, value); }
    protected final void header(String name, String value)     { headers.put(name, value); }
    protected final void body(Object payload)                  { this.body = payload; }

    /** Override and return false for requests sent without a bearer token (e.g. login). */
    protected boolean requiresAuth() { return true; }

    /**
     * Override in every request that creates a resource: return the request that deletes it.
     * RestClient registers it for cleanup as soon as the create call succeeds.
     */
    protected BaseApiRequest cleanupFor(Response response) { return null; }

    /** Use in the act and assert parts of a test. */
    public final Response send() { return RestClient.send(this); }

    /** Use for arrange steps. A wrong status is a setup failure, not a product assertion failure. */
    public final Response sendAsSetup(int expectedStatus) {
        Response response = send();
        if (response.statusCode() != expectedStatus) {
            throw new SetupFailedException(this + " returned HTTP " + response.statusCode()
                    + ", expected " + expectedStatus);
        }
        return response;
    }

    @Override
    public String toString() {
        return method + " " + path + (pathParams.isEmpty() ? "" : " " + pathParams);
    }

    // Read access for RestClient (same package)
    Method method()                   { return method; }
    String path()                     { return path; }
    Map<String, Object> pathParams()  { return pathParams; }
    Map<String, Object> queryParams() { return queryParams; }
    Map<String, String> headers()     { return headers; }
    Object body()                     { return body; }
}
```

```java
package com.enterprise.qa.automation.endpoints.employee;

public final class AddEmployeeRequest extends BaseApiRequest {

    public AddEmployeeRequest(EmployeePayload employee) {
        super(Method.POST, "/employees");
        body(employee);
    }

    @Override
    protected BaseApiRequest cleanupFor(Response response) {
        String id = response.jsonPath().getString("id");
        return id == null ? null : new DeleteEmployeeRequest(id);
    }
}

public final class UpdateEmployeeRequest extends BaseApiRequest {

    public UpdateEmployeeRequest(String employeeId, EmployeePayload employee) {
        super(Method.PUT, "/employees/{id}");
        pathParam("id", employeeId);
        body(employee);
    }
}

public final class GetEmployeeRequest extends BaseApiRequest {

    public GetEmployeeRequest(String employeeId) {
        super(Method.GET, "/employees/{id}");
        pathParam("id", employeeId);
    }
}

public final class DeleteEmployeeRequest extends BaseApiRequest {

    public DeleteEmployeeRequest(String employeeId) {
        super(Method.DELETE, "/employees/{id}");
        pathParam("id", employeeId);
    }
}

public final class SearchEmployeeRequest extends BaseApiRequest {

    public SearchEmployeeRequest() {
        super(Method.GET, "/employees");
    }

    public SearchEmployeeRequest department(String department) {
        queryParam("department", department);
        return this;
    }

    public SearchEmployeeRequest page(int page) {
        queryParam("page", page);
        return this;
    }
}
```

```java
package com.enterprise.qa.automation.endpoints.auth;

public final class LoginRequest extends BaseApiRequest {

    public LoginRequest(LoginPayload credentials) {
        super(Method.POST, EnvironmentConfig.get("auth.path"));
        body(credentials);
    }

    @Override
    protected boolean requiresAuth() { return false; }
}
```

(Each class is its own file; imports are omitted above for brevity.)

### 2.2 HTTP engine

- **Rule 2.8** `RestClient` is the only class that calls `given()`.
- **Rule 2.9** NEVER set static REST Assured globals (`RestAssured.baseURI`, `RestAssured.requestSpecification`). They are shared across threads. All settings go through spec objects.

```java
package com.enterprise.qa.automation.client;

public final class RestClient {

    private RestClient() {}

    static Response send(BaseApiRequest request) {
        RequestSpecification spec = given()
                .spec(request.requiresAuth()
                        ? RequestSpecFactory.authenticated()
                        : RequestSpecFactory.anonymous())
                .pathParams(request.pathParams())
                .queryParams(request.queryParams())
                .headers(request.headers());

        if (request.body() != null) {
            spec.body(request.body());
        }

        Response response = Allure.step(request.toString(),
                () -> spec.request(request.method(), request.path()));

        registerCleanup(request, response);
        return response;
    }

    private static void registerCleanup(BaseApiRequest request, Response response) {
        boolean succeeded = response.statusCode() >= 200 && response.statusCode() < 300;
        if (!succeeded) {
            return;
        }
        BaseApiRequest deleteRequest = request.cleanupFor(response);
        if (deleteRequest != null) {
            CleanupRegistry.register(deleteRequest);
        }
    }
}
```

```java
package com.enterprise.qa.automation.client;

public final class ApiClientFactory {

    private ApiClientFactory() {}

    public static RestAssuredConfig config() {
        return RestAssuredConfig.config()
                .httpClient(HttpClientConfig.httpClientConfig()
                        .setParam("http.connection.timeout", 5_000)
                        .setParam("http.socket.timeout", 30_000));
    }
}
```

### 2.3 Specifications

- **Rule 2.10** Base URI, base path, `Content-Type`, `Accept`, config, and filters are set once in `RequestSpecFactory`.
- **Rule 2.11** Response preconditions (status code, content type) are reusable `ResponseSpecification` objects from `ResponseSpecFactory`.

```java
package com.enterprise.qa.automation.specs;

public final class RequestSpecFactory {

    private RequestSpecFactory() {}

    public static RequestSpecification anonymous() {
        return new RequestSpecBuilder()
                .setBaseUri(EnvironmentConfig.get("base.uri"))
                .setBasePath(EnvironmentConfig.get("base.path"))
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .setConfig(ApiClientFactory.config())
                .addFilter(new DiagnosticFilter())
                .build();
    }

    public static RequestSpecification authenticated() {
        return new RequestSpecBuilder()
                .addRequestSpecification(anonymous())
                .addHeader("Authorization", "Bearer " + TokenManager.getToken())
                .build();
    }
}

public final class ResponseSpecFactory {

    private ResponseSpecFactory() {}

    public static ResponseSpecification json(int statusCode) {
        return new ResponseSpecBuilder()
                .expectStatusCode(statusCode)
                .expectContentType(ContentType.JSON)
                .build();
    }

    public static ResponseSpecification status(int statusCode) {
        return new ResponseSpecBuilder().expectStatusCode(statusCode).build();
    }
}
```

### 2.4 DTOs

- **Rule 2.12** Bodies are typed DTOs serialized by Jackson. NEVER build JSON with string concatenation.
- **Rule 2.13** Response DTOs use `@JsonIgnoreProperties(ignoreUnknown = true)`, so a new backend field does not break tests.
- **Rule 2.14** Request DTOs use `@JsonInclude(NON_NULL)`, so optional fields can be left out.

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EmployeePayload(String firstName, String lastName, String email, String department) {}

@JsonIgnoreProperties(ignoreUnknown = true)
public record EmployeeResponse(String id, String firstName, String lastName, String email, String department) {}

public record LoginPayload(String username, String password) {}

@JsonIgnoreProperties(ignoreUnknown = true)
public record LoginResponse(@JsonProperty("access_token") String accessToken,
                            @JsonProperty("expires_in") long expiresIn) {}
```

### 2.5 Test levels

| Level | Focus | Owner |
|---|---|---|
| Unit | Isolated logic, input validation, negative inputs. Runs in milliseconds | Developers |
| Integration | Communication and contracts between services; status mapping across boundaries | QA |
| System (E2E) | Full business workflows across the stack | QA |

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

---

## 3. Configuration and data management

### 3.1 Environment configuration

- **Rule 3.1** NEVER hardcode hosts, ports, credentials, or tokens in Java code.
- **Rule 3.2** One properties file per environment, selected at run time with `-Denv=<name>`.
- **Rule 3.3** Resolution order: system property → environment variable → properties file. CI can override any value without file edits.
- **Rule 3.4** Secrets come from environment variables or the CI secret store. NEVER commit them.
- **Rule 3.5** A missing key fails immediately with a clear message. No silent defaults.

```java
package com.enterprise.qa.automation.utils;

public final class EnvironmentConfig {

    private static final String ENV =
            System.getProperty("env", System.getenv().getOrDefault("TEST_ENV", "dev"));
    private static final Properties PROPS = load("config/" + ENV + ".properties");

    private EnvironmentConfig() {}

    private static Properties load(String file) {
        Properties props = new Properties();
        try (InputStream in = EnvironmentConfig.class.getClassLoader().getResourceAsStream(file)) {
            if (in == null) {
                throw new IllegalStateException("Config file not found on classpath: " + file);
            }
            props.load(in);
            return props;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static String environment() {
        return ENV;
    }

    /** -Dbase.uri  >  BASE_URI env var  >  config/<env>.properties */
    public static String get(String key) {
        String value = System.getProperty(key);
        if (value == null) value = System.getenv(key.toUpperCase().replace('.', '_'));
        if (value == null) value = PROPS.getProperty(key);
        if (value == null) {
            throw new IllegalStateException("Missing config key '" + key + "' for env '" + ENV + "'");
        }
        return value;
    }
}
```

```properties
# src/test/resources/config/staging.properties
base.uri=https://staging-api.enterprise.internal
base.path=/api/v1
auth.path=/auth/login
auth.username=qa-automation
# auth.password comes from the AUTH_PASSWORD environment variable
```

### 3.2 Backend infrastructure dependencies

| Component | Risk | Framework response |
|---|---|---|
| Message queues (RabbitMQ, Kafka) | Async processing; messages dropped on overflow | Poll for the final state. Never assume instant persistence |
| Caches (Redis, in-memory) | A read right after a write returns stale data until the TTL expires | Know the invalidation rules. Use a cache-bypass or flush endpoint in setup, or poll |
| Load balancers | Session state not shared across nodes; mid-test "unauthenticated" responses; race conditions | Use stateless token-based authorization |
| Rate limiting | Requests rejected after a threshold | Environment flag to raise the limit, test-user rotation, or a client-side throttle |

- **Rule 3.6** NEVER use `Thread.sleep` to wait for async state. Poll with a timeout.
- **Rule 3.7** Treat an intermittent failure caused by one of these components as a signal to investigate, not something to retry until it passes.

```java
await().atMost(Duration.ofSeconds(30))
       .pollInterval(Duration.ofSeconds(1))
       .untilAsserted(() ->
               new GetEmployeeRequest(employeeId).send()
                       .then().statusCode(200)
                       .body("status", equalTo("ACTIVE")));
```

### 3.3 Test data

- **Rule 3.8** **Static reference data** (lookup tables, country codes, fixed configuration) is loaded from external configuration files.
- **Rule 3.9** **Dynamic test data** (employees, accounts) is created at run time, with unique values per test so parallel runs do not collide.
- **Rule 3.10** Each test creates its own data. Tests MUST NOT depend on data created by another test.

```java
package com.enterprise.qa.automation.payloads.factories;

public final class EmployeeDataFactory {

    private EmployeeDataFactory() {}

    public static EmployeePayload valid() {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        return new EmployeePayload("QA", "User-" + unique, "qa." + unique + "@enterprise.test", "Engineering");
    }
}
```

### 3.4 Teardown

- **Rule 3.11** Clean up through the **API**, with `Delete...Request` objects. API teardown respects business rules and does not lock the database during parallel runs. The full strategy is in §3.6.
- **Rule 3.12** Direct database resets (`DatabaseUtils`) are only for environment initialization **before** the suite starts.

### 3.5 Authentication

- **Rule 3.13** `TokenManager` is the only place that logs in. It fetches a token on demand, caches it for its valid lifetime, and `RequestSpecFactory` injects it.
- **Rule 3.14** NEVER hardcode tokens and NEVER log in inside individual tests.
- **Rule 3.15** The token cache is thread-safe.
- **Rule 3.16** A failed login throws immediately with the status code. It must not surface later as a 401 in an unrelated test.

```java
package com.enterprise.qa.automation.utils;

public final class TokenManager {

    private static final Logger LOG = LoggerFactory.getLogger(TokenManager.class);
    private static final long REFRESH_MARGIN_SECONDS = 30;
    private static String token;
    private static Instant expiresAt = Instant.MIN;

    private TokenManager() {}

    public static synchronized String getToken() {
        if (token == null || Instant.now().isAfter(expiresAt)) {
            refresh();
        }
        return token;
    }

    private static void refresh() {
        LoginPayload credentials = new LoginPayload(
                EnvironmentConfig.get("auth.username"),
                EnvironmentConfig.get("auth.password"));

        Response response = new LoginRequest(credentials).send();
        if (response.statusCode() != 200) {
            throw new IllegalStateException("Login failed with HTTP " + response.statusCode());
        }

        LoginResponse login = response.as(LoginResponse.class);
        token = login.accessToken();
        expiresAt = Instant.now().plusSeconds(login.expiresIn() - REFRESH_MARGIN_SECONDS);
        LOG.info("Token refreshed, valid for {} s", login.expiresIn());
    }
}
```

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

| Scenario | What the framework does |
|---|---|
| **Partial test execution** (the test fails or throws midway) | Everything created so far is already registered. The listener deletes it after the method ends |
| **Failed setup:** an arrange call returns the wrong status | `sendAsSetup` throws `SetupFailedException`. The test is reported under "Test setup failures". Resources created before the failure are cleaned up |
| **Failed setup:** a `@BeforeMethod` throws | TestNG skips the test. The listener runs cleanup for the failed configuration method |
| **Failed cleanup** | The delete is logged at WARN and attached to the test's Cleanup step. The resource goes on the leaked list. It is retried once when the suite ends. If it still fails: ERROR log, a line in `target/leaked-resources.txt`, and the `leaked.resources` count in the Allure Environment panel |
| **Run killed** (JVM crash, cancelled CI job) | No cleanup ran. The pre-suite sweep of the next run deletes data that carries the automation marker |

CI SHOULD mark the build unstable when `target/leaked-resources.txt` exists.

```java
package com.enterprise.qa.automation.utils;

public class SetupFailedException extends RuntimeException {

    public SetupFailedException(String message) {
        super("Setup failed: " + message);
    }
}
```

```java
package com.enterprise.qa.automation.utils;

public final class CleanupRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(CleanupRegistry.class);
    private static final ThreadLocal<Deque<BaseApiRequest>> PENDING = ThreadLocal.withInitial(ArrayDeque::new);
    private static final Queue<BaseApiRequest> LEAKED = new ConcurrentLinkedQueue<>();

    private CleanupRegistry() {}

    public static void register(BaseApiRequest deleteRequest) {
        PENDING.get().push(deleteRequest);
    }

    /** Called by SuiteLifecycleListener after every test method. Never throws. */
    public static void runPending() {
        Deque<BaseApiRequest> pending = PENDING.get();
        if (pending.isEmpty()) {
            return;
        }
        Allure.step("Cleanup", () -> {
            while (!pending.isEmpty()) {
                BaseApiRequest deleteRequest = pending.pop();
                if (!deleted(deleteRequest)) {
                    LEAKED.add(deleteRequest);
                }
            }
        });
    }

    /** Called once when the suite ends. Returns the resources that are still leaked. */
    public static List<String> retryLeaked() {
        List<String> stillLeaked = new ArrayList<>();
        BaseApiRequest deleteRequest;
        while ((deleteRequest = LEAKED.poll()) != null) {
            if (!deleted(deleteRequest)) {
                stillLeaked.add(deleteRequest.toString());
            }
        }
        return stillLeaked;
    }

    private static boolean deleted(BaseApiRequest deleteRequest) {
        String failure;
        try {
            int status = deleteRequest.send().statusCode();
            if ((status >= 200 && status < 300) || status == 404) {
                return true;
            }
            failure = deleteRequest + " returned HTTP " + status;
        } catch (RuntimeException e) {
            failure = deleteRequest + " threw " + e;
        }
        LOG.warn("Cleanup failed: {}", failure);
        Allure.addAttachment("Cleanup failure", "text/plain", failure);
        return false;
    }
}
```

---

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

### Console log levels

| Event | Level | Logged by |
|---|---|---|
| Suite start: environment and base URI | INFO | `SuiteLifecycleListener` |
| API call: method, URI, status, latency | INFO | `DiagnosticFilter` |
| Full masked request and response | DEBUG | `DiagnosticFilter` |
| Token refreshed (never the token value) | INFO | `TokenManager` |
| API call ended with an exception (timeout, connection) | WARN | `DiagnosticFilter` |
| Test retried | WARN | `TransientRetryAnalyzer` |
| Cleanup failure | WARN | `CleanupRegistry` |
| Resource still leaked after the suite-end retry | ERROR | `SuiteLifecycleListener` |

Each console line carries the thread name and the running test name, so parallel output stays readable.

```xml
<!-- src/test/resources/logback-test.xml -->
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} %-5level [%thread] [%X{test}] %logger{0} - %msg%n</pattern>
        </encoder>
    </appender>
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>
</configuration>
```

```java
package com.enterprise.qa.automation.client;

public final class DiagnosticFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(DiagnosticFilter.class);
    private static final Set<String> MASKED_HEADERS = Set.of("authorization", "cookie", "set-cookie");
    private static final Pattern SECRET_FIELDS = Pattern.compile(
            "(\"(?:password|token|access_token|refresh_token|secret)\"\\s*:\\s*)\"[^\"]*\"",
            Pattern.CASE_INSENSITIVE);

    @Override
    public Response filter(FilterableRequestSpecification request,
                           FilterableResponseSpecification responseSpec,
                           FilterContext context) {
        String requestText = describe(request);
        Allure.addAttachment("Request", "text/plain", requestText);
        LOG.debug("Request:\n{}", requestText);

        Response response;
        try {
            response = context.next(request, responseSpec);
        } catch (RuntimeException e) {
            LOG.warn("{} {} failed: {}", request.getMethod(), request.getURI(), e.toString());
            throw e;
        }

        String responseText = describe(response);
        Allure.addAttachment("Response", "text/plain", responseText);
        LOG.info("{} {} -> {} in {} ms",
                request.getMethod(), request.getURI(), response.getStatusCode(), response.getTime());
        LOG.debug("Response:\n{}", responseText);
        return response;
    }

    private static String describe(FilterableRequestSpecification request) {
        StringBuilder text = new StringBuilder()
                .append(request.getMethod()).append(' ').append(request.getURI()).append('\n');
        request.getHeaders().forEach(h -> appendHeader(text, h));
        if (request.getBody() != null) {
            text.append('\n').append(maskSecrets(String.valueOf((Object) request.getBody())));
        }
        return text.toString();
    }

    private static String describe(Response response) {
        StringBuilder text = new StringBuilder()
                .append(response.getStatusLine()).append('\n')
                .append("Latency: ").append(response.getTime()).append(" ms\n");
        response.getHeaders().forEach(h -> appendHeader(text, h));
        text.append('\n').append(maskSecrets(response.getBody().asPrettyString()));
        return text.toString();
    }

    private static void appendHeader(StringBuilder text, Header header) {
        boolean masked = MASKED_HEADERS.contains(header.getName().toLowerCase());
        text.append(header.getName()).append(": ").append(masked ? "***" : header.getValue()).append('\n');
    }

    private static String maskSecrets(String body) {
        return SECRET_FIELDS.matcher(body).replaceAll("$1\"***\"");
    }
}
```

```properties
# src/test/resources/allure.properties
allure.results.directory=target/allure-results
```

Failure categories for Rule 4.7 (`src/test/resources/categories.json`, copied to the results directory by the listener):

```json
[
  {
    "name": "Test setup failures",
    "matchedStatuses": ["broken"],
    "traceRegex": ".*SetupFailedException.*"
  },
  {
    "name": "Automation infrastructure failures",
    "matchedStatuses": ["broken"],
    "messageRegex": ".*(timed out|Connection refused|Login failed|ConditionTimeout).*"
  },
  {
    "name": "Application defects",
    "matchedStatuses": ["failed"]
  }
]
```

---

## 5. Assertions and validation rules

### Validation scope per tier

| Tier | Responsibility | Validation scope |
|---|---|---|
| Unit (dev-owned) | Isolated logic | Exhaustive negative inputs; field limits; validation error messages |
| Integration (QA) | Inter-service contracts | JSON schema; HTTP status mapping; data passed across services |
| System (QA) | Multi-step workflows | Business outcomes; persisted state; journey success |

### Rule 5.1 — Assertion execution standard

- **Hard assertions MUST be used for execution prerequisites:** status code, `Content-Type`, and token validity. If these fail, stop. Field checks on an error body are noise.
- **Soft assertions MUST be used for multi-field payload verification.** All field failures are collected and reported together at the end of the test.

### Other rules

- **Rule 5.2** Validate the payload contract with JSON Schema at integration level. A `200` alone proves nothing; some services return `200` around an error body.
- **Rule 5.3** Deserialize responses into DTOs for field assertions. A string path such as `"data[0].id"` still compiles after a field is renamed and fails only at run time. Use JsonPath/GPath for quick collection checks.
- **Rule 5.4 (no over-asserting)** Assert business-critical identifiers, schemas, status codes, and state changes. NEVER assert cosmetic text (`"Record updated successfully!"`), static notification messages, or headers with no business value.
- **Rule 5.5** For collections, use `hasItems` or `containsInAnyOrder` unless the contract guarantees order.
- **Rule 5.6** Some secure APIs return `404` instead of `403` for another customer's resource, to hide whether it exists. Assert what the API contract documents.

```java
package com.enterprise.qa.automation.tests.employee;

@Feature("Employee Management")
public class AddEmployeeTest {

    @Test(groups = {"smoke", "regression"})
    @Severity(SeverityLevel.CRITICAL)
    public void addEmployee_createsRecord() {
        EmployeePayload employee = EmployeeDataFactory.valid();

        Response response = new AddEmployeeRequest(employee).send();

        // Rule 5.1 hard assertions + Rule 5.2 contract
        response.then()
                .spec(ResponseSpecFactory.json(201))
                .body(matchesJsonSchemaInClasspath("schemas/employee.schema.json"));

        EmployeeResponse created = response.as(EmployeeResponse.class);

        // Rule 5.1 soft assertions on business fields
        SoftAssert soft = new SoftAssert();
        soft.assertNotNull(created.id(), "id");
        soft.assertEquals(created.firstName(), employee.firstName(), "firstName");
        soft.assertEquals(created.lastName(), employee.lastName(), "lastName");
        soft.assertEquals(created.email(), employee.email(), "email");
        soft.assertEquals(created.department(), employee.department(), "department");
        soft.assertAll();
    }
}
```

The test class extends nothing and has no setup or cleanup method. Base URI, token, headers, logging, and cleanup come from the framework layers. The employee is deleted after the method ends, even if an assertion fails (§3.6).

- **Rule 5.7** A delete test verifies the result twice: the delete status, then a read that confirms the resource is gone. If the API caches reads, poll for the `404` (Rule 3.6).

```java
@Test(groups = {"regression"})
public void deleteEmployee_removesRecord() {
    // Arrange: a failure here is a setup failure, not a product defect
    String employeeId = new AddEmployeeRequest(EmployeeDataFactory.valid())
            .sendAsSetup(201)
            .jsonPath().getString("id");

    new DeleteEmployeeRequest(employeeId).send()
            .then().spec(ResponseSpecFactory.status(204));

    new GetEmployeeRequest(employeeId).send()
            .then().spec(ResponseSpecFactory.status(404));
}
```

Automatic cleanup still runs for this employee and receives `404`, which counts as already gone.

```java
// Collection check with Hamcrest. All expectations in one then() block are reported together.
new SearchEmployeeRequest().department("Engineering").page(1).send()
        .then()
        .spec(ResponseSpecFactory.json(200))
        .body("data.department", everyItem(equalTo("Engineering")))
        .body("data.id", hasItems(createdId));
```

---

## 6. TestNG and execution

### 6.1 Lifecycle without a BaseTest

- **Rule 6.1** Test classes MUST NOT extend a base test class.
- **Rule 6.2** Per-test needs (request defaults, tokens, logging) are provided by the framework layers when a request is sent.
- **Rule 6.3** Suite-wide lifecycle is one TestNG **listener**, `SuiteLifecycleListener`. It validates the environment at start, runs cleanup after every test method, and writes the report files at the end.
- **Rule 6.4** The listener is registered through the Java `ServiceLoader` file, so it applies to every suite and IDE run and cannot be left out of a suite XML.

```text
# src/test/resources/META-INF/services/org.testng.ITestNGListener
com.enterprise.qa.automation.utils.SuiteLifecycleListener
```

```java
package com.enterprise.qa.automation.utils;

public class SuiteLifecycleListener implements ISuiteListener, IInvokedMethodListener {

    private static final Logger LOG = LoggerFactory.getLogger(SuiteLifecycleListener.class);

    @Override
    public void onStart(ISuite suite) {
        // Reading base.uri fails fast on bad configuration
        LOG.info("Suite '{}' starting on '{}' against {}", suite.getName(),
                EnvironmentConfig.environment(), EnvironmentConfig.get("base.uri"));
        // Project-specific: sweep leftover automation data (Rule 3.24), then seed if needed (DatabaseUtils)
    }

    @Override
    public void beforeInvocation(IInvokedMethod method, ITestResult result) {
        MDC.put("test", result.getTestClass().getRealClass().getSimpleName()
                + "." + method.getTestMethod().getMethodName());
    }

    @Override
    public void afterInvocation(IInvokedMethod method, ITestResult result) {
        // After every test method, and after any configuration method that failed
        if (method.isTestMethod() || !result.isSuccess()) {
            CleanupRegistry.runPending();
        }
        MDC.remove("test");
    }

    @Override
    public void onFinish(ISuite suite) {
        List<String> leaked = CleanupRegistry.retryLeaked();
        leaked.forEach(resource -> LOG.error("Leaked resource: {}", resource));
        writeReportFiles(leaked);
    }

    private void writeReportFiles(List<String> leaked) {
        try {
            Path results = Path.of("target", "allure-results");
            Files.createDirectories(results);
            Files.write(results.resolve("environment.properties"), List.of(
                    "env=" + EnvironmentConfig.environment(),
                    "base.uri=" + EnvironmentConfig.get("base.uri"),
                    "leaked.resources=" + leaked.size()));
            try (InputStream categories = getClass().getClassLoader().getResourceAsStream("categories.json")) {
                if (categories != null) {
                    Files.copy(categories, results.resolve("categories.json"), StandardCopyOption.REPLACE_EXISTING);
                }
            }
            if (!leaked.isEmpty()) {
                Files.write(Path.of("target", "leaked-resources.txt"), leaked);
            }
        } catch (IOException e) {
            LOG.error("Could not write report files", e);
        }
    }
}
```

### 6.2 Orchestration

- **Rule 6.5** Tag every test with groups (`smoke`, `regression`, plus feature groups). Define one suite file per purpose.
- **Rule 6.6** Run in parallel. This is safe because request objects are created per test, test data is unique, and specs are never shared mutable state.
- **Rule 6.7** Use `@DataProvider` for data-driven tests, with one DTO per row.
- **Rule 6.8** Retries handle only transient network or environment errors, at most once. NEVER use retries to hide flaky test code or application defects. Move flaky tests to a `quarantine` group and fix them.

```xml
<!-- src/test/resources/suites/regression.xml -->
<!DOCTYPE suite SYSTEM "https://testng.org/testng-1.0.dtd">
<suite name="api-regression" parallel="classes" thread-count="4">
    <test name="regression">
        <groups>
            <run>
                <include name="regression"/>
                <exclude name="quarantine"/>
            </run>
        </groups>
        <packages>
            <package name="com.enterprise.qa.automation.tests.*"/>
        </packages>
    </test>
</suite>
```

```java
package com.enterprise.qa.automation.utils;

public class TransientRetryAnalyzer implements IRetryAnalyzer {

    private static final Logger LOG = LoggerFactory.getLogger(TransientRetryAnalyzer.class);
    private static final int MAX_RETRIES = 1;
    private int attempts;

    @Override
    public boolean retry(ITestResult result) {
        Throwable error = result.getThrowable();
        boolean transientError = error instanceof SocketTimeoutException
                || error instanceof ConnectException;
        boolean retry = transientError && attempts++ < MAX_RETRIES;
        if (retry) {
            LOG.warn("Retrying {} after transient error: {}", result.getName(), error.toString());
        }
        return retry;
    }
}
```

Use it per test: `@Test(retryAnalyzer = TransientRetryAnalyzer.class)`.

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

---

## 7. Anti-patterns to avoid

| # | Anti-pattern | Required practice | Rule |
|---|---|---|---|
| 1 | Folders and suites named after sprints or stories | Feature-based packages | 1.1 |
| 2 | Wrong-level testing: exhaustive negative and 401/403/404 checks at integration or system level | Push down to unit tests or gateway rules | 2.15 |
| 3 | Black-box blindness: designing suites without the architecture diagrams | Study gateway, queues, caches, load balancers, rate limits first | 2.18 |
| 4 | Waterfall deadlines for "100% automation" | Capacity-based sprint planning and PoCs | 6.11 |
| 5 | Over-asserting static strings, messages, and headers | Business-critical assertions; Rule 5.1 | 5.4 |
| 6 | Duplicated coverage across folders | One feature-mapped repository | 1.1 |
| 7 | A `BaseTest` parent class for every test | Composition through framework layers; listeners for suite lifecycle | 6.1 |
| 8 | `given()`, routes, or headers written inside tests | One request class per operation | 2.6 |
| 9 | One "god" endpoint class with every call and loose parameters | One class per request; mandatory inputs in the constructor | 2.1, 2.2 |
| 10 | Request inheritance between verbs (`Put extends Post`) | Extend `BaseApiRequest` only; share DTOs by composition | 2.5 |
| 11 | JSON built by string concatenation | Typed DTOs with Jackson | 2.12 |
| 12 | Hardcoded hosts, ports, or credentials | `EnvironmentConfig` plus environment variables | 3.1 |
| 13 | Logging in inside every test, or hardcoded tokens | Cached `TokenManager` | 3.13 |
| 14 | Static `RestAssured.*` globals | Spec objects | 2.9 |
| 15 | Missing `Content-Type` on requests with a body | Set once in `RequestSpecFactory` | 2.10 |
| 16 | Path parameters confused with query parameters | Path identifies; query filters | 2.7 |
| 17 | `Thread.sleep` for async state | Polling with a timeout | 3.6 |
| 18 | Full database resets between tests | API teardown; DB reset only before the suite | 3.11 |
| 19 | Retries that hide flaky code or defects | Retry transient errors only, once | 6.8 |
| 20 | Secrets in logs or reports | Header and field masking | 4.3 |
| 21 | Single calls labelled as integration scenarios | Multi-step flows that pass state | 2.17 |
| 22 | A heavy framework for a short-lived project | Lightweight tooling | Appendix C |
| 23 | Delete calls whose result is never checked | Verified deletes in tests and in cleanup | 3.19, 5.7 |
| 24 | Cleanup written by hand in each test, or skipped when a test fails midway | Automatic registration on create; listener-driven cleanup | 3.17, 3.18 |
| 25 | A cleanup failure that fails the test or stops other cleanup | Record as leaked, retry at suite end, report separately | 3.20 |
| 26 | Arrange failures reported as product defects | `sendAsSetup` and the "Test setup failures" category | 3.21 |
| 27 | Shared data created in `@BeforeClass` | Data created per test method | 3.22 |
| 28 | `log().all()`, `System.out`, or several logging paths | `DiagnosticFilter` and SLF4J only | 4.4, 4.10 |

---

## Appendix A: HTTP status codes

| Code | Meaning | Typical condition |
|---|---|---|
| 200 OK | Success with body | GET, PUT, PATCH |
| 201 Created | Resource created | POST |
| 202 Accepted | Accepted for async processing | Queue-backed operations; poll for the result |
| 204 No Content | Success without body | DELETE |
| 400 Bad Request | Malformed request | Invalid JSON, missing mandatory parameter |
| 401 Unauthorized | Not authenticated | Missing, invalid, or expired token |
| 403 Forbidden | Authenticated but not permitted | Missing permission |
| 404 Not Found | Route or resource missing | Also used instead of 403 to hide existence |
| 409 Conflict | State conflict | Duplicate creation |
| 415 Unsupported Media Type | Wrong or missing `Content-Type` | Body sent without `application/json` |
| 422 Unprocessable Content | Valid syntax, invalid meaning | Business-rule validation (API-dependent) |
| 429 Too Many Requests | Rate limit reached | Check `Retry-After` |
| 500 Internal Server Error | Unhandled server error | — |
| 502 Bad Gateway | Invalid upstream response | Proxy or gateway problem |
| 503 Service Unavailable | Temporarily unavailable | Overload, maintenance |
| 504 Gateway Timeout | Upstream did not answer in time | — |

---

## Appendix B: Service virtualization with WireMock

Use WireMock when the system under test depends on unstable, rate-limited, or paid third-party services.

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

```json
{
  "request": {
    "method": "GET",
    "urlPath": "/api/users",
    "queryParameters": { "role": { "equalTo": "admin" } },
    "headers": { "Accept": { "contains": "application/json" } }
  },
  "response": {
    "status": 200,
    "bodyFileName": "users_admin.json",
    "headers": { "Content-Type": "application/json" }
  }
}
```

```json
{
  "request": { "method": "POST", "url": "/api/checkout" },
  "response": {
    "status": 504,
    "fixedDelayMilliseconds": 2000,
    "jsonBody": { "error": "Gateway Timeout" }
  }
}
```

---

## Appendix C: Framework vs. lightweight tooling

```text
Savings per regression cycle = (manual time − automated time) × tests per cycle × hourly cost
Break-even cycles            = total investment (build + maintenance) / savings per cycle
```

- **Rule C.1** Short-lived projects (weeks to about three months): use lightweight tooling such as Postman collections. A layered Java framework will not reach break-even before the project ends.
- **Rule C.2** Multi-year products: a layered Java/REST Assured framework pays back through low maintenance, scalability, and CI/CD integration.
- **Rule C.3** Always include maintenance cost in the estimate.
