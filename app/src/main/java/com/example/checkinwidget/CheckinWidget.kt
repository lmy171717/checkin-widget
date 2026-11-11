package com.example.checkinwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/**
 * 桌面打卡小组件。
 * 列表由 CheckinWidgetService 通过 RemoteViewsService 提供。
 * 点击对钩/删除通过 fillInIntent 把 action + taskId 传回这里的 onReceive。
 */
class CheckinWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_TOGGLE = "com.example.checkinwidget.TOGGLE"
        const val ACTION_DELETE = "com.example.checkinwidget.DELETE"
        const val ACTION_ADD = "com.example.checkinwidget.ADD"
        const val EXTRA_ID = "task_id"

        fun updateAll(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, CheckinWidget::class.java))
            for (id in ids) updateAppWidget(context, mgr, id)
        }

        private fun updateAppWidget(context: Context, mgr: AppWidgetManager, appId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_layout)

            // 列表数据来自 CheckinWidgetService
            val service = Intent(context, CheckinWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appId)
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            views.setRemoteAdapter(R.id.list, service)
            views.setEmptyView(R.id.list, R.id.empty)

            // “+”按钮：打开 Activity 添加任务
            val addIntent = Intent(context, CheckinWidget::class.java).apply { action = ACTION_ADD }
            views.setOnClickPendingIntent(
                R.id.add,
                PendingIntent.getBroadcast(
                    context, 0, addIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )

            // 列表项点击的模板（具体 action/taskId 由 fillInIntent 填充）
            val base = Intent(context, CheckinWidget::class.java)
            val template = PendingIntent.getBroadcast(
                context, 1, base,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setPendingIntentTemplate(R.id.list, template)

            mgr.notifyAppWidgetViewDataChanged(appId, R.id.list)
            mgr.updateAppWidget(appId, views)
        }
    }

    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        for (id in ids) updateAppWidget(context, mgr, id)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_TOGGLE -> {
                TaskStore.toggle(context, intent.getStringExtra(EXTRA_ID) ?: "")
                updateAll(context)
            }
            ACTION_DELETE -> {
                TaskStore.remove(context, intent.getStringExtra(EXTRA_ID) ?: "")
                updateAll(context)
            }
            ACTION_ADD -> {
                val i = Intent(context, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(i)
            }
        }
    }

    override fun onEnabled(context: Context) {
        // 第一个小组件添加到桌面时，安排每天 00:05 自动刷新（对钩恢复）
        DailyResetScheduler.schedule(context)
    }

    override fun onDisabled(context: Context) {
        // 最后一个小组件被移除时，取消定时刷新
        DailyResetScheduler.cancel(context)
    }
}
