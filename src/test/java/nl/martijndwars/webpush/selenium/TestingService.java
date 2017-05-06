package nl.martijndwars.webpush.selenium;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Java wrapper for interacting with the Web Push Testing Service.
 */
public class TestingService {
    private String baseUrl;

    public TestingService(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
     * Start a new test suite.
     *
     * @return
     */
    public int startTestSuite() throws IOException {
        String startTestSuite = request(baseUrl + "start-test-suite/");

        JsonElement root = new JsonParser().parse(startTestSuite);

        return root
                .getAsJsonObject()
                .get("data")
                .getAsJsonObject()
                .get("testSuiteId")
                .getAsInt();
    }

    /**
     * Get a test ID and subscription for the given test case.
     *
     * @param testSuiteId
     * @param configuration
     * @return
     * @throws IOException
     */
    public JsonObject getSubscription(int testSuiteId, Configuration configuration) throws IOException {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("testSuiteId", testSuiteId);
        jsonObject.addProperty("browserName", configuration.browser);
        jsonObject.addProperty("browserVersion", configuration.version);
        jsonObject.addProperty("vapidPublicKey", configuration.publicKey);

        HttpEntity entity = new StringEntity(jsonObject.toString(), ContentType.APPLICATION_JSON);

        String getSubscription = request(baseUrl + "get-subscription/", entity);

        return getData(getSubscription);
    }

    /**
     * Get the notification status for the given test case.
     *
     * @param testSuiteId
     * @param testId
     * @return
     * @throws IOException
     */
    public JsonArray getNotificationStatus(int testSuiteId, int testId) throws IOException {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("testSuiteId", testSuiteId);
        jsonObject.addProperty("testId", testId);

        HttpEntity entity = new StringEntity(jsonObject.toString(), ContentType.APPLICATION_JSON);

        String notificationStatus = request(baseUrl + "get-notification-status/", entity);

        return getData(notificationStatus).get("messages").getAsJsonArray();
    }

    /**
     * End the given test suite.
     *
     * @return
     */
    public boolean endTestSuite(int testSuiteId) throws IOException {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("testSuiteId", testSuiteId);

        HttpEntity entity = new StringEntity(jsonObject.toString(), ContentType.APPLICATION_JSON);

        String endTestSuite = request(baseUrl + "end-test-suite/", entity);

        return getData(endTestSuite).get("success").getAsBoolean();
    }

    /**
     * Perform HTTP request and return response.
     *
     * @param uri
     * @return
     */
    protected String request(String uri) throws IOException {
        return request(uri, null);
    }

    /**
     * Perform HTTP request and return response.
     *
     * @param uri
     * @return
     */
    protected String request(String uri, HttpEntity entity) throws IOException {
        return Request.Post(uri).body(entity).execute().handleResponse(new ResponseHandler<String>() {
            @Override
            public String handleResponse(HttpResponse httpResponse) throws IOException {
                String json = EntityUtils.toString(httpResponse.getEntity());

                if (httpResponse.getStatusLine().getStatusCode() != 200) {
                    JsonElement root = new JsonParser().parse(json);
                    JsonObject error = root.getAsJsonObject().get("error").getAsJsonObject();

                    String errorId = error.get("id").getAsString();
                    String errorMessage = error.get("message").getAsString();

                    throw new IllegalStateException("Error " + errorId + ": " + errorMessage);
                }

                return json;
            }
        });

        /*
        Request request = Request.Post(uri).body(entity);

        Response response = request.execute();

        HttpResponse httpResponse = response.returnResponse();

        String content = EntityUtils.toString(httpResponse.getEntity());

        if (httpResponse.getStatusLine().getStatusCode() != 200) {
            throw new IllegalStateException(content);
        }

        return content;
        */
    }

    /**
     * Get the a JSON object of the data in the JSON response.
     *
     * @param response
     */
    protected JsonObject getData(String response) {
        JsonElement root = new JsonParser().parse(response);

        return root
                .getAsJsonObject()
                .get("data")
                .getAsJsonObject();
    }
}
