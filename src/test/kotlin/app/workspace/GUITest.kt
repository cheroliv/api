package app.workspace

import org.assertj.swing.edt.GuiActionRunner.execute
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import org.assertj.swing.fixture.FrameFixture

//@org.junit.jupiter.api.extension.ExtendWith
class GUITest {
    private lateinit var window: FrameFixture

    @BeforeTest
    fun setUp() = execute {
        window =
//            context.
            run(Installer::GUI)
                .run(::FrameFixture)
                .apply(FrameFixture::show)
    }

    @AfterTest
    fun tearDown() = window.cleanUp()
}