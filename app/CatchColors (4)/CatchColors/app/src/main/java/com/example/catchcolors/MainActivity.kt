package com.example.catchcolors

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.catchcolors.databinding.ActivityMainBinding
import com.google.android.material.tabs.TabLayoutMediator
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val REQUEST_PERMISSIONS = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkPermission()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 뷰페이저2를 초기화하고 어댑터를 설정
        val pagerAdapter = MyPagerAdapter(this)
        binding.viewpager.adapter = pagerAdapter

        // 하단 탐색을 설정합니다
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item) {
                R.id.FragmentAnavi -> binding.viewpager.currentItem = 0
                R.id.FragmentBnavi -> binding.viewpager.currentItem = 1
                R.id.FragmentCnavi -> binding.viewpager.currentItem = 2
                R.id.FragmentDnavi -> binding.viewpager.currentItem = 3
            }
            true
        }

        // ViewPager2의 페이지 변경에 맞춰 하단 탭을 업데이트합니다
        binding.viewpager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                // 페이지가 변경될 때, 하단 탐색 업데이트
                when (position) {
                    0 -> binding.bottomNavigation.setItemSelected(R.id.FragmentAnavi)
                    1 -> binding.bottomNavigation.setItemSelected(R.id.FragmentBnavi)
                    2 -> binding.bottomNavigation.setItemSelected(R.id.FragmentCnavi)
                    3 -> binding.bottomNavigation.setItemSelected(R.id.FragmentDnavi)
                }
            }
        })
        // 초기 탭
        binding.bottomNavigation.setItemSelected(R.id.FragmentBnavi)
    }

    private fun checkPermission() {
        var permission = mutableMapOf<String, String>()
        permission["camera"] = android.Manifest.permission.CAMERA
        permission["storageRead"] = android.Manifest.permission.READ_EXTERNAL_STORAGE
        permission["storageWrite"] =  android.Manifest.permission.WRITE_EXTERNAL_STORAGE

        // 현재 권한 상태 검사
        var denied = permission.count { ContextCompat.checkSelfPermission(this, it.value)  == PackageManager.PERMISSION_DENIED }

        // 마시멜로 버전 이후
        if(denied > 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(permission.values.toTypedArray(), REQUEST_PERMISSIONS)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == REQUEST_PERMISSIONS) {

            grantResults.forEach {
                if(it == PackageManager.PERMISSION_DENIED) {
                    Toast.makeText(applicationContext, "앱 서비스 이용에 필요한 권한입니다.\n권한에 동의해주세요.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }
}