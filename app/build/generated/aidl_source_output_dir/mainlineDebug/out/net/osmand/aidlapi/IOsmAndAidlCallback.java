/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: /home/ismael/Android/Sdk/build-tools/36.0.0/aidl -p/home/ismael/Android/Sdk/platforms/android-36/framework.aidl -o/home/ismael/Gadgetbridge-EmpaticaE4/app/build/generated/aidl_source_output_dir/mainlineDebug/out -I/home/ismael/Gadgetbridge-EmpaticaE4/app/src/main/aidl -I/home/ismael/Gadgetbridge-EmpaticaE4/app/src/mainline/aidl -I/home/ismael/Gadgetbridge-EmpaticaE4/app/src/debug/aidl -I/home/ismael/Gadgetbridge-EmpaticaE4/app/src/mainlineDebug/aidl -I/home/ismael/.gradle/caches/8.14.3/transforms/360fce3357d8fa64ee60bca7ca381179/transformed/media-1.0.0/aidl -I/home/ismael/.gradle/caches/8.14.3/transforms/1da19ddfb0db7e6722c2b847ec2413d6/transformed/core-1.17.0/aidl -I/home/ismael/.gradle/caches/8.14.3/transforms/7d9314fbe77cd913c209f56df7488e3a/transformed/versionedparcelable-1.1.1/aidl -d/tmp/aidl11043301789785738886.d /home/ismael/Gadgetbridge-EmpaticaE4/app/src/main/aidl/net/osmand/aidlapi/IOsmAndAidlCallback.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package net.osmand.aidlapi;
public interface IOsmAndAidlCallback extends android.os.IInterface
{
  /** Default implementation for IOsmAndAidlCallback. */
  public static class Default implements net.osmand.aidlapi.IOsmAndAidlCallback
  {
    /**
     *  Callback for search requests.
     * 
     *  @return resultSet - set of SearchResult
     */
    @Override public void onSearchComplete(java.util.List<net.osmand.aidlapi.search.SearchResult> resultSet) throws android.os.RemoteException
    {
    }
    /**  Callback for {@link IOsmAndAidlInterface} registerForUpdates() method. */
    @Override public void onUpdate() throws android.os.RemoteException
    {
    }
    /**  Callback for {@link IOsmAndAidlInterface} registerForOsmandInitListener() method. */
    @Override public void onAppInitialized() throws android.os.RemoteException
    {
    }
    /**
     *  Callback for {@link IOsmAndAidlInterface} getBitmapForGpx() method.
     * 
     *  @return bitmap - snapshot image of gpx track on map
     */
    @Override public void onGpxBitmapCreated(net.osmand.aidlapi.gpx.AGpxBitmap bitmap) throws android.os.RemoteException
    {
    }
    /**
     *  Callback for {@link IOsmAndAidlInterface} registerForNavigationUpdates() method.
     * 
     *  @return directionInfo - update on distance to next turn and turns type.
     */
    @Override public void updateNavigationInfo(net.osmand.aidlapi.navigation.ADirectionInfo directionInfo) throws android.os.RemoteException
    {
    }
    /**
     *  Callback for {@link IOsmAndAidlInterface} buttons set with addContextMenuButtons() method.
     * 
     *  @param buttonId - id of custom button
     *  @param pointId - id of point button associated with
     *  @param layerId - id of layer point and button associated with
     */
    @Override public void onContextMenuButtonClicked(int buttonId, java.lang.String pointId, java.lang.String layerId) throws android.os.RemoteException
    {
    }
    /**  Callback for {@link IOsmAndAidlInterface} registerForVoiceRouterMessages() method. */
    @Override public void onVoiceRouterNotify(net.osmand.aidlapi.navigation.OnVoiceNavigationParams params) throws android.os.RemoteException
    {
    }
    /**  Callback for {@link IOsmAndAidlInterface} registerForKeyEvents() method. */
    @Override public void onKeyEvent(android.view.KeyEvent params) throws android.os.RemoteException
    {
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements net.osmand.aidlapi.IOsmAndAidlCallback
  {
    /** Construct the stub and attach it to the interface. */
    @SuppressWarnings("this-escape")
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an net.osmand.aidlapi.IOsmAndAidlCallback interface,
     * generating a proxy if needed.
     */
    public static net.osmand.aidlapi.IOsmAndAidlCallback asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof net.osmand.aidlapi.IOsmAndAidlCallback))) {
        return ((net.osmand.aidlapi.IOsmAndAidlCallback)iin);
      }
      return new net.osmand.aidlapi.IOsmAndAidlCallback.Stub.Proxy(obj);
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
        case TRANSACTION_onSearchComplete:
        {
          java.util.List<net.osmand.aidlapi.search.SearchResult> _arg0;
          _arg0 = data.createTypedArrayList(net.osmand.aidlapi.search.SearchResult.CREATOR);
          this.onSearchComplete(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_onUpdate:
        {
          this.onUpdate();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_onAppInitialized:
        {
          this.onAppInitialized();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_onGpxBitmapCreated:
        {
          net.osmand.aidlapi.gpx.AGpxBitmap _arg0;
          _arg0 = _Parcel.readTypedObject(data, net.osmand.aidlapi.gpx.AGpxBitmap.CREATOR);
          this.onGpxBitmapCreated(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_updateNavigationInfo:
        {
          net.osmand.aidlapi.navigation.ADirectionInfo _arg0;
          _arg0 = _Parcel.readTypedObject(data, net.osmand.aidlapi.navigation.ADirectionInfo.CREATOR);
          this.updateNavigationInfo(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_onContextMenuButtonClicked:
        {
          int _arg0;
          _arg0 = data.readInt();
          java.lang.String _arg1;
          _arg1 = data.readString();
          java.lang.String _arg2;
          _arg2 = data.readString();
          this.onContextMenuButtonClicked(_arg0, _arg1, _arg2);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_onVoiceRouterNotify:
        {
          net.osmand.aidlapi.navigation.OnVoiceNavigationParams _arg0;
          _arg0 = _Parcel.readTypedObject(data, net.osmand.aidlapi.navigation.OnVoiceNavigationParams.CREATOR);
          this.onVoiceRouterNotify(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_onKeyEvent:
        {
          android.view.KeyEvent _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.view.KeyEvent.CREATOR);
          this.onKeyEvent(_arg0);
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
    private static class Proxy implements net.osmand.aidlapi.IOsmAndAidlCallback
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
       *  Callback for search requests.
       * 
       *  @return resultSet - set of SearchResult
       */
      @Override public void onSearchComplete(java.util.List<net.osmand.aidlapi.search.SearchResult> resultSet) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedList(_data, resultSet, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onSearchComplete, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**  Callback for {@link IOsmAndAidlInterface} registerForUpdates() method. */
      @Override public void onUpdate() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onUpdate, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**  Callback for {@link IOsmAndAidlInterface} registerForOsmandInitListener() method. */
      @Override public void onAppInitialized() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onAppInitialized, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**
       *  Callback for {@link IOsmAndAidlInterface} getBitmapForGpx() method.
       * 
       *  @return bitmap - snapshot image of gpx track on map
       */
      @Override public void onGpxBitmapCreated(net.osmand.aidlapi.gpx.AGpxBitmap bitmap) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, bitmap, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onGpxBitmapCreated, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**
       *  Callback for {@link IOsmAndAidlInterface} registerForNavigationUpdates() method.
       * 
       *  @return directionInfo - update on distance to next turn and turns type.
       */
      @Override public void updateNavigationInfo(net.osmand.aidlapi.navigation.ADirectionInfo directionInfo) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, directionInfo, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_updateNavigationInfo, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**
       *  Callback for {@link IOsmAndAidlInterface} buttons set with addContextMenuButtons() method.
       * 
       *  @param buttonId - id of custom button
       *  @param pointId - id of point button associated with
       *  @param layerId - id of layer point and button associated with
       */
      @Override public void onContextMenuButtonClicked(int buttonId, java.lang.String pointId, java.lang.String layerId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(buttonId);
          _data.writeString(pointId);
          _data.writeString(layerId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onContextMenuButtonClicked, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**  Callback for {@link IOsmAndAidlInterface} registerForVoiceRouterMessages() method. */
      @Override public void onVoiceRouterNotify(net.osmand.aidlapi.navigation.OnVoiceNavigationParams params) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, params, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onVoiceRouterNotify, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**  Callback for {@link IOsmAndAidlInterface} registerForKeyEvents() method. */
      @Override public void onKeyEvent(android.view.KeyEvent params) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, params, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onKeyEvent, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
    }
    static final int TRANSACTION_onSearchComplete = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_onUpdate = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_onAppInitialized = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_onGpxBitmapCreated = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_updateNavigationInfo = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_onContextMenuButtonClicked = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_onVoiceRouterNotify = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
    static final int TRANSACTION_onKeyEvent = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
  }
  /** @hide */
  public static final java.lang.String DESCRIPTOR = "net.osmand.aidlapi.IOsmAndAidlCallback";
  /**
   *  Callback for search requests.
   * 
   *  @return resultSet - set of SearchResult
   */
  public void onSearchComplete(java.util.List<net.osmand.aidlapi.search.SearchResult> resultSet) throws android.os.RemoteException;
  /**  Callback for {@link IOsmAndAidlInterface} registerForUpdates() method. */
  public void onUpdate() throws android.os.RemoteException;
  /**  Callback for {@link IOsmAndAidlInterface} registerForOsmandInitListener() method. */
  public void onAppInitialized() throws android.os.RemoteException;
  /**
   *  Callback for {@link IOsmAndAidlInterface} getBitmapForGpx() method.
   * 
   *  @return bitmap - snapshot image of gpx track on map
   */
  public void onGpxBitmapCreated(net.osmand.aidlapi.gpx.AGpxBitmap bitmap) throws android.os.RemoteException;
  /**
   *  Callback for {@link IOsmAndAidlInterface} registerForNavigationUpdates() method.
   * 
   *  @return directionInfo - update on distance to next turn and turns type.
   */
  public void updateNavigationInfo(net.osmand.aidlapi.navigation.ADirectionInfo directionInfo) throws android.os.RemoteException;
  /**
   *  Callback for {@link IOsmAndAidlInterface} buttons set with addContextMenuButtons() method.
   * 
   *  @param buttonId - id of custom button
   *  @param pointId - id of point button associated with
   *  @param layerId - id of layer point and button associated with
   */
  public void onContextMenuButtonClicked(int buttonId, java.lang.String pointId, java.lang.String layerId) throws android.os.RemoteException;
  /**  Callback for {@link IOsmAndAidlInterface} registerForVoiceRouterMessages() method. */
  public void onVoiceRouterNotify(net.osmand.aidlapi.navigation.OnVoiceNavigationParams params) throws android.os.RemoteException;
  /**  Callback for {@link IOsmAndAidlInterface} registerForKeyEvents() method. */
  public void onKeyEvent(android.view.KeyEvent params) throws android.os.RemoteException;
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
