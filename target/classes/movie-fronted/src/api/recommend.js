import request from '@/utils/request'

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

// 融合推荐（协同过滤 + 内容推荐 + 知识图谱，按权重融合）
export function getFusedRecommend(username, limit = 20) {
    return request({
        url: '/recommendation/fused',
        method: 'get',
        params: { username, limit }
    })
}

// 获取所有评论创建者
export function getAllCommentCreators() {
    return request({
        url: '/comment/creators',
        method: 'get'
    })
}

// 获取指定电影的评论记录及 AI 总结（根据电影名称）。首次请求可能调用大模型，超时设为 60 秒；有缓存时响应较快
// config 可选，如 { cancelToken } 用于切换电影时取消上一次请求
export function getMovieCommentsByMovieName(movieName, config = {}) {
    return request({
        url: `/recommendation/movie-comments?movieName=${encodeURIComponent(movieName)}`,
        method: 'get',
        timeout: 60000,
        ...config
    })
}

// 获取多维度电影榜单
export function getMultiDimensionalRanking(params) {
    return request({
        url: '/recommendation/multi-dimensional-ranking',
        method: 'get',
        params: params
    })
}

// 基于用户点赞的电影推荐：喜欢的电影 → 同导演 → 其他电影
export function getRecommendMoviesByDirector(username, limit) {
    return request({
        url: `/api/kg/recommend/director`,
        method: 'get',
        params: {
            username: username,
            limit: limit
        }
    })
}

// 基于用户点赞的电影推荐：喜欢的电影 → 同类型 → 同地区 → 其他电影
export function getRecommendMoviesByTypeAndRegion(username, limit) {
    return request({
        url: `/api/kg/recommend/type-region`,
        method: 'get',
        params: {
            username: username,
            limit: limit
        }
    })
}

// 基于用户点赞的电影推荐：喜欢的电影 → 同演员 → 同导演 → 其他电影
export function getRecommendMoviesByActorAndDirector(username, limit) {
    return request({
        url: `/api/kg/recommend/actor-director`,
        method: 'get',
        params: {
            username: username,
            limit: limit
        }
    })
}

// 获取有点赞记录的用户名列表（用于知识图谱推荐）
export function getUsersWithLikes() {
    return request({
        url: '/like/users-with-likes',
        method: 'get'
    })
}

// 获取指定用户的点赞电影列表（基于用户点赞记录）
export function getLikedMovies(username) {
    return request({
        url: '/like/liked-movies',
        method: 'get',
        params: { username }
    })
}

// 为指定用户对电影点赞
export function addLike(username, movieId) {
    return request({
        url: '/like/add',
        method: 'post',
        params: { username, movieId }
    })
}

// 取消指定用户对电影的点赞
export function removeLike(username, movieId) {
    return request({
        url: '/like/remove',
        method: 'post',
        params: { username, movieId }
    })
}

// 查询指定用户是否已点赞某部电影
export function isLiked(username, movieId) {
    return request({
        url: '/like/is-liked',
        method: 'get',
        params: { username, movieId }
    })
}

// 基于评分预测的推荐
export function getRatingPredictionRecommend(username, limit = 20) {
    return request({
        url: '/recommendation/rating-prediction',
        method: 'get',
        params: { username, limit }
    })
}

// 基于评论 + 大模型分析的推荐
export function getCommentBasedRecommend(payload) {
    return request({
        url: '/recommendation/comment-based',
        method: 'post',
        data: payload,
        // 大模型分析可能较慢，单独放宽超时时间
        timeout: 60000
    })
}

// 火山方舟单次对话（大模型响应较慢，单独设置 60 秒超时）
export function arkChat(message) {
    return request({
        url: '/ark/chat',
        method: 'post',
        timeout: 60000,
        data: {
            message: message
        }
    })
}

// 火山方舟多轮对话（大模型响应较慢，单独设置 60 秒超时）
export function arkMultiChat(requestData) {
    return request({
        url: '/ark/chat/multi',
        method: 'post',
        timeout: 60000,
        data: requestData // 包含messages和userId
    })
}