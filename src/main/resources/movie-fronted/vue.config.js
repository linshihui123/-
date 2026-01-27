module.exports = {
    transpileDependencies: [
        // 可以在这里指定需要转译的依赖，或者保持为空数组
    ],
    // 代理配置（解决跨域）
    devServer: {
        proxy: {
            '/api': {
                target: 'http://localhost:8081', // 后端服务地址
                changeOrigin: true,
                pathRewrite: {
                    '^/api': '' // 去除/api前缀
                }
            }
        }
    }
}