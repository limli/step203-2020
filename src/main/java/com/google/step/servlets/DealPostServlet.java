package com.google.step.servlets;

import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.step.datamanager.DealManager;
import com.google.step.datamanager.DealManagerDatastore;
import com.google.step.datamanager.UserManager;
import com.google.step.datamanager.UserManagerDatastore;
import com.google.step.model.Deal;
import com.google.step.model.User;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Servlet that handles posting deals. */
@WebServlet("/api/deals")
public class DealPostServlet extends HttpServlet {

  private final UserService userService;
  private final DealManager dealManager;
  private final UserManager userManager;

  public DealPostServlet(
      DealManager dealManager, UserManager userManager, UserService userService) {
    this.dealManager = dealManager;
    this.userManager = userManager;
    this.userService = userService;
  }

  public DealPostServlet() {
    userService = UserServiceFactory.getUserService();
    dealManager = new DealManagerDatastore();
    userManager = new UserManagerDatastore();
  }

  /** Posts the deal with the given id parameter */
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    if (!userService.isUserLoggedIn()) {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      return;
    }
    System.out.println("post deal");
    String description = request.getParameter("description");
    String photoBlobkey = "TODO"; // TODO connect to blobstore
    String start = request.getParameter("start");
    String end = request.getParameter("end");
    String source = request.getParameter("source");

    String email = userService.getCurrentUser().getEmail();
    User poster = userManager.readUserByEmail(email);
    long posterId = poster.id;

    long restaurantId;
    try {
      restaurantId = Long.parseLong(request.getParameter("restaurant"));
    } catch (NumberFormatException e) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }
    // TODO validate that restaurant ID exists

    // validate required parameters exist
    if (anyEmpty(description, photoBlobkey, start, end)) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }

    // validate dates
    if (!isValidDate(start) || !isValidDate(end)) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }
    if (start.compareTo(end) > 0) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }
    Deal deal =
        dealManager.createDeal(
            description, photoBlobkey, start, end, source, posterId, restaurantId);
    response.sendRedirect("/deals/" + deal.id);
  }

  private boolean anyEmpty(String... strs) {
    for (String str : strs) {
      if (str == null || str.isEmpty()) {
        return true;
      }
    }
    return false;
  }

  private boolean isValidDate(String date) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    try {
      formatter.parse(date);
    } catch (DateTimeParseException e) {
      return false;
    }
    return true;
  }
}
