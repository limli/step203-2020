package com.google.step.servlets;

import com.google.gson.Gson;
import com.google.step.datamanager.DealSearchManager;
import com.google.step.datamanager.DealSearchManagerIndex;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Servlet that handles searching of deals. */
@WebServlet("/api/search/deals")
public class DealSearchServlet extends HttpServlet {

  private final DealSearchManager manager;

  public DealSearchServlet() {
    manager = new DealSearchManagerIndex();
  }

  /** Posts the deal with the given id parameter */
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    System.out.println("search deal");
    String query = request.getParameter("q");
    if (query == null) {
      query = "";
    }
    String tags = request.getParameter("tags");
    if (tags == null) {
      tags = "";
    }
    String[] tagsArray = tags.split(",");
    List<Long> tagsList = new ArrayList<>();
    for (int i = 0; i < tagsArray.length; i++) {
      if (tagsArray[i].isEmpty()) {
        continue;
      }
      Long id;
      try {
        id = Long.parseLong(tagsArray[i]);
      } catch (NumberFormatException e) {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        return;
      }
      tagsList.add(id);
    }

    List<Long> dealIds = manager.search(query, tagsList);

    Gson gson = new Gson();
    String json = gson.toJson(dealIds);
    response.getWriter().println(json);
  }
}
