package com.geeksville.mesh.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.KeyboardArrowDown
import androidx.compose.material.icons.twotone.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.geeksville.mesh.R
import com.google.protobuf.ProtocolMessageEnum

@Composable
fun <T : Enum<T>> DropDownPreference(
    title: String,
    enabled: Boolean,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
) {
    DropDownPreference(
        title = title,
        enabled = enabled,
        items = selectedItem.declaringJavaClass.enumConstants
            ?.filter { it.name != "UNRECOGNIZED" }?.map { it to it.name } ?: emptyList(),
        selectedItem = selectedItem,
        onItemSelected = onItemSelected,
        modifier = modifier,
        summary = summary,
    )
}

@Composable
fun <T> DropDownPreference(
    title: String,
    enabled: Boolean,
    items: List<Pair<T, String>>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
) {
    var dropDownExpanded by remember { mutableStateOf(value = false) }

    val deprecatedItems: List<T> = remember {
        if (selectedItem is ProtocolMessageEnum) {
            val enum = (selectedItem as? Enum<*>)?.declaringJavaClass?.enumConstants
            val descriptor = (selectedItem as ProtocolMessageEnum).descriptorForType

            @Suppress("UNCHECKED_CAST")
            enum?.filter { entries ->
                descriptor.values.any { it.name == entries.name && it.options.deprecated }
            } as? List<T> ?: emptyList() // Safe cast to List<T> or return emptyList if cast fails
        } else {
            emptyList()
        }
    }

    RegularPreference(
        title = title,
        subtitle = items.find { it.first == selectedItem }?.second
            ?: stringResource(id = R.string.unrecognized),
        onClick = {
            dropDownExpanded = true
        },
        enabled = enabled,
        trailingIcon = if (dropDownExpanded) {
            Icons.TwoTone.KeyboardArrowUp
        } else {
            Icons.TwoTone.KeyboardArrowDown
        },
        summary = summary,
    )

    Box {
        DropdownMenu(
            expanded = dropDownExpanded,
            onDismissRequest = { dropDownExpanded = !dropDownExpanded },
        ) {
            items.filterNot { it.first in deprecatedItems }.forEach { item ->
                DropdownMenuItem(
                    onClick = {
                        dropDownExpanded = false
                        onItemSelected(item.first)
                    },
                    modifier = modifier
                        .background(
                            color = if (selectedItem == item.first) {
                                MaterialTheme.colors.primary.copy(alpha = 0.3f)
                            } else {
                                Color.Unspecified
                            },
                        ),
                    content = {
                        Text(
                            text = item.second,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                        )
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DropDownPreferencePreview() {
    DropDownPreference(
        title = "Settings",
        summary = "Lorem ipsum dolor sit amet",
        enabled = true,
        items = listOf("TEST1" to "text1", "TEST2" to "text2"),
        selectedItem = "TEST2",
        onItemSelected = {}
    )
}
