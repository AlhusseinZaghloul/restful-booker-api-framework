package io.github.alhusseinzaghloul.restfulbooker.tests.health;

import io.github.alhusseinzaghloul.restfulbooker.endpoints.health.CheckHealthRequest;
import io.github.alhusseinzaghloul.restfulbooker.specs.ResponseSpecFactory;
import io.github.alhusseinzaghloul.restfulbooker.utils.TransientRetryAnalyzer;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.qameta.allure.TmsLink;
import org.testng.annotations.Test;

@Epic("Restful Booker")
@Feature("Health")
public class CheckHealthTest {

    @Test(groups = {"smoke", "regression", "health"},
            description = "TC-HLTH-001 GET /ping returns 201 (documented, RB-NOTE-003)",
            retryAnalyzer = TransientRetryAnalyzer.class)
    @TmsLink("TC-HLTH-001")
    @Story("Health check")
    @Severity(SeverityLevel.BLOCKER)
    public void ping_returnsCreated() {
        new CheckHealthRequest().send()
                .then().spec(ResponseSpecFactory.status(201));
    }
}
