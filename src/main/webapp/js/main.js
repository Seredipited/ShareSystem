/**
 * 个人学习资料分享系统 - 主页面逻辑
 */

// ==================== 全局状态 ====================
let currentUser = null;
let currentFolderId = 0;
let folderStack = [{ id: 0, name: '全部文件' }];
let currentFiles = [];
let currentPage = 'files'; // files | shared | recycle
let selectMode = false;
let selectedFiles = new Set();
let currentView = 'grid'; // grid | list
let renameFileId = null;
let moveFileId = null;
let shareFileId = null;
let searchTimer = null;

// ==================== 初始化 ====================
document.addEventListener('DOMContentLoaded', async () => {
    await loadCurrentUser();
    if (!currentUser) {
        window.location.href = 'login.html';
        return;
    }
    updateUserUI();
    loadStorageInfo();
    loadFiles();
    initDragDrop();
    initContextMenu();
});

async function loadCurrentUser() {
    try {
        const result = await API.user.getCurrent();
        currentUser = result.data;
    } catch (e) {
        currentUser = null;
    }
}

function updateUserUI() {
    if (!currentUser) return;
    document.getElementById('userName').textContent =
        currentUser.nickname || currentUser.username;
    const avatar = document.getElementById('userAvatar');
    avatar.textContent = (currentUser.nickname || currentUser.username).charAt(0).toUpperCase();
    if (currentUser.avatar) {
        avatar.style.backgroundImage = `url(${currentUser.avatar})`;
        avatar.style.backgroundSize = 'cover';
    }
}

async function loadStorageInfo() {
    try {
        const result = await API.user.getStorageInfo();
        if (result.data) {
            const used = result.data.used || 0;
            const max = result.data.max || 1073741824;
            const percent = max > 0 ? Math.min((used / max) * 100, 100) : 0;
            document.getElementById('storageFill').style.width = percent + '%';
            document.getElementById('storageText').textContent =
                formatSize(used) + ' / ' + formatSize(max);
        }
    } catch (e) { /* ignore */ }
}

// ==================== 文件列表 ====================
async function loadFiles() {
    const fileView = document.getElementById('fileView');
    fileView.innerHTML = '<div class="loading-spinner" style="margin:40px auto;"></div>';

    try {
        let result;
        switch (currentPage) {
            case 'shared':
                result = await API.file.getSharedFiles();
                break;
            case 'recycle':
                result = await API.file.getRecycleBin();
                break;
            default:
                result = await API.file.getList(currentFolderId);
        }
        currentFiles = result.data || [];
        renderFiles(currentFiles);
        updateBreadcrumb();
        updateToolbar();
    } catch (e) {
        fileView.innerHTML = `<div class="empty-state">
            <div class="empty-icon">⚠️</div>
            <div class="empty-title">加载失败</div>
            <div class="empty-desc">${e.message}</div>
        </div>`;
    }
}

function renderFiles(files) {
    const fileView = document.getElementById('fileView');

    if (!files || files.length === 0) {
        let icon = '📂', title = '暂无文件', desc = '上传文件或创建文件夹开始使用';
        if (currentPage === 'shared') {
            icon = '🔗'; title = '暂无分享'; desc = '选择文件创建分享链接';
        } else if (currentPage === 'recycle') {
            icon = '🗑️'; title = '回收站为空'; desc = '删除的文件会出现在这里';
        }
        fileView.innerHTML = `<div class="empty-state">
            <div class="empty-icon">${icon}</div>
            <div class="empty-title">${title}</div>
            <div class="empty-desc">${desc}</div>
            ${currentPage === 'files' ? '<button class="btn btn-primary" onclick="openUploadDialog()">⬆ 上传文件</button>' : ''}
        </div>`;
        return;
    }

    fileView.className = 'file-view' + (currentView === 'list' ? ' list-view' : '');
    fileView.innerHTML = '';

    files.forEach(file => {
        if (currentView === 'list') {
            fileView.appendChild(createFileRow(file));
        } else {
            fileView.appendChild(createFileCard(file));
        }
    });
}

function createFileCard(file) {
    const card = document.createElement('div');
    card.className = 'file-card' + (selectMode ? ' select-mode' : '') +
        (selectedFiles.has(file.id) ? ' selected' : '');
    card.dataset.fileId = file.id;
    card.dataset.fileName = file.fileName;
    card.dataset.isFolder = file.isFolder;

    const iconClass = file.isFolder ? 'folder-icon' :
        getFileIconClass(file.fileType);

    card.innerHTML = `
        <div class="file-checkbox">✓</div>
        <div class="file-icon ${iconClass}">${getFileEmoji(file)}</div>
        <div class="file-name">${file.fileName}</div>
        <div class="file-meta">${file.isFolder === 1 ? '' : formatSize(file.fileSize)}</div>
    `;

    card.addEventListener('click', (e) => handleFileClick(file, e));
    card.addEventListener('dblclick', () => handleFileDoubleClick(file));
    card.addEventListener('contextmenu', (e) => {
        e.preventDefault();
        showContextMenu(e, file);
    });

    return card;
}

function createFileRow(file) {
    const row = document.createElement('div');
    row.className = 'file-row';
    row.dataset.fileId = file.id;

    const iconClass = file.isFolder ? 'folder-icon' :
        getFileIconClass(file.fileType);

    row.innerHTML = `
        <div class="row-icon ${iconClass}">${getFileEmoji(file)}</div>
        <div class="row-name">${file.fileName}</div>
        <div class="row-size">${file.isFolder === 1 ? '-' : formatSize(file.fileSize)}</div>
        <div class="row-date">${formatDate(file.createTime)}</div>
        <div class="row-actions">
            ${file.isFolder === 0 ? `<button class="btn btn-ghost btn-sm" onclick="event.stopPropagation();previewFile(${file.id})" title="预览">👁</button>` : ''}
            <button class="btn btn-ghost btn-sm" onclick="event.stopPropagation();shareFile(${file.id})" title="分享">🔗</button>
            <button class="btn btn-ghost btn-sm" onclick="event.stopPropagation();downloadFile(${file.id})" title="下载">⬇</button>
            <button class="btn btn-ghost btn-sm btn-danger" onclick="event.stopPropagation();deleteFile(${file.id})" title="删除">🗑</button>
        </div>
    `;

    row.addEventListener('click', (e) => handleFileClick(file, e));
    row.addEventListener('dblclick', () => handleFileDoubleClick(file));
    row.addEventListener('contextmenu', (e) => {
        e.preventDefault();
        showContextMenu(e, file);
    });

    return row;
}

function handleFileClick(file, e) {
    if (selectMode) {
        toggleFileSelection(file.id);
        return;
    }
    // 单击操作
}

function handleFileDoubleClick(file) {
    if (file.isFolder === 1) {
        navigateToFolder(file.id, file.fileName);
    } else {
        previewFile(file.id);
    }
}

function toggleFileSelection(fileId) {
    if (selectedFiles.has(fileId)) {
        selectedFiles.delete(fileId);
    } else {
        selectedFiles.add(fileId);
    }
    updateSelectionUI();
}

function updateSelectionUI() {
    document.querySelectorAll('.file-card, .file-row').forEach(el => {
        const id = parseInt(el.dataset.fileId);
        if (id && selectedFiles.has(id)) {
            el.classList.add('selected');
        } else {
            el.classList.remove('selected');
        }
    });
}

// ==================== 文件操作 ====================

function openUploadDialog() {
    document.getElementById('fileInput').click();
}

function handleFileSelect(event) {
    const files = event.target.files;
    if (!files || files.length === 0) return;

    const CHUNK_SIZE = 1024 * 1024; // 1MB

    for (let i = 0; i < files.length; i++) {
        const file = files[i];
        if (file.size > CHUNK_SIZE) {
            uploadFileChunked(file);
        } else {
            uploadFileSmall(file);
        }
    }
    event.target.value = '';
}

async function uploadFileSmall(file) {
    const progressItem = addProgressItem(file.name);

    try {
        await API.file.upload(file, currentFolderId);
        progressItemComplete(progressItem, '✅');
        await loadFiles();
        loadStorageInfo();
    } catch (e) {
        progressItemComplete(progressItem, '❌');
        Toast.error('上传失败: ' + e.message);
    }
}

async function uploadFileChunked(file) {
    const CHUNK_SIZE = 1024 * 1024;
    const totalChunks = Math.ceil(file.size / CHUNK_SIZE);
    const progressItem = addProgressItem(file.name);
    const progressEl = progressItem.querySelector('.progress-fill');

    // 计算文件MD5（简化，实际应使用spark-md5等库）
    let fileMd5 = '';
    try {
        const buffer = await file.arrayBuffer();
        fileMd5 = await computeMD5(buffer);
    } catch(e) {
        fileMd5 = 'md5_' + Date.now() + '_' + file.size;
    }

    // 检查已有进度
    try {
        const progress = await API.file.checkChunkProgress(fileMd5);
        const uploadedChunks = new Set(progress.data || []);
        let uploadedCount = uploadedChunks.size;

        for (let i = 0; i < totalChunks; i++) {
            if (uploadedChunks.has(i)) continue;

            const start = i * CHUNK_SIZE;
            const end = Math.min(start + CHUNK_SIZE, file.size);
            const chunk = file.slice(start, end);

            try {
                await API.file.uploadChunk(chunk, fileMd5, i, totalChunks, file.name);
                uploadedCount++;
                const pct = Math.round((uploadedCount / totalChunks) * 100);
                progressEl.style.width = pct + '%';
                progressItem.querySelector('.progress-detail').textContent =
                    `${pct}% (${i + 1}/${totalChunks})`;
            } catch (e) {
                progressItemComplete(progressItem, '⏸');
                Toast.warning(`分片上传已暂停: ${file.name}`);
                return;
            }
        }

        // 合并分片
        await API.file.mergeChunks(fileMd5, file.name, currentFolderId);
        progressItemComplete(progressItem, '✅');
        await loadFiles();
        loadStorageInfo();
    } catch (e) {
        progressItemComplete(progressItem, '❌');
        Toast.error('上传失败: ' + e.message);
    }
}

function addProgressItem(fileName) {
    const container = document.getElementById('progressList');
    const item = document.createElement('div');
    item.className = 'progress-item';
    item.innerHTML = `
        <div class="progress-icon">📄</div>
        <div class="progress-info">
            <div class="progress-name">${fileName}</div>
            <div class="progress-detail">准备中...</div>
            <div class="progress-bar-wrap">
                <div class="progress-fill" style="width:0%"></div>
            </div>
        </div>
    `;
    container.appendChild(item);
    openModal('uploadProgressModal');
    return item;
}

function progressItemComplete(item, icon) {
    item.querySelector('.progress-icon').textContent = icon;
    const fill = item.querySelector('.progress-fill');
    if (icon === '✅') fill.style.background = '#34c759';
    else if (icon === '❌') fill.style.background = '#ff3b30';
}

async function createNewFolder() {
    document.getElementById('folderNameInput').value = '';
    openModal('folderModal');
    setTimeout(() => document.getElementById('folderNameInput').focus(), 200);
}

async function confirmCreateFolder() {
    const name = document.getElementById('folderNameInput').value.trim();
    if (!name) { Toast.warning('请输入文件夹名称'); return; }
    try {
        await API.file.createFolder(name, currentFolderId);
        closeModal('folderModal');
        loadFiles();
        Toast.success('文件夹创建成功');
    } catch (e) { Toast.error(e.message); }
}

function renameFileTrigger(fileId, currentName) {
    renameFileId = fileId;
    document.getElementById('renameInput').value = currentName;
    openModal('renameModal');
    setTimeout(() => {
        const input = document.getElementById('renameInput');
        input.focus();
        input.select();
    }, 200);
}

async function confirmRename() {
    const newName = document.getElementById('renameInput').value.trim();
    if (!newName || !renameFileId) return;
    try {
        await API.file.rename(renameFileId, newName);
        closeModal('renameModal');
        loadFiles();
        Toast.success('重命名成功');
    } catch (e) { Toast.error(e.message); }
}

async function moveFileTrigger(fileId) {
    moveFileId = fileId;
    openModal('moveModal');
    const listEl = document.getElementById('moveFolderList');
    listEl.innerHTML = '<p>加载中...</p>';
    try {
        const result = await API.file.getList(0);
        const folders = (result.data || []).filter(f => f.isFolder === 1);
        listEl.innerHTML = `
            <div class="form-group">
                <label class="form-label">选择目标文件夹</label>
                <div style="max-height:300px;overflow-y:auto;">
                    <div style="padding:8px 12px;cursor:pointer;border-radius:6px;"
                         onclick="this.style.background='var(--accent)';this.style.color='white';this.dataset.selected='true';
                         Array.from(this.parentNode.children).forEach(c=>{if(c!==this){c.style.background='';c.style.color='';c.dataset.selected='';}})"
                         data-id="0">📁 根目录</div>
                    ${folders.map(f => `
                        <div style="padding:8px 12px;cursor:pointer;border-radius:6px;"
                             onclick="this.style.background='var(--accent)';this.style.color='white';this.dataset.selected='true';
                             Array.from(this.parentNode.children).forEach(c=>{if(c!==this){c.style.background='';c.style.color='';c.dataset.selected='';}})"
                             data-id="${f.id}">📁 ${f.fileName}</div>
                    `).join('')}
                </div>
            </div>
        `;
    } catch (e) {
        listEl.innerHTML = '<p style="color:var(--danger);">加载失败</p>';
    }
}

async function confirmMove() {
    const selected = document.querySelector('#moveFolderList div[data-selected="true"][data-id]');
    const targetId = selected ? parseInt(selected.dataset.id) : 0;
    try {
        await API.file.move(moveFileId, targetId);
        closeModal('moveModal');
        loadFiles();
        Toast.success('移动成功');
    } catch (e) { Toast.error(e.message); }
}

async function deleteFile(fileId) {
    if (!confirm('确定要删除吗？文件将移入回收站。')) return;
    try {
        await API.file.delete(fileId);
        loadFiles();
        loadStorageInfo();
        Toast.success('已移入回收站');
    } catch (e) { Toast.error(e.message); }
}

async function batchDelete() {
    if (selectedFiles.size === 0) return;
    if (!confirm(`确定要删除 ${selectedFiles.size} 个文件吗？`)) return;
    try {
        await API.file.batchDelete(Array.from(selectedFiles));
        selectedFiles.clear();
        toggleSelectMode();
        loadFiles();
        loadStorageInfo();
        Toast.success('批量删除成功');
    } catch (e) { Toast.error(e.message); }
}

async function restoreFile(fileId) {
    try {
        await API.file.restore(fileId);
        loadFiles();
        Toast.success('文件已还原');
    } catch (e) { Toast.error(e.message); }
}

async function permanentDeleteFile(fileId) {
    if (!confirm('确定要彻底删除吗？此操作不可恢复！')) return;
    try {
        await API.file.permanentDelete(fileId);
        loadFiles();
        loadStorageInfo();
        Toast.success('已彻底删除');
    } catch (e) { Toast.error(e.message); }
}

async function clearRecycleBin() {
    if (!confirm('确定要清空回收站吗？所有文件将被彻底删除！')) return;
    try {
        await API.file.clearRecycleBin();
        loadFiles();
        loadStorageInfo();
        Toast.success('回收站已清空');
    } catch (e) { Toast.error(e.message); }
}

function downloadFile(fileId) {
    window.open(API.file.getDownloadUrl(fileId), '_blank');
}

function previewFile(fileId) {
    window.open(`preview.html?fileId=${fileId}`, '_blank');
}

// ==================== 分享相关 ====================

function shareFile(fileId) {
    shareFileId = fileId;
    document.getElementById('sharePwdInput').value = '';
    document.getElementById('shareExpireSelect').value = '7';
    document.getElementById('shareResult').style.display = 'none';
    document.getElementById('shareCreateFooter').style.display = 'flex';
    openModal('shareModal');
}

async function confirmCreateShare() {
    const pwd = document.getElementById('sharePwdInput').value.trim() || null;
    const expireDays = parseInt(document.getElementById('shareExpireSelect').value);
    try {
        const result = await API.share.create(shareFileId, pwd, expireDays);
        const link = window.location.origin + '/ShareSystem/pages/share.html?code=' +
            result.data.shareCode;
        document.getElementById('shareLink').value = link;
        document.getElementById('shareResult').style.display = 'block';
        document.getElementById('shareCreateFooter').style.display = 'none';
        Toast.success('分享创建成功');
    } catch (e) { Toast.error(e.message); }
}

function copyShareLink() {
    const input = document.getElementById('shareLink');
    input.select();
    document.execCommand('copy');
    Toast.success('链接已复制到剪贴板');
}

// ==================== 导航 ====================

function switchPage(page) {
    currentPage = page;
    currentFolderId = 0;
    folderStack = [{ id: 0, name: '全部文件' }];
    selectedFiles.clear();
    selectMode = false;

    document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));
    document.querySelector(`.nav-item[data-page="${page}"]`)?.classList.add('active');

    loadFiles();
}

function navigateToFolder(folderId, folderName) {
    currentFolderId = folderId;
    folderStack.push({ id: folderId, name: folderName });
    loadFiles();
}

function navigateToRoot() {
    if (currentPage !== 'files') {
        switchPage('files');
        return;
    }
    if (currentFolderId === 0) return;
    currentFolderId = 0;
    folderStack = [{ id: 0, name: '全部文件' }];
    loadFiles();
}

function updateBreadcrumb() {
    if (currentPage !== 'files') {
        document.getElementById('breadcrumb').innerHTML = `
            <span class="breadcrumb-item" onclick="navigateToRoot()">🏠 全部文件</span>
            <span class="breadcrumb-separator">/</span>
            <span class="breadcrumb-current">${currentPage === 'shared' ? '我的分享' : '回收站'}</span>
        `;
        return;
    }

    let html = '';
    folderStack.forEach((item, index) => {
        if (index > 0) html += '<span class="breadcrumb-separator">/</span>';
        if (index === folderStack.length - 1) {
            html += `<span class="breadcrumb-current">${item.name}</span>`;
        } else {
            html += `<span class="breadcrumb-item" onclick="navigateToBreadcrumb(${item.id},${index})">${item.name}</span>`;
        }
    });
    document.getElementById('breadcrumb').innerHTML = '<span class="breadcrumb-item" onclick="navigateToRoot()">🏠 全部文件</span>' + html;
}

function navigateToBreadcrumb(folderId, index) {
    folderStack = folderStack.slice(0, index + 1);
    currentFolderId = folderId;
    loadFiles();
}

function updateToolbar() {
    if (currentPage === 'recycle') {
        document.querySelector('.toolbar-group:first-child').style.display = 'none';
        document.getElementById('batchDownloadBtn').style.display = 'none';
        document.getElementById('batchDeleteBtn').style.display = 'none';
        document.getElementById('selectBtn').style.display = 'none';
    } else {
        document.querySelector('.toolbar-group:first-child').style.display = '';
        document.getElementById('selectBtn').style.display = '';
    }
}

// ==================== 选择模式 ====================

function toggleSelectMode() {
    selectMode = !selectMode;
    if (!selectMode) selectedFiles.clear();
    document.getElementById('selectBtn').textContent = selectMode ? '取消' : '多选';
    document.getElementById('batchDownloadBtn').style.display = selectMode ? '' : 'none';
    document.getElementById('batchDeleteBtn').style.display = selectMode ? '' : 'none';
    renderFiles(currentFiles);
}

// ==================== 视图切换 ====================

function switchView(view) {
    currentView = view;
    document.getElementById('viewGridBtn').style.fontWeight = view === 'grid' ? '600' : '400';
    document.getElementById('viewListBtn').style.fontWeight = view === 'list' ? '600' : '400';
    renderFiles(currentFiles);
}

// ==================== 搜索 ====================

function handleSearch(keyword) {
    clearTimeout(searchTimer);
    searchTimer = setTimeout(async () => {
        if (!keyword || keyword.trim() === '') {
            loadFiles();
            return;
        }
        try {
            const result = await API.file.search(keyword.trim());
            currentFiles = result.data || [];
            renderFiles(currentFiles);
        } catch (e) { /* ignore */ }
    }, 300);
}

// ==================== 右键菜单 ====================

let contextMenu = null;
function initContextMenu() {
    contextMenu = document.createElement('div');
    contextMenu.className = 'context-menu';
    document.body.appendChild(contextMenu);
    document.addEventListener('click', () => contextMenu.classList.remove('show'));
}

function showContextMenu(e, file) {
    contextMenu.innerHTML = file.isFolder === 1 ? `
        <div class="context-menu-item" onclick="navigateToFolder(${file.id},'${file.fileName}')">📂 打开</div>
        <div class="context-menu-item" onclick="renameFileTrigger(${file.id},'${file.fileName}')">✏️ 重命名</div>
        <div class="context-menu-item" onclick="moveFileTrigger(${file.id})">📁 移动到</div>
        <div class="context-menu-divider"></div>
        <div class="context-menu-item danger" onclick="deleteFile(${file.id})">🗑 删除</div>
    ` : `
        <div class="context-menu-item" onclick="previewFile(${file.id})">👁 预览</div>
        <div class="context-menu-item" onclick="shareFile(${file.id})">🔗 分享</div>
        <div class="context-menu-item" onclick="downloadFile(${file.id})">⬇ 下载</div>
        <div class="context-menu-divider"></div>
        <div class="context-menu-item" onclick="renameFileTrigger(${file.id},'${file.fileName}')">✏️ 重命名</div>
        <div class="context-menu-item" onclick="moveFileTrigger(${file.id})">📁 移动到</div>
        <div class="context-menu-divider"></div>
        <div class="context-menu-item danger" onclick="deleteFile(${file.id})">🗑 删除</div>
    `;

    const x = Math.min(e.clientX, window.innerWidth - 180);
    const y = Math.min(e.clientY, window.innerHeight - 300);
    contextMenu.style.left = x + 'px';
    contextMenu.style.top = y + 'px';
    contextMenu.classList.add('show');
}

// ==================== 拖拽上传 ====================

function initDragDrop() {
    const area = document.getElementById('contentArea');
    area.addEventListener('dragover', (e) => { e.preventDefault(); });
    area.addEventListener('drop', async (e) => {
        e.preventDefault();
        const files = e.dataTransfer.files;
        if (!files.length) return;
        for (let i = 0; i < files.length; i++) {
            const file = files[i];
            if (file.size > 1024 * 1024) {
                uploadFileChunked(file);
            } else {
                uploadFileSmall(file);
            }
        }
    });
}

// ==================== 用户菜单 ====================

function toggleUserMenu() {
    if (confirm('确定要退出登录吗？')) {
        API.user.logout();
        window.location.href = 'login.html';
    }
}

// ==================== 工具函数 ====================

function openModal(id) {
    document.getElementById(id).classList.add('active');
}

function closeModal(id) {
    document.getElementById(id).classList.remove('active');
}

function getFileEmoji(file) {
    if (file.isFolder === 1) return '📁';
    const type = (file.fileType || '').toLowerCase();
    const map = {
        jpg: '🖼', jpeg: '🖼', png: '🖼', gif: '🖼', bmp: '🖼', svg: '🖼', webp: '🖼',
        mp4: '🎬', avi: '🎬', mov: '🎬', mkv: '🎬', webm: '🎬', flv: '🎬',
        mp3: '🎵', wav: '🎵', flac: '🎵', aac: '🎵', ogg: '🎵',
        pdf: '📕', doc: '📝', docx: '📝', xls: '📊', xlsx: '📊',
        ppt: '📽', pptx: '📽', txt: '📄', zip: '📦', rar: '📦', '7z': '📦'
    };
    return map[type] || '📄';
}

function getFileIconClass(fileType) {
    if (!fileType) return 'file-icon-default';
    const type = fileType.toLowerCase();
    if (type.match(/jpg|jpeg|png|gif|bmp|svg|webp/)) return 'image-icon';
    if (type.match(/mp4|avi|mov|mkv|webm|flv/)) return 'video-icon';
    if (type.match(/mp3|wav|flac|aac|ogg/)) return 'audio-icon';
    if (type.match(/txt|html|css|js|json|xml|java|py|cpp/)) return 'text-icon';
    if (type === 'pdf') return 'pdf-icon';
    if (type.match(/doc|docx/)) return 'word-icon';
    if (type.match(/xls|xlsx/)) return 'excel-icon';
    if (type.match(/ppt|pptx/)) return 'ppt-icon';
    if (type.match(/zip|rar|7z|tar|gz/)) return 'archive-icon';
    return 'file-icon-default';
}

function formatSize(bytes) {
    if (!bytes || bytes === 0) return '0 B';
    const units = ['B', 'KB', 'MB', 'GB', 'TB'];
    const digitGroups = Math.floor(Math.log10(bytes) / Math.log10(1024));
    return (bytes / Math.pow(1024, digitGroups)).toFixed(1) + ' ' + units[digitGroups];
}

function formatDate(dateStr) {
    if (!dateStr) return '-';
    const d = new Date(dateStr);
    return d.getFullYear() + '-' +
        String(d.getMonth() + 1).padStart(2, '0') + '-' +
        String(d.getDate()).padStart(2, '0') + ' ' +
        String(d.getHours()).padStart(2, '0') + ':' +
        String(d.getMinutes()).padStart(2, '0');
}

async function computeMD5(buffer) {
    // 简化的MD5生成（生产环境应使用 spark-md5 库）
    let hash = 0;
    const view = new Uint8Array(buffer);
    for (let i = 0; i < view.length; i++) {
        hash = ((hash << 5) - hash) + view[i];
        hash |= 0;
    }
    return 'sim_md5_' + Math.abs(hash).toString(16) + '_' + buffer.byteLength;
}

function batchDownload() {
    if (selectedFiles.size === 0) return;
    selectedFiles.forEach(id => {
        window.open(API.file.getDownloadUrl(id), '_blank');
    });
}
