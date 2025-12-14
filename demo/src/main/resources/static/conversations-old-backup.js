// Unified Conversations Dashboard with Embedded Chat

let currentUser = null;
let searchTimeout = null;
let stompClient = null;
let currentChat = null; // {type: 'PRIVATE'|'TEAM', id, name, participantId, isOnline}
let currentTeamSubscription = null; // Track current team subscription to avoid duplicates

// ============================================================================
// INITIALIZATION
// ============================================================================

document.addEventListener('DOMContentLoaded', async function() {
    currentUser = sessionStorage.getItem('username');
    
    if (!currentUser) {
        window.location.href = 'team-chat-client.html';
        return;
    }
    
    document.getElementById('currentUsername').textContent = currentUser;
    
    // Get current user ID
    await fetchCurrentUserId();
    
    // Connect WebSocket
    connectWebSocket();
    
    // Load conversations
    loadConversations();
    
    // Setup click outside handlers
    setupClickOutsideHandlers();
});

// ============================================================================
// GET CURRENT USER ID
// ============================================================================

let currentUserId = null;

async function fetchCurrentUserId() {
    try {
        const response = await fetch(`http://localhost:8080/api/auth/user/${encodeURIComponent(currentUser)}`);
        const data = await response.json();
        if (response.ok && data.success) {
            currentUserId = data.user.id;
            console.log('Current user ID:', currentUserId);
        }
    } catch (error) {
        console.error('Error fetching user ID:', error);
    }
}

// ============================================================================
// WEBSOCKET CONNECTION
// ============================================================================

function connectWebSocket() {
    console.log('🔌 Connecting to WebSocket...');
    
    const socket = new SockJS('http://localhost:8080/ws');
    stompClient = Stomp.over(socket);
    stompClient.debug = null;
    
    stompClient.connect({}, onWebSocketConnected, onWebSocketError);
}

function onWebSocketConnected() {
    console.log('✅ WebSocket connected');
    
    // Subscribe to private messages
    stompClient.subscribe(`/user/queue/private`, function(message) {
        const messageData = JSON.parse(message.body);
        console.log('📨 Received private message:', messageData);
        handleIncomingPrivateMessage(messageData);
    });
    
    // Subscribe to user status updates (online/offline)
    stompClient.subscribe(`/topic/user.status`, function(message) {
        const statusData = JSON.parse(message.body);
        console.log('👤 User status changed:', statusData);
        handleUserStatusChange(statusData);
    });
    
    // Note: Team message subscriptions are created dynamically when joining teams
    // See subscribeToTeam() function
    
    // Register for private messaging
    stompClient.send("/app/private.register", {}, JSON.stringify({
        username: currentUser
    }));
    
    // Register for team messaging
    stompClient.send("/app/team.register", {}, JSON.stringify({
        username: currentUser
    }));
    
    console.log('✅ Subscribed to all message channels');
}

function onWebSocketError(error) {
    console.error('❌ WebSocket error:', error);
}

// ============================================================================
// HANDLE INCOMING MESSAGES
// ============================================================================

function handleIncomingPrivateMessage(messageData) {
    const senderId = Number(messageData.senderId);
    const receiverId = Number(messageData.receiverId);
    const myUserId = currentUserId || (currentChat && currentChat.myUserId);
    
    console.log('📨 Private message:', messageData.content.substring(0, 30), 'from:', senderId, 'to:', receiverId);
    console.log('   My user ID:', myUserId, 'Current chat:', currentChat?.type, 'Participant:', currentChat?.participantId);
    
    // If private chat is open and message belongs to this conversation
    if (currentChat && currentChat.type === 'PRIVATE' && currentChat.participantId && myUserId) {
        const participantId = Number(currentChat.participantId);
        
        // Message belongs if: from participant to me OR from me to participant
        if ((senderId === participantId && receiverId === myUserId) || 
            (senderId === myUserId && receiverId === participantId)) {
            console.log('✅ Message is for current chat - displaying');
            displayMessage(messageData);
        } else {
            console.log('ℹ️ Message is for different chat');
        }
    }
    
    // ALWAYS update conversations list to show latest message in sidebar
    console.log('🔄 Updating conversations list...');
    setTimeout(() => loadConversations(true), 100);
}

function handleIncomingTeamMessage(messageData) {
    console.log('📨 Team message:', messageData.content.substring(0, 30), 'for team:', messageData.teamId);
    console.log('   Current chat:', currentChat?.type, 'Team ID:', currentChat?.teamId);
    
    // If team chat is open and message is for this team
    if (currentChat && currentChat.type === 'TEAM' && Number(messageData.teamId) === Number(currentChat.teamId)) {
        console.log('✅ Message is for current team - displaying');
        displayMessage(messageData);
    } else {
        console.log('ℹ️ Message is for different team');
    }
    
    // ALWAYS update conversations list to show latest message in sidebar
    console.log('🔄 Updating conversations list...');
    setTimeout(() => loadConversations(true), 100);
}

function handleUserStatusChange(statusData) {
    // Update conversation list to reflect online/offline status
    loadConversations(true); // true = silent update, no loading indicators
    
    // If the user whose status changed is in the current open chat, update their status
    if (currentChat && currentChat.type === 'PRIVATE' && 
        currentChat.participantUsername === statusData.username) {
        currentChat.isOnline = statusData.online;
        const statusEl = document.getElementById('chatStatus');
        if (statusEl) {
            statusEl.textContent = statusData.online ? 'Online' : 'Offline';
            statusEl.className = statusData.online ? 'chat-status' : 'chat-status offline';
        }
    }
}

function subscribeToTeam(teamId) {
    if (!stompClient || !stompClient.connected) {
        console.error('WebSocket not connected, cannot subscribe to team');
        return;
    }
    
    // Unsubscribe from previous team if any
    if (currentTeamSubscription) {
        console.log('📡 Unsubscribing from previous team');
        currentTeamSubscription.unsubscribe();
        currentTeamSubscription = null;
    }
    
    const destination = `/user/queue/team/${teamId}/messages`;
    console.log('📡 Subscribing to team:', destination);
    
    currentTeamSubscription = stompClient.subscribe(destination, function(message) {
        const messageData = JSON.parse(message.body);
        console.log('📨 Received team message:', messageData);
        handleIncomingTeamMessage(messageData);
    });
}

// ============================================================================
// LOAD CONVERSATIONS
// ============================================================================

async function loadConversations(silent = false) {
    console.log('🔄 Loading conversations...', silent ? '(silent)' : '(visible)');
    try {
        const response = await fetch(`http://localhost:8080/api/conversations/user/${currentUser}`);
        const data = await response.json();
        
        console.log('📦 Received conversations:', data.conversations?.length || 0);
        
        if (!silent) {
            document.getElementById('loadingMessage').style.display = 'none';
        }
        
        if (response.ok && data.success) {
            displayConversations(data.conversations);
            console.log('✅ Conversations displayed');
        } else if (!silent) {
            document.getElementById('emptyState').style.display = 'block';
        }
    } catch (error) {
        console.error('Error loading conversations:', error);
        if (!silent) {
            document.getElementById('emptyState').style.display = 'block';
        }
    }
}

function displayConversations(conversations) {
    console.log('🎨 Displaying conversations:', conversations?.length || 0);
    const listEl = document.getElementById('conversationsList');
    const emptyEl = document.getElementById('emptyState');
    
    if (!conversations || conversations.length === 0) {
        emptyEl.style.display = 'block';
        listEl.innerHTML = '';
        return;
    }
    
    emptyEl.style.display = 'none';
    
    // Store scroll position
    const scrollTop = listEl.scrollTop;
    
    // Clear first
    listEl.innerHTML = '';
    
    // Add each conversation immediately without fragment (for real-time visibility)
    conversations.forEach(conv => {
        const convItem = createConversationElement(conv);
        listEl.appendChild(convItem);
    });
    
    // Force immediate visual update
    listEl.style.display = 'none';
    void listEl.offsetHeight; // Force reflow
    listEl.style.display = '';
    
    // Restore scroll position
    listEl.scrollTop = scrollTop;
    
    console.log('✅ DOM updated, conversations visible');
}

function createConversationElement(conv) {
    const div = document.createElement('div');
    div.className = 'conversation-item';
    if (currentChat && currentChat.id === conv.id) {
        div.classList.add('active');
    }
    div.onclick = () => openConversation(conv);
    
    const isTeam = conv.type === 'TEAM';
    const name = conv.name || (isTeam ? conv.teamName : conv.participantUsername);
    const lastMessage = conv.lastMessage || 'No messages yet';
    const unreadCount = conv.unreadCount || 0;
    
    let timeStr = '';
    if (conv.lastMessageTime) {
        const date = new Date(conv.lastMessageTime);
        const today = new Date();
        
        if (date.toDateString() === today.toDateString()) {
            timeStr = date.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
        } else {
            timeStr = date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
        }
    }
    
    let onlineIndicator = '';
    if (!isTeam && conv.participantOnline !== undefined) {
        onlineIndicator = `<div class="${conv.participantOnline ? 'online' : 'offline'}-indicator"></div>`;
    }
    
    const unreadBadge = unreadCount > 0 ? `<span class="unread-badge">${unreadCount}</span>` : '';
    
    div.innerHTML = `
        <div class="conversation-header">
            <div class="conversation-title">
                <span class="conversation-name">${escapeHtml(name)}</span>
                <span class="conversation-type ${isTeam ? 'team' : ''}">${isTeam ? 'TEAM' : '1-ON-1'}</span>
                ${onlineIndicator}
                ${unreadBadge}
            </div>
            <span class="conversation-time">${timeStr}</span>
        </div>
        <div class="conversation-preview">${escapeHtml(lastMessage)}</div>
    `;
    
    return div;
}

// Helper function to update conversation's last message in sidebar
async function updateConversationLastMessage(content) {
    if (!currentChat) return;
    
    try {
        // Send API request to update the conversation
        await fetch('http://localhost:8080/api/conversations/update-private', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                username: currentUser,
                participantId: currentChat.participantId,
                lastMessage: content.substring(0, 100)
            })
        });
        
        // Wait for conversations to reload
        await loadConversations(true);
    } catch (err) {
        console.error('Failed to update conversation:', err);
    }
}

// ============================================================================
// OPEN CONVERSATION
// ============================================================================

async function openConversation(conv) {
    console.log('Opening conversation:', conv);
    
    // Update active conversation in sidebar
    document.querySelectorAll('.conversation-item').forEach(item => {
        item.classList.remove('active');
    });
    event.currentTarget.classList.add('active');
    
    // Hide placeholder, show chat
    document.getElementById('chatPlaceholder').style.display = 'none';
    document.getElementById('chatContainer').classList.add('active');
    
    // Clear current chat
    document.getElementById('chatMessages').innerHTML = '<div class="loading">Loading messages...</div>';
    document.getElementById('sendBtn').disabled = true;
    
    const isTeam = conv.type === 'TEAM';
    const chatName = conv.name || (isTeam ? conv.teamName : conv.participantUsername);
    
    // Update header
    document.getElementById('chatName').textContent = chatName;
    
    // Set current chat info
    currentChat = {
        id: conv.id,
        type: conv.type,
        name: chatName,
        participantId: isTeam ? null : conv.participantId,
        participantUsername: isTeam ? null : conv.participantUsername,
        teamId: isTeam ? conv.teamId : null,
        teamName: isTeam ? conv.teamName : null,
        isOnline: isTeam ? true : (conv.participantOnline || false),
        myUserId: currentUserId // Set immediately so real-time messages work
    };
    
    // Update status
    const statusEl = document.getElementById('chatStatus');
    if (isTeam) {
        statusEl.textContent = 'Team Chat';
        statusEl.className = 'chat-status';
    } else {
        statusEl.textContent = currentChat.isOnline ? 'Online' : 'Offline';
        statusEl.className = currentChat.isOnline ? 'chat-status' : 'chat-status offline';
    }
    
    // Load messages
    if (isTeam) {
        await loadTeamMessages(conv.teamId);
    } else {
        await loadPrivateMessages(conv.participantId);
    }
    
    // Enable send button
    document.getElementById('sendBtn').disabled = false;
    document.getElementById('messageInput').focus();
}

// ============================================================================
// LOAD MESSAGES
// ============================================================================

async function loadPrivateMessages(participantId) {
    try {
        const response = await fetch(`http://localhost:8080/api/private-messages/history?participantId=${participantId}&username=${encodeURIComponent(currentUser)}`);
        const data = await response.json();
        
        document.getElementById('chatMessages').innerHTML = '';
        
        if (response.ok && data.success && data.messages.length > 0) {
            // Get current user ID from first message
            const firstMsg = data.messages[0];
            if (firstMsg.sender === currentUser) {
                currentChat.myUserId = Number(firstMsg.senderId);
            } else {
                currentChat.myUserId = Number(firstMsg.receiverId);
            }
            
            data.messages.forEach(msg => displayMessage(msg, false));
            scrollToBottom();
        } else {
            // No messages yet - use currentUserId from global
            currentChat.myUserId = currentUserId;
            document.getElementById('chatMessages').innerHTML = '<div class="empty-state">No messages yet. Start chatting!</div>';
        }
    } catch (error) {
        console.error('Error loading private messages:', error);
        document.getElementById('chatMessages').innerHTML = '<div class="error">Failed to load messages</div>';
    }
}

async function loadTeamMessages(teamId) {
    try {
        // Subscribe to team messages
        subscribeToTeam(teamId);
        
        // Join the team via WebSocket
        if (stompClient && stompClient.connected) {
            stompClient.send("/app/team.join", {}, JSON.stringify({
                teamId: teamId,
                username: currentUser
            }));
        }
        
        const response = await fetch(`http://localhost:8080/api/teams/${teamId}/messages`);
        const data = await response.json();
        
        document.getElementById('chatMessages').innerHTML = '';
        
        if (response.ok && data.success && data.messages.length > 0) {
            data.messages.forEach(msg => displayMessage(msg, false));
            scrollToBottom();
        } else {
            document.getElementById('chatMessages').innerHTML = '<div class="empty-state">No messages yet. Start chatting!</div>';
        }
    } catch (error) {
        console.error('Error loading team messages:', error);
        document.getElementById('chatMessages').innerHTML = '<div class="error">Failed to load messages</div>';
    }
}

// ============================================================================
// DISPLAY MESSAGE
// ============================================================================

function displayMessage(message, scroll = true) {
    console.log('💬 Displaying message:', message.content.substring(0, 30), 'sender:', message.sender || message.senderId);
    const container = document.getElementById('chatMessages');
    
    // Remove loading/empty states
    const loading = container.querySelector('.loading');
    const empty = container.querySelector('.empty-state');
    if (loading) loading.remove();
    if (empty) empty.remove();
    
    const messageDiv = document.createElement('div');
    
    let isOwn = false;
    let senderName = '';
    
    if (currentChat.type === 'PRIVATE') {
        const senderId = Number(message.senderId);
        const myUserId = currentChat.myUserId;
        isOwn = senderId === myUserId || message.sender === currentUser;
        senderName = isOwn ? currentUser : currentChat.participantUsername;
    } else {
        isOwn = message.sender === currentUser;
        senderName = message.sender;
    }
    
    messageDiv.className = `message ${isOwn ? 'own' : ''}`;
    
    const initials = senderName.substring(0, 2).toUpperCase();
    const time = formatTime(message.timestamp);
    
    messageDiv.innerHTML = `
        <div class="message-avatar">${initials}</div>
        <div class="message-content">
            <div class="message-header">
                <span class="message-sender">${escapeHtml(senderName)}</span>
                <span class="message-time">${time}</span>
            </div>
            <div class="message-bubble">${escapeHtml(message.content)}</div>
        </div>
    `;
    
    container.appendChild(messageDiv);
    
    // Force immediate visual update
    void messageDiv.offsetHeight;
    
    if (scroll) {
        scrollToBottom();
    }
    
    console.log('✅ Message displayed in chat');
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

function scrollToBottom() {
    const container = document.getElementById('chatMessages');
    container.scrollTop = container.scrollHeight;
}

// ============================================================================
// SEND MESSAGE
// ============================================================================

function sendMessage() {
    const input = document.getElementById('messageInput');
    const content = input.value.trim();
    
    if (!content || !stompClient || !stompClient.connected || !currentChat) {
        return;
    }
    
    if (currentChat.type === 'PRIVATE') {
        sendPrivateMessage(content);
    } else {
        sendTeamMessage(content);
    }
    
    input.value = '';
    input.focus();
}

function sendPrivateMessage(content) {
    const message = {
        username: currentUser,
        content: content,
        receiverId: currentChat.participantId
    };
    
    console.log('📤 Sending private message:', message);
    
    // Send to server - backend will echo back to both users
    stompClient.send("/app/private.send", {}, JSON.stringify(message));
    
    // Update conversation to show "last message" in sidebar immediately
    updateConversationLastMessage(content);
}

function sendTeamMessage(content) {
    const message = {
        username: currentUser,
        content: content,
        teamId: currentChat.teamId
    };
    
    console.log('📤 Sending team message:', message);
    
    // Send to server - backend will broadcast to all team members
    stompClient.send("/app/team.send", {}, JSON.stringify(message));
    
    // Update conversation to show "last message" in sidebar immediately
    updateConversationLastMessage(content);
}

function handleKeyPress(event) {
    if (event.key === 'Enter') {
        sendMessage();
    }
}

// ============================================================================
// USER SEARCH
// ============================================================================

function showSearchResults() {
    const input = document.getElementById('userSearch');
    const results = document.getElementById('searchResults');
    
    if (input.value.trim().length >= 2) {
        results.classList.add('show');
    }
}

async function searchUsers() {
    const query = document.getElementById('userSearch').value.trim();
    const resultsEl = document.getElementById('searchResults');
    
    if (query.length < 2) {
        resultsEl.classList.remove('show');
        return;
    }
    
    if (searchTimeout) {
        clearTimeout(searchTimeout);
    }
    
    searchTimeout = setTimeout(async () => {
        try {
            const response = await fetch(`http://localhost:8080/api/auth/search?query=${encodeURIComponent(query)}`);
            const data = await response.json();
            
            if (response.ok && data.success) {
                displaySearchResults(data.users);
            } else {
                resultsEl.innerHTML = '<div class="search-empty">No users found</div>';
                resultsEl.classList.add('show');
            }
        } catch (error) {
            console.error('Search error:', error);
            resultsEl.innerHTML = '<div class="search-empty">Search failed</div>';
            resultsEl.classList.add('show');
        }
    }, 300);
}

function displaySearchResults(users) {
    const resultsEl = document.getElementById('searchResults');
    
    const filteredUsers = users.filter(user => user.username !== currentUser);
    
    if (filteredUsers.length === 0) {
        resultsEl.innerHTML = '<div class="search-empty">No other users found</div>';
        resultsEl.classList.add('show');
        return;
    }
    
    resultsEl.innerHTML = '';
    filteredUsers.forEach(user => {
        const item = document.createElement('div');
        item.className = 'search-result-item';
        item.onclick = () => startPrivateChat(user);
        
        const onlineIndicator = user.online ? 
            '<div class="online-indicator-small"></div>' : 
            '<div class="offline-indicator-small"></div>';
        
        item.innerHTML = `
            ${onlineIndicator}
            <span class="username">${escapeHtml(user.username)}</span>
        `;
        
        resultsEl.appendChild(item);
    });
    
    resultsEl.classList.add('show');
}

async function startPrivateChat(user) {
    document.getElementById('searchResults').classList.remove('show');
    document.getElementById('userSearch').value = '';
    
    // Create/update conversation
    try {
        await fetch('http://localhost:8080/api/conversations/update-private', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                username: currentUser,
                participantId: user.id,
                lastMessage: ''
            })
        });
        
        // Reload conversations and wait for completion
        await loadConversations();
        
        // Use requestAnimationFrame to ensure DOM is ready
        await new Promise(resolve => requestAnimationFrame(() => {
            requestAnimationFrame(resolve);
        }));
        
        // Find and open the conversation
        const convItems = document.querySelectorAll('.conversation-item');
        for (const item of convItems) {
            if (item.textContent.includes(user.username)) {
                item.click();
                break;
            }
        }
    } catch (error) {
        console.error('Error starting private chat:', error);
    }
}

// ============================================================================
// TEAM MODAL
// ============================================================================

function showTeamModal() {
    document.getElementById('teamModal').classList.add('show');
    document.getElementById('teamError').style.display = 'none';
}

function closeTeamModal() {
    document.getElementById('teamModal').classList.remove('show');
    document.getElementById('joinTeamName').value = '';
    document.getElementById('createTeamName').value = '';
    document.getElementById('teamError').style.display = 'none';
}

async function joinTeam() {
    const teamName = document.getElementById('joinTeamName').value.trim();
    
    if (!teamName) {
        showTeamError('Please enter a team name');
        return;
    }
    
    try {
        const response = await fetch('http://localhost:8080/api/teams/join', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ teamName: teamName, username: currentUser })
        });
        
        const data = await response.json();
        
        if (response.ok && data.success) {
            closeTeamModal();
            // Sync conversation then reload
            await fetch(`http://localhost:8080/api/conversations/sync/${encodeURIComponent(currentUser)}`, { method: 'POST' });
            // Small delay to ensure backend sync completes
            setTimeout(async () => {
                await loadConversations();
            }, 300);
        } else {
            showTeamError(data.error || 'Failed to join team');
        }
    } catch (error) {
        showTeamError('Network error. Please try again.');
        console.error('Join team error:', error);
    }
}

async function createTeam() {
    const teamName = document.getElementById('createTeamName').value.trim();
    
    if (!teamName) {
        showTeamError('Please enter a team name');
        return;
    }
    
    try {
        const response = await fetch('http://localhost:8080/api/teams/create', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ teamName: teamName, username: currentUser })
        });
        
        const data = await response.json();
        
        if (response.ok && data.success) {
            closeTeamModal();
            // Sync conversation then reload
            await fetch(`http://localhost:8080/api/conversations/sync/${encodeURIComponent(currentUser)}`, { method: 'POST' });
            // Small delay to ensure backend sync completes
            setTimeout(async () => {
                await loadConversations();
            }, 300);
        } else {
            showTeamError(data.error || 'Failed to create team');
        }
    } catch (error) {
        showTeamError('Network error. Please try again.');
        console.error('Create team error:', error);
    }
}

function showTeamError(message) {
    const errorEl = document.getElementById('teamError');
    errorEl.textContent = message;
    errorEl.style.display = 'block';
}

// ============================================================================
// LOGOUT
// ============================================================================

async function logout() {
    try {
        await fetch('http://localhost:8080/api/auth/logout', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username: currentUser })
        });
    } catch (error) {
        console.error('Logout error:', error);
    }
    
    if (stompClient && stompClient.connected) {
        stompClient.disconnect();
    }
    
    sessionStorage.clear();
    window.location.href = 'team-chat-client.html';
}

// ============================================================================
// UTILITY FUNCTIONS
// ============================================================================

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function setupClickOutsideHandlers() {
    document.addEventListener('click', function(event) {
        const modal = document.getElementById('teamModal');
        if (event.target === modal) {
            closeTeamModal();
        }
        
        const searchContainer = document.querySelector('.search-container');
        const searchResults = document.getElementById('searchResults');
        if (!searchContainer.contains(event.target)) {
            searchResults.classList.remove('show');
        }
    });
}

// Cleanup on page unload
window.addEventListener('beforeunload', function() {
    if (stompClient && stompClient.connected) {
        stompClient.disconnect();
    }
});
