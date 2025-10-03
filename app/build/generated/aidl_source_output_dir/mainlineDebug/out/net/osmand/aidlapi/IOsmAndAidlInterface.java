/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: /home/ismael/Android/Sdk/build-tools/36.0.0/aidl -p/home/ismael/Android/Sdk/platforms/android-36/framework.aidl -o/home/ismael/Gadgetbridge-EmpaticaE4/app/build/generated/aidl_source_output_dir/mainlineDebug/out -I/home/ismael/Gadgetbridge-EmpaticaE4/app/src/main/aidl -I/home/ismael/Gadgetbridge-EmpaticaE4/app/src/mainline/aidl -I/home/ismael/Gadgetbridge-EmpaticaE4/app/src/debug/aidl -I/home/ismael/Gadgetbridge-EmpaticaE4/app/src/mainlineDebug/aidl -I/home/ismael/.gradle/caches/8.14.3/transforms/360fce3357d8fa64ee60bca7ca381179/transformed/media-1.0.0/aidl -I/home/ismael/.gradle/caches/8.14.3/transforms/1da19ddfb0db7e6722c2b847ec2413d6/transformed/core-1.17.0/aidl -I/home/ismael/.gradle/caches/8.14.3/transforms/7d9314fbe77cd913c209f56df7488e3a/transformed/versionedparcelable-1.1.1/aidl -d/tmp/aidl13549340589069160104.d /home/ismael/Gadgetbridge-EmpaticaE4/app/src/main/aidl/net/osmand/aidlapi/IOsmAndAidlInterface.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package net.osmand.aidlapi;
public interface IOsmAndAidlInterface extends android.os.IInterface
{
  /** Default implementation for IOsmAndAidlInterface. */
  public static class Default implements net.osmand.aidlapi.IOsmAndAidlInterface
  {
    /**
     * Method to register for updates during navgation. Notifies user about distance to the next turn and its type.
     * 
     * @param subscribeToUpdates (boolean) - subscribe or unsubscribe from updates
     * @param callbackId (long) - id of callback, needed to unsubscribe from updates
     * @param callback (IOsmAndAidlCallback) - callback to notify user on navigation data change
     */
    @Override public long registerForNavigationUpdates(net.osmand.aidlapi.navigation.ANavigationUpdateParams params, net.osmand.aidlapi.IOsmAndAidlCallback callback) throws android.os.RemoteException
    {
      return 0L;
    }
    /**
     * Method to register for Voice Router voice messages during navigation. Notifies user about voice messages.
     * 
     * @params subscribeToUpdates (boolean) - boolean flag to subscribe or unsubscribe from messages
     * @params callbackId (long) - id of callback, needed to unsubscribe from messages
     * @params callback (IOsmAndAidlCallback) - callback to notify user on voice message
     */
    @Override public long registerForVoiceRouterMessages(net.osmand.aidlapi.navigation.ANavigationVoiceRouterMessageParams params, net.osmand.aidlapi.IOsmAndAidlCallback callback) throws android.os.RemoteException
    {
      return 0L;
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements net.osmand.aidlapi.IOsmAndAidlInterface
  {
    /** Construct the stub and attach it to the interface. */
    @SuppressWarnings("this-escape")
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an net.osmand.aidlapi.IOsmAndAidlInterface interface,
     * generating a proxy if needed.
     */
    public static net.osmand.aidlapi.IOsmAndAidlInterface asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof net.osmand.aidlapi.IOsmAndAidlInterface))) {
        return ((net.osmand.aidlapi.IOsmAndAidlInterface)iin);
      }
      return new net.osmand.aidlapi.IOsmAndAidlInterface.Stub.Proxy(obj);
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
        case TRANSACTION_registerForNavigationUpdates:
        {
          net.osmand.aidlapi.navigation.ANavigationUpdateParams _arg0;
          _arg0 = _Parcel.readTypedObject(data, net.osmand.aidlapi.navigation.ANavigationUpdateParams.CREATOR);
          net.osmand.aidlapi.IOsmAndAidlCallback _arg1;
          _arg1 = net.osmand.aidlapi.IOsmAndAidlCallback.Stub.asInterface(data.readStrongBinder());
          long _result = this.registerForNavigationUpdates(_arg0, _arg1);
          reply.writeNoException();
          reply.writeLong(_result);
          break;
        }
        case TRANSACTION_registerForVoiceRouterMessages:
        {
          net.osmand.aidlapi.navigation.ANavigationVoiceRouterMessageParams _arg0;
          _arg0 = _Parcel.readTypedObject(data, net.osmand.aidlapi.navigation.ANavigationVoiceRouterMessageParams.CREATOR);
          net.osmand.aidlapi.IOsmAndAidlCallback _arg1;
          _arg1 = net.osmand.aidlapi.IOsmAndAidlCallback.Stub.asInterface(data.readStrongBinder());
          long _result = this.registerForVoiceRouterMessages(_arg0, _arg1);
          reply.writeNoException();
          reply.writeLong(_result);
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements net.osmand.aidlapi.IOsmAndAidlInterface
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
      /**
       * Method to register for updates during navgation. Notifies user about distance to the next turn and its type.
       * 
       * @param subscribeToUpdates (boolean) - subscribe or unsubscribe from updates
       * @param callbackId (long) - id of callback, needed to unsubscribe from updates
       * @param callback (IOsmAndAidlCallback) - callback to notify user on navigation data change
       */
      @Override public long registerForNavigationUpdates(net.osmand.aidlapi.navigation.ANavigationUpdateParams params, net.osmand.aidlapi.IOsmAndAidlCallback callback) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        long _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, params, 0);
          _data.writeStrongInterface(callback);
          boolean _status = mRemote.transact(Stub.TRANSACTION_registerForNavigationUpdates, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readLong();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /**
       * Method to register for Voice Router voice messages during navigation. Notifies user about voice messages.
       * 
       * @params subscribeToUpdates (boolean) - boolean flag to subscribe or unsubscribe from messages
       * @params callbackId (long) - id of callback, needed to unsubscribe from messages
       * @params callback (IOsmAndAidlCallback) - callback to notify user on voice message
       */
      @Override public long registerForVoiceRouterMessages(net.osmand.aidlapi.navigation.ANavigationVoiceRouterMessageParams params, net.osmand.aidlapi.IOsmAndAidlCallback callback) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        long _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, params, 0);
          _data.writeStrongInterface(callback);
          boolean _status = mRemote.transact(Stub.TRANSACTION_registerForVoiceRouterMessages, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readLong();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
    }
    static final int TRANSACTION_registerForNavigationUpdates = (android.os.IBinder.FIRST_CALL_TRANSACTION + 65);
    static final int TRANSACTION_registerForVoiceRouterMessages = (android.os.IBinder.FIRST_CALL_TRANSACTION + 71);
  }
  /** @hide */
  public static final java.lang.String DESCRIPTOR = "net.osmand.aidlapi.IOsmAndAidlInterface";
  /**
   * Method to register for updates during navgation. Notifies user about distance to the next turn and its type.
   * 
   * @param subscribeToUpdates (boolean) - subscribe or unsubscribe from updates
   * @param callbackId (long) - id of callback, needed to unsubscribe from updates
   * @param callback (IOsmAndAidlCallback) - callback to notify user on navigation data change
   */
  public long registerForNavigationUpdates(net.osmand.aidlapi.navigation.ANavigationUpdateParams params, net.osmand.aidlapi.IOsmAndAidlCallback callback) throws android.os.RemoteException;
  /**
   * Method to register for Voice Router voice messages during navigation. Notifies user about voice messages.
   * 
   * @params subscribeToUpdates (boolean) - boolean flag to subscribe or unsubscribe from messages
   * @params callbackId (long) - id of callback, needed to unsubscribe from messages
   * @params callback (IOsmAndAidlCallback) - callback to notify user on voice message
   */
  public long registerForVoiceRouterMessages(net.osmand.aidlapi.navigation.ANavigationVoiceRouterMessageParams params, net.osmand.aidlapi.IOsmAndAidlCallback callback) throws android.os.RemoteException;
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
