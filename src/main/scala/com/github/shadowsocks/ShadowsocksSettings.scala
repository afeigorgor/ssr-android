package com.github.shadowsocks

import java.util.Locale

import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.{DialogInterface, Intent, SharedPreferences, _}
import android.net.Uri
import android.os.{Build, Bundle, Handler, Looper}
import java.io.{File, IOException}
import java.net.URL
import java.util.concurrent.TimeUnit

import android.preference.{Preference, PreferenceFragment, PreferenceGroup, SwitchPreference}
import android.support.v7.app.AlertDialog
import android.app.{Activity, ProgressDialog}
import android.text.TextUtils
import android.view.View
import android.widget._
import com.github.shadowsocks.ShadowsocksApplication.app
import com.github.shadowsocks.database.{Profile, SSRSub}
import com.github.shadowsocks.preferences._
import com.github.shadowsocks.utils._
import com.github.shadowsocks.utils.CloseUtils._
import android.util.{Base64, Log}
import android.util.Log
import okhttp3.{Call, OkHttpClient, Request, Response}
import org.json.JSONObject
import top.bitleo.http.{NetUtils, SystemUtil, ToolUtils}
import com.github.shadowsocks.flyrouter.R
import com.github.shadowsocks.flyrouter.BuildConfig

object ShadowsocksSettings {
  // Constants
  private final val TAG = "ShadowsocksSettings"
  private val PROXY_PREFS = Array(Key.group_name, Key.name)
  //, Key.host, Key.remotePort, Key.localPort, Key.password, Key.method,
  //    Key.protocol, Key.obfs, Key.obfs_param, Key.dns, Key.china_dns, Key.protocol_param
  private val FEATURE_PREFS = Array(Key.route,Key.scan_code,Key.select_experience)//, Key.proxyApps, Key.udpdns, Key.ipv6, Key.tfo


  // Helper functions
  def updateDropDownPreference(pref: Preference, value: String) {
    pref.asInstanceOf[DropDownPreference].setValue(value)
  }

  def updatePasswordEditTextPreference(pref: Preference, value: String) {
    pref.setSummary(value)
    pref.asInstanceOf[PasswordEditTextPreference].setText(value)
  }

  def updateNumberPickerPreference(pref: Preference, value: Int) {
    pref.asInstanceOf[NumberPickerPreference].setValue(value)
  }

  def updateSummaryEditTextPreference(pref: Preference, value: String) {
    pref.setSummary(value)
    pref.asInstanceOf[SummaryEditTextPreference].setText(value)
  }

  def updateSwitchPreference(pref: Preference, value: Boolean) {
    pref.asInstanceOf[SwitchPreference].setChecked(value)
  }

  def updatePreference(pref: Preference, name: String, profile: Profile) {
    if(profile==null){
      name match {
        case Key.group_name => updateSummaryEditTextPreference(pref, "")
        case Key.name => updateSummaryEditTextPreference(pref, "")
        case Key.route => updateDropDownPreference(pref, ToolUtils.DEFUALT_PASS)
        case _ => {}
      }
    }else{

      name match {
        case Key.group_name => updateSummaryEditTextPreference(pref, profile.url_group)
        case Key.name => updateSummaryEditTextPreference(pref, profile.name)
        case Key.remotePort => updateNumberPickerPreference(pref, profile.remotePort)
        case Key.localPort => updateNumberPickerPreference(pref, profile.localPort)
        case Key.password => updatePasswordEditTextPreference(pref, profile.password)
        case Key.method => updateDropDownPreference(pref, profile.method)
        case Key.protocol => updateDropDownPreference(pref, profile.protocol)
        case Key.protocol_param => updateSummaryEditTextPreference(pref, profile.protocol_param)
        case Key.obfs => updateDropDownPreference(pref, profile.obfs)
        case Key.obfs_param => updateSummaryEditTextPreference(pref, profile.obfs_param)
        case Key.route => updateDropDownPreference(pref, profile.route)
        case Key.proxyApps => updateSwitchPreference(pref, profile.proxyApps)
        case Key.udpdns => updateSwitchPreference(pref, profile.udpdns)
        case Key.dns => updateSummaryEditTextPreference(pref, profile.dns)
        case Key.china_dns => updateSummaryEditTextPreference(pref, profile.china_dns)
        case Key.ipv6 => updateSwitchPreference(pref, profile.ipv6)
        case _ => {}
      }
    }
  }
}

class ShadowsocksSettings extends PreferenceFragment with OnSharedPreferenceChangeListener {
  import ShadowsocksSettings._

  private val TAG =ShadowsocksSettings.getClass.getName
  private def activity = getActivity.asInstanceOf[Shadowsocks]
  lazy val natSwitch = findPreference(Key.isNAT).asInstanceOf[SwitchPreference]

  private var select_experience:Preference =_
  private var parent_experience:PreferenceGroup =_
  private var remain_flow:Preference =_
  private var remain_time:Preference =_
  private var experince_extend:String = _
  private var remainFlow:String =_
  private var remainTime:Integer =_


//  private var isProxyApps: SwitchPreference = _
  def qrcodeScan() {
    startActivityForResult(new Intent(this.getActivity, classOf[ScannerActivity]),0)
  }

  override def onActivityResult(requestCode: Int, resultCode: Int, data: Intent){
    resultCode match {
      case 1001=>
        startActivity(new Intent(this.getActivity, classOf[ProfileManagerActivity]))
      case _  =>
        Log.d(TAG,"no scan")
    }
  }

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    addPreferencesFromResource(R.xml.pref_all)
    getPreferenceManager.getSharedPreferences.registerOnSharedPreferenceChangeListener(this)
    experince_extend=""
    findPreference(Key.group_name).setOnPreferenceChangeListener((_, value) => {
      profile.url_group = value.asInstanceOf[String]
      app.profileManager.updateProfile(profile)
    })
    findPreference(Key.name).setOnPreferenceChangeListener((_, value) => {
      profile.name = value.asInstanceOf[String]
      app.profileManager.updateProfile(profile)
    })
//    findPreference(Key.host).setOnPreferenceClickListener((preference: Preference) => {
//      val HostEditText = new EditText(activity);
//      HostEditText.setText(profile.host);
//      new AlertDialog.Builder(activity)
//        .setTitle(getString(R.string.proxy))
//        .setPositiveButton(android.R.string.ok, ((_, _) => {
//          profile.host = HostEditText.getText().toString()
//          app.profileManager.updateProfile(profile)
//        }): DialogInterface.OnClickListener)
//        .setNegativeButton(android.R.string.no,  ((_, _) => {
//          setProfile(profile)
//        }): DialogInterface.OnClickListener)
//        .setView(HostEditText)
//        .create()
//        .show()
//      true
//    })
//    findPreference(Key.remotePort).setOnPreferenceChangeListener((_, value) => {
//      profile.remotePort = value.asInstanceOf[Int]
//      app.profileManager.updateProfile(profile)
//    })
//    findPreference(Key.localPort).setOnPreferenceChangeListener((_, value) => {
//      profile.localPort = value.asInstanceOf[Int]
//      app.profileManager.updateProfile(profile)
//    })
//    findPreference(Key.password).setOnPreferenceChangeListener((_, value) => {
//      profile.password = value.asInstanceOf[String]
//      app.profileManager.updateProfile(profile)
//    })
//    findPreference(Key.method).setOnPreferenceChangeListener((_, value) => {
//      profile.method = value.asInstanceOf[String]
//      app.profileManager.updateProfile(profile)
//    })
//    findPreference(Key.protocol).setOnPreferenceChangeListener((_, value) => {
//      profile.protocol = value.asInstanceOf[String]
//      app.profileManager.updateProfile(profile)
//    })
//    findPreference(Key.protocol_param).setOnPreferenceChangeListener((_, value) => {
//      profile.protocol_param = value.asInstanceOf[String]
//      app.profileManager.updateProfile(profile)
//    })
//    findPreference(Key.obfs).setOnPreferenceChangeListener((_, value) => {
//      profile.obfs = value.asInstanceOf[String]
//      app.profileManager.updateProfile(profile)
//    })
//    findPreference(Key.obfs_param).setOnPreferenceChangeListener((_, value) => {
//      profile.obfs_param = value.asInstanceOf[String]
//      app.profileManager.updateProfile(profile)
//    })
    findPreference(Key.route).setOnPreferenceChangeListener((_, value) => {
      if(value == "self") {
        val AclUrlEditText = new EditText(activity);
        AclUrlEditText.setText(getPreferenceManager.getSharedPreferences.getString(Key.aclurl, ""));
        new AlertDialog.Builder(activity)
          .setTitle(getString(R.string.acl_file))
          .setPositiveButton(android.R.string.ok, ((_, _) => {
            if(AclUrlEditText.getText().toString() == "")
            {
              setProfile(profile)
            }
            else
            {
              getPreferenceManager.getSharedPreferences.edit.putString(Key.aclurl, AclUrlEditText.getText().toString()).commit()
              downloadAcl(AclUrlEditText.getText().toString())
              app.profileManager.updateAllProfile_String(Key.route, value.asInstanceOf[String])
            }
          }): DialogInterface.OnClickListener)
          .setNegativeButton(android.R.string.no,  ((_, _) => {
            setProfile(profile)
          }): DialogInterface.OnClickListener)
          .setView(AclUrlEditText)
          .create()
          .show()
      }
      else {
        app.profileManager.updateAllProfile_String(Key.route, value.asInstanceOf[String])
      }

      true
    })

    findPreference(Key.scan_code).setOnPreferenceClickListener((preference: Preference) => {
      qrcodeScan()
      true
    })
    select_experience=findPreference(Key.select_experience)
    parent_experience =  findPreference(Key.parent_experience).asInstanceOf[PreferenceGroup]
    remain_flow = new Preference(this.getActivity);
    remain_time = new Preference(this.getActivity);
    remain_flow.setEnabled(false)
    remain_time.setEnabled(false)

    refreshExperience()
    select_experience.setOnPreferenceClickListener((preference: Preference) => {
      if(!SharedPrefsUtil.getValue(app,ToolUtils.SHARE_KEY,ToolUtils.LOCAL_SAVE_BETA_KEY,false)){
        ToolUtils.requestPermissionsReadPhoneState(this.getActivity);
        val experienceView = View.inflate(activity, R.layout.layout_experience, null);
        val card_number = experienceView.findViewById(R.id.card_number).asInstanceOf[EditText]
        val activation_code = experienceView.findViewById(R.id.activation_code).asInstanceOf[EditText]
//        card_number.setText("2018091195670438")
//        activation_code.setText("4AUGQFlnJOxz")
        new AlertDialog.Builder(activity)
          .setTitle(getString(R.string.title_experience).formatLocal(Locale.ENGLISH, BuildConfig.VERSION_NAME))
          .setNegativeButton(getString(android.R.string.cancel), null)
          .setPositiveButton(getString(android.R.string.ok),((_, _) => {

            var cardNumber = card_number.getText().toString;
            var cl:Long= 0
            if(!TextUtils.isEmpty(cardNumber)){
              try{
                cl=cardNumber.toLong
              }catch {
                case ex:Exception => cl = 0
              }
            }
            if(cl==0){
              ToolUtils.syncToast(getActivity,"卡号输入有误");
            }else{
              val activationCode = activation_code.getText().toString;
              ToolUtils.savaCardNumberAndCode(app,cl,activationCode);
              val jsonString = ToolUtils.getActiveJson(app);
              Log.d(TAG, "xiaoliu request :"+jsonString)
              val encodeString = ToolUtils.getAESEncode(jsonString);
              NetworkUtils.postActiveUser(encodeString,new NetworkUtils.ActiveUserNetCall {
                override def activeSuccess(flow: String, time: Int, url: String): Unit = {
                  remainFlow = flow
                  remainTime = time
                  if(url!=null){
                    refreshSSRURL(url,cl)
                  }else{
                    Log.e(this.getClass.getName,"xiaoliu the response card url is null")
                  }
                }
                override def activeFailed(msg: String): Unit = {
                  ToolUtils.asyncToast(getActivity,msg)
                }

                override def onException(exception: Exception): Unit = {

                }

              })




            }

          }): DialogInterface.OnClickListener)
          .setView(experienceView)
          .create()
          .show()
      }else{
        new AlertDialog.Builder(activity)
          .setTitle(getString(R.string.del_experience_tip).formatLocal(Locale.ENGLISH, BuildConfig.VERSION_NAME))
          .setNegativeButton(getString(android.R.string.cancel), null)
          .setPositiveButton(getString(android.R.string.ok),((_, _) => {
            val url_group = SharedPrefsUtil.getValue(app,ToolUtils.SHARE_KEY,ToolUtils.LOCAL_BETA_URL_GROUP,"none")
            val url = SharedPrefsUtil.getValue(app,ToolUtils.SHARE_KEY,ToolUtils.LOCAL_BETA_URL,"")
            Log.d(TAG,"xiaoliu url_grop:"+url_group+":url:"+url)
            var delete_profiles = app.profileManager.getAllProfilesByUrl(url) match {
              case Some(profiles) =>
                profiles
              case _ => null
            }
            delete_profiles.foreach((profile: Profile) => {
              Log.d(TAG,"xiaoliu delete_profiles:"+profile.id+":url:"+url+":profile_url:"+profile.url)
//                if(url.equals(profile.))
              if(url == profile.url){
                app.profileManager.delProfile(profile.id)
              }
            })

             app.ssrsubManager.getAllSSRSubs.get.foreach(s=>{
               if(s.url==url){
                 app.ssrsubManager.delSSRSub(s.id)
               }
             })

            SharedPrefsUtil.putValue(app,ToolUtils.SHARE_KEY,ToolUtils.LOCAL_SAVE_BETA_KEY,false)
            SharedPrefsUtil.putValue(app,ToolUtils.SHARE_KEY,ToolUtils.LOCAL_BETA_URL_GROUP,"none");
            SharedPrefsUtil.putValue(app,ToolUtils.SHARE_KEY,ToolUtils.LOCAL_BETA_URL,"");
            SharedPrefsUtil.putValue(app,ToolUtils.SHARE_KEY,ToolUtils.LOCAL_BETA_REMAIN_FLOW,"")
            SharedPrefsUtil.putValue(app,ToolUtils.SHARE_KEY,ToolUtils.LOCAL_BETA_REMAIN_DATE,0)

            val first:Profile =  app.profileManager.getFirstProfile match {
              case Some(p) =>
                app.profileId(p.id)
                p
              case None => null

            }
            setProfile(first)
            experince_extend =""
            refreshExperience()
          }):DialogInterface.OnClickListener)
          .create()
          .show()
      }
      true
    })


  }

  def refreshExperience(): Unit ={
    val flag =SharedPrefsUtil.getValue(app,ToolUtils.SHARE_KEY,ToolUtils.LOCAL_SAVE_BETA_KEY,false);
    Log.d(TAG,"xiaoliu refresh experience:"+flag)
    if(!flag){
      ToolUtils.removeCardNumberAndCode(app);
      select_experience.setTitle(getString(R.string.use_experience)+experince_extend)
      parent_experience.removePreference(remain_flow);
      parent_experience.removePreference(remain_time);
    }else{
      var requestJson = ToolUtils.getSyncRemainDataJson(app);
      Log.d(TAG,"xiaoliu requestJson:"+requestJson)
      if(requestJson!=null&&(!"".equals(requestJson))){
        var dataobj:JSONObject = new JSONObject(new JSONObject(requestJson).getString("data"))
        experince_extend = dataobj.getLong("card_number").toString
      }
      select_experience.setTitle(getString(R.string.del_experience)+"("+experince_extend+")")
      parent_experience.addPreference(remain_flow);
      parent_experience.addPreference(remain_time);
      refreshRemain()

    }
  }

  def refreshRemain(): Unit ={
    remainFlow=SharedPrefsUtil.getValue(app,ToolUtils.SHARE_KEY,ToolUtils.LOCAL_BETA_REMAIN_FLOW,"")
    remainTime=SharedPrefsUtil.getValue(app,ToolUtils.SHARE_KEY,ToolUtils.LOCAL_BETA_REMAIN_DATE,0)
    remain_flow.setTitle(getString(R.string.remain_flow)+remainFlow)
    remain_time.setTitle(getString(R.string.remain_time)+remainTime+ getString(R.string.days))
  }

  def refreshSSRURL(ssb_url : String,cardNumber:Long): Unit ={

    var result = ""
    val builder = new OkHttpClient.Builder()
      .connectTimeout(5, TimeUnit.SECONDS)
      .writeTimeout(5, TimeUnit.SECONDS)
      .readTimeout(5, TimeUnit.SECONDS)

    val client = builder.build();

    val request = new Request.Builder()
      .url(ssb_url)
      .build();

    try {
      val response = client.newCall(request).execute()
      val code = response.code()
      if (code == 200) {
        val response_string = new String(Base64.decode(response.body().string, Base64.URL_SAFE))
        val profiles_ssr_group = Parser.findAll_ssr(response_string).toList
        val ssrsub = new SSRSub {
          url =ssb_url;
          url_group = profiles_ssr_group(0).url_group
        }
        Log.d(TAG,"xiaoliu save url_group:"+ssrsub.url_group)
        var delete_profiles = app.profileManager.getAllProfilesByUrl(ssrsub.url) match {
          case Some(profiles) =>
            profiles
          case _ => null
        }

        var limit_num = -1
        var encounter_num = 0
        if (response_string.indexOf("MAX=") == 0) {
          limit_num = response_string.split("\\n")(0).split("MAX=")(1).replaceAll("\\D+","").toInt
        }
        Log.d(TAG,"xiaoliu parser response:"+response_string)
        var profiles_ssr = Parser.findAll_ssr(response_string)
        profiles_ssr = scala.util.Random.shuffle(profiles_ssr)

        profiles_ssr.foreach((profile: Profile) => {
          profile.url_group = ssrsub.url_group
          profile.url = ssrsub.url
          if (encounter_num < limit_num && limit_num != -1 || limit_num == -1) {
            val result = app.profileManager.createProfile_sub(profile)
            if (result != 0) {
              delete_profiles = delete_profiles.filter(_.id != result)
            }
          }
          encounter_num += 1
        })

        delete_profiles.foreach((profile: Profile) => {
          if (profile.url == ssrsub.url) {
            app.profileManager.delProfile(profile.id)
          }
        })

        var new_profiels:List[Profile] = app.profileManager.getAllProfilesByUrl(ssrsub.url) match {
          case Some(profiles) =>
            profiles
          case _ => null
        }
        if(new_profiels!=null&&new_profiels.length>0) {
          Log.d(TAG,"xiaoliu add ssr_beta:"+new_profiels.length)
          SharedPrefsUtil.putValue(app,ToolUtils.SHARE_KEY,ToolUtils.LOCAL_SAVE_BETA_KEY,true)
          SharedPrefsUtil.putValue(app,ToolUtils.SHARE_KEY,ToolUtils.LOCAL_BETA_URL_GROUP,ssrsub.url_group);
          SharedPrefsUtil.putValue(app,ToolUtils.SHARE_KEY,ToolUtils.LOCAL_BETA_URL,ssrsub.url);
          SharedPrefsUtil.putValue(app,ToolUtils.SHARE_KEY,ToolUtils.LOCAL_BETA_REMAIN_FLOW,remainFlow)
          SharedPrefsUtil.putValue(app,ToolUtils.SHARE_KEY,ToolUtils.LOCAL_BETA_REMAIN_DATE,remainTime)
          getActivity.runOnUiThread(new Runnable() {
            override def run(): Unit = {
              experince_extend = cardNumber.toString()
              app.ssrsubManager.createSSRSub(ssrsub);
              refreshExperience()
              app.switchProfile(new_profiels.head.id)
              setProfile(new_profiels.head)
              ToolUtils.asyncToast(getActivity,getString(R.string.sub_success))

            }
          })
        }else{

        }

      } else throw new Exception(getString(R.string.ssrsub_error, code: Integer))
      response.body().close()
    } catch {
      case e: IOException =>
        result = getString(R.string.ssrsub_error, e.getMessage)
    }

  }


  def downloadAcl(url: String) {
    val progressDialog = ProgressDialog.show(activity, getString(R.string.aclupdate), getString(R.string.aclupdate_downloading), false, false)
    new Thread {
      override def run() {
        Looper.prepare();
        try {
          IOUtils.writeString(app.getApplicationInfo.dataDir + '/' + "self.acl", autoClose(
            new URL(url).openConnection().getInputStream())(IOUtils.readString))
          progressDialog.dismiss()
          new AlertDialog.Builder(activity, R.style.Theme_Material_Dialog_Alert)
            .setTitle(getString(R.string.aclupdate))
            .setNegativeButton(android.R.string.yes, null)
            .setMessage(getString(R.string.aclupdate_successfully))
            .create()
            .show()
        } catch {
          case e: IOException =>
            e.printStackTrace()
            progressDialog.dismiss()
            new AlertDialog.Builder(activity, R.style.Theme_Material_Dialog_Alert)
              .setTitle(getString(R.string.aclupdate))
              .setNegativeButton(android.R.string.yes, null)
              .setMessage(getString(R.string.aclupdate_failed))
              .create()
              .show()
          case e: Exception =>  // unknown failures, probably shouldn't retry
            e.printStackTrace()
            progressDialog.dismiss()
            new AlertDialog.Builder(activity, R.style.Theme_Material_Dialog_Alert)
              .setTitle(getString(R.string.aclupdate))
              .setNegativeButton(android.R.string.yes, null)
              .setMessage(getString(R.string.aclupdate_failed))
              .create()
              .show()
        }
        Looper.loop();
      }
    }.start()
  }

  def refreshProfile() {
    profile = app.currentProfile match {
      case Some(p) => p
      case None =>
        app.profileManager.getFirstProfile match {
          case Some(p) =>
            app.profileId(p.id)
            p
          case None => null
        }
    }

//    isProxyApps.setChecked(profile.proxyApps)
  }

  override def onDestroy {
    super.onDestroy()
    app.settings.unregisterOnSharedPreferenceChangeListener(this)
  }

  def onSharedPreferenceChanged(pref: SharedPreferences, key: String) = key match {
    case Key.isNAT =>
      activity.handler.post(() => {
        activity.detachService
        activity.attachService
      })
    case _ =>
  }

  private var enabled = true
  def setEnabled(enabled: Boolean) {
    this.enabled = enabled
    for (name <- Key.isNAT #:: PROXY_PREFS.toStream #::: FEATURE_PREFS.toStream) {
      val pref = findPreference(name)
      if (pref != null) pref.setEnabled(enabled &&
        (name != Key.proxyApps || Utils.isLollipopOrAbove || app.isNatEnabled))
    }
  }

  var profile: Profile = _
  def setProfile(profile: Profile) {
    this.profile = profile
    for (name <- Array(PROXY_PREFS, FEATURE_PREFS).flatten) updatePreference(findPreference(name), name, profile)
  }
}
