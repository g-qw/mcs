package org.cloud.fs.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.cloud.fs.model.MinioDirectory;
import org.cloud.fs.model.MinioFile;

import java.io.Serializable;
import java.util.List;

/**
 * 用于存储当前目录的所有数据
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class MinioDirContent implements Serializable {
    /**
     * 目录ID
     */
    private String directoryId;

    /**
     * 目录的绝对路径
     */
    private String path;

    /**
     * 子目录对象列表
     */
    private List<MinioDirectory> directories;

    /**
     * 子文件对象列表
     */
    private List<MinioFile> files;
}
