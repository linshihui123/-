package org.example.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.dify.DifyIntegrationService;
import org.example.model.RecommendIntent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/dify")
@CrossOrigin(origins = "*")
public class DifyController {

    @Autowired
    private DifyIntegrationService difyService;

    /**
     * 解析用户意图
     */
    @PostMapping("/parseIntent")
    public RecommendIntent parseIntent(
            @RequestParam String userInput,
            @RequestParam String userId) {
        return difyService.parseUserIntent(userInput, userId);
    }
}
