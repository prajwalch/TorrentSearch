package com.prajwalch.torrentsearch.ui.tv

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.tv.material3.Text

import androidx.test.ext.junit.runners.AndroidJUnit4

import com.prajwalch.torrentsearch.ui.tv.component.TvActionButton
import com.prajwalch.torrentsearch.ui.tv.component.TvOptionDialog
import com.prajwalch.torrentsearch.ui.tv.component.TvSelectableChip
import com.prajwalch.torrentsearch.ui.tv.theme.TvTheme

import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * D-pad interaction tests for the 10-foot UI primitives.
 *
 * Input is injected directly into the Compose scene (key press simulation), so
 * these exercise the actual focus/click plumbing the app relies on for a TV
 * remote - independent of any device's physical key injection.
 */
@RunWith(AndroidJUnit4::class)
class TvDpadInteractionTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private fun Scene(content: @Composable () -> Unit) {
        rule.setContent { TvTheme(darkTheme = true, content = content) }
    }

    @Test
    fun actionButtonActivatedByDpadCenterAndEnter() {
        val clicks = mutableStateOf(0)
        Scene {
            val fr = remember { FocusRequester() }
            LaunchedEffect(Unit) { fr.requestFocus() }
            Column {
                TvActionButton(
                    onClick = { clicks.value++ },
                    modifier = Modifier.focusRequester(fr),
                ) {
                    Text("Activate")
                }
                Text("count=${clicks.value}")
            }
        }

        val node = rule.onNodeWithText("Activate")
        node.performKeyInput { pressKey(Key.DirectionCenter) }
        rule.onNodeWithText("count=1").assertExists()

        node.performKeyInput { pressKey(Key.Enter) }
        rule.onNodeWithText("count=2").assertExists()
    }

    @Test
    fun selectableChipTogglesSelectionOnDpadActivate() {
        Scene {
            val selected = remember { mutableStateOf(false) }
            val fr = remember { FocusRequester() }
            LaunchedEffect(Unit) { fr.requestFocus() }
            Column {
                TvSelectableChip(
                    label = "Chip",
                    selected = selected.value,
                    onClick = { selected.value = !selected.value },
                    modifier = Modifier.focusRequester(fr),
                )
                Text("selected=${selected.value}")
            }
        }

        val chip = rule.onNodeWithText("Chip")
        chip.performKeyInput { pressKey(Key.DirectionCenter) }
        rule.onNodeWithText("selected=true").assertExists()

        chip.performKeyInput { pressKey(Key.Enter) }
        rule.onNodeWithText("selected=false").assertExists()
    }

    @Test
    fun optionDialogOpensOnDpadAndSelectsOnClick() {
        Scene {
            var showDialog by remember { mutableStateOf(false) }
            val pick = remember { mutableStateOf("") }
            val fr = remember { FocusRequester() }
            LaunchedEffect(Unit) { fr.requestFocus() }
            Column {
                TvActionButton(
                    onClick = { showDialog = true },
                    modifier = Modifier.focusRequester(fr),
                ) {
                    Text("Open")
                }
                Text("pick=${pick.value}")
                if (showDialog) {
                    TvOptionDialog(
                        title = "Sort criteria",
                        options = listOf(
                            Triple("Name", pick.value == "Name") { pick.value = "Name" },
                            Triple("Seeders", pick.value == "Seeders") { pick.value = "Seeders" },
                        ),
                        onDismiss = { showDialog = false },
                    )
                }
            }
        }

        rule.onNodeWithText("Open").performKeyInput { pressKey(Key.DirectionCenter) }
        rule.onNodeWithText("Sort criteria").assertExists()

        rule.onNodeWithText("Seeders").performSemanticsAction(SemanticsActions.OnClick)
        rule.onNodeWithText("pick=Seeders").assertExists()
    }
}