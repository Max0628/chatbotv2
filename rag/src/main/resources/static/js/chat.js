document.addEventListener('DOMContentLoaded', async () => {
    const chatMessages = document.getElementById('chat-messages');
    const messageInput = document.getElementById('message-input');
    const sendButton = document.getElementById('send-button');
    const logoutButton = document.getElementById('logout-button');
    const presetButtonsContainer = document.getElementById('preset-buttons');
    const userInfoElement = document.getElementById('user-info');
    const loadingOverlay = document.getElementById('loading-overlay');

    let currentConversationId = null;
    let isComposing = false;

    const isLoggedIn = await checkLoginStatus();
    if (!isLoggedIn) {
        alert('您尚未登入！');
        window.location.href = 'index.html';
        return;
    }

    if (loadingOverlay) {
        loadingOverlay.style.display = 'none';
    }

    sendButton.addEventListener('click', sendMessage);
    logoutButton.addEventListener('click', logout);

    messageInput.addEventListener('compositionstart', () => {
        isComposing = true;
    });

    messageInput.addEventListener('compositionend', () => {
        isComposing = false;
    });

    messageInput.addEventListener('keydown', (event) => {
        if (event.key === 'Enter' && !isComposing) {
            event.preventDefault();
            presetButtonsContainer.classList.remove('d-flex');
            presetButtonsContainer.classList.add('d-none');
            sendMessage();
        }
    });

    document.querySelectorAll('.preset-btn').forEach(button => {
        button.addEventListener('click', () => {
            presetButtonsContainer.classList.remove('d-flex');
            presetButtonsContainer.classList.add('d-none');
            messageInput.value = button.getAttribute('data-message');
            sendMessage();
        });
    });

    async function checkLoginStatus() {
        try {
            const response = await fetch('api/1.0/userinfo', {
                method: 'GET',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include'
            });
            if (response.ok) {
                const data = await response.json();
                displayUserInfo(data);
                return true;
            } else {
                return false;
            }
        } catch (error) {
            console.error('Error checking login status:', error);
            return false;
        }
    }

    async function logout() {
        try {
            await fetch('api/1.0/logout', {
                method: 'POST',
                credentials: 'include'
            });
            window.location.href = 'index.html';
        } catch (error) {
            console.error('Error during logout:', error);
        }
    }

    function displayUserInfo(data) {
        userInfoElement.innerHTML = `
            <div>尊貴的 momo 會員：${data.username} 您好！</div>
            <div>信箱：${data.email}</div>
        `;
    }

    async function sendMessage() {
        const message = messageInput.value.trim();
        if (message) {
            addMessage(message, 'user-message');
            messageInput.value = '';
            try {
                const response = await fetch('api/1.0/chat', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ conversationId: currentConversationId, text: message })
                });
                const data = await response.json();


                if (!currentConversationId) {
                    currentConversationId = data.conversationId;
                }

                if (data && data.aiResponse) {
                    const aiResponseParsed = JSON.parse(data.aiResponse);
                    const aiMessageContent = marked.parse(aiResponseParsed.choices[0].message.content);
                    addMessage(aiMessageContent, 'bot-message');
                } else {
                    console.error('Unexpected response structure:', data);
                    addMessage('Sorry, something went wrong. Please try again later.', 'bot-message');
                }
            } catch (error) {
                console.error('Error:', error);
                addMessage('Sorry, something went wrong. Please try again later.', 'bot-message');
            }
        }
    }

    function addMessage(text, className) {
        const messageElement = document.createElement('div');
        presetButtonsContainer.classList.remove('d-flex');
        presetButtonsContainer.classList.add('d-none');
        messageElement.classList.add('message', className);
        messageElement.innerHTML = text;
        chatMessages.appendChild(messageElement);
        chatMessages.scrollTop = chatMessages.scrollHeight; //make the chatMessage scroll up to the page.
    }
});
