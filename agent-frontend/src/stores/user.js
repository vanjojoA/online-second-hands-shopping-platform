import { reactive } from 'vue'

const USER_KEY = 'sp_user'

function readStored() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY) || '{}') || {}
  } catch {
    return {}
  }
}

// 全局用户状态：登录、改头像、改昵称后，所有用到的地方（顶栏、个人中心）会一起更新
export const userState = reactive({
  userId: readStored().userId,
  nickname: readStored().nickname || '',
  avatar: readStored().avatar || '',
  role: readStored().role
})

export function setUserState(partial) {
  Object.assign(userState, partial)
}

export function resetUserState() {
  userState.userId = undefined
  userState.nickname = ''
  userState.avatar = ''
  userState.role = undefined
}
