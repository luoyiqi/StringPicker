package com.zfs.stringpicker;

import android.graphics.Typeface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
	
	private Toast toast;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		toast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
		StringPicker stringPicker = (StringPicker) findViewById(R.id.string_picker);
		List<String> list = new ArrayList<>();
		for (int i = 1900; i <= 2016; i++) {
			list.add(String.valueOf(i));
		}
		stringPicker.setData(list);
		try {
			stringPicker.setTypeface(Typeface.createFromAsset(getAssets(), "slender.ttf"));
		} catch (Exception e){
			e.printStackTrace();
		}
		//滚动监听
		stringPicker.setOnSelectListener(new StringPicker.onSelectListener() {
			@Override
			public void onSelect(String text) {
				toast.setText(text);
				toast.show();
			}
		});
	}
}
