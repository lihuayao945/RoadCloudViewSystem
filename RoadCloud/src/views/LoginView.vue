<template>
  <div class="main-container">
    <nav class="navbar navbar-expand-lg fixed-top custom-navbar">
      <div class="container-fluid">
        <div class="mx-auto mt-3">
          <!-- 橙红色圆环 -->
          <div class="circle-ring2"></div>
        </div>
      </div>
    </nav>
    
    <div 
      id="message-box" 
      class="message-box" 
      :class="{ 'show': showMessage }"
    >
      {{ message }}
    </div>

    <!-- 使用flex布局的内容包装容器 -->
    <div class="content-wrapper">
      <!-- 图片容器 -->
      <div class="image-container rounded">
        <img :src="imageSource" alt="Login Image" class="responsive-image">
      </div>

      <div class="shell">
        <div class="container a-container" id="a-container" :class="{ 'is-txl': isLogin }">
          <form @submit.prevent="submitRegistration" class="form">
            <h2 class="form_title title2">Register Account</h2>
            <span class="form_span">Road Cloud System Visualization for Login</span>
            <input 
              type="text" 
              v-model="registerForm.username" 
              class="form_input" 
              placeholder="Username" 
              required
              :disabled="isLoading"
            >
            <input 
              type="password" 
              v-model="registerForm.password" 
              class="form_input" 
              placeholder="Password" 
              required
              :disabled="isLoading"
            >
            <button 
              type="submit" 
              class="form_button button submit"
              :disabled="isLoading"
            >
              {{ isLoading ? '注册中...' : 'Register' }}
            </button>
          </form>
        </div>

        <div class="container b-container" id="b-container" :class="{ 'is-txl': isLogin, 'is-z': isLogin }">
          <form @submit.prevent="submitLogin" class="form" id="b-form">
            <h2 class="form_title title2">Login Account</h2>
            <span class="form_span">Face recognition activated for Login</span>
            <input 
              type="text" 
              v-model="loginForm.username" 
              class="form_input" 
              placeholder="Username"
              required
            >
            <input 
              type="password" 
              v-model="loginForm.password" 
              class="form_input" 
              placeholder="Password"
              required
            >
            <button type="submit" class="form_button button">Login</button>
          </form>
        </div>

        <div class="switch" id="switch-cnt" :class="{ 'is-txr': isLogin }">
          <div class="switch_circle" :class="{ 'is-txr': isLogin }"></div>
          <div class="switch_circle switch_circle-t" :class="{ 'is-txr': isLogin }"></div>
          
          <div class="switch_container" id="switch-c1" :class="{ 'is-hidden': isLogin }">
            <h2 class="switch_title title" style="letter-spacing: 0;">Welcome Back！</h2>
            <p class="switch_description description">Already have an account? Let's get started!</p>
            <button class="switch_button button switch-btn" @click="changeForm">SIGN IN</button>
          </div>

          <div class="switch_container" id="switch-c2" :class="{ 'is-hidden': !isLogin }">
            <h2 class="switch_title title" style="letter-spacing: 0;">Hello Friend！</h2>
            <p class="switch_description description">Sign up for an account now !</p>
            <button class="switch_button button switch-btn" @click="changeForm">SIGN UP</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'LoginRegister',
  data() {
    return {
      isLogin: false,
      showMessage: false,
      message: '',
      isGx: false,
      isLoading: false,
      registerForm: {
        username: '',
        password: ''
      },
      loginForm: {
        username: '',
        password: ''
      },
      imageSource: 'public/login.png'
    };
  },
  mounted() {
    // 检查URL参数中是否有消息
    const urlParams = new URLSearchParams(window.location.search);
    const message = urlParams.get('message');
    if (message) {
      this.message = message;
      this.showMessage = true;
      setTimeout(() => {
        this.showMessage = false;
      }, 4000); // 消息将在4秒后消失
    }
    
    // 添加窗口大小变化监听器以响应调整大小
    window.addEventListener('resize', this.handleResize);
    // 初始调用一次以设置初始布局
    this.handleResize();
  },
  beforeUnmount() {
    // 组件销毁前移除监听器
    window.removeEventListener('resize', this.handleResize);
  },
  methods: {
    handleResize() {
      // 获取当前窗口宽度
      const windowWidth = window.innerWidth;
      
      // 基于窗口宽度调整布局
      // 这里可以添加特定的调整逻辑，如果需要的话
      // 实际的自适应工作主要通过CSS媒体查询完成
    },
    changeForm() {
      this.isGx = true;
      setTimeout(() => {
        this.isGx = false;
      }, 1500);
      this.isLogin = !this.isLogin;
    },
    submitRegistration() {
      // 防止重复提交
      if (this.isLoading) return;
      
      // 设置加载状态
      this.isLoading = true;

      // 检查网络连接
      if (!navigator.onLine) {
        this.message = '网络连接已断开，请检查网络设置';
        this.showMessage = true;
        this.isLoading = false;
        setTimeout(() => {
          this.showMessage = false;
        }, 4000);
        return;
      }

      // 构建请求配置
      const config = {
        method: 'post',
        url: '/api/register',
        headers: { 
          'Content-Type': 'application/json'
        },
        data: {
          username: this.registerForm.username,
          password: this.registerForm.password
        }
      };

      // 发送注册请求
      axios(config)
        .then(response => {
          // axios直接返回解析后的JSON数据
          const data = response.data;
          if (data.status === 'success') {
            this.message = '注册成功！';
            this.showMessage = true;
            this.isLogin = true;
            // 清空表单
            this.registerForm.username = '';
            this.registerForm.password = '';
          } else {
            this.message = data.msg || '注册失败，请稍后重试';
            this.showMessage = true;
          }
        })
        .catch(error => {
          console.error('Registration error:', error);
          if (error.response) {
            // 服务器返回了错误状态码
            this.message = `服务器响应错误 (${error.response.status})，请联系管理员`;
          } else if (error.request) {
            // 请求已发送但没有收到响应
            this.message = '无法连接到服务器，请确认服务器是否启动';
          } else {
            // 请求设置有问题
            this.message = '请求发送失败: ' + error.message;
          }
          this.showMessage = true;
        })
        .finally(() => {
          this.isLoading = false;
          setTimeout(() => {
            this.showMessage = false;
          }, 4000);
        });
    },
    submitLogin() {
      // 防止重复提交
      if (this.isLoading) return;
      
      // 设置加载状态
      this.isLoading = true;

      // 检查网络连接
      if (!navigator.onLine) {
        this.message = '网络连接已断开，请检查网络设置';
        this.showMessage = true;
        this.isLoading = false;
        setTimeout(() => {
          this.showMessage = false;
        }, 4000);
        return;
      }

      // 构建请求配置
      const config = {
        method: 'post',
        url: '/api/login',
        headers: { 
          'Content-Type': 'application/json'
        },
        data: {
          username: this.loginForm.username,
          password: this.loginForm.password
        }
      };

      // 发送登录请求
      axios(config)
        .then(response => {
          // axios直接返回解析后的JSON数据
          const data = response.data;
          if (data.status === 'success') {
            // 保存JWT令牌到localStorage，用于后续的身份验证
            if (data.Authorization) {
              localStorage.setItem('token', data.Authorization);
              console.log('JWT令牌已保存');
            }
            
            this.message = '登录成功！';
            this.showMessage = true;
            // 清空表单
            this.loginForm.username = '';
            this.loginForm.password = '';
            // 登录成功后跳转到首页
            setTimeout(() => {
              this.$router.push('/index');
            }, 1500);
          } else {
            this.message = data.msg || '登录失败，请检查用户名和密码';
            this.showMessage = true;
          }
        })
        .catch(error => {
          console.error('Login error:', error);
          if (error.response) {
            // 服务器返回了错误状态码
            this.message = `服务器响应错误 (${error.response.status})，请联系管理员`;
          } else if (error.request) {
            // 请求已发送但没有收到响应
            this.message = '无法连接到服务器，请确认服务器是否启动';
          } else {
            // 请求设置有问题
            this.message = '请求发送失败: ' + error.message;
          }
          this.showMessage = true;
        })
        .finally(() => {
          this.isLoading = false;
          setTimeout(() => {
            this.showMessage = false;
          }, 4000);
        });
    },
    goToHome() {
      // 导航到首页
      window.location.href = '/index';
      // 如果使用Vue Router，可以改为：
      // this.$router.push('/index');
    }
  }
};
</script>

<style>
/* 全局样式，确保应用到整个文档 */
html, body {
  width: 100%;
  height: 100%;
  margin: 0;
  padding: 0;
  background-color: #000000 !important; /* 使用 !important 确保优先级 */
  overflow-x: hidden; /* 防止水平滚动条 */
}

/* 创建全局根容器 */
#app {
  min-height: 100vh;
  background-color: #000000;
  color: #a0a5a8;
}

/* 添加视口元标记确保适当缩放（通常在HTML头部） */
@viewport {
  width: device-width;
  zoom: 1.0;
}
</style>

<style scoped>
.main-container {
  position: relative;
  width: 100%;
  min-height: 100vh;
  background-color: #000000;
  padding: 0;
  margin: 0;
  overflow-x: hidden;
  display: flex;
  flex-direction: column;
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
  /* 字体无法选中 */
  user-select: none;
}

/* 内容包装器样式 - 保持当前大小 */
.content-wrapper {
  display: flex;
  flex-direction: row;
  justify-content: center;
  align-items: center;
  gap: 10vw;
  padding: 0;
  margin: 0 auto;
  width: 100%;
  height: 100%;
  background-color: #000000;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
}

/* 表单容器样式 - 调整为更紧凑的布局 */
.shell {
  position: relative;
  width: 100%; /* 稍微减小宽度比例 */
  max-width: none; /* 减小最大宽度，更紧凑 */
  height: 50%;
  /* min-height: 600px; */
  min-width: 700px; 
  margin: 0 auto;
  padding: 10px; /* 减小内边距 */
  background-color: rgba(22, 24, 26, 0.81);
  box-shadow: 10px 10px 10px rgba(22, 24, 26, 0.81), -10px -10px 10px rgba(22, 24, 26, 0.81);
  border-radius: 8px;
  overflow: hidden;
  transition: all 0.3s ease;
}

/* 调整内部容器大小 */
.container {
  display: flex;
  justify-content: center;
  align-items: center;
  position: absolute;
  top: 0;
  width: 50%;
  /* max-width: 600px; 匹配shell的最大宽度 */
  height: 100%;
  padding: 0; /* 减小内边距 */
  background-color: #1f1f21;
  transition: 1.25s;
}

/* 图片容器样式 - 保持大小 */
.image-container {
  width: 40%;
  max-width: 550px;
  height: auto;
  min-height: 600px;
  display: flex;
  justify-content: center;
  align-items: center;
  background: none;
  /* border: 2px solid #ffffff; */
  border-radius: 15px;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.5);
  overflow: hidden;
  transition: all 0.3s ease;
}

/* 响应式图片 */
.responsive-image {
  max-width: 100%;
  height: auto;
  object-fit: cover;
}

.form {
  display: flex;
  justify-content: center;
  align-items: center;
  flex-direction: column;
  width: 100%;
  height: 100%;
}

.iconfont {
  margin: 0 5px;
  border: rgba(0, 0, 0, 0.5) 2px solid;
  border-radius: 50%;
  font-size: 25px;
  padding: 3px;
  opacity: 0.5;
  transition: 0.1s;
}

.iconfont:hover {
  opacity: 1;
  transition: 0.15s;
  cursor: pointer;
}

/* 调整表单输入字段尺寸 */
.form_input {
  width: 60%; /* 减小宽度百分比 */
  /* max-width: 320px; 减小最大宽度，更紧凑 */
  height: 30px; /* 减小高度 */
  margin: 8px 0; /* 减小间距 */
  padding-left: 15px; /* 减小内边距 */
  font-size: 15px;
  letter-spacing: 0.20px;
  border: none;
  outline: none;
  background-color: #ecf0f3;
  transition: 0.25s ease;
  border-radius: 8px;
  box-shadow: inset 2px 2px 4px #d1d9e6, inset -2px -2px 4px #f9f9f9;
}

.form_input:focus {
  box-shadow: inset 4px 4px 4px #d1d9e6, inset -4px -4px 4px #f9f9f9;
}

/* 调整表单标题和间距 */
.form_span {
  margin-top: 25px; /* 减小上边距 */
  margin-bottom: 10px; /* 减小下边距 */
  text-align: center;
  font-size: 15px; /* 减小字体大小 */
}

.form_link {
  color: #181818;
  font-size: 10px;
  margin-top: 10px; /* 减小上边距 */
  border-bottom: 1px solid #a0a5a8;
  line-height: 1;
}

.title {
  font-size: 30px; /* 减小字体大小 */
  font-weight: 600;
  line-height: 1.5; /* 减小行高 */
  color: #ffffff;
  letter-spacing: 8px; /* 减小字母间距 */
  text-align: center;
}

.title2 {
  font-size: 30px; /* 减小字体大小 */
  font-weight: 600;
  line-height: 1.5; /* 减小行高 */
  color: #ffffff;
  text-align: center;
}

.description {
  font-size: 14px;
  letter-spacing: 0.1px;
  text-align: center;
  line-height: 2.5; /* 减小行高 */
}

/* 调整按钮尺寸 */
.button {
  width: 100px; /* 减小宽度 */
  height: 40px; /* 减小高度 */
  border-radius: 20px;
  margin-top: 30px; /* 减小上边距 */
  font-weight: 600;
  font-size: 14px;
  letter-spacing: 1.15px;
  background-color: #ee542e;
  color: #f9f9f9;
  box-shadow: 8px 8px 16px #3a3737, -8px -8px 16px #343332;
  border: none;
  outline: none;
  cursor: pointer;
}
/*  */
.a-container {
  z-index: 100;
  left: 50%;
}

.b-container {
  left: 100%;
  z-index: 100%;
}

/* 调整开关区域尺寸 */
.switch {
  display: flex;
  justify-content: center;
  align-items: center;
  position: absolute;
  top: 0;
  left: 0;
  height: 100%;
  min-width: 350px; 
  width: 50%; /* 减小宽度比例 */
  /* max-width: 300px; 减小最大宽度 */
  padding: 0; /* 减小内边距 */
  z-index: 200;
  transition: 1.5s;
  background-color: #16181a;
  overflow: hidden;
  box-shadow: 4px 4px 10px #16181a, -4px -4px 10px #16181a;
}

.switch_circle {
  position: absolute;
  width: 300px; /* 调整大小 */
  height: 300px; /* 调整大小 */
  border-radius: 50%;
  background-color: rgba(22, 24, 26, 0.97);
  box-shadow: inset 8px 8px 12px rgba(22, 24, 26, 0.81), inset -8px -8px 12px rgba(22, 24, 26, 0.81);
  bottom: -60%;
  left: -60%;
  transition: 1.25s;
}

.switch_circle-t {
  top: -30%;
  left: 60%;
  width: 300px; /* 调整大小 */
  height: 300px; /* 调整大小 */
}

.switch_container {
  display: flex;
  justify-content: center;
  align-items: center;
  flex-direction: column;
  position: absolute;
  width: 100%;
  max-width: 300px; /* 与switch最大宽度匹配 */
  padding: 25px; /* 减小内边距 */
  transition: 1.25s;
}

.switch_button {
  cursor: pointer;
}

.switch_button:hover,
.submit:hover {
  box-shadow: 6px 6px 10px #d1d9e6, -6px -6px 10px #f9f9f9;
  transform: scale(0.985);
  transition: 0.25s;
}

.switch_button:active,
.switch_button:focus {
  box-shadow: 2px 2px 6px #d1d9e6, -2px -2px 6px #f9f9f9;
  transform: scale(0.97);
  transition: 0.25s;
}

.is-txr {
  left: calc(100% - 50%); /* 调整为与switch宽度匹配 */
  transition: 1.25s;
  transform-origin: left;
}

.is-txl {
  left: 0;
  transition: 1.25s;
  transform-origin: right;
}

.is-z {
  z-index: 200;
  transition: 1.25s;
}

.is-hidden {
  visibility: hidden;
  opacity: 0;
  position: absolute;
  transition: 1.25s;
}

.is-gx {
  animation: is-gx 1.25s;
}

@keyframes is-gx {
  0%,
  10%,
  100% {
    width: 50%;
    max-width: 300px;
  }

  30%,
  50% {
    width: 65%;
    max-width: 390px;
  }
}

/* 改进响应式布局 */
/* 大型设备 */
@media screen and (min-width: 1400px) {
  .content-wrapper {
    max-width: 85%;
    gap: 2vw;
    padding: 10px; /* 减少内边距 */
  }
  
  .shell {
    width: 45%;
    max-width: 600px;
  }
  
  .image-container {
    width: 38%;
    max-width: 550px;
  }
  
  .form_input {
    max-width: 320px;
  }
  
  .switch {
    width: 50%;
    max-width: 300px;
  }
  
  .is-txr {
    left: calc(100% - 50%);
  }
  
  @keyframes is-gx {
    0%, 10%, 100% {
      width: 50%;
      max-width: 300px;
    }
    30%, 50% {
      width: 65%;
      max-width: 390px;
    }
  }
}

/* 中型设备 */
@media screen and (max-width: 1399px) and (min-width: 992px) {
  .content-wrapper {
    max-width: 90%;
    gap: 6vw;
  }
  
  .shell {
    width: 48%;
    max-width: 580px;
  }
  
  .image-container {
    width: 38%;
    max-width: 520px;
  }
  
  .form_input {
    max-width: 300px;
  }
  
  .switch {
    width: 50%;
    max-width: 290px;
  }
  
  .is-txr {
    left: calc(100% - 50%);
  }
  
  @keyframes is-gx {
    0%, 10%, 100% {
      width: 50%;
      max-width: 290px;
    }
    30%, 50% {
      width: 65%;
      max-width: 380px;
    }
  }
}

/* 小型设备 */
@media screen and (max-width: 991px) {
  .content-wrapper {
    flex-direction: column;
    gap: 40px;
    padding: 40px 20px;
    justify-content: flex-start;
    overflow-y: auto;
    height: auto;
    min-height: 100vh;
    position: relative;
  }
  
  .shell {
    width: 90%;
    max-width: 550px;
    min-height: 520px;
  }
  
  .image-container {
    width: 90%;
    max-width: 550px;
    min-height: 350px;
  }
  
  .title, .title2 {
    font-size: 28px;
    line-height: 2.2;
  }
  
  .description {
    font-size: 14px;
  }
  
  .form_input {
    max-width: 300px;
  }
  
  .switch {
    width: 60%;
    max-width: 330px;
    padding: 25px;
  }
  
  .switch_container {
    padding: 25px;
  }
  
  .is-txr {
    left: calc(100% - 60%);
  }
  
  @keyframes is-gx {
    0%, 10%, 100% {
      width: 60%;
      max-width: 330px;
    }
    30%, 50% {
      width: 75%;
      max-width: 420px;
    }
  }
}

/* 超小型设备（手机） */
@media screen and (max-width: 576px) {
  .content-wrapper {
    padding: 30px 15px;
    gap: 30px;
    max-width: 100%;
  }
  
  .shell {
    width: 95%;
    min-height: 480px;
    padding: 20px;
  }
  
  .container {
    padding: 20px;
  }
  
  .image-container {
    width: 95%;
    min-height: 280px;
  }
  
  .title, .title2 {
    font-size: 24px;
    line-height: 2;
    letter-spacing: 3px;
  }
  
  .form_span {
    margin-top: 15px;
    margin-bottom: 8px;
    font-size: 13px;
  }
  
  .form_input {
    max-width: 280px;
    height: 35px;
  }
  
  .button {
    width: 150px;
    height: 40px;
    margin-top: 25px;
    font-size: 13px;
  }
  
  .switch {
    width: 65%;
    max-width: 310px;
    padding: 20px;
  }
  
  .switch_container {
    padding: 20px;
  }
  
  .is-txr {
    left: calc(100% - 65%);
  }
  
  @keyframes is-gx {
    0%, 10%, 100% {
      width: 65%;
      max-width: 310px;
    }
    30%, 50% {
      width: 80%;
      max-width: 380px;
    }
  }
}

/* 极小型设备 */
@media screen and (max-width: 375px) {
  .content-wrapper {
    padding: 20px 10px;
    gap: 20px;
  }
  
  .shell {
    min-height: 450px;
    padding: 15px;
  }
  
  .container {
    padding: 15px;
  }
  
  .image-container {
    min-height: 230px;
  }
  
  .title, .title2 {
    font-size: 22px;
  }
  
  .form_input {
    max-width: 250px;
    height: 35px;
  }
  
  .button {
    width: 140px;
    height: 40px;
    margin-top: 20px;
  }
  
  .switch {
    width: 70%;
    max-width: 280px;
    padding: 15px;
  }
  
  .switch_container {
    padding: 15px;
  }
  
  .is-txr {
    left: calc(100% - 70%);
  }
  
  @keyframes is-gx {
    0%, 10%, 100% {
      width: 70%;
      max-width: 280px;
    }
    30%, 50% {
      width: 85%;
      max-width: 340px;
    }
  }
}

.navbar-brand img {
  height: 30px;
  margin-right: 10px;
}

.main-heading {
  font-size: 5em;
  font-weight: normal;
  padding-top: 14%;
  text-align: left;
  line-height: 65px;
  padding-left: 14%;
}

.container-main-heading {
  padding-left: 15px;
}

.custom-navbar {
  background-color: #050000;
  z-index: 1001; /* 确保导航栏在最上层 */
}

.navbar-light .navbar-nav .nav-link {
  color: #ffffff;
}

.message-box {
  position: fixed;
  top: 20px;
  right: 20px;
  background-color: rgba(0, 0, 0, 0.8);
  color: #fff;
  padding: 15px 20px;
  border-radius: 5px;
  z-index: 2000; /* 确保消息框在最上层 */
  display: none;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.message-box.show {
  display: block;
  animation: slideIn 0.3s ease;
}

@keyframes slideIn {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

.form_button:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.form_input:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}
</style>