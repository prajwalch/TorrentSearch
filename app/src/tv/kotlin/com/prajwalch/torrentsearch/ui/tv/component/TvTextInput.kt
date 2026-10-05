package com.prajwalch.torrentsearch.ui.tv.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusProperties
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.ui.tv.theme.TvMaterial3Bridge
import com.prajwalch.torrentsearch.ui.tv.theme.isTvDarkTheme
import com.prajwalch.torrentsearch.ui.tv.theme.toMobile
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface

/**
 * TV-safe single-line text input.
 *
 * Two non-obvious TV behaviours are handled here, both documented in the
 * Android "Migrate to Compose for TV" guide:
 *
 * 1. **The IME does not attach on its own.** A bare `TextField` (or a
 *    `Surface(onClick = {})` with no focus target) does not bring up the
 *    on-screen keyboard when DPAD_CENTER is pressed. Wrapping the field in a
 *    focusable TV `Surface` whose click handler moves focus into the field is
 *    what causes the IME to attach on Google TV.
 *
 * 2. **BACK would otherwise exit the screen.** While a field is being edited
 *    BACK normally propagates and navigates away, losing the typed query. The
 *    `onPreviewKeyEvent` trap consumes BACK/Escape first so it dismisses the
 *    keyboard and returns focus to the surface instead.
 *
 * Focus is deliberately *not* requested on entry: auto-focusing would pop the
 * keyboard over the home screen the moment the app launches.
 *
 * @param onSubmit invoked on the IME action key.
 */
@Composable
fun TvTextInput(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onSubmit: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Search,
    // When set, DPAD_DOWN from this field jumps straight here instead of resolving
    // geometrically. Needed because geometric resolution drops the remote into the
    // middle of a wide chip row rather than the selected chip.
    downFocusRequester: FocusRequester? = null,
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Derive the field palette from the TV scheme explicitly rather than relying
    // on the m3 LocalColorScheme CompositionLocal crossing between the two
    // MaterialTheme objects - that propagation is not reliable, and without it
    // OutlinedTextField falls back to the m3 default *light* scheme, rendering a
    // white field on a dark TV UI.
    val isDark = isTvDarkTheme()
    val tvScheme = MaterialTheme.colorScheme
    val fieldScheme = remember(tvScheme, isDark) { tvScheme.toMobile(darkTheme = isDark) }

    Surface(
        onClick = { focusRequester.requestFocus() },
        modifier = modifier
            // Same ring as every other focusable; tv-material's own focused border
            // is noticeably thinner and made the field read as a different kind of
            // control from the buttons and cards around it.
            .tvFocusRing(
                shape = MaterialTheme.shapes.medium,
                scale = TvFocusDefaults.FocusedScaleControl,
            )
            .let { m ->
                if (downFocusRequester != null) {
                    m.focusProperties { down = downFocusRequester }
                } else {
                    m
                }
            },
    ) {
        TvMaterial3Bridge {
        val submitFromIme = {
            val query = value.text.trim()
            keyboardController?.hide()
            if (query.isNotEmpty()) onSubmit(query)
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(fontSize = MaterialTheme.typography.titleMedium.fontSize),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = fieldScheme.surface,
                unfocusedContainerColor = fieldScheme.surface,
                disabledContainerColor = fieldScheme.surface,
                focusedTextColor = fieldScheme.onSurface,
                unfocusedTextColor = fieldScheme.onSurface,
                focusedBorderColor = fieldScheme.primary,
                unfocusedBorderColor = fieldScheme.onSurfaceVariant,
                cursorColor = fieldScheme.primary,
                focusedLabelColor = fieldScheme.primary,
                unfocusedLabelColor = fieldScheme.onSurfaceVariant,
                focusedPlaceholderColor = fieldScheme.onSurfaceVariant,
                unfocusedPlaceholderColor = fieldScheme.onSurfaceVariant,
            ),
            label = label?.let { text -> { Text(text) } },
            placeholder = placeholder?.let { text -> { Text(text) } },
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction,
            ),
            trailingIcon = {
                if (value.text.isNotEmpty()) {
                    androidx.tv.material3.Text(
                        text = stringResource(R.string.action_clear),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier
                            .clickable { onValueChange(TextFieldValue("")) }
                            .padding(horizontal = 16.dp),
                    )
                }
            },
            // The remote's search key reports ImeAction.Search, which Compose routes
            // to onSearch - wiring only onDone means the key is silently ignored.
            // Both are handled so either IME action submits.
            keyboardActions = KeyboardActions(
                onSearch = { submitFromIme() },
                onDone = { submitFromIme() },
            ),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onPreviewKeyEvent { event ->
                    if (
                        event.type == KeyEventType.KeyDown &&
                        (event.key == Key.Back || event.key == Key.Escape)
                    ) {
                        keyboardController?.hide()
                        // Drop focus from the field entirely. Leaving it focused traps
                        // the D-pad: a Compose text field consumes DPAD_UP/DPAD_DOWN for
                        // cursor movement, so after the first BACK the remote stops
                        // navigating the screen. Clearing focus lets Compose restore it
                        // to the wrapping Surface.
                        focusManager.clearFocus(force = true)
                        true
                    } else {
                        false
                    }
                }
                .onFocusChanged { /* traps BACK above; no visual state needed */ },
        )
        }
    }
}