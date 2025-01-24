package com.by.cloud;

import com.by.cloud.common.PageResult;
import com.by.cloud.model.dto.picture.PicturePageDto;
import com.by.cloud.model.vo.picture.PictureVo;
import com.by.cloud.service.PictureService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author lzh
 */
@SpringBootTest
public class PictureServiceTest {

    @Resource
    private PictureService pictureService;

    @Test
    public void testQueryPictureVoByPage() {
        PicturePageDto pageDto = new PicturePageDto();
        pageDto.setCurrent(1);
        pageDto.setPageSize(10);
        PageResult<PictureVo> pageResult = pictureService.queryPictureVoByPage(pageDto);
        List<PictureVo> records = pageResult.getRecords();
    }
}
