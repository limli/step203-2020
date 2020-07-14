package com.google.step.servlets;

import static com.google.step.servlets.ImageUploader.getUploadedImageBlobkey;

import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.step.datamanager.UserManager;
import com.google.step.datamanager.UserManagerDatastore;
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

/** Servlet that handles users. */
@WebServlet("/api/users/*")
public class UserServlet extends HttpServlet {

  private UserManager userManager = new UserManagerDatastore();
  private UserService userService = UserServiceFactory.getUserService();

  public UserServlet(UserManager userManager, UserService userService) {
    super();
    this.userManager = userManager;
    this.userService = userService;
  }

  public UserServlet() {
    super();
  }

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    long id;
    try {
      String idString = request.getPathInfo().substring(1); // Remove '/'
      id = Long.parseLong(idString);
    } catch (IndexOutOfBoundsException | NumberFormatException | NullPointerException e) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }
    User user;
    try {
      user = userManager.readUser(id);
    } catch (IllegalArgumentException e) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }

    List<Deal> deals = new ArrayList<>(); // dealManager.getDealsPublishedByUser(id);
    List<User> following = new ArrayList<>(); // followManager.getUsersFollowedByUser(id);
    List<User> followers = new ArrayList<>(); // followManager.getUsersFollowingUser(id);
    List<Tag> tags = new ArrayList<>(); // followManager.getTagsFollowedByUser(id);
    List<Restaurant> restaurants =
        new ArrayList<>(); // followManager.getRestaurantsFollowedByUser(id);

    String json = JsonFormatter.getUserJson(user, deals, following, followers, tags, restaurants);
    response.setContentType("application/json");
    response.getWriter().println(json);
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    if (!userService.isUserLoggedIn()) {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      return;
    }

    long id;
    User user;
    try {
      String idString = request.getPathInfo().substring(1); // remove '/'
      id = Long.parseLong(idString);
      user = userManager.readUser(id);
    } catch (IndexOutOfBoundsException | IllegalArgumentException | NullPointerException e) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }

    String userEmail = userService.getCurrentUser().getEmail();
    if (!userEmail.equals(user.email)) {
      // Inconsistent request with login status
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }

    String username = (String) request.getParameter("username");
    String bio = (String) request.getParameter("bio");
    String photoBlobKey = getUploadedImageBlobkey(request, "picture");
    boolean isUsingDefaultPhoto = request.getParameter("default-photo") != null;

    User updatedUser;
    if (isUsingDefaultPhoto) {
      updatedUser = new User(user.id, null, username, bio);
    } else if (photoBlobKey != null) {
      updatedUser = new User(user.id, null, username, photoBlobKey, bio);
    } else {
      updatedUser = new User(user.id, null, username, null, bio);
    }

    userManager.updateUser(updatedUser);
    response.sendRedirect("/user/" + user.id);
  }
}
