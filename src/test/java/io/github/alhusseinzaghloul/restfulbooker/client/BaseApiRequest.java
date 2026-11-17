package io.github.alhusseinzaghloul.restfulbooker.client;

import io.github.alhusseinzaghloul.restfulbooker.utils.SetupFailedException;
import io.restassured.http.Method;
import io.restassured.response.Response;

import java.util.LinkedHashMap;
import java.util.Map;

/** Abstract parent of every request class (Standards §2.1, blueprint §6.1). */
public abstract class BaseApiRequest {

    private final Method method;
    private final String path;
    private final Map<String, Object> pathParams = new LinkedHashMap<>();
    private final Map<String, Object> queryParams = new LinkedHashMap<>();
    private final Map<String, String> headers = new LinkedHashMap<>();
    private Object body;
    private String tokenOverride; // null = use the default auth for this request

    protected BaseApiRequest(Method method, String path) {
        this.method = method;
        this.path = path;
    }

    // Building blocks for subclasses only
    protected final void pathParam(String name, Object value)  { pathParams.put(name, value); }
    protected final void queryParam(String name, Object value) { queryParams.put(name, value); }
    protected final void header(String name, String value)     { headers.put(name, value); }
    protected final void body(Object payload)                  { this.body = payload; }

    /** O5: lets a request send a specific (for example invalid) token instead of the TokenManager token. */
    protected final void tokenOverride(String token)           { this.tokenOverride = token; }

    /** Override and return false for requests sent without credentials. */
    protected boolean requiresAuth() { return true; }

    /**
     * Override in every request that creates a resource: return the request that deletes it.
     * RestClient registers it for cleanup as soon as the create call succeeds.
     */
    protected BaseApiRequest cleanupFor(Response response) { return null; }

    /** O12: a cleanup DELETE that returns this status means the resource is already gone. */
    public boolean isAlreadyGone(int statusCode) { return statusCode == 404; }

    /** Use in the act and assert parts of a test. */
    public final Response send() { return RestClient.send(this); }

    /** Use for arrange steps. A wrong status is a setup failure, not a product assertion failure. */
    public final Response sendAsSetup(int expectedStatus) {
        Response response = send();
        if (response.statusCode() != expectedStatus) {
            throw new SetupFailedException(this + " returned HTTP " + response.statusCode()
                    + ", expected " + expectedStatus);
        }
        return response;
    }

    @Override
    public String toString() {
        return method + " " + path + (pathParams.isEmpty() ? "" : " " + pathParams);
    }

    // Read access for RestClient (same package)
    Method method()                   { return method; }
    String path()                     { return path; }
    Map<String, Object> pathParams()  { return pathParams; }
    Map<String, Object> queryParams() { return queryParams; }
    Map<String, String> headers()     { return headers; }
    Object body()                     { return body; }
    String tokenOverride()            { return tokenOverride; }
}
