package com.by.cloud.model.dto.picture;

import com.by.cloud.api.aliyunai.model.CreateOutPaintingTaskRequest;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author lzh
 */
@Data
public class PictureCreateOutPaintingTaskDto implements Serializable {

    @ApiModelProperty("图片ID")
    private Long pictureId;

    @ApiModelProperty("扩图参数")
    private CreateOutPaintingTaskRequest.Parameters parameters;
}
