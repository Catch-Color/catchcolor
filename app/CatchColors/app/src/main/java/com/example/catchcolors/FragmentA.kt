package com.example.catchcolors

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Intent                                     // 안드로이드 인텐트 클래스 임포트
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap                                    // 비트맵 클래스 임포트
import android.graphics.BitmapFactory                             // 비트맵 팩토리 클래스 임포트
import android.graphics.Color
import android.net.Uri                                            // URI 클래스 임포트
import android.os.Bundle                                          // 안드로이드 번들 클래스 임포트
import android.os.Environment                                     // 안드로이드 환경 클래스 임포트
import android.provider.MediaStore                                // 안드로이드 미디어스토어 클래스 임포트
import android.util.Log                                           // 안드로이드 로그 클래스 임포트
import android.view.LayoutInflater                                // 안드로이드 레이아웃 인플레이터 클래스 임포트
import android.view.View                                          // 안드로이드 뷰 클래스 임포트
import android.view.ViewGroup                                     // 안드로이드 뷰그룹 클래스 임포트
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import androidx.fragment.app.Fragment                             // 안드로이드 Fragment 클래스 임포트
import androidx.activity.result.contract.ActivityResultContracts  // 안드로이드 ActivityResultContracts 클래스 임포트
import androidx.core.content.FileProvider                         // 안드로이드 파일 프로바이더 클래스 임포트
import com.example.catchcolors.databinding.FragmentABinding       // 바인딩 클래스 임포트
import androidx.exifinterface.media.ExifInterface
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.widget.Toast
import com.example.catchcolors.PermissionConstants.PERMISSION_REQUEST_CODE
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File                                               // 파일 클래스 임포트
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat                                 // 날짜 형식 클래스 임포트
import java.util.Date                                             // 날짜 클래스 임포트

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class FragmentA : Fragment() {

    // lateinit은 나중에 초기화하겠음을 표시
    private lateinit var binding: FragmentABinding          // 프래그먼트의 레이아웃 관련된 바인딩 객체
    private lateinit var filePath: String                   // 카메라로 찍은 이미지의 파일 경로를 저장하기 위한 변수

    private lateinit var mRetrofit : Retrofit                       // 사용할 레트로핏 객체.
    private lateinit var mRetrofitAPI: RetrofitAPI                  // 레트로핏 api객체.
    private lateinit var mCallTodoList : Call<JsonObject>
    private lateinit var mCallTodoListImage : Call<ResponseBody>

    private val client = OkHttpClient()

    private var isCoatChecked = false               // 아우터 여부에 대한 초기 T/F 전달용 변수. 초기 false 지정.
    private var isSkirtChecked = false

    // 버튼 1 색상 데이터 전역 변수 지정
    var toneInTone_1Red = 0
    var toneInTone_1Green = 0
    var toneInTone_1Blue = 0

    // 버튼 2 색상 데이터 전역 변수 지정
    var toneInTone_2Red = 0
    var toneInTone_2Green = 0
    var toneInTone_2Blue = 0

    // 버튼 3 색상 데이터 전역 변수 지정
    var toneInTone_3Red = 0
    var toneInTone_3Green = 0
    var toneInTone_3Blue = 0

    // 버튼 4 색상 데이터 전역 변수 지정
    var toneOnToneRed = 0
    var toneOnToneGreen = 0
    var toneOnToneBlue = 0

    private lateinit var uniqueFileName: String
    private lateinit var rotatedImageFile: File

    private lateinit var userImage:ImageView

    private var isGCFabOpen = false
    private var isColorFabOpen = false

    private var PorL = true


    // 권한 요청 결과 처리
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 권한 허용 시 이미지 저장
                saveImage()
            } else {
                Toast.makeText(requireContext(), "저장 권한이 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 갤러리 앱에 접근을 하기 위한 요청 런처
    private val requestGalleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            // 갤러리 앱에서 이미지가 성공적으로 로드하고 나서 resultHandler가 실행될 수 있도록 제한. 해당 코드가 없으면 이미지를 불러오기 전
            // 해당 코드가 없으면 이미지를 불러오기 전에 resultHandler가 실행되면서 이미지를 로드하지 못함.
            if (result.resultCode == Activity.RESULT_OK) {

                // 런처 사용시 결과에 대한 uri 저장.
                val uri = result.data?.data
                if (uri != null) {
                    resultHandler(uri, false)      // 버튼 클릭시 메인 기능.
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 카메라 앱에 접근을 하기 위한 요청 런처
    private val requestCameraFileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            // 카메라 앱에서 이미지가 성공적으로 로드하고 나서 resultHandler가 실행될 수 있도록 제한. 해당 코드가 없으면 이미지를 불러오기 전
            // 해당 코드가 없으면 이미지를 불러오기 전에 resultHandler가 실행되면서 이미지를 로드하지 못함.
            if (result.resultCode == Activity.RESULT_OK) {

                // 런처 사용시 이미지가 저장되는 저장 경로를 불러옴
                val uri = Uri.fromFile(File(filePath))
                if (uri != null) {
                    resultHandler(uri, true)      // 버튼 클릭시 메인 기능.
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCreateView(                           // 시작시 사용자 인터페이스 생성
        inflater: LayoutInflater,                        // xml 레이아웃을 인프레이트
        container: ViewGroup?,                           // 부모 뷰 그룹
        savedInstanceState: Bundle?                      // 액티비티나 다른 화면 전환되기 전에 현재 상태 정보를 저장하는 객체
    ): View {       //fragmentABinding 객체 생성. 즉 binding을 이용해서 xml 레이아웃에 파일에 정의한 ui 요소와 프래그먼트의 코드를 연결하는 작업.
        binding = FragmentABinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setRetrofit()

        // 레이아웃에 사진이 표시될 공간 재확인.
        val userImage = view.findViewById<ImageView>(R.id.userImage)

        // 갤러리 버튼과 카메라 버튼 가리면서 애니메이션용 FAB 버튼
        binding.GCfabMain.setOnClickListener {
            GCtoggleFab()
        }

        // 갤러리 버튼 클릭 이벤트 핸들러
        binding.galleryButton.setOnClickListener {

            // 인텐트 생성: 사용자가 이미지를 선택할 수 있는 갤러리 앱을 열기 위한 인텐트(앱 컴포넌트 간 통신을 위한 메시지 객체)
            val intent = Intent(Intent.ACTION_GET_CONTENT, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.type = "image/*"     // 이미지 파일만 선택 가능하도록 인텐트 타입 지정

            // 갤러리 앱 실행을 위한 런처를 통해 인텐트 실행 요청
            requestGalleryLauncher.launch(intent)
        }

        // 카메라 버튼 클릭 이벤트 핸들러
        binding.cameraButton.setOnClickListener {

            // 현재 시간을 이용하여 고유한 파일 이름 생성. 카메라 앱으로 사진 찍어서 전송하더라도 실제 파일이 생긴건 아님.
            val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())

            // 이미지를 저장할 디렉토리 경로 설정
            val storageDir: File? = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES)

            // 임시 파일 생성: "JPEG_시간_고유식별자.jpg" 형식으로 파일 이름 생성
            val file = File.createTempFile(
                "JPEG_${timeStamp}_",
                ".jpg",
                storageDir
            )

            // 임시 파일 경로 저장
            filePath = file.absolutePath

            // 사진 촬영을 위한 카메라 앱 실행을 위한 인텐트 생성
            val photoURI: Uri = FileProvider.getUriForFile(
                requireContext(),
                "com.example.catchcolors.fileprovider",
                file
            )
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)      // 이미지 저장 경로 설정
            requestCameraFileLauncher.launch(intent)                // 카메라 앱 실행 요청

        }

        binding.resendButton.setOnClickListener{
            showLoadingDialog()

            GlobalScope.launch(Dispatchers.IO) {
                Log.d("UploadData", "아우터 체크값: $isCoatChecked")
                Log.d("UploadData", "치마 체크값: $isSkirtChecked")

                FileUploadUtils.goSend(rotatedImageFile, isSkirtChecked, isCoatChecked)

                delay(25000)

                callTodoList()

                isGCFabOpen = false
                isColorFabOpen = false
            }
        }

        // 버튼 가리면서 애니메이션용 FAB 버튼
        binding.ColorfabMain.setOnClickListener {
            ColortoggleFab()
        }

        //버튼 1 클릭시 동작
        binding.button1.setOnClickListener {
            sendRequest("button1")
        }

        //버튼 2 클릭시 동작
        binding.button2.setOnClickListener {
            sendRequest("button2")
        }

        //버튼 3 클릭시 동작
        binding.button3.setOnClickListener {
            sendRequest("button3")
        }

        //버튼 4 클릭시 동작
        binding.button4.setOnClickListener {
            sendRequest("button4")
        }

        //버튼 5 클릭시 동작
        binding.button5.setOnClickListener {
            sendRequest("button5")
        }

        // 아우터 착용 여부 체크박스
        val outerCheckBox = view.findViewById<CheckBox>(R.id.coatcheckbox)
        outerCheckBox.setOnCheckedChangeListener { _, isChecked ->
            isCoatChecked = isChecked
        }

        val skirtCheckbox= view.findViewById<CheckBox>(R.id.skirtcheckbox)
        skirtCheckbox.setOnCheckedChangeListener { _, isChecked ->
            isSkirtChecked = isChecked
        }

        // 이미지 저장 버튼
        val saveImageButton = view.findViewById<Button>(R.id.saveButton)
        saveImageButton.setOnClickListener {
            saveImage()
        }
    }

    // 갤러리 버튼과 카메라 버튼에 대한 동작
    private fun resultHandler(uri: Uri, fromCamera: Boolean) {
        try {
            // 이미지 정보를 가져오기 위해 BitmapFactory.Options 객체 생성
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true   // 이미지를 실제로 로드하지 않고 정보만 불러옴.

            // 이미지를 열어 정보를 읽기 위해 inputStream을 초기화
            var inputStream = requireActivity().contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream, null, options)

            inputStream?.close()

            // 이미지의 가로와 세로 크기를 가져옴
            val imageWidth = options.outWidth
            val imageHeight = options.outHeight

            // Api 서버의 한계로 이미지의 크기를 조절해야 함. 최대 길이를 1000으로 제한
            val maxWidth = 1000
            val maxHeight = 1000

            // 가로와 세로 길이를 최대 크기에 맞게 조절
            val scaledImageWidth: Int
            val scaledImageHeight: Int

            if (imageWidth > imageHeight) {
                // 이미지의 가로가 더 길 경우
                scaledImageWidth = maxWidth
                scaledImageHeight = (maxWidth * (imageHeight.toFloat() / imageWidth.toFloat())).toInt()
            } else {
                // 이미지의 세로가 더 길거나 가로와 세로가 같은 경우
                scaledImageWidth = (maxHeight * (imageWidth.toFloat() / imageHeight.toFloat())).toInt()
                scaledImageHeight = maxHeight
            }

            // 스케일링한 이미지를 bitmap에 생성형 저장.
            val scaledImage = Bitmap.createScaledBitmap(
                BitmapFactory.decodeStream(requireActivity().contentResolver.openInputStream(uri)),
                scaledImageWidth, scaledImageHeight, true
            )

            // 회전 정보를 확인하기 위한 Exif 데이터 확인을 위함. 카메라/갤러리 둘간에 불러오는 방식이 다름. 시발
            val exif = if (fromCamera) {
                uri.path?.let { ExifInterface(it) }
            } else {
                uri?.let { ExifInterface(requireActivity().contentResolver.openInputStream(it) ?: return) }
            }

            // 회전 정보를 확인하기 위한 Exif 데이터 확인. 디지털 카메라로 촬영한 이미지는 Exif 데이터를 포함함.
            // 인터넷에서 다운받은 이미지 같이. 직접적으로 카메라로 촬영하지 않아서 Exif 데이터가 없는 경우 NORMAL로 지정
            val orientation = exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                ?: ExifInterface.ORIENTATION_NORMAL

            // Exif 데이터에 따라 원래 형태로 회전시기키 위한 회전각 설정.
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(270f)
            }

            // 회전각을 반영해서 이미지 재설정. 추가로 위에서 조절한 가로 세로 크기를 적용.
            val rotatedImage = Bitmap.createBitmap(scaledImage, 0, 0, scaledImageWidth, scaledImageHeight, matrix, true)

            // 회전된 이미지의 가로 세로 비율 계산
            var rotatedImageWidth = rotatedImage.width
            var rotatedImageHeight = rotatedImage.height
            val aspectRatio = rotatedImageWidth.toFloat() / rotatedImageHeight.toFloat()

            // 이미지 파일 고유 이름 생성.
            val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
            uniqueFileName = "temp_${timeStamp}.jpg"    // 이름 형식 temp_날짜_시간

            // 새로운 이미지 파일을 캐시 디렉토리에 저장
            rotatedImageFile = File(requireContext().cacheDir, uniqueFileName)
            val rotatedImageOutputStream = FileOutputStream(rotatedImageFile)
            rotatedImage.compress(Bitmap.CompressFormat.JPEG, 100, rotatedImageOutputStream)
            rotatedImageOutputStream.flush()
            rotatedImageOutputStream.close()

            // 로딩 다이얼로그 표시
            showLoadingDialog()

            // 이미지 업로드 및 처리를 백그라운드 스레드에서 처리
            GlobalScope.launch(Dispatchers.IO) {
                Log.d("UploadData", "아우터 체크값: $isCoatChecked")
                FileUploadUtils.goSend(rotatedImageFile, isSkirtChecked, isCoatChecked)     // 리사이징과 로테이션 처리된 이미지 파일과 아우터 체크 여부를 서버 전송.

                delay(25000) // 대기 시간 지정.

                // 직전 코드 종료 후 수행
                withContext(Dispatchers.Main) {
                    // 이미지의 가로 세로 비율에 따른 레이아웃 변경 수행. 비율 값 전송.
                    updateLayoutBasedOnImageAspectRatio(uri, aspectRatio)
                }
                callTodoList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 이미지의 가로 세로 비율에 따른 레이아웃 교체
    private fun updateLayoutBasedOnImageAspectRatio(uri: Uri, aspectRatio: Float) {
        val inflater = LayoutInflater.from(requireContext())

        // 이미지의 (가로 / 세로) 비율에 따라 새로운 레이아웃 ID 결정
        val newLayoutId = if (aspectRatio <= 1) {
            // 세로의 비율이 높은 이미지 fragment_a_portrait.xml로 전환
            R.layout.fragment_a_portrait
        } else {
            // 가로의 비율이 높은 이미지 fragment_a_landscape.xml로 전환
            //requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            R.layout.fragment_a_landscape
        }

        PorL = if (aspectRatio <= 1) {
            true
        } else {
            false
        }

        // 이전 체크박스 상태 값을 저장. 레이아웃 갱신 전 저장. 코드 순서 주의
        val previousIsCoatChecked = isCoatChecked
        val previousIsSkirtChecked = isSkirtChecked

        // 새로운 레이아웃을 인플레이트---------------------------------------
        val newView = inflater.inflate(newLayoutId, null)


        // 이전의 뷰를 제거-------------------------------------------------
        val parentView = binding.root
        parentView.removeAllViews()

        // LayoutParams를 설정하여 새로운 뷰를 추가
        val layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        newView.layoutParams = layoutParams
        parentView.addView(newView)
        parentView.tag = newLayoutId

        // 변경된 레이아웃에 불러온 이미지를 출력하도록 출력될 공간의 객체 확인.
        userImage = newView.findViewById<ImageView>(R.id.userImage)

        isGCFabOpen = false
        isColorFabOpen = false

        val GCfabMain = newView.findViewById<FloatingActionButton>(R.id.GCfabMain)
        GCfabMain.setOnClickListener {
            GCtoggleFab()
        }

        // 새로운 레이아웃에서 갤러리 버튼과 카메라 버튼 객체 찾고, 이벤트 핸들러를 재등록(동일 코드 재사용. 주석 X)
        val galleryButton = newView.findViewById<FloatingActionButton>(R.id.galleryButton)
        val cameraButton = newView.findViewById<FloatingActionButton>(R.id.cameraButton)
        val resendButton = newView.findViewById<FloatingActionButton>(R.id.resendButton)

        galleryButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.type = "image/*"
            requestGalleryLauncher.launch(intent)
        }

        cameraButton.setOnClickListener {
            val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
            val storageDir: File? = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val file = File.createTempFile(
                "JPEG_${timeStamp}_",
                ".jpg",
                storageDir
            )
            filePath = file.absolutePath
            val photoURI: Uri = FileProvider.getUriForFile(
                requireContext(),
                "com.example.catchcolors.fileprovider",
                file
            )
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            requestCameraFileLauncher.launch(intent)
        }

        resendButton.setOnClickListener{
            showLoadingDialog()
            GlobalScope.launch(Dispatchers.IO) {
                Log.d("UploadData", "아우터 체크값: $isCoatChecked")
                Log.d("UploadData", "치마 체크값: $isSkirtChecked")
                FileUploadUtils.goSend(rotatedImageFile, isSkirtChecked, isCoatChecked)
                delay(25000)
                callTodoList()
                isGCFabOpen = false
                isColorFabOpen = false
            }
        }

        val ColorfabMain = newView.findViewById<FloatingActionButton>(R.id.ColorfabMain)
        ColorfabMain.setOnClickListener {
            ColortoggleFab()
        }

        // 새로운 레이아웃에서 색상 변경용 버튼의 객체들을 찾고, 이벤트 핸들러를 재등록(동일 코드 재사용. 주석 X)
        val button1 = newView.findViewById<FloatingActionButton>(R.id.button1)
        val button2 = newView.findViewById<FloatingActionButton>(R.id.button2)
        val button3 = newView.findViewById<FloatingActionButton>(R.id.button3)
        val button4 = newView.findViewById<FloatingActionButton>(R.id.button4)
        val button5 = newView.findViewById<FloatingActionButton>(R.id.button5)


        button1.setOnClickListener {
            createScaleAnimation(button1, 1.2f)
            resetOtherButtons(listOf(button2, button3, button4, button5))
            sendRequest("button1")
        }

        button2.setOnClickListener {
            createScaleAnimation(button2, 1.2f)
            resetOtherButtons(listOf(button1, button3, button4, button5))
            sendRequest("button2")
        }

        button3.setOnClickListener {
            createScaleAnimation(button3, 1.2f)
            resetOtherButtons(listOf(button1, button2, button4, button5))
            sendRequest("button3")
        }

        button4.setOnClickListener {
            createScaleAnimation(button4, 1.2f)
            resetOtherButtons(listOf(button1, button2, button3, button5))
            sendRequest("button4")
        }

        button5.setOnClickListener {
            createScaleAnimation(button5, 1.2f)
            resetOtherButtons(listOf(button1, button2, button3, button4))
            sendRequest("button5")
        }

        // 이전 체크박스 상태를 새로운 레이아웃에 복원
        isCoatChecked = previousIsCoatChecked
        isSkirtChecked = previousIsSkirtChecked

        // 체크박스 객체를 찾고, 이전 체크박스의 상태 값을 적용
        val outerCheckBox = newView.findViewById<CheckBox>(R.id.coatcheckbox)
        outerCheckBox.isChecked = isCoatChecked

        // 체크박스에 대한 이벤트 핸들러 재등록(동일 코드 재사용. 주석 X)
        outerCheckBox.setOnCheckedChangeListener { _, isChecked ->
            isCoatChecked = isChecked
        }

        val skirtCheckbox= newView.findViewById<CheckBox>(R.id.skirtcheckbox)
        skirtCheckbox.setOnCheckedChangeListener { _, isChecked ->
            isSkirtChecked = isChecked
        }

        // 이미지 저장 버튼 객체를 찾고, 이미지 저장 버튼에 대한 이벤트 핸들러 재등록(동일 코드 재사용. 주석 X)
        val saveImageButton = newView.findViewById<Button>(R.id.saveButton)
        saveImageButton.setOnClickListener {
            saveImage()
        }
    }


    // 버튼 1~5 클릭시 서버 응답
    private fun sendRequest(buttonId: String) {
        val JSON = "application/json; charset=utf-8".toMediaTypeOrNull()
        val json = JSONObject().put("button_id", buttonId).toString()

        val body = RequestBody.create(JSON, json)
        val request = Request.Builder()
            .url("http://43.201.32.24/")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                if (response.isSuccessful) {
                    // 응답이 성공했을 때만 처리
                    var responseData = response.body?.bytes() // 이미지 데이터를 바이트 배열로 가져옵니다
                    if (responseData != null) {
                        val bitmap = BitmapFactory.decodeByteArray(responseData, 0, responseData.size) // 바이트 배열을 비트맵으로 변환
                        if (bitmap != null) {
                            // 이미지뷰에 비트맵 설정
                            userImage.setImageBitmap(bitmap)
                        } else {
                            // 비트맵을 만들지 못한 경우에 대한 처리
                            Toast.makeText(requireContext(), "이미지를 표시할 수 없습니다.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // 응답 데이터가 없는 경우에 대한 처리
                        Toast.makeText(requireContext(), "이미지 데이터를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // 응답이 실패한 경우에 대한 처리
                    // 실패한 경우에 대한 추가 처리를 여기에 추가
                }
            }
        })
    }

    // 이미지 저장 버튼 기능
    private fun saveImage() {
        val drawable = userImage.drawable as? BitmapDrawable
        val imageBitmap = drawable?.bitmap

        if (imageBitmap != null) {
            val folderName = "CCEditImage" // 원하는 폴더 이름
            val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
            val fileName = "Edit_image_$timeStamp.jpg"

            val resolver: ContentResolver = requireContext().contentResolver

            // 저장할 이미지의 메타데이터 설정
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + folderName)
            }

            // 미디어 스토어에 이미지 파일을 추가하고 그 파일의 URI를 얻음
            val imageUri: Uri? = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

            if (imageUri != null) {
                try {
                    val outputStream: OutputStream? = resolver.openOutputStream(imageUri)
                    outputStream?.use { os ->
                        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, os)
                    }
                    Toast.makeText(requireContext(), "이미지 저장 성공", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "이미지 저장 실패", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "이미지 저장 실패", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), "저장할 이미지가 없음", Toast.LENGTH_SHORT).show()
        }
    }

    // 레트로핏을 사용하여 가져올 URL 설정 및 레트로핏 객체 초기화
    private fun setRetrofit(){
        mRetrofit = Retrofit
            .Builder()
            .baseUrl("http://43.201.32.24/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        //인터페이스로 만든 레트로핏 api요청 받는 것 변수로 등록
        mRetrofitAPI = mRetrofit.create(RetrofitAPI::class.java)
    }

    // Retrofit 콜백
    private val mRetrofitCallback = object : Callback<JsonObject> {
        override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {

            val result = response.body()                // 서버 응답에서 JSON 데이터를 추출
            Log.d("testt", "결과는 $result")
            //val gson = Gson()                           // Gson 객체를 사용하여 JSON 데이터를 파싱

            // 데이터를 받으면서 일시에 처리하도록 지정.
            // 서버로부터 받은 result 값을 가공.
            val toneInTone_1Array = result?.getAsJsonArray("tone_in_tone")
            val toneInTone_2Array = result?.getAsJsonArray("tone_in_tone_1")
            val toneInTone_3Array = result?.getAsJsonArray("tone_in_tone_2")

            val toneOnToneArray = result?.getAsJsonArray("tone_on_tone")

            val imageUrl = result?.get("url")?.asString
            if (!imageUrl.isNullOrBlank()) {
                callTodoImage(imageUrl)
            } else {
                Log.e("이미지", "Invalid or missing image URL")
            }

            // 받은 result 값에 있는 각각의 색상을 배열로 처리하여 각각의 변수에 저장.
            if (toneInTone_1Array != null && toneOnToneArray != null && toneInTone_2Array != null && toneInTone_3Array != null
                && toneInTone_1Array.size() == 3 && toneInTone_2Array.size() == 3 && toneInTone_3Array.size() == 3 &&toneOnToneArray.size() == 3) {


                // 버튼 1 (id 변경시 인식을 위해 수정할 것!)
                toneInTone_1Red = toneInTone_1Array[0].asInt
                toneInTone_1Green = toneInTone_1Array[1].asInt
                toneInTone_1Blue = toneInTone_1Array[2].asInt

                // 버튼 2 (id 변경시 인식을 위해 수정할 것!)
                toneInTone_2Red = toneInTone_2Array[0].asInt
                toneInTone_2Green = toneInTone_2Array[1].asInt
                toneInTone_2Blue = toneInTone_2Array[2].asInt

                // 버튼 3 (id 변경시 인식을 위해 수정할 것!)
                toneInTone_3Red = toneInTone_3Array[0].asInt
                toneInTone_3Green = toneInTone_3Array[1].asInt
                toneInTone_3Blue = toneInTone_3Array[2].asInt

                // 버튼 42 (id 변경시 인식을 위해 수정할 것!)
                toneOnToneRed = toneOnToneArray[0].asInt
                toneOnToneGreen = toneOnToneArray[1].asInt
                toneOnToneBlue = toneOnToneArray[2].asInt

                // 레이아웃이 변경될 때, 버튼의 id를 찾아서 지정.
                val button1 = requireView().findViewById<FloatingActionButton>(R.id.button1)
                val button2 = requireView().findViewById<FloatingActionButton>(R.id.button2)
                val button3 = requireView().findViewById<FloatingActionButton>(R.id.button3)
                val button4 = requireView().findViewById<FloatingActionButton>(R.id.button4)

                // 버튼의 바탕 색상 설정
                button1.backgroundTintList = ColorStateList.valueOf(Color.rgb(toneInTone_1Red, toneInTone_1Green, toneInTone_1Blue))
                button2.backgroundTintList = ColorStateList.valueOf(Color.rgb(toneInTone_2Red, toneInTone_2Green, toneInTone_2Blue))
                button3.backgroundTintList = ColorStateList.valueOf(Color.rgb(toneInTone_3Red, toneInTone_3Green, toneInTone_3Blue))
                button4.backgroundTintList = ColorStateList.valueOf(Color.rgb(toneOnToneRed, toneOnToneGreen, toneOnToneBlue))

            } else {
                // 적절한 응답이 아닌 경우에 대한 처리
                Log.e("ColorData", "Invalid or incomplete response data")
            }
        }

        override fun onFailure(call: Call<JsonObject>, t: Throwable) {
            t.printStackTrace()
            Log.d("testt", "에러입니다. ${t.message}")
        }
    }

    // Retrofit 콜백
    private val mRetrofitCallbackImage = object : Callback<ResponseBody> {
        override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
            if (response.isSuccessful) {
                val imageBody = response.body() // 이미지 데이터를 추출

                // 이미지를 처리
                if (imageBody != null) {
                    val imageBitmap = BitmapFactory.decodeStream(imageBody.byteStream())

                    // ImageView에 현재 이미지 가져오기
                    userImage.setImageBitmap(imageBitmap)
                }
            } else {
                // 이미지 요청이 실패한 경우에 대한 처리
                Log.e("이미지 요청", "이미지 요청 실패")
            }
        }
        override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
            t.printStackTrace()
            Log.e("이미지 요청", "에러 : ${t.message}")
        }
    }

    // Retrofit을 통한 서버에서 데이터 가져오기
    private fun callTodoList(){
        mCallTodoList = mRetrofitAPI.getTodoList() // RetrofitAPI 에서 JSON 객체를 요청해서 반환하는 메소드 호출
        mCallTodoList.enqueue(mRetrofitCallback) // 응답을 큐에 넣어 대기 시켜놓음. 응답이 생기면 뱉어냄.
    }

    // Retrofit을 통한 서버에서 이미지 가져오기
    private fun callTodoImage(imageFileName: String) {
        mCallTodoListImage = mRetrofitAPI.getImage(imageFileName) // 이미지를 가져오는 메소드 호출
        mCallTodoListImage.enqueue(mRetrofitCallbackImage)
    }

    // 이미지를 불러오고 서버로 전송, 데이터를 수신, 레이아웃이 변경되기 까지 보여지는 로딩 다이얼로그 표시
    private fun showLoadingDialog() {
        CoroutineScope(Dispatchers.Main).launch {
            val dialog = LoadingDialog(requireContext())
            dialog.show()
            delay(25000)        // 딜레이 시간
            dialog.dismiss()
        }
    }

    // FAB 버튼 애니메이션 동작
    private fun GCtoggleFab() {
        val GCfabMain = requireView().findViewById<FloatingActionButton>(R.id.GCfabMain)
        val galleryButton = requireView().findViewById<FloatingActionButton>(R.id.galleryButton)
        val cameraButton = requireView().findViewById<FloatingActionButton>(R.id.cameraButton)
        val resendButton = requireView().findViewById<FloatingActionButton>(R.id.resendButton)

        if (isGCFabOpen) {
            galleryButton.animate().translationX(0f)
            cameraButton.animate().translationX(0f)
            resendButton.animate().translationX(0f)
            GCfabMain.setImageResource(R.drawable.cc_add)
        } else {
            galleryButton.animate().translationX(300f)
            cameraButton.animate().translationX(600f)
            resendButton.animate().translationX(900f)
            GCfabMain.setImageResource(R.drawable.cc_close)
        }
        isGCFabOpen = !isGCFabOpen
    }

    // FAB 버튼 애니메이션 동작
    private fun ColortoggleFab() {
        if (isColorFabOpen) {
            resetFloatingActionButton(requireView().findViewById<FloatingActionButton>(R.id.button5))
            resetFloatingActionButton(requireView().findViewById<FloatingActionButton>(R.id.button4))
            resetFloatingActionButton(requireView().findViewById<FloatingActionButton>(R.id.button3))
            resetFloatingActionButton(requireView().findViewById<FloatingActionButton>(R.id.button2))
            resetFloatingActionButton(requireView().findViewById<FloatingActionButton>(R.id.button1))

        } else {
            val button5 = requireView().findViewById<FloatingActionButton>(R.id.button5)
            val button4 = requireView().findViewById<FloatingActionButton>(R.id.button4)
            val button3 = requireView().findViewById<FloatingActionButton>(R.id.button3)
            val button2 = requireView().findViewById<FloatingActionButton>(R.id.button2)
            val button1 = requireView().findViewById<FloatingActionButton>(R.id.button1)
            val ColorfabMain = requireView().findViewById<FloatingActionButton>(R.id.ColorfabMain)

            ColorfabMain.setImageResource(R.drawable.cc_add)

            if (PorL) {
                button5.animate().translationY(-300f)
                button4.animate().translationY(-600f)
                button3.animate().translationY(-900f)
                button2.animate().translationY(-1200f)
                button1.animate().translationY(-1500f)
                ColorfabMain.setImageResource(R.drawable.cc_close)
            } else {
                button5.animate().translationY(-300f)
                button4.animate().translationX(-270f)
                button3.animate().translationX(-540f)
                button2.animate().translationX(-810f)
                button1.animate().translationX(-1080f)
                ColorfabMain.setImageResource(R.drawable.cc_close)
            }
        }
        isColorFabOpen = !isColorFabOpen
    }

    private fun resetFloatingActionButton(view: View) {
        ObjectAnimator.ofFloat(view, "translationX", 0f).apply { start() }
        ObjectAnimator.ofFloat(view, "translationY", 0f).apply { start() }
        ObjectAnimator.ofFloat(view, "scaleX", 1.0f).apply { start() }
        ObjectAnimator.ofFloat(view, "scaleY", 1.0f).apply { start() }
    }

    // 버튼 1~5 클릭시 스케일 조정
    private fun resetOtherButtons(buttons: List<FloatingActionButton>) {
        buttons.forEach { button ->
            ObjectAnimator.ofFloat(button, "scaleX", 1.0f).apply { start() }
            ObjectAnimator.ofFloat(button, "scaleY", 1.0f).apply { start() }
        }
    }

    // 버튼 1~5 클릭시 스케일 조정
    private fun createScaleAnimation(view: View, scaleValue: Float) {
        val scaleAnimatorX = ObjectAnimator.ofFloat(view, View.SCALE_X, scaleValue)
        val scaleAnimatorY = ObjectAnimator.ofFloat(view, View.SCALE_Y, scaleValue)

        val scaleAnimator = AnimatorSet()
        scaleAnimator.playTogether(scaleAnimatorX, scaleAnimatorY)
        scaleAnimator.duration = 200 // 애니메이션 지속 시간(ms)
        scaleAnimator.start()
    }

    // fragment 구분용
    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            FragmentA().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        //requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
}