package kr.co.lion.android01.navermapuse

import android.Manifest
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraAnimation
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.LocationTrackingMode
import com.naver.maps.map.MapFragment
import com.naver.maps.map.NaverMap
import com.naver.maps.map.NaverMapSdk
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.util.FusedLocationSource
import kr.co.lion.android01.navermapuse.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity()  {

    lateinit var activityMainBinding: ActivityMainBinding

    //확인할 권한 목록
    var permissionList = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    //위치 정보를 관리하는 객체
    lateinit var locationManager:LocationManager

    lateinit var locationSource: FusedLocationSource


    //네이버 지도 객체를 담을 프러퍼티
    lateinit var naverMap:NaverMap

    //위치 측정이 성공하면 동작할 리스너
    var gpsLocationListener:MyLocationListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        activityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(activityMainBinding.root)

        requestPermissions(permissionList, 0)


        setToolBar()
        setNaverMap()

        NaverMapSdk.getInstance(this).client =
            NaverMapSdk.NaverCloudPlatformClient("knvgeuyz96")
    }


    //툴바 설정
    fun setToolBar(){
        activityMainBinding.apply {
            materialToolbar.apply {
                title = "네이버 지도"
                //메뉴
                inflateMenu(R.menu.main_menu)
                //클릭?
                setOnMenuItemClickListener {
                    when(it.itemId){
                        R.id.my_Location -> {
                            getMyLocation()

                        }
                    }

                    true
                }
            }
        }
    }

    //네이버 지도 세팅
    fun setNaverMap(){
        //MapFragment의 객체를 가져온다
        var fm = supportFragmentManager
        var mapFragment = fm.findFragmentById(R.id.map_fragment) as MapFragment?
            ?:MapFragment.newInstance().also {
                fm.beginTransaction().add(R.id.map_fragment, it).commit()
            }
        mapFragment.getMapAsync {
            //Snackbar.make(activityMainBinding.root, "네이버 지도 탄생", Snackbar.LENGTH_SHORT).show()

            //네이버 객체를 담아준다
            naverMap = it

            locationSource = FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE)

            naverMap.locationSource = locationSource
            naverMap.uiSettings.isLocationButtonEnabled = true
            naverMap.locationTrackingMode = LocationTrackingMode.Face

            //맵을 클릭했을때
            naverMap.setOnMapClickListener { point, latLng ->
                var dialog = MaterialAlertDialogBuilder(this)
                dialog.setTitle("지점 표시")
                dialog.setMessage("이 지점을 표시하시겠습니까?")
                dialog.setPositiveButton("확인"){ dialogInterface: DialogInterface, i: Int ->
                    //마커 추가
                    val maker = Marker()
                    maker.position = latLng
                    maker.map = naverMap
                }
                dialog.setNegativeButton("취소", null)
                dialog.show()
            }

            //위치 정보를 관리하는 객체를 가져온다
            locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

            //단말기에 저장되어 있는 위치값을 가져온다
            //둘 중 하나라도 허용일 경우 사용 아니면 만다
            var a1 = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            var a2 = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

            //모든 권한이 허용되어 있다면
            if (a1 && a2){
                var location1 = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)


                //현재 위치를 지도에 표시한다
                if (location1 != null) {
                    setMyLocation(location1)
                }

                //현재 위치를 측정한다
                getMyLocation()
            }
        }
    }
    //위치 측정이 성공하면 동작하는 리스너
    inner class MyLocationListener : LocationListener{
        override fun onLocationChanged(location: Location) {
            //사용한 위치 정보 프로바이더로 분기한다
            when(location.provider){
                //Gps라면
                LocationManager.GPS_PROVIDER -> {
                    //Gps 리스너 연결을 해제해준다
                    locationManager.removeUpdates(gpsLocationListener!!)
                    gpsLocationListener = null
                }
            }
            //측정한 위치로 지도를 움직인다
            setMyLocation(location)
        }

    }

    //현재 나의 위치를 가져오는 메서드
    //gps도 돌리고 network도 돌린다
    fun getMyLocation(){
        //위치 정보 사용 권한 허용여부
        var a1 = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED

        if (a1 == true){
            return
        }

        //둘 중에 GPS가 사용이 가능하다면
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) == true){
            if (gpsLocationListener == null){
                gpsLocationListener = MyLocationListener()
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0.0f, gpsLocationListener!!)
            }
        }

    }

    //지도의 위치를 설정하는 메서드
    fun setMyLocation(location:Location) {
        //위도와 경도를 출력한다
        Snackbar.make(activityMainBinding.root, "위도 : ${location.latitude}, 경도 : ${location.longitude}", Snackbar.LENGTH_SHORT).show()

        //Log.d("test1234", "${location.latitude}")
        //위도와 경도를 관리하는 객체를 생성한다
        var userLocation = LatLng(location.latitude, location.longitude)

        naverMap.cameraPosition = CameraPosition(LatLng(location.latitude, location.longitude), 16.0)

        //지도를 이동시키기 위한 객체를 생성한다
        var cameraUpdate = CameraUpdate.scrollTo(userLocation).animate(CameraAnimation.Easing)

        //카메라를 이동시킨다
        naverMap.moveCamera(cameraUpdate)

    }

    companion object{
        val LOCATION_PERMISSION_REQUEST_CODE = 1000
    }

}







//이 오류는 java.lang.NullPointerException입니다. 이것은 Kotlin 또는 Java에서 가장 흔한 예외 중 하나로, 객체가 Null을 참조하고 있는 상태에서 해당 객체의 메소드나 속성을 호출할 때 발생합니다.
//
//로그를 보면 오류가 MainActivity.kt 파일의 getMyLocation 함수에서 발생했다고 합니다. 이 함수에서 Null Pointer Exception이 발생한 것으로 보입니다.
//
//이 오류를 해결하기 위해서는 해당 함수에서 Null을 참조하고 있는지 확인해야 합니다. MainActivity.kt 파일의 168번째 줄을 살펴보고 해당 줄에서 Null을 참조하고 있는 경우, 해당 객체가 제대로 초기화되었는지 또는 Null을 체크하고 안전하게 접근하고 있는지 확인해야 합니다.
//
//또한, 발생한 상황에서 오류를 유발하는 이벤트나 메소드 호출이 어떤 것인지 파악하여 해당 부분을 수정할 필요가 있습니다. 오류가 발생한 시점에서 위치 정보 관련 코드가 동작하고 있는 것으로 보이므로, 해당 위치 관련 코드에서 문제가 발생한 것으로 추정됩니다.








































