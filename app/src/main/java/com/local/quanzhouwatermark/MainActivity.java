package com.local.quanzhouwatermark;
import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.webkit.*;
import android.util.Base64;
import android.widget.Toast;
import java.io.*;
public class MainActivity extends Activity {
  private WebView web; private ValueCallback<Uri[]> pending; private static final int PICK=510;
  @Override public void onCreate(Bundle state){
    super.onCreate(state); web=new WebView(this);setContentView(web);
    WebSettings s=web.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setAllowFileAccess(true);s.setAllowContentAccess(true);s.setDefaultTextEncodingName("UTF-8");
    web.setWebViewClient(new WebViewClient());
    web.setWebChromeClient(new WebChromeClient(){
      @Override public boolean onShowFileChooser(WebView w,ValueCallback<Uri[]> c,FileChooserParams p){
        if(pending!=null)pending.onReceiveValue(null);pending=c;
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("image/*");i.addCategory(Intent.CATEGORY_OPENABLE);i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);startActivityForResult(i,PICK);return true;
      }
    });
    web.addJavascriptInterface(new Object(){
      @JavascriptInterface public void save(String name,String mime,String base64){
        try{
          name=name.replaceAll("[/\\\\]","_");byte[] bytes=Base64.decode(base64,Base64.DEFAULT);
          if(Build.VERSION.SDK_INT>=29){
            ContentValues v=new ContentValues();v.put(MediaStore.Downloads.DISPLAY_NAME,name);v.put(MediaStore.Downloads.MIME_TYPE,mime);v.put(MediaStore.Downloads.RELATIVE_PATH,Environment.DIRECTORY_DOWNLOADS+"/泉州东站水印");v.put(MediaStore.Downloads.IS_PENDING,1);
            Uri uri=getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,v);
            if(uri==null)throw new IOException("创建文件失败");
            try(OutputStream out=getContentResolver().openOutputStream(uri)){out.write(bytes);}
            v.clear();v.put(MediaStore.Downloads.IS_PENDING,0);getContentResolver().update(uri,v,null,null);
          }else{
            File dir=new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),"泉州东站水印");dir.mkdirs();try(OutputStream out=new FileOutputStream(new File(dir,name))){out.write(bytes);}
          }
          runOnUiThread(()->Toast.makeText(MainActivity.this,"已保存 "+name,Toast.LENGTH_SHORT).show());
        }catch(Exception e){runOnUiThread(()->Toast.makeText(MainActivity.this,"保存失败: "+e.getMessage(),Toast.LENGTH_LONG).show());}
      }
    },"AndroidSave");
    web.loadUrl("file:///android_asset/index.html");
  }
  @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request!=PICK||pending==null)return;Uri[] values=null;if(result==RESULT_OK&&data!=null){if(data.getClipData()!=null){int n=data.getClipData().getItemCount();values=new Uri[n];for(int i=0;i<n;i++)values[i]=data.getClipData().getItemAt(i).getUri();}else if(data.getData()!=null)values=new Uri[]{data.getData()};}pending.onReceiveValue(values);pending=null;}
  @Override public void onBackPressed(){if(web.canGoBack())web.goBack();else super.onBackPressed();}
}