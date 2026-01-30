import request from '@/utils/request'

// 解析用户意图（调用Dify集成接口）
export function parseUserIntent(data) {
    return request({
        url: '/dify/parseIntent',
        method: 'post',
        data
    })
}

// 获取电影推荐结果 - GET方式
export function getRecommendMovies(params) {
    return request({
        url: '/movie/recommend',
        method: 'get',
        params
    })
}

// 获取电影推荐结果 - POST方式（支持请求体）
export function getRecommendMoviesPost(data) {
    return request({
        url: '/movie/recommend',
        method: 'post',
        data
    })
}

// 获取电影详情
export function getMovieDetail(movieId) {
    return request({
        url: `/movie/by-movie-id/${movieId}`,
        method: 'get'
    })
}

// 获取协同过滤推荐
export function getCollaborativeFilteringRecommend(userId) {
    return request({
        url: `/recommendation/collaborative-filtering/${userId}`,
        method: 'get'
    })
}

// 获取默认协同过滤推荐
export function getDefaultCollaborativeFilteringRecommend() {
    return request({
        url: '/recommendation/collaborative-filtering',
        method: 'get'
    })
}

// 获取所有评论创建者
export function getAllCommentCreators() {
    return request({
        url: '/comment/creators',
        method: 'get'
    })
}