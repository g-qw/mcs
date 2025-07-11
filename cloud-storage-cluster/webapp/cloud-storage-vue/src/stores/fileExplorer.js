import { apiEndpoints } from '@/api/api';
import { defineStore } from 'pinia';
import { reactive } from 'vue';


export const useFileExplorerStore = defineStore('fileExplorer', {
    state: () => (
        // 目录模型
        {
            userInfo: {}, // 用户信息
            bucket: '', // 存储桶
            path: '/', //当前目录路径
            dir: {}, //当前目录对象
            files: [], // 存储文件列表, 每个元素是文件对象信息
            folders: [], // 存储目录列表， 每个元素是目录对象信息
            dirNodes: [], // 存储路径中的目录节点
            selectedFiles: [], // 选中的 files 的对象的索引
            downloadTasks: reactive([]) // 存储下载任务的文件对象
        }
    ),
    actions: {
        /* 根据 path 获取当前目录对象 dir */
        async getCurrentDirectory() {
            if (!this.path) {
                console.log("当前路径信息丢失，无法获取当前目录");
                return;
            }

            axios(apiEndpoints.fsParseDirPath(this.path))
                .then(
                    response => {
                        if (response.data.code === 200) {
                            this.dir = response.data.data;
                            console.log(`从当前路径 ${this.path} 加载目录信息完毕`);
                        } else {
                            console.log(response.data.msg);
                        }
                    }
                ).catch(
                    error => {
                        console.log(`加载当前目录 ${this.path} 发生错误：`, error);
                    }
                )
        }
    }
});

export const useMainStore = defineStore('main', {
    state: () => ({
      isShowUserProfile: false, // 是否显示用户信息
      ctrlMode: false,
      shiftMode: false,
      allMode: false,
      sortMode: '', // 排序模式
      isFlush: false, // 是否刷新
      searchText: '', // 搜索文本
    }),
    actions: {
      setCtrlMode(value) {
        this.ctrlMode = value;
        this.shiftMode = !value;
        this.allMode = !value;
      },
      setShiftMode(value) {
        this.ctrlMode = !value;
        this.shiftMode = value;
        this.allMode = !value;
      },
      setAllMode(value) {
        this.ctrlMode = !value;
        this.shiftMode = !value;
        this.allMode = value;
      },
      toggleMode(mode) {
        if (mode === 'ctrl') {
          this.setCtrlMode(!this.ctrlMode);
        } else if (mode === 'shift') {
          this.setShiftMode(!this.shiftMode);
        } else if (mode === 'all') {
          this.setAllMode(!this.allMode);
        }
      },
      toggleScroll() {
        console.log('toggle scroll');
        this.isShowUserProfile = !this.isShowUserProfile;
        if (this.isShowUserProfile) {
          document.body.style.overflow = 'hidden';
        } else {
          document.body.style.overflow = 'auto';
        }
      },
    },
  });