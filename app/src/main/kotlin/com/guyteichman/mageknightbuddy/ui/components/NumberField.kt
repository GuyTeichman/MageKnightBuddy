package com.guyteichman.mageknightbuddy.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType

/**
 * A single-line numeric input field, backed by a `String` (the ViewModel stores everything as
 * strings) so it can stay blank/partial while typing rather than forcing a valid Int at every
 * keystroke.
 *
 * @param modifier normally left at the default full width; the Greatest Leader page passes a
 *   `Modifier.weight(...)` to put two of these side by side in a Row.
 * @param imeAction which action key the soft keyboard shows: [ImeAction.Next] (moves focus to the
 *   next field on the same page) for every field but the last one on a page, [ImeAction.Done]
 *   (dismisses the keyboard) for the last. Deliberately never advances the wizard to the next
 *   *page* - see issue #173.
 */
@Composable
internal fun NumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    imeAction: ImeAction = ImeAction.Done,
) {
    // The focus manager moves/clears focus for the whole composition; `LocalFocusManager.current`
    // reads it out of Compose's CompositionLocal (implicit ambient context) mechanism instead of
    // it having to be passed down through every call site.
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = value,
        // `Char::isDigit` is a function reference (a callable pointer to that method), used here
        // as shorthand for `{ c -> c.isDigit() }`: rejects the edit entirely if any character
        // typed isn't a digit, so the field can never hold non-numeric text.
        onValueChange = { new -> if (new.all(Char::isDigit)) onValueChange(new) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = imeAction),
        // What tapping that action key actually does. Only the handler matching `imeAction` above
        // ever fires: FocusDirection.Next walks focus in layout order to the following focusable
        // field; clearFocus() drops focus entirely, which dismisses the keyboard.
        keyboardActions = KeyboardActions(
            onNext = { focusManager.moveFocus(FocusDirection.Next) },
            onDone = { focusManager.clearFocus() },
        ),
        singleLine = true,
        modifier = modifier,
    )
}
