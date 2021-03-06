package com.google.step.datamanager;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class FollowManagerDatastore implements FollowManager {

  private final String ENTITY_NAME = "Follow";
  private final String FOLLOWER_FIELD_NAME = "follower";
  private final String RESTAURANT_FIELD_NAME = "restaurant";
  private final String USER_FIELD_NAME = "user";
  private final String TAG_FIELD_NAME = "tag";

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

  @Override
  public Set<Long> getFollowedRestaurantIds(long followerId) {
    return getFollowedSomething(followerId, RESTAURANT_FIELD_NAME);
  }

  @Override
  public Set<Long> getFollowedUserIds(long followerId) {
    return getFollowedSomething(followerId, USER_FIELD_NAME);
  }

  @Override
  public Set<Long> getFollowedTagIds(long followerId) {
    return getFollowedSomething(followerId, TAG_FIELD_NAME);
  }

  private Set<Long> getFollowedSomething(long followerId, String fieldName) {
    Filter userFilter = new FilterPredicate(FOLLOWER_FIELD_NAME, FilterOperator.EQUAL, followerId);
    Filter otherFilter = new FilterPredicate(fieldName, FilterOperator.NOT_EQUAL, null);
    Filter filter = CompositeFilterOperator.and(userFilter, otherFilter);
    Query query = new Query(ENTITY_NAME).setFilter(filter);
    PreparedQuery pq = datastore.prepare(query);

    Set<Long> ids = new HashSet<>();
    for (Entity entity : pq.asIterable()) {
      ids.add((Long) entity.getProperty(fieldName));
    }
    return ids;
  }

  @Override
  public Set<Long> getFollowerIdsOfUser(long followeeId) {
    return getFollowersOfSomething(followeeId, USER_FIELD_NAME);
  }

  private Set<Long> getFollowersOfSomething(long followeeId, String fieldName) {
    Filter filter = new FilterPredicate(fieldName, FilterOperator.EQUAL, followeeId);
    Query query = new Query(ENTITY_NAME).setFilter(filter);
    PreparedQuery pq = datastore.prepare(query);
    Set<Long> set = new HashSet<>();
    for (Entity entity : pq.asIterable()) {
      set.add((Long) entity.getProperty(FOLLOWER_FIELD_NAME));
    }
    return set;
  }

  public void updateFollowedTagIds(long followerId, List<Long> tagIds) {
    Set<Long> tagsFollowed = getFollowedTagIds(followerId);
    Set<Long> newTagIds = new HashSet<>(tagIds);
    for (long id : tagsFollowed) {
      if (newTagIds.contains(id)) {
        newTagIds.remove(id);
      } else {
        unfollowTag(followerId, id);
      }
    }

    for (long id : newTagIds) {
      followTag(followerId, id);
    }
  }

  @Override
  public boolean isFollowingUser(long followerId, long followeeId) {
    return isFollowing(followerId, followeeId, USER_FIELD_NAME);
  }

  @Override
  public boolean isFollowingRestaurant(long followerId, long followeeId) {
    return isFollowing(followerId, followeeId, RESTAURANT_FIELD_NAME);
  }

  private boolean isFollowing(long followerId, long followeeId, String followeeFieldName) {
    Filter userFilter = new FilterPredicate(FOLLOWER_FIELD_NAME, FilterOperator.EQUAL, followerId);
    Filter followeeFilter =
        new FilterPredicate(followeeFieldName, FilterOperator.EQUAL, followeeId);
    Filter filter = CompositeFilterOperator.and(userFilter, followeeFilter);
    Query query = new Query(ENTITY_NAME).setFilter(filter);
    PreparedQuery pq = datastore.prepare(query);

    return pq.asSingleEntity() != null;
  }

  @Override
  public void deleteFollowersOfRestaurant(long restaurantId) {
    Filter filter = new FilterPredicate(RESTAURANT_FIELD_NAME, FilterOperator.EQUAL, restaurantId);
    Query query = new Query(ENTITY_NAME).setFilter(filter).setKeysOnly();
    List<Entity> entities = datastore.prepare(query).asList(FetchOptions.Builder.withDefaults());
    List<Key> keys = entities.stream().map(entity -> entity.getKey()).collect(Collectors.toList());
    datastore.delete(keys);
  }
}
