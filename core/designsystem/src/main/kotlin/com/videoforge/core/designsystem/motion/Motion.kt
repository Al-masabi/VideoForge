package com.videoforge.core.designsystem.motion

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

object VfMotion {

    val PressSpring = spring<Float>(
        dampingRatio = 0.78f,
        stiffness = Spring.StiffnessMedium
    )

    val SettleSpring = spring<Float>(
        dampingRatio = 0.86f,
        stiffness = Spring.StiffnessMediumLow
    )

    const val StaggerStepMs = 45

    fun staggeredEnter(index: Int): EnterTransition {
        val delay = index * StaggerStepMs

        return fadeIn(animationSpec = tween(durationMillis = 280, delayMillis = delay)) +
            slideInVertically(
                animationSpec = tween(durationMillis = 340, delayMillis = delay),
                initialOffsetY = { fullHeight -> fullHeight / 14 }
            )
    }
}

@Composable
fun rememberPressScale(interactionSource: MutableInteractionSource): Float {
    val pressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.965f else 1f,
        animationSpec = VfMotion.PressSpring,
        label = "press_scale"
    )

    return scale
}

@Composable
fun VfReveal(
    index: Int = 0,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = VfMotion.staggeredEnter(index.coerceAtMost(8))
    ) {
        content()
    }
}