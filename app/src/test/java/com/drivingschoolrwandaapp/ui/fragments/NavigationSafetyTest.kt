package com.drivingschoolrwandaapp.ui.fragments

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Regression tests for navigation safety.
 *
 * These tests parse the nav graph XML and cross-reference every action ID
 * referenced in Fragment Java/Kotlin source code. If someone deletes an
 * action from the graph but forgets to remove the navigate() call, this
 * test fails — preventing the IllegalArgumentException crash in production.
 */
class NavigationSafetyTest {

    private val navGraphFile = File("src/main/res/navigation/main_nav_graph.xml")
    private val fragmentDir = File("src/main/java/com/drivingschoolrwandaapp/ui/fragments")

    /**
     * All action IDs referenced in Fragment source code via R.id.action_*.
     * Each entry is (fileName, actionId).
     */
    /** Menu item IDs that start with action_ but are not navigation actions. */
    private val menuItemIds = setOf(
        "action_search", "action_logout", "action_test_history",
        "action_toggle_layout", "action_instructions",
        "action_bookmark", "action_go_to_page", "action_view_bookmarks"
    )

    private val fragmentActions: List<Pair<String, String>> by lazy {
        val actions = mutableListOf<Pair<String, String>>()
        fragmentDir.walkTopDown()
            .filter { it.isFile && (it.extension == "java" || it.extension == "kt") }
            .forEach { file ->
                val content = file.readText()
                // Match R.id.action_* references that look like navigation actions
                val regex = Regex("""R\.id\.(action_\w+)""")
                regex.findAll(content).forEach { match ->
                    val id = match.groupValues[1]
                    if (id !in menuItemIds) {
                        actions.add(file.name to id)
                    }
                }
            }
        actions
    }

    /**
     * All action IDs defined in the nav graph XML.
     */
    private val definedActions: Set<String> by lazy {
        val content = navGraphFile.readText()
        val regex = Regex("""android:id="@\+id/(action_\w+)"""")
        regex.findAll(content).map { it.groupValues[1] }.toSet()
    }

    @Test
    fun `all navigation action IDs referenced in fragments exist in nav graph`() {
        val missingActions = mutableListOf<String>()
        for ((fileName, actionId) in fragmentActions) {
            // Skip non-navigation actions (menu items, global actions defined elsewhere)
            if (actionId.startsWith("action_toggle") || actionId.startsWith("action_instructions")) continue
            if (actionId !in definedActions) {
                missingActions.add("$fileName references R.id.$actionId which is not in the nav graph")
            }
        }
        assertTrue(
            "The following navigation action IDs are referenced in fragments but missing from the nav graph:\n" +
                missingActions.joinToString("\n") { "  - $it" } +
                "\n\nThis will cause an IllegalArgumentException crash at runtime.",
            missingActions.isEmpty()
        )
    }

    @Test
    fun `all destination IDs referenced in nav graph are valid`() {
        val content = navGraphFile.readText()
        val destRefs = Regex("""app:destination="@\+id/(\w+)"""")
            .findAll(content)
            .map { it.groupValues[1] }
            .toList()
        val definedIds = Regex("""android:id="@\+id/(\w+)"""")
            .findAll(content)
            .map { it.groupValues[1] }
            .toSet()

        val undefinedDests = destRefs.filter { it !in definedIds }
        assertTrue(
            "The following destinations are referenced but not defined: $undefinedDests",
            undefinedDests.isEmpty()
        )
    }

    @Test
    fun `all required navigation action IDs for DashboardFragment are present`() {
        val requiredActions = listOf(
            "action_dashboardFragment_to_testsFragment",
            "action_dashboardFragment_to_resultsFragment",
            "action_dashboardFragment_to_materialsFragment",
            "action_dashboardFragment_to_profileFragment",
        )
        for (actionId in requiredActions) {
            assertTrue(
                "DashboardFragment requires R.id.$actionId but it is missing from the nav graph",
                actionId in definedActions
            )
        }
    }

    @Test
    fun `all required navigation action IDs for TestsFragment are present`() {
        val requiredActions = listOf(
            "action_testsFragment_to_resultsFragment",
            "action_global_testQuestionsFragment",
        )
        for (actionId in requiredActions) {
            assertTrue(
                "TestsFragment requires R.id.$actionId but it is missing from the nav graph",
                actionId in definedActions
            )
        }
    }

    @Test
    fun `all required navigation action IDs for TestQuestionsFragment are present`() {
        val requiredActions = listOf(
            "action_testQuestionsFragment_to_testResultFragment",
        )
        for (actionId in requiredActions) {
            assertTrue(
                "TestQuestionsFragment requires R.id.$actionId but it is missing from the nav graph",
                actionId in definedActions
            )
        }
    }

    @Test
    fun `all required navigation action IDs for ResultsFragment are present`() {
        val requiredActions = listOf(
            "action_global_testQuestionsFragment",
        )
        for (actionId in requiredActions) {
            assertTrue(
                "ResultsFragment requires R.id.$actionId but it is missing from the nav graph",
                actionId in definedActions
            )
        }
    }
}
