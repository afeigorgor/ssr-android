package top.bitleo.http;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;
import com.github.shadowsocks.AESOperator;
import com.github.shadowsocks.SharedPrefsUtil;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import static android.graphics.Color.BLACK;
import static android.graphics.Color.WHITE;


public class ToolUtils {
    public static  final String SHARE_KEY ="shadowsock_leo";
    public static final String LOCAL_SAVE_BETA_KEY= "has_experience";
    public static final String LOCAL_BETA_URL_GROUP= "beta_url_group";
    public static final String LOCAL_BETA_URL= "beta_group";
    public static final String LOCAL_COMMON_DATA_JSON= "common_data_json";
    public static final String LOCAL_CARD_INFO_JSON= "card_info_json";
    public static final String LOCAL_BETA_REMAIN_FLOW= "remain_flow";
    public static final String LOCAL_BETA_REMAIN_DATE= "remain_date";
    public static final String DEFUALT_PASS = "bypass-lan-china";
    public static final String COMMON_DATA_KEY = "commonData";
    public static final String DATA_KEY = "data";


    public static String getActiveJson(Context ctx){
        JSONObject object = new JSONObject();
        String commonData = SharedPrefsUtil.getValue(ctx,ToolUtils.SHARE_KEY,ToolUtils.LOCAL_COMMON_DATA_JSON,"");
        String cardInfoData =  SharedPrefsUtil.getValue(ctx,ToolUtils.SHARE_KEY,ToolUtils.LOCAL_CARD_INFO_JSON,"");
        try {
            JSONObject object1=new JSONObject(commonData);
            object.put(COMMON_DATA_KEY,object1);
            JSONObject object2 =new JSONObject(cardInfoData);
            object.put(DATA_KEY,object2);
        } catch (Exception e) {
            return null;
        }
        return object.toString();
    }

    public static String getCheckUpdateJson(Context ctx){
        JSONObject object = new JSONObject();
        String commonData = SharedPrefsUtil.getValue(ctx,ToolUtils.SHARE_KEY,ToolUtils.LOCAL_COMMON_DATA_JSON,"");
        try {
            JSONObject object1=new JSONObject(commonData);
            object.put(COMMON_DATA_KEY,object1);
        } catch (Exception e) {
            return null;
        }
        return object.toString();
    }

    public static String getSyncRemainDataJson(Context ctx){
        return getActiveJson(ctx);
    }


    public static void requestPermissionsReadPhoneState(Activity activity){

        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1){

            int permissionCheck = activity.checkSelfPermission(Manifest.permission.READ_PHONE_STATE);

            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                activity.requestPermissions( new String[]{Manifest.permission.READ_PHONE_STATE}, 0);
            } else {
                //TODO
            }
        }

    }

    public static String getRecordJson(String requestJson,String ping,String ipOrDomain){

        try {
            JSONObject postObject =new JSONObject(requestJson);
            JSONObject dataobj = new JSONObject(postObject.getString("data"));
            JSONArray array = new JSONArray();
            JSONObject pingObj = new JSONObject();
            pingObj.put("ping",ping);
            pingObj.put("ip_or_damain",ipOrDomain);
            array.put(pingObj);
            dataobj.put("ping_list",array);
            dataobj.remove("activation_code");
            postObject.put(DATA_KEY,dataobj);
            return postObject.toString();

        } catch (Exception e) {
            return null;
        }
    }

    public static String getRecordJsonFromList(String requestJson,List<PingInfo> pingInfos){

        try {
            JSONObject postObject =new JSONObject(requestJson);
            JSONObject dataobj = new JSONObject(postObject.getString("data"));
            JSONArray array = new JSONArray();
            for(PingInfo p:pingInfos){
                JSONObject pingObj = new JSONObject();
                pingObj.put("ping",p.elapsed);
                pingObj.put("ip_or_damain",p.host);
                array.put(pingObj);
            }
            dataobj.put("ping_list",array);
            dataobj.remove("activation_code");
            postObject.put(DATA_KEY,dataobj);
            return postObject.toString();

        } catch (Exception e) {
            return null;
        }
    }

    public static String getAESEncode(String jsonString){
        try {
            String encodeString =AESOperator.getInstance().encrypt(jsonString);
            return encodeString;
        } catch (Exception e) {
            return null;
        }
    }

    public static JSONObject parseToJson(String str){

        try {
            JSONObject object1 = new JSONObject(str);
            return object1;
        }catch(Exception e){
            return null;
        }
    }

    public static void asyncToast(final Activity getActivity, final String msg){
        getActivity.runOnUiThread(new Runnable() {
            @Override
            public void run(){
                Toast.makeText(getActivity,msg, Toast.LENGTH_SHORT).show();
            }
        });
    }
    public static void syncToast(final Activity getActivity, final String msg){
        Toast.makeText(getActivity,msg, Toast.LENGTH_SHORT).show();
    }

    //生成二维码
    public static Bitmap createBitmap(String str,int width,int height){
        Bitmap bitmap = null;
        BitMatrix matrix = null;
        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
        try {
            matrix = multiFormatWriter.encode(str, BarcodeFormat.QR_CODE, width, height);

            int[] pixels = new int[width * height];

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (matrix.get(x, y)) {
                        pixels[y * width + x] = BLACK;
                    }else{
                        pixels[y * width + x] = WHITE;
                    }
                }
            }
            bitmap = Bitmap.createBitmap(width, height,Bitmap.Config.RGB_565);
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        } catch (WriterException e){
            e.printStackTrace();
        } catch (IllegalArgumentException iae){ // ?
            return null;
        }
        return bitmap;
    }


    /**
     * 获取文件保存路径 sdcard根目录/download/文件名称
     * @param fileUrl
     * @return
     */
    public static String getSaveFilePath(String fileUrl){
        String fileName=fileUrl.substring(fileUrl.lastIndexOf("/")+1,fileUrl.length());//获取文件名称
        String newFilePath= Environment.getExternalStorageDirectory() + "/Download/"+fileName;
        return newFilePath;
    }

    /**
     * 获取APP版本号
     * @param ctx
     * @return
     */
    public static int getVersionCode(Context ctx) {
        // 获取packagemanager的实例
        int version = 0;
        try {
            PackageManager packageManager = ctx.getPackageManager();
            //getPackageName()是你当前程序的包名
            PackageInfo packInfo = packageManager.getPackageInfo(ctx.getPackageName(), 0);
            version = packInfo.versionCode;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return version;
    }

    /**
     * 获取APP版本号
     * @param ctx
     * @return
     */
    public static String getVersionName(Context ctx) {
        // 获取packagemanager的实例
        String versionName = "";
        try {
            PackageManager packageManager = ctx.getPackageManager();
            //getPackageName()是你当前程序的包名
            PackageInfo packInfo = packageManager.getPackageInfo(ctx.getPackageName(), 0);
            versionName = packInfo.versionName;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return versionName;
    }

    /**
     * 初始化公共数据，每次应用创建时调用
     * @param ctx
     */
    public static   String initCommonData(Context ctx){
        String version =getVersionName(ctx);
        String platform = "android";
        String systemVersion = SystemUtil.getSystemVersion();
        String imei = SystemUtil.getIMEI(ctx);
        JSONObject object1=new JSONObject();
        try {
            object1.put("version",version);
            object1.put("platform",platform);
            object1.put("systemVersion",systemVersion);
            object1.put("imei",imei);
            String json = object1.toString();
            SharedPrefsUtil.putValue(ctx,ToolUtils.SHARE_KEY,ToolUtils.LOCAL_COMMON_DATA_JSON,json);
            return json;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public  static  String savaCardNumberAndCode(Context ctx,Long cardNumber,String activationCode){
        JSONObject object2 =new JSONObject();
        try {
            object2.put("card_number",cardNumber);
            object2.put("activation_code",activationCode);
            String json  = object2.toString();
            SharedPrefsUtil.putValue(ctx,ToolUtils.SHARE_KEY,ToolUtils.LOCAL_CARD_INFO_JSON,json);
            return json;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public  static  void removeCardNumberAndCode(Context ctx){
        SharedPrefsUtil.putValue(ctx,ToolUtils.SHARE_KEY,ToolUtils.LOCAL_CARD_INFO_JSON,"");
    }

    public  static Long getCardNumber(Context ctx){
        String cardInfoData =  SharedPrefsUtil.getValue(ctx,ToolUtils.SHARE_KEY,ToolUtils.LOCAL_CARD_INFO_JSON,"");
        try {
            JSONObject object =new JSONObject(cardInfoData);
            Long cardNumber =object.getLong("card_number");
            return cardNumber;

        } catch (Exception e) {
            return null;
        }

    }

    public  static String  getUploadPingInfoJson(Context ctx,List<PingInfo> pingInfos){
        String requestJson = ToolUtils.getSyncRemainDataJson(ctx);
        String recordJson = getRecordJsonFromList(requestJson,pingInfos);
        String encodePingValue = null;
        try {
            Log.d("ToolUtils","xiaoliu getUploadPingInfoJson:"+recordJson);
            encodePingValue = AESOperator.getInstance().encrypt(recordJson);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return encodePingValue;
    }
}
