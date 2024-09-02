document.addEventListener('DOMContentLoaded', async () => {
    const loginForm = document.getElementById('login-form');
    const registerForm = document.getElementById('register-form');

    document.getElementById('show-register').addEventListener('click', (e) => {
        e.preventDefault();
        loginForm.style.display = 'none';
        registerForm.style.display = 'block';
    });

    document.getElementById('show-login').addEventListener('click', (e) => {
        e.preventDefault();
        registerForm.style.display = 'none';
        loginForm.style.display = 'block';
    });


    document.getElementById('loginForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const email = document.getElementById('login-email').value;
        const password = document.getElementById('login-password').value;
        await login(email, password);
    });


    document.getElementById('registerForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const username = document.getElementById('register-username').value;
        const email = document.getElementById('register-email').value;
        const password = document.getElementById('register-password').value;
        await register(username, email, password);
    });

    async function login(email, password) {
        try {
            const response = await fetch('api/1.0/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email, password })
            });
            const data = await response.json();
            if (response.ok) {
                window.location.href = 'chat.html';
            } else {
                alert(data.message);
            }
        } catch (error) {
            console.error('Error:', error);
        }
    }

    async function register(username, email, password) {
        try {
            const response = await fetch('api/1.0/register', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, email, password })
            });
            const data = await response.json();
            if (response.ok) {
                await login(email, password);
            } else {
                alert(data.message);
            }
        } catch (error) {
            console.error('Error:', error);
        }
    }
});
