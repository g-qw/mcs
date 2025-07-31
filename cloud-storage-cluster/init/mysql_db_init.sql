CREATE DATABASE cloud_storage_cluster;

-- 用户表 --
CREATE TABLE users (
   user_id CHAR(36) PRIMARY KEY DEFAULT UUID(),
   email VARCHAR(255) UNIQUE NOT NULL,
   username VARCHAR(64) UNIQUE NOT NULL,
   password_hash BINARY(60) NOT NULL,
   created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
   last_login_at TIMESTAMP,
   user_status VARCHAR(32) NOT NULL DEFAULT 'NORMAL',
   role VARCHAR(32) NOT NULL DEFAULT 'user',
   storage_capacity BIGINT NOT NULL DEFAULT 5368709120,
   used_capacity BIGINT NOT NULL DEFAULT 0,
   bio TEXT,
   avatar VARCHAR(255) DEFAULT (CONCAT('user-avatars/default/', FLOOR(RAND() * 10), '.png'))
);

CREATE INDEX idx_users_email ON users(email);

-- 文件系统 --
CREATE TABLE directories (
     directory_id CHAR(36) PRIMARY KEY DEFAULT UUID(), -- MySQL不支持函数作为默认值，需通过触发器或应用层生成
     parent_directory_id CHAR(36), -- 外键关联需要手动处理
     user_id CHAR(36), -- 外键关联需要手动处理
     name VARCHAR(255) NOT NULL,
     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
     updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
     UNIQUE (user_id, parent_directory_id, name),
     FOREIGN KEY (user_id) REFERENCES users(user_id), -- 添加外键约束，关联到 users 表的 user_id 字段
     FOREIGN KEY (parent_directory_id) REFERENCES directories(directory_id) -- 添加外键约束，关联到 directories 表的 directory_id 字段
);

CREATE TABLE files (
    file_id CHAR(36) PRIMARY KEY DEFAULT UUID(), -- 同上
    directory_id CHAR(36), -- 外键关联需要手动处理
    user_id CHAR(36), -- 外键关联需要手动处理
    object_name VARCHAR(255) NOT NULL,
    mime_type VARCHAR(128) NOT NULL,
    size BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE (directory_id, object_name),
    FOREIGN KEY (directory_id) REFERENCES directories(directory_id), -- 添加外键约束，关联到 directories 表的 directory_id 字段
    FOREIGN KEY (user_id) REFERENCES users(user_id) -- 添加外键约束，关联到 users 表的 user_id 字段
);

-- 创建父目录索引
CREATE INDEX idx_child_directories
    ON directories(parent_directory_id);

-- 创建用户ID索引，用于快速查询用户的根目录
CREATE INDEX idx_user_id
    ON directories(user_id);

-- 创建父目录和名称联合索引
CREATE INDEX idx_dir_child_name
    ON directories(parent_directory_id, name);

-- 创建文件目录和名称联合索引
CREATE INDEX idx_directory_id_files
    ON files(directory_id, object_name);

-- 创建文件 ID 和名称联合索引
CREATE INDEX idx_object_name
    ON files(file_id, object_name);

-- 创建文件 ID 和用户 ID 联合索引
CREATE INDEX idx_userId
    ON files(file_id, user_id);

-- 创建目录 ID 索引
CREATE INDEX idx_directory_id
    ON files(directory_id);