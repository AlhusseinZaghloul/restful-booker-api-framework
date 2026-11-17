package io.github.alhusseinzaghloul.restfulbooker.tests.booking;

import io.github.alhusseinzaghloul.restfulbooker.endpoints.booking.CreateBookingRequest;
import io.github.alhusseinzaghloul.restfulbooker.endpoints.booking.GetBookingIdsRequest;
import io.github.alhusseinzaghloul.restfulbooker.payloads.factories.BookingDataFactory;
import io.github.alhusseinzaghloul.restfulbooker.payloads.pojos.BookingPayload;
import io.github.alhusseinzaghloul.restfulbooker.payloads.pojos.CreatedBookingResponse;
import io.github.alhusseinzaghloul.restfulbooker.specs.ResponseSpecFactory;
import io.github.alhusseinzaghloul.restfulbooker.utils.TransientRetryAnalyzer;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.qameta.allure.TmsLink;
import org.testng.annotations.Test;

import static io.github.alhusseinzaghloul.restfulbooker.payloads.factories.BookingDataFactory.shiftDays;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;

@Epic("Restful Booker")
@Feature("Booking")
public class GetBookingIdsTest {

    private record Created(int id, BookingPayload booking) {
        String checkin()  { return booking.bookingdates().checkin(); }
        String checkout() { return booking.bookingdates().checkout(); }
    }

    @Test(groups = {"regression", "booking"},
            description = "TC-BKG-003 GET /booking without filters contains the created id",
            retryAnalyzer = TransientRetryAnalyzer.class)
    @TmsLink("TC-BKG-003")
    @Story("Search bookings")
    @Severity(SeverityLevel.NORMAL)
    public void getBookingIds_noFilter_containsCreatedId() {
        Created created = createBooking();

        new GetBookingIdsRequest().send()
                .then()
                .spec(ResponseSpecFactory.json(200))
                .body("bookingid", hasItem(created.id()));
    }

    @Test(groups = {"regression", "booking"},
            description = "TC-BKG-004 GET /booking?firstname&lastname returns only the created id",
            retryAnalyzer = TransientRetryAnalyzer.class)
    @TmsLink("TC-BKG-004")
    @Story("Search bookings")
    @Severity(SeverityLevel.NORMAL)
    public void getBookingIds_byFirstnameAndLastname_returnsOnlyCreatedId() {
        Created created = createBooking();

        new GetBookingIdsRequest()
                .firstname(created.booking().firstname())
                .lastname(created.booking().lastname())
                .send()
                .then()
                .spec(ResponseSpecFactory.json(200))
                .body("bookingid", contains(created.id()));
    }

    @Test(groups = {"regression", "booking"},
            description = "TC-BKG-005 GET /booking?firstname contains the created id",
            retryAnalyzer = TransientRetryAnalyzer.class)
    @TmsLink("TC-BKG-005")
    @Story("Search bookings")
    @Severity(SeverityLevel.NORMAL)
    public void getBookingIds_byFirstname_containsCreatedId() {
        Created created = createBooking();

        new GetBookingIdsRequest().firstname(created.booking().firstname()).send()
                .then()
                .spec(ResponseSpecFactory.json(200))
                .body("bookingid", hasItem(created.id()));
    }

    @Test(groups = {"regression", "booking"},
            description = "TC-BKG-006 GET /booking?lastname contains the created id",
            retryAnalyzer = TransientRetryAnalyzer.class)
    @TmsLink("TC-BKG-006")
    @Story("Search bookings")
    @Severity(SeverityLevel.NORMAL)
    public void getBookingIds_byLastname_containsCreatedId() {
        Created created = createBooking();

        new GetBookingIdsRequest().lastname(created.booking().lastname()).send()
                .then()
                .spec(ResponseSpecFactory.json(200))
                .body("bookingid", hasItem(created.id()));
    }

    @Test(groups = {"regression", "booking"},
            description = "TC-BKG-007 GET /booking?checkin=<day before> contains the created id",
            retryAnalyzer = TransientRetryAnalyzer.class)
    @TmsLink("TC-BKG-007")
    @Story("Search bookings")
    @Severity(SeverityLevel.NORMAL)
    public void getBookingIds_byCheckinDayBefore_containsCreatedId() {
        Created created = createBooking();

        new GetBookingIdsRequest().checkin(shiftDays(created.checkin(), -1)).send()
                .then()
                .spec(ResponseSpecFactory.json(200))
                .body("bookingid", hasItem(created.id()));
    }

    @Test(groups = {"regression", "booking"},
            description = "TC-BKG-008 GET /booking?checkout=<same day> contains the created id",
            retryAnalyzer = TransientRetryAnalyzer.class)
    @TmsLink("TC-BKG-008")
    @Story("Search bookings")
    @Severity(SeverityLevel.NORMAL)
    public void getBookingIds_byCheckoutSameDay_containsCreatedId() {
        Created created = createBooking();

        new GetBookingIdsRequest().checkout(created.checkout()).send()
                .then()
                .spec(ResponseSpecFactory.json(200))
                .body("bookingid", hasItem(created.id()));
    }

    @Test(groups = {"regression", "booking"},
            description = "TC-BKG-009 GET /booking?firstname&checkin&checkout returns only the created id",
            retryAnalyzer = TransientRetryAnalyzer.class)
    @TmsLink("TC-BKG-009")
    @Story("Search bookings")
    @Severity(SeverityLevel.NORMAL)
    public void getBookingIds_byCombinedFilters_returnsOnlyCreatedId() {
        Created created = createBooking();

        new GetBookingIdsRequest()
                .firstname(created.booking().firstname())
                .checkin(shiftDays(created.checkin(), -1))
                .checkout(created.checkout())
                .send()
                .then()
                .spec(ResponseSpecFactory.json(200))
                .body("bookingid", contains(created.id()));
    }

    @Test(groups = {"regression", "booking"},
            description = "TC-BKG-010 GET /booking?checkin=<same day>: actual behavior excludes the booking (RB-DEF-006)",
            retryAnalyzer = TransientRetryAnalyzer.class)
    @TmsLink("TC-BKG-010")
    @Issue("RB-DEF-006")
    @Story("Search bookings")
    @Severity(SeverityLevel.MINOR)
    public void getBookingIds_checkinSameDay_actualBehavior_excludesBooking() {
        Created created = createBooking();

        new GetBookingIdsRequest()
                .firstname(created.booking().firstname())
                .checkin(created.checkin())
                .send()
                .then()
                .spec(ResponseSpecFactory.json(200))
                .body("bookingid", empty());
    }

    @Test(groups = {"regression", "booking"},
            description = "TC-BKG-011 GET /booking?checkout=<day after>: actual behavior includes the booking (RB-DEF-007)",
            retryAnalyzer = TransientRetryAnalyzer.class)
    @TmsLink("TC-BKG-011")
    @Issue("RB-DEF-007")
    @Story("Search bookings")
    @Severity(SeverityLevel.MINOR)
    public void getBookingIds_checkoutDayAfter_actualBehavior_includesBooking() {
        Created created = createBooking();

        new GetBookingIdsRequest()
                .firstname(created.booking().firstname())
                .checkout(shiftDays(created.checkout(), 1))
                .send()
                .then()
                .spec(ResponseSpecFactory.json(200))
                .body("bookingid", contains(created.id()));
    }

    @Test(groups = {"regression", "contract", "booking"},
            description = "TC-SCH-005 GET /booking response matches booking-ids.schema.json",
            retryAnalyzer = TransientRetryAnalyzer.class)
    @TmsLink("TC-SCH-005")
    @Story("Search bookings")
    @Severity(SeverityLevel.NORMAL)
    public void getBookingIds_response_matchesBookingIdsSchema() {
        Created created = createBooking();

        new GetBookingIdsRequest().firstname(created.booking().firstname()).send()
                .then()
                .spec(ResponseSpecFactory.json(200))
                .body(matchesJsonSchemaInClasspath("schemas/booking-ids.schema.json"));
    }

    private static Created createBooking() {
        BookingPayload booking = BookingDataFactory.valid();
        int id = new CreateBookingRequest(booking).sendAsSetup(200)
                .as(CreatedBookingResponse.class).bookingId();
        return new Created(id, booking);
    }
}
