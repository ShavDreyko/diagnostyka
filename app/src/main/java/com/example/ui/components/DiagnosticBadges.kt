package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.AcoposError
import com.example.model.ErrorCategory
import com.example.model.ErrorSeverity
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalDiagnosticColors

@Composable
fun severityColors(severity: ErrorSeverity): Pair<Color, Color> {
    val sem = LocalDiagnosticColors.current
    return when (severity) {
        ErrorSeverity.CRITICAL -> sem.criticalFg to sem.criticalBg
        ErrorSeverity.ERROR -> sem.errorFg to sem.errorBg
        ErrorSeverity.WARNING -> sem.warningFg to sem.warningBg
        ErrorSeverity.INFO -> sem.infoFg to sem.infoBg
    }
}

fun severityIcon(severity: ErrorSeverity): ImageVector {
    return when (severity) {
        ErrorSeverity.CRITICAL -> Icons.Filled.Error
        ErrorSeverity.ERROR -> Icons.Filled.Warning
        ErrorSeverity.WARNING -> Icons.Filled.Warning
        ErrorSeverity.INFO -> Icons.Filled.Info
    }
}

fun categoryIcon(category: ErrorCategory): ImageVector {
    return when (category) {
        ErrorCategory.POWER_DC_BUS -> Icons.Filled.Bolt
        ErrorCategory.POWER_STAGE_BRAKE -> Icons.Filled.Build
        ErrorCategory.ENCODER_FEEDBACK -> Icons.Filled.Sensors
        ErrorCategory.THERMAL_OVERLOAD -> Icons.Filled.DeviceThermostat
        ErrorCategory.MOTION_HOMING -> Icons.Filled.OpenWith
        ErrorCategory.SAFETY_SAFEMC -> Icons.Filled.Shield
        ErrorCategory.NETWORK_COMM -> Icons.Filled.Lan
        ErrorCategory.CAM_SYNC -> Icons.Filled.Sync
        ErrorCategory.PLCOPEN_NC -> Icons.Filled.Code
        ErrorCategory.PARAMETERS_MEMORY -> Icons.Filled.Memory
    }
}

@Composable
fun SeverityBadge(
    severity: ErrorSeverity,
    modifier: Modifier = Modifier
) {
    val (fg, bg) = severityColors(severity)
    Surface(
        modifier = modifier,
        color = bg,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, fg.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = severityIcon(severity),
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = severity.labelPl.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = fg,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun AlarmCodePill(
    code: Int,
    severity: ErrorSeverity,
    modifier: Modifier = Modifier
) {
    val (fg, bg) = severityColors(severity)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = code.toString(),
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleSmall,
            color = fg
        )
    }
}

@Composable
fun ErrorListRowCard(
    error: AcoposError,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sem = LocalDiagnosticColors.current
    val (fg, _) = severityColors(error.severity)
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, sem.subtleBorder),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left severity accent bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .size(width = 4.dp, height = 42.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(fg)
            )
            Spacer(modifier = Modifier.width(12.dp))
            AlarmCodePill(
                code = error.code,
                severity = error.severity
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = error.titlePl,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = categoryIcon(error.category),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = error.category.shortTitlePl,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (error.infoDetail != null) {
                        Text(
                            text = "• Info",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}
