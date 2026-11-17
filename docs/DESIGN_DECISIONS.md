# Design Decisions

Architecture Decision Records (ADRs) for the restful-booker API test framework. Each record explains one major decision: the context, the options considered, the decision, and its consequences.

- Rules (`Rule x.y`) refer to [STANDARDS.md](STANDARDS.md).
- Overrides (`Ox`) refer to the [Overrides table in the blueprint](../ARCHITECTURE_BLUEPRINT.md#2-overrides).
- Defect IDs (`RB-DEF-xxx`) refer to [Known deviations](../ARCHITECTURE_BLUEPRINT.md#9-known-deviations).

All records have status **Accepted** (2026-11-17).

| ADR | Decision |
|---|---|
| [ADR-001](#adr-001-one-class-per-api-request) | One class per API request |
| [ADR-002](#adr-002-no-basetest-lifecycle-in-one-listener) | No `BaseTest`; lifecycle in one listener |
| [ADR-003](#adr-003-automatic-cleanup-plus-a-marker-sweep) | Automatic cleanup plus a marker sweep |
| [ADR-004](#adr-004-dual-tests-for-api-deviations) | Dual tests for API deviations |
| [ADR-005](#adr-005-configurable-authentication-authtype) | Configurable authentication (`auth.type`) |
| [ADR-006](#adr-006-diagnosticfilter-as-the-single-telemetry-source) | `DiagnosticFilter` as the single telemetry source |
| [ADR-007](#adr-007-records-lombok-builders-and-typed-negative-payloads) | Records, Lombok builders and typed negative payloads |
| [ADR-008](#adr-008-parallel-execution-by-class) | Parallel execution by class |
| [ADR-009](#adr-009-toolchain-and-version-policy) | Toolchain and version policy |

---

## ADR-001: One class per API request

**Related:** Rules 1.3, 2.1–2.6 ([§2.1](STANDARDS.md#21-one-class-per-request)); O5.

### Context
Tests must not depend on HTTP details. When a route, header or payload changes, only one class should need editing (Rule 1.3). A missing mandatory input should be a compile error, not a 400 found at run time.

### Options considered
1. **One "endpoint" class per resource** (for example `BookingApi.create/get/update/...`), with loose parameters. Few classes, but mandatory inputs are easy to forget, and the class grows with every operation.
2. **REST Assured calls directly in tests.** The fastest to write, but routes and headers end up in many places (anti-pattern 8).
3. **One `final` class per operation** extending `BaseApiRequest`. Mandatory inputs are constructor arguments; optional inputs are fluent methods.

### Decision
Option 3. Every operation (`CreateBookingRequest`, `GetBookingIdsRequest`, …) owns its verb and route. `RestClient` is the only class that calls `given()`. Negative-test hooks stay inside the request classes: `withToken(String)` and `CreateBookingRequest.withRawBody(String)` (O5).

### Consequences
- A test cannot build an incomplete request, and an API change touches one class.
- There are more classes (8 request classes for 8 operations), each very small.
- Requests extend only `BaseApiRequest` (Rule 2.5). Shared fields are shared by composition, through the same DTOs.

---

## ADR-002: No BaseTest; lifecycle in one listener

**Related:** Rules 6.1–6.4 ([§6.1](STANDARDS.md#61-lifecycle-without-a-basetest)); O16.

### Context
Suite-wide work (environment check, data sweep, per-test cleanup, report files) must run for every suite and every IDE run. A base class makes every test inherit hidden setup, and it is easy to forget in one class.

### Options considered
1. **A `BaseTest` parent class** with `@BeforeSuite` / `@AfterMethod`. Familiar, but the setup is coupled to inheritance and runs only for subclasses (anti-pattern 7).
2. **Listeners declared in each suite XML.** Explicit, but a suite or IDE run without the declaration silently skips cleanup.
3. **One listener registered through `ServiceLoader`** (`META-INF/services/org.testng.ITestNGListener`).

### Decision
Option 3. `SuiteLifecycleListener` checks health and sweeps leftover data at suite start, runs cleanup after every test method, and writes `environment.properties` and `categories.json` at the end. Test classes extend nothing.

### Consequences
- The lifecycle applies everywhere and cannot be left out.
- Behavior is less visible from the test code; [README](../README.md) and the blueprint document it.
- The service file must name the class with the project's package. A copy of the Standards sample line would fail at start-up.

---

## ADR-003: Automatic cleanup plus a marker sweep

**Related:** Rules 3.9, 3.10, 3.17–3.24 ([§3.6](STANDARDS.md#36-cleanup-strategy)); O3, O12, O16.

### Context
The API is public and shared. Every test creates its own bookings (Rule 3.10), and they must be deleted even when a test fails halfway. A killed CI job leaves data behind that nothing else will remove.

### Options considered
1. **Manual cleanup in each test** (`@AfterMethod` or `finally`). Easy to forget, and it skips resources created before a failure (anti-pattern 24).
2. **No cleanup**, relying on the API's periodic reset. Data piles up between resets, and searches become noisy.
3. **Automatic registration plus a pre-suite sweep.** Every create request returns its delete request through `cleanupFor()`, and every booking carries a marker.

### Decision
Option 3. `RestClient` registers the delete as soon as a create succeeds; `CleanupRegistry` runs registered deletes after each test, in reverse order, and verifies each result (Rule 3.19). All bookings use `lastname = "QA-AUTO"`, and the listener deletes every booking with that marker at suite start. Because restful-booker answers a delete of an already-deleted booking with **405**, `DeleteBookingRequest.isAlreadyGone()` accepts 404 and 405 (O12). Leaked resources are retried at suite end, appended to `target/leaked-resources.txt` and counted in the Allure Environment panel (O16).

### Consequences
- Tests contain no cleanup code, and a failed test does not leak data.
- The sweep deletes **all** `QA-AUTO` bookings, including those of a parallel run on another machine. CI runs are serialized with a `concurrency` group; local runs can use `-Dsweep.enabled=false`.
- Uniqueness comes from the firstname (Datafaker name + UUID suffix), because the lastname is fixed to the marker.

---

## ADR-004: Dual tests for API deviations

**Related:** Rules 2.15, 4.7 ([§4](STANDARDS.md#4-logging-and-allure-reporting)); O6, O7, O22, O24; RB-DEF-001…008.

### Context
Probing showed that restful-booker breaks REST conventions or its own docs in several places (for example 500 for a missing field, 403 for an invalid token). If tests assert the correct behavior, the CI gate is permanently red. If they assert the actual behavior, the defects disappear from view.

### Options considered
1. **Assert REST-correct behavior only.** Honest, but the gate never passes, so real regressions are hidden among known failures.
2. **Assert actual behavior only.** The gate is green, but the defects are documented nowhere in the report.
3. **Dual tests.** An actual-behavior test in the gate, plus a contract test in a separate, non-blocking suite.

### Decision
Option 3, limited to one representative test per negative category (Rule 2.15, O6). Actual-behavior tests are in the `regression` group. Contract tests are in the `known-defect` group, with `@Issue("RB-DEF-xxx")` and messages that start with `[KNOWN-DEFECT]` (O7, O22). `categories.json` lists "Known defects" first, so these failures never mix with new product defects (O24). Deviations outside the O6 categories get the actual-behavior test only; RB-DEF-008 is recorded but not automated.

### Consequences
- The gate stays green and still detects any change in API behavior.
- If the API is fixed, the actual-behavior test turns red and must be updated together with its contract test. That is intentional, but it is maintenance work.
- Responses that differ from REST conventions but match the docs (200 on create, 201 on delete and ping) are notes, not defects, and have no contract tests.

---

## ADR-005: Configurable authentication (`auth.type`)

**Related:** Rules 3.5, 3.13–3.16 ([§3.5](STANDARDS.md#35-authentication)); O4, O15, O23.

### Context
The Standards sample uses a bearer token with `expires_in`. Restful-booker issues a short token without expiry information and expects it as `Cookie: token=<value>`. A bad password returns **200** with a `reason` field instead of 401 (RB-DEF-001).

### Options considered
1. **Hard-code cookie auth** for this API. Simple, but the framework cannot be reused for a bearer API.
2. **Use Basic auth**, which restful-booker also accepts. No login call is needed, but the auth endpoint itself is then never exercised as the framework's login path.
3. **A configurable `auth.type`**: `none`, `cookie-token` or `bearer`.

### Decision
Option 3. `RequestSpecFactory.withToken()` adds the credential for the configured type; `TokenManager` logs in once, caches the token (for the whole run with `cookie-token`, until expiry with `bearer`) and fails fast when the response has no token, not only on a non-200 status. `auth.type` is one of only two optional config keys, defaulting to `none` (O15). Only the tests of `POST /auth` itself call the login request directly (O23).

### Consequences
- One login per run, thread-safe, and a bad password fails the suite immediately with "Login failed".
- The `bearer` path is implemented but has never run against a real bearer API.

---

## ADR-006: DiagnosticFilter as the single telemetry source

**Related:** Rules 4.1–4.5, 4.11 ([§4](STANDARDS.md#4-logging-and-allure-reporting)); O1, O14.

### Context
A CI failure must be diagnosable from the report alone (Rule 4.1), but tokens and passwords must never reach logs or reports (Rule 4.3). REST Assured's own log filters do not mask body fields.

### Options considered
1. **REST Assured `log().all()` / logging filters.** Built in, but no masking, and output is split between the console and the report.
2. **Allure's REST Assured integration.** Rich attachments, but masking would have to be configured separately from console logging.
3. **One custom filter** that writes the same masked data to Allure (full detail) and to SLF4J (summary).

### Decision
Option 3. `DiagnosticFilter` attaches the request and response to Allure, logs a one-line summary per call, and attaches a cURL command for every response with status 400 or higher. It masks `Authorization`, `Cookie` and `Set-Cookie` headers and `password`/`token` fields in bodies. Because the token is sent through REST Assured's cookie API, not as a header, the filter also prints cookies from `request.getCookies()`, with masked values (O14). Logging goes through SLF4J to Log4j2, with the thread and test name on every line (O1).

### Consequences
- Each call appears once, consistently masked, in both the report and `logs/automation.log`.
- Any new secret field name must be added to the filter's masking pattern.
- Outside a test (the suite-setup health check and sweep), calls are logged but not attached, because Allure has no test to attach them to.

---

## ADR-007: Records, Lombok builders and typed negative payloads

**Related:** Rules 1.4, 2.12–2.14 ([§2.4](STANDARDS.md#24-dtos)); O2, O5, O20.

### Context
Request and response bodies must be typed (Rule 2.12). Negative tests need payloads that are deliberately incomplete, mistyped or malformed, without building JSON by string concatenation in tests.

### Options considered
1. **Mutable POJOs with getters and setters.** Flexible, but verbose and easy to mutate by accident.
2. **Plain records.** Immutable and concise, but changing one field means repeating the whole constructor.
3. **Records with Lombok `@Builder(toBuilder = true)`** where variants are needed, plus dedicated DTOs for wrong types.

### Decision
Option 3. DTOs are records; `BookingPayload` and `PartialBookingPayload` have builders. A missing field is `valid().toBuilder().firstname(null)`, omitted by `@JsonInclude(NON_NULL)`. A wrong type uses a typed record, `TextPriceBookingPayload` (with `String totalprice`). Malformed JSON comes only from constants in `MalformedPayloads` (O5). The nested `BookingDates` record is shared by payloads and responses (O20).

### Consequences
- Factories express variations in one line, and test data cannot be mutated by accident.
- Lombok must be on the compiler's annotation processor path, with `<proc>full</proc>` for JDK 23+ (O2).
- Dates are ISO strings rather than `LocalDate`, which avoids extra Jackson modules but moves date arithmetic into the factory.

---

## ADR-008: Parallel execution by class

**Related:** Rules 3.22, 3.23, 6.6, 6.8 ([§6.2](STANDARDS.md#62-orchestration)); O3, O19.

### Context
Running tests in parallel shortens the run, but shared state (static REST Assured config, shared test data, cleanup queues) causes flaky failures.

### Options considered
1. **Sequential execution.** The simplest and safest, but slower.
2. **Parallel by method.** The fastest, but it puts the most pressure on the shared public API.
3. **Parallel by class** (`parallel="classes" thread-count="4"`).

### Decision
Option 3. Request objects and specs are created per call, cleanup queues are `ThreadLocal`, test data is unique per test, and `Faker` instances are per thread (O3). `TransientRetryAnalyzer` retries only socket timeouts and connection failures, once, checking the whole cause chain (O19).

### Consequences
- A regression run takes about 40 seconds against the live API.
- `@BeforeClass` data and `@Test(timeOut)` are not allowed (Rules 3.22, 3.23), because cleanup is bound to the test thread.
- Assertions on shared collections use `hasItem`, not exact lists, unless a unique filter makes the result deterministic.

---

## ADR-009: Toolchain and version policy

**Related:** Rule 1.8 ([§1 Dependencies](STANDARDS.md#dependencies)); O1, O9, O11, O13, O17, O21, O25; [Dependency matrix](../ARCHITECTURE_BLUEPRINT.md#5-dependency-matrix).

### Context
Rule 1.8 asks for the latest stable release of every library. Several libraries have moved to new major versions since the Standards samples were written, and some of those changes break the samples.

### Options considered
1. **Pin the versions used in the Standards samples.** Samples work unchanged, but the project starts on outdated libraries.
2. **Latest stable everywhere, no exceptions.** Up to date, but one library (Surefire 3.6) removes a feature the design depends on.
3. **Latest stable by default, with each exception recorded as an override.**

### Decision
Option 3, on Java 25 (O11). The adaptations are:
- **REST Assured 6.x:** `Duration` timeouts instead of HttpClient 4 parameters (O13).
- **Allure Java 3.x:** `Allure.attachment(...)` replaces `addAttachment(...)` (O9). The HTML report is generated with the Allure 2 command-line tool, which reads `categories.json` and `environment.properties` natively (O17).
- **Log4j2** instead of Logback as the SLF4J backend (O1).
- **`Accept` set to exactly `application/json`**: REST Assured's `ContentType.JSON` sends four media types, and restful-booker answers that with 418 (O21, RB-DEF-008).
- **Surefire pinned to 3.5.6**, the one exception to "latest": 3.6.0+ runs TestNG on the JUnit Platform and no longer reads TestNG suite XML files (O25).

### Consequences
- The project is current on every dependency except Surefire.
- Moving to Surefire 3.6+ later means replacing the suite XMLs with group selection (`-Dgroups`) or a JUnit Platform `@Suite`.
- Every version is listed with its verification date in the blueprint, so the next upgrade starts from a known baseline.
