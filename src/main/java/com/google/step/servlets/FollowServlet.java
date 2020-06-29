package com.google.step.servlets;

import com.google.step.datamanager.FollowManager;
import com.google.step.datamanager.FollowManagerDatastore;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/api/follows/*")
public class FollowServlet extends HttpServlet {

  private final FollowManager manager;

  public FollowServlet() {
    manager = new FollowManagerDatastore();
  }

  /** Follows a restaurant, tag, or another user */
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    if (request.getPathInfo().length() == 0) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }
    String pathInfo = request.getPathInfo().substring(1);
    long id = getId(pathInfo);
    if (id == -1) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }

    long followerId = 3141; // TODO: check user authentication

    if (pathInfo.startsWith("retaurant/")) {
      manager.followRestaurant(followerId, id);
    } else if (pathInfo.startsWith("tags/")) {
      manager.followTag(followerId, id);
    } else if (pathInfo.startsWith("users/")) {
      manager.followUser(followerId, id);
    } else {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }
    response.setStatus(HttpServletResponse.SC_OK);
  }

  /** Unfollows a restaurant, tag, or another user */
  @Override
  public void doDelete(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    if (request.getPathInfo().length() == 0) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }
    String pathInfo = request.getPathInfo().substring(1);
    long id = getId(pathInfo);
    if (id == -1) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }

    long followerId = 3141; // TODO: check user authentication

    if (pathInfo.startsWith("retaurant/")) {
      manager.unfollowRestaurant(followerId, id);
    } else if (pathInfo.startsWith("tags/")) {
      manager.unfollowTag(followerId, id);
    } else if (pathInfo.startsWith("users/")) {
      manager.unfollowUser(followerId, id);
    } else {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }
    response.setStatus(HttpServletResponse.SC_OK);
  }

  /**
   * Parses the String to the right of the first forward slash '/' as a long and returns it. If not
   * possible, returns -1;
   */
  private long getId(String pathInfo) {
    for (int i = 0; i < pathInfo.length(); i++) {
      if (pathInfo.charAt(i) == '/') {
        String rest = pathInfo.substring(i + 1);
        long id;
        try {
          id = Long.parseLong(rest);
        } catch (NumberFormatException e) {
          return -1;
        }
        return id;
      }
    }
    return -1;
  }
}
