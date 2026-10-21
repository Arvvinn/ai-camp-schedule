# 营期日程 · 安卓原生版

使用 Kotlin / Jetpack Compose / Material 3，支持 Android 8.0 及以上。1.5.0 沿用高对比的工作台界面：白色卡片、浅灰背景、深色正文、灰蓝主色和明确的状态色，阶段标签保留柔和点缀。中文使用 MiSans，少量英文与百分比数字使用 Lora 衬线字体。按 Oil UI 方法精修：进度摘要与周历合为一个规划区，同组任务使用连续列表与缩进分隔线，任务标题优先于营期信息，筛选器共享滑动选中底板。任务标题 15sp、说明 13sp，勾选保持 48dp 点击范围。100% 单独居中，空日程显示「—」。

浮动底部导航与同步胶囊使用实时背景毛玻璃，并加强遮罩以保持文字清楚。保留滑动周历和 Lucide 图标。勾选即时反馈，带轻回弹、圆环扩散和触感，任务淡出后列表自动补位。保存转到后台，同一任务保存时防重复提交，其他任务可继续勾选；失败时恢复已同步记录并提示。未确认状态只用于界面，提醒仍以已保存记录为准。本地快照的读、写、清除共享进程锁，避免不同存储实例同时操作 AtomicFile 时丢失已确认记录。

在「营期 → 添加营期」填写期数、选择开营日期，可选第 1～12 周的假期休息周，保存后自动生成日程并安排本地提醒，与网站共用团队排期。开营日期通常为周日，也允许其他日期。重复期数会提示；网络未能确认保存时，先刷新并核对是否已有该期，避免重复添加。演示模式和离线缓存保持只读。

## 安装到小米手机

将 `artifacts/android/camp-schedule-1.5.0.apk` 发到手机，点击安装；如系统提示，允许当前文件管理器安装应用。安装后用网站相同的邮箱与密码登录。也可以点击「先看看应用」浏览明确标记的只读示例，示例不会写入团队数据库。

在「设置」中开启通知和精准提醒，发送一条测试通知。小米手机在自启动管理中开启「营期日程」，并根据实际需要将应用省电策略设为「无限制」。系统入口因 MIUI / HyperOS 版本而异，应用内提供设置入口和手工路径。

默认北京时间每天 09:00 汇总未完成事项，有明确时刻的任务提前 15 分钟提醒，可修改。周日 24:00 按周一 00:00 处理；“上午”“直播前”等模糊时间不自动猜测，进入每日汇总。在任务详情可设一次性的本机提醒，该设置覆盖本任务的默认时间提醒，不影响团队排期。

任务资料优先交给已安装的飞书打开；飞书未安装或未能处理链接时交给浏览器。文档仍需飞书账号具有访问权限。

已同步的排期和提醒支持离线使用。后台同步由系统安排，云端改期、他人完成任务或权限变动，在成功同步后反映到本机；不能保证关闭应用时立即获知。打开应用可手动刷新。离线不允许修改共享完成状态。系统强行停止应用后需要重新打开，重启设备后会恢复调度。

## 构建

需要 Node.js 24+、JDK 17 或 21、Android SDK Platform 35。设置 `JAVA_HOME` 和 `ANDROID_HOME`；Gradle Wrapper 会下载 Gradle 8.11.1。

```powershell
npm ci
.\android\build.ps1
```

脚本导出最新 `data.js` / `config.js`，运行网站测试、安卓单元测试与 lint，构建启用 R8 优化的 release APK 并生成 SHA-256。首次在本机生成个人安装签名，后续重复使用；密钥和凭据位于 `%USERPROFILE%\.android\camp-schedule-signing\`，引用配置位于被 Git 忽略的 `android/signing.properties`。请妥善备份整个签名目录，后续覆盖更新必须使用相同签名。凭据不可提交仓库或发送给其他人。

其他系统也可配置相同内容的 `android/signing.properties` 后运行 `./gradlew -p android testDebugUnitTest lintDebug assembleRelease`。Properties 文件含 `storeFile`、`storePassword`、`keyAlias`、`keyPassword` 四项。未配置签名时，使用 `assembleDebug` 构建调试版，不能覆盖已安装的 release 版。

```powershell
node scripts/export-android-data.js --check
.\android\gradlew.bat -p android testDebugUnitTest lintDebug assembleDebug
adb install -r artifacts/android/camp-schedule-1.5.0.apk
```

修改 SOP 后需要重新打包 APK；营期开营日期、休息周和完成状态通过现有云端同步，无需重新打包。应用不包含 service role key，不需要新增后台服务或推送平台。

覆盖安装新版前请在 `android/app/build.gradle` 递增 `versionCode`，并保留相同的签名。前台每 30 秒检查一次团队排期，后台通过系统调度定期同步。

## 模拟器集成验证

```powershell
.\android\gradlew.bat -p android assembleDebug assembleDebugAndroidTest
.\scripts\verify-android-background.ps1 -Device emulator-5554
```

此脚本仅允许 Android 模拟器，使用明确的测试夹具并清空测试应用账号；不会写入线上数据。它检查加密缓存、退出清理、权限撤销、登录过期、版本冲突、退出与同步竞争、通知点击路由，并在应用进程退出后等待实际系统闹钟通知。不得在存有真实账号数据的手机上运行这些测试夹具。

安装调试版或 release 版后，可运行 `./scripts/verify-android-ui.ps1 -Device emulator-5554` 检查演示页面、周历滑动、日期保留、筛选、详情与设置；应先退出真实账号。脚本只接受模拟器，截图前会检查前台包名、UI 层级与崩溃日志，结果保存在 `artifacts/android/ui-redesign/`。

## 文件分工与组件来源

- `scripts/export-android-data.js` 将网站内容导出为 `app/src/main/assets/schedule.json`，网站测试防止内容漂移。
- `ScheduleEngine.kt` 与 `ReminderPlan.kt` 负责日期、稳定任务键、跟进和提醒计算。
- `ScheduleRepository.kt` 使用 Supabase Auth、`my_app_access` 和现有版本化排期 RPC；`LocalStore.kt` 使用应用私有缓存和 Android Keystore AES-GCM 加密会话，退出时清除缓存与待发提醒。
- `ReminderScheduler.kt` 使用 AlarmManager；`SyncWorker.kt` 使用 WorkManager 做可延迟的联网同步。接收器恢复重启、升级和时间变化后的提醒。
- `*Screen.kt` 与 `TaskDetail.kt` 使用 Material 3 原生组件和惰性列表。周历采用 [Kizitonwose Calendar 2.6.1](https://github.com/kizitonwose/Calendar)，MIT 许可。图标采用 [Lucide](https://github.com/lucide-icons/lucide) 官方 SVG 的固定提交，由 `scripts/sync-lucide-icons.ps1` 转为原生 VectorDrawable，采用 ISC / 原 Feather MIT 许可。两者许可随 APK 打包，可在「设置 → 开源许可」查看。Kotlin 和 Compose 编译插件统一为 2.0.21；Compose 运行库为 1.7.6。Jetpack 组件遵循 [AndroidX Apache 2.0 许可](https://github.com/androidx/androidx/blob/androidx-main/LICENSE.txt)。
- `GlassSurface.kt` 使用 [Haze 1.1.1](https://github.com/chrisbanes/haze/tree/1.1.1)，Apache 2.0 许可。屏幕只录制一个共享背景层，玻璃控件单独取样；模糊输入缩至 0.66，正文不参与模糊，静止时没有持续运行动画。该版本遵循库的默认平台兼容策略，旧系统使用柔和底色回退。选用匹配 Kotlin 2.0.21 / Compose 1.7 的版本，避免为视觉效果升级整个编译工具链。纯 `Modifier.blur` 会模糊控件自身，不能直接作为背景毛玻璃。
- 中文字体为[小米官方 MiSans](https://hyperos.mi.com/font/zh/download/) 的原始 Regular/Medium OTF 文件，不裁剪、不修改字体。官网字体包 SHA-256 为 `b6aa1fc827035922612df8edf36e5609bca1c5441e25cd57572204569b7b81d9`。英文点缀和百分比使用 [Lora](https://github.com/cyrealtype/Lora-Cyrillic)，OFL 1.1 许可，固定源提交 `403b1a66ca2e79f81d749c5299559e168591a4df`。其气质参考 Claude，但不声称是 Claude 官网的专用字体；Lora 也列于 [Anthropic 的公开品牌建议](https://github.com/anthropics/skills/blob/main/skills/brand-guidelines/SKILL.md)。字体随 APK 离线提供，启动时无需下载。用 `python scripts/prepare-android-font.py`（需 fonttools、pypdf）下载原始文件及许可；MiSans 官方完整许可 PDF 与提取文本、Lora OFL 随 APK 打包，可从「设置 → 字体与组件许可」阅读。设置中保留 MiSans 使用声明。

本地构建、模拟器验证结果与尚需真机检查的项目记录在 `docs/audits/2026-10-19-android.md`、`docs/audits/2026-10-20-android-ui.md`、`docs/audits/2026-10-20-android-completion.md` 和 `docs/audits/2026-10-20-android-responsive-completion.md`。
