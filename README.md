1.安装idea 在插件库搜索安装 scala插件 安装 这个插件为了方便看代码 和编辑代码方便
2.下载android ndk12b  (https://developer.android.google.cn/ndk/)
3.下载android studio（https://developer.android.google.cn/studio/ ：主要用里面的android sdk,目前找不到单独sdk下载，只能下载这个软件了，利用android studio里的sdkmanager下载 anddroid platform 25 sdk）
4.下载 jdk1.8.1 （https://www.oracle.com/technetwork/java/javase/downloads/index.html）
5.下载 sbt 1.2.3 （https://www.scala-sbt.org）

6.配置sdk，ndk环境目录 ANDROID_HOME= /sdk目录 ，ANDROID_NDK_HOME=/ndk目录

7.git 代码下来，git clone https://github.com/shadowsocksrr/shadowsocksr-android.git 执行git checkout Akkariiin/master 更换我mac下编译的库 src/下asset 和libs目录 

8.在命令行进入到项目目录 执行 sbt clean android:package-release 下载依赖包并编译apk

ps编译出来的apk打开直接闪退，
如果原因是说找不到资源文件， android.content.res.Resources$NotFoundException: Drawable in.zhaoj.shadowsocksrr:drawable/abc_vector_test with resource ID #0x7f020052
请清空target目录 下的文件 重新编译

rm -rf ~/.android/sbt/exploded-aars/  上面那个原因仍没法解决，可以用这个命令把编译依赖的缓存包清空，可解决大多找不到依赖的库类问题