package org.example.controller;

import org.example.response.Result;
import org.example.service.LikeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/like")
public class LikeController {

    @Autowired
    private LikeService likeService;

    /**
     * 为所有用户随机生成点赞记录
     */
    @PostMapping("/generate-random")
    public Result<Integer> generateRandomLikes() {
        try {
            int count = likeService.generateRandomLikes();
            Result<Integer> result = Result.success(count);
            result.setMsg("成功生成" + count + "条点赞记录");
            return result;
        } catch (Exception e) {
            return Result.error(500, "生成点赞记录失败: " + e.getMessage());
        }
    }
}
