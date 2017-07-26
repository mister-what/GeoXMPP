/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: /Volumes/Android Developing/AndroidWorkspace/GeoXMPP/src/de/napalm/geoxmpp_pp/GeoXMPPServiceCallbacks.aidl
 */
package de.napalm.geoxmpp_pp;
public interface GeoXMPPServiceCallbacks extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements de.napalm.geoxmpp_pp.GeoXMPPServiceCallbacks
{
private static final java.lang.String DESCRIPTOR = "de.napalm.geoxmpp_pp.GeoXMPPServiceCallbacks";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an de.napalm.geoxmpp_pp.GeoXMPPServiceCallbacks interface,
 * generating a proxy if needed.
 */
public static de.napalm.geoxmpp_pp.GeoXMPPServiceCallbacks asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof de.napalm.geoxmpp_pp.GeoXMPPServiceCallbacks))) {
return ((de.napalm.geoxmpp_pp.GeoXMPPServiceCallbacks)iin);
}
return new de.napalm.geoxmpp_pp.GeoXMPPServiceCallbacks.Stub.Proxy(obj);
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
case TRANSACTION_loginSuccessful:
{
data.enforceInterface(DESCRIPTOR);
this.loginSuccessful();
return true;
}
case TRANSACTION_loginFailed:
{
data.enforceInterface(DESCRIPTOR);
this.loginFailed();
return true;
}
case TRANSACTION_entryAdded:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
this.entryAdded(_arg0);
return true;
}
case TRANSACTION_entryRemoved:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
this.entryRemoved(_arg0);
return true;
}
case TRANSACTION_presenceChanged:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
boolean _arg1;
_arg1 = (0!=data.readInt());
this.presenceChanged(_arg0, _arg1);
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements de.napalm.geoxmpp_pp.GeoXMPPServiceCallbacks
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
@Override public void loginSuccessful() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_loginSuccessful, _data, null, android.os.IBinder.FLAG_ONEWAY);
}
finally {
_data.recycle();
}
}
@Override public void loginFailed() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_loginFailed, _data, null, android.os.IBinder.FLAG_ONEWAY);
}
finally {
_data.recycle();
}
}
@Override public void entryAdded(java.lang.String jid) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(jid);
mRemote.transact(Stub.TRANSACTION_entryAdded, _data, null, android.os.IBinder.FLAG_ONEWAY);
}
finally {
_data.recycle();
}
}
@Override public void entryRemoved(java.lang.String jid) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(jid);
mRemote.transact(Stub.TRANSACTION_entryRemoved, _data, null, android.os.IBinder.FLAG_ONEWAY);
}
finally {
_data.recycle();
}
}
@Override public void presenceChanged(java.lang.String jid, boolean isOnline) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(jid);
_data.writeInt(((isOnline)?(1):(0)));
mRemote.transact(Stub.TRANSACTION_presenceChanged, _data, null, android.os.IBinder.FLAG_ONEWAY);
}
finally {
_data.recycle();
}
}
}
static final int TRANSACTION_loginSuccessful = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_loginFailed = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_entryAdded = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
static final int TRANSACTION_entryRemoved = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
static final int TRANSACTION_presenceChanged = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
}
public void loginSuccessful() throws android.os.RemoteException;
public void loginFailed() throws android.os.RemoteException;
public void entryAdded(java.lang.String jid) throws android.os.RemoteException;
public void entryRemoved(java.lang.String jid) throws android.os.RemoteException;
public void presenceChanged(java.lang.String jid, boolean isOnline) throws android.os.RemoteException;
}
