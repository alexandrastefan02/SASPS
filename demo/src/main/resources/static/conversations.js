// ============================================================================
// REAL-TIME CONVERSATIONS DASHBOARD - COMPLETE REWRITE
// Event-driven architecture for instant UI updates
// ============================================================================

// ============================================================================
// STATE MANAGEMENT
// ============================================================================

const AppState = {
    currentUser: null,
    currentUserId: null,
    stompClient: null,
    currentChat: null,
    currentTeamSubscription: null,
    conversations: new Map(), // id -> conversation object
    searchTimeout: null,
    domReady: false
};

// ============================================================================
// EVENT BUS FOR REAL-TIME UPDATES
// ============================================================================

const EventBus = {
    listeners: {},
    
    on(event, callback) {
        if (!this.listeners[event]) {
            this.listeners[event] = [];
        }
        this.listeners[event].push(callback);
    },
    
    emit(event, data) {
        console.log(`🔔 Event: ${event}`, data);
        if (this.listeners[event]) {
            this.listeners[event].forEach(callback => callback(data));
        }
    }
};

// ============================================================================
// INITIALIZATION
// ============================================================================

document.addEventListener('DOMContentLoaded', async function() {
    AppState.currentUser = sessionStorage.getItem('username');
    
    if (!AppState.currentUser) {
        window.location.href = 'team-chat-client.html';
        return;
    }
    
    document.getElementById('currentUsername').textContent = AppState.currentUser;
    
    // Mark DOM as ready
    AppState.domReady = true;
    
    // Setup event listeners for real-time updates
    setupEventListeners();
    
    // Initialize components
    await init();
});

async function init() {
    console.log('🚀 Initializing application...');
    
    // Get current user ID
    await fetchCurrentUserId();
    
    // Connect WebSocket
    await connectWebSocket();
    
    // Load initial conversations
    await loadConversations();
    
    // Setup UI handlers
    setupClickOutsideHandlers();
    
    // Force initial render to ensure UI is up to date
    renderConversationList();
    
    console.log('✅ Application initialized');
}

// ============================================================================
// EVENT LISTENERS FOR REAL-TIME UPDATES
// ============================================================================

function setupEventListeners() {
    // When a new message arrives, update conversation list AND chat window
    EventBus.on('message:received', (message) => {
        console.log('📬 Processing received message:', message);
        console.log('   Current chat:', AppState.currentChat);
        
        updateConversationWithMessage(message);
        
        // Display message if it's for the current chat (no optimistic updates, display all messages)
        const shouldDisplay = isMessageForCurrentChat(message);
        
        if (shouldDisplay) {
            console.log('✅ Message is for current chat, displaying...');
            displayMessageInChat(message);
        } else {
            console.log('⚠️ Message is NOT for current chat');
        }
    });
    
    // When conversation list changes, re-render
    EventBus.on('conversations:updated', () => {
        console.log('📋 Rendering conversation list');
        renderConversationList();
    });
    
    // When user status changes, update UI
    EventBus.on('user:status', (statusData) => {
        console.log('👤 Updating user status');
        updateUserStatus(statusData);
    });
    
    // When new conversation is created
    EventBus.on('conversation:created', (conversation) => {
        console.log('➕ New conversation created');
        AppState.conversations.set(conversation.id, conversation);
        EventBus.emit('conversations:updated');
    });
}

// ============================================================================
// WEBSOCKET CONNECTION
// ============================================================================

async function connectWebSocket() {
    return new Promise((resolve, reject) => {
        console.log('🔌 Connecting to WebSocket...');
        
        // Try WebSocket-only first, with longer timeout
        const socket = new SockJS('http://localhost:8080/ws', null, {
            transports: ['websocket'],
            timeout: 10000  // 10 second timeout
        });
        AppState.stompClient = Stomp.over(socket);
        AppState.stompClient.debug = null;
        
        // Set heartbeat to keep connection alive
        AppState.stompClient.heartbeat.outgoing = 20000; // 20 seconds
        AppState.stompClient.heartbeat.incoming = 20000;
        
        // Connect with username in headers for User Principal authentication
        const connectHeaders = {
            username: AppState.currentUser
        };
        
        console.log('🔑 Connecting with username:', AppState.currentUser);
        
        AppState.stompClient.connect(connectHeaders, () => {
            console.log('✅ WebSocket connected');
            
            // Subscribe to private messages
            console.log('🔔 Subscribing to /user/queue/private for user:', AppState.currentUser);
            AppState.stompClient.subscribe(`/user/queue/private`, (message) => {
                console.log('📨 RAW WebSocket message received (private):', message);
                console.log('📨 Message body:', message.body);
                const messageData = JSON.parse(message.body);
                messageData.type = 'PRIVATE';
                console.log('📨 Parsed private message:', messageData);
                EventBus.emit('message:received', messageData);
            });
            
            // Team subscriptions are handled per-chat when opening a team
            // No need for global team subscriptions here
            
            // Subscribe to user status updates
            AppState.stompClient.subscribe(`/topic/user.status`, (message) => {
                const statusData = JSON.parse(message.body);
                EventBus.emit('user:status', statusData);
            });
            
            // Register for messaging
            AppState.stompClient.send("/app/private.register", {}, JSON.stringify({
                username: AppState.currentUser
            }));
            
            AppState.stompClient.send("/app/team.register", {}, JSON.stringify({
                username: AppState.currentUser
            }));
            
            console.log('✅ WebSocket subscriptions active');
            resolve();
        }, (error) => {
            console.error('❌ WebSocket connection failed:', error);
            reject(error);
        });
    });
}

async function fetchCurrentUserId() {
    try {
        const response = await fetch(`http://localhost:8080/api/auth/user/${encodeURIComponent(AppState.currentUser)}`);
        const data = await response.json();
        if (response.ok && data.success) {
            AppState.currentUserId = data.user.id;
            console.log('✅ User ID:', AppState.currentUserId);
        }
    } catch (error) {
        console.error('❌ Error fetching user ID:', error);
    }
}

// ============================================================================
// CONVERSATION MANAGEMENT
// ============================================================================

async function loadConversations(clearExisting = true) {
    try {
        console.log('📥 Loading conversations from server...');
        
        // Check if teams changed (from team page)
        if (localStorage.getItem('teamsChanged') === 'true') {
            console.log('   🔄 Teams changed detected, forcing refresh');
            clearExisting = true;
            localStorage.removeItem('teamsChanged');
        }
        
        const response = await fetch(`http://localhost:8080/api/conversations/user/${AppState.currentUser}`);
        const data = await response.json();
        
        if (response.ok && data.success) {
            console.log(`✅ Loaded ${data.conversations.length} conversations from server`);
            console.log('   Conversations data:', data.conversations);
            console.log('   Clear existing?', clearExisting);
            
            // Update state - clear only if requested (on initial load)
            if (clearExisting) {
                console.log('   🗑️ Clearing existing conversations');
                AppState.conversations.clear();
            } else {
                // Remove temporary conversations that have been replaced by real ones
                const tempIds = [];
                for (const [id, conv] of AppState.conversations) {
                    if (String(id).startsWith('temp-')) {
                        // Check if we have a real conversation for this participant
                        const hasReal = data.conversations.some(c => 
                            c.type === 'PRIVATE' && c.participantId === conv.participantId
                        );
                        if (hasReal) {
                            tempIds.push(id);
                        }
                    }
                }
                tempIds.forEach(id => {
                    console.log('   🗑️ Removing temporary conversation:', id);
                    AppState.conversations.delete(id);
                });
            }
            
            // Add or update each conversation
            data.conversations.forEach(conv => {
                console.log(`   ➕ Adding/updating conversation ID ${conv.id}:`, conv);
                AppState.conversations.set(conv.id, conv);
            });
            
            console.log('   Final AppState.conversations size:', AppState.conversations.size);
            
            // Trigger re-render
            EventBus.emit('conversations:updated');
            
            // Update UI elements if they exist
            const loadingEl = document.getElementById('loadingMessage');
            const emptyEl = document.getElementById('emptyState');
            if (loadingEl) loadingEl.style.display = 'none';
            if (emptyEl) emptyEl.style.display = data.conversations.length === 0 ? 'block' : 'none';
        }
    } catch (error) {
        console.error('❌ Error loading conversations:', error);
    }
}

function renderConversationList() {
    console.log('🎨 Rendering conversation list...');
    console.log('   AppState.conversations size:', AppState.conversations.size);
    console.log('   AppState.conversations:', Array.from(AppState.conversations.values()));
    const listEl = document.getElementById('conversationsList');
    const emptyEl = document.getElementById('emptyState');
    const loadingEl = document.getElementById('loadingMessage');
    
    // Check if DOM elements exist before proceeding
    if (!listEl) {
        console.warn('⚠️ conversationsList element not found');
        return;
    }
    
    // Hide loading message
    if (loadingEl) {
        loadingEl.style.display = 'none';
    }
    
    console.log('✅ DOM elements found, rendering...');
    
    const conversations = Array.from(AppState.conversations.values());
    
    // Remove existing conversation items (but keep emptyState and loadingMessage)
    const existingItems = listEl.querySelectorAll('.conversation-item');
    existingItems.forEach(item => item.remove());
    
    if (conversations.length === 0) {
        console.log('   ⚠️ No conversations to render!');
        if (emptyEl) {
            emptyEl.style.display = 'block';
        }
        return;
    }
    
    if (emptyEl) {
        emptyEl.style.display = 'none';
    }
    
    // Sort by last message time
    conversations.sort((a, b) => {
        const timeA = a.lastMessageTime ? new Date(a.lastMessageTime) : new Date(0);
        const timeB = b.lastMessageTime ? new Date(b.lastMessageTime) : new Date(0);
        return timeB - timeA;
    });
    
    // Render each conversation
    conversations.forEach(conv => {
        const convElement = createConversationElement(conv);
        listEl.appendChild(convElement);
    });
    
    console.log(`✅ Rendered ${conversations.length} conversations`);
}

function createConversationElement(conv) {
    const div = document.createElement('div');
    div.className = 'conversation-item';
    div.dataset.conversationId = conv.id;
    
    if (AppState.currentChat && AppState.currentChat.id === conv.id) {
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
                <span class="conversation-type ${isTeam ? 'team' : ''}">${isTeam ? 'TEAM' : 'PRIVATE'}</span>
                ${onlineIndicator}
                ${unreadBadge}
            </div>
            <span class="conversation-time">${timeStr}</span>
        </div>
        <div class="conversation-preview">${escapeHtml(lastMessage)}</div>
    `;
    
    return div;
}

async function updateConversationWithMessage(message) {
    console.log('📝 Updating conversation with new message', message);
    
    // Determine conversation based on message type
    let conversationId = null;
    let otherUserId = null;
    
    if (message.type === 'TEAM') {
        // Find team conversation
        console.log('   Looking for TEAM conversation with teamId:', message.teamId);
        for (const [id, conv] of AppState.conversations) {
            if (conv.type === 'TEAM' && String(conv.teamId) === String(message.teamId)) {
                conversationId = id;
                console.log('   Found existing team conversation:', id);
                break;
            }
        }
    } else if (message.type === 'PRIVATE') {
        // UUIDs are strings, compare them as strings
        const senderId = String(message.senderId);
        const receiverId = String(message.receiverId);
        const myUserId = String(AppState.currentUserId);
        otherUserId = senderId === myUserId ? receiverId : senderId;
        
        console.log('   Looking for PRIVATE conversation with user ID:', otherUserId);
        
        // Find existing private conversation
        for (const [id, conv] of AppState.conversations) {
            if (conv.type === 'PRIVATE' && String(conv.participantId) === otherUserId) {
                conversationId = id;
                console.log('   Found existing conversation:', id);
                break;
            }
        }
    }
    
    if (conversationId) {
        const conv = AppState.conversations.get(conversationId);
        conv.lastMessage = message.content.substring(0, 100);
        conv.lastMessageTime = message.timestamp || new Date().toISOString();
        
        // Update in map
        AppState.conversations.set(conversationId, conv);
        
        console.log('✅ Conversation updated, triggering re-render');
        
        // Trigger re-render
        EventBus.emit('conversations:updated');
    } else if (message.type === 'PRIVATE' && otherUserId) {
        // New private conversation - create it optimistically and reload from server
        console.log('🆕 New private conversation detected, creating optimistically...');
        
        // Create temporary conversation object
        const tempConv = {
            id: 'temp-' + Date.now(), // Temporary ID
            type: 'PRIVATE',
            participantId: otherUserId,
            participantUsername: message.senderUsername || 'User',
            participantOnline: true,
            lastMessage: message.content.substring(0, 100),
            lastMessageTime: message.timestamp || new Date().toISOString(),
            unreadCount: 1
        };
        
        // Add to state temporarily
        AppState.conversations.set(tempConv.id, tempConv);
        EventBus.emit('conversations:updated');
        
        // Reload from server to get real conversation (without clearing existing)
        console.log('   Reloading from server to get actual conversation...');
        await loadConversations(false);
    } else {
        // Team or unknown - reload from server
        console.log('🆕 New conversation detected, reloading from server...');
        await loadConversations(false);
    }
}

function updateUserStatus(statusData) {
    const { username, online } = statusData;
    
    // Update conversations with this user
    for (const [id, conv] of AppState.conversations) {
        if (conv.type === 'PRIVATE' && conv.participantUsername === username) {
            conv.participantOnline = online;
            AppState.conversations.set(id, conv);
        }
    }
    
    // Trigger re-render
    EventBus.emit('conversations:updated');
    
    // Update current chat header if applicable
    if (AppState.currentChat && AppState.currentChat.participantUsername === username) {
        AppState.currentChat.isOnline = online;
        updateChatHeader();
    }
}

// ============================================================================
// CHAT WINDOW
// ============================================================================

async function openConversation(conv) {
    console.log('💬 Opening conversation:', conv.name || conv.participantUsername);
    
    // Update active state in sidebar
    document.querySelectorAll('.conversation-item').forEach(item => {
        item.classList.remove('active');
    });
    const convElement = document.querySelector(`[data-conversation-id="${conv.id}"]`);
    if (convElement) {
        convElement.classList.add('active');
    }
    
    // Show chat container
    document.getElementById('chatPlaceholder').style.display = 'none';
    document.getElementById('chatContainer').classList.add('active');
    
    // Clear and show loading
    document.getElementById('chatMessages').innerHTML = '<div class="loading">Loading messages...</div>';
    document.getElementById('sendBtn').disabled = true;
    
    const isTeam = conv.type === 'TEAM';
    const chatName = conv.name || (isTeam ? conv.teamName : conv.participantUsername);
    
    // Update header
    document.getElementById('chatName').textContent = chatName;
    
    // Set current chat - use participantId from backend
    AppState.currentChat = {
        id: conv.id,
        type: conv.type,
        name: chatName,
        participantUserId: conv.participantId,
        participantUsername: conv.participantUsername,
        teamId: conv.teamId,
        isOnline: conv.participantOnline,
        myUserId: AppState.currentUserId
    };
    
    console.log('   Current chat opened with participant ID:', conv.participantId);
    
    updateChatHeader();
    
    // Subscribe to team messages if needed
    if (isTeam) {
        subscribeToTeam(conv.teamId);
    }
    
    // Load messages
    await loadMessages();
    
    // Enable send button
    document.getElementById('sendBtn').disabled = false;
    document.getElementById('messageInput').focus();
}

function updateChatHeader() {
    const headerEl = document.getElementById('chatName');
    if (!AppState.currentChat) return;
    
    if (AppState.currentChat.type === 'PRIVATE') {
        const statusIcon = AppState.currentChat.isOnline ? 
            '<span style="color: #10b981;">●</span>' : 
            '<span style="color: #6b7280;">●</span>';
        headerEl.innerHTML = `${AppState.currentChat.name} ${statusIcon}`;
    } else {
        headerEl.textContent = AppState.currentChat.name;
    }
}

async function loadMessages() {
    try {
        let response;
        
        if (AppState.currentChat.type === 'PRIVATE') {
            const url = `http://localhost:8080/api/private-messages/history?participantId=${AppState.currentChat.participantUserId}&username=${AppState.currentUser}`;
            console.log('📞 Fetching private messages from:', url);
            response = await fetch(url);
        } else {
            response = await fetch(`http://localhost:8080/api/teams/${AppState.currentChat.teamId}/messages`);
        }
        
        console.log('📥 Message API response status:', response.status, response.statusText);
        const responseText = await response.text();
        console.log('📄 Raw response:', responseText.substring(0, 200));
        const data = JSON.parse(responseText);
        
        if (response.ok) {
            const messages = data.messages || data;
            displayAllMessages(messages);
        } else {
            document.getElementById('chatMessages').innerHTML = 
                '<div class="empty-state">Failed to load messages</div>';
        }
    } catch (error) {
        console.error('❌ Error loading messages:', error);
        document.getElementById('chatMessages').innerHTML = 
            '<div class="empty-state">Error loading messages</div>';
    }
}

function displayAllMessages(messages) {
    const container = document.getElementById('chatMessages');
    container.innerHTML = '';
    
    if (!messages || messages.length === 0) {
        container.innerHTML = '<div class="empty-state">No messages yet. Start the conversation!</div>';
        return;
    }
    
    messages.forEach(message => displayMessageInChat(message, false));
    scrollToBottom();
}

function displayMessageInChat(message, scroll = true) {
    console.log('💬 Displaying message in chat');
    const container = document.getElementById('chatMessages');
    
    // Remove empty state
    const empty = container.querySelector('.empty-state');
    const loading = container.querySelector('.loading');
    if (empty) empty.remove();
    if (loading) loading.remove();
    
    // Check if message already exists (prevent duplicates)
    const messageId = message.id;
    if (messageId) {
        const existingMessage = container.querySelector(`[data-message-id="${messageId}"]`);
        if (existingMessage) {
            console.log('⏭️ Message already displayed (by ID), skipping:', messageId);
            return;
        }
    }
    
    // Additional duplicate check: same content + sender within 2 seconds
    // This handles cases where optimistic updates don't have IDs yet
    const timestamp = message.timestamp || new Date().toISOString();
    const messageTime = new Date(timestamp).getTime();
    const content = message.content;
    const sender = message.sender || message.senderId;
    
    const existingMessages = Array.from(container.querySelectorAll('.message'));
    for (const existingMsg of existingMessages) {
        const existingTimestamp = existingMsg.getAttribute('data-timestamp');
        const existingContent = existingMsg.getAttribute('data-content');
        const existingSender = existingMsg.getAttribute('data-sender');
        
        if (existingTimestamp && existingContent && existingSender) {
            const existingTime = new Date(existingTimestamp).getTime();
            const timeDiff = Math.abs(messageTime - existingTime);
            
            // If same sender, same content, and within 2 seconds - it's a duplicate
            if (existingSender === String(sender) && 
                existingContent === content && 
                timeDiff < 2000) {
                console.log('⏭️ Message already displayed (by content), skipping duplicate');
                return;
            }
        }
    }
    
    const messageDiv = document.createElement('div');
    
    let isOwn = false;
    let senderName = '';
    
    if (AppState.currentChat.type === 'PRIVATE') {
        // UUIDs are strings, compare them as strings
        const senderId = String(message.senderId);
        isOwn = senderId === String(AppState.currentUserId) || message.sender === AppState.currentUser;
        senderName = isOwn ? AppState.currentUser : AppState.currentChat.participantUsername;
    } else {
        isOwn = message.sender === AppState.currentUser;
        senderName = message.sender;
    }
    
    messageDiv.className = `message ${isOwn ? 'own' : ''}`;
    if (messageId) {
        messageDiv.setAttribute('data-message-id', messageId);
    }
    
    // Store metadata for duplicate detection and sorting
    messageDiv.setAttribute('data-timestamp', timestamp);
    messageDiv.setAttribute('data-content', content);
    messageDiv.setAttribute('data-sender', String(sender));
    
    const initials = senderName.substring(0, 2).toUpperCase();
    const time = formatTime(timestamp);
    
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
    
    // Insert message in correct chronological position
    const messages = Array.from(container.querySelectorAll('.message'));
    let inserted = false;
    
    for (let i = 0; i < messages.length; i++) {
        const existingTimestamp = messages[i].getAttribute('data-timestamp');
        if (existingTimestamp && timestamp < existingTimestamp) {
            container.insertBefore(messageDiv, messages[i]);
            inserted = true;
            console.log('📌 Message inserted at position', i, 'based on timestamp');
            break;
        }
    }
    
    // If not inserted (newest message), append at the end
    if (!inserted) {
        container.appendChild(messageDiv);
        console.log('📌 Message appended at the end (newest)');
    }
    
    if (scroll) {
        scrollToBottom();
    }
    
    console.log('✅ Message displayed');
}

function isMessageForCurrentChat(message) {
    if (!AppState.currentChat) {
        console.log('   No current chat open');
        return false;
    }
    
    if (message.type === 'PRIVATE' && AppState.currentChat.type === 'PRIVATE') {
        // UUIDs are strings, compare them as strings
        const senderId = String(message.senderId);
        const receiverId = String(message.receiverId);
        const participantId = String(AppState.currentChat.participantUserId);
        const myUserId = String(AppState.currentUserId);
        
        console.log('   📊 Checking if message for current chat:');
        console.log('      Message senderId:', senderId, 'receiverId:', receiverId);
        console.log('      Current chat participantUserId:', participantId);
        console.log('      My userId:', myUserId);
        
        // Message is for current chat if:
        // 1. It's FROM the participant TO me, OR
        // 2. It's FROM me TO the participant
        const isForMe = (senderId === participantId && receiverId === myUserId) ||
                        (senderId === myUserId && receiverId === participantId);
        
        console.log('      ✨ Result:', isForMe);
        return isForMe;
    }
    
    if (message.type === 'TEAM' && AppState.currentChat.type === 'TEAM') {
        // Chat ID is 'team-{teamId}', extract teamId from it
        const currentTeamId = AppState.currentChat.id.replace('team-', '');
        const messageTeamId = String(message.teamId);
        const matches = currentTeamId === messageTeamId;
        console.log('   📊 Team chat check: currentTeamId:', currentTeamId, 'messageTeamId:', messageTeamId, 'matches:', matches);
        return matches;
    }
    
    console.log('   Type mismatch or other issue');
    return false;
}

function subscribeToTeam(teamId) {
    if (AppState.currentTeamSubscription) {
        console.log('📡 Unsubscribing from previous team');
        AppState.currentTeamSubscription.unsubscribe();
    }
    
    const destination = `/user/queue/team/${teamId}/messages`;
    console.log('📡 Subscribing to team:', destination);
    
    AppState.currentTeamSubscription = AppState.stompClient.subscribe(destination, (message) => {
        const messageData = JSON.parse(message.body);
        messageData.type = 'TEAM';
        messageData.teamId = teamId;
        // Extract senderId from sender username by fetching user info
        // For now, use sender username as senderId for own message detection
        messageData.senderId = messageData.sender;
        console.log('📨 Team message received:', messageData);
        EventBus.emit('message:received', messageData);
    });
}

// ============================================================================
// SEND MESSAGE
// ============================================================================

function sendMessage() {
    const input = document.getElementById('messageInput');
    const content = input.value.trim();
    
    if (!content || !AppState.stompClient || !AppState.stompClient.connected || !AppState.currentChat) {
        return;
    }
    
    if (AppState.currentChat.type === 'PRIVATE') {
        sendPrivateMessage(content);
    } else {
        sendTeamMessage(content);
    }
    
    input.value = '';
    input.focus();
}

function sendPrivateMessage(content) {
    const message = {
        username: AppState.currentUser,
        content: content,
        receiverId: AppState.currentChat.participantUserId
    };
    
    console.log('📤 Sending private message to user ID:', AppState.currentChat.participantUserId);
    AppState.stompClient.send("/app/private.send", {}, JSON.stringify(message));
    
    // Note: Removed optimistic update - we'll wait for WebSocket echo
    // This prevents duplicate messages and ensures correct server timestamps
}

function sendTeamMessage(content) {
    const message = {
        username: AppState.currentUser,
        content: content,
        teamId: AppState.currentChat.teamId
    };
    
    console.log('📤 Sending team message');
    AppState.stompClient.send("/app/team.send", {}, JSON.stringify(message));
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
    
    if (AppState.searchTimeout) {
        clearTimeout(AppState.searchTimeout);
    }
    
    AppState.searchTimeout = setTimeout(async () => {
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
            console.error('❌ Search error:', error);
            resultsEl.innerHTML = '<div class="search-empty">Search failed</div>';
            resultsEl.classList.add('show');
        }
    }, 300);
}

function displaySearchResults(users) {
    const resultsEl = document.getElementById('searchResults');
    
    const filteredUsers = users.filter(user => user.username !== AppState.currentUser);
    
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
    console.log('🆕 Starting private chat with:', user.username, 'ID:', user.id);
    
    document.getElementById('searchResults').classList.remove('show');
    document.getElementById('userSearch').value = '';
    
    try {
        // Check if conversation already exists
        for (const [id, conv] of AppState.conversations) {
            if (conv.type === 'PRIVATE' && Number(conv.participantId) === user.id) {
                console.log('✅ Conversation already exists, opening it');
                openConversation(conv);
                return;
            }
        }
        
        console.log('➕ Creating new conversation on backend...');
        
        // Create conversation on backend
        const response = await fetch('http://localhost:8080/api/conversations/update-private', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                username: AppState.currentUser,
                participantId: user.id,
                lastMessage: ''
            })
        });
        
        if (!response.ok) {
            console.error('❌ Failed to create conversation');
            return;
        }
        
        const result = await response.json();
        console.log('✅ Conversation created on backend:', result);
        
        if (!result.conversationId) {
            console.error('❌ No conversationId returned from backend');
            return;
        }
        
        // Create conversation object with the ID from backend
        const newConversation = {
            id: result.conversationId,
            type: 'PRIVATE',
            participantId: user.id,
            participantUserId: user.id,
            participantUsername: user.username,
            participantOnline: user.online,
            name: user.username,
            lastMessage: '',
            lastMessageTime: new Date().toISOString(),
            unreadCount: 0
        };
        
        // Add to state
        AppState.conversations.set(newConversation.id, newConversation);
        console.log('✅ Conversation added to state with ID:', newConversation.id);
        
        // Open the conversation first
        openConversation(newConversation);
        
        // Then trigger UI update (render the sidebar)
        EventBus.emit('conversations:updated');
        
        console.log('✅ Conversation sidebar updated');
    } catch (error) {
        console.error('❌ Error starting chat:', error);
    }
}

// ============================================================================
// TEAM MANAGEMENT
// ============================================================================

function showCreateTeamModal() {
    document.getElementById('teamModal').style.display = 'flex';
    document.getElementById('createTeamName').value = '';
    document.getElementById('joinTeamName').value = '';
    document.getElementById('createTeamName').focus();
}

function closeTeamModal() {
    document.getElementById('teamModal').style.display = 'none';
    document.getElementById('teamError').style.display = 'none';
}

async function createTeam() {
    const teamName = document.getElementById('createTeamName').value.trim();
    
    if (!teamName) {
        alert('Please enter a team name');
        return;
    }
    
    try {
        const response = await fetch('http://localhost:8080/api/teams/create', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                teamName: teamName,
                username: AppState.currentUser
            })
        });
        
        const data = await response.json();
        
        if (response.ok && data.success) {
            console.log('✅ Team created:', teamName);
            closeTeamModal();
            await loadConversations();
        } else {
            const errorDiv = document.getElementById('teamError');
            errorDiv.textContent = data.message || 'Failed to create team';
            errorDiv.style.display = 'block';
        }
    } catch (error) {
        console.error('❌ Error creating team:', error);
        const errorDiv = document.getElementById('teamError');
        errorDiv.textContent = 'Error creating team';
        errorDiv.style.display = 'block';
    }
}

async function joinTeam() {
    const teamName = document.getElementById('joinTeamName').value.trim();
    
    if (!teamName) {
        const errorDiv = document.getElementById('teamError');
        errorDiv.textContent = 'Please enter a team name';
        errorDiv.style.display = 'block';
        return;
    }
    
    try {
        const response = await fetch('http://localhost:8080/api/teams/join', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                teamName: teamName,
                username: AppState.currentUser
            })
        });
        
        const data = await response.json();
        
        if (response.ok && data.success) {
            console.log('✅ Joined team:', teamName);
            closeTeamModal();
            await loadConversations();
        } else {
            const errorDiv = document.getElementById('teamError');
            errorDiv.textContent = data.message || 'Failed to join team';
            errorDiv.style.display = 'block';
        }
    } catch (error) {
        console.error('❌ Error joining team:', error);
        const errorDiv = document.getElementById('teamError');
        errorDiv.textContent = 'Error joining team';
        errorDiv.style.display = 'block';
    }
}

// ============================================================================
// UTILITY FUNCTIONS
// ============================================================================

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

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function setupClickOutsideHandlers() {
    document.addEventListener('click', function(event) {
        const searchResults = document.getElementById('searchResults');
        const searchInput = document.getElementById('userSearch');
        
        if (!searchResults.contains(event.target) && event.target !== searchInput) {
            searchResults.classList.remove('show');
        }
        
        const createTeamModal = document.getElementById('createTeamModal');
        if (event.target === createTeamModal) {
            hideCreateTeamModal();
        }
    });
}

function logout() {
    if (confirm('Are you sure you want to logout?')) {
        fetch('http://localhost:8080/api/auth/logout', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username: AppState.currentUser })
        }).finally(() => {
            sessionStorage.removeItem('username');
            window.location.href = 'team-chat-client.html';
        });
    }
}
