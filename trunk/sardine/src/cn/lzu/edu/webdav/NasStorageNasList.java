package cn.lzu.edu.webdav;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.googlecode.sardine.DavResource;
import com.googlecode.sardine.Sardine;
import com.googlecode.sardine.SardineFactory;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.AdapterView.OnItemClickListener;

public class NasStorageNasList extends Activity {
	
	private String ROOT = "http://202.201.1.135:30080/mnt/li/lzu1/s1/";
	private List<DavResource> resources = null;
	private Sardine sardine = null;
	private int dept = 0;
	
	
	private final int CREATE_FOLDER = 1;
	private int mPresentDown = 0;

	private int mPictures[];
	
	private String tag = "xiao";
	
	private ListView mFileDirList;
	
	private ArrayList<HashMap<String, Object>> recordItem;
	
	BroadcastReceiver mExternalStorageReceiver;
	private ProgressDialog progressDialog;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.server_files);
        initVariable();
        final Resources localResources = this.getResources();
        progressDialog = ProgressDialog.show(this, localResources.getString(R.string.load_dialog_tile), localResources.getString(R.string.load_dialog_mess));
        new mThread().run();
        this.setTitle("NasStorage Server");
    }
    
    class mThread extends Thread
    {        
        @Override
        public void run() {
            // TODO Auto-generated method stub
            super.run();
            try {
            	 connecting();
            	 Message msg_listData = new Message();
            	 handler.sendMessageDelayed(msg_listData, 500);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
    }    
   
    
    private Handler handler = new Handler() {               
        public void handleMessage(Message message) {                                                       
             progressDialog.dismiss(); // �رս�����
             listFile();
        }
	};
    
    public void initVariable(){
    	mFileDirList = (ListView)findViewById(R.id.mServerList);
    	mPictures = new int[]{R.drawable.back, R.drawable.dir, R.drawable.doc};
    }
    
    public void connecting(){
    	sardine = SardineFactory.begin("lzu", "nopasswd");
    } 
    
    public void listFile(){
    	try {
			resources = sardine.list(ROOT);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	fillFile();
    }
    
    public void fillFile(){
    	SimpleAdapter adapter = null;
    	recordItem = null;
		recordItem = new ArrayList<HashMap<String, Object>>();
		int count = 0;
		for (DavResource res : resources) {
			HashMap<String, Object> map = new HashMap<String, Object>();
			if(count == 0) {
				count++;
				map.put("picture", mPictures[0]);
				map.put("type", "directory");
				map.put("name", res.getName());
				recordItem.add(map);
			} else if(res.getContentType() == null) {
				map.put("picture", mPictures[2]);
				map.put("type", "file");
				map.put("name", res.getName());
				recordItem.add(map);
			} else if(res.getContentType().equalsIgnoreCase("httpd/unix-directory")) {
				map.put("picture", mPictures[1]);
				map.put("type", "directory");
				map.put("name", res.getName());
				recordItem.add(map);
			} else {
				map.put("picture", mPictures[2]);
				map.put("type", "file");
				map.put("name", res.getName());
				recordItem.add(map);
			}
		}
	    Log.i("xiao", "recordItem.size = " + recordItem.size());
		adapter = new SimpleAdapter(this, recordItem, R.layout.server_item, new String[]{"picture", "name"}, new int[]{R.id.server_picture, R.id.server_text});
		mFileDirList.setAdapter(adapter);
		mFileDirList.setOnItemLongClickListener(new LongClickListener());
		mFileDirList.setOnItemClickListener(new ClickListener());
    }
    
    public boolean onCreateOptionsMenu(Menu menu) {
    	super.onCreateOptionsMenu(menu);
    	
    	menu.add(0, CREATE_FOLDER, 0, R.string.create_folder);
    	return true;
    }
    
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId()){
    	case CREATE_FOLDER:
    		createDialog().show();
    		break;
    	}
    	
    	return true;
    }
    
    protected void onDestroy(){
    	Log.i(tag, "onDestroy");
    	super.onDestroy();
    }
    
    public Dialog createDialog(){
    	final Resources localResources = this.getResources();
	    AlertDialog.Builder builder = new Builder(this);
		final View layout = View.inflate(this, R.layout.create_new_folder, null);
		final EditText localFileName = (EditText)layout.findViewById(R.id.folder_name);
		
		builder.setTitle(this.getResources().getString(R.string.create_folder));
		builder.setView(layout);
		builder.setPositiveButton(localResources.getString(R.string.ok), new OnClickListener(){
	
		
			public void onClick(DialogInterface dialog, int which) {
				try {
					sardine.createDirectory(ROOT + localFileName.getText().toString().trim());
				} catch (IOException e) {
					e.printStackTrace();
				}
	    		listFile();
				dialog.dismiss();
			}
		});
		
		builder.setNegativeButton(localResources.getString(R.string.cancel), new OnClickListener(){
	
			
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		return builder.create();
    }
    
    public Dialog createFunctionDialog(){
    	final Resources localResources = this.getResources();
	    AlertDialog.Builder builder = new Builder(this);
	    String []localArray = {localResources.getString(R.string.download), localResources.getString(R.string.delete), localResources.getString(R.string.rename)};
	    builder.setItems(localArray, new OnClickListener(){

			public void onClick(DialogInterface dialog, int which) {
				switch(which){
				case 0:
					progressDialog = ProgressDialog.show(NasStorageNasList.this, localResources.getString(R.string.load_dialog_tile), localResources.getString(R.string.load_dialog_mess));
					new DownLoadThread().run();
					break;
				case 1:
					progressDialog = ProgressDialog.show(NasStorageNasList.this, localResources.getString(R.string.dele_dialog_tile), localResources.getString(R.string.dele_dialog_tile));
					new DeleteFileThread().run();
					break;
				case 2:
					RenameFile((String)recordItem.get(mPresentDown).get("name")).show();
					break;
				}
			}
	    	
	    });
		return builder.create();
    }
    
    class ClickListener implements OnItemClickListener {

		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
				long arg3) {
			mPresentDown = arg2;
			changeDirectory();
			
		}
    	
    }
    
    public void changeDirectory() {
    	final Resources localResources = this.getResources();
		progressDialog = ProgressDialog.show(NasStorageNasList.this, localResources.getString(R.string.load_dialog_tile), localResources.getString(R.string.load_dialog_mess));
    	String selectedFile = (String) recordItem.get(mPresentDown).get("name");
		if(mPresentDown == 0 && dept > 0) {
			dept--;
			ROOT = ROOT.replaceAll(selectedFile + "/", "");
		}else if(recordItem.get(mPresentDown).get("type").toString().equalsIgnoreCase("directory") && mPresentDown > 0) {
			dept++;
			ROOT = ROOT + selectedFile + "/";
		}
		listFile();
    }
    
    class LongClickListener implements OnItemLongClickListener{

		public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
				int arg2, long arg3) {
			mPresentDown = arg2;
			createFunctionDialog().show();
			return false;
		}
    	
    }
    
    class DownLoadThread implements Runnable{

		public void run() {
			downFile((String)recordItem.get(mPresentDown).get("name"));
			Message msg_listData = new Message();
        	handler.sendMessageDelayed(msg_listData, 500);
		}
    	
    }
    
    class DeleteFileThread implements Runnable{

		public void run() {
			deleteFileFromDir((String)recordItem.get(mPresentDown).get("name"));
			Message msg_listData = new Message();
        	handler.sendMessageDelayed(msg_listData, 500);
		}
    	
    }
    
    public void deleteFileFromDir(String fileName){
    	try {
			sardine.delete(ROOT + fileName.replace(" ", "%20"));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
    } 
    
	public void downFile(String fileName) {
		try {
			File destDir = new File("/mnt/sdcard/weslab/");
			if (!destDir.exists()) {
				destDir.mkdirs();
			}
			File outputFile = new File(destDir, fileName);
			
			InputStream fis = sardine.get(ROOT + fileName.replace(" ", "%20"));
			FileOutputStream fos=new FileOutputStream(outputFile);
			byte[] buffer = new byte[1444];
			int byteread=0;
			while((byteread=fis.read(buffer))!=-1) { 
			      fos.write(buffer,0,byteread); 
			  }   
			fis.close();
			fos.close();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * �������ļ�
	 */
	public Dialog RenameFile(final String fileName) {
		System.out.println("important: rename file");
		final Resources localResources = this.getResources();
	    AlertDialog.Builder builder = new Builder(this);
		final View layout = View.inflate(this, R.layout.create_new_folder, null);
		final EditText localFileName = (EditText)layout.findViewById(R.id.folder_name);
		localFileName.setText(fileName);
		
		builder.setTitle(this.getResources().getString(R.string.rename_prompt));
		builder.setView(layout);
		builder.setPositiveButton(localResources.getString(R.string.ok), new OnClickListener(){
	
		
			public void onClick(DialogInterface dialog, int which) {
				try {
					sardine.move(ROOT + fileName, ROOT + localFileName.getText().toString().trim());
				} catch (IOException e) {
					e.printStackTrace();
				}
	    		listFile();
				dialog.dismiss();
			}
		});
		
		builder.setNegativeButton(localResources.getString(R.string.cancel), new OnClickListener(){
	
			
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		return builder.create();
	}
	
	public boolean onKeyDown(int keyCode, KeyEvent event){
    	if(keyCode == KeyEvent.KEYCODE_BACK){
    		Intent localIntent = new Intent();
    		localIntent.setClass(NasStorageNasList.this, SardineActivity.class);
    		NasStorageNasList.this.startActivity(localIntent);
    		NasStorageNasList.this.finish();
    	}
    	return false;
    }
}