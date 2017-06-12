package tileview.demo;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * @author Mike Dunn, 6/11/17.
 */

public class ScrollViewDemo extends AppCompatActivity {

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    //setContentView(R.layout.activity_demos_scrollview);
    TwoDScrollView scrollView = new TwoDScrollView(this);
    setContentView(scrollView);
    LinearLayout linearLayout = new LinearLayout(this);
    linearLayout.setOrientation(LinearLayout.VERTICAL);
    scrollView.addView(linearLayout, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    for (int i = 0; i < 100; i++) {
      TextView textView = new TextView(this);
      textView.setText("Hi I'm TextView #" + i);
      linearLayout.addView(textView);
    }
  }
}
