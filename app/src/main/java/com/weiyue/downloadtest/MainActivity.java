package com.weiyue.downloadtest;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    
    //dangqianwei  dec

    private String[] urls={"http://jzvd.nathen.cn/c494b340ff704015bb6682ffde3cd302/64929c369124497593205a4190d7d128-5287d2089db37e62345123a1be272f8b.mp4",
    "http://jzvd.nathen.cn/63f3f73712544394be981d9e4f56b612/69c5767bb9e54156b5b60a1b6edeb3b5-5287d2089db37e62345123a1be272f8b.mp4",
    "http://jzvd.nathen.cn/b201be3093814908bf987320361c5a73/2f6d913ea25941ffa78cc53a59025383-5287d2089db37e62345123a1be272f8b.mp4",
     "http://jzvd.nathen.cn/d2438fd1c37c4618a704513ad38d68c5/68626a9d53ca421c896ac8010f172b68-5287d2089db37e62345123a1be272f8b.mp4",
    "http://jzvd.nathen.cn/25a8d119cfa94b49a7a4117257d8ebd7/f733e65a22394abeab963908f3c336db-5287d2089db37e62345123a1be272f8b.mp4",
    "http://jzvd.nathen.cn/7512edd1ad834d40bb5b978402274b1a/9691c7f2d7b74b5e811965350a0e5772-5287d2089db37e62345123a1be272f8b.mp4",
    "http://jzvd.nathen.cn/c6e3dc12a1154626b3476d9bf3bd7266/6b56c5f0dc31428083757a45764763b0-5287d2089db37e62345123a1be272f8b.mp4"

};

    // 固定存放下载的音乐的路径：SD卡目录下
    private static final String SD_PATH = "/mnt/sdcard/";
    // 存放各个下载器
    private Map<String, DownLoader> downloaders = new HashMap<String, DownLoader>();
    // 存放与下载器对应的进度条
    private Map<String, ProgressBar> ProgressBars = new HashMap<String, ProgressBar>();
    /**
     * 利用消息处理机制适时更新进度条
     */
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                String url = (String) msg.obj;
                int length = msg.arg1;
                ProgressBar bar = ProgressBars.get(url);
                if (bar != null) {
                    // 设置进度条按读取的length长度更新
                    bar.incrementProgressBy(length);
                    if (bar.getProgress() == bar.getMax()) {
                        LinearLayout layout = (LinearLayout) bar.getParent();
                        TextView resouceName=(TextView)layout.findViewById(R.id.tv_resouce_name);
                        Toast.makeText(MainActivity.this, "["+resouceName.getText()+"]下载完成！", Toast.LENGTH_SHORT).show();
                        // 下载完成后清除进度条并将map中的数据清空
                        layout.removeView(bar);
                        ProgressBars.remove(url);
                        downloaders.get(url).delete(url);
                        downloaders.get(url).reset();
                        downloaders.remove(url);
                        Button btn_start=(Button)layout.findViewById(R.id.btn_start);
                        Button btn_pause=(Button)layout.findViewById(R.id.btn_pause);
                        btn_pause.setVisibility(View.GONE);
                        btn_start.setVisibility(View.GONE);
                    }
                }
            }
        }
    };
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        showListView();
    }
    // 显示listView，这里可以随便添加
    private void showListView() {
        List<Map<String, String>> data = new ArrayList<Map<String, String>>();
        for (int i=0;i<urls.length;i++){
            Map<String, String> map = new HashMap<String, String>();
            map.put("name", urls[i]);
            data.add(map);
        }
        DownLoadAdapter adapter=new DownLoadAdapter(this,data);
        setListAdapter(adapter);
    }

    private void setListAdapter(DownLoadAdapter adapter) {
        ListView listView=findViewById(R.id.list);
        listView.setAdapter(adapter);
    }

    /**
     * 响应开始下载按钮的点击事件
     */
    public void startDownload(View v) {
        // 得到textView的内容
        LinearLayout layout = (LinearLayout) v.getParent();
        String resouceName = ((TextView) layout.findViewById(R.id.tv_resouce_name)).getText().toString();
        String urlstr = resouceName;
        String localfile = SD_PATH + resouceName.substring(resouceName.lastIndexOf("/"));
        //设置下载线程数为4，这里是我为了方便随便固定的
        String threadcount = "4";
        DownloadTask downloadTask=new DownloadTask(v);
        downloadTask.execute(urlstr,localfile,threadcount);

    };
    class DownloadTask extends AsyncTask<String, Integer, LoadInfo> {
        DownLoader downloader=null;
        View v=null;
        String urlstr=null;
        public DownloadTask(final View v){
            this.v=v;
        }
        @Override
        protected void onPreExecute() {
            Button btn_start=(Button)((View)v.getParent()).findViewById(R.id.btn_start);
            Button btn_pause=(Button)((View)v.getParent()).findViewById(R.id.btn_pause);
            btn_start.setVisibility(View.GONE);
            btn_pause.setVisibility(View.VISIBLE);
        }
        @Override
        protected LoadInfo doInBackground(String... params) {
            urlstr=params[0];
            String localfile=params[1];
            int threadcount=Integer.parseInt(params[2]);
            // 初始化一个downloader下载器
            downloader = downloaders.get(urlstr);
            if (downloader == null) {
                downloader = new DownLoader(urlstr, localfile, threadcount, MainActivity.this, mHandler);
                downloaders.put(urlstr, downloader);
            }
            if (downloader.isdownloading())
                return null;
            // 得到下载信息类的个数组成集合
            return downloader.getDownloaderInfors();
        }
        @Override
        protected void onPostExecute(LoadInfo loadInfo) {
            if(loadInfo!=null){
                // 显示进度条
                showProgress(loadInfo, urlstr, v);
                // 调用方法开始下载
                downloader.download();
            }
        }

    };
    /**
     * 显示进度条
     */
    private void showProgress(LoadInfo loadInfo, String url, View v) {
        ProgressBar bar = ProgressBars.get(url);
        if (bar == null) {
            bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            bar.setMax(loadInfo.getFileSize());
            bar.setProgress(loadInfo.getComplete());
            ProgressBars.put(url, bar);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, 5);
            ((LinearLayout) ((LinearLayout) v.getParent()).getParent()).addView(bar, params);
        }
    }
    /**
     * 响应暂停下载按钮的点击事件
     */
    public void pauseDownload(View v) {
        LinearLayout layout = (LinearLayout) v.getParent();
        String resouceName = ((TextView) layout.findViewById(R.id.tv_resouce_name)).getText().toString();
        String urlstr =resouceName;
        downloaders.get(urlstr).pause();
        Button btn_start=(Button)((View)v.getParent()).findViewById(R.id.btn_start);
        Button btn_pause=(Button)((View)v.getParent()).findViewById(R.id.btn_pause);
        btn_pause.setVisibility(View.GONE);
        btn_start.setVisibility(View.VISIBLE);
    }
}
