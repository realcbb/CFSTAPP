# Clash Meta YAML 导出

测速结束后点击「导出 Clash Meta YAML」，填写 UUID、TLS SNI、WebSocket Host、路径和端口，勾选 IP 并点击「保存 YAML」。安卓系统文件选择器可以将文件保存到下载目录或用户选择的位置，不需要申请整个存储空间的访问权限。

参数首次填写后保存在应用私有 SharedPreferences。公开源码不包含私人参数。导出的文件包含 UUID，请仅分享给可信对象。取消文件选择不会显示保存成功；写入失败会留在对话框内显示错误。

生成的配置沿用 CF IP 配置工坊：VLESS + WS + TLS、手动节点组、可选自动测速组、AI 服务组、国内直连和默认代理规则，支持 IPv4 / IPv6 输入与 IP 去重。测速完成仅代表入口测试结果；实际节点可用性仍取决于填写的服务端参数。首次导入时 mihomo 需要联网下载 geodata。

GitHub Actions 的 Android APK 工作流执行单元测试并构建 debug APK，可在运行详情的 Artifacts 下载 CFSTAPP-debug。debug 包使用 CI 临时签名，不保证不同构建可覆盖安装；长期发行应配置固定发布签名。
