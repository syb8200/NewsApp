package com.practice.fc_2_chapter5

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import com.practice.fc_2_chapter5.databinding.ActivityMainBinding
import com.tickaroo.tikxml.TikXml
import com.tickaroo.tikxml.retrofit.TikXmlConverterFactory
import org.jsoup.Jsoup
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit

class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding
    private lateinit var newsAdapter : NewsAdapter
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://news.google.com/")
        .addConverterFactory(
            TikXmlConverterFactory.create(
                TikXml.Builder()
                    .exceptionOnUnreadXml(false) // 원래 다 파싱을 해줘야 하는데, 다 안써줘도 예외 발생하지 않도록 처리
                    .build()
            )
        ).build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        newsAdapter = NewsAdapter()
        val newsService = retrofit.create(NewsService::class.java)

        binding.newsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = newsAdapter
        }

        binding.feedChip.setOnClickListener {
            binding.chipGroup.clearCheck() // 모든 칩들 isChecked를 해제시킴 (번거롭게 하나씩 지정하지 않아도 된다)
            binding.feedChip.isChecked = true

            newsService.mainFeed().submitList()
        }

        binding.politicsChip.setOnClickListener {
            binding.chipGroup.clearCheck()
            binding.politicsChip.isChecked = true

            newsService.politicsNews().submitList()
        }

        binding.economyChip.setOnClickListener {
            binding.chipGroup.clearCheck()
            binding.economyChip.isChecked = true

            newsService.economyNews().submitList()
        }

        binding.socialChip.setOnClickListener {
            binding.chipGroup.clearCheck()
            binding.socialChip.isChecked = true

            newsService.socialNews().submitList()
        }

        binding.itChip.setOnClickListener {
            binding.chipGroup.clearCheck()
            binding.itChip.isChecked = true

            newsService.itNews().submitList()
        }

        binding.sportsChip.setOnClickListener {
            binding.chipGroup.clearCheck()
            binding.sportsChip.isChecked = true

            newsService.sportsNews().submitList()
        }

        newsService.mainFeed().submitList()
    }

    // 확장함수
    private fun Call<NewsRss>.submitList() {
        enqueue(object: Callback<NewsRss> {
            override fun onResponse(call: Call<NewsRss>, response: Response<NewsRss>) {
                Log.e("MainActivity", "${response.body()?.channel?.items}")

                val list = response.body()?.channel?.items.orEmpty().transform()
                newsAdapter.submitList(list)

                list.forEachIndexed { index, news ->
                    // jsoup.connect는 네트워크에 접속 -> main thread에서 돌 수 없음 -> 따로 thread 생성 필요
                    Thread {
                        try {
                            val jsoup = Jsoup.connect(news.link).get()
                            val elements = jsoup.select("meta[property^=og:]")
                            val ogImageNode = elements.find { node ->
                                node.attr("property") == "og:image"
                            }
                            news.imageUrl = ogImageNode?.attr("content")

                        } catch (e: java.lang.Exception) {
                            e.printStackTrace()
                        }

                        // UI 변경 작업은 Thread 안에서 불가능 -> runOnUiThread 사용
                        runOnUiThread {
                            newsAdapter.notifyItemChanged(index)
                        }
                    }.start()
                }
            }

            override fun onFailure(call: Call<NewsRss>, t: Throwable) {
                t.printStackTrace()
            }
        })
    }
}