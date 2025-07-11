package org.cloud.fs.exception;

public class DirectoryNotEmptyException extends RuntimeException{

    /**
     * 构造方法
     * @param directory 目录名称或目录ID
     */
    public DirectoryNotEmptyException(String directory) {
        super("目录 " + directory + " 不为空，无法删除");
    }
}
