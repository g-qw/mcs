<template>
    <main class="auth">
        <div class="return" @click="back">
            <svg-icon icon-class="return" color="rgba(255, 255, 255, 0.5)" size="0.8vw"></svg-icon>
        </div>
        <router-view></router-view>
    </main>
</template>

<script>
export default {
    methods: {
        back() {
            this.$router.go(-1);
        }
    }
}
</script>

<style>
.auth {
    width: 100%;
    height: 100vh;
    background-image: linear-gradient(120deg, #6e45e2 0%, #88d3ce 100%);
    background-repeat: no-repeat;
    overflow: hidden;
}

.return {
    position: absolute;
    top: 1.5em;
    left: 2.5em;;
    width: 4vw;
    height: 4vh;
    display: flex;
    justify-content: center;
    align-items: center;
    border: 1px solid rgba(255, 255, 255, 0.15);
    box-shadow: 4px 4px 8px rgba(255, 255, 255, 0.2);
    z-index: 1;
}

.return:hover {
    box-shadow: -4px -4px 8px rgba(255, 255, 255, 0.3);
}

.return:hover svg use{
    fill: rgba(255, 255, 255, 0.8);
}

/* public form style */
.form-container{
    position: relative;
    top: 0;
    right: 0;
    display: block;
    width: 30em;
    padding: 2em 1em;
    margin: 12em auto;
    border: 1px solid rgba(255, 255, 255, 0.5);
    border-radius: 1em;
    background-color: rgba(0, 0, 0, 0.3);
    box-shadow: 5px 5px 10px 0 rgba(255, 255, 255, 0.3);
}

.auth-form {
    display: flex;
    justify-content: center;
    align-items: center;
    flex-direction: column;
    margin-bottom: 1.5em;
}

.auth-form .input {
    box-sizing: border-box;
    background-color: rgba(255, 255, 255, 0.1);
    color: rgba(255, 255, 255, 0.5);
    width: 25em;
    padding: 0.8em;;
    margin: 1em 0;
    border: 1px solid rgba(255, 255, 255, 0.1);
    border-radius: 0.75em;
    box-shadow: 3px 3px 10px 0 rgba(0, 0, 0, 0.3);
    outline: none;
}

.auth-form .input:hover {
    border-color: rgba(255, 255, 255, 0.5);
    box-shadow: 3px 3px 10px rgba(255, 255, 255, 0.5);
}

.auth-form .input:focus {
    border: none !important;
}

.auth-form .input::placeholder {
    color: rgba(255, 255, 255, 0.3);
}

.auth-form .title{
    display: flex;
    justify-content: center;
    align-items: center;
    column-gap: 0.5vw;
    margin-top: 1em;
}

.title span{
    font-size: 1.2em;
    color: rgba(255, 255, 255, 0.5);
}

.auth-form .auth-code{
    position: relative;
}

.auth-form .auth-code button{
    background: none;
    text-shadow: none;
    outline: none;
    border: none;
    -webkit-tap-highlight-color: transparent;   /* 去除点击时的焦点环 */
}

.auth-form .auth-code .verify {
    position: absolute;
    right: 0;
    top: 50%;
    transform: translateY(-50%);
    color: rgba(255, 255, 255, 0.5);
    font-size: 0.6em;
    margin-right: 1vw;
    cursor: pointer;
}

.auth-form .auth-code .verify:hover {
    color: rgba(255, 255, 255, 0.7);
}

.auth-form .auth-code .verify:disabled{
    color: rgba(255, 255, 255, 0.3) !important;
    cursor: url("@/assets/image/not-allowed.png") 16 16, not-allowed;
}
</style>
