package io.github.alhusseinzaghloul.restfulbooker.tests.booking;

import io.github.alhusseinzaghloul.restfulbooker.endpoints.auth.CreateTokenRequest;
import io.github.alhusseinzaghloul.restfulbooker.endpoints.booking.CreateBookingRequest;
import io.github.alhusseinzaghloul.restfulbooker.endpoints.booking.DeleteBookingRequest;
import io.github.alhusseinzaghloul.restfulbooker.endpoints.booking.GetBookingIdsRequest;
import io.github.alhusseinzaghloul.restfulbooker.endpoints.booking.GetBookingRequest;
import io.github.alhusseinzaghloul.restfulbooker.endpoints.booking.PartialUpdateBookingRequest;
import io.github.alhusseinzaghloul.restfulbooker.endpoints.booking.UpdateBookingRequest;
import io.github.alhusseinzaghloul.restfulbooker.payloads.factories.BookingDataFactory;
import io.github.alhusseinzaghloul.restfulbooker.payloads.factories.CredentialsDataFactory;
import io.github.alhusseinzaghloul.restfulbooker.payloads.pojos.BookingPayload;
import io.github.alhusseinzaghloul.restfulbooker.payloads.pojos.BookingResponse;
import io.github.alhusseinzaghloul.restfulbooker.payloads.pojos.CreatedBookingResponse;
import io.github.alhusseinzaghloul.restfulbooker.payloads.pojos.PartialBookingPayload;
import io.github.alhusseinzaghloul.restfulbooker.payloads.pojos.TokenResponse;
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

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;

/** End-to-end flows that pass state across calls (Rule 2.17). */
@Epic("Restful Booker")
@Feature("Booking")
public class ManageBookingTest {

    @Test(groups = {"smoke", "regression", "booking", "e2e"},
            description = "TC-E2E-001 Create -> Get -> Put -> Patch -> Delete -> Get 404",
            retryAnalyzer = TransientRetryAnalyzer.class)
    @TmsLink("TC-E2E-001")
    @Story("Booking lifecycle")
    @Severity(SeverityLevel.CRITICAL)
    public void bookingLifecycle_createReadUpdatePatchDelete() {
        SoftAssert soft = new SoftAssert();

        // Create
        BookingPayload original = BookingDataFactory.valid();
        Response created = new CreateBookingRequest(original).send();
        created.then().spec(ResponseSpecFactory.json(200));
        int id = created.as(CreatedBookingResponse.class).bookingId();

        // Read
        assertBookingEquals(soft, "GET after create", readBooking(id), original);

        // Full update
        BookingPayload updated = BookingDataFactory.updated(original);
        Response put = new UpdateBookingRequest(id, updated).send();
        put.then().spec(ResponseSpecFactory.json(200));
        assertBookingEquals(soft, "PUT response", put.as(BookingResponse.class), updated);
        assertBookingEquals(soft, "GET after PUT", readBooking(id), updated);

        // Partial update: only firstname and totalprice change
        PartialBookingPayload patch = BookingDataFactory.partialUpdate();
        Response patched = new PartialUpdateBookingRequest(id, patch).send();
        patched.then().spec(ResponseSpecFactory.json(200));
        BookingPayload expectedAfterPatch = updated.toBuilder()
                .firstname(patch.firstname())
                .totalprice(patch.totalprice())
                .build();
        assertBookingEquals(soft, "PATCH response", patched.as(BookingResponse.class), expectedAfterPatch);

        // Delete, then verify it is gone (Rule 5.7)
        new DeleteBookingRequest(id).send().then().spec(ResponseSpecFactory.status(201));
        new GetBookingRequest(id).send().then().spec(ResponseSpecFactory.status(404));

        soft.assertAll();
    }

    @Test(groups = {"regression", "booking", "e2e"},
            description = "TC-E2E-002 A created booking appears in search until it is deleted",
            retryAnalyzer = TransientRetryAnalyzer.class)
    @TmsLink("TC-E2E-002")
    @Story("Booking search")
    @Severity(SeverityLevel.NORMAL)
    public void createdBooking_appearsInSearch_untilDeleted() {
        BookingPayload booking = BookingDataFactory.valid();
        Response created = new CreateBookingRequest(booking).send();
        created.then().spec(ResponseSpecFactory.json(200));
        int id = created.as(CreatedBookingResponse.class).bookingId();

        searchByName(booking)
                .then().spec(ResponseSpecFactory.json(200))
                .body("bookingid", contains(id));

        new DeleteBookingRequest(id).send().then().spec(ResponseSpecFactory.status(201));

        searchByName(booking)
                .then().spec(ResponseSpecFactory.json(200))
                .body("bookingid", empty());
    }

    @Test(groups = {"regression", "auth", "booking", "e2e"},
            description = "TC-E2E-003 A token issued by POST /auth authorizes PUT /booking/{id}",
            retryAnalyzer = TransientRetryAnalyzer.class)
    @TmsLink("TC-E2E-003")
    @Story("Auth -> Booking")
    @Severity(SeverityLevel.CRITICAL)
    public void issuedToken_authorizesUpdate() {
        // POST /auth is part of the flow under test (O23); other tests get the token from TokenManager
        Response auth = new CreateTokenRequest(CredentialsDataFactory.fromConfig()).send();
        auth.then().spec(ResponseSpecFactory.json(200));
        String token = auth.as(TokenResponse.class).token();

        BookingPayload original = BookingDataFactory.valid();
        int id = new CreateBookingRequest(original).sendAsSetup(200)
                .as(CreatedBookingResponse.class).bookingId();
        BookingPayload updated = BookingDataFactory.updated(original);

        new UpdateBookingRequest(id, updated).withToken(token).send()
                .then().spec(ResponseSpecFactory.json(200));

        SoftAssert soft = new SoftAssert();
        assertBookingEquals(soft, "GET after PUT", readBooking(id), updated);
        soft.assertAll();
    }

    private static Response searchByName(BookingPayload booking) {
        return new GetBookingIdsRequest()
                .firstname(booking.firstname())
                .lastname(booking.lastname())
                .send();
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
