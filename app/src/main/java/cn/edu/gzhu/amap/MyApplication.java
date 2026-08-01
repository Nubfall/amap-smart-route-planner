package cn.edu.gzhu.amap;

import android.app.Application;

import com.amap.api.maps.MapsInitializer;
import com.amap.api.services.core.ServiceSettings;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // 高德地图隐私合规
        MapsInitializer.updatePrivacyShow(this, true, true);
        MapsInitializer.updatePrivacyAgree(this, true);
        // 高德地图定位服务隐私合规
        ServiceSettings.updatePrivacyShow(this, true, true);
        ServiceSettings.updatePrivacyAgree(this, true);
    }
}
