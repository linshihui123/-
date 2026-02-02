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