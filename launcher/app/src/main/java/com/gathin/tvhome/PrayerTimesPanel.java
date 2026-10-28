package com.gathin.tvhome;

import android.content.Context;
import android.util.AttributeSet;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PrayerTimesPanel extends LinearLayout {
    final String[] labels={"IMSAK","SUBUH","TERBIT","ZUHUR","ASAR","MAGRIB","ISYA"};
    final String[] keys={"Imsak","Fajr","Sunrise","Dhuhr","Asr","Maghrib","Isha"};
    final TextView[] times=new TextView[7];
    final LinearLayout[] cells=new LinearLayout[7];
    final Handler handler=new Handler(Looper.getMainLooper());
    final ExecutorService worker=Executors.newSingleThreadExecutor();
    TextView status;
    String[] schedule=new String[7];
    boolean loaded=false, stopped=false;

    public PrayerTimesPanel(Context c) {
        super(c);
        setOrientation(VERTICAL);
        setPadding(14,10,14,10);
        GradientDrawable bg=new GradientDrawable();
        bg.setColor(Color.argb(220,20,26,36));
        bg.setCornerRadius(14);
        bg.setStroke(1,Color.argb(60,255,255,255));
        setBackground(bg);

        LinearLayout head=new LinearLayout(c);
        head.setGravity(Gravity.CENTER_VERTICAL);
        addView(head,new LayoutParams(-1,-2));

        ImageView logo=new ImageView(c);
        logo.setImageResource(R.drawable.adzan_tv_logo);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        head.addView(logo,new LayoutParams(38,38));

        LinearLayout titles=new LinearLayout(c);
        titles.setOrientation(VERTICAL);
        titles.setPadding(9,0,0,0);
        head.addView(titles,new LayoutParams(0,-2,1));
        titles.addView(text("ADZAN TV",14,Color.WHITE,true));
        titles.addView(text("GRABAG · MAGELANG",10,Color.LTGRAY,false));

        status=text("MEMUAT JADWAL SALAT",10,Color.LTGRAY,false);
        head.addView(status,new LayoutParams(-2,-2));

        LinearLayout row=new LinearLayout(c);
        row.setGravity(Gravity.CENTER);
        LayoutParams rp=new LayoutParams(-1,-2);
        rp.topMargin=10;
        addView(row,rp);

        for(int i=0;i<7;i++){
            LinearLayout cell=new LinearLayout(c);
            cell.setOrientation(VERTICAL);
            cell.setGravity(Gravity.CENTER);
            cell.setPadding(2,7,2,7);
            row.addView(cell,new LayoutParams(0,-2,1));
            cells[i]=cell;
            TextView name=text(labels[i],9,Color.LTGRAY,true);
            name.setGravity(Gravity.CENTER);
            cell.addView(name);
            times[i]=text("--:--",13,Color.WHITE,true);
            times[i].setGravity(Gravity.CENTER);
            times[i].setPadding(0,5,0,0);
            cell.addView(times[i]);
        }
        fetch();
        handler.post(tick);
        handler.postDelayed(refresh,21600000);
    }

    TextView text(String s,int size,int color,boolean bold) {
        TextView t=new TextView(getContext());
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setTypeface(Typeface.create(bold?"sans-serif-medium":"sans-serif",Typeface.NORMAL));
        t.setIncludeFontPadding(false);
        return t;
    }

    Runnable tick=new Runnable(){
        public void run(){
            if(stopped)return;
            highlight();
            handler.postDelayed(this,30000);
        }
    };
    Runnable refresh=new Runnable(){
        public void run(){
            if(stopped)return;
            fetch();
            handler.postDelayed(this,21600000);
        }
    };

    void fetch(){
        worker.execute(()->{
            HttpURLConnection conn=null;
            try{
                SimpleDateFormat df=new SimpleDateFormat("dd-MM-yyyy",Locale.US);
                df.setTimeZone(TimeZone.getTimeZone("Asia/Jakarta"));
                URL url=new URL("https://api.aladhan.com/v1/timings/"
                    +df.format(new Date())
                    +"?latitude=-7.3707&longitude=110.3204&method=20");
                conn=(HttpURLConnection)url.openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                StringBuilder body=new StringBuilder();
                try(BufferedReader r=new BufferedReader(
                    new InputStreamReader(conn.getInputStream()))){
                    String line;
                    while((line=r.readLine())!=null)body.append(line);
                }
                JSONObject t=new JSONObject(body.toString())
                    .getJSONObject("data").getJSONObject("timings");
                String[] result=new String[7];
                for(int i=0;i<7;i++){
                    String v=t.getString(keys[i]).trim();
                    result[i]=v.substring(0,5);
                }
                handler.post(()->{
                    schedule=result;
                    loaded=true;
                    for(int i=0;i<7;i++)times[i].setText(result[i]);
                    highlight();
                });
            }catch(Exception e){
                handler.post(()->{
                    if(!loaded)status.setText("Jadwal offline · cek internet");
                });
            }finally{
                if(conn!=null)conn.disconnect();
            }
        });
    }

    void highlight(){
        if(!loaded||stopped)return;
        SimpleDateFormat f=new SimpleDateFormat("HH:mm",Locale.US);
        f.setTimeZone(TimeZone.getTimeZone("Asia/Jakarta"));
        String n=f.format(new Date());
        int now=Integer.parseInt(n.substring(0,2))*60
            +Integer.parseInt(n.substring(3,5));
        int next=-1;
        for(int i=1;i<7;i++){
            try{
                String s=schedule[i];
                int m=Integer.parseInt(s.substring(0,2))*60
                    +Integer.parseInt(s.substring(3,5));
                if(m>now){next=i;break;}
            }catch(Exception ignored){}
        }
        if(next<0)next=1;
        status.setText("SALAT BERIKUTNYA: "+labels[next]+" · "+schedule[next]);
        for(int i=0;i<7;i++){
            boolean active=i==next;
            cells[i].setBackgroundColor(active
                ?Color.argb(70,50,190,135):Color.TRANSPARENT);
            times[i].setTextColor(active
                ?Color.rgb(125,245,190):Color.WHITE);
        }
    }

    public void stopUpdates(){
        stopped=true;
        handler.removeCallbacks(tick);
        handler.removeCallbacks(refresh);
        worker.shutdownNow();
    }
}
