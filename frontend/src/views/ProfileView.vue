<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { authApi, uploadApi } from '@/api'
import { roleLabel, useAuthStore } from '@/stores/auth'
import { avatarColor, formatDateTime } from '@/utils/format'
import PageHeader from '@/components/PageHeader.vue'

const auth = useAuthStore()
const profileForm = reactive({ phone: '', avatarUrl: '' })
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirm: '' })
const pwdRef = ref<FormInstance>()
const saving = ref(false)
const uploading = ref(false)

const rules: FormRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度需在 6-32 位之间', trigger: 'blur' }
  ],
  confirm: [
    {
      validator: (_rule, value, callback) => {
        if (!value) callback(new Error('请再次输入新密码'))
        else if (value !== pwdForm.newPassword) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ]
}

const initial = computed(() => auth.user?.realName?.slice(0, 1) || 'C')

onMounted(async () => {
  try {
    const me = await auth.fetchMe()
    profileForm.phone = me.phone || ''
    profileForm.avatarUrl = me.avatarUrl || ''
  } catch {
    // handled by http interceptor
  }
})

const saveProfile = async () => {
  saving.value = true
  try {
    const user = await authApi.updateProfile(profileForm)
    auth.updateUser(user)
    ElMessage.success('个人资料已更新')
  } finally {
    saving.value = false
  }
}

const uploadAvatar = async (options: any) => {
  uploading.value = true
  try {
    const result = await uploadApi.upload(options.file)
    profileForm.avatarUrl = result.url
    const user = await authApi.updateProfile({ avatarUrl: result.url })
    auth.updateUser(user)
    ElMessage.success('头像已更新')
  } finally {
    uploading.value = false
  }
}

const changePassword = async () => {
  if (!pwdRef.value) return
  const valid = await pwdRef.value.validate().catch(() => false)
  if (!valid) return
  await authApi.changePassword({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
  ElMessage.success('密码修改成功，请牢记新密码')
  pwdForm.oldPassword = ''
  pwdForm.newPassword = ''
  pwdForm.confirm = ''
}
</script>

<template>
  <div class="page">
    <PageHeader title="个人中心" subtitle="维护账号资料与登录安全，所有敏感信息均由后端校验" eyebrow="PROFILE" />

    <div class="grid grid-main-side">
      <div class="panel profile-card">
        <div class="cover" />
        <div class="avatar-block">
          <el-upload
            :show-file-list="false"
            :http-request="uploadAvatar"
            accept="image/png,image/jpeg,image/webp"
          >
            <div class="avatar-big" :style="{ color: avatarColor(auth.user?.realName) }">
              <img v-if="auth.user?.avatarUrl" :src="auth.user.avatarUrl" alt="avatar" />
              <span v-else>{{ initial }}</span>
              <div class="avatar-mask"><el-icon><Camera /></el-icon></div>
            </div>
          </el-upload>
          <div>
            <div class="name">{{ auth.user?.realName }}</div>
            <div class="row wrap" style="margin-top: 6px">
              <span class="chip cyan"><i class="dot" />{{ roleLabel(auth.role) }}</span>
              <span class="chip mono">{{ auth.user?.relatedNo || auth.user?.username }}</span>
            </div>
          </div>
        </div>

        <div class="divider" />

        <div class="info-list">
          <div><span>登录账号</span><strong class="mono">{{ auth.user?.username }}</strong></div>
          <div v-if="auth.user?.className"><span>所属班级</span><strong>{{ auth.user.className }}</strong></div>
          <div v-if="auth.user?.department"><span>所属院系</span><strong>{{ auth.user.department }}</strong></div>
          <div><span>手机号</span><strong class="mono">{{ auth.user?.phone || '未填写' }}</strong></div>
          <div><span>最近登录</span><strong class="mono">{{ formatDateTime(auth.user?.lastLoginTime) }}</strong></div>
        </div>
        <div class="uploading" v-if="uploading">头像上传中…</div>
      </div>

      <div class="col" style="gap: 16px">
        <div class="panel">
          <div class="panel-head">
            <div class="panel-title">基本资料</div>
            <span class="panel-sub">头像支持 jpg / png / webp</span>
          </div>
          <el-form label-position="top">
            <el-form-item label="手机号">
              <el-input v-model="profileForm.phone" placeholder="用于接收课堂提醒" maxlength="20" />
            </el-form-item>
            <el-form-item label="头像地址">
              <el-input v-model="profileForm.avatarUrl" placeholder="上传后自动填充，也可粘贴图片地址" />
            </el-form-item>
            <el-button type="primary" :loading="saving" @click="saveProfile">保存资料</el-button>
          </el-form>
        </div>

        <div class="panel">
          <div class="panel-head">
            <div class="panel-title">修改密码</div>
            <span class="panel-sub">密码使用 BCrypt 加密存储</span>
          </div>
          <el-form ref="pwdRef" :model="pwdForm" :rules="rules" label-position="top">
            <el-form-item label="原密码" prop="oldPassword">
              <el-input v-model="pwdForm.oldPassword" type="password" show-password />
            </el-form-item>
            <div class="grid grid-2" style="gap: 12px">
              <el-form-item label="新密码" prop="newPassword">
                <el-input v-model="pwdForm.newPassword" type="password" show-password />
              </el-form-item>
              <el-form-item label="确认新密码" prop="confirm">
                <el-input v-model="pwdForm.confirm" type="password" show-password />
              </el-form-item>
            </div>
            <el-button type="primary" plain @click="changePassword">更新密码</el-button>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.profile-card { padding: 0; overflow: hidden; align-self: start; }
.cover {
  height: 96px;
  background:
    radial-gradient(circle at 20% 20%, rgba(56, 221, 248, 0.22), transparent 60%),
    radial-gradient(circle at 80% 0%, rgba(45, 212, 191, 0.16), transparent 55%),
    #101828;
  border-bottom: 1px solid var(--line);
}
.avatar-block { display: flex; gap: 16px; align-items: flex-end; padding: 0 22px 0; margin-top: -34px; }
.avatar-big {
  width: 76px; height: 76px; border-radius: 18px;
  display: flex; align-items: center; justify-content: center;
  font-size: 26px; font-weight: 650;
  background: #101828;
  border: 1px solid rgba(56, 221, 248, 0.35);
  position: relative; overflow: hidden; cursor: pointer;
}
.avatar-big img { width: 100%; height: 100%; object-fit: cover; }
.avatar-mask {
  position: absolute; inset: auto 0 0; height: 24px;
  background: rgba(4, 10, 20, 0.7);
  display: flex; align-items: center; justify-content: center;
  font-size: 11px; opacity: 0; transition: opacity 0.16s ease;
}
.avatar-big:hover .avatar-mask { opacity: 1; }
.name { font-size: 20px; font-weight: 650; margin-bottom: 4px; }
.info-list { padding: 0 22px 22px; display: flex; flex-direction: column; gap: 12px; }
.info-list div { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.info-list span { color: var(--muted); font-size: 12px; }
.info-list strong { font-size: 12.5px; font-weight: 500; color: var(--text-2); text-align: right; }
.uploading { padding: 0 22px 18px; font-size: 11px; color: var(--cyan); }
</style>
