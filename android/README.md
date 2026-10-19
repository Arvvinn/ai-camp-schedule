# 营期日程 · 安卓原生版

使用 Kotlin / Jetpack Compose / Material 3，支持 Android 8.0 及以上。界面沿用网站的深蓝、浅灰、白色卡片与阶段配色。网站继续负责管理营期，安卓端查看全部阶段、勾选共享任务、打开资料并发送本机通知。

## 安装到小米手机

将 `artifacts/android/camp-schedule-1.0.0.apk` 发到手机，点击安装；如系统提示，允许当前文件管理器安装应用。安装后用网站相同的邮箱与密码登录。也可以点击「先看看应用」浏览明确标记的只读示例，示例不会写入团队数据库。

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
adb install -r artifacts/android/camp-schedule-1.0.0.apk
```

修改 SOP 后需要重新打包 APK；营期开营日期、休息周和完成状态通过现有云端同步，无需重新打包。应用不包含 service role key，不需要新增后台服务或推送平台。

覆盖安装新版前请在 `android/app/build.gradle` 递增 `versionCode`，并保留相同的签名。前台每 30 秒检查一次团队排期，后台通过系统调度定期同步。

## 模拟器集成验证

```powershell
.\android\gradlew.bat -p android assembleDebug assembleDebugAndroidTest
.\scripts\verify-android-background.ps1 -Device emulator-5554
```

此脚本仅允许 Android 模拟器，使用明确的测试夹具并清空测试应用账号；不会写入线上数据。它检查加密缓存、退出清理、权限撤销、登录过期、版本冲突、退出与同步竞争、通知点击路由，并在应用进程退出后等待实际系统闹钟通知。不得在存有真实账号数据的手机上运行这些测试夹具。

## 文件分工与组件来源

- `scripts/export-android-data.js` 将网站内容导出为 `app/src/main/assets/schedule.json`，网站测试防止内容漂移。
- `ScheduleEngine.kt` 与 `ReminderPlan.kt` 负责日期、稳定任务键、跟进和提醒计算。
- `ScheduleRepository.kt` 使用 Supabase Auth、`my_app_access` 和现有版本化排期 RPC；`LocalStore.kt` 使用应用私有缓存和 Android Keystore AES-GCM 加密会话，退出时清除缓存与待发提醒。
- `ReminderScheduler.kt` 使用 AlarmManager；`SyncWorker.kt` 使用 WorkManager 做可延迟的联网同步。接收器恢复重启、升级和时间变化后的提醒。
- `*Screen.kt` 与 `TaskDetail.kt` 使用 Material 3 原生组件和惰性列表。参考 [Google Compose 官方示例](https://github.com/android/compose-samples)，第一版周日期条无需额外日历库。Jetpack 组件遵循 [AndroidX Apache 2.0 许可](https://github.com/androidx/androidx/blob/androidx-main/LICENSE.txt)。

本地构建、模拟器验证结果与尚需真机检查的项目记录在 `docs/audits/2026-10-19-android.md`。
