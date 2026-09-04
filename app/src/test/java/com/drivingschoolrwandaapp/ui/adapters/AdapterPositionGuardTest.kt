package com.drivingschoolrwandaapp.ui.adapters

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Regression tests for adapter position guards.
 *
 * RecyclerView.Adapter.getAdapterPosition() can return NO_POSITION (-1) during
 * layout animations or when a ViewHolder is being removed. If a click listener
 * uses this value to index into a data list, it causes an IndexOutOfBoundsException.
 *
 * These tests scan every adapter source file and verify that every call to
 * getAdapterPosition() / getBindingAdapterPosition() is preceded by a NO_POSITION
 * check. If someone adds a new adapter without the guard, this test fails.
 */
class AdapterPositionGuardTest {

    private val adapterDir = File("src/main/java/com/drivingschoolrwandaapp/ui/adapters")

    /**
     * All adapter files with their click-listener patterns.
     * Each adapter must guard getAdapterPosition() with a NO_POSITION check.
     */
    private data class AdapterFile(val name: String, val file: File)

    private val adapterFiles: List<AdapterFile> by lazy {
        adapterDir.walkTopDown()
            .filter { it.isFile && (it.extension == "java" || it.extension == "kt") }
            .map { AdapterFile(it.nameWithoutExtension, it) }
            .toList()
    }

    @Test
    fun `all adapters with getAdapterPosition guard with NO_POSITION check`() {
        val violations = mutableListOf<String>()

        for (adapter in adapterFiles) {
            val content = adapter.file.readText()
            val lines = content.lines()

            for (i in lines.indices) {
                val line = lines[i]
                // Check for getAdapterPosition() or getBindingAdapterPosition() calls
                val posCall = Regex("""getAdapterPosition\(\)|getBindingAdapterPosition\(\)""")
                if (posCall.containsMatchIn(line)) {
                    // Look in the surrounding 10 lines for a NO_POSITION check
                    val start = maxOf(0, i - 10)
                    val end = minOf(lines.size - 1, i + 5)
                    val context = lines.subList(start, end + 1).joinToString("\n")

                    if (!context.contains("NO_POSITION") && !context.contains("RecyclerView.NO_POSITION")) {
                        violations.add(
                            "${adapter.name}.java line ${i + 1}: getAdapterPosition() without NO_POSITION guard"
                        )
                    }
                }
            }
        }

        assertTrue(
            "The following adapters have unguarded getAdapterPosition() calls (will crash with IndexOutOfBoundsException):\n" +
                violations.joinToString("\n") { "  - $it" },
            violations.isEmpty()
        )
    }

    @Test
    fun `all adapters use safe list access after position check`() {
        val violations = mutableListOf<String>()

        for (adapter in adapterFiles) {
            val content = adapter.file.readText()
            val lines = content.lines()

            // Find click listener lambdas that call getAdapterPosition() then access a list
            val posCall = Regex("""getAdapterPosition\(\)|getBindingAdapterPosition\(\)""")
            val listAccess = Regex("""\.(?:get|remove|set)\(""")

            for (i in lines.indices) {
                if (posCall.containsMatchIn(lines[i])) {
                    // Look at the next 15 lines for list access
                    val end = minOf(lines.size - 1, i + 15)
                    val followingLines = lines.subList(i, end + 1).joinToString("\n")

                    // If there's a list access but no NO_POSITION guard nearby
                    if (listAccess.containsMatchIn(followingLines)) {
                        val contextStart = maxOf(0, i - 5)
                        val contextEnd = minOf(lines.size - 1, i + 15)
                        val context = lines.subList(contextStart, contextEnd + 1).joinToString("\n")

                        if (!context.contains("NO_POSITION")) {
                            violations.add(
                                "${adapter.name}.java line ${i + 1}: list access after getAdapterPosition() without NO_POSITION guard"
                            )
                        }
                    }
                }
            }
        }

        assertTrue(
            "The following adapters access lists without NO_POSITION guard (will crash with IndexOutOfBoundsException):\n" +
                violations.joinToString("\n") { "  - $it" },
            violations.isEmpty()
        )
    }

    @Test
    fun `all adapters check listener for null before calling`() {
        val violations = mutableListOf<String>()

        for (adapter in adapterFiles) {
            val content = adapter.file.readText()
            val lines = content.lines()

            val listenerCall = Regex("""(?:click|itemClick|optionSelect|download|user)\w*Listener\??\.\w+\(""")
            val nullCheck = Regex("""(?:click|itemClick|optionSelect|download|user)\w*Listener\s*!=\s*null|if\s*\(\s*listener\s*!=\s*null""")

            for (i in lines.indices) {
                if (listenerCall.containsMatchIn(lines[i])) {
                    val start = maxOf(0, i - 5)
                    val context = lines.subList(start, i + 1).joinToString("\n")

                    if (!nullCheck.containsMatchIn(context)) {
                        violations.add(
                            "${adapter.name}.java line ${i + 1}: listener call without null check"
                        )
                    }
                }
            }
        }

        assertTrue(
            "The following adapters call listeners without null checks:\n" +
                violations.joinToString("\n") { "  - $it" },
            violations.isEmpty()
        )
    }
}
