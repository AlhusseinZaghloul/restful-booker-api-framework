package io.github.alhusseinzaghloul.restfulbooker.tests.booking;

import io.github.alhusseinzaghloul.restfulbooker.endpoints.booking.CreateBookingRequest;
import io.github.alhusseinzaghloul.restfulbooker.endpoints.booking.DeleteBookingRequest;
import io.github.alhusseinzaghloul.restfulbooker.endpoints.booking.GetBookingRequest;
import io.github.alhusseinzaghloul.restfulbooker.payloads.factories.BookingDataFactory;
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
import org.testng.Assert;
import org.testng.annotations.Test;

@Epic("Restful Booker")
@Feature("Booking")
public class DeleteBookingTest {

    @Test(groups = {"regression", "booking"},
            description = "TC-BKG-014 DELETE /booking/{id} returns 201 and the booking is gone (RB-NOTE-002)",
            retryAnalyzer = TransientRetryAnalyzer.class)
    @TmsLink("TC-BKG-014")
    @Story("Delete booking")
    @Severity(SeverityLevel.CRITICAL)
    public void deleteBooking_existingId_removesBooking() {
        int id = new CreateBookingRequest(BookingDataFactory.valid()).sendAsSetup(200)
                .as(CreatedBookingResponse.class).bookingId();

        new DeleteBookingRequest(id).send()
                .then().spec(ResponseSpecFactory.status(201));

        // Rule 5.7: verify the delete with a read
        new GetBookingRequest(id).send()
                .then().spec(ResponseSpecFactory.status(404));
    }

    @Test(groups = {"regression", "booking", "negative"},
            description = "TC-NEG-007 DELETE /booking/{id} for a non-existent id: actual behavior is 405 (RB-DEF-005)",
            retryAnalyzer = TransientRetryAnalyzer.class)
    @TmsLink("TC-NEG-007")
    @Issue("RB-DEF-005")
    @Story("Delete booking")
    @Severity(SeverityLevel.NORMAL)
    public void deleteBooking_nonExistentId_actualBehavior_returns405() {
        new DeleteBookingRequest(BookingDataFactory.NON_EXISTENT_ID).send()
                .then().spec(ResponseSpecFactory.status(405));
    }

    @Test(groups = {"known-defect", "booking", "negative"},
            description = "TC-NEG-008 DELETE /booking/{id} for a non-existent id should return 404 (RB-DEF-005)",
            retryAnalyzer = TransientRetryAnalyzer.class)
    @TmsLink("TC-NEG-008")
    @Issue("RB-DEF-005")
    @Story("Delete booking")
    @Severity(SeverityLevel.NORMAL)
    public void deleteBooking_nonExistentId_contract_returns404() {
        int status = new DeleteBookingRequest(BookingDataFactory.NON_EXISTENT_ID).send().statusCode();

        Assert.assertEquals(status, 404,
                "[KNOWN-DEFECT] RB-DEF-005: DELETE /booking/{id} for a non-existent id should return 404");
    }
}
