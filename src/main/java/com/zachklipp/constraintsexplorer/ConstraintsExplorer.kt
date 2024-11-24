package com.zachklipp.constraintsexplorer

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.Orientation.Horizontal
import androidx.compose.foundation.gestures.Orientation.Vertical
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import kotlin.math.roundToInt

private val ControlsSize = 20.dp
private val ConstraintHandleSize = 5.dp
private val ControlsBackgroundColor = Color.DarkGray
private val ControlsForegroundColor = Color.White

@Composable
public fun ConstraintsExplorer(
  modifier: Modifier = Modifier,
  enabled: Boolean = LocalInspectionMode.current,
  content: @Composable () -> Unit
) {
  // TODO detect when running in test?
  if (enabled) {
    ConstraintsExplorerImpl(modifier, content)
  } else {
    Box(modifier, propagateMinConstraints = true) {
      content()
    }
  }
}

@Composable
private fun ConstraintsExplorerImpl(
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit
) {
  val showControls = detectInteractivePreviewMode()
  val state = remember { ConstraintsExplorerState() }

  ConstraintsControlsLayout(
    modifier = modifier,
    widthControls = {
      Box(
        modifier = Modifier.background(ControlsBackgroundColor),
        propagateMinConstraints = true
      ) {
        if (!showControls) {
          StaticPreviewHeader()
        } else {
          WidthConstraintsControls(
            min = state.minWidth,
            max = state.maxWidth,
            onMinDrag = { state.offsetMinWidth(it.roundToInt()) },
            onMaxDrag = { state.offsetMaxWidth(it.roundToInt()) }
          )
        }
      }
    },
    heightControls = {
      Box(
        modifier = Modifier.background(ControlsBackgroundColor),
        propagateMinConstraints = true
      ) {
        if (showControls) {
          HeightConstraintsControls(
            min = state.minHeight,
            max = state.maxHeight,
            onMinDrag = { state.offsetMinHeight(it.roundToInt()) },
            onMaxDrag = { state.offsetMaxHeight(it.roundToInt()) })
        }
      }
    },
    corner = { Box(Modifier.background(ControlsBackgroundColor)) },
    content = content,
    contentModifier = Modifier.then(state.contentModifier)
  )
}

private class ConstraintsExplorerState {

  private var contentSize: IntSize? by mutableStateOf(null)

  var minWidth by mutableIntStateOf(0)
    private set
  var maxWidth by mutableIntStateOf(Constraints.Infinity)
    private set
  var minHeight by mutableIntStateOf(0)
    private set
  var maxHeight by mutableIntStateOf(Constraints.Infinity)
    private set

  private val widthLimit: Int get() = contentSize?.width ?: Constraints.Infinity
  private val heightLimit: Int get() = contentSize?.height ?: Constraints.Infinity

  val contentModifier: Modifier = Modifier.layout { measurable, constraints ->
    var contentSize = contentSize
    val placeable = if (contentSize == null) {
      val placeable = measurable.measure(constraints)
      contentSize = IntSize(placeable.width, placeable.height)
      this@ConstraintsExplorerState.contentSize = contentSize
      minWidth = constraints.minWidth
      maxWidth = widthLimit
      minHeight = constraints.minHeight
      maxHeight = heightLimit
      placeable
    } else {
      val childConstraints = Constraints(minWidth, maxWidth, minHeight, maxHeight)
      measurable.measure(childConstraints)
    }

    layout(contentSize.width, contentSize.height) {
      placeable.place(0, 0)
    }
  }

  fun offsetMinWidth(delta: Int) {
    minWidth = (minWidth + delta).coerceIn(0, widthLimit)
    maxWidth = maxWidth.coerceAtLeast(minWidth)
  }

  fun offsetMaxWidth(delta: Int) {
    maxWidth = (maxWidth + delta).coerceIn(0, widthLimit)
    minWidth = minWidth.coerceAtMost(maxWidth)
  }

  fun offsetMinHeight(delta: Int) {
    minHeight = (minHeight + delta).coerceIn(0, heightLimit)
    maxHeight = maxHeight.coerceAtLeast(minHeight)
  }

  fun offsetMaxHeight(delta: Int) {
    maxHeight = (maxHeight + delta).coerceIn(0, heightLimit)
    minHeight = minHeight.coerceAtMost(maxHeight)
  }
}

@Composable
private fun detectInteractivePreviewMode(): Boolean = produceState(false) {
  // Hack to figure out if we're in "interactive mode" or not. In interactive mode, frames will be
  // rendered continuously, but in non-interactive mode, only the first two frames are rendered. The
  // initial frame will create this LaunchedEffect, and then the frame callback will run for the
  // second frame, but the coroutine will never resume after the second frame completes. So if we
  // hit the code that sets the flag, we must be in interactive mode.
  withFrameMillis {}
  value = true
}.value

/**
 * Layout [content] with a thin bar on top for [widthControls] and a thin bar on the right for
 * [heightControls].
 */
@Composable
private fun ConstraintsControlsLayout(
  modifier: Modifier,
  widthControls: @Composable () -> Unit,
  heightControls: @Composable () -> Unit,
  corner: @Composable () -> Unit,
  content: @Composable () -> Unit,
  contentModifier: Modifier,
) {
  Layout(
    modifier = modifier,
    content = {
      // Enforce a single Measurable per slot.
      Box(propagateMinConstraints = true) { widthControls() }
      Box(propagateMinConstraints = true) { heightControls() }
      Box(propagateMinConstraints = true) { corner() }
      Box(contentModifier, propagateMinConstraints = true) { content() }
    }
  ) { measureables, constraints ->
    val (
      widthControlsMeasurable,
      heightControlsMeasurable,
      cornerMeasurable,
      contentMeasurable
    ) = measureables

    val controlsSizePx = ControlsSize.roundToPx()
    val contentConstraints = constraints.offset(
      horizontal = -controlsSizePx,
      vertical = -controlsSizePx
    )
    val contentPlaceable = contentMeasurable.measure(contentConstraints)

    val widthControlsConstraints = Constraints.fixed(
      width = contentPlaceable.width,
      height = controlsSizePx
    )
    val heightControlsConstraints = Constraints.fixed(
      width = controlsSizePx,
      height = contentPlaceable.height
    )
    val cornerConstraints = Constraints.fixed(controlsSizePx, controlsSizePx)
    val widthControlsPlaceable = widthControlsMeasurable.measure(widthControlsConstraints)
    val heightControlsPlaceable = heightControlsMeasurable.measure(heightControlsConstraints)
    val cornerPlaceable = cornerMeasurable.measure(cornerConstraints)

    layout(
      width = heightControlsPlaceable.width + contentPlaceable.width,
      height = widthControlsPlaceable.height + contentPlaceable.height
    ) {
      contentPlaceable.place(0, widthControlsPlaceable.height)
      cornerPlaceable.place(contentPlaceable.width, 0)
      widthControlsPlaceable.place(0, 0)
      heightControlsPlaceable.place(contentPlaceable.width, widthControlsPlaceable.height)
    }
  }
}

@Composable
private fun WidthConstraintsControls(
  min: Int,
  max: Int,
  onMinDrag: (Float) -> Unit,
  onMaxDrag: (Float) -> Unit
) {
  Layout(
    content = {
      DisableTouchSlop {
        ConstraintHandle(orientation = Horizontal, min = true, onDrag = onMinDrag)
        ConstraintHandle(orientation = Horizontal, min = false, onDrag = onMaxDrag)
      }
    },
    modifier = Modifier.drawBehind {
      drawLine(
        ControlsForegroundColor,
        strokeWidth = 1.dp.toPx(),
        start = Offset(min.toFloat(), size.height / 2),
        end = Offset(max.toFloat(), size.height / 2f)
      )
      drawRect(
        Color.Black,
        topLeft = Offset.Zero,
        size = Size(min.toFloat(), size.height)
      )
      drawRect(
        Color.Black,
        topLeft = Offset(max.toFloat(), 0f),
        size = Size(size.width - max.toFloat(), size.height)
      )
    }
  ) { measureables, constraints ->
    val (minHandleMeasurable, maxHandleMeasurable) = measureables
    val handleConstraints = Constraints.fixed(
      width = ConstraintHandleSize.roundToPx(),
      height = ControlsSize.roundToPx()
    )
    val minHandlePlaceable = minHandleMeasurable.measure(handleConstraints)
    val maxHandlePlaceable = maxHandleMeasurable.measure(handleConstraints)
    layout(constraints.maxWidth, constraints.maxHeight) {
      minHandlePlaceable.place(min, 0)
      maxHandlePlaceable.place(max - maxHandlePlaceable.width, 0)
    }
  }
}

@Composable
private fun HeightConstraintsControls(
  min: Int,
  max: Int,
  onMinDrag: (Float) -> Unit,
  onMaxDrag: (Float) -> Unit
) {
  Layout(
    content = {
      DisableTouchSlop {
        ConstraintHandle(orientation = Vertical, min = true, onDrag = onMinDrag)
        ConstraintHandle(orientation = Vertical, min = false, onDrag = onMaxDrag)
      }
    },
    modifier = Modifier.drawBehind {
      drawLine(
        ControlsForegroundColor,
        strokeWidth = 1.dp.toPx(),
        start = Offset(size.width / 2, min.toFloat()),
        end = Offset(size.width / 2, max.toFloat())
      )
      drawRect(
        Color.Black,
        topLeft = Offset.Zero,
        size = Size(size.width, min.toFloat())
      )
      drawRect(
        Color.Black,
        topLeft = Offset(0f, max.toFloat()),
        size = Size(size.width, size.height - max.toFloat())
      )
    }
  ) { measureables, constraints ->
    val (minHandleMeasurable, maxHandleMeasurable) = measureables
    val handleConstraints = Constraints.fixed(
      width = ControlsSize.roundToPx(),
      height = ConstraintHandleSize.roundToPx()
    )
    val minHandlePlaceable = minHandleMeasurable.measure(handleConstraints)
    val maxHandlePlaceable = maxHandleMeasurable.measure(handleConstraints)
    layout(constraints.maxWidth, constraints.maxHeight) {
      minHandlePlaceable.place(0, min)
      maxHandlePlaceable.place(0, max - maxHandlePlaceable.height)
    }
  }
}

@Composable
private fun DisableTouchSlop(content: @Composable () -> Unit) {
  val originalConfig = LocalViewConfiguration.current
  val config = remember(originalConfig) {
    object : ViewConfiguration by originalConfig {
      override val touchSlop: Float get() = 0f
    }
  }
  CompositionLocalProvider(LocalViewConfiguration provides config, content = content)
}

@Composable
private fun ConstraintHandle(
  orientation: Orientation,
  min: Boolean,
  onDrag: (Float) -> Unit
) {
  val state = remember { DraggableState(onDrag) }
  Box(
    Modifier
      .draggable(state, orientation = orientation)
      .drawWithCache {
        val path = Path().apply {
          moveTo(0f, 0f)
          when (orientation) {
            Horizontal -> {
              lineTo(size.width, size.height / 2)
              lineTo(0f, size.height)
            }

            Vertical -> {
              lineTo(size.width / 2, size.height)
              lineTo(size.width, 0f)
            }
          }
          close()
        }
        onDrawBehind {
          withTransform({
            if (!min) rotate(180f)
          }) {
            drawPath(path, ControlsForegroundColor)
          }
        }
      }
  )
}

@Composable
private fun StaticPreviewHeader() {
  BasicText(
    "Constraints explorer available",
    modifier = Modifier
      .fillMaxWidth()
      .wrapContentWidth(),
    style = TextStyle(
      color = ControlsForegroundColor,
      textAlign = TextAlign.Center
    ),
    overflow = TextOverflow.Ellipsis,
  )
}

@Preview(widthDp = 750, heightDp = 750)
@Composable
private fun PreviewPreview() {
  val size = 500.dp
  ConstraintsExplorer(Modifier.wrapContentSize()) {
    BasicText(
      "Preferred size: $size",
      modifier = Modifier
        .size(size)
        .background(Color.LightGray)
        .wrapContentSize(),
    )
  }
}
