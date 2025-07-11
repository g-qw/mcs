const API_BASE_URL = 'http://localhost:75' // 后端的网关入口
const API_USER_URL = `${API_BASE_URL}/users` // 用户服务
const API_MAIL_URL = `${API_BASE_URL}/mail` // 邮件服务
const API_FU_URL = `${API_BASE_URL}/fu` // 文件上传服务
const API_FD_URL = `${API_BASE_URL}/fd` // 文件下载服务
const API_FS_URL = `${API_BASE_URL}/fs` // 文件系统服务

let cacheToken = null; // 缓存的token

// 监听 sessionStorage 和 localStorage 中的 token 变化
window.addEventListener('storage', (event) => {
    if (event.key === 'token') {
        cacheToken = event.newValue;
    }
});

// 获取token
function getToken() {
    if (cacheToken) {
        return cacheToken;
    }

    // 如果缓存中没有 token，则从 sessionStorage 或 localStorage 中获取
    return sessionStorage.getItem('token') || localStorage.getItem('token');
}

export const apiEndpoints = {

    /* ==================== 用户服务接口 ==================== */
    userRegister: (email, username, password, code) => ( // 注册
        {
            url: `${API_USER_URL}/register`,
            method: 'POST',
            data: {
                email,
                username,
                password,
                code
            }
        }
    ),
    userLogin: (email, password, code) => ( // 登录
        {
            url: `${API_USER_URL}/login`,
            method: 'POST',
            data: {
                email,
                password,
                code
            }
        }
    ),

    userInfo: () => ( // 获取用户信息
        {
            url: `${API_USER_URL}/info`,
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${getToken()}`
            }
        }
    ),

    userLogout: () => ( // 退出登录
        {
            url: `${API_USER_URL}/logout`,
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${getToken()}`
            }
        }
    ),

    userUpdatePwd: (email, password, newPassword) => ( // 更新密码
        {
            url: `${API_USER_URL}/update_pwd`,
            method: 'POST',
            data: {
                email,
                password,
                newPassword
            },
            headers: {
                'Authorization': `Bearer ${getToken()}`
            }
        }
    ),

    userResetPwd: (email, newPassword, code) => ( // 忘记密码时重置密码
        {
            url: `${API_USER_URL}/reset_pwd`,
            method: 'POST',
            data: {
                email,
                newPassword,
                code
            },
            headers: {
                'Authorization': `Bearer ${getToken()}`
            }
        }
    ),

    userUsedCapacity: (userId) => ( // 获取用户已用容量
        {
            url: `${API_USER_URL}/used_capacity?userId=${userId}`,
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${getToken()}`
            }
        }
    ),

    userStatus: (userId) => ( // 获取用户状态
        {
            url: `${API_USER_URL}/status?userId=${userId}`,
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${getToken()}`
            }
        }
    ),

    /* ==================== 邮件服务接口 ==================== */
    sendEmailCode: (email) => ( // 发送邮件验证码
        {
            url: `${API_MAIL_URL}/verify`,
            method: 'POST',
            data: {
                email
            }
        }
    ),


    /* ==================== 文件上传服务接口 ==================== */
    /**
     * 
     * @param {String} bucketName 
     * @param {String} objectName 
     * @param {File} file 
     * @returns 
     */
    uploadFile: (bucketName, objectName, file, fileItem) => {
        const encodedObjectName = encodeURIComponent(objectName);
        const formData = new FormData();
        formData.append("file", file);

        return {
            url: `${API_FU_URL}/upload/file?bucketName=${bucketName}&objectName=${encodedObjectName}`,
            method: 'POST',
            data: formData,
            headers: {
                'Authorization': `Bearer ${getToken()}`,
            },
            onUploadProgress: (progressEvent) => {
                const progress = progressEvent.loaded / progressEvent.total * 100.0; // 计算上传进度
                fileItem.progress = progress
            }
        };
    },

    /**
     * 
     * @param {String} bucketName 
     * @param {File[]} files 
     * @param {[]} fileItems 文件信息列表，元素包含progress用于更新进度条
     * @returns 返回构造axios请求体的方法
     */
    uploadFiles: (bucketName, files, fileItems) => { // 上传多个文件
        const formData = new FormData();
        files.forEach(file => {
            formData.append("files", file);
        });

        return {
            url: `${API_FU_URL}/upload/files?bucketName=${bucketName}`,
            method: 'POST',
            data: formData,
            headers: {
                'Authorization': `Bearer ${getToken()}`,
            },
            onUploadProgress: (progressEvent) => {
                const progress = progressEvent.loaded / progressEvent.total * 100.0; // 计算上传进度
                fileItems.forEach(file => {
                    file.progress = progress
                });
            }
        }
    },


    /**
     * 初始化分块上传
     * @param {String} bucketName 存储桶
     * @param {String} objectName 文件的绝对路径
     * @param {String} contentType 文件的MIME类型
     * @returns 
     */
    initMultipartUpload: (bucketName, objectName, contentType) => {
        return {
            url: `${API_FU_URL}/upload/multipart/init`,
            method: 'POST',
            data: {
                bucketName: bucketName,
                objectName: objectName,
                contentType: contentType
            },
            headers: {
                'Authorization': `Bearer ${getToken()}`,
                'Content-Type': 'application/json'
            }
        };
    },

    /**
     * 上传分块
     * @param {String} bucketName 存储桶
     * @param {String} objectName 文件的绝对路径
     * @param {String} uploadId 任务ID
     * @param {int} partNumber 分块索引，从1开始
     * @param {byte[]} filePart 文件数据
     * @returns 
     */
    uploadPart: (bucketName, objectName, uploadId, partNumber, filePart) => {
        const params = new URLSearchParams();
        params.append("bucketName", bucketName);
        params.append("objectName", objectName);
        params.append("uploadId", uploadId);
        params.append("partNumber", partNumber);
    
        return {
            url: `${API_FU_URL}/upload/multipart/part?${params.toString()}`,
            method: 'POST',
            data: filePart, // 直接发送文件分片数据
            headers: {
                'Authorization': `Bearer ${getToken()}`,
                'Content-Type': 'application/octet-stream' // 设置为二进制流类型
            },
        };
    },

    /**
     * 
     * @param {String} bucketName 存储桶
     * @param {String} objectName 文件的绝对路径
     * @param {String} uploadId 上传任务ID
     * @param {String} contentType 文件的MIME类型
     * @returns 
     */
    uploadComplete: (bucketName, objectName, uploadId, contentType) => { // 完成分片上传
        const formData = new FormData();
        formData.append("bucketName", bucketName);
        formData.append("objectName", objectName);
        formData.append("uploadId", uploadId);
        formData.append("contentType", contentType);
        return {
            url: `${API_FU_URL}/upload/multipart/complete`,
            method: 'POST',
            data: formData,
            headers: {
                'Authorization': `Bearer ${getToken()}`
            }
        }
    },

    /* ==================== 文件下载服务接口 ==================== */
    downloadFile(bucket, object, task) { // 单文件下载
        const encodedObjectName = encodeURIComponent(object);

        return {
            url: `${API_FD_URL}/download/file?bucket=${bucket}&object=${encodedObjectName}`,
            method: `GET`,
            headers: {
                'Authorization': `Bearer ${getToken()}`
            },
            responseType: 'blob',
            onDownloadProgress: (progressEvent) => {
                // 计算已下载的字节数
                const totalBytes = task.size; // 文件总字节数
                const downloadedBytes = progressEvent.loaded; // 已下载的字节数
    
                // 更新task对象的属性
                task.downloaded = downloadedBytes; // 更新已下载的字节数
                task.progress = (downloadedBytes / totalBytes) * 100; // 更新进度百分比
                task.status = 'downloading'; // 更新任务状态为正在下载
            }
        }
    },

    /**
     * 
     * @param {String} bucket 存储桶
     * @param {String} object 文件在minio的绝对路径
     * @param {int} start 分片起始位置
     * @param {int} end 分片结束位置
     */
    downloadPart(bucket, object, start, end) { // 下载分片
        const encodedObjectName = encodeURIComponent(object);

        return {
            url: `${API_FD_URL}/download/multipart_download?bucket=${bucket}&object=${encodedObjectName}`,
            method: `GET`,
            headers: {
                'Authorization': `Bearer ${getToken()}`,
                'Range': `bytes=${start}-${end}`
            },
            responseType: 'arraybuffer'
        }
    },

    /* ==================== 文件系统服务接口 ==================== */
    fsCreatRootDir(userId) { // 创建文件系统的根目录
        return {
            url: `${API_FS_URL}/root_dir?userId=${userId}`,
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${getToken()}`
            }
        }
    },

    fsIsDirectoryExist(directoryId) { // 检查目录是否存在
        return {
            url: `${API_FS_URL}/is_dir_exist?directoryId=${directoryId}`,
            method: `GET`,
            headers: {
                'Authorization': `Bearer ${getToken()}`
            }
        }
    },

    fsIsFileExist(fileId) { // 检查文件是否存在
        return {
            url: `${API_FS_URL}/is_file_exist?fileId=${fileId}`,
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${getToken()}`
            }
        }
    },

    /**
     * 在文件系统创建文件
     * @param {String} directoryId 目录ID
     * @param {String} userId 用户ID
     * @param {String} objectName 文件的绝对路径
     * @param {Long} size 文件的字节数(byte)
     * @returns 
     */
    fsAddFile(directoryId, userId, objectName, mimeType, size) { // 在文件系统创建文件
        if (mimeType === "") {
            mimeType = "application/octet-stream"; // 设置为二进制类型
        }

        return {
            url: `${API_FS_URL}/add_file`,
            method: 'POST',
            data: {
                directoryId: directoryId,
                userId: userId,
                objectName: objectName,
                mimeType: mimeType,
                size: size
            },
            headers: {
                'Authorization': `Bearer ${getToken()}`
            }
        }
    },

    fsGetFile(fileId) { // 获取文件
        return {
            url: `${API_FS_URL}/get_file?fileId=${fileId}`,
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${getToken()}`
            }
        }
    },

    fsGetFileName(fileId) { // 获取文件名称
        return {
            url: `${API_FS_URL}/get_file_name?fileId=${fileId}`,
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${getToken()}`
            }
        }
    },

    fsDeleteFile(fileId) {
        return {
            url: `${API_FS_URL}/delete_file?fileId=${fileId}`,
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${getToken()}`
            }
        }
    },

    /**
     * 
     * @param {String} fileId 文件ID
     * @param {String} objectName 文件的绝对路径
     * @returns 
     */
    fsUpdateFileName(fileId, objectName) {
        return {
            url: `${API_FS_URL}/update_file_name`,
            method: 'POST',
            data: {
                fileId: fileId,
                objectName: objectName
            },
            headers: {
                'Authorization': `Bearer ${getToken()}`
            }
        }
    },

    fsMoveFile(fileId, directoryId) {
        return {
            url: `${API_FS_URL}/move_file`,
            method: 'POST',
            data: {
                fileId: fileId,
                directoryId: directoryId
            },
            headers: {
                'Authorization': `Bearer ${getToken()}`
            }
        }
    },

    fsCreateDir(parentDirectoryId, userId, name) { // 在文件系统中创建目录
        return {
            url: `${API_FS_URL}/create_dir`,
            method: 'POST',
            data: {
                parentDirectoryId: parentDirectoryId,
                userId: userId,
                name: name
            },
            headers: {
                'Authorization': `Bearer ${getToken()}`
            }
        }
    },

    fsGetDir(directoryId) {
        return {
            url: `${API_FS_URL}/get_dir?directoryId=${directoryId}`,
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${getToken()}`
            }
        }
    },

    fsGetDirName(directoryId) {
        return {
            url: `${API_FS_URL}/get_dir_name?directoryId=${directoryId}`,
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${getToken()}`
            }
        }
    },

    /* 删除目录 */
    fsDeleteDir(directoryId) {
        return {
            url: `${API_FS_URL}/delete_dir?directoryId=${directoryId}`,
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${getToken()}`
            }
        }
    },

    fsUpdateDirName(directoryId, name) {
        return {
            url: `${API_FS_URL}/update_dir_name?directoryId=${directoryId}&name=${name}`,
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${getToken()}`
            }
        }

    },

    fsMoveDir(directoryId, parentDirectoryId) {
        return {
            url: `${API_FS_URL}/move_dir`,
            method: 'POST',
            data: {
                directoryId: directoryId,
                parentDirectoryId: parentDirectoryId
            },
            headers: {
                'Authorization': `Bearer ${getToken()}`
            }
        }
    },

    /* 最重要的接口之一，可以将当前目录的数据加载到 Pinia 中 */
    fsLoadDir(directoryId) { // 加载目录
        return {
            url: `${API_FS_URL}/load_dir?directoryId=${directoryId}`,
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${getToken()}`
            }
        }
    },

    fsParseDirPath(path, userId) { // 解析目录路径
        return {
            url: `${API_FS_URL}/parse_dir_path`,
            method: 'POST',
            data: {
                path: path,
                userId: userId
            },
            headers: {
                'Authorization': `Bearer ${getToken()}`
            }
        }
    },

    fsParseFilePath(path, userId) { // 解析文件路径
        return {
            url: `${API_FS_URL}/parse_file_path`,
            method: 'POST',
            data: {
                path: path,
                userId: userId
            },
            headers: {
                'Authorization': `Bearer ${getToken()}`
            }
        }
    },

    fsGetFilePath(fileId) { // 获取文件路径
        return {
            url: `${API_FS_URL}/get_file_path?fileId=${fileId}`,
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${getToken()}`
            }
        }
    },

    fsGetDirPath(directoryId) {
        return {
            url: `${API_FS_URL}/get_dir_path?directoryId=${directoryId}`,
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${getToken()}`
            }
        }
    }
}
