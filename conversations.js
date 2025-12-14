// Conversations Dashboard - Main entry point after login

let currentUser = null;
let searchTimeout = null;

// ============================================================================
// INITIALIZATION
// ============================================================================

document.addEventListener('DOMContentLoaded', function() {
    // Get username from session storage
    currentUser = sessionStorage.getItem('username');
    
    if (!currentUser) {
        // Redirect to login if not authenticated
        window.location.href = 'team-chat-client.html';
        return;
    }
    
    // Display username
    document.getElementById('currentUsername').textContent = currentUser;
    
    // Load conversations
    loadConversations();
});

// ============================================================================
// LOAD CONVERSATIONS
// ============================================================================

async function loadConversations() {
    try {
        showLoading();
        
        const response = await fetch(`http://localhost:8080/api/conversations/user/${currentUser}`);
        const data = await response.json();
        
        if (response.ok && data.success) {
            displayConversations(data.conversations);
        } else {
            showError(data.error || 'Failed to load conversations');
        }
    } catch (error) {
        console.error('Error loading conversations:', error);
        showError('Network error. Please check your connection.');
    }
}

function displayConversations(conversations) {
    const listEl = document.getElementById('conversationsList');
    const emptyEl = document.getElementById('emptyState');
    const loadingEl = document.getElementById('loadingMessage');
    
    loadingEl.style.display = 'none';
    
    if (!conversations || conversations.length === 0) {
        listEl.style.display = 'none';
        emptyEl.style.display = 'block';
        return;
    }
    
    listEl.style.display = 'flex';
    emptyEl.style.display = 'none';
    listEl.innerHTML = '';
    
    conversations.forEach(conv => {
        const convItem = createConversationElement(conv);
        listEl.appendChild(convItem);
    });
}

function createConversationElement(conv) {
    const div = document.createElement('div');
    div.className = 'conversation-item';
    div.onclick = () => openConversation(conv);
    
    const isTeam = conv.type === 'TEAM';
    const name = conv.name || (isTeam ? conv.teamName : conv.participantUsername);
    const lastMessage = conv.lastMessage || 'No messages yet';
    const unreadCount = conv.unreadCount || 0;
    
    // Format time
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
    
    // Online indicator for private chats
    let onlineIndicator = '';
    if (!isTeam && conv.participantOnline !== undefined) {
        onlineIndicator = `<div class="${conv.participantOnline ? 'online' : 'offline'}-indicator"></div>`;
    }
    
    div.innerHTML = `
        <div class="conversation-main">
            <div class="conversation-header">
                <div class="conversation-name">${name}</div>
                <span class="conversation-type ${isTeam ? 'team' : ''}">${isTeam ? '👥 Team' : '💬 Direct'}</span>
                ${onlineIndicator}
            </div>
            <div class="conversation-preview">${lastMessage}</div>
        </div>
        <div class="conversation-meta">
            <div class="conversation-time">${timeStr}</div>
            ${unreadCount > 0 ? `<div class="unread-badge">${unreadCount}</div>` : ''}
        </div>
    `;
    
    return div;
}

function openConversation(conv) {
    const isTeam = conv.type === 'TEAM';
    
    if (isTeam) {
        // Store team info and redirect to team chat
        sessionStorage.setItem('teamId', conv.teamId);
        sessionStorage.setItem('teamName', conv.teamName);
        window.location.href = 'team-chat-client.html?skipLogin=true';
    } else {
        // Open private chat
        sessionStorage.setItem('chatParticipantId', conv.participantId);
        sessionStorage.setItem('chatParticipantUsername', conv.participantUsername);
        window.location.href = 'private-chat.html';
    }
}

// ============================================================================
// TEAM MODAL
// ============================================================================

function showTeamModal() {
    document.getElementById('teamModal').classList.add('show');
    document.getElementById('joinTeamName').value = '';
    document.getElementById('createTeamName').value = '';
    document.getElementById('teamError').style.display = 'none';
}

function closeTeamModal() {
    document.getElementById('teamModal').classList.remove('show');
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
            body: JSON.stringify({ teamName, username: currentUser })
        });
        
        const data = await response.json();
        
        if (response.ok) {
            closeTeamModal();
            loadConversations(); // Reload to show new team
            
            // Optionally redirect to the team chat
            setTimeout(() => {
                sessionStorage.setItem('teamId', data.team.id);
                sessionStorage.setItem('teamName', data.team.name);
                window.location.href = 'team-chat-client.html?skipLogin=true';
            }, 500);
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
            body: JSON.stringify({ teamName, username: currentUser })
        });
        
        const data = await response.json();
        
        if (response.ok) {
            closeTeamModal();
            loadConversations(); // Reload to show new team
            
            // Redirect to the team chat
            setTimeout(() => {
                sessionStorage.setItem('teamId', data.team.id);
                sessionStorage.setItem('teamName', data.team.name);
                window.location.href = 'team-chat-client.html?skipLogin=true';
            }, 500);
        } else {
            showTeamError(data.error || 'Failed to create team');
        }
    } catch (error) {
        showTeamError('Network error. Please try again.');
        console.error('Create team error:', error);
    }
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
    
    sessionStorage.clear();
    window.location.href = 'team-chat-client.html';
}

// ============================================================================
// UI HELPERS
// ============================================================================

function showLoading() {
    document.getElementById('loadingMessage').style.display = 'block';
    document.getElementById('conversationsList').style.display = 'none';
    document.getElementById('emptyState').style.display = 'none';
    document.getElementById('errorMessage').style.display = 'none';
}

function showError(message) {
    document.getElementById('loadingMessage').style.display = 'none';
    document.getElementById('errorMessage').textContent = message;
    document.getElementById('errorMessage').style.display = 'block';
}

function showTeamError(message) {
    const errorEl = document.getElementById('teamError');
    errorEl.textContent = message;
    errorEl.style.display = 'block';
}

// Close modal when clicking outside
document.addEventListener('click', function(event) {
    const modal = document.getElementById('teamModal');
    if (event.target === modal) {
        closeTeamModal();
    }
    
    // Close search results when clicking outside
    const searchContainer = document.querySelector('.search-container');
    const searchResults = document.getElementById('searchResults');
    if (!searchContainer.contains(event.target)) {
        searchResults.classList.remove('show');
    }
});

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
    
    // Debounce search
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
    
    if (users.length === 0) {
        resultsEl.innerHTML = '<div class="search-empty">No users found</div>';
        resultsEl.classList.add('show');
        return;
    }
    
    // Filter out current user
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
            '<div class="online-indicator"></div>' : 
            '<div class="offline-indicator"></div>';
        
        item.innerHTML = `
            ${onlineIndicator}
            <span class="username">${user.username}</span>
        `;
        
        resultsEl.appendChild(item);
    });
    
    resultsEl.classList.add('show');
}

function startPrivateChat(user) {
    // Hide search results
    document.getElementById('searchResults').classList.remove('show');
    document.getElementById('userSearch').value = '';
    
    // Store chat info and redirect to private chat
    sessionStorage.setItem('chatParticipantId', user.id);
    sessionStorage.setItem('chatParticipantUsername', user.username);
    window.location.href = 'private-chat.html';
}
