/*
 * Copyright 2021 Google LLC
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

import com.google.android.ground.persistence.local.LocalValueStore
import com.google.android.ground.ui.map.CameraPosition
import io.reactivex.Flowable
import javax.inject.Inject
import javax.inject.Singleton

/** Provides access and storage of persistent map states. */
@Singleton
class MapStateRepository @Inject constructor(private val localValueStore: LocalValueStore) {

  val mapTypeFlowable: Flowable<Int> by localValueStore::mapTypeFlowable
  var mapType: Int by localValueStore::mapType
  var isLocationLockEnabled: Boolean by localValueStore::isLocationLockEnabled

  fun setCameraPosition(cameraPosition: CameraPosition) =
    localValueStore.setLastCameraPosition(localValueStore.lastActiveSurveyId, cameraPosition)

  fun getCameraPosition(surveyId: String): CameraPosition? =
    localValueStore.getLastCameraPosition(surveyId)
}
