<template>
  <div class="user-auth-container">
    <el-card class="auth-card" shadow="hover">
      <div slot="header" class="card-header">
        <el-button 
          type="text" 
          :class="{ 'active-tab': activeTab === 'login' }"
          @click="switchTab('login')"
          style="font-size: 16px; margin-right: 20px;"
        >
          登录
        </el-button>
        <el-button 
          type="text" 
          :class="{ 'active-tab': activeTab === 'register' }"
          @click="switchTab('register')"
          style="font-size: 16px;"
        >
          注册
        </el-button>
      </div>
      
      <!-- 登录表单 -->
      <el-form 
        v-show="activeTab === 'login'" 
        :model="loginForm" 
        :rules="loginRules" 
        ref="loginForm"
        label-width="80px"
      >
        <el-form-item label="用户名" prop="username">
          <el-input 
            v-model="loginForm.username" 
            placeholder="请输入用户名"
            prefix-icon="el-icon-user"
          ></el-input>
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input 
            v-model="loginForm.password" 
            type="password" 
            placeholder="请输入密码"
            prefix-icon="el-icon-lock"
          ></el-input>
        </el-form-item>
        <el-form-item>
          <el-button 
            type="primary" 
            @click="handleLogin" 
            :loading="loginLoading" 
            style="width: 100%;"
          >
            {{ loginLoading ? '登录中...' : '登录' }}
          </el-button>
        </el-form-item>
      </el-form>
      
      <!-- 注册表单 -->
      <el-form 
        v-show="activeTab === 'register'" 
        :model="registerForm" 
        :rules="registerRules" 
        ref="registerForm"
        label-width="80px"
      >
        <el-form-item label="用户名" prop="username">
          <el-input 
            v-model="registerForm.username" 
            placeholder="请输入用户名"
            prefix-icon="el-icon-user"
          ></el-input>
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input 
            v-model="registerForm.password" 
            type="password" 
            placeholder="请输入密码"
            prefix-icon="el-icon-lock"
          ></el-input>
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input 
            v-model="registerForm.confirmPassword" 
            type="password" 
            placeholder="请再次输入密码"
            prefix-icon="el-icon-lock"
          ></el-input>
        </el-form-item>
        <el-form-item>
          <el-button 
            type="success" 
            @click="handleRegister" 
            :loading="registerLoading" 
            style="width: 100%;"
          >
            {{ registerLoading ? '注册中...' : '注册' }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
import { registerUser, loginUser, checkUsername } from '@/api/user'

export default {
  name: 'UserAuth',
  data() {
    // 自定义验证规则 - 确认密码
    const validateConfirmPassword = (rule, value, callback) => {
      if (value !== this.registerForm.password) {
        callback(new Error('两次输入的密码不一致'))
      } else {
        callback()
      }
    }

    return {
      activeTab: 'login', // 当前激活的标签页
      loginForm: {
        username: '',
        password: ''
      },
      registerForm: {
        username: '',
        password: '',
        confirmPassword: ''
      },
      loginLoading: false,
      registerLoading: false,
      loginRules: {
        username: [
          { required: true, message: '请输入用户名', trigger: 'blur' },
          { min: 3, max: 20, message: '用户名长度在3到20个字符之间', trigger: 'blur' },
          // 添加用户名格式验证，允许字母、数字、下划线
          { pattern: /^[a-zA-Z0-9_\u4e00-\u9fa5]+$/, message: '用户名只能包含字母、数字、下划线或中文', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' },
          { min: 6, max: 20, message: '密码长度在6到20个字符之间', trigger: 'blur' }
        ]
      },
      registerRules: {
        username: [
          { required: true, message: '请输入用户名', trigger: 'blur' },
          { min: 3, max: 20, message: '用户名长度在3到20个字符之间', trigger: 'blur' },
          // 添加用户名格式验证，允许字母、数字、下划线
          { pattern: /^[a-zA-Z0-9_\u4e00-\u9fa5]+$/, message: '用户名只能包含字母、数字、下划线或中文', trigger: 'blur' },
          { validator: this.validateUsername, trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' },
          { min: 6, max: 20, message: '密码长度在6到20个字符之间', trigger: 'blur' }
        ],
        confirmPassword: [
          { required: true, message: '请再次输入密码', trigger: 'blur' },
          { validator: validateConfirmPassword, trigger: 'blur' }
        ]
      }
    }
  },
  methods: {
    // 切换标签页
    switchTab(tab) {
      this.activeTab = tab
    },
    
    // 验证用户名是否已存在
    validateUsername(rule, value, callback) {
      if (!value) {
        callback()
        return
      }
      
      checkUsername(value).then(response => {
        if (response.data) {
          callback(new Error('用户名已存在'))
        } else {
          callback()
        }
      }).catch(error => {
        callback(new Error('验证用户名时发生错误'))
      })
    },
    
    // 处理登录
    async handleLogin() {
      this.$refs.loginForm.validate(valid => {
        if (valid) {
          this.loginLoading = true
          try {
            loginUser(this.loginForm).then(response => {
              this.loginLoading = false
              if (response.code === 200) {
                // 登录成功，保存用户信息和token
                const { user, token } = response.data
                localStorage.setItem('user', JSON.stringify(user))
                localStorage.setItem('token', token)
                
                this.$message.success('登录成功！')
                // 触发登录成功的事件
                this.$emit('login-success', { user, token })
              } else {
                this.$message.error(response.msg || '登录失败')
              }
            }).catch(error => {
              this.loginLoading = false
              this.$message.error(error.message || '登录失败')
            })
          } catch (error) {
            this.loginLoading = false
            this.$message.error(error.message || '登录失败')
          }
        } else {
          return false
        }
      })
    },
    
    // 处理注册
    async handleRegister() {
      this.$refs.registerForm.validate(valid => {
        if (valid) {
          this.registerLoading = true
          try {
            registerUser(this.registerForm).then(response => {
              this.registerLoading = false
              if (response.code === 200) {
                this.$message.success('注册成功，请登录！')
                // 自动切换到登录页
                this.switchTab('login')
                // 清空注册表单
                this.registerForm = {
                  username: '',
                  password: '',
                  confirmPassword: ''
                }
              } else {
                this.$message.error(response.msg || '注册失败')
              }
            }).catch(error => {
              this.registerLoading = false
              this.$message.error(error.message || '注册失败')
            })
          } catch (error) {
            this.registerLoading = false
            this.$message.error(error.message || '注册失败')
          }
        } else {
          return false
        }
      })
    }
  }
}
</script>

<style scoped>
.user-auth-container {
  width: 100%;
  max-width: 400px;
  margin: 20px auto;
}

.auth-card {
  border-radius: 8px;
  overflow: hidden;
}

.card-header {
  text-align: center;
  padding: 0 !important;
}

.card-header .el-button {
  padding: 15px 30px;
  border-bottom: 2px solid transparent;
}

.card-header .el-button.active-tab {
  border-bottom-color: #409EFF;
  color: #409EFF;
  font-weight: bold;
}

.el-form-item {
  margin-bottom: 20px;
}
</style>