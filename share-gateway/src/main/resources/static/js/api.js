/**
 * 学习资料分享系统 - API 请求模块
 * 直接访问后端服务（绕过网关）
 */

const API = {
    // 服务地址
    userServiceURL: 'http://localhost:8181',
    fileServiceURL: 'http://localhost:8182',

    /** 从 localStorage 获取 token */
    getToken() { return localStorage.getItem('share_token'); },

    /** 保存 token */
    setToken(token) { localStorage.setItem('share_token', token); },

    /** 清除 token */
    clearToken() { localStorage.removeItem('share_token'); localStorage.removeItem('share_user'); },

    /** 获取当前用户信息 */
    getCurrentUser() { const raw = localStorage.getItem('share_user'); return raw ? JSON.parse(raw) : null; },

    /** 保存当前用户 */
    setCurrentUser(user) { localStorage.setItem('share_user', JSON.stringify(user)); },

    /** 通用请求方法 */
    async request(base, path, options = {}) {
        const token = this.getToken();
        const headers = { ...options.headers };
        if (!(options.body instanceof FormData)) {
            headers['Content-Type'] = headers['Content-Type'] || 'application/json';
        }
        if (token) headers['Authorization'] = 'Bearer ' + token;

        const config = { ...options, headers };
        const response = await fetch(base + path, config);

        if (response.status === 401) {
            this.clearToken();
            window.location.href = '/pages/login.html';
            throw new Error('未登录，请先登录');
        }

        const ct = response.headers.get('Content-Type') || '';
        if (ct.includes('application/octet-stream') || ct.includes('image/') ||
            ct.includes('video/') || ct.includes('audio/') || ct.includes('application/pdf')) {
            return response;
        }

        const data = await response.json();
        if (data.code && data.code !== 200) {
            throw new Error(data.message || '请求失败');
        }
        return data;
    },

    // 用户服务
    userReq(path, opts) { return this.request(this.userServiceURL, path, opts); },

    // 文件服务
    fileReq(path, opts) { return this.request(this.fileServiceURL, path, opts); },

    // ==================== 用户相关 ====================
    user: {
        register(data) { return API.userReq('/api/user/register', { method: 'POST', body: JSON.stringify(data) }); },
        async login(data) {
            const result = await API.userReq('/api/user/login', { method: 'POST', body: JSON.stringify(data) });
            if (result.code === 200 && result.data) {
                if (result.data.token) API.setToken(result.data.token);
                if (result.data.user) API.setCurrentUser(result.data.user);
            }
            return result;
        },
        logout() { API.clearToken(); return Promise.resolve({ code: 200 }); },
        getCurrent() { return API.userReq('/api/user/current'); },
        getQQLoginUrl() { return API.userReq('/api/user/qq/login'); },
        changePassword(oldPassword, newPassword) {
            return API.userReq('/api/user/changePassword?oldPassword=' + encodeURIComponent(oldPassword) +
                '&newPassword=' + encodeURIComponent(newPassword), { method: 'POST' });
        },
        updateProfile(data) { return API.userReq('/api/user/profile', { method: 'PUT', body: JSON.stringify(data) }); },
        getStorageInfo() { return API.userReq('/api/user/storage'); },
        uploadAvatar(file) {
            const fd = new FormData(); fd.append('file', file);
            return API.userReq('/api/user/avatar', { method: 'POST', body: fd });
        }
    },

    // ==================== 文件相关 ====================
    file: {
        getList(parentId) { return API.fileReq('/api/file/list?parentId=' + (parentId || 0)); },
        createFolder(folderName, parentId) {
            return API.fileReq('/api/file/folder?folderName=' + encodeURIComponent(folderName) +
                '&parentId=' + (parentId || 0), { method: 'POST' });
        },
        upload(file, parentId) {
            const fd = new FormData(); fd.append('file', file); fd.append('parentId', parentId || 0);
            return API.fileReq('/api/file/upload?parentId=' + (parentId || 0), { method: 'POST', body: fd });
        },
        checkInstant(fileName, fileMd5, parentId) {
            return API.fileReq('/api/file/checkInstant?fileName=' + encodeURIComponent(fileName) +
                '&fileMd5=' + fileMd5 + '&parentId=' + (parentId || 0), { method: 'POST' });
        },
        uploadChunk(chunk, fileMd5, chunkIndex, totalChunks, fileName) {
            const fd = new FormData(); fd.append('file', chunk); fd.append('fileMd5', fileMd5);
            fd.append('chunkIndex', chunkIndex); fd.append('totalChunks', totalChunks); fd.append('fileName', fileName);
            return API.fileReq('/api/file/chunk', { method: 'POST', body: fd });
        },
        checkChunkProgress(fileMd5) { return API.fileReq('/api/file/chunkProgress?fileMd5=' + fileMd5); },
        mergeChunks(fileMd5, fileName, parentId) {
            return API.fileReq('/api/file/merge?fileMd5=' + fileMd5 + '&fileName=' + encodeURIComponent(fileName) +
                '&parentId=' + (parentId || 0), { method: 'POST' });
        },
        rename(fileId, newName) {
            return API.fileReq('/api/file/rename/' + fileId + '?newName=' + encodeURIComponent(newName), { method: 'PUT' });
        },
        move(fileId, targetParentId) {
            return API.fileReq('/api/file/move/' + fileId + '?targetParentId=' + targetParentId, { method: 'PUT' });
        },
        delete(fileId) { return API.fileReq('/api/file/' + fileId, { method: 'DELETE' }); },
        batchDelete(fileIds) { return API.fileReq('/api/file/batchDelete', { method: 'POST', body: JSON.stringify(fileIds) }); },
        restore(fileId) { return API.fileReq('/api/file/restore/' + fileId, { method: 'PUT' }); },
        permanentDelete(fileId) { return API.fileReq('/api/file/permanent/' + fileId, { method: 'DELETE' }); },
        getRecycleBin() { return API.fileReq('/api/file/recycle'); },
        clearRecycleBin() { return API.fileReq('/api/file/recycle/clear', { method: 'DELETE' }); },
        getSharedFiles() { return API.fileReq('/api/file/shared'); },
        search(keyword) { return API.fileReq('/api/file/search?keyword=' + encodeURIComponent(keyword)); },
        getDownloadUrl(fileId) { return 'http://localhost:8182/api/file/download/' + fileId; },
        getPreviewUrl(fileId) { return 'http://localhost:8182/api/file/preview/' + fileId; },
        getDetail(fileId) { return API.fileReq('/api/file/detail/' + fileId); }
    },

    // ==================== 分享相关 ====================
    share: {
        create(fileId, sharePwd, expireDays) {
            let url = '/api/share/create?fileId=' + fileId;
            if (sharePwd) url += '&sharePwd=' + sharePwd;
            if (expireDays) url += '&expireDays=' + expireDays;
            return API.fileReq(url, { method: 'POST' });
        },
        getInfo(shareCode) { return API.fileReq('/api/share/info/' + shareCode); },
        verify(shareCode, extractCode) {
            let url = '/api/share/verify?shareCode=' + shareCode;
            if (extractCode) url += '&extractCode=' + extractCode;
            return API.fileReq(url, { method: 'POST' });
        },
        getDownloadUrl(shareCode) { return 'http://localhost:8182/api/share/download/' + shareCode; },
        cancel(shareId) { return API.fileReq('/api/share/' + shareId, { method: 'DELETE' }); },
        getMyShares() { return API.fileReq('/api/share/my'); }
    },

    // ==================== 管理员相关 ====================
    admin: {
        getUsers() { return API.userReq('/api/admin/users'); },
        searchUsers(keyword) {
            return API.userReq('/api/admin/users/search?keyword=' + encodeURIComponent(keyword));
        },
        getUserDetail(userId) { return API.userReq('/api/admin/users/' + userId); },
        updateUserStatus(userId, status) {
            return API.userReq('/api/admin/users/' + userId + '/status?status=' + status, { method: 'PUT' });
        },
        allocateStorage(userId, storageMax) {
            return API.userReq('/api/admin/users/' + userId + '/storage?storageMax=' + storageMax, { method: 'PUT' });
        },
        deleteUser(userId) { return API.userReq('/api/admin/users/' + userId, { method: 'DELETE' }); },
        getStats() { return API.fileReq('/api/admin/stats'); },
        getAllFiles() { return API.fileReq('/api/admin/files'); },
        getFilesByUser(userId) { return API.fileReq('/api/admin/files/user/' + userId); },
        deleteFile(fileId) { return API.fileReq('/api/admin/files/' + fileId, { method: 'DELETE' }); },
        getShares() { return API.fileReq('/api/admin/shares'); },
        deleteShare(shareId) { return API.fileReq('/api/admin/shares/' + shareId, { method: 'DELETE' }); },
        getLogs() { return API.fileReq('/api/admin/logs'); }
    }
};

// ==================== Toast 提示 ====================
const Toast = {
    show(message, type) {
        const icons = { success: '\u2713', error: '\u2717', warning: '!', info: 'i' };
        let container = document.querySelector('.toast-container');
        if (!container) { container = document.createElement('div'); container.className = 'toast-container'; document.body.appendChild(container); }
        const toast = document.createElement('div');
        toast.className = 'toast ' + (type || 'info');
        toast.innerHTML = '<span class="toast-icon">' + (icons[type || 'info'] || 'i') +
            '</span><span class="toast-msg">' + message + '</span>';
        container.appendChild(toast);
        setTimeout(() => { toast.classList.add('fade-out'); setTimeout(() => toast.remove(), 300); }, 3000);
    },
    success(msg) { this.show(msg, 'success'); },
    error(msg) { this.show(msg, 'error'); },
    warning(msg) { this.show(msg, 'warning'); },
    info(msg) { this.show(msg, 'info'); }
};