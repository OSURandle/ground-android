/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.ground.repository

import com.google.android.ground.BaseHiltTest
import com.google.android.ground.domain.usecases.survey.ActivateSurveyUseCase
import com.google.android.ground.model.geometry.*
import com.google.android.ground.model.mutation.Mutation.Type.CREATE
import com.google.android.ground.persistence.sync.MutationSyncWorkManager
import com.google.android.ground.ui.map.Bounds
import com.sharedtest.FakeData
import com.sharedtest.persistence.remote.FakeRemoteDataStore
import com.sharedtest.system.auth.FakeAuthenticationManager
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import io.reactivex.Completable
import io.reactivex.Flowable
import java.util.*
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.*
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class LocationOfInterestRepositoryTest : BaseHiltTest() {
  @BindValue @Mock lateinit var mockWorkManager: MutationSyncWorkManager

  @Inject lateinit var fakeAuthenticationManager: FakeAuthenticationManager
  @Inject lateinit var fakeRemoteDataStore: FakeRemoteDataStore
  @Inject lateinit var locationOfInterestRepository: LocationOfInterestRepository
  @Inject lateinit var userRepository: UserRepository
  @Inject lateinit var activateSurvey: ActivateSurveyUseCase

  private val mutation = LOCATION_OF_INTEREST.toMutation(CREATE, TEST_USER.id)

  @Before
  override fun setUp() {
    super.setUp()
    runWithTestDispatcher {
      // Setup user
      userRepository.saveUser(TEST_USER).await()
      fakeAuthenticationManager.setUser(TEST_USER)

      // Setup survey and LOIs
      fakeRemoteDataStore.surveys = listOf(TEST_SURVEY)
      fakeRemoteDataStore.lois = TEST_LOCATIONS_OF_INTEREST
      activateSurvey(TEST_SURVEY.id)
      advanceUntilIdle()
    }
  }

  private fun mockEnqueueSyncWorker() {
    `when`(mockWorkManager.enqueueSyncWorker(anyString())).thenReturn(Completable.complete())
  }

  @Test
  fun testApplyAndEnqueue_createsLocalLoi() {
    // TODO(#1559): Remove once customId and caption are handled consistently.
    val loi =
      LOCATION_OF_INTEREST.copy(
        customId = null,
        caption = null,
        // TODO(#1562): Remove once creation time is preserved in local db.
        lastModified = LOCATION_OF_INTEREST.created
      )
    mockEnqueueSyncWorker()
    locationOfInterestRepository
      .applyAndEnqueue(loi.toMutation(CREATE, TEST_USER.id))
      .test()
      .assertNoErrors()
      .assertComplete()

    locationOfInterestRepository
      .getOfflineLocationOfInterest(TEST_SURVEY.id, loi.id)
      .test()
      .assertNoErrors()
      .assertValue(loi)
  }

  @Test
  fun testApplyAndEnqueue_enqueuesLoiMutation() {
    mockEnqueueSyncWorker()

    locationOfInterestRepository.applyAndEnqueue(mutation).test().assertNoErrors().assertComplete()

    locationOfInterestRepository
      .getIncompleteLocationOfInterestMutationsOnceAndStream(LOCATION_OF_INTEREST.id)
      .test()
      .assertNoErrors()
      .assertValue(listOf(mutation.copy(id = 1)))
  }

  @Test
  fun testApplyAndEnqueue_enqueuesWorker() {
    mockEnqueueSyncWorker()

    locationOfInterestRepository.applyAndEnqueue(mutation).test().assertNoErrors().assertComplete()

    verify(mockWorkManager).enqueueSyncWorker(LOCATION_OF_INTEREST.id)
  }

  @Test
  fun testApplyAndEnqueue_returnsErrorOnWorkerSyncFailure() {
    `when`(mockWorkManager.enqueueSyncWorker(anyString())).thenReturn(Completable.error(Error()))

    locationOfInterestRepository
      .applyAndEnqueue(LOCATION_OF_INTEREST.toMutation(CREATE, TEST_USER.id))
      .test()
      .assertError(Error::class.java)
      .assertNotComplete()

    verify(mockWorkManager, times(1)).enqueueSyncWorker(LOCATION_OF_INTEREST.id)
  }

  // TODO(#1373): Add tests for new LOI sync once implemented (create, update, delete, error).

  // TODO(#1373): Add tests for getLocationsOfInterest once new LOI sync implemented.

  @Test
  fun testLoiWithinBounds_whenBoundsNotAvailable_returnsNothing() = runTest {
    locationOfInterestRepository
      .getWithinBoundsOnceAndStream(TEST_SURVEY, Flowable.empty())
      .test()
      .assertNoValues()
  }

  @Test
  fun testLoiWithinBounds_whenOutOfBounds_returnsEmptyList() {
    val southwest = Coordinate(-60.0, -60.0)
    val northeast = Coordinate(-50.0, -50.0)

    locationOfInterestRepository
      .getWithinBoundsOnceAndStream(TEST_SURVEY, Flowable.just(Bounds(southwest, northeast)))
      .test()
      .assertValues(listOf())
  }

  @Test
  fun testLoiWithinBounds_whenSomeLOIsInsideBounds_returnsPartialList() {
    val southwest = Coordinate(-20.0, -20.0)
    val northeast = Coordinate(-10.0, -10.0)

    locationOfInterestRepository
      .getWithinBoundsOnceAndStream(TEST_SURVEY, Flowable.just(Bounds(southwest, northeast)))
      .test()
      .assertValues(listOf(TEST_POINT_OF_INTEREST_1, TEST_AREA_OF_INTEREST_1))
  }

  @Test
  fun testLoiWithinBounds_whenAllLOIsInsideBounds_returnsCompleteList() {
    val southwest = Coordinate(-20.0, -20.0)
    val northeast = Coordinate(20.0, 20.0)

    locationOfInterestRepository
      .getWithinBoundsOnceAndStream(TEST_SURVEY, Flowable.just(Bounds(southwest, northeast)))
      .test()
      .assertValues(
        listOf(
          TEST_POINT_OF_INTEREST_1,
          TEST_POINT_OF_INTEREST_2,
          TEST_POINT_OF_INTEREST_3,
          TEST_AREA_OF_INTEREST_1,
          TEST_AREA_OF_INTEREST_2
        )
      )
  }

  companion object {
    private val COORDINATE_1 = Coordinate(-20.0, -20.0)
    private val COORDINATE_2 = Coordinate(0.0, 0.0)
    private val COORDINATE_3 = Coordinate(20.0, 20.0)

    private val AREA_OF_INTEREST = FakeData.AREA_OF_INTEREST
    private val LOCATION_OF_INTEREST = FakeData.LOCATION_OF_INTEREST
    private val TEST_SURVEY = FakeData.SURVEY
    private val TEST_USER = FakeData.USER

    private val TEST_POINT_OF_INTEREST_1 = createPoint("1", COORDINATE_1)
    private val TEST_POINT_OF_INTEREST_2 = createPoint("2", COORDINATE_2)
    private val TEST_POINT_OF_INTEREST_3 = createPoint("3", COORDINATE_3)
    private val TEST_AREA_OF_INTEREST_1 =
      createPolygon("4", listOf(COORDINATE_1, COORDINATE_2, COORDINATE_1))
    private val TEST_AREA_OF_INTEREST_2 =
      createPolygon("5", listOf(COORDINATE_2, COORDINATE_3, COORDINATE_2))

    private val TEST_LOCATIONS_OF_INTEREST =
      listOf(
        TEST_POINT_OF_INTEREST_1,
        TEST_POINT_OF_INTEREST_2,
        TEST_POINT_OF_INTEREST_3,
        TEST_AREA_OF_INTEREST_1,
        TEST_AREA_OF_INTEREST_2
      )

    private fun createPoint(id: String, coordinate: Coordinate) =
      LOCATION_OF_INTEREST.copy(
        id = id,
        geometry = Point(coordinate),
        surveyId = TEST_SURVEY.id,
        caption = null,
        customId = null
      )

    private fun createPolygon(id: String, coordinates: List<Coordinate>) =
      AREA_OF_INTEREST.copy(
        id = id,
        geometry = Polygon(LinearRing(coordinates)),
        surveyId = TEST_SURVEY.id,
        caption = null,
        customId = null
      )
  }
}
