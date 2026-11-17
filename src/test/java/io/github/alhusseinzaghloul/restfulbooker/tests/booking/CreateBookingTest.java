package io.github.alhusseinzaghloul.restfulbooker.tests.booking;

import io.github.alhusseinzaghloul.restfulbooker.endpoints.booking.CreateBookingRequest;
import io.github.alhusseinzaghloul.restfulbooker.payloads.factories.BookingDataFactory;
import io.github.alhusseinzaghloul.restfulbooker.payloads.factories.MalformedPayloads;
import io.github.alhusseinzaghloul.restfulbooker.payloads.pojos.BookingPayload;
import io.github.alhusseinzaghloul.restfulbooker.payloads.pojos.BookingResponse;
import io.github.alhusseinzaghloul.restfulbooker.payloads.pojos.CreatedBookingResponse;
import io.github.alhusseinzaghloul.restfulbooker.payloads.pojos.TextPriceBookingPayload;
import io.github.alhusseinzaghloul.restfulbooker.specs.ResponseSpecFactory;
import io.github.alhusseinzaghloul.restfulbooker.utils.TransientRetryAnalyzer;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.qameta.allure.TmsLink;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;

@Epic("Restful Booker")
@Feature("Booking")
public class CreateBookingTest {

    @Test(groups = {"smoke", "regression", "booking"},
            description = "TC-BKG-001 POST /booking with a valid payload returns the created booking",
            retryAnalyzer = TransientRetryAnalyzer.class)
    @TmsLink("TC-BKG-001")
    @Story("Create booking")
    @Severity(SeverityLevel.CRITICAL)
    public void createBooking_validPayload_returnsCreatedBooking() {
        BookingPayload booking = BookingDataFactory.valid();

        Response response = new CreateBookingRequest(booking).send();

        response.then().spec(ResponseSpecFactory.json(200)); // documented 200 (RB-NOTE-001)
        CreatedBookingResponse created = response.as(CreatedBookingResponse.class);

        SoftAssert soft = new SoftAssert();
        soft.assertTrue(created.bookingId() > 0, "bookingid > 0");
        assertBookingEquals(soft, created.booking(), booking);
        soft.assertAll();
    }

    @Test(groups = {"regression", "booking", "negative"},
            description = "TC-NEG-002 POST /booking without firstname: actual behavior is 500 (RB-DEF-002)",
            retryAnalyzer = TransientRetryAnalyzer.class)
    @TmsLink("TC-NEG-002")
    @Issue("RB-DEF-002")
    @Story("Create booking")
    @Severity(SeverityLevel.NORMAL)
    public void createBooking_missingFirstname_actualBehavior_returns500() {
        new CreateBookingRequest(BookingDataFactory.missingFirstname()).send()
                .then().spec(ResponseSpecFactory.status(500));
    }

    @Test(groups = {"known-defect", "booking", "negative"},
            description = "TC-NEG-003 POST /booking without firstname should return 400 (RB-DEF-002)",
            retryAnalyzer = TransientRetryAnalyzer.class)
    @TmsLink("TC-NEG-003")
    @Issue("RB-DEF-002")
    @Story("Create booking")
    @Severity(SeverityLevel.NORMAL)
    public void createBooking_missingFirstname_contract_returns400() {
        int status = new CreateBookingRequest(BookingDataFactory.missingFirstname()).send().statusCode();

        Assert.assertEquals(status, 400,
                "[KNOWN-DEFECT] RB-DEF-002: POST /booking without a required field should return 400");
    }

    @Test(groups = {"regression", "booking", "negative"},
            description = "TC-NEG-004 POST /booking with text totalprice: actual behavior stores a null price (RB-DEF-003)",
            retryAnalyzer = TransientRetryAnalyzer.class)
    @TmsLink("TC-NEG-004")
    @Issue("RB-DEF-003")
    @Story("Create booking")
    @Severity(SeverityLevel.NORMAL)
    public void createBooking_textTotalprice_actualBehavior_storesNullPrice() {
        TextPriceBookingPayload booking = BookingDataFactory.textPrice();

        Response response = new CreateBookingRequest(booking).send();

        response.then().spec(ResponseSpecFactory.json(200));
        BookingResponse stored = response.as(CreatedBookingResponse.class).booking();

        SoftAssert soft = new SoftAssert();
        soft.assertNull(stored.totalprice(), "totalprice");
        soft.assertEquals(stored.firstname(), booking.firstname(), "firstname");
        soft.assertEquals(stored.lastname(), booking.lastname(), "lastname");
        soft.assertEquals(stored.depositpaid(), booking.depositpaid(), "depositpaid");
        soft.assertEquals(stored.bookingdates(), booking.bookingdates(), "bookingdates");
        soft.assertEquals(stored.additionalneeds(), booking.additionalneeds(), "additionalneeds");
        soft.assertAll();
    }

    @Test(groups = {"known-defect", "booking", "negative"},
            description = "TC-NEG-005 POST /booking with text totalprice should return 400 (RB-DEF-003)",
            retryAnalyzer = TransientRetryAnalyzer.class)
    @TmsLink("TC-NEG-005")
    @Issue("RB-DEF-003")
    @Story("Create booking")
    @Severity(SeverityLevel.NORMAL)
    public void createBooking_textTotalprice_contract_returns400() {
        int status = new CreateBookingRequest(BookingDataFactory.textPrice()).send().statusCode();

        Assert.assertEquals(status, 400,
                "[KNOWN-DEFECT] RB-DEF-003: POST /booking with a text totalprice should return 400");
    }

    @Test(groups = {"regression", "booking", "negative"},
            description = "TC-NEG-006 POST /booking with malformed JSON returns 400",
            retryAnalyzer = TransientRetryAnalyzer.class)
    @TmsLink("TC-NEG-006")
    @Story("Create booking")
    @Severity(SeverityLevel.NORMAL)
    public void createBooking_malformedJson_returns400() {
        CreateBookingRequest.withRawBody(MalformedPayloads.TRUNCATED_BOOKING_JSON).send()
                .then().spec(ResponseSpecFactory.status(400));
    }

    @Test(groups = {"regression", "contract", "booking"},
            description = "TC-SCH-003 POST /booking response matches created-booking.schema.json",
            retryAnalyzer = TransientRetryAnalyzer.class)
    @TmsLink("TC-SCH-003")
    @Story("Create booking")
    @Severity(SeverityLevel.NORMAL)
    public void createBooking_response_matchesCreatedBookingSchema() {
        new CreateBookingRequest(BookingDataFactory.valid()).send()
                .then()
                .spec(ResponseSpecFactory.json(200))
                .body(matchesJsonSchemaInClasspath("schemas/created-booking.schema.json"));
    }

    private static void assertBookingEquals(SoftAssert soft, BookingResponse actual, BookingPayload expected) {
        soft.assertEquals(actual.firstname(), expected.firstname(), "firstname");
        soft.assertEquals(actual.lastname(), expected.lastname(), "lastname");
        soft.assertEquals(actual.totalprice(), expected.totalprice(), "totalprice");
        soft.assertEquals(actual.depositpaid(), expected.depositpaid(), "depositpaid");
        soft.assertEquals(actual.bookingdates(), expected.bookingdates(), "bookingdates");
        soft.assertEquals(actual.additionalneeds(), expected.additionalneeds(), "additionalneeds");
    }
}
