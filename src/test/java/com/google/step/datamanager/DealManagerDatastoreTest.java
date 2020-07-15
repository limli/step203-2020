package com.google.step.datamanager;

import static com.google.step.TestConstants.BLOBKEY_A;
import static com.google.step.TestConstants.BLOBKEY_B;
import static com.google.step.TestConstants.DATE_A;
import static com.google.step.TestConstants.DATE_B;
import static com.google.step.TestConstants.DATE_C;
import static com.google.step.TestConstants.DATE_D;
import static com.google.step.TestConstants.DEAL_A;
import static com.google.step.TestConstants.DEAL_ID_A;
import static com.google.step.TestConstants.DESCRIPTION_A;
import static com.google.step.TestConstants.DESCRIPTION_B;
import static com.google.step.TestConstants.RESTAURANT_ID_A;
import static com.google.step.TestConstants.RESTAURANT_ID_B;
import static com.google.step.TestConstants.SOURCE_A;
import static com.google.step.TestConstants.SOURCE_B;
import static com.google.step.TestConstants.USER_ID_A;
import static com.google.step.TestConstants.USER_ID_B;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.step.model.Deal;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class DealManagerDatastoreTest {

  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());
  private final DealSearchManager searchManager = mock(DealSearchManager.class);

  private final DealManagerDatastore dealManagerDatastore = new DealManagerDatastore(searchManager);

  @Before
  public void setUp() {
    helper.setUp();
  }

  @After
  public void tearDown() {
    helper.tearDown();
  }

  @Test
  public void testCreate_success() {
    Deal deal =
        dealManagerDatastore.createDeal(
            DESCRIPTION_A, BLOBKEY_A, DATE_A, DATE_B, SOURCE_A, USER_ID_A, RESTAURANT_ID_A);
    assertEquals(DEAL_A, deal);
  }

  @Test
  public void testRead_success() {
    Deal createdDeal =
        dealManagerDatastore.createDeal(
            DESCRIPTION_A, BLOBKEY_A, DATE_A, DATE_B, SOURCE_A, USER_ID_A, RESTAURANT_ID_A);
    Deal deal = dealManagerDatastore.readDeal(createdDeal.id);
    assertEquals(DEAL_A, deal);
  }

  @Test
  public void testRead_invalidId_returnsNull() {
    Deal deal = dealManagerDatastore.readDeal(DEAL_ID_A);
    assertNull(deal);
  }

  @Test
  public void testDelete() {
    Deal createdDeal =
        dealManagerDatastore.createDeal(
            DESCRIPTION_A, BLOBKEY_A, DATE_A, DATE_B, SOURCE_A, USER_ID_A, RESTAURANT_ID_A);
    dealManagerDatastore.deleteDeal(createdDeal.id);
    Deal deal = dealManagerDatastore.readDeal(createdDeal.id);
    assertNull(deal);
  }

  @Test
  public void testUpdate_invalidId_returnsNull() {
    Deal deal = new Deal(DEAL_ID_A, null, null, null, null, null, -1, -1);
    Deal updatedDeal = dealManagerDatastore.updateDeal(deal);
    assertNull(updatedDeal);
  }

  @Test
  public void testUpdate_descriptionOnly() {
    Deal createdDeal =
        dealManagerDatastore.createDeal(
            DESCRIPTION_A, BLOBKEY_A, DATE_A, DATE_B, SOURCE_A, USER_ID_A, RESTAURANT_ID_A);
    Deal dealToUpdate = new Deal(createdDeal.id, DESCRIPTION_B, null, null, null, null, -1, -1);
    Deal updatedDeal = dealManagerDatastore.updateDeal(dealToUpdate);

    // only description should change, everything else should remain
    Deal expected =
        new Deal(
            DEAL_ID_A,
            DESCRIPTION_B,
            BLOBKEY_A,
            DATE_A,
            DATE_B,
            SOURCE_A,
            USER_ID_A,
            RESTAURANT_ID_A);
    assertEquals(expected, updatedDeal);
  }

  @Test
  public void testUpdate_poster_noChange() {
    // method should not allow updating of poster
    Deal createdDeal =
        dealManagerDatastore.createDeal(
            DESCRIPTION_A, BLOBKEY_A, DATE_A, DATE_B, SOURCE_A, USER_ID_A, RESTAURANT_ID_A);
    Deal dealToUpdate = new Deal(createdDeal.id, null, null, null, null, null, USER_ID_B, -1);
    Deal updatedDeal = dealManagerDatastore.updateDeal(dealToUpdate);
    assertEquals(DEAL_A, updatedDeal);
  }

  @Test
  public void testUpdate_allFields() {
    Deal createdDeal =
        dealManagerDatastore.createDeal(
            DESCRIPTION_A, BLOBKEY_A, DATE_A, DATE_B, SOURCE_A, USER_ID_A, RESTAURANT_ID_A);
    Deal dealToUpdate =
        new Deal(
            createdDeal.id,
            DESCRIPTION_B,
            BLOBKEY_B,
            DATE_C,
            DATE_D,
            SOURCE_B,
            -1,
            RESTAURANT_ID_B);
    Deal updatedDeal = dealManagerDatastore.updateDeal(dealToUpdate);

    Deal expected =
        new Deal(
            createdDeal.id,
            DESCRIPTION_B,
            BLOBKEY_B,
            DATE_C,
            DATE_D,
            SOURCE_B,
            USER_ID_A,
            RESTAURANT_ID_B);
    assertEquals(expected, updatedDeal);
  }
}