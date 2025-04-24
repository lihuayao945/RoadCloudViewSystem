<template>
  <div class="profile-container">
    <div class="profile-header">
      <h1>个人资料</h1>
      <!-- <el-button 
        type="primary" 
        :icon="Edit" 
        @click="editMode = !editMode"
      >
        {{ editMode ? '取消编辑' : '修改资料' }}
      </el-button> -->
    </div>

    <el-card class="profile-card">
      <el-form 
        :model="userProfile" 
        :disabled="!editMode" 
        label-width="100px"
        ref="profileForm"
        :rules="formRules"
      >
        <el-row :gutter="20">
          <el-col :span="8">
            <div class="avatar-container">
              <el-avatar :size="120" :src="userProfile.avatar" />
              <el-upload
                v-if="editMode"
                class="avatar-uploader"
                action="#"
                :show-file-list="false"
                :auto-upload="false"
                :on-change="handleAvatarChange"
              >
                <el-button size="small" type="primary">更换头像</el-button>
              </el-upload>
            </div>
          </el-col>
          <el-col :span="16">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="userProfile.username" :disabled="true" />
            </el-form-item>
            
            <el-form-item label="真实姓名" prop="realName">
              <el-input v-model="userProfile.realName" />
            </el-form-item>
            
            <el-form-item label="电子邮箱" prop="email">
              <el-input v-model="userProfile.email" />
            </el-form-item>
            
            <el-form-item label="手机号码" prop="phone">
              <el-input v-model="userProfile.phone" />
            </el-form-item>
            
            <el-form-item label="所属部门" prop="department">
              <el-input v-model="userProfile.department" />
            </el-form-item>
            
            <el-form-item label="职位" prop="position">
              <el-input v-model="userProfile.position" />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-divider />
        
        <el-form-item label="个人简介" prop="bio">
          <el-input type="textarea" v-model="userProfile.bio" :rows="4" />
        </el-form-item>
        
        <el-form-item v-if="editMode">
          <el-button type="primary" @click="saveProfile">保存</el-button>
          <el-button @click="editMode = false">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    
    <!-- 修改密码部分 -->
    <el-card class="profile-card" v-if="editMode">
      <div class="card-header">
        <h2>修改密码</h2>
      </div>
      <el-form 
        :model="passwordForm" 
        label-width="100px"
        ref="passwordFormRef"
        :rules="passwordRules"
      >
        <el-form-item label="当前密码" prop="currentPassword">
          <el-input v-model="passwordForm.currentPassword" type="password" show-password />
        </el-form-item>
        
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" show-password />
        </el-form-item>
        
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" @click="changePassword">更新密码</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { Edit } from '@element-plus/icons-vue'

// 编辑模式标志
const editMode = ref(false)

// 用户资料数据
const userProfile = reactive({
  username: 'admin',
  realName: '管理员',
  email: 'admin@roadcloud.com',
  phone: '13800138000',
  department: '技术部',
  position: '系统管理员',
  bio: '负责RoadCloud系统的日常运维和管理工作。',
  avatar: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'
})

// 表单验证规则
const formRules = {
  email: [
    { required: true, message: '请输入电子邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的电子邮箱格式', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号码', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码格式', trigger: 'blur' }
  ]
}

// 密码表单数据
const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 密码表单验证规则
const passwordRules = {
  currentPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能小于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 保存个人资料
const saveProfile = () => {
  ElMessage({
    type: 'success',
    message: '个人资料已更新'
  })
  editMode.value = false
  // 这里应该添加与后端API交互的代码
}

// 修改密码
const changePassword = () => {
  ElMessage({
    type: 'success',
    message: '密码已成功修改'
  })
  passwordForm.currentPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  // 这里应该添加与后端API交互的代码
}

// 处理头像更改
const handleAvatarChange = (file) => {
  // 这里应该添加上传头像的逻辑
  const reader = new FileReader()
  reader.onload = (e) => {
    userProfile.avatar = e.target.result
  }
  reader.readAsDataURL(file.raw)
}
</script>

<style scoped>
.profile-container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 20px;
}

.profile-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.profile-header h1 {
  color: #00dffc;
  margin: 0;
  font-size: 24px;
  text-shadow: 0 0 10px rgba(0, 223, 252, 0.5);
}

.profile-card {
  margin-bottom: 20px;
  background: rgba(6, 11, 40, 0.6);
  border: 1px solid rgba(0, 223, 252, 0.2);
  box-shadow: 0 0 20px rgba(0, 140, 255, 0.1);
}

.avatar-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 20px;
  padding: 20px;
}

.avatar-uploader {
  margin-top: 10px;
}

.card-header {
  border-bottom: 1px solid rgba(0, 223, 252, 0.2);
  padding-bottom: 15px;
  margin-bottom: 20px;
}

.card-header h2 {
  color: #00dffc;
  font-size: 18px;
  margin: 0;
}

:deep(.el-divider) {
  background-color: rgba(0, 223, 252, 0.2);
}

:deep(.el-card__body) {
  padding: 20px;
}

@media (max-width: 768px) {
  .profile-container {
    padding: 10px;
  }
  
  .el-row {
    display: flex;
    flex-direction: column;
  }
  
  .el-col {
    width: 100% !important;
    margin-right: 0 !important;
  }
}
</style> 