package com.google.step.servlets;

import com.google.step.datamanager.DealManager;
import com.google.step.datamanager.DealManagerDatastore;
import com.google.step.model.Deal;
import com.google.step.model.Restaurant;
import com.google.step.model.Tag;
import com.google.step.model.User;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Servlet that handles individual deals. */
@WebServlet("/api/deals/*")
public class DealDetailServlet extends HttpServlet {

  private final DealManager manager;

  public DealDetailServlet() {
    manager = new DealManagerDatastore();
  }

  /** Deletes the deal with the given id parameter */
  @Override
  public void doDelete(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    // TODO: check user authentication
    long id;
    try {
      id = Long.parseLong(request.getPathInfo().substring(1));
    } catch (NumberFormatException e) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }
    manager.deleteDeal(id);
  }

  /** Gets the deal with the given id parameter */
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    long id;
    try {
      id = Long.parseLong(request.getPathInfo().substring(1));
    } catch (NumberFormatException e) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }
    Deal deal = manager.readDeal(id);
    if (deal == null) {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    // TODO get real restaurant
    Restaurant restaurant = new Restaurant(deal.restaurantId, "Restaurant Name", "ablobkey");

    // TODO get real poster
    User poster = new User(deal.posterId, "a@a.com");

    // TODO get real tags
    List<Tag> tags = new ArrayList<>();

    // TODO get real votes
    int votes = 123;

    response.setContentType("application/json;");
    response.getWriter().println(JsonFormatter.getDealJson(deal, restaurant, poster, tags, votes));
  }
}
