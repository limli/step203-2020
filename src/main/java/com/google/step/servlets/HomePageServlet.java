package com.google.step.servlets;

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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
  private final TagManager tagManager;
  private final FollowManager followManager;

  private final Long OLDEST_DEAL_TIMESTAMP = 1594652120L; // arbitrary datetime of first deal posted
  private final String LOCATION = "Asia/Singapore";

  public HomePageServlet(
      DealManager dealManager,
      UserManager userManager,
      RestaurantManager restaurantManager,
      VoteManager voteManager,
      DealTagManager dealTagManager,
      TagManager tagManager,
      FollowManager followManager) {
    this.dealManager = dealManager;
    this.userManager = userManager;
    this.voteManager = voteManager;
    this.restaurantManager = restaurantManager;
    this.dealTagManager = dealTagManager;
    this.tagManager = tagManager;
    this.followManager = followManager;
  }

  public HomePageServlet() {
    dealManager = new DealManagerDatastore();
    userManager = new UserManagerDatastore();
    voteManager = new VoteManagerDatastore();
    restaurantManager = new RestaurantManagerDatastore();
    tagManager = new TagManagerDatastore();
    dealTagManager = new DealTagManagerDatastore();
    followManager = new FollowManagerDatastore();
  }

  /** Class to store deal along with relevant attribute (hot score/votes) to be sorted */
  class DealPair<T extends Comparable<T>> implements Comparable<DealPair<T>> {
    public final T key;
    public final Deal deal;

    public DealPair(T key, Deal deal) {
      this.key = key;
      this.deal = deal;
    }

    @Override
    public int compareTo(DealPair<T> other) {
      return key.compareTo(other.key);
    }
  }

  /** Gets the deals for the home page */
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    long userId = 1;
    List<Deal> allDeals = dealManager.getAllDeals();
    List<Deal> trendingDeals = sortDealsBasedOnHotScore(allDeals);
    List<Deal> dealsByUsersFollowed =
        dealManager.getDealsPublishedByUsers(followManager.getFollowedUserIds(userId));
    List<Deal> dealsByRestaurantsFollowed =
        dealManager.getDealsPublishedByRestaurants(followManager.getFollowedRestaurantIds(userId));
    List<Deal> dealsByTagsFollowed =
        getDealsPublishedByTags(followManager.getFollowedTagIds(userId));
    List<List<Deal>> homePageDeals =
        new ArrayList<>(
            Arrays.asList(
                trendingDeals,
                dealsByUsersFollowed,
                dealsByRestaurantsFollowed,
                dealsByTagsFollowed));
    response.setContentType("application/json;");

    List<List<Map<String, Object>>> homePageDealsMaps = getHomePageDealMaps(homePageDeals);
    Map<String, Object> homePageMap = new HashMap<>();
    homePageMap.put("popularDeals", homePageDealsMaps.get(0));
    homePageMap.put("usersIFollow", homePageDealsMaps.get(1));
    homePageMap.put("restaurantsIFollow", homePageDealsMaps.get(2));
    homePageMap.put("tagsIFollow", homePageDealsMaps.get(3));
    response.getWriter().println(JsonFormatter.getHomePageJson(homePageMap));
  }

  /** Creates a list of list of deal maps for the home page */
  private List<List<Map<String, Object>>> getHomePageDealMaps(List<List<Deal>> homePageDeals) {
    List<List<Map<String, Object>>> homePageDealsMapList = new ArrayList<>();
    for (List<Deal> dealList : homePageDeals) {
      List<Map<String, Object>> homePageSectionDealMaps = new ArrayList<>();
      for (Deal deal : dealList) {
        User user = userManager.readUser(deal.posterId);
        Restaurant restaurant = restaurantManager.readRestaurant(deal.restaurantId);
        List<Tag> tags = tagManager.readTags(dealTagManager.getTagIdsOfDeal(deal.id));
        int votes = voteManager.getVotes(deal.id);
        Map<String, Object> homePageDealMap =
            JsonFormatter.getBriefHomePageDealMap(deal, user, restaurant, tags, votes);
        homePageSectionDealMaps.add(homePageDealMap);
      }
      homePageDealsMapList.add(homePageSectionDealMaps);
    }
    return homePageDealsMapList;
  }

  /** Retrieves deals posted by tags followed by user */
  private List<Deal> getDealsPublishedByTags(Set<Long> tagIds) {
    List<Long> dealIdResults = new ArrayList<>();
    for (Long id : tagIds) {
      List<Long> dealIdsWithTag = dealTagManager.getDealIdsWithTag(id);
      dealIdResults.addAll(dealIdsWithTag);
    }
    // Get rid of duplicate dealID (Deals with multiple tags)
    List<Long> dealsWithoutDuplicates = new ArrayList<>(new HashSet<>(dealIdResults));
    return dealManager.readDeals(dealsWithoutDuplicates);
  }

  /** Sorts deals based on new (Newest to oldest) */
  private List<Deal> sortDealsBasedOnNew(List<Deal> deals) {
    Collections.sort(
        deals,
        new Comparator<Deal>() {
          @Override
          public int compare(Deal deal1, Deal deal2) { // TODO fix parse
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
    if (votes > 0) sign = 1;
    else if (votes < 0) sign = -1;
    double seconds = epochSeconds((String) deal.creationTimeStamp) - OLDEST_DEAL_TIMESTAMP;
    return sign * order + seconds / 45000;
  }

  /** Sorts deals based on value (hot score or votes) */
  private List<Deal> sortDealsBasedOnHotScore(List<Deal> deals) {
    List<DealPair> dealPairs = new ArrayList<DealPair>();
    for (Deal deal : deals) {
      int votes = voteManager.getVotes(deal.id);
      dealPairs.add(new DealPair(calculateHotScore(deal, votes), deal));
    }
    Collections.sort(dealPairs);
    List<Deal> dealResults = new ArrayList<>(); // creating list of deals
    for (DealPair dealPair : dealPairs) {
      dealResults.add(dealPair.deal);
    }
    return dealResults;
  }

  /** Sorts deals based on value (hot score or votes) */
  private List<Deal> sortDealsBasedOnVotes(List<Deal> deals) {
    List<DealPair> dealPairs = new ArrayList<DealPair>();
    for (Deal deal : deals) {
      int votes = voteManager.getVotes(deal.id);
      dealPairs.add(new DealPair(votes, deal));
    }
    Collections.sort(dealPairs);
    List<Deal> dealResults = new ArrayList<>(); // creating list of deals
    for (DealPair dealPair : dealPairs) {
      dealResults.add(dealPair.deal);
    }
    return dealResults;
  }
}
