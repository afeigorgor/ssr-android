/*
 * Shadowsocks - A shadowsocks client for Android
 * Copyright (C) 2014 <max.c.lv@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *                            ___====-_  _-====___
 *                      _--^^^#####//      \\#####^^^--_
 *                   _-^##########// (    ) \\##########^-_
 *                  -############//  |\^^/|  \\############-
 *                _/############//   (@::@)   \\############\_
 *               /#############((     \\//     ))#############\
 *              -###############\\    (oo)    //###############-
 *             -#################\\  / VV \  //#################-
 *            -###################\\/      \//###################-
 *           _#/|##########/\######(   /\   )######/\##########|\#_
 *           |/ |#/\#/\#/\/  \#/\##\  |  |  /##/\#/  \/\#/\#/\#| \|
 *           `  |/  V  V  `   V  \#\| |  | |/#/  V   '  V  V  \|  '
 *              `   `  `      `   / | |  | | \   '      '  '   '
 *                               (  | |  | |  )
 *                              __\ | |  | | /__
 *                             (vvv(VVV)(VVV)vvv)
 *
 *                              HERE BE DRAGONS
 *
 */
package com.github.shadowsocks

import java.io.IOException
import java.lang.System.currentTimeMillis
import java.net.{HttpURLConnection, URL}
import java.util
import java.util.{GregorianCalendar, Locale}

import android.app.backup.BackupManager
import android.app.{Activity, AlertDialog, ProgressDialog}
import android.content._
import android.content.pm.{PackageInfo, PackageManager}
import android.graphics.Typeface
import android.net.VpnService
import android.os._
import android.support.design.widget.{FloatingActionButton, Snackbar}
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.{View, ViewGroup, WindowManager}
import android.widget._
import com.github.jorgecastilloprz.FABProgressCircle
import com.github.shadowsocks.aidl.IShadowsocksServiceCallback
import com.github.shadowsocks.database._
import com.github.shadowsocks.utils.CloseUtils._
import com.github.shadowsocks.utils._
import com.github.shadowsocks.job.SSRSubUpdateJob
import com.github.shadowsocks.ShadowsocksApplication.app
import okhttp3.{Call, Response}
import top.bitleo.http.{NetUtils, SystemUtil, ToolUtils}
import com.github.shadowsocks.flyrouter.R
import org.json.JSONObject

import scala.util.Random
import android.net.Uri
import com.ansen.http.net.HTTPCaller
import io.github.lizhangqu.coreprogress.ProgressUIListener

object Typefaces {
  def get(c: Context, assetPath: String): Typeface = {
    cache synchronized {
      if (!cache.containsKey(assetPath)) {
        try {
          cache.put(assetPath, Typeface.createFromAsset(c.getAssets, assetPath))
        } catch {
          case e: Exception =>
            Log.e(TAG, "Could not get typeface '" + assetPath + "' because " + e.getMessage)
            app.track(e)
            return null
        }
      }
      return cache.get(assetPath)
    }
  }

  private final val TAG = "Typefaces"
  private final val cache = new util.Hashtable[String, Typeface]
}

object Shadowsocks {
  // Constants
  private final val TAG = "Shadowsocks"
  private final val REQUEST_CONNECT = 1
  private val EXECUTABLES = Array(Executable.PDNSD, Executable.REDSOCKS, Executable.SS_TUNNEL, Executable.SS_LOCAL,
    Executable.TUN2SOCKS)
}

class Shadowsocks extends AppCompatActivity with ServiceBoundContext{

  import Shadowsocks._

  // Variables
  var serviceStarted: Boolean = _
  var fab: FloatingActionButton = _
  var fabProgressCircle: FABProgressCircle = _
  var progressDialog: ProgressDialog = _
  var state = State.STOPPED
  var currentProfile = new Profile
  val perms = Array(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
   val PERMS_REQUEST_CODE = 200

  // Services
  private val callback = new IShadowsocksServiceCallback.Stub {
    def stateChanged(s: Int, profileName: String, m: String) {
      handler.post(() => {
        s match {
          case State.CONNECTING =>
            fab.setBackgroundTintList(greyTint)
            fab.setImageResource(R.drawable.ic_start_busy)
            fab.setEnabled(false)
            fabProgressCircle.show()
            preferences.setEnabled(false)
            stat.setVisibility(View.GONE)
          case State.CONNECTED =>
            fab.setBackgroundTintList(greenTint)
            if (state == State.CONNECTING) {
              fabProgressCircle.beginFinalAnimation()
            } else {
              fabProgressCircle.postDelayed(hideCircle, 1000)
            }
            fab.setEnabled(true)
            changeSwitch(checked = true)
            preferences.setEnabled(false)
            stat.setVisibility(View.VISIBLE)
            if (app.isNatEnabled) connectionTestText.setVisibility(View.GONE)
            else {
              connectionTestText.setVisibility(View.VISIBLE)
              connectionTestText.setText(getString(R.string.connection_test_pending))
            }
          case State.STOPPED =>
            fab.setBackgroundTintList(greyTint)
            fabProgressCircle.postDelayed(hideCircle, 1000)
            fab.setEnabled(true)
            changeSwitch(checked = false)
            if (m != null) {
              val snackbar = Snackbar.make(findViewById(android.R.id.content),
                getString(R.string.vpn_error).formatLocal(Locale.ENGLISH, m), Snackbar.LENGTH_LONG)
              if (m == getString(R.string.nat_no_root)) snackbar.setAction(R.string.switch_to_vpn,
                (_ => preferences.natSwitch.setChecked(false)): View.OnClickListener)
              snackbar.show
              Log.e(TAG, "Error to start VPN service: " + m)
            }
            preferences.setEnabled(true)
            stat.setVisibility(View.GONE)
          case State.STOPPING =>
            fab.setBackgroundTintList(greyTint)
            fab.setImageResource(R.drawable.ic_start_busy)
            fab.setEnabled(false)
            if (state == State.CONNECTED) fabProgressCircle.show() // ignore for stopped
            preferences.setEnabled(false)
            stat.setVisibility(View.GONE)
        }
        state = s
      })
    }

    def trafficUpdated(txRate: Long, rxRate: Long, txTotal: Long, rxTotal: Long) {
      handler.post(() => updateTraffic(txRate, rxRate, txTotal, rxTotal))
    }
  }

  def updateTraffic(txRate: Long, rxRate: Long, txTotal: Long, rxTotal: Long) {
    txText.setText(TrafficMonitor.formatTraffic(txTotal))
    rxText.setText(TrafficMonitor.formatTraffic(rxTotal))
    txRateText.setText(TrafficMonitor.formatTraffic(txRate) + "/s")
    rxRateText.setText(TrafficMonitor.formatTraffic(rxRate) + "/s")
  }

  def attachService: Unit = attachService(callback)

  override def onServiceConnected() {
    // Update the UI
    if (fab != null) fab.setEnabled(true)
    updateState()
    if (Build.VERSION.SDK_INT >= 21 && app.isNatEnabled) {
      val snackbar = Snackbar.make(findViewById(android.R.id.content), R.string.nat_deprecated, Snackbar.LENGTH_LONG)
      snackbar.setAction(R.string.switch_to_vpn, (_ => preferences.natSwitch.setChecked(false)): View.OnClickListener)
      snackbar.show
    }
  }

  override def onServiceDisconnected() {
    if (fab != null) fab.setEnabled(false)
  }


  override def binderDied {
    detachService()
    app.crashRecovery()
    attachService
  }

  private var testCount: Int = _
  private var stat: View = _
  private var connectionTestText: TextView = _
  private var txText: TextView = _
  private var rxText: TextView = _
  private var txRateText: TextView = _
  private var rxRateText: TextView = _

  private lazy val greyTint = ContextCompat.getColorStateList(this, R.color.material_blue_grey_700)
  private lazy val greenTint = ContextCompat.getColorStateList(this, R.color.material_green_700)
  //private var adView: AdView = _
  private lazy val preferences =
  getFragmentManager.findFragmentById(android.R.id.content).asInstanceOf[ShadowsocksSettings]

  val handler = new Handler()

  private def changeSwitch(checked: Boolean) {
    serviceStarted = checked
    fab.setImageResource(if (checked) R.drawable.ic_start_connected else R.drawable.ic_start_idle)
    if (fab.isEnabled) {
      fab.setEnabled(false)
      handler.postDelayed(() => fab.setEnabled(true), 1000)
    }
  }

  private def showProgress(msg: Int): Handler = {
    clearDialog()
    progressDialog = ProgressDialog.show(this, "", getString(msg), true, false)
    new Handler {
      override def handleMessage(msg: Message) {
        clearDialog()
      }
    }
  }

  def cancelStart() {
    clearDialog()
    changeSwitch(checked = false)
  }

  def prepareStartService() {
    Utils.ThrowableFuture {
      if (app.isNatEnabled) serviceLoad()
      else {
        val intent = VpnService.prepare(this)
        if (intent != null) {
          startActivityForResult(intent, REQUEST_CONNECT)
        } else {
          handler.post(() => onActivityResult(REQUEST_CONNECT, Activity.RESULT_OK, null))
        }
      }
    }
  }

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
//    getWindow.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
//    getWindow.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

    setContentView(R.layout.layout_main)
    // Initialize Toolbar
    val toolbar = findViewById(R.id.toolbar).asInstanceOf[Toolbar]
    toolbar.setTitle(getString(R.string.main_title)) // non-translatable logo
    toolbar.setTitleTextAppearance(toolbar.getContext, R.style.Toolbar_Logo)
    val field = classOf[Toolbar].getDeclaredField("mTitleTextView")
    field.setAccessible(true)
    val title = field.get(toolbar).asInstanceOf[TextView]
    title.setFocusable(true)
    title.setGravity(0x10)
    title.getLayoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
    title.setOnClickListener(_ => startActivity(new Intent(this, classOf[ProfileManagerActivity])))
    val typedArray = obtainStyledAttributes(Array(R.attr.selectableItemBackgroundBorderless))
    title.setBackgroundResource(typedArray.getResourceId(0, 0))
    typedArray.recycle
    val tf = Typefaces.get(this, "fonts/Iceland.ttf")
    if (tf != null) title.setTypeface(tf)
    title.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_arrow_drop_down, 0)

    stat = findViewById(R.id.stat)
    connectionTestText = findViewById(R.id.connection_test).asInstanceOf[TextView]
    txText = findViewById(R.id.tx).asInstanceOf[TextView]
    txRateText = findViewById(R.id.txRate).asInstanceOf[TextView]
    rxText = findViewById(R.id.rx).asInstanceOf[TextView]
    rxRateText = findViewById(R.id.rxRate).asInstanceOf[TextView]
    stat.setOnClickListener(_ => {
      val id = synchronized {
        testCount += 1
        handler.post(() => connectionTestText.setText(R.string.connection_test_testing))
        testCount
      }
      Utils.ThrowableFuture {
        // Based on: https://android.googlesource.com/platform/frameworks/base/+/master/services/core/java/com/android/server/connectivity/NetworkMonitor.java#640
        autoDisconnect(new URL("https", "www.google.com", "/generate_204").openConnection()
          .asInstanceOf[HttpURLConnection]) { conn =>
          conn.setConnectTimeout(5 * 1000)
          conn.setReadTimeout(5 * 1000)
          conn.setInstanceFollowRedirects(false)
          conn.setUseCaches(false)
          if (testCount == id) {
            var result: String = null
            var success = true
            try {
              val start = currentTimeMillis
              conn.getInputStream
              val elapsed = currentTimeMillis - start
              val code = conn.getResponseCode
              if (code == 204 || code == 200 && conn.getContentLength == 0){
                result = getString(R.string.connection_test_available, elapsed: java.lang.Long) //测试结果
                var pingValue:String = elapsed.toString+"ms"
                var requestJson = SharedPrefsUtil.getValue(this,ToolUtils.SHARE_KEY,ToolUtils.LOCAL_BETA_JSON,"")
                if(!"".equals(requestJson)){
                  var  p:Profile = app.currentProfile match {
                    case Some(p) => p
                    case None =>
                      app.profileManager.getFirstProfile match {
                        case Some(p) =>
                          app.profileId(p.id)
                          p
                        case None => null
                      }
                  }
                  if(p==null){
                    requestJson=ToolUtils.getRecordJson(requestJson,pingValue,"127.0.0.1");
                  }else{
                    requestJson=ToolUtils.getRecordJson(requestJson,pingValue,p.host);
                  }
                  Log.d(TAG,"xiaoliu ping requestJson:"+requestJson)
                  var encodePingValue = AESOperator.getInstance().encrypt(requestJson)
                   NetUtils.getInstance().postDataAsynToNet(NetUtils.RECORD_PING,encodePingValue,new NetUtils.MyNetCall {
                    override def success(call: Call, response: Response): Unit = {
                      var body =response.body().string()
                      Log.d(TAG, "xiaoliu ping request success:"+body)
                      if(body != null){
                        var json = ToolUtils.parseToJson(body)
                        var code =json.getInt("code")
                        if(code!=200) {

                        }
                      }
                    }
                    override def failed(call: Call, e: IOException): Unit = {

                    }
                  })
                }
              }
              else throw new Exception(getString(R.string.connection_test_error_status_code, code: Integer))
            } catch {
              case e: Exception =>
                success = false
                result = getString(R.string.connection_test_error, e.getMessage)
            }
            synchronized(if (testCount == id && app.isVpnEnabled) handler.post(() =>
              if (success) connectionTestText.setText(result)
              else {
                connectionTestText.setText(R.string.connection_test_fail)
                Snackbar.make(findViewById(android.R.id.content), result, Snackbar.LENGTH_LONG).show
              }))
          }
        }
      }
    })

    fab = findViewById(R.id.fab).asInstanceOf[FloatingActionButton]
    fabProgressCircle = findViewById(R.id.fabProgressCircle).asInstanceOf[FABProgressCircle]
    fab.setOnClickListener(_ => if (serviceStarted) serviceStop()
    else if (bgService != null) prepareStartService()
    else changeSwitch(checked = false))
    fab.setOnLongClickListener((v: View) => {
      Utils.positionToast(Toast.makeText(this, if (serviceStarted) R.string.stop else R.string.connect,
        Toast.LENGTH_SHORT), fab, getWindow, 0, Utils.dpToPx(this, 8)).show
      true
    })
    updateTraffic(0, 0, 0, 0)

    app.ssrsubManager.getFirstSSRSub match {
      case Some(first) => {

      }
      case None => null
    }

    SSRSubUpdateJob.schedule()

    handler.post(() => attachService)

    ToolUtils.requestPermissionsReadPhoneState(this);

    //开始检测有没版本更新
    import android.content.pm.PackageManager
    import android.os.Build
    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1 && PackageManager.PERMISSION_GRANTED != checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) requestPermissions(perms, PERMS_REQUEST_CODE)
    else checkUpdate();


  }

  private def checkUpdate(): Unit ={
    var imei = SystemUtil.getIMEI(this);
    var pm:PackageManager = this.getPackageManager();//context为当前Activity上下文
    var pi:PackageInfo = pm.getPackageInfo(this.getPackageName(), 0);

    var requestJson  = ToolUtils.getCheckUpdateJson(pi.versionName,imei);
    if(!"".equals(requestJson)) {
      var encodeJson = AESOperator.getInstance().encrypt(requestJson)
      NetUtils.getInstance().postDataAsynToNet(NetUtils.APP_UPDATE, encodeJson, new NetUtils.MyNetCall {
        override def success(call: Call, response: Response): Unit = {
          var body = response.body().string()
          Log.d(TAG, "xiaoliu checkUpdate success:" + body)
          if (body != null) {
            var json = ToolUtils.parseToJson(body)
            var code = json.getInt("code")
            if (code == 200) {
              var dataobj: JSONObject = new JSONObject(json.getString("data"))
              var fileurl = dataobj.getString("fileUrl")
              if(fileurl!=null){
                showUpdaloadDialog(fileurl);
              }
            }
          }
        }

        override def failed(call: Call, e: IOException): Unit = {

        }
      })
    }
  }

  import android.content.DialogInterface
  import com.github.shadowsocks.flyrouter.R

  /**
    * 显示更新对话框
    *
    * @param downloadUrl
    */
  private def showUpdaloadDialog(downloadUrl: String): Unit = { // 这里的属性可以一直设置，因为每次设置后返回的是一个builder对象
    val builder = new AlertDialog.Builder(this)
    // 设置提示框的标题
    builder.setTitle("版本升级").setIcon(R.mipmap.logo).// 设置提示框的图标
      setMessage("发现新版本！请及时更新").// 设置要显示的信息
      setPositiveButton("确定", new DialogInterface.OnClickListener() { // 设置确定按钮
    override def onClick(dialog: DialogInterface, which: Int): Unit =
    {
      startUpload(downloadUrl) //下载最新的版本程序

    }
  }

  ).setNegativeButton("取消", null) //设置取消按钮,null是什么都不做，并关闭对话框

    val alertDialog: AlertDialog = builder.create
    // 显示对话框
    alertDialog.show
}


  import android.app.ProgressDialog
  import android.widget.Toast

  /**
    * 开始下载
    *
    * @param downloadUrl 下载url
    */
  private def startUpload(downloadUrl: String): Unit = {
    progressDialog = new ProgressDialog(this)
    progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
    progressDialog.setMessage("正在下载新版本")
    progressDialog.setCancelable(false) //不能手动取消下载进度对话框

    val fileSavePath = ToolUtils.getSaveFilePath(downloadUrl)
    HTTPCaller.getInstance.downloadFile(downloadUrl, fileSavePath, null, new ProgressUIListener() {
      override def onUIProgressStart(totalBytes: Long): Unit = { //下载开始
        progressDialog.setMax(totalBytes.toInt)
        progressDialog.show
      } //更新进度
      override def onUIProgressChanged(numBytes: Long, totalBytes: Long, percent: Float, speed: Float): Unit = {
        progressDialog.setProgress(numBytes.toInt)
      }

      override def onUIProgressFinish(): Unit = { //下载完成
        Toast.makeText(getApplicationContext, "下载完成", Toast.LENGTH_LONG).show()
        progressDialog.dismiss
        openAPK(fileSavePath)
      }
    })
  }

  import android.content.Intent
  import android.net.Uri
  import android.os.Build
  import android.support.v4.content.FileProvider
  import java.io.File

  /**
    * 下载完成安装apk
    *
    * @param fileSavePath
    */
  private def openAPK(fileSavePath: String): Unit = {
    val file:File = new File(fileSavePath)
    val intent = new Intent(Intent.ACTION_VIEW)
    var data: Uri = null
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) { //判断版本大于等于7.0
      // "com.ansen.checkupdate.fileprovider"即是在清单文件中配置的authorities
      // 通过FileProvider创建一个content类型的Uri
      data = FileProvider.getUriForFile(this, "top.bitleo.checkupdate.fileprovider", file)
      intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // 给目标应用一个临时授权

    }
    else data = Uri.fromFile(file)
    intent.setDataAndType(data, "application/vnd.android.package-archive")
    startActivity(intent)
  }

  import android.content.pm.PackageManager

  override  def onRequestPermissionsResult(permsRequestCode: Int, permissions: Array[String], grantResults: Array[Int]): Unit = {
    permsRequestCode match {
      case PERMS_REQUEST_CODE =>
        val storageAccepted = grantResults(0) == PackageManager.PERMISSION_GRANTED
        if (storageAccepted) checkUpdate();
    }
  }



  private def hideCircle() {
    try {
      fabProgressCircle.hide()
    } catch {
      case _: java.lang.NullPointerException => // Ignore
    }
  }

  private def updateState(resetConnectionTest: Boolean = true) {
    if (bgService != null) {
      bgService.getState match {
        case State.CONNECTING =>
          fab.setBackgroundTintList(greyTint)
          serviceStarted = false
          fab.setImageResource(R.drawable.ic_start_busy)
          preferences.setEnabled(false)
          fabProgressCircle.show()
          stat.setVisibility(View.GONE)
        case State.CONNECTED =>
          fab.setBackgroundTintList(greenTint)
          serviceStarted = true
          fab.setImageResource(R.drawable.ic_start_connected)
          preferences.setEnabled(false)
          fabProgressCircle.postDelayed(hideCircle, 100)
          stat.setVisibility(View.VISIBLE)
          if (resetConnectionTest || state != State.CONNECTED)
            if (app.isNatEnabled) connectionTestText.setVisibility(View.GONE)
            else {
              connectionTestText.setVisibility(View.VISIBLE)
              connectionTestText.setText(getString(R.string.connection_test_pending))
            }
        case State.STOPPING =>
          fab.setBackgroundTintList(greyTint)
          serviceStarted = false
          fab.setImageResource(R.drawable.ic_start_busy)
          preferences.setEnabled(false)
          fabProgressCircle.show()
          stat.setVisibility(View.GONE)
        case _ =>
          fab.setBackgroundTintList(greyTint)
          serviceStarted = false
          fab.setImageResource(R.drawable.ic_start_idle)
          preferences.setEnabled(true)
          fabProgressCircle.postDelayed(hideCircle, 100)
          stat.setVisibility(View.GONE)
      }
      state = bgService.getState
    }
  }

  private def updateCurrentProfile() = {
    // Check if current profile changed
    if (preferences.profile == null || app.profileId != preferences.profile.id) {
      updatePreferenceScreen(app.currentProfile match {
        case Some(profile) => profile // updated
        case None => // removed
          var p:Profile =app.profileManager.getFirstProfile match {
            case Some(first) => first
            case None => null
          }
          if(p!=null){
            app.switchProfile(p.id)
          }
          p
      })

      if (serviceStarted) serviceLoad()

      true
    } else {
      preferences.refreshProfile()
      false
    }
  }

  protected override def onResume() {
    super.onResume()
    app.refreshContainerHolder
    updateState(updateCurrentProfile())
    syncRemainData();

  }

  private def syncRemainData(): Unit ={
    var requestJson  = SharedPrefsUtil.getValue(app,ToolUtils.SHARE_KEY,ToolUtils.LOCAL_BETA_JSON,"")
    if(!"".equals(requestJson)){
      var encodeJson = AESOperator.getInstance().encrypt(requestJson)
      NetUtils.getInstance().postDataAsynToNet(NetUtils.SELECT_INFO,encodeJson,new NetUtils.MyNetCall {
        override def success(call: Call, response: Response): Unit = {
          var body =response.body().string()
          Log.d(TAG, "xiaoliu syncRemainData success:"+body)
          if(body!=null){
            var json = ToolUtils.parseToJson(body)
            var code =json.getInt("code")
            if(code==200) {
              var dataobj: JSONObject = new JSONObject(json.getString("data"))
              var url = dataobj.getString("url")
              var remainFlow = dataobj.getString("remain")
              var remainTime = dataobj.getInt("effective_time")
              SharedPrefsUtil.putValue(app,ToolUtils.SHARE_KEY,ToolUtils.LOCAL_BETA_REMAIN_FLOW,remainFlow)
              SharedPrefsUtil.putValue(app,ToolUtils.SHARE_KEY,ToolUtils.LOCAL_BETA_REMAIN_DATE,remainTime)
              handler.post(()=>preferences.refreshExperience());
            }
          }
        }
        override def failed(call: Call, e: IOException): Unit = {

        }
      })
    }
  }

  private def updatePreferenceScreen(profile: Profile) {
    preferences.setProfile(profile)
  }

  override def onStart() {
    super.onStart()
    registerCallback
  }

  override def onStop() {
    super.onStop()
    unregisterCallback
    clearDialog()
  }

  private var _isDestroyed: Boolean = _

  override def isDestroyed = if (Build.VERSION.SDK_INT >= 17) super.isDestroyed else _isDestroyed

  override def onDestroy() {
    super.onDestroy()
    _isDestroyed = true
    detachService()
    new BackupManager(this).dataChanged()
    handler.removeCallbacksAndMessages(null)
  }

  def recovery() {
    if (serviceStarted) serviceStop()
    val h = showProgress(R.string.recovering)
    Utils.ThrowableFuture {
      app.copyAssets()
      h.sendEmptyMessage(0)
    }
  }

  def ignoreBatteryOptimization() {
    // TODO do . ignore_battery_optimization ......................................
    // http://blog.csdn.net/laxian2009/article/details/52474214

    var exception = false
    try {
      val powerManager: PowerManager = this.getSystemService(Context.POWER_SERVICE).asInstanceOf[PowerManager]
      val packageName = this.getPackageName
      val hasIgnored = powerManager.isIgnoringBatteryOptimizations(packageName)
      if (!hasIgnored) {
        val intent = new Intent()
        intent.setAction(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
        intent.setData(android.net.Uri.parse("package:" + packageName))
        startActivity(intent)
      }
      exception = false
    } catch {
      case _: Throwable =>
        exception = true
    } finally {
    }
    if (exception) {
      try {
        val intent = new Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)

        val cn = new ComponentName(
          "com.android.settings",
          "com.android.com.settings.Settings@HighPowerApplicationsActivity"
        )

        intent.setComponent(cn)
        startActivity(intent)

        exception = false
      } catch {
        case _: Throwable =>
          exception = true
      } finally {
      }
    }
  }

  override def onActivityResult(requestCode: Int, resultCode: Int, data: Intent) = resultCode match {
    case Activity.RESULT_OK =>
      serviceLoad()
    case _ =>
      cancelStart()
      Log.e(TAG, "Failed to start VpnService")

  }

  def serviceStop() {
    if (bgService != null) bgService.use(-1)
  }

  /** Called when connect button is clicked. */
  def serviceLoad() {
    bgService.use(app.profileId)

    if (app.isVpnEnabled) {
      changeSwitch(checked = false)
    }
  }

  def clearDialog() {
    if (progressDialog != null && progressDialog.isShowing) {
      if (!isDestroyed) progressDialog.dismiss()
      progressDialog = null
    }
  }
}
