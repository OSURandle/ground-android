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
package com.google.android.ground.ui.editsubmission

import android.graphics.Bitmap

/** Contains the bitmap or path to the photo a user captured or selected. */
data class PhotoResult
@JvmOverloads
constructor(
  val taskId: String,
  val bitmap: Bitmap? = null,
  val path: String? = null,
  var isHandled: Boolean = false
) {
  fun isEmpty(): Boolean = bitmap == null && path == null
}
