/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: /home/ismael/Android/Sdk/build-tools/36.0.0/aidl -p/home/ismael/Android/Sdk/platforms/android-36/framework.aidl -o/home/ismael/Gadgetbridge-EmpaticaE4/app/build/generated/aidl_source_output_dir/mainlineDebug/out -I/home/ismael/Gadgetbridge-EmpaticaE4/app/src/main/aidl -I/home/ismael/Gadgetbridge-EmpaticaE4/app/src/mainline/aidl -I/home/ismael/Gadgetbridge-EmpaticaE4/app/src/debug/aidl -I/home/ismael/Gadgetbridge-EmpaticaE4/app/src/mainlineDebug/aidl -I/home/ismael/.gradle/caches/8.14.3/transforms/360fce3357d8fa64ee60bca7ca381179/transformed/media-1.0.0/aidl -I/home/ismael/.gradle/caches/8.14.3/transforms/1da19ddfb0db7e6722c2b847ec2413d6/transformed/core-1.17.0/aidl -I/home/ismael/.gradle/caches/8.14.3/transforms/7d9314fbe77cd913c209f56df7488e3a/transformed/versionedparcelable-1.1.1/aidl -d/tmp/aidl2259437072326325544.d /home/ismael/Gadgetbridge-EmpaticaE4/app/src/main/aidl/lineageos/weather/ILineageWeatherManager.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package lineageos.weather;
public interface ILineageWeatherManager extends android.os.IInterface
{
  /** Default implementation for ILineageWeatherManager. */
  public static class Default implements lineageos.weather.ILineageWeatherManager
  {
    @Override public void updateWeather(lineageos.weather.RequestInfo info) throws android.os.RemoteException
    {
    }
    @Override public void lookupCity(lineageos.weather.RequestInfo info) throws android.os.RemoteException
    {
    }
    @Override public void registerWeatherServiceProviderChangeListener(lineageos.weather.IWeatherServiceProviderChangeListener listener) throws android.os.RemoteException
    {
    }
    @Override public void unregisterWeatherServiceProviderChangeListener(lineageos.weather.IWeatherServiceProviderChangeListener listener) throws android.os.RemoteException
    {
    }
    @Override public java.lang.String getActiveWeatherServiceProviderLabel() throws android.os.RemoteException
    {
      return null;
    }
    @Override public void cancelRequest(int requestId) throws android.os.RemoteException
    {
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements lineageos.weather.ILineageWeatherManager
  {
    /** Construct the stub and attach it to the interface. */
    @SuppressWarnings("this-escape")
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an lineageos.weather.ILineageWeatherManager interface,
     * generating a proxy if needed.
     */
    public static lineageos.weather.ILineageWeatherManager asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof lineageos.weather.ILineageWeatherManager))) {
        return ((lineageos.weather.ILineageWeatherManager)iin);
      }
      return new lineageos.weather.ILineageWeatherManager.Stub.Proxy(obj);
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
        case TRANSACTION_updateWeather:
        {
          lineageos.weather.RequestInfo _arg0;
          _arg0 = _Parcel.readTypedObject(data, lineageos.weather.RequestInfo.CREATOR);
          this.updateWeather(_arg0);
          break;
        }
        case TRANSACTION_lookupCity:
        {
          lineageos.weather.RequestInfo _arg0;
          _arg0 = _Parcel.readTypedObject(data, lineageos.weather.RequestInfo.CREATOR);
          this.lookupCity(_arg0);
          break;
        }
        case TRANSACTION_registerWeatherServiceProviderChangeListener:
        {
          lineageos.weather.IWeatherServiceProviderChangeListener _arg0;
          _arg0 = lineageos.weather.IWeatherServiceProviderChangeListener.Stub.asInterface(data.readStrongBinder());
          this.registerWeatherServiceProviderChangeListener(_arg0);
          break;
        }
        case TRANSACTION_unregisterWeatherServiceProviderChangeListener:
        {
          lineageos.weather.IWeatherServiceProviderChangeListener _arg0;
          _arg0 = lineageos.weather.IWeatherServiceProviderChangeListener.Stub.asInterface(data.readStrongBinder());
          this.unregisterWeatherServiceProviderChangeListener(_arg0);
          break;
        }
        case TRANSACTION_getActiveWeatherServiceProviderLabel:
        {
          java.lang.String _result = this.getActiveWeatherServiceProviderLabel();
          reply.writeNoException();
          reply.writeString(_result);
          break;
        }
        case TRANSACTION_cancelRequest:
        {
          int _arg0;
          _arg0 = data.readInt();
          this.cancelRequest(_arg0);
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements lineageos.weather.ILineageWeatherManager
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
      @Override public void updateWeather(lineageos.weather.RequestInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_updateWeather, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void lookupCity(lineageos.weather.RequestInfo info) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, info, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_lookupCity, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void registerWeatherServiceProviderChangeListener(lineageos.weather.IWeatherServiceProviderChangeListener listener) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongInterface(listener);
          boolean _status = mRemote.transact(Stub.TRANSACTION_registerWeatherServiceProviderChangeListener, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void unregisterWeatherServiceProviderChangeListener(lineageos.weather.IWeatherServiceProviderChangeListener listener) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongInterface(listener);
          boolean _status = mRemote.transact(Stub.TRANSACTION_unregisterWeatherServiceProviderChangeListener, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public java.lang.String getActiveWeatherServiceProviderLabel() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        java.lang.String _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getActiveWeatherServiceProviderLabel, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readString();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public void cancelRequest(int requestId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(requestId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_cancelRequest, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
    }
    static final int TRANSACTION_updateWeather = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_lookupCity = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_registerWeatherServiceProviderChangeListener = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_unregisterWeatherServiceProviderChangeListener = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_getActiveWeatherServiceProviderLabel = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_cancelRequest = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
  }
  /** @hide */
  public static final java.lang.String DESCRIPTOR = "lineageos.weather.ILineageWeatherManager";
  public void updateWeather(lineageos.weather.RequestInfo info) throws android.os.RemoteException;
  public void lookupCity(lineageos.weather.RequestInfo info) throws android.os.RemoteException;
  public void registerWeatherServiceProviderChangeListener(lineageos.weather.IWeatherServiceProviderChangeListener listener) throws android.os.RemoteException;
  public void unregisterWeatherServiceProviderChangeListener(lineageos.weather.IWeatherServiceProviderChangeListener listener) throws android.os.RemoteException;
  public java.lang.String getActiveWeatherServiceProviderLabel() throws android.os.RemoteException;
  public void cancelRequest(int requestId) throws android.os.RemoteException;
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
  }
}
