package org.example.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO, value = "userid") // 使用数据库实际的主键列名
    private Integer userid;  // 使用实际数据库中的列名
    
    @TableField("username") // 数据库中实际的字段名是username
    private String username;
    
    @TableField("password")
    private String password;
    
    /**
     * 创建时间（自动填充）
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间（自动填充/更新）
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    // 为兼容前端访问，提供id属性的getter方法
    public Integer getId() {
        return this.userid;
    }
    
    public void setId(Integer id) {
        this.userid = id;
    }
    
    // 为兼容前端访问，提供userId属性的getter方法
    public Integer getUserId() {
        return this.userid;
    }
    
    public void setUserId(Integer userId) {
        this.userid = userId;
    }
}