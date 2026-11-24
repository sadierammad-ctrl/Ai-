package com.example.data.local

import kotlinx.coroutines.flow.Flow

class ChatHistoryRepository(private val dao: ConversationDao) {

    fun observeConversations(searchQuery: String = ""): Flow<List<ConversationEntity>> {
        return if (searchQuery.isBlank()) {
            dao.observeAllConversations()
        } else {
            dao.searchConversations(searchQuery.trim())
        }
    }

    fun observeMessages(conversationId: Long): Flow<List<MessageEntity>> =
        dao.observeMessagesForConversation(conversationId)

    suspend fun getMessages(conversationId: Long): List<MessageEntity> =
        dao.getMessagesForConversation(conversationId)

    suspend fun createConversation(title: String, preview: String, languageCode: String): Long {
        val now = System.currentTimeMillis()
        return dao.insertConversation(
            ConversationEntity(
                title = title.take(60),
                preview = preview.take(120),
                languageCode = languageCode,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun addMessage(
        conversationId: Long,
        role: String,
        content: String,
        isSampleResponse: Boolean
    ): Long {
        val id = dao.insertMessage(
            MessageEntity(
                conversationId = conversationId,
                role = role,
                content = content,
                isSampleResponse = isSampleResponse,
                timestamp = System.currentTimeMillis()
            )
        )
        val existing = dao.getConversationById(conversationId)
        val title = if (existing == null || existing.title.isBlank() || existing.title == "New Conversation" || existing.title == "নতুন কথোপকথন") {
            if (role == "user") content.take(48) else existing?.title ?: "BABY AI"
        } else {
            existing.title
        }
        dao.updateConversationSummary(
            id = conversationId,
            title = title,
            preview = content.take(100),
            updatedAt = System.currentTimeMillis()
        )
        return id
    }

    suspend fun deleteMessage(messageId: Long) = dao.deleteMessageById(messageId)

    suspend fun clearConversationMessages(conversationId: Long) {
        dao.deleteMessagesForConversation(conversationId)
    }

    suspend fun deleteConversation(conversationId: Long) {
        dao.deleteConversationById(conversationId)
    }

    suspend fun clearAllHistory() {
        dao.deleteAllConversations()
    }
}
