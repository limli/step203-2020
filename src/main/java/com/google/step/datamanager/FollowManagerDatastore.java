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

public class FollowManagerDatastore implements FollowManager {

  private final String ENTITY_NAME = "Follow";
  private final String FOLLOWER_FIELD_NAME = "follower";
  private final String RESTAURANT_FIELD_NAME = "restaurant";
  private final String USER_FIELD_NAME = "restaurant";
  private final String TAG_FIELD_NAME = "restaurant";

  private final DatastoreService datastore;

  public FollowManagerDatastore() {
    datastore = DatastoreServiceFactory.getDatastoreService();
  }

  @Override
  public void followRestaurant(long followerId, long restaurantId) {
    followSomething(followerId, restaurantId, RESTAURANT_FIELD_NAME);
  }

  @Override
  public void followUser(long followerId, long userId) {
    followSomething(followerId, userId, USER_FIELD_NAME);
  }

  @Override
  public void followTag(long followerId, long tagId) {
    followSomething(followerId, tagId, TAG_FIELD_NAME);
  }

  @Override
  public void unfollowRestaurant(long followerId, long restaurantId) {
    unfollowSomething(followerId, restaurantId, RESTAURANT_FIELD_NAME);
  }

  @Override
  public void unfollowUser(long followerId, long userId) {
    unfollowSomething(followerId, userId, USER_FIELD_NAME);
  }

  @Override
  public void unfollowTag(long followerId, long tagId) {
    unfollowSomething(followerId, tagId, TAG_FIELD_NAME);
  }

  private Entity getEntity(long followerId, long fieldId, String fieldName) {
    Filter userFilter = new FilterPredicate(FOLLOWER_FIELD_NAME, FilterOperator.EQUAL, followerId);
    Filter otherFilter = new FilterPredicate(fieldName, FilterOperator.EQUAL, fieldId);
    Filter filter = CompositeFilterOperator.and(userFilter, otherFilter);
    Query query = new Query(ENTITY_NAME).setFilter(filter);
    PreparedQuery pq = datastore.prepare(query);
    Entity entity = pq.asSingleEntity();
    return entity;
  }

  private void followSomething(long followerId, long fieldId, String fieldName) {
    Entity entity = getEntity(followerId, fieldId, fieldName);

    if (entity != null) {
      return;
    }

    entity = new Entity(ENTITY_NAME);
    entity.setProperty(FOLLOWER_FIELD_NAME, followerId);
    entity.setProperty(fieldName, fieldId);
    datastore.put(entity);
  }

  private void unfollowSomething(long followerId, long fieldId, String fieldName) {
    Entity entity = getEntity(followerId, fieldId, fieldName);

    if (entity == null) {
      return;
    }

    datastore.delete(entity.getKey());
  }
}
