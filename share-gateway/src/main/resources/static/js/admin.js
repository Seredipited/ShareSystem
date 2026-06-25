let currentAdminTab = 'dashboard';
let storageUserId = null;

// ==================== 页面初始化 ====================
document.addEventListener('DOMContentLoaded', () => {
    const user = API.getCurrentUser();
    if (!user || user.role !== 1) {
        window.location.href = '/pages/login.html';
        return;
    }
    loadAdminStats();
    switchAdminTab('dashboard');
});

// ==================== 标签切换 ====================
async function switchAdminTab(tab) {
    currentAdminTab = tab;
    document.querySelectorAll('.nav-item[data-tab]').forEach(n => n.classList.remove('active'));
    const navItem = document.querySelector('.nav-item[data-tab="' + tab + '"]');
    if (navItem) navItem.classList.add('active');

    const titles = { dashboard: '仪表盘', users: '用户管理', files: '文件管理', shares: '分享管理', logs: '操作日志' };
    document.getElementById('tabTitle').textContent = titles[tab] || tab;
    document.getElementById('searchBox').style.display = (tab === 'users' || tab === 'files') ? '' : 'none';

    switch (tab) {
        case 'dashboard': loadAdminStats(); break;
        case 'users': loadUsers(); break;
        case 'files': loadAllFiles(); break;
        case 'shares': loadShares(); break;
        case 'logs': loadOperationLogs(); break;
    }
}

// ==================== 仪表盘 ====================
async function loadAdminStats() {
    const content = document.getElementById('adminContent');
    content.innerHTML = '<div class="loading-spinner" style="margin:40px auto;"></div>';

    try {
        const result = await API.admin.getStats();
        const stats = result.data || {};
        content.innerHTML = `
            <div class="stats-grid">
                <div class="stat-card">
                    <div class="stat-value">${stats.totalUsers || 0}</div>
                    <div class="stat-label">用户总数</div>
                </div>
                <div class="stat-card">
                    <div class="stat-value">${stats.totalFiles || 0}</div>
                    <div class="stat-label">文件总数</div>
                </div>
                <div class="stat-card">
                    <div class="stat-value">${formatSize(stats.totalSize || 0)}</div>
                    <div class="stat-label">存储总量</div>
                </div>
            </div>

            <div style="background:var(--bg-secondary);border-radius:var(--radius-md);padding:24px;border:1px solid var(--border-lighter);">
                <h3 style="margin-bottom:16px;">管理功能导航</h3>
                <div style="display:grid;grid-template-columns:repeat(auto-fit,minmax(180px,1fr));gap:12px;">
                    <div class="stat-card" style="cursor:pointer;" onclick="switchAdminTab('users')">
                        <div style="font-size:32px;margin-bottom:8px;">👥</div>
                        <div class="stat-label">用户管理 - 管理用户、分配空间</div>
                    </div>
                    <div class="stat-card" style="cursor:pointer;" onclick="switchAdminTab('files')">
                        <div style="font-size:32px;margin-bottom:8px;">📦</div>
                        <div class="stat-label">文件管理 - 查看和删除文件</div>
                    </div>
                    <div class="stat-card" style="cursor:pointer;" onclick="switchAdminTab('shares')">
                        <div style="font-size:32px;margin-bottom:8px;">🔗</div>
                        <div class="stat-label">分享管理 - 管理分享链接</div>
                    </div>
                    <div class="stat-card" style="cursor:pointer;" onclick="switchAdminTab('logs')">
                        <div style="font-size:32px;margin-bottom:8px;">📋</div>
                        <div class="stat-label">操作日志 - 查看系统操作记录</div>
                    </div>
                </div>
            </div>
        `;
    } catch (e) {
        content.innerHTML = '<div class="empty-state"><div class="empty-icon">⚠️</div><div class="empty-title">加载失败</div><div class="empty-desc">' + e.message + '</div></div>';
    }
}

// ==================== 用户管理 ====================
async function loadUsers() {
    const content = document.getElementById('adminContent');
    content.innerHTML = '<div class="loading-spinner" style="margin:40px auto;"></div>';

    try {
        const result = await API.admin.getUsers();
        const users = result.data || [];
        content.innerHTML = renderUserTable(users);
    } catch (e) {
        content.innerHTML = '<div class="empty-state"><div class="empty-icon">⚠️</div><div class="empty-title">加载失败</div><div class="empty-desc">' + e.message + '</div></div>';
    }
}

function renderUserTable(users) {
    return `
        <div style="background:var(--bg-secondary);border-radius:var(--radius-md);overflow:hidden;border:1px solid var(--border-lighter);">
            <table class="data-table">
                <thead><tr><th>ID</th><th>用户名</th><th>昵称</th><th>角色</th><th>已用空间</th><th>总空间</th><th>状态</th><th>注册时间</th><th>操作</th></tr></thead>
                <tbody>${users.map(u => `
                    <tr><td>${u.id}</td><td>${u.username}</td><td>${u.nickname||'-'}</td>
                    <td><span class="badge ${u.role===1?'badge-info':'badge-success'}">${u.role===1?'管理员':'普通用户'}</span></td>
                    <td>${formatSize(u.storageUsed||0)}</td><td>${formatSize(u.storageMax||0)}</td>
                    <td><span class="badge ${u.status===1?'badge-success':'badge-danger'}">${u.status===1?'正常':'禁用'}</span></td>
                    <td>${formatDate(u.createTime)}</td>
                    <td>
                        <button class="btn btn-sm ${u.status===1?'btn-danger':'btn-primary'}" onclick="toggleUserStatus(${u.id},${u.status===1?0:1})">${u.status===1?'禁用':'启用'}</button>
                        <button class="btn btn-secondary btn-sm" onclick="openStorageModal(${u.id})">分配空间</button>
                    </td></tr>
                `).join('')}</tbody>
            </table>
        </div>`;
}

async function toggleUserStatus(userId, status) {
    try {
        const result = await API.admin.updateUserStatus(userId, status);
        Toast.success(status === 1 ? '用户已启用' : '用户已禁用');
        loadUsers();
    } catch (e) { Toast.error(e.message); }
}

// ==================== 分配存储空间 ====================
function openStorageModal(userId) {
    storageUserId = userId;
    document.getElementById('storageUserId').textContent = userId;
    document.getElementById('storageSize').value = '';
    document.getElementById('storageUnit').value = '1073741824';
    openModal('storageModal');
}

async function confirmAllocateStorage() {
    const size = parseInt(document.getElementById('storageSize').value);
    const unit = parseInt(document.getElementById('storageUnit').value);
    if (!size || size < 1) { Toast.error('请输入有效的空间大小'); return; }
    try {
        await API.admin.allocateStorage(storageUserId, size * unit);
        Toast.success('空间分配成功');
        closeModal('storageModal');
        loadUsers();
    } catch (e) { Toast.error(e.message); }
}

// ==================== 文件管理 ====================
async function loadAllFiles() {
    const content = document.getElementById('adminContent');
    content.innerHTML = '<div class="loading-spinner" style="margin:40px auto;"></div>';
    try {
        const result = await API.admin.getAllFiles();
        const files = result.data || [];
        content.innerHTML = renderFileTable(files);
    } catch (e) {
        content.innerHTML = '<div class="empty-state"><div class="empty-icon">⚠️</div><div class="empty-title">加载失败</div><div class="empty-desc">' + e.message + '</div></div>';
    }
}

function renderFileTable(files) {
    return `
        <div style="background:var(--bg-secondary);border-radius:var(--radius-md);overflow:hidden;border:1px solid var(--border-lighter);">
            <table class="data-table">
                <thead><tr><th>ID</th><th>文件名</th><th>类型</th><th>大小</th><th>用户ID</th><th>分享</th><th>上传时间</th><th>操作</th></tr></thead>
                <tbody>${files.map(f => `
                    <tr><td>${f.id}</td><td>${f.isFolder===1?'📁':'📄'} ${f.fileName}</td>
                    <td>${f.isFolder===1?'文件夹':f.fileType||'-'}</td><td>${formatSize(f.fileSize||0)}</td><td>${f.userId}</td>
                    <td><span class="badge ${f.shareStatus===1?'badge-info':'badge-success'}">${f.shareStatus===1?'已分享':'未分享'}</span></td>
                    <td>${formatDate(f.createTime)}</td>
                    <td><button class="btn btn-danger btn-sm" onclick="deleteAdminFile(${f.id})">删除</button></td></tr>
                `).join('')}</tbody>
            </table>
        </div>`;
}

async function deleteAdminFile(fileId) {
    if (!confirm('确定要永久删除此文件吗？')) return;
    try {
        await API.admin.deleteFile(fileId);
        Toast.success('文件已删除');
        loadAllFiles();
    } catch (e) { Toast.error(e.message); }
}

// ==================== 分享管理 ====================
async function loadShares() {
    const content = document.getElementById('adminContent');
    content.innerHTML = '<div class="loading-spinner" style="margin:40px auto;"></div>';
    try {
        const result = await API.admin.getShares();
        const shares = result.data || [];
        content.innerHTML = renderShareTable(shares);
    } catch (e) {
        content.innerHTML = '<div class="empty-state"><div class="empty-icon">⚠️</div><div class="empty-title">加载失败</div><div class="empty-desc">' + e.message + '</div></div>';
    }
}

function renderShareTable(shares) {
    if (!shares.length) {
        return '<div class="empty-state"><div class="empty-icon">🔗</div><div class="empty-title">暂无分享</div><div class="empty-desc">用户还没有创建任何分享</div></div>';
    }
    return `
        <div style="background:var(--bg-secondary);border-radius:var(--radius-md);overflow:hidden;border:1px solid var(--border-lighter);">
            <table class="data-table">
                <thead><tr><th>ID</th><th>文件ID</th><th>用户ID</th><th>分享码</th><th>提取码</th><th>访问次数</th><th>过期时间</th><th>创建时间</th><th>操作</th></tr></thead>
                <tbody>${shares.map(s => `
                    <tr><td>${s.id}</td><td>${s.fileId}</td><td>${s.userId}</td>
                    <td><code>${s.shareCode}</code></td>
                    <td>${s.sharePwd ? '<code>' + s.sharePwd + '</code>' : '无'}</td>
                    <td>${s.viewCount||0} 次</td>
                    <td>${formatDate(s.expireTime)}</td>
                    <td>${formatDate(s.createTime)}</td>
                    <td><button class="btn btn-danger btn-sm" onclick="deleteAdminShare(${s.id})">删除分享</button></td></tr>
                `).join('')}</tbody>
            </table>
        </div>`;
}

async function deleteAdminShare(shareId) {
    if (!confirm('确定要删除此分享链接吗？删除后用户将无法访问。')) return;
    try {
        await API.admin.deleteShare(shareId);
        Toast.success('分享已删除');
        loadShares();
    } catch (e) { Toast.error(e.message); }
}

// ==================== 操作日志 ====================
async function loadOperationLogs() {
    const content = document.getElementById('adminContent');
    content.innerHTML = '<div class="loading-spinner" style="margin:40px auto;"></div>';
    try {
        const result = await API.admin.getLogs();
        const logs = result.data || [];
        content.innerHTML = renderLogTable(logs);
    } catch (e) {
        content.innerHTML = '<div class="empty-state"><div class="empty-icon">⚠️</div><div class="empty-title">加载失败</div><div class="empty-desc">' + e.message + '</div></div>';
    }
}

function renderLogTable(logs) {
    if (!logs.length) {
        return '<div class="empty-state"><div class="empty-icon">📋</div><div class="empty-title">暂无操作日志</div></div>';
    }
    return `
        <div style="background:var(--bg-secondary);border-radius:var(--radius-md);overflow:hidden;border:1px solid var(--border-lighter);">
            <table class="data-table">
                <thead><tr><th>ID</th><th>用户</th><th>操作</th><th>目标</th><th>详情</th><th>IP</th><th>时间</th></tr></thead>
                <tbody>${logs.map(l => `
                    <tr><td>${l.id}</td><td>${l.username||'-'}</td>
                    <td><span class="badge badge-info">${l.operation||'-'}</span></td>
                    <td>${l.target||'-'}</td><td>${l.detail||'-'}</td>
                    <td>${l.ip||'-'}</td><td>${formatDate(l.createTime)}</td></tr>
                `).join('')}</tbody>
            </table>
        </div>`;
}

// ==================== 搜索 ====================
function handleAdminSearch(keyword) {
    if (currentAdminTab === 'users') searchUsers(keyword);
    else if (currentAdminTab === 'files') searchFiles(keyword);
}

let adminSearchTimer;
async function searchUsers(keyword) {
    clearTimeout(adminSearchTimer);
    adminSearchTimer = setTimeout(async () => {
        if (!keyword || keyword.trim() === '') { loadUsers(); return; }
        try {
            const result = await API.admin.searchUsers(keyword.trim());
            const users = result.data || [];
            document.getElementById('adminContent').innerHTML = renderUserTable(users);
        } catch(e) {}
    }, 300);
}

async function searchFiles(keyword) {
    clearTimeout(adminSearchTimer);
    adminSearchTimer = setTimeout(async () => {
        if (!keyword || keyword.trim() === '') { loadAllFiles(); return; }
        try {
            const result = await API.admin.getAllFiles();
            const files = (result.data || []).filter(f =>
                f.fileName && f.fileName.includes(keyword.trim())
            );
            document.getElementById('adminContent').innerHTML = renderFileTable(files);
        } catch(e) {}
    }, 300);
}

// ==================== 工具函数 ====================
function openModal(id) { document.getElementById(id).classList.add('active'); }
function closeModal(id) { document.getElementById(id).classList.remove('active'); }

function toggleUserMenu() {
    if (confirm('确定要退出登录吗？')) {
        API.user.logout();
        window.location.href = '/pages/login.html';
    }
}

function formatSize(bytes) {
    if (!bytes || bytes === 0) return '0 B';
    const units = ['B', 'KB', 'MB', 'GB', 'TB'];
    const d = Math.floor(Math.log10(bytes) / Math.log10(1024));
    return (bytes / Math.pow(1024, d)).toFixed(1) + ' ' + units[d];
}

function formatDate(str) {
    if (!str) return '-';
    const d = new Date(str);
    return d.getFullYear() + '-' +
        String(d.getMonth()+1).padStart(2,'0') + '-' +
        String(d.getDate()).padStart(2,'0') + ' ' +
        String(d.getHours()).padStart(2,'0') + ':' +
        String(d.getMinutes()).padStart(2,'0');
}