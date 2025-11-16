// Team Chat Client - WebSocket communication for team-based messaging

// Global state
let stompClient = null;
let currentUser = null;
let currentTeam = null;
let teamMembers = [];

// ============================================================================
// AUTHENTICATION
// ============================================================================

function showLogin() {
    document.getElementById('loginFormContent').classList.remove('hidden');
    document.getElementById('registerFormContent').classList.add('hidden');
    clearAuthMessages();
}

function showRegister() {
    document.getElementById('loginFormContent').classList.add('hidden');
    document.getElementById('registerFormContent').classList.remove('hidden');
    clearAuthMessages();
}

function clearAuthMessages() {
    document.getElementById('authError').textContent = '';
    document.getElementById('authSuccess').textContent = '';
}

function clearTeamMessages() {
    document.getElementById('teamError').textContent = '';
    document.getElementById('teamSuccess').textContent = '';
}

async function register() {
    const username = document.getElementById('regUsername').value.trim();
    const password = document.getElementById('regPassword').value;

    if (!username || !password) {
        document.getElementById('authError').textContent = 'Please fill in all fields';
        return;
    }

    if (password.length < 3) {
        document.getElementById('authError').textContent = 'Password must be at least 3 characters';
        return;
    }

    try {
        const response = await fetch('http://localhost:8080/api/auth/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });

        const data = await response.json();

        if (response.ok) {
            document.getElementById('authSuccess').textContent = 'Account created! You can now login.';
            document.getElementById('authError').textContent = '';
            document.getElementById('regUsername').value = '';
            document.getElementById('regPassword').value = '';
            
            setTimeout(() => {
                showLogin();
                document.getElementById('username').value = username;
            }, 1500);
        } else {
            document.getElementById('authError').textContent = data.error || 'Registration failed';
            document.getElementById('authSuccess').textContent = '';
        }
    } catch (error) {
        document.getElementById('authError').textContent = 'Network error. Please try again.';
        console.error('Registration error:', error);
    }
}

async function login() {
    const username = document.getElementById('username').value.trim();
    const password = document.getElementById('password').value;

    if (!username || !password) {
        document.getElementById('authError').textContent = 'Please enter username and password';
        return;
    }

    try {
        const response = await fetch('http://localhost:8080/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });

        const data = await response.json();

        if (response.ok) {
            currentUser = username;
            document.getElementById('currentUsername').textContent = username;
            
            // Connect to WebSocket
            connectWebSocket();
            
            // Show team selection
            document.getElementById('loginScreen').style.display = 'none';
            document.getElementById('teamSelection').style.display = 'flex';
            
            // Load user's teams
            loadUserTeams();
        } else {
            document.getElementById('authError').textContent = data.error || 'Login failed';
        }
    } catch (error) {
        document.getElementById('authError').textContent = 'Network error. Please try again.';
        console.error('Login error:', error);
    }
}

// ============================================================================
// WEBSOCKET CONNECTION
// ============================================================================

function connectWebSocket() {
    console.log('🔌 Connecting to WebSocket...');
    updateConnectionStatus('Connecting...', false);
    
    const socket = new SockJS('http://localhost:8080/ws');
    stompClient = Stomp.over(socket);
    
    // Disable debug logging
    stompClient.debug = null;
    
    stompClient.connect({}, onConnected, onError);
}

function onConnected() {
    console.log('✅ WebSocket connected');
    updateConnectionStatus('Connected', true);
    
    // Register user for team messaging
    stompClient.send("/app/team.register", {}, JSON.stringify({
        username: currentUser
    }));
    
    console.log('📝 User registered for team messaging');
}

function onError(error) {
    console.error('❌ WebSocket error:', error);
    updateConnectionStatus('Disconnected', false);
}

function updateConnectionStatus(text, isConnected) {
    const statusEl = document.getElementById('connectionStatus');
    if (statusEl) {
        statusEl.textContent = text;
        statusEl.className = 'connection-status ' + (isConnected ? 'connected' : 'disconnected');
    }
}

// ============================================================================
// TEAM MANAGEMENT
// ============================================================================

async function loadUserTeams() {
    try {
        const response = await fetch(`http://localhost:8080/api/teams/user/${currentUser}`);
        const data = await response.json();

        if (response.ok && data.teams && data.teams.length > 0) {
            displayUserTeams(data.teams);
        } else {
            document.getElementById('myTeamsList').innerHTML = '<p style="color: #999; text-align: center;">No teams yet</p>';
        }
    } catch (error) {
        console.error('Error loading teams:', error);
    }
}

function displayUserTeams(teams) {
    const container = document.getElementById('myTeamsList');
    container.innerHTML = '';
    
    teams.forEach(team => {
        const teamDiv = document.createElement('div');
        teamDiv.className = 'team-item';
        teamDiv.onclick = () => enterTeam(team);
        teamDiv.innerHTML = `
            <h4>${team.name}</h4>
            <p>${team.memberCount} member${team.memberCount !== 1 ? 's' : ''}</p>
        `;
        container.appendChild(teamDiv);
    });
}

async function createTeam() {
    const teamName = document.getElementById('createTeamName').value.trim();

    if (!teamName) {
        document.getElementById('teamError').textContent = 'Please enter a team name';
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
            document.getElementById('teamSuccess').textContent = `Team "${teamName}" created!`;
            document.getElementById('teamError').textContent = '';
            document.getElementById('createTeamName').value = '';
            
            // Enter the newly created team
            setTimeout(() => {
                enterTeam(data.team);
            }, 1000);
        } else {
            document.getElementById('teamError').textContent = data.error || 'Failed to create team';
            document.getElementById('teamSuccess').textContent = '';
        }
    } catch (error) {
        document.getElementById('teamError').textContent = 'Network error. Please try again.';
        console.error('Create team error:', error);
    }
}

async function joinTeam() {
    const teamName = document.getElementById('joinTeamName').value.trim();

    if (!teamName) {
        document.getElementById('teamError').textContent = 'Please enter a team name';
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
            document.getElementById('teamSuccess').textContent = `Joined team "${teamName}"!`;
            document.getElementById('teamError').textContent = '';
            document.getElementById('joinTeamName').value = '';
            
            // Enter the team
            setTimeout(() => {
                enterTeam(data.team);
            }, 1000);
        } else {
            document.getElementById('teamError').textContent = data.error || 'Failed to join team';
            document.getElementById('teamSuccess').textContent = '';
        }
    } catch (error) {
        document.getElementById('teamError').textContent = 'Network error. Please try again.';
        console.error('Join team error:', error);
    }
}

async function enterTeam(team) {
    currentTeam = team;
    
    console.log('🏢 Entering team:', team.name, '(ID:', team.id, ')');
    
    // Update UI
    document.getElementById('currentTeamName').textContent = team.name;
    document.getElementById('teamSelection').style.display = 'none';
    document.getElementById('chatInterface').style.display = 'flex';
    
    // Subscribe to team messages
    subscribeToTeam(team.id);
    
    // Load team members
    await loadTeamMembers(team.id);
    
    // Notify server that user joined team
    stompClient.send("/app/team.join", {}, JSON.stringify({
        username: currentUser,
        teamId: team.id
    }));
    
    console.log('✅ Entered team successfully');
}

function subscribeToTeam(teamId) {
    // Subscribe to team messages
    const messageDestination = `/user/queue/team/${teamId}/messages`;
    stompClient.subscribe(messageDestination, function(message) {
        const messageData = JSON.parse(message.body);
        displayMessage(messageData);
    });
    
    console.log('📬 Subscribed to team messages:', messageDestination);
}

async function loadTeamMembers(teamId) {
    try {
        const response = await fetch(`http://localhost:8080/api/teams/${teamId}/members`);
        const data = await response.json();

        if (response.ok) {
            teamMembers = data.members;
            displayTeamMembers(data.members);
        }
    } catch (error) {
        console.error('Error loading team members:', error);
    }
}

function displayTeamMembers(members) {
    const container = document.getElementById('membersList');
    const memberCount = document.getElementById('memberCount');
    
    memberCount.textContent = members.length;
    container.innerHTML = '';
    
    members.forEach(member => {
        const memberDiv = document.createElement('div');
        memberDiv.className = 'member-item';
        memberDiv.innerHTML = `
            <div class="status-indicator ${member.online ? '' : 'offline'}"></div>
            <div>${member.username}</div>
        `;
        container.appendChild(memberDiv);
    });
}

function leaveTeam() {
    if (!currentTeam) return;
    
    // Notify server
    if (stompClient && stompClient.connected) {
        stompClient.send("/app/team.leave", {}, JSON.stringify({
            username: currentUser,
            teamId: currentTeam.id
        }));
    }
    
    // Clear UI
    document.getElementById('messagesContainer').innerHTML = '';
    currentTeam = null;
    teamMembers = [];
    
    // Show team selection
    document.getElementById('chatInterface').style.display = 'none';
    document.getElementById('teamSelection').style.display = 'flex';
    
    // Reload teams
    loadUserTeams();
}

// ============================================================================
// MESSAGING
// ============================================================================

function sendMessage() {
    const input = document.getElementById('messageInput');
    const content = input.value.trim();
    
    if (!content || !currentTeam || !stompClient || !stompClient.connected) {
        return;
    }
    
    const message = {
        sender: currentUser,
        teamId: currentTeam.id,
        content: content,
        type: 'CHAT'
    };
    
    stompClient.send("/app/team.send", {}, JSON.stringify(message));
    
    input.value = '';
    input.focus();
}

function handleKeyPress(event) {
    if (event.key === 'Enter') {
        sendMessage();
    }
}

function displayMessage(message) {
    const container = document.getElementById('messagesContainer');
    const messageDiv = document.createElement('div');
    
    const isOwn = message.sender === currentUser;
    const isSystem = message.type === 'JOIN' || message.type === 'LEAVE' || message.type === 'SYSTEM';
    
    if (isSystem) {
        messageDiv.className = 'message system';
        messageDiv.innerHTML = `
            <div class="message-content">
                <div class="message-bubble">${message.content}</div>
            </div>
        `;
    } else {
        messageDiv.className = `message ${isOwn ? 'own' : ''}`;
        
        const initials = message.sender.substring(0, 2).toUpperCase();
        const time = formatTime(message.timestamp);
        
        messageDiv.innerHTML = `
            <div class="message-avatar">${initials}</div>
            <div class="message-content">
                <div class="message-header">
                    <span class="message-sender">${message.sender}</span>
                    <span class="message-time">${time}</span>
                </div>
                <div class="message-bubble">${escapeHtml(message.content)}</div>
            </div>
        `;
    }
    
    container.appendChild(messageDiv);
    container.scrollTop = container.scrollHeight;
}

function formatTime(timestamp) {
    if (!timestamp) return '';
    
    const date = new Date(timestamp);
    const hours = date.getHours().toString().padStart(2, '0');
    const minutes = date.getMinutes().toString().padStart(2, '0');
    return `${hours}:${minutes}`;
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// ============================================================================
// LOGOUT
// ============================================================================

function logoutFromTeamSelection() {
    logout();
}

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
    
    // Reset state
    currentUser = null;
    currentTeam = null;
    teamMembers = [];
    
    // Show login screen
    document.getElementById('chatInterface').style.display = 'none';
    document.getElementById('teamSelection').style.display = 'none';
    document.getElementById('loginScreen').style.display = 'flex';
    
    // Clear forms
    document.getElementById('username').value = '';
    document.getElementById('password').value = '';
    clearAuthMessages();
}

// ============================================================================
// INITIALIZATION
// ============================================================================

document.addEventListener('DOMContentLoaded', () => {
    console.log('💬 Team Chat Client initialized');
    
    // Add Enter key support for login
    document.getElementById('password').addEventListener('keypress', (e) => {
        if (e.key === 'Enter') login();
    });
    
    document.getElementById('regPassword').addEventListener('keypress', (e) => {
        if (e.key === 'Enter') register();
    });
    
    document.getElementById('createTeamName').addEventListener('keypress', (e) => {
        if (e.key === 'Enter') createTeam();
    });
    
    document.getElementById('joinTeamName').addEventListener('keypress', (e) => {
        if (e.key === 'Enter') joinTeam();
    });
});
