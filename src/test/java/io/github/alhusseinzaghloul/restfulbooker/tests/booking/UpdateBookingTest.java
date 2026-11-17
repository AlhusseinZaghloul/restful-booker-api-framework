package io.github.alhusseinzaghloul.restfulbooker.tests.booking;

import io.github.alhusseinzaghloul.restfulbooker.endpoints.booking.CreateBookingRequest;
import io.github.alhusseinzaghloul.restfulbooker.endpoints.booking.GetBookingRequest;
import io.github.alhusseinzaghloul.restfulbooker.endpoints.booking.UpdateBookingRequest;
import io.github.alhusseinzaghloul.restfulbooker.payloads.factories.BookingDataFactory;
import io.github.alhusseinzaghloul.restfulbooker.payloads.factories.CredentialsDataFactory;
import io.github.alhusseinzaghloul.restfulbooker.payloads.pojos.BookingPayload;
import io.github.alhusseinzaghloul.restfulbooker.payloads.pojos.BookingResponse;
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
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;

@Epic("Restful Booker")
@Feature("Booking")
public class UpdateBookingTest {

    @Test(groups = {"regression", "booking"},
            description = "TC-BKG-012 PUT /booking/{id} replaces the booking and the change persists",
            retryAnalyzer = TransientRetryAnalyzer.class)
    @TmsLink("TC-BKG-012")
    @Story("Update booking")
    @Severity(SeverityLevel.CRITICAL)
    public void updateBooking_fullPayload_replacesBooking() {
        BookingPayload original = BookingDataFactory.valid();
        int id = createBooking(original);
        BookingPayload updated = BookingDataFactory.updated(original);

        Response response = new UpdateBookingRequest(id, updated).send();

        response.then().spec(ResponseSpecFactory.json(200));
        SoftAssert soft = new SoftAssert();
        assertBookingEquals(soft, "response", response.as(BookingResponse.class), updated);
        assertBookingEquals(soft, "GET", readBooking(id), updated);
        soft.assertAll();
    }

    @Test(groups = {"regression", "booking", "auth", "negative"},
            description = "TC-NEG-009 PUT /booking/{id} with an invalid token: actual behavior is 403 (RB-DEF-004)",
            retryAnalyzer = TransientRetryAnalyzer.class)
    @TmsLink("TC-NEG-009")
    @Issue("RB-DEF-004")
    @Story("Update booking")
    @Severity(SeverityLevel.CRITICAL)
    public void updateBooking_invalidToken_actualBehavior_returns403() {
        BookingPayload original = BookingDataFactory.valid();
        int id = createBooking(original);

        new UpdateBookingRequest(id, BookingDataFactory.updated(original))
                .withToken(CredentialsDataFactory.INVALID_TOKEN)
                .send()
                .then().spec(ResponseSpecFactory.status(403));

        SoftAssert soft = new SoftAssert();
        assertBookingEquals(soft, "GET (unchanged)", readBooking(id), original);
        soft.assertAll();
    }

    @Test(groups = {"known-defect", "booking", "auth", "negative"},
            description = "TC-NEG-010 PUT /booking/{id} with an invalid token should return 401 (RB-DEF-004)",
            retryAnalyzer = TransientRetryAnalyzer.class)
    @TmsLink("TC-NEG-010")
    @Issue("RB-DEF-004")
    @Story("Update booking")
    @Severity(SeverityLevel.CRITICAL)
    public void updateBooking_invalidToken_contract_returns401() {
        BookingPayload original = BookingDataFactory.valid();
        int id = createBooking(original);

        int status = new UpdateBookingRequest(id, BookingDataFactory.updated(original))
                .withToken(CredentialsDataFactory.INVALID_TOKEN)
                .send()
                .statusCode();

        Assert.assertEquals(status, 401,
                "[KNOWN-DEFECT] RB-DEF-004: PUT /booking/{id} with an invalid token should return 401");
    }

    @Test(groups = {"regression", "contract", "booking"},
            description = "TC-SCH-006 PUT /booking/{id} response matches booking.schema.json",
            retryAnalyzer = TransientRetryAnalyzer.class)
    @TmsLink("TC-SCH-006")
    @Story("Update booking")
    @Severity(SeverityLevel.NORMAL)
    public void updateBooking_response_matchesBookingSchema() {
        BookingPayload original = BookingDataFactory.valid();
        int id = createBooking(original);

        new UpdateBookingRequest(id, BookingDataFactory.updated(original)).send()
                .then()
                .spec(ResponseSpecFactory.json(200))
                .body(matchesJsonSchemaInClasspath("schemas/booking.schema.json"));
    }

    private static int createBooking(BookingPayload booking) {
        return new CreateBookingRequest(booking).sendAsSetup(200)
                .as(CreatedBookingResponse.class).bookingId();
    }

    private static BookingResponse readBooking(int id) {
        Response response = new GetBookingRequest(id).send();
        response.then().spec(ResponseSpecFactory.json(200));
        return response.as(BookingResponse.class);
    }

    private static void assertBookingEquals(SoftAssert soft, String source, BookingResponse actual, BookingPayload expected) {
        soft.assertEquals(actual.firstname(), expected.firstname(), source + " firstname");
        soft.assertEquals(actual.lastname(), expected.lastname(), source + " lastname");
        soft.assertEquals(actual.totalprice(), expected.totalprice(), source + " totalprice");
        soft.assertEquals(actual.depositpaid(), expected.depositpaid(), source + " depositpaid");
        soft.assertEquals(actual.bookingdates(), expected.bookingdates(), source + " bookingdates");
        soft.assertEquals(actual.additionalneeds(), expected.additionalneeds(), source + " additionalneeds");
    }
}
