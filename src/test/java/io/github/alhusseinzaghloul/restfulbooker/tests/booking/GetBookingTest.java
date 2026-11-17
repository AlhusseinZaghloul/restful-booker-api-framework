package io.github.alhusseinzaghloul.restfulbooker.tests.booking;

import io.github.alhusseinzaghloul.restfulbooker.endpoints.booking.CreateBookingRequest;
import io.github.alhusseinzaghloul.restfulbooker.endpoints.booking.GetBookingRequest;
import io.github.alhusseinzaghloul.restfulbooker.payloads.factories.BookingDataFactory;
import io.github.alhusseinzaghloul.restfulbooker.payloads.pojos.BookingPayload;
import io.github.alhusseinzaghloul.restfulbooker.payloads.pojos.BookingResponse;
import io.github.alhusseinzaghloul.restfulbooker.payloads.pojos.CreatedBookingResponse;
import io.github.alhusseinzaghloul.restfulbooker.specs.ResponseSpecFactory;
import io.github.alhusseinzaghloul.restfulbooker.utils.TransientRetryAnalyzer;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.qameta.allure.TmsLink;
import io.restassured.response.Response;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;

@Epic("Restful Booker")
@Feature("Booking")
public class GetBookingTest {

    @Test(groups = {"smoke", "regression", "booking"},
            description = "TC-BKG-002 GET /booking/{id} returns the stored booking",
            retryAnalyzer = TransientRetryAnalyzer.class)
    @TmsLink("TC-BKG-002")
    @Story("Get booking")
    @Severity(SeverityLevel.CRITICAL)
    public void getBooking_existingId_returnsBooking() {
        BookingPayload booking = BookingDataFactory.valid();
        int id = createBooking(booking);

        Response response = new GetBookingRequest(id).send();

        response.then().spec(ResponseSpecFactory.json(200));
        BookingResponse actual = response.as(BookingResponse.class);

        SoftAssert soft = new SoftAssert();
        soft.assertEquals(actual.firstname(), booking.firstname(), "firstname");
        soft.assertEquals(actual.lastname(), booking.lastname(), "lastname");
        soft.assertEquals(actual.totalprice(), booking.totalprice(), "totalprice");
        soft.assertEquals(actual.depositpaid(), booking.depositpaid(), "depositpaid");
        soft.assertEquals(actual.bookingdates(), booking.bookingdates(), "bookingdates");
        soft.assertEquals(actual.additionalneeds(), booking.additionalneeds(), "additionalneeds");
        soft.assertAll();
    }

    @Test(groups = {"regression", "contract", "booking"},
            description = "TC-SCH-004 GET /booking/{id} response matches booking.schema.json",
            retryAnalyzer = TransientRetryAnalyzer.class)
    @TmsLink("TC-SCH-004")
    @Story("Get booking")
    @Severity(SeverityLevel.NORMAL)
    public void getBooking_response_matchesBookingSchema() {
        int id = createBooking(BookingDataFactory.valid());

        new GetBookingRequest(id).send()
                .then()
                .spec(ResponseSpecFactory.json(200))
                .body(matchesJsonSchemaInClasspath("schemas/booking.schema.json"));
    }

    private static int createBooking(BookingPayload booking) {
        return new CreateBookingRequest(booking).sendAsSetup(200)
                .as(CreatedBookingResponse.class).bookingId();
    }
}
