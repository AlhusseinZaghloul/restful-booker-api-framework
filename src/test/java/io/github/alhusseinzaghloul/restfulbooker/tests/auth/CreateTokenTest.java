package io.github.alhusseinzaghloul.restfulbooker.tests.auth;

import io.github.alhusseinzaghloul.restfulbooker.endpoints.auth.CreateTokenRequest;
import io.github.alhusseinzaghloul.restfulbooker.payloads.factories.CredentialsDataFactory;
import io.github.alhusseinzaghloul.restfulbooker.payloads.pojos.TokenResponse;
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
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;

/** POST /auth is the system under test here, so these tests call CreateTokenRequest directly (O23). */
@Epic("Restful Booker")
@Feature("Auth")
public class CreateTokenTest {

    @Test(groups = {"smoke", "regression", "auth"},
            description = "TC-AUTH-001 POST /auth with valid credentials returns a token",
            retryAnalyzer = TransientRetryAnalyzer.class)
    @TmsLink("TC-AUTH-001")
    @Story("Create token")
    @Severity(SeverityLevel.BLOCKER)
    public void createToken_validCredentials_returnsToken() {
        Response response = new CreateTokenRequest(CredentialsDataFactory.fromConfig()).send();

        response.then().spec(ResponseSpecFactory.json(200));
        TokenResponse token = response.as(TokenResponse.class);

        SoftAssert soft = new SoftAssert();
        soft.assertNotNull(token.token(), "token");
        soft.assertFalse(token.token() == null || token.token().isBlank(), "token is not blank");
        soft.assertNull(token.reason(), "reason");
        soft.assertAll();
    }

    @Test(groups = {"regression", "auth", "negative"},
            description = "TC-NEG-001 POST /auth with bad credentials: actual behavior is 200 + reason (RB-DEF-001)",
            retryAnalyzer = TransientRetryAnalyzer.class)
    @TmsLink("TC-NEG-001")
    @Issue("RB-DEF-001")
    @Story("Create token")
    @Severity(SeverityLevel.NORMAL)
    public void createToken_badCredentials_actualBehavior_returns200WithReason() {
        Response response = new CreateTokenRequest(CredentialsDataFactory.invalid()).send();

        response.then().spec(ResponseSpecFactory.json(200));
        TokenResponse body = response.as(TokenResponse.class);

        SoftAssert soft = new SoftAssert();
        soft.assertFalse(body.reason() == null || body.reason().isBlank(), "reason is present");
        soft.assertNull(body.token(), "token");
        soft.assertAll();
    }

    @Test(groups = {"regression", "contract", "auth"},
            description = "TC-SCH-001 POST /auth response matches token.schema.json",
            retryAnalyzer = TransientRetryAnalyzer.class)
    @TmsLink("TC-SCH-001")
    @Story("Create token")
    @Severity(SeverityLevel.NORMAL)
    public void createToken_response_matchesTokenSchema() {
        new CreateTokenRequest(CredentialsDataFactory.fromConfig()).send()
                .then()
                .spec(ResponseSpecFactory.json(200))
                .body(matchesJsonSchemaInClasspath("schemas/token.schema.json"));
    }

    @Test(groups = {"regression", "contract", "auth"},
            description = "TC-SCH-002 POST /auth bad-credentials response matches auth-failure.schema.json",
            retryAnalyzer = TransientRetryAnalyzer.class)
    @TmsLink("TC-SCH-002")
    @Issue("RB-DEF-001")
    @Story("Create token")
    @Severity(SeverityLevel.MINOR)
    public void createToken_badCredentialsResponse_matchesAuthFailureSchema() {
        new CreateTokenRequest(CredentialsDataFactory.invalid()).send()
                .then()
                .spec(ResponseSpecFactory.json(200))
                .body(matchesJsonSchemaInClasspath("schemas/auth-failure.schema.json"));
    }
}
