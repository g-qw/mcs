<template>
    <component v-if="popup.isPopup" :is="popup.popupComponent" :type="popup.popupType">{{ popup.message }}</component>
    <div class="form-container">
        <form class="auth-form login" @submit.prevent="signIn">
            <div class="title">
                <svg-icon icon-class="login" color="rgba(255, 255, 255, 0.5)" size="1.5em"></svg-icon>
                <span>登录</span>
            </div>
            <div class="form-icons">
                <svg-icon icon-class="qq" color="rgba(255, 255, 255, 0.3)" size="1.5em"></svg-icon>
                <svg-icon icon-class="wechat" color="rgba(255, 255, 255, 0.3)" size="1.5em"></svg-icon>
                <svg-icon icon-class="github" color="rgba(255, 255, 255, 0.3)" size="1.5em"></svg-icon>
            </div>
            <input class="input" v-model="login.email" type="text" name="email" placeholder="邮箱"
                pattern="^[^\s@]+@[^\s@]+\.[^\s@]+$" title="请输入有效的邮箱地址" required autocomplete>

            <div class="password">
                <input class="input" v-model="login.password" type="password" name="password" placeholder="密码" title="请输入密码"
                minlength="6" maxlength="20" pattern="^(?=.*[A-Za-z])(?=.*\d).+$" required autocomplete>
            </div>

            <div class="auth-code">
                <input v-model="login.code" class="input" type="text" name="code" placeholder="验证码" title="请输入验证码" required>
                <button class="verify" :disabled="login.isSending" @click="verify">{{ this.login.codeButtonText }}</button>
            </div>

            <input class="input" type="submit" value="登录" title="登录">
            
            <div class="help">忘记了您的密码？<RouterLink to="/auth/reset">找回密码</RouterLink></div>
            <div class="help">没有账号？<RouterLink to="/auth/signup">创建您的账号。</RouterLink></div>
        </form>
    </div>
</template>

<script>
import { apiEndpoints } from '@/api/api';
import axios from 'axios';
import { useFileExplorerStore } from '@/stores/fileExplorer';
import { storeToRefs } from 'pinia';
export default {
    name: "LoginView",
    setup() {
        const fe = useFileExplorerStore();

        const { userInfo, bucket } = storeToRefs(fe);

        return {
            userInfo, bucket
        }
    },
    data() {
        return {
            popup: {
                isPopup: false, // 控制弹窗显示状态
                popupComponent: 'Popup', //弹窗组件
                popupType: '', //弹窗类型
                message: '', //弹窗消息
            },
            login: {
                email: '', // 邮箱
                password: '', // 密码
                code: '', // 验证码
                isSending: false, // 控制验证码发送状态
                codeButtonText: '获取验证码', // 验证码按钮文本
                timer: null // 用于存储倒计时定时器
            },
        }
    },
    created() {
        //this.autoLogin(); //自动登录
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
        async verify() {
            // 验证邮箱格式
            const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
            if (!emailPattern.test(this.login.email)) {
                this.createPopup('warning', "请输入有效的邮箱地址。");
                return;
            }

            if(this.login.isSending) {
                this.createPopup('info', '验证码已发送，请稍后再试。');
                return;
            }

            let countdown = 60; // 倒计时60秒
            this.login.isSending = true;
            this.login.timer = setInterval(() => { // 创建定时器
                if (countdown > 0) {
                    countdown--;
                    this.login.codeButtonText = countdown;
                } else {
                    clearInterval(this.login.timer); // 清除定时器
                    this.login.isSending = false;
                    this.login.codeButtonText = '重新获取';
                }
            }, 1000);

            axios(apiEndpoints.sendEmailCode(this.login.email))
                .then(response => {
                    if (response.data.code === 200) {
                        this.createPopup('success', '验证码已发送至您的邮箱，请在1分钟内完成验证。');
                    } else {
                        this.createPopup('error', response.data.msg || '验证码发送失败，请重试。');
                    }
                })
                .catch(error => {
                    console.error('验证码请求失败:', error);
                    this.createPopup('error', '验证码请求失败，请稍后再试。');
                });
        },

        async signIn() {
            const { email, password, code } = this.login;
            axios(apiEndpoints.userLogin(email, password, code))
                .then(response => {
                    if (response.data.code === 200) {
                        this.createPopup('success', '登录成功！即将跳转到首页。');
                        // 登录成功后，将token存储到sessionStorage
                        let token = response.data.data;
                        sessionStorage.setItem('token', token);

                        setTimeout(() => {
                            window.location.href = '/main';
                        }, 1000); // 1 秒后跳转到首页
                        
                    } else {
                        this.createPopup('error', response.data.msg || '登录失败，请重试。');
                    }
                })
                .catch(error => {
                    console.error('登录请求失败:', error);
                    this.createPopup('error', '登录请求失败，请稍后再试。');
                });
        },
    }
}
</script>

<style scoped>
/* login form */
.login .form-icons {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 2em;
    margin: 0.75em 0;
}

.login .form-icons svg{
    cursor: pointer;
}

.login .form-icons svg:hover:deep(use){
    transition: all 0.7s linear;
    fill: rgba(255, 255, 255, 0.8);
}

.login .help {
    color: rgba(255, 255, 255, 0.3);
    margin-top: 1em;
}

.login .help a{
    text-decoration: underline;
    color: rgba(255, 255, 255, 0.3);
    cursor: pointer;
    transition: color 0.5s linear;
}

.login .help a:hover{
    color: rgba(255, 255, 255, 0.7); 
}
</style>