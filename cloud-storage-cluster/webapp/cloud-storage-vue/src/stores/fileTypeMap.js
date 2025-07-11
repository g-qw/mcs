// 文件扩展名 -> 类型描述
export const fileTypeMap = new Map([
    ['.exe', '可执行文件'],
    ['.sh', 'Shell 脚本'],
    ['.bat', 'Windows 批处理脚本'],
    ['.ps1', 'PowerShell 脚本'],
    ['.apk', 'Android 应用程序包'],
    ['.ipa', 'iOS 应用程序包'],

    /* ========== 文档部分 ========== */ 
    ['.txt', '文本文件'],
    ['.doc', 'Microsoft Word 文档'],
    ['.docx', 'Microsoft Word 文档'],
    ['.ppt', 'Microsoft PowerPoint 演示文稿'],
    ['.pptx', 'Microsoft PowerPoint 演示文稿'],
    ['.xls', 'Microsoft Excel 工作簿'],
    ['.xlsx', 'Microsoft Excel 工作簿'],
    ['.pdf', 'PDF 文档'],
    ['.json', 'JSON 配置文件'],
    ['.xml', 'XML 配置文件'],
    ['.yml', 'YAML 配置文件'],
    ['.yaml', 'YAML 配置文件'],
    ['.ini', 'INI 配置文件'],
    ['.db', '数据库文件'],
    ['.sql', 'SQL 脚本'],
    ['.ttf', 'TrueType 字体文件'],
    ['.woff', 'Web Open Font Format 字体文件'],
    ['.md', 'Markdown 文件'],
    ['.rtf', '富文本格式文件'],

    /* ========== 图片部分 ========== */ 
    ['.jpg', 'JPEG 图片'],
    ['.jpeg', 'JPEG 图片'],
    ['.png', 'PNG 图片'],
    ['.gif', 'GIF 图片'],
    ['.bmp', 'BMP 图片'],
    ['.webp', 'WebP 图片'],
    ['.tiff', 'TIFF 图片'],
    ['.tif', 'TIFF 图片'],
    ['.svg', 'SVG 矢量图'],
    ['.ico', '图标文件'],
    ['.psd', 'Photoshop 图像文件'],

    /* ========== 音频部分 ========== */ 
    ['.mp3', 'MP3 音频文件'],
    ['.wav', 'WAV 音频文件'],
    ['.aac', 'AAC 音频文件'],
    ['.flac', 'FLAC 音频文件'],
    ['.ogg', 'OGG 音频文件'],
    ['.wma', 'WMA 音频文件'],
    ['.m4a', 'M4A 音频文件'],
    ['.amr', 'AMR 音频文件'],
    ['.aiff', 'AIFF 音频文件'],
    ['.ape', 'APE 音频文件'],
    ['.dts', 'DTS 音频文件'],

    /* ========== 压缩文件部分 ========== */ 
    ['.zip', 'ZIP 压缩文件'],
    ['.tar', 'TAR 压缩文件'],
    ['.gz', 'GZ 压缩文件'],
    ['.rar', 'RAR 压缩文件'],
    ['.7z', '7z 压缩文件'],

    /* ========== 视频部分 ========== */ 
    ['.mp4', 'MP4 视频文件'],
    ['.avi', 'AVI 视频文件'],
    ['.mov', 'MOV 视频文件'],
    ['.flv', 'FLV 视频文件'],
    ['.mpg', 'MPEG 视频文件'],
    ['.mkv', 'Matroska 视频文件'],
    ['.webm', 'WebM 视频文件'],
    ['.3gp', '3GP 视频文件'],
    ['.m4v', 'M4V 视频文件'],
    ['.wmv', 'WMV 视频文件'],
    ['.asf', 'ASF 视频文件'],
    ['.ogv', 'OGV 视频文件'],

    /* ========== 源程序文件部分 ========== */ 
    ['.js', 'JavaScript 文件'],
    ['.py', 'Python 文件'],
    ['.java', 'Java 文件'],
    ['.php', 'PHP 文件'],
    ['.gitignore', 'Git 忽略文件'],
    ['.git', 'Git 仓库目录'],
    ['.cpp', 'C++ 源代码'],
    ['.c', 'C 源代码'],
    ['.h', 'C/C++ 头文件'],
    ['.hpp', 'C++ 头文件'],
    ['.cs', 'C# 文件'],
    ['.swift', 'Swift 文件'],
    ['.kt', 'Kotlin 文件'],
    ['.go', 'Go 文件'],
    ['.rs', 'Rust 文件'],
    ['.ts', 'TypeScript 文件'],
    ['.scala', 'Scala 文件'],
    ['.rb', 'Ruby 文件'],
    ['.pl', 'Perl 文件'],
    ['.lua', 'Lua 文件'],
    ['.groovy', 'Groovy 文件'],
    ['.r', 'R 语言文件'],
    ['.m', 'Objective-C 文件'],
    ['.mm', 'Objective-C++ 文件'],
    ['.vue', 'Vue.js 文件'],
    ['.jsx', 'React JSX 文件'],
    ['.tsx', 'React TypeScript JSX 文件'],
    ['.dart', 'Dart 文件'],
    ['.html', 'HTML 文件'],
    ['.css', 'CSS 文件'],
    ['.scss', 'SCSS 文件'],
    ['.less', 'LESS 文件'],
    ['.toml', 'TOML 文件'],
    ['.cfg', '配置文件'],
    ['.asm', '汇编语言文件'],
    ['.f90', 'Fortran 90 文件'],
    ['.pro', 'Prolog 文件'],
    ['.ml', 'OCaml 文件'],
    ['.hs', 'Haskell 文件'],
    ['.el', 'Emacs Lisp 文件'],
    ['.lisp', 'Lisp 文件'],
    ['.clj', 'Clojure 文件'],
    ['.cljs', 'ClojureScript 文件'],
    ['.erl', 'Erlang 文件'],
    ['.ex', 'Elixir 文件'],
    ['.fs', 'F# 文件'],
    ['.fsx', 'F# 脚本文件'],
    ['.jl', 'Julia 文件'],
    ['.mat', 'MATLAB 文件'],
    ['.mex', 'MATLAB MEX 文件'],
]);

// 文件扩展名 -> icon 名称
export const fileIconMap = new Map([
    ['.exe', 'exe'], // 可执行文件
    ['.sh', 'exe'], // shell 脚本
    ['.bat', 'exe'], // bat 脚本
    ['.ps1', 'exe'], // powershell 脚本
    ['.apk', 'exe'], // Android 应用程序包
    ['.ipa', 'exe'], // ios 应用程序包

    /* ========== 文档部分 ========== */ 
    ['.txt', 'document'], // 文本文件
    ['.doc', 'document'], // Word 文档
    ['.docx', 'document'], // Word 文档
    ['.ppt', 'document'], // PPT 演示文稿
    ['.pptx', 'document'], // PPT 演示文稿
    ['.xls', 'document'], // Excel 电子表格
    ['.xlsx', 'document'], // Excel 电子表格
    ['.pdf', 'document'], // PDF 文档
    ['.json', 'document'], // JSON 配置
    ['.xml', 'document'], // XML 配置
    ['.yml', 'document'], // YAML 配置
    ['.yaml', 'document'], // YAML 配置
    ['.ini', 'document'], // INI 配置
    ['.db', 'document'], // 数据库文件
    ['.sql', 'document'], // SQL 脚本
    ['.ttf', 'document'], // 字体文件
    ['.woff', 'document'], // 字体文件
    ['.md', 'document'], // Markdown 文件
    ['.rtf', 'document'], // 富文本格式

    /* ========== 图片部分 ========== */ 
    ['.jpg', 'image'], // JPG 图片
    ['.jpeg', 'image'], // JPEG 图片
    ['.png', 'image'], // PNG 图片
    ['.gif', 'image'], // GIF 图片
    ['.bmp', 'image'], // BMP 图片
    ['.webp', 'image'], // WebP 图片
    ['.tiff', 'image'], // TIFF 图片
    ['.tif', 'image'], // TIFF 图片的另一种扩展名
    ['.svg', 'image'], // SVG 图片
    ['.ico', 'image'], // ICO 图片
    ['.psd', 'image'], // PSD 图片

    /* ========== 音频部分 ========== */ 
    ['.mp3', 'music'], // MP3 音乐
    ['.wav', 'music'], // WAV 音频
    ['.aac', 'music'], // AAC 音频
    ['.flac', 'music'], // FLAC 音频
    ['.ogg', 'music'], // OGG 音频
    ['.wma', 'music'], // WMA 音频
    ['.m4a', 'music'], // M4A 音频
    ['.amr', 'music'], // AMR 音频
    ['.aiff', 'music'], // AIFF 音频
    ['.ape', 'music'], // APE 音频
    ['.dts', 'music'], // DTS 音频

    /* ========== 压缩文件部分 ========== */ 
    ['.zip', 'zip'], // ZIP 压缩文件
    ['.tar', 'zip'], // tar 压缩文件
    ['.gz', 'zip'], // gz压缩文件
    ['.rar', 'zip'], // RAR 压缩文件
    ['.7z', 'zip'], // 7z 压缩文件

    /* ========== 视频部分 ========== */ 
    ['.mp4', 'video'], // MP4 视频
    ['.avi', 'video'], // AVI 视频
    ['.mov', 'video'], // MOV 视频
    ['.flv', 'video'], // FLV 视频
    ['.mpg', 'video'], // MPEG 视频
    ['.mkv', 'video'], // Matroska 视频
    ['.webm', 'video'], // WebM 视频
    ['.3gp', 'video'], // 3GP 视频
    ['.m4v', 'video'], // M4V 视频
    ['.wmv', 'video'], // WMV 视频
    ['.asf', 'video'], // ASF 视频
    ['.ogv', 'video'], // OGV 视频

    /* ========== 源程序文件部分 ========== */ 
    ['.js', 'src'], // JavaScript 文件
    ['.py', 'src'], // Python 文件
    ['.java', 'src'], // Java 文件
    ['.php', 'src'], // PHP 文件
    ['.gitignore', 'src'], // Git 忽略文件
    ['.git', 'src'], // Git 仓库目录
    ['.js', 'src'], // JavaScript 文件
    ['.py', 'src'], // Python 文件
    ['.java', 'src'], // Java 文件
    ['.php', 'src'], // PHP 文件
    ['.gitignore', 'src'], // Git 忽略文件
    ['.git', 'src'], // Git 仓库目录
    ['.cpp', 'src'], // C++ 源代码
    ['.c', 'src'], // C 源代码
    ['.h', 'src'], // C/C++ 头文件
    ['.hpp', 'src'], // C++ 头文件
    ['.cs', 'src'], // C# 文件
    ['.swift', 'src'], // Swift 文件
    ['.kt', 'src'], // Kotlin 文件
    ['.go', 'src'], // Go 文件
    ['.rs', 'src'], // Rust 文件
    ['.ts', 'src'], // TypeScript 文件
    ['.scala', 'src'], // Scala 文件
    ['.rb', 'src'], // Ruby 文件
    ['.pl', 'src'], // Perl 文件
    ['.lua', 'src'], // Lua 文件
    ['.groovy', 'src'], // Groovy 文件
    ['.r', 'src'], // R 语言文件
    ['.m', 'src'], // Objective-C 文件
    ['.mm', 'src'], // Objective-C++ 文件
    ['.vue', 'src'], // Vue.js 文件
    ['.jsx', 'src'], // React JSX 文件
    ['.tsx', 'src'], // React TypeScript JSX 文件
    ['.dart', 'src'], // Dart 文件
    ['.html', 'src'], // HTML 文件
    ['.css', 'src'], // CSS 文件
    ['.scss', 'src'], // SCSS 文件
    ['.less', 'src'], // LESS 文件
    ['.toml', 'src'], // TOML 文件
    ['.ini', 'src'], // INI 配置文件
    ['.cfg', 'src'], // 配置文件
    ['.asm', 'src'], // 汇编语言文件
    ['.f90', 'src'], // Fortran 90 文件
    ['.pro', 'src'], // Prolog 文件
    ['.ml', 'src'], // OCaml 文件
    ['.hs', 'src'], // Haskell 文件
    ['.el', 'src'], // Emacs Lisp 文件
    ['.lisp', 'src'], // Lisp 文件
    ['.clj', 'src'], // Clojure 文件
    ['.cljs', 'src'], // ClojureScript 文件
    ['.erl', 'src'], // Erlang 文件
    ['.ex', 'src'], // Elixir 文件
    ['.fs', 'src'], // F# 文件
    ['.fsx', 'src'], // F# 脚本文件
    ['.jl', 'src'], // Julia 文件
    ['.mat', 'src'], // MATLAB 文件
    ['.mex', 'src'], // MATLAB MEX 文件
])