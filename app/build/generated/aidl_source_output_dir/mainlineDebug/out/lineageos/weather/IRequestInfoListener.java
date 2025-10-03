/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: /home/ismael/Android/Sdk/build-tools/36.0.0/aidl -p/home/ismael/Android/Sdk/platforms/android-36/framework.aidl -o/home/ismael/Gadgetbridge-EmpaticaE4/app/build/generated/aidl_source_output_dir/mainlineDebug/out -I/home/ismael/Gadgetbridge-EmpaticaE4/app/src/main/aidl -I/home/ismael/Gadgetbridge-EmpaticaE4/app/src/mainline/aidl -I/home/ismael/Gadgetbridge-EmpaticaE4/app/src/debug/aidl -I/home/ismael/Gadgetbridge-EmpaticaE4/app/src/mainlineDebug/aidl -I/home/ismael/.gradle/caches/8.14.3/transforms/360fce3357d8fa64ee60bca7ca381179/transformed/media-1.0.0/aidl -I/home/ismael/.gradle/caches/8.14.3/transforms/1da19ddfb0db7e6722c2b847ec2413d6/transformed/core-1.17.0/aidl -I/home/ismael/.gradle/caches/8.14.3/transforms/7d9314fbe77cd913c209f56df7488e3a/transformed/versionedparcelable-1.1.1/aidl -d/tmp/aidl1587330222575726411.d /home/ismael/Gadgetbridge-EmpaticaE4/app/src/main/aidl/lineageos/weather/IRequestInfoListener.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package lineageos.weather;
public interface IRequestInfoListener extends android.os.IInterface
{
  /** Default implementation for IRequestInfoListener. */
  public static class Default implements lineageos.weather.IRequestInfoListener
  {
    @Override public void onWeatherRequestCompleted(lineageos.weather.RequestInfo requestInfo, int status, lineageos.weather.WeatherInfo weatherInfo) throws android.os.RemoteException
    {
    }
    @Override public void onLookupCityRequestCompleted(lineageos.weather.RequestInfo requestInfo, int status, java.util.List<lineageos.weather.WeatherLocation> weatherLocation) throws android.os.RemoteException
    {
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements lineageos.weather.IRequestInfoListener
  {
    /** Construct the stub and attach it to the interface. */
    @SuppressWarnings("this-escape")
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an lineageos.weather.IRequestInfoListener interface,
     * generating a proxy if needed.
     */
    public static lineageos.weather.IRequestInfoListener asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof lineageos.weather.IRequestInfoListener))) {
        return ((lineageos.weather.IRequestInfoListener)iin);
      }
      return new lineageos.weather.IRequestInfoListener.Stub.Proxy(obj);
    }
    @Override public android.os.IBinder asBinder()
    {
      return this;
    }
    @Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
    {
      java.lang.String descriptor = DESCRIPTOR;
      if (code >= android.os.IBinder.FIRST_CALL_TRANSACTION && code <= android.os.IBinder.LAST_CALL_TRANSACTION) {
        data.enforceInterface(descriptor);
      }
      if (code == INTERFACE_TRANSACTION) {
        reply.writeString(descriptor);
        return true;
      }
      switch (code)
      {
        case TRANSACTION_onWeatherRequestCompleted:
        {
          lineageos.weather.RequestInfo _arg0;
          _arg0 = _Parcel.readTypedObject(data, lineageos.weather.RequestInfo.CREATOR);
          int _arg1;
          _arg1 = data.readInt();
          lineageos.weather.WeatherInfo _arg2;
          _arg2 = _Parcel.readTypedObject(data, lineageos.weather.WeatherInfo.CREATOR);
          this.onWeatherRequestCompleted(_arg0, _arg1, _arg2);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_onLookupCityRequestCompleted:
        {
          lineageos.weather.RequestInfo _arg0;
          _arg0 = _Parcel.readTypedObject(data, lineageos.weather.RequestInfo.CREATOR);
          int _arg1;
          _arg1 = data.readInt();
          java.util.List<lineageos.weather.WeatherLocation> _arg2;
          _arg2 = data.createTypedArrayList(lineageos.weather.WeatherLocation.CREATOR);
          this.onLookupCityRequestCompleted(_arg0, _arg1, _arg2);
          reply.writeNoException();
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements lineageos.weather.IRequestInfoListener
    {
      private android.os.IBinder mRemote;
      Proxy(android.os.IBinder remote)
      {
        mRemote = remote;
      }
      @Override public android.os.IBinder asBinder()
      {
        return mRemote;
      }
      public java.lang.String getInterfaceDescriptor()
      {
        return DESCRIPTOR;
      }
      @Override public void onWeatherRequestCompleted(lineageos.weather.RequestInfo requestInfo, int status, lineageos.weather.WeatherInfo weatherInfo) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, requestInfo, 0);
          _data.writeInt(status);
          _Parcel.writeTypedObject(_data, weatherInfo, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onWeatherRequestCompleted, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void onLookupCityRequestCompleted(lineageos.weather.RequestInfo requestInfo, int status, java.util.List<lineageos.weather.WeatherLocation> weatherLocation) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, requestInfo, 0);
          _data.writeInt(status);
          _Parcel.writeTypedList(_data, weatherLocation, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onLookupCityRequestCompleted, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
    }
    static final int TRANSACTION_onWeatherRequestCompleted = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_onLookupCityRequestCompleted = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
  }
  /** @hide */
  public static final java.lang.String DESCRIPTOR = "lineageos.weather.IRequestInfoListener";
  public void onWeatherRequestCompleted(lineageos.weather.RequestInfo requestInfo, int status, lineageos.weather.WeatherInfo weatherInfo) throws android.os.RemoteException;
  public void onLookupCityRequestCompleted(lineageos.weather.RequestInfo requestInfo, int status, java.util.List<lineageos.weather.WeatherLocation> weatherLocation) throws android.os.RemoteException;
  /** @hide */
  static class _Parcel {
    static private <T> T readTypedObject(
        android.os.Parcel parcel,
        android.os.Parcelable.Creator<T> c) {
      if (parcel.readInt() != 0) {
          return c.createFromParcel(parcel);
      } else {
          return null;
      }
    }
    static private <T extends android.os.Parcelable> void writeTypedObject(
        android.os.Parcel parcel, T value, int parcelableFlags) {
      if (value != null) {
        parcel.writeInt(1);
        value.writeToParcel(parcel, parcelableFlags);
      } else {
        parcel.writeInt(0);
      }
    }
    static private <T extends android.os.Parcelable> void writeTypedList(
        android.os.Parcel parcel, java.util.List<T> value, int parcelableFlags) {
      if (value == null) {
        parcel.writeInt(-1);
      } else {
        int N = value.size();
        int i = 0;
        parcel.writeInt(N);
        while (i < N) {
    writeTypedObject(parcel, value.get(i), parcelableFlags);
          i++;
        }
      }
    }
  }
}
