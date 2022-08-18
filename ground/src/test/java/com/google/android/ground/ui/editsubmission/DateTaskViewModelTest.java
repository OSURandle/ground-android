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

package com.google.android.ground.ui.editsubmission;

import static com.google.android.ground.test.TestObservers.observeUntilFirstChange;
import static com.google.common.truth.Truth.assertThat;

import com.google.android.ground.BaseHiltTest;
import com.google.android.ground.model.submission.DateResponse;
import com.google.android.ground.rx.Nil;
import dagger.hilt.android.testing.HiltAndroidTest;
import io.reactivex.observers.TestObserver;
import java.util.Date;
import javax.inject.Inject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@HiltAndroidTest
@RunWith(RobolectricTestRunner.class)
public class DateTaskViewModelTest extends BaseHiltTest {

  // Date represented in milliseconds for date: 2021-09-24T16:40+0000.
  private static final Date TEST_DATE = new Date(1632501600000L);

  @Inject DateTaskViewModel dateTaskViewModel;

  @Test
  public void testUpdateResponse() {
    dateTaskViewModel.updateResponse(TEST_DATE);

    observeUntilFirstChange(dateTaskViewModel.getResponse());
    assertThat(dateTaskViewModel.getResponse().getValue())
        .isEqualTo(DateResponse.fromDate(TEST_DATE));
  }

  @Test
  public void testDialogClick() {
    TestObserver<Nil> testObserver = dateTaskViewModel.getShowDialogClicks().test();

    dateTaskViewModel.onShowDialogClick();

    testObserver.assertNoErrors().assertNotComplete().assertValue(Nil.NIL);
  }
}