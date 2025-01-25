package com.by.cloud;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
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

    @Test
    public void testHttpRequest() {
        String url = "https://vigen-invi.oss-cn-shanghai.aliyuncs.com/service_dashscope/ImageOutPainting/2025-01-25/public/1a53b1bc-ca6e-47b1-9561-a8d872b99ab9/result-66de2e79-f6b9-457a-ae9d-e140546a8112.jpg?OSSAccessKeyId=LTAI5t7aiMEUzu1F2xPMCdFj&Expires=1737801426&Signature=Ed4Bp8KvjqZt%2Bb2M0AXYQErPxjs%3D";
        HttpRequest httpRequest = HttpRequest.get(url);
        HttpResponse httpResponse = httpRequest.execute();
        System.out.println(httpResponse.getStatus());
    }
}
