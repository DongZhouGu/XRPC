package com.dzgu.xprc.entity;

import lombok.*;

import java.io.Serializable;

/**
 * @description:
 * @Author： dzgu
 * @Date： 2022/4/27 0:19
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@ToString
public class Hello implements Serializable {
    private String message;
    private String description;
}
