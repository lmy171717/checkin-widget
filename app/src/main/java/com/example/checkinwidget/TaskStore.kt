package com.example.checkinwidget

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * 打卡任务数据层：用 SharedPreferences 持久化（与小组件、Activity 共享同一份数据）。
 * 完成状态只记录「完成的日期」，因此第二天自动不再算完成（对钩自动恢复）。
 * 只有调用 remove() 才会真正删除任务。
 */
data class Task(
    val id: String,
    val text: String,
    val createdAt: Long,
    var doneDate: String?
)

object TaskStore {

    private const val PREFS = "checkin_tasks"
    private const val KEY = "tasks_v1"

    /** 本地日期字符串 YYYY-MM-DD（按设备时区） */
    fun todayStr(): String {
        val c = java.util.Calendar.getInstance()
        val y = c.get(java.util.Calendar.YEAR)
        val m = c.get(java.util.Calendar.MONTH) + 1
        val d = c.get(java.util.Calendar.DAY_OF_MONTH)
        return "%04d-%02d-%02d".format(y, m, d)
    }

    fun isDone(t: Task, today: String = todayStr()): Boolean = t.doneDate == today

    fun load(ctx: Context): MutableList<Task> {
        val sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = sp.getString(KEY, "[]") ?: "[]"
        val list = mutableListOf<Task>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val done = o.optString("doneDate", "null")
                list.add(
                    Task(
                        id = o.getString("id"),
                        text = o.getString("text"),
                        createdAt = o.optLong("createdAt", 0L),
                        doneDate = if (done == "null" || done.isEmpty()) null else done
                    )
                )
            }
        } catch (e: Exception) {
            // 解析失败则忽略，返回空列表
        }
        return list
    }

    fun save(ctx: Context, tasks: List<Task>) {
        val arr = JSONArray()
        for (t in tasks) {
            val o = JSONObject().apply {
                put("id", t.id)
                put("text", t.text)
                put("createdAt", t.createdAt)
                put("doneDate", t.doneDate ?: JSONObject.NULL)
            }
            arr.put(o)
        }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, arr.toString()).apply()
    }

    fun add(ctx: Context, text: String) {
        val tasks = load(ctx)
        tasks.add(
            Task(
                id = "t" + System.nanoTime().toString(36),
                text = text,
                createdAt = System.currentTimeMillis(),
                doneDate = null
            )
        )
        save(ctx, tasks)
    }

    fun toggle(ctx: Context, id: String) {
        val today = todayStr()
        val tasks = load(ctx)
        for (t in tasks) {
            if (t.id == id) {
                t.doneDate = if (isDone(t, today)) null else today
                break
            }
        }
        save(ctx, tasks)
    }

    fun remove(ctx: Context, id: String) {
        save(ctx, load(ctx).filter { it.id != id })
    }
}
