package com.example.checkinwidget

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * 管理/添加打卡任务。新增、点选打钩、长按删除都会同步刷新桌面小组件。
 */
class MainActivity : AppCompatActivity() {

    private lateinit var input: EditText
    private lateinit var listView: ListView
    private lateinit var adapter: TaskAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        input = findViewById(R.id.input)
        listView = findViewById(R.id.list)
        adapter = TaskAdapter()
        listView.adapter = adapter

        findViewById<Button>(R.id.add_btn).setOnClickListener {
            val txt = input.text.toString().trim()
            if (txt.isNotEmpty()) {
                TaskStore.add(this, txt)
                input.setText("")
                refresh()
            }
        }

        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, pos, _ ->
            val t = adapter.getItem(pos)
            if (t != null) {
                TaskStore.toggle(this, t.id)
                refresh()
            }
        }
        listView.onItemLongClickListener = AdapterView.OnItemLongClickListener { _, _, pos, _ ->
            val t = adapter.getItem(pos)
            if (t != null) {
                TaskStore.remove(this, t.id)
                refresh()
                Toast.makeText(this, "已删除，不再恢复", Toast.LENGTH_SHORT).show()
            }
            true
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        adapter.tasks = TaskStore.load(this)
        adapter.notifyDataSetChanged()
        CheckinWidget.updateAll(this)
    }

    class TaskAdapter : BaseAdapter() {
        var tasks: List<Task> = emptyList()

        override fun getCount(): Int = tasks.size
        override fun getItem(i: Int): Task = tasks[i]
        override fun getItemId(i: Int): Long = tasks[i].id.hashCode().toLong()

        override fun getView(i: Int, convertView: View?, parent: ViewGroup): View {
            val v = convertView
                ?: LayoutInflater.from(parent.context)
                    .inflate(android.R.layout.simple_list_item_1, parent, false)
            val t = tasks[i]
            val tv = v as TextView
            tv.text = t.text
            tv.textSize = 16f
            val c = parent.context
            tv.setTextColor(
                c.resources.getColor(
                    if (TaskStore.isDone(t)) R.color.text_muted else R.color.text_main,
                    null
                )
            )
            return tv
        }
    }
}
