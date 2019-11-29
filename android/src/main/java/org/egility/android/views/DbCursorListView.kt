/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.android.views

import android.content.Context
import android.os.Parcelable
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.ListView
import org.egility.library.database.DbCursorInterface
import java.util.*

class DbCursorListView : ListView, AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {
    interface Listener {
        fun whenItemClick(position: Int)
        fun whenLongClick(position: Int)
        fun whenPopulate(view: View, position: Int)
    }

    // General fields
    private var _listener: Listener? = null
    private var _cursor: DbCursorInterface? = null
    private var _array: ArrayList<*>? = null
    private var resource: Int = 0


    constructor(context: Context) : super(context) {
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        super.onRestoreInstanceState(state)
    }

    override fun onSaveInstanceState(): Parcelable {
        return super.onSaveInstanceState()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return false /* to force keys to be intercepted by the activity */
    }

    override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        _listener?.whenItemClick(position)
    }

    override fun onItemLongClick(parent: AdapterView<*>, view: View, position: Int, id: Long): Boolean {
        _listener?.whenLongClick(position)
        return true
    }

    fun load(listener: Listener? = null, cursor: DbCursorInterface, resource: Int) {
        this.resource = resource
        this._cursor = cursor
        this._listener = listener
        adapter = DbCursorAdapter()
        onItemClickListener = this
        onItemLongClickListener = this
//        setDrawSelectorOnTop(true)
    }

    fun load(listener: Listener? = null, array: ArrayList<*>, resource: Int) {
        this.resource = resource
        this._array = array
        this._listener = listener
        adapter = DbCursorAdapter()
        onItemClickListener = this
        onItemLongClickListener = this
//        setDrawSelectorOnTop(true)
    }

    fun pageTop() {
        setSelection(0)
    }

    fun pageBottom() {
        val vTop = count - (lastVisiblePosition - firstVisiblePosition)
        if (vTop >= 0) {
            setSelection(vTop)
        } else {
            setSelection(0)
        }
    }

    fun pageDown() {
        val vTop = lastVisiblePosition
        if (vTop < count) {
            setSelection(vTop)
        }
    }

    fun pageUp() {
        val vTop = firstVisiblePosition - (lastVisiblePosition - firstVisiblePosition)
        if (vTop >= 0) {
            setSelection(vTop)
        } else {
            setSelection(0)
        }
    }

    inner class DbCursorAdapter : BaseAdapter() {

        /*
                override fun getItem(position: Int): DbCursor? {
                    var cursor = _cursor
                    if (cursor != null) {
                        cursor.setCursor(position)
                        return cursor
                    }
                    return null
                }
        */


        override fun getItem(position: Int): Any? {
            throw UnsupportedOperationException()
        }

        override fun getCount(): Int {
            return _cursor?.rowCount ?: _array?.size ?: -1
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
            var view = convertView
            if (view == null) {
                val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                view = inflater.inflate(resource, parent, false)
            }
            val result = view
            if (result != null) {
                _listener?.whenPopulate(result, position)
            }
            return result
        }
    }

}
