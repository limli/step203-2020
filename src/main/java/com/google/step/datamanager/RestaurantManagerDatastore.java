package com.google.step.datamanager;

import com.google.appengine.api.datastore.DatastoreFailureException;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.step.model.Restaurant;
import com.google.step.servlets.ImageUploader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class RestaurantManagerDatastore implements RestaurantManager {

  private final DatastoreService datastore;

  public RestaurantManagerDatastore() {
    datastore = DatastoreServiceFactory.getDatastoreService();
  }

  /** Creates a new restaurant entity */
  @Override
  public Restaurant createRestaurantWithBlobKey(String name, String photoBlobKey, long posterId) {
    Entity entity = new Entity("Restaurant");
    entity.setProperty("name", name);
    entity.setProperty("photoUrl", Restaurant.getImageUrlFromBlobKey(photoBlobKey));
    entity.setProperty("name_lowercase", name.toLowerCase());
    entity.setProperty("posterId", posterId);

    Key key = datastore.put(entity);
    long id = key.getId();

    return Restaurant.createRestaurantWithBlobkey(id, name, photoBlobKey, posterId);
  }

  @Override
  public Restaurant createRestaurantWithPhotoReference(
      String name, String photoReference, long posterId) {
    Entity entity = new Entity("Restaurant");
    entity.setProperty("name", name);
    entity.setProperty("photoUrl", Restaurant.getImageUrlFromPhotoReference(photoReference));
    entity.setProperty("name_lowercase", name.toLowerCase());
    entity.setProperty("posterId", posterId);

    Key key = datastore.put(entity);
    long id = key.getId();

    return Restaurant.createRestaurantWithPhotoReference(id, name, photoReference, posterId);
  }

  /** Gets info on a restaurant given an id */
  @Override
  public Restaurant readRestaurant(long id) {
    Key key = KeyFactory.createKey("Restaurant", id);
    Entity restaurantEntity;
    try {
      restaurantEntity = datastore.get(key);
    } catch (EntityNotFoundException e) {
      return null;
    }
    return transformEntityToRestaurant(restaurantEntity);
  }

  /** Updates restaurant info given an id */
  @Override
  public Restaurant updateRestaurant(Restaurant restaurant) {
    Key key = KeyFactory.createKey("Restaurant", restaurant.id);
    Entity restaurantEntity;
    try {
      restaurantEntity = datastore.get(key);
    } catch (EntityNotFoundException e) {
      return null;
    }
    if (restaurant.name != null) {
      restaurantEntity.setProperty("name", restaurant.name);
      restaurantEntity.setProperty("name_lowercase", restaurant.name.toLowerCase());
    }
    if (restaurant.photoUrl != null) {
      restaurantEntity.setProperty("photoUrl", restaurant.photoUrl);
    }
    datastore.put(restaurantEntity);
    return transformEntityToRestaurant(restaurantEntity);
  }

  /** Deletes restaurant given an id */
  @Override
  public void deleteRestaurant(long id) {
    Key key = KeyFactory.createKey("Restaurant", id);
    Restaurant restaurant = readRestaurant(id);
    datastore.delete(key);
    ImageUploader.deleteImage(ImageUploader.getBlobKeyFromUrl(restaurant.photoUrl));
  }

  @Override
  public List<Restaurant> searchRestaurants(String queryString) {
    queryString = queryString.toLowerCase();
    Filter filterPrefix =
        CompositeFilterOperator.and(
            FilterOperator.GREATER_THAN_OR_EQUAL.of("name_lowercase", queryString),
            FilterOperator.LESS_THAN.of("name_lowercase", queryString + "~"));
    Query query = new Query("Restaurant").setFilter(filterPrefix);
    PreparedQuery preparedQuery = datastore.prepare(query);

    FetchOptions limitQueries = FetchOptions.Builder.withLimit(20);

    List<Restaurant> restaurants = new ArrayList<>();
    for (Entity entity : preparedQuery.asIterable(limitQueries)) {
      restaurants.add(transformEntityToRestaurant(entity));
    }

    return restaurants;
  }

  /**
   * Returns a Restaurant object transformed from a restaurant entity.
   *
   * @param restaurantEntity Restaurant entity.
   * @return a Restaurant object transformed from the entity.
   */
  private Restaurant transformEntityToRestaurant(Entity restaurantEntity) {
    String name = (String) restaurantEntity.getProperty("name");
    String photoUrl = (String) restaurantEntity.getProperty("photoUrl");
    long posterId = (long) restaurantEntity.getProperty("posterId");
    long id = restaurantEntity.getKey().getId();

    return Restaurant.createRestaurantWithPhotoUrl(id, name, photoUrl, posterId);
  }

  @Override
  public void deleteAllRestaurants() {
    Query query = new Query("Restaurant");
    PreparedQuery preparedQuery = datastore.prepare(query);

    List<Entity> entities = preparedQuery.asList(FetchOptions.Builder.withDefaults());
    List<Key> keys = entities.stream().map(entity -> entity.getKey()).collect(Collectors.toList());
    datastore.delete(keys);
    String[] blobKeys =
        entities.stream()
            .map(entity -> ImageUploader.getBlobKeyFromUrl((String) entity.getProperty("photoUrl")))
            .filter(blobKey -> blobKey != null)
            .toArray(String[]::new);
    ImageUploader.deleteImage(blobKeys);
  }

  @Override
  public List<Restaurant> readRestaurants(List<Long> ids) {
    List<Key> keys =
        ids.stream().map(id -> KeyFactory.createKey("Restaurant", id)).collect(Collectors.toList());
    Collection<Entity> restaurantEntities;
    try {
      restaurantEntities = datastore.get(keys).values();
    } catch (IllegalArgumentException | DatastoreFailureException e) {
      e.printStackTrace();
      return new ArrayList<>();
    }
    List<Restaurant> restaurants =
        restaurantEntities.stream()
            .map(entity -> transformEntityToRestaurant(entity))
            .collect(Collectors.toList());
    return restaurants;
  }

  @Override
  public List<Restaurant> getAllRestaurants() {
    List<Restaurant> restaurants = new ArrayList<>();
    Query query = new Query("Restaurant");
    PreparedQuery pq = datastore.prepare(query);
    for (Entity restaurantEntity : pq.asIterable()) {
      restaurants.add(transformEntityToRestaurant(restaurantEntity));
    }
    return restaurants;
  }
}
