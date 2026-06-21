package com.andrew264.habits.ui.usage.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.andrew264.habits.R
import com.andrew264.habits.ui.common.color_picker.utils.toColorOrNull
import com.andrew264.habits.ui.common.components.DrawableImage
import com.andrew264.habits.ui.common.components.list.ListItemPosition
import com.andrew264.habits.ui.common.components.list.toShapes
import com.andrew264.habits.ui.common.utils.FormatUtils
import com.andrew264.habits.ui.common.utils.rememberAppIcon
import com.andrew264.habits.ui.theme.Dimens
import com.andrew264.habits.ui.usage.AppDetails

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppListItem(
    appDetails: AppDetails,
    position: ListItemPosition,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val clickAction: () -> Unit = {
        onClick()
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    val colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface)

    val icon = rememberAppIcon(packageName = appDetails.packageName)
    val leadingNode: @Composable () -> Unit = {
        DrawableImage(
            drawable = icon,
            contentDescription = stringResource(R.string.app_list_item_app_icon_content_description, appDetails.friendlyName),
            modifier = Modifier.size(40.dp)
        )
    }

    val contentNode: @Composable () -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = appDetails.friendlyName,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = FormatUtils.formatDuration(appDetails.totalUsageMillis),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    val supportingNode: @Composable () -> Unit = {
        Column {
            Spacer(Modifier.height(Dimens.PaddingExtraSmall))
            LinearProgressIndicator(
                progress = { appDetails.usagePercentage },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = appDetails.color.toColorOrNull() ?: MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }

    if (position == ListItemPosition.SEPARATE) {
        ListItem(
            onClick = clickAction,
            modifier = modifier,
            shapes = position.toShapes(),
            colors = colors,
            leadingContent = leadingNode,
            content = contentNode,
            supportingContent = supportingNode
        )
    } else {
        SegmentedListItem(
            onClick = clickAction,
            modifier = modifier,
            shapes = position.toShapes(),
            colors = colors,
            leadingContent = leadingNode,
            content = contentNode,
            supportingContent = supportingNode
        )
    }
}
