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
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.tv.material3.Text

import com.prajwalch.torrentsearch.ui.tv.component.TvActionButton
import com.prajwalch.torrentsearch.ui.tv.component.TvOptionDialog
import com.prajwalch.torrentsearch.ui.tv.theme.TvTheme

import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Renders the dialog state and stores a PNG for the PR. UI is driven with the
 * same D-pad key injection as [TvDpadInteractionTest].
 */
@RunWith(AndroidJUnit4::class)
class TvScreenshotCapture {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    private fun Scene(content: @Composable () -> Unit) {
        rule.setContent { TvTheme(darkTheme = true, content = content) }
    }

    @Test
    fun captureSortCriteriaDialog() {
        val showDialog = mutableStateOf(true)
        val pick = mutableStateOf("Date")
        Scene {
            Column {
                Text("Backdrop")
                if (showDialog.value) {
                    TvOptionDialog(
                        title = "Sort criteria",
                        options = listOf(
                            Triple("Name", pick.value == "Name") { pick.value = "Name" },
                            Triple("Seeders", pick.value == "Seeders") { pick.value = "Seeders" },
                            Triple("Peers", pick.value == "Peers") { pick.value = "Peers" },
                            Triple("File size", pick.value == "File size") { pick.value = "File size" },
                            Triple("Date", pick.value == "Date") { pick.value = "Date" },
                        ),
                        onDismiss = { showDialog.value = false },
                    )
                }
            }
        }

        rule.onNodeWithText("Sort criteria").assertExists()
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()

        val dir = context.getExternalFilesDir(null)
        val outFile = java.io.File(dir, "tv-sort-criteria-dialog.png")
        outFile.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        println("SCREENSHOT_SAVED=${outFile.absolutePath}")
        rule.onNodeWithText("Sort criteria").assertExists()

        // Keep the app installed long enough for the host to pull the file before
        // the connected-test task uninstalls it.
        Thread.sleep(20000)
    }
}