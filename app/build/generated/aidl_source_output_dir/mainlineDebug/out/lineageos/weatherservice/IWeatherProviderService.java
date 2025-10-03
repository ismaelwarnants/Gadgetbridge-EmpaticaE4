/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: /home/ismael/Android/Sdk/build-tools/36.0.0/aidl -p/home/ismael/Android/Sdk/platforms/android-36/framework.aidl -o/home/ismael/Gadgetbridge-EmpaticaE4/app/build/generated/aidl_source_output_dir/mainlineDebug/out -I/home/ismael/Gadgetbridge-EmpaticaE4/app/src/main/aidl -I/home/ismael/Gadgetbridge-EmpaticaE4/app/src/mainline/aidl -I/home/ismael/Gadgetbridge-EmpaticaE4/app/src/debug/aidl -I/home/ismael/Gadgetbridge-EmpaticaE4/app/src/mainlineDebug/aidl -I/home/ismael/.gradle/caches/8.14.3/transforms/360fce3357d8fa64ee60bca7ca381179/transformed/media-1.0.0/aidl -I/home/ismael/.gradle/caches/8.14.3/transforms/1da19ddfb0db7e6722c2b847ec2413d6/transformed/core-1.17.0/aidl -I/home/ismael/.gradle/caches/8.14.3/transforms/7d9314fbe77cd913c209f56df7488e3a/transformed/versionedparcelable-1.1.1/aidl -d/tmp/aidl11652928972316725050.d /home/ismael/Gadgetbridge-EmpaticaE4/app/src/main/aidl/lineageos/weatherservice/IWeatherProviderService.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package lineageos.weatherservice;
public interface IWeatherProviderService extends android.os.IInterface
{
  /** Default implementation for IWeatherProviderService. */
  public static class Default implements lineageos.weatherservice.IWeatherProviderService
  {
    @Override public void processWeatherUpdateRequest(lineageos.weather.RequestInfo request) throws android.os.RemoteException
    {
    }
    @Override public void processCityNameLookupRequest(lineageos.weather.RequestInfo request) throws android.os.RemoteException
    {
    }
    @Override public void setServiceClient(lineageos.weatherservice.IWeatherProviderServiceClient client) throws android.os.RemoteException
    {
    }
    @Override public void cancelOngoingRequests() throws android.os.RemoteException
    {
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
  public static abstract class Stub extends android.os.Binder implements lineageos.weatherservice.IWeatherProviderService
  {
    /** Construct the stub and attach it to the interface. */
    @SuppressWarnings("this-escape")
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an lineageos.weatherservice.IWeatherProviderService interface,
     * generating a proxy if needed.
     */
    public static lineageos.weatherservice.IWeatherProviderService asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof lineageos.weatherservice.IWeatherProviderService))) {
        return ((lineageos.weatherservice.IWeatherProviderService)iin);
      }
      return new lineageos.weatherservice.IWeatherProviderService.Stub.Proxy(obj);
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
        case TRANSACTION_processWeatherUpdateRequest:
        {
          lineageos.weather.RequestInfo _arg0;
          _arg0 = _Parcel.readTypedObject(data, lineageos.weather.RequestInfo.CREATOR);
          this.processWeatherUpdateRequest(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_processCityNameLookupRequest:
        {
          lineageos.weather.RequestInfo _arg0;
          _arg0 = _Parcel.readTypedObject(data, lineageos.weather.RequestInfo.CREATOR);
          this.processCityNameLookupRequest(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_setServiceClient:
        {
          lineageos.weatherservice.IWeatherProviderServiceClient _arg0;
          _arg0 = lineageos.weatherservice.IWeatherProviderServiceClient.Stub.asInterface(data.readStrongBinder());
          this.setServiceClient(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_cancelOngoingRequests:
        {
          this.cancelOngoingRequests();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_cancelRequest:
        {
          int _arg0;
          _arg0 = data.readInt();
          this.cancelRequest(_arg0);
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
    private static class Proxy implements lineageos.weatherservice.IWeatherProviderService
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
      @Override public void processWeatherUpdateRequest(lineageos.weather.RequestInfo request) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, request, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_processWeatherUpdateRequest, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void processCityNameLookupRequest(lineageos.weather.RequestInfo request) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, request, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_processCityNameLookupRequest, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void setServiceClient(lineageos.weatherservice.IWeatherProviderServiceClient client) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongInterface(client);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setServiceClient, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void cancelOngoingRequests() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_cancelOngoingRequests, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void cancelRequest(int requestId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(requestId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_cancelRequest, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
    }
    static final int TRANSACTION_processWeatherUpdateRequest = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_processCityNameLookupRequest = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_setServiceClient = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_cancelOngoingRequests = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_cancelRequest = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
  }
  /** @hide */
  public static final java.lang.String DESCRIPTOR = "lineageos.weatherservice.IWeatherProviderService";
  public void processWeatherUpdateRequest(lineageos.weather.RequestInfo request) throws android.os.RemoteException;
  public void processCityNameLookupRequest(lineageos.weather.RequestInfo request) throws android.os.RemoteException;
  public void setServiceClient(lineageos.weatherservice.IWeatherProviderServiceClient client) throws android.os.RemoteException;
  public void cancelOngoingRequests() throws android.os.RemoteException;
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
