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
package com.google.android.ground.ui.map

import com.google.android.ground.model.geometry.Geometry

/** Represents an individual feature on a map with a given [Geometry] and [Tag]. */
data class Feature(val tag: Tag, val geometry: Geometry) {
  constructor(
    id: String,
    type: Int,
    flag: Boolean = false,
    geometry: Geometry
  ) : this(Tag(id, type, flag), geometry)

  /**
   * Denotes the kind of entity this map feature represents and contains any additional data it
   * carries.
   */
  data class Tag(
    /** A unique identifier for the model object that this feature represents. */
    val id: String,
    /**
     * A integer that indicates how to interpret this feature as a model object. Interpretations of
     * the value are decided by callers.
     */
    val type: Int,
    /** An arbitrary slot for boolean flag. The interpretation of this field is type-dependent. */
    val flag: Boolean = false
  )
}
