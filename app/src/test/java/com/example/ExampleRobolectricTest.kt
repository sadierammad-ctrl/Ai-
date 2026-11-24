package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.BabyAiDatabase
import com.example.data.local.ChatHistoryRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun pumpFrames() {
        repeat(4) {
            composeRule.mainClock.advanceTimeByFrame()
        }
        composeRule.mainClock.advanceTimeBy(250L)
    }

    @Test
    fun appMetadataAndBrandingAreConfigured() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("BABY AI", context.getString(R.string.app_name))
        assertEquals("com.siam.babyai", context.packageName)
    }

    @Test
    fun homeDashboardNavigationAndAlwaysReadyScreenWork() {
        composeRule.mainClock.autoAdvance = false
        pumpFrames()

        composeRule.onNodeWithTag("home_ai_orb").assertIsDisplayed()
        composeRule.onNodeWithTag("firebase_notice_banner").assertIsDisplayed()

        // Open Always-Ready Screen from Home chip
        composeRule.onNodeWithTag("home_always_ready_chip").performClick()
        pumpFrames()
        composeRule.onNodeWithTag("always_ready_list").assertIsDisplayed()
        composeRule.onAllNodesWithText("Always-Ready \"Hey Baby\"", substring = true)
            .let { assertTrue(it.fetchSemanticsNodes().isNotEmpty()) }

        // Navigate to Chat and send a Bengali prompt
        composeRule.onNodeWithTag("nav_chat").let {
            // Return to Home first because Always-Ready hides bottom bar
            composeRule.activityRule.scenario.onActivity { act -> act.onBackPressedDispatcher.onBackPressed() }
        }
        pumpFrames()

        composeRule.onNodeWithTag("nav_chat").performClick()
        pumpFrames()
        composeRule.onNodeWithTag("chat_text_input").assertIsDisplayed()
        composeRule.onNodeWithTag("chat_text_input").performTextReplacement("হ্যালো বেবি")
        pumpFrames()
        composeRule.onNodeWithTag("chat_send_button").performClick()
        var foundMessage = false
        repeat(20) {
            Thread.sleep(50)
            pumpFrames()
            if (composeRule.onAllNodesWithText("হ্যালো বেবি", substring = true).fetchSemanticsNodes().isNotEmpty()) {
                foundMessage = true
                return@repeat
            }
        }
        assertTrue(foundMessage)

        // Navigate to Voice
        composeRule.onNodeWithTag("nav_voice").performClick()
        pumpFrames()
        composeRule.onNodeWithTag("voice_screen_orb").assertIsDisplayed()

        // Navigate to History
        composeRule.onNodeWithTag("nav_history").performClick()
        pumpFrames()
        composeRule.onNodeWithTag("history_search_input").assertIsDisplayed()

        // Navigate to Settings
        composeRule.onNodeWithTag("nav_settings").performClick()
        pumpFrames()
        composeRule.onNodeWithTag("settings_list").assertIsDisplayed()
    }

    @Test
    fun roomDatabasePersistsAndDeletesConversations() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, BabyAiDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repo = ChatHistoryRepository(db.conversationDao())

        val id = repo.createConversation("বাংলা টেস্ট", "হেই বেবি!", "bn")
        repo.addMessage(id, "user", "হেই বেবি!", isSampleResponse = false)
        repo.addMessage(id, "assistant", "হেই বেবি! আমি তোমার BABY AI।", isSampleResponse = true)

        val conversations = repo.observeConversations().first()
        assertEquals(1, conversations.size)
        assertEquals("বাংলা টেস্ট", conversations.first().title)

        val messages = repo.getMessages(id)
        assertEquals(2, messages.size)

        repo.deleteConversation(id)
        assertTrue(repo.observeConversations().first().isEmpty())
        db.close()
    }
}
