package com.example.catchcolors

import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import android.widget.ScrollView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContentProviderCompat.requireContext
import com.bumptech.glide.Glide
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.InputStream
import java.net.URL

@Suppress("DEPRECATION")
class WebScrapingActivity : AppCompatActivity() {

    private var currentPage = 1 // 현재 페이지를 나타내는 변수
    private var currentImagePage = 1 // 이미지의 현재 페이지를 나타내는 변수
    private val imagesPerPage = 30
    private var totalImageCount = 0
    private val numColumns = 3


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_scraping)

        // 인텐트로부터 선택한 카테고리 정보 가져오기
        val selectedCategory = intent.getSerializableExtra("selectedCategory") as CategoryItem

        // 선택한 카테고리에 따라 웹 스크래핑을 수행
        GlobalScope.launch(Dispatchers.Main) {
            val result = performWebScraping(selectedCategory, currentImagePage)
            // 웹 스크래핑이 완료된 후에 수행할 작업을 여기에 추가
            println(result)
        }

        // 페이지 로드 버튼에 대한 클릭 이벤트 핸들러 설정
        val PageButton: Button = findViewById(R.id.PageButton)
        PageButton.setOnClickListener {
            // 다음 페이지가 있는지 확인
            if (hasNextPage(selectedCategory)) {
                currentPage++ // 다음 페이지로 업데이트
                currentImagePage = 1 // 새로운 버튼을 누를 때 이미지 페이지 초기화
                // 스크롤을 맨 위로 이동
                val scrollView: ScrollView = findViewById(R.id.scrollView)
                scrollView.fullScroll(ScrollView.FOCUS_UP)
                // GridLayout 초기화
                val gridLayout: GridLayout = findViewById(R.id.gridLayout)
                gridLayout.removeAllViews()
                // 이미지를 추가로 로드
                loadPage(selectedCategory, currentPage, currentImagePage)
            } else {
                // 다음 페이지가 없을 때 처리할 내용을 여기에 추가
                showNoMorePagesDialog()
            }
        }
        // 새로운 버튼을 추가하고 클릭 이벤트 핸들러 설정
        val loadMoreButton: Button = findViewById(R.id.loadMoreButton)
        loadMoreButton.setOnClickListener {
            currentImagePage++ // 이미지 페이지 업데이트
            loadPage(selectedCategory, currentPage, currentImagePage)
        }
    }
    private suspend fun performWebScraping(categoryItem: CategoryItem, imagePage: Int): String {
        return withContext(Dispatchers.IO) {
            try {
                // 이전에 로드된 이미지 수를 업데이트
                totalImageCount = (currentPage - 1) * imagesPerPage

                showLoadingDialog()

                val screenWidth = resources.displayMetrics.widthPixels
                val itemWidth = screenWidth / numColumns

                val document = Jsoup.connect(categoryItem.getPageUrl(currentPage)).get()

                // 웹 스크래핑 로직을 작성
                val links = document.select("#searchList > li > div.li_inner > div.list_img > a")

                // 추가로 불러올 이미지 수를 설정
                val additionalImages = imagesPerPage * currentImagePage

                Log.d("WebScrapingActivity", "additionalImages: $additionalImages, currentImagePage: $currentImagePage, totalImageCount: $totalImageCount")

                // 처음에는 아무런 제약 없이 이미지를 불러옴
                if (currentImagePage > 1 && links.size < additionalImages) {
                    showNoMoreImagesDialog()
                    return@withContext "No Images to Load"
                }

                // 이미지뷰를 반복문 외부에서 선언
                val gridLayout: GridLayout = findViewById(R.id.gridLayout)

                // 반복문을 통해 이미지뷰에 띄우기
                for ((index, link) in links.take(additionalImages).withIndex()) {
                    // 이미지의 총 수를 업데이트
                    totalImageCount++
                    // UI 업데이트는 메인 스레드에서 진행
                    withContext(Dispatchers.Main) {
                        // 이미지 URL이 상대 경로인 경우 절대 경로로 변환
                        val image = link.select("img").attr("data-original")
                        val imageUrl = if (!image.startsWith("http")) {
                            // 기본적으로 "http:"을 추가하고 필요에 따라 "https:"로 변경할 수 있습니다.
                            "http:$image"
                        } else {
                            image
                        }


                        // ImageView에 이미지 로딩
                        val imageView = ImageView(this@WebScrapingActivity)
                        val params = GridLayout.LayoutParams()
                        params.width = itemWidth
                        params.height = ViewGroup.LayoutParams.WRAP_CONTENT

                        // 현재 이미지의 행 및 열 인덱스 계산
                        params.rowSpec = GridLayout.spec(index / numColumns)
                        params.columnSpec = GridLayout.spec(index % numColumns)

                        imageView.layoutParams = params

                        Glide.with(this@WebScrapingActivity)
                            .load(imageUrl)
                            .override(400, 400)
                            .into(imageView)

                        gridLayout.addView(imageView)

                        imageView.setOnClickListener {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link.attr("href")))
                            startActivity(intent)
                        }
                    }
                }

                // 웹 스크래핑 결과를 문자열로 반환
                "Web Scraping Success"

            } catch (e: Exception) {
                e.printStackTrace()
                // 에러가 발생한 경우 에러 메시지를 반환
                "Web Scraping Error: ${e.message}"
            }finally {
                delay(1000) // 딜레이 추가 또는 다른 로직 수행
                dismissLoadingDialog() // 로딩 다이얼로그를 닫음
            }
        }
    }
    private fun showLoadingDialog() {
        CoroutineScope(Dispatchers.Main).launch {
            val dialog = LoadingDialog(this@WebScrapingActivity)
            dialog.show()
            delay(1000)        // 딜레이 시간
            dialog.dismiss()
        }
    }
    // 페이지를 로드하는 함수
    private fun loadPage(selectedCategory: CategoryItem, page: Int, imagePage: Int) {
        GlobalScope.launch(Dispatchers.Main) {
            // 현재 페이지의 URL을 사용하여 웹 스크래핑 수행
            val result = performWebScraping(selectedCategory, imagePage)
            // 웹 스크래핑이 완료된 후에 수행할 작업을 여기에 추가
            println(result)
        }
    }
    // 다음 페이지가 있는지 확인하는 함수
    private fun hasNextPage(selectedCategory: CategoryItem): Boolean {
        return when (currentPage) {
            1 -> selectedCategory.page2 != null
            2 -> selectedCategory.page3 != null
            3 -> selectedCategory.page4 != null
            4 -> selectedCategory.page5 != null
            // 추가적인 페이지가 있다면 계속해서 처리할 수 있습니다.
            else -> false // 기본적으로 false를 반환하여 다음 페이지가 없다고 간주
        }
    }

    private fun showNoMorePagesDialog() {
        runOnUiThread {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("알림")
                .setMessage("다음 페이지가 없습니다.")
                .setPositiveButton("확인") { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
                .show()
        }
    }

    // 이미지를 더 이상 불러올 수 없는 경우 팝업을 띄우는 함수
    private fun showNoMoreImagesDialog() {
        runOnUiThread {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("알림")
                .setMessage("불러올 이미지가 없습니다.")
                .setPositiveButton("확인") { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
                .show()
        }
    }
    private fun CategoryItem.getPageUrl(page: Int): String? {
        return when (page) {
            1 -> this.page1
            2 -> this.page2 ?: run {
                showNoMorePagesDialog()
                return null
            }
            3 -> this.page3 ?: run {
                showNoMorePagesDialog()
                return null
            }
            4 -> this.page4 ?: run {
                showNoMorePagesDialog()
                return null
            }
            5 -> this.page5 ?: run {
                showNoMorePagesDialog()
                return null
            }
            // 추가적인 페이지가 있다면 계속해서 처리할 수 있습니다.
            else -> {
                showNoMorePagesDialog()
                return null // 기본적으로 첫 번째 페이지로 설정
            }
        }
    }
    private fun dismissLoadingDialog() {
        // 로딩 다이얼로그를 닫음
    }
}