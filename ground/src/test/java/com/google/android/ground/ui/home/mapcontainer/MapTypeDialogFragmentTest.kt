/*
 * Copyright 2023 Google LLC
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
package com.google.android.ground.ui.home.mapcontainer

import androidx.core.os.bundleOf
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import com.google.android.gms.maps.GoogleMap
import com.google.android.ground.BaseHiltTest
import com.google.android.ground.R
import com.google.android.ground.launchFragmentInHiltContainer
import com.google.android.ground.repository.MapStateRepository
import com.google.android.ground.shouldHaveTextAtPosition
import com.google.android.ground.ui.map.gms.GoogleMapsFragment
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.hamcrest.Matchers.allOf
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class MapTypeDialogFragmentTest : BaseHiltTest() {

  @Inject lateinit var mapStateRepository: MapStateRepository

  private lateinit var fragment: MapTypeDialogFragment

  @Before
  override fun setUp() {
    super.setUp()
    setupFragment()
  }

  @Test
  fun test_render() {
    assertThat(fragment.isVisible).isTrue()

    onView(withText("Map Type")).check(matches(isDisplayed()))
    onView(withId(R.id.recycler_view)).check(matches(allOf(isDisplayed(), hasChildCount(3))))
    with(R.id.recycler_view) {
      shouldHaveTextAtPosition("Road Map", 0)
      shouldHaveTextAtPosition("Terrain", 1)
      shouldHaveTextAtPosition("Satellite", 2)
    }
  }

  @Test
  fun test_close() {
    onView(withId(R.id.dialog_close_btn)).perform(click())

    assertThat(fragment.isVisible).isFalse()
  }

  @Test
  fun test_defaultMapType() {
    assertThat(mapStateRepository.mapType).isEqualTo(GoogleMap.MAP_TYPE_HYBRID)
  }

  @Test
  fun test_changeMapType() {
    onView(withText("Terrain")).perform(click())

    assertThat(mapStateRepository.mapType).isEqualTo(GoogleMap.MAP_TYPE_TERRAIN)
  }

  private fun setupFragment() {
    launchFragmentInHiltContainer<MapTypeDialogFragment>(
      bundleOf(Pair("mapTypes", GoogleMapsFragment.MAP_TYPES))
    ) {
      fragment = this as MapTypeDialogFragment
    }
  }
}
