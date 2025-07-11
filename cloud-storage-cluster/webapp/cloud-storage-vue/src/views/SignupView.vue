<template>
    <component v-if="popup.isPopup" :is="popup.popupComponent" :type="popup.popupType">{{ popup.message }}</component>
    <div class="form-container">
        <form class="auth-form signup" @submit.prevent="signup">
            <div class="title">
                <svg-icon icon-class="signup" color="rgb(255, 255, 255, 0.5)" size="1.5em"></svg-icon>
                <span>注册</span>
            </div>
            <input class="input" v-model="register.email" type="text" name="email" placeholder="邮箱"
                pattern="^[^\s@]+@[^\s@]+\.[^\s@]+$" title="请输入有效的邮箱地址" required autocomplete>

            <input class="input" v-model="register.username" type="text" name="username" placeholder="用户名" required />

            <div class="password">
                <input class="input" v-model="register.password" type="password" name="password" placeholder="密码" title="请输入密码，长度6-20位且至少包含数字和字母"
                minlength="6" maxlength="20" pattern="^(?=.*[A-Za-z])(?=.*\d).+$" required autocomplete>
                <svg-icon v-if="register.password.trim() != ''" icon-class="strength" class="strength" :color="strengthColor" size="1.5em"></svg-icon>
            </div>

            <div class="auth-code">
                <input v-model="register.code" class="input" type="text" name="code" placeholder="验证码" title="请输入验证码" required>
                <button class="verify" :disabled="register.isSending" @click="verify">{{ this.register.codeButtonText }}</button>
            </div>

            <div class="option">
                <input class="input" type="submit" value="注册">
                <input class="input" type="button" value="登录" @click="$router.push({ name: 'login' })">
            </div>
        </form>
    </div>
</template>

<script>
import axios from 'axios';
import { apiEndpoints } from '@/api/api';

export default {
    data() {
        return {
            popup: {
                isPopup: false, // 控制弹窗显示状态
                popupComponent: 'Popup', //弹窗组件
                popupType: '', //弹窗类型
                message: '', //弹窗消息
            },
            register: {
                email: '',
                username: '',
                password: '',
                code: '',
                isSending: false, // 控制验证码发送状态
                codeButtonText: '获取验证码' // 验证码按钮文本
            },
        }
    },
    computed: {
        strengthColor() {
            const strength = this.getStrength(this.register.password);
            switch (strength) {
                case 0:
                case 1:
                    return "#f6998e";//弱密码
                case 2:
                    return "#ccb800";//中等密码
                case 3:
                case 4:
                    return "#3f8950";//强密码
            }
        }
    },
    methods: {
        /* 获取密码强度 */
        getStrength(password) {
            let strength = 0;
            if (password.length < 6) {
                return strength;
            }
            if (/[a-z]/.test(password)) {//包含至少一个小写字母
                strength++;
            }
            if (/[A-Z]/.test(password)) {//包含至少一个大写字母
                strength++;
            }
            if (/\d/.test(password)) {//包含至少一个数字
                strength++;
            }
            if (/[\W_]/.test(password)) {//包含至少一个特殊字符
                strength++;
            }
            return strength;
        },
        // 弹出弹窗
        async createPopup(type = 'info', message) {
            this.popup.isPopup = true;
            this.popup.popupType = type;
            this.popup.message = message;
            setTimeout(() => {
                this.popup.isPopup = false;
            }, 10000); // 弹窗显示 10 秒
        },

        /* 发送验证码 */
        async verify() {
            // 验证邮箱格式
            const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
            if (!emailPattern.test(this.register.email)) {
                this.createPopup('warning', "请输入有效的邮箱地址。");
                return;
            }

            if(this.register.isSending) {
                this.createPopup('info', '验证码已发送，请稍后再试。');
                return;
            }

            let countdown = 60; // 倒计时60秒
            this.register.isSending = true;
            this.register.timer = setInterval(() => { // 创建定时器
                if (countdown > 0) {
                    countdown--;
                    this.register.codeButtonText = countdown;
                } else {
                    clearInterval(this.register.timer); // 清除定时器
                    this.register.isSending = false;
                    this.register.codeButtonText = '重新获取';
                }
            }, 1000);

            axios(apiEndpoints.sendEmailCode(this.register.email))
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

        async signup() {
            const { email, username, password, code } = this.register;
            axios(apiEndpoints.userRegister(email, username, password, code))
                .then(response => {
                    if (response.data.code === 200) {
                        this.createPopup('success', '注册成功，即将跳转到登录页面。');
                        setTimeout(() => {
                            this.$router.push({ name: 'login' });
                        }, 3000); // 3秒后跳转到登录页面
                    } else {
                        this.createPopup('error', response.data.msg || '注册失败，请重试。');
                    }
                })
                .catch(error => {
                    console.error('注册请求失败:', error);
                    this.createPopup('error', '注册请求失败，请稍后再试。');
                });
        }
    }
}
</script>

<style scoped>
/* signup form */
.signup .title{
    margin-bottom: 2vh;
}

.signup .option {
    display: flex;
    justify-content: center;
    align-items: center;
    column-gap: 1em;
}

.signup .option .input {
    width: 12em;
}

.signup .password{
    position: relative;
}

.signup .strength {
    position: absolute;
    top: 50%;
    right: -1.2vw;
    transform: translate(50%, -50%);
}
</style>