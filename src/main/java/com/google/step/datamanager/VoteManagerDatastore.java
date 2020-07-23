package com.google.step.datamanager;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;

public class VoteManagerDatastore implements VoteManager {

  private final DatastoreService datastore;

  public VoteManagerDatastore() {
    datastore = DatastoreServiceFactory.getDatastoreService();
  }

  @Override
  public int getVotes(long dealId) {
    Filter dealFilter = new FilterPredicate("deal", FilterOperator.EQUAL, dealId);
    Query query = new Query("DealVote").setFilter(dealFilter);
    PreparedQuery pq = datastore.prepare(query);
    Entity entity = pq.asSingleEntity();
    if (entity == null) {
      return 0;
    }
    int votes = (int) (long) entity.getProperty("votes");
    return votes;
  }

  @Override
  public void vote(long userId, long dealId, int dir) {
    // check if this user has voted on this deal before
    Filter userFilter = new FilterPredicate("user", FilterOperator.EQUAL, userId);
    Filter dealFilter = new FilterPredicate("deal", FilterOperator.EQUAL, dealId);
    Filter filter = CompositeFilterOperator.and(userFilter, dealFilter);
    Query query = new Query("Vote").setFilter(filter);
    PreparedQuery pq = datastore.prepare(query);
    Entity entity = pq.asSingleEntity();

    // If entity exists, use it. Else, create a new entity.
    if (entity == null) {
      entity = new Entity("Vote");
      entity.setProperty("user", userId);
      entity.setProperty("deal", dealId);
      updateDealVotes(dealId, dir);
    } else {
      // if user voted before
      int prevDir = (int) (long) entity.getProperty("dir");
      if (prevDir != dir) {
        if (dir == 0) {
          updateDealVotes(dealId, -prevDir);
        } else {
          updateDealVotes(dealId, dir * 2);
        }
      }
    }
    entity.setProperty("dir", dir);
    datastore.put(entity);
  }

  private void updateDealVotes(long dealId, int dir) {
    // check if this user has voted on this deal before
    Filter filter = new FilterPredicate("deal", FilterOperator.EQUAL, dealId);
    Query query = new Query("DealVote").setFilter(filter);
    PreparedQuery pq = datastore.prepare(query);
    Entity entity = pq.asSingleEntity();

    // If the deal has not been voted before
    if (entity == null) {
      entity = new Entity("DealVote");
      entity.setProperty("deal", dealId);
      entity.setProperty("votes", dir);
    } else {
      int votes = (int) (long) entity.getProperty("votes");
      entity.setProperty("votes", votes + dir);
    }
    datastore.put(entity);
  }

  @Override
  public int getDirection(long userId, long dealId) {
    // check if this user has voted on this deal before
    Filter userFilter = new FilterPredicate("user", FilterOperator.EQUAL, userId);
    Filter dealFilter = new FilterPredicate("deal", FilterOperator.EQUAL, dealId);
    Filter filter = CompositeFilterOperator.and(userFilter, dealFilter);
    Query query = new Query("Vote").setFilter(filter);
    PreparedQuery pq = datastore.prepare(query);
    Entity entity = pq.asSingleEntity();

    // If user have not voted before return 0. Else, return the value of the entity.
    if (entity == null) {
      return 0;
    }
    return ((Long) entity.getProperty("dir")).intValue();
  }
}
