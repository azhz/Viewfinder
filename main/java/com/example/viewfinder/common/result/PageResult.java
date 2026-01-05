package com.example.viewfinder.common.result;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * 分页结果封装类
 * 用于封装分页查询的结果数据，包含记录列表、分页信息等
 *
 * @param <T> 数据类型
 * @author Viewfinder Team
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    /**
     * 数据列表
     */
    private List<T> records;

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 当前页码
     */
    private Long current;

    /**
     * 每页大小
     */
    private Long size;

    /**
     * 总页数
     */
    private Long pages;

    /**
     * 构造方法 - 基于MyBatis-Plus的分页结果
     * 
     * @param records 数据列表
     * @param total 总记录数
     * @param current 当前页码
     * @param size 每页大小
     */
    public PageResult(List<T> records, Long total, Long current, Long size) {
        this.records = records;
        this.total = total;
        this.current = current;
        this.size = size;
        // 计算总页数
        this.pages = (total + size - 1) / size;
    }

    /**
     * 创建空分页结果
     * 
     * @param <T> 数据类型
     * @return 空的分页结果
     */
    public static <T> PageResult<T> empty() {
        return new PageResult<>(null, 0L, 1L, 10L);
    }

    /**
     * 创建空分页结果（指定页码和大小）
     * 
     * @param current 当前页码
     * @param size 每页大小
     * @param <T> 数据类型
     * @return 空的分页结果
     */
    public static <T> PageResult<T> empty(Long current, Long size) {
        return new PageResult<>(null, 0L, current, size);
    }
}
