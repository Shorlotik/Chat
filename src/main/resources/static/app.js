const API_URL = 'http://localhost:8080/api';
const WS_URL = 'http://localhost:8080/ws'; // SockJS требует HTTP, не WS!

let token = localStorage.getItem('token');
let currentUser = JSON.parse(localStorage.getItem('currentUser') || 'null');
let currentChat = null;
let stompClient = null;
let chats = [];
let selectedMessage = null;
let replyToMessageId = null;

// Initialize
document.addEventListener('DOMContentLoaded', () => {
    console.log('🚀 App initialized');
    
    if (token && currentUser) {
        console.log('👤 User already logged in:', currentUser.username);
        showChatScreen();
        loadChats().then(() => {
            // Подключаемся после загрузки чатов
            setTimeout(() => connectWebSocket(), 300);
        });
    } else {
        showAuthScreen();
    }

    // Setup forms
    document.getElementById('loginForm').addEventListener('submit', handleLogin);
    document.getElementById('registerForm').addEventListener('submit', handleRegister);
    document.getElementById('newChatForm').addEventListener('submit', handleNewChat);
});

// Auth Functions
function switchTab(tab) {
    const loginForm = document.getElementById('loginForm');
    const registerForm = document.getElementById('registerForm');
    const tabs = document.querySelectorAll('.auth-tab');

    tabs.forEach(t => t.classList.remove('active'));

    if (tab === 'login') {
        loginForm.classList.remove('hidden');
        registerForm.classList.add('hidden');
        tabs[0].classList.add('active');
    } else {
        loginForm.classList.add('hidden');
        registerForm.classList.remove('hidden');
        tabs[1].classList.add('active');
    }
    hideError();
}

async function handleLogin(e) {
    e.preventDefault();
    const username = document.getElementById('loginUsername').value;
    const password = document.getElementById('loginPassword').value;

    try {
        const response = await fetch(`${API_URL}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });

        if (!response.ok) throw new Error('Неверные учетные данные');

        const data = await response.json();
        token = data.token;
        currentUser = { id: data.userId, username: data.username };
        
        localStorage.setItem('token', token);
        localStorage.setItem('currentUser', JSON.stringify(currentUser));

        showChatScreen();
        loadChats().then(() => {
            // Подключение к WebSocket после загрузки чатов
            setTimeout(() => connectWebSocket(), 300);
        });
    } catch (error) {
        showError(error.message);
    }
}

async function handleRegister(e) {
    e.preventDefault();
    const username = document.getElementById('registerUsername').value;
    const email = document.getElementById('registerEmail').value;
    const displayName = document.getElementById('registerDisplayName').value;
    const password = document.getElementById('registerPassword').value;

    try {
        const response = await fetch(`${API_URL}/auth/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, email, displayName, password })
        });

        if (!response.ok) throw new Error('Ошибка регистрации');

        const data = await response.json();
        token = data.token;
        currentUser = { id: data.userId, username: data.username };
        
        localStorage.setItem('token', token);
        localStorage.setItem('currentUser', JSON.stringify(currentUser));

        showChatScreen();
        loadChats().then(() => {
            // Подключение к WebSocket после загрузки чатов
            setTimeout(() => connectWebSocket(), 300);
        });
    } catch (error) {
        showError(error.message);
    }
}

function logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('currentUser');
    token = null;
    currentUser = null;
    if (stompClient) stompClient.disconnect();
    location.reload();
}

// UI Functions
function showAuthScreen() {
    document.getElementById('authScreen').style.display = 'flex';
    document.getElementById('chatScreen').classList.remove('active');
}

function showChatScreen() {
    document.getElementById('authScreen').style.display = 'none';
    document.getElementById('chatScreen').classList.add('active');
    document.getElementById('currentUserName').textContent = currentUser.username;
}

function showError(message) {
    const errorDiv = document.getElementById('errorMessage');
    errorDiv.textContent = message;
    errorDiv.classList.remove('hidden');
}

function hideError() {
    document.getElementById('errorMessage').classList.add('hidden');
}

// Chat Functions
async function loadChats() {
    try {
        const response = await fetch(`${API_URL}/chats`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (!response.ok) throw new Error('Не удалось загрузить чаты');

        chats = await response.json();
        console.log('📋 Loaded chats:', chats.length);
        renderChats();
        
        // Subscribe to chats after loading
        if (stompClient && stompClient.connected) {
            subscribeToChats();
        }
        
        return chats;
    } catch (error) {
        console.error('Error loading chats:', error);
        return [];
    }
}

function renderChats() {
    const chatList = document.getElementById('chatList');
    chatList.innerHTML = '';

    chats.forEach(chat => {
        const chatItem = document.createElement('div');
        chatItem.className = 'chat-item';
        if (currentChat && currentChat.id === chat.id) {
            chatItem.classList.add('active');
        }
        chatItem.innerHTML = `
            <div class="chat-name">${chat.name}</div>
            <div class="chat-type">${chat.type === 'PRIVATE' ? '👤 Личный' : '👥 Группа'}</div>
        `;
        chatItem.onclick = () => selectChat(chat);
        chatList.appendChild(chatItem);
    });
}

async function selectChat(chat) {
    currentChat = chat;
    renderChats();

    document.getElementById('emptyChatState').classList.add('hidden');
    document.getElementById('activeChatArea').classList.remove('hidden');
    document.getElementById('activeChatName').textContent = chat.name;

    await loadMessages(chat.id);
}

async function loadMessages(chatId) {
    try {
        const response = await fetch(`${API_URL}/messages/chat/${chatId}`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (!response.ok) throw new Error('Не удалось загрузить сообщения');

        const messages = await response.json();
        renderMessages(messages);
    } catch (error) {
        console.error('Error loading messages:', error);
    }
}

function renderMessages(messages) {
    const container = document.getElementById('messagesContainer');
    container.innerHTML = '';

    messages.reverse().forEach(message => {
        appendMessage(message);
    });

    container.scrollTop = container.scrollHeight;
}

function appendMessage(message) {
    const container = document.getElementById('messagesContainer');
    const messageDiv = document.createElement('div');
    messageDiv.className = 'message';
    messageDiv.dataset.messageId = message.id;
    messageDiv.dataset.senderId = message.senderId;
    
    if (message.senderId === currentUser.id) {
        messageDiv.classList.add('own');
    }
    
    if (message.isDeleted) {
        messageDiv.classList.add('message-deleted');
    }

    const time = new Date(message.sentAt).toLocaleTimeString('ru-RU', { 
        hour: '2-digit', 
        minute: '2-digit' 
    });
    
    let contentHTML = '';
    
    // Пересылка
    if (message.forwardedFromUser) {
        contentHTML += `<div class="message-forwarded">↗️ Переслано от ${message.forwardedFromUser}</div>`;
    }
    
    // Ответ на сообщение
    if (message.replyToMessage) {
        const replyText = message.replyToMessage.content || '[Вложение]';
        contentHTML += `
            <div class="message-reply" onclick="scrollToMessage(${message.replyToMessage.id})" style="cursor: pointer;">
                <div class="reply-author">${escapeHtml(message.replyToMessage.senderName)}</div>
                <div class="reply-text">${escapeHtml(replyText)}</div>
            </div>
        `;
    }
    
    // Контент сообщения
    const content = message.isDeleted ? 'Сообщение удалено' : linkifyText(message.content);
    contentHTML += `<div class="message-bubble">${content}`;
    
    // Метка "изменено"
    if (message.editedAt) {
        contentHTML += `<span class="message-edited">(изменено)</span>`;
    }
    
    contentHTML += `</div>`;
    
    // Вложения (файлы)
    if (message.attachments && message.attachments.length > 0) {
        message.attachments.forEach(attachment => {
            const icon = getFileIcon(attachment.mimeType);
            const size = formatFileSize(attachment.fileSize);
            
            // Если это изображение - показываем превью
            if (attachment.mimeType && attachment.mimeType.startsWith('image/')) {
                contentHTML += `
                    <div class="message-attachment message-image">
                        <a href="${attachment.fileUrl}" target="_blank">
                            <img src="${attachment.fileUrl}" alt="${attachment.fileName}" />
                        </a>
                    </div>
                `;
            } else {
                // Для остальных файлов - показываем иконку и имя
                contentHTML += `
                    <a href="${attachment.fileUrl}" target="_blank" class="message-attachment message-file" download>
                        <span class="file-icon">${icon}</span>
                        <div class="file-info">
                            <div class="file-name">${escapeHtml(attachment.fileName)}</div>
                            <div class="file-size">${size}</div>
                        </div>
                    </a>
                `;
            }
        });
    }
    
    contentHTML += `<div class="message-time">${time}</div>`;

    messageDiv.innerHTML = `
        ${message.senderId !== currentUser.id ? `<div class="message-sender">${message.senderName}</div>` : ''}
        ${contentHTML}
    `;

    container.appendChild(messageDiv);
    
    // Добавить обработчик контекстного меню после добавления в DOM
    messageDiv.addEventListener('contextmenu', (e) => {
        e.preventDefault();
        showContextMenu(e, message);
    });
    
    // Также добавить клик для мобильных устройств (долгое нажатие)
    let pressTimer;
    messageDiv.addEventListener('touchstart', (e) => {
        pressTimer = setTimeout(() => {
            showContextMenu(e.touches[0], message);
        }, 500);
    });
    messageDiv.addEventListener('touchend', () => {
        clearTimeout(pressTimer);
    });

    container.scrollTop = container.scrollHeight;
}

async function sendMessage() {
    const input = document.getElementById('messageInput');
    const content = input.value.trim();

    if (!content || !currentChat) return;

    try {
        const messageData = {
            chatId: currentChat.id,
            content: content,
            type: 'TEXT'
        };
        
        // Если отвечаем на сообщение
        if (replyToMessageId) {
            messageData.replyToMessageId = replyToMessageId;
            replyToMessageId = null; // Сбросить после отправки
        }
        
        const response = await fetch(`${API_URL}/messages`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify(messageData)
        });

        if (!response.ok) throw new Error('Не удалось отправить сообщение');

        // Не добавляем локально - сообщение придет через WebSocket
        input.value = '';
    } catch (error) {
        console.error('Error sending message:', error);
        alert('Не удалось отправить сообщение');
    }
}

function handleMessageKeyPress(event) {
    if (event.key === 'Enter') {
        sendMessage();
    }
}

// New Chat Modal
function showNewChatModal() {
    document.getElementById('newChatModal').classList.add('active');
}

function closeNewChatModal() {
    document.getElementById('newChatModal').classList.remove('active');
    document.getElementById('newChatForm').reset();
}

async function handleNewChat(e) {
    e.preventDefault();
    
    const name = document.getElementById('newChatName').value;
    const type = document.getElementById('newChatType').value;
    const membersStr = document.getElementById('newChatMembers').value;
    
    const memberIds = membersStr
        .split(',')
        .map(id => parseInt(id.trim()))
        .filter(id => !isNaN(id));

    try {
        const response = await fetch(`${API_URL}/chats`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({ name, type, memberIds })
        });

        if (!response.ok) throw new Error('Не удалось создать чат');

        closeNewChatModal();
        await loadChats();
    } catch (error) {
        console.error('Error creating chat:', error);
        alert('Не удалось создать чат');
    }
}

// WebSocket
function connectWebSocket() {
    console.log('🔌 Attempting to connect to WebSocket...');
    
    try {
    const socket = new SockJS(WS_URL);
    stompClient = Stomp.over(socket);
    
        // Отключить отладку STOMP (можно включить для детальной отладки)
        // stompClient.debug = null;
        
        stompClient.connect({}, () => {
            console.log('✅ WebSocket connected successfully!');
            subscribeToChats();
        }, (error) => {
            console.error('❌ WebSocket connection error:', error);
        });
    } catch (error) {
        console.error('❌ Failed to initialize WebSocket:', error);
    }
}

function subscribeToChats() {
    if (!stompClient || !stompClient.connected) {
        console.warn('⚠️ Cannot subscribe: WebSocket not connected');
        return;
    }
    
    console.log(`📡 Subscribing to ${chats.length} chats...`);
    
    // Subscribe to all user chats
        chats.forEach(chat => {
        console.log(`  📌 Subscribing to /topic/chat/${chat.id}`);
            stompClient.subscribe(`/topic/chat/${chat.id}`, (message) => {
                const newMessage = JSON.parse(message.body);
            console.log('📨 Received message via WebSocket:', newMessage);
            
            // Only append if we're viewing this chat
            if (currentChat && currentChat.id === newMessage.chatId) {
                // Check if message already exists to avoid duplicates
                const container = document.getElementById('messagesContainer');
                const existingMessage = Array.from(container.children).find(
                    el => el.dataset.messageId === String(newMessage.id)
                );
                
                if (existingMessage) {
                    // Update existing message (for edits/deletes)
                    console.log('🔄 Updating existing message:', newMessage.id);
                    existingMessage.remove();
                    appendMessage(newMessage);
                } else {
                    // Add new message
                    console.log('➕ Adding new message:', newMessage.id);
                    appendMessage(newMessage);
                }
            }
        });
    });
    console.log(`✅ Subscribed to ${chats.length} chats successfully`);
}

// Context Menu Functions
function showContextMenu(e, message) {
    selectedMessage = message;
    const menu = document.getElementById('contextMenu');
    menu.style.left = e.pageX + 'px';
    menu.style.top = e.pageY + 'px';
    menu.classList.add('active');
    
    // Скрыть меню при клике вне его
    document.addEventListener('click', hideContextMenu);
}

function hideContextMenu() {
    document.getElementById('contextMenu').classList.remove('active');
    document.removeEventListener('click', hideContextMenu);
}

function replyToMessage() {
    if (selectedMessage) {
        replyToMessageId = selectedMessage.id;
        const input = document.getElementById('messageInput');
        input.placeholder = `Ответ на: ${selectedMessage.content.substring(0, 30)}...`;
        input.focus();
    }
    hideContextMenu();
}

async function forwardMessage() {
    if (selectedMessage && currentChat) {
        try {
            const response = await fetch(`${API_URL}/messages`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify({
                    chatId: currentChat.id,
                    content: selectedMessage.content,
                    type: selectedMessage.type,
                    forwardFromMessageId: selectedMessage.id
                })
            });
            
            if (!response.ok) throw new Error('Не удалось переслать сообщение');
        } catch (error) {
            console.error('Error forwarding message:', error);
            alert('Не удалось переслать сообщение');
        }
    }
    hideContextMenu();
}

async function editMessage() {
    if (selectedMessage && selectedMessage.senderId === currentUser.id) {
        const newContent = prompt('Редактировать сообщение:', selectedMessage.content);
        if (newContent && newContent.trim()) {
            try {
                const response = await fetch(`${API_URL}/messages/${selectedMessage.id}`, {
                    method: 'PUT',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${token}`
                    },
                    body: JSON.stringify({ content: newContent })
                });
                
                if (!response.ok) throw new Error('Не удалось отредактировать сообщение');
            } catch (error) {
                console.error('Error editing message:', error);
                alert('Не удалось отредактировать сообщение');
            }
        }
    }
    hideContextMenu();
}

async function deleteMessage() {
    if (selectedMessage && selectedMessage.senderId === currentUser.id) {
        if (confirm('Удалить это сообщение?')) {
            try {
                const response = await fetch(`${API_URL}/messages/${selectedMessage.id}`, {
                    method: 'DELETE',
                    headers: {
                        'Authorization': `Bearer ${token}`
                    }
                });
                
                if (!response.ok) throw new Error('Не удалось удалить сообщение');
            } catch (error) {
                console.error('Error deleting message:', error);
                alert('Не удалось удалить сообщение');
            }
        }
    }
    hideContextMenu();
}

// File Functions
function showAttachMenu() {
    document.getElementById('fileInput').click();
}

async function handleFileSelect(event) {
    const file = event.target.files[0];
    if (!file || !currentChat) return;
    
    // Сначала создаем сообщение
    try {
        const response = await fetch(`${API_URL}/messages`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({
                chatId: currentChat.id,
                content: `📎 ${file.name}`,
                type: determineFileType(file)
            })
        });
        
        if (!response.ok) throw new Error('Не удалось отправить файл');
        
        const message = await response.json();
        
        // Загружаем файл
        const formData = new FormData();
        formData.append('file', file);
        formData.append('messageId', message.id);
        
        const uploadResponse = await fetch(`${API_URL}/files/upload`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`
            },
            body: formData
        });
        
        if (!uploadResponse.ok) throw new Error('Не удалось загрузить файл');
        
        // Сообщение с вложением придет через WebSocket
        console.log('✅ File uploaded successfully');
        
        event.target.value = ''; // Сбросить input
    } catch (error) {
        console.error('Error uploading file:', error);
        alert('Не удалось загрузить файл');
    }
}

function determineFileType(file) {
    const type = file.type;
    if (type.startsWith('image/')) {
        if (type === 'image/gif') return 'GIF';
        return 'IMAGE';
    } else if (type.startsWith('video/')) {
        return 'VIDEO';
    } else if (type.startsWith('audio/')) {
        return 'VOICE';
    }
    return 'DOCUMENT';
}

// Scroll to message
function scrollToMessage(messageId) {
    const messageElement = document.querySelector(`[data-message-id="${messageId}"]`);
    if (messageElement) {
        messageElement.scrollIntoView({ behavior: 'smooth', block: 'center' });
        
        // Подсветка сообщения
        messageElement.style.transition = 'background-color 0.3s';
        const originalBg = messageElement.style.backgroundColor;
        messageElement.style.backgroundColor = 'rgba(102, 126, 234, 0.2)';
        
        setTimeout(() => {
            messageElement.style.backgroundColor = originalBg;
        }, 1500);
    }
}

// Utilities
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// Преобразование текста с ссылками в HTML
function linkifyText(text) {
    if (!text) return '';
    
    // Escape HTML сначала
    text = escapeHtml(text);
    
    // Регулярное выражение для поиска URL
    const urlPattern = /(\b(https?|ftp):\/\/[-A-Z0-9+&@#\/%?=~_|!:,.;]*[-A-Z0-9+&@#\/%=~_|])/gim;
    
    // Заменяем URL на кликабельные ссылки
    return text.replace(urlPattern, '<a href="$1" target="_blank" rel="noopener noreferrer" style="color: inherit; text-decoration: underline;">$1</a>');
}

// Получить иконку для типа файла
function getFileIcon(mimeType) {
    if (!mimeType) return '📄';
    
    if (mimeType.startsWith('image/')) return '🖼️';
    if (mimeType.startsWith('video/')) return '🎬';
    if (mimeType.startsWith('audio/')) return '🎵';
    if (mimeType.includes('pdf')) return '📕';
    if (mimeType.includes('word') || mimeType.includes('document')) return '📝';
    if (mimeType.includes('excel') || mimeType.includes('spreadsheet')) return '📊';
    if (mimeType.includes('zip') || mimeType.includes('rar') || mimeType.includes('archive')) return '📦';
    
    return '📄';
}

// Форматировать размер файла
function formatFileSize(bytes) {
    if (!bytes) return '0 B';
    
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    
    return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i];
}

// WebSocket libraries are now loaded via HTML <script> tags




