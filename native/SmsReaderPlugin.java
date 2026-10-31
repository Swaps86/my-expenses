package com.swapnilpalande.myexpenses;

import android.Manifest;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;

@CapacitorPlugin(
    name = "SmsReader",
    permissions = { @Permission(strings = { Manifest.permission.READ_SMS }, alias = "sms") }
)
public class SmsReaderPlugin extends Plugin {

    @PluginMethod
    public void read(PluginCall call) {
        if (getPermissionState("sms") != PermissionState.GRANTED) {
            requestPermissionForAlias("sms", call, "smsPermsCallback");
            return;
        }
        loadMessages(call);
    }

    @PermissionCallback
    private void smsPermsCallback(PluginCall call) {
        if (getPermissionState("sms") == PermissionState.GRANTED) {
            loadMessages(call);
        } else {
            call.reject("SMS permission denied");
        }
    }

    private void loadMessages(PluginCall call) {
        long since = call.getLong("since", 0L);
        JSArray list = new JSArray();
        Cursor c = null;
        try {
            ContentResolver cr = getContext().getContentResolver();
            c = cr.query(
                Uri.parse("content://sms/inbox"),
                new String[] { "_id", "address", "body", "date" },
                "date >= ?",
                new String[] { String.valueOf(since) },
                "date DESC"
            );
            int n = 0;
            while (c != null && c.moveToNext() && n < 400) {
                JSObject o = new JSObject();
                o.put("id", c.getString(0));
                o.put("address", c.getString(1));
                o.put("body", c.getString(2));
                o.put("date", c.getLong(3));
                list.put(o);
                n++;
            }
        } catch (Exception e) {
            call.reject("Could not read SMS");
            return;
        } finally {
            if (c != null) {
                c.close();
            }
        }
        JSObject res = new JSObject();
        res.put("messages", list);
        call.resolve(res);
    }
}
