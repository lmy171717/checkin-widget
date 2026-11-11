package com.example.checkinwidget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService

/**
 * 为小组件列表提供每一项 RemoteViews。
 * RemoteViews 不支持 EditText，因此「输入打卡内容」放在 MainActivity 里完成；
 * 卡片上直接支持：点圆圈打卡、点垃圾桶删除。
 */
class CheckinWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return Factory(applicationContext, intent)
    }

    class Factory(private val ctx: Context, private val intent: Intent) :
        RemoteViewsService.RemoteViewsFactory {

        private var tasks: List<Task> = emptyList()

        override fun onCreate() {}

        override fun onDataSetChanged() {
            tasks = TaskStore.load(ctx)
        }

        override fun onDestroy() {
            tasks = emptyList()
        }

        override fun getCount(): Int = tasks.size

        override fun getViewTypeCount(): Int = 1

        override fun getItemId(position: Int): Long = tasks[position].id.hashCode().toLong()

        override fun hasStableIds(): Boolean = true

        override fun getLoadingView(): RemoteViews? = null

        override fun getViewAt(position: Int): RemoteViews {
            val t = tasks[position]
            val done = TaskStore.isDone(t)
            val rv = RemoteViews(ctx.packageName, R.layout.widget_item)

            rv.setTextViewText(R.id.item_text, t.text)
            rv.setImageViewResource(
                R.id.item_check,
                if (done) R.drawable.ic_check_on else R.drawable.ic_check_off
            )
            rv.setTextColor(
                R.id.item_text,
                ctx.resources.getColor(
                    if (done) R.color.text_muted else R.color.text_main,
                    null
                )
            )

            // 点击圆圈：切换完成
            val toggle = Intent(ctx, CheckinWidget::class.java).apply {
                action = CheckinWidget.ACTION_TOGGLE
                putExtra(CheckinWidget.EXTRA_ID, t.id)
            }
            rv.setOnClickFillInIntent(R.id.item_check, toggle)
            // 点文字区域也切换
            rv.setOnClickFillInIntent(R.id.item_root, toggle)

            // 点击垃圾桶：删除
            val del = Intent(ctx, CheckinWidget::class.java).apply {
                action = CheckinWidget.ACTION_DELETE
                putExtra(CheckinWidget.EXTRA_ID, t.id)
            }
            rv.setOnClickFillInIntent(R.id.item_del, del)

            return rv
        }
    }
}
