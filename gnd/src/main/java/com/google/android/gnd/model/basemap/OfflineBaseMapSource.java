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

package com.google.android.gnd.model.basemap;

import androidx.annotation.NonNull;
import com.google.auto.value.AutoValue;
import java.net.URL;

/** Represents a possible source for offline base map data. */
@AutoValue
public abstract class OfflineBaseMapSource {

  public enum OfflineBaseMapSourceType {
    MBTILES_FOOTPRINTS,
    TILED_WEB_MAP,
    UNKNOWN
  }

  public static OfflineBaseMapSourceType typeFromExtension(String url) {
    int extPos = url.lastIndexOf('.');

    switch (extPos == -1 ? "" : url.substring(extPos + 1)) {
      case "geojson":
        return OfflineBaseMapSourceType.MBTILES_FOOTPRINTS;
      case "png":
        return OfflineBaseMapSourceType.TILED_WEB_MAP;
      default:
        return OfflineBaseMapSourceType.UNKNOWN;
    }
  }

  @NonNull
  public abstract URL getUrl();

  public abstract OfflineBaseMapSourceType getType();

  public static Builder builder() {
    return new AutoValue_OfflineBaseMapSource.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setUrl(@NonNull URL newUrl);

    public abstract Builder setType(OfflineBaseMapSourceType type);

    public abstract OfflineBaseMapSource build();
  }
}
