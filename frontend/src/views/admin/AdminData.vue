<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api'
import { formatDateTime } from '@/utils/format'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'

const route = useRoute()
const tab = ref('students')
const loading = ref(false)

const classOptions = ref<any[]>([])
const courseOptions = ref<any[]>([])
const teacherOptions = ref<any[]>([])

/* ============ 学生 ============ */
const students = ref<any[]>([])
const studentTotal = ref(0)
const studentPage = ref(1)
const studentSize = ref(10)
const studentFilters = reactive({ keyword: '', classId: undefined as number | undefined })
const studentDialog = ref(false)
const studentForm = reactive({ id: undefined as number | undefined, studentNo: '', name: '', classId: undefined as number | undefined, phone: '' })

/* ============ 教师 ============ */
const teachers = ref<any[]>([])
const teacherTotal = ref(0)
const teacherPage = ref(1)
const teacherFilters = reactive({ keyword: '' })
const teacherDialog = ref(false)
const teacherForm = reactive({ id: undefined as number | undefined, teacherNo: '', name: '', department: '', phone: '' })

/* ============ 班级 ============ */
const classes = ref<any[]>([])
const classDialog = ref(false)
const classForm = reactive({ id: undefined as number | undefined, name: '', grade: '', major: '' })

/* ============ 课程 ============ */
const courses = ref<any[]>([])
const courseDialog = ref(false)
const courseForm = reactive({
  id: undefined as number | undefined,
  courseNo: '',
  name: '',
  teacherId: undefined as number | undefined,
  semester: '2026-2027-1',
  credit: 3,
  coverColor: '#38DDF8',
  classroom: '',
  longitude: 118.1234567 as number | undefined,
  latitude: 32.1234567 as number | undefined,
  defaultRadius: 150,
  classIds: [] as number[]
})

/* ============ 账号 ============ */
const users = ref<any[]>([])
const userTotal = ref(0)
const userPage = ref(1)
const userFilters = reactive({ keyword: '', role: 'ALL' })
const userDialog = ref(false)
const userForm = reactive({
  id: undefined as number | undefined,
  username: '',
  password: '',
  role: 'STUDENT',
  realName: '',
  phone: '',
  enabled: true
})

/* ============ 导入 ============ */
const importVisible = ref(false)
const importReport = ref<any>(null)
const importing = ref(false)

const roleLabel = (role: string) => (role === 'ADMIN' ? '管理员' : role === 'TEACHER' ? '教师' : '学生')

const loadReferences = async () => {
  const [classList, courseList, teacherPage] = await Promise.all([
    adminApi.classes(),
    adminApi.courses(),
    adminApi.teachers({ page: 1, size: 100 })
  ])
  classOptions.value = classList
  courseOptions.value = courseList
  teacherOptions.value = teacherPage.list
}

const loadStudents = async () => {
  loading.value = true
  try {
    const data = await adminApi.students({
      page: studentPage.value,
      size: studentSize.value,
      keyword: studentFilters.keyword || undefined,
      classId: studentFilters.classId
    })
    students.value = data.list
    studentTotal.value = data.total
  } finally {
    loading.value = false
  }
}

const loadTeachers = async () => {
  loading.value = true
  try {
    const data = await adminApi.teachers({ page: teacherPage.value, size: 10, keyword: teacherFilters.keyword || undefined })
    teachers.value = data.list
    teacherTotal.value = data.total
  } finally {
    loading.value = false
  }
}

const loadClasses = async () => {
  loading.value = true
  try {
    classes.value = await adminApi.classes()
  } finally {
    loading.value = false
  }
}

const loadCourses = async () => {
  loading.value = true
  try {
    courses.value = await adminApi.courses()
  } finally {
    loading.value = false
  }
}

const loadUsers = async () => {
  loading.value = true
  try {
    const data = await adminApi.users({
      page: userPage.value,
      size: 10,
      keyword: userFilters.keyword || undefined,
      role: userFilters.role
    })
    users.value = data.list
    userTotal.value = data.total
  } finally {
    loading.value = false
  }
}

const loadTab = () => {
  if (tab.value === 'students') loadStudents()
  if (tab.value === 'teachers') loadTeachers()
  if (tab.value === 'classes') loadClasses()
  if (tab.value === 'courses') loadCourses()
  if (tab.value === 'users') loadUsers()
}

watch(tab, loadTab)

/* ============ 学生操作 ============ */
const openStudent = (row?: any) => {
  studentForm.id = row?.id
  studentForm.studentNo = row?.studentNo || ''
  studentForm.name = row?.name || ''
  studentForm.classId = row?.classId || classOptions.value[0]?.id
  studentForm.phone = row?.phone || ''
  studentDialog.value = true
}

const saveStudent = async () => {
  if (!studentForm.studentNo.trim() || !studentForm.name.trim() || !studentForm.classId) {
    ElMessage.warning('学号、姓名与班级为必填项')
    return
  }
  if (studentForm.id) await adminApi.updateStudent({ ...studentForm })
  else await adminApi.createStudent({ ...studentForm })
  ElMessage.success('学生信息已保存')
  studentDialog.value = false
  await Promise.all([loadStudents(), loadReferences()])
}

const removeStudent = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确认停用学生「${row.name}」？该学生的历史签到记录会保留。`, '停用学生', {
      confirmButtonText: '确认停用',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  await adminApi.deleteStudent(row.id)
  ElMessage.success('学生已停用')
  loadStudents()
}

const importStudents = async (options: any) => {
  importing.value = true
  try {
    importReport.value = await adminApi.importStudents(options.file)
    importVisible.value = true
    await Promise.all([loadStudents(), loadReferences()])
  } finally {
    importing.value = false
  }
}

/* ============ 教师操作 ============ */
const openTeacher = (row?: any) => {
  teacherForm.id = row?.id
  teacherForm.teacherNo = row?.teacherNo || ''
  teacherForm.name = row?.name || ''
  teacherForm.department = row?.department || ''
  teacherForm.phone = ''
  teacherDialog.value = true
}

const saveTeacher = async () => {
  if (!teacherForm.teacherNo.trim() || !teacherForm.name.trim()) {
    ElMessage.warning('工号与姓名为必填项')
    return
  }
  if (teacherForm.id) await adminApi.updateTeacher({ ...teacherForm })
  else await adminApi.createTeacher({ ...teacherForm })
  ElMessage.success('教师信息已保存')
  teacherDialog.value = false
  await Promise.all([loadTeachers(), loadReferences()])
}

const removeTeacher = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确认停用教师「${row.name}」？`, '停用教师', {
      confirmButtonText: '确认停用',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  await adminApi.deleteTeacher(row.id)
  ElMessage.success('教师已停用')
  loadTeachers()
}

/* ============ 班级操作 ============ */
const openClass = (row?: any) => {
  classForm.id = row?.id
  classForm.name = row?.name || ''
  classForm.grade = row?.grade || ''
  classForm.major = row?.major || ''
  classDialog.value = true
}

const saveClass = async () => {
  if (!classForm.name.trim()) {
    ElMessage.warning('请输入班级名称')
    return
  }
  if (classForm.id) await adminApi.updateClass({ ...classForm })
  else await adminApi.createClass({ ...classForm })
  ElMessage.success('班级信息已保存')
  classDialog.value = false
  await Promise.all([loadClasses(), loadReferences()])
}

const removeClass = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确认删除班级「${row.name}」？班级下有学生时无法删除。`, '删除班级', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  await adminApi.deleteClass(row.id)
  ElMessage.success('班级已删除')
  await loadClasses()
}

/* ============ 课程操作 ============ */
const openCourse = (row?: any) => {
  courseForm.id = row?.id
  courseForm.courseNo = row?.courseNo || ''
  courseForm.name = row?.name || ''
  courseForm.teacherId = row?.teacherId || teacherOptions.value[0]?.id
  courseForm.semester = row?.semester || '2026-2027-1'
  courseForm.credit = row?.credit ?? 3
  courseForm.coverColor = row?.coverColor || '#38DDF8'
  courseForm.classroom = row?.classroom || ''
  courseForm.longitude = row?.longitude ?? 118.1234567
  courseForm.latitude = row?.latitude ?? 32.1234567
  courseForm.defaultRadius = row?.defaultRadius || 150
  courseForm.classIds = row?.classIds ? [...row.classIds] : []
  courseDialog.value = true
}

const saveCourse = async () => {
  if (!courseForm.courseNo.trim() || !courseForm.name.trim() || !courseForm.teacherId) {
    ElMessage.warning('课程编号、名称与授课教师为必填项')
    return
  }
  if (courseForm.id) await adminApi.updateCourse({ ...courseForm })
  else await adminApi.createCourse({ ...courseForm })
  ElMessage.success('课程信息已保存')
  courseDialog.value = false
  await Promise.all([loadCourses(), loadReferences()])
}

const removeCourse = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确认删除课程「${row.name}」？已有签到任务的课程无法删除。`, '删除课程', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  await adminApi.deleteCourse(row.id)
  ElMessage.success('课程已删除')
  await loadCourses()
}

/* ============ 账号操作 ============ */
const openUser = (row?: any) => {
  userForm.id = row?.id
  userForm.username = row?.username || ''
  userForm.password = ''
  userForm.role = row?.role || 'STUDENT'
  userForm.realName = row?.realName || ''
  userForm.phone = row?.phone || ''
  userForm.enabled = row?.enabled === 1 || row?.enabled === true
  userDialog.value = true
}

const saveUser = async () => {
  if (!userForm.username.trim()) {
    ElMessage.warning('请输入登录名')
    return
  }
  const payload = {
    ...userForm,
    enabled: userForm.enabled
  }
  if (userForm.id) await adminApi.updateUser(payload)
  else await adminApi.createUser(payload)
  ElMessage.success('账号已保存')
  userDialog.value = false
  loadUsers()
}

const resetPassword = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确认将「${row.username}」的密码重置为 Pass@123？`, '重置密码', {
      confirmButtonText: '确认重置',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  await adminApi.resetPassword(row.id)
  ElMessage.success('密码已重置为 Pass@123')
}

onMounted(async () => {
  await loadReferences()
  if (typeof route.query.keyword === 'string') {
    studentFilters.keyword = route.query.keyword
  }
  loadTab()
})
</script>

<template>
  <div class="page">
    <PageHeader title="基础数据" subtitle="维护用户、学生、教师、班级、课程与授课关系" eyebrow="MASTER DATA">
      <template #actions>
        <el-button @click="adminApi.exportStudents()">导出学生</el-button>
        <el-upload :show-file-list="false" :http-request="importStudents" accept=".xlsx">
          <el-button :loading="importing">导入学生</el-button>
        </el-upload>
      </template>
    </PageHeader>

    <section class="panel">
      <el-tabs v-model="tab">
        <!-- 学生 -->
        <el-tab-pane label="学生" name="students">
          <div class="toolbar">
            <el-input v-model="studentFilters.keyword" placeholder="搜索学号或姓名" clearable style="width: 200px" @keyup.enter="loadStudents">
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
            <el-select v-model="studentFilters.classId" placeholder="全部班级" clearable style="width: 180px" @change="loadStudents">
              <el-option v-for="item in classOptions" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
            <el-button @click="loadStudents">查询</el-button>
            <div class="grow" />
            <el-button type="primary" @click="openStudent()"><el-icon><Plus /></el-icon> 新增学生</el-button>
          </div>
          <el-table :data="students" v-loading="loading" size="small" max-height="520">
            <el-table-column label="学号" width="130"><template #default="{ row }"><span class="mono">{{ row.studentNo }}</span></template></el-table-column>
            <el-table-column label="姓名" width="100" prop="name" />
            <el-table-column label="班级" min-width="160" prop="className" />
            <el-table-column label="手机号" width="130"><template #default="{ row }"><span class="mono muted">{{ row.phone || '—' }}</span></template></el-table-column>
            <el-table-column label="账号状态" width="100">
              <template #default="{ row }"><span :class="row.enabled ? 'green' : 'coral'" style="font-size: 12px">{{ row.enabled ? '正常' : '已停用' }}</span></template>
            </el-table-column>
            <el-table-column label="创建时间" width="160">
              <template #default="{ row }"><span class="mono muted">{{ formatDateTime(row.createTime) }}</span></template>
            </el-table-column>
            <el-table-column label="操作" width="130" fixed="right">
              <template #default="{ row }">
                <el-button text type="primary" size="small" @click="openStudent(row)">编辑</el-button>
                <el-button text type="danger" size="small" @click="removeStudent(row)">停用</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pager">
            <el-pagination v-model:current-page="studentPage" :page-size="studentSize" :total="studentTotal" layout="prev, pager, next" background @current-change="loadStudents" />
          </div>
        </el-tab-pane>

        <!-- 教师 -->
        <el-tab-pane label="教师" name="teachers">
          <div class="toolbar">
            <el-input v-model="teacherFilters.keyword" placeholder="搜索工号或姓名" clearable style="width: 200px" @keyup.enter="loadTeachers">
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
            <el-button @click="loadTeachers">查询</el-button>
            <div class="grow" />
            <el-button type="primary" @click="openTeacher()"><el-icon><Plus /></el-icon> 新增教师</el-button>
          </div>
          <el-table :data="teachers" v-loading="loading" size="small" max-height="520">
            <el-table-column label="工号" width="130"><template #default="{ row }"><span class="mono">{{ row.teacherNo }}</span></template></el-table-column>
            <el-table-column label="姓名" width="110" prop="name" />
            <el-table-column label="院系" min-width="170" prop="department" />
            <el-table-column label="授课数" width="90" prop="courseCount" />
            <el-table-column label="操作" width="130" fixed="right">
              <template #default="{ row }">
                <el-button text type="primary" size="small" @click="openTeacher(row)">编辑</el-button>
                <el-button text type="danger" size="small" @click="removeTeacher(row)">停用</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pager">
            <el-pagination v-model:current-page="teacherPage" :page-size="10" :total="teacherTotal" layout="prev, pager, next" background @current-change="loadTeachers" />
          </div>
        </el-tab-pane>

        <!-- 班级 -->
        <el-tab-pane label="班级" name="classes">
          <div class="toolbar">
            <span class="muted" style="font-size: 12px">共 {{ classes.length }} 个班级</span>
            <div class="grow" />
            <el-button type="primary" @click="openClass()"><el-icon><Plus /></el-icon> 新增班级</el-button>
          </div>
          <el-table :data="classes" v-loading="loading" size="small" max-height="520">
            <el-table-column label="班级名称" min-width="180" prop="name" />
            <el-table-column label="年级" width="100" prop="grade" />
            <el-table-column label="专业" min-width="170" prop="major" />
            <el-table-column label="学生数" width="90" prop="studentCount" />
            <el-table-column label="操作" width="130" fixed="right">
              <template #default="{ row }">
                <el-button text type="primary" size="small" @click="openClass(row)">编辑</el-button>
                <el-button text type="danger" size="small" @click="removeClass(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 课程 -->
        <el-tab-pane label="课程" name="courses">
          <div class="toolbar">
            <span class="muted" style="font-size: 12px">共 {{ courses.length }} 门课程</span>
            <div class="grow" />
            <el-button type="primary" @click="openCourse()"><el-icon><Plus /></el-icon> 新增课程</el-button>
          </div>
          <el-table :data="courses" v-loading="loading" size="small" max-height="520">
            <el-table-column label="编号" width="110"><template #default="{ row }"><span class="mono">{{ row.courseNo }}</span></template></el-table-column>
            <el-table-column label="课程" min-width="150">
              <template #default="{ row }">
                <div class="row" style="gap: 8px">
                  <span class="color-dot" :style="{ background: row.coverColor || 'var(--cyan)' }" />
                  <span>{{ row.name }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="教师" width="110" prop="teacherName" />
            <el-table-column label="班级" min-width="180">
              <template #default="{ row }"><span class="muted">{{ row.classNames?.join('、') || '未关联' }}</span></template>
            </el-table-column>
            <el-table-column label="学生数" width="90" prop="studentCount" />
            <el-table-column label="操作" width="130" fixed="right">
              <template #default="{ row }">
                <el-button text type="primary" size="small" @click="openCourse(row)">编辑</el-button>
                <el-button text type="danger" size="small" @click="removeCourse(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 账号 -->
        <el-tab-pane label="账号" name="users">
          <div class="toolbar">
            <el-input v-model="userFilters.keyword" placeholder="搜索登录名或姓名" clearable style="width: 200px" @keyup.enter="loadUsers">
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
            <el-select v-model="userFilters.role" style="width: 130px" @change="loadUsers">
              <el-option label="全部角色" value="ALL" />
              <el-option label="学生" value="STUDENT" />
              <el-option label="教师" value="TEACHER" />
              <el-option label="管理员" value="ADMIN" />
            </el-select>
            <el-button @click="loadUsers">查询</el-button>
            <div class="grow" />
            <el-button type="primary" @click="openUser()"><el-icon><Plus /></el-icon> 新增账号</el-button>
          </div>
          <el-table :data="users" v-loading="loading" size="small" max-height="520">
            <el-table-column label="登录名" width="130"><template #default="{ row }"><span class="mono">{{ row.username }}</span></template></el-table-column>
            <el-table-column label="姓名" width="110" prop="realName" />
            <el-table-column label="角色" width="90">
              <template #default="{ row }"><span class="chip">{{ roleLabel(row.role) }}</span></template>
            </el-table-column>
            <el-table-column label="手机号" width="130"><template #default="{ row }"><span class="mono muted">{{ row.phone || '—' }}</span></template></el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }"><span :class="row.enabled ? 'green' : 'coral'" style="font-size: 12px">{{ row.enabled ? '启用' : '禁用' }}</span></template>
            </el-table-column>
            <el-table-column label="最近登录" width="160">
              <template #default="{ row }"><span class="mono muted">{{ formatDateTime(row.lastLoginTime) }}</span></template>
            </el-table-column>
            <el-table-column label="操作" width="190" fixed="right">
              <template #default="{ row }">
                <el-button text type="primary" size="small" @click="openUser(row)">编辑</el-button>
                <el-button text size="small" @click="resetPassword(row)">重置密码</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pager">
            <el-pagination v-model:current-page="userPage" :page-size="10" :total="userTotal" layout="prev, pager, next" background @current-change="loadUsers" />
          </div>
        </el-tab-pane>
      </el-tabs>
    </section>

    <!-- 学生弹窗 -->
    <el-dialog v-model="studentDialog" :title="studentForm.id ? '编辑学生' : '新增学生'" width="420px">
      <el-form label-position="top">
        <el-form-item label="学号" required><el-input v-model="studentForm.studentNo" maxlength="32" /></el-form-item>
        <el-form-item label="姓名" required><el-input v-model="studentForm.name" maxlength="64" /></el-form-item>
        <el-form-item label="班级" required>
          <el-select v-model="studentForm.classId" style="width: 100%">
            <el-option v-for="item in classOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="手机号"><el-input v-model="studentForm.phone" maxlength="20" /></el-form-item>
        <div class="form-tip">新增学生会自动创建登录账号，默认密码为 Student@123</div>
      </el-form>
      <template #footer>
        <el-button @click="studentDialog = false">取消</el-button>
        <el-button type="primary" @click="saveStudent">保存</el-button>
      </template>
    </el-dialog>

    <!-- 教师弹窗 -->
    <el-dialog v-model="teacherDialog" :title="teacherForm.id ? '编辑教师' : '新增教师'" width="420px">
      <el-form label-position="top">
        <el-form-item label="工号" required><el-input v-model="teacherForm.teacherNo" maxlength="32" /></el-form-item>
        <el-form-item label="姓名" required><el-input v-model="teacherForm.name" maxlength="64" /></el-form-item>
        <el-form-item label="院系"><el-input v-model="teacherForm.department" maxlength="64" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="teacherForm.phone" maxlength="20" /></el-form-item>
        <div class="form-tip">新增教师会自动创建登录账号，默认密码为 Teacher@123</div>
      </el-form>
      <template #footer>
        <el-button @click="teacherDialog = false">取消</el-button>
        <el-button type="primary" @click="saveTeacher">保存</el-button>
      </template>
    </el-dialog>

    <!-- 班级弹窗 -->
    <el-dialog v-model="classDialog" :title="classForm.id ? '编辑班级' : '新增班级'" width="420px">
      <el-form label-position="top">
        <el-form-item label="班级名称" required><el-input v-model="classForm.name" maxlength="64" /></el-form-item>
        <div class="grid grid-2">
          <el-form-item label="年级"><el-input v-model="classForm.grade" maxlength="32" /></el-form-item>
          <el-form-item label="专业"><el-input v-model="classForm.major" maxlength="64" /></el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="classDialog = false">取消</el-button>
        <el-button type="primary" @click="saveClass">保存</el-button>
      </template>
    </el-dialog>

    <!-- 课程弹窗 -->
    <el-dialog v-model="courseDialog" :title="courseForm.id ? '编辑课程' : '新增课程'" width="560px">
      <el-form label-position="top">
        <div class="grid grid-2">
          <el-form-item label="课程编号" required><el-input v-model="courseForm.courseNo" maxlength="32" /></el-form-item>
          <el-form-item label="课程名称" required><el-input v-model="courseForm.name" maxlength="128" /></el-form-item>
        </div>
        <div class="grid grid-2">
          <el-form-item label="授课教师" required>
            <el-select v-model="courseForm.teacherId" style="width: 100%">
              <el-option v-for="item in teacherOptions" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="学期"><el-input v-model="courseForm.semester" maxlength="32" /></el-form-item>
        </div>
        <div class="grid grid-2">
          <el-form-item label="学分"><el-input-number v-model="courseForm.credit" :min="0.5" :max="10" :step="0.5" style="width: 100%" /></el-form-item>
          <el-form-item label="课程标识色"><el-color-picker v-model="courseForm.coverColor" /></el-form-item>
        </div>
        <el-form-item label="授课班级">
          <el-select v-model="courseForm.classIds" multiple style="width: 100%" placeholder="选择授课班级">
            <el-option v-for="item in classOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <div class="grid grid-2">
          <el-form-item label="教室"><el-input v-model="courseForm.classroom" maxlength="128" /></el-form-item>
          <el-form-item label="默认定位半径（米）"><el-input-number v-model="courseForm.defaultRadius" :min="20" :max="5000" style="width: 100%" /></el-form-item>
        </div>
        <div class="grid grid-2">
          <el-form-item label="教室经度">
            <el-input-number v-model="courseForm.longitude" :precision="7" :controls="false" style="width: 100%" />
          </el-form-item>
          <el-form-item label="教室纬度">
            <el-input-number v-model="courseForm.latitude" :precision="7" :controls="false" style="width: 100%" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="courseDialog = false">取消</el-button>
        <el-button type="primary" @click="saveCourse">保存</el-button>
      </template>
    </el-dialog>

    <!-- 账号弹窗 -->
    <el-dialog v-model="userDialog" :title="userForm.id ? '编辑账号' : '新增账号'" width="420px">
      <el-form label-position="top">
        <el-form-item label="登录名" required><el-input v-model="userForm.username" maxlength="64" /></el-form-item>
        <el-form-item :label="userForm.id ? '重置密码（留空不修改）' : '初始密码（留空默认 Pass@123）'">
          <el-input v-model="userForm.password" type="password" show-password maxlength="32" />
        </el-form-item>
        <el-form-item label="角色">
          <el-radio-group v-model="userForm.role">
            <el-radio-button value="STUDENT">学生</el-radio-button>
            <el-radio-button value="TEACHER">教师</el-radio-button>
            <el-radio-button value="ADMIN">管理员</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="姓名"><el-input v-model="userForm.realName" maxlength="64" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="userForm.phone" maxlength="20" /></el-form-item>
        <el-form-item label="启用状态"><el-switch v-model="userForm.enabled" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="userDialog = false">取消</el-button>
        <el-button type="primary" @click="saveUser">保存</el-button>
      </template>
    </el-dialog>

    <!-- 导入结果 -->
    <el-dialog v-model="importVisible" title="导入结果" width="520px">
      <div v-if="importReport" class="import-report">
        <div class="row" style="gap: 16px; margin-bottom: 12px">
          <span class="chip green">成功 {{ importReport.success }}</span>
          <span class="chip coral">失败 {{ importReport.failed }}</span>
        </div>
        <el-table v-if="importReport.errors?.length" :data="importReport.errors" size="small" max-height="280">
          <el-table-column label="行号" width="80" prop="row" />
          <el-table-column label="错误原因" prop="message" />
        </el-table>
        <div v-else class="muted" style="font-size: 12px">全部导入成功</div>
      </div>
      <template #footer><el-button type="primary" @click="importVisible = false">知道了</el-button></template>
    </el-dialog>
  </div>
</template>

<style scoped>
.toolbar { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; margin-bottom: 14px; }
.pager { display: flex; justify-content: flex-end; margin-top: 12px; }
.color-dot { width: 8px; height: 8px; border-radius: 2px; flex-shrink: 0; }
.form-tip { font-size: 11px; color: var(--muted); margin-top: 4px; }
.import-report { font-size: 12px; }
@media (max-width: 760px) { .pager { justify-content: center; } }
</style>
