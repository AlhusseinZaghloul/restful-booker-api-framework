package io.github.alhusseinzaghloul.restfulbooker.tests.booking;

import io.github.alhusseinzaghloul.restfulbooker.endpoints.booking.CreateBookingRequest;
import io.github.alhusseinzaghloul.restfulbooker.endpoints.booking.GetBookingRequest;
import io.github.alhusseinzaghloul.restfulbooker.endpoints.booking.PartialUpdateBookingRequest;
import io.github.alhusseinzaghloul.restfulbooker.payloads.factories.BookingDataFactory;
import io.github.alhusseinzaghloul.restfulbooker.payloads.pojos.BookingPayload;
import io.github.alhusseinzaghloul.restfulbooker.payloads.pojos.BookingResponse;
import io.github.alhusseinzaghloul.restfulbooker.payloads.pojos.CreatedBookingResponse;
import io.github.alhusseinzaghloul.restfulbooker.payloads.pojos.PartialBookingPayload;
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
public class PartialUpdateBookingTest {

    @Test(groups = {"regression", "booking"},
            description = "TC-BKG-013 PATCH /booking/{id} updates only firstname and totalprice",
            retryAnalyzer = TransientRetryAnalyzer.class)
    @TmsLink("TC-BKG-013")
    @Story("Partial update booking")
    @Severity(SeverityLevel.NORMAL)
    public void partialUpdateBooking_firstnameAndPrice_updatesOnlyThose() {
        BookingPayload original = BookingDataFactory.valid();
        int id = createBooking(original);
        PartialBookingPayload patch = BookingDataFactory.partialUpdate();

        Response response = new PartialUpdateBookingRequest(id, patch).send();

        response.then().spec(ResponseSpecFactory.json(200));
        SoftAssert soft = new SoftAssert();
        assertPatched(soft, "response", response.as(BookingResponse.class), original, patch);

        Response read = new GetBookingRequest(id).send();
        read.then().spec(ResponseSpecFactory.json(200));
        assertPatched(soft, "GET", read.as(BookingResponse.class), original, patch);
        soft.assertAll();
    }

    @Test(groups = {"regression", "contract", "booking"},
            description = "TC-SCH-007 PATCH /booking/{id} response matches booking.schema.json",
            retryAnalyzer = TransientRetryAnalyzer.class)
    @TmsLink("TC-SCH-007")
    @Story("Partial update booking")
    @Severity(SeverityLevel.NORMAL)
    public void partialUpdateBooking_response_matchesBookingSchema() {
        int id = createBooking(BookingDataFactory.valid());

        new PartialUpdateBookingRequest(id, BookingDataFactory.partialUpdate()).send()
                .then()
                .spec(ResponseSpecFactory.json(200))
                .body(matchesJsonSchemaInClasspath("schemas/booking.schema.json"));
    }

    private static int createBooking(BookingPayload booking) {
        return new CreateBookingRequest(booking).sendAsSetup(200)
                .as(CreatedBookingResponse.class).bookingId();
    }

    private static void assertPatched(SoftAssert soft, String source, BookingResponse actual,
                                      BookingPayload original, PartialBookingPayload patch) {
        soft.assertEquals(actual.firstname(), patch.firstname(), source + " firstname (patched)");
        soft.assertEquals(actual.totalprice(), patch.totalprice(), source + " totalprice (patched)");
        soft.assertEquals(actual.lastname(), original.lastname(), source + " lastname (unchanged)");
        soft.assertEquals(actual.depositpaid(), original.depositpaid(), source + " depositpaid (unchanged)");
        soft.assertEquals(actual.bookingdates(), original.bookingdates(), source + " bookingdates (unchanged)");
        soft.assertEquals(actual.additionalneeds(), original.additionalneeds(), source + " additionalneeds (unchanged)");
    }
}
