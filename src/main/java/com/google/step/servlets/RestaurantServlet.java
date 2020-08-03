package com.google.step.servlets;

import com.google.step.datamanager.DealManager;
import com.google.step.datamanager.DealManagerDatastore;
import com.google.step.datamanager.FollowManager;
import com.google.step.datamanager.FollowManagerDatastore;
import com.google.step.datamanager.RestaurantManager;
import com.google.step.datamanager.RestaurantManagerDatastore;
import com.google.step.datamanager.RestaurantPlaceManager;
import com.google.step.datamanager.RestaurantPlaceManagerDatastore;
import com.google.step.model.Deal;
import com.google.step.model.Restaurant;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Servlet that handles individual restaurants */
@WebServlet("/api/restaurants/*")
public class RestaurantServlet extends HttpServlet {

  private RestaurantManager restaurantManager;
  private DealManager dealManager;
  private RestaurantPlaceManager restaurantPlaceManager;
  private FollowManager followManager;

  public RestaurantServlet(
      RestaurantManager restaurantManager,
      DealManager dealManager,
      RestaurantPlaceManager restaurantPlaceManager,
      FollowManager followManager) {
    this.restaurantManager = restaurantManager;
    this.dealManager = dealManager;
    this.restaurantPlaceManager = restaurantPlaceManager;
    this.followManager = followManager;
  }

  public RestaurantServlet() {
    restaurantManager = new RestaurantManagerDatastore();
    dealManager = new DealManagerDatastore();
    restaurantPlaceManager = new RestaurantPlaceManagerDatastore();
    followManager = new FollowManagerDatastore();
  }

  /** Deletes the restaurant with the given id parameter */
  @Override
  public void doDelete(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    long id;
    try {
      id = Long.parseLong(request.getPathInfo().substring(1));
    } catch (NumberFormatException e) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }
    restaurantManager.deleteRestaurant(id);
    restaurantPlaceManager.deletePlacesOfRestaurant(id);
    followManager.deleteFollowersOfRestaurant(id);
  }

  /** Gets the restaurant with the given id parameter */
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    long id;
    try {
      id = Long.parseLong(request.getPathInfo().substring(1));
    } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }
    Restaurant restaurant = restaurantManager.readRestaurant(id);
    if (restaurant == null) {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    List<Deal> deals = dealManager.getDealsOfRestaurant(id);
    List<String> placeIds = new ArrayList<>(restaurantPlaceManager.getPlaceIdsOfRestaurant(id));

    response.setContentType("application/json;");
    response.getWriter().println(JsonFormatter.getRestaurantJson(restaurant, deals, placeIds));
  }

  /** Updates a restaurant with the given id parameter */
  @Override
  public void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
    long id;
    try {
      id = Long.parseLong(request.getPathInfo().substring(1));
    } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }
    String name = request.getParameter("name");
    String photoBlobkey = "A_BLOB_KEY"; // TODO Blobkey
    Restaurant restaurant = Restaurant.createRestaurantWithBlobkey(id, name, photoBlobkey);
    Restaurant updatedRestaurant = restaurantManager.updateRestaurant(restaurant);
    if (updatedRestaurant == null) {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    } else {
      List<Deal> deals = dealManager.getDealsOfRestaurant(id);
      List<String> placeIds = new ArrayList<>(restaurantPlaceManager.getPlaceIdsOfRestaurant(id));
      response
          .getWriter()
          .println(JsonFormatter.getRestaurantJson(updatedRestaurant, deals, placeIds));
    }
  }
}
