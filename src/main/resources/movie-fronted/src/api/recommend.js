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

// 获取所有评论创建者
export function getAllCommentCreators() {
    return request({
        url: '/comment/creators',
        method: 'get'
    })
}

// 获取单部电影的评分分布
export function getMovieRatings(movieName) {
    return request({
        url: `/recommendation/movie-ratings/${encodeURIComponent(movieName)}`,
        method: 'get'
    })
}

// 按类型统计电影的平均评分和电影数量
export function getMovieRatingsByType() {
    return request({
        url: '/recommendation/movie-ratings-by-type',
        method: 'get'
    })
}

// 按地区统计电影的平均评分和电影数量
export function getMovieRatingsByRegion() {
    return request({
        url: '/recommendation/movie-ratings-by-region',
        method: 'get'
    })
}

// 获取指定电影的评论记录（根据电影名称）
export function getMovieCommentsByMovieName(movieName) {
    return request({
        url: `/recommendation/movie-comments?movieName=${encodeURIComponent(movieName)}`,
        method: 'get'
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