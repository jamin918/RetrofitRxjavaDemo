package com.jm.retrofitdemo;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.jm.retrofitdemo.api.DoubanApi2;
import com.jm.retrofitdemo.model.Movie;
import com.jm.retrofitdemo.model.Top250;
import com.jm.retrofitdemo.view.EndlessScrollListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    Handler mHandler;
    Context mContext;

    List<Movie> movies = new ArrayList<>();

    MoviesAdapter mAdapter;

    SwipeRefreshLayout refreshLayout;
    ListView mMoviesLv;

    Retrofit retrofit;
    DoubanApi2 api2;

    private static final String TAG = "MainActivity";

    public static final String BASE_URL = "https://api.douban.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHandler = new Handler();
        mContext = this;

        init();
    }

    private void init() {

        // 创建Retrofit
        retrofit = new Retrofit.Builder().baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create()) // 支持Rxjava
                .build();

        // 获取接口实例
//        api = retrofit.create(DoubanApi.class);

        api2 = retrofit.create(DoubanApi2.class);

        refreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        mMoviesLv = (ListView) findViewById(R.id.lv_movies);

        refreshLayout.setEnabled(false);
        refreshLayout.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        refreshLayout.post(new Runnable() {
            @Override
            public void run() {
                refreshLayout.setRefreshing(true);
            }
        });

        mAdapter = new MoviesAdapter();

        //先显示emptyView
        View emptyView = LayoutInflater.from(mContext).inflate(R.layout.empty_view, null);
        addContentView(emptyView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        mMoviesLv.setEmptyView(emptyView);
        mMoviesLv.setAdapter(mAdapter);

        requestMovieList(0, 20);

        mMoviesLv.setOnScrollListener(new EndlessScrollListener() {
            @Override
            public void onLoadMore(int page, int totalItemsCount) {
                if (totalItemsCount == 0) {
                    requestMovieList(0, 20);
                } else {
                    requestMovieList(totalItemsCount - 1, 20);
                }
            }
        });

    }


    /**
     * 请求电影列表数据
     * @param start
     * @param count
     */
    private void requestMovieList(int start, int count){

        // 调用方法得到一个Call
//        Call<Top250> call = api.getResult(start, count);

        // 进行网络请求，异步方式 ----->1、Retrofit方式
        /*call.enqueue(new Callback<Top250>() {
            @Override
            public void onResponse(Call<Top250> call, Response<Top250> response) {
                List<Top250.SubjectsEntity> subjectsEntities = response.body().getSubjects();
                for (Top250.SubjectsEntity subject : subjectsEntities) {
                    Movie movie = new Movie();
                    movie.setPoster(subject.getImages()
                            .getSmall());
                    movie.setRating((float) subject.getRating()
                            .getAverage());
                    movie.setTitle(subject.getTitle());
                    List<String> genres = subject.getGenres();
                    String genresStr = "";
                    for (String s : genres) {
                        genresStr += s + " ";
                    }
                    movie.setGenres(genresStr);
                    movies.add(movie);
                }

                mAdapter.notifyDataSetChanged();

                if (refreshLayout.isRefreshing()) {
                    refreshLayout.post(new Runnable() {
                        @Override
                        public void run() {
                            refreshLayout.setRefreshing(false);
                        }
                    });
                }
            }

            @Override
            public void onFailure(Call<Top250> call, Throwable t) {
                t.printStackTrace();
            }
        });*/


        // 2、Retrofit + Rxjava
        api2.getResult(start, count)
                .subscribeOn(Schedulers.newThread()) // 在新的线程中执行网络请求
                /*.observeOn(Schedulers.io())  // 在io线程中执行
                .doOnNext(new Action1<Top250>() {
                    @Override
                    public void call(Top250 top250) {
                        setupData(top250); // 设置数据无效，因为该线程是io线程，需在主线程更新UI
                    }
                })*/.observeOn(AndroidSchedulers.mainThread())// 请求完成后在主线程中更新ui操作
                .subscribe(new Subscriber<Top250>() {
                    @Override
                    public void onCompleted() {
                        Log.d(TAG, "----RxJava: onCompleted----");
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Top250 top250) {
                        //请求成功
                        setupData(top250);
                    }
                });


    }

    private void setupData(Top250 bean){
        List<Top250.SubjectsEntity> subjectsEntities =  bean.getSubjects();
        for (Top250.SubjectsEntity subject : subjectsEntities) {
            Movie movie = new Movie();
            movie.setPoster(subject.getImages()
                    .getSmall());
            movie.setRating((float) subject.getRating()
                    .getAverage());
            movie.setTitle(subject.getTitle());
            List<String> genres = subject.getGenres();
            String genresStr = "";
            for (String s : genres) {
                genresStr += s + " ";
            }
            movie.setGenres(genresStr);
            movies.add(movie);
        }

        mAdapter.notifyDataSetChanged();

        if (refreshLayout.isRefreshing()) {
            refreshLayout.post(new Runnable() {
                @Override
                public void run() {
                    refreshLayout.setRefreshing(false);
                }
            });
        }
    }


    class MoviesAdapter extends BaseAdapter {

        public static final int VIEW_TYPE_LOADING  = 0;
        public static final int VIEW_TYPE_ITEM = 1;


        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            return (position >=movies.size())? VIEW_TYPE_LOADING: VIEW_TYPE_ITEM;
        }

        @Override
        public int getCount() {
            return null == movies || movies.size() ==0 ? 0: movies.size()+1;
        }

        @Override
        public Object getItem(int position) {
            return (getItemViewType(position) == VIEW_TYPE_ITEM)? movies.get(position):null;
        }

        @Override
        public long getItemId(int position) {
            return (getItemViewType(position) == VIEW_TYPE_ITEM)? position : -1;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (getItemViewType(position) == VIEW_TYPE_LOADING)
                return getFooterView(position, convertView, parent);
            ViewHolder vh;
            if (convertView == null) {
                convertView = LayoutInflater.from(mContext)
                        .inflate(R.layout.item_movie, parent, false);
                vh = new ViewHolder();
                vh.poster = (ImageView) convertView.findViewById(R.id.poster);
                vh.title = (TextView) convertView.findViewById(R.id.title);
                vh.genres = (TextView) convertView.findViewById(R.id.genres);
                vh.rating = (TextView) convertView.findViewById(R.id.rating);
                vh.number = (TextView) convertView.findViewById(R.id.number);
                convertView.setTag(vh);
            } else {
                vh = (ViewHolder) convertView.getTag();
            }

            final Movie movie = movies.get(position);
            Picasso.with(mContext)
                    .load(movie.getPoster())
                    .into(vh.poster);
            vh.title.setText(movie.getTitle());
            vh.genres.setText(movie.getGenres());
            vh.rating.setText(String.format("评分：%1$.1f", movie.getRating()));
            vh.number.setText((position + 1) + "");
            return convertView;
        }


        private View getFooterView(int position, View convertView, ViewGroup parent){
            if (position >= 250) {
                return LayoutInflater.from(mContext)
                        .inflate(R.layout.loading_view_zero_height, null);
            }
            return LayoutInflater.from(mContext)
                    .inflate(R.layout.loading_view, null);
        }
    }

    class ViewHolder {
        ImageView poster;
        TextView  title;
        TextView  genres;
        TextView  rating;
        TextView  number;
    }

}
