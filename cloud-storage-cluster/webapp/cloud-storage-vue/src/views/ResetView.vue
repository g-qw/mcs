<template>
    <component v-if="popup.isPopup" :is="popup.popupComponent" :type="popup.popupType">{{ popup.message }}</component>
    <div class="form-container">
        <form class="auth-form reset" @submit.prevent="resetPwd">
            <div class="title">
                <svg-icon icon-class="reset" color="rgba(255, 255, 255, 0.5)" size="1.5em"></svg-icon>
                <span>重置密码</span>
            </div>
            <input class="input" v-model="reset.email" type="text" name="email" placeholder="邮箱"
                pattern="^[^\s@]+@[^\s@]+\.[^\s@]+$" title="请输入有效的邮箱地址" required autocomplete>

            <div class="password">
                <input class="input" v-model="reset.password" type="password" name="password" placeholder="密码" title="请输入密码6-20位，至少包含数字和字母"
                minlength="6" maxlength="20" pattern="^(?=.*[A-Za-z])(?=.*\d).+$" required>
                <svg-icon v-if="reset.password.trim() != ''" icon-class="strength" class="strength" :color="strengthColor" size="1.5em"></svg-icon>
            </div>

            <input class="input" v-model="reset.confirm" type="password" name="confirm" placeholder="确认密码" title="请再次输入重置后的密码"
                minlength="6" maxlength="20" required>

            <div class="auth-code">
                <input v-model="reset.code" class="input" type="text" name="code" placeholder="验证码" title="请输入验证码" required>
                <button class="verify" :disabled="reset.isSending" @click="verify">{{ reset.codeButtonText }}</button>
            </div>
            <input class="input" type="submit" value="重置密码" title="重置密码">
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
            reset: {
                email: '',//邮箱
                password: '',//重置密码
                confirm: '',//确认密码
                code: '',//验证码
                isSending: false, // 控制验证码发送状态
                codeButtonText: '获取验证码', // 验证码按钮文本
                timer: null // 用于存储倒计时定时器
            }
        }
    },
    computed: {
        strengthColor() {
            const strength = this.getStrength(this.reset.password);
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
        },
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

        /* 弹出弹窗 */
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
            if (!emailPattern.test(this.reset.email)) {
                this.createPopup('warning', "请输入有效的邮箱地址。");
                return;
            }

            if(this.reset.isSending) {
                this.createPopup('info', '验证码已发送，请稍后再试。');
                return;
            }

            let countdown = 60; // 倒计时60秒
            this.reset.isSending = true;
            this.reset.timer = setInterval(() => { // 创建定时器
                if (countdown > 0) {
                    countdown--;
                    this.reset.codeButtonText = countdown;
                } else {
                    clearInterval(this.reset.timer); // 清除定时器
                    this.reset.isSending = false;
                    this.reset.codeButtonText = '重新获取';
                }
            }, 1000);

            axios(apiEndpoints.sendEmailCode(this.reset.email))
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

        /* 重置密码 */
        async resetPwd() {
            console.log('reset');
            if (this.reset.password !== this.reset.confirm) {
                this.createPopup('warning', '两次输入的密码不一致，请重新输入。');
                return;
            }

            const { email, password, code } = this.reset;
            axios(apiEndpoints.userResetPwd(email, password, code))
                .then(response => {
                    if (response.data.code === 200) {
                        this.createPopup('success', '密码重置成功，即将跳转登录页面。');
                        setTimeout(() => {
                            this.$router.push({ name: 'login' });
                        }, 3000); // 3秒后跳转到登录页面
                    } else {
                        this.createPopup('error', response.data.msg || '密码重置失败，请重试。');
                    }
                })
                .catch(error => {
                    console.error('密码重置请求失败:', error);
                    this.createPopup('error', '密码重置请求失败，请稍后再试。');
                });
        }
    }
}
</script>

<style scoped>
/* reset password form */
.reset .password{
    position: relative;
}

.reset .strength {
    position: absolute;
    top: 50%;
    right: -1.2vw;
    transform: translate(50%, -50%);
}
</style>