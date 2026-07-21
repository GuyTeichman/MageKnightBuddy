package com.guyteichman.mageknightbuddy.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType

/**
 * A single-line numeric input field, backed by a `String` (the ViewModel stores everything as
 * strings) so it can stay blank/partial while typing rather than forcing a valid Int at every
 * keystroke.
 */
@Composable
internal fun NumberField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        // `Char::isDigit` is a function reference (a callable pointer to that method), used here
        // as shorthand for `{ c -> c.isDigit() }`: rejects the edit entirely if any character
        // typed isn't a digit, so the field can never hold non-numeric text.
        onValueChange = { new -> if (new.all(Char::isDigit)) onValueChange(new) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}
