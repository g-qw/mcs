<template>
    <component v-if="popup.isPopup" :is="popup.popupComponent" :type="popup.popupType">{{ popup.message }}</component>

    <div class="file-explorer">
        <div class="tool-bar">
            <div class="path-container">
                <svg-icon icon-class="path" color="#8d96a0" size="1em"></svg-icon>
                <span class="dir-node " 
                v-for="node in dirNodes"
                @click="switchToDirectory(node)">
                &#8239; {{ getDirNodeName(node) }} &#8239;
                </span>
            </div>

            <div id="multi-download" @click="multiDownload()">
                <svg-icon icon-class="multi-download" color="#8d96a0" size="1.25em"></svg-icon> 
            </div>

            <div id="refresh" @click="refreshStorage()">
                <svg-icon icon-class="refresh" color="#8d96a0" size="1.25em"></svg-icon> 
            </div>

            <div id="new-folder" @click="newFolder()">
                <svg-icon icon-class="mkdir" color="#8d96a0" size="1.25em"></svg-icon>
            </div>
            
            <div id="download-task" @click="taskWindow.show = true">
                <svg-icon icon-class="task" color="#8d96a0" size="1.25em"></svg-icon>
            </div>
        </div>

        <div class="storage-content">
            <div class="property">
                <div class="name">名称</div>
                <div class="create-date">创建日期</div>
                <div class="modify-date">修改日期</div>
                <div class="type">类型</div>
                <div class="size">大小</div>
            </div>

            <div class="fs-content">
                <div class="fs-folder" v-for="(folder, index) in fsFolders" @dblclick="enterDirectory(folder)">
                    <div class="select"></div>
                    <div class="name">
                        <svg-icon class="folder-icon" icon-class="folder" color="#8d96a0" size="1.5em" />
                        <input class="folder-name" 
                        v-model="folder.name" 
                        @blur="updateFolderName(folder)"
                        @keyup.enter="$event.target.blur()">
                    </div>
                    <div class="created-at">{{ folder.createdAt}}</div>
                    <div class="updated-at">{{ folder.updatedAt }}</div>
                    <div class="type">文件夹</div>
                    <div class="size"></div>
                    <div class="buttons">
                        <svg-icon class="download-icon" icon-class="download" color="#8d96a0" size="1.5em" @click="" />
                        <svg-icon class="delete-icon" icon-class="delete" color="#8d96a0" size="1.25em" @click="deleteDirectory(index)" />
                    </div>
                </div>

                <div class="fs-file" 
                v-for="(file, index) in fsFiles" 
                @contextmenu.prevent = "openContextMenu($event, index)">
                    <div class="select">
                        <input type="checkbox" 
                        v-model="selectedFiles"
                        :value="index">
                    </div>
                    <div class="name">
                        <svg-icon class="file-icon" :icon-class="getFileIcon(file.objectName)" color="#8d96a0" size="1.75em" />
                        <div class="file-name">{{ file.objectName }}</div>
                    </div>
                    <div class="created-at">{{ file.createdAt }}</div>
                    <div class="updated-at">{{ file.updatedAt }}</div>
                    <div class="type"> {{ getFileType(file.objectName) }}</div>
                    <div class="size">{{ formatBytes(file.size) }}</div>
                    <div class="buttons">
                        <svg-icon class="download-icon" icon-class="download" color="#8d96a0" size="1.5em" @click="downloadFile(index)" />
                        <svg-icon class="delete-icon" icon-class="delete" color="#8d96a0" size="1.25em" @click="deleteFile(index)" />
                    </div>
                </div>
            </div>
        </div>

        <div :class="{upload: true, disabled: isDisabled}">
            <form method="post" class="upload-form" enctype="multipart/form-data" @submit.prevent="upload">
                <transition name="fade">
                    <div id="select" v-show="uploadStatus === 'uploading'">
                        <div class="upload-info">
                            <svg-icon icon-class="upload" color="#8d96a0" size="2em"></svg-icon>
                            <span>上传文件至云盘，请选择文件或文件夹</span>
                        </div>
                        <input type="file" id="upload-input" name="file[]" multiple="multiple" @change="loadFileList"/>
                    </div>
                </transition>

                <transition name="fade">
                    <div id="submit" v-show="uploadStatus === 'ready'" @click="isDisabled = true">
                        <div class="submit-info">
                            <svg-icon icon-class="submit" color="#8d96a0" size="2em"></svg-icon>
                            <span>发送到云盘</span>
                        </div>
                        <input type="submit" id="files-submit">
                    </div>
                </transition>
            </form>
        </div>

        <transition name="fade">
            <div class="file-list" v-show="isShowFileList" ref="fileListRef">
                <div class="title">已选择以下文件：</div>
                <div class="files-content">
                    <div v-for="(fileItem) in fileList" class="file-item">
                        <div class="file-name">{{ fileItem.name }}</div>
                        <ProgressBar :progress=fileItem.progress />
                        <div class="file-size">{{ formatBytes(fileItem.size) }}</div>
                        <svg-icon icon-class="file-delete" color="#8d96a0" size="1.2em" @click="removeFile(fileItem.id)"></svg-icon>
                    </div>
                </div>
                <div class="clear" @click="resetUpload">
                    <svg-icon icon-class="reset" color="rgba(255, 255, 255, 0.6)" size="1.5em"></svg-icon>
                    <span>重新上传</span>
                </div>
            </div>
        </transition>

        <transition name="fade">
            <ul id="context-menu" v-if="showContextMenu" ref="contextMenu">
                <li>
                    <div class="download-selected" @click="multiDownload()">
                        <svg-icon icon-class="download" color="#8d96a0" size="1.5em"></svg-icon>
                        <span>下载</span>
                    </div>
                </li>
                <li>
                    <div class="delete-selected" @click="deleteSelected()">
                        <svg-icon icon-class="delete" color="#8d96a0" size="1.3em"></svg-icon>
                        <span>删除</span>
                    </div>
                </li>
            </ul>
        </transition>
    </div>

    <!-- 下载任务窗口 -->
    <transition name="fade">
        <div
            class="download-window"
            v-show="taskWindow.show"
            @mousedown="startDrag"
            @mousemove="drag"
            @mouseup="stopDrag"
            :style="windowStyle"
            :class="{dragging: taskWindow.isDragging}">
            <div class="window-header">
                <div>
                    <svg-icon icon-class="task" color="#8d96a0" size="1.25em"></svg-icon>
                    &nbsp;下载任务
                </div>
                    
                <div class="buttons">
                    <svg-icon
                        icon-class="clean"
                        color="#8d96a0"
                        size="1.25em"
                        @click="cleanTask()">
                    </svg-icon>
                
                    <svg-icon
                        icon-class="min"
                        color="#8d96a0"
                        size="1.25em"
                        @click="taskWindow.show = false">
                    </svg-icon>
                </div>
            </div>
            <div class="task-list">
                <div
                    v-for="(task, index) in downloadTasks"
                    :key="task.taskId"
                    class="task-item">
                    <svg-icon
                        class="task-icon"
                        :icon-class="getFileIcon(task.taskName)"
                        color="#8d96a0" size="1.25em">
                    </svg-icon>
                    <div class="task-name">{{ task.taskName }}</div>
                    <ProgressBar :progress="task.progress" />
                    <div class="download-progress">
                        <span class="downloaded">{{ formatBytes(task.downloaded) }}</span>
                        / 
                        <span class="total">{{ formatBytes(task.size) }}</span>
                    </div>
                    <svg-icon
                        icon-class="file-delete"
                        class="delete-icon"
                        color="#8d96a0"
                        size="1em"
                        @click="removeTask(index)"
                    ></svg-icon>
                </div>
            </div>
        </div>
    </transition>
</template>

<script>
import { apiEndpoints } from '@/api/api';
import axios from 'axios';
import ProgressBar from '@/components/ProgressBar.vue';
import { useFileExplorerStore } from '@/stores/fileExplorer';
import { storeToRefs } from 'pinia';
import SvgIcon from '@/components/SvgIcon.vue';
import { fileIconMap, fileTypeMap } from '@/stores/fileTypeMap';
import { reactive } from 'vue';

export default {
    components: {
        ProgressBar
    },
    setup() {
        const fe = useFileExplorerStore();

        // 使用 storeToRefs 将 store 中的状态转换为响应式引用
        const { userInfo, bucket, path, dir, files, folders, dirNodes, selectedFiles, downloadTasks} = storeToRefs(fe);


        return {
            userInfo,
            bucket,
            path,
            dir,
            fsFiles: files,
            fsFolders: folders,
            dirNodes,
            selectedFiles,
            downloadTasks
        };
    },
    data() {
        return {
            popup: {
                isPopup: false, // 控制弹窗显示状态
                popupComponent: 'Popup', //弹窗组件
                popupType: '', //弹窗类型
                message: '', //弹窗消息
            },
            isFlush: false, // 标识页面数据是否需要刷新
            isShowFileList: false,
            uploadStatus: "uploading",
            isDisabled: false,
            files: [], // 上传的文件对象列表
            ignoreFiles: [], // 忽略的文件对象的索引列表
            fileList: [], // 已选择的文件信息列表
            showContextMenu: false, //是否显示选项栏
            taskWindow: {
                show: false, // 是否显示窗口
                isDragging: false, // 是否正在拖拽
                startX: 0, // 鼠标按下时的初始X坐标
                startY: 0, // 鼠标按下时的初始Y坐标
                initialX: 0, // 窗口初始X坐标
                initialY: 0, // 窗口初始Y坐标
            },
        }
    },
    computed: {
        // 动态计算窗口的样式
        windowStyle() {
            return {
                transform: `translate(${this.taskWindow.initialX}px, ${this.taskWindow.initialY}px)`,
            };
        },
    },
    watch: {
        isFlush(newValue, oldValue) {
            if (newValue === true) {
                this.loadDirectory();
                this.isFlush = false;
            }
        }
    },
    methods: {
        // 弹出弹窗
        async createPopup(type = 'info', message) {
            this.popup.isPopup = true;
            this.popup.popupType = type;
            this.popup.message = message;
            setTimeout(() => {
                this.popup.isPopup = false;
            }, 10000); // 弹窗显示 10 秒
        },
        /* 格式化字节数 */
        formatBytes(bytes) {
            if (bytes === 0) return '0 Bytes';
            const k = 1024; // 1KB = 1024Bytes
            const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB', 'PB'];
            const i = Math.floor(Math.log(bytes) / Math.log(k));
            return `${parseFloat((bytes / Math.pow(k, i)).toFixed(2))} ${sizes[i]}`;
        },

        /* 获取文件的扩展名 */
        getExtension(filename) {
            let extension = filename.split('.').pop();
            if (extension.trim() === '') {
                return '.bin';
            }
            return "." + extension;
        },

        // 获取文件的类型描述
        getFileType(filename) {
            const extension = '.' + filename.split('.').pop();
            const type = fileTypeMap.get(extension);

            if (type) {
                return type;
            } else {
                return "未知类型";
            }
        },

        // 获取文件的图标名称
        getFileIcon(filename) {
            const extension = '.' + filename.split('.').pop();
            const icon = fileIconMap.get(extension);

            if (icon) {
                return icon;
            } else {
                return "other";
            }
        },

        /* 将选择上传的文件对象信息加载到 fileList 中 */
        loadFileList(event) {
            this.fileList = [];
            this.files = event.target.files;
            console.log(this.files);
            for (let i = 0; i < this.files.length; i++) {
                this.fileList.push(
                    {
                        "id": i,
                        "name": this.files[i].name,
                        "size": this.files[i].size,
                        "progress": 0.0,
                    }
                );
            }
            this.isShowFileList = true;
            this.uploadStatus = "ready";

            // 使用 $nextTick 等待 DOM 更新
            this.$nextTick(() => {
                const ref = this.$refs.fileListRef;
                if (ref) {
                    ref.scrollIntoView({ behavior: 'smooth', block: 'end', inline: 'nearest' });
                }
            });
        },

        /* 标记要忽略上传的文件 */
        removeFile(id) {
            const index = this.fileList.findIndex(file => file.id === id); // 查询索引
            if (index !== -1) {
                this.fileList.splice(index, 1); // 在文件信息列表删除该文件
            }
            this.ignoreFiles.push(id);// 忽略已删除的文件
        },

        /* 重置上传 */
        resetUpload() {
            this.fileList = [];
            this.isShowFileList = false;
            this.uploadStatus = "uploading";
            this.isDisabled = false;
        },

        async upload() {
            const SMALL_FILE_SIZE = 10 * 1024 * 1024; // 50 MB 以下为小型文件
            const MEDIUM_FILE_SIZE = 100 * 1024 * 1024; // 50MB - 100MB 为中型文件

            // 初始化分类数组，存储它们在 files 文件对象数组的索引
            let smallFiles = [];
            let mediumFiles = [];
            let largeFiles = [];

            for (let i = 0; i < this.files.length; i++) {
                if (this.ignoreFiles.includes(i)) continue; // 忽略用户不上传的文件

                // 获取文件大小
                const fileSize = this.files[i].size;

                // 根据文件大小分类
                if (fileSize < SMALL_FILE_SIZE) {
                    smallFiles.push(i); // 小型文件
                } else if (fileSize < MEDIUM_FILE_SIZE) {
                    mediumFiles.push(i); // 中型文件
                } else {
                    largeFiles.push(i); // 大型文件
                }
            }

            console.log("小型文件索引:", smallFiles);
            console.log("中型文件索引:", mediumFiles);
            console.log("大型文件索引:", largeFiles);

            // 小型文件采用多文件上传方式，减少请求次数
            if(smallFiles.length > 0)
                this.uploadSmallFiles(smallFiles);

            // 中型文件使用单文件上传方式
            mediumFiles.forEach(index => {
                this.uploadMediumFile(index);    
            });

            // 大型文件采用分块上传方式
            largeFiles.forEach(index => {
                this.uploadLargeFile(index);
            });
        },

        /*
            一次性上传多个小型文件
            后端接口直接以文件名称作为文件的绝对路径
            @index 指定要上传的文件在 files 对象列表的索引构成的列表
        */
        async uploadSmallFiles(indexs) {
            const startTime = Date.now(); // 记录整个小文件上传流程的开始时间
            const newFiles = [];
            const fileNames = [];
            const fileItems = this.fileList.filter(file => indexs.includes(file.id)); // 获取小文件的文件信息元素列表，用于更新进度条

            for (let i = 0; i < indexs.length; i++) {
                const file = this.files[indexs[i]];
                const objectName = `${this.path}${file.name}`;

                // 创建新的 File 对象, 这里会创建文件的副本，不过这里是处理小文件影响不大
                const newFile = new File([file], objectName, {
                    type: file.type,
                    lastModified: file.lastModified
                });

                newFiles.push(newFile);
                fileNames.push(objectName); // 收集文件名称
            }

            axios(apiEndpoints.uploadFiles(this.bucket, newFiles, fileItems))
                .then(
                    response => {
                        if (response.data.code === 200) {
                            const endTime = Date.now(); // 记录上传的结束时间
                            const duration = (endTime - startTime) / 1000; // 计算耗时，单位(秒)
                            console.log(`${newFiles.length} 个文件上传成功！耗时 ${duration.toFixed(2)} 秒`);
                            console.log("所有小文件上传成功：\n", fileNames.join('\n'));
                            this.createPopup('success', `${newFiles.length} 个小文件已上传成功！`);

                            // 将文件信息添加到文件系统
                            newFiles.forEach(file => {
                                let fileName = file.name.split('/').pop(); // 提取文件名, 构造的新文件的名称为绝对路径
                                this.createFile(fileName, file.type, file.size);
                            });
                        } else {
                            this.createPopup('error', response.data.msg || `上传 ${newFiles.length} 个小文件失败`);
                        }
                    }
                ).catch(
                    error => {
                        console.error(`上传 ${indexs.length} 个小型文件发生错误:`, error);
                        this.createPopup('error', `上传小型文件发生错误:\n${fileNames.join('\n')}`);
                    }
            );
        },

        /* 
            上传中型文件，采用单文件上传方式
            @index 指定要上传的文件在 files 对象列表的索引
         */
        async uploadMediumFile(index) {
            const file = this.files[index];
            const fileItem = this.fileList.find(file => file.id === index);// 找到该中型文件的文件信息元素，用于更新进度条
            const fileName = file.name;
            const objectName = `${this.path}${fileName}`;
            const fileSize = this.formatBytes(file.size); // 文件的大小字符串
            const startTime = Date.now(); // 记录上传的开始时间

            axios(apiEndpoints.uploadFile(this.bucket, objectName, file, fileItem))
                .then(
                    response => {
                        if (response.data.code === 200) {
                            const endTime = Date.now(); // 记录上传的结束时间
                            const duration = (endTime - startTime) / 1000; // 计算耗时，单位(秒)
                            console.log(`文件 ${fileName} [${fileSize}] 上传成功！耗时 ${duration.toFixed(2)} 秒`);

                            this.createPopup('success', `上传 ${fileName} [${fileSize}] 成功！`);

                            // 将文件信息添加到文件系统
                            this.createFile(fileName, file.type, file.size);
                        } else {
                            this.createPopup('error', response.data.msg || `上传 ${fileName} [${fileSize}] 失败`);
                        }
                    }
                )
                .catch(
                    error => {
                        console.error(`上传中型文件 ${fileName} [${fileSize}] 发生错误:`, error);
                        this.createPopup('error', `上传中型文件 ${fileName} [${fileSize}] 发生错误:`);
                    }
                );
        },

        /*
            大型文件，采用分块上传
            @index 指定要上传的文件在 files 对象列表的索引
        */
        async uploadLargeFile(index) {
            const file = this.files[index];
            const fileItem = this.fileList.find(file => file.id === index);// 找到该中型文件的文件信息元素，用于更新进度条
            const fileName = file.name;
            const contentType = file.type;
            const fileSize = this.formatBytes(file.size);
            const objectName = `${this.path}${fileName}`

            // 初始化分块上传
            axios(apiEndpoints.initMultipartUpload(this.bucket, objectName, contentType))
                .then(
                    response => {
                        if (response.data.code === 200) {
                            let uploadId = response.data.data;
                            this.uploadParts(file, uploadId, objectName, contentType, fileItem);
                            console.log(`初始化大型文件 ${fileName} [${fileSize}] 的分块上传成功！\n上传任务ID：${uploadId}`);

                            // 将文件添加到文件系统
                            this.createFile(fileName, file.type, file.size);
                        } else {
                            this.createPopup('error', response.data.msg || `上传大型文件 ${fileName} [${fileSize}] 失败！`);
                        }
                    }
            ).catch(
                error => {
                    console.log(`初始化大型文件 ${fileName} [${fileSize}] 的分块上传失败！`, error);
                    this.createPopup('error', `上传大型文件 ${fileName} [${fileSize}] 失败！`);
                }
            );
        },

        /**
         * 上传分块
         * @param file 大文件对象的分块
         * @param uploadId 上传任务ID
         * @param objectName 文件的上传位置的绝对路径
         * @param contentType MIME类型
         * @param fileItem 文件信息元素，用于更新进度条
         */
        async uploadParts(file, uploadId, objectName, contentType, fileItem) {
            const chunkSize = 10 * 1024 * 1024; // 每个分块大小为 10MB
            const totalChunks = Math.ceil(file.size / chunkSize);
            const startTime = Date.now(); // 记录整个分块上传流程的开始时间

            // 创建一个数组来存储每个分块上传的 Promise
            const uploadPromises = [];

            for (let partNumber = 1; partNumber <= totalChunks; partNumber++) {
                const start = (partNumber - 1) * chunkSize;
                const end = Math.min(start + chunkSize, file.size);
                const filePart = file.slice(start, end);

                // 使用 FileReader 读取分块数据
                const reader = new FileReader();
                const chunkDataPromise = new Promise((resolve, reject) => {
                    reader.onload = (event) => {
                        resolve(event.target.result);
                    };
                    reader.onerror = (error) => {
                        reject(new Error(`读取分块 ${partNumber} 时出错: ${error}`));
                    };
                    reader.readAsArrayBuffer(filePart); // 读取为 ArrayBuffer
                });

                // 等待读取完成，并将上传 Promise 添加到数组中
                const chunkData = await chunkDataPromise;
                uploadPromises.push(this.uploadChunk(chunkData, uploadId, objectName, partNumber, totalChunks, fileItem));
            }

            
            try {
                await Promise.all(uploadPromises); // 等待所有分块上传完成
                await this.completeUpload(uploadId, objectName, contentType); // 所有分块上传完成后，调用合并方法

                const endTime = Date.now();
                const totalDurationInSeconds = (endTime - startTime) / 1000;
                console.log(`所有分块上传完成！上传大型文件 ${objectName} 总耗时 ${totalDurationInSeconds.toFixed(2)} 秒`);
            } catch (error) {
                console.error("分块上传过程中出现错误，无法完成合并", error);
                this.createPopup('error', `分块上传过程中出现错误：${error.message}`);
            }
        },

        /*
            上传单个分块
        */
        async uploadChunk(chunkData, uploadId, objectName, partNumber, totalChunks, fileItem) {
            const startTime = Date.now(); // 记录分块上传的开始时间
            const response = await axios(apiEndpoints.uploadPart(this.bucket, objectName, uploadId, partNumber, chunkData));

            // 检查响应状态
            if (response.data.code === 200) {
                const endTime = Date.now(); // 记录分块上传的结束时间
                const duration = (endTime - startTime) / 1000; // 计算耗时，单位(秒)
                console.log(`${objectName} 分块 ${partNumber}/${totalChunks} 上传成功！耗时 ${duration.toFixed(2)} 秒`);

                fileItem.progress += 100.0 / totalChunks; // 更新进度条
            } else {
                throw new Error(response.data.msg || `${objectName} 分块 ${partNumber}/${totalChunks} 上传失败！`);
            }
        },

        /* 合成分块 */
        async completeUpload(uploadId, objectName, contentType) {
            axios(apiEndpoints.uploadComplete(this.bucket, objectName, uploadId, contentType))
            .then(
                response => {
                    if (response.data.code === 200) {
                        this.createPopup('success', `大型文件 ${objectName} 上传成功！`);
                    } else {
                        this.createPopup('error', response.data.msg || `${objectName} 分块上传合并失败！`);
                    }
                }
            )
            .catch(
                error => {
                    console.error(`分块合并失败！`, error);
                    this.createPopup('error', `${objectName} 分块上传合并失败！`);
                }
            );
        },

        /* 将文件信息上传到文件系统 */
        createFile(objectName, type, size) {
            axios(apiEndpoints.fsAddFile(this.dir.directoryId, this.bucket, objectName, type, size))
            .then(
                response => {
                    if (response.data.code === 200) {
                        this.createPopup("success", `将 ${objectName} 添加到文件系统成功！`);
                    } else {
                        console.log(`将 ${objectName} 添加到文件系统失败`);
                        this.createPopup("error", response.data.msg || `将 ${objectName} 添加到文件系统失败！`)
                    }
                }    
            ).catch(
                error => {
                    console.log(`将 ${objectName} 添加到文件系统失败:`, error);
                    this.createPopup("error", `将 ${objectName} 添加到文件系统发生错误！`)
                }
            )
        },
        /* 刷新当前目录的数据 */
        refreshStorage() {
            this.loadDirectory();
        },
        /* 创建新文件夹 */
        newFolder() {
            const currentMills = Date.now();
            const newFolderName = `新建文件夹_${currentMills}`;
            axios(apiEndpoints.fsCreateDir(this.dir.directoryId, this.bucket, newFolderName))
            .then(
                response => {
                    if (response.data.code === 200) {
                        const folder = response.data.data;
                        this.fsFolders.push(folder);
                        console.log(`创建 ${this.path}${newFolderName} 成功`);
                    } else {
                        this.createPopup("error", "创建新文件夹失败！");
                    }
                }
            ).catch(
                error => {
                    console.log("创建新文件夹发生错误：", error);
                    this.createPopup("error", "创建新文件夹发生错误！");
                }
            )
        },
        /* 根据dir的目录ID加载当前目录数据 */
        async loadDirectory() {
            if (!this.dir.directoryId) {
                console.log('没有获取到当前目录信息，无法加载当前目录数据');
                return;
            }

            axios(apiEndpoints.fsLoadDir(this.dir.directoryId))
            .then(
                response => {
                    if (response.data.code === 200) {
                        const { path, directories, files } = response.data.data;
                        this.path = path;
                        this.fsFolders = directories;
                        this.fsFiles = files;
                        console.log(`目录 ${this.path} 加载数据完毕`);
                    } else {
                        this.createPopup("error", response.data.msg || `目录 ${this.path} 加载数据失败!`);
                    }
                } 
            ).catch(
                error => {
                    console.log(`加载目录 ${this.path} 发生错误：`, error);
                    this.createPopup("error", `加载目录 ${this.path} 发生错误`);
                }
            )
        },

        /* 获取文件夹名称 */
        async getFolderName(directoryId) {
            try {
                const response = await axios(apiEndpoints.fsGetDirName(directoryId));
                if (response.data.code === 200) {
                    return response.data.data;
                } else {
                    return Date.now();
                }
            } catch (error) {
                console.log('文件夹名称为空，请求文件夹名称失败:', error);
                return Date.now();
            }
        },

        /* 获取文件名称 */
        async getFileName(fileId) {
            try {
                const response = await axios(apiEndpoints.fsGetFileName(fileId))

                if (response.data.code === 200) {
                    return response.data.data;
                } else {
                    return Date.now();
                }
            } catch (error) {
                console.log('文件名称为空，请求文件名称失败：', error);
                return Date.now();
            }
        },

        /* 获取当前的日期时间字符串  */
        getCurrentDateTime() {
            const now = new Date();
            const year = now.getFullYear(); // 获取年份
            const month = String(now.getMonth() + 1).padStart(2, '0'); // 获取月份，+1 是因为月份从 0 开始
            const day = String(now.getDate()).padStart(2, '0'); // 获取日期
            const hours = String(now.getHours()).padStart(2, '0'); // 获取小时
            const minutes = String(now.getMinutes()).padStart(2, '0'); // 获取分钟
            const seconds = String(now.getSeconds()).padStart(2, '0'); // 获取秒
            return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
        },

        /* 更新文件夹名称 */
        async updateFolderName(folder) {
            const name = folder.name;
            const directoryId = folder.directoryId;
            const originName = await this.getFolderName(directoryId);
            if (name.trim() === '') {
                this.createPopup("warning", "文件夹名称不能为空");
                folder.name = originName;
                return;
            }

            const illegalCharRegex = /[/\\:*?"<>|]/;
            if (illegalCharRegex.test(name)) {
                this.createPopup("warning", "文件夹名称包含非法路径字符");
                folder.name = originName;
                return;
            }
            
            axios(apiEndpoints.fsUpdateDirName(directoryId, name))
            .then(
                response => {
                    if (response.data.code !== 200) {
                        this.createPopup('warning', response.data.msg || `修改文件夹名称为 ${name} 失败`);
                        folder.name = originName;
                    }
                }
            ).catch(
                error => {
                    console.log("修改文件夹名称发生错误:", error);
                    folder.name = originName;
                    this.createPopup("error", "修改文件夹名称发生错误");
                }
            )

            folder.updatedAt = this.getCurrentDateTime();
        },

        /* 更新文件名称 */
        updateFileName(file) {
            const fileId = file.fileId;
            const objectName = file.objectName;
            const originName = this.getFileName(file.fileId);

            if (objectName.trim() === '') {
                this.popup("warning", "文件名称不能为空");
                file.objectName = originName;
                return;
            }

            const illegalCharRegex = /[/\\:*?"<>|]/;
            if (illegalCharRegex.test(name)) {
                this.createPopup("warning", "文件名称包含非法路径字符");
                file.objectName = originName;
                return;
            }

            axios(apiEndpoints.fsUpdateFileName(fileId, objectName))
            .then(
                response => {
                    if (response.data.code === 200) {
                        console.log(`更新文件名称：${objectName}`);
                    } else {
                        this.createPopup('error', `更新文件名称 ${objectName} 失败`);
                        file.objectName = originName;
                    }
                }
            ).catch(
                error => {
                    console.log(`更新文件名称 ${objectName} 发生错误`, error);
                    this.createPopup('error', `更新文件名称 ${objectName} 发生错误`);
                }
            )
        },

        /* 删除文件 */
        async deleteFile(index) {
            const file = this.fsFiles[index];
            const fileId = file.fileId;
            const objectName = file.objectName;

            axios(apiEndpoints.fsDeleteFile(fileId))
            .then(
                response => {
                    if (response.data.code === 200) {
                        console.log(`删除文件 ${objectName} 成功! fileId: ${fileId}`);
                        this.fsFiles.splice(index, 1);
                        this.createPopup("info", `删除文件 ${objectName} 成功!`);
                    } else {
                        console.log(`删除文件 ${objectName} 失败!`);
                        this.createPopup("error", response.data.msg || `删除文件 ${objectName} 失败!`);
                    }
                }
            ).catch(
                error => {
                    console.log(`删除文件 ${objectName} 发生错误：`, error);
                    this.createPopup("error", `删除文件 ${objectName} 发生错误!`)
                }
            )
        },
        /** 
            下载文件，根据文件大小进行自动选择下载方式 
            size <= 10 MB: 单文件下载
            size > 10 MB: 分块下载
            @param index 文件在 this.fsFiles 中的索引
        */
        async downloadFile(index) {
            const file = this.fsFiles[index]; // 获取文件信息对象
            const SMALL_FILE_SIZE = 10 * 1024 * 1024; 

            // 创建task对象，包含file的属性，并添加progress属性
            const task = reactive({
                taskId: file.fileId, // 任务ID
                taskName: file.objectName, // 下载任务名称
                size: file.size, // 文件总字节数
                downloaded: 0.0, // 已下载的字节数
                progress: 0.0, // 初始化progress属性为0
                status: 'waiting' //任务状态
            });

            this.downloadTasks.push(task); // 将任务插入到任务数组开头

            // 获取文件路径
            const filePath = await this.getFilePath(file.fileId);
            if (filePath === '') { // 获取失败
                return;
            }

            // 根据文件大小选择不同的下载方式
            if (file.size <= SMALL_FILE_SIZE) {
                this.downloadSmallFile(filePath, file, task);
            } else {
                this.multipartDownload(filePath, file, task);
            }
        },

        /**
         * 获取文件在minio的绝对路径
         * @param fileId 文件ID
         * @param fileName 文件名称
         */
         async getFilePath(fileId, fileName) {
            try {
                const response = await axios(apiEndpoints.fsGetFilePath(fileId));
                if (response.data.code === 200) {
                    let filePath = response.data.data;
                    console.log(`初始化文件 ${fileId} 的下载：${filePath}`);
                    return filePath;
                } else {
                    console.log(`下载文件 ${fileName} 失败：无法获取到文件路径。`, error);
                    this.createPopup(`下载文件 ${fileName} 失败`);
                }
            } catch (error) {
                console.log(`下载文件 ${fileName} 失败：无法获取到文件路径。`, error);
                this.createPopup(`下载文件 ${fileName} 失败：无法获取到文件路径。`);
                return '';
            }
        },

        /**
         * 下载单个小文件
         * @param filePath 文件在minio的绝对路径
         * @param fileItem 文件信息对象
         * @param task 下载任务对象
         */
         async downloadSmallFile(filePath, fileItem, task) {
            const startTime = Date.now(); // 记录开始下载的时间
            let fileName = fileItem.objectName;
            const mimeType = fileItem.mimeType;
            const formatSize = this.formatBytes(fileItem.size);

            // 发起下载请求
            axios(apiEndpoints.downloadFile(this.bucket, filePath, task))
            .then(response => {
                if (response.status === 200) {
                    if (!fileName) {
                        fileName = filePath.split('/').pop();
                    }

                    // 创建一个 Blob 对象，并创建一个可下载的链接
                    const data = response.data;
                    const blob = new Blob([data], { type: mimeType });
                    const downloadUrl = URL.createObjectURL(blob);

                    // 创建一个 <a> 标签并点击触发浏览器下载
                    const link = document.createElement('a');
                    link.href = downloadUrl;
                    link.download = fileName;
                    document.body.appendChild(link);
                    link.click();

                    // 释放对象 URL
                    URL.revokeObjectURL(downloadUrl); // 释放 blob
                    document.body.removeChild(link);

                    const endTime = Date.now(); // 记录下载的结束时间
                    const duration = (endTime - startTime) / 1000; // 计算耗时，单位(秒)
                    task.status = 'complete';
                    // setTimeout(() => {
                    //     const index = this.downloadTasks.findIndex(e => e.taskId === task.taskId);
                    //     this.downloadTasks.splice(index);
                    // }, 5000)
                    console.log(`下载文件 ${fileName} [${formatSize}] 成功！耗时 ${duration.toFixed(2)} 秒`);
                } else {
                    console.log(`下载 ${filePath} 失败！`);
                    this.createPopup("error", `下载 ${fileName} 失败！`);
                }
            })
            .catch(error => {
                console.log(`下载 ${filePath} 发生错误`, error);
                this.createPopup("error", `下载 ${fileName} 发生错误！`);
            });
        },
        /* 分块下载 */
        async multipartDownload(filePath, fileItem, task) {
            try {
                // 分块下载
                const fileName = fileItem.objectName; // 文件的名称
                const size = fileItem.size; // 文件的总字节数
                const mimeType = fileItem.mimeType; // 文件的 MIME 类型
                const chunkSize = 10 * 1024 * 1024; // 分块大小
                const startTime = Date.now(); // 记录开始下载的时间
                const formatSize = this.formatBytes(size);

                // 计算分块范围
                let start = 0;
                let end = start + chunkSize - 1;
                const downloadedChunks = []; // 存储下载的分块数据

                while (start < size) {
                    const chunkStart = Date.now();
                    end = Math.min(end, size - 1); // 确保最后一个分块不超过文件大小
                    let actualChunkSize = end - start; // 分块的实际字节数
                    let chunkProgress = (end - start) * 100.0 / size; // 计算单分块贡献的进度
                    try {
                        await axios(apiEndpoints.downloadPart(this.bucket, filePath, start, end))
                        .then(
                            response => {
                                if (response.status === 206) {
                                    const arrayBuffer = response.data;
                                    const downloadedChunk = new Uint8Array(arrayBuffer);
                                    downloadedChunks.push(downloadedChunk);
                                    task.downloaded += actualChunkSize; // 记录下载的字节数
                                    task.progress += chunkProgress; // 记录分块进度

                                    const chunkEnd = Date.now();
                                    const duration = (chunkEnd - chunkStart) / 1000; // 计算耗时，单位(秒)
                                    
                                    console.log(`下载分块 ${start}-${end}/${size} 成功！总耗时 ${duration.toFixed(2)} 秒`);
                                } else {
                                    throw new Error(`分块请求失败 ${response.status}`);
                                }
                            }
                        );
                    } catch (error) {
                        console.error(`下载分块 ${start}-${end} 时发生错误，终止下载`, error);
                        this.createPopup("error", `下载分块 ${start}-${end}/${size} 时发生错误，终止下载`);
                        return; // 终止下载流程
                    }
                    start += chunkSize;
                    end += chunkSize;
                }

                // 合并分块数据
                const mergedBlob = new Blob(downloadedChunks, { type: mimeType });
                const url = URL.createObjectURL(mergedBlob);

                // 触发文件下载
                const a = document.createElement("a");
                a.href = url;
                a.download = fileName;
                document.body.appendChild(a);
                a.click();
                task.status = 'complete'; // 标记任务完成

                // 释放对象 URL
                document.body.removeChild(a);
                URL.revokeObjectURL(url);

                const endTime = Date.now(); // 记录下载的结束时间
                const duration = (endTime - startTime) / 1000; // 计算耗时，单位(秒)
                console.log(`下载文件 ${fileName} [${formatSize}] 成功！耗时 ${duration.toFixed(2)} 秒`);
            } catch (error) {
                console.log('分块下载发生错误', error);
                this.createPopup("error", `分块下载 ${filePath} 遇到错误`);
            }
        },
        /* 获取目录路径中的目录节点的名称 */
        getDirNodeName(directory) {
            if (directory.name === '/') {
                return '/';
            } else {
                // 如果名称长度超过15个字符，截取前15个字符并添加省略号
                let name = directory.name;
                if (name.length > 15) {
                    name = name.slice(0, 15) + '...';
                }
                return `${name} /`;
            }
        },
        /* 进入目录 */
        enterDirectory(folder) {
            this.dir = folder;
            this.path = `${this.path}/${folder.name}/`;
            this.dirNodes.push(folder);
            this.loadDirectory();
        },
        /* 切换目录 */
        switchToDirectory(directory) {
            // 如果切换至的目录与当前目录相同, 直接返回
            if (directory.directoryId === this.dir.directoryId) {
                return;
            }

            // 移除切换的节点后的目录节点
            const index = this.dirNodes.findIndex(node => node.directoryId === directory.directoryId);
            if (index !== -1) {
                this.dirNodes.splice(index + 1);
            }

            this.dir = directory; // 切换目录
            this.loadDirectory(); // 加载目录
        },
        /* 删除目录 */
        deleteDirectory(index) {
            const directory = this.fsFolders[index];
            const name = directory.name;
            axios(apiEndpoints.fsDeleteDir(directory.directoryId))
            .then(
                response => {
                    if (response.data.code === 200) {
                        console.log(`删除文件夹 ${name} 成功`);
                        this.fsFolders.splice(index, 1); // 删除目录对象
                        this.createPopup("success",`删除文件夹 ${name} 成功！`);
                    } else {
                        this.createPopup("warning", response.msg || `删除文件夹 ${name} 失败！`);
                    }
                }
            ).catch(
                error => {
                    console.log(`删除文件夹 ${name} 发生错误：`, error);
                    this.createPopup("error", `删除文件夹 ${name} 发生错误！`);
                }
            )
        },
        /* 多文件下载 */
        async multiDownload() {
            // 下载选中的文件
            for (const index of this.selectedFiles) {
                await this.downloadFile(index);
            }

            // 下载完成后清空 selectedFiles 数组
            this.selectedFiles = [];
        },
        /* 删除选中的文件 */
        async deleteSelected() {
            for (let i = this.selectedFiles.length - 1; i >= 0; i--) { // 从后往前删除更加高效
                await this.deleteFile(this.selectedFiles[i]);
                this.selectedFiles.splice(i, 1);
            }
        },
        /* 打开选项栏 */
        openContextMenu(event, index) {
            if (!this.selectedFiles.includes(index)) { // 如果没有选中
                this.selectedFiles.push(index); // 将当前要下载的文件索引添加到选中数组中
            }
            this.showContextMenu = true;//显示右键菜单

            // 使用 $nextTick 等待 DOM 更新
            this.$nextTick(() => {
                const contextMenu = this.$refs.contextMenu;
                if (contextMenu) {
                    // 计算菜单的显示位置
                    let x = event.clientX;
                    let y = event.clientY + window.scrollY;
                    contextMenu.style.left = x + 'px';
                    contextMenu.style.top = y + 'px';
                    contextMenu.style.display = 'flex';

                    // 单击隐藏选项栏
                    document.addEventListener(
                        'click',
                        () => {
                            this.showContextMenu = false; // 隐藏选项栏
                        },
                        { once: true }
                    );

                    // 按下 ESC 隐藏选项栏
                    document.addEventListener(
                        'keydown', 
                        () => {
                            this.showContextMenu = false; // 隐藏选项栏
                        },
                        { once: true }
                    );
                }
            });
        },

        /* 下载任务窗口 */
        startDrag(event) {
            const startX = event.clientX;
            const startY = event.clientY;
            const initialX = this.windowPosition.x;
            const initialY = this.windowPosition.y;

            const move = (e) => {
                this.windowPosition.x = initialX + (e.clientX - startX) / window.innerWidth * 100;
                this.windowPosition.y = initialY + (e.clientY - startY) / window.innerHeight * 100;
            };

            const stop = () => {
                document.removeEventListener('mousemove', move);
                document.removeEventListener('mouseup', stop);
            };

            document.addEventListener('mousemove', move);
            document.addEventListener('mouseup', stop);
        },
        /* 移除下载任务 */
        removeTask(index) {
            this.downloadTasks.splice(index, 1);
        },

        /* 清理已经完成的任务 */
        cleanTask() {
            for (let i = this.downloadTasks.length - 1; i >= 0; i--) { // 从后往前删，更加高效
                if (this.downloadTasks[i].status === 'complete') {
                    this.downloadTasks.splice(i, 1);
                }
            }
        },

        // 任务窗口开始拖拽，鼠标按下时触发
        startDrag(event) {
            if (this.taskWindow.isDragging) { // 如果已经按住拖拽了，直接返回
                return;
            }

            this.taskWindow.isDragging = true;
            this.taskWindow.startX = event.clientX;
            this.taskWindow.startY = event.clientY;
            this.taskWindow.initialX = this.taskWindow.initialX || 0;
            this.taskWindow.initialY = this.taskWindow.initialY || 0;
            // 阻止默认行为，避免选中文本等
            event.preventDefault();
        },
        // 鼠标移动时触发窗口移动
        drag(event) {
            if (!this.taskWindow.isDragging) {
                return;
            }

            // 计算移动的距离
            const deltaX = event.clientX - this.taskWindow.startX;
            const deltaY = event.clientY - this.taskWindow.startY;

            // 更新窗口位置
            this.taskWindow.initialX = this.taskWindow.initialX + deltaX;
            this.taskWindow.initialY = this.taskWindow.initialY + deltaY;

            // 重置起始点为当前鼠标位置
            this.taskWindow.startX = event.clientX;
            this.taskWindow.startY = event.clientY;
        },
        // 鼠标释放时结束拖拽
        stopDrag() {
            this.taskWindow.isDragging = false;
        },
    },
    async created() {
        try {
            // 初始化用户信息
            const userResponse = await axios(apiEndpoints.userInfo());
            if (userResponse.data.code === 200) {
                this.userInfo = userResponse.data.data;
                this.bucket = this.userInfo.userId;

                // 初始化当前目录信息
                let userId = this.userInfo.userId;
                try {
                    const dirResponse = await axios(apiEndpoints.fsParseDirPath(this.path, userId));
                    if (dirResponse.data.code === 200) {
                        this.dir = await dirResponse.data.data;
                        this.dirNodes.push(this.dir);
                        await this.loadDirectory(); // 加载当前目录的数据
                    } else {
                        this.createPopup('error', `加载当前目录 ${this.path} 的信息失败！请稍后重试`);
                    }
                } catch (error) {
                    console.log(`加载当前目录 ${this.path} 信息失败`, error);
                    this.createPopup('error', `加载当前目录 ${this.path} 的信息发生错误`);
                }
            } else {
                this.createPopup('warning', "登录状态已过期，请重新登录。即将跳转至登录页面...");
                setTimeout(() => {
                    const loginUrl = this.$router.resolve({ name: 'login' }).href;
                    window.location.href = loginUrl;
                },3000);
            }
        } catch (error) {
            console.log("加载用户数据失败：", error);
            this.createPopup('warning', "登录状态已过期，请重新登录。即将跳转至登录页面...");
            setTimeout(() => {
                const loginUrl = this.$router.resolve({ name: 'login' }).href;
                window.location.href = loginUrl;
            },3000);
        }
    }
}
</script>

<style>
/* 全局样式 */
* {
    box-sizing: border-box;
    margin: 0;
    padding: 0;
    font-family: 
        "Roboto", "Arial", "Helvetica", sans-serif, /* 英文字体 */
        "PingFang SC", "Source Han Sans SC", "Noto Sans SC", sans-serif; /* 中文字体 */
    color: #8d96a0;
}

.file-explorer {
  background: #161b22;
  overflow: auto;
}

/* 工具栏 */
.tool-bar {
    position: relative;
    left: 50%;
    height: 5vh;
    width: 85%;
    transform: translateX(-50%);
    margin-top: 5vh;
    display: flex;
    justify-content: flex-start;
    align-items: center;
    border: 1px solid #30363d;
    border-bottom: none;
    z-index: 1;
}

.path-container {
    width: 88%;
    display: flex;
    justify-content: start;
    align-items: center;
    margin-left: 1em;
    overflow-x: scroll;
    scrollbar-width: none;
}

.path-container .dir-node {
    position: relative;
    font-size: 0.9em;
    padding: 0.25em 0;
    padding-left: 0;
    text-align: center;
    white-space: nowrap;
    cursor: pointer;
    transition: all 0.3s ease;
    background-color: transparent;
}

.path-container .dir-node:first-child {
    margin-left: 1em;
}

.path-container .dir-node:hover {
    background-color: rgba(0, 128, 255, 0.1);
}

.path-container .dir-node:hover::after {
    content: "";
    position: absolute;
    bottom: 0;
    left: 0;
    width: 100%;
    height: 1px;
    background-color: rgba(0, 128, 255, 0.8); 
}

/* 覆盖浏览器的自动填充样式 */
input:-webkit-autofill,
input:-webkit-autofill:hover,
input:-webkit-autofill:focus,
input:-webkit-autofill:active {
    -webkit-box-shadow: 0 0 0 30px transparent inset !important;
    -webkit-text-fill-color: #8d96a0 !important;
    caret-color: #8d96a0 !important;
    transition: background-color 5000s ease-in-out 0s;
}

/* 针对Firefox的自动填充样式 */
input:-moz-autofill,
input:-moz-autofill:hover,
input:-moz-autofill:focus,
input:-moz-autofill:active {
    box-shadow: 0 0 0 30px transparent inset !important;
    color: #8d96a0 !important;
    caret-color: #8d96a0 !important;
}

#refresh, #new-folder, #multi-download, #download-task{
    position: absolute;
    cursor: pointer;
    transition: all 0.3s linear;
}

#refresh {
    right: 7.5vw;
}

#new-folder {
    right: 5.5vw;
}

#multi-download {
    right: 3.5vw;
}

#download-task {
    right: 1.5vw;
}

#refresh:hover svg use,
#new-folder:hover svg use ,
#download-task:hover svg use,
#multi-download:hover svg use{
    fill: #ddd;
    transition: all 0.3s linear;
}

/* 文件夹和文件的属性栏 */
.storage-content {
    position: relative;
    left: 50%;
    transform: translateX(-50%);
    display: flex;
    justify-content: start;
    align-items: center;
    flex-direction: column;
    width: 85%;
    height: 80vh;
    border: 1px solid #30363d;
    overflow: hidden;
}

.property {
    position: fixed;
    width: 100%;
    height: 5vh;
    font-size: 0.8em;
    color: #8d96a0;
}

.property .name,
.property .create-date,
.property .modify-date,
.property .type,
.property .size {
    position: absolute;
    height: 5vh;
    line-height: 5vh;
}

.property .name {
    left: 4vw;
}

.property .create-date{
    left: 41vw;
}

.property .modify-date {
    left: 53vw;
}

.property .type {
    left: 65.5vw;
}

.property .size {
    left: 73.7vw;
}

.fs-content {
    position: relative;
    display: flex;
    justify-content: start;
    align-items: center;
    flex-direction: column;
    width: 100%;
    height: 100%;
    padding: 1vh 1.5vw;
    margin-top: 5vh;
    color: #8d96a0;
    overflow: auto;
    scrollbar-width: none;
}

/* 文件夹 */
.fs-folder, .fs-file {
    position: relative;
    width: 100%;
    height: 5vh;
    display: flex;
    justify-content: start;
    align-items: center;
    padding: 0.5em 0;
    font-size: 0.9em;
    cursor: default;
    border: 1px solid transparent;
}

.fs-folder:hover, .fs-file:hover {
    border: 1px solid #30363d;
}

.fs-file:last-child {
    margin-bottom: 2vh;
}

.fs-folder .select, .fs-file .select,
.fs-folder .created-at, .fs-file .created-at,
.fs-folder .updated-at, .fs-file .updated-at,
.fs-folder .type, .fs-file .type,
.fs-folder .size, .fs-file .size {
    display: flex;
    justify-content: center;
    align-items: center;
    height: 5vh;
    text-align: center;
}

.fs-folder .select, .fs-file .select {
    width: 2vw;
}

.fs-file .select input[type="checkbox"]{
    appearance: none;
    -webkit-appearance: none;
    -moz-appearance: none;
    width: 1.2em;
    height: 1.2em;
    border: 1px solid #30363d;
    background-color: transparent;
    position: relative;
    transition: border-color 0.2s ease, background-color 0.2s ease;
    cursor: pointer;
}

.fs-file .select input[type="checkbox"]:hover,
.fs-file .select input[type="checkbox"]:checked {
  border-color: rgba(0, 128, 255, 0.6);
}

.fs-file .select input[type="checkbox"]:checked::before {
  content: "";
  position: absolute;
  top: 40%;
  left: 50%;
  width: 0.7em;
  height: 0.3em;
  border-left: 1px solid #0080ff;
  border-bottom: 1px solid #0080ff;
  transform: translate(-50%, -50%) rotate(-45deg);
  transition: transform 0.2s ease;
}

.fs-folder .name,
.fs-file .name{
    position: relative;
    display: flex;
    justify-content: start;
    align-items: center;
    width: 35vw;
    height: 5vh;
}

.fs-folder .name .folder-name {
    background-color: transparent;
    text-align: left;
    font-size: 0.95em;
    width: 20em;
    border: none;
    outline: none;
    padding: 1vh 0.75em;
    caret-color: #fff;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.fs-file .name .file-name {
    text-align: left;
    font-size: 0.95em;
    width: 33vw;
    border: none;
    outline: none;
    padding: 1vh 0.75em;
    caret-color: #fff;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.fs-folder .name .folder-name:focus,
.fs-file .name .file-name:focus {
    border: 1px solid #30363d;
}

.fs-folder .name .folder-name::placeholder
.fs-file .name .file-name::placeholder {
    color: #fff;
}

.fs-folder .created-at, .fs-folder .updated-at,
.fs-file .created-at, .fs-file .updated-at,
.fs-folder .type,.fs-file .type {
    width: 12vw;
    font-size: 0.9em;
}

.fs-folder .size,
.fs-file .size {
    width: 4vw;
    padding: 1vh 0;
    font-size: 0.8em;
}

.fs-folder .buttons,
.fs-file .buttons{
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 0.5vw;
    width: 6vw;
    padding: 1vh 0;
}

.storage-content .buttons .download-icon,
.storage-content .buttons .delete-icon {
    cursor: pointer;
    transition: all 0.3s linear;
}

.storage-content .buttons .download-icon:hover use{
    fill: green !important;
    transition: all 0.3s linear;
}

.storage-content .buttons .delete-icon:hover use{
    fill:rgb(202, 110, 110) !important;
    transition: all 0.3s linear;
}

/* 上传模块 */
.upload {
    position: relative;
    left: 50%;
    transform: translateX(-50%);
    width: 40%;
    height: 20vh;
    display: flex;
    justify-content: center;
    align-items: center;
    flex-direction: column;
    border: 1px dashed #8d96a0;
    margin-top: 20vh;
    margin-bottom: 10vh;
}

.upload.disabled {
    cursor: not-allowed;
    opacity: 0.5;
    pointer-events: none;
}

.upload-form {
    position: relative;
    width: 40vw;
    height: 20vh;
}

#select，#submit {
    position: relative;
    width: 40vw;
    height: 20vh;
    transform: translateX(100%);
}

.upload-info,
.submit-info {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 0.8em;
  display: flex;
  justify-content: center;
  align-items: center;
  column-gap: 1vw;
}

#upload-input,
#files-submit {
    width: 40vw;
    height: 20vh;
    opacity: 0;
    z-index: 1;
}

/* 上传的文件列表 */
.file-list {
    position: relative;
    left: 10%;
    width: 80%;
    background-color: #161b22;
    margin-bottom: 5vh;
    padding-bottom: 2vh;
}

.file-list .title {
  width: 8vw;
  margin: 2vh 0;
}

.file-list .files-content {  
    display: flex;
    justify-content: center;
    align-items: start;
    flex-direction: column;
    min-height: 4vh;
}

.file-list .files-content .file-item {
    position: relative;
    display: flex;
    justify-content: center;
    align-content: center;
    width: 100%;
    border: 1px solid #30363d;
    padding: 1em;
}

.file-list .files-content .file-item .file-name {
    width: 50%;
    font-size: 0.8em;
    color: #8d96a0;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.file-list .files-content .file-item .file-size {
  width: 10%; 
  display: flex;
  align-items: center;
  justify-content: center;
  color: hsl(212, 12%, 50%);
  font-size: 0.7em;
}

.file-list .files-content .file-item svg{
    cursor: pointer;
    transition: all linear 0.5s;
}

.file-list .files-content .file-item svg:hover use{
  fill: rgb(202, 110, 110);
}

.file-list .clear {
    position: relative;
    left: 50%;
    transform: translateX(-50%);
    width: 6vw;
    margin-top: 3vh;
    padding: 1vh 0.5vw;
    border: 1px solid #30363d;
    border-radius: 0.5vw;
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 0.5em;
    cursor: pointer;
    font-size: 0.8em;
}

.file-list .clear:hover {
    opacity: 0.6;
}

/* 上传模块的切换动画 */
.fade-enter-active, .fade-leave-active {
    transition: opacity 0.3s ease;
}
.fade-enter-from, .fade-leave-to {
    opacity: 0;
}

/* 右键所选项显示的选项窗口 */
#context-menu {
    position: absolute;
    display: none;
    justify-content: center;
    align-items: center;
    flex-direction: column;
    list-style: none;
    font-size: 0.8em;
    background-color: rgba(0, 0, 0, 0.1);
    backdrop-filter: blur(5px);
    border: 1px solid #30363d;
    box-shadow: 5px 5px 15px rgba(255, 255, 255, 0.1);
    z-index: 3;
}

#context-menu li {
    padding: 10px 20px;
    cursor: pointer;
}

#context-menu li:first-child {
    border-bottom: 1px solid #30363d;
}

#context-menu li:hover {
    background-color: rgba(255, 255, 255, 0.1);
}

.download-selected,
.delete-selected {
    display: flex;
    align-items: center;
    color: white;
}

#context-menu li .download-icon,
#context-menu li .delete-icon {
    width: 20px;
    height: 20px;
    fill: currentColor;
}

#context-menu li span {
    padding: 0.5vh 1vw;
}

/* 下载任务窗口 */
.download-window {
    position: fixed;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    width: 42vw;
    height: 50vh;
    color: #30363d;
    background-color: rgba(0, 0, 0, 0.1);
    backdrop-filter: blur(5px);
    box-shadow: 0 8px 15px rgba(255, 255, 255, 0.1);
    font-size: 0.8em;
    overflow: hidden;
}

.dragging {
    cursor: grabbing;
    box-shadow: 0 2px 16px rgba(255, 255, 255, 0.3);
    transform: translate(-50%, -50%) scale(1.02);
    transition: box-shadow 0.2s ease, transform 0.2s ease;
}

.dragging .window-header {
    background: linear-gradient(135deg, rgb(40, 116, 166, 0.3), rgb(93, 173, 226, 0.4));
}

.window-header {
    position: fixed;
    width: 42vw;
    height: 4vh;
    line-height: 4vh;
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0.75em 1em;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
    background: linear-gradient(135deg, rgb(40, 116, 166, 0.15) , rgb(93, 173, 226, 0.2));
    backdrop-filter: blur(5px);
}

.window-header .buttons {
    width: 4em;
    height: 4vh;
    display: flex;;
    justify-content: space-around;
    align-items: center;
}

.window-header .buttons .svg-icon {
    cursor: pointer;
}

.task-list {
    position: relative;
    top: 4vh;
    height: 100%;
    padding: 1em;
    display: flex;
    justify-content: start;
    align-items: center;
    flex-direction: column;
    gap: 1vh;
    overflow-y: auto;
    scrollbar-width: none;
}

.task-item {
    height: 4vh;
    line-height: 4vh;
    padding: 0.5em 1em;;
    display: flex;
    align-items: center;
    background: rgba(255, 255, 255, 0.05);
    border-radius: 0.25em;
    transition: background 0.2s ease;
    cursor: default;
}

.task-item:hover {
    background: rgba(255, 255, 255, 0.1);
}

.task-item .task-icon {
    height: 4vh;
    line-height: 4vh;;
}

.task-item .task-name {
    width: 20em;
    height: 4vh;
    line-height: 4vh;;
    padding: 0 0.5em;
    font-size: 0.9em;
    color: #8d96a0;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.task-item .progress-bar-container {
    width: 18vw;
}

.task-item .download-progress {
    width: 7vw;
    height: 4vh;
    line-height: 4vh;;
    text-align: center;
    font-size: 10px;
}

.task-item .download-progress .downloaded {
    color: #328032;
}

.task-item .download-progress .total {
    color: rgb(52, 152, 219, 0.7);
}

.task-item .delete-icon {
    cursor: pointer;
    transition: all 0.3s ease;
}

.task-item .delete-icon:hover {
    transform: scale(1.5);
}
</style>