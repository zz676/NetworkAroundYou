/*
 * Zhisheng Zhou
 */

// http://standards.ieee.org/regauth/oui/oui.txt

package edu.nyu.ga2620.network.network;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseCorruptException;
import android.database.sqlite.SQLiteException;
import android.util.Log;
import edu.nyu.ga2620.network.utils.Db;

public class HardwareAddress {

	private final static String TAG = "HardwareAddress";
	private final static String REQ = "select vendor from oui where mac=?";
	// 0x1 is HW Type: Ethernet (10Mb) [JBP]
	// 0x2 is ARP Flag: completed entry (ha valid)
	private final static String MAC_RE = "^%s\\s+0x1\\s+0x2\\s+([:0-9a-fA-F]+)\\s+\\*\\s+\\w+$";
	private final static int BUF = 8 * 1024;
	private WeakReference<Activity> mActivity;

	public HardwareAddress(Activity activity) {
	}

	public static String getHardwareAddress(String ip) {
		String hw = NetInfo.NOMAC;
		try {
			if (ip != null) {
				String ptrn = String.format(MAC_RE, ip.replace(".", "\\."));
				Pattern pattern = Pattern.compile(ptrn);
				BufferedReader bufferedReader = new BufferedReader(
						new FileReader("/proc/net/arp"), BUF);
				String line;
				Matcher matcher;
				while ((line = bufferedReader.readLine()) != null) {
					matcher = pattern.matcher(line);
					if (matcher.matches()) {
						hw = matcher.group(1);
						break;
					}
				}
				bufferedReader.close();
			} else {
				Log.e(TAG, "ip is null");
			}
		} catch (IOException e) {
			Log.e(TAG, "Can't open/read file ARP: " + e.getMessage());
			return hw;
		}
		return hw;
	}

	public static String getNicVendor(String hw)
			throws SQLiteDatabaseCorruptException {
		String ni = null;
		try {
			SQLiteDatabase db = SQLiteDatabase.openDatabase(
					Db.PATH + Db.DB_NIC, null, SQLiteDatabase.OPEN_READONLY
							| SQLiteDatabase.NO_LOCALIZED_COLLATORS);
			if (db != null) {
				// Db request
				if (db.isOpen()) {
					Cursor c = db.rawQuery(REQ,
							new String[] { hw.replace(":", "").substring(0, 6)
									.toUpperCase() });
					if (c.moveToFirst()) {
						ni = c.getString(0);
					}
					c.close();
				}
				db.close();
			}
		} catch (IllegalStateException e) {
			Log.e(TAG, e.getMessage());
		} catch (SQLiteException e) {
			Log.e(TAG, e.getMessage());
			// FIXME: Reset db
			// Context ctxt = d.getApplicationContext();
			// Editor edit =
			// PreferenceManager.getDefaultSharedPreferences(ctxt).edit();
			// edit.putInt(Prefs.KEY_RESET_NICDB, 1);
			// edit.commit();
		}
		return ni;
	}
}
