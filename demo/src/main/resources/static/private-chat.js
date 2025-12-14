// Private Chat Client - 1-on-1 messaging

let stompClient = null;
let currentUser = null;
let participantId = null;
let participantUsername = null;

// ============================================================================
// INITIALIZATION
// ============================================================================

document.addEventListener('DOMContentLoaded', function() {
    // Get user info from session
    currentUser = sessionStorage.getItem('username');
    participantId = sessionStorage.getItem('chatParticipantId');
    participantUsername = sessionStorage.getItem('chatParticipantUsername');
    
    if (!currentUser || !participantId || !participantUsername) {
        window.location.href = 'conversations.html';
        return;
    }
    
    // Display participant name
    document.getElementById('participantName').textContent = participantUsername;
    
    // Connect to WebSocket
    connectWebSocket();
    
    // Load message history
    loadMessageHistory();
});

// ============================================================================
// WEBSOCKET CONNECTION
// ============================================================================

function connectWebSocket() {
    console.log('🔌 Connecting to WebSocket...');
    updateConnectionStatus('Connecting...', false);
    
    const socket = new SockJS('http://localhost:8080/ws');
    stompClient = Stomp.over(socket);
    
    stompClient.debug = null;
    
    stompClient.connect({}, onConnected, onError);
}

function onConnected() {
    console.log('✅ WebSocket connected');
    updateConnectionStatus('Connected', true);
    
    // Subscribe to private messages
    stompClient.subscribe(`/user/queue/private`, function(message) {
        const messageData = JSON.parse(message.body);
        console.log('📨 Received private message:', messageData);
        
        // Only display if it's from our chat participant
        if (messageData.senderId == participantId || messageData.receiverId == participantId) {
            displayMessage(messageData);
            
            // Update conversation
            updateConversation(messageData.content);
        }
    });
    
    // Register for private messaging
    stompClient.send("/app/private.register", {}, JSON.stringify({
        username: currentUser
    }));
    
    console.log('✅ Subscribed to private messages');
    
    // Enable send button
    document.getElementById('sendBtn').disabled = false;
}

function onError(error) {
    console.error('❌ WebSocket error:', error);
    updateConnectionStatus('Disconnected', false);
    document.getElementById('sendBtn').disabled = true;
}

function updateConnectionStatus(text, isConnected) {
    const statusEl = document.getElementById('connectionStatus');
    statusEl.textContent = text;
    statusEl.className = 'connection-status ' + (isConnected ? 'connected' : '');
}

// ============================================================================
// MESSAGE HISTORY
// ============================================================================

async function loadMessageHistory() {
    try {
        const response = await fetch(`http://localhost:8080/api/private-messages/history?participantId=${participantId}&username=${encodeURIComponent(currentUser)}`);
        const data = await response.json();
        
        document.getElementById('loadingMessages').style.display = 'none';
        
        if (response.ok && data.success && data.messages.length > 0) {
            data.messages.forEach(msg => displayMessage(msg, false));
        }
    } catch (error) {
        console.error('Error loading message history:', error);
        document.getElementById('loadingMessages').textContent = 'Failed to load messages';
    }
}

// ============================================================================
// SEND MESSAGE
// ============================================================================

function sendMessage() {
    const input = document.getElementById('messageInput');
    const content = input.value.trim();
    
    if (!content || !stompClient || !stompClient.connected) {
        return;
    }
    
    const message = {
        content: content,
        receiverId: participantId
    };
    
    console.log('📤 Sending private message:', message);
    
    stompClient.send("/app/private.send", {}, JSON.stringify(message));
    
    input.value = '';
    input.focus();
}

function handleKeyPress(event) {
    if (event.key === 'Enter') {
        sendMessage();
    }
}

// ============================================================================
// DISPLAY MESSAGE
// ============================================================================

function displayMessage(message, scroll = true) {
    const container = document.getElementById('messagesContainer');
    const messageDiv = document.createElement('div');
    
    const isOwn = message.senderId == currentUser || message.sender === currentUser;
    
    messageDiv.className = `message ${isOwn ? 'own' : ''}`;
    
    const senderName = isOwn ? currentUser : participantUsername;
    const initials = senderName.substring(0, 2).toUpperCase();
    const time = formatTime(message.timestamp);
    
    messageDiv.innerHTML = `
        <div class="message-avatar">${initials}</div>
        <div class="message-content">
            <div class="message-header">
                <span class="message-sender">${senderName}</span>
                <span class="message-time">${time}</span>
            </div>
            <div class="message-bubble">${escapeHtml(message.content)}</div>
        </div>
    `;
    
    container.appendChild(messageDiv);
    
    if (scroll) {
        container.scrollTop = container.scrollHeight;
    }
}

function formatTime(timestamp) {
    if (!timestamp) return '';
    
    const date = new Date(timestamp);
    const now = new Date();
    
    if (date.toDateString() === now.toDateString()) {
        return date.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
    } else {
        return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' }) + ' ' +
               date.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
    }
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// ============================================================================
// UPDATE CONVERSATION
// ============================================================================

async function updateConversation(lastMessage) {
    try {
        await fetch('http://localhost:8080/api/conversations/update-private', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                username: currentUser,
                participantId: participantId,
                lastMessage: lastMessage
            })
        });
    } catch (error) {
        console.error('Error updating conversation:', error);
    }
}

// ============================================================================
// NAVIGATION
// ============================================================================

function goBack() {
    window.location.href = 'conversations.html';
}

// Cleanup on page unload
window.addEventListener('beforeunload', function() {
    if (stompClient && stompClient.connected) {
        stompClient.disconnect();
    }
});
