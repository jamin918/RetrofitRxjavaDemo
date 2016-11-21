package com.jm.retrofitdemo.api;

import com.jm.retrofitdemo.model.Top250;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * @author jamin
 * @date 2016/11/21
 * @desc 获取豆瓣电影Top250 榜单
 */
public interface DoubanApi {

    @GET("/v2/movie/top250")
    Call<Top250> getResult(@Query("start")int start, @Query("count")int count);
}
