package com.awab.ai.data.models

data class ConversationContext(
    var lastUserMessage: String = "",
    var lastAIResponse: String = "",
    var messageCount: Int = 0,
    var lastInteractionTime: Long = 0L,
    var discussedTopics: MutableSet<String> = mutableSetOf(),
    var currentSpeaker: String? = null,
    var emotionalState: String = "neutral",
    var preferredResponseStyle: String = "detailed"
)

data class ChatMessage(
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long,
    val messageType: MessageType = MessageType.TEXT,
    val speakerName: String? = null,
    val confidence: Float = 1.0f
)

enum class MessageType {
    TEXT, VOICE, IMAGE, SYSTEM
}