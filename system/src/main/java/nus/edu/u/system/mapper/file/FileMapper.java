package nus.edu.u.system.mapper.file;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import nus.edu.u.system.domain.dataobject.file.FileDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @author Lu Shuwen
 * @date 2025-08-31
 */
@Mapper
public interface FileMapper extends BaseMapper<FileDO> {
    int insertBatch(@Param("list") List<FileDO> list);
}
