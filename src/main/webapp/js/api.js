/**
 * 个人学习资料分享系统 - API 请求模块
 */

const API = {
    baseURL: '/ShareSystem/api',

    /**
     * 通用请求方法
     */
    async request(url, options = {}) {
        const config = {
            headers: { 'Content-Type': 'application/json' },
            ...options
        };

        // 非GET/FORM不设置Content-Type让浏览器自动处理
        if (options.body instanceof FormData) {
            delete config.headers['Content-Type'];
        }

        const response = await fetch(this.baseURL + url, config);

        if (response.status === 401) {
            window.location.href = '/ShareSystem/pages/login.html';
            throw new Error('未登录');
        }

        // 处理流式响应（下载/预览）
        const contentType = response.headers.get('Content-Type') || '';
        if (contentType.includes('application/octet-stream') ||
            contentType.includes('image/') ||
            contentType.includes('video/') ||
            contentType.includes('audio/') ||
            contentType.includes('application/pdf')) {
            return response;
        }

        const data = await response.json();
        if (data.code && data.code !== 200) {
            throw new Error(data.message || '请求失败');
        }
        return data;
    },

    get(url) { return this.request(url); },
    post(url, data) { return this.request(url, { method: 'POST', body: JSON.stringify(data) }); },
    put(url, data) { return this.request(url, { method: 'PUT', body: JSON.stringify(data) }); },
    delete(url) { return this.request(url, { method: 'DELETE' }); },

    // ==================== 用户相关 ====================

    user: {
        register(data) { return API.post('/user/register', data); },
        login(data) { return API.post('/user/login', data); },
        logout() { return API.post('/user/logout'); },
        getCurrent() { return API.get('/user/current'); },
        getQQLoginUrl() { return API.get('/user/qq/login'); },
        changePassword(oldPassword, newPassword) {
            return API.post('/user/changePassword?oldPassword=' +
                encodeURIComponent(oldPassword) + '&newPassword=' +
                encodeURIComponent(newPassword));
        },
        updateProfile(data) { return API.put('/user/profile', data); },
        getStorageInfo() { return API.get('/user/storage'); },

        uploadAvatar(file) {
            const fd = new FormData();
            fd.append('file', file);
            return API.request('/user/avatar', { method: 'POST', body: fd });
        }
    },

    // ==================== 文件相关 ====================

    file: {
        getList(parentId = 0) {
            return API.get('/file/list?parentId=' + parentId);
        },
        createFolder(folderName, parentId = 0) {
            return API.post('/file/folder?folderName=' +
                encodeURIComponent(folderName) + '&parentId=' + parentId);
        },
        upload(file, parentId = 0) {
            const fd = new FormData();
            fd.append('file', file);
            fd.append('parentId', parentId);
            return API.request('/file/upload?parentId=' + parentId,
                { method: 'POST', body: fd });
        },
        checkInstant(fileName, fileMd5, parentId = 0) {
            return API.post('/file/checkInstant?fileName=' +
                encodeURIComponent(fileName) + '&fileMd5=' + fileMd5 +
                '&parentId=' + parentId);
        },
        uploadChunk(chunk, fileMd5, chunkIndex, totalChunks, fileName) {
            const fd = new FormData();
            fd.append('file', chunk);
            fd.append('fileMd5', fileMd5);
            fd.append('chunkIndex', chunkIndex);
            fd.append('totalChunks', totalChunks);
            fd.append('fileName', fileName);
            return API.request('/file/chunk', { method: 'POST', body: fd });
        },
        checkChunkProgress(fileMd5) {
            return API.get('/file/chunkProgress?fileMd5=' + fileMd5);
        },
        mergeChunks(fileMd5, fileName, parentId = 0) {
            return API.post('/file/merge?fileMd5=' + fileMd5 +
                '&fileName=' + encodeURIComponent(fileName) +
                '&parentId=' + parentId);
        },
        rename(fileId, newName) {
            return API.put('/file/rename/' + fileId +
                '?newName=' + encodeURIComponent(newName));
        },
        move(fileId, targetParentId) {
            return API.put('/file/move/' + fileId + '?targetParentId=' + targetParentId);
        },
        delete(fileId) { return API.delete('/file/' + fileId); },
        batchDelete(fileIds) { return API.post('/file/batchDelete', fileIds); },
        restore(fileId) { return API.put('/file/restore/' + fileId); },
        permanentDelete(fileId) { return API.delete('/file/permanent/' + fileId); },
        getRecycleBin() { return API.get('/file/recycle'); },
        clearRecycleBin() { return API.delete('/file/recycle/clear'); },
        getSharedFiles() { return API.get('/file/shared'); },
        search(keyword) {
            return API.get('/file/search?keyword=' + encodeURIComponent(keyword));
        },
        getDownloadUrl(fileId) {
            return API.baseURL + '/file/download/' + fileId;
        },
        getPreviewUrl(fileId) {
            return API.baseURL + '/file/preview/' + fileId;
        },
        getDetail(fileId) {
            return API.get('/file/detail/' + fileId);
        }
    },

    // ==================== 分享相关 ====================

    share: {
        create(fileId, sharePwd, expireDays) {
            let url = '/share/create?fileId=' + fileId;
            if (sharePwd) url += '&sharePwd=' + sharePwd;
            if (expireDays) url += '&expireDays=' + expireDays;
            return API.post(url);
        },
        getInfo(shareCode) { return API.get('/share/info/' + shareCode); },
        verify(shareCode, extractCode) {
            let url = '/share/verify?shareCode=' + shareCode;
            if (extractCode) url += '&extractCode=' + extractCode;
            return API.post(url);
        },
        getDownloadUrl(shareCode) {
            return API.baseURL + '/share/download/' + shareCode;
        },
        cancel(shareId) { return API.delete('/share/' + shareId); },
        getMyShares() { return API.get('/share/my'); }
    },

    // ==================== 管理员相关 ====================

    admin: {
        getUsers() { return API.get('/admin/users'); },
        searchUsers(keyword) {
            return API.get('/admin/users/search?keyword=' + encodeURIComponent(keyword));
        },
        getUserDetail(userId) { return API.get('/admin/users/' + userId); },
        updateUserStatus(userId, status) {
            return API.put('/admin/users/' + userId + '/status?status=' + status);
        },
        allocateStorage(userId, storageMax) {
            return API.put('/admin/users/' + userId + '/storage?storageMax=' + storageMax);
        },
        deleteUser(userId) { return API.delete('/admin/users/' + userId); },
        getStats() { return API.get('/admin/stats'); },
        getAllFiles() { return API.get('/admin/files'); },
        getFilesByUser(userId) { return API.get('/admin/files/user/' + userId); },
        deleteFile(fileId) { return API.delete('/admin/files/' + fileId); },
        getShares() { return API.get('/admin/shares'); },
        deleteShare(shareId) { return API.delete('/admin/shares/' + shareId); }
    }
};

// ==================== Toast 提示 ====================
const Toast = {
    show(message, type = 'info') {
        const icons = {
            success: '\u2713',
            error: '\u2717',
            warning: '!',
            info: 'i'
        };
        const container = document.querySelector('.toast-container') || (() => {
            const c = document.createElement('div');
            c.className = 'toast-container';
            document.body.appendChild(c);
            return c;
        })();

        const toast = document.createElement('div');
        toast.className = `toast ${type}`;
        toast.innerHTML = `
            <span class="toast-icon">${icons[type] || 'i'}</span>
            <span class="toast-msg">${message}</span>
        `;

        container.appendChild(toast);

        setTimeout(() => {
            toast.classList.add('fade-out');
            setTimeout(() => toast.remove(), 300);
        }, 3000);
    },
    success(msg) { this.show(msg, 'success'); },
    error(msg) { this.show(msg, 'error'); },
    warning(msg) { this.show(msg, 'warning'); },
    info(msg) { this.show(msg, 'info'); }
};
