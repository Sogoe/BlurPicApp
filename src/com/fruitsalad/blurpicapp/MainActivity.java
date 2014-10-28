package com.fruitsalad.blurpicapp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener {
	private ImageView blurView = null;
	private Button selectButton = null;
	private Button saveButton = null;
	private Intent selectPicIntent = null;
	private static int REQUEST_CODE = 3;
	private Bitmap oldImage = null;
	private Bitmap newImage = null;
	private File outDir = null;
	private Toast toast = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		blurView = (ImageView) findViewById(R.id.show);
		selectButton = (Button) findViewById(R.id.select_pic);
		saveButton = (Button) findViewById(R.id.save_pic);

		selectButton.setOnClickListener(this);
		saveButton.setOnClickListener(this);

		outDir = new File(Environment.getExternalStorageDirectory(), "BlurPic");
		if (!outDir.exists())
			outDir.mkdirs();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.select_pic:
			selectPicIntent = new Intent(Intent.ACTION_GET_CONTENT);
			selectPicIntent.setType("image/*");
			startActivityForResult(selectPicIntent, REQUEST_CODE);
			break;
		case R.id.save_pic:
			if (newImage != null && !newImage.isRecycled()) {
				String filename = System.currentTimeMillis() + ".jpg";
				File temp = new File(outDir, filename);
				try {
					newImage.compress(Bitmap.CompressFormat.JPEG, 100,
							new FileOutputStream(temp));
					if (toast == null) {
						toast = Toast.makeText(this, outDir.toString() + "/"
								+ filename + " saved!", Toast.LENGTH_SHORT);
					} else {
						toast.setText(outDir.toString() + "/" + filename
								+ " saved!");
					}
					toast.show();
				} catch (FileNotFoundException e) {
					Log.e("compress", e.getMessage());
				}
			}
			break;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
			Uri imageUri = data.getData();
			if (imageUri != null) {

				blurView.setImageBitmap(null);
	
				try {
					if (newImage != null && !newImage.isRecycled()) {
						newImage.recycle();
						newImage = null;
					}
					if (oldImage != null && !oldImage.isRecycled()) {
						oldImage.recycle();
						oldImage = null;
					}
					InputStream is = getContentResolver().openInputStream(
							imageUri);
					BitmapFactory.Options options = new BitmapFactory.Options();
					options.inJustDecodeBounds = true;
					BitmapFactory.decodeStream(is, null, options);
					options.inSampleSize = ImageUtils.computeSampleSize(
							options,
							ImageUtils.getScreenWidth(this),
							ImageUtils.getScreenHeight(this)
									* ImageUtils.getScreenWidth(this));
					options.inJustDecodeBounds = false;
					options.inPurgeable = true;
					options.inInputShareable = true;
					InputStream nis = getContentResolver().openInputStream(
							imageUri);
					oldImage = BitmapFactory.decodeStream(nis, null, options);
					is.close();
					nis.close();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				new Thread(new Runnable() {

					@Override
					public void run() {

						newImage = Blur.fastblur(MainActivity.this, oldImage,
								24);

						runOnUiThread(new Runnable() {

							@Override
							public void run() {
								blurView.setImageBitmap(newImage);
							}
						});

					}
				}).start();
			}
		}
	}
}
