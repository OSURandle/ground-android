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
package com.google.android.ground

import android.os.Bundle
import com.google.android.ground.databinding.SettingsActivityBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint(AbstractActivity::class)
class SettingsActivity : Hilt_SettingsActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val binding = SettingsActivityBinding.inflate(layoutInflater)
    setContentView(binding.root)

    setActionBar(binding.settingsToolbar, true)
  }
}
