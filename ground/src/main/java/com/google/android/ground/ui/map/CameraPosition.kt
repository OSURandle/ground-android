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
package com.google.android.ground.ui.map

import com.google.android.ground.model.geometry.Coordinate

data class CameraPosition(
  val target: Coordinate,
  val zoomLevel: Float? = null,
  val isAllowZoomOut: Boolean = false,
  val bounds: Bounds? = null
) {

  fun serialize(): String =
    arrayOf<Any>(
        target.lat,
        target.lng,
        zoomLevel.toString(),
        isAllowZoomOut,
        bounds?.south.toString(),
        bounds?.west.toString(),
        bounds?.north.toString(),
        bounds?.east.toString(),
      )
      .joinToString { it.toString() }

  companion object {

    fun deserialize(serializedValue: String): CameraPosition? {
      if (serializedValue.isEmpty()) return null
      val parts = serializedValue.split(",")
      val lat = parts[0].trim().toDouble()
      val long = parts[1].trim().toDouble()
      val zoomLevel = parts[2].trim().toFloatOrNull()
      val isAllowZoomOut = parts[3].trim().toBoolean()
      val swLat = parts[4].trim().toDoubleOrNull()
      val swLong = parts[5].trim().toDoubleOrNull()
      val neLat = parts[6].trim().toDoubleOrNull()
      val neLong = parts[7].trim().toDoubleOrNull()

      var bounds: Bounds? = null
      if (swLat != null && swLong != null && neLat != null && neLong != null) {
        bounds = Bounds(Coordinate(swLat, swLong), Coordinate(neLat, neLong))
      }

      return CameraPosition(Coordinate(lat, long), zoomLevel, isAllowZoomOut, bounds)
    }
  }
}
