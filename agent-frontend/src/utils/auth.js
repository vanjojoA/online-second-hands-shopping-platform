import { resetUserState, setUserState, userState } from '@/stores/user'

const TOKEN_KEY = 'sp_token'
const USER_KEY = 'sp_user'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || ''
}

export function setAuth({ token, userId, nickname, avatar, role }) {
  if (token) localStorage.setItem(TOKEN_KEY, token)
  localStorage.setItem(USER_KEY, JSON.stringify({ userId, nickname, avatar, role }))
  setUserState({ userId, nickname: nickname || '', avatar: avatar || '', role })
}

export function getUser() {
  return {
    userId: userState.userId,
    nickname: userState.nickname,
    avatar: userState.avatar,
    role: userState.role
  }
}

export function updateUser(partial) {
  const next = { ...getUser(), ...partial }
  localStorage.setItem(USER_KEY, JSON.stringify(next))
  setUserState(next)
}

export function clearAuth() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  resetUserState()
}

export function isLoggedIn() {
  return Boolean(getToken())
}
