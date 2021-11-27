/* Copyright 2021 Braden Farmer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.farmerbb.notepad.ui.routes

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.farmerbb.notepad.R
import com.farmerbb.notepad.android.NotepadViewModel
import com.farmerbb.notepad.models.NoteMetadata
import com.farmerbb.notepad.ui.content.*
import com.farmerbb.notepad.ui.menus.NoteListMenu
import com.farmerbb.notepad.ui.menus.NoteViewEditMenu
import com.farmerbb.notepad.ui.routes.RightPaneState.Edit
import com.farmerbb.notepad.ui.routes.RightPaneState.Empty
import com.farmerbb.notepad.ui.routes.RightPaneState.View
import com.farmerbb.notepad.ui.widgets.*

sealed interface RightPaneState {
  object Empty: RightPaneState
  data class View(val id: Long): RightPaneState
  data class Edit(val id: Long? = null): RightPaneState
}

@Composable fun MultiPane(
  navController: NavController,
  vm: NotepadViewModel = hiltViewModel()
) {
  val state = noteListState(vm)

  MultiPane(
    notes = state.value,
    navController = navController,
    vm = vm
  )
}

@Composable fun MultiPane(
  notes: List<NoteMetadata>,
  navController: NavController? = null,
  vm: NotepadViewModel? = null
) {
  val rightPaneState = remember { mutableStateOf<RightPaneState>(Empty) }

  val showAboutDialog = remember { mutableStateOf(false) }
  AboutDialog(showAboutDialog, vm)

  val showSettingsDialog = remember { mutableStateOf(false) }
  SettingsDialog(showSettingsDialog)

  val title: String
  val actions: @Composable RowScope.() -> Unit
  val content: @Composable BoxScope.() -> Unit

  when(val state = rightPaneState.value) {
    Empty -> {
      title = stringResource(id = R.string.app_name)
      actions = {
        NoteListMenu(
          navController = navController,
          vm = vm,
          showAboutDialog = showAboutDialog,
          showSettingsDialog = showSettingsDialog,
        )
      }
      content = { EmptyDetails() }
    }

    is View -> {
      val viewState = viewState(state.id, vm)

      title = viewState.value.metadata.title
      actions = {
        EditButton(state.id, navController, rightPaneState)
        DeleteButton(state.id, navController, vm, rightPaneState)
        NoteViewEditMenu(viewState.value.contents.text, vm)
      }
      content = {
        ViewNote(
          id = state.id,
          isMultiPane = true,
          state = viewState
        )
      }
    }

    is Edit -> {
      val editState = editState(state.id, vm)
      val textState = textState(editState.value.contents.text)
      val id = editState.value.metadata.metadataId

      title = editState.value.metadata.title
      actions = {
        SaveButton(id, textState.value.text, navController, vm, rightPaneState)
        DeleteButton(id, navController, vm, rightPaneState)
        NoteViewEditMenu(textState.value.text, vm)
      }
      content = {
        EditNote(
          id = id,
          isMultiPane = true,
          state = editState,
          textState = textState
        )
      }
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { AppBarText(title) },
        backgroundColor = colorResource(id = R.color.primary),
        actions = actions
      )
    },
    floatingActionButton = {
      if(rightPaneState.value == Empty) {
        FloatingActionButton(
          onClick = { rightPaneState.value = Edit() },
          backgroundColor = colorResource(id = R.color.primary),
          content = {
            Icon(
              imageVector = Icons.Filled.Add,
              contentDescription = null,
              tint = Color.White
            )
          }
        )
      }
    },
    content = {
      Row {
        Box(modifier = Modifier.weight(1f)) {
          NoteListContent(notes, rightPaneState, navController)
        }

        Divider(
          modifier = Modifier
            .fillMaxHeight()
            .width(1.dp)
        )

        Box(
          modifier = Modifier.weight(2f),
          content = content
        )
      }
    }
  )
}

@Composable fun EmptyDetails() {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .fillMaxHeight(),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Image(
      painter = painterResource(id = R.drawable.notepad_logo),
      contentDescription = null,
      modifier = Modifier
        .height(512.dp)
        .width(512.dp)
        .alpha(0.5f)
    )
  }
}

@Suppress("FunctionName")
fun NavGraphBuilder.MultiPaneRoute(
  navController: NavController
) = composable(route = "MultiPane") {
  MultiPane(
    navController = navController
  )
}

@Preview(device = Devices.PIXEL_C)
@Composable fun MultiPanePreview() = MaterialTheme {
  MultiPane(
    notes = listOf(
      NoteMetadata(title = "Test Note 1"),
      NoteMetadata(title = "Test Note 2")
    )
  )
}

@Preview(device = Devices.PIXEL_C)
@Composable fun MultiPaneEmptyPreview() = MaterialTheme {
  MultiPane(
    notes = emptyList()
  )
}