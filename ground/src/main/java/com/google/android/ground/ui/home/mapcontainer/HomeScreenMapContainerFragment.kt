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
package com.google.android.ground.ui.home.mapcontainer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import com.google.android.ground.databinding.BasemapLayoutBinding
import com.google.android.ground.databinding.LoiCardsRecyclerViewBinding
import com.google.android.ground.databinding.MenuButtonBinding
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.model.locationofinterest.LocationOfInterestType
import com.google.android.ground.repository.SubmissionRepository
import com.google.android.ground.rx.RxAutoDispose
import com.google.android.ground.ui.common.AbstractMapContainerFragment
import com.google.android.ground.ui.common.BaseMapViewModel
import com.google.android.ground.ui.home.BottomSheetState
import com.google.android.ground.ui.home.HomeScreenFragmentDirections
import com.google.android.ground.ui.home.HomeScreenViewModel
import com.google.android.ground.ui.home.mapcontainer.cards.MapCardAdapter
import com.google.android.ground.ui.home.mapcontainer.cards.MapCardUiData
import com.google.android.ground.ui.map.MapFragment
import dagger.hilt.android.AndroidEntryPoint
import java8.util.Optional
import javax.inject.Inject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import timber.log.Timber

/** Main app view, displaying the map and related controls (center cross-hairs, add button, etc). */
@AndroidEntryPoint(AbstractMapContainerFragment::class)
class HomeScreenMapContainerFragment : Hilt_HomeScreenMapContainerFragment() {

  @Inject lateinit var submissionRepository: SubmissionRepository

  private lateinit var mapContainerViewModel: HomeScreenMapContainerViewModel
  private lateinit var homeScreenViewModel: HomeScreenViewModel
  private lateinit var binding: BasemapLayoutBinding
  private lateinit var adapter: MapCardAdapter

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    mapContainerViewModel = getViewModel(HomeScreenMapContainerViewModel::class.java)
    homeScreenViewModel = getViewModel(HomeScreenViewModel::class.java)
    mapFragment.locationOfInterestInteractions
      .`as`(RxAutoDispose.disposeOnDestroy(this))
      .subscribe { mapContainerViewModel.onFeatureClick(it) }
    mapFragment.tileProviders.`as`(RxAutoDispose.disposeOnDestroy(this)).subscribe {
      mapContainerViewModel.queueTileProvider(it)
    }

    mapContainerViewModel
      .getZoomThresholdCrossed()
      .`as`(RxAutoDispose.autoDisposable(this))
      .subscribe { onZoomThresholdCrossed() }

    adapter = MapCardAdapter(submissionRepository, lifecycleScope)
    adapter.setCollectDataListener { navigateToDataCollectionFragment(it) }

    lifecycleScope.launch {
      mapContainerViewModel.loisWithinMapBoundsAtVisibleZoomLevel
        .asFlow()
        .combine(mapContainerViewModel.suggestLoiJobs) { lois, jobs ->
          val loiCards = lois.map { MapCardUiData.LoiCardUiData(it) }
          val jobCards = jobs.map { MapCardUiData.SuggestLoiCardUiData(it) }

          Pair(loiCards + jobCards, lois.size)
        }
        .collect { (mapCards, loiCount) -> adapter.updateData(mapCards, loiCount - 1) }
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    binding = BasemapLayoutBinding.inflate(inflater, container, false)
    binding.fragment = this
    binding.viewModel = mapContainerViewModel
    binding.lifecycleOwner = this
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setupMenuFab()
    setupBottomLoiCards()
  }

  private fun setupMenuFab() {
    val mapOverlay = binding.overlay
    val menuBinding = MenuButtonBinding.inflate(layoutInflater, mapOverlay, true)
    menuBinding.homeScreenViewModel = homeScreenViewModel
    menuBinding.lifecycleOwner = this
  }

  private fun setupBottomLoiCards() {
    val container = binding.bottomContainer
    val recyclerViewBinding = LoiCardsRecyclerViewBinding.inflate(layoutInflater, container, true)
    val recyclerView = recyclerViewBinding.recyclerView
    recyclerView.adapter = adapter
    recyclerView.addOnScrollListener(
      object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
          super.onScrollStateChanged(recyclerView, newState)
          val layoutManager = recyclerView.layoutManager as LinearLayoutManager
          val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
          val lastVisiblePosition = layoutManager.findLastVisibleItemPosition()
          val firstCompletelyVisiblePosition =
            layoutManager.findFirstCompletelyVisibleItemPosition()
          var midPosition = (firstVisiblePosition + lastVisiblePosition) / 2

          // Focus the last card
          if (firstCompletelyVisiblePosition > midPosition) {
            midPosition = firstCompletelyVisiblePosition
          }

          adapter.focusItemAtIndex(midPosition)
        }
      }
    )

    val helper: SnapHelper = PagerSnapHelper()
    helper.attachToRecyclerView(recyclerView)
  }

  private fun navigateToDataCollectionFragment(cardUiData: MapCardUiData) {
    when (cardUiData) {
      is MapCardUiData.LoiCardUiData ->
        navigator.navigate(
          HomeScreenFragmentDirections.actionHomeScreenFragmentToDataCollectionFragment(
            cardUiData.loi.id,
            cardUiData.loi.job.id
          )
        )
      is MapCardUiData.SuggestLoiCardUiData -> {
        navigator.navigate(
          HomeScreenFragmentDirections.actionHomeScreenFragmentToDataCollectionFragment(
            null,
            cardUiData.job.id
          )
        )
      }
    }
  }

  override fun onMapReady(mapFragment: MapFragment) {
    // Observe events emitted by the ViewModel.
    mapContainerViewModel.mapLocationOfInterestFeatures.observe(this) {
      mapFragment.renderFeatures(it)
    }
    homeScreenViewModel.bottomSheetState.observe(this) { state: BottomSheetState ->
      onBottomSheetStateChange(state, mapFragment)
    }
    mapContainerViewModel.mbtilesFilePaths.observe(this) { mapFragment.addLocalTileOverlays(it) }
    adapter.setLoiCardFocusedListener {
      when (it) {
        is MapCardUiData.LoiCardUiData -> mapFragment.setActiveLocationOfInterest(it.loi.id)
        is MapCardUiData.SuggestLoiCardUiData,
        null -> mapFragment.setActiveLocationOfInterest(null)
      }
    }
  }

  override fun getMapViewModel(): BaseMapViewModel = mapContainerViewModel

  private fun onBottomSheetStateChange(state: BottomSheetState, map: MapFragment) {
    val loi: Optional<LocationOfInterest> = Optional.ofNullable(state.locationOfInterest)
    when (state.visibility) {
      BottomSheetState.Visibility.VISIBLE -> {
        map.disableGestures()
        // TODO(#358): Once polygon drawing is implemented, pan & zoom to polygon when
        // selected. This will involve calculating centroid and possibly zoom level based on
        // vertices.
        loi
          .filter { it.type === LocationOfInterestType.POINT }
          .ifPresent { mapContainerViewModel.panAndZoomCamera(it.geometry.vertices[0]) }
      }
      BottomSheetState.Visibility.HIDDEN -> map.enableGestures()
    }
  }

  private fun onZoomThresholdCrossed() {
    Timber.v("Refresh markers after zoom threshold crossed")
    mapFragment.refresh()
  }

  override fun onDestroy() {
    mapContainerViewModel.closeProviders()
    super.onDestroy()
  }
}
