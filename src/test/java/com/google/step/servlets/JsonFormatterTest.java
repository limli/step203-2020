package com.google.step.servlets;

import static com.google.step.TestConstants.BIO_A;
import static com.google.step.TestConstants.BLOBKEY_URL_A;
import static com.google.step.TestConstants.DATE_A;
import static com.google.step.TestConstants.DATE_B;
import static com.google.step.TestConstants.DEAL_A;
import static com.google.step.TestConstants.DEAL_A_BRIEF_JSON;
import static com.google.step.TestConstants.DEAL_ID_A;
import static com.google.step.TestConstants.DESCRIPTION_A;
import static com.google.step.TestConstants.EMAIL_A;
import static com.google.step.TestConstants.RESTAURANT_A;
import static com.google.step.TestConstants.RESTAURANT_A_BRIEF_JSON;
import static com.google.step.TestConstants.SOURCE_A;
import static com.google.step.TestConstants.TAG_A;
import static com.google.step.TestConstants.TAG_A_JSON;
import static com.google.step.TestConstants.TAG_B;
import static com.google.step.TestConstants.TAG_B_JSON;
import static com.google.step.TestConstants.USERNAME_A;
import static com.google.step.TestConstants.USER_A;
import static com.google.step.TestConstants.USER_A_BRIEF_JSON;
import static com.google.step.TestConstants.USER_B;
import static com.google.step.TestConstants.USER_B_BRIEF_JSON;
import static com.google.step.TestConstants.USER_C;
import static com.google.step.TestConstants.USER_C_BRIEF_JSON;
import static com.google.step.TestConstants.USER_ID_A;

import com.google.step.model.Deal;
import com.google.step.model.Restaurant;
import com.google.step.model.Tag;
import com.google.step.model.User;
import java.util.Arrays;
import java.util.List;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

@RunWith(JUnit4.class)
public class JsonFormatterTest {
  @Test
  public void testGetUserJson() {
    List<Deal> deals = Arrays.asList(DEAL_A);
    List<User> following = Arrays.asList(USER_B);
    List<User> followers = Arrays.asList(USER_C);
    List<Tag> tags = Arrays.asList(TAG_A);
    List<Restaurant> restaurants = Arrays.asList(RESTAURANT_A);
    String userJson =
        JsonFormatter.getUserJson(USER_A, deals, following, followers, tags, restaurants);
    String expected =
        String.format(
            "{"
                + "  \"id\": %d,"
                + "  \"email\": \"%s\","
                + "  \"username\": \"%s\","
                + "  \"bio\": \"%s\","
                + "  \"picture\": \"%s\","
                + "  \"dealsUploaded\": [%s],"
                + "  \"following\": [%s],"
                + "  \"followers\": [%s],"
                + "  \"tagsFollowed\": [%s],"
                + "  \"restaurantsFollowed\": [%s]"
                + "}",
            USER_ID_A,
            EMAIL_A,
            USERNAME_A,
            BIO_A,
            BLOBKEY_URL_A,
            DEAL_A_BRIEF_JSON,
            USER_B_BRIEF_JSON,
            USER_C_BRIEF_JSON,
            TAG_A_JSON,
            RESTAURANT_A_BRIEF_JSON);
    try {
      JSONAssert.assertEquals(expected, userJson, JSONCompareMode.STRICT);
    } catch (JSONException e) {
      Assert.fail();
      e.printStackTrace();
    }
  }

  @Test
  public void testGetDealJson() {
    List<Tag> tags = Arrays.asList(TAG_A, TAG_B);
    String dealJson = JsonFormatter.getDealJson(DEAL_A, RESTAURANT_A, USER_A, tags, 123);
    String expected =
        String.format(
            "{"
                + "\"id\": %d,"
                + "\"description\": \"%s\","
                + "\"image\": \"%s\","
                + "\"start\": \"%s\","
                + "\"end\": \"%s\","
                + "\"source\": \"%s\","
                + "\"poster\": %s,"
                + "\"restaurant\": %s,"
                + "\"tags\": [%s,%s],"
                + "\"votes\": 123"
                + "}",
            DEAL_ID_A,
            DESCRIPTION_A,
            BLOBKEY_URL_A,
            DATE_A,
            DATE_B,
            SOURCE_A,
            USER_A_BRIEF_JSON,
            RESTAURANT_A_BRIEF_JSON,
            TAG_A_JSON,
            TAG_B_JSON);

    try {
      JSONAssert.assertEquals(expected, dealJson, JSONCompareMode.STRICT);
    } catch (JSONException e) {
      Assert.fail();
      e.printStackTrace();
    }
  }
}
