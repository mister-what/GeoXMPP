/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: /Volumes/Android Developing/AndroidWorkspace/GeoXMPP/src/de/napalm/geoxmpp_pp/RemoteInterface.aidl
 */
package de.napalm.geoxmpp_pp;
public interface RemoteInterface extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements de.napalm.geoxmpp_pp.RemoteInterface
{
private static final java.lang.String DESCRIPTOR = "de.napalm.geoxmpp_pp.RemoteInterface";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an de.napalm.geoxmpp_pp.RemoteInterface interface,
 * generating a proxy if needed.
 */
public static de.napalm.geoxmpp_pp.RemoteInterface asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof de.napalm.geoxmpp_pp.RemoteInterface))) {
return ((de.napalm.geoxmpp_pp.RemoteInterface)iin);
}
return new de.napalm.geoxmpp_pp.RemoteInterface.Stub.Proxy(obj);
}
@Override public android.os.IBinder asBinder()
{
return this;
}
@Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
{
switch (code)
{
case INTERFACE_TRANSACTION:
{
reply.writeString(DESCRIPTOR);
return true;
}
case TRANSACTION_registerCallback:
{
data.enforceInterface(DESCRIPTOR);
de.napalm.geoxmpp_pp.GeoXMPPServiceCallbacks _arg0;
_arg0 = de.napalm.geoxmpp_pp.GeoXMPPServiceCallbacks.Stub.asInterface(data.readStrongBinder());
this.registerCallback(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_unregisterCallback:
{
data.enforceInterface(DESCRIPTOR);
this.unregisterCallback();
return true;
}
case TRANSACTION_checkLogin:
{
data.enforceInterface(DESCRIPTOR);
this.checkLogin();
return true;
}
case TRANSACTION_addUser:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
this.addUser(_arg0);
return true;
}
case TRANSACTION_removeUser:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
this.removeUser(_arg0);
return true;
}
case TRANSACTION_getOnlineStatus:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
boolean _result = this.getOnlineStatus(_arg0);
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_getRosterEntries:
{
data.enforceInterface(DESCRIPTOR);
java.util.List<java.lang.String> _result = this.getRosterEntries();
reply.writeNoException();
reply.writeStringList(_result);
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements de.napalm.geoxmpp_pp.RemoteInterface
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
@Override public void registerCallback(de.napalm.geoxmpp_pp.GeoXMPPServiceCallbacks mCallback) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeStrongBinder((((mCallback!=null))?(mCallback.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_registerCallback, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void unregisterCallback() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_unregisterCallback, _data, null, android.os.IBinder.FLAG_ONEWAY);
}
finally {
_data.recycle();
}
}
@Override public void checkLogin() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_checkLogin, _data, null, android.os.IBinder.FLAG_ONEWAY);
}
finally {
_data.recycle();
}
}
@Override public void addUser(java.lang.String jid) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(jid);
mRemote.transact(Stub.TRANSACTION_addUser, _data, null, android.os.IBinder.FLAG_ONEWAY);
}
finally {
_data.recycle();
}
}
@Override public void removeUser(java.lang.String jid) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(jid);
mRemote.transact(Stub.TRANSACTION_removeUser, _data, null, android.os.IBinder.FLAG_ONEWAY);
}
finally {
_data.recycle();
}
}
@Override public boolean getOnlineStatus(java.lang.String jid) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(jid);
mRemote.transact(Stub.TRANSACTION_getOnlineStatus, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public java.util.List<java.lang.String> getRosterEntries() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.util.List<java.lang.String> _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getRosterEntries, _data, _reply, 0);
_reply.readException();
_result = _reply.createStringArrayList();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
}
static final int TRANSACTION_registerCallback = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_unregisterCallback = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_checkLogin = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
static final int TRANSACTION_addUser = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
static final int TRANSACTION_removeUser = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
static final int TRANSACTION_getOnlineStatus = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
static final int TRANSACTION_getRosterEntries = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
}
public void registerCallback(de.napalm.geoxmpp_pp.GeoXMPPServiceCallbacks mCallback) throws android.os.RemoteException;
public void unregisterCallback() throws android.os.RemoteException;
public void checkLogin() throws android.os.RemoteException;
public void addUser(java.lang.String jid) throws android.os.RemoteException;
public void removeUser(java.lang.String jid) throws android.os.RemoteException;
public boolean getOnlineStatus(java.lang.String jid) throws android.os.RemoteException;
public java.util.List<java.lang.String> getRosterEntries() throws android.os.RemoteException;
}
