package cdglacier.mytool.ui.screen.home

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cdglacier.mytool.ui.theme.GruvboxBg
import cdglacier.mytool.ui.theme.GruvboxGreen
import cdglacier.mytool.ui.theme.GruvboxMuted
import cdglacier.mytool.ui.theme.GruvboxOnPrimary
import cdglacier.mytool.ui.theme.GruvboxOnSurface
import cdglacier.mytool.ui.theme.GruvboxRed
import cdglacier.mytool.ui.theme.GruvboxSurface
import cdglacier.mytool.ui.theme.GruvboxSurfaceLow
import cdglacier.mytool.ui.theme.GruvboxYellow
import cdglacier.mytool.ui.theme.SpaceGroteskFamily

@Composable
fun HomeScreen(
    onNavigateToCopyObsidianJournal: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    Scaffold(
        topBar = { TerminalTopBar() },
        containerColor = GruvboxBg,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            SystemTelemetryCard()
            Spacer(modifier = Modifier.height(32.dp))
            ExecCommandsSection(
                onNavigateToCopyObsidianJournal = onNavigateToCopyObsidianJournal,
                onNavigateToSettings = onNavigateToSettings,
            )
        }
    }
}

@Composable
private fun TerminalTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GruvboxBg)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(32.dp)
                .height(32.dp)
                .background(GruvboxRed),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = ">_",
                color = GruvboxOnPrimary,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "SYSTEM_OPERATOR_V1.0",
            color = GruvboxRed,
            fontFamily = SpaceGroteskFamily,
            fontWeight = FontWeight.Black,
            fontSize = 18.sp,
            letterSpacing = (-0.5).sp,
        )
    }
}

@Composable
private fun SystemTelemetryCard() {
    val yellowBorderWidth = 4.dp
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(GruvboxSurfaceLow)
            .drawBehind {
                drawRect(
                    color = GruvboxYellow,
                    topLeft = Offset.Zero,
                    size = Size(width = yellowBorderWidth.toPx(), height = size.height),
                )
            }
            .padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "SYSTEM_TELEMETRY",
                color = GruvboxMuted,
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 2.sp,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "ACTIVE_STABLE",
                color = GruvboxYellow,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        TelemetryRow(label = "CPU_USAGE:", value = "0.42%", valueColor = GruvboxYellow)
        Spacer(modifier = Modifier.height(4.dp))
        TelemetryRow(label = "UPTIME:", value = "742:12:09", valueColor = GruvboxGreen)
        Spacer(modifier = Modifier.height(4.dp))
        TelemetryRow(label = "MEM_ALLOC:", value = "12.4GB / 64GB", valueColor = GruvboxGreen)

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(GruvboxSurface)
        )

        Spacer(modifier = Modifier.height(12.dp))

        TerminalCursorText()
    }
}

@Composable
private fun TelemetryRow(label: String, value: String, valueColor: Color) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = GruvboxMuted,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            color = valueColor,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun TerminalCursorText() {
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "cursorAlpha",
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "guest@localhost:~ $ ",
            color = GruvboxGreen,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
        )
        Text(
            text = "█",
            color = GruvboxGreen.copy(alpha = alpha),
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun ExecCommandsSection(
    onNavigateToCopyObsidianJournal: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 16.dp),
    ) {
        Box(
            modifier = Modifier
                .width(32.dp)
                .height(1.dp)
                .background(GruvboxSurface)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "EXEC_COMMANDS",
            color = GruvboxMuted,
            fontFamily = SpaceGroteskFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 2.sp,
        )
    }

    CommandMenuItem(
        number = "01.",
        label = "COPY_JOURNAL",
        onClick = onNavigateToCopyObsidianJournal,
    )
    Spacer(modifier = Modifier.height(2.dp))
    CommandMenuItem(
        number = "02.",
        label = "SYS_SETTINGS",
        onClick = onNavigateToSettings,
    )
}

@Composable
private fun CommandMenuItem(
    number: String,
    label: String,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isPressed) GruvboxYellow else GruvboxSurface)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(56.dp)
                .align(Alignment.CenterStart)
                .background(GruvboxYellow.copy(alpha = if (isPressed) 1f else 0f))
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = number,
                color = if (isPressed) GruvboxOnPrimary else GruvboxYellow,
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label,
                color = if (isPressed) GruvboxOnPrimary else GruvboxOnSurface,
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                letterSpacing = 1.sp,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = ">",
                color = if (isPressed) GruvboxOnPrimary else GruvboxYellow,
                fontFamily = FontFamily.Monospace,
                fontSize = 16.sp,
            )
        }
    }
}
