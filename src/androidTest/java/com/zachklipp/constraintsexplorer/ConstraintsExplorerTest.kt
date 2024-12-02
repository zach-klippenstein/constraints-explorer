package com.zachklipp.constraintsexplorer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.constrain
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class ConstraintsExplorerTest {

  @get:Rule val rule = createComposeRule()

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

  @Test fun clickingMaxWidthHandleTogglesBounds() {
    var constraints: Constraints? = null
    rule.mainClock.autoAdvance = true
    val min = 1000
    rule.setContent {
      ConstraintsExplorerImpl(
        showControls = true,
        modifier = Modifier
          .background(Color.Blue)
          .constrain(
            with(rule.density) {
              Constraints(
                minWidth = 0,
                maxWidth = min + ControlsSize.roundToPx(),
                minHeight = 0,
                maxHeight = min + ControlsSize.roundToPx()
              )
            }
          )
      ) {
        Box(
          Modifier
            .onConstraints { constraints = it }
            .sizePx(min)
            .background(Color.Red)
        )
      }
    }

    rule.onNodeWithTag(MaxWidthHandleTag).isDisplayed()
    rule.runOnIdle {
      assertEquals(
        with(rule.density) {
          Constraints(
            minWidth = 0,
            maxWidth = min,
            minHeight = 0,
            maxHeight = min
          )
        },
        constraints
      )
    }

    rule.onNodeWithTag(MaxWidthHandleTag).performClick()

    rule.runOnIdle {
      assertEquals(
        Constraints(
          minWidth = 0,
          maxWidth = Constraints.Infinity,
          minHeight = 0,
          maxHeight = min
        ),
        constraints
      )
    }

    rule.onNodeWithTag(MaxWidthHandleTag).performClick()

    rule.runOnIdle {
      assertEquals(
        Constraints(
          minWidth = 0,
          maxWidth = min,
          minHeight = 0,
          maxHeight = min
        ),
        constraints
      )
    }
  }

  @Test fun clickingMaxHeightHandleTogglesBounds() {
    var constraints: Constraints? = null
    rule.mainClock.autoAdvance = true
    val min = 1000
    rule.setContent {
      ConstraintsExplorerImpl(
        showControls = true,
        modifier = Modifier
          .background(Color.Blue)
          .constrain(
            with(rule.density) {
              Constraints(
                minWidth = 0,
                maxWidth = min + ControlsSize.roundToPx(),
                minHeight = 0,
                maxHeight = min + ControlsSize.roundToPx()
              )
            }
          )
      ) {
        Box(
          Modifier
            .onConstraints { constraints = it }
            .sizePx(min)
            .background(Color.Red)
        )
      }
    }

    rule.onNodeWithTag(MaxHeightHandleTag).isDisplayed()
    rule.runOnIdle {
      assertEquals(
        with(rule.density) {
          Constraints(
            minWidth = 0,
            maxWidth = min,
            minHeight = 0,
            maxHeight = min
          )
        },
        constraints
      )
    }

    rule.onNodeWithTag(MaxHeightHandleTag).performClick()

    rule.runOnIdle {
      assertEquals(
        Constraints(
          minWidth = 0,
          maxWidth = min,
          minHeight = 0,
          maxHeight = Constraints.Infinity
        ),
        constraints
      )
    }

    rule.onNodeWithTag(MaxHeightHandleTag).performClick()

    rule.runOnIdle {
      assertEquals(
        Constraints(
          minWidth = 0,
          maxWidth = min,
          minHeight = 0,
          maxHeight = min
        ),
        constraints
      )
    }
  }

  @Test fun draggingMaxWidthHandleEnablesBounds() {
    var constraints: Constraints? = null
    rule.mainClock.autoAdvance = true
    val min = 1000
    rule.setContent {
      ConstraintsExplorerImpl(
        showControls = true,
        modifier = Modifier
          .background(Color.Blue)
          .constrain(
            with(rule.density) {
              Constraints(
                minWidth = 0,
                maxWidth = min + ControlsSize.roundToPx(),
                minHeight = 0,
                maxHeight = min + ControlsSize.roundToPx()
              )
            }
          )
      ) {
        Box(
          Modifier
            .onConstraints { constraints = it }
            .sizePx(min)
            .background(Color.Red)
        )
      }
    }

    rule.onNodeWithTag(MaxWidthHandleTag).performClick()
    rule.runOnIdle {
      assertEquals(
        with(rule.density) {
          Constraints(
            minWidth = 0,
            maxWidth = Constraints.Infinity,
            minHeight = 0,
            maxHeight = min
          )
        },
        constraints
      )
    }

    rule.onNodeWithTag(MaxWidthHandleTag).performTouchInput {
      down(center)
      moveBy(center - Offset(10f + viewConfiguration.touchSlop, 0f))
      up()
    }

    rule.runOnIdle {
      assertTrue(constraints!!.maxWidth < min)
      assertEquals(min, constraints!!.maxHeight)
    }
  }

  @Test fun draggingMaxHeightHandleEnablesBounds() {
    var constraints: Constraints? = null
    rule.mainClock.autoAdvance = true
    val min = 1000
    rule.setContent {
      ConstraintsExplorerImpl(
        showControls = true,
        modifier = Modifier
          .background(Color.Blue)
          .constrain(
            with(rule.density) {
              Constraints(
                minWidth = 0,
                maxWidth = min + ControlsSize.roundToPx(),
                minHeight = 0,
                maxHeight = min + ControlsSize.roundToPx()
              )
            }
          )
      ) {
        Box(
          Modifier
            .onConstraints { constraints = it }
            .sizePx(min)
            .background(Color.Red)
        )
      }
    }

    rule.onNodeWithTag(MaxHeightHandleTag).performClick()
    rule.runOnIdle {
      assertEquals(
        with(rule.density) {
          Constraints(
            minWidth = 0,
            maxWidth = min,
            minHeight = 0,
            maxHeight = Constraints.Infinity
          )
        },
        constraints
      )
    }

    rule.onNodeWithTag(MaxHeightHandleTag).performTouchInput {
      down(center)
      moveBy(center - Offset(0f, 10f + viewConfiguration.touchSlop))
      up()
    }

    rule.runOnIdle {
      assertTrue(constraints!!.maxHeight < min)
      assertEquals(min, constraints!!.maxWidth)
    }
  }

  private fun Modifier.sizePx(size: Int) = layout { measurable, constraints ->
    val childSize = constraints.constrain(IntSize(size, size))
    val childConstraints = Constraints.fixed(childSize.width, childSize.height)
    val placeable = measurable.measure(childConstraints)
    layout(placeable.width, placeable.height) {
      placeable.place(0, 0)
    }
  }

  private fun Modifier.constrain(
    constraints: Constraints
  ) = layout { measurable, parentConstraints ->
    val childConstraints = parentConstraints.constrain(constraints)
    val placeable = measurable.measure(childConstraints)
    layout(placeable.width, placeable.height) {
      placeable.place(0, 0)
    }
  }

  private fun Modifier.onConstraints(
    onConstraints: (Constraints) -> Unit
  ): Modifier = layout { measurable, constraints ->
    onConstraints(constraints)
    val placeable = measurable.measure(constraints)
    layout(placeable.width, placeable.height) {
      placeable.place(0, 0)
    }
  }
}
