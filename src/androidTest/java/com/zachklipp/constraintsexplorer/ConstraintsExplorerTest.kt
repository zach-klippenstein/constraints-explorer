package com.zachklipp.constraintsexplorer

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.constrain
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

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

  private fun Modifier.sizePx(size: Int) = layout { measurable, constraints ->
    val childSize = constraints.constrain(IntSize(size, size))
    val childConstraints = Constraints.fixed(childSize.width, childSize.height)
    val placeable = measurable.measure(childConstraints)
    layout(placeable.width, placeable.height) {
      placeable.place(0, 0)
    }
  }
}
