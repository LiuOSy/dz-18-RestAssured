
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.hamcrest.Matchers;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;


public class RestAssuredApiTest {

    String baseURI = "https://restful-booker.herokuapp.com";
    String sessionToken;
    String bookingId;

    @DataProvider(name = "originalData")
    public Object[][] createOriginalData() {
        return new Object [][] {
                {"John", "Smith", 123, true, "2024-01-01", "2025-01-10", "Test Value"}
        };
    }

    @DataProvider(name = "patchUpdateData")
    public Object[][] createPatchUpdateData() {
        return new Object [][] {
                {321}
        };
    }

    @DataProvider(name = "putUpdateData")
    public Object[][] createPutUpdateData() {
        return new Object [][] {
                {"Mary", "Adams", 321, true, "2024-01-01", "2025-01-10", "Breakfast"}
        };
    }

    //Authorisation To The https://restful-booker.herokuapp.com/ API

    @BeforeTest
    public void beforeSuite() {
        String authLogin = "admin";
        String authPassword = "password123";
        Response res = RestAssured.given()
                .body(String.format("{\"username\":\"%s\",\"password\":\"%s\"}", authLogin, authPassword))
                .contentType(ContentType.JSON)
                .when()
                .post(String.format("%s/auth", baseURI))
                .then()
                .assertThat()
                .statusCode(200)
                .extract().response();
        this.sessionToken = res.jsonPath().getString("token");
        System.out.println(String.format("Token is %s", sessionToken));
    }

    //GET Request - Getting All The Bookings Present In The System

    @Test(priority = 0)
    public void getAllBookingsTest() {

        Response res = RestAssured.given()
                .log().all()
                .when()
                .get(String.format("%s/booking", baseURI))
                .then()
                .assertThat()
                .statusCode(200)
                .extract().response();

        List<Integer> bookingIdList = res.jsonPath().getList("bookingid");

        Assert.assertFalse(bookingIdList.isEmpty());

        System.out.println(bookingIdList.size());
    }

    //POST Request - Creating New Booking

    @Test(priority = 1, dataProvider = "originalData")
    public void createNewBookingTest(String firstName, String lastName, int totalPrice, boolean depositPaid,
                                     String checkInDate, String checkOutDate, String additionalNeeds) {


        String inputJson = String.join(System.lineSeparator(),
                String.format("{\"firstname\" : \"%s\",", firstName),
                          String.format("\"lastname\" : \"%s\",", lastName),
                          String.format("\"totalprice\" : %d,", totalPrice),
                          String.format("\"depositpaid\" : %b,", depositPaid),
                          String.format("\"bookingdates\" : { \"checkin\" : \"%s\", \"checkout\" : \"%s\" },", checkInDate, checkOutDate),
                          String.format("\"additionalneeds\" : \"%s\"}", additionalNeeds));


        Response res = RestAssured.given()
                .log().all()
                .body(inputJson)
                .contentType(ContentType.JSON)
                .when()
                .post(String.format("%s/booking", baseURI))
                .then()
                .assertThat()
                .statusCode(200)
                .extract().response();

        String responseBody = res.getBody().asString();
        this.bookingId = res.jsonPath().getString("bookingid");

        JSONObject jsonObject = new JSONObject(responseBody);
        JSONObject getBookingDetails = jsonObject.getJSONObject("booking");

        Assert.assertNotNull(bookingId);
        Assert.assertEquals(getBookingDetails.get("firstname"), firstName);
        Assert.assertEquals(getBookingDetails.get("lastname"), lastName);
        Assert.assertEquals(getBookingDetails.get("totalprice"), totalPrice);
        Assert.assertEquals(getBookingDetails.get("depositpaid"), depositPaid);
        Assert.assertEquals(getBookingDetails.get("additionalneeds"), additionalNeeds);

        System.out.println(String.format("Response Body is %s. %n BookingID is %s", responseBody, bookingId));

    }

    //PATCH Request - Updating Booking Created With Post Request: TotalPrice Update

    @Test(priority = 2, dataProvider = "patchUpdateData")
    public void patchUpdateBookingTest(int updatedPrice) {

        Response res = RestAssured.given()
                .log().all()
                .body(String.format("{\"totalprice\" : %d}",updatedPrice))
                .cookie("token", sessionToken)
                .contentType(ContentType.JSON)
                .when()
                .patch(String.format("%s/booking/%s", baseURI, bookingId))
                .then()
                .assertThat()
                .statusCode(200)
                .body("totalprice", Matchers.equalTo(updatedPrice))
                .extract().response();

        String responseBody = res.getBody().asString();
        System.out.println(String.format("Response Body is %s.", responseBody));

    }

    //PUT Request - Updating Booking Created With Post Request: First and Last Name Update

    @Test(priority = 3, dataProvider = "putUpdateData")
    public void putUpdateBookingTest(String firstName, String lastName, int totalPrice, boolean depositpaid,
                                     String checkInDate, String checkOutDate, String additionalNeeds) {

        String inputJson = String.join(System.lineSeparator(),
                String.format("{\"firstname\" : \"%s\",", firstName),
                String.format("\"lastname\" : \"%s\",", lastName),
                String.format("\"totalprice\" : %d,", totalPrice),
                String.format("\"depositpaid\" : %b,", depositpaid),
                String.format("\"bookingdates\" : { \"checkin\" : \"%s\", \"checkout\" : \"%s\" },", checkInDate, checkOutDate),
                String.format("\"additionalneeds\" : \"%s\"}", additionalNeeds));

        Response res = RestAssured.given()
                .log().all()
                .body(inputJson)
                .cookie("token", sessionToken)
                .contentType(ContentType.JSON)
                .when()
                .put(String.format("%s/booking/%s", baseURI, bookingId))
                .then()
                .assertThat()
                .statusCode(200)
                .body("firstname", Matchers.equalTo(firstName))
                .body("lastname", Matchers.equalTo(lastName))
                .extract().response();

        String responseBody = res.getBody().asString();
        System.out.println(String.format("Response Body is %s.", responseBody));

    }

    //DELETE Request - Deleting Booking Created With Post Request

    @Test(priority = 4)
    public void deleteBookingTest() {

        //Check the required bookingId is present

        RestAssured.given()
                .log().all()
                .contentType(ContentType.JSON)
                .when()
                .get(String.format("%s/booking/%s", baseURI, bookingId))
                .then()
                .assertThat()
                .statusCode(200);


        //Delete the required bookingId

        RestAssured.given()
                .log().all()
                .cookie("token", sessionToken)
                .contentType(ContentType.JSON)
                .when()
                .delete(String.format("%s/booking/%s", baseURI, bookingId))
                .then()
                .assertThat()
                .statusCode(201);


        //Verify the required bookingId is no longer present

        RestAssured.given()
                .log().all()
                .contentType(ContentType.JSON)
                .when()
                .get(String.format("%s/booking/%s", baseURI, bookingId))
                .then()
                .assertThat()
                .statusCode(404);

    }

}
