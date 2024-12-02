package com.zachklipp.constraintsexplorer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.constrain
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@Suppress("SameParameterValue")
@RunWith(AndroidJUnit4::class)
class ConstraintsExplorerTest {

  @get:Rule val rule = createComposeRule()

  private val tag = "tag"

  @Before
  fun setUp() {
    // This component has meaningfully different behavior on the first frame vs subsequent frames,
    // so tests need to control frames individually.
    rule.mainClock.autoAdvance = false
  }

  @Test
  fun controlsAreaShownOnFirstFrameWhenEnabled() {
    rule.setContent {
      ConstraintsExplorer(enabled = true) {
        Box(Modifier.sizePx(10))
      }
    }

    val rootNode = rule.onRoot().fetchSemanticsNode()
    with(rule.density) {
      assertEquals(10 + ControlsSize.roundToPx(), rootNode.size.width)
      assertEquals(10 + ControlsSize.roundToPx(), rootNode.size.height)
    }
  }

  @Test
  fun controlsAreaNotShownWhenNotEnabled() {
    rule.setContent {
      ConstraintsExplorer(enabled = false) {
        Box(Modifier.sizePx(10))
      }
    }

    with(rule.onRoot().fetchSemanticsNode()) {
      assertEquals(10, size.width)
      assertEquals(10, size.height)
    }

    repeat(3) {
      rule.mainClock.advanceTimeByFrame()
    }

    with(rule.onRoot().fetchSemanticsNode()) {
      assertEquals(10, size.width)
      assertEquals(10, size.height)
    }
  }

  @Test fun previewHeaderOnlyShownOnFirstFrame() {
    rule.setContent {
      ConstraintsExplorer(enabled = true) {
        Box(Modifier.sizePx(10))
      }
    }

    rule.onNodeWithText(PreviewHeaderText).isDisplayed()

    rule.mainClock.advanceTimeByFrame()

    rule.onNodeWithText(PreviewHeaderText).isNotDisplayed()
  }

  private fun Modifier.sizePx(size: Int) = layout { measurable, constraints ->
    val childSize = constraints.constrain(IntSize(size, size))
    val childConstraints = Constraints.fixed(childSize.width, childSize.height)
    val placeable = measurable.measure(childConstraints)
    layout(placeable.width, placeable.height) {
      placeable.place(0, 0)
    }
  }

  @Test fun readingIntrinsicsOnFirstFrameDoesNotInitializeViewport() {
    var showControls by mutableStateOf(false)
    var minIntrinsicWidth = -1
    var maxIntrinsicWidth = -1
    var minIntrinsicHeight = -1
    var maxIntrinsicHeight = -1
    rule.setContent {
      ConstraintsExplorerImpl(
        showControls = showControls,
        modifier = Modifier
          .testTag(tag)
          .sizeIn(minWidth = 0.dp, minHeight = 0.dp, maxWidth = 100.dp, maxHeight = 100.dp)
          .readIntrinsics { minWidth, maxWidth, minHeight, maxHeight ->
            minIntrinsicWidth = minWidth
            maxIntrinsicWidth = maxWidth
            minIntrinsicHeight = minHeight
            maxIntrinsicHeight = maxHeight
          }
      ) {
        BoxWithIntrinsicSize(
          minIntrinsicWidth = 5,
          maxIntrinsicWidth = 10,
          minIntrinsicHeight = 15,
          maxIntrinsicHeight = 20,
          width = 1,
          height = 2
        )
      }
    }

    // Non-interactive mode
    rule.onNodeWithTag(tag).fetchSemanticsNode().also {
      assertEquals(IntSize(1, 2), it.size)
      assertEquals(5, minIntrinsicWidth)
      assertEquals(10, maxIntrinsicWidth)
      assertEquals(15, minIntrinsicHeight)
      assertEquals(20, maxIntrinsicHeight)
    }

    showControls = true

    // Interactive mode
    rule.onNodeWithTag(tag).fetchSemanticsNode().also {
      val expectedSize = with(rule.density) {
        IntSize(1 + ControlsSize.roundToPx(), 2 + ControlsSize.roundToPx())
      }
      assertEquals(expectedSize, it.size)
      assertEquals(expectedSize.width, minIntrinsicWidth)
      assertEquals(expectedSize.width, maxIntrinsicWidth)
      assertEquals(expectedSize.height, minIntrinsicHeight)
      assertEquals(expectedSize.height, maxIntrinsicHeight)
    }
  }

  private fun Modifier.readIntrinsics(
    onIntrinsics: (minWidth: Int, maxWidth: Int, minHeight: Int, maxHeight: Int) -> Unit
  ): Modifier = layout { measurable, constraints ->
    val minWidth = measurable.minIntrinsicWidth(Constraints.Infinity)
    val maxWidth = measurable.maxIntrinsicWidth(Constraints.Infinity)
    val minHeight = measurable.minIntrinsicHeight(Constraints.Infinity)
    val maxHeight = measurable.maxIntrinsicHeight(Constraints.Infinity)
    onIntrinsics(minWidth, maxWidth, minHeight, maxHeight)

    val placeable = measurable.measure(constraints)
    layout(placeable.width, placeable.height) {
      placeable.place(0, 0)
    }
  }

  @Composable
  private fun BoxWithIntrinsicSize(
    minIntrinsicWidth: Int,
    maxIntrinsicWidth: Int,
    minIntrinsicHeight: Int,
    maxIntrinsicHeight: Int,
    width: Int,
    height: Int,
  ) {
    val measurePolicy = remember(
      minIntrinsicWidth,
      maxIntrinsicWidth,
      minIntrinsicHeight,
      maxIntrinsicHeight
    ) {
      object : MeasurePolicy {
        override fun MeasureScope.measure(
          measurables: List<Measurable>,
          constraints: Constraints
        ): MeasureResult = layout(width, height) {}

        override fun IntrinsicMeasureScope.minIntrinsicWidth(
          measurables: List<IntrinsicMeasurable>,
          height: Int
        ): Int = minIntrinsicWidth

        override fun IntrinsicMeasureScope.minIntrinsicHeight(
          measurables: List<IntrinsicMeasurable>,
          width: Int
        ): Int = minIntrinsicHeight

        override fun IntrinsicMeasureScope.maxIntrinsicWidth(
          measurables: List<IntrinsicMeasurable>,
          height: Int
        ): Int = maxIntrinsicWidth

        override fun IntrinsicMeasureScope.maxIntrinsicHeight(
          measurables: List<IntrinsicMeasurable>,
          width: Int
        ): Int = maxIntrinsicHeight
      }
    }
    Layout(measurePolicy = measurePolicy)
  }
}
