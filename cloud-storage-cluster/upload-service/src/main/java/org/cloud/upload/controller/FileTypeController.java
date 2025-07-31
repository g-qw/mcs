package org.cloud.upload.controller;

import org.apache.tika.config.TikaConfig;
import org.apache.tika.detect.Detector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;
import org.cloud.upload.dto.FileTypeResponse;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/parse")
public class FileTypeController {
    private final Detector detector = TikaConfig.getDefaultConfig().getDetector();

    /*
     * 使用 ConcurrentHashMap 来缓存文件类型的扩展名，避免重复计算
     * 一个 MIME 类型可能对应多个文件扩展名, 键：MIME 类型，值：文件扩展名列表
     */
    private final Map<String, List<String>> mediaTypeExtensionsCache = new ConcurrentHashMap<>();
    private static final MimeTypes mimeTypes = MimeTypes.getDefaultMimeTypes();  // 获取默认的 MimeTypes 对象

    /**
     * 识别文件类型
     * @apiNote 根据发送到接口的文件头部片段，利用tika库识别文件类型
     * @param filePartMono 文件头部片段
     * @return 文件的MIME类型
     */
    @PostMapping("/fileType")
    Mono<FileTypeResponse> parseFileType(@RequestPart("file") Mono<FilePart> filePartMono) {
        return filePartMono.flatMap(filePart -> {// 对文件部分进行处理
            return DataBufferUtils.join(filePart.content()) // 将多个 DataBuffer 合并为一个
                    .flatMap( // flatMap 将一个数据流中的每个元素映射成一个新的数据流
                        buffer -> {
                            try {
                                TikaInputStream tis = TikaInputStream.get(buffer.asInputStream());  // 将 DataBuffer 转换为 TikaInputStream
                                MediaType mediaType = detector.detect(tis, new Metadata()); // 检测文件的 MIME 类型

                                // 从缓存中获取文件类型的拓展名，如果不存在则计算并缓存
                                List<String> mediaTypeExtensions = mediaTypeExtensionsCache.computeIfAbsent(
                                        mediaType.toString(),
                                        key -> {
                                            try {
                                                return Collections.singletonList(mimeTypes.forName(key).getExtension()); // 单个文件拓展名时会包装成 List
                                            } catch (MimeTypeException e) {
                                                throw new RuntimeException(e);
                                            }
                                        }
                                );

                                // 获取文件的 MIME 类型和扩展名
                                String fileType = mediaTypeExtensions.isEmpty() ? "unknown" : mediaTypeExtensions.getFirst().replace(".", "");
                                String contentType = mediaType.toString();

                                return Mono.just(new FileTypeResponse(fileType, contentType));
                            }catch (IOException e) {
                                return Mono.error(e);
                            } finally {
                                DataBufferUtils.release(buffer);
                            }

                        }
                    );
            }
        );
    }
}
