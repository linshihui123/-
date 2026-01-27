import request from '@/utils/request'

// 注册用户
export function registerUser(userData) {
  return request({
    url: '/users',  // 相对于baseURL的路径
    method: 'post',
    data: userData
  })
}

// 登录用户
export function loginUser(loginData) {
  return request({
    url: '/users/login',  // 相对于baseURL的路径
    method: 'post',
    params: loginData  // 用户名和密码作为查询参数传递
  })
}

// 检查用户名是否存在
export function checkUsername(username) {
  return request({
    url: `/users/check-username/${username}`,  // 相对于baseURL的路径
    method: 'get'
  })
}