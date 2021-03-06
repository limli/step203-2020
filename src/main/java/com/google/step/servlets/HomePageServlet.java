package com.google.step.servlets;

import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.step.datamanager.DealManager;
import com.google.step.datamanager.DealManagerDatastore;
import com.google.step.datamanager.DealTagManager;
import com.google.step.datamanager.DealTagManagerDatastore;
import com.google.step.datamanager.FollowManager;
import com.google.step.datamanager.FollowManagerDatastore;
import com.google.step.datamanager.RestaurantManager;
import com.google.step.datamanager.RestaurantManagerDatastore;
import com.google.step.datamanager.TagManager;
import com.google.step.datamanager.TagManagerDatastore;
import com.google.step.datamanager.UserManager;
import com.google.step.datamanager.UserManagerDatastore;
import com.google.step.datamanager.VoteManager;
import com.google.step.datamanager.VoteManagerDatastore;
import com.google.step.model.Deal;
import com.google.step.model.Restaurant;
import com.google.step.model.Tag;
import com.google.step.model.User;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Servlet that handles retrieving deals for home page */
@WebServlet("/api/home")
public class HomePageServlet extends HttpServlet {

  private final DealManager dealManager;
  private final UserManager userManager;
  private final VoteManager voteManager;
  private final RestaurantManager restaurantManager;
  private final DealTagManager dealTagManager;
  private final UserService userService;
  private final TagManager tagManager;
  private final FollowManager followManager;

  private final Long OLDEST_DEAL_TIMESTAMP = 1594652120L; // arbitrary datetime of first deal posted
  private final String LOCATION = "Asia/Singapore";

  private static final String TRENDING_SECTION = "trending";
  private static final String USERS_SECTION = "users";
  private static final String RESTAURANTS_SECTION = "restaurants";
  private static final String TAGS_SECTION = "tags";

  public HomePageServlet(
      DealManager dealManager,
      UserManager userManager,
      RestaurantManager restaurantManager,
      VoteManager voteManager,
      DealTagManager dealTagManager,
      TagManager tagManager,
      FollowManager followManager,
      UserService userService) {
    this.dealManager = dealManager;
    this.userManager = userManager;
    this.voteManager = voteManager;
    this.restaurantManager = restaurantManager;
    this.dealTagManager = dealTagManager;
    this.tagManager = tagManager;
    this.followManager = followManager;
    this.userService = userService;
  }

  public HomePageServlet() {
    dealManager = new DealManagerDatastore();
    userManager = new UserManagerDatastore();
    voteManager = new VoteManagerDatastore();
    restaurantManager = new RestaurantManagerDatastore();
    tagManager = new TagManagerDatastore();
    dealTagManager = new DealTagManagerDatastore();
    followManager = new FollowManagerDatastore();
    userService = UserServiceFactory.getUserService();
  }

  /** Class to store deal along with relevant attribute (hot score/votes) to be sorted */
  class ScoredDeal implements Comparable<ScoredDeal> {
    public final Deal deal;
    public final double score;

    public ScoredDeal(Deal deal, double score) {
      this.deal = deal;
      this.score = score;
    }

    @Override
    public int compareTo(ScoredDeal other) {
      return Double.compare(score, other.score);
    }
  }

  /** Gets the deals for the home page/ view all deals */
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String homePageSection = request.getParameter("section");
    if (homePageSection != null
        && !homePageSection.equals(TRENDING_SECTION)
        && !homePageSection.equals(USERS_SECTION)
        && !homePageSection.equals(RESTAURANTS_SECTION)
        && !homePageSection.equals(TAGS_SECTION)) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }
    // if no home page section is being specified to view all deals, return home page data
    if (userService.isUserLoggedIn()) { // all sections are available
      response.setContentType("application/json;");
      response.getWriter().println(userLoggedIn(homePageSection));
    } else {
      String result = userNotLoggedIn(homePageSection);
      if (result != null) {
        response.setContentType("application/json;");
        response.getWriter().println(result);
      } else {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return;
      }
    }
  }

  private String userLoggedIn(String homePageSection) {
    String email = userService.getCurrentUser().getEmail();
    User user = userManager.readUserByEmail(email);
    long userId = user.id;
    if (homePageSection == null) {
      // for each section, limit to 8 deals for home page
      List<List<Map<String, Object>>> homePageDealsMaps =
          getSectionListMaps(homePageSection, userId, 8);
      Map<String, Object> homePageMap = new HashMap<>();
      homePageMap.put(
          TRENDING_SECTION,
          homePageDealsMaps.get(0).stream().limit(8).collect(Collectors.toList()));
      homePageMap.put(USERS_SECTION, homePageDealsMaps.get(1));
      homePageMap.put(RESTAURANTS_SECTION, homePageDealsMaps.get(2));
      homePageMap.put(
          TAGS_SECTION, homePageDealsMaps.get(3).stream().limit(8).collect(Collectors.toList()));
      return JsonFormatter.getHomePageJson(homePageMap);
      // user requested to view all deals of particular section, no
    } else {
      List<List<Map<String, Object>>> homePageDealsMaps =
          getSectionListMaps(homePageSection, userId, -1);
      return JsonFormatter.getHomePageSectionJson(homePageDealsMaps.get(0));
    }
  }

  private String userNotLoggedIn(String homePageSection) {
    if (homePageSection == null) { // only trending will be shown when not logged in
      List<List<Map<String, Object>>> homePageDealsMaps =
          getSectionListMaps(TRENDING_SECTION, -1, -1);
      Map<String, Object> homePageMap = new HashMap<>();
      homePageMap.put(
          TRENDING_SECTION,
          homePageDealsMaps.get(0).stream().limit(8).collect(Collectors.toList()));
      return JsonFormatter.getHomePageJson(homePageMap);
    } else if (homePageSection.equals(TRENDING_SECTION)) {
      // User views all deals for trending section
      List<List<Map<String, Object>>> homePageDealsMaps =
          getSectionListMaps(homePageSection, -1, -1);
      return JsonFormatter.getHomePageSectionJson(homePageDealsMaps.get(0));
    } else { // user is unable to view all deals for other sections when not logged in
      return null;
    }
  }

  /** Gets a list of list of maps based on the required section(s) */
  private List<List<Map<String, Object>>> getSectionListMaps(
      String section, long userId, int limit) {
    List<List<Map<String, Object>>> totalDealMaps = new ArrayList<>();
    if (section == null || section.equals(TRENDING_SECTION)) {
      List<Deal> allDeals = dealManager.getAllDeals();
      List<Deal> trendingDeals = sortDealsBasedOnHotScore(allDeals);
      totalDealMaps.add(getHomePageSectionMap(trendingDeals));
    }
    if (section == null || section.equals(USERS_SECTION)) {
      List<Deal> dealsByUsersFollowed =
          dealManager.getDealsPublishedByUsers(followManager.getFollowedUserIds(userId), limit);
      totalDealMaps.add(getHomePageSectionMap(dealsByUsersFollowed));
    }
    if (section == null || section.equals(RESTAURANTS_SECTION)) {
      List<Deal> dealsByRestaurantsFollowed =
          dealManager.getDealsPublishedByRestaurants(
              followManager.getFollowedRestaurantIds(userId), limit);
      totalDealMaps.add(getHomePageSectionMap(dealsByRestaurantsFollowed));
    }
    if (section == null || section.equals(TAGS_SECTION)) {
      List<Deal> dealsByTagsFollowed =
          getDealsPublishedByTags(followManager.getFollowedTagIds(userId));
      totalDealMaps.add(getHomePageSectionMap(dealsByTagsFollowed));
    }
    return totalDealMaps;
  }

  /** Creates a list of deal maps for a section */
  private List<Map<String, Object>> getHomePageSectionMap(List<Deal> sectionDeals) {
    List<Map<String, Object>> homePageSectionDealMaps = new ArrayList<>();
    for (Deal deal : sectionDeals) {
      User user = userManager.readUser(deal.posterId);
      Restaurant restaurant = restaurantManager.readRestaurant(deal.restaurantId);
      List<Tag> tags = tagManager.readTags(dealTagManager.getTagIdsOfDeal(deal.id));
      int votes = voteManager.getVotes(deal.id);
      Map<String, Object> homePageDealMap =
          JsonFormatter.getBriefHomePageDealMap(deal, user, restaurant, tags, votes);
      homePageSectionDealMaps.add(homePageDealMap);
    }
    return homePageSectionDealMaps;
  }

  /** Retrieves deals posted by tags followed by user */
  private List<Deal> getDealsPublishedByTags(Set<Long> tagIds) {
    Set<Long> dealIdResults = new HashSet<>();
    for (Long id : tagIds) {
      List<Long> dealIdsWithTag = dealTagManager.getDealIdsWithTag(id);
      dealIdResults.addAll(dealIdsWithTag);
    }
    // Get rid of duplicate dealID (Deals with multiple tags)
    List<Long> dealsWithoutDuplicates = new ArrayList<>(dealIdResults);
    return dealManager.readDeals(dealsWithoutDuplicates);
  }

  /** Sorts deals based on new (Newest to oldest) */
  private List<Deal> sortDealsBasedOnNew(List<Deal> deals) {
    Collections.sort(
        deals,
        new Comparator<Deal>() {
          @Override
          public int compare(Deal deal1, Deal deal2) {
            return LocalDateTime.parse(deal2.creationTimeStamp)
                .compareTo(LocalDateTime.parse(deal1.creationTimeStamp)); // Descending
          }
        });
    return deals;
  }

  private double epochSeconds(String timestamp) {
    LocalDateTime time = LocalDateTime.parse(timestamp);
    long epoch = time.atZone(ZoneId.of(LOCATION)).toEpochSecond();
    return epoch;
  }

  /**
   * Calculates a hot score for each deal entity, which takes into account both the time and the
   * amount of votes it got
   */
  private double calculateHotScore(Deal deal, int votes) {
    double order = Math.log(Math.max(Math.abs(votes), 1));
    int sign = 0;
    if (votes > 0) {
      sign = 1;
    } else if (votes < 0) {
      sign = -1;
    }
    double seconds = epochSeconds((String) deal.creationTimeStamp) - OLDEST_DEAL_TIMESTAMP;
    return sign * order + seconds / 45000;
  }

  /** Sorts deals based on hot score */
  private List<Deal> sortDealsBasedOnHotScore(List<Deal> deals) {
    List<ScoredDeal> scoredDeals = new ArrayList<>();
    for (Deal deal : deals) {
      int votes = voteManager.getVotes(deal.id);
      scoredDeals.add(new ScoredDeal(deal, calculateHotScore(deal, votes)));
    }
    Collections.sort(scoredDeals, Collections.reverseOrder());
    List<Deal> dealResults = new ArrayList<>(); // creating list of deals
    for (ScoredDeal scoredDeal : scoredDeals) {
      dealResults.add(scoredDeal.deal);
    }
    return dealResults;
  }

  /** Sorts deals based on votes */
  private List<Deal> sortDealsBasedOnVotes(List<Deal> deals) {
    List<ScoredDeal> scoredDeals = new ArrayList<>();
    for (Deal deal : deals) {
      int votes = voteManager.getVotes(deal.id);
      scoredDeals.add(new ScoredDeal(deal, votes));
    }
    Collections.sort(scoredDeals);
    List<Deal> dealResults = new ArrayList<>(); // creating list of deals
    for (ScoredDeal scoredDeal : scoredDeals) {
      dealResults.add(scoredDeal.deal);
    }
    return dealResults;
  }
}
