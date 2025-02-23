/*
 * Copyright 2022 Google LLC
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
package com.google.android.ground.ui.map

import com.google.android.ground.model.geometry.Coordinate

/**
 * Represents a rectangular bound on a map. A bounds may be constructed using only southwest and
 * northeast coordinates.
 */
data class Bounds(val southwest: Coordinate, val northeast: Coordinate) {
  val north = northeast.lat
  val east = northeast.lng
  val south = southwest.lat
  val west = southwest.lng
}
