### [English](https://github.com/g-qw/mcs/blob/main/CHANGELOG.en.md) | [简体中文](https://github.com/g-qw/mcs/blob/main/CHANGELOG.md)

# Update log

## v1.0.0

### New

* The first official version of the high-performance distributed object storage microservice system based on MinIO
  * Backend microservice architecture
    * User Service (identity registration, login, permission management)
    * Email service (verification code, notification email)
    * Upload service (single file, multiple files, block upload)
    * Download service (single file, block download, breakpoint continuous transmission)
    * File system service (virtual directory tree, file/folder CRUD, empty folder cleaning)
    * Gateway service (unified identity verification, dynamic routing, load balancing)
  * front end
    * Responsive Web UI built by Vue 3 + Vite, GitHub style dark theme
    * Interaction imitating Windows File Explorer
    * Automatically select the appropriate upload/download strategy based on file size
    * Real-time progress bar, concurrent upload/download, download task window
