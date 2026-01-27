import axios from 'axios'

// 创建axios实例
const service = axios.create({
    baseURL: process.env.VUE_APP_BASE_API || '/api', // 后端接口前缀
    timeout: 10000 // 请求超时
})

// 请求拦截器
service.interceptors.request.use(
    config => {
        // 可添加token等请求头
        config.headers['Content-Type'] = 'application/json;charset=utf-8'
        return config
    },
    error => {
        console.error('请求错误:', error)
        return Promise.reject(error)
    }
)

// 响应拦截器
service.interceptors.response.use(
    response => {
        const res = response.data
        // 统一处理响应码
        if (res.code !== 200) {
            console.error('接口返回异常:', res.msg)
            return Promise.reject(new Error(res.msg || '请求失败'))
        } else {
            return res
        }
    },
    error => {
        console.error('响应错误:', error)
        return Promise.reject(error)
    }
)

export default service