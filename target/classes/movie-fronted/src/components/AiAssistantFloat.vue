<template>
  <div v-show="visible" class="ai-float-mask" @click.self="close">
    <div class="ai-float-panel" @click.stop>
      <div class="ai-float-header">
        <div class="ai-float-title">
          <i class="el-icon-chat-dot-round"></i>
          <span>AI 智能助手</span>
        </div>
        <el-button type="text" icon="el-icon-close" class="ai-float-close" @click="close"></el-button>
      </div>
      <div class="ai-float-body">
        <div class="ai-float-messages" ref="chatMessages">
          <div v-for="(msg, index) in chatMessages" :key="index" class="msg-item" :class="msg.role">
            <div class="msg-avatar" :class="msg.role">
              <i v-if="msg.role === 'user'" class="el-icon-user-solid"></i>
              <i v-if="msg.role === 'assistant'" class="el-icon-chat-line-round"></i>
            </div>
            <div class="msg-content">
              <div class="msg-bubble" :class="msg.role">
                <p v-html="formatMessage(msg.content)"></p>
              </div>
            </div>
          </div>
          <div v-if="aiLoading" class="msg-item assistant">
            <div class="msg-avatar assistant"><i class="el-icon-chat-line-round"></i></div>
            <div class="msg-content">
              <div class="msg-bubble assistant typing"><span></span><span></span><span></span></div>
            </div>
          </div>
        </div>
        <div class="ai-float-input">
          <el-input
            v-model="userInput"
            type="textarea"
            :rows="2"
            placeholder="输入问题，例如：推荐一些高分电影"
            maxlength="200"
            show-word-limit
            @keyup.enter.native="handleSend($event)"
          ></el-input>
          <el-button
            type="primary"
            :loading="aiLoading"
            @click="sendRequest"
            :disabled="!userInput.trim()"
            class="send-btn"
            icon="el-icon-position"
          >
            {{ aiLoading ? '思考中...' : '发送' }}
          </el-button>
        </div>
        <div class="ai-float-quick">
          <el-button size="mini" @click="setQuickPrompt('推荐一些高分电影')">高分电影</el-button>
          <el-button size="mini" @click="setQuickPrompt('推荐科幻电影')">科幻</el-button>
          <el-button size="mini" @click="setQuickPrompt('推荐喜剧电影')">喜剧</el-button>
          <el-button size="mini" @click="setQuickPrompt('推荐张艺谋导演的电影')">张艺谋</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { arkMultiChat } from '@/api/recommend'

export default {
  name: 'AiAssistantFloat',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    currentUser: {
      type: Object,
      default: null
    }
  },
  data() {
    return {
      chatMessages: [
        { role: 'assistant', content: '您好！我是AI电影推荐助手，可以根据您的喜好推荐电影。请告诉我您想看什么类型的电影？' }
      ],
      userInput: '',
      aiLoading: false
    }
  },
  watch: {
    visible(val) {
      if (val) this.$nextTick(() => this.scrollToBottom())
    },
    chatMessages: {
      handler() {
        this.$nextTick(() => this.scrollToBottom())
      },
      deep: true
    }
  },
  methods: {
    close() {
      this.$emit('close')
    },
    formatMessage(content) {
      return (content || '').replace(/\n/g, '<br>')
    },
    setQuickPrompt(prompt) {
      this.userInput = prompt
    },
    handleSend(e) {
      if (e.shiftKey) return
      e.preventDefault()
      this.sendRequest()
    },
    scrollToBottom() {
      this.$nextTick(() => {
        const el = this.$refs.chatMessages
        if (el) el.scrollTop = el.scrollHeight
      })
    },
    async sendRequest() {
      if (!this.userInput.trim()) return
      const userMsg = { role: 'user', content: this.userInput }
      this.chatMessages.push(userMsg)
      this.aiLoading = true
      const input = this.userInput
      this.userInput = ''
      try {
        const userId = this.currentUser ? (this.currentUser.userid || this.currentUser.id || this.currentUser.userId) : 'anonymous'
        const messages = this.chatMessages.slice(-10).map(m => ({ role: m.role, content: m.content }))
        const res = await arkMultiChat({ messages, userId })
        if (res && res.code === 200 && res.data) {
          this.chatMessages.push({ role: 'assistant', content: res.data })
        } else {
          this.chatMessages.push({ role: 'assistant', content: '抱歉，' + (res && res.msg ? res.msg : '服务暂时不可用') + '，请稍后重试。' })
        }
      } catch (err) {
        console.error('火山方舟对话失败:', err)
        const msg = (err.response && err.response.data && err.response.data.msg) ? err.response.data.msg : (err.message || '')
        const tip = msg ? `抱歉，${msg}，请稍后重试。` : '抱歉，对话服务暂时不可用，请检查网络或后端服务后重试。'
        this.chatMessages.push({ role: 'assistant', content: tip })
      } finally {
        this.aiLoading = false
      }
    }
  }
}
</script>

<style scoped>
.ai-float-mask {
  position: fixed;
  left: 0;
  top: 0;
  right: 0;
  bottom: 0;
  z-index: 1999;
  background: rgba(0, 0, 0, 0.3);
}

.ai-float-panel {
  position: absolute;
  right: 24px;
  top: 80px;
  bottom: 24px;
  width: 380px;
  max-height: calc(100vh - 120px);
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.ai-float-header {
  flex-shrink: 0;
  padding: 14px 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.ai-float-title {
  font-size: 16px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
}

.ai-float-close {
  color: #fff;
  font-size: 20px;
  padding: 4px;
}

.ai-float-body {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  padding: 12px;
  background: #f8f9fa;
}

/* 可滚动消息区：固定高度，保证能下拉 */
.ai-float-messages {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  -webkit-overflow-scrolling: touch;
  padding: 8px 0;
  margin-bottom: 12px;
  background: #fff;
  border-radius: 8px;
  padding: 12px;
}

.ai-float-messages::-webkit-scrollbar {
  width: 6px;
}

.ai-float-messages::-webkit-scrollbar-thumb {
  background: #c0c0c0;
  border-radius: 3px;
}

.msg-item {
  display: flex;
  align-items: flex-start;
  margin-bottom: 16px;
}

.msg-item.user {
  flex-direction: row-reverse;
}

.msg-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 16px;
}

.msg-avatar.user {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.msg-avatar.assistant {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.msg-content {
  max-width: calc(100% - 50px);
  margin: 0 10px;
  min-width: 0;
}

.msg-bubble {
  padding: 10px 14px;
  border-radius: 14px;
  font-size: 14px;
  line-height: 1.5;
  word-wrap: break-word;
  word-break: break-word;
}

.msg-bubble.user {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border-bottom-right-radius: 4px;
}

.msg-bubble.assistant {
  background: #fff;
  color: #333;
  border: 1px solid #e8e8e8;
  border-bottom-left-radius: 4px;
}

.msg-bubble p {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
}

.msg-bubble.typing {
  display: flex;
  gap: 4px;
  align-items: center;
}

.msg-bubble.typing span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #999;
  animation: typing 1.4s infinite;
}

.msg-bubble.typing span:nth-child(2) { animation-delay: 0.2s; }
.msg-bubble.typing span:nth-child(3) { animation-delay: 0.4s; }

@keyframes typing {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.7; }
  30% { transform: translateY(-6px); opacity: 1; }
}

.ai-float-input {
  flex-shrink: 0;
}

.ai-float-input .el-textarea__inner {
  border-radius: 8px;
  margin-bottom: 8px;
}

.send-btn {
  width: 100%;
  border-radius: 8px;
}

.ai-float-quick {
  flex-shrink: 0;
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.ai-float-quick .el-button {
  font-size: 12px;
  padding: 6px 10px;
}
</style>
