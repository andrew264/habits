package com.andrew264.habits.ui.usage.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.andrew264.habits.ui.common.components.DrawableImage
import com.andrew264.habits.ui.common.utils.rememberAppIcon
import com.andrew264.habits.ui.theme.Dimens
import com.andrew264.habits.ui.usage.AppDetails

@Composable
internal fun HeaderCard(app: AppDetails) {
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.PaddingLarge),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge)
        ) {
            val icon = rememberAppIcon(packageName = app.packageName)
            DrawableImage(drawable = icon, contentDescription = null, modifier = Modifier.size(56.dp))
            Column {
                Text(app.friendlyName, style = MaterialTheme.typography.headlineSmall)
                Text(
                    app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}