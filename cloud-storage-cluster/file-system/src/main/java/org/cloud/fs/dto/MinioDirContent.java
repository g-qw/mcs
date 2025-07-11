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
    private String path; // 绝对路径
    private String directoryId; // 目录ID
    private List<MinioDirectory> directories; // 目录列表
    private List<MinioFile> files; // 文件列表
}
