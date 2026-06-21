package com.andrew264.habits.ui.common.components.list

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.andrew264.habits.ui.theme.Dimens

@Composable
fun ListSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = Dimens.PaddingSmall,
                top = Dimens.PaddingMedium,
                bottom = Dimens.PaddingMedium
            )
    )
}

@Preview(showBackground = true)
@Composable
private fun SectionHeaderPreview() {
    ListSectionHeader(title = "Section Title")
}