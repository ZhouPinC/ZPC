// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', function() {
    // 后台管理按钮事件
    const adminBtn = document.getElementById('toAdmin');
    if (adminBtn) {
        adminBtn.addEventListener('click', function() {
            window.location.href = '/login';
        });
    }

    // 连接WebSocket同步游戏数据到服务器
    let socket;
    try {
        socket = new WebSocket('ws://' + window.location.host + '/gameSync');

        // 发送游戏数据到服务器
        function sendGameDataToServer(result) {
            if (socket && socket.readyState === WebSocket.OPEN) {
                const data = {
                    clientId: localStorage.getItem('gameClientId') || ('web_' + Math.random().toString(36).substr(2, 9)),
                    clientType: 'web',
                    gameType: 'guessNumber',
                    score: result.success ? 100 - result.attempts * 5 : 0,
                    level: `${settings.minValue}-${settings.maxValue}`,
                    result: result.success ? 'win' : (result.gaveUp ? 'giveup' : 'lose'),
                    attempts: result.attempts
                };

                // 保存客户端ID
                localStorage.setItem('gameClientId', data.clientId);

                socket.send(JSON.stringify(data));
            }
        }

        // 重写游戏结果保存函数，添加服务器同步
        const originalSaveGameResult = saveGameResult;
        saveGameResult = function(success, gaveUp = false) {
            originalSaveGameResult(success, gaveUp);
            sendGameDataToServer({
                success,
                gaveUp,
                attempts
            });
        };

    } catch (e) {
        console.error('WebSocket连接失败:', e);
    }
});