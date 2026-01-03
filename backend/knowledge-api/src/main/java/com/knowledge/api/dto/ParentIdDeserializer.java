package com.knowledge.api.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;

/**
 * 自定义 parentId 反序列化器
 * 处理字符串格式的部门ID（如 "dept-1"）和负数ID，统一转换为 null
 */
public class ParentIdDeserializer extends JsonDeserializer<Long> {
    
    @Override
    public Long deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonToken token = p.getCurrentToken();
        
        if (token == JsonToken.VALUE_NULL) {
            return null;
        }
        
        // 如果是字符串类型
        if (token == JsonToken.VALUE_STRING) {
            String strValue = p.getText();
            // 如果是 "dept-X" 格式，说明是部门根节点，返回 null
            if (strValue != null && strValue.startsWith("dept-")) {
                return null;
            }
            // 尝试转换为数字
            try {
                long numValue = Long.parseLong(strValue);
                // 如果是负数，说明是部门根节点，返回 null
                return numValue < 0 ? null : numValue;
            } catch (NumberFormatException e) {
                // 无法转换为数字，返回 null
                return null;
            }
        }
        
        // 如果是数字类型
        if (token.isNumeric()) {
            long numValue = p.getLongValue();
            // 如果是负数，说明是部门根节点，返回 null
            return numValue < 0 ? null : numValue;
        }
        
        // 其他类型，返回 null
        return null;
    }
}

