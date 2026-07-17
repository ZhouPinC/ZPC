document.addEventListener('DOMContentLoaded', function() {
    const loginForm = document.getElementById('login-form');

    loginForm.addEventListener('submit', function(e) {
        const username = document.getElementById('username').value;
        const password = document.getElementById('password').value;

        // 简单验证
        if (!username || !password) {
            e.preventDefault();
            alert('请输入用户名和密码');
        }

    });
});