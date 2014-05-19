package paypal;
import java.util.HashMap;
import java.util.Map;

/**
 *  For a full list of configuration parameters refer in wiki page.(https://github.com/paypal/sdk-core-java/wiki/SDK-Configuration-Parameters) 
 */
public class Configuration {

    // Creates a configuration map containing credentials and other required configuration parameters.
    public static Map<String,String> getAcctAndConfig(){
        Map<String,String> configMap = new HashMap<String,String>();
        configMap.putAll(getConfig());

        // Account Credential
        configMap.put("acct1.UserName", "li.ming116+paypal-facilitator_api1.gmail.com");
        configMap.put("acct1.Password", "1400195683");
        configMap.put("acct1.Signature", "Aa2txZh906MBr.lqjcn.t3RDhCcYAbT9q5imDsRtTW-cSriontWVNKlB");
        // Subject is optional, only required in case of third party permission
        //configMap.put("acct1.Subject", "");

        // Sample Certificate credential
        // configMap.put("acct2.UserName", "certuser_biz_api1.paypal.com");
        // configMap.put("acct2.Password", "D6JNKKULHN3G5B8A");
        // configMap.put("acct2.CertKey", "password");
        // configMap.put("acct2.CertPath", "resource/sdk-cert.p12");
        // configMap.put("acct2.AppId", "APP-80W284485P519543T");

        return configMap;
    }

    public static final Map<String,String> getConfig(){
        Map<String,String> configMap = new HashMap<String,String>();

        // Endpoints are varied depending on whether sandbox OR live is chosen for mode
        configMap.put("mode", "sandbox");


        // These values are defaulted in SDK. If you want to override default values, uncomment it and add your value.
        // configMap.put("http.ConnectionTimeOut", "5000");
        // configMap.put("http.Retry", "2");
        // configMap.put("http.ReadTimeOut", "30000");
        // configMap.put("http.MaxConnection", "100");
        return configMap;
    }
}