package com.elpassion.intelijidea.common.console

import org.assertj.core.api.Assertions
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteToLocalInputConverterTest {

    private val PROJECT_NAME = "testProject"
    private val localBasePath = "base/path/$PROJECT_NAME"
    private val converter = RemoteToLocalInputConverter(PROJECT_NAME)

    @Test
    fun `Should catch file path from remote machine`() {
        assertTrue(converter.FILE_PATH_REGEX.matches("/mainframer/$PROJECT_NAME"))
    }

    @Test
    fun `Should not catch file path if it is not from remote machine`() {
        assertFalse(converter.FILE_PATH_REGEX.matches("/$PROJECT_NAME"))
    }

    @Test
    fun `Should catch file path if it starts with longer path`() {
        assertTrue(converter.FILE_PATH_REGEX.matches("/longer/path/mainframer/$PROJECT_NAME"))
    }

    @Test
    fun `Should catch file path if it ends with kotlin class name`() {
        assertTrue(converter.FILE_PATH_REGEX.matches("/longer/path/mainframer/$PROJECT_NAME/Example.kt"))
    }

    @Test
    fun `Should catch file path if it ends with java class name`() {
        assertTrue(converter.FILE_PATH_REGEX.matches("/longer/path/mainframer/$PROJECT_NAME/Example.java"))
    }

    @Test
    fun `Should not catch file path if it ends with undefined class name`() {
        assertFalse(converter.FILE_PATH_REGEX.matches("/longer/path/mainframer/$PROJECT_NAME/Example."))
    }

    @Test
    fun `Should replace remote base path with given local path`() {
        val replacedPath = "Error: /longer/path/mainframer/$PROJECT_NAME/com/elpassion/mainframer/Example.kt: (19, 10): error".replace(converter.FILE_PATH_REGEX, "$localBasePath<$1>")
        Assertions.assertThat(replacedPath).isEqualTo("Error: $localBasePath</com/elpassion/mainframer/Example.kt>: (19, 10): error")
    }

    @Test
    fun `Should catch colon and space signs in first fragment of line`() {
        assertTrue(converter.FIRST_FRAGMENT_REGEX.matches(": "))
    }

    @Test
    fun `Should catch whole first fragment of line`() {
        assertTrue(converter.FIRST_FRAGMENT_REGEX.matches("Very complicated exception: "))
    }

    @Test
    fun `Should check only one line if it matches first fragment regex`() {
        assertFalse(converter.FIRST_FRAGMENT_REGEX.matches("Very complicated exception\n: "))
    }

    @Test
    fun `Should replace first fragment only`() {
        val replacedPath = "Error: /longer/path/mainframer/$PROJECT_NAME/com/elpassion/mainframer/Example.kt: (19, 10): error".replaceFirst(converter.FIRST_FRAGMENT_REGEX, "<$1>")
        Assertions.assertThat(replacedPath).isEqualTo("<Error: >/longer/path/mainframer/$PROJECT_NAME/com/elpassion/mainframer/Example.kt: (19, 10): error")
    }

    @Test
    fun `Should replace remote base path with given local path and have first fragment`() {
        val replacedPath = "Complicated error: /longer/path/mainframer/$PROJECT_NAME/com/elpassion/mainframer/Example.kt: (19, 10): error: Errors everywhere!".replace(converter.LINE_WITH_REMOTE_EXCEPTION, "$localBasePath$1:$2")
        Assertions.assertThat(replacedPath).isEqualTo("Complicated error: $localBasePath/com/elpassion/mainframer/Example.kt:19: error: Errors everywhere!")
    }

    @Test
    fun `Should replace remote base path with given local path also when line number is simply given`() {
        val replacedPath = "Complicated error: /longer/path/mainframer/$PROJECT_NAME/com/elpassion/mainframer/Example.kt:19: error: Errors everywhere!".replace(converter.LINE_WITH_REMOTE_EXCEPTION, "$localBasePath$1:$2")
        Assertions.assertThat(replacedPath).isEqualTo("Complicated error: $localBasePath/com/elpassion/mainframer/Example.kt:19: error: Errors everywhere!")
    }

    @Test
    fun `Should catch line number`() {
        assertTrue(converter.LINE_NUMBER_REGEX.matches(":100"))
    }

    @Test
    fun `Should not catch wrong line number`() {
        assertFalse(converter.LINE_NUMBER_REGEX.matches(":wrongLineNumber"))
    }

    @Test
    fun `Should catch line number when there is also given column number`() {
        assertTrue(converter.LINE_NUMBER_REGEX.matches(": (9, 10)"))
    }

    @Test
    fun `Should format correctly simple path line number value`() {
        val replacedPathSimple = ":321".replace(converter.LINE_NUMBER_REGEX, ":$1")
        Assertions.assertThat(replacedPathSimple).isEqualTo(":321")
    }

    @Test
    fun `Should format correctly complex path line number value`() {
        val replacedPathComplex = ": (90, 100)".replace(converter.LINE_NUMBER_REGEX, ":$1")
        Assertions.assertThat(replacedPathComplex).isEqualTo(":90")
    }

    @Test
    fun `Should catch last fragment of line`() {
        assertTrue(converter.LAST_FRAGMENT_REGEX.matches(": Rest of line"))
    }

    @Test
    fun `Should not catch last fragment of line if it is incorrect`() {
        assertFalse(converter.LAST_FRAGMENT_REGEX.matches("Rest of line"))
    }

    @Test
    fun `Should catch path segment`() {
        assertTrue(converter.PATH_SEGMENT.toRegex().matches("/test/test2"))
    }

}

class RemoteToLocalInputConverter(projectName: String) {
    val PATH_SEGMENT = "(?:/\\w+)*?"
    private val FILE_EXTENSION = "\\.\\w+"
    private val END_PATH = "($PATH_SEGMENT$FILE_EXTENSION)*"
    private val REMOTE_START_PATH = "(?:$PATH_SEGMENT)*"
    private val REMOTE_PATH = "$REMOTE_START_PATH/mainframer/$projectName"
    val FILE_PATH_REGEX = "(?:$REMOTE_PATH$END_PATH)".toRegex()
    val FIRST_FRAGMENT_REGEX = "(.*?:\\s)".toRegex()
    private val LINE_NUMBER_START = ":(?:\\s\\()?"
    private val LINE_NUMBER_VALUE = "(\\d+)"
    private val LINE_NUMBER_END = "(?:,\\s\\d+\\))?"
    val LINE_NUMBER_REGEX = "$LINE_NUMBER_START$LINE_NUMBER_VALUE$LINE_NUMBER_END".toRegex()
    val LAST_FRAGMENT_REGEX = ":\\s.+".toRegex()
    val LINE_WITH_REMOTE_EXCEPTION = "${FILE_PATH_REGEX.pattern}${LINE_NUMBER_REGEX.pattern}".toRegex()
}
