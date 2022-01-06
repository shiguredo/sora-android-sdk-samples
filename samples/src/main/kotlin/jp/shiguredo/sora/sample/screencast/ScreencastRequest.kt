package jp.shiguredo.sora.sample.screencast

import android.annotation.TargetApi
import android.content.Intent
import android.os.Parcel
import android.os.Parcelable

@TargetApi(21)
class ScreencastRequest(
        val data:              Intent?,
        val signalingEndpoint: String,
        val channelId:         String,
        val signalingMetadata: String?,
        val videoScale:        Float,
        val videoFPS:          Int,
        val videoCodec:        String?,
        val audioCodec:        String?,
        val stateTitle:        String?,
        val stateText:         String?,
        val stateIcon:         Int,
        val notificationIcon:  Int,
        val boundActivityName: String?,
        val multistream:       Boolean
) : Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readParcelable(Intent::class.java.classLoader),
            parcel.readString()!!,
            parcel.readString()!!,
            parcel.readString(),
            parcel.readFloat(),
            parcel.readInt(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readInt(),
            parcel.readInt(),
            parcel.readString(),
            parcel.readByte() != 0.toByte()) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(data, flags)
        parcel.writeString(signalingEndpoint)
        parcel.writeString(channelId)
        parcel.writeString(signalingMetadata)
        parcel.writeFloat(videoScale)
        parcel.writeInt(videoFPS)
        parcel.writeString(videoCodec)
        parcel.writeString(audioCodec)
        parcel.writeString(stateTitle)
        parcel.writeString(stateText)
        parcel.writeInt(stateIcon)
        parcel.writeInt(notificationIcon)
        parcel.writeString(boundActivityName)
        parcel.writeByte(if (multistream) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ScreencastRequest> {
        override fun createFromParcel(parcel: Parcel): ScreencastRequest {
            return ScreencastRequest(parcel)
        }

        override fun newArray(size: Int): Array<ScreencastRequest?> {
            return arrayOfNulls(size)
        }
    }

}
