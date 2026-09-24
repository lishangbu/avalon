/**
 * 存储适配层。封装实例 JSON
 * 文本与不可变用例模型之间的转换，不计算奖励资格或授予资源。存储解码错误属于内部故障，不能以空集合静默掩盖，也不能重新执行已经完成的命令。实体不离开本模块，查询结果只包含普通值。
 */
package io.github.lishangbu.avalon.creature.persistence.mapper;
