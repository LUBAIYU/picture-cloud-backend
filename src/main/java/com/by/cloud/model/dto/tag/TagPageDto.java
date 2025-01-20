package com.by.cloud.model.dto.tag;

import com.by.cloud.common.PageRequest;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @author lzh
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TagPageDto extends PageRequest implements Serializable {

    @ApiModelProperty("标签名称")
    private String name;
}
