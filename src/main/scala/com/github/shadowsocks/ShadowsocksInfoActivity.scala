package com.github.shadowsocks

import android.app.TaskStackBuilder
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.WindowManager


class ShadowsocksInfoActivity extends AppCompatActivity {


//  def navigateUp() {
//    val intent = getParentActivityIntent
//    if (shouldUpRecreateTask(intent) || isTaskRoot)
//    {
//      TaskStackBuilder.create(this).addNextIntentWithParentStack(intent).startActivities()
//    }
//    else {
//
//    }
//  }
  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.layout_info)
    val toolbar = findViewById(R.id.toolbar).asInstanceOf[Toolbar]
    toolbar.setTitle(getTitle)
    toolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_material)
    toolbar.setNavigationOnClickListener(_ => {
      setResult(1002)
      finish()
    })
  }

}
