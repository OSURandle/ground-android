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
package com.google.android.ground.ui.datacollection.tasks.point

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.google.android.ground.R
import com.google.android.ground.model.submission.isNotNullOrEmpty
import com.google.android.ground.model.submission.isNullOrEmpty
import com.google.android.ground.ui.MarkerIconFactory
import com.google.android.ground.ui.datacollection.components.ButtonAction
import com.google.android.ground.ui.datacollection.components.TaskView
import com.google.android.ground.ui.datacollection.components.TaskViewFactory
import com.google.android.ground.ui.datacollection.tasks.AbstractTaskFragment
import com.google.android.ground.ui.map.MapFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint(AbstractTaskFragment::class)
class DropAPinTaskFragment : Hilt_DropAPinTaskFragment<DropAPinTaskViewModel>() {

  @Inject lateinit var markerIconFactory: MarkerIconFactory
  @Inject lateinit var mapFragment: MapFragment

  override fun onCreateTaskView(inflater: LayoutInflater, container: ViewGroup?): TaskView =
    TaskViewFactory.createWithoutHeader(inflater, R.drawable.outline_pin_drop, R.string.drop_a_pin)

  override fun onCreateTaskBody(inflater: LayoutInflater): View {
    val rowLayout = LinearLayout(requireContext()).apply { id = View.generateViewId() }
    parentFragmentManager
      .beginTransaction()
      .add(
        rowLayout.id,
        DropAPinMapFragment.newInstance(viewModel, mapFragment),
        "Drop a pin fragment"
      )
      .commit()
    return rowLayout
  }

  override fun onCreateActionButtons() {
    addButton(ButtonAction.DROP_PIN)
      .setOnClickListener { viewModel.dropPin() }
      .setOnTaskUpdated { button, taskData -> button.showIfTrue(taskData.isNullOrEmpty()) }
    addButton(ButtonAction.CONTINUE)
      .setOnClickListener { dataCollectionViewModel.onContinueClicked() }
      .setOnTaskUpdated { button, taskData -> button.showIfTrue(taskData.isNotNullOrEmpty()) }
      .hide()
    addUndoButton()
    addSkipButton()
  }
}
