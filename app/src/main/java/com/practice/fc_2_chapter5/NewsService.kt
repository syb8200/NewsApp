package com.practice.fc_2_chapter5

import retrofit2.Call
import retrofit2.http.GET

interface NewsService {
    @GET("rss?hl=ko&gl=KR&ceid=KR:ko")
    fun mainFeed() : Call<NewsRss>
}