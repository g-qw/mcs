<template>
    <main class="home">
        <div class="parallax start" ref="parallaxStart">
            <video autoplay muted loop class="background-video">
                <source src="/src/assets/main/start.mp4" type="video/mp4">
                Your browser does not support the video tag.
            </video>

            <div class="logo">
                <svg-icon icon-class="logo" color="rgba(255,255,255, 0.5)" size="2vw"></svg-icon>
                <div class="brand">{{ brand }}</div>
            </div>

            <div class="content">
                <div class="title">{{ parallaxStart.title1 }}</div>
                <div class="title">{{ parallaxStart.title2 }}</div>
                <p class="intro">{{ parallaxStart.intro }}</p>
                <RouterLink :to="{name: 'login'}" class="login-btn">
                    <div class="text">开始使用</div>
                    <svg-icon icon-class="entry" color="rgba(255,255,255, 0.5)" size="1.2em"></svg-icon>
                </RouterLink>
            </div>

            <div class="next" @click="scrollToNextParallax(1)">
                <svg-icon icon-class="arrow-down" color="rgba(255, 255, 255, 0.5)" size="1.5em"></svg-icon>
            </div>
        </div>

        <div v-for="(parallax, index) in parallaxes" :class="['parallax', parallax.classname]" ref="parallaxRefs">
            <div class="content item">
                <div class="title">{{ parallax.title }}</div>
                <div class="intro">{{ parallax.intro }}</div>
            </div>
            <div class="next" @click="scrollToNextParallax(parallax.index)">
                <svg-icon icon-class="arrow-down" color="rgba(255, 255, 255, 0.5)" size="1.5em" v-if="index < parallaxes.length - 1"></svg-icon>
            </div>
        </div>
    </main>

    <footer>
        <div class="footer">
            <div class="follow">
                <div v-for="follow in follows" class="follow-icon">
                    <a :href="follow.href">
                        <svg-icon :icon-class="follow.icon" color="rgba(255, 255, 255, 0.3)" size="1.6em"/>
                    </a>
                </div>
            </div>
            <div class="links">
                <div v-for="item in linkItems" class="link-item">
                    <h3>{{ item.title }}</h3>
                    <ul>
                        <li v-for="link in item.links">
                            <a :href="link.href">{{ link.text }}</a>
                        </li>
                    </ul>
                </div>
            </div>
            <div class="top">
                <svg-icon icon-class="top" size="2em" color="rgba(255, 255, 255, 0.3)" @click="scrollToTop"></svg-icon>
            </div>
            <div class="copyright">Copyright © 1998 - 2024 CloudStorage. All Rights Reserved. CloudStorage 版权所有</div>
        </div>  
    </footer>
</template>

<script>
export default {
    data() {
        return {
            brand: '云存储',
            parallaxStart: {
                title1: '记录每一份热爱',
                title2: '让美好永远陪伴',
                intro: '为你电脑/手机中的文件提供云备份、预览、分享等服务，帮你更便捷安全地管理数据。'
            },
            parallaxes: [
                {
                    classname: 'p1',
                    title: '智能管理，简化操作',
                    intro: '复杂的数据管理不再是难题。我们的智能管理系统，通过简化的操作流程，让您轻松管理大量数据，释放您的双手，专注于更重要的事务。',
                    index: 2,
                },
                {
                    classname: 'p2',
                    title: '高效同步，无缝协作',
                    intro: '工作流程中的每一步都至关重要。我们的高效同步技术，让团队协作变得无缝而流畅，每个成员都能实时访问最新资料，提升工作效率，共创辉煌。',
                    index: 3,
                },
                {
                    classname: 'p3',
                    title: '全球访问，极速体验',
                    intro: '无论您身在何处，我们的全球访问服务都能提供极速的网络体验。无论距离多远，都能即刻访问所需数据，让远程工作和国际合作变得轻松自如。',
                    index: 4,
                },
                {
                    classname: 'p4',
                    title: '智能管理，简化操作',
                    intro: '复杂的数据管理不再是难题。我们的智能管理系统，通过简化的操作流程，让您轻松管理大量数据，释放您的双手，专注于更重要的事务。',
                    index: 5,
                },
                {
                    classname: 'p5',
                    title: '数据加密，隐私保护',
                    intro: '在信息泛滥的时代，隐私保护至关重要。我们的数据加密技术，为您的数据安全筑起坚不可摧的防线，确保您的隐私得到最大程度的尊重和保护。',
                    index: 6,

                },
                {
                    classname: 'p6',
                    title: '一键预览，效率倍增',
                    intro: '无论您存储的是文档、图片还是视频，我们的文件预览功能都能让您在云端轻松预览，无需下载即可查看文件内容，节省您的宝贵时间和存储空间。',
                    index: -1,
                }
            ],
            follows:[
                {
                    href: "#weibo",
                    icon: "weibo"
                },
                {
                    href: "#wechat",
                    icon: "wechat"
                },
                {
                    href: "#qq",
                    icon: "qq"
                },
                {
                    href: "#x",
                    icon: "x"
                },
                {
                    href: "#youtube",
                    icon: "youtube"
                },
                {
                    href: "#share",
                    icon: "share"
                }
            ],
            linkItems:[
                {
                    title: "关于我们",
                    links: [
                        {
                            href: "#society_join",
                            text: "加入我们"
                        },
                        {
                            href: "#campus_join",
                            text: "校园招聘"
                        },
                        {
                            href: "#internation_join",
                            text: "国际招聘"
                        }
                    ]
                },
                {
                    title: "联系我们",
                    links: [
                        {
                            href: "#service",
                            text: "客户服务"
                        },
                        {
                            href: "#cooperation",
                            text: "合作洽谈"
                        },
                        {
                            href: "#media",
                            text: "媒体采访"
                        },
                        {
                            href: "#invest",
                            text: "投资采购"
                        }
                    ]
                },
                {
                    title: "法律信息",
                    links: [
                        {
                            href: "#service-agreement",
                            text: "服务协议"
                        },
                        {
                            href: "#privacy-policy",
                            text: "隐私政策"
                        },
                        {
                            href: "#law-and-disclaimer",
                            text: "法律声明"
                        }
                    ]
                },
                {
                    title: "帮助中心",
                    links: [
                        {
                            href: "#security",
                            text: "隐私安全"
                        },
                        {
                            href: "#document",
                            text: "使用文档"
                        },
                        {
                            href: "#authorization",
                            text: "授权认证"
                        },
                        {
                            href: "#report",
                            text: "举报投诉"
                        }
                    ]
                }
            ]
        }
    },
    methods: {
        scrollToNextParallax(index) {
            const target = this.$refs.parallaxRefs[index];

            // 计算下一个parallax元素距离文档顶部的距离
            const elementTop = target.offsetTop;

            // 滚动到下一个parallax元素顶部
            window.scrollTo({
                top: elementTop,
                behavior: 'smooth' // 使滚动平滑
            });
        },
        // 立即滚动到页面顶部
        scrollToTop() {
            window.scroll({
                top: 0,
                left: 0,
                behavior: 'auto' // 'auto' 表示立即滚动
            });
        }
    },
    mounted() {
        this.observer = new IntersectionObserver((entries) => {
            entries.forEach((entry) => {
                // 如果元素在视口中出现部分达到50%
                if (entry.intersectionRatio >= 0.5) {
                    // 获取当前元素
                    const targetElement = entry.target;

                    // 计算元素距离文档顶部的距离
                    const elementTop = targetElement.offsetTop;

                    // 滚动到元素顶部
                    window.scrollTo({
                        top: elementTop,
                        behavior: 'smooth' // 平滑滚动
                    });
                }
            });
        },
        {
            // 配置选项
            root: null, // 观察者根元素，null表示视口
            rootMargin: '0px', // 根元素的边界盒，可以是正值或负值
            threshold: 0.5 // 触发回调的阈值
        });

        const parallaxRefs = this.$refs.parallaxRefs;
        parallaxRefs.unshift(this.$refs.parallaxStart)

        // 监听parallax元素的视口变化
        parallaxRefs.forEach((el) => {
            this.observer.observe(el);
        });

        return {
            parallaxRefs
        }
    },
    beforeUnmount() {
        const parallaxRefs = this.$refs.parallaxRefs;
        parallaxRefs.forEach((el) => {
            this.observer.unobserve(el);
        });
        this.observer.disconnect();
    },    
}
</script>

<style scoped>
* {
    padding: 0;
    margin: 0;
    box-sizing: border-box;
    font-family: "Helvetica Neue", Helvetica, Arial, "PingFang SC", "Hiragino Sans GB", "Heiti SC", "Microsoft YaHei", "WenQuanYi Micro Hei", sans-serif;
}

.home .start .background-video {
    position: absolute;
    top: 50%;
    left: 50%;
    min-width: 100%;
    min-height: 100%;
    transform: translate(-50%, -50%);
    z-index: -1;
    filter: brightness(0.5);
}


.home .start .logo {
    position: absolute;
    top: 0;
    left: 0;
    transform: translate(50%, 50%);
    width: 6vw;
    height: 5vh;
    display: flex;
    justify-content: center;
    align-items: center;
    flex-direction: row;
}

.home .start .brand {
    color: rgba(255, 255, 255, 0.8);
    font-size: 1vw;
    margin-left: 0.5vw;
}

.home .start .content .title {
    font-size: 4em;
}

.home .start .content .intro {
    color: rgba(255, 255, 255, 0.5);
    padding: 1vh 0;
}

.home .start .content .login-btn {
    --border-width: 1px;
    --radius: 2vw;
    position: relative;
    border-radius: var(--radius);
    border: var(--border-width) solid transparent;
    background-color: rgb(69, 123, 204);
    padding: 1vh 2vw;
    margin-top: 2vh;
    text-decoration: none;
    display: flex;
    justify-content: center;
    align-items: center;
    cursor: pointer;
    transition: all 0.3s linear;
}

.home .start .content .login-btn:hover {
    background-color: rgb(60, 143, 238);
    box-shadow: 0 0 1vw rgba(255, 255, 255, 0.5);
}

.home .start .content .login-btn:active {
    background-color: rgb(60, 143, 238);
    box-shadow: 0 0 2vw rgba(255, 255, 255, 0.7) inset,
        0 0 1vw rgba(255, 255, 255, 0.5);
}

.home .start .content .login-btn .text{
    color: rgba(255, 255, 255, 0.5);
    font-size: 1vw;
    margin-right: 1vw;
}

.home .parallax {
    width: 100%;
    height: 100vh;
    position: relative;
    overflow: hidden;
    background-repeat: no-repeat;
    background-size: cover;
    background-position: center center;
    background-attachment: fixed;
}

.home .p1 {
    background-image: url("@/assets/image/1.jpg");
}

.home .p2 {
    background-image: url("@/assets/image/2.jpg");
}

.home .p3 {
    background-image: url("@/assets/image/3.jpg");
}

.home .p4 {
    background-image: url("@/assets/image/4.jpg");
}

.home .p5 {
    background-image: url("@/assets/image/5.jpg");
}

.home .p6 {
    background-image: url("@/assets/image/6.jpg");
}

.home .parallax .content {
    width: 50vw;
    height: 60vh;
    display: flex;
    justify-content: center;
    align-items: center;
    flex-direction: column;
    color: #fff;
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
}

.home .parallax .item {
    height: 20vh;
    background-color: rgba(0, 0, 0, 0.3);
}

.home .parallax .item .title {
    font-size: 3vw;
    padding: 2vh 0;
}

.home .parallax .item .intro {
    font-size: 1vw;
    color: rgba(255, 255, 255, 0.5);
    padding: 1vh 5vw;
}

.home .parallax .next {
    position: absolute;
    bottom: 0;
    left: 50%;
    transform: translate(-50%, -50%);
    margin-bottom: 2vh;
    padding: 0.25vh 1vw;
    cursor: pointer;
}

.home .parallax .next svg use{
    transition: all 0.5s linear;
}

.home .parallax .next:hover svg use{
    fill: #fff;
}

.footer {
    position: relative;
    display: flex;
    justify-content: center;
    align-items: center;
    flex-direction: column;
    background-color: #000;
}

.footer svg:deep(use) {
    transition: all 0.5s ease-in-out;
}

.footer svg:hover:deep(use) {
    fill: rgba(255, 255, 255, 0.8);
}

.footer .follow {
    margin: 0 auto;
    width: 60%;
    height: 10vh;
    display: flex;
    justify-content: center;
    align-items: center;
    border-bottom: 1px solid rgba(255, 255, 255, 0.3);
}

.footer .follow .follow-icon {
    padding: 1vh 1vw;
}

.footer .links {
    padding: 4vh 0;
    display: flex;
    justify-content: space-around;
    flex-wrap: wrap;
}

.footer .link-item {
    flex: 1;
    min-width: 10vw;
    margin-left: 5vw;
}

.footer .link-item h3 {
    color: rgba(255, 255, 255, 0.8);
    margin-bottom: 1vh;
}

.footer .link-item ul {
    list-style: none;
    padding: 0;
}

.footer .link-item li {
    margin-bottom: 5px;
}

.footer .link-item a {
    color: rgba(255, 255, 255, 0.5);
    text-decoration: none;
}

.footer .link-item a:hover {
    color: rgba(255, 255, 255, 0.7);
    transition: all 0.5s ease-out;
    text-decoration: underline;
}

.footer .top{
    position: absolute;
    bottom: 2.5vh;
    right: 2.5vw;
    cursor: pointer;
}

.footer .copyright {
    color: rgba(255, 255, 255, 0.3);
    font-family: 'Times New Roman', Times, serif;
    font-size: 0.8vw;
    margin: 2vh 0 4vh 0;
}
</style>