<template>
    <svg
        aria-hidden="true"
        class="svg-icon" 
        :style="'width:' + size + '; height:' + size">
        <use :xlink:href="symbolId" :fill="color"/>
    </svg>
</template>

<style scoped>
.svg-icon{
    display: inline-block;
    outline: none;
    width: 1em;
    height: 1em;
    vertical-align: -0.15em;    /* 因为icon大小设置为与字体一致，而span等标签的下缘线会和字体的基线对齐，故需要设置一个往下的偏移，来纠正视觉上的未对齐效果 */
    fill: currentColor;         /* currentColor为变量，如果未设置值则默认从父元素继承 */
    overflow: hidden;
}
</style>

<script setup>
import { computed } from 'vue';

const props = defineProps({
    prefix: {
        type: String,
        default: 'icon'
    },
    iconClass: {
        type: String,
        required: true
    },
    color: {
        type: String,
    },
    size: {
        type: String,
        default: '1em'
    }
});

const symbolId = computed(() => `#${props.prefix}-${props.iconClass}`);
</script>