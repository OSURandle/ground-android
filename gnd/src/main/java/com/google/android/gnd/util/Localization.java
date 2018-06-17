/*
 * Copyright 2018 Google LLC
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

package com.google.android.gnd.util;

import java.util.Map;
import java8.util.Optional;

public abstract class Localization {
  private Localization() {}

  public static String getLocalizedMessage(Map<String, String> messages) {
    // TODO: i18n.
    return Optional.ofNullable(messages.get("pt"))
        .orElse(Optional.ofNullable(messages.get("en")).orElse(""));
  }
}