package com.hpc.jobagent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 40, message = "用户名长度需要在 3 到 40 之间")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 80, message = "密码长度需要在 6 到 80 之间")
        String password,

        @Size(max = 80, message = "昵称不能超过 80 个字符")
        String displayName
) {
}
