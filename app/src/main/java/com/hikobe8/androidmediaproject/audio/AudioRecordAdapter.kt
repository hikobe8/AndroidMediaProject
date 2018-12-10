package com.hikobe8.androidmediaproject.audio

import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.hikobe8.androidmediaproject.R
import com.hikobe8.androidmediaproject.inflate
import kotlinx.android.synthetic.main.item_record.view.*

/***
 *  Author : ryu18356@gmail.com
 *  Create at 2018-12-10 16:57
 *  description :
 */

class AudioAdapter: RecyclerView.Adapter<AudioAdapter.AudioHolder>() {

    private val mDataList:ArrayList<AudioRecordBean> = ArrayList()

    fun addData(audioRecordBean: AudioRecordBean) {
        mDataList.add(audioRecordBean)
        notifyItemInserted(itemCount)
    }

    interface OnItemClickListener{
        fun onPlayClicked(audioRecordBean: AudioRecordBean)
        fun onStopClicked(audioRecordBean: AudioRecordBean)
    }

    var mOnItemClickListener:OnItemClickListener?=null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AudioHolder = AudioHolder(parent.inflate(R.layout.item_record, parent))

    override fun getItemCount(): Int = mDataList.size

    override fun onBindViewHolder(holder: AudioHolder, position: Int) {
        holder.bindData(mDataList[position])
    }

    fun update(audioRecordBean: AudioRecordBean) {
        mDataList.asSequence().filter { it != audioRecordBean }.map {
            it.isPlaying = false
        }.toList()
        notifyDataSetChanged()
    }

    inner class AudioHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bindData(audioRecordBean: AudioRecordBean) {
            itemView.tv_name.text = audioRecordBean.name
            if (audioRecordBean.isPlaying) itemView.iv_play.setImageResource(R.drawable.ic_stop) else itemView.iv_play.setImageResource(R.drawable.ic_play)
            itemView.iv_play.setOnClickListener {
                audioRecordBean.isPlaying = !audioRecordBean.isPlaying
                it as ImageView
                if (audioRecordBean.isPlaying){
                    it.setImageResource(R.drawable.ic_stop)
                    mOnItemClickListener?.onPlayClicked(audioRecordBean)
                }
                else {
                    it.setImageResource(R.drawable.ic_play)
                    mOnItemClickListener?.onStopClicked(audioRecordBean)
                }
            }
        }
    }

}

data class AudioRecordBean(val name:String, val path:String) {
    var isPlaying = false
}