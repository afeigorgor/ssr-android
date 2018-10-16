package com.github.shadowsocks.utils

import java.io.IOException
import java.lang.Exception

import android.content.Context
import android.os.Handler
import android.util.Log
import com.github.shadowsocks.Shadowsocks.TAG
import com.github.shadowsocks.ShadowsocksApplication.app
import com.github.shadowsocks.SharedPrefsUtil
import okhttp3.{Call, Response}
import org.json.JSONObject
import top.bitleo.http.{NetUtils, ToolUtils}

object NetworkUtils {

  val TAG:String = "NetworkUtils";

  def postRecordPing(encodePingValue:String,isNeedRePost:Boolean = false): Unit ={

    if(!isNeedRePost){
      NetUtils.getInstance().postDataAsynToNet(NetUtils.RECORD_PING,encodePingValue,new NetUtils.MyNetCall {
        override def success(call: Call, response: Response): Unit = {
          try{

            val body =response.body().string()
            Log.d(TAG, "xiaoliu ping request success:"+body)
            if(body != null){
              val json = ToolUtils.parseToJson(body)
              val code =json.getInt("code")
              if(code==404||code==500){
                Log.e(TAG, "xiaoliu ping request first faild:")
                postRecordPing(encodePingValue,true)
              }
            }
          }catch {
            case e:Exception => {
              Log.e(TAG, "xiaoliu ping request first faild:")
              postRecordPing(encodePingValue,true)
            };
          }
        }
        override def failed(call: Call, e: IOException): Unit = {
          Log.e(TAG, "xiaoliu ping request first faild:")
          postRecordPing(encodePingValue,true)

        }
      })


    }else{
      NetUtils.getInstance().postDataAsynToNet(NetUtils.RECORD_PING_OTHER,encodePingValue,new NetUtils.MyNetCall {
        override def success(call: Call, response: Response): Unit = {
          try{

            val body =response.body().string()
            Log.d(TAG, "xiaoliu ping request success:"+body)
            if(body != null){
              val json = ToolUtils.parseToJson(body)
              val code =json.getInt("code")
              if(code==404||code==500){
                Log.e(TAG, "xiaoliu ping request all faild:")
              }
            }
          }catch {
            case e:Exception => {
              Log.e(TAG, "xiaoliu ping request all faild:")
            };
          }
        }
        override def failed(call: Call, e: IOException): Unit = {
          Log.e(TAG, "xiaoliu ping request first faild:")
        }
      })
    }
  }

  /**
    * 自定义checkupdate回调接口
    */
  trait CheckUpdateNetCall {
    @throws[IOException]
    def needUpdate(fileUrl:String): Unit
    def onException(exception: Exception): Unit
  }

  def postCheckUpdate(encodeJson:String, callback:CheckUpdateNetCall,isNeedRePost:Boolean = false): Unit ={
    if(!isNeedRePost){
      NetUtils.getInstance().postDataAsynToNet(NetUtils.APP_UPDATE, encodeJson, new NetUtils.MyNetCall {
        override def success(call: Call, response: Response): Unit = {

          try{
            val body = response.body().string()
            Log.d(TAG, "xiaoliu checkUpdate success:" + body)
            if (body != null) {
              val json = ToolUtils.parseToJson(body)
              val code = json.getInt("code")
              if (code == 200) {
                val dataobj: JSONObject = new JSONObject(json.getString("data"))
                val fileurl = dataobj.getString("fileUrl")
                if(fileurl!=null&&callback!=null){
                  callback.needUpdate(fileurl);
                }
              }else if(code==404||code==500){
                Log.e(TAG, "xiaoliu checkUpdate failed first request:")
                postCheckUpdate(encodeJson,callback,true);
              }
            }
          }catch {
            case e:Exception=>{
              if(callback!=null){
                Log.e(TAG, "xiaoliu checkUpdate failed first request:")
                postCheckUpdate(encodeJson,callback,true);
              }
            }
          }
        }

        override def failed(call: Call, e: IOException): Unit = {
          Log.e(TAG, "xiaoliu checkUpdate failed first request:")
          postCheckUpdate(encodeJson,callback,true);

        }
      })
    }else{
      NetUtils.getInstance().postDataAsynToNet(NetUtils.APP_UPDATE_OTHER, encodeJson, new NetUtils.MyNetCall {
        override def success(call: Call, response: Response): Unit = {
          try{
            val body = response.body().string()
            Log.d(TAG, "xiaoliu checkUpdate success:" + body)
            if (body != null) {
              val json = ToolUtils.parseToJson(body)
              val code = json.getInt("code")
              if (code == 200) {
                val dataobj: JSONObject = new JSONObject(json.getString("data"))
                val fileurl = dataobj.getString("fileUrl")
                if(fileurl!=null&&callback!=null){
                  callback.needUpdate(fileurl);
                }
              }else if(code==404||code==500){
                Log.e(TAG, "xiaoliu checkUpdate failed all request:")
              }
            }
          }catch {
            case e:Exception=>{
              if(callback!=null){
                Log.e(TAG, "xiaoliu checkUpdate failed all request:")
                if(callback!=null){
                  callback.onException(e);
                }
              }
            }
          }
        }

        override def failed(call: Call, e: IOException): Unit = {
          Log.e(TAG, "xiaoliu checkUpdate failed all request:")
          if(callback!=null){
            callback.onException(e);
          }
        }
      })
    }

  }



  /**
    * 自定义syncremain回调接口
    */
  trait SyncRemainNetCall {
    @throws[IOException]
    def needUpdateUI(): Unit
    def onException(exception: Exception): Unit
  }


  def postSyncRemainData(ctx:Context,encodeJson:String, callback:SyncRemainNetCall,isNeedRePost:Boolean = false): Unit = {
    if (!isNeedRePost) {
      NetUtils.getInstance().postDataAsynToNet(NetUtils.SELECT_INFO, encodeJson, new NetUtils.MyNetCall {
        override def success(call: Call, response: Response): Unit = {
          try {
            val body = response.body().string()
            Log.d(TAG, "xiaoliu syncRemainData success:" + body)
            if (body != null) {
              val json = ToolUtils.parseToJson(body)
              val code = json.getInt("code")
              if (code == 200) {
                val dataobj: JSONObject = new JSONObject(json.getString("data"))
                var url = dataobj.getString("url")
                val remainFlow = dataobj.getString("remain")
                val remainTime = dataobj.getInt("effective_time")
                SharedPrefsUtil.putValue(ctx, ToolUtils.SHARE_KEY, ToolUtils.LOCAL_BETA_REMAIN_FLOW, remainFlow)
                SharedPrefsUtil.putValue(ctx, ToolUtils.SHARE_KEY, ToolUtils.LOCAL_BETA_REMAIN_DATE, remainTime)
                if(callback!=null){
                  callback.needUpdateUI();
                }
              } else if (code == 404 || code == 500) {
                Log.e(TAG, "xiaoliu syncRemainData first faild:")
                postSyncRemainData(ctx, encodeJson, callback, true);
              }
            }
          } catch {
            case e: Exception => {
              Log.e(TAG, "xiaoliu syncRemainData first faild:")
              postSyncRemainData(ctx, encodeJson, callback, true);
            }
          }
        }

        override def failed(call: Call, e: IOException): Unit = {
          Log.e(TAG, "xiaoliu syncRemainData first faild:")
          postSyncRemainData(ctx, encodeJson, callback, true);
        }
      })
    } else {
      NetUtils.getInstance().postDataAsynToNet(NetUtils.SELECT_INFO_OTHER, encodeJson, new NetUtils.MyNetCall {
        override def success(call: Call, response: Response): Unit = {
          try {
            val body = response.body().string()
            Log.d(TAG, "xiaoliu syncRemainData success:" + body)
            if (body != null) {
              val json = ToolUtils.parseToJson(body)
              val code = json.getInt("code")
              if (code == 200) {
                val dataobj: JSONObject = new JSONObject(json.getString("data"))
                var url = dataobj.getString("url")
                val remainFlow = dataobj.getString("remain")
                val remainTime = dataobj.getInt("effective_time")
                SharedPrefsUtil.putValue(ctx, ToolUtils.SHARE_KEY, ToolUtils.LOCAL_BETA_REMAIN_FLOW, remainFlow)
                SharedPrefsUtil.putValue(ctx, ToolUtils.SHARE_KEY, ToolUtils.LOCAL_BETA_REMAIN_DATE, remainTime)
                if(callback!=null){
                  callback.needUpdateUI();
                }
              } else if (code == 404 || code == 500) {
                Log.e(TAG, "xiaoliu syncRemainData all faild:")
              }
            }
          } catch {
            case e: Exception => {
              Log.e(TAG, "xiaoliu syncRemainData all faild:")
              if(callback!=null){
                callback.onException(e);
              }

            }
          }
        }

        override def failed(call: Call, e: IOException): Unit = {
          Log.e(TAG, "xiaoliu syncRemainData all faild:")
          if(callback!=null){
            callback.onException(e);
          }
        }
      })

    }
  }



  /**
    * 自定义syncremain回调接口
    */
  trait ActiveUserNetCall {
    @throws[IOException]
    def activeSuccess(flow:String,time:Int,url:String): Unit
    def activeFailed(msg:String): Unit
    def onException(exception: Exception): Unit
  }

  def postActiveUser(encodeString:String,callback:ActiveUserNetCall,isNeedRePost:Boolean = false): Unit ={

    if(!isNeedRePost){
      NetUtils.getInstance().postDataAsynToNet(NetUtils.ACTIVE_USER,encodeString,new NetUtils.MyNetCall {
        override def success(call: Call, response: Response): Unit = {
          try{

            var body =response.body().string()
            Log.d(TAG, "xiaoliu success:"+body)
            if(body!=null){
              var json = ToolUtils.parseToJson(body)
              var code =json.getInt("code")
              if(code==200){
                val dataobj:JSONObject = new JSONObject(json.getString("data"))
                val url = dataobj.getString("url")
                val remainFlow = dataobj.getString("remain")
                val remainTime = dataobj.getInt("effective_time")
                if(callback!=null){
                  callback.activeSuccess(remainFlow,remainTime,url);
                }
              }else if(code == -1){
                NetUtils.getInstance().postDataAsynToNet(NetUtils.SELECT_INFO,encodeString,new NetUtils.MyNetCall {
                  override def success(call: Call, response: Response): Unit = {
                    try{
                      val new_body =response.body().string()
                      Log.d(TAG, "xiaoliu postActiveUser selectinfo success:"+new_body)
                      if(new_body!=null){
                        val new_json = ToolUtils.parseToJson(new_body)
                        val new_code =new_json.getInt("code")
                        if(new_code==200) {
                          val new_dataobj: JSONObject = new JSONObject(new_json.getString("data"))
                          val new_url = new_dataobj.getString("url")
                          val remainFlow = new_dataobj.getString("remain")
                          val remainTime = new_dataobj.getInt("effective_time")
                          if(callback!=null){
                            callback.activeSuccess(remainFlow,remainTime,new_url);
                          }
                        }else if(new_code == -1){
                          if(callback!=null){
                            callback.activeFailed(new_json.getString("msg"))
                          }
                        }else if(new_code==404||new_code==500){
                          Log.e(TAG, "xiaoliu active first faild:")
                          postActiveUser(encodeString,callback,true)
                        }
                      }

                    }catch {
                      case e: Exception => {
                        Log.e(TAG, "xiaoliu active first faild:")
                        postActiveUser(encodeString,callback,true)
                      }
                    }
                  }
                  override def failed(call: Call, e: IOException): Unit = {
                    Log.e(TAG, "xiaoliu active first faild:")
                    postActiveUser(encodeString,callback,true)
                  }
                })

              }else if(code==404||code ==500){
                 postActiveUser(encodeString,callback,true)
              }

            }
          }catch {
            case e: Exception => {
              Log.e(TAG, "xiaoliu active first faild:")
              postActiveUser(encodeString,callback,true)
            }
          }
        }

        override def failed(call: Call, e: IOException): Unit = {
          Log.e(TAG, "xiaoliu active first failed")
          postActiveUser(encodeString,callback,true)
        }
      })
    }else{
      NetUtils.getInstance().postDataAsynToNet(NetUtils.ACTIVE_USER_OTHER,encodeString,new NetUtils.MyNetCall {
        override def success(call: Call, response: Response): Unit = {
          try{

            var body =response.body().string()
            Log.d(TAG, "xiaoliu second success:"+body)
            if(body!=null){
              var json = ToolUtils.parseToJson(body)
              var code =json.getInt("code")
              if(code==200){
                val dataobj:JSONObject = new JSONObject(json.getString("data"))
                val url = dataobj.getString("url")
                val remainFlow = dataobj.getString("remain")
                val remainTime = dataobj.getInt("effective_time")
                if(callback!=null){
                  callback.activeSuccess(remainFlow,remainTime,url);
                }
              }else if(code == -1){
                NetUtils.getInstance().postDataAsynToNet(NetUtils.SELECT_INFO_OTHER,encodeString,new NetUtils.MyNetCall {
                  override def success(call: Call, response: Response): Unit = {
                    try{
                      val new_body =response.body().string()
                      Log.d(TAG, "xiaoliu postActiveUser second selectinfo success:"+new_body)
                      if(new_body!=null){
                        val new_json = ToolUtils.parseToJson(new_body)
                        val new_code =new_json.getInt("code")
                        if(new_code==200) {
                          val new_dataobj: JSONObject = new JSONObject(new_json.getString("data"))
                          val new_url = new_dataobj.getString("url")
                          val remainFlow = new_dataobj.getString("remain")
                          val remainTime = new_dataobj.getInt("effective_time")
                          if(callback!=null){
                            callback.activeSuccess(remainFlow,remainTime,new_url);
                          }
                        }else if(new_code == -1){
                          if(callback!=null){
                            callback.activeFailed(new_json.getString("msg"))
                          }
                        }else if(new_code==404||new_code==500){
                          Log.e(TAG, "xiaoliu active all faild:")
                          if(callback!=null){
                            callback.onException(null);
                          }
                        }
                      }

                    }catch {
                      case e: Exception => {
                        Log.e(TAG, "xiaoliu active all faild:")
                        if(callback!=null){
                          callback.onException(e);
                        }
                      }
                    }
                  }
                  override def failed(call: Call, e: IOException): Unit = {
                    Log.e(TAG, "xiaoliu active all faild:")
                    if(callback!=null){
                      callback.onException(e);
                    }
                  }
                })

              }else if(code==404||code ==500){
                if(callback!=null){
                  callback.onException(null);
                }
              }

            }
          }catch {
            case e: Exception => {
              Log.e(TAG, "xiaoliu active all faild:")
              if(callback!=null){
                callback.onException(e);
              }
            }
          }
        }

        override def failed(call: Call, e: IOException): Unit = {
          Log.e(TAG, "xiaoliu active all failed")
          if(callback!=null){
            callback.onException(e);
          }
        }
      })
    }
  }

}
