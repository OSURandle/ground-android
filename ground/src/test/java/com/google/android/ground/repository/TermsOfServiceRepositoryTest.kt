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

import com.google.android.ground.BaseHiltTest
import com.google.common.truth.Truth.assertThat
import com.sharedtest.FakeData
import com.sharedtest.persistence.remote.FakeRemoteDataStore
import dagger.hilt.android.testing.HiltAndroidTest
import io.reactivex.Maybe
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class TermsOfServiceRepositoryTest : BaseHiltTest() {
  @Inject lateinit var fakeRemoteDataStore: FakeRemoteDataStore

  @Inject lateinit var termsOfServiceRepository: TermsOfServiceRepository

  @Test
  fun testGetTermsOfService() = runBlocking {
    fakeRemoteDataStore.termsOfService = Maybe.just(FakeData.TERMS_OF_SERVICE)
    assertThat(termsOfServiceRepository.getTermsOfService()).isEqualTo(FakeData.TERMS_OF_SERVICE)
  }

  @Test
  fun testGetTermsOfService_whenMissing_doesNotThrowException() = runBlocking {
    fakeRemoteDataStore.termsOfService = Maybe.empty()
    assertThat(termsOfServiceRepository.getTermsOfService()).isNull()
  }

  @Test
  fun testGetTermsOfService_whenErrorFetchingTos_doesNotThrowException() = runBlocking {
    fakeRemoteDataStore.termsOfService = Maybe.error(Error("some error"))
    assertThat(termsOfServiceRepository.getTermsOfService()).isNull()
  }

  @Test
  fun testTermsOfServiceAccepted() {
    termsOfServiceRepository.isTermsOfServiceAccepted = true
    assertThat(termsOfServiceRepository.isTermsOfServiceAccepted).isTrue()
  }

  @Test
  fun testTermsOfServiceNotAccepted() {
    assertThat(termsOfServiceRepository.isTermsOfServiceAccepted).isFalse()
  }
}
