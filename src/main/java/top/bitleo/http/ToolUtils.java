package top.bitleo.http;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Environment;
import android.widget.Toast;
import com.github.shadowsocks.AESOperator;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static android.graphics.Color.BLACK;
import static android.graphics.Color.WHITE;


public class ToolUtils {
    public static  final String SHARE_KEY ="shadowsock_leo";
    public static final String LOCAL_SAVE_BETA_KEY= "has_experience";
    public static final String LOCAL_BETA_URL_GROUP= "beta_url_group";
    public static final String LOCAL_BETA_URL= "beta_group";
    public static final String LOCAL_BETA_JSON= "beta_json";
    public static final String LOCAL_BETA_REMAIN_FLOW= "remain_flow";
    public static final String LOCAL_BETA_REMAIN_DATE= "remain_date";
    public static final String DEFUALT_PASS = "bypass-lan-china";
    public static final boolean LOCAL_TEST =true;
    public static  final  String TEST_SSR_URL= "https://s.vpnwifirouter.com/subscribe/rd4iYti-Z3vP6FuFK2fNK7ByaH6mpjTONqGXWhbGDvo";
//    public static  final  String TEST_SSR_URL= "https://fly.517.bz/subscribe/kELwxznF-V-aAUQRSTEJ3RdkWg0fGkE5Wb3PC3ns0Go";
    public static String getPackgeJson(String version,String imei,Long cardNumber,String activationCode){
        JSONObject object = new JSONObject();
        try {
            JSONObject object1=new JSONObject();
            object1.put("version",version);
            object1.put("platform","android");
            object1.put("imei",imei);
            object.put("commonData",object1);
            JSONObject object2 =new JSONObject();
            object2.put("card_number",cardNumber);
            object2.put("activation_code",activationCode);
            object.put("data",object2);
        } catch (JSONException e) {
            return null;
        }
        return object.toString();
    }

    public static String getCheckUpdateJson(String version,String imei){
        JSONObject object = new JSONObject();
        try {
            JSONObject object1=new JSONObject();
            object1.put("version",version);
            object1.put("platform","android");
            object1.put("imei",imei);
            object.put("commonData",object1);
        } catch (JSONException e) {
            return null;
        }
        return object.toString();
    }

    public static String getRecordJson(String requestJson,String ping,String ipOrDomain){

        try {
            JSONObject postObject =new JSONObject(requestJson);
            postObject.remove("activation_code");
            JSONObject dataobj = new JSONObject(postObject.getString("data"));
            JSONArray array = new JSONArray();
            JSONObject pingObj = new JSONObject();
            pingObj.put("ping",ping);
            pingObj.put("ip_or_damain",ipOrDomain);
            array.put(pingObj);
            dataobj.put("ping_list",array);
            postObject.put("data",dataobj);
            return postObject.toString();

        } catch (JSONException e) {
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

    public static void requestPermissionsReadPhoneState(Activity activity){
        int permissionCheck = activity.checkSelfPermission(Manifest.permission.READ_PHONE_STATE);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            activity.requestPermissions( new String[]{Manifest.permission.READ_PHONE_STATE}, 0);
        } else {
            //TODO
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
}
