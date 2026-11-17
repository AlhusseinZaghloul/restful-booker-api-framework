# Test Cases Matrix: restful-booker API

Companion to [ARCHITECTURE_BLUEPRINT.md](ARCHITECTURE_BLUEPRINT.md). Endpoints come from `apidoc/api_data.json` (8 operations). Expected statuses come from the live API probe on 2026-11-17.

**Conventions**
- Class paths are relative to `io.github.alhusseinzaghloul.restfulbooker.tests`. Request and factory names match blueprint §6.
- Every test uses `retryAnalyzer = TransientRetryAnalyzer.class`. Every test creates its own data, and cleanup is automatic (Rules 3.10, 3.17).
- "setup:" means an arrange call made with `sendAsSetup(<status>)` (Rule 3.21).
- `BDF` = `BookingDataFactory`, `CDF` = `CredentialsDataFactory`, `MP` = `MalformedPayloads`.
- Assertions: status and content type are **hard** checks; multi-field checks are **soft** (`SoftAssert`) (Rule 5.1). Schemas live in `src/test/resources/schemas/` (Rule 5.2).
- `known-defect` tests are never in the `regression` group (O7). Their messages start with `[KNOWN-DEFECT] RB-DEF-xxx` (O22).
- Allure `@Feature` is per class: `CheckHealthTest` → Health, `CreateTokenTest` → Auth, everything else → Booking. The Feature / Severity column shows `@Story` / `@Severity`.

---

## 1. End-to-end flows (Rule 2.17)

| Test ID | Endpoint & Method | Test class#method | Data (factory method) | Expected status | Key assertions | Groups | Feature / Severity | Defect ID |
|---|---|---|---|---|---|---|---|---|
| TC-E2E-001 | `POST /booking` → `GET /booking/{id}` → `PUT /booking/{id}` → `PATCH /booking/{id}` → `DELETE /booking/{id}` → `GET /booking/{id}` | `booking.ManageBookingTest#bookingLifecycle_createReadUpdatePatchDelete` | `BDF.valid()`, `BDF.updated(original)`, `BDF.partialUpdate()` | 200 → 200 → 200 → 200 → 201 → 404 | `bookingid` from the create is reused in every later call. GET equals the created payload (soft). The PUT body and the following GET equal the updated payload (soft). After PATCH, only `firstname`/`totalprice` changed and the rest equals the PUT state (soft). The DELETE status is checked, then GET returns 404 (Rule 5.7) | `smoke`, `regression`, `booking`, `e2e` | Booking lifecycle / CRITICAL | — |
| TC-E2E-002 | `POST /booking` → `GET /booking?firstname&lastname` → `DELETE /booking/{id}` → `GET /booking?firstname&lastname` | `booking.ManageBookingTest#createdBooking_appearsInSearch_untilDeleted` | `BDF.valid()` | 200 → 200 → 201 → 200 | The first search's `bookingid` list `contains(id)` (unique firstname). The search after the delete returns an empty list | `regression`, `booking`, `e2e` | Booking search / NORMAL | — |
| TC-E2E-003 | `POST /auth` → `POST /booking` → `PUT /booking/{id}` → `GET /booking/{id}` | `booking.ManageBookingTest#issuedToken_authorizesUpdate` | `CDF.fromConfig()`, setup: `BDF.valid()`, `BDF.updated(original)` | 200 → 200 (setup) → 200 → 200 | `token` taken from `/auth` (not from `TokenManager`) is passed with `UpdateBookingRequest.withToken(token)`. PUT returns 200, and GET equals the updated payload (soft) | `regression`, `auth`, `booking`, `e2e` | Auth → Booking / CRITICAL | — |

---

## 2. Positive functional cases

| Test ID | Endpoint & Method | Test class#method | Data (factory method) | Expected status | Key assertions | Groups | Feature / Severity | Defect ID |
|---|---|---|---|---|---|---|---|---|
| TC-HLTH-001 | `GET /ping` | `health.CheckHealthTest#ping_returnsCreated` | — | 201 | `ResponseSpecFactory.status(201)` only (the body is the text `Created`, which is cosmetic, Rule 5.4) | `smoke`, `regression`, `health` | Health check / BLOCKER | — |
| TC-AUTH-001 | `POST /auth` | `auth.CreateTokenTest#createToken_validCredentials_returnsToken` | `CDF.fromConfig()` | 200 | `json(200)` (hard); `TokenResponse.token` is not blank and `reason` is null (soft) | `smoke`, `regression`, `auth` | Create token / BLOCKER | — |
| TC-BKG-001 | `POST /booking` | `booking.CreateBookingTest#createBooking_validPayload_returnsCreatedBooking` | `BDF.valid()` | 200 | `json(200)` (hard); `bookingId > 0`; `booking` equals the payload field by field, including `bookingdates` (soft) | `smoke`, `regression`, `booking` | Create booking / CRITICAL | — |
| TC-BKG-002 | `GET /booking/{id}` | `booking.GetBookingTest#getBooking_existingId_returnsBooking` | setup: `BDF.valid()` | 200 | `json(200)`; every `BookingResponse` field equals the payload (soft) | `smoke`, `regression`, `booking` | Get booking / CRITICAL | — |
| TC-BKG-003 | `GET /booking` | `booking.GetBookingIdsTest#getBookingIds_noFilter_containsCreatedId` | setup: `BDF.valid()` | 200 | `json(200)`; `bookingid` `hasItem(id)` (Rule 5.5) | `regression`, `booking` | Search bookings / NORMAL | — |
| TC-BKG-004 | `GET /booking?firstname=&lastname=` | `booking.GetBookingIdsTest#getBookingIds_byFirstnameAndLastname_returnsOnlyCreatedId` | setup: `BDF.valid()` | 200 | `bookingid` `contains(id)` (exactly one; the firstname is unique) | `regression`, `booking` | Search bookings / NORMAL | — |
| TC-BKG-005 | `GET /booking?firstname=` | `booking.GetBookingIdsTest#getBookingIds_byFirstname_containsCreatedId` | setup: `BDF.valid()` | 200 | `hasItem(id)` | `regression`, `booking` | Search bookings / NORMAL | — |
| TC-BKG-006 | `GET /booking?lastname=` | `booking.GetBookingIdsTest#getBookingIds_byLastname_containsCreatedId` | setup: `BDF.valid()` (lastname = `QA-AUTO`) | 200 | `hasItem(id)` (other runs may add more) | `regression`, `booking` | Search bookings / NORMAL | — |
| TC-BKG-007 | `GET /booking?checkin=` | `booking.GetBookingIdsTest#getBookingIds_byCheckinDayBefore_containsCreatedId` | setup: `BDF.valid()`; `checkin = BDF.shiftDays(checkin, -1)` | 200 | `hasItem(id)` (true for both documented `>=` and actual `>`) | `regression`, `booking` | Search bookings / NORMAL | — |
| TC-BKG-008 | `GET /booking?checkout=` | `booking.GetBookingIdsTest#getBookingIds_byCheckoutSameDay_containsCreatedId` | setup: `BDF.valid()`; `checkout` = the booking's checkout | 200 | `hasItem(id)` (true for both documented `>=` and actual `<=`) | `regression`, `booking` | Search bookings / NORMAL | — |
| TC-BKG-009 | `GET /booking?firstname=&checkin=&checkout=` | `booking.GetBookingIdsTest#getBookingIds_byCombinedFilters_returnsOnlyCreatedId` | setup: `BDF.valid()`; checkin −1 day, checkout same day | 200 | `contains(id)` | `regression`, `booking` | Search bookings / NORMAL | — |
| TC-BKG-010 | `GET /booking?firstname=&checkin=` | `booking.GetBookingIdsTest#getBookingIds_checkinSameDay_actualBehavior_excludesBooking` | setup: `BDF.valid()`; `checkin` = the booking's checkin | 200 | Actual behavior: the list is **empty** (strict `>`). The docs say `>=`, which would include it | `regression`, `booking` | Search bookings / MINOR | RB-DEF-006 |
| TC-BKG-011 | `GET /booking?firstname=&checkout=` | `booking.GetBookingIdsTest#getBookingIds_checkoutDayAfter_actualBehavior_includesBooking` | setup: `BDF.valid()`; `checkout = BDF.shiftDays(checkout, +1)` | 200 | Actual behavior: `contains(id)` (inverted `<=`). The docs say `>=`, which would exclude it | `regression`, `booking` | Search bookings / MINOR | RB-DEF-007 |
| TC-BKG-012 | `PUT /booking/{id}` | `booking.UpdateBookingTest#updateBooking_fullPayload_replacesBooking` | setup: `BDF.valid()`; `BDF.updated(original)` | 200 | `json(200)`; the response equals the updated payload (soft); a later `GET` equals the updated payload (soft, persisted state) | `regression`, `booking` | Update booking / CRITICAL | — |
| TC-BKG-013 | `PATCH /booking/{id}` | `booking.PartialUpdateBookingTest#partialUpdateBooking_firstnameAndPrice_updatesOnlyThose` | setup: `BDF.valid()`; `BDF.partialUpdate()` | 200 | `json(200)`; `firstname`/`totalprice` equal the patch, and `lastname`/`depositpaid`/`bookingdates`/`additionalneeds` equal the original (soft); `GET` confirms (soft) | `regression`, `booking` | Partial update booking / NORMAL | — |
| TC-BKG-014 | `DELETE /booking/{id}` | `booking.DeleteBookingTest#deleteBooking_existingId_removesBooking` | setup: `BDF.valid()` | 201, then GET 404 | `status(201)`, then `GetBookingRequest` `status(404)` (Rule 5.7). Cleanup later gets 405 and counts it as already gone (O12) | `regression`, `booking` | Delete booking / CRITICAL | — |

---

## 3. Negative cases (O6 scope, O7 dual tests)

| Test ID | Endpoint & Method | Test class#method | Data (factory method) | Expected status | Key assertions | Groups | Feature / Severity | Defect ID |
|---|---|---|---|---|---|---|---|---|
| TC-NEG-001 | `POST /auth` | `auth.CreateTokenTest#createToken_badCredentials_actualBehavior_returns200WithReason` | `CDF.invalid()` | 200 | Actual behavior: `json(200)`; `reason` is not blank and `token` is null (soft). The reason text is not asserted (Rule 5.4) | `regression`, `auth`, `negative` | Create token / NORMAL | RB-DEF-001 |
| TC-NEG-002 | `POST /booking` | `booking.CreateBookingTest#createBooking_missingFirstname_actualBehavior_returns500` | `BDF.missingFirstname()` | 500 | Actual behavior: `status(500)` | `regression`, `booking`, `negative` | Create booking / NORMAL | RB-DEF-002 |
| TC-NEG-003 | `POST /booking` | `booking.CreateBookingTest#createBooking_missingFirstname_contract_returns400` | `BDF.missingFirstname()` | 400 (REST) | `Assert.assertEquals(status, 400, "[KNOWN-DEFECT] RB-DEF-002: …")` | `known-defect`, `booking`, `negative` | Create booking / NORMAL | RB-DEF-002 |
| TC-NEG-004 | `POST /booking` | `booking.CreateBookingTest#createBooking_textTotalprice_actualBehavior_storesNullPrice` | `BDF.textPrice()` | 200 | Actual behavior: `json(200)`; `booking.totalprice` is null; the other fields equal the payload (soft). The booking is cleaned up automatically | `regression`, `booking`, `negative` | Create booking / NORMAL | RB-DEF-003 |
| TC-NEG-005 | `POST /booking` | `booking.CreateBookingTest#createBooking_textTotalprice_contract_returns400` | `BDF.textPrice()` | 400 (REST) | `Assert.assertEquals(status, 400, "[KNOWN-DEFECT] RB-DEF-003: …")` | `known-defect`, `booking`, `negative` | Create booking / NORMAL | RB-DEF-003 |
| TC-NEG-006 | `POST /booking` | `booking.CreateBookingTest#createBooking_malformedJson_returns400` | `CreateBookingRequest.withRawBody(MP.TRUNCATED_BOOKING_JSON)` | 400 | `status(400)` (conforms; no dual test) | `regression`, `booking`, `negative` | Create booking / NORMAL | — |
| TC-NEG-007 | `DELETE /booking/{id}` | `booking.DeleteBookingTest#deleteBooking_nonExistentId_actualBehavior_returns405` | `BDF.NON_EXISTENT_ID` | 405 | Actual behavior: `status(405)` | `regression`, `booking`, `negative` | Delete booking / NORMAL | RB-DEF-005 |
| TC-NEG-008 | `DELETE /booking/{id}` | `booking.DeleteBookingTest#deleteBooking_nonExistentId_contract_returns404` | `BDF.NON_EXISTENT_ID` | 404 (REST) | `Assert.assertEquals(status, 404, "[KNOWN-DEFECT] RB-DEF-005: …")` | `known-defect`, `booking`, `negative` | Delete booking / NORMAL | RB-DEF-005 |
| TC-NEG-009 | `PUT /booking/{id}` | `booking.UpdateBookingTest#updateBooking_invalidToken_actualBehavior_returns403` | setup: `BDF.valid()`; `BDF.updated(original)`; `.withToken(CDF.INVALID_TOKEN)` | 403 | Actual behavior: `status(403)`; a later `GET` still equals the original payload (soft, no state change) | `regression`, `booking`, `auth`, `negative` | Update booking / CRITICAL | RB-DEF-004 |
| TC-NEG-010 | `PUT /booking/{id}` | `booking.UpdateBookingTest#updateBooking_invalidToken_contract_returns401` | setup: `BDF.valid()`; `BDF.updated(original)`; `.withToken(CDF.INVALID_TOKEN)` | 401 (REST) | `Assert.assertEquals(status, 401, "[KNOWN-DEFECT] RB-DEF-004: …")` | `known-defect`, `booking`, `auth`, `negative` | Update booking / CRITICAL | RB-DEF-004 |

---

## 4. Contract / schema cases (Rule 5.2)

| Test ID | Endpoint & Method | Test class#method | Data (factory method) | Expected status | Key assertions | Groups | Feature / Severity | Defect ID |
|---|---|---|---|---|---|---|---|---|
| TC-SCH-001 | `POST /auth` | `auth.CreateTokenTest#createToken_response_matchesTokenSchema` | `CDF.fromConfig()` | 200 | `json(200)` + `matchesJsonSchemaInClasspath("schemas/token.schema.json")` | `regression`, `contract`, `auth` | Create token / NORMAL | — |
| TC-SCH-002 | `POST /auth` | `auth.CreateTokenTest#createToken_badCredentialsResponse_matchesAuthFailureSchema` | `CDF.invalid()` | 200 | `json(200)` + `schemas/auth-failure.schema.json` | `regression`, `contract`, `auth` | Create token / MINOR | RB-DEF-001 |
| TC-SCH-003 | `POST /booking` | `booking.CreateBookingTest#createBooking_response_matchesCreatedBookingSchema` | `BDF.valid()` | 200 | `json(200)` + `schemas/created-booking.schema.json` | `regression`, `contract`, `booking` | Create booking / NORMAL | — |
| TC-SCH-004 | `GET /booking/{id}` | `booking.GetBookingTest#getBooking_response_matchesBookingSchema` | setup: `BDF.valid()` | 200 | `json(200)` + `schemas/booking.schema.json` | `regression`, `contract`, `booking` | Get booking / NORMAL | — |
| TC-SCH-005 | `GET /booking` | `booking.GetBookingIdsTest#getBookingIds_response_matchesBookingIdsSchema` | setup: `BDF.valid()`; filter `firstname` | 200 | `json(200)` + `schemas/booking-ids.schema.json` | `regression`, `contract`, `booking` | Search bookings / NORMAL | — |
| TC-SCH-006 | `PUT /booking/{id}` | `booking.UpdateBookingTest#updateBooking_response_matchesBookingSchema` | setup: `BDF.valid()`; `BDF.updated(original)` | 200 | `json(200)` + `schemas/booking.schema.json` | `regression`, `contract`, `booking` | Update booking / NORMAL | — |
| TC-SCH-007 | `PATCH /booking/{id}` | `booking.PartialUpdateBookingTest#partialUpdateBooking_response_matchesBookingSchema` | setup: `BDF.valid()`; `BDF.partialUpdate()` | 200 | `json(200)` + `schemas/booking.schema.json` | `regression`, `contract`, `booking` | Partial update booking / NORMAL | — |

`GET /ping` and `DELETE /booking/{id}` return `text/plain` (`Created`), so they have no JSON schema.

---

## 5. Coverage summary

**Per endpoint.** "Dedicated" means tests whose act step targets the endpoint. "Also exercised" lists other tests that call it as a step.

| Endpoint | Dedicated tests | Also exercised in |
|---|---|---|
| `GET /ping` | 1 (TC-HLTH-001) | `SuiteLifecycleListener` health check on every run |
| `POST /auth` | 4 (TC-AUTH-001, TC-NEG-001, TC-SCH-001, TC-SCH-002) | TC-E2E-003; `TokenManager` |
| `POST /booking` | 7 (TC-BKG-001, TC-NEG-002…006, TC-SCH-003) | TC-E2E-001…003, setup of 19 other tests |
| `GET /booking/{id}` | 2 (TC-BKG-002, TC-SCH-004) | TC-E2E-001, TC-E2E-003, TC-BKG-012…014, TC-NEG-009 |
| `GET /booking` | 10 (TC-BKG-003…011, TC-SCH-005) | TC-E2E-002; pre-suite sweep |
| `PUT /booking/{id}` | 4 (TC-BKG-012, TC-NEG-009, TC-NEG-010, TC-SCH-006) | TC-E2E-001, TC-E2E-003 |
| `PATCH /booking/{id}` | 2 (TC-BKG-013, TC-SCH-007) | TC-E2E-001 |
| `DELETE /booking/{id}` | 3 (TC-BKG-014, TC-NEG-007, TC-NEG-008) | TC-E2E-001, TC-E2E-002; automatic cleanup and sweep |
| **Total** | **33 + 3 E2E = 36 test methods** | All 8 documented endpoints are covered |

**Per category and suite**

| Category | Tests | In `regression.xml` | In `known-defects.xml` | In `smoke.xml` |
|---|---|---|---|---|
| End-to-end | 3 | 3 | 0 | 1 (TC-E2E-001) |
| Positive functional | 16 | 16 | 0 | 4 (TC-HLTH-001, TC-AUTH-001, TC-BKG-001, TC-BKG-002) |
| Negative | 10 | 6 | 4 | 0 |
| Contract / schema | 7 | 7 | 0 | 0 |
| **Total** | **36** | **32** | **4** | **5** |

**Per test class**: `CheckHealthTest` 1, `CreateTokenTest` 4, `CreateBookingTest` 7, `GetBookingTest` 2, `GetBookingIdsTest` 10, `UpdateBookingTest` 4, `PartialUpdateBookingTest` 2, `DeleteBookingTest` 3, `ManageBookingTest` 3.

**Deviations covered**: 7 (RB-DEF-001…007). 4 have dual tests (002, 003, 004, 005); 3 have actual-behavior tests only (001, 006, 007 are outside O6). Plus 3 REST-convention notes (RB-NOTE-001…003, docs-conformant, not defects; see blueprint §9).
