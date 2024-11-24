package com.zachklipp.constraintsexplorer

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.Orientation.Horizontal
import androidx.compose.foundation.gestures.Orientation.Vertical
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
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
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
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
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

internal const val PreviewHeaderText = "Constraints explorer available"
internal val ControlsSize = 20.dp
private val ConstraintHandleSize = 5.dp
private val ControlsFontSize = 10.sp
private val UnboundedConstraintsViewportPadding = 50.dp
private val ControlsBackgroundColor = Color.DarkGray
private val ControlsForegroundColor = Color.White

/**
 * Provides a set of interactive controls to manipulate the constraints passed to [content].
 *
 * By default, this component will no-op when not running in a preview so it won't accidentally
 * show controls in production. You can override this behavior by passing the [enabled] flag.
 *
 * The preview will show a controls gutter on the top and right of the preview, and a label
 * indicating the explorer is available. To access the explorer, enter “Interactive mode” using the
 * the button over the top-right of the preview frame. Once in interactive mode, the controls will
 * be shown in the gutters: drag the left/top arrows to adjust minimum width/height constraints, and
 * the bottom/right arrows to adjust maximum constraints.
 *
 * In interactive mode, the preview will grow to fill the maximum incoming constraints. If the
 * constraints are unbounded, the preview will grow by a small amount to indicate that the content
 * is not filling maximum constraints, and the max constraints arrows will be drawn as outlines.
 * When a constraint is unbounded, dragging the maximum constraint handle past the current
 * viewport's bounds will grow the viewport.
 *
 * To learn more about constraints and layout, see the
 * [Developer Android docs](https://developer.android.com/develop/ui/compose/layouts/basics).
 */
@Composable
public fun ConstraintsExplorer(
  modifier: Modifier = Modifier,
  enabled: Boolean = LocalInspectionMode.current,
  content: @Composable () -> Unit
) {
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
  val showControls by detectInteractivePreviewMode()
  val state = remember { ConstraintsExplorerState() }
  state.showControls = showControls

  ConstraintsControlsLayout(
    modifier = modifier.drawBehind {
      // Draw the controls background only behind the controls: clip out the content area.
      clipRect(
        right = size.width - ControlsSize.toPx(),
        top = ControlsSize.toPx(),
        clipOp = ClipOp.Difference
      ) {
        drawRect(ControlsBackgroundColor)
      }
    },
    widthControls = {
      if (!showControls) {
        StaticPreviewHeader()
      } else {
        WidthConstraintsControls(
          min = state.minWidth,
          max = state.maxWidth,
          fillMax = state.incomingConstraints.hasBoundedWidth,
          onMinDrag = { state.offsetMinWidth(it.roundToInt()) },
          onMaxDrag = { state.offsetMaxWidth(it.roundToInt()) }
        )
      }
    },
    heightControls = {
      if (showControls) {
        HeightConstraintsControls(
          min = state.minHeight,
          max = state.maxHeight,
          fillMax = state.incomingConstraints.hasBoundedHeight,
          onMinDrag = { state.offsetMinHeight(it.roundToInt()) },
          onMaxDrag = { state.offsetMaxHeight(it.roundToInt()) })
      }
    },
    content = content,
    contentModifier = Modifier.then(state.contentModifier)
  )
}

private class ConstraintsExplorerState {

  var incomingConstraints: Constraints by mutableStateOf(Constraints())
    private set
  private var viewportSize: IntSize by mutableStateOf(IntSize.Zero)

  /**
   * Whether the controls are being shown. When false, [contentModifier] will always measure itself
   * to be the size of the content. When true, it will fill max size.
   */
  var showControls: Boolean by mutableStateOf(false)

  var minWidth by mutableIntStateOf(0)
    private set
  var maxWidth by mutableIntStateOf(Constraints.Infinity)
    private set
  var minHeight by mutableIntStateOf(0)
    private set
  var maxHeight by mutableIntStateOf(Constraints.Infinity)
    private set

  val contentModifier: Modifier = Modifier.layout { measurable, constraints ->
    val placeable = if (viewportSize == IntSize.Zero) {
      incomingConstraints = constraints
      val placeable = measurable.measure(constraints)

      val viewportWidth = if (constraints.hasBoundedWidth) {
        constraints.maxWidth
      } else {
        placeable.width + UnboundedConstraintsViewportPadding.roundToPx()
      }
      val viewportHeight = if (constraints.hasBoundedHeight) {
        constraints.maxHeight
      } else {
        placeable.height + UnboundedConstraintsViewportPadding.roundToPx()
      }

      minWidth = constraints.minWidth
      maxWidth = viewportWidth
      minHeight = constraints.minHeight
      maxHeight = viewportHeight
      viewportSize = IntSize(viewportWidth, viewportHeight)
      this@ConstraintsExplorerState.viewportSize = viewportSize

      placeable
    } else {
      val childConstraints = Constraints(minWidth, maxWidth, minHeight, maxHeight)
      measurable.measure(childConstraints)
    }

    val width = if (showControls) {
      viewportSize.width
    } else {
      placeable.width
    }
    val height = if (showControls) {
      viewportSize.height
    } else {
      placeable.height
    }

    layout(width, height) {
      placeable.place(0, 0)
    }
  }

  fun offsetMinWidth(delta: Int) {
    minWidth = (minWidth + delta).coerceIn(0, incomingConstraints.maxWidth)
    maxWidth = maxWidth.coerceAtLeast(minWidth)
    updateViewport()
  }

  fun offsetMaxWidth(delta: Int) {
    maxWidth = (maxWidth + delta).coerceIn(0, incomingConstraints.maxWidth)
    minWidth = minWidth.coerceAtMost(maxWidth)
    updateViewport()
  }

  fun offsetMinHeight(delta: Int) {
    minHeight = (minHeight + delta).coerceIn(0, incomingConstraints.maxHeight)
    maxHeight = maxHeight.coerceAtLeast(minHeight)
    updateViewport()
  }

  fun offsetMaxHeight(delta: Int) {
    maxHeight = (maxHeight + delta).coerceIn(0, incomingConstraints.maxHeight)
    minHeight = minHeight.coerceAtMost(maxHeight)
    updateViewport()
  }

  private fun updateViewport() {
    viewportSize = IntSize(
      width = viewportSize.width.coerceAtLeast(maxWidth).coerceAtMost(incomingConstraints.maxWidth),
      height = viewportSize.height.coerceAtLeast(maxHeight)
        .coerceAtMost(incomingConstraints.maxHeight)
    )
  }
}

/**
 * Hack to figure out if we're in "interactive mode" or not. In interactive mode, frames will be
 * rendered continuously, but in non-interactive mode, only the first two frames are rendered. The
 * initial frame will create this LaunchedEffect, and then the frame callback will run for the
 * second frame, but the coroutine will never resume after the second frame completes. So if we
 * hit the code that sets the flag, we must be in interactive mode.
 */
@Composable
private fun detectInteractivePreviewMode(): State<Boolean> = produceState(false) {
  withFrameMillis {}
  value = true
}

/**
 * Layout [content] with a thin bar on top for [widthControls] and a thin bar on the right for
 * [heightControls].
 */
@Composable
private fun ConstraintsControlsLayout(
  modifier: Modifier,
  widthControls: @Composable () -> Unit,
  heightControls: @Composable () -> Unit,
  corner: @Composable () -> Unit = {},
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
  fillMax: Boolean,
  onMinDrag: (Float) -> Unit,
  onMaxDrag: (Float) -> Unit
) {
  Layout(
    content = {
      DisableTouchSlop {
        ConstraintHandle(
          orientation = Horizontal,
          min = true,
          fill = true,
          onDrag = onMinDrag
        )
        ConstraintHandle(
          orientation = Horizontal,
          min = false,
          fill = fillMax,
          onDrag = onMaxDrag
        )
      }
    },
    modifier = Modifier.drawBehind {
      drawLine(
        ControlsForegroundColor,
        strokeWidth = 1.dp.toPx(),
        start = Offset(min.toFloat() + ConstraintHandleSize.toPx(), size.height / 2),
        end = Offset(max.toFloat() - ConstraintHandleSize.toPx(), size.height / 2f)
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
  fillMax: Boolean,
  onMinDrag: (Float) -> Unit,
  onMaxDrag: (Float) -> Unit
) {
  Layout(
    content = {
      DisableTouchSlop {
        ConstraintHandle(
          orientation = Vertical,
          min = true,
          fill = true,
          onDrag = onMinDrag
        )
        ConstraintHandle(
          orientation = Vertical,
          min = false,
          fill = fillMax,
          onDrag = onMaxDrag
        )
      }
    },
    modifier = Modifier.drawBehind {
      drawLine(
        ControlsForegroundColor,
        strokeWidth = 1.dp.toPx(),
        start = Offset(size.width / 2, min.toFloat() + ConstraintHandleSize.toPx()),
        end = Offset(size.width / 2, max.toFloat() - ConstraintHandleSize.toPx())
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
  fill: Boolean,
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
        val style = if (fill) Fill else Stroke(1.dp.toPx())
        onDrawBehind {
          withTransform({
            if (!min) rotate(180f)
          }) {
            drawPath(path, ControlsForegroundColor, style = style)
          }
        }
      }
  )
}

@Composable
private fun StaticPreviewHeader() {
  BasicText(
    PreviewHeaderText,
    modifier = Modifier
      .fillMaxSize()
      .wrapContentSize(),
    style = TextStyle(
      color = ControlsForegroundColor,
      textAlign = TextAlign.Center,
      fontSize = ControlsFontSize,
    ),
    overflow = TextOverflow.Ellipsis,
  )
}

@Preview(backgroundColor = 0xff0000, showBackground = true)
@Composable
private fun PreviewWithPreferredSizeSmallerThanPreviewSize() {
  val size = 200.dp
  ConstraintsExplorer(
    Modifier
      .wrapContentSize()
      .sizeIn(
        minWidth = 100.dp, minHeight = 100.dp,
        maxWidth = 300.dp, maxHeight = 300.dp
      )
  ) {
    BasicText(
      "Preferred size: $size",
      modifier = Modifier
        .size(size)
        .background(Color.LightGray)
        .wrapContentSize(),
    )
  }
}

@Preview(backgroundColor = 0xff0000, showBackground = true)
@Composable
private fun PreviewWithWrapContentSizeAndDefaultPreviewSize() {
  ConstraintsExplorer(Modifier.wrapContentSize()) {
    BasicText(
      "wrapContentSize",
      modifier = Modifier
        .background(Color.LightGray)
        .wrapContentSize(),
    )
  }
}

@Preview(backgroundColor = 0xff0000, showBackground = true)
@Composable
private fun PreviewWithFillMaxSizeAndDefaultPreviewSize() {
  ConstraintsExplorer(Modifier.wrapContentSize()) {
    BasicText(
      "fillMaxSize",
      modifier = Modifier
        .background(Color.LightGray)
        .fillMaxSize()
        .wrapContentSize(),
    )
  }
}

@Preview(backgroundColor = 0xff0000, showBackground = true)
@Composable
private fun PreviewWithInfiniteConstraints() {
  ConstraintsExplorer(
    Modifier
      .horizontalScroll(rememberScrollState())
      .verticalScroll(rememberScrollState())
  ) {
    BasicText(
      "wrapContentSize",
      modifier = Modifier
        .background(Color.LightGray)
        .wrapContentSize(),
    )
  }
}
