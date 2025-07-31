CREATE DATABASE cloud_storage_cluster;

-- 返回一个随机的头像地址
CREATE OR REPLACE FUNCTION generate_random_avatar()
    RETURNS TEXT AS $$
BEGIN
    RETURN 'user-avatars/default/' || floor(random() * 10)::TEXT || '.png';
END;
$$ LANGUAGE plpgsql;

-- 用户表 --
CREATE TABLE users (
    user_id UUID PRIMARY KEY DEFAULT gen_random_uuid(), -- 用户ID，主键，使用UUID，自动生成
    email VARCHAR(255) UNIQUE NOT NULL, -- 邮箱地址，唯一，非空
    username VARCHAR(64) UNIQUE NOT NULL, -- 用户名，唯一，非空
    password_hash BYTEA NOT NULL, -- 密码哈希，存储哈希值和盐值的组合，非空
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(), -- 创建时间，默认为当前时间
    last_login_at TIMESTAMP WITHOUT TIME ZONE, -- 最后登录时间，可为空
    user_status VARCHAR(32) NOT NULL DEFAULT 'NORMAL', -- 用户状态，如激活、禁用等，非空
    role VARCHAR(32) NOT NULL DEFAULT 'user', -- 权限级别，如普通用户、管理员等
    storage_capacity NUMERIC NOT NULL DEFAULT 5368709120, -- 存储容量，默认 5GB，单位：字节
    used_capacity NUMERIC NOT NULL DEFAULT 0, -- 已使用容量，单位：字节
    bio TEXT, -- 用户简介，可为空
    avatar VARCHAR(255) DEFAULT generate_random_avatar() -- 用户头像 url，可为空
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_username ON users(username);

-- 文件系统 --
CREATE TABLE directories (
     directory_id UUID PRIMARY KEY DEFAULT gen_random_uuid(), -- 目录ID，主键，自动生成
     parent_directory_id UUID REFERENCES directories(directory_id),  --目录的父目录ID，外键关联到本表的directory_id，NULL表示根目录
     user_id UUID REFERENCES users(user_id),  -- 目录所属用户ID，外键关联到用户表的user_id
     name VARCHAR(255) NOT NULL,  -- 文件夹名称，最大 255 个字符，非空
     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,  -- 创建时间，默认为当前时间
     updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,   -- 更新时间，默认为当前时间
     UNIQUE (user_id, parent_directory_id, name)  -- 同一个目录下不能有相同名称的子目录
);

CREATE TABLE files (
    file_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),  -- 文件ID，主键，自动生成
    directory_id UUID REFERENCES directories(directory_id),  -- 文件所在目录ID，外键关联到目录表的directory_id
    user_id UUID REFERENCES users(user_id),  -- 文件所属用户ID，同时也是minio中用户的桶名称，外键关联到用户表的user_id
    object_name VARCHAR(255) NOT NULL, -- 文件在minio中的对象名称，必须包括文件的拓展名
    mime_type VARCHAR(128) NOT NULL,  -- 文件的MIME类型
    size BIGINT,  -- 文件大小，单位：字节，可为空
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,  -- 创建时间，默认为当前时间
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,   -- 更新时间，默认为当前时间
    UNIQUE(directory_id, object_name)  -- 同一个目录下不能有相同名称的文件
);

-- 创建部分索引，索引所有的根目录，用于快速查询根目录的ID
CREATE INDEX idx_root_directories
    ON directories(user_id)
    WHERE parent_directory_id IS NULL;

-- 索引子目录（父目录ID），用于快速查询子目录
CREATE INDEX idx_child_directories ON directories(parent_directory_id);

-- 索引子目录（父目录ID + 名称），查询指定目录下的子目录
CREATE INDEX idx_dir_child_name ON directories(parent_directory_id, name);

-- 索引文件（目录ID + 文件名），用于指定目录下按文件名查询
CREATE INDEX idx_directory_id_files ON files(directory_id, object_name);

-- 索引文件（文件ID + 用户ID），用于快速查询指定文件的名称
CREATE INDEX idx_object_name ON files(file_id, object_name);

-- 联合索引，用于查询指定文件的存储桶
CREATE INDEX idx_userId ON files(file_id, user_id);

-- 索引文件（目录ID），用于查询某一目录下的所有子文件
CREATE INDEX idx_directory_id ON files(directory_id);


