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
import java.io.EOFException

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

        binding.newsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = newsAdapter
        }

        val newsService = retrofit.create(NewsService::class.java)
        newsService.mainFeed().enqueue(object: Callback<NewsRss> {
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