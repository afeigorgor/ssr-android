package top.bitleo.http;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.telephony.TelephonyManager;
import android.widget.Toast;
import com.github.shadowsocks.AESOperator;
import org.json.JSONException;
import org.json.JSONObject;


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
    public static String getRecordJson(String requestJson,String ping,String ipOrDomain){

        try {
            JSONObject postObject =new JSONObject(requestJson);
            JSONObject dataobj = new JSONObject(postObject.getString("data"));
            dataobj.put("ping",ping);
            dataobj.put("ip_or_damain",ipOrDomain);
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

}
