package io.github.alhusseinzaghloul.restfulbooker.client;

import io.qameta.allure.Allure;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.http.Cookie;
import io.restassured.http.Header;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.regex.Pattern;

/** Single source of HTTP telemetry (Rule 4.4), with masking (Rule 4.3) and cURL on failures (O14, Rule 4.11). */
public final class DiagnosticFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(DiagnosticFilter.class);
    private static final Set<String> MASKED_HEADERS = Set.of("authorization", "cookie", "set-cookie");
    private static final Pattern SECRET_FIELDS = Pattern.compile(
            "(\"(?:password|token|access_token|refresh_token|secret)\"\\s*:\\s*)\"[^\"]*\"",
            Pattern.CASE_INSENSITIVE);
    private static final String MASK = "***";

    @Override
    public Response filter(FilterableRequestSpecification request,
                           FilterableResponseSpecification responseSpec,
                           FilterContext context) {
        String requestText = describe(request);
        attach("Request", requestText);
        LOG.debug("Request:\n{}", requestText);

        Response response;
        try {
            response = context.next(request, responseSpec);
        } catch (RuntimeException e) {
            LOG.warn("{} {} failed: {}", request.getMethod(), request.getURI(), e.toString());
            throw e;
        }

        String responseText = describe(response);
        attach("Response", responseText);
        LOG.info("{} {} -> {} in {} ms",
                request.getMethod(), request.getURI(), response.getStatusCode(), response.getTime());
        LOG.debug("Response:\n{}", responseText);

        if (response.getStatusCode() >= 400) {
            String curl = curl(request);
            attach("cURL", curl);
            LOG.debug("cURL: {}", curl);
        }
        return response;
    }

    /** Outside a test (suite setup) there is no Allure context; the console log still has the call. */
    private static void attach(String name, String content) {
        if (Allure.getLifecycle().getCurrentExecutableKey().isPresent()) {
            Allure.attachment(name, "text/plain", content);
        }
    }

    private static String describe(FilterableRequestSpecification request) {
        StringBuilder text = new StringBuilder()
                .append(request.getMethod()).append(' ').append(request.getURI()).append('\n');
        request.getHeaders().forEach(h -> appendHeader(text, h));
        for (Cookie cookie : request.getCookies()) {
            text.append("Cookie: ").append(cookie.getName()).append('=').append(MASK).append('\n');
        }
        String body = bodyOf(request);
        if (body != null) {
            text.append('\n').append(body);
        }
        return text.toString();
    }

    private static String describe(Response response) {
        StringBuilder text = new StringBuilder()
                .append(response.getStatusLine()).append('\n')
                .append("Latency: ").append(response.getTime()).append(" ms\n");
        response.getHeaders().forEach(h -> appendHeader(text, h));
        text.append('\n').append(maskSecrets(response.getBody().asPrettyString()));
        return text.toString();
    }

    private static String curl(FilterableRequestSpecification request) {
        StringBuilder curl = new StringBuilder("curl -X ")
                .append(request.getMethod()).append(" '").append(request.getURI()).append('\'');
        for (Header header : request.getHeaders()) {
            boolean masked = MASKED_HEADERS.contains(header.getName().toLowerCase());
            curl.append(" -H '").append(header.getName()).append(": ")
                    .append(masked ? MASK : header.getValue()).append('\'');
        }
        for (Cookie cookie : request.getCookies()) {
            curl.append(" -b '").append(cookie.getName()).append('=').append(MASK).append('\'');
        }
        String body = bodyOf(request);
        if (body != null) {
            curl.append(" --data '").append(body.replace("'", "'\\''")).append('\'');
        }
        return curl.toString();
    }

    private static String bodyOf(FilterableRequestSpecification request) {
        Object body = request.getBody();
        return body == null ? null : maskSecrets(String.valueOf(body));
    }

    private static void appendHeader(StringBuilder text, Header header) {
        boolean masked = MASKED_HEADERS.contains(header.getName().toLowerCase());
        text.append(header.getName()).append(": ").append(masked ? MASK : header.getValue()).append('\n');
    }

    private static String maskSecrets(String body) {
        return SECRET_FIELDS.matcher(body).replaceAll("$1\"" + MASK + "\"");
    }
}
