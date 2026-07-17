document.addEventListener('DOMContentLoaded', function() {
    // 初始化WebSocket连接
    let socket;
    try {
        socket = new WebSocket('ws://' + window.location.host + '/gameSync');

        socket.onmessage = function(event) {
            // 收到新数据时刷新页面
            refreshData();
        };
    } catch (e) {
        console.error('WebSocket连接失败:', e);
    }

    // 刷新数据按钮
    document.getElementById('refresh-btn').addEventListener('click', refreshData);

    // 筛选器变化时刷新数据
    document.getElementById('filter-type').addEventListener('change', refreshData);

    // 详情弹窗相关
    const detailModal = document.getElementById('detail-modal');
    document.getElementById('close-modal').addEventListener('click', () => {
        detailModal.style.display = 'none';
    });
    document.getElementById('close-detail').addEventListener('click', () => {
        detailModal.style.display = 'none';
    });

    // 点击模态框外部关闭
    window.addEventListener('click', (e) => {
        if (e.target === detailModal) {
            detailModal.style.display = 'none';
        }
    });

    // 初始化数据
    refreshData();

    // 刷新数据函数
    function refreshData() {
        fetch('/api/allData')
            .then(response => response.json())
            .then(data => {
                updateTable(data);
                updateStats(data);
            })
            .catch(error => console.error('获取数据失败:', error));
    }

    // 更新表格数据
    function updateTable(data) {
        const filterType = document.getElementById('filter-type').value;
        const tableBody = document.getElementById('data-table-body');
        tableBody.innerHTML = '';

        // 应用筛选
        let filteredData = data;
        if (filterType !== 'all') {
            filteredData = data.filter(item => item.result === filterType);
        }

        if (filteredData.length === 0) {
            tableBody.innerHTML = '<tr><td colspan="9" style="text-align: center;">暂无数据</td></tr>';
            return;
        }

        // 填充表格
        filteredData.forEach(item => {
            const row = document.createElement('tr');

            // 格式化日期时间
            const formattedTime = new Date(item.updateTime).toLocaleString();

            // 结果文本和样式
            let resultText, resultClass;
            switch(item.result) {
                case 'win':
                    resultText = '胜利';
                    resultClass = 'success';
                    break;
                case 'lose':
                    resultText = '失败';
                    resultClass = 'danger';
                    break;
                case 'giveup':
                    resultText = '放弃';
                    resultClass = 'warning';
                    break;
                default:
                    resultText = item.result;
                    resultClass = '';
            }

            row.innerHTML = `
                <td>${item.id}</td>
                <td>${item.clientId}</td>
                <td>${item.clientType}</td>
                <td>${item.level}</td>
                <td><span class="result-badge ${resultClass}">${resultText}</span></td>
                <td>${item.attempts}</td>
                <td>${item.score}</td>
                <td>${formattedTime}</td>
                <td>
                    <button class="action-btn view-btn" data-id="${item.id}">查看</button>
                </td>
            `;

            tableBody.appendChild(row);
        });

        // 为查看按钮添加事件
        document.querySelectorAll('.view-btn').forEach(btn => {
            btn.addEventListener('click', function() {
                const id = this.getAttribute('data-id');
                showDetail(data.find(item => item.id == id));
            });
        });
    }

    // 更新统计信息
    function updateStats(data) {
        if (data.length === 0) {
            document.getElementById('total-games').textContent = '0';
            document.getElementById('total-users').textContent = '0';
            document.getElementById('avg-score').textContent = '0';
            return;
        }

        // 总游戏次数
        document.getElementById('total-games').textContent = data.length;

        // 去重用户数
        const uniqueUsers = new Set(data.map(item => item.clientId));
        document.getElementById('total-users').textContent = uniqueUsers.size;

        // 平均得分
        const totalScore = data.reduce((sum, item) => sum + item.score, 0);
        const avgScore = Math.round(totalScore / data.length);
        document.getElementById('avg-score').textContent = avgScore;
    }

    // 显示详情
    function showDetail(item) {
        const detailContent = document.getElementById('detail-content');
        const formattedTime = new Date(item.updateTime).toLocaleString();

        detailContent.innerHTML = `
            <div class="detail-item">
                <strong>游戏ID:</strong> ${item.id}
            </div>
            <div class="detail-item">
                <strong>客户端ID:</strong> ${item.clientId}
            </div>
            <div class="detail-item">
                <strong>客户端类型:</strong> ${item.clientType}
            </div>
            <div class="detail-item">
                <strong>游戏类型:</strong> ${item.gameType || '猜数字游戏'}
            </div>
            <div class="detail-item">
                <strong>难度区间:</strong> ${item.level}
            </div>
            <div class="detail-item">
                <strong>游戏结果:</strong> 
                <span class="result-badge ${item.result === 'win' ? 'success' : (item.result === 'lose' ? 'danger' : 'warning')}">
                    ${item.result === 'win' ? '胜利' : (item.result === 'lose' ? '失败' : '放弃')}
                </span>
            </div>
            <div class="detail-item">
                <strong>尝试次数:</strong> ${item.attempts}
            </div>
            <div class="detail-item">
                <strong>得分:</strong> ${item.score}
            </div>
            <div class="detail-item">
                <strong>游戏时间:</strong> ${formattedTime}
            </div>
        `;

        document.getElementById('detail-modal').style.display = 'flex';
    }
});