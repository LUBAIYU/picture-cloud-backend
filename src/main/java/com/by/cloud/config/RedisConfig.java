package com.by.cloud.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * @author lzh
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        // 创建 RedisTemplate 实例
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        // 设置 Redis 连接工厂
        template.setConnectionFactory(redisConnectionFactory);

        // 使用 Jackson2JsonRedisSerializer 来序列化和反序列化 Redis 的 value 值
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper om = new ObjectMapper();
        // 指定要序列化的域，field、get 和 set 以及修饰符范围，ANY 表示任何修饰符
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        // 启用默认的类型信息，在序列化时会包含类的类型信息
        om.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);
        jackson2JsonRedisSerializer.setObjectMapper(om);

        // 使用 StringRedisSerializer 来序列化和反序列化 Redis 的 key 值
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

        // 设置 key 的序列化方式
        template.setKeySerializer(stringRedisSerializer);
        // 设置 hash key 的序列化方式
        template.setHashKeySerializer(stringRedisSerializer);
        // 设置 value 的序列化方式
        template.setValueSerializer(jackson2JsonRedisSerializer);
        // 设置 hash value 的序列化方式
        template.setHashValueSerializer(jackson2JsonRedisSerializer);

        template.afterPropertiesSet();
        return template;
    }
}