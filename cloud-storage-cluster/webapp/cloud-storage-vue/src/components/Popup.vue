<template>
    <Transition name="fade">
        <div v-if="visible" class="popup-overlay">
            <div class="popup">
                <svg-icon :icon-class="type" size="1em"></svg-icon>
                <p><slot></slot></p>
            </div>
        </div>
    </Transition>
</template>

<script>
export default {
    name: 'Popup',
    props: {
        /*
            info: 信息提示
            warning: 警告提示
            error: 错误提示
            success: 成功提示
        */
        type: {
            type: String,
            default: 'info',
            required: false
        }
    },
    data() {
        return {
            visible: true,
            timer: null //定时器
        }  
    },
    mounted() {
        this.timer = setTimeout(() => {
            this.closePopup();
        }, 5000);
    },
    beforeDestroy() {
        //组件销毁前清除定时器
        if (this.timer) {
            clearTimeout(this.timer);
        } 
    },
    methods: {
        closePopup() {
            this.visible = false;
            this.timer = null;
        }
    }
}
</script>

<style scoped>
.popup-overlay {
    position: fixed;
    top: 5vh;
    left: 50%;
    transform: translate(-50%, -50%);
    z-index: 9999;
}

.popup{
    display: flex;
    justify-content: center;
    align-items: center;
    column-gap: 0.5vw;
    padding: 0.5em 1em;
    background-color: #fff;
    box-shadow: 4px 4px 8px 0 rgba(0, 0, 0, 0.3);
}

.popup p{
    font-family: "Helvetica Neue", Helvetica, Arial, "PingFang SC", "Hiragino Sans GB", "Heiti SC", "Microsoft YaHei", "WenQuanYi Micro Hei", sans-serif;;
    font-size: 0.8em;
    color: rgba(0, 0, 0, 0.8);
}

/* 弹窗的初始状态 */
.fade-enter-from, .fade-leave-to {
    top: -5vh;
    opacity: 0;
}

/* 弹窗的最终状态 */
.fade-enter-to, .fade-leave-from {
    top: 5vh;
    opacity: 1;
}

.fade-enter-active, .fade-leave-active {
    top: 0;
    transition: all 1s ease;
}
</style>