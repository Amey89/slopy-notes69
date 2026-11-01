package com.example

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.example.ui.components.RichTextViewer
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun rich_text_viewer_screenshot() {
        composeTestRule.setContent {
            MyApplicationTheme {
                RichTextViewer(
                    markdownContent = """
                        # Project Kickoff
                        ## Action Items
                        - [x] Room database setup
                        - [ ] Configure full-screen alarms
                        - [ ] Local JSON and Markdown backups
                        
                        > Note: Maximum smoothness and performance enabled!
                    """.trimIndent(),
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/rich_text_viewer.png")
    }
}
